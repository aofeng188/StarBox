import SwiftUI
import Combine

struct FortuneDetailView: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase
    @State private var today = DayKey.today
    @State private var offset = 0

    var body: some View {
        let day = today.adding(days: offset)
        let fortune = FortuneEngine.daily(for: app.birth, on: day)
        // 看的不是今天时，文案里的「今日」改成「昨日 / 明日」
        let dayWord = Self.dayWord(offset)
        let moon = MoonInfo.on(day)
        let moonText = "\(dayWord)月相为\(moon.name.name)，亮面约 \(Int((moon.illumination * 100).rounded()))%，月亮行经\(fortune.moonSign.name)。"
        let ownerName = app.primary?.name
        // 分享卡标题：看的不是今天时写具体日期
        let shareTitle = offset == 0 ? "星匣 · 今日运势" : "星匣 · \(DateText.monthDay(day))运势"

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
                        FortuneInsightRow(symbol: moon.name.symbol, text: moonText)
                    }
                    .glassCard()
                } else {
                    FortuneInsightRow(symbol: moon.name.symbol, text: moonText)
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
                ShareCardButton(title: "分享运势", previewTitle: "我的\(DateText.monthDay(day))运势") { style in
                    ShareCardFrame(style: style, day: day, title: shareTitle) {
                        FortuneShareContent(fortune: fortune, name: ownerName, style: style)
                    }
                }
            }
        }
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

/// 运势分享卡的内容，放在 ShareCardFrame 里：分数环、等级、星级、摘要、四项指标和幸运项
struct FortuneShareContent: View {
    let fortune: DailyFortune
    /// 档案名字，「我」或空时不署名
    let name: String?
    let style: ShareCardStyle

    private var signatureText: String? {
        guard let name, !name.isEmpty, name != "我" else { return nil }
        return "—— 来自 \(name) 的分享"
    }

    var body: some View {
        let tall = style.isTall
        let signature = signatureText
        VStack(spacing: tall ? 18 : 14) {
            if tall {
                VStack(spacing: 10) {
                    ShareScoreRing(score: fortune.score, size: 140, lineWidth: 13, label: "运势指数")
                    GradientText(text: fortune.levelName, font: .system(size: 40, weight: .heavy, design: .rounded))
                    StarRatingView(stars: fortune.stars, size: 16)
                }
            } else {
                HStack(spacing: 20) {
                    ShareScoreRing(score: fortune.score, size: 112, lineWidth: 11, label: "运势指数")
                    VStack(alignment: .leading, spacing: 8) {
                        GradientText(text: fortune.levelName, font: .system(size: 36, weight: .heavy, design: .rounded))
                        StarRatingView(stars: fortune.stars, size: 15)
                        if let signature {
                            Text(signature)
                                .font(.system(size: 11))
                                .foregroundStyle(.white.opacity(0.6))
                                .lineLimit(1)
                                .minimumScaleFactor(0.8)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }

            Text(fortune.summary)
                .font(.system(size: tall ? 16 : 14))
                .lineSpacing(4)
                .multilineTextAlignment(.center)
                .foregroundStyle(.white.opacity(0.92))
                .lineLimit(tall ? 4 : 3)
                .minimumScaleFactor(0.85)

            HStack(spacing: 8) {
                ForEach(FortuneCategory.allCases) { category in
                    let value = fortune.score(category)
                    VStack(spacing: 5) {
                        Image(systemName: category.symbol)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(style.palette.accentGradient)
                            .accessibilityHidden(true)
                        Text(category.shortName)
                            .font(.system(size: 11, weight: .medium))
                            .foregroundStyle(.white.opacity(0.7))
                        Text("\(value)")
                            .font(.system(size: 19, weight: .bold, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(.white)
                        ShareMeter(value: value)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 9)
                    .padding(.horizontal, 8)
                    .background(.white.opacity(0.07), in: .rect(cornerRadius: 14))
                    .accessibilityElement(children: .combine)
                }
            }

            VStack(spacing: 8) {
                HStack(spacing: 6) {
                    ShareChip(text: "幸运色 \(fortune.luckyColor.name)", dot: Color(fortune.luckyColor))
                    ShareChip(text: "幸运数字 \(fortune.luckyNumber)")
                    ShareChip(text: "喜神 \(fortune.luckyDirection)")
                }
                if tall {
                    HStack(spacing: 6) {
                        ShareChip(text: "吉时 \(fortune.luckyHour.hourName)")
                        ShareChip(text: "幸运物 \(fortune.luckyItem)")
                        ShareChip(text: "贵人 \(fortune.noblemanSign.name)")
                    }
                }
            }

            if tall, let signature {
                Text(signature)
                    .font(.system(size: 12))
                    .foregroundStyle(.white.opacity(0.6))
                    .lineLimit(1)
            }
        }
    }
}
