package com.tianaofeng.xingxia.ui.screens.ritual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.MoonBadgeProgress
import com.tianaofeng.xingxia.core.MoonBadges
import com.tianaofeng.xingxia.core.MoonPhaseName
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.CosmicBackdrop
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.theme.LocalPalette

/**
 * 「我的」页的月相徽章卡：已获得几枚满月徽章、本轮集到了哪几种月相，点开看说明（与 iOS 的 MoonBadgeCard 一致）。
 * 只算走完仪式的日子，跳过的不算
 */
@Composable
fun MoonBadgeCard() {
    val palette = LocalPalette.current
    val today = rememberToday()
    val ritualDays = Store.ritualDays.items
    val progress = remember(ritualDays, today) {
        MoonBadges.progress(ritualDays.filter { !it.skipped }.map { it.dayKey }, today.rawValue)
    }
    val count = progress.collected.size
    val enabled = AppSettings.dailyRitualEnabled
    var showInfo by rememberSaveable { mutableStateOf(false) }
    val open = { showInfo = true }

    GlassCard(
        Modifier
            .pressable(onClick = open)
            .clearAndSetSemantics {
                contentDescription = "月相徽章，已获得 ${progress.earned} 枚，本轮已集 $count/8 种月相"
                role = Role.Button
                onClick(label = "查看本轮月相和点亮规则") {
                    open()
                    true
                }
            },
        spacing = 14.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader("月相徽章", subtitle = "已获得 ${progress.earned} 枚", icon = Icons.Rounded.NightsStay, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
        MoonPhaseStrip(progress.collected)
        SecondaryText(if (enabled) "本轮已集 $count/8 种月相" else "本轮已集 $count/8 种月相 · 每日仪式已关闭")
    }

    if (showInfo) MoonBadgeInfoDialog(progress, today) { showInfo = false }
}

/** 八种月相排成一行：集到的亮、下面带一个小圆点，没集到的暗 */
@Composable
fun MoonPhaseStrip(collected: Set<MoonPhaseName>, size: Dp = 28.dp) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = moonStripSpokenText(collected) }) {
        for (name in MoonPhaseName.entries) {
            val got = name in collected
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                MoonPhaseView(name, size = size, glow = false, isLit = got)
                // 新月本来就是暗的，靠圆点区分有没有集到
                Box(
                    Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .then(if (got) Modifier.background(palette.accentBrush) else Modifier.background(palette.subtle(0.12f))),
                )
            }
        }
    }
}

/** 读屏用：已集到哪些、还差哪些 */
fun moonStripSpokenText(collected: Set<MoonPhaseName>): String {
    val got = MoonPhaseName.entries.filter { it in collected }.map { it.label }
    val missing = MoonPhaseName.entries.filter { it !in collected }.map { it.label }
    if (got.isEmpty()) return "本轮还没有集到月相"
    if (missing.isEmpty()) return "本轮八种月相都已集齐"
    return "已集到${got.joinToString("、")}；还差${missing.joinToString("、")}"
}

/** 月相徽章说明：已获得几枚、本轮月相、怎么点亮；仪式关闭时可以直接打开 */
@Composable
private fun MoonBadgeInfoDialog(progress: MoonBadgeProgress, today: DayKey, onDismiss: () -> Unit) {
    val lunation = remember(today) { MoonBadges.lunation(today) }
    val start = lunation.start
    val next = lunation.endExclusive
    val lastDay = next.adding(-1)
    val complete = progress.collected.size == MoonPhaseName.entries.size
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val palette = LocalPalette.current
        // 全屏弹层自己的星空背景，里面的玻璃模糊它（不是被盖住的主窗口）
        CosmicBackdrop {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                ) {
                    CircleIconButton(Icons.Rounded.Close, "关闭", Modifier.align(Alignment.CenterStart), onClick = onDismiss)
                    Text(
                        "月相徽章",
                        style = Fonts.headline,
                        color = palette.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics { heading() },
                    )
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        GlassCard(
                            Modifier.semantics(mergeDescendants = true) {},
                            spacing = 10.dp,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            MoonPhaseView(0.5, size = 96.dp, isLit = progress.earned > 0)
                            Text(
                                if (progress.earned > 0) "已获得 ${progress.earned} 枚满月徽章" else "还没有满月徽章",
                                style = Fonts.title3.copy(fontWeight = FontWeight.Bold),
                                color = palette.text,
                                textAlign = TextAlign.Center,
                            )
                            SecondaryText("本轮已集 ${progress.collected.size}/8 种月相", textAlign = TextAlign.Center)
                        }

                        GlassCard(spacing = 14.dp) {
                            SectionHeader(
                                "本轮月相",
                                subtitle = "${DateText.monthDay(start)} – ${DateText.monthDay(lastDay)}",
                                icon = Icons.Rounded.DarkMode,
                            )
                            for (row in MoonPhaseName.entries.chunked(4)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (name in row) {
                                        val got = name in progress.collected
                                        Column(
                                            Modifier
                                                .weight(1f)
                                                .clearAndSetSemantics { contentDescription = "${name.label}，${if (got) "已集到" else "还没集到"}" },
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            MoonPhaseView(name, size = 40.dp, glow = false, isLit = got)
                                            Text(name.label, style = Fonts.caption, color = palette.text, maxLines = 1)
                                            Icon(
                                                if (got) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                                null,
                                                Modifier.size(14.dp),
                                                tint = if (got) palette.accent else palette.secondaryText,
                                            )
                                        }
                                    }
                                }
                            }
                            SecondaryText(
                                if (complete) {
                                    "本轮已经集齐，下一轮从 ${DateText.monthDay(next)} 新月开始。"
                                } else {
                                    "本轮还剩 ${today.daysTo(next)} 天，${DateText.monthDay(next)} 新月开始下一轮。"
                                },
                                style = Fonts.footnote,
                            )
                        }

                        GlassCard(spacing = 10.dp) {
                            SectionHeader("怎么点亮", icon = Icons.Rounded.AutoAwesome)
                            Text("每天走完「每日仪式」，就点亮那天晚上的月相；跳过的日子不算。", style = Fonts.subheadline, color = palette.text)
                            Text(
                                "从一次新月到下一次新月算一轮，大约 29.5 天，新月当天算新一轮的第一天。一轮里集齐新月、娥眉月、上弦月、盈凸月、满月、亏凸月、下弦月、残月八种月相，就得到一枚满月徽章。",
                                style = Fonts.subheadline,
                                color = palette.text,
                            )
                            Text(
                                "每种月相会持续三四天，不用天天打开；新月只在一轮的开头和结尾出现，这几天记得来看看。",
                                style = Fonts.subheadline,
                                color = palette.text,
                            )
                        }

                        if (!AppSettings.dailyRitualEnabled) {
                            GlassCard {
                                Text("每日仪式已关闭，月相暂时不会点亮。", style = Fonts.subheadline, color = palette.text)
                                GlassButton("打开每日仪式", Icons.Rounded.NightsStay) {
                                    AppSettings.dailyRitualEnabled = true
                                    Haptics.success()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
