import SwiftUI
import Combine

struct AlmanacDetailView: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.colorScheme) private var scheme
    @State private var today = DayKey.today
    @State private var day = DayKey.today
    @State private var showPicker = false
    @State private var pickerDate = Date()

    var body: some View {
        let almanac = AlmanacDay(day)
        let fortune = FortuneEngine.daily(for: app.birth, on: day)
        // 翻到别的日期时，「今日」改成「昨日 / 明日 / 当日」
        let dayWord = Self.dayWord(for: day, today: today)

        ScrollView {
            VStack(spacing: 16) {
                // 日期翻页
                HStack {
                    Button { move(-1) } label: { Image(systemName: "chevron.left").frame(width: 36, height: 36) }
                        .buttonStyle(.glass)
                        .accessibilityLabel("前一天")
                    Spacer()
                    Button {
                        pickerDate = day.date
                        showPicker = true
                    } label: {
                        VStack(spacing: 2) {
                            Text("\(DateText.full(day)) \(DateText.weekday(day))").font(.headline)
                            if day != today { Text("点按选择日期").font(.caption2).foregroundStyle(.secondary) }
                        }
                    }
                    .buttonStyle(.plain)
                    Spacer()
                    Button { move(1) } label: { Image(systemName: "chevron.right").frame(width: 36, height: 36) }
                        .buttonStyle(.glass)
                        .accessibilityLabel("后一天")
                }

                // 主信息
                VStack(spacing: 10) {
                    Text(almanac.lunar.monthName + almanac.lunar.dayName)
                        .font(AppFont.serif(46, weight: .bold))
                        .foregroundStyle(palette.accentGradient)
                        .shadow(color: palette.accent.opacity(0.4), radius: 12)
                    Text(almanac.ganzhiText).font(.subheadline.weight(.medium))
                    Text("\(almanac.lunar.yearGanZhi.branch.animalEmoji) \(almanac.lunar.yearGanZhi.name)\(almanac.lunar.yearGanZhi.branch.animal)年 · 纳音\(almanac.day.nayin)")
                        .font(.footnote).foregroundStyle(.secondary)
                    HStack(spacing: 8) {
                        TagChip(text: almanac.jianChu.name, color: almanac.jianChu.isLucky ? Color(hex: 0x34D399) : Color(hex: 0xFBBF24))
                        TagChip(text: "\(almanac.deity.name) · \(almanac.deity.pathName)", color: almanac.deity.isYellowPath ? Color(hex: 0xFBBF24) : Color(hex: 0x94A3B8))
                    }
                    if !almanac.shensha.isEmpty {
                        // 神煞多时（大字号下）一行放不下就竖排
                        ViewThatFits(in: .horizontal) {
                            HStack(spacing: 8) { shenshaChips(almanac.shensha) }
                            VStack(spacing: 6) { shenshaChips(almanac.shensha) }
                        }
                        .accessibilityElement(children: .ignore)
                        .accessibilityLabel("神煞：\(almanac.shensha.joined(separator: "、"))")
                    }
                    if !almanac.festivals.isEmpty {
                        HStack(spacing: 8) {
                            ForEach(almanac.festivals, id: \.self) { TagChip(text: $0, color: Color(hex: 0xFB7185)) }
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .glassCard(padding: 22)

                // 传统宜忌
                VStack(alignment: .leading, spacing: 14) {
                    SectionHeader(title: "黄历宜忌", subtitle: almanac.jianChu.summary, symbol: "book.closed.fill")
                    YiJiRow(isYi: true, items: almanac.yi)
                    YiJiRow(isYi: false, items: almanac.ji)
                    if !almanac.shensha.isEmpty {
                        Divider().opacity(0.4)
                        VStack(alignment: .leading, spacing: 8) {
                            ForEach(almanac.shensha, id: \.self) { name in
                                HStack(alignment: .firstTextBaseline, spacing: 8) {
                                    Text(name)
                                        .font(.subheadline.weight(.semibold))
                                        .foregroundStyle(palette.ink(shenshaColor(name)))
                                    Text(Shensha.notes[name] ?? "")
                                        .font(.subheadline)
                                        .foregroundStyle(.secondary)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                                .accessibilityElement(children: .combine)
                            }
                        }
                    }
                }
                .glassCard()

                // 细节
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                    InfoCell(title: "冲煞", value: almanac.clash, symbol: "arrow.left.arrow.right.circle.fill")
                    InfoCell(title: "值神", value: "\(almanac.deity.name)（\(almanac.deity.isYellowPath ? "吉" : "凶")）", symbol: "seal.fill")
                    InfoCell(title: "喜神方位", value: almanac.joyDirection, symbol: "face.smiling.inverse")
                    InfoCell(title: "财神方位", value: almanac.wealthDirection, symbol: "yensign.circle.fill")
                    InfoCell(title: "日柱纳音", value: almanac.day.nayin, symbol: "waveform")
                    InfoCell(title: "\(dayWord)冲", value: "属\(almanac.clashAnimal.animal)的朋友多注意", symbol: "exclamationmark.shield.fill")
                }

                VStack(alignment: .leading, spacing: 10) {
                    SectionHeader(title: "彭祖百忌", symbol: "scroll.fill")
                    ForEach(almanac.pengzu, id: \.self) { line in
                        Text(line).font(AppFont.serif(17))
                    }
                }
                .glassCard()

                // 节气
                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: "节气", symbol: "leaf.fill")
                    let current = almanac.solarTerm ?? SolarTerms.previous(onOrBefore: day)
                    VStack(alignment: .leading, spacing: 6) {
                        Text(almanac.solarTerm != nil ? "\(dayWord) \(current.name)" : "当前节气 · \(current.name)")
                            .font(.headline)
                        Text(SolarTerms.blurbs[current.name] ?? "").font(.subheadline).foregroundStyle(.secondary)
                    }
                    Divider().opacity(0.4)
                    ForEach(upcomingTerms(after: day), id: \.self) { term in
                        HStack {
                            Text(term.name).font(.subheadline.weight(.semibold))
                            Spacer()
                            Text("\(term.day.month)月\(term.day.day)日 \(term.moment.formatted(.dateTime.hour().minute()))")
                                .font(.caption).foregroundStyle(.secondary)
                            Text("\(day.days(to: term.day))天后").font(.caption).foregroundStyle(.tint)
                        }
                    }
                }
                .glassCard()

                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: "趣味宜忌", subtitle: app.primary == nil ? nil : "专属于\(app.primary?.name ?? "你")", symbol: "sparkles")
                    YiJiRow(isYi: true, items: fortune.funYi)
                    YiJiRow(isYi: false, items: fortune.funJi)
                }
                .glassCard()

                Text("黄历宜忌按「建除十二神」「黄黑道值神」推算，并参考天德、月德、天赦、四离四绝等神煞，仅作传统文化参考。")
                    .font(.caption2).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("黄历")
        .toolbar {
            if day != today {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("今天") { withAnimation(.snappy) { day = today } }
                }
            }
        }
        .gesture(DragGesture(minimumDistance: 40).onEnded { value in
            if value.translation.width < -60 { move(1) } else if value.translation.width > 60 { move(-1) }
        })
        .sheet(isPresented: $showPicker) {
            NavigationStack {
                DatePicker("日期", selection: $pickerDate, displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .padding()
                    .navigationTitle("选择日期")
                    .navigationBarTitleDisplayMode(.inline)
                    .toolbar {
                        ToolbarItem(placement: .confirmationAction) {
                            Button("完成", systemImage: "checkmark") {
                                day = DayKey(pickerDate)
                                showPicker = false
                            }
                        }
                    }
            }
            .presentationDetents([.medium, .large])
        }
        // 跨过零点、或者隔天从后台回来时刷新「今天」
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
        .onAppear(perform: refreshToday)
    }

    private func refreshToday() {
        let now = DayKey.today
        guard now != today else { return }
        // 原本停在今天的跟着换到新的一天，翻到别的日期的不动
        if day == today { day = now }
        today = now
    }

    private func move(_ delta: Int) {
        Haptics.selection()
        withAnimation(.snappy) { day = day.adding(days: delta) }
    }

    @ViewBuilder
    private func shenshaChips(_ names: [String]) -> some View {
        ForEach(names, id: \.self) { name in
            TagChip(text: name, color: shenshaColor(name), symbol: Shensha.isLucky(name) ? "sparkle" : "exclamationmark.triangle")
        }
    }

    /// 吉神用金色，四离、四绝这类凶日沿用黑道日的灰色
    private func shenshaColor(_ name: String) -> Color {
        Shensha.isLucky(name) ? Color.gold(scheme) : Color(hex: 0x94A3B8)
    }

    private static func dayWord(for day: DayKey, today: DayKey) -> String {
        switch today.days(to: day) {
        case 0: "今日"
        case -1: "昨日"
        case 1: "明日"
        default: "当日"
        }
    }

    private func upcomingTerms(after day: DayKey) -> [SolarTerms.Term] {
        var result: [SolarTerms.Term] = []
        var cursor = day
        while result.count < 3 {
            let next = SolarTerms.next(after: cursor)
            result.append(next)
            cursor = next.day
        }
        return result
    }
}

private struct InfoCell: View {
    let title: String
    let value: String
    let symbol: String
    @Environment(\.palette) private var palette

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Label(title, systemImage: symbol)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value).font(.subheadline.weight(.semibold)).fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .glassCard(cornerRadius: 22, padding: 14)
        .accessibilityElement(children: .combine)
    }
}
