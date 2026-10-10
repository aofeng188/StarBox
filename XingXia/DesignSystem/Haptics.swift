import UIKit
import CoreHaptics

/// 震动反馈。简单反馈用 UIKit 的 FeedbackGenerator，复杂节奏用 Core Haptics。
@MainActor
enum Haptics {
    static var isEnabled = true

    private static let light = UIImpactFeedbackGenerator(style: .light)
    private static let medium = UIImpactFeedbackGenerator(style: .medium)
    private static let heavy = UIImpactFeedbackGenerator(style: .heavy)
    private static let soft = UIImpactFeedbackGenerator(style: .soft)
    private static let rigid = UIImpactFeedbackGenerator(style: .rigid)
    private static let selectionGenerator = UISelectionFeedbackGenerator()
    private static let notification = UINotificationFeedbackGenerator()
    private static var engine: CHHapticEngine?

    static func tap() { guard isEnabled else { return }; light.impactOccurred() }
    static func impact(_ intensity: CGFloat = 0.7) { guard isEnabled else { return }; medium.impactOccurred(intensity: intensity) }
    static func heavyImpact() { guard isEnabled else { return }; heavy.impactOccurred() }
    static func softImpact(_ intensity: CGFloat = 0.6) { guard isEnabled else { return }; soft.impactOccurred(intensity: intensity) }
    static func rigidImpact(_ intensity: CGFloat = 0.8) { guard isEnabled else { return }; rigid.impactOccurred(intensity: intensity) }
    static func selection() { guard isEnabled else { return }; selectionGenerator.selectionChanged() }
    static func success() { guard isEnabled else { return }; notification.notificationOccurred(.success) }
    static func warning() { guard isEnabled else { return }; notification.notificationOccurred(.warning) }
    static func error() { guard isEnabled else { return }; notification.notificationOccurred(.error) }

    static func prepare() {
        light.prepare()
        selectionGenerator.prepare()
    }

    /// 开奖/揭晓：由弱到强的心跳 + 一记重击
    static func reveal() {
        play([
            .init(time: 0, intensity: 0.35, sharpness: 0.3),
            .init(time: 0.12, intensity: 0.5, sharpness: 0.4),
            .init(time: 0.24, intensity: 0.7, sharpness: 0.5),
            .init(time: 0.42, intensity: 1.0, sharpness: 0.8),
        ], continuous: (0.42, 0.35, 0.6))
    }

    /// 摇签筒时的签子碰撞
    static func rattle(count: Int = 6) {
        var events: [Pulse] = []
        for i in 0..<count {
            events.append(.init(time: Double(i) * 0.055 + Double.random(in: 0...0.02),
                                intensity: Float.random(in: 0.35...0.8), sharpness: Float.random(in: 0.6...1)))
        }
        play(events)
    }

    /// 硬币/骰子落地
    static func land(strength: Float = 1) {
        play([
            .init(time: 0, intensity: min(1, strength), sharpness: 0.9),
            .init(time: 0.09, intensity: min(1, strength * 0.45), sharpness: 0.9),
            .init(time: 0.16, intensity: min(1, strength * 0.2), sharpness: 0.9),
        ])
    }

    struct Pulse {
        let time: Double
        let intensity: Float
        let sharpness: Float
    }

    private static func play(_ pulses: [Pulse], continuous: (start: Double, duration: Double, intensity: Float)? = nil) {
        guard isEnabled, CHHapticEngine.capabilitiesForHardware().supportsHaptics else {
            if isEnabled { medium.impactOccurred() }
            return
        }
        do {
            if engine == nil {
                engine = try CHHapticEngine()
                engine?.resetHandler = { Task { @MainActor in try? Haptics.engine?.start() } }
                engine?.stoppedHandler = { _ in Task { @MainActor in Haptics.engine = nil } }
            }
            try engine?.start()
            var events = pulses.map {
                CHHapticEvent(eventType: .hapticTransient, parameters: [
                    CHHapticEventParameter(parameterID: .hapticIntensity, value: $0.intensity),
                    CHHapticEventParameter(parameterID: .hapticSharpness, value: $0.sharpness),
                ], relativeTime: $0.time)
            }
            if let continuous {
                events.append(CHHapticEvent(eventType: .hapticContinuous, parameters: [
                    CHHapticEventParameter(parameterID: .hapticIntensity, value: continuous.intensity),
                    CHHapticEventParameter(parameterID: .hapticSharpness, value: 0.2),
                ], relativeTime: continuous.start, duration: continuous.duration))
            }
            let player = try engine?.makePlayer(with: CHHapticPattern(events: events, parameters: []))
            try player?.start(atTime: CHHapticTimeImmediate)
        } catch {
            medium.impactOccurred()
        }
    }
}
