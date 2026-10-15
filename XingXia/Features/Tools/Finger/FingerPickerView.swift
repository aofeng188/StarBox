import SwiftUI
import SwiftData
import UIKit

/// 捕获多点触控的 UIKit 视图
struct MultiTouchSurface: UIViewRepresentable {
    /// 读屏提示（当前模式的规则）
    var spokenHint: String?
    var onChange: ([Int: CGPoint]) -> Void

    func makeUIView(context: Context) -> TouchView {
        let view = TouchView()
        view.isMultipleTouchEnabled = true
        view.backgroundColor = .clear
        // 开着 VoiceOver 时把原始触摸直接交给这块区域，否则手指会被当成浏览手势
        view.isAccessibilityElement = true
        view.accessibilityLabel = "触控区域，所有人把一根手指放在屏幕上"
        view.accessibilityHint = spokenHint
        view.accessibilityTraits = .allowsDirectInteraction
        view.onChange = onChange
        return view
    }

    func updateUIView(_ uiView: TouchView, context: Context) {
        uiView.onChange = onChange
        uiView.accessibilityHint = spokenHint
    }

    final class TouchView: UIView {
        var onChange: (([Int: CGPoint]) -> Void)?
        private var points: [ObjectIdentifier: CGPoint] = [:]
        private var ids: [ObjectIdentifier: Int] = [:]
        private var nextID = 0

        override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
            for touch in touches {
                let key = ObjectIdentifier(touch)
                ids[key] = nextID
                nextID += 1
                points[key] = touch.location(in: self)
            }
            publish()
        }

        override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
            for touch in touches { points[ObjectIdentifier(touch)] = touch.location(in: self) }
            publish()
        }

        override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) { remove(touches) }
        override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) { remove(touches) }

        private func remove(_ touches: Set<UITouch>) {
            for touch in touches {
                let key = ObjectIdentifier(touch)
                points[key] = nil
                ids[key] = nil
            }
            publish()
        }

        private func publish() {
            var result: [Int: CGPoint] = [:]
            for (key, point) in points { if let id = ids[key] { result[id] = point } }
            onChange?(result)
        }
    }
}

struct FingerPickerView: View {
    enum Mode: String, CaseIterable, Identifiable {
        case one, several, teams
        var id: String { rawValue }
        var name: String { ["one": "选 1 人", "several": "选多人", "teams": "分两队"][rawValue]! }
    }

    enum Phase { case waiting, counting, done }

    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var mode: Mode = .one
    @State private var pickCount = 2
    @State private var touches: [Int: CGPoint] = [:]
    @State private var colors: [Int: Color] = [:]
    @State private var phase: Phase = .waiting
    @State private var stableSince = Date()
    @State private var countdownStart: Date?
    @State private var winners: Set<Int> = []
    @State private var teams: [Int: Int] = [:]
    @State private var resultTime: Date?

    private let fingerColors: [Color] = [0xFF6B6B, 0xFFD166, 0x06D6A0, 0x4CC9F0, 0xB388FF, 0xFF8FC7, 0xF4A261, 0x80ED99, 0x48CAE4, 0xFFAFCC].map { Color(hex: $0) }
    private let teamColors: [Color] = [Color(hex: 0xFF5D73), Color(hex: 0x4CC9F0)]
    private let countdown = 2.2

