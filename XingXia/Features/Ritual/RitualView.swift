import SwiftUI
import SwiftData

/// 每日仪式的五页
enum RitualPage: Int, CaseIterable, Hashable {
    case moon, fortune, stick, mood, summary

    var title: String {
        switch self {
        case .moon: "今天的月亮"
        case .fortune: "运势揭晓"
        case .stick: "今日一签"
        case .mood: "点一下心情"
        case .summary: "今日卡片"
        }
    }
}

/// 每日仪式：今天的月亮 → 运势揭晓 → 今日一签 → 心情 → 今日卡片。
/// 随时可以「跳过」，跳过也记一条 RitualDay（skipped），今天就不再出现；走完或点「去求签」记为完成，计入月相徽章。
struct RitualView: View {
    let day: DayKey
    /// 仪式结束；route 不为 nil 时，关闭后跳到那个页面
    let onFinish: (AppRoute?) -> Void

    @Environment(AppState.self) private var app
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Query private var moods: [MoodEntry]
    @Query private var draws: [StickDraw]

    @State private var page: RitualPage = .moon
    @State private var revealed = false
    @State private var finished = false

    init(day: DayKey, onFinish: @escaping (AppRoute?) -> Void) {
        self.day = day
        self.onFinish = onFinish
        let key = day.rawValue
        _moods = Query(filter: #Predicate<MoodEntry> { $0.dayKey == key }, sort: \MoodEntry.date, order: .reverse)
        _draws = Query(filter: #Predicate<StickDraw> { $0.dayKey == key }, sort: \StickDraw.date, order: .reverse)
    }

    var body: some View {
        let moon = MoonInfo.on(day)
        let fortune = FortuneEngine.daily(for: app.birth, on: day)
        // 只认当前档案的签：给家人朋友求的签记在他们自己的档案下
        let profileID = app.primary?.id
        let stick = draws.first { $0.profileID == profileID }.map { StickLibrary.stick($0.stickNumber) }
        let mood = moods.first.flatMap { MoodLevel(rawValue: $0.mood) }

        NavigationStack {
            VStack(spacing: 0) {
                TabView(selection: $page) {
                    RitualMoonPage(day: day, moon: moon)
                        .tag(RitualPage.moon)
                    RitualFortunePage(fortune: fortune, revealed: revealed)
                        .tag(RitualPage.fortune)
                    RitualStickPage(stick: stick) { finish(skipped: false, then: .stick) }
                        .tag(RitualPage.stick)
                    RitualMoodPage(mood: mood, onSelect: setMood)
                        .tag(RitualPage.mood)
                    RitualSummaryPage(day: day, moon: moon, fortune: fortune, stick: stick, mood: mood)
                        .tag(RitualPage.summary)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))

                bottomBar
            }
            .cosmicBackground()
            .navigationTitle(page.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if page != .summary {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button("跳过") { finish(skipped: true) }
                    }
                }
            }
        }
        .tint(palette.accent)
        .task(id: page) { await revealIfNeeded(fortune) }
    }

    private var bottomBar: some View {
        VStack(spacing: 14) {
            RitualPageDots(current: page)
            BigActionButton(title: page == .summary ? "开始今天" : "继续", symbol: page == .summary ? "sun.max.fill" : nil) {
                advance()
            }
        }
        .frame(maxWidth: 480)
        .padding(.horizontal, 24)
        .padding(.top, 8)
        .padding(.bottom, 12)
    }

    private func advance() {
        guard let next = RitualPage(rawValue: page.rawValue + 1) else {
            finish(skipped: false)
            return
        }
        Haptics.tap()
        withAnimation(reduceMotion ? nil : Animation.snappy) { page = next }
        Announce.post(next.title)
    }

    /// 第一次翻到运势页时揭晓：分数环从 0 转到当天分数，转到头时一声钟响加一记触感
    private func revealIfNeeded(_ fortune: DailyFortune) async {
        guard page == .fortune, !revealed else { return }
        let announcement = "运势指数 \(fortune.score) 分，\(fortune.levelName)"
        if reduceMotion {
            revealed = true
            SoundEngine.shared.play(.chime)
            Haptics.success()
            Announce.post(announcement)
            return
        }
        // 等翻页停稳再转
        try? await Task.sleep(for: .milliseconds(300))
        guard !Task.isCancelled else { return }
        withAnimation(.easeInOut(duration: 1.6)) { revealed = true }
        // Haptics.reveal 由弱到强，0.42 秒后一记重击，正好落在分数环转到头的时候
        try? await Task.sleep(for: .milliseconds(1150))
        guard !Task.isCancelled else { return }
        Haptics.reveal()
        try? await Task.sleep(for: .milliseconds(420))
        guard !Task.isCancelled else { return }
        SoundEngine.shared.play(.chime)
        Announce.post(announcement)
    }

