import Foundation

/// 八相月相
enum MoonPhaseName: Int, CaseIterable, Sendable {
    case new, waxingCrescent, firstQuarter, waxingGibbous, full, waningGibbous, lastQuarter, waningCrescent

    var name: String {
        ["新月", "娥眉月", "上弦月", "盈凸月", "满月", "亏凸月", "下弦月", "残月"][rawValue]
    }

    var symbol: String {
        ["moonphase.new.moon", "moonphase.waxing.crescent", "moonphase.first.quarter", "moonphase.waxing.gibbous",
         "moonphase.full.moon", "moonphase.waning.gibbous", "moonphase.last.quarter", "moonphase.waning.crescent"][rawValue]
    }

    /// phase 为 Astronomy.moonPhase 的 0…1（0 新月，0.5 满月），按最近的一相取：floor(phase × 8 + 0.5) mod 8
    static func of(phase: Double) -> MoonPhaseName {
        let index = Int(floor(phase * 8 + 0.5)) % 8
        return MoonPhaseName(rawValue: (index + 8) % 8) ?? .new
    }
}

/// 某一时刻的月相
struct MoonInfo: Sendable {
    /// 0…1：日月黄经差 / 360，0 新月，0.5 满月
    let phase: Double
    /// 亮面比例 0…1，(1 - cos(2π·phase)) / 2
    let illumination: Double
    let name: MoonPhaseName

    static func at(_ date: Date) -> MoonInfo {
        let phase = Astronomy.moonPhase(date)
        return MoonInfo(phase: phase, illumination: (1 - cos(2 * .pi * phase)) / 2, name: .of(phase: phase))
    }

    /// 当天晚上 21:00（北京时间）的月相
    static func on(_ day: DayKey) -> MoonInfo {
        at(SkyEvents.beijingMoment(day, hour: 21))
    }

    /// 亮面百分比，四舍五入到整数
    var percent: Int { Int((illumination * 100).rounded()) }

    /// 上半月：亮面在右侧（北半球看），越来越圆
    var isWaxing: Bool { phase < 0.5 }

    /// 月龄：距上次新月的天数（按平均朔望月估算）
    var age: Double { phase * SkyEvents.synodicMonth }
}

/// 一条天象：新月、满月、节气、流星雨极大、日月食。日期一律按北京时间。
struct SkyEvent: Identifiable, Hashable, Sendable {
    enum Kind: String, Sendable {
        case newMoon, fullMoon, solarTerm, meteorShower, eclipse
    }

    let day: DayKey
    let kind: Kind
    let title: String
    /// 一两句说明：发生时刻和观测建议
    let detail: String

    var id: String { "\(kind.rawValue)-\(day.rawValue)-\(title)" }
}

enum SkyEvents {
    /// 平均朔望月（天）
    static let synodicMonth = 29.530588853

    /// [start, start + days) 之间的天象，按日期排序；同一天依次是新月 / 满月、日月食、节气、流星雨
    static func upcoming(from start: DayKey, days: Int) -> [SkyEvent] {
        guard days > 0 else { return [] }
        let end = start.adding(days: days)
        var events: [SkyEvent] = []

        // 新月、满月：前后各多找一天，最后统一按北京时间的日期筛选
        let startJD = Astronomy.julianDay(beijingMoment(start, hour: 0)) - 1
        let endJD = Astronomy.julianDay(beijingMoment(end, hour: 0)) + 1
        for kind in [SkyEvent.Kind.newMoon, .fullMoon] {
            let target = kind == .newMoon ? 0.0 : 0.5
            var jd = nextPhase(target, after: startJD)
            while jd < endJD {
                events.append(moonEvent(kind, moment: Astronomy.date(julianDay: jd)))
                jd = nextPhase(target, after: jd + 1)
            }
        }

        for year in start.year...end.year {
            events += SolarTerms.terms(in: year).map(termEvent)
        }
        // 流星雨按「春分起算的年份」推算，象限仪座落在下一个公历年的 1 月，所以从前一年开始
        for year in (start.year - 1)...end.year {
            events += meteorShowers.map { meteorEvent($0, year: year) }
        }
        events += eclipses.compactMap { item in
            DayKey(rawValue: item.day).map { SkyEvent(day: $0, kind: .eclipse, title: item.title, detail: item.detail) }
        }

        return events
            .filter { $0.day >= start && $0.day < end }
            .sorted { a, b in
                if a.day != b.day { return a.day < b.day }
                if order(a.kind) != order(b.kind) { return order(a.kind) < order(b.kind) }
                return a.title < b.title
            }
    }

