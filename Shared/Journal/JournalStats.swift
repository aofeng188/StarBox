import Foundation

// 日记的统计：本月回顾、年度回顾、年度关键词。都是纯函数，输入记录的值拷贝，输出统计结构，
// 可以在后台线程算。Android 的日记按这里的规则逐条实现，两端结果一致。
//
// 名词：
// - 「决定」：tool 不是占卜类（stick 求签、tarot 塔罗、jiaobei 掷筊）也不是游戏类（rps 猜拳、scratch 刮刮乐）的记录。
//   占卜在日记里单独成行，游戏只是玩，都不算决定。
// - 「照做」：adopted == true；「回访」：satisfaction != nil；满意率 = satisfaction == 1 的条数 / 回访条数。
// - 百分比一律四舍五入到整数（0.5 进一）；分母为 0 时为 nil，界面上不显示。
// - 「问题」：DecisionRecord.askedQuestion（question；没有时硬币 coin、答案之书 answers 用 title）。
// - 「最纠结的问题类型」：决定的问题用 QuestionClassifier 分类后出现最多的类型；没写问题的「今天吃什么」「附近餐厅」算吃什么，其余不计。
// - 「有记录的日子」：当天有任意决策记录（含占卜和游戏）、心情、每日一签或塔罗。
// - 「运势最高 / 最幸运的一天」：有记录的日子里，当前档案运势分数最高的一天；同分取较早的。
// - 排名（最常用的工具、最纠结的类型、最常吃的菜）：次数多的在前；次数相同，先出现的在前（记录按时间正序）。

/// 一条决策记录的值拷贝
struct DecisionSample: Sendable, Hashable {
    var tool: String
    var result: String
    var question: String?
    var date: Date
    var adopted: Bool?
    var satisfaction: Int?

    var day: DayKey { DayKey(date) }
}

extension DecisionSample {
    /// question 取 record.askedQuestion
    init(_ record: DecisionRecord) {
        self.init(tool: record.tool, result: record.result, question: record.askedQuestion, date: record.date,
                  adopted: record.adopted, satisfaction: record.satisfaction)
    }
}

extension DecisionRecord {
    /// 当时在问的问题：「问一问」带过来的 question；没有时，硬币（coin）和答案之书（answers）的 title
    /// 就是用户在页面里写的问题。都没有时为 nil
    var askedQuestion: String? {
        if let question = question?.trimmingCharacters(in: .whitespacesAndNewlines), !question.isEmpty { return question }
        guard tool == "coin" || tool == "answers" else { return nil }
        let written = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return written.isEmpty ? nil : written
    }
}

/// 一天的心情（MoodEntry 的值拷贝）
struct MoodSample: Sendable, Hashable {
    /// yyyyMMdd
    var dayKey: Int
    /// 1…5
    var mood: Int
}

struct NameCount: Sendable, Hashable, Identifiable {
    let name: String
    let count: Int
    var id: String { name }
}

struct DayScore: Sendable, Hashable {
    let day: DayKey
    let score: Int
}

/// 某个月的平均心情
struct MonthMood: Sendable, Hashable, Identifiable {
    /// 1…12
    let month: Int
    let average: Double
    /// 这个月打卡的天数
    let days: Int
    var id: Int { month }
}

/// 决定的数量、照做和回访
struct DecisionTally: Sendable, Hashable {
    var decisions = 0
    /// 「就这么办」
    var adopted = 0
    /// 「再想想」
    var reconsidered = 0
    /// 回访过（满意 / 一般 / 后悔）
    var rated = 0
    var satisfied = 0
    var neutral = 0
    var regretted = 0

    /// 照做比例 = 照做 / 决定
    var adoptionRate: Int? { JournalStats.percent(adopted, of: decisions) }
    /// 满意率 = 满意 / 回访
    var satisfactionRate: Int? { JournalStats.percent(satisfied, of: rated) }
}

