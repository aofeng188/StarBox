import Foundation

/// 出生城市：用于计算上升星座和真太阳时。
struct BirthCity: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let region: String
    let latitude: Double
    let longitude: Double
    let timeZoneID: String

    var timeZone: TimeZone { TimeZone(identifier: timeZoneID) ?? .current }

    static let beijing = BirthCity(id: "beijing", name: "北京", region: "华北", latitude: 39.90, longitude: 116.40, timeZoneID: "Asia/Shanghai")

    static func find(_ id: String?) -> BirthCity? {
        guard let id else { return nil }
        return all.first { $0.id == id }
    }

    static let all: [BirthCity] = [
        beijing,
        .init(id: "tianjin", name: "天津", region: "华北", latitude: 39.13, longitude: 117.20, timeZoneID: "Asia/Shanghai"),
        .init(id: "shijiazhuang", name: "石家庄", region: "华北", latitude: 38.04, longitude: 114.51, timeZoneID: "Asia/Shanghai"),
        .init(id: "taiyuan", name: "太原", region: "华北", latitude: 37.87, longitude: 112.55, timeZoneID: "Asia/Shanghai"),
        .init(id: "hohhot", name: "呼和浩特", region: "华北", latitude: 40.84, longitude: 111.75, timeZoneID: "Asia/Shanghai"),
        .init(id: "shanghai", name: "上海", region: "华东", latitude: 31.23, longitude: 121.47, timeZoneID: "Asia/Shanghai"),
        .init(id: "nanjing", name: "南京", region: "华东", latitude: 32.06, longitude: 118.80, timeZoneID: "Asia/Shanghai"),
        .init(id: "suzhou", name: "苏州", region: "华东", latitude: 31.30, longitude: 120.59, timeZoneID: "Asia/Shanghai"),
        .init(id: "wuxi", name: "无锡", region: "华东", latitude: 31.49, longitude: 120.31, timeZoneID: "Asia/Shanghai"),
        .init(id: "hangzhou", name: "杭州", region: "华东", latitude: 30.27, longitude: 120.16, timeZoneID: "Asia/Shanghai"),
        .init(id: "ningbo", name: "宁波", region: "华东", latitude: 29.87, longitude: 121.55, timeZoneID: "Asia/Shanghai"),
        .init(id: "hefei", name: "合肥", region: "华东", latitude: 31.82, longitude: 117.23, timeZoneID: "Asia/Shanghai"),
        .init(id: "fuzhou", name: "福州", region: "华东", latitude: 26.07, longitude: 119.30, timeZoneID: "Asia/Shanghai"),
        .init(id: "xiamen", name: "厦门", region: "华东", latitude: 24.48, longitude: 118.09, timeZoneID: "Asia/Shanghai"),
        .init(id: "nanchang", name: "南昌", region: "华东", latitude: 28.68, longitude: 115.86, timeZoneID: "Asia/Shanghai"),
        .init(id: "jinan", name: "济南", region: "华东", latitude: 36.65, longitude: 117.12, timeZoneID: "Asia/Shanghai"),
        .init(id: "qingdao", name: "青岛", region: "华东", latitude: 36.07, longitude: 120.38, timeZoneID: "Asia/Shanghai"),
        .init(id: "guangzhou", name: "广州", region: "华南", latitude: 23.13, longitude: 113.26, timeZoneID: "Asia/Shanghai"),
        .init(id: "shenzhen", name: "深圳", region: "华南", latitude: 22.54, longitude: 114.06, timeZoneID: "Asia/Shanghai"),
        .init(id: "dongguan", name: "东莞", region: "华南", latitude: 23.02, longitude: 113.75, timeZoneID: "Asia/Shanghai"),
        .init(id: "foshan", name: "佛山", region: "华南", latitude: 23.02, longitude: 113.12, timeZoneID: "Asia/Shanghai"),
        .init(id: "nanning", name: "南宁", region: "华南", latitude: 22.82, longitude: 108.37, timeZoneID: "Asia/Shanghai"),
        .init(id: "haikou", name: "海口", region: "华南", latitude: 20.04, longitude: 110.20, timeZoneID: "Asia/Shanghai"),
        .init(id: "wuhan", name: "武汉", region: "华中", latitude: 30.59, longitude: 114.31, timeZoneID: "Asia/Shanghai"),
        .init(id: "changsha", name: "长沙", region: "华中", latitude: 28.23, longitude: 112.94, timeZoneID: "Asia/Shanghai"),
        .init(id: "zhengzhou", name: "郑州", region: "华中", latitude: 34.75, longitude: 113.63, timeZoneID: "Asia/Shanghai"),
        .init(id: "chongqing", name: "重庆", region: "西南", latitude: 29.56, longitude: 106.55, timeZoneID: "Asia/Shanghai"),
        .init(id: "chengdu", name: "成都", region: "西南", latitude: 30.57, longitude: 104.07, timeZoneID: "Asia/Shanghai"),
        .init(id: "kunming", name: "昆明", region: "西南", latitude: 25.04, longitude: 102.71, timeZoneID: "Asia/Shanghai"),
        .init(id: "guiyang", name: "贵阳", region: "西南", latitude: 26.65, longitude: 106.63, timeZoneID: "Asia/Shanghai"),
        .init(id: "lhasa", name: "拉萨", region: "西南", latitude: 29.65, longitude: 91.14, timeZoneID: "Asia/Shanghai"),
        .init(id: "xian", name: "西安", region: "西北", latitude: 34.34, longitude: 108.94, timeZoneID: "Asia/Shanghai"),
        .init(id: "lanzhou", name: "兰州", region: "西北", latitude: 36.06, longitude: 103.83, timeZoneID: "Asia/Shanghai"),
        .init(id: "xining", name: "西宁", region: "西北", latitude: 36.62, longitude: 101.78, timeZoneID: "Asia/Shanghai"),
        .init(id: "yinchuan", name: "银川", region: "西北", latitude: 38.49, longitude: 106.23, timeZoneID: "Asia/Shanghai"),
        .init(id: "urumqi", name: "乌鲁木齐", region: "西北", latitude: 43.83, longitude: 87.62, timeZoneID: "Asia/Shanghai"),
        .init(id: "shenyang", name: "沈阳", region: "东北", latitude: 41.81, longitude: 123.43, timeZoneID: "Asia/Shanghai"),
        .init(id: "dalian", name: "大连", region: "东北", latitude: 38.91, longitude: 121.61, timeZoneID: "Asia/Shanghai"),
        .init(id: "changchun", name: "长春", region: "东北", latitude: 43.82, longitude: 125.32, timeZoneID: "Asia/Shanghai"),
        .init(id: "harbin", name: "哈尔滨", region: "东北", latitude: 45.80, longitude: 126.53, timeZoneID: "Asia/Shanghai"),
        .init(id: "hongkong", name: "香港", region: "港澳台", latitude: 22.32, longitude: 114.17, timeZoneID: "Asia/Hong_Kong"),
        .init(id: "macau", name: "澳门", region: "港澳台", latitude: 22.20, longitude: 113.54, timeZoneID: "Asia/Macau"),
        .init(id: "taipei", name: "台北", region: "港澳台", latitude: 25.03, longitude: 121.57, timeZoneID: "Asia/Taipei"),
        .init(id: "tokyo", name: "东京", region: "海外", latitude: 35.68, longitude: 139.69, timeZoneID: "Asia/Tokyo"),
        .init(id: "seoul", name: "首尔", region: "海外", latitude: 37.57, longitude: 126.98, timeZoneID: "Asia/Seoul"),
        .init(id: "singapore", name: "新加坡", region: "海外", latitude: 1.35, longitude: 103.82, timeZoneID: "Asia/Singapore"),
        .init(id: "bangkok", name: "曼谷", region: "海外", latitude: 13.76, longitude: 100.50, timeZoneID: "Asia/Bangkok"),
        .init(id: "london", name: "伦敦", region: "海外", latitude: 51.51, longitude: -0.13, timeZoneID: "Europe/London"),
        .init(id: "paris", name: "巴黎", region: "海外", latitude: 48.86, longitude: 2.35, timeZoneID: "Europe/Paris"),
        .init(id: "newyork", name: "纽约", region: "海外", latitude: 40.71, longitude: -74.01, timeZoneID: "America/New_York"),
        .init(id: "losangeles", name: "洛杉矶", region: "海外", latitude: 34.05, longitude: -118.24, timeZoneID: "America/Los_Angeles"),
        .init(id: "vancouver", name: "温哥华", region: "海外", latitude: 49.28, longitude: -123.12, timeZoneID: "America/Vancouver"),
        .init(id: "toronto", name: "多伦多", region: "海外", latitude: 43.65, longitude: -79.38, timeZoneID: "America/Toronto"),
        .init(id: "sydney", name: "悉尼", region: "海外", latitude: -33.87, longitude: 151.21, timeZoneID: "Australia/Sydney"),
        .init(id: "melbourne", name: "墨尔本", region: "海外", latitude: -37.81, longitude: 144.96, timeZoneID: "Australia/Melbourne"),
    ]

    static var regions: [String] {
        var seen: [String] = []
        for city in all where !seen.contains(city.region) { seen.append(city.region) }
        return seen
    }
}
