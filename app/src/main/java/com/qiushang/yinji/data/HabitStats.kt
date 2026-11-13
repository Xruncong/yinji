package com.qiushang.yinji.data

import com.qiushang.yinji.util.Dates
import java.time.LocalDate

/**
 * 习惯统计 —— 派生数据，不落库。
 *
 * 建模照 Eshan549/Habit-Tracker 的「habitId+date 唯一键 + 每次拉全部 completion 重算」，
 * 连续天数照 uHabits「基于computed entries重算」的思路。
 *
 * 修复开源实现里的已知 bug：Eshan549 的 longestStreak 用 `date == prev.plusDays(1)`，
 * 对「指定周几」的习惯跨周末会误判断链（周一→上周五相隔 3 天算断）。
 * 这里改为按「应做日序列」的相邻关系判断，休息日不打断 streak。
 */
data class HabitStats(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val completionRate30: Float = 0f,
    val doneToday: Boolean = false
)

object HabitStatsCalculator {

    /** 某天是否为该习惯的「应做日」：weekdays 为空 = 每天；否则 ISO 周一=1…周日=7。 */
    fun isScheduled(habit: HabitEntity, date: LocalDate): Boolean {
        val spec = habit.weekdays.trim()
        if (spec.isEmpty()) return true
        val set = spec.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        return date.dayOfWeek.value in set
    }

    fun stats(habit: HabitEntity, doneDates: Set<String>, today: LocalDate = Dates.today()): HabitStats {
        // 只看未软删的完成记录（doneDates 已由 Repository 过滤）
        val doneToday = doneDates.contains(Dates.str(today))

        // 应做日序列：从今天往前回溯 400 天，包含今天及之前的应做日
        val scheduled = (0..399).map { today.minusDays(it.toLong()) }
            .filter { isScheduled(habit, it) }
            .toList()

        // 从今天（若今天未打卡则从昨天起算，不清零 —— 借鉴 XuGuu 的口径）倒序找连续段
        var cursor = 0
        if (!scheduled.isEmpty() && scheduled[0] == today && !doneToday) cursor = 1
        var current = 0
        while (cursor < scheduled.size && doneDates.contains(Dates.str(scheduled[cursor]))) {
            current++
            cursor++
        }

        // 最长连续：按应做日序列相邻判断（不要求自然日相邻）
        var longest = 0
        var run = 0
        for (d in scheduled) {
            if (doneDates.contains(Dates.str(d))) {
                run++
                if (run > longest) longest = run
            } else {
                run = 0
            }
        }

        // 近 30 天完成率：分母 = 这 30 天里的应做日数
        val last30 = (0..29).map { today.minusDays(it.toLong()) }
        val due = last30.count { isScheduled(habit, it) }
        val hit = last30.count { isScheduled(habit, it) && doneDates.contains(Dates.str(it)) }
        val rate = if (due == 0) 0f else hit.toFloat() / due

        return HabitStats(current, maxOf(longest, current), rate, doneToday)
    }

    /** 按一组习惯汇总「今日完成 / 今日应做」。doneByHabit: habitId -> 该习惯的已完成日期集合。 */
    fun todayProgress(habits: List<HabitEntity>, doneByHabit: Map<Long, Set<String>>, today: LocalDate = Dates.today()): Pair<Int, Int> {
        val due = habits.count { isScheduled(it, today) }
        val hit = habits.count { isScheduled(it, today) && doneByHabit[it.id].orEmpty().contains(Dates.str(today)) }
        return hit to due
    }
}
