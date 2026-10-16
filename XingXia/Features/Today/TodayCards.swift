import SwiftUI
import SwiftData

// MARK: - 今日运势

struct FortuneHeroCard: View {
    let fortune: DailyFortune
    @Environment(\.palette) private var palette
    @Environment(\.dynamicTypeSize) private var typeSize
    // 分数环（连同环里的分数）和等级字跟随系统字号放大，设上限免得撑爆卡片
    @ScaledMetric(relativeTo: .title) private var ringSize: CGFloat = 112
    @ScaledMetric(relativeTo: .largeTitle) private var levelSize: CGFloat = 34

    var body: some View {
        let isLarge = typeSize.isAccessibilitySize
        let ring = min(ringSize, 160)
        // 超大字号时分数环放上面、文字放下面，摘要才有足够宽度
        let header = isLarge ? AnyLayout(VStackLayout(alignment: .leading, spacing: 14))
                             : AnyLayout(HStackLayout(alignment: .center, spacing: 18))
        NavigationLink(value: AppRoute.fortune) {
            VStack(alignment: .leading, spacing: 16) {
                header {
                    ScoreRing(score: fortune.score, size: ring, lineWidth: ring * 11 / 112, label: "运势指数")
                    VStack(alignment: .leading, spacing: 6) {
                        Text(fortune.isPersonalized ? "专属运势" : "今日运势")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                        GradientText(text: fortune.levelName, font: .system(size: min(levelSize, 48), weight: .heavy, design: .rounded))
                        StarRatingView(stars: fortune.stars, size: 13)
                        Text(fortune.summary)
                            .font(.subheadline)
                            .foregroundStyle(.primary.opacity(0.85))
                            .lineLimit(typeSize >= .xLarge ? nil : 3)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                Grid(horizontalSpacing: 14, verticalSpacing: 10) {
                    GridRow {
                        ForEach(FortuneCategory.allCases.prefix(2)) { category in
                            CategoryMeter(category: category, value: fortune.score(category))
                        }
                    }
                    GridRow {
                        ForEach(FortuneCategory.allCases.suffix(2)) { category in
                            CategoryMeter(category: category, value: fortune.score(category))
                        }
                    }
                }

                Divider().opacity(0.4)

                // 超大字号时四项排成两行，免得挤成一团
                if isLarge {
                    VStack(spacing: 12) {
                        HStack(spacing: 0) { luckyColorItem; luckyNumberItem }
                        HStack(spacing: 0) { luckyDirectionItem; luckyHourItem }
                    }
                } else {
                    HStack(spacing: 0) {
                        luckyColorItem
                        luckyNumberItem
                        luckyDirectionItem
                        luckyHourItem
                    }
                }
            }
            .glassCard(interactive: true)
        }
        .buttonStyle(.pressableCard)
    }

    private var luckyColorItem: some View {
        LuckyItem(title: "幸运色") {
            HStack(spacing: 5) {
                Circle()
                    .fill(Color(fortune.luckyColor))
                    .frame(width: 14, height: 14)
                    .overlay(Circle().strokeBorder(.white.opacity(0.5), lineWidth: 1))
                    .shadow(color: Color(fortune.luckyColor).opacity(0.8), radius: 4)
                Text(fortune.luckyColor.name)
            }
        }
    }

    private var luckyNumberItem: some View {
        LuckyItem(title: "幸运数字") { Text("\(fortune.luckyNumber)").monospacedDigit() }
    }

    private var luckyDirectionItem: some View {
        LuckyItem(title: "喜神方位") { Text(fortune.luckyDirection) }
    }

    private var luckyHourItem: some View {
        LuckyItem(title: "吉时") { Text(fortune.luckyHour.hourName) }
    }
}

struct CategoryMeter: View {
    let category: FortuneCategory
    let value: Int
    @Environment(\.palette) private var palette

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack(spacing: 4) {
                Image(systemName: category.symbol).font(.caption2)
                Text(category.shortName).font(.caption)
                Spacer()
                Text("\(value)").font(.caption.weight(.semibold)).monospacedDigit()
            }
            .foregroundStyle(.secondary)
            MeterBar(value: value, height: 5)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(category.name) \(value) 分")
    }
}

private struct LuckyItem<Content: View>: View {
    let title: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(spacing: 4) {
            Text(title).font(.caption2).foregroundStyle(.secondary)
            content.font(.footnote.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - 家人今日

/// 每个档案今天的运势分数；和当天相冲的生肖标出来。只有一个档案时不显示。
struct FamilyFortuneCard: View {
    let today: DayKey
    @Environment(AppState.self) private var app
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]
    @State private var switching: Profile?

    var body: some View {
        if profiles.count > 1 {
            let clash = AlmanacDay(today).clashAnimal
            VStack(alignment: .leading, spacing: 12) {
                SectionHeader(title: "家人今日", subtitle: "今日冲\(clash.animal)", symbol: "person.2.fill")
                CardHScroll {
                    HStack(spacing: 10) {
                        ForEach(profiles) { profile in
                            let isCurrent = profile.id == app.primary?.id
                            let tile = FamilyMemberTile(profile: profile, fortune: FortuneEngine.daily(for: profile.birth, on: today),
                                                        clashes: profile.animal == clash, isCurrent: isCurrent)
                            if isCurrent {
                                // 当前档案：直接看运势详情
                                NavigationLink(value: AppRoute.fortune) { tile }
                                    .buttonStyle(.pressableCard)
                                    .accessibilityHint("查看运势详情")
                            } else {
                                Button { switching = profile } label: { tile }
                                    .buttonStyle(.pressableCard)
                                    .accessibilityHint("切换为当前档案")
                            }
                        }
                    }
                    .padding(.vertical, 2)
                }
            }
            .glassCard(clipped: true)
            .confirmationDialog("切换当前档案？",
                                isPresented: Binding(get: { switching != nil }, set: { if !$0 { switching = nil } }),
                                titleVisibility: .visible, presenting: switching) { profile in
                Button("切换到「\(profile.name)」") {
                    app.setPrimary(profile)
                    Haptics.selection()
                    Announce.post("已切换到\(profile.name)")
                }
                Button("取消", role: .cancel) {}
            } message: { profile in
                Text("首页、每日提醒和小组件会改为显示「\(profile.name)」的运势。")
            }
        }
    }
}

private struct FamilyMemberTile: View {
    let profile: Profile
    let fortune: DailyFortune
    let clashes: Bool
    let isCurrent: Bool
    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let color = palette.color(at: profile.colorIndex)
        let animal = profile.animal.animal
        VStack(spacing: 6) {
            Text(profile.avatar)
                .font(.title2)
                .frame(width: 46, height: 46)
                .background(Circle().fill(color.opacity(0.2)))
                .overlay(Circle().strokeBorder(isCurrent ? palette.accent : .clear, lineWidth: 2))
                .overlay(alignment: .bottomTrailing) {
                    if isCurrent {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.caption)
                            .foregroundStyle(palette.onAccent, palette.accent)
                            .offset(x: 3, y: 3)
                    }
                }
            Text(profile.name)
                .font(.caption.weight(.semibold))
                .foregroundStyle(.primary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text("\(fortune.score)")
                    .font(.system(.title3, design: .rounded).weight(.bold))
                    .monospacedDigit()
                    .foregroundStyle(palette.inkColor(at: profile.colorIndex))
                Text(fortune.levelName).font(.caption2).subtleForeground()
            }
            if clashes {
                TagChip(text: "今日冲\(animal)", color: Color.caution(scheme))
            }
        }
        .frame(minWidth: 84)
        .padding(.vertical, 10)
        .padding(.horizontal, 8)
        .background(color.opacity(0.1), in: .rect(cornerRadius: 18))
        .contentShape(.rect(cornerRadius: 18))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(profile.name)，今日运势 \(fortune.score) 分，\(fortune.levelName)"
                            + (clashes ? "，今日冲\(animal)" : "") + (isCurrent ? "，当前查看" : ""))
    }
}

