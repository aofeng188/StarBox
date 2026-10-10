import Foundation

enum ZodiacElement: Int, CaseIterable, Sendable {
    case fire, earth, air, water
    var name: String { ["火象", "土象", "风象", "水象"][rawValue] }
    var shortName: String { ["火", "土", "风", "水"][rawValue] }
    var trait: String { ["热情冲动、行动力强", "务实稳重、脚踏实地", "理性灵活、善于沟通", "感性细腻、直觉敏锐"][rawValue] }
}

enum ZodiacModality: Int, Sendable {
    case cardinal, fixed, mutable
    var name: String { ["基本宫", "固定宫", "变动宫"][rawValue] }
}

/// 西方十二星座
enum ZodiacSign: Int, CaseIterable, Identifiable, Codable, Sendable {
    case aries, taurus, gemini, cancer, leo, virgo, libra, scorpio, sagittarius, capricorn, aquarius, pisces

    var id: Int { rawValue }

    var name: String {
        ["白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座"][rawValue]
    }

    var shortName: String { String(name.dropLast()) }

    var englishName: String {
        ["Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"][rawValue]
    }

    /// 文本样式的星座符号（加 U+FE0E 避免被渲染成 emoji）
    var glyph: String {
        ["\u{2648}", "\u{2649}", "\u{264A}", "\u{264B}", "\u{264C}", "\u{264D}", "\u{264E}", "\u{264F}", "\u{2650}", "\u{2651}", "\u{2652}", "\u{2653}"][rawValue] + "\u{FE0E}"
    }

    var dateRange: String {
        ["3.21–4.19", "4.20–5.20", "5.21–6.21", "6.22–7.22", "7.23–8.22", "8.23–9.22",
         "9.23–10.23", "10.24–11.22", "11.23–12.21", "12.22–1.19", "1.20–2.18", "2.19–3.20"][rawValue]
    }

    var element: ZodiacElement { ZodiacElement(rawValue: rawValue % 4)! }
    var modality: ZodiacModality { ZodiacModality(rawValue: rawValue % 3)! }

    var ruler: String {
        ["火星", "金星", "水星", "月亮", "太阳", "水星", "金星", "冥王星", "木星", "土星", "天王星", "海王星"][rawValue]
    }

    var keywords: [String] {
        [
            ["勇敢", "直率", "行动派"], ["稳重", "享受", "专一"], ["机智", "好奇", "多变"],
            ["温柔", "顾家", "敏感"], ["自信", "慷慨", "闪耀"], ["细致", "理性", "完美主义"],
            ["优雅", "公正", "社交达人"], ["深邃", "专注", "洞察力"], ["自由", "乐观", "冒险"],
            ["坚韧", "务实", "有野心"], ["独立", "创新", "博爱"], ["浪漫", "共情", "想象力"],
        ][rawValue]
    }

    var profile: String {
        [
            "白羊座是黄道第一宫，像初春破土的嫩芽，永远带着「先冲了再说」的勇气。你的热情来得快，行动力惊人，擅长开启新局面；需要学会的是耐心，把开头的火焰延续成长久的光。",
            "金牛座重视安全感与生活质感，喜欢一切美好、真实、能握在手里的东西。你慢热却可靠，一旦认定便长久坚持；放下对变化的抗拒，会遇见更多惊喜。",
            "双子座拥有星座里最灵活的大脑，好奇心像永不停歇的风。你擅长沟通与学习，能同时处理多件事情；给自己留一点专注的时间，才华会更加耀眼。",
            "巨蟹座温柔而坚韧，像月光一样守护在意的人。你对情绪有天然的敏感，懂得照顾他人；也别忘了把同样的温柔留给自己。",
            "狮子座天生自带主角光环，慷慨、热烈、敢于表达。你喜欢被欣赏，也乐于照亮身边的人；真正的自信来自内心，而不是掌声。",
            "处女座追求细节与秩序，是最可靠的执行者和最体贴的照顾者。你的标准很高，思维缜密；偶尔允许不完美，生活会轻松许多。",
            "天秤座优雅、讲究公平，擅长在人与人之间找到平衡点。你审美出众、人缘极佳；面对选择时多相信直觉，不必事事求全。",
            "天蝎座深邃而专注，拥有看透表象的洞察力。你爱憎分明、情感浓烈，一旦投入便全力以赴；学会信任与放下，会让你更有力量。",
            "射手座是自由的灵魂，热爱远方、知识与真理。你乐观幽默，总能把困境变成冒险；在奔跑的同时记得回头看看身边的人。",
            "摩羯座踏实坚韧，是星座里的长跑冠军。你有清晰的目标和惊人的自律，越到后程越显实力；别忘了给自己放个假。",
            "水瓶座独立、前卫，脑子里总有别人想不到的点子。你重视友谊与理想，关心更大的世界；偶尔也让身边人走进你的内心。",
            "双鱼座浪漫、善良，拥有丰富的想象力和强大的共情能力。你能感受到别人感受不到的情绪；为自己建立边界，梦想才能落地生根。",
        ][rawValue]
    }

    /// 由太阳黄经确定星座（比日期表更准确，能处理交界日）
    init(longitude: Double) {
        self = ZodiacSign(rawValue: Int(Astronomy.normalize(longitude) / 30) % 12)!
    }

    /// 由生日确定太阳星座
    static func sunSign(of birth: BirthInfo) -> ZodiacSign {
        ZodiacSign(longitude: Astronomy.sunLongitude(birth.moment))
    }

    static func moonSign(at date: Date) -> ZodiacSign {
        ZodiacSign(longitude: Astronomy.moonLongitude(date))
    }

    static func ascendant(of birth: BirthInfo) -> ZodiacSign? {
        guard birth.hasTime else { return nil }
        let city = birth.city
        return ZodiacSign(longitude: Astronomy.ascendant(birth.moment, latitude: city.latitude, longitude: city.longitude))
    }

    /// 两个星座之间的相位距离（0…6）
    func distance(to other: ZodiacSign) -> Int {
        let d = (other.rawValue - rawValue + 12) % 12
        return min(d, 12 - d)
    }

    func offset(_ n: Int) -> ZodiacSign { ZodiacSign(rawValue: ((rawValue + n) % 12 + 12) % 12)! }

    /// 三分相（同元素）的星座
    var trines: [ZodiacSign] { [offset(4), offset(8)] }
    var sextiles: [ZodiacSign] { [offset(2), offset(10)] }
    var squares: [ZodiacSign] { [offset(3), offset(9)] }
    var opposite: ZodiacSign { offset(6) }
}

enum AspectKind: Sendable {
    case conjunction, semiSextile, sextile, square, trine, quincunx, opposition

    init(distance: Int) {
        switch distance {
        case 0: self = .conjunction
        case 1: self = .semiSextile
        case 2: self = .sextile
        case 3: self = .square
        case 4: self = .trine
        case 5: self = .quincunx
        default: self = .opposition
        }
    }

    var name: String {
        switch self {
        case .conjunction: "合相"
        case .semiSextile: "半六合"
        case .sextile: "六合"
        case .square: "刑相"
        case .trine: "拱相"
        case .quincunx: "梅花相"
        case .opposition: "冲相"
        }
    }
}
