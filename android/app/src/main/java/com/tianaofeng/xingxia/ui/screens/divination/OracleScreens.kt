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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.ListAlt
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WineBar
import androidx.compose.material.icons.rounded.CropPortrait
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneStick
import com.tianaofeng.xingxia.core.SeededGenerator
import com.tianaofeng.xingxia.core.TarotCard
import com.tianaofeng.xingxia.core.TarotSuit
import com.tianaofeng.xingxia.core.TrueRandom
import com.tianaofeng.xingxia.core.chineseNumber
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Profile
import com.tianaofeng.xingxia.data.StickDraw
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.data.TarotReading
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.ChipRow
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.Confetti
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.FavoriteToggleButton
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.SoundEffect
import com.tianaofeng.xingxia.ui.components.SoundEngine
import com.tianaofeng.xingxia.ui.components.OnShake
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.SelectChip
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.isReduceMotion
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.starPath
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.common.StickTubeIllustration
import com.tianaofeng.xingxia.ui.screens.common.TarotBackMini
import com.tianaofeng.xingxia.ui.screens.common.TarotCardBack
import com.tianaofeng.xingxia.ui.screens.common.stickLevelColor
import com.tianaofeng.xingxia.ui.screens.common.stickLevelInk
import com.tianaofeng.xingxia.ui.screens.tools.FavoriteTool
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

// region 求签

private enum class StickPhase { IDLE, SHAKING, DROPPED, REVEALED }

/** 某人今天求过的签；profileId 为 null 表示没有档案时的「自己」 */
private fun todayStickDraw(profileId: String?): StickDraw? =
    Store.stickDraws.items.lastOrNull { it.dayKey == DayKey.today.rawValue && it.profileId == profileId }

/** 随机抽一支签，excluding 是要避开的签号（掷筊被否定的那一支）；按签库实际的支数取 */
private fun randomStick(excluding: Int?): FortuneStick {
    val pool = ContentLibrary.sticks.filter { it.n != excluding }
    return TrueRandom.pick(pool.ifEmpty { ContentLibrary.sticks })
}

/** 读屏播报用，例如「第 12 签，上上签，云开见月」（与 iOS 的 spokenText 一致） */
private val FortuneStick.spokenText: String get() = "第 $n 签，$levelDisplayName，$title"

