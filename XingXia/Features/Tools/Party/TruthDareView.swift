import SwiftUI
import SwiftData

private struct TruthDareContent: Decodable {
    let truth: [[String]]
    let dare: [[String]]
    static let shared: TruthDareContent = ContentLibrary.load("truthdare")
}

struct TruthDareView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Query(sort: \CustomPrompt.createdAt) private var customPrompts: [CustomPrompt]
    @AppStorage("td.level") private var level = 0
    @AppStorage("td.players") private var playersText = ""

    @State private var kind: String?
    @State private var prompt = ""
    @State private var isFaceUp = false
    @State private var used: Set<String> = []
    @State private var currentPlayer: String?
    @State private var playerIndex = 0
    @State private var showCustom = false

    private let levels = ["轻松", "进阶", "刺激"]

    private var players: [String] {
        playersText.components(separatedBy: CharacterSet(charactersIn: ",，、 \n")).map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                Picker("尺度", selection: $level) {
                    ForEach(0..<3, id: \.self) { Text(levels[$0]).tag($0) }
                }
                .pickerStyle(.segmented)

                TextField("玩家名字（可选，用逗号分隔）", text: $playersText)
                    .padding(12)
                    .glassEffect(.regular, in: .capsule)

                if let currentPlayer {
                    Text("轮到 \(currentPlayer)")
                        .font(.title3.bold())
                        .foregroundStyle(palette.accentGradient)
                        .transition(.scale.combined(with: .opacity))
                }

                // 卡片
                ZStack {
                    PromptCardBack()
                        .modifier(FlipVisibility(angle: isFaceUp ? 180 : 0, showsWhenFlipped: false))
                    PromptCardFront(kind: kind ?? "truth", prompt: prompt)
                        .rotation3DEffect(.degrees(180), axis: (0, 1, 0))
                        .modifier(FlipVisibility(angle: isFaceUp ? 180 : 0, showsWhenFlipped: true))
                }
                .rotation3DEffect(.degrees(isFaceUp ? 180 : 0), axis: (0, 1, 0), perspective: 0.5)
                .frame(height: 300)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel(isFaceUp ? "\(kind == "dare" ? "大冒险" : "真心话")：\(prompt)" : "卡片背面")

                HStack(spacing: 12) {
                    choiceButton(title: "真心话", symbol: "bubble.left.and.text.bubble.right.fill", colors: [Color(hex: 0x60A5FA), Color(hex: 0x8B5CF6)]) { draw("truth") }
                    choiceButton(title: "大冒险", symbol: "flame.fill", colors: [Color(hex: 0xFB7185), Color(hex: 0xF59E0B)]) { draw("dare") }
                }
                Button("随机来一个", systemImage: "shuffle") { draw(Bool.random() ? "truth" : "dare") }
                    .buttonStyle(.glass)
                    .controlSize(.large)

                Button("自定义题目（\(customPrompts.count)）", systemImage: "square.and.pencil") { showCustom = true }
                    .font(.subheadline)
                    .padding(.top, 4)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("真心话大冒险")
        .sheet(isPresented: $showCustom) { CustomPromptsView() }
        .onShake { draw(Bool.random() ? "truth" : "dare") }
    }

    private func choiceButton(title: String, symbol: String, colors: [Color], action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: symbol).font(.title)
                Text(title).font(.headline)
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 18)
            .background(RoundedRectangle(cornerRadius: 22).fill(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing)))
            .shadow(color: colors[0].opacity(0.5), radius: 10, y: 4)
        }
        .buttonStyle(.pressableCard)
    }

    private func draw(_ kind: String) {
        let content = TruthDareContent.shared
        let builtIn = kind == "truth" ? content.truth[level] : content.dare[level]
        let custom = customPrompts.filter { $0.kind == kind && $0.level <= level }.map(\.text)
        var pool = (builtIn + custom).filter { !used.contains($0) }
        if pool.isEmpty {
            used.subtract(builtIn + custom)
            pool = builtIn + custom
        }
        guard let next = pool.randomElement() else { return }
        used.insert(next)

        SoundEngine.shared.play(.cardFlip)
        Haptics.impact()
        if isFaceUp {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.85)) { isFaceUp = false }
            Task {
                try? await Task.sleep(for: .milliseconds(380))
                reveal(kind: kind, prompt: next)
            }
        } else {
            reveal(kind: kind, prompt: next)
        }
    }

    private func reveal(kind: String, prompt: String) {
        self.kind = kind
        self.prompt = prompt
        if !players.isEmpty {
            withAnimation { currentPlayer = players[playerIndex % players.count] }
            playerIndex += 1
        } else {
            currentPlayer = nil
        }
        withAnimation(.spring(response: 0.6, dampingFraction: 0.75)) { isFaceUp = true }
        SoundEngine.shared.play(.sparkle, volume: 0.6)
        Haptics.success()
    }
}

