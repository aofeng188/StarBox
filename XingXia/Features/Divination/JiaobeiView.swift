import SwiftUI
import SwiftData

// MARK: - 结果与记录

/// 一次掷筊的结果。两片筊杯各自有一半机会平面朝上：圣杯 50%、笑杯 25%、阴杯 25%
enum JiaobeiResult: String, Hashable {
    /// 一平一凸：神明认可
    case sheng
    /// 两面皆平：神明笑而不答，可以再掷
    case xiao
    /// 两面皆凸：神明未允
    case yin

    init(leftFlat: Bool, rightFlat: Bool) {
        if leftFlat != rightFlat {
            self = .sheng
        } else {
            self = leftFlat ? .xiao : .yin
        }
    }

    var name: String {
        switch self {
        case .sheng: "圣杯"
        case .xiao: "笑杯"
        case .yin: "阴杯"
        }
    }

    var meaning: String {
        switch self {
        case .sheng: "一平一凸，神明认可"
        case .xiao: "两面皆平，神明笑而不答"
        case .yin: "两面皆凸，神明未允"
        }
    }

    func color(_ scheme: ColorScheme) -> Color {
        switch self {
        case .sheng: Color.positive(scheme)
        case .xiao: Color.caution(scheme)
        case .yin: Color.negative(scheme)
        }
    }
}

/// 记下哪些签已经掷筊确认（按日期、求签对象、签号），签面据此盖「已确认」小印。只留最近的一批
enum JiaobeiLedger {
    private static let key = "jiaobeiConfirmed"
    private static let limit = 120

    static func entry(day: DayKey, owner: UUID?, stick: Int) -> String {
        "\(day.rawValue)|\(owner?.uuidString.lowercased() ?? "me")|\(stick)"
    }

    static func isConfirmed(_ entry: String) -> Bool {
        (UserDefaults.standard.stringArray(forKey: key) ?? []).contains(entry)
    }

    static func markConfirmed(_ entry: String) {
        var list = UserDefaults.standard.stringArray(forKey: key) ?? []
        guard !list.contains(entry) else { return }
        list.append(entry)
        if list.count > limit { list.removeFirst(list.count - limit) }
        UserDefaults.standard.set(list, forKey: key)
    }
}

// MARK: - 掷筊

/// 掷筊确认：一次掷两片筊杯，连续三次圣杯这支签才算得到确认；
/// 笑杯时圣杯重新计数、可以再掷；阴杯表示神明未允，可以重新求一签（不计入今日一签）
struct JiaobeiView: View {
    let stick: FortuneStick
    /// 为家人朋友求的签写对方名字；nil 是自己的签
    var forName: String?
    /// 连得三次圣杯
    let onConfirmed: () -> Void
    /// 得阴杯后选了「重新求一签」，关闭后由求签页处理
    let onRedraw: () -> Void

    static let needed = 3

    init(stick: FortuneStick, forName: String? = nil, onConfirmed: @escaping () -> Void, onRedraw: @escaping () -> Void) {
        self.stick = stick
        self.forName = forName
        self.onConfirmed = onConfirmed
        self.onRedraw = onRedraw
    }

    private struct Pose: Equatable {
        /// 绕水平轴翻转的累计角度，见 JiaobeiPiece
        var spin: Double
        /// 平面内的转角
        var turn: Double
    }

    private enum Outcome { case pending, confirmed, denied }

    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Environment(AppSettings.self) private var settings
    @Environment(\.colorScheme) private var scheme
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    /// 一开始左凸右平，摆成圣杯的样子
    @State private var poses = [Pose(spin: 0, turn: -14), Pose(spin: 180, turn: 12)]
    /// 0 在桌面上，1 抛到最高处
    @State private var lift: CGFloat = 0
    /// 落地时压扁一下
    @State private var squash: CGFloat = 0
    /// 减弱动态效果时用淡出淡入代替抛起
    @State private var stageOpacity: Double = 1
    @State private var isThrowing = false
    /// 连续圣杯数
    @State private var streak = 0
    @State private var last: JiaobeiResult?
    @State private var outcome: Outcome = .pending
    /// 本轮每一掷的结果，写进决策历史
    @State private var log: [JiaobeiResult] = []
    @State private var flow: Task<Void, Never>?
    @State private var confetti = 0

