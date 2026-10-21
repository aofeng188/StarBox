import SwiftUI

/// 「问一问」tab 根页：问题框 → 推荐方法；下面是收藏、占卜区和全部工具（原「占卜」「百宝」两个 tab 的入口都在这里）
struct AskView: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var draft = ""
    @State private var analysis: QuestionAnalysis?
    /// analysis 对应的问题；问题框改成别的内容后结果收起，重新提交再判断
    @State private var analyzedText = ""
    @State private var placeholderIndex = 0
    @FocusState private var focused: Bool
    /// 问题框里的清空、提交按钮跟着字号放大（封顶 1.6 倍），图标不会撑出圆底
    @ScaledMetric(relativeTo: .body) private var clearSize: CGFloat = 32
    @ScaledMetric(relativeTo: .headline) private var sendSize: CGFloat = 36

    private static let examples = ["中午吃火锅还是烤肉？", "周末要不要去杭州？", "哪天搬家比较好？", "他是不是喜欢我？", "奶茶选哪一杯？"]

    private var trimmedDraft: String { draft.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                questionCard

                if let analysis {
                    AskResultCard(analysis: analysis) { go($0, analysis: analysis) }
                        .transition(reduceMotion ? .opacity : .move(edge: .top).combined(with: .opacity))
                }

                FavoriteToolsRow()

                VStack(alignment: .leading, spacing: 14) {
                    AskSectionTitle(title: "占卜", subtitle: "求签 · 塔罗 · 星座 · 黄历")
                    DivinationSection()
                }

                VStack(alignment: .leading, spacing: 14) {
                    AskSectionTitle(title: "工具", subtitle: "帮我决定 · 随机一下 · 聚会时光")
                    ToolCategorySections()
                }
            }
            .pageContent(maxWidth: PageMetrics.gridWidth)
            .padding(.bottom, 30)
        }
        .scrollDismissesKeyboard(.interactively)
        .cosmicBackground()
        .navigationTitle("问一问")
        .onChange(of: draft) { _, newValue in
            let trimmed = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
            guard trimmed != analyzedText || trimmed.isEmpty else { return }
            // 清空或改成了别的问题：上一题的结果和带到工具页的问题都作废
            if app.currentQuestion != nil { app.currentQuestion = nil }
            if analysis != nil {
                withAnimation(.easeOut(duration: 0.2)) { analysis = nil }
            }
            analyzedText = ""
        }
        .task {
            // 问题框空着时轮换示例
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(3.5))
                guard !Task.isCancelled else { return }
                if draft.isEmpty && !focused {
                    placeholderIndex = (placeholderIndex + 1) % Self.examples.count
                }
            }
        }
    }

    // MARK: 问题框

    private var questionCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("你想决定什么？")
                .font(AppFont.serif(24, weight: .bold))
                .accessibilityAddTraits(.isHeader)

            HStack(spacing: 6) {
                TextField(Self.examples[placeholderIndex], text: $draft)
                    .focused($focused)
                    .submitLabel(.go)
                    .onSubmit(submit)
                    .accessibilityLabel("你想决定什么")
                if !draft.isEmpty {
                    Button {
                        draft = ""
                        focused = true
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .symbolRenderingMode(.hierarchical)
                            .foregroundStyle(.secondary)
                            .frame(width: min(clearSize, 32 * 1.6), height: min(clearSize, 32 * 1.6))
                            .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("清空问题")
                }
                Button(action: submit) {
                    Image(systemName: "arrow.up")
                        .font(.headline.weight(.bold))
                        .foregroundStyle(palette.onAccent)
                        .frame(width: min(sendSize, 36 * 1.6), height: min(sendSize, 36 * 1.6))
                        .background(Circle().fill(palette.accentGradient))
                }
                .buttonStyle(.plain)
                .disabled(trimmedDraft.isEmpty)
                .opacity(trimmedDraft.isEmpty ? 0.4 : 1)
                .accessibilityLabel("问一问")
            }
            .padding(.leading, 16)
            .padding(.trailing, 6)
            .padding(.vertical, 6)
            .background(.primary.opacity(0.07), in: .capsule)
            // 深色玻璃上底色几乎看不出，加一道细边标出输入框
            .overlay(Capsule().strokeBorder(.primary.opacity(focused ? 0.25 : 0.12), lineWidth: 1))

            if analysis == nil {
                VStack(alignment: .leading, spacing: 8) {
                    Text("试试这样问").font(.caption).subtleForeground()
                    CardHScroll {
                        HStack(spacing: 8) {
                            ForEach(Self.examples, id: \.self) { example in
                                Button {
                                    draft = example
                                    submit()
                                } label: {
                                    TagChip(text: example)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
            }
        }
        .glassCard(clipped: true)
    }

    private func submit() {
        let question = trimmedDraft
        guard !question.isEmpty else { return }
        focused = false
        let result = QuestionClassifier.analyze(question)
        analyzedText = question
        app.currentQuestion = question
        withAnimation(reduceMotion ? .easeInOut(duration: 0.2) : .spring(response: 0.45, dampingFraction: 0.85)) {
            analysis = result
        }
        Haptics.selection()
        Announce.post("\(result.kind.title)。推荐\(result.methods.map(\.name).joined(separator: "、"))")
    }

    private func go(_ method: AskMethod, analysis: QuestionAnalysis) {
        app.currentQuestion = analyzedText.isEmpty ? nil : analyzedText
        app.pendingWheelOptions = method == .wheel && analysis.options.count >= 2 ? analysis.options : nil
        app.pendingTarotSpread = method.tarotSpread
        Haptics.selection()
        app.paths[.ask, default: []].append(method.route)
    }
}

/// 问一问里的大分区标题
private struct AskSectionTitle: View {
    let title: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title).font(.title2.bold())
            Text(subtitle).font(.caption).subtleForeground()
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

// MARK: - 推荐结果

private struct AskResultCard: View {
    let analysis: QuestionAnalysis
    let onPick: (AskMethod) -> Void

    @Environment(\.palette) private var palette

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: analysis.kind.symbol)
                    .font(.title2)
                    .foregroundStyle(palette.accentGradient)
                    .frame(width: 32)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 3) {
                    Text("「\(analysis.kind.title)」，推荐这样决定").font(.headline)
                    Text(analysis.kind.hint).font(.caption).subtleForeground()
                }
            }
            .accessibilityElement(children: .combine)

            if !analysis.options.isEmpty {
                CardHScroll {
                    HStack(spacing: 6) {
                        ForEach(Array(analysis.options.enumerated()), id: \.offset) { _, option in
                            TagChip(text: option)
                        }
                    }
                }
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("选项：\(analysis.options.joined(separator: "、"))")
            }

            VStack(spacing: 10) {
                ForEach(Array(analysis.methods.enumerated()), id: \.element) { index, method in
                    let reason = method.reason(for: analysis)
                    Button {
                        onPick(method)
                    } label: {
                        HStack(spacing: 12) {
                            ToolIcon(symbol: method.symbol, colors: palette.identity(method.colors), size: 40)
                            VStack(alignment: .leading, spacing: 3) {
                                // 一行放不下（超大字号）时「推荐」标签换到名字下面，名字不被挤断
                                ViewThatFits(in: .horizontal) {
                                    HStack(spacing: 6) {
                                        Text(method.name).font(.headline).foregroundStyle(.primary)
                                        if index == 0 {
                                            TagChip(text: "推荐", symbol: "hand.thumbsup.fill")
                                        }
                                    }
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text(method.name).font(.headline).foregroundStyle(.primary)
                                        if index == 0 {
                                            TagChip(text: "推荐", symbol: "hand.thumbsup.fill")
                                        }
                                    }
                                }
                                Text(reason)
                                    .font(.caption)
                                    .subtleForeground()
                                    .multilineTextAlignment(.leading)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            Spacer(minLength: 0)
                            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                        }
                        .padding(12)
                        .background(index == 0 ? palette.accent.opacity(palette.isDark ? 0.16 : 0.12) : Color.primary.opacity(0.05),
                                    in: .rect(cornerRadius: 18))
                        .contentShape(.rect(cornerRadius: 18))
                    }
                    .buttonStyle(.pressableCard)
                    .accessibilityLabel(index == 0 ? "推荐：\(method.name)" : method.name)
                    .accessibilityHint(reason)
                }
            }
        }
        .glassCard(clipped: true)
    }
}

