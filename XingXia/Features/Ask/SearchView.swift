import SwiftUI
import SwiftData

/// 搜索 tab：按名称、关键词和拼音首字母（如「yb」→ 抛硬币）搜全部工具、占卜、记录页和设置项。
/// 空查询时显示最近用过的工具。
struct SearchView: View {
    @Environment(AppState.self) private var app
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette

    @State private var query = ""
    @State private var recent: [SearchItem] = []

    var body: some View {
        let results = SearchCatalog.search(query)
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                if query.trimmingCharacters(in: .whitespaces).isEmpty {
                    if !recent.isEmpty {
                        section(title: "最近用过", symbol: "clock.arrow.circlepath", items: recent)
                    }
                    ForEach(SearchItem.Kind.allCases) { group in
                        section(title: group.name, symbol: group.symbol, items: SearchCatalog.all.filter { $0.group == group })
                    }
                } else if results.isEmpty {
                    EmptyHint(symbol: "magnifyingglass", title: "没有找到「\(query)」",
                              message: "试试「吃」「硬币」「签」，或拼音首字母，如「yb」")
                } else {
                    section(title: "找到 \(results.count) 项", symbol: "magnifyingglass", items: results)
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .scrollDismissesKeyboard(.interactively)
        .cosmicBackground()
        .navigationTitle("搜索")
        .searchable(text: $query, prompt: "工具、占卜、记录、设置")
        .onAppear { loadRecent() }
    }

    private func section(title: String, symbol: String, items: [SearchItem]) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: title, symbol: symbol)
            VStack(spacing: 0) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    if index > 0 { Divider().padding(.leading, 54) }
                    Button {
                        Haptics.selection()
                        open(item)
                    } label: {
                        HStack(spacing: 12) {
                            ToolIcon(symbol: item.symbol, colors: palette.identity(item.colors), size: 40)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.name).font(.headline).foregroundStyle(.primary)
                                Text(item.subtitle).font(.caption).subtleForeground().lineLimit(2)
                            }
                            Spacer(minLength: 0)
                            Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
                                .accessibilityHidden(true)
                        }
                        .padding(.vertical, 10)
                        .contentShape(.rect)
                    }
                    .buttonStyle(.plain)
                }
            }
            .glassCard(padding: 14)
        }
    }

    private func open(_ item: SearchItem) {
        switch item.target {
        case .route(.annualReview): app.open(.annualReview(year: AppRoute.reviewYear()))
        case .route(let route): app.open(route)
        case .tab(let tab): app.selectedTab = tab
        }
    }

    /// 最近 200 条决定里用过的工具，按最近一次使用排序，最多 6 个
    private func loadRecent() {
        var descriptor = FetchDescriptor<DecisionRecord>(sortBy: [SortDescriptor(\.date, order: .reverse)])
        descriptor.fetchLimit = 200
        let records = (try? context.fetch(descriptor)) ?? []
        var seen = Set<String>()
        var items: [SearchItem] = []
        for record in records {
            guard let item = SearchCatalog.item(forTool: record.tool), seen.insert(item.id).inserted else { continue }
            items.append(item)
            if items.count == 6 { break }
        }
        recent = items
    }
}

struct SearchItem: Identifiable {
    enum Target {
        case route(AppRoute)
        case tab(AppTab)
    }

    enum Kind: String, CaseIterable, Identifiable {
        case tools, divination, records, me

        var id: String { rawValue }
        var name: String {
            switch self {
            case .tools: "工具"
            case .divination: "占卜"
            case .records: "记录"
            case .me: "我的与设置"
            }
        }
        var symbol: String {
            switch self {
            case .tools: "square.grid.2x2.fill"
            case .divination: "moon.stars.fill"
            case .records: "text.book.closed.fill"
            case .me: "person.crop.circle.fill"
            }
        }
    }

    let id: String
    let group: Kind
    let name: String
    let subtitle: String
    let symbol: String
    let colors: [Color]
    let target: Target
    /// 额外的中文关键词（空格分隔）
    let keywords: String
    /// 名称和关键词的拼音首字母，每个词一段，如「pyb yb zf」
    let initials: [String]

    init(id: String, group: Kind, name: String, subtitle: String, symbol: String, colors: [Color], target: Target, keywords: String = "") {
        self.id = id
        self.group = group
        self.name = name
        self.subtitle = subtitle
        self.symbol = symbol
        self.colors = colors
        self.target = target
        self.keywords = keywords
        let words = [name] + keywords.split(separator: " ").map(String.init)
        initials = words.map(Pinyin.initials(of:))
    }

    func matches(_ query: String) -> Bool {
        if name.localizedCaseInsensitiveContains(query) || subtitle.localizedCaseInsensitiveContains(query)
            || keywords.localizedCaseInsensitiveContains(query) {
            return true
        }
        // 纯字母时按拼音首字母匹配
        let letters = query.lowercased().filter { !$0.isWhitespace }
        guard !letters.isEmpty, letters.allSatisfy({ $0.isASCII && $0.isLetter }) else { return false }
        return initials.contains { $0.contains(letters) }
    }
}

