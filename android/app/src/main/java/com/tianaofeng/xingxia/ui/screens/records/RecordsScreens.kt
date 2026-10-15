package com.tianaofeng.xingxia.ui.screens.records

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.data.DecisionRecord
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.ChipRow
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SelectChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.copyText
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.screens.tools.toolInfo
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import java.time.Instant

@Composable
fun RecordsHubScreen() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    // 过零点、回到前台时刷新，心情卡和一周走势跟着换日
    val today = rememberToday()
    Screen(title = "记录", isRoot = true) {
        val records = Store.records.items
        MoodCheckInCard(today) { navigator.open(Routes.MOOD) }
        WeekMoodCard(today) { navigator.open(Routes.MOOD) }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            RecordTile(
                Icons.Rounded.CalendarMonth, listOf(palette.accent, palette.accentSecondary), "运势日历",
                "${today.month}月", "每日运势一览", Modifier.weight(1f),
            ) { navigator.open(Routes.CALENDAR) }
            val upcoming = Store.countdowns.items
                .filter { it.kindValue != CountdownKind.ANNIVERSARY }
                .map { it to it.status(today) }
                .filter { !it.second.isPast }
                .minByOrNull { it.second.days }
            val caption = upcoming?.let { (c, s) -> if (s.isToday) "${c.title} 就是今天" else "${c.title} 还有${s.days}天" } ?: "添加重要日子"
            RecordTile(
                Icons.Rounded.HourglassEmpty, listOf(hex(0x22D3EE), hex(0x6366F1)), "倒数日",
                "${Store.countdowns.items.size}", caption, Modifier.weight(1f),
            ) { navigator.open(Routes.COUNTDOWNS) }
        }
        GlassCard(onClick = { navigator.open(Routes.HISTORY) }, spacing = 10.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("决策历史", subtitle = "共 ${records.size} 条", icon = Icons.Rounded.History, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
            }
            if (records.isEmpty()) {
                SecondaryText("用过的随机工具结果都会自动记在这里")
            } else {
                for (record in records.takeLast(4).reversed()) RecordRow(record.tool, record.title, record.result, record.date)
            }
        }
    }
}

@Composable
private fun RecordRow(tool: String, title: String, result: String, date: Long) {
    val (name, icon, colors) = toolInfo(tool)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ToolIcon(icon, colors, 34.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(result, style = Fonts.subheadline.copy(fontWeight = FontWeight.Medium), maxLines = 2)
            SecondaryText(if (title.isBlank()) name else "$name · $title", style = Fonts.caption, maxLines = 1)
        }
        SecondaryText(DateText.relative(Instant.ofEpochMilli(date)), style = Fonts.caption2)
    }
}

@Composable
fun HistoryScreen() {
    val palette = LocalPalette.current
    val today = rememberToday()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val records = Store.records.items
    // 新的在前
    val tools = remember(records) { records.asReversed().map { it.tool }.distinct() }
    val query = search.trim()
    val shown = remember(records, filter, query) {
        records.asReversed().filter { record ->
            (filter == null || record.tool == filter) &&
                (query.isEmpty() || record.result.contains(query, ignoreCase = true) || record.title.contains(query, ignoreCase = true))
        }
    }
    // 按天分组
    val groups = remember(shown) { shown.groupBy { DayKey.of(Instant.ofEpochMilli(it.date)) }.toList() }

    // 记录可能上千条，用 LazyColumn 只组合屏幕上的部分
    Screen(title = "决策历史", scrollable = false, actions = {
        if (records.isNotEmpty()) {
            if (confirmClear) {
                TextAction("确定清空") {
                    Store.records.update { emptyList() }
                    confirmClear = false
                }
            } else {
                CircleIconButton(Icons.Rounded.DeleteSweep, "清空") { confirmClear = true }
            }
        }
    }) {
        if (records.isEmpty()) {
            EmptyHint(Icons.Rounded.History, "还没有记录", "求签、塔罗、转盘、抛硬币等结果都会记在这里")
            return@Screen
        }
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "stats") { HistoryStats(records, filter, Modifier.padding(bottom = 12.dp)) }
            item(key = "search") {
                Box(Modifier.padding(bottom = 10.dp)) { GlassTextField(search, "搜索结果") { search = it } }
            }
            item(key = "filters") {
                ChipRow {
                    SelectChip("全部", filter == null) { filter = null }
                    for (tool in tools) SelectChip(toolInfo(tool).first, filter == tool) { filter = tool }
                }
            }
            item(key = "count") {
                SecondaryText(
                    if (query.isEmpty()) "共 ${shown.size} 条 · 长按可拷贝或删除" else "找到 ${shown.size} 条",
                    style = Fonts.caption,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp),
                )
            }
            if (shown.isEmpty()) {
                item(key = "empty") { EmptyHint(Icons.Rounded.Search, "没有匹配的记录", "换个关键词或筛选条件试试") }
            }
            for ((day, items) in groups) {
                item(key = "day-${day.rawValue}") {
                    Text(
                        if (day == today) "今天" else "${DateText.full(day)} ${DateText.weekday(day)}",
                        style = Fonts.footnote.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.secondaryText,
                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
                    )
                }
                itemsIndexed(items, key = { _, record -> record.id }) { index, record ->
                    HistoryItem(record, isFirst = index == 0, isLast = index == items.lastIndex)
                }
            }
        }
    }
}

