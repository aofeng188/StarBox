import SwiftUI

struct DivinationHubView: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @State private var isWide = false
    @State private var fold: ClosedRange<CGFloat>?

    var body: some View {
        ScrollView {
            // 宽屏（Duo 展开横持）左右两栏，其余上下排
            ColumnsLayout(columns: isWide ? 2 : 1, spacing: 14, fold: fold) {
                VStack(spacing: 14) {
                    NavigationLink(value: AppRoute.stick) {
                        HeroCard(title: "摇签筒", subtitle: "心诚则灵 · 一日一签", detail: "默念心中所求，摇动手机，抽出今天的指引",
                                 colors: [Color(hex: 0xB91C1C), Color(hex: 0xF59E0B)]) {
                            StickTubeIllustration(size: 96)
                        }
                    }
                    .buttonStyle(.pressableCard)

                    HStack(spacing: 14) {
                        NavigationLink(value: AppRoute.tarot) {
                            SmallHeroCard(title: "塔罗占卜", subtitle: "大阿卡纳 · 四种牌阵", colors: [Color(hex: 0x6D28D9), Color(hex: 0x2563EB)]) {
                                TarotBackMini()
                            }
                        }
                        .buttonStyle(.pressableCard)
                        NavigationLink(value: AppRoute.answers) {
                            SmallHeroCard(title: "答案之书", subtitle: "默念问题 · 翻开一页", colors: [Color(hex: 0xD97706), Color(hex: 0x92400E)]) {
                                Image(systemName: "book.closed.fill")
                                    .font(.system(size: 44))
                                    .foregroundStyle(LinearGradient(colors: [Color(hex: 0xF5B83D), Color(hex: 0xB45309)], startPoint: .top, endPoint: .bottom))
                                    .shadow(color: Color(hex: 0xF5B83D).opacity(0.7), radius: 12)
                                    .accessibilityHidden(true)
                            }
                        }
                        .buttonStyle(.pressableCard)
                    }
                }

                VStack(spacing: 14) {
                    NavigationLink(value: AppRoute.horoscope) {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                SectionHeader(title: "星座运势", subtitle: "今日 · 明日 · 本周 · 本月", symbol: "moon.stars.fill")
                                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                            }
                            // 12 星座两行排开，只作装饰：整张卡是一个入口
                            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 4), count: 6), spacing: 10) {
                                ForEach(ZodiacSign.allCases) { sign in
                                    VStack(spacing: 4) {
                                        ZodiacBadge(sign: sign, size: 38)
                                        Text(sign.shortName)
                                            .font(.caption2).foregroundStyle(.secondary)
                                            .lineLimit(1).minimumScaleFactor(0.6)
                                    }
                                }
                            }
                            // 纯装饰，超大字号下名字挤不进 6 列，字号封顶
                            .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
                            .accessibilityHidden(true)
                        }
                        .glassCard(interactive: true)
                    }
                    .buttonStyle(.pressableCard)
                    .accessibilityLabel("星座运势")
                    .accessibilityHint("查看十二星座的今日、明日、本周、本月运势")

                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)], spacing: 14) {
                        MiniTile(route: .fortune, symbol: "sparkles", title: "运势详解", subtitle: "五维运势与幸运指南", colors: [palette.accent, palette.accentSecondary], symbolColor: palette.onAccent)
                        MiniTile(route: .almanac, symbol: "calendar.badge.clock", title: "黄历", subtitle: "农历 · 节气 · 宜忌", colors: [Color(hex: 0xEF4444), Color(hex: 0xF59E0B)])
                        MiniTile(route: .birthChart, symbol: "circle.hexagongrid.fill", title: "命盘", subtitle: "八字 · 星盘三巨头", colors: [Color(hex: 0xA78BFA), Color(hex: 0xEC4899)])
                        MiniTile(route: .compatibility, symbol: "heart.circle.fill", title: "缘分配对", subtitle: "星座 + 生肖", colors: [Color(hex: 0xFB7185), Color(hex: 0xF43F5E)])
                    }
                }
            }
            .foldGap($fold)
            .isWideLayout($isWide)
            .pageContent(maxWidth: PageMetrics.gridWidth)
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("占卜")
    }
}

/// 大号入口卡片
struct HeroCard<Art: View>: View {
    let title: String
    let subtitle: String
    let detail: String
    let colors: [Color]
    @ViewBuilder var art: Art

    @Environment(\.palette) private var palette

    var body: some View {
        HStack(spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                // 浅色模式下把卡片主色压暗，免得橙字融进浅粉底
                Text(subtitle).font(.caption.weight(.semibold)).foregroundStyle(palette.ink(colors[1]))
                Text(title).font(.title.bold())
                Text(detail).font(.subheadline).foregroundStyle(.secondary).fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
            art
        }
        .padding(.vertical, 6)
        .glassCard(tint: colors[0].opacity(0.12), interactive: true)
        .overlay(alignment: .topTrailing) {
            Circle().fill(colors[1].opacity(0.35)).frame(width: 120).blur(radius: 40).offset(x: 20, y: -30).allowsHitTesting(false)
        }
        .clipShape(.rect(cornerRadius: 28))
    }
}

