import SwiftUI
import Combine

struct HoroscopeView: View {
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @AppStorage("lastViewedSign", store: AppGroup.defaults) private var lastViewedSign = -1
    @State private var sign: ZodiacSign = .aries
    @State private var period: HoroscopePeriod = .today
    @State private var initialized = false
    @State private var today = DayKey.today

    var body: some View {
        let horoscope = HoroscopeEngine.horoscope(sign, period: period, reference: today)
        ScrollView {
            VStack(spacing: 16) {
                // 星座选择
                ScrollViewReader { proxy in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            ForEach(ZodiacSign.allCases) { item in
                                Button {
                                    withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) { sign = item }
                                    lastViewedSign = item.rawValue
                                    Haptics.selection()
                                } label: {
                                    VStack(spacing: 4) {
                                        ZodiacBadge(sign: item, size: sign == item ? 56 : 44)
                                            .opacity(sign == item ? 1 : 0.55)
                                        Text(item.shortName)
                                            .font(.caption2.weight(sign == item ? .bold : .regular))
                                            .foregroundStyle(sign == item ? .primary : .secondary)
                                    }
                                    .frame(width: 60, height: 80)
                                }
                                .buttonStyle(.plain)
                                .accessibilityElement(children: .ignore)
                                .accessibilityLabel(item.name)
                                .accessibilityAddTraits(sign == item ? [.isButton, .isSelected] : .isButton)
                                .id(item)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .onChange(of: sign) { _, new in withAnimation { proxy.scrollTo(new, anchor: .center) } }
                    .onAppear { proxy.scrollTo(sign, anchor: .center) }
                }
                .pageBleed()

                Picker("时间", selection: $period.animation(.snappy)) {
                    ForEach(HoroscopePeriod.allCases) { Text($0.name).tag($0) }
                }
                .pickerStyle(.segmented)

                // 总览
                VStack(spacing: 14) {
                    HStack(spacing: 16) {
                        // 旁边已有星座名，徽章不再重复朗读
                        ZodiacBadge(sign: sign, size: 84).accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 4) {
                            Text(sign.name).font(.title.bold())
                            Text("\(sign.englishName) · \(sign.dateRange)").font(.caption).foregroundStyle(.secondary)
                            StarRatingView(stars: horoscope.stars, size: 15)
                            // 窄列放不下一行时分两行，避免「周四」单独掉到下一行
                            ViewThatFits(in: .horizontal) {
                                Text("\(period.name)运势 · \(horoscope.rangeText)").lineLimit(1)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("\(period.name)运势")
                                    Text(horoscope.rangeText)
                                }
                            }
                            .font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        ScoreRing(score: horoscope.score, size: 70, lineWidth: 7)
                            .id("\(sign)-\(period)")
                    }
                    Text(horoscope.overview).font(.body).lineSpacing(4)
                    HStack {
                        Image(systemName: "moon.stars.fill").foregroundStyle(palette.accentGradient)
                        Text(horoscope.skyNote).font(.caption).foregroundStyle(.secondary)
                        Spacer()
                    }
                }
                .glassCard()

                // 分项
                VStack(spacing: 14) {
                    ForEach(FortuneCategory.allCases) { category in
                        let value = horoscope.score(category)
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Label(category.shortName + "运", systemImage: category.symbol).font(.subheadline.weight(.semibold))
                                Spacer()
                                StarRatingView(stars: StarRating.stars(for: value), size: 11)
                            }
                            Text(horoscope.categoryTexts[category] ?? "").font(.subheadline).foregroundStyle(.primary.opacity(0.85))
                        }
                        if category != .health { Divider().opacity(0.4) }
                    }
                }
                .glassCard()

                // 幸运。辅助功能大字号下一行放不下四个，改成两行两列
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: dynamicTypeSize.isAccessibilitySize ? 2 : 4), spacing: 12) {
                    // 和运势卡按五行算的「幸运色」区分开
                    LuckyBubble(title: "星座幸运色") {
                        HStack(spacing: 5) {
                            Circle().fill(Color(horoscope.luckyColor)).frame(width: 12, height: 12)
                            Text(horoscope.luckyColor.name)
                        }
                    }
                    LuckyBubble(title: "幸运数字") { Text("\(horoscope.luckyNumber)") }
                    LuckyBubble(title: "速配星座") { Text(horoscope.matchSign.shortName) }
                    LuckyBubble(title: "提防星座") { Text(horoscope.cautionSign.shortName) }
                }

                HStack {
                    Image(systemName: "quote.bubble.fill").foregroundStyle(palette.accentGradient)
                    Text("\(period.name)提醒：\(horoscope.comment)").font(.subheadline.weight(.medium))
                    Spacer()
                }
                .glassCard()

                // 星座档案
                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: "星座档案", symbol: "person.text.rectangle")
                    FlowLayout(spacing: 8) {
                        TagChip(text: sign.element.name)
                        TagChip(text: sign.modality.name, color: palette.accentSecondary)
                        TagChip(text: "守护星 \(sign.ruler)", color: palette.nebula[2])
                    }
                    HStack(spacing: 6) {
                        ForEach(sign.keywords, id: \.self) { Text("#\($0)").font(.caption).foregroundStyle(.secondary) }
                    }
                    Text(sign.profile).font(.subheadline).lineSpacing(4)
                    HStack {
                        Text("最佳拍档").font(.caption).foregroundStyle(.secondary)
                        ForEach(sign.trines, id: \.self) { Text($0.name).font(.caption.weight(.semibold)) }
                    }
                }
                .glassCard()

                NavigationLink(value: AppRoute.compatibility) {
                    Label("查看星座配对", systemImage: "heart.circle")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.glass)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("星座运势")
        .onAppear {
            refreshToday()
            guard !initialized else { return }
            initialized = true
            if let birth = app.birth, lastViewedSign < 0 {
                sign = ZodiacSign.sunSign(of: birth)
            } else if let stored = ZodiacSign(rawValue: lastViewedSign) {
                sign = stored
            } else if let birth = app.birth {
                sign = ZodiacSign.sunSign(of: birth)
            }
        }
        // 跨过零点、或者隔天从后台回来时刷新「今天」
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
    }

    private func refreshToday() {
        let now = DayKey.today
        if now != today { today = now }
    }
}

private struct LuckyBubble<Content: View>: View {
    let title: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(spacing: 4) {
            Text(title).font(.caption2).foregroundStyle(.secondary).lineLimit(1).minimumScaleFactor(0.7)
            content.font(.subheadline.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .glassEffect(.regular, in: .rect(cornerRadius: 18))
        .accessibilityElement(children: .combine)
    }
}
