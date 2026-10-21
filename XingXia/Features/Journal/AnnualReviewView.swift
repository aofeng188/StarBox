import SwiftUI
import SwiftData
import Charts

/// 年度回顾：年度关键词、做了多少决定和照做比例、满意率、最常吃的菜、心情曲线、最幸运的一天、
/// 求签等级分布、最常用的工具，底部可以分享成图。统计规则见 JournalStats
struct AnnualReviewView: View {
    let year: Int

    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme
    @Environment(\.dynamicTypeSize) private var typeSize
    @Query private var records: [DecisionRecord]
    @Query private var moods: [MoodEntry]
    @Query private var draws: [StickDraw]
    @Query private var readings: [TarotReading]
    @Query private var foods: [FoodLog]

    /// 算好的回顾；输入变了（新记录、补记、换档案）就重算
    @State private var review: AnnualReview?
    /// 年份大字跟随系统字号放大（设上限）
    @ScaledMetric(relativeTo: .largeTitle) private var yearSize: CGFloat = 56
    /// 「最幸运的一天」的分数环跟着字号放大（封顶 1.6 倍）
    @ScaledMetric(relativeTo: .title) private var luckyRingSize: CGFloat = 76

    init(year: Int) {
        self.year = year
        let start = DayKey(year: year, month: 1, day: 1).startOfDay
        let end = DayKey(year: year + 1, month: 1, day: 1).startOfDay
        // yyyy0000 < dayKey < (yyyy+1)0000：这一年的每一天
        let low = year * 10000
        let high = (year + 1) * 10000
        _records = Query(filter: #Predicate<DecisionRecord> { record in
            record.date >= start && record.date < end
        }, sort: \DecisionRecord.date)
        _moods = Query(filter: #Predicate<MoodEntry> { entry in
            entry.dayKey > low && entry.dayKey < high
        }, sort: \MoodEntry.dayKey)
        _draws = Query(filter: #Predicate<StickDraw> { draw in
            draw.dayKey > low && draw.dayKey < high
        }, sort: \StickDraw.date)
        _readings = Query(filter: #Predicate<TarotReading> { reading in
            reading.date >= start && reading.date < end
        }, sort: \TarotReading.date)
        _foods = Query(filter: #Predicate<FoodLog> { log in
            log.date >= start && log.date < end
        }, sort: \FoodLog.date)
    }

    private var inputKey: InputKey {
        var marks = Hasher()
        for record in records {
            marks.combine(record.adopted)
            marks.combine(record.satisfaction)
        }
        return InputKey(year: year, birth: app.birth,
                        counts: [records.count, moods.count, draws.count, readings.count, foods.count],
                        marks: marks.finalize())
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if let review {
                    if review.isEmpty {
                        EmptyHint(symbol: "chart.bar.xaxis", title: "\(String(year)) 年还没有记录",
                                  message: "点心情、做决定、求签、抽塔罗，这一年的点点滴滴都会汇总在这里")
                            .glassCard()
                    } else {
                        content(review)
                    }
                } else {
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 60)
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("\(String(year)) 年度回顾")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if let review, !review.isEmpty {
                ToolbarItem(placement: .topBarTrailing) { shareButton(review) }
            }
        }
        .task(id: inputKey) { await compute() }
    }

    // MARK: - 内容

    @ViewBuilder
    private func content(_ review: AnnualReview) -> some View {
        hero(review)
        keywordsCard(review)
        decisionsCard(review)
        if !review.topTools.isEmpty { toolsCard(review) }
        foodsCard(review)
        moodCard(review)
        if let lucky = review.luckiestDay { luckiestCard(lucky) }
        if review.stickCount > 0 { sticksCard(review) }

        // 不在这里加 buttonStyle / font：会被分享卡的弹窗继承，弹窗里的按钮跟着变样
        shareButton(review)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
    }

    /// 分享卡：3:4 和 9:16 都排得下，9:16 多放心情曲线、求签和最常用的工具
    private func shareButton(_ review: AnnualReview) -> some View {
        ShareCardButton(title: "分享年度回顾", previewTitle: "我的 \(String(year)) 星匣年度回顾", initialRatio: .story) { style in
            ShareCardFrame(style: style, day: shareDay, title: "星匣 · \(String(year)) 年度回顾") {
                AnnualShareContent(review: review, style: style)
            }
        }
    }

    /// 分享卡页脚的日期：回顾今年时用今天，回顾往年用那年的最后一天
    private var shareDay: DayKey {
        let today = DayKey.today
        return year >= today.year ? today : DayKey(year: year, month: 12, day: 31)
    }

    private func hero(_ review: AnnualReview) -> some View {
        VStack(spacing: 6) {
            Text(String(year))
                .font(.system(size: min(yearSize, 84), weight: .heavy, design: .rounded))
                .foregroundStyle(palette.accentGradient)
                .shadow(color: palette.accent.opacity(0.35), radius: 10)
            Text("年度回顾").font(.title3.weight(.semibold))
            Text("这一年你在星匣里留下了 \(review.recordDays) 天的记录")
                .font(.subheadline)
                .subtleForeground()
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
        .glassCard(padding: 22)
    }

    private func keywordsCard(_ review: AnnualReview) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "年度关键词", subtitle: "来自你问过的问题和得到的结果", symbol: "tag.fill")
            if review.keywords.isEmpty {
                Text("问过的问题还不够多，多用几次「问一问」就能看到关键词")
                    .font(.subheadline)
                    .subtleForeground()
            } else {
                FlowLayout(spacing: 8) {
                    ForEach(Array(review.keywords.enumerated()), id: \.element.id) { index, keyword in
                        Text("#\(keyword.name)")
                            .font(index == 0 ? .title3.weight(.bold) : .headline)
                            .foregroundStyle(palette.ink(palette.accent))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(palette.accent.opacity(index == 0 ? 0.22 : 0.13), in: .capsule)
                            .accessibilityLabel("\(keyword.name)，\(keyword.count) 次")
                    }
                }
            }
        }
        .glassCard()
    }

    private func decisionsCard(_ review: AnnualReview) -> some View {
        let tally = review.tally
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "听星匣的话", symbol: "checkmark.seal.fill")
            // 超大字号时三格竖排
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 10) { decisionStats(tally) }
                VStack(spacing: 10) { decisionStats(tally) }
            }
            if tally.decisions > 0 && tally.rated == 0 {
                Text("做完决定点「就这么办」，第二天首页会来问你后来怎么样")
                    .font(.caption)
                    .subtleForeground()
            }
        }
        .glassCard()
    }

    @ViewBuilder
    private func decisionStats(_ tally: DecisionTally) -> some View {
        bigStat(value: "\(tally.decisions)", unit: "个", label: "做了决定")
        bigStat(value: tally.adoptionRate.map { "\($0)" } ?? "—", unit: tally.adoptionRate == nil ? "" : "%",
                label: "照做 \(tally.adopted) 个")
        bigStat(value: tally.satisfactionRate.map { "\($0)" } ?? "—", unit: tally.satisfactionRate == nil ? "" : "%",
                label: tally.rated > 0 ? "满意率 · 回访 \(tally.rated) 次" : "满意率")
    }

    private func bigStat(value: String, unit: String, label: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 2) {
                Text(value)
                    .font(.system(.title, design: .rounded).weight(.bold))
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text(unit).font(.caption).subtleForeground()
            }
            Text(label).font(.caption).subtleForeground().fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.primary.opacity(0.05), in: .rect(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }

    private func toolsCard(_ review: AnnualReview) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "最常用的工具", symbol: "star.fill")
            ForEach(Array(review.topTools.enumerated()), id: \.element.id) { index, tool in
                let info = HistoryLog.info(for: tool.name)
                HStack(spacing: 12) {
                    Text("\(index + 1)")
                        .font(.headline.monospacedDigit())
                        .foregroundStyle(palette.ink(palette.accent))
                        .frame(width: 20)
                    ToolIcon(symbol: info.symbol, colors: palette.identity(info.colors), size: 34)
                    Text(info.name).font(.subheadline.weight(.semibold))
                    Spacer(minLength: 0)
                    Text("\(tool.count) 次").font(.subheadline).subtleForeground().monospacedDigit()
                }
                .accessibilityElement(children: .combine)
            }
        }
        .glassCard()
    }

    private func foodsCard(_ review: AnnualReview) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "最常吃的", symbol: "fork.knife")
            if review.topFoods.isEmpty {
                Text("在「今天吃什么」里点「就吃这个」，这里就会记下你最爱的菜")
                    .font(.subheadline)
                    .subtleForeground()
            } else {
                ForEach(Array(review.topFoods.enumerated()), id: \.element.id) { index, food in
                    HStack(spacing: 12) {
                        Text(["🥇", "🥈", "🥉"][min(index, 2)]).font(.title3).accessibilityHidden(true)
                        Text(food.name).font(index == 0 ? .headline : .subheadline.weight(.semibold))
                        Spacer(minLength: 0)
                        Text("\(food.count) 次").font(.subheadline).subtleForeground().monospacedDigit()
                    }
                    .accessibilityElement(children: .combine)
                }
            }
        }
        .glassCard()
    }

    private func moodCard(_ review: AnnualReview) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "心情曲线",
                          subtitle: review.moodAverage.map { String(format: "打卡 %ld 天 · 平均 %.1f", review.moodDays, $0) },
                          symbol: "waveform.path.ecg")
            if review.moodByMonth.isEmpty {
                Text("这一年还没有记过心情")
                    .font(.subheadline)
                    .subtleForeground()
            } else {
                Chart(review.moodByMonth) { item in
                    LineMark(x: .value("月份", item.month), y: .value("平均心情", item.average))
                        .interpolationMethod(.monotone)
                        .foregroundStyle(palette.accentGradient)
                        .lineStyle(StrokeStyle(lineWidth: 3, lineCap: .round))
                    PointMark(x: .value("月份", item.month), y: .value("平均心情", item.average))
                        .foregroundStyle(MoodLevel(rawValue: Int(item.average.rounded()))?.color ?? palette.accent)
                        .symbolSize(60)
                }
                .chartXScale(domain: 1...12)
                .chartYScale(domain: 1.0...5.0)
                .chartXAxis {
                    AxisMarks(values: [1, 3, 5, 7, 9, 11]) { value in
                        AxisGridLine()
                        AxisValueLabel {
                            if let month = value.as(Int.self) { Text("\(month)月") }
                        }
                    }
                }
                .chartYAxis {
                    AxisMarks(position: .leading, values: [1.0, 3.0, 5.0]) { value in
                        AxisGridLine()
                        AxisValueLabel {
                            if let level = value.as(Double.self).flatMap({ MoodLevel(rawValue: Int($0.rounded())) }) {
                                Text(level.emoji)
                            }
                        }
                    }
                }
                .frame(height: 170)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(moodSummary(review))
            }
        }
        .glassCard()
    }

    private func moodSummary(_ review: AnnualReview) -> String {
        let parts = review.moodByMonth.map { item in
            "\(item.month)月\(MoodLevel(rawValue: Int(item.average.rounded()))?.name ?? "")"
        }
        return "每月平均心情：" + parts.joined(separator: "，")
    }

    private func luckiestCard(_ lucky: DayScore) -> some View {
        let raw = lucky.day.rawValue
        let mood = moods.first { $0.dayKey == raw }.flatMap { MoodLevel(rawValue: $0.mood) }
        let decisions = records.filter { DayKey($0.date) == lucky.day && JournalStats.isDecision($0.tool) }.count
        let stick = draws.first { $0.dayKey == raw }.map { StickLibrary.stick($0.stickNumber) }
        var notes: [String] = []
        if let mood { notes.append("心情\(mood.name) \(mood.emoji)") }
        if decisions > 0 { notes.append("做了 \(decisions) 个决定") }
        if let stick { notes.append("求得\(stick.levelInfo.displayName)") }
        let ring = min(luckyRingSize, 76 * 1.6)
        // 超大字号时分数环放上面，日期和那天的记录占整行
        let layout = typeSize.isAccessibilitySize ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12))
                                                  : AnyLayout(HStackLayout(spacing: 16))
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "最幸运的一天", symbol: "sparkles")
            layout {
                ScoreRing(score: lucky.score, size: ring, lineWidth: ring * 8 / 76, label: "运势")
                VStack(alignment: .leading, spacing: 4) {
                    Text(DateText.monthDay(lucky.day)).font(.title3.weight(.bold))
                    Text("\(DateText.weekday(lucky.day)) · 农历\(LunarDate(lucky.day).text)").font(.caption).subtleForeground()
                    if !notes.isEmpty {
                        Text("那天你" + notes.joined(separator: "，"))
                            .font(.subheadline)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                Spacer(minLength: 0)
            }
            .accessibilityElement(children: .combine)
        }
        .glassCard()
    }

    private func sticksCard(_ review: AnnualReview) -> some View {
        let maxCount = max(review.stickLevels.map(\.count).max() ?? 1, 1)
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "求签等级", subtitle: "共 \(review.stickCount) 支", symbol: "flame.fill")
            ForEach(review.stickLevels) { item in
                let level = StickLevel(rawValue: item.name) ?? .middle
                HStack(spacing: 10) {
                    Text(level.displayName)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(level.ink(scheme))
                        .frame(minWidth: 48, alignment: .leading)
                    MeterBar(value: item.count * 100 / maxCount, color: level.color, height: 8)
                    Text("\(item.count)")
                        .font(.subheadline.monospacedDigit())
                        .subtleForeground()
                        .frame(minWidth: 24, alignment: .trailing)
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(level.displayName) \(item.count) 支")
            }
        }
        .glassCard()
    }

    // MARK: - 计算

    private func compute() async {
        let birth = app.birth
        let input = AnnualInput(
            year: year,
            decisions: records.map { DecisionSample($0) },
            moods: moods.map { MoodSample(dayKey: $0.dayKey, mood: $0.mood) },
            sticks: draws.map { (dayKey: $0.dayKey, level: StickLibrary.stick($0.stickNumber).level) },
            tarotDays: readings.map { DayKey($0.date).rawValue },
            foods: foods.map(\.name))
        // 一年的运势分数要算几百天，放到后台
        let review = await Task.detached(priority: .userInitiated) {
            JournalStats.annual(input) { FortuneEngine.daily(for: birth, on: $0).score }
        }.value
        guard !Task.isCancelled else { return }
        self.review = review
    }
}

