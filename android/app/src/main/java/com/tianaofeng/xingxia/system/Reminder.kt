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
        alarm.setAndAllowWhileIdle(AlarmManager.RTC, millis, intent)
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
