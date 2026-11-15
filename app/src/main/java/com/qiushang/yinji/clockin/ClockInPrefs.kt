package com.qiushang.yinji.clockin

import android.content.Context

/**
 * 「上班打卡提醒」设置。用 SharedPreferences 而不是 Room：
 * 这是单份全局配置，不是业务记录，落库反而多一张表 + 迁移，没必要。
 */
object ClockInPrefs {
    private const val FILE = "clock_in_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_ON_TIME = "on_time"
    private const val KEY_OFF_TIME = "off_time"
    private const val KEY_WEEKDAYS = "weekdays"
    private const val KEY_ADVANCE = "advance"

    data class Settings(
        val enabled: Boolean = true,
        val onTime: String = "09:00",
        val offTime: String = "18:00",
        // ISO 周一=1 … 周日=7，逗号分隔
        val weekdays: String = "1,2,3,4,5",
        val advanceMinutes: Int = 5
    )

    fun get(context: Context): Settings {
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return Settings(
            enabled = p.getBoolean(KEY_ENABLED, true),
            onTime = p.getString(KEY_ON_TIME, "09:00") ?: "09:00",
            offTime = p.getString(KEY_OFF_TIME, "18:00") ?: "18:00",
            weekdays = p.getString(KEY_WEEKDAYS, "1,2,3,4,5") ?: "1,2,3,4,5",
            advanceMinutes = p.getInt(KEY_ADVANCE, 5)
        )
    }

    fun save(context: Context, s: Settings) {
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, s.enabled)
            .putString(KEY_ON_TIME, s.onTime)
            .putString(KEY_OFF_TIME, s.offTime)
            .putString(KEY_WEEKDAYS, s.weekdays)
            .putInt(KEY_ADVANCE, s.advanceMinutes)
            .apply()
    }

    fun weekdaySet(s: Settings): Set<Int> =
        s.weekdays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
}
