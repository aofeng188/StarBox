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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.JournalStats
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
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.screens.journal.DecisionEditDialog
import com.tianaofeng.xingxia.ui.screens.journal.DecisionMarks
import com.tianaofeng.xingxia.ui.screens.tools.toolInfo
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import java.time.Instant

/** 决策历史的一行：结果、工具和标题，「问一问」带来的问题，以及照做 / 满意度标记（与 iOS 的 HistoryRow 一致） */
@Composable
private fun RecordRow(record: DecisionRecord) {
    val (name, icon, colors) = toolInfo(record.tool)
    // 「问一问」带过来的问题；和标题一样时（硬币、答案之书把问题当标题）不重复显示
    val question = record.question?.trim()?.takeIf { it.isNotEmpty() && it != record.title.trim() }
    Row(
        Modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolIcon(icon, colors, 34.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(record.result, style = Fonts.subheadline.copy(fontWeight = FontWeight.Medium), maxLines = 2)
            SecondaryText(if (record.title.isBlank()) name else "$name · ${record.title}", style = Fonts.caption, maxLines = 1)
            if (question != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.QuestionAnswer, null, Modifier.size(12.dp), tint = LocalPalette.current.secondaryText)
                    SecondaryText(question, style = Fonts.caption, maxLines = 1)
                }
            }
            DecisionMarks(record)
        }
        SecondaryText(DateText.relative(Instant.ofEpochMilli(record.date)), style = Fonts.caption2)
    }
}

@Composable
fun HistoryScreen() {
    val palette = LocalPalette.current
    val today = rememberToday()
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    /** 点开补记照做 / 满意度的那条 */
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    val records = Store.records.items
    // 新的在前
    val tools = remember(records) { records.asReversed().map { it.tool }.distinct() }
    // 筛选的工具已经没有记录（比如刚把它删光）时回到「全部」，免得一个筛选都不亮
    val activeFilter = filter?.takeIf { it in tools }
    val query = search.trim()
    val shown = remember(records, activeFilter, query) {
        records.asReversed().filter { record ->
            (activeFilter == null || record.tool == activeFilter) &&
                (
                    query.isEmpty() || record.result.contains(query, ignoreCase = true) ||
                        record.title.contains(query, ignoreCase = true) ||
                        record.question?.contains(query, ignoreCase = true) == true
                    )
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
            item(key = "stats") { HistoryStats(records, activeFilter, Modifier.padding(bottom = 12.dp)) }
            item(key = "search") {
                Box(Modifier.padding(bottom = 10.dp)) { GlassTextField(search, "搜索结果") { search = it } }
            }
            item(key = "filters") {
                ChipRow {
                    SelectChip("全部", activeFilter == null) { filter = null }
                    for (tool in tools) SelectChip(toolInfo(tool).first, activeFilter == tool) { filter = tool }
                }
            }
            item(key = "count") {
                SecondaryText(
                    if (query.isEmpty()) "共 ${shown.size} 条 · 点决定可补记后来怎么样，长按可拷贝或删除" else "找到 ${shown.size} 条",
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
                    HistoryItem(record, isFirst = index == 0, isLast = index == items.lastIndex) { editing = record.id }
                }
            }
        }
    }

    editing?.let { id -> DecisionEditDialog(id) { editing = null } }
}

/**
 * 历史列表里的一条：同一天的几条拼成一张卡片。决定点按补记「照做了吗、后来怎么样」，
 * 占卜和游戏点按弹出菜单；长按都弹出「拷贝结果 / 删除」
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItem(record: DecisionRecord, isFirst: Boolean, isLast: Boolean, onEdit: () -> Unit) {
    val editable = JournalStats.isDecision(record.tool)
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
            .combinedClickable(
                onClickLabel = if (editable) "补记是否照做、后来怎么样" else "更多操作",
                onLongClickLabel = "更多操作",
                onLongClick = { menu = true },
            ) {
                if (editable) {
                    onEdit()
                } else {
                    menu = true
                }
            },
    ) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            if (!isFirst) HorizontalDivider(color = palette.subtle(0.08f))
            Column(Modifier.padding(vertical = 12.dp)) {
                RecordRow(record)
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
