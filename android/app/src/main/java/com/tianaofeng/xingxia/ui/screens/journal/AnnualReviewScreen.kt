package com.tianaofeng.xingxia.ui.screens.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.AnnualInput
import com.tianaofeng.xingxia.core.AnnualReview
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.DayScore
import com.tianaofeng.xingxia.core.DecisionTally
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.JournalStats
import com.tianaofeng.xingxia.core.MonthMood
import com.tianaofeng.xingxia.core.MoodSample
import com.tianaofeng.xingxia.core.StickSample
import com.tianaofeng.xingxia.core.lunar
import com.tianaofeng.xingxia.core.roundHalfAway
import com.tianaofeng.xingxia.data.DecisionRecord
import com.tianaofeng.xingxia.data.MoodEntry
import com.tianaofeng.xingxia.data.StickDraw
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.GradientText
import com.tianaofeng.xingxia.ui.components.MeterBar
import com.tianaofeng.xingxia.ui.components.ScoreRing
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.common.stickLevelColor
import com.tianaofeng.xingxia.ui.screens.common.stickLevelInk
import com.tianaofeng.xingxia.ui.screens.divination.ShareCardDialog
import com.tianaofeng.xingxia.ui.screens.divination.ShareCardRatio
import com.tianaofeng.xingxia.ui.screens.divination.ShareCardFrame
import com.tianaofeng.xingxia.ui.screens.divination.ShareCardStyle
import com.tianaofeng.xingxia.ui.screens.divination.ShareChip
import com.tianaofeng.xingxia.ui.screens.records.MoodLevel
import com.tianaofeng.xingxia.ui.screens.tools.toolInfo
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.sqrt

/**
 * 年度回顾：年度关键词、做了多少决定和照做比例、满意率、最常用的工具、最常吃的菜、心情曲线、最幸运的一天、
 * 求签等级分布，底部可以分享成图。统计规则见 core 的 JournalStats（与 iOS 的 AnnualReviewView 一致）
 */
@Composable
fun AnnualReviewScreen(year: Int) {
    val palette = LocalPalette.current
    val records = Store.records.items
    val moods = Store.moods.items
    val draws = Store.stickDraws.items
    val readings = Store.tarotReadings.items
    val foods = Store.foodLogs.items

    // 这一年的数据（求签按所有档案算，与 iOS 一致）
    val yearRecords = remember(records, year) { records.filter { dayOf(it.date).year == year }.sortedBy { it.date } }
    val yearMoods = remember(moods, year) { moods.filter { it.dayKey / 10000 == year }.sortedBy { it.dayKey } }
    val yearDraws = remember(draws, year) { draws.filter { it.dayKey / 10000 == year }.sortedBy { it.date } }
    val input = remember(yearRecords, yearMoods, yearDraws, readings, foods, year) {
        AnnualInput(
            year = year,
            decisions = yearRecords.map { it.sample() },
            moods = yearMoods.map { MoodSample(it.dayKey, it.mood) },
            sticks = yearDraws.map { StickSample(it.dayKey, ContentLibrary.stick(it.stickNumber).level) },
            tarotDays = readings.map { dayOf(it.date) }.filter { it.year == year }.map { it.rawValue },
            foods = foods.filter { dayOf(it.date).year == year }.sortedBy { it.date }.map { it.name },
        )
    }
    // 一年的运势分数要算几百天，放到后台；输入变了（新记录、补记、换档案）就重算，算好之前先显示上一次的结果
    val birth = Store.birth
    val review by produceState<AnnualReview?>(null, input, birth) {
        val owner = birth
        val data = input
        value = withContext(Dispatchers.Default) { JournalStats.annual(data) { FortuneEngine.daily(owner, it).score } }
    }
    var sharing by rememberSaveable { mutableStateOf(false) }
    val current = review

    Screen(
        title = "$year 年度回顾",
        actions = {
            if (current != null && !current.isEmpty) {
                CircleIconButton(Icons.Rounded.Share, "分享年度回顾") { sharing = true }
            }
        },
    ) {
        when {
            current == null -> Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 60.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = palette.accent) }

            current.isEmpty -> GlassCard {
                EmptyHint(
                    Icons.Rounded.BarChart,
                    "$year 年还没有记录",
                    "点心情、做决定、求签、抽塔罗，这一年的点点滴滴都会汇总在这里",
                )
            }

            else -> {
                AnnualContent(current, yearRecords, yearMoods, yearDraws)
                GlassButton(
                    "分享年度回顾",
                    Icons.Rounded.Share,
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                ) { sharing = true }
            }
        }
    }

    if (sharing && current != null && !current.isEmpty) {
        // 分享卡：3:4 和 9:16 都排得下，9:16 多放心情曲线、求签和最常用的工具
        ShareCardDialog("我的 $year 星匣年度回顾", "review", onDismiss = { sharing = false }, initialRatio = ShareCardRatio.STORY) { style ->
            ShareCardFrame(style, day = annualShareDay(year), title = "星匣 · $year 年度回顾") {
                AnnualShareContent(current, style)
            }
        }
    }
}

