import Foundation

/// 农历日期
struct LunarDate: Hashable, Sendable {
    let cycleYear: Int      // 1…60
    let month: Int          // 1…12
    let day: Int            // 1…30
    let isLeapMonth: Bool

    static let monthNames = ["正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"]
    static let dayNames: [String] = {
        let digits = ["", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"]
        return (1...30).map { d in
            switch d {
            case 1...10: return "初" + digits[d]
            case 11...19: return "十" + digits[d - 10]
            case 20: return "二十"
            case 21...29: return "廿" + digits[d - 20]
            default: return "三十"
            }
        }
    }()

    /// 公历某一天对应的农历。只按年月日换算（取北京时间当天中午），与设备时区无关。
    init(_ key: DayKey) {
        let noon = Calendar.gregorianBeijing.date(from: DateComponents(year: key.year, month: key.month, day: key.day, hour: 12)) ?? key.date
        let c = Calendar.chineseLocal.dateComponents([.year, .month, .day], from: noon)
        cycleYear = c.year ?? 1
        month = c.month ?? 1
        day = c.day ?? 1
        isLeapMonth = c.isLeapMonth ?? false
    }

    /// 某个时刻在设备本地日历上所在那一天的农历
    init(_ date: Date) {
        self.init(DayKey(date))
    }

    var yearGanZhi: GanZhi { GanZhi(cycleYear - 1) }
    var monthName: String { (isLeapMonth ? "闰" : "") + Self.monthNames[month - 1] + "月" }
    var dayName: String { Self.dayNames[day - 1] }
    var text: String { monthName + dayName }
    var fullText: String { "\(yearGanZhi.name)\(yearGanZhi.branch.animal)年 \(text)" }

    /// 由农历年月日换算公历日期（gregorianYear 指农历年开始所在的公历年），不存在的日期返回 nil
    static func solarDay(gregorianYear: Int, month: Int, day: Int, isLeap: Bool) -> DayKey? {
        let offset = gregorianYear - 1984
        let era = 78 + Int(floor(Double(offset) / 60))
        let year = ((offset % 60) + 60) % 60 + 1
        var components = DateComponents()
        components.era = era
        components.year = year
        components.month = month
        components.day = day
        components.isLeapMonth = isLeap
        components.hour = 12
        guard let date = Calendar.chineseLocal.date(from: components) else { return nil }
        // 得到的是北京时间中午，必须按北京时间取公历年月日
        let key = DayKey(date, calendar: .gregorianBeijing)
        // 校验：防止不存在的日期（如没有三十的小月）被自动顺延
        let check = LunarDate(key)
        guard check.month == month, check.day == day, check.isLeapMonth == isLeap else { return nil }
        return key
    }

    /// 同 `solarDay`，返回那一天本地时区的中午，`DayKey(_:)` 换回来就是正确的公历日期
    static func solarDate(gregorianYear: Int, month: Int, day: Int, isLeap: Bool) -> Date? {
        solarDay(gregorianYear: gregorianYear, month: month, day: day, isLeap: isLeap)?.date
    }
}

/// 建除十二值星
struct JianChu: Sendable {
    let index: Int
    static let names = ["建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭"]
    var name: String { Self.names[index] + "日" }

    var yi: [String] { Self.yiTable[index] }
    var ji: [String] { Self.jiTable[index] }
    var summary: String { Self.summaries[index] }

    private static let yiTable: [[String]] = [
        ["出行", "上任", "会友", "求职", "祈福"],
        ["沐浴", "扫舍", "求医", "疗病", "解除"],
        ["祭祀", "祈福", "开市", "交易", "纳财"],
        ["修饰", "涂泥", "平治道路", "祭祀"],
        ["订盟", "嫁娶", "纳采", "交易", "立券"],
        ["祈福", "栽种", "纳畜", "捕捉", "立约"],
        ["求医", "治病", "破屋", "坏垣"],
        ["祭祀", "祈福", "安床", "静养"],
        ["嫁娶", "开市", "入学", "移徙", "开工", "出行"],
        ["纳财", "收账", "入学", "纳畜", "收纳"],
        ["开市", "开业", "入学", "嫁娶", "求职", "出行"],
        ["筑堤", "补垣", "修墙", "安葬", "收藏"],
    ]

    private static let jiTable: [[String]] = [
        ["动土", "开仓", "嫁娶", "安葬"],
        ["嫁娶", "远行", "开市", "栽种"],
        ["动土", "栽种", "上任", "求医"],
        ["嫁娶", "开渠", "栽种", "远行"],
        ["诉讼", "出行", "求医"],
        ["开市", "出行", "移徙", "搬家"],
        ["嫁娶", "开市", "签约", "动土", "出行"],
        ["登高", "乘船", "冒险", "远行"],
        ["诉讼", "争执"],
        ["出行", "安葬", "开市"],
        ["安葬", "伐木", "动土"],
        ["开市", "出行", "求医", "上任", "开渠"],
    ]

