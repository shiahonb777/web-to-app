package com.webtoapp.core.isolation

import android.content.Context
import android.util.Log
import android.webkit.WebView
import com.google.gson.Gson

/**
 * 隔离环境管理器
 * 
 * 负责管理应用的独立浏览器环境，包括：
 * - 指纹生成和管理
 * - 脚本注入
 * - 配置持久化
 */
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
    private val gson = Gson()
    
    private var currentConfig: IsolationConfig? = null
    private var currentFingerprint: GeneratedFingerprint? = null
    
    /**
     * 初始化隔离环境
     */
    fun initialize(config: IsolationConfig) {
        currentConfig = config
        
        if (!config.enabled) {
            Log.d(TAG, "隔离环境未启用")
            return
        }
        
        Log.d(TAG, "初始化隔离环境: $config")
        
        // Generate或加载指纹
        currentFingerprint = if (config.fingerprintConfig.regenerateOnLaunch) {
            // 每次启动重新生成
            generateNewFingerprint(config.fingerprintConfig.fingerprintId)
        } else {
            // 尝试加载已保存的指纹，如果没有则生成新的
            loadFingerprint() ?: generateNewFingerprint(config.fingerprintConfig.fingerprintId)
        }
        
        // Save配置
        saveConfig(config)
    }
    
    /**
     * 生成新指纹
     */
    private fun generateNewFingerprint(seed: String): GeneratedFingerprint {
        val fingerprint = FingerprintGenerator.generateFingerprint(seed)
        saveFingerprint(fingerprint)
        Log.d(TAG, "生成新指纹: UA=${fingerprint.userAgent.take(50)}...")
        return fingerprint
    }
    
    /**
     * 保存指纹
     */
    private fun saveFingerprint(fingerprint: GeneratedFingerprint) {
        prefs.edit().putString(KEY_FINGERPRINT, gson.toJson(fingerprint)).apply()
    }
    
    /**
     * 加载指纹
     */
    private fun loadFingerprint(): GeneratedFingerprint? {
        val json = prefs.getString(KEY_FINGERPRINT, null) ?: return null
        return try {
            gson.fromJson(json, GeneratedFingerprint::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "加载指纹失败", e)
            null
        }
    }
    
    /**
     * 保存配置
     */
    private fun saveConfig(config: IsolationConfig) {
        prefs.edit().putString(KEY_CONFIG, gson.toJson(config)).apply()
    }
    
    /**
     * 加载配置
     */
    fun loadConfig(): IsolationConfig? {
        val json = prefs.getString(KEY_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, IsolationConfig::class.java).also {
                currentConfig = it
            }
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败", e)
            null
        }
    }
    
    /**
     * 获取当前配置
     */
    fun getConfig(): IsolationConfig? = currentConfig
    
    /**
     * 获取当前指纹
     */
    fun getFingerprint(): GeneratedFingerprint? = currentFingerprint
    
    /**
     * 获取伪造的 User-Agent
     */
    fun getUserAgent(): String? {
        val config = currentConfig ?: return null
        if (!config.enabled) return null
        
        return config.fingerprintConfig.customUserAgent 
            ?: currentFingerprint?.userAgent
    }
    
    /**
     * 获取伪造的 HTTP Headers
     */
    fun getCustomHeaders(): Map<String, String> {
        val config = currentConfig ?: return emptyMap()
        if (!config.enabled || !config.headerConfig.enabled) return emptyMap()
        
        val headers = mutableMapOf<String, String>()
        
        // 基础 Headers
        config.headerConfig.customHeaders.forEach { (key, value) ->
            headers[key] = value
        }
        
        // Accept-Language
        config.headerConfig.acceptLanguage?.let {
            headers["Accept-Language"] = it
        } ?: currentFingerprint?.language?.let {
            headers["Accept-Language"] = it
        }
        
        // DNT
        if (config.headerConfig.dnt) {
            headers["DNT"] = "1"
        }
        
        // IP 伪装 Headers
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
    
    /**
     * 生成隔离脚本
     */
    fun generateIsolationScript(): String {
        val config = currentConfig ?: return ""
        val fingerprint = currentFingerprint ?: return ""
        
        if (!config.enabled) return ""
        
        return IsolationScriptInjector.generateIsolationScript(config, fingerprint)
    }
    
    /**
     * 应用隔离配置到 WebView
     */
    fun applyToWebView(webView: WebView) {
        val config = currentConfig ?: return
        if (!config.enabled) return
        
        // Set User-Agent
        getUserAgent()?.let { ua ->
            webView.settings.userAgentString = ua
            Log.d(TAG, "设置 User-Agent: ${ua.take(50)}...")
        }
        
        // Inject隔离脚本
        val script = generateIsolationScript()
        if (script.isNotEmpty()) {
            webView.evaluateJavascript(script) { result ->
                Log.d(TAG, "隔离脚本注入完成: $result")
            }
        }
    }
    
    /**
     * 在页面加载开始时注入脚本
     */
    fun injectOnPageStart(webView: WebView) {
        val script = generateIsolationScript()
        if (script.isNotEmpty()) {
            webView.evaluateJavascript(script, null)
        }
    }
    
    /**
     * 清除隔离数据
     */
    fun clearData() {
        prefs.edit().clear().apply()
        currentConfig = null
        currentFingerprint = null
        Log.d(TAG, "隔离数据已清除")
    }
    
    /**
     * 重新生成指纹
     */
    fun regenerateFingerprint() {
        currentConfig ?: return
        currentFingerprint = generateNewFingerprint(java.util.UUID.randomUUID().toString())
        Log.d(TAG, "指纹已重新生成")
    }
}
