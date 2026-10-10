import SwiftUI

// MARK: - 玻璃卡片

struct GlassCard<Content: View>: View {
    var cornerRadius: CGFloat = 28
    var padding: CGFloat = 18
    var tint: Color?
    var interactive = false
    @ViewBuilder var content: Content

    @Environment(\.palette) private var palette

    var body: some View {
        content
            .padding(padding)
            .frame(maxWidth: .infinity, alignment: .leading)
            .glassEffect(.regular.tint(tint ?? palette.glassTint).interactive(interactive),
                         in: .rect(cornerRadius: cornerRadius))
    }
}

extension View {
    func glassCard(cornerRadius: CGFloat = 28, padding: CGFloat = 18, tint: Color? = nil, interactive: Bool = false) -> some View {
        GlassCard(cornerRadius: cornerRadius, padding: padding, tint: tint, interactive: interactive) { self }
    }

    /// 圆形玻璃底
    func glassCircle(tint: Color? = nil, interactive: Bool = false) -> some View {
        glassEffect(.regular.tint(tint).interactive(interactive), in: .circle)
    }
}

/// 让 NavigationLink 卡片按下时有轻微缩放，而不是整块变灰
struct PressableCardStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? 0.97 : 1)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: configuration.isPressed)
    }
}

extension ButtonStyle where Self == PressableCardStyle {
    static var pressableCard: PressableCardStyle { PressableCardStyle() }
}

// MARK: - 标题

struct SectionHeader: View {
    let title: String
    var subtitle: String?
    var symbol: String?

