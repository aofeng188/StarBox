package com.tianaofeng.xingxia.ui.screens.divination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.AuspiciousDay
import com.tianaofeng.xingxia.core.AuspiciousDays
import com.tianaofeng.xingxia.core.AuspiciousEvent
import com.tianaofeng.xingxia.core.AuspiciousReason
import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.EarthlyBranch
import com.tianaofeng.xingxia.core.lunar
import com.tianaofeng.xingxia.data.Countdown
import com.tianaofeng.xingxia.data.Prefs
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import com.tianaofeng.xingxia.ui.theme.hex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 择吉日页上次选的事项（存 AuspiciousEvent.key，占卜首页的入口卡片也读它；键名与 iOS 相同） */
internal const val AUSPICIOUS_EVENT_KEY = "auspiciousEvent"

/** 上次选的查找范围（天） */
private const val AUSPICIOUS_RANGE_KEY = "auspiciousRange"

/** 事项图标（对应 iOS 的 SF Symbol） */
private val AuspiciousEvent.icon: ImageVector
    get() = when (this) {
        AuspiciousEvent.MOVING -> Icons.Rounded.Home
        AuspiciousEvent.OPENING -> Icons.Rounded.Storefront
        AuspiciousEvent.WEDDING -> Icons.Rounded.Favorite
        AuspiciousEvent.TRAVEL -> Icons.Rounded.Luggage
        AuspiciousEvent.CONTRACT -> Icons.Rounded.Draw
        AuspiciousEvent.RENOVATION -> Icons.Rounded.Construction
        AuspiciousEvent.HAIRCUT -> Icons.Rounded.ContentCut
        AuspiciousEvent.INTERVIEW -> Icons.Rounded.Work
        AuspiciousEvent.CAR -> Icons.Rounded.DirectionsCar
        AuspiciousEvent.WORSHIP -> Icons.Rounded.VolunteerActivism
    }

/** 一次查找的条件；结果和条件一起存，条件变了、新结果还没算出来时旧结果先淡着显示 */
private data class AuspiciousSearch(
    val event: AuspiciousEvent,
    val days: Int,
    val weekendsOnly: Boolean,
    val today: DayKey,
    val animal: EarthlyBranch?,
)

private class AuspiciousResult(val search: AuspiciousSearch, val days: List<AuspiciousDay>)

