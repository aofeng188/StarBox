import SwiftUI

/// 「老虎机」式快速滚动后减速停下，用于吃什么、抽奖等。
@MainActor
enum Roller {
    static func roll<T>(pool: [T], final: T, steps: Int = 20, update: (T) -> Void) async {
        var delay = 0.03
        for _ in 0..<steps {
            update(pool.randomElement() ?? final)
            SoundEngine.shared.play(.tick, volume: 0.45)
            Haptics.selection()
            try? await Task.sleep(for: .seconds(delay))
            delay *= 1.13
        }
        update(final)
    }
}

/// 结果展示：大 emoji + 标题 + 副标题，滚动时有模糊位移效果
struct RollingResultView: View {
    let emoji: String
    let title: String
    let subtitle: String
    let isRolling: Bool
    var revealCount = 0

    @Environment(\.palette) private var palette

    var body: some View {
        VStack(spacing: 10) {
            Text(emoji)
                .font(.system(size: 76))
                .shadow(color: palette.accent.opacity(0.6), radius: isRolling ? 4 : 20)
                .id("e-" + emoji + title)
                .transition(.push(from: .bottom))
            Text(title)
                .font(.system(size: 32, weight: .bold, design: .rounded))
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .minimumScaleFactor(0.6)
                .id("t-" + title)
                .transition(.push(from: .bottom))
            Text(subtitle)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .blur(radius: isRolling ? 1.2 : 0)
        .scaleEffect(isRolling ? 0.96 : 1)
        .animation(.spring(response: 0.18, dampingFraction: 0.9), value: title)
        .animation(.spring(response: 0.4, dampingFraction: 0.5), value: isRolling)
        .keyframeAnimator(initialValue: 1.0, trigger: revealCount) { view, scale in
            view.scaleEffect(scale)
        } keyframes: { _ in
            CubicKeyframe(1.18, duration: 0.15)
            SpringKeyframe(1.0, duration: 0.4, spring: .bouncy)
        }
        .accessibilityElement(children: .combine)
    }
}

/// 一排可多选的筛选标签
struct ChipPicker: View {
    let options: [String]
    @Binding var selection: Set<String>
    var allLabel = "全部"

    @Environment(\.palette) private var palette

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip(allLabel, isOn: selection.isEmpty) { selection = [] }
                ForEach(options, id: \.self) { option in
                    chip(option, isOn: selection.contains(option)) {
                        if selection.contains(option) { selection.remove(option) } else { selection.insert(option) }
                    }
                }
            }
            .padding(.horizontal, 16)
        }
        .pageBleed()
    }

    private func chip(_ title: String, isOn: Bool, action: @escaping () -> Void) -> some View {
        Button {
            action()
            Haptics.selection()
        } label: {
            Text(title)
                .font(.subheadline.weight(isOn ? .semibold : .regular))
                .lineLimit(1)
                .fixedSize()
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                .foregroundStyle(isOn ? palette.onAccent : .primary)
                .background(Capsule().fill(isOn ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.07))))
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}