/** 分享卡页脚的日期：回顾今年时用今天，回顾往年用那年的最后一天 */
private fun annualShareDay(year: Int): DayKey {
    val today = DayKey.today
    return if (year >= today.year) today else DayKey(year, 12, 31)
}

@Composable
private fun AnnualContent(
    review: AnnualReview,
    records: List<DecisionRecord>,
    moods: List<MoodEntry>,
    draws: List<StickDraw>,
) {
    AnnualHero(review)
    KeywordsCard(review)
    DecisionsCard(review.tally)
    if (review.topTools.isNotEmpty()) ToolsCard(review)
    FoodsCard(review)
    MoodCard(review)
    review.luckiestDay?.let { LuckiestCard(it, records, moods, draws) }
    if (review.stickCount > 0) SticksCard(review)
}

@Composable
private fun AnnualHero(review: AnnualReview) {
    val palette = LocalPalette.current
    GlassCard(
        Modifier.semantics(mergeDescendants = true) {},
        padding = 22.dp,
        spacing = 6.dp,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        GradientText(
            "${review.year}",
            style = Fonts.display(56, FontWeight.ExtraBold).copy(
                shadow = Shadow(palette.accent.copy(alpha = 0.35f), blurRadius = 24f),
            ),
        )
        Text("年度回顾", style = Fonts.title3, color = palette.text)
        SecondaryText("这一年你在星匣里留下了 ${review.recordDays} 天的记录", textAlign = TextAlign.Center)
    }
}

@Composable
private fun KeywordsCard(review: AnnualReview) {
    val palette = LocalPalette.current
    GlassCard {
        SectionHeader("年度关键词", subtitle = "来自你问过的问题和得到的结果", icon = Icons.Rounded.LocalOffer)
        if (review.keywords.isEmpty()) {
            SecondaryText("问过的问题还不够多，多用几次「问一问」就能看到关键词")
        } else {
            Flow(spacing = 8.dp) {
                review.keywords.forEachIndexed { index, keyword ->
                    Text(
                        "#${keyword.name}",
                        style = if (index == 0) Fonts.title3.weight(FontWeight.Bold) else Fonts.headline,
                        color = palette.ink(palette.accent),
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(palette.accent.copy(alpha = if (index == 0) 0.22f else 0.13f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .clearAndSetSemantics { contentDescription = "${keyword.name}，${keyword.count} 次" },
                    )
                }
            }
        }
    }
}

@Composable
private fun DecisionsCard(tally: DecisionTally) {
    // 超大字号时三格竖排
    val stacked = LocalDensity.current.fontScale >= 1.6f
    GlassCard {
        SectionHeader("听星匣的话", icon = Icons.Rounded.Verified)
        val adoption = tally.adoptionRate
        val satisfaction = tally.satisfactionRate
        val stats = listOf(
            Triple("${tally.decisions}", "个", "做了决定"),
            Triple(adoption?.toString() ?: "—", if (adoption == null) "" else "%", "照做 ${tally.adopted} 个"),
            Triple(
                satisfaction?.toString() ?: "—",
                if (satisfaction == null) "" else "%",
                if (tally.rated > 0) "满意率 · 回访 ${tally.rated} 次" else "满意率",
            ),
        )
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((value, unit, label) in stats) BigStat(value, unit, label, Modifier.fillMaxWidth())
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for ((value, unit, label) in stats) BigStat(value, unit, label, Modifier.weight(1f).fillMaxHeight())
            }
        }
        if (tally.decisions > 0 && tally.rated == 0) {
            SecondaryText("做完决定点「就这么办」，第二天首页会来问你后来怎么样", style = Fonts.caption)
        }
    }
}

@Composable
private fun BigStat(value: String, unit: String, label: String, modifier: Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.subtle(0.05f))
            .padding(12.dp)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                value,
                style = Fonts.title.weight(FontWeight.Bold),
                color = palette.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignByBaseline(),
            )
            if (unit.isNotEmpty()) SecondaryText(unit, style = Fonts.caption, modifier = Modifier.alignByBaseline())
        }
        SecondaryText(label, style = Fonts.caption)
    }
}

