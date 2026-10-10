package com.tianaofeng.xingxia.ui.screens.divination

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneStick
import com.tianaofeng.xingxia.core.SeededGenerator
import com.tianaofeng.xingxia.core.TarotCard
import com.tianaofeng.xingxia.core.TrueRandom
import com.tianaofeng.xingxia.core.chineseNumber
import com.tianaofeng.xingxia.data.StickDraw
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.ChipRow
import com.tianaofeng.xingxia.ui.components.Confetti
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.OnShake
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SelectChip
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.starPath
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.common.StickTubeIllustration
import com.tianaofeng.xingxia.ui.screens.common.TarotBackMini
import com.tianaofeng.xingxia.ui.screens.common.TarotCardBack
import com.tianaofeng.xingxia.ui.screens.common.stickLevelColor
import com.tianaofeng.xingxia.ui.screens.common.stickLevelInk
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

// region 求签

private enum class StickPhase { IDLE, SHAKING, DROPPED, REVEALED }

@Composable
fun StickScreen() {
    val palette = LocalPalette.current
    val today = DayKey.today
    val todayDraw = Store.stickDraws.items.lastOrNull { it.dayKey == today.rawValue && it.profileId == Store.primary?.id }

    var phase by remember { mutableStateOf(if (todayDraw != null) StickPhase.REVEALED else StickPhase.IDLE) }
    var stick by remember { mutableStateOf(todayDraw?.let { ContentLibrary.stick(it.stickNumber) }) }
    var energy by remember { mutableFloatStateOf(0f) }
    var wobble by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var wish by remember { mutableStateOf("综合") }
    var isExtra by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    var lastRattle by remember { mutableStateOf(0L) }
    val wishes = listOf("综合", "事业", "感情", "财运", "健康", "学业", "出行")

    fun rattle() {
        lastRattle = System.currentTimeMillis()
        Haptics.selection()
    }

    fun reveal() {
        val s = stick ?: return
        phase = StickPhase.REVEALED
        Haptics.success()
        if (s.isLucky) confetti++
        if (todayDraw == null && !isExtra) {
            Store.stickDraws.add(StickDraw(today.rawValue, Store.primary?.id, s.n))
        }
        Store.log("stick", "求$wish", "${s.numberText} ${s.levelDisplayName} · ${s.title}")
    }

    OnShake(enabled = phase == StickPhase.IDLE || phase == StickPhase.SHAKING) {
        energy += 0.45f
        phase = StickPhase.SHAKING
        rattle()
    }

    LaunchedEffect(phase) {
        while (phase == StickPhase.IDLE || phase == StickPhase.SHAKING) {
            delay(33)
            val intensity = if (holding) 0.85f else 0f
            if (intensity > 0) {
                if (phase == StickPhase.IDLE) phase = StickPhase.SHAKING
                energy += intensity * 0.014f
                wobble += 0.22f + intensity * 0.25f
                if (System.currentTimeMillis() - lastRattle > 280) rattle()
            } else if (phase == StickPhase.SHAKING) {
                wobble += 0.05f
                energy = maxOf(0f, energy - 0.002f)
            }
            if (energy >= 1f) {
                stick = ContentLibrary.stick(TrueRandom.int(1..ContentLibrary.sticks.size))
                holding = false
                phase = StickPhase.DROPPED
                delay(350)
                Haptics.heavy()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        val current = stick
        if (phase == StickPhase.REVEALED && current != null) {
            StickResult(current, wish, isExtra) {
                isExtra = true
                energy = 0f
                wobble = 0f
                stick = null
                phase = StickPhase.IDLE
            }
        } else {
            Screen(title = "求签", scrollable = false, horizontalAlignment = Alignment.CenterHorizontally) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (phase == StickPhase.DROPPED) "签已落地" else "心中默念所求之事", style = Fonts.serif(26, bold = true))
                    SecondaryText(if (phase == StickPhase.DROPPED) "点按签文，看看今天的指引" else "摇动手机，或按住下方按钮摇签")
                }
                if (phase == StickPhase.IDLE) {
                    ChipRow { for (item in wishes) SelectChip(item, wish == item) { wish = item } }
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(380.dp)) {
                        drawCircle(
                            Brush.radialGradient(listOf(hex(0xFFB347).copy(alpha = 0.45f + energy.coerceAtMost(1f) * 0.4f), Color.Transparent)),
                            size.minDimension / 2 * (1 + energy.coerceAtMost(1f) * 0.15f),
                        )
                    }
                    if (phase == StickPhase.DROPPED && current != null) {
                        val out = remember { Animatable(0f) }
                        LaunchedEffect(Unit) { out.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
                        SingleStick(
                            current.n,
                            Modifier
                                .graphicsLayer {
                                    rotationZ = -40f + 32f * out.value
                                    translationX = (40f - 40f * out.value) * density
                                    translationY = (-160f + 180f * out.value) * density
                                    val s = 0.6f + 0.5f * out.value
                                    scaleX = s
                                    scaleY = s
                                }
                                .pressable { reveal() },
                        )
                    } else {
                        val shaking = phase == StickPhase.SHAKING
                        StickTubeIllustration(
                            230.dp,
                            shake = wobble,
                            modifier = Modifier.graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 1f)
                                rotationZ = if (shaking) sin(wobble * 2.6f) * 9f * minOf(1f, 0.3f + energy) else 0f
                                translationY = if (shaking) sin(wobble * 5.1f) * 6f * density else 0f
                            },
                        )
                    }
                }
                if (phase != StickPhase.DROPPED) {
                    Box(
                        Modifier
                            .width(260.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(palette.subtle(0.1f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(energy.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(hex(0xFFD166), hex(0xFF6B35)))),
                        )
                    }
                    val pressScale by animateFloatAsState(if (holding) 0.95f else 1f, label = "hold")
                    Box(
                        Modifier
                            .padding(bottom = 30.dp)
                            .width(220.dp)
                            .height(56.dp)
                            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(hex(0xE11D48), hex(0xF59E0B))))
                            .pointerInput(Unit) {
                                detectTapGestures(onPress = {
                                    holding = true
                                    tryAwaitRelease()
                                    holding = false
                                })
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("按住摇签", style = Fonts.headline, color = Color.White)
                    }
                } else {
                    BigActionButton("解签", modifier = Modifier.padding(bottom = 30.dp)) { reveal() }
                }
            }
        }
        Confetti(confetti)
    }
}

