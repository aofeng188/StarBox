import SwiftUI

// MARK: - 线稿数据

/// tarot-art.json 里的一个图元。坐标在 100 × 160 的牌面里（y 向下，角度单位是度，0 指向右、顺时针），
/// 字段含义见文件里的 _doc；Android 的 ArtShape 与它一一对应
struct TarotArtShape: Decodable, Sendable {
    enum Kind: String, Sendable {
        case circle, ring, star, crescent, line, polyline, polygon, arc, suit, glyph

        /// 缺省的填充不透明度
        var defaultFill: Double {
            switch self {
            case .circle, .star, .crescent, .glyph: 1
            case .ring, .line, .polyline, .polygon, .arc, .suit: 0
            }
        }

        /// 缺省的线宽（牌面单位）；花色图元上是模板线宽的倍数
        var defaultWidth: Double {
            switch self {
            case .ring, .line, .polyline, .polygon, .arc: 0.8
            case .suit: 1
            case .circle, .star, .crescent, .glyph: 0
            }
        }
    }

    let t: String
    var c: String?
    var o: Double?
    var w: Double?
    var f: Double?
    var x: Double?
    var y: Double?
    var r: Double?
    var n: Int?
    var k: Double?
    var a: Double?
    var a0: Double?
    var a1: Double?
    var rot: Double?
    var p: [Double]?
    var s: String?
    var g: String?

    var kind: Kind? { Kind(rawValue: t) }
    var opacity: Double { o ?? 1 }
    var radius: Double { r ?? 0 }

    /// 填充不透明度（乘在 o 上）；线、折线和弧只描边
    var fillAlpha: Double {
        switch kind {
        case .line?, .polyline?, .arc?: return 0
        default: return f ?? kind?.defaultFill ?? 0
        }
    }

    var strokeWidth: Double { w ?? kind?.defaultWidth ?? 0 }

    /// 多边形、星形、月牙是闭合的
    var isClosed: Bool { kind == .polygon || kind == .star || kind == .crescent }

    /// 画成折线 / 多边形的图元采样出的点，和 Android 的 ArtShape.outline() 一致
    func outline() -> [CGPoint] {
        let cx = x ?? 0, cy = y ?? 0
        switch kind {
        case .star?:
            return TarotArtGeometry.starPoints(x: cx, y: cy, r: radius, n: n ?? 5, k: k ?? 0.45, rot: rot ?? -90)
        case .crescent?:
            return TarotArtGeometry.crescentPoints(x: cx, y: cy, r: radius, a: a ?? 0, k: k ?? 0.4)
        case .arc?:
            return TarotArtGeometry.arcPoints(x: cx, y: cy, r: radius, a0: a0 ?? 0, a1: a1 ?? 0)
        case .line?, .polyline?, .polygon?:
            let values = p ?? []
            return stride(from: 0, to: values.count - 1, by: 2).map { CGPoint(x: values[$0], y: values[$0 + 1]) }
        default:
            return []
        }
    }

    /// 牌面外框上用的花色小符号
    static func suit(_ name: String, x: Double, y: Double, r: Double, color: String) -> TarotArtShape {
        TarotArtShape(t: Kind.suit.rawValue, c: color, x: x, y: y, r: r, s: name)
    }
}

/// tarot-art.json：四个花色符号模板和 78 张牌的线稿（Shared/Content，两端共用）
struct TarotArtLibrary: Decodable, Sendable {
    let suits: [String: [TarotArtShape]]
    let cards: [String: [TarotArtShape]]

