import AppIntents
import SwiftUI

/// Siri / 快捷指令里显示的结果卡片
struct IntentSnippet: View {
    let emoji: String
    let title: String
    let subtitle: String

    var body: some View {
        HStack(spacing: 14) {
            Text(emoji).font(.system(size: 44))
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.title2.bold())
                Text(subtitle).font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding()
    }
}

struct WhatToEatIntent: AppIntent {
    static let title: LocalizedStringResource = "今天吃什么"
    static let description = IntentDescription("根据当前时间，从星匣美食库里随机挑一道菜。")

    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let pool = FoodLibrary.filter(meal: .current, cuisines: [], tastes: [], maxPrice: 3)
        let dish = (pool.isEmpty ? FoodLibrary.all : pool).randomElement()!
        return .result(dialog: "\(MealTime.current.name)就吃「\(dish.name)」吧！",
                       view: IntentSnippet(emoji: dish.emoji, title: dish.name, subtitle: dish.detail))
    }
}

struct FlipCoinIntent: AppIntent {
    static let title: LocalizedStringResource = "抛硬币"
    static let description = IntentDescription("抛一枚硬币，看看是正面还是反面。")

    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let heads = Bool.random()
        return .result(dialog: heads ? "是正面！" : "是反面！",
                       view: IntentSnippet(emoji: heads ? "🌞" : "🌙", title: heads ? "正面" : "反面", subtitle: "星匣 · 抛硬币"))
    }
}

struct RollDiceIntent: AppIntent {
    static let title: LocalizedStringResource = "掷骰子"
    static let description = IntentDescription("掷 1 到 6 颗骰子。")

    @Parameter(title: "骰子数量", default: 1, inclusiveRange: (1, 6))
    var count: Int

    static var parameterSummary: some ParameterSummary {
        Summary("掷 \(\.$count) 颗骰子")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let values = (0..<max(1, min(6, count))).map { _ in Int.random(in: 1...6) }
        let total = values.reduce(0, +)
        let text = values.count > 1 ? "\(values.map(String.init).joined(separator: " + ")) = \(total)" : "\(total) 点"
        return .result(dialog: "掷出了 \(text)", view: IntentSnippet(emoji: "🎲", title: text, subtitle: "星匣 · 骰子"))
    }
}

struct RandomNumberIntent: AppIntent {
    static let title: LocalizedStringResource = "随机数字"
    static let description = IntentDescription("在指定范围内生成一个随机整数。")

    @Parameter(title: "最小值", default: 1)
    var minimum: Int

    @Parameter(title: "最大值", default: 100)
    var maximum: Int

    static var parameterSummary: some ParameterSummary {
        Summary("在 \(\.$minimum) 到 \(\.$maximum) 之间随机")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog & ReturnsValue<Int> {
        let low = min(minimum, maximum), high = max(minimum, maximum)
        let value = Int.random(in: low...high)
        return .result(value: value, dialog: "随机数是 \(value)")
    }
}

struct TodayFortuneIntent: AppIntent {
    static let title: LocalizedStringResource = "今日运势"
    static let description = IntentDescription("查看当前档案的今日运势。")

    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let fortune = FortuneEngine.daily(for: ProfileSnapshot.load()?.birth, on: .today)
        let dialog = "今日运势\(fortune.levelName)，\(fortune.score) 分。\(fortune.summary)宜\(fortune.funYi.joined(separator: "、"))，忌\(fortune.funJi.joined(separator: "、"))。"
        return .result(dialog: IntentDialog(stringLiteral: dialog),
                       view: IntentSnippet(emoji: "✨", title: "\(fortune.levelName) · \(fortune.score) 分",
                                           subtitle: "幸运色 \(fortune.luckyColor.name) · 幸运数字 \(fortune.luckyNumber)"))
    }
}

struct HoroscopeIntent: AppIntent {
    static let title: LocalizedStringResource = "星座运势"
    static let description = IntentDescription("查看某个星座今天的运势。")

    @Parameter(title: "星座")
    var sign: ZodiacSignAppEnum?

    static var parameterSummary: some ParameterSummary {
        Summary("查看 \(\.$sign) 今日运势")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let resolved = sign?.sign ?? ProfileSnapshot.load().map { ZodiacSign.sunSign(of: $0.birth) } ?? .aries
        let horoscope = HoroscopeEngine.horoscope(resolved, period: .today)
        // 按半星播报，与 App 里的星级一致：整数不带 .0，如「4 星」「3.5 星」
        let stars = horoscope.stars
        let starsText = stars == stars.rounded() ? String(Int(stars)) : String(format: "%.1f", stars)
        return .result(dialog: IntentDialog(stringLiteral: "\(resolved.name)今日 \(starsText) 星。\(horoscope.overview)"),
                       view: IntentSnippet(emoji: resolved.glyph, title: "\(resolved.name) · \(horoscope.score) 分", subtitle: horoscope.comment))
    }
}

struct AnswerBookIntent: AppIntent {
    static let title: LocalizedStringResource = "答案之书"
    static let description = IntentDescription("心中默念一个问题，翻开答案之书。")

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let answers: [String] = ContentLibrary.load("answers")
        return .result(dialog: IntentDialog(stringLiteral: answers.randomElement() ?? "顺其自然"))
    }
}

struct XingXiaShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(intent: WhatToEatIntent(), phrases: [
            "\(.applicationName)今天吃什么",
            "用\(.applicationName)决定吃什么",
            "问\(.applicationName)吃什么",
        ], shortTitle: "今天吃什么", systemImageName: "fork.knife")
        AppShortcut(intent: TodayFortuneIntent(), phrases: [
            "\(.applicationName)今日运势",
            "看看\(.applicationName)今天的运势",
        ], shortTitle: "今日运势", systemImageName: "sparkles")
        AppShortcut(intent: FlipCoinIntent(), phrases: [
            "用\(.applicationName)抛硬币",
            "\(.applicationName)抛个硬币",
        ], shortTitle: "抛硬币", systemImageName: "centsign.circle.fill")
        AppShortcut(intent: RollDiceIntent(), phrases: [
            "用\(.applicationName)掷骰子",
            "\(.applicationName)掷骰子",
        ], shortTitle: "掷骰子", systemImageName: "dice.fill")
        AppShortcut(intent: HoroscopeIntent(), phrases: [
            "\(.applicationName)星座运势",
            "\(.applicationName)查看\(\.$sign)运势",
        ], shortTitle: "星座运势", systemImageName: "moon.stars.fill")
        AppShortcut(intent: AnswerBookIntent(), phrases: [
            "\(.applicationName)答案之书",
            "翻开\(.applicationName)答案之书",
        ], shortTitle: "答案之书", systemImageName: "book.fill")
        AppShortcut(intent: RandomNumberIntent(), phrases: [
            "\(.applicationName)随机数字",
        ], shortTitle: "随机数字", systemImageName: "number")
        AppShortcut(intent: OpenXingXiaIntent(), phrases: [
            "打开\(.applicationName)\(\.$destination)",
        ], shortTitle: "打开工具", systemImageName: "square.grid.2x2")
    }
}
