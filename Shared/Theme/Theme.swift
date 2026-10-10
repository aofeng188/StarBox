import SwiftUI

extension Color {
    init(hex: UInt32, opacity: Double = 1) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: opacity)
    }

    init(_ named: NamedColor) {
        self.init(hex: named.hex)
    }
}

/// 一套配色（主题的深色或浅色版本）
struct ThemePalette: Sendable {
    let isDark: Bool
    let backgroundTop: Color
    let backgroundBottom: Color
    /// 星云/极光色块，至少 4 个
    let nebula: [Color]
    let accent: Color
    let accentSecondary: Color
    let star: Color

    var accentGradient: LinearGradient {
        LinearGradient(colors: [accent, accentSecondary], startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    var backgroundGradient: LinearGradient {
        LinearGradient(colors: [backgroundTop, backgroundBottom], startPoint: .top, endPoint: .bottom)
    }

    var glassTint: Color { accent.opacity(isDark ? 0.08 : 0.05) }

    /// 在卡片、图标等处轮换使用的一组彩色
    var spectrum: [Color] { [accent, accentSecondary] + nebula }

    func color(at index: Int) -> Color {
        let colors = spectrum
        return colors[((index % colors.count) + colors.count) % colors.count]
    }
}

struct AppTheme: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let subtitle: String
    let symbol: String
    let dark: ThemePalette
    let light: ThemePalette

    func palette(for scheme: ColorScheme) -> ThemePalette { scheme == .dark ? dark : light }

    static func == (lhs: AppTheme, rhs: AppTheme) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }

    static func find(_ id: String?) -> AppTheme { all.first { $0.id == id } ?? nebula }

    /// 五行 → 主题（「跟随幸运色」规则使用）
    static func forElement(_ element: WuXing) -> AppTheme {
        switch element {
        case .wood: aurora
        case .fire: redGiant
        case .earth: solarGold
        case .metal: eventHorizon
        case .water: abyss
        }
    }

    /// 星座元素 → 主题
    static func forZodiac(_ element: ZodiacElement) -> AppTheme {
        switch element {
        case .fire: sunset
        case .earth: solarGold
        case .air: nebula
        case .water: abyss
        }
    }

    static let nebula = AppTheme(
        id: "nebula", name: "星云紫", subtitle: "猎户座星云的梦幻紫粉", symbol: "sparkles",
        dark: .init(isDark: true, backgroundTop: Color(hex: 0x0B0720), backgroundBottom: Color(hex: 0x1A0B3B),
                    nebula: [Color(hex: 0x7C5CFF), Color(hex: 0xD946EF), Color(hex: 0x3B82F6), Color(hex: 0xF472B6)],
                    accent: Color(hex: 0xA78BFA), accentSecondary: Color(hex: 0xF472B6), star: .white),
        light: .init(isDark: false, backgroundTop: Color(hex: 0xF6F1FF), backgroundBottom: Color(hex: 0xFDEAF5),
                     nebula: [Color(hex: 0xC4B5FD), Color(hex: 0xF5D0FE), Color(hex: 0xBFDBFE), Color(hex: 0xFBCFE8)],
                     accent: Color(hex: 0x7C3AED), accentSecondary: Color(hex: 0xDB2777), star: Color(hex: 0x8B5CF6))
    )

    static let aurora = AppTheme(
        id: "aurora", name: "极光绿", subtitle: "北极圈夜空的流动绿光", symbol: "wind",
        dark: .init(isDark: true, backgroundTop: Color(hex: 0x02121A), backgroundBottom: Color(hex: 0x05282A),
                    nebula: [Color(hex: 0x10B981), Color(hex: 0x22D3EE), Color(hex: 0x34F5C5), Color(hex: 0x6366F1)],
                    accent: Color(hex: 0x34F5C5), accentSecondary: Color(hex: 0x38BDF8), star: .white),
        light: .init(isDark: false, backgroundTop: Color(hex: 0xECFEF7), backgroundBottom: Color(hex: 0xE2F6FF),
                     nebula: [Color(hex: 0xA7F3D0), Color(hex: 0xA5F3FC), Color(hex: 0x99F6E4), Color(hex: 0xC7D2FE)],
                     accent: Color(hex: 0x059669), accentSecondary: Color(hex: 0x0284C7), star: Color(hex: 0x10B981))
    )

