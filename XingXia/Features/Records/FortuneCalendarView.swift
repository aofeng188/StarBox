import SwiftUI
import SwiftData
import Charts

struct FortuneCalendarView: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Query(sort: \StickDraw.date, order: .reverse) private var draws: [StickDraw]
    @Query(sort: \MoodEntry.dayKey, order: .reverse) private var moods: [MoodEntry]

    @State private var month = DayKey(year: DayKey.today.year, month: DayKey.today.month, day: 1)
    @State private var selected = DayKey.today

    private var days: [DayKey] {
        let count = Calendar.gregorianLocal.range(of: .day, in: .month, for: month.date)?.count ?? 30
        return (1...count).map { DayKey(year: month.year, month: month.month, day: $0) }
    }

    var body: some View {
        let scores = Dictionary(uniqueKeysWithValues: days.map { ($0, FortuneEngine.daily(for: app.birth, on: $0).score) })
        let stickMap = Dictionary(draws.filter { $0.profileID == app.primary?.id }.map { ($0.dayKey, $0.stickNumber) }, uniquingKeysWith: { a, _ in a })
        let moodMap = Dictionary(moods.map { ($0.dayKey, $0.mood) }, uniquingKeysWith: { a, _ in a })

        ScrollView {
            VStack(spacing: 16) {
                HStack {
                    Button { shift(-1) } label: { Image(systemName: "chevron.left").frame(width: 34, height: 34) }
                        .buttonStyle(.glass)
                        .accessibilityLabel("上个月")
                    Spacer()
                    Text("\(String(month.year))年\(month.month)月").font(.title3.bold())
                    Spacer()
                    Button { shift(1) } label: { Image(systemName: "chevron.right").frame(width: 34, height: 34) }
                        .buttonStyle(.glass)
                        .accessibilityLabel("下个月")
                }

                // 日历
                let leading = (month.weekday + 6) % 7
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 7), spacing: 8) {
                    ForEach(["一", "二", "三", "四", "五", "六", "日"], id: \.self) { Text($0).font(.caption2).foregroundStyle(.secondary) }
                    ForEach(0..<leading, id: \.self) { _ in Color.clear.frame(height: 54) }
                    ForEach(days, id: \.self) { day in
                        DayCell(day: day, score: scores[day] ?? 60, hasStick: stickMap[day.rawValue] != nil,
                                mood: moodMap[day.rawValue].flatMap(MoodLevel.init(rawValue:)), isSelected: day == selected)
                            .onTapGesture {
                                selected = day
                                Haptics.selection()
                            }
                    }
                }
                .glassCard(padding: 12)

                detail(for: selected, stick: stickMap[selected.rawValue], mood: moodMap[selected.rawValue].flatMap(MoodLevel.init(rawValue:)))

                // 趋势
                VStack(alignment: .leading, spacing: 10) {
                    let values = days.map { scores[$0] ?? 60 }
                    let average = Double(values.reduce(0, +)) / Double(max(1, values.count))
                    SectionHeader(title: "本月运势走势", subtitle: String(format: "平均 %.0f 分", average), symbol: "chart.xyaxis.line")
                    Chart {
                        ForEach(days, id: \.self) { day in
                            AreaMark(x: .value("日", day.day), y: .value("分数", scores[day] ?? 60))
                                .foregroundStyle(LinearGradient(colors: [palette.accent.opacity(0.4), .clear], startPoint: .top, endPoint: .bottom))
                                .interpolationMethod(.catmullRom)
                            LineMark(x: .value("日", day.day), y: .value("分数", scores[day] ?? 60))
                                .foregroundStyle(palette.accentGradient)
                                .interpolationMethod(.catmullRom)
                                .lineStyle(StrokeStyle(lineWidth: 2.5))
                        }
                        RuleMark(y: .value("平均", average))
                            .foregroundStyle(.secondary.opacity(0.5))
                            .lineStyle(StrokeStyle(lineWidth: 1, dash: [4, 4]))
                        if let s = scores[selected], selected.month == month.month {
                            PointMark(x: .value("日", selected.day), y: .value("分数", s))
                                .foregroundStyle(palette.accentSecondary)
                                .symbolSize(90)
                        }
                    }
                    .chartYScale(domain: 30...100)
                    .frame(height: 170)
                }
                .glassCard()
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("运势日历")
        .toolbar {
            if month.month != DayKey.today.month || month.year != DayKey.today.year {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("今天") {
                        month = DayKey(year: DayKey.today.year, month: DayKey.today.month, day: 1)
                        selected = .today
                    }
                }
            }
        }
    }

    private func shift(_ delta: Int) {
        let date = Calendar.gregorianLocal.date(byAdding: .month, value: delta, to: month.date) ?? month.date
        let key = DayKey(date)
        withAnimation(.snappy) {
            month = DayKey(year: key.year, month: key.month, day: 1)
            selected = month
        }
    }

    @ViewBuilder
    private func detail(for day: DayKey, stick: Int?, mood: MoodLevel?) -> some View {
        let fortune = FortuneEngine.daily(for: app.birth, on: day)
        let almanac = AlmanacDay(day)
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 14) {
                ScoreRing(score: fortune.score, size: 70, lineWidth: 7).id(day)
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(DateText.monthDay(day)) \(DateText.weekday(day)) · 农历\(almanac.lunar.text)").font(.subheadline.weight(.semibold))
                    Text("\(fortune.levelName) · \(fortune.summary)").font(.subheadline).foregroundStyle(.secondary).lineLimit(2)
                }
            }
            FlowLayout(spacing: 8) {
                TagChip(text: "宜 " + fortune.funYi.prefix(2).joined(separator: "、"), color: Color(hex: 0x34D399))
                TagChip(text: "忌 " + fortune.funJi.prefix(2).joined(separator: "、"), color: Color(hex: 0xFB7185))
            }
            if let stick {
                let item = StickLibrary.stick(stick)
                Label("求得 \(item.numberText) \(item.levelInfo.displayName) ·「\(item.title)」", systemImage: "flame.fill").font(.caption)
            }
            if let mood {
                Label("心情：\(mood.emoji) \(mood.name)", systemImage: "face.smiling").font(.caption)
            }
            if day > .today {
                Text("未来的运势仅供参考，好运要靠自己创造～").font(.caption2).foregroundStyle(.secondary)
            }
        }
        .glassCard()
    }
}

