package com.tianaofeng.xingxia.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SkyTest {
    private fun days(vararg md: Pair<Int, Int>, year: Int = 2026) = md.map { DayKey(year, it.first, it.second) }

    /**
     * 2026 年的朔望（北京时间日期），与 PyEphem 星历逐一核对一致。
     * 本算法的朔望时刻与星历相差不超过 6 分钟（2025–2036 年逐次比较），这期间没有因此落到另一天的。
     */
    @Test
    fun newAndFullMoons2026() {
        val newMoons = SkyEvents.newMoons(DayKey(2026, 1, 1), DayKey(2026, 12, 31)).map { SolarTerms.beijingDay(it) }
        assertEquals(
            days(1 to 19, 2 to 17, 3 to 19, 4 to 17, 5 to 17, 6 to 15, 7 to 14, 8 to 13, 9 to 11, 10 to 10, 11 to 9, 12 to 9),
            newMoons,
        )
        val fullMoons = SkyEvents.fullMoons(DayKey(2026, 1, 1), DayKey(2026, 12, 31)).map { SolarTerms.beijingDay(it) }
        assertEquals(
            days(1 to 3, 2 to 2, 3 to 3, 4 to 2, 5 to 2, 5 to 31, 6 to 30, 7 to 29, 8 to 28, 9 to 27, 10 to 26, 11 to 24, 12 to 24),
            fullMoons,
        )
    }

    @Test
    fun nextFullAndNewMoon() {
        assertEquals(DayKey(2026, 10, 26), SkyEvents.nextFullMoon(DayKey(2026, 10, 16)))
        assertEquals(DayKey(2026, 11, 9), SkyEvents.nextNewMoon(DayKey(2026, 10, 16)))
        // 不含当天：满月当天往后找的是下一次
        assertEquals(DayKey(2026, 11, 24), SkyEvents.nextFullMoon(DayKey(2026, 10, 26)))
        assertEquals(DayKey(2026, 10, 26), SkyEvents.nextFullMoon(DayKey(2026, 10, 25)))
        // 2026-10-10 23:50 合朔，紧贴零点也落在北京时间当天
        assertEquals(DayKey(2026, 10, 10), SkyEvents.nextNewMoon(DayKey(2026, 10, 9)))
    }

    @Test
    fun moonInfo() {
        val full = MoonInfo.on(DayKey(2026, 10, 26))
        assertEquals(MoonPhaseName.FULL, full.name)
        assertTrue(full.illumination > 0.97)
        val new = MoonInfo.on(DayKey(2026, 11, 9))
        assertEquals(MoonPhaseName.NEW, new.name)
        assertTrue(new.illumination < 0.03)
        // 提案里的示例：10 月 15 日是娥眉月，亮面两成多
        val crescent = MoonInfo.on(DayKey(2026, 10, 15))
        assertEquals(MoonPhaseName.WAXING_CRESCENT, crescent.name)
        assertTrue(crescent.isWaxing)
        assertTrue(crescent.percent in 18..30, "${crescent.percent}")
        assertEquals((1 - kotlin.math.cos(2 * Math.PI * crescent.phase)) / 2, crescent.illumination, 1e-12)
    }

    @Test
    fun phaseNames() {
        assertEquals(MoonPhaseName.NEW, MoonPhaseName.of(0.0))
        assertEquals(MoonPhaseName.NEW, MoonPhaseName.of(0.97))
        assertEquals(MoonPhaseName.NEW, MoonPhaseName.of(1.0))
        assertEquals(MoonPhaseName.WAXING_CRESCENT, MoonPhaseName.of(0.0625))
        assertEquals(MoonPhaseName.FIRST_QUARTER, MoonPhaseName.of(0.25))
        assertEquals(MoonPhaseName.WAXING_GIBBOUS, MoonPhaseName.of(0.375))
        assertEquals(MoonPhaseName.FULL, MoonPhaseName.of(0.5))
        assertEquals(MoonPhaseName.WANING_GIBBOUS, MoonPhaseName.of(0.625))
        assertEquals(MoonPhaseName.LAST_QUARTER, MoonPhaseName.of(0.75))
        assertEquals(MoonPhaseName.WANING_CRESCENT, MoonPhaseName.of(0.875))
        assertEquals(
            listOf("新月", "娥眉月", "上弦月", "盈凸月", "满月", "亏凸月", "下弦月", "残月"),
            MoonPhaseName.entries.map { it.label },
        )
    }

    @Test
    fun upcomingEvents() {
        val events = SkyEvents.upcoming(DayKey(2026, 10, 16), 60)
        assertEquals(events.sortedBy { it.day }, events)
        assertTrue(events.all { it.day >= DayKey(2026, 10, 16) && it.day <= DayKey(2026, 12, 14) })
        fun has(kind: SkyEvent.Kind, day: DayKey, title: String) =
            assertTrue(events.any { it.kind == kind && it.day == day && it.title == title }, "$title $day")
        has(SkyEvent.Kind.FULL_MOON, DayKey(2026, 10, 26), "满月")
        has(SkyEvent.Kind.FULL_MOON, DayKey(2026, 11, 24), "满月")
        has(SkyEvent.Kind.NEW_MOON, DayKey(2026, 11, 9), "新月")
        has(SkyEvent.Kind.NEW_MOON, DayKey(2026, 12, 9), "新月")
        has(SkyEvent.Kind.SOLAR_TERM, DayKey(2026, 10, 23), "霜降")
        has(SkyEvent.Kind.SOLAR_TERM, DayKey(2026, 11, 7), "立冬")
        has(SkyEvent.Kind.SOLAR_TERM, DayKey(2026, 12, 7), "大雪")
        // 流星雨极大按太阳黄经推算（IMO 2026：猎户座 10 月 21 日 18 时 UT，即北京时间 22 日凌晨）
        has(SkyEvent.Kind.METEOR_SHOWER, DayKey(2026, 10, 22), "猎户座流星雨")
        has(SkyEvent.Kind.METEOR_SHOWER, DayKey(2026, 11, 18), "狮子座流星雨")
        // 最后一天（起始日 + 59）也包含在内，起始日 + 60 不含
        has(SkyEvent.Kind.METEOR_SHOWER, DayKey(2026, 12, 14), "双子座流星雨")
        assertTrue(SkyEvents.upcoming(DayKey(2026, 10, 17), 58).none { it.title == "双子座流星雨" })
        assertEquals(events.size, events.map { it.id }.toSet().size)
        // 文案与 iOS 相同（星历满月时刻为 12:11，本算法 12:15）
        assertEquals(
            "北京时间 12:15 月最圆（农历九月十七）。月亮整夜都在天上，适合赏月、拍月亮；月光明亮，暗星不容易看到。",
            events.first { it.kind == SkyEvent.Kind.FULL_MOON }.detail,
        )
        assertEquals(
            "北京时间 17:40 交节，太阳到达黄经 210°。秋季夜晚，可以在头顶附近找飞马座的四边形。",
            events.first { it.title == "霜降" }.detail,
        )
        assertTrue(events.first { it.title == "双子座流星雨" }.detail.startsWith("极大约在北京时间12月14日 21 时前后，理想条件下每小时约 150 颗。"))
        // 每年 7 场流星雨都在
        for (year in 2026..2035) {
            assertEquals(7, SkyEvents.upcoming(DayKey(year, 1, 1), if (year % 4 == 0) 366 else 365).count { it.kind == SkyEvent.Kind.METEOR_SHOWER }, "$year")
        }
        assertTrue(events.all { it.detail.isNotEmpty() })
        assertTrue(SkyEvents.upcoming(DayKey(2026, 10, 16), 0).isEmpty())
        // 跨年
        val newYear = SkyEvents.upcoming(DayKey(2026, 12, 20), 30)
        assertTrue(newYear.any { it.title == "象限仪座流星雨" && it.day == DayKey(2027, 1, 4) })
        assertTrue(newYear.any { it.title == "冬至" && it.day == DayKey(2026, 12, 22) })
        assertTrue(newYear.any { it.title == "小寒" && it.day.year == 2027 })
    }

    /** 日月食表与朔望一致：日食在新月当天，月食在满月当天 */
    @Test
    fun eclipsesFallOnSyzygies() {
        val news = SkyEvents.newMoons(DayKey(2026, 1, 1), DayKey(2035, 12, 31)).map { SolarTerms.beijingDay(it) }.toSet()
        val fulls = SkyEvents.fullMoons(DayKey(2026, 1, 1), DayKey(2035, 12, 31)).map { SolarTerms.beijingDay(it) }.toSet()
        assertEquals(36, SkyEvents.ECLIPSES.size)
        for (eclipse in SkyEvents.ECLIPSES) {
            val day = assertNotNull(DayKey.fromRaw(eclipse.day))
            if (eclipse.title.startsWith("月")) {
                assertTrue(day in fulls, "${eclipse.title} $day")
            } else {
                assertTrue(eclipse.title in listOf("日全食", "日环食", "日偏食", "全环食"), eclipse.title)
                assertTrue(day in news, "${eclipse.title} $day")
            }
            assertTrue("我国" in eclipse.detail || "全国" in eclipse.detail, "$day 要写明我国能否看到")
        }
        assertEquals(SkyEvents.ECLIPSES.sortedBy { it.day }, SkyEvents.ECLIPSES)
        val events = SkyEvents.upcoming(DayKey(2035, 8, 20), 30)
        assertTrue(events.any { it.kind == SkyEvent.Kind.ECLIPSE && it.day == DayKey(2035, 9, 2) && it.title == "日全食" })
        assertTrue(events.any { it.kind == SkyEvent.Kind.NEW_MOON && it.day == DayKey(2035, 9, 2) })
    }

    /** 跨十年逐月检查：相邻两次满月间隔 29–30 天，没有漏算或重复 */
    @Test
    fun lunationsAreContinuous() {
        for (target in listOf(0.0, 0.5)) {
            val moments = SkyEvents.phaseMoments(target, DayKey(2026, 1, 1), DayKey(2035, 12, 31))
            assertTrue(moments.size in 122..125, "${moments.size}")
            moments.zipWithNext().forEach { (a, b) ->
                val gap = (b.toEpochMilli() - a.toEpochMilli()) / 86_400_000.0
                assertTrue(gap in 29.2..29.9, "gap $gap at $a")
            }
        }
    }
}