/// 本月回顾
struct MonthlyReview: Sendable, Hashable {
    var tally = DecisionTally()
    /// 最常用的工具（tool 键，含占卜和游戏）
    var topTool: NameCount?
    /// 最纠结的问题类型（只看决定）
    var topKind: QuestionKind?
    var topKindCount = 0
    /// 平均心情 1…5
    var moodAverage: Double?
    var moodDays = 0
    /// 有记录的日子里运势分数最高的一天
    var bestDay: DayScore?
    var recordDays = 0

    var isEmpty: Bool { recordDays == 0 }
}

/// 年度回顾的输入
struct AnnualInput: Sendable {
    var year: Int
    /// 这一年的全部决策记录（含占卜和游戏）
    var decisions: [DecisionSample]
    var moods: [MoodSample]
    /// 每日一签：yyyyMMdd 和签的等级（「上上」「上吉」「中吉」「中平」「下」）
    var sticks: [(dayKey: Int, level: String)]
    /// 塔罗占卜的日子 yyyyMMdd
    var tarotDays: [Int]
    /// 「就吃这个」的菜名（FoodLog.name），按时间正序
    var foods: [String]
}

/// 年度回顾
struct AnnualReview: Sendable, Hashable {
    var year: Int
    var keywords: [NameCount] = []
    var tally = DecisionTally()
    /// 最常吃的菜，最多 3 个
    var topFoods: [NameCount] = []
    /// 有心情记录的月份，按月份排
    var moodByMonth: [MonthMood] = []
    var moodAverage: Double?
    var moodDays = 0
    var luckiestDay: DayScore?
    /// 求签等级分布，固定按 上上、上吉、中吉、中平、下 排，没求到的等级 count 为 0
    var stickLevels: [NameCount] = []
    var stickCount = 0
    /// 最常用的工具（tool 键），最多 3 个
    var topTools: [NameCount] = []
    var recordDays = 0

    var isEmpty: Bool { recordDays == 0 }
}

enum JournalStats {
    /// 占卜类：在日记里单独成行，不算「决定」
    static let divinationTools: Set<String> = ["stick", "tarot", "jiaobei"]
    /// 游戏类：只是玩，不算「决定」
    static let gameTools: Set<String> = ["rps", "scratch"]
    /// 签的等级，从好到差
    static let stickLevelOrder = ["上上", "上吉", "中吉", "中平", "下"]

    static func isDecision(_ tool: String) -> Bool {
        !divinationTools.contains(tool) && !gameTools.contains(tool)
    }

    /// 四舍五入的百分比；分母为 0 时为 nil
    static func percent(_ part: Int, of total: Int) -> Int? {
        guard total > 0 else { return nil }
        return Int((Double(part) * 100 / Double(total)).rounded(.toNearestOrAwayFromZero))
    }

    /// 一条决定在问哪类事：有问题按 QuestionClassifier 判断；没写问题时「今天吃什么」「附近餐厅」算吃什么，其余不计
    static func questionKind(tool: String, question: String?) -> QuestionKind? {
        if let question = question?.trimmingCharacters(in: .whitespacesAndNewlines), !question.isEmpty {
            return QuestionClassifier.analyze(question).kind
        }
        return tool == "food" || tool == "nearby" ? .food : nil
    }

    /// 只统计决定（isDecision）
    static func tally(_ samples: [DecisionSample]) -> DecisionTally {
        var tally = DecisionTally()
        for sample in samples where isDecision(sample.tool) {
            tally.decisions += 1
            switch sample.adopted {
            case true?: tally.adopted += 1
            case false?: tally.reconsidered += 1
            case nil: break
            }
            switch sample.satisfaction {
            case 1?: tally.rated += 1; tally.satisfied += 1
            case 0?: tally.rated += 1; tally.neutral += 1
            case -1?: tally.rated += 1; tally.regretted += 1
            default: break
            }
        }
        return tally
    }

