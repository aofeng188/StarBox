import Foundation

/// 二十四节气（按北京时间计算日期）
enum SolarTerms {
    /// 从小寒开始的顺序
    static let names = [
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至",
    ]

    static let blurbs: [String: String] = [
        "小寒": "寒气渐深，宜早睡晚起，温补养藏。",
        "大寒": "一年最冷之时，岁末将尽，静待新春。",
        "立春": "东风解冻，万物始生，适合立下新的计划。",
        "雨水": "冰雪消融，雨水渐多，宜调理脾胃。",
        "惊蛰": "春雷乍动，万物复苏，适合开启新的尝试。",
        "春分": "昼夜平分，阴阳相半，宜保持平衡。",
        "清明": "气清景明，宜踏青、宜怀念。",
        "谷雨": "雨生百谷，春季最后一个节气，宜祛湿。",
        "立夏": "万物繁茂，夏日开启，宜养心。",
        "小满": "麦粒渐满而未满，小满即是圆满。",
        "芒种": "有芒之谷可种，忙碌而有希望。",
        "夏至": "白昼最长，阳气最盛，宜清补。",
        "小暑": "温风至，蟋蟀居壁，宜消暑静心。",
        "大暑": "一年最热之时，宜避暑养阴。",
        "立秋": "凉风至，暑去秋来，宜收敛神气。",
        "处暑": "暑气至此而止，秋意渐浓。",
        "白露": "露凝而白，早晚温差大，注意添衣。",
        "秋分": "昼夜再次平分，丹桂飘香，宜团圆。",
        "寒露": "露气寒冷，将凝结也，宜养阴润燥。",
        "霜降": "气肃而凝，露结为霜，秋季最后一个节气。",
        "立冬": "水始冰，地始冻，宜进补藏精。",
        "小雪": "气寒将雪，宜温润养护。",
        "大雪": "积雪渐厚，围炉煮茶正当时。",
        "冬至": "白昼最短，一阳初生，宜团聚、宜吃饺子或汤圆。",
    ]

    struct Term: Hashable, Sendable {
        let index: Int
        let name: String
        let moment: Date
        let day: DayKey
    }

    private static let beijing = TimeZone(identifier: "Asia/Shanghai")!
    nonisolated(unsafe) private static var cache: [Int: [Term]] = [:]
    private static let lock = NSLock()

    static func beijingDay(_ date: Date) -> DayKey {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = beijing
        return DayKey(date, calendar: calendar)
    }

    /// 某个公历年里的 24 个节气（小寒 … 冬至）
    static func terms(in year: Int) -> [Term] {
        lock.lock()
        defer { lock.unlock() }
        if let cached = cache[year] { return cached }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        let springEquinoxGuess = calendar.date(from: DateComponents(year: year - 1, month: 3, day: 20, hour: 12)) ?? Date()
        let baseJD = Astronomy.julianDay(springEquinoxGuess)
        var result: [Term] = []
        for i in 0..<24 {
            let offset = Double(285 + 15 * i)
            let target = Astronomy.normalize(offset)
            let jd = Astronomy.timeOfSunLongitude(target, near: baseJD + offset / 360 * 365.2422)
            let moment = Astronomy.date(julianDay: jd)
            result.append(Term(index: i, name: names[i], moment: moment, day: beijingDay(moment)))
        }
        cache[year] = result
        return result
    }

    static func term(on day: DayKey) -> Term? {
        terms(in: day.year).first { $0.day == day }
    }

    static func next(after day: DayKey) -> Term {
        if let term = terms(in: day.year).first(where: { $0.day > day }) { return term }
        return terms(in: day.year + 1)[0]
    }

    static func previous(onOrBefore day: DayKey) -> Term {
        if let term = terms(in: day.year).last(where: { $0.day <= day }) { return term }
        return terms(in: day.year - 1)[23]
    }

    /// 节气月序：0 = 寅月（立春起）… 11 = 丑月
    static func monthIndex(at date: Date) -> Int {
        let lon = Astronomy.sunLongitude(date)
        return Int(Astronomy.normalize(lon - 315) / 30) % 12
    }

    /// 某天（北京时间）所属的节气月：交节当天按新月计算。
    static func monthIndex(of day: DayKey) -> Int {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = beijing
        let endOfDay = calendar.date(from: DateComponents(year: day.year, month: day.month, day: day.day, hour: 23, minute: 59, second: 59)) ?? day.date
        return monthIndex(at: endOfDay)
    }
}
