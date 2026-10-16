import Foundation

/// 择吉日的事项。每件事对应黄历里的若干「宜」用词和「忌」用词。
/// 与 Android 版 core 的 AuspiciousEvent 逐项对应（id、名称、用词、彭祖忌日都相同）。
enum AuspiciousEvent: String, CaseIterable, Identifiable, Sendable {
    case moving, opening, wedding, travel, contract, renovation, haircut, interview, car, worship

    var id: String { rawValue }

    var name: String {
        switch self {
        case .moving: "搬家"
        case .opening: "开业"
        case .wedding: "结婚领证"
        case .travel: "出行"
        case .contract: "签约"
        case .renovation: "动土装修"
        case .haircut: "理发"
        case .interview: "求职面试"
        case .car: "购车提车"
        case .worship: "祭祀祈福"
        }
    }

    /// 选择格里的小字说明
    var subtitle: String {
        switch self {
        case .moving: "入宅 · 移徙"
        case .opening: "开市 · 开工"
        case .wedding: "嫁娶 · 订盟"
        case .travel: "远行 · 旅游"
        case .contract: "立约 · 交易"
        case .renovation: "动土 · 修造"
        case .haircut: "剪发 · 沐浴"
        case .interview: "求职 · 上任"
        case .car: "交易 · 纳财"
        case .worship: "祭祀 · 祈福"
        }
    }

    var emoji: String {
        switch self {
        case .moving: "🏠"
        case .opening: "🎊"
        case .wedding: "💍"
        case .travel: "🧳"
        case .contract: "✍️"
        case .renovation: "🔨"
        case .haircut: "💇"
        case .interview: "💼"
        case .car: "🚗"
        case .worship: "🙏"
        }
    }

    var symbol: String {
        switch self {
        case .moving: "house.fill"
        case .opening: "storefront.fill"
        case .wedding: "heart.circle.fill"
        case .travel: "suitcase.rolling.fill"
        case .contract: "signature"
        case .renovation: "hammer.fill"
        case .haircut: "scissors"
        case .interview: "briefcase.fill"
        case .car: "car.fill"
        case .worship: "hands.sparkles.fill"
        }
    }

    /// 黄历「宜」里出现其中任意一个，就算对口
    var yiWords: [String] {
        switch self {
        case .moving: ["入宅", "移徙", "搬家"]
        case .opening: ["开市", "开业", "开工", "交易", "纳财"]
        case .wedding: ["嫁娶", "订盟", "纳采"]
        case .travel: ["出行"]
        case .contract: ["立券", "立约", "交易", "订盟"]
        case .renovation: ["动土", "修造", "修饰", "涂泥", "开工", "修墙", "补垣"]
        case .haircut: ["理发", "剃头", "沐浴"]
        case .interview: ["求职", "上任", "会友"]
        case .car: ["交易", "纳财", "出行"]
        case .worship: ["祭祀", "祈福", "求嗣", "解除"]
        }
    }

    /// 黄历「忌」里出现其中任意一个，这天就不选
    var jiWords: [String] {
        switch self {
        case .moving: ["入宅", "移徙", "搬家"]
        case .opening: ["开市", "开业"]
        case .wedding: ["嫁娶"]
        case .travel: ["出行", "远行"]
        case .contract: ["签约"]
        case .renovation: ["动土"]
        case .haircut: ["理发", "剃头"]
        case .interview: ["上任"]
        case .car: ["出行"]
        case .worship: ["祭祀", "祈福"]
        }
    }

    /// 彭祖百忌里与这件事相关的日干（丁不剃头），没有为 nil
    var avoidStem: Int? {
        switch self {
        case .haircut: 3
        default: nil
        }
    }

    /// 彭祖百忌里与这件事相关的日支（巳不远行、亥不嫁娶、寅不祭祀），没有为 nil
    var avoidBranch: Int? {
        switch self {
        case .travel: 5
        case .wedding: 11
        case .worship: 2
        default: nil
        }
    }
}

/// 择吉日的一条理由
struct AuspiciousReason: Hashable, Sendable {
    enum Kind: String, Sendable {
        /// 建除，如「开日」
        case jianChu
        /// 黄道值神，如「金匮黄道」
        case deity
        /// 对口的宜，如「宜开市、交易」
        case yi
        /// 吉神，如「天德」「天赦日」
        case shensha
        /// 不冲你的生肖，如「不冲属马」（马为档案生肖）
        case zodiac
        /// 冲你的生肖（文字固定为「冲你的生肖」）
        case clash
    }

    let kind: Kind
    let text: String
}

/// 一个推荐的日子
struct AuspiciousDay: Identifiable, Hashable, Sendable {
    let day: DayKey
    let score: Int
    /// 黄历「宜」里与事项对口的用词（按黄历里的顺序）
    let matched: [String]
    let reasons: [AuspiciousReason]
    /// 这天冲当前档案的生肖
    let clashesUser: Bool

    var id: Int { day.rawValue }

