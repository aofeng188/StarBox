import SwiftUI
import SwiftData

struct TarotCard: Decodable, Identifiable, Hashable {
    let id: Int
    let name: String
    let en: String
    let roman: String
    let symbol: String
    let keywords: [String]
    let upright: String
    let reversed: String
    let love: String
    let career: String
    let advice: String
    // 逆位释义。字段缺失时界面隐藏对应内容，绝不拿正位的顶替
    let keywordsReversed: [String]?
    let loveReversed: String?
    let careerReversed: String?
    let adviceReversed: String?
    /// 小阿卡纳的花色（wands / cups / swords / pentacles）；大阿卡纳没有
    let suit: String?

    /// 完整牌组：22 张大阿卡纳在前，之后是 56 张小阿卡纳
    static let deck: [TarotCard] = ContentLibrary.load("tarot")
    static let majorArcana: [TarotCard] = deck.filter(\.isMajor)

    /// 洗牌用的牌组：完整 78 张，或只用 22 张大阿卡纳
    static func drawingDeck(fullDeck: Bool) -> [TarotCard] {
        fullDeck || majorArcana.isEmpty ? deck : majorArcana
    }

    var isMajor: Bool { suit == nil }
    var suitKind: TarotSuit? { suit.flatMap(TarotSuit.init(rawValue:)) }

    /// 牌号位置显示的字：大阿卡纳是罗马数字，小阿卡纳是 A、2…10、侍从、骑士、王后、国王
    var rankText: String {
        guard !isMajor else { return roman }
        switch roman {
        case "Page": return "侍从"
        case "Knight": return "骑士"
        case "Queen": return "王后"
        case "King": return "国王"
        default: return roman
        }
    }

    /// 每张牌的主色调：大阿卡纳限定在深蓝 → 紫 → 品红的「宝石色」区间，与星空主题协调；小阿卡纳用花色主色
    var hue: Double { 0.55 + Double((id * 7) % 22) / 22.0 * 0.4 }
    var colors: [Color] {
        if let suit = suitKind { return suit.colors }
        return [Color(hue: hue, saturation: 0.7, brightness: 0.5), Color(hue: (hue + 0.06).truncatingRemainder(dividingBy: 1), saturation: 0.8, brightness: 0.2)]
    }

    /// 牌面描边、牌号等点缀色
    var accent: Color { suitKind?.accent ?? Color(hex: 0xF5D78E) }

    /// 按正逆位取关键词；逆位缺字段时为 nil
    func shownKeywords(isReversed: Bool) -> [String]? {
        isReversed ? keywordsReversed : keywords
    }

    /// 按正逆位取感情 / 事业 / 建议释义；逆位缺字段时为 nil
    func text(for aspect: TarotAspect, isReversed: Bool) -> String? {
        switch aspect {
        case .love: return isReversed ? loveReversed : love
        case .career: return isReversed ? careerReversed : career
        case .advice: return isReversed ? adviceReversed : advice
        }
    }
}

/// 小阿卡纳的四个花色
enum TarotSuit: String, CaseIterable {
    case wands, cups, swords, pentacles

    var name: String {
        switch self {
        case .wands: "权杖"
        case .cups: "圣杯"
        case .swords: "宝剑"
        case .pentacles: "星币"
        }
    }

    /// 花色图标：权杖属火、圣杯是酒杯、宝剑属风、星币是带星的圆币
    var symbol: String {
        switch self {
        case .wands: "flame.fill"
        case .cups: "wineglass.fill"
        case .swords: "wind"
        case .pentacles: "star.circle.fill"
        }
    }

    /// 牌面渐变：权杖火红、圣杯水蓝、宝剑银灰、星币金黄
    var colors: [Color] {
        switch self {
        case .wands: [Color(hex: 0xB83A1B), Color(hex: 0x3A0D05)]
        case .cups: [Color(hex: 0x1E6FB8), Color(hex: 0x0A1E45)]
        case .swords: [Color(hex: 0x6B7686), Color(hex: 0x1C222C)]
        case .pentacles: [Color(hex: 0xA97A12), Color(hex: 0x3A2806)]
        }
    }

    /// 压在牌面深色渐变上的点缀色
    var accent: Color {
        switch self {
        case .wands: Color(hex: 0xFFB48A)
        case .cups: Color(hex: 0xA5DBFF)
        case .swords: Color(hex: 0xE5E9F0)
        case .pentacles: Color(hex: 0xFFE08A)
        }
    }
}

