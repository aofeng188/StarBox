package com.tianaofeng.xingxia.ui.screens.profile

import android.content.Context
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.rememberMinuteClock
import com.tianaofeng.xingxia.ui.components.ChipRow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.SelectChip
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.theme.AppTheme
import com.tianaofeng.xingxia.ui.theme.AppearanceMode
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemeManager
import com.tianaofeng.xingxia.ui.theme.ThemeRuleMode
import com.tianaofeng.xingxia.ui.theme.TimeSlot

@Composable
fun ThemeSettingsScreen() {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val now = rememberMinuteClock()
    val powerSave = (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode == true
    val (current, reason) = ThemeManager.resolve(now, Store.birth, powerSave)

    Screen(title = "主题与外观") {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ThemeSwatch(current, Modifier.size(56.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("当前：${current.name}", style = Fonts.headline)
                    SecondaryText(reason, style = Fonts.caption)
                }
                if (ThemeManager.mode == ThemeRuleMode.LAUNCH_RANDOM || ThemeManager.mode == ThemeRuleMode.DAILY_RANDOM) {
                    GlassButton("", Icons.Rounded.Shuffle) {
                        ThemeManager.mode = ThemeRuleMode.LAUNCH_RANDOM
                        ThemeManager.shuffleNow(current.id)
                    }
                }
            }
        }

        GlassCard {
            SectionHeader("外观", icon = Icons.Rounded.Palette)
            SegmentedPicker(AppearanceMode.entries, ThemeManager.appearance, { it.label }) { ThemeManager.appearance = it }
        }

        GlassCard(spacing = 10.dp) {
            SectionHeader("自动切换", icon = Icons.Rounded.Tune)
            for (mode in ThemeRuleMode.entries) {
                val selected = ThemeManager.mode == mode
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) palette.accent.copy(alpha = 0.12f) else Color.Transparent)
                        .pressable { ThemeManager.mode = mode }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(mode.label, style = Fonts.body.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal))
                        SecondaryText(mode.detail, style = Fonts.caption)
                    }
                    if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = palette.accent)
                }
            }
        }

        when (ThemeManager.mode) {
            ThemeRuleMode.FIXED -> ThemeGrid("选择主题", { it.id == ThemeManager.fixedThemeId }) { ThemeManager.fixedThemeId = it.id }
            ThemeRuleMode.DAILY_RANDOM, ThemeRuleMode.LAUNCH_RANDOM -> ThemeGrid("随机池（点按加入/移出）", { it.id in ThemeManager.randomPool }) { theme ->
                val pool = ThemeManager.randomPool
                ThemeManager.randomPool = if (theme.id in pool) pool - theme.id else pool + theme.id
            }
            ThemeRuleMode.TIME_OF_DAY -> GlassCard {
                SectionHeader("各时段的主题")
                for (slot in TimeSlot.entries) {
                    SecondaryText("${slot.label} · ${slot.range}", style = Fonts.footnote)
                    ThemeChips(ThemeManager.timeSlotThemes.getOrNull(slot.ordinal)) { id ->
                        ThemeManager.timeSlotThemes = ThemeManager.timeSlotThemes.toMutableList().also { it[slot.ordinal] = id }
                    }
                }
            }
            ThemeRuleMode.WEEKDAY -> GlassCard {
                SectionHeader("每周七天的主题")
                listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").forEachIndexed { index, day ->
                    SecondaryText(day, style = Fonts.footnote)
                    ThemeChips(ThemeManager.weekdayThemes.getOrNull(index)) { id ->
                        ThemeManager.weekdayThemes = ThemeManager.weekdayThemes.toMutableList().also { it[index] = id }
                    }
                }
            }
            else -> {}
        }

        GlassCard {
            SectionHeader("彩蛋", icon = Icons.Rounded.AutoAwesome)
            ToggleRow("省电模式切换为「黑洞」", ThemeManager.lowPowerOverride, "开启系统省电模式时自动使用深色省电主题") { ThemeManager.lowPowerOverride = it }
            ToggleRow("节日彩蛋", ThemeManager.festivalEggs, "春节、中秋、七夕、圣诞等节日自动换上应景主题") { ThemeManager.festivalEggs = it }
        }
    }
}

@Composable
private fun ThemeChips(selectedId: String?, onSelect: (String) -> Unit) {
    ChipRow { for (theme in AppTheme.ALL) SelectChip(theme.name, theme.id == selectedId) { onSelect(theme.id) } }
}

@Composable
private fun ThemeGrid(title: String, isSelected: (AppTheme) -> Boolean, onTap: (AppTheme) -> Unit) {
    val palette = LocalPalette.current
    GlassCard {
        SectionHeader(title)
        for (row in AppTheme.ALL.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (theme in row) {
                    val selected = isSelected(theme)
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .border(2.dp, if (selected) palette.accent else Color.Transparent, RoundedCornerShape(18.dp))
                            .pressable { onTap(theme) }
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ThemeSwatch(
                            theme,
                            Modifier
                                .fillMaxWidth()
                                .height(70.dp),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(theme.name, style = Fonts.subheadline.copy(fontWeight = FontWeight.SemiBold))
                                SecondaryText(theme.subtitle, style = Fonts.caption2, maxLines = 1)
                            }
                            if (selected) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(18.dp), tint = palette.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(theme: AppTheme, modifier: Modifier) {
    val p = theme.dark
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(p.backgroundTop, p.backgroundBottom))),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Brush.radialGradient(listOf(p.nebula[0].copy(alpha = 0.7f), Color.Transparent))),
        )
        Row(Modifier.align(Alignment.BottomEnd).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (c in listOf(p.accent, p.accentSecondary)) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(c),
                )
            }
        }
    }
}

@Composable
fun SettingsScreen() {
    Screen(title = "设置") {
        GlassCard {
            SectionHeader("反馈")
            ToggleRow("触感反馈", AppSettings.hapticsEnabled, "抽签、转盘、掷骰子时的震动") { AppSettings.hapticsEnabled = it }
            ToggleRow("摇一摇", AppSettings.shakeEnabled, "在求签、骰子、抛硬币等页面摇动手机触发") { AppSettings.shakeEnabled = it }
        }
        GlassCard {
            SectionHeader("显示")
            ToggleRow("背景动画", AppSettings.backgroundAnimation, "星空闪烁、星云流动和流星；关闭可省电") { AppSettings.backgroundAnimation = it }
        }
        GlassCard(spacing = 8.dp) {
            SectionHeader("关于", icon = Icons.Rounded.Info)
            SecondaryText("星匣 Android 版 1.0.0")
            SecondaryText("运势与占卜内容为娱乐性质，仅供参考。所有数据只保存在本机，不联网、不收集任何信息。", style = Fonts.caption)
            SecondaryText("音效、每日提醒、桌面小组件、心情与倒数日等功能会在后续版本加入。", style = Fonts.caption)
        }
    }
}
