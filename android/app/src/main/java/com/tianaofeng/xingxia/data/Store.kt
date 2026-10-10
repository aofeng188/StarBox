package com.tianaofeng.xingxia.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tianaofeng.xingxia.core.BirthInfo
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.ZodiacSign
import com.tianaofeng.xingxia.core.BaZi
import com.tianaofeng.xingxia.core.EarthlyBranch
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
)

/** 每日一签 */
@Serializable
data class StickDraw(val dayKey: Int, val profileId: String?, val stickNumber: Int, val date: Long = System.currentTimeMillis())

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
    val wheels = PersistedList("wheelTemplates", WheelTemplate.serializer())
    val foodLogs = PersistedList("foodLogs", FoodLog.serializer())
    val blockedFoods = PersistedList("blockedFoods", BlockedFood.serializer())
    val rosters = PersistedList("rosters", Roster.serializer())

    private val primaryId by lazy { Prefs.string("primaryProfileId", "") }

    fun init(context: Context) {
        Prefs.init(context)
        listOf(profiles, records, stickDraws, wheels, foodLogs, blockedFoods, rosters).forEach { it.load() }
        SeedData.insertIfNeeded()
    }

    val sortedProfiles: List<Profile> get() = profiles.items.sortedBy { it.sortIndex }

    /** 当前查看的档案 */
    val primary: Profile? get() = profiles.items.firstOrNull { it.id == primaryId.value }

    val birth: BirthInfo? get() = primary?.birth

    fun setPrimary(profile: Profile?) {
        primaryId.value = profile?.id ?: ""
    }

    fun saveProfile(profile: Profile) {
        profiles.update { list ->
            if (list.any { it.id == profile.id }) list.map { if (it.id == profile.id) profile else it } else list + profile
        }
    }

    fun deleteProfile(profile: Profile) {
        val wasPrimary = primary?.id == profile.id
        profiles.update { list -> list.filter { it.id != profile.id } }
        if (wasPrimary) setPrimary(sortedProfiles.firstOrNull())
    }

    /** 往「决策历史」里记一笔 */
    fun log(tool: String, title: String, result: String, detail: String? = null) {
        records.update { (it + DecisionRecord(tool = tool, title = title, result = result, detail = detail)).takeLast(2000) }
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
        if (version >= 1) return
        version = 1

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
}
