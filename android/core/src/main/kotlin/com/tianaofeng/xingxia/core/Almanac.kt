package com.tianaofeng.xingxia.core

import java.time.Instant

/** 建除十二值星 */
class JianChu(val index: Int) {
    val name: String get() = NAMES[index] + "日"
    val yi: List<String> get() = YI[index]
    val ji: List<String> get() = JI[index]
    val summary: String get() = SUMMARIES[index]
    val isLucky: Boolean get() = index in listOf(0, 2, 4, 8, 9, 10)

    companion object {
        val NAMES = listOf("建", "除", "满", "平", "定", "执", "破", "危", "成", "收", "开", "闭")

        private val YI = listOf(
            listOf("出行", "上任", "会友", "求职", "祈福"),
            listOf("沐浴", "扫舍", "求医", "疗病", "解除"),
            listOf("祭祀", "祈福", "开市", "交易", "纳财"),
            listOf("修饰", "涂泥", "平治道路", "祭祀"),
            listOf("订盟", "嫁娶", "纳采", "交易", "立券"),
            listOf("祈福", "栽种", "纳畜", "捕捉", "立约"),
            listOf("求医", "治病", "破屋", "坏垣"),
            listOf("祭祀", "祈福", "安床", "静养"),
            listOf("嫁娶", "开市", "入学", "移徙", "开工", "出行"),
            listOf("纳财", "收账", "入学", "纳畜", "收纳"),
            listOf("开市", "开业", "入学", "嫁娶", "求职", "出行"),
            listOf("筑堤", "补垣", "修墙", "安葬", "收藏"),
        )

        private val JI = listOf(
            listOf("动土", "开仓", "嫁娶", "安葬"),
            listOf("嫁娶", "远行", "开市", "栽种"),
            listOf("动土", "栽种", "上任", "求医"),
            listOf("嫁娶", "开渠", "栽种", "远行"),
            listOf("诉讼", "出行", "求医"),
            listOf("开市", "出行", "移徙", "搬家"),
            listOf("嫁娶", "开市", "签约", "动土", "出行"),
            listOf("登高", "乘船", "冒险", "远行"),
            listOf("诉讼", "争执"),
            listOf("出行", "安葬", "开市"),
            listOf("安葬", "伐木", "动土"),
            listOf("开市", "出行", "求医", "上任", "开渠"),
        )

        private val SUMMARIES = listOf(
            "建日为一月之始，宜起事不宜动土。",
            "除日除旧布新，宜清扫、治病。",
            "满日丰收之象，宜祭祀纳财。",
            "平日平常之日，诸事平平，宜修整。",
            "定日安定之象，宜订盟立约。",
            "执日执守之象，宜守成与栽种。",
            "破日月破大耗，诸事不宜，只宜破旧。",
            "危日小心谨慎，宜安静守成。",
            "成日诸事有成，是大吉之日。",
            "收日收获之日，宜纳财收账。",
            "开日开通顺畅，宜开业、出行。",
            "闭日闭藏之象，宜收藏、修补。",
        )
    }
}

/** 黄道/黑道值神 */
class DayDeity(val index: Int) {
    val name: String get() = NAMES[index]
    val isYellowPath: Boolean get() = index in listOf(0, 1, 4, 5, 7, 10)
    val pathName: String get() = if (isYellowPath) "黄道吉日" else "黑道日"

    companion object {
        val NAMES = listOf("青龙", "明堂", "天刑", "朱雀", "金匮", "天德", "白虎", "玉堂", "天牢", "玄武", "司命", "勾陈")
    }
}

/** 某一天的完整黄历 */
class AlmanacDay(val key: DayKey) {
    val lunar: LunarDate = key.lunar()
    val day: GanZhi = GanZhi.ofDay(key)
    val year: GanZhi
    val month: GanZhi
    val solarTerm: SolarTerms.Term? = SolarTerms.termOn(key)
    val nextTerm: SolarTerms.Term = SolarTerms.next(key)
    val jianChu: JianChu
    val deity: DayDeity
    val yi: List<String>
    val ji: List<String>
    val festivals: List<String>

