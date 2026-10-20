import SwiftUI
import WidgetKit

struct FortuneEntry: TimelineEntry {
    let date: Date
    let fortune: DailyFortune
    let almanac: AlmanacDay
    let name: String?
}

struct FortuneProvider: TimelineProvider {
    func placeholder(in context: Context) -> FortuneEntry { entry(for: Date()) }

    func getSnapshot(in context: Context, completion: @escaping (FortuneEntry) -> Void) {
        completion(entry(for: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<FortuneEntry>) -> Void) {
        let now = Date()
        let calendar = Calendar.gregorianLocal
        let tomorrow = calendar.startOfDay(for: calendar.date(byAdding: .day, value: 1, to: now) ?? now)
        let dayAfter = calendar.date(byAdding: .day, value: 1, to: tomorrow) ?? tomorrow
        completion(Timeline(entries: [entry(for: now), entry(for: tomorrow.addingTimeInterval(1))], policy: .after(dayAfter)))
    }

    private func entry(for date: Date) -> FortuneEntry {
        let profile = ProfileSnapshot.load()
        let day = DayKey(date)
        return FortuneEntry(date: date, fortune: FortuneEngine.daily(for: profile?.birth, on: day), almanac: AlmanacDay(day), name: profile?.name)
    }
}

struct FortuneWidgetView: View {
    let entry: FortuneEntry
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme
    @Environment(\.widgetRenderingMode) private var renderingMode

    var body: some View {
        let palette = WidgetPalette.current(scheme)
        let f = entry.fortune
        switch family {
        case .accessoryCircular:
            Gauge(value: Double(f.score), in: 0...100) {
                Text("运势")
            } currentValueLabel: {
                Text("\(f.score)")
            }
            .gaugeStyle(.accessoryCircularCapacity)
            .widgetAccentable()
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 1) {
                Text("今日 \(f.levelName) · \(f.score)分").font(.headline).widgetAccentable()
                Text("宜 \(f.funYi.prefix(2).joined(separator: " "))").font(.caption)
                Text("忌 \(f.funJi.prefix(2).joined(separator: " "))").font(.caption)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        case .accessoryInline:
            Text("✨ \(f.levelName) · 宜\(f.funYi.first ?? "")")
        case .systemMedium:
            HStack(spacing: 14) {
                VStack(spacing: 6) {
                    WidgetScoreRing(score: f.score, lineWidth: 8).frame(width: 78, height: 78)
                    Text(f.levelName).font(.headline).foregroundStyle(palette.accentGradient)
                }
                VStack(alignment: .leading, spacing: 6) {
                    Text("\(entry.almanac.key.month)月\(entry.almanac.key.day)日 · 农历\(entry.almanac.lunar.text)")
                        .font(.caption2).foregroundStyle(.secondary)
                    Text(f.summary).font(.subheadline.weight(.medium)).lineLimit(2)
                    HStack(spacing: 4) {
                        badge("宜", fill: .positive(scheme))
                        Text(f.funYi.joined(separator: " ")).font(.caption).lineLimit(1)
                    }
                    HStack(spacing: 4) {
                        badge("忌", fill: .negative(scheme))
                        Text(f.funJi.joined(separator: " ")).font(.caption).lineLimit(1)
                    }
                    HStack(spacing: 4) {
                        Circle().fill(Color(f.luckyColor)).frame(width: 8, height: 8)
                        Text("\(f.luckyColor.name) · 数字\(f.luckyNumber) · \(f.luckyDirection)").font(.caption2).foregroundStyle(.secondary)
                    }
                }
                Spacer(minLength: 0)
            }
            .widgetURL(URL(string: "xingxia://fortune"))
        default:
            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("今日运势").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                    Spacer()
                    Circle().fill(Color(f.luckyColor)).frame(width: 10, height: 10)
                }
                HStack(alignment: .center, spacing: 10) {
                    WidgetScoreRing(score: f.score, lineWidth: 6).frame(width: 56, height: 56)
                    Text(f.levelName)
                        .font(.system(size: 26, weight: .heavy, design: .rounded))
                        .foregroundStyle(palette.accentGradient)
                        .minimumScaleFactor(0.6)
                }
                Spacer(minLength: 0)
                Text("宜 " + f.funYi.prefix(2).joined(separator: " ")).font(.caption2).lineLimit(1)
                Text("忌 " + f.funJi.prefix(2).joined(separator: " ")).font(.caption2).lineLimit(1).foregroundStyle(.secondary)
            }
            .widgetURL(URL(string: "xingxia://fortune"))
        }
    }

    /// 「宜」「忌」圆形徽章：底色用语义色，文字按底色亮度选深浅；着色/透明模式下颜色会被统一，改用描边
    @ViewBuilder
    private func badge(_ text: String, fill: Color) -> some View {
        if renderingMode == .fullColor {
            Text(text).font(.caption2.bold()).foregroundStyle(fill.readableForeground).padding(3).background(Circle().fill(fill))
        } else {
            Text(text).font(.caption2.bold()).padding(3).background(Circle().stroke(.primary.opacity(0.6), lineWidth: 1))
        }
    }
}

struct FortuneWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetKinds.fortune, provider: FortuneProvider()) { entry in
            FortuneWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetCosmicBackground() }
        }
        .configurationDisplayName("今日运势")
        .description("运势指数、幸运色与今日宜忌。")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}