@Composable
private fun ToolsCard(review: AnnualReview) {
    val palette = LocalPalette.current
    GlassCard {
        SectionHeader("最常用的工具", icon = Icons.Rounded.Star)
        review.topTools.forEachIndexed { index, tool ->
            val (name, icon, colors) = toolInfo(tool.name)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = "第 ${index + 1} 名，$name，${tool.count} 次" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("${index + 1}", style = Fonts.headline, color = palette.ink(palette.accent), modifier = Modifier.width(20.dp))
                ToolIcon(icon, colors, 34.dp)
                Text(name, style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.text, modifier = Modifier.weight(1f))
                SecondaryText("${tool.count} 次")
            }
        }
    }
}

@Composable
private fun FoodsCard(review: AnnualReview) {
    val palette = LocalPalette.current
    val medals = listOf("🥇", "🥈", "🥉")
    GlassCard {
        SectionHeader("最常吃的", icon = Icons.Rounded.Restaurant)
        if (review.topFoods.isEmpty()) {
            SecondaryText("在「今天吃什么」里点「就吃这个」，这里就会记下你最爱的菜")
        } else {
            review.topFoods.forEachIndexed { index, food ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clearAndSetSemantics { contentDescription = "${food.name}，${food.count} 次" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(medals[minOf(index, 2)], style = Fonts.title3)
                    Text(
                        food.name,
                        style = if (index == 0) Fonts.headline else Fonts.subheadline.weight(FontWeight.SemiBold),
                        color = palette.text,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryText("${food.count} 次")
                }
            }
        }
    }
}

@Composable
private fun MoodCard(review: AnnualReview) {
    GlassCard {
        SectionHeader(
            "心情曲线",
            subtitle = review.moodAverage?.let { "打卡 ${review.moodDays} 天 · 平均 ${String.format(Locale.ROOT, "%.1f", it)}" },
            icon = Icons.Rounded.ShowChart,
        )
        if (review.moodByMonth.isEmpty()) {
            SecondaryText("这一年还没有记过心情")
        } else {
            MoodChart(
                review.moodByMonth,
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clearAndSetSemantics { contentDescription = moodSummary(review.moodByMonth) },
            )
        }
    }
}

private fun moodLevelOf(average: Double): MoodLevel? = MoodLevel.of(average.roundHalfAway().toInt())

/** 「每月平均心情：1月不错，2月一般」 */
private fun moodSummary(months: List<MonthMood>): String =
    "每月平均心情：" + months.joinToString("，") { "${it.month}月${moodLevelOf(it.average)?.label ?: ""}" }

/** 心情曲线：横轴 1–12 月，纵轴 1–5（表情标注 1、3、5），平滑折线 + 每月一个点（颜色取该月平均心情） */
@Composable
private fun MoodChart(months: List<MonthMood>, modifier: Modifier) {
    val palette = LocalPalette.current
    val measurer = rememberTextMeasurer()
    val labelStyle = Fonts.caption2.copy(color = palette.secondaryText)
    val emojiStyle = Fonts.footnote
    val gridColor = palette.subtle(0.12f)
    val points = remember(months) { months.filter { it.month in 1..12 }.sortedBy { it.month } }
    Canvas(modifier) {
        val left = 30.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = size.height - 22.dp.toPx()
        val plotWidth = (right - left).coerceAtLeast(1f)
        val plotHeight = (bottom - top).coerceAtLeast(1f)
        fun x(month: Int) = left + (month - 1) / 11f * plotWidth
        fun y(value: Double) = bottom - ((value.coerceIn(1.0, 5.0) - 1) / 4).toFloat() * plotHeight
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        val hairline = 1.dp.toPx()

        // 纵轴：1、3、5 的网格线和表情
        for (level in listOf(1, 3, 5)) {
            val lineY = y(level.toDouble())
            drawLine(gridColor, Offset(left, lineY), Offset(right, lineY), hairline, pathEffect = dash)
            val emoji = MoodLevel.of(level)?.emoji ?: continue
            val layout = measurer.measure(emoji, emojiStyle)
            drawText(layout, topLeft = Offset((left - layout.size.width) / 2 - 2.dp.toPx(), lineY - layout.size.height / 2f))
        }
        // 横轴：单数月的网格线和「N月」
        for (month in listOf(1, 3, 5, 7, 9, 11)) {
            val lineX = x(month)
            drawLine(gridColor, Offset(lineX, top), Offset(lineX, bottom), hairline, pathEffect = dash)
            val layout = measurer.measure("${month}月", labelStyle)
            val labelX = (lineX - layout.size.width / 2f).coerceIn(0f, maxOf(0f, size.width - layout.size.width))
            drawText(layout, topLeft = Offset(labelX, bottom + 5.dp.toPx()))
        }

        val xs = points.map { x(it.month) }
        val ys = points.map { y(it.average) }
        if (points.size >= 2) {
            drawPath(
                monotonePath(xs, ys),
                Brush.horizontalGradient(listOf(palette.accent, palette.accentSecondary), startX = left, endX = right),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        val radius = 4.5.dp.toPx()
        points.forEachIndexed { index, item ->
            val center = Offset(xs[index], ys[index])
            drawCircle(moodLevelOf(item.average)?.color ?: palette.accent, radius, center)
        }
    }
}

/** 单调三次插值（Fritsch–Carlson）：曲线经过每个点，相邻两点之间不会冲过头 */
private fun monotonePath(xs: List<Float>, ys: List<Float>): Path {
    val n = xs.size
    val path = Path()
    if (n == 0) return path
    path.moveTo(xs[0], ys[0])
    if (n == 1) return path
    val slopes = FloatArray(n - 1) { i ->
        val dx = xs[i + 1] - xs[i]
        if (dx == 0f) 0f else (ys[i + 1] - ys[i]) / dx
    }
    val tangents = FloatArray(n)
    tangents[0] = slopes[0]
    tangents[n - 1] = slopes[n - 2]
    for (i in 1 until n - 1) {
        tangents[i] = if (slopes[i - 1] * slopes[i] <= 0f) 0f else (slopes[i - 1] + slopes[i]) / 2
    }
    for (i in 0 until n - 1) {
        if (slopes[i] == 0f) {
            tangents[i] = 0f
            tangents[i + 1] = 0f
        } else {
            val a = tangents[i] / slopes[i]
            val b = tangents[i + 1] / slopes[i]
            val s = a * a + b * b
            if (s > 9f) {
                val t = 3f / sqrt(s)
                tangents[i] = t * a * slopes[i]
                tangents[i + 1] = t * b * slopes[i]
            }
        }
    }
    for (i in 0 until n - 1) {
        val dx = (xs[i + 1] - xs[i]) / 3
        path.cubicTo(
            xs[i] + dx, ys[i] + tangents[i] * dx,
            xs[i + 1] - dx, ys[i + 1] - tangents[i + 1] * dx,
            xs[i + 1], ys[i + 1],
        )
    }
    return path
}

@Composable
private fun LuckiestCard(lucky: DayScore, records: List<DecisionRecord>, moods: List<MoodEntry>, draws: List<StickDraw>) {
    val palette = LocalPalette.current
    val raw = lucky.day.rawValue
    val notes = remember(lucky, records, moods, draws) {
        buildList {
            moods.firstOrNull { it.dayKey == raw }?.let { MoodLevel.of(it.mood) }?.let { add("心情${it.label} ${it.emoji}") }
            val decisions = records.count { dayOf(it.date) == lucky.day && JournalStats.isDecision(it.tool) }
            if (decisions > 0) add("做了 $decisions 个决定")
            draws.firstOrNull { it.dayKey == raw }?.let { add("求得${ContentLibrary.stick(it.stickNumber).levelDisplayName}") }
        }
    }
    GlassCard {
        SectionHeader("最幸运的一天", icon = Icons.Rounded.AutoAwesome)
        Row(
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ScoreRing(lucky.score, size = 76.dp, lineWidth = 8.dp, label = "运势")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(DateText.monthDay(lucky.day), style = Fonts.title3.weight(FontWeight.Bold), color = palette.text)
                SecondaryText("${DateText.weekday(lucky.day)} · 农历${lucky.day.lunar().text}", style = Fonts.caption)
                if (notes.isNotEmpty()) {
                    Text("那天你" + notes.joinToString("，"), style = Fonts.subheadline, color = palette.text)
                }
            }
        }
    }
}

@Composable
private fun SticksCard(review: AnnualReview) {
    val palette = LocalPalette.current
    val maxCount = maxOf(review.stickLevels.maxOfOrNull { it.count } ?: 1, 1)
    GlassCard {
        SectionHeader("求签等级", subtitle = "共 ${review.stickCount} 支", icon = Icons.Rounded.LocalFireDepartment)
        for (item in review.stickLevels) {
            val name = stickLevelName(item.name)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { contentDescription = "$name ${item.count} 支" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    name,
                    style = Fonts.subheadline.weight(FontWeight.SemiBold),
                    color = stickLevelInk(item.name, palette.isDark),
                    modifier = Modifier.widthIn(min = 48.dp),
                )
                MeterBar(item.count * 100 / maxCount, Modifier.weight(1f), color = stickLevelColor(item.name), height = 8.dp)
                Text(
                    "${item.count}",
                    style = Fonts.subheadline,
                    color = palette.secondaryText,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 24.dp),
                )
            }
        }
    }
}

