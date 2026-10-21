package com.tianaofeng.xingxia.ui.screens.today

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness1
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Tonality
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.MoonInfo
import com.tianaofeng.xingxia.core.SkyEvent
import com.tianaofeng.xingxia.core.SkyEvents
import com.tianaofeng.xingxia.core.lunar
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.system.ReminderScheduler
import com.tianaofeng.xingxia.system.SkyReminders
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.DayKeySaver
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.records.CalendarGrid
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** 天象日历：本月每天的月相，未来 60 天的新月、满月、节气、流星雨和日月食，以及天象提醒开关（与 iOS 的 SkyCalendarView 一致） */
@Composable
fun SkyCalendarScreen() {
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val today = rememberToday()
    var month by rememberSaveable(stateSaver = DayKeySaver) { mutableStateOf(DayKey(today.year, today.month, 1)) }
    var selected by rememberSaveable(stateSaver = DayKeySaver) { mutableStateOf(today) }
    // 跨过零点：停在「今天」上的话跟着换天
    var shownToday by rememberSaveable(stateSaver = DayKeySaver) { mutableStateOf(today) }
    LaunchedEffect(today) {
        if (today == shownToday) return@LaunchedEffect
        if (selected == shownToday) selected = today
        if (month.year == shownToday.year && month.month == shownToday.month) month = DayKey(today.year, today.month, 1)
        shownToday = today
    }

    val days = remember(month) { (1..YearMonth.of(month.year, month.month).lengthOfMonth()).map { DayKey(month.year, month.month, it) } }
    val monthEvents = remember(month) { SkyEvents.upcoming(days.first(), days.size).groupBy { it.day } }
    val upcoming = remember(today) { SkyEvents.upcoming(today, 60) }

    fun select(day: DayKey) {
        month = DayKey(day.year, day.month, 1)
        selected = day
    }

    fun shift(delta: Long) {
        val next = YearMonth.of(month.year, month.month).plusMonths(delta)
        month = DayKey(next.year, next.monthValue, 1)
        selected = if (next.year == today.year && next.monthValue == today.month) today else month
        Haptics.selection()
    }

    val isCurrent = month.year == today.year && month.month == today.month && selected == today
    Screen(title = "天象日历", scrollState = scroll, actions = {
        if (!isCurrent) TextAction("今天") { select(today) }
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Rounded.ChevronLeft, "上个月") { shift(-1) }
            Text(
                "${month.year}年${month.month}月",
                style = Fonts.title3.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            CircleIconButton(Icons.Rounded.ChevronRight, "下个月") { shift(1) }
        }

        SkyMonthGrid(days, monthEvents, selected, today) {
            selected = it
            Haptics.selection()
        }
        SkyDayDetail(selected, monthEvents[selected].orEmpty())
        SkyReminderCard()
        UpcomingSkyEvents(upcoming, today) { event ->
            select(event.day)
            Haptics.selection()
            scope.launch { scroll.animateScrollTo(0) }
        }
    }
}

// region 月相日历

@Composable
private fun SkyMonthGrid(days: List<DayKey>, events: Map<DayKey, List<SkyEvent>>, selected: DayKey, today: DayKey, onSelect: (DayKey) -> Unit) {
    // 格子跟随系统字号变高，设上限
    val fontScale = LocalDensity.current.fontScale
    val cellHeight = (66f * fontScale.coerceIn(1f, 1.45f)).dp
    val moons = remember(days) { days.associateWith { MoonInfo.on(it) } }
    GlassCard(padding = 12.dp) {
        val leading = (days.first().weekday + 6) % 7
        CalendarGrid(leading, days.size, cellHeight) { d ->
            val day = days[d - 1]
            val moon = moons.getValue(day)
            val dayEvents = events[day].orEmpty()
            val lunar = day.lunar()
            val label = dayEvents.firstOrNull()?.let { shortTitle(it) } ?: if (lunar.day == 1) lunar.monthName else lunar.dayName
            SkyDayCell(
                day, moon, label,
                highlighted = dayEvents.isNotEmpty(),
                isSelected = day == selected,
                isToday = day == today,
                modifier = Modifier
                    .clickable { onSelect(day) }
                    .clearAndSetSemantics {
                        contentDescription = "${day.month}月${day.day}日，${moon.name.label}，亮面 ${moon.percent}%" +
                            dayEvents.joinToString("") { "，${it.title}" }
                        role = Role.Button
                        this.selected = day == selected
                    },
            )
        }
    }
}