    /// 今天已有心情就更新，没有就新建
    private func setMood(_ level: MoodLevel) {
        if let entry = moods.first {
            entry.mood = level.rawValue
            entry.date = Date()
        } else {
            context.insert(MoodEntry(dayKey: day.rawValue, mood: level.rawValue))
        }
        try? context.save()
    }

    private func finish(skipped: Bool, then route: AppRoute? = nil) {
        guard !finished else { return }
        finished = true
        record(skipped: skipped)
        if !skipped { Haptics.success() }
        onFinish(route)
    }

    /// 每天最多一条 RitualDay；完成过就一直算完成
    private func record(skipped: Bool) {
        let key = day.rawValue
        let descriptor = FetchDescriptor<RitualDay>(predicate: #Predicate<RitualDay> { $0.dayKey == key })
        let existing = (try? context.fetch(descriptor)) ?? []
        if let entry = existing.first {
            if entry.skipped && !skipped {
                entry.skipped = false
                entry.completedAt = Date()
            }
            for extra in existing.dropFirst() { context.delete(extra) }
        } else {
            context.insert(RitualDay(dayKey: key, skipped: skipped, moonPhase: MoonInfo.on(day).name.rawValue))
        }
        try? context.save()
    }
}

// MARK: - 页面骨架

/// 一页的内容竖直居中；大字号、横屏放不下时可以上下滚动
private struct RitualPageScroll<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        GeometryReader { proxy in
            ScrollView {
                VStack(spacing: 20) {
                    content
                }
                .frame(maxWidth: 480)
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
                .frame(maxWidth: .infinity, minHeight: proxy.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
    }
}

private struct RitualPageDots: View {
    let current: RitualPage

    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        HStack(spacing: 8) {
            ForEach(RitualPage.allCases, id: \.self) { page in
                Capsule()
                    .fill(page == current ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(Color.primary.opacity(0.2)))
                    .frame(width: page == current ? 22 : 8, height: 8)
            }
        }
        .animation(reduceMotion ? nil : Animation.snappy, value: current)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("第 \(current.rawValue + 1) 页，共 \(RitualPage.allCases.count) 页")
    }
}

// MARK: - ① 今天的月亮

private struct RitualMoonPage: View {
    let day: DayKey
    let moon: MoonInfo

    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .largeTitle) private var moonSize: CGFloat = 200
    @State private var shown = false

    /// 每一相一句话，按 MoonPhaseName 的顺序
    private static let tips = [
        "月隐无光，夜空最暗，适合许下一个新的心愿。",
        "新月初生，细细一弯，适合开始一件小事。",
        "月亮亮了一半，事情也走到一半，再推一把。",
        "月亮一天比一天圆，适合继续积累、打磨细节。",
        "月正圆，适合收获、感恩，和想念的人说说话。",
        "满月之后慢慢变亏，适合分享、回顾这些天的收获。",
        "月亮只剩一半，适合整理房间，放下不需要的东西。",
        "残月将隐，好好休息，为下一轮蓄力。",
    ]

