package com.qiushang.yinji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiushang.yinji.data.HabitEntity
import com.qiushang.yinji.data.HabitStatsCalculator
import com.qiushang.yinji.data.YinjiRepository
import com.qiushang.yinji.ui.theme.YinjiColors
import com.qiushang.yinji.ui.theme.YinjiShapes
import com.qiushang.yinji.util.Dates
import kotlinx.coroutines.launch
import java.time.LocalDate

private val WEEK_LABELS = listOf("一", "二", "三", "四", "五", "六", "日")
private val EMOJI_CHOICES = listOf("✅", "📖", "💧", "🏃", "🧘", "🌙", "☀️", "🍎", "✍️", "🎯")

private fun periodLabel(weekdays: String): String {
    val set = weekdays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    if (set.isEmpty()) return "每天"
    return (1..7).filter { it in set }.joinToString("/") { "周${WEEK_LABELS[it - 1]}" }
}

@Composable
fun HabitScreen(repo: YinjiRepository, onSettings: () -> Unit) {
    val habits by repo.habitsFlow().collectAsState(initial = emptyList())
    val completions by repo.completionsFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = Dates.today()
    val todayStr = Dates.str(today)

    val doneByHabit: Map<Long, Set<String>> = completions.groupBy { it.habitId }
        .mapValues { (_, list) -> list.map { it.date }.toSet() }
    val (hit, due) = HabitStatsCalculator.todayProgress(habits, doneByHabit, today)

    var editing by remember { mutableStateOf<HabitEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    var confirmDelete by remember { mutableStateOf<HabitEntity?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(
            title = "早上好，秋殇",
            subtitle = "${today.monthValue} 月 ${today.dayOfMonth} 日 · 周${Dates.weekdayLabel(today)}",
            onSettings = onSettings
        )

        // 今日进度卡：奶油薄荷软糖卡 + 手绘小芽（随进度长大）
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(5.dp, YinjiShapes.Card)
                .clip(YinjiShapes.Card)
                .background(YinjiColors.PrimarySoft)
                .border(1.dp, YinjiColors.MintLight, YinjiShapes.Card)
                .padding(18.dp)
        ) {
            val pct = if (due == 0) 0 else hit * 100 / due
            val f = if (due == 0) 0f else hit.toFloat() / due
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Sprout(Modifier.size(16.dp), color = YinjiColors.PrimaryDark)
                        Spacer(Modifier.width(6.dp))
                        Text("今日进度", fontSize = 13.sp, color = YinjiColors.Ink)
                        Spacer(Modifier.weight(1f))
                        Text("$pct%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = YinjiColors.PrimaryDark)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$hit", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
                        Text(" / $due", fontSize = 18.sp, color = YinjiColors.Muted)
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(YinjiShapes.Pill)
                            .background(YinjiColors.MintLight)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(f)
                                .height(8.dp)
                                .clip(YinjiShapes.Pill)
                                .background(YinjiColors.PrimaryDark)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("点一下圆圈即可打卡，再点一下可取消", fontSize = 11.sp, color = YinjiColors.Muted)
                }
                Spacer(Modifier.width(10.dp))
                Sprout(
                    modifier = Modifier.size((44 + 40 * f).dp),
                    color = YinjiColors.Primary
                )
            }
        }

        SectionTitle("今日习惯", trailing = {
            Text("＋ 新建", fontSize = 13.sp, color = YinjiColors.PrimaryDark,
                modifier = Modifier.clickable { showAdd = true })
        })

        if (habits.isEmpty()) {
            EmptyHint("还没有习惯，点右上角「＋ 新建」加一个吧")
        } else {
            habits.forEach { h ->
                val done = doneByHabit[h.id].orEmpty().contains(todayStr)
                val stat = HabitStatsCalculator.stats(h, doneByHabit[h.id].orEmpty(), today)
                CardBox(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp)
                        .clickable { detailId = h.id }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CheckCircle(done, Color(h.colorArgb)) {
                            scope.launch { repo.toggleCompletion(h.id, todayStr) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${h.emoji} ${h.name}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = YinjiColors.Ink
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${periodLabel(h.weekdays)} · 连续 ${stat.currentStreak} 天",
                                fontSize = 11.sp,
                                color = YinjiColors.Muted
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = YinjiColors.Muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Icon(
                    Icons.Rounded.Spa,
                    contentDescription = null,
                    tint = YinjiColors.Primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "点习惯名可看月历热力图、给忘记的日子补卡",
                    fontSize = 11.sp,
                    color = YinjiColors.Muted
                )
            }
        }
    }

    if (showAdd) {
        HabitEditDialog(
            initial = null,
            onSave = { name, emoji, wd ->
                scope.launch { repo.addHabit(name, emoji, 0xFF3A9A80.toInt(), wd) }
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    editing?.let { h ->
        HabitEditDialog(
            initial = h,
            onSave = { name, emoji, wd ->
                scope.launch { repo.updateHabit(h.copy(name = name, emoji = emoji, weekdays = wd)) }
                editing = null
            },
            onDismiss = { editing = null },
            onDelete = { confirmDelete = h; editing = null }
        )
    }

    detailId?.let { id ->
        val h = habits.find { it.id == id }
        if (h != null) {
            HabitDetailDialog(
                habit = h,
                doneDates = doneByHabit[id].orEmpty(),
                onToggle = { date -> scope.launch { repo.toggleCompletion(id, Dates.str(date)) } },
                onEdit = { detailId = null; editing = h },
                onDelete = { detailId = null; confirmDelete = h },
                onDismiss = { detailId = null }
            )
        }
    }

    confirmDelete?.let { h ->
        ConfirmDialog(
            title = "删除习惯",
            message = "「${h.name}」及其打卡记录会一并删除，无法恢复。",
            onConfirm = { scope.launch { repo.deleteHabit(h.id) } },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
private fun HabitEditDialog(
    initial: HabitEntity?,
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "✅") }
    var days by remember {
        mutableStateOf(
            initial?.weekdays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
                ?: emptySet()
        )
    }
    YDialog(onDismiss = onDismiss) {
        DialogTitle(if (initial == null) "新建习惯" else "编辑习惯")
        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("想养成什么习惯？", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("图标", fontSize = 12.sp, color = YinjiColors.Muted)
        Spacer(Modifier.height(6.dp))
        EMOJI_CHOICES.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { e ->
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (emoji == e) YinjiColors.PrimarySoft else YinjiColors.Page)
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) { Text(e, fontSize = 16.sp) }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text("周期", fontSize = 12.sp, color = YinjiColors.Muted)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..7).forEach { d ->
                val selected = if (days.isEmpty()) false else d in days
                Box(
                    Modifier
                        .weight(1f)
                        .clip(YinjiShapes.Small)
                        .background(if (selected) YinjiColors.Primary else YinjiColors.PrimarySoft)
                        .clickable {
                            days = if (d in days) days - d else days + d
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(WEEK_LABELS[d - 1], fontSize = 12.sp,
                        color = if (selected) Color.White else YinjiColors.PrimaryDark)
                }
            }
        }
        Text(
            if (days.isEmpty()) "不勾选 = 每天" else "只在上勾选的日子提醒打卡",
            fontSize = 11.sp, color = YinjiColors.Muted,
            modifier = Modifier.padding(top = 6.dp)
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (onDelete != null) {
                GhostButton("删除", onClick = onDelete!!)
            }
            Spacer(Modifier.weight(1f))
            GhostButton("取消", onDismiss)
            Spacer(Modifier.width(8.dp))
            PrimaryButton("保存") {
                val wd = (1..7).filter { it in days }.joinToString(",")
                if (name.isNotBlank()) onSave(name.trim(), emoji, wd)
            }
        }
    }
}

@Composable
private fun HabitDetailDialog(
    habit: HabitEntity,
    doneDates: Set<String>,
    onToggle: (LocalDate) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var monthOffset by remember { mutableStateOf(0) }
    val base = Dates.today().withDayOfMonth(1).plusMonths(monthOffset.toLong())
    val year = base.year
    val month = base.monthValue
    val today = Dates.today()
    val stat = HabitStatsCalculator.stats(habit, doneDates, today)

    YDialog(onDismiss = onDismiss) {
        DialogTitle("${habit.emoji} ${habit.name}")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatCell("当前连续", "${stat.currentStreak} 天")
            StatCell("最长连续", "${stat.longestStreak} 天")
            StatCell("近30天", "${(stat.completionRate30 * 100).toInt()}%")
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(YinjiColors.PrimarySoft)
                .clickable { monthOffset-- }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上个月",
                    tint = YinjiColors.PrimaryDark, modifier = Modifier.size(18.dp))
            }
            Text(Dates.monthLabel(base), fontSize = 14.sp, fontWeight = FontWeight.Bold,
                color = YinjiColors.Ink, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Box(Modifier.size(28.dp).clip(CircleShape).background(YinjiColors.PrimarySoft)
                .clickable { monthOffset++ }, contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下个月",
                    tint = YinjiColors.PrimaryDark, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            WEEK_LABELS.forEach { w ->
                Text(w, fontSize = 11.sp, color = YinjiColors.Muted,
                    modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))
        Dates.monthGrid(year, month).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                week.forEach { (d, inMonth) ->
                    val key = Dates.str(d)
                    val done = doneDates.contains(key)
                    val future = d.isAfter(today)
                    val scheduled = HabitStatsCalculator.isScheduled(habit, d)
                    val bg = when {
                        done -> Color(habit.colorArgb)
                        future || !inMonth -> YinjiColors.HeatFuture
                        scheduled -> YinjiColors.HeatEmpty
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .height(30.dp)
                            .clip(YinjiShapes.Tiny)
                            .background(bg)
                            .then(if (d == today) Modifier.border(2.dp, YinjiColors.Primary, YinjiShapes.Tiny) else Modifier)
                            .then(
                                if (!future && inMonth && scheduled) Modifier.clickable { onToggle(d) }
                                else Modifier
                            ),
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
        Text("点过去某天可补卡 / 取消，连续天数会按真实记录重算", fontSize = 11.sp, color = YinjiColors.Muted)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GhostButton("删除", onClick = { onDelete() })
            Spacer(Modifier.weight(1f))
            GhostButton("编辑", onClick = { onEdit() })
            Spacer(Modifier.width(4.dp))
            GhostButton("关闭", onDismiss)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = YinjiColors.PrimaryDark)
        Text(label, fontSize = 10.sp, color = YinjiColors.Muted)
    }
}
