package com.webtoapp.core.wordpress

import android.content.Context
import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream











object WordPressManager {

    private const val TAG = "WordPressManager"


    private const val MAX_INSTALL_VERIFY_RETRIES = 10


    private const val INSTALL_VERIFY_INTERVAL_MS = 500L

    data class ProjectMetadata(
        val themes: List<String> = emptyList(),
        val plugins: List<String> = emptyList(),
        val version: String? = null
    )















    suspend fun createProject(
        context: Context,
        siteTitle: String = "My Site",
        adminUser: String = "admin",
        adminEmail: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {

            if (!WordPressDependencyManager.isAllReady(context)) {
                AppLogger.e(TAG, "依赖未就绪，无法创建项目")
                return@withContext null
            }

            val projectId = UUID.randomUUID().toString().take(8)
            val projectDir = getProjectDir(context, projectId)

            AppLogger.i(TAG, "创建 WordPress 项目: $projectId")


            val wpSourceDir = File(WordPressDependencyManager.getDepsDir(context), "wordpress")
            copyDirectory(wpSourceDir, projectDir)
            AppLogger.i(TAG, "WordPress 核心已复制")


            installSqlitePlugin(context, projectDir)


            generateWpConfig(projectDir, siteTitle)

            AppLogger.i(TAG, "WordPress 项目创建完成: $projectId (${projectDir.absolutePath})")
            projectId
        } catch (e: Exception) {
            AppLogger.e(TAG, "创建 WordPress 项目失败", e)
            null
        }
    }




    suspend fun importTheme(
        context: Context,
        projectId: String,
        themeZipUri: Uri
    ): String? = withContext(Dispatchers.IO) {
        try {
            val projectDir = getProjectDir(context, projectId)
            val themesDir = File(projectDir, "wp-content/themes")
            themesDir.mkdirs()


            val themeName = extractZipFromUri(context, themeZipUri, themesDir)
            AppLogger.i(TAG, "主题已导入: $themeName")
            themeName
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入主题失败", e)
            null
        }
    }




    suspend fun importPlugin(
        context: Context,
        projectId: String,
        pluginZipUri: Uri
    ): String? = withContext(Dispatchers.IO) {
        try {
            val projectDir = getProjectDir(context, projectId)
            val pluginsDir = File(projectDir, "wp-content/plugins")
            pluginsDir.mkdirs()

            val pluginName = extractZipFromUri(context, pluginZipUri, pluginsDir)
            AppLogger.i(TAG, "插件已导入: $pluginName")
            pluginName
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入插件失败", e)
            null
        }
    }




