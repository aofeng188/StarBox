import SwiftUI
import SwiftData
import Charts
import Combine

struct MoodView: View {
    @Environment(\.modelContext) private var context
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Query(sort: \MoodEntry.dayKey, order: .reverse) private var entries: [MoodEntry]

    /// 零点和回到前台时刷新
    @State private var today = DayKey.today
    /// 心情月历显示的月份（1 号）
    @State private var month = DayKey(year: DayKey.today.year, month: DayKey.today.month, day: 1)
    /// 正在补记或修改的过去某天
    @State private var editingDay: EditingDay?
    /// 刚删除的记录，几秒内可以撤销
    @State private var deleted: DeletedMood?

    private static let editorAnchor = "moodEditor"
    private static let weekdayHeaders = ["一", "二", "三", "四", "五", "六", "日"]

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                VStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 14) {
                        SectionHeader(title: "今天的心情", subtitle: DateText.monthDay(today), symbol: "face.smiling")
                        // 跨天时换成新的一天；旧的输入框消失前会把内容存回原来那天
                        MoodDayEditor(day: today, showsSaveButton: true)
                            .id(today)
                    }
                    .glassCard()
                    .id(Self.editorAnchor)

                    monthHeatmap(proxy: proxy)

                    if let insight = correlationInsight {
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "sparkles").foregroundStyle(palette.accentGradient).font(.title3)
                            Text(insight).font(.subheadline).lineSpacing(3)
                        }
                        .glassCard()
                    }

                    distribution

                    diary(proxy: proxy)
                }
                .pageContent()
                .padding(.bottom, 30)
            }
            .safeAreaInset(edge: .bottom) { undoBanner }
        }
        .cosmicBackground()
        .navigationTitle("心情打卡")
        .sheet(item: $editingDay) { item in
            NavigationStack {
                ScrollView {
                    MoodDayEditor(day: item.day)
                        .glassCard()
                        .pageContent()
                        .padding(.top, 8)
                }
                .cosmicBackground(dimmed: true)
                .navigationTitle("\(DateText.monthDay(item.day)) \(DateText.weekday(item.day))")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("完成", systemImage: "checkmark") { editingDay = nil }
                    }
                }
            }
            .presentationDetents([.medium, .large])
        }
        .task(id: deleted?.id) {
            guard deleted != nil else { return }
            // 读屏用户找到「撤销」需要更久
            try? await Task.sleep(for: .seconds(UIAccessibility.isVoiceOverRunning ? 10 : 5))
            guard !Task.isCancelled else { return }
            withAnimation(.snappy) { deleted = nil }
        }
        // 这个通知不保证在主线程发出
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
    }

    private func monthHeatmap(proxy: ScrollViewProxy) -> some View {
        let days = Calendar.gregorianLocal.range(of: .day, in: .month, for: month.date)?.count ?? 30
        let leading = (month.weekday + 6) % 7
        let monthDays = (1...days).map { DayKey(year: month.year, month: month.month, day: $0) }
        let map = Dictionary(entries.map { ($0.dayKey, $0.mood) }, uniquingKeysWith: { a, _ in a })
        let isCurrentMonth = month.year == today.year && month.month == today.month
        let title = month.year == today.year ? "\(month.month)月心情" : "\(String(month.year))年\(month.month)月心情"
        return VStack(alignment: .leading, spacing: 10) {
            HStack(spacing: 4) {
                SectionHeader(title: title, subtitle: averageText, symbol: "square.grid.3x3.fill")
                Button { shiftMonth(-1) } label: { Image(systemName: "chevron.left").frame(width: 30, height: 30) }
                    .accessibilityLabel("上个月")
                Button { shiftMonth(1) } label: { Image(systemName: "chevron.right").frame(width: 30, height: 30) }
                    .accessibilityLabel("下个月")
                    .disabled(isCurrentMonth)
            }
            .font(.subheadline.weight(.semibold))
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
                // 三段的 id 类型各不相同（字符串 / 负数 / DayKey），不会互相撞 id 导致格子空白
                ForEach(Self.weekdayHeaders, id: \.self) { Text($0).font(.caption2).foregroundStyle(.primary.opacity(0.7)) }
                ForEach((0..<leading).map { -1 - $0 }, id: \.self) { _ in Color.clear.frame(height: 34) }
                ForEach(monthDays, id: \.self) { key in
                    dayCell(key, mood: map[key.rawValue].flatMap(MoodLevel.init(rawValue:)), proxy: proxy)
                }
            }
            Text("点过去的日子可以补记心情").font(.caption2).foregroundStyle(.primary.opacity(0.7))
        }
        .glassCard()
    }

    private func dayCell(_ key: DayKey, mood: MoodLevel?, proxy: ScrollViewProxy) -> some View {
        let isFuture = key > today
        return Button {
            Haptics.selection()
            edit(key, proxy: proxy)
        } label: {
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(mood?.color.opacity(0.75) ?? .primary.opacity(isFuture ? 0.02 : 0.06))
                if let mood {
                    Text(mood.emoji).font(.caption)
                } else {
                    Text("\(key.day)").font(.caption2).foregroundStyle(.primary.opacity(isFuture ? 0.35 : 0.7))
                }
            }
            .frame(height: 34)
            .overlay {
                if key == today { RoundedRectangle(cornerRadius: 8).strokeBorder(palette.accent, lineWidth: 1.5) }
            }
        }
        .buttonStyle(.plain)
        .disabled(isFuture)
        .accessibilityLabel("\(key.month)月\(key.day)日，\(mood.map { "心情\($0.name)" } ?? "未记录")")
        .accessibilityHint("记录这天的心情")
    }

    private func diary(proxy: ScrollViewProxy) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "心情日记", subtitle: entries.isEmpty ? nil : "长按可修改或删除", symbol: "book")
            if entries.isEmpty {
                Text("还没有记录，从今天开始吧").font(.subheadline).foregroundStyle(.primary.opacity(0.7))
            }
            ForEach(entries.prefix(30)) { entry in
                if let mood = MoodLevel(rawValue: entry.mood), let key = DayKey(rawValue: entry.dayKey) {
                    HStack(alignment: .top, spacing: 12) {
                        Text(mood.emoji).font(.title2)
                        VStack(alignment: .leading, spacing: 3) {
                            Text("\(DateText.monthDay(key)) \(DateText.weekday(key)) · \(mood.name)").font(.subheadline.weight(.semibold))
                            if !entry.note.isEmpty { Text(entry.note).font(.subheadline).foregroundStyle(.primary.opacity(0.75)) }
                            if !entry.tags.isEmpty { Text(entry.tags.map { "#" + $0 }.joined(separator: " ")).font(.caption).foregroundStyle(.tint) }
                        }
                        Spacer()
                    }
                    .contentShape(.rect)
                    // ScrollView 里没有 swipeActions，改成长按菜单
                    .contextMenu {
                        Button("修改", systemImage: "pencil") { edit(key, proxy: proxy) }
                        Button("删除", systemImage: "trash", role: .destructive) { delete(entry) }
                    }
                    .accessibilityElement(children: .combine)
                    .accessibilityAction(named: "修改") { edit(key, proxy: proxy) }
                    .accessibilityAction(named: "删除") { delete(entry) }
                }
            }
        }
        .glassCard()
    }

    @ViewBuilder
    private var undoBanner: some View {
        if let deleted, let key = DayKey(rawValue: deleted.dayKey) {
            HStack(spacing: 12) {
                Image(systemName: "trash").foregroundStyle(.secondary)
                Text("已删除 \(DateText.monthDay(key)) 的心情").font(.subheadline)
                Spacer(minLength: 0)
                Button("撤销") { undoDelete() }
                    .font(.subheadline.weight(.semibold))
            }
            .glassCard(cornerRadius: 20, padding: 14)
            .frame(maxWidth: PageMetrics.readableWidth)
            .padding(.horizontal, 16)
            .padding(.bottom, 8)
            .transition(reduceMotion ? AnyTransition.opacity : AnyTransition.move(edge: .bottom).combined(with: .opacity))
        }
    }

    private var averageText: String? {
        let monthEntries = entries.filter { $0.dayKey / 100 == month.year * 100 + month.month }
        guard !monthEntries.isEmpty else { return nil }
        let avg = Double(monthEntries.map(\.mood).reduce(0, +)) / Double(monthEntries.count)
        return String(format: "打卡 %d 天 · 平均 %.1f", monthEntries.count, avg)
    }

    private var distribution: some View {
        let recent = entries.prefix(60)
        let counts = MoodLevel.allCases.map { mood in (mood, recent.filter { $0.mood == mood.rawValue }.count) }
        return VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "心情分布", subtitle: "最近 \(recent.count) 次", symbol: "chart.bar.fill")
            Chart(counts, id: \.0) { item in
                BarMark(x: .value("心情", item.0.emoji), y: .value("次数", item.1))
                    .foregroundStyle(item.0.color.gradient)
                    .cornerRadius(6)
            }
            .chartYAxis(.hidden)
            .frame(height: 140)
        }
        .glassCard()
    }

    /// 心情与当日运势分数的相关性（皮尔逊相关系数）
    private var correlationInsight: String? {
        let pairs: [(Double, Double)] = entries.prefix(60).compactMap { entry in
            guard let key = DayKey(rawValue: entry.dayKey) else { return nil }
            return (Double(entry.mood), Double(FortuneEngine.daily(for: app.birth, on: key).score))
        }
        guard pairs.count >= 5 else {
            return pairs.isEmpty ? nil : "再打卡 \(5 - pairs.count) 天，就能看到心情和运势之间的关联啦。"
        }
        let n = Double(pairs.count)
        let mx = pairs.map(\.0).reduce(0, +) / n
        let my = pairs.map(\.1).reduce(0, +) / n
        let cov = pairs.reduce(0) { $0 + ($1.0 - mx) * ($1.1 - my) }
        let vx = pairs.reduce(0) { $0 + pow($1.0 - mx, 2) }
        let vy = pairs.reduce(0) { $0 + pow($1.1 - my, 2) }
        guard vx > 0, vy > 0 else { return "最近心情很稳定，运势起伏对你影响不大。" }
        let r = cov / (vx * vy).squareRoot()
        switch r {
        case 0.4...: return String(format: "你的心情和运势很同步（相关度 %.2f）：运势好的日子，你多半也心情不错。", r)
        case 0.15..<0.4: return String(format: "心情和运势有一点点关联（相关度 %.2f），好运会给你加点分。", r)
        case -0.15..<0.15: return String(format: "你的心情几乎不受运势影响（相关度 %.2f），快乐由自己决定！", r)
        default: return String(format: "有趣：运势平平的日子你反而更开心（相关度 %.2f），你是逆风翻盘型选手。", r)
        }
    }

    /// 今天回到顶部的输入卡片；过去的日子弹出补记面板
    private func edit(_ day: DayKey, proxy: ScrollViewProxy) {
        guard day <= today else { return }
        if day == today {
            withAnimation(.snappy) { proxy.scrollTo(Self.editorAnchor, anchor: .top) }
        } else {
            editingDay = EditingDay(day: day)
        }
    }

    private func delete(_ entry: MoodEntry) {
        let snapshot = DeletedMood(dayKey: entry.dayKey, mood: entry.mood, note: entry.note, tags: entry.tags, date: entry.date)
        context.delete(entry)
        try? context.save()
        Haptics.tap()
        withAnimation(.snappy) { deleted = snapshot }
        if let key = DayKey(rawValue: snapshot.dayKey) {
            Announce.post("已删除\(DateText.monthDay(key))的心情，可以撤销")
        }
    }

    private func undoDelete() {
        guard let snapshot = deleted else { return }
        withAnimation(.snappy) { deleted = nil }
        // 删除后那天可能又记过心情，这时不再恢复，避免同一天出现两条
        guard !entries.contains(where: { $0.dayKey == snapshot.dayKey }) else {
            Announce.post("这天已经重新记录了心情，没有恢复")
            return
        }
        let entry = MoodEntry(dayKey: snapshot.dayKey, mood: snapshot.mood, note: snapshot.note)
        entry.tags = snapshot.tags
        entry.date = snapshot.date
        context.insert(entry)
        try? context.save()
        Haptics.success()
        Announce.post("已恢复")
    }

    private func shiftMonth(_ delta: Int) {
        let date = Calendar.gregorianLocal.date(byAdding: .month, value: delta, to: month.date) ?? month.date
        let key = DayKey(date)
        withAnimation(.snappy) { month = DayKey(year: key.year, month: key.month, day: 1) }
    }

    private func refreshToday() {
        let now = DayKey.today
        guard now != today else { return }
        // 月历停在「本月」时跟着翻到新的月份
        if month.year == today.year && month.month == today.month {
            month = DayKey(year: now.year, month: now.month, day: 1)
        }
        today = now
    }
}

