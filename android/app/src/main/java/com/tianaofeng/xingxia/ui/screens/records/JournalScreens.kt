package com.tianaofeng.xingxia.ui.screens.records

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.AlmanacDay
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.LunarDate
import com.tianaofeng.xingxia.core.pearson
import com.tianaofeng.xingxia.data.Countdown
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.ScoreRing
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.common.stickLevelColor
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

// region 心情

enum class MoodLevel(val value: Int, val emoji: String, val label: String, val color: Color) {
    AWFUL(1, "😣", "很糟", hex(0x8B95A7)),
    DOWN(2, "😔", "低落", hex(0x7C9CF5)),
    OKAY(3, "😐", "一般", hex(0x5ECFD6)),
    GOOD(4, "🙂", "不错", hex(0x7EE08A)),
    GREAT(5, "🤩", "超棒", hex(0xFFC94D));

    companion object {
        fun of(value: Int?) = entries.firstOrNull { it.value == value }
        val TAGS = listOf("工作", "学习", "家人", "朋友", "恋爱", "运动", "美食", "旅行", "休息", "天气", "健康", "娱乐")
    }
}

/** 五个心情按钮 */
@Composable
fun MoodPicker(selected: Int?, size: Dp = 46.dp, onSelect: (MoodLevel) -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth()) {
        for (mood in MoodLevel.entries) {
            val isSelected = selected == mood.value
            val scale by animateFloatAsState(if (isSelected) 1.12f else 1f, label = "mood")
            Column(
                Modifier
                    .weight(1f)
                    .clickable {
                        Haptics.selection()
                        onSelect(mood)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .size(size)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(CircleShape)
                        .background(mood.color.copy(alpha = if (isSelected) 0.35f else 0f))
                        .border(1.5.dp, mood.color.copy(alpha = if (isSelected) 0.9f else 0f), CircleShape)
                        .alpha(if (selected == null || isSelected) 1f else 0.5f),
                    contentAlignment = Alignment.Center,
                ) { Text(mood.emoji, style = TextStyle(fontSize = (size.value * 0.55f).sp)) }
                Text(mood.label, style = Fonts.caption2, color = if (isSelected) palette.text else palette.secondaryText)
            }
        }
    }
}

