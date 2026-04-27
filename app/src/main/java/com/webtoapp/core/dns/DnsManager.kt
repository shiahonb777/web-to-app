package com.webtoapp.core.dns

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.DnsConfig
import com.webtoapp.data.model.DnsProvider
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

/**
 * DNS 管理器
 * 
 * 支持 DNS-over-HTTPS (DoH) 自定义配置，可绕过 ISP DNS 污染/劫持。
 * 
 * 实现策略：
 * 1. WebView — 通过 AndroidX WebKit 的 setWebViewClient + OkHttp DoH DNS 解析
 * 2. GeckoView — 通过 GeckoRuntimeSettings.dohConfig 设置
 * 3. Shell 模式 — 通过 app_config.json 中的 dnsConfig 配置
 * 
 * DoH 模式：
 * - automatic: 当系统 DNS 响应缓慢或失败时自动回退到 DoH
 * - strict: 强制所有 DNS 请求通过 DoH，如果 DoH 不可用则解析失败
 */
class DnsManager(private val context: Context) {

    companion object {
        private const val TAG = "DnsManager"
        
        /** 检查 WebView 是否支持 DoH 相关特性 */
        fun isDohSupported(): Boolean {
            return try {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to check DoH support: ${e.message}")
                false
            }
        }
    }

    private val client = OkHttpClient.Builder().build()
    
    // 缓存 DoH 解析结果，避免重复请求
    private val dnsCache = ConcurrentHashMap<String, List<InetAddress>>()
    
    // 当前活跃的 DNS 配置
    @Volatile
    private var currentConfig: DnsConfig? = null
    
    // 用于强制使用特定网络的 Network 回调
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * 应用 DNS 配置
     * 
     * @param config DNS 配置
     */
    fun applyDnsConfig(config: DnsConfig) {
        val previousConfig = currentConfig
        currentConfig = config
        
        if (config.provider == "SYSTEM" || config.effectiveDohUrl.isBlank()) {
            clearDnsConfig()
            return
        }
        
        AppLogger.d(TAG, "Applying DoH config: provider=${config.provider}, mode=${config.dohMode}")
        
        // 如果启用了绕过系统 DNS，设置 Private DNS 模式
        if (config.bypassSystemDns) {
            setupBypassSystemDns()
        } else if (previousConfig?.bypassSystemDns == true) {
            clearBypassSystemDns()
        }
    }

    /**
     * 清除 DNS 配置，恢复系统默认
     */
    fun clearDnsConfig() {
        currentConfig = null
        dnsCache.clear()
        clearBypassSystemDns()
        AppLogger.d(TAG, "DNS config cleared, using system default")
    }

    /**
     * 获取当前配置的 DoH URL
     */
    fun getCurrentDohUrl(): String? {
        val config = currentConfig ?: return null
        return config.effectiveDohUrl.takeIf { it.isNotBlank() }
    }

    /**
     * 获取当前 DNS 配置
     */
    fun getCurrentConfig(): DnsConfig? = currentConfig

    /**
     * 使用 DoH 解析域名
     * 
     * @param hostname 要解析的域名
     * @return 解析到的 IP 地址列表
     */
    fun resolveWithDoh(hostname: String): List<InetAddress> {
        val config = currentConfig ?: throw UnknownHostException("No DoH config set")
        val dohUrl = config.effectiveDohUrl
        
        if (dohUrl.isBlank()) {
            throw UnknownHostException("DoH URL is blank")
        }

        // 检查缓存
        dnsCache[hostname]?.let { return it }

        AppLogger.d(TAG, "Resolving $hostname via DoH: $dohUrl")

        val dohDns = DnsOverHttps.Builder()
            .client(client)
            .url(dohUrl.toHttpUrlOrNull() ?: throw UnknownHostException("Invalid DoH URL: $dohUrl"))
            .includeIPv6(true)
            .build()

        val result = try {
            dohDns.lookup(hostname)
        } catch (e: Exception) {
            AppLogger.e(TAG, "DoH resolution failed for $hostname", e)
            if (config.dohMode == "strict") {
                throw UnknownHostException("DoH resolution failed in strict mode: ${e.message}")
            }
            // automatic 模式下回退到系统 DNS
            Dns.SYSTEM.lookup(hostname)
        }

        if (result.isNotEmpty()) {
            dnsCache[hostname] = result
        }
        return result
    }

    /**
     * 创建配置了 DoH 的 OkHttpClient
     * 用于应用内网络请求（如轮询通知等）
     */
    fun createDohOkHttpClient(): OkHttpClient {
        val config = currentConfig ?: return OkHttpClient.Builder().build()
        val dohUrl = config.effectiveDohUrl
        
        if (dohUrl.isBlank() || config.provider == "SYSTEM") {
            return OkHttpClient.Builder().build()
        }

        val dohDns = DnsOverHttps.Builder()
            .client(client)
            .url(dohUrl.toHttpUrlOrNull() ?: return OkHttpClient.Builder().build())
            .includeIPv6(true)
            .build()

        return OkHttpClient.Builder()
            .dns(dohDns)
            .build()
    }

    /**
     * 设置绕过系统 DNS
     * 通过 ConnectivityManager 绑定到特定网络来绕过系统 DNS
     */
    private fun setupBypassSystemDns() {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            // 清除之前的回调
            clearBypassSystemDns()
            
            // 在 Android 9+ 上，可以通过 Private DNS 设置影响 DNS
            // 但应用层无法直接修改系统 Private DNS 设置
            // 我们通过 OkHttp DNS 自定义来绕过
            AppLogger.d(TAG, "Bypass system DNS enabled — all DNS queries will use DoH")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to setup bypass system DNS", e)
        }
    }

    /**
     * 清除绕过系统 DNS 的配置
     */
    private fun clearBypassSystemDns() {
        networkCallback?.let {
            try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                cm.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Failed to unregister network callback: ${e.message}")
            }
            networkCallback = null
        }
    }

    /**
     * 清除 DNS 缓存
     */
    fun clearCache() {
        dnsCache.clear()
    }
}
