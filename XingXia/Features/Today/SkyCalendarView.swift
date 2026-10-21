import SwiftUI
import Combine

/// 天象日历：本月每天的月相，未来 60 天的新月、满月、节气、流星雨和日月食，以及天象提醒开关
struct SkyCalendarView: View {
    @Environment(AppSettings.self) private var settings
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.dynamicTypeSize) private var typeSize
    @ScaledMetric(relativeTo: .caption) private var cellHeight: CGFloat = 66
    /// 月份切换按钮跟着字号放大（封顶 1.6 倍）
    @ScaledMetric(relativeTo: .body) private var switcherSize: CGFloat = 34
    /// 选中那天的大月亮跟着字号放大（封顶 1.6 倍）
    @ScaledMetric(relativeTo: .title2) private var detailMoonSize: CGFloat = 64

    @State private var today = DayKey.today
    @State private var month = DayKey(year: DayKey.today.year, month: DayKey.today.month, day: 1)
    @State private var selected = DayKey.today
    @State private var notificationDenied = false

    private var days: [DayKey] {
        let count = Calendar.gregorianLocal.range(of: .day, in: .month, for: month.date)?.count ?? 30
        return (1...count).map { DayKey(year: month.year, month: month.month, day: $0) }
    }

    var body: some View {
        let monthDays = days
        let monthEvents = Dictionary(grouping: SkyEvents.upcoming(from: monthDays[0], days: monthDays.count), by: \.day)
        let upcoming = SkyEvents.upcoming(from: today, days: 60)

        ScrollViewReader { proxy in
            ScrollView {
                VStack(spacing: 16) {
                    monthSwitcher
                        .id("calendar")
                    calendarGrid(monthDays, events: monthEvents)
                    dayDetail(selected, events: monthEvents[selected] ?? [])
                    reminderCard
                    upcomingList(upcoming) { event in
                        select(event.day)
                        withAnimation(reduceMotion ? nil : .snappy) { proxy.scrollTo("calendar", anchor: .top) }
                    }
                }
                .pageContent()
                .padding(.bottom, 30)
            }
        }
        .cosmicBackground()
        .navigationTitle("天象日历")
        .toolbar {
            if month.month != today.month || month.year != today.year || selected != today {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("今天") { select(today) }
                }
            }
        }
        .alert("通知权限未开启", isPresented: $notificationDenied) {
            Button("去设置") {
                if let url = URL(string: UIApplication.openNotificationSettingsURLString) { UIApplication.shared.open(url) }
            }
            Button("好", role: .cancel) {}
        } message: {
            Text("请在系统设置中允许「星匣」发送通知。")
        }
        // 跨过零点、或者隔天从后台回来时刷新「今天」
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
    }

    // MARK: - 月份切换

    private var monthSwitcher: some View {
        let size = min(switcherSize, 34 * 1.6)
        return HStack {
            Button { shift(-1) } label: { Image(systemName: "chevron.left").frame(width: size, height: size) }
                .buttonStyle(.glass)
                .accessibilityLabel("上个月")
            Spacer()
            Text("\(String(month.year))年\(month.month)月").font(.title3.bold())
                .multilineTextAlignment(.center)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            Button { shift(1) } label: { Image(systemName: "chevron.right").frame(width: size, height: size) }
                .buttonStyle(.glass)
                .accessibilityLabel("下个月")
        }
    }

    private func shift(_ delta: Int) {
        let date = Calendar.gregorianLocal.date(byAdding: .month, value: delta, to: month.date) ?? month.date
        let key = DayKey(date)
        withAnimation(reduceMotion ? nil : .snappy) {
            month = DayKey(year: key.year, month: key.month, day: 1)
            selected = month.year == today.year && month.month == today.month ? today : month
        }
    }

    private func select(_ day: DayKey) {
        withAnimation(reduceMotion ? nil : .snappy) {
            month = DayKey(year: day.year, month: day.month, day: 1)
            selected = day
        }
    }

    private func refreshToday() {
        let now = DayKey.today
        guard now != today else { return }
        // 停在「今天」上的话跟着换天
        if selected == today { selected = now }
        if month.year == today.year && month.month == today.month { month = DayKey(year: now.year, month: now.month, day: 1) }
        today = now
    }

    // MARK: - 月相日历

    private func calendarGrid(_ monthDays: [DayKey], events: [DayKey: [SkyEvent]]) -> some View {
        let leading = (monthDays[0].weekday + 6) % 7
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 8) {
            ForEach(["一", "二", "三", "四", "五", "六", "日"], id: \.self) {
                Text($0).font(.caption2).foregroundStyle(.primary.opacity(0.7)).accessibilityHidden(true)
            }
            ForEach((0..<leading).map { -1 - $0 }, id: \.self) { _ in Color.clear.frame(height: min(cellHeight, 96)) }
            ForEach(monthDays, id: \.self) { day in
                let moon = MoonInfo.on(day)
                let dayEvents = events[day] ?? []
                let lunar = LunarDate(day)
                let label = dayEvents.first.map(Self.shortTitle) ?? (lunar.day == 1 ? lunar.monthName : lunar.dayName)
                let spoken: [String] = ["\(day.month)月\(day.day)日", moon.name.name, "亮面 \(moon.percent)%"] + dayEvents.map(\.title)
                Button {
                    selected = day
                    Haptics.selection()
                } label: {
                    SkyDayCell(day: day, moon: moon, label: label, highlighted: !dayEvents.isEmpty,
                               isSelected: day == selected, isToday: day == today, height: min(cellHeight, 96))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(spoken.joined(separator: "，"))
                .accessibilityAddTraits(day == selected ? .isSelected : [])
            }
        }
        .glassCard(padding: 12)
    }

    /// 日历格子里放得下的简称
    private static func shortTitle(_ event: SkyEvent) -> String {
        event.kind == .meteorShower ? "流星雨" : event.title
    }

    // MARK: - 选中那天

    private func dayDetail(_ day: DayKey, events: [SkyEvent]) -> some View {
        let moon = MoonInfo.on(day)
        let lunar = LunarDate(day)
        let nextFull = SkyEvents.nextFullMoon(after: day)
        let nextNew = SkyEvents.nextNewMoon(after: day)
        let fullText = "下次满月 \(DateText.monthDay(nextFull)) · \(Self.relative(nextFull, from: day))"
        let newText = "下次新月 \(DateText.monthDay(nextNew)) · \(Self.relative(nextNew, from: day))"
        let isLarge = typeSize.isAccessibilitySize
        // 超大字号时月亮放上面、文字放下面，文字才有整行的宽度
        let header = isLarge ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12)) : AnyLayout(HStackLayout(spacing: 16))
        return VStack(alignment: .leading, spacing: 14) {
            header {
                MoonDisc(phase: moon.phase, size: min(detailMoonSize, 64 * 1.6))
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(DateText.monthDay(day)) \(DateText.weekday(day)) · 农历\(lunar.text)")
                        .font(.subheadline.weight(.semibold))
                    Text(moon.name.name).font(.title2.bold())
                    Text("亮面 \(moon.percent)% · 月龄 \(String(format: "%.1f", moon.age)) 天")
                        .font(.caption)
                        .subtleForeground()
                }
                if !isLarge { Spacer(minLength: 0) }
            }
            .accessibilityElement(children: .combine)

            if isLarge {
                // 标签不换行，超大字号下会撑出卡片，改成能换行的两行文字
                VStack(alignment: .leading, spacing: 8) {
                    Label(fullText, systemImage: "moonphase.full.moon")
                    Label(newText, systemImage: "moonphase.new.moon")
                }
                .font(.caption.weight(.medium))
                .fixedSize(horizontal: false, vertical: true)
            } else {
                FlowLayout(spacing: 8) {
                    TagChip(text: fullText, symbol: "moonphase.full.moon")
                    TagChip(text: newText, symbol: "moonphase.new.moon")
                }
            }

            ForEach(events) { event in
                VStack(alignment: .leading, spacing: 4) {
                    Label(event.title, systemImage: SkyEventStyle.symbol(event.kind))
                        .font(.subheadline.weight(.semibold))
                    Text(event.detail)
                        .font(.footnote)
                        .subtleForeground()
                        .fixedSize(horizontal: false, vertical: true)
                }
                .accessibilityElement(children: .combine)
            }
            if events.isEmpty {
                Text("月相按当晚 21:00（北京时间）计算。")
                    .font(.caption2)
                    .subtleForeground()
            }
        }
        .glassCard()
    }

    /// 「明天」「3 天后」「2 天前」
    private static func relative(_ day: DayKey, from base: DayKey) -> String {
        let diff = base.days(to: day)
        switch diff {
        case 0: return "当天"
        case 1: return "第二天"
        case 2...: return "\(diff) 天后"
        default: return "\(-diff) 天前"
        }
    }

    // MARK: - 提醒

    private var reminderCard: some View {
        VStack(alignment: .leading, spacing: 8) {
            // 只响应这里的开关操作（不用 onChange）：设置页也有同一个开关，两个页面同时在导航栈里时不会各请求一次授权、各弹一次提示
            Toggle(isOn: Binding(get: { settings.skyReminders }, set: { setSkyReminders($0) })) {
                Label("天象提醒", systemImage: "bell.badge.fill")
            }
            Text("满月、新月和流星雨极大的前一天晚上 8 点提醒你，和每日运势提醒互不影响。")
                .font(.caption)
                .subtleForeground()
        }
        .glassCard()
    }

    private func setSkyReminders(_ on: Bool) {
        settings.skyReminders = on
        Task {
            if on {
                let granted = await NotificationScheduler.requestAuthorization()
                if !granted {
                    settings.skyReminders = false
                    notificationDenied = true
                }
            }
            NotificationScheduler.reschedule()
        }
    }

    // MARK: - 未来 60 天

    private func upcomingList(_ events: [SkyEvent], onSelect: @escaping (SkyEvent) -> Void) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(title: "未来 60 天", subtitle: "共 \(events.count) 个天象", symbol: "sparkles")
            ForEach(Array(events.enumerated()), id: \.element.id) { index, event in
                if index > 0 { Divider().opacity(0.4) }
                Button { onSelect(event) } label: {
                    SkyEventRow(event: event, today: today)
                }
                .buttonStyle(.plain)
                .accessibilityHint("在日历中查看这一天")
            }
            if events.isEmpty {
                Text("最近没有特别的天象。").font(.subheadline).subtleForeground()
            }
        }
        .glassCard()
    }
}

