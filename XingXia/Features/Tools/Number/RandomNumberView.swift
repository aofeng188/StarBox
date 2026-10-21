import SwiftUI
import SwiftData

struct RandomNumberView: View {
    enum Mode: String, CaseIterable, Identifiable {
        case custom, dice, hundred, ssq, dlt
        var id: String { rawValue }
        var name: String {
            switch self {
            case .custom: "自定义"
            case .dice: "1–6"
            case .hundred: "1–100"
            case .ssq: "双色球"
            case .dlt: "大乐透"
            }
        }
    }

    struct Ball: Hashable {
        let value: Int
        let isSpecial: Bool
    }

    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @AppStorage("number.min") private var minValue = 1
    @AppStorage("number.max") private var maxValue = 100
    @AppStorage("number.count") private var count = 1
    @AppStorage("number.unique") private var unique = true
    @AppStorage("number.sorted") private var sorted = false
    @State private var mode: Mode = .custom
    @State private var balls: [Ball] = []
    @State private var isRolling = false
    @State private var history: [String] = []
    @FocusState private var focused: Bool
    /// 单个结果的大数字跟着字号放大，封顶 1.6 倍
    @ScaledMetric(relativeTo: .largeTitle) private var numberSize: CGFloat = 110

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Picker("模式", selection: $mode) {
                    ForEach(Mode.allCases) { Text($0.name).tag($0) }
                }
                .pickerStyle(.segmented)
                .disabled(isRolling)
                .onChange(of: mode) { _, _ in balls = [] }

                // 结果
                VStack(spacing: 16) {
                    if balls.isEmpty {
                        Text("?")
                            .font(.system(size: min(numberSize, 110 * 1.6) * 96 / 110, weight: .heavy, design: .rounded))
                            .foregroundStyle(palette.accentGradient.opacity(0.5))
                    } else if balls.count == 1 {
                        Text("\(balls[0].value)")
                            .font(.system(size: min(numberSize, 110 * 1.6), weight: .heavy, design: .rounded))
                            .monospacedDigit()
                            .foregroundStyle(palette.accentGradient)
                            .contentTransition(.numericText(value: Double(balls[0].value)))
                            .shadow(color: palette.accent.opacity(0.5), radius: 16)
                            .minimumScaleFactor(0.4)
                            .lineLimit(1)
                    } else {
                        FlowLayout(spacing: 10) {
                            ForEach(Array(balls.enumerated()), id: \.offset) { _, ball in
                                BallView(value: ball.value, isSpecial: ball.isSpecial, lotteryStyle: mode == .ssq || mode == .dlt)
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                    Text(caption).font(.footnote).foregroundStyle(.secondary)
                }
                .frame(maxWidth: .infinity, minHeight: 200)
                .glassCard(padding: 22)

                if mode == .custom {
                    VStack(spacing: 14) {
                        HStack(spacing: 12) {
                            NumberField(title: "最小", value: $minValue, focused: $focused)
                            Image(systemName: "arrow.left.and.right").foregroundStyle(.secondary)
                            NumberField(title: "最大", value: $maxValue, focused: $focused)
                        }
                        Stepper("生成 \(count) 个", value: $count, in: 1...30)
                        Toggle("不重复", isOn: $unique)
                        Toggle("结果排序", isOn: $sorted)
                    }
                    .glassCard()
                }

                BigActionButton(title: "生成", symbol: "sparkles", isEnabled: !isRolling) { generate() }

                if mode == .ssq || mode == .dlt {
                    Text("号码随机生成，仅供娱乐，请理性购彩。").font(.caption).foregroundStyle(.secondary)
                }

                if !history.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        SectionHeader(title: "历史", symbol: "clock")
                        ForEach(Array(history.enumerated().reversed()), id: \.offset) { _, item in
                            Text(item).font(.subheadline.monospacedDigit()).foregroundStyle(.secondary)
                                .textSelection(.enabled)
                        }
                    }
                    .glassCard()
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .contentMargins(.bottom, 24, for: .scrollContent)
        .scrollDismissesKeyboard(.immediately)
        .cosmicBackground()
        .navigationTitle("随机数字")
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("完成") { focused = false }
            }
        }
        .onShake { generate() }
    }

    private var caption: String {
        switch mode {
        case .custom: "\(min(minValue, maxValue)) 到 \(max(minValue, maxValue)) 之间 · \(count) 个\(unique ? " · 不重复" : "")"
        case .dice: "掷一个六面骰"
        case .hundred: "1 到 100 之间"
        case .ssq: "红球 6 个（1–33）+ 蓝球 1 个（1–16）"
        case .dlt: "前区 5 个（1–35）+ 后区 2 个（1–12）"
        }
    }

    private func pick(_ range: ClosedRange<Int>, count: Int, unique: Bool) -> [Int] {
        if unique {
            let n = min(count, range.count)
            if range.count > 5_000 {
                // 范围很大时不要生成整个数组
                var picked: [Int] = []
                var seen = Set<Int>()
                while picked.count < n {
                    let value = Int.random(in: range)
                    if seen.insert(value).inserted { picked.append(value) }
                }
                return picked
            }
            return Array(Array(range).shuffled().prefix(n))
        }
        return (0..<count).map { _ in Int.random(in: range) }
    }

    private func finalBalls() -> [Ball] {
        switch mode {
        case .custom:
            let low = min(minValue, maxValue), high = max(minValue, maxValue)
            var values = pick(low...high, count: count, unique: unique)
            if sorted { values.sort() }
            return values.map { Ball(value: $0, isSpecial: false) }
        case .dice: return [Ball(value: Int.random(in: 1...6), isSpecial: false)]
        case .hundred: return [Ball(value: Int.random(in: 1...100), isSpecial: false)]
        case .ssq:
            return pick(1...33, count: 6, unique: true).sorted().map { Ball(value: $0, isSpecial: false) }
                + [Ball(value: Int.random(in: 1...16), isSpecial: true)]
        case .dlt:
            return pick(1...35, count: 5, unique: true).sorted().map { Ball(value: $0, isSpecial: false) }
                + pick(1...12, count: 2, unique: true).sorted().map { Ball(value: $0, isSpecial: true) }
        }
    }

    private func generate() {
        guard !isRolling else { return }
        focused = false
        isRolling = true
        let final = finalBalls()
        Task {
            for step in 0..<14 {
                let shuffled = final.map { Ball(value: Int.random(in: 1...max(9, final.map(\.value).max() ?? 9)), isSpecial: $0.isSpecial) }
                withAnimation(.snappy(duration: 0.08)) { balls = shuffled }
                SoundEngine.shared.play(.tick, volume: 0.4)
                if step % 2 == 0 { Haptics.selection() }
                try? await Task.sleep(for: .milliseconds(45 + step * 6))
            }
            withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) { balls = final }
            SoundEngine.shared.play(.pop)
            Haptics.success()
            isRolling = false
            Announce.post(announcement(for: final))
            let text = final.map { String(format: $0.isSpecial ? "[%02d]" : "%d", $0.value) }.joined(separator: " ")
            history.append(text)
            HistoryLog.add(context, tool: ToolKind.number.rawValue, title: mode.name, result: text)
        }
    }

    /// 读屏播报用的结果文字；彩票模式分开念红蓝球 / 前后区
    private func announcement(for values: [Ball]) -> String {
        let normal = values.filter { !$0.isSpecial }.map { "\($0.value)" }.joined(separator: "、")
        let special = values.filter(\.isSpecial).map { "\($0.value)" }.joined(separator: "、")
        switch mode {
        case .ssq: return "红球 \(normal)，蓝球 \(special)"
        case .dlt: return "前区 \(normal)，后区 \(special)"
        default: return "结果：\(normal)"
        }
    }
}

