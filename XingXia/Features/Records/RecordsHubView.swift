import SwiftUI
import SwiftData

struct RecordsHubView: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Query(sort: \DecisionRecord.date, order: .reverse) private var records: [DecisionRecord]
    @Query(sort: \MoodEntry.dayKey, order: .reverse) private var moods: [MoodEntry]
    @Query(sort: \Countdown.createdAt) private var countdowns: [Countdown]

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                MoodCheckInCard()

                // 最近 7 天心情
                NavigationLink(value: AppRoute.mood) {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack {
                            SectionHeader(title: "最近一周", subtitle: "心情 × 运势", symbol: "waveform.path.ecg")
                            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                        }
                        HStack(spacing: 0) {
                            ForEach((0..<7).reversed(), id: \.self) { offset in
                                let day = DayKey.today.adding(days: -offset)
                                let mood = moods.first { $0.dayKey == day.rawValue }.flatMap { MoodLevel(rawValue: $0.mood) }
                                let score = FortuneEngine.daily(for: app.birth, on: day).score
                                VStack(spacing: 6) {
                                    Text(mood?.emoji ?? "·").font(.title3).frame(height: 28)
                                    Capsule()
                                        .fill(palette.accentGradient)
                                        .frame(width: 8, height: CGFloat(score - 30) * 0.7)
                                        .frame(height: 50, alignment: .bottom)
                                    Text(offset == 0 ? "今天" : DateText.weekday(day)).font(.caption2).foregroundStyle(.secondary)
                                }
                                .frame(maxWidth: .infinity)
                            }
                        }
                    }
                    .glassCard(interactive: true)
                }
                .buttonStyle(.pressableCard)

                HStack(spacing: 14) {
                    NavigationLink(value: AppRoute.fortuneCalendar) {
                        RecordTile(symbol: "calendar", colors: [palette.accent, palette.accentSecondary], title: "运势日历",
                                   value: "\(DayKey.today.month)月", caption: "每日运势一览")
                    }
                    .buttonStyle(.pressableCard)
                    NavigationLink(value: AppRoute.countdowns) {
                        RecordTile(symbol: "hourglass", colors: [Color(hex: 0x22D3EE), Color(hex: 0x6366F1)], title: "倒数日",
                                   value: "\(countdowns.count)", caption: nextCountdownText)
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
                            Text("用过的随机工具结果都会自动记在这里").font(.subheadline).foregroundStyle(.secondary)
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
    }

    private var nextCountdownText: String {
        let upcoming = countdowns
            .filter { $0.kindValue != .anniversary }
            .map { ($0, $0.status()) }
            .filter { $0.1.prefix != "已过" }
            .min { $0.1.days < $1.1.days }
        guard let (countdown, status) = upcoming else { return "添加重要日子" }
        return status.isToday ? "\(countdown.title) 就是今天" : "\(countdown.title) 还有\(status.days)天"
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
                Text(caption).font(.caption2).foregroundStyle(.secondary).lineLimit(1)
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
                    .font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer()
            Text(DateText.relative(record.date)).font(.caption2).foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
    }
}
