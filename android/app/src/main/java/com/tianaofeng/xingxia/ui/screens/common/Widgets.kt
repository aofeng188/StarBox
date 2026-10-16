package com.tianaofeng.xingxia.ui.screens.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.FortuneCategory
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.MeterBar
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.copyText
import com.tianaofeng.xingxia.ui.components.shareText
import com.tianaofeng.xingxia.ui.components.starPath
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.readableForeground
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

val FortuneCategory.icon: ImageVector
    get() = when (this) {
        FortuneCategory.LOVE -> Icons.Rounded.Favorite
        FortuneCategory.CAREER -> Icons.Rounded.Work
        FortuneCategory.WEALTH -> Icons.Rounded.Paid
        FortuneCategory.HEALTH -> Icons.Rounded.Spa
    }

/** 发光的星座徽章 */
@Composable
fun ZodiacBadge(sign: ZodiacSign, size: Dp = 64.dp, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            drawCircle(
                Brush.radialGradient(
                    listOf(palette.accent.copy(alpha = 0.55f), palette.accentSecondary.copy(alpha = 0.25f), Color.Transparent),
                    center, r * 1.2f,
                ),
                r,
            )
            drawCircle(
                Brush.sweepGradient(listOf(palette.accent, palette.accentSecondary, palette.accent), center),
                r - 0.75.dp.toPx(),
                style = Stroke(1.5.dp.toPx()),
            )
        }
        Text(
            sign.glyph,
            style = TextStyle(fontSize = (size.value * 0.44f).sp, brush = palette.accentBrush, textAlign = TextAlign.Center),
        )
    }
}

@Composable
fun CategoryMeter(category: FortuneCategory, value: Int, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(category.icon, null, Modifier.size(12.dp), tint = palette.secondaryText)
            Text(category.shortName, style = Fonts.caption, color = palette.secondaryText, modifier = Modifier.weight(1f))
            Text("$value", style = Fonts.caption.weight(FontWeight.SemiBold), color = palette.secondaryText)
        }
        MeterBar(value, height = 5.dp)
    }
}

/** 宜 / 忌 一行 */
@Composable
fun YiJiRow(isYi: Boolean, items: List<String>) {
    val palette = LocalPalette.current
    val color = if (isYi) palette.positive else palette.negative
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.75f)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (isYi) "宜" else "忌", style = Fonts.serif(18, bold = true), color = color.readableForeground)
        }
        Flow(spacing = 8.dp, modifier = Modifier.weight(1f)) {
            for (item in items) {
                Text(
                    item,
                    style = Fonts.subheadline.weight(FontWeight.Medium),
                    color = palette.text,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.13f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** 每日一句：点按弹出「拷贝 / 分享」（与 iOS 的长按菜单一致） */
@Composable
fun QuoteCard(quote: String) {
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        GlassCard(
            padding = 22.dp,
            horizontalAlignment = Alignment.CenterHorizontally,
            spacing = 10.dp,
            onClick = { menu = true },
        ) {
            GradientIcon(Icons.Rounded.FormatQuote, size = 24.dp)
            Text(quote, style = Fonts.serif(19).copy(lineHeight = 30.sp), textAlign = TextAlign.Center, color = LocalPalette.current.text)
            SecondaryText("— 星匣 · 每日一句（点按拷贝或分享）", style = Fonts.caption2)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("拷贝") },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                onClick = {
                    menu = false
                    copyText(context, quote)
                },
            )
            DropdownMenuItem(
                text = { Text("分享") },
                leadingIcon = { Icon(Icons.Rounded.Share, null) },
                onClick = {
                    menu = false
                    shareText(context, "$quote\n— 星匣 · 每日一句")
                },
            )
        }
    }
}

/** 幸运小格子（标题 + 内容） */
@Composable
fun LuckyItem(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SecondaryText(title, style = Fonts.caption2)
        content()
    }
}

// region 签

/** 签等的展示色 */
fun stickLevelColor(level: String): Color = when (level) {
    "上上" -> hex(0xFFD166)
    "上吉" -> hex(0xFF8FA3)
    "中吉" -> hex(0x7DD3FC)
    "下" -> hex(0xC4B5FD)
    else -> hex(0xA7F3D0)
}

/** 用作文字颜色时的版本：浅色模式下用加深的同色系 */
fun stickLevelInk(level: String, dark: Boolean): Color = if (dark) stickLevelColor(level) else when (level) {
    "上上" -> hex(0xA16207)
    "上吉" -> hex(0xBE185D)
    "中吉" -> hex(0x0369A1)
    "下" -> hex(0x6D28D9)
    else -> hex(0x047857)
}

