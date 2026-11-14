package com.qiushang.yinji.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qiushang.yinji.ui.theme.YinjiColors
import com.qiushang.yinji.ui.theme.YinjiShapes

/** 软糖卡片：白底 + 大圆角 + 极柔阴影 + 奶油色细描边，不用 M3 Card 的默认值。 */
@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(5.dp, YinjiShapes.Card)
            .clip(YinjiShapes.Card)
            .background(YinjiColors.Surface)
            .border(1.dp, YinjiColors.Line, YinjiShapes.Card)
            .padding(18.dp),
        content = content
    )
}

/**
 * 手绘小芽：一根短茎 + 两片向上展开的叶子。
 * 纯 Canvas 自绘，不依赖 emoji / 图片资源，缩放不失真。
 */
@Composable
fun Sprout(modifier: Modifier = Modifier, color: Color = YinjiColors.PrimaryDark) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val top = h * 0.52f
        drawLine(
            color = color,
            start = Offset(cx, h * 0.9f),
            end = Offset(cx, top),
            strokeWidth = (w * 0.075f).coerceAtLeast(2f),
            cap = StrokeCap.Round
        )
        val lw = w * 0.4f
        val lh = h * 0.24f
        rotate(42f, Offset(cx, top)) {
            drawOval(color, Offset(cx - lw, top - lh / 2f), Size(lw, lh))
        }
        rotate(-42f, Offset(cx, top)) {
            drawOval(color.copy(alpha = 0.78f), Offset(cx, top - lh / 2f), Size(lw, lh))
        }
    }
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
            .padding(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
            if (subtitle != null) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, fontSize = 12.sp, color = YinjiColors.Muted)
            }
        }
        right()
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(YinjiColors.Surface)
                .border(1.dp, YinjiColors.Line, CircleShape)
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Settings,
                contentDescription = "设置",
                tint = YinjiColors.Muted,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, trailing: @Composable (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
        Spacer(Modifier.weight(1f))
        if (trailing != null) trailing()
    }
}

@Composable
fun BottomNav(index: Int, onIndex: (Int) -> Unit) {
    val items = listOf(
        Icons.Rounded.Home to "今日",
        Icons.Rounded.AccountBalanceWallet to "记账",
        Icons.AutoMirrored.Rounded.StickyNote2 to "备忘",
        Icons.Rounded.Checklist to "待办"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, YinjiShapes.Card)
            .clip(YinjiShapes.Card)
            .background(YinjiColors.Surface)
            .border(1.dp, YinjiColors.Line, YinjiShapes.Card)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { i, (icon, label) ->
            val selected = i == index
            val c = if (selected) YinjiColors.PrimaryDark else YinjiColors.Muted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(YinjiShapes.Pill)
                    .background(if (selected) YinjiColors.PrimarySoft else Color.Transparent)
                    .clickable { onIndex(i) }
                    .padding(vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, contentDescription = label, tint = c, modifier = Modifier.size(21.dp))
                Spacer(Modifier.height(3.dp))
                Text(
                    label,
                    fontSize = 11.sp,
                    color = c,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(YinjiShapes.Pill)
            .background(if (selected) YinjiColors.PrimaryDark else YinjiColors.PrimarySoft)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = if (selected) Color.White else YinjiColors.PrimaryDark,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
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
                .shadow(10.dp, YinjiShapes.Dialog)
                .clip(YinjiShapes.Dialog)
                .background(YinjiColors.Surface)
                .border(1.dp, YinjiColors.Line, YinjiShapes.Dialog)
                .padding(20.dp)
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
    Text(text, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = YinjiColors.Ink)
    Spacer(Modifier.height(14.dp))
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = YinjiShapes.Pill,
        colors = ButtonDefaults.buttonColors(containerColor = YinjiColors.PrimaryDark)
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier, shape = YinjiShapes.Pill) {
        Text(text, color = YinjiColors.Muted)
    }
}

@Composable
fun EmptyHint(text: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Spa,
            contentDescription = null,
            tint = YinjiColors.Primary.copy(alpha = 0.45f),
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(text, fontSize = 13.sp, color = YinjiColors.Muted, textAlign = TextAlign.Center)
    }
}

@Composable
fun CheckCircle(checked: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (checked) color else Color.Transparent)
            .border(2.dp, if (checked) color else color.copy(alpha = 0.55f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = "已完成",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
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
