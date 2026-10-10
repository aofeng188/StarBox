import SwiftUI

/// 一支签
struct FortuneStick: Decodable, Identifiable, Hashable {
    let n: Int
    let level: String
    let title: String
    let poem: [String]
    let meaning: String

    var id: Int { n }
    var levelInfo: StickLevel { StickLevel(rawValue: level) ?? .middle }
    var numberText: String { "第\(StickLibrary.chineseNumber(n))签" }

    /// 各方面的断语（按签等从断语库中稳定地选取）
    var areas: [(String, String)] {
        var rng = SeededGenerator("stick-area", n)
        return StickLibrary.areaNames.map { area in
            (area, rng.pick(StickLibrary.verdicts[levelInfo]?[area] ?? ["顺其自然"]))
        }
    }
}

enum StickLevel: String, CaseIterable {
    case best = "上上"
    case good = "上吉"
    case fair = "中吉"
    case middle = "中平"
    case low = "下"

    var displayName: String { rawValue == "下" ? "下签" : rawValue + "签" }

    var color: Color {
        switch self {
        case .best: Color(hex: 0xFFD166)
        case .good: Color(hex: 0xFF8FA3)
        case .fair: Color(hex: 0x7DD3FC)
        case .middle: Color(hex: 0xA7F3D0)
        case .low: Color(hex: 0xC4B5FD)
        }
    }

    /// 用作文字颜色时的版本：浅色模式下用加深的同色系，保证在浅底上看得清
    func ink(_ scheme: ColorScheme) -> Color {
        guard scheme == .light else { return color }
        switch self {
        case .best: return Color(hex: 0xA16207)
        case .good: return Color(hex: 0xBE185D)
        case .fair: return Color(hex: 0x0369A1)
        case .middle: return Color(hex: 0x047857)
        case .low: return Color(hex: 0x6D28D9)
        }
    }

    var blessing: String {
        switch self {
        case .best: "大吉大利，心想事成"
        case .good: "吉星高照，诸事顺遂"
        case .fair: "稳中有进，渐入佳境"
        case .middle: "平安是福，守中得益"
        case .low: "静待时机，否极泰来"
        }
    }
}

enum StickLibrary {
    static let all: [FortuneStick] = ContentLibrary.load("sticks")

    static func stick(_ number: Int) -> FortuneStick {
        all.first { $0.n == number } ?? all[0]
    }

    static let areaNames = ["心愿", "事业", "财运", "感情", "健康", "出行"]

    static let verdicts: [StickLevel: [String: [String]]] = [
        .best: [
            "心愿": ["心想事成", "所求必得", "如愿以偿"],
            "事业": ["步步高升", "大展宏图", "贵人提携"],
            "财运": ["财源广进", "正偏皆旺", "积少成多"],
            "感情": ["良缘天成", "甜蜜美满", "情投意合"],
            "健康": ["身康体健", "精神焕发", "百病不侵"],
            "出行": ["一路顺风", "出入平安", "远行大吉"],
        ],
        .good: [
            "心愿": ["终能如愿", "渐次达成", "好事将近"],
            "事业": ["稳步上升", "机会在前", "宜积极进取"],
            "财运": ["收入可观", "小有横财", "理财得宜"],
            "感情": ["情意渐浓", "宜主动表达", "桃花朵朵"],
            "健康": ["状态良好", "注意作息", "宜多运动"],
            "出行": ["顺利平安", "宜结伴同行", "旅途愉快"],
        ],
        .fair: [
            "心愿": ["稍待可成", "需再努力", "循序渐进"],
            "事业": ["稳中求进", "宜守正道", "多请教前辈"],
            "财运": ["收支平衡", "宜开源节流", "小财可得"],
            "感情": ["细水长流", "宜多沟通", "缘分可期"],
            "健康": ["无大碍", "注意饮食", "劳逸结合"],
            "出行": ["平安无事", "宜提前规划", "近行为宜"],
        ],
        .middle: [
            "心愿": ["顺其自然", "缓缓图之", "莫要强求"],
            "事业": ["守成为上", "按部就班", "勿轻易变动"],
            "财运": ["守财为要", "勿贪小利", "量入为出"],
            "感情": ["平淡是真", "宜包容理解", "静待缘分"],
            "健康": ["宜静养", "早睡早起", "注意保暖"],
            "出行": ["宜慎重", "非必要不远行", "注意安全"],
        ],
        .low: [
            "心愿": ["时机未到", "宜先放下", "改日再求"],
            "事业": ["宜韬光养晦", "暂避锋芒", "从小事做起"],
            "财运": ["谨防破财", "勿做投资", "节俭为宜"],
            "感情": ["宜冷静", "勿操之过急", "以退为进"],
            "健康": ["多加休息", "勿过度劳累", "及时就医"],
            "出行": ["宜延期", "注意财物", "结伴更佳"],
        ],
    ]

    static func chineseNumber(_ n: Int) -> String {
        let digits = ["零", "一", "二", "三", "四", "五", "六", "七", "八", "九"]
        if n < 10 { return digits[n] }
        if n == 10 { return "十" }
        if n < 20 { return "十" + digits[n % 10] }
        let tens = digits[n / 10] + "十"
        return n % 10 == 0 ? tens : tens + digits[n % 10]
    }
}