/// 某一天的心情、一句话和标签。文字停顿片刻后自动保存，离开时再存一次。
private struct MoodDayEditor: View {
    let day: DayKey
    var showsSaveButton = false

    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Query(sort: \MoodEntry.dayKey, order: .reverse) private var entries: [MoodEntry]

    @State private var note = ""
    @State private var tags: Set<String> = []
    @State private var loaded = false
    @FocusState private var noteFocused: Bool

    private var entry: MoodEntry? { entries.first { $0.dayKey == day.rawValue } }

    private var placeholder: String { day == .today ? "写一句话记录今天…" : "写一句话记录这一天…" }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            MoodPicker(selected: entry?.mood, size: 52) { mood in setMood(mood.rawValue) }
            TextField(placeholder, text: $note, axis: .vertical)
                .lineLimit(1...4)
                .focused($noteFocused)
                .padding(12)
                .background(.primary.opacity(0.05), in: .rect(cornerRadius: 14))
            if entry == nil && !note.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text("选一个心情后，这句话会一起保存").font(.caption).foregroundStyle(.primary.opacity(0.7))
            }
            FlowLayout(spacing: 6) {
                ForEach(MoodLevel.tagOptions, id: \.self) { tag in
                    let isOn = tags.contains(tag)
                    Button {
                        if isOn { tags.remove(tag) } else { tags.insert(tag) }
                        saveNote()
                    } label: {
                        Text("#" + tag)
                            .font(.caption.weight(.medium))
                            .padding(.horizontal, 10).padding(.vertical, 6)
                            .foregroundStyle(isOn ? palette.onAccent : .primary)
                            .background(Capsule().fill(isOn ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.07))))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("标签 \(tag)")
                    .accessibilityAddTraits(isOn ? .isSelected : [])
                }
            }
            if showsSaveButton && entry != nil {
                Button("保存", systemImage: "checkmark") {
                    noteFocused = false
                    saveNote()
                    Haptics.success()
                }
                .buttonStyle(.glass)
            }
        }
        .onAppear(perform: load)
        .onChange(of: entry?.id) { _, _ in
            // 记录被删除或撤销恢复后，输入框跟着更新
            note = entry?.note ?? ""
            tags = Set(entry?.tags ?? [])
        }
        .task(id: note) {
            // 多行输入框按回车只会换行、不会触发 onSubmit，所以停止输入片刻后自动保存
            guard loaded else { return }
            try? await Task.sleep(for: .milliseconds(600))
            guard !Task.isCancelled else { return }
            saveNote()
        }
        .onDisappear(perform: saveNote)
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        note = entry?.note ?? ""
        tags = Set(entry?.tags ?? [])
    }

    private func setMood(_ value: Int) {
        if let entry {
            entry.mood = value
            entry.date = Date()
        } else {
            let newEntry = MoodEntry(dayKey: day.rawValue, mood: value, note: note)
            newEntry.tags = tags.sorted()
            context.insert(newEntry)
        }
        try? context.save()
    }

    private func saveNote() {
        guard let entry else { return }
        let sortedTags = tags.sorted()
        guard entry.note != note || entry.tags != sortedTags else { return }
        entry.note = note
        entry.tags = sortedTags
        try? context.save()
    }
}

private struct EditingDay: Identifiable {
    let day: DayKey
    var id: Int { day.rawValue }
}

private struct DeletedMood: Identifiable {
    let id = UUID()
    let dayKey: Int
    let mood: Int
    let note: String
    let tags: [String]
    let date: Date
}
