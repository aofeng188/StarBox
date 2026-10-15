package com.tianaofeng.xingxia.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.tianaofeng.xingxia.core.SeededGenerator
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * 动态星空背景：缓慢流动的星云光斑 + 闪烁星点 + 偶尔划过的流星。
 * 浅色模式下变成柔和的晨曦光斑。关闭「背景动画」或系统省电模式下保持静止；
 * 只在页面可见（STARTED）时以 15fps 刷新，退到后台就停。
 */
@Composable
fun CosmicBackground(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val powerSave = rememberPowerSaveMode()
    val animate = AppSettings.backgroundAnimation && !powerSave
    val time by produceState(0.0, animate, lifecycle) {
        if (!animate) {
            value = 0.0
            return@produceState
        }
        val start = System.nanoTime()
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                // 系统关闭了动画（对应 iOS 的「减弱动态效果」）时停在当前画面，隔一秒再看设置有没有变
                val still = isReduceMotion()
                if (!still) value = (System.nanoTime() - start) / 1e9 + 1.0
                delay(if (still) 1_000L else 66L)
            }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(palette.backgroundTop, palette.backgroundBottom)))
        drawNebula(time, palette)
        if (palette.isDark) StarField.draw(this, time, palette) else StarField.drawDaylight(this, time, palette)
    }
}

private fun DrawScope.drawNebula(t: Double, palette: ThemePalette) {
    val w = size.width
    val h = size.height
    val strength = if (palette.isDark) 0.5f else 0.75f
    val blobs = listOf(
        Triple(Offset((0.5 + 0.08 * sin(t * 0.11)).toFloat() * w, 0.05f * h), palette.nebula[0], 0.55f),
        Triple(Offset(0.02f * w, (0.45 + 0.1 * sin(t * 0.17)).toFloat() * h), palette.nebula[1], 0.45f),
        Triple(Offset(0.98f * w, (0.55 + 0.1 * cos(t * 0.15)).toFloat() * h), palette.nebula[2], 0.5f),
        Triple(Offset((0.5 + 0.1 * cos(t * 0.12)).toFloat() * w, 0.98f * h), palette.nebula[3], 0.4f),
        Triple(Offset((0.5 + 0.18 * sin(t * 0.13)).toFloat() * w, (0.5 + 0.14 * cos(t * 0.09)).toFloat() * h), palette.nebula[0], 0.18f),
    )
    val radius = max(w, h) * 0.6f
    for ((center, color, alpha) in blobs) {
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = alpha * strength), Color.Transparent), center, radius),
            radius,
            center,
        )
    }
}

private object StarField {
    class Star(val x: Double, val y: Double, val radius: Double, val speed: Double, val phase: Double, val tint: Int, val sparkle: Boolean)

    val stars: List<Star> = run {
        val rng = SeededGenerator(20_261_009uL)
        (0 until 150).map {
            val big = rng.chance(0.08)
            Star(
                x = rng.unit(),
                y = rng.unit(),
                radius = if (big) rng.double(1.4, 2.4) else rng.double(0.4, 1.2),
                speed = rng.double(0.6, 2.4),
                phase = rng.double(0.0, 2 * Math.PI),
                tint = if (rng.chance(0.25)) rng.intBelow(4) else -1,
                sparkle = big && rng.chance(0.6),
            )
        }
    }

    fun draw(scope: DrawScope, time: Double, palette: ThemePalette) = with(scope) {
        val scale = density
        for (star in stars) {
            val twinkle = (0.55 + 0.45 * sin(time * star.speed + star.phase)).toFloat()
            val point = Offset((star.x * size.width).toFloat(), (star.y * size.height).toFloat())
            val color = if (star.tint >= 0) palette.nebula[star.tint] else palette.star
            val r = (star.radius * scale).toFloat()
            if (star.radius > 1.3) {
                drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.35f * twinkle), Color.Transparent), point, r * 4), r * 4, point)
            }
            drawCircle(color.copy(alpha = 0.35f + 0.65f * twinkle), r, point)
            if (star.sparkle) {
                val len = r * 5 * twinkle
                val c = color.copy(alpha = 0.5f * twinkle)
                drawLine(c, Offset(point.x - len, point.y), Offset(point.x + len, point.y), 0.6f * scale)
                drawLine(c, Offset(point.x, point.y - len), Offset(point.x, point.y + len), 0.6f * scale)
            }
        }
        drawShootingStar(this, time, palette)
    }

    private fun drawShootingStar(scope: DrawScope, time: Double, palette: ThemePalette) = with(scope) {
        val period = 11.0
        val cycle = (time / period).toInt()
        val local = time - cycle * period
        val duration = 1.1
        if (local >= duration || time <= 1.0) return@with
        val rng = SeededGenerator("meteor", cycle)
        val start = Offset((size.width * rng.double(0.3, 1.1)).toFloat(), (size.height * rng.double(-0.05, 0.35)).toFloat())
        val angle = rng.double(2.4, 2.75)
        val distance = size.width * 0.75
        val p = local / duration
        val head = Offset((start.x + cos(angle) * distance * p).toFloat(), (start.y + sin(angle) * distance * p).toFloat())
        val tailLength = 120.0 * density * sin(Math.PI * p)
        val tail = Offset((head.x - cos(angle) * tailLength).toFloat(), (head.y - sin(angle) * tailLength).toFloat())
        val alpha = sin(Math.PI * p).toFloat()
        drawLine(
            Brush.linearGradient(listOf(Color.Transparent, palette.star.copy(alpha = 0.9f * alpha)), tail, head),
            tail, head, 1.6f * density, StrokeCap.Round,
        )
        drawCircle(palette.star.copy(alpha = alpha), 2f * density, head)
    }

    /** 浅色模式：漂浮的柔光光斑 + 少量细碎闪光 */
    fun drawDaylight(scope: DrawScope, time: Double, palette: ThemePalette) = with(scope) {
        stars.take(18).forEachIndexed { i, star ->
            val drift = sin(time * 0.05 * star.speed + star.phase)
            val point = Offset(
                ((star.x + 0.04 * drift) * size.width).toFloat(),
                ((star.y + 0.03 * cos(time * 0.04 * star.speed + star.phase)) * size.height).toFloat(),
            )
            val r = ((40 + star.radius * 40) * density).toFloat()
            val color = palette.nebula[i % palette.nebula.size]
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0f)), point, r), r, point)
        }
        stars.drop(18).take(50).forEach { star ->
            val twinkle = max(0.0, sin(time * star.speed + star.phase)).toFloat()
            val point = Offset((star.x * size.width).toFloat(), (star.y * size.height).toFloat())
            drawCircle(palette.star.copy(alpha = 0.15f + 0.45f * twinkle), (star.radius * 0.9 * density).toFloat(), point)
        }
    }
}
