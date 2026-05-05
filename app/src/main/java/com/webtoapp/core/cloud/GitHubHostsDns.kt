package com.webtoapp.core.cloud

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Custom OkHttp DNS resolver that uses GitHub hosts IP mappings
 * to bypass DNS pollution in China.
 *
 * Instead of routing through unstable third-party proxies (ghfast.top, etc.),
 * this directly resolves GitHub domains to their known working IPs,
 * which is faster (no proxy hop) and more stable.
 *
 * IP mappings are fetched from the github-hosts project and cached locally.
 */
object GitHubHostsDns : Dns {

    private const val TAG = "GitHubHostsDns"
    private const val HOSTS_URL = "https://raw.githubusercontent.com/maxiaof/github-hosts/master/hosts"
    private const val FALLBACK_HOSTS_URL = "https://ghfast.top/https://raw.githubusercontent.com/maxiaof/github-hosts/master/hosts"
    private const val PREFS_NAME = "github_hosts_dns"
    private const val KEY_LAST_SUCCESS_AT = "last_success_at"
    private const val KEY_LAST_ATTEMPT_AT = "last_attempt_at"
    private const val SUCCESS_REFRESH_TTL_MS = 24 * 60 * 60 * 1000L
    private const val FAILURE_RETRY_TTL_MS = 6 * 60 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshInFlight = AtomicBoolean(false)

    // Domain -> List of IP addresses
    private val hostsMap = ConcurrentHashMap<String, List<String>>()

    // Hardcoded fallback IPs from github-hosts (updated 2026-04-30)
    // Used when network fetch fails
    private val fallbackHosts = mapOf(
        "raw.githubusercontent.com" to listOf("185.199.108.133", "185.199.109.133", "185.199.110.133", "185.199.111.133"),
        "github.githubassets.com" to listOf("185.199.111.215"),
        "avatars.githubusercontent.com" to listOf("185.199.108.133"),
        "avatars0.githubusercontent.com" to listOf("185.199.109.133"),
        "avatars1.githubusercontent.com" to listOf("185.199.109.133"),
        "avatars2.githubusercontent.com" to listOf("185.199.111.133"),
        "avatars3.githubusercontent.com" to listOf("185.199.111.133"),
        "avatars4.githubusercontent.com" to listOf("185.199.110.133"),
        "avatars5.githubusercontent.com" to listOf("185.199.108.133"),
        "user-images.githubusercontent.com" to listOf("185.199.110.133"),
        "media.githubusercontent.com" to listOf("185.199.109.133"),
        "camo.githubusercontent.com" to listOf("185.199.108.133"),
        "cloud.githubusercontent.com" to listOf("185.199.111.133"),
        "objects.githubusercontent.com" to listOf("185.199.110.133"),
        "desktop.githubusercontent.com" to listOf("185.199.110.133"),
        "favicons.githubusercontent.com" to listOf("185.199.109.133"),
        "codeload.github.com" to listOf("140.82.121.9"),
        "github.com" to listOf("140.82.121.4"),
        "api.github.com" to listOf("140.82.121.5"),
        "github.global.ssl.fastly.net" to listOf("146.75.121.194"),
        "github.map.fastly.net" to listOf("185.199.109.133"),
    )

    init {
        // Start with fallback hosts
        hostsMap.putAll(fallbackHosts)
    }

    /**
     * Fetch latest hosts from remote and update the mapping.
     * Called asynchronously on app startup.
     */
    fun refreshAsync(context: Context) {
        val appContext = context.applicationContext
        if (!shouldRefresh(appContext)) return
        if (!refreshInFlight.compareAndSet(false, true)) return

        scope.launch {
            try {
                markAttempt(appContext)
                val fetched = fetchHosts(HOSTS_URL)
                val resolved = if (fetched.isNotEmpty()) {
                    fetched
                } else {
                    // Try fallback URL through proxy
                    fetchHosts(FALLBACK_HOSTS_URL)
                }

                if (resolved.isNotEmpty()) {
                    hostsMap.putAll(resolved)
                    markSuccess(appContext)
                    AppLogger.i(TAG, "Updated GitHub hosts: ${resolved.size} domains")
                } else {
                    AppLogger.i(TAG, "GitHub hosts refresh returned no updates")
                }
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to refresh GitHub hosts, using fallback", e)
            } finally {
                refreshInFlight.set(false)
            }
        }
    }

    internal fun shouldRefresh(now: Long, lastSuccessAt: Long, lastAttemptAt: Long): Boolean {
        if (lastSuccessAt > 0 && now - lastSuccessAt < SUCCESS_REFRESH_TTL_MS) return false
        if (lastAttemptAt > 0 && now - lastAttemptAt < FAILURE_RETRY_TTL_MS) return false
        return true
    }

    private fun shouldRefresh(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return shouldRefresh(
            now = System.currentTimeMillis(),
            lastSuccessAt = prefs.getLong(KEY_LAST_SUCCESS_AT, 0L),
            lastAttemptAt = prefs.getLong(KEY_LAST_ATTEMPT_AT, 0L),
        )
    }

    private fun markAttempt(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_ATTEMPT_AT, System.currentTimeMillis())
            .apply()
    }

    private fun markSuccess(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis())
            .apply()
    }

    private fun fetchHosts(url: String): Map<String, List<String>> {
        return try {
            val connection = java.net.URL(url).openConnection()
            connection.connectTimeout = 8000
            connection.readTimeout = 10000
            val text = connection.getInputStream().bufferedReader().readText()
            parseHosts(text)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to fetch hosts from $url", e)
            emptyMap()
        }
    }

    private fun parseHosts(text: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 2) {
                    val ip = parts[0]
                    val domain = parts[1]
                    if (ip.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) && domain.contains("github")) {
                        result.getOrPut(domain) { mutableListOf() }.add(ip)
                    }
                }
            }
        return result
    }

    /**
     * Check if a hostname is a GitHub domain that we have IP mappings for.
     */
    fun isGitHubDomain(hostname: String): Boolean {
        return hostsMap.containsKey(hostname)
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val ips = hostsMap[hostname]
        if (ips != null && ips.isNotEmpty()) {
            return ips.mapNotNull { ip ->
                try {
                    InetAddress.getByName(ip)
                } catch (e: UnknownHostException) {
                    null
                }
            }.takeIf { it.isNotEmpty() }
                ?: fallthroughDns(hostname)
        }
        return fallthroughDns(hostname)
    }

    private fun fallthroughDns(hostname: String): List<InetAddress> {
        return try {
            Dns.SYSTEM.lookup(hostname)
        } catch (e: UnknownHostException) {
            AppLogger.w(TAG, "DNS lookup failed for $hostname", e)
            throw e
        }
    }
}
