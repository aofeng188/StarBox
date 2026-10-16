import Foundation
import SwiftData
import WidgetKit

/// 备份里的数据类别；rawValue 就是 JSON 里的键名（与 Android 版一致）
enum BackupCategory: String, CaseIterable, Identifiable {
    case profiles, decisionRecords, stickDraws, tarotReadings, moods, countdowns, wheelTemplates, foodMenus,
         foodLogs, blockedFoods, lotteryPools, rosters, customPrompts, scratchPresets

    var id: String { rawValue }

    var name: String {
        switch self {
        case .profiles: "档案"
        case .decisionRecords: "决策记录"
        case .stickDraws: "每日一签"
        case .tarotReadings: "塔罗手记"
        case .moods: "心情打卡"
        case .countdowns: "倒数日"
        case .wheelTemplates: "转盘"
        case .foodMenus: "菜单"
        case .foodLogs: "吃过的"
        case .blockedFoods: "不想吃"
        case .lotteryPools: "抽奖池"
        case .rosters: "分组名单"
        case .customPrompts: "自定义题目"
        case .scratchPresets: "刮刮卡"
        }
    }

    var unit: String {
        switch self {
        case .profiles, .countdowns, .wheelTemplates, .foodMenus, .lotteryPools, .rosters, .scratchPresets: "个"
        case .decisionRecords, .stickDraws, .foodLogs: "条"
        case .tarotReadings: "次"
        case .moods: "天"
        case .blockedFoods: "样"
        case .customPrompts: "道"
        }
    }
}

enum BackupMode {
    /// 按 id 去重，已有的保留
    case merge
    /// 文件里有的类别先清空再导入；文件里没有的类别不动
    case replace
}

enum BackupError: LocalizedError {
    case unreadable, notBackup, newerVersion, tooLarge, exportFailed

    var errorDescription: String? {
        switch self {
        case .unreadable: "文件读不出来，可能不是 JSON 或已经损坏。"
        case .notBackup: "这不是星匣导出的备份文件。"
        case .newerVersion: "这份备份来自更新版本的星匣，请先更新 App 再恢复。"
        case .tooLarge: "文件太大，不像是星匣的备份。"
        case .exportFailed: "生成备份文件失败，请稍后再试。"
        }
    }
}

/// 备份里的偏好设置：首页快捷工具收藏和首页模块
struct BackupSettings {
    var favoriteTools: [String]
    var homeCards: [String]
    var hiddenHomeCards: [String]

    @MainActor
    static var current: BackupSettings {
        let settings = AppSettings.shared
        return BackupSettings(favoriteTools: settings.favoriteTools,
                              homeCards: settings.homeCards.map(\.rawValue),
                              hiddenHomeCards: HomeCard.allCases.filter { settings.hiddenHomeCards.contains($0) }.map(\.rawValue))
    }
}

/// 解析好、还没导入的备份文件
struct BackupFile {
    let version: Int
    /// "ios" / "android"
    let platform: String
    let exportedAt: Date?
    /// 只含文件里出现的类别
    fileprivate let sections: [BackupCategory: [BackupFields]]
    fileprivate let settings: BackupFields?

    var platformName: String {
        switch platform {
        case "ios": "iPhone"
        case "android": "Android"
        default: "未知设备"
        }
    }

    /// 文件里有这一类时返回条数，没有这一类返回 nil
    func count(of category: BackupCategory) -> Int? { sections[category]?.count }

    var totalCount: Int { sections.values.reduce(0) { $0 + $1.count } }

    var includesSettings: Bool { settings != nil }
}

struct BackupRestoreResult {
    var added: [BackupCategory: Int] = [:]
    /// 已有、重复或数据不完整而跳过的条数
    var skipped = 0

    var totalAdded: Int { added.values.reduce(0, +) }
}

/// 数据备份：导出全部 SwiftData 数据和首页偏好为 JSON（与 Android 版格式互通），导入时合并或替换。
enum Backup {
    static let format = "xingxia-backup"
    static let version = 1
    /// 上次导出时间（UserDefaults，秒），页面用 @AppStorage 读
    static let lastExportKey = "backupLastExportAt"

    /// 导出时的默认文件名，扩展名由系统按 JSON 类型补上
    static var defaultFileName: String { "星匣备份-\(DayKey.today.rawValue)" }

    static var lastExportAt: Date? {
        get {
            let value = UserDefaults.standard.double(forKey: lastExportKey)
            return value > 0 ? Date(timeIntervalSince1970: value) : nil
        }
        set { UserDefaults.standard.set(newValue?.timeIntervalSince1970 ?? 0, forKey: lastExportKey) }
    }

    // MARK: - 导出

