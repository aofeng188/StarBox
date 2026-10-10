import Foundation

/// 五行
enum WuXing: Int, CaseIterable, Codable, Sendable {
    case wood, fire, earth, metal, water

    var name: String { ["木", "火", "土", "金", "水"][rawValue] }

    /// 我生者
    var generates: WuXing { WuXing(rawValue: (rawValue + 1) % 5)! }
    /// 生我者
    var generatedBy: WuXing { WuXing(rawValue: (rawValue + 4) % 5)! }
    /// 我克者
    var controls: WuXing { WuXing(rawValue: (rawValue + 2) % 5)! }
    /// 克我者
    var controlledBy: WuXing { WuXing(rawValue: (rawValue + 3) % 5)! }

    var direction: String { ["正东", "正南", "中央", "正西", "正北"][rawValue] }

    var numbers: [Int] { [[3, 8], [2, 7], [5, 0], [4, 9], [1, 6]][rawValue] }

    /// 五行对应的幸运色（中国传统色名 + 十六进制色值）
    var luckyColors: [NamedColor] {
        switch self {
        case .wood: [.init(name: "翡翠绿", hex: 0x3DA86A), .init(name: "薄荷绿", hex: 0x5DD3A5), .init(name: "竹青", hex: 0x789262), .init(name: "松石绿", hex: 0x40A497)]
        case .fire: [.init(name: "朱红", hex: 0xFF4C29), .init(name: "胭脂红", hex: 0xD9466B), .init(name: "珊瑚橙", hex: 0xFF7F50), .init(name: "玫瑰紫", hex: 0xC24B8A)]
        case .earth: [.init(name: "琥珀黄", hex: 0xF5B83D), .init(name: "驼色", hex: 0xB08B62), .init(name: "杏黄", hex: 0xF7C173), .init(name: "赭石", hex: 0xA5703F)]
        case .metal: [.init(name: "月白", hex: 0xD6ECF0), .init(name: "香槟金", hex: 0xE8D3A6), .init(name: "银灰", hex: 0xB8BCC8), .init(name: "珍珠白", hex: 0xF3EFE6)]
        case .water: [.init(name: "黛蓝", hex: 0x3F5F8F), .init(name: "靛青", hex: 0x177CB0), .init(name: "墨色", hex: 0x3A3A48), .init(name: "湖蓝", hex: 0x4FA3D9)]
        }
    }

    var keyword: String { ["生发", "热烈", "稳重", "果断", "灵动"][rawValue] }
}

struct NamedColor: Hashable, Codable, Sendable {
    let name: String
    let hex: UInt32

    var red: Double { Double((hex >> 16) & 0xFF) / 255 }
    var green: Double { Double((hex >> 8) & 0xFF) / 255 }
    var blue: Double { Double(hex & 0xFF) / 255 }
    var hexString: String { String(format: "#%06X", hex) }
}

/// 天干
struct HeavenlyStem: Hashable, Sendable {
    let index: Int
    static let names = ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"]

    init(_ index: Int) { self.index = ((index % 10) + 10) % 10 }

    var name: String { Self.names[index] }
    var element: WuXing { WuXing(rawValue: index / 2)! }
    var isYang: Bool { index % 2 == 0 }
}

/// 地支
struct EarthlyBranch: Hashable, Sendable {
    let index: Int
    static let names = ["子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"]
    static let animals = ["鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"]
    static let animalEmoji = ["🐭", "🐮", "🐯", "🐰", "🐲", "🐍", "🐴", "🐑", "🐵", "🐔", "🐶", "🐷"]
    private static let elements: [WuXing] = [.water, .earth, .wood, .wood, .earth, .fire, .fire, .earth, .metal, .metal, .earth, .water]

    init(_ index: Int) { self.index = ((index % 12) + 12) % 12 }

    var name: String { Self.names[index] }
    var animal: String { Self.animals[index] }
    var animalEmoji: String { Self.animalEmoji[index] }
    var element: WuXing { Self.elements[index] }

    /// 时辰名称，如「子时 23:00-01:00」
    var hourName: String { name + "时" }
    var hourRange: String {
        let start = (index * 2 + 23) % 24
        let end = (start + 2) % 24
        return String(format: "%02d:00-%02d:00", start, end)
    }

    static func forHour(_ hour: Int) -> EarthlyBranch { EarthlyBranch(((hour + 1) / 2) % 12) }

    var opposite: EarthlyBranch { EarthlyBranch(index + 6) }

    func relation(to other: EarthlyBranch) -> BranchRelation {
        if index == other.index { return .same }
        if (index + other.index) % 12 == 1 { return .sixHarmony }
        if index % 4 == other.index % 4 { return .threeHarmony }
        if (index - other.index + 12) % 12 == 6 { return .clash }
        if (index + other.index) % 12 == 7 { return .harm }
        return .neutral
    }
}

enum BranchRelation: Sendable {
    case same, sixHarmony, threeHarmony, clash, harm, neutral

    var name: String {
        switch self {
        case .same: "同支"
        case .sixHarmony: "六合"
        case .threeHarmony: "三合"
        case .clash: "相冲"
        case .harm: "相害"
        case .neutral: "平和"
        }
    }

    var score: Int {
        switch self {
        case .same: 2
        case .sixHarmony: 8
        case .threeHarmony: 5
        case .clash: -9
        case .harm: -4
        case .neutral: 0
        }
    }
}

/// 六十甲子中的一个干支
struct GanZhi: Hashable, Sendable {
    let index: Int

    init(_ index: Int) { self.index = ((index % 60) + 60) % 60 }

    init(stem: Int, branch: Int) {
        let s = ((stem % 10) + 10) % 10
        let b = ((branch % 12) + 12) % 12
        self.init(6 * s - 5 * b)
    }

    var stem: HeavenlyStem { HeavenlyStem(index % 10) }
    var branch: EarthlyBranch { EarthlyBranch(index % 12) }
    var name: String { stem.name + branch.name }

    var nayin: String { Self.nayinNames[index / 2] }

    private static let nayinNames = [
        "海中金", "炉中火", "大林木", "路旁土", "剑锋金", "山头火",
        "涧下水", "城头土", "白蜡金", "杨柳木", "泉中水", "屋上土",
        "霹雳火", "松柏木", "长流水", "沙中金", "山下火", "平地木",
        "壁上土", "金箔金", "覆灯火", "天河水", "大驿土", "钗钏金",
        "桑柘木", "大溪水", "沙中土", "天上火", "石榴木", "大海水",
    ]

    static func ofDay(_ key: DayKey) -> GanZhi { GanZhi(key.daysSinceJiaZiReference) }

    static func ofYear(_ year: Int) -> GanZhi { GanZhi(year - 4) }

    /// 五虎遁：由年干和月序（0 = 寅月）得到月柱
    static func ofMonth(yearStem: HeavenlyStem, monthIndex: Int) -> GanZhi {
        let firstStem = (yearStem.index % 5) * 2 + 2
        return GanZhi(stem: firstStem + monthIndex, branch: monthIndex + 2)
    }

    /// 五鼠遁：由日干和时支得到时柱
    static func ofHour(dayStem: HeavenlyStem, branch: EarthlyBranch) -> GanZhi {
        let firstStem = (dayStem.index % 5) * 2
        return GanZhi(stem: firstStem + branch.index, branch: branch.index)
    }
}
