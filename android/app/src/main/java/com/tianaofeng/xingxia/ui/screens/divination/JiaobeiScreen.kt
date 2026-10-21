package com.tianaofeng.xingxia.ui.screens.divination

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneStick
import com.tianaofeng.xingxia.core.TrueRandom
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Prefs
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.Confetti
import com.tianaofeng.xingxia.ui.components.CosmicBackdrop
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.OnShake
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SoundEffect
import com.tianaofeng.xingxia.ui.components.SoundEngine
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.isReduceMotion
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.common.stickLevelInk
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToLong

// region 结果与记录

/** 一次掷筊的结果。两片筊杯各自有一半机会平面朝上：圣杯 50%、笑杯 25%、阴杯 25%（与 iOS 一致） */
internal enum class JiaobeiResult(val label: String, val meaning: String) {
    /** 一平一凸：神明认可 */
    SHENG("圣杯", "一平一凸，神明认可"),

    /** 两面皆平：神明笑而不答，可以再掷 */
    XIAO("笑杯", "两面皆平，神明笑而不答"),

    /** 两面皆凸：神明未允 */
    YIN("阴杯", "两面皆凸，神明未允");

    fun color(palette: ThemePalette): Color = when (this) {
        SHENG -> palette.positive
        XIAO -> palette.caution
        YIN -> palette.negative
    }

    companion object {
        fun of(leftFlat: Boolean, rightFlat: Boolean): JiaobeiResult = when {
            leftFlat != rightFlat -> SHENG
            leftFlat -> XIAO
            else -> YIN
        }
    }
}

/** 记下哪些签已经掷筊确认（按日期、求签对象、签号），签面据此盖「已确认」小印。只留最近的一批，键名与 iOS 相同 */
internal object JiaobeiLedger {
    private const val LIMIT = 120
    private val confirmed by lazy { Prefs.stringList("jiaobeiConfirmed", emptyList()) }

    fun entry(day: DayKey, owner: String?, stick: Int): String = "${day.rawValue}|${owner?.lowercase() ?: "me"}|$stick"

    fun isConfirmed(entry: String): Boolean = entry in confirmed.value

    fun markConfirmed(entry: String) {
        val list = confirmed.value
        if (entry in list) return
        confirmed.value = (list + entry).takeLast(LIMIT)
    }
}

// endregion

// region 掷筊

private enum class JiaobeiOutcome { PENDING, CONFIRMED, DENIED }

/** 连得几次圣杯才算确认 */
private const val JIAOBEI_NEEDED = 3

/** 两片筊杯在舞台上的横向位置、抛起高度、落点漂移（单位 dp，舞台 320 × 300，与 iOS 一致） */
private val PIECE_X = floatArrayOf(-72f, 72f)
private val PIECE_HEIGHT = floatArrayOf(160f, 138f)
private val PIECE_DRIFT = floatArrayOf(-20f, 24f)

/** 绕水平轴翻转的累计角度：0° 凸面朝上，每翻过半圈换一面 */
private fun isFlatUp(spin: Float): Boolean = Math.floorMod(floor((spin + 90.0) / 180.0).toLong(), 2L) == 1L

/** 落地时的累计角度：从当前的整半圈起再多翻几圈，停在要的那一面 */
private fun landingSpin(from: Float, flat: Boolean, extraTurns: Int): Float {
    val base = (from / 180.0).roundToLong() * 180f + extraTurns * 360f
    return if (isFlatUp(base) == flat) base else base + 180f
}

/**
 * 掷筊确认：一次掷两片筊杯，连续三次圣杯这支签才算得到确认；
 * 笑杯时圣杯重新计数、可以再掷；阴杯表示神明未允，可以重新求一签（不计入今日一签）。
 * onConfirmed 在连得三次圣杯时调用；onRedraw 是得阴杯后选了「重新求一签」，由求签页关掉这里再处理。
 */