    /// after 之后（不含当天）第一次满月所在的日期（北京时间）
    static func nextFullMoon(after day: DayKey) -> DayKey {
        nextPhaseDay(0.5, after: day)
    }

    /// after 之后（不含当天）第一次新月所在的日期（北京时间）
    static func nextNewMoon(after day: DayKey) -> DayKey {
        nextPhaseDay(0, after: day)
    }

    // MARK: - 计算

    /// 北京时间某天某个整点的时刻
    static func beijingMoment(_ day: DayKey, hour: Int) -> Date {
        Calendar.gregorianBeijing.date(from: DateComponents(year: day.year, month: day.month, day: day.day, hour: hour)) ?? day.date
    }

    /// 北京时间的「时:分」，如 "08:05"
    static func beijingTime(_ date: Date) -> String {
        let c = Calendar.gregorianBeijing.dateComponents([.hour, .minute], from: date)
        return String(format: "%02d:%02d", c.hour ?? 0, c.minute ?? 0)
    }

    /// 儒略日 jd 时的月相 0…1，与 Astronomy.moonPhase 相同
    static func phase(julianDay jd: Double) -> Double {
        Astronomy.normalize(Astronomy.moonLongitude(jd: jd) - Astronomy.sunLongitude(jd: jd)) / 360
    }

    /// jd 之后月相第一次到达 target（0 新月，0.5 满月）的时刻（儒略日）。
    /// 先按平均朔望月估一个初值，再用牛顿迭代收敛到 1e-9 个周期以内；与精确星历相比误差在几分钟以内（受月亮黄经算法精度限制）。
    static func nextPhase(_ target: Double, after jd: Double) -> Double {
        var delta = target - phase(julianDay: jd)
        delta -= floor(delta)
        var t = jd + delta * synodicMonth
        for _ in 0..<20 {
            var diff = target - phase(julianDay: t)
            diff -= floor(diff + 0.5)
            t += diff * synodicMonth
            if abs(diff) < 1e-9 { break }
        }
        return t
    }

    private static func nextPhaseDay(_ target: Double, after day: DayKey) -> DayKey {
        let start = Astronomy.julianDay(beijingMoment(day.adding(days: 1), hour: 0))
        return SolarTerms.beijingDay(Astronomy.date(julianDay: nextPhase(target, after: start)))
    }

    private static func order(_ kind: SkyEvent.Kind) -> Int {
        switch kind {
        case .newMoon, .fullMoon: 0
        case .eclipse: 1
        case .solarTerm: 2
        case .meteorShower: 3
        }
    }

    // MARK: - 新月、满月

    private static func moonEvent(_ kind: SkyEvent.Kind, moment: Date) -> SkyEvent {
        let day = SolarTerms.beijingDay(moment)
        let lunar = LunarDate(day)
        let when = "北京时间 \(beijingTime(moment))"
        let lunarText = "农历\(lunar.monthName)\(lunar.dayName)"
        if kind == .newMoon {
            return SkyEvent(day: day, kind: .newMoon, title: "新月",
                            detail: "\(when) 日月合朔（\(lunarText)）。前后几晚没有月光干扰，夜空最暗，适合观星、拍星空。")
        }
        return SkyEvent(day: day, kind: .fullMoon, title: "满月",
                        detail: "\(when) 月最圆（\(lunarText)）。月亮整夜都在天上，适合赏月、拍月亮；月光明亮，暗星不容易看到。")
    }

    // MARK: - 节气

