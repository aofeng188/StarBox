package com.tianaofeng.xingxia.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestionClassifierTest {
    private fun kind(text: String) = QuestionClassifier.analyze(text).kind

    private fun assertKind(expected: QuestionKind, vararg texts: String) {
        for (text in texts) assertEquals(expected, kind(text), text)
    }

    @Test
    fun food() {
        assertKind(QuestionKind.FOOD, "中午吃什么", "外卖点啥", "吃火锅还是吃烧烤", "喝奶茶还是咖啡？", "晚饭吃面、米饭、饺子还是包子")

        val noOptions = QuestionClassifier.analyze("中午吃什么")
        assertEquals(emptyList(), noOptions.options)
        assertEquals(listOf(AskMethod.FOOD, AskMethod.WHEEL, AskMethod.FINGER), noOptions.methods)

        val two = QuestionClassifier.analyze("喝奶茶还是咖啡？")
        assertEquals(listOf("喝奶茶", "咖啡"), two.options)
        assertEquals(listOf(AskMethod.WHEEL, AskMethod.FOOD, AskMethod.COIN), two.methods)

        val four = QuestionClassifier.analyze("晚饭吃面、米饭、饺子还是包子")
        assertEquals(listOf("晚饭吃面", "米饭", "饺子", "包子"), four.options)
        assertEquals(listOf(AskMethod.WHEEL, AskMethod.FOOD, AskMethod.DICE), four.methods)
    }

    @Test
    fun foodNeedsAChoice() {
        // 有吃的字眼但不是选择：「什么时候」不算「什么」
        assertKind(QuestionKind.TIMING, "什么时候去吃饭")
        assertKind(QuestionKind.YES_NO, "今天吃火锅吗", "要不要点外卖")
        assertKind(QuestionKind.GENERAL, "最近饭量变大了")
    }

    @Test
    fun choice() {
        assertKind(QuestionKind.CHOICE, "火锅还是烧烤", "周末去哪玩", "选哪个offer", "买红色，蓝色，白色", "考研或者工作")

        val hotpot = QuestionClassifier.analyze("火锅还是烧烤")
        assertEquals(listOf("火锅", "烧烤"), hotpot.options)
        assertEquals(listOf(AskMethod.WHEEL, AskMethod.COIN, AskMethod.FINGER), hotpot.methods)

        val colors = QuestionClassifier.analyze("买红色，蓝色，白色")
        assertEquals(listOf("买红色", "蓝色", "白色"), colors.options)
        assertEquals(listOf(AskMethod.WHEEL, AskMethod.DICE, AskMethod.FINGER), colors.methods)

        val noOptions = QuestionClassifier.analyze("周末去哪玩")
        assertEquals(emptyList(), noOptions.options)
        assertEquals(listOf(AskMethod.WHEEL, AskMethod.FINGER, AskMethod.DICE), noOptions.methods)

        // 「还是」本身就是选择，排在感情、时间之前
        val pair = QuestionClassifier.analyze("他还是她")
        assertEquals(QuestionKind.CHOICE, pair.kind)
        assertEquals(listOf("他", "她"), pair.options)
        assertKind(QuestionKind.CHOICE, "今天搬还是明天搬")
    }

    @Test
    fun punctuationOnlySplitIsWeak() {
        // 只靠标点拆出的两段遇到时间、感情、是非关键词时不算多选一
        val rain = QuestionClassifier.analyze("下雨了，要不要去跑步")
        assertEquals(QuestionKind.YES_NO, rain.kind)
        assertEquals(emptyList(), rain.options)
        assertKind(QuestionKind.RELATIONSHIP, "她，会回来吗")
        assertKind(QuestionKind.TIMING, "搬家，下周六")
    }

    @Test
    fun timing() {
        assertKind(QuestionKind.TIMING, "哪天搬家好", "什么时候换工作", "下个月结婚合适吗", "开业选什么日子", "现在是表白的时机吗")
        val analysis = QuestionClassifier.analyze("哪天搬家好")
        assertEquals(emptyList(), analysis.options)
        assertEquals(listOf(AskMethod.STICK, AskMethod.AUSPICIOUS, AskMethod.TAROT_TIME), analysis.methods)
    }

    @Test
    fun relationship() {
        assertKind(QuestionKind.RELATIONSHIP, "他喜欢我吗", "我们会复合吗", "要不要表白", "这段感情能走多远")
        val analysis = QuestionClassifier.analyze("他喜欢我吗")
        assertEquals(emptyList(), analysis.options)
        assertEquals(listOf(AskMethod.TAROT_LOVE, AskMethod.COMPATIBILITY, AskMethod.ANSWERS), analysis.methods)
    }

    @Test
    fun yesNo() {
        assertKind(QuestionKind.YES_NO, "明天要不要去面试", "今天适合出门吗？", "能不能买这个包", "明天会下雨吗?", "该不该辞职", "这次考试能过吗")
        val analysis = QuestionClassifier.analyze("明天要不要去面试")
        assertEquals(emptyList(), analysis.options)
        assertEquals(listOf(AskMethod.COIN, AskMethod.ANSWERS, AskMethod.TAROT_SINGLE), analysis.methods)
    }

    @Test
    fun general() {
        assertKind(QuestionKind.GENERAL, "今天运气怎么样", "最近工作顺利", "给我一点建议", "", "   ")
        val analysis = QuestionClassifier.analyze("今天运气怎么样")
        assertEquals(emptyList(), analysis.options)
        assertEquals(listOf(AskMethod.ANSWERS, AskMethod.STICK, AskMethod.COIN), analysis.methods)
    }

    @Test
    fun splitOptions() {
        // 两端的空白（含全角空格）和问号、叹号、句号、省略号、波浪号都去掉
        assertEquals(listOf("火锅", "烧烤"), QuestionClassifier.splitOptions("火锅 还是 烧烤？"))
        assertEquals(listOf("火锅", "烧烤"), QuestionClassifier.splitOptions("火锅　或者　烧烤！"))
        assertEquals(listOf("火锅", "烧烤"), QuestionClassifier.splitOptions("火锅…还是烧烤～"))
        assertEquals(listOf("A", "B", "C", "D"), QuestionClassifier.splitOptions("A,B，C、D"))
        // 空段和重复段丢掉，保留先出现的
        assertEquals(listOf("猫", "狗"), QuestionClassifier.splitOptions("猫、、狗，猫,"))
        assertEquals(listOf("今天运气怎么样"), QuestionClassifier.splitOptions("今天运气怎么样"))
        assertEquals(emptyList(), QuestionClassifier.splitOptions(""))
        assertEquals(emptyList(), QuestionClassifier.splitOptions("？、！"))
        // 最多 8 个
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "7", "8"), QuestionClassifier.splitOptions("1,2,3,4,5,6,7,8,9,10"))
        // 首尾空白在判断前去掉
        val analysis = QuestionClassifier.analyze("  火锅还是烧烤？  ")
        assertEquals(QuestionKind.CHOICE, analysis.kind)
        assertEquals(listOf("火锅", "烧烤"), analysis.options)
    }

    @Test
    fun optionsOnlyForChoiceAndFood() {
        // 选项去重后不足 2 个时不带选项
        val same = QuestionClassifier.analyze("苹果还是苹果")
        assertEquals(QuestionKind.CHOICE, same.kind)
        assertEquals(emptyList(), same.options)
        assertEquals(listOf(AskMethod.WHEEL, AskMethod.FINGER, AskMethod.DICE), same.methods)
        val many = QuestionClassifier.analyze("a,b,c,d,e,f,g,h,i")
        assertEquals(QuestionKind.CHOICE, many.kind)
        assertEquals(8, many.options.size)
        for (text in listOf("哪天搬家好", "他喜欢我吗", "明天要不要去面试", "今天运气怎么样", "下雨了，要不要去跑步")) {
            assertTrue(QuestionClassifier.analyze(text).options.isEmpty(), text)
        }
        // 每类都固定推荐 3 个方法
        for (text in listOf("中午吃什么", "火锅还是烧烤", "哪天搬家好", "他喜欢我吗", "明天要不要去面试", "今天运气怎么样")) {
            assertEquals(3, QuestionClassifier.analyze(text).methods.size, text)
        }
    }

    @Test
    fun keysMatchIos() {
        assertEquals(listOf("yesNo", "choice", "food", "timing", "relationship", "general"), QuestionKind.entries.map { it.key })
        assertEquals(
            listOf("coin", "answers", "tarotSingle", "tarotTime", "tarotLove", "wheel", "food", "finger", "stick", "auspicious", "compatibility", "dice"),
            AskMethod.entries.map { it.key },
        )
        assertEquals(QuestionKind.YES_NO, QuestionKind.of("yesNo"))
        assertEquals(AskMethod.TAROT_LOVE, AskMethod.of("tarotLove"))
        assertNull(QuestionKind.of("unknown"))
        assertNull(AskMethod.of(""))
    }
}
