import Foundation

/// 低精度天文算法（参考 Jean Meeus《天文算法》）。
/// 太阳黄经误差约 0.01°（约 15 分钟），月亮黄经误差约 0.3°，
/// 足够用于节气、星座、月亮星座和上升星座的计算。
enum Astronomy {
    static let j2000 = 2_451_545.0

    static func julianDay(_ date: Date) -> Double {
        date.timeIntervalSince1970 / 86_400.0 + 2_440_587.5
    }

    static func date(julianDay jd: Double) -> Date {
        Date(timeIntervalSince1970: (jd - 2_440_587.5) * 86_400.0)
    }

    private static func centuries(_ jd: Double) -> Double { (jd - j2000) / 36_525.0 }

    static func normalize(_ degrees: Double) -> Double {
        let value = degrees.truncatingRemainder(dividingBy: 360)
        return value < 0 ? value + 360 : value
    }

    private static func rad(_ degrees: Double) -> Double { degrees * .pi / 180 }
    private static func deg(_ radians: Double) -> Double { radians * 180 / .pi }

    /// 太阳视黄经（度）
    static func sunLongitude(jd: Double) -> Double {
        let t = centuries(jd)
        let l0 = 280.46646 + 36_000.76983 * t + 0.0003032 * t * t
        let m = 357.52911 + 35_999.05029 * t - 0.0001537 * t * t
        let mr = rad(m)
        let c = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(mr)
            + (0.019993 - 0.000101 * t) * sin(2 * mr)
            + 0.000289 * sin(3 * mr)
        let omega = 125.04 - 1_934.136 * t
        return normalize(l0 + c - 0.00569 - 0.00478 * sin(rad(omega)))
    }

    static func sunLongitude(_ date: Date) -> Double { sunLongitude(jd: julianDay(date)) }

    /// 月亮黄经（度），取主要周期项。
    static func moonLongitude(jd: Double) -> Double {
        let t = centuries(jd)
        let lp = 218.3164477 + 481_267.88123421 * t
        let d = rad(297.8501921 + 445_267.1114034 * t)
        let m = rad(357.5291092 + 35_999.0502909 * t)
        let mp = rad(134.9633964 + 477_198.8675055 * t)
        let f = rad(93.2720950 + 483_202.0175233 * t)
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

    static func moonLongitude(_ date: Date) -> Double { moonLongitude(jd: julianDay(date)) }

    /// 月相：0 = 新月，0.5 = 满月
    static func moonPhase(_ date: Date) -> Double {
        let jd = julianDay(date)
        return normalize(moonLongitude(jd: jd) - sunLongitude(jd: jd)) / 360
    }

    /// 上升点黄经（度）。latitude 北纬为正，longitude 东经为正。
    static func ascendant(_ date: Date, latitude: Double, longitude: Double) -> Double {
        let jd = julianDay(date)
        let t = centuries(jd)
        let gmst = 280.46061837 + 360.98564736629 * (jd - j2000) + 0.000387933 * t * t - t * t * t / 38_710_000
        let ramc = rad(normalize(gmst + longitude))
        let eps = rad(23.4392911 - 0.0130042 * t)
        let phi = rad(latitude)
        let asc = atan2(cos(ramc), -(sin(ramc) * cos(eps) + tan(phi) * sin(eps)))
        return normalize(deg(asc))
    }

    /// 求太阳到达某个黄经的时刻（儒略日），从 guess 附近迭代。
    static func timeOfSunLongitude(_ target: Double, near guess: Double) -> Double {
        var jd = guess
        for _ in 0..<12 {
            var diff = target - sunLongitude(jd: jd)
            diff = diff.truncatingRemainder(dividingBy: 360)
            if diff > 180 { diff -= 360 }
            if diff < -180 { diff += 360 }
            jd += diff / 360 * 365.2422
            if abs(diff) < 1e-7 { break }
        }
        return jd
    }

    /// 均时差（分钟），用于真太阳时。
    static func equationOfTime(_ date: Date) -> Double {
        let dayOfYear = Double(Calendar(identifier: .gregorian).ordinality(of: .day, in: .year, for: date) ?? 1)
        let b = rad(360.0 / 365.0 * (dayOfYear - 81))
        return 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b)
    }
}
