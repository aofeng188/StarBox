import SwiftUI
import SwiftData

/// 幸运大转盘（工具主页面）
struct WheelListView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Query(sort: \WheelTemplate.createdAt) private var templates: [WheelTemplate]
    @AppStorage("wheel.lastTemplate") private var lastTemplateID = ""

    @State private var spinner = WheelSpinner()
    @State private var eliminated: Set<UUID> = []
    @State private var resultTitle: String?
    @State private var lastIndex = -1
    @State private var lastTick = Date.distantPast
    @State private var confetti = 0
    @State private var editing: WheelTemplate?
    @State private var dragStartAngle: Double?
    @State private var dragBaseRotation: Double = 0
    @State private var lastDragSample: (angle: Double, time: Date)?
    @State private var dragVelocity: Double = 0

    private var template: WheelTemplate? {
        templates.first { $0.id.uuidString == lastTemplateID } ?? templates.first
    }

    private var segments: [WheelSegment] {
        (template?.sortedOptions ?? [])
            .filter { $0.weight > 0 && !eliminated.contains($0.id) }
            .map { WheelSegment(id: $0.id, title: $0.title, weight: $0.weight) }
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                if let template {
                    // 模板切换
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(templates) { item in
                                Button {
                                    lastTemplateID = item.id.uuidString
                                    eliminated = []
                                    resultTitle = nil
                                    Haptics.selection()
                                } label: {
                                    Text(item.name)
                                        .font(.subheadline.weight(.medium))
                                        .padding(.horizontal, 12).padding(.vertical, 8)
                                        .foregroundStyle(item.id == template.id ? .white : .primary)
                                        .background(Capsule().fill(item.id == template.id ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.07))))
                                }
                                .buttonStyle(.plain)
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                    .pageBleed()

                    wheel
                        .padding(.top, 8)

                    // 结果
                    Group {
                        if let resultTitle {
                            VStack(spacing: 6) {
                                Text("转到了").font(.subheadline).foregroundStyle(.secondary)
                                GradientText(text: resultTitle, font: .system(size: 34, weight: .heavy, design: .rounded))
                                    .multilineTextAlignment(.center)
                            }
                            .transition(.scale.combined(with: .opacity))
                        } else {
                            Text(spinner.isSpinning ? "转呀转…" : "点中间的按钮，或用手指拨动转盘")
                                .font(.subheadline).foregroundStyle(.secondary)
                        }
                    }
                    .frame(minHeight: 70)

                    if template.removeAfterSpin {
                        HStack {
                            Label("淘汰模式：转到的选项会暂时移出", systemImage: "scissors").font(.caption).foregroundStyle(.secondary)
                            Spacer()
                            if !eliminated.isEmpty {
                                Button("恢复全部（\(eliminated.count)）") { withAnimation { eliminated = [] } }
                                    .font(.caption)
                            }
                        }
                        .glassCard(padding: 14)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        SectionHeader(title: "选项", subtitle: "\(segments.count) 个 · 按权重分配面积", symbol: "list.bullet")
                        ForEach(Array(segments.enumerated()), id: \.element.id) { index, segment in
                            HStack {
                                Circle().fill(WheelCanvas.color(for: index, count: segments.count, colors: palette.spectrum)).frame(width: 12, height: 12)
                                Text(segment.title).font(.subheadline)
                                Spacer()
                                Text(percent(segment)).font(.caption.monospacedDigit()).foregroundStyle(.secondary)
                            }
                        }
                    }
                    .glassCard()
                } else {
                    EmptyHint(symbol: "circle.dashed", title: "还没有转盘", message: "点右上角新建一个吧")
                }
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .scrollDisabled(dragStartAngle != nil)
        .overlay { ConfettiView(trigger: confetti) }
        .cosmicBackground()
        .navigationTitle("幸运大转盘")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    if let template {
                        Button("编辑当前转盘", systemImage: "pencil") { editing = template }
                    }
                    Button("新建转盘", systemImage: "plus") { create() }
                } label: {
                    Image(systemName: "slider.horizontal.3")
                }
            }
        }
        .sheet(item: $editing) { template in
            NavigationStack { WheelEditor(template: template) }
        }
        .onShake { spin(velocity: nil) }
        .onAppear {
            spinner.onFrame = { angle in tick(angle) }
        }
        .onDisappear { spinner.stop() }
    }

    private var wheel: some View {
        ZStack(alignment: .top) {
            WheelCanvas(segments: segments, rotation: spinner.angle, colors: palette.spectrum, lightsOn: spinner.isSpinning)
                .gesture(dragGesture)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("转盘，共 \(segments.count) 个选项")
                .accessibilityAction(named: "转动") { spin(velocity: nil) }

            WheelPointer().offset(y: -6)

            // 中心按钮
            Button {
                spin(velocity: nil)
            } label: {
                Text("GO")
                    .font(.system(size: 26, weight: .black, design: .rounded))
                    .foregroundStyle(.white)
                    .frame(width: 84, height: 84)
                    .background(Circle().fill(palette.accentGradient))
            }
            .buttonStyle(.plain)
            .glassEffect(.regular.interactive(), in: .circle)
            .shadow(color: palette.accent.opacity(0.6), radius: 14)
            .disabled(spinner.isSpinning || segments.count < 2)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityLabel("开始转动")
        }
        .frame(width: 340, height: 340)
    }

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 4, coordinateSpace: .local)
            .onChanged { value in
                guard !spinner.isSpinning else { return }
                let center = CGPoint(x: 170, y: 170)
                let angle = atan2(value.location.y - center.y, value.location.x - center.x) * 180 / .pi
                if dragStartAngle == nil {
                    dragStartAngle = angle
                    dragBaseRotation = spinner.angle
                    resultTitle = nil
                }
                var delta = angle - (dragStartAngle ?? angle)
                if delta > 180 { delta -= 360 }
                if delta < -180 { delta += 360 }
                spinner.angle = dragBaseRotation + delta
                tick(spinner.angle)
                let now = Date()
                if let last = lastDragSample {
                    var d = spinner.angle - last.angle
                    if d > 180 { d -= 360 }
                    let dt = max(0.001, now.timeIntervalSince(last.time))
                    dragVelocity = dragVelocity * 0.6 + (d / dt) * 0.4
                }
                lastDragSample = (spinner.angle, now)
                // 角度跨越过大时重设基准，避免绕圈
                if abs(delta) > 150 {
                    dragStartAngle = angle
                    dragBaseRotation = spinner.angle
                }
            }
            .onEnded { _ in
                defer {
                    dragStartAngle = nil
                    lastDragSample = nil
                    dragVelocity = 0
                }
                if abs(dragVelocity) > 120 { spin(velocity: dragVelocity) }
            }
    }

    private func percent(_ segment: WheelSegment) -> String {
        let total = segments.reduce(0) { $0 + $1.weight }
        return String(format: "%.0f%%", segment.weight / max(0.0001, total) * 100)
    }

    private func tick(_ angle: Double) {
        let index = WheelCanvas.index(at: angle, segments: segments)
        if index != lastIndex {
            lastIndex = index
            let now = Date()
            if now.timeIntervalSince(lastTick) > 0.035 {
                lastTick = now
                SoundEngine.shared.play(.tick, volume: 0.55)
                Haptics.selection()
            }
        }
    }

    /// velocity 为拨动速度（度/秒，正为顺时针）；nil 表示点按按钮
    private func spin(velocity: Double?) {
        let segments = segments
        guard segments.count >= 2, !spinner.isSpinning else { return }
        guard let index = TrueRandom.weightedIndex(segments.map(\.weight)) else { return }
        let range = WheelCanvas.ranges(segments)[index]
        let width = range.upperBound - range.lowerBound
        let landing = Double.random(in: (range.lowerBound + width * 0.15)...(range.upperBound - width * 0.15))
        let direction: Double = (velocity ?? 1) >= 0 ? 1 : -1
        let speed = min(2400, max(700, abs(velocity ?? 1300)))
        let turns = (speed / 400).rounded(.down) + 3
        let currentMod = Astronomy.normalize(spinner.angle)
        let desiredMod = Astronomy.normalize(360 - landing)
        var delta = direction > 0 ? Astronomy.normalize(desiredMod - currentMod) : -Astronomy.normalize(currentMod - desiredMod)
        delta += direction * turns * 360
        let target = spinner.angle + delta
        let duration = min(7, max(3.2, abs(delta) / speed * 1.6))

        resultTitle = nil
        SoundEngine.shared.play(.whoosh, volume: 0.5)
        Haptics.impact()
        spinner.spin(to: target, duration: duration) {
            let picked = segments[index]
            withAnimation(.spring(response: 0.5, dampingFraction: 0.6)) { resultTitle = picked.title }
            SoundEngine.shared.play(.success)
            Haptics.success()
            confetti += 1
            HistoryLog.add(context, tool: ToolKind.wheel.rawValue, title: template?.name ?? "", result: picked.title)
            if template?.removeAfterSpin == true {
                Task {
                    try? await Task.sleep(for: .seconds(1.2))
                    withAnimation(.snappy) { _ = eliminated.insert(picked.id) }
                }
            }
        }
    }

    private func create() {
        let template = WheelTemplate(name: "新转盘")
        context.insert(template)
        for (i, title) in ["选项一", "选项二", "选项三"].enumerated() {
            let option = WheelOption(title: title, sortIndex: i)
            option.template = template
            context.insert(option)
        }
        try? context.save()
        lastTemplateID = template.id.uuidString
        editing = template
    }
}

