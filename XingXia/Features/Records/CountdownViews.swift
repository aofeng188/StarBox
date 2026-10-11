import SwiftUI
import SwiftData
import WidgetKit

struct CountdownListView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Query(sort: \Countdown.createdAt) private var countdowns: [Countdown]
    @State private var editing: Countdown?
    @State private var creating = false

    private var sorted: [Countdown] {
        countdowns.sorted { a, b in
            if a.pinned != b.pinned { return a.pinned }
            let sa = a.status(), sb = b.status()
            let ka = a.kindValue == .anniversary || sa.prefix == "已过" ? 10_000 + sa.days : sa.days
            let kb = b.kindValue == .anniversary || sb.prefix == "已过" ? 10_000 + sb.days : sb.days
            return ka < kb
        }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                if let hero = sorted.first {
                    CountdownHero(countdown: hero)
                        .onTapGesture { editing = hero }
                }
                ForEach(sorted.dropFirst()) { countdown in
                    CountdownRow(countdown: countdown)
                        .onTapGesture { editing = countdown }
                        .contextMenu { menu(for: countdown) }
                }
                Button {
                    creating = true
                } label: {
                    Label("添加倒数日", systemImage: "plus.circle.fill").frame(maxWidth: .infinity).padding(.vertical, 6)
                }
                .buttonStyle(.glass)
                if countdowns.isEmpty {
                    EmptyHint(symbol: "hourglass", title: "还没有倒数日", message: "生日、纪念日、假期、考试……重要的日子都可以记在这里")
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("倒数日")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) { Button("添加", systemImage: "plus") { creating = true } }
        }
        .sheet(item: $editing) { item in NavigationStack { CountdownEditor(countdown: item) } }
        .sheet(isPresented: $creating) { NavigationStack { CountdownEditor(countdown: nil) } }
    }

    @ViewBuilder
    private func menu(for countdown: Countdown) -> some View {
        Button(countdown.pinned ? "取消置顶" : "置顶到首页", systemImage: countdown.pinned ? "pin.slash" : "pin") {
            countdown.pinned.toggle()
            try? context.save()
            WidgetCenter.shared.reloadAllTimelines()
        }
        Button("编辑", systemImage: "pencil") { editing = countdown }
        Button("删除", systemImage: "trash", role: .destructive) {
            context.delete(countdown)
            try? context.save()
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}

private struct CountdownHero: View {
    let countdown: Countdown
    @Environment(\.palette) private var palette

    var body: some View {
        let status = countdown.status()
        let color = palette.color(at: countdown.colorIndex)
        VStack(spacing: 10) {
            HStack {
                Text(countdown.emoji).font(.title)
                Text(countdown.title).font(.title3.bold())
                if countdown.pinned { Image(systemName: "pin.fill").font(.caption).foregroundStyle(.secondary) }
                Spacer()
                TagChip(text: countdown.kindValue.name, color: color, symbol: countdown.kindValue.symbol)
            }
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(status.prefix).font(.title3).foregroundStyle(.secondary)
                if status.isToday && countdown.kindValue != .anniversary {
                    Text(status.suffix).font(.system(size: 64, weight: .heavy, design: .rounded)).foregroundStyle(color)
                } else {
                    Text("\(status.days)")
                        .font(.system(size: 84, weight: .heavy, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(LinearGradient(colors: [color, color.opacity(0.6)], startPoint: .top, endPoint: .bottom))
                        .shadow(color: color.opacity(0.5), radius: 16)
                    Text(status.suffix).font(.title3).foregroundStyle(.secondary)
                }
            }
            Text(status.caption).font(.subheadline).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .glassCard(padding: 22, tint: color.opacity(0.1))
        .accessibilityElement(children: .combine)
    }
}

private struct CountdownRow: View {
    let countdown: Countdown
    @Environment(\.palette) private var palette

    var body: some View {
        let status = countdown.status()
        let color = palette.color(at: countdown.colorIndex)
        HStack(spacing: 14) {
            Text(countdown.emoji)
                .font(.title2)
                .frame(width: 48, height: 48)
                .background(Circle().fill(color.opacity(0.18)))
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 4) {
                    Text(countdown.title).font(.headline)
                    if countdown.pinned { Image(systemName: "pin.fill").font(.caption2).foregroundStyle(.secondary) }
                }
                Text(status.caption).font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 0) {
                Text(status.prefix).font(.caption2).foregroundStyle(.secondary)
                if status.isToday && countdown.kindValue != .anniversary {
                    Text(status.suffix).font(.title3.bold()).foregroundStyle(color)
                } else {
                    HStack(alignment: .firstTextBaseline, spacing: 2) {
                        Text("\(status.days)").font(.system(size: 30, weight: .bold, design: .rounded)).monospacedDigit().foregroundStyle(color)
                        Text(status.suffix).font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
        }
        .glassCard(padding: 14, interactive: true)
        .accessibilityElement(children: .combine)
    }
}

struct CountdownEditor: View {
    let countdown: Countdown?
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Environment(\.palette) private var palette

    @State private var title = ""
    @State private var emoji = "🎉"
    @State private var kind: CountdownKind = .countdown
    @State private var date = Date()
    @State private var isLunar = false
    @State private var lunarMonth = 1
    @State private var lunarDay = 1
    @State private var repeatsYearly = false
    @State private var colorIndex = 0
    @State private var pinned = false
    @State private var confirmDelete = false

    private let emojis = ["🎉", "🎂", "💝", "✈️", "🏖️", "🎓", "📚", "💼", "🎄", "🧧", "🎆", "🌙", "🏃", "💍", "👶", "🐾", "🏠", "🎤", "⚽️", "📝"]

    var body: some View {
        Form {
            Section {
                TextField("名称，如「妈妈生日」「去日本旅行」", text: $title)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(emojis, id: \.self) { item in
                            Button { emoji = item } label: {
                                Text(item).font(.title2).padding(6)
                                    .background(Circle().fill(emoji == item ? palette.accent.opacity(0.3) : .clear))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                }
                Picker("类型", selection: $kind) {
                    ForEach(CountdownKind.allCases) { Text($0.name).tag($0) }
                }
                .pickerStyle(.segmented)
            }
            .glassRows()

            Section {
                if kind != .anniversary {
                    Toggle("按农历", isOn: $isLunar.animation())
                }
                if isLunar && kind != .anniversary {
                    Picker("农历月", selection: $lunarMonth) {
                        ForEach(1...12, id: \.self) { Text(LunarDate.monthNames[$0 - 1] + "月").tag($0) }
                    }
                    Picker("农历日", selection: $lunarDay) {
                        ForEach(1...30, id: \.self) { Text(LunarDate.dayNames[$0 - 1]).tag($0) }
                    }
                    if kind == .birthday {
                        DatePicker("出生年份参考", selection: $date, displayedComponents: .date)
                    }
                } else {
                    DatePicker(kind == .birthday ? "生日" : (kind == .anniversary ? "开始日期" : "目标日期"), selection: $date, displayedComponents: .date)
                }
                if kind != .birthday {
                    Toggle(kind == .anniversary ? "每年提醒周年" : "每年重复", isOn: $repeatsYearly)
                }
            } header: {
                Text("日期")
            } footer: {
                Text(footer)
            }
            .glassRows()

            Section("样式") {
                HStack(spacing: 12) {
                    ForEach(0..<6, id: \.self) { index in
                        Button { colorIndex = index } label: {
                            Circle().fill(palette.color(at: index)).frame(width: 30, height: 30)
                                .overlay(Circle().strokeBorder(.white, lineWidth: colorIndex == index ? 3 : 0))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("颜色 \(index + 1)")
                    }
                }
                Toggle("置顶到首页", isOn: $pinned)
            }
            .glassRows()

            if countdown != nil {
                Section {
                    Button("删除", role: .destructive) { confirmDelete = true }
                }
                .glassRows()
            }
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle(countdown == nil ? "新建倒数日" : "编辑倒数日")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
            ToolbarItem(placement: .confirmationAction) {
                Button("保存", systemImage: "checkmark") { save() }
                    .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
            }
        }
        .confirmationDialog("删除这个倒数日？", isPresented: $confirmDelete, titleVisibility: .visible) {
            Button("删除", role: .destructive) {
                if let countdown { context.delete(countdown) }
                try? context.save()
                WidgetCenter.shared.reloadAllTimelines()
                dismiss()
            }
        }
        .onAppear(perform: load)
    }

    private var footer: String {
        switch kind {
        case .countdown: "倒数到目标日期还有多少天；勾选「每年重复」可以用于节日。"
        case .anniversary: "从开始日期起已经过了多少天，可以每年提醒周年。"
        case .birthday: "生日每年自动重复，支持农历生日。"
        }
    }

    private func load() {
        guard let countdown else { return }
        title = countdown.title
        emoji = countdown.emoji
        kind = countdown.kindValue
        date = countdown.date
        isLunar = countdown.isLunar
        lunarMonth = countdown.lunarMonth
        lunarDay = countdown.lunarDay
        repeatsYearly = countdown.repeatsYearly
        colorIndex = countdown.colorIndex
        pinned = countdown.pinned
    }

    private func save() {
        let target = countdown ?? Countdown(title: title, emoji: emoji, date: date, kind: kind.rawValue)
        target.title = title.trimmingCharacters(in: .whitespaces)
        target.emoji = emoji
        target.kind = kind.rawValue
        target.date = DayKey(date).date
        target.isLunar = isLunar && kind != .anniversary
        target.lunarMonth = lunarMonth
        target.lunarDay = lunarDay
        target.repeatsYearly = kind == .birthday ? true : repeatsYearly
        target.colorIndex = colorIndex
        target.pinned = pinned
        if countdown == nil { context.insert(target) }
        try? context.save()
        WidgetCenter.shared.reloadAllTimelines()
        Haptics.success()
        dismiss()
    }
}
