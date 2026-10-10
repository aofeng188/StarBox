package com.tianaofeng.xingxia.ui.screens.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.TrueRandom
import com.tianaofeng.xingxia.data.CustomPrompt
import com.tianaofeng.xingxia.data.Prefs
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.Confetti
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientText
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.SoundEffect
import com.tianaofeng.xingxia.ui.components.SoundEngine
import com.tianaofeng.xingxia.ui.components.OnShake
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.StepperRow
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

// region 抛硬币

@Composable
fun CoinScreen() {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var heads by Prefs.int("coin.heads", 0)
    var tails by Prefs.int("coin.tails", 0)
    val angle = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    var flipping by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    var streak by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<Boolean?>(null) }
    var question by remember { mutableStateOf("") }

    fun flip() {
        if (flipping) return
        val isHeads = TrueRandom.bool()
        flipping = true
        result = null
        Haptics.impact()
        SoundEngine.play(SoundEffect.WHOOSH, 0.6f)
        SoundEngine.play(SoundEffect.COIN_FLIP, 0.7f)
        val spins = TrueRandom.int(5..7) * 360f
        val current = angle.value % 360f
        val target = angle.value - current + spins + if (isHeads) 0f else 180f
        scope.launch { angle.animateTo(target, tween(1150, easing = CubicBezierEasing(0.15f, 0.6f, 0.35f, 1f))) }
        scope.launch {
            lift.animateTo(-280f, tween(550, easing = FastOutSlowInEasing))
            lift.animateTo(0f, tween(600, easing = CubicBezierEasing(0.5f, 0f, 1f, 1f)))
            SoundEngine.play(SoundEffect.COIN_LAND)
            Haptics.heavy()
            lift.animateTo(-24f, tween(120))
            lift.animateTo(0f, tween(150))
            delay(100)
            result = isHeads
            flipping = false
            if (isHeads) heads++ else tails++
            streak = if (last == isHeads) streak + 1 else 1
            last = isHeads
            Store.log(ToolKind.COIN.id, question, if (isHeads) "正面" else "反面")
        }
    }

    OnShake { flip() }

    Screen(title = "抛硬币", horizontalAlignment = Alignment.CenterHorizontally) {
        GlassTextField(question, "想决定什么？（可选）") { question = it }
        Box(
            Modifier
                .fillMaxWidth()
                .height(340.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // 落地阴影
            val shadow = 1f + lift.value / 400f
            Box(
                Modifier
                    .padding(bottom = 20.dp)
                    .size(150.dp, 26.dp)
                    .graphicsLayer { scaleX = shadow; scaleY = shadow; alpha = shadow }
                    .drawBehind { drawOval(Color.Black.copy(alpha = if (palette.isDark) 0.45f else 0.18f)) },
            )
            CoinView(
                angle.value,
                Modifier
                    .padding(bottom = 40.dp)
                    .size(190.dp)
                    .graphicsLayer {
                        translationY = lift.value * density
                        val s = 1f - lift.value / 280f * 0.25f
                        scaleX = s
                        scaleY = s
                    }
                    .pressable { flip() },
            )
        }
        Box(Modifier.height(90.dp), contentAlignment = Alignment.Center) {
            val r = result
            if (r != null && !flipping) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    GradientText(if (r) "正面" else "反面", style = Fonts.display(44, FontWeight.Black))
                    if (question.isNotEmpty()) SecondaryText(if (r) "「$question」→ 做！" else "「$question」→ 算了吧")
                    if (streak > 1) Text("已经连续 $streak 次${if (r) "正面" else "反面"}了", style = Fonts.caption, color = palette.accent)
                }
            } else if (!flipping) {
                SecondaryText("点按硬币或摇一摇")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StatPill("正面", heads, heads + tails, hex(0xF5B83D), Modifier.weight(1f))
            StatPill("反面", tails, heads + tails, hex(0x94A3B8), Modifier.weight(1f))
            CircleIconButton(Icons.Rounded.Refresh, "清空统计", size = 44.dp) {
                heads = 0
                tails = 0
                streak = 0
            }
        }
        BigActionButton("抛硬币", Icons.Rounded.TouchApp, enabled = !flipping) { flip() }
    }
}

