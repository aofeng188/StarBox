import SwiftUI
import SwiftData

// MARK: - 今日运势

struct FortuneHeroCard: View {
    let fortune: DailyFortune
    @Environment(\.palette) private var palette

    var body: some View {
        NavigationLink(value: AppRoute.fortune) {
            VStack(alignment: .leading, spacing: 16) {
                HStack(alignment: .center, spacing: 18) {
                    ScoreRing(score: fortune.score, size: 112, lineWidth: 11, label: "运势指数")
                    VStack(alignment: .leading, spacing: 6) {
                        Text(fortune.isPersonalized ? "专属运势" : "今日运势")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(.secondary)
                        GradientText(text: fortune.levelName, font: .system(size: 34, weight: .heavy, design: .rounded))
                        StarRatingView(stars: fortune.stars, size: 13)
                        Text(fortune.summary)
                            .font(.subheadline)
                            .foregroundStyle(.primary.opacity(0.85))
                            .lineLimit(3)
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

                HStack(spacing: 0) {
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
                    LuckyItem(title: "幸运数字") { Text("\(fortune.luckyNumber)").monospacedDigit() }
                    LuckyItem(title: "喜神方位") { Text(fortune.luckyDirection) }
                    LuckyItem(title: "吉时") { Text(fortune.luckyHour.hourName) }
                }
            }
            .glassCard(interactive: true)
        }
        .buttonStyle(.pressableCard)
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

// MARK: - 宜忌

struct AlmanacCard: View {
    let fortune: DailyFortune
    let almanac: AlmanacDay

    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette

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

    var body: some View {
        let color = isYi ? Color(hex: 0x34D399) : Color(hex: 0xFB7185)
        HStack(alignment: .top, spacing: 12) {
            Text(isYi ? "宜" : "忌")
                .font(AppFont.serif(18, weight: .bold))
                .foregroundStyle(.white)
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
    @Environment(AppState.self) private var app
    @AppStorage("lastViewedSign", store: AppGroup.defaults) private var lastViewedSign = -1
    @Environment(\.palette) private var palette

    var body: some View {
        let sign = currentSign
        let horoscope = HoroscopeEngine.horoscope(sign, period: .today)
        NavigationLink(value: AppRoute.horoscope) {
            HStack(spacing: 16) {
                ZodiacBadge(sign: sign, size: 64)
                VStack(alignment: .leading, spacing: 5) {
                    HStack {
                        Text(sign.name).font(.headline)
                        Text(sign.dateRange).font(.caption).foregroundStyle(.secondary)
                    }
                    StarRatingView(stars: horoscope.stars, size: 12)
                    Text(horoscope.comment)
                        .font(.subheadline)
                        .foregroundStyle(.primary.opacity(0.85))
                    HStack(spacing: 6) {
                        TagChip(text: "速配 \(horoscope.matchSign.shortName)", symbol: "heart.fill")
                        TagChip(text: horoscope.luckyColor.name, color: Color(horoscope.luckyColor), symbol: "paintpalette.fill")
                    }
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
    @Environment(AppState.self) private var app
    @Query(sort: \StickDraw.date, order: .reverse) private var draws: [StickDraw]
    @Environment(\.palette) private var palette

    var body: some View {
        let today = DayKey.today.rawValue
        let draw = draws.first { $0.dayKey == today && $0.profileID == app.primary?.id }
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
                                .foregroundStyle(stick.levelInfo.color)
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

    var body: some View {
        let tools = settings.favoriteTools.compactMap(ToolKind.init(rawValue:))
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "快捷工具", subtitle: "在百宝箱长按工具可收藏", symbol: "bolt.fill")
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 14) {
                    ForEach(tools) { tool in
                        NavigationLink(value: AppRoute.tool(tool)) {
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
                    NavigationLink(value: AppRoute.answers) {
                        VStack(spacing: 6) {
                            ToolIcon(symbol: "book.fill", colors: [Color(hex: 0xF5B83D), Color(hex: 0xB45309)], size: 54)
                            Text("答案之书").font(.caption2).foregroundStyle(.primary)
                        }
                        .frame(width: 66)
                    }
                    .buttonStyle(.pressableCard)
                }
                .padding(.vertical, 4)
            }
            .scrollClipDisabled()
        }
        .glassCard()
    }
}

// MARK: - 倒数日

struct CountdownStrip: View {
    @Query(sort: \Countdown.createdAt) private var countdowns: [Countdown]
    @Environment(\.palette) private var palette

    var body: some View {
        let pinned = countdowns.filter(\.pinned)
        let shown = pinned.isEmpty ? Array(countdowns.prefix(3)) : pinned
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
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 12) {
                        ForEach(shown) { countdown in
                            CountdownChip(countdown: countdown)
                        }
                    }
                }
                .scrollClipDisabled()
            }
        }
        .glassCard()
    }
}

struct CountdownChip: View {
    let countdown: Countdown
    @Environment(\.palette) private var palette

    var body: some View {
        let status = countdown.status()
        let color = palette.color(at: countdown.colorIndex)
        VStack(alignment: .leading, spacing: 6) {
            HStack(spacing: 6) {
                Text(countdown.emoji)
                Text(countdown.title).font(.subheadline.weight(.semibold)).lineLimit(1)
            }
            HStack(alignment: .firstTextBaseline, spacing: 3) {
                Text(status.prefix).font(.caption2).foregroundStyle(.secondary)
                if status.isToday && countdown.kindValue != .anniversary {
                    Text(status.suffix).font(.title2.bold()).foregroundStyle(color)
                } else {
                    Text("\(status.days)")
                        .font(.system(size: 30, weight: .bold, design: .rounded))
                        .monospacedDigit()
                        .foregroundStyle(color)
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
    @Environment(\.modelContext) private var context
    @Query(sort: \MoodEntry.date, order: .reverse) private var moods: [MoodEntry]

    var body: some View {
        let today = DayKey.today.rawValue
        let entry = moods.first { $0.dayKey == today }
        VStack(alignment: .leading, spacing: 14) {
            NavigationLink(value: AppRoute.mood) {
                HStack {
                    SectionHeader(title: entry == nil ? "今天心情如何？" : "今日心情", symbol: "face.smiling")
                    Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
                }
            }
            .buttonStyle(.plain)
            MoodPicker(selected: entry?.mood) { mood in
                if let entry {
                    entry.mood = mood.rawValue
                    entry.date = Date()
                } else {
                    context.insert(MoodEntry(dayKey: today, mood: mood.rawValue))
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
