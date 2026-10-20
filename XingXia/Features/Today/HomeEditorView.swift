import SwiftUI

/// 编辑首页模块：拖动排序、显示/隐藏。「决定回访」固定在最前、随记录自动出现，不在列表里
struct HomeEditorView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(settings.homeCards.filter(\.isEditable)) { card in
                        let visible = !settings.hiddenHomeCards.contains(card)
                        HStack(spacing: 14) {
                            Image(systemName: card.symbol)
                                .frame(width: 28)
                                .foregroundStyle(.tint)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(card.name)
                                if card == .family {
                                    Text("有两个及以上档案时显示").font(.caption).foregroundStyle(.secondary)
                                }
                            }
                            Spacer()
                            Toggle(card.name, isOn: Binding(
                                get: { visible },
                                set: { isOn in
                                    if isOn { settings.hiddenHomeCards.remove(card) } else { settings.hiddenHomeCards.insert(card) }
                                }
                            ))
                            .labelsHidden()
                        }
                        .listRowBackground(Color.clear)
                    }
                    .onMove { from, to in
                        // 列表里没有「决定回访」，偏移量按可编辑的模块算，排好后再把它放回最前
                        var cards = settings.homeCards.filter(\.isEditable)
                        cards.move(fromOffsets: from, toOffset: to)
                        settings.homeCards = HomeCard.normalized(cards)
                    }
                } footer: {
                    Text("拖动右侧的把手调整顺序，关闭开关可以隐藏模块。有需要回访的决定时，「决定回访」会自动出现在最前面。")
                }
            }
            .environment(\.editMode, .constant(.active))
            .cosmicBackground(dimmed: true)
            .navigationTitle("编辑首页")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("完成", systemImage: "checkmark") { dismiss() }
                }
            }
        }
    }
}
