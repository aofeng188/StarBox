package com.tianaofeng.xingxia.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.tianaofeng.xingxia.core.NamedColor
import com.tianaofeng.xingxia.core.WuXing
import com.tianaofeng.xingxia.core.ZodiacElement

fun hex(value: Long, alpha: Float = 1f): Color = Color(0xFF000000 or value).copy(alpha = alpha)

fun NamedColor.toColor(): Color = Color(0xFF000000 or hex.toLong())

/** 一套配色（主题的深色或浅色版本） */
data class ThemePalette(
    val isDark: Boolean,
    val backgroundTop: Color,
    val backgroundBottom: Color,
    /** 星云/极光色块，4 个 */
    val nebula: List<Color>,
    val accent: Color,
    val accentSecondary: Color,
    val star: Color,
) {
    val accentBrush: Brush get() = Brush.linearGradient(listOf(accent, accentSecondary))

    /** 在卡片、图标等处轮换使用的一组彩色 */
    val spectrum: List<Color> get() = listOf(accent, accentSecondary) + nebula

    fun color(index: Int): Color = spectrum[Math.floorMod(index, spectrum.size)]

    val text: Color get() = if (isDark) Color(0xFFF5F3FF) else Color(0xFF1B1726)
    /** 次要文字：浅色模式下玻璃卡片透出彩色光斑，0.62 偏淡，提一档（与 iOS 的 subtleForeground 一致） */
    val secondaryText: Color get() = text.copy(alpha = if (isDark) 0.62f else 0.72f)
    val tertiaryText: Color get() = text.copy(alpha = 0.36f)
    /** 相当于 SwiftUI 的 `.primary.opacity(x)` */
    fun subtle(alpha: Float = 0.08f): Color = text.copy(alpha = alpha)

    val glassFill: Color get() = if (isDark) Color.White.copy(alpha = 0.075f) else Color.White.copy(alpha = 0.58f)
    val glassStroke: Color get() = if (isDark) Color.White.copy(alpha = 0.13f) else Color.White.copy(alpha = 0.85f)

    /** 压在 accent / accentBrush 上的文字色：深色主题的强调色都偏亮，白字看不清，改用深色字（与 iOS 一致） */
    val onAccent: Color get() = if (isDark) INK_DARK else Color.White

    /** 把任意颜色调成能直接当文字用的版本：深色模式提亮过暗的颜色，浅色模式压暗过浅的颜色 */
    fun ink(color: Color): Color {
        val luminance = color.luminance()
        return if (isDark) {
            if (luminance < 0.2f) color.mix(Color.White, 0.45f) else color
        } else {
            if (luminance > 0.25f) color.mix(Color.Black, 0.5f) else color
        }
    }

    /** spectrum 的文字版本，和 [color] 一一对应 */
    fun inkColor(index: Int): Color = ink(color(index))

    /**
     * 标签（同色系浅底上的小字，如 TagChip）的文字色：要比底色深两档。
     * 深色模式用 ink；浅色模式在 ink 的基础上继续压暗同色系，直到相对亮度不超过 0.1（压在同色浅底上约 6:1），与 iOS 的 TagChip 一致
     */
    fun tagInk(color: Color): Color = if (isDark) ink(color) else ink(color).darkened(toLuminance = 0.1f)

    /** 工具、签、答案之书、掷筊等写死的身份色向主题强调色靠一点（默认 20%，Oklab 插值）：保留辨识度，换主题时不和强调色打架（与 iOS 一致） */
    fun identity(color: Color, amount: Float = 0.2f): Color = lerp(color, accent, amount)

    fun identity(colors: List<Color>, amount: Float = 0.2f): List<Color> = colors.map { identity(it, amount) }

    /** 语义色：深色模式用明亮的 400 级，浅色模式用加深的 700 级，当文字用时也读得清 */
    val positive: Color get() = if (isDark) hex(0x34D399) else hex(0x047857)
    val negative: Color get() = if (isDark) hex(0xFB7185) else hex(0xBE123C)
    val gold: Color get() = if (isDark) hex(0xFFD166) else hex(0xA16207)
    val caution: Color get() = if (isDark) hex(0xFBBF24) else hex(0xB45309)
}

