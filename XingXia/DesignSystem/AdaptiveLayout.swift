import SwiftUI

// 自适应排版：iPhone Duo 合上时外屏又窄又矮（内容区约 382 × 562pt），展开后是
// regular × regular 的宽屏（约 951 × 669pt，还能转成竖持、和别的 App 分屏）。
// 所以页面一律按「当前可用空间」排版，不判断机型、方向或 idiom。

enum PageMetrics {
    /// 单栏内容的最大宽度，再宽就居中留白
    static let readableWidth: CGFloat = 640
    /// 网格类页面（百宝箱、占卜首页等）的最大宽度
    static let gridWidth: CGFloat = 1000
    /// 内容宽度达到这个值，信息流改成两栏、「舞台」页改成左右分栏
    static let wideWidth: CGFloat = 680

    /// 每列至少 `minimum` 宽时能排下的列数
    static func columns(for width: CGFloat, minimum: CGFloat, spacing: CGFloat = 14, max limit: Int = 4) -> Int {
        min(limit, max(1, Int((width + spacing) / (minimum + spacing))))
    }
}

extension View {
    /// 滚动页的根容器：左右留边；宽屏下把内容限制在易读宽度并居中，而不是把卡片拉得很长。
    func pageContent(maxWidth: CGFloat = PageMetrics.readableWidth, padding: CGFloat = 16) -> some View {
        self
            .padding(.horizontal, padding)
            .frame(maxWidth: maxWidth)
            .frame(maxWidth: .infinity)
    }

    /// 页面里的横向滚动条：抵消页边距，让内容一直滑到安全区边缘再裁剪。
    /// 必须裁在安全区边缘：iPhone Duo 的状态栏和工具栏竖排在右侧，不裁的话标签会滑到它们底下。
    func pageBleed(_ inset: CGFloat = 16) -> some View {
        padding(.horizontal, -inset).clipShape(Rectangle().inset(by: -inset))
    }

    /// 把容器当前宽度能排下的列数写进 `count`（折叠 / 展开 / 分屏时会跟着变）
    func columnCount(_ count: Binding<Int>, minimum: CGFloat, spacing: CGFloat = 14, max limit: Int = 4) -> some View {
        onGeometryChange(for: Int.self) { proxy in
            PageMetrics.columns(for: proxy.size.width, minimum: minimum, spacing: spacing, max: limit)
        } action: { count.wrappedValue = $0 }
    }

    /// iPhone Duo 半折叠（书本姿态）时，折痕在本视图坐标系里占的横向范围；平铺或没有折痕时为 nil。
    /// 两栏布局拿它当栏间距，内容就不会压在折痕上。
    func foldGap(_ gap: Binding<ClosedRange<CGFloat>?>) -> some View {
        onGeometryChange(for: ClosedRange<CGFloat>?.self) { proxy in
            guard #available(iOS 27.1, *) else { return nil }
            guard let fold = proxy.reservedRegions(kind: .division).first(where: \.isActive)?.frame,
                  fold.height > fold.width, fold.minX > 0, fold.maxX < proxy.size.width else { return nil }
            return fold.minX...fold.maxX
        } action: { gap.wrappedValue = $0 }
    }

    /// 当前容器是否宽到可以分两栏
    func isWideLayout(_ isWide: Binding<Bool>) -> some View {
        onGeometryChange(for: Bool.self) { $0.size.width >= PageMetrics.wideWidth } action: { isWide.wrappedValue = $0 }
    }
}

// MARK: - 等比缩小

/// 按设计尺寸排版，空间不够时整体等比缩小（不会放大）。
/// 用于硬币、签筒这类按固定尺寸画的「舞台」，让它在矮屏上让出地方给按钮。
struct ScaleToFit<Content: View>: View {
    let design: CGSize
    var minScale: CGFloat = 0.5
    @ViewBuilder var content: Content

    var body: some View {
        GeometryReader { proxy in
            let scale = min(1, proxy.size.width / design.width, proxy.size.height / design.height)
            content
                .frame(width: design.width, height: design.height)
                .scaleEffect(scale)
                .frame(width: proxy.size.width, height: proxy.size.height)
        }
        .frame(minWidth: design.width * minScale, idealWidth: design.width, maxWidth: design.width,
               minHeight: design.height * minScale, idealHeight: design.height, maxHeight: design.height)
    }
}

// MARK: - 舞台页骨架

/// 「顶部选项 + 舞台 + 底部操作」的页面骨架。
/// - 竖长空间：上中下排列，舞台吃掉剩余高度；
/// - 宽而矮的空间（Duo 展开横持）：舞台在左，选项和操作在右；
/// - 实在放不下时整页可以滚动，按钮不会被挤到屏幕外。
///
/// 用自定义 `Layout` 摆放而不是切换 HStack / VStack，这样折叠、展开时舞台里的动画和 3D 场景不会被重建。
struct StageScaffold<Top: View, Stage: View, Bottom: View>: View {
    var spacing: CGFloat = 18
    /// 上下排列时页面的最小高度，低于它就滚动
    var minHeight: CGFloat = 470
    @ViewBuilder var top: Top
    @ViewBuilder var stage: Stage
    @ViewBuilder var bottom: Bottom

    @State private var fold: ClosedRange<CGFloat>?

