package com.qiushang.yinji.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.withTransaction
import com.qiushang.yinji.util.Dates
import com.qiushang.yinji.widget.YinjiWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 单一写入口：所有会改变卡片内容的操作都从这里过，写事务成功后统一刷新小组件。
 *
 * 照搬 android/socialite 官方 Glance 样例的 Repository 收口做法（写 DAO 之后
 * `SociaLiteAppWidget().updateAll(appContext)`），避免刷新散落在各个 ViewModel 导致后台入口漏刷。
 * 无 Hilt：用 AppGraph 单例取 DAO（对照 home-assistant/android 用 EntryPoints 取仓库的等价做法）。
 */
class YinjiRepository(private val appContext: Context) {

    private val db = AppDatabase.get(appContext)
    private val habits = db.habitDao()
    private val txns = db.transactionDao()
    private val notes = db.noteDao()
    private val todos = db.todoDao()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 写后刷新：不阻塞当前写入，统一在此收口。 */
    private fun refreshWidget() {
        scope.launch {
            runCatching { YinjiWidget().updateAll(appContext) }
        }
    }

    // ---- 习惯 ----
    fun habitsFlow(): Flow<List<HabitEntity>> = habits.habits()
    fun completionsFlow(): Flow<List<HabitCompletionEntity>> = habits.allCompletions()
    suspend fun habit(id: Long): HabitEntity? = habits.habit(id)
    suspend fun habitsSnapshot(): List<HabitEntity> = habits.habitsSnapshot()

    suspend fun addHabit(name: String, emoji: String, colorArgb: Int, weekdays: String): Long {
        val now = System.currentTimeMillis()
        val id = habits.insert(
            HabitEntity(name = name, emoji = emoji, colorArgb = colorArgb, weekdays = weekdays, createdAt = now, updatedAt = now)
        )
        refreshWidget()
        return id
    }

    suspend fun updateHabit(h: HabitEntity) {
        habits.update(h.copy(updatedAt = System.currentTimeMillis()))
        refreshWidget()
    }

    suspend fun deleteHabit(id: Long) {
        val now = System.currentTimeMillis()
        habits.softDelete(id, now)
        refreshWidget()
    }

    /** 打卡 / 取消 / 补卡：同一入口，按真实记录重算 streak。 */
    suspend fun toggleCompletion(habitId: Long, date: String) {
        val now = System.currentTimeMillis()
        val existing = habits.completion(habitId, date)
        if (existing == null || existing.deletedAt != null) {
            habits.upsertCompletion(
                HabitCompletionEntity(habitId = habitId, date = date, createdAt = now, updatedAt = now, deletedAt = null)
            )
        } else {
            habits.cancelCompletion(habitId, date, now)
        }
        refreshWidget()
    }

    suspend fun setCompletion(habitId: Long, date: String, done: Boolean) {
        val now = System.currentTimeMillis()
        if (done) {
            habits.upsertCompletion(
                HabitCompletionEntity(habitId = habitId, date = date, createdAt = now, updatedAt = now, deletedAt = null)
            )
        } else {
            habits.cancelCompletion(habitId, date, now)
        }
        refreshWidget()
    }

    fun completionsForHabitFlow(habitId: Long): Flow<Set<String>> =
        habits.allCompletions().map { list ->
            list.filter { it.habitId == habitId && it.deletedAt == null }
                .map { it.date }.toSet()
        }

    // ---- 记账 ----
    fun txnsInRange(from: String, to: String): Flow<List<TransactionEntity>> = txns.inRange(from, to)
    fun categorySums(type: String, from: String, to: String): Flow<List<CategorySum>> = txns.categorySums(type, from, to)
    fun totalByType(type: String, from: String, to: String): Flow<Long> = txns.totalByType(type, from, to)

    suspend fun addTransaction(type: String, amountCents: Long, categoryKey: String, note: String, date: String): Long {
        val now = System.currentTimeMillis()
        val id = txns.insert(
            TransactionEntity(type = type, amountCents = amountCents, categoryKey = categoryKey, note = note, date = date, createdAt = now, updatedAt = now)
        )
        refreshWidget()
        return id
    }

    suspend fun updateTransaction(t: TransactionEntity) {
        txns.update(t.copy(updatedAt = System.currentTimeMillis()))
        refreshWidget()
    }

    suspend fun deleteTransaction(id: Long) {
        val now = System.currentTimeMillis()
        txns.softDelete(id, now)
        refreshWidget()
    }