    var body: some View {
        RitualPageScroll {
            Text("\(DateText.monthDay(day)) \(DateText.weekday(day))")
                .font(.subheadline.weight(.semibold))
                .subtleForeground()

            MoonPhaseView(phase: moon.phase, size: min(moonSize, 260))
                .scaleEffect(shown || reduceMotion ? 1 : 0.86)
                .opacity(shown ? 1 : 0)
                .padding(.vertical, 8)

            VStack(spacing: 6) {
                Text(moon.name.name)
                    .font(AppFont.serif(34, weight: .bold))
                    .foregroundStyle(palette.accentGradient)
                Text("亮面 \(moon.percent)%")
                    .font(.headline)
                    .monospacedDigit()
                    .subtleForeground()
            }
            .accessibilityElement(children: .combine)

            ViewThatFits(in: .horizontal) {
                HStack(spacing: 8) { chips }
                VStack(spacing: 8) { chips }
            }

            Text(Self.tips[moon.name.rawValue])
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.primary.opacity(0.85))
                .fixedSize(horizontal: false, vertical: true)
        }
        .onAppear {
            withAnimation(reduceMotion ? Animation.easeOut(duration: 0.3) : Animation.spring(response: 0.9, dampingFraction: 0.8)) { shown = true }
        }
    }

    @ViewBuilder
    private var chips: some View {
        TagChip(text: "农历\(LunarDate(day).text)", symbol: "calendar")
        TagChip(text: termText, color: palette.accentSecondary, symbol: "leaf.fill")
        TagChip(text: fullMoonText, color: palette.color(at: 2), symbol: "moon.fill")
    }

    private var termText: String {
        if let term = SolarTerms.term(on: day) { return "今天\(term.name)" }
        let next = SolarTerms.next(after: day)
        return "距\(next.name)还有 \(day.days(to: next.day)) 天"
    }

    private var fullMoonText: String {
        let full = SkyEvents.nextFullMoon(after: day.adding(days: -1))
        if full == day { return "今天满月" }
        return "距满月还有 \(day.days(to: full)) 天"
    }
}

// MARK: - ② 运势揭晓

private struct RitualFortunePage: View {
    let fortune: DailyFortune
    let revealed: Bool

    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @ScaledMetric(relativeTo: .largeTitle) private var ringSize: CGFloat = 190
    @ScaledMetric(relativeTo: .largeTitle) private var levelSize: CGFloat = 40

    var body: some View {
        RitualPageScroll {
            Text(fortune.isPersonalized ? "你的专属运势" : "今天的运势")
                .font(.subheadline.weight(.semibold))
                .subtleForeground()

            RitualScoreRing(progress: revealed ? Double(fortune.score) / 100 : 0, size: min(ringSize, 260), palette: palette)
                .accessibilityLabel(revealed ? "运势指数 \(fortune.score) 分" : "运势指数，即将揭晓")

            VStack(spacing: 12) {
                GradientText(text: fortune.levelName, font: .system(size: min(levelSize, 56), weight: .heavy, design: .rounded))
                StarRatingView(stars: fortune.stars, size: 16)
                Text(fortune.summary)
                    .font(.body)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.primary.opacity(0.88))
                    .fixedSize(horizontal: false, vertical: true)
                ViewThatFits(in: .horizontal) {
                    HStack(spacing: 8) { chips }
                    VStack(spacing: 8) { chips }
                }
                .padding(.top, 4)
            }
            .opacity(revealed ? 1 : 0)
            .animation(reduceMotion ? Animation.easeOut(duration: 0.3) : Animation.easeOut(duration: 0.5).delay(revealed ? 1.3 : 0), value: revealed)
        }
    }

    @ViewBuilder
    private var chips: some View {
        ColorDotChip(named: fortune.luckyColor)
        TagChip(text: "幸运数字 \(fortune.luckyNumber)", symbol: "number")
        TagChip(text: "吉时 \(fortune.luckyHour.hourName)", symbol: "clock.fill")
    }
}

/// 从 0 转到当天分数的分数环，环中的数字跟着一起涨
private struct RitualScoreRing: View, Animatable {
    var progress: Double
    let size: CGFloat
    let palette: ThemePalette

    var animatableData: Double {
        get { progress }
        set { progress = newValue }
    }

