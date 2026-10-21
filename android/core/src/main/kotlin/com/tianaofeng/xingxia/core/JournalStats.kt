package com.tianaofeng.xingxia.core

import java.time.Instant

// 日记的统计：本月回顾、年度回顾、年度关键词。都是纯函数，输入记录的值拷贝，输出统计结构，可以在后台线程算。
// 与 iOS 版 Shared/Journal/JournalStats.swift 逐条一致，两端同一份数据算出的结果相同。
//
// 名词：
// - 「决定」：tool 不是占卜类（stick 求签、tarot 塔罗、jiaobei 掷筊）也不是游戏类（rps 猜拳、scratch 刮刮乐）的记录。
//   占卜在日记里单独成行，游戏只是玩，都不算决定。
// - 「照做」：adopted == true；「回访」：satisfaction != null；满意率 = satisfaction == 1 的条数 / 回访条数。
// - 百分比一律四舍五入到整数（0.5 进一）；分母为 0 时为 null，界面上不显示。
// - 「问题」：JournalStats.askedQuestion（question；没有时硬币 coin、答案之书 answers 用 title）。
// - 「最纠结的问题类型」：决定的问题用 QuestionClassifier 分类后出现最多的类型；没写问题的「今天吃什么」「附近餐厅」算吃什么，其余不计。
// - 「有记录的日子」：当天有任意决策记录（含占卜和游戏）、心情、每日一签或塔罗。
// - 「运势最高 / 最幸运的一天」：有记录的日子里，当前档案运势分数最高的一天；同分取较早的。
// - 排名（最常用的工具、最纠结的类型、最常吃的菜）：次数多的在前；次数相同，先出现的在前（记录按时间正序）。

/** 一条决策记录的值拷贝；question 是 JournalStats.askedQuestion 的结果，用 DecisionSample.of 构造时自动换算 */
data class DecisionSample(
    val tool: String,
    val result: String,
    val question: String?,
    /** 毫秒时间戳 */
    val date: Long,
    val adopted: Boolean? = null,
    val satisfaction: Int? = null,
) {
    /** 记录所在的那天（本地时区） */
    val day: DayKey get() = DayKey.of(Instant.ofEpochMilli(date))

    companion object {
        /** 从决策记录的字段构造，question 按 askedQuestion 的规则取 */
        fun of(
            tool: String,
            title: String,
            result: String,
            question: String?,
            date: Long,
            adopted: Boolean? = null,
            satisfaction: Int? = null,
        ) = DecisionSample(tool, result, JournalStats.askedQuestion(tool, title, question), date, adopted, satisfaction)
    }
}

/** 一天的心情（MoodEntry 的值拷贝） */
data class MoodSample(
    /** yyyyMMdd */
    val dayKey: Int,
    /** 1…5 */
    val mood: Int,
)

/** 一次每日一签：yyyyMMdd 和签的等级（「上上」「上吉」「中吉」「中平」「下」） */
data class StickSample(val dayKey: Int, val level: String)

data class NameCount(val name: String, val count: Int) {
    val id: String get() = name
}

data class DayScore(val day: DayKey, val score: Int)

/** 某个月的平均心情 */
data class MonthMood(
    /** 1…12 */
    val month: Int,
    val average: Double,
    /** 这个月打卡的天数 */
    val days: Int,
) {
    val id: Int get() = month
}

/** 决定的数量、照做和回访 */
data class DecisionTally(
    val decisions: Int = 0,
    /** 「就这么办」 */
    val adopted: Int = 0,
    /** 「再想想」 */
    val reconsidered: Int = 0,
    /** 回访过（满意 / 一般 / 后悔） */
    val rated: Int = 0,
    val satisfied: Int = 0,
    val neutral: Int = 0,
    val regretted: Int = 0,
) {
    /** 照做比例 = 照做 / 决定 */
    val adoptionRate: Int? get() = JournalStats.percent(adopted, decisions)

    /** 满意率 = 满意 / 回访 */
    val satisfactionRate: Int? get() = JournalStats.percent(satisfied, rated)
}

