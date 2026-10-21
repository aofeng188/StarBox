package com.tianaofeng.xingxia.ui.screens.records

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.People
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.core.AlmanacDay
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.CountdownSpec
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.LunarDate
import com.tianaofeng.xingxia.core.pearson
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Countdown
import com.tianaofeng.xingxia.data.CountdownItem
import com.tianaofeng.xingxia.data.MoodEntry
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.DayKeySaver
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GradientIcon
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.ReadableWidth
import com.tianaofeng.xingxia.ui.components.ScoreRing
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.fadingHorizontalScroll
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.common.stickLevelColor
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.readableForeground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
                    }
                    // 读屏读心情名称和选中状态，不读表情符号
                    .clearAndSetSemantics {
                        contentDescription = mood.label
                        role = Role.Button
                        this.selected = isSelected
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
                Text(mood.label, style = Fonts.caption2, color = if (isSelected) palette.text else palette.labelText)
            }
        }
    }
}

/** 首页 / 记录页的心情打卡卡片 */
@Composable
fun MoodCheckInCard(today: DayKey = DayKey.today, onOpen: () -> Unit) {
    val palette = LocalPalette.current
    val entry = Store.todayMood(today)
    GlassCard(spacing = 14.dp) {
        Row(Modifier.pressable(onClick = onOpen), verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(if (entry == null) "今天心情如何？" else "今日心情", icon = Icons.Rounded.SentimentSatisfied, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
        MoodPicker(entry?.mood) { Store.setMood(it.value, today) }
    }
}

@Composable
fun MoodScreen() {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val today = rememberToday()
    // 提到外面：补记完回来时停在原来的位置；点「今天」时滚回顶部的输入卡片
    val scroll = rememberScrollState()
    val entries = Store.moods.items.sortedByDescending { it.dayKey }
    // 心情月历显示的月份（yyyyMM）；null 表示本月，跨月时自动翻到新的月份
    var shownMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    val currentMonth = YearMonth.of(today.year, today.month)
    val month = shownMonth?.let { YearMonth.of(it / 100, it % 100) }?.takeIf { it < currentMonth } ?: currentMonth
    // 正在补记或修改的过去某天
    var editingDay by rememberSaveable { mutableStateOf<Int?>(null) }
    // 刚删除的记录，几秒内可以撤销
    var deleted by remember { mutableStateOf<MoodEntry?>(null) }

    LaunchedEffect(deleted) {
        if (deleted == null) return@LaunchedEffect
        // 读屏用户找到「撤销」需要更久
        val touchExploring = (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.isTouchExplorationEnabled == true
        delay(if (touchExploring) 10_000L else 5_000L)
        deleted = null
    }

    /** 今天回到顶部的输入卡片；过去的日子打开补记页 */
    fun edit(day: DayKey) {
        if (day > today) return
        if (day == today) {
            scope.launch { scroll.animateScrollTo(0) }
        } else {
            editingDay = day.rawValue
        }
    }

    fun delete(entry: MoodEntry) {
        Store.moods.update { list -> list.filter { it.dayKey != entry.dayKey } }
        Haptics.tap()
        deleted = entry
        DayKey.fromRaw(entry.dayKey)?.let { view.announceForAccessibility("已删除${DateText.monthDay(it)}的心情，可以撤销") }
    }

    fun undoDelete() {
        val snapshot = deleted ?: return
        deleted = null
        // 删除后那天可能又记过心情，这时不再恢复，避免同一天出现两条
        if (Store.moods.items.any { it.dayKey == snapshot.dayKey }) {
            view.announceForAccessibility("这天已经重新记录了心情，没有恢复")
            return
        }
        Store.moods.update { it + snapshot }
        Haptics.success()
        view.announceForAccessibility("已恢复")
    }

    val editing = editingDay?.let { DayKey.fromRaw(it) }?.takeIf { it <= today }
    if (editing != null) {
        Screen(
            title = "${DateText.monthDay(editing)} ${DateText.weekday(editing)}",
            onBack = { editingDay = null },
            actions = { TextAction("完成") { editingDay = null } },
        ) {
            GlassCard { MoodDayEditor(editing) }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        Screen(title = "心情打卡", scrollState = scroll) {
            GlassCard(spacing = 14.dp) {
                SectionHeader("今天的心情", subtitle = DateText.monthDay(today), icon = Icons.Rounded.SentimentSatisfied)
                // 跨天时换成新的一天；旧的编辑器离开前会把内容存回原来那天
                key(today) { MoodDayEditor(today, showsSaveButton = true) }
            }

            MonthHeatmap(
                entries.associate { it.dayKey to it.mood }, today, month,
                onShift = { delta ->
                    val target = month.plusMonths(delta.toLong())
                    if (target <= currentMonth) {
                        shownMonth = if (target == currentMonth) null else target.year * 100 + target.monthValue
                        Haptics.selection()
                    }
                },
                onSelect = { edit(it) },
            )

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
                SectionHeader("心情日记", subtitle = if (entries.isEmpty()) null else "长按可修改或删除", icon = Icons.Rounded.MenuBook)
                if (entries.isEmpty()) SecondaryText("还没有记录，从今天开始吧")
                val diary = entries.take(30).mapNotNull { entry ->
                    val mood = MoodLevel.of(entry.mood)
                    val key = DayKey.fromRaw(entry.dayKey)
                    if (mood != null && key != null) Triple(entry, mood, key) else null
                }
                for ((entry, mood, key) in diary) {
                    MoodDiaryRow(entry, mood, key, onEdit = { edit(key) }, onDelete = { delete(entry) })
                }
            }
        }

        deleted?.let { snapshot ->
            val key = DayKey.fromRaw(snapshot.dayKey)
            if (key != null) {
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                        .widthIn(max = ReadableWidth)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        // 压在页面内容上，底色不透明一些才读得清
                        .background(palette.backgroundBottom.copy(alpha = 0.94f))
                        .glass(RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp), tint = palette.secondaryText)
                    Text("已删除 ${DateText.monthDay(key)} 的心情", style = Fonts.subheadline, color = palette.text, modifier = Modifier.weight(1f))
                    TextAction("撤销") { undoDelete() }
                }
            }
        }
    }
}

/** 心情日记的一行：点按或长按弹出「修改 / 删除」，读屏可直接用自定义操作 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MoodDiaryRow(entry: MoodEntry, mood: MoodLevel, key: DayKey, onEdit: () -> Unit, onDelete: () -> Unit) {
    val palette = LocalPalette.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(onClickLabel = "修改或删除", onLongClick = { menu = true }) { menu = true }
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction("修改") { onEdit(); true },
                        CustomAccessibilityAction("删除") { onDelete(); true },
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(mood.emoji, style = Fonts.title2)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("${DateText.monthDay(key)} ${DateText.weekday(key)} · ${mood.label}", style = Fonts.subheadline.copy(fontWeight = FontWeight.SemiBold))
                if (entry.note.isNotEmpty()) SecondaryText(entry.note)
                if (entry.tags.isNotEmpty()) Text(entry.tags.joinToString(" ") { "#$it" }, style = Fonts.caption, color = palette.ink(palette.accent))
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("修改") },
                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = {
                    menu = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text("删除", color = palette.negative) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = palette.negative) },
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

/** 某一天的心情、一句话和标签。文字停顿片刻后自动保存，离开时再存一次（与 iOS 的 MoodDayEditor 一致） */
@Composable
private fun MoodDayEditor(day: DayKey, showsSaveButton: Boolean = false) {
    val palette = LocalPalette.current
    val focus = LocalFocusManager.current
    val entry = Store.moods.items.lastOrNull { it.dayKey == day.rawValue }
    var note by remember(day) { mutableStateOf(entry?.note ?: "") }
    var tags by remember(day) { mutableStateOf(entry?.tags?.toSet() ?: emptySet()) }

    fun saveNote() {
        val current = Store.moods.items.lastOrNull { it.dayKey == day.rawValue } ?: return
        val sortedTags = tags.sorted()
        if (current.note == note && current.tags == sortedTags) return
        Store.moods.update { list ->
            list.map { if (it.dayKey == day.rawValue) it.copy(note = note, tags = sortedTags) else it }
        }
    }

    // 记录被删除或撤销恢复后，输入框跟着更新（新建记录时这句话已经先存进去了，内容不变）
    val exists = entry != null
    LaunchedEffect(day, exists) {
        val current = Store.moods.items.lastOrNull { it.dayKey == day.rawValue }
        note = current?.note ?: ""
        tags = current?.tags?.toSet() ?: emptySet()
    }
    // 边写边存（停顿半秒后写入），离开时再存一次，不必非点「保存」
    LaunchedEffect(day, note) {
        delay(500)
        saveNote()
    }
    val latestSave by rememberUpdatedState { saveNote() }
    DisposableEffect(day) { onDispose { latestSave() } }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        MoodPicker(entry?.mood, 52.dp) {
            Store.setMood(it.value, day)
            saveNote()
        }
        GlassTextField(note, if (day == DayKey.today) "写一句话记录今天…" else "写一句话记录这一天…", singleLine = false) { note = it }
        if (entry == null && note.isNotBlank()) SecondaryText("选一个心情后，这句话会一起保存", style = Fonts.caption)
        Flow(spacing = 6.dp) {
            for (tag in MoodLevel.TAGS) {
                val on = tag in tags
                Text(
                    "#$tag",
                    style = Fonts.caption.copy(fontWeight = FontWeight.Medium),
                    color = if (on) palette.onAccent else palette.text,
                    modifier = Modifier
                        .clip(CircleShape)
                        .then(if (on) Modifier.background(palette.accentBrush) else Modifier.background(palette.subtle(0.07f)))
                        .clickable {
                            tags = if (on) tags - tag else tags + tag
                            saveNote()
                        }
                        .clearAndSetSemantics {
                            contentDescription = "标签 $tag"
                            role = Role.Button
                            this.selected = on
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
        if (showsSaveButton && entry != null) {
            GlassButton("保存", Icons.Rounded.Check) {
                focus.clearFocus()
                saveNote()
                Haptics.success()
            }
        }
    }
}

@Composable
private fun MonthHeatmap(map: Map<Int, Int>, today: DayKey, month: YearMonth, onShift: (Int) -> Unit, onSelect: (DayKey) -> Unit) {
    val palette = LocalPalette.current
    val first = DayKey(month.year, month.monthValue, 1)
    val days = month.lengthOfMonth()
    val leading = (first.weekday + 6) % 7
    val isCurrentMonth = month.year == today.year && month.monthValue == today.month
    val monthEntries = map.filterKeys { it / 100 == month.year * 100 + month.monthValue }
    val average = if (monthEntries.isEmpty()) null else
        "打卡 %d 天 · 平均 %.1f".format(monthEntries.size, monthEntries.values.average())
    val title = if (month.year == today.year) "${month.monthValue}月心情" else "${month.year}年${month.monthValue}月心情"
    GlassCard(spacing = 10.dp) {
        SectionHeader(title, subtitle = average, icon = Icons.Rounded.GridOn) {
            CircleIconButton(Icons.Rounded.ChevronLeft, "上个月", size = 30.dp) { onShift(-1) }
            CircleIconButton(Icons.Rounded.ChevronRight, "下个月", size = 30.dp, enabled = !isCurrentMonth) { onShift(1) }
        }
        CalendarGrid(leading, days, cellHeight = 34.dp) { day ->
            val key = DayKey(month.year, month.monthValue, day)
            val mood = MoodLevel.of(map[key.rawValue])
            val isFuture = key > today
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(mood?.color?.copy(alpha = 0.75f) ?: palette.subtle(if (isFuture) 0.02f else 0.06f))
                    .then(if (key == today) Modifier.border(1.5.dp, palette.accent, RoundedCornerShape(8.dp)) else Modifier)
                    // 过去的日子点一下补记或修改，未来的不能点
                    .clickable(enabled = !isFuture) {
                        Haptics.selection()
                        onSelect(key)
                    }
                    .clearAndSetSemantics {
                        contentDescription = "${key.month}月${key.day}日，" + (mood?.let { "心情${it.label}" } ?: "未记录")
                        role = Role.Button
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (mood != null) {
                    Text(mood.emoji, style = Fonts.caption)
                } else {
                    Text("$day", style = Fonts.caption2, color = palette.labelText, modifier = Modifier.alpha(if (isFuture) 0.5f else 1f))
                }
            }
        }
        Text("点过去的日子可以补记心情", style = Fonts.caption2, color = palette.labelText)
    }
}

/**
 * 心情名称、星期、日期这类压在彩色光斑上的小字：浅色模式下次要文字色还偏淡，再深一档（与 iOS 一致）；深色模式照旧
 */
private val ThemePalette.labelText: Color get() = if (isDark) secondaryText else text.copy(alpha = 0.85f)

/** 一周从周一开始的月历网格 */
@Composable
fun CalendarGrid(leading: Int, days: Int, cellHeight: Dp, cell: @Composable (Int) -> Unit) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (name in listOf("一", "二", "三", "四", "五", "六", "日")) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(name, style = Fonts.caption2, color = palette.labelText) }
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

@Composable
fun CountdownListScreen() {
    val today = rememberToday()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var viewingProfileId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }

    if (creating || editingId != null) {
        CountdownEditor(Store.countdowns.items.firstOrNull { it.id == editingId }) {
            creating = false
            editingId = null
        }
        return
    }

    val items = CountdownItem.sorted(CountdownItem.current(), today)
    // 档案生日只能查看；档案删了或开关关了就回到列表
    val viewing = viewingProfileId?.let { id -> items.firstOrNull { it.profileId == id } }
    if (viewing != null) {
        ProfileBirthdayDetail(viewing, today) { viewingProfileId = null }
        return
    }
    if (viewingProfileId != null) LaunchedEffect(Unit) { viewingProfileId = null }

    /** 数据库里的倒数日打开编辑；档案生日只能查看 */
    fun open(item: CountdownItem) {
        if (item.isFromProfile) viewingProfileId = item.profileId else editingId = item.id
    }

    Screen(
        title = "倒数日",
        spacing = 14.dp,
        actions = {
            Box {
                CircleIconButton(Icons.Rounded.MoreHoriz, "显示选项") { showOptions = true }
                DropdownMenu(expanded = showOptions, onDismissRequest = { showOptions = false }) {
                    val on = AppSettings.profileBirthdaysInCountdowns
                    DropdownMenuItem(
                        text = { Text("显示档案里的生日") },
                        leadingIcon = { Icon(Icons.Rounded.AccountCircle, null) },
                        trailingIcon = { if (on) Icon(Icons.Rounded.Check, "已打开") },
                        onClick = {
                            AppSettings.profileBirthdaysInCountdowns = !on
                            showOptions = false
                        },
                    )
                }
            }
            CircleIconButton(Icons.Rounded.Add, "添加") { creating = true }
        },
    ) {
        items.firstOrNull()?.let { hero -> CountdownHero(hero, today) { open(hero) } }
        for (item in items.drop(1)) CountdownRow(item, today) { open(item) }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButton("添加倒数日", Icons.Rounded.Add) { creating = true }
        }
        if (items.isEmpty()) EmptyHint(Icons.Rounded.HourglassEmpty, "还没有倒数日", "生日、纪念日、假期、考试……重要的日子都可以记在这里")
    }
}

@Composable
private fun CountdownHero(item: CountdownItem, today: DayKey, onClick: (() -> Unit)? = null) {
    val palette = LocalPalette.current
    val status = item.status(today)
    // 色块装饰用原色，文字和数字用调过明暗的版本，浅色模式下粉彩色也读得清
    val color = palette.color(item.colorIndex)
    val ink = palette.inkColor(item.colorIndex)
    GlassCard(padding = 22.dp, tint = color.copy(alpha = 0.1f), horizontalAlignment = Alignment.CenterHorizontally, spacing = 10.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.emoji, style = Fonts.title)
            Text(item.title, style = Fonts.title3.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
            if (item.pinned) Icon(Icons.Rounded.PushPin, "已置顶", Modifier.size(14.dp), tint = palette.secondaryText)
            CountdownSourceChip(item, color)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SecondaryText(status.prefix, style = Fonts.title3, modifier = Modifier.padding(bottom = 14.dp))
            if (status.isToday && item.kind != CountdownKind.ANNIVERSARY) {
                Text(status.suffix, style = Fonts.display(64, FontWeight.Black), color = ink)
            } else {
                Text(
                    "${status.days}",
                    style = Fonts.display(84, FontWeight.Black).copy(brush = Brush.verticalGradient(listOf(ink, ink.copy(alpha = 0.7f)))),
                )
                SecondaryText(status.suffix, style = Fonts.title3, modifier = Modifier.padding(bottom = 14.dp))
            }
        }
        SecondaryText(status.caption)
    }
}