    @MainActor
    static func export(from context: ModelContext, settings: BackupSettings) throws -> Data {
        let profiles = try context.fetch(FetchDescriptor<Profile>(sortBy: [SortDescriptor(\.sortIndex)]))
        let records = try context.fetch(FetchDescriptor<DecisionRecord>(sortBy: [SortDescriptor(\.date)]))
        let draws = try context.fetch(FetchDescriptor<StickDraw>(sortBy: [SortDescriptor(\.date)]))
        let readings = try context.fetch(FetchDescriptor<TarotReading>(sortBy: [SortDescriptor(\.date)]))
        let moods = try context.fetch(FetchDescriptor<MoodEntry>(sortBy: [SortDescriptor(\.dayKey)]))
        let countdowns = try context.fetch(FetchDescriptor<Countdown>(sortBy: [SortDescriptor(\.createdAt)]))
        let wheels = try context.fetch(FetchDescriptor<WheelTemplate>(sortBy: [SortDescriptor(\.createdAt)]))
        let menus = try context.fetch(FetchDescriptor<FoodMenu>(sortBy: [SortDescriptor(\.createdAt)]))
        let foodLogs = try context.fetch(FetchDescriptor<FoodLog>(sortBy: [SortDescriptor(\.date)]))
        let blocked = try context.fetch(FetchDescriptor<BlockedFood>(sortBy: [SortDescriptor(\.date)]))
        let pools = try context.fetch(FetchDescriptor<LotteryPool>(sortBy: [SortDescriptor(\.createdAt)]))
        let rosters = try context.fetch(FetchDescriptor<Roster>(sortBy: [SortDescriptor(\.createdAt)]))
        let prompts = try context.fetch(FetchDescriptor<CustomPrompt>(sortBy: [SortDescriptor(\.createdAt)]))
        let presets = try context.fetch(FetchDescriptor<ScratchPreset>(sortBy: [SortDescriptor(\.createdAt)]))

        var root: [String: Any] = [
            "format": format,
            "version": version,
            "platform": "ios",
            "exportedAt": ms(Date()),
        ]
        root[BackupCategory.profiles.rawValue] = profiles.enumerated().map { (index, p) -> [String: Any] in
            [
                "id": idString(p.id), "name": p.name, "relation": p.relation, "avatar": p.avatar, "colorIndex": p.colorIndex,
                "birthday": DayKey(p.birthday).rawValue, "hasBirthTime": p.hasBirthTime,
                "birthHour": p.birthHour, "birthMinute": p.birthMinute, "birthCityId": nullable(p.birthCityID),
                "enteredAsLunar": p.enteredAsLunar, "sortIndex": index, "createdAt": ms(p.createdAt),
            ]
        }
        root[BackupCategory.decisionRecords.rawValue] = records.map { r -> [String: Any] in
            ["id": idString(r.id), "tool": r.tool, "title": r.title, "result": r.result, "detail": nullable(r.detail), "date": ms(r.date)]
        }
        root[BackupCategory.stickDraws.rawValue] = draws.map { d -> [String: Any] in
            ["dayKey": d.dayKey, "profileId": nullable(d.profileID.map(idString)), "stickNumber": d.stickNumber, "date": ms(d.date)]
        }
        root[BackupCategory.tarotReadings.rawValue] = readings.map { r -> [String: Any] in
            ["id": idString(r.id), "date": ms(r.date), "spread": r.spread, "cards": r.cards, "question": r.question]
        }
        root[BackupCategory.moods.rawValue] = moods.map { m -> [String: Any] in
            ["dayKey": m.dayKey, "mood": m.mood, "note": m.note, "tags": m.tags, "date": ms(m.date)]
        }
        root[BackupCategory.countdowns.rawValue] = countdowns.map { c -> [String: Any] in
            [
                "id": idString(c.id), "title": c.title, "emoji": c.emoji, "date": DayKey(c.date).rawValue, "kind": c.kind,
                "repeatsYearly": c.repeatsYearly, "isLunar": c.isLunar, "lunarMonth": c.lunarMonth, "lunarDay": c.lunarDay,
                "colorIndex": c.colorIndex, "pinned": c.pinned, "createdAt": ms(c.createdAt),
            ]
        }
        root[BackupCategory.wheelTemplates.rawValue] = wheels.map { w -> [String: Any] in
            [
                "id": idString(w.id), "name": w.name, "removeAfterSpin": w.removeAfterSpin, "createdAt": ms(w.createdAt),
                "options": w.sortedOptions.map { o -> [String: Any] in
                    ["id": idString(o.id), "title": o.title, "weight": finite(o.weight)]
                },
            ]
        }
        root[BackupCategory.foodMenus.rawValue] = menus.map { m -> [String: Any] in
            [
                "id": idString(m.id), "name": m.name, "icon": m.icon, "createdAt": ms(m.createdAt),
                "items": m.sortedItems.map { i -> [String: Any] in
                    ["id": idString(i.id), "name": i.name, "note": i.note, "weight": finite(i.weight)]
                },
            ]
        }
        root[BackupCategory.foodLogs.rawValue] = foodLogs.map { f -> [String: Any] in
            ["name": f.name, "source": f.source, "date": ms(f.date)]
        }
        root[BackupCategory.blockedFoods.rawValue] = blocked.map { b -> [String: Any] in
            ["name": b.name, "date": ms(b.date)]
        }
        root[BackupCategory.lotteryPools.rawValue] = pools.map { p -> [String: Any] in
            [
                "id": idString(p.id), "name": p.name, "icon": p.icon, "removeAfterDraw": p.removeAfterDraw, "createdAt": ms(p.createdAt),
                "items": p.sortedItems.map { i -> [String: Any] in
                    ["id": idString(i.id), "name": i.name, "weight": finite(i.weight), "quantity": i.quantity, "remaining": i.remaining]
                },
            ]
        }
        root[BackupCategory.rosters.rawValue] = rosters.map { r -> [String: Any] in
            ["id": idString(r.id), "name": r.name, "members": r.members, "createdAt": ms(r.createdAt)]
        }
        root[BackupCategory.customPrompts.rawValue] = prompts.map { p -> [String: Any] in
            ["id": idString(p.id), "kind": p.kind, "text": p.text, "level": p.level, "createdAt": ms(p.createdAt)]
        }
        root[BackupCategory.scratchPresets.rawValue] = presets.map { p -> [String: Any] in
            ["id": idString(p.id), "name": p.name, "prizes": p.prizes, "blankWeight": finite(p.blankWeight), "createdAt": ms(p.createdAt)]
        }
        root["settings"] = [
            "favoriteTools": settings.favoriteTools,
            "homeCards": settings.homeCards,
            "hiddenHomeCards": settings.hiddenHomeCards,
        ] as [String: Any]

        // NaN 等非法值会让 JSONSerialization 直接抛 Objective-C 异常，先检查
        guard JSONSerialization.isValidJSONObject(root) else { throw BackupError.exportFailed }
        return try JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted, .sortedKeys, .withoutEscapingSlashes])
    }

    private static func ms(_ date: Date) -> Int {
        let value = (date.timeIntervalSince1970 * 1000).rounded()
        return value.isFinite && abs(value) < 9e15 ? Int(value) : 0
    }

    /// id 一律导出成小写 UUID 字符串
    private static func idString(_ id: UUID) -> String { id.uuidString.lowercased() }

    private static func nullable(_ value: String?) -> Any {
        if let value { return value }
        return NSNull()
    }

    private static func finite(_ value: Double) -> Double { value.isFinite ? value : 1 }

    /// 本机各类数据的条数（备份页展示用）
    @MainActor
    static func categoryCounts(in context: ModelContext) -> [BackupCategory: Int] {
        func n<T: PersistentModel>(_ type: T.Type) -> Int { (try? context.fetchCount(FetchDescriptor<T>())) ?? 0 }
        return [
            .profiles: n(Profile.self), .decisionRecords: n(DecisionRecord.self), .stickDraws: n(StickDraw.self),
            .tarotReadings: n(TarotReading.self), .moods: n(MoodEntry.self), .countdowns: n(Countdown.self),
            .wheelTemplates: n(WheelTemplate.self), .foodMenus: n(FoodMenu.self), .foodLogs: n(FoodLog.self),
            .blockedFoods: n(BlockedFood.self), .lotteryPools: n(LotteryPool.self), .rosters: n(Roster.self),
            .customPrompts: n(CustomPrompt.self), .scratchPresets: n(ScratchPreset.self),
        ]
    }

    // MARK: - 解析

    static func parse(_ data: Data) throws -> BackupFile {
        guard data.count <= 64 * 1024 * 1024 else { throw BackupError.tooLarge }
        guard let object = try? JSONSerialization.jsonObject(with: data), let root = object as? [String: Any] else {
            throw BackupError.unreadable
        }
        let fields = BackupFields(root)
        guard fields.string("format") == format else { throw BackupError.notBackup }
        let fileVersion = fields.int("version") ?? version
        guard fileVersion <= version else { throw BackupError.newerVersion }
        var sections: [BackupCategory: [BackupFields]] = [:]
        for category in BackupCategory.allCases where root[category.rawValue] is [Any] {
            sections[category] = fields.objects(category.rawValue)
        }
        let settings = (root["settings"] as? [String: Any]).map(BackupFields.init)
        return BackupFile(version: fileVersion, platform: fields.string("platform") ?? "",
                          exportedAt: fields.date("exportedAt"), sections: sections, settings: settings)
    }

    // MARK: - 导入

    /// 导入到 context 并保存；出错时撤销本次所有改动。调用方随后应调用 `didRestore(context:)`。
    @MainActor
    @discardableResult
    static func restore(_ file: BackupFile, mode: BackupMode, into context: ModelContext, applySettings: Bool = true) throws -> BackupRestoreResult {
        let restorer = BackupRestorer(context: context, mode: mode)
        do {
            try restorer.run(file)
            try context.save()
        } catch {
            context.rollback()
            throw error
        }
        if applySettings, let settings = file.settings { apply(settings, mode: mode) }
        return restorer.result
    }

    /// 导入后：刷新当前档案（原来的还在就沿用，否则取第一个）、主题、提醒和小组件
    @MainActor
    static func didRestore(context: ModelContext) {
        let profiles = (try? context.fetch(FetchDescriptor<Profile>(sortBy: [SortDescriptor(\.sortIndex)]))) ?? []
        let currentID = AppState.shared.primary?.id
        AppState.shared.setPrimary(profiles.first(where: { $0.id == currentID }) ?? profiles.first)
        WidgetCenter.shared.reloadAllTimelines()
    }

    /// 合并时只补充收藏；替换时收藏、首页模块顺序和隐藏模块都用备份里的
    @MainActor
    private static func apply(_ fields: BackupFields, mode: BackupMode) {
        let settings = AppSettings.shared
        if let favorites = fields.strings("favoriteTools") {
            let valid = favorites.filter { FavoriteTool(id: $0) != nil }
            settings.favoriteTools = unique(mode == .merge ? settings.favoriteTools + valid : valid)
        }
        guard mode == .replace else { return }
        if let cards = fields.strings("homeCards") {
            settings.homeCards = withMissingCards(unique(cards.compactMap(HomeCard.init(rawValue:))))
        }
        if let hidden = fields.strings("hiddenHomeCards") {
            settings.hiddenHomeCards = Set(hidden.compactMap(HomeCard.init(rawValue:)))
        }
    }

    /// 备份里没有的模块（新版本加的、或另一端没有的）按默认顺序插到它前一个模块后面
    private static func withMissingCards(_ stored: [HomeCard]) -> [HomeCard] {
        var result = stored
        let defaults = HomeCard.allCases
        for (index, card) in defaults.enumerated() where !result.contains(card) {
            if let previous = defaults[..<index].last(where: { result.contains($0) }),
               let position = result.firstIndex(of: previous) {
                result.insert(card, at: position + 1)
            } else {
                result.insert(card, at: 0)
            }
        }
        return result
    }

    fileprivate static func unique<T: Hashable>(_ items: [T]) -> [T] {
        var seen = Set<T>()
        return items.filter { seen.insert($0).inserted }
    }
}

