package com.qiushang.yinji.util

import kotlin.math.abs

/**
 * 金额一律以「分」为单位存 Long，界面显示时才格式化。
 * 照 Compose-Expense / 鲨鱼记账的口径，绝不存 Double 避免浮点误差。
 */
object Money {

    /** 分 → "¥1,234.56"（带千分位） */
    fun format(cents: Long): String {
        val v = abs(cents)
        val sign = if (cents < 0) "-" else ""
        val grouped = (v / 100).toString().reversed().chunked(3).joinToString(",").reversed()
        return "$sign¥$grouped.%02d".format(v % 100)
    }

    /** 分 → "¥12.00"（无千分位，紧凑显示） */
    fun plain(cents: Long): String {
        val v = abs(cents)
        val sign = if (cents < 0) "-" else ""
        return "$sign¥${v / 100}.%02d".format(v % 100)
    }

    /** 分 → "12.34"（输入框可编辑形态，不带货币符号） */
    fun editable(cents: Long): String {
        val v = abs(cents)
        return "${v / 100}.%02d".format(v % 100)
    }

    /**
     * "12.34" → 1234（分）。纯字符串解析，绝不经过 Double（"1.005" 这类
     * 边界不会因浮点漂移变成 100 或 101 分）。非法 / 负数 / 超过 2 位小数返回 null。
     */
    fun parse(input: String): Long? {
        val s = input.trim()
        if (s.isEmpty()) return null
        if (s.startsWith('-') || s.startsWith('+')) return null

        // 最多一个小数点
        val dot = s.indexOf('.')
        if (dot != s.lastIndexOf('.')) return null

        val intPart: String
        val fracPart: String
        if (dot < 0) {
            intPart = s
            fracPart = ""
        } else {
            intPart = s.substring(0, dot)
            fracPart = s.substring(dot + 1)
        }
        if (intPart.isEmpty() && fracPart.isEmpty()) return null
        if (intPart.any { it !in '0'..'9' }) return null
        if (fracPart.any { it !in '0'..'9' }) return null
        if (fracPart.length > 2) return null // 分是最小单位，不接受更细的小数

        val whole = if (intPart.isEmpty()) 0L else intPart.toLongOrNull() ?: return null
        if (whole > (Long.MAX_VALUE - 99) / 100) return null // 溢出保护

        val fracCents = when (fracPart.length) {
            0 -> 0L
            1 -> (fracPart[0] - '0') * 10L
            2 -> (fracPart[0] - '0') * 10L + (fracPart[1] - '0')
            else -> return null
        }
        return whole * 100 + fracCents
    }
}
