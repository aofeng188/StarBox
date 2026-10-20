package com.tianaofeng.xingxia.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.core.AlmanacDay
import com.tianaofeng.xingxia.core.ContentLibrary
import com.tianaofeng.xingxia.core.DailyFortune
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneCategory
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.HoroscopeEngine
import com.tianaofeng.xingxia.core.HoroscopePeriod
import com.tianaofeng.xingxia.core.MoonInfo
import com.tianaofeng.xingxia.core.Shensha
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.HomeCard
import com.tianaofeng.xingxia.data.Profile
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.rememberMinuteClock
import com.tianaofeng.xingxia.ui.components.ColorDot
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
import com.tianaofeng.xingxia.ui.components.StarRatingView
import com.tianaofeng.xingxia.ui.components.TagChip
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.components.rememberToday
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.common.CategoryMeter
import com.tianaofeng.xingxia.ui.screens.common.LuckyItem
import com.tianaofeng.xingxia.ui.screens.common.QuoteCard
import com.tianaofeng.xingxia.ui.screens.common.YiJiRow
import com.tianaofeng.xingxia.ui.screens.common.ZodiacBadge
import com.tianaofeng.xingxia.ui.screens.common.stickLevelColor
import com.tianaofeng.xingxia.ui.screens.common.stickLevelInk
import com.tianaofeng.xingxia.ui.screens.tools.FavoriteTool
import com.tianaofeng.xingxia.ui.screens.records.CountdownStrip
import com.tianaofeng.xingxia.ui.screens.records.MoodCheckInCard
import com.tianaofeng.xingxia.ui.screens.journal.FollowUpCard
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.toColor
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape

@Composable
fun TodayScreen() {
    val navigator = LocalNavigator.current
    // 过零点、回到前台时刷新「今天」，各卡片跟着换日；问候语按分钟刷新
    val today = rememberToday()
    val now = rememberMinuteClock()
    val birth = Store.birth
    val fortune = remember(today, birth) { FortuneEngine.daily(birth, today) }
    val almanac = remember(today) { AlmanacDay(today) }

    LaunchedEffect(Unit) {
        if (!AppSettings.hasOnboarded && Store.primary == null) {
            delay(600)
            AppSettings.hasOnboarded = true
            navigator.open(Routes.profileEditor(onboarding = true))
        }
    }

    Screen(title = "今日", isRoot = true, actions = { ProfileSwitcher() }) {
        // 换档案时问候语里的名字也要跟着换，不能等下一分钟
        TodayHeader(today, almanac, remember(now, Store.primary?.name) { Store.greeting() })
        if (Store.primary == null) {
            GlassCard(onClick = { navigator.open(Routes.profileEditor(onboarding = Store.profiles.items.isEmpty())) }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val palette = LocalPalette.current
                    GlowIcon(Icons.Rounded.PersonAddAlt1, palette.accent, palette.accentSecondary)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("填写生日，解锁专属运势", style = Fonts.headline)
                        SecondaryText("根据你的八字与星座，每天计算只属于你的运势", style = Fonts.caption)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
                }
            }
        }

        val hidden = AppSettings.hiddenHomeCards
        for (card in AppSettings.homeCards.filter { it !in hidden }) {
            when (card) {
                // 最近几天点过「就这么办」的决定，问问后来怎么样；没有要回访的就不显示
                HomeCard.FOLLOW_UP -> FollowUpCard(today.rawValue)
                HomeCard.FORTUNE -> FortuneHeroCard(fortune)
                HomeCard.FAMILY -> FamilyFortuneCard(today)
                HomeCard.ALMANAC -> AlmanacCard(fortune, almanac)
                HomeCard.HOROSCOPE -> HoroscopeMiniCard(today)
                HomeCard.STICK -> StickMiniCard(today)
                HomeCard.QUICK_TOOLS -> QuickToolsRow()
                HomeCard.COUNTDOWN -> CountdownStrip(today) { navigator.open(Routes.COUNTDOWNS) }
                HomeCard.MOOD -> MoodCheckInCard(today) { navigator.open(Routes.MOOD) }
                HomeCard.QUOTE -> QuoteCard(fortune.quote)
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlassButton("编辑首页", Icons.Rounded.Tune) { navigator.open(Routes.HOME_EDITOR) }
        }
    }
}

@Composable
private fun TodayHeader(day: DayKey, almanac: AlmanacDay, greeting: String) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
        SecondaryText(greeting, style = Fonts.subheadline.weight(FontWeight.Medium))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GradientText("${day.month}月${day.day}日", style = Fonts.display(40))
            Text(DateText.weekday(day), style = Fonts.title3, color = palette.secondaryText, modifier = Modifier.padding(bottom = 6.dp))
        }
        SecondaryText(
            "农历${almanac.lunar.text} · ${almanac.lunar.yearGanZhi.name}${almanac.lunar.yearGanZhi.branch.animal}年 · ${almanac.termText}",
            style = Fonts.footnote,
        )
        if (almanac.festivals.isNotEmpty()) {
            Flow(spacing = 6.dp) { for (f in almanac.festivals) TagChip(f, icon = Icons.Rounded.Celebration) }
        }
        MoonPhaseCapsule(day)
    }
}

