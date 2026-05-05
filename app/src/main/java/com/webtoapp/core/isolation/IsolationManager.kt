package com.webtoapp.core.isolation

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.logging.AppLogger
import android.webkit.WebView









@SuppressLint("StaticFieldLeak")
class IsolationManager(private val context: Context) {

    companion object {
        private const val TAG = "IsolationManager"
        private const val PREFS_NAME = "isolation_prefs"
        private const val KEY_FINGERPRINT = "fingerprint"
        private const val KEY_CONFIG = "config"

        @Volatile
        private var instance: IsolationManager? = null

        fun getInstance(context: Context): IsolationManager {
            return instance ?: synchronized(this) {
                instance ?: IsolationManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = com.webtoapp.util.GsonProvider.gson

    private var currentConfig: IsolationConfig? = null
    private var currentFingerprint: GeneratedFingerprint? = null




    fun initialize(config: IsolationConfig) {
        currentConfig = config

        if (!config.enabled) {
            AppLogger.w(TAG, "隔离环境未启用")
            return
        }

        AppLogger.w(TAG, "初始化隔离环境: $config")


        currentFingerprint = if (config.fingerprintConfig.regenerateOnLaunch) {

            generateNewFingerprint(config.fingerprintConfig.fingerprintId)
        } else {

            loadFingerprint() ?: generateNewFingerprint(config.fingerprintConfig.fingerprintId)
        }


        saveConfig(config)
    }




    private fun generateNewFingerprint(seed: String): GeneratedFingerprint {
        val fingerprint = FingerprintGenerator.generateFingerprint(seed)
        saveFingerprint(fingerprint)
        AppLogger.w(TAG, "生成新指纹: UA=${fingerprint.userAgent.take(50)}...")
        return fingerprint
    }




    private fun saveFingerprint(fingerprint: GeneratedFingerprint) {
        prefs.edit().putString(KEY_FINGERPRINT, gson.toJson(fingerprint)).apply()
    }




    private fun loadFingerprint(): GeneratedFingerprint? {
        val json = prefs.getString(KEY_FINGERPRINT, null) ?: return null
        return try {
            gson.fromJson(json, GeneratedFingerprint::class.java)
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载指纹失败", e)
            null
        }
    }




    private fun saveConfig(config: IsolationConfig) {
        prefs.edit().putString(KEY_CONFIG, gson.toJson(config)).apply()
    }




    fun loadConfig(): IsolationConfig? {
        val json = prefs.getString(KEY_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, IsolationConfig::class.java).also {
                currentConfig = it
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "加载配置失败", e)
            null
        }
    }




    fun getConfig(): IsolationConfig? = currentConfig




    fun getFingerprint(): GeneratedFingerprint? = currentFingerprint




    fun getUserAgent(): String? {
        val config = currentConfig ?: return null
        if (!config.enabled) return null

        return config.fingerprintConfig.customUserAgent
            ?: currentFingerprint?.userAgent
    }




    fun getCustomHeaders(): Map<String, String> {
        val config = currentConfig ?: return emptyMap()
        if (!config.enabled || !config.headerConfig.enabled) return emptyMap()

        val headers = mutableMapOf<String, String>()


        config.headerConfig.customHeaders.forEach { (key, value) ->
            headers[key] = value
        }


        config.headerConfig.acceptLanguage?.let {
            headers["Accept-Language"] = it
        } ?: currentFingerprint?.language?.let {
            headers["Accept-Language"] = it
        }


        if (config.headerConfig.dnt) {
            headers["DNT"] = "1"
        }


        if (config.ipSpoofConfig.enabled) {
            val fakeIp = config.ipSpoofConfig.customIp
                ?: FingerprintGenerator.generateRandomIp(
                    config.ipSpoofConfig.randomIpRange,
                    config.ipSpoofConfig.searchKeyword
                )

            if (config.ipSpoofConfig.xForwardedFor) {
                headers["X-Forwarded-For"] = fakeIp
            }
            if (config.ipSpoofConfig.xRealIp) {
                headers["X-Real-IP"] = fakeIp
            }
            if (config.ipSpoofConfig.clientIp) {
                headers["Client-IP"] = fakeIp
            }
        }

        return headers
    }




    fun generateIsolationScript(): String {
        val config = currentConfig ?: return ""
        val fingerprint = currentFingerprint ?: return ""

        if (!config.enabled) return ""

        return IsolationScriptInjector.generateIsolationScript(config, fingerprint)
    }




    fun applyToWebView(webView: WebView) {
        val config = currentConfig ?: return
        if (!config.enabled) return


        getUserAgent()?.let { ua ->
            webView.settings.userAgentString = ua
            AppLogger.w(TAG, "设置 User-Agent: ${ua.take(50)}...")
        }


        val script = generateIsolationScript()
        if (script.isNotEmpty()) {
            webView.evaluateJavascript(script) { result ->
                AppLogger.w(TAG, "隔离脚本注入完成: $result")
            }
        }
    }




    fun injectOnPageStart(webView: WebView) {
        val script = generateIsolationScript()
        if (script.isNotEmpty()) {
            webView.evaluateJavascript(script, null)
        }
    }




    fun clearData() {
        prefs.edit().clear().apply()
        currentConfig = null
        currentFingerprint = null
        AppLogger.w(TAG, "隔离数据已清除")
    }




    fun regenerateFingerprint() {
        currentConfig ?: return
        currentFingerprint = generateNewFingerprint(java.util.UUID.randomUUID().toString())
        AppLogger.w(TAG, "指纹已重新生成")
    }
}
