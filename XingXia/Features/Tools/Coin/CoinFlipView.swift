import SwiftUI
import SwiftData

struct CoinFlipView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @AppStorage("coin.heads") private var headsCount = 0
    @AppStorage("coin.tails") private var tailsCount = 0

    @State private var angle: Double = 0
    @State private var tossTrigger = 0
    @State private var isFlipping = false
    @State private var result: Bool?
    @State private var streak = 0
    @State private var lastResult: Bool?
    @State private var question = ""

    var body: some View {
        VStack(spacing: 18) {
            TextField("想决定什么？（可选）", text: $question)
                .padding(12)
                .glassEffect(.regular, in: .capsule)
                .padding(.top, 8)

            Spacer(minLength: 0)

            ZStack {
                // 落地阴影
                Ellipse()
                    .fill(.black.opacity(palette.isDark ? 0.5 : 0.2))
                    .frame(width: 150, height: 26)
                    .blur(radius: 8)
                    .offset(y: 120)
                    .keyframeAnimator(initialValue: 1.0, trigger: tossTrigger) { view, scale in
                        view.scaleEffect(scale).opacity(scale)
                    } keyframes: { _ in
                        CubicKeyframe(0.35, duration: 0.55)
                        CubicKeyframe(1.0, duration: 0.6)
                        CubicKeyframe(0.85, duration: 0.12)
                        CubicKeyframe(1.0, duration: 0.15)
                    }

                CoinView(angle: angle)
                    .frame(width: 190, height: 190)
                    .keyframeAnimator(initialValue: CoinToss(), trigger: tossTrigger) { view, toss in
                        view
                            .scaleEffect(toss.scale)
                            .offset(y: toss.lift)
                    } keyframes: { _ in
                        KeyframeTrack(\.lift) {
                            CubicKeyframe(-280, duration: 0.55)
                            CubicKeyframe(0, duration: 0.6)
                            CubicKeyframe(-24, duration: 0.12)
                            CubicKeyframe(0, duration: 0.15)
                        }
                        KeyframeTrack(\.scale) {
                            CubicKeyframe(1.25, duration: 0.55)
                            CubicKeyframe(1.0, duration: 0.6)
                        }
                    }
                    .gesture(DragGesture(minimumDistance: 20).onEnded { value in
                        if value.translation.height < -40 { flip() }
                    })
                    .onTapGesture { flip() }
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(result.map { $0 ? "正面" : "反面" } ?? "硬币")
                    .accessibilityHint("点按抛硬币")
                    .accessibilityAddTraits(.isButton)
            }
            .frame(height: 320)

            Group {
                if let result, !isFlipping {
                    VStack(spacing: 4) {
                        GradientText(text: result ? "正面" : "反面", font: .system(size: 44, weight: .heavy, design: .rounded))
                        if !question.isEmpty {
                            Text(result ? "「\(question)」→ 做！" : "「\(question)」→ 算了吧").font(.subheadline).foregroundStyle(.secondary)
                        }
                        if streak > 1 { Text("已经连续 \(streak) 次\(result ? "正面" : "反面")了").font(.caption).foregroundStyle(.tint) }
                    }
                    .transition(.scale.combined(with: .opacity))
                } else {
                    Text(isFlipping ? "" : "点按、上滑或摇一摇").font(.subheadline).foregroundStyle(.secondary)
                }
            }
            .frame(height: 90)

            Spacer(minLength: 0)

            HStack(spacing: 12) {
                StatPill(title: "正面", value: headsCount, total: headsCount + tailsCount, color: Color(hex: 0xF5B83D))
                StatPill(title: "反面", value: tailsCount, total: headsCount + tailsCount, color: Color(hex: 0x94A3B8))
                Button {
                    headsCount = 0
                    tailsCount = 0
                    streak = 0
                } label: {
                    Image(systemName: "arrow.counterclockwise").frame(width: 44, height: 44)
                }
                .buttonStyle(.glass)
                .accessibilityLabel("清空统计")
            }

            BigActionButton(title: "抛硬币", symbol: "hand.point.up.fill", isEnabled: !isFlipping) { flip() }
                .padding(.bottom, 12)
        }
        .padding(.horizontal, 16)
        .cosmicBackground()
        .navigationTitle("抛硬币")
        .navigationBarTitleDisplayMode(.inline)
        .onShake { flip() }
    }

    private func flip() {
        guard !isFlipping else { return }
        let heads = Bool.random()
        isFlipping = true
        result = nil
        let spins = Double(Int.random(in: 5...7)) * 360
        let current = angle.truncatingRemainder(dividingBy: 360)
        let target = angle - current + spins + (heads ? 0 : 180)
        tossTrigger += 1
        SoundEngine.shared.play(.whoosh, volume: 0.6)
        SoundEngine.shared.play(.coinFlip, volume: 0.7)
        Haptics.impact()
        withAnimation(.timingCurve(0.15, 0.6, 0.35, 1, duration: 1.15)) { angle = target }
        Task {
            try? await Task.sleep(for: .milliseconds(1150))
            SoundEngine.shared.play(.coinLand)
            Haptics.land()
            try? await Task.sleep(for: .milliseconds(250))
            withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) {
                result = heads
                isFlipping = false
            }
            if heads { headsCount += 1 } else { tailsCount += 1 }
            streak = lastResult == heads ? streak + 1 : 1
            lastResult = heads
            HistoryLog.add(context, tool: ToolKind.coin.rawValue, title: question, result: heads ? "正面" : "反面")
        }
    }
}

