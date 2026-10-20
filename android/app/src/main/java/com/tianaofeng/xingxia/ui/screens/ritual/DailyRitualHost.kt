package com.tianaofeng.xingxia.ui.screens.ritual

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.DailyFortune
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.FortuneStick
import com.tianaofeng.xingxia.core.MoonInfo
import com.tianaofeng.xingxia.core.SkyEvents
import com.tianaofeng.xingxia.core.SolarTerms
import com.tianaofeng.xingxia.core.lunar
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.ColorDotChip
import com.tianaofeng.xingxia.ui.components.CosmicBackground
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.GradientText
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SoundEffect
import com.tianaofeng.xingxia.ui.components.SoundEngine
import com.tianaofeng.xingxia.ui.components.StarRatingView
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.gradientTint
import com.tianaofeng.xingxia.ui.components.isReduceMotion
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.common.stickLevelColor
import com.tianaofeng.xingxia.ui.screens.common.stickLevelInk
import com.tianaofeng.xingxia.ui.screens.divination.ShareCardDialog
import com.tianaofeng.xingxia.ui.screens.divination.ShareCardFrame
import com.tianaofeng.xingxia.ui.screens.divination.ShareCardStyle
import com.tianaofeng.xingxia.ui.screens.divination.ShareChip
import com.tianaofeng.xingxia.ui.screens.divination.ShareScoreRing
import com.tianaofeng.xingxia.ui.screens.records.MoodLevel
import com.tianaofeng.xingxia.ui.screens.records.MoodPicker
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 每日仪式的五页 */
enum class RitualPage(val title: String) {
    MOON("今天的月亮"),
    FORTUNE("运势揭晓"),
    STICK("今日一签"),
    MOOD("点一下心情"),
    SUMMARY("今日卡片"),
}

/**
 * 每天第一次进入前台时全屏展示每日仪式（与 iOS 的 DailyRitualModifier 一致）：需要打开了「每日仪式」、
 * 已经走过档案引导或已有档案、今天还没有 RitualDay。首次启动正在走档案引导时这次不打扰，等退到后台再回来。
 * 由根布局挂在 NavHost 之后（需要 LocalNavigator：「去求签」关掉仪式后进求签页）
 */