/** 绕水平轴翻转的硬币，自带厚度 */
@Composable
private fun CoinView(angle: Float, modifier: Modifier) {
    val radians = Math.toRadians(angle.toDouble())
    val c = cos(radians).toFloat()
    val front = c >= 0
    val squash = maxOf(0.02f, abs(c))
    val thickness = 12f * abs(sin(radians)).toFloat()
    Box(modifier, contentAlignment = Alignment.Center) {
        // 边缘
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleY = squash
                    translationY = (if (front) thickness / 2 else -thickness / 2) * density
                }
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(hex(0x8A6A12), hex(0xC9A227), hex(0x6B4F0E)))),
        )
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleY = squash
                    translationY = (if (front) -thickness / 2 else thickness / 2) * density
                },
        ) { CoinFace(front, 1f - 0.25f * (1 - abs(c))) }
    }
}

@Composable
private fun CoinFace(isFront: Boolean, brightness: Float) {
    val ink = hex(0x7A5A0E)
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(hex(0xFFF1B8), hex(0xF2C94C), hex(0xB8862E))))
            .drawBehind {
                drawCircle(hex(0x8A6A12, 0.6f), size.minDimension / 2 - 7.dp.toPx(), style = Stroke(6.dp.toPx()))
                drawCircle(
                    hex(0x8A6A12, 0.5f), size.minDimension / 2 - 16.dp.toPx(),
                    style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))),
                )
                if (brightness < 1f) drawCircle(Color.Black.copy(alpha = 1f - brightness))
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (isFront) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay, null, Modifier.size(54.dp), tint = ink)
            Text(if (isFront) "正" else "反", style = Fonts.serif(30, bold = true), color = ink)
        }
    }
}