    suspend fun importFullProject(
        context: Context,
        projectId: String,
        zipUri: Uri,
        siteTitle: String = "My Site"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectDir = getProjectDir(context, projectId)


            projectDir.deleteRecursively()
            projectDir.mkdirs()


            extractZipFromUri(context, zipUri, projectDir)


            val wpIndicator = File(projectDir, "wp-includes/version.php")
            if (!wpIndicator.exists()) {

                val subdirs = projectDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
                for (subdir in subdirs) {
                    if (File(subdir, "wp-includes/version.php").exists()) {

                        moveDirectoryContents(subdir, projectDir)
                        subdir.deleteRecursively()
                        break
                    }
                }
            }


            val sqlitePluginDir = File(projectDir, "wp-content/plugins/sqlite-database-integration")
            if (!sqlitePluginDir.exists()) {
                installSqlitePlugin(context, projectDir)
            }


            generateWpConfig(projectDir, siteTitle)

            AppLogger.i(TAG, "完整 WordPress 项目已导入: $projectId")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入完整 WordPress 项目失败", e)
            false
        }
    }




    suspend fun importProjectDirectory(
        context: Context,
        sourceDir: File,
        siteTitle: String = "My Site"
    ): String? = withContext(Dispatchers.IO) {
        try {
            if (!WordPressDependencyManager.isAllReady(context)) {
                AppLogger.e(TAG, "依赖未就绪，无法导入 WordPress 项目")
                return@withContext null
            }

            val projectId = UUID.randomUUID().toString().take(8)
            val projectDir = getProjectDir(context, projectId)
            projectDir.deleteRecursively()
            projectDir.mkdirs()

            if (File(sourceDir, "wp-includes/version.php").exists()) {
                copyDirectory(sourceDir, projectDir)
            } else {
                val wpSourceDir = File(WordPressDependencyManager.getDepsDir(context), "wordpress")
                copyDirectory(wpSourceDir, projectDir)
                copyDirectory(sourceDir, projectDir)
            }

            val sqlitePluginDir = File(projectDir, "wp-content/plugins/sqlite-database-integration")
            if (!sqlitePluginDir.exists()) {
                installSqlitePlugin(context, projectDir)
            } else {
                ensureDbPhpExists(context, projectDir)
            }

            generateWpConfig(projectDir, siteTitle)
            AppLogger.i(TAG, "WordPress 目录项目已导入: $projectId (${projectDir.absolutePath})")
            projectId
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入 WordPress 目录失败", e)
            null
        }
    }



    fun deleteProject(context: Context, projectId: String): Boolean {
        return try {
            val projectDir = getProjectDir(context, projectId)
            projectDir.deleteRecursively()
            AppLogger.i(TAG, "项目已删除: $projectId")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "删除项目失败: $projectId", e)
            false
        }
    }




    fun getProjectDir(context: Context, projectId: String): File {
        return File(WordPressDependencyManager.getWordPressProjectsDir(context), projectId)
    }




    fun getInstalledThemes(context: Context, projectId: String): List<String> {
        val themesDir = File(getProjectDir(context, projectId), "wp-content/themes")
        return themesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }




    fun getInstalledPlugins(context: Context, projectId: String): List<String> {
        val pluginsDir = File(getProjectDir(context, projectId), "wp-content/plugins")
        return pluginsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }




    fun getProjectSize(context: Context, projectId: String): Long {
        return getProjectDir(context, projectId)
            .walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    fun inspectProject(context: Context, projectId: String): ProjectMetadata {
        return inspectProjectDir(getProjectDir(context, projectId))
    }

    fun inspectProjectDir(projectDir: File): ProjectMetadata {
        val themes = File(projectDir, "wp-content/themes").listFiles()
            ?.filter { it.isDirectory && it.name != "index.php" }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
        val plugins = File(projectDir, "wp-content/plugins").listFiles()
            ?.filter { it.isDirectory && it.name != "index.php" }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
        val version = runCatching {
            val content = File(projectDir, "wp-includes/version.php").readText()
            Regex("""\${'$'}wp_version\s*=\s*'([^']+)'""").find(content)?.groupValues?.get(1)
        }.getOrNull()
        return ProjectMetadata(themes = themes, plugins = plugins, version = version)
    }







    fun ensureDbPhpExists(context: Context, projectDir: File) {
        val dbPhp = File(projectDir, "wp-content/db.php")
        val pluginLoadPhp = File(projectDir, "wp-content/plugins/sqlite-database-integration/load.php")

        if (!pluginLoadPhp.exists()) {
            AppLogger.d(TAG, "SQLite 插件未安装，跳过 db.php 检查")
            return
        }

        if (!dbPhp.exists()) {
            AppLogger.w(TAG, "db.php 不存在，正在生成...")
            generateDbPhp(dbPhp)
        } else {

            val content = dbPhp.readText()
            if (!content.contains("sqlite-database-integration")) {
                AppLogger.w(TAG, "db.php 未正确引用 SQLite 插件，重新生成")
                generateDbPhp(dbPhp)
            }
        }


        File(projectDir, "wp-content/database").mkdirs()
    }














    suspend fun autoInstallIfNeeded(
        baseUrl: String,
        siteTitle: String = "My Site",
        adminUser: String = "admin",
        adminPassword: String = "admin",
        adminEmail: String = "admin@localhost.local",
        siteLanguage: String = "en_US"
    ): Boolean = withContext(Dispatchers.IO) {
        try {

            if (!isRedirectingToInstall(baseUrl)) {
                AppLogger.d(TAG, "WordPress 已安装，跳过自动安装")
                return@withContext true
            }

            AppLogger.i(TAG, "WordPress 未安装，开始自动安装...")


            val installUrl = java.net.URL("$baseUrl/wp-admin/install.php?step=2")
            val installConn = installUrl.openConnection() as java.net.HttpURLConnection
            installConn.requestMethod = "POST"
            installConn.doOutput = true
            installConn.connectTimeout = 30000
            installConn.readTimeout = 60000
            installConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            val params = buildString {
                append("language=").append(java.net.URLEncoder.encode(siteLanguage, "UTF-8"))
                append("&weblog_title=").append(java.net.URLEncoder.encode(siteTitle, "UTF-8"))
                append("&user_name=").append(java.net.URLEncoder.encode(adminUser, "UTF-8"))
                append("&admin_password=").append(java.net.URLEncoder.encode(adminPassword, "UTF-8"))
                append("&admin_password2=").append(java.net.URLEncoder.encode(adminPassword, "UTF-8"))
                append("&pw_weak=1")
                append("&admin_email=").append(java.net.URLEncoder.encode(adminEmail, "UTF-8"))
                append("&blog_public=0")
            }

            installConn.outputStream.use { it.write(params.toByteArray(Charsets.UTF_8)) }
            val installCode = installConn.responseCode


            var installSuccess = false
            try {
                val responseBody = if (installCode in 200..299) {
                    installConn.inputStream.bufferedReader().readText()
                } else {
                    installConn.errorStream?.bufferedReader()?.readText() ?: ""
                }

                val snippet = responseBody.take(500).replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                AppLogger.d(TAG, "安装响应: code=$installCode, bodyLen=${responseBody.length}, snippet=$snippet")

                installSuccess = responseBody.contains("wp-login.php") ||
                    responseBody.contains("installed") ||
                    responseBody.contains("Success") ||
                    responseBody.contains("成功")
                if (!installSuccess) {
                    AppLogger.w(TAG, "安装响应未包含成功标志，可能安装未完成")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "读取安装响应失败: ${e.message}")
            }
            installConn.disconnect()

            if (installCode !in 200..399) {
                AppLogger.e(TAG, "WordPress 自动安装失败 (code=$installCode)")
                return@withContext false
            }

            AppLogger.i(TAG, "WordPress 自动安装请求完成 (code=$installCode)")



            repeat(MAX_INSTALL_VERIFY_RETRIES) { attempt ->
                kotlinx.coroutines.delay(INSTALL_VERIFY_INTERVAL_MS)
                try {
                    if (!isRedirectingToInstall(baseUrl)) {
                        AppLogger.i(TAG, "WordPress 安装验证成功 (尝试 ${attempt + 1})")
                        return@withContext true
                    }
                    AppLogger.d(TAG, "WordPress 安装验证: 仍重定向到 install.php (尝试 ${attempt + 1})")
                } catch (e: Exception) {
                    AppLogger.d(TAG, "WordPress 安装验证异常 (尝试 ${attempt + 1}): ${e.message}")
                }
            }


            AppLogger.w(TAG, "WordPress 安装验证超时 (${MAX_INSTALL_VERIFY_RETRIES} 次)，继续加载")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "WordPress 自动安装异常", e)
            false
        }
    }






    suspend fun applyRuntimeConfig(
        phpBinary: String,
        projectDir: File,
        siteTitle: String,
        permalinkStructure: String,
        siteLanguage: String,
        themeName: String,
        activePlugins: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val script = File(projectDir, ".webtoapp-wp-configure.php")
            val normalizedPermalink = normalizePermalinkStructure(permalinkStructure)
            val pluginArray = activePlugins.joinToString(",") { "'${escapePhpString(it)}'" }
            val phpScript = buildString {
                appendLine("<?php")
                appendLine("define('WP_USE_THEMES', false);")
                appendLine("require_once __DIR__ . '/wp-load.php';")
                appendLine("if (function_exists('update_option')) {")
                appendLine("update_option('blogname', '${escapePhpString(siteTitle)}');")
                appendLine("update_option('WPLANG', '${escapePhpString(siteLanguage)}');")
                appendLine("update_option('permalink_structure', '${escapePhpString(normalizedPermalink)}');")
                appendLine("}")
                if (themeName.isNotBlank()) {
                    appendLine("if (function_exists('switch_theme')) { switch_theme('${escapePhpString(themeName)}'); }")
                }
                appendLine("if (function_exists('activate_plugin')) {")
                appendLine("require_once ABSPATH . 'wp-admin/includes/plugin.php';")
                appendLine("\$plugins = array($pluginArray);")
                appendLine("foreach (\$plugins as \$pluginDir) {")
                appendLine("\$pluginFile = \$pluginDir . '/' . \$pluginDir . '.php';")
                appendLine("\$pluginPath = __DIR__ . '/wp-content/plugins/' . \$pluginFile;")
                appendLine("if (!is_readable(\$pluginPath)) {")
                appendLine("foreach (glob(__DIR__ . '/wp-content/plugins/' . \$pluginDir . '/*.php') as \$candidate) {")
                appendLine("if (strpos(file_get_contents(\$candidate), 'Plugin Name:') !== false) { \$pluginFile = \$pluginDir . '/' . basename(\$candidate); break; }")
                appendLine("}")
                appendLine("}")
                appendLine("if (is_readable(__DIR__ . '/wp-content/plugins/' . \$pluginFile)) { activate_plugin(\$pluginFile, '', false, true); }")
                appendLine("}")
                appendLine("}")
                appendLine("if (function_exists('flush_rewrite_rules')) { flush_rewrite_rules(false); }")
            }
            script.writeText(phpScript)
            val process = ProcessBuilder(phpBinary, script.absolutePath)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            script.delete()
            if (exitCode == 0) {
                AppLogger.i(TAG, "WordPress 运行时配置已应用")
                true
            } else {
                AppLogger.w(TAG, "WordPress 运行时配置失败 exit=$exitCode output=$output")
                false
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "应用 WordPress 运行时配置失败", e)
            false
        }
    }

    private fun normalizePermalinkStructure(value: String): String {
        return when (value) {
            "plain" -> ""
            "postname" -> "/%postname%/"
            "numeric" -> "/archives/%post_id%"
            else -> value
        }
    }

    private fun escapePhpString(value: String): String {
        return value.replace("\\", "\\\\").replace("'", "\\'")
    }

    private fun isRedirectingToInstall(baseUrl: String): Boolean {
        val checkUrl = java.net.URL("$baseUrl/")
        val checkConn = checkUrl.openConnection() as java.net.HttpURLConnection
        checkConn.connectTimeout = 5000
        checkConn.readTimeout = 10000
        checkConn.instanceFollowRedirects = false
        return try {
            val checkCode = checkConn.responseCode
            val location = checkConn.getHeaderField("Location") ?: ""

            try {
                val stream = if (checkCode in 200..299) checkConn.inputStream else checkConn.errorStream
                stream?.bufferedReader()?.readText()
            } catch (e: Exception) { AppLogger.d(TAG, "Failed to read WP check response body", e) }
            checkCode == 302 && location.contains("install.php")
        } finally {
            checkConn.disconnect()
        }
    }






    private fun installSqlitePlugin(context: Context, projectDir: File) {
        val sqliteSourceDir = File(WordPressDependencyManager.getDepsDir(context), "sqlite-database-integration")
        val pluginsDir = File(projectDir, "wp-content/plugins/sqlite-database-integration")


        copyDirectory(sqliteSourceDir, pluginsDir)


        val dbCopy = File(pluginsDir, "db.copy")
        val dbPhp = File(projectDir, "wp-content/db.php")
        if (dbCopy.exists()) {
            dbCopy.copyTo(dbPhp, overwrite = true)
            AppLogger.d(TAG, "db.copy 已复制为 db.php")
        }



        if (!dbPhp.exists()) {
            AppLogger.w(TAG, "db.copy 不存在，手动生成 db.php drop-in")
            generateDbPhp(dbPhp)
        }



        val loadPhp = File(pluginsDir, "load.php")
        if (dbPhp.exists() && loadPhp.exists()) {
            val dbPhpContent = dbPhp.readText()
            if (!dbPhpContent.contains("sqlite-database-integration")) {
                AppLogger.w(TAG, "db.php 未正确引用 SQLite 插件，重新生成")
                generateDbPhp(dbPhp)
            }
        }


        File(projectDir, "wp-content/database").mkdirs()

        AppLogger.i(TAG, "SQLite 插件已安装 (db.php=${dbPhp.exists()}, load.php=${loadPhp.exists()})")
    }







    private fun generateDbPhp(dbPhp: File) {
        dbPhp.writeText("""<?php
/**
 * SQLite Database Integration drop-in — WebToApp 自动生成
 *
 * 加载 SQLite Database Integration 插件，使 WordPress 使用 SQLite 替代 MySQL。
 * 此文件必须位于 wp-content/db.php。
 */

// 确定插件路径（db.php 加载时 WP_PLUGIN_DIR 可能尚未定义）
${'$'}sqlite_plugin = defined('WP_PLUGIN_DIR')
    ? WP_PLUGIN_DIR . '/sqlite-database-integration/load.php'
    : __DIR__ . '/plugins/sqlite-database-integration/load.php';

if (is_readable(${'$'}sqlite_plugin)) {
    require_once ${'$'}sqlite_plugin;
} else {
    // 尝试备用路径
    ${'$'}alt_path = dirname(__DIR__) . '/wp-content/plugins/sqlite-database-integration/load.php';
    if (is_readable(${'$'}alt_path)) {
        require_once ${'$'}alt_path;
    }
}
""")
    }




    private fun generateWpConfig(projectDir: File, siteTitle: String) {
        val wpConfig = File(projectDir, "wp-config.php")


        val keys = listOf(
            "AUTH_KEY", "SECURE_AUTH_KEY", "LOGGED_IN_KEY", "NONCE_KEY",
            "AUTH_SALT", "SECURE_AUTH_SALT", "LOGGED_IN_SALT", "NONCE_SALT"
        )

        val keyDefinitions = keys.joinToString("\n") { key ->
            val salt = generateRandomSalt()
            "define('$key', '$salt');"
        }

        wpConfig.writeText("""<?php
/**
 * WordPress 配置文件 - WebToApp 自动生成
 * 配置为使用 SQLite 数据库（通过 SQLite Database Integration 插件）
 */

// ** SQLite 数据库配置 ** //
define('DB_ENGINE', 'sqlite');
define('DB_DIR', __DIR__ . '/wp-content/database/');
define('DB_FILE', '.ht.sqlite');

// MySQL 配置保留为空（不使用）
define('DB_NAME', '');
define('DB_USER', '');
define('DB_PASSWORD', '');
define('DB_HOST', '');
define('DB_CHARSET', 'utf8mb4');
define('DB_COLLATE', '');

// ** 安全密钥 ** //
$keyDefinitions

${'$'}table_prefix = 'wp_';

// ** WordPress 地址配置 ** //
// 由 PHP 服务器动态设置
if (isset(${'$'}_SERVER['HTTP_HOST'])) {
    ${'$'}protocol = 'http://';
    define('WP_HOME', ${'$'}protocol . ${'$'}_SERVER['HTTP_HOST']);
    define('WP_SITEURL', ${'$'}protocol . ${'$'}_SERVER['HTTP_HOST']);
}

// ** 禁用自动更新（离线环境） ** //
define('WP_AUTO_UPDATE_CORE', false);
define('AUTOMATIC_UPDATER_DISABLED', true);
define('DISALLOW_FILE_MODS', false);
define('DISALLOW_FILE_EDIT', true);

// ** 调试模式 ** //
define('WP_DEBUG', false);
define('WP_DEBUG_LOG', false);
define('WP_DEBUG_DISPLAY', false);

// ** 文件系统方法 ** //
define('FS_METHOD', 'direct');

// ** 内存限制 ** //
define('WP_MEMORY_LIMIT', '256M');
define('WP_MAX_MEMORY_LIMIT', '256M');

// ** 禁用 Cron（由 PHP 直接处理） ** //
define('DISABLE_WP_CRON', true);

// ** ABSPATH ** //
if (!defined('ABSPATH')) {
    define('ABSPATH', __DIR__ . '/');
}

/** 加载 WordPress */
require_once ABSPATH . 'wp-settings.php';
""")

        AppLogger.i(TAG, "wp-config.php 已生成")
    }




    private fun generateRandomSalt(length: Int = 64): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_+=[]{}|;:,.<>?"
        return (1..length).map { chars.random() }.joinToString("")
    }




    private fun copyDirectory(source: File, dest: File) {
        dest.mkdirs()
        source.walkTopDown().forEach { file ->
            val relativePath = file.relativeTo(source)
            val targetFile = File(dest, relativePath.path)
            if (file.isDirectory) {
                targetFile.mkdirs()
            } else {
                targetFile.parentFile?.mkdirs()
                file.copyTo(targetFile, overwrite = true)
            }
        }
    }




    private fun moveDirectoryContents(source: File, dest: File) {
        source.listFiles()?.forEach { file ->
            val target = File(dest, file.name)
            if (target.exists() && target.absolutePath != file.absolutePath) {
                target.deleteRecursively()
            }
            file.renameTo(target)
        }
    }




    private fun extractZipFromUri(context: Context, uri: Uri, destDir: File): String? {
        var topLevelDir: String? = null

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream.buffered())
            var entry = zipInputStream.nextEntry

            while (entry != null) {

                if (topLevelDir == null && entry.name.contains("/")) {
                    topLevelDir = entry.name.substringBefore("/")
                }

                val outFile = File(destDir, entry.name)


                if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                    AppLogger.w(TAG, "跳过不安全的 zip 条目: ${entry.name}")
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                    continue
                }

                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        zipInputStream.copyTo(fos)
                    }
                }

                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
        }

        return topLevelDir
    }
}
