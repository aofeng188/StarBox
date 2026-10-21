import SwiftUI

/// 收藏的工具横排一行（问一问顶部）。没有收藏时不占位置。
/// 一排玻璃胶囊放在同一个 GlassEffectContainer 里：收藏、取消收藏时胶囊像水滴一样从旁边长出来、融回去
struct FavoriteToolsRow: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette
    @Namespace private var glassNamespace

    var body: some View {
        let favorites = settings.favoriteTools.compactMap(FavoriteTool.init(id:))
        if !favorites.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                SectionHeader(title: "我的收藏", symbol: "star.fill")
                ScrollView(.horizontal, showsIndicators: false) {
                    // 容器间距小于胶囊间距：平时各自独立，增删时才融合变形
                    GlassEffectContainer(spacing: 8) {
                        HStack(spacing: 12) {
                            // 缩放转场的来源标在下面的工具格子和占卜卡片上，这里不重复标
                            ForEach(favorites) { tool in
                                NavigationLink(value: tool.route) {
                                    HStack(spacing: 10) {
                                        ToolIcon(symbol: tool.symbol, colors: palette.identity(tool.colors), size: 36)
                                        Text(tool.name).font(.subheadline.weight(.semibold)).foregroundStyle(.primary)
                                    }
                                    .padding(.leading, 8)
                                    .padding(.trailing, 14)
                                    .padding(.vertical, 8)
                                    .glassEffect(.regular.interactive(), in: .capsule)
                                    .glassEffectID(tool.id, in: glassNamespace)
                                }
                                .buttonStyle(.pressableCard)
                                .contextMenu { FavoriteMenuButton(id: tool.id, settings: settings) }
                            }
                        }
                        .padding(.vertical, 2)
                        .padding(.horizontal, 16)
                    }
                }
                .fadingScrollEdges()
                .pageBleed()
            }
        }
    }
}

/// 全部工具，按「帮我决定 / 随机一下 / 聚会时光」分区排成格子，长按可收藏到首页
struct ToolCategorySections: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var columnCount = 2

    private var columns: [GridItem] {
        Array(repeating: GridItem(.flexible(), spacing: 14), count: columnCount)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            ForEach(ToolCategory.allCases) { category in
                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: category.name, symbol: category.symbol)
                    let tools = ToolKind.tools(in: category)
                    // 两栏且单数时最后一个做成通栏；其他列数直接排进网格
                    let hasWideTile = columnCount == 2 && !tools.count.isMultiple(of: 2)
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(hasWideTile ? Array(tools.dropLast()) : tools) { tile($0) }
                    }
                    if hasWideTile, let last = tools.last {
                        wideTile(last)
                    }
                }
            }
        }
        // 超大字号下格子要更宽（手机上排成一栏），名字和副标题才不会被截断
        .columnCount($columnCount, minimum: typeSize.isAccessibilitySize ? 158 * 1.6 : 158)
    }

    private func tile(_ tool: ToolKind) -> some View {
        let colors = palette.identity(tool.colors)
        return NavigationLink(value: AppRoute.tool(tool)) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    ToolIcon(symbol: tool.symbol, colors: colors, size: 48)
                    Spacer()
                    if settings.isFavorite(tool.rawValue) {
                        Image(systemName: "star.fill").font(.caption).foregroundStyle(Color.gold(scheme))
                            .accessibilityLabel("已收藏")
                    }
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(tool.name).font(.headline).foregroundStyle(.primary)
                    Text(tool.subtitle).font(.caption).foregroundStyle(subtitleStyle(colors[0]))
                        .lineLimit(typeSize.isAccessibilitySize ? 2 : 1)
                }
            }
            .glassCard(padding: 16, tint: colors[0].opacity(0.08), interactive: true)
            .overlay(alignment: .topTrailing) {
                Circle().fill(colors[1].opacity(0.28)).frame(width: 80).blur(radius: 30).offset(x: 10, y: -10).allowsHitTesting(false)
            }
            .clipShape(.rect(cornerRadius: 28))
            .zoomSource(.tool(tool))
        }
        .buttonStyle(.pressableCard)
        .contextMenu { FavoriteMenuButton(id: tool.rawValue, settings: settings) }
    }

    private func wideTile(_ tool: ToolKind) -> some View {
        let colors = palette.identity(tool.colors)
        return NavigationLink(value: AppRoute.tool(tool)) {
            HStack(spacing: 14) {
                ToolIcon(symbol: tool.symbol, colors: colors, size: 48)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(tool.name).font(.headline).foregroundStyle(.primary)
                        if settings.isFavorite(tool.rawValue) {
                            Image(systemName: "star.fill").font(.caption2).foregroundStyle(Color.gold(scheme))
                                .accessibilityLabel("已收藏")
                        }
                    }
                    Text(tool.subtitle).font(.caption).foregroundStyle(subtitleStyle(colors[0]))
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
            }
            .glassCard(padding: 16, tint: colors[0].opacity(0.08), interactive: true)
            .overlay(alignment: .trailing) {
                Circle().fill(colors[1].opacity(0.25)).frame(width: 100).blur(radius: 34).offset(x: 20).allowsHitTesting(false)
            }
            .clipShape(.rect(cornerRadius: 28))
            .zoomSource(.tool(tool))
        }
        .buttonStyle(.pressableCard)
        .contextMenu { FavoriteMenuButton(id: tool.rawValue, settings: settings) }
    }

    /// 格子副标题：浅色模式下用工具色的深色版（比格子的浅色底深两档），深色模式照旧用次要文字色
    private func subtitleStyle(_ color: Color) -> AnyShapeStyle {
        palette.isDark ? AnyShapeStyle(HierarchicalShapeStyle.secondary) : AnyShapeStyle(color.mix(with: .black, by: 0.5))
    }
}

/// 长按菜单里的「收藏到首页 / 取消收藏」
struct FavoriteMenuButton: View {
    let id: String
    let settings: AppSettings
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        let isFavorite = settings.isFavorite(id)
        Button(isFavorite ? "取消收藏" : "收藏到首页", systemImage: isFavorite ? "star.slash" : "star") {
            // 收藏行的玻璃胶囊跟着融合变形；减弱动态效果时直接切换
            withAnimation(reduceMotion ? nil : .bouncy) { settings.toggleFavorite(id) }
            Haptics.success()
        }
    }
}
