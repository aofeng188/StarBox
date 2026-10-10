import SwiftUI
import SwiftData

struct FortuneStickView: View {
    enum Phase { case idle, shaking, dropped, revealed }

    @Environment(\.modelContext) private var context
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Query(sort: \StickDraw.date, order: .reverse) private var draws: [StickDraw]

    @State private var phase: Phase = .idle
    @State private var energy: Double = 0
    @State private var isHolding = false
    @State private var stick: FortuneStick?
    @State private var wish = "综合"
    @State private var wobble: Double = 0
    @State private var stickOut = false
    @State private var lastRattle = Date.distantPast
    @State private var confetti = 0
    @State private var isExtraDraw = false
    @State private var loop: Task<Void, Never>?

    private let wishes = ["综合", "事业", "感情", "财运", "健康", "学业", "出行"]

    private var todayDraw: StickDraw? {
        draws.first { $0.dayKey == DayKey.today.rawValue && $0.profileID == app.primary?.id }
    }

    var body: some View {
        ZStack {
            if phase == .revealed, let stick {
                StickResultView(stick: stick, wish: wish, isExtra: isExtraDraw) {
                    drawAgain()
                }
                .transition(.asymmetric(insertion: .scale(scale: 0.9).combined(with: .opacity), removal: .opacity))
            } else {
                shakeStage
            }
            ConfettiView(trigger: confetti)
        }
        .cosmicBackground()
        .navigationTitle("求签")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            if let todayDraw, phase == .idle {
                stick = StickLibrary.stick(todayDraw.stickNumber)
                phase = .revealed
            }
            MotionManager.shared.start()
            startLoop()
        }
        .onDisappear {
            MotionManager.shared.stop()
            loop?.cancel()
        }
        .onShake { if phase == .idle || phase == .shaking { energy += 0.45; phase = .shaking; rattle() } }
    }

    // MARK: 摇签舞台

    private var shakeStage: some View {
        VStack(spacing: 22) {
            VStack(spacing: 8) {
                Text(phase == .dropped ? "签已落地" : "心中默念所求之事")
                    .font(AppFont.serif(26, weight: .bold))
                    .contentTransition(.opacity)
                Text(phase == .dropped ? "点按签文，看看今天的指引" : "摇动手机，或按住下方按钮摇签")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(.top, 20)

            if phase == .idle {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(wishes, id: \.self) { item in
                            Button(item) {
                                wish = item
                                Haptics.selection()
                            }
                            .font(.subheadline.weight(.medium))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .foregroundStyle(wish == item ? .white : .primary)
                            .background(Capsule().fill(wish == item ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.08))))
                        }
                    }
                    .padding(.horizontal, 20)
                }
                .transition(.opacity)
            }

            Spacer(minLength: 0)

            ZStack {
                // 光晕
                Circle()
                    .fill(RadialGradient(colors: [Color(hex: 0xFFB347).opacity(0.45 + energy * 0.4), .clear], center: .center, startRadius: 0, endRadius: 190))
                    .frame(width: 380, height: 380)
                    .scaleEffect(1 + energy * 0.15)
                    .blur(radius: 10)

                if phase == .dropped, let stick {
                    Button {
                        reveal()
                    } label: {
                        SingleStick(number: stick.n, glowing: true)
                    }
                    .buttonStyle(.plain)
                    .rotationEffect(.degrees(stickOut ? -8 : -40))
                    .offset(x: stickOut ? 0 : 40, y: stickOut ? 20 : -160)
                    .scaleEffect(stickOut ? 1.1 : 0.6)
                    .accessibilityLabel("签文已落地，点按解签")
                } else {
                    StickTubeIllustration(size: 230, shake: wobble)
                        .rotationEffect(.degrees(sin(wobble * 2.6) * (phase == .shaking ? 9 * min(1, 0.3 + energy) : 0)), anchor: .bottom)
                        .offset(y: phase == .shaking ? CGFloat(sin(wobble * 5.1)) * 6 : 0)
                }
            }
            .frame(height: 360)

            Spacer(minLength: 0)

            if phase != .dropped {
                VStack(spacing: 12) {
                    // 进度
                    ZStack(alignment: .leading) {
                        Capsule().fill(.primary.opacity(0.1))
                        Capsule()
                            .fill(LinearGradient(colors: [Color(hex: 0xFFD166), Color(hex: 0xFF6B35)], startPoint: .leading, endPoint: .trailing))
                            .frame(width: max(0, CGFloat(min(1, energy)) * 260))
                            .shadow(color: Color(hex: 0xFF6B35).opacity(0.7), radius: 6)
                    }
                    .frame(width: 260, height: 8)
                    .accessibilityLabel("摇签进度 \(Int(min(1, energy) * 100))%")

                    Text("按住摇签")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(width: 220, height: 56)
                        .background(Capsule().fill(LinearGradient(colors: [Color(hex: 0xE11D48), Color(hex: 0xF59E0B)], startPoint: .leading, endPoint: .trailing)))
                        .glassEffect(.regular.interactive(), in: .capsule)
                        .scaleEffect(isHolding ? 0.95 : 1)
                        .shadow(color: Color(hex: 0xE11D48).opacity(0.5), radius: 12)
                        .onLongPressGesture(minimumDuration: 10, maximumDistance: 60, pressing: { pressing in
                            withAnimation(.spring(response: 0.25)) { isHolding = pressing }
                        }, perform: {})
                        .accessibilityAddTraits(.isButton)
                        .accessibilityLabel("按住摇签")
                        .accessibilityAction { energy = 1.0; phase = .shaking }
                }
                .padding(.bottom, 30)
            } else {
                Button("解签") { reveal() }
                    .buttonStyle(.glassProminent)
                    .controlSize(.large)
                    .tint(Color(hex: 0xE11D48))
                    .padding(.bottom, 30)
            }
        }
        .frame(maxWidth: .infinity)
    }

    // MARK: 逻辑

    private func startLoop() {
        loop?.cancel()
        loop = Task { @MainActor in
            while !Task.isCancelled {
                try? await Task.sleep(for: .milliseconds(33))
                tick()
            }
        }
    }

    private func tick() {
        guard phase == .idle || phase == .shaking else { return }
        let motion = MotionManager.shared.shakeIntensity
        let intensity = max(motion > 0.25 ? min(1.2, motion) : 0, isHolding ? 0.85 : 0)
        if intensity > 0 {
            if phase == .idle { withAnimation(.snappy) { phase = .shaking } }
            energy += intensity * 0.014
            wobble += 0.22 + intensity * 0.25
            if Date().timeIntervalSince(lastRattle) > 0.28 { rattle() }
        } else if phase == .shaking {
            wobble += 0.05
            energy = max(0, energy - 0.002)
        }
        if energy >= 1 { drop() }
    }

    private func rattle() {
        lastRattle = Date()
        SoundEngine.shared.play(.stickRattle, volume: 0.8)
        Haptics.rattle(count: 4)
    }

    private func drop() {
        let number = TrueRandom.int(1...StickLibrary.all.count)
        stick = StickLibrary.stick(number)
        isHolding = false
        stickOut = false
        withAnimation(.snappy) { phase = .dropped }
        withAnimation(reduceMotion ? .none : .spring(response: 0.7, dampingFraction: 0.55).delay(0.05)) { stickOut = true }
        Task {
            try? await Task.sleep(for: .milliseconds(350))
            SoundEngine.shared.play(.stickDrop)
            Haptics.land(strength: 0.9)
        }
    }

    private func reveal() {
        guard let stick else { return }
        withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) { phase = .revealed }
        SoundEngine.shared.play(.chime)
        Haptics.reveal()
        if stick.levelInfo == .best || stick.levelInfo == .good { confetti += 1 }

        if todayDraw == nil && !isExtraDraw {
            context.insert(StickDraw(dayKey: DayKey.today.rawValue, profileID: app.primary?.id, stickNumber: stick.n))
        }
        HistoryLog.add(context, tool: "stick", title: "求\(wish)", result: "\(stick.numberText) \(stick.levelInfo.displayName) · \(stick.title)")
    }

    private func drawAgain() {
        isExtraDraw = true
        energy = 0
        wobble = 0
        stick = nil
        withAnimation(.snappy) { phase = .idle }
    }
}

