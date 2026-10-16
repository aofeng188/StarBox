package com.tianaofeng.xingxia.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class BandedTexts(val high: List<String>, val mid: List<String>, val low: List<String>) {
    fun textsFor(score: Int): List<String> = if (score >= 78) high else if (score >= 55) mid else low
}

@Serializable
data class FortuneContent(
    val levels: List<Level>,
    val categories: Map<String, BandedTexts>,
    val relations: Map<String, String>,
    val branchNotes: Map<String, String>,
    val moonNotes: Map<String, String>,
    val funYi: List<String>,
    val funJi: List<String>,
    val quotes: List<String>,
    val items: List<String>,
    /** 季节性幸运物只在列出的公历月份出现；不在表里的全年可选（键可缺省） */
    val itemMonths: Map<String, List<Int>> = emptyMap(),
) {
    @Serializable
    data class Level(val min: Int, val name: String, val summaries: List<String>)

    /** 某个公历月份可选的幸运物，保持 items 原有顺序；过滤后为空时退回全部（与 iOS items(inMonth:) 一致） */
    fun itemsFor(month: Int): List<String> =
        items.filter { itemMonths[it]?.contains(month) ?: true }.ifEmpty { items }
}

@Serializable
data class HoroscopeContent(
    val overview: BandedTexts,
    val love: BandedTexts,
    val career: BandedTexts,
    val wealth: BandedTexts,
    val health: BandedTexts,
    val comments: List<String>,
)

/** 一支签 */
@Serializable
data class FortuneStick(
    val n: Int,
    val level: String,
    val title: String,
    val poem: List<String>,
    val meaning: String,
) {
    val numberText: String get() = "第${chineseNumber(n)}签"

    val levelDisplayName: String get() = if (level == "下") "下签" else level + "签"

    /** 各方面的断语（按签等从断语库中稳定地选取） */
    val areas: List<Pair<String, String>>
        get() {
            val rng = SeededGenerator("stick-area", n)
            val table = STICK_VERDICTS[level] ?: STICK_VERDICTS.getValue("中平")
            return STICK_AREAS.map { area -> area to rng.pick(table[area] ?: listOf("顺其自然")) }
        }

    val blessing: String
        get() = when (level) {
            "上上" -> "大吉大利，心想事成"
            "上吉" -> "吉星高照，诸事顺遂"
            "中吉" -> "稳中有进，渐入佳境"
            "下" -> "静待时机，否极泰来"
            else -> "平安是福，守中得益"
        }

    val isLucky: Boolean get() = level == "上上" || level == "上吉"

    companion object {
        val STICK_AREAS = listOf("心愿", "事业", "财运", "感情", "健康", "出行")

        private val STICK_VERDICTS: Map<String, Map<String, List<String>>> = mapOf(
            "上上" to mapOf(
                "心愿" to listOf("心想事成", "所求必得", "如愿以偿"),
                "事业" to listOf("步步高升", "大展宏图", "贵人提携"),
                "财运" to listOf("财源广进", "正偏皆旺", "积少成多"),
                "感情" to listOf("良缘天成", "甜蜜美满", "情投意合"),
                "健康" to listOf("身康体健", "精神焕发", "百病不侵"),
                "出行" to listOf("一路顺风", "出入平安", "远行大吉"),
            ),
            "上吉" to mapOf(
                "心愿" to listOf("终能如愿", "渐次达成", "好事将近"),
                "事业" to listOf("稳步上升", "机会在前", "宜积极进取"),
                "财运" to listOf("收入可观", "小有横财", "理财得宜"),
                "感情" to listOf("情意渐浓", "宜主动表达", "桃花朵朵"),
                "健康" to listOf("状态良好", "注意作息", "宜多运动"),
                "出行" to listOf("顺利平安", "宜结伴同行", "旅途愉快"),
            ),
            "中吉" to mapOf(
                "心愿" to listOf("稍待可成", "需再努力", "循序渐进"),
                "事业" to listOf("稳中求进", "宜守正道", "多请教前辈"),
                "财运" to listOf("收支平衡", "宜开源节流", "小财可得"),
                "感情" to listOf("细水长流", "宜多沟通", "缘分可期"),
                "健康" to listOf("无大碍", "注意饮食", "劳逸结合"),
                "出行" to listOf("平安无事", "宜提前规划", "近行为宜"),
            ),
            "中平" to mapOf(
                "心愿" to listOf("顺其自然", "缓缓图之", "莫要强求"),
                "事业" to listOf("守成为上", "按部就班", "勿轻易变动"),
                "财运" to listOf("守财为要", "勿贪小利", "量入为出"),
                "感情" to listOf("平淡是真", "宜包容理解", "静待缘分"),
                "健康" to listOf("宜静养", "早睡早起", "注意保暖"),
                "出行" to listOf("宜慎重", "非必要不远行", "注意安全"),
            ),
            "下" to mapOf(
                "心愿" to listOf("时机未到", "宜先放下", "改日再求"),
                "事业" to listOf("宜韬光养晦", "暂避锋芒", "从小事做起"),
                "财运" to listOf("谨防破财", "勿做投资", "节俭为宜"),
                "感情" to listOf("宜冷静", "勿操之过急", "以退为进"),
                "健康" to listOf("多加休息", "勿过度劳累", "及时就医"),
                "出行" to listOf("宜延期", "注意财物", "结伴更佳"),
            ),
        )
    }
}

