import SwiftUI
import SwiftData
import RealityKit

/// RealityKit 物理骰子世界
@MainActor
final class DiceWorld {
    let root = Entity()
    private(set) var dice: [ModelEntity] = []
    private let size: Float = 0.115
    private var faceMaterials: [Int: PhysicallyBasedMaterial] = [:]
    private var bodyMaterial = PhysicallyBasedMaterial()
    private var lastTransforms: [simd_float4x4] = []
    private var stillFrames = 0
    private var rollTime: Double = 0
    private var lastHit: Date = .distantPast
    private(set) var isRolling = false
    var subscriptions: [EventSubscription] = []
    var onSettled: (([Int]) -> Void)?

    /// 面朝向（本地坐标）→ 点数。对面之和为 7。
    private let faces: [(SIMD3<Float>, Int)] = [
        ([0, 1, 0], 1), ([0, -1, 0], 6), ([0, 0, 1], 2), ([0, 0, -1], 5), ([1, 0, 0], 3), ([-1, 0, 0], 4),
    ]

    func build(into content: inout RealityViewCameraContent, feltColor: UIColor) {
        content.camera = .virtual

        let camera = PerspectiveCamera()
        camera.camera.fieldOfViewInDegrees = 50
        // 对准围栏中心、骰子半高处：骰子能停到的范围在画面里上下对称，停在中央的骰子正好在画面正中
        let focus: SIMD3<Float> = [0, size / 2, 0]
        camera.look(at: focus, from: focus + [0, 0.84, 0.44], relativeTo: nil)
        root.addChild(camera)

        // 主光 + 阴影
        let sun = DirectionalLight()
        sun.light.intensity = 4200
        sun.components.set(DirectionalLightComponent.Shadow(maximumDistance: 2, depthBias: 2))
        sun.look(at: .zero, from: [0.4, 1.2, 0.6], relativeTo: nil)
        root.addChild(sun)
        let fill = DirectionalLight()
        fill.light.intensity = 1600
        fill.look(at: .zero, from: [-0.6, 0.8, -0.4], relativeTo: nil)
        root.addChild(fill)
        let glow = PointLight()
        glow.light.intensity = 9000
        glow.light.attenuationRadius = 2
        glow.position = [0, 0.6, 0.2]
        root.addChild(glow)

        // 绒布桌面
        var felt = PhysicallyBasedMaterial()
        felt.baseColor = .init(tint: feltColor)
        felt.roughness = 1.0
        felt.metallic = 0.0
        let floor = ModelEntity(mesh: .generatePlane(width: 4, depth: 4), materials: [felt])
        floor.components.set(CollisionComponent(shapes: [ShapeResource.generateBox(width: 4, height: 0.04, depth: 4).offsetBy(translation: [0, -0.02, 0])]))
        floor.components.set(PhysicsBodyComponent(massProperties: .default,
                                                  material: .generate(staticFriction: 0.7, dynamicFriction: 0.5, restitution: 0.35),
                                                  mode: .static))
        root.addChild(floor)

        // 看不见的围栏
        let halfX: Float = 0.29
        let halfZ: Float = 0.31
        let walls: [(SIMD3<Float>, SIMD3<Float>)] = [
            ([halfX + 0.05, 0.3, 0], [0.1, 0.6, 2]), ([-halfX - 0.05, 0.3, 0], [0.1, 0.6, 2]),
            ([0, 0.3, halfZ + 0.05], [2, 0.6, 0.1]), ([0, 0.3, -halfZ - 0.05], [2, 0.6, 0.1]),
            ([0, 0.65, 0], [2, 0.1, 2]),
        ]
        for (position, extent) in walls {
            let wall = Entity()
            wall.position = position
            wall.components.set(CollisionComponent(shapes: [.generateBox(size: extent)]))
            wall.components.set(PhysicsBodyComponent(massProperties: .default,
                                                     material: .generate(staticFriction: 0.2, dynamicFriction: 0.2, restitution: 0.6),
                                                     mode: .static))
            root.addChild(wall)
        }

        bodyMaterial.baseColor = .init(tint: UIColor(white: 0.97, alpha: 1))
        bodyMaterial.roughness = 0.3
        bodyMaterial.clearcoat = .init(floatLiteral: 0.8)
        for value in 1...6 {
            var material = PhysicallyBasedMaterial()
            if let image = Self.faceImage(value), let texture = try? TextureResource(image: image, options: .init(semantic: .color)) {
                material.baseColor = .init(tint: .white, texture: .init(texture))
            }
            material.roughness = 0.3
            material.clearcoat = .init(floatLiteral: 0.8)
            faceMaterials[value] = material
        }

        content.add(root)

        subscriptions.append(content.subscribe(to: SceneEvents.Update.self) { [weak self] event in
            MainActor.assumeIsolated { self?.update(deltaTime: event.deltaTime) }
        })
        subscriptions.append(content.subscribe(to: CollisionEvents.Began.self) { [weak self] event in
            MainActor.assumeIsolated { self?.collision(impulse: event.impulse) }
        })
    }

