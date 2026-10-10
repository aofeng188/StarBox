package com.tianaofeng.xingxia.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** 首页模块 */
enum class HomeCard(val key: String, val label: String) {
    FORTUNE("fortune", "今日运势"),
    ALMANAC("almanac", "宜忌"),
    HOROSCOPE("horoscope", "星座运势"),
    STICK("stick", "今日一签"),
    QUICK_TOOLS("quickTools", "快捷工具"),
    QUOTE("quote", "每日一句");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

/** 用户偏好（震动、摇一摇、首页模块等）。 */
object AppSettings {
    private val homeCardKeys by lazy { Prefs.stringList("homeCards", emptyList()) }
    private val hiddenHomeCardKeys by lazy { Prefs.stringList("hiddenHomeCards", emptyList()) }
    val favoriteToolsState by lazy { Prefs.stringList("favoriteTools", listOf("food", "coin", "wheel", "dice")) }

    var hapticsEnabled by Prefs.bool("hapticsEnabled", true)
    var shakeEnabled by Prefs.bool("shakeEnabled", true)
    var backgroundAnimation by Prefs.bool("backgroundAnimation", true)
    var almanacShowsTraditional by Prefs.bool("almanacShowsTraditional", false)
    var hasOnboarded by Prefs.bool("hasOnboarded", false)
    var lastViewedSign by Prefs.int("lastViewedSign", -1)

    /** 新版本加入的模块追加到末尾 */
    var homeCards: List<HomeCard>
        get() {
            val stored = homeCardKeys.value.mapNotNull { HomeCard.of(it) }
            return stored + HomeCard.entries.filter { it !in stored }
        }
        set(value) { homeCardKeys.value = value.map { it.key } }

    var hiddenHomeCards: Set<HomeCard>
        get() = hiddenHomeCardKeys.value.mapNotNull { HomeCard.of(it) }.toSet()
        set(value) { hiddenHomeCardKeys.value = value.map { it.key } }

    var favoriteTools: List<String>
        get() = favoriteToolsState.value
        set(value) { favoriteToolsState.value = value }

    fun toggleFavorite(toolId: String) {
        favoriteTools = if (toolId in favoriteTools) favoriteTools - toolId else favoriteTools + toolId
    }
}
