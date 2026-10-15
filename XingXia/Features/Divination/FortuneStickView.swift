import SwiftUI
import SwiftData

struct FortuneStickView: View {
    enum Phase { case idle, shaking, dropped, revealed }

    /// 「为家人朋友求签」时选中的人
    struct Target: Equatable {
        let id: UUID
        let name: String
        let avatar: String
    }

    @Environment(\.modelContext) private var context
    @Environment(AppState.self) private var app
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Query(sort: \StickDraw.date, order: .reverse) private var draws: [StickDraw]

    @State private var phase: Phase = .idle
    @State private var energy: Double = 0
    @State private var isHolding = false
    @State private var stick: FortuneStick?
    @State private var wish = "综合"
    @State private var wobble: Double = 0
    @State private var stickOut = false
    @State private var lastRattle = Date.distantPast
    @State private var confetti = 0
    /// 为谁求签；nil 表示当前档案自己
    @State private var target: Target?
    @State private var choosingProfile = false
    @State private var loaded = false
    @State private var isVisible = false
    @State private var motionActive = false
    @State private var loop: Task<Void, Never>?

    private let wishes = ["综合", "事业", "感情", "财运", "健康", "学业", "出行"]

    private func draw(for profileID: UUID?) -> StickDraw? {
        let today = DayKey.today.rawValue
        return draws.first { $0.dayKey == today && $0.profileID == profileID }
    }

    private var todayDraw: StickDraw? { draw(for: app.primary?.id) }