/** 掉出来的一支签 */
@Composable
private fun SingleStick(number: Int, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(34.dp, 26.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(hex(0xC0392B)),
        )
        Column(
            Modifier
                .size(34.dp, 210.dp)
                .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(Brush.horizontalGradient(listOf(hex(0xF7E1B5), hex(0xD9AE6D)))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            for (char in "第${chineseNumber(number)}签") {
                Text(char.toString(), style = Fonts.serif(17, bold = true), color = hex(0x7B1E1E))
            }
        }
    }
}

@Composable
private fun StickResult(stick: FortuneStick, wish: String, isExtra: Boolean, onAgain: () -> Unit) {
    val palette = LocalPalette.current
    val ink = stickLevelInk(stick.level, palette.isDark)
    val stamp = remember(stick.n) { Animatable(0f) }
    LaunchedEffect(stick.n) {
        delay(350)
        stamp.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
        Haptics.heavy()
    }
    Screen(title = "求签") {
        GlassCard(padding = 22.dp, tint = stickLevelColor(stick.level).copy(alpha = 0.08f), spacing = 18.dp) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stick.numberText, style = Fonts.serif(30, bold = true))
                    SecondaryText("「${stick.title}」", style = Fonts.serif(20))
                }
                // 印章
                Box(
                    Modifier
                        .size(74.dp)
                        .graphicsLayer {
                            rotationZ = -30f + 18f * stamp.value
                            val s = 2.4f - 1.4f * stamp.value
                            scaleX = s
                            scaleY = s
                            alpha = 0.92f * stamp.value
                        }
                        .border(3.dp, hex(0xE53935), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (stick.level == "下") "下签" else stick.level, style = Fonts.serif(if (stick.level.length > 1) 22 else 26, bold = true), color = hex(0xE53935))
                }
            }

            // 竖排签诗（从右往左）
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(hex(0xFFF6E0, 0.12f), hex(0xFFD166, 0.06f))))
                    .border(1.dp, hex(0xFFD166, 0.35f), RoundedCornerShape(18.dp))
                    .padding(vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            ) {
                for (line in stick.poem.reversed()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (char in line) Text(char.toString(), style = Fonts.serif(24, bold = true))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("解曰", style = Fonts.serif(17, bold = true), color = ink)
                Text(stick.meaning, style = Fonts.body)
                SecondaryText(stick.blessing)
            }
        }

        GlassCard {
            SectionHeader("签文详解", subtitle = if (wish == "综合") null else "所求：$wish", icon = Icons.Rounded.ListAlt)
            for (row in stick.areas.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((area, verdict) in row) {
                        val highlighted = area == wish || (wish == "学业" && area == "事业")
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (highlighted) stickLevelColor(stick.level).copy(alpha = 0.18f) else palette.subtle(0.05f))
                                .then(if (highlighted) Modifier.border(1.dp, stickLevelColor(stick.level).copy(alpha = 0.6f), RoundedCornerShape(14.dp)) else Modifier)
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(area, style = Fonts.caption, color = if (highlighted) ink else palette.secondaryText)
                            Text(verdict, style = Fonts.subheadline.weight(FontWeight.SemiBold), maxLines = 1)
                        }
                    }
                }
            }
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryText(if (isExtra) "这一签不计入今日一签" else "心诚则灵 · 一日一签", style = Fonts.footnote)
            GlassButton("为家人朋友再求一签", Icons.Rounded.Refresh, onClick = onAgain)
        }
    }
}

