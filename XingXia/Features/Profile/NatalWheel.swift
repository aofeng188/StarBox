import SwiftUI

/// 出生时刻的太阳、月亮黄经和上升点（等宫制，从上升点起第一宫）。没有出生时间时不算上升、不分宫。
/// 和命盘页的三巨头用同一套算法，星座一定对得上；Android 的 NatalWheel.kt 与它一致
struct NatalChart {
    let sun: Double
    let moon: Double
    let ascendant: Double?

    init(birth: BirthInfo) {
        let moment = birth.moment
        sun = Astronomy.normalize(Astronomy.sunLongitude(moment))
        moon = Astronomy.normalize(Astronomy.moonLongitude(moment))
        if birth.hasTime {
            let city = birth.city
            ascendant = Astronomy.normalize(Astronomy.ascendant(moment, latitude: city.latitude, longitude: city.longitude))
        } else {
            ascendant = nil
        }
    }

    /// 星座内的度数（向下取整）
    static func degree(_ longitude: Double) -> Int {
        Int(Astronomy.normalize(longitude).truncatingRemainder(dividingBy: 30))
    }

    /// 「双子座 27°」
    static func position(_ longitude: Double) -> String {
        "\(ZodiacSign(longitude: longitude).name) \(degree(longitude))°"
    }

    /// 读屏读的一句话
    var spokenSummary: String {
        var parts = ["太阳在\(Self.position(sun))", "月亮在\(Self.position(moon))"]
        if let ascendant {
            parts.append("上升在\(Self.position(ascendant))")
        } else {
            parts.append("没有出生时间，未画上升和宫位")
        }
        return parts.joined(separator: "，")
    }
}

