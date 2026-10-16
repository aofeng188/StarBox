package com.tianaofeng.xingxia.ui.screens.tools

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.NamedColor
import com.tianaofeng.xingxia.core.SeededGenerator
import com.tianaofeng.xingxia.core.TrueRandom
import com.tianaofeng.xingxia.data.LotteryItem
import com.tianaofeng.xingxia.data.LotteryPool
import com.tianaofeng.xingxia.data.Prefs
import com.tianaofeng.xingxia.data.ScratchPreset
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.ChipRow
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.Confetti
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientText
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.OnShake
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.SelectChip
import com.tianaofeng.xingxia.ui.components.SoundEffect
import com.tianaofeng.xingxia.ui.components.SoundEngine
import com.tianaofeng.xingxia.ui.components.StepperRow
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.copyText
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.toColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.SecureRandom
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// region 自定义抽奖

@Composable
fun LotteryScreen() {
    val palette = LocalPalette.current
    var openId by remember { mutableStateOf<String?>(null) }
    var editingId by remember { mutableStateOf<String?>(null) }

    editingId?.let { id ->
        val pool = Store.lotteryPools.items.firstOrNull { it.id == id }
        if (pool != null) {
            LotteryEditor(pool) { editingId = null }
            return
        }
    }
    openId?.let { id ->
        val pool = Store.lotteryPools.items.firstOrNull { it.id == id }
        if (pool != null) {
            LotteryDraw(pool, onBack = { openId = null }) { editingId = id }
            return
        }
    }

    Screen(title = "自定义抽奖", spacing = 14.dp) {
        for (pool in Store.lotteryPools.items.sortedBy { it.createdAt }) {
            GlassCard(onClick = { openId = pool.id }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(palette.accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(pool.icon, style = TextStyle(fontSize = 34.sp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(pool.name, style = Fonts.headline)
                        SecondaryText(
                            if (pool.removeAfterDraw) "${pool.items.size} 种奖品 · 剩余 ${pool.items.sumOf { it.remaining }} 份"
                            else "${pool.items.size} 个选项 · 可重复抽取",
                            style = Fonts.caption,
                        )
                    }
                    CircleIconButton(Icons.Rounded.Edit, "编辑", size = 34.dp) { editingId = pool.id }
                    Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButton("新建奖池", Icons.Rounded.Add) {
                val pool = LotteryPool(name = "新奖池")
                Store.lotteryPools.add(pool)
                editingId = pool.id
            }
        }
    }
}

@Composable
private fun LotteryDraw(pool: LotteryPool, onBack: () -> Unit, onEdit: () -> Unit) {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var drawCount by remember { mutableIntStateOf(1) }
    var drawing by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    var rollingText by remember { mutableStateOf<String?>(null) }
    var confetti by remember { mutableIntStateOf(0) }
    val sessionLog = remember { mutableStateListOf<String>() }

    fun weightOf(item: LotteryItem, remaining: Int = item.remaining) = if (pool.removeAfterDraw) item.weight * remaining else item.weight
    val available = pool.items.filter { it.weight > 0 && (!pool.removeAfterDraw || it.remaining > 0) }

    fun save(transform: (LotteryPool) -> LotteryPool) {
        Store.lotteryPools.update { list -> list.map { if (it.id == pool.id) transform(it) else it } }
    }

    fun draw() {
        if (available.isEmpty() || drawing) return
        drawing = true
        results = emptyList()
        SoundEngine.play(SoundEffect.WHOOSH)
        // 先算出结果，再播放动画
        val remaining = pool.items.associate { it.id to it.remaining }.toMutableMap()
        val picked = mutableListOf<LotteryItem>()
        repeat(drawCount) {
            val candidates = pool.items.filter { it.weight > 0 && (!pool.removeAfterDraw || (remaining[it.id] ?: 0) > 0) }
            if (candidates.isEmpty()) return@repeat
            val index = TrueRandom.weightedIndex(candidates.map { weightOf(it, remaining[it.id] ?: 0) }) ?: return@repeat
            val item = candidates[index]
            picked += item
            if (pool.removeAfterDraw) remaining[item.id] = (remaining[item.id] ?: 1) - 1
        }
        val names = pool.items.map { it.name }
        scope.launch {
            var delayMs = 30.0
            repeat(22) {
                rollingText = TrueRandom.pick(names)
                SoundEngine.play(SoundEffect.TICK, 0.45f)
                Haptics.selection()
                delay(delayMs.toLong())
                delayMs *= 1.13
            }
            rollingText = null
            drawing = false
            if (pool.removeAfterDraw) {
                save { p -> p.copy(items = p.items.map { it.copy(remaining = max(0, remaining[it.id] ?: it.remaining)) }) }
            }
            results = picked.map { it.name }
            sessionLog.addAll(results)
            confetti++
            SoundEngine.play(SoundEffect.SUCCESS)
            Haptics.success()
            Store.log(ToolKind.LOTTERY.id, pool.name, results.joinToString("、"))
        }
    }

    OnShake { draw() }

    Box(Modifier.fillMaxSize()) {
        // 抽奖动画进行中不能编辑或重置：结果在动画结束时才写回，会覆盖掉中途的修改
        Screen(title = "${pool.icon} ${pool.name}", onBack = onBack, actions = { CircleIconButton(Icons.Rounded.Edit, "编辑", enabled = !drawing, onClick = onEdit) }) {
            GachaMachine(drawing, palette.spectrum)
            GlassCard(Modifier.heightIn(min = 90.dp), horizontalAlignment = Alignment.CenterHorizontally, spacing = 8.dp) {
                val rolling = rollingText
                when {
                    rolling != null -> Text(rolling, style = Fonts.title2, color = palette.secondaryText, textAlign = TextAlign.Center)
                    results.isNotEmpty() -> {
                        SecondaryText(if (results.size == 1) "恭喜抽中" else "本轮抽中 ${results.size} 个")
                        for (r in results) GradientText(r, style = if (results.size == 1) Fonts.display(30, FontWeight.Black) else Fonts.headline, textAlign = TextAlign.Center)
                    }
                    available.isNotEmpty() -> Text("准备好了吗？", style = Fonts.title3)
                    // 奖池为空、权重全为 0、抽完了，分别给出提示
                    pool.items.isEmpty() -> {
                        Text("先添加奖品吧", style = Fonts.title3)
                        SecondaryText("点右上角「编辑」添加奖品或选项", style = Fonts.caption)
                    }
                    pool.items.none { it.weight > 0 } -> {
                        Text("所有奖品权重都是 0", style = Fonts.title3)
                        SecondaryText("在「编辑」里调高权重才能抽", style = Fonts.caption)
                    }
                    else -> {
                        Text("奖品已经抽完啦", style = Fonts.title3)
                        SecondaryText("点下方奖池的「重置」可以再来一轮", style = Fonts.caption)
                    }
                }
            }
            GlassCard(padding = 14.dp) { StepperRow("一次抽 $drawCount 个", drawCount, 1..10) { drawCount = it } }
            BigActionButton(if (drawing) "抽奖中…" else "开始抽奖", Icons.Rounded.Redeem, enabled = !drawing && available.isNotEmpty()) { draw() }

            GlassCard(spacing = 8.dp) {
                SectionHeader("奖池", subtitle = if (pool.removeAfterDraw) "抽中后数量减少" else "可重复抽中", icon = Icons.Rounded.Inventory2) {
                    if (pool.removeAfterDraw) TextAction("重置", enabled = !drawing) {
                        save { p -> p.copy(items = p.items.map { it.copy(remaining = it.quantity) }) }
                        results = emptyList()
                        Haptics.success()
                    }
                }
                val total = available.sumOf { weightOf(it) }
                for (item in pool.items) {
                    val out = pool.removeAfterDraw && item.remaining == 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = Fonts.subheadline, color = if (out) palette.tertiaryText else palette.text, modifier = Modifier.weight(1f))
                        if (pool.removeAfterDraw) SecondaryText("${item.remaining}/${item.quantity}", style = Fonts.caption)
                        val p = if (total > 0 && !out && item.weight > 0) weightOf(item) / total else 0.0
                        Text("%.1f%%".format(p * 100), style = Fonts.caption, color = palette.accent, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
                    }
                }
            }
            if (sessionLog.isNotEmpty()) {
                GlassCard(spacing = 6.dp) {
                    SectionHeader("本次记录", icon = Icons.Rounded.FormatListNumbered)
                    sessionLog.withIndex().reversed().forEach { (i, item) -> SecondaryText("${i + 1}. $item", style = Fonts.caption) }
                }
            }
        }
        Confetti(confetti)
    }
}

/** 扭蛋机 */
@Composable
private fun GachaMachine(shaking: Boolean, colors: List<Color>) {
    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { time = (it - start) / 1000f }
    }
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(250.dp),
    ) {
        val cx = size.width / 2
        val d = density
        // 底座
        drawRoundRect(
            Brush.verticalGradient(listOf(colors[0], colors[1]), 165 * d, 235 * d),
            Offset(cx - 85 * d, 165 * d), Size(170 * d, 70 * d), CornerRadius(20 * d),
        )
        drawCircle(Color.Black.copy(alpha = 0.35f), 17 * d, Offset(cx, 200 * d))
        val knob = if (shaking) time * 400f else 0f
        rotate(knob, Offset(cx, 200 * d)) {
            drawRect(Color.White.copy(alpha = 0.8f), Offset(cx - 2 * d, 189 * d), Size(4 * d, 22 * d))
        }
        // 玻璃球
        val center = Offset(cx + if (shaking) sin(time * 40f) * 3 * d else 0f, 85 * d)
        val r = 85 * d
        drawCircle(Color.White.copy(alpha = 0.08f), r, center)
        val clip = Path().apply { addOval(Rect(center, r)) }
        clipPath(clip) {
            for (i in 0 until 14) {
                val speed = if (shaking) 9.0 else 0.8
                val phase = i * 1.7
                val fx = 0.6 + (i % 3) * 0.2
                val fy = 0.5 + (i % 4) * 0.15
                val ax = if (shaking) 50.0 else 40.0
                val ay = if (shaking) 50.0 else 20.0
                val base = if (shaking) 0.0 else 30.0
                val x = sin(time * speed * fx + phase) * ax
                val y = cos(time * speed * fy + phase * 1.3) * ay + base
                val p = Offset(center.x + (x * d).toFloat(), center.y + (y * d).toFloat())
                val color = colors[i % colors.size]
                drawCircle(Brush.verticalGradient(listOf(color, Color.White.copy(alpha = 0.9f)), p.y - 15 * d, p.y + 15 * d), 15 * d, p)
                drawLine(Color.White.copy(alpha = 0.35f), Offset(p.x - 15 * d, p.y), Offset(p.x + 15 * d, p.y), 2 * d)
            }
        }
        drawCircle(Color.White.copy(alpha = 0.4f), r, center, style = Stroke(1.5f * d))
        drawOval(Color.White.copy(alpha = 0.3f), Offset(center.x - 55 * d, center.y - 70 * d), Size(50 * d, 22 * d))
    }
}

