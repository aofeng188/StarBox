import SwiftUI
import SwiftData

struct RPSView: View {
    enum Hand: Int, CaseIterable, Identifiable {
        case rock, scissors, paper
        var id: Int { rawValue }
        var emoji: String { ["✊", "✌️", "✋"][rawValue] }
        var name: String { ["石头", "剪刀", "布"][rawValue] }
        /// 能打败自己的手势
        var counter: Hand { Hand(rawValue: (rawValue + 2) % 3)! }

        func outcome(against other: Hand) -> Int {
            if self == other { return 0 }
            return other.counter == self ? 1 : -1
        }
    }

    enum Match: String, CaseIterable, Identifiable {
        case free, bo3, bo5
        var id: String { rawValue }
        var name: String { ["free": "自由对战", "bo3": "三局两胜", "bo5": "五局三胜"][rawValue]! }
        var target: Int? { ["free": nil, "bo3": 2, "bo5": 3][rawValue]! }
    }

    /// 结果文字的语义，颜色按深浅模式在 body 里取
    enum Tone { case neutral, win, lose, champion }

    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var colorScheme
    @AppStorage("rps.wins") private var totalWins = 0
    @AppStorage("rps.losses") private var totalLosses = 0
    @AppStorage("rps.draws") private var totalDraws = 0
    @AppStorage("rps.smart") private var smartAI = false

    @State private var match: Match = .bo3
    @State private var myScore = 0
    @State private var aiScore = 0
    @State private var myHand: Hand?
    @State private var aiHand: Hand?
    @State private var shuffleHand: Hand = .rock
    @State private var isPlaying = false
    @State private var chant = ""
    @State private var resultText = ""
    @State private var resultTone: Tone = .neutral
    @State private var matchOver = false
    @State private var confetti = 0
    @State private var history: [Hand] = []
    /// 读心模式：记录「上一手 → 下一手」的频率
    @State private var transitions: [Int: [Int]] = [:]

    var body: some View {
        StageScaffold {
            Picker("赛制", selection: $match) {
                ForEach(Match.allCases) { Text($0.name).tag($0) }
            }
            .pickerStyle(.segmented)
            .onChange(of: match) { _, _ in resetMatch() }
            .padding(.top, 8)

            HStack {
                scoreColumn(title: "你", score: myScore)
                Spacer()
                VStack(spacing: 2) {
                    Text("VS").font(.title3.bold()).foregroundStyle(.secondary)
                    if let target = match.target { Text("先赢 \(target) 局").font(.caption2).foregroundStyle(.secondary) }
                }
                Spacer()
                scoreColumn(title: smartAI ? "星匣·读心" : "星匣", score: aiScore)
            }
            .glassCard(padding: 16)
        } stage: {
            // 对战区
            ScaleToFit(design: CGSize(width: 350, height: 232)) {
                VStack(spacing: 18) {
                    HStack(spacing: 0) {
                        handBubble(myHand?.emoji ?? "🤔", flipped: false)
                        Spacer()
                        handBubble(isPlaying ? shuffleHand.emoji : (aiHand?.emoji ?? "🤖"), flipped: true)
                    }
                    .padding(.horizontal, 20)
                    .frame(height: 170)

                    Text(isPlaying ? chant : resultText)
                        .font(.system(size: 30, weight: .heavy, design: .rounded))
                        .foregroundStyle(isPlaying ? AnyShapeStyle(.primary) : resultStyle)
                        .contentTransition(.numericText())
                        .frame(height: 44)
                }
            }
        } bottom: {
            if matchOver {
                Button { resetMatch() } label: {
                    // 文字色写在 label 里（同 BigActionButton），按钮样式不会再盖掉
                    Label("再来一场", systemImage: "arrow.counterclockwise").foregroundStyle(palette.onAccent)
                }
                .buttonStyle(.glassProminent)
                .tint(palette.accent)
                .controlSize(.large)
            } else {
                HStack(spacing: 16) {
                    ForEach(Hand.allCases) { hand in
                        Button {
                            play(hand)
                        } label: {
                            VStack(spacing: 6) {
                                Text(hand.emoji).font(.system(size: 46))
                                Text(hand.name).font(.caption.weight(.semibold)).foregroundStyle(.primary)
                            }
                            .frame(width: 96, height: 110)
                        }
                        .buttonStyle(.plain)
                        .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 26))
                        .disabled(isPlaying)
                        .accessibilityLabel("出\(hand.name)")
                    }
                }
            }

