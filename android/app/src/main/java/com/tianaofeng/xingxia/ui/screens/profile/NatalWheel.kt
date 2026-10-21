package com.tianaofeng.xingxia.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.Astronomy
import com.tianaofeng.xingxia.core.BirthInfo
import com.tianaofeng.xingxia.core.TarotArt
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 出生时刻的太阳、月亮黄经和上升点（等宫制，从上升点起第一宫）。没有出生时间时不算上升、不分宫。
 * 和命盘页的三巨头用同一套算法，星座一定对得上；与 iOS 的 NatalWheel.swift 一致
 */
internal class NatalChart(birth: BirthInfo) {
    val sun: Double = Astronomy.normalize(Astronomy.sunLongitude(birth.moment))
    val moon: Double = Astronomy.normalize(Astronomy.moonLongitude(birth.moment))
    val ascendant: Double? = if (birth.hasTime) {
        val city = birth.city
        Astronomy.normalize(Astronomy.ascendant(birth.moment, city.latitude, city.longitude))
    } else {
        null
    }

    /** 读屏读的一句话 */
    val spokenSummary: String
        get() {
            val parts = mutableListOf("太阳在${position(sun)}", "月亮在${position(moon)}")
            parts += ascendant?.let { "上升在${position(it)}" } ?: "没有出生时间，未画上升和宫位"
            return parts.joinToString("，")
        }

    companion object {
        /** 星座内的度数（向下取整） */
        fun degree(longitude: Double): Int = (Astronomy.normalize(longitude) % 30).toInt()

        /** 「双子座 27°」 */
        fun position(longitude: Double): String = "${ZodiacSign.fromLongitude(longitude).label} ${degree(longitude)}°"
    }
}

private class WheelColors(val line: Color, val glyph: Color, val sun: Color, val moon: Color, val text: Color)

/**
 * 十二宫星盘：外圈十二星座（符号 + 名字），内圈从上升点起按等宫制画十二宫，标出太阳、月亮和上升。
 * 上升点放在左边（九点钟方向），黄道逆时针排列；没有出生时间时白羊座 0° 放在左边，只画太阳和月亮
 */
@Composable
fun NatalWheel(birth: BirthInfo, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val chart = remember(birth) { NatalChart(birth) }
    val colors = WheelColors(
        line = palette.accent,
        glyph = palette.ink(palette.accent),
        sun = palette.gold,
        moon = palette.ink(palette.accentSecondary),
        text = palette.text,
    )
    val measurer = rememberTextMeasurer()
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clearAndSetSemantics {
                    contentDescription = chart.spokenSummary
                    role = Role.Image
                },
        ) { drawNatalWheel(chart, colors, measurer) }

        // 图例：跟随系统字号；读屏已经从星盘读到同样的内容
        Flow(spacing = 12.dp, modifier = Modifier.clearAndSetSemantics { }, horizontalAlignment = Alignment.CenterHorizontally) {
            LegendItem(Icons.Rounded.WbSunny, colors.sun, "太阳 ${NatalChart.position(chart.sun)}")
            LegendItem(Icons.Rounded.Brightness3, colors.moon, "月亮 ${NatalChart.position(chart.moon)}")
            chart.ascendant?.let { LegendItem(Icons.Rounded.ArrowUpward, colors.glyph, "上升 ${NatalChart.position(it)}") }
        }
        if (chart.ascendant == null) {
            SecondaryText("没有出生时间，未画上升和宫位", style = Fonts.caption, modifier = Modifier.clearAndSetSemantics { })
        }
    }
}

@Composable
private fun LegendItem(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, null, Modifier.size(14.dp), tint = tint)
        Text(text, style = Fonts.caption, color = LocalPalette.current.text)
    }
}

