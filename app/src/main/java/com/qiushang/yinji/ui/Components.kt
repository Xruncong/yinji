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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
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

/**
 * 手账吉祥物「小芽同学」：圆脑袋 + 两片叶 + 笑脸 + 腮红 + 漂浮小圆点。
 * 纯 Canvas 自绘，用于空态插画、环形图中心等，缩放不失真、不依赖图片资源。
 */
@Composable
fun SproutBuddy(modifier: Modifier = Modifier, color: Color = YinjiColors.Primary) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val headR = w * 0.28f
        val headCy = h * 0.60f

        // 漂浮的小圆点（奶油黄 / 蜜桃粉 / 薄荷）
        drawOval(YinjiColors.Cream.copy(alpha = 0.9f),
            Offset(w * 0.09f, h * 0.30f), Size(w * 0.06f, w * 0.06f))
        drawOval(YinjiColors.Peach.copy(alpha = 0.9f),
            Offset(w * 0.85f, h * 0.22f), Size(w * 0.05f, w * 0.05f))
        drawOval(YinjiColors.MintLight,
            Offset(w * 0.87f, h * 0.56f), Size(w * 0.04f, w * 0.04f))

        // 两片圆叶
        val lw = w * 0.30f
        val lh = h * 0.14f
        val leafBaseY = headCy - headR * 0.82f
        rotate(38f, Offset(cx, leafBaseY)) {
            drawOval(color, Offset(cx - lw, leafBaseY - lh), Size(lw, lh))
        }
        rotate(-38f, Offset(cx, leafBaseY)) {
            drawOval(color.copy(alpha = 0.82f), Offset(cx, leafBaseY - lh), Size(lw, lh))
        }

        // 圆脑袋：外圈浅色光晕 + 实心
        drawOval(color.copy(alpha = 0.18f),
            Offset(cx - headR * 1.10f, headCy - headR * 1.10f),
            Size(headR * 2.20f, headR * 2.20f))
        drawOval(color, Offset(cx - headR, headCy - headR), Size(headR * 2f, headR * 2f))

        // 眼睛
        val eyeR = headR * 0.11f
        val eyeY = headCy - headR * 0.14f
        drawOval(Color.White, Offset(cx - headR * 0.42f - eyeR, eyeY - eyeR), Size(eyeR * 2, eyeR * 2))
        drawOval(Color.White, Offset(cx + headR * 0.42f - eyeR, eyeY - eyeR), Size(eyeR * 2, eyeR * 2))

        // 腮红
        drawOval(YinjiColors.Peach.copy(alpha = 0.7f),
            Offset(cx - headR * 0.74f, eyeY + headR * 0.30f), Size(headR * 0.26f, headR * 0.15f))
        drawOval(YinjiColors.Peach.copy(alpha = 0.7f),
            Offset(cx + headR * 0.48f, eyeY + headR * 0.30f), Size(headR * 0.26f, headR * 0.15f))

        // 微笑弧
        val smileW = headR * 0.78f
        drawArc(
            color = Color.White,
            startAngle = 10f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(cx - smileW / 2f, eyeY + headR * 0.06f),
            size = Size(smileW, headR * 0.72f),
            style = Stroke(width = (headR * 0.10f).coerceAtLeast(1.5f), cap = StrokeCap.Round)
        )
    }
}

/** 柔和彩色圆底 + emoji 的分类图标，替换原来过淡的方块底。 */
@Composable
fun CategoryEmoji(emoji: String, color: Color, size: Dp = 40.dp, emojiSize: Int = 18) {
    Box(
        Modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.30f)),
        contentAlignment = Alignment.Center
    ) { Text(emoji, fontSize = emojiSize.sp) }
}

/** 奶油黄强调卡：用于月度总额等主信息卡，和概念图一致。 */
@Composable
fun CreamCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(5.dp, YinjiShapes.Card)
            .clip(YinjiShapes.Card)
            .background(YinjiColors.Cream.copy(alpha = 0.35f))
            .border(1.dp, YinjiColors.Cream.copy(alpha = 0.7f), YinjiShapes.Card)
            .padding(18.dp),
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
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    baseColor: Color = YinjiColors.PrimaryDark
) {
    Box(
        modifier = modifier
            .clip(YinjiShapes.Pill)
            .background(if (selected) baseColor else baseColor.copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = if (selected) Color.White else baseColor,
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
        Modifier.fillMaxWidth().padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SproutBuddy(modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(6.dp))
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
