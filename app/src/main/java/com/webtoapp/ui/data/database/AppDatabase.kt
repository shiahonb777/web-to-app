package com.webtoapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.webtoapp.ui.data.converter.Converters
import com.webtoapp.data.dao.WebAppDao
import com.webtoapp.data.dao.AppCategoryDao
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.AppCategory

/**
 * Room数据库
 */
@Database(
    entities = [WebApp::class, AppCategory::class],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun webAppDao(): WebAppDao
    abstract fun appCategoryDao(): AppCategoryDao

    companion object {
        private const val DATABASE_NAME = "webtoapp.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * 关闭数据库连接
         * 通常在 Application.onTerminate 或测试时调用
         */
        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        /**
         * 通用迁移：添加新列（如果不存在）
         */
        private fun createAddColumnMigration(
            startVersion: Int,
            endVersion: Int,
            columnName: String,
            columnType: String = "TEXT",
            defaultValue: String = "NULL"
        ): Migration {
            return object : Migration(startVersion, endVersion) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    try {
                        db.execSQL("ALTER TABLE web_apps ADD COLUMN $columnName $columnType DEFAULT $defaultValue")
                    } catch (e: Exception) {
                        // 列可能已存在，忽略错误
                        android.util.Log.w("AppDatabase", "Migration $startVersion->$endVersion skipped: ${e.message}")
                    }
                }
            }
        }

        // 迁移定义 - autoStartConfig
        private val MIGRATION_11_12 = createAddColumnMigration(11, 12, "autoStartConfig")
        private val MIGRATION_10_12 = createAddColumnMigration(10, 12, "autoStartConfig")
        private val MIGRATION_9_12 = createAddColumnMigration(9, 12, "autoStartConfig")
        private val MIGRATION_8_12 = createAddColumnMigration(8, 12, "autoStartConfig")

        // 迁移定义 - forcedRunConfig (版本 12 -> 13)
        private val MIGRATION_12_13 = createAddColumnMigration(12, 13, "forcedRunConfig")

        // 迁移定义 - blackTechConfig, disguiseConfig (版本 13 -> 14)
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN blackTechConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 13->14 blackTechConfig skipped: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN disguiseConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 13->14 disguiseConfig skipped: ${e.message}")
                }
            }
        }

        // 迁移定义 - 添加 categoryId 和 app_categories 表 (版本 14 -> 15)
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建 app_categories 表
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS app_categories (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            icon TEXT NOT NULL DEFAULT '📁',
                            color TEXT NOT NULL DEFAULT '#6200EE',
                            sortOrder INTEGER NOT NULL DEFAULT 0,
                            createdAt INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent())
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 14->15 create app_categories skipped: ${e.message}")
                }
                // 添加 categoryId 列
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN categoryId INTEGER DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 14->15 categoryId skipped: ${e.message}")
                }
            }
        }
        private val MIGRATION_11_13 = object : Migration(11, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN autoStartConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 11->13 autoStartConfig skipped: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN forcedRunConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 11->13 forcedRunConfig skipped: ${e.message}")
                }
            }
        }
        private val MIGRATION_10_13 = object : Migration(10, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN autoStartConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 10->13 autoStartConfig skipped: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN forcedRunConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 10->13 forcedRunConfig skipped: ${e.message}")
                }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(
                    MIGRATION_8_12,
                    MIGRATION_9_12,
                    MIGRATION_10_12,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_11_13,
                    MIGRATION_10_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15
                )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