@Composable
private fun CountdownRow(item: CountdownItem, today: DayKey, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val status = item.status(today)
    val color = palette.color(item.colorIndex)
    val ink = palette.inkColor(item.colorIndex)
    GlassCard(padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text(item.emoji, style = Fonts.title2) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.title, style = Fonts.headline, maxLines = 1)
                    if (item.pinned) Icon(Icons.Rounded.PushPin, "已置顶", Modifier.size(12.dp), tint = palette.secondaryText)
                }
                SecondaryText(status.caption, style = Fonts.caption, maxLines = 1)
                if (item.isFromProfile) CountdownSourceChip(item, color)
            }
            Column(horizontalAlignment = Alignment.End) {
                SecondaryText(status.prefix, style = Fonts.caption2)
                if (status.isToday && item.kind != CountdownKind.ANNIVERSARY) {
                    Text(status.suffix, style = Fonts.title3.copy(fontWeight = FontWeight.Bold), color = ink)
                } else {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${status.days}", style = Fonts.display(30), color = ink)
                        SecondaryText(status.suffix, style = Fonts.caption, modifier = Modifier.padding(bottom = 6.dp))
                    }
                }
            }
        }
    }
}

/** 类型标签；档案生日标「来自档案」 */
@Composable
private fun CountdownSourceChip(item: CountdownItem, color: Color) {
    if (item.isFromProfile) TagChip("来自档案", color, Icons.Rounded.AccountCircle)
    else TagChip(item.kind.label, color, item.kind.icon)
}