    var body: some View {
        ZStack {
            if phase == .revealed, let stick {
                StickResultView(stick: stick, wish: wish,
                                forName: target.map { "\($0.avatar) \($0.name)" },
                                onAgain: { choosingProfile = true },
                                onMine: { backToMine() })
                    // 换了一支签或换了人就当新页面，重新盖章
                    .id("\(stick.n)-\(target?.id.uuidString ?? "me")")
                    .transition(.asymmetric(insertion: .scale(scale: 0.9).combined(with: .opacity), removal: .opacity))
            } else {
                shakeStage
            }
            ConfettiView(trigger: confetti)
        }
        .cosmicBackground()
        .navigationTitle("求签")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.hidden, for: .tabBar)
        .onAppear {
            isVisible = true
            // 只在第一次出现时自动揭示今天的签：切标签回来不能打断「为家人朋友求签」
            if !loaded {
                loaded = true
                if let todayDraw, phase == .idle {
                    stick = StickLibrary.stick(todayDraw.stickNumber)
                    phase = .revealed
                }
            }
            syncShaking()
        }
        .onDisappear {
            isVisible = false
            syncShaking()
        }
        .onChange(of: phase) { syncShaking() }
        .onChange(of: choosingProfile) { syncShaking() }
        .onChange(of: settings.shakeEnabled) { syncShaking() }
        .onShake(isEnabled: !choosingProfile) {
            if phase == .idle || phase == .shaking { energy += 0.45; phase = .shaking; rattle() }
        }
        .sheet(isPresented: $choosingProfile) {
            NavigationStack {
                StickProfilePicker { choose($0) }
            }
            .presentationDetents([.medium, .large])
        }
    }

    // MARK: 摇签舞台

    private var shakeStage: some View {
        StageScaffold(spacing: 22, minHeight: 500) {
            VStack(spacing: 8) {
                Text(phase == .dropped ? "签已落地" : "心中默念所求之事")
                    .font(AppFont.serif(26, weight: .bold))
                    .contentTransition(.opacity)
                Text(phase == .dropped ? "点按签文，看看今天的指引" : (settings.shakeEnabled ? "摇动手机，或按住下方按钮摇签" : "按住下方按钮摇签"))
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(.top, 20)

            if let target {
                HStack(spacing: 8) {
                    Text("为 \(target.avatar) \(target.name) 求签").font(.subheadline.weight(.semibold))
                    Button("改为自己", systemImage: "xmark.circle.fill") { backToMine() }
                        .labelStyle(.iconOnly)
                        .foregroundStyle(.secondary)
                        .accessibilityLabel("不为\(target.name)求了，回到自己的签")
                }
                .padding(.leading, 14)
                .padding(.trailing, 10)
                .padding(.vertical, 6)
                .background(.primary.opacity(0.08), in: .capsule)
            }

            if phase == .idle {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(wishes, id: \.self) { item in
                            Button(item) {
                                wish = item
                                Haptics.selection()
                            }
                            .font(.subheadline.weight(.medium))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .foregroundStyle(wish == item ? palette.onAccent : .primary)
                            .background(Capsule().fill(wish == item ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.08))))
                            .accessibilityAddTraits(wish == item ? .isSelected : [])
                        }
                    }
                    .padding(.horizontal, 16)
                }
                // 横条铺到屏幕边缘，两端渐隐，提示还能滑动
                .mask {
                    HStack(spacing: 0) {
                        LinearGradient(colors: [.clear, .black], startPoint: .leading, endPoint: .trailing).frame(width: 16)
                        Rectangle()
                        LinearGradient(colors: [.black, .clear], startPoint: .leading, endPoint: .trailing).frame(width: 16)
                    }
                }
                .pageBleed()
                .transition(.opacity)
            }
        } stage: {
            ScaleToFit(design: CGSize(width: 320, height: 360)) {
                ZStack {
                    // 光晕
                    Circle()
                        .fill(RadialGradient(colors: [Color(hex: 0xFFB347).opacity(0.45 + energy * 0.4), .clear], center: .center, startRadius: 0, endRadius: 190))
                        .frame(width: 380, height: 380)
                        .scaleEffect(1 + energy * 0.15)
                        .blur(radius: 10)

                    if phase == .dropped, let stick {
                        Button {
                            reveal()
                        } label: {
                            SingleStick(number: stick.n, glowing: true)
                        }
                        .buttonStyle(.plain)
                        .rotationEffect(.degrees(stickOut ? -8 : -40))
                        .offset(x: stickOut ? 0 : 40, y: stickOut ? 20 : -160)
                        .scaleEffect(stickOut ? 1.1 : 0.6)
                        .accessibilityLabel("签文已落地，点按解签")
                    } else {
                        // 减弱动态效果时签筒不摇摆，只靠进度条和光晕反馈
                        let swing = phase == .shaking && !reduceMotion
                        StickTubeIllustration(size: 230, shake: reduceMotion ? 0 : wobble)
                            .rotationEffect(.degrees(swing ? sin(wobble * 2.6) * 9 * min(1, 0.3 + energy) : 0), anchor: .bottom)
                            .offset(y: swing ? CGFloat(sin(wobble * 5.1)) * 6 : 0)
                    }
                }
            }
        } bottom: {
            if phase != .dropped {
                VStack(spacing: 12) {
                    // 进度
                    ZStack(alignment: .leading) {
                        Capsule().fill(.primary.opacity(0.1))
                        Capsule()
                            .fill(LinearGradient(colors: [Color(hex: 0xFFD166), Color(hex: 0xFF6B35)], startPoint: .leading, endPoint: .trailing))
                            .frame(width: max(0, CGFloat(min(1, energy)) * 260))
                            .shadow(color: Color(hex: 0xFF6B35).opacity(0.7), radius: 6)
                    }
                    .frame(width: 260, height: 8)
                    .accessibilityLabel("摇签进度 \(Int(min(1, energy) * 100))%")

                    Text("按住摇签")
                        .font(.headline)
                        .foregroundStyle(.white)
                        .frame(width: 220, height: 56)
                        .background(Capsule().fill(LinearGradient(colors: [Color(hex: 0xE11D48), Color(hex: 0xF59E0B)], startPoint: .leading, endPoint: .trailing)))
                        .glassEffect(.regular.interactive(), in: .capsule)
                        .scaleEffect(isHolding ? 0.95 : 1)
                        .shadow(color: Color(hex: 0xE11D48).opacity(0.5), radius: 12)
                        .onLongPressGesture(minimumDuration: 10, maximumDistance: 60, pressing: { pressing in
                            withAnimation(.spring(response: 0.25)) { isHolding = pressing }
                        }, perform: {})
                        .accessibilityAddTraits(.isButton)
                        .accessibilityLabel("按住摇签")
                        .accessibilityAction { energy = 1.0; phase = .shaking }
                }
                .padding(.bottom, 30)
            } else {
                Button("解签") { reveal() }
                    .buttonStyle(.glassProminent)
                    .controlSize(.large)
                    .tint(Color(hex: 0xE11D48))
                    .padding(.bottom, 30)
            }
        }
    }

    // MARK: 逻辑

    /// 摇签循环只在页面可见、还在摇签阶段时运行；陀螺仪另外要求打开了「摇一摇触发」且没有弹出选人面板。
    /// start / stop 成对调用
    private func syncShaking() {
        let shaking = isVisible && (phase == .idle || phase == .shaking)
        if shaking {
            if loop == nil { startLoop() }
        } else {
            loop?.cancel()
            loop = nil
        }
        let wantsMotion = shaking && settings.shakeEnabled && !choosingProfile
        guard wantsMotion != motionActive else { return }
        motionActive = wantsMotion
        if wantsMotion { MotionManager.shared.start() } else { MotionManager.shared.stop() }
    }

    private func startLoop() {
        loop?.cancel()
        loop = Task { @MainActor in
            while !Task.isCancelled {
                try? await Task.sleep(for: .milliseconds(33))
                guard !Task.isCancelled else { break }
                tick()
            }
        }
    }

    private func tick() {
        guard phase == .idle || phase == .shaking, !choosingProfile else { return }
        // 关了「摇一摇触发」时不读运动强度，只认按住按钮
        let motion = motionActive ? MotionManager.shared.shakeIntensity : 0
        let intensity = max(motion > 0.25 ? min(1.2, motion) : 0, isHolding ? 0.85 : 0)
        if intensity > 0 {
            if phase == .idle { withAnimation(.snappy) { phase = .shaking } }
            energy += intensity * 0.014
            wobble += 0.22 + intensity * 0.25
            if Date().timeIntervalSince(lastRattle) > 0.28 { rattle() }
        } else if phase == .shaking {
            wobble += 0.05
            energy = max(0, energy - 0.002)
        }
        if energy >= 1 { drop() }
    }

    private func rattle() {
        lastRattle = Date()
        SoundEngine.shared.play(.stickRattle, volume: 0.8)
        Haptics.rattle(count: 4)
    }

    private func drop() {
        let number = TrueRandom.int(1...StickLibrary.all.count)
        stick = StickLibrary.stick(number)
        isHolding = false
        stickOut = false
        withAnimation(.snappy) { phase = .dropped }
        withAnimation(reduceMotion ? .none : .spring(response: 0.7, dampingFraction: 0.55).delay(0.05)) { stickOut = true }
        Task {
            try? await Task.sleep(for: .milliseconds(350))
            SoundEngine.shared.play(.stickDrop)
            Haptics.land(strength: 0.9)
        }
    }

    private func reveal() {
        guard let stick else { return }
        withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) { phase = .revealed }
        SoundEngine.shared.play(.chime)
        Haptics.reveal()
        if stick.levelInfo == .best || stick.levelInfo == .good { confetti += 1 }

        // 每人每天一签：记在求签对象（自己或选中的家人朋友）名下
        let owner = target?.id ?? app.primary?.id
        if draw(for: owner) == nil {
            context.insert(StickDraw(dayKey: DayKey.today.rawValue, profileID: owner, stickNumber: stick.n))
        }
        HistoryLog.add(context, tool: "stick", title: historyTitle, result: "\(stick.numberText) \(stick.levelInfo.displayName) · \(stick.title)")
        Announce.post((target.map { "为\($0.name)求得，" } ?? "") + stick.spokenText)
    }

    /// 决策历史里的标题。所求是「综合」时不写，免得出现「求综合」
    private var historyTitle: String {
        let who = target.map { "为\($0.name)" } ?? ""
        return wish == "综合" ? who : who + "求\(wish)"
    }

    /// 选好了为谁求签：对方今天已经求过就直接显示那一支，否则回到摇签
    private func choose(_ person: Target) {
        target = person
        wish = "综合"
        energy = 0
        wobble = 0
        isHolding = false
        if let existing = draw(for: person.id) {
            let drawn = StickLibrary.stick(existing.stickNumber)
            stick = drawn
            withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) { phase = .revealed }
            Announce.post("\(person.name)今天已经求过，" + drawn.spokenText)
        } else {
            stick = nil
            withAnimation(.snappy) { phase = .idle }
        }
    }

    private func backToMine() {
        target = nil
        wish = "综合"
        energy = 0
        wobble = 0
        isHolding = false
        if let todayDraw {
            stick = StickLibrary.stick(todayDraw.stickNumber)
            withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) { phase = .revealed }
        } else {
            stick = nil
            withAnimation(.snappy) { phase = .idle }
        }
    }
}

