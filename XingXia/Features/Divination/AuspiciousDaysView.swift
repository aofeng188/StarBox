import SwiftUI
import SwiftData
import WidgetKit
import Combine

/// 择吉日：选一件事，按黄历列出未来一段时间里办这件事的好日子，可以一键加进倒数日
struct AuspiciousDaysView: View {
    @Environment(AppState.self) private var app
    @Environment(\.modelContext) private var context
    @Environment(\.scenePhase) private var scenePhase
    @Query private var countdowns: [Countdown]
    /// 上次选的事项和范围（占卜首页的入口卡片也读 "auspiciousEvent"）
    @AppStorage("auspiciousEvent") private var eventID = AuspiciousEvent.moving.rawValue
    @AppStorage("auspiciousRange") private var storedRange = AuspiciousDays.defaultRange
    @State private var weekendsOnly = false
    @State private var today = DayKey.today
    @State private var found: SearchResult?

    private var event: AuspiciousEvent { AuspiciousEvent(rawValue: eventID) ?? .moving }

    private var range: Int { AuspiciousDays.rangeOptions.contains(storedRange) ? storedRange : AuspiciousDays.defaultRange }

    /// 当前档案的生肖（年柱地支，以立春为界）
    private var animal: EarthlyBranch? { app.primary.map { BaZi($0.birth).zodiacAnimal } }

    var body: some View {
        let key = SearchKey(event: event, days: range, weekendsOnly: weekendsOnly, today: today, animal: animal)
        ScrollView {
            VStack(spacing: 16) {
                eventPicker
                options(animal: key.animal)
                results(for: key)
                Text("按建除十二神、黄黑道值神、天德月德等神煞和黄历宜忌推算，仅作传统文化参考。")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("择吉日")
        .task(id: key) { await search(key) }
        // 跨过零点、或者隔天从后台回来时，从新的一天重新算
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
    }

    // MARK: - 选事项

    private var eventPicker: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeader(title: "想办什么事", subtitle: event.subtitle, symbol: "calendar.badge.checkmark")
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 58, maximum: 96), spacing: 8)], spacing: 14) {
                ForEach(AuspiciousEvent.allCases) { item in
                    EventTile(event: item, isSelected: item == event) {
                        guard item != event else { return }
                        eventID = item.rawValue
                        Haptics.selection()
                    }
                }
            }
        }
        .glassCard()
    }

    private func options(animal: EarthlyBranch?) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Picker("查找范围", selection: Binding(get: { range }, set: { storedRange = $0 })) {
                ForEach(AuspiciousDays.rangeOptions, id: \.self) { days in
                    Text("未来 \(days) 天").tag(days)
                }
            }
            .pickerStyle(.segmented)
            Toggle(isOn: $weekendsOnly) {
                Label("只看周末", systemImage: "sun.max.fill")
            }
            Group {
                if let animal {
                    let name = app.primary?.name ?? ""
                    Text("按\(name.isEmpty ? "你" : name)的生肖（属\(animal.animal)）避开冲日：冲日会排到后面并标出。")
                } else {
                    Text("还没有档案。添加档案后，会帮你避开冲你生肖的日子。")
                }
            }
            .font(.footnote)
            .subtleForeground()
            .fixedSize(horizontal: false, vertical: true)
        }
        .glassCard()
    }

    // MARK: - 结果

    @ViewBuilder
    private func results(for key: SearchKey) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "宜\(key.event.name)的日子",
                          subtitle: "未来 \(key.days) 天" + (key.weekendsOnly ? " · 只看周末" : ""),
                          symbol: key.event.symbol)
                .padding(.horizontal, 4)
            if let found {
                let isCurrent = found.key == key
                if found.days.isEmpty {
                    EmptyHint(symbol: "calendar.badge.exclamationmark", title: "这段时间没有合适的日子",
                              message: Self.emptyMessage(for: found.key))
                        .glassCard()
                        .opacity(isCurrent ? 1 : 0.4)
                } else {
                    ForEach(Array(found.days.enumerated()), id: \.element.id) { index, item in
                        AuspiciousDayRow(item: item, isBest: index == 0, today: today,
                                         isAdded: isAdded(item.day, event: found.key.event)) {
                            addCountdown(item.day, event: found.key.event)
                        }
                        .opacity(isCurrent ? 1 : 0.4)
                    }
                }
            } else {
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 40)
            }
        }
    }

    private func search(_ key: SearchKey) async {
        let days = await Task.detached(priority: .userInitiated) {
            AuspiciousDays.find(key.event, from: key.today, days: key.days, animal: key.animal, weekendsOnly: key.weekendsOnly)
        }.value
        guard !Task.isCancelled else { return }
        found = SearchResult(key: key, days: days)
    }

    private static func emptyMessage(for key: SearchKey) -> String {
        if key.weekendsOnly { return "关掉「只看周末」，或者把范围放宽试试" }
        if key.days < 90 { return "把范围放宽到 90 天试试" }
        return "近期不太适合办这件事，过些日子再来看看"
    }

    private func refreshToday() {
        let now = DayKey.today
        if now != today { today = now }
    }

    // MARK: - 加入倒数日

    private func isAdded(_ day: DayKey, event: AuspiciousEvent) -> Bool {
        countdowns.contains {
            $0.title == event.name && $0.kindValue == .countdown && !$0.isLunar && DayKey($0.date) == day
        }
    }

    private func addCountdown(_ day: DayKey, event: AuspiciousEvent) {
        guard !isAdded(day, event: event) else { return }
        let countdown = Countdown(title: event.name, emoji: event.emoji, date: day.date, kind: CountdownKind.countdown.rawValue)
        countdown.colorIndex = AuspiciousEvent.allCases.firstIndex(of: event) ?? 0
        context.insert(countdown)
        try? context.save()
        WidgetCenter.shared.reloadAllTimelines()
        Haptics.success()
        Announce.post("已把\(DateText.monthDay(day))\(event.name)加入倒数日")
    }
}