@Composable
internal fun JiaobeiDialog(stick: FortuneStick, forName: String?, onConfirmed: () -> Unit, onRedraw: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    // 一开始左凸右平，摆成圣杯的样子
    val spins = remember { listOf(Animatable(0f), Animatable(180f)) }
    val turns = remember { listOf(Animatable(-14f), Animatable(12f)) }
    /** 0 在桌面上，1 抛到最高处 */
    val lift = remember { Animatable(0f) }
    /** 落地时压扁一下 */
    val squash = remember { Animatable(0f) }
    /** 减弱动态效果时用淡出淡入代替抛起 */
    val stageAlpha = remember { Animatable(1f) }
    var throwing by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }
    var last by remember { mutableStateOf<JiaobeiResult?>(null) }
    var outcome by remember { mutableStateOf(JiaobeiOutcome.PENDING) }
    /** 本轮每一掷的结果，写进决策历史 */
    val log = remember { mutableStateListOf<JiaobeiResult>() }
    var confetti by remember { mutableIntStateOf(0) }
    var flow by remember { mutableStateOf<Job?>(null) }

    fun announce(text: String) = view.announceForAccessibility(text)

    /** 落地声之后再放结果的音效和震动，免得糊在一起 */
    fun feedback(afterMs: Long, action: () -> Unit) {
        scope.launch {
            delay(afterMs)
            action()
        }
    }

    /** 一轮结束（得到确认或阴杯）时写一条决策历史 */
    fun record(confirmed: Boolean) {
        val title = (forName?.let { "为$it · " } ?: "") + stick.numberText
        val result = if (confirmed) "连得三次圣杯，签已确认" else "阴杯，神明未允"
        Store.log("jiaobei", title, result, log.joinToString("、") { it.label })
    }

    fun land(result: JiaobeiResult) {
        log.add(result)
        throwing = false
        last = result
        // 两片先后落在桌上
        SoundEngine.play(SoundEffect.STICK_DROP, 0.7f)
        Haptics.heavy()
        feedback(90) { SoundEngine.play(SoundEffect.DICE_HIT) }
        when (result) {
            JiaobeiResult.SHENG -> {
                val count = streak + 1
                streak = count
                if (count >= JIAOBEI_NEEDED) {
                    outcome = JiaobeiOutcome.CONFIRMED
                    record(confirmed = true)
                    onConfirmed()
                    announce("圣杯。连得三次圣杯，这支签已得到确认")
                    feedback(280) {
                        confetti++
                        SoundEngine.play(SoundEffect.SUCCESS)
                        Haptics.success()
                    }
                } else {
                    announce("圣杯，已连得 $count 次，再得 ${JIAOBEI_NEEDED - count} 次即可确认")
                    feedback(250) {
                        SoundEngine.play(SoundEffect.CHIME, 0.6f)
                        Haptics.impact()
                    }
                }
            }
            JiaobeiResult.XIAO -> {
                streak = 0
                announce("笑杯，神明笑而不答，圣杯重新计数，可以继续掷")
                feedback(250) {
                    SoundEngine.play(SoundEffect.POP)
                    Haptics.tap()
                }
            }
            JiaobeiResult.YIN -> {
                streak = 0
                outcome = JiaobeiOutcome.DENIED
                record(confirmed = false)
                announce("阴杯，神明未允，可以重新求一签，不计入今日一签")
                feedback(250) {
                    SoundEngine.play(SoundEffect.FAIL)
                    Haptics.error()
                }
            }
        }
    }

    fun throwCups() {
        if (outcome != JiaobeiOutcome.PENDING || throwing) return
        throwing = true
        val flats = listOf(TrueRandom.bool(), TrueRandom.bool())
        val result = JiaobeiResult.of(flats[0], flats[1])
        val spinTargets = spins.indices.map { landingSpin(spins[it].value, flats[it], TrueRandom.int(2..3)) }
        val turnTargets = turns.indices.map { (-26.0 + TrueRandom.double() * 52.0).toFloat() }
        SoundEngine.play(SoundEffect.WHOOSH)
        Haptics.impact()
        flow?.cancel()
        flow = scope.launch {
            val reduceMotion = isReduceMotion()
            if (reduceMotion) {
                stageAlpha.animateTo(0f, tween(180))
                delay(20)
                spins.forEachIndexed { i, spin -> spin.snapTo(spinTargets[i]) }
                turns.forEachIndexed { i, turn -> turn.snapTo(turnTargets[i]) }
                launch { stageAlpha.animateTo(1f, tween(300)) }
            } else {
                // 抛起、翻转、落地：翻转跟着整段飞行走完，落地时正好停在结果那一面
                spins.forEachIndexed { i, spin -> launch { spin.animateTo(spinTargets[i], tween(800, easing = LinearOutSlowInEasing)) } }
                turns.forEachIndexed { i, turn -> launch { turn.animateTo(turnTargets[i], tween(800, easing = LinearOutSlowInEasing)) } }
                lift.animateTo(1f, tween(420, easing = LinearOutSlowInEasing))
                lift.animateTo(0f, tween(380, easing = FastOutLinearInEasing))
                squash.animateTo(1f, tween(60))
            }
            land(result)
            if (!reduceMotion) {
                delay(60)
                squash.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
            }
        }
    }

    fun restart() {
        outcome = JiaobeiOutcome.PENDING
        streak = 0
        last = null
        log.clear()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        OnShake(enabled = outcome == JiaobeiOutcome.PENDING && !throwing) { throwCups() }
        // 全屏弹层自己的星空背景，里面的玻璃模糊它（不是被盖住的主窗口）
        CosmicBackdrop {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                ) {
                    CircleIconButton(Icons.Rounded.Close, "关闭", Modifier.align(Alignment.CenterStart), onClick = onDismiss)
                    Text(
                        "掷筊确认",
                        style = Fonts.headline,
                        color = palette.text,
                        maxLines = 1,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics { heading() },
                    )
                }
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    val stageScale = minOf(1f, (maxWidth.value - 32f) / 320f).coerceAtLeast(0.5f)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight)
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween,
                    ) {
                        JiaobeiHeader(stick, forName, streak)
                        JiaobeiStage(
                            scale = stageScale,
                            spins = { i -> spins[i].value },
                            turns = { i -> turns[i].value },
                            lift = { lift.value },
                            squash = { squash.value },
                            alpha = { stageAlpha.value },
                            throwing = throwing,
                            last = last,
                        )
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (outcome) {
                                JiaobeiOutcome.PENDING -> {
                                    val hint = when (last) {
                                        JiaobeiResult.SHENG -> "再得 ${JIAOBEI_NEEDED - streak} 次圣杯即可确认"
                                        JiaobeiResult.XIAO -> "笑杯不算数，圣杯重新计数，可以继续掷"
                                        else -> null
                                    }
                                    if (hint != null) SecondaryText(hint, textAlign = TextAlign.Center)
                                    BigActionButton(
                                        if (throwing) "掷筊中…" else if (log.isEmpty()) "掷筊" else "再掷一次",
                                        Icons.Rounded.AutoAwesome,
                                        enabled = !throwing,
                                    ) { throwCups() }
                                }
                                JiaobeiOutcome.CONFIRMED -> {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Rounded.Verified, null, Modifier.size(20.dp), tint = palette.positive)
                                        Text("连得三次圣杯，这支签已得到确认", style = Fonts.headline, color = palette.positive, textAlign = TextAlign.Center)
                                    }
                                    BigActionButton("完成", Icons.Rounded.Check, onClick = onDismiss)
                                }
                                JiaobeiOutcome.DENIED -> {
                                    Text("神明未允，可以重新求一签（不计入今日一签）", style = Fonts.headline, color = palette.negative, textAlign = TextAlign.Center)
                                    BigActionButton("重新求一签", Icons.Rounded.Refresh, onClick = onRedraw)
                                    GlassButton("再掷一轮", Icons.Rounded.Replay) { restart() }
                                }
                            }
                            if (log.isNotEmpty()) {
                                val shown = log.takeLast(10).joinToString(" · ") { it.label }
                                SecondaryText(
                                    "本轮 ${log.size} 掷：" + (if (log.size > 10) "… " else "") + shown,
                                    style = Fonts.caption,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
            Confetti(confetti)
        }
    }
}