/** 择吉日：选一件事，按黄历列出未来一段时间里办这件事的好日子，可以一键加进倒数日 */
@Composable
fun AuspiciousDaysScreen() {
    val palette = LocalPalette.current
    val view = LocalView.current
    // 跨过零点、隔天回到前台时从新的一天重新算
    val today = rememberToday()
    val eventKey = remember { Prefs.string(AUSPICIOUS_EVENT_KEY, AuspiciousEvent.MOVING.key) }
    val storedRange = remember { Prefs.int(AUSPICIOUS_RANGE_KEY, AuspiciousDays.DEFAULT_RANGE) }
    val event = AuspiciousEvent.of(eventKey.value) ?: AuspiciousEvent.MOVING
    val range = storedRange.value.takeIf { it in AuspiciousDays.RANGE_OPTIONS } ?: AuspiciousDays.DEFAULT_RANGE
    var weekendsOnly by rememberSaveable { mutableStateOf(false) }
    // 当前档案的生肖（年柱地支，以立春为界）
    val primary = Store.primary
    val animal = primary?.animal
    val search = AuspiciousSearch(event, range, weekendsOnly, today, animal)
    val found by produceState<AuspiciousResult?>(null, search) {
        val days = withContext(Dispatchers.Default) {
            AuspiciousDays.find(search.event, search.today, search.days, search.animal, search.weekendsOnly)
        }
        value = AuspiciousResult(search, days)
    }

    fun isAdded(day: DayKey, item: AuspiciousEvent): Boolean = Store.countdowns.items.any {
        it.title == item.label && it.kindValue == CountdownKind.COUNTDOWN && !it.isLunar && it.date == day.rawValue
    }

    fun addCountdown(day: DayKey, item: AuspiciousEvent) {
        if (isAdded(day, item)) return
        Store.saveCountdown(
            Countdown(title = item.label, emoji = item.emoji, date = day.rawValue, kind = CountdownKind.COUNTDOWN.key, colorIndex = item.ordinal),
        )
        Haptics.success()
        view.announceForAccessibility("已把${DateText.monthDay(day)}${item.label}加入倒数日")
    }

    Screen(title = "择吉日") {
        // 选事项
        GlassCard(spacing = 14.dp) {
            SectionHeader("想办什么事", subtitle = event.subtitle, icon = Icons.Rounded.EventAvailable)
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                for (row in AuspiciousEvent.entries.chunked(5)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (item in row) {
                            EventTile(item, item == event, Modifier.weight(1f)) {
                                if (item != event) {
                                    eventKey.value = item.key
                                    Haptics.selection()
                                }
                            }
                        }
                        // 最后一行不满时补位，格子宽度保持一致
                        repeat(5 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }

        // 范围与条件
        GlassCard(spacing = 12.dp) {
            SegmentedPicker(AuspiciousDays.RANGE_OPTIONS, range, { "未来 $it 天" }) { storedRange.value = it }
            ToggleRow("只看周末", weekendsOnly) { weekendsOnly = it }
            SecondaryText(
                if (primary != null && animal != null) {
                    val name = primary.name.ifEmpty { "你" }
                    "按${name}的生肖（属${animal.animal}）避开冲日：冲日会排到后面并标出。"
                } else {
                    "还没有档案。添加档案后，会帮你避开冲你生肖的日子。"
                },
                style = Fonts.footnote,
            )
        }

        // 结果
        SectionHeader(
            "宜${event.label}的日子",
            subtitle = "未来 $range 天" + if (weekendsOnly) " · 只看周末" else "",
            icon = event.icon,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        val result = found
        if (result == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = palette.accent)
            }
        } else {
            // 条件刚变、新结果还没算出来时，旧结果先淡着显示
            val shown = if (result.search == search) 1f else 0.4f
            if (result.days.isEmpty()) {
                GlassCard(Modifier.alpha(shown)) {
                    EmptyHint(Icons.Rounded.EventBusy, "这段时间没有合适的日子", emptyMessage(result.search))
                }
            } else {
                result.days.forEachIndexed { index, item ->
                    AuspiciousDayRow(
                        item,
                        isBest = index == 0,
                        today = today,
                        isAdded = isAdded(item.day, result.search.event),
                        modifier = Modifier.alpha(shown),
                    ) { addCountdown(item.day, result.search.event) }
                }
            }
        }

        SecondaryText(
            "按建除十二神、黄黑道值神、天德月德等神煞和黄历宜忌推算，仅作传统文化参考。",
            style = Fonts.caption2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun emptyMessage(search: AuspiciousSearch): String = when {
    search.weekendsOnly -> "关掉「只看周末」，或者把范围放宽试试"
    search.days < 90 -> "把范围放宽到 90 天试试"
    else -> "近期不太适合办这件事，过些日子再来看看"
}

/** 事项格子：圆形图标 + 名称 */
@Composable
private fun EventTile(event: AuspiciousEvent, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .pressable(onClick = onClick)
            // 读屏读事项名、说明和选中状态；点按动作来自前面的 pressable，要放在它后面
            .clearAndSetSemantics {
                contentDescription = "${event.label}，${event.subtitle}"
                role = Role.Button
                selected = isSelected
            }
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) Modifier.background(palette.accentBrush)
                    else Modifier.background(palette.accent.copy(alpha = if (palette.isDark) 0.14f else 0.12f)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(event.icon, null, Modifier.size(21.dp), tint = if (isSelected) palette.onAccent else palette.ink(palette.accent))
        }
        Text(
            event.label,
            style = Fonts.caption.weight(if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isSelected) palette.ink(palette.accent) else palette.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

/** 一个推荐的日子：日期块、农历、理由标签和「加入倒数日」 */
@Composable
private fun AuspiciousDayRow(
    item: AuspiciousDay,
    isBest: Boolean,
    today: DayKey,
    isAdded: Boolean,
    modifier: Modifier,
    onAdd: () -> Unit,
) {
    val palette = LocalPalette.current
    val day = item.day
    val lunar = remember(day) { day.lunar() }
    val relative = relativeText(today, day)
    val date = if (day.year == today.year) DateText.monthDay(day) else DateText.full(day)
    val spoken = (if (isBest) "首选，" else "") + "$date ${DateText.weekday(day)}，农历${lunar.text}，$relative。${item.summary}"
    GlassCard(modifier, padding = 16.dp, tint = if (item.clashesUser) palette.caution.copy(alpha = 0.08f) else null) {
        Row(
            Modifier.clearAndSetSemantics { contentDescription = spoken },
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DateBadge(day, today)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("农历${lunar.text}", style = Fonts.subheadline.weight(FontWeight.SemiBold), modifier = Modifier.weight(1f, fill = false))
                    if (isBest) TagChip("首选", palette.gold, Icons.Rounded.WorkspacePremium)
                }
                Flow(spacing = 6.dp) {
                    for (reason in item.reasons) {
                        TagChip(
                            reason.text,
                            reasonColor(reason.kind, palette),
                            if (reason.kind == AuspiciousReason.Kind.CLASH) Icons.Rounded.WarningAmber else null,
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryText(
                relative,
                style = Fonts.footnote,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { },
            )
            GlassButton(
                if (isAdded) "已在倒数日" else "加入倒数日",
                if (isAdded) Icons.Rounded.CheckCircle else Icons.Rounded.EditCalendar,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = if (isAdded) "${DateText.monthDay(day)}已在倒数日" else "把${DateText.monthDay(day)}加入倒数日"
                    role = Role.Button
                    if (isAdded) stateDescription = "已添加"
                },
                enabled = !isAdded,
                onClick = onAdd,
            )
        }
    }
}

/** 日期块：月份、日、星期，周末的星期用第二强调色 */
@Composable
private fun DateBadge(day: DayKey, today: DayKey) {
    val palette = LocalPalette.current
    val isWeekend = day.weekday == 0 || day.weekday == 6
    Column(
        Modifier
            .widthIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.accent.copy(alpha = if (palette.isDark) 0.12f else 0.1f))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            if (day.year == today.year) "${day.month}月" else "${day.year % 100}年${day.month}月",
            style = Fonts.caption2.weight(FontWeight.SemiBold),
            color = palette.ink(palette.accent),
            maxLines = 1,
        )
        Text("${day.day}", style = Fonts.title.weight(FontWeight.Bold), color = palette.text, maxLines = 1)
        Text(
            DateText.weekday(day),
            style = Fonts.caption2.weight(if (isWeekend) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isWeekend) palette.ink(palette.accentSecondary) else palette.secondaryText,
            maxLines = 1,
        )
    }
}

/** 理由标签配色：建除绿、黄道金、对口的宜用强调色、吉神用第二强调色、不冲生肖灰、冲生肖用警示色 */
private fun reasonColor(kind: AuspiciousReason.Kind, palette: ThemePalette): Color = when (kind) {
    AuspiciousReason.Kind.JIAN_CHU -> palette.positive
    AuspiciousReason.Kind.DEITY -> palette.gold
    AuspiciousReason.Kind.YI -> palette.accent
    AuspiciousReason.Kind.SHENSHA -> palette.accentSecondary
    AuspiciousReason.Kind.ZODIAC -> hex(0x94A3B8)
    AuspiciousReason.Kind.CLASH -> palette.caution
}

private fun relativeText(today: DayKey, day: DayKey): String = when (val days = today.daysTo(day)) {
    0 -> "就是今天"
    1 -> "明天"
    2 -> "后天"
    else -> "还有 $days 天"
}
