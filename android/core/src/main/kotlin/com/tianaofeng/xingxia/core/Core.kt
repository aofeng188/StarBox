package com.tianaofeng.xingxia.core

import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sqrt

/** Swift 的 `Double.rounded()`：四舍五入，.5 远离零。 */
fun Double.roundHalfAway(): Double = if (this >= 0) floor(this + 0.5) else -floor(-this + 0.5)

/** 用 yyyyMMdd 形式的整数标识「某一天」，便于存储、比较和作为随机种子。 */
data class DayKey(val year: Int, val month: Int, val day: Int) : Comparable<DayKey> {

    val rawValue: Int get() = year * 10000 + month * 100 + day

    override fun toString(): String = rawValue.toString()

    override fun compareTo(other: DayKey): Int = rawValue.compareTo(other.rawValue)

    val localDate: LocalDate get() = LocalDate.of(year, month, day)

    /** 当天中午 12 点（本地时区），避免夏令时/零点边界问题。 */
    val noon: Instant get() = LocalDateTime.of(year, month, day, 12, 0).atZone(ZoneId.systemDefault()).toInstant()

    val julianDayNumber: Int get() = julianDayNumber(year, month, day)

    fun adding(days: Int): DayKey = of(localDate.plusDays(days.toLong()))

    /** 从 1949-10-01（甲子日）起算的天数，用于干支纪日。 */
    val daysSinceJiaZiReference: Int get() = julianDayNumber - julianDayNumber(1949, 10, 1)

    /** 0 = 周日 … 6 = 周六 */
    val weekday: Int get() = (julianDayNumber + 1) % 7

    fun daysTo(other: DayKey): Int = other.julianDayNumber - julianDayNumber

    companion object {
        fun of(date: LocalDate) = DayKey(date.year, date.monthValue, date.dayOfMonth)

        fun of(instant: Instant, zone: ZoneId = ZoneId.systemDefault()) = of(instant.atZone(zone).toLocalDate())

        fun fromRaw(raw: Int): DayKey? =
            if (raw > 10000101) DayKey(raw / 10000, raw / 100 % 100, raw % 100) else null

        val today: DayKey get() = of(LocalDate.now())

        fun julianDayNumber(year: Int, month: Int, day: Int): Int {
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        }

        fun fromJulianDayNumber(jdn: Int): DayKey {
            val a = jdn + 32044
            val b = (4 * a + 3) / 146097
            val c = a - 146097 * b / 4
            val d = (4 * c + 3) / 1461
            val e = c - 1461 * d / 4
            val m = (5 * e + 2) / 153
            val day = e - (153 * m + 2) / 5 + 1
            val month = m + 3 - 12 * (m / 10)
            val year = 100 * b + d - 4800 + m / 10
            return DayKey(year, month, day)
        }
    }
}

object DateText {
    val weekdayNames = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

    fun monthDay(key: DayKey) = "${key.month}月${key.day}日"
    fun full(key: DayKey) = "${key.year}年${key.month}月${key.day}日"
    fun weekday(key: DayKey) = weekdayNames[key.weekday]

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun time(instant: Instant): String = instant.atZone(ZoneId.systemDefault()).format(timeFormat)

    fun relative(instant: Instant): String {
        val today = DayKey.today
        val key = DayKey.of(instant)
        return when (key.daysTo(today)) {
            0 -> "今天 " + time(instant)
            1 -> "昨天 " + time(instant)
            2 -> "前天 " + time(instant)
            else -> if (key.year == today.year) "${key.month}月${key.day}日 " + time(instant)
            else "${key.year}/${key.month}/${key.day}"
        }
    }
}

/**
 * 稳定的 64 位 FNV-1a 哈希。与 iOS 版完全一致，
 * 所以同一个人同一天在两个平台上看到的运势相同。
 */
object StableHash {
    fun fnv1a(string: String): ULong {
        var hash = 0xcbf29ce484222325uL
        for (byte in string.encodeToByteArray()) {
            hash = hash xor byte.toUByte().toULong()
            hash *= 0x00000100000001B3uL
        }
        return hash
    }

