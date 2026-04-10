package com.webtoapp.core.apkbuilder

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * APK 构建日志记录器
 * 将构建过程的日志保存到文件，方便调试
 */
class BuildLogger(private val context: Context) {
    
    private val logDir = File(context.getExternalFilesDir(null), "build_logs").apply { mkdirs() }
    private var currentLogFile: File? = null
    // Create SimpleDateFormat locally per call to ensure thread safety
    // (BuildLogger is used from Dispatchers.IO which may run on different threads)
    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private fun fileNameFormat() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    companion object {
        private const val TAG = "BuildLogger"
        private const val MAX_LOG_FILES = 10  // 最多保留10个日志文件
        private val SAFE_FILENAME_REGEX = Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]")
    }
    
    /**
     * 开始新的构建日志
     */
    fun startNewLog(appName: String): File {
        // Cleanup旧日志
        cleanOldLogs()
        
        val timestamp = fileNameFormat().format(Date())
        val safeAppName = appName.replace(SAFE_FILENAME_REGEX, "_").take(20)
        currentLogFile = File(logDir, "build_${safeAppName}_$timestamp.log")
        
        log("========================================")
        log("APK 构建日志")
        log("应用名称: $appName")
        log("开始时间: ${dateFormat().format(Date())}")
        log("========================================")
        
        return currentLogFile!!
    }
    
    /**
     * 记录普通日志
     */
    fun log(message: String) {
        val timestamp = dateFormat().format(Date())
        val logLine = "[$timestamp] INFO: $message"
        writeToFile(logLine)
        AppLogger.d(TAG, message)
    }
    
    /**
     * 记录调试日志
     */
    fun debug(message: String) {
        val timestamp = dateFormat().format(Date())
        val logLine = "[$timestamp] DEBUG: $message"
        writeToFile(logLine)
        AppLogger.d(TAG, message)
    }
    
    /**
     * 记录警告日志
     */
    fun warn(message: String) {
        val timestamp = dateFormat().format(Date())
        val logLine = "[$timestamp] WARN: $message"
        writeToFile(logLine)
        AppLogger.w(TAG, message)
    }
    
    /**
     * 记录错误日志
     */
    fun error(message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat().format(Date())
        val logLine = buildString {
            append("[$timestamp] ERROR: $message")
            throwable?.let {
                append("\n  Exception: ${it.javaClass.simpleName}: ${it.message}")
                append("\n  StackTrace:")
                it.stackTrace.take(10).forEach { element ->
                    append("\n    at $element")
                }
            }
        }
        writeToFile(logLine)
        AppLogger.e(TAG, message, throwable)
    }
    
    /**
     * 记录分隔线
     */
    fun section(title: String) {
        log("----------------------------------------")
        log(">>> $title")
        log("----------------------------------------")
    }
    
    /**
     * 记录键值对
     */
    fun logKeyValue(key: String, value: Any?) {
        log("  $key = $value")
    }
    
    /**
     * 记录列表
     */
    fun logList(title: String, items: List<Any?>) {
        log("  $title (${items.size} 项):")
        items.forEachIndexed { index, item ->
            log("    [$index] $item")
        }
    }
    
    /**
     * 结束日志
     */
    fun endLog(success: Boolean, message: String = "") {
        log("========================================")
        log("构建${if (success) "成功" else "失败"}")
        if (message.isNotBlank()) {
            log("结果: $message")
        }
        log("结束时间: ${dateFormat().format(Date())}")
        log("日志文件: ${currentLogFile?.absolutePath}")
        log("========================================")
    }
    
    /**
     * 获取当前日志文件路径
     */
    fun getCurrentLogPath(): String? = currentLogFile?.absolutePath
    
    /**
     * 获取所有日志文件
     */
    fun getAllLogFiles(): List<File> {
        return logDir.listFiles()
            ?.filter { it.extension == "log" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
    
    private val writeLock = Any()
    
    private fun writeToFile(line: String) {
        synchronized(writeLock) {
            try {
                currentLogFile?.appendText("$line\n")
            } catch (e: Exception) {
                AppLogger.e(TAG, "写入日志文件失败", e)
            }
        }
    }
    
    private fun cleanOldLogs() {
        try {
            val logFiles = logDir.listFiles()
                ?.filter { it.extension == "log" }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            
            if (logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    file.delete()
                    AppLogger.d(TAG, "删除旧日志: ${file.name}")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "清理旧日志失败", e)
        }
    }
}