@Composable
fun StickScreen() {
    val palette = LocalPalette.current
    val view = LocalView.current

    // 流程状态用 rememberSaveable：去添加家人朋友的档案、切标签再回来时，求签进度和「为谁求」都还在，
    // 今天的签也只在第一次进入时自动揭示
    var phase by rememberSaveable { mutableStateOf(if (todayStickDraw(Store.primary?.id) != null) StickPhase.REVEALED else StickPhase.IDLE) }
    var stickNumber by rememberSaveable { mutableStateOf(todayStickDraw(Store.primary?.id)?.stickNumber) }
    val stick = stickNumber?.let { ContentLibrary.stick(it) }
    var wish by rememberSaveable { mutableStateOf("综合") }
    // 为谁求签（档案 id）；null 表示当前档案自己
    var targetId by rememberSaveable { mutableStateOf<String?>(null) }
    val target = targetId?.let { id -> Store.profiles.items.firstOrNull { it.id == id } }
    var choosingProfile by rememberSaveable { mutableStateOf(false) }
    // 掷筊得阴杯后重新求签的那一天（yyyyMMdd）：这时求得的签只看不记，不计入今日一签。null 是正常的今日一签
    var extraDay by rememberSaveable { mutableStateOf<Int?>(null) }
    // 被神明否定的那一支，重新求签时避开
    var deniedNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var energy by remember { mutableFloatStateOf(0f) }
    var wobble by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    var confetti by remember { mutableIntStateOf(0) }
    var lastRattle by remember { mutableStateOf(0L) }
    val wishes = listOf("综合", "事业", "感情", "财运", "健康", "学业", "出行")

    fun rattle() {
        lastRattle = System.currentTimeMillis()
        SoundEngine.play(SoundEffect.STICK_RATTLE, 0.8f)
        Haptics.selection()
    }

    fun reveal() {
        // 签和「解签」按钮都能触发，连点时只解一次，免得重复写记录
        if (phase != StickPhase.DROPPED) return
        val s = stick ?: return
        phase = StickPhase.REVEALED
        SoundEngine.play(SoundEffect.CHIME)
        Haptics.success()
        if (s.isLucky) confetti++
        // 每人每天一签：记在求签对象（自己或选中的家人朋友）名下；掷筊后重新求的签不计入
        val owner = targetId ?: Store.primary?.id
        if (extraDay == null && todayStickDraw(owner) == null) {
            Store.stickDraws.add(StickDraw(DayKey.today.rawValue, owner, s.n))
        }
        // 「综合」是默认所求，不写进标题，免得出现「求综合」；为别人求的写「为某某」
        val who = target?.let { "为${it.name}" } ?: ""
        val base = if (wish == "综合") who else "${who}求$wish"
        val title = if (extraDay == null) base else if (base.isEmpty()) "重新求签" else "$base · 重新求签"
        Store.log("stick", title, "${s.numberText} ${s.levelDisplayName} · ${s.title}")
        view.announceForAccessibility((target?.let { "为${it.name}求得，" } ?: "") + s.spokenText)
    }

    /** 回到摇签前的状态；这个人今天已经求过就直接显示那一支 */
    fun startFor(profileId: String?) {
        targetId = profileId
        wish = "综合"
        extraDay = null
        deniedNumber = null
        energy = 0f
        wobble = 0f
        holding = false
        val existing = todayStickDraw(profileId ?: Store.primary?.id)
        stickNumber = existing?.stickNumber
        phase = if (existing != null) StickPhase.REVEALED else StickPhase.IDLE
        if (existing != null && profileId != null) {
            val name = Store.profiles.items.firstOrNull { it.id == profileId }?.name
            if (name != null) view.announceForAccessibility("${name}今天已经求过，" + ContentLibrary.stick(existing.stickNumber).spokenText)
        }
    }

    /**
     * 进入页面、回到前台时对一下今天的记录（按当前求签对象）：今天已经求过就显示那一支（别的地方求的、换了当前档案也算）；
     * 显示的签已不是今天的（跨过零点）就回到摇签。落地还没解的签不动；重新求签只在当天有效
     */
    fun syncWithToday() {
        if (phase == StickPhase.DROPPED) return
        val extra = extraDay
        if (extra != null) {
            if (extra == DayKey.today.rawValue) return
            extraDay = null
            deniedNumber = null
        }
        val existing = todayStickDraw(targetId ?: Store.primary?.id)
        if (existing != null) {
            if (phase == StickPhase.REVEALED && stickNumber == existing.stickNumber) return
            stickNumber = existing.stickNumber
            energy = 0f
            holding = false
            phase = StickPhase.REVEALED
        } else if (phase == StickPhase.REVEALED) {
            stickNumber = null
            energy = 0f
            wobble = 0f
            phase = StickPhase.IDLE
        }
    }

    /** 掷筊得阴杯后重新求一签：所求和求签对象不变，求得的签不计入今日一签 */
    fun redraw() {
        deniedNumber = stickNumber
        extraDay = DayKey.today.rawValue
        stickNumber = null
        energy = 0f
        wobble = 0f
        holding = false
        phase = StickPhase.IDLE
        view.announceForAccessibility("重新求一签，这次求得的签不计入今日一签")
    }

    /** 不再重新求签，回到当前求签对象的今日一签 */
    fun leaveExtra() {
        extraDay = null
        deniedNumber = null
        energy = 0f
        wobble = 0f
        holding = false
        val existing = todayStickDraw(targetId ?: Store.primary?.id)
        stickNumber = existing?.stickNumber
        phase = if (existing != null) StickPhase.REVEALED else StickPhase.IDLE
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { syncWithToday() }

    OnShake(enabled = !choosingProfile && (phase == StickPhase.IDLE || phase == StickPhase.SHAKING)) {
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
                // 重新求签只在当天有效：页面一直开着跨过零点时，这一支按新一天的今日一签算
                val extra = extraDay
                if (extra != null && extra != DayKey.today.rawValue) {
                    extraDay = null
                    deniedNumber = null
                }
                stickNumber = randomStick(if (extraDay != null) deniedNumber else null).n
                holding = false
                phase = StickPhase.DROPPED
            }
        }
    }

    // 落地反馈单独放：上面的循环以 phase 为 key，切到 DROPPED 时就被取消了
    LaunchedEffect(phase) {
        if (phase == StickPhase.DROPPED) {
            delay(350)
            SoundEngine.play(SoundEffect.STICK_DROP)
            Haptics.heavy()
        }
    }

    Box(Modifier.fillMaxSize()) {
        val current = stick
        if (choosingProfile) {
            StickProfilePicker(onBack = { choosingProfile = false }) { profile ->
                choosingProfile = false
                startFor(profile.id)
            }
        } else if (phase == StickPhase.REVEALED && current != null) {
            // 换了一支签、换了人或重新求签就当新页面，重新盖章
            key(current.n, targetId, extraDay != null) {
                StickResult(
                    current,
                    wish,
                    forName = target?.let { "${it.avatar} ${it.name}" },
                    targetName = target?.name,
                    ownerId = targetId ?: Store.primary?.id,
                    isExtra = extraDay != null,
                    onAgain = { choosingProfile = true },
                    onMine = { startFor(null) },
                    onRedraw = { redraw() },
                    onLeaveExtra = { leaveExtra() },
                )
            }
        } else {
            Screen(title = "求签", scrollable = false, horizontalAlignment = Alignment.CenterHorizontally) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (phase == StickPhase.DROPPED) "签已落地" else "心中默念所求之事", style = Fonts.serif(26, bold = true))
                    SecondaryText(
                        when {
                            phase == StickPhase.DROPPED -> "点按签文，看看今天的指引"
                            AppSettings.shakeEnabled -> "摇动手机，或按住下方按钮摇签"
                            else -> "按住下方按钮摇签"
                        },
                    )
                }
                if (target != null) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(palette.subtle(0.08f))
                            .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("为 ${target.avatar} ${target.name} 求签", style = Fonts.subheadline.weight(FontWeight.SemiBold))
                        Box(
                            Modifier
                                .size(36.dp)
                                .pressable { startFor(null) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Close, "不为${target.name}求了，回到自己的签", Modifier.size(18.dp), tint = palette.secondaryText)
                        }
                    }
                }
                if (extraDay != null) {
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(palette.subtle(0.08f))
                            .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("重新求签 · 不计入今日一签", style = Fonts.subheadline.weight(FontWeight.SemiBold))
                        Box(
                            Modifier
                                .size(36.dp)
                                .pressable { leaveExtra() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Close, "回到今日一签", Modifier.size(18.dp), tint = palette.secondaryText)
                        }
                    }
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

