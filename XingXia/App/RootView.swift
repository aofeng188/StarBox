import SwiftUI

struct RootView: View {
    @Environment(AppState.self) private var app
    @Environment(AppSettings.self) private var settings
    @Environment(ThemeManager.self) private var themes
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        @Bindable var app = app
        TabView(selection: $app.selectedTab) {
            Tab("今日", systemImage: "sparkles", value: AppTab.today) {
                NavigationStack(path: app.binding(for: .today)) { TodayView().withAppRoutes() }
            }
            Tab("占卜", systemImage: "moon.stars.fill", value: AppTab.divination) {
                NavigationStack(path: app.binding(for: .divination)) { DivinationHubView().withAppRoutes() }
            }
            Tab("百宝", systemImage: "square.grid.2x2.fill", value: AppTab.tools) {
                NavigationStack(path: app.binding(for: .tools)) { ToolsHubView().withAppRoutes() }
            }
            Tab("记录", systemImage: "calendar", value: AppTab.records) {
                NavigationStack(path: app.binding(for: .records)) { RecordsHubView().withAppRoutes() }
            }
            Tab("我的", systemImage: "person.crop.circle.fill", value: AppTab.me) {
                NavigationStack(path: app.binding(for: .me)) { MeView().withAppRoutes() }
            }
        }
        .tabBarMinimizeBehavior(.onScrollDown)
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
}

extension View {
    func withAppRoutes() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            RouteDestination(route: route)
        }
    }
}