    init {
        val monthIndex = SolarTerms.monthIndex(key)
        var ganzhiYear = key.year
        if (monthIndex >= 10 && key.month <= 2) ganzhiYear -= 1
        year = GanZhi.ofYear(ganzhiYear)
        month = GanZhi.ofMonth(year.stem, monthIndex)

        val monthBranch = month.branch.index
        jianChu = JianChu((day.branch.index - monthBranch + 12) % 12)
        val dragonStart = (((monthBranch % 6) - 2) * 2 + 12) % 12
        deity = DayDeity((day.branch.index - dragonStart + 12) % 12)

        var yi = jianChu.yi
        val ji = jianChu.ji.toMutableList()
        if (deity.isYellowPath) {
            if ("祈福" !in yi) yi = yi + "祈福"
        } else if ("大事勿用" !in ji && jianChu.index == 6) {
            ji.add("大事勿用")
        }
        if (jianChu.index == 6) yi = listOf("求医", "治病", "破旧")
        this.yi = yi
        this.ji = ji
        festivals = Festivals.on(key, lunar)
    }

    /** 冲煞，如「冲马(甲午) 煞南」 */
    val clash: String
        get() {
            // 天克地冲：被日干所克的天干 + 对冲地支，如甲子日冲戊午
            val opposite = GanZhi.of(day.stem.index + 4, day.branch.index + 6)
            val sha = listOf("南", "东", "北", "西")[day.branch.index % 4]
            return "冲${day.branch.opposite.animal}(${opposite.name}) 煞$sha"
        }

    val clashAnimal: EarthlyBranch get() = day.branch.opposite

    val pengzu: List<String> get() = listOf(PENGZU_STEM[day.stem.index], PENGZU_BRANCH[day.branch.index])

    val joyDirection: String get() = listOf("东北", "西北", "西南", "正南", "东南")[day.stem.index % 5]
    val wealthDirection: String get() = listOf("东北", "西南", "正北", "正东", "正南")[day.stem.index / 2]

    val ganzhiText: String get() = "${year.name}年 ${month.name}月 ${day.name}日"

    val termText: String
        get() = solarTerm?.let { "今日${it.name}" } ?: "距${nextTerm.name}还有${key.daysTo(nextTerm.day)}天"

    companion object {
        private val PENGZU_STEM = listOf(
            "甲不开仓 财物耗散", "乙不栽植 千株不长", "丙不修灶 必见灾殃", "丁不剃头 头必生疮", "戊不受田 田主不祥",
            "己不破券 二比并亡", "庚不经络 织机虚张", "辛不合酱 主人不尝", "壬不汲水 更难提防", "癸不词讼 理弱敌强",
        )
        private val PENGZU_BRANCH = listOf(
            "子不问卜 自惹祸殃", "丑不冠带 主不还乡", "寅不祭祀 神鬼不尝", "卯不穿井 水泉不香",
            "辰不哭泣 必主重丧", "巳不远行 财物伏藏", "午不苫盖 屋主更张", "未不服药 毒气入肠",
            "申不安床 鬼祟入房", "酉不会客 醉坐颠狂", "戌不吃犬 作怪上床", "亥不嫁娶 不利新郎",
        )
    }
}

/** 节日 */
object Festivals {
    private val SOLAR = mapOf(
        101 to "元旦", 214 to "情人节", 308 to "妇女节", 312 to "植树节", 401 to "愚人节",
        501 to "劳动节", 504 to "青年节", 601 to "儿童节", 701 to "建党节", 801 to "建军节",
        910 to "教师节", 1001 to "国庆节", 1031 to "万圣夜", 1224 to "平安夜", 1225 to "圣诞节",
    )
    private val LUNAR = mapOf(
        101 to "春节", 115 to "元宵节", 202 to "龙抬头", 505 to "端午节", 707 to "七夕",
        715 to "中元节", 815 to "中秋节", 909 to "重阳节", 1208 to "腊八节", 1223 to "北方小年",
    )