/** 头部的月相小胶囊：今晚的月相和亮面比例，点开看天象日历 */
@Composable
private fun MoonPhaseCapsule(day: DayKey) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val moon = remember(day) { MoonInfo.on(day) }
    Row(
        Modifier
            .padding(top = 4.dp)
            .pressable { navigator.open(Routes.SKY_CALENDAR) }
            .clearAndSetSemantics {
                contentDescription = "今晚${moon.name.label}，亮面 ${moon.percent}%"
                role = Role.Button
            }
            .glass(CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MoonDisc(moon.phase, 18.dp)
        Text(moon.name.label, style = Fonts.footnote.weight(FontWeight.SemiBold))
        Text("亮 ${moon.percent}%", style = Fonts.footnote, color = palette.secondaryText)
        Icon(Icons.Rounded.ChevronRight, null, Modifier.size(14.dp), tint = palette.tertiaryText)
    }
}

/** 档案切换 */
@Composable
private fun ProfileSwitcher() {
    val navigator = LocalNavigator.current
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(40.dp)
                .pressable { open = true }
                .glass(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(Store.primary?.avatar ?: "👤", style = Fonts.title3)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            for (profile in Store.sortedProfiles) {
                DropdownMenuItem(
                    text = { Text("${profile.avatar} ${profile.name}" + if (profile.id == Store.primary?.id) "  ✓" else "") },
                    onClick = {
                        Store.setPrimary(profile)
                        Haptics.selection()
                        open = false
                    },
                )
            }
            if (Store.profiles.items.isNotEmpty()) HorizontalDivider()
            DropdownMenuItem(
                text = { Text("管理档案") },
                leadingIcon = { Icon(Icons.Rounded.People, null) },
                onClick = {
                    open = false
                    navigator.open(Routes.PROFILES)
                },
            )
        }
    }
}

@Composable
fun FortuneHeroCard(fortune: DailyFortune) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    GlassCard(onClick = { navigator.open(Routes.FORTUNE) }, spacing = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            ScoreRing(fortune.score, size = 112.dp, lineWidth = 11.dp, label = "运势指数")
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SecondaryText(if (fortune.isPersonalized) "专属运势" else "今日运势", style = Fonts.caption.weight(FontWeight.SemiBold))
                GradientText(fortune.levelName, style = Fonts.display(34, FontWeight.Black))
                StarRatingView(fortune.stars, size = 13.dp)
                Text(fortune.summary, style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }
        val categories = FortuneCategory.entries
        for (row in categories.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (category in row) CategoryMeter(category, fortune.score(category), Modifier.weight(1f))
            }
        }
        HorizontalDivider(color = palette.subtle(0.12f))
        Row {
            LuckyItem("幸运色", Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ColorDot(fortune.luckyColor.toColor(), 14.dp, glow = true)
                    Text(fortune.luckyColor.name, style = Fonts.footnote.weight(FontWeight.SemiBold), maxLines = 1)
                }
            }
            LuckyItem("幸运数字", Modifier.weight(1f)) { Text("${fortune.luckyNumber}", style = Fonts.footnote.weight(FontWeight.SemiBold)) }
            LuckyItem("喜神方位", Modifier.weight(1f)) { Text(fortune.luckyDirection, style = Fonts.footnote.weight(FontWeight.SemiBold)) }
            LuckyItem("吉时", Modifier.weight(1f)) { Text(fortune.luckyHour.hourName, style = Fonts.footnote.weight(FontWeight.SemiBold)) }
        }
    }
}

/** 家人今日：多个档案时并排看各自的今日运势，点别人的可以切换当前档案 */
@Composable
private fun FamilyFortuneCard(today: DayKey) {
    val profiles = Store.sortedProfiles
    if (profiles.size <= 1) return
    val navigator = LocalNavigator.current
    val view = LocalView.current
    val clash = remember(today) { AlmanacDay(today).clashAnimal }
    var switching by remember { mutableStateOf<Profile?>(null) }
    GlassCard {
        SectionHeader("家人今日", subtitle = "今日冲${clash.animal}", icon = Icons.Rounded.People)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (profile in profiles) {
                val isCurrent = profile.id == Store.primary?.id
                FamilyMemberTile(profile, today, clashes = profile.animal == clash, isCurrent = isCurrent) {
                    // 当前档案直接看运势详情，别人的先确认再切换
                    if (isCurrent) navigator.open(Routes.FORTUNE) else switching = profile
                }
            }
        }
    }
    switching?.let { profile ->
        AlertDialog(
            onDismissRequest = { switching = null },
            confirmButton = {
                TextButton(onClick = {
                    switching = null
                    Store.setPrimary(profile)
                    Haptics.selection()
                    view.announceForAccessibility("已切换到${profile.name}")
                }) { Text("切换到「${profile.name}」") }
            },
            dismissButton = { TextButton(onClick = { switching = null }) { Text("取消") } },
            title = { Text("切换当前档案？") },
            text = { Text("首页、每日提醒和小组件会改为显示「${profile.name}」的运势。") },
        )
    }
}