// MARK: - 宜忌

struct AlmanacCard: View {
    let fortune: DailyFortune
    let almanac: AlmanacDay

    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        @Bindable var settings = settings
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                SectionHeader(title: settings.almanacShowsTraditional ? "传统黄历" : "今日宜忌",
                              subtitle: settings.almanacShowsTraditional ? "\(almanac.jianChu.name) · \(almanac.deity.name)" : nil,
                              symbol: "book.closed.fill")
                Picker("宜忌样式", selection: $settings.almanacShowsTraditional) {
                    Text("趣味").tag(false)
                    Text("黄历").tag(true)
                }
                .pickerStyle(.segmented)
                .frame(width: 112)
            }

            NavigationLink(value: AppRoute.almanac) {
                VStack(alignment: .leading, spacing: 12) {
                    YiJiRow(isYi: true, items: settings.almanacShowsTraditional ? almanac.yi : fortune.funYi)
                    YiJiRow(isYi: false, items: settings.almanacShowsTraditional ? almanac.ji : fortune.funJi)
                    if settings.almanacShowsTraditional {
                        if !almanac.shensha.isEmpty {
                            // 和黄历页一致：吉神金色，四绝、四离灰色
                            FlowLayout(spacing: 6) {
                                ForEach(almanac.shensha, id: \.self) { name in
                                    let isLucky = Shensha.isLucky(name)
                                    TagChip(text: name, color: isLucky ? Color.gold(scheme) : Color(hex: 0x94A3B8),
                                            symbol: isLucky ? "sparkle" : "exclamationmark.triangle")
                                }
                            }
                            .accessibilityElement(children: .ignore)
                            .accessibilityLabel("神煞：" + almanac.shensha.joined(separator: "、"))
                        }
                        HStack(spacing: 12) {
                            Label(almanac.clash, systemImage: "arrow.left.arrow.right")
                            Label("财神\(almanac.wealthDirection)", systemImage: "location.north.fill")
                        }
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    }
                }
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
        }
        .glassCard()
        .animation(.snappy, value: settings.almanacShowsTraditional)
    }
}