// endregion

// region 塔罗

private enum class TarotSpread(val label: String, val positions: List<String>, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SINGLE("单张指引", listOf("今日指引"), "抽一张牌，获得今天的提示", Icons.Rounded.CropPortrait),
    TIME("时间之流", listOf("过去", "现在", "未来"), "看清事情的来龙去脉", Icons.Rounded.Schedule),
    LOVE("情感关系", listOf("你的状态", "对方的状态", "关系走向"), "了解两个人之间的能量", Icons.Rounded.Favorite),
    ADVICE("问题解答", listOf("现状", "阻碍", "建议"), "针对一个具体问题给出方向", Icons.Rounded.QuestionAnswer),
}

private enum class TarotPhase { SETUP, SHUFFLING, PICKING, READING }

private data class DrawnCard(val deckIndex: Int, val card: TarotCard, val reversed: Boolean)

/** SF Symbol → 牌面图案 */
private fun tarotEmoji(symbol: String): String = when (symbol) {
    "figure.walk" -> "🚶"
    "wand.and.stars" -> "🪄"
    "moon.fill" -> "🌙"
    "leaf.fill" -> "🌿"
    "crown.fill" -> "👑"
    "building.columns.fill" -> "🏛️"
    "heart.circle.fill" -> "💞"
    "shield.lefthalf.filled" -> "🛡️"
    "flame.fill" -> "🔥"
    "flashlight.on.fill" -> "🏮"
    "arrow.triangle.2.circlepath" -> "☸️"
    "scalemass.fill" -> "⚖️"
    "hourglass" -> "⏳"
    "leaf.arrow.triangle.circlepath" -> "🍂"
    "drop.halffull" -> "💧"
    "link" -> "⛓️"
    "bolt.fill" -> "⚡"
    "star.fill" -> "⭐"
    "moon.stars.fill" -> "🌕"
    "sun.max.fill" -> "☀️"
    "megaphone.fill" -> "📯"
    "globe.asia.australia.fill" -> "🌏"
    else -> "✨"
}