    /// 一句话理由，如「开日，金匮黄道；宜开市、交易；天德；不冲属马」
    var summary: String {
        let head = reasons.filter { $0.kind == .jianChu || $0.kind == .deity }.map(\.text).joined(separator: "，")
        let yi = reasons.filter { $0.kind == .yi }.map(\.text)
        let shensha = reasons.filter { $0.kind == .shensha }.map(\.text).joined(separator: "、")
        let zodiac = reasons.filter { $0.kind == .zodiac || $0.kind == .clash }.map(\.text)
        return ([head] + yi + [shensha] + zodiac).filter { !$0.isEmpty }.joined(separator: "；")
    }
}

/// 择吉日：对未来若干天逐日排黄历，按「宜」是否对口、黄道、建除、吉神打分，
/// 排除忌这件事的日子、破日、四离四绝日和彭祖忌日，冲当前档案生肖的日子降级。纯函数。
///
/// 计分（整数，Android 版相同）：
/// - 宜里有对口用词：+20，每多一个再 +4
/// - 黄道日 +10；建除为吉（建满定成收开）+8
/// - 天赦日 +6；天德、天德合、月德、月德合有任意一个 +4
/// - 周六、周日 +3
/// - 冲你的生肖 -20
/// 宜里没有对口用词时，只有「黄道且建除为吉」的日子入选。按分数从高到低、同分按日期先后排序。
enum AuspiciousDays {
    static let rangeOptions = [30, 60, 90]
    static let defaultRange = 60
    static let defaultLimit = 10

    private static let deShensha = ["天德", "天德合", "月德", "月德合"]
    private static let sheShensha = "天赦日"
    private static let badShensha = ["四绝日", "四离日"]

    /// - Parameters:
    ///   - event: 要办的事
    ///   - start: 起始日（含）
    ///   - days: 往后看多少天（含起始日）
    ///   - animal: 当前档案的生肖（年支），没有档案时为 nil
    ///   - weekendsOnly: 只看周六、周日
    ///   - limit: 最多返回几天
    static func find(_ event: AuspiciousEvent, from start: DayKey, days: Int = defaultRange,
                     animal: EarthlyBranch? = nil, weekendsOnly: Bool = false, limit: Int = defaultLimit) -> [AuspiciousDay] {
        guard days > 0, limit > 0 else { return [] }
        var result: [AuspiciousDay] = []
        for offset in 0..<days {
            let day = start.adding(days: offset)
            if let found = evaluate(event, almanac: AlmanacDay(day), animal: animal, weekendsOnly: weekendsOnly) {
                result.append(found)
            }
        }
        result.sort { a, b in
            if a.score != b.score { return a.score > b.score }
            return a.day < b.day
        }
        return Array(result.prefix(limit))
    }

    /// 单独评估一天；这天不适合办这件事时返回 nil
    static func evaluate(_ event: AuspiciousEvent, almanac: AlmanacDay, animal: EarthlyBranch? = nil,
                         weekendsOnly: Bool = false) -> AuspiciousDay? {
        let day = almanac.key
        let isWeekend = day.weekday == 0 || day.weekday == 6
        if weekendsOnly && !isWeekend { return nil }

        // 排除：破日、大事勿用、四离四绝、忌这件事、彭祖忌日
        if almanac.jianChu.index == 6 || almanac.ji.contains("大事勿用") { return nil }
        if almanac.shensha.contains(where: { badShensha.contains($0) }) { return nil }
        if almanac.ji.contains(where: { event.jiWords.contains($0) }) { return nil }
        if let stem = event.avoidStem, almanac.day.stem.index == stem { return nil }
        if let branch = event.avoidBranch, almanac.day.branch.index == branch { return nil }

        let matched = almanac.yi.filter { event.yiWords.contains($0) }
        let isYellow = almanac.deity.isYellowPath
        let isLucky = almanac.jianChu.isLucky
        if matched.isEmpty && !(isYellow && isLucky) { return nil }

        let lucky = almanac.shensha.filter { deShensha.contains($0) || $0 == sheShensha }
        let clashes = animal.map { almanac.clashAnimal == $0 } ?? false

        var score = 0
        if !matched.isEmpty { score += 20 + 4 * (matched.count - 1) }
        if isYellow { score += 10 }
        if isLucky { score += 8 }
        if almanac.shensha.contains(sheShensha) { score += 6 }
        if almanac.shensha.contains(where: { deShensha.contains($0) }) { score += 4 }
        if isWeekend { score += 3 }
        if clashes { score -= 20 }

        var reasons = [AuspiciousReason(kind: .jianChu, text: almanac.jianChu.name)]
        if isYellow { reasons.append(AuspiciousReason(kind: .deity, text: almanac.deity.name + "黄道")) }
        if !matched.isEmpty { reasons.append(AuspiciousReason(kind: .yi, text: "宜" + matched.joined(separator: "、"))) }
        reasons += lucky.map { AuspiciousReason(kind: .shensha, text: $0) }
        if let animal {
            reasons.append(clashes ? AuspiciousReason(kind: .clash, text: "冲你的生肖")
                                   : AuspiciousReason(kind: .zodiac, text: "不冲属\(animal.animal)"))
        }
        return AuspiciousDay(day: day, score: score, matched: matched, reasons: reasons, clashesUser: clashes)
    }
}