    var body: some View {
        let lineWidth = size * 0.08
        ZStack {
            Circle()
                .stroke(.primary.opacity(0.08), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(AngularGradient(colors: [palette.accent, palette.accentSecondary, palette.nebula[0], palette.accent],
                                        center: .center),
                        style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .shadow(color: palette.accent.opacity(0.6), radius: lineWidth * 0.8)
            VStack(spacing: 2) {
                Text("\(Int((progress * 100).rounded()))")
                    .font(.system(size: size * 0.32, weight: .bold, design: .rounded))
                    .monospacedDigit()
                Text("运势指数")
                    .font(.system(size: size * 0.09, weight: .semibold))
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: size, height: size)
        .accessibilityElement(children: .ignore)
    }
}

// MARK: - ③ 今日一签

private struct RitualStickPage: View {
    let stick: FortuneStick?
    let onDraw: () -> Void

    @Environment(\.colorScheme) private var scheme

    var body: some View {
        RitualPageScroll {
            if let stick {
                VStack(spacing: 14) {
                    HStack(spacing: 10) {
                        Text(stick.numberText)
                            .font(AppFont.serif(28, weight: .bold))
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                        Text(stick.levelInfo.displayName)
                            .font(.subheadline.weight(.bold))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(stick.levelInfo.color.opacity(0.25), in: .capsule)
                            .foregroundStyle(stick.levelInfo.ink(scheme))
                    }
                    Text("「\(stick.title)」")
                        .font(AppFont.serif(22))
                        .subtleForeground()
                    VStack(spacing: 8) {
                        ForEach(Array(stick.poem.enumerated()), id: \.offset) { _, line in
                            Text(line).font(AppFont.serif(19))
                        }
                    }
                    .padding(.vertical, 6)
                    Text(stick.meaning)
                        .font(.subheadline)
                        .subtleForeground()
                        .fixedSize(horizontal: false, vertical: true)
                }
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
                .glassCard(padding: 22)
                .accessibilityElement(children: .combine)

                Text("今天已经求过签了，首页的「今日一签」随时能再看。")
                    .font(.footnote)
                    .multilineTextAlignment(.center)
                    .subtleForeground()
            } else {
                ZStack {
                    RoundedRectangle(cornerRadius: 24, style: .continuous)
                        .fill(LinearGradient(colors: [Color(hex: 0xB91C1C), Color(hex: 0x7F1D1D)], startPoint: .top, endPoint: .bottom))
                    Image(systemName: "flame.fill")
                        .font(.system(size: 44))
                        .foregroundStyle(LinearGradient(colors: [Color(hex: 0xFFD166), Color(hex: 0xFF8A3D)], startPoint: .top, endPoint: .bottom))
                }
                .frame(width: 100, height: 116)
                .shadow(color: Color(hex: 0xE11D48).opacity(0.5), radius: 12)
                .accessibilityHidden(true)

                VStack(spacing: 8) {
                    Text("今天还没有求签")
                        .font(.title2.bold())
                    Text("心诚则灵，摇一摇签筒，看看今天的指引。")
                        .font(.body)
                        .multilineTextAlignment(.center)
                        .subtleForeground()
                }

                Button(action: onDraw) {
                    Label("去求签", systemImage: "flame.fill")
                        .font(.headline)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 4)
                }
                .buttonStyle(.glass)
                .controlSize(.large)

                Text("也可以先继续，之后在首页或「问一问」里随时求签。")
                    .font(.footnote)
                    .multilineTextAlignment(.center)
                    .subtleForeground()
            }
        }
    }
}

// MARK: - ④ 心情

private struct RitualMoodPage: View {
    let mood: MoodLevel?
    let onSelect: (MoodLevel) -> Void

    var body: some View {
        RitualPageScroll {
            Text(mood?.emoji ?? "🌙")
                .font(.system(size: 72))
                .contentTransition(.opacity)
                .accessibilityHidden(true)

            VStack(spacing: 8) {
                Text("今天心情怎么样？")
                    .font(.title2.bold())
                Text("点一下就好，之后还能改。")
                    .font(.subheadline)
                    .subtleForeground()
            }
            .multilineTextAlignment(.center)

            MoodPicker(selected: mood?.rawValue, size: 54, onSelect: onSelect)
                .glassCard(padding: 14)

            if let mood {
                Text("已记下「\(mood.name)」。想多写几句，可以去「日记」里的心情页。")
                    .font(.footnote)
                    .multilineTextAlignment(.center)
                    .subtleForeground()
            }
        }
        .animation(.easeOut(duration: 0.25), value: mood)
    }
}

// MARK: - ⑤ 今日卡片

private struct RitualSummaryPage: View {
    let day: DayKey
    let moon: MoonInfo
    let fortune: DailyFortune
    let stick: FortuneStick?
    let mood: MoodLevel?

    @Environment(\.palette) private var palette

    var body: some View {
        RitualPageScroll {
            VStack(alignment: .leading, spacing: 16) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(DateText.monthDay(day)) \(DateText.weekday(day))")
                        .font(.title3.bold())
                    Text("农历\(LunarDate(day).text)")
                        .font(.subheadline)
                        .subtleForeground()
                }
                .accessibilityElement(children: .combine)

                Divider().opacity(0.4)

                RitualSummaryRow(title: "月亮", value: "\(moon.name.name) · 亮面 \(moon.percent)%") {
                    MoonPhaseView(phase: moon.phase, size: 34, glow: false)
                }
                RitualSummaryRow(title: "运势", value: "\(fortune.score) 分 · \(fortune.levelName)") {
                    Image(systemName: "sparkles")
                        .font(.title2)
                        .foregroundStyle(palette.accentGradient)
                }
                RitualSummaryRow(title: "今日一签", value: stick.map { "\($0.numberText) · \($0.levelInfo.displayName) · 「\($0.title)」" } ?? "还没有求签") {
                    Image(systemName: "flame.fill")
                        .font(.title2)
                        .foregroundStyle(LinearGradient(colors: [Color(hex: 0xFF8A3D), Color(hex: 0xE11D48)], startPoint: .top, endPoint: .bottom))
                }
                RitualSummaryRow(title: "心情", value: mood.map { "\($0.emoji) \($0.name)" } ?? "还没有记录") {
                    Text(mood?.emoji ?? "🙂")
                        .font(.title)
                        .grayscale(mood == nil ? 1 : 0)
                        .opacity(mood == nil ? 0.45 : 1)
                }

                Divider().opacity(0.4)

                Text(fortune.quote)
                    .font(AppFont.serif(17))
                    .lineSpacing(4)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .glassCard()

            ShareCardButton(title: "分享今日卡片", previewTitle: "星匣 · 今日") { style in
                ShareCardFrame(style: style, day: day, title: "星匣 · 今日") {
                    RitualShareContent(moon: moon, fortune: fortune, stick: stick, mood: mood, style: style)
                }
            }
            .buttonStyle(.glass)
            .controlSize(.large)
        }
    }
}

private struct RitualSummaryRow<Icon: View>: View {
    let title: String
    let value: String
    @ViewBuilder var icon: Icon