/** 日历格子里放得下的简称 */
private fun shortTitle(event: SkyEvent): String = if (event.kind == SkyEvent.Kind.METEOR_SHOWER) "流星雨" else event.title

@Composable
private fun SkyDayCell(day: DayKey, moon: MoonInfo, label: String, highlighted: Boolean, isSelected: Boolean, isToday: Boolean, modifier: Modifier) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .then(
                if (isSelected) Modifier.background(palette.accentBrush)
                else Modifier.background(palette.accent.copy(alpha = if (highlighted) 0.16f else 0.06f)),
            )
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, palette.accent, shape) else Modifier)
            .then(modifier)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Text(
            "${day.day}",
            style = Fonts.caption.copy(fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal),
            color = if (isSelected) palette.onAccent else palette.text,
        )
        MoonDisc(moon.phase, 20.dp)
        Text(
            label,
            style = TextStyle(fontSize = 10.sp, fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal),
            color = when {
                isSelected -> palette.onAccent.copy(alpha = 0.9f)
                highlighted -> palette.ink(palette.accent)
                else -> palette.text.copy(alpha = 0.7f)
            },
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
    }
}

// endregion

// region 选中那天

@Composable
private fun SkyDayDetail(day: DayKey, events: List<SkyEvent>) {
    val moon = remember(day) { MoonInfo.on(day) }
    val lunar = remember(day) { day.lunar() }
    val nextFull = remember(day) { SkyEvents.nextFullMoon(day) }
    val nextNew = remember(day) { SkyEvents.nextNewMoon(day) }
    GlassCard(spacing = 14.dp) {
        Row(
            Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MoonDisc(moon.phase, 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${DateText.monthDay(day)} ${DateText.weekday(day)} · 农历${lunar.text}", style = Fonts.subheadline.copy(fontWeight = FontWeight.SemiBold))
                Text(moon.name.label, style = Fonts.title2)
                SecondaryText("亮面 ${moon.percent}% · 月龄 ${String.format(Locale.ROOT, "%.1f", moon.age)} 天", style = Fonts.caption)
            }
        }
        Flow(spacing = 8.dp) {
            TagChip("下次满月 ${DateText.monthDay(nextFull)} · ${relative(nextFull, day)}", icon = Icons.Rounded.Brightness1)
            TagChip("下次新月 ${DateText.monthDay(nextNew)} · ${relative(nextNew, day)}", icon = Icons.Rounded.RadioButtonUnchecked)
        }
        for (event in events) {
            Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(skyEventIcon(event.kind), null, Modifier.size(16.dp), tint = LocalPalette.current.ink(skyEventColors(event.kind).first()))
                    Text(event.title, style = Fonts.subheadline.copy(fontWeight = FontWeight.SemiBold))
                }
                SecondaryText(event.detail, style = Fonts.footnote)
            }
        }
        if (events.isEmpty()) SecondaryText("月相按当晚 21:00（北京时间）计算。", style = Fonts.caption2)
    }
}

/** 「当天」「第二天」「3 天后」「2 天前」 */
private fun relative(day: DayKey, base: DayKey): String {
    val diff = base.daysTo(day)
    return when {
        diff == 0 -> "当天"
        diff == 1 -> "第二天"
        diff >= 2 -> "$diff 天后"
        else -> "${-diff} 天前"
    }
}

// endregion

// region 提醒

@Composable
private fun SkyReminderCard() {
    val context = LocalContext.current
    var denied by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            AppSettings.skyReminders = true
            denied = false
        } else {
            denied = true
        }
        SkyReminders.reschedule(context)
    }
    GlassCard(spacing = 8.dp) {
        ToggleRow("天象提醒", AppSettings.skyReminders, "满月、新月和流星雨极大的前一天晚上 8 点提醒你，和每日运势提醒互不影响。") { on ->
            if (on && !ReminderScheduler.hasPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                AppSettings.skyReminders = on
                SkyReminders.reschedule(context)
            }
        }
        if (denied) SecondaryText("没有通知权限。可以在系统设置 ▸ 应用 ▸ 星匣 ▸ 通知 里打开。", style = Fonts.caption)
    }
}

// endregion

// region 未来 60 天

