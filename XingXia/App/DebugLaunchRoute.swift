#if DEBUG
import Foundation

/// 调试用启动参数：在模拟器里直接停到某个页面，方便逐页截图检查排版。
/// `xcrun simctl launch <设备> com.tianaofeng.xingxia -debugTab tools`
/// `xcrun simctl launch <设备> com.tianaofeng.xingxia -debugRoute xingxia://tool/coin`
enum DebugLaunchRoute {
    @MainActor
    static func apply() {
        let tabs: [String: AppTab] = ["today": .today, "divination": .divination, "tools": .tools, "records": .records, "me": .me]
        if let tab = UserDefaults.standard.string(forKey: "debugTab").flatMap({ tabs[$0] }) {
            AppState.shared.selectedTab = tab
        }
        if let url = UserDefaults.standard.string(forKey: "debugRoute").flatMap(URL.init(string:)) {
            AppState.shared.handle(url: url)
        }
    }
}
#endif
