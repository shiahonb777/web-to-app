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
import com.google.gson.reflect.TypeToken
import com.webtoapp.util.GsonProvider
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.*
import com.webtoapp.data.model.AiFeature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.aiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_config")

/**
 * AI configuration manager
 * Handles API keys, saved models, and related settings
 */
class AiConfigManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AiConfigManager"
        private val KEY_API_KEYS = stringPreferencesKey("api_keys")
        private val KEY_SAVED_MODELS = stringPreferencesKey("saved_models")
        private val KEY_DEFAULT_MODEL = stringPreferencesKey("default_model")
        
        // Gson singleton
        private val gson get() = GsonProvider.gson
        
        // Cached TypeToken
        private val apiKeyListType: Type by lazy {
            object : TypeToken<List<ApiKeyConfig>>() {}.type
        }
        
        private val savedModelListType: Type by lazy {
            object : TypeToken<List<SavedModel>>() {}.type
        }

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "webtoapp_ai_api_keys"
        private const val ENCRYPTED_PREFIX = "enc_v1:"
        private const val AES_MODE = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val GCM_IV_BYTES = 12
    }
    
    // API Keys Flow
    val apiKeysFlow: Flow<List<ApiKeyConfig>> = context.aiConfigDataStore.data.map { prefs ->
        val stored = prefs[KEY_API_KEYS] ?: "[]"
        val json = decodeSensitiveJson(stored) ?: "[]"
        try {
            val result: List<ApiKeyConfig> = gson.fromJson(json, apiKeyListType)
            result ?: emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse API keys JSON", e)
            emptyList()
        }
    }
    
    // Flow of saved models
    val savedModelsFlow: Flow<List<SavedModel>> = context.aiConfigDataStore.data.map { prefs ->
        val json = prefs[KEY_SAVED_MODELS] ?: "[]"
        try {
            val result: List<SavedModel> = gson.fromJson(json, savedModelListType)
            result ?: emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to parse saved models JSON", e)
            emptyList()
        }
    }
    
    // Flow for the default model ID
    val defaultModelIdFlow: Flow<String?> = context.aiConfigDataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_MODEL]
    }
    
    /**
     * Add an API key
     */
    suspend fun addApiKey(config: ApiKeyConfig): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getApiKeys(prefs)
                val updated = current + config
                val jsonStr = gson.toJson(updated)
                prefs[KEY_API_KEYS] = encodeSensitiveJson(jsonStr)
                AppLogger.d(TAG, "API key added: ${config.provider.name}, total: ${updated.size}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to add API key", e)
            false
        }
    }
    
    /**
     * Update an API key
     */
    suspend fun updateApiKey(config: ApiKeyConfig): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getApiKeys(prefs)
                val updated = current.map { if (it.id == config.id) config else it }
                prefs[KEY_API_KEYS] = encodeSensitiveJson(gson.toJson(updated))
                AppLogger.d(TAG, "API key updated: ${config.provider.name}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to update API key", e)
            false
        }
    }
    
    /**
     * Delete an API key
     */
    suspend fun deleteApiKey(id: String): Boolean {
        return try {
            context.aiConfigDataStore.edit { prefs ->
                val current = getApiKeys(prefs)
                val updated = current.filter { it.id != id }
                prefs[KEY_API_KEYS] = encodeSensitiveJson(gson.toJson(updated))
                AppLogger.d(TAG, "API key deleted, remaining: ${updated.size}")
            }
            true
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to delete API key", e)
            false
        }
    }
    
    /**
     * Save a model configuration
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
     * Update a saved model
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
     * Delete a saved model entry
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
     * Set the default model
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
     * Filter models by capability
     */
    suspend fun getModelsByCapability(capability: ModelCapability): Flow<List<SavedModel>> {
        return savedModelsFlow.map { models ->
            models.filter { it.capabilities.contains(capability) }
        }
    }
    
    /**
     * Filter models by usage scenario
     */
    fun getModelsByFeature(feature: AiFeature): Flow<List<SavedModel>> {
        return savedModelsFlow.map { models ->
            models.filter { it.supportsFeature(feature) }
        }
    }
    
    /**
     * Get the default model for a capability
     */
    fun getDefaultModelForFeature(feature: AiFeature): Flow<SavedModel?> {
        return savedModelsFlow.map { models ->
            // Prefer models marked as default that support this capability
            models.find { it.isDefault && it.supportsFeature(feature) }
                ?: models.find { it.supportsFeature(feature) }
        }
    }
    
    /**
     * Retrieve an API key by ID
     */
    suspend fun getApiKeyById(id: String): ApiKeyConfig? {
        // Use data.first() for reads instead of edit{} which acquires a write lock unnecessarily
        val prefs = context.aiConfigDataStore.data.first()
        return getApiKeys(prefs).find { it.id == id }
    }
    
    /**
     * Retrieve a saved model by ID
     */
    suspend fun getSavedModelById(id: String): SavedModel? {
        val prefs = context.aiConfigDataStore.data.first()
        return getSavedModels(prefs).find { it.id == id }
    }
    
    // Helper methods
    private fun getApiKeys(prefs: Preferences): List<ApiKeyConfig> {
        val stored = prefs[KEY_API_KEYS] ?: return emptyList()
        val json = decodeSensitiveJson(stored)
        if (json == null) {
            AppLogger.e(TAG, "Failed to decode API keys, data may be corrupted")
            return emptyList()
        }
        return try {
            val result: List<ApiKeyConfig> = gson.fromJson(json, apiKeyListType)
            result ?: emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to deserialize API keys", e)
            emptyList()
        }
    }
    
    private fun getSavedModels(prefs: Preferences): List<SavedModel> {
        val json = prefs[KEY_SAVED_MODELS] ?: return emptyList()
        return try {
            val result: List<SavedModel> = gson.fromJson(json, savedModelListType)
            result ?: emptyList()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to deserialize saved models", e)
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
            // If decryption fails, try parsing as plain JSON to avoid losing data when keys expire
            try {
                gson.fromJson<List<*>>(payload, List::class.java)
                // If parsing works, the stored data was not encrypted
                payload
            } catch (_: Exception) {
                null
            }
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