/** 档案生日的详情：不能编辑删除，生日要在档案里改 */
@Composable
private fun ProfileBirthdayDetail(item: CountdownItem, today: DayKey, onDone: () -> Unit) {
    val navigator = LocalNavigator.current
    val status = item.status(today)
    val profile = Store.profiles.items.firstOrNull { it.id == item.profileId }
    val birth = item.spec.origin
    Screen(title = item.title, onBack = onDone) {
        CountdownHero(item, today)
        GlassCard {
            if (profile != null) BirthdayInfoRow("档案", "${profile.avatar} ${profile.name}")
            BirthdayInfoRow("公历生日", DateText.full(birth))
            if (item.spec.isLunar) LunarDate.of(birth)?.let { BirthdayInfoRow("农历生日", it.monthName + it.dayName) }
            BirthdayInfoRow("下一次", "${DateText.full(status.targetDay)} ${DateText.weekday(status.targetDay)}")
            BirthdayInfoRow("生肖", profile?.animal?.let { "${it.animalEmoji} 属${it.animal}" } ?: "—")
        }
        GlassCard {
            Text(
                if (item.spec.isLunar) "这一天按档案里的农历生日每年自动换算。" else "这一天由档案里的生日自动生成，每年重复。",
                style = Fonts.footnote,
            )
            SecondaryText("想修改生日请到「我的 › 档案」；不想在倒数日里看到档案生日，可以在「设置」或倒数日页右上角关掉。", style = Fonts.footnote)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButton("管理档案", Icons.Rounded.People) {
                onDone()
                navigator.open(Routes.PROFILES)
            }
        }
    }
}

