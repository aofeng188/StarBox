import SwiftUI
import SwiftData

extension View {
    /// 表单行使用半透明磨砂底，透出背后的星空
    func glassRows() -> some View {
        listRowBackground(Rectangle().fill(.ultraThinMaterial))
    }
}

struct ProfileEditorView: View {
    let profile: Profile?
    var isOnboarding = false

    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]

    @State private var name = ""
    @State private var relation = "自己"
    @State private var avatar = "🌟"
    @State private var useLunar = false
    @State private var birthday = DayKey(year: 1998, month: 6, day: 18).date
    @State private var lunarYear = 1998
    @State private var lunarMonth = 5
    @State private var lunarDay = 1
    @State private var lunarLeap = false
    @State private var knowsTime = false
    @State private var birthTime = Calendar.current.date(from: DateComponents(hour: 12, minute: 0)) ?? Date()
    @State private var cityID = "beijing"
    @State private var showDeleteConfirm = false
    /// 从「出生城市」返回时 onAppear 会再触发一次，只在第一次出现时读档案，不能覆盖没保存的修改
    @State private var loaded = false

    static let avatars = ["🌟", "🌙", "☀️", "🪐", "🌸", "🍀", "🔥", "💧", "🦊", "🐱", "🐶", "🐼",
                          "🐰", "🦄", "🐯", "🐲", "🐻", "🐧", "🐳", "🦋", "🍑", "🍓", "🧸", "🎈"]
    static let relations = ["自己", "伴侣", "家人", "朋友", "同事", "其他"]

    var body: some View {
        Form {
            if isOnboarding {
                Section {
                    VStack(spacing: 10) {
                        GradientText(text: "欢迎来到星匣", font: .title.bold())
                        Text("填写生日后，星匣会结合你的八字、星座与每天的干支、月相，计算只属于你的运势。所有数据只保存在本机。")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                }
                .listRowBackground(Color.clear)
            }

            Section("头像与称呼") {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(Self.avatars, id: \.self) { emoji in
                            Button {
                                avatar = emoji
                                Haptics.selection()
                            } label: {
                                Text(emoji)
                                    .font(.system(size: 28))
                                    .frame(width: 46, height: 46)
                                    .background(Circle().fill(avatar == emoji ? palette.accent.opacity(0.3) : .clear))
                                    .overlay(Circle().strokeBorder(avatar == emoji ? palette.accent : .clear, lineWidth: 2))
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("头像 \(emoji)")
                            .accessibilityAddTraits(avatar == emoji ? .isSelected : [])
                        }
                    }
                    .padding(.vertical, 4)
                }
                TextField("名字或昵称", text: $name)
                Picker("关系", selection: $relation) {
                    ForEach(Self.relations, id: \.self) { Text($0) }
                }
            }
            .glassRows()

            Section {
                Picker("历法", selection: $useLunar.animation()) {
                    Text("公历").tag(false)
                    Text("农历").tag(true)
                }
                .pickerStyle(.segmented)

                if useLunar {
                    Picker("年", selection: $lunarYear) {
                        ForEach((1901...DayKey.today.year).reversed(), id: \.self) { year in
                            Text("\(String(year))年（\(GanZhi.ofYear(year).name)\(GanZhi.ofYear(year).branch.animal)）").tag(year)
                        }
                    }
                    Picker("月", selection: $lunarMonth) {
                        ForEach(1...12, id: \.self) { Text(LunarDate.monthNames[$0 - 1] + "月").tag($0) }
                    }
                    if let leap = leapMonth(of: lunarYear), leap == lunarMonth {
                        Toggle("闰\(LunarDate.monthNames[leap - 1])月", isOn: $lunarLeap)
                    }
                    Picker("日", selection: $lunarDay) {
                        ForEach(1...30, id: \.self) { Text(LunarDate.dayNames[$0 - 1]).tag($0) }
                    }
                    if let converted = convertedLunar {
                        LabeledContent("对应公历", value: DateText.full(DayKey(converted)))
                    } else {
                        Label("这一天在农历中不存在，请检查日期", systemImage: "exclamationmark.triangle")
                            .foregroundStyle(.orange)
                            .font(.footnote)
                    }
                } else {
                    DatePicker("生日", selection: $birthday, in: DayKey(year: 1900, month: 1, day: 1).date...Date(), displayedComponents: .date)
                }
            } header: {
                Text("生日")
            }
            .glassRows()

            Section {
                Toggle("知道出生时间", isOn: $knowsTime.animation())
                if knowsTime {
                    DatePicker("出生时间", selection: $birthTime, displayedComponents: .hourAndMinute)
                    Menu {
                        ForEach(0..<12, id: \.self) { index in
                            let branch = EarthlyBranch(index)
                            Button("\(branch.hourName)  \(branch.hourRange)") {
                                let hour = (index * 2) % 24
                                birthTime = Calendar.current.date(from: DateComponents(hour: hour, minute: 0)) ?? birthTime
                            }
                        }
                    } label: {
                        LabeledContent("只记得时辰？", value: "\(currentBranch.hourName) \(currentBranch.hourRange)")
                    }
                    NavigationLink {
                        CityPickerView(selection: $cityID)
                    } label: {
                        LabeledContent("出生城市", value: BirthCity.find(cityID)?.name ?? "北京")
                    }
                }
            } header: {
                Text("出生时间（可选）")
            } footer: {
                Text("填写出生时间后可以排出完整八字（含时柱，按真太阳时校正）以及上升星座。不知道可以先不填，以后随时补充。")
            }
            .glassRows()

            Section("预览") {
                let birth = previewBirth
                let bazi = BaZi(birth)
                LabeledContent("太阳星座", value: ZodiacSign.sunSign(of: birth).name)
                LabeledContent("生肖", value: "\(bazi.zodiacAnimal.animalEmoji) \(bazi.zodiacAnimal.animal)")
                LabeledContent("八字", value: bazi.text)
                if let asc = ZodiacSign.ascendant(of: birth) {
                    LabeledContent("上升星座", value: asc.name)
                }
            }
            .glassRows()

            if let profile, !isOnboarding {
                Section {
                    Button("删除这个档案", role: .destructive) { showDeleteConfirm = true }
                }
                .glassRows()
                .confirmationDialog("删除「\(profile.name)」？", isPresented: $showDeleteConfirm, titleVisibility: .visible) {
                    Button("删除", role: .destructive) { delete(profile) }
                }
            }
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle(isOnboarding ? "创建档案" : (profile == nil ? "新建档案" : "编辑档案"))
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(isOnboarding ? "跳过" : "取消") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("保存", systemImage: "checkmark") { save() }
                    .disabled(useLunar && convertedLunar == nil)
            }
        }
        .onAppear(perform: load)
    }

    private var currentBranch: EarthlyBranch {
        EarthlyBranch.forHour(Calendar.current.component(.hour, from: birthTime))
    }

    private var convertedLunar: Date? {
        LunarDate.solarDate(gregorianYear: lunarYear, month: lunarMonth, day: lunarDay, isLeap: lunarLeap && leapMonth(of: lunarYear) == lunarMonth)
    }

    private func leapMonth(of year: Int) -> Int? {
        (1...12).first { LunarDate.solarDate(gregorianYear: year, month: $0, day: 1, isLeap: true) != nil }
    }

    private var resolvedBirthday: Date {
        if useLunar, let converted = convertedLunar { return DayKey(converted).date }
        return DayKey(birthday).date
    }

    private var previewBirth: BirthInfo {
        let c = Calendar.current.dateComponents([.hour, .minute], from: birthTime)
        return BirthInfo(birthday: resolvedBirthday, hour: knowsTime ? c.hour : nil, minute: knowsTime ? c.minute : nil,
                         cityID: knowsTime ? cityID : nil)
    }

    private func load() {
        guard !loaded else { return }
        loaded = true
        guard let profile else {
            if isOnboarding || profiles.isEmpty { relation = "自己" } else { relation = "朋友" }
            return
        }
        name = profile.name
        relation = profile.relation
        avatar = profile.avatar
        birthday = profile.birthday
        useLunar = profile.enteredAsLunar
        let lunar = LunarDate(profile.birthday)
        lunarMonth = lunar.month
        lunarDay = lunar.day
        lunarLeap = lunar.isLeapMonth
        lunarYear = DayKey(profile.birthday).year - (DayKey(profile.birthday).month < lunar.month ? 1 : 0)
        knowsTime = profile.hasBirthTime
        birthTime = Calendar.current.date(from: DateComponents(hour: profile.birthHour, minute: profile.birthMinute)) ?? birthTime
        cityID = profile.birthCityID ?? "beijing"
    }

    private func save() {
        let target = profile ?? Profile(name: "", birthday: resolvedBirthday)
        target.name = name.trimmingCharacters(in: .whitespaces).isEmpty ? (relation == "自己" ? "我" : relation) : name
        target.relation = relation
        target.avatar = avatar
        target.birthday = resolvedBirthday
        target.enteredAsLunar = useLunar
        target.hasBirthTime = knowsTime
        let c = Calendar.current.dateComponents([.hour, .minute], from: birthTime)
        target.birthHour = c.hour ?? 12
        target.birthMinute = c.minute ?? 0
        target.birthCityID = knowsTime ? cityID : nil
        if profile == nil {
            target.sortIndex = (profiles.map(\.sortIndex).max() ?? -1) + 1
            context.insert(target)
        }
        try? context.save()
        if isOnboarding || app.primary == nil || app.primary?.id == target.id {
            app.setPrimary(target)
        }
        Haptics.success()
        dismiss()
    }

    private func delete(_ profile: Profile) {
        let wasPrimary = app.primary?.id == profile.id
        context.delete(profile)
        try? context.save()
        if wasPrimary { app.setPrimary(profiles.first { $0.id != profile.id }) }
        dismiss()
    }
}

struct CityPickerView: View {
    @Binding var selection: String
    @Environment(\.dismiss) private var dismiss
    @State private var search = ""

    var body: some View {
        List {
            ForEach(BirthCity.regions, id: \.self) { region in
                let cities = BirthCity.all.filter { $0.region == region && (search.isEmpty || $0.name.contains(search)) }
                if !cities.isEmpty {
                    Section(region) {
                        ForEach(cities) { city in
                            Button {
                                selection = city.id
                                dismiss()
                            } label: {
                                HStack {
                                    Text(city.name).foregroundStyle(.primary)
                                    Spacer()
                                    Text(String(format: "%.1f°%@ %.1f°%@", abs(city.latitude), city.latitude >= 0 ? "N" : "S",
                                                abs(city.longitude), city.longitude >= 0 ? "E" : "W"))
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                    if city.id == selection {
                                        Image(systemName: "checkmark").foregroundStyle(.tint)
                                    }
                                }
                            }
                        }
                    }
                    .glassRows()
                }
            }
        }
        .searchable(text: $search, prompt: "搜索城市")
        .cosmicBackground(dimmed: true)
        .navigationTitle("出生城市")
        .navigationBarTitleDisplayMode(.inline)
    }
}
