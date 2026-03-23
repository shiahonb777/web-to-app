package com.webtoapp.core.logging

import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 应用运行日志管理器
 * 
 * 功能特性：
 * 1. 记录应用从启动到退出的完整运行日志
 * 2. 每次运行生成独立的日志文件（以启动时间命名）
 * 3. 支持多级别日志（VERBOSE/DEBUG/INFO/WARN/ERROR/FATAL）
 * 4. 支持分类日志（生命周期/UI操作/网络/数据库/WebView等）
 * 5. 异步写入，不阻塞主线程
 * 6. 自动捕获未处理异常
 * 7. 自动清理过期日志文件
 * 
 * 日志文件位置：/data/data/[包名]/files/logs/run_[时间戳].log
 * 
 * 使用示例：
 * ```kotlin
 * // 初始化（在 Application.onCreate 中调用）
 * AppLogger.init(applicationContext)
 * 
 * // 记录日志
 * AppLogger.i("MainActivity", "用户点击了按钮")
 * AppLogger.d("Network", "请求 URL: https://example.com")
 * AppLogger.e("Database", "查询失败", exception)
 * 
 * // 记录生命周期
 * AppLogger.lifecycle("MainActivity", "onCreate")
 * 
 * // 记录用户操作
 * AppLogger.userAction("创建应用", "appName=TestApp, url=https://example.com")
 * 
 * // 应用退出时
 * AppLogger.shutdown()
 * ```
 */
object AppLogger {
    
    private const val TAG = "AppLogger"
    private const val LOG_DIR_NAME = "logs"
    private const val LOG_FILE_PREFIX = "run_"
    private const val LOG_FILE_EXTENSION = ".log"
    private const val MAX_LOG_FILES = 20  // 最多保留20个日志文件
    private const val MAX_LOG_SIZE = 10 * 1024 * 1024L  // 单个日志文件最大10MB
    private const val FLUSH_INTERVAL_MS = 3000L  // 3秒刷新一次缓冲
    private const val MAX_BUFFER_SIZE = 100  // 缓冲区最大条目数
    
    // 日志级别
    enum class Level(val value: Int, val label: String) {
        VERBOSE(0, "V"),
        DEBUG(1, "D"),
        INFO(2, "I"),
        WARN(3, "W"),
        ERROR(4, "E"),
        FATAL(5, "F")
    }
    
    // 日志分类
    enum class Category(val label: String) {
        SYSTEM("SYSTEM"),           // 系统级别
        LIFECYCLE("LIFECYCLE"),     // 生命周期
        UI("UI"),                   // UI 操作
        USER_ACTION("USER_ACTION"), // 用户操作
        NETWORK("NETWORK"),         // 网络请求
        DATABASE("DATABASE"),       // 数据库操作
        WEBVIEW("WEBVIEW"),         // WebView 相关
        APK_BUILD("APK_BUILD"),     // APK 构建
        EXTENSION("EXTENSION"),     // 扩展模块
        AI("AI"),                   // AI 功能
        CRYPTO("CRYPTO"),           // 加密相关
        MEDIA("MEDIA"),             // 媒体处理
        FILE("FILE"),               // 文件操作
        GENERAL("GENERAL")          // 通用
    }
    
    private var context: Context? = null
    private var logFile: File? = null
    private var logDir: File? = null
    private var sessionId: String = ""
    private var startTime: Long = 0
    
