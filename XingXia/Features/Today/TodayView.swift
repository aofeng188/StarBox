import SwiftUI
import SwiftData
import Combine

struct TodayView: View {
    @Environment(AppState.self) private var app
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase

    @State private var showEditor = false
    @State private var showOnboarding = false
    @State private var today = DayKey.today
    @State private var isWide = false
    @State private var fold: ClosedRange<CGFloat>?

    var body: some View {
        let fortune = FortuneEngine.daily(for: app.birth, on: today)
        let almanac = AlmanacDay(today)

        ScrollView {
            VStack(spacing: 16) {
                TodayHeader(day: today, almanac: almanac, greeting: app.greeting)
                    .padding(.bottom, 4)

                if app.primary == nil {
                    SetupProfileBanner { showOnboarding = true }
                }

                // 宽屏（Duo 展开横持）排成两栏
                ColumnsLayout(columns: isWide ? 2 : 1, spacing: 16, fold: fold) {
                    ForEach(settings.homeCards.filter { !settings.hiddenHomeCards.contains($0) }) { card in
                        switch card {
                        case .fortune: FortuneHeroCard(fortune: fortune)
                        case .almanac: AlmanacCard(fortune: fortune, almanac: almanac)
                        case .horoscope: HoroscopeMiniCard(today: today)
                        case .stick: StickMiniCard(today: today)
                        case .quickTools: QuickToolsRow()
                        case .countdown: CountdownStrip(today: today)
                        case .mood: MoodCheckInCard(today: today)
                        case .quote: QuoteCard(quote: fortune.quote)
                        }
                    }
                }
                .foldGap($fold)

                Button {
                    showEditor = true
                } label: {
                    Label("编辑首页", systemImage: "slider.horizontal.3")
                        .font(.subheadline)
                }
                .buttonStyle(.glass)
                .padding(.top, 4)
                .padding(.bottom, 24)
            }
            .isWideLayout($isWide)
            .pageContent(maxWidth: PageMetrics.gridWidth)
        }
        .cosmicBackground()
        .navigationTitle("今日")
        .toolbarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .principal) { Text("") }
            ToolbarItem(placement: .topBarTrailing) { ProfileSwitcher() }
        }
        .sheet(isPresented: $showEditor) { HomeEditorView() }
        .sheet(isPresented: $showOnboarding) { NavigationStack { ProfileEditorView(profile: nil, isOnboarding: true) } }
        .task {
            if !settings.hasOnboarded && app.primary == nil {
                try? await Task.sleep(for: .seconds(0.6))
                showOnboarding = true
                settings.hasOnboarded = true
            }
        }
        // 跨过零点、或者隔天从后台回来时刷新「今天」，卡片跟着换天
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
        if now != today { today = now }
    }
}

// MARK: - 头部

private struct TodayHeader: View {
    let day: DayKey
    let almanac: AlmanacDay
    let greeting: String

    @Environment(\.palette) private var palette
    /// 日期大字跟随系统字号放大（设上限），和旁边的星期保持比例
    @ScaledMetric(relativeTo: .largeTitle) private var dateSize: CGFloat = 40

    var body: some View {
        let dateText = Text("\(day.month)月\(day.day)日")
            .font(.system(size: min(dateSize, 60), weight: .bold, design: .rounded))
            .foregroundStyle(palette.accentGradient)
        let weekdayText = Text(DateText.weekday(day))
            .font(.title3.weight(.semibold))
            .foregroundStyle(.secondary)
        let lunarText = "农历\(almanac.lunar.text)"
        let yearText = "\(almanac.lunar.yearGanZhi.name)\(almanac.lunar.yearGanZhi.branch.animal)年"
        VStack(alignment: .leading, spacing: 6) {
            Text(greeting)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
            // 一行放不下（超大字号）时星期换到日期下面
            ViewThatFits(in: .horizontal) {
                HStack(alignment: .firstTextBaseline, spacing: 10) {
                    dateText
                    weekdayText
                }
                VStack(alignment: .leading, spacing: 2) {
                    dateText
                    weekdayText
                }
            }
            // 农历一行放不下时改成竖排，而不是被挤成几列
            ViewThatFits(in: .horizontal) {
                HStack(spacing: 8) {
                    Text(lunarText)
                    Text("·")
                    Text(yearText)
                    Text("·")
                    Text(almanac.termText)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(lunarText)
                    Text(yearText)
                    Text(almanac.termText)
                }
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            if !almanac.festivals.isEmpty {
                FlowLayout(spacing: 6) {
                    ForEach(almanac.festivals, id: \.self) { TagChip(text: $0, symbol: "party.popper.fill") }
                }
                .padding(.top, 2)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.top, 4)
        .accessibilityElement(children: .combine)
    }
}

private struct SetupProfileBanner: View {
    let action: () -> Void
    @Environment(\.palette) private var palette

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                GlowIcon(symbol: "person.crop.circle.badge.plus", color: palette.accent, secondary: palette.accentSecondary, size: 44)
                VStack(alignment: .leading, spacing: 3) {
                    Text("填写生日，解锁专属运势").font(.headline)
                    Text("根据你的八字与星座，每天计算只属于你的运势")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right").foregroundStyle(.tertiary)
            }
            .glassCard(interactive: true)
        }
        .buttonStyle(.pressableCard)
    }
}

// MARK: - 档案切换

struct ProfileSwitcher: View {
    @Environment(AppState.self) private var app
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]

    var body: some View {
        Menu {
            Section("当前查看") {
                ForEach(profiles) { profile in
                    Button {
                        app.setPrimary(profile)
                        Haptics.selection()
                    } label: {
                        if profile.id == app.primary?.id {
                            Label("\(profile.avatar) \(profile.name)", systemImage: "checkmark")
                        } else {
                            Text("\(profile.avatar) \(profile.name)")
                        }
                    }
                }
            }
            Button {
                app.open(.profiles)
            } label: {
                Label("管理档案", systemImage: "person.2")
            }
        } label: {
            Text(app.primary?.avatar ?? "👤")
                .font(.title3)
                .frame(width: 34, height: 34)
        }
        .accessibilityLabel("切换档案")
    }
}