/// 掉出来的一支签
private struct SingleStick: View {
    let number: Int
    var glowing = false

    var body: some View {
        VStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 4).fill(Color(hex: 0xC0392B)).frame(width: 34, height: 26)
            ZStack {
                RoundedRectangle(cornerRadius: 4)
                    .fill(LinearGradient(colors: [Color(hex: 0xF7E1B5), Color(hex: 0xD9AE6D)], startPoint: .leading, endPoint: .trailing))
                VStack(spacing: 4) {
                    ForEach(Array(Array("第\(StickLibrary.chineseNumber(number))签").enumerated()), id: \.offset) { _, char in
                        Text(String(char)).font(AppFont.serif(17, weight: .bold)).foregroundStyle(Color(hex: 0x7B1E1E))
                    }
                }
            }
            .frame(width: 34, height: 210)
        }
        .shadow(color: glowing ? Color(hex: 0xFFD166).opacity(0.9) : .clear, radius: 18)
    }
}

// MARK: - 解签结果

struct StickResultView: View {
    let stick: FortuneStick
    let wish: String
    let isExtra: Bool
    let onAgain: () -> Void

    @Environment(\.palette) private var palette
    @State private var stamped = false

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                // 签面
                VStack(spacing: 18) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(stick.numberText).font(AppFont.serif(30, weight: .bold))
                            Text("「\(stick.title)」").font(AppFont.serif(20)).foregroundStyle(.secondary)
                        }
                        Spacer()
                        // 印章
                        Text(stick.levelInfo.rawValue == "下" ? "下签" : stick.levelInfo.rawValue)
                            .font(AppFont.serif(stick.levelInfo.rawValue.count > 1 ? 22 : 26, weight: .bold))
                            .foregroundStyle(Color(hex: 0xE53935))
                            .frame(width: 74, height: 74)
                            .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(Color(hex: 0xE53935), lineWidth: 3))
                            .rotationEffect(.degrees(stamped ? -12 : -30))
                            .scaleEffect(stamped ? 1 : 2.4)
                            .opacity(stamped ? 0.92 : 0)
                    }

                    // 竖排签诗（从右往左）
                    HStack(alignment: .top, spacing: 18) {
                        ForEach(Array(stick.poem.enumerated().reversed()), id: \.offset) { _, line in
                            VStack(spacing: 6) {
                                ForEach(Array(line.enumerated()), id: \.offset) { _, char in
                                    Text(String(char)).font(AppFont.serif(24, weight: .semibold))
                                }
                            }
                        }
                    }
                    .padding(.vertical, 18)
                    .frame(maxWidth: .infinity)
                    .background(
                        RoundedRectangle(cornerRadius: 18)
                            .fill(LinearGradient(colors: [Color(hex: 0xFFF6E0).opacity(0.12), Color(hex: 0xFFD166).opacity(0.06)], startPoint: .top, endPoint: .bottom))
                    )
                    .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(Color(hex: 0xFFD166).opacity(0.35), lineWidth: 1))
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(stick.poem.joined(separator: "，"))

                    VStack(alignment: .leading, spacing: 8) {
                        Text("解曰").font(AppFont.serif(17, weight: .bold)).foregroundStyle(stick.levelInfo.color)
                        Text(stick.meaning).font(.body).lineSpacing(4)
                        Text(stick.levelInfo.blessing).font(.subheadline).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .glassCard(padding: 22, tint: stick.levelInfo.color.opacity(0.08))

                // 分项
                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: "签文详解", subtitle: wish == "综合" ? nil : "所求：\(wish)", symbol: "list.bullet.rectangle.portrait")
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                        ForEach(stick.areas, id: \.0) { area, verdict in
                            let highlighted = area == wish || (wish == "学业" && area == "事业")
                            VStack(spacing: 4) {
                                Text(area).font(.caption).foregroundStyle(highlighted ? stick.levelInfo.color : .secondary)
                                Text(verdict).font(.subheadline.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(highlighted ? stick.levelInfo.color.opacity(0.18) : .primary.opacity(0.05), in: .rect(cornerRadius: 14))
                            .overlay {
                                if highlighted { RoundedRectangle(cornerRadius: 14).strokeBorder(stick.levelInfo.color.opacity(0.6)) }
                            }
                        }
                    }
                }
                .glassCard()

                VStack(spacing: 6) {
                    Text(isExtra ? "这一签不计入今日一签" : "心诚则灵 · 一日一签")
                        .font(.footnote).foregroundStyle(.secondary)
                    Button("为家人朋友再求一签", systemImage: "arrow.counterclockwise") { onAgain() }
                        .buttonStyle(.glass)
                }
                .padding(.bottom, 30)
            }
            .padding(.horizontal, 16)
            .padding(.top, 8)
        }
        .onAppear {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.55).delay(0.35)) { stamped = true }
            Task {
                try? await Task.sleep(for: .milliseconds(500))
                SoundEngine.shared.play(.drum, volume: 0.8)
                Haptics.heavyImpact()
            }
        }
    }
}
