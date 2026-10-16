@file:OptIn(ExperimentalLayoutApi::class)

package com.tianaofeng.xingxia.ui.screens.divination

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.AlmanacDay
import com.tianaofeng.xingxia.core.AuspiciousDay
import com.tianaofeng.xingxia.core.AuspiciousDays
import com.tianaofeng.xingxia.core.AuspiciousEvent
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneCategory
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.FortuneInsightKind
import com.tianaofeng.xingxia.core.HoroscopeEngine
import com.tianaofeng.xingxia.core.HoroscopePeriod
import com.tianaofeng.xingxia.core.MoonInfo
import com.tianaofeng.xingxia.core.Shensha
import com.tianaofeng.xingxia.core.SolarTerms
import com.tianaofeng.xingxia.core.StarRating
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Prefs
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.ColorDot
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.GradientText
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.MeterBar
import com.tianaofeng.xingxia.ui.components.ScoreRing
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.StarRatingView
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.common.QuoteCard
import com.tianaofeng.xingxia.ui.screens.common.StickTubeIllustration
import com.tianaofeng.xingxia.ui.screens.common.TarotBackMini
import com.tianaofeng.xingxia.ui.screens.common.YiJiRow
import com.tianaofeng.xingxia.ui.screens.common.ZodiacBadge
import com.tianaofeng.xingxia.ui.screens.common.icon
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.toColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.ZoneId

// region 占卜首页

@Composable
fun DivinationHubScreen() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    Screen(title = "占卜", isRoot = true, spacing = 14.dp) {
        // 摇签筒
        GlassCard(tint = hex(0xB91C1C, 0.12f), onClick = { navigator.open(Routes.STICK) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("心诚则灵 · 一日一签", style = Fonts.caption.weight(FontWeight.SemiBold), color = palette.ink(hex(0xF59E0B)))
                    Text("摇签筒", style = Fonts.title)
                    SecondaryText("默念心中所求，摇动手机，抽出今天的指引")
                }
                StickTubeIllustration(96.dp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val tarotSubtitle = if (AppSettings.tarotFullDeck) "${ContentLibrary.tarot.size} 张 · 四种牌阵" else "大阿卡纳 · 四种牌阵"
            SmallHeroCard("塔罗占卜", tarotSubtitle, hex(0x6D28D9), Modifier.weight(1f), { navigator.open(Routes.TAROT) }) {
                TarotBackMini()
            }
            SmallHeroCard("答案之书", "默念问题 · 翻开一页", hex(0xD97706), Modifier.weight(1f), { navigator.open(Routes.ANSWERS) }) {
                Icon(Icons.Rounded.MenuBook, null, Modifier.size(52.dp), tint = hex(0xF5B83D))
            }
        }

        GlassCard(onClick = { navigator.open(Routes.HOROSCOPE) }) {
            SectionHeader("星座运势", subtitle = "今日 · 明日 · 本周 · 本月", icon = Icons.Rounded.NightsStay) {
                Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
            }
            // 12 星座两行排开，只作装饰：整张卡是一个入口，读屏只读标题
            Column(Modifier.clearAndSetSemantics { }, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (row in ZodiacSign.entries.chunked(6)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (sign in row) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                ZodiacBadge(sign, 38.dp)
                                SecondaryText(sign.shortName, style = Fonts.caption2, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        val tiles = listOf(
            MiniTileData(Routes.FORTUNE, Icons.Rounded.AutoAwesome, "运势详解", "五维运势与幸运指南", listOf(palette.accent, palette.accentSecondary), palette.onAccent),
            MiniTileData(Routes.ALMANAC, Icons.Rounded.CalendarMonth, "黄历", "农历 · 节气 · 宜忌", listOf(hex(0xEF4444), hex(0xF59E0B))),
            MiniTileData(Routes.BIRTH_CHART, Icons.Rounded.GridOn, "命盘", "八字 · 星盘三巨头", listOf(hex(0xA78BFA), hex(0xEC4899))),
            MiniTileData(Routes.COMPATIBILITY, Icons.Rounded.Favorite, "缘分配对", "星座 + 生肖", listOf(hex(0xFB7185), hex(0xF43F5E))),
        )
        for (row in tiles.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (tile in row) {
                    GlassCard(Modifier.weight(1f), padding = 16.dp, spacing = 10.dp, onClick = { navigator.open(tile.route) }) {
                        MiniTileIcon(tile)
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(tile.title, style = Fonts.headline)
                            SecondaryText(tile.subtitle, style = Fonts.caption2, maxLines = 1)
                        }
                    }
                }
            }
        }

        AuspiciousHubCard()
    }
}

/** 择吉日入口：附上最近 30 天里办上次所选事项（择吉日页记下的 "auspiciousEvent"）的首选日子 */
@Composable
private fun AuspiciousHubCard() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val today = rememberToday()
    val eventKey = remember { Prefs.string(AUSPICIOUS_EVENT_KEY, AuspiciousEvent.MOVING.key) }
    val event = AuspiciousEvent.of(eventKey.value) ?: AuspiciousEvent.MOVING
    val animal = Store.primary?.animal
    val teaser by produceState<AuspiciousDay?>(null, event, today, animal) {
        value = withContext(Dispatchers.Default) { AuspiciousDays.find(event, today, days = 30, animal = animal, limit = 1).firstOrNull() }
    }
    val headline = teaser?.let { "近 30 天最宜${event.label}：${DateText.monthDay(it.day)} ${DateText.weekday(it.day)}" }
    GlassCard(onClick = { navigator.open(Routes.AUSPICIOUS) }) {
        SectionHeader("择吉日", subtitle = "搬家 · 开业 · 领证 · 出行", icon = Icons.Rounded.EventAvailable) {
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
        // 事项图案只作装饰
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }) {
            for (item in AuspiciousEvent.entries.take(5)) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(palette.accent.copy(alpha = if (palette.isDark) 0.14f else 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(item.emoji, style = Fonts.title3) }
                }
            }
        }
        val day = teaser
        if (headline != null && day != null) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(headline, style = Fonts.subheadline.weight(FontWeight.SemiBold))
                SecondaryText(day.summary, style = Fonts.caption, maxLines = 2)
            }
        }
    }
}

