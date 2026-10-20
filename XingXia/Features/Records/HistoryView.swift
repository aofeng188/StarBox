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
    /// 点开补记照做 / 满意度的那条
    @State private var editing: DecisionRecord?

    private var tools: [String] {
        var seen: [String] = []
        for record in records where !seen.contains(record.tool) { seen.append(record.tool) }
        return seen
    }

    private var filtered: [DecisionRecord] {
        records.filter { record in
            (filter == nil || record.tool == filter)
                && (search.isEmpty || record.result.contains(search) || record.title.contains(search)
                    || record.question?.contains(search) == true)
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
                        row(record)
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
        .sheet(item: $editing) { record in
            DecisionEditSheet(record: record)
        }
    }

    /// 决定可以点开补记「照做了吗、后来怎么样」；占卜和游戏只是记一笔
    @ViewBuilder
    private func row(_ record: DecisionRecord) -> some View {
        if JournalStats.isDecision(record.tool) {
            Button { editing = record } label: { HistoryRow(record: record) }
                .buttonStyle(.plain)
                .accessibilityHint("补记是否照做、后来怎么样")
        } else {
            HistoryRow(record: record)
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

/// 决策历史的一行：结果、工具和标题，「问一问」带来的问题，以及照做 / 满意度标记
struct HistoryRow: View {
    let record: DecisionRecord

    var body: some View {
        let info = HistoryLog.info(for: record.tool)
        HStack(spacing: 12) {
            ToolIcon(symbol: info.symbol, colors: info.colors, size: 34)
            VStack(alignment: .leading, spacing: 3) {
                Text(record.result).font(.subheadline.weight(.semibold)).foregroundStyle(.primary).lineLimit(1)
                Text(record.title.isEmpty ? info.name : "\(info.name) · \(record.title)")
                    .font(.caption).foregroundStyle(.primary.opacity(0.7)).lineLimit(1)
                if let question = extraQuestion {
                    Label(question, systemImage: "questionmark.bubble")
                        .font(.caption)
                        .foregroundStyle(.primary.opacity(0.7))
                        .lineLimit(1)
                }
                DecisionMarks(record: record)
            }
            Spacer()
            Text(DateText.relative(record.date)).font(.caption2).foregroundStyle(.primary.opacity(0.7))
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }

    /// 「问一问」带过来的问题；和标题一样时（硬币、答案之书把问题当标题）不重复显示
    private var extraQuestion: String? {
        guard let question = record.question?.trimmingCharacters(in: .whitespacesAndNewlines), !question.isEmpty,
              question != record.title.trimmingCharacters(in: .whitespacesAndNewlines) else { return nil }
        return question
    }
}
