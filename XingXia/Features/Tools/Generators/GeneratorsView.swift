import SwiftUI
import SwiftData
import Security

struct GeneratorsView: View {
    enum Kind: String, CaseIterable, Identifiable {
        case color, password, name, letter
        var id: String { rawValue }
        var name: String { ["color": "配色", "password": "密码", "name": "名字", "letter": "字母"][rawValue]! }
    }

    @State private var kind: Kind = .color

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Picker("类型", selection: $kind.animation(.snappy)) {
                    ForEach(Kind.allCases) { Text($0.name).tag($0) }
                }
                .pickerStyle(.segmented)
                switch kind {
                case .color: ColorGenerator()
                case .password: PasswordGenerator()
                case .name: NameGenerator()
                case .letter: LetterGenerator()
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .contentMargins(.bottom, 24, for: .scrollContent)
        .cosmicBackground()
        .navigationTitle("灵感生成器")
    }
}

// MARK: - 配色

private struct ColorGenerator: View {
    enum Style: String, CaseIterable, Identifiable {
        case harmony, analogous, complementary, traditional
        var id: String { rawValue }
        var name: String { ["harmony": "随机和谐", "analogous": "邻近色", "complementary": "互补色", "traditional": "中国传统色"][rawValue]! }
    }

    @State private var style: Style = .harmony
    @State private var colors: [NamedColor] = []
    @State private var locked: Set<Int> = []
    @State private var copied: Int?

