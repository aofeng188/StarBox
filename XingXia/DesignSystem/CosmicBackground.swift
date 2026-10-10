import SwiftUI

/// 动态星空背景：流动的星云网格渐变 + 闪烁星点 + 偶尔划过的流星。
/// 浅色模式下变成柔和的晨曦光斑。开启「减弱动态效果」时保持静止。
struct CosmicBackground: View {
    var dimmed = false

    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(AppSettings.self) private var settings

    var body: some View {
        let animate = !reduceMotion && settings.backgroundAnimation
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: !animate)) { context in
            let t = animate ? context.date.timeIntervalSinceReferenceDate : 0
            ZStack {
                palette.backgroundGradient
                NebulaMesh(time: t, palette: palette)
                    .opacity(palette.isDark ? (dimmed ? 0.45 : 0.7) : (dimmed ? 0.6 : 0.85))
                Canvas(rendersAsynchronously: false) { context, size in
                    if palette.isDark {
                        StarField.draw(in: &context, size: size, time: t, palette: palette)
                    } else {
                        StarField.drawDaylight(in: &context, size: size, time: t, palette: palette)
                    }
                }
            }
        }
        .ignoresSafeArea()
        .accessibilityHidden(true)
    }
}

private struct NebulaMesh: View {
    let time: Double
    let palette: ThemePalette

    var body: some View {
        let t = Float(time)
        let n = palette.nebula
        let base = palette.backgroundTop
        let deep = palette.backgroundBottom
        let a: Float = palette.isDark ? 0.9 : 1
        MeshGradient(
            width: 3, height: 3,
            points: [
                [0, 0], [0.5 + 0.08 * sin(t * 0.11), 0], [1, 0],
                [0, 0.45 + 0.1 * sin(t * 0.17)], [0.5 + 0.18 * sin(t * 0.13), 0.5 + 0.14 * cos(t * 0.09)], [1, 0.55 + 0.1 * cos(t * 0.15)],
                [0, 1], [0.5 + 0.1 * cos(t * 0.12), 1], [1, 1],
            ],
            colors: [
                base, n[0].opacity(Double(a) * 0.55), base,
                n[1].opacity(Double(a) * 0.45), deep.opacity(0.2), n[2].opacity(Double(a) * 0.5),
                deep, n[3].opacity(Double(a) * 0.4), deep,
            ],
            smoothsColors: true
        )
    }
}

enum StarField {
    struct Star {
        let x: Double
        let y: Double
        let radius: Double
        let speed: Double
        let phase: Double
        let tint: Int
        let sparkle: Bool
    }

    static let stars: [Star] = {
        var rng = SeededGenerator(seed: 20_261_009)
        return (0..<150).map { _ in
            let big = rng.chance(0.08)
            return Star(x: rng.unit(), y: rng.unit(),
                        radius: big ? rng.double(1.4...2.4) : rng.double(0.4...1.2),
                        speed: rng.double(0.6...2.4), phase: rng.double(0...(2 * .pi)),
                        tint: rng.chance(0.25) ? rng.int(below: 4) : -1,
                        sparkle: big && rng.chance(0.6))
        }
    }()

    static func draw(in context: inout GraphicsContext, size: CGSize, time: Double, palette: ThemePalette) {
        for star in stars {
            let twinkle = 0.55 + 0.45 * sin(time * star.speed + star.phase)
            let point = CGPoint(x: star.x * size.width, y: star.y * size.height)
            let color = star.tint >= 0 ? palette.nebula[star.tint] : palette.star
            let r = star.radius
            if star.radius > 1.3 {
                let glow = Path(ellipseIn: CGRect(x: point.x - r * 4, y: point.y - r * 4, width: r * 8, height: r * 8))
                context.fill(glow, with: .radialGradient(Gradient(colors: [color.opacity(0.35 * twinkle), .clear]),
                                                         center: point, startRadius: 0, endRadius: r * 4))
            }
            context.fill(Path(ellipseIn: CGRect(x: point.x - r, y: point.y - r, width: r * 2, height: r * 2)),
                         with: .color(color.opacity(0.35 + 0.65 * twinkle)))
            if star.sparkle {
                let len = r * 5 * twinkle
                var cross = Path()
                cross.move(to: CGPoint(x: point.x - len, y: point.y))
                cross.addLine(to: CGPoint(x: point.x + len, y: point.y))
                cross.move(to: CGPoint(x: point.x, y: point.y - len))
                cross.addLine(to: CGPoint(x: point.x, y: point.y + len))
                context.stroke(cross, with: .color(color.opacity(0.5 * twinkle)), lineWidth: 0.6)
            }
        }
        drawShootingStar(in: &context, size: size, time: time, palette: palette)
    }