/** 本月回顾 */
data class MonthlyReview(
    val tally: DecisionTally = DecisionTally(),
    /** 最常用的工具（tool 键，含占卜和游戏） */
    val topTool: NameCount? = null,
    /** 最纠结的问题类型（只看决定） */
    val topKind: QuestionKind? = null,
    val topKindCount: Int = 0,
    /** 平均心情 1…5 */
    val moodAverage: Double? = null,
    val moodDays: Int = 0,
    /** 有记录的日子里运势分数最高的一天 */
    val bestDay: DayScore? = null,
    val recordDays: Int = 0,
) {
    val isEmpty: Boolean get() = recordDays == 0
}

/** 年度回顾的输入 */
data class AnnualInput(
    val year: Int,
    /** 这一年的全部决策记录（含占卜和游戏） */
    val decisions: List<DecisionSample>,
    val moods: List<MoodSample>,
    /** 每日一签 */
    val sticks: List<StickSample>,
    /** 塔罗占卜的日子 yyyyMMdd */
    val tarotDays: List<Int>,
    /** 「就吃这个」的菜名（FoodLog.name），按时间正序 */
    val foods: List<String>,
)

/** 年度回顾 */
data class AnnualReview(
    val year: Int,
    val keywords: List<NameCount> = emptyList(),
    val tally: DecisionTally = DecisionTally(),
    /** 最常吃的菜，最多 3 个 */
    val topFoods: List<NameCount> = emptyList(),
    /** 有心情记录的月份，按月份排 */
    val moodByMonth: List<MonthMood> = emptyList(),
    val moodAverage: Double? = null,
    val moodDays: Int = 0,
    val luckiestDay: DayScore? = null,
    /** 求签等级分布，固定按 上上、上吉、中吉、中平、下 排，没求到的等级 count 为 0 */
    val stickLevels: List<NameCount> = emptyList(),
    val stickCount: Int = 0,
    /** 最常用的工具（tool 键），最多 3 个 */
    val topTools: List<NameCount> = emptyList(),
    val recordDays: Int = 0,
) {
    val isEmpty: Boolean get() = recordDays == 0
}

object JournalStats {
    /** 占卜类：在日记里单独成行，不算「决定」 */
    val divinationTools = setOf("stick", "tarot", "jiaobei")

    /** 游戏类：只是玩，不算「决定」 */
    val gameTools = setOf("rps", "scratch")

    /** 签的等级，从好到差 */
    val stickLevelOrder = listOf("上上", "上吉", "中吉", "中平", "下")

    fun isDecision(tool: String): Boolean = tool !in divinationTools && tool !in gameTools

    /** 四舍五入的百分比；分母为 0 时为 null */
    fun percent(part: Int, total: Int): Int? {
        if (total <= 0) return null
        return (part * 100.0 / total).roundHalfAway().toInt()
    }

    /**
     * 当时在问的问题：「问一问」带过来的 question；没有时，硬币（coin）和答案之书（answers）的 title
     * 就是用户在页面里写的问题。都没有时为 null
     */
    fun askedQuestion(tool: String, title: String, question: String?): String? {
        val asked = question?.trim()
        if (!asked.isNullOrEmpty()) return asked
        if (tool != "coin" && tool != "answers") return null
        val written = title.trim()
        return written.ifEmpty { null }
    }

    /** 一条决定在问哪类事：有问题按 QuestionClassifier 判断；没写问题时「今天吃什么」「附近餐厅」算吃什么，其余不计 */
    fun questionKind(tool: String, question: String?): QuestionKind? {
        val asked = question?.trim()
        if (!asked.isNullOrEmpty()) return QuestionClassifier.analyze(asked).kind
        return if (tool == "food" || tool == "nearby") QuestionKind.FOOD else null
    }

