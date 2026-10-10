import Foundation

/// 稳定的 64 位 FNV-1a 哈希。Swift 自带的 `Hasher` 每次启动都会换种子，
/// 不能用来生成「同一天结果不变」的运势，所以这里自己实现一个。
enum StableHash {
    static func fnv1a(_ string: String) -> UInt64 {
        var hash: UInt64 = 0xcbf2_9ce4_8422_2325
        for byte in string.utf8 {
            hash ^= UInt64(byte)
            hash = hash &* 0x0000_0100_0000_01B3
        }
        return hash
    }

    static func combine(_ parts: [CustomStringConvertible]) -> UInt64 {
        fnv1a(parts.map(\.description).joined(separator: "|"))
    }
}

/// SplitMix64 伪随机数生成器：同样的种子永远得到同样的序列。
/// 所有「抽取」相关的工具函数都自己实现，避免依赖标准库内部算法的实现细节。
struct SeededGenerator: RandomNumberGenerator {
    private var state: UInt64

    init(seed: UInt64) {
        state = seed ^ 0x9E37_79B9_7F4A_7C15
    }

    init(_ parts: CustomStringConvertible...) {
        self.init(seed: StableHash.combine(parts))
    }

    mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }

    /// [0, 1)
    mutating func unit() -> Double {
        Double(next() >> 11) * (1.0 / 9_007_199_254_740_992.0)
    }

    mutating func int(_ range: ClosedRange<Int>) -> Int {
        let span = UInt64(range.upperBound - range.lowerBound) + 1
        return range.lowerBound + Int(next() % span)
    }

    mutating func int(below upper: Int) -> Int {
        precondition(upper > 0)
        return Int(next() % UInt64(upper))
    }

    mutating func double(_ range: ClosedRange<Double>) -> Double {
        range.lowerBound + unit() * (range.upperBound - range.lowerBound)
    }

    mutating func chance(_ probability: Double) -> Bool {
        unit() < probability
    }

    mutating func pick<T>(_ items: [T]) -> T {
        items[int(below: items.count)]
    }

    /// 不重复地抽取 count 个元素（保持抽取顺序）。
    mutating func pick<T>(_ items: [T], count: Int) -> [T] {
        var pool = items
        var result: [T] = []
        for _ in 0..<min(count, pool.count) {
            result.append(pool.remove(at: int(below: pool.count)))
        }
        return result
    }

    mutating func shuffled<T>(_ items: [T]) -> [T] {
        var array = items
        guard array.count > 1 else { return array }
        for i in stride(from: array.count - 1, to: 0, by: -1) {
            array.swapAt(i, int(below: i + 1))
        }
        return array
    }

    /// 按权重抽取下标，权重 <= 0 的项不会被抽中。
    mutating func weightedIndex(_ weights: [Double]) -> Int? {
        let total = weights.reduce(0) { $0 + max(0, $1) }
        guard total > 0 else { return nil }
        var target = unit() * total
        for (index, weight) in weights.enumerated() where weight > 0 {
            target -= weight
            if target < 0 { return index }
        }
        return weights.lastIndex { $0 > 0 }
    }

    /// 近似正态分布（Box-Muller），用于让分数更集中在中间。
    mutating func gaussian(mean: Double, deviation: Double) -> Double {
        let u1 = max(unit(), 1e-12)
        let u2 = unit()
        let z = (-2 * log(u1)).squareRoot() * cos(2 * .pi * u2)
        return mean + z * deviation
    }
}

/// 真随机（系统级加密安全随机源），用于抛硬币、骰子等「每次都不一样」的工具。
enum TrueRandom {
    static func int(_ range: ClosedRange<Int>) -> Int {
        Int.random(in: range)
    }

    static func weightedIndex(_ weights: [Double]) -> Int? {
        var generator = SystemRandomNumberGenerator()
        let total = weights.reduce(0) { $0 + max(0, $1) }
        guard total > 0 else { return nil }
        var target = Double.random(in: 0..<total, using: &generator)
        for (index, weight) in weights.enumerated() where weight > 0 {
            target -= weight
            if target < 0 { return index }
        }
        return weights.lastIndex { $0 > 0 }
    }
}
