import SwiftUI

/// 日记顶部的本月回顾：做了几个决定、照做几个、满意率，最常用的工具、最纠结的事、平均心情、运势最高的一天
struct MonthlyReviewCard: View {
    let review: MonthlyReview
    let month: DayKey
    let isCurrentMonth: Bool

    @ScaledMetric(relativeTo: .subheadline) private var statWidth: CGFloat = 140

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(title: isCurrentMonth ? "本月回顾" : "\(month.month)月回顾",
                          subtitle: review.isEmpty ? nil : "记录了 \(review.recordDays) 天",
                          symbol: "chart.pie.fill")
            if review.isEmpty {
                Text(isCurrentMonth ? "这个月还没有记录。用工具做个决定、点一下心情，这里会慢慢长出一份小结。" : "这个月没有记录。")
                    .font(.subheadline)
                    .subtleForeground()
            } else {
                Text(Self.summary(review.tally))
                    .font(.headline)
                    .fixedSize(horizontal: false, vertical: true)
                LazyVGrid(columns: [GridItem(.adaptive(minimum: statWidth), spacing: 10, alignment: .topLeading)],
                          alignment: .leading, spacing: 10) {
                    ForEach(stats) { stat in
                        ReviewStat(stat: stat)
                    }
                }
            }
        }
        .glassCard()
    }

    /// 「做了 23 个决定，照做 17 个，满意率 82%」
    static func summary(_ tally: DecisionTally) -> String {
        guard tally.decisions > 0 else { return "这个月还没有用星匣做决定" }
        var text = "做了 \(tally.decisions) 个决定"
        if tally.adopted > 0 { text += "，照做 \(tally.adopted) 个" }
        if let rate = tally.satisfactionRate { text += "，满意率 \(rate)%" }
        return text
    }

    private var stats: [ReviewStat.Item] {
        var items: [ReviewStat.Item] = []
        if let tool = review.topTool {
            items.append(.init(title: "最常用", symbol: "star.fill", value: "\(HistoryLog.info(for: tool.name).name) · \(tool.count) 次"))
        }
        if let kind = review.topKind {
            items.append(.init(title: "最纠结", symbol: "questionmark.bubble.fill", value: "\(kind.title) · \(review.topKindCount) 次"))
        }
        if let average = review.moodAverage {
            let emoji = MoodLevel(rawValue: Int(average.rounded()))?.emoji ?? ""
            items.append(.init(title: "平均心情", symbol: "face.smiling",
                               value: "\(emoji) \(String(format: "%.1f", average)) · 打卡 \(review.moodDays) 天"))
        }
        if let best = review.bestDay {
            items.append(.init(title: "运势最高", symbol: "sparkles", value: "\(DateText.monthDay(best.day)) · \(best.score) 分"))
        }
        let tally = review.tally
        if tally.rated > 0 {
            items.append(.init(title: "回访", symbol: "arrow.uturn.backward.circle",
                               value: "满意 \(tally.satisfied) · 一般 \(tally.neutral) · 后悔 \(tally.regretted)"))
        }
        return items
    }
}

/// 回顾卡里的一小格：标题 + 数值
struct ReviewStat: View {
    struct Item: Identifiable {
        let title: String
        let symbol: String
        let value: String
        var id: String { title }
    }

    let stat: Item

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Label(stat.title, systemImage: stat.symbol)
                .font(.caption)
                .subtleForeground()
            Text(stat.value)
                .font(.subheadline.weight(.semibold))
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(.primary.opacity(0.05), in: .rect(cornerRadius: 16))
        .accessibilityElement(children: .combine)
    }
}
