import Foundation

/// 星座 + 生肖配对
struct Compatibility: Sendable {
    let score: Int
    let title: String
    let summary: String
    let aspect: AspectKind
    let elementNote: String
    let animalNote: String?
    let tips: [String]

    static func between(_ a: ZodiacSign, _ b: ZodiacSign, animals: (EarthlyBranch, EarthlyBranch)? = nil) -> Compatibility {
        let aspect = AspectKind(distance: a.distance(to: b))
        var (score, title, summary) = aspectInfo(aspect)
        let elementNote = elementInfo(a.element, b.element)

        var animalNote: String?
        if let (x, y) = animals {
            let relation = x.relation(to: y)
            score += relation.score
            animalNote = switch relation {
            case .sixHarmony: "生肖\(x.animal)与\(y.animal)六合，传统上被视为天生一对。"
            case .threeHarmony: "生肖\(x.animal)与\(y.animal)三合，志同道合，合作愉快。"
            case .clash: "生肖\(x.animal)与\(y.animal)相冲，个性都很鲜明，需要更多包容。"
            case .harm: "生肖\(x.animal)与\(y.animal)相害，沟通时多一分耐心。"
            case .same: "同属\(x.animal)，相似的成长背景让你们更容易理解彼此。"
            case .neutral: "生肖\(x.animal)与\(y.animal)关系平和，相处没有明显阻碍。"
            }
        }
        var rng = SeededGenerator("compat", min(a.rawValue, b.rawValue), max(a.rawValue, b.rawValue))
        score += rng.int(-2...2)
        let tips = rng.pick(tipPool, count: 3)
        return Compatibility(score: min(99, max(40, score)), title: title, summary: summary,
                             aspect: aspect, elementNote: elementNote, animalNote: animalNote, tips: tips)
    }

    private static func aspectInfo(_ aspect: AspectKind) -> (Int, String, String) {
        switch aspect {
        case .conjunction:
            (80, "同频共振", "同一个星座的你们像在照镜子，天生懂得对方的想法与情绪。默契来得很快，但也容易放大相同的小缺点，记得互相提醒、互相欣赏。")
        case .semiSextile:
            (63, "相邻互补", "相邻星座性格差异明显，却又对彼此充满好奇。一个人的短板恰好是另一个人的长处，多一点耐心去适应对方的节奏。")
        case .sextile:
            (85, "志趣相投", "元素相生、沟通顺畅，你们像老朋友一样轻松自在，聊得来也玩得来，是让人非常舒服的组合。")
        case .square:
            (56, "火花四溅", "行事风格和价值观差异较大，偶尔会擦出摩擦的火花。但正是这种张力推动彼此成长，学会换位思考就能化阻力为动力。")
        case .trine:
            (93, "天作之合", "同属一个元素，三观契合、步调一致，相处起来自然而然，是十二星座中最和谐的组合之一。")
        case .quincunx:
            (61, "需要磨合", "你们关注的重点不太一样，需要不断调整彼此的期待。磨合好了，会成为互相成就的伙伴。")
        case .opposition:
            (75, "互相吸引", "对宫星座就像镜子的两面，对方身上有你缺少的特质，吸引力极强。在拉扯中找到平衡，会是很迷人的关系。")
        }
    }

    private static func elementInfo(_ a: ZodiacElement, _ b: ZodiacElement) -> String {
        let pair = Set([a, b])
        if a == b { return "同为\(a.name)星座，天然的默契让你们很容易理解对方。" }
        if pair == [.fire, .air] { return "火象遇上风象：火借风势，越烧越旺，彼此激发热情与灵感。" }
        if pair == [.earth, .water] { return "土象遇上水象：水润土沃，彼此滋养，是安全感满满的组合。" }
        if pair == [.fire, .water] { return "火象遇上水象：冰火两重天，一个热烈一个细腻，需要相互体谅。" }
        if pair == [.earth, .air] { return "土象遇上风象：一个务实一个理想，需要在现实与想法之间找到平衡。" }
        if pair == [.fire, .earth] { return "火象遇上土象：热情遇上稳重，一个想冲一个想稳，需要磨合节奏。" }
        return "风象遇上水象：理性与感性的碰撞，多倾听对方的感受会更加融洽。"
    }

    private static let tipPool = [
        "每周留一段只属于你们的时间。",
        "遇到分歧时先说感受，再说道理。",
        "记住对方在意的小细节。",
        "一起完成一件小目标，会增加默契。",
        "给彼此保留一些独处的空间。",
        "真诚地夸奖对方，比什么都有用。",
        "争吵不过夜，有话好好说。",
        "一起尝试一件新鲜事。",
        "多问一句「你今天过得怎么样」。",
        "尊重彼此的朋友圈和兴趣爱好。",
        "用对方喜欢的方式表达爱。",
        "偶尔制造一点小惊喜。",
    ]
}
