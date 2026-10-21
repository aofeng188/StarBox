package com.tianaofeng.xingxia.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tianaofeng.xingxia.BuildConfig
import com.tianaofeng.xingxia.core.BaZi
import com.tianaofeng.xingxia.core.BirthCity
import com.tianaofeng.xingxia.core.BirthInfo
import com.tianaofeng.xingxia.core.Compatibility
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.EarthlyBranch
import com.tianaofeng.xingxia.core.GanZhi
import com.tianaofeng.xingxia.core.LunarDate
import com.tianaofeng.xingxia.core.WuXing
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.core.lunar
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Profile
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.BigActionButton
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import com.tianaofeng.xingxia.ui.components.DayKeySaver
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Flow
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassButton
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.GlowIcon
import com.tianaofeng.xingxia.ui.components.GradientText
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.ScoreRing
import com.tianaofeng.xingxia.ui.components.Screen
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.SegmentedPicker
import com.tianaofeng.xingxia.ui.components.SelectChip
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.TextAction
import com.tianaofeng.xingxia.ui.components.ToggleRow
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.fadingHorizontalScroll
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.common.ZodiacBadge
import com.tianaofeng.xingxia.ui.screens.divination.CompatibilityShareButton
import com.tianaofeng.xingxia.ui.screens.divination.GlassTextField
import com.tianaofeng.xingxia.ui.screens.divination.ShareSide
import com.tianaofeng.xingxia.ui.screens.ritual.MoonBadgeCard
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.ThemeManager
import com.tianaofeng.xingxia.ui.theme.color
import com.tianaofeng.xingxia.ui.theme.hex
import java.time.Instant
import java.time.ZoneOffset

private fun Profile.birthText(): String =
    DateText.full(birthdayKey) + if (hasBirthTime) " %02d:%02d".format(birthHour, birthMinute) else ""

// region 我的

