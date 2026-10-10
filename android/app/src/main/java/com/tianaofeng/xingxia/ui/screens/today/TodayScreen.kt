package com.tianaofeng.xingxia.ui.screens.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAddAlt1
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import com.tianaofeng.xingxia.ui.components.CircleIconButton
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.HomeCard
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.rememberMinuteClock
import com.tianaofeng.xingxia.ui.components.ColorDot
import com.tianaofeng.xingxia.ui.components.ColorDotChip
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
import com.tianaofeng.xingxia.ui.screens.tools.ToolKind
import com.tianaofeng.xingxia.ui.screens.records.CountdownStrip
import com.tianaofeng.xingxia.ui.screens.records.MoodCheckInCard
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex
import com.tianaofeng.xingxia.ui.theme.toColor
import kotlinx.coroutines.delay
import androidx.compose.foundation.shape.CircleShape

@Composable
fun TodayScreen() {
    val navigator = LocalNavigator.current
    val today = DayKey.of(rememberMinuteClock().toLocalDate())
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
        TodayHeader(today, almanac, Store.greeting())
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
                HomeCard.FORTUNE -> FortuneHeroCard(fortune)
                HomeCard.ALMANAC -> AlmanacCard(fortune, almanac)
                HomeCard.HOROSCOPE -> HoroscopeMiniCard(today)
                HomeCard.STICK -> StickMiniCard(today)
                HomeCard.QUICK_TOOLS -> QuickToolsRow()
                HomeCard.COUNTDOWN -> CountdownStrip { navigator.open(Routes.COUNTDOWNS) }
                HomeCard.MOOD -> MoodCheckInCard { navigator.open(Routes.MOOD) }
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
                Flow(spacing = 6.dp) {
                    TagChip("速配 ${horoscope.matchSign.shortName}", icon = Icons.Rounded.Favorite)
                    ColorDotChip(horoscope.luckyColor)
                }
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
    val tools = AppSettings.favoriteTools.mapNotNull { ToolKind.of(it) }.filter { it.available }
    GlassCard {
        SectionHeader("快捷工具", subtitle = "在百宝箱长按工具可收藏", icon = Icons.Rounded.Bolt)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            for (tool in tools) {
                QuickTool(tool.label, { ToolIcon(tool.icon, tool.colors, 54.dp) }) { navigator.open(Routes.tool(tool)) }
            }
            QuickTool("答案之书", { ToolIcon(Icons.Rounded.MenuBook, listOf(hex(0xF5B83D), hex(0xB45309)), 54.dp) }) {
                navigator.open(Routes.ANSWERS)
            }
        }
    }
}

@Composable
private fun QuickTool(title: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        Modifier
            .width(66.dp)
            .pressable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon()
        Text(title, style = Fonts.caption2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** 编辑首页：显示/隐藏、调整顺序 */
@Composable
fun HomeEditorScreen() {
    val palette = LocalPalette.current
    Screen(title = "编辑首页") {
        SecondaryText("打开开关显示模块，用箭头调整顺序。", style = Fonts.footnote)
        GlassCard(padding = 8.dp, spacing = 0.dp) {
            val cards = AppSettings.homeCards
            cards.forEachIndexed { index, card ->
                val shown = card !in AppSettings.hiddenHomeCards
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(card.label, style = Fonts.body, modifier = Modifier.weight(1f), color = if (shown) palette.text else palette.tertiaryText)
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
