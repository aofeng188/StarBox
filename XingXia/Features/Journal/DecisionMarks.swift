import SwiftUI
import SwiftData

/// 回访结果（DecisionRecord.satisfaction）：1 满意，0 一般，-1 后悔
enum DecisionSatisfaction: Int, CaseIterable, Identifiable {
    case satisfied = 1, okay = 0, regret = -1

    var id: Int { rawValue }

    var name: String {
        switch self {
        case .satisfied: "满意"
        case .okay: "一般"
        case .regret: "后悔"
        }
    }

    var emoji: String {
        switch self {
        case .satisfied: "😊"
        case .okay: "😐"
        case .regret: "😣"
        }
    }

    var symbol: String {
        switch self {
        case .satisfied: "hand.thumbsup.fill"
        case .okay: "minus.circle.fill"
        case .regret: "hand.thumbsdown.fill"
        }
    }

    func color(_ scheme: ColorScheme) -> Color {
        switch self {
        case .satisfied: Color.positive(scheme)
        case .okay: Color(hex: 0x94A3B8)
        case .regret: Color.negative(scheme)
        }
    }
}

/// 决定后面的小标记：照做了 / 再想想，满意 / 一般 / 后悔。什么都没有时不占位置
struct DecisionMarks: View {
    let record: DecisionRecord
    /// 还没表态时显示「还没决定」（日记时间线里用）
    var showsPending = false

    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let adopted = record.adopted
        let satisfaction = record.satisfaction.flatMap(DecisionSatisfaction.init(rawValue:))
        if adopted != nil || satisfaction != nil || showsPending {
            HStack(spacing: 6) {
                switch adopted {
                case true?:
                    TagChip(text: "照做了", color: Color.positive(scheme), symbol: "checkmark.circle.fill")
                case false?:
                    TagChip(text: "再想想", color: Color(hex: 0x94A3B8), symbol: "arrow.uturn.left.circle")
                case nil:
                    if satisfaction == nil {
                        TagChip(text: "还没决定", color: Color(hex: 0x94A3B8), symbol: "circle.dashed")
                    }
                }
                if let satisfaction {
                    TagChip(text: satisfaction.name, color: satisfaction.color(scheme), symbol: satisfaction.symbol)
                }
            }
        }
    }
}

/// 点开一条决定补记：照做了吗、后来怎么样。改动立即保存
struct DecisionEditSheet: View {
    let record: DecisionRecord

    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Environment(\.palette) private var palette

    var body: some View {
        let info = HistoryLog.info(for: record.tool)
        // 在 body 里读一次，记录改动后页面跟着刷新
        let adopted = record.adopted
        let satisfaction = record.satisfaction
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 12) {
                        HStack(spacing: 12) {
                            ToolIcon(symbol: info.symbol, colors: palette.identity(info.colors), size: 40)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(info.name).font(.headline)
                                Text("\(DateText.full(DayKey(record.date))) \(DateText.time(record.date))")
                                    .font(.caption)
                                    .subtleForeground()
                            }
                        }
                        if let question = record.askedQuestion {
                            Text("「\(question)」").font(.subheadline).subtleForeground()
                        }
                        Text(record.result)
                            .font(.title3.weight(.bold))
                            .fixedSize(horizontal: false, vertical: true)
                            .textSelection(.enabled)
                    }
                    .accessibilityElement(children: .combine)
                    .glassCard()

                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeader(title: "照做了吗？", symbol: "checkmark.circle")
                        Picker("照做了吗？", selection: Binding(get: { adopted }, set: { setAdopted($0) })) {
                            Text("还没决定").tag(Bool?.none)
                            Text("照做了").tag(Bool?.some(true))
                            Text("再想想").tag(Bool?.some(false))
                        }
                        .pickerStyle(.segmented)
                    }
                    .glassCard()

                    VStack(alignment: .leading, spacing: 12) {
                        SectionHeader(title: "后来怎么样？", symbol: "arrow.uturn.backward.circle")
                        Picker("后来怎么样？", selection: Binding(get: { satisfaction }, set: { setSatisfaction($0) })) {
                            Text("不评价").tag(Int?.none)
                            ForEach(DecisionSatisfaction.allCases) { option in
                                Text(option.name).tag(Int?.some(option.rawValue))
                            }
                        }
                        .pickerStyle(.segmented)
                    }
                    .glassCard()
                }
                .pageContent()
                .padding(.top, 8)
                .padding(.bottom, 24)
            }
            .cosmicBackground(dimmed: true)
            .navigationTitle("补记")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成", systemImage: "checkmark") { dismiss() }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func setAdopted(_ value: Bool?) {
        if let value {
            HistoryLog.setAdopted(record, value, context: context)
        } else {
            record.adopted = nil
            try? context.save()
        }
        Haptics.selection()
    }

    private func setSatisfaction(_ value: Int?) {
        // 选「不评价」也记下回访时间，首页不再来问
        HistoryLog.setSatisfaction(record, value, context: context)
        Haptics.selection()
    }
}

/// 回访卡上的那句话。Android 的回访卡用同样的规则和文案：
/// 「{哪天}{谁}{怎么}「{结果}」，后来怎么样？」
/// - 哪天：昨天 / 前天 / 三天前（按日期差，回访只看最近 3 天）
/// - 谁：food、nearby →「星匣」；wheel 转盘、coin 硬币、dice 骰子、finger 指尖轮盘、answers 答案之书、
///   lottery 抽奖、number 随机数、groups 随机分组；其他用 HistoryLog.info 的工具名
/// - 怎么：answers →「告诉你」；food、nearby →「让你吃了」；wheel、lottery 且问题判断为 food（结果就是菜名）→「让你吃了」；
///   其余（硬币、骰子这类结果不是菜名的）→「帮你选了」
@MainActor
enum FollowUpText {
    static func dayWord(_ day: DayKey, today: DayKey) -> String {
        switch day.days(to: today) {
        case 1: "昨天"
        case 2: "前天"
        case 3: "三天前"
        default: "前几天"
        }
    }

    static func subject(for tool: String) -> String {
        switch tool {
        case "food", "nearby": "星匣"
        case "wheel": "转盘"
        case "coin": "硬币"
        case "dice": "骰子"
        case "finger": "指尖轮盘"
        case "answers": "答案之书"
        case "lottery": "抽奖"
        case "number": "随机数"
        case "groups": "随机分组"
        default: HistoryLog.info(for: tool).name
        }
    }

    static func verb(for record: DecisionRecord) -> String {
        switch record.tool {
        case "answers": return "告诉你"
        case "food", "nearby": return "让你吃了"
        case "wheel", "lottery":
            return JournalStats.questionKind(tool: record.tool, question: record.askedQuestion) == .food ? "让你吃了" : "帮你选了"
        default: return "帮你选了"
        }
    }

    static func sentence(_ record: DecisionRecord, today: DayKey) -> String {
        "\(dayWord(DayKey(record.date), today: today))\(subject(for: record.tool))\(verb(for: record))「\(record.result)」，后来怎么样？"
    }
}
