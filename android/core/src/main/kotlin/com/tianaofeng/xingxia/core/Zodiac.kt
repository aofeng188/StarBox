package com.tianaofeng.xingxia.core

import java.time.Instant

enum class ZodiacElement(val label: String, val shortName: String, val trait: String) {
    FIRE("火象", "火", "热情冲动、行动力强"),
    EARTH("土象", "土", "务实稳重、脚踏实地"),
    AIR("风象", "风", "理性灵活、善于沟通"),
    WATER("水象", "水", "感性细腻、直觉敏锐"),
}

enum class ZodiacModality(val label: String) {
    CARDINAL("基本宫"), FIXED("固定宫"), MUTABLE("变动宫"),
}

/** 西方十二星座 */
enum class ZodiacSign {
    ARIES, TAURUS, GEMINI, CANCER, LEO, VIRGO, LIBRA, SCORPIO, SAGITTARIUS, CAPRICORN, AQUARIUS, PISCES;

    val label: String
        get() = listOf("白羊座", "金牛座", "双子座", "巨蟹座", "狮子座", "处女座", "天秤座", "天蝎座", "射手座", "摩羯座", "水瓶座", "双鱼座")[ordinal]

    val shortName: String get() = label.dropLast(1)

    val englishName: String
        get() = listOf("Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo", "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces")[ordinal]

    /** 文本样式的星座符号（加 U+FE0E 避免被渲染成 emoji） */
    val glyph: String get() = (0x2648 + ordinal).toChar().toString() + "︎"

    val dateRange: String
        get() = listOf(
            "3.21–4.19", "4.20–5.20", "5.21–6.21", "6.22–7.22", "7.23–8.22", "8.23–9.22",
            "9.23–10.23", "10.24–11.22", "11.23–12.21", "12.22–1.19", "1.20–2.18", "2.19–3.20",
        )[ordinal]

    val element: ZodiacElement get() = ZodiacElement.entries[ordinal % 4]
    val modality: ZodiacModality get() = ZodiacModality.entries[ordinal % 3]

    val ruler: String
        get() = listOf("火星", "金星", "水星", "月亮", "太阳", "水星", "金星", "冥王星", "木星", "土星", "天王星", "海王星")[ordinal]

    val keywords: List<String>
        get() = listOf(
            listOf("勇敢", "直率", "行动派"), listOf("稳重", "享受", "专一"), listOf("机智", "好奇", "多变"),
            listOf("温柔", "顾家", "敏感"), listOf("自信", "慷慨", "闪耀"), listOf("细致", "理性", "完美主义"),
            listOf("优雅", "公正", "社交达人"), listOf("深邃", "专注", "洞察力"), listOf("自由", "乐观", "冒险"),
            listOf("坚韧", "务实", "有野心"), listOf("独立", "创新", "博爱"), listOf("浪漫", "共情", "想象力"),
        )[ordinal]

    val profile: String
        get() = listOf(
            "白羊座是黄道第一宫，像初春破土的嫩芽，永远带着「先冲了再说」的勇气。你的热情来得快，行动力惊人，擅长开启新局面；需要学会的是耐心，把开头的火焰延续成长久的光。",
            "金牛座重视安全感与生活质感，喜欢一切美好、真实、能握在手里的东西。你慢热却可靠，一旦认定便长久坚持；放下对变化的抗拒，会遇见更多惊喜。",
            "双子座拥有星座里最灵活的大脑，好奇心像永不停歇的风。你擅长沟通与学习，能同时处理多件事情；给自己留一点专注的时间，才华会更加耀眼。",
            "巨蟹座温柔而坚韧，像月光一样守护在意的人。你对情绪有天然的敏感，懂得照顾他人；也别忘了把同样的温柔留给自己。",
            "狮子座天生自带主角光环，慷慨、热烈、敢于表达。你喜欢被欣赏，也乐于照亮身边的人；真正的自信来自内心，而不是掌声。",
            "处女座追求细节与秩序，是最可靠的执行者和最体贴的照顾者。你的标准很高，思维缜密；偶尔允许不完美，生活会轻松许多。",
            "天秤座优雅、讲究公平，擅长在人与人之间找到平衡点。你审美出众、人缘极佳；面对选择时多相信直觉，不必事事求全。",
            "天蝎座深邃而专注，拥有看透表象的洞察力。你爱憎分明、情感浓烈，一旦投入便全力以赴；学会信任与放下，会让你更有力量。",
            "射手座是自由的灵魂，热爱远方、知识与真理。你乐观幽默，总能把困境变成冒险；在奔跑的同时记得回头看看身边的人。",
            "摩羯座踏实坚韧，是星座里的长跑冠军。你有清晰的目标和惊人的自律，越到后程越显实力；别忘了给自己放个假。",
            "水瓶座独立、前卫，脑子里总有别人想不到的点子。你重视友谊与理想，关心更大的世界；偶尔也让身边人走进你的内心。",
            "双鱼座浪漫、善良，拥有丰富的想象力和强大的共情能力。你能感受到别人感受不到的情绪；为自己建立边界，梦想才能落地生根。",
        )[ordinal]

