import SwiftUI
import WidgetKit

/// 小组件背景：与 App 当前主题一致的星空渐变
struct WidgetCosmicBackground: View {
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = AppTheme.shared.palette(for: scheme)
        ZStack {
            palette.backgroundGradient
            Circle().fill(palette.nebula[0].opacity(palette.isDark ? 0.55 : 0.7)).frame(width: 180).blur(radius: 50).offset(x: -60, y: -50)
            Circle().fill(palette.nebula[1].opacity(palette.isDark ? 0.45 : 0.6)).frame(width: 160).blur(radius: 50).offset(x: 80, y: 60)
            Canvas { context, size in
                guard palette.isDark else { return }
                var rng = SeededGenerator(seed: 7)
                for _ in 0..<24 {
                    let r = rng.double(0.4...1.2)
                    context.fill(Path(ellipseIn: CGRect(x: rng.unit() * size.width, y: rng.unit() * size.height, width: r * 2, height: r * 2)),
                                 with: .color(.white.opacity(rng.double(0.3...0.9))))
                }
            }
        }
    }
}

struct WidgetPalette {
    static func current(_ scheme: ColorScheme) -> ThemePalette { AppTheme.shared.palette(for: scheme) }
}

/// 小组件里的分数环
struct WidgetScoreRing: View {
    let score: Int
    var lineWidth: CGFloat = 7
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let palette = WidgetPalette.current(scheme)
        ZStack {
            Circle().stroke(.primary.opacity(0.12), lineWidth: lineWidth)
            Circle()
                .trim(from: 0, to: Double(score) / 100)
                .stroke(AngularGradient(colors: [palette.accent, palette.accentSecondary, palette.accent], center: .center),
                        style: StrokeStyle(lineWidth: lineWidth, lineCap: .round))
                .rotationEffect(.degrees(-90))
                .widgetAccentable()
            Text("\(score)")
                .font(.system(size: 26, weight: .bold, design: .rounded))
                .monospacedDigit()
                .minimumScaleFactor(0.6)
        }
    }
}

/// App Group 不可用（例如免费签名）时，需要和 App 共用数据的小组件显示这个，点一下打开 App
struct WidgetOpenAppView: View {
    let symbol: String
    let text: String

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: symbol).font(.title2)
            Text(text).font(.caption).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
