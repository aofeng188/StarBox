import SwiftUI
import Photos

// MARK: - 分享卡：比例与样式

/// 分享卡比例。点尺寸固定，导出时按 3 倍出图（1080 宽）
enum ShareCardRatio: String, CaseIterable, Identifiable {
    /// 朋友圈、小红书
    case portrait = "3:4"
    /// 故事、壁纸
    case story = "9:16"

    var id: String { rawValue }

    var size: CGSize {
        switch self {
        case .portrait: CGSize(width: 360, height: 480)
        case .story: CGSize(width: 360, height: 640)
        }
    }

    var name: String {
        switch self {
        case .portrait: "3:4 朋友圈"
        case .story: "9:16 故事"
        }
    }
}

struct ShareCardStyle: Equatable {
    let ratio: ShareCardRatio
    let theme: AppTheme
    /// 分享卡一律用主题的深色版本，文字用白色系
    var palette: ThemePalette { theme.dark }
    var size: CGSize { ratio.size }
    /// 9:16 更高，内容可以排得舒展些
    var isTall: Bool { ratio == .story }
}

// MARK: - 卡片外框

/// 分享卡外框：主题深色渐变 + 星云色块 + 固定星点的背景，顶部标题，底部「公历日期 · 农历 · 星匣」页脚，内容放在中间。
/// 尺寸固定、字号不随系统字号变化，导出的图片每次都一样。
struct ShareCardFrame<Content: View>: View {
    let style: ShareCardStyle
    let day: DayKey
    let title: String
    let content: Content

    init(style: ShareCardStyle, day: DayKey = .today, title: String, @ViewBuilder content: () -> Content) {
        self.style = style
        self.day = day
        self.title = title
        self.content = content()
    }

    var body: some View {
        let palette = style.palette
        VStack(spacing: 0) {
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .tracking(3)
                .foregroundStyle(.white.opacity(0.78))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
                .accessibilityAddTraits(.isHeader)
            Spacer(minLength: 10)
            content
                .frame(maxWidth: .infinity)
            Spacer(minLength: 10)
            ShareCardFooter(day: day, palette: palette)
        }
        .padding(.horizontal, 24)
        .padding(.top, 26)
        .padding(.bottom, 20)
        .frame(width: style.size.width, height: style.size.height)
        .background { ShareCardBackground(palette: palette) }
        .environment(\.palette, palette)
        .environment(\.colorScheme, .dark)
        .dynamicTypeSize(.large)
    }
}

private struct ShareCardFooter: View {
    let day: DayKey
    let palette: ThemePalette

    var body: some View {
        let lunar = LunarDate(day)
        VStack(spacing: 10) {
            Rectangle()
                .fill(.white.opacity(0.18))
                .frame(height: 0.5)
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(format: "%d.%02d.%02d", day.year, day.month, day.day) + " · " + DateText.weekday(day))
                    Text("农历\(lunar.yearGanZhi.name)年 \(lunar.text)")
                }
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(.white.opacity(0.72))
                Spacer(minLength: 8)
                HStack(spacing: 5) {
                    Image(systemName: "sparkles")
                        .font(.system(size: 12, weight: .semibold))
                    Text("星匣")
                        .font(AppFont.serif(17, weight: .bold))
                }
                .foregroundStyle(palette.accentGradient)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("星匣")
            }
        }
    }
}

/// 背景：主题深色渐变 + 星云色块 + 固定种子的星点，不随时间变化
private struct ShareCardBackground: View {
    let palette: ThemePalette

