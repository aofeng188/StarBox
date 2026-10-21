import SwiftUI
import SwiftData
import WidgetKit

struct StickLockEntry: TimelineEntry {
    enum Status {
        case drawn(OracleContent.Stick)
        case notDrawn
        /// App Group 不可用，读不到 App 里的求签记录
        case unavailable
    }

    let date: Date
    let status: Status
}

struct StickLockProvider: TimelineProvider {
    func placeholder(in context: Context) -> StickLockEntry {
        StickLockEntry(date: Date(), status: .drawn(OracleContent.Stick(n: 1, level: "上上", title: "云开见月")))
    }

    func getSnapshot(in context: Context, completion: @escaping (StickLockEntry) -> Void) {
        completion(context.isPreview ? placeholder(in: context) : StickLockEntry(date: Date(), status: status(on: .today)))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<StickLockEntry>) -> Void) {
        // 零点那条直接排好：过了零点回到「去求签」
        let now = Date()
        let today = DayKey(now)
        let tomorrow = today.adding(days: 1)
        let entries = [StickLockEntry(date: now, status: status(on: today)),
                       StickLockEntry(date: tomorrow.startOfDay, status: status(on: tomorrow))]
        completion(Timeline(entries: entries, policy: .after(tomorrow.startOfDay.addingTimeInterval(5))))
    }

    /// 只认当前档案那天的签（给家人朋友求的记在他们名下），和首页「今日一签」一致；
    /// 读不到当前档案（还没建档，或档案还没同步过来）时取那天最近求的一支
    private func status(on day: DayKey) -> StickLockEntry.Status {
        guard let container = SharedStore.container() else { return .unavailable }
        let context = ModelContext(container)
        let key = day.rawValue
        let descriptor = FetchDescriptor<StickDraw>(predicate: #Predicate { $0.dayKey == key },
                                                    sortBy: [SortDescriptor(\.date, order: .reverse)])
        let draws = (try? context.fetch(descriptor)) ?? []
        let draw: StickDraw?
        if let owner = ProfileSnapshot.load()?.id {
            draw = draws.first { $0.profileID == owner }
        } else {
            draw = draws.first { $0.profileID == nil } ?? draws.first
        }
        guard let number = draw?.stickNumber, let stick = OracleContent.stick(number) else { return .notDrawn }
        return .drawn(stick)
    }
}

struct StickLockWidgetView: View {
    let entry: StickLockEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if family == .accessoryInline {
            inline
        } else {
            rectangular
        }
    }

    @ViewBuilder
    private var inline: some View {
        switch entry.status {
        case .drawn(let stick):
            Label("今日一签 \(stick.levelText)", systemImage: "flame.fill")
        case .notDrawn:
            Label("今日一签 · 去求签", systemImage: "flame")
        case .unavailable:
            Label("打开星匣求签", systemImage: "flame")
        }
    }

    private var rectangular: some View {
        VStack(alignment: .leading, spacing: 1) {
            Label("今日一签", systemImage: "flame.fill")
                .font(.caption)
                .foregroundStyle(.secondary)
            switch entry.status {
            case .drawn(let stick):
                Text("第 \(stick.n) 签 · \(stick.levelText)")
                    .font(.headline)
                    .widgetAccentable()
                Text(stick.title).font(.caption).lineLimit(1)
            case .notDrawn:
                Text("去求签").font(.headline).widgetAccentable()
                Text("今天还没求签").font(.caption).lineLimit(1)
            case .unavailable:
                Text("打开星匣").font(.headline).widgetAccentable()
                Text("求签后在这里看").font(.caption).lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .combine)
    }
}

struct StickLockWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetKinds.stick, provider: StickLockProvider()) { entry in
            StickLockWidgetView(entry: entry)
                .containerBackground(for: .widget) { Color.clear }
                .widgetURL(URL(string: "xingxia://stick"))
        }
        .configurationDisplayName("今日一签")
        .description("在锁屏上看今天求到的签。")
        .supportedFamilies([.accessoryRectangular, .accessoryInline])
    }
}
