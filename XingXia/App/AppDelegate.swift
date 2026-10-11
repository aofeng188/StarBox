import UIKit

final class AppDelegate: NSObject, UIApplicationDelegate {
    /// 普通 iPhone 和 iPhone Duo 的外屏只用竖屏（摇一摇、掷骰子时界面不会跟着乱转）；
    /// Duo 展开后的内屏本来就是横向的大屏，放开所有方向，界面才能铺满而不是被缩成一条竖屏。
    /// 排版本身不依赖方向，只看可用空间，见 `AdaptiveLayout.swift`。
    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        guard let screen = window?.windowScene?.screen else { return .portrait }
        return min(screen.bounds.width, screen.bounds.height) >= 600 ? .all : .portrait
    }

    /// 折叠 / 展开后窗口换了屏幕，让系统重新询问支持的方向
    static func refreshSupportedOrientations() {
        for case let scene as UIWindowScene in UIApplication.shared.connectedScenes {
            for window in scene.windows {
                window.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
            }
        }
    }
}
