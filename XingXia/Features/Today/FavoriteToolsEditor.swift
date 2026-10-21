import SwiftUI

/// 管理首页「快捷工具」：调整顺序、移除、从全部工具里添加
struct FavoriteToolsEditor: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.dismiss) private var dismiss
    @Environment(\.palette) private var palette

    var body: some View {
        let favorites = settings.favoriteTools.compactMap(FavoriteTool.init(id:))
        let others = FavoriteTool.all.filter { !settings.isFavorite($0.id) }
        NavigationStack {
            List {
                Section {
                    if favorites.isEmpty {
                        Text("还没有收藏，从下面挑几个常用的吧")
                            .font(.subheadline)
                            .subtleForeground()
                            .listRowBackground(Color.clear)
                    }
                    ForEach(favorites) { tool in
                        row(tool)
                            .listRowBackground(Color.clear)
                    }
                    .onMove(perform: move)
                    .onDelete(perform: remove)
                } header: {
                    Text("已收藏")
                } footer: {
                    Text("拖动右侧的把手调整顺序，首页按这个顺序显示。在工具页右上角点星标也能收藏或取消。")
                }

                if !others.isEmpty {
                    Section("可以添加") {
                        ForEach(others) { tool in
                            Button {
                                settings.toggleFavorite(tool.id)
                                Haptics.selection()
                                // 这一行会挪到「已收藏」里，读屏焦点跟不过去，补一句播报
                                Announce.post("已添加\(tool.name)")
                            } label: {
                                HStack(spacing: 14) {
                                    row(tool)
                                    Spacer(minLength: 0)
                                    Image(systemName: "plus.circle.fill")
                                        .font(.title3)
                                        .foregroundStyle(.tint)
                                }
                                .contentShape(.rect)
                            }
                            // 编辑模式下行本身不响应点按，用 borderless 让按钮自己接收
                            .buttonStyle(.borderless)
                            .accessibilityLabel("添加\(tool.name)")
                            .listRowBackground(Color.clear)
                        }
                    }
                }
            }
            .environment(\.editMode, .constant(.active))
            .cosmicBackground(dimmed: true)
            .navigationTitle("快捷工具")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成", systemImage: "checkmark") { dismiss() }
                }
            }
        }
    }

    private func row(_ tool: FavoriteTool) -> some View {
        HStack(spacing: 14) {
            ToolIcon(symbol: tool.symbol, colors: palette.identity(tool.colors), size: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(tool.name).foregroundStyle(.primary)
                Text(tool.subtitle).font(.caption).subtleForeground()
            }
        }
    }

    /// 收藏列表里可能存着已下线工具的 id，按显示出来的这部分排序，未识别的放回末尾
    private func move(from source: IndexSet, to destination: Int) {
        var shown = settings.favoriteTools.filter { FavoriteTool(id: $0) != nil }
        let unknown = settings.favoriteTools.filter { FavoriteTool(id: $0) == nil }
        shown.move(fromOffsets: source, toOffset: destination)
        settings.favoriteTools = shown + unknown
    }

    private func remove(at offsets: IndexSet) {
        let shown = settings.favoriteTools.filter { FavoriteTool(id: $0) != nil }
        let ids = Set(offsets.compactMap { shown.indices.contains($0) ? shown[$0] : nil })
        settings.favoriteTools.removeAll { ids.contains($0) }
        Haptics.selection()
    }
}
