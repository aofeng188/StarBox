package com.tianaofeng.xingxia.core

/**
 * 农历日期（覆盖 1900–2100 年）。
 *
 * 数据表按紫金历/香港天文台口径，由 lunar-javascript 的精确天文算法逐日生成。
 * ICU（iOS 的 `Calendar(identifier: .chinese)` 也基于它）在少数年份与官方农历不符，
 * 例如把 1987 年闰六月算成闰七月、把 2027 年春节算成 2 月 7 日（官方为 2 月 6 日），这里以官方为准。
 * iOS 的 Shared/Almanac/Almanac.swift 里 LunarTable 是这份数据表的同一份拷贝，改数据要两边一起改。
 *
 * 每个农历年一个整数，低 13 位依次是各月是否为大月（30 天），
 * 第 13–16 位是闰月月份（0 表示无闰月），第 17 位起是正月初一距当年公历元旦的天数。
 */
data class LunarDate(
    /** 农历年开始（正月初一）所在的公历年 */
    val lunarYear: Int,
    /** 1…12 */
    val month: Int,
    /** 1…30 */
    val day: Int,
    val isLeapMonth: Boolean,
) {
    /** 六十甲子序号 1…60 */
    val cycleYear: Int get() = Math.floorMod(lunarYear - 1984, 60) + 1
    val yearGanZhi: GanZhi get() = GanZhi(cycleYear - 1)
    val monthName: String get() = (if (isLeapMonth) "闰" else "") + MONTH_NAMES[month - 1] + "月"
    val dayName: String get() = DAY_NAMES[day - 1]
    val text: String get() = monthName + dayName
    val fullText: String get() = "${yearGanZhi.name}${yearGanZhi.branch.animal}年 $text"

    companion object {
        const val FIRST_YEAR = 1900
        const val LAST_YEAR = 2100

        val MONTH_NAMES = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
        val DAY_NAMES: List<String> = (1..30).map { d ->
            val digits = listOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十")
            when (d) {
                in 1..10 -> "初" + digits[d]
                in 11..19 -> "十" + digits[d - 10]
                20 -> "二十"
                in 21..29 -> "廿" + digits[d - 20]
                else -> "三十"
            }
        }

        private val TABLE = intArrayOf(
            0x03D16D2, 0x0620752, 0x04C0EA5, 0x038B64A, 0x05C064B, 0x0440A9B, 0x0309556, 0x056056A,
            0x0400B59, 0x02A5752, 0x0500752, 0x03ADB25, 0x0600B25, 0x0480A4B, 0x032B4AB, 0x05802AD,
            0x042056B, 0x02C4B69, 0x0520DA9, 0x03EFD92, 0x0640E92, 0x04C0D25, 0x036BA4D, 0x05C0A56,
            0x04602B6, 0x02E95B5, 0x05606D4, 0x0400EA9, 0x02C5E92, 0x0500E92, 0x03ACD26, 0x05E052B,
            0x0480A57, 0x032B2B6, 0x0580B5A, 0x04406D4, 0x02E6EC9, 0x0520749, 0x03CF693, 0x0620A93,
            0x04C052B, 0x034CA5B, 0x05A0AAD, 0x046056A, 0x0309B55, 0x0560BA4, 0x0400B49, 0x02A5A93,
            0x0500A95, 0x038F52D, 0x05E0536, 0x0480AAD, 0x034B5AA, 0x05805B2, 0x0420DA5, 0x02E7D4A,
            0x0540D4A, 0x03D0A95, 0x0600A97, 0x04C0556, 0x036CAB5, 0x05A0AD5, 0x04606D2, 0x0308EA5,
            0x0560EA5, 0x040064A, 0x0286C97, 0x04E0A9B, 0x03AF55A, 0x05E056A, 0x0480B69, 0x034B752,
            0x05A0B52, 0x0420B25, 0x02C964B, 0x0520A4B, 0x03D14AB, 0x06002AD, 0x04A056D, 0x036CB69,
            0x05C0DA9, 0x0460D92, 0x0309D25, 0x0560D25, 0x0415A4D, 0x0640A56, 0x04E02B6, 0x038C5B5,
            0x05E06D5, 0x0480EA9, 0x034BE92, 0x05A0E92, 0x0440D26, 0x02C6A56, 0x0500A57, 0x03D14D6,
            0x062035A, 0x04A06D5, 0x036B6C9, 0x05C0749, 0x0460693, 0x02E952B, 0x054052B, 0x03E0A5B,
            0x02A555A, 0x04E056A, 0x038FB55, 0x0600BA4, 0x04A0B49, 0x032BA93, 0x0580A95, 0x042052D,
            0x02C8AAD, 0x0500AB5, 0x03D35AA, 0x06205D2, 0x04C0DA5, 0x036DD4A, 0x05C0D4A, 0x0460C95,
            0x030952E, 0x0540556, 0x03E0AB5, 0x02A55B2, 0x05006D2, 0x038CEA5, 0x05E0725, 0x048064B,
            0x032AC97, 0x0560CAB, 0x042055A, 0x02C6AD6, 0x0520B69, 0x03D7752, 0x0620B52, 0x04C0B25,
            0x036DA4B, 0x05A0A4B, 0x04404AB, 0x02EA55B, 0x05405AD, 0x03E0B6A, 0x02A5B52, 0x0500D92,
            0x03AFD25, 0x05E0D25, 0x0480A55, 0x032B4AD, 0x05804B6, 0x04005B5, 0x02C6DAA, 0x0520EC9,
            0x03F1E92, 0x0620E92, 0x04C0D26, 0x036CA56, 0x05A0A57, 0x04404D6, 0x02E86D5, 0x0540755,
            0x0400749, 0x0286E93, 0x04E0693, 0x038F52B, 0x05E052B, 0x0460A5B, 0x032B55A, 0x058056A,
            0x0420B65, 0x02C974A, 0x0520B4A, 0x03D1A95, 0x0620A95, 0x04A052D, 0x034CAAD, 0x05A0AB5,
            0x04605AA, 0x02E8BA5, 0x0540DA5, 0x0400D4A, 0x02A7C95, 0x04E0C96, 0x038F94E, 0x05E0556,
            0x0480AB5, 0x032B5B2, 0x05806D2, 0x0420EA5, 0x02E8E4A, 0x050068B, 0x03B0C97, 0x06004AB,
            0x04A055B, 0x034CAD6, 0x05A0B6A, 0x0460752, 0x0309725, 0x0540B45, 0x03E0A8B, 0x028549B,
            0x04E04AB,
        )

        private fun info(year: Int) = TABLE[year - FIRST_YEAR]

        private fun newYearJdn(year: Int) = DayKey.julianDayNumber(year, 1, 1) + (info(year) ushr 17)

        /** 闰月月份，没有闰月返回 0 */
        fun leapMonth(year: Int): Int = if (year in FIRST_YEAR..LAST_YEAR) (info(year) ushr 13) and 0xF else 0

        /** 某个农历年里按顺序排列的月份：(月份, 是否闰月, 天数) */
        private fun months(year: Int): List<Triple<Int, Boolean, Int>> {
            val bits = info(year)
            val leap = (bits ushr 13) and 0xF
            val result = mutableListOf<Triple<Int, Boolean, Int>>()
            var i = 0
            for (m in 1..12) {
                result.add(Triple(m, false, if (bits and (1 shl i) != 0) 30 else 29)); i++
                if (m == leap) {
                    result.add(Triple(m, true, if (bits and (1 shl i) != 0) 30 else 29)); i++
                }
            }
            return result
        }

        /** 公历日期 → 农历（按北京时间的日期）。超出 1900-01-31…2101 的范围时返回 null。 */
        fun of(key: DayKey): LunarDate? {
            val jdn = key.julianDayNumber
            var year = minOf(key.year, LAST_YEAR)
            if (year < FIRST_YEAR) return null
            if (jdn < newYearJdn(year)) year -= 1
            if (year < FIRST_YEAR) return null
            var offset = jdn - newYearJdn(year)
            for ((month, leap, length) in months(year)) {
                if (offset < length) return LunarDate(year, month, offset + 1, leap)
                offset -= length
            }
            return null
        }

        /** 由农历年月日换算公历（lunarYear 指农历年开始所在的公历年）；日期不存在时返回 null。 */
        fun solarDate(lunarYear: Int, month: Int, day: Int, isLeap: Boolean): DayKey? {
            if (lunarYear !in FIRST_YEAR..LAST_YEAR || day < 1) return null
            var offset = 0
            for ((m, leap, length) in months(lunarYear)) {
                if (m == month && leap == isLeap) {
                    if (day > length) return null
                    return DayKey.fromJulianDayNumber(newYearJdn(lunarYear) + offset + day - 1)
                }
                offset += length
            }
            return null
        }
    }
}

/** 与 iOS 版一致：取不到时退回到正月初一，保证界面不崩。 */
fun DayKey.lunar(): LunarDate = LunarDate.of(this) ?: LunarDate(year, 1, 1, false)