class ShenshaTest {
    private fun shensha(y: Int, m: Int, d: Int) = AlmanacDay(DayKey(y, m, d)).shensha

    /** 期望值来自独立的 Python 实现（PyEphem 节气 + 干支），2026–2027 年逐日比对全部一致，这里抽样 */
    @Test
    fun knownDays() {
        assertEquals(listOf("天赦日"), shensha(2026, 3, 5)) // 卯月戊寅
        assertEquals(listOf("天赦日", "四绝日"), shensha(2026, 5, 4)) // 辰月戊寅，立夏前一天
        assertEquals(listOf("天赦日"), shensha(2026, 5, 20)) // 巳月甲午
        assertEquals(listOf("天德", "月德", "天赦日"), shensha(2026, 7, 19)) // 未月甲午
        assertEquals(listOf("天赦日"), shensha(2026, 10, 1)) // 酉月戊申
        assertEquals(listOf("天赦日"), shensha(2026, 12, 16)) // 子月甲子
        assertEquals(listOf("四绝日"), shensha(2026, 2, 3)) // 立春前一天
        assertEquals(listOf("四离日"), shensha(2026, 3, 19)) // 春分前一天
        assertEquals(listOf("四离日"), shensha(2026, 6, 20)) // 夏至前一天
        assertEquals(listOf("四绝日"), shensha(2026, 8, 6)) // 立秋前一天
        assertEquals(listOf("天德合", "四离日"), shensha(2026, 9, 22)) // 秋分前一天，酉月亥日
        assertEquals(listOf("四绝日"), shensha(2026, 11, 6)) // 立冬前一天
        assertEquals(listOf("天德", "四离日"), shensha(2026, 12, 21)) // 冬至前一天，子月巳日
        assertEquals(listOf("天德"), shensha(2026, 2, 12)) // 寅月丁日
        assertEquals(listOf("天德合", "月德合"), shensha(2026, 10, 14)) // 戌月辛日
        assertEquals(listOf("天德", "月德"), shensha(2026, 10, 19)) // 戌月丙日
        assertEquals(emptyList(), shensha(2026, 10, 16))
        assertEquals(listOf("天赦日"), shensha(2027, 2, 28)) // 寅月戊寅
        assertEquals(listOf("天德", "月德", "天赦日"), shensha(2027, 7, 14))
    }

