package com.tianaofeng.xingxia.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MoonBadgesTest {
    private val allPhases = MoonPhaseName.entries.toSet()

    /** [from, until) 之间的每一天，yyyyMMdd */
    private fun everyDay(from: DayKey, until: DayKey): List<Int> =
        generateSequence(from) { it.adding(1) }.takeWhile { it < until }.map { it.rawValue }.toList()

    @Test
    fun empty() {
        assertEquals(MoonBadgeProgress.EMPTY, MoonBadges.progress(emptyList(), 20261020))
        // 晚于今天的日子不算
        assertEquals(MoonBadgeProgress.EMPTY, MoonBadges.progress(listOf(20261021, 20261101), 20261020))
        // 无效日期忽略
        assertEquals(MoonBadgeProgress.EMPTY, MoonBadges.progress(listOf(0, 20261340), 20261020))
    }

    @Test
    fun lunation() {
        // 2026 年 10 月 10 日、11 月 9 日新月（见 SkyTest）
        val october = MoonBadges.lunation(DayKey(2026, 10, 20))
        assertEquals(DayKey(2026, 10, 10), october.start)
        assertEquals(DayKey(2026, 11, 9), october.endExclusive)
        // 新月当天是新一轮的第一天，前一天还在上一轮
        assertEquals(DayKey(2026, 11, 9), MoonBadges.lunation(DayKey(2026, 11, 9)).start)
        assertEquals(DayKey(2026, 10, 10), MoonBadges.lunation(DayKey(2026, 11, 8)).start)
        assertEquals(DayKey(2026, 11, 9), MoonBadges.lunation(DayKey(2026, 11, 8)).endExclusive)
        assertEquals(DayKey(2026, 10, 10), MoonBadges.lunation(20261010).start)
        // 跨年的一轮
        val newYear = MoonBadges.lunation(20270101)
        assertEquals(DayKey(2026, 12, 9), newYear.start)
        assertEquals(2027, newYear.endExclusive.year)
        assertEquals(SkyEvents.nextNewMoon(DayKey(2026, 12, 9)), newYear.endExclusive)
    }

    @Test
    fun lunationsTileTheCalendar() {
        // 一轮紧接着一轮，每轮 29 或 30 天
        var day = DayKey(2025, 1, 1)
        var lunation = MoonBadges.lunation(day)
        repeat(30) {
            val length = lunation.start.daysTo(lunation.endExclusive)
            assertTrue(length in 29..30, "$lunation")
            assertTrue(day in lunation)
            day = lunation.endExclusive
            val next = MoonBadges.lunation(day)
            assertEquals(lunation.endExclusive, next.start)
            lunation = next
        }
    }

    @Test
    fun newMoonDayStartsANewRound() {
        // 11 月 9 日新月：8 日和 9 日分属两轮
        val eighth = MoonInfo.on(DayKey(2026, 11, 8)).name
        val ninth = MoonInfo.on(DayKey(2026, 11, 9)).name
        assertEquals(MoonPhaseName.NEW, ninth)
        val progress = MoonBadges.progress(listOf(20261108, 20261109), 20261109)
        assertEquals(setOf(ninth), progress.collected)
        assertEquals(0, progress.earned)
        // 今天是 8 日时，9 日还没到，不算
        assertEquals(setOf(eighth), MoonBadges.progress(listOf(20261108, 20261109), 20261108).collected)
    }

    @Test
    fun fullRoundEarnsABadge() {
        val round = everyDay(DayKey(2026, 10, 10), DayKey(2026, 11, 9))
        val lastDay = MoonBadges.progress(round, 20261108)
        assertEquals(allPhases, lastDay.collected)
        assertEquals(1, lastDay.earned)
        // 新月一到就是新的一轮：本轮还没有记录，已获得的徽章还在
        val nextRound = MoonBadges.progress(round, 20261109)
        assertEquals(emptySet(), nextRound.collected)
        assertEquals(1, nextRound.earned)
        // 重复的日子不影响结果，顺序也无所谓
        assertEquals(lastDay, MoonBadges.progress((round + round).reversed(), 20261108))
        // DayKey 版本结果相同
        assertEquals(lastDay, MoonBadges.progress(round.map { DayKey.fromRaw(it)!! }, DayKey(2026, 11, 8)))
    }

    @Test
    fun ongoingRound() {
        // 本轮只打卡了前几天：集到的月相不全，没有徽章
        val firstWeek = everyDay(DayKey(2026, 10, 10), DayKey(2026, 10, 17))
        val progress = MoonBadges.progress(firstWeek, 20261020)
        assertEquals(firstWeek.map { MoonInfo.on(DayKey.fromRaw(it)!!).name }.toSet(), progress.collected)
        assertTrue(progress.collected.size in 2..7)
        assertEquals(0, progress.earned)
    }

    @Test
    fun skippedDaysAreLeftOut() {
        // 调用方去掉跳过的日子：满月前后几天都跳过，这一轮就缺满月
        val round = everyDay(DayKey(2026, 10, 10), DayKey(2026, 11, 9))
        val completed = round.filter { MoonInfo.on(DayKey.fromRaw(it)!!).name != MoonPhaseName.FULL }
        assertTrue(completed.size < round.size)
        val progress = MoonBadges.progress(completed, 20261108)
        assertEquals(allPhases - MoonPhaseName.FULL, progress.collected)
        assertEquals(0, progress.earned)
    }

    @Test
    fun acrossTheYear() {
        // 2026-12-09 新月开始的一轮跨到 2027 年
        val end = SkyEvents.nextNewMoon(DayKey(2026, 12, 9))
        val round = everyDay(DayKey(2026, 12, 9), end)
        assertTrue(round.any { it >= 20270101 })
        val progress = MoonBadges.progress(round, end.adding(-1).rawValue)
        assertEquals(allPhases, progress.collected)
        assertEquals(1, progress.earned)
        // 只打卡到 12 月 31 日：跨年那几天没打卡，这一轮还没集齐
        val december = round.filter { it < 20270101 }
        val partial = MoonBadges.progress(december, 20270102)
        assertEquals(0, partial.earned)
        assertEquals(december.map { MoonInfo.on(DayKey.fromRaw(it)!!).name }.toSet(), partial.collected)
    }

    @Test
    fun everyDayEveryRound() {
        // 天天都打卡：2026-01-19 到 12-08 正好 11 轮，每轮都集齐
        val days = everyDay(DayKey(2026, 1, 19), DayKey(2026, 12, 9))
        val progress = MoonBadges.progress(days, 20261208)
        assertEquals(11, progress.earned)
        assertEquals(allPhases, progress.collected)
        // 中间某一轮断了几天，少一枚
        val gap = days.filter { it !in 20260601..20260630 }
        assertTrue(MoonBadges.progress(gap, 20261208).earned < 11)
    }
}