@Composable
private fun LotteryEditor(pool: LotteryPool, onDone: () -> Unit) {
    val palette = LocalPalette.current
    var newName by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf(false) }
    val icons = listOf("🎁", "🎊", "🧧", "🏆", "🍀", "🎉", "💎", "🎟️", "🍬", "🌟")

    fun save(transform: (LotteryPool) -> LotteryPool) {
        Store.lotteryPools.update { list -> list.map { if (it.id == pool.id) transform(it) else it } }
    }

    fun updateItem(id: String, transform: (LotteryItem) -> LotteryItem) = save { p -> p.copy(items = p.items.map { if (it.id == id) transform(it) else it }) }

    Screen(title = "编辑奖池", onBack = onDone, actions = { TextAction("完成", onClick = onDone) }) {
        GlassCard {
            GlassTextField(pool.name, "名称") { name -> save { it.copy(name = name) } }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (icon in icons) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (pool.icon == icon) palette.accent.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { save { it.copy(icon = icon) } },
                        contentAlignment = Alignment.Center,
                    ) { Text(icon, style = Fonts.title2) }
                }
            }
            ToggleRow("抽中后减少数量（不放回）", pool.removeAfterDraw) { on -> save { it.copy(removeAfterDraw = on) } }
        }
        GlassCard(spacing = 10.dp) {
            SectionHeader("奖品", subtitle = if (pool.removeAfterDraw) "抽中概率 = 权重 × 剩余数量" else "抽中概率由权重决定")
            for (item in pool.items) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassTextField(item.name, "名称", modifier = Modifier.weight(1f)) { name -> updateItem(item.id) { it.copy(name = name) } }
                        CircleIconButton(Icons.Rounded.Delete, "删除", size = 32.dp) {
                            save { p -> p.copy(items = p.items.filter { it.id != item.id }) }
                        }
                    }
                    StepperRow("权重 ${item.weight.toInt()}", item.weight.toInt(), 0..20) { w -> updateItem(item.id) { it.copy(weight = w.toDouble()) } }
                    if (pool.removeAfterDraw) {
                        StepperRow("数量 ${item.quantity}", item.quantity, 1..999) { q ->
                            updateItem(item.id) { it.copy(quantity = q, remaining = max(0, it.remaining + q - it.quantity)) }
                        }
                    }
                    HorizontalDivider(color = palette.subtle(0.08f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassTextField(newName, "添加奖品或选项", modifier = Modifier.weight(1f)) { newName = it }
                CircleIconButton(Icons.Rounded.Add, "添加", enabled = newName.isNotBlank()) {
                    val name = newName.trim()
                    save { it.copy(items = it.items + LotteryItem(name = name)) }
                    newName = ""
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (confirmDelete) {
                GlassButton("确定删除「${pool.name}」", Icons.Rounded.Delete) {
                    Store.lotteryPools.update { list -> list.filter { it.id != pool.id } }
                    onDone()
                }
            } else {
                GlassButton("删除这个奖池", Icons.Rounded.Delete) { confirmDelete = true }
            }
        }
    }
}