// MARK: - 宽松读取

/// JSON 对象的宽松读取：缺字段或类型不对都返回 nil，由调用方给默认值，坏数据不会崩溃
fileprivate struct BackupFields {
    let raw: [String: Any]

    init(_ raw: [String: Any]) { self.raw = raw }

    private static func isBool(_ number: NSNumber) -> Bool {
        CFGetTypeID(number) == CFBooleanGetTypeID()
    }

    func string(_ key: String) -> String? { raw[key] as? String }

    func number(_ key: String) -> Double? {
        if let number = raw[key] as? NSNumber, !Self.isBool(number) {
            let value = number.doubleValue
            return value.isFinite ? value : nil
        }
        if let text = raw[key] as? String, let value = Double(text.trimmingCharacters(in: .whitespaces)), value.isFinite {
            return value
        }
        return nil
    }

    func int(_ key: String) -> Int? {
        guard let value = number(key), abs(value) < 9e15 else { return nil }
        return Int(value.rounded(.towardZero))
    }

    func bool(_ key: String) -> Bool? {
        if let number = raw[key] as? NSNumber { return number.boolValue }
        guard let text = raw[key] as? String else { return nil }
        switch text.lowercased() {
        case "true", "1": return true
        case "false", "0": return false
        default: return nil
        }
    }

    /// 毫秒时间戳
    func date(_ key: String) -> Date? {
        guard let value = number(key), value > 0, value < 32_503_680_000_000 else { return nil }
        return Date(timeIntervalSince1970: value / 1000)
    }

    /// yyyyMMdd 整数，日期不存在时返回 nil
    func dayKey(_ key: String) -> DayKey? {
        guard let value = int(key) else { return nil }
        let year = value / 10000, month = value / 100 % 100, day = value % 100
        guard (1800...2300).contains(year), (1...12).contains(month) else { return nil }
        let isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
        let lengths = [31, isLeap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31]
        guard (1...lengths[month - 1]).contains(day) else { return nil }
        return DayKey(year: year, month: month, day: day)
    }

    /// UUID 字符串，不区分大小写
    func uuid(_ key: String) -> UUID? {
        string(key).flatMap { UUID(uuidString: $0.trimmingCharacters(in: .whitespaces)) }
    }

    func strings(_ key: String) -> [String]? {
        (raw[key] as? [Any])?.compactMap { $0 as? String }
    }

    func objects(_ key: String) -> [BackupFields] {
        ((raw[key] as? [Any]) ?? []).compactMap { ($0 as? [String: Any]).map(BackupFields.init) }
    }
}