/// 十二宫星盘：外圈十二星座（符号 + 名字），内圈从上升点起按等宫制画十二宫，标出太阳、月亮和上升。
/// 上升点放在左边（九点钟方向），黄道逆时针排列；没有出生时间时白羊座 0° 放在左边，只画太阳和月亮
struct NatalWheel: View {
    let birth: BirthInfo

    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let chart = NatalChart(birth: birth)
        let colors = WheelColors(line: palette.accent, glyph: palette.ink(palette.accent), sun: Color.gold(scheme),
                                 moon: palette.ink(palette.accentSecondary), text: palette.isDark ? .white : Color(hex: 0x1B1726))
        VStack(spacing: 12) {
            Canvas { context, size in
                Self.draw(chart, colors: colors, in: context, size: size)
            }
            .aspectRatio(1, contentMode: .fit)
            .frame(maxWidth: 340)
            .frame(maxWidth: .infinity)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(chart.spokenSummary)
            .accessibilityAddTraits(.isImage)

            // 图例：跟随动态字体；读屏已经从星盘读到同样的内容
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 14) { legend(chart, colors: colors) }
                VStack(alignment: .leading, spacing: 6) { legend(chart, colors: colors) }
            }
            .font(.caption)
            .accessibilityHidden(true)
            if chart.ascendant == nil {
                Text("没有出生时间，未画上升和宫位")
                    .font(.caption)
                    .subtleForeground()
                    .accessibilityHidden(true)
            }
        }
    }

    @ViewBuilder
    private func legend(_ chart: NatalChart, colors: WheelColors) -> some View {
        Label { Text("太阳 \(NatalChart.position(chart.sun))") } icon: { Image(systemName: "sun.max.fill").foregroundStyle(colors.sun) }
        Label { Text("月亮 \(NatalChart.position(chart.moon))") } icon: { Image(systemName: "moon.fill").foregroundStyle(colors.moon) }
        if let ascendant = chart.ascendant {
            Label { Text("上升 \(NatalChart.position(ascendant))") } icon: { Image(systemName: "sunrise.fill").foregroundStyle(colors.glyph) }
        }
    }

    struct WheelColors {
        let line: Color
        let glyph: Color
        let sun: Color
        let moon: Color
        let text: Color
    }

    // MARK: 绘制（几何尺寸按星盘大小缩放，不随动态字体变化）

    private static func draw(_ chart: NatalChart, colors: WheelColors, in context: GraphicsContext, size: CGSize) {
        let side = Double(min(size.width, size.height))
        let center = CGPoint(x: Double(size.width) / 2, y: Double(size.height) / 2)
        let radius = side / 2 - 1.5
        let reference = chart.ascendant ?? 0
        let signOuter = radius, signInner = radius * 0.76, houseInner = radius * 0.64, core = radius * 0.22

        /// 黄经 → 画布上的点：参考点（上升或白羊 0°）在左边，逆时针增加
        func point(_ longitude: Double, _ r: Double) -> CGPoint {
            let angle = (180 + longitude - reference) * .pi / 180
            return CGPoint(x: Double(center.x) + r * cos(angle), y: Double(center.y) - r * sin(angle))
        }
        func circle(_ r: Double) -> Path {
            Path(ellipseIn: CGRect(x: Double(center.x) - r, y: Double(center.y) - r, width: r * 2, height: r * 2))
        }
        func segment(from a: Double, to b: Double, inner: Double, outer: Double) -> Path {
            var path = Path()
            let steps = 12
            path.addLines((0...steps).map { point(a + (b - a) * Double($0) / Double(steps), outer) }
                          + (0...steps).map { point(b - (b - a) * Double($0) / Double(steps), inner) })
            path.closeSubpath()
            return path
        }
        func line(_ longitude: Double, from r0: Double, to r1: Double) -> Path {
            var path = Path()
            path.move(to: point(longitude, r0))
            path.addLine(to: point(longitude, r1))
            return path
        }
        func text(_ string: String, size: Double, weight: Font.Weight = .regular, color: Color, at position: CGPoint) {
            context.draw(Text(string).font(.system(size: size, weight: weight)).foregroundStyle(color), at: position, anchor: .center)
        }

        // 星座带：相邻星座深浅交替
        for i in 0..<12 {
            let start = Double(i) * 30
            context.fill(segment(from: start, to: start + 30, inner: signInner, outer: signOuter),
                         with: .color(colors.line.opacity(i % 2 == 0 ? 0.12 : 0.05)))
        }
        context.fill(circle(core), with: .color(colors.line.opacity(0.06)))
        context.stroke(circle(signOuter), with: .color(colors.line.opacity(0.75)), lineWidth: 1.2)
        context.stroke(circle(signInner), with: .color(colors.line.opacity(0.55)), lineWidth: 0.8)
        context.stroke(circle(houseInner), with: .color(colors.line.opacity(0.3)), lineWidth: 0.6)
        context.stroke(circle(core), with: .color(colors.line.opacity(0.3)), lineWidth: 0.6)
        // 星座分界和每 5° 的刻度
        for i in 0..<72 {
            let longitude = Double(i) * 5
            if i % 6 == 0 {
                context.stroke(line(longitude, from: signInner, to: signOuter), with: .color(colors.line.opacity(0.55)), lineWidth: 0.8)
            } else {
                let tick = i % 2 == 0 ? radius * 0.035 : radius * 0.02
                context.stroke(line(longitude, from: signInner, to: signInner + tick), with: .color(colors.line.opacity(0.4)), lineWidth: 0.5)
            }
        }
        // 星座符号和名字（字保持正立）
        for sign in ZodiacSign.allCases {
            let middle = Double(sign.rawValue) * 30 + 15
            text(sign.glyph, size: radius * 0.093, weight: .medium, color: colors.glyph, at: point(middle, radius * 0.93))
            text(sign.shortName, size: radius * 0.054, color: colors.text.opacity(0.7), at: point(middle, radius * 0.825))
        }

        // 十二宫：等宫制，第一宫从上升点起，逆时针
        if let ascendant = chart.ascendant {
            for house in 0..<12 {
                let cusp = ascendant + Double(house) * 30
                let axis = house == 0 || house == 6
                if !axis {
                    context.stroke(line(cusp, from: core, to: signInner), with: .color(colors.line.opacity(0.4)), lineWidth: 0.6)
                }
                text("\(house + 1)", size: radius * 0.05, color: colors.text.opacity(0.55), at: point(cusp + 15, radius * 0.70))
            }
            // 上升—下降轴；ASC 标在轴的上方，放在太阳、月亮符号够不到的半径上
            var axis = Path()
            axis.move(to: point(ascendant, signInner))
            axis.addLine(to: point(ascendant + 180, signInner))
            context.stroke(axis, with: .color(colors.glyph), lineWidth: 1.4)
            text("ASC", size: radius * 0.056, weight: .bold, color: colors.glyph, at: point(ascendant - 7, radius * 0.6))
        }

        // 太阳、月亮：内圈边上一道刻线标准确位置，符号放在里面；两者靠得太近时符号往两边错开，用虚线连回刻线
        let shown = spread(chart.sun, chart.moon, minimum: 20)
        let bodyRadius = radius * 0.47
        let mark = radius * 0.055
        for (longitude, angle, color) in [(chart.sun, shown.0, colors.sun), (chart.moon, shown.1, colors.moon)] {
            context.stroke(line(longitude, from: signInner - radius * 0.06, to: signInner), with: .color(color), lineWidth: 1.6)
            var guide = Path()
            guide.move(to: point(angle, bodyRadius + mark * 1.5))
            guide.addLine(to: point(longitude, signInner - radius * 0.06))
            context.stroke(guide, with: .color(color.opacity(0.45)), style: StrokeStyle(lineWidth: 0.6, dash: [2, 2]))
            let c = point(angle, bodyRadius)
            context.fill(Path(ellipseIn: CGRect(x: Double(c.x) - mark * 1.8, y: Double(c.y) - mark * 1.8, width: mark * 3.6, height: mark * 3.6)),
                         with: .color(color.opacity(0.14)))
            text("\(NatalChart.degree(longitude))°", size: radius * 0.05, color: colors.text.opacity(0.7), at: point(angle, bodyRadius - mark * 2.4))
        }
        // 太阳：圆圈加中心点
        let sunCenter = point(shown.0, bodyRadius)
        context.stroke(Path(ellipseIn: CGRect(x: Double(sunCenter.x) - mark, y: Double(sunCenter.y) - mark, width: mark * 2, height: mark * 2)),
                       with: .color(colors.sun), lineWidth: 1.4)
        context.fill(Path(ellipseIn: CGRect(x: Double(sunCenter.x) - mark * 0.28, y: Double(sunCenter.y) - mark * 0.28, width: mark * 0.56, height: mark * 0.56)),
                     with: .color(colors.sun))
        // 月亮：亮面朝左的月牙
        let moonCenter = point(shown.1, bodyRadius)
        var moon = Path()
        moon.addLines(TarotArtGeometry.crescentPoints(x: Double(moonCenter.x), y: Double(moonCenter.y), r: mark * 1.1, a: 180, k: 0.36))
        moon.closeSubpath()
        context.fill(moon, with: .color(colors.moon))
    }

    /// 两个黄经在盘上至少隔开 minimum 度：太近时以两者中点为中心各往两边让开，返回画符号用的角度（Android 同一算法）
    static func spread(_ a: Double, _ b: Double, minimum: Double) -> (Double, Double) {
        let d = Astronomy.normalize(b - a + 180) - 180
        guard abs(d) < minimum else { return (a, b) }
        let middle = a + d / 2
        let half = (d < 0 ? -minimum : minimum) / 2
        return (middle - half, middle + half)
    }
}
