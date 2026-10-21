package com.tianaofeng.xingxia.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.BirthInfo
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.DecisionSample
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.JournalStats
import com.tianaofeng.xingxia.core.MonthlyReview
import com.tianaofeng.xingxia.core.MoodSample
import com.tianaofeng.xingxia.core.lunar
import com.tianaofeng.xingxia.core.roundHalfAway
import com.tianaofeng.xingxia.data.DecisionRecord
import com.tianaofeng.xingxia.data.MoodEntry
import com.tianaofeng.xingxia.data.StickDraw
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.data.TarotReading
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.DayKeySaver
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GlowIcon
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.rootBottomPadding
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.common.stickLevelColor
import com.tianaofeng.xingxia.ui.screens.common.stickLevelInk
import com.tianaofeng.xingxia.ui.screens.divination.TarotSpread
import com.tianaofeng.xingxia.ui.screens.divination.decodeTarotCards
import com.tianaofeng.xingxia.ui.screens.records.MoodLevel
import com.tianaofeng.xingxia.ui.screens.tools.toolInfo
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.Locale

// region 路由与日期小工具

/** 年度回顾的路由（Navigation.kt 里注册为 "annualReview/{year}"） */
internal fun annualReviewRoute(year: Int): String = "annualReview/$year"

/** 默认回顾哪一年：1 月看去年，其余看今年（与 iOS 的 AppRoute.reviewYear 一致） */
internal fun reviewYear(today: DayKey = DayKey.today): Int = if (today.month == 1) today.year - 1 else today.year

private fun firstDayOf(day: DayKey) = DayKey(day.year, day.month, 1)

/** 前后挪几个月，返回那个月的 1 号 */
private fun shiftMonth(month: DayKey, delta: Int): DayKey {
    val total = month.year * 12 + (month.month - 1) + delta
    return DayKey(Math.floorDiv(total, 12), Math.floorMod(total, 12) + 1, 1)
}

internal fun dayOf(millis: Long): DayKey = DayKey.of(Instant.ofEpochMilli(millis))

/** 决策记录的值拷贝，question 按 askedQuestion 的规则取 */
internal fun DecisionRecord.sample(): DecisionSample =
    DecisionSample.of(tool, title, result, question, date, adopted, satisfaction)

/** 「上上签」「下签」 */
internal fun stickLevelName(level: String): String = if (level == "下") "下签" else level + "签"

// endregion

/**
 * 日记 tab：月份切换、本月回顾、快捷入口，下面是按天倒序的时间线（心情、决定、签、塔罗），只组合屏幕上的部分。
 * 12 月和 1 月顶部多一条年度回顾横幅（与 iOS 的 JournalView 一致）
 */