// MARK: - 导入实现

/// 逐类导入。合并时按 id 去重；内容完全相同的（例如两端各自生成的示例转盘、同名同生日的档案）也视为已有，不重复导入。
@MainActor
private final class BackupRestorer {
    let context: ModelContext
    let mode: BackupMode
    private(set) var result = BackupRestoreResult()
    /// 与已有档案是同一个人（名字、生日相同）时：备份里的 id → 已有的 id，每日一签跟着改过来
    private var profileRemap: [UUID: UUID] = [:]

    init(context: ModelContext, mode: BackupMode) {
        self.context = context
        self.mode = mode
    }

    func run(_ file: BackupFile) throws {
        // 档案先导入，每日一签要用 id 映射
        if let items = file.sections[.profiles] { try profiles(items) }
        if let items = file.sections[.decisionRecords] { try decisionRecords(items) }
        if let items = file.sections[.stickDraws] { try stickDraws(items) }
        if let items = file.sections[.tarotReadings] { try tarotReadings(items) }
        if let items = file.sections[.moods] { try moods(items) }
        if let items = file.sections[.countdowns] { try countdowns(items) }
        if let items = file.sections[.wheelTemplates] { try wheelTemplates(items) }
        if let items = file.sections[.foodMenus] { try foodMenus(items) }
        if let items = file.sections[.foodLogs] { try foodLogs(items) }
        if let items = file.sections[.blockedFoods] { try blockedFoods(items) }
        if let items = file.sections[.lotteryPools] { try lotteryPools(items) }
        if let items = file.sections[.rosters] { try rosters(items) }
        if let items = file.sections[.customPrompts] { try customPrompts(items) }
        if let items = file.sections[.scratchPresets] { try scratchPresets(items) }
    }

    // MARK: 工具

    /// 替换模式下删掉这一类的全部已有数据（逐个删，保证级联规则生效）
    private func clear<T: PersistentModel>(_ type: T.Type) throws {
        guard mode == .replace else { return }
        for item in try context.fetch(FetchDescriptor<T>()) { context.delete(item) }
    }

    /// 合并模式下的已有数据；替换模式下已清空，返回空
    private func existing<T: PersistentModel>(_ type: T.Type) throws -> [T] {
        guard mode == .merge else { return [] }
        return try context.fetch(FetchDescriptor<T>())
    }

    private func added(_ category: BackupCategory) { result.added[category, default: 0] += 1 }
    private func skipped() { result.skipped += 1 }

    private func signature(_ parts: [String]) -> String { parts.joined(separator: "\u{1F}") }

    private func clamp(_ value: Int, _ range: ClosedRange<Int>) -> Int { min(max(value, range.lowerBound), range.upperBound) }

