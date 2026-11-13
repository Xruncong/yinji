package com.qiushang.yinji

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qiushang.yinji.data.AppGraph
import com.qiushang.yinji.ui.BottomNav
import com.qiushang.yinji.ui.ConfirmDialog
import com.qiushang.yinji.ui.GhostButton
import com.qiushang.yinji.ui.HabitScreen
import com.qiushang.yinji.ui.LedgerScreen
import com.qiushang.yinji.ui.NotesScreen
import com.qiushang.yinji.ui.TodosScreen
import com.qiushang.yinji.ui.theme.YinjiTheme
import com.qiushang.yinji.ui.YDialog
import com.qiushang.yinji.ui.DialogTitle
import com.qiushang.yinji.ui.theme.YinjiColors
import com.qiushang.yinji.util.Dates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            YinjiTheme {
                YinjiApp()
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun YinjiApp() {
    val context = LocalContext.current
    val repo = remember { AppGraph.repository(context) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var confirmRestore by remember { mutableStateOf(false) }
    var toastText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastText) {
        toastText?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            toastText = null
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val json = repo.exportJson()
                        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                            ?: error("无法打开文件")
                    }.isSuccess
                }
                toastText = if (ok) "已导出备份文件" else "导出失败"
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val text = context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: error("无法读取文件")
                        repo.importJson(text).getOrThrow()
                    }
                }
                toastText = if (result.isSuccess) "已恢复备份" else "恢复失败：${result.exceptionOrNull()?.message}"
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HabitScreen(repo) { showSettings = true }
                    1 -> LedgerScreen(repo) { showSettings = true }
                    2 -> NotesScreen(repo) { showSettings = true }
                    else -> TodosScreen(repo) { showSettings = true }
                }
            }
            BottomNav(tab) { tab = it }
        }
    }

    if (showSettings) {
        YDialog(onDismiss = { showSettings = false }) {
            DialogTitle("设置")
            Text("数据全部保存在本机，不联网、无账号。", fontSize = 12.sp, color = YinjiColors.Muted)
            Spacer(Modifier.height(6.dp))
            Text("建议定期导出一份备份，卸载 App 前务必导出。", fontSize = 12.sp, color = YinjiColors.Muted)
            Spacer(Modifier.height(18.dp))

            androidx.compose.material3.Button(
                onClick = {
                    val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
                    exportLauncher.launch("yinji-backup-$stamp.json")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("导出备份（JSON）", color = androidx.compose.ui.graphics.Color.White) }
            Spacer(Modifier.height(10.dp))
            androidx.compose.material3.Button(
                onClick = { confirmRestore = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("从备份恢复", color = androidx.compose.ui.graphics.Color.White) }
            Spacer(Modifier.height(14.dp))
            GhostButton("关闭", onClick = { showSettings = false }, modifier = Modifier.fillMaxWidth())
        }
    }

    if (confirmRestore) {
        ConfirmDialog(
            title = "从备份恢复",
            message = "恢复会覆盖当前全部数据（习惯、账目、备忘、待办），且无法撤销。建议先导出一份当前备份。",
            confirmText = "选择备份文件",
            onConfirm = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onDismiss = { confirmRestore = false }
        )
    }
}
