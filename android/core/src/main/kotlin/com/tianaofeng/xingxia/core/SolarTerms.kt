package com.tianaofeng.xingxia.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** 二十四节气（按北京时间计算日期） */
object SolarTerms {
    /** 从小寒开始的顺序 */
    val NAMES = listOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至",
    )

    val BLURBS = mapOf(
        "小寒" to "寒气渐深，宜早睡晚起，温补养藏。",
        "大寒" to "一年最冷之时，岁末将尽，静待新春。",
        "立春" to "东风解冻，万物始生，适合立下新的计划。",
        "雨水" to "冰雪消融，雨水渐多，宜调理脾胃。",
        "惊蛰" to "春雷乍动，万物复苏，适合开启新的尝试。",
        "春分" to "昼夜平分，阴阳相半，宜保持平衡。",
        "清明" to "气清景明，宜踏青、宜怀念。",
        "谷雨" to "雨生百谷，春季最后一个节气，宜祛湿。",
        "立夏" to "万物繁茂，夏日开启，宜养心。",
        "小满" to "麦粒渐满而未满，小满即是圆满。",
        "芒种" to "有芒之谷可种，忙碌而有希望。",
        "夏至" to "白昼最长，阳气最盛，宜清补。",
        "小暑" to "温风至，蟋蟀居壁，宜消暑静心。",
        "大暑" to "一年最热之时，宜避暑养阴。",
        "立秋" to "凉风至，暑去秋来，宜收敛神气。",
        "处暑" to "暑气至此而止，秋意渐浓。",
        "白露" to "露凝而白，早晚温差大，注意添衣。",
        "秋分" to "昼夜再次平分，丹桂飘香，宜团圆。",
        "寒露" to "露气寒冷，将凝结也，宜养阴润燥。",
        "霜降" to "气肃而凝，露结为霜，秋季最后一个节气。",
        "立冬" to "水始冰，地始冻，宜进补藏精。",
        "小雪" to "气寒将雪，宜温润养护。",
        "大雪" to "积雪渐厚，围炉煮茶正当时。",
        "冬至" to "白昼最短，一阳初生，宜团聚、宜吃饺子或汤圆。",
    )

    data class Term(val index: Int, val name: String, val moment: Instant, val day: DayKey)

    val BEIJING: ZoneId = ZoneId.of("Asia/Shanghai")
    private val cache = HashMap<Int, List<Term>>()

    fun beijingDay(instant: Instant) = DayKey.of(instant, BEIJING)

    /** 某个公历年里的 24 个节气（小寒 … 冬至） */
    @Synchronized
    fun terms(year: Int): List<Term> = cache.getOrPut(year) {
        val guess = LocalDateTime.of(year - 1, 3, 20, 12, 0).toInstant(ZoneOffset.UTC)
        val baseJd = Astronomy.julianDay(guess)
        (0 until 24).map { i ->
            val offset = (285 + 15 * i).toDouble()
            val target = Astronomy.normalize(offset)
            val jd = Astronomy.timeOfSunLongitude(target, baseJd + offset / 360 * 365.2422)
            val moment = Astronomy.instant(jd)
            Term(i, NAMES[i], moment, beijingDay(moment))
        }
    }

    fun termOn(day: DayKey): Term? = terms(day.year).firstOrNull { it.day == day }

    fun next(after: DayKey): Term = terms(after.year).firstOrNull { it.day > after } ?: terms(after.year + 1)[0]

    fun previous(onOrBefore: DayKey): Term =
        terms(onOrBefore.year).lastOrNull { it.day <= onOrBefore } ?: terms(onOrBefore.year - 1)[23]

    /** 节气月序：0 = 寅月（立春起）… 11 = 丑月 */
    fun monthIndex(at: Instant): Int {
        val lon = Astronomy.sunLongitude(at)
        return (Astronomy.normalize(lon - 315) / 30).toInt() % 12
    }

    /** 某天（北京时间）所属的节气月：交节当天按新月计算。 */
    fun monthIndex(day: DayKey): Int {
        val endOfDay = LocalDateTime.of(day.year, day.month, day.day, 23, 59, 59).atZone(BEIJING).toInstant()
        return monthIndex(endOfDay)
    }
}
