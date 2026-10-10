package com.tianaofeng.xingxia.core

import com.ibm.icu.util.ChineseCalendar
import com.ibm.icu.util.GregorianCalendar
import com.ibm.icu.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LunarTest {
    /** 逐日对比 ICU 中国农历（iOS 的 Calendar(.chinese) 也基于 ICU） */
    @Test
    fun matchesIcuForEveryDay() {
        val zone = TimeZone.getTimeZone("Asia/Shanghai")
        val chinese = ChineseCalendar(zone)
        val gregorian = GregorianCalendar(zone)
        var key = DayKey(1900, 1, 31)
        var checked = 0
        while (key <= DayKey(2100, 12, 31)) {
            gregorian.clear()
            gregorian.set(key.year, key.month - 1, key.day, 12, 0, 0)
            chinese.time = gregorian.time
            val lunar = assertNotNull(LunarDate.of(key), "missing $key")
            val expectedMonth = chinese.get(ChineseCalendar.MONTH) + 1
            val expectedDay = chinese.get(ChineseCalendar.DAY_OF_MONTH)
            val expectedLeap = chinese.get(ChineseCalendar.IS_LEAP_MONTH) == 1
            assertEquals(Triple(expectedMonth, expectedDay, expectedLeap), Triple(lunar.month, lunar.day, lunar.isLeapMonth), "at $key")
            assertEquals(chinese.get(ChineseCalendar.YEAR), lunar.cycleYear, "cycle year at $key")
            assertEquals(key, LunarDate.solarDate(lunar.lunarYear, lunar.month, lunar.day, lunar.isLeapMonth), "round trip at $key")
            key = key.adding(1)
            checked++
        }
        assertTrue(checked > 73_000)
    }

    @Test
    fun knownDates() {
        val springFestival2024 = LunarDate.of(DayKey(2024, 2, 10))!!
        assertEquals(1, springFestival2024.month)
        assertEquals(1, springFestival2024.day)
        assertEquals("甲辰", springFestival2024.yearGanZhi.name)
        assertEquals("正月初一", springFestival2024.text)
        // 2023 年有闰二月
        assertEquals(2, LunarDate.leapMonth(2023))
        assertEquals(DayKey(2023, 3, 22), LunarDate.solarDate(2023, 2, 1, true))
        // 不存在的日期
        assertNull(LunarDate.solarDate(2024, 1, 31, false))
        assertNull(LunarDate.solarDate(2024, 3, 1, true))
        val smallMonths = (1..12).count { LunarDate.solarDate(2024, it, 30, false) == null }
        assertTrue(smallMonths in 4..8)
    }
}

class CalendarTest {
    @Test
    fun ganzhiOfDays() {
        assertEquals("甲子", GanZhi.ofDay(DayKey(1949, 10, 1)).name)
        assertEquals("戊午", GanZhi.ofDay(DayKey(2000, 1, 1)).name)
        assertEquals("甲辰", GanZhi.ofYear(2024).name)
    }

    @Test
    fun solarTerms() {
        val terms = SolarTerms.terms(2024)
        assertEquals(DayKey(2024, 2, 4), terms.first { it.name == "立春" }.day)
        assertEquals(DayKey(2024, 12, 21), terms.first { it.name == "冬至" }.day)
        assertEquals(DayKey(2024, 4, 4), terms.first { it.name == "清明" }.day)
    }

    @Test
    fun almanacAndFestivals() {
        val day = AlmanacDay(DayKey(2024, 2, 10))
        assertTrue("春节" in day.festivals)
        assertEquals("甲辰", day.year.name)
        assertTrue("除夕" in Festivals.on(DayKey(2024, 2, 9)))
        assertTrue("母亲节" in Festivals.on(DayKey(2024, 5, 12)))
    }

    @Test
    fun weekdayAndJdnRoundTrip() {
        assertEquals(4, DayKey(2026, 10, 1).weekday) // 2026-10-01 是周四
        val key = DayKey(2026, 10, 10)
        assertEquals(key, DayKey.fromJulianDayNumber(key.julianDayNumber))
    }

