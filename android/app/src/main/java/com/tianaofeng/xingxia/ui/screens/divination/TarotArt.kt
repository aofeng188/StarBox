package com.tianaofeng.xingxia.ui.screens.divination

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.tianaofeng.xingxia.core.ArtKind
import com.tianaofeng.xingxia.core.ArtShape
import com.tianaofeng.xingxia.core.SeededGenerator
import com.tianaofeng.xingxia.core.TarotCard
import com.tianaofeng.xingxia.ui.theme.AppTheme
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import com.tianaofeng.xingxia.core.TarotArt as TarotArtLibrary

// 塔罗牌面：tarot-art.json 的星空线稿 + 金框、牌号、牌名。画法与 iOS 的 TarotArt.swift 一一对应

/** 牌框、牌号的金色（和牌背一致） */
internal val TAROT_GOLD = hex(0xF5D78E)

/** 牌面是深色的「物件」：浅色模式下也用同一主题的深色配色来画 */
internal val ThemePalette.tarotCardPalette: ThemePalette
    get() = if (isDark) this else AppTheme.ALL.firstOrNull { it.light == this }?.dark ?: AppTheme.NEBULA.dark

/** 线稿的六个语义色，按当前主题解析 */
internal class TarotArtColors(palette: ThemePalette) {
    val base = palette.tarotCardPalette
    val accent = base.accent
    val accent2 = base.accentSecondary
    val gold = TAROT_GOLD
    val ink = lerp(base.star, base.accent, 0.12f)
    val star = lerp(base.star, TAROT_GOLD, 0.3f)
    val soft = lerp(base.accent, base.backgroundBottom, 0.45f)

    fun color(name: String?): Color? = when (name) {
        "accent" -> accent
        "accent2" -> accent2
        "gold" -> gold
        "ink" -> ink
        "star" -> star
        "soft" -> soft
        else -> null
    }
}

/** 牌面底色：主题的深空色；小阿卡纳再透出一点花色（花色身份色先向强调色靠） */
internal fun TarotCard.faceColors(palette: ThemePalette): List<Color> {
    val p = palette.tarotCardPalette
    var top = lerp(p.backgroundBottom, p.accent, 0.12f)
    var bottom = lerp(p.backgroundTop, Color.Black, 0.2f)
    val suit = tarotSuit
    if (suit != null) {
        val tint = p.identity(suit.colors)
        top = lerp(top, tint[0], 0.3f)
        bottom = lerp(bottom, tint[1], 0.35f)
    }
    return listOf(top, bottom)
}

/** 牌面坐标（100 × 160，y 向下）到画布像素的变换：等比缩放 + 旋转 + 平移（花色符号的模板在里面再套一层） */
internal class ArtTransform(private val originX: Double, private val originY: Double, val scale: Double, private val rotation: Double = 0.0) {
    private val cosR = cos(Math.toRadians(rotation))
    private val sinR = sin(Math.toRadians(rotation))

    private fun mapX(x: Double, y: Double) = originX + scale * (cosR * x - sinR * y)
    private fun mapY(x: Double, y: Double) = originY + scale * (sinR * x + cosR * y)

    fun map(x: Double, y: Double): Offset = Offset(mapX(x, y).toFloat(), mapY(x, y).toFloat())

    /** 以 (x, y) 为中心、放大 r 倍、再转 rot 度的子坐标系 */
    fun child(x: Double, y: Double, r: Double, rot: Double) = ArtTransform(mapX(x, y), mapY(x, y), scale * r, rotation + rot)

    companion object {
        /** 把 100 × 160 的牌面等比放进 size，居中 */
        fun fitting(size: Size): ArtTransform {
            val scale = min(size.width / TarotArtLibrary.WIDTH, size.height / TarotArtLibrary.HEIGHT)
            return ArtTransform((size.width - TarotArtLibrary.WIDTH * scale) / 2, (size.height - TarotArtLibrary.HEIGHT * scale) / 2, scale)
        }
    }
}

/**
 * 画一组图元。unit 是一个牌面单位对应的像素，线宽按它换算（花色模板里的线宽也按牌面单位，不随符号大小变粗）；
 * 没写颜色的图元用 inherited（花色模板继承 suit 图元的颜色），再没有就用 ink
 */
