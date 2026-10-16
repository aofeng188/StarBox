package com.tianaofeng.xingxia.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor

/** 八相月相（与 iOS 版 Shared/Astro/Sky.swift 的 MoonPhaseName 同序） */
enum class MoonPhaseName(val label: String, val symbol: String, val emoji: String) {
    NEW("新月", "moonphase.new.moon", "🌑"),
    WAXING_CRESCENT("娥眉月", "moonphase.waxing.crescent", "🌒"),
    FIRST_QUARTER("上弦月", "moonphase.first.quarter", "🌓"),
    WAXING_GIBBOUS("盈凸月", "moonphase.waxing.gibbous", "🌔"),
    FULL("满月", "moonphase.full.moon", "🌕"),
    WANING_GIBBOUS("亏凸月", "moonphase.waning.gibbous", "🌖"),
    LAST_QUARTER("下弦月", "moonphase.last.quarter", "🌗"),
    WANING_CRESCENT("残月", "moonphase.waning.crescent", "🌘");

    companion object {
        /** phase 为 Astronomy.moonPhase 的 0…1（0 新月，0.5 满月），按最近的一相取：floor(phase × 8 + 0.5) mod 8 */
        fun of(phase: Double): MoonPhaseName {
            val index = floor(phase * 8 + 0.5).toInt() % 8
            return entries[(index + 8) % 8]
        }
    }
}

/** 某一时刻的月相 */
data class MoonInfo(
    /** 0…1：日月黄经差 / 360，0 新月，0.5 满月 */
    val phase: Double,
    /** 亮面比例 0…1，(1 - cos(2π·phase)) / 2 */
    val illumination: Double,
    val name: MoonPhaseName,
) {
    /** 亮面百分比，四舍五入到整数 */
    val percent: Int get() = (illumination * 100).roundHalfAway().toInt()

    /** 上半月：亮面在右侧（北半球看），越来越圆 */
    val isWaxing: Boolean get() = phase < 0.5

    /** 月龄：距上次新月的天数（按平均朔望月估算） */
    val age: Double get() = phase * SkyEvents.SYNODIC_MONTH

    companion object {
        fun at(instant: Instant): MoonInfo {
            val phase = Astronomy.moonPhase(instant)
            return MoonInfo(phase, (1 - cos(2 * PI * phase)) / 2, MoonPhaseName.of(phase))
        }

        /** 当天晚上 21:00（北京时间）的月相 */
        fun on(day: DayKey): MoonInfo = at(SkyEvents.beijingMoment(day, 21))
    }
}

/** 一条天象：新月、满月、节气、流星雨极大、日月食。日期一律按北京时间。 */
data class SkyEvent(
    val day: DayKey,
    val kind: Kind,
    val title: String,
    /** 一两句说明：发生时刻和观测建议 */
    val detail: String,
) {
    /** key 与 iOS 的 rawValue 相同 */
    enum class Kind(val key: String) {
        NEW_MOON("newMoon"),
        FULL_MOON("fullMoon"),
        SOLAR_TERM("solarTerm"),
        METEOR_SHOWER("meteorShower"),
        ECLIPSE("eclipse"),
    }

    val id: String get() = "${kind.key}-${day.rawValue}-$title"
}

/** 天象日历，与 iOS 版 SkyEvents 逐行对应（日期、时刻和文案两端相同） */
object SkyEvents {
    /** 平均朔望月（天） */
    const val SYNODIC_MONTH = 29.530588853

    /** [from, from + days) 之间的天象，按日期排序；同一天依次是新月 / 满月、日月食、节气、流星雨 */
    fun upcoming(from: DayKey, days: Int): List<SkyEvent> {
        if (days <= 0) return emptyList()
        val end = from.adding(days)
        val events = mutableListOf<SkyEvent>()

        // 新月、满月：前后各多找一天，最后统一按北京时间的日期筛选
        val startJd = Astronomy.julianDay(beijingMoment(from, 0)) - 1
        val endJd = Astronomy.julianDay(beijingMoment(end, 0)) + 1
        for (kind in listOf(SkyEvent.Kind.NEW_MOON, SkyEvent.Kind.FULL_MOON)) {
            val target = if (kind == SkyEvent.Kind.NEW_MOON) 0.0 else 0.5
            var jd = nextPhase(target, startJd)
            while (jd < endJd) {
                events.add(moonEvent(kind, Astronomy.instant(jd)))
                jd = nextPhase(target, jd + 1)
            }
        }

        for (year in from.year..end.year) {
            SolarTerms.terms(year).mapTo(events) { termEvent(it) }
        }
        // 流星雨按「春分起算的年份」推算，象限仪座落在下一个公历年的 1 月，所以从前一年开始
        for (year in (from.year - 1)..end.year) {
            METEOR_SHOWERS.mapTo(events) { meteorEvent(it, year) }
        }
        for (eclipse in ECLIPSES) {
            DayKey.fromRaw(eclipse.day)?.let { events.add(SkyEvent(it, SkyEvent.Kind.ECLIPSE, eclipse.title, eclipse.detail)) }
        }

        return events
            .filter { it.day >= from && it.day < end }
            .sortedWith(compareBy<SkyEvent>({ it.day }, { order(it.kind) }, { it.title }))
    }

