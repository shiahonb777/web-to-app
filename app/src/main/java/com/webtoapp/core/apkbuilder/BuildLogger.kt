package com.webtoapp.core.apkbuilder

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.*





class BuildLogger(private val context: Context) {

    private val logDir = File(context.getExternalFilesDir(null), "build_logs").apply { mkdirs() }
    private var currentLogFile: File? = null


    private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private fun fileNameFormat() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    companion object {
        private const val TAG = "BuildLogger"
        private const val MAX_LOG_FILES = 10
        private val SAFE_FILENAME_REGEX = Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5]")
    }




    fun startNewLog(appName: String): File {

        cleanOldLogs()

        val timestamp = fileNameFormat().format(Date())
        val safeAppName = appName.replace(SAFE_FILENAME_REGEX, "_").take(20)
        currentLogFile = File(logDir, "build_${safeAppName}_$timestamp.log")

        log("========================================")
        log(Strings.buildLogTitle)
        log("${Strings.buildLogAppName}: $appName")
        log("${Strings.buildLogStartTime}: ${dateFormat().format(Date())}")
        log("========================================")

        return currentLogFile!!
    }




    fun log(message: String) {
        val timestamp = dateFormat().format(Date())
        val logLine = "[$timestamp] INFO: $message"
        writeToFile(logLine)
        AppLogger.d(TAG, message)
    }




    fun debug(message: String) {
        val timestamp = dateFormat().format(Date())
        val logLine = "[$timestamp] DEBUG: $message"
        writeToFile(logLine)
        AppLogger.d(TAG, message)
    }




    fun warn(message: String) {
        val timestamp = dateFormat().format(Date())
        val logLine = "[$timestamp] WARN: $message"
        writeToFile(logLine)
        AppLogger.w(TAG, message)
    }




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




    fun section(title: String) {
        log("----------------------------------------")
        log(">>> $title")
        log("----------------------------------------")
    }




    fun logKeyValue(key: String, value: Any?) {
        log("  $key = $value")
    }




    fun logList(title: String, items: List<Any?>) {
        log("  $title (${items.size} ${Strings.buildLogItems}):")
        items.forEachIndexed { index, item ->
            log("    [$index] $item")
        }
    }




    fun endLog(success: Boolean, message: String = "") {
        log("========================================")
        log(if (success) Strings.buildLogSuccess else Strings.buildLogFailed)
        if (message.isNotBlank()) {
            log("${Strings.buildLogResult}: $message")
        }
        log("${Strings.buildLogEndTime}: ${dateFormat().format(Date())}")
        log("${Strings.buildLogLogFile}: ${currentLogFile?.absolutePath}")
        log("========================================")
    }




    fun getCurrentLogPath(): String? = currentLogFile?.absolutePath




    fun readLogContent(path: String?, maxChars: Int = 20000): String? {
        if (path.isNullOrBlank()) return null

        return try {
            val file = File(path)
            if (!file.exists() || !file.isFile) {
                null
            } else {
                val content = file.readText()
                if (content.length <= maxChars) {
                    content
                } else {
                    content.takeLast(maxChars)
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "读取构建日志失败", e)
            null
        }
    }




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
                    AppLogger.d(TAG, "Deleted old log: ${file.name}")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "清理旧日志失败", e)
        }
    }
}
