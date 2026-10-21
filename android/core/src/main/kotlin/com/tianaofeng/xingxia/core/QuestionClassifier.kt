package com.tianaofeng.xingxia.core

/** 「问一问」判断出的问题类型；key 与 iOS 的 rawValue 相同 */
enum class QuestionKind(val key: String) {
    YES_NO("yesNo"),
    CHOICE("choice"),
    FOOD("food"),
    TIMING("timing"),
    RELATIONSHIP("relationship"),
    GENERAL("general");

    companion object {
        fun of(key: String): QuestionKind? = entries.firstOrNull { it.key == key }
    }
}

/** 推荐的方法，App 端负责映射到具体页面；key 与 iOS 的 rawValue 相同 */
enum class AskMethod(val key: String) {
    COIN("coin"),
    ANSWERS("answers"),
    TAROT_SINGLE("tarotSingle"),
    TAROT_TIME("tarotTime"),
    TAROT_LOVE("tarotLove"),
    WHEEL("wheel"),
    FOOD("food"),
    FINGER("finger"),
    STICK("stick"),
    AUSPICIOUS("auspicious"),
    COMPATIBILITY("compatibility"),
    DICE("dice");

    companion object {
        fun of(key: String): AskMethod? = entries.firstOrNull { it.key == key }
    }
}

data class QuestionAnalysis(
    val kind: QuestionKind,
    /** 「A 还是 B」拆出的选项，最多 8 个；只有 CHOICE、FOOD 两类带选项，且至少 2 个，否则为空 */
    val options: List<String>,
    /** 推荐顺序，固定 3 个 */
    val methods: List<AskMethod>,
)

/**
 * 离线关键词规则，按顺序判断，先命中先用。与 iOS 版 Shared/Ask/QuestionClassifier.swift 逐条一致：
 *
 * 0. 问题先去掉首尾空白。拆选项：把「还是」「或者」当分隔，再按「、」「,」「，」切开；
 *    每段去掉两端的空白（含全角空格）和「？?！!。.…～~」，丢掉空段和重复段（保留先出现的），最多 8 段。
 * 1. FOOD：含「吃|喝|外卖|饭|菜」，且是选择——满足第 2 条的条件，或（去掉「什么时候」后）含「什么」「啥」。
 * 2. CHOICE：含「还是|或者|选哪|哪个|哪一个|选谁|去哪」；或拆出 ≥ 2 段，且不含第 3、4、5 条的关键词
 *    （只靠标点拆出来的两段是弱信号：「下雨了，要不要去跑步」仍按是非题算）。
 * 3. TIMING：含「哪天|什么时候|何时|日子|时机|吉日|搬家|开业|结婚」。
 * 4. RELATIONSHIP：含「喜欢|感情|恋爱|复合|表白|对象|他|她|缘分」。
 * 5. YES_NO：含「要不要|能不能|该不该|可不可以|是否|会不会|行不行|好不好」，或以「吗」「吗？」「吗?」结尾。
 * 6. GENERAL：其余。
 *
 * 推荐方法（2 段选项时用硬币，更多时用骰子）：
 * - FOOD：有选项 → 转盘、今天吃什么、硬币/骰子；没有 → 今天吃什么、转盘、指尖轮盘
 * - CHOICE：有选项 → 转盘、硬币/骰子、指尖轮盘；没有 → 转盘、指尖轮盘、骰子
 * - TIMING：求签、择吉日、塔罗时间之流
 * - RELATIONSHIP：塔罗情感关系、缘分配对、答案之书
 * - YES_NO：硬币、答案之书、塔罗单张
 * - GENERAL：答案之书、求签、硬币
 */
object QuestionClassifier {
    const val MAX_OPTIONS = 8

