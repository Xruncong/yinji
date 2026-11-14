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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiushang.yinji.data.NoteEntity
import com.qiushang.yinji.data.YinjiRepository
import com.qiushang.yinji.ui.theme.YinjiColors
import com.qiushang.yinji.ui.theme.YinjiShapes
import com.qiushang.yinji.util.Dates
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private fun noteTime(createdAt: Long): String {
    val d = Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return Dates.friendlyDate(d)
}

/** 从正文里抽出 #标签，用于搜索与展示。 */
private fun tagsOf(text: String): List<String> =
    Regex("#([^\\s#]+)").findAll(text).map { it.groupValues[1] }.toList()

@Composable
fun NotesScreen(repo: YinjiRepository, onSettings: () -> Unit) {
    val notes by repo.notesFlow().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<NoteEntity?>(null) }
    var confirmDelete by remember { mutableStateOf<NoteEntity?>(null) }

    val visible = notes.filter {
        query.isBlank() || it.text.contains(query, ignoreCase = true)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ScreenHeader(title = "备忘", subtitle = "想到就记下来", onSettings = onSettings)

            // 一行输入（flomo 风格）
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("一句话记下来，可用 #标签", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(YinjiShapes.Pill)
                        .background(YinjiColors.PrimaryDark)
                        .clickable {
                            val t = input.trim()
                            if (t.isNotEmpty()) {
                                scope.launch { repo.addNote(t) }
                                input = ""
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) { Text("记下", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(10.dp))
            TextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Search, contentDescription = null,
                                tint = YinjiColors.Muted, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("搜索正文或 #标签", fontSize = 12.sp)
                        }
                    },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            SectionTitle("共 ${visible.size} 条")
            if (visible.isEmpty()) {
                EmptyHint(if (query.isBlank()) "还没有备忘，上面写一句吧" else "没找到匹配的备忘")
            } else {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    visible.forEach { n ->
                        val tags = tagsOf(n.text)
                        CardBox(
                            Modifier
                                .padding(bottom = 10.dp)
                                .clickable { editing = n }
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                if (n.pinned) {
                                    Icon(Icons.Rounded.PushPin, contentDescription = "已置顶",
                                        tint = YinjiColors.Expense, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(6.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(n.text, fontSize = 14.sp, color = YinjiColors.Ink, lineHeight = 20.sp)
                                    if (tags.isNotEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        Row {
                                            tags.take(4).forEach { t ->
                                                Box(
                                                    Modifier
                                                        .padding(end = 6.dp)
                                                        .clip(YinjiShapes.Pill)
                                                        .background(YinjiColors.PrimarySoft)
                                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                                ) { Text("#$t", fontSize = 10.sp, color = YinjiColors.PrimaryDark) }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(noteTime(n.createdAt), fontSize = 10.sp, color = YinjiColors.Muted)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (n.pinned) "取消置顶" else "置顶",
                                    fontSize = 11.sp,
                                    color = YinjiColors.Primary,
                                    modifier = Modifier.clickable { scope.launch { repo.setNotePinned(n.id, !n.pinned) } }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    editing?.let { n ->
        var text by remember { mutableStateOf(n.text) }
        YDialog(onDismiss = { editing = null }) {
            DialogTitle("编辑备忘")
            TextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                GhostButton("删除", onClick = { confirmDelete = n; editing = null })
                Spacer(Modifier.weight(1f))
                GhostButton("取消", onClick = { editing = null })
                Spacer(Modifier.width(8.dp))
                PrimaryButton("保存") {
                    val t = text.trim()
                    if (t.isNotEmpty()) {
                        scope.launch { repo.updateNote(n.copy(text = t)) }
                        editing = null
                    }
                }
            }
        }
    }
    confirmDelete?.let { n ->
        ConfirmDialog(
            title = "删除备忘",
            message = "这条备忘会被删除，无法恢复。",
            onConfirm = { scope.launch { repo.deleteNote(n.id) } },
            onDismiss = { confirmDelete = null }
        )
    }
}
