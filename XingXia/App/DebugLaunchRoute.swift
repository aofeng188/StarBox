#if DEBUG
import Foundation

/// 调试用启动参数：在模拟器里直接停到某个页面，方便逐页截图检查排版。
/// `xcrun simctl launch <设备> com.tianaofeng.xingxia -debugTab ask`（today / ask / journal / me / search）
/// `xcrun simctl launch <设备> com.tianaofeng.xingxia -debugRoute xingxia://tool/coin`
/// 没有深链接的页面直接写名字：`-debugRoute backup`、`-debugRoute settings`；
/// 年度回顾：`-debugRoute review`（默认年份）或 `-debugRoute xingxia://review/2025`
/// 每日仪式：带 `-debugTab` / `-debugRoute` 时不弹；只截别的页面可加 `-skipRitual`，截仪式本身加 `-forceRitual`
enum DebugLaunchRoute {
    /// 名字 → 页面，覆盖所有不需要参数的路由（含没有深链接的档案、主题、设置等）
    private static var named: [String: AppRoute] {
        [
            "fortune": .fortune, "almanac": .almanac, "horoscope": .horoscope, "stick": .stick, "tarot": .tarot,
            "answers": .answers, "birthChart": .birthChart, "compatibility": .compatibility, "countdowns": .countdowns,
            "mood": .mood, "history": .history, "calendar": .fortuneCalendar, "profiles": .profiles, "themes": .themes,
            "settings": .settings, "auspicious": .auspicious, "sky": .skyCalendar, "tarot-journal": .tarotJournal,
            "backup": .backup, "review": .annualReview(year: AppRoute.reviewYear()),
        ]
    }

    @MainActor
    static func apply() {
        let tabs: [String: AppTab] = ["today": .today, "ask": .ask, "journal": .journal, "me": .me, "search": .search]
        if let tab = UserDefaults.standard.string(forKey: "debugTab").flatMap({ tabs[$0] }) {
            AppState.shared.selectedTab = tab
        }
        guard let value = UserDefaults.standard.string(forKey: "debugRoute") else { return }
        if let route = named[value] {
            AppState.shared.open(route)
        } else if let url = URL(string: value) {
            AppState.shared.handle(url: url)
        }
    }
}
#endif
