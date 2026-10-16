import SwiftUI
import SwiftData
import Combine

struct RecordsHubView: View {
    @Environment(AppState.self) private var app
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase
    @Query(sort: \DecisionRecord.date, order: .reverse) private var records: [DecisionRecord]
    @Query(sort: \MoodEntry.dayKey, order: .reverse) private var moods: [MoodEntry]
    @Query(sort: \Countdown.createdAt) private var countdowns: [Countdown]
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]

    /// 零点和回到前台时刷新，App 跨天常驻时心情卡和一周条不会停在昨天
    @State private var today = DayKey.today

    var body: some View {
        // 倒数日含档案生日（开关打开时），和倒数日列表一致
        let countdownItems = CountdownItem.all(countdowns, profiles: profiles, includeProfiles: settings.profileBirthdaysInCountdowns)
        ScrollView {
            VStack(spacing: 16) {
                MoodCheckInCard(today: today)

                // 最近 7 天心情
                NavigationLink(value: AppRoute.mood) {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            SectionHeader(title: "最近一周", subtitle: "心情 × 运势", symbol: "waveform.path.ecg")
                            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                        }
                        HStack(spacing: 0) {
                            ForEach((0..<7).reversed(), id: \.self) { offset in
                                let day = today.adding(days: -offset)
                                let mood = moods.first { $0.dayKey == day.rawValue }.flatMap { MoodLevel(rawValue: $0.mood) }
                                let score = FortuneEngine.daily(for: app.birth, on: day).score
                                let dayName = offset == 0 ? "今天" : DateText.weekday(day)
                                VStack(spacing: 6) {
                                    Group {
                                        if let mood {
                                            Text(mood.emoji).font(.title3)
                                        } else {
                                            // 没记心情：浅色空心圆
                                            Circle().strokeBorder(.primary.opacity(0.25), lineWidth: 1.5).frame(width: 16, height: 16)
                                        }
                                    }
                                    .frame(height: 28)
                                    Capsule()
                                        .fill(palette.accentGradient)
                                        .frame(width: 8, height: CGFloat(score - 30) * 0.7)
                                        .frame(height: 50, alignment: .bottom)
                                    Text(dayName).font(.caption2).foregroundStyle(.primary.opacity(0.7))
                                }
                                .frame(maxWidth: .infinity)
                                .accessibilityElement(children: .ignore)
                                .accessibilityLabel("\(dayName)，\(mood.map { "心情\($0.name)" } ?? "未记录心情")，运势 \(score) 分")
                            }
                        }
                    }
                    .glassCard(interactive: true)
                }
                .buttonStyle(.pressableCard)

                HStack(spacing: 14) {
                    NavigationLink(value: AppRoute.fortuneCalendar) {
                        RecordTile(symbol: "calendar", colors: [palette.accent, palette.accentSecondary], title: "运势日历",
                                   value: "\(today.month)月", caption: "每日运势一览")
                    }
                    .buttonStyle(.pressableCard)
                    NavigationLink(value: AppRoute.countdowns) {
                        RecordTile(symbol: "hourglass", colors: [Color(hex: 0x22D3EE), Color(hex: 0x6366F1)], title: "倒数日",
                                   value: "\(countdownItems.count)", caption: nextCountdownText(countdownItems))
                    }
                    .buttonStyle(.pressableCard)
                }

                NavigationLink(value: AppRoute.history) {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            SectionHeader(title: "决策历史", subtitle: "共 \(records.count) 条", symbol: "clock.arrow.circlepath")
                            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                        }
                        if records.isEmpty {
                            Text("用过的随机工具结果都会自动记在这里").font(.subheadline).foregroundStyle(.primary.opacity(0.7))
                        } else {
                            ForEach(records.prefix(4)) { record in
                                HistoryRow(record: record)
                            }
                        }
                    }
                    .glassCard(interactive: true)
                }
                .buttonStyle(.pressableCard)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("记录")
        .onAppear(perform: refreshToday)
        // 这个通知不保证在主线程发出
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
    }

    private func refreshToday() {
        let now = DayKey.today
        if now != today { today = now }
    }

    private func nextCountdownText(_ items: [CountdownItem]) -> String {
        guard let next = CountdownItem.nearest(items, today: today) else { return "添加重要日子" }
        return next.status.isToday ? "\(next.item.title) 就是今天" : "\(next.item.title) 还有\(next.status.days)天"
    }
}

private struct RecordTile: View {
    let symbol: String
    let colors: [Color]
    let title: String
    let value: String
    let caption: String

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ToolIcon(symbol: symbol, colors: colors, size: 40)
            Text(value).font(.system(size: 26, weight: .bold, design: .rounded)).foregroundStyle(.primary)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline).foregroundStyle(.primary)
                Text(caption).font(.caption2).foregroundStyle(.primary.opacity(0.7)).lineLimit(1)
            }
        }
        .glassCard(padding: 16, interactive: true)
    }
}

struct HistoryRow: View {
    let record: DecisionRecord

    var body: some View {
        let info = HistoryLog.info(for: record.tool)
        HStack(spacing: 12) {
            ToolIcon(symbol: info.symbol, colors: info.colors, size: 34)
            VStack(alignment: .leading, spacing: 2) {
                Text(record.result).font(.subheadline.weight(.semibold)).foregroundStyle(.primary).lineLimit(1)
                Text(record.title.isEmpty ? info.name : "\(info.name) · \(record.title)")
                    .font(.caption).foregroundStyle(.primary.opacity(0.7)).lineLimit(1)
            }
            Spacer()
            Text(DateText.relative(record.date)).font(.caption2).foregroundStyle(.primary.opacity(0.7))
        }
        .accessibilityElement(children: .combine)
    }
}