@Composable
fun TarotScreen() {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(TarotPhase.SETUP) }
    var spread by remember { mutableStateOf(TarotSpread.TIME) }
    var question by remember { mutableStateOf("") }
    var deck by remember { mutableStateOf<List<DrawnCard>>(emptyList()) }
    val picked = remember { mutableStateListOf<DrawnCard>() }
    val flipped = remember { mutableStateListOf<Int>() }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var confetti by remember { mutableIntStateOf(0) }

    fun reset() {
        phase = TarotPhase.SETUP
        picked.clear()
        flipped.clear()
    }

    fun startShuffle() {
        deck = TrueRandom.shuffled(ContentLibrary.tarot).mapIndexed { i, card -> DrawnCard(i, card, TrueRandom.bool()) }
        picked.clear()
        flipped.clear()
        phase = TarotPhase.SHUFFLING
        scope.launch {
            repeat(6) {
                delay(220)
                shuffleSeed = it + 1
                Haptics.selection()
            }
            delay(250)
            phase = TarotPhase.PICKING
        }
    }

    fun save() {
        val result = picked.joinToString(" · ") { it.card.name + if (it.reversed) "(逆)" else "" }
        Store.log("tarot", spread.label, result, question.ifEmpty { null })
    }

    fun flip(slot: Int, delayMs: Long = 0) {
        if (slot in flipped) return
        scope.launch {
            delay(delayMs)
            if (slot in flipped) return@launch
            flipped.add(slot)
            Haptics.impact()
            if (flipped.size == picked.size) {
                delay(400)
                if (picked.all { !it.reversed }) confetti++
                save()
            }
        }
    }

    fun pick(entry: DrawnCard) {
        if (phase != TarotPhase.PICKING || picked.size >= spread.positions.size || entry in picked) return
        Haptics.impact()
        picked.add(entry)
        if (picked.size == spread.positions.size) {
            scope.launch {
                delay(700)
                phase = TarotPhase.READING
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (phase) {
            TarotPhase.SETUP -> Screen(title = "塔罗占卜", horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(top = 16.dp)) { TarotBackMini(1.5f) }
                Text("静下心来，想一个问题", style = Fonts.serif(24, bold = true))
                GlassTextField(question, "你想问什么？（可不填）") { question = it }
                for (item in TarotSpread.entries) {
                    val selected = item == spread
                    GlassCard(cornerRadius = 22.dp, padding = 14.dp, onClick = { spread = item }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .then(if (selected) Modifier.background(palette.accentBrush) else Modifier.background(palette.accent.copy(alpha = 0.12f))),
                                contentAlignment = Alignment.Center,
                            ) { Icon(item.icon, null, Modifier.size(20.dp), tint = if (selected) Color.White else palette.accent) }
                            Column(Modifier.weight(1f)) {
                                Text("${item.label} · ${item.positions.size} 张", style = Fonts.headline)
                                SecondaryText(item.detail, style = Fonts.caption)
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = palette.accent)
                        }
                    }
                }
                BigActionButton("开始洗牌", Icons.Rounded.AutoAwesome) { startShuffle() }
            }

            TarotPhase.SHUFFLING, TarotPhase.PICKING -> Screen(
                title = "塔罗占卜",
                scrollable = false,
                horizontalAlignment = Alignment.CenterHorizontally,
                actions = { TextAction("重新开始") { reset() } },
            ) {
                Text(if (phase == TarotPhase.SHUFFLING) "洗牌中…" else "凭直觉抽取 ${spread.positions.size} 张牌", style = Fonts.serif(22, bold = true))
                if (phase == TarotPhase.PICKING) SecondaryText("已选 ${picked.size} / ${spread.positions.size}")
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    spread.positions.forEachIndexed { slot, title ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                Modifier
                                    .size(84.dp, 136.dp)
                                    .border(1.5.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                            ) {
                                if (slot < picked.size) TarotCardBack(Modifier.fillMaxSize())
                            }
                            SecondaryText(title, style = Fonts.caption)
                        }
                    }
                }
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    val width = maxWidth.value
                    deck.forEachIndexed { index, entry ->
                        if (entry !in picked) {
                            FanCard(index, deck.size, width, phase == TarotPhase.SHUFFLING, shuffleSeed) { pick(entry) }
                        }
                    }
                }
            }

            TarotPhase.READING -> Screen(
                title = "塔罗占卜",
                horizontalAlignment = Alignment.CenterHorizontally,
                actions = { TextAction("重新开始") { reset() } },
            ) {
                if (question.isNotEmpty()) SecondaryText("「$question」", style = Fonts.serif(18), textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    picked.forEachIndexed { slot, entry ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val single = spread == TarotSpread.SINGLE
                            FlippableTarotCard(
                                entry,
                                slot in flipped,
                                Modifier
                                    .size(if (single) 180.dp else 106.dp, if (single) 292.dp else 172.dp)
                                    .pressable { flip(slot) },
                            )
                            SecondaryText(spread.positions[slot], style = Fonts.caption.weight(FontWeight.SemiBold))
                        }
                    }
                }
                if (flipped.size < picked.size) {
                    GlassButton("全部翻开", Icons.Rounded.Layers) { picked.indices.forEach { flip(it, it * 250L) } }
                    SecondaryText("点按牌面逐张翻开", style = Fonts.caption)
                } else {
                    picked.forEachIndexed { slot, entry -> TarotInterpretation(spread, slot, entry) }
                    TarotSummary(picked)
                }
            }
        }
        Confetti(confetti)
    }
}

