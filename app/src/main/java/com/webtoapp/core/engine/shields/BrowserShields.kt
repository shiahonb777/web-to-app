package com.webtoapp.core.engine.shields

import android.annotation.SuppressLint
import android.content.Context
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.util.GsonProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BrowserShields — 浏览器隐私保护总控
 *
 * 统一管理 HTTPS 升级、跟踪器拦截、Cookie 弹窗关闭、GPC、阅读模式等
 * 所有子模块的生命周期和状态由此类管理
 */
@SuppressLint("StaticFieldLeak")
class BrowserShields(private val context: Context) {

    companion object {
        private const val TAG = "BrowserShields"
        private const val PREFS_NAME = "browser_shields_prefs"
        private const val KEY_CONFIG = "shields_config"

        @Volatile
        private var instance: BrowserShields? = null

        fun getInstance(context: Context): BrowserShields {
            return instance ?: synchronized(this) {
                instance ?: BrowserShields(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        synchronized(Companion) {
            if (instance == null) {
                instance = this
            }
        }
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = GsonProvider.gson

    // ==================== 配置 ====================

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ShieldsConfig> = _config.asStateFlow()

    // ==================== 子模块 ====================

    val stats = ShieldsStats()
    val httpsUpgrader = HttpsUpgrader()
    val trackerBlocker = TrackerBlocker()
    val gpcInjector = GpcInjector()
    val cookieConsentBlocker = CookieConsentBlocker()
    val readerMode = ReaderMode()

    // ==================== 配置管理 ====================

    fun getConfig(): ShieldsConfig = _config.value

    fun updateConfig(newConfig: ShieldsConfig) {
        _config.value = newConfig
        saveConfig(newConfig)
        AppLogger.i(TAG, "Shields config updated: enabled=${newConfig.enabled}")
    }

    fun updateConfig(transform: (ShieldsConfig) -> ShieldsConfig) {
        val newConfig = transform(_config.value)
        updateConfig(newConfig)
    }

    /** 总开关 */
    fun setEnabled(enabled: Boolean) {
        updateConfig { it.copy(enabled = enabled) }
    }

    fun isEnabled(): Boolean = _config.value.enabled

    // ==================== 各功能开关快捷方法 ====================

    fun setHttpsUpgrade(enabled: Boolean) = updateConfig { it.copy(httpsUpgrade = enabled) }
    fun setTrackerBlocking(enabled: Boolean) = updateConfig { it.copy(trackerBlocking = enabled) }
    fun setCookieConsentBlock(enabled: Boolean) = updateConfig { it.copy(cookieConsentBlock = enabled) }
    fun setGpcEnabled(enabled: Boolean) = updateConfig { it.copy(gpcEnabled = enabled) }
    fun setReaderMode(enabled: Boolean) = updateConfig { it.copy(readerModeEnabled = enabled) }
    
    fun setThirdPartyCookiePolicy(policy: ThirdPartyCookiePolicy) {
        updateConfig { it.copy(thirdPartyCookiePolicy = policy) }
    }
    
    fun setReferrerPolicy(policy: ShieldsReferrerPolicy) {
        updateConfig { it.copy(referrerPolicy = policy) }
    }

    fun setSslErrorPolicy(policy: SslErrorPolicy) {
        updateConfig { it.copy(sslErrorPolicy = policy) }
    }

    // ==================== 页面生命周期 ====================

    /** 页面开始加载时调用 */
    fun onPageStarted(url: String?) {
        stats.resetPageStats()
        httpsUpgrader.onPageStarted()
    }

    /** 页面加载完成时调用 */
    fun onPageFinished(url: String?) {
        // 可用于后续统计汇总
    }

    // ==================== 持久化 ====================

    private fun loadConfig(): ShieldsConfig {
        val json = prefs.getString(KEY_CONFIG, null) ?: return ShieldsConfig.DEFAULT
        return try {
            gson.fromJson(json, ShieldsConfig::class.java) ?: ShieldsConfig.DEFAULT
        } catch (e: Exception) {
            AppLogger.w(TAG, "Failed to load shields config: ${e.message}")
            ShieldsConfig.DEFAULT
        }
    }

    private fun saveConfig(config: ShieldsConfig) {
        try {
            prefs.edit().putString(KEY_CONFIG, gson.toJson(config)).apply()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save shields config", e)
        }
    }
}
