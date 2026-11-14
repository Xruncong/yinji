package com.qiushang.yinji.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiushang.yinji.data.TodoEntity
import com.qiushang.yinji.data.YinjiRepository
import com.qiushang.yinji.ui.theme.YinjiColors
import com.qiushang.yinji.ui.theme.YinjiShapes
import com.qiushang.yinji.util.Dates
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TodosScreen(repo: YinjiRepository, onSettings: () -> Unit) {
    val todos by repo.todosFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val today = Dates.today()
    val todayStr = Dates.str(today)

    var tab by remember { mutableStateOf(0) } // 0 今天 1 全部 2 已完成
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TodoEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<TodoEntity?>(null) }

    val undone = todos.filter { !it.done }
    val done = todos.filter { it.done }
    val todayList = undone.filter { t -> t.dueDate == null || t.dueDate!! <= todayStr }
    val visible = when (tab) {
        0 -> todayList
        1 -> undone
        else -> done
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ScreenHeader(title = "待办", subtitle = "把要做的事放下", onSettings = onSettings)

            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("今天 ${todayList.size}", tab == 0, { tab = 0 })
                Chip("全部 ${undone.size}", tab == 1, { tab = 1 })
                Chip("已完成 ${done.size}", tab == 2, { tab = 2 })
            }

            Spacer(Modifier.height(10.dp))
            if (visible.isEmpty()) {
                EmptyHint(
                    when (tab) {
                        0 -> "今天没有待办啦 🎉"
                        1 -> "还没有待办"
                        else -> "还没有已完成的事项"
                    }
                )
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    visible.forEach { t ->
                        CardBox(
                            Modifier
                                .padding(bottom = 10.dp)
                                .clickable { editing = t }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CheckCircle(t.done, YinjiColors.Primary) {
                                    scope.launch { repo.setTodoDone(t.id, !t.done) }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        t.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (t.done) YinjiColors.Muted else YinjiColors.Ink,
                                        textDecoration = if (t.done) TextDecoration.LineThrough else null
                                    )
                                    val overdue = !t.done && t.dueDate != null && t.dueDate!! < todayStr
                                    if (t.dueDate != null) {
                                        Spacer(Modifier.height(3.dp))
                                        val d = Dates.parse(t.dueDate!!)
                                        val label = if (d != null) Dates.friendlyDate(d, today) else t.dueDate!!
                                        Text(
                                            (if (overdue) "⚠ 已逾期 · " else "") + label + (t.dueTime?.let { " $it" } ?: ""),
                                            fontSize = 11.sp,
                                            color = if (overdue) YinjiColors.Overdue else YinjiColors.Muted
                                        )
                                    }
                                }
                                if (t.done) {
                                    Text(
                                        "恢复",
                                        fontSize = 12.sp,
                                        color = YinjiColors.Primary,
                                        modifier = Modifier.clickable { scope.launch { repo.setTodoDone(t.id, false) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(90.dp))
        }

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
            Text("＋ 新建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    if (showAdd) {
        TodoEditDialog(
            initial = null,
            onSave = { title, due, time ->
                scope.launch { repo.addTodo(title, due, time) }
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }
    editing?.let { t ->
        TodoEditDialog(
            initial = t,
            onSave = { title, due, time ->
                scope.launch { repo.updateTodo(t.copy(title = title, dueDate = due, dueTime = time)) }
                editing = null
            },
            onDismiss = { editing = null },
            onDelete = { confirmDelete = t; editing = null }
        )
    }
    confirmDelete?.let { t ->
        ConfirmDialog(
            title = "删除待办",
            message = "「${t.title}」会被删除，无法恢复。",
            onConfirm = { scope.launch { repo.deleteTodo(t.id) } },
            onDismiss = { confirmDelete = null }
        )
    }
}

@Composable
private fun TodoEditDialog(
    initial: TodoEntity?,
    onSave: (String, String?, String?) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var dateInput by remember { mutableStateOf(initial?.dueDate ?: "") }
    var time by remember { mutableStateOf(initial?.dueTime ?: "") }
    var hint by remember { mutableStateOf<String?>(null) }

    YDialog(onDismiss = onDismiss) {
        DialogTitle(if (initial == null) "新建待办" else "编辑待办")
        TextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("要做什么？", fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Text("截止（可选，支持「明天」「周五」「3天后」「11/20」）", fontSize = 11.sp, color = YinjiColors.Muted)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = dateInput,
                onValueChange = {
                    dateInput = it
                    val d = Dates.parseNatural(it, Dates.today())
                    hint = if (d != null) "→ ${Dates.friendlyDate(d)}" else null
                },
                placeholder = { Text("留空 = 无截止", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        hint?.let { Text(it, fontSize = 11.sp, color = YinjiColors.Primary, modifier = Modifier.padding(top = 4.dp)) }
        Spacer(Modifier.height(8.dp))
        TextField(
            value = time,
            onValueChange = { time = it },
            placeholder = { Text("时间（可选，如 09:30）", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (onDelete != null) GhostButton("删除", onClick = onDelete!!)
            Spacer(Modifier.weight(1f))
            GhostButton("取消", onDismiss)
            Spacer(Modifier.width(8.dp))
            PrimaryButton("保存") {
                val d = if (dateInput.isBlank()) null else Dates.parseNatural(dateInput, Dates.today())
                val t = time.trim().takeIf { it.isNotEmpty() }
                if (title.isNotBlank()) onSave(title.trim(), d?.let { Dates.str(it) }, t)
            }
        }
    }
}