@Composable
private fun FanCard(index: Int, count: Int, width: Float, shuffling: Boolean, seed: Int, onTap: () -> Unit) {
    val t = if (count > 1) index.toFloat() / (count - 1) - 0.5f else 0f
    val rng = SeededGenerator("shuffle", seed, index)
    val tap by rememberUpdatedState(onTap)
    val targetX = if (shuffling) rng.double(-90.0, 90.0).toFloat() else t * width * 0.42f
    val targetY = if (shuffling) rng.double(-30.0, 30.0).toFloat() else abs(t) * 40f
    val targetRotation = if (shuffling) rng.double(-25.0, 25.0).toFloat() else t * 100f
    val spec = spring<Float>(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)
    val x by animateFloatAsState(targetX, spec, label = "x")
    val y by animateFloatAsState(targetY, spec, label = "y")
    val rotation by animateFloatAsState(targetRotation, spec, label = "r")
    TarotCardBack(
        Modifier
            .size(64.dp, 104.dp)
            .graphicsLayer {
                translationX = x * density
                translationY = y * density
                rotationZ = rotation
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .pointerInput(index) { detectTapGestures { tap() } },
    )
}

@Composable
private fun FlippableTarotCard(entry: DrawnCard, faceUp: Boolean, modifier: Modifier) {
    val angle by animateFloatAsState(if (faceUp) 180f else 0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow), label = "flip")
    Box(
        modifier.graphicsLayer {
            rotationY = angle
            cameraDistance = 12 * density
        },
    ) {
        if (angle <= 90f) {
            TarotCardBack(Modifier.fillMaxSize())
        } else {
            TarotCardFace(
                entry.card,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 180f
                        rotationZ = if (entry.reversed) 180f else 0f
                    },
            )
        }
    }
}

