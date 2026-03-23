package com.webtoapp.core.announcement

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.webtoapp.data.model.Announcement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * 公告管理器
 */
private val Context.announcementDataStore: DataStore<Preferences> by preferencesDataStore(name = "announcement")

class AnnouncementManager(private val context: Context) {
    
    // Network状态
    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable
    
    // Network状态回调
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    init {
        checkNetworkStatus()
    }
    
    /**
     * 检查当前网络状态
     */
    private fun checkNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        _isNetworkAvailable.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
    
    /**
     * 开始监听网络状态变化
     */
    fun startNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isNetworkAvailable.value = true
            }
            
            override fun onLost(network: Network) {
                _isNetworkAvailable.value = false
            }
            
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _isNetworkAvailable.value = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 停止网络监听
     */
    fun stopNetworkMonitoring() {
        networkCallback?.let {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        networkCallback = null
    }

    /**
     * 检查是否需要显示公告（启动时触发）
     * @param appId 应用ID
     * @param announcement 公告配置
     * @return 是否需要显示
     */
    suspend fun shouldShowAnnouncement(appId: Long, announcement: Announcement?): Boolean {
        return shouldShowAnnouncementForTrigger(appId, announcement, isLaunch = true, isNoNetwork = false)
    }
    
    /**
     * 检查是否需要显示公告（根据触发条件）
     * @param appId 应用ID
     * @param announcement 公告配置
     * @param isLaunch 是否为启动触发
     * @param isNoNetwork 是否为无网络触发
     * @param isInterval 是否为定时间隔触发
     * @return 是否需要显示
     */
    suspend fun shouldShowAnnouncementForTrigger(
        appId: Long, 
        announcement: Announcement?,
        isLaunch: Boolean = false,
        isNoNetwork: Boolean = false,
        isInterval: Boolean = false
    ): Boolean {
        if (announcement == null || !announcement.enabled) {
            return false
        }

        if (announcement.title.isBlank() && announcement.content.isBlank()) {
            return false
        }

        // Check用户是否选择了“不再显示”
        if (isNeverShow(appId)) {
            return false
        }
        
        // Check触发条件
        val shouldTrigger = when {
            isLaunch -> announcement.triggerOnLaunch
            isNoNetwork -> announcement.triggerOnNoNetwork
            isInterval -> announcement.triggerIntervalMinutes > 0
            else -> false
        }
        
        if (!shouldTrigger) {
            return false
        }

        // 如果设置为仅显示一次，检查是否已显示过
        if (announcement.showOnce) {
            val shownVersion = getShownVersion(appId)
            return shownVersion < announcement.version
        }

        return true
    }
    
    /**
     * 检查是否需要触发定时公告
     * @param appId 应用ID
     * @param announcement 公告配置
     * @return 是否需要触发
     */
    suspend fun shouldTriggerIntervalAnnouncement(appId: Long, announcement: Announcement?): Boolean {
        if (announcement == null || !announcement.enabled) {
            return false
        }
        
        val intervalMinutes = announcement.triggerIntervalMinutes
        if (intervalMinutes <= 0) {
            return false
        }
        
        // Check用户是否选择了“不再显示”
        if (isNeverShow(appId)) {
            return false
        }
        
        val lastTriggerTime = getLastIntervalTriggerTime(appId)
        val currentTime = System.currentTimeMillis()
        val intervalMs = intervalMinutes * 60 * 1000L
        
        return (currentTime - lastTriggerTime) >= intervalMs
    }
    
    /**
     * 获取上次定时触发时间
     */
    private suspend fun getLastIntervalTriggerTime(appId: Long): Long {
        return context.announcementDataStore.data.first()[
            longPreferencesKey("announcement_interval_trigger_$appId")
        ] ?: 0L
    }
    
    /**
     * 记录定时触发时间
     */
    suspend fun markIntervalTrigger(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences[longPreferencesKey("announcement_interval_trigger_$appId")] = System.currentTimeMillis()
        }
    }
    
    /**
     * 重置定时触发状态（启动时调用）
     */
    suspend fun resetIntervalTrigger(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences.remove(longPreferencesKey("announcement_interval_trigger_$appId"))
        }
    }
    
    /**
     * 检查用户是否选择了“不再显示”
     */
    private suspend fun isNeverShow(appId: Long): Boolean {
        return context.announcementDataStore.data.first()[
            booleanPreferencesKey("announcement_never_show_$appId")
        ] ?: false
    }
    
    /**
     * 标记“不再显示”
     */
    suspend fun markNeverShow(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences[booleanPreferencesKey("announcement_never_show_$appId")] = true
        }
    }

    /**
     * 获取已显示的公告版本
     */
    private suspend fun getShownVersion(appId: Long): Int {
        return context.announcementDataStore.data.first()[
            intPreferencesKey("announcement_shown_$appId")
        ] ?: 0
    }

    /**
     * 标记公告已显示
     */
    suspend fun markAnnouncementShown(appId: Long, version: Int) {
        context.announcementDataStore.edit { preferences ->
            preferences[intPreferencesKey("announcement_shown_$appId")] = version
        }
    }

    /**
     * 重置公告显示状态（用于测试或重新显示）
     */
    suspend fun resetAnnouncementStatus(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences.remove(intPreferencesKey("announcement_shown_$appId"))
        }
    }

    /**
     * 创建公告配置
     */
    fun createAnnouncement(
        title: String,
        content: String,
        linkUrl: String? = null,
        linkText: String? = null,
        showOnce: Boolean = true
    ): Announcement {
        return Announcement(
            title = title,
            content = content,
            linkUrl = linkUrl,
            linkText = linkText,
            showOnce = showOnce,
            enabled = true,
            version = System.currentTimeMillis().toInt()
        )
    }
}
