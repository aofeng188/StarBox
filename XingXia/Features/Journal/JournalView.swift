import SwiftUI
import SwiftData
import Combine

/// 日记 tab：月份切换、本月回顾、快捷入口，下面是按天倒序的时间线（心情、决定、签、塔罗）。
/// 12 月和 1 月顶部多一条年度回顾横幅
struct JournalView: View {
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// 零点和回到前台时刷新
    @State private var today = DayKey.today
    /// 正在看的月份（1 号）
    @State private var month = JournalView.firstDay(of: .today)

    var body: some View {
        let isCurrentMonth = month == Self.firstDay(of: today)
        ScrollView {
            VStack(spacing: 16) {
                if today.month == 12 || today.month == 1 {
                    AnnualReviewBanner(year: AppRoute.reviewYear(on: today.date), isDecember: today.month == 12)
                }
                monthSwitcher(isCurrentMonth: isCurrentMonth)
                JournalMonthContent(month: month, today: today)
                    .id(month)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("日记")
        .onAppear(perform: refreshToday)
        // 这个通知不保证在主线程发出
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
    }

    private func monthSwitcher(isCurrentMonth: Bool) -> some View {
        HStack(spacing: 10) {
            VStack(alignment: .leading, spacing: 0) {
                Text("\(String(month.year))年").font(.caption).subtleForeground()
                Text("\(month.month)月").font(.title2.weight(.bold))
            }
            .accessibilityElement(children: .combine)
            Spacer(minLength: 0)
            if !isCurrentMonth {
                Button("回到本月") { setMonth(Self.firstDay(of: today)) }
                    .font(.subheadline)
                    .buttonStyle(.glass)
            }
            Button {
                setMonth(Self.shift(month, by: -1))
            } label: {
                Image(systemName: "chevron.left").frame(width: 22, height: 22)
            }
            .buttonStyle(.glass)
            .accessibilityLabel("上个月")
            Button {
                setMonth(Self.shift(month, by: 1))
            } label: {
                Image(systemName: "chevron.right").frame(width: 22, height: 22)
            }
            .buttonStyle(.glass)
            .disabled(isCurrentMonth)
            .accessibilityLabel("下个月")
        }
        .padding(.horizontal, 4)
    }

    private func setMonth(_ value: DayKey) {
        Haptics.selection()
        withAnimation(reduceMotion ? nil : Animation.snappy) { month = value }
    }

    private func refreshToday() {
        let now = DayKey.today
        guard now != today else { return }
        // 停在「本月」时跟着翻到新的月份
        if month == Self.firstDay(of: today) { month = Self.firstDay(of: now) }
        today = now
    }

    nonisolated static func firstDay(of day: DayKey) -> DayKey {
        DayKey(year: day.year, month: day.month, day: 1)
    }

    /// 前后挪几个月，返回那个月的 1 号
    nonisolated static func shift(_ month: DayKey, by delta: Int) -> DayKey {
        let total = month.year * 12 + (month.month - 1) + delta
        return DayKey(year: total / 12, month: total % 12 + 1, day: 1)
    }
}

/// 某个月的回顾、快捷入口和时间线。月份变了整个重建，查询只取这个月的数据
private struct JournalMonthContent: View {
    let month: DayKey
    let today: DayKey

    @Environment(AppState.self) private var app
    @Query private var records: [DecisionRecord]
    @Query private var moods: [MoodEntry]
    @Query private var draws: [StickDraw]
    @Query private var readings: [TarotReading]
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]

    @State private var editing: DecisionRecord?
    /// 这个月每天的运势分数（按当前档案），算一次存起来
    @State private var scores = ScoreCache()

    init(month: DayKey, today: DayKey) {
        self.month = month
        self.today = today
        let start = month.startOfDay
        let end = JournalView.shift(month, by: 1).startOfDay
        // yyyyMM00 < dayKey < yyyyMM99：这个月的每一天
        let low = month.year * 10000 + month.month * 100
        let high = low + 99
        _records = Query(filter: #Predicate<DecisionRecord> { record in
            record.date >= start && record.date < end
        }, sort: \DecisionRecord.date, order: .reverse)
        _moods = Query(filter: #Predicate<MoodEntry> { entry in
            entry.dayKey > low && entry.dayKey < high
        }, sort: \MoodEntry.dayKey, order: .reverse)
        _draws = Query(filter: #Predicate<StickDraw> { draw in
            draw.dayKey > low && draw.dayKey < high
        }, sort: \StickDraw.date)
        _readings = Query(filter: #Predicate<TarotReading> { reading in
            reading.date >= start && reading.date < end
        }, sort: \TarotReading.date)
    }

    var body: some View {
        let birth = app.birth
        let key = ScoreKey(month: month.rawValue, birth: birth)
        let days = JournalDay.group(records: records, moods: moods, draws: draws, readings: readings)
        let review = JournalStats.monthly(
            decisions: records.map { DecisionSample($0) },
            moods: moods.map { MoodSample(dayKey: $0.dayKey, mood: $0.mood) },
            extraDays: Set(draws.map(\.dayKey)).union(readings.map { DayKey($0.date).rawValue }),
            fortuneScore: { score($0, key: key, birth: birth) })
        let names = Dictionary(profiles.map { ($0.id, $0.name) }, uniquingKeysWith: { first, _ in first })
        let isCurrentMonth = month == JournalView.firstDay(of: today)

        VStack(spacing: 16) {
            MonthlyReviewCard(review: review, month: month, isCurrentMonth: isCurrentMonth)
            JournalShortcuts()
            LazyVStack(alignment: .leading, spacing: 16) {
                SectionHeader(title: "时间线", subtitle: days.isEmpty ? nil : "\(days.count) 天有记录", symbol: "list.bullet.below.rectangle")
                    .padding(.horizontal, 4)
                if days.isEmpty {
                    EmptyHint(symbol: "text.book.closed",
                              title: isCurrentMonth ? "这个月还没有记录" : "这个月没有记录",
                              message: "点一下心情、用工具做个决定、求一支签，都会按天记在这里")
                        .glassCard()
                } else {
                    ForEach(days) { day in
                        TimelineDayCard(entry: day, today: today, score: score(day.day, key: key, birth: birth),
                                        names: names, primaryID: app.primary?.id) { record in
                            editing = record
                        }
                    }
                }
            }
        }
        .sheet(item: $editing) { record in
            DecisionEditSheet(record: record)
        }
        .task(id: key) {
            // 算到今天为止；过去的月份算整月
            let first = month
            let last = today
            let result = await Task.detached(priority: .userInitiated) {
                var result: [Int: Int] = [:]
                var day = first
                while day.month == first.month && day <= last {
                    result[day.rawValue] = FortuneEngine.daily(for: birth, on: day).score
                    day = day.adding(days: 1)
                }
                return result
            }.value
            guard !Task.isCancelled else { return }
            scores = ScoreCache(key: key, scores: result)
        }
    }

    /// 先查缓存（档案没换过时），查不到再现算
    private func score(_ day: DayKey, key: ScoreKey, birth: BirthInfo?) -> Int {
        if scores.key == key, let value = scores.scores[day.rawValue] { return value }
        return FortuneEngine.daily(for: birth, on: day).score
    }
}

