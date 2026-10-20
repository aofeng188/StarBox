package com.tianaofeng.xingxia.ui.screens.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.data.DecisionRecord
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.tools.toolInfo
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import java.time.Instant

/**
 * 首页最前面的回访卡（today 为 yyyyMMdd）：最近 3 天内（不含今天）点过「就这么办」、还没回访的最新一条决定，
 * 问「昨天转盘让你吃了「火锅」，后来怎么样？」。选「满意 / 一般 / 后悔」写 satisfaction 和回访时间，
 * 「跳过」只写回访时间。没有要回访的决定时什么都不显示，也不占位置（与 iOS 一致）
 */
@Composable
fun FollowUpCard(today: Int) {
    val day = DayKey.fromRaw(today) ?: return
    val records = Store.records.items
    val pending = remember(records, today) {
        records
            .filter { record ->
                record.adopted == true && record.satisfaction == null && record.followedUpAt == null &&
                    DayKey.of(Instant.ofEpochMilli(record.date)).daysTo(day) in 1..3
            }
            .maxByOrNull { it.date }
    } ?: return
    key(pending.id) { FollowUpContent(pending, day) }
}

@Composable
private fun FollowUpContent(record: DecisionRecord, today: DayKey) {
    val palette = LocalPalette.current
    val view = LocalView.current
    val (_, icon, colors) = toolInfo(record.tool)

    fun rate(option: DecisionSatisfaction) {
        Store.setSatisfaction(record.id, option.value)
        if (option == DecisionSatisfaction.REGRET) Haptics.selection() else Haptics.success()
        view.announceForAccessibility("已记下：${option.label}")
    }

    fun skip() {
        Store.setSatisfaction(record.id, null)
        Haptics.selection()
        view.announceForAccessibility("已跳过这次回访")
    }

    GlassCard(spacing = 14.dp) {
        SectionHeader("决定回访", icon = Icons.Rounded.Replay) {
            Text(
                "跳过",
                style = Fonts.subheadline,
                color = palette.secondaryText,
                modifier = Modifier
                    .pressable { skip() }
                    .clearAndSetSemantics {
                        contentDescription = "跳过这次回访"
                        role = Role.Button
                    }
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
            )
        }
        Row(
            Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ToolIcon(icon, colors, 40.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(FollowUpText.sentence(record, today), style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.text)
                record.askedQuestion?.let { SecondaryText("当时问的是「$it」", style = Fonts.caption, maxLines = 2) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (option in DecisionSatisfaction.entries) {
                Row(
                    Modifier
                        .weight(1f)
                        .pressable { rate(option) }
                        .glass(CircleShape)
                        .clearAndSetSemantics {
                            contentDescription = option.label
                            role = Role.Button
                        }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(option.emoji, style = Fonts.subheadline)
                    Text(" ${option.label}", style = Fonts.subheadline.weight(FontWeight.SemiBold), color = palette.text, maxLines = 1)
                }
            }
        }
    }
}
