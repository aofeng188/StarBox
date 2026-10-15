import SwiftUI

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

    private init() {
        defaults.register(defaults: [
            "soundEnabled": true, "soundVolume": 0.7, "hapticsEnabled": true, "shakeEnabled": true,
            "backgroundAnimation": true, "dailyReminderEnabled": false, "reminderHour": 8, "reminderMinute": 0,
            "almanacShowsTraditional": false, "hasOnboarded": false,
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
        var favorites = defaults.stringArray(forKey: "favoriteTools") ?? ["food", "coin", "wheel", "dice"]
        // 答案之书以前固定在首页快捷工具末尾，现在改成普通收藏项：升级后第一次启动补进收藏，保持原样，之后可以移除
        if !defaults.bool(forKey: "answersFavoriteMigrated") {
            if !favorites.contains(FavoriteTool.answersID) { favorites.append(FavoriteTool.answersID) }
            defaults.set(favorites, forKey: "favoriteTools")
            defaults.set(true, forKey: "answersFavoriteMigrated")
        }
        favoriteTools = favorites

        let stored = (defaults.stringArray(forKey: "homeCards") ?? []).compactMap(HomeCard.init(rawValue:))
        // 新版本加入的模块追加到末尾
        homeCards = stored + HomeCard.allCases.filter { !stored.contains($0) }
        hiddenHomeCards = Set((defaults.stringArray(forKey: "hiddenHomeCards") ?? []).compactMap(HomeCard.init(rawValue:)))

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

/// 首页模块
enum HomeCard: String, CaseIterable, Identifiable {
    case fortune, almanac, horoscope, stick, quickTools, countdown, mood, quote

    var id: String { rawValue }

    var name: String {
        switch self {
        case .fortune: "今日运势"
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
        case .fortune: "sparkles"
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
