package com.webtoapp.core.cloud

import com.webtoapp.core.logging.AppLogger
import java.util.Locale

/**
 * GitHub URL accelerator — rewrites raw GitHub URLs through regional CDN proxies.
 *
 * Strategy:
 * - CN (Chinese) users: route through gh-proxy mirrors for faster access
 * - Global users: use GitHub URLs directly
 * - Automatic region detection based on system locale
 * - Fallback: if proxy fails, original URL is still available
 */
object GitHubAccelerator {

    private const val TAG = "GitHubAccelerator"

    enum class Region { CN, GLOBAL }

    /** CN proxy prefixes (ordered by reliability) */
    private val CN_PROXIES = listOf(
        "https://ghfast.top/",
        "https://gh-proxy.com/",
        "https://ghproxy.cc/",
    )

    /** GitHub URL patterns that need acceleration */
    private val GITHUB_PATTERNS = listOf(
        "https://github.com/",
        "https://raw.githubusercontent.com/",
        "https://objects.githubusercontent.com/",
        "https://uploads.github.com/",
    )

    private var _region: Region? = null
    private var _preferredProxyIndex = 0

    /** Set region manually (null = auto-detect) */
    fun setRegion(region: Region?) {
        _region = region
    }

    /** Get current region (auto-detect from locale if not set) */
    fun getRegion(): Region {
        _region?.let { return it }
        val lang = Locale.getDefault().language
        val country = Locale.getDefault().country
        return if (lang == "zh" || country in listOf("CN", "TW", "HK", "MO")) Region.CN else Region.GLOBAL
    }

    /**
     * Accelerate a URL if it points to GitHub.
     * - CN: rewrite through proxy
     * - Global: return as-is
     * - Non-GitHub URLs: return as-is
     */
    fun accelerate(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (getRegion() != Region.CN) return url
        if (!isGitHubUrl(url)) return url
        return applyProxy(url)
    }

    /**
     * Get accelerated URL with fallback list.
     * Returns the original URL as last fallback.
     */
    fun accelerateWithFallbacks(url: String?): List<String> {
        if (url.isNullOrBlank()) return emptyList()
        if (getRegion() != Region.CN) return listOf(url)
        if (!isGitHubUrl(url)) return listOf(url)
        return CN_PROXIES.map { proxy -> "${proxy}${url}" } + url
    }

    /**
     * Pick the best URL from a list of mirror URLs (e.g., urlGitee, urlGithub).
     * CN: prefer Gitee, then proxied GitHub
     * Global: prefer GitHub directly
     */
    fun pickBestUrl(vararg urls: String?): String? {
        if (getRegion() == Region.CN) {
            // CN: prefer Gitee (faster in China), then proxied GitHub
            val gitee = urls.firstOrNull { !it.isNullOrBlank() && it.contains("gitee") }
            if (gitee != null) return gitee
            val github = urls.firstOrNull { !it.isNullOrBlank() && isGitHubUrl(it) }
            if (github != null) return applyProxy(github)
            return urls.firstOrNull { !it.isNullOrBlank() }
        } else {
            // Global: prefer GitHub directly, then Gitee
            val github = urls.firstOrNull { !it.isNullOrBlank() && isGitHubUrl(it) }
            if (github != null) return github
            return urls.firstOrNull { !it.isNullOrBlank() }
        }
    }

    /** Mark current proxy as failed, rotate to next */
    fun rotateProxy() {
        _preferredProxyIndex = (_preferredProxyIndex + 1) % CN_PROXIES.size
        AppLogger.w(TAG, "Rotated to proxy index $_preferredProxyIndex: ${CN_PROXIES[_preferredProxyIndex]}")
    }

    private fun isGitHubUrl(url: String): Boolean {
        return GITHUB_PATTERNS.any { url.startsWith(it) }
    }

    private fun applyProxy(url: String): String {
        val proxy = CN_PROXIES[_preferredProxyIndex]
        return "${proxy}${url}"
    }
}
