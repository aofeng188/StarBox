import SwiftUI
import SwiftData
import MapKit

struct FoodPickerView: View {
    enum Source: String, CaseIterable, Identifiable {
        case library, menus, nearby
        var id: String { rawValue }
        var name: String { ["library": "美食库", "menus": "我的菜单", "nearby": "附近餐厅"][rawValue]! }
    }

    struct Candidate: Equatable {
        var emoji: String
        var title: String
        var subtitle: String
        var mapItem: MKMapItem?
        var source: String
    }

    @Environment(\.modelContext) private var context
    @Environment(\.palette) private var palette
    @Query(sort: \FoodLog.date, order: .reverse) private var logs: [FoodLog]
    @Query private var blocked: [BlockedFood]
    @Query(sort: \FoodMenu.createdAt) private var menus: [FoodMenu]

    @AppStorage("food.smartDedupe") private var smartDedupe = true
    @State private var source: Source = .library
    @State private var meal: MealTime = .current
    @State private var cuisines: Set<String> = []
    @State private var tastes: Set<String> = []
    @State private var maxPrice = 3
    @State private var menuID: UUID?

    @State private var display: Candidate?
    @State private var result: Candidate?
    @State private var isRolling = false
    @State private var revealCount = 0
    @State private var confetti = 0

    @State private var nearby: [MKMapItem] = []
    @State private var userLocation: CLLocation?
    @State private var nearbyError: String?
    @State private var isSearching = false

    @State private var showMenus = false
    @State private var showBlocked = false

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Picker("来源", selection: $source.animation(.snappy)) {
                    ForEach(Source.allCases) { Text($0.name).tag($0) }
                }
                .pickerStyle(.segmented)

                filters

                // 结果卡
                VStack(spacing: 18) {
                    if let shown = display ?? result {
                        RollingResultView(emoji: shown.emoji, title: shown.title, subtitle: shown.subtitle,
                                          isRolling: isRolling, revealCount: revealCount)
                    } else {
                        VStack(spacing: 10) {
                            Text("🍽️").font(.system(size: 70))
                            Text("今天吃点什么好呢？").font(.title2.bold())
                            Text(candidateHint).font(.subheadline).foregroundStyle(.secondary)
                        }
                    }

                    if let result, !isRolling {
                        HStack(spacing: 10) {
                            Button("就吃这个", systemImage: "checkmark.circle.fill") { accept(result) }
                                .buttonStyle(.glassProminent)
                                .tint(palette.accent)
                            if let item = result.mapItem {
                                Button("导航", systemImage: "location.fill") {
                                    item.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeWalking])
                                }
                                .buttonStyle(.glass)
                            } else if source == .library {
                                Button("不想吃", systemImage: "hand.thumbsdown") { block(result.title) }
                                    .buttonStyle(.glass)
                            }
                        }
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .frame(maxWidth: .infinity, minHeight: 260)
                .glassCard(padding: 24)

                BigActionButton(title: result == nil ? "帮我选！" : "换一个", symbol: "dice.fill", isEnabled: !isRolling && !pool.isEmpty) {
                    roll()
                }
                Text("摇一摇手机也可以").font(.caption).foregroundStyle(.secondary)

