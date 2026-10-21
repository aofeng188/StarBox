import SwiftUI
import SwiftData
import WidgetKit
import AppIntents

/// 小组件里的五个心情，和 App 的 MoodLevel 一一对应（1…5）
private struct MoodFace: Identifiable {
    let value: Int
    let emoji: String
    let name: String
    let color: Color

    var id: Int { value }

    static let all: [MoodFace] = [
        MoodFace(value: 1, emoji: "😣", name: "很糟", color: Color(hex: 0x8B95A7)),
        MoodFace(value: 2, emoji: "😔", name: "低落", color: Color(hex: 0x7C9CF5)),
        MoodFace(value: 3, emoji: "😐", name: "一般", color: Color(hex: 0x5ECFD6)),
        MoodFace(value: 4, emoji: "🙂", name: "不错", color: Color(hex: 0x7EE08A)),
        MoodFace(value: 5, emoji: "🤩", name: "超棒", color: Color(hex: 0xFFC94D)),
    ]

    static func find(_ value: Int?) -> MoodFace? {
        all.first { $0.value == value }
    }
}

struct MoodWidgetEntry: TimelineEntry {
    let date: Date
    /// 今天记下的心情（1…5），还没记是 nil
    let mood: Int?
    /// App Group 可用、小组件写的心情 App 能看到
    let isAvailable: Bool
}

struct MoodProvider: TimelineProvider {
    func placeholder(in context: Context) -> MoodWidgetEntry {
        MoodWidgetEntry(date: Date(), mood: nil, isAvailable: true)
    }

    func getSnapshot(in context: Context, completion: @escaping (MoodWidgetEntry) -> Void) {
        completion(context.isPreview ? MoodWidgetEntry(date: Date(), mood: 4, isAvailable: true) : entry(on: .today, date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<MoodWidgetEntry>) -> Void) {
        // 零点那条直接排好：过了零点心情清空，不等系统下一次刷新
        let now = Date()
        let today = DayKey(now)
        let tomorrow = today.adding(days: 1).startOfDay
        let entries = [entry(on: today, date: now),
                       MoodWidgetEntry(date: tomorrow, mood: nil, isAvailable: AppGroup.canShareRecords)]
        completion(Timeline(entries: entries, policy: .after(tomorrow.addingTimeInterval(5))))
    }

    private func entry(on day: DayKey, date: Date) -> MoodWidgetEntry {
        guard let container = SharedStore.container() else {
            return MoodWidgetEntry(date: date, mood: nil, isAvailable: false)
        }
        let context = ModelContext(container)
        let key = day.rawValue
        let descriptor = FetchDescriptor<MoodEntry>(predicate: #Predicate { $0.dayKey == key },
                                                    sortBy: [SortDescriptor(\.date, order: .reverse)])
        let mood = (try? context.fetch(descriptor))?.first?.mood
        return MoodWidgetEntry(date: date, mood: mood, isAvailable: true)
    }
}

struct MoodWidgetView: View {
    let entry: MoodWidgetEntry
    @Environment(\.widgetFamily) private var family

    var body: some View {
        if !entry.isAvailable {
            WidgetOpenAppView(symbol: "face.smiling", text: "打开星匣记录心情")
        } else if family == .systemMedium {
            medium
        } else {
            small
        }
    }

    private var title: String {
        MoodFace.find(entry.mood).map { "今天\($0.name)" } ?? "今天心情如何？"
    }

    private var small: some View {
        let faces = MoodFace.all
        return VStack(alignment: .leading, spacing: 8) {
            header
            Spacer(minLength: 0)
            // 小尺寸分两行，按钮大一些好点
            VStack(spacing: 6) {
                HStack(spacing: 6) {
                    ForEach(faces.prefix(3)) { face in button(face, size: 36, showsName: false) }
                }
                HStack(spacing: 6) {
                    ForEach(faces.suffix(2)) { face in button(face, size: 36, showsName: false) }
                }
            }
            .frame(maxWidth: .infinity)
        }
    }

    private var medium: some View {
        VStack(alignment: .leading, spacing: 8) {
            header
            Spacer(minLength: 0)
            HStack(spacing: 0) {
                ForEach(MoodFace.all) { face in
                    button(face, size: 46, showsName: true).frame(maxWidth: .infinity)
                }
            }
        }
    }

    private var header: some View {
        HStack(spacing: 4) {
            Image(systemName: "face.smiling")
            Text(title).lineLimit(1).minimumScaleFactor(0.8)
        }
        .font(.caption.weight(.semibold))
        .foregroundStyle(.secondary)
        .accessibilityElement(children: .combine)
    }

    private func button(_ face: MoodFace, size: CGFloat, showsName: Bool) -> some View {
        let isSelected = entry.mood == face.value
        return Button(intent: LogMoodIntent(mood: face.value)) {
            VStack(spacing: 4) {
                Text(face.emoji)
                    .font(.system(size: size * 0.55))
                    .frame(width: size, height: size)
                    .background {
                        Circle()
                            .fill(face.color.opacity(isSelected ? 0.35 : 0.0))
                            .overlay(Circle().strokeBorder(face.color.opacity(isSelected ? 0.9 : 0), lineWidth: 1.5))
                            .widgetAccentable()
                    }
                    .scaleEffect(isSelected ? 1.08 : 1)
                    .grayscale(entry.mood == nil || isSelected ? 0 : 0.6)
                if showsName {
                    Text(face.name)
                        .font(.caption2)
                        .foregroundStyle(.primary.opacity(isSelected ? 1 : 0.7))
                }
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(face.name)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

struct MoodWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetKinds.mood, provider: MoodProvider()) { entry in
            MoodWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetCosmicBackground() }
                .widgetURL(URL(string: "xingxia://mood"))
        }
        .configurationDisplayName("心情")
        .description("点一下记下今天的心情。")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
