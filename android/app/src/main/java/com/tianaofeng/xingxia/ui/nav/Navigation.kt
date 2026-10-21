package com.tianaofeng.xingxia.ui.nav

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.LocalFavoriteId
import com.tianaofeng.xingxia.ui.components.LocalGlassBackdrop
import com.tianaofeng.xingxia.ui.components.LocalTabBarHeight
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.ask.AskScreen
import com.tianaofeng.xingxia.ui.screens.ask.DecisionPrompt
import com.tianaofeng.xingxia.ui.screens.divination.AlmanacDetailScreen
import com.tianaofeng.xingxia.ui.screens.divination.AnswerBookScreen
import com.tianaofeng.xingxia.ui.screens.divination.AuspiciousDaysScreen
import com.tianaofeng.xingxia.ui.screens.divination.FortuneDetailScreen
import com.tianaofeng.xingxia.ui.screens.divination.HoroscopeScreen
import com.tianaofeng.xingxia.ui.screens.divination.StickScreen
import com.tianaofeng.xingxia.ui.screens.divination.TarotJournalScreen
import com.tianaofeng.xingxia.ui.screens.divination.TarotScreen
import com.tianaofeng.xingxia.ui.screens.journal.AnnualReviewScreen
import com.tianaofeng.xingxia.ui.screens.journal.JournalScreen
import com.tianaofeng.xingxia.ui.screens.profile.BackupScreen
import com.tianaofeng.xingxia.ui.screens.profile.BirthChartScreen
import com.tianaofeng.xingxia.ui.screens.profile.CompatibilityScreen
import com.tianaofeng.xingxia.ui.screens.profile.MeScreen
import com.tianaofeng.xingxia.ui.screens.profile.ProfileEditorScreen
import com.tianaofeng.xingxia.ui.screens.profile.ProfileListScreen
import com.tianaofeng.xingxia.ui.screens.profile.SettingsScreen
import com.tianaofeng.xingxia.ui.screens.profile.ThemeSettingsScreen
import com.tianaofeng.xingxia.ui.screens.records.ComingSoonScreen
import com.tianaofeng.xingxia.ui.screens.records.CountdownListScreen
import com.tianaofeng.xingxia.ui.screens.records.FortuneCalendarScreen
import com.tianaofeng.xingxia.ui.screens.records.MoodScreen
import com.tianaofeng.xingxia.ui.screens.records.HistoryScreen
import com.tianaofeng.xingxia.ui.screens.ritual.DailyRitualHost
import com.tianaofeng.xingxia.ui.screens.today.HomeEditorScreen
import com.tianaofeng.xingxia.ui.screens.today.SkyCalendarScreen
import com.tianaofeng.xingxia.ui.screens.today.TodayScreen
import com.tianaofeng.xingxia.ui.screens.tools.FavoriteToolsScreen
import com.tianaofeng.xingxia.ui.screens.tools.ToolKind
import com.tianaofeng.xingxia.ui.screens.tools.ToolScreen
import com.tianaofeng.xingxia.ui.theme.LocalPalette

/** App 内所有可直接跳转的页面 */
object Routes {
    // 底栏四个 tab：今日 · 问一问 · 日记 · 我的（原「占卜」「百宝」并进问一问，「记录」换成日记）
    const val TODAY = "today"
    const val ASK = "ask"
    const val JOURNAL = "journal"
    const val ME = "me"

    const val FORTUNE = "fortune"
    const val ALMANAC = "almanac"
    const val HOROSCOPE = "horoscope"
    const val STICK = "stick"
    const val TAROT = "tarot"
    const val ANSWERS = "answers"
    const val BIRTH_CHART = "birthChart"
    const val COMPATIBILITY = "compatibility"
    const val HISTORY = "history"
    const val PROFILES = "profiles"
    const val THEMES = "themes"
    const val SETTINGS = "settings"
    const val HOME_EDITOR = "homeEditor"
    const val MOOD = "mood"
    const val COUNTDOWNS = "countdowns"
    const val CALENDAR = "calendar"
    /** 管理首页「快捷工具」收藏：排序、移除、添加 */
    const val FAVORITES = "favorites"
    /** 择吉日（占卜） */
    const val AUSPICIOUS = "auspicious"
    /** 天象日历（今日头部的月相胶囊进入） */
    const val SKY_CALENDAR = "skyCalendar"
    /** 塔罗手记（占卜） */
    const val TAROT_JOURNAL = "tarotJournal"
    /** 数据备份（我的 / 设置） */
    const val BACKUP = "backup"

