import SwiftUI

struct ToolsHubView: View {
    @Environment(AppSettings.self) private var settings
    @State private var search = ""

    private let columns = [GridItem(.flexible(), spacing: 14), GridItem(.flexible(), spacing: 14)]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                let matches = ToolKind.allCases.filter(matchesSearch)
                if search.isEmpty {
                    let favorites = settings.favoriteTools.compactMap(ToolKind.init(rawValue:))
                    if !favorites.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            SectionHeader(title: "我的收藏", symbol: "star.fill")
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ForEach(favorites) { tool in
                                        NavigationLink(value: AppRoute.tool(tool)) {
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
                                        .contextMenu { favoriteButton(tool) }
                                    }
                                }
                                .padding(.vertical, 2)
                            }
                            .scrollClipDisabled()
                        }
                    }
                    ForEach(ToolCategory.allCases) { category in
                        VStack(alignment: .leading, spacing: 12) {
                            SectionHeader(title: category.name, symbol: category.symbol)
                            let tools = ToolKind.tools(in: category)
                            LazyVGrid(columns: columns, spacing: 14) {
                                ForEach(tools.count.isMultiple(of: 2) ? tools : Array(tools.dropLast())) { tile($0) }
                            }
                            // 单数时最后一个做成通栏
                            if !tools.count.isMultiple(of: 2), let last = tools.last {
                                wideTile(last)
                            }
                        }
                    }
                } else if matches.isEmpty {
                    EmptyHint(symbol: "magnifyingglass", title: "没有找到「\(search)」", message: "试试「吃」「硬币」「抽奖」等关键词")
                } else {
                    LazyVGrid(columns: columns, spacing: 14) {
                        ForEach(matches) { tile($0) }
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("百宝箱")
        .searchable(text: $search, prompt: "搜索工具")
    }

    private func matchesSearch(_ tool: ToolKind) -> Bool {
        guard !search.isEmpty else { return true }
        return tool.name.contains(search) || tool.subtitle.contains(search) || tool.keywords.contains(search)
    }

    private func tile(_ tool: ToolKind) -> some View {
        NavigationLink(value: AppRoute.tool(tool)) {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top) {
                    ToolIcon(symbol: tool.symbol, colors: tool.colors, size: 48)
                    Spacer()
                    if settings.favoriteTools.contains(tool.rawValue) {
                        Image(systemName: "star.fill").font(.caption).foregroundStyle(.yellow)
                            .accessibilityLabel("已收藏")
                    }
                }
                VStack(alignment: .leading, spacing: 3) {
                    Text(tool.name).font(.headline).foregroundStyle(.primary)
                    Text(tool.subtitle).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
            }
            .glassCard(padding: 16, tint: tool.colors[0].opacity(0.08), interactive: true)
            .overlay(alignment: .topTrailing) {
                Circle().fill(tool.colors[1].opacity(0.28)).frame(width: 80).blur(radius: 30).offset(x: 10, y: -10).allowsHitTesting(false)
            }
            .clipShape(.rect(cornerRadius: 28))
        }
        .buttonStyle(.pressableCard)
        .contextMenu { favoriteButton(tool) }
    }

    private func wideTile(_ tool: ToolKind) -> some View {
        NavigationLink(value: AppRoute.tool(tool)) {
            HStack(spacing: 14) {
                ToolIcon(symbol: tool.symbol, colors: tool.colors, size: 48)
                VStack(alignment: .leading, spacing: 3) {
                    HStack(spacing: 6) {
                        Text(tool.name).font(.headline).foregroundStyle(.primary)
                        if settings.favoriteTools.contains(tool.rawValue) {
                            Image(systemName: "star.fill").font(.caption2).foregroundStyle(.yellow)
                        }
                    }
                    Text(tool.subtitle).font(.caption).foregroundStyle(.secondary)
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
        .contextMenu { favoriteButton(tool) }
    }

    @ViewBuilder
    private func favoriteButton(_ tool: ToolKind) -> some View {
        let isFavorite = settings.favoriteTools.contains(tool.rawValue)
        Button(isFavorite ? "取消收藏" : "收藏到首页", systemImage: isFavorite ? "star.slash" : "star") {
            settings.toggleFavorite(tool.rawValue)
            Haptics.success()
        }
    }
}