    /// 读不到或解析失败时为空：牌面只画外框和牌名，不崩溃
    static let shared: TarotArtLibrary = {
        guard let url = Bundle.main.url(forResource: "tarot-art", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let library = try? JSONDecoder().decode(TarotArtLibrary.self, from: data) else {
            return TarotArtLibrary(suits: [:], cards: [:])
        }
        return library
    }()

    func shapes(for cardID: String) -> [TarotArtShape] { cards[cardID] ?? [] }
}

/// 星形、圆弧、月牙的采样，和 Android core 的 TarotArt.starPoints / arcPoints / crescentPoints 同一套算法
enum TarotArtGeometry {
    static func starPoints(x: Double, y: Double, r: Double, n: Int, k: Double, rot: Double) -> [CGPoint] {
        let count = max(n, 2)
        return (0..<(count * 2)).map { i in
            let radius = i % 2 == 0 ? r : r * k
            let angle = (rot + Double(i) * 180 / Double(count)) * .pi / 180
            return CGPoint(x: x + radius * cos(angle), y: y + radius * sin(angle))
        }
    }

    static func arcPoints(x: Double, y: Double, r: Double, a0: Double, a1: Double) -> [CGPoint] {
        let steps = max(6, Int((abs(a1 - a0) / 8).rounded(.up)))
        return (0...steps).map { i in
            let angle = (a0 + (a1 - a0) * Double(i) / Double(steps)) * .pi / 180
            return CGPoint(x: x + r * cos(angle), y: y + r * sin(angle))
        }
    }

    /// 半径 r 的圆减去向反方向平移 d = 2r·k 的同样大的圆，亮面朝 a 度
    static func crescentPoints(x: Double, y: Double, r: Double, a: Double, k: Double) -> [CGPoint] {
        let d = 2 * r * min(max(k, 0.05), 0.95)
        let alpha = acos(-d / (2 * r)) * 180 / .pi
        let beta = acos(d / (2 * r)) * 180 / .pi
        let ux = cos(a * .pi / 180), uy = sin(a * .pi / 180)
        return arcPoints(x: x, y: y, r: r, a0: a - alpha, a1: a + alpha)
            + arcPoints(x: x - ux * d, y: y - uy * d, r: r, a0: a + beta, a1: a - beta)
    }
}

// MARK: - 颜色

extension ThemePalette {
    /// 牌面是深色的「物件」：浅色模式下也用同一主题的深色配色来画
    var tarotCardPalette: ThemePalette {
        if isDark { return self }
        let theme = AppTheme.all.first { $0.light.accent == accent && $0.light.accentSecondary == accentSecondary }
        return theme?.dark ?? AppTheme.nebula.dark
    }
}

/// 线稿的六个语义色，按当前主题解析
struct TarotArtPalette {
    /// 牌框、牌号的金色（和牌背一致）
    static let gold = Color(hex: 0xF5D78E)

    /// 解析时用的主题配色（总是深色版本）
    let base: ThemePalette
    let accent: Color
    let accent2: Color
    let gold: Color
    let ink: Color
    let star: Color
    let soft: Color

    init(_ palette: ThemePalette) {
        let p = palette.tarotCardPalette
        base = p
        accent = p.accent
        accent2 = p.accentSecondary
        gold = Self.gold
        ink = p.star.mix(with: p.accent, by: 0.12)
        star = p.star.mix(with: Self.gold, by: 0.3)
        soft = p.accent.mix(with: p.backgroundBottom, by: 0.45)
    }

    func color(_ name: String?) -> Color? {
        switch name {
        case "accent": return accent
        case "accent2": return accent2
        case "gold": return gold
        case "ink": return ink
        case "star": return star
        case "soft": return soft
        default: return nil
        }
    }
}

// MARK: - 绘制

/// 牌面坐标到画布坐标的变换：等比缩放 + 旋转 + 平移（花色符号的模板在里面再套一层）
struct TarotArtTransform {
    var origin: CGPoint
    var scale: Double
    /// 度
    var rotation: Double = 0

    /// 把 100 × 160 的牌面等比放进 size，居中
    static func fitting(_ size: CGSize) -> TarotArtTransform {
        let width = Double(size.width), height = Double(size.height)
        let scale = min(width / 100, height / 160)
        return TarotArtTransform(origin: CGPoint(x: (width - 100 * scale) / 2, y: (height - 160 * scale) / 2), scale: scale)
    }