    private func makeDie() -> ModelEntity {
        let die = ModelEntity(mesh: .generateBox(size: size, cornerRadius: size * 0.12), materials: [bodyMaterial])
        let faceSize = size * 0.74
        let mesh = MeshResource.generatePlane(width: faceSize, depth: faceSize, cornerRadius: size * 0.1)
        for (normal, value) in faces {
            let face = ModelEntity(mesh: mesh, materials: [faceMaterials[value] ?? bodyMaterial])
            face.position = normal * (size / 2 + 0.0006)
            face.orientation = Self.rotation(from: [0, 1, 0], to: normal)
            die.addChild(face)
        }
        die.components.set(CollisionComponent(shapes: [.generateBox(size: [size, size, size])]))
        die.components.set(PhysicsBodyComponent(shapes: [.generateBox(size: [size, size, size])], mass: 0.05,
                                                material: .generate(staticFriction: 0.6, dynamicFriction: 0.45, restitution: 0.45),
                                                mode: .dynamic))
        return die
    }

    /// 第 index 颗骰子的摆放格位：每行最多三颗，每行各自左右居中
    private static func slot(_ index: Int, of count: Int) -> (column: Float, row: Int, rows: Int) {
        let row = index / 3
        let inRow = min(3, count - row * 3)
        return (Float(index % 3) - Float(inRow - 1) / 2, row, (count + 2) / 3)
    }

    func setCount(_ count: Int) {
        while dice.count < count {
            let die = makeDie()
            root.addChild(die)
            dice.append(die)
        }
        while dice.count > count {
            dice.removeLast().removeFromParent()
        }
        // 重新整齐摆在桌面正中，新加的骰子也不会和停在原处的骰子重叠
        for (index, die) in dice.enumerated() {
            let slot = Self.slot(index, of: dice.count)
            die.position = [slot.column * 0.14, size / 2 + 0.001, (Float(slot.row) - Float(slot.rows - 1) / 2) * 0.15]
            die.orientation = simd_quatf(angle: 0, axis: [0, 1, 0])
            die.components.set(PhysicsMotionComponent(linearVelocity: .zero, angularVelocity: .zero))
        }
    }

    func roll() {
        guard !dice.isEmpty else { return }
        isRolling = true
        rollTime = 0
        stillFrames = 0
        lastTransforms = []
        for (index, die) in dice.enumerated() {
            // 最多三列，多出来的叠在上一层，避免初始位置互相穿插
            let slot = Self.slot(index, of: dice.count)
            die.position = [slot.column * 0.14 + Float.random(in: -0.02...0.02), 0.26 + Float(slot.row) * 0.15, 0.18 + Float.random(in: -0.03...0.03)]
            die.orientation = simd_quatf(angle: Float.random(in: 0...(2 * .pi)), axis: simd_normalize([Float.random(in: -1...1), Float.random(in: -1...1), Float.random(in: -1...1)] + [0.001, 0, 0]))
            // 从靠近镜头的一侧往里抛；这个力度下骰子平均停在桌面中央，而不是堆到里侧围栏边
            die.components.set(PhysicsMotionComponent(
                linearVelocity: [Float.random(in: -0.5...0.5), -0.5, Float.random(in: -0.75 ... -0.35)],
                angularVelocity: [Float.random(in: -22...22), Float.random(in: -22...22), Float.random(in: -22...22)]
            ))
        }
    }