private val INK_DARK = Color(0xFF14101F)

private fun Color.mix(other: Color, amount: Float) = Color(
    red + (other.red - red) * amount,
    green + (other.green - green) * amount,
    blue + (other.blue - blue) * amount,
    alpha,
)

/** 按比例缩小 sRGB 分量（色相不变）直到相对亮度不超过 toLuminance；本来就够深时原样返回（与 iOS 的 darkened 一致） */
private fun Color.darkened(toLuminance: Float): Color {
    var factor = 1f
    var result = this
    while (factor > 0.3f && result.luminance() > toLuminance) {
        factor -= 0.05f
        result = Color(red * factor, green * factor, blue * factor, alpha)
    }
    return result
}

/** 压在这个颜色上的文字该用的颜色：亮底用深字，暗底用白字 */
val Color.readableForeground: Color get() = if (luminance() > 0.18f) INK_DARK else Color.White

data class AppTheme(
    val id: String,
    val name: String,
    val subtitle: String,
    val dark: ThemePalette,
    val light: ThemePalette,
) {
    fun palette(dark: Boolean) = if (dark) this.dark else light

    companion object {
        private fun p(dark: Boolean, top: Long, bottom: Long, nebula: List<Long>, accent: Long, second: Long, star: Long) =
            ThemePalette(dark, hex(top), hex(bottom), nebula.map { hex(it) }, hex(accent), hex(second), hex(star))

        val NEBULA = AppTheme(
            "nebula", "星云紫", "猎户座星云的梦幻紫粉",
            p(true, 0x0B0720, 0x1A0B3B, listOf(0x7C5CFF, 0xD946EF, 0x3B82F6, 0xF472B6), 0xA78BFA, 0xF472B6, 0xFFFFFF),
            p(false, 0xF6F1FF, 0xFDEAF5, listOf(0xC4B5FD, 0xF5D0FE, 0xBFDBFE, 0xFBCFE8), 0x7C3AED, 0xDB2777, 0x8B5CF6),
        )
        val AURORA = AppTheme(
            "aurora", "极光绿", "北极圈夜空的流动绿光",
            p(true, 0x02121A, 0x05282A, listOf(0x10B981, 0x22D3EE, 0x34F5C5, 0x6366F1), 0x34F5C5, 0x38BDF8, 0xFFFFFF),
            p(false, 0xECFEF7, 0xE2F6FF, listOf(0xA7F3D0, 0xA5F3FC, 0x99F6E4, 0xC7D2FE), 0x059669, 0x0284C7, 0x10B981),
        )
        val SUNSET = AppTheme(
            "sunset", "日落橙", "地平线上最后一抹晚霞",
            p(true, 0x1A0A0F, 0x3A1022, listOf(0xFF7A45, 0xFF3D7F, 0xFFC15E, 0x9D4EDD), 0xFF9F43, 0xFF4D6D, 0xFFE8D6),
            p(false, 0xFFF4EC, 0xFFE6EE, listOf(0xFED7AA, 0xFBCFE8, 0xFDE68A, 0xE9D5FF), 0xEA580C, 0xE11D48, 0xF97316),
        )
        val ABYSS = AppTheme(
            "abyss", "深海蓝", "万米之下的幽蓝微光",
            p(true, 0x020817, 0x071C46, listOf(0x1D4ED8, 0x06B6D4, 0x6366F1, 0x0EA5E9), 0x60A5FA, 0x22D3EE, 0xE0F2FE),
            p(false, 0xEEF6FF, 0xE0F2FE, listOf(0xBFDBFE, 0xA5F3FC, 0xC7D2FE, 0xBAE6FD), 0x2563EB, 0x0891B2, 0x3B82F6),
        )
        val RED_GIANT = AppTheme(
            "redGiant", "红巨星", "燃烧殆尽前最炽烈的光",
            p(true, 0x160305, 0x3A0A0C, listOf(0xEF4444, 0xF97316, 0xBE185D, 0xFBBF24), 0xFF6B6B, 0xFFB86B, 0xFFE4E1),
            p(false, 0xFFF1F0, 0xFFF4E6, listOf(0xFECACA, 0xFED7AA, 0xFBCFE8, 0xFDE68A), 0xDC2626, 0xEA580C, 0xEF4444),
        )
        val SAKURA = AppTheme(
            "sakura", "樱花粉", "春夜里飘落的花瓣",
            p(true, 0x1A0915, 0x351333, listOf(0xFF8FC7, 0xC084FC, 0xFDA4AF, 0x818CF8), 0xFF8FC7, 0xC084FC, 0xFFE4F1),
            p(false, 0xFFF5FA, 0xFBEFFF, listOf(0xFBCFE8, 0xE9D5FF, 0xFECDD3, 0xE0E7FF), 0xDB2777, 0x9333EA, 0xEC4899),
        )
        val EVENT_HORIZON = AppTheme(
            "eventHorizon", "黑洞", "事件视界边缘的金色吸积盘",
            p(true, 0x000000, 0x0A0A10, listOf(0x3F3F55, 0xE8C77A, 0x6B5B95, 0x1F2937), 0xF5D78E, 0xE5E7EB, 0xF5F5F5),
            p(false, 0xF7F7F8, 0xEDEDF0, listOf(0xE5E7EB, 0xF3E8C8, 0xDDD6FE, 0xD1D5DB), 0x3F3F46, 0xA16207, 0x71717A),
        )
        val SOLAR_GOLD = AppTheme(
            "solarGold", "鎏金", "日冕与金色星尘",
            p(true, 0x120C02, 0x2A1B05, listOf(0xF59E0B, 0xFCD34D, 0xB45309, 0xF97316), 0xFFD166, 0xF4A261, 0xFFF7E0),
            p(false, 0xFFFBEB, 0xFEF3C7, listOf(0xFDE68A, 0xFEF3C7, 0xFCD34D, 0xFED7AA), 0xB45309, 0xD97706, 0xD97706),
        )

        val ALL = listOf(NEBULA, AURORA, SUNSET, ABYSS, RED_GIANT, SAKURA, EVENT_HORIZON, SOLAR_GOLD)

        fun find(id: String?): AppTheme = ALL.firstOrNull { it.id == id } ?: NEBULA

        /** 五行 → 主题（「跟随幸运色」规则使用） */
        fun forElement(element: WuXing) = when (element) {
            WuXing.WOOD -> AURORA
            WuXing.FIRE -> RED_GIANT
            WuXing.EARTH -> SOLAR_GOLD
            WuXing.METAL -> EVENT_HORIZON
            WuXing.WATER -> ABYSS
        }

        /** 星座元素 → 主题 */
        fun forZodiac(element: ZodiacElement) = when (element) {
            ZodiacElement.FIRE -> SUNSET
            ZodiacElement.EARTH -> SOLAR_GOLD
            ZodiacElement.AIR -> NEBULA
            ZodiacElement.WATER -> ABYSS
        }
    }
}

val LocalPalette = staticCompositionLocalOf { AppTheme.NEBULA.dark }

/** 五行的展示色 */
val WuXing.color: Color
    get() = when (this) {
        WuXing.WOOD -> hex(0x4ADE80)
        WuXing.FIRE -> hex(0xF87171)
        WuXing.EARTH -> hex(0xE0A84A)
        WuXing.METAL -> hex(0xD4AF37)
        WuXing.WATER -> hex(0x60A5FA)
    }

@Composable
fun XingXiaTheme(palette: ThemePalette, content: @Composable () -> Unit) {
    val scheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.accentSecondary,
            background = palette.backgroundTop,
            surface = palette.backgroundBottom,
            onSurface = palette.text,
            onBackground = palette.text,
            surfaceContainerHigh = palette.backgroundBottom,
            surfaceContainer = palette.backgroundBottom,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.accentSecondary,
            background = palette.backgroundTop,
            surface = palette.backgroundTop,
            onSurface = palette.text,
            onBackground = palette.text,
            surfaceContainerHigh = palette.backgroundTop,
            surfaceContainer = palette.backgroundTop,
        )
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
