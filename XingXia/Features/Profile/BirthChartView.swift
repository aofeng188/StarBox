import SwiftUI
import SwiftData
import Charts

/// 命盘：太阳/月亮/上升 + 四柱八字 + 五行分布
struct BirthChartView: View {
    @Environment(AppState.self) private var app
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]
    @State private var selectedID: UUID?
    @Environment(\.palette) private var palette

    var body: some View {
        let profile = profiles.first { $0.id == (selectedID ?? app.primary?.id) } ?? profiles.first
        ScrollView {
            if let profile {
                content(profile)
                    .pageContent()
                    .padding(.bottom, 30)
            } else {
                EmptyHint(symbol: "person.crop.circle.badge.questionmark", title: "还没有档案",
                          message: "在「我的 ▸ 档案管理」里添加生日后即可查看命盘")
                    .padding(.top, 80)
            }
        }
        .cosmicBackground()
        .navigationTitle("命盘")
        .toolbar {
            if profiles.count > 1 {
                ToolbarItem(placement: .topBarTrailing) {
                    Menu {
                        ForEach(profiles) { p in
                            Button("\(p.avatar) \(p.name)") { selectedID = p.id }
                        }
                    } label: {
                        Label("切换档案", systemImage: "person.2")
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func content(_ profile: Profile) -> some View {
        let birth = profile.birth
        let bazi = BaZi(birth)
        let sun = ZodiacSign.sunSign(of: birth)
        let moon = ZodiacSign.moonSign(at: birth.moment)
        let rising = ZodiacSign.ascendant(of: birth)
        let lunar = LunarDate(birth.dayKey)

        VStack(spacing: 16) {
            VStack(spacing: 6) {
                Text(profile.avatar).font(.system(size: 54))
                Text(profile.name).font(.title2.bold())
                Text("公历 \(DateText.full(birth.dayKey))\(profile.hasBirthTime ? String(format: " %02d:%02d", profile.birthHour, profile.birthMinute) : "")")
                    .font(.subheadline).foregroundStyle(.secondary)
                Text("农历 \(lunar.yearGanZhi.name)年 \(lunar.text)")
                    .font(.subheadline).foregroundStyle(.secondary)
            }
            .padding(.top, 8)

            // 三巨头
            VStack(alignment: .leading, spacing: 14) {
                SectionHeader(title: "星盘三巨头", subtitle: "太阳 · 月亮 · 上升", symbol: "sparkles")
                HStack(spacing: 10) {
                    BigThree(title: "太阳星座", subtitle: "外在性格", sign: sun)
                    BigThree(title: "月亮星座", subtitle: "内在情绪", sign: moon)
                    if let rising {
                        BigThree(title: "上升星座", subtitle: "第一印象", sign: rising)
                    } else {
                        VStack(spacing: 8) {
                            Image(systemName: "questionmark.circle").font(.system(size: 34)).foregroundStyle(.secondary)
                            Text("上升星座").font(.caption.weight(.semibold))
                            Text("需要出生时间").font(.caption2).foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                    }
                }
                Text(sun.profile)
                    .font(.subheadline)
                    .foregroundStyle(.primary.opacity(0.85))
                    .lineSpacing(4)
            }
            .glassCard()

            // 四柱
            VStack(alignment: .leading, spacing: 14) {
                SectionHeader(title: "四柱八字", subtitle: bazi.trueSolarApplied ? "已按真太阳时校正" : "未填出生时间，仅排三柱", symbol: "square.grid.3x3.fill")
                HStack(spacing: 8) {
                    PillarView(title: "年柱", pillar: bazi.year)
                    PillarView(title: "月柱", pillar: bazi.month)
                    PillarView(title: "日柱", pillar: bazi.day, highlight: true)
                    if let hour = bazi.hour {
                        PillarView(title: "时柱", pillar: hour)
                    } else {
                        VStack(spacing: 6) {
                            Text("时柱").font(.caption).foregroundStyle(.secondary)
                            Text("?").font(AppFont.serif(30, weight: .bold)).foregroundStyle(.tertiary)
                            Text("?").font(AppFont.serif(30, weight: .bold)).foregroundStyle(.tertiary)
                            Text("待补充").font(.caption2).foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(.primary.opacity(0.04), in: .rect(cornerRadius: 16))
                    }
                }
                let master = bazi.dayMaster
                Text("日主 \(master.name)\(master.element.name)：\(Self.masterTexts[master.index])")
                    .font(.subheadline)
                    .lineSpacing(4)
            }
            .glassCard()

            // 五行
            VStack(alignment: .leading, spacing: 14) {
                SectionHeader(title: "五行分布", symbol: "chart.bar.fill")
                let counts = bazi.elementCounts
                Chart(WuXing.allCases, id: \.self) { element in
                    BarMark(x: .value("五行", element.name), y: .value("个数", counts[element] ?? 0))
                        .foregroundStyle(element.color.gradient)
                        .cornerRadius(6)
                        .annotation(position: .top) {
                            Text("\(counts[element] ?? 0)").font(.caption.bold()).foregroundStyle(palette.ink(element.color))
                        }
                }
                .chartYAxis(.hidden)
                .frame(height: 150)

                let favorable = bazi.favorableElement
                let weak = bazi.weakestElements.map(\.name).joined(separator: "、")
                VStack(alignment: .leading, spacing: 8) {
                    Text("五行最旺为「\(bazi.strongestElement.name)」，相对偏弱为「\(weak)」。")
                    Text("喜用参考「\(favorable.name)」：幸运色 \(favorable.luckyColors.prefix(2).map(\.name).joined(separator: "、"))；方位 \(favorable.direction)；数字 \(favorable.numbers.map(String.init).joined(separator: "、"))。")
                }
                .font(.subheadline)
                .foregroundStyle(.primary.opacity(0.85))
                Text("以上为简化的娱乐性推算，仅供参考。")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .glassCard()
        }
    }

    static let masterTexts = [
        "如参天大树，正直向上，有担当与领导力。",
        "如花草藤蔓，柔韧灵活，善于适应环境。",
        "如太阳之火，热情开朗，光明磊落。",
        "如烛火星光，细腻温暖，富有洞察力。",
        "如高山厚土，稳重可靠，包容力强。",
        "如田园沃土，务实细心，善于经营。",
        "如刀剑之金，果断刚毅，重情重义。",
        "如珠玉之金，精致敏锐，追求完美。",
        "如江河之水，聪慧奔放，富有谋略。",
        "如雨露之水，温柔内敛，直觉敏锐。",
    ]
}

private struct BigThree: View {
    let title: String
    let subtitle: String
    let sign: ZodiacSign

    var body: some View {
        VStack(spacing: 8) {
            ZodiacBadge(sign: sign, size: 54)
            Text(sign.name).font(.subheadline.weight(.semibold))
            Text(title).font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title) \(sign.name)，\(subtitle)")
    }
}

private struct PillarView: View {
    let title: String
    let pillar: GanZhi
    var highlight = false
    @Environment(\.palette) private var palette

    var body: some View {
        VStack(spacing: 6) {
            Text(title).font(.caption).foregroundStyle(.secondary)
            // 五行色是 400 级，浅色模式下直接当文字太淡，用 ink 版本
            Text(pillar.stem.name)
                .font(AppFont.serif(30, weight: .bold))
                .foregroundStyle(palette.ink(pillar.stem.element.color))
            Text(pillar.branch.name)
                .font(AppFont.serif(30, weight: .bold))
                .foregroundStyle(palette.ink(pillar.branch.element.color))
            Text(pillar.nayin).font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(highlight ? palette.accent.opacity(0.14) : .primary.opacity(0.04), in: .rect(cornerRadius: 16))
        .overlay {
            if highlight { RoundedRectangle(cornerRadius: 16).strokeBorder(palette.accent.opacity(0.5), lineWidth: 1) }
        }
        .accessibilityElement(children: .combine)
    }
}

// MARK: - 缘分配对

struct CompatibilityView: View {
    @Query(sort: \Profile.sortIndex) private var profiles: [Profile]
    @State private var left: PairSide = .sign(.aries)
    @State private var right: PairSide = .sign(.leo)
    @State private var initialized = false
    @Environment(\.palette) private var palette

    enum PairSide: Hashable {
        case profile(UUID)
        case sign(ZodiacSign)
    }

    var body: some View {
        let a = resolve(left)
        let b = resolve(right)
        let animals: (EarthlyBranch, EarthlyBranch)? = (a.animal != nil && b.animal != nil) ? (a.animal!, b.animal!) : nil
        let result = Compatibility.between(a.sign, b.sign, animals: animals)

        ScrollView {
            VStack(spacing: 18) {
                HStack(alignment: .center, spacing: 0) {
                    sidePicker($left, info: a)
                    Image(systemName: "heart.fill")
                        .font(.title)
                        .foregroundStyle(palette.accentGradient)
                        .symbolEffect(.pulse)
                        .frame(width: 50)
                    sidePicker($right, info: b)
                }
                .glassCard()

                VStack(spacing: 14) {
                    ScoreRing(score: result.score, size: 150, lineWidth: 14, label: "契合度")
                        .id("\(a.sign.rawValue)-\(b.sign.rawValue)-\(result.score)")
                    GradientText(text: result.title, font: .title.bold())
                    Text("\(a.sign.name) × \(b.sign.name) · \(result.aspect.name)")
                        .font(.subheadline).foregroundStyle(.secondary)
                    Text(result.summary).font(.body).lineSpacing(4)
                    Text(result.elementNote).font(.subheadline).foregroundStyle(.primary.opacity(0.85))
                    if let note = result.animalNote {
                        Text(note).font(.subheadline).foregroundStyle(.primary.opacity(0.85))
                    }
                }
                .frame(maxWidth: .infinity)
                .glassCard()

                VStack(alignment: .leading, spacing: 10) {
                    SectionHeader(title: "相处小建议", symbol: "lightbulb.fill")
                    ForEach(result.tips, id: \.self) { tip in
                        Label(tip, systemImage: "sparkle").font(.subheadline)
                    }
                }
                .glassCard()
            }
            .pageContent()
            .padding(.bottom, 30)
        }
        .cosmicBackground()
        .navigationTitle("缘分配对")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                ShareCardButton(title: "分享配对", previewTitle: "\(a.title) × \(b.title) 缘分配对") { style in
                    ShareCardFrame(style: style, title: "星匣 · 缘分配对") {
                        CompatibilityShareContent(left: a, right: b, result: result, style: style)
                    }
                }
            }
        }
        .onAppear {
            guard !initialized else { return }
            initialized = true
            if let first = profiles.first { left = .profile(first.id) }
            if profiles.count > 1 { right = .profile(profiles[1].id) }
        }
    }

    private func resolve(_ side: PairSide) -> (sign: ZodiacSign, animal: EarthlyBranch?, title: String, avatar: String) {
        switch side {
        case .profile(let id):
            if let profile = profiles.first(where: { $0.id == id }) {
                return (profile.sunSign, profile.animal, profile.name, profile.avatar)
            }
            return (.aries, nil, "白羊座", "")
        case .sign(let sign):
            return (sign, nil, sign.name, "")
        }
    }

    private func sidePicker(_ side: Binding<PairSide>, info: (sign: ZodiacSign, animal: EarthlyBranch?, title: String, avatar: String)) -> some View {
        Menu {
            if !profiles.isEmpty {
                Section("档案") {
                    ForEach(profiles) { p in Button("\(p.avatar) \(p.name)") { side.wrappedValue = .profile(p.id) } }
                }
            }
            Section("星座") {
                ForEach(ZodiacSign.allCases) { s in Button("\(s.glyph) \(s.name)") { side.wrappedValue = .sign(s) } }
            }
        } label: {
            VStack(spacing: 8) {
                if info.avatar.isEmpty {
                    ZodiacBadge(sign: info.sign, size: 64)
                } else {
                    Text(info.avatar).font(.system(size: 40)).frame(width: 64, height: 64)
                        .background(Circle().fill(palette.accent.opacity(0.2)))
                }
                Text(info.title).font(.headline).foregroundStyle(.primary)
                Text(info.animal.map { "\(info.sign.name) · 属\($0.animal)" } ?? info.sign.dateRange)
                    .font(.caption).foregroundStyle(.secondary)
                Label("更换", systemImage: "chevron.up.chevron.down").font(.caption2).foregroundStyle(.tint)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

/// 缘分配对分享卡的内容：两人的头像和名字、星座和生肖、契合度和一句话结论
private struct CompatibilityShareContent: View {
    typealias Side = (sign: ZodiacSign, animal: EarthlyBranch?, title: String, avatar: String)

    let left: Side
    let right: Side
    let result: Compatibility
    let style: ShareCardStyle

    var body: some View {
        let tall = style.isTall
        VStack(spacing: tall ? 18 : 14) {
            HStack(alignment: .top, spacing: 0) {
                person(left, tall: tall)
                Image(systemName: "heart.fill")
                    .font(.system(size: tall ? 26 : 22))
                    .foregroundStyle(style.palette.accentGradient)
                    .shadow(color: style.palette.accent.opacity(0.6), radius: 8)
                    .frame(width: 44)
                    .padding(.top, tall ? 24 : 20)
                    .accessibilityHidden(true)
                person(right, tall: tall)
            }

            if tall {
                VStack(spacing: 10) {
                    ShareScoreRing(score: result.score, size: 130, lineWidth: 12, label: "契合度")
                    GradientText(text: result.title, font: .system(size: 32, weight: .heavy, design: .rounded))
                    Text("\(left.sign.name) × \(right.sign.name) · \(result.aspect.name)")
                        .font(.system(size: 12))
                        .foregroundStyle(.white.opacity(0.65))
                }
            } else {
                HStack(spacing: 18) {
                    ShareScoreRing(score: result.score, size: 96, lineWidth: 10, label: "契合度")
                    VStack(alignment: .leading, spacing: 6) {
                        GradientText(text: result.title, font: .system(size: 28, weight: .heavy, design: .rounded))
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                        Text("\(left.sign.name) × \(right.sign.name)")
                            .font(.system(size: 12))
                            .foregroundStyle(.white.opacity(0.65))
                        Text(result.aspect.name)
                            .font(.system(size: 12))
                            .foregroundStyle(.white.opacity(0.65))
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
            }

            Text(Self.firstSentence(result.summary))
                .font(.system(size: tall ? 16 : 14))
                .lineSpacing(4)
                .multilineTextAlignment(.center)
                .foregroundStyle(.white.opacity(0.92))
                .lineLimit(tall ? 4 : 3)
                .minimumScaleFactor(0.85)

            // 9:16 再加一句生肖或元素的说明
            if tall {
                Text(result.animalNote ?? result.elementNote)
                    .font(.system(size: 13))
                    .lineSpacing(3)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.white.opacity(0.7))
                    .lineLimit(2)
                    .minimumScaleFactor(0.85)
            }
        }
    }

    private func person(_ side: Side, tall: Bool) -> some View {
        let size: CGFloat = tall ? 72 : 60
        return VStack(spacing: 6) {
            // 头像是装饰，读屏读名字和星座
            if side.avatar.isEmpty {
                ZodiacBadge(sign: side.sign, size: size)
                    .accessibilityHidden(true)
            } else {
                Text(side.avatar)
                    .font(.system(size: size * 0.62))
                    .frame(width: size, height: size)
                    .background(Circle().fill(style.palette.accent.opacity(0.22)))
                    .overlay(Circle().strokeBorder(style.palette.accent.opacity(0.5), lineWidth: 1))
                    .accessibilityHidden(true)
            }
            Text(side.title)
                .font(.system(size: tall ? 18 : 16, weight: .bold))
                .foregroundStyle(.white)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            Text(side.animal.map { "\(side.sign.name) · 属\($0.animal)" } ?? side.sign.dateRange)
                .font(.system(size: 11))
                .foregroundStyle(.white.opacity(0.65))
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .combine)
    }

    /// 取摘要的第一句当结论
    private static func firstSentence(_ text: String) -> String {
        guard let end = text.firstIndex(where: { "。！？".contains($0) }) else { return text }
        return String(text[...end])
    }
}
