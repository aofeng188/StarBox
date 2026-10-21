import AppIntents
import Foundation
import SwiftData
import SwiftUI
import WidgetKit

// 可以点的小组件用到的 Intent。Shared 同时编进 App 和小组件：点小组件上的按钮时在小组件进程里执行，
// Siri / 快捷指令的「抛硬币」在 App 进程里执行，两边都通过 SharedStore 写同一个数据库。

/// 各小组件的 kind。App 改了相关数据后用它只刷新对应的小组件
enum WidgetKinds {
    static let fortune = "XingXiaFortune"
    static let horoscope = "XingXiaHoroscope"
    static let countdown = "XingXiaCountdown"
    static let mood = "XingXiaMood"
    static let coin = "XingXiaCoin"
    static let answerBook = "XingXiaAnswerBook"
    static let stick = "XingXiaStick"

    /// App 里记了心情、求了签以后调用：刷新从数据库读记录的心情和今日一签小组件
    static func reloadRecordWidgets() {
        WidgetCenter.shared.reloadTimelines(ofKind: mood)
        WidgetCenter.shared.reloadTimelines(ofKind: stick)
    }
}

/// 心情小组件：点一下记下今天的心情，今天已经记过就改成这次点的
struct LogMoodIntent: AppIntent {
    static let title: LocalizedStringResource = "记录心情"
    static let description = IntentDescription("记下今天的心情。")
    static let isDiscoverable = false

    @Parameter(title: "心情", default: 3, inclusiveRange: (1, 5))
    var mood: Int

    init() {}

    init(mood: Int) {
        self.mood = mood
    }

    @MainActor
    func perform() async throws -> some IntentResult {
        guard let container = SharedStore.container() else { return .result() }
        // 每次新建 context：小组件进程常驻时，mainContext 里留着的旧心情不会随 App 里的修改刷新，改写它会覆盖或冲突
        let context = ModelContext(container)
        let key = DayKey.today.rawValue
        let value = min(max(mood, 1), 5)
        let descriptor = FetchDescriptor<MoodEntry>(predicate: #Predicate { $0.dayKey == key },
                                                    sortBy: [SortDescriptor(\.date, order: .reverse)])
        if let entry = (try? context.fetch(descriptor))?.first {
            entry.mood = value
            entry.date = Date()
        } else {
            context.insert(MoodEntry(dayKey: key, mood: value))
        }
        try? context.save()
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetKinds.mood)
        return .result()
    }
}

/// 抛硬币：抛硬币小组件和 Siri「抛硬币」共用，结果记进决策历史（tool "coin"）
struct FlipCoinIntent: AppIntent {
    static let title: LocalizedStringResource = "抛硬币"
    static let description = IntentDescription("抛一枚硬币，看看是正面还是反面。")

    init() {}

    @MainActor
    func perform() async throws -> some IntentResult & ProvidesDialog & ShowsSnippetView {
        let heads = TrueRandom.int(0...1) == 0
        WidgetToys.recordCoin(heads: heads)
        if let container = SharedStore.container() {
            let context = container.mainContext
            context.insert(DecisionRecord(tool: "coin", title: "", result: heads ? "正面" : "反面"))
            try? context.save()
        }
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetKinds.coin)
        // 小组件里点按钮时系统不显示对话和结果卡，只刷新小组件
        return .result(dialog: heads ? "是正面！" : "是反面！", view: CoinSnippet(heads: heads))
    }
}

/// 答案之书小组件：随机翻开一页
struct AskAnswerBookIntent: AppIntent {
    static let title: LocalizedStringResource = "翻开答案之书"
    static let description = IntentDescription("从答案之书里随机翻开一页。")
    static let isDiscoverable = false

    init() {}

    @MainActor
    func perform() async throws -> some IntentResult {
        WidgetToys.recordAnswer(OracleContent.randomAnswer(avoiding: WidgetToys.lastAnswer?.text))
        WidgetCenter.shared.reloadTimelines(ofKind: WidgetKinds.answerBook)
        return .result()
    }
}

/// Siri 抛硬币的结果卡
private struct CoinSnippet: View {
    let heads: Bool

    var body: some View {
        HStack(spacing: 14) {
            Text(heads ? "🌞" : "🌙").font(.system(size: 44))
            VStack(alignment: .leading, spacing: 4) {
                Text(heads ? "正面" : "反面").font(.title2.bold())
                Text("星匣 · 抛硬币").font(.subheadline).foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .padding()
    }
}

/// 小组件里抛硬币、翻答案之书的最近一次结果，存在 AppGroup.defaults。按天计次，过了零点重新数
enum WidgetToys {
    struct Coin: Codable, Sendable {
        var heads: Bool
        /// DayKey.rawValue
        var day: Int
        /// 当天第几次
        var count: Int
    }

    struct Answer: Codable, Sendable {
        var text: String
        var day: Int
        var count: Int
    }

    private static let coinKey = "widget.coin"
    private static let answerKey = "widget.answer"

    static var lastCoin: Coin? { read(coinKey) }
    static var lastAnswer: Answer? { read(answerKey) }

    /// 只取某一天的结果，别的日子的当作还没抛过
    static func coin(on day: DayKey) -> Coin? {
        lastCoin.flatMap { $0.day == day.rawValue ? $0 : nil }
    }

    static func answer(on day: DayKey) -> Answer? {
        lastAnswer.flatMap { $0.day == day.rawValue ? $0 : nil }
    }

    @discardableResult
    static func recordCoin(heads: Bool, today: DayKey = .today) -> Coin {
        let value = Coin(heads: heads, day: today.rawValue, count: (coin(on: today)?.count ?? 0) + 1)
        write(value, coinKey)
        return value
    }

    @discardableResult
    static func recordAnswer(_ text: String, today: DayKey = .today) -> Answer {
        let value = Answer(text: text, day: today.rawValue, count: (answer(on: today)?.count ?? 0) + 1)
        write(value, answerKey)
        return value
    }

    private static func read<T: Decodable>(_ key: String) -> T? {
        guard let data = AppGroup.defaults.data(forKey: key) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }

    private static func write<T: Encodable>(_ value: T, _ key: String) {
        guard let data = try? JSONEncoder().encode(value) else { return }
        AppGroup.defaults.set(data, forKey: key)
    }
}

/// 小组件和 Intent 用的答案之书、签诗，只解码用得到的字段。
/// 两个 JSON 在 Shared/Content，App 和小组件各打包一份；读不到时给空表，小组件不会因此崩溃
enum OracleContent {
    struct Stick: Decodable, Sendable {
        let n: Int
        let level: String
        let title: String

        /// 「上上」「中平」「下签」，和 App 里签的印章字一致
        var levelText: String { level == "下" ? "下签" : level }
    }

    static let answers: [String] = decode("answers") ?? []
    static let sticks: [Stick] = decode("sticks") ?? []

    static func stick(_ number: Int) -> Stick? {
        sticks.first { $0.n == number }
    }

    /// 随机一条答案，尽量不和上一次一样
    static func randomAnswer(avoiding previous: String? = nil) -> String {
        let pool = answers.filter { $0 != previous }
        let source = pool.isEmpty ? answers : pool
        guard !source.isEmpty else { return "顺其自然" }
        return source[TrueRandom.int(0...(source.count - 1))]
    }

    private static func decode<T: Decodable>(_ name: String) -> T? {
        guard let url = Bundle.main.url(forResource: name, withExtension: "json"),
              let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(T.self, from: data)
    }
}