/** iconTint：底色用主题强调色时传 palette.onAccent，深色主题的强调色偏亮，白色图标看不清 */
private data class MiniTileData(val route: String, val icon: ImageVector, val title: String, val subtitle: String, val colors: List<Color>, val iconTint: Color = Color.White)

/** 和 ToolIcon 同款的渐变方块图标，但图标颜色可调 */
@Composable
private fun MiniTileIcon(tile: MiniTileData) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(40.dp)
            .clip(shape)
            .background(Brush.linearGradient(tile.colors))
            .border(0.8.dp, Color.White.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(tile.icon, null, Modifier.size(18.dp), tint = tile.iconTint)
    }
}

@Composable
private fun SmallHeroCard(title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit, art: @Composable () -> Unit) {
    GlassCard(modifier, tint = color.copy(alpha = 0.14f), onClick = onClick, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(90.dp), contentAlignment = Alignment.Center) { art() }
        Text(title, style = Fonts.headline)
        SecondaryText(subtitle, style = Fonts.caption2, maxLines = 1)
    }
}

// endregion

// region 运势详解

@Composable
fun FortuneDetailScreen() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    var offset by remember { mutableIntStateOf(0) }
    // 过零点、隔天回到前台时「今天」跟着换
    val today = rememberToday()
    val day = today.adding(offset)
    val birth = Store.birth
    val fortune = remember(day, birth) { FortuneEngine.daily(birth, day) }
    // 看的不是今天时，页面自己拼的「今日…」改成「昨日 / 明日」（JSON 文案本身不含相对日期）
    val dayWord = relativeDayWord(offset)
    // 当天晚上 21:00（北京时间）的月相，与 iOS 一致
    val moon = remember(day) { MoonInfo.on(day) }
    val moonText = "${dayWord}月相为${moon.name.label}，亮面约 ${moon.percent}%，月亮行经${fortune.moonSign.label}。"
    val ownerName = Store.primary?.name
    // 分享卡标题：看的不是今天时写具体日期
    val shareTitle = if (offset == 0) "星匣 · 今日运势" else "星匣 · ${DateText.monthDay(day)}运势"

    Screen(
        title = "运势详解",
        actions = {
            ShareCardButton("分享运势", "fortune") { style ->
                ShareCardFrame(style, day, shareTitle) {
                    FortuneShareContent(fortune, ownerName, style)
                }
            }
        },
    ) {
        SegmentedPicker(listOf(-1, 0, 1), offset, { listOf("昨天", "今天", "明天")[it + 1] }) { offset = it }

        GlassCard(padding = 22.dp, horizontalAlignment = Alignment.CenterHorizontally, spacing = 14.dp) {
            SecondaryText("${DateText.full(day)} ${DateText.weekday(day)}", style = Fonts.footnote)
            ScoreRing(fortune.score, size = 170.dp, lineWidth = 16.dp, label = "运势指数")
            GradientText(fortune.levelName, style = Fonts.display(44, FontWeight.Black))
            StarRatingView(fortune.stars, size = 18.dp)
            Text(fortune.summary, style = Fonts.body, textAlign = TextAlign.Center)
        }

        if (fortune.insights.isNotEmpty()) {
            GlassCard {
                SectionHeader("运势解读", subtitle = "依据你的八字与${dayWord}天象", icon = Icons.Rounded.Explore)
                fortune.insights.forEachIndexed { index, text ->
                    InsightRow(text) { GradientIcon(fortune.insightKinds.getOrNull(index).icon, size = 20.dp) }
                }
                // 月亮星座那条解读总在最后，月相紧跟其后
                InsightRow(moonText) { Text(moon.name.emoji, style = Fonts.subheadline) }
            }
        } else {
            GlassCard { InsightRow(moonText) { Text(moon.name.emoji, style = Fonts.subheadline) } }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassButton("填写生日后可查看基于八字与星座的专属解读", Icons.Rounded.Lock) { navigator.open(Routes.PROFILES) }
            }
        }

        for (category in FortuneCategory.entries) {
            val value = fortune.score(category)
            GlassCard(spacing = 10.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(palette.accentBrush),
                        contentAlignment = Alignment.Center,
                    ) { Icon(category.icon, null, Modifier.size(16.dp), tint = palette.onAccent) }
                    Text(category.label, style = Fonts.headline)
                    StarRatingView(StarRating.stars(value), size = 11.dp)
                    Box(Modifier.weight(1f))
                    GradientText("$value", style = Fonts.title3.weight(FontWeight.Bold))
                }
                MeterBar(value)
                Text(fortune.text(category), style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f))
            }
        }

        GlassCard(spacing = 14.dp) {
            SectionHeader("幸运指南", icon = Icons.Rounded.AutoAwesome)
            val cells: List<LuckyCellData> = listOf(
                LuckyCellData("幸运色", fortune.luckyColor.name, fortune.luckyColor.hexString) { ColorDot(fortune.luckyColor.toColor(), 28.dp, glow = true) },
                LuckyCellData("幸运数字", "${fortune.luckyNumber}", "五行属${fortune.favorableElement.label}") { GradientIcon(Icons.Rounded.Tag, 28.dp) },
                LuckyCellData("喜神方位", fortune.luckyDirection, "财神 ${fortune.wealthDirection}") { GradientIcon(Icons.Rounded.Explore, 28.dp) },
                LuckyCellData("吉时", fortune.luckyHour.hourName, fortune.luckyHour.hourRange) { GradientIcon(Icons.Rounded.AccessTime, 28.dp) },
                LuckyCellData("幸运物", fortune.luckyItem, "带在身边") { GradientIcon(Icons.Rounded.Redeem, 28.dp) },
                LuckyCellData("贵人星座", fortune.noblemanSign.label, "${dayWord}月亮在${fortune.moonSign.shortName}") {
                    Text(fortune.noblemanSign.glyph, style = Fonts.title.copy(brush = palette.accentBrush))
                },
            )
            for (row in cells.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (cell in row) LuckyCell(cell, Modifier.weight(1f))
                }
            }
        }

        GlassCard {
            SectionHeader("趣味宜忌", icon = Icons.Rounded.SentimentSatisfied)
            YiJiRow(true, fortune.funYi)
            YiJiRow(false, fortune.funJi)
        }

        QuoteCard(fortune.quote)
    }
}

