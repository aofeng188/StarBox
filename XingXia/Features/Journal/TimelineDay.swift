import SwiftUI
import SwiftData

/// 时间线里一天中的一行（心情单独放在最前，不在这里）
enum JournalItem: Identifiable {
    case decision(DecisionRecord)
    case stick(StickDraw)
    case tarot(TarotReading)

    var id: String {
        switch self {
        case .decision(let record): "d-\(record.id.uuidString)"
        case .stick(let draw): "s-\(draw.id.uuidString)"
        case .tarot(let reading): "t-\(reading.id.uuidString)"
        }
    }

    var date: Date {
        switch self {
        case .decision(let record): record.date
        case .stick(let draw): draw.date
        case .tarot(let reading): reading.date
        }
    }
}

/// 时间线上的一天：心情 + 按时间排的决定、签、塔罗
struct JournalDay: Identifiable {
    let day: DayKey
    var mood: MoodEntry?
    var items: [JournalItem] = []

    var id: Int { day.rawValue }

    /// 求签、塔罗在时间线里用 StickDraw、TarotReading 单独成行，同时记下的决策记录不再重复显示
    static let ownRowTools: Set<String> = ["stick", "tarot"]

    /// 按天分组，只留有内容的日子，新的在前；每天里的行按时间先后
    static func group(records: [DecisionRecord], moods: [MoodEntry], draws: [StickDraw], readings: [TarotReading]) -> [JournalDay] {
        var days: [Int: JournalDay] = [:]
        func append(_ item: JournalItem, on day: DayKey) {
            days[day.rawValue, default: JournalDay(day: day)].items.append(item)
        }
        for record in records where !ownRowTools.contains(record.tool) {
            append(.decision(record), on: DayKey(record.date))
        }
        for draw in draws {
            guard let day = DayKey(rawValue: draw.dayKey) else { continue }
            append(.stick(draw), on: day)
        }
        for reading in readings {
            append(.tarot(reading), on: DayKey(reading.date))
        }
        for mood in moods {
            guard let day = DayKey(rawValue: mood.dayKey) else { continue }
            days[day.rawValue, default: JournalDay(day: day)].mood = mood
        }
        return days.values
            .map { day in
                var day = day
                day.items.sort { $0.date < $1.date }
                return day
            }
            .sorted { $0.day > $1.day }
    }
}

/// 时间线上的一天：日期头（公历、农历、心情、运势分数），下面是心情、决定、签、塔罗
struct TimelineDayCard: View {
    let entry: JournalDay
    let today: DayKey
    let score: Int
    /// 档案 id → 名字，给别人求的签标上「为某某」
    let names: [UUID: String]
    let primaryID: UUID?
    let onEdit: (DecisionRecord) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header
            if let mood = entry.mood, let level = MoodLevel(rawValue: mood.mood) {
                NavigationLink(value: AppRoute.mood) {
                    MoodLine(mood: mood, level: level)
                }
                .buttonStyle(.plain)
                .accessibilityHint("打开心情打卡")
            }
            ForEach(entry.items) { item in
                switch item {
                case .decision(let record):
                    DecisionLine(record: record, onEdit: onEdit)
                case .stick(let draw):
                    StickLine(draw: draw, forName: ownerName(draw.profileID))
                case .tarot(let reading):
                    NavigationLink(value: AppRoute.tarotJournal) {
                        TarotLine(reading: reading)
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint("打开塔罗手记")
                }
            }
        }
        .glassCard()
    }

    private var header: some View {
        let lunar = LunarDate(entry.day)
        let mood = entry.mood.flatMap { MoodLevel(rawValue: $0.mood) }
        let title: String = switch entry.day.days(to: today) {
        case 0: "今天"
        case 1: "昨天"
        default: DateText.monthDay(entry.day)
        }
        return HStack(alignment: .center, spacing: 10) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.headline)
                Text("\(DateText.weekday(entry.day)) · 农历\(lunar.text)")
                    .font(.caption)
                    .subtleForeground()
            }
            Spacer(minLength: 4)
            if let mood {
                Text(mood.emoji).font(.title3)
            }
            TagChip(text: "运势 \(score)", symbol: "sparkles")
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(headerLabel(title: title, lunar: lunar, mood: mood))
        .accessibilityAddTraits(.isHeader)
    }

    private func headerLabel(title: String, lunar: LunarDate, mood: MoodLevel?) -> String {
        var parts = [title == DateText.monthDay(entry.day) ? title : "\(title)，\(DateText.monthDay(entry.day))",
                     DateText.weekday(entry.day), "农历\(lunar.text)"]
        if let mood { parts.append("心情\(mood.name)") }
        parts.append("运势 \(score) 分")
        return parts.joined(separator: "，")
    }

    private func ownerName(_ id: UUID?) -> String? {
        guard let id, id != primaryID else { return nil }
        return names[id]
    }
}

