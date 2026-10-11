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

    static let deck: [TarotCard] = ContentLibrary.load("tarot")

    /// 每张牌的主色调：限定在深蓝 → 紫 → 品红的「宝石色」区间，与星空主题协调
    var hue: Double { 0.55 + Double((id * 7) % 22) / 22.0 * 0.4 }
    var colors: [Color] {
        [Color(hue: hue, saturation: 0.7, brightness: 0.5), Color(hue: (hue + 0.06).truncatingRemainder(dividingBy: 1), saturation: 0.8, brightness: 0.2)]
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
    @Namespace private var namespace

    @State private var phase: Phase = .setup
    @State private var spread: TarotSpread = .time
    @State private var question = ""
    @State private var deck: [DrawnCard] = []
    @State private var picked: [DrawnCard] = []
    @State private var flipped: Set<Int> = []
    @State private var shuffleSeed = 0
    @State private var fanned = false
    @State private var confetti = 0

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
        .toolbar {
            if phase != .setup {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("重新开始", systemImage: "arrow.counterclockwise") { reset() }
                }
            }
        }
        .onAppear { MotionManager.shared.start() }
        .onDisappear { MotionManager.shared.stop() }
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
                                    .foregroundStyle(spread == item ? .white : palette.accent)
                                    .background(Circle().fill(spread == item ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(palette.accent.opacity(0.12))))
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("\(item.name) · \(item.positions.count) 张").font(.headline).foregroundStyle(.primary)
                                    Text(item.detail).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if spread == item { Image(systemName: "checkmark.circle.fill").foregroundStyle(palette.accent) }
                            }
                            .glassCard(cornerRadius: 22, padding: 14, interactive: true)
                        }
                        .buttonStyle(.pressableCard)
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
                            Text(spread.positions[slot]).font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(.top, 12)

                if flipped.count < picked.count {
                    Button("全部翻开", systemImage: "rectangle.on.rectangle.angled") {
                        for slot in picked.indices { flip(slot, delay: Double(slot) * 0.25) }
                    }
                    .buttonStyle(.glass)
                    Text("点按牌面逐张翻开").font(.caption).foregroundStyle(.secondary)
                } else {
                    ForEach(Array(picked.enumerated()), id: \.offset) { slot, entry in
                        interpretation(slot: slot, entry: entry)
                    }
                    summary
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
    }

    private func interpretation(slot: Int, entry: DrawnCard) -> some View {
        let card = entry.card
        let extra: (String, String)? = switch spread {
        case .love: ("感情", card.love)
        case .advice where slot == 2: ("建议", card.advice)
        case .single: ("建议", card.advice)
        default: nil
        }
        return VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(spread.positions[slot]).font(.caption.weight(.bold))
                    .padding(.horizontal, 8).padding(.vertical, 3)
                    .background(palette.accent.opacity(0.2), in: .capsule)
                Text("\(card.roman) · \(card.name)").font(.headline)
                Text(entry.reversed ? "逆位" : "正位")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(entry.reversed ? Color(hex: 0xFB7185) : Color(hex: 0x34D399))
                Spacer()
            }
            HStack(spacing: 6) {
                ForEach(card.keywords, id: \.self) { TagChip(text: $0) }
            }
            Text(entry.reversed ? card.reversed : card.upright).font(.subheadline).lineSpacing(3)
            if let extra {
                Text("\(extra.0)：\(extra.1)").font(.subheadline).foregroundStyle(.secondary)
            } else {
                Text("事业：\(card.career)").font(.subheadline).foregroundStyle(.secondary)
            }
        }
        .glassCard()
    }

    private var summary: some View {
        let uprightCount = picked.filter { !$0.reversed }.count
        let text: String = if picked.count == 1 {
            picked[0].reversed ? "逆位提醒你放慢脚步，先处理内心的阻碍。" : "正位的能量清晰明亮，跟着这张牌的指引行动吧。"
        } else if uprightCount == picked.count {
            "全部为正位，整体能量顺畅积极，是推进事情的好时机。"
        } else if uprightCount == 0 {
            "全部为逆位，当下阻力较多，宜先调整心态、等待时机。"
        } else if uprightCount > picked.count / 2 {
            "正位居多，整体趋势向好，留意逆位牌提示的小问题即可。"
        } else {
            "逆位居多，事情尚有阻碍，不妨换个角度重新审视。"
        }
        return VStack(alignment: .leading, spacing: 8) {
            SectionHeader(title: "综合解读", symbol: "sparkles")
            Text(text).font(.body).lineSpacing(4)
            Text("塔罗是一面映照内心的镜子，答案最终在你自己手中。")
                .font(.caption).foregroundStyle(.secondary)
        }
        .glassCard()
    }

    // MARK: 动作

    private func startShuffle() {
        deck = TarotCard.deck.shuffled().enumerated().map { DrawnCard(deckIndex: $0.offset, card: $0.element, reversed: Bool.random()) }
        picked = []
        flipped = []
        fanned = false
        withAnimation(.snappy) { phase = .shuffling }
        Task {
            for i in 0..<6 {
                try? await Task.sleep(for: .milliseconds(220))
                withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) { shuffleSeed = i + 1 }
                SoundEngine.shared.play(.cardFlip, volume: 0.6)
                Haptics.softImpact(0.5)
            }
            try? await Task.sleep(for: .milliseconds(250))
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
            Task {
                try? await Task.sleep(for: .milliseconds(700))
                withAnimation(.easeInOut(duration: 0.5)) { phase = .reading }
            }
        }
    }

    private func flip(_ slot: Int, delay: Double = 0) {
        guard !flipped.contains(slot) else { return }
        Task {
            try? await Task.sleep(for: .seconds(delay))
            withAnimation(.spring(response: 0.7, dampingFraction: 0.75)) { _ = flipped.insert(slot) }
            SoundEngine.shared.play(.cardFlip)
            Haptics.impact(0.8)
            if flipped.count == picked.count {
                try? await Task.sleep(for: .milliseconds(400))
                SoundEngine.shared.play(.sparkle)
                if picked.allSatisfy({ !$0.reversed }) { confetti += 1 }
                save()
            }
        }
    }

    private func save() {
        let cards = picked.map { "\($0.card.id)\($0.reversed ? "r" : "u")" }.joined(separator: ",")
        context.insert(TarotReading(spread: spread.rawValue, cards: cards, question: question))
        let result = picked.map { "\($0.card.name)\($0.reversed ? "(逆)" : "")" }.joined(separator: " · ")
        HistoryLog.add(context, tool: "tarot", title: spread.name, result: result, detail: question.isEmpty ? nil : question)
    }

    private func reset() {
        withAnimation(.snappy) {
            phase = .setup
            picked = []
            flipped = []
            fanned = false
        }
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
                let motion = MotionManager.shared
                let tilt = CGSize(width: motion.roll + sin(t * 0.7) * 0.35, height: motion.pitch + cos(t * 0.5) * 0.25)
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
                    RoundedRectangle(cornerRadius: w * 0.07)
                        .strokeBorder(Color(hex: 0xF5D78E), lineWidth: max(1, w * 0.012))
                        .padding(w * 0.05)
                    VStack(spacing: w * 0.05) {
                        Text(card.roman)
                            .font(.system(size: w * 0.11, weight: .semibold, design: .serif))
                            .foregroundStyle(Color(hex: 0xF5D78E))
                        Spacer()
                        Image(systemName: card.symbol)
                            .font(.system(size: w * 0.36, weight: .light))
                            .foregroundStyle(LinearGradient(colors: [.white, Color(hex: 0xF5D78E)], startPoint: .top, endPoint: .bottom))
                            .shadow(color: .white.opacity(0.6), radius: w * 0.06)
                        Spacer()
                        Text(card.name)
                            .font(AppFont.serif(w * 0.13, weight: .bold))
                            .foregroundStyle(.white)
                        Text(card.en.uppercased())
                            .font(.system(size: w * 0.055, weight: .medium, design: .serif))
                            .tracking(1)
                            .foregroundStyle(Color(hex: 0xF5D78E).opacity(0.9))
                            .lineLimit(1)
                            .minimumScaleFactor(0.5)
                    }
                    .padding(.vertical, w * 0.12)
                    .padding(.horizontal, w * 0.08)
                }
                .holoFoil(tilt: tilt, strength: 0.9)
                .rotation3DEffect(.degrees(tilt.width * 6), axis: (0, 1, 0))
                .rotation3DEffect(.degrees(-tilt.height * 6), axis: (1, 0, 0))
            }
        }
        .shadow(color: card.colors[0].opacity(0.6), radius: 14)
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
