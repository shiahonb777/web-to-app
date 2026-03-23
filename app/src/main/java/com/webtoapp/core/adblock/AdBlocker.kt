package com.webtoapp.core.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ad Blocker - Block web ads based on URL rules and hosts files
 */
class AdBlocker {

    companion object {
        // Translation API whitelist - these domains will not be blocked
        private val WHITELIST_HOSTS = listOf(
            "translate.googleapis.com",
            "translate.google.com",
            "translation.googleapis.com"
        )
        
        // Popular hosts file sources
        fun getPopularHostsSources() = listOf(
            HostsSource(
                name = "AdGuard DNS Filter",
                url = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAdGuardDesc
            ),
            HostsSource(
                name = "StevenBlack Hosts",
                url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                description = com.webtoapp.core.i18n.Strings.hostsStevenBlackDesc
            ),
            HostsSource(
                name = "AdAway Default",
                url = "https://adaway.org/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAdAwayDesc
            ),
            HostsSource(
                name = "Anti-AD",
                url = "https://anti-ad.net/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAntiADDesc
            ),
            HostsSource(
                name = "1Hosts Lite",
                url = "https://o0.pages.dev/Lite/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hosts1HostsLiteDesc
            )
        )
        
        // Maintain backward compatibility
        @Deprecated("Use getPopularHostsSources() instead for i18n support")
        val POPULAR_HOSTS_SOURCES get() = getPopularHostsSources()
        
        // Default ad blocking rules (common ad domains)
        private val DEFAULT_AD_HOSTS = listOf(
            // Google Ads
            "googleadservices.com",
            "googlesyndication.com",
            "doubleclick.net",
            "google-analytics.com",
            "googletagmanager.com",
            "googletagservices.com",

            // Facebook Ads
            "facebook.net",
            "fbcdn.net",

            // Other common ad networks
            "adservice",
            "adsserver",
            "adnxs.com",
            "advertising.com",
            "taboola.com",
            "outbrain.com",
            "criteo.com",
            "pubmatic.com",
            "rubiconproject.com",
            "moatads.com",

            // Common Chinese ads
            "cpro.baidu.com",
            "pos.baidu.com",
            "cbjs.baidu.com",
            "eclick.baidu.com",
            "hm.baidu.com",
            "tanx.com",
            "alimama.com",
            "mmstat.com",
            "cnzz.com",
            "51.la",
            "union.sogou.com",
            "js.sogou.com"
        )

        private val DEFAULT_AD_PATTERNS = listOf(
            "*/ads/*",
            "*/ad/*",
            "*/advert/*",
            "*/banner/*",
            "*_ad.*",
            "*_ads.*",
            "*-ad.*",
            "*-ads.*",
            "*.ad.*",
            "*admob*",
            "*adsense*",
            "*adserver*",
            "*tracking*",
            "*analytics*"
        )
    }

    private val blockedHosts = mutableSetOf<String>()
    private val blockedPatterns = mutableSetOf<String>()
    
    // Domains imported from hosts files (stored separately for management)
    private val hostsFileHosts = mutableSetOf<String>()
    
    // Cache compiled regex, avoid repeated compilation
    private val compiledPatterns = mutableMapOf<String, Regex?>()
    
    // Enabled hosts sources
    private val enabledHostsSources = mutableSetOf<String>()
    
    private var enabled = false

    /**
     * Initialize blocker
     */
    fun initialize(customRules: List<String> = emptyList(), useDefaultRules: Boolean = true) {
        blockedHosts.clear()
        blockedPatterns.clear()
        compiledPatterns.clear()  // Clear cached regex

        if (useDefaultRules) {
            blockedHosts.addAll(DEFAULT_AD_HOSTS)
            blockedPatterns.addAll(DEFAULT_AD_PATTERNS)
        }

        // Parse custom rules
        customRules.forEach { rule ->
            when {
                rule.startsWith("||") -> {
                    // Domain rule ||example.com
                    blockedHosts.add(rule.removePrefix("||").removeSuffix("^"))
                }
                rule.contains("*") -> {
                    // Wildcard rule
                    blockedPatterns.add(rule)
                }
                else -> {
                    // Plain domain
                    blockedHosts.add(rule)
                }
            }
        }
        
        // Pre-compile all regex patterns
        precompilePatterns()
    }
    
