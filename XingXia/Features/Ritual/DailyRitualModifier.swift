import SwiftUI
import SwiftData

extension View {
    /// 每天第一次进入前台时全屏展示每日仪式：需要打开了「每日仪式」、已经走过档案引导、今天还没有 RitualDay。
    /// DEBUG 下启动参数 `-forceRitual` 每次启动都展示一次（截图用），`-skipRitual` 或带 `-debugTab` / `-debugRoute` 时不展示。
    func dailyRitual() -> some View {
        modifier(DailyRitualModifier())
    }
}

private struct RitualSession: Identifiable {
    let day: DayKey
    var id: Int { day.rawValue }
}

private struct DailyRitualModifier: ViewModifier {
    @Environment(AppState.self) private var app
    @Environment(AppSettings.self) private var settings
    @Environment(\.modelContext) private var context
    @Environment(\.scenePhase) private var scenePhase

    @State private var session: RitualSession?
    /// 仪式真的显示出来了（别的弹窗挡着时 fullScreenCover 可能没弹出来）
    @State private var isShowing = false
    /// 首次启动正在走档案引导：这次启动不打扰，等下次从后台回来
    @State private var waitsForBackground = false
    /// 仪式结束后要去的页面（「去求签」）
    @State private var pendingRoute: AppRoute?
    @State private var didAppear = false
    #if DEBUG
    @State private var didForce = false
    #endif

    func body(content: Content) -> some View {
        content
            .fullScreenCover(item: $session, onDismiss: openPendingRoute) { session in
                RitualView(day: session.day) { route in
                    pendingRoute = route
                    self.session = nil
                }
                .onAppear { isShowing = true }
                .onDisappear { isShowing = false }
            }
            .onAppear {
                guard !didAppear else { return }
                didAppear = true
                if !settings.hasOnboarded && app.primary == nil { waitsForBackground = true }
                if scenePhase == .active { evaluate() }
            }
            .onChange(of: scenePhase) { _, phase in
                switch phase {
                case .active:
                    // 跨天回来时昨天的仪式还开着：换成今天的
                    if let current = session, current.day != .today, isShowing { session = nil }
                    evaluate()
                case .background:
                    waitsForBackground = false
                    // 没能弹出来的仪式不要一直占着，下次回到前台重新判断
                    if session != nil && !isShowing { session = nil }
                default:
                    break
                }
            }
    }

    private func evaluate() {
        guard session == nil else { return }
        let today = DayKey.today
        #if DEBUG
        let arguments = ProcessInfo.processInfo.arguments
        // 用 -debugTab / -debugRoute 打开指定页面截图时也不弹，免得仪式盖住要看的页面
        if arguments.contains("-skipRitual") || arguments.contains("-debugTab") || arguments.contains("-debugRoute") { return }
        if arguments.contains("-forceRitual") && !didForce {
            didForce = true
            session = RitualSession(day: today)
            return
        }
        #endif
        guard settings.dailyRitualEnabled, !waitsForBackground,
              settings.hasOnboarded || app.primary != nil else { return }
        let key = today.rawValue
        let descriptor = FetchDescriptor<RitualDay>(predicate: #Predicate<RitualDay> { $0.dayKey == key })
        // 查询出错时按「已经看过」处理，宁可少弹一次
        guard ((try? context.fetchCount(descriptor)) ?? 1) == 0 else { return }
        session = RitualSession(day: today)
    }

    private func openPendingRoute() {
        guard let route = pendingRoute else { return }
        pendingRoute = nil
        app.open(route)
    }
}