/**
 * 解签结果。forName：为家人朋友求的签写「为 某某 求得」（带头像），null 是自己的签；targetName 是对方的名字（分享卡、掷筊页用）；
 * ownerId：掷筊确认按「日期 + 求签对象 + 签号」记；isExtra：掷到阴杯后重新求的签，不计入今日一签；
 * onMine：看完别人的签，回到自己的今日一签；onRedraw：掷筊得阴杯后重新求一签；onLeaveExtra：不再重新求签，回到今日一签
 */
@Composable
private fun StickResult(
    stick: FortuneStick,
    wish: String,
    forName: String?,
    targetName: String?,
    ownerId: String?,
    isExtra: Boolean,
    onAgain: () -> Unit,
    onMine: () -> Unit,
    onRedraw: () -> Unit,
    onLeaveExtra: () -> Unit,
) {
    val palette = LocalPalette.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val ink = stickLevelInk(stick.level, palette.isDark)
    val ledgerEntry = remember(stick.n, ownerId) { JiaobeiLedger.entry(DayKey.today, ownerId, stick.n) }
    /** 已掷筊确认（连得三次圣杯） */
    var confirmed by remember(ledgerEntry) { mutableStateOf(JiaobeiLedger.isConfirmed(ledgerEntry)) }
    var showingJiaobei by rememberSaveable { mutableStateOf(false) }
    /** 掷筊页关上之后要盖「已确认」印 */
    var pendingStamp by remember { mutableStateOf(false) }
    // 切标签、去别的页面再回来时不再重放盖章的鼓声和震动
    var stamped by rememberSaveable { mutableStateOf(false) }
    var reduceMotion by remember { mutableStateOf(false) }
    val stamp = remember(stick.n) { Animatable(if (stamped) 1f else 0f) }
    val sealIn = remember(ledgerEntry) { Animatable(if (confirmed) 1f else 0f) }
    LaunchedEffect(stick.n) {
        reduceMotion = isReduceMotion()
        if (stamped) return@LaunchedEffect
        stamped = true
        delay(350)
        // 减弱动态效果时不从大号斜着砸下来，只淡入
        stamp.animateTo(1f, if (reduceMotion) tween(300) else spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
        SoundEngine.play(SoundEffect.DRUM, 0.8f)
        Haptics.heavy()
    }

    /** 掷筊页关上之后：得到确认就在签上盖「已确认」小印 */
    fun jiaobeiDismissed() {
        showingJiaobei = false
        if (!pendingStamp) return
        pendingStamp = false
        confirmed = true
        scope.launch {
            val reduce = isReduceMotion()
            delay(200)
            launch { sealIn.animateTo(1f, if (reduce) tween(300) else spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)) }
            delay(150)
            SoundEngine.play(SoundEffect.DRUM, 0.7f)
            Haptics.heavy()
        }
        view.announceForAccessibility("这支签已盖上已确认印")
    }

    Screen(
        title = "求签",
        actions = {
            ShareCardButton("分享签诗", "stick") { style ->
                ShareCardFrame(style, DayKey.today, if (isExtra) "星匣 · 求签" else "星匣 · 今日一签") {
                    StickShareContent(stick, wish, targetName, confirmed, style)
                }
            }
        },
    ) {
        if (forName != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "为 $forName 求得",
                    style = Fonts.subheadline.weight(FontWeight.SemiBold),
                    modifier = Modifier
                        .glass(CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
        if (isExtra) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(
                    Modifier
                        .glass(CircleShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Rounded.Replay, null, Modifier.size(16.dp), tint = palette.text)
                    Text("重新求得 · 不计入今日一签", style = Fonts.subheadline.weight(FontWeight.SemiBold))
                }
            }
        }
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
                            rotationZ = if (reduceMotion) -12f else -30f + 18f * stamp.value
                            val s = if (reduceMotion) 1f else 2.4f - 1.4f * stamp.value
                            scaleX = s
                            scaleY = s
                            alpha = 0.92f * stamp.value
                        }
                        .border(3.dp, hex(0xE53935), RoundedCornerShape(10.dp))
                        .clearAndSetSemantics { contentDescription = stick.levelDisplayName },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (stick.level == "下") "下签" else stick.level, style = Fonts.serif(if (stick.level.length > 1) 22 else 26, bold = true), color = hex(0xE53935))
                }
            }

            // 竖排签诗（从右往左）
            Box(Modifier.fillMaxWidth()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.verticalGradient(listOf(hex(0xFFF6E0, 0.12f), hex(0xFFD166, 0.06f))))
                        .border(1.dp, hex(0xFFD166, 0.35f), RoundedCornerShape(18.dp))
                        .padding(vertical = 18.dp)
                        .clearAndSetSemantics { contentDescription = stick.poem.joinToString("，") },
                    horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
                ) {
                    for (line in stick.poem.reversed()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (char in line) Text(char.toString(), style = Fonts.serif(24, bold = true))
                        }
                    }
                }
                // 掷筊连得三次圣杯后盖的小印；字号封顶，免得大字号下盖住签诗
                if (confirmed) {
                    val density = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1.3f))) {
                        JiaobeiSeal(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 6.dp, y = 10.dp)
                                .graphicsLayer {
                                    rotationZ = 10f
                                    val s = if (reduceMotion) 1f else 1.8f - 0.8f * sealIn.value
                                    scaleX = s
                                    scaleY = s
                                    alpha = sealIn.value
                                },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("解曰", style = Fonts.serif(17, bold = true), color = ink)
                Text(stick.meaning, style = Fonts.body)
                SecondaryText(stick.blessing)
            }
        }

        // 掷筊确认
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (confirmed) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Verified, null, Modifier.size(18.dp), tint = palette.positive)
                    Text("连得三次圣杯，这支签已得到确认", style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.positive, textAlign = TextAlign.Center)
                }
            } else {
                BigActionButton("掷筊确认", Icons.Rounded.NightsStay) { showingJiaobei = true }
                SecondaryText("传统求签后要掷筊，连得三次圣杯才算数", style = Fonts.footnote, textAlign = TextAlign.Center)
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
            SecondaryText("心诚则灵 · 一日一签", style = Fonts.footnote)
            if (isExtra) GlassButton(targetName?.let { "回到${it}的今日一签" } ?: "回到今日一签", Icons.Rounded.Replay, onClick = onLeaveExtra)
            GlassButton("为家人朋友再求一签", Icons.Rounded.People, onClick = onAgain)
            if (forName != null) GlassButton("看我的今日一签", Icons.Rounded.Person, onClick = onMine)
        }
    }

    if (showingJiaobei) {
        JiaobeiDialog(
            stick,
            targetName,
            onConfirmed = {
                JiaobeiLedger.markConfirmed(ledgerEntry)
                pendingStamp = true
            },
            onRedraw = {
                showingJiaobei = false
                pendingStamp = false
                onRedraw()
            },
            onDismiss = { jiaobeiDismissed() },
        )
    }
}