private class LuckyCellData(val title: String, val value: String, val caption: String, val icon: @Composable () -> Unit)

/** 解读图标按来源取，不按顺序：生肖提示为空时月亮那条不会配上爪印 */
private val FortuneInsightKind?.icon: ImageVector
    get() = when (this) {
        FortuneInsightKind.STEM -> Icons.Rounded.WbSunny
        FortuneInsightKind.BRANCH -> Icons.Rounded.Pets
        FortuneInsightKind.MOON -> Icons.Rounded.NightsStay
        null -> Icons.Rounded.AutoAwesome
    }

@Composable
private fun InsightRow(text: String, icon: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(20.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) { icon() }
        Text(text, style = Fonts.subheadline)
    }
}

/** 神煞标签色：吉神用金色，四离、四绝这类凶日沿用黑道日的灰色（与 iOS 一致） */
private fun shenshaColor(name: String, palette: ThemePalette): Color = if (Shensha.isLucky(name)) palette.gold else hex(0x94A3B8)

/** 相对今天的称呼：0 今日、-1 昨日、1 明日，其余为当日 */
private fun relativeDayWord(daysFromToday: Int): String = when (daysFromToday) {
    0 -> "今日"
    -1 -> "昨日"
    1 -> "明日"
    else -> "当日"
}

