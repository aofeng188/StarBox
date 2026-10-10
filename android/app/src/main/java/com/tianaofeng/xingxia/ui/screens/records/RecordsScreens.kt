package com.tianaofeng.xingxia.ui.screens.records

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
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
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.tools.toolInfo
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import java.time.Instant

@Composable
fun RecordsHubScreen() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    Screen(title = "记录", isRoot = true) {
        val records = Store.records.items
        MoodCheckInCard { navigator.open(Routes.MOOD) }
        WeekMoodCard { navigator.open(Routes.MOOD) }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val today = DayKey.today
            RecordTile(
                Icons.Rounded.CalendarMonth, listOf(palette.accent, palette.accentSecondary), "运势日历",
                "${today.month}月", "每日运势一览", Modifier.weight(1f),
            ) { navigator.open(Routes.CALENDAR) }
            val upcoming = Store.countdowns.items
                .filter { it.kindValue != CountdownKind.ANNIVERSARY }
                .map { it to it.status() }
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
    var filter by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val records = Store.records.items
    val tools = records.map { it.tool }.distinct()
    val shown = records.filter { filter == null || it.tool == filter }.reversed()

    Screen(title = "决策历史", actions = {
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
        ChipRow {
            SelectChip("全部", filter == null) { filter = null }
            for (tool in tools) SelectChip(toolInfo(tool).first, filter == tool) { filter = tool }
        }
        // 按天分组
        val groups = shown.groupBy { DayKey.of(Instant.ofEpochMilli(it.date)) }
        val stats = shown.size
        SecondaryText("共 $stats 条", style = Fonts.caption)
        for ((day, items) in groups) {
            Text(
                if (day == DayKey.today) "今天" else "${DateText.full(day)} ${DateText.weekday(day)}",
                style = Fonts.footnote.copy(fontWeight = FontWeight.SemiBold),
                color = palette.secondaryText,
                modifier = Modifier.padding(start = 4.dp),
            )
            GlassCard(spacing = 0.dp, padding = 12.dp) {
                items.forEachIndexed { i, record ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        RecordRow(record.tool, record.title, record.result, record.date)
                        record.detail?.let { SecondaryText("「$it」", style = Fonts.caption, modifier = Modifier.padding(start = 46.dp, top = 2.dp)) }
                    }
                    if (i < items.size - 1) HorizontalDivider(color = palette.subtle(0.08f))
                }
            }
        }
    }
}

@Composable
fun ComingSoonScreen(title: String) {
    Screen(title = title) {
        EmptyHint(Icons.Rounded.Construction, "「$title」即将推出", "Android 版正在逐步移植中，这个功能会在后续版本加入。")
    }
}