@Composable
fun StatPill(title: String, value: Int, total: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .glass(RoundedCornerShape(16.dp), tint = color.copy(alpha = 0.15f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$value", style = Fonts.title3.weight(FontWeight.Bold))
        SecondaryText(if (total > 0) "$title ${value * 100 / total}%" else title, style = Fonts.caption2)
    }
}

// endregion

// region 骰子

@Composable
fun DiceScreen() {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var count by Prefs.int("dice.count", 2)
    val faces = remember { mutableStateListOf<Int>() }
    var rolling by remember { mutableStateOf(false) }
    var settled by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<List<Int>>() }
    val spin = remember { Animatable(0f) }

    LaunchedEffect(count) {
        faces.clear()
        repeat(count) { faces.add(it % 6 + 1) }
        settled = false
    }

    fun roll() {
        if (rolling) return
        rolling = true
        settled = false
        Haptics.impact()
        SoundEngine.play(SoundEffect.WHOOSH, 0.5f)
        val final = List(count) { TrueRandom.int(1..6) }
        scope.launch {
            launch { spin.snapTo(0f); spin.animateTo(720f, tween(900, easing = FastOutSlowInEasing)) }
            repeat(12) { step ->
                for (i in faces.indices) faces[i] = TrueRandom.int(1..6)
                SoundEngine.play(SoundEffect.DICE_HIT, 0.3f + step * 0.05f)
                Haptics.selection()
                delay(40L + step * 8)
            }
            for (i in faces.indices) faces[i] = final[i]
            rolling = false
            settled = true
            Haptics.success()
            history.add(final)
            if (history.size > 12) history.removeAt(0)
            val total = final.sum()
            Store.log(ToolKind.DICE.id, "${final.size} 颗", if (final.size > 1) "${final.joinToString("+")} = $total" else "$total 点")
        }
    }

    OnShake { roll() }

    Screen(title = "骰子", horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(50.dp), contentAlignment = Alignment.Center) {
            if (!settled || rolling) {
                SecondaryText(if (rolling) "骰子滚动中…" else "点按桌面或摇一摇掷骰子", style = Fonts.headline)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(faces.joinToString(" "), style = Fonts.title2)
                    if (faces.size > 1) GradientText("= ${faces.sum()}", style = Fonts.display(34))
                }
            }
        }
        // 桌面
        Box(
            Modifier
                .fillMaxWidth()
                .height(320.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            (if (palette.isDark) palette.backgroundBottom else palette.accent).copy(alpha = 0.9f),
                            (if (palette.isDark) palette.backgroundTop else palette.accentSecondary).copy(alpha = 0.9f),
                        ),
                    ),
                )
                .border(1.5.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(30.dp))
                .pressable { roll() },
            contentAlignment = Alignment.Center,
        ) {
            Flow(spacing = 22.dp, modifier = Modifier.padding(24.dp)) {
                faces.forEachIndexed { i, value ->
                    DieFace(
                        value,
                        Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                rotationZ = if (rolling) spin.value * (if (i % 2 == 0) 1f else -0.8f) else 0f
                                val bounce = if (rolling) sin(Math.toRadians(spin.value.toDouble() * 2 + i * 50)).toFloat() * 14f else 0f
                                translationY = bounce * density
                            },
                    )
                }
            }
        }
        GlassCard(padding = 14.dp) {
            StepperRow("骰子 $count 颗", count, 1..6) { if (!rolling) count = it }
        }
        BigActionButton("掷骰子", Icons.Rounded.Casino, enabled = !rolling) { roll() }
        if (history.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (values in history.reversed()) {
                    Text(
                        "${values.joinToString("+")}=${values.sum()}",
                        style = Fonts.caption,
                        modifier = Modifier
                            .glass(CircleShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** 传统中式骰子：一点和四点是红色 */
@Composable
fun DieFace(value: Int, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val side = size.minDimension
        drawRoundRect(Color(0xFFF7F7F7), cornerRadius = CornerRadius(side * 0.18f))
        drawRoundRect(Color.Black.copy(alpha = 0.08f), cornerRadius = CornerRadius(side * 0.18f), style = Stroke(1.dp.toPx()))
        val a = 0.24f
        val b = 0.5f
        val c = 0.76f
        val layout = listOf(
            listOf(b to b),
            listOf(a to a, c to c),
            listOf(a to a, b to b, c to c),
            listOf(a to a, c to a, a to c, c to c),
            listOf(a to a, c to a, b to b, a to c, c to c),
            listOf(a to a, a to b, a to c, c to a, c to b, c to c),
        )
        val red = value == 1 || value == 4
        val color = if (red) Color(0xFFDB1F29) else Color(0xFF1F2133)
        val radius = side * if (value == 1) 0.17f else 0.09f
        for ((x, y) in layout[(value - 1).coerceIn(0, 5)]) {
            val center = Offset(x * side, y * side)
            drawCircle(Color.Black.copy(alpha = 0.18f), radius, center.copy(y = center.y - 1.dp.toPx()))
            drawCircle(color, radius, center)
        }
    }
}

// endregion

// region 随机数字

private enum class NumberMode(val label: String) { CUSTOM("自定义"), DICE("1–6"), HUNDRED("1–100"), SSQ("双色球"), DLT("大乐透") }

private data class Ball(val value: Int, val special: Boolean)

@Composable
fun NumberScreen() {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var minValue by Prefs.int("number.min", 1)
    var maxValue by Prefs.int("number.max", 100)
    var count by Prefs.int("number.count", 1)
    var unique by Prefs.bool("number.unique", true)
    var sorted by Prefs.bool("number.sorted", false)
    var mode by remember { mutableStateOf(NumberMode.CUSTOM) }
    var balls by remember { mutableStateOf<List<Ball>>(emptyList()) }
    var rolling by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }
    var minText by remember { mutableStateOf(minValue.toString()) }
    var maxText by remember { mutableStateOf(maxValue.toString()) }

    fun pick(range: IntRange, n: Int, distinct: Boolean): List<Int> {
        val size = range.last - range.first + 1
        if (!distinct) return List(n) { TrueRandom.int(range) }
        val target = minOf(n, size)
        val seen = LinkedHashSet<Int>()
        while (seen.size < target) seen.add(TrueRandom.int(range))
        return seen.toList()
    }

    fun finalBalls(): List<Ball> = when (mode) {
        NumberMode.CUSTOM -> {
            val low = minOf(minValue, maxValue)
            val high = maxOf(minValue, maxValue)
            var values = pick(low..high, count, unique)
            if (sorted) values = values.sorted()
            values.map { Ball(it, false) }
        }
        NumberMode.DICE -> listOf(Ball(TrueRandom.int(1..6), false))
        NumberMode.HUNDRED -> listOf(Ball(TrueRandom.int(1..100), false))
        NumberMode.SSQ -> pick(1..33, 6, true).sorted().map { Ball(it, false) } + Ball(TrueRandom.int(1..16), true)
        NumberMode.DLT -> pick(1..35, 5, true).sorted().map { Ball(it, false) } + pick(1..12, 2, true).sorted().map { Ball(it, true) }
    }

    fun generate() {
        if (rolling) return
        rolling = true
        val final = finalBalls()
        scope.launch {
            val top = maxOf(9, final.maxOf { it.value })
            repeat(14) { step ->
                balls = final.map { Ball(TrueRandom.int(1..top), it.special) }
                SoundEngine.play(SoundEffect.TICK, 0.4f)
                if (step % 2 == 0) Haptics.selection()
                delay(45L + step * 6)
            }
            balls = final
            SoundEngine.play(SoundEffect.POP)
            Haptics.success()
            rolling = false
            val text = final.joinToString(" ") { if (it.special) "[%02d]".format(it.value) else "${it.value}" }
            history.add(text)
            Store.log(ToolKind.NUMBER.id, mode.label, text)
        }
    }

    OnShake { generate() }

    val caption = when (mode) {
        NumberMode.CUSTOM -> "${minOf(minValue, maxValue)} 到 ${maxOf(minValue, maxValue)} 之间 · $count 个${if (unique) " · 不重复" else ""}"
        NumberMode.DICE -> "掷一个六面骰"
        NumberMode.HUNDRED -> "1 到 100 之间"
        NumberMode.SSQ -> "红球 6 个（1–33）+ 蓝球 1 个（1–16）"
        NumberMode.DLT -> "前区 5 个（1–35）+ 后区 2 个（1–12）"
    }

    Screen(title = "随机数字") {
        SegmentedPicker(NumberMode.entries, mode, { it.label }) {
            mode = it
            balls = emptyList()
        }
        GlassCard(Modifier.heightIn(min = 200.dp), padding = 22.dp, horizontalAlignment = Alignment.CenterHorizontally, spacing = 16.dp) {
            when {
                balls.isEmpty() -> Text("?", style = Fonts.display(96, FontWeight.Black).copy(brush = palette.accentBrush), modifier = Modifier.graphicsLayer { alpha = 0.5f })
                balls.size == 1 -> GradientText("${balls[0].value}", style = Fonts.display(if (balls[0].value > 99999) 56 else 100, FontWeight.Black))
                else -> Flow(spacing = 10.dp) {
                    for (ball in balls) BallView(ball.value, ball.special, mode == NumberMode.SSQ || mode == NumberMode.DLT)
                }
            }
            SecondaryText(caption, style = Fonts.footnote, textAlign = TextAlign.Center)
        }
        if (mode == NumberMode.CUSTOM) {
            GlassCard(spacing = 14.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    NumberField("最小", minText, Modifier.weight(1f)) {
                        minText = it
                        it.toIntOrNull()?.let { v -> minValue = v }
                    }
                    Text("↔", style = Fonts.title3, color = palette.secondaryText)
                    NumberField("最大", maxText, Modifier.weight(1f)) {
                        maxText = it
                        it.toIntOrNull()?.let { v -> maxValue = v }
                    }
                }
                StepperRow("生成 $count 个", count, 1..30) { count = it }
                ToggleRow("不重复", unique) { unique = it }
                ToggleRow("结果排序", sorted) { sorted = it }
            }
        }
        BigActionButton("生成", Icons.Rounded.AutoAwesome, enabled = !rolling) { generate() }
        if (mode == NumberMode.SSQ || mode == NumberMode.DLT) SecondaryText("号码随机生成，仅供娱乐，请理性购彩。", style = Fonts.caption)
        if (history.isNotEmpty()) {
            GlassCard(spacing = 8.dp) {
                SectionHeader("历史", icon = Icons.Rounded.History)
                for (item in history.reversed()) SecondaryText(item)
            }
        }
    }
}

@Composable
private fun NumberField(title: String, value: String, modifier: Modifier, onChange: (String) -> Unit) {
    val palette = LocalPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SecondaryText(title, style = Fonts.caption)
        OutlinedTextField(
            value = value,
            onValueChange = { text -> onChange(text.filter { it.isDigit() || it == '-' }.take(9)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = Fonts.title2.copy(textAlign = TextAlign.Center, color = palette.text),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = palette.glassFill,
                unfocusedContainerColor = palette.glassFill,
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.glassStroke,
                cursorColor = palette.accent,
            ),
        )
    }
}

@Composable
private fun BallView(value: Int, special: Boolean, lottery: Boolean) {
    val palette = LocalPalette.current
    val colors = if (lottery) {
        if (special) listOf(hex(0x60A5FA), hex(0x1D4ED8)) else listOf(hex(0xF87171), hex(0xB91C1C))
    } else {
        listOf(palette.accent, palette.accentSecondary)
    }
    Box(
        Modifier
            .sizeIn(minWidth = 52.dp, minHeight = 52.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(colors))
            .padding(horizontal = if (value > 999) 8.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (lottery) "%02d".format(value) else "$value", style = Fonts.title3.weight(FontWeight.Bold), color = Color.White)
    }
}

// endregion

// region 石头剪刀布

private enum class Hand(val emoji: String, val label: String) {
    ROCK("✊", "石头"), SCISSORS("✌️", "剪刀"), PAPER("✋", "布");

    /** 能打败自己的手势 */
    val counter: Hand get() = entries[(ordinal + 2) % 3]

    fun outcome(other: Hand): Int = if (this == other) 0 else if (other.counter == this) 1 else -1
}

private enum class Match(val label: String, val target: Int?) { FREE("自由对战", null), BO3("三局两胜", 2), BO5("五局三胜", 3) }

@Composable
fun RpsScreen() {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var wins by Prefs.int("rps.wins", 0)
    var losses by Prefs.int("rps.losses", 0)
    var draws by Prefs.int("rps.draws", 0)
    var smart by Prefs.bool("rps.smart", false)
    var match by remember { mutableStateOf(Match.BO3) }
    var myScore by remember { mutableIntStateOf(0) }
    var aiScore by remember { mutableIntStateOf(0) }
    var myHand by remember { mutableStateOf<Hand?>(null) }
    var aiHand by remember { mutableStateOf<Hand?>(null) }
    var shuffleHand by remember { mutableStateOf(Hand.ROCK) }
    var playing by remember { mutableStateOf(false) }
    var chant by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }
    var resultColor by remember { mutableStateOf(Color.Unspecified) }
    var matchOver by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<Hand>() }
    /** 读心模式：记录「上一手 → 下一手」的频率 */
    val transitions = remember { mutableStateMapOf<Hand, List<Hand>>() }

    fun resetMatch() {
        myScore = 0
        aiScore = 0
        myHand = null
        aiHand = null
        resultText = ""
        matchOver = false
    }

    fun aiChoice(): Hand {
        val last = history.lastOrNull()
        val next = last?.let { transitions[it] }
        if (!smart || next == null || next.size < 2) return TrueRandom.pick(Hand.entries)
        // 预测你最可能出的手势，然后出能克制它的；保留 25% 的随机，避免被反向利用
        if (TrueRandom.double() < 0.25) return TrueRandom.pick(Hand.entries)
        val predicted = next.groupingBy { it }.eachCount().maxBy { it.value }.key
        return predicted.counter
    }

    fun settle(mine: Hand, ai: Hand) {
        history.lastOrNull()?.let { transitions[it] = (transitions[it] ?: emptyList()) + mine }
        history.add(mine)
        when (mine.outcome(ai)) {
            1 -> {
                myScore++; wins++
                resultText = "你赢了！"; resultColor = hex(0x34D399)
                SoundEngine.play(SoundEffect.SUCCESS)
                Haptics.success()
            }
            -1 -> {
                aiScore++; losses++
                resultText = "你输了"; resultColor = hex(0xFB7185)
                SoundEngine.play(SoundEffect.FAIL)
                Haptics.error()
            }
            else -> {
                draws++
                resultText = "平局"; resultColor = palette.secondaryText
                SoundEngine.play(SoundEffect.TAP)
                Haptics.tap()
            }
        }
        val target = match.target
        if (target != null && (myScore >= target || aiScore >= target)) {
            matchOver = true
            val won = myScore > aiScore
            resultText = if (won) "🏆 你赢下了比赛！" else "星匣赢下了比赛"
            resultColor = if (won) hex(0xFFD166) else hex(0xFB7185)
            if (won) confetti++
            Store.log(ToolKind.RPS.id, match.label, "$myScore : $aiScore ${if (won) "胜" else "负"}")
        }
    }

    fun play(hand: Hand) {
        if (playing) return
        playing = true
        myHand = hand
        aiHand = null
        val ai = aiChoice()
        scope.launch {
            for (word in listOf("石头", "剪刀", "布！")) {
                chant = word
                SoundEngine.play(SoundEffect.DRUM, 0.5f)
                Haptics.impact()
                repeat(3) {
                    shuffleHand = TrueRandom.pick(Hand.entries)
                    delay(85)
                }
            }
            aiHand = ai
            playing = false
            settle(hand, ai)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Screen(title = "石头剪刀布", horizontalAlignment = Alignment.CenterHorizontally) {
            SegmentedPicker(Match.entries, match, { it.label }) {
                match = it
                resetMatch()
            }
            GlassCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreColumn("你", myScore, Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SecondaryText("VS", style = Fonts.title3.weight(FontWeight.Bold))
                        match.target?.let { SecondaryText("先赢 $it 局", style = Fonts.caption2) }
                    }
                    ScoreColumn(if (smart) "星匣·读心" else "星匣", aiScore, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                HandBubble(myHand?.emoji ?: "🤔", false)
                HandBubble(if (playing) shuffleHand.emoji else aiHand?.emoji ?: "🤖", true)
            }
            Text(
                if (playing) chant else resultText,
                style = Fonts.display(30, FontWeight.Black),
                color = if (playing || resultColor == Color.Unspecified) palette.text else resultColor,
                modifier = Modifier.height(44.dp),
            )
            if (matchOver) {
                BigActionButton("再来一场", Icons.Rounded.Refresh) { resetMatch() }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    for (hand in Hand.entries) {
                        Column(
                            Modifier
                                .size(96.dp, 110.dp)
                                .pressable(!playing) { play(hand) }
                                .glass(RoundedCornerShape(26.dp)),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(hand.emoji, style = TextStyle(fontSize = 46.sp))
                            Text(hand.label, style = Fonts.caption.weight(FontWeight.SemiBold))
                        }
                    }
                }
            }
            GlassCard(padding = 14.dp) {
                ToggleRow("读心模式", smart, "星匣会根据你的出拳习惯来预测") { smart = it }
            }
            val total = wins + losses + draws
            SecondaryText("总战绩 $wins 胜 $losses 负 $draws 平 · 胜率 ${if (total == 0) "—" else "${wins * 100 / total}%"}", style = Fonts.caption)
        }
        Confetti(confetti)
    }
}

