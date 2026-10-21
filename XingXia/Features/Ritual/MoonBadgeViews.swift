import SwiftUI
import SwiftData

/// 「我的」页的月相徽章卡：已获得几枚满月徽章、本轮集到了哪几种月相，点开看说明
struct MoonBadgeCard: View {
    @Environment(AppSettings.self) private var settings
    @Query(filter: #Predicate<RitualDay> { $0.skipped == false }) private var ritualDays: [RitualDay]
    @State private var showInfo = false

    var body: some View {
        let today = DayKey.today
        let progress = MoonBadges.progress(ritualDays: ritualDays.compactMap { DayKey(rawValue: $0.dayKey) }, today: today)
        let count = progress.collected.count
        Button {
            showInfo = true
        } label: {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .firstTextBaseline) {
                    SectionHeader(title: "月相徽章", subtitle: "已获得 \(progress.earned) 枚", symbol: "moon.stars.fill")
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.tertiary)
                }
                MoonPhaseStrip(collected: progress.collected)
                Text(settings.dailyRitualEnabled ? "本轮已集 \(count)/8 种月相" : "本轮已集 \(count)/8 种月相 · 每日仪式已关闭")
                    .font(.subheadline)
                    .subtleForeground()
            }
            .glassCard(interactive: true)
        }
        .buttonStyle(.pressableCard)
        .accessibilityLabel("月相徽章，已获得 \(progress.earned) 枚，本轮已集 \(count)/8 种月相")
        .accessibilityHint("查看本轮月相和点亮规则")
        .sheet(isPresented: $showInfo) {
            MoonBadgeInfoSheet(progress: progress, today: today)
        }
    }
}

/// 八种月相排成一行：集到的亮、下面带一个小圆点，没集到的暗
struct MoonPhaseStrip: View {
    let collected: Set<MoonPhaseName>
    var size: CGFloat = 28

    @Environment(\.palette) private var palette

    var body: some View {
        HStack(spacing: 0) {
            ForEach(MoonPhaseName.allCases, id: \.self) { name in
                let got = collected.contains(name)
                VStack(spacing: 5) {
                    MoonPhaseView(name, size: size, glow: false, isLit: got)
                    // 新月本来就是暗的，靠圆点区分有没有集到
                    Circle()
                        .fill(got ? AnyShapeStyle(palette.accentGradient) : AnyShapeStyle(Color.primary.opacity(0.12)))
                        .frame(width: 5, height: 5)
                }
                .frame(maxWidth: .infinity)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Self.spokenText(collected))
    }

    static func spokenText(_ collected: Set<MoonPhaseName>) -> String {
        let got = MoonPhaseName.allCases.filter { collected.contains($0) }.map(\.name)
        let missing = MoonPhaseName.allCases.filter { !collected.contains($0) }.map(\.name)
        if got.isEmpty { return "本轮还没有集到月相" }
        if missing.isEmpty { return "本轮八种月相都已集齐" }
        return "已集到\(got.joined(separator: "、"))；还差\(missing.joined(separator: "、"))"
    }
}

private struct MoonBadgeInfoSheet: View {
    let progress: MoonBadgeProgress
    let today: DayKey

    @Environment(\.dismiss) private var dismiss
    @Environment(AppSettings.self) private var settings
    @Environment(\.palette) private var palette

    var body: some View {
        let lunation = MoonBadges.lunation(containing: today)
        let lastDay = lunation.upperBound.adding(days: -1)
        let complete = progress.collected.count == MoonPhaseName.allCases.count
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    VStack(spacing: 10) {
                        MoonPhaseView(phase: 0.5, size: 96, isLit: progress.earned > 0)
                        Text(progress.earned > 0 ? "已获得 \(progress.earned) 枚满月徽章" : "还没有满月徽章")
                            .font(.title3.bold())
                        Text("本轮已集 \(progress.collected.count)/8 种月相")
                            .font(.subheadline)
                            .subtleForeground()
                    }
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: .infinity)
                    .glassCard()
                    .accessibilityElement(children: .combine)

                    VStack(alignment: .leading, spacing: 14) {
                        SectionHeader(title: "本轮月相",
                                      subtitle: "\(DateText.monthDay(lunation.lowerBound)) – \(DateText.monthDay(lastDay))",
                                      symbol: "moonphase.waxing.crescent")
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 14) {
                            ForEach(MoonPhaseName.allCases, id: \.self) { name in
                                let got = progress.collected.contains(name)
                                VStack(spacing: 6) {
                                    MoonPhaseView(name, size: 40, glow: false, isLit: got)
                                    Text(name.name)
                                        .font(.caption)
                                        .lineLimit(1)
                                        .minimumScaleFactor(0.7)
                                    Image(systemName: got ? "checkmark.circle.fill" : "circle.dashed")
                                        .font(.caption)
                                        .foregroundStyle(got ? AnyShapeStyle(palette.accent) : AnyShapeStyle(HierarchicalShapeStyle.secondary))
                                }
                                .accessibilityElement(children: .ignore)
                                .accessibilityLabel("\(name.name)，\(got ? "已集到" : "还没集到")")
                            }
                        }
                        Text(complete
                             ? "本轮已经集齐，下一轮从 \(DateText.monthDay(lunation.upperBound)) 新月开始。"
                             : "本轮还剩 \(today.days(to: lunation.upperBound)) 天，\(DateText.monthDay(lunation.upperBound)) 新月开始下一轮。")
                            .font(.footnote)
                            .subtleForeground()
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    .glassCard()

                    VStack(alignment: .leading, spacing: 10) {
                        SectionHeader(title: "怎么点亮", symbol: "sparkles")
                        Text("每天走完「每日仪式」，就点亮那天晚上的月相；跳过的日子不算。")
                        Text("从一次新月到下一次新月算一轮，大约 29.5 天，新月当天算新一轮的第一天。一轮里集齐新月、娥眉月、上弦月、盈凸月、满月、亏凸月、下弦月、残月八种月相，就得到一枚满月徽章。")
                        Text("每种月相会持续三四天，不用天天打开；新月只在一轮的开头和结尾出现，这几天记得来看看。")
                    }
                    .font(.subheadline)
                    .fixedSize(horizontal: false, vertical: true)
                    .glassCard()

                    if !settings.dailyRitualEnabled {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("每日仪式已关闭，月相暂时不会点亮。")
                                .font(.subheadline)
                            Button("打开每日仪式", systemImage: "moon.circle.fill") {
                                settings.dailyRitualEnabled = true
                                Haptics.success()
                            }
                            .buttonStyle(.glass)
                        }
                        .glassCard()
                    }
                }
                .pageContent()
                .padding(.vertical, 12)
            }
            .cosmicBackground(dimmed: true)
            .navigationTitle("月相徽章")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭", systemImage: "xmark") { dismiss() }
                }
            }
        }
    }
}