/** 为谁求签：列出当前档案以外的家人朋友 */
@Composable
private fun StickProfilePicker(onBack: () -> Unit, onPick: (Profile) -> Unit) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val others = Store.sortedProfiles.filter { it.id != Store.primary?.id }
    Screen(title = "为谁求签", onBack = onBack) {
        if (others.isEmpty()) {
            GlassCard {
                EmptyHint(Icons.Rounded.People, "还没有家人朋友的档案", "添加他们的生日后，就能为他们各求一支今日签")
            }
        } else {
            GlassCard(padding = 8.dp, spacing = 0.dp) {
                others.forEachIndexed { index, profile ->
                    val drawn = todayStickDraw(profile.id)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .pressable { onPick(profile) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(profile.avatar, style = Fonts.title) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(profile.name, style = Fonts.headline)
                            SecondaryText(profile.relation, style = Fonts.caption)
                        }
                        if (drawn != null) {
                            SecondaryText("今日已求 · ${ContentLibrary.stick(drawn.stickNumber).numberText}", style = Fonts.caption)
                        }
                    }
                    if (index < others.size - 1) HorizontalDivider(color = palette.subtle(0.08f))
                }
            }
            SecondaryText("每人每天一签，今天已经求过的会直接显示那一支", style = Fonts.caption)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButton("添加家人朋友", Icons.Rounded.PersonAddAlt1) { navigator.open(Routes.profileEditor()) }
        }
    }
}

// endregion

// region 塔罗

/** 牌阵。key 与 iOS 的 rawValue 相同，存在塔罗手记里 */
internal enum class TarotSpread(val key: String, val label: String, val positions: List<String>, val detail: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SINGLE("single", "单张指引", listOf("今日指引"), "抽一张牌，获得今天的提示", Icons.Rounded.CropPortrait),
    TIME("time", "时间之流", listOf("过去", "现在", "未来"), "看清事情的来龙去脉", Icons.Rounded.Schedule),
    LOVE("love", "情感关系", listOf("你的状态", "对方的状态", "关系走向"), "了解两个人之间的能量", Icons.Rounded.Favorite),
    ADVICE("advice", "问题解答", listOf("现状", "阻碍", "建议"), "针对一个具体问题给出方向", Icons.Rounded.QuestionAnswer);

    /** 牌位名；超出牌阵的（旧数据）写「第 N 张」 */
    fun position(slot: Int): String = positions.getOrNull(slot) ?: "第 ${slot + 1} 张"

    companion object {
        fun of(key: String): TarotSpread = entries.firstOrNull { it.key == key } ?: SINGLE
    }

    /**
     * 每个牌位补充哪一行（与 iOS 一致）：收尾的牌给建议，情感牌阵的两人状态给感情，
     * 「现在 / 现状」按问题的话题补感情或事业，问题没提到就不补，免得每张牌都挂同一类话
     */
    fun aspect(slot: Int, question: String): TarotAspect? = when (this) {
        SINGLE -> TarotAspect.ADVICE
        TIME -> if (slot == 2) TarotAspect.ADVICE else if (slot == 1) TarotAspect.topic(question) else null
        LOVE -> if (slot == 2) TarotAspect.ADVICE else TarotAspect.LOVE
        ADVICE -> if (slot == 2) TarotAspect.ADVICE else if (slot == 0) TarotAspect.topic(question) else null
    }
}

