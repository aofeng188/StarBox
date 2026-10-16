package com.tianaofeng.xingxia.data

import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.ui.screens.tools.FavoriteTool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Instant
import java.util.UUID
import kotlin.math.abs

/** 备份里的数据类别；key 就是 JSON 里的键名（与 iOS 版一致） */
enum class BackupCategory(val key: String, val label: String, val unit: String) {
    PROFILES("profiles", "档案", "个"),
    DECISION_RECORDS("decisionRecords", "决策记录", "条"),
    STICK_DRAWS("stickDraws", "每日一签", "条"),
    TAROT_READINGS("tarotReadings", "塔罗手记", "次"),
    MOODS("moods", "心情打卡", "天"),
    COUNTDOWNS("countdowns", "倒数日", "个"),
    WHEEL_TEMPLATES("wheelTemplates", "转盘", "个"),
    FOOD_MENUS("foodMenus", "菜单", "个"),
    FOOD_LOGS("foodLogs", "吃过的", "条"),
    BLOCKED_FOODS("blockedFoods", "不想吃", "样"),
    LOTTERY_POOLS("lotteryPools", "抽奖池", "个"),
    ROSTERS("rosters", "分组名单", "个"),
    CUSTOM_PROMPTS("customPrompts", "自定义题目", "道"),
    SCRATCH_PRESETS("scratchPresets", "刮刮卡", "个");

    /** 本机这一类的条数 */
    val localCount: Int
        get() = when (this) {
            PROFILES -> Store.profiles.items.size
            DECISION_RECORDS -> Store.records.items.size
            STICK_DRAWS -> Store.stickDraws.items.size
            TAROT_READINGS -> Store.tarotReadings.items.size
            MOODS -> Store.moods.items.size
            COUNTDOWNS -> Store.countdowns.items.size
            WHEEL_TEMPLATES -> Store.wheels.items.size
            FOOD_MENUS -> Store.foodMenus.items.size
            FOOD_LOGS -> Store.foodLogs.items.size
            BLOCKED_FOODS -> Store.blockedFoods.items.size
            LOTTERY_POOLS -> Store.lotteryPools.items.size
            ROSTERS -> Store.rosters.items.size
            CUSTOM_PROMPTS -> Store.customPrompts.items.size
            SCRATCH_PRESETS -> Store.scratchPresets.items.size
        }
}

enum class BackupMode {
    /** 按 id 去重，已有的保留 */
    MERGE,

    /** 文件里有的类别先清空再导入；文件里没有的类别不动 */
    REPLACE,
}

/** 备份出错；message 是给用户看的说明（与 iOS 的 BackupError 文案一致） */
class BackupException(message: String) : Exception(message) {
    companion object {
        val unreadable get() = BackupException("文件读不出来，可能不是 JSON 或已经损坏。")
        val notBackup get() = BackupException("这不是星匣导出的备份文件。")
        val newerVersion get() = BackupException("这份备份来自更新版本的星匣，请先更新 App 再恢复。")
        val tooLarge get() = BackupException("文件太大，不像是星匣的备份。")
        val exportFailed get() = BackupException("生成备份文件失败，请稍后再试。")
    }
}

/** 解析好、还没导入的备份文件 */
class BackupFile internal constructor(
    val version: Int,
    /** "ios" / "android" */
    val platform: String,
    /** 导出时间（毫秒） */
    val exportedAt: Long?,
    /** 只含文件里出现的类别 */
    internal val sections: Map<BackupCategory, List<BackupFields>>,
    internal val settings: BackupFields?,
) {
    val platformName: String
        get() = when (platform) {
            "ios" -> "iPhone"
            "android" -> "Android"
            else -> "未知设备"
        }

    /** 文件里有这一类时返回条数，没有这一类返回 null */
    fun count(category: BackupCategory): Int? = sections[category]?.size

    val totalCount: Int get() = sections.values.sumOf { it.size }

    val includesSettings: Boolean get() = settings != null
}

class BackupRestoreResult {
    val added: MutableMap<BackupCategory, Int> = mutableMapOf()

    /** 已有、重复或数据不完整而跳过的条数 */
    var skipped = 0

    val totalAdded: Int get() = added.values.sum()
}

/**
 * 数据备份：导出全部本地数据和首页偏好为 JSON（与 iOS 版格式互通），导入时合并或替换。
 * 导入规则与 iOS 的 Backup.swift 一致，同一份文件在两端导入的结果相同。
 */
object Backup {
    const val FORMAT = "xingxia-backup"
    const val VERSION = 1
    private const val MAX_BYTES = 64 * 1024 * 1024

