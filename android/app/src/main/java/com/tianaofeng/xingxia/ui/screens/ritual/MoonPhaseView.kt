package com.tianaofeng.xingxia.ui.screens.ritual

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.MoonPhaseName
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min

/**
 * 按真实亮面比例画的月亮（北半球看）：上半月亮面在右，下半月亮面在左，明暗交界线是半个椭圆（与 iOS 的 MoonPhaseView 一致）。
 * phase 为 0…1（0 新月，0.25 上弦，0.5 满月，0.75 下弦）；glow 为亮面外的柔光，小图标可以关掉；
 * isLit 为 false 时压暗、去色（徽章里还没集到的月相）。纯装饰，读屏文字由使用的地方给出
 */
@Composable
fun MoonPhaseView(phase: Double, size: Dp = 120.dp, glow: Boolean = true, isLit: Boolean = true, modifier: Modifier = Modifier) {
    val isDark = LocalPalette.current.isDark
    val illumination = ((1 - cos(2 * PI * phase)) / 2).toFloat()
    fun tone(color: Color): Color {
        if (isLit) return color
        // 去色：按亮度换成灰
        val luminance = 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
        return Color(luminance, luminance, luminance, color.alpha)
    }
    val darkSide = tone(if (isDark) hex(0x23263A) else hex(0x474B63))
    val litLight = tone(MoonLitLight)
    val litDark = tone(MoonLitDark)
    val crater = tone(hex(0xC9B98A).copy(alpha = 0.35f))
    val rim = if (isDark) Color.White.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.18f)
    val glowColor = MoonLitLight.copy(alpha = 0.5f * illumination)
    Canvas(
        modifier
            .size(size)
            .graphicsLayer { alpha = if (isLit) 1f else 0.32f }
            .clearAndSetSemantics { },
    ) {
        val radius = min(this.size.width, this.size.height) / 2
        val c = Offset(this.size.width / 2, this.size.height / 2)

        // 亮面外的柔光
        if (glow && isLit && illumination > 0.01f) {
            val outer = radius * 1.28f
            drawCircle(
                Brush.radialGradient(
                    0f to glowColor,
                    radius / outer to glowColor,
                    1f to Color.Transparent,
                    center = c,
                    radius = outer,
                ),
                outer,
                c,
            )
        }

        // 暗面
        drawCircle(darkSide, radius, c)

        // 亮面：左上方打光的径向渐变
        val lit = moonLitPath(phase, c, radius)
        drawPath(
            lit,
            Brush.radialGradient(
                listOf(litLight, litDark),
                center = Offset(c.x - radius * 0.3f, c.y - radius * 0.35f),
                radius = radius * 1.6f,
            ),
        )

        // 环形山只出现在亮面上
        clipPath(lit) {
            for ((x, y, r) in MoonCraters) {
                drawCircle(crater, r * radius, Offset(c.x + x * radius, c.y + y * radius))
            }
        }

        drawCircle(rim, radius, c, style = Stroke(maxOf(0.6.dp.toPx(), radius * 0.025f)))
    }
}

/** 某一相的典型样子（取这一相正中的月相） */
@Composable
fun MoonPhaseView(name: MoonPhaseName, size: Dp = 120.dp, glow: Boolean = true, isLit: Boolean = true, modifier: Modifier = Modifier) {
    MoonPhaseView(name.ordinal / 8.0, size, glow, isLit, modifier)
}

private val MoonLitLight = hex(0xFFF6DA)
private val MoonLitDark = hex(0xE2D3A4)

/** 环形山：圆心和半径都是相对月面半径的比例 */
private val MoonCraters = listOf(
    Triple(-0.32f, -0.30f, 0.17f), Triple(0.28f, -0.42f, 0.10f), Triple(0.38f, 0.12f, 0.15f),
    Triple(-0.10f, 0.36f, 0.12f), Triple(-0.48f, 0.18f, 0.08f), Triple(0.08f, -0.05f, 0.07f),
)

/** 亮面的轮廓：亮的一侧的半圆边缘，加上明暗交界线（横向半轴为 r·cos(2π·phase) 的半椭圆） */
private fun moonLitPath(phase: Double, c: Offset, r: Float): Path {
    val p = phase - floor(phase)
    // 上半月亮面在右（+1），下半月在左（-1）
    val side = if (p < 0.5) 1f else -1f
    val limb = side * r
    val terminator = side * cos(2 * PI * p).toFloat() * r
    // 四分之一椭圆的贝塞尔控制点系数
    val k = 0.55228475f
    return Path().apply {
        moveTo(c.x, c.y - r)
        // 上 → 边缘 → 下
        cubicTo(c.x + k * limb, c.y - r, c.x + limb, c.y - k * r, c.x + limb, c.y)
        cubicTo(c.x + limb, c.y + k * r, c.x + k * limb, c.y + r, c.x, c.y + r)
        // 下 → 交界线 → 上
        cubicTo(c.x + k * terminator, c.y + r, c.x + terminator, c.y + k * r, c.x + terminator, c.y)
        cubicTo(c.x + terminator, c.y - k * r, c.x + k * terminator, c.y - r, c.x, c.y - r)
        close()
    }
}
