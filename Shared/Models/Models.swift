import Foundation
import SwiftData

// 所有属性都有默认值，方便以后做轻量迁移。

@Model
final class Profile {
    var id: UUID = UUID()
    var name: String = ""
    var relation: String = "自己"
    var avatar: String = "🌟"
    var colorIndex: Int = 0
    /// 公历生日（存当天中午）
    var birthday: Date = Date()
    var hasBirthTime: Bool = false
    var birthHour: Int = 12
    var birthMinute: Int = 0
    var birthCityID: String?
    /// 录入时是否使用农历（仅用于展示）
    var enteredAsLunar: Bool = false
    var sortIndex: Int = 0
    var createdAt: Date = Date()

    init(name: String, relation: String = "自己", avatar: String = "🌟", birthday: Date) {
        self.name = name
        self.relation = relation
        self.avatar = avatar
        self.birthday = birthday
    }

    var birth: BirthInfo {
        BirthInfo(birthday: birthday,
                  hour: hasBirthTime ? birthHour : nil,
                  minute: hasBirthTime ? birthMinute : nil,
                  cityID: birthCityID)
    }

    var snapshot: ProfileSnapshot {
        ProfileSnapshot(id: id, name: name, avatar: avatar, birthday: birthday,
                        birthHour: hasBirthTime ? birthHour : nil,
                        birthMinute: hasBirthTime ? birthMinute : nil,
                        birthCityID: birthCityID)
    }

    var sunSign: ZodiacSign { ZodiacSign.sunSign(of: birth) }
    var animal: EarthlyBranch { BaZi(birth).zodiacAnimal }
}

/// 所有随机工具的结果记录
@Model
final class DecisionRecord {
    var id: UUID = UUID()
    var tool: String = ""
    var title: String = ""
    var result: String = ""
    var detail: String?
    var date: Date = Date()
    /// 「问一问」里当时在问的问题（AppState.currentQuestion），没有就是 nil
    var question: String?
    /// nil 没表态，true「就这么办」，false「再想想」
    var adopted: Bool?
    /// 回访结果：1 满意，0 一般，-1 后悔；nil 还没回访
    var satisfaction: Int?
    /// 回访（含「跳过」）的时间；有值就不再出现在首页回访卡里
    var followedUpAt: Date?

    init(tool: String, title: String, result: String, detail: String? = nil, date: Date = Date()) {
        self.tool = tool
        self.title = title
        self.result = result
        self.detail = detail
        self.date = date
    }
}

/// 每日一签
@Model
final class StickDraw {
    var id: UUID = UUID()
    var dayKey: Int = 0
    var profileID: UUID?
    var stickNumber: Int = 1
    var date: Date = Date()

    init(dayKey: Int, profileID: UUID?, stickNumber: Int) {
        self.dayKey = dayKey
        self.profileID = profileID
        self.stickNumber = stickNumber
    }
}

@Model
final class TarotReading {
    var id: UUID = UUID()
    var date: Date = Date()
    var spread: String = ""
    /// 形如 "0u,13r,5u"：牌号 + 正(u)/逆(r)
    var cards: String = ""
    var question: String = ""

    init(spread: String, cards: String, question: String) {
        self.spread = spread
        self.cards = cards
        self.question = question
    }
}

@Model
final class FoodMenu {
    var id: UUID = UUID()
    var name: String = ""
    var icon: String = "🍱"
    var createdAt: Date = Date()
    @Relationship(deleteRule: .cascade, inverse: \FoodMenuItem.menu)
    var items: [FoodMenuItem]? = []

    init(name: String, icon: String = "🍱") {
        self.name = name
        self.icon = icon
    }

    var sortedItems: [FoodMenuItem] { (items ?? []).sorted { $0.sortIndex < $1.sortIndex } }
}

@Model
final class FoodMenuItem {
    var id: UUID = UUID()
    var name: String = ""
    var note: String = ""
    var weight: Double = 1
    var sortIndex: Int = 0
    var menu: FoodMenu?

    init(name: String, note: String = "", weight: Double = 1, sortIndex: Int = 0) {
        self.name = name
        self.note = note
        self.weight = weight
        self.sortIndex = sortIndex
    }
}

/// 「就吃这个」的记录，用于智能去重
@Model
final class FoodLog {
    var id: UUID = UUID()
    var name: String = ""
    var source: String = ""
    var date: Date = Date()

    init(name: String, source: String) {
        self.name = name
        self.source = source
    }
}

@Model
final class BlockedFood {
    var name: String = ""
    var date: Date = Date()

    init(name: String) { self.name = name }
}

@Model
final class LotteryPool {
    var id: UUID = UUID()
    var name: String = ""
    var icon: String = "🎁"
    var removeAfterDraw: Bool = true
    var createdAt: Date = Date()
    @Relationship(deleteRule: .cascade, inverse: \LotteryItem.pool)
    var items: [LotteryItem]? = []

    init(name: String, icon: String = "🎁", removeAfterDraw: Bool = true) {
        self.name = name
        self.icon = icon
        self.removeAfterDraw = removeAfterDraw
    }