    private func update(deltaTime: Double) {
        guard isRolling else { return }
        rollTime += deltaTime
        let transforms = dice.map { $0.transformMatrix(relativeTo: nil) }
        for (index, die) in dice.enumerated() where die.position(relativeTo: nil).y < -0.3 {
            die.position = [0, 0.3, 0]
            _ = index
        }
        if lastTransforms.count == transforms.count {
            let moving = zip(transforms, lastTransforms).contains { a, b in
                simd_distance(a.columns.3, b.columns.3) > 0.0005
                    || simd_distance(a.columns.0, b.columns.0) > 0.004
                    || simd_distance(a.columns.1, b.columns.1) > 0.004
            }
            stillFrames = moving ? 0 : stillFrames + 1
        }
        lastTransforms = transforms
        if (rollTime > 0.7 && stillFrames > 18) || rollTime > 7 {
            isRolling = false
            onSettled?(dice.map(topValue))
        }
    }

    private func collision(impulse: Float) {
        guard impulse > 0.02 else { return }
        let now = Date()
        guard now.timeIntervalSince(lastHit) > 0.05 else { return }
        lastHit = now
        let strength = min(1, impulse / 0.25)
        SoundEngine.shared.play(.diceHit, volume: 0.3 + strength * 0.7)
        Haptics.rigidImpact(CGFloat(0.3 + strength * 0.7))
    }

    private func topValue(_ die: ModelEntity) -> Int {
        let orientation = die.orientation(relativeTo: nil)
        return faces.max { simd_dot(orientation.act($0.0), [0, 1, 0]) < simd_dot(orientation.act($1.0), [0, 1, 0]) }?.1 ?? 1
    }

    private static func rotation(from a: SIMD3<Float>, to b: SIMD3<Float>) -> simd_quatf {
        let dot = simd_dot(a, b)
        if dot > 0.999 { return simd_quatf(angle: 0, axis: [1, 0, 0]) }
        if dot < -0.999 { return simd_quatf(angle: .pi, axis: [1, 0, 0]) }
        return simd_quatf(from: a, to: b)
    }

    /// 点数贴图：传统中式骰子，一点和四点是红色
    static func faceImage(_ value: Int) -> CGImage? {
        let side: CGFloat = 256
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: side, height: side))
        let image = renderer.image { _ in
            UIColor(white: 0.97, alpha: 1).setFill()
            UIBezierPath(rect: CGRect(x: 0, y: 0, width: side, height: side)).fill()
            let a: CGFloat = 0.24, b: CGFloat = 0.5, c: CGFloat = 0.76
            let layout: [[(CGFloat, CGFloat)]] = [
                [(b, b)],
                [(a, a), (c, c)],
                [(a, a), (b, b), (c, c)],
                [(a, a), (c, a), (a, c), (c, c)],
                [(a, a), (c, a), (b, b), (a, c), (c, c)],
                [(a, a), (a, b), (a, c), (c, a), (c, b), (c, c)],
            ]
            let red = value == 1 || value == 4
            let color = red ? UIColor(red: 0.86, green: 0.12, blue: 0.16, alpha: 1) : UIColor(red: 0.12, green: 0.13, blue: 0.2, alpha: 1)
            let radius: CGFloat = value == 1 ? 46 : 23
            for (x, y) in layout[value - 1] {
                let center = CGPoint(x: x * side, y: y * side)
                let rect = CGRect(x: center.x - radius, y: center.y - radius, width: radius * 2, height: radius * 2)
                // 凹陷感：内阴影 + 主色
                UIColor(white: 0, alpha: 0.18).setFill()
                UIBezierPath(ovalIn: rect.offsetBy(dx: 0, dy: -2)).fill()
                color.setFill()
                UIBezierPath(ovalIn: rect).fill()
            }
        }
        return image.cgImage
    }
}

