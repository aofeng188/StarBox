import SwiftUI
import SwiftData
import WidgetKit
import AppIntents

/// 小组件里可选择的倒数日
struct CountdownEntity: AppEntity {
    let id: UUID
    let title: String
    let emoji: String

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "倒数日"
    static let defaultQuery = CountdownQuery()

    var displayRepresentation: DisplayRepresentation { DisplayRepresentation(title: "\(emoji) \(title)") }
}

struct CountdownQuery: EntityQuery {
    func entities(for identifiers: [UUID]) async throws -> [CountdownEntity] {
        await CountdownStore.all().filter { identifiers.contains($0.id) }.map(\.entity)
    }

    func suggestedEntities() async throws -> [CountdownEntity] {
        await CountdownStore.all().map(\.entity)
    }
}

/// 从共享数据库读取倒数日（拷贝成值类型，避免跨线程使用模型对象）
struct CountdownValue: Sendable {
    let id: UUID
    let title: String
    let emoji: String
    let kind: CountdownKind
    let status: CountdownStatus
    let colorIndex: Int
    let pinned: Bool

    var entity: CountdownEntity { CountdownEntity(id: id, title: title, emoji: emoji) }
}

@MainActor
enum CountdownStore {
    private static let container: ModelContainer? = try? AppSchema.makeContainer()

    static func all() -> [CountdownValue] {
        all(on: [.today]).first ?? []
    }

    /// 读一次库，按给定的每一天分别计算状态
    static func all(on days: [DayKey]) -> [[CountdownValue]] {
        guard let container else { return days.map { _ in [] } }
        let items = (try? container.mainContext.fetch(FetchDescriptor<Countdown>(sortBy: [SortDescriptor(\.createdAt)]))) ?? []
        return days.map { day in
            items.map {
                CountdownValue(id: $0.id, title: $0.title, emoji: $0.emoji, kind: $0.kindValue, status: $0.status(today: day),
                               colorIndex: $0.colorIndex, pinned: $0.pinned)
            }
        }
    }
}

struct CountdownConfigIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "选择倒数日"
    static let description = IntentDescription("不选时显示置顶或最近的一个。")

    @Parameter(title: "倒数日")
    var countdown: CountdownEntity?
}

struct CountdownEntry: TimelineEntry {
    let date: Date
    let value: CountdownValue?
}

struct CountdownProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> CountdownEntry {
        CountdownEntry(date: Date(), value: CountdownValue(id: UUID(), title: "春节", emoji: "🧧", kind: .countdown,
                                                           status: CountdownStatus(days: 120, prefix: "还有", suffix: "天", caption: "农历正月初一", targetDay: .today, isToday: false),
                                                           colorIndex: 1, pinned: true))
    }

    func snapshot(for configuration: CountdownConfigIntent, in context: Context) async -> CountdownEntry {
        await entries(for: configuration, days: 1).first ?? CountdownEntry(date: Date(), value: nil)
    }

    func timeline(for configuration: CountdownConfigIntent, in context: Context) async -> Timeline<CountdownEntry> {
        // 一次排好今天和之后 6 个零点的条目：系统推迟刷新时，天数也会按时变化
        let upcoming = await entries(for: configuration, days: 7)
        let calendar = Calendar.gregorianLocal
        let tomorrow = calendar.startOfDay(for: calendar.date(byAdding: .day, value: 1, to: Date()) ?? Date())
        return Timeline(entries: upcoming, policy: .after(tomorrow.addingTimeInterval(5)))
    }

    @MainActor
    private func entries(for configuration: CountdownConfigIntent, days count: Int) -> [CountdownEntry] {
        let today = DayKey.today
        let days = (0..<max(1, count)).map { today.adding(days: $0) }
        let lists = CountdownStore.all(on: days)
        return days.indices.map { index in
            CountdownEntry(date: index == 0 ? Date() : days[index].startOfDay,
                           value: choose(from: lists[index], for: configuration))
        }
    }

    private func choose(from all: [CountdownValue], for configuration: CountdownConfigIntent) -> CountdownValue? {
        configuration.countdown.flatMap { selected in all.first { $0.id == selected.id } }
            ?? all.first(where: \.pinned)
            // 和「记录」页一致：纪念日的数字是已经过了多少天，不和倒数天数比远近
            ?? all.filter { $0.kind != .anniversary && $0.status.prefix != "已过" }.min { $0.status.days < $1.status.days }
            ?? all.first
    }
}

struct CountdownWidgetView: View {
    let entry: CountdownEntry
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = WidgetPalette.current(scheme)
        if let value = entry.value {
            // 当文字用，取调过明暗的版本（浅色模式下粉彩色会融进背景）
            let ink = palette.inkColor(at: value.colorIndex)
            let status = value.status
            if family == .accessoryRectangular {
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(value.emoji) \(value.title)").font(.headline).lineLimit(1)
                    Text(status.isToday ? "就是今天" : "\(status.prefix) \(status.days) \(status.suffix)").font(.title3.bold()).widgetAccentable()
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                VStack(alignment: .leading, spacing: 2) {
                    HStack {
                        Text(value.emoji).font(.title3)
                        Text(value.title).font(.subheadline.weight(.semibold)).lineLimit(1)
                    }
                    Spacer(minLength: 0)
                    Text(status.prefix).font(.caption).foregroundStyle(.secondary)
                    if status.isToday && status.suffix != "天" {
                        Text(status.suffix).font(.system(size: 32, weight: .heavy, design: .rounded)).foregroundStyle(ink)
                    } else {
                        HStack(alignment: .firstTextBaseline, spacing: 2) {
                            Text("\(status.days)")
                                .font(.system(size: 44, weight: .heavy, design: .rounded))
                                .monospacedDigit()
                                .foregroundStyle(ink)
                                .minimumScaleFactor(0.5)
                                .widgetAccentable()
                            Text(status.suffix).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    Text(status.caption).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        } else {
            VStack(spacing: 6) {
                Image(systemName: "hourglass").font(.title2)
                Text(AppGroup.isAvailable ? "在星匣里添加倒数日" : "打开星匣查看倒数日").font(.caption).multilineTextAlignment(.center)
            }
        }
    }
}

struct CountdownWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "XingXiaCountdown", intent: CountdownConfigIntent.self, provider: CountdownProvider()) { entry in
            CountdownWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetCosmicBackground() }
                .widgetURL(URL(string: "xingxia://countdowns"))
        }
        .configurationDisplayName("倒数日")
        .description("生日、纪念日、节假日倒计时。")
        .supportedFamilies([.systemSmall, .accessoryRectangular])
    }
}