private struct CoinToss {
    var lift: Double = 0
    var scale: Double = 1
}

/// 绕水平轴翻转的硬币，自带厚度
struct CoinView: View, Animatable {
    var angle: Double

    var animatableData: Double {
        get { angle }
        set { angle = newValue }
    }

    var body: some View {
        let radians = angle * .pi / 180
        let c = cos(radians)
        let showsFront = c >= 0
        let thickness = 12 * abs(sin(radians))
        ZStack {
            // 边缘
            Circle()
                .fill(LinearGradient(colors: [Color(hex: 0x8A6A12), Color(hex: 0xC9A227), Color(hex: 0x6B4F0E)], startPoint: .leading, endPoint: .trailing))
                .scaleEffect(x: 1, y: max(0.02, abs(c)))
                .offset(y: showsFront ? thickness / 2 : -thickness / 2)
            CoinFace(isFront: showsFront)
                .scaleEffect(x: 1, y: max(0.02, abs(c)))
                .offset(y: showsFront ? -thickness / 2 : thickness / 2)
                .brightness(-0.25 * (1 - abs(c)))
        }
    }
}

private struct CoinFace: View {
    let isFront: Bool

    var body: some View {
        ZStack {
            Circle()
                .fill(RadialGradient(colors: [Color(hex: 0xFFF1B8), Color(hex: 0xF2C94C), Color(hex: 0xB8862E)], center: .init(x: 0.35, y: 0.3), startRadius: 4, endRadius: 120))
            Circle()
                .strokeBorder(Color(hex: 0x8A6A12).opacity(0.6), lineWidth: 6)
                .padding(4)
            Circle()
                .strokeBorder(style: StrokeStyle(lineWidth: 1.5, dash: [3, 4]))
                .foregroundStyle(Color(hex: 0x8A6A12).opacity(0.5))
                .padding(16)
            VStack(spacing: 2) {
                Image(systemName: isFront ? "sun.max.fill" : "moon.stars.fill")
                    .font(.system(size: 54, weight: .semibold))
                Text(isFront ? "正" : "反")
                    .font(AppFont.serif(30, weight: .bold))
            }
            .foregroundStyle(Color(hex: 0x7A5A0E))
            .shadow(color: .white.opacity(0.7), radius: 0, x: 1, y: 1)
        }
        .shadow(color: Color(hex: 0xF2C94C).opacity(0.5), radius: 12)
    }
}

struct StatPill: View {
    let title: String
    let value: Int
    let total: Int
    let color: Color

    var body: some View {
        VStack(spacing: 2) {
            Text("\(value)").font(.title3.bold()).monospacedDigit().contentTransition(.numericText())
            Text(total > 0 ? "\(title) \(Int(Double(value) / Double(total) * 100))%" : title)
                .font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 8)
        .glassEffect(.regular.tint(color.opacity(0.15)), in: .rect(cornerRadius: 16))
    }
}
