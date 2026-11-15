package com.qiushang.yinji.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        HabitEntity::class,
        HabitCompletionEntity::class,
        TransactionEntity::class,
        NoteEntity::class,
        TodoEntity::class,
        ClockInEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun transactionDao(): TransactionDao
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao
    abstract fun clockInDao(): ClockInDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v1 -> v2：新增「上班打卡」表。
         * 必须显式迁移，否则 fallbackToDestructiveMigration 会把真机上已有的
         * 习惯/账目/备忘/待办全部清空。CREATE TABLE 与索引逐字对齐 Room 生成的 schema，
         * 这样迁移完成后 Room 的 schema 校验才能通过。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `clock_in` (" +
                        "`date` TEXT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`time` TEXT NOT NULL, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "`lateMinutes` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL, " +
                        "`deletedAt` INTEGER, " +
                        "`deviceId` TEXT NOT NULL, " +
                        "PRIMARY KEY(`date`, `type`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_clock_in_date` ON `clock_in` (`date`)"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yinji.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    // 仅作更高版本的兜底；1->2 走上面的显式迁移，不会清数据。
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
