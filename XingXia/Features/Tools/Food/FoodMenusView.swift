import SwiftUI
import SwiftData

/// 管理自定义菜单
struct FoodMenusView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \FoodMenu.createdAt) private var menus: [FoodMenu]

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(menus) { menu in
                        NavigationLink {
                            FoodMenuEditor(menu: menu)
                        } label: {
                            HStack {
                                Text(menu.icon).font(.title2)
                                VStack(alignment: .leading) {
                                    Text(menu.name)
                                    Text("\(menu.items?.count ?? 0) 个选项").font(.caption).foregroundStyle(.secondary)
                                }
                            }
                        }
                    }
                    .onDelete { offsets in
                        for index in offsets { context.delete(menus[index]) }
                        try? context.save()
                    }
                } footer: {
                    Text("把常去的店、家里会做的菜整理成菜单，抽签时只在菜单里选。")
                }
                .glassRows()
            }
            .overlay {
                if menus.isEmpty { EmptyHint(symbol: "list.bullet.rectangle", title: "还没有菜单", message: "点右上角 + 新建") }
            }
            .cosmicBackground(dimmed: true)
            .navigationTitle("我的菜单")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("完成") { dismiss() } }
                ToolbarItem(placement: .primaryAction) {
                    Button("新建", systemImage: "plus") {
                        let menu = FoodMenu(name: "新菜单 \(menus.count + 1)")
                        context.insert(menu)
                        try? context.save()
                    }
                }
            }
        }
    }
}

struct FoodMenuEditor: View {
    @Bindable var menu: FoodMenu
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @State private var newItem = ""

    private let icons = ["🍱", "🍜", "🍔", "🍕", "🍣", "🥗", "🍲", "🥟", "🍛", "🧋", "🏢", "🏠", "🎉", "💼"]

    var body: some View {
        List {
            Section("名称与图标") {
                TextField("菜单名称", text: $menu.name)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(icons, id: \.self) { icon in
                            Button {
                                menu.icon = icon
                            } label: {
                                Text(icon).font(.title2).padding(6)
                                    .background(Circle().fill(menu.icon == icon ? palette.accent.opacity(0.3) : .clear))
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("图标 \(icon)")
                            .accessibilityAddTraits(menu.icon == icon ? .isSelected : [])
                        }
                    }
                }
                .fadingScrollEdges()
            }
            .glassRows()

            Section {
                ForEach(menu.sortedItems) { item in
                    MenuItemRow(item: item)
                }
                .onDelete { offsets in
                    let items = menu.sortedItems
                    for index in offsets { context.delete(items[index]) }
                    try? context.save()
                }
                HStack {
                    TextField("添加选项，如「楼下兰州拉面」", text: $newItem)
                        .onSubmit(add)
                    Button("添加", systemImage: "plus.circle.fill", action: add)
                        .labelStyle(.iconOnly)
                        .disabled(newItem.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            } header: {
                Text("选项")
            } footer: {
                Text("权重越高越容易被抽中；权重为 0 表示暂时不参与。")
            }
            .glassRows()
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle(menu.name)
        .navigationBarTitleDisplayMode(.inline)
    }

    private func add() {
        let name = newItem.trimmingCharacters(in: .whitespaces)
        guard !name.isEmpty else { return }
        // 接在现有最大序号后面，删除过选项也不会和已有项重号
        let nextIndex = ((menu.items ?? []).map(\.sortIndex).max() ?? -1) + 1
        let item = FoodMenuItem(name: name, sortIndex: nextIndex)
        item.menu = menu
        context.insert(item)
        try? context.save()
        newItem = ""
    }
}

private struct MenuItemRow: View {
    @Bindable var item: FoodMenuItem

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            TextField("名称", text: $item.name).font(.body.weight(.medium))
            HStack {
                TextField("备注（可选）", text: $item.note).font(.caption).foregroundStyle(.secondary)
                Stepper("权重 \(Int(item.weight))", value: $item.weight, in: 0...10, step: 1)
                    .font(.caption)
                    .fixedSize()
            }
        }
    }
}
