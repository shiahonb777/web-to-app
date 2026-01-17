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
import com.webtoapp.data.model.WebApp

/**
 * Room数据库
 */
@Database(
    entities = [WebApp::class],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun webAppDao(): WebAppDao

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
                        android.util.Log.w("AppDatabase", "迁移 $startVersion->$endVersion 跳过: ${e.message}")
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
        private val MIGRATION_11_13 = object : Migration(11, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN autoStartConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "迁移 11->13 autoStartConfig 跳过: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN forcedRunConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "迁移 11->13 forcedRunConfig 跳过: ${e.message}")
                }
            }
        }
        private val MIGRATION_10_13 = object : Migration(10, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN autoStartConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "迁移 10->13 autoStartConfig 跳过: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN forcedRunConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "迁移 10->13 forcedRunConfig 跳过: ${e.message}")
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
                    MIGRATION_10_13
                )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
