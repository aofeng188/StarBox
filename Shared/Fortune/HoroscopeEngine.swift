import Foundation

enum HoroscopePeriod: String, CaseIterable, Identifiable, Sendable {
    case today, tomorrow, week, month

    var id: String { rawValue }

    var name: String {
        switch self {
        case .today: "今日"
        case .tomorrow: "明日"
        case .week: "本周"
        case .month: "本月"
        }
    }
}

struct Horoscope: Sendable {
    let sign: ZodiacSign
    let period: HoroscopePeriod
    let rangeText: String
    let score: Int
    let categoryScores: [FortuneCategory: Int]
    let categoryTexts: [FortuneCategory: String]
    let overview: String
    let comment: String
    let luckyColor: NamedColor
    let luckyNumber: Int
    let matchSign: ZodiacSign
    let cautionSign: ZodiacSign
    let skyNote: String

    var stars: Double { StarRating.stars(for: score) }
    func score(_ category: FortuneCategory) -> Int { categoryScores[category] ?? score }
}

enum HoroscopeEngine {
    static func horoscope(_ sign: ZodiacSign, period: HoroscopePeriod, reference: DayKey = .today) -> Horoscope {
        let content = ContentLibrary.horoscope
        let calendar = Calendar.gregorianLocal

        let periodKey: Int
        let rangeText: String
        let skyDate: Date
        var skyBoost: Double
        let skyNote: String

        switch period {
        case .today, .tomorrow:
            let day = period == .today ? reference : reference.adding(days: 1)
            periodKey = day.rawValue
            rangeText = DateText.monthDay(day) + " " + DateText.weekday(day)
            skyDate = day.date
            let moon = ZodiacSign.moonSign(at: skyDate)
            skyBoost = boost(for: AspectKind(distance: sign.distance(to: moon)))
            skyNote = "月亮行经\(moon.name)，与\(sign.name)形成\(AspectKind(distance: sign.distance(to: moon)).name)"
        case .week:
            let weekStart = calendar.dateInterval(of: .weekOfYear, for: reference.date)?.start ?? reference.date
            let start = DayKey(weekStart)
            let end = start.adding(days: 6)
            periodKey = start.rawValue * 10 + 7
            rangeText = "\(start.month).\(start.day)–\(end.month).\(end.day)"
            skyDate = start.adding(days: 3).date
            let sun = ZodiacSign(longitude: Astronomy.sunLongitude(skyDate))
            skyBoost = boost(for: AspectKind(distance: sign.distance(to: sun))) * 0.8
            skyNote = "太阳正行经\(sun.name)，与\(sign.name)形成\(AspectKind(distance: sign.distance(to: sun)).name)"
        case .month:
            periodKey = reference.year * 100 + reference.month
            rangeText = "\(reference.year)年\(reference.month)月"
            skyDate = DayKey(year: reference.year, month: reference.month, day: 15).date
            let sun = ZodiacSign(longitude: Astronomy.sunLongitude(skyDate))
            skyBoost = boost(for: AspectKind(distance: sign.distance(to: sun))) * 0.8
            skyNote = "本月太阳主要行经\(sun.name)"
        }

        // 今日和明日共用 "day"：昨天看到的「明日」与今天的「今日」一致
        let seedPeriod = period == .today || period == .tomorrow ? "day" : period.rawValue
        var rng = SeededGenerator("horoscope", sign.rawValue, seedPeriod, periodKey)
        if period == .month || period == .week { skyBoost += rng.double(-2...2) }
        let score = clamp(rng.gaussian(mean: 68, deviation: 12) + skyBoost, 38, 98)

        // 不同元素的星座在不同领域略有天赋加成
        let elementBias: [FortuneCategory: Double] = switch sign.element {
        case .fire: [.career: 3, .health: 2]
        case .earth: [.wealth: 4, .career: 1]
        case .air: [.career: 2, .love: 2]
        case .water: [.love: 4, .health: -1]
        }

        var scores: [FortuneCategory: Int] = [:]
        var texts: [FortuneCategory: String] = [:]
        for category in FortuneCategory.allCases {
            let value = clamp(Double(score) + rng.gaussian(mean: 0, deviation: 10) + (elementBias[category] ?? 0), 30, 99)
            scores[category] = value
            let pool: BandedTexts = switch category {
            case .love: content.love
            case .career: content.career
            case .wealth: content.wealth
            case .health: content.health
            }
            texts[category] = rng.pick(pool.texts(for: value))
        }

        let allColors = WuXing.allCases.flatMap(\.luckyColors)
        return Horoscope(
            sign: sign,
            period: period,
            rangeText: rangeText,
            score: score,
            categoryScores: scores,
            categoryTexts: texts,
            overview: rng.pick(content.overview.texts(for: score)),
            comment: rng.pick(content.comments),
            luckyColor: rng.pick(allColors),
            luckyNumber: rng.int(1...99),
            matchSign: rng.pick(sign.trines + sign.sextiles),
            cautionSign: rng.pick(sign.squares),
            skyNote: skyNote
        )
    }

    private static func boost(for aspect: AspectKind) -> Double {
        switch aspect {
        case .conjunction: 6
        case .trine: 7
        case .sextile: 4
        case .semiSextile: 0
        case .quincunx: -2
        case .square: -6
        case .opposition: -3
        }
    }

    private static func clamp(_ value: Double, _ low: Int, _ high: Int) -> Int {
        min(high, max(low, Int(value.rounded())))
    }
}