private struct InputKey: Hashable {
    let year: Int
    let birth: BirthInfo?
    let counts: [Int]
    /// 照做和回访的变化也要重算
    let marks: Int
}

// MARK: - 分享卡

/// 年度回顾分享卡：年份、关键词、决定三连、最常吃、最幸运的一天；9:16 再加心情曲线和求签
struct AnnualShareContent: View {
    let review: AnnualReview
    let style: ShareCardStyle

    var body: some View {
        let tall = style.isTall
        let palette = style.palette
        let tally = review.tally
        VStack(spacing: tall ? 14 : 10) {
            VStack(spacing: 2) {
                Text(String(review.year))
                    .font(.system(size: tall ? 64 : 50, weight: .heavy, design: .rounded))
                    .foregroundStyle(palette.accentGradient)
                    .shadow(color: palette.accent.opacity(0.4), radius: 12)
                Text("这一年记录了 \(review.recordDays) 天")
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(.white.opacity(0.7))
            }

            if !review.keywords.isEmpty {
                VStack(spacing: 8) {
                    Text("年度关键词")
                        .font(.system(size: 11, weight: .semibold))
                        .tracking(2)
                        .foregroundStyle(.white.opacity(0.6))
                    HStack(spacing: 6) {
                        ForEach(review.keywords.prefix(3)) { keyword in
                            ShareChip(text: "#" + keyword.name)
                        }
                    }
                    if tall && review.keywords.count > 3 {
                        HStack(spacing: 6) {
                            ForEach(review.keywords.dropFirst(3).prefix(2)) { keyword in
                                ShareChip(text: "#" + keyword.name)
                            }
                        }
                    }
                }
            }

            HStack(spacing: 8) {
                statBox(value: "\(tally.decisions)", label: "个决定")
                statBox(value: tally.adoptionRate.map { "\($0)%" } ?? "—", label: "照做")
                statBox(value: tally.satisfactionRate.map { "\($0)%" } ?? "—", label: "满意")
            }

            if tall && !review.moodByMonth.isEmpty {
                moodBars
            }

            VStack(alignment: .leading, spacing: 8) {
                if let food = review.topFoods.first {
                    fact(symbol: "fork.knife", title: "最常吃", value: "\(food.name) · \(food.count) 次")
                }
                if let lucky = review.luckiestDay {
                    fact(symbol: "sparkles", title: "最幸运的一天", value: "\(DateText.monthDay(lucky.day)) · \(lucky.score) 分")
                }
                if tall, let best = review.stickLevels.first(where: { $0.count > 0 }) {
                    fact(symbol: "flame.fill", title: "求签", value: "共 \(review.stickCount) 支 · 最好\(best.name == "下" ? "下签" : best.name + "签") \(best.count) 次")
                }
                if tall, let tool = review.topTools.first {
                    fact(symbol: "star.fill", title: "最常用", value: "\(HistoryLog.info(for: tool.name).name) · \(tool.count) 次")
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(12)
            .background(.white.opacity(0.07), in: .rect(cornerRadius: 14))
        }
    }

    private func statBox(value: String, label: String) -> some View {
        VStack(spacing: 3) {
            Text(value)
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .monospacedDigit()
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(label)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(.white.opacity(0.7))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(.white.opacity(0.07), in: .rect(cornerRadius: 14))
        .accessibilityElement(children: .combine)
    }

    private func fact(symbol: String, title: String, value: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: symbol)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(style.palette.accentGradient)
                .frame(width: 16)
                .accessibilityHidden(true)
            Text(title)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(.white.opacity(0.65))
            Spacer(minLength: 6)
            Text(value)
                .font(.system(size: 13, weight: .semibold))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .accessibilityElement(children: .combine)
    }

    /// 12 个月的平均心情小柱子，没记心情的月份是一个小圆点
    private var moodBars: some View {
        let byMonth = Dictionary(review.moodByMonth.map { ($0.month, $0.average) }, uniquingKeysWith: { first, _ in first })
        return VStack(spacing: 6) {
            Text("心情曲线")
                .font(.system(size: 11, weight: .semibold))
                .tracking(2)
                .foregroundStyle(.white.opacity(0.6))
            HStack(alignment: .bottom, spacing: 6) {
                ForEach(1...12, id: \.self) { month in
                    VStack(spacing: 3) {
                        if let average = byMonth[month] {
                            Capsule()
                                .fill(style.palette.accentGradient)
                                .frame(width: 10, height: 8 + CGFloat(average - 1) / 4 * 40)
                        } else {
                            Circle().fill(.white.opacity(0.15)).frame(width: 6, height: 6)
                        }
                        Text("\(month)")
                            .font(.system(size: 9, weight: .medium))
                            .foregroundStyle(.white.opacity(0.55))
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .frame(height: 64, alignment: .bottom)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("每月平均心情")
    }
}