// region 分享卡

/** 年度回顾分享卡：年份、关键词、决定三连、最常吃、最幸运的一天；9:16 再加心情曲线、求签和最常用的工具 */
@Composable
fun AnnualShareContent(review: AnnualReview, style: ShareCardStyle) {
    val tall = style.isTall
    val palette = style.palette
    val tally = review.tally
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (tall) 14.dp else 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${review.year}",
                style = TextStyle(
                    fontSize = (if (tall) 64 else 50).sp,
                    fontWeight = FontWeight.ExtraBold,
                    brush = palette.accentBrush,
                    shadow = Shadow(palette.accent.copy(alpha = 0.4f), blurRadius = 28f),
                ),
            )
            Text(
                "这一年记录了 ${review.recordDays} 天",
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f)),
            )
        }

        if (review.keywords.isNotEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ShareSectionTitle("年度关键词")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (keyword in review.keywords.take(3)) ShareChip("#${keyword.name}")
                }
                if (tall && review.keywords.size > 3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (keyword in review.keywords.drop(3).take(2)) ShareChip("#${keyword.name}")
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShareStatBox("${tally.decisions}", "个决定", Modifier.weight(1f))
            ShareStatBox(tally.adoptionRate?.let { "$it%" } ?: "—", "照做", Modifier.weight(1f))
            ShareStatBox(tally.satisfactionRate?.let { "$it%" } ?: "—", "满意", Modifier.weight(1f))
        }

        if (tall && review.moodByMonth.isNotEmpty()) ShareMoodBars(review.moodByMonth, style)

        val facts = buildList {
            review.topFoods.firstOrNull()?.let { add(Triple(Icons.Rounded.Restaurant, "最常吃", "${it.name} · ${it.count} 次")) }
            review.luckiestDay?.let { add(Triple(Icons.Rounded.AutoAwesome, "最幸运的一天", "${DateText.monthDay(it.day)} · ${it.score} 分")) }
            if (tall) {
                review.stickLevels.firstOrNull { it.count > 0 }?.let { best ->
                    add(Triple(Icons.Rounded.LocalFireDepartment, "求签", "共 ${review.stickCount} 支 · 最好${stickLevelName(best.name)} ${best.count} 次"))
                }
                review.topTools.firstOrNull()?.let { add(Triple(Icons.Rounded.Star, "最常用", "${toolInfo(it.name).first} · ${it.count} 次")) }
            }
        }
        if (facts.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.07f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for ((icon, title, value) in facts) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .semantics(mergeDescendants = true) {},
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GradientIcon(icon, size = 14.dp)
                        Text(title, style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.65f)))
                        Spacer(Modifier.weight(1f))
                        Text(
                            value,
                            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareSectionTitle(text: String) {
    Text(
        text,
        style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = Color.White.copy(alpha = 0.6f)),
    )
}

@Composable
private fun ShareStatBox(value: String, label: String, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(value, style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White), maxLines = 1)
        Text(label, style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.7f)))
    }
}

/** 12 个月的平均心情小柱子，没记心情的月份是一个小圆点 */
@Composable
private fun ShareMoodBars(months: List<MonthMood>, style: ShareCardStyle) {
    val byMonth = months.associate { it.month to it.average }
    Column(
        Modifier.clearAndSetSemantics { contentDescription = "每月平均心情" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ShareSectionTitle("心情曲线")
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (month in 1..12) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    val average = byMonth[month]
                    if (average != null) {
                        Box(
                            Modifier
                                .size(width = 10.dp, height = (8 + (average.coerceIn(1.0, 5.0) - 1) / 4 * 40).dp)
                                .clip(CircleShape)
                                .background(style.palette.accentBrush),
                        )
                    } else {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f)),
                        )
                    }
                    Text("$month", style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.55f)))
                }
            }
        }
    }
}

// endregion
