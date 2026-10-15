import SwiftUI
import SwiftData

struct ScratchCardView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Query(sort: \ScratchPreset.createdAt) private var presets: [ScratchPreset]
    @AppStorage("scratch.preset") private var presetID = ""

    @State private var customMessage = ""
    @State private var useCustom = false
    @State private var prize = ""
    @State private var isBlank = false
    @State private var strokes: [[CGPoint]] = []
    @State private var scratched: Set<Int> = []
    @State private var revealed = false
    @State private var confetti = 0
    @State private var lastSound = Date.distantPast
    @State private var editing: ScratchPreset?
    @State private var cardID = 0

    private let cardSize = CGSize(width: 330, height: 210)
    private let columns = 22
    private let rows = 14

    private var preset: ScratchPreset? { presets.first { $0.id.uuidString == presetID } ?? presets.first }

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                Picker("类型", selection: $useCustom) {
                    Text("随机奖品").tag(false)
                    Text("写一句话").tag(true)
                }
                .pickerStyle(.segmented)
                .onChange(of: useCustom) { _, _ in newCard() }

                if useCustom {
                    TextField("写下刮开后要显示的话，比如「生日快乐！」", text: $customMessage, axis: .vertical)
                        .lineLimit(1...3)
                        .padding(12)
                        .glassEffect(.regular, in: .rect(cornerRadius: 16))
                        // 多行输入框按回车只会换行、不会提交，所以边输入边换上新卡面
                        .onChange(of: customMessage) { _, _ in if useCustom { newCard() } }
                } else if !presets.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(presets) { item in
                                let isOn = item.id == preset?.id
                                Button(item.name) {
                                    presetID = item.id.uuidString
                                    newCard()
                                }
                                .font(.subheadline.weight(.medium))
                                .padding(.horizontal, 12).padding(.vertical, 8)
                                .foregroundStyle(isOn ? palette.onAccent : .primary)
                                .background(Capsule().fill(isOn ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.07))))
                                .accessibilityAddTraits(isOn ? .isSelected : [])
                                .contextMenu {
                                    Button("编辑", systemImage: "pencil") { editing = item }
                                    Button("删除", systemImage: "trash", role: .destructive) { context.delete(item); try? context.save() }
                                }
                            }
                            Button {
                                let item = ScratchPreset(name: "新卡池", prizes: ["奖品一", "奖品二"])
                                context.insert(item)
                                try? context.save()
                                editing = item
                            } label: {
                                Image(systemName: "plus").padding(9).background(Circle().fill(.primary.opacity(0.07)))
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("新建卡池")
                        }
                        .padding(.horizontal, 16)
                    }
                    .pageBleed()
                }

                card
                    .id(cardID)
                    .padding(.vertical, 10)

                Text(revealed ? (isBlank ? "下次一定！" : "恭喜！") : "用手指刮开银色涂层")
                    .font(.headline)
                    .foregroundStyle(revealed ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.secondary))

                HStack(spacing: 12) {
                    Button("直接揭晓", systemImage: "eye") { reveal() }
                        .buttonStyle(.glass)
                        .disabled(revealed)
                    Button("再来一张", systemImage: "arrow.counterclockwise") { newCard() }
                        .buttonStyle(.glassProminent)
                        .tint(palette.accent)
                        .foregroundStyle(palette.onAccent)
                }
                .controlSize(.large)

                if !useCustom, let preset {
                    VStack(alignment: .leading, spacing: 6) {
                        SectionHeader(title: "卡池「\(preset.name)」", subtitle: "长按上方卡池可编辑", symbol: "giftcard")
                        Text(preset.prizes.joined(separator: " · ")).font(.caption).foregroundStyle(.secondary)
                        Text("谢谢惠顾的权重：\(Int(preset.blankWeight))").font(.caption2).foregroundStyle(.tertiary)
                    }
                    .glassCard()
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .scrollDisabled(!revealed && !strokes.isEmpty)
        .overlay { ConfettiView(trigger: confetti) }
        .cosmicBackground()
        .navigationTitle("刮刮乐")
        .toolbar(.hidden, for: .tabBar)
        .sheet(item: $editing) { item in NavigationStack { ScratchPresetEditor(preset: item) } }
        .onAppear { if prize.isEmpty { newCard() } }
    }

    private var card: some View {
        ZStack {
            // 底层：奖品
            RoundedRectangle(cornerRadius: 24)
                .fill(LinearGradient(colors: isBlank ? [Color(hex: 0x64748B), Color(hex: 0x334155)] : [palette.accent, palette.accentSecondary],
                                     startPoint: .topLeading, endPoint: .bottomTrailing))
                .overlay {
                    VStack(spacing: 10) {
                        Image(systemName: isBlank ? "face.smiling" : "gift.fill").font(.system(size: 34))
                        Text(prize)
                            .font(.system(size: 28, weight: .heavy, design: .rounded))
                            .multilineTextAlignment(.center)
                            .minimumScaleFactor(0.5)
                            .padding(.horizontal, 20)
                    }
                    .foregroundStyle(isBlank ? .white : palette.onAccent)
                }

            // 涂层
            ScratchCoating()
                .mask {
                    Canvas { context, size in
                        context.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.white))
                        context.blendMode = .clear
                        for stroke in strokes {
                            var path = Path()
                            path.addLines(stroke)
                            context.stroke(path, with: .color(.white), style: StrokeStyle(lineWidth: 44, lineCap: .round, lineJoin: .round))
                            if stroke.count == 1, let p = stroke.first {
                                context.fill(Path(ellipseIn: CGRect(x: p.x - 22, y: p.y - 22, width: 44, height: 44)), with: .color(.white))
                            }
                        }
                    }
                }
                .opacity(revealed ? 0 : 1)
                .animation(.easeOut(duration: 0.6), value: revealed)
                .allowsHitTesting(!revealed)
        }
        .frame(width: cardSize.width, height: cardSize.height)
        .clipShape(.rect(cornerRadius: 24))
        .shadow(color: palette.accent.opacity(0.4), radius: 16)
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    guard !revealed else { return }
                    if value.translation == .zero || strokes.isEmpty {
                        strokes.append([value.location])
                    } else {
                        strokes[strokes.count - 1].append(value.location)
                    }
                    mark(value.location)
                    let now = Date()
                    if now.timeIntervalSince(lastSound) > 0.08 {
                        lastSound = now
                        SoundEngine.shared.play(.scratch, volume: 0.7)
                        Haptics.softImpact(0.35)
                    }
                }
                .onEnded { _ in
                    strokes.append([])
                }
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(revealed ? "刮刮卡：\(prize)" : "刮刮卡，尚未刮开")
        .accessibilityAction(named: "揭晓") { reveal() }
    }

    private func mark(_ point: CGPoint) {
        let cellW = cardSize.width / CGFloat(columns)
        let cellH = cardSize.height / CGFloat(rows)
        let r: CGFloat = 22
        let minC = max(0, Int((point.x - r) / cellW)), maxC = min(columns - 1, Int((point.x + r) / cellW))
        let minR = max(0, Int((point.y - r) / cellH)), maxR = min(rows - 1, Int((point.y + r) / cellH))
        guard minC <= maxC, minR <= maxR else { return }
        for row in minR...maxR {
            for column in minC...maxC {
                let center = CGPoint(x: (CGFloat(column) + 0.5) * cellW, y: (CGFloat(row) + 0.5) * cellH)
                if hypot(center.x - point.x, center.y - point.y) <= r { scratched.insert(row * columns + column) }
            }
        }
        if Double(scratched.count) / Double(columns * rows) > 0.55 { reveal() }
    }

    private func reveal() {
        guard !revealed else { return }
        revealed = true
        if isBlank {
            SoundEngine.shared.play(.fail)
            Haptics.warning()
        } else {
            SoundEngine.shared.play(.success)
            Haptics.reveal()
            confetti += 1
        }
        HistoryLog.add(context, tool: ToolKind.scratch.rawValue, title: useCustom ? "自定义" : (preset?.name ?? ""), result: prize)
    }

    private func newCard() {
        strokes = []
        scratched = []
        revealed = false
        cardID += 1
        if useCustom {
            let message = customMessage.trimmingCharacters(in: .whitespacesAndNewlines)
            prize = message.isEmpty ? "今天也要开心！" : message
            isBlank = false
            return
        }
        let prizes = preset?.prizes ?? ["惊喜"]
        let weights = prizes.map { _ in 1.0 } + [preset?.blankWeight ?? 1]
        let index = TrueRandom.weightedIndex(weights) ?? 0
        if index >= prizes.count {
            prize = "谢谢惠顾"
            isBlank = true
        } else {
            prize = prizes[index]
            isBlank = false
        }
    }
}

