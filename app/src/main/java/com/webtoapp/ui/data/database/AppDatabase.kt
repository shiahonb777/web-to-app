package com.webtoapp.data.database

import android.content.Context
import com.webtoapp.core.logging.AppLogger
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
import com.webtoapp.core.stats.AppUsageStats
import com.webtoapp.core.stats.AppHealthRecord
import com.webtoapp.core.stats.AppUsageStatsDao

/**
 * Room数据库
 */
@Database(
    entities = [WebApp::class, AppCategory::class, AppUsageStats::class, AppHealthRecord::class],
    version = 33,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun webAppDao(): WebAppDao
    abstract fun appCategoryDao(): AppCategoryDao
    abstract fun appUsageStatsDao(): AppUsageStatsDao

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
         * 通用迁移：重建 web_apps 表（删除列时使用）
         * SQLite 不支持 DROP COLUMN，需要 CREATE → INSERT → DROP → RENAME 四步操作。
         * @param createTableSql 完整的 CREATE TABLE IF NOT EXISTS web_apps_new (...) 语句
         * @param columnNames 要保留的列名（逗号分隔），同时用于 INSERT INTO 和 SELECT
         * @param postSql 重建后要执行的额外 SQL（如重建索引）
         */
        private fun rebuildWebAppsTable(
            db: SupportSQLiteDatabase,
            createTableSql: String,
            columnNames: String,
            postSql: List<String> = emptyList()
        ) {
            db.execSQL(createTableSql)
            db.execSQL("INSERT INTO web_apps_new ($columnNames) SELECT $columnNames FROM web_apps")
            db.execSQL("DROP TABLE web_apps")
            db.execSQL("ALTER TABLE web_apps_new RENAME TO web_apps")
            postSql.forEach { db.execSQL(it) }
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
                        AppLogger.w("AppDatabase", "迁移 $startVersion->$endVersion 跳过: ${e.message}")
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
                    AppLogger.w("AppDatabase", "迁移 13->14 blackTechConfig 跳过: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN disguiseConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 13->14 disguiseConfig 跳过: ${e.message}")
                }
            }
        }
        
        // 迁移定义 - 添加 categoryId 和 app_categories 表 (版本 14 -> 15)
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create app_categories 表
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
                    AppLogger.w("AppDatabase", "迁移 14->15 创建 app_categories 跳过: ${e.message}")
                }
                // 添加 categoryId 列
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN categoryId INTEGER DEFAULT NULL")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 14->15 categoryId 跳过: ${e.message}")
                }
            }
        }
        private val MIGRATION_11_13 = object : Migration(11, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN autoStartConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 11->13 autoStartConfig 跳过: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN forcedRunConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 11->13 forcedRunConfig 跳过: ${e.message}")
                }
            }
        }
        private val MIGRATION_10_13 = object : Migration(10, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN autoStartConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 10->13 autoStartConfig 跳过: ${e.message}")
                }
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN forcedRunConfig TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 10->13 forcedRunConfig 跳过: ${e.message}")
                }
            }
        }
        
        // 迁移定义 - 删除浏览器扩展字段 (版本 16 -> 17)
        // SQLite 不支持直接删除列，需要重建表
        // Migration 15 -> 16: no-op (schema unchanged, version bump only)
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 15->16: no schema changes")
            }
        }
        
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 16->17: 删除浏览器扩展字段")
                
                // 1. 创建新表（不包含 browserExtensionEnabled 和 browserExtensionIds）
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS web_apps_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        url TEXT NOT NULL,
                        iconPath TEXT,
                        packageName TEXT,
                        appType TEXT NOT NULL DEFAULT 'WEB',
                        mediaConfig TEXT,
                        galleryConfig TEXT,
                        htmlConfig TEXT,
                        activationEnabled INTEGER NOT NULL DEFAULT 0,
                        activationCodes TEXT NOT NULL DEFAULT '[]',
                        activationCodeList TEXT NOT NULL DEFAULT '[]',
                        activationRequireEveryTime INTEGER NOT NULL DEFAULT 0,
                        isActivated INTEGER NOT NULL DEFAULT 0,
                        adsEnabled INTEGER NOT NULL DEFAULT 0,
                        adConfig TEXT,
                        announcementEnabled INTEGER NOT NULL DEFAULT 0,
                        announcement TEXT,
                        adBlockEnabled INTEGER NOT NULL DEFAULT 0,
                        adBlockRules TEXT NOT NULL DEFAULT '[]',
                        webViewConfig TEXT NOT NULL,
                        splashEnabled INTEGER NOT NULL DEFAULT 0,
                        splashConfig TEXT,
                        bgmEnabled INTEGER NOT NULL DEFAULT 0,
                        bgmConfig TEXT,
                        apkExportConfig TEXT,
                        themeType TEXT NOT NULL DEFAULT 'AURORA',
                        translateEnabled INTEGER NOT NULL DEFAULT 0,
                        translateConfig TEXT,
                        extensionModuleIds TEXT NOT NULL DEFAULT '[]',
                        userscriptEnabled INTEGER NOT NULL DEFAULT 0,
                        userscriptIds TEXT NOT NULL DEFAULT '[]',
                        autoStartConfig TEXT,
                        forcedRunConfig TEXT,
                        blackTechConfig TEXT,
                        disguiseConfig TEXT,
                        categoryId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // 2. 复制数据（排除删除的列）
                db.execSQL("""
                    INSERT INTO web_apps_new (
                        id, name, url, iconPath, packageName, appType,
                        mediaConfig, galleryConfig, htmlConfig,
                        activationEnabled, activationCodes, activationCodeList, activationRequireEveryTime, isActivated,
                        adsEnabled, adConfig,
                        announcementEnabled, announcement,
                        adBlockEnabled, adBlockRules,
                        webViewConfig,
                        splashEnabled, splashConfig,
                        bgmEnabled, bgmConfig,
                        apkExportConfig, themeType,
                        translateEnabled, translateConfig,
                        extensionModuleIds,
                        userscriptEnabled, userscriptIds,
                        autoStartConfig, forcedRunConfig,
                        blackTechConfig, disguiseConfig,
                        categoryId, createdAt, updatedAt
                    )
                    SELECT 
                        id, name, url, iconPath, packageName, appType,
                        mediaConfig, galleryConfig, htmlConfig,
                        activationEnabled, activationCodes, activationCodeList, activationRequireEveryTime, isActivated,
                        adsEnabled, adConfig,
                        announcementEnabled, announcement,
                        adBlockEnabled, adBlockRules,
                        webViewConfig,
                        splashEnabled, splashConfig,
                        bgmEnabled, bgmConfig,
                        apkExportConfig, themeType,
                        translateEnabled, translateConfig,
                        extensionModuleIds,
                        userscriptEnabled, userscriptIds,
                        autoStartConfig, forcedRunConfig,
                        blackTechConfig, disguiseConfig,
                        categoryId, createdAt, updatedAt
                    FROM web_apps
                """.trimIndent())
                
                // 3. 删除旧表
                db.execSQL("DROP TABLE web_apps")
                
                // 4. 重命名新表
                db.execSQL("ALTER TABLE web_apps_new RENAME TO web_apps")
                
                AppLogger.i("AppDatabase", "迁移 16->17 完成")
            }
        }
        
        // 迁移定义 - 删除油猴脚本字段 (版本 17 -> 18)
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 17->18: 删除油猴脚本字段")
                
                // 1. 创建新表（不包含 userscriptEnabled 和 userscriptIds）
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS web_apps_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        url TEXT NOT NULL,
                        iconPath TEXT,
                        packageName TEXT,
                        appType TEXT NOT NULL DEFAULT 'WEB',
                        mediaConfig TEXT,
                        galleryConfig TEXT,
                        htmlConfig TEXT,
                        activationEnabled INTEGER NOT NULL DEFAULT 0,
                        activationCodes TEXT NOT NULL DEFAULT '[]',
                        activationCodeList TEXT NOT NULL DEFAULT '[]',
                        activationRequireEveryTime INTEGER NOT NULL DEFAULT 0,
                        isActivated INTEGER NOT NULL DEFAULT 0,
                        adsEnabled INTEGER NOT NULL DEFAULT 0,
                        adConfig TEXT,
                        announcementEnabled INTEGER NOT NULL DEFAULT 0,
                        announcement TEXT,
                        adBlockEnabled INTEGER NOT NULL DEFAULT 0,
                        adBlockRules TEXT NOT NULL DEFAULT '[]',
                        webViewConfig TEXT NOT NULL,
                        splashEnabled INTEGER NOT NULL DEFAULT 0,
                        splashConfig TEXT,
                        bgmEnabled INTEGER NOT NULL DEFAULT 0,
                        bgmConfig TEXT,
                        apkExportConfig TEXT,
                        themeType TEXT NOT NULL DEFAULT 'AURORA',
                        translateEnabled INTEGER NOT NULL DEFAULT 0,
                        translateConfig TEXT,
                        extensionModuleIds TEXT NOT NULL DEFAULT '[]',
                        autoStartConfig TEXT,
                        forcedRunConfig TEXT,
                        blackTechConfig TEXT,
                        disguiseConfig TEXT,
                        categoryId INTEGER,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                
                // 2. 复制数据（排除删除的列）
                db.execSQL("""
                    INSERT INTO web_apps_new (
                        id, name, url, iconPath, packageName, appType,
                        mediaConfig, galleryConfig, htmlConfig,
                        activationEnabled, activationCodes, activationCodeList, activationRequireEveryTime, isActivated,
                        adsEnabled, adConfig,
                        announcementEnabled, announcement,
                        adBlockEnabled, adBlockRules,
                        webViewConfig,
                        splashEnabled, splashConfig,
                        bgmEnabled, bgmConfig,
                        apkExportConfig, themeType,
                        translateEnabled, translateConfig,
                        extensionModuleIds,
                        autoStartConfig, forcedRunConfig,
                        blackTechConfig, disguiseConfig,
                        categoryId, createdAt, updatedAt
                    )
                    SELECT 
                        id, name, url, iconPath, packageName, appType,
                        mediaConfig, galleryConfig, htmlConfig,
                        activationEnabled, activationCodes, activationCodeList, activationRequireEveryTime, isActivated,
                        adsEnabled, adConfig,
                        announcementEnabled, announcement,
                        adBlockEnabled, adBlockRules,
                        webViewConfig,
                        splashEnabled, splashConfig,
                        bgmEnabled, bgmConfig,
                        apkExportConfig, themeType,
                        translateEnabled, translateConfig,
                        extensionModuleIds,
                        autoStartConfig, forcedRunConfig,
                        blackTechConfig, disguiseConfig,
                        categoryId, createdAt, updatedAt
                    FROM web_apps
                """.trimIndent())
                
                // 3. 删除旧表
                db.execSQL("DROP TABLE web_apps")
                
                // 4. 重命名新表
                db.execSQL("ALTER TABLE web_apps_new RENAME TO web_apps")
                
                AppLogger.i("AppDatabase", "迁移 17->18 完成")
            }
        }

        // 迁移定义 - activationDialogConfig (版本 18 -> 19)
        private val MIGRATION_18_19 = createAddColumnMigration(18, 19, "activationDialogConfig")
        
        // 迁移定义 - extensionFabIcon (版本 19 -> 20)
        private val MIGRATION_19_20 = createAddColumnMigration(19, 20, "extensionFabIcon")
        
        // 迁移定义 - wordpressConfig (版本 21 -> 22)
        private val MIGRATION_21_22 = createAddColumnMigration(21, 22, "wordpressConfig")
        
        // 迁移定义 - nodejsConfig (版本 22 -> 23)
        private val MIGRATION_22_23 = createAddColumnMigration(22, 23, "nodejsConfig")
        
        // 迁移定义 - phpAppConfig (版本 23 -> 24)
        private val MIGRATION_23_24 = createAddColumnMigration(23, 24, "phpAppConfig")
        
        // 迁移定义 - pythonAppConfig (版本 24 -> 25)
        private val MIGRATION_24_25 = createAddColumnMigration(24, 25, "pythonAppConfig")
        
        // 迁移定义 - goAppConfig (版本 25 -> 26)
        private val MIGRATION_25_26 = createAddColumnMigration(25, 26, "goAppConfig")
        
        // 迁移定义 - docsSiteConfig (版本 26 -> 27)
        private val MIGRATION_26_27 = createAddColumnMigration(26, 27, "docsSiteConfig")
        
        // 迁移定义 - 添加使用统计和健康检测表 (版本 28 -> 29)
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 28->29: 添加使用统计和健康检测表")
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_usage_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        appId INTEGER NOT NULL,
                        launchCount INTEGER NOT NULL DEFAULT 0,
                        totalUsageMs INTEGER NOT NULL DEFAULT 0,
                        lastUsedAt INTEGER NOT NULL DEFAULT 0,
                        lastSessionDurationMs INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (appId) REFERENCES web_apps(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_app_usage_stats_appId ON app_usage_stats(appId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_stats_lastUsedAt ON app_usage_stats(lastUsedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_stats_launchCount ON app_usage_stats(launchCount)")
                
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_health_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        appId INTEGER NOT NULL,
                        url TEXT NOT NULL,
                        status TEXT NOT NULL DEFAULT 'UNKNOWN',
                        responseTimeMs INTEGER NOT NULL DEFAULT 0,
                        httpStatusCode INTEGER NOT NULL DEFAULT 0,
                        errorMessage TEXT,
                        checkedAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (appId) REFERENCES web_apps(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_health_records_appId ON app_health_records(appId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_health_records_checkedAt ON app_health_records(checkedAt)")
                
                AppLogger.i("AppDatabase", "迁移 28->29 完成")
            }
        }
        
        // 迁移定义 - cloudConfig (版本 29 -> 30)
        private val MIGRATION_29_30 = createAddColumnMigration(29, 30, "cloudConfig")

        // 迁移定义 - multiWebConfig (版本 30 -> 31)
        private val MIGRATION_30_31 = createAddColumnMigration(30, 31, "multiWebConfig")
        
        // 迁移定义 - browserDisguiseConfig (版本 31 -> 32)
        private val MIGRATION_31_32 = createAddColumnMigration(31, 32, "browserDisguiseConfig")
        
        // 迁移定义 - deviceDisguiseConfig (版本 32 -> 33)
        private val MIGRATION_32_33 = createAddColumnMigration(32, 33, "deviceDisguiseConfig")
        
        // 迁移定义 - 删除 docsSiteConfig (版本 27 -> 28)
        // 使用 rebuildWebAppsTable 辅助方法简化表重建
        private val MIGRATION_27_28_COLUMNS = """
            id, name, url, iconPath, packageName, appType,
            mediaConfig, galleryConfig, htmlConfig,
            wordpressConfig, nodejsConfig, phpAppConfig, pythonAppConfig, goAppConfig,
            activationEnabled, activationCodes, activationCodeList, activationRequireEveryTime, isActivated,
            adsEnabled, adConfig,
            announcementEnabled, announcement,
            adBlockEnabled, adBlockRules,
            webViewConfig,
            splashEnabled, splashConfig,
            bgmEnabled, bgmConfig,
            apkExportConfig, themeType,
            translateEnabled, translateConfig,
            extensionModuleIds, extensionFabIcon,
            autoStartConfig, forcedRunConfig,
            blackTechConfig, disguiseConfig,
            activationDialogConfig,
            categoryId, createdAt, updatedAt
        """.trimIndent()
        
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 27->28: 删除 docsSiteConfig 字段")
                
                rebuildWebAppsTable(
                    db = db,
                    createTableSql = """
                        CREATE TABLE IF NOT EXISTS web_apps_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            name TEXT NOT NULL,
                            url TEXT NOT NULL,
                            iconPath TEXT,
                            packageName TEXT,
                            appType TEXT NOT NULL DEFAULT 'WEB',
                            mediaConfig TEXT,
                            galleryConfig TEXT,
                            htmlConfig TEXT,
                            wordpressConfig TEXT,
                            nodejsConfig TEXT,
                            phpAppConfig TEXT,
                            pythonAppConfig TEXT,
                            goAppConfig TEXT,
                            activationEnabled INTEGER NOT NULL DEFAULT 0,
                            activationCodes TEXT NOT NULL DEFAULT '[]',
                            activationCodeList TEXT NOT NULL DEFAULT '[]',
                            activationRequireEveryTime INTEGER NOT NULL DEFAULT 0,
                            isActivated INTEGER NOT NULL DEFAULT 0,
                            adsEnabled INTEGER NOT NULL DEFAULT 0,
                            adConfig TEXT,
                            announcementEnabled INTEGER NOT NULL DEFAULT 0,
                            announcement TEXT,
                            adBlockEnabled INTEGER NOT NULL DEFAULT 0,
                            adBlockRules TEXT NOT NULL DEFAULT '[]',
                            webViewConfig TEXT NOT NULL,
                            splashEnabled INTEGER NOT NULL DEFAULT 0,
                            splashConfig TEXT,
                            bgmEnabled INTEGER NOT NULL DEFAULT 0,
                            bgmConfig TEXT,
                            apkExportConfig TEXT,
                            themeType TEXT NOT NULL DEFAULT 'AURORA',
                            translateEnabled INTEGER NOT NULL DEFAULT 0,
                            translateConfig TEXT,
                            extensionModuleIds TEXT NOT NULL DEFAULT '[]',
                            extensionFabIcon TEXT,
                            autoStartConfig TEXT,
                            forcedRunConfig TEXT,
                            blackTechConfig TEXT,
                            disguiseConfig TEXT,
                            activationDialogConfig TEXT,
                            categoryId INTEGER,
                            createdAt INTEGER NOT NULL DEFAULT 0,
                            updatedAt INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent(),
                    columnNames = MIGRATION_27_28_COLUMNS,
                    postSql = listOf(
                        "CREATE INDEX IF NOT EXISTS index_web_apps_updatedAt ON web_apps(updatedAt)",
                        "CREATE INDEX IF NOT EXISTS index_web_apps_categoryId ON web_apps(categoryId)",
                        "CREATE INDEX IF NOT EXISTS index_web_apps_isActivated ON web_apps(isActivated)"
                    )
                )
                
                AppLogger.i("AppDatabase", "迁移 27->28 完成")
            }
        }
        
        // 迁移定义 - 添加索引提升查询性能 (版本 20 -> 21)
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_web_apps_updatedAt ON web_apps(updatedAt)")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 20->21 updatedAt 索引跳过: ${e.message}")
                }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_web_apps_categoryId ON web_apps(categoryId)")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 20->21 categoryId 索引跳过: ${e.message}")
                }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_web_apps_isActivated ON web_apps(isActivated)")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 20->21 isActivated 索引跳过: ${e.message}")
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
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                    MIGRATION_26_27,
                    MIGRATION_27_28,
                    MIGRATION_28_29,
                    MIGRATION_29_30,
                    MIGRATION_30_31,
                    MIGRATION_31_32,
                    MIGRATION_32_33
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
                .build()
        }
    }
}