@Composable
private fun JiaobeiHeader(stick: FortuneStick, forName: String?, streak: Int) {
    val palette = LocalPalette.current
    Column(
        Modifier.padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            Modifier.semantics(mergeDescendants = true) { },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (forName != null) {
                SecondaryText("为 $forName 掷筊", style = Fonts.subheadline.weight(FontWeight.SemiBold), maxLines = 1)
            }
            Text(
                "${stick.numberText}「${stick.title}」",
                style = Fonts.serif(22, bold = true),
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(stick.levelDisplayName, style = Fonts.subheadline.weight(FontWeight.SemiBold), color = stickLevelInk(stick.level, palette.isDark))
        }
        SecondaryText("心中默念所求，一次掷两片筊杯，连得三次圣杯，这支签才算得到确认", style = Fonts.footnote, textAlign = TextAlign.Center)
        // 连续圣杯计数
        Row(
            Modifier
                .glass(CircleShape)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .clearAndSetSemantics { contentDescription = "已连得圣杯 $streak 次，共需 $JIAOBEI_NEEDED 次" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(JIAOBEI_NEEDED) { index ->
                    val fill = if (index < streak) palette.identity(hex(0xE53935)) else palette.subtle(0.15f)
                    Canvas(Modifier.size(26.dp, 16.dp)) { drawPath(jiaobeiPath(size), fill) }
                }
            }
            Text("圣杯 $streak/$JIAOBEI_NEEDED", style = Fonts.headline, color = palette.text)
        }
    }
}