@Composable
fun MeScreen() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val profiles = Store.sortedProfiles
    val primary = Store.primary

    Screen(title = "我的", isRoot = true) {
        if (primary != null) {
            val bazi = BaZi(primary.birth)
            GlassCard(onClick = { navigator.open(Routes.profileEditor(primary.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(palette.accent.copy(alpha = 0.35f), palette.accentSecondary.copy(alpha = 0.35f))))
                            .border(2.dp, palette.accentBrush, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text(primary.avatar, style = TextStyle(fontSize = 40.sp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(primary.name, style = Fonts.title2)
                        SecondaryText(primary.birthText())
                        Flow(spacing = 6.dp) {
                            TagChip(primary.sunSign.label, icon = Icons.Rounded.AutoAwesome)
                            TagChip("属${bazi.zodiacAnimal.animal}", palette.accentSecondary)
                            TagChip("日主${bazi.dayMaster.name}${bazi.dayMaster.element.label}", bazi.dayMaster.element.color)
                        }
                    }
                    Icon(Icons.Rounded.Edit, null, tint = palette.secondaryText)
                }
            }
        } else {
            GlassCard(onClick = { navigator.open(Routes.profileEditor(onboarding = profiles.isEmpty())) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    GlowIcon(Icons.Rounded.PersonAddAlt1, palette.accent, palette.accentSecondary, 64.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("创建我的档案", style = Fonts.title3)
                        SecondaryText("填写生日，开启专属运势")
                    }
                }
            }
        }

        // 月相徽章：走完每日仪式的日子点亮当晚的月相
        MoonBadgeCard()

        GlassCard(padding = 6.dp, spacing = 0.dp) {
            MeRow(Icons.Rounded.People, listOf(hex(0x60A5FA), hex(0x6366F1)), "档案管理", "${profiles.size} 个") { navigator.open(Routes.PROFILES) }
            RowDivider()
            MeRow(Icons.Rounded.GridOn, listOf(hex(0xA78BFA), hex(0xEC4899)), "我的命盘", "八字 · 星盘") { navigator.open(Routes.BIRTH_CHART) }
        }
        GlassCard(padding = 6.dp, spacing = 0.dp) {
            MeRow(Icons.Rounded.Palette, palette.spectrum.take(2), "主题与外观", ThemeManager.resolve(java.time.LocalDateTime.now(), Store.birth, false).first.name) { navigator.open(Routes.THEMES) }
            RowDivider()
            MeRow(Icons.Rounded.Settings, listOf(hex(0x94A3B8), hex(0x475569)), "设置", "音效 · 震动 · 提醒") { navigator.open(Routes.SETTINGS) }
            RowDivider()
            MeRow(Icons.Rounded.SettingsBackupRestore, listOf(hex(0x2DD4BF), hex(0x0EA5E9)), "数据备份", backupValue()) { navigator.open(Routes.BACKUP) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("${Store.records.items.size}", "次随机决定", Icons.Rounded.Casino, Modifier.weight(1f))
            val first = (profiles.map { it.createdAt } + Store.records.items.map { it.date }).minOrNull() ?: System.currentTimeMillis()
            StatTile("${DayKey.of(Instant.ofEpochMilli(first)).daysTo(DayKey.today) + 1}", "天相伴", Icons.Rounded.AutoAwesome, Modifier.weight(1f))
        }

        SecondaryText(
            "星匣 Android 版 ${BuildConfig.VERSION_NAME} · 所有数据仅保存在你的设备上",
            style = Fonts.caption,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 「我的」里数据备份一行的说明：上次导出是哪天 */
private fun backupValue(): String {
    val last = AppSettings.backupLastExportAt
    if (last <= 0) return "导出 · 恢复"
    return when (val days = DayKey.of(Instant.ofEpochMilli(last)).daysTo(DayKey.today)) {
        in Int.MIN_VALUE..0 -> "今天已备份"
        1 -> "昨天已备份"
        else -> "$days 天前备份"
    }
}

@Composable
private fun RowDivider() = HorizontalDivider(Modifier.padding(start = 58.dp), color = LocalPalette.current.subtle(0.08f))

@Composable
private fun MeRow(icon: ImageVector, colors: List<Color>, title: String, value: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ToolIcon(icon, palette.identity(colors), 34.dp)
        Text(title, style = Fonts.body, modifier = Modifier.weight(1f))
        SecondaryText(value)
        Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
    }
}

@Composable
private fun StatTile(value: String, label: String, icon: ImageVector, modifier: Modifier) {
    GlassCard(modifier, padding = 16.dp, spacing = 6.dp) {
        com.tianaofeng.xingxia.ui.components.GradientIcon(icon, 20.dp)
        Text(value, style = Fonts.display(28))
        SecondaryText(label, style = Fonts.caption)
    }
}

// endregion

// region 档案列表

@Composable
fun ProfileListScreen() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val profiles = Store.sortedProfiles
    Screen(title = "档案管理", actions = {
        CircleIconButton(Icons.Rounded.Add, "添加") { navigator.open(Routes.profileEditor(onboarding = profiles.isEmpty())) }
    }) {
        if (profiles.isEmpty()) {
            EmptyHint(Icons.Rounded.People, "还没有档案", "添加自己、家人和朋友的生日，随时切换查看运势")
        } else {
            GlassCard(padding = 6.dp, spacing = 0.dp) {
                profiles.forEachIndexed { index, profile ->
                    val isPrimary = Store.primary?.id == profile.id
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { navigator.open(Routes.profileEditor(profile.id)) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(palette.accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) { Text(profile.avatar, style = Fonts.title2) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(profile.name, style = Fonts.headline)
                                SecondaryText(profile.relation, style = Fonts.caption)
                            }
                            SecondaryText("${profile.sunSign.label} · 属${profile.animal.animal} · ${DateText.full(profile.birthdayKey)}", style = Fonts.caption)
                        }
                        if (isPrimary) {
                            Icon(Icons.Rounded.CheckCircle, "当前档案", tint = palette.accent)
                        } else {
                            TextAction("设为当前") {
                                Store.setPrimary(profile)
                                Haptics.selection()
                            }
                        }
                        if (index > 0) {
                            CircleIconButton(Icons.Rounded.SwapVert, "上移", size = 32.dp) {
                                val ordered = profiles.toMutableList().apply { add(index - 1, removeAt(index)) }
                                Store.profiles.update { ordered.mapIndexed { i, p -> p.copy(sortIndex = i) } }
                            }
                        }
                    }
                    if (index < profiles.size - 1) RowDivider()
                }
            }
            SecondaryText("点按编辑；首页会显示「当前」档案的运势。", style = Fonts.caption)
        }
    }
}

// endregion

// region 档案编辑

private val AVATARS = listOf(
    "🌟", "🌙", "☀️", "🪐", "🌸", "🍀", "🔥", "💧", "🦊", "🐱", "🐶", "🐼",
    "🐰", "🦄", "🐯", "🐲", "🐻", "🐧", "🐳", "🦋", "🍑", "🍓", "🧸", "🎈",
)
private val RELATIONS = listOf("自己", "伴侣", "家人", "朋友", "同事", "其他")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(profileId: String?, isOnboarding: Boolean) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val existing = remember(profileId) { Store.profiles.items.firstOrNull { it.id == profileId } }
    val initialLunar = remember(existing) { existing?.birthdayKey?.lunar() }

    // 旋转、切换深浅色等配置变化后保留已填的内容
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var relation by rememberSaveable { mutableStateOf(existing?.relation ?: if (isOnboarding || Store.profiles.items.isEmpty()) "自己" else "朋友") }
    var avatar by rememberSaveable { mutableStateOf(existing?.avatar ?: "🌟") }
    var useLunar by rememberSaveable { mutableStateOf(existing?.enteredAsLunar ?: false) }
    var birthday by rememberSaveable(stateSaver = DayKeySaver) { mutableStateOf(existing?.birthdayKey ?: DayKey(1998, 6, 18)) }
    var lunarYear by rememberSaveable { mutableIntStateOf(initialLunar?.lunarYear ?: 1998) }
    var lunarMonth by rememberSaveable { mutableIntStateOf(initialLunar?.month ?: 5) }
    var lunarDay by rememberSaveable { mutableIntStateOf(initialLunar?.day ?: 1) }
    var lunarLeap by rememberSaveable { mutableStateOf(initialLunar?.isLeapMonth ?: false) }
    var knowsTime by rememberSaveable { mutableStateOf(existing?.hasBirthTime ?: false) }
    var hour by rememberSaveable { mutableIntStateOf(existing?.birthHour ?: 12) }
    var minute by rememberSaveable { mutableIntStateOf(existing?.birthMinute ?: 0) }
    var cityId by rememberSaveable { mutableStateOf(existing?.birthCityId ?: "beijing") }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showTime by rememberSaveable { mutableStateOf(false) }
    var showCity by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val leapOfYear = LunarDate.leapMonth(lunarYear)
    val converted = LunarDate.solarDate(lunarYear, lunarMonth, lunarDay, lunarLeap && leapOfYear == lunarMonth)
    val resolvedBirthday = if (useLunar) converted ?: birthday else birthday
    val preview = BirthInfo(resolvedBirthday, if (knowsTime) hour else null, if (knowsTime) minute else null, if (knowsTime) cityId else null)

    fun save() {
        if (useLunar && converted == null) return
        val base = existing ?: Profile(sortIndex = (Store.profiles.items.maxOfOrNull { it.sortIndex } ?: -1) + 1)
        val saved = base.copy(
            name = name.trim().ifEmpty { if (relation == "自己") "我" else relation },
            relation = relation,
            avatar = avatar,
            birthday = resolvedBirthday.rawValue,
            enteredAsLunar = useLunar,
            hasBirthTime = knowsTime,
            birthHour = hour,
            birthMinute = minute,
            birthCityId = if (knowsTime) cityId else null,
        )
        Store.saveProfile(saved)
        if (isOnboarding || Store.primary == null || Store.primary?.id == saved.id) Store.setPrimary(saved)
        Haptics.success()
        navigator.back()
    }

    if (showCity) {
        CityPicker(cityId, onBack = { showCity = false }) {
            cityId = it
            showCity = false
        }
        return
    }

    Screen(
        title = if (isOnboarding) "创建档案" else if (existing == null) "新建档案" else "编辑档案",
        actions = { TextAction("保存", enabled = !(useLunar && converted == null)) { save() } },
    ) {
        if (isOnboarding) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientText("欢迎来到星匣", style = Fonts.title)
                SecondaryText(
                    "填写生日后，星匣会结合你的八字、星座与每天的干支、月相，计算只属于你的运势。所有数据只保存在本机。",
                    textAlign = TextAlign.Center,
                )
            }
        }

        GlassCard {
            SectionHeader("头像与称呼")
            Row(Modifier.fadingHorizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (emoji in AVATARS) {
                    val selected = emoji == avatar
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (selected) palette.accent.copy(alpha = 0.3f) else Color.Transparent)
                            .border(2.dp, if (selected) palette.accent else Color.Transparent, CircleShape)
                            .clickable {
                                avatar = emoji
                                Haptics.selection()
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, style = TextStyle(fontSize = 26.sp)) }
                }
            }
            GlassTextField(name, "名字或昵称") { name = it }
            Flow(spacing = 8.dp) { for (r in RELATIONS) SelectChip(r, r == relation) { relation = r } }
        }

        GlassCard {
            SectionHeader("生日")
            SegmentedPicker(listOf(false, true), useLunar, { if (it) "农历" else "公历" }) { useLunar = it }
            if (useLunar) {
                LunarPickerRow("年", "$lunarYear 年（${GanZhi.ofYear(lunarYear).name}${GanZhi.ofYear(lunarYear).branch.animal}）",
                    (1901..DayKey.today.year).reversed().map { it to "$it 年（${GanZhi.ofYear(it).name}${GanZhi.ofYear(it).branch.animal}）" }) { lunarYear = it }
                LunarPickerRow("月", LunarDate.MONTH_NAMES[lunarMonth - 1] + "月", (1..12).map { it to LunarDate.MONTH_NAMES[it - 1] + "月" }) { lunarMonth = it }
                if (leapOfYear != 0 && leapOfYear == lunarMonth) {
                    ToggleRow("闰${LunarDate.MONTH_NAMES[leapOfYear - 1]}月", lunarLeap) { lunarLeap = it }
                }
                LunarPickerRow("日", LunarDate.DAY_NAMES[lunarDay - 1], (1..30).map { it to LunarDate.DAY_NAMES[it - 1] }) { lunarDay = it }
                if (converted != null) {
                    InfoRow("对应公历", DateText.full(converted))
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, Modifier.size(16.dp), tint = palette.caution)
                        Text("这一天在农历中不存在，请检查日期", style = Fonts.footnote, color = palette.caution)
                    }
                }
            } else {
                PickerButtonRow("生日", DateText.full(birthday)) { showDate = true }
            }
        }

        GlassCard {
            SectionHeader("出生时间（可选）")
            ToggleRow("知道出生时间", knowsTime) { knowsTime = it }
            if (knowsTime) {
                PickerButtonRow("出生时间", "%02d:%02d".format(hour, minute)) { showTime = true }
                val branch = EarthlyBranch.forHour(hour)
                LunarPickerRow("只记得时辰？", "${branch.hourName} ${branch.hourRange}", (0 until 12).map { it to "${EarthlyBranch(it).hourName}  ${EarthlyBranch(it).hourRange}" }) {
                    hour = (it * 2) % 24
                    minute = 0
                }
                PickerButtonRow("出生城市", BirthCity.find(cityId)?.name ?: "北京") { showCity = true }
            }
            SecondaryText("填写出生时间后可以排出完整八字（含时柱，按真太阳时校正）以及上升星座。不知道可以先不填。", style = Fonts.caption)
        }

        GlassCard(spacing = 8.dp) {
            SectionHeader("预览")
            val bazi = BaZi(preview)
            InfoRow("太阳星座", ZodiacSign.sunSign(preview).label)
            InfoRow("生肖", "${bazi.zodiacAnimal.animalEmoji} ${bazi.zodiacAnimal.animal}")
            InfoRow("八字", bazi.text)
            ZodiacSign.ascendant(preview)?.let { InfoRow("上升星座", it.label) }
        }

        BigActionButton("保存", Icons.Rounded.Check, enabled = !(useLunar && converted == null)) { save() }

        if (existing != null && !isOnboarding) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (confirmDelete) {
                    GlassButton("确定删除「${existing.name}」") {
                        Store.deleteProfile(existing)
                        navigator.back()
                    }
                } else {
                    GlassButton("删除这个档案") { confirmDelete = true }
                }
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = birthday.localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = 1900..DayKey.today.year,
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { birthday = DayKey.of(Instant.ofEpochMilli(it), ZoneOffset.UTC) }
                    showDate = false
                }) { Text("完成") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }

    if (showTime) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    showTime = false
                }) { Text("完成") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("取消") } },
            title = { Text("出生时间") },
            text = { TimePicker(state = state) },
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = Fonts.body, modifier = Modifier.weight(1f))
        SecondaryText(value, style = Fonts.body)
    }
}