@Composable
private fun FamilyMemberTile(profile: Profile, today: DayKey, clashes: Boolean, isCurrent: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val birth = profile.birth
    val fortune = remember(birth, today) { FortuneEngine.daily(birth, today) }
    val color = palette.color(profile.colorIndex)
    val animal = profile.animal.animal
    val spoken = buildList {
        add(profile.name)
        add("今日运势 ${fortune.score} 分")
        add(fortune.levelName)
        if (clashes) add("今日冲$animal")
        if (isCurrent) add("当前查看")
    }.joinToString("，")
    Column(
        Modifier
            .widthIn(min = 84.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.1f))
            .pressable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(46.dp)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f))
                    .then(if (isCurrent) Modifier.border(2.dp, palette.accent, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) { Text(profile.avatar, style = Fonts.title2) }
            if (isCurrent) {
                Icon(
                    Icons.Rounded.CheckCircle, null,
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(3.dp, 3.dp)
                        .size(16.dp)
                        .background(palette.onAccent, CircleShape),
                    tint = palette.accent,
                )
            }
        }
        Text(profile.name, style = Fonts.caption.weight(FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("${fortune.score}", style = Fonts.title3.weight(FontWeight.Bold), color = palette.inkColor(profile.colorIndex))
            SecondaryText(fortune.levelName, style = Fonts.caption2, modifier = Modifier.padding(bottom = 2.dp))
        }
        if (clashes) TagChip("今日冲$animal", color = palette.caution)
    }
}