@Composable
fun JournalScreen() {
    val navigator = LocalNavigator.current
    // 过零点、回到前台时刷新
    val today = rememberToday()
    /** 正在看的月份（1 号） */
    var month by rememberSaveable(stateSaver = DayKeySaver) { mutableStateOf(firstDayOf(DayKey.today)) }
    // 和 month 一起保存：切 tab 回来时页面会重建，过了零点也要能认出「停在本月」
    var shownToday by rememberSaveable(stateSaver = DayKeySaver) { mutableStateOf(today) }
    LaunchedEffect(today) {
        if (today != shownToday) {
            // 停在「本月」时跟着翻到新的月份
            if (month == firstDayOf(shownToday)) month = firstDayOf(today)
            shownToday = today
        }
    }
    val isCurrentMonth = month == firstDayOf(today)
    val monthKey = month.year * 100 + month.month
    val shownMonth = month

    val records = Store.records.items
    val moodEntries = Store.moods.items
    val draws = Store.stickDraws.items
    val readings = Store.tarotReadings.items
    val monthRecords = remember(records, monthKey) { records.filter { dayOf(it.date).let { d -> d.year * 100 + d.month } == monthKey } }
    val monthMoods = remember(moodEntries, monthKey) { moodEntries.filter { it.dayKey / 100 == monthKey } }
    val monthDraws = remember(draws, monthKey) { draws.filter { it.dayKey / 100 == monthKey } }
    val monthReadings = remember(readings, monthKey) { readings.filter { dayOf(it.date).let { d -> d.year * 100 + d.month } == monthKey } }
    val days = remember(monthRecords, monthMoods, monthDraws, monthReadings) {
        JournalDay.group(monthRecords, monthMoods, monthDraws, monthReadings)
    }

    // 这个月每天的运势分数（按当前档案），在后台算好存起来；没算好之前现算
    val birth = Store.birth
    val cache by produceState<ScoreCache?>(null, monthKey, birth, today) {
        val first = shownMonth
        val last = today
        val owner = birth
        val scores = withContext(Dispatchers.Default) {
            val result = HashMap<Int, Int>()
            var day = first
            while (day.month == first.month && day <= last) {
                result[day.rawValue] = FortuneEngine.daily(owner, day).score
                day = day.adding(1)
            }
            result
        }
        value = ScoreCache(monthKey, owner, scores)
    }
    fun score(day: DayKey): Int =
        cache?.takeIf { it.month == monthKey && it.birth == birth }?.scores?.get(day.rawValue) ?: FortuneEngine.daily(birth, day).score

    val review = remember(monthRecords, monthMoods, monthDraws, monthReadings, cache, birth) {
        JournalStats.monthly(
            decisions = monthRecords.map { it.sample() },
            moods = monthMoods.map { MoodSample(it.dayKey, it.mood) },
            extraDays = monthDraws.map { it.dayKey }.toSet() + monthReadings.map { dayOf(it.date).rawValue },
            fortuneScore = { score(it) },
        )
    }
    val names = Store.profiles.items.associate { it.id to it.name }
    val primaryId = Store.primary?.id
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    fun setMonth(value: DayKey) {
        Haptics.selection()
        month = value
    }

    Screen(title = "日记", isRoot = true, scrollable = false) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = rootBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (today.month == 12 || today.month == 1) {
                item(key = "banner") {
                    AnnualReviewBanner(reviewYear(today), isDecember = today.month == 12) {
                        navigator.open(annualReviewRoute(reviewYear(today)))
                    }
                }
            }
            item(key = "month") {
                MonthSwitcher(
                    month = month,
                    isCurrentMonth = isCurrentMonth,
                    onToday = { setMonth(firstDayOf(today)) },
                    onShift = { setMonth(shiftMonth(month, it)) },
                )
            }
            item(key = "review") { MonthlyReviewCard(review, month, isCurrentMonth) }
            item(key = "shortcuts") { JournalShortcuts(today) }
            item(key = "timeline") {
                SectionHeader(
                    "时间线",
                    subtitle = if (days.isEmpty()) null else "${days.size} 天有记录",
                    icon = Icons.Rounded.Timeline,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            if (days.isEmpty()) {
                item(key = "empty") {
                    GlassCard {
                        EmptyHint(
                            Icons.Rounded.MenuBook,
                            if (isCurrentMonth) "这个月还没有记录" else "这个月没有记录",
                            "点一下心情、用工具做个决定、求一支签，都会按天记在这里",
                        )
                    }
                }
            } else {
                items(days, key = { "day-${it.day.rawValue}" }) { entry ->
                    TimelineDayCard(
                        entry = entry,
                        today = today,
                        score = score(entry.day),
                        ownerName = { id -> if (id == null || id == primaryId) null else names[id] },
                        onEdit = { editing = it.id },
                    )
                }
            }
        }
    }

    editing?.let { id -> DecisionEditDialog(id) { editing = null } }
}

private class ScoreCache(val month: Int, val birth: BirthInfo?, val scores: Map<Int, Int>)

@Composable
private fun MonthSwitcher(month: DayKey, isCurrentMonth: Boolean, onToday: () -> Unit, onShift: (Int) -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .clearAndSetSemantics {
                    contentDescription = "${month.year}年${month.month}月"
                    heading()
                },
        ) {
            SecondaryText("${month.year}年", style = Fonts.caption)
            Text("${month.month}月", style = Fonts.title2, color = palette.text)
        }
        if (!isCurrentMonth) GlassButton("回到本月", onClick = onToday)
        CircleIconButton(Icons.Rounded.ChevronLeft, "上个月") { onShift(-1) }
        CircleIconButton(Icons.Rounded.ChevronRight, "下个月", enabled = !isCurrentMonth) { onShift(1) }
    }
}

