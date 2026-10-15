import SwiftUI
import Combine

struct FortuneDetailView: View {
    @Environment(AppState.self) private var app
    @Environment(ThemeManager.self) private var themes
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase
    @State private var today = DayKey.today
    @State private var offset = 0
    @State private var shareImage: Image?

    var body: some View {
        let day = today.adding(days: offset)
        let fortune = FortuneEngine.daily(for: app.birth, on: day)
        // 看的不是今天时，文案里的「今日」改成「昨日 / 明日」
        let dayWord = Self.dayWord(offset)
        let moonPhase = FortuneMoonPhase(Astronomy.moonPhase(day.date))
        let moonText = "\(dayWord)月相为\(moonPhase.name)，月亮行经\(fortune.moonSign.name)。"

        ScrollView {
            VStack(spacing: 16) {
                Picker("日期", selection: $offset.animation(.snappy)) {
                    Text("昨天").tag(-1)
                    Text("今天").tag(0)
                    Text("明天").tag(1)
                }
                .pickerStyle(.segmented)

                // 主卡
                VStack(spacing: 14) {
                    Text("\(DateText.full(day)) \(DateText.weekday(day))")
                        .font(.footnote).foregroundStyle(.secondary)
                    ScoreRing(score: fortune.score, size: 170, lineWidth: 16, label: "运势指数")
                        .id(day)
                    GradientText(text: fortune.levelName, font: .system(size: 44, weight: .heavy, design: .rounded))
                    StarRatingView(stars: fortune.stars, size: 18)
                    Text(fortune.summary)
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                }
                .frame(maxWidth: .infinity)
                .glassCard(padding: 22)

                if !fortune.insights.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeader(title: "运势解读", subtitle: "依据你的八字与\(dayWord)天象", symbol: "scope")
                        ForEach(Array(fortune.insights.enumerated()), id: \.offset) { index, text in
                            FortuneInsightRow(symbol: Self.symbol(for: fortune.insightKinds[safe: index]),
                                              text: dayWord == "今日" ? text : text.replacingOccurrences(of: "今日", with: dayWord))
                        }
                        // 月亮星座那条解读总在最后，月相紧跟其后
                        FortuneInsightRow(symbol: moonPhase.symbol, text: moonText)
                    }
                    .glassCard()
                } else {
                    FortuneInsightRow(symbol: moonPhase.symbol, text: moonText)
                        .glassCard()
                    NavigationLink(value: AppRoute.profiles) {
                        Label("填写生日后可查看基于八字与星座的专属解读", systemImage: "lock.fill")
                            .font(.subheadline)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 6)
                    }
                    .buttonStyle(.glass)
                }

                ForEach(FortuneCategory.allCases) { category in
                    let value = fortune.score(category)
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Image(systemName: category.symbol)
                                .foregroundStyle(palette.onAccent)
                                .frame(width: 30, height: 30)
                                .background(Circle().fill(palette.accentGradient))
                                .accessibilityHidden(true)
                            Text(category.name).font(.headline)
                            StarRatingView(stars: StarRating.stars(for: value), size: 11)
                            Spacer()
                            Text("\(value)")
                                .font(.title3.weight(.bold)).monospacedDigit()
                                .foregroundStyle(palette.accentGradient)
                        }
                        MeterBar(value: value, height: 6).id("\(day)-\(category)")
                        Text(fortune.text(category)).font(.subheadline).foregroundStyle(.primary.opacity(0.85))
                    }
                    .glassCard()
                }

                VStack(alignment: .leading, spacing: 14) {
                    SectionHeader(title: "幸运指南", symbol: "wand.and.stars")
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 14) {
                        LuckyCell(title: "幸运色", value: fortune.luckyColor.name, caption: fortune.luckyColor.hexString) {
                            Circle().fill(Color(fortune.luckyColor)).frame(width: 30, height: 30)
                                .shadow(color: Color(fortune.luckyColor).opacity(0.8), radius: 8)
                        }
                        LuckyCell(title: "幸运数字", value: "\(fortune.luckyNumber)", caption: "五行属\(fortune.favorableElement.name)") {
                            Image(systemName: "number.circle.fill").font(.title).foregroundStyle(palette.accentGradient)
                        }
                        LuckyCell(title: "喜神方位", value: fortune.luckyDirection, caption: "财神 \(fortune.wealthDirection)") {
                            Image(systemName: "location.north.circle.fill").font(.title).foregroundStyle(palette.accentGradient)
                        }
                        LuckyCell(title: "吉时", value: fortune.luckyHour.hourName, caption: fortune.luckyHour.hourRange) {
                            Image(systemName: "clock.fill").font(.title).foregroundStyle(palette.accentGradient)
                        }
                        LuckyCell(title: "幸运物", value: fortune.luckyItem, caption: "带在身边") {
                            Image(systemName: "gift.fill").font(.title).foregroundStyle(palette.accentGradient)
                        }
                        LuckyCell(title: "贵人星座", value: fortune.noblemanSign.name, caption: "\(dayWord)月亮在\(fortune.moonSign.shortName)") {
                            Text(fortune.noblemanSign.glyph).font(.title).foregroundStyle(palette.accentGradient)
                        }
                    }
                }
                .glassCard()

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: "趣味宜忌", symbol: "face.smiling")
                    YiJiRow(isYi: true, items: fortune.funYi)
                    YiJiRow(isYi: false, items: fortune.funJi)
                }
                .glassCard()

                QuoteCard(quote: fortune.quote)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("运势详解")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                if let shareImage {
                    ShareLink(item: shareImage, preview: SharePreview("我的\(DateText.monthDay(day))运势", image: shareImage)) {
                        Image(systemName: "square.and.arrow.up")
                    }
                }
            }
        }
        // 换日期、主题或档案（生日、名字）后重新渲染分享图
        .task(id: ShareImageKey(day: day, themeID: themes.current.id, profile: app.primary)) { renderShareImage(fortune: fortune) }
        // 跨过零点、或者隔天从后台回来时刷新「今天」
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
        .onAppear(perform: refreshToday)
    }

    private func refreshToday() {
        let now = DayKey.today
        if now != today { today = now }
    }

    @MainActor
    private func renderShareImage(fortune: DailyFortune) {
        // ImageRenderer 不一定带上页面的环境值，主题显式传入；分享图是白字，统一用主题的深色版本
        let card = FortuneShareCard(fortune: fortune, name: app.primary?.name, theme: themes.current.dark)
        let renderer = ImageRenderer(content: card)
        renderer.scale = 3
        if let image = renderer.uiImage { shareImage = Image(uiImage: image) }
    }

    private static func dayWord(_ offset: Int) -> String {
        switch offset {
        case 0: "今日"
        case -1: "昨日"
        case 1: "明日"
        default: "当日"
        }
    }

    private static func symbol(for kind: FortuneInsightKind?) -> String {
        switch kind {
        case .stem?: "sun.and.horizon.fill"
        case .branch?: "pawprint.fill"
        case .moon?: "moon.fill"
        case nil: "sparkles"
        }
    }
}