@Composable
private fun AlmanacCard(fortune: DailyFortune, almanac: AlmanacDay) {
    val navigator = LocalNavigator.current
    val traditional = AppSettings.almanacShowsTraditional
    GlassCard(spacing = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(
                if (traditional) "传统黄历" else "今日宜忌",
                subtitle = if (traditional) "${almanac.jianChu.name} · ${almanac.deity.name}" else null,
                icon = Icons.Rounded.MenuBook,
                modifier = Modifier.weight(1f),
            )
            SegmentedPicker(listOf(false, true), traditional, { if (it) "黄历" else "趣味" }, Modifier.width(120.dp)) {
                AppSettings.almanacShowsTraditional = it
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .pressable { navigator.open(Routes.ALMANAC) },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            YiJiRow(true, if (traditional) almanac.yi else fortune.funYi)
            YiJiRow(false, if (traditional) almanac.ji else fortune.funJi)
            if (traditional) {
                if (almanac.shensha.isNotEmpty()) {
                    // 和黄历页一致：吉神金色，四绝、四离灰色
                    val gold = LocalPalette.current.gold
                    Box(Modifier.clearAndSetSemantics { contentDescription = "神煞：" + almanac.shensha.joinToString("、") }) {
                        Flow(spacing = 6.dp) {
                            for (name in almanac.shensha) {
                                val lucky = Shensha.isLucky(name)
                                TagChip(
                                    name,
                                    color = if (lucky) gold else hex(0x94A3B8),
                                    icon = if (lucky) Icons.Rounded.AutoAwesome else Icons.Rounded.Warning,
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.SwapHoriz, null, Modifier.size(14.dp), tint = LocalPalette.current.secondaryText)
                    SecondaryText(almanac.clash, style = Fonts.caption)
                    Icon(Icons.Rounded.LocationOn, null, Modifier.size(14.dp), tint = LocalPalette.current.secondaryText)
                    SecondaryText("财神${almanac.wealthDirection}", style = Fonts.caption)
                }
            }
        }
    }
}

@Composable
private fun HoroscopeMiniCard(today: DayKey) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val sign = Store.birth?.let { ZodiacSign.sunSign(it) } ?: ZodiacSign.entries.getOrElse(AppSettings.lastViewedSign) { ZodiacSign.ARIES }
    val horoscope = remember(sign, today) { HoroscopeEngine.horoscope(sign, HoroscopePeriod.TODAY, today) }
    GlassCard(onClick = { navigator.open(Routes.HOROSCOPE) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ZodiacBadge(sign, 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sign.label, style = Fonts.headline)
                    SecondaryText(sign.dateRange, style = Fonts.caption)
                }
                StarRatingView(horoscope.stars, size = 12.dp)
                Text(horoscope.comment, style = Fonts.subheadline, color = palette.text.copy(alpha = 0.85f))
                // 不放星座幸运色：首页运势卡已有按五行算的幸运色，两个并列容易混淆
                TagChip("速配 ${horoscope.matchSign.shortName}", icon = Icons.Rounded.Favorite)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
    }
}

@Composable
private fun StickMiniCard(today: DayKey) {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    val draw = Store.stickDraws.items.lastOrNull { it.dayKey == today.rawValue && it.profileId == Store.primary?.id }
    GlassCard(onClick = { navigator.open(Routes.STICK) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                Modifier
                    .size(56.dp, 64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(hex(0xB91C1C), hex(0x7F1D1D)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(28.dp), tint = hex(0xFFB347))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (draw != null) {
                    val stick = ContentLibrary.stick(draw.stickNumber)
                    SecondaryText("今日已求签", style = Fonts.caption)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stick.numberText, style = Fonts.serif(17, bold = true))
                        Text(
                            stick.levelDisplayName,
                            style = Fonts.caption.weight(FontWeight.Bold),
                            color = stickLevelInk(stick.level, palette.isDark),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(stickLevelColor(stick.level).copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    SecondaryText("「${stick.title}」${stick.poem[0]}", maxLines = 1)
                } else {
                    Text("今日一签", style = Fonts.headline)
                    SecondaryText("心诚则灵，摇一摇签筒，看看今天的指引")
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = palette.tertiaryText)
        }
    }
}

@Composable
private fun QuickToolsRow() {
    val navigator = LocalNavigator.current
    val palette = LocalPalette.current
    GlassCard {
        SectionHeader("快捷工具", subtitle = "工具页右上角点星标收藏", icon = Icons.Rounded.Bolt)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 按收藏顺序显示；认不出的 id（已下线的工具）跳过
            for (tool in AppSettings.favoriteTools.mapNotNull { FavoriteTool.of(it) }) {
                QuickTool(tool.label, { ToolIcon(tool.icon, tool.colors, 54.dp) }) { navigator.open(tool.route) }
            }
            QuickTool("添加", {
                val accent = palette.accent
                Box(
                    Modifier
                        .size(54.dp)
                        .drawBehind {
                            val stroke = 1.5.dp.toPx()
                            drawRoundRect(
                                color = accent.copy(alpha = 0.6f),
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(size.minDimension * 0.3f),
                                style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, null, Modifier.size(26.dp), tint = palette.ink(accent)) }
            }, description = "管理快捷工具", subtle = true) { navigator.open(Routes.FAVORITES) }
        }
    }
}

@Composable
private fun QuickTool(
    title: String,
    icon: @Composable () -> Unit,
    description: String? = null,
    subtle: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .width(66.dp)
            .pressable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description ?: title
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Text(
            title, style = Fonts.caption2, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (subtle) LocalPalette.current.secondaryText else LocalPalette.current.text,
        )
    }
}

/** 编辑首页：显示/隐藏、调整顺序 */
@Composable
fun HomeEditorScreen() {
    val palette = LocalPalette.current
    Screen(title = "编辑首页") {
        SecondaryText("打开开关显示模块，用箭头调整顺序。「决定回访」固定在最前，有要回访的决定时才出现。", style = Fonts.footnote)
        GlassCard(padding = 8.dp, spacing = 0.dp) {
            // 「决定回访」固定在最前、不能隐藏，不进编辑列表
            val cards = AppSettings.homeCards.filter { it.isEditable }
            cards.forEachIndexed { index, card ->
                val shown = card !in AppSettings.hiddenHomeCards
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(card.label, style = Fonts.body, color = if (shown) palette.text else palette.tertiaryText)
                        if (card == HomeCard.FAMILY) SecondaryText("有两个及以上档案时显示", style = Fonts.caption)
                    }
                    CircleIconButton(
                        Icons.Rounded.KeyboardArrowUp, "上移", size = 34.dp, enabled = index > 0,
                    ) {
                        AppSettings.homeCards = cards.toMutableList().apply { add(index - 1, removeAt(index)) }
                    }
                    Spacer(Modifier.width(6.dp))
                    CircleIconButton(
                        Icons.Rounded.KeyboardArrowDown, "下移", size = 34.dp, enabled = index < cards.size - 1,
                    ) {
                        AppSettings.homeCards = cards.toMutableList().apply { add(index + 1, removeAt(index)) }
                    }
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Switch(
                        checked = shown,
                        onCheckedChange = {
                            AppSettings.hiddenHomeCards = if (it) AppSettings.hiddenHomeCards - card else AppSettings.hiddenHomeCards + card
                        },
                        colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = palette.accent),
                    )
                }
                if (index < cards.size - 1) HorizontalDivider(color = palette.subtle(0.08f))
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