/** 牌面主释义之外补充的一行 */
internal enum class TarotAspect(val label: String) {
    LOVE("感情"), CAREER("事业"), ADVICE("建议");

    /** 逆位缺字段时为 null，整行隐藏 */
    fun text(card: TarotCard, reversed: Boolean): String? = when (this) {
        LOVE -> card.loveFor(reversed)
        CAREER -> card.careerFor(reversed)
        ADVICE -> card.adviceFor(reversed)
    }

    companion object {
        private val loveWords = listOf("感情", "恋", "喜欢", "爱情", "对象", "男友", "女友", "男朋友", "女朋友", "前任", "暧昧", "复合", "表白", "结婚", "分手", "桃花", "脱单", "约会")
        private val careerWords = listOf("工作", "事业", "职", "面试", "升职", "加薪", "跳槽", "老板", "同事", "项目", "生意", "创业", "offer")

        /** 从问题里粗略判断话题；没提到感情或工作时返回 null */
        fun topic(question: String): TarotAspect? = when {
            loveWords.any { it in question } -> LOVE
            careerWords.any { it in question } -> CAREER
            else -> null
        }
    }
}

private enum class TarotPhase { SETUP, SHUFFLING, PICKING, READING }

internal data class DrawnCard(val deckIndex: Int, val card: TarotCard, val reversed: Boolean)

/** 存进塔罗手记的牌：牌号 + 正位 u / 逆位 r，如 "0u,13r,5u"（与 iOS 一致） */
internal fun encodeTarotCards(entries: List<DrawnCard>): String =
    entries.joinToString(",") { "${it.card.id}${if (it.reversed) "r" else "u"}" }

/** 解析塔罗手记里的牌；认不出的跳过 */
internal fun decodeTarotCards(cards: String): List<DrawnCard> =
    cards.split(",").mapIndexedNotNull { index, raw ->
        val token = raw.trim()
        val flag = token.lastOrNull()
        if (flag != 'u' && flag != 'r') return@mapIndexedNotNull null
        val card = token.dropLast(1).toIntOrNull()?.let { ContentLibrary.tarotCard(it) } ?: return@mapIndexedNotNull null
        DrawnCard(index, card, flag == 'r')
    }

/** 按正逆位的比例给一句综合解读（解读页、塔罗手记和分享卡共用，与 iOS 一致） */
internal fun tarotSummaryText(entries: List<DrawnCard>): String {
    val upright = entries.count { !it.reversed }
    return when {
        entries.size == 1 -> if (entries[0].reversed) "逆位提醒你放慢脚步，先处理内心的阻碍。" else "正位的能量清晰明亮，跟着这张牌的指引行动吧。"
        upright == entries.size -> "全部为正位，整体能量顺畅积极，是推进事情的好时机。"
        upright == 0 -> "全部为逆位，当下阻力较多，宜先调整心态、等待时机。"
        upright > entries.size / 2 -> "正位居多，整体趋势向好，留意逆位牌提示的小问题即可。"
        else -> "逆位居多，事情尚有阻碍，不妨换个角度重新审视。"
    }
}

/** 小阿卡纳的牌面渐变：权杖火红、圣杯水蓝、宝剑银灰、星币金黄（与 iOS 一致） */
internal val TarotSuit.colors: List<Color>
    get() = when (this) {
        TarotSuit.WANDS -> listOf(hex(0xB83A1B), hex(0x3A0D05))
        TarotSuit.CUPS -> listOf(hex(0x1E6FB8), hex(0x0A1E45))
        TarotSuit.SWORDS -> listOf(hex(0x6B7686), hex(0x1C222C))
        TarotSuit.PENTACLES -> listOf(hex(0xA97A12), hex(0x3A2806))
    }

/** 压在牌面深色渐变上的点缀色 */
internal val TarotSuit.accent: Color
    get() = when (this) {
        TarotSuit.WANDS -> hex(0xFFB48A)
        TarotSuit.CUPS -> hex(0xA5DBFF)
        TarotSuit.SWORDS -> hex(0xE5E9F0)
        TarotSuit.PENTACLES -> hex(0xFFE08A)
    }

/** 花色图标：权杖属火、圣杯是酒杯、宝剑属风、星币是带星的圆币 */
internal val TarotSuit.icon: androidx.compose.ui.graphics.vector.ImageVector
    get() = when (this) {
        TarotSuit.WANDS -> Icons.Rounded.LocalFireDepartment
        TarotSuit.CUPS -> Icons.Rounded.WineBar
        TarotSuit.SWORDS -> Icons.Rounded.Air
        TarotSuit.PENTACLES -> Icons.Rounded.Stars
    }

/** 牌面的点缀色：小阿卡纳用花色色，大阿卡纳用金色 */
internal fun tarotAccent(card: TarotCard): Color = card.tarotSuit?.accent ?: hex(0xF5D78E)

