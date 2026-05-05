package com.webtoapp.core.logging

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import com.webtoapp.util.threadLocalCompat
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



































@SuppressLint("StaticFieldLeak")
object AppLogger {

    private const val TAG = "AppLogger"
    private const val LOG_DIR_NAME = "logs"
    private const val LOG_FILE_PREFIX = "run_"
    private const val LOG_FILE_EXTENSION = ".log"
    private const val MAX_LOG_FILES = 20
    private const val MAX_LOG_SIZE = 10 * 1024 * 1024L
    private const val FLUSH_INTERVAL_MS = 3000L
    private const val MAX_BUFFER_SIZE = 100


    enum class Level(val value: Int, val label: String) {
        VERBOSE(0, "V"),
        DEBUG(1, "D"),
        INFO(2, "I"),
        WARN(3, "W"),
        ERROR(4, "E"),
        FATAL(5, "F")
    }


    enum class Category(val label: String) {
        SYSTEM("SYSTEM"),
        LIFECYCLE("LIFECYCLE"),
        UI("UI"),
        USER_ACTION("USER_ACTION"),
        NETWORK("NETWORK"),
        DATABASE("DATABASE"),
        WEBVIEW("WEBVIEW"),
        APK_BUILD("APK_BUILD"),
        EXTENSION("EXTENSION"),
        AI("AI"),
        CRYPTO("CRYPTO"),
        MEDIA("MEDIA"),
        FILE("FILE"),
        GENERAL("GENERAL")
    }

    private var context: Context? = null
    private var logFile: File? = null
    private var logDir: File? = null
    private var sessionId: String = ""
    private var startTime: Long = 0

    private val isInitialized = AtomicBoolean(false)
    private val isShutdown = AtomicBoolean(false)


