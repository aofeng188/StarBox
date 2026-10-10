import SwiftUI
import WidgetKit

enum AppearanceMode: String, CaseIterable, Identifiable {
    case system, light, dark

    var id: String { rawValue }
    var name: String { ["system": "跟随系统", "light": "浅色", "dark": "深色"][rawValue]! }
    var symbol: String { ["system": "circle.lefthalf.filled", "light": "sun.max", "dark": "moon"][rawValue]! }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

enum ThemeRuleMode: String, CaseIterable, Identifiable {
    case fixed, dailyRandom, launchRandom, timeOfDay, weekday, luckyColor, zodiac

    var id: String { rawValue }

    var name: String {
        switch self {
        case .fixed: "固定主题"
        case .dailyRandom: "每天随机"
        case .launchRandom: "每次打开随机"
        case .timeOfDay: "按时段切换"
        case .weekday: "按星期切换"
        case .luckyColor: "跟随今日幸运色"
        case .zodiac: "跟随星座元素"
        }
    }

    var detail: String {
        switch self {
        case .fixed: "始终使用你选中的主题。"
        case .dailyRandom: "每天零点从随机池里抽一套，当天保持不变。"
        case .launchRandom: "每次打开 App 都换一套，惊喜感拉满。"
        case .timeOfDay: "清晨、白天、傍晚、深夜各用一套主题。"
        case .weekday: "周一到周日，每天一套专属主题。"
        case .luckyColor: "根据今日运势的幸运五行自动配色。"
        case .zodiac: "根据当前档案的星座元素（火土风水）配色。"
        }
    }

    var symbol: String {
        switch self {
        case .fixed: "pin.fill"
        case .dailyRandom: "calendar"
        case .launchRandom: "shuffle"
        case .timeOfDay: "clock"
        case .weekday: "calendar.day.timeline.left"
        case .luckyColor: "paintpalette.fill"
        case .zodiac: "moon.stars.fill"
        }
    }
}

enum TimeSlot: Int, CaseIterable, Identifiable {
    case morning, daytime, evening, night

    var id: Int { rawValue }
    var name: String { ["清晨", "白天", "傍晚", "深夜"][rawValue] }
    var range: String { ["5:00–11:00", "11:00–17:00", "17:00–21:00", "21:00–5:00"][rawValue] }
    var symbol: String { ["sunrise.fill", "sun.max.fill", "sunset.fill", "moon.stars.fill"][rawValue] }

    static func current(_ date: Date = Date()) -> TimeSlot {
        switch Calendar.current.component(.hour, from: date) {
        case 5..<11: .morning
        case 11..<17: .daytime
        case 17..<21: .evening
        default: .night
        }
    }
}

/// 管理主题选择和自动切换规则。
@Observable
@MainActor
final class ThemeManager {
    static let shared = ThemeManager()

    private let defaults = AppGroup.defaults

    var appearance: AppearanceMode { didSet { save("appearance", appearance.rawValue) } }
    var mode: ThemeRuleMode { didSet { save("themeMode", mode.rawValue); refresh() } }
    var fixedThemeID: String { didSet { save("fixedThemeID", fixedThemeID); refresh() } }
    var randomPool: [String] { didSet { save("randomPool", randomPool); refresh() } }
    var timeSlotThemes: [String] { didSet { save("timeSlotThemes", timeSlotThemes); refresh() } }
    var weekdayThemes: [String] { didSet { save("weekdayThemes", weekdayThemes); refresh() } }
    var lowPowerOverride: Bool { didSet { save("lowPowerOverride", lowPowerOverride); refresh() } }
    var festivalEggs: Bool { didSet { save("festivalEggs", festivalEggs); refresh() } }

    private(set) var current: AppTheme = .nebula
    /// 当前主题的来由，展示在主题设置页
    private(set) var reason: String = ""

    private var launchRandomID: String
    private var birth: BirthInfo?

