import SwiftUI
import QuartzCore

/// 用 CADisplayLink 逐帧驱动转盘角度，便于在每次经过扇区边界时发出「咔哒」声和震动。
@Observable
@MainActor
final class WheelSpinner: NSObject {
    /// 顺时针角度（度）
    var angle: Double = 0
    private(set) var isSpinning = false

    private var link: CADisplayLink?
    private var startAngle: Double = 0
    private var targetAngle: Double = 0
    private var startTime: CFTimeInterval = 0
    private var duration: Double = 4
    private var completion: (() -> Void)?
    var onFrame: ((Double) -> Void)?

    func spin(to target: Double, duration: Double, completion: @escaping () -> Void) {
        startAngle = angle
        targetAngle = target
        self.duration = duration
        self.completion = completion
        startTime = CACurrentMediaTime()
        isSpinning = true
        link?.invalidate()
        let link = CADisplayLink(target: self, selector: #selector(step))
        link.preferredFrameRateRange = CAFrameRateRange(minimum: 60, maximum: 120, preferred: 120)
        link.add(to: .main, forMode: .common)
        self.link = link
    }

    @objc private func step(_ link: CADisplayLink) {
        let p = min(1, (link.timestamp - startTime) / duration)
        // 先快后慢（ease-out quint），最后一段有明显的「快停下又走了一格」的悬念
        let eased = 1 - pow(1 - p, 4.2)
        angle = startAngle + (targetAngle - startAngle) * eased
        onFrame?(angle)
        if p >= 1 {
            link.invalidate()
            self.link = nil
            isSpinning = false
            completion?()
            completion = nil
        }
    }

    func stop() {
        link?.invalidate()
        link = nil
        isSpinning = false
    }
}

struct WheelSegment: Identifiable, Hashable {
    let id: UUID
    let title: String
    let weight: Double
}

/// 转盘绘制
struct WheelCanvas: View {
    let segments: [WheelSegment]
    let rotation: Double
    let colors: [Color]
    let lightsOn: Bool

