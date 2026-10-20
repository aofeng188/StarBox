import Foundation
import SwiftData

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

    /// 当前进程是不是小组件扩展
    static let isExtension = Bundle.main.bundlePath.hasSuffix(".appex")

    /// 小组件里写进数据库的记录 App 能不能看到：扩展在没有 App Group 时只能写进自己的沙盒，等于白写
    static var canShareRecords: Bool { isAvailable || !isExtension }
}

/// 进程内共用的数据库容器：小组件的时间线和 Intent 共用一个，Siri 在 App 进程里跑 Intent 时用 App 自己的。
///
/// App 启动时调用 `SharedStore.use(container)` 登记；没登记时第一次用到再建。
/// 小组件扩展拿不到 App Group 时返回 nil，调用方显示「打开 App」，不写数据。
enum SharedStore {
    private static let lock = NSLock()
    nonisolated(unsafe) private static var cached: ModelContainer?

    static func use(_ container: ModelContainer) {
        lock.withLock { cached = container }
    }

    static func container() -> ModelContainer? {
        guard AppGroup.canShareRecords else { return nil }
        return lock.withLock {
            if let cached { return cached }
            let made = try? AppSchema.makeContainer()
            cached = made
            return made
        }
    }
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
