package com.qiushang.yinji.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiushang.yinji.data.Categories
import com.qiushang.yinji.data.CategorySum
import com.qiushang.yinji.data.TransactionEntity
import com.qiushang.yinji.data.YinjiRepository
import com.qiushang.yinji.ui.theme.YinjiColors
import com.qiushang.yinji.ui.theme.YinjiShapes
import com.qiushang.yinji.util.Dates
import com.qiushang.yinji.util.Money
import kotlinx.coroutines.launch

@Composable
fun LedgerScreen(repo: YinjiRepository, onSettings: () -> Unit) {
    val today = Dates.today()
    val from = Dates.str(today.withDayOfMonth(1))
    val to = Dates.str(today.withDayOfMonth(today.lengthOfMonth()))
    val txns by repo.txnsInRange(from, to).collectAsState(initial = emptyList())
    val sums by repo.categorySums("expense", from, to).collectAsState(initial = emptyList())
    val expenseTotal by repo.totalByType("expense", from, to).collectAsState(initial = 0L)
    val incomeTotal by repo.totalByType("income", from, to).collectAsState(initial = 0L)
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedCat by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(title = "记账", subtitle = Dates.monthLabel(today), onSettings = onSettings)

        // 月度总额卡（奶油黄强调卡，与概念图一致）
        CreamCard(Modifier.padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth()) {
                AmountCell("本月支出", Money.format(expenseTotal), YinjiColors.Expense)
                Box(Modifier.width(1.dp).height(38.dp).background(YinjiColors.Line))
                AmountCell("本月收入", Money.format(incomeTotal), YinjiColors.Income)
                Box(Modifier.width(1.dp).height(38.dp).background(YinjiColors.Line))
                AmountCell("结余", Money.format(incomeTotal - expenseTotal), YinjiColors.Ink)
            }
        }

        SectionTitle("分类占比", trailing = {
            Text("点色块看金额", fontSize = 11.sp, color = YinjiColors.Muted)
        })

        CardBox(Modifier.padding(horizontal = 16.dp)) {
            if (sums.isEmpty()) {
                EmptyHint("本月还没有支出")
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.Center) {
                        DonutChart(sums, expenseTotal)
                        SproutBuddy(Modifier.size(46.dp))
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f)) {
                        sums.take(5).forEach { s ->
                            val cat = Categories.of(s.categoryKey)
                            val pct = if (expenseTotal == 0L) 0 else (s.total * 100 / expenseTotal).toInt()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedCat == s.categoryKey) YinjiColors.PrimarySoft else Color.Transparent)
                                    .clickable { selectedCat = if (selectedCat == s.categoryKey) null else s.categoryKey }
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(9.dp).clip(CircleShape).background(Color(cat.colorArgb)))
                                Spacer(Modifier.width(7.dp))
                                Text(cat.label, fontSize = 12.sp, color = YinjiColors.Ink, modifier = Modifier.weight(1f))
                                Text(Money.format(s.total), fontSize = 11.sp, color = YinjiColors.Muted)
                            }
                        }
                    }
                }
                selectedCat?.let { key ->
                    val s = sums.find { it.categoryKey == key }
                    if (s != null) {
                        val pct = if (expenseTotal == 0L) 0f else s.total * 100f / expenseTotal
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${Categories.of(key).label}：${Money.format(s.total)} · 占本月支出 ${"%.1f".format(pct)}%",
                            fontSize = 12.sp,
                            color = YinjiColors.Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        SectionTitle("最近记录", trailing = {
            Text("＋ 记一笔", fontSize = 13.sp, color = YinjiColors.Primary,
                modifier = Modifier.clickable { showAdd = true })
        })

        if (txns.isEmpty()) {
            EmptyHint("还没有账目，点「＋ 记一笔」记下第一笔")
        } else {
            Column(Modifier.padding(horizontal = 16.dp)) {
                txns.take(20).forEach { t ->
                    val cat = Categories.of(t.categoryKey)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(YinjiColors.Surface)
                            .clickable { editing = t }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CategoryEmoji(cat.emoji, Color(cat.colorArgb), size = 38.dp, emojiSize = 18)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (t.note.isBlank()) cat.label else t.note,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
                            Text("${cat.label} · ${t.date}", fontSize = 11.sp, color = YinjiColors.Muted)
                        }
                        val isExpense = t.type == "expense"
                        Text(
                            (if (isExpense) "−" else "+") + Money.format(t.amountCents),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpense) YinjiColors.Expense else YinjiColors.Income
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(80.dp))
        }
        // 悬浮记一笔按钮
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .clip(YinjiShapes.Pill)
                .background(YinjiColors.PrimaryDark)
                .clickable { showAdd = true }
                .padding(horizontal = 20.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("＋ 记一笔", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    if (showAdd) {
        AddTransactionDialog(
            initial = null,
            onSave = { type, cents, key, note, date ->
                scope.launch { repo.addTransaction(type, cents, key, note, date) }
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
    editing?.let { t ->
        AddTransactionDialog(
            initial = t,
            onSave = { type, cents, key, note, date ->
                scope.launch { repo.updateTransaction(t.copy(type = type, amountCents = cents, categoryKey = key, note = note, date = date)) }
                editing = null
            },
            onDismiss = { editing = null },
            onDelete = { confirmDelete = t; editing = null }
        )
    }
    confirmDelete?.let { t ->
        ConfirmDialog(
            title = "删除账目",
            message = "这笔 ${Money.format(t.amountCents)} 的记录会被删除。",
            onConfirm = { scope.launch { repo.deleteTransaction(t.id) } },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
private fun RowScope.AmountCell(label: String, value: String, color: Color) {
    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = YinjiColors.Muted)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun DonutChart(sums: List<CategorySum>, total: Long) {
    Canvas(modifier = Modifier.size(120.dp)) {
        val stroke = Stroke(width = 20.dp.toPx())
        val inset = 10.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)
        var start = -90f
        sums.forEach { s ->
            val sweep = if (total == 0L) 0f else s.total * 360f / total
            drawArc(
                color = Color(Categories.of(s.categoryKey).colorArgb),
                startAngle = start,
                sweepAngle = sweep - 1.5f, // 留一点缝隙
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
            start += sweep
        }
    }
}

/** 分类 5 列网格与数字键盘同屏 —— 鲨鱼记账「两步记一笔」，但分类始终可见。 */
@Composable
private fun AddTransactionDialog(
    initial: TransactionEntity?,
    onSave: (String, Long, String, String, String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var type by remember { mutableStateOf(initial?.type ?: "expense") }
    var key by remember { mutableStateOf(initial?.categoryKey ?: "food") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var dateText by remember { mutableStateOf(initial?.date ?: Dates.todayStr()) }
    var amount by remember { mutableStateOf(if (initial != null) Money.editable(initial.amountCents) else "") }
    val cats = Categories.forType(type)
    // 日期可编辑：支持「今天 / 明天 / 周三 / 2026-11-13 / 11月13日」，
    // 保存时用解析后的本地日，识别不了就拦下保存而不是静默写今天。
    val parsedDate = Dates.parseNatural(dateText)
    val dateValid = parsedDate != null

    YDialog(onDismiss = onDismiss) {
        DialogTitle(if (initial == null) "记一笔" else "编辑账目")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("支出", type == "expense", { type = "expense"; key = "food" }, Modifier.weight(1f))
            Chip("收入", type == "income", { type = "income"; key = "salary" }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        // 金额显示
        Text(
            if (amount.isEmpty()) "¥ 0.00" else "¥ $amount",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = if (type == "expense") YinjiColors.Expense else YinjiColors.Income,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text("选分类 → 输金额 → 保存", fontSize = 11.sp, color = YinjiColors.Muted,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))

        // 分类网格（与键盘同屏可见）
        cats.chunked(5).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                row.forEach { c ->
                    val selected = key == c.key
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(YinjiShapes.Small)
                            .background(if (selected) Color(c.colorArgb).copy(alpha = 0.24f) else YinjiColors.Page)
                            .border(
                                1.dp,
                                if (selected) Color(c.colorArgb) else Color.Transparent,
                                YinjiShapes.Small
                            )
                            .clickable { key = c.key }
                            .padding(vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CategoryEmoji(c.emoji, Color(c.colorArgb), size = 30.dp, emojiSize = 15)
                        Spacer(Modifier.height(2.dp))
                        Text(c.label, fontSize = 9.sp, color = YinjiColors.Ink, maxLines = 1)
                    }
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(10.dp))

        TextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("备注（可选）", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        TextField(
            value = dateText,
            onValueChange = { dateText = it },
            placeholder = { Text("日期（今天 / 明天 / 周三 / 2026-11-13）", fontSize = 11.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("今天", fontSize = 11.sp, color = YinjiColors.Primary,
                modifier = Modifier.clickable { dateText = "今天" })
            Text("昨天", fontSize = 11.sp, color = YinjiColors.Primary,
                modifier = Modifier.clickable { dateText = Dates.str(Dates.today().minusDays(1)) })
            Text(
                if (dateValid) "→ ${Dates.friendlyDate(parsedDate!!)}" else "日期无法识别",
                fontSize = 11.sp,
                color = if (dateValid) YinjiColors.Muted else YinjiColors.Overdue
            )
        }
        Spacer(Modifier.height(10.dp))

        // 数字键盘
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", ".", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(YinjiShapes.Small)
                            .background(YinjiColors.Surface)
                            .border(1.dp, YinjiColors.Line, YinjiShapes.Small)
                            .clickable {
                                when (k) {
                                    "⌫" -> if (amount.isNotEmpty()) amount = amount.dropLast(1)
                                    "." -> if (!amount.contains(".")) amount += "."
                                    else -> {
                                        // 小数点后最多两位
                                        val dot = amount.indexOf('.')
                                        if (dot < 0 || amount.length - dot <= 2) amount += k
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) { Text(k, fontSize = 18.sp, color = YinjiColors.Ink) }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (onDelete != null) GhostButton("删除", onClick = onDelete!!)
            Spacer(Modifier.weight(1f))
            GhostButton("取消", onDismiss)
            Spacer(Modifier.width(8.dp))
            PrimaryButton("保存") {
                val cents = Money.parse(amount)
                val resolvedDate = parsedDate?.let { Dates.str(it) }
                if (cents != null && cents > 0 && resolvedDate != null) {
                    onSave(type, cents, key, note.trim(), resolvedDate)
                }
            }
        }
    }
}