/** 首页 / 记录页的心情打卡卡片 */
@Composable
fun MoodCheckInCard(onOpen: () -> Unit) {
    val palette = LocalPalette.current
    val entry = Store.todayMood()
    GlassCard(spacing = 14.dp) {
        Row(Modifier.pressable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(if (entry == null) "今天心情如何？" else "今日心情", icon = Icons.Rounded.SentimentSatisfied, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
        MoodPicker(entry?.mood) { Store.setMood(it.value) }
    }
}

@Composable
fun MoodScreen() {
    val palette = LocalPalette.current
    val today = DayKey.today
    val entries = Store.moods.items.sortedByDescending { it.dayKey }
    val todayEntry = Store.todayMood()
    var note by remember { mutableStateOf(todayEntry?.note ?: "") }
    var tags by remember { mutableStateOf(todayEntry?.tags?.toSet() ?: emptySet()) }

    fun saveNote() {
        if (Store.todayMood() == null) return
        Store.moods.update { list ->
            list.map { if (it.dayKey == today.rawValue) it.copy(note = note, tags = tags.sorted()) else it }
        }
    }

    Screen(title = "心情打卡") {
        GlassCard(spacing = 14.dp) {
            SectionHeader("今天的心情", subtitle = DateText.monthDay(today), icon = Icons.Rounded.SentimentSatisfied)
            MoodPicker(todayEntry?.mood, 52.dp) {
                Store.setMood(it.value)
                saveNote()
            }
            GlassTextField(note, "写一句话记录今天…", singleLine = false) { note = it }
            Flow(spacing = 6.dp) {
                for (tag in MoodLevel.TAGS) {
                    val on = tag in tags
                    Text(
                        "#$tag",
                        style = Fonts.caption.copy(fontWeight = FontWeight.Medium),
                        color = if (on) Color.White else palette.text,
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(if (on) Modifier.background(palette.accentBrush) else Modifier.background(palette.subtle(0.07f)))
                            .clickable {
                                tags = if (on) tags - tag else tags + tag
                                saveNote()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
            if (todayEntry != null) {
                GlassButton("保存", Icons.Rounded.Favorite) {
                    saveNote()
                    Haptics.success()
                }
            } else {
                SecondaryText("先选一个心情，再写笔记和标签", style = Fonts.caption)
            }
        }

        MonthHeatmap(entries.associate { it.dayKey to it.mood })

        correlationInsight()?.let { insight ->
            GlassCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientIcon(Icons.Rounded.AutoAwesome, 22.dp)
                    Text(insight, style = Fonts.subheadline)
                }
            }
        }

        val recent = entries.take(60)
        GlassCard(spacing = 10.dp) {
            SectionHeader("心情分布", subtitle = "最近 ${recent.size} 次", icon = Icons.Rounded.BarChart)
            val counts = MoodLevel.entries.map { mood -> mood to recent.count { it.mood == mood.value } }
            val max = maxOf(1, counts.maxOf { it.second })
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                for ((mood, count) in counts) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SecondaryText("$count", style = Fonts.caption)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((90f * count / max).coerceAtLeast(3f).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.verticalGradient(listOf(mood.color, mood.color.copy(alpha = 0.6f)))),
                        )
                        Text(mood.emoji, style = Fonts.subheadline)
                    }
                }
            }
        }

        GlassCard(spacing = 10.dp) {
            SectionHeader("心情日记", icon = Icons.Rounded.MenuBook)
            if (entries.isEmpty()) SecondaryText("还没有记录，从今天开始吧")
            val diary = entries.take(30).mapNotNull { entry ->
                val mood = MoodLevel.of(entry.mood)
                val key = DayKey.fromRaw(entry.dayKey)
                if (mood != null && key != null) Triple(entry, mood, key) else null
            }
            for ((entry, mood, key) in diary) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(mood.emoji, style = Fonts.title2)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${DateText.monthDay(key)} ${DateText.weekday(key)} · ${mood.label}", style = Fonts.subheadline.copy(fontWeight = FontWeight.SemiBold))
                        if (entry.note.isNotEmpty()) SecondaryText(entry.note)
                        if (entry.tags.isNotEmpty()) Text(entry.tags.joinToString(" ") { "#$it" }, style = Fonts.caption, color = palette.accent)
                    }
                    CircleIconButton(Icons.Rounded.Delete, "删除", size = 30.dp) {
                        Store.moods.update { list -> list.filter { it.dayKey != entry.dayKey } }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeatmap(map: Map<Int, Int>) {
    val palette = LocalPalette.current
    val today = DayKey.today
    val first = DayKey(today.year, today.month, 1)
    val days = YearMonth.of(today.year, today.month).lengthOfMonth()
    val leading = (first.weekday + 6) % 7
    val monthEntries = map.filterKeys { it / 100 == today.year * 100 + today.month }
    val average = if (monthEntries.isEmpty()) null else
        "打卡 %d 天 · 平均 %.1f".format(monthEntries.size, monthEntries.values.average())
    GlassCard(spacing = 10.dp) {
        SectionHeader("${today.month}月心情", subtitle = average, icon = Icons.Rounded.GridOn)
        CalendarGrid(leading, days, cellHeight = 34.dp) { day ->
            val key = DayKey(today.year, today.month, day)
            val mood = MoodLevel.of(map[key.rawValue])
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(mood?.color?.copy(alpha = 0.75f) ?: palette.subtle(if (key > today) 0.02f else 0.06f))
                    .then(if (key == today) Modifier.border(1.5.dp, palette.accent, RoundedCornerShape(8.dp)) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (mood != null) Text(mood.emoji, style = Fonts.caption) else SecondaryText("$day", style = Fonts.caption2)
            }
        }
    }
}

/** 一周从周一开始的月历网格 */
@Composable
fun CalendarGrid(leading: Int, days: Int, cellHeight: Dp, cell: @Composable (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (name in listOf("一", "二", "三", "四", "五", "六", "日")) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { SecondaryText(name, style = Fonts.caption2) }
            }
        }
        val cells: List<Int?> = List(leading) { null } + (1..days).toList()
        for (week in cells.chunked(7)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until 7) {
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f).height(cellHeight)) { if (day != null) cell(day) }
                }
            }
        }
    }
}