    @Environment(\.palette) private var palette

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 8) {
            if let symbol {
                Image(systemName: symbol)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(palette.accentGradient)
            }
            Text(title)
                .font(.headline)
            if let subtitle {
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 0)
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

// MARK: - 分数环

struct ScoreRing: View {
    let score: Int
    var size: CGFloat = 120
    var lineWidth: CGFloat = 12
    var label: String?

    @Environment(\.palette) private var palette
    @State private var progress: Double = 0

    var body: some View {
        ZStack {
            Circle()
                .stroke(.primary.opacity(0.08), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(AngularGradient(colors: [palette.accent, palette.accentSecondary, palette.nebula[0], palette.accent],
                                        center: .center),
                        style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .shadow(color: palette.accent.opacity(0.6), radius: lineWidth * 0.8)
            VStack(spacing: 0) {
                Text("\(Int((progress * 100).rounded()))")
                    .font(.system(size: size * 0.32, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .contentTransition(.numericText(value: progress))
                if let label {
                    Text(label)
                        .font(.system(size: size * 0.12, weight: .semibold))
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(width: size, height: size)
        .onAppear {
            withAnimation(.spring(response: 1.4, dampingFraction: 0.85).delay(0.1)) { progress = Double(score) / 100 }
        }
        .onChange(of: score) { _, new in
            withAnimation(.spring(response: 1, dampingFraction: 0.85)) { progress = Double(new) / 100 }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label ?? "分数") \(score) 分")
    }
}

// MARK: - 星级

struct StarRatingView: View {
    let stars: Double
    var size: CGFloat = 14
    var color: Color?

    @Environment(\.palette) private var palette

    var body: some View {
        HStack(spacing: 2) {
            ForEach(0..<5, id: \.self) { index in
                let value = stars - Double(index)
                Image(systemName: value >= 1 ? "star.fill" : (value >= 0.5 ? "star.leadinghalf.filled" : "star"))
                    .font(.system(size: size))
                    .foregroundStyle(color ?? palette.accent)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(stars.formatted()) 颗星")
    }
}

// MARK: - 进度条

struct MeterBar: View {
    let value: Int
    var color: Color?
    var height: CGFloat = 6

    @Environment(\.palette) private var palette
    @State private var shown: Double = 0

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(.primary.opacity(0.08))
                Capsule()
                    .fill(LinearGradient(colors: [color ?? palette.accent, (color ?? palette.accentSecondary).opacity(0.7)],
                                         startPoint: .leading, endPoint: .trailing))
                    .frame(width: proxy.size.width * shown)
                    .shadow(color: (color ?? palette.accent).opacity(0.5), radius: 4)
            }
        }
        .frame(height: height)
        .onAppear { withAnimation(.easeOut(duration: 1).delay(0.2)) { shown = Double(value) / 100 } }
        .onChange(of: value) { _, new in withAnimation(.easeOut(duration: 0.6)) { shown = Double(new) / 100 } }
    }
}

// MARK: - 小标签

struct TagChip: View {
    let text: String
    var color: Color?
    var symbol: String?

    @Environment(\.palette) private var palette

    var body: some View {
        HStack(spacing: 4) {
            if let symbol { Image(systemName: symbol).font(.caption2) }
            Text(text).font(.caption.weight(.medium))
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .foregroundStyle(color ?? palette.accent)
        .background((color ?? palette.accent).opacity(0.14), in: .capsule)
    }
}

// MARK: - 发光图标

struct GlowIcon: View {
    let symbol: String
    var color: Color
    var secondary: Color?
    var size: CGFloat = 44

    var body: some View {
        ZStack {
            Circle()
                .fill(LinearGradient(colors: [color, secondary ?? color.opacity(0.6)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .shadow(color: color.opacity(0.55), radius: size * 0.22)
            Image(systemName: symbol)
                .font(.system(size: size * 0.44, weight: .semibold))
                .foregroundStyle(.white)
                .symbolRenderingMode(.hierarchical)
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// 带渐变底的方形图标（工具格子用）
struct ToolIcon: View {
    let symbol: String
    let colors: [Color]
    var size: CGFloat = 52
    var emoji: String?

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.3, style: .continuous)
                .fill(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing))
                .shadow(color: colors.first?.opacity(0.5) ?? .clear, radius: 8, y: 3)
            RoundedRectangle(cornerRadius: size * 0.3, style: .continuous)
                .strokeBorder(.white.opacity(0.35), lineWidth: 0.8)
            if let emoji {
                Text(emoji).font(.system(size: size * 0.5))
            } else {
                Image(systemName: symbol)
                    .font(.system(size: size * 0.42, weight: .semibold))
                    .foregroundStyle(.white)
                    .shadow(color: .black.opacity(0.15), radius: 2, y: 1)
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

// MARK: - 渐变文字

struct GradientText: View {
    let text: String
    var font: Font = .largeTitle.bold()

    @Environment(\.palette) private var palette

    var body: some View {
        Text(text)
            .font(font)
            .foregroundStyle(palette.accentGradient)
            .shadow(color: palette.accent.opacity(0.35), radius: 10)
    }
}

// MARK: - 空状态

struct EmptyHint: View {
    let symbol: String
    let title: String
    var message: String?

    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: symbol)
                .font(.system(size: 36))
                .foregroundStyle(.secondary)
            Text(title).font(.headline)
            if let message {
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 30)
    }
}

// MARK: - 大号主按钮

struct BigActionButton: View {
    let title: String
    var symbol: String?
    var isEnabled = true
    let action: () -> Void

    @Environment(\.palette) private var palette

    var body: some View {
        Button {
            action()
        } label: {
            HStack(spacing: 8) {
                if let symbol { Image(systemName: symbol) }
                Text(title)
            }
            .font(.headline)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 6)
        }
        .buttonStyle(.glassProminent)
        .tint(palette.accent)
        .controlSize(.large)
        .disabled(!isEnabled)
    }
}

// MARK: - 中文字体

enum AppFont {
    /// 宋体风格（用于签诗、标题等需要古典氛围的地方）
    static func serif(_ size: CGFloat, weight: Font.Weight = .regular) -> Font {
        let name = weight == .bold || weight == .semibold || weight == .heavy ? "STSongti-SC-Bold" : "STSongti-SC-Regular"
        if UIFont(name: name, size: size) != nil {
            return .custom(name, size: size, relativeTo: .body)
        }
        return .system(size: size, weight: weight, design: .serif)
    }
}