    var body: some View {
        VStack(spacing: 14) {
            Picker("风格", selection: $style) {
                ForEach(Style.allCases) { Text($0.name).tag($0) }
            }
            .pickerStyle(.menu)
            .frame(maxWidth: .infinity, alignment: .leading)
            .onChange(of: style) { _, _ in generate() }

            HStack(spacing: 6) {
                ForEach(Array(colors.enumerated()), id: \.offset) { index, color in
                    let isLocked = locked.contains(index)
                    let state: [String] = (isLocked ? ["已锁定"] : []) + (copied == index ? ["已拷贝"] : [])
                    // 锁头和拷贝按钮并列叠放，不嵌在拷贝按钮里：点锁头不会误拷贝，读屏也能单独聚焦
                    ZStack(alignment: .top) {
                        Button {
                            UIPasteboard.general.string = color.hexString
                            copied = index
                            Haptics.success()
                        } label: {
                            VStack(spacing: 6) {
                                RoundedRectangle(cornerRadius: 16)
                                    .fill(Color(color))
                                    .frame(height: 220)
                                    .shadow(color: Color(color).opacity(0.5), radius: 8)
                                Text(color.name.isEmpty ? " " : color.name).font(.caption.weight(.semibold)).lineLimit(1).foregroundStyle(.primary)
                                Text(copied == index ? "已拷贝" : color.hexString).font(.caption2.monospaced()).foregroundStyle(.secondary)
                                    .minimumScaleFactor(0.7)
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(color.name.isEmpty ? color.hexString : "\(color.name) \(color.hexString)")
                        .accessibilityValue(state.joined(separator: "，"))
                        .accessibilityHint("点按拷贝色值")

                        Button {
                            if isLocked { locked.remove(index) } else { locked.insert(index) }
                            Haptics.selection()
                        } label: {
                            Image(systemName: isLocked ? "lock.fill" : "lock.open")
                                .font(.caption)
                                .padding(6)
                                .background(Circle().fill(.black.opacity(0.25)))
                                .foregroundStyle(.white)
                                .frame(minWidth: 44, minHeight: 44)
                                .contentShape(.rect)
                        }
                        .buttonStyle(.plain)
                        .padding(.top, 2)
                        .accessibilityLabel(isLocked ? "解锁" : "锁定")
                        .accessibilityValue(color.name.isEmpty ? color.hexString : color.name)
                    }
                    // 每个色块自成一组，读屏按「色块 → 它的锁头」的顺序走，不会先读完五个色块再读五个锁头
                    .accessibilityElement(children: .contain)
                }
            }
            .glassCard(padding: 12)

            BigActionButton(title: "换一组", symbol: "paintpalette.fill") { generate() }
            Text("点颜色拷贝色值，点锁头保留喜欢的颜色").font(.caption).foregroundStyle(.secondary)
        }
        .onAppear { if colors.isEmpty { generate() } }
        .onShake { generate() }
    }

    private func generate() {
        copied = nil
        var next: [NamedColor] = []
        let baseHue = Double.random(in: 0..<1)
        for i in 0..<5 {
            if locked.contains(i), i < colors.count {
                next.append(colors[i])
                continue
            }
            switch style {
            case .traditional:
                let pool = GeneratorData.traditionalColors.filter { c in !next.contains(c) }
                next.append(pool.randomElement() ?? GeneratorData.traditionalColors[0])
            case .harmony:
                let h = (baseHue + Double(i) * 0.618034).truncatingRemainder(dividingBy: 1)
                next.append(Self.make(h: h, s: Double.random(in: 0.45...0.75), b: Double.random(in: 0.7...0.95)))
            case .analogous:
                let h = (baseHue + Double(i) * 0.06).truncatingRemainder(dividingBy: 1)
                next.append(Self.make(h: h, s: 0.4 + Double(i) * 0.1, b: 0.95 - Double(i) * 0.1))
            case .complementary:
                let h = i % 2 == 0 ? baseHue : (baseHue + 0.5).truncatingRemainder(dividingBy: 1)
                next.append(Self.make(h: h, s: 0.35 + Double(i) * 0.12, b: 0.95 - Double(i) * 0.08))
            }
        }
        withAnimation(.spring(response: 0.4, dampingFraction: 0.75)) { colors = next }
        SoundEngine.shared.play(.pop, volume: 0.6)
        Haptics.selection()
    }

    static func make(h: Double, s: Double, b: Double) -> NamedColor {
        let color = UIColor(hue: h, saturation: s, brightness: b, alpha: 1)
        var r: CGFloat = 0, g: CGFloat = 0, bl: CGFloat = 0, a: CGFloat = 0
        color.getRed(&r, green: &g, blue: &bl, alpha: &a)
        let hex = (UInt32(r * 255) << 16) | (UInt32(g * 255) << 8) | UInt32(bl * 255)
        return NamedColor(name: "", hex: hex)
    }
}

// MARK: - 密码

private struct PasswordGenerator: View {
    @AppStorage("pw.length") private var length = 16.0
    @AppStorage("pw.upper") private var upper = true
    @AppStorage("pw.lower") private var lower = true
    @AppStorage("pw.digits") private var digits = true
    @AppStorage("pw.symbols") private var symbols = true
    @AppStorage("pw.noSimilar") private var noSimilar = true
    @State private var password = ""
    @State private var copied = false
    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme

    private var charset: [Character] {
        var set = ""
        if upper { set += "ABCDEFGHIJKLMNOPQRSTUVWXYZ" }
        if lower { set += "abcdefghijklmnopqrstuvwxyz" }
        if digits { set += "0123456789" }
        if symbols { set += "!@#$%^&*-_=+?" }
        if noSimilar { set.removeAll { "O0Il1".contains($0) } }
        return Array(set)
    }

    private var entropy: Double {
        guard !charset.isEmpty else { return 0 }
        return length * log2(Double(charset.count))
    }

    var body: some View {
        VStack(spacing: 14) {
            VStack(spacing: 12) {
                Text(password.isEmpty ? "——" : password)
                    .font(.system(.title2, design: .monospaced).weight(.semibold))
                    .multilineTextAlignment(.center)
                    .minimumScaleFactor(0.5)
                    .lineLimit(2)
                    .textSelection(.enabled)
                    .contentTransition(.interpolate)
                HStack(spacing: 6) {
                    ForEach(0..<4, id: \.self) { i in
                        Capsule().fill(i < strengthLevel ? strengthColor : .primary.opacity(0.1)).frame(height: 5)
                    }
                }
                Text("强度：\(strengthName) · 约 \(Int(entropy)) 位熵").font(.caption).foregroundStyle(.secondary)
                Button(copied ? "已拷贝" : "拷贝密码", systemImage: copied ? "checkmark" : "doc.on.doc") {
                    // 拷贝的密码 60 秒后自动从剪贴板清除
                    UIPasteboard.general.setItems([[UIPasteboard.typeAutomatic: password]],
                                                  options: [.expirationDate: Date().addingTimeInterval(60)])
                    copied = true
                    Haptics.success()
                }
                .buttonStyle(.glass)
                .disabled(password.isEmpty)
            }
            .frame(maxWidth: .infinity)
            .glassCard(padding: 20)

            VStack(spacing: 12) {
                HStack {
                    Text("长度")
                    Slider(value: $length, in: 6...40, step: 1)
                    Text("\(Int(length))").monospacedDigit().frame(width: 30)
                }
                Toggle("大写字母 A-Z", isOn: $upper)
                Toggle("小写字母 a-z", isOn: $lower)
                Toggle("数字 0-9", isOn: $digits)
                Toggle("符号 !@#…", isOn: $symbols)
                Toggle("排除易混淆字符（0 O 1 l I）", isOn: $noSimilar)
            }
            .glassCard()
            .onChange(of: length) { _, _ in generate() }

            BigActionButton(title: "生成新密码", symbol: "key.fill", isEnabled: !charset.isEmpty) { generate() }
            Text("使用系统加密级随机数生成，仅在本机生成，不会上传。").font(.caption).foregroundStyle(.secondary)
        }
        .onAppear { if password.isEmpty { generate() } }
    }

    private var strengthLevel: Int { entropy < 40 ? 1 : (entropy < 60 ? 2 : (entropy < 80 ? 3 : 4)) }
    private var strengthName: String { ["", "弱", "一般", "强", "非常强"][strengthLevel] }
    /// 弱、一般、强用语义的红、橙、绿，非常强用青色
    private var strengthColor: Color {
        [.clear, Color.negative(scheme), Color.caution(scheme), Color.positive(scheme), palette.identity(Color(hex: 0x22D3EE))][strengthLevel]
    }

    private func generate() {
        let set = charset
        guard !set.isEmpty else { return }
        var bytes = [UInt8](repeating: 0, count: Int(length) * 2)
        _ = SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes)
        var result = ""
        var i = 0
        while result.count < Int(length) && i + 1 < bytes.count {
            let value = Int(bytes[i]) << 8 | Int(bytes[i + 1])
            // 拒绝采样，避免取模偏差
            if value < (65_536 / set.count) * set.count { result.append(set[value % set.count]) }
            i += 2
        }
        while result.count < Int(length) { result.append(set.randomElement()!) }
        withAnimation { password = result }
        copied = false
        SoundEngine.shared.play(.tick)
        Haptics.selection()
    }
}

// MARK: - 名字

private struct NameGenerator: View {
    enum Style: String, CaseIterable, Identifiable {
        case chineseMale, chineseFemale, chineseAny, english, nickname, pet
        var id: String { rawValue }
        var name: String {
            ["chineseMale": "中文·男", "chineseFemale": "中文·女", "chineseAny": "中文·中性", "english": "英文名", "nickname": "网名", "pet": "宠物名"][rawValue]!
        }
    }