@Composable
private fun LuckyCell(data: LuckyCellData, modifier: Modifier) {
    val palette = LocalPalette.current
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.subtle(0.05f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) { data.icon() }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SecondaryText(data.title, style = Fonts.caption2)
            Text(data.value, style = Fonts.subheadline.weight(FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            SecondaryText(data.caption, style = Fonts.caption2, maxLines = 1)
        }
    }
}

// endregion

// region 黄历

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacDetailScreen() {
    val palette = LocalPalette.current
    val navigator = LocalNavigator.current
    val today = rememberToday()
    var day by remember { mutableStateOf(today) }
    // 跨过零点、隔天回到前台时：原本停在今天的跟着换到新的一天，翻到别的日期的不动
    var shownToday by remember { mutableStateOf(today) }
    LaunchedEffect(today) {
        if (today != shownToday) {
            if (day == shownToday) day = today
            shownToday = today
        }
    }
    var showPicker by remember { mutableStateOf(false) }
    val almanac = remember(day) { AlmanacDay(day) }
    val birth = Store.birth
    val fortune = remember(day, birth) { FortuneEngine.daily(birth, day) }
    // 翻到别的日期时，「今日」改成「昨日 / 明日 / 当日」
    val dayWord = relativeDayWord(today.daysTo(day))

    fun move(delta: Int) {
        Haptics.selection()
        day = day.adding(delta)
    }

    Screen(
        title = "黄历",
        actions = { if (day != today) TextAction("今天") { day = today } },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    var total = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total < -150) move(1) else if (total > 150) move(-1) },
                    ) { _, amount -> total += amount }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.Rounded.ChevronLeft, "前一天") { move(-1) }
            Column(
                Modifier
                    .weight(1f)
                    .pressable { showPicker = true },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${DateText.full(day)} ${DateText.weekday(day)}", style = Fonts.headline)
                SecondaryText("点按选择日期 · 左右滑动翻页", style = Fonts.caption2)
            }
            CircleIconButton(Icons.Rounded.ChevronRight, "后一天") { move(1) }
        }

        GlassCard(padding = 22.dp, horizontalAlignment = Alignment.CenterHorizontally, spacing = 10.dp) {
            Text(almanac.lunar.monthName + almanac.lunar.dayName, style = Fonts.serif(46, bold = true).copy(brush = palette.accentBrush))
            Text(almanac.ganzhiText, style = Fonts.subheadline.weight(FontWeight.Medium))
            val year = almanac.lunar.yearGanZhi
            SecondaryText("${year.branch.animalEmoji} ${year.name}${year.branch.animal}年 · 纳音${almanac.day.nayin}", style = Fonts.footnote)
            Flow(spacing = 8.dp, horizontalAlignment = Alignment.CenterHorizontally) {
                TagChip(almanac.jianChu.name, if (almanac.jianChu.isLucky) hex(0x34D399) else hex(0xFBBF24))
                TagChip("${almanac.deity.name} · ${almanac.deity.pathName}", if (almanac.deity.isYellowPath) hex(0xFBBF24) else hex(0x94A3B8))
            }
            if (almanac.shensha.isNotEmpty()) {
                // 神煞多时（大字号下）放不下就换行；读屏合成一句
                Flow(
                    spacing = 8.dp,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = "神煞：" + almanac.shensha.joinToString("、") },
                ) {
                    for (name in almanac.shensha) {
                        TagChip(name, shenshaColor(name, palette), if (Shensha.isLucky(name)) Icons.Rounded.AutoAwesome else Icons.Rounded.WarningAmber)
                    }
                }
            }
            if (almanac.festivals.isNotEmpty()) {
                Flow(spacing = 8.dp, horizontalAlignment = Alignment.CenterHorizontally) {
                    for (f in almanac.festivals) TagChip(f, hex(0xFB7185))
                }
            }
        }

        GlassCard(spacing = 14.dp) {
            SectionHeader("黄历宜忌", icon = Icons.Rounded.MenuBook)
            SecondaryText(almanac.jianChu.summary, style = Fonts.caption)
            YiJiRow(true, almanac.yi)
            YiJiRow(false, almanac.ji)
            if (almanac.shensha.isNotEmpty()) {
                HorizontalDivider(color = palette.subtle(0.12f))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (name in almanac.shensha) {
                        Row(Modifier.semantics(mergeDescendants = true) { }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(name, style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.ink(shenshaColor(name, palette)))
                            SecondaryText(Shensha.NOTES[name] ?: "")
                        }
                    }
                }
            }
        }

        val infos = listOf(
            Triple("冲煞", almanac.clash, Icons.Rounded.SwapHoriz),
            Triple("值神", "${almanac.deity.name}（${if (almanac.deity.isYellowPath) "吉" else "凶"}）", Icons.Rounded.Verified),
            Triple("喜神方位", almanac.joyDirection, Icons.Rounded.SentimentSatisfied),
            Triple("财神方位", almanac.wealthDirection, Icons.Rounded.Paid),
            Triple("日柱纳音", almanac.day.nayin, Icons.Rounded.Waves),
            Triple("${dayWord}冲", "属${almanac.clashAnimal.animal}的朋友多注意", Icons.Rounded.Shield),
        )
        for (row in infos.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for ((title, value, icon) in row) {
                    GlassCard(Modifier.weight(1f), cornerRadius = 22.dp, padding = 14.dp, spacing = 8.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(icon, null, Modifier.size(14.dp), tint = palette.secondaryText)
                            SecondaryText(title, style = Fonts.caption)
                        }
                        Text(value, style = Fonts.subheadline.weight(FontWeight.SemiBold))
                    }
                }
            }
        }

        GlassCard(spacing = 10.dp) {
            SectionHeader("彭祖百忌", icon = Icons.Rounded.MenuBook)
            for (line in almanac.pengzu) Text(line, style = Fonts.serif(17))
        }

        GlassCard {
            SectionHeader("节气", icon = Icons.Rounded.Spa)
            val current = almanac.solarTerm ?: SolarTerms.previous(day)
            Text(if (almanac.solarTerm != null) "$dayWord ${current.name}" else "当前节气 · ${current.name}", style = Fonts.headline)
            SecondaryText(SolarTerms.BLURBS[current.name] ?: "")
            HorizontalDivider(color = palette.subtle(0.12f))
            val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
            var cursor = day
            repeat(3) {
                val term = SolarTerms.next(cursor)
                cursor = term.day
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(term.name, style = Fonts.subheadline.weight(FontWeight.SemiBold), modifier = Modifier.weight(1f))
                    SecondaryText("${term.day.month}月${term.day.day}日 ${term.moment.atZone(ZoneId.systemDefault()).format(timeFormat)}", style = Fonts.caption)
                    Text("${day.daysTo(term.day)}天后", style = Fonts.caption, color = palette.ink(palette.accent))
                }
            }
        }

        GlassCard {
            SectionHeader("趣味宜忌", subtitle = Store.primary?.let { "专属于${it.name}" }, icon = Icons.Rounded.AutoAwesome)
            YiJiRow(true, fortune.funYi)
            YiJiRow(false, fortune.funJi)
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButton("择吉日：挑个办事的好日子", Icons.Rounded.EventAvailable) { navigator.open(Routes.AUSPICIOUS) }
        }

        SecondaryText(
            "黄历宜忌按「建除十二神」「黄黑道值神」推算，并参考天德、月德、天赦、四离四绝等神煞，仅作传统文化参考。",
            style = Fonts.caption2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { day = DayKey.of(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
                    showPicker = false
                }) { Text("完成") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("取消") } },
        ) {
            DatePicker(state = state)
        }
    }
}