/** 心情与当日运势分数的相关性（皮尔逊相关系数） */
private fun correlationInsight(): String? {
    val birth = Store.birth
    val pairs = Store.moods.items.sortedByDescending { it.dayKey }.take(60).mapNotNull { entry ->
        val key = DayKey.fromRaw(entry.dayKey) ?: return@mapNotNull null
        entry.mood.toDouble() to FortuneEngine.daily(birth, key).score.toDouble()
    }
    if (pairs.size < 5) return if (pairs.isEmpty()) null else "再打卡 ${5 - pairs.size} 天，就能看到心情和运势之间的关联啦。"
    val r = pearson(pairs) ?: return "最近心情很稳定，运势起伏对你影响不大。"
    return when {
        r >= 0.4 -> "你的心情和运势很同步（相关度 %.2f）：运势好的日子，你多半也心情不错。".format(r)
        r >= 0.15 -> "心情和运势有一点点关联（相关度 %.2f），好运会给你加点分。".format(r)
        r > -0.15 -> "你的心情几乎不受运势影响（相关度 %.2f），快乐由自己决定！".format(r)
        else -> "有趣：运势平平的日子你反而更开心（相关度 %.2f），你是逆风翻盘型选手。".format(r)
    }
}

// endregion

// region 倒数日

private val COUNTDOWN_EMOJIS = listOf("🎉", "🎂", "💝", "✈️", "🏖️", "🎓", "📚", "💼", "🎄", "🧧", "🎆", "🌙", "🏃", "💍", "👶", "🐾", "🏠", "🎤", "⚽️", "📝")

private val CountdownKind.icon: ImageVector
    get() = when (this) {
        CountdownKind.COUNTDOWN -> Icons.Rounded.HourglassEmpty
        CountdownKind.ANNIVERSARY -> Icons.Rounded.Favorite
        CountdownKind.BIRTHDAY -> Icons.Rounded.Cake
    }

/** 列表排序：置顶在前；其余按剩余天数，纪念日和已过的排后面 */
fun sortedCountdowns(list: List<Countdown>): List<Countdown> = list.sortedWith(
    compareBy<Countdown> { !it.pinned }.thenBy {
        val s = it.status()
        if (it.kindValue == CountdownKind.ANNIVERSARY || s.isPast) 10_000 + s.days else s.days
    },
)

@Composable
fun CountdownListScreen() {
    val palette = LocalPalette.current
    var editingId by remember { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }

    if (creating || editingId != null) {
        CountdownEditor(Store.countdowns.items.firstOrNull { it.id == editingId }) {
            creating = false
            editingId = null
        }
        return
    }

    val sorted = sortedCountdowns(Store.countdowns.items)
    Screen(title = "倒数日", spacing = 14.dp, actions = { CircleIconButton(Icons.Rounded.Add, "添加") { creating = true } }) {
        sorted.firstOrNull()?.let { hero ->
            val status = hero.status()
            val color = palette.color(hero.colorIndex)
            GlassCard(padding = 22.dp, tint = color.copy(alpha = 0.1f), horizontalAlignment = Alignment.CenterHorizontally, spacing = 10.dp, onClick = { editingId = hero.id }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(hero.emoji, style = Fonts.title)
                    Text(hero.title, style = Fonts.title3.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    if (hero.pinned) Icon(Icons.Rounded.PushPin, "置顶", Modifier.size(14.dp), tint = palette.secondaryText)
                    TagChip(hero.kindValue.label, color, hero.kindValue.icon)
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SecondaryText(status.prefix, style = Fonts.title3, modifier = Modifier.padding(bottom = 14.dp))
                    if (status.isToday && hero.kindValue != CountdownKind.ANNIVERSARY) {
                        Text(status.suffix, style = Fonts.display(64, FontWeight.Black), color = color)
                    } else {
                        Text(
                            "${status.days}",
                            style = Fonts.display(84, FontWeight.Black).copy(brush = Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f)))),
                        )
                        SecondaryText(status.suffix, style = Fonts.title3, modifier = Modifier.padding(bottom = 14.dp))
                    }
                }
                SecondaryText(status.caption)
            }
        }
        for (countdown in sorted.drop(1)) CountdownRow(countdown) { editingId = countdown.id }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButton("添加倒数日", Icons.Rounded.Add) { creating = true }
        }
        if (sorted.isEmpty()) EmptyHint(Icons.Rounded.HourglassEmpty, "还没有倒数日", "生日、纪念日、假期、考试……重要的日子都可以记在这里")
    }
}