@Composable
private fun TarotCardFace(card: TarotCard, modifier: Modifier) {
    val hue = (card.hue % 1.0).toFloat() * 360f
    val colors = listOf(Color.hsv(hue, 0.7f, 0.5f), Color.hsv((hue + 21.6f) % 360f, 0.8f, 0.2f))
    val gold = hex(0xF5D78E)
    BoxWithConstraints(modifier) {
        val w = maxWidth.value
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Brush.verticalGradient(colors), cornerRadius = CornerRadius(size.width * 0.09f))
            val rng = SeededGenerator("tarot-face", card.id)
            repeat(30) {
                val r = rng.double(0.4, 1.4).toFloat() * density
                drawCircle(Color.White.copy(alpha = rng.double(0.3, 0.9).toFloat()), r, Offset(rng.unit().toFloat() * size.width, rng.unit().toFloat() * size.height))
            }
            val inset = size.width * 0.05f
            drawRoundRect(gold, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(size.width * 0.07f), style = Stroke(maxOf(density, size.width * 0.012f)))
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = (w * 0.12f).dp, horizontal = (w * 0.08f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(card.roman, style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = (w * 0.11f).sp, color = gold))
            Spacer(Modifier.weight(1f))
            Text(tarotEmoji(card.symbol), style = TextStyle(fontSize = (w * 0.3f).sp))
            Spacer(Modifier.weight(1f))
            Text(card.name, style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = (w * 0.13f).sp, color = Color.White))
            Text(card.en.uppercase(), style = TextStyle(fontFamily = FontFamily.Serif, fontSize = (w * 0.055f).sp, color = gold.copy(alpha = 0.9f)), maxLines = 1)
        }
    }
}