    // ---- 备忘录 ----
    fun notesFlow(): Flow<List<NoteEntity>> = notes.all()

    suspend fun addNote(text: String): Long {
        val now = System.currentTimeMillis()
        val id = notes.insert(NoteEntity(text = text, createdAt = now, updatedAt = now))
        refreshWidget()
        return id
    }

    suspend fun updateNote(n: NoteEntity) {
        notes.update(n.copy(updatedAt = System.currentTimeMillis()))
        refreshWidget()
    }

    suspend fun setNotePinned(id: Long, pinned: Boolean) {
        notes.setPinned(id, pinned, System.currentTimeMillis())
        refreshWidget()
    }

    suspend fun deleteNote(id: Long) {
        val now = System.currentTimeMillis()
        notes.softDelete(id, now)
        refreshWidget()
    }

    // ---- 待办 ----
    fun todosFlow(): Flow<List<TodoEntity>> = todos.all()

    suspend fun addTodo(title: String, dueDate: String?, dueTime: String?): Long {
        val now = System.currentTimeMillis()
        val id = todos.insert(TodoEntity(title = title, dueDate = dueDate, dueTime = dueTime, createdAt = now, updatedAt = now))
        refreshWidget()
        return id
    }

    suspend fun updateTodo(t: TodoEntity) {
        todos.update(t.copy(updatedAt = System.currentTimeMillis()))
        refreshWidget()
    }

    /** 完成只标 done，不删行 —— App 内「已完成」可一键恢复（定稿覆盖「不可撤销」）。 */
    suspend fun setTodoDone(id: Long, done: Boolean) {
        todos.setDone(id, done, System.currentTimeMillis())
        refreshWidget()
    }

    suspend fun deleteTodo(id: Long) {
        val now = System.currentTimeMillis()
        todos.softDelete(id, now)
        refreshWidget()
    }

    // ---- 卡片数据（供 Glance 一次性快照，不依赖 Flow 重组） ----
    suspend fun widgetSnapshot(): WidgetSnapshot {
        val today = Dates.today()
        val todayStr = Dates.str(today)
        val hs = habits.habitsSnapshot()
        val comps = habits.completionsSnapshot()
        val doneByHabit = comps.filter { it.deletedAt == null }.groupBy { it.habitId }
        val habitRows = hs.filter { HabitStatsCalculator.isScheduled(it, today) }
            .filter { h -> doneByHabit[h.id].orEmpty().none { it.date == todayStr } }
            .map { WidgetItem(it.id, "${it.emoji} ${it.name}", it.colorArgb) }
        val todoRows = todos.undoneSnapshot().map {
            WidgetItem(it.id, it.title, 0xFF2E9C8E.toInt())
        }
        return WidgetSnapshot(habitRows, todoRows)
    }

    // ---- 导出 / 备份恢复（纯本地 JSON，不联网；一期无会员墙） ----
    suspend fun exportJson(): String = BackupCodec.export(
        habits.allSnapshot(), habits.allCompletionsSnapshot(), txns.allSnapshot(), notes.allSnapshot(), todos.allSnapshot()
    )

    suspend fun importJson(json: String): Result<Unit> = runCatching {
        val b = BackupCodec.import(json)
        // 跨 5 张表的「清空 + 回灌」必须在同一事务内：任何一行插入失败
        // （坏备份 / 外键约束冲突）都要整体回滚，不能把用户原数据删光后回不去。
        db.withTransaction {
            habits.clearCompletions()
            habits.clear()
            txns.clear()
            notes.clear()
            todos.clear()
            habits.insertAll(b.habits)
            habits.insertCompletions(b.completions)
            txns.insertAll(b.transactions)
            notes.insertAll(b.notes)
            todos.insertAll(b.todos)
        }
        refreshWidget()
    }
}

data class WidgetItem(val id: Long, val label: String, val colorArgb: Int)
data class WidgetSnapshot(val habits: List<WidgetItem>, val todos: List<WidgetItem>)

/** 无 Hilt 的极简单例容器（对照 HA / socialite 用 DI 取仓库的等价物）。 */
object AppGraph {
    @Volatile
    private var repo: YinjiRepository? = null

    fun repository(context: Context): YinjiRepository =
        repo ?: synchronized(this) {
            repo ?: YinjiRepository(context.applicationContext).also { repo = it }
        }
}