    /** 只统计决定（isDecision） */
    fun tally(samples: List<DecisionSample>): DecisionTally {
        var decisions = 0
        var adopted = 0
        var reconsidered = 0
        var satisfied = 0
        var neutral = 0
        var regretted = 0
        for (sample in samples) {
            if (!isDecision(sample.tool)) continue
            decisions += 1
            when (sample.adopted) {
                true -> adopted += 1
                false -> reconsidered += 1
                null -> {}
            }
            when (sample.satisfaction) {
                1 -> satisfied += 1
                0 -> neutral += 1
                -1 -> regretted += 1
            }
        }
        return DecisionTally(
            decisions = decisions,
            adopted = adopted,
            reconsidered = reconsidered,
            rated = satisfied + neutral + regretted,
            satisfied = satisfied,
            neutral = neutral,
            regretted = regretted,
        )
    }

    /** 排名：次数多的在前，次数相同先出现的在前 */
    fun ranked(names: List<String>): List<NameCount> {
        // LinkedHashMap 保留首次出现的顺序，稳定排序后同次数的仍按出现先后
        val counts = LinkedHashMap<String, Int>()
        for (name in names) counts[name] = (counts[name] ?: 0) + 1
        return counts.entries.sortedByDescending { it.value }.map { NameCount(it.key, it.value) }
    }

    /** 有记录的日子（yyyyMMdd）里运势最高的一天；同分取较早的那天 */
    fun bestDay(days: Set<Int>, fortuneScore: (DayKey) -> Int): DayScore? {
        var best: DayScore? = null
        for (raw in days.sorted()) {
            val day = validDay(raw) ?: continue
            val score = fortuneScore(day)
            val current = best
            if (current != null && score <= current.score) continue
            best = DayScore(day, score)
        }
        return best
    }

    // 本月回顾

    /** decisions、moods 只传这个月的；extraDays 是这个月求过签、占过塔罗的日子 */
    fun monthly(
        decisions: List<DecisionSample>,
        moods: List<MoodSample>,
        extraDays: Set<Int>,
        fortuneScore: (DayKey) -> Int,
    ): MonthlyReview {
        val samples = decisions.sortedBy { it.date }
        val kinds = samples.filter { isDecision(it.tool) }.mapNotNull { questionKind(it.tool, it.question) }
        // 最纠结的类型和它出现的次数
        val topKind = ranked(kinds.map { it.key }).firstOrNull()?.let { top -> QuestionKind.of(top.name)?.let { it to top.count } }
        val days = samples.map { it.day.rawValue }.toSet() + moods.map { it.dayKey } + extraDays
        return MonthlyReview(
            tally = tally(samples),
            topTool = ranked(samples.map { it.tool }).firstOrNull(),
            topKind = topKind?.first,
            topKindCount = topKind?.second ?: 0,
            moodAverage = average(moods),
            moodDays = moods.size,
            bestDay = bestDay(days, fortuneScore),
            recordDays = days.size,
        )
    }

    // 年度回顾

    fun annual(input: AnnualInput, fortuneScore: (DayKey) -> Int): AnnualReview {
        val samples = input.decisions.sortedBy { it.date }
        val moodByMonth = input.moods
            .groupBy { it.dayKey / 100 % 100 }
            .toSortedMap()
            .map { (month, entries) -> MonthMood(month, entries.sumOf { it.mood }.toDouble() / entries.size, entries.size) }

        val levelCounts = input.sticks.groupingBy { it.level }.eachCount()
        val days = samples.map { it.day.rawValue }.toSet() +
            input.moods.map { it.dayKey } +
            input.sticks.map { it.dayKey } +
            input.tarotDays
        return AnnualReview(
            year = input.year,
            keywords = keywords(keywordTexts(samples)),
            tally = tally(samples),
            topFoods = ranked(input.foods.filter { it.isNotEmpty() }).take(3),
            moodByMonth = moodByMonth,
            moodAverage = average(input.moods),
            moodDays = input.moods.size,
            luckiestDay = bestDay(days, fortuneScore),
            stickLevels = stickLevelOrder.map { NameCount(it, levelCounts[it] ?: 0) },
            stickCount = input.sticks.size,
            topTools = ranked(samples.map { it.tool }).take(3),
            recordDays = days.size,
        )
    }