internal fun DrawScope.drawTarotShapes(
    shapes: List<ArtShape>,
    transform: ArtTransform,
    unit: Double,
    colors: TarotArtColors,
    measurer: TextMeasurer,
    inherited: Color? = null,
    opacity: Double = 1.0,
    widthScale: Double = 1.0,
) {
    for (shape in shapes) {
        val kind = shape.kind ?: continue
        val color = colors.color(shape.c) ?: inherited ?: colors.ink
        val alpha = shape.o * opacity
        when (kind) {
            ArtKind.SUIT -> {
                val template = shape.s?.let { TarotArtLibrary.suits[it] } ?: continue
                if (shape.r <= 0) continue
                drawTarotShapes(
                    template, transform.child(shape.x, shape.y, shape.r, shape.rot ?: 0.0), unit, colors, measurer,
                    inherited = colors.color(shape.c) ?: inherited, opacity = alpha, widthScale = widthScale * shape.strokeWidth,
                )
            }
            ArtKind.GLYPH -> {
                val text = shape.g
                val fontPx = (shape.r * transform.scale).toFloat()
                if (text.isNullOrEmpty() || fontPx <= 0f) continue
                val style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = fontPx.toSp(), color = color.copy(alpha = alpha.toFloat()))
                drawCenteredText(measurer, text, style, transform.map(shape.x, shape.y), Float.MAX_VALUE)
            }
            ArtKind.CIRCLE, ArtKind.RING -> {
                val center = transform.map(shape.x, shape.y)
                val radius = (shape.r * transform.scale).toFloat()
                if (radius <= 0f) continue
                val fill = shape.fillAlpha
                if (fill > 0) drawCircle(color.copy(alpha = (alpha * fill).toFloat()), radius, center)
                val width = shape.strokeWidth * widthScale * unit
                if (width > 0) drawCircle(color.copy(alpha = alpha.toFloat()), radius, center, style = artStroke(width))
            }
            ArtKind.STAR, ArtKind.CRESCENT, ArtKind.LINE, ArtKind.POLYLINE, ArtKind.POLYGON, ArtKind.ARC -> {
                val points = shape.outline()
                if (points.size < 2) continue
                val path = Path()
                points.forEachIndexed { i, (x, y) ->
                    val point = transform.map(x, y)
                    if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                if (shape.isClosed) path.close()
                val fill = shape.fillAlpha
                if (shape.isClosed && fill > 0) drawPath(path, color.copy(alpha = (alpha * fill).toFloat()))
                val width = shape.strokeWidth * widthScale * unit
                if (width > 0) drawPath(path, color.copy(alpha = alpha.toFloat()), style = artStroke(width))
            }
        }
    }
}

/** 圆头圆角的描边；太细的线在小牌上会断续，至少画 0.35dp */
private fun DrawScope.artStroke(width: Double) =
    Stroke(width = max(width.toFloat(), 0.35f * density), cap = StrokeCap.Round, join = StrokeJoin.Round)

/** 以 at 为中心画一行字，宽过 maxWidth 时整体缩小 */
private fun DrawScope.drawCenteredText(measurer: TextMeasurer, text: String, style: TextStyle, at: Offset, maxWidth: Float) {
    val layout = measurer.measure(text, style, softWrap = false, maxLines = 1)
    val width = layout.size.width.toFloat()
    val height = layout.size.height.toFloat()
    if (width <= 0f) return
    val factor = if (width > maxWidth) maxWidth / width else 1f
    scale(factor, factor, pivot = at) {
        drawText(layout, topLeft = Offset(at.x - width / 2, at.y - height / 2))
    }
}

/**
 * 一张牌的星空线稿（不含底色、外框和牌名），按 100 × 160 等比放进自己的尺寸里。
 * 纯静态，读屏跳过（牌名由外层读）
 */
@Composable
fun TarotArt(cardId: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val colors = remember(palette) { TarotArtColors(palette) }
    val shapes = remember(cardId) { TarotArtLibrary.load()[cardId].orEmpty() }
    val measurer = rememberTextMeasurer()
    Canvas(modifier.clearAndSetSemantics { }) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val transform = ArtTransform.fitting(size)
        drawTarotShapes(shapes, transform, transform.scale, colors, measurer)
    }
}

