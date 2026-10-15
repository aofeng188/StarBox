import SwiftUI

struct ThemeSettingsView: View {
    @Environment(ThemeManager.self) private var themes
    @Environment(\.colorScheme) private var scheme
    @Environment(\.palette) private var palette

    private let columns = [GridItem(.adaptive(minimum: 140), spacing: 12)]

    var body: some View {
        @Bindable var themes = themes
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                // 当前主题
                VStack(alignment: .leading, spacing: 12) {
                    ThemePreviewTile(theme: themes.current, scheme: scheme, isSelected: false, height: 130)
                    HStack {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("当前：\(themes.current.name)").font(.headline)
                            Text(themes.reason).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        if themes.mode != .fixed {
                            Button("换一套", systemImage: "shuffle") {
                                themes.mode = .launchRandom
                                themes.shuffleNow()
                                Haptics.impact()
                            }
                            .buttonStyle(.glass)
                        }
                    }
                }
                .glassCard()

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: "外观", symbol: "circle.lefthalf.filled")
                    Picker("外观", selection: $themes.appearance) {
                        ForEach(AppearanceMode.allCases) { Label($0.name, systemImage: $0.symbol).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    Text("每套主题都有深色「星空」与浅色「晨曦」两个版本。")
                        .font(.caption).foregroundStyle(.secondary)
                }
                .glassCard()

                VStack(alignment: .leading, spacing: 4) {
                    SectionHeader(title: "切换规则", symbol: "wand.and.stars")
                        .padding(.bottom, 8)
                    ForEach(ThemeRuleMode.allCases) { mode in
                        Button {
                            withAnimation(.snappy) { themes.mode = mode }
                            Haptics.selection()
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: mode.symbol)
                                    .frame(width: 30, height: 30)
                                    .foregroundStyle(themes.mode == mode ? palette.onAccent : palette.accent)
                                    .background(Circle().fill(themes.mode == mode ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(palette.accent.opacity(0.12))))
                                    .accessibilityHidden(true)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(mode.name).foregroundStyle(.primary)
                                    Text(mode.detail).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                // 选中状态由 isSelected 朗读，对勾不再读出符号名
                                if themes.mode == mode {
                                    Image(systemName: "checkmark").foregroundStyle(palette.accent).fontWeight(.semibold)
                                        .accessibilityHidden(true)
                                }
                            }
                            .padding(.vertical, 8)
                            .contentShape(.rect)
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(themes.mode == mode ? .isSelected : [])
                    }
                }
                .glassCard()

                modeDetail

