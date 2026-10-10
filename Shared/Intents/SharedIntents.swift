import AppIntents
import Foundation

/// 星座参数（Siri 与小组件配置共用）
enum ZodiacSignAppEnum: String, AppEnum {
    case aries, taurus, gemini, cancer, leo, virgo, libra, scorpio, sagittarius, capricorn, aquarius, pisces

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "星座"
    static let caseDisplayRepresentations: [ZodiacSignAppEnum: DisplayRepresentation] = [
        .aries: "白羊座", .taurus: "金牛座", .gemini: "双子座", .cancer: "巨蟹座",
        .leo: "狮子座", .virgo: "处女座", .libra: "天秤座", .scorpio: "天蝎座",
        .sagittarius: "射手座", .capricorn: "摩羯座", .aquarius: "水瓶座", .pisces: "双鱼座",
    ]

    var sign: ZodiacSign {
        ZodiacSign(rawValue: Self.allCases.firstIndex(of: self) ?? 0) ?? .aries
    }

    init(_ sign: ZodiacSign) {
        self = Self.allCases[sign.rawValue]
    }
}

/// 可以从控制中心、快捷指令直接打开的页面
enum XingXiaDestination: String, AppEnum {
    case food, coin, dice, wheel, number, stick, tarot, answers, fortune, horoscope

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "星匣页面"
    static let caseDisplayRepresentations: [XingXiaDestination: DisplayRepresentation] = [
        .food: DisplayRepresentation(title: "今天吃什么", image: .init(systemName: "fork.knife")),
        .coin: DisplayRepresentation(title: "抛硬币", image: .init(systemName: "centsign.circle.fill")),
        .dice: DisplayRepresentation(title: "3D 骰子", image: .init(systemName: "dice.fill")),
        .wheel: DisplayRepresentation(title: "幸运大转盘", image: .init(systemName: "circle.dashed")),
        .number: DisplayRepresentation(title: "随机数字", image: .init(systemName: "number")),
        .stick: DisplayRepresentation(title: "求签", image: .init(systemName: "flame.fill")),
        .tarot: DisplayRepresentation(title: "塔罗占卜", image: .init(systemName: "suit.diamond.fill")),
        .answers: DisplayRepresentation(title: "答案之书", image: .init(systemName: "book.fill")),
        .fortune: DisplayRepresentation(title: "今日运势", image: .init(systemName: "sparkles")),
        .horoscope: DisplayRepresentation(title: "星座运势", image: .init(systemName: "moon.stars.fill")),
    ]

    var url: URL {
        switch self {
        case .food, .coin, .dice, .wheel, .number: URL(string: "xingxia://tool/\(rawValue)")!
        default: URL(string: "xingxia://\(rawValue)")!
        }
    }

    var symbol: String {
        switch self {
        case .food: "fork.knife"
        case .coin: "centsign.circle.fill"
        case .dice: "dice.fill"
        case .wheel: "circle.dashed"
        case .number: "number"
        case .stick: "flame.fill"
        case .tarot: "suit.diamond.fill"
        case .answers: "book.fill"
        case .fortune: "sparkles"
        case .horoscope: "moon.stars.fill"
        }
    }
}

/// 打开星匣的某个页面
struct OpenXingXiaIntent: AppIntent {
    static let title: LocalizedStringResource = "打开星匣"
    static let description = IntentDescription("直接打开星匣里的某个工具或页面。")

    @Parameter(title: "页面", default: .food)
    var destination: XingXiaDestination

    init() {}

    init(destination: XingXiaDestination) {
        self.destination = destination
    }

    func perform() async throws -> some IntentResult & OpensIntent {
        .result(opensIntent: OpenURLIntent(destination.url))
    }
}
