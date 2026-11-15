package com.qiushang.yinji.clockin

import android.app.TimePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiushang.yinji.data.ClockInEntity
import com.qiushang.yinji.data.YinjiRepository
import com.qiushang.yinji.ui.Chip
import com.qiushang.yinji.ui.CreamCard
import com.qiushang.yinji.ui.DialogTitle
import com.qiushang.yinji.ui.GhostButton
import com.qiushang.yinji.ui.PrimaryButton
import com.qiushang.yinji.ui.SectionLabel
import com.qiushang.yinji.ui.SproutBuddy
import com.qiushang.yinji.ui.YDialog
import com.qiushang.yinji.ui.theme.YinjiColors
import com.qiushang.yinji.ui.theme.YinjiShapes
import com.qiushang.yinji.util.Dates
import kotlinx.coroutines.launch
import java.time.LocalDate

private val WEEK = listOf("一", "二", "三", "四", "五", "六", "日")

/** 上班打卡的有效日期集合（软删的不算）。 */
private fun onDoneDates(all: List<ClockInEntity>): Set<String> =
    all.filter { it.type == "on" && it.deletedAt == null }.map { it.date }.toSet()

/** 连续打卡：从今天（未打则从昨天）往前数，有打卡记录就累计；休息日没打卡跳过、不打断；
 *  工作日没打卡才断。这样周末打了卡也算数，连续天数能跨周累积。 */
private fun currentStreak(all: List<ClockInEntity>, weekdays: Set<Int>, today: LocalDate): Int {
    if (weekdays.isEmpty()) return 0
    val done = onDoneDates(all)
    var d = if (done.contains(Dates.str(today))) today else today.minusDays(1)
    var count = 0
    var guard = 0
    while (guard++ < 400) {
        if (done.contains(Dates.str(d))) {
            count++
            d = d.minusDays(1)
        } else if (d.dayOfWeek.value !in weekdays) {
            // 休息日没打卡：不打断，继续往前
            d = d.minusDays(1)
        } else break
    }
    return count
}

/**
 * 今日页顶部的「上班打卡」卡（概念图①）：
 * 左边状态 + 右边带进度环的小芽圆按钮，底部放打下班卡 / 打卡记录 / 提醒设置。
 */
@Composable
fun ClockInCard(
    repo: YinjiRepository,
    onOpenSettings: () -> Unit,
    onOpenStats: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = Dates.today()
    val todayStr = Dates.str(today)
    val records by repo.clockInInRange(todayStr, todayStr).collectAsState(initial = emptyList())
    val settings = remember { ClockInPrefs.get(context) }

    val onRec = records.firstOrNull { it.type == "on" && it.deletedAt == null }
    val offRec = records.firstOrNull { it.type == "off" && it.deletedAt == null }
    val doneOn = onRec != null
    val doneOff = offRec != null
    val doneCount = (if (doneOn) 1 else 0) + (if (doneOff) 1 else 0)

    fun punch(type: String) {
        scope.launch { ClockInScheduler.markClockIn(context.applicationContext, type) }
    }

    CreamCard(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${today.monthValue}.${today.dayOfMonth}", fontSize = 12.sp, color = YinjiColors.Muted)
                Spacer(Modifier.height(3.dp))
                Text("上班打卡", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
                Spacer(Modifier.height(6.dp))
                if (doneOn) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, contentDescription = null,
                            tint = YinjiColors.PrimaryDark, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("已打卡 ${onRec!!.time}", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = YinjiColors.PrimaryDark)
                    }
                    if (doneOff) {
                        Spacer(Modifier.height(2.dp))
                        Text("下班 ${offRec!!.time}", fontSize = 11.sp, color = YinjiColors.Muted)
                    }
                } else {
                    Text("点右边小芽，记一下到岗时间", fontSize = 12.sp, color = YinjiColors.Muted)
                }
                Spacer(Modifier.height(6.dp))
                Text("工作日 ${settings.onTime} 提醒 · 提前 ${settings.advanceMinutes} 分钟",
                    fontSize = 11.sp, color = YinjiColors.Muted)
            }
            Spacer(Modifier.width(10.dp))
            // 圆形打卡按钮：外圈进度环（上班+下班完成度）+ 中间笑脸小芽
            Box(Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(76.dp)) {
                    val stroke = 6.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = YinjiColors.MintLight,
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    if (doneCount > 0) {
                        drawArc(
                            color = YinjiColors.Primary,
                            startAngle = -90f, sweepAngle = 360f * doneCount / 2f, useCenter = false,
                            topLeft = Offset(inset, inset), size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Box(
                    Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(if (doneOn) YinjiColors.MintLight else YinjiColors.PrimarySoft)
                        .border(1.dp, YinjiColors.MintLight, CircleShape)
                        .clickable { punch("on") },
                    contentAlignment = Alignment.Center
                ) {
                    SproutBuddy(Modifier.size(40.dp), color = YinjiColors.Primary)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!doneOn) {
                PrimaryButton("打上班卡", Modifier.weight(1f)) { punch("on") }
            } else {
                Chip("✓ 已上班", true, {}, Modifier.weight(1f))
            }
            if (!doneOff) {
                Chip("打下班卡", false, { punch("off") }, Modifier.weight(1f), YinjiColors.CandyGold)
            } else {
                Chip("✓ 已下班", true, {}, Modifier.weight(1f), YinjiColors.CandyGold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            GhostButton("打卡记录", onOpenStats, Modifier.weight(1f))
            GhostButton("提醒设置", onOpenSettings, Modifier.weight(1f))
        }
    }
}

/** 自绘的小开关，避免 M3 Switch 的版本默认值差异。 */
@Composable
private fun CuteSwitch(checked: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(48.dp, 28.dp)
            .clip(YinjiShapes.Pill)
            .background(if (checked) YinjiColors.Primary else YinjiColors.HeatEmpty)
            .clickable { onToggle(!checked) }
            .padding(3.dp)
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White)
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
        )
    }
}