@Composable
fun DailyRitualHost() {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    /** 正在展示的仪式是哪一天的（yyyyMMdd）；null 时不展示 */
    var session by rememberSaveable { mutableStateOf<Int?>(null) }
    /** 首次启动正在走档案引导：这次启动不打扰，等下次从后台回来 */
    var waitsForBackground by rememberSaveable { mutableStateOf(!AppSettings.hasOnboarded && Store.primary == null) }

    fun evaluate() {
        if (session != null) return
        if (!AppSettings.dailyRitualEnabled || waitsForBackground) return
        if (!AppSettings.hasOnboarded && Store.primary == null) return
        val today = DayKey.today
        if (Store.hasRitual(today)) return
        session = today.rawValue
    }

    DisposableEffect(lifecycleOwner) {
        // 添加观察者时会补发到当前状态为止的事件：已经在前台时立刻判断一次
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    // 跨天回来时昨天的仪式还开着：换成今天的
                    if (session != null && session != DayKey.today.rawValue) {
                        session = null
                    }
                    evaluate()
                }
                Lifecycle.Event.ON_STOP -> {
                    // 旋转屏幕等重建不算退到后台
                    if (context.findActivity()?.isChangingConfigurations != true) {
                        waitsForBackground = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val day = session?.let { DayKey.fromRaw(it) } ?: return
    key(day.rawValue) {
        RitualDialog(day) { route ->
            session = null
            if (route != null) navigator.open(route)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * 每日仪式：今天的月亮 → 运势揭晓 → 今日一签 → 心情 → 今日卡片。
 * 随时可以「跳过」（返回键也算跳过），跳过也记一条 RitualDay（skipped），今天就不再出现；
 * 走完或点「去求签」记为完成，计入月相徽章。onFinish 的 route 不为 null 时，关闭后跳到那个页面
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RitualDialog(day: DayKey, onFinish: (String?) -> Unit) {
    val palette = LocalPalette.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val pages = RitualPage.entries
    val pagerState = rememberPagerState { pages.size }
    val page = pages[pagerState.currentPage.coerceIn(0, pages.lastIndex)]

    val moon = remember(day) { MoonInfo.on(day) }
    val birth = Store.birth
    val fortune = remember(day, birth) { FortuneEngine.daily(birth, day) }
    // 只认当前档案的签：给家人朋友求的签记在他们自己的档案下
    val profileId = Store.primary?.id
    val stick = Store.stickDraws.items
        .filter { it.dayKey == day.rawValue && it.profileId == profileId }
        .maxByOrNull { it.date }
        ?.let { ContentLibrary.stick(it.stickNumber) }
    val mood = MoodLevel.of(Store.todayMood(day)?.mood)

    var revealed by rememberSaveable { mutableStateOf(false) }
    val ring = remember { Animatable(if (revealed) fortune.score / 100f else 0f) }
    var finished by remember { mutableStateOf(false) }

    fun finish(skipped: Boolean, route: String? = null) {
        if (finished) return
        finished = true
        // 每天最多一条；完成过就一直算完成
        Store.recordRitual(day, skipped)
        if (!skipped) Haptics.success()
        onFinish(route)
    }

    fun advance() {
        val next = pagerState.currentPage + 1
        if (next > pages.lastIndex) {
            finish(skipped = false)
            return
        }
        Haptics.tap()
        scope.launch { pagerState.animateScrollToPage(next) }
        view.announceForAccessibility(pages[next].title)
    }

    // 第一次翻到运势页时揭晓：分数环从 0 转到当天分数，转到头时一声钟响加一记触感
    LaunchedEffect(pagerState.settledPage) {
        if (pages[pagerState.settledPage] != RitualPage.FORTUNE) return@LaunchedEffect
        val target = fortune.score / 100f
        if (revealed) {
            // 揭晓到一半就翻走了：回来时把分数环补完
            if (ring.value != target) ring.animateTo(target, tween(400))
            return@LaunchedEffect
        }
        val announcement = "运势指数 ${fortune.score} 分，${fortune.levelName}"
        if (isReduceMotion()) {
            revealed = true
            ring.snapTo(target)
            SoundEngine.play(SoundEffect.CHIME)
            Haptics.success()
            view.announceForAccessibility(announcement)
            return@LaunchedEffect
        }
        // 等翻页停稳再转
        delay(300)
        revealed = true
        launch { ring.animateTo(target, tween(1600, easing = FastOutSlowInEasing)) }
        // 由弱到强：先一记轻触，0.42 秒后一记重击，正好落在分数环转到头的时候
        delay(1150)
        Haptics.impact()
        delay(420)
        Haptics.heavy()
        SoundEngine.play(SoundEffect.CHIME)
        view.announceForAccessibility(announcement)
    }

    Dialog(
        onDismissRequest = { finish(skipped = true) },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize()) {
            CosmicBackground()
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        page.title,
                        style = Fonts.headline,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 64.dp)
                            .semantics { heading() },
                    )
                    if (page != RitualPage.SUMMARY) {
                        Box(Modifier.align(Alignment.CenterEnd)) {
                            TextAction("跳过") { finish(skipped = true) }
                        }
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.Top,
                ) { index ->
                    when (pages[index]) {
                        RitualPage.MOON -> RitualMoonPage(day, moon)
                        RitualPage.FORTUNE -> RitualFortunePage(fortune, revealed, ring.value)
                        RitualPage.STICK -> RitualStickPage(stick) { finish(skipped = false, route = Routes.STICK) }
                        RitualPage.MOOD -> RitualMoodPage(mood) { level -> Store.setMood(level.value, day) }
                        RitualPage.SUMMARY -> RitualSummaryPage(day, moon, fortune, stick, mood)
                    }
                }
                Column(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RitualPageDots(page)
                    val last = page == RitualPage.SUMMARY
                    BigActionButton(if (last) "开始今天" else "继续", if (last) Icons.Rounded.WbSunny else null) { advance() }
                }
            }
        }
    }
}

// region 页面骨架

/** 一页的内容竖直居中；大字号、横屏放不下时可以上下滚动 */
@Composable
private fun RitualPageScroll(content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .heightIn(min = minHeight)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}

@Composable
private fun RitualPageDots(current: RitualPage) {
    val palette = LocalPalette.current
    Row(
        Modifier.clearAndSetSemantics { contentDescription = "第 ${current.ordinal + 1} 页，共 ${RitualPage.entries.size} 页" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (page in RitualPage.entries) {
            val selected = page == current
            val width by animateDpAsState(if (selected) 22.dp else 8.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "dot")
            Box(
                Modifier
                    .size(width, 8.dp)
                    .clip(CircleShape)
                    .then(if (selected) Modifier.background(palette.accentBrush) else Modifier.background(palette.subtle(0.2f))),
            )
        }
    }
}

/** 随系统字号放大的尺寸，设上限（对应 iOS 的 @ScaledMetric + min） */
@Composable
private fun scaled(base: Dp, max: Dp): Dp = minOf(base * LocalDensity.current.fontScale, max)

// endregion

// region ① 今天的月亮

/** 每一相一句话，按 MoonPhaseName 的顺序（与 iOS 一致） */
private val MoonTips = listOf(
    "月隐无光，夜空最暗，适合许下一个新的心愿。",
    "新月初生，细细一弯，适合开始一件小事。",
    "月亮亮了一半，事情也走到一半，再推一把。",
    "月亮一天比一天圆，适合继续积累、打磨细节。",
    "月正圆，适合收获、感恩，和想念的人说说话。",
    "满月之后慢慢变亏，适合分享、回顾这些天的收获。",
    "月亮只剩一半，适合整理房间，放下不需要的东西。",
    "残月将隐，好好休息，为下一轮蓄力。",
)

@Composable
private fun RitualMoonPage(day: DayKey, moon: MoonInfo) {
    val palette = LocalPalette.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val appear by animateFloatAsState(if (shown) 1f else 0f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessVeryLow), label = "moon")
    val termText = remember(day) {
        SolarTerms.termOn(day)?.let { "今天${it.name}" }
            ?: SolarTerms.next(day).let { "距${it.name}还有 ${day.daysTo(it.day)} 天" }
    }
    val fullMoonText = remember(day) {
        val full = SkyEvents.nextFullMoon(day.adding(-1))
        if (full == day) "今天满月" else "距满月还有 ${day.daysTo(full)} 天"
    }
    RitualPageScroll {
        SecondaryText("${DateText.monthDay(day)} ${DateText.weekday(day)}", style = Fonts.subheadline.weight(FontWeight.SemiBold))

        MoonPhaseView(
            moon.phase,
            size = scaled(200.dp, 260.dp),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .graphicsLayer {
                    val scale = 0.86f + 0.14f * appear
                    scaleX = scale
                    scaleY = scale
                    alpha = appear.coerceIn(0f, 1f)
                },
        )

        Column(
            Modifier.semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GradientText(moon.name.label, style = Fonts.serif(34, bold = true))
            SecondaryText("亮面 ${moon.percent}%", style = Fonts.headline)
        }

        Flow(spacing = 8.dp, horizontalAlignment = Alignment.CenterHorizontally) {
            TagChip("农历${day.lunar().text}", icon = Icons.Rounded.CalendarMonth)
            TagChip(termText, palette.accentSecondary, Icons.Rounded.Eco)
            TagChip(fullMoonText, palette.color(2), Icons.Rounded.NightsStay)
        }

        Text(
            MoonTips[moon.name.ordinal],
            style = Fonts.body,
            color = palette.text.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
    }
}

// endregion

// region ② 运势揭晓

@Composable
private fun RitualFortunePage(fortune: DailyFortune, revealed: Boolean, progress: Float) {
    val palette = LocalPalette.current
    // 分数环转得差不多了再显示等级和摘要
    val textAlpha by animateFloatAsState(
        if (revealed) 1f else 0f,
        tween(500, delayMillis = if (revealed) 1300 else 0),
        label = "fortuneText",
    )
    RitualPageScroll {
        SecondaryText(if (fortune.isPersonalized) "你的专属运势" else "今天的运势", style = Fonts.subheadline.weight(FontWeight.SemiBold))

        RitualScoreRing(
            progress,
            size = scaled(190.dp, 260.dp),
            description = if (revealed) "运势指数 ${fortune.score} 分" else "运势指数，即将揭晓",
        )

        Column(
            Modifier
                .alpha(textAlpha)
                .then(if (revealed) Modifier else Modifier.clearAndSetSemantics { }),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GradientText(fortune.levelName, style = Fonts.display(40, FontWeight.Black))
            StarRatingView(fortune.stars, size = 16.dp)
            Text(fortune.summary, style = Fonts.body, color = palette.text.copy(alpha = 0.88f), textAlign = TextAlign.Center)
            Flow(spacing = 8.dp, modifier = Modifier.padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ColorDotChip(fortune.luckyColor)
                TagChip("幸运数字 ${fortune.luckyNumber}", icon = Icons.Rounded.Tag)
                TagChip("吉时 ${fortune.luckyHour.hourName}", icon = Icons.Rounded.Schedule)
            }
        }
    }
}

/** 从 0 转到当天分数的分数环，环中的数字跟着一起涨 */
@Composable
private fun RitualScoreRing(progress: Float, size: Dp, description: String) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val lineWidth = size * 0.08f
    val track = palette.subtle(0.08f)
    val sweep = Brush.sweepGradient(listOf(palette.accent, palette.accentSecondary, palette.nebula[0], palette.accent))
    val glow = palette.accent.copy(alpha = 0.18f)
    Box(
        Modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = lineWidth.toPx()
            val inset = stroke / 2
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            if (progress > 0f) {
                rotate(-90f) {
                    // 环外一圈淡淡的光
                    drawArc(glow, 0f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(stroke * 1.9f, cap = StrokeCap.Round))
                    drawArc(sweep, 0f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // 字号按环的大小算（环已经随系统字号放大过，这里不再叠加）
            Text(
                "${(progress * 100).roundToInt()}",
                style = TextStyle(fontSize = with(density) { (size * 0.32f).toSp() }, fontWeight = FontWeight.Bold),
                color = palette.text,
            )
            Text(
                "运势指数",
                style = TextStyle(fontSize = with(density) { (size * 0.09f).toSp() }, fontWeight = FontWeight.SemiBold),
                color = palette.secondaryText,
            )
        }
    }
}

// endregion

// region ③ 今日一签

@Composable
private fun RitualStickPage(stick: FortuneStick?, onDraw: () -> Unit) {
    val palette = LocalPalette.current
    RitualPageScroll {
        if (stick != null) {
            GlassCard(
                Modifier.semantics(mergeDescendants = true) {},
                padding = 22.dp,
                spacing = 14.dp,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stick.numberText, style = Fonts.serif(28, bold = true), color = palette.text, maxLines = 1)
                    Text(
                        stick.levelDisplayName,
                        style = Fonts.subheadline.weight(FontWeight.Bold),
                        color = stickLevelInk(stick.level, palette.isDark),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(stickLevelColor(stick.level).copy(alpha = 0.25f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                SecondaryText("「${stick.title}」", style = Fonts.serif(22), textAlign = TextAlign.Center)
                Column(
                    Modifier.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (line in stick.poem) Text(line, style = Fonts.serif(19), color = palette.text, textAlign = TextAlign.Center)
                }
                SecondaryText(stick.meaning, textAlign = TextAlign.Center)
            }
            SecondaryText("今天已经求过签了，首页的「今日一签」随时能再看。", style = Fonts.footnote, textAlign = TextAlign.Center)
        } else {
            Box(
                Modifier
                    .size(100.dp, 116.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(hex(0xB91C1C), hex(0x7F1D1D))))
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    null,
                    Modifier
                        .size(48.dp)
                        .gradientTint(Brush.verticalGradient(listOf(hex(0xFFD166), hex(0xFF8A3D)))),
                    tint = Color.White,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("今天还没有求签", style = Fonts.title2, color = palette.text, textAlign = TextAlign.Center)
                SecondaryText("心诚则灵，摇一摇签筒，看看今天的指引。", style = Fonts.body, textAlign = TextAlign.Center)
            }
            GlassButton("去求签", Icons.Rounded.LocalFireDepartment, onClick = onDraw)
            SecondaryText("也可以先继续，之后在首页或「问一问」里随时求签。", style = Fonts.footnote, textAlign = TextAlign.Center)
        }
    }
}

// endregion

// region ④ 心情

@Composable
private fun RitualMoodPage(mood: MoodLevel?, onSelect: (MoodLevel) -> Unit) {
    RitualPageScroll {
        Text(mood?.emoji ?: "🌙", style = TextStyle(fontSize = 72.sp), modifier = Modifier.clearAndSetSemantics { })
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("今天心情怎么样？", style = Fonts.title2, color = LocalPalette.current.text, textAlign = TextAlign.Center)
            SecondaryText("点一下就好，之后还能改。", textAlign = TextAlign.Center)
        }
        GlassCard(padding = 14.dp) {
            MoodPicker(selected = mood?.value, size = 54.dp, onSelect = onSelect)
        }
        if (mood != null) {
            SecondaryText(
                "已记下「${mood.label}」。想多写几句，可以去「日记」里的心情页。",
                style = Fonts.footnote,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// endregion

// region ⑤ 今日卡片

@Composable
private fun RitualSummaryPage(day: DayKey, moon: MoonInfo, fortune: DailyFortune, stick: FortuneStick?, mood: MoodLevel?) {
    val palette = LocalPalette.current
    var sharing by rememberSaveable { mutableStateOf(false) }
    RitualPageScroll {
        GlassCard(spacing = 16.dp) {
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${DateText.monthDay(day)} ${DateText.weekday(day)}", style = Fonts.title3.weight(FontWeight.Bold), color = palette.text)
                SecondaryText("农历${day.lunar().text}")
            }
            HorizontalDivider(color = palette.subtle(0.12f))
            RitualSummaryRow("月亮", "${moon.name.label} · 亮面 ${moon.percent}%") {
                MoonPhaseView(moon.phase, size = 34.dp, glow = false)
            }
            RitualSummaryRow("运势", "${fortune.score} 分 · ${fortune.levelName}") {
                GradientIcon(Icons.Rounded.AutoAwesome, size = 28.dp)
            }
            RitualSummaryRow(
                "今日一签",
                stick?.let { "${it.numberText} · ${it.levelDisplayName} · 「${it.title}」" } ?: "还没有求签",
            ) {
                Icon(
                    Icons.Rounded.LocalFireDepartment,
                    null,
                    Modifier
                        .size(28.dp)
                        .gradientTint(Brush.verticalGradient(listOf(hex(0xFF8A3D), hex(0xE11D48)))),
                    tint = Color.White,
                )
            }
            RitualSummaryRow("心情", mood?.let { "${it.emoji} ${it.label}" } ?: "还没有记录") {
                Text(mood?.emoji ?: "🙂", style = Fonts.title, modifier = Modifier.alpha(if (mood == null) 0.45f else 1f))
            }
            HorizontalDivider(color = palette.subtle(0.12f))
            Text(fortune.quote, style = Fonts.serif(17).copy(lineHeight = 26.sp), color = palette.text)
        }
        GlassButton("分享今日卡片", Icons.Rounded.Share) { sharing = true }
    }
    if (sharing) {
        ShareCardDialog("星匣 · 今日", "ritual", onDismiss = { sharing = false }) { style ->
            ShareCardFrame(style, day = day, title = "星匣 · 今日") {
                RitualShareContent(moon, fortune, stick, mood, style)
            }
        }
    }
}

@Composable
private fun RitualSummaryRow(title: String, value: String, icon: @Composable () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) { icon() }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SecondaryText(title, style = Fonts.caption)
            Text(value, style = Fonts.body.weight(FontWeight.SemiBold), color = palette.text)
        }
    }
}

/** 「星匣 · 今日」分享卡的内容：月相、运势、签和心情。字号固定，导出的图每次一样 */
@Composable
fun RitualShareContent(moon: MoonInfo, fortune: DailyFortune, stick: FortuneStick?, mood: MoodLevel?, style: ShareCardStyle) {
    val tall = style.isTall
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (tall) 20.dp else 14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MoonPhaseView(moon.phase, size = if (tall) 104.dp else 84.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(moon.name.label, style = Fonts.serif(if (tall) 30 else 26, bold = true), color = Color.White)
                Text(
                    "今晚亮面 ${moon.percent}%",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f)),
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ShareScoreRing(fortune.score, if (tall) 104.dp else 84.dp, if (tall) 10.dp else 8.dp, "运势")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GradientText(fortune.levelName, style = Fonts.display(if (tall) 32 else 28, FontWeight.Black))
                StarRatingView(fortune.stars, size = 13.dp)
            }
        }

        if (tall) {
            Text(
                fortune.summary,
                style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (stick != null || mood != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (stick != null) {
                    val seal = if (stick.level == "下") "下签" else stick.level
                    ShareChip("今日一签 · ${stick.numberText} · $seal · ${stick.title}", stickLevelColor(stick.level))
                }
                if (mood != null) ShareChip("心情 ${mood.emoji} ${mood.label}", mood.color)
            }
        }

        Text(
            fortune.quote,
            style = Fonts.serif(if (tall) 16 else 14).copy(
                lineHeight = if (tall) 23.sp else 20.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            ),
            maxLines = if (tall) 3 else 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// endregion