    val foodWords = listOf("吃", "喝", "外卖", "饭", "菜")
    val choiceWords = listOf("还是", "或者", "选哪", "哪个", "哪一个", "选谁", "去哪")
    val timingWords = listOf("哪天", "什么时候", "何时", "日子", "时机", "吉日", "搬家", "开业", "结婚")
    val relationshipWords = listOf("喜欢", "感情", "恋爱", "复合", "表白", "对象", "他", "她", "缘分")
    val yesNoWords = listOf("要不要", "能不能", "该不该", "可不可以", "是否", "会不会", "行不行", "好不好")
    val yesNoEndings = listOf("吗", "吗？", "吗?")
    val whatWords = listOf("什么", "啥")

    /** 拆选项：这两个词先换成「、」，再按标点切 */
    val separatorWords = listOf("还是", "或者")
    val separatorMarks = setOf('、', ',', '，')

    /** 选项两端去掉的字符 */
    private const val OPTION_TRIM = " \t\r\n　？?！!。.…～~"

    fun analyze(text: String): QuestionAnalysis {
        val question = text.trim()
        val split = splitOptions(question)

        val isTiming = containsAny(question, timingWords)
        val isRelationship = containsAny(question, relationshipWords)
        val isYesNo = containsAny(question, yesNoWords) || yesNoEndings.any { question.endsWith(it) }
        val isChoice = containsAny(question, choiceWords) ||
            (split.size >= 2 && !isTiming && !isRelationship && !isYesNo)
        val asksWhat = containsAny(question.replace("什么时候", ""), whatWords)
        val options = if (split.size >= 2) split else emptyList()

        if (containsAny(question, foodWords) && (isChoice || asksWhat)) {
            val methods = if (options.isEmpty()) {
                listOf(AskMethod.FOOD, AskMethod.WHEEL, AskMethod.FINGER)
            } else {
                listOf(AskMethod.WHEEL, AskMethod.FOOD, if (options.size == 2) AskMethod.COIN else AskMethod.DICE)
            }
            return QuestionAnalysis(QuestionKind.FOOD, options, methods)
        }
        if (isChoice) {
            val methods = if (options.isEmpty()) {
                listOf(AskMethod.WHEEL, AskMethod.FINGER, AskMethod.DICE)
            } else {
                listOf(AskMethod.WHEEL, if (options.size == 2) AskMethod.COIN else AskMethod.DICE, AskMethod.FINGER)
            }
            return QuestionAnalysis(QuestionKind.CHOICE, options, methods)
        }
        if (isTiming) {
            return QuestionAnalysis(QuestionKind.TIMING, emptyList(), listOf(AskMethod.STICK, AskMethod.AUSPICIOUS, AskMethod.TAROT_TIME))
        }
        if (isRelationship) {
            return QuestionAnalysis(QuestionKind.RELATIONSHIP, emptyList(), listOf(AskMethod.TAROT_LOVE, AskMethod.COMPATIBILITY, AskMethod.ANSWERS))
        }
        if (isYesNo) {
            return QuestionAnalysis(QuestionKind.YES_NO, emptyList(), listOf(AskMethod.COIN, AskMethod.ANSWERS, AskMethod.TAROT_SINGLE))
        }
        return QuestionAnalysis(QuestionKind.GENERAL, emptyList(), listOf(AskMethod.ANSWERS, AskMethod.STICK, AskMethod.COIN))
    }

    /** 按「还是 / 或者 / 、 / , / ，」拆出的选项（未判断类型，可能只有 0 或 1 段） */
    fun splitOptions(text: String): List<String> {
        var unified = text
        for (word in separatorWords) {
            unified = unified.replace(word, "、")
        }
        val result = mutableListOf<String>()
        for (part in unified.split(*separatorMarks.toCharArray())) {
            val option = part.trim { it in OPTION_TRIM }
            if (option.isEmpty() || option in result) continue
            result.add(option)
            if (result.size == MAX_OPTIONS) break
        }
        return result
    }

    private fun containsAny(text: String, words: List<String>): Boolean = words.any { text.contains(it) }
}
