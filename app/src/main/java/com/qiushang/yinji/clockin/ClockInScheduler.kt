package com.qiushang.yinji.clockin

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.qiushang.yinji.MainActivity
import com.qiushang.yinji.data.AppGraph
import com.qiushang.yinji.util.Dates
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 「上班打卡提醒」的闹钟调度与通知。
 *
 * 只用平台 API（AlarmManager / Notification / NotificationManager），不引 androidx.work 等新依赖。
 * 策略：每次只排「下一个」上班 / 下班提醒；响了之后 Receiver 再调 schedule() 排下一次，
 * 因此能自然跟随工作日、跨天、改时间、重启（BOOT_COMPLETED 会重排）。
 * 没有精确闹钟权限（用户在系统设置里关了）时退回 setAndAllowWhileIdle，只是不够准，不会崩。
 */
object ClockInScheduler {
    const val ACTION_ALARM_ON = "com.qiushang.yinji.action.ALARM_ON"
    const val ACTION_ALARM_OFF = "com.qiushang.yinji.action.ALARM_OFF"
    const val ACTION_MARK_ON = "com.qiushang.yinji.action.MARK_ON"
    const val ACTION_MARK_OFF = "com.qiushang.yinji.action.MARK_OFF"
    const val ACTION_SNOOZE = "com.qiushang.yinji.action.SNOOZE"

    const val EXTRA_TYPE = "type"   // "on" / "off"
    private const val CHANNEL_ID = "clock_in_reminder"
    private const val NOTIFICATION_ID = 1001

    private const val RC_ALARM_ON = 2101
    private const val RC_ALARM_OFF = 2102
    private const val RC_MARK_ON = 2103
    private const val RC_MARK_OFF = 2104
    private const val RC_SNOOZE = 2105

    private fun alarmIntent(context: Context, action: String, type: String? = null): Intent =
        Intent(action).setPackage(context.packageName).apply {
            if (type != null) putExtra(EXTRA_TYPE, type)
        }

    private fun broadcastPi(context: Context, rc: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context, rc, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    /** 计算下一个触发时刻：从今天起最多往后找 9 天，命中「工作日 + 上班/下班时间 - 提前量」。 */
    private fun nextTrigger(now: LocalDateTime, s: ClockInPrefs.Settings, type: String): LocalDateTime? {
        val weekdays = ClockInPrefs.weekdaySet(s)
        if (weekdays.isEmpty()) return null
        val raw = if (type == "on") s.onTime else s.offTime
        val parts = raw.split(":")
        val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        val minutes = (h * 60 + m - (if (type == "on") s.advanceMinutes else 0)).coerceIn(0, 1439)
        for (d in 0..9) {
            val date = now.toLocalDate().plusDays(d.toLong())
            if (date.dayOfWeek.value !in weekdays) continue
            val candidate = date.atStartOfDay().plusMinutes(minutes.toLong())
            if (candidate.isAfter(now)) return candidate
        }
        return null
    }

    private fun setAlarm(context: Context, rc: Int, action: String, type: String?, triggerAt: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = broadcastPi(context, rc, alarmIntent(context, action, type))
        if (!am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelAlarm(context: Context, rc: Int, action: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = broadcastPi(context, rc, alarmIntent(context, action))
        am.cancel(pi)
    }

    /** 排下一次上班 / 下班提醒。设置关闭时取消全部。 */
    fun schedule(context: Context) {
        val s = ClockInPrefs.get(context)
        if (!s.enabled) {
            cancel(context)
            return
        }
        val now = LocalDateTime.now()
        for (type in listOf("on", "off")) {
            val rc = if (type == "on") RC_ALARM_ON else RC_ALARM_OFF
            val action = if (type == "on") ACTION_ALARM_ON else ACTION_ALARM_OFF
            val t = nextTrigger(now, s, type)
            if (t == null) {
                cancelAlarm(context, rc, action)
            } else {
                val millis = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                setAlarm(context, rc, action, null, millis)
            }
        }
    }

    fun cancel(context: Context) {
        cancelAlarm(context, RC_ALARM_ON, ACTION_ALARM_ON)
        cancelAlarm(context, RC_ALARM_OFF, ACTION_ALARM_OFF)
        cancelNotification(context)
    }

    /** 通知里的「稍后提醒」：10 分钟后再响一次，响时 Receiver 会再排下一次周期。 */
    fun snooze(context: Context, type: String) {
        cancelNotification(context)
        val rc = if (type == "on") RC_ALARM_ON else RC_ALARM_OFF
        val action = if (type == "on") ACTION_ALARM_ON else ACTION_ALARM_OFF
        setAlarm(context, rc, action, null, System.currentTimeMillis() + 10 * 60 * 1000L)
    }

    private fun createChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(CHANNEL_ID, "打卡提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "上班 / 下班打卡提醒"
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun showNotification(context: Context, type: String) {
        createChannel(context)
        val s = ClockInPrefs.get(context)
        val isOn = type == "on"
        val title = if (isOn) "该打上班卡啦" else "该打下班卡啦"
        val clock = if (isOn) s.onTime else s.offTime
        val text = "$clock 了，别忘记打考勤 🌱"

        val openPi = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val markPi = broadcastPi(
            context, if (isOn) RC_MARK_ON else RC_MARK_OFF,
            alarmIntent(context, if (isOn) ACTION_MARK_ON else ACTION_MARK_OFF)
        )
        val snoozePi = broadcastPi(context, RC_SNOOZE, alarmIntent(context, ACTION_SNOOZE, type))
        val icon = Icon.createWithResource(context, android.R.drawable.ic_dialog_info)

        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(0xFF3A9A80.toInt())
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setContentIntent(openPi)
            .addAction(Notification.Action.Builder(icon, "已打卡", markPi).build())
            .addAction(Notification.Action.Builder(icon, "稍后提醒", snoozePi).build())
            .build()

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 用户没授予 POST_NOTIFICATIONS：静默跳过，不崩。
        }
    }

    fun cancelNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIFICATION_ID)
    }

    /** 通知「已打卡」按钮：写一条真实打卡记录并撤掉通知。迟到分钟按设置里的上班时间算好落库。 */
    suspend fun markClockIn(context: Context, type: String) {
        val s = ClockInPrefs.get(context)
        val now = LocalDateTime.now()
        val date = Dates.str(now.toLocalDate())
        val time = "%02d:%02d".format(now.hour, now.minute)
        val late = if (type == "on") {
            val parts = s.onTime.split(":")
            val h = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 9
            val m = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            ((now.hour * 60 + now.minute) - (h * 60 + m)).coerceAtLeast(0)
        } else 0
        AppGraph.repository(context.applicationContext).clockIn(type, date, time, late)
        cancelNotification(context)
    }
}