@Composable
private fun PickerButtonRow(title: String, value: String, onClick: () -> Unit) {
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

/** 下拉选择行 */
@Composable
private fun <T> LunarPickerRow(title: String, value: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PickerButtonRow(title, value) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.height(360.dp)) {
            for ((key, label) in options) {
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelect(key)
                    open = false
                })
            }
        }
    }
}

@Composable
private fun CityPicker(selection: String, onBack: () -> Unit, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    var search by remember { mutableStateOf("") }
    Screen(title = "出生城市", onBack = onBack) {
        GlassTextField(search, "搜索城市") { search = it }
        for (region in BirthCity.REGIONS) {
            val cities = BirthCity.ALL.filter { it.region == region && (search.isBlank() || search.trim() in it.name) }
            if (cities.isEmpty()) continue
            SecondaryText(region, style = Fonts.footnote.copy(fontWeight = FontWeight.SemiBold))
            GlassCard(padding = 6.dp, spacing = 0.dp) {
                cities.forEachIndexed { i, city ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(city.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(city.name, style = Fonts.body, modifier = Modifier.weight(1f))
                        SecondaryText(
                            "%.1f°%s %.1f°%s".format(
                                kotlin.math.abs(city.latitude), if (city.latitude >= 0) "N" else "S",
                                kotlin.math.abs(city.longitude), if (city.longitude >= 0) "E" else "W",
                            ),
                            style = Fonts.caption,
                        )
                        if (city.id == selection) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Rounded.Check, null, tint = palette.accent)
                        }
                    }
                    if (i < cities.size - 1) HorizontalDivider(color = palette.subtle(0.08f))
                }
            }
        }
    }
}

