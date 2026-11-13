package com.qiushang.yinji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qiushang.yinji.ui.theme.YinjiColors

/** 统一的白色圆角卡片（不用 Card，避免不同 M3 版本的默认值差异）。 */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(YinjiColors.Surface)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onSettings: () -> Unit,
    right: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = YinjiColors.Muted)
            }
        }
        right()
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(YinjiColors.Surface)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", fontSize = 16.sp, color = YinjiColors.Muted)
        }
    }
}

@Composable
fun SectionTitle(text: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
        Spacer(Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}

@Composable
fun BottomNav(index: Int, onIndex: (Int) -> Unit) {
    val items = listOf("◉" to "今日", "¥" to "记账", "✎" to "备忘", "✓" to "待办")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .background(YinjiColors.Surface)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { i, (glyph, label) ->
            val selected = i == index
            val c = if (selected) YinjiColors.Primary else YinjiColors.Muted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onIndex(i) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    glyph,
                    fontSize = 18.sp,
                    color = c,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(Modifier.height(2.dp))
                Text(label, fontSize = 11.sp, color = c)
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) YinjiColors.Primary else YinjiColors.PrimarySoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = if (selected) Color.White else YinjiColors.PrimaryDark,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun YDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(YinjiColors.Surface)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                content = content
            )
        }
    }
}

@Composable
fun DialogTitle(text: String) {
    Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
    Spacer(Modifier.height(12.dp))
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier) {
        Text(text, color = Color.White)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text, color = YinjiColors.Muted) }
}

@Composable
fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 28.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = YinjiColors.Muted)
    }
}

@Composable
fun CheckCircle(checked: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (checked) color else Color.Transparent)
            .border(2.dp, color, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text("✓", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "删除",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    YDialog(onDismiss = onDismiss) {
        DialogTitle(title)
        Text(message, fontSize = 13.sp, color = YinjiColors.Muted)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            GhostButton("取消", onDismiss)
            Spacer(Modifier.width(8.dp))
            PrimaryButton(confirmText) {
                onConfirm()
                onDismiss()
            }
        }
    }
}
