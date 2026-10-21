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
    /// 结果的大 emoji 和名字跟着字号放大，封顶 1.6 倍
    @ScaledMetric(relativeTo: .largeTitle) private var emojiSize: CGFloat = 76
    @ScaledMetric(relativeTo: .title) private var titleSize: CGFloat = 32

    var body: some View {
        VStack(spacing: 10) {
            Text(emoji)
                .font(.system(size: min(emojiSize, 76 * 1.6)))
                .shadow(color: palette.accent.opacity(0.6), radius: isRolling ? 4 : 20)
                .id("e-" + emoji + title)
                .transition(.push(from: .bottom))
            Text(title)
                .font(.system(size: min(titleSize, 32 * 1.6), weight: .bold, design: .rounded))
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

/// 一排可多选的筛选标签：一行玻璃胶囊放在同一个 GlassEffectContainer 里，选中的铺主题强调色
struct ChipPicker: View {
    let options: [String]
    @Binding var selection: Set<String>
    var allLabel = "全部"

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Namespace private var glassNamespace

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            GlassEffectContainer(spacing: GlassChipMetrics.containerSpacing) {
                HStack(spacing: GlassChipMetrics.spacing) {
                    chip(allLabel, id: "all", isOn: selection.isEmpty) { selection = [] }
                    ForEach(options, id: \.self) { option in
                        chip(option, id: option, isOn: selection.contains(option)) {
                            if selection.contains(option) { selection.remove(option) } else { selection.insert(option) }
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 2)
            }
        }
        .fadingScrollEdges()
        .pageBleed()
    }

    private func chip(_ title: String, id: String, isOn: Bool, action: @escaping () -> Void) -> some View {
        Button {
            withAnimation(reduceMotion ? nil : .snappy) { action() }
            Haptics.selection()
        } label: {
            Text(title)
                .font(.subheadline.weight(isOn ? .semibold : .regular))
                .lineLimit(1)
                .fixedSize()
                .padding(.horizontal, 12)
                .padding(.vertical, 7)
                // 多选：每个 chip 的玻璃各自固定，不在 chip 之间流动
                .glassChip(isOn: isOn, id: id, flows: false, in: glassNamespace)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}

// MARK: - 玻璃 chip

enum GlassChipMetrics {
    /// chip 之间的间距
    static let spacing: CGFloat = 8
    /// GlassEffectContainer 的融合距离，比 chip 间距小：平时各自独立，换选时才融合变形
    static let containerSpacing: CGFloat = 4
}

extension View {
    /// chip 行里的一个玻璃胶囊（整行放进 GlassEffectContainer）。选中时铺主题强调色。
    /// 单选行传 `flows: true`：选中态的玻璃共用一个 ID，换选（包在 withAnimation 里）时像水滴一样流到新的 chip 上；
    /// 多选行传 false，每个 chip 的玻璃固定不动。
    func glassChip(isOn: Bool, id: String, flows: Bool, in namespace: Namespace.ID) -> some View {
        modifier(GlassChipModifier(isOn: isOn, id: id, flows: flows, namespace: namespace))
    }
}

private struct GlassChipModifier: ViewModifier {
    let isOn: Bool
    let id: String
    let flows: Bool
    let namespace: Namespace.ID

    @Environment(\.palette) private var palette

    func body(content: Content) -> some View {
        content
            .foregroundStyle(isOn ? palette.onAccent : Color.primary)
            .background {
                // 选中的底色不透明，深浅主题下 onAccent 文字的对比度和原来的实心胶囊一样
                if isOn { Capsule().fill(palette.accentGradient) }
            }
            .glassEffect(isOn ? .regular.tint(palette.accent).interactive() : .regular.interactive(), in: .capsule)
            .glassEffectID(flows && isOn ? "chip-selection" : "chip-" + id, in: namespace)
    }
}
