import SwiftUI
import SwiftData

/// 刚记下一个决定时，底部浮出「工具：结果」和「就这么办 / 再想想」，选择写进 DecisionRecord.adopted。
/// 8 秒后（VoiceOver 打开时 20 秒）或切换 tab、页面时自动收起，收起不算表态。
struct DecisionPrompt: View {
    /// 底部让出的高度：tab 栏可见时要让出 tab 栏
    var bottomInset: CGFloat = 8

    @Environment(AppState.self) private var app
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.accessibilityVoiceOverEnabled) private var voiceOver

    /// 会弹出浮层的工具：真的在替人拿主意的那些。猜拳、刮刮乐、抽奖、分组、随机数之类不问；
    /// 求签、塔罗、掷筊在日记里单独记，也不问（掷筊在弹出的面板里，浮层会被挡住）
    static let promptTools: Set<String> = [
        ToolKind.food.rawValue, ToolKind.wheel.rawValue, ToolKind.finger.rawValue, ToolKind.coin.rawValue,
        ToolKind.dice.rawValue, "answers", "nearby",
    ]

    private var record: DecisionRecord? {
        guard let record = app.lastDecision, !record.isDeleted, record.modelContext != nil,
              Self.promptTools.contains(record.tool) else { return nil }
        return record
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            if let record {
                card(record)
                    .id(record.id)
                    .transition(reduceMotion ? .opacity : .move(edge: .bottom).combined(with: .opacity))
                    .task(id: record.id) { await autoDismiss(record.id) }
            }
        }
        .frame(maxWidth: 560)
        .padding(.horizontal, 16)
        .padding(.bottom, bottomInset)
        .animation(reduceMotion ? .easeInOut(duration: 0.25) : .spring(response: 0.45, dampingFraction: 0.85), value: record?.id)
    }

    private func card(_ record: DecisionRecord) -> some View {
        let info = HistoryLog.info(for: record.tool)
        let colors = palette.identity(info.colors)
        let summary = "\(info.name)：\(record.result)"
        return ViewThatFits(in: .horizontal) {
            // 放得下时一行胶囊
            HStack(spacing: 10) {
                ToolIcon(symbol: info.symbol, colors: colors, size: 30)
                Text(summary).font(.subheadline.weight(.semibold)).lineLimit(1)
                Spacer(minLength: 4)
                buttons(record)
            }
            .padding(.leading, 8)
            .padding(.trailing, 6)
            .padding(.vertical, 6)
            .glassEffect(.regular, in: .capsule)

            // 结果长或字号大时分两行
            VStack(alignment: .leading, spacing: 10) {
                HStack(spacing: 10) {
                    ToolIcon(symbol: info.symbol, colors: colors, size: 30)
                    Text(summary).font(.subheadline.weight(.semibold)).lineLimit(3)
                    Spacer(minLength: 0)
                }
                HStack(spacing: 8) {
                    Spacer(minLength: 0)
                    buttons(record)
                }
            }
            .padding(12)
            .glassEffect(.regular, in: .rect(cornerRadius: 24))
        }
        .accessibilityElement(children: .contain)
    }

    @ViewBuilder
    private func buttons(_ record: DecisionRecord) -> some View {
        Button {
            decide(record, adopted: false)
        } label: {
            Text("再想想")
                .font(.subheadline.weight(.medium))
                .lineLimit(1)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(.primary.opacity(0.08), in: .capsule)
        }
        .buttonStyle(.plain)

        Button {
            decide(record, adopted: true)
        } label: {
            Text("就这么办")
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                .foregroundStyle(palette.onAccent)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(palette.accentGradient, in: .capsule)
        }
        .buttonStyle(.plain)
    }

    private func decide(_ record: DecisionRecord, adopted: Bool) {
        HistoryLog.setAdopted(record, adopted, context: context)
        if adopted { Haptics.success() } else { Haptics.selection() }
        Announce.post(adopted ? "已记下：就这么办" : "已记下：再想想")
        app.dismissDecisionPrompt()
    }

    private func autoDismiss(_ id: UUID) async {
        if voiceOver {
            // 等工具自己的结果播报念完再提示
            try? await Task.sleep(for: .seconds(1.5))
            guard !Task.isCancelled else { return }
            Announce.post("要照这个结果做吗？屏幕底部可以选「就这么办」或「再想想」")
        }
        try? await Task.sleep(for: .seconds(voiceOver ? 20 : 8))
        guard !Task.isCancelled, app.lastDecision?.id == id else { return }
        app.dismissDecisionPrompt()
    }
}

extension AppRoute {
    /// 隐藏了浮动 tab 栏的沉浸页（StageScaffold 或页面自己的 `.toolbar(.hidden, for: .tabBar)`），
    /// 决策浮层在这些页面贴近底部安全区
    var hidesTabBar: Bool {
        switch self {
        case .tool(let tool): [ToolKind.coin, .dice, .finger, .rps, .scratch, .truthDare].contains(tool)
        case .stick, .tarot, .answers, .tarotJournal: true
        default: false
        }
    }

    /// 最底下是大号主按钮的舞台页（抛硬币、掷骰子）：浮层抬到主按钮上方，不然连抛、连掷时 8 秒内按不到按钮。
    /// 按默认字号下主按钮（掷骰子还有下面一行历史）的高度估出
    var promptLiftAboveAction: CGFloat? {
        guard case .tool(let tool) = self else { return nil }
        switch tool {
        case .coin: return 80
        case .dice: return 108
        default: return nil
        }
    }
}
