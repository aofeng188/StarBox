import SwiftUI
import WidgetKit
import AppIntents

struct CoinWidgetEntry: TimelineEntry {
    let date: Date
    /// 今天最近一次的结果，今天还没抛过是 nil
    let coin: WidgetToys.Coin?
    /// 抛的结果要记进决策历史，App Group 不可用时 App 看不到，只显示「打开星匣」
    let isAvailable: Bool
}

struct CoinProvider: TimelineProvider {
    func placeholder(in context: Context) -> CoinWidgetEntry {
        CoinWidgetEntry(date: Date(), coin: nil, isAvailable: true)
    }

    func getSnapshot(in context: Context, completion: @escaping (CoinWidgetEntry) -> Void) {
        if context.isPreview {
            completion(CoinWidgetEntry(date: Date(), coin: WidgetToys.Coin(heads: true, day: DayKey.today.rawValue, count: 1), isAvailable: true))
        } else {
            completion(CoinWidgetEntry(date: Date(), coin: WidgetToys.coin(on: .today), isAvailable: AppGroup.canShareRecords))
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<CoinWidgetEntry>) -> Void) {
        // 次数按天算，零点回到「点一下」
        let now = Date()
        let today = DayKey(now)
        let tomorrow = today.adding(days: 1).startOfDay
        let available = AppGroup.canShareRecords
        let entries = [CoinWidgetEntry(date: now, coin: WidgetToys.coin(on: today), isAvailable: available),
                       CoinWidgetEntry(date: tomorrow, coin: nil, isAvailable: available)]
        completion(Timeline(entries: entries, policy: .after(tomorrow.addingTimeInterval(5))))
    }
}

struct CoinWidgetView: View {
    let entry: CoinWidgetEntry
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        if entry.isAvailable {
            content
        } else {
            WidgetOpenAppView(symbol: "centsign.circle.fill", text: "打开星匣抛硬币")
        }
    }

    private var content: some View {
        let coin = entry.coin
        return VStack(spacing: 6) {
            HStack {
                Text("抛硬币").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                Spacer(minLength: 4)
                if let coin {
                    Text("第 \(coin.count) 次")
                        .font(.caption2)
                        .monospacedDigit()
                        .foregroundStyle(.secondary)
                        .contentTransition(.numericText(value: Double(coin.count)))
                }
            }
            Spacer(minLength: 0)
            Button(intent: FlipCoinIntent()) {
                WidgetCoinFace(heads: coin?.heads)
                    .frame(width: 72, height: 72)
                    // 每抛一次换一个身份，新结果从上面翻进来；减弱动态效果时只淡入淡出
                    .id(coin?.count ?? 0)
                    .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.push(from: .top))
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text(coin.map { $0.heads ? "抛硬币，这次是正面" : "抛硬币，这次是反面" } ?? "抛硬币"))
            Spacer(minLength: 0)
            Text(coin.map { $0.heads ? "正面" : "反面" } ?? "点硬币抛一次")
                .font(coin == nil ? Font.caption : Font.headline)
                .foregroundStyle(coin == nil ? HierarchicalShapeStyle.secondary : HierarchicalShapeStyle.primary)
                .lineLimit(1)
                .contentTransition(.opacity)
                .accessibilityHidden(true)
        }
    }
}

/// 和 App 里的硬币同一套样子：正面太阳，反面月亮；还没抛时显示问号
private struct WidgetCoinFace: View {
    let heads: Bool?

    var body: some View {
        let ink = Color(hex: 0x7A5A0E)
        ZStack {
            Circle()
                .fill(RadialGradient(colors: [Color(hex: 0xFFF1B8), Color(hex: 0xF2C94C), Color(hex: 0xB8862E)],
                                     center: .init(x: 0.35, y: 0.3), startRadius: 2, endRadius: 60))
            Circle()
                .strokeBorder(Color(hex: 0x8A6A12).opacity(0.6), lineWidth: 3)
                .padding(2)
            Circle()
                .strokeBorder(style: StrokeStyle(lineWidth: 1, dash: [2, 3]))
                .foregroundStyle(Color(hex: 0x8A6A12).opacity(0.5))
                .padding(8)
            if let heads {
                VStack(spacing: 0) {
                    Image(systemName: heads ? "sun.max.fill" : "moon.stars.fill")
                        .font(.system(size: 22, weight: .semibold))
                    Text(heads ? "正" : "反")
                        .font(.system(size: 16, weight: .bold, design: .serif))
                }
                .foregroundStyle(ink)
            } else {
                Text("?")
                    .font(.system(size: 30, weight: .bold, design: .serif))
                    .foregroundStyle(ink)
            }
        }
        .shadow(color: Color(hex: 0xF2C94C).opacity(0.45), radius: 6)
    }
}

struct CoinWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: WidgetKinds.coin, provider: CoinProvider()) { entry in
            CoinWidgetView(entry: entry)
                .containerBackground(for: .widget) { WidgetCosmicBackground() }
                .widgetURL(URL(string: "xingxia://tool/coin"))
        }
        .configurationDisplayName("抛硬币")
        .description("点一下硬币，正面还是反面。")
        .supportedFamilies([.systemSmall])
    }
}