@Composable
private fun TimePill(label: String, time: String, modifier: Modifier = Modifier, onChange: (Int, Int) -> Unit) {
    val context = LocalContext.current
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    Column(
        modifier
            .clip(YinjiShapes.Small)
            .background(YinjiColors.Field)
            .border(1.dp, YinjiColors.Line, YinjiShapes.Small)
            .clickable {
                TimePickerDialog(context, { _, hh, mm -> onChange(hh, mm) }, h, m, true).show()
            }
            .padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = YinjiColors.Muted)
        Spacer(Modifier.height(2.dp))
        Text(time, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
    }
}

/** 提醒设置弹层（概念图②）：开关 + 上下班时间 + 工作日 + 提前提醒 + 保存。 */
@Composable
fun ClockInSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var s by remember { mutableStateOf(ClockInPrefs.get(context)) }

    YDialog(onDismiss = onDismiss) {
        DialogTitle("打卡提醒")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("开启上班打卡提醒", fontSize = 14.sp, color = YinjiColors.Ink, modifier = Modifier.weight(1f))
            CuteSwitch(s.enabled) { s = s.copy(enabled = it) }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("提醒时间", YinjiColors.Peach)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimePill("上班", s.onTime, Modifier.weight(1f)) { h, m -> s = s.copy(onTime = "%02d:%02d".format(h, m)) }
            TimePill("下班", s.offTime, Modifier.weight(1f)) { h, m -> s = s.copy(offTime = "%02d:%02d".format(h, m)) }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("提醒工作日", YinjiColors.CandyGold)
        Spacer(Modifier.height(8.dp))
        val daySet = s.weekdays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..7).forEach { d ->
                val selected = d in daySet
                Box(
                    Modifier
                        .weight(1f)
                        .clip(YinjiShapes.Pill)
                        .background(if (selected) YinjiColors.Primary else YinjiColors.PrimarySoft)
                        .border(1.dp, if (selected) YinjiColors.PrimaryDark else YinjiColors.MintLight, YinjiShapes.Pill)
                        .clickable {
                            val next = if (selected) daySet - d else daySet + d
                            s = s.copy(weekdays = next.sorted().joinToString(","))
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(WEEK[d - 1], fontSize = 12.sp,
                        color = if (selected) Color.White else YinjiColors.PrimaryDark,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("提前提醒", YinjiColors.Primary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 5, 10, 15).forEach { m ->
                val selected = s.advanceMinutes == m
                Box(
                    Modifier
                        .weight(1f)
                        .clip(YinjiShapes.Pill)
                        .background(if (selected) YinjiColors.Primary else YinjiColors.PrimarySoft)
                        .border(1.dp, if (selected) YinjiColors.PrimaryDark else YinjiColors.MintLight, YinjiShapes.Pill)
                        .clickable { s = s.copy(advanceMinutes = m) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (m == 0) "准点" else "${m}分",
                        fontSize = 12.sp, textAlign = TextAlign.Center,
                        color = if (selected) Color.White else YinjiColors.PrimaryDark,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GhostButton("取消", onDismiss)
            Spacer(Modifier.width(8.dp))
            PrimaryButton("保存") {
                ClockInPrefs.save(context, s)
                ClockInScheduler.schedule(context.applicationContext)
                onDismiss()
            }
        }
    }
}

/** 打卡记录弹层（概念图④）：本月出勤 / 连续打卡 / 迟到 + 周一起始月历热力图。 */
@Composable
fun ClockInStatsDialog(repo: YinjiRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val all by repo.clockInFlow().collectAsState(initial = emptyList())
    var monthOffset by remember { mutableStateOf(0) }
    val base = Dates.today().withDayOfMonth(1).plusMonths(monthOffset.toLong())
    val from = Dates.str(base.withDayOfMonth(1))
    val to = Dates.str(base.withDayOfMonth(base.lengthOfMonth()))
    val settings = remember { ClockInPrefs.get(context) }
    val weekdays = ClockInPrefs.weekdaySet(settings)
    val today = Dates.today()

    val doneDates = onDoneDates(all)
    val lateDates = all.filter { it.type == "on" && it.deletedAt == null && it.lateMinutes > 0 }
        .map { it.date }.toSet()
    val monthOn = all.filter { it.type == "on" && it.deletedAt == null && it.date in from..to }
    val attendance = monthOn.map { it.date }.distinct().size
    val lateCount = monthOn.count { it.lateMinutes > 0 }
    val streak = currentStreak(all, weekdays, today)

    YDialog(onDismiss = onDismiss) {
        DialogTitle("本月打卡")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMini("本月出勤", "$attendance 天", YinjiColors.Cream, Modifier.weight(1f))
            StatMini("连续打卡", "$streak 天", YinjiColors.MintLight, Modifier.weight(1f))
            StatMini("迟到", "$lateCount 次", YinjiColors.Peach, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(YinjiColors.PrimarySoft)
                .clickable { monthOffset-- }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上个月",
                    tint = YinjiColors.PrimaryDark, modifier = Modifier.size(18.dp))
            }
            Text(Dates.monthLabel(base), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = YinjiColors.Ink, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(Modifier.size(28.dp).clip(CircleShape).background(YinjiColors.PrimarySoft)
                .clickable { monthOffset++ }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下个月",
                    tint = YinjiColors.PrimaryDark, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            WEEK.forEach { w ->
                Text(w, fontSize = 11.sp, color = YinjiColors.Muted,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))
        Dates.monthGrid(base.year, base.monthValue).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                week.forEach { (d, inMonth) ->
                    val key = Dates.str(d)
                    val done = doneDates.contains(key)
                    val late = lateDates.contains(key)
                    val future = d.isAfter(today)
                    val workday = d.dayOfWeek.value in weekdays
                    val bg = when {
                        done && late -> YinjiColors.Peach
                        done -> YinjiColors.Primary
                        future || !inMonth -> YinjiColors.HeatFuture
                        workday -> YinjiColors.HeatEmpty
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(30.dp)
                            .clip(YinjiShapes.Tiny)
                            .background(bg)
                            .then(if (d == today) Modifier.border(2.dp, YinjiColors.PrimaryDark, YinjiShapes.Tiny) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${d.dayOfMonth}", fontSize = 11.sp,
                            color = when {
                                done -> Color.White
                                !inMonth || future -> YinjiColors.Muted
                                else -> YinjiColors.Ink
                            })
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LegendDot(YinjiColors.Primary, "已打卡")
            LegendDot(YinjiColors.Peach, "迟到")
            LegendDot(YinjiColors.HeatEmpty, "未打卡")
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GhostButton("关闭", onDismiss)
        }
    }
}

@Composable
private fun StatMini(label: String, value: String, bg: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(YinjiShapes.Small)
            .background(bg.copy(alpha = 0.45f))
            .border(1.dp, bg, YinjiShapes.Small)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 10.sp, color = YinjiColors.Muted)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = YinjiColors.Muted)
    }
}