private struct NumberField: View {
    let title: String
    @Binding var value: Int
    var focused: FocusState<Bool>.Binding

    var body: some View {
        VStack(spacing: 4) {
            Text(title).font(.caption).foregroundStyle(.secondary)
            TextField(title, value: $value, format: .number)
                .keyboardType(.numberPad)
                .multilineTextAlignment(.center)
                .font(.title2.bold().monospacedDigit())
                .focused(focused)
                .padding(.vertical, 8)
                .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 14))
        }
    }
}

struct BallView: View {
    let value: Int
    let isSpecial: Bool
    let lotteryStyle: Bool
    @Environment(\.palette) private var palette

    var body: some View {
        let colors: [Color] = lotteryStyle
            ? (isSpecial ? [Color(hex: 0x60A5FA), Color(hex: 0x1D4ED8)] : [Color(hex: 0xF87171), Color(hex: 0xB91C1C)])
            : [palette.accent, palette.accentSecondary]
        Text(String(format: lotteryStyle ? "%02d" : "%d", value))
            .font(.title3.bold().monospacedDigit())
            // 彩票球是固定的深红、深蓝；普通球用主题强调色，文字跟着 onAccent
            .foregroundStyle(lotteryStyle ? .white : palette.onAccent)
            .frame(minWidth: 52, minHeight: 52)
            .padding(.horizontal, value > 999 ? 8 : 0)
            .background(Circle().fill(RadialGradient(colors: [colors[0], colors[1]], center: .init(x: 0.35, y: 0.3), startRadius: 2, endRadius: 40)))
            .overlay(alignment: .topLeading) {
                Ellipse().fill(.white.opacity(0.45)).frame(width: 16, height: 9).offset(x: 12, y: 9)
            }
            .shadow(color: colors[1].opacity(0.5), radius: 6, y: 3)
            .contentTransition(.numericText(value: Double(value)))
    }
}