private struct SearchKey: Hashable, Sendable {
    let event: AuspiciousEvent
    let days: Int
    let weekendsOnly: Bool
    let today: DayKey
    let animal: EarthlyBranch?
}

private struct SearchResult {
    let key: SearchKey
    let days: [AuspiciousDay]
}

// MARK: - 事项格子

private struct EventTile: View {
    let event: AuspiciousEvent
    let isSelected: Bool
    let action: () -> Void
    @Environment(\.palette) private var palette

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                Image(systemName: event.symbol)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(isSelected ? palette.onAccent : palette.ink(palette.accent))
                    .frame(width: 46, height: 46)
                    .background {
                        if isSelected {
                            Circle().fill(palette.accentGradient)
                                .shadow(color: palette.accent.opacity(0.45), radius: 8, y: 2)
                        } else {
                            Circle().fill(palette.accent.opacity(palette.isDark ? 0.14 : 0.12))
                        }
                    }
                Text(event.name)
                    .font(.caption.weight(isSelected ? .semibold : .regular))
                    .foregroundStyle(isSelected ? palette.ink(palette.accent) : Color.primary)
                    .multilineTextAlignment(.center)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .contentShape(.rect)
        }
        .buttonStyle(.pressableCard)
        .accessibilityLabel(event.name)
        .accessibilityHint(event.subtitle)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - 结果行

private struct AuspiciousDayRow: View {
    let item: AuspiciousDay
    let isBest: Bool
    let today: DayKey
    let isAdded: Bool
    let add: () -> Void

    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme
    @Environment(\.dynamicTypeSize) private var typeSize

