import SwiftUI
import WidgetKit

enum AppTab: Hashable {
    case today, divination, tools, records, me
}

/// App 内所有可直接跳转的页面（深链接、小组件、Siri 都走这里）
enum AppRoute: Hashable {
    case tool(ToolKind)
    case fortune
    case almanac
    case horoscope
    case stick
    case tarot
    case answers
    case birthChart
    case compatibility
    case countdowns
    case mood
    case history
    case fortuneCalendar
    case profiles
    case themes
    case settings
    /// 择吉日
    case auspicious
    /// 天象日历
    case skyCalendar
    /// 塔罗手记
    case tarotJournal
    /// 数据备份
    case backup

    var tab: AppTab {
        switch self {
        case .tool: .tools
        case .skyCalendar: .today
        case .fortune, .almanac, .horoscope, .stick, .tarot, .answers, .birthChart, .compatibility, .auspicious, .tarotJournal: .divination
        case .countdowns, .mood, .history, .fortuneCalendar: .records
        case .profiles, .themes, .settings, .backup: .me
        }
    }

    /// xingxia://tool/coin、xingxia://fortune、xingxia://auspicious 等
    init?(url: URL) {
        guard url.scheme == "xingxia" else { return nil }
        let host = url.host() ?? ""
        let first = url.pathComponents.dropFirst().first
        switch host {
        case "tool":
            guard let id = first, let tool = ToolKind(rawValue: id) else { return nil }
            self = .tool(tool)
        case "fortune": self = .fortune
        case "almanac": self = .almanac
        case "horoscope": self = .horoscope
        case "stick": self = .stick
        case "tarot": self = .tarot
        case "answers": self = .answers
        case "countdowns": self = .countdowns
        case "mood": self = .mood
        case "history": self = .history
        case "calendar": self = .fortuneCalendar
        case "auspicious": self = .auspicious
        case "sky": self = .skyCalendar
        case "tarot-journal": self = .tarotJournal
        case "backup": self = .backup
        default: return nil
        }
    }
}

@Observable
@MainActor
final class AppState {
    static let shared = AppState()

    var selectedTab: AppTab = .today
    var paths: [AppTab: [AppRoute]] = [.today: [], .divination: [], .tools: [], .records: [], .me: []]

    private(set) var primary: ProfileSnapshot?

    var birth: BirthInfo? { primary?.birth }

    private init() {
        primary = ProfileSnapshot.load()
        ThemeManager.shared.update(birth: primary?.birth)
    }

    func binding(for tab: AppTab) -> Binding<[AppRoute]> {
        Binding(get: { self.paths[tab] ?? [] }, set: { self.paths[tab] = $0 })
    }

    func open(_ route: AppRoute) {
        selectedTab = route.tab
        paths[route.tab] = [route]
    }

    func handle(url: URL) {
        if let route = AppRoute(url: url) { open(route) }
    }

    /// 切换「当前档案」：同步到小组件、主题规则和每日提醒
    func setPrimary(_ profile: Profile?) {
        let snapshot = profile?.snapshot
        primary = snapshot
        ProfileSnapshot.save(snapshot)
        ThemeManager.shared.update(birth: snapshot?.birth)
        WidgetCenter.shared.reloadAllTimelines()
        NotificationScheduler.reschedule()
    }

    var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        let base = switch hour {
        case 5..<9: "早上好"
        case 9..<12: "上午好"
        case 12..<14: "中午好"
        case 14..<18: "下午好"
        case 18..<23: "晚上好"
        default: "夜深了"
        }
        if let name = primary?.name, !name.isEmpty, name != "我" { return "\(base)，\(name)" }
        return base
    }
}
