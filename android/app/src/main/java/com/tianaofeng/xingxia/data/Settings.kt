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
    COUNTDOWN("countdown", "倒数日"),
    MOOD("mood", "心情打卡"),
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
    var soundEnabled by Prefs.bool("soundEnabled", true)
    /** 0…100 */
    var soundVolume by Prefs.int("soundVolume", 70)
    var dailyReminderEnabled by Prefs.bool("dailyReminderEnabled", false)
    var reminderHour by Prefs.int("reminderHour", 8)
    var reminderMinute by Prefs.int("reminderMinute", 0)

    /** 新版本加入的模块追加到末尾 */
    var homeCards: List<HomeCard>
        get() {
            val stored = homeCardKeys.value.mapNotNull { HomeCard.of(it) }
            val missing = HomeCard.entries.filter { it !in stored }
            // 新模块按默认顺序插到「每日一句」前面
            return if (HomeCard.QUOTE in stored) {
                stored.takeWhile { it != HomeCard.QUOTE } + missing + stored.dropWhile { it != HomeCard.QUOTE }
            } else {
                stored + missing
            }
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

    /**
     * 答案之书以前固定在首页快捷工具末尾，现在改成普通收藏项（id "answers"，与 iOS 一致）：
     * 升级后第一次启动补进收藏并立即写回，首页看起来和以前一样，之后可以移除。新用户的默认列表同样带上。
     * 在 Application.onCreate 里 Store.init 之后调用。
     */
    fun migrateIfNeeded() {
        var migrated by Prefs.bool("answersFavoriteMigrated", false)
        if (migrated) return
        // 列表有变化时 PrefState 会立即写回 SharedPreferences
        if ("answers" !in favoriteTools) favoriteTools = favoriteTools + "answers"
        migrated = true
    }
}