    var body: some View {
        ZStack {
            palette.backgroundGradient
            Canvas { context, size in
                let w = size.width
                let h = size.height
                // 星云色块：(颜色序号, 圆心, 半径, 不透明度)
                let blobs: [(Int, CGPoint, CGFloat, Double)] = [
                    (0, CGPoint(x: w * 0.12, y: h * 0.1), w * 0.8, 0.55),
                    (3, CGPoint(x: w * 0.92, y: h * 0.06), w * 0.45, 0.32),
                    (1, CGPoint(x: w * 0.95, y: h * 0.5), w * 0.65, 0.38),
                    (2, CGPoint(x: w * 0.1, y: h * 0.95), w * 0.8, 0.45),
                ]
                for (index, center, radius, alpha) in blobs {
                    let color = palette.nebula[index % palette.nebula.count]
                    let rect = CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)
                    context.fill(Path(ellipseIn: rect),
                                 with: .radialGradient(Gradient(colors: [color.opacity(alpha), color.opacity(alpha * 0.3), .clear]),
                                                       center: center, startRadius: 0, endRadius: radius))
                }
                for star in ShareCardStars.all {
                    let point = CGPoint(x: star.x * w, y: star.y * h)
                    let color = star.tint >= 0 ? palette.nebula[star.tint % palette.nebula.count] : palette.star
                    let r = star.radius
                    if r > 1.3 {
                        let glow = CGRect(x: point.x - r * 4, y: point.y - r * 4, width: r * 8, height: r * 8)
                        context.fill(Path(ellipseIn: glow),
                                     with: .radialGradient(Gradient(colors: [color.opacity(0.35 * star.brightness), .clear]),
                                                           center: point, startRadius: 0, endRadius: r * 4))
                    }
                    context.fill(Path(ellipseIn: CGRect(x: point.x - r, y: point.y - r, width: r * 2, height: r * 2)),
                                 with: .color(color.opacity(star.brightness)))
                    if star.sparkle {
                        let len = r * 4.5
                        var cross = Path()
                        cross.move(to: CGPoint(x: point.x - len, y: point.y))
                        cross.addLine(to: CGPoint(x: point.x + len, y: point.y))
                        cross.move(to: CGPoint(x: point.x, y: point.y - len))
                        cross.addLine(to: CGPoint(x: point.x, y: point.y + len))
                        context.stroke(cross, with: .color(color.opacity(0.45 * star.brightness)), lineWidth: 0.6)
                    }
                }
            }
            // 底部压暗一点，页脚文字更清楚
            LinearGradient(colors: [.clear, .black.opacity(0.22)], startPoint: .center, endPoint: .bottom)
        }
        .accessibilityHidden(true)
    }
}

/// 分享卡的星点：固定种子生成，每次导出都一样
private enum ShareCardStars {
    struct Star {
        let x: Double
        let y: Double
        let radius: Double
        let brightness: Double
        let tint: Int
        let sparkle: Bool
    }

    static let all: [Star] = {
        var rng = SeededGenerator(seed: 20_261_016)
        return (0..<90).map { _ in
            let big = rng.chance(0.1)
            return Star(x: rng.unit(), y: rng.unit(),
                        radius: big ? rng.double(1.4...2.2) : rng.double(0.4...1.1),
                        brightness: rng.double(0.35...0.95),
                        tint: rng.chance(0.25) ? rng.int(below: 4) : -1,
                        sparkle: big && rng.chance(0.6))
        }
    }()
}

// MARK: - 卡片内常用的静态元素

/// 分享卡里的分数环。ImageRenderer 不跑出现动画，ScoreRing 会画成 0，所以这里画成静态的
struct ShareScoreRing: View {
    let score: Int
    var size: CGFloat = 120
    var lineWidth: CGFloat = 11
    var label: String?

    @Environment(\.palette) private var palette

