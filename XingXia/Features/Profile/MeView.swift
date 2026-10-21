import SwiftUI
import SwiftData

struct MeView: View {
    @Environment(AppState.self) private var app
    @Environment(ThemeManager.self) private var themes
    @Environment(\.palette) private var palette
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]
    @Query private var records: [DecisionRecord]
    @AppStorage(Backup.lastExportKey) private var lastBackup: Double = 0
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var editing: Profile?
    @State private var creating = false
    /// 头像跟着字号放大（封顶 1.6 倍）
    @ScaledMetric(relativeTo: .largeTitle) private var avatarSize: CGFloat = 76

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                header

                MoonBadgeCard()

                VStack(spacing: 0) {
                    MeRow(route: .profiles, symbol: "person.2.fill", colors: palette.identity([Color(hex: 0x60A5FA), Color(hex: 0x6366F1)]),
                          title: "档案管理", value: "\(profiles.count) 个")
                    Divider().padding(.leading, 58)
                    MeRow(route: .birthChart, symbol: "circle.hexagongrid.fill", colors: palette.identity([Color(hex: 0xA78BFA), Color(hex: 0xEC4899)]),
                          title: "我的命盘", value: "八字 · 星盘")
                }
                .glassCard(padding: 6)

                VStack(spacing: 0) {
                    // 主题这一行本来就是主题自己的颜色，不再混
                    MeRow(route: .themes, symbol: "paintpalette.fill", colors: themes.current.dark.spectrum.prefix(2).map { $0 },
                          title: "主题与外观", value: themes.current.name)
                    Divider().padding(.leading, 58)
                    MeRow(route: .settings, symbol: "gearshape.fill", colors: palette.identity([Color(hex: 0x94A3B8), Color(hex: 0x475569)]),
                          title: "设置", value: "音效 · 震动 · 提醒")
                    Divider().padding(.leading, 58)
                    MeRow(route: .backup, symbol: "externaldrive.fill", colors: palette.identity([Color(hex: 0x2DD4BF), Color(hex: 0x0EA5E9)]),
                          title: "数据备份", value: backupValue)
                }
                .glassCard(padding: 6)

                // 超大字号时两格竖排，数字和说明不被挤断
                let stats = typeSize.isAccessibilitySize ? AnyLayout(VStackLayout(spacing: 12)) : AnyLayout(HStackLayout(spacing: 12))
                stats {
                    StatTile(value: "\(records.count)", label: "次随机决定", symbol: "dice.fill")
                    StatTile(value: "\(daysTogether)", label: "天相伴", symbol: "sparkles")
                }

                Text("星匣 \(Bundle.main.appVersionText) · 所有数据仅保存在你的设备上")
                    .font(.caption)
                    .subtleForeground()
                    .padding(.top, 8)
                    .padding(.bottom, 30)
            }
            .pageContent()
        }
        .cosmicBackground()
        .navigationTitle("我的")
        .sheet(item: $editing) { profile in NavigationStack { ProfileEditorView(profile: profile) } }
        .sheet(isPresented: $creating) { NavigationStack { ProfileEditorView(profile: nil, isOnboarding: profiles.isEmpty) } }
    }

    /// 「我的」里数据备份一行的说明：上次导出是哪天
    private var backupValue: String {
        guard lastBackup > 0 else { return "导出 · 恢复" }
        switch DayKey(Date(timeIntervalSince1970: lastBackup)).days(to: .today) {
        case ...0: return "今天已备份"
        case 1: return "昨天已备份"
        case let days: return "\(days) 天前备份"
        }
    }

    private var daysTogether: Int {
        let first = profiles.map(\.createdAt).min() ?? records.map(\.date).min() ?? Date()
        return DayKey(first).days(to: .today) + 1
    }

    @ViewBuilder
    private var header: some View {
        if let primary = app.primary, let profile = profiles.first(where: { $0.id == primary.id }) {
            let birth = profile.birth
            let bazi = BaZi(birth)
            let avatar = min(avatarSize, 76 * 1.6)
            let isLarge = typeSize.isAccessibilitySize
            // 超大字号时头像放上面，名字、生日和标签占整行，铅笔挪到右上角
            let layout = isLarge ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12))
                                 : AnyLayout(HStackLayout(spacing: 16))
            Button {
                editing = profile
            } label: {
                layout {
                    Text(profile.avatar)
                        .font(.system(size: avatar * 44 / 76))
                        .frame(width: avatar, height: avatar)
                        .background(Circle().fill(palette.accentGradient.opacity(0.35)))
                        .overlay(Circle().strokeBorder(palette.accentGradient, lineWidth: 2))
                        .shadow(color: palette.accent.opacity(0.5), radius: 10)
                    VStack(alignment: .leading, spacing: 6) {
                        Text(profile.name).font(.title2.bold())
                        Text(DateText.full(DayKey(profile.birthday)) + (profile.hasBirthTime ? String(format: " %02d:%02d", profile.birthHour, profile.birthMinute) : ""))
                            .font(.subheadline)
                            .subtleForeground()
                        FlowLayout(spacing: 6) {
                            TagChip(text: profile.sunSign.name, symbol: "sparkle")
                            TagChip(text: "属\(bazi.zodiacAnimal.animal)", color: palette.accentSecondary)
                            TagChip(text: "日主\(bazi.dayMaster.name)\(bazi.dayMaster.element.name)", color: bazi.dayMaster.element.color)
                        }
                    }
                    if !isLarge {
                        Spacer(minLength: 0)
                        Image(systemName: "pencil").subtleForeground()
                            .accessibilityLabel("编辑档案")
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .overlay(alignment: .topTrailing) {
                    if isLarge {
                        Image(systemName: "pencil").subtleForeground()
                            .accessibilityLabel("编辑档案")
                    }
                }
                .glassCard(interactive: true)
            }
            .buttonStyle(.pressableCard)
        } else {
            Button {
                creating = true
            } label: {
                HStack(spacing: 16) {
                    GlowIcon(symbol: "person.crop.circle.badge.plus", color: palette.accent, secondary: palette.accentSecondary, size: 64)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("创建我的档案").font(.title3.bold())
                        Text("填写生日，开启专属运势").font(.subheadline).subtleForeground()
                    }
                    Spacer()
                }
                .glassCard(interactive: true)
            }
            .buttonStyle(.pressableCard)
        }
    }
}

