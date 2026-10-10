import SwiftUI
import SwiftData

struct TodayView: View {
    @Environment(AppState.self) private var app
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette

    @State private var showEditor = false
    @State private var showOnboarding = false
    @State private var today = DayKey.today

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

                ForEach(settings.homeCards.filter { !settings.hiddenHomeCards.contains($0) }) { card in
                    switch card {
                    case .fortune: FortuneHeroCard(fortune: fortune)
                    case .almanac: AlmanacCard(fortune: fortune, almanac: almanac)
                    case .horoscope: HoroscopeMiniCard()
                    case .stick: StickMiniCard()
                    case .quickTools: QuickToolsRow()
                    case .countdown: CountdownStrip()
                    case .mood: MoodCheckInCard()
                    case .quote: QuoteCard(quote: fortune.quote)
                    }
                }

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
            .padding(.horizontal, 16)
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
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged)) { _ in
            today = .today
        }
    }
}

// MARK: - 头部

private struct TodayHeader: View {
    let day: DayKey
    let almanac: AlmanacDay
    let greeting: String

    @Environment(\.palette) private var palette

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(greeting)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
            HStack(alignment: .firstTextBaseline, spacing: 10) {
                Text("\(day.month)月\(day.day)日")
                    .font(.system(size: 40, weight: .bold, design: .rounded))
                    .foregroundStyle(palette.accentGradient)
                Text(DateText.weekday(day))
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.secondary)
                Spacer()
            }
            HStack(spacing: 8) {
                Text("农历\(almanac.lunar.text)")
                Text("·")
                Text("\(almanac.lunar.yearGanZhi.name)\(almanac.lunar.yearGanZhi.branch.animal)年")
                Text("·")
                Text(almanac.termText)
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
