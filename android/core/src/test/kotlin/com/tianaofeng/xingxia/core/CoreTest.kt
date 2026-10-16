package com.tianaofeng.xingxia.core

import com.ibm.icu.util.ChineseCalendar
import com.ibm.icu.util.GregorianCalendar
import com.ibm.icu.util.TimeZone
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LunarTest {
    /** ICU 与官方农历（紫金历/香港天文台）不一致的农历年，这些年份以官方为准，单独断言 */
    private val icuDiffers = setOf(1917, 1922, 1953, 1954, 1955, 1987, 1998, 2012, 2018, 2026, 2027, 2029, 2030, 2070)

    /** 逐日对比 ICU 中国农历，已知差异年份除外（只说明数据表在其余年份与 ICU 一致；iOS 现在也查同一份表） */
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
            if (lunar.lunarYear in icuDiffers || (lunar.month == 12 && lunar.lunarYear + 1 in icuDiffers)) {
                key = key.adding(1)
                continue
            }
            val expectedMonth = chinese.get(ChineseCalendar.MONTH) + 1
            val expectedDay = chinese.get(ChineseCalendar.DAY_OF_MONTH)
            val expectedLeap = chinese.get(ChineseCalendar.IS_LEAP_MONTH) == 1
            assertEquals(Triple(expectedMonth, expectedDay, expectedLeap), Triple(lunar.month, lunar.day, lunar.isLeapMonth), "at $key")
            assertEquals(chinese.get(ChineseCalendar.YEAR), lunar.cycleYear, "cycle year at $key")
            assertEquals(key, LunarDate.solarDate(lunar.lunarYear, lunar.month, lunar.day, lunar.isLeapMonth), "round trip at $key")
            key = key.adding(1)
            checked++
        }
        assertTrue(checked > 65_000)
    }

    @Test
    fun knownDates() {
        val springFestival2024 = LunarDate.of(DayKey(2024, 2, 10))!!
        assertEquals(1, springFestival2024.month)
        assertEquals(1, springFestival2024.day)
        assertEquals("甲辰", springFestival2024.yearGanZhi.name)
        assertEquals("正月初一", springFestival2024.text)
        // 官方农历：2027 年春节为 2 月 6 日，1987 年闰六月（ICU 在这两处有误）
        assertEquals(LunarDate(2027, 1, 1, false), LunarDate.of(DayKey(2027, 2, 6)))
        assertEquals(6, LunarDate.leapMonth(1987))
        assertEquals(LunarDate(2026, 12, 29, false), LunarDate.of(DayKey(2027, 2, 5)))
        assertTrue("除夕" in Festivals.on(DayKey(2027, 2, 5)))
        // 官方农历：2030 年春节为 2 月 3 日（ICU 算成 2 月 2 日）
        assertEquals(LunarDate(2030, 1, 1, false), LunarDate.of(DayKey(2030, 2, 3)))
        assertTrue("除夕" in Festivals.on(DayKey(2030, 2, 2)))
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
        // 第二批扩到 100 支签、78 张塔罗（共享 JSON 由内容代理追加，追加前仍是 60 / 22）
        assertTrue(ContentLibrary.sticks.size in setOf(60, 100), "${ContentLibrary.sticks.size}")
        assertTrue(ContentLibrary.tarot.size in setOf(22, 78), "${ContentLibrary.tarot.size}")
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
        assertEquals(listOf(FortuneInsightKind.STEM, FortuneInsightKind.BRANCH, FortuneInsightKind.MOON), a.insightKinds)
        assertEquals(3, a.funYi.size)
        val anonymous = FortuneEngine.daily(null, day)
        assertTrue(anonymous.insights.isEmpty())
        assertTrue(anonymous.insightKinds.isEmpty())
    }

    @Test
    fun insightKindsMatchInsights() {
        val birth = BirthInfo(DayKey(1995, 3, 2), null, null, "beijing")
        var skippedBranch = false
        for (i in 0 until 60) {
            val fortune = FortuneEngine.daily(birth, DayKey(2026, 1, 1).adding(i))
            assertEquals(fortune.insights.size, fortune.insightKinds.size)
            assertEquals(FortuneInsightKind.STEM, fortune.insightKinds.first())
            // 生肖提示为空的日子，月亮提示仍标为 MOON
            if (FortuneInsightKind.BRANCH !in fortune.insightKinds) {
                skippedBranch = true
                assertEquals(FortuneInsightKind.MOON, fortune.insightKinds.getOrNull(1) ?: FortuneInsightKind.MOON)
            }
        }
        assertTrue(skippedBranch)
    }

    @Test
    fun seasonalItems() {
        val content = ContentLibrary.fortune.copy(
            items = listOf("围巾", "小风扇", "钥匙扣"),
            itemMonths = mapOf("围巾" to listOf(10, 11, 12, 1, 2, 3), "小风扇" to listOf(5, 6, 7, 8, 9)),
        )
        assertEquals(listOf("小风扇", "钥匙扣"), content.itemsFor(7))
        assertEquals(listOf("围巾", "钥匙扣"), content.itemsFor(12))
        assertEquals(listOf("钥匙扣"), content.itemsFor(4))
        // 没有 itemMonths 时全年可选；全被过滤掉时退回全部
        assertEquals(content.items, content.copy(itemMonths = emptyMap()).itemsFor(7))
        assertEquals(listOf("围巾"), content.copy(items = listOf("围巾")).itemsFor(7))

        // 引擎抽到的幸运物一定在当月可选范围内
        val birth = BirthInfo(DayKey(1998, 6, 18), 8, 30, "shanghai")
        val months = ContentLibrary.fortune.itemMonths
        for (i in 0 until 366) {
            val day = DayKey(2026, 1, 1).adding(i)
            for (who in listOf(birth, null)) {
                val item = FortuneEngine.daily(who, day).luckyItem
                val allowed = months[item]
                assertTrue(allowed == null || day.month in allowed, "$item 不应出现在 $day")
            }
        }
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
    fun horoscopeTomorrowEqualsNextToday() {
        var day = DayKey(2026, 10, 10)
        repeat(40) {
            for (sign in ZodiacSign.entries) {
                val tomorrow = HoroscopeEngine.horoscope(sign, HoroscopePeriod.TOMORROW, day)
                val nextToday = HoroscopeEngine.horoscope(sign, HoroscopePeriod.TODAY, day.adding(1))
                assertEquals(nextToday.copy(period = HoroscopePeriod.TOMORROW), tomorrow, "$sign $day")
            }
            day = day.adding(1)
        }
    }

    @Test
    fun compatibility() {
        val c = Compatibility.between(ZodiacSign.ARIES, ZodiacSign.LEO)
        assertEquals(AspectKind.TRINE, c.aspect)
        assertTrue(c.score in 90..95)
        assertEquals(3, c.tips.size)
    }

    @Test
    fun tarotReversedNeverShowsUpright() {
        for (card in ContentLibrary.tarot) {
            assertEquals(card.keywords, card.keywordsFor(false))
            assertEquals(card.love, card.loveFor(false))
            // 逆位只取逆位字段
            assertEquals(card.keywordsReversed?.takeIf { it.isNotEmpty() }, card.keywordsFor(true))
            assertEquals(card.loveReversed?.takeIf { it.isNotBlank() }, card.loveFor(true))
            assertEquals(card.careerReversed?.takeIf { it.isNotBlank() }, card.careerFor(true))
            assertEquals(card.adviceReversed?.takeIf { it.isNotBlank() }, card.adviceFor(true))
        }
        // 没有逆位字段的旧数据也能解码，逆位内容为空而不是正位
        val legacy = Json.decodeFromString(
            TarotCard.serializer(),
            """{"id":0,"name":"愚者","en":"The Fool","roman":"0","symbol":"figure.walk","keywords":["自由","冒险","开始"],"upright":"u","reversed":"r","love":"l","career":"c","advice":"a"}""",
        )
        assertNull(legacy.keywordsFor(true))
        assertNull(legacy.loveFor(true))
        assertNull(legacy.careerFor(true))
        assertNull(legacy.adviceFor(true))
        assertEquals("a", legacy.adviceFor(false))
    }

    @Test
    fun stickAreas() {
        val stick = ContentLibrary.stick(1)
        assertEquals("第一签", stick.numberText)
        assertEquals(6, stick.areas.size)
        assertEquals("第五十九签", ContentLibrary.stick(59).numberText)
        val last = ContentLibrary.sticks.last()
        assertEquals("第${chineseNumber(last.n)}签", last.numberText)
        if (ContentLibrary.sticks.size == 100) assertEquals("第一百签", ContentLibrary.stick(100).numberText)
        // 与 iOS StickLibrary.chineseNumber 相同的写法
        assertEquals(
            listOf("十", "十九", "六十", "一百", "一百零一", "一百零五", "一百一十", "一百一十五", "一百二十", "九百九十九", "1000"),
            listOf(10, 19, 60, 100, 101, 105, 110, 115, 120, 999, 1000).map { chineseNumber(it) },
        )
    }

    @Test
    fun sticksCoverAllNumbers() {
        // 求签按数组长度取号，签号 1…N 连续不重复
        val sticks = ContentLibrary.sticks
        assertEquals((1..sticks.size).toList(), sticks.map { it.n })
        for (stick in sticks) {
            assertEquals(4, stick.poem.size, "第 ${stick.n} 支")
            assertTrue(stick.poem.all { it.length == 7 }, "第 ${stick.n} 支不是七言：${stick.poem}")
            assertTrue(stick.level in listOf("上上", "上吉", "中吉", "中平", "下"), "第 ${stick.n} 支 ${stick.level}")
            assertTrue(stick.title.isNotBlank() && stick.meaning.isNotBlank(), "第 ${stick.n} 支")
            assertEquals(stick, ContentLibrary.stick(stick.n))
        }
        assertEquals(sticks.size, sticks.map { it.title }.toSet().size, "签题重复")
        // 抽签用 1…sticks.size，最后一支也能取到，不会退回第一支
        assertEquals(sticks.size, ContentLibrary.stick(sticks.size).n)
    }

    @Test
    fun tarotFullDeck() {
        val deck = ContentLibrary.tarot
        assertEquals((0 until deck.size).toList(), deck.map { it.id })
        assertEquals(22, ContentLibrary.tarotDeck(false).size)
        assertTrue(ContentLibrary.tarotDeck(false).all { it.isMajor && it.id < 22 })
        assertEquals(deck, ContentLibrary.tarotDeck(true))
        val ranks = listOf("A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "侍从", "骑士", "王后", "国王")
        // 22 张大阿卡纳后按权杖、圣杯、宝剑、星币各 14 张（小阿卡纳追加进 JSON 之后才检查）
        if (deck.size > 22) assertEquals(78, deck.size)
        for ((i, suit) in TarotSuit.entries.withIndex()) {
            if (deck.size <= 22) break
            val cards = deck.subList(22 + i * 14, 22 + (i + 1) * 14)
            assertTrue(cards.all { it.tarotSuit == suit && !it.isMajor }, suit.label)
            assertEquals(ranks, cards.map { it.rankText }, suit.label)
            assertTrue(cards.all { it.name.startsWith(suit.label) }, suit.label)
        }
        for (card in deck) {
            assertTrue(card.keywords.isNotEmpty() && card.upright.isNotBlank() && card.reversed.isNotBlank(), card.name)
            assertTrue(!card.keywordsReversed.isNullOrEmpty() && !card.adviceReversed.isNullOrBlank(), card.name)
        }
        assertEquals(deck.size, deck.map { it.name }.toSet().size)
        assertEquals("XXI", ContentLibrary.tarotCard(21)?.rankText)
        assertEquals(null, ContentLibrary.tarotCard(deck.size))
    }

    /** 小阿卡纳的 suit 字段能解码；大阿卡纳没有 suit */
    @Test
    fun tarotSuitDecodes() {
        val json = Json { ignoreUnknownKeys = true }
        val queen = json.decodeFromString(
            TarotCard.serializer(),
            """{"id":49,"name":"圣杯王后","en":"Queen of Cups","roman":"Queen","symbol":"drop.fill","keywords":["共情"],"upright":"u","reversed":"r","love":"l","career":"c","advice":"a","keywordsReversed":["情绪化"],"loveReversed":"lr","careerReversed":"cr","adviceReversed":"ar","suit":"cups"}""",
        )
        assertEquals(TarotSuit.CUPS, queen.tarotSuit)
        assertFalse(queen.isMajor)
        assertEquals("王后", queen.rankText)
        val ace = json.decodeFromString(
            TarotCard.serializer(),
            """{"id":22,"name":"权杖一","en":"Ace of Wands","roman":"A","symbol":"flame.fill","keywords":["灵感"],"upright":"u","reversed":"r","love":"l","career":"c","advice":"a","suit":"wands"}""",
        )
        assertEquals(TarotSuit.WANDS, ace.tarotSuit)
        assertEquals("A", ace.rankText)
        assertTrue(ContentLibrary.tarot.take(22).all { it.isMajor && it.suit == null })
        assertNull(TarotSuit.of("major"))
        assertEquals(listOf("wands", "cups", "swords", "pentacles"), TarotSuit.entries.map { it.key })
    }
}

class CountdownTest {
    private val today = DayKey(2026, 10, 10)

    @Test
    fun oneOffCountdown() {
        val future = CountdownSpec(CountdownKind.COUNTDOWN, DayKey(2026, 10, 20), repeatsYearly = false).status(today)
        assertEquals(10, future.days)
        assertEquals("还有", future.prefix)
        val past = CountdownSpec(CountdownKind.COUNTDOWN, DayKey(2026, 10, 1), repeatsYearly = false).status(today)
        assertEquals(9, past.days)
        assertTrue(past.isPast)
        val same = CountdownSpec(CountdownKind.COUNTDOWN, today, repeatsYearly = false).status(today)
        assertTrue(same.isToday)
    }

    @Test
    fun yearlyAndLunar() {
        // 元旦每年重复
        val newYear = CountdownSpec(CountdownKind.COUNTDOWN, DayKey(2020, 1, 1), repeatsYearly = true)
        assertEquals(DayKey(2027, 1, 1), newYear.nextOccurrence(today))
        // 农历正月初一：2027 年春节是 2 月 6 日
        val spring = CountdownSpec(CountdownKind.COUNTDOWN, today, repeatsYearly = true, isLunar = true, lunarMonth = 1, lunarDay = 1)
        assertEquals(DayKey(2027, 2, 6), spring.nextOccurrence(today))
        // 2 月 29 日生日在平年按 28 日
        val leap = CountdownSpec(CountdownKind.BIRTHDAY, DayKey(2000, 2, 29), repeatsYearly = true)
        assertEquals(DayKey(2027, 2, 28), leap.nextOccurrence(today))
        assertEquals("2月29日 · 即将 27 岁", leap.status(today).caption)
    }

    @Test
    fun birthdayToday() {
        val status = CountdownSpec(CountdownKind.BIRTHDAY, DayKey(2000, 10, 10), repeatsYearly = true).status(today)
        assertTrue(status.isToday)
        assertEquals("10月10日 · 26 岁生日", status.caption)
        val upcoming = CountdownSpec(CountdownKind.BIRTHDAY, DayKey(2000, 10, 11), repeatsYearly = true).status(today)
        assertEquals("10月11日 · 即将 26 岁", upcoming.caption)
        val noYear = CountdownSpec(CountdownKind.BIRTHDAY, DayKey(2000, 10, 10), repeatsYearly = true, showsAge = false).status(today)
        assertEquals("10月10日", noYear.caption)
    }

    @Test
    fun lunarOneOff() {
        // 不重复的农历倒数日：从创建日起找第一个农历八月十五
        val afterMidAutumn = CountdownSpec(CountdownKind.COUNTDOWN, today, repeatsYearly = false, isLunar = true, lunarMonth = 8, lunarDay = 15)
        assertEquals(DayKey(2027, 9, 15), afterMidAutumn.nextOccurrence(today))
        val beforeMidAutumn = CountdownSpec(CountdownKind.COUNTDOWN, DayKey(2026, 9, 1), repeatsYearly = false, isLunar = true, lunarMonth = 8, lunarDay = 15)
        val status = beforeMidAutumn.status(today)
        assertEquals(DayKey(2026, 9, 25), status.targetDay)
        assertTrue(status.isPast)
        assertEquals(15, status.days)
    }

    @Test
    fun anniversary() {
        val spec = CountdownSpec(CountdownKind.ANNIVERSARY, DayKey(2025, 10, 10), repeatsYearly = true)
        val status = spec.status(today)
        assertEquals(366, status.days)
        assertTrue(status.isToday)
        assertEquals("今天是 1 周年纪念日", status.caption)
    }

    @Test
    fun profileBirthdays() {
        // 公历录入：按公历每年重复
        val solar = ProfileBirthdays.spec(DayKey(1998, 6, 18), enteredAsLunar = false)
        assertEquals(DayKey(2027, 6, 18), solar.nextOccurrence(today))
        assertEquals("6月18日 · 即将 29 岁", solar.status(today).caption)
        // 农历录入：1998-06-18 是农历五月廿四，按农历每年重复
        val lunar = ProfileBirthdays.spec(DayKey(1998, 6, 18), enteredAsLunar = true)
        assertTrue(lunar.isLunar)
        assertEquals(5 to 24, lunar.lunarMonth to lunar.lunarDay)
        assertEquals(LunarDate.solarDate(2027, 5, 24, false), lunar.nextOccurrence(today))
        assertEquals("小明的生日", ProfileBirthdays.title(" 小明 "))
        assertTrue(ProfileBirthdays.shouldSkip("小明", listOf("小明生日")))
        assertTrue(ProfileBirthdays.shouldSkip("  ", emptyList()))
        assertFalse(ProfileBirthdays.shouldSkip("小明", listOf("妈妈的生日")))
    }

    @Test
    fun correlation() {
        assertEquals(1.0, pearson(listOf(1.0 to 2.0, 2.0 to 4.0, 3.0 to 6.0))!!, 1e-9)
        assertNull(pearson(listOf(1.0 to 2.0, 1.0 to 3.0)))
    }
}
