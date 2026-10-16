package com.tianaofeng.xingxia.core

import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * 低精度天文算法（参考 Jean Meeus《天文算法》）。
 * 太阳黄经误差约 0.01°，月亮黄经误差约 0.3°，
 * 足够用于节气、星座、月亮星座和上升星座的计算。
 */
object Astronomy {
    const val J2000 = 2_451_545.0

    fun julianDay(instant: Instant): Double = instant.toEpochMilli() / 86_400_000.0 + 2_440_587.5

    fun instant(julianDay: Double): Instant =
        Instant.ofEpochMilli(((julianDay - 2_440_587.5) * 86_400_000.0).toLong())

    private fun centuries(jd: Double) = (jd - J2000) / 36_525.0

    fun normalize(degrees: Double): Double {
        val value = degrees % 360
        return if (value < 0) value + 360 else value
    }

    private fun rad(degrees: Double) = degrees * Math.PI / 180
    private fun deg(radians: Double) = radians * 180 / Math.PI

    /** 太阳视黄经（度） */
    fun sunLongitude(jd: Double): Double {
        val t = centuries(jd)
        val l0 = 280.46646 + 36_000.76983 * t + 0.0003032 * t * t
        val m = 357.52911 + 35_999.05029 * t - 0.0001537 * t * t
        val mr = rad(m)
        val c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mr) +
            (0.019993 - 0.000101 * t) * sin(2 * mr) +
            0.000289 * sin(3 * mr)
        val omega = 125.04 - 1_934.136 * t
        return normalize(l0 + c - 0.00569 - 0.00478 * sin(rad(omega)))
    }

    fun sunLongitude(instant: Instant) = sunLongitude(julianDay(instant))

    /** 月亮黄经（度），取主要周期项。 */
    fun moonLongitude(jd: Double): Double {
        val t = centuries(jd)
        val lp = 218.3164477 + 481_267.88123421 * t
        val d = rad(297.8501921 + 445_267.1114034 * t)
        val m = rad(357.5291092 + 35_999.0502909 * t)
        val mp = rad(134.9633964 + 477_198.8675055 * t)
        val f = rad(93.2720950 + 483_202.0175233 * t)
        var lon = lp
        lon += 6.288774 * sin(mp)
        lon += 1.274027 * sin(2 * d - mp)
        lon += 0.658314 * sin(2 * d)
        lon += 0.213618 * sin(2 * mp)
        lon -= 0.185116 * sin(m)
        lon -= 0.114332 * sin(2 * f)
        lon += 0.058793 * sin(2 * d - 2 * mp)
        lon += 0.057066 * sin(2 * d - m - mp)
        lon += 0.053322 * sin(2 * d + mp)
        lon += 0.045758 * sin(2 * d - m)
        lon -= 0.040923 * sin(m - mp)
        lon -= 0.034720 * sin(d)
        lon -= 0.030383 * sin(m + mp)
        lon += 0.015327 * sin(2 * d - 2 * f)
        lon -= 0.012528 * sin(mp + 2 * f)
        lon += 0.010980 * sin(mp - 2 * f)
        lon += 0.010675 * sin(4 * d - mp)
        lon += 0.010034 * sin(3 * mp)
        return normalize(lon)
    }

    fun moonLongitude(instant: Instant) = moonLongitude(julianDay(instant))

    /** 月相：0 = 新月，0.5 = 满月 */
    fun moonPhase(instant: Instant): Double = moonPhase(julianDay(instant))

    fun moonPhase(jd: Double): Double = normalize(moonLongitude(jd) - sunLongitude(jd)) / 360

    /** 上升点黄经（度）。latitude 北纬为正，longitude 东经为正。 */
    fun ascendant(instant: Instant, latitude: Double, longitude: Double): Double {
        val jd = julianDay(instant)
        val t = centuries(jd)
        val gmst = 280.46061837 + 360.98564736629 * (jd - J2000) + 0.000387933 * t * t - t * t * t / 38_710_000
        val ramc = rad(normalize(gmst + longitude))
        val eps = rad(23.4392911 - 0.0130042 * t)
        val phi = rad(latitude)
        val asc = atan2(cos(ramc), -(sin(ramc) * cos(eps) + tan(phi) * sin(eps)))
        return normalize(deg(asc))
    }

    /** 求太阳到达某个黄经的时刻（儒略日），从 guess 附近迭代。 */
    fun timeOfSunLongitude(target: Double, near: Double): Double {
        var jd = near
        repeat(12) {
            var diff = target - sunLongitude(jd)
            diff %= 360
            if (diff > 180) diff -= 360
            if (diff < -180) diff += 360
            jd += diff / 360 * 365.2422
            if (abs(diff) < 1e-7) return jd
        }
        return jd
    }

    /** 均时差（分钟），用于真太阳时。 */
    fun equationOfTime(instant: Instant): Double {
        val dayOfYear = instant.atZone(ZoneId.systemDefault()).dayOfYear.toDouble()
        val b = rad(360.0 / 365.0 * (dayOfYear - 81))
        return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    }
}