    func map(_ x: Double, _ y: Double) -> CGPoint {
        let angle = rotation * .pi / 180
        let c = cos(angle), s = sin(angle)
        let ox = Double(origin.x), oy = Double(origin.y)
        return CGPoint(x: ox + scale * (c * x - s * y), y: oy + scale * (s * x + c * y))
    }

    func map(_ point: CGPoint) -> CGPoint { map(Double(point.x), Double(point.y)) }

    /// 以 (x, y) 为中心、放大 r 倍、再转 rot 度的子坐标系
    func child(x: Double, y: Double, r: Double, rot: Double) -> TarotArtTransform {
        TarotArtTransform(origin: map(x, y), scale: scale * r, rotation: rotation + rot)
    }
}

enum TarotArtRenderer {
    /// unit 是一个牌面单位对应的点数，线宽按它换算（花色模板里的线宽也按牌面单位，不随符号大小变粗）
    static func draw(_ shapes: [TarotArtShape], in context: GraphicsContext, transform: TarotArtTransform, unit: Double,
                     colors: TarotArtPalette, inherited: Color? = nil, opacity: Double = 1, widthScale: Double = 1) {
        for shape in shapes {
            guard let kind = shape.kind else { continue }
            let color = colors.color(shape.c) ?? inherited ?? colors.ink
            let alpha = shape.opacity * opacity
            switch kind {
            case .suit:
                guard let name = shape.s, let template = TarotArtLibrary.shared.suits[name], shape.radius > 0 else { continue }
                let child = transform.child(x: shape.x ?? 0, y: shape.y ?? 0, r: shape.radius, rot: shape.rot ?? 0)
                draw(template, in: context, transform: child, unit: unit, colors: colors,
                     inherited: colors.color(shape.c) ?? inherited, opacity: alpha, widthScale: widthScale * shape.strokeWidth)
            case .glyph:
                guard let text = shape.g, !text.isEmpty else { continue }
                let size = CGFloat(shape.radius * transform.scale)
                guard size > 0 else { continue }
                context.draw(Text(text).font(.system(size: size, weight: .medium, design: .serif)).foregroundStyle(color.opacity(alpha)),
                             at: transform.map(shape.x ?? 0, shape.y ?? 0), anchor: .center)
            case .circle, .ring:
                let center = transform.map(shape.x ?? 0, shape.y ?? 0)
                let r = CGFloat(shape.radius * transform.scale)
                let path = Path(ellipseIn: CGRect(x: center.x - r, y: center.y - r, width: r * 2, height: r * 2))
                paint(path, closed: true, shape: shape, color: color, alpha: alpha, lineWidth: shape.strokeWidth * widthScale * unit, in: context)
            case .star, .crescent, .line, .polyline, .polygon, .arc:
                let points = shape.outline().map { transform.map($0) }
                guard points.count >= 2 else { continue }
                var path = Path()
                path.addLines(points)
                if shape.isClosed { path.closeSubpath() }
                paint(path, closed: shape.isClosed, shape: shape, color: color, alpha: alpha, lineWidth: shape.strokeWidth * widthScale * unit, in: context)
            }
        }
    }

    private static func paint(_ path: Path, closed: Bool, shape: TarotArtShape, color: Color, alpha: Double, lineWidth: Double, in context: GraphicsContext) {
        let fill = shape.fillAlpha
        if closed, fill > 0 {
            context.fill(path, with: .color(color.opacity(alpha * fill)))
        }
        if lineWidth > 0 {
            // 太细的线在小牌上会断续，至少画 0.35 点
            context.stroke(path, with: .color(color.opacity(alpha)),
                           style: StrokeStyle(lineWidth: CGFloat(max(lineWidth, 0.35)), lineCap: .round, lineJoin: .round))
        }
    }
}

// MARK: - 视图

/// 一张牌的星空线稿（不含底色、外框和牌名），按 100 × 160 等比放进自己的尺寸里。
/// 纯静态的 Canvas，分享卡的 ImageRenderer 也能直接渲染
struct TarotArtView: View {
    let cardID: String
    @Environment(\.palette) private var palette