/** 12 月和 1 月日记顶部的年度回顾横幅 */
@Composable
private fun AnnualReviewBanner(year: Int, isDecember: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val (amber, red) = palette.identity(listOf(hex(0xF59E0B), hex(0xEF4444)))
    GlassCard(onClick = onClick, tint = amber.copy(alpha = 0.12f)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            GlowIcon(Icons.Rounded.AutoAwesome, amber, red, 46.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("$year 年度回顾", style = Fonts.headline, color = palette.text)
                SecondaryText(
                    if (isDecember) "这一年快过完了，看看你的年度关键词和最幸运的一天" else "新年好！回头看看去年做过的决定和心情",
                    style = Fonts.caption,
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
    }
}

// region 本月回顾

/** 日记顶部的本月回顾：做了几个决定、照做几个、满意率，最常用的工具、最纠结的事、平均心情、运势最高的一天 */
@Composable
private fun MonthlyReviewCard(review: MonthlyReview, month: DayKey, isCurrentMonth: Boolean) {
    val palette = LocalPalette.current
    GlassCard(spacing = 14.dp) {
        SectionHeader(
            if (isCurrentMonth) "本月回顾" else "${month.month}月回顾",
            subtitle = if (review.isEmpty) null else "记录了 ${review.recordDays} 天",
            icon = Icons.Rounded.PieChart,
        )
        if (review.isEmpty) {
            SecondaryText(if (isCurrentMonth) "这个月还没有记录。用工具做个决定、点一下心情，这里会慢慢长出一份小结。" else "这个月没有记录。")
        } else {
            Text(monthlySummary(review), style = Fonts.headline, color = palette.text)
            for (row in monthlyStats(review).chunked(2)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (stat in row) ReviewStat(stat, Modifier.weight(1f).fillMaxHeight())
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** 「做了 23 个决定，照做 17 个，满意率 82%」 */
private fun monthlySummary(review: MonthlyReview): String {
    val tally = review.tally
    if (tally.decisions == 0) return "这个月还没有用星匣做决定"
    var text = "做了 ${tally.decisions} 个决定"
    if (tally.adopted > 0) text += "，照做 ${tally.adopted} 个"
    tally.satisfactionRate?.let { text += "，满意率 $it%" }
    return text
}

private class StatItem(val title: String, val icon: ImageVector, val value: String)

private fun monthlyStats(review: MonthlyReview): List<StatItem> = buildList {
    review.topTool?.let { add(StatItem("最常用", Icons.Rounded.Star, "${toolInfo(it.name).first} · ${it.count} 次")) }
    review.topKind?.let { add(StatItem("最纠结", Icons.Rounded.QuestionAnswer, "${questionKindTitle(it)} · ${review.topKindCount} 次")) }
    review.moodAverage?.let { average ->
        val emoji = MoodLevel.of(average.roundHalfAway().toInt())?.emoji ?: ""
        add(StatItem("平均心情", Icons.Rounded.SentimentSatisfied, "$emoji ${String.format(Locale.ROOT, "%.1f", average)} · 打卡 ${review.moodDays} 天"))
    }
    review.bestDay?.let { add(StatItem("运势最高", Icons.Rounded.AutoAwesome, "${DateText.monthDay(it.day)} · ${it.score} 分")) }
    val tally = review.tally
    if (tally.rated > 0) {
        add(StatItem("回访", Icons.Rounded.Replay, "满意 ${tally.satisfied} · 一般 ${tally.neutral} · 后悔 ${tally.regretted}"))
    }
}

/** 回顾卡里的一小格：标题 + 数值 */
@Composable
private fun ReviewStat(stat: StatItem, modifier: Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.subtle(0.05f))
            .padding(12.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(stat.icon, null, Modifier.size(13.dp), tint = palette.secondaryText)
            SecondaryText(stat.title, style = Fonts.caption)
        }
        Text(stat.value, style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.text)
    }
}

// endregion

// region 快捷入口

private class Shortcut(val name: String, val icon: ImageVector, val colors: List<Color>, val route: String)

/** 心情、倒数日、运势日历、决策历史、塔罗手记、年度回顾 */
@Composable
private fun JournalShortcuts(today: DayKey) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val shortcuts = listOf(
        Shortcut("心情", Icons.Rounded.SentimentSatisfied, listOf(hex(0xFBBF24), hex(0xF97316)), Routes.MOOD),
        Shortcut("倒数日", Icons.Rounded.HourglassEmpty, listOf(hex(0x60A5FA), hex(0x6366F1)), Routes.COUNTDOWNS),
        Shortcut("运势日历", Icons.Rounded.CalendarMonth, listOf(hex(0x8B5CF6), hex(0xEC4899)), Routes.CALENDAR),
        Shortcut("决策历史", Icons.Rounded.History, listOf(hex(0x94A3B8), hex(0x6366F1)), Routes.HISTORY),
        Shortcut("塔罗手记", Icons.Rounded.MenuBook, listOf(hex(0x8B5CF6), hex(0x3B82F6)), Routes.TAROT_JOURNAL),
        Shortcut("年度回顾", Icons.Rounded.BarChart, listOf(hex(0xF59E0B), hex(0xEF4444)), annualReviewRoute(reviewYear(today))),
    )
    // 超大字号时一行放两个，名字不被截断（iOS 用随字号放大的自适应网格）
    val columns = if (LocalDensity.current.fontScale >= 1.5f) 2 else 3
    GlassCard {
        SectionHeader("快捷入口", icon = Icons.Rounded.GridView)
        for (row in shortcuts.chunked(columns)) {
            Row(Modifier.fillMaxWidth()) {
                for (shortcut in row) {
                    Column(
                        Modifier
                            .weight(1f)
                            .pressable { navigator.open(shortcut.route) }
                            .clearAndSetSemantics {
                                contentDescription = shortcut.name
                                role = Role.Button
                            }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ToolIcon(shortcut.icon, palette.identity(shortcut.colors), 44.dp)
                        Text(shortcut.name, style = Fonts.caption, color = palette.text, maxLines = 1)
                    }
                }
            }
        }
    }
}

// endregion

// region 时间线

/** 时间线里一天中的一行（心情单独放在最前，不在这里） */
private sealed interface JournalItem {
    val date: Long
    val key: String
}

private class DecisionItem(val record: DecisionRecord) : JournalItem {
    override val date: Long get() = record.date
    override val key: String get() = "d-${record.id}"
}

private class StickItem(val draw: StickDraw) : JournalItem {
    override val date: Long get() = draw.date
    override val key: String get() = "s-${draw.dayKey}-${draw.profileId}-${draw.date}"
}

private class TarotItem(val reading: TarotReading) : JournalItem {
    override val date: Long get() = reading.date
    override val key: String get() = "t-${reading.id}"
}

/** 时间线上的一天：心情 + 按时间排的决定、签、塔罗 */
private class JournalDay(val day: DayKey, val mood: MoodEntry?, val items: List<JournalItem>) {
    companion object {
        /** 求签、塔罗在时间线里用 StickDraw、TarotReading 单独成行，同时记下的决策记录不再重复显示 */
        private val ownRowTools = setOf("stick", "tarot")

        /** 按天分组，只留有内容的日子，新的在前；每天里的行按时间先后 */
        fun group(records: List<DecisionRecord>, moods: List<MoodEntry>, draws: List<StickDraw>, readings: List<TarotReading>): List<JournalDay> {
            val items = HashMap<Int, MutableList<JournalItem>>()
            val dayMoods = HashMap<Int, MoodEntry>()
            for (record in records) {
                if (record.tool in ownRowTools) continue
                items.getOrPut(dayOf(record.date).rawValue) { mutableListOf() }.add(DecisionItem(record))
            }
            for (draw in draws) {
                if (DayKey.fromRaw(draw.dayKey) == null) continue
                items.getOrPut(draw.dayKey) { mutableListOf() }.add(StickItem(draw))
            }
            for (reading in readings) {
                items.getOrPut(dayOf(reading.date).rawValue) { mutableListOf() }.add(TarotItem(reading))
            }
            for (mood in moods) {
                if (DayKey.fromRaw(mood.dayKey) == null) continue
                dayMoods[mood.dayKey] = mood
            }
            return (items.keys + dayMoods.keys)
                .sortedDescending()
                .mapNotNull { raw ->
                    val day = DayKey.fromRaw(raw) ?: return@mapNotNull null
                    JournalDay(day, dayMoods[raw], items[raw].orEmpty().sortedBy { it.date })
                }
        }
    }
}

/** 时间线上的一天：日期头（公历、农历、心情、运势分数），下面是心情、决定、签、塔罗 */
@Composable
private fun TimelineDayCard(
    entry: JournalDay,
    today: DayKey,
    score: Int,
    ownerName: (String?) -> String?,
    onEdit: (DecisionRecord) -> Unit,
) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val mood = MoodLevel.of(entry.mood?.mood)
    val lunar = entry.day.lunar()
    val monthDay = DateText.monthDay(entry.day)
    val title = when (entry.day.daysTo(today)) {
        0 -> "今天"
        1 -> "昨天"
        else -> monthDay
    }
    val spoken = buildList {
        add(if (title == monthDay) title else "$title，$monthDay")
        add(DateText.weekday(entry.day))
        add("农历${lunar.text}")
        if (mood != null) add("心情${mood.label}")
        add("运势 $score 分")
    }.joinToString("，")
    GlassCard(spacing = 14.dp) {
        Row(
            Modifier.clearAndSetSemantics {
                contentDescription = spoken
                heading()
            },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = Fonts.headline, color = palette.text)
                SecondaryText("${DateText.weekday(entry.day)} · 农历${lunar.text}", style = Fonts.caption)
            }
            if (mood != null) Text(mood.emoji, style = Fonts.title3)
            TagChip("运势 $score", icon = Icons.Rounded.AutoAwesome)
        }
        val moodEntry = entry.mood
        if (moodEntry != null && mood != null) {
            MoodLine(moodEntry, mood) { navigator.open(Routes.MOOD) }
        }
        for (item in entry.items) {
            androidx.compose.runtime.key(item.key) {
                when (item) {
                    is DecisionItem -> DecisionLine(item.record, onEdit)
                    is StickItem -> StickLine(item.draw, ownerName(item.draw.profileId))
                    is TarotItem -> TarotLine(item.reading) { navigator.open(Routes.TAROT_JOURNAL) }
                }
            }
        }
    }
}

