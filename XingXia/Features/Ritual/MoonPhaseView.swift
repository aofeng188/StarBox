import SwiftUI

/// 按真实亮面比例画的月亮（北半球看）：上半月亮面在右，下半月亮面在左，明暗交界线是半个椭圆。
/// 纯装饰，读屏文字由使用的地方给出。
struct MoonPhaseView: View {
    /// Astronomy.moonPhase 的 0…1：0 新月，0.25 上弦，0.5 满月，0.75 下弦
    let phase: Double
    var size: CGFloat = 120
    /// 亮面外的柔光，小图标可以关掉
    var glow = true
    /// false 时压暗、去色（徽章里还没集到的月相）
    var isLit = true

    @Environment(\.colorScheme) private var scheme

    init(phase: Double, size: CGFloat = 120, glow: Bool = true, isLit: Bool = true) {
        self.phase = phase
        self.size = size
        self.glow = glow
        self.isLit = isLit
    }

    /// 某一相的典型样子（取这一相正中的月相）
    init(_ name: MoonPhaseName, size: CGFloat = 120, glow: Bool = true, isLit: Bool = true) {
        self.init(phase: Double(name.rawValue) / 8, size: size, glow: glow, isLit: isLit)
    }

    private static let litLight = Color(hex: 0xFFF6DA)
    private static let litDark = Color(hex: 0xE2D3A4)

    /// 环形山：圆心和半径都是相对月面半径的比例
    private static let craters: [(x: CGFloat, y: CGFloat, r: CGFloat)] = [
        (-0.32, -0.30, 0.17), (0.28, -0.42, 0.10), (0.38, 0.12, 0.15),
        (-0.10, 0.36, 0.12), (-0.48, 0.18, 0.08), (0.08, -0.05, 0.07),
    ]

    var body: some View {
        let illumination = (1 - cos(2 * .pi * phase)) / 2
        let isDark = scheme == .dark
        Canvas { context, canvasSize in
            let radius = min(canvasSize.width, canvasSize.height) / 2
            let center = CGPoint(x: canvasSize.width / 2, y: canvasSize.height / 2)
            let disk = Path(ellipseIn: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2))

            // 暗面
            context.fill(disk, with: .color(isDark ? Color(hex: 0x23263A) : Color(hex: 0x474B63)))

            // 亮面：左上方打光的径向渐变
            let lit = Self.litPath(phase: phase, center: center, radius: radius)
            context.fill(lit, with: .radialGradient(Gradient(colors: [Self.litLight, Self.litDark]),
                                                    center: CGPoint(x: center.x - radius * 0.3, y: center.y - radius * 0.35),
                                                    startRadius: 0, endRadius: radius * 1.6))

            // 环形山只出现在亮面上
            var craterLayer = context
            craterLayer.clip(to: lit)
            for crater in Self.craters {
                let r = crater.r * radius
                let rect = CGRect(x: center.x + crater.x * radius - r, y: center.y + crater.y * radius - r, width: r * 2, height: r * 2)
                craterLayer.fill(Path(ellipseIn: rect), with: .color(Color(hex: 0xC9B98A).opacity(0.35)))
            }

            context.stroke(disk, with: .color(isDark ? Color.white.opacity(0.22) : Color.black.opacity(0.18)), lineWidth: max(0.6, radius * 0.025))
        }
        .frame(width: size, height: size)
        .shadow(color: glow && isLit ? Self.litLight.opacity(0.5 * illumination) : .clear, radius: size * 0.12)
        .saturation(isLit ? 1 : 0)
        .opacity(isLit ? 1 : 0.32)
        .accessibilityHidden(true)
    }

    /// 亮面的轮廓：亮的一侧的半圆边缘，加上明暗交界线（横向半轴为 r·cos(2π·phase) 的半椭圆）
    static func litPath(phase: Double, center c: CGPoint, radius r: CGFloat) -> Path {
        let p = phase - floor(phase)
        // 上半月亮面在右（+1），下半月在左（-1）
        let side: CGFloat = p < 0.5 ? 1 : -1
        let limb = side * r
        let terminator = side * CGFloat(cos(2 * .pi * p)) * r
        // 四分之一椭圆的贝塞尔控制点系数
        let k: CGFloat = 0.552_284_75
        var path = Path()
        path.move(to: CGPoint(x: c.x, y: c.y - r))
        // 上 → 边缘 → 下
        path.addCurve(to: CGPoint(x: c.x + limb, y: c.y),
                      control1: CGPoint(x: c.x + k * limb, y: c.y - r),
                      control2: CGPoint(x: c.x + limb, y: c.y - k * r))
        path.addCurve(to: CGPoint(x: c.x, y: c.y + r),
                      control1: CGPoint(x: c.x + limb, y: c.y + k * r),
                      control2: CGPoint(x: c.x + k * limb, y: c.y + r))
        // 下 → 交界线 → 上
        path.addCurve(to: CGPoint(x: c.x + terminator, y: c.y),
                      control1: CGPoint(x: c.x + k * terminator, y: c.y + r),
                      control2: CGPoint(x: c.x + terminator, y: c.y + k * r))
        path.addCurve(to: CGPoint(x: c.x, y: c.y - r),
                      control1: CGPoint(x: c.x + terminator, y: c.y - k * r),
                      control2: CGPoint(x: c.x + k * terminator, y: c.y - r))
        path.closeSubpath()
        return path
    }
}
