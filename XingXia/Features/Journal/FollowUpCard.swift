import SwiftUI
import SwiftData

/// 首页最前面的回访卡：最近 3 天内（不含今天）点过「就这么办」、还没回访的最新一条决定，
/// 问「昨天转盘让你吃了「火锅」，后来怎么样？」。选「满意 / 一般 / 后悔」写 satisfaction 和回访时间，
/// 「跳过」只写回访时间。没有要回访的决定时什么都不显示，也不占位置
struct FollowUpCard: View {
    let today: DayKey

    @Environment(\.modelContext) private var context
    @Query private var recent: [DecisionRecord]

    init(today: DayKey) {
        self.today = today
        let start = today.adding(days: -3).startOfDay
        let end = today.startOfDay
        _recent = Query(filter: #Predicate<DecisionRecord> { record in
            record.date >= start && record.date < end
        }, sort: \DecisionRecord.date, order: .reverse)
    }

    private var pending: DecisionRecord? {
        recent.first { $0.adopted == true && $0.satisfaction == nil && $0.followedUpAt == nil }
    }

    var body: some View {
        if let record = pending {
            FollowUpContent(record: record, today: today, rate: { rate(record, $0) }, skip: { skip(record) })
                .id(record.id)
        }
    }

    private func rate(_ record: DecisionRecord, _ option: DecisionSatisfaction) {
        HistoryLog.setSatisfaction(record, option.rawValue, context: context)
        if option == .regret { Haptics.selection() } else { Haptics.success() }
        Announce.post("已记下：\(option.name)")
    }

    private func skip(_ record: DecisionRecord) {
        HistoryLog.setSatisfaction(record, nil, context: context)
        Haptics.selection()
        Announce.post("已跳过这次回访")
    }
}

private struct FollowUpContent: View {
    let record: DecisionRecord
    let today: DayKey
    let rate: (DecisionSatisfaction) -> Void
    let skip: () -> Void

    var body: some View {
        let info = HistoryLog.info(for: record.tool)
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center) {
                SectionHeader(title: "决定回访", symbol: "arrow.uturn.backward.circle")
                Button("跳过", action: skip)
                    .font(.subheadline)
                    .buttonStyle(.plain)
                    .subtleForeground()
                    .padding(.vertical, 4)
                    .padding(.leading, 8)
                    .contentShape(.rect)
                    .accessibilityLabel("跳过这次回访")
            }

            HStack(alignment: .top, spacing: 12) {
                ToolIcon(symbol: info.symbol, colors: info.colors, size: 40)
                VStack(alignment: .leading, spacing: 4) {
                    Text(FollowUpText.sentence(record, today: today))
                        .font(.subheadline.weight(.semibold))
                        .fixedSize(horizontal: false, vertical: true)
                    if let question = record.askedQuestion {
                        Text("当时问的是「\(question)」")
                            .font(.caption)
                            .subtleForeground()
                            .lineLimit(2)
                    }
                }
                Spacer(minLength: 0)
            }
            .accessibilityElement(children: .combine)

            // 一行放不下（超大字号）时竖排
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 8) { options }
                VStack(spacing: 8) { options }
            }
        }
        .glassCard()
    }

    @ViewBuilder
    private var options: some View {
        ForEach(DecisionSatisfaction.allCases) { option in
            Button {
                rate(option)
            } label: {
                HStack(spacing: 6) {
                    Text(option.emoji)
                    Text(option.name).font(.subheadline.weight(.semibold))
                }
                .lineLimit(1)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 4)
            }
            .buttonStyle(.glass)
            .accessibilityLabel(option.name)
        }
    }
}
