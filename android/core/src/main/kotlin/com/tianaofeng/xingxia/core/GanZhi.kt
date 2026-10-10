package com.tianaofeng.xingxia.core

/** 带名字的颜色（中国传统色名 + 十六进制色值） */
data class NamedColor(val name: String, val hex: Int) {
    val hexString: String get() = "#%06X".format(hex)
}

/** 五行 */
enum class WuXing {
    WOOD, FIRE, EARTH, METAL, WATER;

    val label: String get() = listOf("木", "火", "土", "金", "水")[ordinal]

    /** 我生者 */
    val generates: WuXing get() = entries[(ordinal + 1) % 5]
    /** 生我者 */
    val generatedBy: WuXing get() = entries[(ordinal + 4) % 5]
    /** 我克者 */
    val controls: WuXing get() = entries[(ordinal + 2) % 5]
    /** 克我者 */
    val controlledBy: WuXing get() = entries[(ordinal + 3) % 5]

    val direction: String get() = listOf("正东", "正南", "中央", "正西", "正北")[ordinal]

    val numbers: List<Int> get() = listOf(listOf(3, 8), listOf(2, 7), listOf(5, 0), listOf(4, 9), listOf(1, 6))[ordinal]

    val luckyColors: List<NamedColor>
        get() = when (this) {
            WOOD -> listOf(NamedColor("翡翠绿", 0x3DA86A), NamedColor("薄荷绿", 0x5DD3A5), NamedColor("竹青", 0x789262), NamedColor("松石绿", 0x40A497))
            FIRE -> listOf(NamedColor("朱红", 0xFF4C29), NamedColor("胭脂红", 0xD9466B), NamedColor("珊瑚橙", 0xFF7F50), NamedColor("玫瑰紫", 0xC24B8A))
            EARTH -> listOf(NamedColor("琥珀黄", 0xF5B83D), NamedColor("驼色", 0xB08B62), NamedColor("杏黄", 0xF7C173), NamedColor("赭石", 0xA5703F))
            METAL -> listOf(NamedColor("月白", 0xD6ECF0), NamedColor("香槟金", 0xE8D3A6), NamedColor("银灰", 0xB8BCC8), NamedColor("珍珠白", 0xF3EFE6))
            WATER -> listOf(NamedColor("黛蓝", 0x3F5F8F), NamedColor("靛青", 0x177CB0), NamedColor("墨色", 0x3A3A48), NamedColor("湖蓝", 0x4FA3D9))
        }

    val keyword: String get() = listOf("生发", "热烈", "稳重", "果断", "灵动")[ordinal]
}

/** 天干 */
class HeavenlyStem(index: Int) {
    val index: Int = ((index % 10) + 10) % 10

    val name: String get() = NAMES[index]
    val element: WuXing get() = WuXing.entries[index / 2]
    val isYang: Boolean get() = index % 2 == 0

    override fun equals(other: Any?) = other is HeavenlyStem && other.index == index
    override fun hashCode() = index

    companion object {
        val NAMES = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    }
}

enum class BranchRelation(val label: String, val score: Int) {
    SAME("同支", 2),
    SIX_HARMONY("六合", 8),
    THREE_HARMONY("三合", 5),
    CLASH("相冲", -9),
    HARM("相害", -4),
    NEUTRAL("平和", 0);

    val key: String
        get() = when (this) {
            SAME -> "same"
            SIX_HARMONY -> "sixHarmony"
            THREE_HARMONY -> "threeHarmony"
            CLASH -> "clash"
            HARM -> "harm"
            NEUTRAL -> "neutral"
        }
}

/** 地支 */
class EarthlyBranch(index: Int) {
    val index: Int = ((index % 12) + 12) % 12

    val name: String get() = NAMES[index]
    val animal: String get() = ANIMALS[index]
    val animalEmoji: String get() = ANIMAL_EMOJI[index]
    val element: WuXing get() = ELEMENTS[index]

    /** 时辰名称，如「子时」 */
    val hourName: String get() = name + "时"
    val hourRange: String
        get() {
            val start = (index * 2 + 23) % 24
            val end = (start + 2) % 24
            return "%02d:00-%02d:00".format(start, end)
        }

    val opposite: EarthlyBranch get() = EarthlyBranch(index + 6)

    fun relation(other: EarthlyBranch): BranchRelation = when {
        index == other.index -> BranchRelation.SAME
        (index + other.index) % 12 == 1 -> BranchRelation.SIX_HARMONY
        index % 4 == other.index % 4 -> BranchRelation.THREE_HARMONY
        (index - other.index + 12) % 12 == 6 -> BranchRelation.CLASH
        (index + other.index) % 12 == 7 -> BranchRelation.HARM
        else -> BranchRelation.NEUTRAL
    }

    override fun equals(other: Any?) = other is EarthlyBranch && other.index == index
    override fun hashCode() = index

    companion object {
        val NAMES = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
        val ANIMALS = listOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
        val ANIMAL_EMOJI = listOf("🐭", "🐮", "🐯", "🐰", "🐲", "🐍", "🐴", "🐑", "🐵", "🐔", "🐶", "🐷")
        private val ELEMENTS: List<WuXing> = "水土木木土火火土金金土水".map { c -> WuXing.entries.first { it.label == c.toString() } }

        fun forHour(hour: Int) = EarthlyBranch(((hour + 1) / 2) % 12)
    }
}

/** 六十甲子中的一个干支 */
class GanZhi(index: Int) {
    val index: Int = ((index % 60) + 60) % 60

    val stem: HeavenlyStem get() = HeavenlyStem(index % 10)
    val branch: EarthlyBranch get() = EarthlyBranch(index % 12)
    val name: String get() = stem.name + branch.name
    val nayin: String get() = NAYIN[index / 2]

    override fun equals(other: Any?) = other is GanZhi && other.index == index
    override fun hashCode() = index
    override fun toString() = name

    companion object {
        fun of(stem: Int, branch: Int): GanZhi {
            val s = ((stem % 10) + 10) % 10
            val b = ((branch % 12) + 12) % 12
            return GanZhi(6 * s - 5 * b)
        }

        fun ofDay(key: DayKey) = GanZhi(key.daysSinceJiaZiReference)

        fun ofYear(year: Int) = GanZhi(year - 4)

        /** 五虎遁：由年干和月序（0 = 寅月）得到月柱 */
        fun ofMonth(yearStem: HeavenlyStem, monthIndex: Int): GanZhi {
            val firstStem = (yearStem.index % 5) * 2 + 2
            return of(firstStem + monthIndex, monthIndex + 2)
        }

        /** 五鼠遁：由日干和时支得到时柱 */
        fun ofHour(dayStem: HeavenlyStem, branch: EarthlyBranch): GanZhi {
            val firstStem = (dayStem.index % 5) * 2
            return of(firstStem + branch.index, branch.index)
        }

        private val NAYIN = listOf(
            "海中金", "炉中火", "大林木", "路旁土", "剑锋金", "山头火",
            "涧下水", "城头土", "白蜡金", "杨柳木", "泉中水", "屋上土",
            "霹雳火", "松柏木", "长流水", "沙中金", "山下火", "平地木",
            "壁上土", "金箔金", "覆灯火", "天河水", "大驿土", "钗钏金",
            "桑柘木", "大溪水", "沙中土", "天上火", "石榴木", "大海水",
        )
    }
}