fun chineseNumber(n: Int): String {
    val digits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    // 与 iOS 的 StickLibrary.chineseNumber 一致：负数和四位数以上直接用阿拉伯数字
    if (n < 0 || n >= 1000) return n.toString()
    // 一百、一百零五、一百一十二
    if (n >= 100) {
        val rest = n % 100
        val hundreds = digits[n / 100] + "百"
        return when {
            rest == 0 -> hundreds
            rest < 10 -> hundreds + "零" + digits[rest]
            rest < 20 -> hundreds + "一" + chineseNumber(rest)
            else -> hundreds + chineseNumber(rest)
        }
    }
    if (n < 10) return digits[n]
    if (n == 10) return "十"
    if (n < 20) return "十" + digits[n % 10]
    val tens = digits[n / 10] + "十"
    return if (n % 10 == 0) tens else tens + digits[n % 10]
}

@Serializable
data class TarotCard(
    val id: Int,
    val name: String,
    val en: String,
    val roman: String,
    val symbol: String,
    val keywords: List<String>,
    val upright: String,
    val reversed: String,
    val love: String,
    val career: String,
    val advice: String,
    // 逆位释义（可缺省）
    val keywordsReversed: List<String>? = null,
    val loveReversed: String? = null,
    val careerReversed: String? = null,
    val adviceReversed: String? = null,
    /** 小阿卡纳的花色："wands" | "cups" | "swords" | "pentacles"；大阿卡纳没有 */
    val suit: String? = null,
) {
    /** 每张牌的主色调：限定在深蓝 → 紫 → 品红的「宝石色」区间 */
    val hue: Double get() = 0.55 + ((id * 7) % 22) / 22.0 * 0.4

    val tarotSuit: TarotSuit? get() = TarotSuit.of(suit)

    val isMajor: Boolean get() = suit == null

    /** 牌面角标：大阿卡纳是罗马数字，小阿卡纳是 A、2…10、侍从、骑士、王后、国王 */
    val rankText: String get() = if (isMajor) roman else COURT_NAMES[roman] ?: roman

    // 逆位只取逆位字段，缺失时返回 null，界面隐藏对应内容，绝不拿正位释义顶替
    fun keywordsFor(reversed: Boolean): List<String>? =
        if (reversed) keywordsReversed?.takeIf { it.isNotEmpty() } else keywords

    fun loveFor(reversed: Boolean): String? = if (reversed) loveReversed?.takeIf { it.isNotBlank() } else love
    fun careerFor(reversed: Boolean): String? = if (reversed) careerReversed?.takeIf { it.isNotBlank() } else career
    fun adviceFor(reversed: Boolean): String? = if (reversed) adviceReversed?.takeIf { it.isNotBlank() } else advice

    companion object {
        private val COURT_NAMES = mapOf("Page" to "侍从", "Knight" to "骑士", "Queen" to "王后", "King" to "国王")
    }
}

/** 小阿卡纳的四个花色 */
enum class TarotSuit(val key: String, val label: String, val element: String, val emoji: String) {
    WANDS("wands", "权杖", "火", "🔥"),
    CUPS("cups", "圣杯", "水", "💧"),
    SWORDS("swords", "宝剑", "风", "⚔️"),
    PENTACLES("pentacles", "星币", "土", "⭐");

    companion object {
        fun of(key: String?): TarotSuit? = entries.firstOrNull { it.key == key }
    }
}

/** 内置菜品 */
data class Dish(
    val name: String,
    val emoji: String,
    val meals: String,
    val cuisine: String,
    val tastes: List<String>,
    val price: Int,
) {
    val priceText: String get() = "¥".repeat(price)
    val detail: String get() = "$cuisine · ${tastes.joinToString("/")} · $priceText"
}

@Serializable
data class TruthDareContent(val truth: List<List<String>>, val dare: List<List<String>>)

/** 打包在 App 内的离线文案库（JSON 与 iOS 版共用）。 */
object ContentLibrary {
    private val json = Json { ignoreUnknownKeys = true }

    private fun text(name: String): String {
        val stream = ContentLibrary::class.java.getResourceAsStream("/$name.json")
            ?: error("缺少资源文件 $name.json")
        return stream.use { it.readBytes().decodeToString() }
    }

    val fortune: FortuneContent by lazy { json.decodeFromString(text("fortune")) }
    val horoscope: HoroscopeContent by lazy { json.decodeFromString(text("horoscope")) }
    val sticks: List<FortuneStick> by lazy { json.decodeFromString(text("sticks")) }
    val tarot: List<TarotCard> by lazy { json.decodeFromString(text("tarot")) }
    val answers: List<String> by lazy { json.decodeFromString(text("answers")) }
    val truthDare: TruthDareContent by lazy { json.decodeFromString(text("truthdare")) }

    val foods: List<Dish> by lazy {
        json.parseToJsonElement(text("foods")).jsonArray.map { element ->
            val row = element.jsonArray
            Dish(
                name = row[0].jsonPrimitive.content,
                emoji = row[1].jsonPrimitive.content,
                meals = row[2].jsonPrimitive.content,
                cuisine = row[3].jsonPrimitive.content,
                tastes = row[4].jsonArray.map { it.jsonPrimitive.content },
                price = row[5].jsonPrimitive.int,
            )
        }
    }

    fun stick(number: Int): FortuneStick = sticks.firstOrNull { it.n == number } ?: sticks[0]

    /** 塔罗牌组：完整 78 张，或只用 22 张大阿卡纳 */
    fun tarotDeck(fullDeck: Boolean): List<TarotCard> = if (fullDeck) tarot else tarot.filter { it.isMajor }

    fun tarotCard(id: Int): TarotCard? = tarot.firstOrNull { it.id == id }
}