// MARK: - 为谁求签

/// 列出当前档案以外的家人朋友
private struct StickProfilePicker: View {
    let onPick: (FortuneStickView.Target) -> Void

    @Environment(\.dismiss) private var dismiss
    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]
    @Query(sort: \StickDraw.date, order: .reverse) private var draws: [StickDraw]
    @State private var creating = false

    var body: some View {
        let others = profiles.filter { $0.id != app.primary?.id }
        let today = DayKey.today.rawValue
        List {
            if others.isEmpty {
                Section {
                    EmptyHint(symbol: "person.2", title: "还没有家人朋友的档案", message: "添加他们的生日后，就能为他们各求一支今日签")
                }
                .listRowBackground(Color.clear)
            } else {
                Section {
                    ForEach(others) { profile in
                        let drawn = draws.first { $0.dayKey == today && $0.profileID == profile.id }
                        Button {
                            onPick(FortuneStickView.Target(id: profile.id, name: profile.name, avatar: profile.avatar))
                            dismiss()
                        } label: {
                            HStack(spacing: 14) {
                                Text(profile.avatar)
                                    .font(.title)
                                    .frame(width: 48, height: 48)
                                    .background(Circle().fill(palette.accent.opacity(0.18)))
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(profile.name).font(.headline)
                                    Text(profile.relation).font(.caption).foregroundStyle(.secondary)
                                }
                                Spacer()
                                if let drawn {
                                    Text("今日已求 · \(StickLibrary.stick(drawn.stickNumber).numberText)")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .contentShape(.rect)
                        }
                        .buttonStyle(.plain)
                    }
                } footer: {
                    Text("每人每天一签，今天已经求过的会直接显示那一支")
                }
                .glassRows()
            }

            Section {
                Button("添加家人朋友", systemImage: "person.badge.plus") { creating = true }
            }
            .glassRows()
        }
        .cosmicBackground(dimmed: true)
        .navigationTitle("为谁求签")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("取消") { dismiss() }
            }
        }
        .sheet(isPresented: $creating) {
            NavigationStack { ProfileEditorView(profile: nil) }
        }
    }
}