    /**
     * Pre-compile regex patterns
     */
    private fun precompilePatterns() {
        blockedPatterns.forEach { pattern ->
            if (!compiledPatterns.containsKey(pattern)) {
                compiledPatterns[pattern] = compilePattern(pattern)
            }
        }
    }
    
    /**
     * Compile single pattern to regex
     */
    private fun compilePattern(pattern: String): Regex? {
        return try {
            val regexPattern = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".?")
            Regex(regexPattern, RegexOption.IGNORE_CASE)
        } catch (e: Exception) {
            null
        }
    }

    fun setEnabled(enable: Boolean) {
        enabled = enable
    }

    fun isEnabled(): Boolean = enabled

    /**
     * Check if URL should be blocked
     */
    fun shouldBlock(url: String): Boolean {
        if (!enabled) return false

        val lowerUrl = url.lowercase()

        // Whitelist check - don't block translation APIs etc.
        WHITELIST_HOSTS.forEach { host ->
            if (lowerUrl.contains(host)) {
                return false
            }
        }
        
        // Extract domain from URL for exact matching
        val urlHost = extractHost(lowerUrl)

        // Check domains (original rules)
        blockedHosts.forEach { host ->
            if (lowerUrl.contains(host)) {
                return true
            }
        }
        
        // Check hosts file imported domains (exact match)
        if (urlHost != null && hostsFileHosts.contains(urlHost)) {
            return true
        }
        // Also check subdomains
        if (urlHost != null) {
            hostsFileHosts.forEach { blockedHost ->
                if (urlHost.endsWith(".$blockedHost")) {
                    return true
                }
            }
        }

        // Check URL patterns
        blockedPatterns.forEach { pattern ->
            if (matchPattern(lowerUrl, pattern)) {
                return true
            }
        }

        return false
    }
    
    /**
     * Extract domain from URL
     */
    private fun extractHost(url: String): String? {
        return try {
            val uri = Uri.parse(url)
            uri.host?.lowercase()
        } catch (e: Exception) {
            // Fallback method
            val regex = Regex("^(?:https?://)?([^/]+)")
            regex.find(url)?.groupValues?.getOrNull(1)?.lowercase()
        }
    }

    /**
     * Check if WebResourceRequest should be blocked
     */
    fun shouldBlock(request: WebResourceRequest): Boolean {
        return shouldBlock(request.url.toString())
    }

    /**
     * Return empty response (used to block ad requests)
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * Simple wildcard pattern matching (using cached regex)
     */
    private fun matchPattern(url: String, pattern: String): Boolean {
        // Use cached regex, compile and cache if not exists
        val regex = compiledPatterns.getOrPut(pattern) { compilePattern(pattern) }
        return regex?.containsMatchIn(url) == true
    }

    /**
     * Get current rule count
     */
    fun getRuleCount(): Int = blockedHosts.size + blockedPatterns.size + hostsFileHosts.size
    
    /**
     * Get hosts file rule count
     */
    fun getHostsFileRuleCount(): Int = hostsFileHosts.size

    /**
     * Add custom rule
     */
    fun addRule(rule: String) {
        when {
            rule.startsWith("||") -> blockedHosts.add(rule.removePrefix("||").removeSuffix("^"))
            rule.contains("*") -> blockedPatterns.add(rule)
            else -> blockedHosts.add(rule)
        }
    }

    /**
     * Remove rule
     */
    fun removeRule(rule: String) {
        blockedHosts.remove(rule)
        blockedPatterns.remove(rule)
    }

    /**
     * Clear all rules
     */
    fun clearRules() {
        blockedHosts.clear()
        blockedPatterns.clear()
        compiledPatterns.clear()
    }
    
    /**
     * Clear hosts file rules
     */
    fun clearHostsFileRules() {
        hostsFileHosts.clear()
        enabledHostsSources.clear()
    }
    
    /**
     * Clear regex pattern cache (call when low memory)
     */
    fun clearPatternCache() {
        compiledPatterns.clear()
    }
    
    /**
     * Get blocking statistics (for debugging)
     */
    fun getStats(): Map<String, Int> {
        return mapOf(
            "hosts" to blockedHosts.size,
            "hostsFile" to hostsFileHosts.size,
            "patterns" to blockedPatterns.size,
            "compiledPatterns" to compiledPatterns.size
        )
    }
    
    // ==================== Hosts File Support ====================
    
    /**
     * Import hosts from local file
     * @param context Context
     * @param uri File URI
     * @return Number of imported rules
     */
    suspend fun importHostsFromFile(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))
            
            val count = parseHostsContent(inputStream.bufferedReader().readText())
            inputStream.close()
            
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Download and import hosts from URL
     * @param url hosts file URL
     * @return Number of imported rules
     */
    suspend fun importHostsFromUrl(url: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "WebToApp/1.0")
            
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.failure(
                    Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
                )
            }
            
            val content = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            
            val count = parseHostsContent(content)
            enabledHostsSources.add(url)
            
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Parse hosts file content
     * Supports standard hosts format and AdBlock/AdGuard format
     */
    private fun parseHostsContent(content: String): Int {
        var count = 0
        
        content.lineSequence().forEach { line ->
            val trimmedLine = line.trim()
            
            // Skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#") || trimmedLine.startsWith("!")) {
                return@forEach
            }
            
            val host = parseHostLine(trimmedLine)
            if (host != null && isValidHost(host)) {
                hostsFileHosts.add(host.lowercase())
                count++
            }
        }
        
        return count
    }
    
    /**
     * Parse single hosts record
     * Supports multiple formats:
     * - Standard hosts: "0.0.0.0 ads.example.com" or "127.0.0.1 ads.example.com"
     * - AdBlock: "||ads.example.com^"
     * - Pure domain: "ads.example.com"
     */
    private fun parseHostLine(line: String): String? {
        // AdBlock format: ||domain^
        if (line.startsWith("||")) {
            return line.removePrefix("||").removeSuffix("^").removeSuffix("$").trim()
        }
        
        // Standard hosts format: IP domain [domain2 ...]
        val parts = line.split(Regex("\\s+"))
        if (parts.size >= 2) {
            val firstPart = parts[0]
            // Check if first part is IP address
            if (firstPart == "0.0.0.0" || firstPart == "127.0.0.1" || 
                firstPart.startsWith("0.") || firstPart.startsWith("127.") ||
                firstPart == "::" || firstPart == "::1") {
                // Return second part as domain (ignore trailing comment)
                val domain = parts[1].split("#")[0].trim()
                if (domain.isNotEmpty() && domain != "localhost" && domain != "localhost.localdomain") {
                    return domain
                }
            }
        }
        
        // Pure domain format
        if (parts.size == 1 && parts[0].contains(".") && !parts[0].contains("/")) {
            return parts[0]
        }
        
        return null
    }
    
    /**
     * Validate domain format
     */
    private fun isValidHost(host: String): Boolean {
        if (host.isEmpty() || host.length > 253) return false
        if (host.startsWith(".") || host.endsWith(".")) return false
        if (host.contains("..")) return false
        
        // Exclude IP addresses
        if (host.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))) return false
        
        // Exclude some invalid domains
        if (host == "localhost" || host == "broadcasthost" || host == "local") return false
        
        return true
    }
    
    /**
     * Get enabled hosts sources
     */
    fun getEnabledHostsSources(): Set<String> = enabledHostsSources.toSet()
    
    /**
     * Check if a hosts source is enabled
     */
    fun isHostsSourceEnabled(url: String): Boolean = enabledHostsSources.contains(url)
    
    /**
     * Save hosts rules to file
     */
    suspend fun saveHostsRules(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "adblock_hosts.txt")
            file.writeText(hostsFileHosts.joinToString("\n"))
            
            // Save enabled sources
            val sourcesFile = File(context.filesDir, "adblock_hosts_sources.txt")
            sourcesFile.writeText(enabledHostsSources.joinToString("\n"))
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load hosts rules from file
     */
    suspend fun loadHostsRules(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, "adblock_hosts.txt")
            if (file.exists()) {
                val hosts = file.readLines().filter { it.isNotBlank() }
                hostsFileHosts.addAll(hosts)
            }
            
            // Load enabled sources
            val sourcesFile = File(context.filesDir, "adblock_hosts_sources.txt")
            if (sourcesFile.exists()) {
                val sources = sourcesFile.readLines().filter { it.isNotBlank() }
                enabledHostsSources.addAll(sources)
            }
            
            Result.success(hostsFileHosts.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Hosts source info
 */
data class HostsSource(
    val name: String,
    val url: String,
    val description: String
)
