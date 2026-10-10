import SwiftUI
import CoreMotion

extension Notification.Name {
    static let deviceDidShake = Notification.Name("XingXiaDeviceDidShake")
}

/// 系统摇一摇事件（模拟器里可以用「Device ▸ Shake」触发）
extension UIWindow {
    override open func motionEnded(_ motion: UIEvent.EventSubtype, with event: UIEvent?) {
        if motion == .motionShake {
            NotificationCenter.default.post(name: .deviceDidShake, object: nil)
        }
        super.motionEnded(motion, with: event)
    }
}

private struct ShakeModifier: ViewModifier {
    let action: () -> Void
    @Environment(AppSettings.self) private var settings

    func body(content: Content) -> some View {
        content.onReceive(NotificationCenter.default.publisher(for: .deviceDidShake)) { _ in
            if settings.shakeEnabled { action() }
        }
    }
}

extension View {
    /// 摇一摇触发（受设置里「摇一摇」开关控制）
    func onShake(perform action: @escaping () -> Void) -> some View {
        modifier(ShakeModifier(action: action))
    }
}

/// 设备姿态与晃动强度，用于全息塔罗牌和签筒。
@Observable
@MainActor
final class MotionManager {
    static let shared = MotionManager()

    /// -1…1，左右倾斜
    private(set) var roll: Double = 0
    /// -1…1，前后倾斜
    private(set) var pitch: Double = 0
    /// 当前加速度强度（g），晃动时会明显变大
    private(set) var shakeIntensity: Double = 0

    private let manager = CMMotionManager()
    private var users = 0
    private var basePitch: Double?

    var isAvailable: Bool { manager.isDeviceMotionAvailable }

    func start() {
        users += 1
        guard users == 1, manager.isDeviceMotionAvailable else { return }
        basePitch = nil
        manager.deviceMotionUpdateInterval = 1.0 / 50
        manager.startDeviceMotionUpdates(to: .main) { [weak self] motion, _ in
            guard let self, let motion else { return }
            let attitude = motion.attitude
            if self.basePitch == nil { self.basePitch = attitude.pitch }
            self.roll = max(-1, min(1, attitude.roll / 0.6))
            self.pitch = max(-1, min(1, (attitude.pitch - (self.basePitch ?? 0)) / 0.6))
            let a = motion.userAcceleration
            let magnitude = (a.x * a.x + a.y * a.y + a.z * a.z).squareRoot()
            self.shakeIntensity = self.shakeIntensity * 0.7 + magnitude * 0.3
        }
    }

    func stop() {
        users = max(0, users - 1)
        guard users == 0 else { return }
        manager.stopDeviceMotionUpdates()
        roll = 0
        pitch = 0
        shakeIntensity = 0
    }
}