    var sortedItems: [LotteryItem] { (items ?? []).sorted { $0.sortIndex < $1.sortIndex } }
}

@Model
final class LotteryItem {
    var id: UUID = UUID()
    var name: String = ""
    var weight: Double = 1
    var quantity: Int = 1
    var remaining: Int = 1
    var sortIndex: Int = 0
    var pool: LotteryPool?

    init(name: String, weight: Double = 1, quantity: Int = 1, sortIndex: Int = 0) {
        self.name = name
        self.weight = weight
        self.quantity = quantity
        self.remaining = quantity
        self.sortIndex = sortIndex
    }
}

@Model
final class WheelTemplate {
    var id: UUID = UUID()
    var name: String = ""
    var removeAfterSpin: Bool = false
    var createdAt: Date = Date()
    @Relationship(deleteRule: .cascade, inverse: \WheelOption.template)
    var options: [WheelOption]? = []

    init(name: String) { self.name = name }

    var sortedOptions: [WheelOption] { (options ?? []).sorted { $0.sortIndex < $1.sortIndex } }
}

@Model
final class WheelOption {
    var id: UUID = UUID()
    var title: String = ""
    var weight: Double = 1
    var sortIndex: Int = 0
    var template: WheelTemplate?

    init(title: String, weight: Double = 1, sortIndex: Int = 0) {
        self.title = title
        self.weight = weight
        self.sortIndex = sortIndex
    }
}

@Model
final class Roster {
    var id: UUID = UUID()
    var name: String = ""
    var members: [String] = []
    var createdAt: Date = Date()

    init(name: String, members: [String]) {
        self.name = name
        self.members = members
    }
}

/// 真心话大冒险的自定义题目
@Model
final class CustomPrompt {
    var id: UUID = UUID()
    /// "truth" / "dare"
    var kind: String = "truth"
    var text: String = ""
    var level: Int = 0
    var createdAt: Date = Date()

    init(kind: String, text: String, level: Int) {
        self.kind = kind
        self.text = text
        self.level = level
    }
}

@Model
final class ScratchPreset {
    var id: UUID = UUID()
    var name: String = ""
    var prizes: [String] = []
    var blankWeight: Double = 1
    var createdAt: Date = Date()

    init(name: String, prizes: [String], blankWeight: Double = 1) {
        self.name = name
        self.prizes = prizes
        self.blankWeight = blankWeight
    }
}

@Model
final class Countdown {
    var id: UUID = UUID()
    var title: String = ""
    var emoji: String = "🎉"
    var date: Date = Date()
    /// "countdown" 倒数日 / "anniversary" 纪念日 / "birthday" 生日
    var kind: String = "countdown"
    var repeatsYearly: Bool = false
    var isLunar: Bool = false
    var lunarMonth: Int = 1
    var lunarDay: Int = 1
    var colorIndex: Int = 0
    var pinned: Bool = false
    var createdAt: Date = Date()

    init(title: String, emoji: String, date: Date, kind: String) {
        self.title = title
        self.emoji = emoji
        self.date = date
        self.kind = kind
    }
}

@Model
final class MoodEntry {
    var id: UUID = UUID()
    var dayKey: Int = 0
    /// 1…5
    var mood: Int = 3
    var note: String = ""
    var tags: [String] = []
    var date: Date = Date()

    init(dayKey: Int, mood: Int, note: String = "") {
        self.dayKey = dayKey
        self.mood = mood
        self.note = note
    }
}

/// 每日仪式：某天看过（或跳过）了仪式。每天最多一条，月相徽章按完成的日子统计
@Model
final class RitualDay {
    var dayKey: Int = 0
    var completedAt: Date = Date()
    var skipped: Bool = false
    /// MoonPhaseName.rawValue
    var moonPhase: Int = 0

    init(dayKey: Int, completedAt: Date = Date(), skipped: Bool = false, moonPhase: Int = 0) {
        self.dayKey = dayKey
        self.completedAt = completedAt
        self.skipped = skipped
        self.moonPhase = moonPhase
    }
}

enum AppSchema {
    static let models: [any PersistentModel.Type] = [
        Profile.self, DecisionRecord.self, StickDraw.self, TarotReading.self,
        FoodMenu.self, FoodMenuItem.self, FoodLog.self, BlockedFood.self,
        LotteryPool.self, LotteryItem.self, WheelTemplate.self, WheelOption.self,
        Roster.self, CustomPrompt.self, ScratchPreset.self, Countdown.self, MoodEntry.self, RitualDay.self,
    ]

    /// App 与小组件共用同一个数据库；App Group 不可用时退回 App 自己的沙盒。
    static func makeContainer() throws -> ModelContainer {
        let schema = Schema(models)
        let configuration: ModelConfiguration
        if AppGroup.isAvailable {
            configuration = ModelConfiguration("XingXia", schema: schema, groupContainer: .identifier(AppGroup.identifier))
        } else {
            configuration = ModelConfiguration("XingXia", schema: schema)
        }
        return try ModelContainer(for: schema, configurations: [configuration])
    }
}
