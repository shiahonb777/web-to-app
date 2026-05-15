package com.webtoapp.core.webview

import android.content.Context
import android.webkit.WebView
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
















class PacProxyManager(private val context: Context) {

    companion object {
        private const val TAG = "PacProxyManager"




        fun isProxyOverrideSupported(): Boolean {
            return try {
                WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to check PROXY_OVERRIDE support: ${e.message}")
                false
            }
        }
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()


    @Volatile
    private var cachedPacScript: String? = null
    @Volatile
    private var cachedPacUrl: String? = null


    @Volatile
    private var currentProxyUsername: String = ""
    @Volatile
    private var currentProxyPassword: String = ""













    suspend fun applyProxy(
        proxyMode: String,
        staticProxyHost: String = "",
        staticProxyPort: Int = 0,
        staticProxyType: String = "HTTP",
        pacUrl: String = "",
        bypassRules: List<String> = emptyList(),
        username: String = "",
        password: String = ""
    ) {
        if (!isProxyOverrideSupported()) {
            AppLogger.w(TAG, "PROXY_OVERRIDE not supported on this WebView version")
            return
        }


        currentProxyUsername = username
        currentProxyPassword = password


        if (username.isNotBlank() && password.isNotBlank()) {
            setupProxyAuthenticator(username, password)
        } else {
            clearProxyAuthenticator()
        }

        when (proxyMode.uppercase()) {
            "NONE" -> clearProxy()
            "STATIC" -> applyStaticProxy(staticProxyHost, staticProxyPort, staticProxyType, bypassRules, username, password)
            "PAC" -> applyPacProxy(pacUrl, bypassRules)
            else -> {
                AppLogger.w(TAG, "Unknown proxy mode: $proxyMode, clearing proxy")
                clearProxy()
            }
        }
    }




    fun clearProxy() {
        if (!isProxyOverrideSupported()) return

        try {
            ProxyController.getInstance().clearProxyOverride(
                { command -> command.run() },
                { AppLogger.d(TAG, "Proxy cleared successfully") }
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear proxy", e)
        }


        currentProxyUsername = ""
        currentProxyPassword = ""
        clearProxyAuthenticator()


        LocalHttpToSocksBridge.stop()
        LocalHttpHostMappingBridge.stop()
    }





    private fun setupProxyAuthenticator(username: String, password: String) {
        try {
            java.net.Authenticator.setDefault(object : java.net.Authenticator() {
                override fun getPasswordAuthentication(): java.net.PasswordAuthentication? {
                    if (requestorType == RequestorType.PROXY) {
                        return java.net.PasswordAuthentication(username, password.toCharArray())
                    }
                    return null
                }
            })
            AppLogger.d(TAG, "Proxy authenticator set for username=$username")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to set proxy authenticator", e)
        }
    }




    private fun clearProxyAuthenticator() {
        try {
            java.net.Authenticator.setDefault(null)
        } catch (_: Exception) {

        }
    }




    private fun applyStaticProxy(
        host: String,
        port: Int,
        type: String,
        bypassRules: List<String>,
        username: String = "",
        password: String = ""
    ) {
        if (host.isBlank() || port <= 0) {
            AppLogger.w(TAG, "Invalid static proxy config: host=$host, port=$port")
            return
        }

        try {
            val upperType = type.uppercase()
            val isSocks = upperType == "SOCKS5" || upperType == "SOCKS"


            val proxyRule = if (isSocks) {
                val bridgePort = LocalHttpToSocksBridge.start(
                    LocalHttpToSocksBridge.Upstream(host, port, username, password)
                )
                if (bridgePort <= 0) {
                    AppLogger.e(TAG, "Local SOCKS5 bridge failed to start; aborting proxy apply")
                    return
                }
                AppLogger.i(
                    TAG,
                    "WebView SOCKS5 routed via local bridge 127.0.0.1:$bridgePort -> $host:$port"
                )
                "127.0.0.1:$bridgePort"
            } else {

                LocalHttpToSocksBridge.stop()
                when (upperType) {
                    "HTTPS" -> "https://$host:$port"
                    else -> "$host:$port"
                }
            }

            val builder = ProxyConfig.Builder()
                .addProxyRule(proxyRule)


            builder.addBypassRule("localhost")
            builder.addBypassRule("127.0.0.1")
            builder.addBypassRule("[::1]")
            builder.addBypassRule("10.0.2.2")


            bypassRules.filter { it.isNotBlank() }.forEach { rule ->
                builder.addBypassRule(rule)
            }

            ProxyController.getInstance().setProxyOverride(
                builder.build(),
                { command -> command.run() },
                { AppLogger.d(TAG, "Static proxy applied: $proxyRule") }
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to apply static proxy", e)
        }
    }
















    private suspend fun applyPacProxy(pacUrl: String, bypassRules: List<String>) {
        if (pacUrl.isBlank()) {
            AppLogger.w(TAG, "Empty PAC URL")
            return
        }

        try {

            val pacScript = downloadPacScript(pacUrl)
            if (pacScript.isNullOrBlank()) {
                AppLogger.e(TAG, "Failed to download PAC script from: $pacUrl")
                return
            }


            cachedPacScript = pacScript
            cachedPacUrl = pacUrl


            val proxyEntries = parsePacScript(pacScript)

            if (proxyEntries.isEmpty()) {
                AppLogger.w(TAG, "No proxy entries found in PAC script, using DIRECT")
                clearProxy()
                return
            }


            val builder = ProxyConfig.Builder()

            proxyEntries.forEach { entry ->
                when {
                    entry.startsWith("DIRECT", ignoreCase = true) -> {
                        builder.addDirect()
                    }
                    entry.startsWith("SOCKS5 ", ignoreCase = true) ||
                    entry.startsWith("SOCKS ", ignoreCase = true) -> {
                        val server = entry.substringAfter(" ").trim()
                        if (server.isNotBlank()) {
                            builder.addProxyRule("socks5://$server")
                        }
                    }
                    entry.startsWith("PROXY ", ignoreCase = true) -> {
                        val server = entry.substringAfter(" ").trim()
                        if (server.isNotBlank()) {
                            builder.addProxyRule(server)
                        }
                    }
                    entry.startsWith("HTTPS ", ignoreCase = true) -> {
                        val server = entry.substringAfter(" ").trim()
                        if (server.isNotBlank()) {
                            builder.addProxyRule("https://$server")
                        }
                    }
                    else -> {

                        if (entry.isNotBlank()) {
                            builder.addProxyRule(entry)
                        }
                    }
                }
            }


            builder.addBypassRule("localhost")
            builder.addBypassRule("127.0.0.1")
            builder.addBypassRule("[::1]")
            builder.addBypassRule("10.0.2.2")


            bypassRules.filter { it.isNotBlank() }.forEach { rule ->
                builder.addBypassRule(rule)
            }

            ProxyController.getInstance().setProxyOverride(
                builder.build(),
                { command -> command.run() },
                {
                    AppLogger.d(TAG, "PAC proxy applied from $pacUrl, " +
                            "${proxyEntries.size} proxy entries extracted")
                }
            )
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to apply PAC proxy", e)
        }
    }




    private suspend fun downloadPacScript(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/x-ns-proxy-autoconfig, application/javascript, text/javascript, */*")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    AppLogger.d(TAG, "PAC script downloaded: ${body?.length ?: 0} chars")
                    body
                } else {
                    AppLogger.e(TAG, "PAC download failed: ${response.code} ${response.message}")
                    null
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "PAC download error: ${e.message}")
                null
            }
        }
    }












    private fun parsePacScript(script: String): List<String> {
        val entries = mutableListOf<String>()
        val seen = mutableSetOf<String>()






        val returnPattern = Regex(
            """(?:return\s+['"])([^'"]+)['"]""",
            RegexOption.IGNORE_CASE
        )

        returnPattern.findAll(script).forEach { match ->
            val returnValue = match.groupValues[1].trim()


            returnValue.split(";").forEach { entry ->
                val trimmed = entry.trim()
                if (trimmed.isNotBlank() && trimmed !in seen) {
                    seen.add(trimmed)
                    entries.add(trimmed)
                }
            }
        }


        if (entries.isEmpty()) {

            val proxyPattern = Regex(
                """(PROXY|SOCKS5?|HTTPS|DIRECT)\s+[\w.\-]+:\d+""",
                RegexOption.IGNORE_CASE
            )
            proxyPattern.findAll(script).forEach { match ->
                val trimmed = match.value.trim()
                if (trimmed !in seen) {
                    seen.add(trimmed)
                    entries.add(trimmed)
                }
            }

            if (script.contains("DIRECT", ignoreCase = true) && "DIRECT" !in seen) {
                entries.add("DIRECT")
            }
        }

        AppLogger.d(TAG, "Parsed ${entries.size} proxy entries from PAC: $entries")
        return entries
    }




    fun getCachedPacUrl(): String? = cachedPacUrl




    suspend fun refreshPac(bypassRules: List<String> = emptyList()) {
        val url = cachedPacUrl ?: return
        applyPacProxy(url, bypassRules)
    }
}