    /// 权重：必须是正数，否则按 1
    private func weight(_ value: Double?) -> Double {
        guard let value, value > 0 else { return 1 }
        return value
    }

    // MARK: 各类数据

    private func profiles(_ items: [BackupFields]) throws {
        try clear(Profile.self)
        let old = try existing(Profile.self)
        var ids: Set<UUID> = Set(old.map { $0.id })
        var people: [String: UUID] = [:]
        for profile in old { people[signature([profile.name, String(DayKey(profile.birthday).rawValue)])] = profile.id }
        var nextIndex = (old.map(\.sortIndex).max() ?? -1) + 1
        // 按备份里的 sortIndex（没有就按数组位置）排好，导入时重新编号
        let ordered = items.enumerated()
            .sorted { ($0.element.int("sortIndex") ?? $0.offset, $0.offset) < ($1.element.int("sortIndex") ?? $1.offset, $1.offset) }
            .map { $0.element }
        for f in ordered {
            let id = f.uuid("id") ?? UUID()
            let name = f.string("name") ?? ""
            let birthday = f.dayKey("birthday") ?? DayKey(year: 1998, month: 6, day: 18)
            let person = signature([name, String(birthday.rawValue)])
            if ids.contains(id) { skipped(); continue }
            if let same = people[person] {
                profileRemap[id] = same
                skipped()
                continue
            }
            let profile = Profile(name: name, relation: f.string("relation") ?? "自己", avatar: f.string("avatar") ?? "🌟", birthday: birthday.date)
            profile.id = id
            profile.colorIndex = f.int("colorIndex") ?? 0
            profile.hasBirthTime = f.bool("hasBirthTime") ?? false
            profile.birthHour = clamp(f.int("birthHour") ?? 12, 0...23)
            profile.birthMinute = clamp(f.int("birthMinute") ?? 0, 0...59)
            profile.birthCityID = f.string("birthCityId").flatMap { $0.isEmpty ? nil : $0 }
            profile.enteredAsLunar = f.bool("enteredAsLunar") ?? false
            profile.sortIndex = nextIndex
            nextIndex += 1
            profile.createdAt = f.date("createdAt") ?? Date()
            context.insert(profile)
            ids.insert(id)
            people[person] = id
            added(.profiles)
        }
    }