    private val isInitialized = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)
    
    // 异步写入相关
    private val logBuffer = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "AppLogger-Writer").apply { isDaemon = true }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    // 最小日志级别（可配置）
    var minLevel: Level = Level.VERBOSE
    
    // 是否同时输出到 Logcat
    var logToLogcat: Boolean = true
    
    /**
     * 初始化日志系统
     * 应在 Application.onCreate() 中调用
     */
    @Synchronized
    fun init(context: Context) {
        if (isInitialized.get()) {
            Log.w(TAG, "AppLogger already initialized")
            return
        }
        
        try {
            this.context = context.applicationContext
            this.startTime = System.currentTimeMillis()
            this.sessionId = generateSessionId()
            
            // 创建日志目录
            logDir = File(context.filesDir, LOG_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            
            // 创建当前会话的日志文件
            val timestamp = fileNameFormat.format(Date(startTime))
            logFile = File(logDir, "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION")
            
            // 清理旧日志
            cleanOldLogs()
            
            // 设置全局异常处理器
            setupCrashHandler()
            
            // 启动定时刷新任务
            startFlushTask()
            
            isInitialized.set(true)
            
            // 写入启动信息
            writeStartupInfo()
            
            Log.d(TAG, "AppLogger initialized, log file: ${logFile?.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AppLogger", e)
        }
    }
    
    /**
     * 关闭日志系统
     * 应在 Application.onTerminate() 或应用退出时调用
     */
    @Synchronized
    fun shutdown() {
        if (!isInitialized.get() || isShutdown.get()) return
        
        try {
            isShutdown.set(true)
            
            // 写入退出信息
            writeShutdownInfo()
            
            // 刷新所有缓冲
            flushBuffer()
            
            // 关闭执行器
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)
            
            Log.d(TAG, "AppLogger shutdown completed")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during AppLogger shutdown", e)
        }
    }
    
    // ==================== 公共日志方法 ====================
    
    /** Verbose 级别日志 */
    fun v(tag: String, message: String) {
        log(Level.VERBOSE, Category.GENERAL, tag, message)
    }
    
    /** Debug 级别日志 */
    fun d(tag: String, message: String) {
        log(Level.DEBUG, Category.GENERAL, tag, message)
    }
    
    /** Info 级别日志 */
    fun i(tag: String, message: String) {
        log(Level.INFO, Category.GENERAL, tag, message)
    }
    
    /** Warn 级别日志 */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, Category.GENERAL, tag, message, throwable)
    }
    
    /** Error 级别日志 */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, Category.GENERAL, tag, message, throwable)
    }
    
    /** Fatal 级别日志（严重错误/崩溃） */
    fun f(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.FATAL, Category.GENERAL, tag, message, throwable)
    }
    
    // ==================== 分类日志方法 ====================
    
    /** 生命周期日志 */
    fun lifecycle(component: String, event: String, details: String = "") {
        val message = buildString {
            append("$component.$event")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.INFO, Category.LIFECYCLE, "Lifecycle", message)
    }
    
    /** 用户操作日志 */
    fun userAction(action: String, details: String = "") {
        val message = buildString {
            append("Action: $action")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.INFO, Category.USER_ACTION, "UserAction", message)
    }
    
    /** UI 操作日志 */
    fun ui(component: String, action: String, details: String = "") {
        val message = buildString {
            append("$component: $action")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.UI, "UI", message)
    }
    
    /** 网络请求日志 */
    fun network(action: String, url: String = "", details: String = "") {
        val message = buildString {
            append("$action")
            if (url.isNotEmpty()) append(" | URL: $url")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.NETWORK, "Network", message)
    }
    
    /** 数据库操作日志 */
    fun database(operation: String, table: String = "", details: String = "") {
        val message = buildString {
            append("$operation")
            if (table.isNotEmpty()) append(" | Table: $table")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.DATABASE, "Database", message)
    }
    
    /** WebView 日志 */
    fun webView(action: String, url: String = "", details: String = "") {
        val message = buildString {
            append("$action")
            if (url.isNotEmpty()) append(" | URL: $url")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.WEBVIEW, "WebView", message)
    }
    
    /** APK 构建日志 */
    fun apkBuild(step: String, details: String = "", error: Throwable? = null) {
        val level = if (error != null) Level.ERROR else Level.INFO
        val message = buildString {
            append("Step: $step")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(level, Category.APK_BUILD, "ApkBuild", message, error)
    }
    
    /** 扩展模块日志 */
    fun extension(moduleName: String, action: String, details: String = "") {
        val message = buildString {
            append("Module: $moduleName | Action: $action")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.EXTENSION, "Extension", message)
    }
    
    /** AI 功能日志 */
    fun ai(provider: String, action: String, details: String = "") {
        val message = buildString {
            append("Provider: $provider | Action: $action")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.AI, "AI", message)
    }
    
    /** 加密相关日志 */
    fun crypto(operation: String, details: String = "", error: Throwable? = null) {
        val level = if (error != null) Level.ERROR else Level.DEBUG
        val message = buildString {
            append("Operation: $operation")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(level, Category.CRYPTO, "Crypto", message, error)
    }
    
    /** 媒体处理日志 */
    fun media(action: String, type: String = "", details: String = "") {
        val message = buildString {
            append("$action")
            if (type.isNotEmpty()) append(" | Type: $type")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.MEDIA, "Media", message)
    }
    
    /** 文件操作日志 */
    fun file(operation: String, path: String = "", details: String = "") {
        val message = buildString {
            append("$operation")
            if (path.isNotEmpty()) append(" | Path: $path")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.FILE, "File", message)
    }
    
    /** 系统日志 */
    fun system(event: String, details: String = "") {
        val message = buildString {
            append("Event: $event")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.INFO, Category.SYSTEM, "System", message)
    }
    
    // ==================== 日志查询方法 ====================
    
    /** 获取当前日志文件路径 */
    fun getLogFilePath(): String? = logFile?.absolutePath
    
    /** 获取日志目录路径 */
    fun getLogDirPath(): String? = logDir?.absolutePath
    
    /** 获取当前会话 ID */
    fun getSessionId(): String = sessionId
    
    /** 获取所有日志文件列表 */
    fun getAllLogFiles(): List<File> {
        return logDir?.listFiles()
            ?.filter { it.name.startsWith(LOG_FILE_PREFIX) && it.name.endsWith(LOG_FILE_EXTENSION) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
    
    /** 获取当前日志内容 */
    fun getLogContent(maxLines: Int = 1000): String {
        flushBuffer()  // 先刷新缓冲
        return try {
            logFile?.let { file ->
                if (file.exists()) {
                    val lines = file.readLines()
                    val startIndex = maxOf(0, lines.size - maxLines)
                    lines.subList(startIndex, lines.size).joinToString("\n")
                } else {
                    "日志文件不存在"
                }
            } ?: "日志未初始化"
        } catch (e: Exception) {
            "读取日志失败: ${e.message}"
        }
    }
    
    /** 获取运行时间统计 */
    fun getSessionDuration(): Long {
        return System.currentTimeMillis() - startTime
    }
    
    /** 手动刷新缓冲 */
    fun flush() {
        flushBuffer()
    }
    
    // ==================== 私有方法 ====================
    
    private fun log(
        level: Level,
        category: Category,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        if (!isInitialized.get() || isShutdown.get()) return
        if (level.value < minLevel.value) return
        
        try {
            val timestamp = dateFormat.format(Date())
            val pid = Process.myPid()
            val tid = Process.myTid()
            
            val logLine = buildString {
                append("$timestamp ")
                append("$pid-$tid ")
                append("[${level.label}] ")
                append("[${category.label}] ")
                append("[$tag] ")
                append(message)
                if (throwable != null) {
                    append("\n")
                    append(getStackTraceString(throwable))
                }
            }
            
            // 添加到缓冲
            logBuffer.offer(logLine)
            
            // 如果缓冲过大，立即刷新
            if (logBuffer.size >= MAX_BUFFER_SIZE) {
                flushBuffer()
            }
            
            // 同时输出到 Logcat
            if (logToLogcat) {
                val logcatTag = "[$category] $tag"
                when (level) {
                    Level.VERBOSE -> Log.v(logcatTag, message, throwable)
                    Level.DEBUG -> Log.d(logcatTag, message, throwable)
                    Level.INFO -> Log.i(logcatTag, message, throwable)
                    Level.WARN -> Log.w(logcatTag, message, throwable)
                    Level.ERROR -> Log.e(logcatTag, message, throwable)
                    Level.FATAL -> Log.wtf(logcatTag, message, throwable)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log", e)
        }
    }
    
    private fun writeStartupInfo() {
        val separator = "═".repeat(70)
        val info = buildString {
            append("\n$separator\n")
            append("  应用启动 - WebToApp 运行日志\n")
            append("$separator\n")
            append("  会话 ID:      $sessionId\n")
            append("  启动时间:     ${dateFormat.format(Date(startTime))}\n")
            append("  包名:         ${context?.packageName ?: "N/A"}\n")
            append("  版本:         ${getAppVersion()}\n")
            append("  设备型号:     ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("  Android版本:  ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("  设备指纹:     ${Build.FINGERPRINT}\n")
            append("  CPU ABI:      ${Build.SUPPORTED_ABIS.joinToString(", ")}\n")
            append("  可用内存:     ${getAvailableMemory()} MB\n")
            append("  堆内存限制:   ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB\n")
            append("$separator\n\n")
        }
        
        writeDirectly(info)
    }
    
    private fun writeShutdownInfo() {
        val duration = getSessionDuration()
        val hours = duration / 3600000
        val minutes = (duration % 3600000) / 60000
        val seconds = (duration % 60000) / 1000
        
        val separator = "═".repeat(70)
        val info = buildString {
            append("\n$separator\n")
            append("  应用退出\n")
            append("$separator\n")
            append("  会话 ID:      $sessionId\n")
            append("  退出时间:     ${dateFormat.format(Date())}\n")
            append("  运行时长:     ${hours}h ${minutes}m ${seconds}s\n")
            append("  当前内存:     ${getUsedMemory()} MB\n")
            append("$separator\n")
        }
        
        writeDirectly(info)
    }
    
    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val separator = "!".repeat(70)
                val crashInfo = buildString {
                    append("\n$separator\n")
                    append("  !! 应用崩溃 !!\n")
                    append("$separator\n")
                    append("  时间:       ${dateFormat.format(Date())}\n")
                    append("  会话 ID:    $sessionId\n")
                    append("  线程:       ${thread.name} (id=${thread.id})\n")
                    append("  异常类型:   ${throwable.javaClass.name}\n")
                    append("  异常信息:   ${throwable.message}\n")
                    append("  堆栈跟踪:\n")
                    append(getStackTraceString(throwable))
                    append("\n$separator\n")
                }
                
                // 直接同步写入（崩溃时不能依赖异步）
                writeDirectly(crashInfo)
                
                Log.e(TAG, "Application crash logged", throwable)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash", e)
            }
            
            // 调用默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    private fun startFlushTask() {
        executor.scheduleWithFixedDelay({
            try {
                flushBuffer()
                checkLogFileSize()
            } catch (e: Exception) {
                Log.e(TAG, "Error in flush task", e)
            }
        }, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS)
    }
    
    @Synchronized
    private fun flushBuffer() {
        if (logBuffer.isEmpty()) return
        
        try {
            val file = logFile ?: return
            
            FileWriter(file, true).use { writer ->
                var line = logBuffer.poll()
                while (line != null) {
                    writer.append(line)
                    writer.append("\n")
                    line = logBuffer.poll()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush buffer", e)
        }
    }
    
    @Synchronized
    private fun writeDirectly(content: String) {
        try {
            val file = logFile ?: return
            FileWriter(file, true).use { writer ->
                writer.append(content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write directly", e)
        }
    }
    
    private fun checkLogFileSize() {
        try {
            logFile?.let { file ->
                if (file.exists() && file.length() > MAX_LOG_SIZE) {
                    // 日志文件过大，创建新文件
                    val timestamp = fileNameFormat.format(Date())
                    val newFile = File(logDir, "${LOG_FILE_PREFIX}${timestamp}_cont$LOG_FILE_EXTENSION")
                    logFile = newFile
                    
                    writeDirectly("\n[日志续写] 原文件: ${file.name} -> 新文件: ${newFile.name}\n")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check log file size", e)
        }
    }
    
    private fun cleanOldLogs() {
        try {
            val logFiles = logDir?.listFiles()
                ?.filter { it.name.startsWith(LOG_FILE_PREFIX) && it.name.endsWith(LOG_FILE_EXTENSION) }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            
            if (logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old log: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean old logs", e)
        }
    }
    
    private fun generateSessionId(): String {
        return UUID.randomUUID().toString().substring(0, 8).uppercase()
    }
    
    private fun getAppVersion(): String {
        return try {
            context?.packageManager?.getPackageInfo(context?.packageName ?: "", 0)?.let { info ->
                "${info.versionName} (${info.longVersionCode})"
            } ?: "N/A"
        } catch (e: Exception) {
            "N/A"
        }
    }
    
    private fun getAvailableMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            (runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory()) / 1024 / 1024
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getUsedMemory(): Long {
        return try {
            val runtime = Runtime.getRuntime()
            (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
}