@Composable
private fun CountdownRow(countdown: Countdown, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val status = countdown.status()
    val color = palette.color(countdown.colorIndex)
    GlassCard(padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text(countdown.emoji, style = Fonts.title2) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(countdown.title, style = Fonts.headline, maxLines = 1)
                    if (countdown.pinned) Icon(Icons.Rounded.PushPin, "置顶", Modifier.size(12.dp), tint = palette.secondaryText)
                }
                SecondaryText(status.caption, style = Fonts.caption, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                SecondaryText(status.prefix, style = Fonts.caption2)
                if (status.isToday && countdown.kindValue != CountdownKind.ANNIVERSARY) {
                    Text(status.suffix, style = Fonts.title3.copy(fontWeight = FontWeight.Bold), color = color)
                } else {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${status.days}", style = Fonts.display(30), color = color)
                        SecondaryText(status.suffix, style = Fonts.caption, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
        }
    }
}

/** 首页的倒数日横条 */
@Composable
fun CountdownStrip(onOpen: () -> Unit) {
    val palette = LocalPalette.current
    val countdowns = Store.countdowns.items.sortedBy { it.createdAt }
    val pinned = countdowns.filter { it.pinned }
    val shown = pinned.ifEmpty { countdowns.take(3) }
    GlassCard {
        Row(Modifier.pressable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("倒数日", icon = Icons.Rounded.HourglassEmpty, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassButton("添加生日、纪念日或假期倒计时", Icons.Rounded.Add, onClick = onOpen)
            }
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (countdown in shown) {
                    val status = countdown.status()
                    val color = palette.color(countdown.colorIndex)
                    Column(
                        Modifier
                            .width(140.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(color.copy(alpha = 0.12f))
                            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpen)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(countdown.emoji)
                            Text(countdown.title, style = Fonts.subheadline.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            SecondaryText(status.prefix, style = Fonts.caption2, modifier = Modifier.padding(bottom = 4.dp))
                            if (status.isToday && countdown.kindValue != CountdownKind.ANNIVERSARY) {
                                Text(status.suffix, style = Fonts.title2, color = color)
                            } else {
                                Text("${status.days}", style = Fonts.display(30), color = color)
                                SecondaryText(status.suffix, style = Fonts.caption2, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountdownEditor(existing: Countdown?, onDone: () -> Unit) {
    val palette = LocalPalette.current
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🎉") }
    var kind by remember { mutableStateOf(existing?.kindValue ?: CountdownKind.COUNTDOWN) }
    var date by remember { mutableStateOf(existing?.date?.let { DayKey.fromRaw(it) } ?: DayKey.today) }
    var isLunar by remember { mutableStateOf(existing?.isLunar ?: false) }
    var lunarMonth by remember { mutableStateOf(existing?.lunarMonth ?: 1) }
    var lunarDay by remember { mutableStateOf(existing?.lunarDay ?: 1) }
    var repeats by remember { mutableStateOf(existing?.repeatsYearly ?: false) }
    var colorIndex by remember { mutableStateOf(existing?.colorIndex ?: 0) }
    var pinned by remember { mutableStateOf(existing?.pinned ?: false) }
    var showDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun save() {
        if (title.isBlank()) return
        val base = existing ?: Countdown(title = title, date = date.rawValue)
        Store.saveCountdown(
            base.copy(
                title = title.trim(),
                emoji = emoji,
                kind = kind.key,
                date = date.rawValue,
                isLunar = isLunar && kind != CountdownKind.ANNIVERSARY,
                lunarMonth = lunarMonth,
                lunarDay = lunarDay,
                repeatsYearly = if (kind == CountdownKind.BIRTHDAY) true else repeats,
                colorIndex = colorIndex,
                pinned = pinned,
            ),
        )
        Haptics.success()
        onDone()
    }

    Screen(
        title = if (existing == null) "新建倒数日" else "编辑倒数日",
        onBack = onDone,
        actions = { TextAction("保存", enabled = title.isNotBlank()) { save() } },
    ) {
        GlassCard {
            GlassTextField(title, "名称，如「妈妈生日」「去日本旅行」") { title = it }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (item in COUNTDOWN_EMOJIS) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (emoji == item) palette.accent.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { emoji = item },
                        contentAlignment = Alignment.Center,
                    ) { Text(item, style = Fonts.title2) }
                }
            }
            SegmentedPicker(CountdownKind.entries, kind, { it.label }) { kind = it }
        }
        GlassCard {
            SectionHeader("日期")
            if (kind != CountdownKind.ANNIVERSARY) ToggleRow("按农历", isLunar) { isLunar = it }
            if (isLunar && kind != CountdownKind.ANNIVERSARY) {
                OptionRow("农历月", LunarDate.MONTH_NAMES[lunarMonth - 1] + "月", (1..12).map { it to LunarDate.MONTH_NAMES[it - 1] + "月" }) { lunarMonth = it }
                OptionRow("农历日", LunarDate.DAY_NAMES[lunarDay - 1], (1..30).map { it to LunarDate.DAY_NAMES[it - 1] }) { lunarDay = it }
                if (kind == CountdownKind.BIRTHDAY) PickerRow("出生年份参考", DateText.full(date)) { showDate = true }
            } else {
                PickerRow(
                    when (kind) {
                        CountdownKind.BIRTHDAY -> "生日"
                        CountdownKind.ANNIVERSARY -> "开始日期"
                        CountdownKind.COUNTDOWN -> "目标日期"
                    },
                    DateText.full(date),
                ) { showDate = true }
            }
            if (kind != CountdownKind.BIRTHDAY) {
                ToggleRow(if (kind == CountdownKind.ANNIVERSARY) "每年提醒周年" else "每年重复", repeats) { repeats = it }
            }
            SecondaryText(
                when (kind) {
                    CountdownKind.COUNTDOWN -> "倒数到目标日期还有多少天；勾选「每年重复」可以用于节日。"
                    CountdownKind.ANNIVERSARY -> "从开始日期起已经过了多少天，可以每年提醒周年。"
                    CountdownKind.BIRTHDAY -> "生日每年自动重复，支持农历生日。"
                },
                style = Fonts.caption,
            )
        }
        GlassCard {
            SectionHeader("样式")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (index in 0 until 6) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(palette.color(index))
                            .border(if (colorIndex == index) 3.dp else 0.dp, Color.White, CircleShape)
                            .clickable { colorIndex = index },
                    )
                }
            }
            ToggleRow("置顶到首页", pinned, "首页和小组件优先显示置顶的倒数日") { pinned = it }
        }
        BigActionButton("保存", enabled = title.isNotBlank()) { save() }
        if (existing != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (confirmDelete) {
                    GlassButton("确定删除", Icons.Rounded.Delete) {
                        Store.deleteCountdown(existing.id)
                        onDone()
                    }
                } else {
                    GlassButton("删除", Icons.Rounded.Delete) { confirmDelete = true }
                }
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = date.localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = DayKey.of(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
                    showDate = false
                }) { Text("完成") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun PickerRow(title: String, value: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = Fonts.body, modifier = Modifier.weight(1f))
        Text(value, style = Fonts.body, color = palette.accent)
        Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
    }
}

@Composable
private fun <T> OptionRow(title: String, value: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PickerRow(title, value) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.height(360.dp)) {
            for ((key, label) in options) DropdownMenuItem(text = { Text(label) }, onClick = {
                onSelect(key)
                open = false
            })
        }
    }
}

// endregion

// region 运势日历

@Composable
fun FortuneCalendarScreen() {
    val palette = LocalPalette.current
    val today = DayKey.today
    var month by remember { mutableStateOf(YearMonth.of(today.year, today.month)) }
    var selected by remember { mutableStateOf(today) }
    val birth = Store.birth
    val days = (1..month.lengthOfMonth()).map { DayKey(month.year, month.monthValue, it) }
    val scores = remember(month, birth) { days.associateWith { FortuneEngine.daily(birth, it).score } }
    val sticks = Store.stickDraws.items.filter { it.profileId == Store.primary?.id }.associate { it.dayKey to it.stickNumber }
    val moods = Store.moods.items.associate { it.dayKey to it.mood }

    fun shift(delta: Long) {
        month = month.plusMonths(delta)
        selected = DayKey(month.year, month.monthValue, 1)
        Haptics.selection()
    }

    Screen(
        title = "运势日历",
        actions = {
            if (month != YearMonth.of(today.year, today.month)) TextAction("今天") {
                month = YearMonth.of(today.year, today.month)
                selected = today
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Rounded.ChevronLeft, "上个月") { shift(-1) }
            Text("${month.year}年${month.monthValue}月", style = Fonts.title3.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            CircleIconButton(Icons.Rounded.ChevronRight, "下个月") { shift(1) }
        }

        GlassCard(padding = 12.dp) {
            val leading = (days.first().weekday + 6) % 7
            CalendarGrid(leading, days.size, cellHeight = 54.dp) { d ->
                val day = DayKey(month.year, month.monthValue, d)
                val score = scores[day] ?: 60
                val mood = MoodLevel.of(moods[day.rawValue])
                val isSelected = day == selected
                val intensity = (score - 35) / 64f
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .alpha(if (day > today.adding(7)) 0.55f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .then(
                            if (isSelected) Modifier.background(palette.accentBrush)
                            else Modifier.background(palette.accent.copy(alpha = 0.08f + intensity * 0.35f)),
                        )
                        .then(if (day == today && !isSelected) Modifier.border(1.5.dp, palette.accent, RoundedCornerShape(12.dp)) else Modifier)
                        .clickable {
                            selected = day
                            Haptics.selection()
                        },
                ) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$d",
                            style = Fonts.subheadline.copy(fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) Color.White else palette.text,
                        )
                        Text(
                            mood?.emoji ?: "$score",
                            style = TextStyle(fontSize = if (mood == null) 9.sp else 11.sp, fontWeight = FontWeight.Medium),
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else palette.secondaryText,
                        )
                    }
                    if (sticks[day.rawValue] != null) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(hex(0xEF4444)),
                        )
                    }
                }
            }
        }

        // 选中日详情
        val fortune = remember(selected, birth) { FortuneEngine.daily(birth, selected) }
        val almanac = remember(selected) { AlmanacDay(selected) }
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ScoreRing(fortune.score, size = 70.dp, lineWidth = 7.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${DateText.monthDay(selected)} ${DateText.weekday(selected)} · 农历${almanac.lunar.text}", style = Fonts.subheadline.copy(fontWeight = FontWeight.SemiBold))
                    SecondaryText("${fortune.levelName} · ${fortune.summary}", maxLines = 2)
                }
            }
            Flow(spacing = 8.dp) {
                TagChip("宜 " + fortune.funYi.take(2).joinToString("、"), hex(0x34D399))
                TagChip("忌 " + fortune.funJi.take(2).joinToString("、"), hex(0xFB7185))
            }
            sticks[selected.rawValue]?.let { n ->
                val stick = ContentLibrary.stick(n)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(14.dp), tint = stickLevelColor(stick.level))
                    Text("求得 ${stick.numberText} ${stick.levelDisplayName} ·「${stick.title}」", style = Fonts.caption)
                }
            }
            MoodLevel.of(moods[selected.rawValue])?.let { mood ->
                Text("心情：${mood.emoji} ${mood.label}", style = Fonts.caption)
            }
            if (selected > today) SecondaryText("未来的运势仅供参考，好运要靠自己创造～", style = Fonts.caption2)
        }

        // 走势
        GlassCard(spacing = 10.dp) {
            val values = days.map { scores[it] ?: 60 }
            val average = values.average()
            SectionHeader("本月运势走势", subtitle = "平均 %.0f 分".format(average), icon = Icons.Rounded.ShowChart)
            TrendChart(values, average, days.indexOf(selected).takeIf { it >= 0 })
        }
    }
}

