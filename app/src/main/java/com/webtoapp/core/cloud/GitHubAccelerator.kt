package com.webtoapp.core.cloud

import com.webtoapp.core.logging.AppLogger
import java.util.Locale










object GitHubAccelerator {

    private const val TAG = "GitHubAccelerator"

    enum class Region { CN, GLOBAL }


    private val CN_PROXIES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/",
        "https://ghproxy.cc/",
    )


    private val GITHUB_PATTERNS = listOf(
        "https://github.com/",
        "https://raw.githubusercontent.com/",
        "https://objects.githubusercontent.com/",
        "https://uploads.github.com/",
    )

    private var _region: Region? = null
    private var _preferredProxyIndex = 0


    fun setRegion(region: Region?) {
        _region = region
    }


    fun getRegion(): Region {
        _region?.let { return it }
        val lang = Locale.getDefault().language
        val country = Locale.getDefault().country
        return if (lang == "zh" || country in listOf("CN", "TW", "HK", "MO")) Region.CN else Region.GLOBAL
    }







    fun accelerate(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (getRegion() != Region.CN) return url
        if (!isGitHubUrl(url)) return url
        // DNS direct connection: if we have IP mappings, return original URL
        // The custom DNS (GitHubHostsDns) will resolve it to the correct IP automatically
        // This is faster than proxy (no extra hop) and more stable
        if (hasDnsMapping(url)) return url
        return applyProxy(url)
    }





    fun accelerateWithFallbacks(url: String?): List<String> {
        if (url.isNullOrBlank()) return emptyList()
        if (getRegion() != Region.CN) return listOf(url)
        if (!isGitHubUrl(url)) return listOf(url)
        // DNS direct is first priority, then proxies as fallbacks
        if (hasDnsMapping(url)) return listOf(url) + CN_PROXIES.map { proxy -> "${proxy}${url}" }
        return CN_PROXIES.map { proxy -> "${proxy}${url}" } + url
    }






    fun pickBestUrl(vararg urls: String?): String? {
        if (getRegion() == Region.CN) {
            // Gitee is fastest for CN (domestic CDN)
            val gitee = urls.firstOrNull { !it.isNullOrBlank() && it.contains("gitee") }
            if (gitee != null) return gitee
            // GitHub via DNS direct (no proxy needed if we have IP mapping)
            val github = urls.firstOrNull { !it.isNullOrBlank() && isGitHubUrl(it) }
            if (github != null) return accelerate(github)
            return urls.firstOrNull { !it.isNullOrBlank() }
        } else {
            val github = urls.firstOrNull { !it.isNullOrBlank() && isGitHubUrl(it) }
            if (github != null) return github
            return urls.firstOrNull { !it.isNullOrBlank() }
        }
    }


    fun rotateProxy() {
        _preferredProxyIndex = (_preferredProxyIndex + 1) % CN_PROXIES.size
        AppLogger.w(TAG, "Rotated to proxy index $_preferredProxyIndex: ${CN_PROXIES[_preferredProxyIndex]}")
    }

    private fun isGitHubUrl(url: String): Boolean {
        return GITHUB_PATTERNS.any { url.startsWith(it) }
    }

    private fun hasDnsMapping(url: String): Boolean {
        val host = try {
            java.net.URI(url).host
        } catch (e: Exception) {
            url.substringAfter("://").substringBefore("/")
        }
        return GitHubHostsDns.isGitHubDomain(host)
    }

    private fun applyProxy(url: String): String {
        val proxy = CN_PROXIES[_preferredProxyIndex]
        return "${proxy}${url}"
    }
}