/**
 * 舞台：结果文字、两片筊杯和桌上的影子。按 320 × 300 的设计尺寸排，窄屏整体缩小；
 * 字号封顶，免得大字号下结果文字压到筊杯上（结果另有读屏播报和下方提示）。动画值用 lambda 传入，只在绘制阶段读取
 */
@Composable
private fun JiaobeiStage(
    scale: Float,
    spins: (Int) -> Float,
    turns: (Int) -> Float,
    lift: () -> Float,
    squash: () -> Float,
    alpha: () -> Float,
    throwing: Boolean,
    last: JiaobeiResult?,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val sides = (0..1).map { if (isFlatUp(spins(it))) "平面" else "凸面" }
    val state = "两片筊杯，左边${sides[0]}朝上，右边${sides[1]}朝上"
    val label = last?.let { "${it.label}，${it.meaning}。$state" } ?: state
    Box(
        Modifier
            .size((320 * scale).dp, (300 * scale).dp)
            .clearAndSetSemantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.5f))) {
            Box(
                Modifier
                    .requiredSize(320.dp, 300.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha()
                    },
                contentAlignment = Alignment.Center,
            ) {
                // 结果
                Column(
                    Modifier.graphicsLayer {
                        translationY = -96f * this.density
                        this.alpha = if (throwing) 0f else 1f
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(last?.label ?: "请掷筊", style = Fonts.serif(34, bold = true), color = last?.color(palette) ?: palette.text)
                    SecondaryText(
                        last?.meaning ?: if (AppSettings.shakeEnabled) "点按下方按钮，或摇一摇手机" else "点按下方按钮",
                        maxLines = 1,
                    )
                }

                // 桌面上的影子：抛得越高越小越淡
                Canvas(Modifier.requiredSize(320.dp, 300.dp)) {
                    val up = lift()
                    val unit = this.density
                    for (index in 0..1) {
                        val w = 110f * (1 - up * 0.45f) * unit
                        val h = 18f * (1 - up * 0.45f) * unit
                        val cx = size.width / 2 + (PIECE_X[index] + PIECE_DRIFT[index] * up) * unit
                        val cy = size.height / 2 + 92f * unit
                        val shade = Color.Black.copy(alpha = 0.28f * (1 - up * 0.7f))
                        // 压扁的圆形渐变当作模糊的椭圆影子
                        scale(scaleX = 1f, scaleY = h / w, pivot = Offset(cx, cy)) {
                            drawCircle(Brush.radialGradient(listOf(shade, shade.copy(alpha = 0f)), Offset(cx, cy), w / 2), w / 2, Offset(cx, cy))
                        }
                    }
                }

                for (index in 0..1) {
                    JiaobeiPiece(
                        spin = { spins(index) },
                        modifier = Modifier
                            .size(124.dp, 76.dp)
                            .graphicsLayer {
                                val up = lift()
                                translationX = (PIECE_X[index] + PIECE_DRIFT[index] * up) * this.density
                                translationY = (52f - PIECE_HEIGHT[index] * up) * this.density
                                rotationZ = turns(index)
                            }
                            .graphicsLayer {
                                val s = squash()
                                scaleX = 1 + 0.06f * s
                                scaleY = 1 - 0.1f * s
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            },
                    )
                }

                // 每片朝上的是哪一面
                for (index in 0..1) {
                    SecondaryText(
                        sides[index],
                        style = Fonts.caption.weight(FontWeight.Medium),
                        modifier = Modifier.graphicsLayer {
                            translationX = PIECE_X[index] * this.density
                            translationY = 122f * this.density
                            this.alpha = if (throwing) 0f else 1f
                        },
                    )
                }
            }
        }
    }
}

/** 一片筊杯：绕水平轴翻转，翻到侧立（90°）时才换面 */
@Composable
private fun JiaobeiPiece(spin: () -> Float, modifier: Modifier) {
    val palette = LocalPalette.current
    val tones = remember(palette) { JiaobeiTones(palette) }
    Canvas(
        modifier.graphicsLayer {
            rotationX = spin()
            cameraDistance = 10f * density
        },
    ) {
        if (isFlatUp(spin())) {
            // 平面那一面是翻过半圈才看到的，先上下倒一次抵消
            scale(scaleX = 1f, scaleY = -1f) { drawJiaobeiFace(flat = true, tones) }
        } else {
            drawJiaobeiFace(flat = false, tones)
        }
    }
}