// MARK: - 各种行

private struct MoodLine: View {
    let mood: MoodEntry
    let level: MoodLevel

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Text(level.emoji)
                .font(.title3)
                .frame(width: 30, height: 30)
                .background(Circle().fill(level.color.opacity(0.25)))
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 3) {
                Text("心情\(level.name)").font(.subheadline.weight(.semibold))
                if !mood.note.isEmpty {
                    Text(mood.note)
                        .font(.subheadline)
                        .foregroundStyle(.primary.opacity(0.85))
                        .fixedSize(horizontal: false, vertical: true)
                }
                if !mood.tags.isEmpty {
                    Text(mood.tags.map { "#" + $0 }.joined(separator: " ")).font(.caption).foregroundStyle(.tint)
                }
            }
            Spacer(minLength: 0)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

private struct DecisionLine: View {
    let record: DecisionRecord
    let onEdit: (DecisionRecord) -> Void

    var body: some View {
        // 决定可以点开补记；猜拳、刮刮乐、掷筊只是记一笔
        if JournalStats.isDecision(record.tool) {
            Button { onEdit(record) } label: { content(editable: true) }
                .buttonStyle(.plain)
                .accessibilityHint("补记是否照做、后来怎么样")
        } else {
            content(editable: false)
        }
    }

    private func content(editable: Bool) -> some View {
        let info = HistoryLog.info(for: record.tool)
        return HStack(alignment: .top, spacing: 12) {
            ToolIcon(symbol: info.symbol, colors: info.colors, size: 30)
            VStack(alignment: .leading, spacing: 4) {
                Text("\(DateText.time(record.date)) · \(info.name)").font(.caption).subtleForeground()
                Text(record.result)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(3)
                if let question = record.askedQuestion {
                    Text("「\(question)」").font(.caption).subtleForeground().lineLimit(2)
                }
                if editable {
                    DecisionMarks(record: record, showsPending: true)
                }
            }
            Spacer(minLength: 0)
            if editable {
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .padding(.top, 8)
                    .accessibilityHidden(true)
            }
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

private struct StickLine: View {
    let draw: StickDraw
    let forName: String?

    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let stick = StickLibrary.stick(draw.stickNumber)
        let level = stick.levelInfo
        HStack(alignment: .top, spacing: 12) {
            ToolIcon(symbol: "flame.fill", colors: [Color(hex: 0xFF8A3D), Color(hex: 0xE11D48)], size: 30)
            VStack(alignment: .leading, spacing: 4) {
                Text("\(DateText.time(draw.date)) · 今日一签" + (forName.map { " · 为\($0)" } ?? ""))
                    .font(.caption)
                    .subtleForeground()
                HStack(spacing: 8) {
                    Text(stick.numberText).font(AppFont.serif(16, weight: .bold))
                    Text(level.displayName)
                        .font(.caption.weight(.bold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(level.color.opacity(0.25), in: .capsule)
                        .foregroundStyle(level.ink(scheme))
                }
                Text("「\(stick.title)」").font(.subheadline).subtleForeground()
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel((forName.map { "为\($0)求签，" } ?? "今日一签，") + stick.spokenText)
    }
}

private struct TarotLine: View {
    let reading: TarotReading

    var body: some View {
        let spread = reading.spreadKind
        let cards = reading.drawnCards
        HStack(alignment: .top, spacing: 12) {
            ToolIcon(symbol: "suit.diamond.fill", colors: [Color(hex: 0x8B5CF6), Color(hex: 0x3B82F6)], size: 30)
            VStack(alignment: .leading, spacing: 4) {
                Text("\(DateText.time(reading.date)) · 塔罗 · \(spread.name)").font(.caption).subtleForeground()
                Text(cards.map { "\($0.card.name)\($0.reversed ? "（逆位）" : "（正位）")" }.joined(separator: " · "))
                    .font(.subheadline.weight(.semibold))
                    .fixedSize(horizontal: false, vertical: true)
                if !reading.question.isEmpty {
                    Text("「\(reading.question)」").font(.caption).subtleForeground().lineLimit(2)
                }
            }
            Spacer(minLength: 0)
            Image(systemName: "chevron.right")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.tertiary)
                .padding(.top, 8)
                .accessibilityHidden(true)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}