    var body: some View {
        let colors = TarotArtPalette(palette)
        let shapes = TarotArtLibrary.shared.shapes(for: cardID)
        Canvas { context, size in
            let transform = TarotArtTransform.fitting(size)
            TarotArtRenderer.draw(shapes, in: context, transform: transform, unit: transform.scale, colors: colors)
        }
        .accessibilityHidden(true)
    }
}

extension TarotCard {
    /// 牌面底色：主题的深空色；小阿卡纳再透出一点花色（花色身份色先向强调色靠）
    func faceColors(_ palette: ThemePalette) -> [Color] {
        let p = palette.tarotCardPalette
        var top = p.backgroundBottom.mix(with: p.accent, by: 0.12)
        var bottom = p.backgroundTop.mix(with: .black, by: 0.2)
        if let suit = suitKind {
            let tint = p.identity(suit.colors)
            top = top.mix(with: tint[0], by: 0.3)
            bottom = bottom.mix(with: tint[1], by: 0.35)
        }
        return [top, bottom]
    }

    /// 牌面外发光
    func glowColor(_ palette: ThemePalette) -> Color {
        let p = palette.tarotCardPalette
        return suitKind.map { p.identity($0.colors[0]) } ?? p.accent
    }

    /// 压在深色牌面 / 分享卡上的点缀文字色：小阿卡纳用花色点缀色，大阿卡纳用金色
    func tint(_ palette: ThemePalette) -> Color {
        let p = palette.tarotCardPalette
        return suitKind.map { p.identity($0.accent) } ?? TarotArtPalette.gold
    }
}

/// 静态的牌面：主题深空底色、金框、牌号、星空线稿、牌名。塔罗页在外面加镭射光泽和视差，分享卡直接用它。
/// 牌面是一张画，字和线都按牌宽缩放，不随系统字号变化
struct TarotCardArt: View {
    let card: TarotCard
    @Environment(\.palette) private var palette

    var body: some View {
        let colors = TarotArtPalette(palette)
        let background = card.faceColors(palette)
        ZStack {
            Canvas { context, size in
                Self.drawFrame(card: card, background: background, colors: colors, in: context, size: size)
            }
            TarotArtView(cardID: String(card.id))
        }
        .accessibilityHidden(true)
    }

    private static let hasSongti = UIFont(name: "STSongti-SC-Bold", size: 12) != nil

    /// 固定字号的宋体（牌面上的字不跟随动态字体）
    private static func serif(_ size: CGFloat, bold: Bool) -> Font {
        if hasSongti { return .custom(bold ? "STSongti-SC-Bold" : "STSongti-SC-Regular", fixedSize: size) }
        return .system(size: size, weight: bold ? .bold : .regular, design: .serif)
    }