                if source == .library {
                    Toggle(isOn: $smartDedupe) {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("智能去重")
                            Text("最近几天吃过的会降低被抽中的概率").font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    .glassCard(padding: 16)
                }

                if source == .nearby && !nearby.isEmpty {
                    nearbyList
                }

                recentSection
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .overlay { ConfettiView(trigger: confetti) }
        .cosmicBackground()
        .navigationTitle("今天吃什么")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("管理我的菜单", systemImage: "list.bullet.rectangle") { showMenus = true }
                    Button("黑名单（\(blocked.count)）", systemImage: "hand.raised") { showBlocked = true }
                } label: {
                    Image(systemName: "ellipsis")
                }
            }
        }
        .sheet(isPresented: $showMenus) { FoodMenusView() }
        .sheet(isPresented: $showBlocked) { BlockedFoodsView() }
        .onShake { if !isRolling && !pool.isEmpty { roll() } }
        .onChange(of: source) { _, new in
            result = nil
            display = nil
            if new == .nearby && nearby.isEmpty { Task { await searchNearby() } }
            if new == .menus && menuID == nil { menuID = menus.first?.id }
        }
    }

    // MARK: 筛选

    @ViewBuilder
    private var filters: some View {
        switch source {
        case .library:
            VStack(alignment: .leading, spacing: 10) {
                // 五个餐段在窄屏上放不下，改为可横向滑动，文字不换行
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(MealTime.allCases) { item in
                            Button {
                                meal = item
                                Haptics.selection()
                            } label: {
                                Label(item.name, systemImage: item.symbol)
                                    .labelStyle(.titleAndIcon)
                                    .font(.caption.weight(.semibold))
                                    .lineLimit(1)
                                    .fixedSize()
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 7)
                                    .foregroundStyle(meal == item ? .white : .primary)
                                    .background(Capsule().fill(meal == item ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.07))))
                            }
                            .buttonStyle(.plain)
                            .accessibilityAddTraits(meal == item ? .isSelected : [])
                        }
                    }
                    .padding(.horizontal, 16)
                }
                .pageBleed()
                ChipPicker(options: FoodLibrary.cuisines, selection: $cuisines, allLabel: "全部菜系")
                HStack {
                    ChipPicker(options: FoodLibrary.tastes, selection: $tastes, allLabel: "任意口味")
                }
                Picker("价位", selection: $maxPrice) {
                    Text("¥ 实惠").tag(1)
                    Text("¥¥ 以内").tag(2)
                    Text("不限价位").tag(3)
                }
                .pickerStyle(.segmented)
            }
        case .menus:
            if menus.isEmpty {
                Button {
                    showMenus = true
                } label: {
                    Label("创建第一个菜单（如：公司楼下、周末聚餐）", systemImage: "plus.circle.fill")
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 6)
                }
                .buttonStyle(.glass)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(menus) { menu in
                            Button {
                                menuID = menu.id
                                result = nil
                                Haptics.selection()
                            } label: {
                                Text("\(menu.icon) \(menu.name) · \(menu.items?.count ?? 0)")
                                    .font(.subheadline.weight(.medium))
                                    .padding(.horizontal, 12).padding(.vertical, 8)
                                    .foregroundStyle(menuID == menu.id ? .white : .primary)
                                    .background(Capsule().fill(menuID == menu.id ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(.primary.opacity(0.07))))
                            }
                            .buttonStyle(.plain)
                        }
                        Button { showMenus = true } label: {
                            Image(systemName: "pencil").padding(9).background(Circle().fill(.primary.opacity(0.07)))
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("编辑菜单")
                    }
                    .padding(.horizontal, 16)
                }
                .pageBleed()
            }
        case .nearby:
            HStack {
                if isSearching {
                    ProgressView()
                    Text("正在搜索附近 1.5 公里内的餐厅…").font(.subheadline).foregroundStyle(.secondary)
                } else if let nearbyError {
                    Label(nearbyError, systemImage: "location.slash").font(.subheadline).foregroundStyle(.secondary)
                } else {
                    Label("附近找到 \(nearby.count) 家店", systemImage: "mappin.and.ellipse").font(.subheadline)
                }
                Spacer()
                Button("刷新", systemImage: "arrow.clockwise") { Task { await searchNearby() } }
                    .labelStyle(.iconOnly)
                    .buttonStyle(.glass)
                    .disabled(isSearching)
            }
            .glassCard(padding: 14)
        }
    }

    private var nearbyList: some View {
        VStack(alignment: .leading, spacing: 10) {
            SectionHeader(title: "附近的店", symbol: "list.bullet")
            ForEach(nearby.prefix(12), id: \.self) { item in
                HStack {
                    Text(emoji(for: item))
                    VStack(alignment: .leading, spacing: 2) {
                        Text(item.name ?? "未知店铺").font(.subheadline.weight(.medium))
                        Text(distanceText(item)).font(.caption).foregroundStyle(.secondary)
                    }
                    Spacer()
                    Button {
                        item.openInMaps(launchOptions: [MKLaunchOptionsDirectionsModeKey: MKLaunchOptionsDirectionsModeWalking])
                    } label: {
                        Image(systemName: "arrow.triangle.turn.up.right.circle.fill").font(.title3)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(.tint)
                    .accessibilityLabel("导航到\(item.name ?? "这家店")")
                }
            }
        }
        .glassCard()
    }

    private var recentSection: some View {
        let recent = logs.prefix(6)
        return Group {
            if !recent.isEmpty {
                VStack(alignment: .leading, spacing: 10) {
                    SectionHeader(title: "最近吃过", subtitle: "本月 \(monthCount) 餐", symbol: "clock.arrow.circlepath")
                    ForEach(recent) { log in
                        HStack {
                            Text(log.name).font(.subheadline)
                            Spacer()
                            Text(DateText.relative(log.date)).font(.caption).foregroundStyle(.secondary)
                        }
                    }
                    if let top = topThisMonth {
                        Text("这个月吃得最多的是「\(top.0)」，一共 \(top.1) 次").font(.caption).foregroundStyle(.tint)
                    }
                }
                .glassCard()
            }
        }
    }

    // MARK: 数据

    private var pool: [Candidate] {
        switch source {
        case .library:
            let blockedNames = Set(blocked.map(\.name))
            return FoodLibrary.filter(meal: meal, cuisines: cuisines, tastes: tastes, maxPrice: maxPrice)
                .filter { !blockedNames.contains($0.name) }
                .map { Candidate(emoji: $0.emoji, title: $0.name, subtitle: $0.detail, source: "library") }
        case .menus:
            let menu = menus.first { $0.id == menuID } ?? menus.first
            return (menu?.sortedItems ?? []).filter { $0.weight > 0 }.map {
                Candidate(emoji: menu?.icon ?? "🍱", title: $0.name, subtitle: $0.note.isEmpty ? (menu?.name ?? "") : $0.note, source: "menu")
            }
        case .nearby:
            return nearby.map { Candidate(emoji: emoji(for: $0), title: $0.name ?? "附近的店", subtitle: distanceText($0), mapItem: $0, source: "nearby") }
        }
    }

    private var candidateHint: String {
        switch source {
        case .library: "从 \(pool.count) 道菜里帮你挑一道"
        case .menus: pool.isEmpty ? "先在菜单里添加几家常去的店" : "从「\((menus.first { $0.id == menuID } ?? menus.first)?.name ?? "")」的 \(pool.count) 个选项里挑"
        case .nearby: nearby.isEmpty ? "需要定位权限来查找附近的餐厅" : "从附近 \(nearby.count) 家店里挑一家"
        }
    }

    private var monthCount: Int {
        let today = DayKey.today
        return logs.filter { let d = DayKey($0.date); return d.year == today.year && d.month == today.month }.count
    }

    private var topThisMonth: (String, Int)? {
        let today = DayKey.today
        let names = logs.filter { let d = DayKey($0.date); return d.year == today.year && d.month == today.month }.map(\.name)
        let counts = Dictionary(names.map { ($0, 1) }, uniquingKeysWith: +)
        guard let best = counts.max(by: { $0.value < $1.value }), best.value > 1 else { return nil }
        return (best.key, best.value)
    }

    private func roll() {
        let candidates = pool
        guard !candidates.isEmpty else { return }
        let weights: [Double] = switch source {
        case .library: candidates.map { FoodLibrary.weight(for: $0.title, logs: logs, smart: smartDedupe) }
        case .menus: ((menus.first { $0.id == menuID } ?? menus.first)?.sortedItems ?? []).filter { $0.weight > 0 }.map(\.weight)
        case .nearby: candidates.map { _ in 1 }
        }
        let index = TrueRandom.weightedIndex(weights) ?? 0
        let final = candidates[min(index, candidates.count - 1)]
        isRolling = true
        result = nil
        SoundEngine.shared.play(.whoosh, volume: 0.6)
        Task {
            await Roller.roll(pool: candidates, final: final) { display = $0 }
            isRolling = false
            result = final
            display = nil
            revealCount += 1
            SoundEngine.shared.play(.success)
            Haptics.success()
            HistoryLog.add(context, tool: source == .nearby ? "nearby" : ToolKind.food.rawValue,
                           title: source == .library ? meal.name : source.name, result: final.title)
        }
    }

    private func accept(_ candidate: Candidate) {
        context.insert(FoodLog(name: candidate.title, source: candidate.source))
        try? context.save()
        confetti += 1
        Haptics.success()
        SoundEngine.shared.play(.sparkle)
    }

    private func block(_ name: String) {
        context.insert(BlockedFood(name: name))
        try? context.save()
        Haptics.warning()
        roll()
    }

    private func searchNearby() async {
        isSearching = true
        nearbyError = nil
        defer { isSearching = false }
        do {
            let (location, items) = try await LocationService.shared.nearbyRestaurants()
            userLocation = location
            nearby = items.sorted { $0.location.distance(from: location) < $1.location.distance(from: location) }
            if nearby.isEmpty { nearbyError = "附近没有找到餐厅" }
        } catch {
            nearbyError = error.localizedDescription
        }
    }

    private func distanceText(_ item: MKMapItem) -> String {
        guard let userLocation else { return item.address?.shortAddress ?? "" }
        let meters = item.location.distance(from: userLocation)
        let distance = meters < 1000 ? "\(Int(meters)) 米" : String(format: "%.1f 公里", meters / 1000)
        return [distance, item.address?.shortAddress].compactMap { $0 }.joined(separator: " · ")
    }

    private func emoji(for item: MKMapItem) -> String {
        switch item.pointOfInterestCategory {
        case .cafe: "☕"
        case .bakery: "🥐"
        default: "🍴"
        }
    }
}

// MARK: - 黑名单

struct BlockedFoodsView: View {
    @Environment(\.modelContext) private var context
    @Environment(\.dismiss) private var dismiss
    @Query(sort: \BlockedFood.date, order: .reverse) private var blocked: [BlockedFood]

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(blocked) { item in
                        Text(item.name)
                    }
                    .onDelete { offsets in
                        for index in offsets { context.delete(blocked[index]) }
                        try? context.save()
                    }
                } footer: {
                    Text("黑名单里的菜不会被抽中。左滑可以移出黑名单。")
                }
                .glassRows()
            }
            .overlay {
                if blocked.isEmpty { EmptyHint(symbol: "hand.raised", title: "黑名单是空的", message: "抽到不想吃的菜时，点「不想吃」即可加入") }
            }
            .cosmicBackground(dimmed: true)
            .navigationTitle("黑名单")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button("完成", systemImage: "checkmark") { dismiss() } }
            }
        }
    }
}
