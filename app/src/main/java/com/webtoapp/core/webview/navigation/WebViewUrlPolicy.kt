package com.webtoapp.core.webview

import android.net.Uri

internal class WebViewUrlPolicy {
    private val ipAddressRegex = Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")

    fun shouldUseConservativeScriptMode(pageUrl: String?): Boolean {
        val url = pageUrl?.takeIf { it.isNotBlank() } ?: return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return host !in WebViewManager.LOCAL_CLEARTEXT_HOSTS
    }

    fun shouldUseScriptlessMode(pageUrl: String?): Boolean {
        val url = pageUrl?.takeIf { it.isNotBlank() } ?: return false
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return WebViewManager.STRICT_COMPAT_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }

    fun isMapTileRequest(url: String): Boolean {
        val host = extractHostFromUrl(url) ?: return false
        return WebViewManager.MAP_TILE_HOST_SUFFIXES.any { suffix ->
            host == suffix || host.endsWith(".$suffix")
        }
    }

    fun isSameSiteHost(hostA: String, hostB: String): Boolean {
        if (hostA == hostB) return true
        val domainA = getRegistrableDomain(hostA) ?: hostA
        val domainB = getRegistrableDomain(hostB) ?: hostB
        return domainA == domainB
    }

    fun getRegistrableDomain(host: String): String? {
        if (host.isBlank()) return null
        val normalized = host.lowercase().trim('.')
        if (normalized.isBlank()) return null
        if (normalized == "localhost" || ipAddressRegex.matches(normalized)) return normalized

        val parts = normalized.split('.')
        if (parts.size < 2) return normalized

        val suffix2 = parts.takeLast(2).joinToString(".")
        return if (suffix2 in WebViewManager.COMMON_SECOND_LEVEL_TLDS && parts.size >= 3) {
            parts.takeLast(3).joinToString(".")
        } else {
            parts.takeLast(2).joinToString(".")
        }
    }

    fun extractHostFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            Uri.parse(url).host?.lowercase()?.removePrefix("www.")
        }.getOrNull()
    }

    fun normalizeHttpUrlForSecurity(url: String): String {
        return upgradeInsecureHttpUrl(url) ?: url
    }

    fun upgradeInsecureHttpUrl(url: String): String? {
        if (!url.startsWith("http://", ignoreCase = true)) return null
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return null
        if (host in WebViewManager.LOCAL_CLEARTEXT_HOSTS) return null
        return url.replaceFirst(Regex("(?i)^http://"), "https://")
    }
}