// 绘制：几何尺寸按星盘大小缩放，不随系统字号变化（与 iOS 的 NatalWheel.draw 一致）
private fun DrawScope.drawNatalWheel(chart: NatalChart, colors: WheelColors, measurer: TextMeasurer) {
    val side = min(size.width, size.height)
    if (side <= 0f) return
    val cx = size.width / 2
    val cy = size.height / 2
    val radius = side / 2 - 1.5f * density
    val reference = chart.ascendant ?: 0.0
    val signOuter = radius
    val signInner = radius * 0.76f
    val houseInner = radius * 0.64f
    val core = radius * 0.22f
    val hairline = density

    /** 黄经 → 画布上的点：参考点（上升或白羊 0°）在左边，逆时针增加 */
    fun point(longitude: Double, r: Float): Offset {
        val angle = Math.toRadians(180 + longitude - reference)
        return Offset(cx + r * cos(angle).toFloat(), cy - r * sin(angle).toFloat())
    }

    fun segment(from: Double, to: Double, inner: Float, outer: Float): Path {
        val steps = 12
        val points = (0..steps).map { point(from + (to - from) * it / steps, outer) } +
            (0..steps).map { point(to - (to - from) * it / steps, inner) }
        return Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
            close()
        }
    }

    fun line(longitude: Double, from: Float, to: Float, color: Color, width: Float) {
        drawLine(color, point(longitude, from), point(longitude, to), strokeWidth = width * hairline)
    }

    fun text(string: String, sizePx: Float, color: Color, at: Offset, weight: FontWeight = FontWeight.Normal) {
        val layout = measurer.measure(string, TextStyle(fontSize = sizePx.toSp(), fontWeight = weight, color = color), softWrap = false, maxLines = 1)
        drawText(layout, topLeft = Offset(at.x - layout.size.width / 2f, at.y - layout.size.height / 2f))
    }

    val center = Offset(cx, cy)
    // 星座带：相邻星座深浅交替
    for (i in 0 until 12) {
        val start = i * 30.0
        drawPath(segment(start, start + 30, signInner, signOuter), colors.line.copy(alpha = if (i % 2 == 0) 0.12f else 0.05f))
    }
    drawCircle(colors.line.copy(alpha = 0.06f), core, center)
    drawCircle(colors.line.copy(alpha = 0.75f), signOuter, center, style = Stroke(1.2f * hairline))
    drawCircle(colors.line.copy(alpha = 0.55f), signInner, center, style = Stroke(0.8f * hairline))
    drawCircle(colors.line.copy(alpha = 0.3f), houseInner, center, style = Stroke(0.6f * hairline))
    drawCircle(colors.line.copy(alpha = 0.3f), core, center, style = Stroke(0.6f * hairline))
    // 星座分界和每 5° 的刻度
    for (i in 0 until 72) {
        val longitude = i * 5.0
        if (i % 6 == 0) {
            line(longitude, signInner, signOuter, colors.line.copy(alpha = 0.55f), 0.8f)
        } else {
            val tick = if (i % 2 == 0) radius * 0.035f else radius * 0.02f
            line(longitude, signInner, signInner + tick, colors.line.copy(alpha = 0.4f), 0.5f)
        }
    }
    // 星座符号和名字（字保持正立）
    for (sign in ZodiacSign.entries) {
        val middle = sign.ordinal * 30.0 + 15
        text(sign.glyph, radius * 0.093f, colors.glyph, point(middle, radius * 0.93f), FontWeight.Medium)
        text(sign.shortName, radius * 0.054f, colors.text.copy(alpha = 0.7f), point(middle, radius * 0.825f))
    }

    // 十二宫：等宫制，第一宫从上升点起，逆时针
    val ascendant = chart.ascendant
    if (ascendant != null) {
        for (house in 0 until 12) {
            val cusp = ascendant + house * 30
            if (house != 0 && house != 6) line(cusp, core, signInner, colors.line.copy(alpha = 0.4f), 0.6f)
            text("${house + 1}", radius * 0.05f, colors.text.copy(alpha = 0.55f), point(cusp + 15, radius * 0.70f))
        }
        // 上升—下降轴；ASC 标在轴的上方，放在太阳、月亮符号够不到的半径上
        drawLine(colors.glyph, point(ascendant, signInner), point(ascendant + 180, signInner), strokeWidth = 1.4f * hairline)
        text("ASC", radius * 0.056f, colors.glyph, point(ascendant - 7, radius * 0.6f), FontWeight.Bold)
    }

    // 太阳、月亮：内圈边上一道刻线标准确位置，符号放在里面；两者靠得太近时符号往两边错开，用虚线连回刻线
    val (sunAngle, moonAngle) = spreadApart(chart.sun, chart.moon, minimum = 20.0)
    val bodyRadius = radius * 0.47f
    val mark = radius * 0.055f
    val dash = PathEffect.dashPathEffect(floatArrayOf(2 * hairline, 2 * hairline))
    for ((longitude, angle, color) in listOf(Triple(chart.sun, sunAngle, colors.sun), Triple(chart.moon, moonAngle, colors.moon))) {
        line(longitude, signInner - radius * 0.06f, signInner, color, 1.6f)
        drawLine(
            color.copy(alpha = 0.45f), point(angle, bodyRadius + mark * 1.5f), point(longitude, signInner - radius * 0.06f),
            strokeWidth = 0.6f * hairline, pathEffect = dash,
        )
        drawCircle(color.copy(alpha = 0.14f), mark * 1.8f, point(angle, bodyRadius))
        text("${NatalChart.degree(longitude)}°", radius * 0.05f, colors.text.copy(alpha = 0.7f), point(angle, bodyRadius - mark * 2.4f))
    }
    // 太阳：圆圈加中心点
    val sunCenter = point(sunAngle, bodyRadius)
    drawCircle(colors.sun, mark, sunCenter, style = Stroke(1.4f * hairline))
    drawCircle(colors.sun, mark * 0.28f, sunCenter)
    // 月亮：亮面朝左的月牙
    val moonCenter = point(moonAngle, bodyRadius)
    val moon = Path()
    TarotArt.crescentPoints(moonCenter.x.toDouble(), moonCenter.y.toDouble(), mark * 1.1, 180.0, 0.36).forEachIndexed { i, (x, y) ->
        if (i == 0) moon.moveTo(x.toFloat(), y.toFloat()) else moon.lineTo(x.toFloat(), y.toFloat())
    }
    moon.close()
    drawPath(moon, colors.moon)
}

/** 两个黄经在盘上至少隔开 minimum 度：太近时以两者中点为中心各往两边让开，返回画符号用的角度（与 iOS 同一算法） */
private fun spreadApart(a: Double, b: Double, minimum: Double): Pair<Double, Double> {
    val d = Astronomy.normalize(b - a + 180) - 180
    if (abs(d) >= minimum) return a to b
    val middle = a + d / 2
    val half = (if (d < 0) -minimum else minimum) / 2
    return (middle - half) to (middle + half)
}