/** 签筒插画 */
@Composable
fun StickTubeIllustration(size: Dp = 96.dp, shake: Float = 0f, modifier: Modifier = Modifier) {
    Box(modifier.size(size, size * 1.05f), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val s = this.size.width
            val bottomY = this.size.height
            val tubeW = s * 0.5f
            val tubeH = s * 0.62f
            val tubeTop = bottomY - tubeH
            // 签子
            for (i in 0 until 7) {
                val angle = (i - 3) * 6f + sin(shake * 3 + i) * 4f
                val stickW = s * 0.07f
                val stickH = s * 0.75f
                val pivot = Offset(s / 2 + (i - 3) * s * 0.05f, tubeTop + tubeH * 0.55f - s * 0.32f + abs(i - 3) * s * 0.02f - sin(shake * 5 + i) * s * 0.04f + stickH / 2)
                rotate(angle, pivot) {
                    drawRoundRect(
                        Brush.verticalGradient(listOf(hex(0xF5DEB3), hex(0xD2A86E)), pivot.y - stickH, pivot.y),
                        Offset(pivot.x - stickW / 2, pivot.y - stickH), Size(stickW, stickH), CornerRadius(2.dp.toPx()),
                    )
                    drawRoundRect(hex(0xC0392B), Offset(pivot.x - stickW / 2, pivot.y - stickH), Size(stickW, s * 0.08f), CornerRadius(2.dp.toPx()))
                }
            }
            // 筒身
            val left = (s - tubeW) / 2
            drawRoundRect(
                Brush.horizontalGradient(listOf(hex(0xC0392B), hex(0x7B1E1E)), left, left + tubeW),
                Offset(left, tubeTop), Size(tubeW, tubeH), CornerRadius(s * 0.08f),
            )
            val gold = hex(0xFFD166)
            drawRoundRect(gold, Offset(left + s * 0.06f, tubeTop + tubeH * 0.2f), Size(tubeW - s * 0.12f, s * 0.025f), CornerRadius(s * 0.0125f))
            drawRoundRect(gold, Offset(left + s * 0.06f, tubeTop + tubeH * 0.78f), Size(tubeW - s * 0.12f, s * 0.025f), CornerRadius(s * 0.0125f))
        }
        Text(
            "签",
            style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = (size.value * 0.22f).sp, color = hex(0xFFD166)),
            modifier = Modifier.padding(bottom = size * 0.17f),
        )
    }
}

// endregion

// region 塔罗

/** 塔罗牌背面 */
@Composable
fun TarotCardBack(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val gold = hex(0xF5D78E)
        drawRoundRect(Brush.verticalGradient(listOf(hex(0x1E1B4B), hex(0x0B0720))), cornerRadius = CornerRadius(w * 0.09f))
        val c = center
        for (i in 1..3) drawCircle(gold.copy(alpha = 0.55f), w * 0.12f * i, c, style = Stroke(0.8.dp.toPx()))
        translate(c.x, c.y) { drawPath(starPath(w * 0.18f, points = 8, innerRatio = 0.35f), gold.copy(alpha = 0.85f)) }
        for (i in 0 until 12) {
            val a = i / 12.0 * 2 * Math.PI
            drawLine(
                gold.copy(alpha = 0.6f),
                Offset(c.x + (cos(a) * w * 0.38).toFloat(), c.y + (sin(a) * w * 0.38).toFloat()),
                Offset(c.x + (cos(a) * w * 0.44).toFloat(), c.y + (sin(a) * w * 0.44).toFloat()),
                0.8.dp.toPx(),
            )
        }
        for (dy in listOf(-1f, 1f)) drawCircle(gold.copy(alpha = 0.7f), w * 0.07f, Offset(c.x, c.y + dy * h * 0.34f))
        val inset = w * 0.05f
        drawRoundRect(
            gold.copy(alpha = 0.8f), Offset(inset, inset), Size(w - inset * 2, h - inset * 2),
            CornerRadius(w * 0.07f), style = Stroke(maxOf(0.8.dp.toPx(), w * 0.012f)),
        )
    }
}

/** 三张叠在一起的牌背 */
@Composable
fun TarotBackMini(scale: Float = 1f) {
    Box(Modifier.size((110 * scale).dp, (90 * scale).dp), contentAlignment = Alignment.BottomCenter) {
        for (i in 0 until 3) {
            TarotCardBack(
                Modifier
                    .size((46 * scale).dp, (74 * scale).dp)
                    .graphicsLayer {
                        rotationZ = (i - 1) * 14f
                        translationX = (i - 1) * 14f * scale * density
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    },
            )
        }
    }
}

// endregion

/** 边框圆角卡片（非玻璃） */
fun Modifier.softTile(color: Color, radius: Dp = 16.dp, stroke: Color? = null): Modifier {
    val shape = RoundedCornerShape(radius)
    val base = this.clip(shape).background(color)
    return if (stroke != null) base.border(1.dp, stroke, shape) else base
}

@Composable
fun FullWidthCenter(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
}
