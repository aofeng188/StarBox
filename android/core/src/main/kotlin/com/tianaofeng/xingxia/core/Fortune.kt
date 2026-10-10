package com.tianaofeng.xingxia.core

enum class FortuneCategory(val key: String, val label: String, val shortName: String) {
    LOVE("love", "爱情", "爱情"),
    CAREER("career", "事业学业", "事业"),
    WEALTH("wealth", "财运", "财运"),
    HEALTH("health", "健康", "健康"),
}

object StarRating {
    /** 分数换算为 1…5 星（以半星为单位） */
    fun stars(score: Int): Double {
        val raw = (score - 30.0) / 70 * 5
        return minOf(5.0, maxOf(1.0, (raw * 2).roundHalfAway() / 2))
    }
}

/** 某人某天的运势 */
data class DailyFortune(
    val day: DayKey,
    val score: Int,
    val levelName: String,
    /** 0 = 大吉 … 4 = 宜静 */
    val levelIndex: Int,
    val summary: String,
    val categoryScores: Map<FortuneCategory, Int>,
    val categoryTexts: Map<FortuneCategory, String>,
    val luckyColor: NamedColor,
    val luckyNumber: Int,
    val luckyDirection: String,
    val wealthDirection: String,
    val luckyHour: EarthlyBranch,
    val luckyItem: String,
    val noblemanSign: ZodiacSign,
    val favorableElement: WuXing,
    val funYi: List<String>,
    val funJi: List<String>,
    val quote: String,
    val insights: List<String>,
    val moonSign: ZodiacSign,
    val isPersonalized: Boolean,
) {
    val stars: Double get() = StarRating.stars(score)
    fun score(category: FortuneCategory): Int = categoryScores[category] ?: score
    fun text(category: FortuneCategory): String = categoryTexts[category] ?: ""
}