    private func decisionRecords(_ items: [BackupFields]) throws {
        try clear(DecisionRecord.self)
        var ids: Set<UUID> = try Set(existing(DecisionRecord.self).map { $0.id })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            guard ids.insert(id).inserted else { skipped(); continue }
            let record = DecisionRecord(tool: f.string("tool") ?? "", title: f.string("title") ?? "", result: f.string("result") ?? "",
                                        detail: f.string("detail"), date: f.date("date") ?? Date())
            record.id = id
            context.insert(record)
            added(.decisionRecords)
        }
    }

    private func stickDraws(_ items: [BackupFields]) throws {
        try clear(StickDraw.self)
        func key(_ day: Int, _ owner: UUID?) -> String { "\(day)|\(owner?.uuidString ?? "")" }
        var keys = try Set(existing(StickDraw.self).map { key($0.dayKey, $0.profileID) })
        for f in items {
            guard let day = f.dayKey("dayKey"), let number = f.int("stickNumber"), number >= 1 else { skipped(); continue }
            var owner = f.uuid("profileId")
            if let id = owner, let mapped = profileRemap[id] { owner = mapped }
            // 每人每天一签
            guard keys.insert(key(day.rawValue, owner)).inserted else { skipped(); continue }
            let draw = StickDraw(dayKey: day.rawValue, profileID: owner, stickNumber: number)
            draw.date = f.date("date") ?? day.date
            context.insert(draw)
            added(.stickDraws)
        }
    }

    private func tarotReadings(_ items: [BackupFields]) throws {
        try clear(TarotReading.self)
        var ids: Set<UUID> = try Set(existing(TarotReading.self).map { $0.id })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            guard ids.insert(id).inserted else { skipped(); continue }
            let reading = TarotReading(spread: f.string("spread") ?? "", cards: f.string("cards") ?? "", question: f.string("question") ?? "")
            reading.id = id
            reading.date = f.date("date") ?? Date()
            context.insert(reading)
            added(.tarotReadings)
        }
    }

    private func moods(_ items: [BackupFields]) throws {
        try clear(MoodEntry.self)
        var days = try Set(existing(MoodEntry.self).map(\.dayKey))
        for f in items {
            // 每天一条
            guard let day = f.dayKey("dayKey"), days.insert(day.rawValue).inserted else { skipped(); continue }
            let entry = MoodEntry(dayKey: day.rawValue, mood: clamp(f.int("mood") ?? 3, 1...5), note: f.string("note") ?? "")
            entry.tags = f.strings("tags") ?? []
            entry.date = f.date("date") ?? day.date
            context.insert(entry)
            added(.moods)
        }
    }

    /// 倒数日的内容特征：重复的日子只看月日（农历看农历月日），不重复的看完整日期
    private func countdownSignature(title: String, kind: String, day: DayKey, repeats: Bool, isLunar: Bool, lunarMonth: Int, lunarDay: Int) -> String {
        let repeating = (repeats || kind == CountdownKind.birthday.rawValue) && kind != CountdownKind.anniversary.rawValue
        let when: String
        if isLunar && kind != CountdownKind.anniversary.rawValue {
            when = repeating ? "L\(lunarMonth)-\(lunarDay)" : "L\(lunarMonth)-\(lunarDay)@\(day.rawValue)"
        } else {
            when = repeating ? "\(day.month)-\(day.day)" : "\(day.rawValue)"
        }
        return signature([kind, title, when])
    }

    private func countdowns(_ items: [BackupFields]) throws {
        try clear(Countdown.self)
        let old = try existing(Countdown.self)
        var ids: Set<UUID> = Set(old.map { $0.id })
        var contents = Set(old.map {
            countdownSignature(title: $0.title, kind: $0.kind, day: DayKey($0.date), repeats: $0.repeatsYearly,
                               isLunar: $0.isLunar, lunarMonth: $0.lunarMonth, lunarDay: $0.lunarDay)
        })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            let title = f.string("title") ?? ""
            let kind = CountdownKind(rawValue: f.string("kind") ?? "")?.rawValue ?? CountdownKind.countdown.rawValue
            let createdAt = f.date("createdAt") ?? Date()
            let day = f.dayKey("date") ?? DayKey(createdAt)
            let repeats = f.bool("repeatsYearly") ?? false
            let isLunar = f.bool("isLunar") ?? false
            let lunarMonth = clamp(f.int("lunarMonth") ?? 1, 1...12)
            let lunarDay = clamp(f.int("lunarDay") ?? 1, 1...30)
            let content = countdownSignature(title: title, kind: kind, day: day, repeats: repeats,
                                             isLunar: isLunar, lunarMonth: lunarMonth, lunarDay: lunarDay)
            guard !ids.contains(id), !contents.contains(content) else { skipped(); continue }
            ids.insert(id)
            contents.insert(content)
            let countdown = Countdown(title: title, emoji: f.string("emoji") ?? "🎉", date: day.date, kind: kind)
            countdown.id = id
            countdown.repeatsYearly = repeats
            countdown.isLunar = isLunar
            countdown.lunarMonth = lunarMonth
            countdown.lunarDay = lunarDay
            countdown.colorIndex = f.int("colorIndex") ?? 0
            countdown.pinned = f.bool("pinned") ?? false
            countdown.createdAt = createdAt
            context.insert(countdown)
            added(.countdowns)
        }
    }

    private func wheelTemplates(_ items: [BackupFields]) throws {
        try clear(WheelOption.self)
        try clear(WheelTemplate.self)
        let old = try existing(WheelTemplate.self)
        var ids: Set<UUID> = Set(old.map { $0.id })
        var contents = Set(old.map { signature([$0.name] + $0.sortedOptions.map(\.title)) })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            let name = f.string("name") ?? ""
            let options = f.objects("options")
            let content = signature([name] + options.map { $0.string("title") ?? "" })
            guard !ids.contains(id), !contents.contains(content) else { skipped(); continue }
            ids.insert(id)
            contents.insert(content)
            let template = WheelTemplate(name: name)
            template.id = id
            template.removeAfterSpin = f.bool("removeAfterSpin") ?? false
            template.createdAt = f.date("createdAt") ?? Date()
            context.insert(template)
            for (index, o) in options.enumerated() {
                let option = WheelOption(title: o.string("title") ?? "", weight: weight(o.number("weight")), sortIndex: index)
                option.id = o.uuid("id") ?? UUID()
                option.template = template
                context.insert(option)
            }
            added(.wheelTemplates)
        }
    }

    private func foodMenus(_ items: [BackupFields]) throws {
        try clear(FoodMenuItem.self)
        try clear(FoodMenu.self)
        let old = try existing(FoodMenu.self)
        var ids: Set<UUID> = Set(old.map { $0.id })
        var contents = Set(old.map { signature([$0.name] + $0.sortedItems.map(\.name)) })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            let name = f.string("name") ?? ""
            let entries = f.objects("items")
            let content = signature([name] + entries.map { $0.string("name") ?? "" })
            guard !ids.contains(id), !contents.contains(content) else { skipped(); continue }
            ids.insert(id)
            contents.insert(content)
            let menu = FoodMenu(name: name, icon: f.string("icon") ?? "🍱")
            menu.id = id
            menu.createdAt = f.date("createdAt") ?? Date()
            context.insert(menu)
            for (index, e) in entries.enumerated() {
                let item = FoodMenuItem(name: e.string("name") ?? "", note: e.string("note") ?? "", weight: weight(e.number("weight")), sortIndex: index)
                item.id = e.uuid("id") ?? UUID()
                item.menu = menu
                context.insert(item)
            }
            added(.foodMenus)
        }
    }

    private func foodLogs(_ items: [BackupFields]) throws {
        try clear(FoodLog.self)
        func key(_ name: String, _ source: String, _ date: Date) -> String { signature([name, source, String(Int(date.timeIntervalSince1970))]) }
        var keys = try Set(existing(FoodLog.self).map { key($0.name, $0.source, $0.date) })
        for f in items {
            guard let name = f.string("name"), !name.isEmpty else { skipped(); continue }
            let source = f.string("source") ?? ""
            let date = f.date("date") ?? Date()
            guard keys.insert(key(name, source, date)).inserted else { skipped(); continue }
            let log = FoodLog(name: name, source: source)
            log.date = date
            context.insert(log)
            added(.foodLogs)
        }
    }

    private func blockedFoods(_ items: [BackupFields]) throws {
        try clear(BlockedFood.self)
        var names = try Set(existing(BlockedFood.self).map(\.name))
        for f in items {
            guard let name = f.string("name"), !name.isEmpty, names.insert(name).inserted else { skipped(); continue }
            let food = BlockedFood(name: name)
            food.date = f.date("date") ?? Date()
            context.insert(food)
            added(.blockedFoods)
        }
    }

    private func lotteryPools(_ items: [BackupFields]) throws {
        try clear(LotteryItem.self)
        try clear(LotteryPool.self)
        let old = try existing(LotteryPool.self)
        var ids: Set<UUID> = Set(old.map { $0.id })
        var contents = Set(old.map { signature([$0.name] + $0.sortedItems.map(\.name)) })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            let name = f.string("name") ?? ""
            let entries = f.objects("items")
            let content = signature([name] + entries.map { $0.string("name") ?? "" })
            guard !ids.contains(id), !contents.contains(content) else { skipped(); continue }
            ids.insert(id)
            contents.insert(content)
            let pool = LotteryPool(name: name, icon: f.string("icon") ?? "🎁", removeAfterDraw: f.bool("removeAfterDraw") ?? true)
            pool.id = id
            pool.createdAt = f.date("createdAt") ?? Date()
            context.insert(pool)
            for (index, e) in entries.enumerated() {
                let quantity = max(e.int("quantity") ?? 1, 0)
                let item = LotteryItem(name: e.string("name") ?? "", weight: weight(e.number("weight")), quantity: quantity, sortIndex: index)
                item.id = e.uuid("id") ?? UUID()
                item.remaining = clamp(e.int("remaining") ?? quantity, 0...quantity)
                item.pool = pool
                context.insert(item)
            }
            added(.lotteryPools)
        }
    }

    private func rosters(_ items: [BackupFields]) throws {
        try clear(Roster.self)
        let old = try existing(Roster.self)
        var ids: Set<UUID> = Set(old.map { $0.id })
        var contents = Set(old.map { signature([$0.name] + $0.members) })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            let name = f.string("name") ?? ""
            let members = f.strings("members") ?? []
            let content = signature([name] + members)
            guard !ids.contains(id), !contents.contains(content) else { skipped(); continue }
            ids.insert(id)
            contents.insert(content)
            let roster = Roster(name: name, members: members)
            roster.id = id
            roster.createdAt = f.date("createdAt") ?? Date()
            context.insert(roster)
            added(.rosters)
        }
    }

    private func customPrompts(_ items: [BackupFields]) throws {
        try clear(CustomPrompt.self)
        let old = try existing(CustomPrompt.self)
        var ids: Set<UUID> = Set(old.map { $0.id })
        var contents = Set(old.map { signature([$0.kind, $0.text]) })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            let kind = f.string("kind") == "dare" ? "dare" : "truth"
            guard let text = f.string("text"), !text.isEmpty else { skipped(); continue }
            let content = signature([kind, text])
            guard !ids.contains(id), !contents.contains(content) else { skipped(); continue }
            ids.insert(id)
            contents.insert(content)
            let prompt = CustomPrompt(kind: kind, text: text, level: f.int("level") ?? 0)
            prompt.id = id
            prompt.createdAt = f.date("createdAt") ?? Date()
            context.insert(prompt)
            added(.customPrompts)
        }
    }

    private func scratchPresets(_ items: [BackupFields]) throws {
        try clear(ScratchPreset.self)
        let old = try existing(ScratchPreset.self)
        var ids: Set<UUID> = Set(old.map { $0.id })
        var contents = Set(old.map { signature([$0.name] + $0.prizes) })
        for f in items {
            let id = f.uuid("id") ?? UUID()
            let name = f.string("name") ?? ""
            let prizes = f.strings("prizes") ?? []
            let content = signature([name] + prizes)
            guard !ids.contains(id), !contents.contains(content) else { skipped(); continue }
            ids.insert(id)
            contents.insert(content)
            let blank = f.number("blankWeight") ?? 1
            let preset = ScratchPreset(name: name, prizes: prizes, blankWeight: blank >= 0 ? blank : 1)
            preset.id = id
            preset.createdAt = f.date("createdAt") ?? Date()
            context.insert(preset)
            added(.scratchPresets)
        }
    }
}