struct YiJiRow: View {
    let isYi: Bool
    let items: [String]
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        // 语义色按深浅模式取色，徽章上的字按底色深浅选黑或白，保证对比度
        let color = isYi ? Color.positive(scheme) : Color.negative(scheme)
        HStack(alignment: .top, spacing: 12) {
            Text(isYi ? "宜" : "忌")
                .font(AppFont.serif(18, weight: .bold))
                .foregroundStyle(color.readableForeground)
                .frame(width: 34, height: 34)
                .background(Circle().fill(color.gradient))
                .shadow(color: color.opacity(0.6), radius: 6)
            FlowLayout(spacing: 8) {
                ForEach(items, id: \.self) { item in
                    Text(item)
                        .font(.subheadline.weight(.medium))
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(color.opacity(0.13), in: .capsule)
                }
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel((isYi ? "宜：" : "忌：") + items.joined(separator: "、"))
    }
}

// MARK: - 星座

struct HoroscopeMiniCard: View {
    let today: DayKey
    @Environment(AppState.self) private var app
    @AppStorage("lastViewedSign", store: AppGroup.defaults) private var lastViewedSign = -1
    @Environment(\.palette) private var palette

    var body: some View {
        let sign = currentSign
        let horoscope = HoroscopeEngine.horoscope(sign, period: .today, reference: today)
        NavigationLink(value: AppRoute.horoscope) {
            HStack(spacing: 16) {
                // 旁边已有星座名，徽章不再重复朗读
                ZodiacBadge(sign: sign, size: 64)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 5) {
                    HStack {
                        Text(sign.name).font(.headline)
                        Text(sign.dateRange).font(.caption).foregroundStyle(.secondary)
                    }
                    StarRatingView(stars: horoscope.stars, size: 12)
                    Text(horoscope.comment)
                        .font(.subheadline)
                        .foregroundStyle(.primary.opacity(0.85))
                    // 星座幸运色和运势卡的幸运色是两套算法，首页只留运势卡那个，避免两个幸运色打架
                    TagChip(text: "速配 \(horoscope.matchSign.shortName)", symbol: "heart.fill")
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
            }
            .glassCard(interactive: true)
        }
        .buttonStyle(.pressableCard)
    }

    private var currentSign: ZodiacSign {
        if let birth = app.birth { return ZodiacSign.sunSign(of: birth) }
        return ZodiacSign(rawValue: lastViewedSign) ?? .aries
    }
}

/// 发光的星座徽章
struct ZodiacBadge: View {
    let sign: ZodiacSign
    var size: CGFloat = 64
    @Environment(\.palette) private var palette

    var body: some View {
        ZStack {
            Circle()
                .fill(RadialGradient(colors: [palette.accent.opacity(0.55), palette.accentSecondary.opacity(0.25), .clear],
                                     center: .center, startRadius: 0, endRadius: size * 0.6))
            Circle()
                .strokeBorder(AngularGradient(colors: [palette.accent, palette.accentSecondary, palette.accent], center: .center),
                              lineWidth: 1.5)
            Text(sign.glyph)
                .font(.system(size: size * 0.46, weight: .medium))
                .foregroundStyle(palette.accentGradient)
                .shadow(color: palette.accent, radius: 8)
        }
        .frame(width: size, height: size)
        .accessibilityLabel(sign.name)
    }
}

// MARK: - 今日一签

struct StickMiniCard: View {
    let today: DayKey
    @Environment(\.colorScheme) private var scheme
    @Environment(AppState.self) private var app
    @Query(sort: \StickDraw.date, order: .reverse) private var draws: [StickDraw]
    @Environment(\.palette) private var palette