/// 牌面主释义之外补充的一行
enum TarotAspect {
    case love, career, advice

    var label: String {
        switch self {
        case .love: "感情"
        case .career: "事业"
        case .advice: "建议"
        }
    }

    /// 从问题里粗略判断话题；没提到感情或工作时返回 nil
    static func topic(of question: String) -> TarotAspect? {
        let love = ["感情", "恋", "喜欢", "爱情", "对象", "男友", "女友", "男朋友", "女朋友", "前任", "暧昧", "复合", "表白", "结婚", "分手", "桃花", "脱单", "约会"]
        let career = ["工作", "事业", "职", "面试", "升职", "加薪", "跳槽", "老板", "同事", "项目", "生意", "创业", "offer"]
        if love.contains(where: { question.contains($0) }) { return .love }
        if career.contains(where: { question.contains($0) }) { return .career }
        return nil
    }
}

enum TarotSpread: String, CaseIterable, Identifiable {
    case single, time, love, advice

    var id: String { rawValue }

    var name: String {
        switch self {
        case .single: "单张指引"
        case .time: "时间之流"
        case .love: "情感关系"
        case .advice: "问题解答"
        }
    }

    var positions: [String] {
        switch self {
        case .single: ["今日指引"]
        case .time: ["过去", "现在", "未来"]
        case .love: ["你的状态", "对方的状态", "关系走向"]
        case .advice: ["现状", "阻碍", "建议"]
        }
    }

    var detail: String {
        switch self {
        case .single: "抽一张牌，获得今天的提示"
        case .time: "看清事情的来龙去脉"
        case .love: "了解两个人之间的能量"
        case .advice: "针对一个具体问题给出方向"
        }
    }

    var symbol: String {
        switch self {
        case .single: "rectangle.portrait"
        case .time: "clock.arrow.circlepath"
        case .love: "heart.text.square"
        case .advice: "questionmark.bubble"
        }
    }

    func position(_ slot: Int) -> String {
        positions.indices.contains(slot) ? positions[slot] : "第 \(slot + 1) 张"
    }

    /// 每个牌位补充哪一行：收尾的牌给建议，情感牌阵的两人状态给感情，
    /// 「现在 / 现状」按问题的话题补感情或事业，问题没提到就不补，免得每张牌都挂同一类话
    func aspect(at slot: Int, question: String) -> TarotAspect? {
        switch self {
        case .single: return .advice
        case .time: return slot == 2 ? .advice : (slot == 1 ? TarotAspect.topic(of: question) : nil)
        case .love: return slot == 2 ? .advice : .love
        case .advice: return slot == 2 ? .advice : (slot == 0 ? TarotAspect.topic(of: question) : nil)
        }
    }
}

struct TarotView: View {
    enum Phase { case setup, shuffling, picking, reading }

    struct DrawnCard: Hashable {
        let deckIndex: Int
        let card: TarotCard
        let reversed: Bool
    }

    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Environment(AppSettings.self) private var settings
    @Namespace private var namespace

    @State private var phase: Phase = .setup
    @State private var spread: TarotSpread = .time
    @State private var question = ""
    @State private var deck: [DrawnCard] = []
    @State private var picked: [DrawnCard] = []
    @State private var flipped: Set<Int> = []
    /// 已经安排翻开的牌位。翻牌有延迟，先同步占位，同一张牌不会被安排两次，记录也就只存一次
    @State private var scheduled: Set<Int> = []
    @State private var shuffleSeed = 0
    @State private var fanned = false
    @State private var confetti = 0
    /// 洗牌、入场这类延时流程，重新开始时取消
    @State private var flow: Task<Void, Never>?
    /// 局次号：重新开始或重新洗牌后加一，上一局遗留的延时任务据此作废
    @State private var generation = 0

