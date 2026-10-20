import Foundation

/// 月相徽章的进度
struct MoonBadgeProgress: Sendable, Equatable {
    /// 当前这一轮已经集到的月相
    let collected: Set<MoonPhaseName>
    /// 历史上（含本轮）集齐 8 种月相的轮数，也就是已获得的满月徽章数
    let earned: Int

    static let empty = MoonBadgeProgress(collected: [], earned: 0)
}

/// 月相徽章：以朔望月为一轮，一轮从新月所在的那天（北京时间）开始，到下一次新月的前一天结束，
/// 新月当天算新一轮的第一天（和农历初一的定法一致）。
/// 完成每日仪式的日子按 MoonInfo.on(day)（当天 21:00 北京时间）取八相之一，一轮里八相都出现过就点亮一枚满月徽章。
/// 跳过的日子由调用方去掉；晚于 today 的日子忽略。Android core 的 MoonBadges 按同样的规则实现。
enum MoonBadges {
    static func progress(ritualDays: [DayKey], today: DayKey) -> MoonBadgeProgress {
        let days = Set(ritualDays).filter { $0 <= today }.sorted()
        guard !days.isEmpty else { return .empty }
        let current = lunation(containing: today)
        var collected: Set<MoonPhaseName> = []
        var earned = 0
        var index = 0
        while index < days.count {
            // days[index] 所在的一轮到下一次新月为止，这之前的日子都属于同一轮
            let first = days[index]
            let end = SkyEvents.nextNewMoon(after: first)
            var phases: Set<MoonPhaseName> = []
            while index < days.count && days[index] < end {
                phases.insert(MoonInfo.on(days[index]).name)
                index += 1
            }
            if phases.count == MoonPhaseName.allCases.count { earned += 1 }
            if current.contains(first) { collected = phases }
        }
        return MoonBadgeProgress(collected: collected, earned: earned)
    }

    /// day 所在的一轮：这一轮的新月日 ..< 下一次新月日
    static func lunation(containing day: DayKey) -> Range<DayKey> {
        let next = SkyEvents.nextNewMoon(after: day)
        // 相邻两次新月相隔 29 或 30 天：从 next 往前 32 天之后的第一次新月，就是这一轮的起点
        let start = SkyEvents.nextNewMoon(after: next.adding(days: -32))
        return start..<next
    }
}
