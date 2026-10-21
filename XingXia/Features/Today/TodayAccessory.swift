import SwiftUI
import Combine

/// 底栏上方常驻的今日胶囊：「今日 80 分 · 卯时吉」，点一下打开运势详解。
/// tab 栏随滚动缩小时配件挪到 tab 栏旁边（inline），只留分数和时辰。
struct TodayAccessory: View {
    /// 配件是否以展开形态浮在 tab 栏上方：RootView 据此把决策浮层抬高，不压住配件
    @Binding var isExpanded: Bool

    @Environment(AppState.self) private var app
    @Environment(\.palette) private var palette
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.tabViewBottomAccessoryPlacement) private var placement
    /// 零点、回到前台时刷新，跨天后不显示昨天的分数
    @State private var today = DayKey.today

    var body: some View {
        let fortune = FortuneEngine.daily(for: app.birth, on: today)
        let hasProfile = app.primary != nil
        let isInline = placement == .inline
        let isExpandedPlacement = placement == .expanded
        Button {
            Haptics.selection()
            app.open(.fortune)
        } label: {
            HStack(spacing: 6) {
                Image(systemName: hasProfile ? "sparkles" : "person.crop.circle.badge.plus")
                    .foregroundStyle(palette.accentGradient)
                if hasProfile {
                    if !isInline { Text("今日") }
                    Text("\(fortune.score)")
                        .font(.headline.weight(.bold))
                        .monospacedDigit()
                        .foregroundStyle(palette.accentGradient)
                    Text("分")
                    Text("·").foregroundStyle(.secondary)
                    Text(isInline ? fortune.luckyHour.hourName : "\(fortune.luckyHour.hourName)吉")
                } else {
                    Text(isInline ? "今日运势" : "建个档案，看今日运势")
                }
                if !isInline {
                    Spacer(minLength: 0)
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
            }
            .font(.subheadline.weight(.semibold))
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            .padding(.horizontal, isInline ? 12 : 16)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: isInline ? .center : .leading)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        // 配件高度由系统固定，字号封顶免得撑破；长按可以用大字查看器看
        .dynamicTypeSize(...DynamicTypeSize.xxxLarge)
        .accessibilityLabel(spokenLabel(fortune, hasProfile: hasProfile))
        .accessibilityHint("打开运势详解")
        .accessibilityShowsLargeContentViewer()
        .onAppear {
            refreshToday()
            isExpanded = isExpandedPlacement
        }
        .onChange(of: isExpandedPlacement) { _, expanded in isExpanded = expanded }
        // 展开、内联切换时系统可能先后各建一份配件，只让展开的那份在消失时清掉标记
        .onDisappear { if isExpandedPlacement { isExpanded = false } }
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged).receive(on: DispatchQueue.main)) { _ in
            refreshToday()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { refreshToday() }
        }
    }

    private func spokenLabel(_ fortune: DailyFortune, hasProfile: Bool) -> String {
        guard hasProfile else { return "建个档案，看今日运势" }
        return "今日运势 \(fortune.score) 分，\(fortune.levelName)，吉时是\(fortune.luckyHour.hourName)"
    }

    private func refreshToday() {
        let now = DayKey.today
        if now != today { today = now }
    }
}
