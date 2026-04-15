package com.webtoapp.core.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.JsonParser
import com.webtoapp.util.GsonProvider
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.*
import com.webtoapp.data.model.AiFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_config")

/**
 * AI 配置管理器
 * 管理 API Keys、模型配置等
 */
class AiConfigManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AiConfigManager"
        private val KEY_API_KEYS = stringPreferencesKey("api_keys")
        private val KEY_API_KEYS_BACKUP = stringPreferencesKey("api_keys_backup")  // 明文备份，防止 KeyStore 密钥丢失
        private val KEY_SAVED_MODELS = stringPreferencesKey("saved_models")
        private val KEY_DEFAULT_MODEL = stringPreferencesKey("default_model")
        
        // Gson 单例
        private val gson get() = GsonProvider.gson

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "webtoapp_ai_api_keys"
        private const val ENCRYPTED_PREFIX = "enc_v1:"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val GCM_IV_BYTES = 12
    }
    
    // API Keys Flow
    val apiKeysFlow: Flow<List<ApiKeyConfig>> = context.aiConfigDataStore.data.map { prefs ->
        // 优先从加密存储读取
        val stored = prefs[KEY_API_KEYS]
        if (stored != null) {
            val json = decodeSensitiveJson(stored)
            if (json != null) {
                val result = parseApiKeyConfigs(json)
                if (result != null) return@map result
                AppLogger.e(TAG, "Failed to parse API keys from primary")
            }
        }
        // 主存储解密/解析失败，尝试明文备份
        val backup = prefs[KEY_API_KEYS_BACKUP]
        if (backup != null) {
            val result = parseApiKeyConfigs(backup)
            if (result != null && result.isNotEmpty()) {
                AppLogger.w(TAG, "apiKeysFlow: recovered ${result.size} keys from backup")
                return@map result
            }
            if (result == null) {
                AppLogger.e(TAG, "Failed to parse API keys from backup")
            }
        }
        emptyList()
    }
    
    // Saved的模型 Flow
    val savedModelsFlow: Flow<List<SavedModel>> = context.aiConfigDataStore.data.map { prefs ->
        val json = prefs[KEY_SAVED_MODELS] ?: "[]"
        val result = parseSavedModels(json)
        if (result != null) {
            result
        } else {
            AppLogger.e(TAG, "Failed to parse saved models JSON")
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
    suspend fun addApiKey(config: ApiKeyConfig): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getApiKeys(prefs)
                if (current == null) {
                    // 解密失败，不覆盖现有数据，避免误删
                    AppLogger.e(TAG, "Cannot add API key: failed to decrypt existing keys, aborting to prevent data loss")
                    throw IllegalStateException("Decrypt failure, aborting write")
                }
                val updated = current + config
                val jsonStr = gson.toJson(updated)
                prefs[KEY_API_KEYS] = encodeSensitiveJson(jsonStr)
                prefs[KEY_API_KEYS_BACKUP] = jsonStr  // 明文备份
                AppLogger.d(TAG, "API key added: ${config.provider.name}, total: ${updated.size}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to add API key", e)
            false
        }
    }
    
    /**
     * 更新 API Key
     */
    suspend fun updateApiKey(config: ApiKeyConfig): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getApiKeys(prefs)
                if (current == null) {
                    AppLogger.e(TAG, "Cannot update API key: failed to decrypt existing keys, aborting")
                    throw IllegalStateException("Decrypt failure, aborting write")
                }
                val updated = current.map { if (it.id == config.id) config else it }
                val jsonStr = gson.toJson(updated)
                prefs[KEY_API_KEYS] = encodeSensitiveJson(jsonStr)
                prefs[KEY_API_KEYS_BACKUP] = jsonStr
                AppLogger.d(TAG, "API key updated: ${config.provider.name}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to update API key", e)
            false
        }
    }
    
    /**
     * 删除 API Key
     */
    suspend fun deleteApiKey(id: String): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getApiKeys(prefs)
                if (current == null) {
                    AppLogger.e(TAG, "Cannot delete API key: failed to decrypt existing keys, aborting")
                    throw IllegalStateException("Decrypt failure, aborting write")
                }
                val updated = current.filter { it.id != id }
                val jsonStr = gson.toJson(updated)
                prefs[KEY_API_KEYS] = encodeSensitiveJson(jsonStr)
                prefs[KEY_API_KEYS_BACKUP] = jsonStr
                AppLogger.d(TAG, "API key deleted, remaining: ${updated.size}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete API key", e)
            false
        }
    }
    
    /**
     * 添加已保存的模型
     */
    suspend fun saveModel(model: SavedModel): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getSavedModels(prefs)
                val updated = current + model
                prefs[KEY_SAVED_MODELS] = gson.toJson(updated)
                AppLogger.d(TAG, "Model saved: ${model.model.name}, total: ${updated.size}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to save model", e)
            false
        }
    }
    
    /**
     * 更新已保存的模型
     */
    suspend fun updateSavedModel(model: SavedModel): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getSavedModels(prefs)
                val updated = current.map { if (it.id == model.id) model else it }
                prefs[KEY_SAVED_MODELS] = gson.toJson(updated)
                AppLogger.d(TAG, "Model updated: ${model.model.name}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to update model", e)
            false
        }
    }
    
    /**
     * 删除已保存的模型
     */
    suspend fun deleteSavedModel(id: String): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getSavedModels(prefs)
                val updated = current.filter { it.id != id }
                prefs[KEY_SAVED_MODELS] = gson.toJson(updated)
                AppLogger.d(TAG, "Model deleted, remaining: ${updated.size}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete model", e)
            false
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
        // Use data.first() for reads instead of edit{} which acquires a write lock unnecessarily
        val prefs = context.aiConfigDataStore.data.first()
        return (getApiKeys(prefs) ?: emptyList()).find { it.id == id }
    }
    
    /**
     * 获取指定已保存的模型
     */
    suspend fun getSavedModelById(id: String): SavedModel? {
        val prefs = context.aiConfigDataStore.data.first()
        return getSavedModels(prefs).find { it.id == id }
    }
    
    // 辅助方法
    // 返回 null 表示解密失败（区别于空列表），用于写入操作中防止误覆盖
    private fun getApiKeys(prefs: Preferences): List<ApiKeyConfig>? {
        val stored = prefs[KEY_API_KEYS] ?: return emptyList()
        val json = decodeSensitiveJson(stored)
        if (json != null) {
            val result = parseApiKeyConfigs(json)
            if (result != null) {
                return result
            }
            AppLogger.e(TAG, "Failed to deserialize API keys from primary")
        }
        // 主存储解密/解析失败，尝试从备份恢复
        val backup = prefs[KEY_API_KEYS_BACKUP]
        if (backup != null) {
            val result = parseApiKeyConfigs(backup)
            if (result != null && result.isNotEmpty()) {
                AppLogger.w(TAG, "Recovered ${result.size} API keys from backup")
                return result
            }
            if (result == null) {
                AppLogger.e(TAG, "Failed to deserialize API keys from backup")
            }
        }
        // 两个都失败了
        if (json == null) {
            AppLogger.e(TAG, "Failed to decode API keys from both primary and backup")
            return null  // 返回 null 表示解密失败
        }
        return emptyList()
    }
    
    private fun getSavedModels(prefs: Preferences): List<SavedModel> {
        val json = prefs[KEY_SAVED_MODELS] ?: return emptyList()
        return parseSavedModels(json) ?: run {
            AppLogger.e(TAG, "Failed to deserialize saved models")
            emptyList()
        }
    }

    private fun encodeSensitiveJson(plainJson: String): String {
        val encrypted = try {
            encrypt(plainJson)
        } catch (e: Exception) {
            AppLogger.w(TAG, "Encryption failed, saving as plain JSON", e)
            null
        }
        return if (encrypted != null) "$ENCRYPTED_PREFIX$encrypted" else plainJson
    }

    private fun decodeSensitiveJson(stored: String): String? {
        if (!stored.startsWith(ENCRYPTED_PREFIX)) return stored
        val payload = stored.removePrefix(ENCRYPTED_PREFIX)
        return try {
            decrypt(payload)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Decryption failed, attempting plain JSON fallback", e)
            // 如果解密失败，尝试当作纯 JSON 解析（防止加密密钥失效导致数据丢失）
            try {
                gson.fromJson<List<*>>(payload, List::class.java)
                // 如果能解析为 JSON，说明是未加密的
                payload
            } catch (_: Exception) {
                null
            }
        }
    }

    internal fun parseApiKeyConfigs(json: String): List<ApiKeyConfig>? {
        return parseJsonArray(json, ApiKeyConfig::class.java)
    }

    internal fun parseSavedModels(json: String): List<SavedModel>? {
        return parseJsonArray(json, SavedModel::class.java)
    }

    private fun <T> parseJsonArray(json: String, clazz: Class<T>): List<T>? {
        return try {
            val parsed = JsonParser.parseString(json)
            if (parsed.isJsonNull) return emptyList()
            if (!parsed.isJsonArray) return null

            val jsonArray = parsed.asJsonArray
            val result = jsonArray.mapNotNull { element ->
                try {
                    gson.fromJson(element, clazz)
                } catch (_: Exception) {
                    null
                }
            }

            if (jsonArray.size() > 0 && result.isEmpty()) null else result
        } catch (_: Exception) {
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(base64CipherText: String): String {
        val decoded = Base64.decode(base64CipherText, Base64.NO_WRAP)
        if (decoded.size <= GCM_IV_BYTES) return ""

        val iv = decoded.copyOfRange(0, GCM_IV_BYTES)
        val encrypted = decoded.copyOfRange(GCM_IV_BYTES, decoded.size)

        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(GCM_TAG_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        val plainBytes = cipher.doFinal(encrypted)
        return String(plainBytes, Charsets.UTF_8)
    }
}