// endregion

// region 指尖轮盘

private enum class FingerMode(val label: String) { ONE("选 1 人"), SEVERAL("选多人"), TEAMS("分两队") }
private enum class FingerPhase { WAITING, COUNTING, DONE }

@OptIn(ExperimentalTextApi::class)
@Composable
fun FingerScreen() {
    val palette = LocalPalette.current
    val measurer = rememberTextMeasurer()
    var mode by remember { mutableStateOf(FingerMode.ONE) }
    var pickCount by remember { mutableIntStateOf(2) }
    val touches = remember { mutableStateMapOf<Long, Offset>() }
    val colors = remember { mutableStateMapOf<Long, Color>() }
    var phase by remember { mutableStateOf(FingerPhase.WAITING) }
    var stableSince by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var countdownStart by remember { mutableLongStateOf(0L) }
    var winners by remember { mutableStateOf(setOf<Long>()) }
    var teams by remember { mutableStateOf(mapOf<Long, Int>()) }
    var resultTime by remember { mutableLongStateOf(0L) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val fingerColors = listOf(0xFF6B6B, 0xFFD166, 0x06D6A0, 0x4CC9F0, 0xB388FF, 0xFF8FC7, 0xF4A261, 0x80ED99, 0x48CAE4, 0xFFAFCC).map { hex(it.toLong()) }
    val teamColors = listOf(hex(0xFF5D73), hex(0x4CC9F0))
    val countdownMs = 2200L
    val minimum = if (mode == FingerMode.SEVERAL) pickCount + 1 else 2

    fun reset() {
        phase = FingerPhase.WAITING
        winners = emptySet()
        teams = emptyMap()
        colors.clear()
    }

    fun update(points: Map<Long, Offset>) {
        val added = points.keys - touches.keys
        val removed = touches.keys - points.keys
        removed.forEach { touches.remove(it) }
        touches.putAll(points)
        for (id in added) {
            colors[id] = fingerColors[colors.size % fingerColors.size]
            Haptics.tap()
            SoundEngine.play(SoundEffect.POP, 0.5f)
        }
        if (added.isNotEmpty() || removed.isNotEmpty()) {
            if (phase != FingerPhase.DONE) {
                phase = FingerPhase.WAITING
                stableSince = System.currentTimeMillis()
            }
        }
        if (points.isEmpty()) reset()
    }

    fun choose() {
        val ids = TrueRandom.shuffled(touches.keys.toList())
        when (mode) {
            FingerMode.ONE -> winners = setOf(ids[0])
            FingerMode.SEVERAL -> winners = ids.take(pickCount).toSet()
            FingerMode.TEAMS -> {
                winners = emptySet()
                teams = ids.withIndex().associate { it.value to it.index % 2 }
            }
        }
        phase = FingerPhase.DONE
        resultTime = System.currentTimeMillis()
        SoundEngine.play(if (mode == FingerMode.TEAMS) SoundEffect.SUCCESS else SoundEffect.CHIME)
        Haptics.success()
        val detail = if (mode == FingerMode.TEAMS) "${touches.size} 人分两队" else "${touches.size} 人中选 ${winners.size} 人"
        Store.log(ToolKind.FINGER.id, mode.label, detail)
    }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { }
            now = System.currentTimeMillis()
            val needed = if (mode == FingerMode.SEVERAL) pickCount + 1 else 2
            when (phase) {
                FingerPhase.WAITING -> if (touches.size >= needed && now - stableSince > 1000) {
                    phase = FingerPhase.COUNTING
                    countdownStart = now
                    Haptics.impact()
                }
                FingerPhase.COUNTING -> if (now - countdownStart >= countdownMs) choose()
                FingerPhase.DONE -> {}
            }
        }
    }

    val statusText = when (phase) {
        FingerPhase.WAITING -> when {
            touches.isEmpty() -> "等待手指…"
            touches.size < minimum -> "${touches.size} 人，还需要 ${minimum - touches.size} 人"
            else -> "${touches.size} 人 · 保持不动"
        }
        FingerPhase.COUNTING -> "选择中…"
        FingerPhase.DONE -> if (mode == FingerMode.TEAMS) "分队完成！" else if (winners.size == 1) "就是你了！" else "选中了 ${winners.size} 位"
    }

    Screen(title = "指尖轮盘", scrollable = false) {
        if (touches.isEmpty()) {
            GlassCard(padding = 14.dp) {
                SegmentedPicker(FingerMode.entries, mode, { it.label }) { mode = it }
                if (mode == FingerMode.SEVERAL) StepperRow("选出 $pickCount 人", pickCount, 2..8) { pickCount = it }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(palette.subtle(0.03f))
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val points = event.changes.filter { it.pressed }.associate { it.id.value to it.position }
                            event.changes.forEach { it.consume() }
                            update(points)
                        }
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val progress = if (phase == FingerPhase.COUNTING) min(1f, (now - countdownStart) / countdownMs.toFloat()) else 0f
                val since = if (phase == FingerPhase.DONE) (now - resultTime) / 1000f else 0f
                val t = now / 1000.0
                for ((id, point) in touches) {
                    var color = colors[id] ?: Color.White
                    var radius = (46 + sin(t * 5 + id) * 4).toFloat() * density
                    var alpha = 1f
                    if (phase == FingerPhase.DONE) {
                        if (mode == FingerMode.TEAMS) {
                            color = teamColors[teams[id] ?: 0]
                        } else if (id in winners) {
                            radius = (56 + min(1f, since * 3) * 24 + sin(t * 8).toFloat() * 5) * density
                        } else {
                            alpha = max(0.12f, 1 - since * 3)
                            radius = 40 * density
                        }
                    } else if (phase == FingerPhase.COUNTING) {
                        radius -= progress * 8 * density
                    }
                    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f * alpha), Color.Transparent), point, radius * 2), radius * 2, point)
                    drawCircle(color.copy(alpha = 0.85f * alpha), radius, point)
                    drawCircle(color.copy(alpha = 0.6f * alpha), radius + 8 * density, point, style = Stroke(3 * density))
                    if (phase == FingerPhase.COUNTING) {
                        val r = radius + 16 * density
                        drawArc(
                            Color.White.copy(alpha = 0.9f), -90f, 360f * progress, false,
                            Offset(point.x - r, point.y - r), Size(r * 2, r * 2),
                            style = Stroke(5 * density, cap = StrokeCap.Round),
                        )
                    }
                    val label = when {
                        phase == FingerPhase.DONE && mode == FingerMode.TEAMS -> if (teams[id] == 0) "红队" else "蓝队"
                        phase == FingerPhase.DONE && id in winners -> "★"
                        else -> null
                    }
                    if (label != null) {
                        val layout = measurer.measure(label, TextStyle(color = Color.White, fontSize = if (label == "★") 34.sp else 17.sp, fontWeight = FontWeight.Bold))
                        val y = if (label == "★") point.y - layout.size.height / 2f else point.y - radius - 30 * density - layout.size.height / 2f
                        drawText(layout, topLeft = Offset(point.x - layout.size.width / 2f, y))
                    }
                }
            }
            if (touches.isEmpty()) {
                Column(Modifier.align(Alignment.Center).padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Rounded.TouchApp, null, Modifier.size(50.dp), tint = palette.accent)
                    Text("每个人把一根手指放在屏幕上", style = Fonts.title3.copy(fontWeight = FontWeight.Bold))
                    SecondaryText(
                        when (mode) {
                            FingerMode.ONE -> "至少 2 人。手指放稳后自动倒数，随机选出一位（谁买单？谁先手？）"
                            FingerMode.SEVERAL -> "至少 ${pickCount + 1} 人，随机选出 $pickCount 位"
                            FingerMode.TEAMS -> "至少 2 人，随机分成红蓝两队"
                        },
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Text(
                statusText,
                style = Fonts.headline,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .glass(CircleShape)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

// endregion

// region 灵感生成器

private object GeneratorData {
    val traditionalColors = listOf(
        "胭脂" to 0x9D2933, "朱砂" to 0xFF461F, "绯红" to 0xC83C23, "海棠红" to 0xDB5A6B, "桃红" to 0xF47983, "嫣红" to 0xEF7A82,
        "银红" to 0xF05654, "酡红" to 0xDC3023, "品红" to 0xF00056, "绛紫" to 0x8C4356, "藕荷" to 0xE4C6D0, "丁香" to 0xCCA4E3,
        "雪青" to 0xB0A4E3, "青莲" to 0x801DAE, "黛紫" to 0x574266, "藏青" to 0x2E4E7E, "靛蓝" to 0x065279, "宝蓝" to 0x4B5CC4,
        "群青" to 0x4C8DAE, "石青" to 0x1685A9, "湖蓝" to 0x30DFF3, "月白" to 0xD6ECF0, "蟹壳青" to 0xBBCDC5, "鸦青" to 0x424C50,
        "墨色" to 0x50616D, "黛绿" to 0x426666, "苍翠" to 0x519A73, "竹青" to 0x789262, "葱绿" to 0x0EB83A, "松花绿" to 0xBCE672,
        "碧色" to 0x1BD1A5, "翡翠" to 0x3DE1AD, "青碧" to 0x48C0A3, "茶白" to 0xF3F9F1, "象牙白" to 0xFFFBF0, "缟色" to 0xF2ECDE,
        "鹅黄" to 0xFFF143, "秋香" to 0xD9B611, "姜黄" to 0xFFC773, "杏黄" to 0xFFA631, "橘黄" to 0xFF8936, "琥珀" to 0xCA6924,
        "赭石" to 0x845A33, "驼色" to 0xA88462, "檀色" to 0xB36D61, "珊瑚" to 0xFF7F50, "霁青" to 0x63BBD0, "天水碧" to 0x5AA4AE,
    ).map { NamedColor(it.first, it.second) }

    val surnames = "王李张刘陈杨黄赵吴周徐孙马朱胡郭何高林罗郑梁谢宋唐许韩冯邓曹彭曾萧田董袁潘于蒋蔡余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江尹薛闫段雷侯龙史陶黎贺顾毛郝龚邵万钱严覃武戴莫孔向汤".map { it.toString() }
    val compoundSurnames = listOf("欧阳", "司马", "上官", "诸葛", "慕容", "南宫", "东方", "令狐")
    val maleChars = "宇轩浩然子墨梓睿泽辰逸昊天佑铭博文瑞霖承安景行致远思源嘉懿煜城懿轩烨伟鸿涛晨阳俊驰皓明晟睿哲瀚彦卓凯昕旭尧舟川".map { it.toString() }
    val femaleChars = "梓涵欣怡诗雨语嫣若曦可馨梦琪雅静思彤一诺芷若清欢婉清悦心沐晴紫萱嘉怡安然星辰知夏初夏书瑶念慈语桐晓棠".map { it.toString() }
    val neutralChars = "安宁可言知行一白予夏清和星河云舒南风若水思远沐阳".map { it.toString() }
    val englishMale = listOf("Leo", "Oliver", "Ethan", "Noah", "Lucas", "Mason", "Elijah", "Logan", "Aiden", "Caleb", "Owen", "Ryan", "Nathan", "Dylan", "Ian", "Jasper", "Felix", "Hugo", "Theo", "Milo", "Adrian", "Julian", "Kai", "Max", "Sean", "Victor", "Eric", "Kevin", "Jason", "Allen")
    val englishFemale = listOf("Luna", "Olivia", "Emma", "Ava", "Mia", "Chloe", "Lily", "Grace", "Zoe", "Ivy", "Nora", "Ella", "Aria", "Stella", "Hazel", "Iris", "Ruby", "Cora", "Alice", "Elsa", "Sophie", "Clara", "Vivian", "Hannah", "Jessie", "Amy", "Cindy", "Daisy", "Fiona", "Yuki")
    val nickAdjectives = listOf("温柔的", "爱吃饭的", "不想上班的", "会发光的", "迷路的", "快乐的", "失眠的", "认真的", "糊涂的", "勇敢的", "慢吞吞的", "元气满满的", "安静的", "闪闪发光的", "想去旅行的", "爱睡觉的", "热爱生活的", "在逃的", "可爱的", "清醒的")
    val nickNouns = listOf("小熊", "月亮", "橘子汽水", "星星", "猫咪", "柠檬", "云朵", "海盐", "小鲸鱼", "蘑菇", "奶盖", "晚风", "兔子", "芝士", "企鹅", "西瓜", "小王子", "棉花糖", "宇航员", "气泡")
    val petNames = listOf("团子", "豆包", "可乐", "布丁", "年糕", "汤圆", "奶糖", "芝麻", "饭团", "毛毛", "球球", "皮蛋", "旺财", "来福", "咪咪", "胖虎", "花卷", "麻薯", "雪球", "栗子")
}

private enum class GeneratorKind(val label: String) { COLOR("配色"), PASSWORD("密码"), NAME("名字"), LETTER("字母") }

@Composable
fun GeneratorsScreen() {
    var kind by remember { mutableStateOf(GeneratorKind.COLOR) }
    Screen(title = "灵感生成器") {
        SegmentedPicker(GeneratorKind.entries, kind, { it.label }) { kind = it }
        when (kind) {
            GeneratorKind.COLOR -> ColorGenerator()
            GeneratorKind.PASSWORD -> PasswordGenerator()
            GeneratorKind.NAME -> NameGenerator()
            GeneratorKind.LETTER -> LetterGenerator()
        }
    }
}

private enum class ColorStyle(val label: String) { HARMONY("随机和谐"), ANALOGOUS("邻近色"), COMPLEMENTARY("互补色"), TRADITIONAL("中国传统色") }

private fun hsvColor(h: Double, s: Double, v: Double): NamedColor {
    val argb = Color.hsv((h % 1.0 * 360).toFloat(), s.toFloat(), v.toFloat()).toArgb()
    return NamedColor("", argb and 0xFFFFFF)
}

@Composable
private fun ColorGenerator() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    var style by remember { mutableStateOf(ColorStyle.HARMONY) }
    var colors by remember { mutableStateOf<List<NamedColor>>(emptyList()) }
    var locked by remember { mutableStateOf(setOf<Int>()) }
    var copied by remember { mutableStateOf<Int?>(null) }

    fun generate() {
        copied = null
        val next = mutableListOf<NamedColor>()
        val baseHue = TrueRandom.double()
        for (i in 0 until 5) {
            if (i in locked && i < colors.size) {
                next += colors[i]
                continue
            }
            next += when (style) {
                ColorStyle.TRADITIONAL -> TrueRandom.pick(GeneratorData.traditionalColors.filter { it !in next })
                ColorStyle.HARMONY -> hsvColor(baseHue + i * 0.618034, 0.45 + TrueRandom.double() * 0.3, 0.7 + TrueRandom.double() * 0.25)
                ColorStyle.ANALOGOUS -> hsvColor(baseHue + i * 0.06, 0.4 + i * 0.1, 0.95 - i * 0.1)
                ColorStyle.COMPLEMENTARY -> hsvColor(if (i % 2 == 0) baseHue else baseHue + 0.5, 0.35 + i * 0.12, 0.95 - i * 0.08)
            }
        }
        colors = next
        SoundEngine.play(SoundEffect.POP, 0.6f)
        Haptics.selection()
    }

    LaunchedEffect(style) { generate() }
    OnShake { generate() }

    ChipRow { for (s in ColorStyle.entries) SelectChip(s.label, s == style) { style = s } }
    GlassCard(padding = 12.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            colors.forEachIndexed { index, color ->
                Column(
                    Modifier
                        .weight(1f)
                        .clickable {
                            copyText(context, color.hexString)
                            copied = index
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(color.toColor()),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            Modifier
                                .padding(8.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.25f))
                                .clickable { locked = if (index in locked) locked - index else locked + index },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(if (index in locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen, if (index in locked) "解锁颜色" else "锁定颜色", Modifier.size(14.dp), tint = Color.White)
                        }
                    }
                    Text(color.name.ifEmpty { " " }, style = Fonts.caption.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                    Text(if (copied == index) "已拷贝" else color.hexString, style = TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace), color = palette.secondaryText, maxLines = 1)
                }
            }
        }
    }
    BigActionButton("换一组", Icons.Rounded.Palette) { generate() }
    SecondaryText("点颜色拷贝色值，点锁头保留喜欢的颜色", style = Fonts.caption)
}