    /** after 之后（不含当天）第一次满月所在的日期（北京时间） */
    fun nextFullMoon(after: DayKey): DayKey = nextPhaseDay(0.5, after)

    /** after 之后（不含当天）第一次新月所在的日期（北京时间） */
    fun nextNewMoon(after: DayKey): DayKey = nextPhaseDay(0.0, after)

    // 计算

    /** 北京时间某天某个整点的时刻 */
    fun beijingMoment(day: DayKey, hour: Int): Instant =
        LocalDateTime.of(day.year, day.month, day.day, hour, 0).atZone(SolarTerms.BEIJING).toInstant()

    /** 北京时间的「时:分」，如 "08:05" */
    fun beijingTime(instant: Instant): String {
        val t = instant.atZone(SolarTerms.BEIJING)
        return String.format(Locale.ROOT, "%02d:%02d", t.hour, t.minute)
    }

    /** 儒略日 jd 时的月相 0…1，与 Astronomy.moonPhase 相同 */
    fun phase(jd: Double): Double = Astronomy.moonPhase(jd)

    /**
     * jd 之后月相第一次到达 target（0 新月，0.5 满月）的时刻（儒略日）。
     * 先按平均朔望月估一个初值，再用牛顿迭代收敛到 1e-9 个周期以内；与精确星历相比误差在几分钟以内（受月亮黄经算法精度限制）。
     */
    fun nextPhase(target: Double, after: Double): Double {
        var delta = target - phase(after)
        delta -= floor(delta)
        var t = after + delta * SYNODIC_MONTH
        for (i in 0 until 20) {
            var diff = target - phase(t)
            diff -= floor(diff + 0.5)
            t += diff * SYNODIC_MONTH
            if (abs(diff) < 1e-9) break
        }
        return t
    }

    /** [from, through] 之间（按北京时间日期）月相到达 target 的时刻，按时间排序（Android 排提醒用） */
    fun phaseMoments(target: Double, from: DayKey, through: DayKey): List<Instant> {
        val result = mutableListOf<Instant>()
        val endJd = Astronomy.julianDay(beijingMoment(through.adding(1), 0)) + 1
        var jd = nextPhase(target, Astronomy.julianDay(beijingMoment(from, 0)) - 1)
        while (jd < endJd) {
            val moment = Astronomy.instant(jd)
            val day = SolarTerms.beijingDay(moment)
            if (day >= from && day <= through) result.add(moment)
            jd = nextPhase(target, jd + 1)
        }
        return result
    }

    fun newMoons(from: DayKey, through: DayKey): List<Instant> = phaseMoments(0.0, from, through)

    fun fullMoons(from: DayKey, through: DayKey): List<Instant> = phaseMoments(0.5, from, through)

    private fun nextPhaseDay(target: Double, after: DayKey): DayKey {
        val start = Astronomy.julianDay(beijingMoment(after.adding(1), 0))
        return SolarTerms.beijingDay(Astronomy.instant(nextPhase(target, start)))
    }

    private fun order(kind: SkyEvent.Kind): Int = when (kind) {
        SkyEvent.Kind.NEW_MOON, SkyEvent.Kind.FULL_MOON -> 0
        SkyEvent.Kind.ECLIPSE -> 1
        SkyEvent.Kind.SOLAR_TERM -> 2
        SkyEvent.Kind.METEOR_SHOWER -> 3
    }

    // 新月、满月

    private fun moonEvent(kind: SkyEvent.Kind, moment: Instant): SkyEvent {
        val day = SolarTerms.beijingDay(moment)
        val lunar = day.lunar()
        val time = "北京时间 ${beijingTime(moment)}"
        val lunarText = "农历${lunar.monthName}${lunar.dayName}"
        if (kind == SkyEvent.Kind.NEW_MOON) {
            return SkyEvent(day, SkyEvent.Kind.NEW_MOON, "新月", "$time 日月合朔（$lunarText）。前后几晚没有月光干扰，夜空最暗，适合观星、拍星空。")
        }
        return SkyEvent(day, SkyEvent.Kind.FULL_MOON, "满月", "$time 月最圆（$lunarText）。月亮整夜都在天上，适合赏月、拍月亮；月光明亮，暗星不容易看到。")
    }

