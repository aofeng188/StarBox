import Foundation

enum CountdownKind: String, CaseIterable, Identifiable, Sendable {
    case countdown, anniversary, birthday

    var id: String { rawValue }

    var name: String {
        switch self {
        case .countdown: "倒数日"
        case .anniversary: "纪念日"
        case .birthday: "生日"
        }
    }

    var symbol: String {
        switch self {
        case .countdown: "hourglass"
        case .anniversary: "heart.circle"
        case .birthday: "gift"
        }
    }
}

struct CountdownStatus: Sendable {
    /// 主数字
    let days: Int
    /// 主数字前的说明，如「还有」「已经」
    let prefix: String
    /// 主数字后的单位，如「天」
    let suffix: String
    /// 次要说明，如「2027年3月1日 周一」「第 3 个周年」
    let caption: String
    let targetDay: DayKey
    let isToday: Bool
}

extension Countdown {
    var kindValue: CountdownKind { CountdownKind(rawValue: kind) ?? .countdown }

    var originDay: DayKey { DayKey(date) }

    /// 农历生日是否记录了出生年份。没记录时 date 存的是创建当天（旧数据里没动过「出生年份参考」的也是这样），不显示年龄。
    var knowsBirthYear: Bool {
        !(isLunar && kindValue == .birthday && originDay == DayKey(createdAt))
    }

    func nextOccurrence(from today: DayKey = .today) -> DayKey {
        let repeats = repeatsYearly || kindValue == .birthday
        if isLunar {
            // 不重复的农历日子：取不早于 date（创建日，或编辑器保存时写入的换算结果）的第一个对应公历日期
            return Self.lunarOccurrence(month: lunarMonth, day: lunarDay, onOrAfter: repeats ? today : originDay) ?? originDay
        }
        guard repeats else { return originDay }
        for year in (today.year - 1)...(today.year + 2) {
            let origin = originDay
            var day = origin.day
            // 2 月 29 日在平年按 2 月 28 日算
            if origin.month == 2 && day == 29 && !Self.isLeapYear(year) { day = 28 }
            let candidate = DayKey(year: year, month: origin.month, day: day)
            if candidate >= today { return candidate }
        }
        return originDay
    }

    /// 农历某月某日在 start 当天或之后第一次出现的公历日期；小月没有三十时按廿九算
    static func lunarOccurrence(month: Int, day: Int, onOrAfter start: DayKey) -> DayKey? {
        for year in (start.year - 1)...(start.year + 2) {
            let date = LunarDate.solarDate(gregorianYear: year, month: month, day: day, isLeap: false)
                ?? LunarDate.solarDate(gregorianYear: year, month: month, day: day - 1, isLeap: false)
            guard let date else { continue }
            let candidate = DayKey(date)
            if candidate >= start { return candidate }
        }
        return nil
    }

    func status(today: DayKey = .today) -> CountdownStatus {
        switch kindValue {
        case .countdown:
            let target = nextOccurrence(from: today)
            let diff = today.days(to: target)
            let caption = DateText.full(target) + " " + DateText.weekday(target)
            if diff >= 0 {
                return CountdownStatus(days: diff, prefix: diff == 0 ? "就是" : "还有", suffix: diff == 0 ? "今天" : "天",
                                       caption: caption, targetDay: target, isToday: diff == 0)
            }
            return CountdownStatus(days: -diff, prefix: "已过", suffix: "天", caption: caption, targetDay: target, isToday: false)
        case .anniversary:
            let origin = originDay
            let passed = origin.days(to: today)
            if passed < 0 {
                return CountdownStatus(days: -passed, prefix: "还有", suffix: "天", caption: DateText.full(origin),
                                       targetDay: origin, isToday: false)
            }
            var caption = "始于 " + DateText.full(origin)
            let next = nextOccurrence(from: today)
            if repeatsYearly {
                let years = next.year - origin.year
                let left = today.days(to: next)
                caption = left == 0 ? "今天是 \(years) 周年纪念日" : "距 \(years) 周年还有 \(left) 天"
            }
            return CountdownStatus(days: passed + 1, prefix: "第", suffix: "天", caption: caption, targetDay: origin,
                                   isToday: repeatsYearly && next == today)
        case .birthday:
            let next = nextOccurrence(from: today)
            let left = today.days(to: next)
            let age = next.year - originDay.year
            let calendarName = isLunar ? "农历\(LunarDate.monthNames[max(0, lunarMonth - 1)])月\(LunarDate.dayNames[max(0, lunarDay - 1)])" : DateText.monthDay(originDay)
            var caption = calendarName
            if knowsBirthYear && age > 0 {
                caption += left == 0 ? " · \(age) 岁生日" : " · 即将 \(age) 岁"
            }
            return CountdownStatus(days: left, prefix: left == 0 ? "生日" : "还有", suffix: left == 0 ? "快乐" : "天",
                                   caption: caption, targetDay: next, isToday: left == 0)
        }
    }

    private static func isLeapYear(_ year: Int) -> Bool {
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }
}
