import Foundation

enum FortuneCategory: String, CaseIterable, Identifiable, Codable, Sendable {
    case love, career, wealth, health

    var id: String { rawValue }

    var name: String {
        switch self {
        case .love: "爱情"
        case .career: "事业学业"
        case .wealth: "财运"
        case .health: "健康"
        }
    }

    var shortName: String {
        switch self {
        case .love: "爱情"
        case .career: "事业"
        case .wealth: "财运"
        case .health: "健康"
        }
    }

    var symbol: String {
        switch self {
        case .love: "heart.fill"
        case .career: "briefcase.fill"
        case .wealth: "yensign.circle.fill"
        case .health: "leaf.fill"
        }
    }
}

/// 某人某天的运势
struct DailyFortune: Sendable {
    let day: DayKey
    let score: Int
    let levelName: String
    /// 0 = 大吉 … 4 = 宜静
    let levelIndex: Int
    let summary: String
    let categoryScores: [FortuneCategory: Int]
    let categoryTexts: [FortuneCategory: String]
    let luckyColor: NamedColor
    let luckyNumber: Int
    let luckyDirection: String
    let wealthDirection: String
    let luckyHour: EarthlyBranch
    let luckyItem: String
    let noblemanSign: ZodiacSign
    let favorableElement: WuXing
    let funYi: [String]
    let funJi: [String]
    let quote: String
    let insights: [String]
    let moonSign: ZodiacSign
    let isPersonalized: Bool

    var stars: Double { StarRating.stars(for: score) }

    func score(_ category: FortuneCategory) -> Int { categoryScores[category] ?? score }
    func text(_ category: FortuneCategory) -> String { categoryTexts[category] ?? "" }
}

enum StarRating {
    /// 分数换算为 1…5 星（以半星为单位）
    static func stars(for score: Int) -> Double {
        let raw = (Double(score) - 30) / 70 * 5
        return min(5, max(1, (raw * 2).rounded() / 2))
    }
}

enum FortuneEngine {
    private static func clamp(_ value: Double, _ low: Int, _ high: Int) -> Int {
        min(high, max(low, Int(value.rounded())))
    }

    static func daily(for birth: BirthInfo?, on day: DayKey = .today) -> DailyFortune {
        let content = ContentLibrary.fortune
        let birthKey = birth.map { "\($0.dayKey.rawValue)-\($0.hour ?? -1)-\($0.minute ?? -1)" } ?? "anonymous"
        var rng = SeededGenerator("daily-fortune", day.rawValue, birthKey)
        let almanac = AlmanacDay(day)
        let moonSign = ZodiacSign.moonSign(at: day.date)

        var base = rng.gaussian(mean: 69, deviation: 12)
        var mods = Dictionary(uniqueKeysWithValues: FortuneCategory.allCases.map { ($0, 0.0) })
        var insights: [String] = []
        let favorable: WuXing
        var nobleman = ZodiacSign(rawValue: rng.int(below: 12))!

        if let birth {
            let bazi = BaZi(birth)
            let master = bazi.dayMaster.element
            let today = almanac.day.stem.element
            let relationKey: String
            switch today {
            case master:
                relationKey = "peer"; base += 2; mods[.career]! += 4; mods[.love]! += 2
            case master.generatedBy:
                relationKey = "resource"; base += 5; mods[.career]! += 4; mods[.health]! += 5
            case master.generates:
                relationKey = "output"; base += 1; mods[.love]! += 6; mods[.wealth]! += 2
            case master.controls:
                relationKey = "wealth"; base += 3; mods[.wealth]! += 9
            default:
                relationKey = "officer"; base -= 3; mods[.career]! += 3; mods[.health]! -= 5
            }
            if let template = content.relations[relationKey] {
                insights.append(template
                    .replacingOccurrences(of: "{day}", with: "\(almanac.day.name)日属\(today.name)")
                    .replacingOccurrences(of: "{master}", with: "\(bazi.dayMaster.name)\(master.name)"))
            }

            let branchRelation = almanac.day.branch.relation(to: bazi.year.branch)
            base += Double(branchRelation.score)
            if let note = content.branchNotes[Self.key(branchRelation)], !note.isEmpty { insights.append(note) }

            let sunSign = ZodiacSign.sunSign(of: birth)
            let aspect = AspectKind(distance: sunSign.distance(to: moonSign))
            let aspectScore: Double = switch aspect {
            case .conjunction: 3
            case .sextile: 4
            case .trine: 6
            case .square: -5
            case .opposition: -3
            case .quincunx: -1
            case .semiSextile: 0
            }
            base += aspectScore
            mods[.love]! += aspectScore * 1.2
            if let note = content.moonNotes[Self.key(aspect)], !note.isEmpty { insights.append(note) }

            favorable = bazi.favorableElement
            nobleman = rng.pick(sunSign.trines + sunSign.sextiles)
        } else {
            favorable = almanac.day.stem.element
        }

        if almanac.deity.isYellowPath { base += 3 }
        if almanac.jianChu.index == 6 { base -= 4 }
        if [8, 10].contains(almanac.jianChu.index) { base += 2 }

        let score = clamp(base, 35, 99)
        let level = content.levels.firstIndex { score >= $0.min } ?? content.levels.count - 1
        var categoryScores: [FortuneCategory: Int] = [:]
        var categoryTexts: [FortuneCategory: String] = [:]
        for category in FortuneCategory.allCases {
            let value = clamp(Double(score) + rng.gaussian(mean: 0, deviation: 9) + (mods[category] ?? 0), 28, 99)
            categoryScores[category] = value
            let pool = content.categories[category.rawValue]?.texts(for: value) ?? []
            categoryTexts[category] = pool.isEmpty ? "" : rng.pick(pool)
        }

        var number = rng.pick(favorable.numbers) + 10 * rng.int(0...3)
        if number == 0 { number = 10 }

        return DailyFortune(
            day: day,
            score: score,
            levelName: content.levels[level].name,
            levelIndex: level,
            summary: rng.pick(content.levels[level].summaries),
            categoryScores: categoryScores,
            categoryTexts: categoryTexts,
            luckyColor: rng.pick(favorable.luckyColors),
            luckyNumber: number,
            luckyDirection: almanac.joyDirection,
            wealthDirection: almanac.wealthDirection,
            luckyHour: EarthlyBranch(13 - almanac.day.branch.index),
            luckyItem: rng.pick(content.items),
            noblemanSign: nobleman,
            favorableElement: favorable,
            funYi: rng.pick(content.funYi, count: 3),
            funJi: rng.pick(content.funJi, count: 3),
            quote: rng.pick(content.quotes),
            insights: insights,
            moonSign: moonSign,
            isPersonalized: birth != nil
        )
    }

    private static func key(_ relation: BranchRelation) -> String {
        switch relation {
        case .same: "same"
        case .sixHarmony: "sixHarmony"
        case .threeHarmony: "threeHarmony"
        case .clash: "clash"
        case .harm: "harm"
        case .neutral: "neutral"
        }
    }

    private static func key(_ aspect: AspectKind) -> String {
        switch aspect {
        case .conjunction: "conjunction"
        case .semiSextile: "semiSextile"
        case .sextile: "sextile"
        case .square: "square"
        case .trine: "trine"
        case .quincunx: "quincunx"
        case .opposition: "opposition"
        }
    }
}