/**
 * 牌面的底和框：主题深空底色、柔光、星尘、双层金框、牌名上方的饰线、牌号（小阿卡纳两边各一个花色小符号）、牌名和英文名。
 * 线稿由 [TarotArt] 叠在上面。字和线都按牌宽缩放，不随系统字号变化
 */
internal fun DrawScope.drawTarotCardFrame(card: TarotCard, background: List<Color>, colors: TarotArtColors, measurer: TextMeasurer) {
    if (size.width <= 0f || size.height <= 0f) return
    val t = ArtTransform.fitting(size)
    val u = t.scale.toFloat()
    val corner = CornerRadius(size.width * 0.09f)
    drawRoundRect(Brush.verticalGradient(background), cornerRadius = corner)
    // 线稿后面的一团柔光
    drawRoundRect(
        Brush.radialGradient(listOf(colors.accent.copy(alpha = 0.2f), colors.accent.copy(alpha = 0f)), center = t.map(50.0, 72.0), radius = 52 * u),
        cornerRadius = corner,
    )
    // 星尘（和 iOS 同一个种子、同样的取数顺序）
    val rng = SeededGenerator("tarot-face", card.id)
    repeat(16) {
        val point = t.map(rng.unit() * 100, rng.unit() * 160)
        val r = rng.double(0.3, 0.8).toFloat() * u
        drawCircle(colors.star.copy(alpha = rng.double(0.15, 0.45).toFloat()), r, point)
    }
    // 金框
    drawRoundRect(colors.gold, t.map(5.0, 5.0), Size(90 * u, 150 * u), CornerRadius(7 * u), style = Stroke(max(1.2f * u, 0.8f * density)))
    drawRoundRect(colors.gold.copy(alpha = 0.4f), t.map(7.4, 7.4), Size(85.2f * u, 145.2f * u), CornerRadius(5.2f * u), style = Stroke(max(0.4f * u, 0.35f * density)))
    // 牌名上方的分隔饰线
    for ((x0, x1) in listOf(26.0 to 45.0, 55.0 to 74.0)) {
        drawLine(colors.gold.copy(alpha = 0.5f), t.map(x0, 126.5), t.map(x1, 126.5), strokeWidth = max(0.5f * u, 0.35f * density))
    }
    val sparkle = Path()
    TarotArtLibrary.starPoints(50.0, 126.5, 1.8, 4, 0.3, -90.0).forEachIndexed { i, (x, y) ->
        val point = t.map(x, y)
        if (i == 0) sparkle.moveTo(point.x, point.y) else sparkle.lineTo(point.x, point.y)
    }
    sparkle.close()
    drawPath(sparkle, colors.gold)

    // 牌号：大阿卡纳写罗马数字，小阿卡纳写 A、2…10、侍从…，两边各一个花色小符号
    val suit = card.tarotSuit
    if (suit != null) {
        drawCenteredText(
            measurer, card.rankText,
            TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (9 * u).toSp(), color = colors.gold),
            t.map(50.0, 16.0), 40 * u,
        )
        val marks = listOf(19.0, 81.0).map { ArtShape(t = "suit", c = "gold", x = it, y = 16.0, r = 4.2, s = suit.key) }
        drawTarotShapes(marks, t, t.scale, colors, measurer)
    } else {
        drawCenteredText(
            measurer, card.roman,
            TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = (10 * u).toSp(), color = colors.gold),
            t.map(50.0, 16.0), 60 * u,
        )
    }
    drawCenteredText(
        measurer, card.name,
        TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (12.5f * u).toSp(), color = colors.ink),
        t.map(50.0, 136.5), 80 * u,
    )
    drawCenteredText(
        measurer, card.en.uppercase(),
        TextStyle(
            fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, fontSize = (5 * u).toSp(),
            letterSpacing = (0.6f * u).toSp(), color = colors.gold.copy(alpha = 0.85f),
        ),
        t.map(50.0, 146.5), 82 * u,
    )
}
