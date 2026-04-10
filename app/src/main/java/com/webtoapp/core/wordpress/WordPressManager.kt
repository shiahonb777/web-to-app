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
 * WordPress 项目管理器
 * 
 * 负责：
 * - 创建 WordPress 项目（从缓存的 WP 核心复制）
 * - 导入用户的主题/插件
 * - 自动配置 wp-config.php（SQLite 模式）
 * - 安装 SQLite Database Integration 插件
 * - 管理项目文件生命周期
 */
object WordPressManager {
    
    private const val TAG = "WordPressManager"
    
    /** 安装验证最大重试次数 */
    private const val MAX_INSTALL_VERIFY_RETRIES = 10
    
    /** 安装验证重试间隔（毫秒） */
    private const val INSTALL_VERIFY_INTERVAL_MS = 500L
    
    // ==================== 公开 API ====================
    
    /**
     * 创建新的 WordPress 项目
     * 
     * 从缓存的 WordPress 核心复制一份完整的 WP 文件，
     * 自动配置 SQLite 数据库支持。
     * 
     * @param context Android Context
     * @param siteTitle 站点标题
     * @param adminUser 管理员用户名
     * @param adminEmail 管理员邮箱
     * @return 项目 ID，失败返回 null
     */
    suspend fun createProject(
        context: Context,
        siteTitle: String = "My Site",
        adminUser: String = "admin",
        adminEmail: String = ""
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 检查依赖是否就绪
            if (!WordPressDependencyManager.isAllReady(context)) {
                AppLogger.e(TAG, "依赖未就绪，无法创建项目")
                return@withContext null
            }
            
            val projectId = UUID.randomUUID().toString().take(8)
            val projectDir = getProjectDir(context, projectId)
            
            AppLogger.i(TAG, "创建 WordPress 项目: $projectId")
            
            // 1. 复制 WordPress 核心文件
            val wpSourceDir = File(WordPressDependencyManager.getDepsDir(context), "wordpress")
            copyDirectory(wpSourceDir, projectDir)
            AppLogger.i(TAG, "WordPress 核心已复制")
            
            // 2. 安装 SQLite 插件
            installSqlitePlugin(context, projectDir)
            
            // 3. 生成 wp-config.php
            generateWpConfig(projectDir, siteTitle)
            
            AppLogger.i(TAG, "WordPress 项目创建完成: $projectId (${projectDir.absolutePath})")
            projectId
        } catch (e: Exception) {
            AppLogger.e(TAG, "创建 WordPress 项目失败", e)
            null
        }
    }
    
    /**
     * 导入主题 zip 到项目
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
            
            // 解压主题 zip
            val themeName = extractZipFromUri(context, themeZipUri, themesDir)
            AppLogger.i(TAG, "主题已导入: $themeName")
            themeName
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入主题失败", e)
            null
        }
    }
    
    /**
     * 导入插件 zip 到项目
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
     * 导入完整 WordPress 压缩包（含主题和插件）
     */
    suspend fun importFullProject(
        context: Context,
        projectId: String,
        zipUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val projectDir = getProjectDir(context, projectId)
            
            // 清空项目目录
            projectDir.deleteRecursively()
            projectDir.mkdirs()
            
            // 解压整个压缩包
            extractZipFromUri(context, zipUri, projectDir)
            
            // 检查是否解压到了子目录（常见：wordpress/xxx）
            val wpIndicator = File(projectDir, "wp-includes/version.php")
            if (!wpIndicator.exists()) {
                // 检查是否在子目录中
                val subdirs = projectDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
                for (subdir in subdirs) {
                    if (File(subdir, "wp-includes/version.php").exists()) {
                        // 将子目录内容移到项目根目录
                        moveDirectoryContents(subdir, projectDir)
                        subdir.deleteRecursively()
                        break
                    }
                }
            }
            
            // 安装 SQLite 插件（如果尚未安装）
            val sqlitePluginDir = File(projectDir, "wp-content/plugins/sqlite-database-integration")
            if (!sqlitePluginDir.exists()) {
                installSqlitePlugin(context, projectDir)
            }
            
            // 确保 wp-config.php 正确配置
            generateWpConfig(projectDir, "My Site")
            
            AppLogger.i(TAG, "完整 WordPress 项目已导入: $projectId")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "导入完整 WordPress 项目失败", e)
            false
        }
    }
    
    /**
     * 删除项目
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
     * 获取项目目录
     */
    fun getProjectDir(context: Context, projectId: String): File {
        return File(WordPressDependencyManager.getWordPressProjectsDir(context), projectId)
    }
    
    /**
     * 获取项目中已安装的主题列表
     */
    fun getInstalledThemes(context: Context, projectId: String): List<String> {
        val themesDir = File(getProjectDir(context, projectId), "wp-content/themes")
        return themesDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }
    
    /**
     * 获取项目中已安装的插件列表
     */
    fun getInstalledPlugins(context: Context, projectId: String): List<String> {
        val pluginsDir = File(getProjectDir(context, projectId), "wp-content/plugins")
        return pluginsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?: emptyList()
    }
    
    /**
     * 获取项目占用空间（字节）
     */
    fun getProjectSize(context: Context, projectId: String): Long {
        return getProjectDir(context, projectId)
            .walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
    
    /**
     * 确保 wp-content/db.php drop-in 存在
     * 
     * 在启动 PHP 服务器之前调用，修复因 db.copy 缺失而导致 db.php 未生成的问题。
     * 对于已存在的项目也会检查并修复。
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
            // 验证已有 db.php 是否正确引用了 SQLite 插件
            val content = dbPhp.readText()
            if (!content.contains("sqlite-database-integration")) {
                AppLogger.w(TAG, "db.php 未正确引用 SQLite 插件，重新生成")
                generateDbPhp(dbPhp)
            }
        }
        
        // 确保数据库目录存在
        File(projectDir, "wp-content/database").mkdirs()
    }
    
    /**
     * 自动完成 WordPress 安装（如果尚未安装）
     * 
     * 检测 WordPress 是否需要安装（访问首页是否 302 到 install.php），
     * 如果需要，自动 POST 安装参数完成安装流程。
     * 
     * @param baseUrl PHP 服务器地址，如 http://127.0.0.1:18500
     * @param siteTitle 站点标题
     * @param adminUser 管理员用户名
     * @param adminPassword 管理员密码
     * @param adminEmail 管理员邮箱
     * @return true=已安装或安装成功，false=安装失败
     */
    suspend fun autoInstallIfNeeded(
        baseUrl: String,
        siteTitle: String = "My Site",
        adminUser: String = "admin",
        adminPassword: String = "admin",
        adminEmail: String = "admin@localhost.local"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 检查是否需要安装：GET / 看是否 302 到 install.php
            if (!isRedirectingToInstall(baseUrl)) {
                AppLogger.d(TAG, "WordPress 已安装，跳过自动安装")
                return@withContext true
            }
            
            AppLogger.i(TAG, "WordPress 未安装，开始自动安装...")
            
            // 2. POST 安装参数到 /wp-admin/install.php?step=2
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
            // 读取完整响应体，确保服务端 PHP 进程完整执行完毕（包括 SQLite 写入提交）
            // 不读取 body 就 disconnect 可能导致服务端写管道断裂，安装未完成
            var installSuccess = false
            try {
                val responseBody = if (installCode in 200..299) {
                    installConn.inputStream.bufferedReader().readText()
                } else {
                    installConn.errorStream?.bufferedReader()?.readText() ?: ""
                }
                // 记录响应摘要，用于诊断安装是否真正成功
                val snippet = responseBody.take(500).replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
                AppLogger.d(TAG, "安装响应: code=$installCode, bodyLen=${responseBody.length}, snippet=$snippet")
                // 检查响应中是否包含安装成功标志
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
            
            // 3. 验证安装是否成功：重试检查 GET / 是否不再重定向到 install.php
            // SQLite 数据库写入可能存在延迟（WAL 模式、文件系统同步等）
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
            
            // 验证超时，仍然返回 true 让 WebView 尝试加载（可能需要更长时间）
            AppLogger.w(TAG, "WordPress 安装验证超时 (${MAX_INSTALL_VERIFY_RETRIES} 次)，继续加载")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "WordPress 自动安装异常", e)
            false
        }
    }
    
    /**
     * 检查 GET baseUrl/ 是否重定向到 install.php
     * 
     * @return true 如果重定向到 install.php（即 WordPress 未安装），false 否则
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
            // 读取响应体以确保服务端完整处理
            try {
                val stream = if (checkCode in 200..299) checkConn.inputStream else checkConn.errorStream
                stream?.bufferedReader()?.readText()
            } catch (e: Exception) { AppLogger.d(TAG, "Failed to read WP check response body", e) }
            checkCode == 302 && location.contains("install.php")
        } finally {
            checkConn.disconnect()
        }
    }
    
    // ==================== 内部方法 ====================
    
    /**
     * 安装 SQLite Database Integration 插件到项目
     */
    private fun installSqlitePlugin(context: Context, projectDir: File) {
        val sqliteSourceDir = File(WordPressDependencyManager.getDepsDir(context), "sqlite-database-integration")
        val pluginsDir = File(projectDir, "wp-content/plugins/sqlite-database-integration")
        
        // 复制插件文件
        copyDirectory(sqliteSourceDir, pluginsDir)
        
        // 复制 db.php drop-in 到 wp-content/
        val dbCopy = File(pluginsDir, "db.copy")
        val dbPhp = File(projectDir, "wp-content/db.php")
        if (dbCopy.exists()) {
            dbCopy.copyTo(dbPhp, overwrite = true)
            AppLogger.d(TAG, "db.copy 已复制为 db.php")
        }
        
        // 如果 db.php 仍不存在（db.copy 在某些插件版本中可能缺失或路径不同），
        // 则手动生成 db.php drop-in，确保 SQLite 集成正常加载
        if (!dbPhp.exists()) {
            AppLogger.w(TAG, "db.copy 不存在，手动生成 db.php drop-in")
            generateDbPhp(dbPhp)
        }
        
        // 验证 db.php 内容是否正确引用了 SQLite 插件
        // 某些版本的 db.copy 可能包含占位符或错误路径
        val loadPhp = File(pluginsDir, "load.php")
        if (dbPhp.exists() && loadPhp.exists()) {
            val dbPhpContent = dbPhp.readText()
            if (!dbPhpContent.contains("sqlite-database-integration")) {
                AppLogger.w(TAG, "db.php 未正确引用 SQLite 插件，重新生成")
                generateDbPhp(dbPhp)
            }
        }
        
        // 确保数据库目录存在
        File(projectDir, "wp-content/database").mkdirs()
        
        AppLogger.i(TAG, "SQLite 插件已安装 (db.php=${dbPhp.exists()}, load.php=${loadPhp.exists()})")
    }
    
    /**
     * 生成 db.php drop-in 文件
     * 
     * 当 SQLite Database Integration 插件的 db.copy 缺失或内容不正确时，
     * 手动生成正确的 db.php，确保 WordPress 使用 SQLite 而非 MySQL。
     */
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
    
    /**
     * 生成 wp-config.php
     */
    private fun generateWpConfig(projectDir: File, siteTitle: String) {
        val wpConfig = File(projectDir, "wp-config.php")
        
        // 生成随机的安全密钥
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
    
    /**
     * 生成随机安全密钥
     */
    private fun generateRandomSalt(length: Int = 64): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$%^&*()-_+=[]{}|;:,.<>?"
        return (1..length).map { chars.random() }.joinToString("")
    }
    
    /**
     * 复制目录
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
     * 移动目录内容到目标目录
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
     * 从 Uri 解压 zip 文件，返回解压出的顶层目录名
     */
    private fun extractZipFromUri(context: Context, uri: Uri, destDir: File): String? {
        var topLevelDir: String? = null
        
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(inputStream.buffered())
            var entry = zipInputStream.nextEntry
            
            while (entry != null) {
                // 记录顶层目录名
                if (topLevelDir == null && entry.name.contains("/")) {
                    topLevelDir = entry.name.substringBefore("/")
                }
                
                val outFile = File(destDir, entry.name)
                
                // 安全检查：防止 zip slip 攻击
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