    /** 两个星座之间的相位距离（0…6） */
    fun distance(other: ZodiacSign): Int {
        val d = (other.ordinal - ordinal + 12) % 12
        return minOf(d, 12 - d)
    }

    fun offset(n: Int): ZodiacSign = entries[Math.floorMod(ordinal + n, 12)]

    /** 三分相（同元素）的星座 */
    val trines: List<ZodiacSign> get() = listOf(offset(4), offset(8))
    val sextiles: List<ZodiacSign> get() = listOf(offset(2), offset(10))
    val squares: List<ZodiacSign> get() = listOf(offset(3), offset(9))
    val opposite: ZodiacSign get() = offset(6)

    companion object {
        /** 由黄经确定星座（比日期表更准确，能处理交界日） */
        fun fromLongitude(longitude: Double): ZodiacSign = entries[(Astronomy.normalize(longitude) / 30).toInt() % 12]

        fun sunSign(birth: BirthInfo): ZodiacSign = fromLongitude(Astronomy.sunLongitude(birth.moment))

        fun moonSign(at: Instant): ZodiacSign = fromLongitude(Astronomy.moonLongitude(at))

        fun ascendant(birth: BirthInfo): ZodiacSign? {
            if (!birth.hasTime) return null
            val city = birth.city
            return fromLongitude(Astronomy.ascendant(birth.moment, city.latitude, city.longitude))
        }
    }
}

enum class AspectKind(val label: String, val key: String) {
    CONJUNCTION("合相", "conjunction"),
    SEMI_SEXTILE("半六合", "semiSextile"),
    SEXTILE("六合", "sextile"),
    SQUARE("刑相", "square"),
    TRINE("拱相", "trine"),
    QUINCUNX("梅花相", "quincunx"),
    OPPOSITION("冲相", "opposition");

    companion object {
        fun of(distance: Int): AspectKind = when (distance) {
            0 -> CONJUNCTION
            1 -> SEMI_SEXTILE
            2 -> SEXTILE
            3 -> SQUARE
            4 -> TRINE
            5 -> QUINCUNX
            else -> OPPOSITION
        }
    }
}

