import SwiftData
import Foundation

/// 首次启动时放入一些示例数据，让各个工具开箱即用。
@MainActor
enum SeedData {
    private static let key = "seedVersion"
    private static let version = 1

    static func insertIfNeeded(into context: ModelContext) {
        let defaults = UserDefaults.standard
        guard defaults.integer(forKey: key) < version else { return }
        defaults.set(version, forKey: key)

        let wheels: [(String, [String])] = [
            ("今晚谁洗碗", ["我", "你", "石头剪刀布决定", "一起洗", "明天再洗"]),
            ("周末去哪儿", ["看电影", "逛公园", "爬山", "宅家追剧", "探店", "博物馆", "去海边"]),
            ("要不要做", ["做！", "不做", "再想想", "问问朋友"]),
            ("奶茶选哪杯", ["珍珠奶茶", "杨枝甘露", "柠檬茶", "芋泥波波", "椰椰拿铁", "葡萄冰"]),
        ]
        for (name, options) in wheels {
            let template = WheelTemplate(name: name)
            context.insert(template)
            for (i, title) in options.enumerated() {
                let option = WheelOption(title: title, sortIndex: i)
                option.template = template
                context.insert(option)
            }
        }

        let pool = LotteryPool(name: "年会抽奖", icon: "🎊", removeAfterDraw: true)
        context.insert(pool)
        for (i, prize) in [("特等奖 · 新款手机", 1.0, 1), ("一等奖 · 无线耳机", 1, 2), ("二等奖 · 购物卡", 1, 5), ("三等奖 · 奶茶券", 1, 10), ("参与奖 · 幸运红包", 1, 20)].enumerated() {
            let item = LotteryItem(name: prize.0, weight: prize.1, quantity: prize.2, sortIndex: i)
            item.pool = pool
            context.insert(item)
        }
        let daily = LotteryPool(name: "今日小奖励", icon: "🎁", removeAfterDraw: false)
        context.insert(daily)
        for (i, prize) in ["一杯奶茶", "看一集剧", "早睡一小时", "买束花", "散步半小时", "吃顿好的", "谢谢惠顾"].enumerated() {
            let item = LotteryItem(name: prize, weight: prize == "谢谢惠顾" ? 2 : 1, quantity: 99, sortIndex: i)
            item.pool = daily
            context.insert(item)
        }

        let menu = FoodMenu(name: "公司楼下", icon: "🏢")
        context.insert(menu)
        for (i, name) in ["兰州拉面", "黄焖鸡米饭", "麻辣烫", "沙拉轻食", "日式便当", "螺蛳粉"].enumerated() {
            let item = FoodMenuItem(name: name, sortIndex: i)
            item.menu = menu
            context.insert(item)
        }

        context.insert(ScratchPreset(name: "今日惊喜", prizes: ["奶茶一杯 🧋", "免做家务一次 🧹", "电影之夜 🎬", "抱抱一下 🤗", "零食大礼包 🍫"], blankWeight: 1))

        // 节日倒数
        let today = DayKey.today
        let newYear = Countdown(title: "元旦", emoji: "🎆", date: DayKey(year: today.year + 1, month: 1, day: 1).date, kind: CountdownKind.countdown.rawValue)
        newYear.repeatsYearly = true
        newYear.colorIndex = 0
        context.insert(newYear)
        let spring = Countdown(title: "春节", emoji: "🧧", date: today.date, kind: CountdownKind.countdown.rawValue)
        spring.isLunar = true
        spring.lunarMonth = 1
        spring.lunarDay = 1
        spring.repeatsYearly = true
        spring.pinned = true
        spring.colorIndex = 1
        context.insert(spring)

        try? context.save()
    }
}