// MARK: - 类型与方法的展示

extension QuestionKind {
    var title: String {
        switch self {
        case .yesNo: "是非题"
        case .choice: "多选一"
        case .food: "吃什么"
        case .timing: "挑时机"
        case .relationship: "感情事"
        case .general: "随便问问"
        }
    }

    var hint: String {
        switch self {
        case .yesNo: "答案只有「是」或「否」，交给运气最干脆"
        case .choice: "在几个选项里挑一个"
        case .food: "吃饭的事，交给美食库和转盘"
        case .timing: "关于什么时候，看看签文和黄历"
        case .relationship: "关于两个人，听听塔罗和缘分怎么说"
        case .general: "没想好怎么问？先翻一页答案之书"
        }
    }

    var symbol: String {
        switch self {
        case .yesNo: "checkmark.circle.fill"
        case .choice: "list.bullet.circle.fill"
        case .food: "fork.knife.circle.fill"
        case .timing: "calendar.circle.fill"
        case .relationship: "heart.circle.fill"
        case .general: "sparkles"
        }
    }
}

extension AskMethod {
    var name: String {
        switch self {
        case .coin: ToolKind.coin.name
        case .answers: "答案之书"
        case .tarotSingle: "塔罗 · 单张指引"
        case .tarotTime: "塔罗 · 时间之流"
        case .tarotLove: "塔罗 · 情感关系"
        case .wheel: ToolKind.wheel.name
        case .food: ToolKind.food.name
        case .finger: ToolKind.finger.name
        case .stick: "求签"
        case .auspicious: "择吉日"
        case .compatibility: "缘分配对"
        case .dice: ToolKind.dice.name
        }
    }

