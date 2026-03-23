package com.webtoapp.core.shell

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shell 应用日志工具
 * 
 * 用于构建出来的 APK 记录运行日志，便于：
 * 1. 追踪应用启动和功能使用情况
 * 2. 定位闪退和异常问题
 * 3. 调试和问题排查
 * 
 * 日志文件位置：Android/data/[包名]/files/logs/app_log.txt
 * 用户可通过文件管理器访问此文件并发送给开发者
 */
object ShellLogger {
    
    private const val TAG = "ShellLogger"
    private const val LOG_FILE_NAME = "app_log.txt"
    private const val MAX_LOG_SIZE = 2 * 1024 * 1024L // 2MB 最大日志大小，超过则清空重写
    
    private var logFile: File? = null
    private var isInitialized = false
    private var appName: String = "ShellApp"
    private var appVersion: String = "1.0.0"
    private var packageName: String = ""
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    /**
     * 初始化日志系统
     * 
     * 日志文件保存在外部存储的应用专属目录，用户可通过文件管理器访问：
     * Android/data/[包名]/files/logs/app_log.txt
     */
    @Synchronized
    fun init(context: Context, appName: String = "ShellApp", appVersion: String = "1.0.0") {
        if (isInitialized) return
        
        try {
            this.appName = appName
            this.appVersion = appVersion
            this.packageName = context.packageName
            
            // 使用外部存储的应用专属目录，用户可通过文件管理器访问
            // Path: Android/data/[包名]/files/logs/
            val externalDir = context.getExternalFilesDir(null)
            val logDir = if (externalDir != null) {
                File(externalDir, "logs")
            } else {
                // 降级到内部存储
                File(context.filesDir, "logs")
            }
            
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            // Create日志文件
            logFile = File(logDir, LOG_FILE_NAME)
            
            // 如果日志文件过大，清空重写
            checkAndTruncateLog()
            
            isInitialized = true
            
            // Set全局异常处理器（在记录启动日志之前，确保崩溃能被捕获）
            setupCrashHandler(context)
            
            // 记录应用启动日志
            logAppStart(context)
            
            Log.d(TAG, "ShellLogger 初始化成功")
            Log.d(TAG, "日志文件路径: ${logFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "ShellLogger 初始化失败", e)
        }
    }
    
    /**
     * 记录信息日志
     */
    fun i(tag: String, message: String) {
        log("INFO", tag, message)
        Log.i(tag, message)
    }
    
    /**
     * 记录调试日志
     */
    fun d(tag: String, message: String) {
        log("DEBUG", tag, message)
        Log.d(tag, message)
    }
    
    /**
     * 记录警告日志
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log("WARN", tag, message, throwable)
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * 记录错误日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * 记录功能使用日志
     */
    fun logFeature(feature: String, action: String, details: String = "") {
        val message = buildString {
            append("Feature: $feature, Action: $action")
            if (details.isNotEmpty()) {
                append(", Details: $details")
            }
        }
        log("FEATURE", "FeatureTrack", message)
    }
    
    /**
     * 记录 WebView 相关日志
     */
    fun logWebView(action: String, url: String = "", details: String = "") {
        val message = buildString {
            append("Action: $action")
            if (url.isNotEmpty()) {
                append(", URL: $url")
            }
            if (details.isNotEmpty()) {
                append(", Details: $details")
            }
        }
        log("WEBVIEW", "WebView", message)
    }
    
    /**
     * 记录生命周期日志
     */
    fun logLifecycle(component: String, event: String) {
        log("LIFECYCLE", component, "Event: $event")
    }
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String? {
        return logFile?.absolutePath
    }
    
    /**
     * 获取日志内容
     */
    fun getLogContent(maxLines: Int = 500): String {
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
    
    /**
     * 清空日志
     */
    fun clearLog() {
        try {
            logFile?.let { file ->
                if (file.exists()) {
                    file.writeText("")
                    i(TAG, "日志已清空")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清空日志失败", e)
        }
    }
    
    // ==================== 私有方法 ====================
    
    private fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        if (!isInitialized || logFile == null) return
        
        try {
            val timestamp = dateFormat.format(Date())
            val logLine = buildString {
                append("[$timestamp] [$level] [$tag] $message")
                if (throwable != null) {
                    append("\n")
                    append(getStackTraceString(throwable))
                }
                append("\n")
            }
            
            synchronized(this) {
                FileWriter(logFile, true).use { writer ->
                    writer.append(logLine)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入日志失败", e)
        }
    }
    
    private fun logAppStart(context: Context) {
        val separator = "=" .repeat(60)
        val startInfo = buildString {
            append("\n$separator\n")
            append("应用启动 - ${dateFormat.format(Date())}\n")
            append("$separator\n")
            append("应用名称: $appName\n")
            append("应用版本: $appVersion\n")
            append("包名: ${context.packageName}\n")
            append("设备型号: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            append("Android版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            append("设备指纹: ${Build.FINGERPRINT}\n")
            append("$separator\n")
        }
        
        try {
            synchronized(this) {
                FileWriter(logFile, true).use { writer ->
                    writer.append(startInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入启动日志失败", e)
        }
    }
    
    private fun setupCrashHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashInfo = buildString {
                    append("\n")
                    append("!" .repeat(60))
                    append("\n")
                    append("应用崩溃 - ${dateFormat.format(Date())}\n")
                    append("!" .repeat(60))
                    append("\n")
                    append("线程: ${thread.name}\n")
                    append("异常类型: ${throwable.javaClass.name}\n")
                    append("异常信息: ${throwable.message}\n")
                    append("堆栈跟踪:\n")
                    append(getStackTraceString(throwable))
                    append("\n")
                    append("!" .repeat(60))
                    append("\n")
                }
                
                synchronized(this@ShellLogger) {
                    FileWriter(logFile, true).use { writer ->
                        writer.append(crashInfo)
                    }
                }
                
                Log.e(TAG, "应用崩溃已记录到日志")
            } catch (e: Exception) {
                Log.e(TAG, "记录崩溃日志失败", e)
            }
            
            // 调用默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    /**
     * 检查日志文件大小，超过限制则保留最后部分
     */
    private fun checkAndTruncateLog() {
        try {
            logFile?.let { file ->
                if (file.exists() && file.length() > MAX_LOG_SIZE) {
                    // 保留最后 500KB 的日志
                    val keepSize = 500 * 1024
                    val content = file.readText()
                    if (content.length > keepSize) {
                        val truncatedContent = buildString {
                            append("=".repeat(60))
                            append("\n[日志已截断] 原文件过大，仅保留最近的日志记录\n")
                            append("截断时间: ${dateFormat.format(Date())}\n")
                            append("=".repeat(60))
                            append("\n\n")
                            append(content.takeLast(keepSize))
                        }
                        file.writeText(truncatedContent)
                    }
                    Log.d(TAG, "日志文件已截断")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "截断日志文件失败", e)
        }
    }
    
    /**
     * 获取日志文件位置提示（用于告知用户）
     */
    fun getLogFileLocationHint(): String {
        return "Android/data/$packageName/files/logs/$LOG_FILE_NAME"
    }
    
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
}