    @State private var style: Style = .chineseAny
    @State private var surname = ""
    @State private var names: [String] = []
    @State private var copied: String?
    @FocusState private var editingSurname: Bool

    var body: some View {
        VStack(spacing: 14) {
            Picker("风格", selection: $style) {
                ForEach(Style.allCases) { Text($0.name).tag($0) }
            }
            .pickerStyle(.menu)
            .frame(maxWidth: .infinity, alignment: .leading)
            .onChange(of: style) { _, _ in generate() }

            if [.chineseMale, .chineseFemale, .chineseAny].contains(style) {
                TextField("指定姓氏（可选）", text: $surname)
                    .focused($editingSurname)
                    .padding(12)
                    .glassEffect(.regular, in: .capsule)
                    .onSubmit { generate() }
            }

            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                ForEach(names, id: \.self) { name in
                    Button {
                        UIPasteboard.general.string = name
                        copied = name
                        Haptics.success()
                    } label: {
                        VStack(spacing: 4) {
                            Text(name).font(.title3.weight(.semibold)).foregroundStyle(.primary).lineLimit(1).minimumScaleFactor(0.6)
                            Text(copied == name ? "已拷贝" : "点按拷贝").font(.caption2).foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 18))
                    }
                    .buttonStyle(.plain)
                    .transition(.scale.combined(with: .opacity))
                }
            }