/// 拼音首字母：用系统的汉字转拼音，取每个音节的首字母（「抛硬币」→「pyb」，「3D 骰子」→「3tz」）
enum Pinyin {
    static func initials(of text: String) -> String {
        let latin = text.applyingTransform(.mandarinToLatin, reverse: false)?
            .applyingTransform(.stripDiacritics, reverse: false) ?? text
        return latin.lowercased()
            .split(whereSeparator: { $0.isWhitespace })
            .compactMap(\.first)
            .map(String.init)
            .joined()
    }
}

@MainActor
enum SearchCatalog {
    static let all: [SearchItem] = tools + divination + records + me

    static func search(_ query: String) -> [SearchItem] {
        let trimmed = query.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return [] }
        return all.filter { $0.matches(trimmed) }
    }

    /// 决策历史里的 tool 字段 → 搜索项（「最近用过」用）
    static func item(forTool tool: String) -> SearchItem? {
        let id = switch tool {
        case "nearby": ToolKind.food.rawValue
        case "jiaobei": "stick"
        default: tool
        }
        return all.first { $0.id == id }
    }

    private static let tools: [SearchItem] = ToolKind.allCases.map { tool in
        SearchItem(id: tool.rawValue, group: .tools, name: tool.name, subtitle: tool.subtitle, symbol: tool.symbol,
                   colors: tool.colors, target: .route(.tool(tool)), keywords: tool.keywords)
    }

    private static let divination: [SearchItem] = [
        SearchItem(id: "fortune", group: .divination, name: "运势详解", subtitle: "五维运势与幸运指南", symbol: "sparkles",
                   colors: [Color(hex: 0x8B5CF6), Color(hex: 0xEC4899)], target: .route(.fortune), keywords: "今日运势 运气 幸运色 幸运数字"),
        SearchItem(id: "stick", group: .divination, name: "求签", subtitle: "一日一签 · 解签 · 掷筊", symbol: "flame.fill",
                   colors: [Color(hex: 0xFF8A3D), Color(hex: 0xE11D48)], target: .route(.stick), keywords: "抽签 摇签 灵签 签文 掷筊 圣杯"),
        SearchItem(id: "tarot", group: .divination, name: "塔罗占卜", subtitle: "单张 · 时间之流 · 情感关系 · 问题解答", symbol: "suit.diamond.fill",
                   colors: [Color(hex: 0x8B5CF6), Color(hex: 0x3B82F6)], target: .route(.tarot), keywords: "塔罗牌 牌阵 占卜"),
        SearchItem(id: "answers", group: .divination, name: "答案之书", subtitle: "默念问题 · 翻开一页", symbol: "book.fill",
                   colors: [Color(hex: 0xF5B83D), Color(hex: 0xB45309)], target: .route(.answers), keywords: "答案 问题 是否"),
        SearchItem(id: "horoscope", group: .divination, name: "星座运势", subtitle: "今日 · 明日 · 本周 · 本月", symbol: "moon.stars.fill",
                   colors: [Color(hex: 0x6366F1), Color(hex: 0xA78BFA)], target: .route(.horoscope),
                   keywords: "星座 白羊 金牛 双子 巨蟹 狮子 处女 天秤 天蝎 射手 摩羯 水瓶 双鱼"),
        SearchItem(id: "almanac", group: .divination, name: "黄历", subtitle: "农历 · 节气 · 宜忌", symbol: "calendar.badge.clock",
                   colors: [Color(hex: 0xEF4444), Color(hex: 0xF59E0B)], target: .route(.almanac), keywords: "老黄历 农历 节气 宜忌 万年历"),
        SearchItem(id: "auspicious", group: .divination, name: "择吉日", subtitle: "搬家 · 开业 · 领证 · 出行", symbol: "calendar.badge.checkmark",
                   colors: [Color(hex: 0xEF4444), Color(hex: 0xF59E0B)], target: .route(.auspicious), keywords: "吉日 黄道吉日 好日子 搬家 结婚 开业"),
        SearchItem(id: "compatibility", group: .divination, name: "缘分配对", subtitle: "星座 + 生肖", symbol: "heart.circle.fill",
                   colors: [Color(hex: 0xFB7185), Color(hex: 0xF43F5E)], target: .route(.compatibility), keywords: "配对 合不合 缘分 速配"),
        SearchItem(id: "tarotJournal", group: .divination, name: "塔罗手记", subtitle: "回看抽过的牌", symbol: "book.closed.fill",
                   colors: [Color(hex: 0x8B5CF6), Color(hex: 0x3B82F6)], target: .route(.tarotJournal), keywords: "塔罗记录 解读"),
    ]

    private static let records: [SearchItem] = [
        SearchItem(id: "journal", group: .records, name: "日记", subtitle: "每天的心情、决定和签", symbol: "text.book.closed.fill",
                   colors: [Color(hex: 0x10B981), Color(hex: 0x22D3EE)], target: .tab(.journal), keywords: "时间线 回顾 本月"),
        SearchItem(id: "mood", group: .records, name: "心情", subtitle: "每天点一下心情", symbol: "face.smiling.inverse",
                   colors: [Color(hex: 0xFBBF24), Color(hex: 0xF97316)], target: .route(.mood), keywords: "心情打卡 情绪"),
        SearchItem(id: "countdowns", group: .records, name: "倒数日", subtitle: "纪念日 · 生日 · 重要的日子", symbol: "hourglass",
                   colors: [Color(hex: 0x60A5FA), Color(hex: 0x6366F1)], target: .route(.countdowns), keywords: "倒计时 纪念日 生日"),
        SearchItem(id: "history", group: .records, name: "决策历史", subtitle: "转过、抛过、抽过的每一次", symbol: "clock.arrow.circlepath",
                   colors: [Color(hex: 0x94A3B8), Color(hex: 0x6366F1)], target: .route(.history), keywords: "历史 记录 决定"),
        SearchItem(id: "fortuneCalendar", group: .records, name: "运势日历", subtitle: "一个月的运势起伏", symbol: "calendar",
                   colors: [Color(hex: 0x8B5CF6), Color(hex: 0xEC4899)], target: .route(.fortuneCalendar), keywords: "日历 运势 月历"),
        SearchItem(id: "annualReview", group: .records, name: "年度回顾", subtitle: "一年的决定、心情和好运", symbol: "chart.bar.xaxis",
                   colors: [Color(hex: 0xF59E0B), Color(hex: 0xEF4444)], target: .route(.annualReview(year: AppRoute.reviewYear())),
                   keywords: "年终总结 回顾 关键词"),
        SearchItem(id: "skyCalendar", group: .records, name: "天象日历", subtitle: "月相 · 节气 · 流星雨", symbol: "moon.circle.fill",
                   colors: [Color(hex: 0x312E81), Color(hex: 0x6366F1)], target: .route(.skyCalendar), keywords: "月相 满月 新月 流星雨 节气 天象"),
    ]

    private static let me: [SearchItem] = [
        SearchItem(id: "birthChart", group: .me, name: "命盘", subtitle: "八字 · 星盘三巨头", symbol: "circle.hexagongrid.fill",
                   colors: [Color(hex: 0xA78BFA), Color(hex: 0xEC4899)], target: .route(.birthChart), keywords: "八字 星盘 上升 月亮星座"),
        SearchItem(id: "profiles", group: .me, name: "档案", subtitle: "自己和家人的生日、出生地", symbol: "person.2.fill",
                   colors: [Color(hex: 0x60A5FA), Color(hex: 0x6366F1)], target: .route(.profiles), keywords: "家人 生日 出生地"),
        SearchItem(id: "themes", group: .me, name: "主题", subtitle: "配色 · 深浅色 · 按时段切换", symbol: "paintpalette.fill",
                   colors: [Color(hex: 0xF472B6), Color(hex: 0x8B5CF6)], target: .route(.themes), keywords: "外观 深色 浅色 配色 皮肤"),
        SearchItem(id: "backup", group: .me, name: "数据备份", subtitle: "导出、导入全部数据", symbol: "externaldrive.fill",
                   colors: [Color(hex: 0x2DD4BF), Color(hex: 0x0EA5E9)], target: .route(.backup), keywords: "备份 恢复 导出 导入 换手机"),
        SearchItem(id: "settings", group: .me, name: "设置", subtitle: "声音、触感、提醒等", symbol: "gearshape.fill",
                   colors: [Color(hex: 0x94A3B8), Color(hex: 0x475569)], target: .route(.settings), keywords: "偏好"),
        SearchItem(id: "settings.sound", group: .me, name: "音效与震动", subtitle: "设置 · 声音与触感", symbol: "speaker.wave.2.fill",
                   colors: [Color(hex: 0x94A3B8), Color(hex: 0x475569)], target: .route(.settings), keywords: "声音 静音 震动 触感 振动"),
        SearchItem(id: "settings.shake", group: .me, name: "摇一摇触发", subtitle: "设置 · 交互", symbol: "iphone.gen3.radiowaves.left.and.right",
                   colors: [Color(hex: 0x94A3B8), Color(hex: 0x475569)], target: .route(.settings), keywords: "摇一摇 星空背景 动画"),
        SearchItem(id: "settings.reminder", group: .me, name: "每日运势提醒", subtitle: "设置 · 提醒", symbol: "bell.badge.fill",
                   colors: [Color(hex: 0x94A3B8), Color(hex: 0x475569)], target: .route(.settings), keywords: "提醒 通知 推送 天象提醒"),
        SearchItem(id: "settings.ritual", group: .me, name: "每日仪式", subtitle: "设置 · 每天第一次打开时的小仪式", symbol: "sunrise.fill",
                   colors: [Color(hex: 0x94A3B8), Color(hex: 0x475569)], target: .route(.settings), keywords: "仪式 月相 今日卡片"),
    ]
}