    /// 排名：次数多的在前，次数相同先出现的在前
    static func ranked(_ names: [String]) -> [NameCount] {
        var counts: [String: Int] = [:]
        var first: [String: Int] = [:]
        for (index, name) in names.enumerated() {
            counts[name, default: 0] += 1
            if first[name] == nil { first[name] = index }
        }
        return counts
            .sorted { lhs, rhs in
                lhs.value != rhs.value ? lhs.value > rhs.value : (first[lhs.key] ?? 0) < (first[rhs.key] ?? 0)
            }
            .map { NameCount(name: $0.key, count: $0.value) }
    }

    /// 有记录的日子里运势最高的一天；同分取较早的那天
    static func bestDay(among days: Set<Int>, fortuneScore: (DayKey) -> Int) -> DayScore? {
        var best: DayScore?
        for raw in days.sorted() {
            guard let day = DayKey(rawValue: raw) else { continue }
            let score = fortuneScore(day)
            if let current = best, score <= current.score { continue }
            best = DayScore(day: day, score: score)
        }
        return best
    }

    // MARK: - 本月回顾

    /// decisions、moods 只传这个月的；extraDays 是这个月求过签、占过塔罗的日子
    static func monthly(decisions: [DecisionSample], moods: [MoodSample], extraDays: Set<Int>,
                        fortuneScore: (DayKey) -> Int) -> MonthlyReview {
        let samples = decisions.sorted { $0.date < $1.date }
        var review = MonthlyReview()
        review.tally = tally(samples)
        review.topTool = ranked(samples.map(\.tool)).first

        let kinds = samples.filter { isDecision($0.tool) }.compactMap { questionKind(tool: $0.tool, question: $0.question) }
        if let top = ranked(kinds.map(\.rawValue)).first, let kind = QuestionKind(rawValue: top.name) {
            review.topKind = kind
            review.topKindCount = top.count
        }

        if !moods.isEmpty {
            review.moodDays = moods.count
            review.moodAverage = Double(moods.map(\.mood).reduce(0, +)) / Double(moods.count)
        }

        let days = Set(samples.map { $0.day.rawValue }).union(moods.map(\.dayKey)).union(extraDays)
        review.recordDays = days.count
        review.bestDay = bestDay(among: days, fortuneScore: fortuneScore)
        return review
    }

    // MARK: - 年度回顾

    static func annual(_ input: AnnualInput, fortuneScore: (DayKey) -> Int) -> AnnualReview {
        let samples = input.decisions.sorted { $0.date < $1.date }
        var review = AnnualReview(year: input.year)
        review.tally = tally(samples)
        review.topTools = Array(ranked(samples.map(\.tool)).prefix(3))
        review.topFoods = Array(ranked(input.foods.filter { !$0.isEmpty }).prefix(3))
        review.keywords = keywords(keywordTexts(samples))

        if !input.moods.isEmpty {
            review.moodDays = input.moods.count
            review.moodAverage = Double(input.moods.map(\.mood).reduce(0, +)) / Double(input.moods.count)
            let byMonth = Dictionary(grouping: input.moods) { $0.dayKey / 100 % 100 }
            review.moodByMonth = byMonth.keys.sorted().compactMap { month in
                guard let entries = byMonth[month], !entries.isEmpty else { return nil }
                return MonthMood(month: month, average: Double(entries.map(\.mood).reduce(0, +)) / Double(entries.count), days: entries.count)
            }
        }

        var levelCounts: [String: Int] = [:]
        for stick in input.sticks { levelCounts[stick.level, default: 0] += 1 }
        review.stickLevels = stickLevelOrder.map { NameCount(name: $0, count: levelCounts[$0] ?? 0) }
        review.stickCount = input.sticks.count

        let days = Set(samples.map { $0.day.rawValue })
            .union(input.moods.map(\.dayKey))
            .union(input.sticks.map { $0.dayKey })
            .union(input.tarotDays)
        review.recordDays = days.count
        review.luckiestDay = bestDay(among: days, fortuneScore: fortuneScore)
        return review
    }