                VStack(spacing: 4) {
                    Toggle(isOn: $themes.lowPowerOverride) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("低电量时切换为「黑洞」")
                            Text("开启低电量模式后自动使用最省电的纯黑主题").font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    Divider().padding(.vertical, 6)
                    Toggle(isOn: $themes.festivalEggs) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("节日彩蛋")
                            Text("春节、中秋、七夕、圣诞等节日自动换上应景主题").font(.caption).foregroundStyle(.secondary)
                        }
                    }
                }
                .glassCard()
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("主题与外观")
    }

    @ViewBuilder
    private var modeDetail: some View {
        @Bindable var themes = themes
        switch themes.mode {
        case .fixed:
            VStack(alignment: .leading, spacing: 12) {
                SectionHeader(title: "选择主题", symbol: "paintpalette")
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(AppTheme.all) { theme in
                        Button {
                            themes.fixedThemeID = theme.id
                            Haptics.selection()
                        } label: {
                            ThemePreviewTile(theme: theme, scheme: scheme, isSelected: themes.fixedThemeID == theme.id)
                        }
                        .buttonStyle(.pressableCard)
                    }
                }
            }
            .glassCard()
        case .dailyRandom, .launchRandom:
            VStack(alignment: .leading, spacing: 12) {
                SectionHeader(title: "随机池", subtitle: "只会从勾选的主题里抽", symbol: "checklist")
                LazyVGrid(columns: columns, spacing: 12) {
                    ForEach(AppTheme.all) { theme in
                        let included = themes.randomPool.contains(theme.id)
                        Button {
                            if included {
                                if themes.randomPool.count > 1 { themes.randomPool.removeAll { $0 == theme.id } }
                            } else {
                                themes.randomPool.append(theme.id)
                            }
                            Haptics.selection()
                        } label: {
                            ThemePreviewTile(theme: theme, scheme: scheme, isSelected: included, checkmarkStyle: true)
                        }
                        .buttonStyle(.pressableCard)
                    }
                }
            }
            .glassCard()
        case .timeOfDay:
            VStack(spacing: 0) {
                ForEach(TimeSlot.allCases) { slot in
                    themeMenuRow(symbol: slot.symbol, title: slot.name, subtitle: slot.range,
                                 selection: Binding(get: { themes.timeSlotThemes[safe: slot.rawValue] ?? "nebula" },
                                                    set: { themes.timeSlotThemes[slot.rawValue] = $0 }))
                    if slot != .night { Divider() }
                }
            }
            .glassCard()
        case .weekday:
            VStack(spacing: 0) {
                ForEach(0..<7, id: \.self) { index in
                    themeMenuRow(symbol: "\(index + 1).circle", title: ["周一", "周二", "周三", "周四", "周五", "周六", "周日"][index], subtitle: nil,
                                 selection: Binding(get: { themes.weekdayThemes[safe: index] ?? "nebula" },
                                                    set: { themes.weekdayThemes[index] = $0 }))
                    if index < 6 { Divider() }
                }
            }
            .glassCard()
        case .luckyColor:
            mappingCard(title: "五行与主题", rows: WuXing.allCases.map { ("\($0.name) · \($0.luckyColors[0].name)", AppTheme.forElement($0)) })
        case .zodiac:
            mappingCard(title: "星座元素与主题", rows: ZodiacElement.allCases.map { ("\($0.name)星座", AppTheme.forZodiac($0)) })
        }
    }

    private func themeMenuRow(symbol: String, title: String, subtitle: String?, selection: Binding<String>) -> some View {
        HStack {
            Image(systemName: symbol).frame(width: 28).foregroundStyle(palette.accent)
            VStack(alignment: .leading) {
                Text(title)
                if let subtitle { Text(subtitle).font(.caption).foregroundStyle(.secondary) }
            }
            Spacer()
            Picker(title, selection: selection) {
                ForEach(AppTheme.all) { Text($0.name).tag($0.id) }
            }
            .labelsHidden()
        }
        .padding(.vertical, 6)
    }

    private func mappingCard(title: String, rows: [(String, AppTheme)]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: title, symbol: "arrow.triangle.branch")
            ForEach(rows, id: \.0) { row in
                HStack {
                    Text(row.0)
                    Spacer()
                    Circle().fill(row.1.palette(for: scheme).accentGradient).frame(width: 14, height: 14)
                    Text(row.1.name).foregroundStyle(.secondary)
                }
                .font(.subheadline)
            }
        }
        .glassCard()
    }
}

/// 静态的主题缩略图
struct ThemePreviewTile: View {
    let theme: AppTheme
    let scheme: ColorScheme
    let isSelected: Bool
    var checkmarkStyle = false
    var height: CGFloat = 96

    var body: some View {
        let p = theme.palette(for: scheme)
        ZStack(alignment: .bottomLeading) {
            p.backgroundGradient
            Circle().fill(p.nebula[0].opacity(0.7)).frame(width: height).blur(radius: 24).offset(x: -20, y: -height * 0.3)
            Circle().fill(p.nebula[1].opacity(0.6)).frame(width: height * 0.8).blur(radius: 22).offset(x: height * 0.9, y: -height * 0.1)
            Circle().fill(p.nebula[2].opacity(0.5)).frame(width: height * 0.6).blur(radius: 18).offset(x: height * 0.4, y: height * 0.2)
            Canvas { context, size in
                var rng = SeededGenerator(theme.id)
                for _ in 0..<26 {
                    let r = rng.double(0.5...1.6)
                    let rect = CGRect(x: rng.unit() * size.width, y: rng.unit() * size.height, width: r * 2, height: r * 2)
                    context.fill(Path(ellipseIn: rect), with: .color(p.star.opacity(p.isDark ? rng.double(0.4...1) : rng.double(0.2...0.5))))
                }
            }
            HStack(spacing: 6) {
                Image(systemName: theme.symbol)
                Text(theme.name).font(.subheadline.weight(.semibold))
            }
            .foregroundStyle(p.isDark ? .white : .black.opacity(0.75))
            .padding(10)
        }
        .frame(height: height)
        .frame(maxWidth: .infinity)
        .clipShape(.rect(cornerRadius: 18))
        .overlay {
            RoundedRectangle(cornerRadius: 18)
                .strokeBorder(isSelected ? AnyShapeStyle(p.accentGradient) : AnyShapeStyle(.white.opacity(0.15)), lineWidth: isSelected ? 3 : 1)
        }
        .overlay(alignment: .topTrailing) {
            if checkmarkStyle || isSelected {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(isSelected ? p.accent : .white.opacity(0.6))
                    .background(Circle().fill(.black.opacity(0.15)))
                    .padding(8)
            }
        }
        .shadow(color: isSelected ? p.accent.opacity(0.4) : .clear, radius: 8)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(theme.name + (isSelected ? "，已选中" : ""))
    }
}
