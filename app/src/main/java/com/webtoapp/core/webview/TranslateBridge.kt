package com.webtoapp.core.webview

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray
import org.json.JSONObject

/**
 * 多引擎翻译桥接 — 在 Native 层执行翻译请求，避免 CORS 限制
 *
 * 支持的翻译引擎（按优先级自动降级）：
 * 1. Google Translate (translate.googleapis.com)    — 免费、无限制
 * 2. MyMemory       (api.mymemory.translated.net)  — 开源翻译记忆库
 * 3. LibreTranslate  (libretranslate.com)           — 自托管开源翻译引擎
 * 4. Lingva         (lingva.ml)                     — 开源 Google 前端代理
 *
 * 设计原则：
 * - 不依赖任何特定浏览器或 Chrome 专属 API
 * - 所有请求在 Native 层通过 HttpURLConnection 发起，直接绕过 WebView CORS
 * - 自动引擎降级：当前引擎连续失败 ≥3 次后自动切换到下一引擎
 * - 线程安全：所有翻译在 Dispatchers.IO 上执行，回调通过 Dispatchers.Main 回传 WebView
 */
class TranslateBridge(
    private val webView: WebView,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "TranslateBridge"
        const val JS_INTERFACE_NAME = "_nativeTranslate"

        /** 翻译引擎枚举 — 按优先级排列 */
        enum class TranslateEngine(val displayName: String) {
            GOOGLE("Google Translate"),
            MYMEMORY("MyMemory"),
            LIBRE("LibreTranslate"),
            LINGVA("Lingva Translate");
        }

        // 引擎 API 端点
        private const val GOOGLE_API = "https://translate.googleapis.com/translate_a/single"
        private const val MYMEMORY_API = "https://api.mymemory.translated.net/get"
        private const val LIBRE_API = "https://libretranslate.com/translate"
        private const val LINGVA_API = "https://lingva.ml/api/v1"

        // 降级阈值：连续失败 N 次后切换引擎
        private const val ENGINE_FAILURE_THRESHOLD = 3
        // 通用连接超时
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000
        // 重试延迟
        private const val RETRY_DELAY_MS = 200L
    }

    // 当前活跃引擎索引
    @Volatile
    private var activeEngineIndex = 0

    // 每个引擎的连续失败计数
    private val failureCounts = IntArray(TranslateEngine.entries.size)

    /** 引擎列表（固定优先级顺序） */
    private val engines = TranslateEngine.entries.toTypedArray()

    /** 获取当前活跃引擎 */
    private fun currentEngine(): TranslateEngine = engines[activeEngineIndex]

    /**
     * 记录引擎成功 — 重置该引擎的失败计数
     */
    private fun recordSuccess(engine: TranslateEngine) {
        failureCounts[engine.ordinal] = 0
    }

    /**
     * 记录引擎失败 — 如果超过阈值则自动切换到下一引擎
     */
    private fun recordFailure(engine: TranslateEngine) {
        failureCounts[engine.ordinal]++
        if (failureCounts[engine.ordinal] >= ENGINE_FAILURE_THRESHOLD) {
            val nextIndex = (engine.ordinal + 1) % engines.size
            AppLogger.w(TAG, "${engine.displayName} 连续失败 ${failureCounts[engine.ordinal]} 次，降级到 ${engines[nextIndex].displayName}")
            activeEngineIndex = nextIndex
        }
    }

    /**
     * JavaScript 调用此方法进行翻译
     * @param textsJson JSON 数组格式的待翻译文本
     * @param targetLang 目标语言代码
     * @param callbackId 回调 ID，用于将结果返回给 JavaScript
     */
    @JavascriptInterface
    fun translate(textsJson: String, targetLang: String, callbackId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val texts = JSONArray(textsJson)
                val results = mutableListOf<String>()

                // 分批翻译，每批最多 10 条
                val batchSize = 10
                for (i in 0 until texts.length() step batchSize) {
                    val batch = mutableListOf<String>()
                    for (j in i until minOf(i + batchSize, texts.length())) {
                        batch.add(texts.getString(j))
                    }
                    val translated = translateBatchWithFallback(batch, targetLang)
                    results.addAll(translated)
                }

                // 将结果返回给 JavaScript
                val resultsJson = JSONArray(results).toString()
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")

                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "window._translateCallback('$callbackId', '$resultsJson');",
                        null
                    )
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "翻译执行失败", e)
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "window._translateCallback('$callbackId', null, '${e.message?.replace("'", "\\'")}');",
                        null
                    )
                }
            }
        }
    }

    /**
     * JavaScript 调用此方法获取当前引擎状态（用于 UI 显示）
     */
    @JavascriptInterface
    fun getEngineStatus(): String {
        val status = JSONObject().apply {
            put("activeEngine", currentEngine().displayName)
            put("engines", JSONArray().apply {
                engines.forEach { engine ->
                    put(JSONObject().apply {
                        put("name", engine.displayName)
                        put("failures", failureCounts[engine.ordinal])
                        put("active", engine == currentEngine())
                    })
                }
            })
        }
        return status.toString()
    }

    /**
     * JavaScript 调用此方法手动选择翻译引擎
     */
    @JavascriptInterface
    fun setEngine(engineName: String) {
        val engine = engines.find {
            it.name.equals(engineName, ignoreCase = true) ||
            it.displayName.equals(engineName, ignoreCase = true)
        }
        if (engine != null) {
            activeEngineIndex = engine.ordinal
            failureCounts[engine.ordinal] = 0  // 重置失败计数
            AppLogger.i(TAG, "手动切换翻译引擎: ${engine.displayName}")
        }
    }

    /**
     * 带降级的批量翻译 — 自动在多引擎间切换
     */
    private fun translateBatchWithFallback(texts: List<String>, targetLang: String): List<String> {
        if (texts.isEmpty()) return emptyList()

        // 尝试所有引擎（从当前活跃引擎开始）
        val attemptOrder = (0 until engines.size).map { offset ->
            engines[(activeEngineIndex + offset) % engines.size]
        }

        for (engine in attemptOrder) {
            try {
                val result = when (engine) {
                    TranslateEngine.GOOGLE -> translateViaGoogle(texts, targetLang)
                    TranslateEngine.MYMEMORY -> translateViaMyMemory(texts, targetLang)
                    TranslateEngine.LIBRE -> translateViaLibre(texts, targetLang)
                    TranslateEngine.LINGVA -> translateViaLingva(texts, targetLang)
                }
                recordSuccess(engine)
                if (engine != currentEngine()) {
                    // 如果是通过降级成功的，更新活跃引擎
                    activeEngineIndex = engine.ordinal
                    AppLogger.i(TAG, "降级翻译成功，切换活跃引擎到: ${engine.displayName}")
                }
                return result
            } catch (e: Exception) {
                AppLogger.w(TAG, "${engine.displayName} 翻译失败: ${e.message}")
                recordFailure(engine)
                // 短暂延迟后尝试下一引擎
                Thread.sleep(RETRY_DELAY_MS)
            }
        }

        // 所有引擎都失败，返回空列表
        AppLogger.e(TAG, "所有翻译引擎均失败，返回原文")
        return texts
    }

    // ═══════════════════════════════════════════════════
    // 引擎 1: Google Translate (translate.googleapis.com)
    // ═══════════════════════════════════════════════════

    private fun translateViaGoogle(texts: List<String>, targetLang: String): List<String> {
        val combined = texts.joinToString("\n")
        val encoded = URLEncoder.encode(combined, "UTF-8")
        val urlStr = "$GOOGLE_API?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"

        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36")
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS

        return try {
            val response = conn.inputStream.bufferedReader().readText()
            parseGoogleResponse(response, texts.size)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseGoogleResponse(response: String, expectedCount: Int): List<String> {
        // Google Translate API 返回格式: [[["translated","original",null,null,10],...],null,"en",...]
        val results = mutableListOf<String>()
        try {
            val json = JSONArray(response)
            val translations = json.getJSONArray(0)
            val sb = StringBuilder()

            for (i in 0 until translations.length()) {
                val item = translations.getJSONArray(i)
                sb.append(item.getString(0))
            }

            // 按换行符分割回原始数量
            val parts = sb.toString().split("\n")
            results.addAll(parts)

            // 补齐数量
            while (results.size < expectedCount) {
                results.add("")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Google 响应解析失败: ${e.message}")
            for (i in 0 until expectedCount) results.add("")
        }
        return results
    }

    // ═══════════════════════════════════════════════════
    // 引擎 2: MyMemory (api.mymemory.translated.net)
    // ═══════════════════════════════════════════════════

    private fun translateViaMyMemory(texts: List<String>, targetLang: String): List<String> {
        // MyMemory 不支持批量翻译，需逐条请求
        // 为减少请求数，合并为单次请求（用换行分隔）
        val combined = texts.joinToString("\n")
        val encoded = URLEncoder.encode(combined, "UTF-8")
        // MyMemory 的源语言用 autodetect
        val langPair = URLEncoder.encode("autodetect|$targetLang", "UTF-8")
        val urlStr = "$MYMEMORY_API?q=$encoded&langpair=$langPair"

        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "WebToApp/1.0")
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS

        return try {
            val response = conn.inputStream.bufferedReader().readText()
            parseMyMemoryResponse(response, texts.size)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseMyMemoryResponse(response: String, expectedCount: Int): List<String> {
        val results = mutableListOf<String>()
        try {
            val json = JSONObject(response)
            val responseData = json.getJSONObject("responseData")
            val translatedText = responseData.getString("translatedText")

            val parts = translatedText.split("\n")
            results.addAll(parts)

            while (results.size < expectedCount) results.add("")
        } catch (e: Exception) {
            AppLogger.w(TAG, "MyMemory 响应解析失败: ${e.message}")
            for (i in 0 until expectedCount) results.add("")
        }
        return results
    }

    // ═══════════════════════════════════════════════════
    // 引擎 3: LibreTranslate (libretranslate.com)
    // ═══════════════════════════════════════════════════

    private fun translateViaLibre(texts: List<String>, targetLang: String): List<String> {
        val combined = texts.joinToString("\n")

        val url = URL(LIBRE_API)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("User-Agent", "WebToApp/1.0")
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.doOutput = true

        val body = JSONObject().apply {
            put("q", combined)
            put("source", "auto")
            put("target", targetLang)
            put("format", "text")
        }.toString()

        return try {
            conn.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }
            val response = conn.inputStream.bufferedReader().readText()
            parseLibreResponse(response, texts.size)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseLibreResponse(response: String, expectedCount: Int): List<String> {
        val results = mutableListOf<String>()
        try {
            val json = JSONObject(response)
            val translatedText = json.getString("translatedText")

            val parts = translatedText.split("\n")
            results.addAll(parts)

            while (results.size < expectedCount) results.add("")
        } catch (e: Exception) {
            AppLogger.w(TAG, "LibreTranslate 响应解析失败: ${e.message}")
            for (i in 0 until expectedCount) results.add("")
        }
        return results
    }

    // ═══════════════════════════════════════════════════
    // 引擎 4: Lingva Translate (lingva.ml)
    // ═══════════════════════════════════════════════════

    private fun translateViaLingva(texts: List<String>, targetLang: String): List<String> {
        val combined = texts.joinToString("\n")
        val encoded = URLEncoder.encode(combined, "UTF-8")
        // Lingva API: /api/v1/:source/:target/:query
        val urlStr = "$LINGVA_API/auto/$targetLang/$encoded"

        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "WebToApp/1.0")
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS

        return try {
            val response = conn.inputStream.bufferedReader().readText()
            parseLingvaResponse(response, texts.size)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseLingvaResponse(response: String, expectedCount: Int): List<String> {
        val results = mutableListOf<String>()
        try {
            val json = JSONObject(response)
            val translatedText = json.getString("translation")

            val parts = translatedText.split("\n")
            results.addAll(parts)

            while (results.size < expectedCount) results.add("")
        } catch (e: Exception) {
            AppLogger.w(TAG, "Lingva 响应解析失败: ${e.message}")
            for (i in 0 until expectedCount) results.add("")
        }
        return results
    }
}
