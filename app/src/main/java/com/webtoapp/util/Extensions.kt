package com.webtoapp.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared extension helpers.
 */

// Context extensions

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
 * Check whether the current network is Wi-Fi.
 */
fun Context.isWifiConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

fun Context.openUrl(url: String) {
    try {
        val safeUrl = normalizeExternalIntentUrl(url)
        if (safeUrl.isEmpty()) {
            AppLogger.w("Extensions", "Blocked openUrl with invalid or dangerous URL: $url")
            toast("无法打开链接")
            return
        }
        val scheme = getUrlScheme(safeUrl)
        val allowedSchemes = setOf("http", "https", "tel", "mailto", "sms", "smsto", "mms", "mmsto", "geo", "market")
        if (!isAllowedUrlScheme(safeUrl, allowedSchemes)) {
            AppLogger.w("Extensions", "Blocked openUrl with disallowed scheme: $scheme")
            toast("无法打开链接")
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl))
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
 * Return the total cache directory size.
 */
fun Context.getCacheDirSize(): Long {
    return cacheDir.calculateDirSize() + (externalCacheDir?.calculateDirSize() ?: 0L)
}

/**
 * Clear app cache directories.
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

// String extensions

fun String.isValidUrl(): Boolean {
    return try {
        val uri = Uri.parse(this)
        uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank()
    } catch (e: Exception) {
        false
    }
}

fun String.normalizeUrl(): String {
    // Keep the user input unchanged.
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
 * Truncate a string and append an ellipsis when needed.
 */
fun String.truncate(maxLength: Int, ellipsis: String = "..."): String {
    return if (length <= maxLength) this
    else take(maxLength - ellipsis.length) + ellipsis
}

/**
 * Parse an Int or return the fallback value.
 */
fun String.toIntOrDefault(default: Int = 0): Int {
    return toIntOrNull() ?: default
}

/**
 * Parse a Long or return the fallback value.
 */
fun String.toLongOrDefault(default: Long = 0L): Long {
    return toLongOrNull() ?: default
}

// Long extensions

// Thread-safe date formatter cache.
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
 * Format a byte count as a human-readable file size.
 */
fun Long.toFileSizeString(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f KB", this / 1024.0)
        this < 1024 * 1024 * 1024 -> String.format(java.util.Locale.getDefault(), "%.1f MB", this / (1024.0 * 1024))
        else -> String.format(java.util.Locale.getDefault(), "%.2f GB", this / (1024.0 * 1024 * 1024))
    }
}

/**
 * Format milliseconds as a duration string.
 */
fun Long.toDurationString(): String {
    val seconds = this / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, secs)
    }
}

// File extensions

/**
 * Compute the total size of a directory tree.
 */
fun File.calculateDirSize(): Long {
    if (!exists()) return 0L
    if (isFile) return length()
    
    return walkTopDown()
        .filter { it.isFile }
        .sumOf { it.length() }
}

/**
 * Delete a file or directory without throwing.
 */
fun File.safeDelete(): Boolean {
    return try {
        if (isDirectory) deleteRecursively() else delete()
    } catch (e: Exception) {
        false
    }
}

/**
 * Return the lowercase file extension.
 */
fun File.getExtension(): String {
    return name.substringAfterLast('.', "").lowercase()
}

// Collection extensions

fun <T> List<T>.safeGet(index: Int): T? = getOrNull(index)

/**
 * Return the first item or null.
 */
fun <T> List<T>.safeFirst(): T? = firstOrNull()

/**
 * Return the last item or null.
 */
fun <T> List<T>.safeLast(): T? = lastOrNull()

/**
 * Iterate through the list in batches.
 */
inline fun <T> List<T>.forEachBatch(batchSize: Int, action: (List<T>) -> Unit) {
    chunked(batchSize).forEach(action)
}
