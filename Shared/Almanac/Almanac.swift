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

    /// 公历某一天对应的农历。只按年月日换算，与设备时区无关。
    /// 1900–2100 年查官方农历表（与 Android 版同一份数据），表外才用系统农历（取北京时间当天中午）。
    init(_ key: DayKey) {
        if let found = LunarTable.lunar(of: key) {
            cycleYear = ((found.year - 1984) % 60 + 60) % 60 + 1
            month = found.month
            day = found.day
            isLeapMonth = found.isLeap
            return
        }
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
        if LunarTable.covers(gregorianYear) {
            return LunarTable.solarDay(lunarYear: gregorianYear, month: month, day: day, isLeap: isLeap)
        }
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

/// 官方农历表（紫金山天文台 / 香港天文台口径，1900–2100 年），数据与 Android 版 Lunar.kt 逐项相同。
/// 系统农历（ICU）在少数年份与官方不符，例如把 2027 年春节算成 2 月 7 日（官方 2 月 6 日）、
/// 2030 年春节算成 2 月 2 日（官方 2 月 3 日）、1987 年闰六月算成闰七月，所以表内日期一律查表。
///
/// 每个农历年一个整数：低 13 位依次是各月是否为大月（30 天），第 13–16 位是闰月月份（0 表示无闰月），
/// 第 17 位起是正月初一距当年公历元旦的天数。
private enum LunarTable {
    static let firstYear = 1900
    static let lastYear = 2100

    private struct Month {
        let month: Int
        let isLeap: Bool
        let length: Int
    }

    private static let data: [Int] = [
        0x03D16D2, 0x0620752, 0x04C0EA5, 0x038B64A, 0x05C064B, 0x0440A9B, 0x0309556, 0x056056A,
        0x0400B59, 0x02A5752, 0x0500752, 0x03ADB25, 0x0600B25, 0x0480A4B, 0x032B4AB, 0x05802AD,
        0x042056B, 0x02C4B69, 0x0520DA9, 0x03EFD92, 0x0640E92, 0x04C0D25, 0x036BA4D, 0x05C0A56,
        0x04602B6, 0x02E95B5, 0x05606D4, 0x0400EA9, 0x02C5E92, 0x0500E92, 0x03ACD26, 0x05E052B,
        0x0480A57, 0x032B2B6, 0x0580B5A, 0x04406D4, 0x02E6EC9, 0x0520749, 0x03CF693, 0x0620A93,
        0x04C052B, 0x034CA5B, 0x05A0AAD, 0x046056A, 0x0309B55, 0x0560BA4, 0x0400B49, 0x02A5A93,
        0x0500A95, 0x038F52D, 0x05E0536, 0x0480AAD, 0x034B5AA, 0x05805B2, 0x0420DA5, 0x02E7D4A,
        0x0540D4A, 0x03D0A95, 0x0600A97, 0x04C0556, 0x036CAB5, 0x05A0AD5, 0x04606D2, 0x0308EA5,
        0x0560EA5, 0x040064A, 0x0286C97, 0x04E0A9B, 0x03AF55A, 0x05E056A, 0x0480B69, 0x034B752,
        0x05A0B52, 0x0420B25, 0x02C964B, 0x0520A4B, 0x03D14AB, 0x06002AD, 0x04A056D, 0x036CB69,
        0x05C0DA9, 0x0460D92, 0x0309D25, 0x0560D25, 0x0415A4D, 0x0640A56, 0x04E02B6, 0x038C5B5,
        0x05E06D5, 0x0480EA9, 0x034BE92, 0x05A0E92, 0x0440D26, 0x02C6A56, 0x0500A57, 0x03D14D6,
        0x062035A, 0x04A06D5, 0x036B6C9, 0x05C0749, 0x0460693, 0x02E952B, 0x054052B, 0x03E0A5B,
        0x02A555A, 0x04E056A, 0x038FB55, 0x0600BA4, 0x04A0B49, 0x032BA93, 0x0580A95, 0x042052D,
        0x02C8AAD, 0x0500AB5, 0x03D35AA, 0x06205D2, 0x04C0DA5, 0x036DD4A, 0x05C0D4A, 0x0460C95,
        0x030952E, 0x0540556, 0x03E0AB5, 0x02A55B2, 0x05006D2, 0x038CEA5, 0x05E0725, 0x048064B,
        0x032AC97, 0x0560CAB, 0x042055A, 0x02C6AD6, 0x0520B69, 0x03D7752, 0x0620B52, 0x04C0B25,
        0x036DA4B, 0x05A0A4B, 0x04404AB, 0x02EA55B, 0x05405AD, 0x03E0B6A, 0x02A5B52, 0x0500D92,
        0x03AFD25, 0x05E0D25, 0x0480A55, 0x032B4AD, 0x05804B6, 0x04005B5, 0x02C6DAA, 0x0520EC9,
        0x03F1E92, 0x0620E92, 0x04C0D26, 0x036CA56, 0x05A0A57, 0x04404D6, 0x02E86D5, 0x0540755,
        0x0400749, 0x0286E93, 0x04E0693, 0x038F52B, 0x05E052B, 0x0460A5B, 0x032B55A, 0x058056A,
        0x0420B65, 0x02C974A, 0x0520B4A, 0x03D1A95, 0x0620A95, 0x04A052D, 0x034CAAD, 0x05A0AB5,
        0x04605AA, 0x02E8BA5, 0x0540DA5, 0x0400D4A, 0x02A7C95, 0x04E0C96, 0x038F94E, 0x05E0556,
        0x0480AB5, 0x032B5B2, 0x05806D2, 0x0420EA5, 0x02E8E4A, 0x050068B, 0x03B0C97, 0x06004AB,
        0x04A055B, 0x034CAD6, 0x05A0B6A, 0x0460752, 0x0309725, 0x0540B45, 0x03E0A8B, 0x028549B,
        0x04E04AB,
    ]

    static func covers(_ lunarYear: Int) -> Bool { lunarYear >= firstYear && lunarYear <= lastYear }

    private static func newYearJDN(_ year: Int) -> Int {
        DayKey.julianDayNumber(year: year, month: 1, day: 1) + (data[year - firstYear] >> 17)
    }

    /// 某个农历年里按顺序排列的月份（闰月紧跟在同名月之后）
    private static func months(_ year: Int) -> [Month] {
        let bits = data[year - firstYear]
        let leap = (bits >> 13) & 0xF
        var result: [Month] = []
        var bit = 0
        for m in 1...12 {
            result.append(Month(month: m, isLeap: false, length: (bits & (1 << bit)) != 0 ? 30 : 29))
            bit += 1
            if m == leap {
                result.append(Month(month: m, isLeap: true, length: (bits & (1 << bit)) != 0 ? 30 : 29))
                bit += 1
            }
        }
        return result
    }

    /// 公历日期 → 农历（year 为农历年开始所在的公历年）；超出表的范围返回 nil
    static func lunar(of key: DayKey) -> (year: Int, month: Int, day: Int, isLeap: Bool)? {
        let jdn = DayKey.julianDayNumber(year: key.year, month: key.month, day: key.day)
        var year = min(key.year, lastYear)
        guard year >= firstYear else { return nil }
        if jdn < newYearJDN(year) { year -= 1 }
        guard year >= firstYear else { return nil }
        var offset = jdn - newYearJDN(year)
        for item in months(year) {
            if offset < item.length { return (year: year, month: item.month, day: offset + 1, isLeap: item.isLeap) }
            offset -= item.length
        }
        return nil
    }

    /// 农历 → 公历；该年没有这个月或这一天时返回 nil
    static func solarDay(lunarYear: Int, month: Int, day: Int, isLeap: Bool) -> DayKey? {
        guard covers(lunarYear), day >= 1 else { return nil }
        var offset = 0
        for item in months(lunarYear) {
            if item.month == month && item.isLeap == isLeap {
                guard day <= item.length else { return nil }
                return dayKey(julianDayNumber: newYearJDN(lunarYear) + offset + day - 1)
            }
            offset += item.length
        }
        return nil
    }

    private static func dayKey(julianDayNumber jdn: Int) -> DayKey {
        let a = jdn + 32044
        let b = (4 * a + 3) / 146097
        let c = a - 146097 * b / 4
        let d = (4 * c + 3) / 1461
        let e = c - 1461 * d / 4
        let m = (5 * e + 2) / 153
        return DayKey(year: 100 * b + d - 4800 + m / 10, month: m + 3 - 12 * (m / 10), day: e - (153 * m + 2) / 5 + 1)
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