@Composable
private fun UpcomingSkyEvents(events: List<SkyEvent>, today: DayKey, onSelect: (SkyEvent) -> Unit) {
    val palette = LocalPalette.current
    GlassCard(spacing = 14.dp) {
        SectionHeader("未来 60 天", subtitle = "共 ${events.size} 个天象", icon = Icons.Rounded.AutoAwesome)
        events.forEachIndexed { index, event ->
            if (index > 0) HorizontalDivider(color = palette.subtle(0.08f))
            SkyEventRow(event, today, Modifier.clickable(onClickLabel = "在日历中查看这一天") { onSelect(event) })
        }
        if (events.isEmpty()) SecondaryText("最近没有特别的天象。")
    }
}

@Composable
private fun SkyEventRow(event: SkyEvent, today: DayKey, modifier: Modifier) {
    val palette = LocalPalette.current
    val diff = today.daysTo(event.day)
    val whenText = when (diff) {
        0 -> "今天"
        1 -> "明天"
        else -> "$diff 天后"
    }
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolIcon(skyEventIcon(event.kind), palette.identity(skyEventColors(event.kind)), 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, style = Fonts.headline, modifier = Modifier.weight(1f))
                SecondaryText(whenText, style = Fonts.caption.copy(fontWeight = FontWeight.SemiBold))
            }
            SecondaryText("${DateText.monthDay(event.day)} ${DateText.weekday(event.day)}", style = Fonts.caption)
            Text(event.detail, style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f))
        }
    }
}

fun skyEventIcon(kind: SkyEvent.Kind): ImageVector = when (kind) {
    SkyEvent.Kind.NEW_MOON -> Icons.Rounded.Brightness3
    SkyEvent.Kind.FULL_MOON -> Icons.Rounded.Brightness1
    SkyEvent.Kind.SOLAR_TERM -> Icons.Rounded.WbSunny
    SkyEvent.Kind.METEOR_SHOWER -> Icons.Rounded.AutoAwesome
    SkyEvent.Kind.ECLIPSE -> Icons.Rounded.Tonality
}

fun skyEventColors(kind: SkyEvent.Kind): List<Color> = when (kind) {
    SkyEvent.Kind.NEW_MOON -> listOf(hex(0x6366F1), hex(0x312E81))
    SkyEvent.Kind.FULL_MOON -> listOf(hex(0xFBBF24), hex(0xD97706))
    SkyEvent.Kind.SOLAR_TERM -> listOf(hex(0x34D399), hex(0x059669))
    SkyEvent.Kind.METEOR_SHOWER -> listOf(hex(0x22D3EE), hex(0x6366F1))
    SkyEvent.Kind.ECLIPSE -> listOf(hex(0xA855F7), hex(0x6D28D9))
}

// endregion

// region 月亮

/** 画出来的月亮：亮面比例和朝向按真实月相（在北半球看，上半月右侧亮，下半月左侧亮）。phase 为 0…1，0 新月，0.5 满月 */
@Composable
fun MoonDisc(phase: Double, diameter: Dp = 24.dp, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val lit = if (palette.isDark) hex(0xFFF1C9) else hex(0xF0B429)
    val unlit = if (palette.isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)
    val rim = if (palette.isDark) Color.White.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.2f)
    Canvas(
        modifier
            .size(diameter)
            .clearAndSetSemantics { },
    ) {
        val radius = this.size.minDimension / 2 - 0.5.dp.toPx()
        val c = this.center
        drawCircle(unlit, radius, c)
        drawPath(moonLitPath(phase, c, radius), lit)
        drawCircle(rim, radius, c, style = Stroke(0.5.dp.toPx()))
    }
}

/** 亮面：亮的一侧的半圆边缘，加上明暗界线（横向半径为 r·cos(2π·phase) 的半椭圆） */
private fun moonLitPath(phase: Double, center: Offset, radius: Float): Path {
    val side = if (phase < 0.5) 1f else -1f
    val terminator = cos(2 * PI * phase).toFloat()
    val steps = 40
    return Path().apply {
        // 亮侧边缘：从顶部沿外圆到底部
        for (i in 0..steps) {
            val t = i.toDouble() / steps * PI
            val x = center.x + side * radius * sin(t).toFloat()
            val y = center.y - radius * cos(t).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        // 明暗界线：从底部回到顶部
        for (i in 0..steps) {
            val t = PI - i.toDouble() / steps * PI
            lineTo(center.x + side * terminator * radius * sin(t).toFloat(), center.y - radius * cos(t).toFloat())
        }
        close()
    }
}

// endregion
