import SwiftUI
import SwiftData
import Charts

struct HistoryView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Query(sort: \DecisionRecord.date, order: .reverse) private var records: [DecisionRecord]
    @State private var filter: String?
    @State private var search = ""
    @State private var confirmClear = false

    private var tools: [String] {
        var seen: [String] = []
        for record in records where !seen.contains(record.tool) { seen.append(record.tool) }
        return seen
    }

    private var filtered: [DecisionRecord] {
        records.filter { record in
            (filter == nil || record.tool == filter)
                && (search.isEmpty || record.result.contains(search) || record.title.contains(search))
        }
    }

    private var grouped: [(DayKey, [DecisionRecord])] {
        var result: [(DayKey, [DecisionRecord])] = []
        for record in filtered {
            let key = DayKey(record.date)
            if let last = result.last, last.0 == key {
                result[result.count - 1].1.append(record)
            } else {
                result.append((key, [record]))
            }
        }
        return result
    }

    var body: some View {
        List {
            if !records.isEmpty {
                Section {
                    stats
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))

                Section {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            chip(title: "全部", isOn: filter == nil) { filter = nil }
                            ForEach(tools, id: \.self) { tool in
                                chip(title: HistoryLog.info(for: tool).name, isOn: filter == tool) { filter = tool }
                            }
                        }
                    }
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets(top: 0, leading: 0, bottom: 0, trailing: 0))
            }

            ForEach(grouped, id: \.0) { day, items in
                Section {
                    ForEach(items) { record in
                        HistoryRow(record: record)
                            .swipeActions {
                                Button("删除", systemImage: "trash", role: .destructive) {
                                    context.delete(record)
                                    try? context.save()
                                }
                            }
                            .contextMenu {
                                Button("拷贝结果", systemImage: "doc.on.doc") { UIPasteboard.general.string = record.result }
                            }
                    }
                } header: {
                    Text(day == .today ? "今天" : "\(DateText.full(day)) \(DateText.weekday(day))")
                }
                .glassRows()
            }
        }
        .overlay {
            if records.isEmpty {
                EmptyHint(symbol: "clock.arrow.circlepath", title: "还没有记录", message: "使用随机工具、求签、塔罗后，结果会自动保存在这里")
            } else if filtered.isEmpty {
                EmptyHint(symbol: "magnifyingglass", title: "没有匹配的记录")
            }
        }
        .searchable(text: $search, prompt: "搜索结果")
        .cosmicBackground()
        .navigationTitle("决策历史")
        .toolbar {
            if !records.isEmpty {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        Button("清空全部", systemImage: "trash", role: .destructive) { confirmClear = true }
                    } label: {
                        Image(systemName: "ellipsis")
                    }
                }
            }
        }
        .confirmationDialog("清空全部历史记录？", isPresented: $confirmClear, titleVisibility: .visible) {
            Button("清空", role: .destructive) {
                try? context.delete(model: DecisionRecord.self)
                try? context.save()
            }
        }
    }

    private func chip(title: String, isOn: Bool, action: @escaping () -> Void) -> some View {
        Button {
            withAnimation(.snappy) { action() }
            Haptics.selection()
        } label: {
            Text(title)
                .font(.subheadline.weight(isOn ? .semibold : .regular))
                .padding(.horizontal, 12).padding(.vertical, 7)
                .foregroundStyle(isOn ? palette.onAccent : .primary)
                .background(Capsule().fill(isOn ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.07))))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }

    private var stats: some View {
        let since = Calendar.current.date(byAdding: .day, value: -30, to: Date()) ?? Date()
        let recent = records.filter { $0.date >= since }
        let counts = Dictionary(recent.map { ($0.tool, 1) }, uniquingKeysWith: +)
            .sorted { $0.value > $1.value }
            .prefix(6)
        let topResult = Dictionary((filter == nil ? recent : recent.filter { $0.tool == filter }).map { ($0.result, 1) }, uniquingKeysWith: +)
            .max { $0.value < $1.value }
        return VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "近 30 天", subtitle: "共 \(recent.count) 次", symbol: "chart.bar.xaxis")
            Chart(Array(counts), id: \.key) { item in
                BarMark(x: .value("次数", item.value), y: .value("工具", HistoryLog.info(for: item.key).name))
                    .foregroundStyle(palette.accentGradient)
                    .cornerRadius(5)
                    .annotation(position: .trailing) { Text("\(item.value)").font(.caption2).foregroundStyle(.secondary) }
            }
            .chartXAxis(.hidden)
            .frame(height: CGFloat(max(1, counts.count)) * 30)
            if let topResult, topResult.value > 1 {
                Text("出现最多的结果是「\(topResult.key)」，共 \(topResult.value) 次").font(.caption).foregroundStyle(.tint)
            }
        }
        .glassCard()
    }
}
