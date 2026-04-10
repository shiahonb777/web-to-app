package com.webtoapp.core.adblock

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest

/**
 * AdBlock 过滤规则编译缓存
 *
 * 将已编译的 ABP/Hosts 过滤规则序列化到磁盘，避免每次启动重新解析。
 * 同时缓存下载的过滤列表原始内容，避免重复网络请求。
 *
 * 缓存结构：
 * - adblock_cache/          缓存根目录
 *   ├─ compiled_state.bin   二进制编译状态（所有已解析规则）
 *   ├─ content_hash.txt     内容哈希（用于检测规则是否变化）
 *   └─ url_cache/           URL 内容缓存
 *      ├─ {urlHash}.txt     下载的过滤列表原始内容
 *      └─ {urlHash}.meta    元数据（URL、时间戳）
 */
object AdBlockFilterCache {

    private const val TAG = "AdBlockFilterCache"
    private const val CACHE_DIR = "adblock_cache"
    private const val URL_CACHE_DIR = "url_cache"
    private const val COMPILED_STATE_FILE = "compiled_state.bin"
    private const val CONTENT_HASH_FILE = "content_hash.txt"
    private const val CACHE_VERSION = 1
    private const val URL_CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

    // ==================== URL Content Cache ====================

    /**
     * 获取 URL 内容的磁盘缓存（如果存在且未过期）
     */
    suspend fun getCachedUrlContent(context: Context, url: String): String? = withContext(Dispatchers.IO) {
        try {
            val urlHash = md5(url)
            val cacheDir = File(context.filesDir, "$CACHE_DIR/$URL_CACHE_DIR")
            val contentFile = File(cacheDir, "$urlHash.txt")
            val metaFile = File(cacheDir, "$urlHash.meta")

            if (!contentFile.exists() || !metaFile.exists()) return@withContext null

            // Check TTL
            val meta = metaFile.readText().split("\n")
            val timestamp = meta.getOrNull(1)?.toLongOrNull() ?: 0L
            if (System.currentTimeMillis() - timestamp > URL_CACHE_TTL_MS) {
                AppLogger.d(TAG, "URL cache expired for: $url")
                return@withContext null
            }

            AppLogger.d(TAG, "URL cache hit: $url (${contentFile.length() / 1024}KB)")
            contentFile.readText()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to read URL cache", e)
            null
        }
    }

    /**
     * 缓存 URL 内容到磁盘
     */
    suspend fun cacheUrlContent(context: Context, url: String, content: String) = withContext(Dispatchers.IO) {
        try {
            val urlHash = md5(url)
            val cacheDir = File(context.filesDir, "$CACHE_DIR/$URL_CACHE_DIR").also { it.mkdirs() }
            
            File(cacheDir, "$urlHash.txt").writeText(content)
            File(cacheDir, "$urlHash.meta").writeText("$url\n${System.currentTimeMillis()}")
            
            AppLogger.d(TAG, "Cached URL content: $url (${content.length / 1024}KB)")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to cache URL content", e)
        }
    }

    // ==================== Compiled State Cache ====================