/** 牌面渐变：小阿卡纳用花色主色，大阿卡纳限定在深蓝 → 紫 → 品红的「宝石色」区间 */
private fun tarotColors(card: TarotCard): List<Color> {
    card.tarotSuit?.let { return it.colors }
    val hue = (card.hue % 1.0).toFloat() * 360f
    return listOf(Color.hsv(hue, 0.7f, 0.5f), Color.hsv((hue + 21.6f) % 360f, 0.8f, 0.2f))
}

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
    // 小阿卡纳
    "globe.europe.africa.fill" -> "🌍"
    "sailboat.fill" -> "⛵"
    "house.and.flag.fill" -> "🏡"
    "flag.2.crossed.fill" -> "🎌"
    "trophy.fill" -> "🏆"
    "shield.fill" -> "🛡️"
    "paperplane.fill" -> "✈️"
    "bandage.fill" -> "🩹"
    "figure.strengthtraining.traditional" -> "🏋️"
    "lightbulb.fill" -> "💡"
    "figure.equestrian.sports" -> "🏇"
    "cat.fill" -> "🐈"
    "flame.circle.fill" -> "☀️"
    "drop.fill" -> "💧"
    "heart.fill" -> "❤️"
    "wineglass.fill" -> "🍷"
    "cup.and.saucer.fill" -> "☕"
    "cloud.rain.fill" -> "🌧️"
    "teddybear.fill" -> "🧸"
    "bubbles.and.sparkles.fill" -> "🫧"
    "figure.hiking" -> "🥾"
    "sparkles" -> "✨"
    "rainbow" -> "🌈"
    "fish.fill" -> "🐟"
    "heart.text.square.fill" -> "💌"
    "hands.and.sparkles.fill" -> "🙌"
    "water.waves" -> "🌊"
    "eye.slash.fill" -> "🙈"
    "heart.slash.fill" -> "💔"
    "bed.double.fill" -> "🛏️"
    "figure.fencing" -> "🤺"
    "ferry.fill" -> "⛴️"
    "theatermasks.fill" -> "🎭"
    "lock.fill" -> "🔒"
    "cloud.moon.bolt.fill" -> "⛈️"
    "arrow.down.to.line" -> "⚓"
    "magnifyingglass" -> "🔍"
    "wind" -> "🌬️"
    "eyeglasses" -> "👓"
    "brain.head.profile" -> "🧠"
    "star.circle.fill" -> "🌟"
    "infinity" -> "♾️"
    "person.3.fill" -> "👥"
    "archivebox.fill" -> "📦"
    "snowflake" -> "❄️"
    "gift.fill" -> "🎁"
    "tree.fill" -> "🌳"
    "hammer.fill" -> "🔨"
    "bird.fill" -> "🐦"
    "house.fill" -> "🏠"
    "graduationcap.fill" -> "🎓"
    "tortoise.fill" -> "🐢"
    "hare.fill" -> "🐇"
    "building.2.fill" -> "🏢"
    else -> "✨"
}

