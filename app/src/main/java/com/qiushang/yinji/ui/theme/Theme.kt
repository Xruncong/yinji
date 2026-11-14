package com.qiushang.yinji.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * 「奶油薄荷 · 小芽生长」视觉令牌。
 * 奶白底 + 薄荷绿 + 奶油黄/蜜桃粉点缀，圆润软糖卡片，无第三方设计依赖。
 * Primary/PrimaryDark 保持足够深，强调文字与白字按钮在奶白底上都清晰；
 * 浅薄荷只用于填充与装饰（PrimarySoft / MintLight）。
 */
object YinjiColors {
    val Page = Color(0xFFFBF8F1)
    val Surface = Color(0xFFFFFFFF)
    val Primary = Color(0xFF3A9A80)
    val PrimaryDark = Color(0xFF2A6F5B)
    val PrimarySoft = Color(0xFFE6F5EF)
    val MintLight = Color(0xFFB7E7D6)
    val Cream = Color(0xFFF7D98C)
    val Peach = Color(0xFFF6B8AE)
    val Ink = Color(0xFF33564B)
    val Muted = Color(0xFF5E7C70)
    val Expense = Color(0xFFE8736B)
    val Income = Color(0xFF2A6F5B)
    val Overdue = Color(0xFFD8564C)
    val Line = Color(0xFFF0E9DA)
    val HeatEmpty = Color(0xFFF3EFE4)
    val HeatFuture = Color(0xFFFAF7EF)
    /** 输入框的暖奶油底 —— 覆盖 M3 默认的带紫味 surfaceContainerHighest */
    val Field = Color(0xFFF4EEDD)
    val OnPrimary = Color(0xFFFFFFFF)
    /** 极柔的暖调阴影，避免硬边黑影 */
    val Shadow = Color(0x1A6B5B4A)
}

/** 统一的圆润形状 —— 软糖卡片、全圆按钮/Chip、大圆角弹层。 */
object YinjiShapes {
    val Card: Shape = RoundedCornerShape(24.dp)
    val Dialog: Shape = RoundedCornerShape(28.dp)
    val Pill: Shape = RoundedCornerShape(50)
    val Small: Shape = RoundedCornerShape(14.dp)
    val Tiny: Shape = RoundedCornerShape(10.dp)
}

private val LightColors = lightColorScheme(
    primary = YinjiColors.PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = YinjiColors.PrimarySoft,
    onPrimaryContainer = YinjiColors.Ink,
    secondary = YinjiColors.Primary,
    background = YinjiColors.Page,
    onBackground = YinjiColors.Ink,
    surface = YinjiColors.Surface,
    onSurface = YinjiColors.Ink,
    surfaceVariant = YinjiColors.PrimarySoft,
    onSurfaceVariant = YinjiColors.Muted,
    // M3 1.3.1 的 TextField 默认容器取 surfaceContainerHighest（基线带紫味），
    // 这里把整组 surfaceContainer 都改成暖奶油/薄荷，输入框自然与奶白底一致。
    surfaceContainerLowest = YinjiColors.Surface,
    surfaceContainerLow = YinjiColors.HeatFuture,
    surfaceContainer = YinjiColors.PrimarySoft,
    surfaceContainerHigh = YinjiColors.HeatEmpty,
    surfaceContainerHighest = YinjiColors.Field,
    surfaceDim = YinjiColors.HeatEmpty,
    surfaceBright = YinjiColors.Surface,
    outline = YinjiColors.Line,
    outlineVariant = YinjiColors.Line,
    error = YinjiColors.Expense
)

@Composable
fun YinjiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