    private static func termEvent(_ term: SolarTerms.Term) -> SkyEvent {
        let longitude = (285 + 15 * term.index) % 360
        let tip: String
        switch term.index {
        case 5, 17: tip = "昼夜几乎等长，太阳从正东升起、正西落下，适合校准方向。"
        case 11: tip = "北半球白昼最长，正午的影子是一年中最短的。"
        case 23: tip = "北半球白昼最短、黑夜最长，正午的影子是一年中最长的。"
        case 2...7: tip = "春季傍晚，可以在北方天空找北斗七星，南方找狮子座。"
        case 8...13: tip = "夏夜到远离灯光的地方，能看到银河和南方低空的天蝎座。"
        case 14...19: tip = "秋季夜晚，可以在头顶附近找飞马座的四边形。"
        default: tip = "冬季夜空通透，猎户座和全天最亮的恒星天狼星最显眼。"
        }
        return SkyEvent(day: term.day, kind: .solarTerm, title: term.name,
                        detail: "北京时间 \(beijingTime(term.moment)) 交节，太阳到达黄经 \(longitude)°。\(tip)")
    }

    // MARK: - 流星雨

    private struct MeteorShower: Sendable {
        let name: String
        /// 极大时的太阳黄经（J2000，国际流星组织 IMO 的数值）
        let longitude: Double
        /// 理想条件下每小时的流星数（ZHR）
        let rate: Int
        let advice: String
    }

    private static let meteorShowers: [MeteorShower] = [
        MeteorShower(name: "象限仪座流星雨", longitude: 283.15, rate: 80,
                     advice: "辐射点在北斗七星斗柄附近，后半夜到天亮前最好；高峰只有几个小时，要抓准时间。"),
        MeteorShower(name: "天琴座流星雨", longitude: 32.32, rate: 18,
                     advice: "后半夜辐射点升高后较好，偶尔会有明亮的火流星。"),
        MeteorShower(name: "宝瓶座η流星雨", longitude: 45.5, rate: 50,
                     advice: "来自哈雷彗星的碎屑，辐射点升起得晚，天亮前一两个小时朝东方低空看。"),
        MeteorShower(name: "英仙座流星雨", longitude: 140.0, rate: 100,
                     advice: "一年中最受欢迎的流星雨，午夜后辐射点升高、流星最多，到郊外躺着看就好。"),
        MeteorShower(name: "猎户座流星雨", longitude: 208.0, rate: 20,
                     advice: "同样来自哈雷彗星，午夜后猎户座升起时最好，流星速度很快。"),
        MeteorShower(name: "狮子座流星雨", longitude: 235.27, rate: 15,
                     advice: "后半夜狮子座升起后观看，流星速度快，常拖着余迹。"),
        MeteorShower(name: "双子座流星雨", longitude: 262.2, rate: 150,
                     advice: "一年中数量最多、最稳定的流星雨，晚上九点后就能看到，后半夜最多，注意保暖。"),
    ]

    /// year 年春分之后，太阳到达该流星雨极大黄经的时刻。IMO 的黄经是 J2000 分点，换算到当年分点要加上岁差（每年约 0.01397°）
    private static func meteorEvent(_ shower: MeteorShower, year: Int) -> SkyEvent {
        let target = Astronomy.normalize(shower.longitude + 0.013969713 * Double(year - 2000))
        var utc = Calendar(identifier: .gregorian)
        utc.timeZone = TimeZone(identifier: "UTC") ?? TimeZone(secondsFromGMT: 0)!
        let equinox = utc.date(from: DateComponents(year: year, month: 3, day: 20, hour: 12)) ?? Date()
        let jd = Astronomy.timeOfSunLongitude(target, near: Astronomy.julianDay(equinox) + target / 360 * 365.2422)
        let moment = Astronomy.date(julianDay: jd)
        let day = SolarTerms.beijingDay(moment)
        let hour = Calendar.gregorianBeijing.component(.hour, from: moment)
        let moon = MoonInfo.at(moment)
        let moonText = moon.illumination < 0.25 ? "极大前后月光很弱，观测条件好。"
            : moon.illumination < 0.6 ? "有一些月光干扰，背对月亮的方向看。"
            : "月光较亮，会掩盖暗弱的流星。"
        return SkyEvent(day: day, kind: .meteorShower, title: shower.name,
                        detail: "极大约在北京时间\(day.month)月\(day.day)日 \(hour) 时前后，理想条件下每小时约 \(shower.rate) 颗。\(shower.advice)\(moonText)")
    }

    // MARK: - 日月食