    /** 导出时的默认文件名 */
    fun defaultFileName(today: DayKey = DayKey.today): String = "星匣备份-${today.rawValue}.json"

    private val printer = Json { prettyPrint = true }

    // region 导出

    fun export(): String {
        val root = buildJsonObject {
            put("format", FORMAT)
            put("version", VERSION)
            put("platform", "android")
            put("exportedAt", System.currentTimeMillis())
            putJsonArray(BackupCategory.PROFILES.key) {
                Store.sortedProfiles.forEachIndexed { index, p ->
                    addJsonObject {
                        put("id", p.id.lowercase())
                        put("name", p.name)
                        put("relation", p.relation)
                        put("avatar", p.avatar)
                        put("colorIndex", p.colorIndex)
                        put("birthday", p.birthdayKey.rawValue)
                        put("hasBirthTime", p.hasBirthTime)
                        put("birthHour", p.birthHour)
                        put("birthMinute", p.birthMinute)
                        put("birthCityId", p.birthCityId)
                        put("enteredAsLunar", p.enteredAsLunar)
                        put("sortIndex", index)
                        put("createdAt", p.createdAt)
                    }
                }
            }
            putJsonArray(BackupCategory.DECISION_RECORDS.key) {
                for (r in Store.records.items.sortedBy { it.date }) {
                    addJsonObject {
                        put("id", r.id.lowercase())
                        put("tool", r.tool)
                        put("title", r.title)
                        put("result", r.result)
                        put("detail", r.detail)
                        put("date", r.date)
                    }
                }
            }
            putJsonArray(BackupCategory.STICK_DRAWS.key) {
                for (d in Store.stickDraws.items.sortedBy { it.date }) {
                    addJsonObject {
                        put("dayKey", d.dayKey)
                        put("profileId", d.profileId?.lowercase())
                        put("stickNumber", d.stickNumber)
                        put("date", d.date)
                    }
                }
            }
            putJsonArray(BackupCategory.TAROT_READINGS.key) {
                for (r in Store.tarotReadings.items.sortedBy { it.date }) {
                    addJsonObject {
                        put("id", r.id.lowercase())
                        put("date", r.date)
                        put("spread", r.spread)
                        put("cards", r.cards)
                        put("question", r.question)
                    }
                }
            }
            putJsonArray(BackupCategory.MOODS.key) {
                for (m in Store.moods.items.sortedBy { it.dayKey }) {
                    addJsonObject {
                        put("dayKey", m.dayKey)
                        put("mood", m.mood)
                        put("note", m.note)
                        putJsonArray("tags") { m.tags.forEach { add(it) } }
                        put("date", m.date)
                    }
                }
            }
            putJsonArray(BackupCategory.COUNTDOWNS.key) {
                for (c in Store.countdowns.items.sortedBy { it.createdAt }) {
                    addJsonObject {
                        put("id", c.id.lowercase())
                        put("title", c.title)
                        put("emoji", c.emoji)
                        put("date", (DayKey.fromRaw(c.date) ?: c.createdDay).rawValue)
                        put("kind", c.kind)
                        put("repeatsYearly", c.repeatsYearly)
                        put("isLunar", c.isLunar)
                        put("lunarMonth", c.lunarMonth)
                        put("lunarDay", c.lunarDay)
                        put("colorIndex", c.colorIndex)
                        put("pinned", c.pinned)
                        put("createdAt", c.createdAt)
                    }
                }
            }
            putJsonArray(BackupCategory.WHEEL_TEMPLATES.key) {
                for (w in Store.wheels.items.sortedBy { it.createdAt }) {
                    addJsonObject {
                        put("id", w.id.lowercase())
                        put("name", w.name)
                        put("removeAfterSpin", w.removeAfterSpin)
                        put("createdAt", w.createdAt)
                        putJsonArray("options") {
                            for (o in w.options) {
                                addJsonObject {
                                    put("id", o.id.lowercase())
                                    put("title", o.title)
                                    put("weight", finite(o.weight))
                                }
                            }
                        }
                    }
                }
            }
            putJsonArray(BackupCategory.FOOD_MENUS.key) {
                for (m in Store.foodMenus.items.sortedBy { it.createdAt }) {
                    addJsonObject {
                        put("id", m.id.lowercase())
                        put("name", m.name)
                        put("icon", m.icon)
                        put("createdAt", m.createdAt)
                        putJsonArray("items") {
                            for (i in m.items) {
                                addJsonObject {
                                    put("id", i.id.lowercase())
                                    put("name", i.name)
                                    put("note", i.note)
                                    put("weight", finite(i.weight))
                                }
                            }
                        }
                    }
                }
            }
            putJsonArray(BackupCategory.FOOD_LOGS.key) {
                for (f in Store.foodLogs.items.sortedBy { it.date }) {
                    addJsonObject {
                        put("name", f.name)
                        put("source", f.source)
                        put("date", f.date)
                    }
                }
            }
            putJsonArray(BackupCategory.BLOCKED_FOODS.key) {
                for (b in Store.blockedFoods.items.sortedBy { it.date }) {
                    addJsonObject {
                        put("name", b.name)
                        put("date", b.date)
                    }
                }
            }
            putJsonArray(BackupCategory.LOTTERY_POOLS.key) {
                for (p in Store.lotteryPools.items.sortedBy { it.createdAt }) {
                    addJsonObject {
                        put("id", p.id.lowercase())
                        put("name", p.name)
                        put("icon", p.icon)
                        put("removeAfterDraw", p.removeAfterDraw)
                        put("createdAt", p.createdAt)
                        putJsonArray("items") {
                            for (i in p.items) {
                                addJsonObject {
                                    put("id", i.id.lowercase())
                                    put("name", i.name)
                                    put("weight", finite(i.weight))
                                    put("quantity", i.quantity)
                                    put("remaining", i.remaining)
                                }
                            }
                        }
                    }
                }
            }
            putJsonArray(BackupCategory.ROSTERS.key) {
                for (r in Store.rosters.items.sortedBy { it.createdAt }) {
                    addJsonObject {
                        put("id", r.id.lowercase())
                        put("name", r.name)
                        putJsonArray("members") { r.members.forEach { add(it) } }
                        put("createdAt", r.createdAt)
                    }
                }
            }
            putJsonArray(BackupCategory.CUSTOM_PROMPTS.key) {
                for (p in Store.customPrompts.items.sortedBy { it.createdAt }) {
                    addJsonObject {
                        put("id", p.id.lowercase())
                        put("kind", p.kind)
                        put("text", p.text)
                        put("level", p.level)
                        put("createdAt", p.createdAt)
                    }
                }
            }
            putJsonArray(BackupCategory.SCRATCH_PRESETS.key) {
                for (p in Store.scratchPresets.items.sortedBy { it.createdAt }) {
                    addJsonObject {
                        put("id", p.id.lowercase())
                        put("name", p.name)
                        putJsonArray("prizes") { p.prizes.forEach { add(it) } }
                        put("blankWeight", finite(p.blankWeight))
                        put("createdAt", p.createdAt)
                    }
                }
            }
            putJsonObject("settings") {
                putJsonArray("favoriteTools") { AppSettings.favoriteTools.forEach { add(it) } }
                putJsonArray("homeCards") { AppSettings.homeCards.forEach { add(it.key) } }
                val hidden = AppSettings.hiddenHomeCards
                putJsonArray("hiddenHomeCards") { HomeCard.entries.filter { it in hidden }.forEach { add(it.key) } }
            }
        }
        return runCatching { printer.encodeToString(JsonObject.serializer(), root) }.getOrElse { throw BackupException.exportFailed }
    }

