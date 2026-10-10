import AVFoundation

enum SoundEffect: CaseIterable {
    case tick, tap, pop, whoosh, coinFlip, coinLand, diceHit, stickRattle, stickDrop
    case chime, success, fail, cardFlip, scratch, sparkle, drum
}

/// 全部音效都在启动时用代码合成，不依赖任何音频素材。
/// 使用 ambient 音频会话：尊重静音开关，也不会打断用户正在听的音乐。
@MainActor
final class SoundEngine {
    static let shared = SoundEngine()

    var isEnabled = true
    var volume: Float = 0.7 {
        didSet { engine.mainMixerNode.outputVolume = volume }
    }

    private let engine = AVAudioEngine()
    private var players: [AVAudioPlayerNode] = []
    private var buffers: [SoundEffect: [AVAudioPCMBuffer]] = [:]
    private var nextPlayer = 0
    private var isConfigured = false
    private static let sampleRate = 44_100.0
    private let format = AVAudioFormat(standardFormatWithSampleRate: SoundEngine.sampleRate, channels: 1)!

    private init() {}

    func prepare() {
        guard !isConfigured else { return }
        isConfigured = true
        try? AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default, options: [.mixWithOthers])
        for _ in 0..<10 {
            let player = AVAudioPlayerNode()
            engine.attach(player)
            engine.connect(player, to: engine.mainMixerNode, format: format)
            players.append(player)
        }
        engine.mainMixerNode.outputVolume = volume
        for effect in SoundEffect.allCases {
            buffers[effect] = (0..<variants(of: effect)).compactMap { makeBuffer(Synth.render(effect, variant: $0)) }
        }
        NotificationCenter.default.addObserver(forName: AVAudioSession.interruptionNotification, object: nil, queue: .main) { _ in
            Task { @MainActor in SoundEngine.shared.restartIfNeeded() }
        }
    }

    private func variants(of effect: SoundEffect) -> Int {
        switch effect {
        case .tick, .diceHit, .scratch, .tap: 3
        default: 1
        }
    }

    private func restartIfNeeded() {
        guard isConfigured, !engine.isRunning else { return }
        try? engine.start()
    }

    func play(_ effect: SoundEffect, volume: Float = 1) {
        guard isEnabled else { return }
        prepare()
        if !engine.isRunning {
            do { try engine.start() } catch { return }
        }
        guard let options = buffers[effect], let buffer = options.randomElement() else { return }
        let player = players[nextPlayer]
        nextPlayer = (nextPlayer + 1) % players.count
        player.stop()
        player.volume = max(0, min(1, volume))
        player.scheduleBuffer(buffer, at: nil, options: [], completionHandler: nil)
        player.play()
    }

    private func makeBuffer(_ samples: [Float]) -> AVAudioPCMBuffer? {
        guard let buffer = AVAudioPCMBuffer(pcmFormat: format, frameCapacity: AVAudioFrameCount(samples.count)) else { return nil }
        buffer.frameLength = AVAudioFrameCount(samples.count)
        if let channel = buffer.floatChannelData?[0] {
            samples.withUnsafeBufferPointer { channel.update(from: $0.baseAddress!, count: samples.count) }
        }
        return buffer
    }
}

/// 简单的加法/噪声合成器
private enum Synth {
    static let rate = 44_100.0

    struct Noise {
        var state: UInt32 = 22_222
        mutating func next() -> Float {
            state = state &* 1_664_525 &+ 1_013_904_223
            return Float(Int32(bitPattern: state)) / Float(Int32.max)
        }
    }

