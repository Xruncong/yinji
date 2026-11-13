package com.qiushang.yinji.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 所有表统一预留同步字段：updatedAt / deletedAt(软删) / deviceId。
 * 一期纯本地、单用户、不联网；这些字段只为二期同步留口子，不参与本期逻辑。
 */

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "✅",
    val colorArgb: Int = 0xFF2E9C8E.toInt(),
    // 周期："" = 每天；"1,2,3" = 周一/周二/周三（1=Mon … 7=Sun，ISO 周一起始）
    val weekdays: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val deviceId: String = ""
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("date")]
)
data class HabitCompletionEntity(
    val habitId: Long,
    // 本地日，yyyy-MM-dd。业务键，避开 DST / 时区换算
    val date: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // 非 null = 已取消打卡（软删）。重新打卡即把该行覆盖成 deletedAt=null
    val deletedAt: Long? = null,
    val deviceId: String = ""
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // "expense" | "income"
    val type: String,
    // 金额，单位：分。整数运算，绝不存 Double
    val amountCents: Long,
    val categoryKey: String,
    val note: String = "",
    val date: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val deviceId: String = ""
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // 纯文本，#标签 直接写在正文里（flomo 风格），不单独建标签表
    val text: String,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val deviceId: String = ""
)

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    // 完成只做状态标记，不删行 —— App 内「已完成」可一键恢复
    val done: Boolean = false,
    // 截止日 yyyy-MM-dd；null = 无截止
    val dueDate: String? = null,
    // 可选时间 HH:mm
    val dueTime: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val deviceId: String = ""
)