object FortuneEngine {
    fun daily(birth: BirthInfo?, day: DayKey = DayKey.today): DailyFortune {
        val content = ContentLibrary.fortune
        val rng = SeededGenerator("daily-fortune", day.rawValue, birth?.seedKey ?: "anonymous")
        val almanac = AlmanacDay(day)
        val moonSign = ZodiacSign.moonSign(day.noon)

        var base = rng.gaussian(69.0, 12.0)
        val mods = FortuneCategory.entries.associateWith { 0.0 }.toMutableMap()
        fun bump(category: FortuneCategory, value: Double) { mods[category] = mods.getValue(category) + value }
        val insights = mutableListOf<String>()
        val favorable: WuXing
        var nobleman = ZodiacSign.entries[rng.intBelow(12)]

        if (birth != null) {
            val bazi = BaZi(birth)
            val master = bazi.dayMaster.element
            val today = almanac.day.stem.element
            val relationKey: String
            when (today) {
                master -> { relationKey = "peer"; base += 2; bump(FortuneCategory.CAREER, 4.0); bump(FortuneCategory.LOVE, 2.0) }
                master.generatedBy -> { relationKey = "resource"; base += 5; bump(FortuneCategory.CAREER, 4.0); bump(FortuneCategory.HEALTH, 5.0) }
                master.generates -> { relationKey = "output"; base += 1; bump(FortuneCategory.LOVE, 6.0); bump(FortuneCategory.WEALTH, 2.0) }
                master.controls -> { relationKey = "wealth"; base += 3; bump(FortuneCategory.WEALTH, 9.0) }
                else -> { relationKey = "officer"; base -= 3; bump(FortuneCategory.CAREER, 3.0); bump(FortuneCategory.HEALTH, -5.0) }
            }
            content.relations[relationKey]?.let { template ->
                insights.add(
                    template
                        .replace("{day}", "${almanac.day.name}日属${today.label}")
                        .replace("{master}", "${bazi.dayMaster.name}${master.label}"),
                )
            }

            val branchRelation = almanac.day.branch.relation(bazi.year.branch)
            base += branchRelation.score
            content.branchNotes[branchRelation.key]?.takeIf { it.isNotEmpty() }?.let { insights.add(it) }

            val sunSign = ZodiacSign.sunSign(birth)
            val aspect = AspectKind.of(sunSign.distance(moonSign))
            val aspectScore = when (aspect) {
                AspectKind.CONJUNCTION -> 3.0
                AspectKind.SEXTILE -> 4.0
                AspectKind.TRINE -> 6.0
                AspectKind.SQUARE -> -5.0
                AspectKind.OPPOSITION -> -3.0
                AspectKind.QUINCUNX -> -1.0
                AspectKind.SEMI_SEXTILE -> 0.0
            }
            base += aspectScore
            bump(FortuneCategory.LOVE, aspectScore * 1.2)
            content.moonNotes[aspect.key]?.takeIf { it.isNotEmpty() }?.let { insights.add(it) }

            favorable = bazi.favorableElement
            nobleman = rng.pick(sunSign.trines + sunSign.sextiles)
        } else {
            favorable = almanac.day.stem.element
        }

        if (almanac.deity.isYellowPath) base += 3
        if (almanac.jianChu.index == 6) base -= 4
        if (almanac.jianChu.index in listOf(8, 10)) base += 2

        val score = clampRound(base, 35, 99)
        val level = content.levels.indexOfFirst { score >= it.min }.takeIf { it >= 0 } ?: (content.levels.size - 1)
        val categoryScores = mutableMapOf<FortuneCategory, Int>()
        val categoryTexts = mutableMapOf<FortuneCategory, String>()
        for (category in FortuneCategory.entries) {
            val value = clampRound(score + rng.gaussian(0.0, 9.0) + mods.getValue(category), 28, 99)
            categoryScores[category] = value
            val pool = content.categories[category.key]?.textsFor(value).orEmpty()
            categoryTexts[category] = if (pool.isEmpty()) "" else rng.pick(pool)
        }

        var number = rng.pick(favorable.numbers) + 10 * rng.int(0..3)
        if (number == 0) number = 10

        // 参数求值顺序与 iOS 版保持一致，保证同一天两端结果相同
        val summary = rng.pick(content.levels[level].summaries)
        val luckyColor = rng.pick(favorable.luckyColors)
        val luckyItem = rng.pick(content.items)
        val funYi = rng.pick(content.funYi, 3)
        val funJi = rng.pick(content.funJi, 3)
        val quote = rng.pick(content.quotes)

        return DailyFortune(
            day = day,
            score = score,
            levelName = content.levels[level].name,
            levelIndex = level,
            summary = summary,
            categoryScores = categoryScores,
            categoryTexts = categoryTexts,
            luckyColor = luckyColor,
            luckyNumber = number,
            luckyDirection = almanac.joyDirection,
            wealthDirection = almanac.wealthDirection,
            luckyHour = EarthlyBranch(13 - almanac.day.branch.index),
            luckyItem = luckyItem,
            noblemanSign = nobleman,
            favorableElement = favorable,
            funYi = funYi,
            funJi = funJi,
            quote = quote,
            insights = insights,
            moonSign = moonSign,
            isPersonalized = birth != null,
        )
    }
}

enum class HoroscopePeriod(val key: String, val label: String) {
    TODAY("today", "今日"), TOMORROW("tomorrow", "明日"), WEEK("week", "本周"), MONTH("month", "本月"),
}

data class Horoscope(
    val sign: ZodiacSign,
    val period: HoroscopePeriod,
    val rangeText: String,
    val score: Int,
    val categoryScores: Map<FortuneCategory, Int>,
    val categoryTexts: Map<FortuneCategory, String>,
    val overview: String,
    val comment: String,
    val luckyColor: NamedColor,
    val luckyNumber: Int,
    val matchSign: ZodiacSign,
    val cautionSign: ZodiacSign,
    val skyNote: String,
) {
    val stars: Double get() = StarRating.stars(score)
    fun score(category: FortuneCategory): Int = categoryScores[category] ?: score
}

