import SwiftUI

/// 收藏的工具横排一行（问一问顶部）。没有收藏时不占位置
struct FavoriteToolsRow: View {
    @Environment(AppSettings.self) private var settings

    var body: some View {
        let favorites = settings.favoriteTools.compactMap(FavoriteTool.init(id:))
        if !favorites.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                SectionHeader(title: "我的收藏", symbol: "star.fill")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(favorites) { tool in
                            NavigationLink(value: tool.route) {
                                HStack(spacing: 10) {
                                    ToolIcon(symbol: tool.symbol, colors: tool.colors, size: 36)
                                    Text(tool.name).font(.subheadline.weight(.semibold)).foregroundStyle(.primary)
                                }
                                .padding(.leading, 8)
                                .padding(.trailing, 14)
                                .padding(.vertical, 8)
                                .glassEffect(.regular.interactive(), in: .capsule)
                            }
                            .buttonStyle(.pressableCard)
                            .contextMenu { FavoriteMenuButton(id: tool.id, settings: settings) }
                        }
                    }
                    .padding(.vertical, 2)
                    .padding(.horizontal, 16)
                }
                .pageBleed()
            }
        }
    }
}

/// 全部工具，按「帮我决定 / 随机一下 / 聚会时光」分区排成格子，长按可收藏到首页
struct ToolCategorySections: View {
    @Environment(AppSettings.self) private var settings
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
        .columnCount($columnCount, minimum: 158)
    }

    private func tile(_ tool: ToolKind) -> some View {
        NavigationLink(value: AppRoute.tool(tool)) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    ToolIcon(symbol: tool.symbol, colors: tool.colors, size: 48)
                    Spacer()
                    if settings.isFavorite(tool.rawValue) {
                        Image(systemName: "star.fill").font(.caption).foregroundStyle(.yellow)
                            .accessibilityLabel("已收藏")
                    }
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(tool.name).font(.headline).foregroundStyle(.primary)
                    Text(tool.subtitle).font(.caption).subtleForeground().lineLimit(1)
                }
            }
            .glassCard(padding: 16, tint: tool.colors[0].opacity(0.08), interactive: true)
            .overlay(alignment: .topTrailing) {
                Circle().fill(tool.colors[1].opacity(0.28)).frame(width: 80).blur(radius: 30).offset(x: 10, y: -10).allowsHitTesting(false)
            }
            .clipShape(.rect(cornerRadius: 28))
        }
        .buttonStyle(.pressableCard)
        .contextMenu { FavoriteMenuButton(id: tool.rawValue, settings: settings) }
    }

    private func wideTile(_ tool: ToolKind) -> some View {
        NavigationLink(value: AppRoute.tool(tool)) {
            HStack(spacing: 14) {
                ToolIcon(symbol: tool.symbol, colors: tool.colors, size: 48)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(tool.name).font(.headline).foregroundStyle(.primary)
                        if settings.isFavorite(tool.rawValue) {
                            Image(systemName: "star.fill").font(.caption2).foregroundStyle(.yellow)
                                .accessibilityLabel("已收藏")
                        }
                    }
                    Text(tool.subtitle).font(.caption).subtleForeground()
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
            }
            .glassCard(padding: 16, tint: tool.colors[0].opacity(0.08), interactive: true)
            .overlay(alignment: .trailing) {
                Circle().fill(tool.colors[1].opacity(0.25)).frame(width: 100).blur(radius: 34).offset(x: 20).allowsHitTesting(false)
            }
            .clipShape(.rect(cornerRadius: 28))
        }
        .buttonStyle(.pressableCard)
        .contextMenu { FavoriteMenuButton(id: tool.rawValue, settings: settings) }
    }
}

/// 长按菜单里的「收藏到首页 / 取消收藏」
struct FavoriteMenuButton: View {
    let id: String
    let settings: AppSettings

    var body: some View {
        let isFavorite = settings.isFavorite(id)
        Button(isFavorite ? "取消收藏" : "收藏到首页", systemImage: isFavorite ? "star.slash" : "star") {
            settings.toggleFavorite(id)
            Haptics.success()
        }
    }
}