    // 年度关键词

    /** 关键词素材：按时间正序，每条决定先取问题，再取结果（只取 keywordResultTools 的结果；和问题相同时不重复取） */
    val keywordResultTools = setOf("food", "nearby", "wheel", "lottery", "answers")

    fun keywordTexts(samples: List<DecisionSample>): List<String> {
        val texts = mutableListOf<String>()
        for (sample in samples.sortedBy { it.date }) {
            if (!isDecision(sample.tool)) continue
            val question = sample.question?.trim() ?: ""
            if (question.isNotEmpty()) texts.add(question)
            val result = sample.result.trim()
            if (sample.tool in keywordResultTools && result.isNotEmpty() && result != question) texts.add(result)
        }
        return texts
    }

    /** 不当关键词的常用词 */
    val keywordStopwords = setOf(
        "今天", "明天", "昨天", "后天", "今晚", "现在", "时候", "什么", "怎么", "怎么样", "为什么", "要不要", "能不能",
        "该不该", "可不可以", "是不是", "会不会", "行不行", "好不好", "还是", "或者", "哪个", "哪一个", "一个", "一下",
        "一点", "我们", "你们", "他们", "自己", "这个", "那个", "就是", "没有", "不是", "可能", "应该", "是否", "不要",
        "还有", "的话", "然后", "但是", "因为", "所以", "如果", "已经", "正面", "反面", "选择", "决定",
    )

    /** 首字或末字是这些虚字的片段不当关键词（「吃火锅」只留「火锅」） */
    val keywordEdgeChars: Set<Char> =
        "的了吗呢吧啊呀么嘛哦呗和与或是在我你他她它就都也还要不会能很太把被给让这那哪谁啥什怎个些着过得地吃喝去来做买看想有没".toSet()

    /** 停用词按长度从长到短，切汉字段时先匹配长的 */
    private val stopwordsByLength: List<String> = keywordStopwords.sortedByDescending { it.length }

