package com.tianaofeng.xingxia.ui.screens.ask

import android.icu.text.Transliterator
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.tianaofeng.xingxia.data.Store
import com.tianaofeng.xingxia.ui.components.EmptyHint
import com.tianaofeng.xingxia.ui.components.Fonts
import com.tianaofeng.xingxia.ui.components.GlassCard
import com.tianaofeng.xingxia.ui.components.SecondaryText
import com.tianaofeng.xingxia.ui.components.SectionHeader
import com.tianaofeng.xingxia.ui.components.ToolIcon
import com.tianaofeng.xingxia.ui.components.pressable
import com.tianaofeng.xingxia.ui.nav.LocalNavigator
import com.tianaofeng.xingxia.ui.nav.Routes
import com.tianaofeng.xingxia.ui.screens.tools.ToolKind
import com.tianaofeng.xingxia.ui.theme.LocalPalette
import com.tianaofeng.xingxia.ui.theme.hex

/** 搜索结果：空查询时先列「最近用过」，再按分类列出全部；有查询时列出匹配项（与 iOS 的 SearchView 一致） */
@Composable
fun AskSearchResults(query: String) {
    val navigator = LocalNavigator.current
    val trimmed = query.trim()

    val open: (SearchItem) -> Unit = { item ->
        if (item.isTab) navigator.switchTab(item.route()) else navigator.open(item.route())
    }

    if (trimmed.isEmpty()) {
        val recent = SearchCatalog.recent()
        if (recent.isNotEmpty()) SearchSection("最近用过", Icons.Rounded.History, recent, open)
        for (group in SearchGroup.entries) {
            SearchSection(group.label, group.icon, SearchCatalog.all.filter { it.group == group }, open)
        }
    } else {
        val results = SearchCatalog.search(trimmed)
        if (results.isEmpty()) {
            EmptyHint(Icons.Rounded.Search, "没有找到「$trimmed」", "试试「吃」「硬币」「签」，或拼音首字母，如「yb」")
        } else {
            SearchSection("找到 ${results.size} 项", Icons.Rounded.Search, results, open)
        }
    }
}

@Composable
private fun SearchSection(title: String, icon: ImageVector, items: List<SearchItem>, onOpen: (SearchItem) -> Unit) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title, icon = icon)
        GlassCard(padding = 14.dp, spacing = 0.dp) {
            items.forEachIndexed { index, item ->
                if (index > 0) HorizontalDivider(Modifier.padding(start = 52.dp), color = palette.subtle(0.08f))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressable { onOpen(item) }
                        .clearAndSetSemantics {
                            contentDescription = "${item.name}，${item.subtitle}"
                            role = Role.Button
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ToolIcon(item.icon, palette.identity(item.colors), 40.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.name, style = Fonts.headline, color = palette.text)
                        SecondaryText(item.subtitle, style = Fonts.caption, maxLines = 2)
                    }
                    Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = palette.tertiaryText)
                }
            }
        }
    }
}

enum class SearchGroup(val label: String, val icon: ImageVector) {
    TOOLS("工具", Icons.Rounded.GridView),
    DIVINATION("占卜", Icons.Rounded.NightsStay),
    RECORDS("记录", Icons.Rounded.Book),
    ME("我的与设置", Icons.Rounded.Person),
}

/**
 * 一个可搜索的入口。route 是函数：年度回顾的默认年份按打开时的日期算；isTab 为 true 时切换到那个 tab。
 * keywords 是额外的中文关键词（空格分隔）。
 */
class SearchItem(
    val id: String,
    val group: SearchGroup,
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val colors: List<Color>,
    val keywords: String = "",
    val isTab: Boolean = false,
    val route: () -> String,
) {
    /** 名称和关键词的拼音首字母，每个词一段，如「pyb」「yb」 */
    private val initials: List<String> by lazy {
        (listOf(name) + keywords.split(" ").filter { it.isNotEmpty() }).map { Pinyin.initials(it) }
    }

    fun matches(query: String): Boolean {
        if (name.contains(query, ignoreCase = true) || subtitle.contains(query, ignoreCase = true) ||
            keywords.contains(query, ignoreCase = true)
        ) {
            return true
        }
        // 纯字母时按拼音首字母匹配
        val letters = query.lowercase().filterNot { it.isWhitespace() }
        if (letters.isEmpty() || !letters.all { it in 'a'..'z' }) return false
        return initials.any { it.contains(letters) }
    }
}