@Composable
private fun PasswordGenerator() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    var length by Prefs.int("pw.length", 16)
    var upper by Prefs.bool("pw.upper", true)
    var lower by Prefs.bool("pw.lower", true)
    var digits by Prefs.bool("pw.digits", true)
    var symbols by Prefs.bool("pw.symbols", true)
    var noSimilar by Prefs.bool("pw.noSimilar", true)
    var password by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    val random = remember { SecureRandom() }

    val charset = buildString {
        if (upper) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        if (lower) append("abcdefghijklmnopqrstuvwxyz")
        if (digits) append("0123456789")
        if (symbols) append("!@#$%^&*-_=+?")
    }.filter { !noSimilar || it !in "O0Il1" }
    val entropy = if (charset.isEmpty()) 0.0 else length * ln(charset.length.toDouble()) / ln(2.0)
    val level = when {
        entropy < 40 -> 1
        entropy < 60 -> 2
        entropy < 80 -> 3
        else -> 4
    }
    val levelColor = listOf(Color.Transparent, hex(0xEF4444), hex(0xF59E0B), hex(0x34D399), hex(0x22D3EE))[level]

    fun generate() {
        if (charset.isEmpty()) return
        // SecureRandom.nextInt 本身没有取模偏差
        password = (0 until length).map { charset[random.nextInt(charset.length)] }.joinToString("")
        copied = false
        SoundEngine.play(SoundEffect.TICK)
        Haptics.selection()
    }

    LaunchedEffect(length, upper, lower, digits, symbols, noSimilar) { generate() }

    GlassCard(padding = 20.dp, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            password.ifEmpty { "——" },
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until 4) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (i < level) levelColor else palette.subtle(0.1f)),
                )
            }
        }
        SecondaryText("强度：${listOf("", "弱", "一般", "强", "非常强")[level]} · 约 ${entropy.toInt()} 位熵", style = Fonts.caption)
        GlassButton(if (copied) "已拷贝" else "拷贝密码", Icons.Rounded.ContentCopy, enabled = password.isNotEmpty()) {
            copyText(context, password, sensitive = true)
            copied = true
        }
    }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("长度", style = Fonts.body)
            Slider(
                value = length.toFloat(),
                onValueChange = { length = it.toInt() },
                valueRange = 6f..40f,
                steps = 33,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent),
            )
            Text("$length", style = Fonts.body, modifier = Modifier.width(30.dp))
        }
        ToggleRow("大写字母 A-Z", upper) { upper = it }
        ToggleRow("小写字母 a-z", lower) { lower = it }
        ToggleRow("数字 0-9", digits) { digits = it }
        ToggleRow("符号 !@#…", symbols) { symbols = it }
        ToggleRow("排除易混淆字符（0 O 1 l I）", noSimilar) { noSimilar = it }
    }
    BigActionButton("生成新密码", Icons.Rounded.Key, enabled = charset.isNotEmpty()) { generate() }
    SecondaryText("使用系统加密级随机数生成，仅在本机生成，不会上传。拷贝的密码 60 秒后会从剪贴板清除。", style = Fonts.caption)
}

