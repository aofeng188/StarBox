package com.tianaofeng.xingxia.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tianaofeng.xingxia.core.BirthInfo
import com.tianaofeng.xingxia.core.DateText
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.Festivals
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.SeededGenerator
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.data.Prefs
import java.time.LocalDateTime

enum class AppearanceMode(val key: String, val label: String) {
    SYSTEM("system", "跟随系统"), LIGHT("light", "浅色"), DARK("dark", "深色"),
}

enum class ThemeRuleMode(val key: String, val label: String, val detail: String) {
    FIXED("fixed", "固定主题", "始终使用你选中的主题。"),
    DAILY_RANDOM("dailyRandom", "每天随机", "每天零点从随机池里抽一套，当天保持不变。"),
    LAUNCH_RANDOM("launchRandom", "每次打开随机", "每次打开 App 都换一套，惊喜感拉满。"),
    TIME_OF_DAY("timeOfDay", "按时段切换", "清晨、白天、傍晚、深夜各用一套主题。"),
    WEEKDAY("weekday", "按星期切换", "周一到周日，每天一套专属主题。"),
    LUCKY_COLOR("luckyColor", "跟随今日幸运色", "根据今日运势的幸运五行自动配色。"),
    ZODIAC("zodiac", "跟随星座元素", "根据当前档案的星座元素（火土风水）配色。"),
}

enum class TimeSlot(val label: String, val range: String) {
    MORNING("清晨", "5:00–11:00"), DAYTIME("白天", "11:00–17:00"), EVENING("傍晚", "17:00–21:00"), NIGHT("深夜", "21:00–5:00");

    companion object {
        fun of(hour: Int) = when (hour) {
            in 5..10 -> MORNING
            in 11..16 -> DAYTIME
            in 17..20 -> EVENING
            else -> NIGHT
        }
    }
}

/** 管理主题选择和自动切换规则（与 iOS 版规则一致）。 */
object ThemeManager {
    private val appearanceKey by lazy { Prefs.string("appearance", AppearanceMode.SYSTEM.key) }
    private val modeKey by lazy { Prefs.string("themeMode", ThemeRuleMode.FIXED.key) }

    var appearance: AppearanceMode
        get() = AppearanceMode.entries.firstOrNull { it.key == appearanceKey.value } ?: AppearanceMode.SYSTEM
        set(value) { appearanceKey.value = value.key }

    var mode: ThemeRuleMode
        get() = ThemeRuleMode.entries.firstOrNull { it.key == modeKey.value } ?: ThemeRuleMode.FIXED
        set(value) { modeKey.value = value.key }

    var fixedThemeId by Prefs.string("fixedThemeID", "nebula")
    var randomPool by Prefs.stringList("randomPool", AppTheme.ALL.map { it.id })
    var timeSlotThemes by Prefs.stringList("timeSlotThemes", listOf("sakura", "aurora", "sunset", "nebula"))
    var weekdayThemes by Prefs.stringList("weekdayThemes", listOf("solarGold", "nebula", "aurora", "sunset", "abyss", "sakura", "redGiant"))
    var lowPowerOverride by Prefs.bool("lowPowerOverride", true)
    var festivalEggs by Prefs.bool("festivalEggs", true)

    private var launchRandomId by mutableStateOf(AppTheme.ALL.random().id)

    private val pool: List<String> get() = randomPool.ifEmpty { AppTheme.ALL.map { it.id } }

    fun shuffleNow(currentId: String) {
        launchRandomId = pool.filter { it != currentId }.randomOrNull() ?: launchRandomId
    }

    /** 计算当前主题以及它的来由 */
    fun resolve(now: LocalDateTime, birth: BirthInfo?, isPowerSave: Boolean): Pair<AppTheme, String> {
        if (lowPowerOverride && isPowerSave) {
            return AppTheme.EVENT_HORIZON to "省电模式已开启，切换为省电的「黑洞」主题"
        }
        val today = DayKey.of(now.toLocalDate())
        if (festivalEggs) festivalTheme(today)?.let { return it }
        return when (mode) {
            ThemeRuleMode.FIXED -> AppTheme.find(fixedThemeId) to "固定使用"
            ThemeRuleMode.DAILY_RANDOM -> {
                val rng = SeededGenerator("daily-theme", today.rawValue)
                AppTheme.find(rng.pick(pool)) to "今日随机抽中"
            }
            ThemeRuleMode.LAUNCH_RANDOM -> AppTheme.find(launchRandomId) to "本次打开随机抽中"
            ThemeRuleMode.TIME_OF_DAY -> {
                val slot = TimeSlot.of(now.hour)
                AppTheme.find(timeSlotThemes.getOrNull(slot.ordinal)) to "现在是${slot.label}"
            }
            ThemeRuleMode.WEEKDAY -> {
                // weekdayThemes 以周一开始
                val index = (today.weekday + 6) % 7
                AppTheme.find(weekdayThemes.getOrNull(index)) to "今天是${DateText.weekday(today)}"
            }
            ThemeRuleMode.LUCKY_COLOR -> {
                val fortune = FortuneEngine.daily(birth, today)
                AppTheme.forElement(fortune.favorableElement) to
                    "今日幸运五行属${fortune.favorableElement.label}，幸运色${fortune.luckyColor.name}"
            }
            ThemeRuleMode.ZODIAC -> {
                if (birth == null) {
                    AppTheme.find(fixedThemeId) to "还没有设置生日，暂用固定主题"
                } else {
                    val sign = ZodiacSign.sunSign(birth)
                    AppTheme.forZodiac(sign.element) to "${sign.label}属于${sign.element.label}星座"
                }
            }
        }
    }

    private fun festivalTheme(day: DayKey): Pair<AppTheme, String>? {
        val festivals = Festivals.on(day)
        val table = listOf(
            "春节" to AppTheme.RED_GIANT, "除夕" to AppTheme.RED_GIANT, "元宵节" to AppTheme.RED_GIANT,
            "中秋节" to AppTheme.SOLAR_GOLD, "七夕" to AppTheme.SAKURA, "情人节" to AppTheme.SAKURA,
            "圣诞节" to AppTheme.AURORA, "平安夜" to AppTheme.AURORA, "万圣夜" to AppTheme.EVENT_HORIZON,
        )
        for ((name, theme) in table) if (name in festivals) return theme to "节日彩蛋：${name}快乐"
        return null
    }
}
