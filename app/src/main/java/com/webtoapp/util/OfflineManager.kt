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
 * Note.
 * ， WebView
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
         * （ Application.onTerminate ）
         */
        fun release() {
            synchronized(this) {
                instance?.unregister()
                instance = null
            }
        }
    }
    
    /**
     * Note.
     */
    enum class NetworkState {
        ONLINE,      // Note.
        OFFLINE,     // Note.
        UNKNOWN      // Note.
    }
    
    /**
     * Note.
     */
    enum class NetworkType {
        WIFI,
        MOBILE,
        ETHERNET,
        NONE,
        UNKNOWN
    }
    
    /**
     * Note.
     */
    enum class NetworkQuality {
        EXCELLENT,  // Note.
        GOOD,       // Note.
        MODERATE,   // Note.
        POOR,       // Note.
        UNKNOWN     // Note.
    }
    
    private val _networkState = MutableStateFlow(NetworkState.UNKNOWN)
    private val _networkQuality = MutableStateFlow(NetworkQuality.UNKNOWN)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()
    
    private val _networkType = MutableStateFlow(NetworkType.UNKNOWN)
    val networkType: StateFlow<NetworkType> = _networkType.asStateFlow()
    
    val networkQuality: StateFlow<NetworkQuality> = _networkQuality.asStateFlow()
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    init {
        // Initialize
        updateNetworkState()
        // Note.
        registerNetworkCallback()
    }
    
    /**
     * Note.
     */
    fun isNetworkAvailable(): Boolean {
        return _networkState.value == NetworkState.ONLINE
    }
    
    /**
     * Note.
     */
    fun getCurrentNetworkType(): NetworkType {
        return _networkType.value
    }
    
    /**
     * Note.
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
     * Note.
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
     * Note.
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
     * Note.
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
     * Note.
     */
    fun isSuitableForLargeDownload(): Boolean {
        return _networkQuality.value in LARGE_DOWNLOAD_QUALITIES &&
               _networkType.value == NetworkType.WIFI
    }
    
    /**
     * WebView
     * Note.
     */
    fun configureWebViewForOffline(webView: WebView, forceOffline: Boolean = false) {
        val settings = webView.settings
        
        if (forceOffline || !isNetworkAvailable()) {
            // ：
            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            AppLogger.d(TAG, "WebView 已切换到离线模式")
        } else {
            // ：
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            AppLogger.d(TAG, "WebView 已切换到在线模式")
        }
    }
    
    /**
     * WebView
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
     * Note.
     */
    enum class CacheMode {
        DEFAULT,            // Default： HTTP
        CACHE_ELSE_NETWORK, // ，
        NO_CACHE,           // Note.
        CACHE_ONLY          // Note.
    }
}