    // 节气

    private fun termEvent(term: SolarTerms.Term): SkyEvent {
        val longitude = (285 + 15 * term.index) % 360
        val tip = when (term.index) {
            5, 17 -> "昼夜几乎等长，太阳从正东升起、正西落下，适合校准方向。"
            11 -> "北半球白昼最长，正午的影子是一年中最短的。"
            23 -> "北半球白昼最短、黑夜最长，正午的影子是一年中最长的。"
            in 2..7 -> "春季傍晚，可以在北方天空找北斗七星，南方找狮子座。"
            in 8..13 -> "夏夜到远离灯光的地方，能看到银河和南方低空的天蝎座。"
            in 14..19 -> "秋季夜晚，可以在头顶附近找飞马座的四边形。"
            else -> "冬季夜空通透，猎户座和全天最亮的恒星天狼星最显眼。"
        }
        return SkyEvent(term.day, SkyEvent.Kind.SOLAR_TERM, term.name, "北京时间 ${beijingTime(term.moment)} 交节，太阳到达黄经 $longitude°。$tip")
    }

    // 流星雨

    class MeteorShower(
        val name: String,
        /** 极大时的太阳黄经（J2000，国际流星组织 IMO 的数值） */
        val longitude: Double,
        /** 理想条件下每小时的流星数（ZHR） */
        val rate: Int,
        val advice: String,
    )

    val METEOR_SHOWERS = listOf(
        MeteorShower("象限仪座流星雨", 283.15, 80, "辐射点在北斗七星斗柄附近，后半夜到天亮前最好；高峰只有几个小时，要抓准时间。"),
        MeteorShower("天琴座流星雨", 32.32, 18, "后半夜辐射点升高后较好，偶尔会有明亮的火流星。"),
        MeteorShower("宝瓶座η流星雨", 45.5, 50, "来自哈雷彗星的碎屑，辐射点升起得晚，天亮前一两个小时朝东方低空看。"),
        MeteorShower("英仙座流星雨", 140.0, 100, "一年中最受欢迎的流星雨，午夜后辐射点升高、流星最多，到郊外躺着看就好。"),
        MeteorShower("猎户座流星雨", 208.0, 20, "同样来自哈雷彗星，午夜后猎户座升起时最好，流星速度很快。"),
        MeteorShower("狮子座流星雨", 235.27, 15, "后半夜狮子座升起后观看，流星速度快，常拖着余迹。"),
        MeteorShower("双子座流星雨", 262.2, 150, "一年中数量最多、最稳定的流星雨，晚上九点后就能看到，后半夜最多，注意保暖。"),
    )

    /** year 年春分之后，太阳到达该流星雨极大黄经的时刻。IMO 的黄经是 J2000 分点，换算到当年分点要加上岁差（每年约 0.01397°） */
    private fun meteorEvent(shower: MeteorShower, year: Int): SkyEvent {
        val target = Astronomy.normalize(shower.longitude + 0.013969713 * (year - 2000))
        val equinox = LocalDateTime.of(year, 3, 20, 12, 0).toInstant(ZoneOffset.UTC)
        val jd = Astronomy.timeOfSunLongitude(target, Astronomy.julianDay(equinox) + target / 360 * 365.2422)
        val moment = Astronomy.instant(jd)
        val day = SolarTerms.beijingDay(moment)
        val hour = moment.atZone(SolarTerms.BEIJING).hour
        val moon = MoonInfo.at(moment)
        val moonText = when {
            moon.illumination < 0.25 -> "极大前后月光很弱，观测条件好。"
            moon.illumination < 0.6 -> "有一些月光干扰，背对月亮的方向看。"
            else -> "月光较亮，会掩盖暗弱的流星。"
        }
        return SkyEvent(
            day, SkyEvent.Kind.METEOR_SHOWER, shower.name,
            "极大约在北京时间${day.month}月${day.day}日 $hour 时前后，理想条件下每小时约 ${shower.rate} 颗。${shower.advice}$moonText",
        )
    }

    // 日月食

    /** day 为 yyyyMMdd */
    class Eclipse(val day: Int, val title: String, val detail: String)