private struct PromptCardBack: View {
    @Environment(\.palette) private var palette

    var body: some View {
        RoundedRectangle(cornerRadius: 30)
            .fill(LinearGradient(colors: [palette.accent.opacity(0.9), palette.accentSecondary.opacity(0.9)], startPoint: .topLeading, endPoint: .bottomTrailing))
            .overlay {
                VStack(spacing: 14) {
                    Text("?").font(.system(size: 90, weight: .black, design: .rounded)).foregroundStyle(.white.opacity(0.9))
                    Text("真心话 or 大冒险").font(.headline).foregroundStyle(.white.opacity(0.85))
                }
            }
            .overlay(RoundedRectangle(cornerRadius: 30).strokeBorder(.white.opacity(0.4), lineWidth: 2).padding(10))
            .shadow(color: palette.accent.opacity(0.5), radius: 20)
    }
}

private struct PromptCardFront: View {
    let kind: String
    let prompt: String

    var body: some View {
        let isDare = kind == "dare"
        let colors = isDare ? [Color(hex: 0xFB7185), Color(hex: 0xF59E0B)] : [Color(hex: 0x60A5FA), Color(hex: 0x8B5CF6)]
        RoundedRectangle(cornerRadius: 30)
            .fill(.regularMaterial)
            .overlay {
                VStack(spacing: 18) {
                    Label(isDare ? "大冒险" : "真心话", systemImage: isDare ? "flame.fill" : "bubble.left.and.text.bubble.right.fill")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14).padding(.vertical, 7)
                        .background(Capsule().fill(LinearGradient(colors: colors, startPoint: .leading, endPoint: .trailing)))
                    Text(prompt)
                        .font(.title2.weight(.semibold))
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 24)
                        .minimumScaleFactor(0.7)
                }
            }
            .overlay(RoundedRectangle(cornerRadius: 30).strokeBorder(LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing), lineWidth: 3))
            .shadow(color: colors[0].opacity(0.4), radius: 20)
    }
}

struct CustomPromptsView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \CustomPrompt.createdAt, order: .reverse) private var prompts: [CustomPrompt]
    @State private var text = ""
    @State private var kind = "truth"
    @State private var level = 0

    var body: some View {
        NavigationStack {
            List {
                Section("添加题目") {
                    Picker("类型", selection: $kind) {
                        Text("真心话").tag("truth")
                        Text("大冒险").tag("dare")
                    }
                    .pickerStyle(.segmented)
                    Picker("尺度", selection: $level) {
                        Text("轻松").tag(0)
                        Text("进阶").tag(1)
                        Text("刺激").tag(2)
                    }
                    TextField("写下你的题目", text: $text, axis: .vertical)
                    Button("添加") {
                        context.insert(CustomPrompt(kind: kind, text: text.trimmingCharacters(in: .whitespacesAndNewlines), level: level))
                        try? context.save()
                        text = ""
                        Haptics.success()
                    }
                    .disabled(text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                .glassRows()

                Section("我的题目") {
                    ForEach(prompts) { prompt in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(prompt.text)
                            Text("\(prompt.kind == "truth" ? "真心话" : "大冒险") · \(["轻松", "进阶", "刺激"][min(2, prompt.level)])")
                                .font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    .onDelete { offsets in
                        for index in offsets { context.delete(prompts[index]) }
                        try? context.save()
                    }
                }
                .glassRows()
            }
            .cosmicBackground(dimmed: true)
            .navigationTitle("自定义题目")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("完成", systemImage: "checkmark") { dismiss() } }
            }
        }
    }
}