    /** NaN 等非法值写进 JSON 会让另一端读不出来，按 1 处理 */
    private fun finite(value: Double): Double = if (value.isFinite()) value else 1.0

    // endregion

    // region 解析

    fun parse(bytes: ByteArray): BackupFile {
        if (bytes.size > MAX_BYTES) throw BackupException.tooLarge
        val text = bytes.toString(Charsets.UTF_8).removePrefix("﻿")
        val root = runCatching { Json.parseToJsonElement(text) }.getOrNull() as? JsonObject ?: throw BackupException.unreadable
        val fields = BackupFields(root)
        if (fields.string("format") != FORMAT) throw BackupException.notBackup
        val version = fields.int("version") ?: VERSION
        if (version > VERSION) throw BackupException.newerVersion
        val sections = BackupCategory.entries
            .filter { root[it.key] is JsonArray }
            .associateWith { fields.objects(it.key) }
        val settings = (root["settings"] as? JsonObject)?.let { BackupFields(it) }
        return BackupFile(version, fields.string("platform") ?: "", fields.date("exportedAt"), sections, settings)
    }

    // endregion

    // region 导入

    /**
     * 导入到本机。先在内存里算好每一类的新列表，全部算完再一起写回，中途出错时本机数据不变。
     * 写回后刷新当前档案（原来的还在就沿用，否则取第一个）、提醒和小组件。
     */
    fun restore(file: BackupFile, mode: BackupMode, applySettings: Boolean = true): BackupRestoreResult {
        val restorer = BackupRestorer(mode)
        restorer.run(file)
        restorer.commit()
        if (applySettings) file.settings?.let { apply(it, mode) }
        didRestore()
        return restorer.result
    }