            BigActionButton(title: "换一批", symbol: "text.badge.star") { generate() }
        }
        .onAppear { if names.isEmpty { generate() } }
        .onShake(isEnabled: !editingSurname) { generate() }
    }

    private func generate() {
        var set = Set<String>()
        while set.count < 8 { set.insert(make()) }
        withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) { names = Array(set) }
        copied = nil
        SoundEngine.shared.play(.pop, volume: 0.6)
    }

    private func make() -> String {
        let data = GeneratorData.self
        switch style {
        case .chineseMale, .chineseFemale, .chineseAny:
            let family = surname.trimmingCharacters(in: .whitespaces).isEmpty
                ? (Int.random(in: 0..<20) == 0 ? data.compoundSurnames.randomElement()! : data.surnames.randomElement()!)
                : surname.trimmingCharacters(in: .whitespaces)
            let pool = style == .chineseMale ? data.maleChars : (style == .chineseFemale ? data.femaleChars : data.neutralChars + data.maleChars.prefix(20) + data.femaleChars.prefix(20))
            let given = Bool.random() ? pool.randomElement()! : pool.randomElement()! + pool.randomElement()!
            return family + given
        case .english:
            return (Bool.random() ? data.englishMale : data.englishFemale).randomElement()!
        case .nickname:
            return data.nickAdjectives.randomElement()! + data.nickNouns.randomElement()!
        case .pet:
            return data.petNames.randomElement()!
        }
    }
}

// MARK: - 字母

private struct LetterGenerator: View {
    @State private var letter = "?"
    @State private var lowercase = false
    @State private var history: [String] = []
    @Environment(\.palette) private var palette
    /// 大字母跟着字号放大，封顶 1.6 倍
    @ScaledMetric(relativeTo: .largeTitle) private var letterSize: CGFloat = 140

    var body: some View {
        VStack(spacing: 16) {
            Text(letter)
                .font(.system(size: min(letterSize, 140 * 1.6), weight: .black, design: .rounded))
                .foregroundStyle(palette.accentGradient)
                .shadow(color: palette.accent.opacity(0.5), radius: 20)
                .contentTransition(.numericText())
                .frame(maxWidth: .infinity, minHeight: 200)
                .glassCard()
            Toggle("小写字母", isOn: $lowercase)
                .glassCard(padding: 14)
            BigActionButton(title: "随机一个字母", symbol: "textformat.abc") {
                let letters = lowercase ? "abcdefghijklmnopqrstuvwxyz" : "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                let next = String(letters.randomElement()!)
                withAnimation(.spring(response: 0.3, dampingFraction: 0.6)) { letter = next }
                history.insert(next, at: 0)
                if history.count > 12 { history.removeLast() }
                SoundEngine.shared.play(.pop)
                Haptics.selection()
                Announce.post(next)
            }
            if !history.isEmpty {
                Text(history.joined(separator: "  ")).font(.subheadline.monospaced()).foregroundStyle(.secondary)
            }
        }
    }
}
