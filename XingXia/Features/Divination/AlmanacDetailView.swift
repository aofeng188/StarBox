import SwiftUI

struct AlmanacDetailView: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @State private var day = DayKey.today
    @State private var showPicker = false
    @State private var pickerDate = Date()

    var body: some View {
        let almanac = AlmanacDay(day)
        let fortune = FortuneEngine.daily(for: app.birth, on: day)

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
                            if day != .today { Text("点按选择日期").font(.caption2).foregroundStyle(.secondary) }
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
                }
                .glassCard()

                // 细节
                LazyVGrid(columns: [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)], spacing: 12) {
                    InfoCell(title: "冲煞", value: almanac.clash, symbol: "arrow.left.arrow.right.circle.fill")
                    InfoCell(title: "值神", value: "\(almanac.deity.name)（\(almanac.deity.isYellowPath ? "吉" : "凶")）", symbol: "seal.fill")
                    InfoCell(title: "喜神方位", value: almanac.joyDirection, symbol: "face.smiling.inverse")
                    InfoCell(title: "财神方位", value: almanac.wealthDirection, symbol: "yensign.circle.fill")
                    InfoCell(title: "日柱纳音", value: almanac.day.nayin, symbol: "waveform")
                    InfoCell(title: "今日冲", value: "属\(almanac.clashAnimal.animal)的朋友多注意", symbol: "exclamationmark.shield.fill")
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
                        Text(almanac.solarTerm != nil ? "今日 \(current.name)" : "当前节气 · \(current.name)")
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

                Text("黄历宜忌按「建除十二神」与「黄黑道值神」推算，仅作传统文化参考。")
                    .font(.caption2).foregroundStyle(.secondary).multilineTextAlignment(.center)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("黄历")
        .toolbar {
            if day != .today {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("今天") { withAnimation(.snappy) { day = .today } }
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
    }

    private func move(_ delta: Int) {
        Haptics.selection()
        withAnimation(.snappy) { day = day.adding(days: delta) }
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
    }
}
