package com.tianaofeng.xingxia.core

import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JournalStatsTest {
    private fun millis(raw: Int, hour: Int = 12, minute: Int = 0): Long =
        LocalDateTime.of(raw / 10000, raw / 100 % 100, raw % 100, hour, minute)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun sample(
        tool: String,
        raw: Int,
        result: String = "",
        question: String? = null,
        adopted: Boolean? = null,
        satisfaction: Int? = null,
        hour: Int = 12,
    ) = DecisionSample(tool, result, question, millis(raw, hour), adopted, satisfaction)

    private fun keywords(vararg texts: String) = JournalStats.keywords(texts.toList()).map { it.name to it.count }

    @Test
    fun keywordCases() {
        // 与 iOS 规则的原型算出的结果一致
        assertEquals(
            listOf("火锅" to 4, "报名半马" to 2, "kfc" to 2, "爬山" to 2, "电影" to 2),
            keywords(
                "火锅还是烧烤", "火锅", "午饭吃什么", "麻辣烫", "今天吃火锅吗", "火锅", "要不要报名半马", "报名半马好不好",
                "时机未到，再等等", "明天去不去爬山", "爬山还是看电影", "看电影", "KFC还是麦当劳", "kfc", "他喜欢我吗", "表白会成功吗",
            ),
        )
        assertEquals(listOf("辞职" to 2), keywords("要不要辞职", "辞职还是继续干", "时机未到，再等等"))
        assertEquals(listOf("下雨" to 2), keywords("明天会下雨吗", "明天会下雨吗"))
        assertEquals(listOf("晚饭" to 2), keywords("今天吃什么", "今天吃什么", "晚饭吃什么", "晚饭吃什么"))
        assertEquals(listOf("运气" to 1), keywords("今天运气怎么样"))
        assertEquals(listOf("周末" to 3), keywords("周末去哪玩", "周末去哪儿", "周末还是下周"))
        assertEquals(emptyList(), keywords("今天吃什么"))
        assertEquals(emptyList(), keywords())
    }

    @Test
    fun keywordCandidates() {
        // 停用词挖掉并断开，首尾是虚字的片段丢掉，英文转小写
        assertEquals(listOf("下雨"), JournalStats.keywordCandidates("明天会下雨吗"))
        assertEquals(listOf("火锅"), JournalStats.keywordCandidates("吃火锅"))
        assertEquals(listOf("kfc"), JournalStats.keywordCandidates("KFC!"))
        // 单个字母、数字不算
        assertEquals(emptyList(), JournalStats.keywordCandidates("A 1 2026"))
        // 同一段文本里重复的片段只算一次
        assertEquals(listOf("火锅" to 1), keywords("火锅，火锅，火锅"))
        // 最多 5 个；互相包含的只留排在前面的
        val many = JournalStats.keywords(listOf("苹果香蕉", "苹果香蕉", "橘子", "橘子", "西瓜", "西瓜", "葡萄", "葡萄", "芒果", "芒果", "榴莲", "榴莲"))
        assertEquals(5, many.size)
        assertEquals("苹果香蕉", many.first().name)
        assertTrue(many.none { it.name == "苹果" || it.name == "香蕉" })
    }

    @Test
    fun keywordTexts() {
        val samples = listOf(
            sample("wheel", 20260302, result = "火锅", question = "火锅还是烧烤"),
            sample("coin", 20260301, result = "正面", question = "要不要辞职"),
            sample("food", 20260303, result = "麻辣烫"),
            sample("stick", 20260304, result = "第 8 签", question = "今年运势"),
            sample("answers", 20260305, result = "再等等", question = "再等等"),
        )
        // 按时间正序；硬币的结果不取；占卜不算；结果和问题相同时只取一次
        assertEquals(listOf("要不要辞职", "火锅还是烧烤", "火锅", "麻辣烫", "再等等"), JournalStats.keywordTexts(samples))
    }

    @Test
    fun askedQuestionAndKind() {
        assertEquals("去哪吃", JournalStats.askedQuestion("wheel", "转盘", " 去哪吃 "))
        assertEquals("要不要辞职", JournalStats.askedQuestion("coin", "要不要辞职", null))
        assertEquals("会成功吗", JournalStats.askedQuestion("answers", "会成功吗", "  "))
        assertNull(JournalStats.askedQuestion("wheel", "午饭", null))
        assertNull(JournalStats.askedQuestion("coin", " ", null))

        val sample = DecisionSample.of("coin", "明天要不要去面试", "正面", null, millis(20261020))
        assertEquals("明天要不要去面试", sample.question)
        assertEquals(DayKey(2026, 10, 20), sample.day)

        assertEquals(QuestionKind.YES_NO, JournalStats.questionKind("coin", "明天要不要去面试"))
        assertEquals(QuestionKind.CHOICE, JournalStats.questionKind("food", "火锅还是烧烤"))
        assertEquals(QuestionKind.FOOD, JournalStats.questionKind("food", null))
        assertEquals(QuestionKind.FOOD, JournalStats.questionKind("nearby", " "))
        assertNull(JournalStats.questionKind("wheel", null))
    }

    @Test
    fun tallyAndPercent() {
        val samples = listOf(
            sample("wheel", 20260301, adopted = true, satisfaction = 1),
            sample("coin", 20260301, adopted = true, satisfaction = 0),
            sample("food", 20260302, adopted = false),
            sample("dice", 20260302, adopted = true, satisfaction = -1),
            sample("lottery", 20260303),
            sample("stick", 20260303, adopted = true, satisfaction = 1),
            sample("tarot", 20260303),
            sample("rps", 20260304, adopted = true),
            sample("scratch", 20260304),
            sample("jiaobei", 20260304),
        )
        val tally = JournalStats.tally(samples)
        assertEquals(DecisionTally(decisions = 5, adopted = 3, reconsidered = 1, rated = 3, satisfied = 1, neutral = 1, regretted = 1), tally)
        assertEquals(60, tally.adoptionRate)
        assertEquals(33, tally.satisfactionRate)
        assertNull(DecisionTally().adoptionRate)
        assertNull(DecisionTally().satisfactionRate)

        assertEquals(50, JournalStats.percent(1, 2))
        assertEquals(67, JournalStats.percent(2, 3))
        // 0.5 进一
        assertEquals(13, JournalStats.percent(1, 8))
        assertEquals(100, JournalStats.percent(3, 3))
        assertNull(JournalStats.percent(0, 0))
        assertTrue(JournalStats.isDecision("wheel"))
        assertTrue(!JournalStats.isDecision("tarot"))
    }

    @Test
    fun ranked() {
        assertEquals(
            listOf(NameCount("b", 3), NameCount("a", 2), NameCount("c", 2), NameCount("d", 1)),
            JournalStats.ranked(listOf("a", "b", "c", "b", "a", "b", "c", "d")),
        )
        assertEquals(emptyList(), JournalStats.ranked(emptyList()))
    }

    @Test
    fun bestDay() {
        val scores = mapOf(20260301 to 70, 20260302 to 88, 20260303 to 88, 20260304 to 60)
        val best = JournalStats.bestDay(scores.keys) { scores.getValue(it.rawValue) }
        // 同分取较早的
        assertEquals(DayScore(DayKey(2026, 3, 2), 88), best)
        assertNull(JournalStats.bestDay(emptySet()) { 0 })
        // 无效日期跳过
        assertEquals(DayScore(DayKey(2026, 3, 1), 70), JournalStats.bestDay(setOf(0, 20261340, 20260301)) { 70 })
    }

    @Test
    fun monthly() {
        val decisions = listOf(
            sample("coin", 20260305, question = "要不要辞职", adopted = true, satisfaction = 1),
            sample("wheel", 20260302, result = "火锅", question = "火锅还是烧烤", adopted = true, satisfaction = 0),
            sample("food", 20260302, result = "麻辣烫", adopted = false, hour = 18),
            sample("coin", 20260306, question = "明天会下雨吗"),
            sample("stick", 20260310),
            sample("stick", 20260311),
            sample("stick", 20260312),
        )
        val moods = listOf(MoodSample(20260302, 4), MoodSample(20260303, 5), MoodSample(20260320, 2))
        val scores = mapOf(20260302 to 60, 20260303 to 75, 20260305 to 90, 20260306 to 55, 20260310 to 90, 20260311 to 40, 20260312 to 30, 20260320 to 85, 20260325 to 99)
        val review = JournalStats.monthly(decisions, moods, setOf(20260325)) { scores.getValue(it.rawValue) }

        assertEquals(4, review.tally.decisions)
        assertEquals(2, review.tally.adopted)
        assertEquals(1, review.tally.reconsidered)
        assertEquals(50, review.tally.satisfactionRate)
        // 求签次数最多（含占卜）
        assertEquals(NameCount("stick", 3), review.topTool)
        // 是非题两次（要不要辞职、明天会下雨吗），多选一和吃什么各一次
        assertEquals(QuestionKind.YES_NO, review.topKind)
        assertEquals(2, review.topKindCount)
        assertEquals(11.0 / 3, review.moodAverage!!, 1e-9)
        assertEquals(3, review.moodDays)
        assertEquals(DayScore(DayKey(2026, 3, 25), 99), review.bestDay)
        assertEquals(9, review.recordDays)
        assertTrue(!review.isEmpty)

        val empty = JournalStats.monthly(emptyList(), emptyList(), emptySet()) { 50 }
        assertTrue(empty.isEmpty)
        assertNull(empty.topTool)
        assertNull(empty.topKind)
        assertEquals(0, empty.topKindCount)
        assertNull(empty.moodAverage)
        assertNull(empty.bestDay)
    }

    @Test
    fun monthlyKindTieGoesToFirst() {
        // 次数相同，先出现的类型在前；没写问题的吃什么也算
        val decisions = listOf(
            sample("food", 20260301, hour = 9),
            sample("coin", 20260301, question = "要不要辞职", hour = 10),
            sample("wheel", 20260301, hour = 11),
        )
        val review = JournalStats.monthly(decisions, emptyList(), emptySet()) { 50 }
        assertEquals(QuestionKind.FOOD, review.topKind)
        assertEquals(1, review.topKindCount)
        assertEquals(NameCount("food", 1), review.topTool)
    }

    @Test
    fun annual() {
        val decisions = listOf(
            sample("wheel", 20260115, result = "火锅", question = "火锅还是烧烤", adopted = true, satisfaction = 1),
            sample("food", 20260220, result = "火锅", adopted = true),
            sample("coin", 20260301, question = "要不要辞职", adopted = false),
            sample("coin", 20260302, question = "辞职还是继续干"),
            sample("stick", 20260401),
            sample("rps", 20260402),
        )
        val input = AnnualInput(
            year = 2026,
            decisions = decisions,
            moods = listOf(MoodSample(20260110, 4), MoodSample(20260112, 5), MoodSample(20260305, 2)),
            sticks = listOf(StickSample(20260401, "上上"), StickSample(20260501, "中平"), StickSample(20260502, "上上")),
            tarotDays = listOf(20260601, 20260110),
            foods = listOf("火锅", "", "麻辣烫", "火锅", "寿司", "面条"),
        )
        val review = JournalStats.annual(input) { if (it.rawValue == 20260501) 96 else it.day + 50 }

        assertEquals(2026, review.year)
        assertEquals(listOf(NameCount("火锅", 3), NameCount("辞职", 2)), review.keywords)
        assertEquals(4, review.tally.decisions)
        assertEquals(2, review.tally.adopted)
        assertEquals(50, review.tally.adoptionRate)
        assertEquals(100, review.tally.satisfactionRate)
        assertEquals(listOf(NameCount("火锅", 2), NameCount("麻辣烫", 1), NameCount("寿司", 1)), review.topFoods)
        assertEquals(listOf(MonthMood(1, 4.5, 2), MonthMood(3, 2.0, 1)), review.moodByMonth)
        assertEquals(11.0 / 3, review.moodAverage!!, 1e-9)
        assertEquals(3, review.moodDays)
        assertEquals(DayScore(DayKey(2026, 5, 1), 96), review.luckiestDay)
        assertEquals(
            listOf(NameCount("上上", 2), NameCount("上吉", 0), NameCount("中吉", 0), NameCount("中平", 1), NameCount("下", 0)),
            review.stickLevels,
        )
        assertEquals(3, review.stickCount)
        assertEquals(listOf(NameCount("coin", 2), NameCount("wheel", 1), NameCount("food", 1)), review.topTools)
        // 1/10 1/12 1/15 2/20 3/1 3/2 3/5 4/1 4/2 5/1 5/2 6/1
        assertEquals(12, review.recordDays)

        val empty = JournalStats.annual(AnnualInput(2026, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())) { 50 }
        assertTrue(empty.isEmpty)
        assertEquals(5, empty.stickLevels.size)
        assertTrue(empty.stickLevels.all { it.count == 0 })
        assertNull(empty.luckiestDay)
        assertNull(empty.moodAverage)
        assertEquals(emptyList(), empty.keywords)
    }
}