/** 拼音首字母：用系统 ICU 的汉字转拼音，取每个音节的首字母（「抛硬币」→「pyb」）。Android 10 以下没有转写，只按中文匹配 */
private object Pinyin {
    private val transliterator: Any? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) create() else null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun create(): Transliterator? = runCatching { Transliterator.getInstance("Han-Latin; Latin-ASCII") }.getOrNull()

    fun initials(text: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ""
        val latin = transliterate(text) ?: return ""
        return latin.lowercase()
            .split(Regex("\\s+"))
            .mapNotNull { it.firstOrNull() }
            .joinToString("")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun transliterate(text: String): String? {
        val t = transliterator as? Transliterator ?: return null
        return runCatching { t.transliterate(text) }.getOrNull()
    }
}

object SearchCatalog {
    val all: List<SearchItem> by lazy { tools + divination + records + me }

    fun search(query: String): List<SearchItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return all.filter { it.matches(trimmed) }
    }

    /** 决策历史里的 tool 字段 → 搜索项（「最近用过」用） */
    fun item(forTool: String): SearchItem? {
        val id = when (forTool) {
            "nearby" -> ToolKind.FOOD.id
            "jiaobei" -> "stick"
            else -> forTool
        }
        return all.firstOrNull { it.id == id }
    }

    /** 最近 200 条决定里用过的工具，按最近一次使用排序，最多 6 个 */
    fun recent(): List<SearchItem> {
        val seen = HashSet<String>()
        val result = mutableListOf<SearchItem>()
        for (record in Store.records.items.asReversed().take(200)) {
            val item = item(record.tool) ?: continue
            if (!seen.add(item.id)) continue
            result.add(item)
            if (result.size == 6) break
        }
        return result
    }

    private val tools: List<SearchItem>
        get() = ToolKind.entries.filter { it.available }.map { tool ->
            SearchItem(tool.id, SearchGroup.TOOLS, tool.label, tool.subtitle, tool.icon, tool.colors, tool.keywords) { Routes.tool(tool) }
        }

    private val divination: List<SearchItem>
        get() = listOf(
            SearchItem("fortune", SearchGroup.DIVINATION, "运势详解", "五维运势与幸运指南", Icons.Rounded.AutoAwesome,
                listOf(hex(0x8B5CF6), hex(0xEC4899)), "今日运势 运气 幸运色 幸运数字") { Routes.FORTUNE },
            SearchItem("stick", SearchGroup.DIVINATION, "求签", "一日一签 · 解签 · 掷筊", Icons.Rounded.LocalFireDepartment,
                listOf(hex(0xFF8A3D), hex(0xE11D48)), "抽签 摇签 灵签 签文 掷筊 圣杯") { Routes.STICK },
            SearchItem("tarot", SearchGroup.DIVINATION, "塔罗占卜", "单张 · 时间之流 · 情感关系 · 问题解答", Icons.Rounded.Style,
                listOf(hex(0x8B5CF6), hex(0x3B82F6)), "塔罗牌 牌阵 占卜") { Routes.TAROT },
            SearchItem("answers", SearchGroup.DIVINATION, "答案之书", "默念问题 · 翻开一页", Icons.Rounded.MenuBook,
                listOf(hex(0xF5B83D), hex(0xB45309)), "答案 问题 是否") { Routes.ANSWERS },
            SearchItem("horoscope", SearchGroup.DIVINATION, "星座运势", "今日 · 明日 · 本周 · 本月", Icons.Rounded.NightsStay,
                listOf(hex(0x6366F1), hex(0xA78BFA)), "星座 白羊 金牛 双子 巨蟹 狮子 处女 天秤 天蝎 射手 摩羯 水瓶 双鱼") { Routes.HOROSCOPE },
            SearchItem("almanac", SearchGroup.DIVINATION, "黄历", "农历 · 节气 · 宜忌", Icons.Rounded.CalendarMonth,
                listOf(hex(0xEF4444), hex(0xF59E0B)), "老黄历 农历 节气 宜忌 万年历") { Routes.ALMANAC },
            SearchItem("auspicious", SearchGroup.DIVINATION, "择吉日", "搬家 · 开业 · 领证 · 出行", Icons.Rounded.EventAvailable,
                listOf(hex(0xEF4444), hex(0xF59E0B)), "吉日 黄道吉日 好日子 搬家 结婚 开业") { Routes.AUSPICIOUS },
            SearchItem("compatibility", SearchGroup.DIVINATION, "缘分配对", "星座 + 生肖", Icons.Rounded.Favorite,
                listOf(hex(0xFB7185), hex(0xF43F5E)), "配对 合不合 缘分 速配") { Routes.COMPATIBILITY },
            SearchItem("tarotJournal", SearchGroup.DIVINATION, "塔罗手记", "回看抽过的牌", Icons.Rounded.Book,
                listOf(hex(0x8B5CF6), hex(0x3B82F6)), "塔罗记录 解读") { Routes.TAROT_JOURNAL },
        )

    private val records: List<SearchItem>
        get() = listOf(
            SearchItem("journal", SearchGroup.RECORDS, "日记", "每天的心情、决定和签", Icons.Rounded.Book,
                listOf(hex(0x10B981), hex(0x22D3EE)), "时间线 回顾 本月", isTab = true) { Routes.JOURNAL },
            SearchItem("mood", SearchGroup.RECORDS, "心情", "每天点一下心情", Icons.Rounded.SentimentSatisfied,
                listOf(hex(0xFBBF24), hex(0xF97316)), "心情打卡 情绪") { Routes.MOOD },
            SearchItem("countdowns", SearchGroup.RECORDS, "倒数日", "纪念日 · 生日 · 重要的日子", Icons.Rounded.HourglassEmpty,
                listOf(hex(0x60A5FA), hex(0x6366F1)), "倒计时 纪念日 生日") { Routes.COUNTDOWNS },
            SearchItem("history", SearchGroup.RECORDS, "决策历史", "转过、抛过、抽过的每一次", Icons.Rounded.History,
                listOf(hex(0x94A3B8), hex(0x6366F1)), "历史 记录 决定") { Routes.HISTORY },
            SearchItem("fortuneCalendar", SearchGroup.RECORDS, "运势日历", "一个月的运势起伏", Icons.Rounded.CalendarMonth,
                listOf(hex(0x8B5CF6), hex(0xEC4899)), "日历 运势 月历") { Routes.CALENDAR },
            SearchItem("annualReview", SearchGroup.RECORDS, "年度回顾", "一年的决定、心情和好运", Icons.Rounded.BarChart,
                listOf(hex(0xF59E0B), hex(0xEF4444)), "年终总结 回顾 关键词") { Routes.annualReview() },
            SearchItem("skyCalendar", SearchGroup.RECORDS, "天象日历", "月相 · 节气 · 流星雨", Icons.Rounded.NightsStay,
                listOf(hex(0x312E81), hex(0x6366F1)), "月相 满月 新月 流星雨 节气 天象") { Routes.SKY_CALENDAR },
        )

    private val me: List<SearchItem>
        get() = listOf(
            SearchItem("birthChart", SearchGroup.ME, "命盘", "八字 · 星盘三巨头", Icons.Rounded.GridOn,
                listOf(hex(0xA78BFA), hex(0xEC4899)), "八字 星盘 上升 月亮星座") { Routes.BIRTH_CHART },
            SearchItem("profiles", SearchGroup.ME, "档案", "自己和家人的生日、出生地", Icons.Rounded.People,
                listOf(hex(0x60A5FA), hex(0x6366F1)), "家人 生日 出生地") { Routes.PROFILES },
            SearchItem("themes", SearchGroup.ME, "主题", "配色 · 深浅色 · 按时段切换", Icons.Rounded.Palette,
                listOf(hex(0xF472B6), hex(0x8B5CF6)), "外观 深色 浅色 配色 皮肤") { Routes.THEMES },
            SearchItem("backup", SearchGroup.ME, "数据备份", "导出、导入全部数据", Icons.Rounded.SettingsBackupRestore,
                listOf(hex(0x2DD4BF), hex(0x0EA5E9)), "备份 恢复 导出 导入 换手机") { Routes.BACKUP },
            SearchItem("settings", SearchGroup.ME, "设置", "声音、触感、提醒等", Icons.Rounded.Settings,
                listOf(hex(0x94A3B8), hex(0x475569)), "偏好") { Routes.SETTINGS },
            SearchItem("settings.sound", SearchGroup.ME, "音效与震动", "设置 · 声音与触感", Icons.Rounded.Tune,
                listOf(hex(0x94A3B8), hex(0x475569)), "声音 静音 震动 触感 振动") { Routes.SETTINGS },
            SearchItem("settings.shake", SearchGroup.ME, "摇一摇触发", "设置 · 交互", Icons.Rounded.Smartphone,
                listOf(hex(0x94A3B8), hex(0x475569)), "摇一摇 星空背景 动画") { Routes.SETTINGS },
            SearchItem("settings.reminder", SearchGroup.ME, "每日运势提醒", "设置 · 提醒", Icons.Rounded.Schedule,
                listOf(hex(0x94A3B8), hex(0x475569)), "提醒 通知 推送 天象提醒") { Routes.SETTINGS },
            SearchItem("settings.ritual", SearchGroup.ME, "每日仪式", "设置 · 每天第一次打开时的小仪式", Icons.Rounded.WbSunny,
                listOf(hex(0x94A3B8), hex(0x475569)), "仪式 月相 今日卡片") { Routes.SETTINGS },
        )
}