    private static func drawShootingStar(in context: inout GraphicsContext, size: CGSize, time: Double, palette: ThemePalette) {
        let period = 11.0
        let cycle = Int(time / period)
        let local = time - Double(cycle) * period
        let duration = 1.1
        guard local < duration, time > 0 else { return }
        var rng = SeededGenerator("meteor", cycle)
        let start = CGPoint(x: size.width * rng.double(0.3...1.1), y: size.height * rng.double(-0.05...0.35))
        let angle = rng.double(2.4...2.75)
        let distance = size.width * 0.75
        let p = local / duration
        let head = CGPoint(x: start.x + cos(angle) * distance * p, y: start.y + sin(angle) * distance * p)
        let tailLength = 120.0 * sin(.pi * p)
        let tail = CGPoint(x: head.x - cos(angle) * tailLength, y: head.y - sin(angle) * tailLength)
        var path = Path()
        path.move(to: tail)
        path.addLine(to: head)
        let alpha = sin(.pi * p)
        context.stroke(path, with: .linearGradient(Gradient(colors: [.clear, palette.star.opacity(0.9 * alpha)]),
                                                   startPoint: tail, endPoint: head),
                       style: StrokeStyle(lineWidth: 1.6, lineCap: .round))
        context.fill(Path(ellipseIn: CGRect(x: head.x - 2, y: head.y - 2, width: 4, height: 4)), with: .color(palette.star.opacity(alpha)))
    }

    /// 浅色模式：漂浮的柔光光斑 + 少量细碎闪光
    static func drawDaylight(in context: inout GraphicsContext, size: CGSize, time: Double, palette: ThemePalette) {
        for (i, star) in stars.prefix(18).enumerated() {
            let drift = sin(time * 0.05 * star.speed + star.phase)
            let point = CGPoint(x: (star.x + 0.04 * drift) * size.width,
                                y: (star.y + 0.03 * cos(time * 0.04 * star.speed + star.phase)) * size.height)
            let r = 40 + star.radius * 40
            let color = palette.nebula[i % palette.nebula.count]
            context.fill(Path(ellipseIn: CGRect(x: point.x - r, y: point.y - r, width: r * 2, height: r * 2)),
                         with: .radialGradient(Gradient(colors: [color.opacity(0.45), color.opacity(0)]),
                                               center: point, startRadius: 0, endRadius: r))
        }
        for star in stars.dropFirst(18).prefix(50) {
            let twinkle = max(0, sin(time * star.speed + star.phase))
            let point = CGPoint(x: star.x * size.width, y: star.y * size.height)
            let r = star.radius * 0.9
            context.fill(Path(ellipseIn: CGRect(x: point.x - r, y: point.y - r, width: r * 2, height: r * 2)),
                         with: .color(palette.star.opacity(0.15 + 0.45 * twinkle)))
        }
    }
}

extension View {
    /// 页面通用背景：铺满整个导航容器的星空，并隐藏列表默认底色。
    /// 所有页面（包括弹出的 sheet）都放在 NavigationStack 里，以便使用这个背景。
    func cosmicBackground(dimmed: Bool = false) -> some View {
        self
            .scrollContentBackground(.hidden)
            .containerBackground(for: .navigation) { CosmicBackground(dimmed: dimmed) }
    }
}