/// 掉出来的一支签
private struct SingleStick: View {
    let number: Int
    var glowing = false

    var body: some View {
        VStack(spacing: 0) {
            RoundedRectangle(cornerRadius: 4).fill(Color(hex: 0xC0392B)).frame(width: 34, height: 26)
            ZStack {
                RoundedRectangle(cornerRadius: 4)
                    .fill(LinearGradient(colors: [Color(hex: 0xF7E1B5), Color(hex: 0xD9AE6D)], startPoint: .leading, endPoint: .trailing))
                VStack(spacing: 4) {
                    ForEach(Array(Array("第\(StickLibrary.chineseNumber(number))签").enumerated()), id: \.offset) { _, char in
                        Text(String(char)).font(AppFont.serif(17, weight: .bold)).foregroundStyle(Color(hex: 0x7B1E1E))
                    }
                }
            }
            .frame(width: 34, height: 210)
        }
        .shadow(color: glowing ? Color(hex: 0xFFD166).opacity(0.9) : .clear, radius: 18)
    }
}

// MARK: - 解签结果

struct StickResultView: View {
    let stick: FortuneStick
    let wish: String
    /// 为家人朋友求的签写「为 某某 求得」；nil 是自己的签
    var forName: String?
    let onAgain: () -> Void
    /// 看完别人的签，回到自己的今日一签
    var onMine: () -> Void = {}

    @Environment(\.palette) private var palette
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var stamped = false