// endregion

// region 命盘

private val MASTER_TEXTS = listOf(
    "如参天大树，正直向上，有担当与领导力。",
    "如花草藤蔓，柔韧灵活，善于适应环境。",
    "如太阳之火，热情开朗，光明磊落。",
    "如烛火星光，细腻温暖，富有洞察力。",
    "如高山厚土，稳重可靠，包容力强。",
    "如田园沃土，务实细心，善于经营。",
    "如刀剑之金，果断刚毅，重情重义。",
    "如珠玉之金，精致敏锐，追求完美。",
    "如江河之水，聪慧奔放，富有谋略。",
    "如雨露之水，温柔内敛，直觉敏锐。",
)

@Composable
fun BirthChartScreen() {
    val palette = LocalPalette.current
    val profiles = Store.sortedProfiles
    var selectedId by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    val profile = profiles.firstOrNull { it.id == (selectedId ?: Store.primary?.id) } ?: profiles.firstOrNull()

    Screen(title = "命盘", actions = {
        if (profiles.size > 1) {
            Box {
                CircleIconButton(Icons.Rounded.People, "切换档案") { menu = true }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    for (p in profiles) DropdownMenuItem(text = { Text("${p.avatar} ${p.name}") }, onClick = {
                        selectedId = p.id
                        menu = false
                    })
                }
            }
        }
    }) {
        if (profile == null) {
            EmptyHint(Icons.Rounded.People, "还没有档案", "在「我的 ▸ 档案管理」里添加生日后即可查看命盘")
            return@Screen
        }
        val birth = profile.birth
        val bazi = BaZi(birth)
        val sun = ZodiacSign.sunSign(birth)
        val moon = ZodiacSign.moonSign(birth.moment)
        val rising = ZodiacSign.ascendant(birth)
        val lunar = profile.birthdayKey.lunar()

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(profile.avatar, style = TextStyle(fontSize = 54.sp))
            Text(profile.name, style = Fonts.title2)
            SecondaryText("公历 ${profile.birthText()}")
            SecondaryText("农历 ${lunar.yearGanZhi.name}年 ${lunar.text}")
        }

        GlassCard(spacing = 14.dp) {
            SectionHeader("星盘三巨头", subtitle = "太阳 · 月亮 · 上升", icon = Icons.Rounded.AutoAwesome)
            Row {
                BigThree("太阳星座", sun, Modifier.weight(1f))
                BigThree("月亮星座", moon, Modifier.weight(1f))
                if (rising != null) {
                    BigThree("上升星座", rising, Modifier.weight(1f))
                } else {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("?", style = Fonts.display(34), color = palette.secondaryText)
                        Text("上升星座", style = Fonts.caption.weight(FontWeight.SemiBold))
                        SecondaryText("需要出生时间", style = Fonts.caption2)
                    }
                }
            }
            Text(sun.profile, style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f))
        }

        // 星盘：十二星座 + 等宫制十二宫，标出太阳、月亮和上升
        GlassCard(spacing = 14.dp) {
            SectionHeader("星盘", subtitle = if (birth.hasTime) "等宫制 · 第一宫从上升点起" else "太阳与月亮的位置", icon = Icons.Rounded.NightsStay)
            NatalWheel(birth)
        }

        GlassCard(spacing = 14.dp) {
            SectionHeader("四柱八字", subtitle = if (bazi.trueSolarApplied) "已按真太阳时校正" else "未填出生时间，仅排三柱", icon = Icons.Rounded.GridOn)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pillar("年柱", bazi.year, false, Modifier.weight(1f))
                Pillar("月柱", bazi.month, false, Modifier.weight(1f))
                Pillar("日柱", bazi.day, true, Modifier.weight(1f))
                val hourPillar = bazi.hour
                if (hourPillar != null) {
                    Pillar("时柱", hourPillar, false, Modifier.weight(1f))
                } else {
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.subtle(0.04f))
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SecondaryText("时柱", style = Fonts.caption)
                        Text("?", style = Fonts.serif(30, bold = true), color = palette.tertiaryText)
                        Text("?", style = Fonts.serif(30, bold = true), color = palette.tertiaryText)
                        SecondaryText("待补充", style = Fonts.caption2)
                    }
                }
            }
            val master = bazi.dayMaster
            Text("日主 ${master.name}${master.element.label}：${MASTER_TEXTS[master.index]}", style = Fonts.subheadline)
        }

        GlassCard(spacing = 14.dp) {
            SectionHeader("五行分布", icon = Icons.Rounded.BarChart)
            val counts = bazi.elementCounts
            val maxCount = maxOf(1, counts.values.max())
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                for (element in WuXing.entries) {
                    val value = counts.getValue(element)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$value", style = Fonts.caption.weight(FontWeight.Bold), color = palette.ink(element.color))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height((100f * value / maxCount).coerceAtLeast(4f).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Brush.verticalGradient(listOf(element.color, element.color.copy(alpha = 0.6f)))),
                        )
                        SecondaryText(element.label, style = Fonts.caption)
                    }
                }
            }
            val favorable = bazi.favorableElement
            Text("五行最旺为「${bazi.strongestElement.label}」，相对偏弱为「${bazi.weakestElements.joinToString("、") { it.label }}」。", style = Fonts.subheadline)
            Text(
                "喜用参考「${favorable.label}」：幸运色 ${favorable.luckyColors.take(2).joinToString("、") { it.name }}；方位 ${favorable.direction}；数字 ${favorable.numbers.joinToString("、")}。",
                style = Fonts.subheadline,
            )
            SecondaryText("以上为简化的娱乐性推算，仅供参考。", style = Fonts.caption2)
        }
    }
}