struct SmallHeroCard<Art: View>: View {
    let title: String
    let subtitle: String
    let colors: [Color]
    @ViewBuilder var art: Art

    var body: some View {
        VStack(spacing: 12) {
            art.frame(height: 86)
            VStack(spacing: 3) {
                Text(title).font(.headline)
                Text(subtitle).font(.caption2).foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .glassCard(tint: colors[0].opacity(0.14), interactive: true)
        .overlay(alignment: .top) {
            Circle().fill(colors[0].opacity(0.4)).frame(width: 110).blur(radius: 40).offset(y: -20).allowsHitTesting(false)
        }
        .clipShape(.rect(cornerRadius: 28))
    }
}

struct MiniTile: View {
    let route: AppRoute
    let symbol: String
    let title: String
    let subtitle: String
    let colors: [Color]
    /// 图标颜色。底色用主题强调色时传 palette.onAccent，深色主题的强调色偏亮，白色图标看不清
    var symbolColor: Color = .white

    var body: some View {
        NavigationLink(value: route) {
            VStack(alignment: .leading, spacing: 10) {
                MiniTileIcon(symbol: symbol, colors: colors, foreground: symbolColor)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).font(.headline).foregroundStyle(.primary)
                    Text(subtitle).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
                }
            }
            .glassCard(padding: 16, interactive: true)
        }
        .buttonStyle(.pressableCard)
    }
}

/// 和 ToolIcon 同款的渐变方块图标，但图标颜色可调
private struct MiniTileIcon: View {
    let symbol: String
    let colors: [Color]
    let foreground: Color
    private let size: CGFloat = 40

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.3, style: .continuous)
                .fill(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
                .shadow(color: colors.first?.opacity(0.5) ?? .clear, radius: 8, y: 3)
            RoundedRectangle(cornerRadius: size * 0.3, style: .continuous)
                .strokeBorder(.white.opacity(0.35), lineWidth: 0.8)
            Image(systemName: symbol)
                .font(.system(size: size * 0.42, weight: .semibold))
                .foregroundStyle(foreground)
                .shadow(color: .black.opacity(0.15), radius: 2, y: 1)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// 签筒插画
struct StickTubeIllustration: View {
    var size: CGFloat = 96
    var shake: Double = 0

    var body: some View {
        ZStack(alignment: .bottom) {
            // 签子
            ForEach(0..<7, id: \.self) { i in
                RoundedRectangle(cornerRadius: 2)
                    .fill(LinearGradient(colors: [Color(hex: 0xF5DEB3), Color(hex: 0xD2A86E)], startPoint: .top, endPoint: .bottom))
                    .overlay(alignment: .top) {
                        RoundedRectangle(cornerRadius: 2).fill(Color(hex: 0xC0392B)).frame(height: size * 0.08)
                    }
                    .frame(width: size * 0.07, height: size * 0.75)
                    .rotationEffect(.degrees(Double(i - 3) * 6 + sin(shake * 3 + Double(i)) * 4), anchor: .bottom)
                    .offset(x: CGFloat(i - 3) * size * 0.05, y: -size * 0.32 + CGFloat(abs(i - 3)) * size * 0.02 - CGFloat(sin(shake * 5 + Double(i))) * size * 0.04)
            }
            // 筒身
            UnevenRoundedRectangle(topLeadingRadius: size * 0.06, bottomLeadingRadius: size * 0.12, bottomTrailingRadius: size * 0.12, topTrailingRadius: size * 0.06)
                .fill(LinearGradient(colors: [Color(hex: 0xC0392B), Color(hex: 0x7B1E1E)], startPoint: .leading, endPoint: .trailing))
                .overlay {
                    VStack(spacing: size * 0.06) {
                        Capsule().fill(Color(hex: 0xFFD166)).frame(height: size * 0.025)
                        Text("签")
                            .font(.system(size: size * 0.22, weight: .heavy, design: .serif))
                            .foregroundStyle(Color(hex: 0xFFD166))
                        Capsule().fill(Color(hex: 0xFFD166)).frame(height: size * 0.025)
                    }
                    .padding(.horizontal, size * 0.06)
                }
                .frame(width: size * 0.5, height: size * 0.62)
                .shadow(color: Color(hex: 0xE11D48).opacity(0.5), radius: 10, y: 4)
        }
        .frame(width: size, height: size * 1.05)
        .accessibilityHidden(true)
    }
}

/// 塔罗牌背面迷你插画
struct TarotBackMini: View {
    var body: some View {
        ZStack {
            ForEach(0..<3, id: \.self) { i in
                TarotCardBack()
                    .frame(width: 46, height: 74)
                    .rotationEffect(.degrees(Double(i - 1) * 14), anchor: .bottom)
                    .offset(x: CGFloat(i - 1) * 14)
            }
        }
    }
}