    /** 宜忌调整后的黄金值：与 iOS 版 AlmanacDay 同一天的 shensha / yi / ji 逐项相同 */
    @Test
    fun goldenYiJi() {
        fun check(key: DayKey, shensha: List<String>, yi: List<String>, ji: List<String>) {
            val a = AlmanacDay(key)
            assertEquals(shensha, a.shensha, "$key 神煞")
            assertEquals(yi, a.yi, "$key 宜")
            assertEquals(ji, a.ji, "$key 忌")
        }
        // 未月甲午：天德、月德、天赦同日
        check(DayKey(2026, 7, 19), listOf("天德", "月德", "天赦日"),
            listOf("祈福", "求嗣", "祭祀", "解除", "筑堤", "补垣", "修墙", "安葬", "收藏"),
            listOf("开市", "出行", "求医", "上任", "开渠"))
        // 立夏前一天又逢天赦：原有的「祈福」留在原位，出行、嫁娶移到忌的最前
        check(DayKey(2026, 5, 4), listOf("天赦日", "四绝日"),
            listOf("祭祀", "解除", "开市", "开业", "入学", "求职", "祈福"),
            listOf("出行", "嫁娶", "安葬", "伐木", "动土"))
        // 冬至前一天：出行本来就在忌里（位置不动），只把嫁娶插到最前
        check(DayKey(2026, 12, 21), listOf("天德", "四离日"),
            listOf("求嗣", "祈福", "栽种", "纳畜", "捕捉", "立约"),
            listOf("嫁娶", "开市", "出行", "移徙", "搬家"))
        check(DayKey(2026, 10, 14), listOf("天德合", "月德合"),
            listOf("求嗣", "筑堤", "补垣", "修墙", "安葬", "收藏", "祈福"),
            listOf("开市", "出行", "求医", "上任", "开渠"))
        // 破日逢天德、月德
        check(DayKey(2026, 10, 9), listOf("天德", "月德"),
            listOf("祈福", "求嗣", "求医", "治病", "破旧"),
            listOf("嫁娶", "开市", "签约", "动土", "出行"))
        // 申月戊申：天德合 + 天赦
        check(DayKey(2024, 8, 12), listOf("天德合", "天赦日"),
            listOf("求嗣", "祭祀", "解除", "出行", "上任", "会友", "求职", "祈福"),
            listOf("动土", "开仓", "嫁娶", "安葬"))
        // 四离日逢破日
        check(DayKey(2024, 12, 20), listOf("四离日"),
            listOf("求医", "治病", "破旧"),
            listOf("嫁娶", "开市", "签约", "动土", "出行"))
        // 酉月天德在寅（比日支）；寒露当天已是戌月
        check(DayKey(2026, 10, 7), listOf("天德"),
            listOf("求嗣", "祈福", "栽种", "纳畜", "捕捉", "立约"),
            listOf("开市", "出行", "移徙", "搬家"))
        check(DayKey(2026, 10, 8), emptyList(),
            listOf("祈福", "栽种", "纳畜", "捕捉", "立约"),
            listOf("开市", "出行", "移徙", "搬家"))
        check(DayKey(2024, 1, 1), listOf("天赦日"),
            listOf("祭祀", "解除", "出行", "上任", "会友", "求职", "祈福"),
            listOf("动土", "开仓", "嫁娶", "安葬"))
    }