struct DiceView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @AppStorage("dice.count") private var count = 2
    @State private var world = DiceWorld()
    @State private var results: [Int] = []
    @State private var isRolling = false
    @State private var history: [[Int]] = []
    @State private var revealCount = 0

    var body: some View {
        StageScaffold(spacing: 14) {
            // 结果
            HStack(spacing: 10) {
                if results.isEmpty || isRolling {
                    Text(isRolling ? "骰子滚动中…" : "点按桌面或摇一摇掷骰子")
                        .font(.headline)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(Array(results.enumerated()), id: \.offset) { _, value in
                        Image(systemName: "die.face.\(value).fill")
                            .font(.system(size: 34))
                            .foregroundStyle(value == 1 || value == 4 ? Color(hex: 0xEF4444) : .primary)
                    }
                    if results.count > 1 {
                        Text("= \(results.reduce(0, +))")
                            .font(.system(size: 34, weight: .bold, design: .rounded))
                            .foregroundStyle(palette.accentGradient)
                            .contentTransition(.numericText())
                    }
                }
            }
            .frame(height: 50)
            .keyframeAnimator(initialValue: 1.0, trigger: revealCount) { view, scale in
                view.scaleEffect(scale)
            } keyframes: { _ in
                CubicKeyframe(1.2, duration: 0.12)
                SpringKeyframe(1, duration: 0.4, spring: .bouncy)
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(results.isEmpty ? "还没有结果" : Self.spoken(results))
        } stage: {
            RealityView { content in
                world.build(into: &content, feltColor: UIColor(palette.isDark ? palette.backgroundBottom : palette.accent.opacity(0.85)))
                world.setCount(count)
                world.onSettled = { values in settled(values) }
            }
            .clipShape(.rect(cornerRadius: 30))
            .overlay(RoundedRectangle(cornerRadius: 30).strokeBorder(palette.accentGradient.opacity(0.6), lineWidth: 1.5))
            .shadow(color: palette.accent.opacity(0.3), radius: 16)
            .onTapGesture { roll() }
            .accessibilityAddTraits(.isButton)
            .accessibilityLabel("骰子桌面，点按掷骰子")
        } bottom: {
            HStack(spacing: 14) {
                Stepper("骰子 \(count) 颗", value: $count, in: 1...6)
                    .onChange(of: count) { _, new in
                        world.setCount(new)
                        results = []
                    }
                    .disabled(isRolling)
            }
            .glassCard(padding: 14)

            BigActionButton(title: "掷骰子", symbol: "dice.fill", isEnabled: !isRolling) { roll() }

            if !history.isEmpty {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(Array(history.enumerated().reversed()), id: \.offset) { _, values in
                            Text("\(values.map(String.init).joined(separator: "+"))=\(values.reduce(0, +))")
                                .font(.caption.monospacedDigit())
                                .padding(.horizontal, 10).padding(.vertical, 6)
                                .glassEffect(.regular, in: .capsule)
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
            }
        }
        .padding(.bottom, 12)
        .cosmicBackground()
        .navigationTitle("3D 骰子")
        .navigationBarTitleDisplayMode(.inline)
        .onShake { roll() }
    }

    private func roll() {
        guard !isRolling else { return }
        isRolling = true
        SoundEngine.shared.play(.whoosh, volume: 0.5)
        Haptics.rattle(count: 5)
        world.roll()
    }

    private func settled(_ values: [Int]) {
        isRolling = false
        withAnimation(.snappy) { results = values }
        revealCount += 1
        history.append(values)
        if history.count > 12 { history.removeFirst() }
        Haptics.success()
        Announce.post(Self.spoken(values))
        let total = values.reduce(0, +)
        HistoryLog.add(context, tool: ToolKind.dice.rawValue, title: "\(values.count) 颗",
                       result: values.count > 1 ? "\(values.map(String.init).joined(separator: "+")) = \(total)" : "\(total) 点")
    }

    /// 读屏用的结果：「点数 3、5，合计 8」，只有一颗时是「点数 3」
    private static func spoken(_ values: [Int]) -> String {
        let points = "点数 \(values.map(String.init).joined(separator: "、"))"
        return values.count > 1 ? "\(points)，合计 \(values.reduce(0, +))" : points
    }
}
