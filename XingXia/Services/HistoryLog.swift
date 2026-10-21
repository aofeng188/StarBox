import SwiftData
import SwiftUI

/// 往「决策历史」里记一笔，以及记下之后的表态和回访
@MainActor
enum HistoryLog {
    /// 记一笔并返回这条记录。「问一问」里正在问的问题（AppState.currentQuestion）一起记进 question；
    /// App 在前台时把它设为 AppState.lastDecision，RootView 据此弹出「就这么办 / 再想想」
    @discardableResult
    static func add(_ context: ModelContext, tool: String, title: String, result: String, detail: String? = nil) -> DecisionRecord {
        let record = DecisionRecord(tool: tool, title: title, result: result, detail: detail)
        if let question = AppState.shared.currentQuestion?.trimmingCharacters(in: .whitespacesAndNewlines), !question.isEmpty {
            record.question = question
        }
        context.insert(record)
        try? context.save()
        if UIApplication.shared.applicationState == .active {
            AppState.shared.lastDecision = record
        }
        return record
    }

    /// 「就这么办」(true) /「再想想」(false)
    static func setAdopted(_ record: DecisionRecord, _ adopted: Bool, context: ModelContext) {
        guard !record.isDeleted else { return }
        record.adopted = adopted
        try? context.save()
    }

    /// 回访：1 满意，0 一般，-1 后悔；nil 表示「跳过」，只记回访时间。
    /// 不管选了哪一项都会写 followedUpAt，这条记录之后不再出现在首页回访卡里
    static func setSatisfaction(_ record: DecisionRecord, _ value: Int?, context: ModelContext) {
        guard !record.isDeleted else { return }
        record.satisfaction = value.map { min(max($0, -1), 1) }
        record.followedUpAt = Date()
        try? context.save()
    }

    /// 历史记录里显示的工具名称和图标
    static func info(for tool: String) -> (name: String, symbol: String, colors: [Color]) {
        if let kind = ToolKind(rawValue: tool) { return (kind.name, kind.symbol, kind.colors) }
        switch tool {
        case "stick": return ("求签", "flame.fill", [Color(hex: 0xFF8A3D), Color(hex: 0xE11D48)])
        case "jiaobei": return ("掷筊", "moon.fill", [Color(hex: 0xEF4444), Color(hex: 0x9F1239)])
        case "tarot": return ("塔罗牌", "suit.diamond.fill", [Color(hex: 0x8B5CF6), Color(hex: 0x3B82F6)])
        case "answers": return ("答案之书", "book.fill", [Color(hex: 0xF5B83D), Color(hex: 0xB45309)])
        case "nearby": return ("附近餐厅", "mappin.and.ellipse", [Color(hex: 0xFF9F43), Color(hex: 0xFF4D6D)])
        default: return ("其他", "sparkles", [Color(hex: 0x94A3B8), Color(hex: 0x64748B)])
        }
    }
}