    /** 2024–2027 每年各神煞的天数，与 iOS 逐日计算的统计相同 */
    @Test
    fun yearlyCounts() {
        val expected = mapOf(
            2024 to listOf(36, 35, 34, 37, 7, 4, 4),
            2025 to listOf(33, 34, 38, 35, 6, 4, 4),
            2026 to listOf(36, 35, 35, 38, 6, 4, 4),
            2027 to listOf(35, 33, 38, 36, 6, 4, 4),
        )
        val names = listOf("天德", "天德合", "月德", "月德合", "天赦日", "四绝日", "四离日")
        for ((year, counts) in expected) {
            val all = mutableListOf<String>()
            var day = DayKey(year, 1, 1)
            while (day.year == year) {
                all += AlmanacDay(day).shensha
                day = day.adding(1)
            }
            assertEquals(counts, names.map { name -> all.count { it == name } }, "$year")
        }
    }

    @Test
    fun tableRules() {
        // 干支须阴阳相配（GanZhi.of 的干支奇偶要一致）
        // 寅月（正月）：丁日天德、壬日天德合、丙日月德、辛日月德合
        assertEquals(listOf("天德"), Shensha.of(0, GanZhi.of(3, 1), null))
        assertEquals(listOf("天德合"), Shensha.of(0, GanZhi.of(8, 0), null))
        assertEquals(listOf("月德"), Shensha.of(0, GanZhi.of(2, 0), null))
        assertEquals(listOf("月德合"), Shensha.of(0, GanZhi.of(7, 1), null))
        // 卯月（二月）天德在申、天德合在巳：比日支
        assertEquals(listOf("天德"), Shensha.of(1, GanZhi.of(4, 8), null))
        assertEquals(listOf("天德合"), Shensha.of(1, GanZhi.of(1, 5), null))
        // 亥卯未月月德甲、月德合己
        assertEquals(listOf("月德"), Shensha.of(1, GanZhi.of(0, 0), null))
        assertEquals(listOf("月德合"), Shensha.of(9, GanZhi.of(5, 3), null))
        // 天赦四季
        assertEquals(listOf("天赦日"), Shensha.of(2, GanZhi.of(4, 2), null))
        assertEquals(listOf("天德", "月德", "天赦日"), Shensha.of(5, GanZhi.of(0, 6), null))
        assertEquals(listOf("天赦日"), Shensha.of(7, GanZhi.of(4, 8), null))
        assertEquals(listOf("天赦日"), Shensha.of(11, GanZhi.of(0, 0), null))
        // 不是天赦的季节
        assertEquals(emptyList(), Shensha.of(3, GanZhi.of(4, 2), null))
        assertEquals(listOf("四绝日"), Shensha.of(4, GanZhi.of(1, 1), "立秋"))
        assertEquals(listOf("四离日"), Shensha.of(4, GanZhi.of(1, 1), "夏至"))
        assertEquals(emptyList(), Shensha.of(4, GanZhi.of(1, 1), "小暑"))
        assertTrue(Shensha.isLucky("天赦日"))
        assertFalse(Shensha.isLucky("四离日"))
        assertEquals(7, Shensha.NOTES.size)
    }

