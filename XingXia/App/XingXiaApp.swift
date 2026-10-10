import SwiftUI
import SwiftData
import UserNotifications

@main
struct XingXiaApp: App {
    @State private var appState = AppState.shared
    @State private var settings = AppSettings.shared
    @State private var themes = ThemeManager.shared
    private let container: ModelContainer

    init() {
        do {
            container = try AppSchema.makeContainer()
        } catch {
            fatalError("无法打开数据库：\(error)")
        }
        SeedData.insertIfNeeded(into: container.mainContext)
        UNUserNotificationCenter.current().delegate = NotificationDelegate.shared
    }

    var body: some Scene {
        WindowGroup {
            ThemedRoot {
                RootView()
            }
            .environment(appState)
            .environment(settings)
            .environment(themes)
            .onOpenURL { appState.handle(url: $0) }
        }
        .modelContainer(container)
    }
}

/// 根据「主题 + 深浅色」把调色板注入环境
struct ThemedRoot<Content: View>: View {
    @ViewBuilder var content: Content

    @Environment(ThemeManager.self) private var themes
    @Environment(\.colorScheme) private var systemScheme

    var body: some View {
        let scheme = themes.appearance.colorScheme ?? systemScheme
        let palette = themes.current.palette(for: scheme)
        content
            .environment(\.palette, palette)
            .tint(palette.accent)
            .preferredColorScheme(themes.appearance.colorScheme)
            .animation(.easeInOut(duration: 0.6), value: themes.current.id)
    }
}