private enum class NameStyle(val label: String) {
    CHINESE_MALE("中文·男"), CHINESE_FEMALE("中文·女"), CHINESE_ANY("中文·中性"), ENGLISH("英文名"), NICKNAME("网名"), PET("宠物名")
}

@Composable
private fun NameGenerator() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    var style by remember { mutableStateOf(NameStyle.CHINESE_ANY) }
    var surname by remember { mutableStateOf("") }
    var names by remember { mutableStateOf<List<String>>(emptyList()) }
    var copied by remember { mutableStateOf<String?>(null) }

    fun make(): String {
        val d = GeneratorData
        return when (style) {
            NameStyle.CHINESE_MALE, NameStyle.CHINESE_FEMALE, NameStyle.CHINESE_ANY -> {
                val family = surname.trim().ifEmpty {
                    if (TrueRandom.int(0..19) == 0) TrueRandom.pick(d.compoundSurnames) else TrueRandom.pick(d.surnames)
                }
                val pool = when (style) {
                    NameStyle.CHINESE_MALE -> d.maleChars
                    NameStyle.CHINESE_FEMALE -> d.femaleChars
                    else -> d.neutralChars + d.maleChars.take(20) + d.femaleChars.take(20)
                }
                family + if (TrueRandom.bool()) TrueRandom.pick(pool) else TrueRandom.pick(pool) + TrueRandom.pick(pool)
            }
            NameStyle.ENGLISH -> TrueRandom.pick(if (TrueRandom.bool()) d.englishMale else d.englishFemale)
            NameStyle.NICKNAME -> TrueRandom.pick(d.nickAdjectives) + TrueRandom.pick(d.nickNouns)
            NameStyle.PET -> TrueRandom.pick(d.petNames)
        }
    }

    fun generate() {
        val set = LinkedHashSet<String>()
        var guard = 0
        while (set.size < 8 && guard++ < 200) set.add(make())
        names = set.toList()
        copied = null
        SoundEngine.play(SoundEffect.POP, 0.6f)
    }

    LaunchedEffect(style) { generate() }
    OnShake { generate() }

    ChipRow { for (s in NameStyle.entries) SelectChip(s.label, s == style) { style = s } }
    if (style in listOf(NameStyle.CHINESE_MALE, NameStyle.CHINESE_FEMALE, NameStyle.CHINESE_ANY)) {
        GlassTextField(surname, "指定姓氏（可选）") { surname = it }
    }
    for (row in names.chunked(2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (name in row) {
                Column(
                    Modifier
                        .weight(1f)
                        .pressable {
                            copyText(context, name)
                            copied = name
                        }
                        .glass(RoundedCornerShape(18.dp))
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(name, style = Fonts.title3, maxLines = 1)
                    Text(if (copied == name) "已拷贝" else "点按拷贝", style = Fonts.caption2, color = palette.secondaryText)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    BigActionButton("换一批", Icons.Rounded.TextFields) { generate() }
}

@Composable
private fun LetterGenerator() {
    val palette = LocalPalette.current
    var letter by remember { mutableStateOf("?") }
    var lowercase by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<String>() }
    val pop = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    fun next() {
        val letters = if (lowercase) "abcdefghijklmnopqrstuvwxyz" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val value = letters[TrueRandom.int(0..25)].toString()
        letter = value
        history.add(0, value)
        if (history.size > 12) history.removeAt(history.size - 1)
        SoundEngine.play(SoundEffect.POP)
        Haptics.selection()
        scope.launch {
            pop.snapTo(0.7f)
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
    }

    GlassCard(Modifier.heightIn(min = 200.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            letter,
            style = Fonts.display(140, FontWeight.Black).copy(brush = palette.accentBrush),
            modifier = Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value },
        )
    }
    GlassCard(padding = 14.dp) { ToggleRow("小写字母", lowercase) { lowercase = it } }
    BigActionButton("随机一个字母", Icons.Rounded.TextFields) { next() }
    if (history.isNotEmpty()) SecondaryText(history.joinToString("  "), style = Fonts.subheadline.copy(fontFamily = FontFamily.Monospace))
}

// endregion

// region 刮刮乐

@Composable
fun ScratchScreen() {
    val palette = LocalPalette.current
    var presetId by Prefs.string("scratch.preset", "")
    var useCustom by remember { mutableStateOf(false) }
    var customMessage by remember { mutableStateOf("") }
    var prize by remember { mutableStateOf("") }
    var isBlank by remember { mutableStateOf(false) }
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var scratched by remember { mutableStateOf(setOf<Int>()) }
    var revealed by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    var cardId by remember { mutableIntStateOf(0) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var lastSound by remember { mutableLongStateOf(0L) }
    var editingId by remember { mutableStateOf<String?>(null) }
    val presets = Store.scratchPresets.items.sortedBy { it.createdAt }
    val preset = presets.firstOrNull { it.id == presetId } ?: presets.firstOrNull()
    val columns = 22
    val rows = 14

    fun reveal() {
        if (revealed) return
        revealed = true
        if (isBlank) {
            SoundEngine.play(SoundEffect.FAIL)
            Haptics.error()
        } else {
            SoundEngine.play(SoundEffect.SUCCESS)
            Haptics.success()
            confetti++
        }
        Store.log(ToolKind.SCRATCH.id, if (useCustom) "自定义" else preset?.name ?: "", prize)
    }

    fun newCard() {
        strokes.clear()
        scratched = emptySet()
        revealed = false
        cardId++
        if (useCustom) {
            prize = customMessage.ifEmpty { "今天也要开心！" }
            isBlank = false
            return
        }
        val prizes = preset?.prizes?.ifEmpty { null } ?: listOf("惊喜")
        val weights = prizes.map { 1.0 } + (preset?.blankWeight ?: 1.0)
        val index = TrueRandom.weightedIndex(weights) ?: 0
        if (index >= prizes.size) {
            prize = "谢谢惠顾"
            isBlank = true
        } else {
            prize = prizes[index]
            isBlank = false
        }
    }

    fun mark(point: Offset) {
        if (cardSize.width == 0) return
        val cellW = cardSize.width / columns.toFloat()
        val cellH = cardSize.height / rows.toFloat()
        val r = 22f * (cardSize.width / 330f)
        val added = mutableSetOf<Int>()
        for (row in 0 until rows) for (column in 0 until columns) {
            val center = Offset((column + 0.5f) * cellW, (row + 0.5f) * cellH)
            if (hypot(center.x - point.x, center.y - point.y) <= r) added += row * columns + column
        }
        scratched = scratched + added
        if (scratched.size.toFloat() / (columns * rows) > 0.55f) reveal()
    }

    LaunchedEffect(useCustom, preset?.id) { newCard() }

    editingId?.let { id ->
        val item = Store.scratchPresets.items.firstOrNull { it.id == id }
        if (item != null) {
            ScratchPresetEditor(item) { editingId = null }
            return
        }
    }

    Box(Modifier.fillMaxSize()) {
        Screen(title = "刮刮乐", horizontalAlignment = Alignment.CenterHorizontally) {
            SegmentedPicker(listOf(false, true), useCustom, { if (it) "写一句话" else "随机奖品" }) { useCustom = it }
            if (useCustom) {
                GlassTextField(customMessage, "写下刮开后要显示的话，比如「生日快乐！」", singleLine = false) {
                    customMessage = it
                    if (!revealed && strokes.isEmpty()) prize = it.ifEmpty { "今天也要开心！" }
                }
            } else {
                ChipRow {
                    for (item in presets) SelectChip(item.name, item.id == preset?.id) { presetId = item.id }
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.subtle(0.07f))
                            .clickable {
                                val item = ScratchPreset(name = "新卡池", prizes = listOf("奖品一", "奖品二"))
                                Store.scratchPresets.add(item)
                                editingId = item.id
                            },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Add, "新建卡池", Modifier.size(18.dp), tint = palette.text) }
                }
            }

            // 卡片
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .onSizeChanged { cardSize = it },
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                if (isBlank) listOf(hex(0x64748B), hex(0x334155)) else listOf(palette.accent, palette.accentSecondary),
                            ),
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                ) {
                    Text(if (isBlank) "🙂" else "🎁", style = TextStyle(fontSize = 34.sp))
                    Text(prize, style = Fonts.display(28, FontWeight.Black), color = if (isBlank) Color.White else palette.onAccent, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 20.dp))
                }
                val coatingAlpha by animateFloatAsState(if (revealed) 0f else 1f, tween(600), label = "coating")
                if (coatingAlpha > 0f) {
                    ScratchCoating(
                        strokes,
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = coatingAlpha }
                            .pointerInput(cardId, revealed) {
                                if (revealed) return@pointerInput
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    down.consume()
                                    strokes.add(listOf(down.position))
                                    mark(down.position)
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) break
                                        change.consume()
                                        strokes[strokes.size - 1] = strokes.last() + change.position
                                        mark(change.position)
                                        val now = System.currentTimeMillis()
                                        if (now - lastSound > 80) {
                                            lastSound = now
                                            SoundEngine.play(SoundEffect.SCRATCH, 0.7f)
                                            Haptics.selection()
                                        }
                                        if (revealed) break
                                    }
                                }
                            },
                    )
                }
            }

            Text(
                if (revealed) (if (isBlank) "下次一定！" else "恭喜！") else "用手指刮开银色涂层",
                style = Fonts.headline,
                color = if (revealed) palette.accent else palette.secondaryText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton("直接揭晓", Icons.Rounded.Visibility, enabled = !revealed) { reveal() }
                GlassButton("再来一张", Icons.Rounded.Refresh) { newCard() }
            }
            if (!useCustom && preset != null) {
                GlassCard(spacing = 6.dp) {
                    SectionHeader("卡池「${preset.name}」", icon = Icons.Rounded.Redeem) {
                        CircleIconButton(Icons.Rounded.Edit, "编辑卡池", size = 32.dp) { editingId = preset.id }
                    }
                    SecondaryText(preset.prizes.joinToString(" · "), style = Fonts.caption)
                    SecondaryText("谢谢惠顾的权重：${preset.blankWeight.toInt()}", style = Fonts.caption2)
                }
            }
        }
        Confetti(confetti)
    }
}

