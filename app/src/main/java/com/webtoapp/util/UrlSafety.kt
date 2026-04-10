package com.webtoapp.util

import android.net.Uri

val LOCAL_HTTP_HOSTS: Set<String> = setOf("localhost", "127.0.0.1", "10.0.2.2")

val BLOCKED_EXTERNAL_URL_SCHEMES: Set<String> = setOf("javascript", "data", "file", "content", "about")

private val HTTP_SCHEME_REGEX = Regex("(?i)^http://")

fun getUrlScheme(rawUrl: String): String? {
    return runCatching { Uri.parse(rawUrl).scheme?.lowercase() }.getOrNull()
}

fun isAllowedUrlScheme(rawUrl: String, allowedSchemes: Set<String>): Boolean {
    val scheme = getUrlScheme(rawUrl) ?: return false
    return scheme in allowedSchemes
}

fun isInsecureRemoteHttpUrl(rawUrl: String, localHttpHosts: Set<String> = LOCAL_HTTP_HOSTS): Boolean {
    val trimmed = rawUrl.trim()
    if (!trimmed.startsWith("http://", ignoreCase = true)) return false
    val host = runCatching { Uri.parse(trimmed).host?.lowercase() }.getOrNull() ?: return true
    return host !in localHttpHosts
}

fun upgradeRemoteHttpToHttps(rawUrl: String, localHttpHosts: Set<String> = LOCAL_HTTP_HOSTS): String {
    val trimmed = rawUrl.trim()
    if (!trimmed.startsWith("http://", ignoreCase = true)) return trimmed
    val host = runCatching { Uri.parse(trimmed).host?.lowercase() }.getOrNull() ?: return trimmed
    if (host in localHttpHosts) return trimmed
    return trimmed.replaceFirst(HTTP_SCHEME_REGEX, "https://")
}

fun ensureWebUrlScheme(rawUrl: String, defaultScheme: String = "https"): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""

    val hasScheme = runCatching { Uri.parse(trimmed).scheme?.isNotBlank() == true }.getOrDefault(false)
    if (hasScheme) return trimmed

    val httpsCandidate = "$defaultScheme://$trimmed"
    val host = runCatching { Uri.parse(httpsCandidate).host?.trim() }.getOrNull()
    if (host.isNullOrBlank()) return trimmed

    val normalizedHost = host.lowercase()
    val scheme = if (normalizedHost in LOCAL_HTTP_HOSTS) "http" else defaultScheme
    return "$scheme://$trimmed"
}

fun normalizeExternalIntentUrl(
    rawUrl: String,
    defaultScheme: String = "https",
    localHttpHosts: Set<String> = LOCAL_HTTP_HOSTS,
    blockedSchemes: Set<String> = BLOCKED_EXTERNAL_URL_SCHEMES
): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""

    val hasScheme = runCatching { Uri.parse(trimmed).scheme?.isNotBlank() == true }.getOrDefault(false)
    val withScheme = if (hasScheme) trimmed else "$defaultScheme://$trimmed"
    val scheme = getUrlScheme(withScheme) ?: return ""
    if (scheme in blockedSchemes) return ""

    return if (scheme == "http") upgradeRemoteHttpToHttps(withScheme, localHttpHosts) else withScheme
}
