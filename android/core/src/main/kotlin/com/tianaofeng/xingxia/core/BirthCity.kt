package com.tianaofeng.xingxia.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** 出生城市：用于计算上升星座和真太阳时。 */
data class BirthCity(
    val id: String,
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
) {
    val zone: ZoneId get() = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())

    companion object {
        val BEIJING = BirthCity("beijing", "北京", "华北", 39.90, 116.40, "Asia/Shanghai")

        fun find(id: String?): BirthCity? = id?.let { key -> ALL.firstOrNull { it.id == key } }

        val ALL: List<BirthCity> = listOf(
            BEIJING,
            BirthCity("tianjin", "天津", "华北", 39.13, 117.20, "Asia/Shanghai"),
            BirthCity("shijiazhuang", "石家庄", "华北", 38.04, 114.51, "Asia/Shanghai"),
            BirthCity("taiyuan", "太原", "华北", 37.87, 112.55, "Asia/Shanghai"),
            BirthCity("hohhot", "呼和浩特", "华北", 40.84, 111.75, "Asia/Shanghai"),
            BirthCity("shanghai", "上海", "华东", 31.23, 121.47, "Asia/Shanghai"),
            BirthCity("nanjing", "南京", "华东", 32.06, 118.80, "Asia/Shanghai"),
            BirthCity("suzhou", "苏州", "华东", 31.30, 120.59, "Asia/Shanghai"),
            BirthCity("wuxi", "无锡", "华东", 31.49, 120.31, "Asia/Shanghai"),
            BirthCity("hangzhou", "杭州", "华东", 30.27, 120.16, "Asia/Shanghai"),
            BirthCity("ningbo", "宁波", "华东", 29.87, 121.55, "Asia/Shanghai"),
            BirthCity("hefei", "合肥", "华东", 31.82, 117.23, "Asia/Shanghai"),
            BirthCity("fuzhou", "福州", "华东", 26.07, 119.30, "Asia/Shanghai"),
            BirthCity("xiamen", "厦门", "华东", 24.48, 118.09, "Asia/Shanghai"),
            BirthCity("nanchang", "南昌", "华东", 28.68, 115.86, "Asia/Shanghai"),
            BirthCity("jinan", "济南", "华东", 36.65, 117.12, "Asia/Shanghai"),
            BirthCity("qingdao", "青岛", "华东", 36.07, 120.38, "Asia/Shanghai"),
            BirthCity("guangzhou", "广州", "华南", 23.13, 113.26, "Asia/Shanghai"),
            BirthCity("shenzhen", "深圳", "华南", 22.54, 114.06, "Asia/Shanghai"),
            BirthCity("dongguan", "东莞", "华南", 23.02, 113.75, "Asia/Shanghai"),
            BirthCity("foshan", "佛山", "华南", 23.02, 113.12, "Asia/Shanghai"),
            BirthCity("nanning", "南宁", "华南", 22.82, 108.37, "Asia/Shanghai"),
            BirthCity("haikou", "海口", "华南", 20.04, 110.20, "Asia/Shanghai"),
            BirthCity("wuhan", "武汉", "华中", 30.59, 114.31, "Asia/Shanghai"),
            BirthCity("changsha", "长沙", "华中", 28.23, 112.94, "Asia/Shanghai"),
            BirthCity("zhengzhou", "郑州", "华中", 34.75, 113.63, "Asia/Shanghai"),
            BirthCity("chongqing", "重庆", "西南", 29.56, 106.55, "Asia/Shanghai"),
            BirthCity("chengdu", "成都", "西南", 30.57, 104.07, "Asia/Shanghai"),
            BirthCity("kunming", "昆明", "西南", 25.04, 102.71, "Asia/Shanghai"),
            BirthCity("guiyang", "贵阳", "西南", 26.65, 106.63, "Asia/Shanghai"),
            BirthCity("lhasa", "拉萨", "西南", 29.65, 91.14, "Asia/Shanghai"),
            BirthCity("xian", "西安", "西北", 34.34, 108.94, "Asia/Shanghai"),
            BirthCity("lanzhou", "兰州", "西北", 36.06, 103.83, "Asia/Shanghai"),
            BirthCity("xining", "西宁", "西北", 36.62, 101.78, "Asia/Shanghai"),
            BirthCity("yinchuan", "银川", "西北", 38.49, 106.23, "Asia/Shanghai"),
            BirthCity("urumqi", "乌鲁木齐", "西北", 43.83, 87.62, "Asia/Shanghai"),
            BirthCity("shenyang", "沈阳", "东北", 41.81, 123.43, "Asia/Shanghai"),
            BirthCity("dalian", "大连", "东北", 38.91, 121.61, "Asia/Shanghai"),
            BirthCity("changchun", "长春", "东北", 43.82, 125.32, "Asia/Shanghai"),
            BirthCity("harbin", "哈尔滨", "东北", 45.80, 126.53, "Asia/Shanghai"),
            BirthCity("hongkong", "香港", "港澳台", 22.32, 114.17, "Asia/Hong_Kong"),
            BirthCity("macau", "澳门", "港澳台", 22.20, 113.54, "Asia/Macau"),
            BirthCity("taipei", "台北", "港澳台", 25.03, 121.57, "Asia/Taipei"),
            BirthCity("tokyo", "东京", "海外", 35.68, 139.69, "Asia/Tokyo"),
            BirthCity("seoul", "首尔", "海外", 37.57, 126.98, "Asia/Seoul"),
            BirthCity("singapore", "新加坡", "海外", 1.35, 103.82, "Asia/Singapore"),
            BirthCity("bangkok", "曼谷", "海外", 13.76, 100.50, "Asia/Bangkok"),
            BirthCity("london", "伦敦", "海外", 51.51, -0.13, "Europe/London"),
            BirthCity("paris", "巴黎", "海外", 48.86, 2.35, "Europe/Paris"),
            BirthCity("newyork", "纽约", "海外", 40.71, -74.01, "America/New_York"),
            BirthCity("losangeles", "洛杉矶", "海外", 34.05, -118.24, "America/Los_Angeles"),
            BirthCity("vancouver", "温哥华", "海外", 49.28, -123.12, "America/Vancouver"),
            BirthCity("toronto", "多伦多", "海外", 43.65, -79.38, "America/Toronto"),
            BirthCity("sydney", "悉尼", "海外", -33.87, 151.21, "Australia/Sydney"),
            BirthCity("melbourne", "墨尔本", "海外", -37.81, 144.96, "Australia/Melbourne"),
        )

        val REGIONS: List<String> get() = ALL.map { it.region }.distinct()
    }
}

/** 生日 + 可选出生时间/城市。 */
data class BirthInfo(
    val birthday: DayKey,
    val hour: Int? = null,
    val minute: Int? = null,
    val cityId: String? = null,
) {
    val hasTime: Boolean get() = hour != null
    val city: BirthCity get() = BirthCity.find(cityId) ?: BirthCity.BEIJING

    /** 出生时刻（城市当地时间）；不知道时间时取中午。 */
    val moment: Instant
        get() = LocalDateTime.of(birthday.year, birthday.month, birthday.day, hour ?: 12, minute ?: 0)
            .atZone(city.zone).toInstant()

    /** 用作随机种子的字符串，与 iOS 版一致 */
    val seedKey: String get() = "${birthday.rawValue}-${hour ?: -1}-${minute ?: -1}"
}
