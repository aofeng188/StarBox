import SwiftUI

enum MoodLevel: Int, CaseIterable, Identifiable {
    case awful = 1, down, okay, good, great

    var id: Int { rawValue }

    var emoji: String { ["😣", "😔", "😐", "🙂", "🤩"][rawValue - 1] }
    var name: String { ["很糟", "低落", "一般", "不错", "超棒"][rawValue - 1] }
    var color: Color {
        [Color(hex: 0x8B95A7), Color(hex: 0x7C9CF5), Color(hex: 0x5ECFD6), Color(hex: 0x7EE08A), Color(hex: 0xFFC94D)][rawValue - 1]
    }

    static let tagOptions = ["工作", "学习", "家人", "朋友", "恋爱", "运动", "美食", "旅行", "休息", "天气", "健康", "娱乐"]
}

/// 五个心情按钮。选中的那颗底下是一颗玻璃泡，换心情时像水滴一样流到新选的那颗上（减弱动态效果时直接切换）
struct MoodPicker: View {
    var selected: Int?
    var size: CGFloat = 46
    let onSelect: (MoodLevel) -> Void

    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Namespace private var glassNamespace
    /// 表情跟着字号放大（封顶 1.6 倍），但五个要在一行里排得下
    @ScaledMetric(relativeTo: .title) private var scale: CGFloat = 1
    @State private var rowWidth: CGFloat = 0

    /// 表情圆的直径：放大后不超过一格的宽度（选中时还要再放大 1.12 倍）
    private var diameter: CGFloat {
        let scaled = size * min(max(scale, 1), 1.6)
        guard rowWidth > 0 else { return size }
        let fit = rowWidth / CGFloat(MoodLevel.allCases.count) / 1.12
        return max(24, min(scaled, fit))
    }

    var body: some View {
        let d = diameter
        GlassEffectContainer(spacing: 8) {
            HStack(spacing: 0) {
                ForEach(MoodLevel.allCases) { mood in
                    let isSelected = selected == mood.rawValue
                    Button {
                        Haptics.selection()
                        onSelect(mood)
                    } label: {
                        VStack(spacing: 4) {
                            Text(mood.emoji)
                                .font(.system(size: d * 0.55))
                                .frame(width: d, height: d)
                                // 去色只作用在表情上，不包住玻璃（滤镜会让玻璃取不到背后的画面）
                                .grayscale(selected == nil || isSelected ? 0 : 0.6)
                                .background {
                                    if isSelected {
                                        Circle()
                                            .fill(mood.color.opacity(0.2))
                                            .glassEffect(.regular.tint(mood.color.opacity(0.35)).interactive(), in: .circle)
                                            .glassEffectID("selectedMood", in: glassNamespace)
                                    }
                                }
                                .overlay(Circle().strokeBorder(mood.color.opacity(isSelected ? 0.9 : 0), lineWidth: 1.5))
                                .scaleEffect(isSelected ? 1.12 : 1)
                            Text(mood.name)
                                .font(.caption2)
                                .lineLimit(1)
                                .minimumScaleFactor(0.7)
                                // 浅色模式下没选中的名字也要读得清
                                .foregroundStyle(.primary.opacity(isSelected ? 1 : (palette.isDark ? 0.7 : 0.85)))
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(mood.name)
                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                }
            }
        }
        .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { rowWidth = $0 }
        .animation(reduceMotion ? nil : Animation.spring(response: 0.35, dampingFraction: 0.6), value: selected)
    }
}