// MARK: - 自检

#if DEBUG
extension Backup {
    /// 自检：在内存数据库里造一份样例数据，导出后用「替换」导入另一个内存数据库比对条数和关键字段，
    /// 再用「合并」导入一次，确认不会重复。不碰真实数据。
    @MainActor
    static func selfTest() -> String {
        do {
            let schema = Schema(AppSchema.models)
            func memory(_ name: String) throws -> ModelContainer {
                let configuration = ModelConfiguration(name, schema: schema, isStoredInMemoryOnly: true,
                                                       groupContainer: .none, cloudKitDatabase: .none)
                return try ModelContainer(for: schema, configurations: [configuration])
            }
            let source = try memory("BackupSelfTestSource")
            let target = try memory("BackupSelfTestTarget")
            let samples = insertSamples(into: source.mainContext)
            try source.mainContext.save()

            let data = try export(from: source.mainContext, settings: BackupSettings(favoriteTools: ["coin"], homeCards: [], hiddenHomeCards: []))
            let file = try parse(data)
            try restore(file, mode: .replace, into: target.mainContext, applySettings: false)
            let expected = try counts(in: source.mainContext)
            let replaced = try counts(in: target.mainContext)
            let merged = try restore(file, mode: .merge, into: target.mainContext, applySettings: false)
            let afterMerge = try counts(in: target.mainContext)

            var problems: [String] = []
            if replaced != expected { problems.append("替换导入后条数不一致：\(replaced) ≠ \(expected)") }
            if afterMerge != expected || merged.totalAdded != 0 { problems.append("再次合并导入时产生了重复数据") }
            let profiles = try target.mainContext.fetch(FetchDescriptor<Profile>())
            if let profile = profiles.first(where: { $0.id == samples.profile }) {
                if DayKey(profile.birthday) != DayKey(year: 1998, month: 6, day: 18) || !profile.hasBirthTime || profile.birthCityID != nil {
                    problems.append("档案字段不一致")
                }
            } else {
                problems.append("档案 id 没有保留")
            }
            let countdowns = try target.mainContext.fetch(FetchDescriptor<Countdown>())
            if let countdown = countdowns.first(where: { $0.id == samples.countdown }) {
                if !countdown.isLunar || countdown.lunarMonth != 8 || countdown.lunarDay != 15 || DayKey(countdown.date) != DayKey(year: 2026, month: 9, day: 25) {
                    problems.append("倒数日字段不一致")
                }
            } else {
                problems.append("倒数日 id 没有保留")
            }
            let wheels = try target.mainContext.fetch(FetchDescriptor<WheelTemplate>())
            if wheels.first?.sortedOptions.map(\.title) != ["一", "二", "三"] { problems.append("转盘选项顺序不一致") }
            let draws = try target.mainContext.fetch(FetchDescriptor<StickDraw>())
            if !draws.contains(where: { $0.profileID == nil }) || !draws.contains(where: { $0.profileID == samples.profile }) {
                problems.append("每日一签的档案关联不一致")
            }
            if problems.isEmpty {
                return "自检通过：\(expected.values.reduce(0, +)) 条数据导出再导入一致，重复合并没有新增"
            }
            return problems.joined(separator: "\n")
        } catch {
            return "自检失败：\(error.localizedDescription)"
        }
    }

