import Foundation

/// 内置菜品
struct Dish: Decodable, Hashable, Identifiable {
    let name: String
    let emoji: String
    let meals: String
    let cuisine: String
    let tastes: [String]
    let price: Int

    var id: String { name }

    init(from decoder: Decoder) throws {
        var c = try decoder.unkeyedContainer()
        name = try c.decode(String.self)
        emoji = try c.decode(String.self)
        meals = try c.decode(String.self)
        cuisine = try c.decode(String.self)
        tastes = try c.decode([String].self)
        price = try c.decode(Int.self)
    }

    var priceText: String { String(repeating: "¥", count: price) }
    var detail: String { "\(cuisine) · \(tastes.joined(separator: "/")) · \(priceText)" }
}

enum MealTime: String, CaseIterable, Identifiable {
    case breakfast = "b", lunch = "l", dinner = "d", night = "n", any = "*"

    var id: String { rawValue }

    var name: String {
        switch self {
        case .breakfast: "早餐"
        case .lunch: "午餐"
        case .dinner: "晚餐"
        case .night: "夜宵"
        case .any: "不限"
        }
    }

    var symbol: String {
        switch self {
        case .breakfast: "sunrise.fill"
        case .lunch: "sun.max.fill"
        case .dinner: "sunset.fill"
        case .night: "moon.stars.fill"
        case .any: "clock"
        }
    }

    static var current: MealTime {
        switch Calendar.current.component(.hour, from: Date()) {
        case 5..<10: .breakfast
        case 10..<15: .lunch
        case 15..<21: .dinner
        default: .night
        }
    }
}

enum FoodLibrary {
    static let all: [Dish] = ContentLibrary.load("foods")

    static var cuisines: [String] {
        var seen: [String] = []
        for dish in all where !seen.contains(dish.cuisine) { seen.append(dish.cuisine) }
        return seen
    }

    static let tastes = ["辣", "清淡", "酸爽", "甜口", "咸鲜", "浓郁"]

    static func filter(meal: MealTime, cuisines: Set<String>, tastes: Set<String>, maxPrice: Int) -> [Dish] {
        all.filter { dish in
            (meal == .any || dish.meals.contains(meal.rawValue))
                && (cuisines.isEmpty || cuisines.contains(dish.cuisine))
                && (tastes.isEmpty || !tastes.isDisjoint(with: dish.tastes))
                && dish.price <= maxPrice
        }
    }

    /// 智能去重：最近吃过的降低权重
    static func weight(for name: String, logs: [FoodLog], smart: Bool) -> Double {
        guard smart else { return 1 }
        guard let last = logs.first(where: { $0.name == name }) else { return 1 }
        let days = DayKey(last.date).days(to: .today)
        switch days {
        case ..<1: return 0.05
        case 1..<3: return 0.2
        case 3..<7: return 0.5
        default: return 1
        }
    }
}
