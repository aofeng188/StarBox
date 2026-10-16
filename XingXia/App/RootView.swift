import SwiftUI

struct RootView: View {
    @Environment(AppState.self) private var app
    @Environment(AppSettings.self) private var settings
    @Environment(ThemeManager.self) private var themes
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass

    var body: some View {
        @Bindable var app = app
        TabView(selection: $app.selectedTab) {
            Tab("今日", systemImage: "sparkles", value: AppTab.today) {
                NavigationStack(path: app.binding(for: .today)) { TodayView().withAppRoutes() }
            }
            Tab("问一问", systemImage: "questionmark.bubble.fill", value: AppTab.ask) {
                NavigationStack(path: app.binding(for: .ask)) { AskView().withAppRoutes() }
            }
            Tab("日记", systemImage: "text.book.closed.fill", value: AppTab.journal) {
                NavigationStack(path: app.binding(for: .journal)) { JournalView().withAppRoutes() }
            }
            Tab("我的", systemImage: "person.crop.circle.fill", value: AppTab.me) {
                NavigationStack(path: app.binding(for: .me)) { MeView().withAppRoutes() }
            }
            Tab(value: AppTab.search, role: .search) {
                NavigationStack(path: app.binding(for: .search)) { SearchView().withAppRoutes() }
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
        .overlay(alignment: .bottom) { DecisionPrompt(bottomInset: promptInset) }
        .dailyRitual()
        // 切 tab、进出页面时收起决策浮层
        .onChange(of: app.selectedTab) { dismissDecisionPrompt() }
        .onChange(of: app.paths) { dismissDecisionPrompt() }
        .onGeometryChange(for: CGSize.self) { $0.size } action: { _ in
            // 折叠 / 展开会把窗口换到另一块屏幕：等转场结束，再让系统重新确认支持的方向
            Task {
                try? await Task.sleep(for: .milliseconds(400))
                AppDelegate.refreshSupportedOrientations()
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active {
                themes.refresh()
                NotificationScheduler.reschedule()
                Haptics.prepare()
            } else if phase == .background {
                dismissDecisionPrompt()
            }
        }
        .task {
            SoundEngine.shared.prepare()
            // 「按时段切换」主题需要定时检查
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(60))
                themes.refresh()
            }
        }
    }

    /// 决策浮层底部让出的高度：iPhone 上 tab 栏浮在底部，沉浸页隐藏了 tab 栏就贴近安全区；iPad 的 tab 栏在顶部
    private var promptInset: CGFloat {
        guard horizontalSizeClass == .compact else { return 8 }
        return app.paths[app.selectedTab]?.last?.hidesTabBar == true ? 8 : 64
    }

    private func dismissDecisionPrompt() {
        if app.lastDecision != nil { app.dismissDecisionPrompt() }
    }
}

extension View {
    func withAppRoutes() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            RouteDestination(route: route)
        }
    }
}