    @Test
    fun yiJiAdjustment() {
        // 新加的放在最前，原有项位置不变，整体去重
        val (yi, ji) = Shensha.adjust(listOf("祭祀", "祈福", "开市"), listOf("动土"), listOf("天德", "天赦日"))
        assertEquals(listOf("求嗣", "解除", "祭祀", "祈福", "开市"), yi)
        assertEquals(listOf("动土"), ji)
        val (yi2, ji2) = Shensha.adjust(listOf("嫁娶", "开市", "出行"), listOf("诉讼", "出行"), listOf("四离日"))
        assertEquals(listOf("开市"), yi2)
        assertEquals(listOf("嫁娶", "诉讼", "出行"), ji2)
        val (yi3, ji3) = Shensha.adjust(listOf("出行"), listOf("动土"), emptyList())
        assertEquals(listOf("出行"), yi3)
        assertEquals(listOf("动土"), ji3)

        // 实际日子：天赦日有祭祀、祈福、解除；四离四绝日忌出行、嫁娶且宜里没有
        for (key in listOf(DayKey(2026, 3, 5), DayKey(2026, 5, 4), DayKey(2026, 7, 19))) {
            val day = AlmanacDay(key)
            assertTrue(day.yi.containsAll(listOf("祭祀", "祈福", "解除")), "$key ${day.yi}")
            assertEquals(day.yi.distinct(), day.yi)
        }
        for (key in listOf(DayKey(2026, 2, 3), DayKey(2026, 3, 19), DayKey(2026, 5, 4), DayKey(2026, 9, 22))) {
            val day = AlmanacDay(key)
            assertTrue(day.ji.containsAll(listOf("出行", "嫁娶")), "$key ${day.ji}")
            assertFalse("出行" in day.yi || "嫁娶" in day.yi, "$key ${day.yi}")
        }
        val de = AlmanacDay(DayKey(2026, 10, 19))
        assertTrue(de.yi.containsAll(listOf("祈福", "求嗣")))
        // 同一建除值的两天，神煞不同时宜不同（宜忌不再 12 天一轮）
        val plain = AlmanacDay(DayKey(2026, 10, 31))
        val blessed = AlmanacDay(DayKey(2026, 10, 19))
        assertEquals(plain.jianChu.index, blessed.jianChu.index)
        assertTrue(plain.yi != blessed.yi, "${plain.yi} ${blessed.yi}")
    }
}

