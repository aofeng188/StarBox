package com.tianaofeng.xingxia.data

import com.tianaofeng.xingxia.core.CountdownKind
import com.tianaofeng.xingxia.core.CountdownSpec
import com.tianaofeng.xingxia.core.CountdownStatus
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.ProfileBirthdays

/**
 * 倒数日列表、首页倒数日条和小组件里的一项：Store 里的倒数日，
 * 或由档案生日生成的「虚拟倒数日」（不入库、不可编辑删除）。与 iOS 的 CountdownItem 一致。
 */
data class CountdownItem(
    val id: String,
    val title: String,
    val emoji: String,
    val colorIndex: Int,
    val pinned: Boolean,
    val spec: CountdownSpec,
    /** 由档案生日生成时为档案的 id */
    val profileId: String? = null,
) {
    val kind: CountdownKind get() = spec.kind
    val isFromProfile: Boolean get() = profileId != null

    fun status(today: DayKey = DayKey.today): CountdownStatus = spec.status(today)

    companion object {
        /**
         * 档案生日生成的倒数日：标题「名字的生日」，农历录入的档案按农历生日每年重复。
         * 已经手动建了同一人的生日倒数（kind 为生日、标题包含名字）的档案跳过。
         */
        fun profileBirthdays(profiles: List<Profile>, countdowns: List<Countdown>): List<CountdownItem> {
            val manual = countdowns.filter { it.kindValue == CountdownKind.BIRTHDAY }.map { it.title }
            return profiles.mapNotNull { profile ->
                if (ProfileBirthdays.shouldSkip(profile.name, manual)) return@mapNotNull null
                CountdownItem(
                    id = profile.id,
                    title = ProfileBirthdays.title(profile.name),
                    emoji = ProfileBirthdays.EMOJI,
                    colorIndex = profile.colorIndex,
                    pinned = false,
                    spec = ProfileBirthdays.spec(profile.birthdayKey, profile.enteredAsLunar),
                    profileId = profile.id,
                )
            }
        }

        /** 倒数日 + （开关打开时）档案生日 */
        fun all(countdowns: List<Countdown>, profiles: List<Profile>, includeProfiles: Boolean): List<CountdownItem> =
            countdowns.map { it.item } + if (includeProfiles) profileBirthdays(profiles, countdowns) else emptyList()

        /** 当前的全部项：倒数日按创建时间，档案按档案顺序（App 和小组件共用） */
        fun current(): List<CountdownItem> =
            all(Store.countdowns.items.sortedBy { it.createdAt }, Store.sortedProfiles, AppSettings.profileBirthdaysInCountdowns)

        /** 列表的排序：置顶在前；然后按剩余天数，纪念日和已过的倒数日排在后面 */
        fun sorted(items: List<CountdownItem>, today: DayKey = DayKey.today): List<CountdownItem> = items
            .map { item ->
                val status = item.status(today)
                item to if (item.kind == CountdownKind.ANNIVERSARY || status.isPast) 10_000 + status.days else status.days
            }
            .sortedWith(compareBy<Pair<CountdownItem, Int>> { !it.first.pinned }.thenBy { it.second })
            .map { it.first }

        /** 最近的一个还没到的倒数日或生日（不算纪念日和已过的） */
        fun nearest(items: List<CountdownItem>, today: DayKey = DayKey.today): Pair<CountdownItem, CountdownStatus>? = items
            .filter { it.kind != CountdownKind.ANNIVERSARY }
            .map { it to it.status(today) }
            .filter { !it.second.isPast }
            .minByOrNull { it.second.days }
    }
}

val Countdown.item: CountdownItem
    get() = CountdownItem(id, title, emoji, colorIndex, pinned, spec, null)
