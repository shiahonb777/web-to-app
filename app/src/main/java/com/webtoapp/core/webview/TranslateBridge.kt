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
















class TranslateBridge(
    private val webView: WebView,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "TranslateBridge"
        const val JS_INTERFACE_NAME = "_nativeTranslate"


        enum class TranslateEngine(val displayName: String) {
            GOOGLE("Google Translate"),
            MYMEMORY("MyMemory"),
            LIBRE("LibreTranslate"),
            LINGVA("Lingva Translate");
        }


        private const val GOOGLE_API = "https://translate.googleapis.com/translate_a/single"
        private const val MYMEMORY_API = "https://api.mymemory.translated.net/get"
        private const val LIBRE_API = "https://libretranslate.com/translate"
        private const val LINGVA_API = "https://lingva.ml/api/v1"


        private const val ENGINE_FAILURE_THRESHOLD = 3

        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000

        private const val RETRY_DELAY_MS = 200L
    }


    @Volatile
    private var activeEngineIndex = 0


    private val failureCounts = IntArray(TranslateEngine.entries.size)


    private val engines = TranslateEngine.entries.toTypedArray()


    private fun currentEngine(): TranslateEngine = engines[activeEngineIndex]




    private fun recordSuccess(engine: TranslateEngine) {
        failureCounts[engine.ordinal] = 0
    }




    private fun recordFailure(engine: TranslateEngine) {
        failureCounts[engine.ordinal]++
        if (failureCounts[engine.ordinal] >= ENGINE_FAILURE_THRESHOLD) {
            val nextIndex = (engine.ordinal + 1) % engines.size
            AppLogger.w(TAG, "${engine.displayName} 连续失败 ${failureCounts[engine.ordinal]} 次，降级到 ${engines[nextIndex].displayName}")
            activeEngineIndex = nextIndex
        }
    }







    @JavascriptInterface
    fun translate(textsJson: String, targetLang: String, callbackId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val texts = JSONArray(textsJson)
                val results = mutableListOf<String>()


                val batchSize = 10
                for (i in 0 until texts.length() step batchSize) {
                    val batch = mutableListOf<String>()
                    for (j in i until minOf(i + batchSize, texts.length())) {
                        batch.add(texts.getString(j))
                    }
                    val translated = translateBatchWithFallback(batch, targetLang)
                    results.addAll(translated)
                }


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




    @JavascriptInterface
    fun setEngine(engineName: String) {
        val engine = engines.find {
            it.name.equals(engineName, ignoreCase = true) ||
            it.displayName.equals(engineName, ignoreCase = true)
        }
        if (engine != null) {
            activeEngineIndex = engine.ordinal
            failureCounts[engine.ordinal] = 0
            AppLogger.i(TAG, "手动切换翻译引擎: ${engine.displayName}")
        }
    }




    private suspend fun translateBatchWithFallback(texts: List<String>, targetLang: String): List<String> {
        if (texts.isEmpty()) return emptyList()


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

                    activeEngineIndex = engine.ordinal
                    AppLogger.i(TAG, "降级翻译成功，切换活跃引擎到: ${engine.displayName}")
                }
                return result
            } catch (e: Exception) {
                AppLogger.w(TAG, "${engine.displayName} 翻译失败: ${e.message}")
                recordFailure(engine)
                delay(RETRY_DELAY_MS)
            }
        }


        AppLogger.e(TAG, "所有翻译引擎均失败，返回原文")
        return texts
    }





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

        val results = mutableListOf<String>()
        try {
            val json = JSONArray(response)
            val translations = json.getJSONArray(0)
            val sb = StringBuilder()

            for (i in 0 until translations.length()) {
                val item = translations.getJSONArray(i)
                sb.append(item.getString(0))
            }


            val parts = sb.toString().split("\n")
            results.addAll(parts)


            while (results.size < expectedCount) {
                results.add("")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "Google 响应解析失败: ${e.message}")
            for (i in 0 until expectedCount) results.add("")
        }
        return results
    }





    private fun translateViaMyMemory(texts: List<String>, targetLang: String): List<String> {


        val combined = texts.joinToString("\n")
        val encoded = URLEncoder.encode(combined, "UTF-8")

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





    private fun translateViaLingva(texts: List<String>, targetLang: String): List<String> {
        val combined = texts.joinToString("\n")
        val encoded = URLEncoder.encode(combined, "UTF-8")

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