    var body: some View {
        // 只认当前档案今天的签：给家人朋友求的签记在他们自己的档案下
        let profileID = app.primary?.id
        let draw = draws.first { $0.dayKey == today.rawValue && $0.profileID == profileID }
        NavigationLink(value: AppRoute.stick) {
            HStack(spacing: 16) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(LinearGradient(colors: [Color(hex: 0xB91C1C), Color(hex: 0x7F1D1D)], startPoint: .top, endPoint: .bottom))
                    Image(systemName: "flame.fill")
                        .font(.title2)
                        .foregroundStyle(LinearGradient(colors: [Color(hex: 0xFFD166), Color(hex: 0xFF8A3D)], startPoint: .top, endPoint: .bottom))
                }
                .frame(width: 56, height: 64)
                .shadow(color: Color(hex: 0xE11D48).opacity(0.5), radius: 8)

                if let draw {
                    let stick = StickLibrary.stick(draw.stickNumber)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("今日已求签").font(.caption).foregroundStyle(.secondary)
                        HStack(spacing: 8) {
                            Text(stick.numberText).font(AppFont.serif(17, weight: .bold))
                            Text(stick.levelInfo.displayName)
                                .font(.caption.weight(.bold))
                                .padding(.horizontal, 8).padding(.vertical, 3)
                                .background(stick.levelInfo.color.opacity(0.25), in: .capsule)
                                .foregroundStyle(stick.levelInfo.ink(scheme))
                        }
                        Text("「\(stick.title)」\(stick.poem[0])").font(.subheadline).lineLimit(1).foregroundStyle(.secondary)
                    }
                } else {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("今日一签").font(.headline)
                        Text("心诚则灵，摇一摇签筒，看看今天的指引").font(.subheadline).foregroundStyle(.secondary)
                    }
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
            }
            .glassCard(interactive: true)
        }
        .buttonStyle(.pressableCard)
    }
}

// MARK: - 快捷工具

struct QuickToolsRow: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette
    @State private var showEditor = false

    var body: some View {
        let tools = settings.favoriteTools.compactMap(FavoriteTool.init(id:))
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "快捷工具", subtitle: "工具页右上角点星标收藏", symbol: "bolt.fill")
            CardHScroll {
                HStack(spacing: 14) {
                    ForEach(tools) { tool in
                        NavigationLink(value: tool.route) {
                            VStack(spacing: 6) {
                                ToolIcon(symbol: tool.symbol, colors: tool.colors, size: 54)
                                Text(tool.name)
                                    .font(.caption2)
                                    .foregroundStyle(.primary)
                                    .lineLimit(1)
                            }
                            .frame(width: 66)
                        }
                        .buttonStyle(.pressableCard)
                    }
                    Button {
                        showEditor = true
                    } label: {
                        VStack(spacing: 6) {
                            RoundedRectangle(cornerRadius: 54 * 0.3, style: .continuous)
                                .strokeBorder(palette.accent.opacity(0.6), style: StrokeStyle(lineWidth: 1.5, dash: [5, 4]))
                                .frame(width: 54, height: 54)
                                .overlay {
                                    Image(systemName: "plus")
                                        .font(.title2.weight(.semibold))
                                        .foregroundStyle(palette.ink(palette.accent))
                                }
                            Text("添加")
                                .font(.caption2)
                                .subtleForeground()
                                .lineLimit(1)
                        }
                        .frame(width: 66)
                        .contentShape(.rect)
                    }
                    .buttonStyle(.pressableCard)
                    .accessibilityLabel("管理快捷工具")
                }
                .padding(.vertical, 4)
            }
        }
        .glassCard(clipped: true)
        .sheet(isPresented: $showEditor) { FavoriteToolsEditor() }
    }
}

// MARK: - 倒数日