private struct ScoreKey: Hashable {
    let month: Int
    let birth: BirthInfo?
}

private struct ScoreCache {
    var key: ScoreKey?
    var scores: [Int: Int] = [:]
}

/// 12 月和 1 月日记顶部的年度回顾横幅
private struct AnnualReviewBanner: View {
    let year: Int
    let isDecember: Bool

    var body: some View {
        NavigationLink(value: AppRoute.annualReview(year: year)) {
            HStack(spacing: 14) {
                GlowIcon(symbol: "sparkles", color: Color(hex: 0xF59E0B), secondary: Color(hex: 0xEF4444), size: 46)
                VStack(alignment: .leading, spacing: 3) {
                    Text("\(String(year)) 年度回顾").font(.headline)
                    Text(isDecember ? "这一年快过完了，看看你的年度关键词和最幸运的一天" : "新年好！回头看看去年做过的决定和心情")
                        .font(.caption)
                        .subtleForeground()
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
            }
            .glassCard(tint: Color(hex: 0xF59E0B).opacity(0.12), interactive: true)
        }
        .buttonStyle(.pressableCard)
    }
}

/// 心情、倒数日、运势日历、决策历史、塔罗手记、年度回顾
private struct JournalShortcuts: View {
    private struct Shortcut: Identifiable {
        let name: String
        let symbol: String
        let colors: [Color]
        let route: AppRoute
        var id: String { name }
    }

    @ScaledMetric(relativeTo: .caption) private var tileWidth: CGFloat = 88

    private var shortcuts: [Shortcut] {
        [
            Shortcut(name: "心情", symbol: "face.smiling.inverse", colors: [Color(hex: 0xFBBF24), Color(hex: 0xF97316)], route: .mood),
            Shortcut(name: "倒数日", symbol: "hourglass", colors: [Color(hex: 0x60A5FA), Color(hex: 0x6366F1)], route: .countdowns),
            Shortcut(name: "运势日历", symbol: "calendar", colors: [Color(hex: 0x8B5CF6), Color(hex: 0xEC4899)], route: .fortuneCalendar),
            Shortcut(name: "决策历史", symbol: "clock.arrow.circlepath", colors: [Color(hex: 0x94A3B8), Color(hex: 0x6366F1)], route: .history),
            Shortcut(name: "塔罗手记", symbol: "book.closed.fill", colors: [Color(hex: 0x8B5CF6), Color(hex: 0x3B82F6)], route: .tarotJournal),
            Shortcut(name: "年度回顾", symbol: "chart.bar.xaxis", colors: [Color(hex: 0xF59E0B), Color(hex: 0xEF4444)],
                     route: .annualReview(year: AppRoute.reviewYear())),
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "快捷入口", symbol: "square.grid.2x2.fill")
            LazyVGrid(columns: [GridItem(.adaptive(minimum: tileWidth), spacing: 10)], spacing: 14) {
                ForEach(shortcuts) { shortcut in
                    NavigationLink(value: shortcut.route) {
                        VStack(spacing: 6) {
                            ToolIcon(symbol: shortcut.symbol, colors: shortcut.colors, size: 44)
                            Text(shortcut.name)
                                .font(.caption)
                                .foregroundStyle(.primary)
                                .lineLimit(1)
                                .minimumScaleFactor(0.8)
                        }
                        .frame(maxWidth: .infinity)
                        .contentShape(.rect)
                    }
                    .buttonStyle(.pressableCard)
                    .accessibilityLabel(shortcut.name)
                }
            }
        }
        .glassCard()
    }
}
