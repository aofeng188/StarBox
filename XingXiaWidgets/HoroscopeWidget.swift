import SwiftUI
import WidgetKit
import AppIntents

struct HoroscopeConfigIntent: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "选择星座"
    static let description = IntentDescription("不选时使用星匣里当前档案的星座。")

    @Parameter(title: "星座")
    var sign: ZodiacSignAppEnum?
}

struct HoroscopeEntry: TimelineEntry {
    let date: Date
    let horoscope: Horoscope
}

struct HoroscopeProvider: AppIntentTimelineProvider {
    func placeholder(in context: Context) -> HoroscopeEntry {
        HoroscopeEntry(date: Date(), horoscope: HoroscopeEngine.horoscope(.leo, period: .today))
    }

    func snapshot(for configuration: HoroscopeConfigIntent, in context: Context) async -> HoroscopeEntry {
        entry(for: configuration, date: Date())
    }

    func timeline(for configuration: HoroscopeConfigIntent, in context: Context) async -> Timeline<HoroscopeEntry> {
        let calendar = Calendar.gregorianLocal
        let tomorrow = calendar.startOfDay(for: calendar.date(byAdding: .day, value: 1, to: Date()) ?? Date())
        return Timeline(entries: [entry(for: configuration, date: Date()), entry(for: configuration, date: tomorrow.addingTimeInterval(1))],
                        policy: .after(tomorrow.addingTimeInterval(86_400)))
    }

    private func entry(for configuration: HoroscopeConfigIntent, date: Date) -> HoroscopeEntry {
        let sign = configuration.sign?.sign ?? ProfileSnapshot.load().map { ZodiacSign.sunSign(of: $0.birth) } ?? .aries
        return HoroscopeEntry(date: date, horoscope: HoroscopeEngine.horoscope(sign, period: .today, reference: DayKey(date)))
    }
}

struct HoroscopeWidgetView: View {
    let entry: HoroscopeEntry
    @Environment(\.widgetFamily) private var family
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = WidgetPalette.current(scheme)
        let h = entry.horoscope
        if family == .systemMedium {
            HStack(spacing: 14) {
                VStack(spacing: 4) {
                    Text(h.sign.glyph).font(.system(size: 44)).foregroundStyle(palette.accentGradient).widgetAccentable()
                    Text(h.sign.name).font(.headline)
                    stars(h.stars)
                }
                .frame(width: 92)
                VStack(alignment: .leading, spacing: 5) {
                    Text(h.comment).font(.subheadline.weight(.semibold)).lineLimit(1)
                    ForEach(FortuneCategory.allCases) { category in
                        HStack(spacing: 6) {
                            Image(systemName: category.symbol).font(.caption2).frame(width: 14)
                            Text(category.shortName).font(.caption2)
                            GeometryReader { proxy in
                                Capsule().fill(.primary.opacity(0.12))
                                    .overlay(alignment: .leading) {
                                        Capsule().fill(palette.accentGradient).frame(width: proxy.size.width * Double(h.score(category)) / 100)
                                    }
                            }
                            .frame(height: 5)
                        }
                    }
                    Text("速配 \(h.matchSign.name) · 星座幸运色 \(h.luckyColor.name)")
                        .font(.caption2).foregroundStyle(.secondary).lineLimit(1).minimumScaleFactor(0.8)
                }
            }
            .widgetURL(URL(string: "xingxia://horoscope"))
        } else {
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(h.sign.glyph).font(.title).foregroundStyle(palette.accentGradient).widgetAccentable()
                    Spacer()
                    Text("\(h.score)").font(.title2.bold()).monospacedDigit()
                }
                Text(h.sign.name).font(.headline)
                stars(h.stars)
                Spacer(minLength: 0)
                Text(h.comment).font(.caption).lineLimit(2).foregroundStyle(.secondary)
            }
            .widgetURL(URL(string: "xingxia://horoscope"))
        }
    }

    private func stars(_ value: Double) -> some View {
        HStack(spacing: 1) {
            ForEach(0..<5, id: \.self) { i in
                let v = value - Double(i)
                Image(systemName: v >= 1 ? "star.fill" : (v >= 0.5 ? "star.leadinghalf.filled" : "star"))
                    .font(.system(size: 9))
            }
        }
        // 浅色模式下亮黄色星星几乎看不见，用按深浅模式调整过的金色
        .foregroundStyle(Color.gold(scheme))
    }
}

struct HoroscopeWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: WidgetKinds.horoscope, intent: HoroscopeConfigIntent.self, provider: HoroscopeProvider()) { entry in
            HoroscopeWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetCosmicBackground() }
        }
        .configurationDisplayName("星座运势")
        .description("长按小组件可以切换星座。")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}
