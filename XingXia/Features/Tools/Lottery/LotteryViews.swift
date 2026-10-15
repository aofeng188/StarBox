import SwiftUI
import SwiftData

struct LotteryListView: View {
    @Environment(\.modelContext) private var context
    @Query(sort: \LotteryPool.createdAt) private var pools: [LotteryPool]
    @State private var editing: LotteryPool?

    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                ForEach(pools) { pool in
                    NavigationLink {
                        LotteryDrawView(pool: pool)
                    } label: {
                        HStack(spacing: 14) {
                            Text(pool.icon).font(.system(size: 36))
                                .frame(width: 60, height: 60)
                                .background(Circle().fill(.tint.opacity(0.18)))
                            VStack(alignment: .leading, spacing: 4) {
                                Text(pool.name).font(.headline).foregroundStyle(.primary)
                                Text(summary(pool)).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                        }
                        .glassCard(interactive: true)
                    }
                    .buttonStyle(.pressableCard)
                    .contextMenu {
                        Button("编辑", systemImage: "pencil") { editing = pool }
                        Button("删除", systemImage: "trash", role: .destructive) {
                            context.delete(pool)
                            try? context.save()
                        }
                    }
                }
                Button {
                    let pool = LotteryPool(name: "新奖池")
                    context.insert(pool)
                    try? context.save()
                    editing = pool
                } label: {
                    Label("新建奖池", systemImage: "plus.circle.fill").frame(maxWidth: .infinity).padding(.vertical, 6)
                }
                .buttonStyle(.glass)
                Text("长按奖池可以编辑或删除").font(.caption).foregroundStyle(.secondary)
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("自定义抽奖")
        .sheet(item: $editing) { pool in NavigationStack { LotteryEditor(pool: pool) } }
    }

    private func summary(_ pool: LotteryPool) -> String {
        let items = pool.items ?? []
        if pool.removeAfterDraw {
            return "\(items.count) 种奖品 · 剩余 \(items.reduce(0) { $0 + $1.remaining }) 份"
        }
        return "\(items.count) 个选项 · 可重复抽取"
    }
}

struct LotteryDrawView: View {
    @Bindable var pool: LotteryPool
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette

    @State private var drawCount = 1
    @State private var isDrawing = false
    @State private var results: [String] = []
    @State private var rollingText: String?
    @State private var confetti = 0
    @State private var revealCount = 0
    @State private var editing = false
    @State private var sessionLog: [String] = []

    private var available: [LotteryItem] {
        pool.sortedItems.filter { $0.weight > 0 && (!pool.removeAfterDraw || $0.remaining > 0) }
    }