    private static let pieceX: [CGFloat] = [-72, 72]
    private static let pieceHeight: [CGFloat] = [160, 138]
    private static let pieceDrift: [CGFloat] = [-20, 24]

    var body: some View {
        NavigationStack {
            ZStack {
                StageScaffold(spacing: 16, minHeight: 560) {
                    header
                } stage: {
                    ScaleToFit(design: CGSize(width: 320, height: 300)) { stage }
                } bottom: {
                    controls.padding(.bottom, 24)
                }
                ConfettiView(trigger: confetti)
            }
            .cosmicBackground(dimmed: true)
            .navigationTitle("掷筊确认")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭", systemImage: "xmark") { dismiss() }
                }
            }
        }
        .onShake(isEnabled: outcome == .pending && !isThrowing) { throwCups() }
        .onDisappear { flow?.cancel() }
    }

    // MARK: 头部

    private var header: some View {
        VStack(spacing: 12) {
            VStack(spacing: 4) {
                if let forName {
                    Text("为 \(forName) 掷筊").font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
                }
                Text("\(stick.numberText)「\(stick.title)」")
                    .font(AppFont.serif(22, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                Text(stick.levelInfo.displayName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(stick.levelInfo.ink(scheme))
            }
            .accessibilityElement(children: .combine)

            Text("心中默念所求，一次掷两片筊杯，连得三次圣杯，这支签才算得到确认")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            counter
        }
        .padding(.top, 12)
    }

    private var counter: some View {
        HStack(spacing: 10) {
            HStack(spacing: 6) {
                ForEach(0..<Self.needed, id: \.self) { index in
                    JiaobeiShape()
                        .fill(index < streak ? AnyShapeStyle(palette.identity(Color(hex: 0xE53935))) : AnyShapeStyle(.primary.opacity(0.15)))
                        .frame(width: 26, height: 16)
                }
            }
            Text("圣杯 \(streak)/\(Self.needed)")
                .font(.headline.monospacedDigit())
                .contentTransition(.numericText())
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 8)
        .glassEffect(.regular, in: .capsule)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("已连得圣杯 \(streak) 次，共需 \(Self.needed) 次")
    }

    // MARK: 舞台

    private var stage: some View {
        ZStack {
            // 结果
            VStack(spacing: 6) {
                Text(last?.name ?? "请掷筊")
                    .font(AppFont.serif(34, weight: .bold))
                    .foregroundStyle(last.map { $0.color(scheme) } ?? Color.primary)
                    .contentTransition(.opacity)
                Text(last?.meaning ?? (settings.shakeEnabled ? "点按下方按钮，或摇一摇手机" : "点按下方按钮"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .offset(y: -96)
            .opacity(isThrowing ? 0 : 1)

            // 桌面上的影子：抛得越高越小越淡
            ForEach(0..<2, id: \.self) { index in
                Ellipse()
                    .fill(.black.opacity(0.28 * (1 - lift * 0.7)))
                    .frame(width: 110 * (1 - lift * 0.45), height: 18 * (1 - lift * 0.45))
                    .blur(radius: 4)
                    .offset(x: Self.pieceX[index] + Self.pieceDrift[index] * lift, y: 92)
            }

            ForEach(0..<2, id: \.self) { index in
                JiaobeiPiece(spin: poses[index].spin)
                    .frame(width: 124, height: 76)
                    .scaleEffect(x: 1 + 0.06 * squash, y: 1 - 0.1 * squash, anchor: .bottom)
                    .rotationEffect(.degrees(poses[index].turn))
                    .shadow(color: .black.opacity(0.3), radius: 6, y: 4)
                    .offset(x: Self.pieceX[index] + Self.pieceDrift[index] * lift, y: 52 - Self.pieceHeight[index] * lift)
            }

            // 每片朝上的是哪一面
            ForEach(0..<2, id: \.self) { index in
                Text(JiaobeiPiece.isFlatUp(poses[index].spin) ? "平面" : "凸面")
                    .font(.caption.weight(.medium))
                    .foregroundStyle(.secondary)
                    .offset(x: Self.pieceX[index], y: 122)
                    .opacity(isThrowing ? 0 : 1)
            }
        }
        .frame(width: 320, height: 300)
        // 舞台是固定尺寸的画布，字号过大时结果文字会压到筊杯上；结果另有读屏播报和下方提示
        .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
        .opacity(stageOpacity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(stageLabel)
    }

    private var stageLabel: String {
        let sides = poses.map { JiaobeiPiece.isFlatUp($0.spin) ? "平面" : "凸面" }
        let state = "两片筊杯，左边\(sides[0])朝上，右边\(sides[1])朝上"
        guard let last else { return state }
        return "\(last.name)，\(last.meaning)。\(state)"
    }

    // MARK: 操作

    private var controls: some View {
        VStack(spacing: 12) {
            switch outcome {
            case .pending:
                if let hint {
                    Text(hint)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                BigActionButton(title: isThrowing ? "掷筊中…" : (log.isEmpty ? "掷筊" : "再掷一次"),
                                symbol: "hands.sparkles.fill", isEnabled: !isThrowing) { throwCups() }
            case .confirmed:
                Label("连得三次圣杯，这支签已得到确认", systemImage: "checkmark.seal.fill")
                    .font(.headline)
                    .foregroundStyle(Color.positive(scheme))
                    .multilineTextAlignment(.center)
                BigActionButton(title: "完成", symbol: "checkmark") { dismiss() }
            case .denied:
                Text("神明未允，可以重新求一签（不计入今日一签）")
                    .font(.headline)
                    .foregroundStyle(Color.negative(scheme))
                    .multilineTextAlignment(.center)
                BigActionButton(title: "重新求一签", symbol: "arrow.counterclockwise") {
                    onRedraw()
                    dismiss()
                }
                Button("再掷一轮", systemImage: "arrow.triangle.2.circlepath") { restart() }
                    .buttonStyle(.glass)
            }
            roundLog
        }
    }

    private var hint: String? {
        switch last {
        case .sheng?: "再得 \(Self.needed - streak) 次圣杯即可确认"
        case .xiao?: "笑杯不算数，圣杯重新计数，可以继续掷"
        default: nil
        }
    }

    @ViewBuilder
    private var roundLog: some View {
        if !log.isEmpty {
            let shown = log.suffix(10).map(\.name).joined(separator: " · ")
            Text("本轮 \(log.count) 掷：" + (log.count > 10 ? "… " : "") + shown)
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    // MARK: 动作

    private func throwCups() {
        guard outcome == .pending, !isThrowing else { return }
        withAnimation(.easeOut(duration: 0.15)) { isThrowing = true }
        let flats = [Bool.random(), Bool.random()]
        let result = JiaobeiResult(leftFlat: flats[0], rightFlat: flats[1])
        var landing = poses
        for index in landing.indices {
            landing[index].spin = Self.landingSpin(from: poses[index].spin, flat: flats[index], extraTurns: Int.random(in: 2...3))
            landing[index].turn = Double.random(in: -26...26)
        }
        let targets = landing
        SoundEngine.shared.play(.whoosh)
        Haptics.softImpact(0.7)

        flow?.cancel()
        flow = Task { @MainActor in
            if reduceMotion {
                withAnimation(.easeOut(duration: 0.18)) { stageOpacity = 0 }
                try? await Task.sleep(for: .milliseconds(200))
                guard !Task.isCancelled else { return }
                poses = targets
                withAnimation(.easeIn(duration: 0.3)) { stageOpacity = 1 }
            } else {
                // 抛起、翻转、落地：翻转跟着整段飞行走完，落地时正好停在结果那一面
                withAnimation(.easeOut(duration: 0.8)) { poses = targets }
                withAnimation(.easeOut(duration: 0.42)) { lift = 1 }
                try? await Task.sleep(for: .milliseconds(420))
                guard !Task.isCancelled else { return }
                withAnimation(.easeIn(duration: 0.38)) { lift = 0 }
                try? await Task.sleep(for: .milliseconds(380))
                guard !Task.isCancelled else { return }
                withAnimation(.easeOut(duration: 0.06)) { squash = 1 }
            }
            land(result)
            if !reduceMotion {
                try? await Task.sleep(for: .milliseconds(60))
                withAnimation(.spring(response: 0.3, dampingFraction: 0.45)) { squash = 0 }
            }
        }
    }

    /// 落地时的累计角度：从当前的整半圈起再多翻几圈，停在要的那一面
    private static func landingSpin(from spin: Double, flat: Bool, extraTurns: Int) -> Double {
        let base = (spin / 180).rounded() * 180 + Double(extraTurns) * 360
        return JiaobeiPiece.isFlatUp(base) == flat ? base : base + 180
    }

    private func land(_ result: JiaobeiResult) {
        log.append(result)
        withAnimation(.snappy) {
            isThrowing = false
            last = result
        }

        // 两片先后落在桌上
        SoundEngine.shared.play(.stickDrop, volume: 0.7)
        Haptics.land(strength: 0.85)
        Task {
            try? await Task.sleep(for: .milliseconds(90))
            SoundEngine.shared.play(.diceHit)
        }

        switch result {
        case .sheng:
            let count = streak + 1
            withAnimation(.snappy) { streak = count }
            if count >= Self.needed {
                withAnimation(.snappy) { outcome = .confirmed }
                record(confirmed: true)
                onConfirmed()
                Announce.post("圣杯。连得三次圣杯，这支签已得到确认")
                feedback(after: 280) {
                    confetti += 1
                    SoundEngine.shared.play(.success)
                    Haptics.success()
                }
            } else {
                Announce.post("圣杯，已连得 \(count) 次，再得 \(Self.needed - count) 次即可确认")
                feedback(after: 250) {
                    SoundEngine.shared.play(.chime, volume: 0.6)
                    Haptics.impact(0.6)
                }
            }
        case .xiao:
            withAnimation(.snappy) { streak = 0 }
            Announce.post("笑杯，神明笑而不答，圣杯重新计数，可以继续掷")
            feedback(after: 250) {
                SoundEngine.shared.play(.pop)
                Haptics.softImpact()
            }
        case .yin:
            withAnimation(.snappy) {
                streak = 0
                outcome = .denied
            }
            record(confirmed: false)
            Announce.post("阴杯，神明未允，可以重新求一签，不计入今日一签")
            feedback(after: 250) {
                SoundEngine.shared.play(.fail)
                Haptics.warning()
            }
        }
    }

    /// 落地声之后再放结果的音效和震动，免得糊在一起
    private func feedback(after milliseconds: Int, _ action: @escaping @MainActor () -> Void) {
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(milliseconds))
            action()
        }
    }

    /// 一轮结束（得到确认或阴杯）时写一条决策历史
    private func record(confirmed: Bool) {
        let title = (forName.map { "为\($0) · " } ?? "") + stick.numberText
        let result = confirmed ? "连得三次圣杯，签已确认" : "阴杯，神明未允"
        HistoryLog.add(context, tool: "jiaobei", title: title, result: result,
                       detail: log.map(\.name).joined(separator: "、"))
    }

    private func restart() {
        withAnimation(.snappy) {
            outcome = .pending
            streak = 0
            last = nil
        }
        log = []
    }
}

// MARK: - 筊杯

/// 筊杯的轮廓：月牙形，两角朝上
struct JiaobeiShape: Shape {
    func path(in rect: CGRect) -> Path {
        let w = rect.width
        let h = rect.height
        func point(_ x: CGFloat, _ y: CGFloat) -> CGPoint {
            CGPoint(x: rect.minX + w * x, y: rect.minY + h * y)
        }
        var path = Path()
        path.move(to: point(0.04, 0.24))
        // 外弧
        path.addCurve(to: point(0.96, 0.24), control1: point(0.06, 1.25), control2: point(0.94, 1.25))
        // 右角收圆
        path.addQuadCurve(to: point(0.88, 0.14), control: point(1.0, 0.1))
        // 内弧
        path.addCurve(to: point(0.12, 0.14), control1: point(0.7, 0.52), control2: point(0.3, 0.52))
        // 左角收圆
        path.addQuadCurve(to: point(0.04, 0.24), control: point(0.0, 0.1))
        path.closeSubpath()
        return path
    }
}

/// 一片筊杯。spin 是绕水平轴翻转的累计角度：0° 凸面朝上，每翻过半圈换一面
struct JiaobeiPiece: View {
    let spin: Double

    static func isFlatUp(_ spin: Double) -> Bool {
        !Int(((spin + 90) / 180).rounded(.down)).isMultiple(of: 2)
    }

    var body: some View {
        ZStack {
            JiaobeiFace(flat: false)
                .modifier(JiaobeiSideVisibility(spin: spin, showsFlat: false))
            // 平面那一面是翻过半圈才看到的，先上下倒一次抵消
            JiaobeiFace(flat: true)
                .scaleEffect(x: 1, y: -1)
                .modifier(JiaobeiSideVisibility(spin: spin, showsFlat: true))
        }
        .rotation3DEffect(.degrees(spin), axis: (x: 1, y: 0, z: 0), perspective: 0.35)
        .accessibilityHidden(true)
    }
}

/// 翻到侧立（90°）时才换面，和塔罗翻牌的 FlipVisibility 一样
private struct JiaobeiSideVisibility: ViewModifier, Animatable {
    var spin: Double
    let showsFlat: Bool

    var animatableData: Double {
        get { spin }
        set { spin = newValue }
    }

    func body(content: Content) -> some View {
        content.opacity(JiaobeiPiece.isFlatUp(spin) == showsFlat ? 1 : 0)
    }
}

/// 筊杯的一面：凸面漆色深、带一道高光；平面颜色平匀，带一圈浅色描边。
/// 筊杯保留传统朱漆红，但向主题强调色靠一点
private struct JiaobeiFace: View {
    let flat: Bool

    @Environment(\.palette) private var palette

    var body: some View {
        GeometryReader { proxy in
            let w = proxy.size.width
            let h = proxy.size.height
            ZStack {
                if flat {
                    JiaobeiShape()
                        .fill(LinearGradient(colors: palette.identity([Color(hex: 0xDB4A3E), Color(hex: 0xB3261E)]), startPoint: .top, endPoint: .bottom))
                    JiaobeiShape()
                        .stroke(palette.identity(Color(hex: 0xFFD6C9)).opacity(0.55), lineWidth: max(1, w * 0.012))
                        .scaleEffect(0.82)
                        .offset(y: h * 0.05)
                } else {
                    JiaobeiShape()
                        .fill(LinearGradient(colors: palette.identity([Color(hex: 0xF0564A), Color(hex: 0xB71C1C), Color(hex: 0x6B0F0F)]), startPoint: .top, endPoint: .bottom))
                    Ellipse()
                        .fill(.white.opacity(0.42))
                        .frame(width: w * 0.42, height: h * 0.13)
                        .blur(radius: max(1, w * 0.02))
                        .position(x: w * 0.5, y: h * 0.6)
                }
                JiaobeiShape()
                    .stroke(palette.identity(Color(hex: 0x5A0B0B)).opacity(0.7), lineWidth: max(1, w * 0.012))
            }
            .clipShape(JiaobeiShape())
        }
    }
}

/// 掷筊连得三次圣杯后盖在签上的「已确认」小印（印章红向主题强调色靠一点；在分享卡里跟分享卡的主题走）
struct JiaobeiSeal: View {
    var size: CGFloat = 13

    @Environment(\.palette) private var palette

    var body: some View {
        let red = palette.identity(Color(hex: 0xE53935))
        Text("已确认")
            .font(AppFont.serif(size, weight: .bold))
            .foregroundStyle(red)
            .padding(.horizontal, size * 0.55)
            .padding(.vertical, size * 0.3)
            .background(red.opacity(0.1), in: .rect(cornerRadius: size * 0.35))
            .overlay(RoundedRectangle(cornerRadius: size * 0.35).strokeBorder(red, lineWidth: max(1.2, size * 0.11)))
            .opacity(0.9)
            .accessibilityLabel("已掷筊确认")
    }
}
