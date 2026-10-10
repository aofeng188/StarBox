import SwiftUI

/// 编辑首页模块：拖动排序、显示/隐藏
struct HomeEditorView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(settings.homeCards) { card in
                        let visible = !settings.hiddenHomeCards.contains(card)
                        HStack(spacing: 14) {
                            Image(systemName: card.symbol)
                                .frame(width: 28)
                                .foregroundStyle(.tint)
                            Text(card.name)
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
                    .onMove { from, to in settings.homeCards.move(fromOffsets: from, toOffset: to) }
                } footer: {
                    Text("拖动右侧的把手调整顺序，关闭开关可以隐藏模块。")
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
