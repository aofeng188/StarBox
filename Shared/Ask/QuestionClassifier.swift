import Foundation

/// 「问一问」判断出的问题类型
enum QuestionKind: String, Sendable {
    case yesNo, choice, food, timing, relationship, general
}

/// 推荐的方法。App 端负责映射到具体页面
enum AskMethod: String, Sendable {
    case coin, answers, tarotSingle, tarotTime, tarotLove, wheel, food, finger, stick, auspicious, compatibility, dice
}

struct QuestionAnalysis: Sendable {
    let kind: QuestionKind
    /// 「A 还是 B」拆出的选项，最多 8 个；只有 choice、food 两类带选项，且至少 2 个，否则为空
    let options: [String]
    /// 推荐顺序，固定 3 个
    let methods: [AskMethod]
}

/// 离线关键词规则，按顺序判断，先命中先用。Android core 的 QuestionClassifier 与这里逐条一致：
///
/// 0. 问题先去掉首尾空白。拆选项：把「还是」「或者」当分隔，再按「、」「,」「，」切开；
///    每段去掉两端的空白（含全角空格）和「？?！!。.…～~」，丢掉空段和重复段（保留先出现的），最多 8 段。
/// 1. food：含「吃|喝|外卖|饭|菜」，且是选择——满足第 2 条的条件，或（去掉「什么时候」后）含「什么」「啥」。
/// 2. choice：含「还是|或者|选哪|哪个|哪一个|选谁|去哪」；或拆出 ≥ 2 段，且不含第 3、4、5 条的关键词
///    （只靠标点拆出来的两段是弱信号：「下雨了，要不要去跑步」仍按是非题算）。
/// 3. timing：含「哪天|什么时候|何时|日子|时机|吉日|搬家|开业|结婚」。
/// 4. relationship：含「喜欢|感情|恋爱|复合|表白|对象|他|她|缘分」。
/// 5. yesNo：含「要不要|能不能|该不该|可不可以|是否|会不会|行不行|好不好」，或以「吗」「吗？」「吗?」结尾。
/// 6. general：其余。
///
/// 推荐方法（2 段选项时用硬币，更多时用骰子）：
/// - food：有选项 → 转盘、今天吃什么、硬币/骰子；没有 → 今天吃什么、转盘、指尖轮盘
/// - choice：有选项 → 转盘、硬币/骰子、指尖轮盘；没有 → 转盘、指尖轮盘、骰子
/// - timing：求签、择吉日、塔罗时间之流
/// - relationship：塔罗情感关系、缘分配对、答案之书
/// - yesNo：硬币、答案之书、塔罗单张
/// - general：答案之书、求签、硬币
enum QuestionClassifier {
    static let maxOptions = 8

    static let foodWords = ["吃", "喝", "外卖", "饭", "菜"]
    static let choiceWords = ["还是", "或者", "选哪", "哪个", "哪一个", "选谁", "去哪"]
    static let timingWords = ["哪天", "什么时候", "何时", "日子", "时机", "吉日", "搬家", "开业", "结婚"]
    static let relationshipWords = ["喜欢", "感情", "恋爱", "复合", "表白", "对象", "他", "她", "缘分"]
    static let yesNoWords = ["要不要", "能不能", "该不该", "可不可以", "是否", "会不会", "行不行", "好不好"]
    static let yesNoEndings = ["吗", "吗？", "吗?"]
    static let whatWords = ["什么", "啥"]

    /// 拆选项：这两个词先换成「、」，再按标点切
    static let separatorWords = ["还是", "或者"]
    static let separatorMarks: Set<Character> = ["、", ",", "，"]
    /// 选项两端去掉的字符
    static let optionTrim = CharacterSet(charactersIn: " \t\r\n\u{3000}？?！!。.…～~")

    static func analyze(_ text: String) -> QuestionAnalysis {
        let question = text.trimmingCharacters(in: .whitespacesAndNewlines)
        let split = splitOptions(question)

        let isTiming = containsAny(question, timingWords)
        let isRelationship = containsAny(question, relationshipWords)
        let isYesNo = containsAny(question, yesNoWords) || yesNoEndings.contains { question.hasSuffix($0) }
        let isChoice = containsAny(question, choiceWords)
            || (split.count >= 2 && !isTiming && !isRelationship && !isYesNo)
        let asksWhat = containsAny(question.replacingOccurrences(of: "什么时候", with: ""), whatWords)
        let options = split.count >= 2 ? split : []

        if containsAny(question, foodWords) && (isChoice || asksWhat) {
            let methods: [AskMethod] = options.isEmpty ? [.food, .wheel, .finger]
                                                       : [.wheel, .food, options.count == 2 ? .coin : .dice]
            return QuestionAnalysis(kind: .food, options: options, methods: methods)
        }
        if isChoice {
            let methods: [AskMethod] = options.isEmpty ? [.wheel, .finger, .dice]
                                                       : [.wheel, options.count == 2 ? .coin : .dice, .finger]
            return QuestionAnalysis(kind: .choice, options: options, methods: methods)
        }
        if isTiming {
            return QuestionAnalysis(kind: .timing, options: [], methods: [.stick, .auspicious, .tarotTime])
        }
        if isRelationship {
            return QuestionAnalysis(kind: .relationship, options: [], methods: [.tarotLove, .compatibility, .answers])
        }
        if isYesNo {
            return QuestionAnalysis(kind: .yesNo, options: [], methods: [.coin, .answers, .tarotSingle])
        }
        return QuestionAnalysis(kind: .general, options: [], methods: [.answers, .stick, .coin])
    }

    /// 按「还是 / 或者 / 、 / , / ，」拆出的选项（未判断类型，可能只有 0 或 1 段）
    static func splitOptions(_ text: String) -> [String] {
        var unified = text
        for word in separatorWords {
            unified = unified.replacingOccurrences(of: word, with: "、")
        }
        var result: [String] = []
        for part in unified.split(whereSeparator: { separatorMarks.contains($0) }) {
            let option = String(part).trimmingCharacters(in: optionTrim)
            guard !option.isEmpty, !result.contains(option) else { continue }
            result.append(option)
            if result.count == maxOptions { break }
        }
        return result
    }

    private static func containsAny(_ text: String, _ words: [String]) -> Bool {
        words.contains { text.contains($0) }
    }
}
