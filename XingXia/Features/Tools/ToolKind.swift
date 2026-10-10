import SwiftUI

enum ToolCategory: String, CaseIterable, Identifiable {
    case decide, random, party

    var id: String { rawValue }
    var name: String { ["decide": "帮我决定", "random": "随机一下", "party": "聚会时光"][rawValue]! }
    var symbol: String { ["decide": "wand.and.stars", "random": "dice", "party": "party.popper"][rawValue]! }
}

/// 百宝箱里的所有工具
enum ToolKind: String, CaseIterable, Identifiable, Hashable {
    case food, wheel, lottery, finger, groups
    case number, coin, dice, generators
    case truthDare, scratch, rps

    var id: String { rawValue }

    var name: String {
        switch self {
        case .food: "今天吃什么"
        case .wheel: "幸运大转盘"
        case .lottery: "自定义抽奖"
        case .finger: "指尖轮盘"
        case .groups: "随机分组"
        case .number: "随机数字"
        case .coin: "抛硬币"
        case .dice: "3D 骰子"
        case .generators: "灵感生成器"
        case .truthDare: "真心话大冒险"
        case .scratch: "刮刮乐"
        case .rps: "石头剪刀布"
        }
    }

    var subtitle: String {
        switch self {
        case .food: "终结选择困难"
        case .wheel: "转出你的答案"
        case .lottery: "年会、团建都能用"
        case .finger: "多人按住屏幕选一个"
        case .groups: "分组 · 排序 · 点名"
        case .number: "范围、个数随心定"
        case .coin: "正面还是反面"
        case .dice: "物理引擎真实翻滚"
        case .generators: "颜色 · 密码 · 名字"
        case .truthDare: "破冰神器"
        case .scratch: "刮开有惊喜"
        case .rps: "和星匣一决胜负"
        }
    }

    var symbol: String {
        switch self {
        case .food: "fork.knife"
        case .wheel: "circle.dashed"
        case .lottery: "gift.fill"
        case .finger: "hand.point.up.left.fill"
        case .groups: "person.3.fill"
        case .number: "number"
        case .coin: "centsign.circle.fill"
        case .dice: "dice.fill"
        case .generators: "wand.and.rays"
        case .truthDare: "bubble.left.and.exclamationmark.bubble.right.fill"
        case .scratch: "rectangle.and.hand.point.up.left.fill"
        case .rps: "hand.raised.fill"
        }
    }

    var colors: [Color] {
        let pair: (UInt32, UInt32) = switch self {
        case .food: (0xFF9F43, 0xFF4D6D)
        case .wheel: (0x7C5CFF, 0xD946EF)
        case .lottery: (0xF43F5E, 0xFB923C)
        case .finger: (0x22D3EE, 0x6366F1)
        case .groups: (0x10B981, 0x22D3EE)
        case .number: (0x3B82F6, 0x8B5CF6)
        case .coin: (0xF5B83D, 0xD97706)
        case .dice: (0xEC4899, 0x8B5CF6)
        case .generators: (0x06B6D4, 0x34F5C5)
        case .truthDare: (0xF472B6, 0xEF4444)
        case .scratch: (0x94A3B8, 0x6366F1)
        case .rps: (0x84CC16, 0x10B981)
        }
        return [Color(hex: pair.0), Color(hex: pair.1)]
    }

    var category: ToolCategory {
        switch self {
        case .food, .wheel, .lottery, .finger, .groups: .decide
        case .number, .coin, .dice, .generators: .random
        case .truthDare, .scratch, .rps: .party
        }
    }

    var keywords: String {
        switch self {
        case .food: "吃 饭 外卖 餐厅 午饭 晚饭 美食"
        case .wheel: "转盘 决定 选择"
        case .lottery: "抽奖 奖品 年会"
        case .finger: "手指 选人 谁 买单"
        case .groups: "分组 排序 名单 点名"
        case .number: "数字 号码 彩票 双色球"
        case .coin: "硬币 正反"
        case .dice: "骰子 色子"
        case .generators: "颜色 密码 名字 网名 字母 配色"
        case .truthDare: "真心话 大冒险 游戏"
        case .scratch: "刮刮卡 惊喜"
        case .rps: "猜拳 剪刀 石头 布"
        }
    }

    static func tools(in category: ToolCategory) -> [ToolKind] {
        allCases.filter { $0.category == category }
    }
}
