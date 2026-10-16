package com.tianaofeng.xingxia.core

/**
 * 择吉日的事项。每件事对应黄历里的若干「宜」用词和「忌」用词。
 * 与 iOS 版 Shared/Almanac/AuspiciousDays.swift 的 AuspiciousEvent 逐项对应（key、名称、用词、彭祖忌日都相同）。
 */
enum class AuspiciousEvent(
    /** 与 iOS rawValue 相同 */
    val key: String,
    val label: String,
    /** 选择格里的小字说明 */
    val subtitle: String,
    val emoji: String,
    /** 黄历「宜」里出现其中任意一个，就算对口 */
    val yiWords: List<String>,
    /** 黄历「忌」里出现其中任意一个，这天就不选 */
    val jiWords: List<String>,
    /** 彭祖百忌里与这件事相关的日干（丁不剃头），没有为 null */
    val avoidStem: Int? = null,
    /** 彭祖百忌里与这件事相关的日支（巳不远行、亥不嫁娶、寅不祭祀），没有为 null */
    val avoidBranch: Int? = null,
) {
    MOVING("moving", "搬家", "入宅 · 移徙", "🏠", listOf("入宅", "移徙", "搬家"), listOf("入宅", "移徙", "搬家")),
    OPENING("opening", "开业", "开市 · 开工", "🎊", listOf("开市", "开业", "开工", "交易", "纳财"), listOf("开市", "开业")),
    WEDDING("wedding", "结婚领证", "嫁娶 · 订盟", "💍", listOf("嫁娶", "订盟", "纳采"), listOf("嫁娶"), avoidBranch = 11),
    TRAVEL("travel", "出行", "远行 · 旅游", "🧳", listOf("出行"), listOf("出行", "远行"), avoidBranch = 5),
    CONTRACT("contract", "签约", "立约 · 交易", "✍️", listOf("立券", "立约", "交易", "订盟"), listOf("签约")),
    RENOVATION("renovation", "动土装修", "动土 · 修造", "🔨", listOf("动土", "修造", "修饰", "涂泥", "开工", "修墙", "补垣"), listOf("动土")),
    HAIRCUT("haircut", "理发", "剪发 · 沐浴", "💇", listOf("理发", "剃头", "沐浴"), listOf("理发", "剃头"), avoidStem = 3),
    INTERVIEW("interview", "求职面试", "求职 · 上任", "💼", listOf("求职", "上任", "会友"), listOf("上任")),
    CAR("car", "购车提车", "交易 · 纳财", "🚗", listOf("交易", "纳财", "出行"), listOf("出行")),
    WORSHIP("worship", "祭祀祈福", "祭祀 · 祈福", "🙏", listOf("祭祀", "祈福", "求嗣", "解除"), listOf("祭祀", "祈福"), avoidBranch = 2);

    companion object {
        fun of(key: String): AuspiciousEvent? = entries.firstOrNull { it.key == key }
    }
}

/** 择吉日的一条理由 */
data class AuspiciousReason(val kind: Kind, val text: String) {
    enum class Kind {
        /** 建除，如「开日」 */
        JIAN_CHU,
        /** 黄道值神，如「金匮黄道」 */
        DEITY,
        /** 对口的宜，如「宜开市、交易」 */
        YI,
        /** 吉神，如「天德」「天赦日」 */
        SHENSHA,
        /** 不冲你的生肖，如「不冲属马」（马为档案生肖） */
        ZODIAC,
        /** 冲你的生肖（文字固定为「冲你的生肖」） */
        CLASH,
    }
}

/** 一个推荐的日子 */
data class AuspiciousDay(
    val day: DayKey,
    val score: Int,
    /** 黄历「宜」里与事项对口的用词（按黄历里的顺序） */
    val matched: List<String>,
    val reasons: List<AuspiciousReason>,
    /** 这天冲当前档案的生肖 */
    val clashesUser: Boolean,
) {
    val id: Int get() = day.rawValue

    /** 一句话理由，如「开日，金匮黄道；宜开市、交易；天德；不冲属马」 */
    val summary: String
        get() {
            val head = reasons.filter { it.kind == AuspiciousReason.Kind.JIAN_CHU || it.kind == AuspiciousReason.Kind.DEITY }
                .joinToString("，") { it.text }
            val yi = reasons.filter { it.kind == AuspiciousReason.Kind.YI }.map { it.text }
            val shensha = reasons.filter { it.kind == AuspiciousReason.Kind.SHENSHA }.joinToString("、") { it.text }
            val zodiac = reasons.filter { it.kind == AuspiciousReason.Kind.ZODIAC || it.kind == AuspiciousReason.Kind.CLASH }.map { it.text }
            return (listOf(head) + yi + listOf(shensha) + zodiac).filter { it.isNotEmpty() }.joinToString("；")
        }
}