    fun tool(kind: ToolKind) = "tool/${kind.id}"
    fun profileEditor(id: String? = null, onboarding: Boolean = false) = "profileEditor?id=${id ?: ""}&onboarding=$onboarding"
    fun comingSoon(title: String) = "comingSoon/${Uri.encode(title)}"

    /** 年度回顾（日记） */
    fun annualReview(year: Int = reviewYear()) = "annualReview/$year"

    /** 默认回顾哪一年：1 月看刚过去的一年，其余月份看今年（与 iOS 的 AppRoute.reviewYear 一致） */
    fun reviewYear(today: DayKey = DayKey.today): Int = if (today.month == 1) today.year - 1 else today.year

    /** 深链接里的年份缺省或不在 1900…2200 时用默认年份 */
    private fun reviewYearOf(segment: String?): Int = segment?.toIntOrNull()?.takeIf { it in 1900..2200 } ?: reviewYear()

    /** 深链接：xingxia://tool/coin、xingxia://fortune、xingxia://review/2026 等（与 iOS 版一致） */
    fun fromUri(uri: Uri?): String? {
        if (uri == null || uri.scheme != "xingxia") return null
        val first = uri.pathSegments.firstOrNull()
        return when (uri.host) {
            "tool" -> ToolKind.of(first)?.let { tool(it) }
            "fortune" -> FORTUNE
            "almanac" -> ALMANAC
            "horoscope" -> HOROSCOPE
            "stick" -> STICK
            "tarot" -> TAROT
            "answers" -> ANSWERS
            "history" -> HISTORY
            "mood" -> MOOD
            "countdowns" -> COUNTDOWNS
            "calendar" -> CALENDAR
            "auspicious" -> AUSPICIOUS
            "sky" -> SKY_CALENDAR
            "tarot-journal" -> TAROT_JOURNAL
            "backup" -> BACKUP
            "review" -> annualReview(reviewYearOf(first))
            else -> null
        }
    }
}

class Navigator(private val controller: NavHostController) {
    fun open(route: String) {
        controller.navigate(route) { launchSingleTop = true }
    }

    fun back() {
        if (!controller.popBackStack()) controller.navigate(Routes.TODAY)
    }