/// 银色刮层
private struct ScratchCoating: View {
    var body: some View {
        ZStack {
            LinearGradient(colors: [Color(hex: 0xE5E7EB), Color(hex: 0x9CA3AF), Color(hex: 0xD1D5DB), Color(hex: 0x6B7280)],
                           startPoint: .topLeading, endPoint: .bottomTrailing)
            Canvas { context, size in
                var rng = SeededGenerator(seed: 42)
                for _ in 0..<400 {
                    let rect = CGRect(x: rng.unit() * size.width, y: rng.unit() * size.height, width: 1.5, height: 1.5)
                    context.fill(Path(ellipseIn: rect), with: .color(.white.opacity(rng.double(0.2...0.7))))
                }
                for i in 0..<12 {
                    var path = Path()
                    let y = CGFloat(i) * size.height / 12
                    path.move(to: CGPoint(x: 0, y: y))
                    path.addLine(to: CGPoint(x: size.width, y: y + 40))
                    context.stroke(path, with: .color(.white.opacity(0.12)), lineWidth: 6)
                }
            }
            VStack(spacing: 6) {
                Image(systemName: "hand.draw.fill").font(.title)
                Text("刮开有惊喜").font(.title2.bold())
            }
            .foregroundStyle(Color(hex: 0x4B5563))
        }
    }
}

struct ScratchPresetEditor: View {
    @Bindable var preset: ScratchPreset
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var context
    @State private var text = ""

    var body: some View {
        Form {
            Section("名称") { TextField("卡池名称", text: $preset.name) }.glassRows()
            Section {
                TextEditor(text: $text).frame(minHeight: 160)
            } header: {
                Text("奖品（每行一个）")
            }
            .glassRows()
            Section {
                Stepper("谢谢惠顾权重 \(Int(preset.blankWeight))", value: $preset.blankWeight, in: 0...20)
            } footer: {
                Text("每个奖品的权重为 1。权重为 0 表示必中奖。")
            }
            .glassRows()
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle("编辑卡池")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("完成", systemImage: "checkmark") {
                    preset.prizes = text.components(separatedBy: .newlines).map { $0.trimmingCharacters(in: .whitespaces) }.filter { !$0.isEmpty }
                    try? context.save()
                    dismiss()
                }
            }
        }
        .onAppear { text = preset.prizes.joined(separator: "\n") }
    }
}