    /// 抽不了时说明原因：没有奖品、权重全为 0、不放回且已抽完
    private var unavailableReason: (title: String, hint: String)? {
        let items = pool.sortedItems
        if items.isEmpty { return ("先添加奖品吧", "点右上角「编辑」添加奖品或选项") }
        if items.allSatisfy({ $0.weight <= 0 }) { return ("所有奖品权重都是 0", "在「编辑」里调高权重才能抽") }
        if available.isEmpty { return ("奖品已经抽完啦", "点下方奖池的「重置」可以再来一轮") }
        return nil
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                GachaMachine(isShaking: isDrawing, colors: palette.spectrum)
                    .frame(height: 250)
                    .padding(.top, 8)

                Group {
                    if let rollingText {
                        Text(rollingText)
                            .font(.title2.bold())
                            .foregroundStyle(.secondary)
                    } else if !results.isEmpty {
                        VStack(spacing: 8) {
                            Text(results.count == 1 ? "恭喜抽中" : "本轮抽中 \(results.count) 个").font(.subheadline).foregroundStyle(.secondary)
                            ForEach(Array(results.enumerated()), id: \.offset) { _, result in
                                Text(result)
                                    .font(results.count == 1 ? .system(size: 30, weight: .heavy, design: .rounded) : .headline)
                                    .foregroundStyle(palette.accentGradient)
                                    .multilineTextAlignment(.center)
                            }
                        }
                        .keyframeAnimator(initialValue: 1.0, trigger: revealCount) { view, scale in
                            view.scaleEffect(scale)
                        } keyframes: { _ in
                            CubicKeyframe(1.15, duration: 0.15)
                            SpringKeyframe(1, duration: 0.4, spring: .bouncy)
                        }
                    } else if let reason = unavailableReason {
                        VStack(spacing: 6) {
                            Text(reason.title).font(.title3.weight(.semibold))
                            Text(reason.hint).font(.footnote).foregroundStyle(.secondary)
                        }
                        .multilineTextAlignment(.center)
                    } else {
                        Text("准备好了吗？").font(.title3.weight(.semibold))
                    }
                }
                .frame(minHeight: 80)
                .frame(maxWidth: .infinity)
                .glassCard()

                HStack {
                    Stepper("一次抽 \(drawCount) 个", value: $drawCount, in: 1...10)
                        .font(.subheadline)
                }
                .glassCard(padding: 14)

                BigActionButton(title: isDrawing ? "抽奖中…" : "开始抽奖", symbol: "gift.fill", isEnabled: !isDrawing && !available.isEmpty) {
                    draw()
                }

                VStack(alignment: .leading, spacing: 10) {
                    HStack {
                        SectionHeader(title: "奖池", subtitle: pool.removeAfterDraw ? "抽中后数量减少" : "可重复抽中", symbol: "shippingbox")
                        if pool.removeAfterDraw {
                            Button("重置") { reset() }.font(.caption).buttonStyle(.glass).disabled(isDrawing)
                        }
                    }
                    let totalWeight = available.reduce(0.0) { $0 + effectiveWeight($1) }
                    ForEach(pool.sortedItems) { item in
                        HStack {
                            Text(item.name).font(.subheadline)
                                .strikethrough(pool.removeAfterDraw && item.remaining == 0)
                                .foregroundStyle(pool.removeAfterDraw && item.remaining == 0 ? .secondary : .primary)
                            Spacer()
                            if pool.removeAfterDraw {
                                Text("\(item.remaining)/\(item.quantity)").font(.caption.monospacedDigit()).foregroundStyle(.secondary)
                            }
                            let p = totalWeight > 0 && (!pool.removeAfterDraw || item.remaining > 0) ? effectiveWeight(item) / totalWeight : 0
                            Text(String(format: "%.1f%%", p * 100)).font(.caption.monospacedDigit()).foregroundStyle(.tint)
                                .frame(width: 54, alignment: .trailing)
                        }
                    }
                }
                .glassCard()

                if !sessionLog.isEmpty {
                    VStack(alignment: .leading, spacing: 6) {
                        SectionHeader(title: "本次记录", symbol: "list.number")
                        ForEach(Array(sessionLog.enumerated().reversed()), id: \.offset) { index, item in
                            Text("\(index + 1). \(item)").font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    .glassCard()
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .overlay { ConfettiView(trigger: confetti) }
        .cosmicBackground()
        .navigationTitle("\(pool.icon) \(pool.name)")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) { Button("编辑", systemImage: "pencil") { editing = true }.disabled(isDrawing) }
        }
        .sheet(isPresented: $editing) { NavigationStack { LotteryEditor(pool: pool) } }
        .onShake(isEnabled: !editing) { if !isDrawing && !available.isEmpty { draw() } }
    }

    private func effectiveWeight(_ item: LotteryItem) -> Double {
        pool.removeAfterDraw ? item.weight * Double(item.remaining) : item.weight
    }

    private func draw() {
        guard !isDrawing, !available.isEmpty else { return }
        isDrawing = true
        results = []
        SoundEngine.shared.play(.whoosh)
        Haptics.rattle(count: 8)

        // 先算出结果，再播放动画
        var picked: [LotteryItem] = []
        var remainingMap = Dictionary(uniqueKeysWithValues: pool.sortedItems.map { ($0.id, $0.remaining) })
        for _ in 0..<drawCount {
            let candidates = pool.sortedItems.filter { $0.weight > 0 && (!pool.removeAfterDraw || (remainingMap[$0.id] ?? 0) > 0) }
            guard !candidates.isEmpty else { break }
            let weights = candidates.map { pool.removeAfterDraw ? $0.weight * Double(remainingMap[$0.id] ?? 0) : $0.weight }
            guard let index = TrueRandom.weightedIndex(weights) else { break }
            let item = candidates[index]
            picked.append(item)
            if pool.removeAfterDraw { remainingMap[item.id, default: 0] -= 1 }
        }
        let names = pool.sortedItems.map(\.name)

        Task {
            await Roller.roll(pool: names, final: picked.first?.name ?? "", steps: 22) { rollingText = $0 }
            rollingText = nil
            isDrawing = false
            if pool.removeAfterDraw {
                for item in picked { item.remaining = max(0, item.remaining - 1) }
            }
            results = picked.map(\.name)
            sessionLog.append(contentsOf: results)
            revealCount += 1
            confetti += 1
            SoundEngine.shared.play(.success)
            Haptics.reveal()
            Announce.post("抽中：\(results.joined(separator: "、"))")
            HistoryLog.add(context, tool: ToolKind.lottery.rawValue, title: pool.name, result: results.joined(separator: "、"))
        }
    }

    private func reset() {
        for item in pool.sortedItems { item.remaining = item.quantity }
        try? context.save()
        results = []
        Haptics.success()
    }
}

/// 扭蛋机
struct GachaMachine: View {
    let isShaking: Bool
    let colors: [Color]
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30, paused: reduceMotion)) { context in
            let t = context.date.timeIntervalSinceReferenceDate
            ZStack {
                // 底座
                RoundedRectangle(cornerRadius: 20)
                    .fill(LinearGradient(colors: [colors[0], colors[1]], startPoint: .top, endPoint: .bottom))
                    .frame(width: 170, height: 70)
                    .overlay(alignment: .center) {
                        Circle().fill(.black.opacity(0.35)).frame(width: 34, height: 34)
                            .overlay(Circle().strokeBorder(.white.opacity(0.5), lineWidth: 2))
                            .rotationEffect(.degrees(isShaking ? t * 400 : 0))
                            .overlay(Rectangle().fill(.white.opacity(0.8)).frame(width: 4, height: 22).rotationEffect(.degrees(isShaking ? t * 400 : 0)))
                    }
                    .offset(y: 90)

                // 玻璃球
                ZStack {
                    ForEach(0..<14, id: \.self) { i in
                        let speed = isShaking ? 9.0 : 0.8
                        let phase = Double(i) * 1.7
                        let fx: Double = 0.6 + Double(i % 3) * 0.2
                        let fy: Double = 0.5 + Double(i % 4) * 0.15
                        let ax: Double = isShaking ? 50 : 40
                        let ay: Double = isShaking ? 50 : 20
                        let base: Double = isShaking ? 0 : 30
                        let x: Double = sin(t * speed * fx + phase) * ax
                        let y: Double = cos(t * speed * fy + phase * 1.3) * ay + base
                        Capsule()
                            .fill(LinearGradient(colors: [colors[i % colors.count], .white.opacity(0.9)], startPoint: .top, endPoint: .bottom))
                            .frame(width: 30, height: 30)
                            .overlay(Rectangle().fill(.white.opacity(0.35)).frame(height: 2))
                            .rotationEffect(.degrees(Double(i) * 37 + (isShaking ? t * 300 : 0)))
                            .offset(x: x, y: y)
                    }
                }
                .frame(width: 170, height: 170)
                .clipShape(Circle())
                .glassEffect(.clear, in: .circle)
                .overlay(Circle().strokeBorder(.white.opacity(0.4), lineWidth: 1.5))
                .overlay(alignment: .topLeading) {
                    Ellipse().fill(.white.opacity(0.35)).frame(width: 50, height: 22).rotationEffect(.degrees(-30)).offset(x: 30, y: 26)
                }
                .offset(x: isShaking ? sin(t * 40) * 3 : 0, y: -20)
            }
        }
        .accessibilityHidden(true)
    }
}

