package com.webtoapp.core.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.*
import com.webtoapp.data.model.AiFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type

private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_config")

/**
 * AI 配置管理器
 * 管理 API Keys、模型配置等
 */
class AiConfigManager(private val context: Context) {
    
    companion object {
        private val KEY_API_KEYS = stringPreferencesKey("api_keys")
        private val KEY_SAVED_MODELS = stringPreferencesKey("saved_models")
        private val KEY_DEFAULT_MODEL = stringPreferencesKey("default_model")
        
        // Gson 单例
        private val gson: Gson by lazy { Gson() }
        
        // TypeToken 缓存
        private val apiKeyListType: Type by lazy {
            object : TypeToken<List<ApiKeyConfig>>() {}.type
        }
        
        private val savedModelListType: Type by lazy {
            object : TypeToken<List<SavedModel>>() {}.type
        }
    }
    
    // API Keys Flow
    val apiKeysFlow: Flow<List<ApiKeyConfig>> = context.aiConfigDataStore.data.map { prefs ->
        val json = prefs[KEY_API_KEYS] ?: "[]"
        try {
            gson.fromJson(json, apiKeyListType)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Saved的模型 Flow
    val savedModelsFlow: Flow<List<SavedModel>> = context.aiConfigDataStore.data.map { prefs ->
        val json = prefs[KEY_SAVED_MODELS] ?: "[]"
        try {
            gson.fromJson(json, savedModelListType)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // Default模型 ID Flow
    val defaultModelIdFlow: Flow<String?> = context.aiConfigDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_MODEL]
    }
    
    /**
     * 添加 API Key
     */
    suspend fun addApiKey(config: ApiKeyConfig) {
        context.aiConfigDataStore.edit { prefs ->
            val current = getApiKeys(prefs)
            val updated = current + config
            prefs[KEY_API_KEYS] = gson.toJson(updated)
        }
    }
    
    /**
     * 更新 API Key
     */
    suspend fun updateApiKey(config: ApiKeyConfig) {
        context.aiConfigDataStore.edit { prefs ->
            val current = getApiKeys(prefs)
            val updated = current.map { if (it.id == config.id) config else it }
            prefs[KEY_API_KEYS] = gson.toJson(updated)
        }
    }
    
    /**
     * 删除 API Key
     */
    suspend fun deleteApiKey(id: String) {
        context.aiConfigDataStore.edit { prefs ->
            val current = getApiKeys(prefs)
            val updated = current.filter { it.id != id }
            prefs[KEY_API_KEYS] = gson.toJson(updated)
        }
    }
    
    /**
     * 添加已保存的模型
     */
    suspend fun saveModel(model: SavedModel) {
        context.aiConfigDataStore.edit { prefs ->
            val current = getSavedModels(prefs)
            val updated = current + model
            prefs[KEY_SAVED_MODELS] = gson.toJson(updated)
        }
    }
    
    /**
     * 更新已保存的模型
     */
    suspend fun updateSavedModel(model: SavedModel) {
        context.aiConfigDataStore.edit { prefs ->
            val current = getSavedModels(prefs)
            val updated = current.map { if (it.id == model.id) model else it }
            prefs[KEY_SAVED_MODELS] = gson.toJson(updated)
        }
    }
    
    /**
     * 删除已保存的模型
     */
    suspend fun deleteSavedModel(id: String) {
        context.aiConfigDataStore.edit { prefs ->
            val current = getSavedModels(prefs)
            val updated = current.filter { it.id != id }
            prefs[KEY_SAVED_MODELS] = gson.toJson(updated)
        }
    }
    
    /**
     * 设置默认模型
     */
    suspend fun setDefaultModel(modelId: String?) {
        context.aiConfigDataStore.edit { prefs ->
            if (modelId != null) {
                prefs[KEY_DEFAULT_MODEL] = modelId
            } else {
                prefs.remove(KEY_DEFAULT_MODEL)
            }
        }
    }
    
    /**
     * 根据能力筛选模型
     */
    suspend fun getModelsByCapability(capability: ModelCapability): Flow<List<SavedModel>> {
        return savedModelsFlow.map { models ->
            models.filter { it.capabilities.contains(capability) }
        }
    }
    
    /**
     * 根据功能场景筛选模型
     */
    fun getModelsByFeature(feature: AiFeature): Flow<List<SavedModel>> {
        return savedModelsFlow.map { models ->
            models.filter { it.supportsFeature(feature) }
        }
    }
    
    /**
     * 获取指定功能的默认模型
     */
    fun getDefaultModelForFeature(feature: AiFeature): Flow<SavedModel?> {
        return savedModelsFlow.map { models ->
            // 优先返回标记为默认且支持该功能的模型
            models.find { it.isDefault && it.supportsFeature(feature) }
                ?: models.find { it.supportsFeature(feature) }
        }
    }
    
    /**
     * 获取指定 API Key
     */
    suspend fun getApiKeyById(id: String): ApiKeyConfig? {
        var result: ApiKeyConfig? = null
        context.aiConfigDataStore.edit { prefs ->
            result = getApiKeys(prefs).find { it.id == id }
        }
        return result
    }
    
    /**
     * 获取指定已保存的模型
     */
    suspend fun getSavedModelById(id: String): SavedModel? {
        var result: SavedModel? = null
        context.aiConfigDataStore.edit { prefs ->
            result = getSavedModels(prefs).find { it.id == id }
        }
        return result
    }
    
    // 辅助方法
    private fun getApiKeys(prefs: Preferences): List<ApiKeyConfig> {
        val json = prefs[KEY_API_KEYS] ?: "[]"
        return try {
            gson.fromJson(json, apiKeyListType)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getSavedModels(prefs: Preferences): List<SavedModel> {
        val json = prefs[KEY_SAVED_MODELS] ?: "[]"
        return try {
            gson.fromJson(json, savedModelListType)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
