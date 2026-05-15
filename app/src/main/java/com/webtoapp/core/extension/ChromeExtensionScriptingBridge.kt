package com.webtoapp.core.extension

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import org.json.JSONArray
import org.json.JSONObject
import com.webtoapp.util.sha256

object ChromeExtensionScriptingBridge {

    private const val TAG = "ChromeExtScripting"
    private const val STYLE_ATTR = "data-wta-scripting-style"

    fun executeScript(
        context: Context?,
        webView: WebView?,
        extensionId: String,
        injectionJson: String
    ): String {
        val target = webView ?: return resultsJson(null)
        return try {
            val injection = JSONObject(injectionJson)
            val code = resolveExecutionSource(context, extensionId, injection)
            val wrapped = """
                (function() {
                    try {
                        return (function() {
                            $code
                        })();
                    } catch (e) {
                        return { __wtaExecuteScriptError: String((e && e.message) || e) };
                    }
                })();
            """.trimIndent()
            evaluateSync(target, wrapped)
        } catch (e: Exception) {
            AppLogger.e(TAG, "executeScript failed for $extensionId", e)
            resultsJson(null)
        }
    }

    fun insertCss(
        context: Context?,
        webView: WebView?,
        extensionId: String,
        injectionJson: String
    ): Boolean {
        val target = webView ?: return false
        return try {
            val injection = JSONObject(injectionJson)
            val css = resolveCssText(context, extensionId, injection)
            if (css.isBlank()) return false
            val styleId = computeStyleId(extensionId, css, injection)
            val escapedCss = css
                .replace("\\", "\\\\")
                .replace("`", "\\`")
                .replace("\$", "\\\$")
            val script = """
                (function() {
                    try {
                        var existing = document.querySelector('style[$STYLE_ATTR="${escapeForSelector(styleId)}"]');
                        if (existing) return true;
                        var style = document.createElement('style');
                        style.setAttribute('$STYLE_ATTR', '$styleId');
                        style.textContent = `$escapedCss`;
                        (document.head || document.documentElement).appendChild(style);
                        return true;
                    } catch (e) {
                        return false;
                    }
                })();
            """.trimIndent()
            evaluateSync(target, script).contains("true")
        } catch (e: Exception) {
            AppLogger.e(TAG, "insertCSS failed for $extensionId", e)
            false
        }
    }

    fun removeCss(
        context: Context?,
        webView: WebView?,
        extensionId: String,
        injectionJson: String
    ): Boolean {
        val target = webView ?: return false
        return try {
            val injection = JSONObject(injectionJson)
            val css = resolveCssText(context, extensionId, injection)
            if (css.isBlank()) return false
            val styleId = computeStyleId(extensionId, css, injection)
            val script = """
                (function() {
                    try {
                        var node = document.querySelector('style[$STYLE_ATTR="${escapeForSelector(styleId)}"]');
                        if (!node) return false;
                        node.remove();
                        return true;
                    } catch (e) {
                        return false;
                    }
                })();
            """.trimIndent()
            evaluateSync(target, script).contains("true")
        } catch (e: Exception) {
            AppLogger.e(TAG, "removeCSS failed for $extensionId", e)
            false
        }
    }

    internal fun resolveExecutionSourceForTest(
        context: Context?,
        extensionId: String,
        injectionJson: String
    ): String {
        return resolveExecutionSource(context, extensionId, JSONObject(injectionJson))
    }

    internal fun resolveCssTextForTest(
        context: Context?,
        extensionId: String,
        injectionJson: String
    ): String {
        return resolveCssText(context, extensionId, JSONObject(injectionJson))
    }

    private fun resolveExecutionSource(
        context: Context?,
        extensionId: String,
        injection: JSONObject
    ): String {
        val fileBundle = resolveFileBundle(context, extensionId, injection.optJSONArray("files"), isCss = false)
        if (fileBundle.isNotBlank()) {
            return "$fileBundle\n\nreturn undefined;"
        }
        val argsJson = injection.optJSONArray("args") ?: JSONArray()
        return when {
            injection.optJSONObject("func") != null -> buildFunctionInvocation(
                injection.getJSONObject("func"),
                argsJson
            )
            injection.optString("functionCode").isNotBlank() ->
                buildFunctionCodeInvocation(injection.optString("functionCode"), argsJson)
            injection.optString("code").isNotBlank() -> injection.optString("code")
            else -> "undefined"
        }
    }

    private fun resolveCssText(
        context: Context?,
        extensionId: String,
        injection: JSONObject
    ): String {
        val fileBundle = resolveFileBundle(context, extensionId, injection.optJSONArray("files"), isCss = true)
        if (fileBundle.isNotBlank()) return fileBundle
        return injection.optString("css")
    }

    private fun resolveFileBundle(
        context: Context?,
        extensionId: String,
        filesJson: JSONArray?,
        isCss: Boolean
    ): String {
        if (context == null || filesJson == null || filesJson.length() == 0) return ""
        val paths = buildList {
            for (index in 0 until filesJson.length()) {
                val path = filesJson.optString(index).orEmpty()
                if (path.isNotBlank()) add(path)
            }
        }
        return ChromeExtensionResourceLoader.loadResourceBundle(
            context = context,
            extensionId = extensionId,
            paths = paths,
            isCss = isCss
        )
    }

    private fun buildFunctionInvocation(functionObject: JSONObject, argsJson: JSONArray): String {
        val code = functionObject.optString("code")
        return buildFunctionCodeInvocation(code, argsJson)
    }

    private fun buildFunctionCodeInvocation(functionCode: String, argsJson: JSONArray): String {
        val fnExpr = functionCode.trim().ifBlank { "function(){ return undefined; }" }
        return "return ($fnExpr).apply(null, ${argsJson.toString()});"
    }

    private fun resultsJson(valueJson: String?): String {
        val value = valueJson ?: "null"
        return """[{"result":$value}]"""
    }

    private fun evaluateSync(webView: WebView, script: String): String {
        val latch = java.util.concurrent.CountDownLatch(1)
        var result = "null"
        Handler(Looper.getMainLooper()).post {
            webView.evaluateJavascript(script) {
                result = it ?: "null"
                latch.countDown()
            }
        }
        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
        return resultsJson(result)
    }

    private fun computeStyleId(extensionId: String, css: String, injection: JSONObject): String {
        val origin = buildString {
            append(extensionId)
            append("|")
            append(css)
            append("|")
            append(injection.optString("origin"))
            append("|")
            append(injection.optString("cssOrigin"))
        }
        return "wta-" + origin.sha256().take(16)
    }

    private fun escapeForSelector(input: String): String {
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
    }
}