    private fun didRestore() {
        Store.setPrimary(Store.primary ?: Store.sortedProfiles.firstOrNull())
    }

    /** 合并时只补充收藏；替换时收藏、首页模块顺序和隐藏模块都用备份里的 */
    private fun apply(fields: BackupFields, mode: BackupMode) {
        fields.strings("favoriteTools")?.let { favorites ->
            val valid = favorites.filter { FavoriteTool.of(it) != null }
            AppSettings.favoriteTools = (if (mode == BackupMode.MERGE) AppSettings.favoriteTools + valid else valid).distinct()
        }
        if (mode != BackupMode.REPLACE) return
        fields.strings("homeCards")?.let { keys ->
            AppSettings.homeCards = withMissingCards(keys.mapNotNull { HomeCard.of(it) }.distinct())
        }
        fields.strings("hiddenHomeCards")?.let { keys ->
            AppSettings.hiddenHomeCards = keys.mapNotNull { HomeCard.of(it) }.toSet()
        }
    }

    /** 备份里没有的模块（新版本加的、或另一端没有的）按默认顺序插到它前一个模块后面 */
    private fun withMissingCards(stored: List<HomeCard>): List<HomeCard> {
        val result = stored.toMutableList()
        val defaults = HomeCard.entries
        defaults.forEachIndexed { index, card ->
            if (card in result) return@forEachIndexed
            val previous = defaults.subList(0, index).lastOrNull { it in result }
            val position = previous?.let { result.indexOf(it) } ?: -1
            if (position >= 0) result.add(position + 1, card) else result.add(0, card)
        }
        return result
    }

    // endregion
}

// region 宽松读取