object HoroscopeEngine {
    fun horoscope(sign: ZodiacSign, period: HoroscopePeriod, reference: DayKey = DayKey.today): Horoscope {
        val content = ContentLibrary.horoscope
        val periodKey: Int
        val rangeText: String
        var skyBoost: Double
        val skyNote: String

        when (period) {
            HoroscopePeriod.TODAY, HoroscopePeriod.TOMORROW -> {
                val day = if (period == HoroscopePeriod.TODAY) reference else reference.adding(1)
                periodKey = day.rawValue
                rangeText = DateText.monthDay(day) + " " + DateText.weekday(day)
                val moon = ZodiacSign.moonSign(day.noon)
                val aspect = AspectKind.of(sign.distance(moon))
                skyBoost = boost(aspect)
                skyNote = "月亮行经${moon.label}，与${sign.label}形成${aspect.label}"
            }
            HoroscopePeriod.WEEK -> {
                // 周一为一周的开始
                val start = reference.adding(-((reference.weekday + 6) % 7))
                val end = start.adding(6)
                periodKey = start.rawValue * 10 + 7
                rangeText = "${start.month}.${start.day}–${end.month}.${end.day}"
                val sun = ZodiacSign.fromLongitude(Astronomy.sunLongitude(start.adding(3).noon))
                val aspect = AspectKind.of(sign.distance(sun))
                skyBoost = boost(aspect) * 0.8
                skyNote = "太阳正行经${sun.label}，与${sign.label}形成${aspect.label}"
            }
            HoroscopePeriod.MONTH -> {
                periodKey = reference.year * 100 + reference.month
                rangeText = "${reference.year}年${reference.month}月"
                val sun = ZodiacSign.fromLongitude(Astronomy.sunLongitude(DayKey(reference.year, reference.month, 15).noon))
                skyBoost = boost(AspectKind.of(sign.distance(sun))) * 0.8
                skyNote = "本月太阳主要行经${sun.label}"
            }
        }

        val rng = SeededGenerator("horoscope", sign.ordinal, period.key, periodKey)
        if (period == HoroscopePeriod.MONTH || period == HoroscopePeriod.WEEK) skyBoost += rng.double(-2.0, 2.0)
        val score = clampRound(rng.gaussian(68.0, 12.0) + skyBoost, 38, 98)

        // 不同元素的星座在不同领域略有天赋加成
        val elementBias: Map<FortuneCategory, Double> = when (sign.element) {
            ZodiacElement.FIRE -> mapOf(FortuneCategory.CAREER to 3.0, FortuneCategory.HEALTH to 2.0)
            ZodiacElement.EARTH -> mapOf(FortuneCategory.WEALTH to 4.0, FortuneCategory.CAREER to 1.0)
            ZodiacElement.AIR -> mapOf(FortuneCategory.CAREER to 2.0, FortuneCategory.LOVE to 2.0)
            ZodiacElement.WATER -> mapOf(FortuneCategory.LOVE to 4.0, FortuneCategory.HEALTH to -1.0)
        }

        val scores = mutableMapOf<FortuneCategory, Int>()
        val texts = mutableMapOf<FortuneCategory, String>()
        for (category in FortuneCategory.entries) {
            val value = clampRound(score + rng.gaussian(0.0, 10.0) + (elementBias[category] ?: 0.0), 30, 99)
            scores[category] = value
            val pool = when (category) {
                FortuneCategory.LOVE -> content.love
                FortuneCategory.CAREER -> content.career
                FortuneCategory.WEALTH -> content.wealth
                FortuneCategory.HEALTH -> content.health
            }
            texts[category] = rng.pick(pool.textsFor(value))
        }

        val allColors = WuXing.entries.flatMap { it.luckyColors }
        val overview = rng.pick(content.overview.textsFor(score))
        val comment = rng.pick(content.comments)
        val luckyColor = rng.pick(allColors)
        val luckyNumber = rng.int(1..99)
        val matchSign = rng.pick(sign.trines + sign.sextiles)
        val cautionSign = rng.pick(sign.squares)
        return Horoscope(sign, period, rangeText, score, scores, texts, overview, comment, luckyColor, luckyNumber, matchSign, cautionSign, skyNote)
    }

    private fun boost(aspect: AspectKind): Double = when (aspect) {
        AspectKind.CONJUNCTION -> 6.0
        AspectKind.TRINE -> 7.0
        AspectKind.SEXTILE -> 4.0
        AspectKind.SEMI_SEXTILE -> 0.0
        AspectKind.QUINCUNX -> -2.0
        AspectKind.SQUARE -> -6.0
        AspectKind.OPPOSITION -> -3.0
    }
}