// MARK: - 日历格子

private struct SkyDayCell: View {
    let day: DayKey
    let moon: MoonInfo
    let label: String
    let highlighted: Bool
    let isSelected: Bool
    let isToday: Bool
    let height: CGFloat
    @Environment(\.palette) private var palette

    var body: some View {
        VStack(spacing: 3) {
            Text("\(day.day)")
                .font(.caption.weight(isToday ? .bold : .regular))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .foregroundStyle(isSelected ? palette.onAccent : .primary)
            MoonDisc(phase: moon.phase, size: 20)
            Text(label)
                .font(.caption2.weight(highlighted ? .semibold : .regular))
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .foregroundStyle(isSelected ? palette.onAccent.opacity(0.9) : (highlighted ? palette.ink(palette.accent) : Color.primary.opacity(0.7)))
        }
        .padding(.horizontal, 2)
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(isSelected ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(palette.accent.opacity(highlighted ? 0.16 : 0.06)))
        }
        .overlay {
            if isToday && !isSelected { RoundedRectangle(cornerRadius: 12).strokeBorder(palette.accent, lineWidth: 1.5) }
        }
        .contentShape(.rect)
    }
}

// MARK: - 天象列表的一行

private struct SkyEventRow: View {
    let event: SkyEvent
    let today: DayKey
    @Environment(\.palette) private var palette