struct CountdownStrip: View {
    let today: DayKey
    @Environment(AppSettings.self) private var settings
    @Query(sort: \Countdown.createdAt) private var countdowns: [Countdown]
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]
    @Environment(\.palette) private var palette

    var body: some View {
        let items = CountdownItem.sorted(CountdownItem.all(countdowns, profiles: profiles,
                                                           includeProfiles: settings.profileBirthdaysInCountdowns), today: today)
        let pinned = items.filter(\.pinned)
        // 有置顶时只放置顶的，外加一周内要到的档案生日；没有置顶时放最近的三个
        let shown = pinned.isEmpty ? Array(items.prefix(3))
            : pinned + items.filter { $0.isFromProfile && $0.status(today: today).days <= 7 }
        VStack(alignment: .leading, spacing: 12) {
            NavigationLink(value: AppRoute.countdowns) {
                HStack {
                    SectionHeader(title: "倒数日", symbol: "hourglass")
                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
                }
            }
            .buttonStyle(.plain)
            if shown.isEmpty {
                NavigationLink(value: AppRoute.countdowns) {
                    Label("添加生日、纪念日或假期倒计时", systemImage: "plus.circle.fill")
                        .font(.subheadline)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                }
                .buttonStyle(.glass)
            } else {
                CardHScroll {
                    HStack(spacing: 12) {
                        ForEach(shown) { item in
                            CountdownChip(item: item, today: today)
                        }
                    }
                }
            }
        }
        .glassCard(clipped: true)
    }
}

struct CountdownChip: View {
    let item: CountdownItem
    let today: DayKey
    @Environment(\.palette) private var palette
    @ScaledMetric(relativeTo: .title) private var numberSize: CGFloat = 30

    var body: some View {
        let status = item.status(today: today)
        // 底色、描边用原色；文字用 ink 版本，浅色模式下粉彩色才看得清
        let color = palette.color(at: item.colorIndex)
        let ink = palette.inkColor(at: item.colorIndex)
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Text(item.emoji)
                Text(item.title).font(.subheadline.weight(.semibold)).lineLimit(1)
                if item.isFromProfile {
                    Image(systemName: "person.crop.circle")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .accessibilityLabel("来自档案")
                }
            }
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text(status.prefix).font(.caption2).foregroundStyle(.secondary)
                if status.isToday && item.kind != .anniversary {
                    Text(status.suffix).font(.title2.bold()).foregroundStyle(ink)
                } else {
                    Text("\(status.days)")
                        .font(.system(size: min(numberSize, 48), weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(ink)
                    Text(status.suffix).font(.caption2).foregroundStyle(.secondary)
                }
            }
        }
        .padding(14)
        .frame(minWidth: 130, alignment: .leading)
        .background(color.opacity(0.12), in: .rect(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).strokeBorder(color.opacity(0.35), lineWidth: 1))
        .accessibilityElement(children: .combine)
    }
}

// MARK: - 心情

struct MoodCheckInCard: View {
    /// 由父视图传入并在零点、回到前台时刷新，跨天后不会把心情记到昨天
    let today: DayKey
    @Environment(\.modelContext) private var context
    @Query(sort: \MoodEntry.date, order: .reverse) private var moods: [MoodEntry]

    var body: some View {
        let key = today.rawValue
        let entry = moods.first { $0.dayKey == key }
        VStack(alignment: .leading, spacing: 14) {
            NavigationLink(value: AppRoute.mood) {
                HStack {
                    SectionHeader(title: entry == nil ? "今天心情如何？" : "今日心情", symbol: "face.smiling")
                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
                }
            }
            .buttonStyle(.plain)
            MoodPicker(selected: entry?.mood) { mood in
                // 点下时重新查一次，不用 body 里可能已过期的 entry
                if let current = moods.first(where: { $0.dayKey == key }) {
                    current.mood = mood.rawValue
                    current.date = Date()
                } else {
                    context.insert(MoodEntry(dayKey: key, mood: mood.rawValue))
                }
                try? context.save()
            }
        }
        .glassCard()
    }
}

// MARK: - 每日一句

struct QuoteCard: View {
    let quote: String
    @Environment(\.palette) private var palette

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: "quote.opening")
                .font(.title3)
                .foregroundStyle(palette.accentGradient)
            Text(quote)
                .font(AppFont.serif(19))
                .multilineTextAlignment(.center)
                .lineSpacing(5)
            Text("— 星匣 · 每日一句")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .glassCard(padding: 22)
        .contextMenu {
            Button("拷贝", systemImage: "doc.on.doc") { UIPasteboard.general.string = quote }
            ShareLink(item: quote)
        }
    }
}

// MARK: - 自动换行布局

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        var x: CGFloat = 0
        var y: CGFloat = 0
        var rowHeight: CGFloat = 0
        var maxX: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > 0 && x + size.width > width {
                x = 0
                y += rowHeight + spacing
                rowHeight = 0
            }
            x += size.width + spacing
            maxX = max(maxX, x - spacing)
            rowHeight = max(rowHeight, size.height)
        }
        return CGSize(width: proposal.width ?? maxX, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX && x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
