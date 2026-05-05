package com.webtoapp.core.engine.shields

import android.net.Uri
import com.webtoapp.core.logging.AppLogger
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap














class HttpsUpgrader {

    companion object {
        private const val TAG = "HttpsUpgrader"


        private val SKIP_HOSTS = setOf(
            "localhost",
            "127.0.0.1",
            "0.0.0.0",
            "::1"
        )


        private val PRIVATE_IP_PREFIXES = listOf(
            "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.",
            "172.25.", "172.26.", "172.27.", "172.28.", "172.29.",
            "172.30.", "172.31.", "192.168."
        )


        private val IP_REGEX = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
    }


    private val failedDomains: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
    private val pendingUpgrades: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val httpFallbackAttempted: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())







    fun tryUpgrade(url: String): String? {
        if (!url.startsWith("http://")) return null

        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            return null
        }

        val host = uri.host?.lowercase() ?: return null


        if (shouldSkip(host)) return null


        if (failedDomains.contains(host)) return null


        val httpsUrl = url.replaceFirst("http://", "https://")
        pendingUpgrades.add(host)
        AppLogger.d(TAG, "HTTPS upgrade: $host")
        return httpsUrl
    }







    fun onSslError(url: String?): String? {
        if (url == null) return null
        val host = try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return null

        if (pendingUpgrades.remove(host)) {
            failedDomains.add(host)
            val httpUrl = url.replaceFirst("https://", "http://")
            AppLogger.w(TAG, "HTTPS upgrade failed for $host, falling back to HTTP")
            return httpUrl
        }
        return null
    }








    fun tryHttpFallback(url: String?): String? {
        if (url == null || !url.startsWith("https://")) return null
        val host = try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            null
        } ?: return null

        if (httpFallbackAttempted.contains(host)) {
            AppLogger.w(TAG, "HTTP fallback already attempted for $host, skipping to avoid loop")
            return null
        }

        httpFallbackAttempted.add(host)
        val httpUrl = url.replaceFirst("https://", "http://")
        AppLogger.w(TAG, "SSL error on $host, attempting HTTP fallback: $httpUrl")
        return httpUrl
    }


    fun onPageStarted() {
        pendingUpgrades.clear()
    }


    private fun shouldSkip(host: String): Boolean {

        if (host in SKIP_HOSTS) return true


        val firstChar = host.firstOrNull()
        if (firstChar == '1' || firstChar == '2') {
            if (PRIVATE_IP_PREFIXES.any { host.startsWith(it) }) return true
        }


        if (host.endsWith(".local") || host.endsWith(".localhost")) return true


        if (IP_REGEX.matches(host)) return true

        return false
    }


    fun clearFailedDomains() {
        failedDomains.clear()
        AppLogger.d(TAG, "Failed domains list cleared")
    }


    fun getFailedDomainCount(): Int = failedDomains.size
}
