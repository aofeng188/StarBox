import SwiftUI
import SwiftData
import Charts

struct MoodView: View {
    @Environment(\.modelContext) private var context
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Query(sort: \MoodEntry.dayKey, order: .reverse) private var entries: [MoodEntry]

    @State private var note = ""
    @State private var tags: Set<String> = []
    @State private var loaded = false

    private var today: MoodEntry? { entries.first { $0.dayKey == DayKey.today.rawValue } }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 14) {
                    SectionHeader(title: "今天的心情", subtitle: DateText.monthDay(.today), symbol: "face.smiling")
                    MoodPicker(selected: today?.mood, size: 52) { mood in setMood(mood.rawValue) }
                    TextField("写一句话记录今天…", text: $note, axis: .vertical)
                        .lineLimit(1...4)
                        .padding(12)
                        .background(.primary.opacity(0.05), in: .rect(cornerRadius: 14))
                        .onSubmit(saveNote)
                    FlowLayout(spacing: 6) {
                        ForEach(MoodLevel.tagOptions, id: \.self) { tag in
                            Button {
                                if tags.contains(tag) { tags.remove(tag) } else { tags.insert(tag) }
                                saveNote()
                            } label: {
                                Text("#" + tag)
                                    .font(.caption.weight(.medium))
                                    .padding(.horizontal, 10).padding(.vertical, 6)
                                    .foregroundStyle(tags.contains(tag) ? .white : .primary)
                                    .background(Capsule().fill(tags.contains(tag) ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.07))))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    if today != nil {
                        Button("保存", systemImage: "checkmark") { saveNote(); Haptics.success() }
                            .buttonStyle(.glass)
                    }
                }
                .glassCard()

                monthHeatmap

                if let insight = correlationInsight {
                    HStack(alignment: .top, spacing: 12) {
                        Image(systemName: "sparkles").foregroundStyle(palette.accentGradient).font(.title3)
                        Text(insight).font(.subheadline).lineSpacing(3)
                    }
                    .glassCard()
                }

                distribution

                VStack(alignment: .leading, spacing: 10) {
                    SectionHeader(title: "心情日记", symbol: "book")
                    if entries.isEmpty {
                        Text("还没有记录，从今天开始吧").font(.subheadline).foregroundStyle(.secondary)
                    }
                    ForEach(entries.prefix(30)) { entry in
                        if let mood = MoodLevel(rawValue: entry.mood), let key = DayKey(rawValue: entry.dayKey) {
                            HStack(alignment: .top, spacing: 12) {
                                Text(mood.emoji).font(.title2)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text("\(DateText.monthDay(key)) \(DateText.weekday(key)) · \(mood.name)").font(.subheadline.weight(.semibold))
                                    if !entry.note.isEmpty { Text(entry.note).font(.subheadline).foregroundStyle(.secondary) }
                                    if !entry.tags.isEmpty { Text(entry.tags.map { "#" + $0 }.joined(separator: " ")).font(.caption).foregroundStyle(.tint) }
                                }
                                Spacer()
                            }
                            .swipeActions { Button("删除", role: .destructive) { context.delete(entry) } }
                        }
                    }
                }
                .glassCard()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("心情打卡")
        .onAppear {
            guard !loaded else { return }
            loaded = true
            note = today?.note ?? ""
            tags = Set(today?.tags ?? [])
        }
    }

    private var monthHeatmap: some View {
        let todayKey = DayKey.today
        let first = DayKey(year: todayKey.year, month: todayKey.month, day: 1)
        let days = Calendar.gregorianLocal.range(of: .day, in: .month, for: first.date)?.count ?? 30
        let leading = (first.weekday + 6) % 7
        let map = Dictionary(entries.map { ($0.dayKey, $0.mood) }, uniquingKeysWith: { a, _ in a })
        return VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "\(todayKey.month)月心情", subtitle: averageText, symbol: "square.grid.3x3.fill")
            LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 6) {
                ForEach(["一", "二", "三", "四", "五", "六", "日"], id: \.self) { Text($0).font(.caption2).foregroundStyle(.secondary) }
                ForEach(0..<leading, id: \.self) { _ in Color.clear.frame(height: 34) }
                ForEach(1...days, id: \.self) { day in
                    let key = DayKey(year: todayKey.year, month: todayKey.month, day: day)
                    let mood = map[key.rawValue].flatMap(MoodLevel.init(rawValue:))
                    ZStack {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(mood?.color.opacity(0.75) ?? .primary.opacity(key > todayKey ? 0.02 : 0.06))
                        if let mood {
                            Text(mood.emoji).font(.caption)
                        } else {
                            Text("\(day)").font(.caption2).foregroundStyle(.secondary)
                        }
                    }
                    .frame(height: 34)
                    .overlay {
                        if key == todayKey { RoundedRectangle(cornerRadius: 8).strokeBorder(palette.accent, lineWidth: 1.5) }
                    }
                }
            }
        }
        .glassCard()
    }

    private var averageText: String? {
        let key = DayKey.today
        let month = entries.filter { $0.dayKey / 100 == key.year * 100 + key.month }
        guard !month.isEmpty else { return nil }
        let avg = Double(month.map(\.mood).reduce(0, +)) / Double(month.count)
        return String(format: "打卡 %d 天 · 平均 %.1f", month.count, avg)
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

    private func setMood(_ value: Int) {
        if let today {
            today.mood = value
            today.date = Date()
        } else {
            let entry = MoodEntry(dayKey: DayKey.today.rawValue, mood: value, note: note)
            entry.tags = Array(tags)
            context.insert(entry)
        }
        try? context.save()
    }

    private func saveNote() {
        guard let today else { return }
        today.note = note
        today.tags = Array(tags).sorted()
        try? context.save()
    }
}