    var body: some View {
        let progress = Double(min(max(score, 0), 100)) / 100
        ZStack {
            Circle()
                .stroke(.white.opacity(0.12), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(AngularGradient(colors: [palette.accent, palette.accentSecondary, palette.nebula[0], palette.accent],
                                        center: .center),
                        style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .shadow(color: palette.accent.opacity(0.6), radius: lineWidth * 0.8)
            VStack(spacing: 0) {
                Text("\(score)")
                    .font(.system(size: size * 0.32, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(.white)
                if let label {
                    Text(label)
                        .font(.system(size: size * 0.11, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.65))
                }
            }
        }
        .frame(width: size, height: size)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label ?? "分数") \(score) 分")
    }
}

/// 分享卡里的静态进度条
struct ShareMeter: View {
    let value: Int
    var height: CGFloat = 4

    @Environment(\.palette) private var palette

    var body: some View {
        let fraction = CGFloat(min(max(value, 0), 100)) / 100
        Capsule()
            .fill(.white.opacity(0.12))
            .overlay(alignment: .leading) {
                GeometryReader { proxy in
                    Capsule()
                        .fill(palette.accentGradient)
                        .frame(width: proxy.size.width * fraction)
                }
            }
            .frame(height: height)
            .accessibilityHidden(true)
    }
}

/// 分享卡里的小标签（白字半透明底），可带一个色点
struct ShareChip: View {
    let text: String
    var dot: Color?

    var body: some View {
        HStack(spacing: 5) {
            if let dot {
                Circle()
                    .fill(dot)
                    .frame(width: 10, height: 10)
                    .overlay(Circle().strokeBorder(.white.opacity(0.4), lineWidth: 0.5))
            }
            Text(text)
                .font(.system(size: 12, weight: .medium))
                .lineLimit(1)
                .minimumScaleFactor(0.75)
        }
        .foregroundStyle(.white.opacity(0.9))
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(.white.opacity(0.1), in: .capsule)
    }
}

// MARK: - 导出

@MainActor
enum ShareCardRenderer {
    /// 给卡片补上导出需要的环境：ImageRenderer 不带页面的环境值，主题、深色、App 级对象都要显式给
    static func prepare<Card: View>(_ card: Card, style: ShareCardStyle) -> some View {
        card
            .frame(width: style.size.width, height: style.size.height)
            .environment(\.palette, style.palette)
            .environment(\.colorScheme, .dark)
            .environment(AppState.shared)
            .environment(AppSettings.shared)
            .environment(ThemeManager.shared)
    }

    /// 以 3 倍导出（360 点宽 → 1080 像素）。失败返回 nil
    static func image<Card: View>(of card: Card, style: ShareCardStyle) -> UIImage? {
        let renderer = ImageRenderer(content: prepare(card, style: style))
        renderer.proposedSize = ProposedViewSize(style.size)
        renderer.scale = 3
        renderer.isOpaque = true
        return renderer.uiImage
    }
}

/// 写入相册。不放在 MainActor 上：Photos 在自己的后台队列里执行 changeBlock
enum PhotoLibrarySaver {
    static func add(pngData data: Data) async throws {
        try await PHPhotoLibrary.shared().performChanges {
            PHAssetCreationRequest.forAsset().addResource(with: .photo, data: data, options: nil)
        }
    }
}

// MARK: - 分享按钮与预览

/// 分享按钮：点开预览，可切换 3:4 / 9:16，用系统分享面板分享图片，或存到相册。
/// 主题取 ThemeManager 当前主题的深色版本，卡片内容由 `card` 按传入的样式画出来。
struct ShareCardButton<Card: View>: View {
    let title: String
    let systemImage: String
    /// 系统分享面板顶部显示的标题，不传时用「星匣分享卡」
    let previewTitle: String?
    /// 页面需要知道预览是否弹出时传入（例如摇一摇页面据此暂停 onShake）
    let externalPresented: Binding<Bool>?
    /// 预览打开时默认选中的比例（年度回顾这类长内容默认 9:16）
    let initialRatio: ShareCardRatio
    let card: (ShareCardStyle) -> Card

    @State private var internalPresented = false

    init(title: String, systemImage: String = "square.and.arrow.up", previewTitle: String? = nil,
         isPresented: Binding<Bool>? = nil, initialRatio: ShareCardRatio = .portrait,
         @ViewBuilder card: @escaping (ShareCardStyle) -> Card) {
        self.title = title
        self.systemImage = systemImage
        self.previewTitle = previewTitle
        self.externalPresented = isPresented
        self.initialRatio = initialRatio
        self.card = card
    }

    private var isPresented: Binding<Bool> { externalPresented ?? $internalPresented }

    var body: some View {
        Button {
            Haptics.tap()
            isPresented.wrappedValue = true
        } label: {
            Label(title, systemImage: systemImage)
        }
        .sheet(isPresented: isPresented) {
            ShareCardSheet(title: title, previewTitle: previewTitle ?? "星匣分享卡", initialRatio: initialRatio, card: card)
        }
    }
}

private struct ShareCardSheet<Card: View>: View {
    let title: String
    let previewTitle: String
    let card: (ShareCardStyle) -> Card

    init(title: String, previewTitle: String, initialRatio: ShareCardRatio, card: @escaping (ShareCardStyle) -> Card) {
        self.title = title
        self.previewTitle = previewTitle
        self.card = card
        _ratio = State(initialValue: initialRatio)
    }

    @Environment(\.dismiss) private var dismiss
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var typeSize

    @State private var ratio: ShareCardRatio
    @State private var rendered: RenderedCard?
    @State private var renderFailed = false
    @State private var isSaving = false
    /// 已经存过的那张（换比例或主题后可以再存）
    @State private var savedStyle: ShareCardStyle?
    @State private var saveError: SaveError?
    @State private var containerSize: CGSize = .zero

    private struct RenderedCard {
        let style: ShareCardStyle
        let image: UIImage
    }

    private enum SaveError {
        case denied
        case failed

        var message: String {
            switch self {
            case .denied: "请在系统设置里允许「星匣」添加照片，再试一次。"
            case .failed: "保存时出了点问题，请稍后再试。"
            }
        }
    }

    var body: some View {
        let style = ShareCardStyle(ratio: ratio, theme: ThemeManager.shared.current)
        // 只用和当前比例、主题对应的那张，切换后还没渲染好时按钮先禁用
        let image: UIImage? = rendered?.style == style ? rendered?.image : nil

        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    Picker("比例", selection: $ratio) {
                        ForEach(ShareCardRatio.allCases) { option in
                            Text(option.name).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)

                    preview(style)

                    if renderFailed && image == nil {
                        VStack(spacing: 8) {
                            Text("图片生成失败").font(.subheadline).foregroundStyle(.secondary)
                            Button("重试", systemImage: "arrow.clockwise") { render(style) }
                                .buttonStyle(.glass)
                        }
                    }

                    actions(style: style, image: image)
                }
                .pageContent()
                .padding(.vertical, 12)
            }
            .onGeometryChange(for: CGSize.self) { $0.size } action: { containerSize = $0 }
            .cosmicBackground(dimmed: true)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭", systemImage: "xmark") { dismiss() }
                }
            }
        }
        .task(id: style) { render(style) }
        .alert("没能存到相册", isPresented: Binding(get: { saveError != nil }, set: { if !$0 { saveError = nil } }),
               presenting: saveError) { error in
            if case .denied = error {
                Button("去设置") {
                    if let url = URL(string: UIApplication.openSettingsURLString) { UIApplication.shared.open(url) }
                }
            }
            Button("好", role: .cancel) {}
        } message: { error in
            Text(error.message)
        }
    }

    /// 预览直接放卡片本身（和导出的是同一个视图），按可用空间等比缩小，读屏可以读到卡片里的文字
    private func preview(_ style: ShareCardStyle) -> some View {
        let size = style.size
        let widthBudget = max(200, min(containerSize.width - 32, size.width))
        let heightBudget = max(300, containerSize.height - 240)
        let scale = min(1, widthBudget / size.width, heightBudget / size.height)
        return ShareCardRenderer.prepare(card(style), style: style)
            .clipShape(.rect(cornerRadius: 24))
            .scaleEffect(scale, anchor: .topLeading)
            .frame(width: size.width * scale, height: size.height * scale, alignment: .topLeading)
            .shadow(color: .black.opacity(0.3), radius: 16, y: 8)
            .animation(reduceMotion ? nil : Animation.snappy, value: style.ratio)
            .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private func actions(style: ShareCardStyle, image: UIImage?) -> some View {
        let layout = typeSize.isAccessibilitySize ? AnyLayout(VStackLayout(spacing: 12)) : AnyLayout(HStackLayout(spacing: 12))
        let saved = savedStyle == style
        layout {
            if let image {
                let picture = Image(uiImage: image)
                ShareLink(item: picture, preview: SharePreview(previewTitle, image: picture)) {
                    Label("分享", systemImage: "square.and.arrow.up")
                        .font(.headline)
                        .foregroundStyle(palette.onAccent)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.glassProminent)
                .tint(palette.accent)
                .controlSize(.large)
            } else {
                Button {} label: {
                    HStack(spacing: 8) {
                        if !renderFailed { ProgressView() }
                        Text(renderFailed ? "暂时无法分享" : "生成中")
                    }
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                }
                .buttonStyle(.glass)
                .controlSize(.large)
                .disabled(true)
            }

            Button {
                guard let image else { return }
                Task { await save(image, style: style) }
            } label: {
                Label(saved ? "已存到相册" : "存到相册", systemImage: saved ? "checkmark" : "square.and.arrow.down")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
            }
            .buttonStyle(.glass)
            .controlSize(.large)
            .disabled(image == nil || isSaving || saved)
        }
    }

    private func render(_ style: ShareCardStyle) {
        if let image = ShareCardRenderer.image(of: card(style), style: style) {
            rendered = RenderedCard(style: style, image: image)
            renderFailed = false
        } else {
            rendered = nil
            renderFailed = true
        }
    }

    private func save(_ image: UIImage, style: ShareCardStyle) async {
        guard !isSaving else { return }
        guard let data = image.pngData() else {
            saveError = .failed
            return
        }
        isSaving = true
        defer { isSaving = false }
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else {
            Haptics.warning()
            saveError = .denied
            return
        }
        do {
            try await PhotoLibrarySaver.add(pngData: data)
            Haptics.success()
            savedStyle = style
            Announce.post("已存到相册")
        } catch {
            Haptics.error()
            saveError = .failed
        }
    }
}
