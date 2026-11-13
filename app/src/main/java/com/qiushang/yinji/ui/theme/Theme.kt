package com.qiushang.yinji.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 简洁浅色 + 青绿主色，Material 3 系统组件，无第三方设计依赖。 */
object YinjiColors {
    val Page = Color(0xFFF5F8F7)
    val Surface = Color(0xFFFFFFFF)
    val Primary = Color(0xFF2E9C8E)
    val PrimaryDark = Color(0xFF1F7A70)
    val PrimarySoft = Color(0xFFE1F1EE)
    val Ink = Color(0xFF1B2B2B)
    val Muted = Color(0xFF7A8A87)
    val Expense = Color(0xFFE0544A)
    val Income = Color(0xFF2E9C8E)
    val Overdue = Color(0xFFE0544A)
    val Line = Color(0xFFE8EFEE)
    val HeatEmpty = Color(0xFFE9EFEE)
    val HeatFuture = Color(0xFFF4F7F6)
}

private val LightColors = lightColorScheme(
    primary = YinjiColors.Primary,
    onPrimary = Color.White,
    primaryContainer = YinjiColors.PrimarySoft,
    onPrimaryContainer = YinjiColors.PrimaryDark,
    secondary = YinjiColors.PrimaryDark,
    background = YinjiColors.Page,
    onBackground = YinjiColors.Ink,
    surface = YinjiColors.Surface,
    onSurface = YinjiColors.Ink,
    surfaceVariant = YinjiColors.PrimarySoft,
    onSurfaceVariant = YinjiColors.Muted,
    outline = YinjiColors.Line,
    error = YinjiColors.Expense
)

@Composable
fun YinjiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