    var body: some View {
        HStack(spacing: 14) {
            icon
                .frame(width: 40, height: 40)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption)
                    .subtleForeground()
                Text(value)
                    .font(.body.weight(.semibold))
                    .fixedSize(horizontal: false, vertical: true)
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
    }
}

/// 「星匣 · 今日」分享卡的内容：月相、运势、签和心情。字号固定，导出的图每次一样
struct RitualShareContent: View {
    let moon: MoonInfo
    let fortune: DailyFortune
    let stick: FortuneStick?
    let mood: MoodLevel?
    let style: ShareCardStyle

    var body: some View {
        let tall = style.isTall
        VStack(spacing: tall ? 20 : 14) {
            HStack(spacing: 18) {
                MoonPhaseView(phase: moon.phase, size: tall ? 104 : 84)
                VStack(alignment: .leading, spacing: 6) {
                    Text(moon.name.name)
                        .font(AppFont.serif(tall ? 30 : 26, weight: .bold))
                        .foregroundStyle(.white)
                    Text("今晚亮面 \(moon.percent)%")
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.white.opacity(0.7))
                }
                Spacer(minLength: 0)
            }
            .accessibilityElement(children: .combine)

            HStack(spacing: 18) {
                ShareScoreRing(score: fortune.score, size: tall ? 104 : 84, lineWidth: tall ? 10 : 8, label: "运势")
                VStack(alignment: .leading, spacing: 6) {
                    GradientText(text: fortune.levelName, font: .system(size: tall ? 32 : 28, weight: .heavy, design: .rounded))
                    StarRatingView(stars: fortune.stars, size: 13)
                }
                Spacer(minLength: 0)
            }

            if tall {
                Text(fortune.summary)
                    .font(.system(size: 15))
                    .lineSpacing(4)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.white.opacity(0.9))
                    .lineLimit(3)
                    .minimumScaleFactor(0.85)
            }

            if stick != nil || mood != nil {
                VStack(spacing: 8) {
                    if let stick {
                        ShareChip(text: "今日一签 · \(stick.numberText) · \(stick.levelInfo.sealText) · \(stick.title)", dot: stick.levelInfo.color)
                    }
                    if let mood {
                        ShareChip(text: "心情 \(mood.emoji) \(mood.name)", dot: mood.color)
                    }
                }
            }

            Text(fortune.quote)
                .font(AppFont.serif(tall ? 16 : 14))
                .lineSpacing(3)
                .multilineTextAlignment(.center)
                .foregroundStyle(.white.opacity(0.85))
                .lineLimit(tall ? 3 : 2)
                .minimumScaleFactor(0.85)
        }
    }
}