@Composable
private fun BigThree(title: String, sign: ZodiacSign, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ZodiacBadge(sign, 54.dp)
        Text(sign.label, style = Fonts.subheadline.weight(FontWeight.SemiBold))
        SecondaryText(title, style = Fonts.caption2)
    }
}

@Composable
private fun Pillar(title: String, pillar: GanZhi, highlight: Boolean, modifier: Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlight) palette.accent.copy(alpha = 0.14f) else palette.subtle(0.04f))
            .then(if (highlight) Modifier.border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp)) else Modifier)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SecondaryText(title, style = Fonts.caption)
        Text(pillar.stem.name, style = Fonts.serif(30, bold = true), color = palette.ink(pillar.stem.element.color))
        Text(pillar.branch.name, style = Fonts.serif(30, bold = true), color = palette.ink(pillar.branch.element.color))
        SecondaryText(pillar.nayin, style = Fonts.caption2)
    }
}

// endregion

// region 缘分配对

private sealed interface PairSide {
    data class OfProfile(val id: String) : PairSide
    data class OfSign(val sign: ZodiacSign) : PairSide
}

private data class SideInfo(val sign: ZodiacSign, val animal: EarthlyBranch?, val title: String, val avatar: String)

@Composable
fun CompatibilityScreen() {
    val palette = LocalPalette.current
    val profiles = Store.sortedProfiles
    var left by remember { mutableStateOf<PairSide>(profiles.getOrNull(0)?.let { PairSide.OfProfile(it.id) } ?: PairSide.OfSign(ZodiacSign.ARIES)) }
    var right by remember { mutableStateOf<PairSide>(profiles.getOrNull(1)?.let { PairSide.OfProfile(it.id) } ?: PairSide.OfSign(ZodiacSign.LEO)) }

    fun resolve(side: PairSide): SideInfo = when (side) {
        is PairSide.OfProfile -> profiles.firstOrNull { it.id == side.id }?.let { SideInfo(it.sunSign, it.animal, it.name, it.avatar) }
            ?: SideInfo(ZodiacSign.ARIES, null, "白羊座", "")
        is PairSide.OfSign -> SideInfo(side.sign, null, side.sign.label, "")
    }

    val a = resolve(left)
    val b = resolve(right)
    val animals = a.animal?.let { x -> b.animal?.let { y -> x to y } }
    val result = Compatibility.between(a.sign, b.sign, animals)

    Screen(
        title = "缘分配对",
        spacing = 18.dp,
        actions = {
            CompatibilityShareButton(ShareSide(a.sign, a.animal, a.title, a.avatar), ShareSide(b.sign, b.animal, b.title, b.avatar), result)
        },
    ) {
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SidePicker(a, profiles, Modifier.weight(1f)) { left = it }
                Icon(Icons.Rounded.Favorite, null, Modifier.size(30.dp), tint = palette.accent)
                SidePicker(b, profiles, Modifier.weight(1f)) { right = it }
            }
        }
        GlassCard(horizontalAlignment = Alignment.CenterHorizontally, spacing = 14.dp) {
            ScoreRing(result.score, size = 150.dp, lineWidth = 14.dp, label = "契合度")
            GradientText(result.title, style = Fonts.title)
            SecondaryText("${a.sign.label} × ${b.sign.label} · ${result.aspect.label}")
            Text(result.summary, style = Fonts.body)
            Text(result.elementNote, style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f))
            result.animalNote?.let { Text(it, style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f)) }
        }
        GlassCard(spacing = 10.dp) {
            SectionHeader("相处小建议", icon = Icons.Rounded.Lightbulb)
            for (tip in result.tips) Text("✦ $tip", style = Fonts.subheadline)
        }
    }
}

@Composable
private fun SidePicker(info: SideInfo, profiles: List<Profile>, modifier: Modifier, onSelect: (PairSide) -> Unit) {
    val palette = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            Modifier.pressable { open = true },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (info.avatar.isEmpty()) {
                ZodiacBadge(info.sign, 64.dp)
            } else {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(palette.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) { Text(info.avatar, style = TextStyle(fontSize = 36.sp)) }
            }
            Text(info.title, style = Fonts.headline)
            SecondaryText(info.animal?.let { "${info.sign.label} · 属${it.animal}" } ?: info.sign.dateRange, style = Fonts.caption)
            Text("更换 ▾", style = Fonts.caption2, color = palette.accent)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.height(380.dp)) {
            for (p in profiles) DropdownMenuItem(text = { Text("${p.avatar} ${p.name}") }, onClick = {
                onSelect(PairSide.OfProfile(p.id))
                open = false
            })
            if (profiles.isNotEmpty()) HorizontalDivider()
            for (s in ZodiacSign.entries) DropdownMenuItem(text = { Text("${s.glyph} ${s.label}") }, onClick = {
                onSelect(PairSide.OfSign(s))
                open = false
            })
        }
    }
}

// endregion
