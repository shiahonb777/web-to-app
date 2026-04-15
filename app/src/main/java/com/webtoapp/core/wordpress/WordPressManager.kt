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

/**
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 * Note: brief English comment.
 */
object WordPressManager {
    
    private const val TAG = "WordPressManager"
    
    /** Note: brief English comment. */
    private const val MAX_INSTALL_VERIFY_RETRIES = 10
    
    /** Note: brief English comment. */
    private const val INSTALL_VERIFY_INTERVAL_MS = 500L
    
    // Note: brief English comment.
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * 
     * @param context Android Context
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    suspend fun createProject(
        context: Context,
        siteTitle: String = "My Site",
        adminUser: String = "admin",
        adminEmail: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Note: brief English comment.
            if (!WordPressDependencyManager.isAllReady(context)) {
                AppLogger.e(TAG, "依赖未就绪，无法创建项目")
                return@withContext null
            }
            
            val projectId = UUID.randomUUID().toString().take(8)
            val projectDir = getProjectDir(context, projectId)
            
            AppLogger.i(TAG, "创建 WordPress 项目: $projectId")
            
            // Note: brief English comment.
            val wpSourceDir = File(WordPressDependencyManager.getDepsDir(context), "wordpress")
            copyDirectory(wpSourceDir, projectDir)
            AppLogger.i(TAG, "WordPress 核心已复制")
            
            // Note: brief English comment.
            installSqlitePlugin(context, projectDir)
            
            // Note: brief English comment.
            generateWpConfig(projectDir, siteTitle)
            
            AppLogger.i(TAG, "WordPress 项目创建完成: $projectId (${projectDir.absolutePath})")
            projectId
        } catch (e: Exception) {
            AppLogger.e(TAG, "创建 WordPress 项目失败", e)
            null
        }
    }
    
    /**
     * Note: brief English comment.
     */
    suspend fun importTheme(
        context: Context,
        projectId: String,
        themeZipUri: Uri
    ): String? = withContext(Dispatchers.IO) {
        try {
            val projectDir = getProjectDir(context, projectId)
            val themesDir = File(projectDir, "wp-content/themes")
            themesDir.mkdirs()
            
            // Note: brief English comment.
            val themeName = extractZipFromUri(context, themeZipUri, themesDir)
            AppLogger.i(TAG, "主题已导入: $themeName")
            themeName
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入主题失败", e)
            null
        }
    }
    
    /**
     * Note: brief English comment.
     */
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
    
    /**
     * Note: brief English comment.
     */
    suspend fun importFullProject(
        context: Context,
        projectId: String,
        zipUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectDir = getProjectDir(context, projectId)
            
            // Note: brief English comment.
            projectDir.deleteRecursively()
            projectDir.mkdirs()
            
            // Note: brief English comment.
            extractZipFromUri(context, zipUri, projectDir)
            
            // Note: brief English comment.
            val wpIndicator = File(projectDir, "wp-includes/version.php")
            if (!wpIndicator.exists()) {
                // Note: brief English comment.
                val subdirs = projectDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
                for (subdir in subdirs) {
                    if (File(subdir, "wp-includes/version.php").exists()) {
                        // Note: brief English comment.
                        moveDirectoryContents(subdir, projectDir)
                        subdir.deleteRecursively()
                        break
                    }
                }
            }
            
            // Note: brief English comment.
            val sqlitePluginDir = File(projectDir, "wp-content/plugins/sqlite-database-integration")
            if (!sqlitePluginDir.exists()) {
                installSqlitePlugin(context, projectDir)
            }
            
            // Note: brief English comment.
            generateWpConfig(projectDir, "My Site")
            
            AppLogger.i(TAG, "完整 WordPress 项目已导入: $projectId")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入完整 WordPress 项目失败", e)
            false
        }
    }
    
    /**
     * Note: brief English comment.
     */
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
    
    /**
     * Note: brief English comment.
     */
    fun getProjectDir(context: Context, projectId: String): File {
        return File(WordPressDependencyManager.getWordPressProjectsDir(context), projectId)
    }
    
    /**
     * Note: brief English comment.
     */
    fun getInstalledThemes(context: Context, projectId: String): List<String> {
        val themesDir = File(getProjectDir(context, projectId), "wp-content/themes")
        return themesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }
    
    /**
     * Note: brief English comment.
     */
    fun getInstalledPlugins(context: Context, projectId: String): List<String> {
        val pluginsDir = File(getProjectDir(context, projectId), "wp-content/plugins")
        return pluginsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }
    
    /**
     * Note: brief English comment.
     */
    fun getProjectSize(context: Context, projectId: String): Long {
        return getProjectDir(context, projectId)
            .walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     */
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
            // Note: brief English comment.
            val content = dbPhp.readText()
            if (!content.contains("sqlite-database-integration")) {
                AppLogger.w(TAG, "db.php 未正确引用 SQLite 插件，重新生成")
                generateDbPhp(dbPhp)
            }
        }
        
        // Note: brief English comment.
        File(projectDir, "wp-content/database").mkdirs()
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    suspend fun autoInstallIfNeeded(
        baseUrl: String,
        siteTitle: String = "My Site",
        adminUser: String = "admin",
        adminPassword: String = "admin",
        adminEmail: String = "admin@localhost.local"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Note: brief English comment.
            if (!isRedirectingToInstall(baseUrl)) {
                AppLogger.d(TAG, "WordPress 已安装，跳过自动安装")
                return@withContext true
            }
            
            AppLogger.i(TAG, "WordPress 未安装，开始自动安装...")
            
            // Note: brief English comment.
            val installUrl = java.net.URL("$baseUrl/wp-admin/install.php?step=2")
            val installConn = installUrl.openConnection() as java.net.HttpURLConnection
            installConn.requestMethod = "POST"
            installConn.doOutput = true
            installConn.connectTimeout = 30000
            installConn.readTimeout = 60000
            installConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            val params = buildString {
                append("language=").append(java.net.URLEncoder.encode("en_US", "UTF-8"))
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
            // Note: brief English comment.
            // Note: brief English comment.
            var installSuccess = false
            try {
                val responseBody = if (installCode in 200..299) {
                    installConn.inputStream.bufferedReader().readText()
                } else {
                    installConn.errorStream?.bufferedReader()?.readText() ?: ""
                }
                // Note: brief English comment.
                val snippet = responseBody.take(500).replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                AppLogger.d(TAG, "安装响应: code=$installCode, bodyLen=${responseBody.length}, snippet=$snippet")
                // Note: brief English comment.
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
            
            // Note: brief English comment.
            // Note: brief English comment.
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
            
            // Note: brief English comment.
            AppLogger.w(TAG, "WordPress 安装验证超时 (${MAX_INSTALL_VERIFY_RETRIES} 次)，继续加载")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "WordPress 自动安装异常", e)
            false
        }
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     */
    private fun isRedirectingToInstall(baseUrl: String): Boolean {
        val checkUrl = java.net.URL("$baseUrl/")
        val checkConn = checkUrl.openConnection() as java.net.HttpURLConnection
        checkConn.connectTimeout = 5000
        checkConn.readTimeout = 10000
        checkConn.instanceFollowRedirects = false
        return try {
            val checkCode = checkConn.responseCode
            val location = checkConn.getHeaderField("Location") ?: ""
            // Note: brief English comment.
            try {
                val stream = if (checkCode in 200..299) checkConn.inputStream else checkConn.errorStream
                stream?.bufferedReader()?.readText()
            } catch (e: Exception) { AppLogger.d(TAG, "Failed to read WP check response body", e) }
            checkCode == 302 && location.contains("install.php")
        } finally {
            checkConn.disconnect()
        }
    }
    
    // Note: brief English comment.
    
    /**
     * Note: brief English comment.
     */
    private fun installSqlitePlugin(context: Context, projectDir: File) {
        val sqliteSourceDir = File(WordPressDependencyManager.getDepsDir(context), "sqlite-database-integration")
        val pluginsDir = File(projectDir, "wp-content/plugins/sqlite-database-integration")
        
        // Note: brief English comment.
        copyDirectory(sqliteSourceDir, pluginsDir)
        
        // Note: brief English comment.
        val dbCopy = File(pluginsDir, "db.copy")
        val dbPhp = File(projectDir, "wp-content/db.php")
        if (dbCopy.exists()) {
            dbCopy.copyTo(dbPhp, overwrite = true)
            AppLogger.d(TAG, "db.copy 已复制为 db.php")
        }
        
        // Note: brief English comment.
        // Note: brief English comment.
        if (!dbPhp.exists()) {
            AppLogger.w(TAG, "db.copy 不存在，手动生成 db.php drop-in")
            generateDbPhp(dbPhp)
        }
        
        // Note: brief English comment.
        // Note: brief English comment.
        val loadPhp = File(pluginsDir, "load.php")
        if (dbPhp.exists() && loadPhp.exists()) {
            val dbPhpContent = dbPhp.readText()
            if (!dbPhpContent.contains("sqlite-database-integration")) {
                AppLogger.w(TAG, "db.php 未正确引用 SQLite 插件，重新生成")
                generateDbPhp(dbPhp)
            }
        }
        
        // Note: brief English comment.
        File(projectDir, "wp-content/database").mkdirs()
        
        AppLogger.i(TAG, "SQLite 插件已安装 (db.php=${dbPhp.exists()}, load.php=${loadPhp.exists()})")
    }
    
    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun generateDbPhp(dbPhp: File) {
        dbPhp.writeText("""<?php
/**
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
 */

// Note: brief English comment.
${'$'}sqlite_plugin = defined('WP_PLUGIN_DIR')
    ? WP_PLUGIN_DIR . '/sqlite-database-integration/load.php'
    : __DIR__ . '/plugins/sqlite-database-integration/load.php';

if (is_readable(${'$'}sqlite_plugin)) {
    require_once ${'$'}sqlite_plugin;
} else {
    // Note: brief English comment.
    ${'$'}alt_path = dirname(__DIR__) . '/wp-content/plugins/sqlite-database-integration/load.php';
    if (is_readable(${'$'}alt_path)) {
        require_once ${'$'}alt_path;
    }
}
""")
    }
    
    /**
     * Note: brief English comment.
     */
    private fun generateWpConfig(projectDir: File, siteTitle: String) {
        val wpConfig = File(projectDir, "wp-config.php")
        
        // Note: brief English comment.
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
 * Note: brief English comment.
 * Note: brief English comment.
 */

// Note: brief English comment.
define('DB_ENGINE', 'sqlite');
define('DB_DIR', __DIR__ . '/wp-content/database/');
define('DB_FILE', '.ht.sqlite');

// Note: brief English comment.
define('DB_NAME', '');
define('DB_USER', '');
define('DB_PASSWORD', '');
define('DB_HOST', '');
define('DB_CHARSET', 'utf8mb4');
define('DB_COLLATE', '');

// Note: brief English comment.
$keyDefinitions

${'$'}table_prefix = 'wp_';

// Note: brief English comment.
// Note: brief English comment.
if (isset(${'$'}_SERVER['HTTP_HOST'])) {
    ${'$'}protocol = 'http://';
    define('WP_HOME', ${'$'}protocol . ${'$'}_SERVER['HTTP_HOST']);
    define('WP_SITEURL', ${'$'}protocol . ${'$'}_SERVER['HTTP_HOST']);
}

// Note: brief English comment.
define('WP_AUTO_UPDATE_CORE', false);
define('AUTOMATIC_UPDATER_DISABLED', true);
define('DISALLOW_FILE_MODS', false);
define('DISALLOW_FILE_EDIT', true);

// Note: brief English comment.
define('WP_DEBUG', false);
define('WP_DEBUG_LOG', false);
define('WP_DEBUG_DISPLAY', false);

// Note: brief English comment.
define('FS_METHOD', 'direct');

// Note: brief English comment.
define('WP_MEMORY_LIMIT', '256M');
define('WP_MAX_MEMORY_LIMIT', '256M');

// Note: brief English comment.
define('DISABLE_WP_CRON', true);

// ** ABSPATH ** //
if (!defined('ABSPATH')) {
    define('ABSPATH', __DIR__ . '/');
}

/** Note: brief English comment. */
require_once ABSPATH . 'wp-settings.php';
""")
        
        AppLogger.i(TAG, "wp-config.php 已生成")
    }
    
    /**
     * Note: brief English comment.
     */
    private fun generateRandomSalt(length: Int = 64): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_+=[]{}|;:,.<>?"
        return (1..length).map { chars.random() }.joinToString("")
    }
    
    /**
     * Note: brief English comment.
     */
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
    
    /**
     * Note: brief English comment.
     */
    private fun moveDirectoryContents(source: File, dest: File) {
        source.listFiles()?.forEach { file ->
            val target = File(dest, file.name)
            if (target.exists() && target.absolutePath != file.absolutePath) {
                target.deleteRecursively()
            }
            file.renameTo(target)
        }
    }
    
    /**
     * Note: brief English comment.
     */
    private fun extractZipFromUri(context: Context, uri: Uri, destDir: File): String? {
        var topLevelDir: String? = null
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream.buffered())
            var entry = zipInputStream.nextEntry
            
            while (entry != null) {
                // Note: brief English comment.
                if (topLevelDir == null && entry.name.contains("/")) {
                    topLevelDir = entry.name.substringBefore("/")
                }
                
                val outFile = File(destDir, entry.name)
                
                // Note: brief English comment.
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