    // MARK: - 年度关键词

    /// 关键词素材：按时间正序，每条决定先取问题，再取结果（只取 keywordResultTools 的结果；和问题相同时不重复取）
    static let keywordResultTools: Set<String> = ["food", "nearby", "wheel", "lottery", "answers"]

    static func keywordTexts(_ samples: [DecisionSample]) -> [String] {
        var texts: [String] = []
        for sample in samples.sorted(by: { $0.date < $1.date }) where isDecision(sample.tool) {
            let question = sample.question?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
            if !question.isEmpty { texts.append(question) }
            let result = sample.result.trimmingCharacters(in: .whitespacesAndNewlines)
            if keywordResultTools.contains(sample.tool), !result.isEmpty, result != question { texts.append(result) }
        }
        return texts
    }

    /// 不当关键词的常用词
    static let keywordStopwords: Set<String> = [
        "今天", "明天", "昨天", "后天", "今晚", "现在", "时候", "什么", "怎么", "怎么样", "为什么", "要不要", "能不能",
        "该不该", "可不可以", "是不是", "会不会", "行不行", "好不好", "还是", "或者", "哪个", "哪一个", "一个", "一下",
        "一点", "我们", "你们", "他们", "自己", "这个", "那个", "就是", "没有", "不是", "可能", "应该", "是否", "不要",
        "还有", "的话", "然后", "但是", "因为", "所以", "如果", "已经", "正面", "反面", "选择", "决定",
    ]

    /// 首字或末字是这些虚字的片段不当关键词（「吃火锅」只留「火锅」）
    static let keywordEdgeChars: Set<Character> = Set(
        "的了吗呢吧啊呀么嘛哦呗和与或是在我你他她它就都也还要不会能很太把被给让这那哪谁啥什怎个些着过得地吃喝去来做买看想有没"
    )

    /// 停用词按长度从长到短，切汉字段时先匹配长的
    private static let stopwordsByLength: [[Character]] = keywordStopwords.map { Array($0) }.sorted { $0.count > $1.count }

    /// 年度关键词。规则（Android 逐条一致）：
    /// 1. 每段文本按字符切开：汉字（U+3400–4DBF、U+4E00–9FFF、U+F900–FAFF）连成汉字段，
    ///    英文字母（A–Z、a–z）连成英文段，其他字符（标点、空白、数字等）都是分隔。
    ///    汉字段再从左到右扫一遍，遇到停用词（同一位置先匹配长的）就把它挖掉、在那里断开：「明天会下雨吗」→「会下雨吗」。
    /// 2. 候选片段，按这个顺序生成：英文段长度 ≥ 2 时整段转小写；汉字段长度 2–4 时整段，
    ///    长度 ≥ 3 时再取长度从 min(4, 段长 - 1) 到 2 的子串（长的先，同长从左到右）。
    /// 3. 丢掉停用词，以及首字或末字是虚字（keywordEdgeChars）的汉字片段。
    /// 4. 同一段文本里重复的片段只算一次；片段的次数 = 含有它的文本段数。
    /// 5. 排序：次数多的在前；次数相同，长的在前；再相同，先生成的在前。
    /// 6. 依次挑选，跳过与已选关键词互相包含的片段，最多 limit 个，只挑次数 ≥ 2 的；
    ///    一个都挑不出来时改挑次数为 1 的，最多 3 个。
    static func keywords(_ texts: [String], limit: Int = 5) -> [NameCount] {
        var counts: [String: Int] = [:]
        var order: [String: Int] = [:]
        for text in texts {
            var seen = Set<String>()
            for candidate in keywordCandidates(text) where seen.insert(candidate).inserted {
                counts[candidate, default: 0] += 1
                if order[candidate] == nil { order[candidate] = order.count }
            }
        }
        let sorted = counts.sorted { lhs, rhs in
            if lhs.value != rhs.value { return lhs.value > rhs.value }
            if lhs.key.count != rhs.key.count { return lhs.key.count > rhs.key.count }
            return (order[lhs.key] ?? 0) < (order[rhs.key] ?? 0)
        }

        func pick(minCount: Int, maxCount: Int) -> [NameCount] {
            var picked: [NameCount] = []
            for (name, count) in sorted where count >= minCount {
                if picked.contains(where: { $0.name.contains(name) || name.contains($0.name) }) { continue }
                picked.append(NameCount(name: name, count: count))
                if picked.count >= maxCount { break }
            }
            return picked
        }

        let frequent = pick(minCount: 2, maxCount: limit)
        return frequent.isEmpty ? pick(minCount: 1, maxCount: min(3, limit)) : frequent
    }