    fun on(key: DayKey, lunar: LunarDate = key.lunar()): List<String> {
        val result = mutableListOf<String>()
        SOLAR[key.month * 100 + key.day]?.let { result.add(it) }
        if (!lunar.isLeapMonth) LUNAR[lunar.month * 100 + lunar.day]?.let { result.add(it) }
        // 除夕：明天是正月初一
        val tomorrow = key.adding(1).lunar()
        if (tomorrow.month == 1 && tomorrow.day == 1 && !tomorrow.isLeapMonth) result.add("除夕")
        // 母亲节：五月第二个周日；父亲节：六月第三个周日
        if (key.weekday == 0) {
            val nth = (key.day - 1) / 7 + 1
            if (key.month == 5 && nth == 2) result.add("母亲节")
            if (key.month == 6 && nth == 3) result.add("父亲节")
        }
        if (SolarTerms.termOn(key)?.name == "清明") result.add("清明节")
        return result
    }
}

/** 八字（四柱） */
class BaZi(birth: BirthInfo) {
    val year: GanZhi
    val month: GanZhi
    val day: GanZhi
    val hour: GanZhi?
    /** 是否已按真太阳时校正 */
    val trueSolarApplied: Boolean

    init {
        val city = birth.city
        val moment = birth.moment
        val zone = city.zone

        // 年、月柱以太阳黄经（节气）为准
        val monthIndex = SolarTerms.monthIndex(moment)
        val local = moment.atZone(zone)
        var ganzhiYear = local.year
        if (monthIndex >= 10 && local.monthValue <= 2) ganzhiYear -= 1
        year = GanZhi.ofYear(ganzhiYear)
        month = GanZhi.ofMonth(year.stem, monthIndex)

        if (birth.hasTime) {
            // 真太阳时 = 钟表时间 + 经度差修正 + 均时差
            val standardMeridian = zone.rules.getOffset(moment).totalSeconds / 3600.0 * 15
            val correction = (city.longitude - standardMeridian) * 4 + Astronomy.equationOfTime(moment)
            val solar: Instant = moment.plusMillis((correction * 60_000).toLong())
            val c = solar.atZone(zone)
            var dayKey = DayKey(c.year, c.monthValue, c.dayOfMonth)
            if (c.hour >= 23) dayKey = dayKey.adding(1)
            day = GanZhi.ofDay(dayKey)
            hour = GanZhi.ofHour(day.stem, EarthlyBranch.forHour(c.hour))
            trueSolarApplied = true
        } else {
            day = GanZhi.ofDay(birth.birthday)
            hour = null
            trueSolarApplied = false
        }
    }

    val pillars: List<GanZhi> get() = listOfNotNull(year, month, day, hour)
    val dayMaster: HeavenlyStem get() = day.stem
    val zodiacAnimal: EarthlyBranch get() = year.branch

    /** 五行个数统计（天干 + 地支本气） */
    val elementCounts: Map<WuXing, Int>
        get() {
            val counts = WuXing.entries.associateWith { 0 }.toMutableMap()
            for (pillar in pillars) {
                counts[pillar.stem.element] = counts.getValue(pillar.stem.element) + 1
                counts[pillar.branch.element] = counts.getValue(pillar.branch.element) + 1
            }
            return counts
        }

    val weakestElements: List<WuXing>
        get() {
            val counts = elementCounts
            val minimum = counts.values.min()
            return WuXing.entries.filter { counts[it] == minimum }
        }

    val strongestElement: WuXing
        get() {
            val counts = elementCounts
            // 并列时取第一个（与 iOS 版一致）
            return WuXing.entries.maxBy { counts.getValue(it) }
        }

    /** 简化的「喜用」：日主五行偏弱则取生扶，偏强则取泄耗。 */
    val favorableElement: WuXing
        get() {
            val counts = elementCounts
            val master = dayMaster.element
            val support = counts.getValue(master) + counts.getValue(master.generatedBy)
            val total = counts.values.sum()
            return if (support.toDouble() < total / 2.5) master.generatedBy else master.generates
        }

    val text: String get() = pillars.joinToString(" ") { it.name }
}