    var body: some View {
        ZStack {
            // 没有手指时什么也不用画，暂停逐帧刷新
            TimelineView(.animation(paused: touches.isEmpty && phase == .waiting)) { context in
                let now = context.date
                Canvas { canvas, _ in
                    draw(in: &canvas, now: now)
                }
                .onChange(of: now) { _, newValue in tick(newValue) }
            }
            .accessibilityHidden(true)

            MultiTouchSurface(spokenHint: hint) { points in update(points) }

            if touches.isEmpty {
                VStack(spacing: 14) {
                    Image(systemName: "hand.point.up.left.and.text.fill")
                        .font(.system(size: 50))
                        .foregroundStyle(palette.accentGradient)
                        .symbolEffect(.bounce, options: .repeating.speed(0.3), isActive: !reduceMotion)
                    Text("每个人把一根手指放在屏幕上")
                        .font(.title3.bold())
                    Text(hint).font(.subheadline).foregroundStyle(.secondary).multilineTextAlignment(.center)
                }
                .padding(30)
                .allowsHitTesting(false)
                // 说明压在屏幕正中，读屏会抢走落在上面的手指；内容已经放进触控区域的标签和提示里
                .accessibilityHidden(true)
            }

            VStack {
                if touches.isEmpty || phase == .waiting {
                    controls
                        .opacity(touches.isEmpty ? 1 : 0)
                }
                Spacer()
                Text(statusText)
                    .font(.headline)
                    .padding(.horizontal, 18).padding(.vertical, 10)
                    .glassEffect(.regular, in: .capsule)
                    .padding(.bottom, 30)
                    .allowsHitTesting(false)
                    .accessibilityAddTraits(.updatesFrequently)
            }
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle("指尖轮盘")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
    }

    private var controls: some View {
        VStack(spacing: 10) {
            Picker("模式", selection: $mode) {
                ForEach(Mode.allCases) { Text($0.name).tag($0) }
            }
            .pickerStyle(.segmented)
            if mode == .several {
                Stepper("选出 \(pickCount) 人", value: $pickCount, in: 2...8)
            }
        }
        .padding(14)
        .glassEffect(.regular, in: .rect(cornerRadius: 22))
        .frame(maxWidth: 480)
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    private var hint: String {
        switch mode {
        case .one: "至少 2 人。手指放稳后自动倒数，随机选出一位（谁买单？谁先手？）"
        case .several: "至少 \(pickCount + 1) 人，随机选出 \(pickCount) 位"
        case .teams: "至少 2 人，随机分成红蓝两队"
        }
    }

    private var minimumFingers: Int { mode == .several ? pickCount + 1 : 2 }

    private var statusText: String {
        switch phase {
        case .waiting:
            if touches.isEmpty { return "等待手指…" }
            if touches.count < minimumFingers { return "\(touches.count) 人，还需要 \(minimumFingers - touches.count) 人" }
            return "\(touches.count) 人 · 保持不动"
        case .counting: return "选择中…"
        case .done:
            switch mode {
            case .teams: return "分队完成！"
            default: return winners.count == 1 ? "就是你了！" : "选中了 \(winners.count) 位"
            }
        }
    }

    // MARK: 逻辑

    private func update(_ points: [Int: CGPoint]) {
        let added = Set(points.keys).subtracting(touches.keys)
        let removed = Set(touches.keys).subtracting(points.keys)
        touches = points
        for id in added {
            colors[id] = fingerColors[(colors.count) % fingerColors.count]
            Haptics.softImpact(0.7)
            SoundEngine.shared.play(.pop, volume: 0.5)
        }
        if !added.isEmpty || !removed.isEmpty {
            if phase == .done {
                if points.isEmpty { reset() }
            } else {
                // 人数变化，重新计时
                phase = .waiting
                countdownStart = nil
                stableSince = Date()
            }
        }
        if points.isEmpty { reset() }
    }

    private func tick(_ now: Date) {
        switch phase {
        case .waiting:
            if touches.count >= minimumFingers && now.timeIntervalSince(stableSince) > 1.0 {
                phase = .counting
                countdownStart = now
                Haptics.impact(0.5)
            }
        case .counting:
            guard let start = countdownStart else { return }
            let elapsed = now.timeIntervalSince(start)
            if elapsed >= countdown {
                choose()
            }
        case .done:
            break
        }
    }

    private func choose() {
        let ids = Array(touches.keys).shuffled()
        switch mode {
        case .one: winners = [ids[0]]
        case .several: winners = Set(ids.prefix(pickCount))
        case .teams:
            winners = []
            teams = Dictionary(uniqueKeysWithValues: ids.enumerated().map { ($0.element, $0.offset % 2) })
        }
        phase = .done
        resultTime = Date()
        SoundEngine.shared.play(mode == .teams ? .success : .chime)
        Haptics.reveal()
        // 读屏用户的手指都在触控区域上，焦点不会移到底部的状态文字，结果出来时主动播报
        Announce.post(statusText)
        let detail = mode == .teams ? "\(touches.count) 人分两队" : "\(touches.count) 人中选 \(winners.count) 人"
        HistoryLog.add(context, tool: ToolKind.finger.rawValue, title: mode.name, result: detail)
    }

    private func reset() {
        phase = .waiting
        winners = []
        teams = [:]
        colors = [:]
        countdownStart = nil
        resultTime = nil
    }

    // MARK: 绘制

    private func draw(in canvas: inout GraphicsContext, now: Date) {
        let t = now.timeIntervalSinceReferenceDate
        let progress: Double = {
            guard phase == .counting, let start = countdownStart else { return 0 }
            return min(1, now.timeIntervalSince(start) / countdown)
        }()
        let since = resultTime.map { now.timeIntervalSince($0) } ?? 0

        for (id, point) in touches {
            var color = colors[id] ?? .white
            var radius = 46 + (reduceMotion ? 0 : sin(t * 5 + Double(id)) * 4)
            var alpha = 1.0

            if phase == .done {
                if mode == .teams {
                    color = teamColors[teams[id] ?? 0]
                } else if winners.contains(id) {
                    radius = 56 + min(1, since * 3) * 24 + (reduceMotion ? 0 : sin(t * 8) * 5)
                } else {
                    alpha = max(0.12, 1 - since * 3)
                    radius = 40
                }
            } else if phase == .counting {
                radius -= progress * 8
            }

            // 光晕
            let glow = CGRect(x: point.x - radius * 2, y: point.y - radius * 2, width: radius * 4, height: radius * 4)
            canvas.fill(Path(ellipseIn: glow), with: .radialGradient(Gradient(colors: [color.opacity(0.45 * alpha), .clear]),
                                                                    center: point, startRadius: radius * 0.5, endRadius: radius * 2))
            // 实心圆
            let circle = CGRect(x: point.x - radius, y: point.y - radius, width: radius * 2, height: radius * 2)
            canvas.fill(Path(ellipseIn: circle), with: .color(color.opacity(0.85 * alpha)))
            canvas.stroke(Path(ellipseIn: circle.insetBy(dx: -8, dy: -8)), with: .color(color.opacity(0.6 * alpha)), lineWidth: 3)

            // 倒计时圆弧
            if phase == .counting {
                var arc = Path()
                arc.addArc(center: point, radius: radius + 16, startAngle: .degrees(-90), endAngle: .degrees(-90 + 360 * progress), clockwise: false)
                canvas.stroke(arc, with: .color(.white.opacity(0.9)), style: StrokeStyle(lineWidth: 5, lineCap: .round))
            }

            if phase == .done && mode == .teams {
                let label = Text(teams[id] == 0 ? "红队" : "蓝队").font(.headline.bold()).foregroundColor(.white)
                canvas.draw(label, at: CGPoint(x: point.x, y: point.y - radius - 30))
            } else if phase == .done && winners.contains(id) {
                canvas.draw(Text("★").font(.system(size: 34, weight: .bold)).foregroundColor(.white), at: point)
            }
        }
    }
}
