package com.webtoapp.data.database

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.webtoapp.data.converter.Converters
import com.webtoapp.data.dao.WebAppDao
import com.webtoapp.data.dao.AppCategoryDao
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.AppCategory
import com.webtoapp.core.stats.AppUsageStats
import com.webtoapp.core.stats.AppHealthRecord
import com.webtoapp.core.stats.AppUsageStatsDao




@Database(
    entities = [WebApp::class, AppCategory::class, AppUsageStats::class, AppHealthRecord::class],
    version = 36,
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





        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }








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

                        AppLogger.w("AppDatabase", "迁移 $startVersion->$endVersion 跳过: ${e.message}")
                    }
                }
            }
        }


        private val MIGRATION_11_12 = createAddColumnMigration(11, 12, "autoStartConfig")
        private val MIGRATION_10_12 = createAddColumnMigration(10, 12, "autoStartConfig")
        private val MIGRATION_9_12 = createAddColumnMigration(9, 12, "autoStartConfig")
        private val MIGRATION_8_12 = createAddColumnMigration(8, 12, "autoStartConfig")


        private val MIGRATION_12_13 = createAddColumnMigration(12, 13, "forcedRunConfig")


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


        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {

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




        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 15->16: no schema changes")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 16->17: 删除浏览器扩展字段")


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


                db.execSQL("DROP TABLE web_apps")


                db.execSQL("ALTER TABLE web_apps_new RENAME TO web_apps")

                AppLogger.i("AppDatabase", "迁移 16->17 完成")
            }
        }


        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 17->18: 删除油猴脚本字段")


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


                db.execSQL("DROP TABLE web_apps")


                db.execSQL("ALTER TABLE web_apps_new RENAME TO web_apps")

                AppLogger.i("AppDatabase", "迁移 17->18 完成")
            }
        }


        private val MIGRATION_18_19 = createAddColumnMigration(18, 19, "activationDialogConfig")


        private val MIGRATION_19_20 = createAddColumnMigration(19, 20, "extensionFabIcon")


        private val MIGRATION_21_22 = createAddColumnMigration(21, 22, "wordpressConfig")


        private val MIGRATION_22_23 = createAddColumnMigration(22, 23, "nodejsConfig")


        private val MIGRATION_23_24 = createAddColumnMigration(23, 24, "phpAppConfig")


        private val MIGRATION_24_25 = createAddColumnMigration(24, 25, "pythonAppConfig")


        private val MIGRATION_25_26 = createAddColumnMigration(25, 26, "goAppConfig")


        private val MIGRATION_26_27 = createAddColumnMigration(26, 27, "docsSiteConfig")


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


        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 29->30: no schema changes")
            }
        }


        private val MIGRATION_30_31 = createAddColumnMigration(30, 31, "multiWebConfig")


        private val MIGRATION_31_32 = createAddColumnMigration(31, 32, "browserDisguiseConfig")


        private val MIGRATION_32_33 = createAddColumnMigration(32, 33, "deviceDisguiseConfig")




        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 33->34: 添加 extensionEnabled 字段")
                try {
                    db.execSQL("ALTER TABLE web_apps ADD COLUMN extensionEnabled INTEGER NOT NULL DEFAULT 0")

                    db.execSQL("UPDATE web_apps SET extensionEnabled = 1 WHERE extensionModuleIds != '[]' AND extensionModuleIds != ''")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 33->34 extensionEnabled 跳过: ${e.message}")
                }
            }
        }


        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 34->35: 补充 web_apps 索引")
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_web_apps_appType_url ON web_apps(appType, url)")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 34->35 appType+url 索引跳过: ${e.message}")
                }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_web_apps_appType_iconPath_url ON web_apps(appType, iconPath, url)")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 34->35 appType+iconPath+url 索引跳过: ${e.message}")
                }
            }
        }

        private val MIGRATION_35_36_COLUMNS = """
            id, name, url, iconPath, packageName, appType,
            mediaConfig, galleryConfig, htmlConfig,
            wordpressConfig, nodejsConfig, phpAppConfig, pythonAppConfig, goAppConfig, multiWebConfig,
            activationEnabled, activationCodes, activationCodeList, activationRequireEveryTime, isActivated,
            adsEnabled, adConfig,
            announcementEnabled, announcement,
            adBlockEnabled, adBlockRules,
            webViewConfig,
            splashEnabled, splashConfig,
            bgmEnabled, bgmConfig,
            apkExportConfig, themeType,
            translateEnabled, translateConfig,
            extensionEnabled, extensionModuleIds, extensionFabIcon,
            autoStartConfig, forcedRunConfig,
            blackTechConfig, disguiseConfig, browserDisguiseConfig, deviceDisguiseConfig,
            activationDialogConfig,
            categoryId, createdAt, updatedAt
        """.trimIndent()

        private val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                AppLogger.i("AppDatabase", "迁移 35->36: 重建 web_apps 表以对齐当前本地结构")
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
                            multiWebConfig TEXT,
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
                            extensionEnabled INTEGER NOT NULL DEFAULT 0,
                            extensionModuleIds TEXT NOT NULL DEFAULT '[]',
                            extensionFabIcon TEXT,
                            autoStartConfig TEXT,
                            forcedRunConfig TEXT,
                            blackTechConfig TEXT,
                            disguiseConfig TEXT,
                            browserDisguiseConfig TEXT,
                            deviceDisguiseConfig TEXT,
                            activationDialogConfig TEXT,
                            categoryId INTEGER,
                            createdAt INTEGER NOT NULL DEFAULT 0,
                            updatedAt INTEGER NOT NULL DEFAULT 0
                        )
                    """.trimIndent(),
                    columnNames = MIGRATION_35_36_COLUMNS,
                    postSql = listOf(
                        "CREATE INDEX IF NOT EXISTS index_web_apps_updatedAt ON web_apps(updatedAt)",
                        "CREATE INDEX IF NOT EXISTS index_web_apps_categoryId ON web_apps(categoryId)",
                        "CREATE INDEX IF NOT EXISTS index_web_apps_isActivated ON web_apps(isActivated)",
                        "CREATE INDEX IF NOT EXISTS index_web_apps_appType_url ON web_apps(appType, url)",
                        "CREATE INDEX IF NOT EXISTS index_web_apps_appType_iconPath_url ON web_apps(appType, iconPath, url)"
                    )
                )
            }
        }



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
                        "CREATE INDEX IF NOT EXISTS index_web_apps_isActivated ON web_apps(isActivated)",
                        "CREATE INDEX IF NOT EXISTS index_web_apps_appType_url ON web_apps(appType, url)",
                        "CREATE INDEX IF NOT EXISTS index_web_apps_appType_iconPath_url ON web_apps(appType, iconPath, url)"
                    )
                )

                AppLogger.i("AppDatabase", "迁移 27->28 完成")
            }
        }


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
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_web_apps_appType_url ON web_apps(appType, url)")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 20->21 appType+url 索引跳过: ${e.message}")
                }
                try {
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_web_apps_appType_iconPath_url ON web_apps(appType, iconPath, url)")
                } catch (e: Exception) {
                    AppLogger.w("AppDatabase", "迁移 20->21 appType+iconPath+url 索引跳过: ${e.message}")
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
                    MIGRATION_32_33,
                    MIGRATION_33_34,
                    MIGRATION_34_35,
                    MIGRATION_35_36
                )
                .fallbackToDestructiveMigrationOnDowngrade()
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7)
                .build()
        }
    }
}