@Composable
private fun MoodLine(entry: MoodEntry, mood: MoodLevel, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "打开心情打卡", onClick = onClick)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(mood.color.copy(alpha = 0.25f))
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) { Text(mood.emoji, style = Fonts.subheadline) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("心情${mood.label}", style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.text)
            if (entry.note.isNotEmpty()) Text(entry.note, style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f))
            if (entry.tags.isNotEmpty()) {
                Text(entry.tags.joinToString(" ") { "#$it" }, style = Fonts.caption, color = palette.ink(palette.accent))
            }
        }
    }
}

@Composable
private fun DecisionLine(record: DecisionRecord, onEdit: (DecisionRecord) -> Unit) {
    val palette = LocalPalette.current
    // 决定可以点开补记；猜拳、刮刮乐、掷筊只是记一笔
    val editable = JournalStats.isDecision(record.tool)
    val (name, icon, colors) = toolInfo(record.tool)
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (editable) Modifier.clickable(onClickLabel = "补记是否照做、后来怎么样") { onEdit(record) } else Modifier)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolIcon(icon, palette.identity(colors), 30.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SecondaryText("${DateText.time(Instant.ofEpochMilli(record.date))} · $name", style = Fonts.caption)
            Text(record.result, style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.text, maxLines = 3)
            record.askedQuestion?.let { SecondaryText("「$it」", style = Fonts.caption, maxLines = 2) }
            if (editable) DecisionMarks(record, showsPending = true)
        }
        if (editable) {
            Icon(Icons.Rounded.ChevronRight, null, Modifier.padding(top = 6.dp).size(18.dp), tint = palette.tertiaryText)
        }
    }
}

