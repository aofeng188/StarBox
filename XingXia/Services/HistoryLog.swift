import SwiftData
import SwiftUI

/// 往「决策历史」里记一笔
@MainActor
enum HistoryLog {
    static func add(_ context: ModelContext, tool: String, title: String, result: String, detail: String? = nil) {
        context.insert(DecisionRecord(tool: tool, title: title, result: result, detail: detail))
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
