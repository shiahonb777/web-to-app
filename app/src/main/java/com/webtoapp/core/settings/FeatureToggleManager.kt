package com.webtoapp.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.featureToggleDataStore by preferencesDataStore(name = "feature_toggles")

/**
 * 功能开关管理器
 * 允许用户禁用不需要的功能，减少界面臃肿感
 */
class FeatureToggleManager(private val context: Context) {

    private val AI_TOOLS = booleanPreferencesKey("feature_ai_tools")
    private val DEV_TOOLS = booleanPreferencesKey("feature_dev_tools")
    private val BROWSER_NETWORK = booleanPreferencesKey("feature_browser_network")
    private val DATA_STATS = booleanPreferencesKey("feature_data_stats")

    val isAiToolsEnabled: Flow<Boolean> = context.featureToggleDataStore.data.map { it[AI_TOOLS] ?: true }
    val isDevToolsEnabled: Flow<Boolean> = context.featureToggleDataStore.data.map { it[DEV_TOOLS] ?: true }
    val isBrowserNetworkEnabled: Flow<Boolean> = context.featureToggleDataStore.data.map { it[BROWSER_NETWORK] ?: true }
    val isDataStatsEnabled: Flow<Boolean> = context.featureToggleDataStore.data.map { it[DATA_STATS] ?: true }

    suspend fun setAiToolsEnabled(enabled: Boolean) {
        context.featureToggleDataStore.edit { it[AI_TOOLS] = enabled }
    }

    suspend fun setDevToolsEnabled(enabled: Boolean) {
        context.featureToggleDataStore.edit { it[DEV_TOOLS] = enabled }
    }

    suspend fun setBrowserNetworkEnabled(enabled: Boolean) {
        context.featureToggleDataStore.edit { it[BROWSER_NETWORK] = enabled }
    }

    suspend fun setDataStatsEnabled(enabled: Boolean) {
        context.featureToggleDataStore.edit { it[DATA_STATS] = enabled }
    }

    companion object {
        @Volatile
        private var instance: FeatureToggleManager? = null

        fun getInstance(context: Context): FeatureToggleManager {
            return instance ?: synchronized(this) {
                instance ?: FeatureToggleManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