/** 银色刮层：在离屏图层里画涂层，再用「清除」混合模式擦掉刮过的地方 */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun ScratchCoating(strokes: List<List<Offset>>, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        drawRect(Brush.linearGradient(listOf(hex(0xE5E7EB), hex(0x9CA3AF), hex(0xD1D5DB), hex(0x6B7280))))
        val rng = SeededGenerator(42uL)
        repeat(400) {
            drawCircle(
                Color.White.copy(alpha = rng.double(0.2, 0.7).toFloat()), 0.75f * density,
                Offset(rng.unit().toFloat() * size.width, rng.unit().toFloat() * size.height),
            )
        }
        for (i in 0 until 12) {
            val y = i * size.height / 12
            drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y + 40 * density), 6 * density)
        }
        val layout = measurer.measure("刮开有惊喜", TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = hex(0x4B5563)))
        drawText(layout, topLeft = Offset((size.width - layout.size.width) / 2, (size.height - layout.size.height) / 2))
        val width = 44 * density
        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            if (stroke.size == 1) {
                drawCircle(Color.Black, width / 2, stroke[0], blendMode = BlendMode.Clear)
            } else {
                val path = Path().apply {
                    moveTo(stroke[0].x, stroke[0].y)
                    for (p in stroke.drop(1)) lineTo(p.x, p.y)
                }
                drawPath(path, Color.Black, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Clear)
            }
        }
    }
}

