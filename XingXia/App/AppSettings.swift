import SwiftUI
import WidgetKit

/// 用户偏好（音效、震动、提醒、首页模块等），持久化在 UserDefaults。
@Observable
@MainActor
final class AppSettings {
    static let shared = AppSettings()

    private let defaults = AppGroup.defaults

    var soundEnabled: Bool { didSet { defaults.set(soundEnabled, forKey: "soundEnabled"); SoundEngine.shared.isEnabled = soundEnabled } }
    var soundVolume: Double { didSet { defaults.set(soundVolume, forKey: "soundVolume"); SoundEngine.shared.volume = Float(soundVolume) } }
    var hapticsEnabled: Bool { didSet { defaults.set(hapticsEnabled, forKey: "hapticsEnabled"); Haptics.isEnabled = hapticsEnabled } }
    var shakeEnabled: Bool { didSet { defaults.set(shakeEnabled, forKey: "shakeEnabled") } }
    var backgroundAnimation: Bool { didSet { defaults.set(backgroundAnimation, forKey: "backgroundAnimation") } }
    var dailyReminderEnabled: Bool { didSet { defaults.set(dailyReminderEnabled, forKey: "dailyReminderEnabled") } }
    var reminderHour: Int { didSet { defaults.set(reminderHour, forKey: "reminderHour") } }
    var reminderMinute: Int { didSet { defaults.set(reminderMinute, forKey: "reminderMinute") } }
    var homeCards: [HomeCard] { didSet { defaults.set(homeCards.map(\.rawValue), forKey: "homeCards") } }
    var hiddenHomeCards: Set<HomeCard> { didSet { defaults.set(hiddenHomeCards.map(\.rawValue), forKey: "hiddenHomeCards") } }
    var favoriteTools: [String] { didSet { defaults.set(favoriteTools, forKey: "favoriteTools") } }
    var almanacShowsTraditional: Bool { didSet { defaults.set(almanacShowsTraditional, forKey: "almanacShowsTraditional") } }
    var hasOnboarded: Bool { didSet { defaults.set(hasOnboarded, forKey: "hasOnboarded") } }
    /// 塔罗用完整 78 张（关掉只用 22 张大阿卡纳）
    var tarotFullDeck: Bool { didSet { defaults.set(tarotFullDeck, forKey: "tarotFullDeck") } }
    /// 档案里的生日自动出现在倒数日列表、首页和小组件里
    var profileBirthdaysInCountdowns: Bool {
        didSet {
            defaults.set(profileBirthdaysInCountdowns, forKey: CountdownItem.profileBirthdaysKey)
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
    /// 满月、新月、流星雨极大前一天晚上提醒
    var skyReminders: Bool { didSet { defaults.set(skyReminders, forKey: "skyReminders") } }
    /// 每天第一次打开 App 时展示「每日仪式」（月相、运势、一签、心情）
    var dailyRitualEnabled: Bool { didSet { defaults.set(dailyRitualEnabled, forKey: "dailyRitualEnabled") } }

    private init() {
        defaults.register(defaults: [
            "soundEnabled": true, "soundVolume": 0.7, "hapticsEnabled": true, "shakeEnabled": true,
            "backgroundAnimation": true, "dailyReminderEnabled": false, "reminderHour": 8, "reminderMinute": 0,
            "almanacShowsTraditional": false, "hasOnboarded": false,
            "tarotFullDeck": true, CountdownItem.profileBirthdaysKey: true, "skyReminders": false,
            "dailyRitualEnabled": true,
        ])
        soundEnabled = defaults.bool(forKey: "soundEnabled")
        soundVolume = defaults.double(forKey: "soundVolume")
        hapticsEnabled = defaults.bool(forKey: "hapticsEnabled")
        shakeEnabled = defaults.bool(forKey: "shakeEnabled")
        backgroundAnimation = defaults.bool(forKey: "backgroundAnimation")
        dailyReminderEnabled = defaults.bool(forKey: "dailyReminderEnabled")
        reminderHour = defaults.integer(forKey: "reminderHour")
        reminderMinute = defaults.integer(forKey: "reminderMinute")
        almanacShowsTraditional = defaults.bool(forKey: "almanacShowsTraditional")
        hasOnboarded = defaults.bool(forKey: "hasOnboarded")
        tarotFullDeck = defaults.bool(forKey: "tarotFullDeck")
        profileBirthdaysInCountdowns = defaults.bool(forKey: CountdownItem.profileBirthdaysKey)
        skyReminders = defaults.bool(forKey: "skyReminders")
        dailyRitualEnabled = defaults.bool(forKey: "dailyRitualEnabled")
        var favorites = defaults.stringArray(forKey: "favoriteTools") ?? ["food", "coin", "wheel", "dice"]
        // 答案之书以前固定在首页快捷工具末尾，现在改成普通收藏项：升级后第一次启动补进收藏，保持原样，之后可以移除
        if !defaults.bool(forKey: "answersFavoriteMigrated") {
            if !favorites.contains(FavoriteTool.answersID) { favorites.append(FavoriteTool.answersID) }
            defaults.set(favorites, forKey: "favoriteTools")
            defaults.set(true, forKey: "answersFavoriteMigrated")
        }
        favoriteTools = favorites

        var cards = (defaults.stringArray(forKey: "homeCards") ?? []).compactMap(HomeCard.init(rawValue:))
        // 新版本加入的模块放到默认顺序里它前一个模块的后面（如「家人今日」跟在「今日运势」后面），找不到就追加到末尾
        for (index, card) in HomeCard.allCases.enumerated() where !cards.contains(card) {
            if index == 0 {
                cards.insert(card, at: 0)
            } else if let anchor = cards.firstIndex(of: HomeCard.allCases[index - 1]) {
                cards.insert(card, at: anchor + 1)
            } else {
                cards.append(card)
            }
        }
        // 「决定回访」随记录自动出现，固定在最前、不能隐藏
        homeCards = HomeCard.normalized(cards)
        hiddenHomeCards = HomeCard.normalizedHidden((defaults.stringArray(forKey: "hiddenHomeCards") ?? []).compactMap(HomeCard.init(rawValue:)))

        SoundEngine.shared.isEnabled = soundEnabled
        SoundEngine.shared.volume = Float(soundVolume)
        Haptics.isEnabled = hapticsEnabled
    }

    func isFavorite(_ toolID: String) -> Bool {
        favoriteTools.contains(toolID)
    }

    func toggleFavorite(_ toolID: String) {
        if let index = favoriteTools.firstIndex(of: toolID) {
            favoriteTools.remove(at: index)
        } else {
            favoriteTools.append(toolID)
        }
    }
}

/// 首页模块。followUp（决定回访）排在最前，有需要回访的决定时自动出现、没有时不占位置，
/// 不出现在「编辑首页」列表里，也不能隐藏；其余模块按用户的顺序和开关
enum HomeCard: String, CaseIterable, Identifiable {
    case followUp, fortune, family, almanac, horoscope, stick, quickTools, countdown, mood, quote

    var id: String { rawValue }

    /// 「编辑首页」里可以排序、隐藏的模块
    var isEditable: Bool { self != .followUp }

    /// followUp 挪到最前，其余保持原顺序
    static func normalized(_ cards: [HomeCard]) -> [HomeCard] {
        [.followUp] + cards.filter { $0 != .followUp }
    }

    /// 隐藏列表里去掉不能隐藏的 followUp
    static func normalizedHidden<S: Sequence>(_ cards: S) -> Set<HomeCard> where S.Element == HomeCard {
        Set(cards.filter(\.isEditable))
    }

    var name: String {
        switch self {
        case .followUp: "决定回访"
        case .fortune: "今日运势"
        case .family: "家人今日"
        case .almanac: "宜忌"
        case .horoscope: "星座运势"
        case .stick: "今日一签"
        case .quickTools: "快捷工具"
        case .countdown: "倒数日"
        case .mood: "心情打卡"
        case .quote: "每日一句"
        }
    }

    var symbol: String {
        switch self {
        case .followUp: "arrow.uturn.backward.circle"
        case .fortune: "sparkles"
        case .family: "person.2"
        case .almanac: "book.closed"
        case .horoscope: "moon.stars"
        case .stick: "flame"
        case .quickTools: "square.grid.2x2"
        case .countdown: "hourglass"
        case .mood: "face.smiling"
        case .quote: "quote.opening"
        }
    }
}
