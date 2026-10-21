package com.tianaofeng.xingxia.core

/** 月相徽章的进度 */
data class MoonBadgeProgress(
    /** 当前这一轮已经集到的月相 */
    val collected: Set<MoonPhaseName>,
    /** 历史上（含本轮）集齐 8 种月相的轮数，也就是已获得的满月徽章数 */
    val earned: Int,
) {
    companion object {
        val EMPTY = MoonBadgeProgress(emptySet(), 0)
    }
}

/**
 * 月相徽章，与 iOS 版 Shared/Astro/MoonBadges.swift 规则一致：以朔望月为一轮，一轮从新月所在的那天（北京时间）开始，
 * 到下一次新月的前一天结束，新月当天算新一轮的第一天（和农历初一的定法一致）。
 * 完成每日仪式的日子按 MoonInfo.on(day)（当天 21:00 北京时间）取八相之一，一轮里八相都出现过就点亮一枚满月徽章。
 * 跳过的日子由调用方去掉；晚于 today 的日子忽略。
 */
object MoonBadges {
    /** ritualDays、today 都是 yyyyMMdd */
    fun progress(ritualDays: List<Int>, today: Int): MoonBadgeProgress {
        val todayKey = validDay(today) ?: return MoonBadgeProgress.EMPTY
        val days = ritualDays.toSet().filter { it <= today }.sorted().mapNotNull { validDay(it) }
        if (days.isEmpty()) return MoonBadgeProgress.EMPTY
        val current = lunation(todayKey)
        var collected = emptySet<MoonPhaseName>()
        var earned = 0
        var index = 0
        while (index < days.size) {
            // days[index] 所在的一轮到下一次新月为止，这之前的日子都属于同一轮
            val first = days[index]
            val end = SkyEvents.nextNewMoon(first)
            val phases = mutableSetOf<MoonPhaseName>()
            while (index < days.size && days[index] < end) {
                phases.add(MoonInfo.on(days[index]).name)
                index += 1
            }
            if (phases.size == MoonPhaseName.entries.size) earned += 1
            if (first in current) collected = phases
        }
        return MoonBadgeProgress(collected, earned)
    }

    @JvmName("progressOfDays")
    fun progress(ritualDays: List<DayKey>, today: DayKey): MoonBadgeProgress =
        progress(ritualDays.map { it.rawValue }, today.rawValue)

    /** day 所在的一轮：这一轮的新月日 ..< 下一次新月日 */
    fun lunation(containing: DayKey): OpenEndRange<DayKey> {
        val next = SkyEvents.nextNewMoon(containing)
        // 相邻两次新月相隔 29 或 30 天：从 next 往前 32 天之后的第一次新月，就是这一轮的起点
        val start = SkyEvents.nextNewMoon(next.adding(-32))
        return start..<next
    }

    /** containing 为 yyyyMMdd；不是有效日期时按今天算 */
    fun lunation(containing: Int): OpenEndRange<DayKey> = lunation(validDay(containing) ?: DayKey.today)

    /** yyyyMMdd 转 DayKey；不是有效日期（如 20261340）时为 null */
    private fun validDay(raw: Int): DayKey? {
        val day = DayKey.fromRaw(raw) ?: return null
        return runCatching { day.localDate }.getOrNull()?.let { day }
    }
}