@Composable
private fun ScratchPresetEditor(preset: ScratchPreset, onDone: () -> Unit) {
    var name by remember { mutableStateOf(preset.name) }
    var text by remember { mutableStateOf(preset.prizes.joinToString("\n")) }
    var blank by remember { mutableIntStateOf(preset.blankWeight.toInt()) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save() {
        Store.scratchPresets.update { list ->
            list.map {
                if (it.id == preset.id) it.copy(
                    name = name.ifBlank { "卡池" },
                    prizes = text.lines().map { line -> line.trim() }.filter { line -> line.isNotEmpty() },
                    blankWeight = blank.toDouble(),
                ) else it
            }
        }
        onDone()
    }

    Screen(title = "编辑卡池", onBack = { save() }, actions = { TextAction("完成") { save() } }) {
        GlassCard {
            GlassTextField(name, "卡池名称") { name = it }
            SecondaryText("奖品（每行一个）", style = Fonts.footnote)
            GlassTextField(text, "奖品一\n奖品二", singleLine = false) { text = it }
            StepperRow("谢谢惠顾权重 $blank", blank, 0..20) { blank = it }
            SecondaryText("每个奖品的权重为 1。权重为 0 表示必中奖。", style = Fonts.caption)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (confirmDelete) {
                GlassButton("确定删除", Icons.Rounded.Delete) {
                    Store.scratchPresets.update { list -> list.filter { it.id != preset.id } }
                    onDone()
                }
            } else {
                GlassButton("删除这个卡池", Icons.Rounded.Delete) { confirmDelete = true }
            }
        }
    }
}

// endregion
