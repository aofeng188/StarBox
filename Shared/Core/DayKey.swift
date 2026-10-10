import Foundation

/// 用 yyyyMMdd 形式的整数标识「某一天」，便于存储、比较和作为随机种子。
struct DayKey: Hashable, Comparable, Codable, CustomStringConvertible, Sendable {
    let year: Int
    let month: Int
    let day: Int

    init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    init(_ date: Date, calendar: Calendar = .gregorianLocal) {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        self.init(year: c.year ?? 2000, month: c.month ?? 1, day: c.day ?? 1)
    }

    init?(rawValue: Int) {
        guard rawValue > 10000101 else { return nil }
        self.init(year: rawValue / 10000, month: rawValue / 100 % 100, day: rawValue % 100)
    }

    static var today: DayKey { DayKey(Date()) }

    var rawValue: Int { year * 10000 + month * 100 + day }
    var description: String { String(rawValue) }

    /// 当天中午 12 点（本地时区），避免夏令时/零点边界问题。
    var date: Date {
        Calendar.gregorianLocal.date(from: DateComponents(year: year, month: month, day: day, hour: 12)) ?? Date()
    }

    var startOfDay: Date { Calendar.gregorianLocal.startOfDay(for: date) }

    func adding(days: Int) -> DayKey {
        DayKey(Calendar.gregorianLocal.date(byAdding: .day, value: days, to: date) ?? date)
    }

    /// 从 1949-10-01（甲子日）起算的天数，用于干支纪日。
    var daysSinceJiaZiReference: Int {
        DayKey.julianDayNumber(year: year, month: month, day: day) - DayKey.julianDayNumber(year: 1949, month: 10, day: 1)
    }

    /// 0 = 周日 … 6 = 周六
    var weekday: Int {
        (DayKey.julianDayNumber(year: year, month: month, day: day) + 1) % 7
    }

    static func julianDayNumber(year: Int, month: Int, day: Int) -> Int {
        let a = (14 - month) / 12
        let y = year + 4800 - a
        let m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    static func < (lhs: DayKey, rhs: DayKey) -> Bool { lhs.rawValue < rhs.rawValue }

    func days(to other: DayKey) -> Int {
        DayKey.julianDayNumber(year: other.year, month: other.month, day: other.day)
            - DayKey.julianDayNumber(year: year, month: month, day: day)
    }
}

extension Calendar {
    /// 公历 + 设备当前时区。
    static var gregorianLocal: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        calendar.locale = Locale(identifier: "zh_CN")
        calendar.firstWeekday = 2
        return calendar
    }

    static var chineseLocal: Calendar {
        var calendar = Calendar(identifier: .chinese)
        calendar.timeZone = TimeZone(identifier: "Asia/Shanghai") ?? .current
        calendar.locale = Locale(identifier: "zh_CN")
        return calendar
    }
}

enum DateText {
    static let weekdayNames = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"]

    static func monthDay(_ key: DayKey) -> String { "\(key.month)月\(key.day)日" }

    static func full(_ key: DayKey) -> String { "\(key.year)年\(key.month)月\(key.day)日" }

    static func weekday(_ key: DayKey) -> String { weekdayNames[key.weekday] }

    static func time(_ date: Date) -> String {
        date.formatted(.dateTime.hour(.twoDigits(amPM: .omitted)).minute(.twoDigits))
    }

    static func relative(_ date: Date) -> String {
        let today = DayKey.today
        let key = DayKey(date)
        switch key.days(to: today) {
        case 0: return "今天 " + time(date)
        case 1: return "昨天 " + time(date)
        case 2: return "前天 " + time(date)
        default:
            if key.year == today.year { return "\(key.month)月\(key.day)日 " + time(date) }
            return "\(key.year)/\(key.month)/\(key.day)"
        }
    }
}