    var body: some View {
        let day = item.day
        let lunar = LunarDate(day)
        // 辅助功能大字号时日期块放到上面，理由标签才有整行的宽度
        let isLarge = typeSize.isAccessibilitySize
        let layout = isLarge ? AnyLayout(VStackLayout(alignment: .leading, spacing: 12)) : AnyLayout(HStackLayout(alignment: .top, spacing: 14))
        let footer = isLarge ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8)) : AnyLayout(HStackLayout(spacing: 10))
        VStack(alignment: .leading, spacing: 12) {
            layout {
                dateBadge(day)
                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .firstTextBaseline, spacing: 6) {
                        Text("农历\(lunar.text)").font(.subheadline.weight(.semibold))
                        if isBest {
                            TagChip(text: "首选", color: Color.gold(scheme), symbol: "crown.fill")
                        }
                    }
                    FlowLayout(spacing: 6) {
                        ForEach(item.reasons, id: \.self) { reason in
                            TagChip(text: reason.text, color: color(for: reason.kind),
                                    symbol: reason.kind == .clash ? "exclamationmark.triangle.fill" : nil)
                        }
                    }
                    // TagChip 不换行，字号再大最长的「宜…」标签会超出卡片
                    .dynamicTypeSize(...DynamicTypeSize.accessibility1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(accessibilityText(day: day, lunar: lunar))

            footer {
                Text(relativeText(day))
                    .font(.footnote)
                    .subtleForeground()
                    .accessibilityHidden(true)
                Spacer(minLength: 0)
                Button(action: add) {
                    Label(isAdded ? "已在倒数日" : "加入倒数日", systemImage: isAdded ? "checkmark.circle.fill" : "calendar.badge.plus")
                        .font(.footnote.weight(.semibold))
                }
                .buttonStyle(.glass)
                .disabled(isAdded)
                .accessibilityLabel(isAdded ? "\(DateText.monthDay(day))已在倒数日" : "把\(DateText.monthDay(day))加入倒数日")
            }
        }
        .glassCard(padding: 16, tint: item.clashesUser ? Color.caution(scheme).opacity(0.08) : nil)
    }

    private func dateBadge(_ day: DayKey) -> some View {
        let isWeekend = day.weekday == 0 || day.weekday == 6
        return VStack(spacing: 2) {
            Text(day.year == today.year ? "\(day.month)月" : "\(day.year % 100)年\(day.month)月")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(palette.ink(palette.accent))
            Text("\(day.day)")
                .font(.system(.title, design: .rounded, weight: .bold))
                .monospacedDigit()
            Text(DateText.weekday(day))
                .font(.caption2.weight(isWeekend ? .semibold : .regular))
                .foregroundStyle(isWeekend ? palette.ink(palette.accentSecondary) : Color.secondary)
        }
        .lineLimit(1)
        .frame(minWidth: 56)
        .padding(.vertical, 8)
        .padding(.horizontal, 4)
        .background(palette.accent.opacity(palette.isDark ? 0.12 : 0.1), in: .rect(cornerRadius: 16))
    }

    private func color(for kind: AuspiciousReason.Kind) -> Color {
        switch kind {
        case .jianChu: Color.positive(scheme)
        case .deity: Color.gold(scheme)
        case .yi: palette.accent
        case .shensha: palette.accentSecondary
        case .zodiac: Color(hex: 0x94A3B8)
        case .clash: Color.caution(scheme)
        }
    }

    private func relativeText(_ day: DayKey) -> String {
        let days = today.days(to: day)
        switch days {
        case 0: return "就是今天"
        case 1: return "明天"
        case 2: return "后天"
        default: return "还有 \(days) 天"
        }
    }

    private func accessibilityText(day: DayKey, lunar: LunarDate) -> String {
        let date = day.year == today.year ? DateText.monthDay(day) : DateText.full(day)
        return (isBest ? "首选，" : "") + "\(date) \(DateText.weekday(day))，农历\(lunar.text)，\(relativeText(day))。\(item.summary)"
    }
}
