package com.webtoapp.core.webview

import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONArray

/**
 * JavaScript桥接类 - 在Native层执行翻译请求，避免CORS限制
 */
class TranslateBridge(
    private val webView: WebView,
    private val scope: CoroutineScope
) {
    companion object {
        const val JS_INTERFACE_NAME = "_nativeTranslate"
        private const val TRANSLATE_API = "https://translate.googleapis.com/translate_a/single"
    }

    /**
     * JavaScript调用此方法进行翻译
     * @param textsJson JSON数组格式的待翻译文本
     * @param targetLang 目标语言代码
     * @param callbackId 回调ID，用于将结果返回给JavaScript
     */
    @JavascriptInterface
    fun translate(textsJson: String, targetLang: String, callbackId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val texts = JSONArray(textsJson)
                val results = mutableListOf<String>()
                
                // 分批翻译，每批最多10条
                val batchSize = 10
                for (i in 0 until texts.length() step batchSize) {
                    val batch = mutableListOf<String>()
                    for (j in i until minOf(i + batchSize, texts.length())) {
                        batch.add(texts.getString(j))
                    }
                    
                    val translated = translateBatch(batch, targetLang)
                    results.addAll(translated)
                }
                
                // 将结果返回给JavaScript
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
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    webView.evaluateJavascript(
                        "window._translateCallback('$callbackId', null, '${e.message}');",
                        null
                    )
                }
            }
        }
    }

    private fun translateBatch(texts: List<String>, targetLang: String): List<String> {
        if (texts.isEmpty()) return emptyList()
        
        val combined = texts.joinToString("\n")
        val encoded = URLEncoder.encode(combined, "UTF-8")
        val urlStr = "$TRANSLATE_API?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"
        
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        
        return try {
            val response = conn.inputStream.bufferedReader().readText()
            parseTranslateResponse(response, texts.size)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseTranslateResponse(response: String, expectedCount: Int): List<String> {
        // Google Translate API返回格式: [[["translated","original",null,null,10],...],null,"en",...]
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
            // Parse失败，返回空列表
            for (i in 0 until expectedCount) {
                results.add("")
            }
        }
        return results
    }
}
