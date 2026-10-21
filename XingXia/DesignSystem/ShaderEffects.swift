import SwiftUI

/// 全息镭射效果：随倾斜角度流动的彩虹光泽 + 一道高光。
/// 纯 SwiftUI 实现（渐变 + 混合模式），不需要额外的 Metal 工具链。tilt 取值约 -1…1。
struct HoloFoilModifier: ViewModifier {
    var tilt: CGSize
    var strength: Double = 1

    func body(content: Content) -> some View {
        let shift = tilt.width * 0.35 + tilt.height * 0.2
        content
            .overlay {
                LinearGradient(
                    colors: [.red, .orange, .yellow, .green, .cyan, .blue, .purple, .pink, .red],
                    startPoint: UnitPoint(x: -0.2 + shift, y: 0 - shift * 0.5),
                    endPoint: UnitPoint(x: 1.2 + shift, y: 1 + shift * 0.5)
                )
                .opacity(0.32 * strength)
                .blendMode(.overlay)
                .allowsHitTesting(false)
            }
            .overlay {
                // 斜向高光带
                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0),
                        .init(color: .white.opacity(0.0), location: max(0, 0.38 + shift)),
                        .init(color: .white.opacity(0.55 * strength), location: min(1, 0.5 + shift)),
                        .init(color: .white.opacity(0.0), location: min(1, 0.62 + shift)),
                        .init(color: .clear, location: 1),
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .blendMode(.softLight)
                .allowsHitTesting(false)
            }
            .compositingGroup()
    }
}

extension View {
    func holoFoil(tilt: CGSize, strength: Double = 1) -> some View {
        modifier(HoloFoilModifier(tilt: tilt, strength: strength))
    }

    /// trigger 变化时从中心扩散一圈光环
    func rippleEffect<T: Equatable>(at origin: CGPoint, trigger: T) -> some View {
        overlay { ShockwaveRing(trigger: trigger) }
    }
}

private struct ShockwaveRing<T: Equatable>: View {
    let trigger: T
    @Environment(\.palette) private var palette

    var body: some View {
        // 先取出颜色，避免在 keyframeAnimator 的 Sendable 闭包里捕获整个 palette
        let gradient = palette.accentGradient
        let accent = palette.accent
        Color.clear
            .keyframeAnimator(initialValue: ShockwaveState(), trigger: trigger) { _, state in
                Circle()
                    .strokeBorder(gradient, lineWidth: 3)
                    .shadow(color: accent, radius: 10)
                    .scaleEffect(state.scale)
                    .opacity(state.opacity)
            } keyframes: { _ in
                KeyframeTrack(\.scale) {
                    MoveKeyframe(0.2)
                    CubicKeyframe(1.8, duration: 1.1)
                }
                KeyframeTrack(\.opacity) {
                    MoveKeyframe(0.9)
                    LinearKeyframe(0, duration: 1.1)
                }
            }
            .allowsHitTesting(false)
    }
}

private struct ShockwaveState {
    var scale: Double = 0.2
    var opacity: Double = 0
}