    private static let summaries = [
        "建日为一月之始，宜起事不宜动土。",
        "除日除旧布新，宜清扫、治病。",
        "满日丰收之象，宜祭祀纳财。",
        "平日平常之日，诸事平平，宜修整。",
        "定日安定之象，宜订盟立约。",
        "执日执守之象，宜守成与栽种。",
        "破日月破大耗，诸事不宜，只宜破旧。",
        "危日小心谨慎，宜安静守成。",
        "成日诸事有成，是大吉之日。",
        "收日收获之日，宜纳财收账。",
        "开日开通顺畅，宜开业、出行。",
        "闭日闭藏之象，宜收藏、修补。",
    ]

    var isLucky: Bool { [0, 2, 4, 8, 9, 10].contains(index) }
}

/// 黄道/黑道值神
struct DayDeity: Sendable {
    let index: Int
    static let names = ["青龙", "明堂", "天刑", "朱雀", "金匮", "天德", "白虎", "玉堂", "天牢", "玄武", "司命", "勾陈"]
    var name: String { Self.names[index] }
    var isYellowPath: Bool { [0, 1, 4, 5, 7, 10].contains(index) }
    var pathName: String { isYellowPath ? "黄道吉日" : "黑道日" }
}

/// 某一天的完整黄历
struct AlmanacDay: Sendable {
    let key: DayKey
    let lunar: LunarDate
    let year: GanZhi
    let month: GanZhi
    let day: GanZhi
    let solarTerm: SolarTerms.Term?
    let nextTerm: SolarTerms.Term
    let jianChu: JianChu
    let deity: DayDeity
    let yi: [String]
    let ji: [String]
    let festivals: [String]

    init(_ key: DayKey) {
        self.key = key
        lunar = LunarDate(key)
        day = GanZhi.ofDay(key)
        let monthIndex = SolarTerms.monthIndex(of: key)
        var ganzhiYear = key.year
        if monthIndex >= 10 && key.month <= 2 { ganzhiYear -= 1 }
        year = GanZhi.ofYear(ganzhiYear)
        month = GanZhi.ofMonth(yearStem: year.stem, monthIndex: monthIndex)
        solarTerm = SolarTerms.term(on: key)
        nextTerm = SolarTerms.next(after: key)

        let monthBranch = month.branch.index
        jianChu = JianChu(index: (day.branch.index - monthBranch + 12) % 12)
        let dragonStart = (((monthBranch % 6) - 2) * 2 + 12) % 12
        deity = DayDeity(index: (day.branch.index - dragonStart + 12) % 12)

        var yi = jianChu.yi
        var ji = jianChu.ji
        if deity.isYellowPath {
            if !yi.contains("祈福") { yi.append("祈福") }
        } else {
            if !ji.contains("大事勿用") && jianChu.index == 6 { ji.append("大事勿用") }
        }
        if jianChu.index == 6 { yi = ["求医", "治病", "破旧"] }
        self.yi = yi
        self.ji = ji
        festivals = Festivals.on(key, lunar: lunar)
    }

    /// 冲煞，如「冲马(甲午) 煞南」
    var clash: String {
        // 天克地冲：被日干所克的天干 + 对冲地支，如甲子日冲戊午
        let opposite = GanZhi(stem: day.stem.index + 4, branch: day.branch.index + 6)
        let sha = ["南", "东", "北", "西"][day.branch.index % 4]
        return "冲\(day.branch.opposite.animal)(\(opposite.name)) 煞\(sha)"
    }

    var clashAnimal: EarthlyBranch { day.branch.opposite }

    var pengzu: [String] {
        [Self.pengzuStem[day.stem.index], Self.pengzuBranch[day.branch.index]]
    }

    var joyDirection: String { ["东北", "西北", "西南", "正南", "东南"][day.stem.index % 5] }
    var wealthDirection: String { ["东北", "西南", "正北", "正东", "正南"][day.stem.index / 2] }

    var ganzhiText: String { "\(year.name)年 \(month.name)月 \(day.name)日" }

    var termText: String {
        if let solarTerm { return "今日\(solarTerm.name)" }
        let days = key.days(to: nextTerm.day)
        return "距\(nextTerm.name)还有\(days)天"
    }

    private static let pengzuStem = [
        "甲不开仓 财物耗散", "乙不栽植 千株不长", "丙不修灶 必见灾殃", "丁不剃头 头必生疮", "戊不受田 田主不祥",
        "己不破券 二比并亡", "庚不经络 织机虚张", "辛不合酱 主人不尝", "壬不汲水 更难提防", "癸不词讼 理弱敌强",
    ]
    private static let pengzuBranch = [
        "子不问卜 自惹祸殃", "丑不冠带 主不还乡", "寅不祭祀 神鬼不尝", "卯不穿井 水泉不香",
        "辰不哭泣 必主重丧", "巳不远行 财物伏藏", "午不苫盖 屋主更张", "未不服药 毒气入肠",
        "申不安床 鬼祟入房", "酉不会客 醉坐颠狂", "戌不吃犬 作怪上床", "亥不嫁娶 不利新郎",
    ]
}

/// 节日
enum Festivals {
    private static let solar: [Int: String] = [
        101: "元旦", 214: "情人节", 308: "妇女节", 312: "植树节", 401: "愚人节",
        501: "劳动节", 504: "青年节", 601: "儿童节", 701: "建党节", 801: "建军节",
        910: "教师节", 1001: "国庆节", 1031: "万圣夜", 1224: "平安夜", 1225: "圣诞节",
    ]
    private static let lunarTable: [Int: String] = [
        101: "春节", 115: "元宵节", 202: "龙抬头", 505: "端午节", 707: "七夕",
        715: "中元节", 815: "中秋节", 909: "重阳节", 1208: "腊八节", 1223: "北方小年",
    ]

