package com.qiushang.yinji.data

/**
 * 记账分类常量表 —— 不建表、不存库。
 *
 * 为什么：一期单币种 ¥、分类不需要用户自定义排序/删除状态，硬编码常量表少一处可变状态、
 * 少一张表、少一类「分类被删了历史账目怎么显示」的 bug。对齐 PRD「约 10+3 个一级分类」。
 * 二期若需要「图标/颜色可再编辑」，再落库并加迁移即可，此处结构已按可迁移的形态定义。
 *
 * 「奶油薄荷 · 小芽生长」配色：分类色统一降饱和、提明度，做成柔和奶油色系小方块。
 */
data class CategoryDef(
    val key: String,
    val label: String,
    val emoji: String,
    val colorArgb: Int,
    // "expense" | "income"
    val type: String
)

object Categories {
    val expense = listOf(
        CategoryDef("food", "餐饮", "🍜", 0xFFF0A868.toInt(), "expense"),
        CategoryDef("transport", "交通", "🚇", 0xFF86B6DE.toInt(), "expense"),
        CategoryDef("shopping", "购物", "🛍️", 0xFFF2A0AC.toInt(), "expense"),
        CategoryDef("home", "居住", "🏠", 0xFFB29BD9.toInt(), "expense"),
        CategoryDef("daily", "日用", "🧻", 0xFF8FCDB8.toInt(), "expense"),
        CategoryDef("fun", "娱乐", "🎮", 0xFFE9C46A.toInt(), "expense"),
        CategoryDef("health", "医疗健康", "💊", 0xFF8FC3CE.toInt(), "expense"),
        CategoryDef("social", "人情社交", "🎁", 0xFFEBA98E.toInt(), "expense"),
        CategoryDef("study", "学习工作", "📚", 0xFF9DBE8C.toInt(), "expense"),
        CategoryDef("other", "其他", "📦", 0xFFB3BAB5.toInt(), "expense")
    )

    val income = listOf(
        CategoryDef("salary", "工资", "💰", 0xFF3A9A80.toInt(), "income"),
        CategoryDef("bonus", "奖金", "🧧", 0xFF6CBE9C.toInt(), "income"),
        CategoryDef("other_income", "其他收入", "➕", 0xFF9FD3B6.toInt(), "income")
    )

    val all: List<CategoryDef> = expense + income

    private val byKey: Map<String, CategoryDef> = all.associateBy { it.key }

    fun of(key: String): CategoryDef = byKey[key] ?: byKey.getValue("other")

    fun forType(type: String): List<CategoryDef> = if (type == "income") income else expense
}