    var body: some View {
        ScrollView {
            VStack(spacing: 18) {
                if let forName {
                    Text("为 \(forName) 求得")
                        .font(.subheadline.weight(.semibold))
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                        .glassEffect(.regular, in: .capsule)
                }

                // 签面
                VStack(spacing: 18) {
                    HStack(alignment: .top) {
                        VStack(alignment: .leading, spacing: 6) {
                            Text(stick.numberText).font(AppFont.serif(30, weight: .bold))
                            Text("「\(stick.title)」").font(AppFont.serif(20)).foregroundStyle(.secondary)
                        }
                        Spacer()
                        // 印章
                        Text(stick.levelInfo.rawValue == "下" ? "下签" : stick.levelInfo.rawValue)
                            .font(AppFont.serif(stick.levelInfo.rawValue.count > 1 ? 22 : 26, weight: .bold))
                            .foregroundStyle(Color(hex: 0xE53935))
                            .frame(width: 74, height: 74)
                            .overlay(RoundedRectangle(cornerRadius: 10).strokeBorder(Color(hex: 0xE53935), lineWidth: 3))
                            // 减弱动态效果时不从大号斜着砸下来，只淡入
                            .rotationEffect(.degrees(stamped || reduceMotion ? -12 : -30))
                            .scaleEffect(stamped || reduceMotion ? 1 : 2.4)
                            .opacity(stamped ? 0.92 : 0)
                    }

                    // 竖排签诗（从右往左）
                    HStack(alignment: .top, spacing: 18) {
                        ForEach(Array(stick.poem.enumerated().reversed()), id: \.offset) { _, line in
                            VStack(spacing: 6) {
                                ForEach(Array(line.enumerated()), id: \.offset) { _, char in
                                    Text(String(char)).font(AppFont.serif(24, weight: .semibold))
                                }
                            }
                        }
                    }
                    .padding(.vertical, 18)
                    .frame(maxWidth: .infinity)
                    .background(
                        RoundedRectangle(cornerRadius: 18)
                            .fill(LinearGradient(colors: [Color(hex: 0xFFF6E0).opacity(0.12), Color(hex: 0xFFD166).opacity(0.06)], startPoint: .top, endPoint: .bottom))
                    )
                    .overlay(RoundedRectangle(cornerRadius: 18).strokeBorder(Color(hex: 0xFFD166).opacity(0.35), lineWidth: 1))
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(stick.poem.joined(separator: "，"))

                    VStack(alignment: .leading, spacing: 8) {
                        Text("解曰").font(AppFont.serif(17, weight: .bold)).foregroundStyle(stick.levelInfo.ink(scheme))
                        Text(stick.meaning).font(.body).lineSpacing(4)
                        Text(stick.levelInfo.blessing).font(.subheadline).foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .glassCard(padding: 22, tint: stick.levelInfo.color.opacity(0.08))

                // 分项
                VStack(alignment: .leading, spacing: 12) {
                    SectionHeader(title: "签文详解", subtitle: wish == "综合" ? nil : "所求：\(wish)", symbol: "list.bullet.rectangle.portrait")
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                        ForEach(stick.areas, id: \.0) { area, verdict in
                            let highlighted = area == wish || (wish == "学业" && area == "事业")
                            VStack(spacing: 4) {
                                Text(area).font(.caption).foregroundStyle(highlighted ? stick.levelInfo.ink(scheme) : .secondary)
                                Text(verdict).font(.subheadline.weight(.semibold)).lineLimit(1).minimumScaleFactor(0.7)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(highlighted ? stick.levelInfo.color.opacity(0.18) : .primary.opacity(0.05), in: .rect(cornerRadius: 14))
                            .overlay {
                                if highlighted { RoundedRectangle(cornerRadius: 14).strokeBorder(stick.levelInfo.color.opacity(0.6)) }
                            }
                        }
                    }
                }
                .glassCard()

                VStack(spacing: 10) {
                    Text("心诚则灵 · 一日一签")
                        .font(.footnote).foregroundStyle(.secondary)
                    Button("为家人朋友再求一签", systemImage: "person.2") { onAgain() }
                        .buttonStyle(.glass)
                    if forName != nil {
                        Button("看我的今日一签", systemImage: "arrow.uturn.backward") { onMine() }
                            .buttonStyle(.glass)
                    }
                }
                .padding(.bottom, 30)
            }
            .pageContent()
            .padding(.top, 8)
        }
        .onAppear {
            // 切标签回来时不再重放盖章的鼓声和震动
            guard !stamped else { return }
            withAnimation(reduceMotion ? .easeOut(duration: 0.3).delay(0.35) : .spring(response: 0.45, dampingFraction: 0.55).delay(0.35)) { stamped = true }
            Task {
                try? await Task.sleep(for: .milliseconds(500))
                SoundEngine.shared.play(.drum, volume: 0.8)
                Haptics.heavyImpact()
            }
        }
    }
}