/// 分享图用到的输入，任何一项变了都要重新渲染
private struct ShareImageKey: Equatable, Sendable {
    let day: DayKey
    let themeID: String
    let profile: ProfileSnapshot?
}

private struct FortuneInsightRow: View {
    let symbol: String
    let text: String
    @Environment(\.palette) private var palette

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: symbol)
                .foregroundStyle(palette.accentGradient)
                .frame(width: 22)
                .accessibilityHidden(true)
            Text(text).font(.subheadline).lineSpacing(3)
            Spacer(minLength: 0)
        }
    }
}

/// 八相月相。Astronomy.moonPhase 返回日月黄经差 / 360：0 新月、0.25 上弦、0.5 满月、0.75 下弦，按最近的一相取
private enum FortuneMoonPhase: Int {
    case new, waxingCrescent, firstQuarter, waxingGibbous, full, waningGibbous, lastQuarter, waningCrescent

    init(_ phase: Double) {
        self = FortuneMoonPhase(rawValue: Int((phase * 8).rounded()) % 8) ?? .new
    }

    var name: String {
        ["新月", "娥眉月", "上弦月", "盈凸月", "满月", "亏凸月", "下弦月", "残月"][rawValue]
    }

    var symbol: String {
        ["moonphase.new.moon", "moonphase.waxing.crescent", "moonphase.first.quarter", "moonphase.waxing.gibbous",
         "moonphase.full.moon", "moonphase.waning.gibbous", "moonphase.last.quarter", "moonphase.waning.crescent"][rawValue]
    }
}

private struct LuckyCell<Icon: View>: View {
    let title: String
    let value: String
    let caption: String
    @ViewBuilder var icon: Icon

    var body: some View {
        HStack(spacing: 10) {
            // 图标是装饰，读屏只读文字
            icon.frame(width: 34).accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.caption2).foregroundStyle(.secondary)
                Text(value).font(.subheadline.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.8)
                Text(caption).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(.primary.opacity(0.05), in: .rect(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}

/// 用于分享的运势卡片图片
struct FortuneShareCard: View {
    let fortune: DailyFortune
    let name: String?
    /// 当前主题的深色版本（卡片是深底白字）
    let theme: ThemePalette

    var body: some View {
        VStack(spacing: 16) {
            Text("星匣 · \(DateText.monthDay(fortune.day))运势").font(.headline).foregroundStyle(.white.opacity(0.8))
            Text("\(fortune.day.year)年 · 农历\(LunarDate(fortune.day).text) · \(DateText.weekday(fortune.day))")
                .font(.subheadline).foregroundStyle(.white.opacity(0.6))
            Text("\(fortune.score)")
                .font(.system(size: 88, weight: .heavy, design: .rounded))
                .foregroundStyle(theme.accentGradient)
            Text(fortune.levelName).font(.system(size: 36, weight: .bold)).foregroundStyle(.white)
            Text(fortune.summary).font(.body).foregroundStyle(.white.opacity(0.9)).multilineTextAlignment(.center)
            HStack(spacing: 20) {
                Text("宜 " + fortune.funYi.joined(separator: " ")).foregroundStyle(Color(hex: 0x6EE7B7))
                Text("忌 " + fortune.funJi.prefix(2).joined(separator: " ")).foregroundStyle(Color(hex: 0xFDA4AF))
            }
            .font(.subheadline.weight(.medium))
            HStack(spacing: 6) {
                Circle().fill(Color(fortune.luckyColor)).frame(width: 14, height: 14)
                Text("幸运色 \(fortune.luckyColor.name) · 幸运数字 \(fortune.luckyNumber)")
            }
            .font(.footnote).foregroundStyle(.white.opacity(0.8))
            if let name, name != "我" { Text("—— 来自 \(name) 的分享").font(.caption).foregroundStyle(.white.opacity(0.5)) }
        }
        .padding(32)
        .frame(width: 360)
        .background {
            ZStack {
                theme.backgroundGradient
                Circle().fill(theme.nebula[0].opacity(0.6)).frame(width: 260).blur(radius: 70).offset(x: -100, y: -160)
                Circle().fill(theme.nebula[1].opacity(0.5)).frame(width: 220).blur(radius: 70).offset(x: 120, y: 160)
            }
        }
        .clipShape(.rect(cornerRadius: 32))
    }
}