@Composable
private fun TarotInterpretation(spread: TarotSpread, slot: Int, entry: DrawnCard) {
    val palette = LocalPalette.current
    val card = entry.card
    val extra: Pair<String, String>? = when {
        spread == TarotSpread.LOVE -> "感情" to card.love
        spread == TarotSpread.ADVICE && slot == 2 -> "建议" to card.advice
        spread == TarotSpread.SINGLE -> "建议" to card.advice
        else -> null
    }
    GlassCard(spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                spread.positions[slot],
                style = Fonts.caption.weight(FontWeight.Bold),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            Text("${card.roman} · ${card.name}", style = Fonts.headline)
            Text(
                if (entry.reversed) "逆位" else "正位",
                style = Fonts.caption.weight(FontWeight.SemiBold),
                color = if (entry.reversed) hex(0xFB7185) else hex(0x34D399),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { for (k in card.keywords) TagChip(k) }
        Text(if (entry.reversed) card.reversed else card.upright, style = Fonts.subheadline)
        SecondaryText(extra?.let { "${it.first}：${it.second}" } ?: "事业：${card.career}")
    }
}

@Composable
private fun TarotSummary(picked: List<DrawnCard>) {
    val upright = picked.count { !it.reversed }
    val text = when {
        picked.size == 1 -> if (picked[0].reversed) "逆位提醒你放慢脚步，先处理内心的阻碍。" else "正位的能量清晰明亮，跟着这张牌的指引行动吧。"
        upright == picked.size -> "全部为正位，整体能量顺畅积极，是推进事情的好时机。"
        upright == 0 -> "全部为逆位，当下阻力较多，宜先调整心态、等待时机。"
        upright > picked.size / 2 -> "正位居多，整体趋势向好，留意逆位牌提示的小问题即可。"
        else -> "逆位居多，事情尚有阻碍，不妨换个角度重新审视。"
    }
    GlassCard(spacing = 8.dp) {
        SectionHeader("综合解读", icon = Icons.Rounded.AutoAwesome)
        Text(text, style = Fonts.body)
        SecondaryText("塔罗是一面映照内心的镜子，答案最终在你自己手中。", style = Fonts.caption)
    }
}

// endregion

// region 答案之书

@Composable
fun AnswerBookScreen() {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    var isOpen by remember { mutableStateOf(false) }
    var pressing by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    val recent = remember { mutableStateListOf<String>() }
    val hold = remember { Animatable(0f) }
    val cover by animateFloatAsState(if (isOpen) 165f else 0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "cover")

    fun open() {
        if (isOpen) return
        answer = TrueRandom.pick(ContentLibrary.answers)
        pressing = false
        isOpen = true
        Haptics.success()
        recent.add(0, answer)
        if (recent.size > 5) recent.removeAt(recent.size - 1)
        Store.log("answers", question, answer)
    }

    fun close() {
        isOpen = false
        question = ""
        scope.launch { hold.snapTo(0f) }
    }

    OnShake(enabled = !isOpen) { open() }

    Screen(title = "答案之书", horizontalAlignment = Alignment.CenterHorizontally, spacing = 22.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (isOpen) "这是宇宙给你的答案" else "心中默念一个问题", style = Fonts.serif(24, bold = true))
            SecondaryText(if (isOpen) "答案只是提示，选择权永远在你" else "最好是能用「是」或「否」回答的问题")
        }
        GlassTextField(question, "写下问题（可选）", enabled = !isOpen) { question = it }

        val bookScale by animateFloatAsState(if (pressing) 1.04f else 1f, label = "book")
        Box(
            Modifier
                .size(260.dp, 340.dp)
                .graphicsLayer { scaleX = bookScale; scaleY = bookScale }
                .pointerInput(isOpen) {
                    if (isOpen) return@pointerInput
                    detectTapGestures(onPress = {
                        pressing = true
                        Haptics.tap()
                        val job = scope.launch {
                            hold.animateTo(1f, tween(1800, easing = LinearEasing))
                            open()
                        }
                        tryAwaitRelease()
                        if (!isOpen) {
                            job.cancel()
                            pressing = false
                            scope.launch { hold.animateTo(0f, tween(300)) }
                        }
                    })
                },
        ) {
            // 内页
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(hex(0xFFF8E7), hex(0xF3E3C3)))),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                Text("✦", color = hex(0xC08A2E), style = Fonts.title3)
                Text(
                    answer,
                    style = Fonts.serif(26, bold = true),
                    color = hex(0x4A2C0A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .alpha(if (cover > 90f) 1f else 0f),
                )
                Text("✦", color = hex(0xC08A2E), style = Fonts.title3)
            }
            // 封面
            if (cover < 90f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            rotationY = -cover
                            cameraDistance = 10 * density
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(hex(0x7C4A12), hex(0x3B2006))))
                        .padding(14.dp)
                        .border(2.dp, hex(0xF5D78E, 0.8f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Icon(Icons.Rounded.NightsStay, null, Modifier.size(54.dp), tint = hex(0xF5B83D))
                        Text("答案之书", style = Fonts.serif(30, bold = true), color = hex(0xF5D78E))
                        Text("THE BOOK OF ANSWERS", style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold), color = hex(0xF5D78E, 0.7f))
                    }
                }
            }
        }

        if (isOpen) {
            GlassButton("再问一次", Icons.Rounded.Refresh) { close() }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(70.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 6.dp.toPx()
                        val inset = stroke / 2
                        val arc = Size(size.width - stroke, size.height - stroke)
                        drawArc(palette.subtle(0.1f), 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                        drawArc(
                            Brush.verticalGradient(listOf(hex(0xFFE7A3), hex(0xF59E0B))),
                            -90f, 360f * hold.value, false, Offset(inset, inset), arc,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                    Icon(Icons.Rounded.TouchApp, null, Modifier.size(28.dp), tint = hex(0xF5B83D))
                }
                SecondaryText(if (pressing) "专注于你的问题…" else "按住书本，默念问题", style = Fonts.footnote)
            }
        }

        if (recent.isNotEmpty()) {
            GlassCard(spacing = 8.dp) {
                SectionHeader("刚刚的答案", icon = Icons.Rounded.History)
                for (item in recent) SecondaryText("· $item")
            }
        }
    }
}

// endregion

@Composable
fun GlassTextField(value: String, placeholder: String, enabled: Boolean = true, singleLine: Boolean = true, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    val palette = LocalPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = singleLine,
        placeholder = { Text(placeholder, color = palette.tertiaryText) },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.glassFill,
            unfocusedContainerColor = palette.glassFill,
            disabledContainerColor = palette.glassFill,
            focusedBorderColor = palette.accent,
            unfocusedBorderColor = palette.glassStroke,
            disabledBorderColor = palette.glassStroke,
            focusedTextColor = palette.text,
            unfocusedTextColor = palette.text,
            disabledTextColor = palette.secondaryText,
            cursorColor = palette.accent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