@Composable
fun TarotScreen() {
    val palette = LocalPalette.current
    val navigator = LocalNavigator.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(TarotPhase.SETUP) }
    // 去塔罗手记看一眼再回来，选好的牌阵和问题还在
    var spread by rememberSaveable { mutableStateOf(TarotSpread.TIME) }
    var question by rememberSaveable { mutableStateOf("") }
    var deck by remember { mutableStateOf<List<DrawnCard>>(emptyList()) }
    val picked = remember { mutableStateListOf<DrawnCard>() }
    val flipped = remember { mutableStateListOf<Int>() }
    var shuffleSeed by remember { mutableIntStateOf(0) }
    var confetti by remember { mutableIntStateOf(0) }
    // 洗牌、入场、翻牌的延迟任务；重新开始或再洗一次时全部取消，免得旧任务把页面拉回去
    val pending = remember { mutableListOf<Job>() }

    fun launchStep(block: suspend CoroutineScope.() -> Unit) {
        pending.removeAll { !it.isActive }
        pending += scope.launch(block = block)
    }

    fun cancelPending() {
        pending.forEach { it.cancel() }
        pending.clear()
    }

    fun reset() {
        cancelPending()
        phase = TarotPhase.SETUP
        picked.clear()
        flipped.clear()
    }

    fun startShuffle() {
        cancelPending()
        deck = TrueRandom.shuffled(ContentLibrary.tarotDeck(AppSettings.tarotFullDeck)).mapIndexed { i, card -> DrawnCard(i, card, TrueRandom.bool()) }
        picked.clear()
        flipped.clear()
        phase = TarotPhase.SHUFFLING
        launchStep {
            repeat(6) {
                delay(220)
                shuffleSeed = it + 1
                SoundEngine.play(SoundEffect.CARD_FLIP, 0.6f)
                Haptics.selection()
            }
            delay(250)
            phase = TarotPhase.PICKING
            SoundEngine.play(SoundEffect.WHOOSH)
        }
    }

    fun save() {
        // 塔罗手记和决策历史各记一笔
        Store.tarotReadings.add(TarotReading(spread = spread.key, cards = encodeTarotCards(picked), question = question))
        val result = picked.joinToString(" · ") { it.card.name + if (it.reversed) "(逆)" else "" }
        Store.log("tarot", spread.label, result, question.ifEmpty { null })
    }

    fun flip(slot: Int, delayMs: Long = 0) {
        if (slot in flipped) return
        launchStep {
            delay(delayMs)
            if (slot in flipped) return@launchStep
            flipped.add(slot)
            SoundEngine.play(SoundEffect.CARD_FLIP)
            Haptics.impact()
            if (flipped.size == picked.size) {
                // 先记下这次占卜，再等收尾音效：翻完马上点「重新开始」会取消这个任务，记录不能跟着丢
                save()
                delay(400)
                SoundEngine.play(SoundEffect.SPARKLE)
                if (picked.all { !it.reversed }) confetti++
                view.announceForAccessibility(
                    picked.mapIndexed { index, item -> "${spread.position(index)}，${item.card.name}，${if (item.reversed) "逆位" else "正位"}" }.joinToString("；"),
                )
            }
        }
    }

    fun pick(entry: DrawnCard) {
        if (phase != TarotPhase.PICKING || picked.size >= spread.positions.size || entry in picked) return
        Haptics.impact()
        SoundEngine.play(SoundEffect.CARD_FLIP)
        picked.add(entry)
        if (picked.size == spread.positions.size) {
            launchStep {
                delay(700)
                phase = TarotPhase.READING
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        when (phase) {
            TarotPhase.SETUP -> Screen(
                title = "塔罗占卜",
                horizontalAlignment = Alignment.CenterHorizontally,
                actions = { CircleIconButton(Icons.Rounded.Book, "塔罗手记") { navigator.open(Routes.TAROT_JOURNAL) } },
            ) {
                Box(Modifier.padding(top = 16.dp)) { TarotBackMini(1.5f) }
                Text("静下心来，想一个问题", style = Fonts.serif(24, bold = true))
                GlassTextField(question, "你想问什么？（可不填）") { question = it }
                // 内容里有小阿卡纳时才给选牌组
                val fullCount = ContentLibrary.tarot.size
                val majorCount = remember { ContentLibrary.tarotDeck(false).size }
                if (fullCount > majorCount) {
                    val fullDeck = AppSettings.tarotFullDeck
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SecondaryText("牌组", style = Fonts.subheadline.weight(FontWeight.SemiBold))
                        SegmentedPicker(listOf(false, true), fullDeck, { if (it) "完整 $fullCount 张" else "大阿卡纳 $majorCount 张" }) {
                            AppSettings.tarotFullDeck = it
                        }
                        SecondaryText(
                            if (fullDeck) "含 ${fullCount - majorCount} 张小阿卡纳，日常小事也能看得更细" else "只用大阿卡纳，聚焦人生的大方向",
                            style = Fonts.caption,
                        )
                    }
                }
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
                            ) { Icon(item.icon, null, Modifier.size(20.dp), tint = if (selected) palette.onAccent else palette.ink(palette.accent)) }
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
                actions = {
                    // 全部翻开后才能分享；顶栏放两个图标，免得挤到标题
                    if (picked.isNotEmpty() && flipped.size == picked.size) {
                        ShareCardButton("分享解读", "tarot") { style ->
                            ShareCardFrame(style, title = "星匣 · 塔罗占卜") {
                                TarotShareContent(spread, question, picked.toList(), style)
                            }
                        }
                        CircleIconButton(Icons.Rounded.Refresh, "重新开始") { reset() }
                    } else {
                        TextAction("重新开始") { reset() }
                    }
                },
            ) {
                if (question.isNotEmpty()) SecondaryText("「$question」", style = Fonts.serif(18), textAlign = TextAlign.Center)
                BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val single = spread == TarotSpread.SINGLE
                    // 多张牌按可用宽度收窄（最宽 106），窄屏和大显示尺寸下也不挤出屏幕
                    val count = maxOf(1, picked.size)
                    val cardWidth = if (single) 180.dp else minOf(106.dp, (maxWidth - 12.dp * (count - 1)) / count)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        picked.forEachIndexed { slot, entry ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FlippableTarotCard(
                                    entry,
                                    slot in flipped,
                                    Modifier
                                        .size(cardWidth, if (single) 292.dp else cardWidth * (172f / 106f))
                                        .pressable { flip(slot) },
                                )
                                SecondaryText(spread.position(slot), style = Fonts.caption.weight(FontWeight.SemiBold))
                            }
                        }
                    }
                }
                if (flipped.size < picked.size) {
                    GlassButton("全部翻开", Icons.Rounded.Layers) { picked.indices.forEach { flip(it, it * 250L) } }
                    SecondaryText("点按牌面逐张翻开", style = Fonts.caption)
                } else {
                    picked.forEachIndexed { slot, entry -> TarotInterpretation(spread, slot, entry, question) }
                    if (picked.isNotEmpty()) TarotSummary(picked)
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
internal fun FlippableTarotCard(entry: DrawnCard, faceUp: Boolean, modifier: Modifier) {
    val angle by animateFloatAsState(if (faceUp) 180f else 0f, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow), label = "flip")
    Box(
        modifier
            .graphicsLayer {
                rotationY = angle
                cameraDistance = 12 * density
            }
            // 读屏只读牌名和正逆位；点按动作来自调用方传入的 pressable，要放在它后面
            .clearAndSetSemantics {
                contentDescription = if (faceUp) "${entry.card.name}，${if (entry.reversed) "逆位" else "正位"}" else "背面朝上的牌，点按翻开"
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

/**
 * 静态的牌面图案（塔罗页、塔罗手记和分享卡共用）。牌面上的字按牌宽缩放、不随系统字号变化。
 * 小阿卡纳用花色主色和花色图标，牌号位置写 A、2…10、侍从、骑士、王后、国王
 */
@Composable
internal fun TarotCardFace(card: TarotCard, modifier: Modifier) {
    val colors = tarotColors(card)
    val accent = tarotAccent(card)
    val suit = card.tarotSuit
    BoxWithConstraints(modifier) {
        val w = maxWidth.value
        val localDensity = LocalDensity.current
        /** 牌宽的 fraction 倍，换成不随系统字号缩放的字号 */
        fun font(fraction: Float) = with(localDensity) { (w * fraction).dp.toSp() }
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(Brush.verticalGradient(colors), cornerRadius = CornerRadius(size.width * 0.09f))
            val rng = SeededGenerator("tarot-face", card.id)
            repeat(30) {
                val r = rng.double(0.4, 1.4).toFloat() * density
                drawCircle(Color.White.copy(alpha = rng.double(0.3, 0.9).toFloat()), r, Offset(rng.unit().toFloat() * size.width, rng.unit().toFloat() * size.height))
            }
            val inset = size.width * 0.05f
            drawRoundRect(accent, Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(size.width * 0.07f), style = Stroke(maxOf(density, size.width * 0.012f)))
        }
        if (suit != null) {
            // 花色图标衬底
            Icon(suit.icon, null, Modifier.align(Alignment.Center).size((w * 0.62f).dp), tint = accent.copy(alpha = 0.1f))
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(vertical = (w * 0.12f).dp, horizontal = (w * 0.08f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (suit != null) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(suit.icon, null, Modifier.size((w * 0.09f).dp), tint = accent)
                    Text(
                        card.rankText,
                        style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = font(0.09f), color = accent, textAlign = TextAlign.Center),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(suit.icon, null, Modifier.size((w * 0.09f).dp), tint = accent)
                }
            } else {
                Text(card.roman, style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = font(0.11f), color = accent))
            }
            Spacer(Modifier.weight(1f))
            Text(tarotEmoji(card.symbol), style = TextStyle(fontSize = font(0.3f)))
            Spacer(Modifier.weight(1f))
            Text(card.name, style = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = font(0.13f), color = Color.White), maxLines = 1)
            Text(
                card.en.uppercase(),
                style = TextStyle(fontFamily = FontFamily.Serif, fontSize = font(0.055f), letterSpacing = font(0.008f), color = accent.copy(alpha = 0.9f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun TarotInterpretation(spread: TarotSpread, slot: Int, entry: DrawnCard, question: String) {
    val palette = LocalPalette.current
    val card = entry.card
    // 逆位只用逆位释义，缺字段就不显示，绝不拿正位内容顶替
    val keywords = card.keywordsFor(entry.reversed)
    val extra = spread.aspect(slot, question)?.let { aspect -> aspect.text(card, entry.reversed)?.let { aspect.label to it } }
    GlassCard(spacing = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                spread.position(slot),
                style = Fonts.caption.weight(FontWeight.Bold),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(palette.accent.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            val suit = card.tarotSuit
            if (suit != null) {
                Icon(suit.icon, null, Modifier.size(16.dp), tint = palette.ink(suit.colors[0]))
                Text(card.name, style = Fonts.headline, modifier = Modifier.weight(1f, fill = false))
            } else {
                Text("${card.roman} · ${card.name}", style = Fonts.headline, modifier = Modifier.weight(1f, fill = false))
            }
            Text(
                if (entry.reversed) "逆位" else "正位",
                style = Fonts.caption.weight(FontWeight.SemiBold),
                color = if (entry.reversed) palette.negative else palette.positive,
            )
        }
        if (!keywords.isNullOrEmpty()) {
            Flow(spacing = 6.dp) { for (k in keywords) TagChip(k) }
        }
        Text(if (entry.reversed) card.reversed else card.upright, style = Fonts.subheadline)
        if (extra != null) SecondaryText("${extra.first}：${extra.second}")
    }
}

@Composable
internal fun TarotSummary(picked: List<DrawnCard>) {
    GlassCard(spacing = 8.dp) {
        SectionHeader("综合解读", icon = Icons.Rounded.AutoAwesome)
        Text(tarotSummaryText(picked), style = Fonts.body)
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
        SoundEngine.play(SoundEffect.CHIME)
        Haptics.success()
        recent.add(0, answer)
        if (recent.size > 5) recent.removeAt(recent.size - 1)
        Store.log("answers", question, answer)
    }

    fun close() {
        SoundEngine.play(SoundEffect.CARD_FLIP)
        isOpen = false
        question = ""
        scope.launch { hold.snapTo(0f) }
    }

    // 分享卡预览弹出时不响应摇一摇
    val sharing = remember { mutableStateOf(false) }
    OnShake(enabled = !isOpen && !sharing.value) { open() }

    Screen(
        title = "答案之书",
        horizontalAlignment = Alignment.CenterHorizontally,
        spacing = 22.dp,
        actions = {
            if (isOpen) {
                ShareCardButton("分享答案", "answer", sharing) { style ->
                    ShareCardFrame(style, title = "星匣 · 答案之书") {
                        AnswerShareContent(question, answer, style)
                    }
                }
            }
            // 收藏到首页快捷工具
            FavoriteToggleButton(FavoriteTool.ANSWERS_ID)
        },
    ) {
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