    static let sunset = AppTheme(
        id: "sunset", name: "日落橙", subtitle: "地平线上最后一抹晚霞", symbol: "sun.horizon.fill",
        dark: .init(isDark: true, backgroundTop: Color(hex: 0x1A0A0F), backgroundBottom: Color(hex: 0x3A1022),
                    nebula: [Color(hex: 0xFF7A45), Color(hex: 0xFF3D7F), Color(hex: 0xFFC15E), Color(hex: 0x9D4EDD)],
                    accent: Color(hex: 0xFF9F43), accentSecondary: Color(hex: 0xFF4D6D), star: Color(hex: 0xFFE8D6)),
        light: .init(isDark: false, backgroundTop: Color(hex: 0xFFF4EC), backgroundBottom: Color(hex: 0xFFE6EE),
                     nebula: [Color(hex: 0xFED7AA), Color(hex: 0xFBCFE8), Color(hex: 0xFDE68A), Color(hex: 0xE9D5FF)],
                     accent: Color(hex: 0xEA580C), accentSecondary: Color(hex: 0xE11D48), star: Color(hex: 0xF97316))
    )

    static let abyss = AppTheme(
        id: "abyss", name: "深海蓝", subtitle: "万米之下的幽蓝微光", symbol: "drop.fill",
        dark: .init(isDark: true, backgroundTop: Color(hex: 0x020817), backgroundBottom: Color(hex: 0x071C46),
                    nebula: [Color(hex: 0x1D4ED8), Color(hex: 0x06B6D4), Color(hex: 0x6366F1), Color(hex: 0x0EA5E9)],
                    accent: Color(hex: 0x60A5FA), accentSecondary: Color(hex: 0x22D3EE), star: Color(hex: 0xE0F2FE)),
        light: .init(isDark: false, backgroundTop: Color(hex: 0xEEF6FF), backgroundBottom: Color(hex: 0xE0F2FE),
                     nebula: [Color(hex: 0xBFDBFE), Color(hex: 0xA5F3FC), Color(hex: 0xC7D2FE), Color(hex: 0xBAE6FD)],
                     accent: Color(hex: 0x2563EB), accentSecondary: Color(hex: 0x0891B2), star: Color(hex: 0x3B82F6))
    )

    static let redGiant = AppTheme(
        id: "redGiant", name: "红巨星", subtitle: "燃烧殆尽前最炽烈的光", symbol: "flame.fill",
        dark: .init(isDark: true, backgroundTop: Color(hex: 0x160305), backgroundBottom: Color(hex: 0x3A0A0C),
                    nebula: [Color(hex: 0xEF4444), Color(hex: 0xF97316), Color(hex: 0xBE185D), Color(hex: 0xFBBF24)],
                    accent: Color(hex: 0xFF6B6B), accentSecondary: Color(hex: 0xFFB86B), star: Color(hex: 0xFFE4E1)),
        light: .init(isDark: false, backgroundTop: Color(hex: 0xFFF1F0), backgroundBottom: Color(hex: 0xFFF4E6),
                     nebula: [Color(hex: 0xFECACA), Color(hex: 0xFED7AA), Color(hex: 0xFBCFE8), Color(hex: 0xFDE68A)],
                     accent: Color(hex: 0xDC2626), accentSecondary: Color(hex: 0xEA580C), star: Color(hex: 0xEF4444))
    )

