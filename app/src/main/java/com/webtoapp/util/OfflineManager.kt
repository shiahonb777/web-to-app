package com.webtoapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.webkit.WebSettings
import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 离线模式管理器
 * 监控网络状态，自动切换 WebView 缓存策略
 */
@SuppressLint("StaticFieldLeak")
class OfflineManager(private val context: Context) {
    
    companion object {
        private const val TAG = "OfflineManager"
        
        // Network qualities suitable for large downloads
        private val LARGE_DOWNLOAD_QUALITIES = setOf(
            NetworkQuality.EXCELLENT, NetworkQuality.GOOD
        )
        
        @Volatile
        private var instance: OfflineManager? = null
        
        fun getInstance(context: Context): OfflineManager {
            return instance ?: synchronized(this) {
                instance ?: OfflineManager(context.applicationContext).also { instance = it }
            }
        }
        
        /**
         * 释放单例实例（通常在 Application.onTerminate 或测试时调用）
         */
        fun release() {
            synchronized(this) {
                instance?.unregister()
                instance = null
            }
        }
    }
    
    /**
     * 网络状态
     */
    enum class NetworkState {
        ONLINE,      // 在线
        OFFLINE,     // 离线
        UNKNOWN      // 未知
    }
    
    /**
     * 网络类型
     */
    enum class NetworkType {
        WIFI,
        MOBILE,
        ETHERNET,
        NONE,
        UNKNOWN
    }
    
    /**
     * 网络质量
     */
    enum class NetworkQuality {
        EXCELLENT,  // 优秀
        GOOD,       // 良好
        MODERATE,   // 一般
        POOR,       // 较差
        UNKNOWN     // 未知
    }
    
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    private val _networkQuality = MutableStateFlow(NetworkQuality.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _networkType = MutableStateFlow(NetworkType.UNKNOWN)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()
    
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    init {
        // Initialize时检查网络状态
        updateNetworkState()
        // 注册网络监听
        registerNetworkCallback()
    }
    
    /**
     * 检查网络是否可用
     */
    fun isNetworkAvailable(): Boolean {
        return _networkState.value == NetworkState.ONLINE
    }
    
    /**
     * 获取当前网络类型
     */
    fun getCurrentNetworkType(): NetworkType {
        return _networkType.value
    }
    
    /**
     * 更新网络状态
     */
    private fun updateNetworkState() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            _networkState.value = NetworkState.ONLINE
            _networkType.value = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }
        } else {
            _networkState.value = NetworkState.OFFLINE
            _networkType.value = NetworkType.NONE
        }
    }
    
    /**
     * 注册网络状态监听
     */
    private fun registerNetworkCallback() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _networkState.value = NetworkState.ONLINE
                updateNetworkState()
                AppLogger.d(TAG, "网络已连接")
            }
            
            override fun onLost(network: Network) {
                _networkState.value = NetworkState.OFFLINE
                _networkType.value = NetworkType.NONE
                AppLogger.d(TAG, "网络已断开")
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                updateNetworkState()
                updateNetworkQuality(capabilities)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            AppLogger.e(TAG, "注册网络监听失败", e)
        }
    }
    
    /**
     * 取消网络监听
     */
    fun unregister() {
        networkCallback?.let { callback ->
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (e: Exception) {
                AppLogger.e(TAG, "取消网络监听失败", e)
            }
        }
        networkCallback = null
    }
    
    /**
     * 更新网络质量评估
     */
    private fun updateNetworkQuality(capabilities: NetworkCapabilities) {
        val downstreamBandwidth = capabilities.linkDownstreamBandwidthKbps
        val upstreamBandwidth = capabilities.linkUpstreamBandwidthKbps

        _networkQuality.value = when {
            downstreamBandwidth >= 10000 && upstreamBandwidth >= 5000 -> NetworkQuality.EXCELLENT
            downstreamBandwidth >= 5000 && upstreamBandwidth >= 1000 -> NetworkQuality.GOOD
            downstreamBandwidth >= 1000 && upstreamBandwidth >= 500 -> NetworkQuality.MODERATE
            downstreamBandwidth > 0 -> NetworkQuality.POOR
            else -> NetworkQuality.UNKNOWN
        }

        AppLogger.d(TAG, "网络质量: ${_networkQuality.value}, 下行: ${downstreamBandwidth}kbps, 上行: ${upstreamBandwidth}kbps")
    }
    
    /**
     * 检查网络质量是否适合大文件下载
     */
    fun isSuitableForLargeDownload(): Boolean {
        return _networkQuality.value in LARGE_DOWNLOAD_QUALITIES &&
               _networkType.value == NetworkType.WIFI
    }
    
    /**
     * 配置 WebView 的离线模式
     * 根据网络状态自动调整缓存策略
     */
    fun configureWebViewForOffline(webView: WebView, forceOffline: Boolean = false) {
        val settings = webView.settings
        
        if (forceOffline || !isNetworkAvailable()) {
            // 离线模式：优先使用缓存
            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            AppLogger.d(TAG, "WebView 已切换到离线模式")
        } else {
            // 在线模式：正常加载
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            AppLogger.d(TAG, "WebView 已切换到在线模式")
        }
    }
    
    /**
     * 设置 WebView 缓存策略
     */
    fun setWebViewCacheMode(webView: WebView, cacheMode: CacheMode) {
        webView.settings.cacheMode = when (cacheMode) {
            CacheMode.DEFAULT -> WebSettings.LOAD_DEFAULT
            CacheMode.CACHE_ELSE_NETWORK -> WebSettings.LOAD_CACHE_ELSE_NETWORK
            CacheMode.NO_CACHE -> WebSettings.LOAD_NO_CACHE
            CacheMode.CACHE_ONLY -> WebSettings.LOAD_CACHE_ONLY
        }
    }
    
    /**
     * 缓存模式
     */
    enum class CacheMode {
        DEFAULT,            // Default：根据 HTTP 头决定
        CACHE_ELSE_NETWORK, // 优先缓存，无缓存时使用网络
        NO_CACHE,           // 不使用缓存
        CACHE_ONLY          // 仅使用缓存
    }
}
