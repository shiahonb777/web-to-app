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
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.Announcement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first




private val Context.announcementDataStore: DataStore<Preferences> by preferencesDataStore(name = "announcement")

class AnnouncementManager(private val context: Context) {

    companion object {
        private const val TAG = "AnnouncementManager"
    }


    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable


    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        checkNetworkStatus()
    }




    private fun checkNetworkStatus() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        _isNetworkAvailable.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }




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
            AppLogger.e(TAG, "Operation failed", e)
        }
    }




    fun stopNetworkMonitoring() {
        networkCallback?.let {
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                AppLogger.e(TAG, "Operation failed", e)
            }
        }
        networkCallback = null
    }







    suspend fun shouldShowAnnouncement(appId: Long, announcement: Announcement?): Boolean {
        return shouldShowAnnouncementForTrigger(appId, announcement, isLaunch = true, isNoNetwork = false)
    }










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


        if (isNeverShow(appId)) {
            return false
        }


        val shouldTrigger = when {
            isLaunch -> announcement.triggerOnLaunch
            isNoNetwork -> announcement.triggerOnNoNetwork
            isInterval -> announcement.triggerIntervalMinutes > 0
            else -> false
        }

        if (!shouldTrigger) {
            return false
        }


        if (announcement.showOnce) {
            val shownVersion = getShownVersion(appId)
            return shownVersion < announcement.version
        }

        return true
    }







    suspend fun shouldTriggerIntervalAnnouncement(appId: Long, announcement: Announcement?): Boolean {
        if (announcement == null || !announcement.enabled) {
            return false
        }

        val intervalMinutes = announcement.triggerIntervalMinutes
        if (intervalMinutes <= 0) {
            return false
        }


        if (isNeverShow(appId)) {
            return false
        }

        val lastTriggerTime = getLastIntervalTriggerTime(appId)
        val currentTime = System.currentTimeMillis()
        val intervalMs = intervalMinutes * 60 * 1000L

        return (currentTime - lastTriggerTime) >= intervalMs
    }

    suspend fun getMillisUntilNextIntervalAnnouncement(appId: Long, announcement: Announcement?): Long {
        if (announcement == null || !announcement.enabled) {
            return Long.MAX_VALUE
        }

        val intervalMinutes = announcement.triggerIntervalMinutes
        if (intervalMinutes <= 0) {
            return Long.MAX_VALUE
        }

        val lastTriggerTime = getLastIntervalTriggerTime(appId)
        val intervalMs = intervalMinutes * 60 * 1000L
        return (lastTriggerTime + intervalMs - System.currentTimeMillis()).coerceAtLeast(0L)
    }




    private suspend fun getLastIntervalTriggerTime(appId: Long): Long {
        return context.announcementDataStore.data.first()[
            longPreferencesKey("announcement_interval_trigger_$appId")
        ] ?: 0L
    }




    suspend fun markIntervalTrigger(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences[longPreferencesKey("announcement_interval_trigger_$appId")] = System.currentTimeMillis()
        }
    }




    suspend fun resetIntervalTrigger(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences.remove(longPreferencesKey("announcement_interval_trigger_$appId"))
        }
    }




    private suspend fun isNeverShow(appId: Long): Boolean {
        return context.announcementDataStore.data.first()[
            booleanPreferencesKey("announcement_never_show_$appId")
        ] ?: false
    }




    suspend fun markNeverShow(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences[booleanPreferencesKey("announcement_never_show_$appId")] = true
        }
    }




    private suspend fun getShownVersion(appId: Long): Int {
        return context.announcementDataStore.data.first()[
            intPreferencesKey("announcement_shown_$appId")
        ] ?: 0
    }




    suspend fun markAnnouncementShown(appId: Long, version: Int) {
        context.announcementDataStore.edit { preferences ->
            preferences[intPreferencesKey("announcement_shown_$appId")] = version
        }
    }




    suspend fun resetAnnouncementStatus(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences.remove(intPreferencesKey("announcement_shown_$appId"))
        }
    }




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