/** JSON 对象的宽松读取：缺字段或类型不对都返回 null，由调用方给默认值，坏数据不会崩溃 */
internal class BackupFields(private val raw: JsonObject) {
    private fun primitive(key: String): JsonPrimitive? = (raw[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }

    fun string(key: String): String? = primitive(key)?.takeIf { it.isString }?.content

    /** 数字；写成字符串的数字也认，布尔值不算 */
    fun number(key: String): Double? {
        val p = primitive(key) ?: return null
        val text = p.content.trim()
        if (!p.isString && (text == "true" || text == "false")) return null
        return text.toDoubleOrNull()?.takeIf { it.isFinite() }
    }

    fun long(key: String): Long? = number(key)?.takeIf { abs(it) < 9e15 }?.toLong()

    fun int(key: String): Int? = long(key)?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()

    fun bool(key: String): Boolean? {
        val p = primitive(key) ?: return null
        if (p.isString) {
            return when (p.content.trim().lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> null
            }
        }
        return when (p.content) {
            "true" -> true
            "false" -> false
            else -> p.content.toDoubleOrNull()?.let { it != 0.0 }
        }
    }

    /** 毫秒时间戳 */
    fun date(key: String): Long? = number(key)?.takeIf { it > 0 && it < 32_503_680_000_000.0 }?.toLong()

    /** yyyyMMdd 整数，日期不存在时返回 null */
    fun dayKey(key: String): DayKey? {
        val value = int(key) ?: return null
        val year = value / 10000
        val month = value / 100 % 100
        val day = value % 100
        if (year !in 1800..2300 || month !in 1..12) return null
        val isLeap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
        val lengths = intArrayOf(31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        if (day !in 1..lengths[month - 1]) return null
        return DayKey(year, month, day)
    }

    /** UUID 字符串，不区分大小写，统一成小写 */
    fun uuid(key: String): String? = string(key)?.trim()?.takeIf { UUID_PATTERN.matches(it) }?.lowercase()

    fun strings(key: String): List<String>? =
        (raw[key] as? JsonArray)?.mapNotNull { element -> (element as? JsonPrimitive)?.takeIf { it.isString }?.content }

    fun objects(key: String): List<BackupFields> =
        (raw[key] as? JsonArray)?.mapNotNull { element -> (element as? JsonObject)?.let { BackupFields(it) } } ?: emptyList()

    private companion object {
        val UUID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}

// endregion

// region 导入实现

/**
 * 逐类导入。合并时按 id 去重；内容完全相同的（例如两端各自生成的示例转盘、同名同生日的档案）也视为已有，不重复导入。
 * 各类的新列表先存在这里，[commit] 时一起写回；为 null 的类别不动。
 */
private class BackupRestorer(private val mode: BackupMode) {
    val result = BackupRestoreResult()

    /** 与已有档案是同一个人（名字、生日相同）时：备份里的 id → 已有的 id，每日一签跟着改过来 */
    private val profileRemap = HashMap<String, String>()
    private val now = System.currentTimeMillis()

    private var profiles: List<Profile>? = null
    private var records: List<DecisionRecord>? = null
    private var stickDraws: List<StickDraw>? = null
    private var tarotReadings: List<TarotReading>? = null
    private var moods: List<MoodEntry>? = null
    private var countdowns: List<Countdown>? = null
    private var wheels: List<WheelTemplate>? = null
    private var foodMenus: List<FoodMenu>? = null
    private var foodLogs: List<FoodLog>? = null
    private var blockedFoods: List<BlockedFood>? = null
    private var lotteryPools: List<LotteryPool>? = null
    private var rosters: List<Roster>? = null
    private var customPrompts: List<CustomPrompt>? = null
    private var scratchPresets: List<ScratchPreset>? = null

    fun run(file: BackupFile) {
        // 档案先导入，每日一签要用 id 映射
        file.sections[BackupCategory.PROFILES]?.let { profiles(it) }
        file.sections[BackupCategory.DECISION_RECORDS]?.let { decisionRecords(it) }
        file.sections[BackupCategory.STICK_DRAWS]?.let { stickDraws(it) }
        file.sections[BackupCategory.TAROT_READINGS]?.let { tarotReadings(it) }
        file.sections[BackupCategory.MOODS]?.let { moods(it) }
        file.sections[BackupCategory.COUNTDOWNS]?.let { countdowns(it) }
        file.sections[BackupCategory.WHEEL_TEMPLATES]?.let { wheelTemplates(it) }
        file.sections[BackupCategory.FOOD_MENUS]?.let { foodMenus(it) }
        file.sections[BackupCategory.FOOD_LOGS]?.let { foodLogs(it) }
        file.sections[BackupCategory.BLOCKED_FOODS]?.let { blockedFoods(it) }
        file.sections[BackupCategory.LOTTERY_POOLS]?.let { lotteryPools(it) }
        file.sections[BackupCategory.ROSTERS]?.let { rosters(it) }
        file.sections[BackupCategory.CUSTOM_PROMPTS]?.let { customPrompts(it) }
        file.sections[BackupCategory.SCRATCH_PRESETS]?.let { scratchPresets(it) }
    }

    fun commit() {
        profiles?.let { list -> Store.profiles.update { list } }
        records?.let { list -> Store.records.update { list } }
        stickDraws?.let { list -> Store.stickDraws.update { list } }
        tarotReadings?.let { list -> Store.tarotReadings.update { list } }
        moods?.let { list -> Store.moods.update { list } }
        countdowns?.let { list -> Store.countdowns.update { list } }
        wheels?.let { list -> Store.wheels.update { list } }
        foodMenus?.let { list -> Store.foodMenus.update { list } }
        foodLogs?.let { list -> Store.foodLogs.update { list } }
        blockedFoods?.let { list -> Store.blockedFoods.update { list } }
        lotteryPools?.let { list -> Store.lotteryPools.update { list } }
        rosters?.let { list -> Store.rosters.update { list } }
        customPrompts?.let { list -> Store.customPrompts.update { list } }
        scratchPresets?.let { list -> Store.scratchPresets.update { list } }
    }

    // 工具

    /** 合并模式下的已有数据；替换模式下这一类先清空，返回空 */
    private fun <T> existing(items: List<T>): List<T> = if (mode == BackupMode.MERGE) items else emptyList()

    private fun added(category: BackupCategory) {
        result.added[category] = (result.added[category] ?: 0) + 1
    }

    private fun skipped() {
        result.skipped += 1
    }

    private fun signature(parts: List<String>): String = parts.joinToString("\u001F")

    private fun newId(): String = UUID.randomUUID().toString()

    /** 权重：0 是用户设的「不参与」，原样保留；缺失或负数按 1；过大的截到 1000（与 iOS 一致） */
    private fun weight(value: Double?): Double = if (value != null && value >= 0) minOf(value, 1000.0) else 1.0

    // 各类数据

    private fun profiles(items: List<BackupFields>) {
        val old = existing(Store.profiles.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val people = HashMap<String, String>()
        for (profile in old) people[signature(listOf(profile.name, profile.birthdayKey.rawValue.toString()))] = profile.id
        var nextIndex = (old.maxOfOrNull { it.sortIndex } ?: -1) + 1
        // 按备份里的 sortIndex（没有就按数组位置）排好，导入时重新编号
        val ordered = items.withIndex()
            .sortedWith(compareBy<IndexedValue<BackupFields>>({ it.value.int("sortIndex") ?: it.index }, { it.index }))
            .map { it.value }
        val added = mutableListOf<Profile>()
        for (f in ordered) {
            val id = f.uuid("id") ?: newId()
            val name = f.string("name") ?: ""
            val birthday = f.dayKey("birthday") ?: DayKey(1998, 6, 18)
            val person = signature(listOf(name, birthday.rawValue.toString()))
            if (id in ids) {
                skipped()
                continue
            }
            val same = people[person]
            if (same != null) {
                profileRemap[id] = same
                skipped()
                continue
            }
            added += Profile(
                id = id,
                name = name,
                relation = f.string("relation") ?: "自己",
                avatar = f.string("avatar") ?: "🌟",
                colorIndex = f.int("colorIndex") ?: 0,
                birthday = birthday.rawValue,
                hasBirthTime = f.bool("hasBirthTime") ?: false,
                birthHour = (f.int("birthHour") ?: 12).coerceIn(0, 23),
                birthMinute = (f.int("birthMinute") ?: 0).coerceIn(0, 59),
                birthCityId = f.string("birthCityId")?.takeIf { it.isNotEmpty() },
                enteredAsLunar = f.bool("enteredAsLunar") ?: false,
                sortIndex = nextIndex++,
                createdAt = f.date("createdAt") ?: now,
            )
            ids += id
            people[person] = id
            added(BackupCategory.PROFILES)
        }
        profiles = old + added
    }

    private fun decisionRecords(items: List<BackupFields>) {
        val old = existing(Store.records.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val added = mutableListOf<DecisionRecord>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            if (!ids.add(id)) {
                skipped()
                continue
            }
            added += DecisionRecord(
                id = id,
                tool = f.string("tool") ?: "",
                title = f.string("title") ?: "",
                result = f.string("result") ?: "",
                detail = f.string("detail"),
                date = f.date("date") ?: now,
            )
            added(BackupCategory.DECISION_RECORDS)
        }
        // 历史页按列表顺序显示，合并后按时间排好
        records = (old + added).sortedBy { it.date }
    }

    private fun stickDraws(items: List<BackupFields>) {
        fun key(day: Int, owner: String?) = "$day|${owner ?: ""}"
        val old = existing(Store.stickDraws.items)
        val keys = old.map { key(it.dayKey, it.profileId?.lowercase()) }.toHashSet()
        val added = mutableListOf<StickDraw>()
        for (f in items) {
            val day = f.dayKey("dayKey")
            val number = f.int("stickNumber")
            if (day == null || number == null || number < 1) {
                skipped()
                continue
            }
            val owner = f.uuid("profileId")?.let { profileRemap[it] ?: it }
            // 每人每天一签
            if (!keys.add(key(day.rawValue, owner?.lowercase()))) {
                skipped()
                continue
            }
            added += StickDraw(day.rawValue, owner, number, f.date("date") ?: day.noon.toEpochMilli())
            added(BackupCategory.STICK_DRAWS)
        }
        stickDraws = old + added
    }

    private fun tarotReadings(items: List<BackupFields>) {
        val old = existing(Store.tarotReadings.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val added = mutableListOf<TarotReading>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            if (!ids.add(id)) {
                skipped()
                continue
            }
            added += TarotReading(
                id = id,
                date = f.date("date") ?: now,
                spread = f.string("spread") ?: "",
                cards = f.string("cards") ?: "",
                question = f.string("question") ?: "",
            )
            added(BackupCategory.TAROT_READINGS)
        }
        tarotReadings = (old + added).sortedBy { it.date }
    }

    private fun moods(items: List<BackupFields>) {
        val old = existing(Store.moods.items)
        val days = old.map { it.dayKey }.toHashSet()
        val added = mutableListOf<MoodEntry>()
        for (f in items) {
            // 每天一条
            val day = f.dayKey("dayKey")
            if (day == null || !days.add(day.rawValue)) {
                skipped()
                continue
            }
            added += MoodEntry(
                dayKey = day.rawValue,
                mood = (f.int("mood") ?: 3).coerceIn(1, 5),
                note = f.string("note") ?: "",
                tags = f.strings("tags") ?: emptyList(),
                date = f.date("date") ?: day.noon.toEpochMilli(),
            )
            added(BackupCategory.MOODS)
        }
        moods = old + added
    }

    /** 倒数日的内容特征：重复的日子只看月日（农历看农历月日），不重复的看完整日期 */
    private fun countdownSignature(title: String, kind: String, day: DayKey, repeats: Boolean, isLunar: Boolean, lunarMonth: Int, lunarDay: Int): String {
        val anniversary = CountdownKind.ANNIVERSARY.key
        val repeating = (repeats || kind == CountdownKind.BIRTHDAY.key) && kind != anniversary
        val whenText = if (isLunar && kind != anniversary) {
            if (repeating) "L$lunarMonth-$lunarDay" else "L$lunarMonth-$lunarDay@${day.rawValue}"
        } else {
            if (repeating) "${day.month}-${day.day}" else "${day.rawValue}"
        }
        return signature(listOf(kind, title, whenText))
    }

    private fun countdowns(items: List<BackupFields>) {
        val old = existing(Store.countdowns.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val contents = old.map {
            countdownSignature(it.title, it.kind, DayKey.fromRaw(it.date) ?: it.createdDay, it.repeatsYearly, it.isLunar, it.lunarMonth, it.lunarDay)
        }.toHashSet()
        val added = mutableListOf<Countdown>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            val title = f.string("title") ?: ""
            val kindKey = f.string("kind")
            val kind = CountdownKind.entries.firstOrNull { it.key == kindKey }?.key ?: CountdownKind.COUNTDOWN.key
            val createdAt = f.date("createdAt") ?: now
            val day = f.dayKey("date") ?: DayKey.of(Instant.ofEpochMilli(createdAt))
            val repeats = f.bool("repeatsYearly") ?: false
            val isLunar = f.bool("isLunar") ?: false
            val lunarMonth = (f.int("lunarMonth") ?: 1).coerceIn(1, 12)
            val lunarDay = (f.int("lunarDay") ?: 1).coerceIn(1, 30)
            val content = countdownSignature(title, kind, day, repeats, isLunar, lunarMonth, lunarDay)
            if (id in ids || content in contents) {
                skipped()
                continue
            }
            ids += id
            contents += content
            added += Countdown(
                id = id,
                title = title,
                emoji = f.string("emoji") ?: "🎉",
                date = day.rawValue,
                kind = kind,
                repeatsYearly = repeats,
                isLunar = isLunar,
                lunarMonth = lunarMonth,
                lunarDay = lunarDay,
                colorIndex = f.int("colorIndex") ?: 0,
                pinned = f.bool("pinned") ?: false,
                createdAt = createdAt,
            )
            added(BackupCategory.COUNTDOWNS)
        }
        countdowns = old + added
    }

    private fun wheelTemplates(items: List<BackupFields>) {
        val old = existing(Store.wheels.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val contents = old.map { w -> signature(listOf(w.name) + w.options.map { it.title }) }.toHashSet()
        val added = mutableListOf<WheelTemplate>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            val name = f.string("name") ?: ""
            val options = f.objects("options")
            val content = signature(listOf(name) + options.map { it.string("title") ?: "" })
            if (id in ids || content in contents) {
                skipped()
                continue
            }
            ids += id
            contents += content
            added += WheelTemplate(
                id = id,
                name = name,
                removeAfterSpin = f.bool("removeAfterSpin") ?: false,
                options = options.map { o ->
                    WheelOption(id = o.uuid("id") ?: newId(), title = o.string("title") ?: "", weight = weight(o.number("weight")))
                },
                createdAt = f.date("createdAt") ?: now,
            )
            added(BackupCategory.WHEEL_TEMPLATES)
        }
        wheels = old + added
    }

    private fun foodMenus(items: List<BackupFields>) {
        val old = existing(Store.foodMenus.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val contents = old.map { m -> signature(listOf(m.name) + m.items.map { it.name }) }.toHashSet()
        val added = mutableListOf<FoodMenu>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            val name = f.string("name") ?: ""
            val entries = f.objects("items")
            val content = signature(listOf(name) + entries.map { it.string("name") ?: "" })
            if (id in ids || content in contents) {
                skipped()
                continue
            }
            ids += id
            contents += content
            added += FoodMenu(
                id = id,
                name = name,
                icon = f.string("icon") ?: "🍱",
                items = entries.map { e ->
                    FoodMenuItem(id = e.uuid("id") ?: newId(), name = e.string("name") ?: "", note = e.string("note") ?: "", weight = weight(e.number("weight")))
                },
                createdAt = f.date("createdAt") ?: now,
            )
            added(BackupCategory.FOOD_MENUS)
        }
        foodMenus = old + added
    }

    private fun foodLogs(items: List<BackupFields>) {
        fun key(name: String, source: String, date: Long) = signature(listOf(name, source, (date / 1000).toString()))
        val old = existing(Store.foodLogs.items)
        val keys = old.map { key(it.name, it.source, it.date) }.toHashSet()
        val added = mutableListOf<FoodLog>()
        for (f in items) {
            val name = f.string("name")
            if (name.isNullOrEmpty()) {
                skipped()
                continue
            }
            val source = f.string("source") ?: ""
            val date = f.date("date") ?: now
            if (!keys.add(key(name, source, date))) {
                skipped()
                continue
            }
            added += FoodLog(name, source, date)
            added(BackupCategory.FOOD_LOGS)
        }
        foodLogs = (old + added).sortedBy { it.date }
    }

    private fun blockedFoods(items: List<BackupFields>) {
        val old = existing(Store.blockedFoods.items)
        val names = old.map { it.name }.toHashSet()
        val added = mutableListOf<BlockedFood>()
        for (f in items) {
            val name = f.string("name")
            if (name.isNullOrEmpty() || !names.add(name)) {
                skipped()
                continue
            }
            added += BlockedFood(name, f.date("date") ?: now)
            added(BackupCategory.BLOCKED_FOODS)
        }
        blockedFoods = old + added
    }

    private fun lotteryPools(items: List<BackupFields>) {
        val old = existing(Store.lotteryPools.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val contents = old.map { p -> signature(listOf(p.name) + p.items.map { it.name }) }.toHashSet()
        val added = mutableListOf<LotteryPool>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            val name = f.string("name") ?: ""
            val entries = f.objects("items")
            val content = signature(listOf(name) + entries.map { it.string("name") ?: "" })
            if (id in ids || content in contents) {
                skipped()
                continue
            }
            ids += id
            contents += content
            added += LotteryPool(
                id = id,
                name = name,
                icon = f.string("icon") ?: "🎁",
                removeAfterDraw = f.bool("removeAfterDraw") ?: true,
                items = entries.map { e ->
                    val quantity = maxOf(e.int("quantity") ?: 1, 0)
                    LotteryItem(
                        id = e.uuid("id") ?: newId(),
                        name = e.string("name") ?: "",
                        weight = weight(e.number("weight")),
                        quantity = quantity,
                        remaining = (e.int("remaining") ?: quantity).coerceIn(0, quantity),
                    )
                },
                createdAt = f.date("createdAt") ?: now,
            )
            added(BackupCategory.LOTTERY_POOLS)
        }
        lotteryPools = old + added
    }

    private fun rosters(items: List<BackupFields>) {
        val old = existing(Store.rosters.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val contents = old.map { signature(listOf(it.name) + it.members) }.toHashSet()
        val added = mutableListOf<Roster>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            val name = f.string("name") ?: ""
            val members = f.strings("members") ?: emptyList()
            val content = signature(listOf(name) + members)
            if (id in ids || content in contents) {
                skipped()
                continue
            }
            ids += id
            contents += content
            added += Roster(id = id, name = name, members = members, createdAt = f.date("createdAt") ?: now)
            added(BackupCategory.ROSTERS)
        }
        rosters = old + added
    }

    private fun customPrompts(items: List<BackupFields>) {
        val old = existing(Store.customPrompts.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val contents = old.map { signature(listOf(it.kind, it.text)) }.toHashSet()
        val added = mutableListOf<CustomPrompt>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            val kind = if (f.string("kind") == "dare") "dare" else "truth"
            val text = f.string("text")
            if (text.isNullOrEmpty()) {
                skipped()
                continue
            }
            val content = signature(listOf(kind, text))
            if (id in ids || content in contents) {
                skipped()
                continue
            }
            ids += id
            contents += content
            added += CustomPrompt(id = id, kind = kind, text = text, level = (f.int("level") ?: 0).coerceIn(0, 2), createdAt = f.date("createdAt") ?: now)
            added(BackupCategory.CUSTOM_PROMPTS)
        }
        customPrompts = old + added
    }

    private fun scratchPresets(items: List<BackupFields>) {
        val old = existing(Store.scratchPresets.items)
        val ids = old.map { it.id.lowercase() }.toHashSet()
        val contents = old.map { signature(listOf(it.name) + it.prizes) }.toHashSet()
        val added = mutableListOf<ScratchPreset>()
        for (f in items) {
            val id = f.uuid("id") ?: newId()
            val name = f.string("name") ?: ""
            val prizes = f.strings("prizes") ?: emptyList()
            val content = signature(listOf(name) + prizes)
            if (id in ids || content in contents) {
                skipped()
                continue
            }
            ids += id
            contents += content
            added += ScratchPreset(id = id, name = name, prizes = prizes, blankWeight = weight(f.number("blankWeight")), createdAt = f.date("createdAt") ?: now)
            added(BackupCategory.SCRATCH_PRESETS)
        }
        scratchPresets = old + added
    }
}

// endregion
