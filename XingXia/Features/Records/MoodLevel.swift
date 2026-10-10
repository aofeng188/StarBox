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

/// 五个心情按钮
struct MoodPicker: View {
    var selected: Int?
    var size: CGFloat = 46
    let onSelect: (MoodLevel) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(MoodLevel.allCases) { mood in
                let isSelected = selected == mood.rawValue
                Button {
                    Haptics.selection()
                    onSelect(mood)
                } label: {
                    VStack(spacing: 4) {
                        Text(mood.emoji)
                            .font(.system(size: size * 0.55))
                            .frame(width: size, height: size)
                            .background {
                                Circle()
                                    .fill(mood.color.opacity(isSelected ? 0.35 : 0.0))
                                    .overlay(Circle().strokeBorder(mood.color.opacity(isSelected ? 0.9 : 0), lineWidth: 1.5))
                            }
                            .scaleEffect(isSelected ? 1.12 : 1)
                            .grayscale(selected == nil || isSelected ? 0 : 0.6)
                        Text(mood.name)
                            .font(.caption2)
                            .foregroundStyle(isSelected ? .primary : .secondary)
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(mood.name)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
        .animation(.spring(response: 0.35, dampingFraction: 0.6), value: selected)
    }
}