    fun combine(parts: List<Any>): ULong = fnv1a(parts.joinToString("|") { it.toString() })
}

/** SplitMix64 伪随机数生成器：同样的种子永远得到同样的序列。 */
class SeededGenerator(seed: ULong) {
    private var state: ULong = seed xor 0x9E3779B97F4A7C15uL

    constructor(vararg parts: Any) : this(StableHash.combine(parts.toList()))

    fun next(): ULong {
        state += 0x9E3779B97F4A7C15uL
        var z = state
        z = (z xor (z shr 30)) * 0xBF58476D1CE4E5B9uL
        z = (z xor (z shr 27)) * 0x94D049BB133111EBuL
        return z xor (z shr 31)
    }

    /** [0, 1) */
    fun unit(): Double = (next() shr 11).toDouble() * (1.0 / 9_007_199_254_740_992.0)

    fun int(range: IntRange): Int {
        val span = (range.last - range.first).toULong() + 1uL
        return range.first + (next() % span).toInt()
    }

    fun intBelow(upper: Int): Int {
        require(upper > 0)
        return (next() % upper.toULong()).toInt()
    }

    fun double(from: Double, to: Double): Double = from + unit() * (to - from)

    fun chance(probability: Double): Boolean = unit() < probability

    fun <T> pick(items: List<T>): T = items[intBelow(items.size)]

    /** 不重复地抽取 count 个元素（保持抽取顺序）。 */
    fun <T> pick(items: List<T>, count: Int): List<T> {
        val pool = items.toMutableList()
        val result = mutableListOf<T>()
        repeat(minOf(count, pool.size)) { result.add(pool.removeAt(intBelow(pool.size))) }
        return result
    }

    fun <T> shuffled(items: List<T>): List<T> {
        val array = items.toMutableList()
        if (array.size <= 1) return array
        for (i in array.size - 1 downTo 1) {
            val j = intBelow(i + 1)
            val tmp = array[i]; array[i] = array[j]; array[j] = tmp
        }
        return array
    }

    /** 按权重抽取下标，权重 <= 0 的项不会被抽中。 */
    fun weightedIndex(weights: List<Double>): Int? {
        val total = weights.sumOf { maxOf(0.0, it) }
        if (total <= 0) return null
        var target = unit() * total
        weights.forEachIndexed { index, weight ->
            if (weight > 0) {
                target -= weight
                if (target < 0) return index
            }
        }
        return weights.indexOfLast { it > 0 }
    }

    /** 近似正态分布（Box-Muller），用于让分数更集中在中间。 */
    fun gaussian(mean: Double, deviation: Double): Double {
        val u1 = maxOf(unit(), 1e-12)
        val u2 = unit()
        val z = sqrt(-2 * ln(u1)) * cos(2 * Math.PI * u2)
        return mean + z * deviation
    }
}

/** 真随机（加密安全随机源），用于抛硬币、骰子等「每次都不一样」的工具。 */
object TrueRandom {
    private val random = SecureRandom()

    fun int(range: IntRange): Int = range.first + random.nextInt(range.last - range.first + 1)

    fun double(): Double = random.nextDouble()

    fun bool(): Boolean = random.nextBoolean()

    fun <T> pick(items: List<T>): T = items[random.nextInt(items.size)]

    fun <T> shuffled(items: List<T>): List<T> = items.shuffled(random)

    fun weightedIndex(weights: List<Double>): Int? {
        val total = weights.sumOf { maxOf(0.0, it) }
        if (total <= 0) return null
        var target = random.nextDouble() * total
        weights.forEachIndexed { index, weight ->
            if (weight > 0) {
                target -= weight
                if (target < 0) return index
            }
        }
        return weights.indexOfLast { it > 0 }
    }
}

internal fun clampRound(value: Double, low: Int, high: Int): Int =
    minOf(high, maxOf(low, value.roundHalfAway().toInt()))