/**
 * 择吉日：对未来若干天逐日排黄历，按「宜」是否对口、黄道、建除、吉神打分，
 * 排除忌这件事的日子、破日、四离四绝日和彭祖忌日，冲当前档案生肖的日子降级。纯函数，与 iOS 版逐行对应。
 *
 * 计分（整数）：
 * - 宜里有对口用词：+20，每多一个再 +4
 * - 黄道日 +10；建除为吉（建满定成收开）+8
 * - 天赦日 +6；天德、天德合、月德、月德合有任意一个 +4
 * - 周六、周日 +3
 * - 冲你的生肖 -20
 * 宜里没有对口用词时，只有「黄道且建除为吉」的日子入选。按分数从高到低、同分按日期先后排序。
 */
object AuspiciousDays {
    val RANGE_OPTIONS = listOf(30, 60, 90)
    const val DEFAULT_RANGE = 60
    const val DEFAULT_LIMIT = 10

    private val DE_SHENSHA = listOf("天德", "天德合", "月德", "月德合")
    private const val SHE_SHENSHA = "天赦日"
    private val BAD_SHENSHA = listOf("四绝日", "四离日")

    /**
     * @param event 要办的事
     * @param from 起始日（含）
     * @param days 往后看多少天（含起始日）
     * @param animal 当前档案的生肖（年支），没有档案时为 null
     * @param weekendsOnly 只看周六、周日
     * @param limit 最多返回几天
     */
    fun find(
        event: AuspiciousEvent,
        from: DayKey,
        days: Int = DEFAULT_RANGE,
        animal: EarthlyBranch? = null,
        weekendsOnly: Boolean = false,
        limit: Int = DEFAULT_LIMIT,
    ): List<AuspiciousDay> {
        if (days <= 0 || limit <= 0) return emptyList()
        val result = mutableListOf<AuspiciousDay>()
        for (offset in 0 until days) {
            evaluate(event, AlmanacDay(from.adding(offset)), animal, weekendsOnly)?.let { result.add(it) }
        }
        return result.sortedWith(compareByDescending<AuspiciousDay> { it.score }.thenBy { it.day }).take(limit)
    }

    /** 单独评估一天；这天不适合办这件事时返回 null */
    fun evaluate(
        event: AuspiciousEvent,
        almanac: AlmanacDay,
        animal: EarthlyBranch? = null,
        weekendsOnly: Boolean = false,
    ): AuspiciousDay? {
        val day = almanac.key
        val isWeekend = day.weekday == 0 || day.weekday == 6
        if (weekendsOnly && !isWeekend) return null

        // 排除：破日、大事勿用、四离四绝、忌这件事、彭祖忌日
        if (almanac.jianChu.index == 6 || "大事勿用" in almanac.ji) return null
        if (almanac.shensha.any { it in BAD_SHENSHA }) return null
        if (almanac.ji.any { it in event.jiWords }) return null
        if (event.avoidStem != null && almanac.day.stem.index == event.avoidStem) return null
        if (event.avoidBranch != null && almanac.day.branch.index == event.avoidBranch) return null

        val matched = almanac.yi.filter { it in event.yiWords }
        val isYellow = almanac.deity.isYellowPath
        val isLucky = almanac.jianChu.isLucky
        if (matched.isEmpty() && !(isYellow && isLucky)) return null

        val lucky = almanac.shensha.filter { it in DE_SHENSHA || it == SHE_SHENSHA }
        val clashes = animal != null && almanac.clashAnimal == animal

        var score = 0
        if (matched.isNotEmpty()) score += 20 + 4 * (matched.size - 1)
        if (isYellow) score += 10
        if (isLucky) score += 8
        if (SHE_SHENSHA in almanac.shensha) score += 6
        if (almanac.shensha.any { it in DE_SHENSHA }) score += 4
        if (isWeekend) score += 3
        if (clashes) score -= 20

        val reasons = mutableListOf(AuspiciousReason(AuspiciousReason.Kind.JIAN_CHU, almanac.jianChu.name))
        if (isYellow) reasons.add(AuspiciousReason(AuspiciousReason.Kind.DEITY, almanac.deity.name + "黄道"))
        if (matched.isNotEmpty()) reasons.add(AuspiciousReason(AuspiciousReason.Kind.YI, "宜" + matched.joinToString("、")))
        lucky.forEach { reasons.add(AuspiciousReason(AuspiciousReason.Kind.SHENSHA, it)) }
        if (animal != null) {
            reasons.add(
                if (clashes) AuspiciousReason(AuspiciousReason.Kind.CLASH, "冲你的生肖")
                else AuspiciousReason(AuspiciousReason.Kind.ZODIAC, "不冲属${animal.animal}"),
            )
        }
        return AuspiciousDay(day, score, matched, reasons, clashes)
    }
}
