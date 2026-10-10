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
    let status: CountdownStatus
    let colorIndex: Int
    let pinned: Bool

    var entity: CountdownEntity { CountdownEntity(id: id, title: title, emoji: emoji) }
}

@MainActor
enum CountdownStore {
    private static let container: ModelContainer? = try? AppSchema.makeContainer()

    static func all() -> [CountdownValue] {
        guard let container else { return [] }
        let items = (try? container.mainContext.fetch(FetchDescriptor<Countdown>(sortBy: [SortDescriptor(\.createdAt)]))) ?? []
        return items.map {
            CountdownValue(id: $0.id, title: $0.title, emoji: $0.emoji, status: $0.status(), colorIndex: $0.colorIndex, pinned: $0.pinned)
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
        CountdownEntry(date: Date(), value: CountdownValue(id: UUID(), title: "春节", emoji: "🧧",
                                                           status: CountdownStatus(days: 120, prefix: "还有", suffix: "天", caption: "农历正月初一", targetDay: .today, isToday: false),
                                                           colorIndex: 1, pinned: true))
    }

    func snapshot(for configuration: CountdownConfigIntent, in context: Context) async -> CountdownEntry {
        await entry(for: configuration)
    }

    func timeline(for configuration: CountdownConfigIntent, in context: Context) async -> Timeline<CountdownEntry> {
        let calendar = Calendar.gregorianLocal
        let tomorrow = calendar.startOfDay(for: calendar.date(byAdding: .day, value: 1, to: Date()) ?? Date())
        return Timeline(entries: [await entry(for: configuration)], policy: .after(tomorrow.addingTimeInterval(5)))
    }

    @MainActor
    private func entry(for configuration: CountdownConfigIntent) -> CountdownEntry {
        let all = CountdownStore.all()
        let chosen = configuration.countdown.flatMap { selected in all.first { $0.id == selected.id } }
            ?? all.first(where: \.pinned)
            ?? all.filter { $0.status.prefix != "已过" }.min { $0.status.days < $1.status.days }
            ?? all.first
        return CountdownEntry(date: Date(), value: chosen)
    }
}

struct CountdownWidgetView: View {
    let entry: CountdownEntry
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = WidgetPalette.current(scheme)
        if let value = entry.value {
            let color = palette.color(at: value.colorIndex)
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
                        Text(status.suffix).font(.system(size: 32, weight: .heavy, design: .rounded)).foregroundStyle(color)
                    } else {
                        HStack(alignment: .firstTextBaseline, spacing: 2) {
                            Text("\(status.days)")
                                .font(.system(size: 44, weight: .heavy, design: .rounded))
                                .monospacedDigit()
                                .foregroundStyle(color)
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