@Composable
private fun TrendChart(values: List<Int>, average: Double, highlight: Int?) {
    val palette = LocalPalette.current
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(170.dp),
    ) {
        if (values.size < 2) return@Canvas
        val minY = 30f
        val maxY = 100f
        fun x(i: Int) = size.width * i / (values.size - 1)
        fun y(v: Double) = (size.height * (1 - (v.toFloat() - minY) / (maxY - minY)))
        val points = values.mapIndexed { i, v -> Offset(x(i), y(v.toDouble())) }
        // 平滑曲线（Catmull-Rom 转贝塞尔）
        val line = Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = points[maxOf(0, i - 1)]
                val p1 = points[i]
                val p2 = points[i + 1]
                val p3 = points[minOf(points.size - 1, i + 2)]
                cubicTo(
                    p1.x + (p2.x - p0.x) / 6, p1.y + (p2.y - p0.y) / 6,
                    p2.x - (p3.x - p1.x) / 6, p2.y - (p3.y - p1.y) / 6,
                    p2.x, p2.y,
                )
            }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(palette.accent.copy(alpha = 0.4f), Color.Transparent)))
        drawPath(line, palette.accentBrush, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        val avgY = y(average)
        drawLine(
            palette.secondaryText.copy(alpha = 0.5f), Offset(0f, avgY), Offset(size.width, avgY), 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
        )
        if (highlight != null) drawCircle(palette.accentSecondary, 5.dp.toPx(), points[highlight])
    }
}

