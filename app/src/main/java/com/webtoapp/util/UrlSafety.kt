package com.webtoapp.util

import android.net.Uri

val LOCAL_HTTP_HOSTS: Set<String> = setOf("localhost", "127.0.0.1", "10.0.2.2")

val BLOCKED_EXTERNAL_URL_SCHEMES: Set<String> = setOf("javascript", "data", "file", "content", "about")

private val HTTP_SCHEME_REGEX = Regex("(?i)^http://")

/**
 * 判断 host 是否为纯 IP 地址（IPv4）。
 * 纯 IP 地址的服务器通常没有 SSL 证书，不应强制升级 HTTPS。
 */
private fun isIpAddress(host: String): Boolean {
    val parts = host.split('.')
    if (parts.size != 4) return false
    return parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }
}

/**
 * 判断 host 是否为私有网络地址（局域网 IP）。
 */
private fun isPrivateNetworkHost(host: String): Boolean {
    if (host in LOCAL_HTTP_HOSTS) return true
    if (host == "::1" || host == "[::1]") return true
    if (host.endsWith(".local")) return true
    val parts = host.split('.')
    if (parts.size != 4) return false
    val octets = parts.map { it.toIntOrNull() ?: return false }
    if (octets.any { it !in 0..255 }) return false
    return when {
        octets[0] == 10 -> true
        octets[0] == 172 && octets[1] in 16..31 -> true
        octets[0] == 192 && octets[1] == 168 -> true
        octets[0] == 127 -> true
        else -> false
    }
}
fun getUrlScheme(rawUrl: String): String? {
    return runCatching { Uri.parse(rawUrl).scheme?.lowercase() }.getOrNull()
}

fun isAllowedUrlScheme(rawUrl: String, allowedSchemes: Set<String>): Boolean {
    val scheme = getUrlScheme(rawUrl) ?: return false
    return scheme in allowedSchemes
}

fun isInsecureRemoteHttpUrl(rawUrl: String, localHttpHosts: Set<String> = LOCAL_HTTP_HOSTS): Boolean {
    val trimmed = rawUrl.trim()
    if (!HTTP_SCHEME_REGEX.containsMatchIn(trimmed)) return false
    val host = runCatching { Uri.parse(trimmed).host?.lowercase() }.getOrNull() ?: return false
    if (host in localHttpHosts) return false
    if (isPrivateNetworkHost(host)) return false
    // 纯 IP（公网 IP）通常没 SSL 证书，强升 https 反而打不开，标记为可继续，但调用方
    // 会决定是否给用户一个开关；这里仅做"是否需要警告"判定。
    if (isIpAddress(host)) return false
    return true
}

fun upgradeRemoteHttpToHttps(rawUrl: String, localHttpHosts: Set<String> = LOCAL_HTTP_HOSTS): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return trimmed
    if (!HTTP_SCHEME_REGEX.containsMatchIn(trimmed)) return trimmed

    val host = runCatching { Uri.parse(trimmed).host?.lowercase() }.getOrNull() ?: return trimmed
    // 私网 / 本机回环 / 纯 IP 一律保留 http，否则会因为没证书直接失败。
    if (host in localHttpHosts) return trimmed
    if (isPrivateNetworkHost(host)) return trimmed
    if (isIpAddress(host)) return trimmed

    return HTTP_SCHEME_REGEX.replaceFirst(trimmed, "https://")
}

fun ensureWebUrlScheme(rawUrl: String, defaultScheme: String = "https"): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""

    val hasScheme = runCatching { Uri.parse(trimmed).scheme?.isNotBlank() == true }.getOrDefault(false)
    if (hasScheme) return trimmed

    // 没有协议头时统一用 http，不做任何假设。
    // 用户想用 https 可以自己写 https://
    return "http://$trimmed"
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
    val withScheme = if (hasScheme) trimmed else "http://$trimmed"
    val scheme = getUrlScheme(withScheme) ?: return ""
    if (scheme in blockedSchemes) return ""

    return withScheme
}