// endregion

// region 星座运势

@Composable
fun HoroscopeScreen() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val initial = remember {
        val stored = AppSettings.lastViewedSign
        val birth = Store.birth
        when {
            birth != null && stored < 0 -> ZodiacSign.sunSign(birth)
            stored in 0..11 -> ZodiacSign.entries[stored]
            birth != null -> ZodiacSign.sunSign(birth)
            else -> ZodiacSign.ARIES
        }
    }
    var sign by remember { mutableStateOf(initial) }
    var period by remember { mutableStateOf(HoroscopePeriod.TODAY) }
    // 过零点、隔天回到前台时按新的一天重算
    val today = rememberToday()
    val horoscope = remember(sign, period, today) { HoroscopeEngine.horoscope(sign, period, today) }
    val scroll = rememberScrollState()

    Screen(title = "星座运势") {
        Row(Modifier.horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (item in ZodiacSign.entries) {
                val selected = item == sign
                Column(
                    Modifier
                        .width(60.dp)
                        .height(84.dp)
                        .pressable {
                            sign = item
                            AppSettings.lastViewedSign = item.ordinal
                            Haptics.selection()
                        }
                        // 读屏只读星座全名和选中状态；点按动作来自前面的 pressable，要放在它后面
                        .clearAndSetSemantics {
                            contentDescription = item.label
                            role = Role.Button
                            this.selected = item == sign
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    ZodiacBadge(item, if (selected) 56.dp else 44.dp, Modifier.then(if (selected) Modifier else Modifier.background(Color.Transparent)))
                    Text(
                        item.shortName,
                        style = Fonts.caption2.weight(if (selected) FontWeight.Bold else FontWeight.Normal),
                        color = if (selected) palette.text else palette.secondaryText,
                    )
                }
            }
        }

        SegmentedPicker(HoroscopePeriod.entries, period, { it.label }) { period = it }

        GlassCard(spacing = 14.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ZodiacBadge(sign, 84.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sign.label, style = Fonts.title)
                    SecondaryText("${sign.englishName} · ${sign.dateRange}", style = Fonts.caption)
                    StarRatingView(horoscope.stars, size = 15.dp)
                    // 窄列放不下一行时分两行，避免日期范围最后两个字单独掉到下一行
                    OneLineOrSplit("${period.label}运势", horoscope.rangeText, Fonts.caption)
                }
                ScoreRing(horoscope.score, size = 70.dp, lineWidth = 7.dp)
            }
            Text(horoscope.overview, style = Fonts.body)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                GradientIcon(Icons.Rounded.NightsStay, 16.dp)
                SecondaryText(horoscope.skyNote, style = Fonts.caption)
            }
        }

        GlassCard(spacing = 14.dp) {
            FortuneCategory.entries.forEachIndexed { index, category ->
                val value = horoscope.score(category)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(category.icon, null, Modifier.size(16.dp), tint = palette.accent)
                        Text(category.shortName + "运", style = Fonts.subheadline.weight(FontWeight.SemiBold), modifier = Modifier.weight(1f))
                        StarRatingView(StarRating.stars(value), size = 11.dp)
                    }
                    Text(horoscope.categoryTexts[category] ?: "", style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f))
                }
                if (index < FortuneCategory.entries.size - 1) HorizontalDivider(color = palette.subtle(0.1f))
            }
        }

        // 大字号下「星座幸运色」可能折成两行，四个气泡统一取最高的那个高度
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // 和运势卡按五行算的「幸运色」区分开
            LuckyBubble("星座幸运色", Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ColorDot(horoscope.luckyColor.toColor(), 12.dp)
                    Text(horoscope.luckyColor.name, style = Fonts.subheadline.weight(FontWeight.SemiBold), maxLines = 1)
                }
            }
            LuckyBubble("幸运数字", Modifier.weight(1f)) { Text("${horoscope.luckyNumber}", style = Fonts.subheadline.weight(FontWeight.SemiBold)) }
            LuckyBubble("速配星座", Modifier.weight(1f)) { Text(horoscope.matchSign.shortName, style = Fonts.subheadline.weight(FontWeight.SemiBold)) }
            LuckyBubble("提防星座", Modifier.weight(1f)) { Text(horoscope.cautionSign.shortName, style = Fonts.subheadline.weight(FontWeight.SemiBold)) }
        }

        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                GradientIcon(Icons.Rounded.FormatQuote, 20.dp)
                Text("${period.label}提醒：${horoscope.comment}", style = Fonts.subheadline.weight(FontWeight.Medium))
            }
        }

        GlassCard {
            SectionHeader("星座档案", icon = Icons.Rounded.AutoAwesome)
            Flow(spacing = 8.dp) {
                TagChip(sign.element.label)
                TagChip(sign.modality.label, palette.accentSecondary)
                TagChip("守护星 ${sign.ruler}", palette.nebula[2])
            }
            SecondaryText(sign.keywords.joinToString("  ") { "#$it" }, style = Fonts.caption)
            Text(sign.profile, style = Fonts.subheadline)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                SecondaryText("最佳拍档", style = Fonts.caption)
                for (t in sign.trines) Text(t.label, style = Fonts.caption.weight(FontWeight.SemiBold))
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButton("查看星座配对", Icons.Rounded.Favorite) { navigator.open(Routes.COMPATIBILITY) }
        }
    }
}

/** 「first · second」放得下就一行，放不下拆成 first 和 second 两行（对应 iOS 的 ViewThatFits） */
@Composable
private fun OneLineOrSplit(first: String, second: String, style: TextStyle) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val joined = "$first · $second"
        val maxPx = constraints.maxWidth
        val fits = remember(joined, style, maxPx, measurer) {
            measurer.measure(joined, style, maxLines = 1, softWrap = false).size.width <= maxPx
        }
        if (fits) {
            SecondaryText(joined, style = style, maxLines = 1)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SecondaryText(first, style = style)
                SecondaryText(second, style = style)
            }
        }
    }
}

@Composable
private fun LuckyBubble(title: String, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxHeight()
            .glass(RoundedCornerShape(18.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SecondaryText(title, style = Fonts.caption2, textAlign = TextAlign.Center)
        content()
    }
}

// endregion
