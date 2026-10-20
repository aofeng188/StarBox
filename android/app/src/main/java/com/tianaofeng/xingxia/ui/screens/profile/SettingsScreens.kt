package com.tianaofeng.xingxia.ui.screens.profile

import android.content.Context
import android.os.PowerManager
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tianaofeng.xingxia.system.ReminderScheduler
import com.tianaofeng.xingxia.system.SkyReminders
import com.tianaofeng.xingxia.ui.components.SoundEffect
import com.tianaofeng.xingxia.ui.components.SoundEngine
import com.tianaofeng.xingxia.ui.components.TextAction
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.SettingsBackupRestore
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
import com.tianaofeng.xingxia.BuildConfig
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
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val navigator = LocalNavigator.current
    var showTime by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            AppSettings.dailyReminderEnabled = true
            ReminderScheduler.reschedule(context)
        } else {
            permissionDenied = true
        }
    }
    // 天象日历页也有同一个开关；授权结果只作用于这里拨动的那一次
    val skyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) AppSettings.skyReminders = true else permissionDenied = true
        SkyReminders.reschedule(context)
    }

    Screen(title = "设置") {
        GlassCard {
            SectionHeader("反馈")
            ToggleRow("音效", AppSettings.soundEnabled, "摇签、翻牌、转盘等动作的音效") {
                AppSettings.soundEnabled = it
                if (it) SoundEngine.play(SoundEffect.POP)
            }
            if (AppSettings.soundEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("音量", style = Fonts.body)
                    Slider(
                        value = AppSettings.soundVolume / 100f,
                        onValueChange = { AppSettings.soundVolume = (it * 100).toInt() },
                        onValueChangeFinished = { SoundEngine.play(SoundEffect.TICK) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent),
                    )
                }
            }
            ToggleRow("触感反馈", AppSettings.hapticsEnabled, "抽签、转盘、掷骰子时的震动") { AppSettings.hapticsEnabled = it }
            ToggleRow("摇一摇", AppSettings.shakeEnabled, "在求签、骰子、抛硬币等页面摇动手机触发") { AppSettings.shakeEnabled = it }
        }
        GlassCard {
            SectionHeader("每日仪式")
            ToggleRow("每日仪式", AppSettings.dailyRitualEnabled) { AppSettings.dailyRitualEnabled = it }
            SecondaryText(
                "每天第一次打开星匣时，先看看今天的月亮、运势、签和心情，随时可以跳过。走完仪式的日子会点亮当晚的月相，一轮集齐八种月相得一枚满月徽章。",
                style = Fonts.caption,
            )
        }
        GlassCard {
            SectionHeader("提醒")
            ToggleRow("每天推送今日运势", AppSettings.dailyReminderEnabled, "按当前档案计算，点通知直接打开运势详解") { on ->
                if (on && !ReminderScheduler.hasPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    AppSettings.dailyReminderEnabled = on
                    ReminderScheduler.reschedule(context)
                }
            }
            if (AppSettings.dailyReminderEnabled) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showTime = true }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("提醒时间", style = Fonts.body, modifier = Modifier.weight(1f))
                    Text("%02d:%02d".format(AppSettings.reminderHour, AppSettings.reminderMinute), style = Fonts.body, color = palette.accent)
                }
                TextAction("现在发一条试试") { ReminderScheduler.showToday(context) }
            }
            ToggleRow("天象提醒", AppSettings.skyReminders, "满月、新月和流星雨极大的前一天晚上 8 点提醒") { on ->
                if (on && !ReminderScheduler.hasPermission(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    skyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    AppSettings.skyReminders = on
                    SkyReminders.reschedule(context)
                }
            }
            if (permissionDenied) {
                SecondaryText("没有通知权限。可以在系统设置 ▸ 应用 ▸ 星匣 ▸ 通知 里打开。", style = Fonts.caption)
            }
        }
        GlassCard {
            SectionHeader("倒数日")
            ToggleRow("档案生日自动倒数", AppSettings.profileBirthdaysInCountdowns) { AppSettings.profileBirthdaysInCountdowns = it }
            SecondaryText("档案里的生日会自动出现在倒数日列表、首页和小组件里，农历录入的按农历每年重复。关掉后只显示你自己添加的倒数日。", style = Fonts.caption)
        }
        GlassCard {
            SectionHeader("数据")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { navigator.open(Routes.BACKUP) }
                    .semantics { role = Role.Button }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.SettingsBackupRestore, null, tint = palette.accent)
                Text("数据备份", style = Fonts.body, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
            }
            SecondaryText("数据只保存在本机。换手机、重装前，可以在「数据备份」导出一份，iPhone 和 Android 版可以互相导入。", style = Fonts.caption)
        }
        GlassCard {
            SectionHeader("显示")
            ToggleRow("背景动画", AppSettings.backgroundAnimation, "星空闪烁、星云流动和流星；关闭可省电，系统省电模式下会自动暂停") { AppSettings.backgroundAnimation = it }
        }
        GlassCard(spacing = 8.dp) {
            SectionHeader("桌面小组件与快捷方式", icon = Icons.Rounded.Widgets)
            SecondaryText("长按桌面空白处 ▸ 小组件 ▸ 星匣，可以添加「今日运势」「星座运势」「倒数日」「心情」「抛硬币」五种小组件；星座运势和倒数日可以选择要显示的星座或日子，之后长按小组件能重新设置；心情和抛硬币直接在桌面上点。长按星匣图标还能直接打开今天吃什么、抛硬币、求签和今日运势。", style = Fonts.caption)
        }
        GlassCard(spacing = 8.dp) {
            SectionHeader("关于", icon = Icons.Rounded.Info)
            SecondaryText("星匣 Android 版 ${BuildConfig.VERSION_NAME}")
            SecondaryText("运势与占卜内容为娱乐性质，仅供参考。所有数据只保存在本机，不联网、不收集任何信息。", style = Fonts.caption)
        }
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = AppSettings.reminderHour, initialMinute = AppSettings.reminderMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    AppSettings.reminderHour = state.hour
                    AppSettings.reminderMinute = state.minute
                    ReminderScheduler.reschedule(context)
                    showTime = false
                }) { Text("完成") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } },
            title = { Text("提醒时间") },
            text = { TimePicker(state = state) },
        )
    }
}
