package com.tianaofeng.xingxia.data

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tianaofeng.xingxia.core.BirthInfo
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.JournalStats
import com.tianaofeng.xingxia.core.MoonInfo
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.core.BaZi
import com.tianaofeng.xingxia.core.EarthlyBranch
import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.CountdownSpec
import com.tianaofeng.xingxia.core.CountdownStatus
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.UUID

private fun newId() = UUID.randomUUID().toString()

// region 数据模型（对应 iOS 版的 SwiftData 模型）

@Serializable
data class Profile(
    val id: String = newId(),
    val name: String = "",
    val relation: String = "自己",
    val avatar: String = "🌟",
    val colorIndex: Int = 0,
    /** 公历生日，yyyyMMdd */
    val birthday: Int = 19980618,
    val hasBirthTime: Boolean = false,
    val birthHour: Int = 12,
    val birthMinute: Int = 0,
    val birthCityId: String? = null,
    /** 录入时是否使用农历（仅用于展示） */
    val enteredAsLunar: Boolean = false,
    val sortIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val birthdayKey: DayKey get() = DayKey.fromRaw(birthday) ?: DayKey(1998, 6, 18)

    val birth: BirthInfo
        get() = BirthInfo(
            birthday = birthdayKey,
            hour = if (hasBirthTime) birthHour else null,
            minute = if (hasBirthTime) birthMinute else null,
            cityId = birthCityId,
        )

    val sunSign: ZodiacSign get() = ZodiacSign.sunSign(birth)
    val animal: EarthlyBranch get() = BaZi(birth).zodiacAnimal
}

/** 所有随机工具的结果记录 */
@Serializable
data class DecisionRecord(
    val id: String = newId(),
    val tool: String,
    val title: String,
    val result: String,
    val detail: String? = null,
    val date: Long = System.currentTimeMillis(),
    /** 「问一问」里当时在问的问题（Store.currentQuestion），没有就是 null */
    val question: String? = null,
    /** null 没表态，true「就这么办」，false「再想想」 */
    val adopted: Boolean? = null,
    /** 回访结果：1 满意，0 一般，-1 后悔；null 还没回访 */
    val satisfaction: Int? = null,
    /** 回访（含「跳过」）的时间（毫秒）；有值就不再出现在首页回访卡里 */
    val followedUpAt: Long? = null,
) {
    /** 当时在问的问题：question；没有时硬币、答案之书的 title 就是用户写的问题（与 iOS 的 askedQuestion 一致） */
    val askedQuestion: String? get() = JournalStats.askedQuestion(tool, title, question)
}

/** 每日一签 */
@Serializable
data class StickDraw(val dayKey: Int, val profileId: String?, val stickNumber: Int, val date: Long = System.currentTimeMillis())

/** 塔罗手记：一次塔罗解读。cards 与 iOS 相同，如 "0u,13r,5u"（牌 id + u 正位 / r 逆位） */
@Serializable
data class TarotReading(
    val id: String = newId(),
    val date: Long = System.currentTimeMillis(),
    val spread: String,
    val cards: String,
    val question: String = "",
)

@Serializable
data class WheelOption(val id: String = newId(), val title: String, val weight: Double = 1.0)