    var body: some View {
        GeometryReader { proxy in
            let wide = StageLayout.isWide(proxy.size)
            let required: CGFloat = wide ? 340 : minHeight
            ScrollView {
                StageLayout(spacing: spacing, fold: fold) {
                    VStack(spacing: spacing) { top }.frame(maxWidth: .infinity)
                    stage.frame(maxWidth: .infinity, maxHeight: .infinity)
                    VStack(spacing: spacing) { bottom }.frame(maxWidth: .infinity)
                }
                .foldGap($fold)
                .padding(.horizontal, 16)
                .frame(maxWidth: wide ? PageMetrics.gridWidth : PageMetrics.readableWidth)
                .frame(maxWidth: .infinity)
                .frame(height: max(proxy.size.height, required))
            }
            // 放得下时完全不滚动，免得和舞台上的拖拽、长按手势抢触摸
            .scrollDisabled(proxy.size.height >= required)
        }
    }
}

struct StageLayout: Layout {
    var spacing: CGFloat = 18
    var sideWidth: CGFloat = 340
    var gap: CGFloat = 28
    /// 半折叠时折痕的横向范围：舞台放折痕左边，操作区放右边
    var fold: ClosedRange<CGFloat>?

    static func isWide(_ size: CGSize) -> Bool {
        size.width >= PageMetrics.wideWidth && size.width > size.height
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        proposal.replacingUnspecifiedDimensions(by: CGSize(width: 390, height: 600))
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        guard subviews.count == 3 else { return }
        let top = subviews[0], stage = subviews[1], bottom = subviews[2]

        if Self.isWide(bounds.size) {
            var side = min(sideWidth, bounds.width * 0.42)
            var stageWidth = bounds.width - side - gap
            var sideX = bounds.minX + stageWidth + gap
            if let fold, fold.lowerBound > 200, bounds.width - fold.upperBound > 200 {
                stageWidth = fold.lowerBound
                side = min(sideWidth, bounds.width - fold.upperBound)
                sideX = bounds.minX + fold.upperBound + (bounds.width - fold.upperBound - side) / 2
            }
            stage.place(at: bounds.origin, proposal: ProposedViewSize(width: stageWidth, height: bounds.height))

            let column = ProposedViewSize(width: side, height: nil)
            let topHeight = top.sizeThatFits(column).height
            let bottomHeight = bottom.sizeThatFits(column).height
            let between = topHeight > 0 ? spacing : 0
            let x = sideX
            // 右栏整体垂直居中
            let y = bounds.minY + max(0, (bounds.height - topHeight - between - bottomHeight) / 2)
            top.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(width: side, height: topHeight))
            bottom.place(at: CGPoint(x: x, y: y + topHeight + between), proposal: ProposedViewSize(width: side, height: bottomHeight))
        } else {
            let column = ProposedViewSize(width: bounds.width, height: nil)
            let topHeight = top.sizeThatFits(column).height
            let bottomHeight = bottom.sizeThatFits(column).height
            let above = topHeight > 0 ? spacing : 0
            let stageHeight = max(0, bounds.height - topHeight - above - spacing - bottomHeight)
            top.place(at: bounds.origin, proposal: ProposedViewSize(width: bounds.width, height: topHeight))
            stage.place(at: CGPoint(x: bounds.minX, y: bounds.minY + topHeight + above),
                        proposal: ProposedViewSize(width: bounds.width, height: stageHeight))
            bottom.place(at: CGPoint(x: bounds.minX, y: bounds.maxY - bottomHeight),
                         proposal: ProposedViewSize(width: bounds.width, height: bottomHeight))
        }
    }
}

// MARK: - 多栏信息流

/// 把子视图按顺序排进若干等宽的栏，每次放进当前最矮的一栏；只有一栏时就是普通的纵向排列。
/// 栏数变化时子视图不会被重建，卡片里的状态和入场动画得以保留。
struct ColumnsLayout: Layout {
    var columns = 1
    var spacing: CGFloat = 16
    /// 半折叠时折痕的横向范围：两栏时拿它当栏间距
    var fold: ClosedRange<CGFloat>?

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 390
        return CGSize(width: width, height: arrange(width: width, subviews: subviews).height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = arrange(width: bounds.width, subviews: subviews)
        for (subview, frame) in zip(subviews, result.frames) {
            subview.place(at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                          proposal: ProposedViewSize(width: frame.width, height: frame.height))
        }
    }

    private func arrange(width: CGFloat, subviews: Subviews) -> (frames: [CGRect], height: CGFloat) {
        let count = max(1, columns)
        let columnWidth = max(0, (width - spacing * CGFloat(count - 1)) / CGFloat(count))
        var origins = (0..<count).map { CGFloat($0) * (columnWidth + spacing) }
        var widths = [CGFloat](repeating: columnWidth, count: count)
        if count == 2, let fold, fold.lowerBound > 200, width - fold.upperBound > 200 {
            origins = [0, fold.upperBound]
            widths = [fold.lowerBound, width - fold.upperBound]
        }
        var heights = [CGFloat](repeating: 0, count: count)
        var frames: [CGRect] = []
        for subview in subviews {
            let column = heights.indices.min { heights[$0] < heights[$1] } ?? 0
            let height = subview.sizeThatFits(ProposedViewSize(width: widths[column], height: nil)).height
            // 没有内容的卡片（比如还没有倒数日）不占间距
            let y = heights[column] + (heights[column] > 0 && height > 0 ? spacing : 0)
            frames.append(CGRect(x: origins[column], y: y, width: widths[column], height: height))
            heights[column] = y + height
        }
        return (frames, heights.max() ?? 0)
    }
}
