package com.tianaofeng.xingxia.ui.nav

import android.net.Uri
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.Haptics
import com.tianaofeng.xingxia.ui.components.LocalFavoriteId
import com.tianaofeng.xingxia.ui.components.glass
import com.tianaofeng.xingxia.ui.components.weight
import com.tianaofeng.xingxia.ui.screens.divination.AlmanacDetailScreen
import com.tianaofeng.xingxia.ui.screens.divination.AnswerBookScreen
import com.tianaofeng.xingxia.ui.screens.divination.AuspiciousDaysScreen
import com.tianaofeng.xingxia.ui.screens.divination.DivinationHubScreen
import com.tianaofeng.xingxia.ui.screens.divination.FortuneDetailScreen
import com.tianaofeng.xingxia.ui.screens.divination.HoroscopeScreen
import com.tianaofeng.xingxia.ui.screens.divination.StickScreen
import com.tianaofeng.xingxia.ui.screens.divination.TarotJournalScreen
import com.tianaofeng.xingxia.ui.screens.divination.TarotScreen
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
import com.tianaofeng.xingxia.ui.screens.records.RecordsHubScreen
import com.tianaofeng.xingxia.ui.screens.today.HomeEditorScreen
import com.tianaofeng.xingxia.ui.screens.today.SkyCalendarScreen
import com.tianaofeng.xingxia.ui.screens.today.TodayScreen
import com.tianaofeng.xingxia.ui.screens.tools.FavoriteToolsScreen
import com.tianaofeng.xingxia.ui.screens.tools.ToolKind
import com.tianaofeng.xingxia.ui.screens.tools.ToolScreen
import com.tianaofeng.xingxia.ui.screens.tools.ToolsHubScreen
import com.tianaofeng.xingxia.ui.theme.LocalPalette

/** App 内所有可直接跳转的页面 */
object Routes {
    const val TODAY = "today"
    const val DIVINATION = "divination"
    const val TOOLS = "tools"
    const val RECORDS = "records"
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

    /** 深链接：xingxia://tool/coin、xingxia://fortune 等（与 iOS 版一致） */
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
    TabItem(Routes.DIVINATION, "占卜", Icons.Rounded.NightsStay),
    TabItem(Routes.TOOLS, "百宝", Icons.Rounded.GridView),
    TabItem(Routes.RECORDS, "记录", Icons.Rounded.CalendarMonth),
    TabItem(Routes.ME, "我的", Icons.Rounded.AccountCircle),
)

@Composable
fun AppNavigation(pendingLink: MutableState<String?>) {
    val controller = rememberNavController()
    val navigator = remember(controller) { Navigator(controller) }
    val backStack by controller.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    LaunchedEffect(pendingLink.value) {
        val link = pendingLink.value ?: return@LaunchedEffect
        pendingLink.value = null
        navigator.open(link)
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = controller,
                startDestination = Routes.TODAY,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { fadeIn() + slideInHorizontally { it / 6 } },
                exitTransition = { fadeOut() },
                popEnterTransition = { fadeIn() },
                popExitTransition = { fadeOut() + slideOutHorizontally { it / 6 } },
            ) {
                composable(Routes.TODAY) { TodayScreen() }
                composable(Routes.DIVINATION) { DivinationHubScreen() }
                composable(Routes.TOOLS) { ToolsHubScreen() }
                composable(Routes.RECORDS) { RecordsHubScreen() }
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
            }

            if (tabs.any { it.route == currentRoute }) {
                TabBar(currentRoute, Modifier.align(Alignment.BottomCenter)) { navigator.switchTab(it) }
            }
        }
    }
}

/** 悬浮的玻璃底栏 */
@Composable
private fun TabBar(current: String?, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier
            .navigationBarsPadding()
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
