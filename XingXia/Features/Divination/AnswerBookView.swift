import SwiftUI
import SwiftData

struct AnswerBookView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @State private var isOpen = false
    @State private var isPressing = false
    @State private var holdProgress: Double = 0
    @State private var answer = ""
    @State private var question = ""
    @State private var rippleTrigger = 0
    @State private var recent: [String] = []

    private static let answers: [String] = ContentLibrary.load("answers")

    var body: some View {
        ScrollView {
            VStack(spacing: 22) {
                VStack(spacing: 8) {
                    Text(isOpen ? "这是宇宙给你的答案" : "心中默念一个问题")
                        .font(AppFont.serif(24, weight: .bold))
                        .contentTransition(.opacity)
                    Text(isOpen ? "答案只是提示，选择权永远在你" : "最好是能用「是」或「否」回答的问题")
                        .font(.subheadline).foregroundStyle(.secondary)
                }
                .padding(.top, 16)

                TextField("写下问题（可选）", text: $question)
                    .padding(12)
                    .glassEffect(.regular, in: .capsule)
                    .disabled(isOpen)

                book
                    .frame(width: 260, height: 340)
                    .padding(.vertical, 10)
                    .rippleEffect(at: CGPoint(x: 130, y: 170), trigger: rippleTrigger)

                if isOpen {
                    Button("再问一次", systemImage: "arrow.counterclockwise") { close() }
                        .buttonStyle(.glass)
                        .controlSize(.large)
                } else {
                    VStack(spacing: 10) {
                        ZStack {
                            Circle().stroke(.primary.opacity(0.1), lineWidth: 6)
                            Circle().trim(from: 0, to: holdProgress)
                                .stroke(LinearGradient(colors: [Color(hex: 0xFFE7A3), Color(hex: 0xF59E0B)], startPoint: .top, endPoint: .bottom),
                                        style: StrokeStyle(lineWidth: 6, lineCap: .round))
                                .rotationEffect(.degrees(-90))
                            Image(systemName: "hand.point.up.left.fill").font(.title2)
                                .foregroundStyle(Color(hex: 0xF5B83D))
                        }
                        .frame(width: 70, height: 70)
                        Text(isPressing ? "专注于你的问题…" : "按住书本，默念问题").font(.footnote).foregroundStyle(.secondary)
                    }
                }

                if !recent.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        SectionHeader(title: "刚刚的答案", symbol: "clock")
                        ForEach(Array(recent.enumerated()), id: \.offset) { _, item in
                            Text("· " + item).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                    .glassCard()
                }
            }
            .pageContent(padding: 20)
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("答案之书")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .onShake { if !isOpen { open() } }
    }

    private var book: some View {
        ZStack {
            // 内页
            RoundedRectangle(cornerRadius: 14)
                .fill(LinearGradient(colors: [Color(hex: 0xFFF8E7), Color(hex: 0xF3E3C3)], startPoint: .top, endPoint: .bottom))
                .overlay {
                    VStack(spacing: 16) {
                        Image(systemName: "sparkle").foregroundStyle(Color(hex: 0xC08A2E))
                        Text(answer)
                            .font(AppFont.serif(26, weight: .bold))
                            .foregroundStyle(Color(hex: 0x4A2C0A))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 24)
                            .opacity(isOpen ? 1 : 0)
                            .blur(radius: isOpen ? 0 : 8)
                        Image(systemName: "sparkle").foregroundStyle(Color(hex: 0xC08A2E))
                    }
                }
                .shadow(color: Color(hex: 0xFFD166).opacity(isOpen ? 0.8 : 0.2), radius: isOpen ? 30 : 10)

            // 封面
            RoundedRectangle(cornerRadius: 14)
                .fill(LinearGradient(colors: [Color(hex: 0x7C4A12), Color(hex: 0x3B2006)], startPoint: .topLeading, endPoint: .bottomTrailing))
                .overlay {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10)
                            .strokeBorder(Color(hex: 0xF5D78E).opacity(0.8), lineWidth: 2)
                            .padding(14)
                        VStack(spacing: 18) {
                            Image(systemName: "moon.stars.fill")
                                .font(.system(size: 54))
                                .foregroundStyle(LinearGradient(colors: [Color(hex: 0xFFE7A3), Color(hex: 0xF5B83D)], startPoint: .top, endPoint: .bottom))
                                .shadow(color: Color(hex: 0xFFD166), radius: isPressing ? 20 : 6)
                            Text("答案之书")
                                .font(AppFont.serif(30, weight: .bold))
                                .foregroundStyle(Color(hex: 0xF5D78E))
                            Text("THE BOOK OF ANSWERS")
                                .font(.system(size: 10, weight: .semibold, design: .serif))
                                .tracking(2)
                                .foregroundStyle(Color(hex: 0xF5D78E).opacity(0.7))
                        }
                    }
                }
                .rotation3DEffect(.degrees(isOpen ? -165 : 0), axis: (0, 1, 0), anchor: .leading, perspective: 0.4)
                .modifier(FlipVisibility(angle: isOpen ? 165 : 0, showsWhenFlipped: false))
                .shadow(color: .black.opacity(0.4), radius: 12, x: 4, y: 8)
        }
        .scaleEffect(isPressing ? 1.04 : 1)
        .offset(x: isPressing && !reduceMotion ? CGFloat.random(in: -1.5...1.5) : 0)
        .onLongPressGesture(minimumDuration: 1.8, maximumDistance: 50) {
            open()
        } onPressingChanged: { pressing in
            guard !isOpen else { return }
            isPressing = pressing
            if pressing {
                Haptics.softImpact()
                withAnimation(.linear(duration: 1.8)) { holdProgress = 1 }
            } else if !isOpen {
                withAnimation(.easeOut(duration: 0.3)) { holdProgress = 0 }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(isOpen ? "答案：\(answer)" : "答案之书，长按打开")
        .accessibilityAction(named: "打开") { open() }
    }

    private func open() {
        guard !isOpen else { return }
        answer = Self.answers.randomElement() ?? "顺其自然"
        isPressing = false
        withAnimation(.spring(response: 0.9, dampingFraction: 0.8)) { isOpen = true }
        rippleTrigger += 1
        SoundEngine.shared.play(.chime)
        Haptics.reveal()
        Announce.post("答案：\(answer)")
        recent.insert(answer, at: 0)
        if recent.count > 5 { recent.removeLast() }
        HistoryLog.add(context, tool: "answers", title: question, result: answer)
    }

    private func close() {
        withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) {
            isOpen = false
            holdProgress = 0
        }
        question = ""
        SoundEngine.shared.play(.cardFlip)
    }
}