private struct DayCell: View {
    let day: DayKey
    let score: Int
    let hasStick: Bool
    let mood: MoodLevel?
    let isSelected: Bool
    @Environment(\.palette) private var palette

    var body: some View {
        let intensity = Double(score - 35) / 64
        let isToday = day == .today
        VStack(spacing: 2) {
            Text("\(day.day)")
                .font(.subheadline.weight(isToday ? .bold : .regular))
                .foregroundStyle(isSelected ? .white : .primary)
            Text(mood?.emoji ?? "\(score)")
                .font(.system(size: mood == nil ? 9 : 11, weight: .medium).monospacedDigit())
                .foregroundStyle(isSelected ? .white.opacity(0.9) : .secondary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 54)
        .background {
            RoundedRectangle(cornerRadius: 12)
                .fill(isSelected ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(palette.accent.opacity(0.08 + intensity * 0.35)))
        }
        .overlay(alignment: .topTrailing) {
            if hasStick { Circle().fill(Color(hex: 0xEF4444)).frame(width: 6, height: 6).padding(5) }
        }
        .overlay {
            if isToday && !isSelected { RoundedRectangle(cornerRadius: 12).strokeBorder(palette.accent, lineWidth: 1.5) }
        }
        .opacity(day > .today.adding(days: 7) ? 0.55 : 1)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(day.month)月\(day.day)日，运势 \(score) 分\(mood.map { "，心情\($0.name)" } ?? "")")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
