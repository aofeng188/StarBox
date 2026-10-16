import Foundation

struct BandedTexts: Decodable, Sendable {
    let high: [String]
    let mid: [String]
    let low: [String]

    func texts(for score: Int) -> [String] {
        score >= 78 ? high : (score >= 55 ? mid : low)
    }
}

struct FortuneContent: Decodable, Sendable {
    struct Level: Decodable, Sendable {
        let min: Int
        let name: String
        let summaries: [String]
    }

    let levels: [Level]
    let categories: [String: BandedTexts]
    let relations: [String: String]
    let branchNotes: [String: String]
    let moonNotes: [String: String]
    let funYi: [String]
    let funJi: [String]
    let quotes: [String]
    let items: [String]
    /// 季节性幸运物可以出现的公历月份；不在表里的条目全年可选
    let itemMonths: [String: [Int]]?

    /// 某个公历月份可选的幸运物，保持 items 原有顺序
    func items(inMonth month: Int) -> [String] {
        guard let itemMonths else { return items }
        let available = items.filter { itemMonths[$0]?.contains(month) ?? true }
        return available.isEmpty ? items : available
    }
}

struct HoroscopeContent: Decodable, Sendable {
    let overview: BandedTexts
    let love: BandedTexts
    let career: BandedTexts
    let wealth: BandedTexts
    let health: BandedTexts
    let comments: [String]
}

/// 打包在 App 内的离线文案库。
enum ContentLibrary {
    static let fortune: FortuneContent = load("fortune")
    static let horoscope: HoroscopeContent = load("horoscope")

    static func load<T: Decodable>(_ name: String, bundle: Bundle = .main) -> T {
        guard let url = bundle.url(forResource: name, withExtension: "json") else {
            fatalError("缺少资源文件 \(name).json")
        }
        do {
            return try JSONDecoder().decode(T.self, from: Data(contentsOf: url))
        } catch {
            fatalError("解析 \(name).json 失败：\(error)")
        }
    }
}