    /**
     * 2026–2035 年的日食和本影月食（半影月食肉眼几乎看不出，不列）。日期为北京时间食甚所在的一天。
     * 类型、时刻和我国可见情况按 PyEphem 星历逐一核算：月食取各地月亮在地平线上的时段，日食取各地太阳在地平线上时的最大食分。
     */
    val ECLIPSES = listOf(
        Eclipse(20260217, "日环食", "环食带在南极洲，我国不可见。"),
        Eclipse(20260303, "月全食", "全食约 19:04–20:02（北京时间）。我国可见：东部地区月出后不久就能看到全食，西部地区月出较晚，月亮升起时全食已接近结束。"),
        Eclipse(20260813, "日全食", "北京时间凌晨发生，全食带经过格陵兰、冰岛和西班牙，我国不可见。"),
        Eclipse(20260828, "月偏食", "发生在北京时间中午，我国不可见。"),
        Eclipse(20270206, "日环食", "环食带经过南美洲南部和大西洋，我国不可见。"),
        Eclipse(20270802, "日全食", "全食最长可达 6 分多钟，全食带经过西班牙南部、北非和阿拉伯半岛，我国不可见。"),
        Eclipse(20280112, "月偏食", "食分很小，发生在北京时间中午，我国不可见。"),
        Eclipse(20280126, "日环食", "环食带经过南美洲北部和大西洋，我国不可见。"),
        Eclipse(20280707, "月偏食", "凌晨约 01:08–03:30，全国可见，月亮约四成被地球本影遮住。"),
        Eclipse(20280722, "日全食", "全食带经过澳大利亚和新西兰；我国只有海南岛南部和南海地区上午能看到很小的偏食。"),
        Eclipse(20290101, "月全食", "跨年夜的月全食：12 月 31 日 23:07 左右开始偏食，全食约 00:16–01:27，全国可见全过程。"),
        Eclipse(20290115, "日偏食", "北美洲可见，我国不可见。"),
        Eclipse(20290612, "日偏食", "北极附近地区可见，我国不可见。"),
        Eclipse(20290626, "月全食", "发生在北京时间中午，我国不可见。"),
        Eclipse(20290711, "日偏食", "南美洲南端可见，我国不可见。"),
        Eclipse(20291205, "日偏食", "南极洲一带可见，我国不可见。"),
        Eclipse(20291221, "月全食", "全食约 06:15–07:08，天亮前月亮西沉时进入全食；我国可见，越往西看得越完整。"),
        Eclipse(20300601, "日环食", "环食带经过内蒙古东北部和黑龙江，全国大部分地区下午可见偏食，越往北食分越大。"),
        Eclipse(20300616, "月偏食", "凌晨约 01:20–03:45，全国可见，月亮约一半被地球本影遮住。"),
        Eclipse(20301125, "日全食", "全食带经过南部非洲和澳大利亚，我国不可见。"),
        Eclipse(20310521, "日环食", "环食带经过非洲、印度洋和东南亚；我国南方大部分地区下午可见偏食，北方看不到。"),
        Eclipse(20311115, "全环食", "少见的全环食，食带主要在太平洋上，我国不可见。"),
        Eclipse(20320425, "月全食", "全食约 22:40–23:46，全国可见全过程。"),
        Eclipse(20320509, "日环食", "环食带在南大西洋上，我国不可见。"),
        Eclipse(20321019, "月全食", "凌晨全食约 02:38–03:25，全国可见。"),
        Eclipse(20321103, "日偏食", "全国可见偏食，越往北食分越大，北京约 13:56 食分最大。"),
        Eclipse(20330331, "日全食", "北京时间凌晨发生，全食带在阿拉斯加，我国不可见。"),
        Eclipse(20330415, "月全食", "凌晨全食约 02:48–03:37，全国可见。"),
        Eclipse(20330923, "日偏食", "南美洲南部可见，我国不可见。"),
        Eclipse(20331008, "月全食", "全食约 18:15–19:33。我国可见：东部地区月出后就能看到全食，西部地区月出较晚，只能看到偏食的尾声。"),
        Eclipse(20340320, "日全食", "全食带经过非洲、中东和南亚，日落前到达西藏西部；我国西部地区傍晚可见偏食。"),
        Eclipse(20340913, "日环食", "北京时间凌晨发生，环食带经过南美洲，我国不可见。"),
        Eclipse(20340928, "月偏食", "食分极小，发生在北京时间上午，我国不可见。"),
        Eclipse(20350310, "日环食", "环食带主要在南太平洋上，我国不可见。"),
        Eclipse(20350819, "月偏食", "发生在北京时间上午，我国不可见。"),
        Eclipse(20350902, "日全食", "全食带从新疆向东经过甘肃、内蒙古、河北、北京等地，北京上午约 8:33 可见日全食；全国其他地区可见偏食。"),
    )
}
