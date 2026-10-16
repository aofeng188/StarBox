package com.tianaofeng.xingxia.system

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tianaofeng.xingxia.MainActivity
import com.tianaofeng.xingxia.R
import com.tianaofeng.xingxia.core.DayKey
import com.tianaofeng.xingxia.core.FortuneEngine
import com.tianaofeng.xingxia.core.SkyEvent
import com.tianaofeng.xingxia.core.SkyEvents
import com.tianaofeng.xingxia.data.AppSettings
import com.tianaofeng.xingxia.data.Store
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 每日运势提醒。每天的运势内容不同，所以每次只排下一次提醒：
 * 到点时由 [ReminderReceiver] 现算当天运势发通知，再排第二天。
 */
object ReminderScheduler {
    private const val CHANNEL_ID = "daily-fortune"
    private const val NOTIFICATION_ID = 1001

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** 重新排下一次提醒（设置变化、开机、提醒触发后调用） */
    fun reschedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = pendingIntent(context)
        alarm.cancel(intent)
        if (!AppSettings.dailyReminderEnabled) return
        val time = LocalTime.of(AppSettings.reminderHour, AppSettings.reminderMinute)
        var next = LocalDateTime.of(LocalDate.now(), time)
        if (!next.isAfter(LocalDateTime.now())) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // 不需要精确闹钟权限；系统可能为省电推迟几分钟
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent)
    }

    /**
     * 每天零点过 5 秒刷新一次小组件，并在收到后排下一天。
     * DATE_CHANGED 在 Android 8+ 不再发给静态 receiver；用非唤醒闹钟，设备醒着或下次亮屏时触发即可。
     * 用 setWindow 给出 10 分钟窗口：setAndAllowWhileIdle 的默认窗口在 Android 8–11 上可达距触发时间的 3/4，
     * 白天排的闹钟可能拖到凌晨好几点才响。窗口内的闹钟不需要精确闹钟权限。
     */
    fun scheduleDayChange(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, DayChangeReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val next = LocalDate.now().plusDays(1).atTime(0, 0, 5)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarm.setWindow(AlarmManager.RTC, millis, 10 * 60 * 1000L, intent)
    }

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "每日运势", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "每天定时推送当天的运势"
                },
            )
        }
    }

    /** 发出今天的运势通知 */
    fun showToday(context: Context) {
        if (!hasPermission(context)) return
        ensureChannel(context)
        val day = DayKey.today
        val fortune = FortuneEngine.daily(Store.birth, day)
        val name = Store.primary?.name
        val open = PendingIntent.getActivity(
            context, 1,
            Intent(Intent.ACTION_VIEW, Uri.parse("xingxia://fortune"), context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = "${fortune.summary}\n宜 ${fortune.funYi.joinToString("、")}  忌 ${fortune.funJi.joinToString("、")}"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${day.month}月${day.day}日 · ${fortune.levelName} ${fortune.score}分")
            .setSubText(if (!name.isNullOrEmpty()) "${name}的今日运势" else null)
            .setContentText(fortune.summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 用户撤销了通知权限
        }
    }
}

/**
 * 天象提醒：满月、新月、流星雨极大的前一天晚上 20:00（本地时间），与每日运势提醒互不影响。
 * 标题、正文和提醒时间与 iOS 的 NotificationScheduler 相同。和每日提醒一样只排下一次：
 * 到点时由 [SkyReminderReceiver] 发出当天该提醒的天象，再排下一次。
 */
object SkyReminders {
    private const val CHANNEL_ID = "sky-events"
    private const val REQUEST_CODE = 3
    private const val EXTRA_DAY = "remindDay"
    private val KINDS = setOf(SkyEvent.Kind.NEW_MOON, SkyEvent.Kind.FULL_MOON, SkyEvent.Kind.METEOR_SHOWER)

    /** 一条提醒：remindDay 是天象前一天，当晚 20:00 提醒 */
    class Item(val event: SkyEvent, val remindDay: DayKey, val title: String, val body: String) {
        val fireAt: LocalDateTime get() = remindDay.localDate.atTime(20, 0)
    }

    /** 从 today 起未来 30 天内要发的提醒（含今晚已经过点的，由调用方按时间筛） */
    fun items(today: DayKey = DayKey.today): List<Item> {
        val events = SkyEvents.upcoming(today, 31)
        return events.filter { it.kind in KINDS }.mapNotNull { event ->
            val remindDay = event.day.adding(-1)
            if (today.daysTo(remindDay) !in 0 until 30) return@mapNotNull null
            val title = "明天${event.title}" + if (event.kind == SkyEvent.Kind.METEOR_SHOWER) "极大" else ""
            // 同一天有日月食时一并提醒
            val eclipses = events.filter { it.kind == SkyEvent.Kind.ECLIPSE && it.day == event.day }
            val body = event.detail + eclipses.joinToString("") { "\n另有${it.title}：${it.detail}" }
            Item(event, remindDay, title, body)
        }
    }

    private fun pendingIntent(context: Context, remindDay: DayKey?): PendingIntent {
        val intent = Intent(context, SkyReminderReceiver::class.java)
        if (remindDay != null) intent.putExtra(EXTRA_DAY, remindDay.rawValue)
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** 重新排下一次天象提醒（开关变化、开机、提醒触发后调用） */
    fun reschedule(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context, null))
        if (!AppSettings.skyReminders) return
        val now = LocalDateTime.now()
        val next = items().filter { it.fireAt.isAfter(now) }.minByOrNull { it.fireAt } ?: return
        val millis = next.fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent(context, next.remindDay))
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "天象提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "满月、新月和流星雨极大的前一天晚上提醒"
                },
            )
        }
    }

    /** 发出 remindDay 当晚的天象提醒。过了那一天才收到（例如设备关机）就不发，免得「明天」说错 */
    fun show(context: Context, remindDayRaw: Int) {
        if (!AppSettings.skyReminders || !ReminderScheduler.hasPermission(context)) return
        val today = DayKey.today
        if (today.rawValue != remindDayRaw) return
        val due = items(today).filter { it.remindDay == today }
        if (due.isEmpty()) return
        ensureChannel(context)
        for (item in due) {
            val code = item.event.id.hashCode()
            val open = PendingIntent.getActivity(
                context, code,
                Intent(Intent.ACTION_VIEW, Uri.parse("xingxia://sky"), context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(item.title)
                .setContentText(item.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.body))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(code, notification)
            } catch (_: SecurityException) {
                // 用户撤销了通知权限
            }
        }
    }

    internal fun remindDay(intent: Intent): Int = intent.getIntExtra(EXTRA_DAY, 0)
}

class SkyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SkyReminders.show(context, SkyReminders.remindDay(intent))
        SkyReminders.reschedule(context)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.showToday(context)
        ReminderScheduler.reschedule(context)
    }
}

/** 开机、时区或时间变化后重新排提醒和跨天刷新，并刷新小组件 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.reschedule(context)
        SkyReminders.reschedule(context)
        ReminderScheduler.scheduleDayChange(context)
        val pending = goAsync()
        Widgets.updateAll(context) { pending.finish() }
    }
}

/** 过了零点：刷新小组件（运势、星座、倒数日都换成新的一天），再排下一天 */
class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderScheduler.scheduleDayChange(context)
        val pending = goAsync()
        Widgets.updateAll(context) { pending.finish() }
    }
}