    static let sakura = AppTheme(
        id: "sakura", name: "樱花粉", subtitle: "春夜里飘落的花瓣", symbol: "camera.macro",
        dark: .init(isDark: true, backgroundTop: Color(hex: 0x1A0915), backgroundBottom: Color(hex: 0x351333),
                    nebula: [Color(hex: 0xFF8FC7), Color(hex: 0xC084FC), Color(hex: 0xFDA4AF), Color(hex: 0x818CF8)],
                    accent: Color(hex: 0xFF8FC7), accentSecondary: Color(hex: 0xC084FC), star: Color(hex: 0xFFE4F1)),
        light: .init(isDark: false, backgroundTop: Color(hex: 0xFFF5FA), backgroundBottom: Color(hex: 0xFBEFFF),
                     nebula: [Color(hex: 0xFBCFE8), Color(hex: 0xE9D5FF), Color(hex: 0xFECDD3), Color(hex: 0xE0E7FF)],
                     accent: Color(hex: 0xDB2777), accentSecondary: Color(hex: 0x9333EA), star: Color(hex: 0xEC4899))
    )

    static let eventHorizon = AppTheme(
        id: "eventHorizon", name: "黑洞", subtitle: "事件视界边缘的金色吸积盘", symbol: "circle.circle.fill",
        dark: .init(isDark: true, backgroundTop: Color(hex: 0x000000), backgroundBottom: Color(hex: 0x0A0A10),
                    nebula: [Color(hex: 0x3F3F55), Color(hex: 0xE8C77A), Color(hex: 0x6B5B95), Color(hex: 0x1F2937)],
                    accent: Color(hex: 0xF5D78E), accentSecondary: Color(hex: 0xE5E7EB), star: Color(hex: 0xF5F5F5)),
        light: .init(isDark: false, backgroundTop: Color(hex: 0xF7F7F8), backgroundBottom: Color(hex: 0xEDEDF0),
                     nebula: [Color(hex: 0xE5E7EB), Color(hex: 0xF3E8C8), Color(hex: 0xDDD6FE), Color(hex: 0xD1D5DB)],
                     accent: Color(hex: 0x3F3F46), accentSecondary: Color(hex: 0xA16207), star: Color(hex: 0x71717A))
    )

    static let solarGold = AppTheme(
        id: "solarGold", name: "鎏金", subtitle: "日冕与金色星尘", symbol: "sun.max.fill",
        dark: .init(isDark: true, backgroundTop: Color(hex: 0x120C02), backgroundBottom: Color(hex: 0x2A1B05),
                    nebula: [Color(hex: 0xF59E0B), Color(hex: 0xFCD34D), Color(hex: 0xB45309), Color(hex: 0xF97316)],
                    accent: Color(hex: 0xFFD166), accentSecondary: Color(hex: 0xF4A261), star: Color(hex: 0xFFF7E0)),
        light: .init(isDark: false, backgroundTop: Color(hex: 0xFFFBEB), backgroundBottom: Color(hex: 0xFEF3C7),
                     nebula: [Color(hex: 0xFDE68A), Color(hex: 0xFEF3C7), Color(hex: 0xFCD34D), Color(hex: 0xFED7AA)],
                     accent: Color(hex: 0xB45309), accentSecondary: Color(hex: 0xD97706), star: Color(hex: 0xD97706))
    )

    static let all: [AppTheme] = [nebula, aurora, sunset, abyss, redGiant, sakura, eventHorizon, solarGold]

    /// App 当前生效的主题会写到共享存储，供小组件使用
    static let sharedKey = "resolvedThemeID"
    static var shared: AppTheme { find(AppGroup.defaults.string(forKey: sharedKey)) }
}

private struct PaletteKey: EnvironmentKey {
    static let defaultValue: ThemePalette = AppTheme.nebula.dark
}

extension EnvironmentValues {
    var palette: ThemePalette {
        get { self[PaletteKey.self] }
        set { self[PaletteKey.self] = newValue }
    }
}