    fun switchTab(route: String) {
        controller.navigate(route) {
            popUpTo(controller.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("Navigator 未提供") }

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.TODAY, "今日", Icons.Rounded.AutoAwesome),
    TabItem(Routes.ASK, "问一问", Icons.Rounded.QuestionAnswer),
    TabItem(Routes.JOURNAL, "日记", Icons.Rounded.Book),
    TabItem(Routes.ME, "我的", Icons.Rounded.AccountCircle),
)

/** 两个 tab 根页之间切换（底栏点按，或返回到今日）：只淡入淡出，不横滑 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    tabs.any { it.route == initialState.destination.route } && tabs.any { it.route == targetState.destination.route }

private const val TAB_FADE_MS = 220

@Composable
fun AppNavigation(pendingLink: MutableState<String?>) {
    val controller = rememberNavController()
    val navigator = remember(controller) { Navigator(controller) }
    val backStack by controller.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // 量出来的悬浮底栏高度：根页面按它在底部留白，决策浮层按它浮在底栏上方
    var tabBarHeight by remember { mutableStateOf(0.dp) }

    LaunchedEffect(pendingLink.value) {
        val link = pendingLink.value ?: return@LaunchedEffect
        pendingLink.value = null
        navigator.open(link)
    }

    // 切换 tab、进出页面时收起决策浮层（收起不算表态）
    LaunchedEffect(backStack?.id) {
        if (Store.lastDecision != null) Store.lastDecision = null
    }

    CompositionLocalProvider(LocalNavigator provides navigator, LocalTabBarHeight provides tabBarHeight) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = controller,
                startDestination = Routes.TODAY,
                modifier = Modifier.fillMaxSize(),
                // tab 之间淡入淡出；进入详情页才横滑（底栏切到「今日」时导航栈会弹回起点，按返回处理，所以 pop 也要分）
                enterTransition = { if (isTabSwitch()) fadeIn(tween(TAB_FADE_MS)) else fadeIn() + slideInHorizontally { it / 6 } },
                exitTransition = { if (isTabSwitch()) fadeOut(tween(TAB_FADE_MS)) else fadeOut() },
                popEnterTransition = { if (isTabSwitch()) fadeIn(tween(TAB_FADE_MS)) else fadeIn() },
                popExitTransition = { if (isTabSwitch()) fadeOut(tween(TAB_FADE_MS)) else fadeOut() + slideOutHorizontally { it / 6 } },
            ) {
                composable(Routes.TODAY) { TodayScreen() }
                composable(Routes.ASK) { AskScreen() }
                composable(Routes.JOURNAL) { JournalScreen() }
                composable(Routes.ME) { MeScreen() }

                composable(Routes.FORTUNE) { FortuneDetailScreen() }
                composable(Routes.ALMANAC) { AlmanacDetailScreen() }
                composable(Routes.HOROSCOPE) { HoroscopeScreen() }
                composable(Routes.STICK) { StickScreen() }
                composable(Routes.TAROT) { TarotScreen() }
                composable(Routes.ANSWERS) { AnswerBookScreen() }
                composable(Routes.BIRTH_CHART) { BirthChartScreen() }
                composable(Routes.COMPATIBILITY) { CompatibilityScreen() }
                composable(Routes.HISTORY) { HistoryScreen() }
                composable(Routes.PROFILES) { ProfileListScreen() }
                composable(Routes.THEMES) { ThemeSettingsScreen() }
                composable(Routes.SETTINGS) { SettingsScreen() }
                composable(Routes.HOME_EDITOR) { HomeEditorScreen() }
                composable(Routes.MOOD) { MoodScreen() }
                composable(Routes.COUNTDOWNS) { CountdownListScreen() }
                composable(Routes.CALENDAR) { FortuneCalendarScreen() }
                composable(Routes.FAVORITES) { FavoriteToolsScreen() }
                composable(Routes.AUSPICIOUS) { AuspiciousDaysScreen() }
                composable(Routes.SKY_CALENDAR) { SkyCalendarScreen() }
                composable(Routes.TAROT_JOURNAL) { TarotJournalScreen() }
                composable(Routes.BACKUP) { BackupScreen() }
                composable("tool/{id}") { entry ->
                    val kind = ToolKind.of(entry.arguments?.getString("id"))
                    if (kind != null) {
                        // 工具页顶栏的收藏星标统一在这里加，不用逐个改工具页
                        CompositionLocalProvider(LocalFavoriteId provides kind.id) { ToolScreen(kind) }
                    } else {
                        ComingSoonScreen("工具")
                    }
                }
                composable("profileEditor?id={id}&onboarding={onboarding}") { entry ->
                    ProfileEditorScreen(
                        profileId = entry.arguments?.getString("id")?.takeIf { it.isNotEmpty() },
                        isOnboarding = entry.arguments?.getString("onboarding") == "true",
                    )
                }
                composable("comingSoon/{title}") { entry ->
                    ComingSoonScreen(entry.arguments?.getString("title") ?: "")
                }
                composable("annualReview/{year}") { entry ->
                    AnnualReviewScreen(entry.arguments?.getString("year")?.toIntOrNull() ?: Routes.reviewYear())
                }
            }

            val showsTabBar = tabs.any { it.route == currentRoute }
            val bottom = Modifier.align(Alignment.BottomCenter)
            // 浮在页面内容上的玻璃不做模糊：模糊源只有星空，会把底下的卡片盖成一块星空
            CompositionLocalProvider(LocalGlassBackdrop provides null) {
                if (showsTabBar) {
                    TabBar(currentRoute, bottom, onHeight = { tabBarHeight = it }) { navigator.switchTab(it) }
                }
                // 刚记下的决定：「就这么办 / 再想想」，底栏可见时浮在底栏上方
                DecisionPrompt(tabBarVisible = showsTabBar, modifier = bottom)
            }
            // 每天第一次打开时的仪式（全屏，自己判断要不要显示）
            DailyRitualHost()
        }
    }
}

/** 悬浮的玻璃底栏；onHeight 报告它占的高度（不含系统导航栏） */
@Composable
private fun TabBar(current: String?, modifier: Modifier = Modifier, onHeight: (Dp) -> Unit, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    Row(
        modifier
            .navigationBarsPadding()
            .onSizeChanged { onHeight(with(density) { it.height.toDp() }) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .glass(CircleShape, tint = palette.backgroundBottom.copy(alpha = if (palette.isDark) 0.55f else 0.35f))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for (tab in tabs) {
            val selected = tab.route == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(if (selected) palette.accent.copy(alpha = 0.16f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable {
                        if (!selected) {
                            Haptics.selection()
                            onSelect(tab.route)
                        }
                    }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(tab.icon, tab.label, Modifier.size(22.dp), tint = if (selected) palette.accent else palette.secondaryText)
                Text(
                    tab.label,
                    style = Fonts.caption2.weight(if (selected) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (selected) palette.accent else palette.secondaryText,
                )
            }
        }
    }
}