    static func render(_ effect: SoundEffect, variant: Int) -> [Float] {
        var noise = Noise(state: UInt32(1234 + variant * 977))
        let v = Double(variant)
        switch effect {
        case .tick:
            return tone(duration: 0.035) { t in
                let f = 2100 + v * 260
                return (0.45 * Double(noise.next()) + 0.8 * sin(2 * .pi * f * t)) * exp(-t / 0.0045)
            }
        case .tap:
            return tone(duration: 0.07) { t in
                let f = 820 - 4000 * t + v * 60
                return sin(2 * .pi * f * t) * exp(-t / 0.018) * 0.7
            }
        case .pop:
            return tone(duration: 0.09) { t in
                let f = 380 + 9000 * t
                return sin(2 * .pi * f * t) * sin(.pi * min(1, t / 0.09)) * 0.6
            }
        case .whoosh:
            var low: Double = 0
            let duration = 0.42
            return tone(duration: duration) { t in
                let p = t / duration
                let cutoff = 0.02 + 0.28 * sin(.pi * p)
                low += cutoff * (Double(noise.next()) - low)
                return low * pow(sin(.pi * p), 2) * 1.4
            }
        case .coinFlip:
            return tone(duration: 0.18) { t in
                (sin(2 * .pi * 3_300 * t) * 0.5 + sin(2 * .pi * 4_870 * t) * 0.3) * exp(-t / 0.04)
            }
        case .coinLand:
            let partials: [(Double, Double, Double)] = [(1_780, 1, 0.38), (2_640, 0.6, 0.26), (3_910, 0.42, 0.18), (5_380, 0.25, 0.12)]
            return tone(duration: 1.0) { t in
                var s = 0.0
                for (f, a, d) in partials {
                    s += a * sin(2 * .pi * f * t) * exp(-t / d)
                    if t > 0.13 { s += 0.35 * a * sin(2 * .pi * f * (t - 0.13)) * exp(-(t - 0.13) / (d * 0.6)) }
                }
                let click = t < 0.004 ? Double(noise.next()) * 0.6 : 0
                return (s * 0.28 + click)
            }
        case .diceHit:
            var low: Double = 0
            return tone(duration: 0.07) { t in
                let n = Double(noise.next())
                low += 0.25 * (n - low)
                let band = n - low
                return (band * 0.8 + 0.25 * sin(2 * .pi * (760 + v * 140) * t)) * exp(-t / 0.011)
            }
        case .stickRattle:
            var out = [Float](repeating: 0, count: Int(0.5 * rate))
            var gen = SeededGenerator(seed: 99)
            for i in 0..<9 {
                let start = Int((Double(i) * 0.052 + gen.double(0...0.018)) * rate)
                let f = gen.double(880...1_450)
                let clack = tone(duration: 0.04) { t in
                    (sin(2 * .pi * f * t) * 0.7 + (t < 0.003 ? Double(noise.next()) * 0.5 : 0)) * exp(-t / 0.012)
                }
                mix(clack, into: &out, at: start, gain: Float(gen.double(0.5...1)))
            }
            return out
        case .stickDrop:
            var out = tone(duration: 0.5) { t in
                (sin(2 * .pi * 210 * t) * exp(-t / 0.07) + 0.5 * sin(2 * .pi * 660 * t) * exp(-t / 0.03)
                 + (t < 0.004 ? Double(noise.next()) * 0.5 : 0)) * 0.8
            }
            let bounce = tone(duration: 0.15) { t in sin(2 * .pi * 720 * t) * exp(-t / 0.02) * 0.35 }
            mix(bounce, into: &out, at: Int(0.16 * rate))
            return out
        case .chime:
            var out = bell(frequency: 1_046.5, duration: 1.8, gain: 0.5)
            mix(bell(frequency: 1_318.5, duration: 1.4, gain: 0.32), into: &out, at: Int(0.11 * rate))
            mix(bell(frequency: 1_568.0, duration: 1.2, gain: 0.24), into: &out, at: Int(0.22 * rate))
            return out
        case .success:
            var out = [Float](repeating: 0, count: Int(0.9 * rate))
            for (i, f) in [523.25, 659.25, 783.99, 1_046.5].enumerated() {
                let note = tone(duration: 0.5) { t in
                    (sin(2 * .pi * f * t) + 0.3 * sin(4 * .pi * f * t)) * exp(-t / 0.18) * 0.4
                }
                mix(note, into: &out, at: Int(Double(i) * 0.085 * rate))
            }
            return out
        case .fail:
            var out = [Float](repeating: 0, count: Int(0.55 * rate))
            for (i, f) in [392.0, 293.66].enumerated() {
                let note = tone(duration: 0.3) { t in
                    let tri = asin(sin(2 * .pi * f * t)) * 2 / .pi
                    return tri * exp(-t / 0.12) * 0.45
                }
                mix(note, into: &out, at: Int(Double(i) * 0.17 * rate))
            }
            return out
        case .cardFlip:
            var low: Double = 0
            return tone(duration: 0.14) { t in
                let n = Double(noise.next())
                low += 0.12 * (n - low)
                let attack = min(1, t / 0.01)
                return (n - low) * attack * exp(-t / 0.035) * 0.8
            }
        case .scratch:
            var low: Double = 0
            return tone(duration: 0.09) { t in
                let n = Double(noise.next())
                low += 0.35 * (n - low)
                return (n - low) * sin(.pi * t / 0.09) * (0.25 + 0.05 * v)
            }
        case .sparkle:
            var out = [Float](repeating: 0, count: Int(0.7 * rate))
            var gen = SeededGenerator(seed: 7)
            for i in 0..<7 {
                let f = gen.double(2_400...5_200)
                let ping = tone(duration: 0.25) { t in sin(2 * .pi * f * t) * exp(-t / 0.05) * 0.25 }
                mix(ping, into: &out, at: Int(Double(i) * 0.06 * rate))
            }
            return out
        case .drum:
            return tone(duration: 0.35) { t in
                let f = 140 * exp(-t / 0.08) + 50
                return (sin(2 * .pi * f * t) * exp(-t / 0.12) + (t < 0.005 ? Double(noise.next()) * 0.4 : 0)) * 0.9
            }
        }
    }

    static func tone(duration: Double, _ sample: (Double) -> Double) -> [Float] {
        let count = Int(duration * rate)
        var out = [Float](repeating: 0, count: count)
        for i in 0..<count {
            let t = Double(i) / rate
            // 末尾 5ms 淡出，避免爆音
            let fade = min(1, Double(count - i) / (0.005 * rate))
            out[i] = Float(max(-1, min(1, sample(t) * fade)))
        }
        return out
    }

    static func bell(frequency: Double, duration: Double, gain: Double) -> [Float] {
        let ratios: [(Double, Double, Double)] = [(1, 1, 1.0), (2.76, 0.45, 0.45), (5.4, 0.25, 0.22), (8.93, 0.12, 0.1)]
        return tone(duration: duration) { t in
            var s = 0.0
            for (r, a, d) in ratios { s += a * sin(2 * .pi * frequency * r * t) * exp(-t / d) }
            return s * gain * min(1, t / 0.002)
        }
    }

    static func mix(_ source: [Float], into target: inout [Float], at offset: Int, gain: Float = 1) {
        for (i, s) in source.enumerated() {
            let index = offset + i
            guard index < target.count else { break }
            target[index] = max(-1, min(1, target[index] + s * gain))
        }
    }
}