/// 「我的」里的入口：点进去时页面从左边的图标放大出来
private struct MeRow: View {
    let route: AppRoute
    let symbol: String
    let colors: [Color]
    let title: String
    let value: String

    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        NavigationLink(value: route) {
            HStack(spacing: 14) {
                ToolIcon(symbol: symbol, colors: colors, size: 34)
                    .zoomSource(route)
                // 超大字号时说明换到标题下面，两段文字不挤在一行
                if typeSize.isAccessibilitySize {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title).foregroundStyle(.primary)
                        Text(value).font(.subheadline).subtleForeground()
                    }
                    Spacer(minLength: 0)
                } else {
                    Text(title).foregroundStyle(.primary)
                    Spacer()
                    Text(value).font(.subheadline).subtleForeground()
                }
                Image(systemName: "chevron.right").font(.caption.weight(.semibold)).foregroundStyle(.tertiary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 12)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
    }
}

struct StatTile: View {
    let value: String
    let label: String
    let symbol: String
    @Environment(\.palette) private var palette
    /// 数字跟着字号放大（封顶 1.6 倍）
    @ScaledMetric(relativeTo: .title) private var valueSize: CGFloat = 28

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: symbol).foregroundStyle(palette.accentGradient)
            Text(value)
                .font(.system(size: min(valueSize, 28 * 1.6), weight: .bold, design: .rounded))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.6)
            Text(label).font(.caption).subtleForeground()
        }
        .glassCard(padding: 16)
    }
}

extension Bundle {
    /// 「1.0.3 (12)」：营销版本号，附带构建号
    var appVersionText: String {
        let version = object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
        let build = object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? ""
        if version.isEmpty { return build }
        return build.isEmpty || build == version ? version : "\(version) (\(build))"
    }
}

extension WuXing {
    var color: Color {
        switch self {
        case .wood: Color(hex: 0x4ADE80)
        case .fire: Color(hex: 0xF87171)
        case .earth: Color(hex: 0xE0A84A)
        case .metal: Color(hex: 0xD4AF37)
        case .water: Color(hex: 0x60A5FA)
        }
    }
}

// MARK: - 档案列表

struct ProfileListView: View {
    @Environment(\.modelContext) private var context
    @Environment(AppState.self) private var app
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]
    @State private var editing: Profile?
    @State private var creating = false
    /// 头像底圈跟着字号放大（封顶 1.6 倍），大号表情不溢出
    @ScaledMetric(relativeTo: .title) private var avatarSize: CGFloat = 48

    var body: some View {
        let avatar = min(avatarSize, 48 * 1.6)
        List {
            Section {
                ForEach(profiles) { profile in
                    HStack(spacing: 14) {
                        Text(profile.avatar)
                            .font(.title)
                            .frame(width: avatar, height: avatar)
                            .background(Circle().fill(.tint.opacity(0.18)))
                        VStack(alignment: .leading, spacing: 3) {
                            HStack {
                                Text(profile.name).font(.headline)
                                Text(profile.relation).font(.caption).subtleForeground()
                            }
                            Text("\(profile.sunSign.name) · 属\(profile.animal.animal) · \(DateText.full(DayKey(profile.birthday)))")
                                .font(.caption)
                                .subtleForeground()
                        }
                        Spacer()
                        if app.primary?.id == profile.id {
                            Image(systemName: "checkmark.circle.fill").foregroundStyle(.tint)
                                .accessibilityLabel("当前档案")
                        }
                    }
                    .contentShape(.rect)
                    .onTapGesture { editing = profile }
                    .swipeActions(edge: .leading) {
                        Button("设为当前", systemImage: "checkmark") { app.setPrimary(profile) }
                            .tint(.accentColor)
                    }
                }
                .onMove(perform: move)
            } footer: {
                Text("点按编辑，向右轻扫设为「当前查看」。首页和小组件会显示当前档案的运势。")
            }
            .glassRows()
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle("档案管理")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("添加", systemImage: "plus") { creating = true }
            }
            ToolbarItem(placement: .topBarTrailing) { EditButton() }
        }
        .overlay {
            if profiles.isEmpty {
                EmptyHint(symbol: "person.2", title: "还没有档案", message: "添加自己、家人和朋友的生日，随时切换查看运势")
            }
        }
        .sheet(item: $editing) { profile in NavigationStack { ProfileEditorView(profile: profile) } }
        .sheet(isPresented: $creating) { NavigationStack { ProfileEditorView(profile: nil, isOnboarding: profiles.isEmpty) } }
    }

    private func move(from: IndexSet, to: Int) {
        var ordered = profiles
        ordered.move(fromOffsets: from, toOffset: to)
        for (index, profile) in ordered.enumerated() { profile.sortIndex = index }
        try? context.save()
    }
}
