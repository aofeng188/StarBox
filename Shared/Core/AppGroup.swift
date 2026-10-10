import Foundation

/// App 与小组件之间共享数据的通道。
///
/// 免费 Apple ID 签名时 App Group 可能不可用，这时 `isAvailable` 为 false，
/// App 会退回到普通的 UserDefaults 和默认数据库位置，小组件则显示通用内容。
enum AppGroup {
    static let identifier = "group.com.tianaofeng.xingxia"

    static var containerURL: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: identifier)
    }

    static var isAvailable: Bool { containerURL != nil }

    static let defaults: UserDefaults = {
        if isAvailable, let shared = UserDefaults(suiteName: identifier) {
            return shared
        }
        return .standard
    }()
}

/// 小组件、Siri、通知需要的「当前档案」精简信息。
struct ProfileSnapshot: Codable, Hashable, Sendable {
    var id: UUID
    var name: String
    var avatar: String
    var birthday: Date
    var birthHour: Int?
    var birthMinute: Int?
    var birthCityID: String?

    var birth: BirthInfo {
        BirthInfo(birthday: birthday, hour: birthHour, minute: birthMinute, cityID: birthCityID)
    }

    private static let key = "primaryProfileSnapshot"

    static func load() -> ProfileSnapshot? {
        guard let data = AppGroup.defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(ProfileSnapshot.self, from: data)
    }

    static func save(_ snapshot: ProfileSnapshot?) {
        if let snapshot, let data = try? JSONEncoder().encode(snapshot) {
            AppGroup.defaults.set(data, forKey: key)
        } else {
            AppGroup.defaults.removeObject(forKey: key)
        }
    }
}

/// 生日 + 可选出生时间/城市。
struct BirthInfo: Hashable, Sendable {
    var birthday: Date
    var hour: Int?
    var minute: Int?
    var cityID: String?

    var dayKey: DayKey { DayKey(birthday) }
    var hasTime: Bool { hour != nil }
    var city: BirthCity { BirthCity.find(cityID) ?? .beijing }

    /// 出生时刻（城市当地时间）；不知道时间时取中午。
    var moment: Date {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = city.timeZone
        let key = dayKey
        return calendar.date(from: DateComponents(year: key.year, month: key.month, day: key.day,
                                                  hour: hour ?? 12, minute: minute ?? 0)) ?? birthday
    }
}