class AuspiciousDaysTest {
    @Test
    fun rulesHold() {
        val start = DayKey(2026, 10, 16)
        val horse = EarthlyBranch(6)
        for (event in AuspiciousEvent.entries) {
            for (weekendsOnly in listOf(false, true)) {
                val result = AuspiciousDays.find(event, start, 60, horse, weekendsOnly)
                assertTrue(result.size <= 10)
                assertEquals(result.sortedWith(compareByDescending<AuspiciousDay> { it.score }.thenBy { it.day }), result)
                for (found in result) {
                    val almanac = AlmanacDay(found.day)
                    assertTrue(found.day >= start && found.day < start.adding(60))
                    assertTrue(almanac.jianChu.index != 6)
                    assertFalse(almanac.ji.any { it in event.jiWords }, "$event ${found.day}")
                    assertFalse("四离日" in almanac.shensha || "四绝日" in almanac.shensha)
                    if (weekendsOnly) assertTrue(found.day.weekday == 0 || found.day.weekday == 6)
                    assertEquals(found.clashesUser, almanac.clashAnimal == horse)
                    assertTrue(found.summary.endsWith(if (found.clashesUser) "冲你的生肖" else "不冲属马"), found.summary)
                    assertTrue(found.matched.isNotEmpty() || (almanac.deity.isYellowPath && almanac.jianChu.isLucky))
                }
            }
        }
        assertTrue(AuspiciousDays.find(AuspiciousEvent.OPENING, start, 0).isEmpty())
        // 理发避开丁日（丁不剃头）
        assertTrue(AuspiciousDays.find(AuspiciousEvent.HAIRCUT, start, 90, limit = 90).none { AlmanacDay(it.day).day.stem.index == 3 })
        // 没有档案时不写生肖
        val anonymous = AuspiciousDays.find(AuspiciousEvent.OPENING, start)
        assertTrue(anonymous.all { !it.clashesUser && "属" !in it.summary && "冲你" !in it.summary })
    }

    @Test
    fun scoring() {
        val almanac = AlmanacDay(DayKey(2026, 10, 25))
        val found = assertNotNull(AuspiciousDays.evaluate(AuspiciousEvent.OPENING, almanac, EarthlyBranch(6)))
        var expected = 0
        if (found.matched.isNotEmpty()) expected += 20 + 4 * (found.matched.size - 1)
        if (almanac.deity.isYellowPath) expected += 10
        if (almanac.jianChu.isLucky) expected += 8
        if ("天赦日" in almanac.shensha) expected += 6
        if (almanac.shensha.any { it in listOf("天德", "天德合", "月德", "月德合") }) expected += 4
        if (found.day.weekday == 0 || found.day.weekday == 6) expected += 3
        if (found.clashesUser) expected -= 20
        assertEquals(expected, found.score)
    }

