package com.qiushang.yinji.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** GROUP BY 聚合结果（照 Compose-Expense / 鲨鱼记账的 SQL 聚合口径）。 */
data class CategorySum(
    @ColumnInfo(name = "categoryKey") val categoryKey: String,
    @ColumnInfo(name = "total") val total: Long
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun habits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun habitsSnapshot(): List<HabitEntity>

    @Query("SELECT * FROM habits")
    suspend fun allSnapshot(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun habit(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(h: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<HabitEntity>)

    @Update
    suspend fun update(h: HabitEntity)

    @Query("UPDATE habits SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("DELETE FROM habits")
    suspend fun clear()

    // ---- 打卡 ----
    @Query("SELECT * FROM habit_completions WHERE deletedAt IS NULL")
    fun allCompletions(): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE deletedAt IS NULL")
    suspend fun completionsSnapshot(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions")
    suspend fun allCompletionsSnapshot(): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun completion(habitId: Long, date: String): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(c: HabitCompletionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletions(list: List<HabitCompletionEntity>)

    @Query("UPDATE habit_completions SET deletedAt = :now, updatedAt = :now WHERE habitId = :habitId AND date = :date")
    suspend fun cancelCompletion(habitId: Long, date: String, now: Long)

    @Query("DELETE FROM habit_completions")
    suspend fun clearCompletions()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND date BETWEEN :from AND :to ORDER BY date DESC, createdAt DESC")
    fun inRange(from: String, to: String): Flow<List<TransactionEntity>>

    @Query(
        """SELECT categoryKey, SUM(amountCents) AS total FROM transactions
           WHERE deletedAt IS NULL AND type = :type AND date BETWEEN :from AND :to
           GROUP BY categoryKey ORDER BY total DESC"""
    )
    fun categorySums(type: String, from: String, to: String): Flow<List<CategorySum>>

    @Query(
        """SELECT COALESCE(SUM(amountCents), 0) FROM transactions
           WHERE deletedAt IS NULL AND type = :type AND date BETWEEN :from AND :to"""
    )
    fun totalByType(type: String, from: String, to: String): Flow<Long>

    @Query("SELECT * FROM transactions")
    suspend fun allSnapshot(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TransactionEntity>)

    @Update
    suspend fun update(t: TransactionEntity)

    @Query("UPDATE transactions SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("DELETE FROM transactions")
    suspend fun clear()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY pinned DESC, createdAt DESC")
    fun all(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes")
    suspend fun allSnapshot(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(n: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<NoteEntity>)

    @Update
    suspend fun update(n: NoteEntity)

    @Query("UPDATE notes SET pinned = :pinned, updatedAt = :now WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean, now: Long)

    @Query("UPDATE notes SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("DELETE FROM notes")
    suspend fun clear()
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE deletedAt IS NULL ORDER BY done ASC, CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC, createdAt DESC")
    fun all(): Flow<List<TodoEntity>>

    @Query("SELECT * FROM todos WHERE deletedAt IS NULL AND done = 0 ORDER BY CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC, createdAt ASC")
    suspend fun undoneSnapshot(): List<TodoEntity>

    @Query("SELECT * FROM todos")
    suspend fun allSnapshot(): List<TodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(t: TodoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<TodoEntity>)

    @Update
    suspend fun update(t: TodoEntity)

    @Query("UPDATE todos SET done = :done, updatedAt = :now WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, now: Long)

    @Query("UPDATE todos SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long)

    @Query("DELETE FROM todos")
    suspend fun clear()
}