    private static func drawFrame(card: TarotCard, background: [Color], colors: TarotArtPalette, in context: GraphicsContext, size: CGSize) {
        let t = TarotArtTransform.fitting(size)
        let u = CGFloat(t.scale)
        let bounds = CGRect(origin: .zero, size: size)
        let outline = Path(roundedRect: bounds, cornerRadius: size.width * 0.09)
        context.fill(outline, with: .linearGradient(Gradient(colors: background), startPoint: .zero, endPoint: CGPoint(x: 0, y: size.height)))
        // 线稿后面的一团柔光
        context.fill(outline, with: .radialGradient(Gradient(colors: [colors.accent.opacity(0.2), colors.accent.opacity(0)]),
                                                    center: t.map(50, 72), startRadius: 0, endRadius: 52 * u))
        // 星尘
        var rng = SeededGenerator("tarot-face", card.id)
        for _ in 0..<16 {
            let point = t.map(rng.unit() * 100, rng.unit() * 160)
            let r = CGFloat(rng.double(0.3...0.8)) * u
            context.fill(Path(ellipseIn: CGRect(x: point.x - r, y: point.y - r, width: r * 2, height: r * 2)),
                         with: .color(colors.star.opacity(rng.double(0.15...0.45))))
        }
        // 金框
        let outer = CGRect(origin: t.map(5, 5), size: CGSize(width: 90 * u, height: 150 * u))
        context.stroke(Path(roundedRect: outer, cornerRadius: 7 * u), with: .color(colors.gold), lineWidth: max(1.2 * u, 0.8))
        let inner = CGRect(origin: t.map(7.4, 7.4), size: CGSize(width: 85.2 * u, height: 145.2 * u))
        context.stroke(Path(roundedRect: inner, cornerRadius: 5.2 * u), with: .color(colors.gold.opacity(0.4)), lineWidth: max(0.4 * u, 0.35))
        // 牌名上方的分隔饰线
        for (x0, x1) in [(26.0, 45.0), (55.0, 74.0)] {
            var rule = Path()
            rule.move(to: t.map(x0, 126.5))
            rule.addLine(to: t.map(x1, 126.5))
            context.stroke(rule, with: .color(colors.gold.opacity(0.5)), lineWidth: max(0.5 * u, 0.35))
        }
        var sparkle = Path()
        sparkle.addLines(TarotArtGeometry.starPoints(x: 50, y: 126.5, r: 1.8, n: 4, k: 0.3, rot: -90).map { t.map($0) })
        sparkle.closeSubpath()
        context.fill(sparkle, with: .color(colors.gold))

        // 牌号：大阿卡纳写罗马数字，小阿卡纳写 A、2…10、侍从…，两边各一个花色小符号
        if let suit = card.suitKind {
            drawText(Text(card.rankText).font(serif(9 * u, bold: true)).foregroundStyle(colors.gold),
                     at: t.map(50, 16), maxWidth: 40 * u, in: context)
            let marks = [19.0, 81.0].map { TarotArtShape.suit(suit.rawValue, x: $0, y: 16, r: 4.2, color: "gold") }
            TarotArtRenderer.draw(marks, in: context, transform: t, unit: t.scale, colors: colors)
        } else {
            drawText(Text(card.roman).font(.system(size: 10 * u, weight: .semibold, design: .serif)).foregroundStyle(colors.gold),
                     at: t.map(50, 16), maxWidth: 60 * u, in: context)
        }
        drawText(Text(card.name).font(serif(12.5 * u, bold: true)).foregroundStyle(colors.ink),
                 at: t.map(50, 136.5), maxWidth: 80 * u, in: context)
        drawText(Text(card.en.uppercased()).font(.system(size: 5 * u, weight: .medium, design: .serif)).tracking(0.6 * u)
                    .foregroundStyle(colors.gold.opacity(0.85)),
                 at: t.map(50, 146.5), maxWidth: 82 * u, in: context)
    }

    /// 居中画一行字，放不下时整体缩小
    private static func drawText(_ text: Text, at point: CGPoint, maxWidth: CGFloat, in context: GraphicsContext) {
        let resolved = context.resolve(text)
        let measured = resolved.measure(in: CGSize(width: CGFloat.greatestFiniteMagnitude, height: .greatestFiniteMagnitude))
        guard measured.width > maxWidth, measured.width > 0 else {
            context.draw(resolved, at: point, anchor: .center)
            return
        }
        var layer = context
        let factor = maxWidth / measured.width
        layer.translateBy(x: point.x, y: point.y)
        layer.scaleBy(x: factor, y: factor)
        layer.draw(resolved, at: .zero, anchor: .center)
    }
}
