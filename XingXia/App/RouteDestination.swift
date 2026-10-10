import SwiftUI

/// 路由 → 页面
struct RouteDestination: View {
    let route: AppRoute

    var body: some View {
        switch route {
        case .tool(let tool): ToolDestination(tool: tool)
        case .fortune: FortuneDetailView()
        case .almanac: AlmanacDetailView()
        case .horoscope: HoroscopeView()
        case .stick: FortuneStickView()
        case .tarot: TarotView()
        case .answers: AnswerBookView()
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