/** 历史列表里的一条：同一天的几条拼成一张卡片；点按或长按弹出「拷贝结果 / 删除」 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItem(record: DecisionRecord, isFirst: Boolean, isLast: Boolean) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    val radius = 22.dp
    val shape = RoundedCornerShape(
        topStart = if (isFirst) radius else 0.dp,
        topEnd = if (isFirst) radius else 0.dp,
        bottomStart = if (isLast) radius else 0.dp,
        bottomEnd = if (isLast) radius else 0.dp,
    )
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.glassFill)
            .combinedClickable(onClickLabel = "更多操作", onLongClickLabel = "更多操作", onLongClick = { menu = true }) { menu = true },
    ) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            if (!isFirst) HorizontalDivider(color = palette.subtle(0.08f))
            Column(Modifier.padding(vertical = 12.dp)) {
                RecordRow(record.tool, record.title, record.result, record.date)
                record.detail?.let { SecondaryText("「$it」", style = Fonts.caption, modifier = Modifier.padding(start = 46.dp, top = 2.dp)) }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("拷贝结果") },
                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                onClick = {
                    copyText(context, record.result)
                    menu = false
                },
            )
            DropdownMenuItem(
                text = { Text("删除", color = palette.negative) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = palette.negative) },
                onClick = {
                    menu = false
                    Store.records.update { list -> list.filter { it.id != record.id } }
                },
            )
        }
    }
}

/** 近 30 天各工具的使用次数，和出现最多的结果 */
@Composable
private fun HistoryStats(records: List<DecisionRecord>, filter: String?, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val since = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    val recent = records.filter { it.date >= since }
    val counts = recent.groupingBy { it.tool }.eachCount().entries.sortedByDescending { it.value }.take(6)
    val top = (if (filter == null) recent else recent.filter { it.tool == filter })
        .groupingBy { it.result }.eachCount().maxByOrNull { it.value }
    GlassCard(modifier, spacing = 10.dp) {
        SectionHeader("近 30 天", subtitle = "共 ${recent.size} 次", icon = Icons.Rounded.BarChart)
        if (counts.isEmpty()) SecondaryText("最近 30 天还没有用过随机工具", style = Fonts.caption)
        val max = counts.maxOfOrNull { it.value } ?: 1
        for ((tool, count) in counts) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(toolInfo(tool).first, style = Fonts.caption, color = palette.text, maxLines = 1, modifier = Modifier.width(76.dp))
                Box(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .fillMaxWidth((count.toFloat() / max).coerceAtLeast(0.04f))
                            .height(14.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(palette.accentBrush),
                    )
                }
                SecondaryText("$count", style = Fonts.caption2)
            }
        }
        if (top != null && top.value > 1) {
            Text("出现最多的结果是「${top.key}」，共 ${top.value} 次", style = Fonts.caption, color = palette.ink(palette.accent))
        }
    }
}

@Composable
fun ComingSoonScreen(title: String) {
    Screen(title = title) {
        EmptyHint(Icons.Rounded.Construction, "「$title」即将推出", "Android 版正在逐步移植中，这个功能会在后续版本加入。")
    }
}
