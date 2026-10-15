import SwiftUI

/// 路由 → 页面
struct RouteDestination: View {
    let route: AppRoute

    var body: some View {
        switch route {
        case .tool(let tool): ToolDestination(tool: tool).favoriteToggle(.tool(tool))
        case .fortune: FortuneDetailView()
        case .almanac: AlmanacDetailView()
        case .horoscope: HoroscopeView()
        case .stick: FortuneStickView()
        case .tarot: TarotView()
        case .answers: AnswerBookView().favoriteToggle(.answers)
        case .birthChart: BirthChartView()
        case .compatibility: CompatibilityView()
        case .countdowns: CountdownListView()
        case .mood: MoodView()
        case .history: HistoryView()
        case .fortuneCalendar: FortuneCalendarView()
        case .profiles: ProfileListView()
        case .themes: ThemeSettingsView()
        case .settings: SettingsView()
        }
    }
}

struct ToolDestination: View {
    let tool: ToolKind

    var body: some View {
        switch tool {
        case .food: FoodPickerView()
        case .wheel: WheelListView()
        case .lottery: LotteryListView()
        case .finger: FingerPickerView()
        case .groups: GroupShuffleView()
        case .number: RandomNumberView()
        case .coin: CoinFlipView()
        case .dice: DiceView()
        case .generators: GeneratorsView()
        case .truthDare: TruthDareView()
        case .scratch: ScratchCardView()
        case .rps: RPSView()
        }
    }
}

extension View {
    /// 工具页右上角的收藏星标（统一在路由处加，不用逐个改工具页）
    func favoriteToggle(_ item: FavoriteTool) -> some View {
        toolbar {
            ToolbarItem(placement: .topBarTrailing) { FavoriteToggleButton(item: item) }
        }
    }
}

/// 切换「收藏到首页快捷工具」
struct FavoriteToggleButton: View {
    let item: FavoriteTool
    @Environment(AppSettings.self) private var settings

    var body: some View {
        let isFavorite = settings.isFavorite(item.id)
        Button {
            settings.toggleFavorite(item.id)
            Haptics.selection()
            Announce.post(isFavorite ? "已取消收藏" : "已收藏到首页快捷工具")
        } label: {
            Label(isFavorite ? "取消收藏" : "收藏到首页", systemImage: isFavorite ? "star.fill" : "star")
        }
        .symbolEffect(.bounce, value: isFavorite)
        .accessibilityValue(isFavorite ? "已收藏" : "未收藏")
    }
}