struct LotteryEditor: View {
    @Bindable var pool: LotteryPool
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @State private var newName = ""

    private let icons = ["🎁", "🎊", "🧧", "🏆", "🍀", "🎉", "💎", "🎟️", "🍬", "🌟"]

    var body: some View {
        List {
            Section("奖池") {
                TextField("名称", text: $pool.name)
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack {
                        ForEach(icons, id: \.self) { icon in
                            Button { pool.icon = icon } label: {
                                Text(icon).font(.title2).padding(6)
                                    .background(Circle().fill(pool.icon == icon ? Color.accentColor.opacity(0.3) : .clear))
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("图标 \(icon)")
                            .accessibilityAddTraits(pool.icon == icon ? .isSelected : [])
                        }
                    }
                }
                Toggle("抽中后减少数量（不放回）", isOn: $pool.removeAfterDraw)
            }
            .glassRows()

            Section {
                ForEach(pool.sortedItems) { item in
                    LotteryItemRow(item: item, showsQuantity: pool.removeAfterDraw)
                }
                .onDelete { offsets in
                    let items = pool.sortedItems
                    for index in offsets { context.delete(items[index]) }
                    try? context.save()
                }
                HStack {
                    TextField("添加奖品或选项", text: $newName).onSubmit(add)
                    Button("添加", systemImage: "plus.circle.fill", action: add)
                        .labelStyle(.iconOnly)
                        .disabled(newName.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            } header: {
                Text("奖品")
            } footer: {
                Text(pool.removeAfterDraw ? "抽中概率 = 权重 × 剩余数量。" : "抽中概率由权重决定。")
            }
            .glassRows()
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle("编辑奖池")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) { Button("完成", systemImage: "checkmark") { try? context.save(); dismiss() } }
        }
    }

    private func add() {
        let name = newName.trimmingCharacters(in: .whitespaces)
        guard !name.isEmpty else { return }
        // 接在现有最大序号后面，删除过奖品也不会和已有项重号
        let nextIndex = ((pool.items ?? []).map(\.sortIndex).max() ?? -1) + 1
        let item = LotteryItem(name: name, quantity: 1, sortIndex: nextIndex)
        item.pool = pool
        context.insert(item)
        try? context.save()
        newName = ""
    }
}

private struct LotteryItemRow: View {
    @Bindable var item: LotteryItem
    let showsQuantity: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            TextField("名称", text: $item.name).font(.body.weight(.medium))
            HStack(spacing: 16) {
                Stepper("权重 \(Int(item.weight))", value: $item.weight, in: 0...20, step: 1).font(.caption)
                if showsQuantity {
                    Stepper("数量 \(item.quantity)", value: Binding(get: { item.quantity }, set: { new in
                        let delta = new - item.quantity
                        item.quantity = new
                        item.remaining = max(0, item.remaining + delta)
                    }), in: 1...999).font(.caption)
                }
            }
        }
    }
}