    var body: some View {
        GeometryReader { proxy in
            let size = min(proxy.size.width, proxy.size.height)
            let radius = size / 2
            ZStack {
                // 外圈
                Circle()
                    .fill(LinearGradient(colors: [Color(hex: 0x2A2340), Color(hex: 0x120E22)], startPoint: .top, endPoint: .bottom))
                    .overlay(Circle().strokeBorder(LinearGradient(colors: [Color(hex: 0xF5D78E), Color(hex: 0xB8862E)], startPoint: .top, endPoint: .bottom), lineWidth: 4))
                    .shadow(color: .black.opacity(0.4), radius: 16, y: 8)

                Canvas { context, canvasSize in
                    let center = CGPoint(x: canvasSize.width / 2, y: canvasSize.height / 2)
                    let r = radius * 0.86
                    let total = max(0.0001, segments.reduce(0) { $0 + $1.weight })
                    var start = -90.0
                    for (index, segment) in segments.enumerated() {
                        let sweep = segment.weight / total * 360
                        var path = Path()
                        path.move(to: center)
                        path.addArc(center: center, radius: r, startAngle: .degrees(start), endAngle: .degrees(start + sweep), clockwise: false)
                        path.closeSubpath()
                        let color = Self.color(for: index, count: segments.count, colors: colors)
                        context.fill(path, with: .linearGradient(Gradient(colors: [color, color.opacity(0.75)]),
                                                                 startPoint: center,
                                                                 endPoint: CGPoint(x: center.x + cos((start + sweep / 2) * .pi / 180) * r,
                                                                                   y: center.y + sin((start + sweep / 2) * .pi / 180) * r)))
                        context.stroke(path, with: .color(.white.opacity(0.35)), lineWidth: 1)

                        // 文字沿半径方向，位于中心按钮和外圈之间；转到左半边时翻转，保证不倒着读
                        let mid = start + sweep / 2
                        let flip = cos((mid + rotation) * .pi / 180) < 0
                        let band = r * 0.57
                        let fontSize = min(18, max(9, band * sweep * .pi / 180 * 0.6))
                        let title = segment.title.count > 10 ? String(segment.title.prefix(9)) + "…" : segment.title
                        // 扇区底色深浅不一，文字按底色亮度取深字或白字
                        let resolved = context.resolve(Text(title).font(.system(size: fontSize, weight: .bold)).foregroundColor(Self.labelColor(on: color)))
                        let measured = resolved.measure(in: CGSize(width: 1000, height: 100))
                        let scale = min(1, (r * 0.56) / max(1, measured.width))
                        var text = context
                        text.translateBy(x: center.x, y: center.y)
                        text.rotate(by: .degrees(mid))
                        text.translateBy(x: band, y: 0)
                        if flip { text.rotate(by: .degrees(180)) }
                        text.scaleBy(x: scale, y: scale)
                        text.draw(resolved, at: .zero, anchor: .center)
                        start += sweep
                    }
                    // 内圈高光
                    context.fill(Path(ellipseIn: CGRect(x: center.x - r, y: center.y - r, width: r * 2, height: r * 2)),
                                 with: .radialGradient(Gradient(colors: [.white.opacity(0.0), .white.opacity(0.0), .black.opacity(0.25)]),
                                                       center: center, startRadius: 0, endRadius: r))
                }
                .rotationEffect(.degrees(rotation))

                // 跑马灯
                ForEach(0..<24, id: \.self) { i in
                    let a = Double(i) / 24 * 2 * .pi
                    let on = lightsOn ? (i + Int(rotation / 15)) % 2 == 0 : i % 2 == 0
                    Circle()
                        .fill(on ? Color(hex: 0xFFF3C4) : Color(hex: 0x8A6A2A))
                        .frame(width: size * 0.028, height: size * 0.028)
                        .shadow(color: on ? Color(hex: 0xFFD166) : .clear, radius: 4)
                        .offset(x: cos(a) * radius * 0.93, y: sin(a) * radius * 0.93)
                }
            }
            .frame(width: size, height: size)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .aspectRatio(1, contentMode: .fit)
    }

    static func color(for index: Int, count: Int, colors: [Color]) -> Color {
        var i = index % colors.count
        // 首尾相邻时避免同色
        if index == count - 1 && count > 1 && i == 0 { i = (i + 1) % colors.count }
        return colors[i]
    }

    /// 扇区标签的文字色。扇区外缘半透明、压在深色底盘上，外圈还有一层暗角，文字处比填充色暗，
    /// 所以换成深字的亮度分界比通用的 readableForeground（0.18）高，按各主题实测取 0.3
    static func labelColor(on fill: Color) -> Color {
        fill.relativeLuminance > 0.3 ? Color(hex: 0x14101F) : .white
    }

    /// 扇区区间（从正上方顺时针，单位度）
    static func ranges(_ segments: [WheelSegment]) -> [ClosedRange<Double>] {
        let total = max(0.0001, segments.reduce(0) { $0 + $1.weight })
        var start = 0.0
        return segments.map { segment in
            let sweep = segment.weight / total * 360
            defer { start += sweep }
            return start...(start + sweep)
        }
    }

    /// 当前指针（正上方）指向的扇区
    static func index(at rotation: Double, segments: [WheelSegment]) -> Int {
        let pointer = Astronomy.normalize(-rotation)
        return ranges(segments).firstIndex { $0.contains(pointer) } ?? 0
    }
}

/// 指针
struct WheelPointer: View {
    var body: some View {
        ZStack {
            Triangle()
                .fill(LinearGradient(colors: [Color(hex: 0xFFF3C4), Color(hex: 0xF5B83D)], startPoint: .top, endPoint: .bottom))
                .frame(width: 30, height: 38)
                .shadow(color: Color(hex: 0xFFD166).opacity(0.9), radius: 8)
            Triangle()
                .stroke(Color(hex: 0x8A6A2A), lineWidth: 1.5)
                .frame(width: 30, height: 38)
        }
        .accessibilityHidden(true)
    }
}

struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.midX, y: rect.maxY))
        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))
        path.closeSubpath()
        return path
    }
}
