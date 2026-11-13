package com.qiushang.yinji.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * 业务键一律用本地 LocalDate + ISO "yyyy-MM-dd" 字符串。
 * 来自 uHabits / Eshan549 / Shukan 的共识：本地日字符串天然避开 DST 与 "localtime" 换算的 off-by-one。
 */
object Dates {
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()
    fun todayStr(): String = today().format(ISO)
    fun str(d: LocalDate): String = d.format(ISO)
    fun parse(s: String): LocalDate? = runCatching { LocalDate.parse(s, ISO) }.getOrNull()

    /** 周起始日周一（定稿默认值） */
    fun startOfWeek(d: LocalDate): LocalDate =
        d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** 月历网格：周一起始，补齐到整周；返回 (date, 是否属于本月) */
    fun monthGrid(year: Int, month: Int): List<List<Pair<LocalDate, Boolean>>> {
        val first = LocalDate.of(year, month, 1)
        val gridStart = startOfWeek(first)
        val last = first.withDayOfMonth(first.lengthOfMonth())
        val gridEnd = last.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val weeks = mutableListOf<List<Pair<LocalDate, Boolean>>>()
        var cursor = gridStart
        while (!cursor.isAfter(gridEnd)) {
            weeks += (0..6).map {
                val d = cursor.plusDays(it.toLong())
                d to (d.monthValue == month && d.year == year)
            }
            cursor = cursor.plusDays(7)
        }
        return weeks
    }

    fun weekdayLabel(d: LocalDate): String = when (d.dayOfWeek.value) {
        1 -> "一"; 2 -> "二"; 3 -> "三"; 4 -> "四"; 5 -> "五"; 6 -> "六"; else -> "日"
    }

    fun monthLabel(d: LocalDate): String = "${d.year} 年 ${d.monthValue} 月"

    fun friendlyDate(d: LocalDate, today: LocalDate = today()): String = when {
        d == today -> "今天"
        d == today.plusDays(1) -> "明天"
        d == today.plusDays(2) -> "后天"
        d.year == today.year -> "${d.monthValue} 月 ${d.dayOfMonth} 日 周${weekdayLabel(d)}"
        else -> "${d.year} 年 ${d.monthValue} 月 ${d.dayOfMonth} 日"
    }

    /**
     * 自然语言日期最小集（借鉴滴答清单「明早十点开会」，一期只识别日期，不做语音/时间）：
     * 今天/明天/后天/大后天、周X、下周X、N天后、M月D日、M/D、yyyy-MM-dd。
     * 识别不到返回 null，调用方保留原文即可。
     */
    fun parseNatural(input: String, today: LocalDate = today()): LocalDate? {
        val s = input.trim()
        if (s.isEmpty()) return null
        when (s) {
            "今天", "今日" -> return today
            "明天", "明日" -> return today.plusDays(1)
            "后天" -> return today.plusDays(2)
            "大后天" -> return today.plusDays(3)
        }
        Regex("^(\\d{1,3})\\s*天后?$").find(s)?.let {
            return today.plusDays(it.groupValues[1].toLong())
        }
        val cnWeek = mapOf(
            "一" to 1, "二" to 2, "三" to 3, "四" to 4,
            "五" to 5, "六" to 6, "日" to 7, "天" to 7
        )
        Regex("^下\\s*(?:周|星期|礼拜)\\s*([一二三四五六日天])$").find(s)?.let {
            val target = cnWeek[it.groupValues[1]] ?: return null
            return startOfWeek(today).plusWeeks(1).plusDays((target - 1).toLong())
        }
        // 周X：本周期内若已过，顺延到下周（滴答清单语义）
        Regex("^(?:周|星期|礼拜)\\s*([一二三四五六日天])$").find(s)?.let {
            val target = cnWeek[it.groupValues[1]] ?: return null
            var d = startOfWeek(today).plusDays((target - 1).toLong())
            if (d.isBefore(today)) d = d.plusWeeks(1)
            return d
        }
        Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})$").find(s)?.let {
            return runCatching {
                LocalDate.of(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].toInt())
            }.getOrNull()
        }
        Regex("^(\\d{1,2})\\s*[月/\\-]\\s*(\\d{1,2})\\s*日?$").find(s)?.let {
            val m = it.groupValues[1].toInt()
            val day = it.groupValues[2].toInt()
            return runCatching {
                var d = LocalDate.of(today.year, m, day)
                if (d.isBefore(today)) d = d.plusYears(1)
                d
            }.getOrNull()
        }
        return parse(s)
    }
}
