package com.webtoapp.core.activation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

/**
 * 激活码管理器
 */
private val Context.activationDataStore: DataStore<Preferences> by preferencesDataStore(name = "activation")

class ActivationManager(private val context: Context) {

    companion object {
        private const val SALT = "WebToApp_Salt_2024"
    }

    /**
     * 验证激活码
     * @param appId 应用ID
     * @param inputCode 用户输入的激活码
     * @param validCodes 有效激活码列表
     * @return 验证结果
     */
    suspend fun verifyActivationCode(
        appId: Long,
        inputCode: String,
        validCodes: List<String>
    ): ActivationResult {
        if (inputCode.isBlank()) {
            return ActivationResult.Empty
        }

        val normalizedInput = normalizeCode(inputCode)

        // 检查是否匹配任一有效激活码
        val isValid = validCodes.any { validCode ->
            val normalizedValid = normalizeCode(validCode)
            normalizedInput == normalizedValid || 
            hashCode(normalizedInput) == normalizedValid ||
            normalizedInput == hashCode(normalizedValid)
        }

        return if (isValid) {
            saveActivationStatus(appId, true)
            ActivationResult.Success
        } else {
            ActivationResult.Invalid
        }
    }

    /**
     * 检查应用是否已激活
     */
    fun isActivated(appId: Long): Flow<Boolean> {
        return context.activationDataStore.data.map { preferences ->
            preferences[booleanPreferencesKey("activated_$appId")] ?: false
        }
    }

    /**
     * 获取已激活状态（挂起函数）
     */
    suspend fun getActivationStatus(appId: Long): Boolean {
        return isActivated(appId).first()
    }

    /**
     * 保存激活状态
     */
    suspend fun saveActivationStatus(appId: Long, activated: Boolean) {
        context.activationDataStore.edit { preferences ->
            preferences[booleanPreferencesKey("activated_$appId")] = activated
            if (activated) {
                preferences[stringPreferencesKey("activated_time_$appId")] = 
                    System.currentTimeMillis().toString()
            }
        }
    }

    /**
     * 重置激活状态
     */
    suspend fun resetActivation(appId: Long) {
        context.activationDataStore.edit { preferences ->
            preferences.remove(booleanPreferencesKey("activated_$appId"))
            preferences.remove(stringPreferencesKey("activated_time_$appId"))
        }
    }

    /**
     * 生成激活码（用于管理端）
     */
    fun generateActivationCode(seed: String = System.currentTimeMillis().toString()): String {
        val hash = hashCode(seed)
        // 格式化为 XXXX-XXXX-XXXX-XXXX 形式
        return hash.take(16).uppercase().chunked(4).joinToString("-")
    }

    /**
     * 批量生成激活码
     */
    fun generateActivationCodes(count: Int): List<String> {
        return (1..count).map { index ->
            generateActivationCode("${System.currentTimeMillis()}_$index")
        }
    }

    /**
     * 标准化激活码（移除空格、横线，转大写）
     */
    private fun normalizeCode(code: String): String {
        return code.replace("-", "")
            .replace(" ", "")
            .uppercase()
            .trim()
    }

    /**
     * 计算哈希值
     */
    private fun hashCode(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((input + SALT).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * 激活结果
 */
sealed class ActivationResult {
    data object Success : ActivationResult()
    data object Invalid : ActivationResult()
    data object Empty : ActivationResult()
    data object AlreadyActivated : ActivationResult()
}