    /// 2026–2035 年的日食和本影月食（半影月食肉眼几乎看不出，不列）。日期为北京时间食甚所在的一天。
    /// 类型、时刻和我国可见情况按 PyEphem 星历逐一核算：月食取各地月亮在地平线上的时段，日食取各地太阳在地平线上时的最大食分。
    private static let eclipses: [(day: Int, title: String, detail: String)] = [
        (20260217, "日环食", "环食带在南极洲，我国不可见。"),
        (20260303, "月全食", "全食约 19:04–20:02（北京时间）。我国可见：东部地区月出后不久就能看到全食，西部地区月出较晚，月亮升起时全食已接近结束。"),
        (20260813, "日全食", "北京时间凌晨发生，全食带经过格陵兰、冰岛和西班牙，我国不可见。"),
        (20260828, "月偏食", "发生在北京时间中午，我国不可见。"),
        (20270206, "日环食", "环食带经过南美洲南部和大西洋，我国不可见。"),
        (20270802, "日全食", "全食最长可达 6 分多钟，全食带经过西班牙南部、北非和阿拉伯半岛，我国不可见。"),
        (20280112, "月偏食", "食分很小，发生在北京时间中午，我国不可见。"),
        (20280126, "日环食", "环食带经过南美洲北部和大西洋，我国不可见。"),
        (20280707, "月偏食", "凌晨约 01:08–03:30，全国可见，月亮约四成被地球本影遮住。"),
        (20280722, "日全食", "全食带经过澳大利亚和新西兰；我国只有海南岛南部和南海地区上午能看到很小的偏食。"),
        (20290101, "月全食", "跨年夜的月全食：12 月 31 日 23:07 左右开始偏食，全食约 00:16–01:27，全国可见全过程。"),
        (20290115, "日偏食", "北美洲可见，我国不可见。"),
        (20290612, "日偏食", "北极附近地区可见，我国不可见。"),
        (20290626, "月全食", "发生在北京时间中午，我国不可见。"),
        (20290711, "日偏食", "南美洲南端可见，我国不可见。"),
        (20291205, "日偏食", "南极洲一带可见，我国不可见。"),
        (20291221, "月全食", "全食约 06:15–07:08，天亮前月亮西沉时进入全食；我国可见，越往西看得越完整。"),
        (20300601, "日环食", "环食带经过内蒙古东北部和黑龙江，全国大部分地区下午可见偏食，越往北食分越大。"),
        (20300616, "月偏食", "凌晨约 01:20–03:45，全国可见，月亮约一半被地球本影遮住。"),
        (20301125, "日全食", "全食带经过南部非洲和澳大利亚，我国不可见。"),
        (20310521, "日环食", "环食带经过非洲、印度洋和东南亚；我国南方大部分地区下午可见偏食，北方看不到。"),
        (20311115, "全环食", "少见的全环食，食带主要在太平洋上，我国不可见。"),
        (20320425, "月全食", "全食约 22:40–23:46，全国可见全过程。"),
        (20320509, "日环食", "环食带在南大西洋上，我国不可见。"),
        (20321019, "月全食", "凌晨全食约 02:38–03:25，全国可见。"),
        (20321103, "日偏食", "全国可见偏食，越往北食分越大，北京约 13:56 食分最大。"),
        (20330331, "日全食", "北京时间凌晨发生，全食带在阿拉斯加，我国不可见。"),
        (20330415, "月全食", "凌晨全食约 02:48–03:37，全国可见。"),
        (20330923, "日偏食", "南美洲南部可见，我国不可见。"),
        (20331008, "月全食", "全食约 18:15–19:33。我国可见：东部地区月出后就能看到全食，西部地区月出较晚，只能看到偏食的尾声。"),
        (20340320, "日全食", "全食带经过非洲、中东和南亚，日落前到达西藏西部；我国西部地区傍晚可见偏食。"),
        (20340913, "日环食", "北京时间凌晨发生，环食带经过南美洲，我国不可见。"),
        (20340928, "月偏食", "食分极小，发生在北京时间上午，我国不可见。"),
        (20350310, "日环食", "环食带主要在南太平洋上，我国不可见。"),
        (20350819, "月偏食", "发生在北京时间上午，我国不可见。"),
        (20350902, "日全食", "全食带从新疆向东经过甘肃、内蒙古、河北、北京等地，北京上午约 8:33 可见日全食；全国其他地区可见偏食。"),
    ]
}