// endregion

/** 记录页顶部：最近一周 心情 × 运势 */
@Composable
fun WeekMoodCard(onOpen: () -> Unit) {
    val palette = LocalPalette.current
    val birth = Store.birth
    GlassCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("最近一周", subtitle = "心情 × 运势", icon = Icons.Rounded.ShowChart, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
        Row(Modifier.fillMaxWidth()) {
            for (offset in 6 downTo 0) {
                val day = DayKey.today.adding(-offset)
                val mood = MoodLevel.of(Store.moods.items.lastOrNull { it.dayKey == day.rawValue }?.mood)
                val score = FortuneEngine.daily(birth, day).score
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(mood?.emoji ?: "·", style = Fonts.title3, modifier = Modifier.height(28.dp))
                    Box(Modifier.height(50.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .width(8.dp)
                                .height(((score - 30) * 0.7f).coerceAtLeast(4f).dp)
                                .clip(CircleShape)
                                .background(palette.accentBrush),
                        )
                    }
                    SecondaryText(if (offset == 0) "今天" else DateText.weekday(day), style = Fonts.caption2)
                }
            }
        }
    }
}

@Composable
fun RecordTile(icon: ImageVector, colors: List<Color>, title: String, value: String, caption: String, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier, padding = 16.dp, spacing = 10.dp, onClick = onClick) {
        com.tianaofeng.xingxia.ui.components.ToolIcon(icon, colors, 40.dp)
        Text(value, style = Fonts.display(26))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = Fonts.headline)
            SecondaryText(caption, style = Fonts.caption2, maxLines = 1)
        }
    }
}

@Composable
fun SpacerH(height: Dp) = Spacer(Modifier.height(height))

