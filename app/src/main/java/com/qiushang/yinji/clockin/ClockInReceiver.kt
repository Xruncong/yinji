package com.qiushang.yinji.clockin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 打卡提醒的广播接收器：定时器到点、通知按钮、系统事件（开机/改时间/跨日/升级）都从这里进。
 *
 * 关键：写库（markClockIn）是 suspend，必须用 goAsync() 挂住广播生命周期，
 * 协程结束再 finish()；否则 onReceive 返回后进程可能被杀，打卡记录丢失。
 */
class ClockInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            ClockInScheduler.ACTION_ALARM_ON -> {
                ClockInScheduler.showNotification(appContext, "on")
                ClockInScheduler.schedule(appContext) // 响了就排下一次
            }
            ClockInScheduler.ACTION_ALARM_OFF -> {
                ClockInScheduler.showNotification(appContext, "off")
                ClockInScheduler.schedule(appContext)
            }
            ClockInScheduler.ACTION_MARK_ON,
            ClockInScheduler.ACTION_MARK_OFF -> {
                val type = if (intent.action == ClockInScheduler.ACTION_MARK_ON) "on" else "off"
                val pending = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        ClockInScheduler.markClockIn(appContext, type)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ClockInScheduler.ACTION_SNOOZE -> {
                val type = intent.getStringExtra(ClockInScheduler.EXTRA_TYPE) ?: "on"
                ClockInScheduler.snooze(appContext, type)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ClockInScheduler.schedule(appContext)
            }
        }
    }
}