/** 筊杯保留传统朱漆红，但向主题强调色靠一点 */
private class JiaobeiTones(palette: ThemePalette) {
    val flat = palette.identity(listOf(hex(0xDB4A3E), hex(0xB3261E)))
    val flatRim = palette.identity(hex(0xFFD6C9)).copy(alpha = 0.55f)
    val dome = palette.identity(listOf(hex(0xF0564A), hex(0xB71C1C), hex(0x6B0F0F)))
    val outline = palette.identity(hex(0x5A0B0B)).copy(alpha = 0.7f)
}

/** 筊杯的一面：凸面漆色深、带一道高光；平面颜色平匀，带一圈浅色描边 */
private fun DrawScope.drawJiaobeiFace(flat: Boolean, tones: JiaobeiTones) {
    val w = size.width
    val h = size.height
    val path = jiaobeiPath(size)
    val line = maxOf(density, w * 0.012f)
    clipPath(path) {
        if (flat) {
            drawPath(path, Brush.verticalGradient(tones.flat))
            translate(top = h * 0.05f) {
                scale(0.82f) { drawPath(path, tones.flatRim, style = Stroke(line)) }
            }
        } else {
            drawPath(path, Brush.verticalGradient(tones.dome))
            // 高光：压扁的圆形渐变
            val center = Offset(w * 0.5f, h * 0.6f)
            val radius = w * 0.21f
            scale(scaleX = 1f, scaleY = (h * 0.13f) / (w * 0.42f), pivot = center) {
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0f)), center, radius), radius, center)
            }
        }
        // 描边一半落在裁切外，看到的宽度与 iOS 相同
        drawPath(path, tones.outline, style = Stroke(line))
    }
}

/** 筊杯的轮廓：月牙形，两角朝上（与 iOS 的 JiaobeiShape 相同，二次曲线换成等价的三次曲线） */
private fun jiaobeiPath(size: Size): Path {
    val w = size.width
    val h = size.height
    fun x(f: Float) = w * f
    fun y(f: Float) = h * f
    return Path().apply {
        moveTo(x(0.04f), y(0.24f))
        // 外弧
        cubicTo(x(0.06f), y(1.25f), x(0.94f), y(1.25f), x(0.96f), y(0.24f))
        // 右角收圆：从 (0.96, 0.24) 经控制点 (1.0, 0.1) 到 (0.88, 0.14)
        cubicTo(x(0.96f + 0.04f * 2 / 3), y(0.24f - 0.14f * 2 / 3), x(0.88f + 0.12f * 2 / 3), y(0.14f - 0.04f * 2 / 3), x(0.88f), y(0.14f))
        // 内弧
        cubicTo(x(0.7f), y(0.52f), x(0.3f), y(0.52f), x(0.12f), y(0.14f))
        // 左角收圆：从 (0.12, 0.14) 经控制点 (0.0, 0.1) 到 (0.04, 0.24)
        cubicTo(x(0.12f - 0.12f * 2 / 3), y(0.14f - 0.04f * 2 / 3), x(0.04f - 0.04f * 2 / 3), y(0.24f - 0.14f * 2 / 3), x(0.04f), y(0.24f))
        close()
    }
}

// endregion

// region 已确认小印

/** 掷筊连得三次圣杯后盖在签上的「已确认」小印（签诗页和分享卡共用；印章红向主题强调色靠一点，分享卡里跟分享卡的主题走） */
@Composable
fun JiaobeiSeal(size: Int = 13, modifier: Modifier = Modifier) {
    val red = LocalPalette.current.identity(hex(0xE53935))
    val shape = RoundedCornerShape((size * 0.35f).dp)
    Text(
        "已确认",
        style = Fonts.serif(size, bold = true),
        color = red,
        maxLines = 1,
        modifier = modifier
            .graphicsLayer { alpha = 0.9f }
            .clip(shape)
            .background(red.copy(alpha = 0.1f))
            .border(maxOf(1.2f, size * 0.11f).dp, red, shape)
            .padding(horizontal = (size * 0.55f).dp, vertical = (size * 0.3f).dp)
            .clearAndSetSemantics { contentDescription = "已掷筊确认" },
    )
}

// endregion