    var body: some View {
        ZStack {
            switch phase {
            case .setup: setup.transition(.opacity)
            case .shuffling, .picking: table
            case .reading: reading.transition(.opacity)
            }
            ConfettiView(trigger: confetti)
        }
        .cosmicBackground()
        .navigationTitle("塔罗占卜")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                NavigationLink(value: AppRoute.tarotJournal) {
                    Label("塔罗手记", systemImage: "book.closed")
                }
            }
            if phase == .reading, !picked.isEmpty, flipped.count == picked.count {
                ToolbarItem(placement: .topBarTrailing) {
                    ShareCardButton(title: "分享解读", previewTitle: "塔罗 · \(spread.name)") { style in
                        ShareCardFrame(style: style, title: "星匣 · 塔罗占卜") {
                            TarotShareContent(spread: spread, question: question, entries: picked, style: style)
                        }
                    }
                }
            }
            if phase != .setup {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("重新开始", systemImage: "arrow.counterclockwise") { reset() }
                }
            }
        }
        .tarotParallax(isActive: phase == .reading)
        .prefillsQuestion($question)
        .onAppear {
            // 问一问推荐了具体牌阵时预选
            if phase == .setup, let pending = AppState.shared.takePendingTarotSpread() { spread = pending }
        }
    }

    // MARK: 选择牌阵

    private var setup: some View {
        ScrollView {
            VStack(spacing: 18) {
                TarotBackMini().scaleEffect(1.6).frame(height: 140).padding(.top, 20)
                Text("静下心来，想一个问题").font(AppFont.serif(24, weight: .bold))
                TextField("你想问什么？（可不填）", text: $question, axis: .vertical)
                    .lineLimit(1...3)
                    .padding(14)
                    .glassEffect(.regular, in: .rect(cornerRadius: 18))

                // 内容里有小阿卡纳时才给选牌组
                if TarotCard.deck.count > TarotCard.majorArcana.count {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("牌组").font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
                        Picker("牌组", selection: Binding(get: { settings.tarotFullDeck }, set: { settings.tarotFullDeck = $0 })) {
                            Text("大阿卡纳 \(TarotCard.majorArcana.count) 张").tag(false)
                            Text("完整 \(TarotCard.deck.count) 张").tag(true)
                        }
                        .pickerStyle(.segmented)
                        .onChange(of: settings.tarotFullDeck) { Haptics.selection() }
                        Text(settings.tarotFullDeck ? "含 \(TarotCard.deck.count - TarotCard.majorArcana.count) 张小阿卡纳，日常小事也能看得更细" : "只用大阿卡纳，聚焦人生的大方向")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }

                VStack(spacing: 10) {
                    ForEach(TarotSpread.allCases) { item in
                        Button {
                            spread = item
                            Haptics.selection()
                        } label: {
                            HStack(spacing: 14) {
                                Image(systemName: item.symbol)
                                    .font(.title3)
                                    .frame(width: 40, height: 40)
                                    .foregroundStyle(spread == item ? palette.onAccent : palette.ink(palette.accent))
                                    .background(Circle().fill(spread == item ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(palette.accent.opacity(0.12))))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("\(item.name) · \(item.positions.count) 张").font(.headline).foregroundStyle(.primary)
                                    Text(item.detail).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if spread == item {
                                    Image(systemName: "checkmark.circle.fill").foregroundStyle(palette.accent)
                                        .accessibilityHidden(true)
                                }
                            }
                            .glassCard(cornerRadius: 22, padding: 14, interactive: true)
                        }
                        .buttonStyle(.pressableCard)
                        .accessibilityAddTraits(spread == item ? .isSelected : [])
                    }
                }

                BigActionButton(title: "开始洗牌", symbol: "sparkles") { startShuffle() }
                    .padding(.top, 6)
            }
            .pageContent(padding: 20)
            .padding(.bottom, 30)
        }
    }

    // MARK: 洗牌 / 抽牌

    private var table: some View {
        GeometryReader { proxy in
            // 矮屏（Duo 外屏、展开横持）上把牌位和间距按高度收一收；宽屏上扇形不跟着无限摊开
            let k = min(1, max(0.62, proxy.size.height / 540))
            let width = min(proxy.size.width, 560)
            VStack(spacing: 0) {
                VStack(spacing: 6) {
                    Text(phase == .shuffling ? "洗牌中…" : "凭直觉抽取 \(spread.positions.count) 张牌")
                        .font(AppFont.serif(22, weight: .bold))
                    if phase == .picking {
                        Text("已选 \(picked.count) / \(spread.positions.count)").font(.subheadline).foregroundStyle(.secondary)
                            .contentTransition(.numericText())
                    }
                }
                .padding(.top, 16 * k)

                // 牌位
                HStack(spacing: 14) {
                    ForEach(0..<spread.positions.count, id: \.self) { slot in
                        VStack(spacing: 6) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 12)
                                    .strokeBorder(style: StrokeStyle(lineWidth: 1.5, dash: [6, 5]))
                                    .foregroundStyle(palette.accent.opacity(0.5))
                                if slot < picked.count {
                                    TarotCardBack()
                                        .matchedGeometryEffect(id: picked[slot].deckIndex, in: namespace)
                                }
                            }
                            .frame(width: 84 * k, height: 136 * k)
                            Text(spread.positions[slot]).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(.top, 24 * k)

                Spacer(minLength: 0)

                // 扇形牌堆
                ZStack {
                    ForEach(Array(deck.enumerated()), id: \.element.deckIndex) { index, entry in
                        if !picked.contains(entry) {
                            let count = Double(deck.count)
                            let t = count > 1 ? Double(index) / (count - 1) - 0.5 : 0
                            let angle = fanned ? t * 100 : 0
                            var rng = SeededGenerator("shuffle", shuffleSeed, index)
                            let jitter = phase == .shuffling ? CGSize(width: rng.double(-90...90), height: rng.double(-30...30)) : .zero
                            TarotCardBack()
                                .matchedGeometryEffect(id: entry.deckIndex, in: namespace)
                                .frame(width: 64, height: 104)
                                .rotationEffect(.degrees(angle + (phase == .shuffling ? rng.double(-25...25) : 0)), anchor: .bottom)
                                .offset(x: fanned ? t * width * 0.42 : jitter.width,
                                        y: fanned ? abs(t) * 40 : jitter.height)
                                .zIndex(Double(index))
                                .onTapGesture { pick(entry) }
                                .accessibilityLabel("第 \(index + 1) 张牌")
                                .accessibilityAddTraits(.isButton)
                        }
                    }
                }
                .frame(height: 220 * max(k, 0.8))
                .padding(.bottom, 60 * k)
            }
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: 解读

    private var reading: some View {
        ScrollView {
            VStack(spacing: 18) {
                if !question.isEmpty {
                    Text("「\(question)」").font(AppFont.serif(18)).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
                HStack(alignment: .top, spacing: 12) {
                    ForEach(Array(picked.enumerated()), id: \.offset) { slot, entry in
                        VStack(spacing: 8) {
                            FlippableTarotCard(entry: entry, isFaceUp: flipped.contains(slot))
                                .frame(width: spread == .single ? 180 : 106, height: spread == .single ? 292 : 172)
                                .onTapGesture { flip(slot) }
                            Text(spread.position(slot)).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(.top, 12)

                if flipped.count < picked.count {
                    Button("全部翻开", systemImage: "rectangle.on.rectangle.angled") {
                        for slot in picked.indices { flip(slot, delay: Double(slot) * 0.25) }
                    }
                    .buttonStyle(.glass)
                    .disabled(scheduled.count >= picked.count)
                    Text("点按牌面逐张翻开").font(.caption).foregroundStyle(.secondary)
                } else {
                    TarotInterpretations(spread: spread, question: question, entries: picked)
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
    }

    // MARK: 动作

    private func startShuffle() {
        flow?.cancel()
        generation += 1
        let current = generation
        deck = TarotCard.drawingDeck(fullDeck: settings.tarotFullDeck).shuffled().enumerated()
            .map { DrawnCard(deckIndex: $0.offset, card: $0.element, reversed: Bool.random()) }
        picked = []
        flipped = []
        scheduled = []
        fanned = false
        withAnimation(.snappy) { phase = .shuffling }
        flow = Task {
            for i in 0..<6 {
                try? await Task.sleep(for: .milliseconds(220))
                guard !Task.isCancelled, current == generation else { return }
                withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { shuffleSeed = i + 1 }
                SoundEngine.shared.play(.cardFlip, volume: 0.6)
                Haptics.softImpact(0.5)
            }
            try? await Task.sleep(for: .milliseconds(250))
            guard !Task.isCancelled, current == generation else { return }
            withAnimation(.spring(response: 0.8, dampingFraction: 0.8)) {
                phase = .picking
                fanned = true
            }
            SoundEngine.shared.play(.whoosh)
        }
    }

    private func pick(_ entry: DrawnCard) {
        guard phase == .picking, picked.count < spread.positions.count, !picked.contains(entry) else { return }
        Haptics.impact(0.6)
        SoundEngine.shared.play(.cardFlip)
        withAnimation(.spring(response: 0.55, dampingFraction: 0.8)) { picked.append(entry) }
        if picked.count == spread.positions.count {
            let current = generation
            flow = Task {
                try? await Task.sleep(for: .milliseconds(700))
                guard !Task.isCancelled, current == generation else { return }
                withAnimation(.easeInOut(duration: 0.5)) { phase = .reading }
            }
        }
    }

    private func flip(_ slot: Int, delay: Double = 0) {
        guard phase == .reading, !flipped.contains(slot), !scheduled.contains(slot) else { return }
        scheduled.insert(slot)
        let current = generation
        Task {
            if delay > 0 { try? await Task.sleep(for: .seconds(delay)) }
            guard current == generation else { return }
            withAnimation(.spring(response: 0.7, dampingFraction: 0.75)) { _ = flipped.insert(slot) }
            SoundEngine.shared.play(.cardFlip)
            Haptics.impact(0.8)
            // 每个牌位只会翻一次，只有翻开最后一张的那次会走到这里
            guard flipped.count == picked.count else { return }
            save()
            try? await Task.sleep(for: .milliseconds(400))
            guard current == generation else { return }
            SoundEngine.shared.play(.sparkle)
            if picked.allSatisfy({ !$0.reversed }) { confetti += 1 }
            Announce.post(picked.enumerated().map { index, item in
                "\(spread.position(index))，\(item.card.name)，\(item.reversed ? "逆位" : "正位")"
            }.joined(separator: "；"))
        }
    }

    private func save() {
        let cards = picked.map { "\($0.card.id)\($0.reversed ? "r" : "u")" }.joined(separator: ",")
        context.insert(TarotReading(spread: spread.rawValue, cards: cards, question: question))
        let result = picked.map { "\($0.card.name)\($0.reversed ? "(逆)" : "")" }.joined(separator: " · ")
        HistoryLog.add(context, tool: "tarot", title: spread.name, result: result, detail: question.isEmpty ? nil : question)
    }

    private func reset() {
        flow?.cancel()
        flow = nil
        generation += 1
        withAnimation(.snappy) {
            phase = .setup
            picked = []
            flipped = []
            scheduled = []
            fanned = false
        }
    }
}

// MARK: - 解读（塔罗页和塔罗手记共用）

/// 逐张释义 + 综合解读
struct TarotInterpretations: View {
    let spread: TarotSpread
    let question: String
    let entries: [TarotView.DrawnCard]

    var body: some View {
        VStack(spacing: 18) {
            ForEach(Array(entries.enumerated()), id: \.offset) { slot, entry in
                TarotInterpretationCard(spread: spread, slot: slot, entry: entry, question: question)
            }
            if !entries.isEmpty {
                TarotSummaryCard(entries: entries)
            }
        }
    }
}

struct TarotInterpretationCard: View {
    let spread: TarotSpread
    let slot: Int
    let entry: TarotView.DrawnCard
    let question: String

    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let card = entry.card
        let aspect = spread.aspect(at: slot, question: question)
        let extra = aspect.flatMap { card.text(for: $0, isReversed: entry.reversed) }
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(spread.position(slot)).font(.caption.weight(.bold))
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(palette.accent.opacity(0.2), in: .capsule)
                if let suit = card.suitKind {
                    Image(systemName: suit.symbol)
                        .font(.subheadline)
                        .foregroundStyle(palette.ink(suit.colors[0]))
                        .accessibilityHidden(true)
                    Text(card.name).font(.headline)
                } else {
                    Text("\(card.roman) · \(card.name)").font(.headline)
                }
                Text(entry.reversed ? "逆位" : "正位")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(entry.reversed ? Color.negative(scheme) : Color.positive(scheme))
                Spacer()
            }
            if let keywords = card.shownKeywords(isReversed: entry.reversed), !keywords.isEmpty {
                HStack(spacing: 6) {
                    ForEach(keywords, id: \.self) { TagChip(text: $0) }
                }
            }
            Text(entry.reversed ? card.reversed : card.upright).font(.subheadline).lineSpacing(3)
            if let aspect, let extra {
                Text("\(aspect.label)：\(extra)").font(.subheadline).foregroundStyle(.secondary)
            }
        }
        .glassCard()
    }
}

struct TarotSummaryCard: View {
    let entries: [TarotView.DrawnCard]

    /// 按正逆位的比例给一句综合解读（分享卡也用）
    static func summary(for entries: [TarotView.DrawnCard]) -> String {
        let uprightCount = entries.filter { !$0.reversed }.count
        if entries.count == 1 {
            return entries[0].reversed ? "逆位提醒你放慢脚步，先处理内心的阻碍。" : "正位的能量清晰明亮，跟着这张牌的指引行动吧。"
        } else if uprightCount == entries.count {
            return "全部为正位，整体能量顺畅积极，是推进事情的好时机。"
        } else if uprightCount == 0 {
            return "全部为逆位，当下阻力较多，宜先调整心态、等待时机。"
        } else if uprightCount > entries.count / 2 {
            return "正位居多，整体趋势向好，留意逆位牌提示的小问题即可。"
        } else {
            return "逆位居多，事情尚有阻碍，不妨换个角度重新审视。"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            SectionHeader(title: "综合解读", symbol: "sparkles")
            Text(Self.summary(for: entries)).font(.body).lineSpacing(4)
            Text("塔罗是一面映照内心的镜子，答案最终在你自己手中。")
                .font(.caption).foregroundStyle(.secondary)
        }
        .glassCard()
    }
}

// MARK: - 牌面视差

/// 牌面随手机倾斜的视差要用陀螺仪：只在页面可见、isActive 且没开「减弱动态效果」时开启，start / stop 成对
private struct TarotParallaxModifier: ViewModifier {
    let isActive: Bool
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isVisible = false
    @State private var running = false

    func body(content: Content) -> some View {
        content
            .onAppear { isVisible = true; sync() }
            .onDisappear { isVisible = false; sync() }
            .onChange(of: isActive) { sync() }
            .onChange(of: reduceMotion) { sync() }
    }

    private func sync() {
        let wanted = isVisible && isActive && !reduceMotion
        guard wanted != running else { return }
        running = wanted
        if wanted { MotionManager.shared.start() } else { MotionManager.shared.stop() }
    }
}

extension View {
    func tarotParallax(isActive: Bool = true) -> some View {
        modifier(TarotParallaxModifier(isActive: isActive))
    }
}

// MARK: - 卡牌视图

struct FlippableTarotCard: View {
    let entry: TarotView.DrawnCard
    let isFaceUp: Bool

    var body: some View {
        let angle: Double = isFaceUp ? 180 : 0
        ZStack {
            TarotCardBack()
                .modifier(FlipVisibility(angle: angle, showsWhenFlipped: false))
            TarotCardFace(card: entry.card)
                .rotationEffect(.degrees(entry.reversed ? 180 : 0))
                .rotation3DEffect(.degrees(180), axis: (0, 1, 0))
                .modifier(FlipVisibility(angle: angle, showsWhenFlipped: true))
        }
        .rotation3DEffect(.degrees(isFaceUp ? 180 : 0), axis: (0, 1, 0), perspective: 0.5)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(isFaceUp ? "\(entry.card.name)，\(entry.reversed ? "逆位" : "正位")" : "背面朝上的牌，点按翻开")
        .accessibilityAddTraits(isFaceUp ? [] : .isButton)
    }
}

/// 翻转动画过半（90°）时才切换正反面，避免两面交叉淡入淡出
struct FlipVisibility: ViewModifier, Animatable {
    var angle: Double
    let showsWhenFlipped: Bool

    var animatableData: Double {
        get { angle }
        set { angle = newValue }
    }

    func body(content: Content) -> some View {
        content.opacity((angle > 90) == showsWhenFlipped ? 1 : 0)
    }
}

struct TarotCardFace: View {
    let card: TarotCard
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        GeometryReader { proxy in
            let w = proxy.size.width
            TimelineView(.animation(minimumInterval: 1.0 / 30, paused: reduceMotion)) { context in
                let t = context.date.timeIntervalSinceReferenceDate
                // 减弱动态效果时不读陀螺仪、不做视差，只留静态的镭射光泽
                let tilt: CGSize = reduceMotion ? .zero : CGSize(width: MotionManager.shared.roll + sin(t * 0.7) * 0.35,
                                                                 height: MotionManager.shared.pitch + cos(t * 0.5) * 0.25)
                TarotCardArt(card: card, width: w)
                    .holoFoil(tilt: tilt, strength: 0.9)
                    .rotation3DEffect(.degrees(tilt.width * 6), axis: (0, 1, 0))
                    .rotation3DEffect(.degrees(-tilt.height * 6), axis: (1, 0, 0))
            }
        }
        .shadow(color: card.colors[0].opacity(0.6), radius: 14)
    }
}

/// 静态的牌面图案。塔罗页在外面加镭射光泽和视差，分享卡直接用它（导出图片时不跑动画和着色器）。
/// 小阿卡纳用花色主色和花色图标，牌号位置写 A、2…10、侍从、骑士、王后、国王
struct TarotCardArt: View {
    let card: TarotCard
    /// 牌宽，字号、描边按它缩放
    let width: CGFloat

    var body: some View {
        let w = width
        let accent = card.accent
        ZStack {
            RoundedRectangle(cornerRadius: w * 0.09)
                .fill(LinearGradient(colors: card.colors, startPoint: .top, endPoint: .bottom))
            // 星点
            Canvas { ctx, size in
                var rng = SeededGenerator("tarot-face", card.id)
                for _ in 0..<30 {
                    let r = rng.double(0.4...1.4)
                    ctx.fill(Path(ellipseIn: CGRect(x: rng.unit() * size.width, y: rng.unit() * size.height, width: r * 2, height: r * 2)),
                             with: .color(.white.opacity(rng.double(0.3...0.9))))
                }
            }
            if let suit = card.suitKind {
                // 花色图标衬底
                Image(systemName: suit.symbol)
                    .font(.system(size: w * 0.62))
                    .foregroundStyle(accent.opacity(0.1))
            }
            RoundedRectangle(cornerRadius: w * 0.07)
                .strokeBorder(accent, lineWidth: max(1, w * 0.012))
                .padding(w * 0.05)
            VStack(spacing: w * 0.05) {
                if let suit = card.suitKind {
                    HStack(spacing: w * 0.03) {
                        Image(systemName: suit.symbol)
                        Spacer(minLength: 0)
                        Text(card.rankText)
                            .lineLimit(1)
                            .minimumScaleFactor(0.6)
                        Spacer(minLength: 0)
                        Image(systemName: suit.symbol)
                    }
                    .font(.system(size: w * 0.09, weight: .semibold, design: .serif))
                    .foregroundStyle(accent)
                } else {
                    Text(card.roman)
                        .font(.system(size: w * 0.11, weight: .semibold, design: .serif))
                        .foregroundStyle(accent)
                }
                Spacer()
                Image(systemName: card.symbol)
                    .font(.system(size: w * 0.36, weight: .light))
                    .foregroundStyle(LinearGradient(colors: [.white, accent], startPoint: .top, endPoint: .bottom))
                    .shadow(color: .white.opacity(0.6), radius: w * 0.06)
                Spacer()
                Text(card.name)
                    .font(AppFont.serif(w * 0.13, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text(card.en.uppercased())
                    .font(.system(size: w * 0.055, weight: .medium, design: .serif))
                    .tracking(1)
                    .foregroundStyle(accent.opacity(0.9))
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
            }
            .padding(.vertical, w * 0.12)
            .padding(.horizontal, w * 0.08)
        }
    }
}

struct TarotCardBack: View {
    var body: some View {
        GeometryReader { proxy in
            let w = proxy.size.width
            ZStack {
                RoundedRectangle(cornerRadius: w * 0.09)
                    .fill(LinearGradient(colors: [Color(hex: 0x1E1B4B), Color(hex: 0x0B0720)], startPoint: .top, endPoint: .bottom))
                Canvas { ctx, size in
                    let center = CGPoint(x: size.width / 2, y: size.height / 2)
                    let gold = Color(hex: 0xF5D78E)
                    for i in 1...3 {
                        let r = size.width * 0.12 * CGFloat(i)
                        ctx.stroke(Path(ellipseIn: CGRect(x: center.x - r, y: center.y - r, width: r * 2, height: r * 2)),
                                   with: .color(gold.opacity(0.55)), lineWidth: 0.8)
                    }
                    let star = StarShape(points: 8, innerRatio: 0.35).path(in: CGRect(x: center.x - size.width * 0.18, y: center.y - size.width * 0.18,
                                                                                     width: size.width * 0.36, height: size.width * 0.36))
                    ctx.fill(star, with: .color(gold.opacity(0.85)))
                    for i in 0..<12 {
                        let a = Double(i) / 12 * 2 * .pi
                        var ray = Path()
                        ray.move(to: CGPoint(x: center.x + cos(a) * size.width * 0.38, y: center.y + sin(a) * size.width * 0.38))
                        ray.addLine(to: CGPoint(x: center.x + cos(a) * size.width * 0.44, y: center.y + sin(a) * size.width * 0.44))
                        ctx.stroke(ray, with: .color(gold.opacity(0.6)), lineWidth: 0.8)
                    }
                    // 上下的月亮
                    for dy in [-1.0, 1.0] {
                        let c = CGPoint(x: center.x, y: center.y + dy * size.height * 0.34)
                        let r = size.width * 0.07
                        ctx.fill(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)), with: .color(gold.opacity(0.7)))
                    }
                }
                RoundedRectangle(cornerRadius: w * 0.07)
                    .strokeBorder(Color(hex: 0xF5D78E).opacity(0.8), lineWidth: max(0.8, w * 0.012))
                    .padding(w * 0.05)
            }
            .shadow(color: Color(hex: 0x6D28D9).opacity(0.5), radius: 6)
        }
    }
}

// MARK: - 分享卡

/// 塔罗解读分享卡的内容，放在 ShareCardFrame 里：牌阵名、问题、每张牌的牌名和正逆、综合解读一句话
struct TarotShareContent: View {
    let spread: TarotSpread
    let question: String
    let entries: [TarotView.DrawnCard]
    let style: ShareCardStyle

    var body: some View {
        let tall = style.isTall
        let asked = question.trimmingCharacters(in: .whitespacesAndNewlines)
        VStack(spacing: tall ? 18 : 12) {
            VStack(spacing: 6) {
                Text("\(spread.name) · \(entries.count) 张")
                    .font(.system(size: 12, weight: .semibold))
                    .tracking(2)
                    .foregroundStyle(style.palette.accentGradient)
                if !asked.isEmpty {
                    Text("「\(asked)」")
                        .font(AppFont.serif(tall ? 18 : 15))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .minimumScaleFactor(0.8)
                }
            }

            if entries.count == 1 && !tall, let entry = entries.first {
                singleRow(entry)
            } else {
                cardsRow(tall: tall)
            }

            Text(TarotSummaryCard.summary(for: entries))
                .font(.system(size: tall ? 15 : 13))
                .lineSpacing(3)
                .multilineTextAlignment(.center)
                .foregroundStyle(.white.opacity(0.92))
                .lineLimit(3)
                .minimumScaleFactor(0.85)
            if tall {
                Text("塔罗是一面映照内心的镜子，答案最终在你自己手中。")
                    .font(.system(size: 11))
                    .foregroundStyle(.white.opacity(0.55))
                    .multilineTextAlignment(.center)
            }
        }
    }

    /// 单张牌、3:4：牌在左，牌名、正逆、关键词和释义在右
    private func singleRow(_ entry: TarotView.DrawnCard) -> some View {
        let card = entry.card
        return HStack(alignment: .center, spacing: 16) {
            cardArt(entry, width: 108)
            VStack(alignment: .leading, spacing: 6) {
                Text(spread.position(0))
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.6))
                Text(card.name)
                    .font(AppFont.serif(20, weight: .bold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                orientation(entry)
                if let keywords = card.shownKeywords(isReversed: entry.reversed), !keywords.isEmpty {
                    Text(keywords.prefix(3).joined(separator: " · "))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(card.accent)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                Text(entry.reversed ? card.reversed : card.upright)
                    .font(.system(size: 12))
                    .lineSpacing(2)
                    .foregroundStyle(.white.opacity(0.85))
                    .lineLimit(4)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private func cardsRow(tall: Bool) -> some View {
        let single = entries.count == 1
        let width: CGFloat = single ? 136 : (tall ? 92 : 78)
        return HStack(alignment: .top, spacing: 10) {
            ForEach(Array(entries.enumerated()), id: \.offset) { slot, entry in
                VStack(spacing: 5) {
                    cardArt(entry, width: width)
                        .padding(.bottom, 3)
                    Text(spread.position(slot))
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.6))
                        .lineLimit(1)
                    Text(entry.card.name)
                        .font(AppFont.serif(14, weight: .bold))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                    orientation(entry)
                }
                .frame(width: max(width, 90))
            }
        }
    }

    private func cardArt(_ entry: TarotView.DrawnCard, width: CGFloat) -> some View {
        TarotCardArt(card: entry.card, width: width)
            .frame(width: width, height: width * 1.62)
            .rotationEffect(.degrees(entry.reversed ? 180 : 0))
            .shadow(color: entry.card.colors[0].opacity(0.6), radius: 10)
            .accessibilityHidden(true)
    }

    private func orientation(_ entry: TarotView.DrawnCard) -> some View {
        Text(entry.reversed ? "逆位" : "正位")
            .font(.system(size: 11, weight: .semibold))
            .foregroundStyle(entry.reversed ? Color.negative(.dark) : Color.positive(.dark))
    }
}