    /** 固定输入的输出（iOS 版同样的输入应得到同样的结果） */
    @Test
    fun stableOutput() {
        val result = AuspiciousDays.find(AuspiciousEvent.OPENING, DayKey(2026, 10, 16), 60, EarthlyBranch(6))
        assertEquals(
            listOf(20261111, 20261205, 20261212, 20261117, 20261123, 20261025, 20261129, 20261019, 20261031, 20261107),
            result.map { it.day.rawValue },
        )
        assertEquals(listOf(50, 49, 49, 46, 46, 45, 45, 42, 41, 39), result.map { it.score })
        assertEquals("满日，玉堂黄道；宜开市、交易、纳财；月德合；不冲属马", result[0].summary)
        assertEquals("开日，金匮黄道；宜开市、开业；不冲属马", result[5].summary)
        assertEquals("定日，司命黄道；宜交易；天德、月德；不冲属马", result[7].summary)
        assertEquals("开日；宜开市、开业；天德；不冲属马", result[9].summary)
        // 三周内最好的是 10 月 25 日（与改版提案里的示例一致）
        assertEquals(DayKey(2026, 10, 25), AuspiciousDays.find(AuspiciousEvent.OPENING, DayKey(2026, 10, 16), 21)[0].day)
        // 只看周末：搬家在 60 天里只有 7 个周末日子可选，宜里没有对口用词的按「黄道且建除为吉」入选
        val weekends = AuspiciousDays.find(AuspiciousEvent.MOVING, DayKey(2026, 10, 16), 60, EarthlyBranch(6), weekendsOnly = true)
        assertEquals(
            listOf(
                "20261212/45/成日，青龙黄道；宜移徙；天德合；不冲属马",
                "20261129/41/成日，明堂黄道；宜移徙；不冲属马",
                "20261121/25/建日，天德黄道；月德合；不冲属马",
                "20261025/21/开日，金匮黄道；不冲属马",
                "20261031/21/定日，司命黄道；不冲属马",
                "20261205/21/满日，玉堂黄道；不冲属马",
                "20261213/21/收日，明堂黄道；不冲属马",
            ),
            weekends.map { "${it.day.rawValue}/${it.score}/${it.summary}" },
        )
        assertEquals(listOf("移徙"), weekends[0].matched)
    }

    /** 冲当前档案生肖的日子降 20 分并标注 */
    @Test
    fun clashDemotes() {
        val start = DayKey(2026, 10, 16)
        for (event in AuspiciousEvent.entries) {
            for (offset in 0 until 60) {
                val almanac = AlmanacDay(start.adding(offset))
                val plain = AuspiciousDays.evaluate(event, almanac) ?: continue
                val clashed = assertNotNull(AuspiciousDays.evaluate(event, almanac, almanac.clashAnimal))
                assertEquals(plain.score - 20, clashed.score)
                assertTrue(clashed.clashesUser)
                assertEquals(AuspiciousReason(AuspiciousReason.Kind.CLASH, "冲你的生肖"), clashed.reasons.last())
                assertEquals(plain.summary + "；冲你的生肖", clashed.summary)
                val other = assertNotNull(AuspiciousDays.evaluate(event, almanac, almanac.clashAnimal.opposite))
                assertEquals(plain.score, other.score)
                assertEquals(plain.summary + "；不冲属" + almanac.clashAnimal.opposite.animal, other.summary)
            }
        }
        assertEquals(AuspiciousEvent.CAR, AuspiciousEvent.of("car"))
        assertNull(AuspiciousEvent.of("unknown"))
        assertEquals(
            listOf("moving", "opening", "wedding", "travel", "contract", "renovation", "haircut", "interview", "car", "worship"),
            AuspiciousEvent.entries.map { it.key },
        )
    }
}