    private val logBuffer = ConcurrentLinkedQueue<String>()
    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "AppLogger-Writer").apply { isDaemon = true }
    }


    private val dateFormat = threadLocalCompat {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    }
    private val fileNameFormat = threadLocalCompat {
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }


    var minLevel: Level = Level.VERBOSE


    var logToLogcat: Boolean = true





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


            logDir = File(context.filesDir, LOG_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }


            val timestamp = fileNameFormat.get()!!.format(Date(startTime))
            logFile = File(logDir, "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION")


            cleanOldLogs()


            setupCrashHandler()


            startFlushTask()

            isInitialized.set(true)


            writeStartupInfo()

            Log.d(TAG, "AppLogger initialized, log file: ${logFile?.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AppLogger", e)
        }
    }





    @Synchronized
    fun shutdown() {
        if (!isInitialized.get() || isShutdown.get()) return

        try {
            isShutdown.set(true)


            writeShutdownInfo()


            flushBuffer()


            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            Log.d(TAG, "AppLogger shutdown completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error during AppLogger shutdown", e)
        }
    }




    fun v(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.VERBOSE, Category.GENERAL, tag, message, throwable)
    }


    fun d(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.DEBUG, Category.GENERAL, tag, message, throwable)
    }


    fun i(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.INFO, Category.GENERAL, tag, message, throwable)
    }


    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, Category.GENERAL, tag, message, throwable)
    }


    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, Category.GENERAL, tag, message, throwable)
    }


    fun f(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.FATAL, Category.GENERAL, tag, message, throwable)
    }




    fun lifecycle(component: String, event: String, details: String = "") {
        val message = buildString {
            append("$component.$event")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.INFO, Category.LIFECYCLE, "Lifecycle", message)
    }


    fun userAction(action: String, details: String = "") {
        val message = buildString {
            append("Action: $action")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.INFO, Category.USER_ACTION, "UserAction", message)
    }


    fun ui(component: String, action: String, details: String = "") {
        val message = buildString {
            append("$component: $action")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.UI, "UI", message)
    }


    fun network(action: String, url: String = "", details: String = "") {
        val message = buildString {
            append("$action")
            if (url.isNotEmpty()) append(" | URL: $url")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.NETWORK, "Network", message)
    }


    fun database(operation: String, table: String = "", details: String = "") {
        val message = buildString {
            append("$operation")
            if (table.isNotEmpty()) append(" | Table: $table")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.DATABASE, "Database", message)
    }


    fun webView(action: String, url: String = "", details: String = "") {
        val message = buildString {
            append("$action")
            if (url.isNotEmpty()) append(" | URL: $url")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.WEBVIEW, "WebView", message)
    }


    fun apkBuild(step: String, details: String = "", error: Throwable? = null) {
        val level = if (error != null) Level.ERROR else Level.INFO
        val message = buildString {
            append("Step: $step")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(level, Category.APK_BUILD, "ApkBuild", message, error)
    }


    fun extension(moduleName: String, action: String, details: String = "") {
        val message = buildString {
            append("Module: $moduleName | Action: $action")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.EXTENSION, "Extension", message)
    }


    fun ai(provider: String, action: String, details: String = "") {
        val message = buildString {
            append("Provider: $provider | Action: $action")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.AI, "AI", message)
    }


    fun crypto(operation: String, details: String = "", error: Throwable? = null) {
        val level = if (error != null) Level.ERROR else Level.DEBUG
        val message = buildString {
            append("Operation: $operation")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(level, Category.CRYPTO, "Crypto", message, error)
    }


    fun media(action: String, type: String = "", details: String = "") {
        val message = buildString {
            append("$action")
            if (type.isNotEmpty()) append(" | Type: $type")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.MEDIA, "Media", message)
    }


    fun file(operation: String, path: String = "", details: String = "") {
        val message = buildString {
            append("$operation")
            if (path.isNotEmpty()) append(" | Path: $path")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.DEBUG, Category.FILE, "File", message)
    }


    fun system(event: String, details: String = "") {
        val message = buildString {
            append("Event: $event")
            if (details.isNotEmpty()) append(" | $details")
        }
        log(Level.INFO, Category.SYSTEM, "System", message)
    }




    fun getLogFilePath(): String? = logFile?.absolutePath


    fun getLogDirPath(): String? = logDir?.absolutePath


    fun getSessionId(): String = sessionId


    fun getAllLogFiles(): List<File> {
        return logDir?.listFiles()
            ?.filter { it.name.startsWith(LOG_FILE_PREFIX) && it.name.endsWith(LOG_FILE_EXTENSION) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }


    fun getLogContent(maxLines: Int = 1000): String {
        flushBuffer()
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


    fun getRecentLogTail(maxChars: Int = 12000): String {
        flushBuffer()
        return try {
            val content = logFile?.takeIf { it.exists() }?.readText().orEmpty()
            when {
                content.isBlank() -> "日志为空"
                content.length <= maxChars -> content
                else -> content.takeLast(maxChars)
            }
        } catch (e: Exception) {
            "读取日志失败: ${e.message}"
        }
    }


    fun getSessionDuration(): Long {
        return System.currentTimeMillis() - startTime
    }


    fun flush() {
        flushBuffer()
    }



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
            val timestamp = dateFormat.get()!!.format(Date())
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


            logBuffer.offer(logLine)


            if (level.value >= Level.ERROR.value || logBuffer.size >= MAX_BUFFER_SIZE) {
                flushBuffer()
            }


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
            append("  启动时间:     ${dateFormat.get()!!.format(Date(startTime))}\n")
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
            append("  退出时间:     ${dateFormat.get()!!.format(Date())}\n")
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
            append("  时间:       ${dateFormat.get()!!.format(Date())}\n")
                    append("  会话 ID:    $sessionId\n")
                    append("  线程:       ${thread.name} (id=${thread.id})\n")
                    append("  异常类型:   ${throwable.javaClass.name}\n")
                    append("  异常信息:   ${throwable.message}\n")
                    append("  堆栈跟踪:\n")
                    append(getStackTraceString(throwable))
                    append("\n$separator\n")
                }


                writeDirectly(crashInfo)

                Log.e(TAG, "Application crash logged", throwable)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to log crash", e)
            }


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

                    val timestamp = fileNameFormat.get()!!.format(Date())
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
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
                "${info.versionName} ($versionCode)"
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