    @MainActor
    private static func insertSamples(into context: ModelContext) -> (profile: UUID, countdown: UUID) {
        let profile = Profile(name: "测试", relation: "自己", avatar: "🧪", birthday: DayKey(year: 1998, month: 6, day: 18).date)
        profile.hasBirthTime = true
        profile.birthHour = 7
        profile.birthMinute = 30
        context.insert(profile)
        context.insert(Profile(name: "家人", relation: "家人", birthday: DayKey(year: 1970, month: 1, day: 2).date))
        context.insert(DecisionRecord(tool: "coin", title: "抛硬币", result: "正面"))
        context.insert(DecisionRecord(tool: "dice", title: "掷骰子", result: "6", detail: "1 颗"))
        context.insert(StickDraw(dayKey: 20261015, profileID: profile.id, stickNumber: 88))
        context.insert(StickDraw(dayKey: 20261015, profileID: nil, stickNumber: 3))
        context.insert(TarotReading(spread: "three", cards: "0u,13r,77u", question: "下周怎么样"))
        let mood = MoodEntry(dayKey: 20261014, mood: 4, note: "不错")
        mood.tags = ["工作", "运动"]
        context.insert(mood)
        let countdown = Countdown(title: "中秋", emoji: "🥮", date: DayKey(year: 2026, month: 9, day: 25).date, kind: CountdownKind.countdown.rawValue)
        countdown.isLunar = true
        countdown.lunarMonth = 8
        countdown.lunarDay = 15
        countdown.repeatsYearly = true
        context.insert(countdown)
        let wheel = WheelTemplate(name: "测试转盘")
        context.insert(wheel)
        for (index, title) in ["三", "一", "二"].enumerated() {
            // 故意打乱插入顺序，sortIndex 才是顺序
            let option = WheelOption(title: title, weight: 1.5, sortIndex: [2, 0, 1][index])
            option.template = wheel
            context.insert(option)
        }
        let menu = FoodMenu(name: "测试菜单")
        context.insert(menu)
        let item = FoodMenuItem(name: "面", note: "加蛋", weight: 2, sortIndex: 0)
        item.menu = menu
        context.insert(item)
        context.insert(FoodLog(name: "面", source: "测试菜单"))
        context.insert(BlockedFood(name: "香菜"))
        let pool = LotteryPool(name: "测试奖池")
        context.insert(pool)
        let prize = LotteryItem(name: "奶茶", weight: 1, quantity: 3, sortIndex: 0)
        prize.remaining = 2
        prize.pool = pool
        context.insert(prize)
        context.insert(Roster(name: "测试名单", members: ["甲", "乙"]))
        context.insert(CustomPrompt(kind: "dare", text: "唱首歌", level: 1))
        context.insert(ScratchPreset(name: "测试刮刮卡", prizes: ["一等奖"], blankWeight: 0.5))
        return (profile.id, countdown.id)
    }

    @MainActor
    private static func counts(in context: ModelContext) throws -> [String: Int] {
        func n<T: PersistentModel>(_ type: T.Type) throws -> Int { try context.fetchCount(FetchDescriptor<T>()) }
        return try [
            "profiles": n(Profile.self), "decisionRecords": n(DecisionRecord.self), "stickDraws": n(StickDraw.self),
            "tarotReadings": n(TarotReading.self), "moods": n(MoodEntry.self), "countdowns": n(Countdown.self),
            "wheelTemplates": n(WheelTemplate.self), "wheelOptions": n(WheelOption.self), "foodMenus": n(FoodMenu.self),
            "foodMenuItems": n(FoodMenuItem.self), "foodLogs": n(FoodLog.self), "blockedFoods": n(BlockedFood.self),
            "lotteryPools": n(LotteryPool.self), "lotteryItems": n(LotteryItem.self), "rosters": n(Roster.self),
            "customPrompts": n(CustomPrompt.self), "scratchPresets": n(ScratchPreset.self),
        ]
    }
}
#endif
