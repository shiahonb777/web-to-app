package com.webtoapp.core.network

import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis


/**
 * Probes CN GitHub accelerator proxies and keeps them ordered by observed latency.
 *
 * Why: the hardcoded order in GITHUB_CN_PROXIES can go stale — a proxy that used
 * to be fastest may time out today. On first use we fire a background probe and
 * subsequent calls return the freshly-ordered list; probes are cached for 10
 * minutes and broken proxies are dropped from the rotation.
 *
 * Scope: only the CN (Chinese) mirror path uses this — international/Arabic
 * users hit origin servers directly and bypass this object entirely.
 */
object CnMirrorProbe {

    private const val TAG = "CnMirrorProbe"
    private const val CACHE_TTL_MS = 10L * 60 * 1000

    // A tiny, always-present GitHub file we can HEAD through every proxy.
    private const val PROBE_SUFFIX = "https://raw.githubusercontent.com/github/gitignore/main/README.md"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    @Volatile
    private var cachedOrder: List<String> = emptyList()

    @Volatile
    private var cachedAt: Long = 0L

    @Volatile
    private var probing: Boolean = false

    private val probeClient by lazy {
        NetworkModule.customClient {
            connectTimeout(4, TimeUnit.SECONDS)
            readTimeout(4, TimeUnit.SECONDS)
            writeTimeout(4, TimeUnit.SECONDS)
            retryOnConnectionFailure(false)
        }
    }

    /**
     * Returns proxies in best-to-worst order. If no probe has been run yet
     * or the cache is stale, kicks off an async probe and returns the given
     * baseline order so the first caller isn't blocked.
     */
    fun getOrderedProxies(baseList: List<String>): List<String> {
        val now = System.currentTimeMillis()
        val fresh = cachedOrder.isNotEmpty() && (now - cachedAt) < CACHE_TTL_MS
        if (fresh) {
            // Preserve any proxies added to baseList since last probe at the tail.
            val extras = baseList.filter { it !in cachedOrder }
            return cachedOrder + extras
        }
        if (!probing) {
            scope.launch { probe(baseList) }
        }
        return baseList
    }

    /**
     * Performs a parallel HEAD across all proxies. Proxies that fail (timeout,
     * non-2xx, connection error) are dropped. Successful proxies are sorted
     * ascending by round-trip time. Result is cached.
     */
    suspend fun probe(baseList: List<String>) {
        mutex.withLock {
            if (probing) return@withLock
            probing = true
        }
        try {
            val results = withContext(Dispatchers.IO) {
                baseList.map { proxy ->
                    async { proxy to measureProxy(proxy) }
                }.awaitAll()
            }
            val ordered = results
                .filter { it.second > 0 }
                .sortedBy { it.second }
                .map { it.first }
            if (ordered.isNotEmpty()) {
                cachedOrder = ordered
                cachedAt = System.currentTimeMillis()
                AppLogger.i(TAG, "Probed proxies: $ordered (rtt ms: ${results.filter { it.second > 0 }.joinToString { "${it.first.substringAfter("//").substringBefore("/")}=${it.second}" }})")
            } else {
                AppLogger.w(TAG, "All proxies failed probe; keeping previous order")
            }
        } finally {
            probing = false
        }
    }

    /**
     * Returns round-trip time in ms, or -1 on failure.
     */
    private fun measureProxy(proxy: String): Long {
        val url = "$proxy$PROBE_SUFFIX"
        return try {
            var code = 0
            val elapsed = measureTimeMillis {
                val req = Request.Builder().url(url).head().build()
                probeClient.newCall(req).execute().use { resp ->
                    code = resp.code
                }
            }
            if (code in 200..399) elapsed else -1
        } catch (e: Exception) {
            -1
        }
    }
}
