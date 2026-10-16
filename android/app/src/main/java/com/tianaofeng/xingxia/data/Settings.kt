package com.tianaofeng.xingxia.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** 首页模块 */
enum class HomeCard(val key: String, val label: String) {
    FORTUNE("fortune", "今日运势"),
    FAMILY("family", "家人今日"),
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
    /** 塔罗用完整 78 张（关掉只用 22 张大阿卡纳） */
    var tarotFullDeck by Prefs.bool("tarotFullDeck", true)
    /** 满月、新月、流星雨极大前一天晚上提醒 */
    var skyReminders by Prefs.bool("skyReminders", false)
    /** 上次导出备份的时间（毫秒），0 表示没导出过 */
    var backupLastExportAt by Prefs.long("backupLastExportAt", 0L)

    private val profileBirthdaysState by lazy { Prefs.bool("profileBirthdaysInCountdowns", true) }

    /** 档案里的生日自动出现在倒数日列表、首页和小组件里（与 iOS 同名同义） */
    var profileBirthdaysInCountdowns: Boolean
        get() = profileBirthdaysState.value
        set(value) {
            if (value == profileBirthdaysState.value) return
            profileBirthdaysState.value = value
            // 倒数日小组件跟着变
            Store.onDataChanged()
        }

    /** 新版本加入的模块放到默认顺序里它前一个模块的后面（如「家人今日」跟在「今日运势」后面），找不到就追加到末尾（与 iOS 一致） */
    var homeCards: List<HomeCard>
        get() {
            val cards = homeCardKeys.value.mapNotNull { HomeCard.of(it) }.distinct().toMutableList()
            HomeCard.entries.forEachIndexed { index, card ->
                if (card in cards) return@forEachIndexed
                val anchor = if (index == 0) -1 else cards.indexOf(HomeCard.entries[index - 1])
                when {
                    index == 0 -> cards.add(0, card)
                    anchor >= 0 -> cards.add(anchor + 1, card)
                    else -> cards.add(card)
                }
            }
            return cards
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
