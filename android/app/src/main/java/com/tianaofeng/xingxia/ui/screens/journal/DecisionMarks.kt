package com.tianaofeng.xingxia.ui.screens.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.JournalStats
import com.tianaofeng.xingxia.core.QuestionKind
import com.tianaofeng.xingxia.data.DecisionRecord
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.tools.toolInfo
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemePalette
import com.tianaofeng.xingxia.ui.theme.hex
import java.time.Instant

/** 回访结果（DecisionRecord.satisfaction）：1 满意，0 一般，-1 后悔（与 iOS 的 DecisionSatisfaction 一致） */
enum class DecisionSatisfaction(val value: Int, val label: String, val emoji: String, val icon: ImageVector) {
    SATISFIED(1, "满意", "😊", Icons.Rounded.ThumbUp),
    OKAY(0, "一般", "😐", Icons.Rounded.RemoveCircle),
    REGRET(-1, "后悔", "😣", Icons.Rounded.ThumbDown);

    fun color(palette: ThemePalette): Color = when (this) {
        SATISFIED -> palette.positive
        OKAY -> hex(0x94A3B8)
        REGRET -> palette.negative
    }

    companion object {
        fun of(value: Int?): DecisionSatisfaction? = entries.firstOrNull { it.value == value }
    }
}

/** 决定后面的小标记：照做了 / 再想想，满意 / 一般 / 后悔。什么都没有时不占位置 */
@Composable
fun DecisionMarks(record: DecisionRecord, showsPending: Boolean = false) {
    val palette = LocalPalette.current
    val adopted = record.adopted
    val satisfaction = DecisionSatisfaction.of(record.satisfaction)
    if (adopted == null && satisfaction == null && !showsPending) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        when (adopted) {
            true -> TagChip("照做了", palette.positive, Icons.Rounded.CheckCircle)
            false -> TagChip("再想想", hex(0x94A3B8), Icons.Rounded.Replay)
            null -> if (satisfaction == null) TagChip("还没决定", hex(0x94A3B8), Icons.Rounded.RadioButtonUnchecked)
        }
        if (satisfaction != null) TagChip(satisfaction.label, satisfaction.color(palette), satisfaction.icon)
    }
}

/** 点开一条决定补记：照做了吗、后来怎么样。改动立即保存；记录被删掉时自动关闭 */
@Composable
fun DecisionEditDialog(recordId: String, onDismiss: () -> Unit) {
    val dismiss by rememberUpdatedState(onDismiss)
    val record = Store.records.items.firstOrNull { it.id == recordId }
    if (record == null) {
        LaunchedEffect(Unit) { dismiss() }
        return
    }
    val (name, icon, colors) = toolInfo(record.tool)
    val instant = Instant.ofEpochMilli(record.date)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToolIcon(icon, colors, 40.dp)
                Column(Modifier.semantics(mergeDescendants = true) {}) {
                    Text(name, style = Fonts.headline)
                    SecondaryText("${DateText.full(DayKey.of(instant))} ${DateText.time(instant)}", style = Fonts.caption)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    record.askedQuestion?.let { SecondaryText("「$it」") }
                    Text(record.result, style = Fonts.title3.weight(FontWeight.Bold))
                }
                Text("照做了吗？", style = Fonts.subheadline.weight(FontWeight.SemiBold))
                SegmentedPicker(listOf<Boolean?>(null, true, false), record.adopted, { adoptedLabel(it) }) { value ->
                    if (value == null) Store.clearAdopted(record.id) else Store.setAdopted(record.id, value)
                    Haptics.selection()
                }
                Text("后来怎么样？", style = Fonts.subheadline.weight(FontWeight.SemiBold))
                SegmentedPicker(listOf<Int?>(null, 1, 0, -1), record.satisfaction, { value -> DecisionSatisfaction.of(value)?.label ?: "不评价" }) { value ->
                    // 选「不评价」也记下回访时间，首页不再来问
                    Store.setSatisfaction(record.id, value)
                    Haptics.selection()
                }
            }
        },
    )
}

private fun adoptedLabel(value: Boolean?): String = when (value) {
    true -> "照做了"
    false -> "再想想"
    null -> "还没决定"
}

/** 「问一问」的问题类型名（本月回顾「最纠结」用，与 iOS 的 QuestionKind.title 一致） */
internal fun questionKindTitle(kind: QuestionKind): String = when (kind) {
    QuestionKind.YES_NO -> "是非题"
    QuestionKind.CHOICE -> "多选一"
    QuestionKind.FOOD -> "吃什么"
    QuestionKind.TIMING -> "挑时机"
    QuestionKind.RELATIONSHIP -> "感情事"
    QuestionKind.GENERAL -> "随便问问"
}

/**
 * 回访卡上的那句话（与 iOS 的 FollowUpText 规则、文案一致）：「{哪天}{谁}{怎么}「{结果}」，后来怎么样？」
 * - 哪天：昨天 / 前天 / 三天前（按日期差，回访只看最近 3 天）
 * - 谁：food、nearby →「星匣」；wheel 转盘、coin 硬币、dice 骰子、finger 指尖轮盘、answers 答案之书、
 *   lottery 抽奖、number 随机数、groups 随机分组；其他用工具名
 * - 怎么：answers →「告诉你」；food、nearby →「让你吃了」；wheel、lottery 且问题判断为吃什么 →「让你吃了」；其余 →「帮你选了」
 */
object FollowUpText {
    fun dayWord(day: DayKey, today: DayKey): String = when (day.daysTo(today)) {
        1 -> "昨天"
        2 -> "前天"
        3 -> "三天前"
        else -> "前几天"
    }

    fun subject(tool: String): String = when (tool) {
        "food", "nearby" -> "星匣"
        "wheel" -> "转盘"
        "coin" -> "硬币"
        "dice" -> "骰子"
        "finger" -> "指尖轮盘"
        "answers" -> "答案之书"
        "lottery" -> "抽奖"
        "number" -> "随机数"
        "groups" -> "随机分组"
        else -> toolInfo(tool).first
    }

    fun verb(record: DecisionRecord): String = when (record.tool) {
        "answers" -> "告诉你"
        "food", "nearby" -> "让你吃了"
        "wheel", "lottery" ->
            if (JournalStats.questionKind(record.tool, record.askedQuestion) == QuestionKind.FOOD) "让你吃了" else "帮你选了"
        else -> "帮你选了"
    }

    fun sentence(record: DecisionRecord, today: DayKey): String {
        val day = DayKey.of(Instant.ofEpochMilli(record.date))
        return "${dayWord(day, today)}${subject(record.tool)}${verb(record)}「${record.result}」，后来怎么样？"
    }
}