@Serializable
data class WheelTemplate(
    val id: String = newId(),
    val name: String,
    val removeAfterSpin: Boolean = false,
    val options: List<WheelOption> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

/** 「就吃这个」的记录，用于智能去重 */
@Serializable
data class FoodLog(val name: String, val source: String, val date: Long = System.currentTimeMillis())

@Serializable
data class BlockedFood(val name: String, val date: Long = System.currentTimeMillis())

@Serializable
data class Roster(val id: String = newId(), val name: String, val members: List<String>, val createdAt: Long = System.currentTimeMillis())

@Serializable
data class MoodEntry(
    val dayKey: Int,
    /** 1…5 */
    val mood: Int,
    val note: String = "",
    val tags: List<String> = emptyList(),
    val date: Long = System.currentTimeMillis(),
)

/** 每日仪式：某天看过（或跳过）了仪式。每天最多一条，月相徽章按完成的日子统计 */
@Serializable
data class RitualDay(
    /** yyyyMMdd */
    val dayKey: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val skipped: Boolean = false,
    /** MoonPhaseName 的序号 0…7（与 iOS 的 rawValue 相同） */
    val moonPhase: Int = 0,
)

@Serializable
data class Countdown(
    val id: String = newId(),
    val title: String,
    val emoji: String = "🎉",
    /** 公历日期 yyyyMMdd（农历倒数日中仅生日用来参考出生年份） */
    val date: Int,
    /** "countdown" 倒数日 / "anniversary" 纪念日 / "birthday" 生日 */
    val kind: String = "countdown",
    val repeatsYearly: Boolean = false,
    val isLunar: Boolean = false,
    val lunarMonth: Int = 1,
    val lunarDay: Int = 1,
    val colorIndex: Int = 0,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val kindValue: CountdownKind get() = CountdownKind.of(kind)

    /** 创建当天 */
    val createdDay: DayKey get() = DayKey.of(java.time.Instant.ofEpochMilli(createdAt))

    /**
     * 农历生日是否记录了出生年份（与 iOS 一致）。没记录时 date 存的是创建当天
     * （旧数据里没动过「出生年份参考」的也是这样），不显示年龄。
     */
    val knowsBirthYear: Boolean
        get() = !(isLunar && kindValue == CountdownKind.BIRTHDAY && date == createdDay.rawValue)

    val spec: CountdownSpec
        get() = CountdownSpec(
            kindValue, DayKey.fromRaw(date) ?: DayKey.today, repeatsYearly, isLunar, lunarMonth, lunarDay,
            showsAge = knowsBirthYear,
        )

    fun status(today: DayKey = DayKey.today): CountdownStatus = spec.status(today)
}

@Serializable
data class LotteryItem(
    val id: String = newId(),
    val name: String,
    val weight: Double = 1.0,
    val quantity: Int = 1,
    val remaining: Int = quantity,
)

@Serializable
data class LotteryPool(
    val id: String = newId(),
    val name: String,
    val icon: String = "🎁",
    val removeAfterDraw: Boolean = true,
    val items: List<LotteryItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class FoodMenuItem(val id: String = newId(), val name: String, val note: String = "", val weight: Double = 1.0)

@Serializable
data class FoodMenu(
    val id: String = newId(),
    val name: String,
    val icon: String = "🍱",
    val items: List<FoodMenuItem> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
)

/** 真心话大冒险的自定义题目 */
@Serializable
data class CustomPrompt(
    val id: String = newId(),
    /** "truth" / "dare" */
    val kind: String,
    val text: String,
    val level: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ScratchPreset(
    val id: String = newId(),
    val name: String,
    val prizes: List<String>,
    val blankWeight: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis(),
)

// endregion

/** 用 SharedPreferences 持久化的 Compose 状态：读写都会触发重组。 */
private class PrefState<T>(initial: T, private val write: (T) -> Unit) : MutableState<T> {
    private val state = mutableStateOf(initial)
    override var value: T
        get() = state.value
        set(newValue) {
            if (newValue != state.value) {
                state.value = newValue
                write(newValue)
            }
        }

    override fun component1(): T = value
    override fun component2(): (T) -> Unit = { value = it }
}

/** 简单的键值偏好设置。同一个 key 共享同一个状态对象。 */
object Prefs {
    private lateinit var sp: SharedPreferences
    private val cache = HashMap<String, MutableState<*>>()

    fun init(context: Context) {
        sp = context.getSharedPreferences("xingxia", Context.MODE_PRIVATE)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> cached(key: String, create: () -> MutableState<T>): MutableState<T> =
        cache.getOrPut(key, create) as MutableState<T>

    fun bool(key: String, default: Boolean): MutableState<Boolean> =
        cached(key) { PrefState(sp.getBoolean(key, default)) { sp.edit().putBoolean(key, it).apply() } }

    fun int(key: String, default: Int): MutableState<Int> =
        cached(key) { PrefState(sp.getInt(key, default)) { sp.edit().putInt(key, it).apply() } }

    fun long(key: String, default: Long): MutableState<Long> =
        cached(key) { PrefState(sp.getLong(key, default)) { sp.edit().putLong(key, it).apply() } }

    fun string(key: String, default: String): MutableState<String> =
        cached(key) { PrefState(sp.getString(key, default) ?: default) { sp.edit().putString(key, it).apply() } }

    fun stringList(key: String, default: List<String>): MutableState<List<String>> = cached(key) {
        val stored = sp.getString(key, null)?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
        PrefState(stored ?: default) { sp.edit().putString(key, json.encodeToString(ListSerializer(String.serializer()), it)).apply() }
    }

    internal fun readRaw(key: String): String? = sp.getString(key, null)
    internal fun writeRaw(key: String, value: String) = sp.edit().putString(key, value).apply()

    internal val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
}

/** 一个持久化的列表（整体存成一段 JSON）。 */
class PersistedList<T>(private val key: String, private val serializer: KSerializer<T>) {
    var items: List<T> by mutableStateOf(emptyList())
        private set

    internal fun load() {
        items = Prefs.readRaw(key)?.let {
            runCatching { Prefs.json.decodeFromString(ListSerializer(serializer), it) }.getOrNull()
        } ?: emptyList()
    }

    fun update(transform: (List<T>) -> List<T>) {
        items = transform(items)
        Prefs.writeRaw(key, Prefs.json.encodeToString(ListSerializer(serializer), items))
    }

    fun add(item: T) = update { it + item }
}

/** App 的全部本地数据。 */
object Store {
    val profiles = PersistedList("profiles", Profile.serializer())
    val records = PersistedList("decisionRecords", DecisionRecord.serializer())
    val stickDraws = PersistedList("stickDraws", StickDraw.serializer())
    val tarotReadings = PersistedList("tarotReadings", TarotReading.serializer())
    val wheels = PersistedList("wheelTemplates", WheelTemplate.serializer())
    val foodLogs = PersistedList("foodLogs", FoodLog.serializer())
    val blockedFoods = PersistedList("blockedFoods", BlockedFood.serializer())
    val rosters = PersistedList("rosters", Roster.serializer())
    val moods = PersistedList("moodEntries", MoodEntry.serializer())
    val countdowns = PersistedList("countdowns", Countdown.serializer())
    val lotteryPools = PersistedList("lotteryPools", LotteryPool.serializer())
    val foodMenus = PersistedList("foodMenus", FoodMenu.serializer())
    val customPrompts = PersistedList("customPrompts", CustomPrompt.serializer())
    val scratchPresets = PersistedList("scratchPresets", ScratchPreset.serializer())
    val ritualDays = PersistedList("ritualDays", RitualDay.serializer())

    private val all get() = listOf(
        profiles, records, stickDraws, tarotReadings, wheels, foodLogs, blockedFoods, rosters,
        moods, countdowns, lotteryPools, foodMenus, customPrompts, scratchPresets, ritualDays,
    )

    private val primaryId by lazy { Prefs.string("primaryProfileId", "") }

    /** 「问一问」里正在问的问题：进入新问题时覆盖，清空问题框时置 null。记决策时一起记进 question */
    var currentQuestion by mutableStateOf<String?>(null)

    /** 刚记下的那条决定（App 在前台时），根布局据此弹出「就这么办 / 再想想」；收起时置 null */
    var lastDecision by mutableStateOf<DecisionRecord?>(null)

    /**
     * 最近一次从深链接（小组件、通知、快捷方式）打开页面的时间（SystemClock.elapsedRealtime 毫秒）：
     * 每日仪式这次不盖住用户点进来的页面（与 iOS 的 AppState.lastDeepLinkAt 一致）
     */
    var lastDeepLinkAt by mutableStateOf<Long?>(null)
        private set

    /** MainActivity 收到能识别的深链接时调用 */
    fun markDeepLink() {
        lastDeepLinkAt = SystemClock.elapsedRealtime()
    }

    private var startedActivities = 0
    private var tracksForeground = false

    /** App 有界面在前台。小组件、通知里记的决定不弹浮层，也不带问题 */
    val isInForeground: Boolean get() = startedActivities > 0

    fun init(context: Context) {
        Prefs.init(context)
        all.forEach { it.load() }
        SeedData.insertIfNeeded()
        trackForeground(context)
    }

    private fun trackForeground(context: Context) {
        if (tracksForeground) return
        val app = context.applicationContext as? Application ?: return
        tracksForeground = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivities += 1
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivities = maxOf(0, startedActivities - 1)
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    val sortedProfiles: List<Profile> get() = profiles.items.sortedBy { it.sortIndex }

    /** 当前查看的档案 */
    val primary: Profile? get() = profiles.items.firstOrNull { it.id == primaryId.value }

    val birth: BirthInfo? get() = primary?.birth

    fun setPrimary(profile: Profile?) {
        primaryId.value = profile?.id ?: ""
        onPrimaryChanged()
    }

    /** 当前档案或倒数日变化时由 App 注册：刷新小组件、重排每日提醒 */
    var onDataChanged: () -> Unit = {}

    private fun onPrimaryChanged() = onDataChanged()

    fun todayMood(today: DayKey = DayKey.today): MoodEntry? = moods.items.lastOrNull { it.dayKey == today.rawValue }

    fun setMood(value: Int, today: DayKey = DayKey.today) {
        moods.update { list ->
            val existing = list.lastOrNull { it.dayKey == today.rawValue }
            if (existing != null) list.map { if (it.dayKey == today.rawValue) it.copy(mood = value, date = System.currentTimeMillis()) else it }
            else list + MoodEntry(today.rawValue, value)
        }
    }

    fun saveCountdown(countdown: Countdown) {
        countdowns.update { list ->
            if (list.any { it.id == countdown.id }) list.map { if (it.id == countdown.id) countdown else it } else list + countdown
        }
        onDataChanged()
    }

    fun deleteCountdown(id: String) {
        countdowns.update { list -> list.filter { it.id != id } }
        onDataChanged()
    }

    fun saveProfile(profile: Profile) {
        profiles.update { list ->
            if (list.any { it.id == profile.id }) list.map { if (it.id == profile.id) profile else it } else list + profile
        }
        onDataChanged()
    }

    fun deleteProfile(profile: Profile) {
        val wasPrimary = primary?.id == profile.id
        profiles.update { list -> list.filter { it.id != profile.id } }
        if (wasPrimary) setPrimary(sortedProfiles.firstOrNull()) else onDataChanged()
    }

    /**
     * 往「决策历史」里记一笔并返回这条记录。App 在前台时，「问一问」里正在问的问题（currentQuestion）一起记进 question，
     * 并把它设为 lastDecision，根布局据此弹出「就这么办 / 再想想」（与 iOS 的 HistoryLog.add 一致）
     */
    fun log(tool: String, title: String, result: String, detail: String? = null): DecisionRecord {
        val foreground = isInForeground
        val question = if (foreground) currentQuestion?.trim()?.takeIf { it.isNotEmpty() } else null
        val record = DecisionRecord(tool = tool, title = title, result = result, detail = detail, question = question)
        records.update { (it + record).takeLast(2000) }
        if (foreground) lastDecision = record
        return record
    }

    /** 「就这么办」(true) /「再想想」(false)。不改 lastDecision，浮层由调用方收起 */
    fun setAdopted(id: String, adopted: Boolean) = updateRecord(id) { it.copy(adopted = adopted) }

    /** 补记里选「还没决定」 */
    fun clearAdopted(id: String) = updateRecord(id) { it.copy(adopted = null) }

    /**
     * 回访：1 满意，0 一般，-1 后悔；null 表示「跳过」，只记回访时间。
     * 不管选了哪一项都会写 followedUpAt，这条记录之后不再出现在首页回访卡里
     */
    fun setSatisfaction(id: String, value: Int?) =
        updateRecord(id) { it.copy(satisfaction = value?.coerceIn(-1, 1), followedUpAt = System.currentTimeMillis()) }

    /** 记录已经删掉时什么都不做 */
    private fun updateRecord(id: String, transform: (DecisionRecord) -> DecisionRecord) {
        if (records.items.none { it.id == id }) return
        records.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    fun hasRitual(day: DayKey): Boolean = ritualDays.items.any { it.dayKey == day.rawValue }

    /** 记下今天的仪式。每天最多一条；完成过就一直算完成，跳过只会升级为完成，不会反过来 */
    fun recordRitual(day: DayKey, skipped: Boolean) {
        ritualDays.update { list ->
            val existing = list.firstOrNull { it.dayKey == day.rawValue }
            val entry = when {
                existing == null -> RitualDay(day.rawValue, skipped = skipped, moonPhase = MoonInfo.on(day).name.ordinal)
                existing.skipped && !skipped -> existing.copy(skipped = false, completedAt = System.currentTimeMillis())
                else -> existing
            }
            (list.filter { it.dayKey != day.rawValue } + entry).sortedBy { it.dayKey }
        }
    }

    fun greeting(): String {
        val hour = java.time.LocalTime.now().hour
        val base = when (hour) {
            in 5..8 -> "早上好"
            in 9..11 -> "上午好"
            in 12..13 -> "中午好"
            in 14..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深了"
        }
        val name = primary?.name
        return if (!name.isNullOrEmpty() && name != "我") "$base，$name" else base
    }
}

/** 首次启动时放入一些示例数据，让各个工具开箱即用。 */
private object SeedData {
    fun insertIfNeeded() {
        var version by Prefs.int("seedVersion", 0)
        if (version < 1) seedV1()
        if (version < 2) seedV2()
        version = 2
    }

    private fun seedV1() {
        val wheels = listOf(
            "今晚谁洗碗" to listOf("我", "你", "石头剪刀布决定", "一起洗", "明天再洗"),
            "周末去哪儿" to listOf("看电影", "逛公园", "爬山", "宅家追剧", "探店", "博物馆", "去海边"),
            "要不要做" to listOf("做！", "不做", "再想想", "问问朋友"),
            "奶茶选哪杯" to listOf("珍珠奶茶", "杨枝甘露", "柠檬茶", "芋泥波波", "椰椰拿铁", "葡萄冰"),
        )
        Store.wheels.update { existing ->
            existing + wheels.mapIndexed { i, (name, options) ->
                WheelTemplate(name = name, options = options.map { WheelOption(title = it) }, createdAt = System.currentTimeMillis() + i)
            }
        }
    }

    /** 第二阶段加入的工具的示例数据 */
    private fun seedV2() {
        Store.lotteryPools.update { existing ->
            existing + listOf(
                LotteryPool(
                    name = "年会抽奖", icon = "🎊", removeAfterDraw = true,
                    items = listOf(
                        LotteryItem(name = "特等奖 · 新款手机", quantity = 1),
                        LotteryItem(name = "一等奖 · 无线耳机", quantity = 2),
                        LotteryItem(name = "二等奖 · 购物卡", quantity = 5),
                        LotteryItem(name = "三等奖 · 奶茶券", quantity = 10),
                        LotteryItem(name = "参与奖 · 幸运红包", quantity = 20),
                    ),
                ),
                LotteryPool(
                    name = "今日小奖励", icon = "🎁", removeAfterDraw = false, createdAt = System.currentTimeMillis() + 1,
                    items = listOf("一杯奶茶", "看一集剧", "早睡一小时", "买束花", "散步半小时", "吃顿好的", "谢谢惠顾").map {
                        LotteryItem(name = it, weight = if (it == "谢谢惠顾") 2.0 else 1.0, quantity = 99)
                    },
                ),
            )
        }
        Store.foodMenus.update { existing ->
            existing + FoodMenu(
                name = "公司楼下", icon = "🏢",
                items = listOf("兰州拉面", "黄焖鸡米饭", "麻辣烫", "沙拉轻食", "日式便当", "螺蛳粉").map { FoodMenuItem(name = it) },
            )
        }
        Store.scratchPresets.update { existing ->
            existing + ScratchPreset(name = "今日惊喜", prizes = listOf("奶茶一杯 🧋", "免做家务一次 🧹", "电影之夜 🎬", "抱抱一下 🤗", "零食大礼包 🍫"))
        }
        val today = DayKey.today
        Store.countdowns.update { existing ->
            existing + listOf(
                Countdown(title = "元旦", emoji = "🎆", date = DayKey(today.year + 1, 1, 1).rawValue, repeatsYearly = true, colorIndex = 0),
                Countdown(
                    title = "春节", emoji = "🧧", date = today.rawValue, repeatsYearly = true, isLunar = true,
                    lunarMonth = 1, lunarDay = 1, pinned = true, colorIndex = 1, createdAt = System.currentTimeMillis() + 1,
                ),
            )
        }
    }
}
