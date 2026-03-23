package com.webtoapp.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 扩展函数集合
 */

// ==================== Context 扩展 ====================

fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * 检查是否为 WiFi 连接
 */
fun Context.isWifiConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (e: Exception) {
        toast("无法打开链接")
    }
}

fun Context.shareText(text: String, title: String = "Share") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title))
}

/**
 * 获取缓存目录大小
 */
fun Context.getCacheDirSize(): Long {
    return cacheDir.calculateDirSize() + (externalCacheDir?.calculateDirSize() ?: 0L)
}

/**
 * 清除缓存
 */
fun Context.clearCache(): Boolean {
    return try {
        cacheDir.deleteRecursively()
        externalCacheDir?.deleteRecursively()
        true
    } catch (e: Exception) {
        false
    }
}

// ==================== String 扩展 ====================

fun String.isValidUrl(): Boolean {
    return try {
        val uri = Uri.parse(this)
        uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank()
    } catch (e: Exception) {
        false
    }
}

fun String.normalizeUrl(): String {
    // 不自动补全协议，用户输入什么就用什么
    return this.trim()
}

fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")

fun String.md5(): String {
    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.sha256(): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun String.extractDomain(): String? {
    return try {
        Uri.parse(this.normalizeUrl()).host
    } catch (e: Exception) {
        null
    }
}

/**
 * 截断字符串，超出部分用省略号表示
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    return if (length <= maxLength) this
    else take(maxLength - ellipsis.length) + ellipsis
}

/**
 * 安全地转换为 Int
 */
fun String.toIntOrDefault(default: Int = 0): Int {
    return toIntOrNull() ?: default
}

/**
 * 安全地转换为 Long
 */
fun String.toLongOrDefault(default: Long = 0L): Long {
    return toLongOrNull() ?: default
}

// ==================== Long 扩展 ====================

// Date格式化器缓存（线程安全）
private val dateFormatCache = object : ThreadLocal<MutableMap<String, SimpleDateFormat>>() {
    override fun initialValue() = mutableMapOf<String, SimpleDateFormat>()
}

fun Long.toDateString(pattern: String = "yyyy-MM-dd HH:mm"): String {
    val formatMap = dateFormatCache.get()!!
    val formatter = formatMap.getOrPut(pattern) {
        SimpleDateFormat(pattern, Locale.getDefault())
    }
    return formatter.format(Date(this))
}

/**
 * 格式化为文件大小字符串
 */
fun Long.toFileSizeString(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> String.format("%.1f KB", this / 1024.0)
        this < 1024 * 1024 * 1024 -> String.format("%.1f MB", this / (1024.0 * 1024))
        else -> String.format("%.2f GB", this / (1024.0 * 1024 * 1024))
    }
}

/**
 * 格式化为时长字符串 (毫秒 -> HH:mm:ss)
 */
fun Long.toDurationString(): String {
    val seconds = this / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

// ==================== File 扩展 ====================

/**
 * 计算目录大小
 */
fun File.calculateDirSize(): Long {
    if (!exists()) return 0L
    if (isFile) return length()
    
    return walkTopDown()
        .filter { it.isFile }
        .sumOf { it.length() }
}

/**
 * 安全删除文件或目录
 */
fun File.safeDelete(): Boolean {
    return try {
        if (isDirectory) deleteRecursively() else delete()
    } catch (e: Exception) {
        false
    }
}

/**
 * 获取文件扩展名
 */
fun File.getExtension(): String {
    return name.substringAfterLast('.', "").lowercase()
}

// ==================== Collection 扩展 ====================

fun <T> List<T>.safeGet(index: Int): T? = getOrNull(index)

/**
 * 安全地获取第一个元素
 */
fun <T> List<T>.safeFirst(): T? = firstOrNull()

/**
 * 安全地获取最后一个元素
 */
fun <T> List<T>.safeLast(): T? = lastOrNull()

/**
 * 分批处理列表
 */
inline fun <T> List<T>.forEachBatch(batchSize: Int, action: (List<T>) -> Unit) {
    chunked(batchSize).forEach(action)
}