@Composable
private fun ScoreColumn(title: String, score: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$score", style = Fonts.display(40))
        SecondaryText(title, style = Fonts.caption)
    }
}

@Composable
private fun HandBubble(emoji: String, flipped: Boolean) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .size(140.dp)
            .glass(CircleShape, tint = palette.accent.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = TextStyle(fontSize = 80.sp), modifier = Modifier.graphicsLayer { scaleX = if (flipped) -1f else 1f })
    }
}

// endregion

// region 真心话大冒险

@Composable
fun TruthDareScreen() {
    val palette = LocalPalette.current
    var level by Prefs.int("td.level", 0)
    var kind by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf("") }
    val recent = remember { mutableStateListOf<String>() }
    val reveal = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val levels = listOf("轻松", "进阶", "刺激")
    var managing by remember { mutableStateOf(false) }

    fun draw(which: String) {
        val content = ContentLibrary.truthDare
        val custom = Store.customPrompts.items.filter { it.kind == which && it.level <= level }.map { it.text }
        val pool = (if (which == "truth") content.truth else content.dare)[level.coerceIn(0, 2)] + custom
        val candidates = pool.filter { it !in recent }.ifEmpty { pool }
        val picked = TrueRandom.pick(candidates)
        kind = which
        prompt = picked
        recent.add(picked)
        if (recent.size > 20) recent.removeAt(0)
        SoundEngine.play(SoundEffect.CARD_FLIP)
        Haptics.success()
        scope.launch {
            reveal.snapTo(0.6f)
            reveal.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
        Store.log(ToolKind.TRUTH_DARE.id, if (which == "truth") "真心话" else "大冒险", picked)
    }

    if (managing) {
        CustomPromptsScreen { managing = false }
        return
    }

    Screen(
        title = "真心话大冒险",
        horizontalAlignment = Alignment.CenterHorizontally,
        actions = { CircleIconButton(Icons.Rounded.Edit, "自定义题目") { managing = true } },
    ) {
        SegmentedPicker(listOf(0, 1, 2), level, { levels[it] }) { level = it }
        val isTruth = kind == "truth"
        val colors = when (kind) {
            null -> listOf(palette.accent, palette.accentSecondary)
            "truth" -> listOf(hex(0x60A5FA), hex(0x8B5CF6))
            else -> listOf(hex(0xF472B6), hex(0xEF4444))
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp)
                .graphicsLayer { scaleX = reveal.value; scaleY = reveal.value }
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(colors.map { it.copy(alpha = 0.85f) }))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text(
                when (kind) { null -> "准备好了吗？"; "truth" -> "真心话"; else -> "大冒险" },
                style = Fonts.headline,
                color = Color.White.copy(alpha = 0.85f),
            )
            Text(
                if (kind == null) "选择真心话或大冒险，抽一道题" else prompt,
                style = Fonts.title2,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            if (kind != null) Text("尺度：${levels[level.coerceIn(0, 2)]}", style = Fonts.caption, color = Color.White.copy(alpha = 0.7f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            ChoiceButton("真心话", Icons.Rounded.Forum, listOf(hex(0x60A5FA), hex(0x8B5CF6)), Modifier.weight(1f)) { draw("truth") }
            ChoiceButton("大冒险", Icons.Rounded.LocalFireDepartment, listOf(hex(0xF472B6), hex(0xEF4444)), Modifier.weight(1f)) { draw("dare") }
        }
        if (isTruth || kind != null) SecondaryText("最近抽过的题目会尽量避开", style = Fonts.caption)
        val customCount = Store.customPrompts.items.size
        SecondaryText(if (customCount > 0) "已加入 $customCount 道自定义题目（右上角管理）" else "右上角可以添加自己的题目", style = Fonts.caption)
    }
}

@Composable
private fun CustomPromptsScreen(onDone: () -> Unit) {
    val palette = LocalPalette.current
    var text by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("truth") }
    var level by remember { mutableIntStateOf(0) }
    val levels = listOf("轻松", "进阶", "刺激")
    Screen(title = "自定义题目", onBack = onDone) {
        GlassCard {
            SectionHeader("添加题目")
            SegmentedPicker(listOf("truth", "dare"), kind, { if (it == "truth") "真心话" else "大冒险" }) { kind = it }
            SegmentedPicker(listOf(0, 1, 2), level, { levels[it] }) { level = it }
            GlassTextField(text, "写下你的题目", singleLine = false) { text = it }
            BigActionButton("添加", Icons.Rounded.Add, enabled = text.isNotBlank()) {
                Store.customPrompts.add(CustomPrompt(kind = kind, text = text.trim(), level = level))
                text = ""
                Haptics.success()
            }
        }
        val prompts = Store.customPrompts.items.sortedByDescending { it.createdAt }
        if (prompts.isNotEmpty()) {
            GlassCard(spacing = 8.dp) {
                SectionHeader("我的题目")
                for (prompt in prompts) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(prompt.text, style = Fonts.body)
                            SecondaryText("${if (prompt.kind == "truth") "真心话" else "大冒险"} · ${levels[prompt.level.coerceIn(0, 2)]}", style = Fonts.caption)
                        }
                        CircleIconButton(Icons.Rounded.Delete, "删除", size = 32.dp) {
                            Store.customPrompts.update { list -> list.filter { it.id != prompt.id } }
                        }
                    }
                }
            }
        }
        SecondaryText("自定义题目会和内置题目一起参与抽取（不超过当前尺度）。", style = Fonts.caption)
    }
}

@Composable
private fun ChoiceButton(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors))
            .padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, Modifier.size(30.dp), tint = Color.White)
        Text(title, style = Fonts.headline, color = Color.White)
    }
}

// endregion