    var body: some View {
        let diff = today.days(to: event.day)
        let when = diff == 0 ? "今天" : diff == 1 ? "明天" : "\(diff) 天后"
        HStack(alignment: .top, spacing: 12) {
            ToolIcon(symbol: SkyEventStyle.symbol(event.kind), colors: palette.identity(SkyEventStyle.colors(event.kind)), size: 40)
            VStack(alignment: .leading, spacing: 4) {
                // 超大字号下标题和「几天后」放不进一行时上下排
                ViewThatFits(in: .horizontal) {
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text(event.title).font(.headline)
                        Spacer(minLength: 0)
                        Text(when).font(.caption.weight(.semibold)).subtleForeground()
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(event.title).font(.headline)
                        Text(when).font(.caption.weight(.semibold)).subtleForeground()
                    }
                }
                Text("\(DateText.monthDay(event.day)) \(DateText.weekday(event.day))")
                    .font(.caption)
                    .subtleForeground()
                Text(event.detail)
                    .font(.subheadline)
                    .foregroundStyle(.primary.opacity(0.85))
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

enum SkyEventStyle {
    static func symbol(_ kind: SkyEvent.Kind) -> String {
        switch kind {
        case .newMoon: "moonphase.new.moon"
        case .fullMoon: "moonphase.full.moon"
        case .solarTerm: "sun.max.fill"
        case .meteorShower: "sparkles"
        case .eclipse: "circle.lefthalf.filled"
        }
    }

    static func colors(_ kind: SkyEvent.Kind) -> [Color] {
        switch kind {
        case .newMoon: [Color(hex: 0x6366F1), Color(hex: 0x312E81)]
        case .fullMoon: [Color(hex: 0xFBBF24), Color(hex: 0xD97706)]
        case .solarTerm: [Color(hex: 0x34D399), Color(hex: 0x059669)]
        case .meteorShower: [Color(hex: 0x22D3EE), Color(hex: 0x6366F1)]
        case .eclipse: [Color(hex: 0xA855F7), Color(hex: 0x6D28D9)]
        }
    }
}

// MARK: - 月亮

/// 用 Canvas 画的月亮：亮面比例和朝向按真实月相（在北半球看，上半月右侧亮，下半月左侧亮）
struct MoonDisc: View {
    /// Astronomy.moonPhase 的 0…1，0 新月，0.5 满月
    let phase: Double
    var size: CGFloat = 24
    @Environment(\.palette) private var palette

    var body: some View {
        let lit = palette.isDark ? Color(hex: 0xFFF1C9) : Color(hex: 0xF0B429)
        let unlit = palette.isDark ? Color.white.opacity(0.12) : Color.black.opacity(0.12)
        let rim = palette.isDark ? Color.white.opacity(0.25) : Color.black.opacity(0.2)
        Canvas { context, canvas in
            let radius = min(canvas.width, canvas.height) / 2 - 0.5
            let center = CGPoint(x: canvas.width / 2, y: canvas.height / 2)
            let disc = Path(ellipseIn: CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2))
            context.fill(disc, with: .color(unlit))
            context.fill(Self.litPath(phase: phase, center: center, radius: radius), with: .color(lit))
            context.stroke(disc, with: .color(rim), lineWidth: 0.5)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }

    /// 亮面：亮的一侧的半圆边缘，加上明暗界线（横向半径为 r·cos(2π·phase) 的半椭圆）
    static func litPath(phase: Double, center: CGPoint, radius: CGFloat) -> Path {
        let side: CGFloat = phase < 0.5 ? 1 : -1
        let terminator = CGFloat(cos(2 * Double.pi * phase))
        let steps = 40
        var path = Path()
        // 亮侧边缘：从顶部沿外圆到底部
        for i in 0...steps {
            let t = Double(i) / Double(steps) * Double.pi
            let point = CGPoint(x: center.x + side * radius * CGFloat(sin(t)), y: center.y - radius * CGFloat(cos(t)))
            if i == 0 { path.move(to: point) } else { path.addLine(to: point) }
        }
        // 明暗界线：从底部回到顶部
        for i in 0...steps {
            let t = Double.pi - Double(i) / Double(steps) * Double.pi
            path.addLine(to: CGPoint(x: center.x + side * terminator * radius * CGFloat(sin(t)), y: center.y - radius * CGFloat(cos(t))))
        }
        path.closeSubpath()
        return path
    }
}