@Composable
private fun BirthdayInfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = Fonts.subheadline, modifier = Modifier.weight(1f))
        SecondaryText(value, style = Fonts.subheadline)
    }
}

/** 首页的倒数日横条 */
@Composable
fun CountdownStrip(today: DayKey = DayKey.today, onOpen: () -> Unit) {
    val palette = LocalPalette.current
    val items = CountdownItem.sorted(CountdownItem.current(), today)
    val pinned = items.filter { it.pinned }
    // 有置顶时只放置顶的，外加一周内要到的档案生日；没有置顶时放最近的三个
    val shown = if (pinned.isEmpty()) items.take(3) else pinned + items.filter { it.isFromProfile && it.status(today).days <= 7 }
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
            Row(Modifier.fadingHorizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (item in shown) {
                    val status = item.status(today)
                    // 底色、描边用原色；文字用 ink 版本，浅色模式下粉彩色才看得清
                    val color = palette.color(item.colorIndex)
                    val ink = palette.inkColor(item.colorIndex)
                    Column(
                        Modifier
                            .widthIn(min = 130.dp, max = 220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(color.copy(alpha = 0.12f))
                            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpen)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.emoji)
                            Text(item.title, style = Fonts.subheadline.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (item.isFromProfile) Icon(Icons.Rounded.AccountCircle, "来自档案", Modifier.size(12.dp), tint = palette.secondaryText)
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            SecondaryText(status.prefix, style = Fonts.caption2, modifier = Modifier.padding(bottom = 4.dp))
                            if (status.isToday && item.kind != CountdownKind.ANNIVERSARY) {
                                Text(status.suffix, style = Fonts.title2, color = ink)
                            } else {
                                Text("${status.days}", style = Fonts.display(30), color = ink)
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
    // 旋转、切换深浅色等配置变化后保留正在编辑的内容
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var emoji by rememberSaveable { mutableStateOf(existing?.emoji ?: "🎉") }
    var kind by rememberSaveable { mutableStateOf(existing?.kindValue ?: CountdownKind.COUNTDOWN) }
    var date by rememberSaveable(stateSaver = DayKeySaver) { mutableStateOf(existing?.date?.let { DayKey.fromRaw(it) } ?: DayKey.today) }
    var isLunar by rememberSaveable { mutableStateOf(existing?.isLunar ?: false) }
    var lunarMonth by rememberSaveable { mutableStateOf(existing?.lunarMonth ?: 1) }
    var lunarDay by rememberSaveable { mutableStateOf(existing?.lunarDay ?: 1) }
    var repeats by rememberSaveable { mutableStateOf(existing?.repeatsYearly ?: false) }
    // 农历生日是否记录出生年份（打开才显示年龄）。只有原本就是生日时 date 才是出生日期；
    // 倒数日、纪念日改成生日时 date 是目标日或开始日，不能当出生年份（与 iOS 一致）
    var recordsBirthYear by rememberSaveable {
        mutableStateOf(existing != null && existing.kindValue == CountdownKind.BIRTHDAY && existing.knowsBirthYear)
    }
    var colorIndex by rememberSaveable { mutableStateOf(existing?.colorIndex ?: 0) }
    var pinned by rememberSaveable { mutableStateOf(existing?.pinned ?: false) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val lunar = isLunar && kind != CountdownKind.ANNIVERSARY

    /** 不重复的农历倒数日对应的公历日期：农历月日没改就沿用原来的目标日，否则从今天起找最近的一次 */
    fun lunarTargetDay(): DayKey {
        if (existing != null && existing.isLunar && existing.kindValue == CountdownKind.COUNTDOWN && !existing.repeatsYearly &&
            existing.lunarMonth == lunarMonth && existing.lunarDay == lunarDay
        ) return existing.spec.nextOccurrence()
        val today = DayKey.today
        return CountdownSpec(CountdownKind.COUNTDOWN, today, false, true, lunarMonth, lunarDay).nextOccurrence(today)
    }

    fun save() {
        if (title.isBlank()) return
        val base = existing ?: Countdown(title = title, date = date.rawValue)
        val savedDate = when {
            // 不重复的农历倒数日：存换算出的公历日期，列表、首页和小组件都以它为准
            lunar && kind == CountdownKind.COUNTDOWN && !repeats -> lunarTargetDay()
            // 没记录出生年份：存创建当天，表示「未设置」，不显示年龄
            lunar && kind == CountdownKind.BIRTHDAY && !recordsBirthYear -> base.createdDay
            else -> date
        }
        Store.saveCountdown(
            base.copy(
                title = title.trim(),
                emoji = emoji,
                kind = kind.key,
                date = savedDate.rawValue,
                isLunar = lunar,
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
            Row(Modifier.fadingHorizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (item in COUNTDOWN_EMOJIS) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (emoji == item) palette.accent.copy(alpha = 0.3f) else Color.Transparent)
                            .clickable { emoji = item }
                            .clearAndSetSemantics {
                                contentDescription = "图标 $item"
                                role = Role.Button
                                this.selected = emoji == item
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(item, style = Fonts.title2) }
                }
            }
            SegmentedPicker(CountdownKind.entries, kind, { it.label }) { kind = it }
        }
        GlassCard {
            SectionHeader("日期")
            if (kind != CountdownKind.ANNIVERSARY) ToggleRow("按农历", isLunar) { isLunar = it }
            if (lunar) {
                OptionRow("农历月", LunarDate.MONTH_NAMES[lunarMonth - 1] + "月", (1..12).map { it to LunarDate.MONTH_NAMES[it - 1] + "月" }) { lunarMonth = it }
                OptionRow("农历日", LunarDate.DAY_NAMES[lunarDay - 1], (1..30).map { it to LunarDate.DAY_NAMES[it - 1] }) { lunarDay = it }
                if (kind == CountdownKind.BIRTHDAY) {
                    ToggleRow("记录出生年份", recordsBirthYear) { recordsBirthYear = it }
                    if (recordsBirthYear) PickerRow("公历出生日期", DateText.full(date)) { showDate = true }
                }
                // 换算出的公历日期，选完农历月日马上能核对
                val preview = if (kind == CountdownKind.COUNTDOWN && !repeats) "对应公历" to lunarTargetDay() else {
                    val today = DayKey.today
                    "下一次" to CountdownSpec(kind, today, true, true, lunarMonth, lunarDay).nextOccurrence(today)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(preview.first, style = Fonts.body, modifier = Modifier.weight(1f))
                    SecondaryText("${DateText.full(preview.second)} ${DateText.weekday(preview.second)}", style = Fonts.body)
                }
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
                    CountdownKind.COUNTDOWN ->
                        if (lunar) "不重复时倒数到最近一次的这个农历日子；勾选「每年重复」可以用于农历节日。"
                        else "倒数到目标日期还有多少天；勾选「每年重复」可以用于节日。"
                    CountdownKind.ANNIVERSARY -> "从开始日期起已经过了多少天，可以每年提醒周年。"
                    CountdownKind.BIRTHDAY ->
                        if (lunar) "农历生日每年自动重复；打开「记录出生年份」才会显示年龄。"
                        else "生日每年自动重复，支持农历生日。"
                },
                style = Fonts.caption,
            )
        }
        GlassCard {
            SectionHeader("样式")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (index in 0 until 6) {
                    val fill = palette.color(index)
                    val isSelected = colorIndex == index
                    // 选中：外圈描边 + 对勾，浅色粉彩上也看得出来
                    Box(
                        Modifier
                            .size(36.dp)
                            .border(if (isSelected) 2.dp else 0.dp, if (isSelected) palette.text else Color.Transparent, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(fill)
                            .clickable { colorIndex = index }
                            .clearAndSetSemantics {
                                contentDescription = "颜色 ${index + 1}，${colorName(fill)}"
                                role = Role.Button
                                this.selected = isSelected
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = fill.readableForeground)
                    }
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

/** 读屏用的颜色名称，按色相粗分（与 iOS 一致） */
private fun colorName(color: Color): String {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    val hue = hsv[0]
    val saturation = hsv[1]
    val brightness = hsv[2]
    if (saturation < 0.15f) return if (brightness < 0.4f) "深灰" else "灰色"
    return when {
        hue < 15f || hue >= 345f -> "红色"
        hue < 40f -> "橙色"
        hue < 70f -> if (brightness < 0.7f) "金色" else "黄色"
        hue < 165f -> "绿色"
        hue < 195f -> "青色"
        hue < 245f -> "蓝色"
        hue < 290f -> "紫色"
        else -> "粉色"
    }
}

// endregion

// region 运势日历

@Composable
fun FortuneCalendarScreen() {
    val palette = LocalPalette.current
    val today = rememberToday()
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
                        }
                        // 读屏读完整日期、分数、心情和求签情况（与 iOS 一致）
                        .clearAndSetSemantics {
                            contentDescription = "${day.month}月${day.day}日，运势 $score 分" +
                                (mood?.let { "，心情${it.label}" } ?: "") +
                                (if (sticks[day.rawValue] != null) "，已求签" else "")
                            role = Role.Button
                            this.selected = isSelected
                        },
                ) {
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$d",
                            style = Fonts.subheadline.copy(fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal),
                            color = if (isSelected) palette.onAccent else palette.text,
                        )
                        Text(
                            mood?.emoji ?: "$score",
                            style = TextStyle(fontSize = if (mood == null) 9.sp else 11.sp, fontWeight = FontWeight.Medium),
                            color = if (isSelected) palette.onAccent.copy(alpha = 0.85f) else palette.labelText,
                        )
                    }
                    if (sticks[day.rawValue] != null) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(palette.identity(hex(0xEF4444))),
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
                TagChip("宜 " + fortune.funYi.take(2).joinToString("、"), palette.positive)
                TagChip("忌 " + fortune.funJi.take(2).joinToString("、"), palette.negative)
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

@Composable
fun SpacerH(height: Dp) = Spacer(Modifier.height(height))

