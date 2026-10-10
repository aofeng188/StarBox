package com.tianaofeng.xingxia.core

enum class CountdownKind(val key: String, val label: String) {
    COUNTDOWN("countdown", "倒数日"),
    ANNIVERSARY("anniversary", "纪念日"),
    BIRTHDAY("birthday", "生日");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key } ?: COUNTDOWN
    }
}

data class CountdownStatus(
    /** 主数字 */
    val days: Int,
    /** 主数字前的说明，如「还有」「已经」 */
    val prefix: String,
    /** 主数字后的单位，如「天」 */
    val suffix: String,
    /** 次要说明，如「2027年3月1日 周一」「距 3 周年还有 20 天」 */
    val caption: String,
    val targetDay: DayKey,
    val isToday: Boolean,
) {
    val isPast: Boolean get() = prefix == "已过"
}

/** 倒数日的日期规则（与 iOS 版 CountdownLogic 一致） */
data class CountdownSpec(
    val kind: CountdownKind,
    /** 公历日期（农历倒数日中仅用于生日的出生年份） */
    val origin: DayKey,
    val repeatsYearly: Boolean,
    val isLunar: Boolean = false,
    val lunarMonth: Int = 1,
    val lunarDay: Int = 1,
) {
    fun nextOccurrence(today: DayKey = DayKey.today): DayKey {
        val repeats = repeatsYearly || kind == CountdownKind.BIRTHDAY
        if (!repeats) return origin
        for (year in (today.year - 1)..(today.year + 2)) {
            val candidate: DayKey? = if (isLunar) {
                // 小月没有三十时按廿九算
                LunarDate.solarDate(year, lunarMonth, lunarDay, false)
                    ?: LunarDate.solarDate(year, lunarMonth, lunarDay - 1, false)
            } else {
                var day = origin.day
                // 2 月 29 日在平年按 2 月 28 日算
                if (origin.month == 2 && day == 29 && !isLeapYear(year)) day = 28
                DayKey(year, origin.month, day)
            }
            if (candidate != null && candidate >= today) return candidate
        }
        return origin
    }

    fun status(today: DayKey = DayKey.today): CountdownStatus = when (kind) {
        CountdownKind.COUNTDOWN -> {
            val target = nextOccurrence(today)
            val diff = today.daysTo(target)
            val caption = DateText.full(target) + " " + DateText.weekday(target)
            if (diff >= 0) {
                CountdownStatus(diff, if (diff == 0) "就是" else "还有", if (diff == 0) "今天" else "天", caption, target, diff == 0)
            } else {
                CountdownStatus(-diff, "已过", "天", caption, target, false)
            }
        }
        CountdownKind.ANNIVERSARY -> {
            val passed = origin.daysTo(today)
            if (passed < 0) {
                CountdownStatus(-passed, "还有", "天", DateText.full(origin), origin, false)
            } else {
                var caption = "始于 " + DateText.full(origin)
                val next = nextOccurrence(today)
                if (repeatsYearly) {
                    val years = next.year - origin.year
                    val left = today.daysTo(next)
                    caption = if (left == 0) "今天是 $years 周年纪念日" else "距 $years 周年还有 $left 天"
                }
                CountdownStatus(passed + 1, "第", "天", caption, origin, repeatsYearly && next == today)
            }
        }
        CountdownKind.BIRTHDAY -> {
            val next = nextOccurrence(today)
            val left = today.daysTo(next)
            val age = next.year - origin.year
            val calendarName = if (isLunar) {
                "农历" + LunarDate.MONTH_NAMES[maxOf(0, lunarMonth - 1)] + "月" + LunarDate.DAY_NAMES[maxOf(0, lunarDay - 1)]
            } else {
                DateText.monthDay(origin)
            }
            val caption = if (age > 0) "$calendarName · 即将 $age 岁" else calendarName
            CountdownStatus(left, if (left == 0) "生日" else "还有", if (left == 0) "快乐" else "天", caption, next, left == 0)
        }
    }

    companion object {
        fun isLeapYear(year: Int) = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }
}

/** 皮尔逊相关系数；样本方差为 0 时返回 null */
fun pearson(pairs: List<Pair<Double, Double>>): Double? {
    if (pairs.size < 2) return null
    val n = pairs.size.toDouble()
    val mx = pairs.sumOf { it.first } / n
    val my = pairs.sumOf { it.second } / n
    val cov = pairs.sumOf { (it.first - mx) * (it.second - my) }
    val vx = pairs.sumOf { (it.first - mx) * (it.first - mx) }
    val vy = pairs.sumOf { (it.second - my) * (it.second - my) }
    if (vx <= 0 || vy <= 0) return null
    return cov / Math.sqrt(vx * vy)
}