    static func on(_ key: DayKey, lunar: LunarDate) -> [String] {
        var result: [String] = []
        if let name = solar[key.month * 100 + key.day] { result.append(name) }
        if !lunar.isLeapMonth, let name = lunarTable[lunar.month * 100 + lunar.day] { result.append(name) }
        // 除夕：明天是正月初一
        let tomorrow = LunarDate(key.adding(days: 1))
        if tomorrow.month == 1 && tomorrow.day == 1 && !tomorrow.isLeapMonth { result.append("除夕") }
        // 母亲节：五月第二个周日；父亲节：六月第三个周日
        if key.weekday == 0 {
            let nth = (key.day - 1) / 7 + 1
            if key.month == 5 && nth == 2 { result.append("母亲节") }
            if key.month == 6 && nth == 3 { result.append("父亲节") }
        }
        if let term = SolarTerms.term(on: key), term.name == "清明" { result.append("清明节") }
        return result
    }
}

/// 八字（四柱）
struct BaZi: Sendable {
    let year: GanZhi
    let month: GanZhi
    let day: GanZhi
    let hour: GanZhi?
    /// 是否已按真太阳时校正
    let trueSolarApplied: Bool

    init(_ birth: BirthInfo) {
        let city = birth.city
        let moment = birth.moment
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = city.timeZone

        // 年、月柱以太阳黄经（节气）为准
        let monthIndex = SolarTerms.monthIndex(at: moment)
        let local = calendar.dateComponents([.year, .month], from: moment)
        var ganzhiYear = local.year ?? 2000
        if monthIndex >= 10 && (local.month ?? 1) <= 2 { ganzhiYear -= 1 }
        year = GanZhi.ofYear(ganzhiYear)
        month = GanZhi.ofMonth(yearStem: year.stem, monthIndex: monthIndex)

        if birth.hasTime {
            // 真太阳时 = 钟表时间 + 经度差修正 + 均时差
            let standardMeridian = Double(city.timeZone.secondsFromGMT(for: moment)) / 3600 * 15
            let correction = (city.longitude - standardMeridian) * 4 + Astronomy.equationOfTime(moment)
            let solar = moment.addingTimeInterval(correction * 60)
            let c = calendar.dateComponents([.year, .month, .day, .hour], from: solar)
            var dayKey = DayKey(year: c.year ?? 2000, month: c.month ?? 1, day: c.day ?? 1)
            let hourValue = c.hour ?? 12
            if hourValue >= 23 { dayKey = dayKey.adding(days: 1) }
            day = GanZhi.ofDay(dayKey)
            hour = GanZhi.ofHour(dayStem: day.stem, branch: .forHour(hourValue))
            trueSolarApplied = true
        } else {
            day = GanZhi.ofDay(birth.dayKey)
            hour = nil
            trueSolarApplied = false
        }
    }

    var pillars: [GanZhi] { [year, month, day] + (hour.map { [$0] } ?? []) }
    var dayMaster: HeavenlyStem { day.stem }
    var zodiacAnimal: EarthlyBranch { year.branch }

    /// 五行个数统计（天干 + 地支本气）
    var elementCounts: [WuXing: Int] {
        var counts = Dictionary(uniqueKeysWithValues: WuXing.allCases.map { ($0, 0) })
        for pillar in pillars {
            counts[pillar.stem.element, default: 0] += 1
            counts[pillar.branch.element, default: 0] += 1
        }
        return counts
    }

    var weakestElements: [WuXing] {
        let counts = elementCounts
        let minimum = counts.values.min() ?? 0
        return WuXing.allCases.filter { counts[$0] == minimum }
    }

    var strongestElement: WuXing {
        let counts = elementCounts
        return WuXing.allCases.max { (counts[$0] ?? 0) < (counts[$1] ?? 0) } ?? .earth
    }

    /// 简化的「喜用」：日主五行偏弱则取生扶，偏强则取泄耗。
    var favorableElement: WuXing {
        let counts = elementCounts
        let master = dayMaster.element
        let support = (counts[master] ?? 0) + (counts[master.generatedBy] ?? 0)
        let total = counts.values.reduce(0, +)
        return Double(support) < Double(total) / 2.5 ? master.generatedBy : master.generates
    }

    var text: String { pillars.map(\.name).joined(separator: " ") }
}
