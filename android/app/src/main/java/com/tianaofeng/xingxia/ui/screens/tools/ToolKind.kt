package com.tianaofeng.xingxia.ui.screens.tools

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tianaofeng.xingxia.ui.theme.hex

enum class ToolCategory(val label: String, val icon: ImageVector) {
    DECIDE("帮我决定", Icons.Rounded.AutoFixHigh),
    RANDOM("随机一下", Icons.Rounded.Shuffle),
    PARTY("聚会时光", Icons.Rounded.Celebration),
}

/** 百宝箱里的所有工具。available = false 的工具会在后续版本移植。 */
enum class ToolKind(
    val id: String,
    val label: String,
    val subtitle: String,
    val icon: ImageVector,
    private val pair: Pair<Long, Long>,
    val category: ToolCategory,
    val keywords: String,
    val available: Boolean = true,
) {
    FOOD("food", "今天吃什么", "终结选择困难", Icons.Rounded.Restaurant, 0xFF9F43L to 0xFF4D6DL, ToolCategory.DECIDE, "吃 饭 外卖 餐厅 午饭 晚饭 美食"),
    WHEEL("wheel", "幸运大转盘", "转出你的答案", Icons.Rounded.DonutLarge, 0x7C5CFFL to 0xD946EFL, ToolCategory.DECIDE, "转盘 决定 选择"),
    LOTTERY("lottery", "自定义抽奖", "年会、团建都能用", Icons.Rounded.Redeem, 0xF43F5EL to 0xFB923CL, ToolCategory.DECIDE, "抽奖 奖品 年会"),
    FINGER("finger", "指尖轮盘", "多人按住屏幕选一个", Icons.Rounded.TouchApp, 0x22D3EEL to 0x6366F1L, ToolCategory.DECIDE, "手指 选人 谁 买单"),
    GROUPS("groups", "随机分组", "分组 · 排序 · 点名", Icons.Rounded.Groups, 0x10B981L to 0x22D3EEL, ToolCategory.DECIDE, "分组 排序 名单 点名"),
    NUMBER("number", "随机数字", "范围、个数随心定", Icons.Rounded.Tag, 0x3B82F6L to 0x8B5CF6L, ToolCategory.RANDOM, "数字 号码 彩票 双色球"),
    COIN("coin", "抛硬币", "正面还是反面", Icons.Rounded.MonetizationOn, 0xF5B83DL to 0xD97706L, ToolCategory.RANDOM, "硬币 正反"),
    DICE("dice", "骰子", "摇一摇掷骰子", Icons.Rounded.Casino, 0xEC4899L to 0x8B5CF6L, ToolCategory.RANDOM, "骰子 色子"),
    GENERATORS("generators", "灵感生成器", "颜色 · 密码 · 名字", Icons.Rounded.AutoFixHigh, 0x06B6D4L to 0x34F5C5L, ToolCategory.RANDOM, "颜色 密码 名字 网名 字母 配色"),
    TRUTH_DARE("truthDare", "真心话大冒险", "破冰神器", Icons.Rounded.Forum, 0xF472B6L to 0xEF4444L, ToolCategory.PARTY, "真心话 大冒险 游戏"),
    SCRATCH("scratch", "刮刮乐", "刮开有惊喜", Icons.Rounded.Style, 0x94A3B8L to 0x6366F1L, ToolCategory.PARTY, "刮刮卡 惊喜"),
    RPS("rps", "石头剪刀布", "和星匣一决胜负", Icons.Rounded.PanTool, 0x84CC16L to 0x10B981L, ToolCategory.PARTY, "猜拳 剪刀 石头 布");

    val colors: List<Color> get() = listOf(hex(pair.first), hex(pair.second))

    companion object {
        fun of(id: String?): ToolKind? = entries.firstOrNull { it.id == id }
        fun inCategory(category: ToolCategory) = entries.filter { it.category == category }
    }
}

/** 历史记录里显示的工具名称和图标 */
fun toolInfo(tool: String): Triple<String, ImageVector, List<Color>> {
    ToolKind.of(tool)?.let { return Triple(it.label, it.icon, it.colors) }
    return when (tool) {
        "stick" -> Triple("求签", Icons.Rounded.Celebration, listOf(hex(0xFF8A3D), hex(0xE11D48)))
        "tarot" -> Triple("塔罗牌", Icons.Rounded.Style, listOf(hex(0x8B5CF6), hex(0x3B82F6)))
        "answers" -> Triple("答案之书", Icons.Rounded.AutoFixHigh, listOf(hex(0xF5B83D), hex(0xB45309)))
        else -> Triple("其他", Icons.Rounded.Casino, listOf(hex(0x94A3B8), hex(0x64748B)))
    }
}