    /**
     * 年度关键词。规则（与 iOS 逐条一致）：
     * 1. 每段文本按字符切开：汉字（U+3400–4DBF、U+4E00–9FFF、U+F900–FAFF）连成汉字段，
     *    英文字母（A–Z、a–z）连成英文段，其他字符（标点、空白、数字等）都是分隔。
     *    汉字段再从左到右扫一遍，遇到停用词（同一位置先匹配长的）就把它挖掉、在那里断开：「明天会下雨吗」→「会下雨吗」。
     * 2. 候选片段，按这个顺序生成：英文段长度 ≥ 2 时整段转小写；汉字段长度 2–4 时整段，
     *    长度 ≥ 3 时再取长度从 min(4, 段长 - 1) 到 2 的子串（长的先，同长从左到右）。
     * 3. 丢掉停用词，以及首字或末字是虚字（keywordEdgeChars）的汉字片段。
     * 4. 同一段文本里重复的片段只算一次；片段的次数 = 含有它的文本段数。
     * 5. 排序：次数多的在前；次数相同，长的在前；再相同，先生成的在前。
     * 6. 依次挑选，跳过与已选关键词互相包含的片段，最多 limit 个，只挑次数 ≥ 2 的；
     *    一个都挑不出来时改挑次数为 1 的，最多 3 个。
     */
    fun keywords(texts: List<String>, limit: Int = 5): List<NameCount> {
        val counts = HashMap<String, Int>()
        val order = HashMap<String, Int>()
        for (text in texts) {
            val seen = HashSet<String>()
            for (candidate in keywordCandidates(text)) {
                if (!seen.add(candidate)) continue
                counts[candidate] = (counts[candidate] ?: 0) + 1
                if (candidate !in order) order[candidate] = order.size
            }
        }
        val sorted = counts.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }
                .thenByDescending { it.key.length }
                .thenBy { order[it.key] ?: 0 },
        )

        fun pick(minCount: Int, maxCount: Int): List<NameCount> {
            val picked = mutableListOf<NameCount>()
            for ((name, count) in sorted) {
                if (count < minCount) continue
                if (picked.any { it.name.contains(name) || name.contains(it.name) }) continue
                picked.add(NameCount(name, count))
                if (picked.size >= maxCount) break
            }
            return picked
        }

        val frequent = pick(2, limit)
        return frequent.ifEmpty { pick(1, minOf(3, limit)) }
    }

    /** 一段文本里的候选片段（已过滤，可能重复），顺序见 keywords 的第 2 条 */
    fun keywordCandidates(text: String): List<String> {
        val candidates = mutableListOf<String>()
        for (segment in segments(text)) {
            if (segment.isLatin) {
                if (segment.text.length < 2) continue
                val word = segment.text.lowercase()
                if (word !in keywordStopwords) candidates.add(word)
                continue
            }
            for (chars in cutStopwords(segment.text)) {
                val length = chars.length
                if (length in 2..4) candidates.add(chars)
                if (length >= 3) {
                    for (size in minOf(4, length - 1) downTo 2) {
                        for (start in 0..(length - size)) {
                            candidates.add(chars.substring(start, start + size))
                        }
                    }
                }
            }
        }
        return candidates.filter { candidate ->
            if (candidate.isEmpty() || candidate in keywordStopwords) return@filter false
            // 英文词不看虚字
            if (candidate.first().code < 0x80) return@filter true
            candidate.first() !in keywordEdgeChars && candidate.last() !in keywordEdgeChars
        }
    }

    /** 汉字段里出现停用词的地方挖掉并断开 */
    private fun cutStopwords(chars: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < chars.length) {
            val word = stopwordsByLength.firstOrNull { chars.startsWith(it, index) }
            if (word != null) {
                if (current.isNotEmpty()) parts.add(current.toString())
                current.clear()
                index += word.length
            } else {
                current.append(chars[index])
                index += 1
            }
        }
        if (current.isNotEmpty()) parts.add(current.toString())
        return parts
    }

    private class Segment(val isLatin: Boolean, val text: String)

    private fun isHan(codePoint: Int): Boolean =
        codePoint in 0x3400..0x4DBF || codePoint in 0x4E00..0x9FFF || codePoint in 0xF900..0xFAFF

    private fun isLatin(codePoint: Int): Boolean = codePoint in 0x41..0x5A || codePoint in 0x61..0x7A

    private fun segments(text: String): List<Segment> {
        val result = mutableListOf<Segment>()
        val current = StringBuilder()
        var currentLatin = false
        fun flush() {
            if (current.isNotEmpty()) result.add(Segment(currentLatin, current.toString()))
            current.clear()
        }
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            offset += Character.charCount(codePoint)
            val latin = isLatin(codePoint)
            if (!latin && !isHan(codePoint)) {
                flush()
                continue
            }
            if (current.isNotEmpty() && currentLatin != latin) flush()
            currentLatin = latin
            current.appendCodePoint(codePoint)
        }
        flush()
        return result
    }

    private fun average(moods: List<MoodSample>): Double? =
        if (moods.isEmpty()) null else moods.sumOf { it.mood }.toDouble() / moods.size

    /** yyyyMMdd 转 DayKey；不是有效日期时为 null */
    private fun validDay(raw: Int): DayKey? {
        val day = DayKey.fromRaw(raw) ?: return null
        return runCatching { day.localDate }.getOrNull()?.let { day }
    }
}
