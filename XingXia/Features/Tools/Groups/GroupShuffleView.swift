import SwiftUI
import SwiftData

struct GroupShuffleView: View {
    enum Mode: String, CaseIterable, Identifiable {
        case groups, order, pick
        var id: String { rawValue }
        var name: String { ["groups": "随机分组", "order": "随机排序", "pick": "随机点名"][rawValue]! }
    }

    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Query(sort: \Roster.createdAt) private var rosters: [Roster]
    @AppStorage("groups.text") private var text = "小明\n小红\n小刚\n小美\n阿杰\n小雨\n大壮\n朵朵"
    @State private var mode: Mode = .groups
    @State private var groupCount = 2
    @State private var bySize = false
    @State private var groupSize = 3
    @State private var pickCount = 1
    @State private var output: [[String]] = []
    @State private var revealed = 0
    /// 逐个揭晓结果的任务，再次开始时取消上一次，避免进度闪回
    @State private var revealTask: Task<Void, Never>?
    @State private var showSave = false
    @State private var rosterName = ""
    @FocusState private var editing: Bool

    private var names: [String] {
        text.components(separatedBy: CharacterSet(charactersIn: "\n,，、 ;；"))
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Picker("模式", selection: $mode) {
                    ForEach(Mode.allCases) { Text($0.name).tag($0) }
                }
                .pickerStyle(.segmented)
                .onChange(of: mode) { _, _ in
                    revealTask?.cancel()
                    output = []
                }

                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        SectionHeader(title: "名单", subtitle: "\(names.count) 人", symbol: "person.3.fill")
                        Menu {
                            if !rosters.isEmpty {
                                Section("载入名单") {
                                    ForEach(rosters) { roster in
                                        Button("\(roster.name)（\(roster.members.count)人）") { text = roster.members.joined(separator: "\n") }
                                    }
                                }
                            }
                            Button("保存当前名单", systemImage: "square.and.arrow.down") { showSave = true }
                            if !rosters.isEmpty {
                                Menu("删除名单") {
                                    ForEach(rosters) { roster in
                                        Button(roster.name, role: .destructive) { context.delete(roster); try? context.save() }
                                    }
                                }
                            }
                        } label: {
                            Image(systemName: "tray.full").padding(8)
                        }
                        .accessibilityLabel("名单管理")
                    }
                    TextEditor(text: $text)
                        .focused($editing)
                        .frame(minHeight: 120, maxHeight: 200)
                        .scrollContentBackground(.hidden)
                        .padding(8)
                        .background(.primary.opacity(0.05), in: .rect(cornerRadius: 14))
                    Text("每行一个名字，也可以用逗号或空格分隔").font(.caption).foregroundStyle(.secondary)
                }
                .glassCard()

                VStack(spacing: 12) {
                    switch mode {
                    case .groups:
                        Toggle("按每组人数分", isOn: $bySize)
                        if bySize {
                            Stepper("每组 \(groupSize) 人", value: $groupSize, in: 1...max(1, names.count))
                        } else {
                            Stepper("分成 \(groupCount) 组", value: $groupCount, in: 2...max(2, min(20, names.count)))
                        }
                    case .order:
                        Text("把名单随机打乱排一个顺序（发言顺序、值日表…）").font(.subheadline).foregroundStyle(.secondary)
                    case .pick:
                        Stepper("抽 \(pickCount) 人", value: $pickCount, in: 1...max(1, names.count))
                    }
                }
                .glassCard()

                BigActionButton(title: "开始", symbol: "shuffle", isEnabled: names.count >= 2) { run() }

                if !output.isEmpty { results }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .scrollDismissesKeyboard(.interactively)
        .cosmicBackground()
        .navigationTitle("随机分组")
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("完成") { editing = false }
            }
        }
        .alert("保存名单", isPresented: $showSave) {
            TextField("名单名称，如「三年二班」", text: $rosterName)
            Button("保存") {
                context.insert(Roster(name: rosterName.isEmpty ? "名单 \(rosters.count + 1)" : rosterName, members: names))
                try? context.save()
                rosterName = ""
            }
            Button("取消", role: .cancel) {}
        }
        .onShake(isEnabled: !showSave) { if names.count >= 2 { run() } }
    }

    @ViewBuilder
    private var results: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                SectionHeader(title: "结果", symbol: "checkmark.seal.fill")
                ShareLink(item: shareText) { Image(systemName: "square.and.arrow.up") }
                Button { UIPasteboard.general.string = shareText; Haptics.success() } label: { Image(systemName: "doc.on.doc") }
                    .padding(.leading, 8)
                    .accessibilityLabel("拷贝结果")
            }
            switch mode {
            case .groups:
                ForEach(Array(output.enumerated()), id: \.offset) { index, group in
                    if index < revealed {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("第 \(index + 1) 组 · \(group.count) 人").font(.subheadline.bold()).foregroundStyle(palette.inkColor(at: index))
                            FlowLayout(spacing: 6) {
                                ForEach(group, id: \.self) { name in
                                    Text(name).font(.subheadline)
                                        .padding(.horizontal, 10).padding(.vertical, 6)
                                        .background(palette.color(at: index).opacity(0.18), in: .capsule)
                                }
                            }
                        }
                        .padding(12)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(palette.color(at: index).opacity(0.08), in: .rect(cornerRadius: 16))
                        .transition(.scale(scale: 0.8).combined(with: .opacity))
                    }
                }
            case .order, .pick:
                ForEach(Array((output.first ?? []).enumerated()), id: \.offset) { index, name in
                    if index < revealed {
                        HStack(spacing: 12) {
                            Text("\(index + 1)")
                                .font(.headline.monospacedDigit())
                                .foregroundStyle(palette.color(at: index).readableForeground)
                                .frame(width: 32, height: 32)
                                .background(Circle().fill(palette.color(at: index)))
                            Text(name).font(mode == .pick ? .title3.bold() : .body)
                            Spacer()
                        }
                        .transition(.move(edge: .leading).combined(with: .opacity))
                    }
                }
            }
        }
        .glassCard()
    }

    private var shareText: String {
        switch mode {
        case .groups: output.enumerated().map { "第\($0.offset + 1)组：\($0.element.joined(separator: "、"))" }.joined(separator: "\n")
        case .order: (output.first ?? []).enumerated().map { "\($0.offset + 1). \($0.element)" }.joined(separator: "\n")
        case .pick: "抽中：" + (output.first ?? []).joined(separator: "、")
        }
    }

    private func run() {
        editing = false
        let shuffled = names.shuffled()
        switch mode {
        case .groups:
            let count = bySize ? Int(ceil(Double(shuffled.count) / Double(max(1, groupSize)))) : min(groupCount, shuffled.count)
            var groups = Array(repeating: [String](), count: max(1, count))
            for (i, name) in shuffled.enumerated() { groups[i % groups.count].append(name) }
            output = groups
        case .order:
            output = [shuffled]
        case .pick:
            output = [Array(shuffled.prefix(pickCount))]
        }
        revealed = 0
        SoundEngine.shared.play(.whoosh, volume: 0.6)
        let total = mode == .groups ? output.count : (output.first?.count ?? 0)
        let interval = mode == .order ? 90 : 220
        let summary = shareText.replacingOccurrences(of: "\n", with: "；")
        let announcement: String = switch mode {
        case .groups: "分好了，\(summary)"
        case .order: "排好了，\(summary)"
        case .pick: summary
        }
        revealTask?.cancel()
        revealTask = Task {
            for i in 1...max(1, total) {
                try? await Task.sleep(for: .milliseconds(interval))
                guard !Task.isCancelled else { return }
                withAnimation(.spring(response: 0.4, dampingFraction: 0.7)) { revealed = i }
                SoundEngine.shared.play(.pop, volume: 0.5)
                Haptics.selection()
            }
            Haptics.success()
            Announce.post(announcement)
        }
        HistoryLog.add(context, tool: ToolKind.groups.rawValue, title: mode.name, result: summary)
    }
}