    private init() {
        let d = AppGroup.defaults
        let allIDs = AppTheme.all.map(\.id)
        appearance = AppearanceMode(rawValue: d.string(forKey: "appearance") ?? "") ?? .system
        mode = ThemeRuleMode(rawValue: d.string(forKey: "themeMode") ?? "") ?? .fixed
        fixedThemeID = d.string(forKey: "fixedThemeID") ?? "nebula"
        let storedPool = d.stringArray(forKey: "randomPool") ?? allIDs
        randomPool = storedPool
        timeSlotThemes = d.stringArray(forKey: "timeSlotThemes") ?? ["sakura", "aurora", "sunset", "nebula"]
        weekdayThemes = d.stringArray(forKey: "weekdayThemes") ?? ["solarGold", "nebula", "aurora", "sunset", "abyss", "sakura", "redGiant"]
        lowPowerOverride = d.object(forKey: "lowPowerOverride") as? Bool ?? true
        festivalEggs = d.object(forKey: "festivalEggs") as? Bool ?? true
        let pool = storedPool.isEmpty ? allIDs : storedPool
        launchRandomID = pool.randomElement() ?? "nebula"
        refresh()

        NotificationCenter.default.addObserver(forName: .NSProcessInfoPowerStateDidChange, object: nil, queue: .main) { _ in
            Task { @MainActor in ThemeManager.shared.refresh() }
        }
    }

    private func save(_ key: String, _ value: Any) { defaults.set(value, forKey: key) }

    func update(birth: BirthInfo?) {
        self.birth = birth
        refresh()
    }

    func shuffleNow() {
        let pool = randomPool.isEmpty ? AppTheme.all.map(\.id) : randomPool
        launchRandomID = pool.filter { $0 != current.id }.randomElement() ?? launchRandomID
        refresh()
    }

    /// 重新计算当前主题。时段规则依赖时间，由界面定时调用。
    func refresh(now: Date = Date()) {
        let (theme, why) = resolve(now: now)
        if theme.id != current.id {
            current = theme
            defaults.set(theme.id, forKey: AppTheme.sharedKey)
            WidgetCenter.shared.reloadAllTimelines()
        }
        reason = why
    }

    private func resolve(now: Date) -> (AppTheme, String) {
        if lowPowerOverride && ProcessInfo.processInfo.isLowPowerModeEnabled {
            return (.eventHorizon, "低电量模式已开启，切换为省电的「黑洞」主题")
        }
        let today = DayKey(now)
        if festivalEggs, let egg = festivalTheme(today) {
            return egg
        }
        switch mode {
        case .fixed:
            return (AppTheme.find(fixedThemeID), "固定使用")
        case .dailyRandom:
            let pool = randomPool.isEmpty ? AppTheme.all.map(\.id) : randomPool
            var rng = SeededGenerator("daily-theme", today.rawValue)
            return (AppTheme.find(rng.pick(pool)), "今日随机抽中")
        case .launchRandom:
            return (AppTheme.find(launchRandomID), "本次打开随机抽中")
        case .timeOfDay:
            let slot = TimeSlot.current(now)
            return (AppTheme.find(timeSlotThemes[safe: slot.rawValue]), "现在是\(slot.name)")
        case .weekday:
            let weekday = today.weekday
            // weekdayThemes 以周一开始
            let index = (weekday + 6) % 7
            return (AppTheme.find(weekdayThemes[safe: index]), "今天是\(DateText.weekday(today))")
        case .luckyColor:
            let fortune = FortuneEngine.daily(for: birth, on: today)
            let theme = AppTheme.forElement(fortune.favorableElement)
            return (theme, "今日幸运五行属\(fortune.favorableElement.name)，幸运色\(fortune.luckyColor.name)")
        case .zodiac:
            guard let birth else { return (AppTheme.find(fixedThemeID), "还没有设置生日，暂用固定主题") }
            let sign = ZodiacSign.sunSign(of: birth)
            return (AppTheme.forZodiac(sign.element), "\(sign.name)属于\(sign.element.name)星座")
        }
    }

    private func festivalTheme(_ day: DayKey) -> (AppTheme, String)? {
        let festivals = Festivals.on(day, lunar: LunarDate(day.date))
        let table: [(String, AppTheme)] = [
            ("春节", .redGiant), ("除夕", .redGiant), ("元宵节", .redGiant), ("中秋节", .solarGold),
            ("七夕", .sakura), ("情人节", .sakura), ("圣诞节", .aurora), ("平安夜", .aurora), ("万圣夜", .eventHorizon),
        ]
        for (name, theme) in table where festivals.contains(name) {
            return (theme, "节日彩蛋：\(name)快乐")
        }
        return nil
    }
}

extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