    @Test
    fun baziWithTime() {
        // 2000-01-01 12:00 北京：己卯年 丙子月 戊午日 戊午时
        val bazi = BaZi(BirthInfo(DayKey(2000, 1, 1), 12, 0, "beijing"))
        assertEquals("己卯", bazi.year.name)
        assertEquals("丙子", bazi.month.name)
        assertEquals("戊午", bazi.day.name)
        assertEquals("戊午", bazi.hour?.name)
    }

    @Test
    fun zodiac() {
        assertEquals(ZodiacSign.CAPRICORN, ZodiacSign.sunSign(BirthInfo(DayKey(2000, 1, 1))))
        assertEquals(ZodiacSign.LEO, ZodiacSign.sunSign(BirthInfo(DayKey(1998, 8, 1))))
        assertEquals("♈︎", ZodiacSign.ARIES.glyph)
    }
}

class RandomTest {
    @Test
    fun fnvVector() {
        assertEquals(0xaf63dc4c8601ec8cuL, StableHash.fnv1a("a"))
    }

    @Test
    fun splitMixMatchesReference() {
        // 参考实现：SplitMix64，种子 1234567
        val rng = SeededGenerator(1234567uL xor 0x9E3779B97F4A7C15uL)
        assertEquals(6457827717110365317uL, rng.next())
        assertEquals(3203168211198807973uL, rng.next())
    }

    @Test
    fun weightedIndexSkipsZero() {
        val rng = SeededGenerator("w", 1)
        repeat(200) { assertTrue(rng.weightedIndex(listOf(0.0, 1.0, 0.0, 2.0)) in listOf(1, 3)) }
        assertNull(rng.weightedIndex(listOf(0.0, -1.0)))
    }
}

class EngineTest {
    @Test
    fun contentLoads() {
        assertEquals(60, ContentLibrary.sticks.size)
        assertEquals(22, ContentLibrary.tarot.size)
        assertTrue(ContentLibrary.answers.size > 100)
        assertTrue(ContentLibrary.foods.size > 150)
        assertEquals(5, ContentLibrary.fortune.levels.size)
    }

    @Test
    fun fortuneIsDeterministic() {
        val birth = BirthInfo(DayKey(1998, 6, 18), 8, 30, "shanghai")
        val day = DayKey(2026, 10, 10)
        val a = FortuneEngine.daily(birth, day)
        val b = FortuneEngine.daily(birth, day)
        assertEquals(a, b)
        assertTrue(a.score in 35..99)
        assertEquals(3, a.insights.size)
        assertEquals(3, a.funYi.size)
        val anonymous = FortuneEngine.daily(null, day)
        assertTrue(anonymous.insights.isEmpty())
    }

    @Test
    fun fortuneScoresSpread() {
        val scores = (0 until 365).map { FortuneEngine.daily(null, DayKey(2026, 1, 1).adding(it)).score }
        assertTrue(scores.min() < 60 && scores.max() > 80)
    }

    @Test
    fun horoscopeAllPeriods() {
        for (sign in ZodiacSign.entries) for (period in HoroscopePeriod.entries) {
            val h = HoroscopeEngine.horoscope(sign, period, DayKey(2026, 10, 10))
            assertTrue(h.score in 38..98)
            assertTrue(h.matchSign in sign.trines + sign.sextiles)
        }
        assertEquals("10.5–10.11", HoroscopeEngine.horoscope(ZodiacSign.LEO, HoroscopePeriod.WEEK, DayKey(2026, 10, 10)).rangeText)
    }

    @Test
    fun compatibility() {
        val c = Compatibility.between(ZodiacSign.ARIES, ZodiacSign.LEO)
        assertEquals(AspectKind.TRINE, c.aspect)
        assertTrue(c.score in 90..95)
        assertEquals(3, c.tips.size)
    }

    @Test
    fun stickAreas() {
        val stick = ContentLibrary.stick(1)
        assertEquals("第一签", stick.numberText)
        assertEquals(6, stick.areas.size)
        assertEquals("第五十九签", ContentLibrary.stick(59).numberText)
    }
}