struct WheelEditor: View {
    @Bindable var template: WheelTemplate
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @State private var newOption = ""
    @State private var confirmDelete = false

    var body: some View {
        List {
            Section("名称") {
                TextField("转盘名称", text: $template.name)
                Toggle("淘汰模式", isOn: $template.removeAfterSpin)
            }
            .glassRows()

            Section {
                ForEach(template.sortedOptions) { option in
                    WheelOptionRow(option: option)
                }
                .onDelete { offsets in
                    let options = template.sortedOptions
                    for index in offsets { context.delete(options[index]) }
                    try? context.save()
                }
                .onMove { from, to in
                    var options = template.sortedOptions
                    options.move(fromOffsets: from, toOffset: to)
                    for (i, option) in options.enumerated() { option.sortIndex = i }
                }
                HStack {
                    TextField("添加选项", text: $newOption).onSubmit(add)
                    Button("添加", systemImage: "plus.circle.fill", action: add)
                        .labelStyle(.iconOnly)
                        .disabled(newOption.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            } header: {
                Text("选项")
            } footer: {
                Text("权重决定扇区大小和被转到的概率。")
            }
            .glassRows()

            Section {
                Button("删除这个转盘", role: .destructive) { confirmDelete = true }
            }
            .glassRows()
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle("编辑转盘")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) { Button("完成", systemImage: "checkmark") { try? context.save(); dismiss() } }
            ToolbarItem(placement: .topBarLeading) { EditButton() }
        }
        .confirmationDialog("删除「\(template.name)」？", isPresented: $confirmDelete, titleVisibility: .visible) {
            Button("删除", role: .destructive) {
                context.delete(template)
                try? context.save()
                dismiss()
            }
        }
    }

    private func add() {
        let title = newOption.trimmingCharacters(in: .whitespaces)
        guard !title.isEmpty else { return }
        let option = WheelOption(title: title, sortIndex: template.options?.count ?? 0)
        option.template = template
        context.insert(option)
        try? context.save()
        newOption = ""
    }
}

private struct WheelOptionRow: View {
    @Bindable var option: WheelOption

    var body: some View {
        HStack {
            TextField("选项", text: $option.title)
            Stepper("×\(Int(option.weight))", value: $option.weight, in: 0...10, step: 1)
                .font(.caption.monospacedDigit())
                .fixedSize()
        }
    }
}