@Composable
private fun StickLine(draw: StickDraw, forName: String?) {
    val palette = LocalPalette.current
    val stick = ContentLibrary.stick(draw.stickNumber)
    Row(
        Modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = (forName?.let { "为${it}求签，" } ?: "今日一签，") +
                    "${stick.numberText}，${stick.levelDisplayName}，${stick.title}"
            },
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolIcon(Icons.Rounded.LocalFireDepartment, palette.identity(listOf(hex(0xFF8A3D), hex(0xE11D48))), 30.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SecondaryText(
                "${DateText.time(Instant.ofEpochMilli(draw.date))} · 今日一签" + (forName?.let { " · 为$it" } ?: ""),
                style = Fonts.caption,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stick.numberText, style = Fonts.serif(16, bold = true), color = palette.text)
                Text(
                    stick.levelDisplayName,
                    style = Fonts.caption.weight(FontWeight.Bold),
                    color = stickLevelInk(stick.level, palette.isDark),
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(stickLevelColor(stick.level).copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            SecondaryText("「${stick.title}」")
        }
    }
}

@Composable
private fun TarotLine(reading: TarotReading, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val spread = TarotSpread.of(reading.spread)
    val cards = remember(reading.cards) { decodeTarotCards(reading.cards) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "打开塔罗手记", onClick = onClick)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolIcon(Icons.Rounded.Style, palette.identity(listOf(hex(0x8B5CF6), hex(0x3B82F6))), 30.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SecondaryText("${DateText.time(Instant.ofEpochMilli(reading.date))} · 塔罗 · ${spread.label}", style = Fonts.caption)
            Text(
                cards.joinToString(" · ") { "${it.card.name}（${if (it.reversed) "逆位" else "正位"}）" },
                style = Fonts.subheadline.weight(FontWeight.SemiBold),
                color = palette.text,
            )
            if (reading.question.isNotEmpty()) SecondaryText("「${reading.question}」", style = Fonts.caption, maxLines = 2)
        }
        Icon(Icons.Rounded.ChevronRight, null, Modifier.padding(top = 6.dp).size(18.dp), tint = palette.tertiaryText)
    }
}

// endregion