    /**
     * 保存已编译的过滤规则状态到磁盘
     *
     * 序列化格式（DataOutputStream）：
     * - int: version
     * - String: contentHash
     * - int + Strings: exactHosts
     * - int + Strings: hostsFileHosts
     * - int + Strings: enabledSources
     * - int + NetworkFilters: networkBlockFilters
     * - int + NetworkFilters: networkExceptionFilters
     * - int + CosmeticFilters: cosmeticBlockFilters
     * - int + CosmeticFilters: cosmeticExceptionFilters
     * - int + ScriptletRules: scriptletRules
     */
    suspend fun saveCompiledState(
        context: Context,
        exactHosts: Set<String>,
        hostsFileHosts: Set<String>,
        enabledSources: Set<String>,
        networkBlockPatterns: List<SerializableNetworkFilter>,
        networkExceptionPatterns: List<SerializableNetworkFilter>,
        cosmeticBlockFilters: List<AdBlocker.CosmeticFilter>,
        cosmeticExceptionFilters: List<AdBlocker.CosmeticFilter>,
        scriptletRules: List<Pair<Set<String>, String>>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val cacheDir = File(context.filesDir, CACHE_DIR).also { it.mkdirs() }
            val stateFile = File(cacheDir, COMPILED_STATE_FILE)

            DataOutputStream(BufferedOutputStream(FileOutputStream(stateFile))).use { out ->
                // Version
                out.writeInt(CACHE_VERSION)

                // Content hash
                val hash = computeContentHash(exactHosts, hostsFileHosts)
                out.writeUTF(hash)

                // Exact hosts
                out.writeInt(exactHosts.size)
                exactHosts.forEach { out.writeUTF(it) }

                // Hosts file hosts
                out.writeInt(hostsFileHosts.size)
                hostsFileHosts.forEach { out.writeUTF(it) }

                // Enabled sources
                out.writeInt(enabledSources.size)
                enabledSources.forEach { out.writeUTF(it) }

                // Network block filters
                out.writeInt(networkBlockPatterns.size)
                networkBlockPatterns.forEach { writeNetworkFilter(out, it) }

                // Network exception filters
                out.writeInt(networkExceptionPatterns.size)
                networkExceptionPatterns.forEach { writeNetworkFilter(out, it) }

                // Cosmetic block filters
                out.writeInt(cosmeticBlockFilters.size)
                cosmeticBlockFilters.forEach { writeCosmeticFilter(out, it) }

                // Cosmetic exception filters
                out.writeInt(cosmeticExceptionFilters.size)
                cosmeticExceptionFilters.forEach { writeCosmeticFilter(out, it) }

                // Scriptlet rules
                out.writeInt(scriptletRules.size)
                scriptletRules.forEach { (domains, script) ->
                    out.writeInt(domains.size)
                    domains.forEach { out.writeUTF(it) }
                    out.writeUTF(script)
                }
            }

            // Save hash separately for quick validity check
            File(cacheDir, CONTENT_HASH_FILE).writeText(computeContentHash(exactHosts, hostsFileHosts))

            val elapsed = System.currentTimeMillis() - startTime
            val sizeKB = stateFile.length() / 1024
            AppLogger.i(TAG, "Saved compiled state: ${sizeKB}KB in ${elapsed}ms " +
                "(${exactHosts.size} exact, ${hostsFileHosts.size} hosts, " +
                "${networkBlockPatterns.size} block, ${networkExceptionPatterns.size} exception, " +
                "${cosmeticBlockFilters.size}+${cosmeticExceptionFilters.size} cosmetic, " +
                "${scriptletRules.size} scriptlets)")
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save compiled state", e)
            false
        }
    }

    /**
     * 加载已编译的过滤规则状态
     */
    suspend fun loadCompiledState(context: Context): CompiledState? = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            val stateFile = File(context.filesDir, "$CACHE_DIR/$COMPILED_STATE_FILE")
            if (!stateFile.exists()) {
                AppLogger.d(TAG, "No compiled state cache found")
                return@withContext null
            }

            DataInputStream(BufferedInputStream(FileInputStream(stateFile))).use { input ->
                // Version check
                val version = input.readInt()
                if (version != CACHE_VERSION) {
                    AppLogger.w(TAG, "Cache version mismatch: $version != $CACHE_VERSION, invalidating")
                    stateFile.delete()
                    return@withContext null
                }

                val contentHash = input.readUTF()

                // Exact hosts
                val exactHostsCount = input.readInt()
                val exactHosts = LinkedHashSet<String>(exactHostsCount)
                repeat(exactHostsCount) { exactHosts.add(input.readUTF()) }

                // Hosts file hosts
                val hostsCount = input.readInt()
                val hostsFileHosts = LinkedHashSet<String>(hostsCount)
                repeat(hostsCount) { hostsFileHosts.add(input.readUTF()) }

                // Enabled sources
                val sourcesCount = input.readInt()
                val enabledSources = LinkedHashSet<String>(sourcesCount)
                repeat(sourcesCount) { enabledSources.add(input.readUTF()) }

                // Network block filters
                val blockCount = input.readInt()
                val networkBlockFilters = ArrayList<SerializableNetworkFilter>(blockCount)
                repeat(blockCount) { networkBlockFilters.add(readNetworkFilter(input)) }

                // Network exception filters
                val exceptionCount = input.readInt()
                val networkExceptionFilters = ArrayList<SerializableNetworkFilter>(exceptionCount)
                repeat(exceptionCount) { networkExceptionFilters.add(readNetworkFilter(input)) }

                // Cosmetic block filters
                val cosBlockCount = input.readInt()
                val cosmeticBlockFilters = ArrayList<AdBlocker.CosmeticFilter>(cosBlockCount)
                repeat(cosBlockCount) { cosmeticBlockFilters.add(readCosmeticFilter(input)) }

                // Cosmetic exception filters
                val cosExCount = input.readInt()
                val cosmeticExceptionFilters = ArrayList<AdBlocker.CosmeticFilter>(cosExCount)
                repeat(cosExCount) { cosmeticExceptionFilters.add(readCosmeticFilter(input)) }

                // Scriptlet rules
                val scriptletCount = input.readInt()
                val scriptletRules = ArrayList<Pair<Set<String>, String>>(scriptletCount)
                repeat(scriptletCount) {
                    val domainCount = input.readInt()
                    val domains = LinkedHashSet<String>(domainCount)
                    repeat(domainCount) { domains.add(input.readUTF()) }
                    val script = input.readUTF()
                    scriptletRules.add(domains to script)
                }

                val elapsed = System.currentTimeMillis() - startTime
                AppLogger.i(TAG, "Loaded compiled state in ${elapsed}ms: " +
                    "${exactHosts.size} exact, ${hostsFileHosts.size} hosts, " +
                    "${networkBlockFilters.size} block, ${networkExceptionFilters.size} exception")

                CompiledState(
                    contentHash = contentHash,
                    exactHosts = exactHosts,
                    hostsFileHosts = hostsFileHosts,
                    enabledSources = enabledSources,
                    networkBlockFilters = networkBlockFilters,
                    networkExceptionFilters = networkExceptionFilters,
                    cosmeticBlockFilters = cosmeticBlockFilters,
                    cosmeticExceptionFilters = cosmeticExceptionFilters,
                    scriptletRules = scriptletRules
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to load compiled state (will re-parse)", e)
            // Invalidate corrupted cache
            File(context.filesDir, "$CACHE_DIR/$COMPILED_STATE_FILE").delete()
            null
        }
    }

    /**
     * 清除所有缓存
     */
    fun clearCache(context: Context) {
        try {
            File(context.filesDir, CACHE_DIR).deleteRecursively()
            AppLogger.i(TAG, "Cache cleared")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to clear cache", e)
        }
    }

    /**
     * 获取缓存大小（字节）
     */
    fun getCacheSize(context: Context): Long {
        val cacheDir = File(context.filesDir, CACHE_DIR)
        if (!cacheDir.exists()) return 0
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    // ==================== Serialization Helpers ====================

    private fun writeNetworkFilter(out: DataOutputStream, filter: SerializableNetworkFilter) {
        out.writeUTF(filter.pattern)
        out.writeBoolean(filter.isException)
        out.writeBoolean(filter.matchCase)
        writeStringSet(out, filter.domains)
        writeStringSet(out, filter.excludedDomains)
        // allowedTypes: nullable
        out.writeBoolean(filter.allowedTypeNames != null)
        if (filter.allowedTypeNames != null) writeStringSet(out, filter.allowedTypeNames)
        writeStringSet(out, filter.excludedTypeNames)
        out.writeBoolean(filter.thirdPartyOnly)
        out.writeBoolean(filter.firstPartyOnly)
        out.writeBoolean(filter.anchorDomain != null)
        if (filter.anchorDomain != null) out.writeUTF(filter.anchorDomain)
    }

    private fun readNetworkFilter(input: DataInputStream): SerializableNetworkFilter {
        val pattern = input.readUTF()
        val isException = input.readBoolean()
        val matchCase = input.readBoolean()
        val domains = readStringSet(input)
        val excludedDomains = readStringSet(input)
        val hasAllowedTypes = input.readBoolean()
        val allowedTypeNames = if (hasAllowedTypes) readStringSet(input) else null
        val excludedTypeNames = readStringSet(input)
        val thirdPartyOnly = input.readBoolean()
        val firstPartyOnly = input.readBoolean()
        val hasAnchorDomain = input.readBoolean()
        val anchorDomain = if (hasAnchorDomain) input.readUTF() else null

        return SerializableNetworkFilter(
            pattern = pattern,
            isException = isException,
            matchCase = matchCase,
            domains = domains,
            excludedDomains = excludedDomains,
            allowedTypeNames = allowedTypeNames,
            excludedTypeNames = excludedTypeNames,
            thirdPartyOnly = thirdPartyOnly,
            firstPartyOnly = firstPartyOnly,
            anchorDomain = anchorDomain
        )
    }

    private fun writeCosmeticFilter(out: DataOutputStream, filter: AdBlocker.CosmeticFilter) {
        out.writeUTF(filter.selector)
        out.writeBoolean(filter.isException)
        writeStringSet(out, filter.domains)
        writeStringSet(out, filter.excludedDomains)
    }

    private fun readCosmeticFilter(input: DataInputStream): AdBlocker.CosmeticFilter {
        return AdBlocker.CosmeticFilter(
            selector = input.readUTF(),
            isException = input.readBoolean(),
            domains = readStringSet(input),
            excludedDomains = readStringSet(input)
        )
    }

    private fun writeStringSet(out: DataOutputStream, set: Set<String>) {
        out.writeInt(set.size)
        set.forEach { out.writeUTF(it) }
    }

    private fun readStringSet(input: DataInputStream): Set<String> {
        val count = input.readInt()
        val set = LinkedHashSet<String>(count)
        repeat(count) { set.add(input.readUTF()) }
        return set
    }

    private fun computeContentHash(exactHosts: Set<String>, hostsFileHosts: Set<String>): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update("exact:${exactHosts.size}".toByteArray())
        digest.update("hosts:${hostsFileHosts.size}".toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    // ==================== Data Classes ====================

    /**
     * 可序列化的网络过滤器（不含 Regex 对象，加载后重新编译）
     */
    data class SerializableNetworkFilter(
        val pattern: String,
        val isException: Boolean,
        val matchCase: Boolean,
        val domains: Set<String>,
        val excludedDomains: Set<String>,
        val allowedTypeNames: Set<String>?,
        val excludedTypeNames: Set<String>,
        val thirdPartyOnly: Boolean,
        val firstPartyOnly: Boolean,
        val anchorDomain: String?
    )

    /**
     * 已编译的完整过滤状态
     */
    data class CompiledState(
        val contentHash: String,
        val exactHosts: Set<String>,
        val hostsFileHosts: Set<String>,
        val enabledSources: Set<String>,
        val networkBlockFilters: List<SerializableNetworkFilter>,
        val networkExceptionFilters: List<SerializableNetworkFilter>,
        val cosmeticBlockFilters: List<AdBlocker.CosmeticFilter>,
        val cosmeticExceptionFilters: List<AdBlocker.CosmeticFilter>,
        val scriptletRules: List<Pair<Set<String>, String>>
    )
}
