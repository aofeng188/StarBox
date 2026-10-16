import SwiftUI
import WidgetKit

enum AppTab: Hashable {
    case today, ask, journal, me, search
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
    /// 年度回顾
    case annualReview(year: Int)

    var tab: AppTab {
        switch self {
        case .skyCalendar: .today
        case .tool, .fortune, .almanac, .horoscope, .stick, .tarot, .answers, .compatibility, .auspicious, .tarotJournal: .ask
        case .countdowns, .mood, .history, .fortuneCalendar, .annualReview: .journal
        case .birthChart, .profiles, .themes, .settings, .backup: .me
        }
    }

    /// 默认回顾哪一年：1 月看刚过去的一年，其余月份看今年
    static func reviewYear(on date: Date = Date()) -> Int {
        let components = Calendar.current.dateComponents([.year, .month], from: date)
        let year = components.year ?? 2026
        return components.month == 1 ? year - 1 : year
    }

    /// xingxia://tool/coin、xingxia://fortune、xingxia://auspicious、xingxia://review/2026 等
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
        case "review":
            // 年份缺省或不合法时回顾默认年份
            let year = first.flatMap { Int($0) }.flatMap { (1900...2200).contains($0) ? $0 : nil }
            self = .annualReview(year: year ?? Self.reviewYear())
        default: return nil
        }
    }
}

@Observable
@MainActor
final class AppState {
    static let shared = AppState()

    var selectedTab: AppTab = .today
    var paths: [AppTab: [AppRoute]] = [.today: [], .ask: [], .journal: [], .me: [], .search: []]

    /// 「问一问」里正在问的问题：提交新问题时覆盖，清空问题框时置 nil。工具页顶部据此显示「你在问」，
    /// HistoryLog.add 记录决定时把它写进 DecisionRecord.question
    var currentQuestion: String?
    /// 刚刚在前台记下的决定，RootView 据此弹出「就这么办 / 再想想」浮层
    var lastDecision: DecisionRecord?
    /// 问一问拆出的选项，转盘页出现时取走，写进「问一问」转盘
    var pendingWheelOptions: [String]?
    /// 问一问推荐的塔罗牌阵，塔罗页出现时取走
    var pendingTarotSpread: TarotSpread?

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

    /// 收起决策浮层（不记录表态）
    func dismissDecisionPrompt() {
        lastDecision = nil
    }

    func takePendingWheelOptions() -> [String]? {
        guard let options = pendingWheelOptions else { return nil }
        pendingWheelOptions = nil
        return options
    }

    func takePendingTarotSpread() -> TarotSpread? {
        guard let spread = pendingTarotSpread else { return nil }
        pendingTarotSpread = nil
        return spread
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