            HStack {
                Toggle(isOn: $smartAI) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("读心模式")
                        Text("星匣会根据你的出拳习惯来预测").font(.caption2).foregroundStyle(.secondary)
                    }
                }
            }
            .glassCard(padding: 14)

            Text("总战绩 \(totalWins) 胜 \(totalLosses) 负 \(totalDraws) 平 · 胜率 \(winRate)")
                .font(.caption).foregroundStyle(.secondary)
                .padding(.bottom, 12)
        }
        .overlay { ConfettiView(trigger: confetti) }
        .cosmicBackground()
        .navigationTitle("石头剪刀布")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var resultStyle: AnyShapeStyle {
        switch resultTone {
        case .neutral: AnyShapeStyle(.secondary)
        case .win: AnyShapeStyle(Color.positive(colorScheme))
        case .lose: AnyShapeStyle(Color.negative(colorScheme))
        case .champion: AnyShapeStyle(Color.gold(colorScheme))
        }
    }

    private var winRate: String {
        let total = totalWins + totalLosses + totalDraws
        return total == 0 ? "—" : "\(Int(Double(totalWins) / Double(total) * 100))%"
    }

    private func scoreColumn(title: String, score: Int) -> some View {
        VStack(spacing: 2) {
            Text("\(score)").font(.system(size: 40, weight: .bold, design: .rounded)).monospacedDigit()
                .contentTransition(.numericText(value: Double(score)))
            Text(title).font(.caption).foregroundStyle(.secondary)
        }
        .frame(minWidth: 90)
    }

    private func handBubble(_ emoji: String, flipped: Bool) -> some View {
        Text(emoji)
            .font(.system(size: 84))
            .scaleEffect(x: flipped ? -1 : 1)
            .frame(width: 140, height: 140)
            .glassEffect(.regular.tint(palette.accent.opacity(0.1)), in: .circle)
    }

    private func aiChoice() -> Hand {
        guard smartAI, let last = history.last, let next = transitions[last.rawValue], next.count >= 2 else {
            return Hand.allCases.randomElement()!
        }
        // 预测你最可能出的手势，然后出能克制它的；保留 25% 的随机，避免被反向利用
        if Double.random(in: 0...1) < 0.25 { return Hand.allCases.randomElement()! }
        let counts = Dictionary(next.map { ($0, 1) }, uniquingKeysWith: +)
        let predicted = Hand(rawValue: counts.max { $0.value < $1.value }?.key ?? 0)!
        return predicted.counter
    }

    private func play(_ hand: Hand) {
        guard !isPlaying else { return }
        isPlaying = true
        myHand = hand
        aiHand = nil
        let ai = aiChoice()
        Task {
            for (i, word) in ["石头", "剪刀", "布！"].enumerated() {
                withAnimation(.snappy) { chant = word }
                SoundEngine.shared.play(.drum, volume: 0.5)
                Haptics.impact(0.5 + Double(i) * 0.2)
                for _ in 0..<3 {
                    shuffleHand = Hand.allCases.randomElement()!
                    try? await Task.sleep(for: .milliseconds(85))
                }
            }
            aiHand = ai
            isPlaying = false
            settle(mine: hand, ai: ai)
        }
    }

    private func settle(mine: Hand, ai: Hand) {
        if let last = history.last { transitions[last.rawValue, default: []].append(mine.rawValue) }
        history.append(mine)
        let outcome = mine.outcome(against: ai)
        withAnimation(.spring(response: 0.4, dampingFraction: 0.6)) {
            switch outcome {
            case 1:
                myScore += 1
                totalWins += 1
                resultText = "你赢了！"
                resultTone = .win
            case -1:
                aiScore += 1
                totalLosses += 1
                resultText = "你输了"
                resultTone = .lose
            default:
                totalDraws += 1
                resultText = "平局"
                resultTone = .neutral
            }
        }
        if outcome == 1 { SoundEngine.shared.play(.success); Haptics.success() }
        else if outcome == -1 { SoundEngine.shared.play(.fail); Haptics.error() }
        else { SoundEngine.shared.play(.tap); Haptics.tap() }

        var announcement = "你出\(mine.name)，星匣出\(ai.name)，\(["你输了", "平局", "你赢了"][outcome + 1])"
        if let target = match.target, myScore >= target || aiScore >= target {
            matchOver = true
            let won = myScore > aiScore
            resultText = won ? "🏆 你赢下了比赛！" : "星匣赢下了比赛"
            resultTone = won ? .champion : .lose
            if won { confetti += 1 }
            announcement += "。\(myScore) 比 \(aiScore)，\(won ? "你赢下了比赛" : "星匣赢下了比赛")"
            HistoryLog.add(context, tool: ToolKind.rps.rawValue, title: match.name, result: "\(myScore) : \(aiScore) \(won ? "胜" : "负")")
        }
        Announce.post(announcement)
    }

    private func resetMatch() {
        myScore = 0
        aiScore = 0
        myHand = nil
        aiHand = nil
        resultText = ""
        matchOver = false
    }
}
