import SwiftUI
import SwiftData
import WidgetKit

struct CountdownListView: View {
    @Environment(\.modelContext) private var context
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette
    @Query(sort: \Countdown.createdAt) private var countdowns: [Countdown]
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]
    @State private var editing: Countdown?
    @State private var viewingBirthday: CountdownItem?
    @State private var creating = false

    var body: some View {
        let today = DayKey.today
        let items = CountdownItem.sorted(CountdownItem.all(countdowns, profiles: profiles,
                                                           includeProfiles: settings.profileBirthdaysInCountdowns), today: today)
        ScrollView {
            VStack(spacing: 14) {
                if let hero = items.first {
                    CountdownHero(item: hero, today: today)
                        .onTapGesture { open(hero) }
                        .accessibilityAddTraits(.isButton)
                        .accessibilityAction { open(hero) }
                        .contextMenu { menu(for: hero) }
                }
                ForEach(items.dropFirst()) { item in
                    CountdownRow(item: item, today: today)
                        .onTapGesture { open(item) }
                        .accessibilityAddTraits(.isButton)
                        .accessibilityAction { open(item) }
                        .contextMenu { menu(for: item) }
                }
                Button {
                    creating = true
                } label: {
                    Label("添加倒数日", systemImage: "plus.circle.fill").frame(maxWidth: .infinity).padding(.vertical, 6)
                }
                .buttonStyle(.glass)
                if items.isEmpty {
                    EmptyHint(symbol: "hourglass", title: "还没有倒数日", message: "生日、纪念日、假期、考试……重要的日子都可以记在这里")
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("倒数日")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Toggle(isOn: Binding(get: { settings.profileBirthdaysInCountdowns },
                                         set: { settings.profileBirthdaysInCountdowns = $0 })) {
                        Label("显示档案里的生日", systemImage: "person.crop.circle.badge.clock")
                    }
                } label: {
                    Label("显示选项", systemImage: "ellipsis.circle")
                }
            }
            ToolbarItem(placement: .topBarTrailing) { Button("添加", systemImage: "plus") { creating = true } }
        }
        .sheet(item: $editing) { item in NavigationStack { CountdownEditor(countdown: item) } }
        .sheet(item: $viewingBirthday) { item in NavigationStack { ProfileBirthdayDetail(item: item) } }
        .sheet(isPresented: $creating) { NavigationStack { CountdownEditor(countdown: nil) } }
    }

    /// 数据库里的倒数日打开编辑；档案生日只能查看
    private func open(_ item: CountdownItem) {
        if item.isFromProfile {
            viewingBirthday = item
        } else {
            editing = countdowns.first { $0.id == item.id }
        }
    }

    @ViewBuilder
    private func menu(for item: CountdownItem) -> some View {
        if item.isFromProfile {
            Button("查看", systemImage: "info.circle") { viewingBirthday = item }
        } else if let countdown = countdowns.first(where: { $0.id == item.id }) {
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
}

private struct CountdownHero: View {
    let item: CountdownItem
    let today: DayKey
    @Environment(\.palette) private var palette

    var body: some View {
        let status = item.status(today: today)
        let color = palette.color(at: item.colorIndex)
        // 色块装饰用原色，文字和数字用调过明暗的版本，浅色模式下粉彩色也读得清
        let ink = palette.inkColor(at: item.colorIndex)
        VStack(spacing: 10) {
            HStack {
                Text(item.emoji).font(.title)
                Text(item.title).font(.title3.bold())
                if item.pinned { Image(systemName: "pin.fill").font(.caption).foregroundStyle(.secondary).accessibilityLabel("已置顶") }
                Spacer()
                CountdownSourceChip(item: item, color: color)
            }
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(status.prefix).font(.title3).foregroundStyle(.secondary)
                if status.isToday && item.kind != .anniversary {
                    Text(status.suffix).font(.system(size: 64, weight: .heavy, design: .rounded)).foregroundStyle(ink)
                } else {
                    Text("\(status.days)")
                        .font(.system(size: 84, weight: .heavy, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(LinearGradient(colors: [ink, ink.opacity(0.75)], startPoint: .top, endPoint: .bottom))
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
    let item: CountdownItem
    let today: DayKey
    @Environment(\.palette) private var palette

    var body: some View {
        let status = item.status(today: today)
        let color = palette.color(at: item.colorIndex)
        let ink = palette.inkColor(at: item.colorIndex)
        HStack(spacing: 14) {
            Text(item.emoji)
                .font(.title2)
                .frame(width: 48, height: 48)
                .background(Circle().fill(color.opacity(0.18)))
            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 4) {
                    Text(item.title).font(.headline)
                    if item.pinned { Image(systemName: "pin.fill").font(.caption2).foregroundStyle(.secondary).accessibilityLabel("已置顶") }
                }
                Text(status.caption).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                if item.isFromProfile {
                    CountdownSourceChip(item: item, color: color)
                }
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 0) {
                Text(status.prefix).font(.caption2).foregroundStyle(.secondary)
                if status.isToday && item.kind != .anniversary {
                    Text(status.suffix).font(.title3.bold()).foregroundStyle(ink)
                } else {
                    HStack(alignment: .firstTextBaseline, spacing: 2) {
                        Text("\(status.days)").font(.system(size: 30, weight: .bold, design: .rounded)).monospacedDigit().foregroundStyle(ink)
                        Text(status.suffix).font(.caption).foregroundStyle(.secondary)
                    }
                }
            }
        }
        .glassCard(padding: 14, interactive: true)
        .accessibilityElement(children: .combine)
    }
}

/// 类型标签；档案生日标「来自档案」
private struct CountdownSourceChip: View {
    let item: CountdownItem
    let color: Color

    var body: some View {
        if item.isFromProfile {
            TagChip(text: "来自档案", color: color, symbol: "person.crop.circle")
        } else {
            TagChip(text: item.kind.name, color: color, symbol: item.kind.symbol)
        }
    }
}

/// 档案生日的详情：不能编辑删除，生日要在档案里改
private struct ProfileBirthdayDetail: View {
    let item: CountdownItem
    @Environment(AppState.self) private var app
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]

    var body: some View {
        let today = DayKey.today
        let status = item.status(today: today)
        let profile = profiles.first { $0.id == item.profileID }
        let birth = item.spec.origin
        let zodiac: String = profile.map { "\($0.animal.animalEmoji) 属\($0.animal.animal)" } ?? "—"
        ScrollView {
            VStack(spacing: 16) {
                CountdownHero(item: item, today: today)

                VStack(alignment: .leading, spacing: 12) {
                    if let profile {
                        LabeledContent("档案") { Text("\(profile.avatar) \(profile.name)") }
                    }
                    LabeledContent("公历生日") { Text(DateText.full(birth)) }
                    if item.spec.isLunar {
                        let lunar = LunarDate(birth)
                        LabeledContent("农历生日") { Text("\(lunar.monthName)\(lunar.dayName)") }
                    }
                    LabeledContent("下一次") {
                        Text("\(DateText.full(status.targetDay)) \(DateText.weekday(status.targetDay))")
                    }
                    LabeledContent("生肖") { Text(zodiac) }
                }
                .font(.subheadline)
                .glassCard()

                VStack(alignment: .leading, spacing: 12) {
                    Text(item.spec.isLunar ? "这一天按档案里的农历生日每年自动换算。" : "这一天由档案里的生日自动生成，每年重复。")
                    Text("想修改生日请到「我的 › 档案」；不想在倒数日里看到档案生日，可以在「设置」或倒数日页右上角关掉。")
                        .subtleForeground()
                }
                .font(.footnote)
                .frame(maxWidth: .infinity, alignment: .leading)
                .glassCard()

                Button {
                    dismiss()
                    app.open(.profiles)
                } label: {
                    Label("管理档案", systemImage: "person.2").frame(maxWidth: .infinity).padding(.vertical, 6)
                }
                .buttonStyle(.glass)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle(item.title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("完成", systemImage: "checkmark") { dismiss() }
            }
        }
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
    /// 农历生日是否记录出生年份（打开才显示年龄）
    @State private var recordsBirthYear = false
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
                            .accessibilityLabel("图标 \(item)")
                            .accessibilityAddTraits(emoji == item ? .isSelected : [])
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
                        Toggle("记录出生年份", isOn: $recordsBirthYear.animation())
                        if recordsBirthYear {
                            DatePicker("公历出生日期", selection: $date, in: ...Date(), displayedComponents: .date)
                        }
                    }
                    if let preview = lunarPreview {
                        LabeledContent {
                            Text("\(DateText.full(preview.day)) \(DateText.weekday(preview.day))")
                        } label: {
                            Text(preview.label)
                        }
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
                HStack(spacing: 10) {
                    ForEach(0..<6, id: \.self) { index in
                        let fill = palette.color(at: index)
                        let isSelected = colorIndex == index
                        Button { colorIndex = index } label: {
                            // 选中：外圈描边 + 对勾，浅色粉彩上也看得出来
                            Circle().fill(fill).frame(width: 30, height: 30)
                                .overlay {
                                    if isSelected {
                                        Image(systemName: "checkmark").font(.caption.weight(.bold)).foregroundStyle(fill.readableForeground)
                                    }
                                }
                                .padding(3)
                                .overlay(Circle().strokeBorder(.primary, lineWidth: isSelected ? 2 : 0))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("颜色 \(index + 1)，\(Self.colorName(fill))")
                        .accessibilityAddTraits(isSelected ? .isSelected : [])
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
        case .countdown:
            isLunar ? "不重复时倒数到最近一次的这个农历日子；勾选「每年重复」可以用于农历节日。" : "倒数到目标日期还有多少天；勾选「每年重复」可以用于节日。"
        case .anniversary: "从开始日期起已经过了多少天，可以每年提醒周年。"
        case .birthday:
            isLunar ? "农历生日每年自动重复；打开「记录出生年份」才会显示年龄。" : "生日每年自动重复，支持农历生日。"
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
        // 只有原本就是生日时 date 才是出生日期；倒数日改成生日时 date 是目标日，不能当出生年份
        recordsBirthYear = countdown.kindValue == .birthday && countdown.knowsBirthYear
        colorIndex = countdown.colorIndex
        pinned = countdown.pinned
    }

    private func save() {
        let lunar = isLunar && kind != .anniversary
        // 在改动模型之前算好，lunarTargetDay 要和原来的设置比较
        let lunarTarget = lunar && kind == .countdown && !repeatsYearly ? lunarTargetDay() : nil
        let target = countdown ?? Countdown(title: title, emoji: emoji, date: date, kind: kind.rawValue)
        target.title = title.trimmingCharacters(in: .whitespaces)
        target.emoji = emoji
        target.kind = kind.rawValue
        if let lunarTarget {
            // 不重复的农历倒数日：存换算出的公历日期，列表、首页和小组件都以它为准
            target.date = lunarTarget.date
        } else if lunar && kind == .birthday && !recordsBirthYear {
            // 没记录出生年份：存创建当天，表示「未设置」，不显示年龄
            target.date = DayKey(target.createdAt).date
        } else {
            target.date = DayKey(date).date
        }
        target.isLunar = lunar
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

    /// 不重复的农历倒数日对应的公历日期：农历月日没改就沿用原来的目标日，否则从今天起找最近的一次
    private func lunarTargetDay() -> DayKey {
        if let countdown, countdown.isLunar, countdown.kindValue == .countdown, !countdown.repeatsYearly,
           countdown.lunarMonth == lunarMonth, countdown.lunarDay == lunarDay {
            return countdown.nextOccurrence()
        }
        return Countdown.lunarOccurrence(month: lunarMonth, day: lunarDay, onOrAfter: .today) ?? .today
    }

    /// 农历日子换算出的公历日期，显示在表单里
    private var lunarPreview: (label: String, day: DayKey)? {
        guard isLunar && kind != .anniversary else { return nil }
        if kind == .countdown && !repeatsYearly { return ("对应公历", lunarTargetDay()) }
        guard let next = Countdown.lunarOccurrence(month: lunarMonth, day: lunarDay, onOrAfter: .today) else { return nil }
        return ("下一次", next)
    }

    /// 读屏用的颜色名称，按色相粗分
    private static func colorName(_ color: Color) -> String {
        var hue: CGFloat = 0, saturation: CGFloat = 0, brightness: CGFloat = 0, alpha: CGFloat = 0
        UIColor(color).getHue(&hue, saturation: &saturation, brightness: &brightness, alpha: &alpha)
        if saturation < 0.15 { return brightness < 0.4 ? "深灰" : "灰色" }
        switch hue * 360 {
        case ..<15, 345...: return "红色"
        case ..<40: return "橙色"
        case ..<70: return brightness < 0.7 ? "金色" : "黄色"
        case ..<165: return "绿色"
        case ..<195: return "青色"
        case ..<245: return "蓝色"
        case ..<290: return "紫色"
        default: return "粉色"
        }
    }
}
