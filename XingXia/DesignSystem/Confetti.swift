import SwiftUI

/// 庆祝用的彩带/星屑粒子。每次 trigger 变化时从顶部中央炸开一次。
struct ConfettiView: View {
    let trigger: Int
    var origin: UnitPoint = .init(x: 0.5, y: 0.35)
    var count = 90

    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var burst: Burst?

    struct Particle {
        let angle: Double
        let speed: Double
        let spin: Double
        let size: CGSize
        let colorIndex: Int
        let shape: Int
        let wobble: Double
    }

    struct Burst {
        let start: Date
        let particles: [Particle]
    }

    var body: some View {
        TimelineView(.animation(paused: burst == nil)) { context in
            Canvas { canvas, size in
                guard let burst else { return }
                let t = context.date.timeIntervalSince(burst.start)
                guard t < 3.2 else { return }
                let center = CGPoint(x: size.width * origin.x, y: size.height * origin.y)
                let colors = palette.spectrum + [.white, Color(hex: 0xFFD166)]
                for p in burst.particles {
                    let vx = cos(p.angle) * p.speed
                    let vy = sin(p.angle) * p.speed
                    // 空气阻力 + 重力
                    let drag = 1 - exp(-t * 1.6)
                    let x = center.x + vx / 1.6 * drag + sin(t * 6 + p.wobble) * 10 * t
                    let y = center.y + vy / 1.6 * drag + 260 * t * t * 0.5
                    let alpha = max(0, 1 - t / 3.2)
                    var particle = canvas
                    particle.opacity = alpha
                    particle.translateBy(x: x, y: y)
                    particle.rotate(by: .radians(p.spin * t))
                    let rect = CGRect(x: -p.size.width / 2, y: -p.size.height / 2, width: p.size.width, height: p.size.height)
                    let color = colors[p.colorIndex % colors.count]
                    switch p.shape {
                    case 0: particle.fill(Path(rect), with: .color(color))
                    case 1: particle.fill(Path(ellipseIn: rect), with: .color(color))
                    default: particle.fill(StarShape().path(in: rect.insetBy(dx: -2, dy: -2)), with: .color(color))
                    }
                }
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
        .onChange(of: trigger) { _, _ in
            guard !reduceMotion else { return }
            let start = Date()
            burst = Burst(start: start, particles: (0..<count).map { _ in
                Particle(angle: Double.random(in: (-.pi * 0.95)...(-.pi * 0.05)),
                         speed: Double.random(in: 380...900),
                         spin: Double.random(in: -8...8),
                         size: CGSize(width: Double.random(in: 5...10), height: Double.random(in: 8...14)),
                         colorIndex: Int.random(in: 0..<20),
                         shape: Int.random(in: 0...2),
                         wobble: Double.random(in: 0...6))
            })
            Task {
                try? await Task.sleep(for: .seconds(3.3))
                // 3.3 秒内又撒了一次时，不要把新的那一把清掉
                if burst?.start == start { burst = nil }
            }
        }
    }
}

struct StarShape: Shape {
    var points = 5
    var innerRatio = 0.45

    func path(in rect: CGRect) -> Path {
        let center = CGPoint(x: rect.midX, y: rect.midY)
        let outer = min(rect.width, rect.height) / 2
        let inner = outer * innerRatio
        var path = Path()
        for i in 0..<(points * 2) {
            let angle = Double(i) * .pi / Double(points) - .pi / 2
            let r = i % 2 == 0 ? outer : inner
            let point = CGPoint(x: center.x + cos(angle) * r, y: center.y + sin(angle) * r)
            i == 0 ? path.move(to: point) : path.addLine(to: point)
        }
        path.closeSubpath()
        return path
    }
}