/** 星座 + 生肖配对 */
data class Compatibility(
    val score: Int,
    val title: String,
    val summary: String,
    val aspect: AspectKind,
    val elementNote: String,
    val animalNote: String?,
    val tips: List<String>,
) {
    companion object {
        fun between(a: ZodiacSign, b: ZodiacSign, animals: Pair<EarthlyBranch, EarthlyBranch>? = null): Compatibility {
            val aspect = AspectKind.of(a.distance(b))
            val (base, title, summary) = aspectInfo(aspect)
            var score = base
            val elementNote = elementInfo(a.element, b.element)

            var animalNote: String? = null
            if (animals != null) {
                val (x, y) = animals
                val relation = x.relation(y)
                score += relation.score
                animalNote = when (relation) {
                    BranchRelation.SIX_HARMONY -> "生肖${x.animal}与${y.animal}六合，传统上被视为天生一对。"
                    BranchRelation.THREE_HARMONY -> "生肖${x.animal}与${y.animal}三合，志同道合，合作愉快。"
                    BranchRelation.CLASH -> "生肖${x.animal}与${y.animal}相冲，个性都很鲜明，需要更多包容。"
                    BranchRelation.HARM -> "生肖${x.animal}与${y.animal}相害，沟通时多一分耐心。"
                    BranchRelation.SAME -> "同属${x.animal}，相似的成长背景让你们更容易理解彼此。"
                    BranchRelation.NEUTRAL -> "生肖${x.animal}与${y.animal}关系平和，相处没有明显阻碍。"
                }
            }
            val rng = SeededGenerator("compat", minOf(a.ordinal, b.ordinal), maxOf(a.ordinal, b.ordinal))
            score += rng.int(-2..2)
            val tips = rng.pick(TIP_POOL, 3)
            return Compatibility(minOf(99, maxOf(40, score)), title, summary, aspect, elementNote, animalNote, tips)
        }

        private fun aspectInfo(aspect: AspectKind): Triple<Int, String, String> = when (aspect) {
            AspectKind.CONJUNCTION -> Triple(80, "同频共振", "同一个星座的你们像在照镜子，天生懂得对方的想法与情绪。默契来得很快，但也容易放大相同的小缺点，记得互相提醒、互相欣赏。")
            AspectKind.SEMI_SEXTILE -> Triple(63, "相邻互补", "相邻星座性格差异明显，却又对彼此充满好奇。一个人的短板恰好是另一个人的长处，多一点耐心去适应对方的节奏。")
            AspectKind.SEXTILE -> Triple(85, "志趣相投", "元素相生、沟通顺畅，你们像老朋友一样轻松自在，聊得来也玩得来，是让人非常舒服的组合。")
            AspectKind.SQUARE -> Triple(56, "火花四溅", "行事风格和价值观差异较大，偶尔会擦出摩擦的火花。但正是这种张力推动彼此成长，学会换位思考就能化阻力为动力。")
            AspectKind.TRINE -> Triple(93, "天作之合", "同属一个元素，三观契合、步调一致，相处起来自然而然，是十二星座中最和谐的组合之一。")
            AspectKind.QUINCUNX -> Triple(61, "需要磨合", "你们关注的重点不太一样，需要不断调整彼此的期待。磨合好了，会成为互相成就的伙伴。")
            AspectKind.OPPOSITION -> Triple(75, "互相吸引", "对宫星座就像镜子的两面，对方身上有你缺少的特质，吸引力极强。在拉扯中找到平衡，会是很迷人的关系。")
        }

        private fun elementInfo(a: ZodiacElement, b: ZodiacElement): String {
            val pair = setOf(a, b)
            return when {
                a == b -> "同为${a.label}星座，天然的默契让你们很容易理解对方。"
                pair == setOf(ZodiacElement.FIRE, ZodiacElement.AIR) -> "火象遇上风象：火借风势，越烧越旺，彼此激发热情与灵感。"
                pair == setOf(ZodiacElement.EARTH, ZodiacElement.WATER) -> "土象遇上水象：水润土沃，彼此滋养，是安全感满满的组合。"
                pair == setOf(ZodiacElement.FIRE, ZodiacElement.WATER) -> "火象遇上水象：冰火两重天，一个热烈一个细腻，需要相互体谅。"
                pair == setOf(ZodiacElement.EARTH, ZodiacElement.AIR) -> "土象遇上风象：一个务实一个理想，需要在现实与想法之间找到平衡。"
                pair == setOf(ZodiacElement.FIRE, ZodiacElement.EARTH) -> "火象遇上土象：热情遇上稳重，一个想冲一个想稳，需要磨合节奏。"
                else -> "风象遇上水象：理性与感性的碰撞，多倾听对方的感受会更加融洽。"
            }
        }

        private val TIP_POOL = listOf(
            "每周留一段只属于你们的时间。",
            "遇到分歧时先说感受，再说道理。",
            "记住对方在意的小细节。",
            "一起完成一件小目标，会增加默契。",
            "给彼此保留一些独处的空间。",
            "真诚地夸奖对方，比什么都有用。",
            "争吵不过夜，有话好好说。",
            "一起尝试一件新鲜事。",
            "多问一句「你今天过得怎么样」。",
            "尊重彼此的朋友圈和兴趣爱好。",
            "用对方喜欢的方式表达爱。",
            "偶尔制造一点小惊喜。",
        )
    }
}
