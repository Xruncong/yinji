package com.qiushang.yinji.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地导出 / 备份恢复：JSON，用 Android 自带的 org.json，零新依赖。
 *
 * 照商业产品反面教训（导出锁会员、网易有钱停服删数据）：一期导出完全免费、无需登录、
 * 不设笔数限制；软删字段一并导出，恢复后历史与统计口径不变。
 */
data class Backup(
    val habits: List<HabitEntity>,
    val completions: List<HabitCompletionEntity>,
    val transactions: List<TransactionEntity>,
    val notes: List<NoteEntity>,
    val todos: List<TodoEntity>
)

object BackupCodec {
    private const val VERSION = 1

    fun export(
        habits: List<HabitEntity>,
        completions: List<HabitCompletionEntity>,
        transactions: List<TransactionEntity>,
        notes: List<NoteEntity>,
        todos: List<TodoEntity>
    ): String {
        val root = JSONObject()
        root.put("app", "yinji")
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("habits", JSONArray().apply {
            habits.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("name", it.name); put("emoji", it.emoji)
                    put("colorArgb", it.colorArgb); put("weekdays", it.weekdays)
                    put("createdAt", it.createdAt); put("updatedAt", it.updatedAt)
                    if (it.deletedAt != null) put("deletedAt", it.deletedAt) else put("deletedAt", JSONObject.NULL)
                    put("deviceId", it.deviceId)
                })
            }
        })
        root.put("completions", JSONArray().apply {
            completions.forEach {
                put(JSONObject().apply {
                    put("habitId", it.habitId); put("date", it.date)
                    put("createdAt", it.createdAt); put("updatedAt", it.updatedAt)
                    if (it.deletedAt != null) put("deletedAt", it.deletedAt) else put("deletedAt", JSONObject.NULL)
                    put("deviceId", it.deviceId)
                })
            }
        })
        root.put("transactions", JSONArray().apply {
            transactions.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("type", it.type); put("amountCents", it.amountCents)
                    put("categoryKey", it.categoryKey); put("note", it.note); put("date", it.date)
                    put("createdAt", it.createdAt); put("updatedAt", it.updatedAt)
                    if (it.deletedAt != null) put("deletedAt", it.deletedAt) else put("deletedAt", JSONObject.NULL)
                    put("deviceId", it.deviceId)
                })
            }
        })
        root.put("notes", JSONArray().apply {
            notes.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("text", it.text); put("pinned", it.pinned)
                    put("createdAt", it.createdAt); put("updatedAt", it.updatedAt)
                    if (it.deletedAt != null) put("deletedAt", it.deletedAt) else put("deletedAt", JSONObject.NULL)
                    put("deviceId", it.deviceId)
                })
            }
        })
        root.put("todos", JSONArray().apply {
            todos.forEach {
                put(JSONObject().apply {
                    put("id", it.id); put("title", it.title); put("done", it.done)
                    if (it.dueDate != null) put("dueDate", it.dueDate) else put("dueDate", JSONObject.NULL)
                    if (it.dueTime != null) put("dueTime", it.dueTime) else put("dueTime", JSONObject.NULL)
                    put("createdAt", it.createdAt); put("updatedAt", it.updatedAt)
                    if (it.deletedAt != null) put("deletedAt", it.deletedAt) else put("deletedAt", JSONObject.NULL)
                    put("deviceId", it.deviceId)
                })
            }
        })
        return root.toString(2)
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private fun JSONObject.optStrOrNull(key: String): String? =
        if (isNull(key)) null else optString(key)

    fun import(json: String): Backup {
        val root = JSONObject(json)
        require(root.optString("app") == "yinji") { "不是印记的备份文件" }
        require(root.optInt("version") <= VERSION) { "备份版本比当前 App 更新，请先升级" }

        fun arr(key: String): JSONArray = root.optJSONArray(key) ?: JSONArray()

        val habits = (0 until arr("habits").length()).map { i ->
            arr("habits").getJSONObject(i).let { o ->
                HabitEntity(
                    id = o.optLong("id"), name = o.optString("name"),
                    emoji = o.optString("emoji", "✅"),
                    colorArgb = o.optInt("colorArgb", 0xFF2E9C8E.toInt()),
                    weekdays = o.optString("weekdays", ""),
                    createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
                    deletedAt = o.optLongOrNull("deletedAt"), deviceId = o.optString("deviceId", "")
                )
            }
        }
        val completions = (0 until arr("completions").length()).map { i ->
            arr("completions").getJSONObject(i).let { o ->
                HabitCompletionEntity(
                    habitId = o.optLong("habitId"), date = o.optString("date"),
                    createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
                    deletedAt = o.optLongOrNull("deletedAt"), deviceId = o.optString("deviceId", "")
                )
            }
        }
        val transactions = (0 until arr("transactions").length()).map { i ->
            arr("transactions").getJSONObject(i).let { o ->
                TransactionEntity(
                    id = o.optLong("id"), type = o.optString("type"), amountCents = o.optLong("amountCents"),
                    categoryKey = o.optString("categoryKey"), note = o.optString("note", ""),
                    date = o.optString("date"), createdAt = o.optLong("createdAt"),
                    updatedAt = o.optLong("updatedAt"), deletedAt = o.optLongOrNull("deletedAt"),
                    deviceId = o.optString("deviceId", "")
                )
            }
        }
        val notes = (0 until arr("notes").length()).map { i ->
            arr("notes").getJSONObject(i).let { o ->
                NoteEntity(
                    id = o.optLong("id"), text = o.optString("text"), pinned = o.optBoolean("pinned", false),
                    createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
                    deletedAt = o.optLongOrNull("deletedAt"), deviceId = o.optString("deviceId", "")
                )
            }
        }
        val todos = (0 until arr("todos").length()).map { i ->
            arr("todos").getJSONObject(i).let { o ->
                TodoEntity(
                    id = o.optLong("id"), title = o.optString("title"), done = o.optBoolean("done", false),
                    dueDate = o.optStrOrNull("dueDate"), dueTime = o.optStrOrNull("dueTime"),
                    createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
                    deletedAt = o.optLongOrNull("deletedAt"), deviceId = o.optString("deviceId", "")
                )
            }
        }
        return Backup(habits, completions, transactions, notes, todos)
    }
}
