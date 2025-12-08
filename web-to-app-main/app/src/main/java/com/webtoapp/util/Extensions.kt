package com.webtoapp.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * 扩展函数集合
 */

// Context扩展
fun Context.toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (e: Exception) {
        toast("无法打开链接")
    }
}

fun Context.shareText(text: String, title: String = "分享") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title))
}

// String扩展
fun String.isValidUrl(): Boolean {
    return try {
        val uri = Uri.parse(this)
        uri.scheme in listOf("http", "https") && !uri.host.isNullOrBlank()
    } catch (e: Exception) {
        false
    }
}

fun String.normalizeUrl(): String {
    val trimmed = this.trim()
    return if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
        "https://$trimmed"
    } else {
        trimmed
    }
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

// Long扩展（时间戳格式化）
fun Long.toDateString(pattern: String = "yyyy-MM-dd HH:mm"): String {
    return java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault())
        .format(java.util.Date(this))
}

// Collection扩展
fun <T> List<T>.safeGet(index: Int): T? = getOrNull(index)