    /// 一段文本里的候选片段（已过滤，可能重复），顺序见 keywords 的第 2 条
    static func keywordCandidates(_ text: String) -> [String] {
        var candidates: [String] = []
        for segment in segments(text) {
            if segment.isLatin {
                guard segment.scalars.count >= 2 else { continue }
                let word = String(String.UnicodeScalarView(segment.scalars)).lowercased()
                if !keywordStopwords.contains(word) { candidates.append(word) }
                continue
            }
            for chars in cutStopwords(segment.scalars.map { Character($0) }) {
                let length = chars.count
                if (2...4).contains(length) { candidates.append(String(chars)) }
                if length >= 3 {
                    for size in stride(from: min(4, length - 1), through: 2, by: -1) {
                        for start in 0...(length - size) {
                            candidates.append(String(chars[start..<(start + size)]))
                        }
                    }
                }
            }
        }
        return candidates.filter { candidate in
            guard !keywordStopwords.contains(candidate) else { return false }
            guard let first = candidate.first, let last = candidate.last else { return false }
            // 英文词不看虚字
            if first.isASCII { return true }
            return !keywordEdgeChars.contains(first) && !keywordEdgeChars.contains(last)
        }
    }

    /// 汉字段里出现停用词的地方挖掉并断开
    private static func cutStopwords(_ chars: [Character]) -> [[Character]] {
        var parts: [[Character]] = []
        var current: [Character] = []
        var index = 0
        while index < chars.count {
            let start = index
            if let word = stopwordsByLength.first(where: { word in
                start + word.count <= chars.count && chars[start..<(start + word.count)].elementsEqual(word)
            }) {
                if !current.isEmpty { parts.append(current) }
                current = []
                index += word.count
            } else {
                current.append(chars[index])
                index += 1
            }
        }
        if !current.isEmpty { parts.append(current) }
        return parts
    }

    private struct Segment {
        var isLatin: Bool
        var scalars: [Unicode.Scalar]
    }

    private static func isHan(_ scalar: Unicode.Scalar) -> Bool {
        switch scalar.value {
        case 0x3400...0x4DBF, 0x4E00...0x9FFF, 0xF900...0xFAFF: true
        default: false
        }
    }

    private static func isLatin(_ scalar: Unicode.Scalar) -> Bool {
        switch scalar.value {
        case 0x41...0x5A, 0x61...0x7A: true
        default: false
        }
    }

    private static func segments(_ text: String) -> [Segment] {
        var result: [Segment] = []
        var current: Segment?
        for scalar in text.unicodeScalars {
            let latin = isLatin(scalar)
            guard latin || isHan(scalar) else {
                if let segment = current { result.append(segment) }
                current = nil
                continue
            }
            if var segment = current, segment.isLatin == latin {
                segment.scalars.append(scalar)
                current = segment
            } else {
                if let segment = current { result.append(segment) }
                current = Segment(isLatin: latin, scalars: [scalar])
            }
        }
        if let segment = current { result.append(segment) }
        return result
    }
}