    var route: AppRoute {
        switch self {
        case .coin: .tool(.coin)
        case .answers: .answers
        case .tarotSingle, .tarotTime, .tarotLove: .tarot
        case .wheel: .tool(.wheel)
        case .food: .tool(.food)
        case .finger: .tool(.finger)
        case .stick: .stick
        case .auspicious: .auspicious
        case .compatibility: .compatibility
        case .dice: .tool(.dice)
        }
    }

    /// 塔罗类方法预选的牌阵
    var tarotSpread: TarotSpread? {
        switch self {
        case .tarotSingle: .single
        case .tarotTime: .time
        case .tarotLove: .love
        default: nil
        }
    }

    var symbol: String {
        switch self {
        case .coin: ToolKind.coin.symbol
        case .answers: "book.fill"
        case .tarotSingle: TarotSpread.single.symbol
        case .tarotTime: TarotSpread.time.symbol
        case .tarotLove: TarotSpread.love.symbol
        case .wheel: ToolKind.wheel.symbol
        case .food: ToolKind.food.symbol
        case .finger: ToolKind.finger.symbol
        case .stick: "flame.fill"
        case .auspicious: "calendar.badge.checkmark"
        case .compatibility: "heart.circle.fill"
        case .dice: ToolKind.dice.symbol
        }
    }

    var colors: [Color] {
        switch self {
        case .coin: ToolKind.coin.colors
        case .answers: [Color(hex: 0xF5B83D), Color(hex: 0xB45309)]
        case .tarotSingle, .tarotTime, .tarotLove: [Color(hex: 0x8B5CF6), Color(hex: 0x3B82F6)]
        case .wheel: ToolKind.wheel.colors
        case .food: ToolKind.food.colors
        case .finger: ToolKind.finger.colors
        case .stick: [Color(hex: 0xFF8A3D), Color(hex: 0xE11D48)]
        case .auspicious: [Color(hex: 0xEF4444), Color(hex: 0xF59E0B)]
        case .compatibility: [Color(hex: 0xFB7185), Color(hex: 0xF43F5E)]
        case .dice: ToolKind.dice.colors
        }
    }

    /// 推荐理由，一句话
    func reason(for analysis: QuestionAnalysis) -> String {
        let options = analysis.options
        switch self {
        case .coin:
            if options.count == 2 { return "正面「\(options[0])」，反面「\(options[1])」，抛一次最干脆" }
            return analysis.kind == .yesNo ? "最快的办法：正面就做，反面就算" : "拿不准时，让硬币替你迈出第一步"
        case .answers:
            return analysis.kind == .relationship ? "心里其实有答案时，翻一页印证一下" : "默念问题翻开一页，听听书怎么说"
        case .tarotSingle: return "想听点理由时，抽一张牌看看提示"
        case .tarotTime: return "三张牌看过去、现在和未来，找准时机"
        case .tarotLove: return "三张牌看你、对方和这段关系的走向"
        case .wheel:
            return options.count >= 2 ? "把这 \(options.count) 个选项放上转盘，转到哪个就是哪个" : "把候选写上转盘，转到哪个算哪个"
        case .food: return "按时段和口味，从美食库里挑一道"
        case .finger: return "大家各按一根手指，随机选出一个"
        case .stick: return analysis.kind == .timing ? "先求一支签，看看最近的时运" : "求一支签，看看今天的指引"
        case .auspicious: return "从黄历里挑出最近适合的好日子"
        case .compatibility: return "看看你们的星座和生肖合不合"
        case .dice:
            return (3...6).contains(options.count) ? "给 \(options.count) 个选项编号，掷出几点选几号" : "掷个骰子，把选择交给点数"
        }
    }
}
