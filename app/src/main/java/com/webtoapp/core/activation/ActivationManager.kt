package com.webtoapp.core.activation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
     * 验证激活码（新版本 - 支持多种类型）
     * @param appId 应用ID
     * @param inputCode 用户输入的激活码
     * @param validCodes 有效激活码列表（支持旧格式字符串和新格式 ActivationCode）
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

        // 尝试解析为新格式激活码
        var matchedCode: ActivationCode? = null
        for (codeStr in validCodes) {
            // 尝试解析为 ActivationCode JSON
            val code = ActivationCode.fromJson(codeStr)
            if (code != null) {
                // 新格式激活码
                val normalizedCode = normalizeCode(code.code)
                if (normalizedInput == normalizedCode || 
                    hashCode(normalizedInput) == normalizedCode ||
                    normalizedInput == hashCode(normalizedCode)) {
                    matchedCode = code
                    break
                }
            } else {
                // 旧格式字符串激活码（兼容性）
                val normalizedValid = normalizeCode(codeStr)
                if (normalizedInput == normalizedValid || 
                    hashCode(normalizedInput) == normalizedValid ||
                    normalizedInput == hashCode(normalizedValid)) {
                    // 使用旧格式创建永久激活码
                    matchedCode = ActivationCode.fromLegacyString(codeStr)
                    break
                }
            }
        }

        if (matchedCode == null) {
            return ActivationResult.Invalid()
        }

        // 检查是否已激活
        val currentStatus = getActivationStatus(appId)
        if (currentStatus.isActivated && currentStatus.isValid) {
            return ActivationResult.AlreadyActivated
        }

        // 验证激活码类型限制
        val deviceId = DeviceIdGenerator.getDeviceId(context)
        val validationResult = validateActivationCode(matchedCode, appId, deviceId)
        
        if (validationResult is ActivationResult.Success) {
            // 保存激活状态
            saveActivationStatus(appId, matchedCode, deviceId)
        }

        return validationResult
    }

    /**
     * 验证激活码（新版本 - 直接使用 ActivationCode 对象）
     */
    suspend fun verifyActivationCodeWithObjects(
        appId: Long,
        inputCode: String,
        validActivationCodes: List<ActivationCode>
    ): ActivationResult {
        if (inputCode.isBlank()) {
            return ActivationResult.Empty
        }

        val normalizedInput = normalizeCode(inputCode)

        // 查找匹配的激活码
        val matchedCode = validActivationCodes.firstOrNull { code ->
            val normalizedCode = normalizeCode(code.code)
            normalizedInput == normalizedCode || 
            hashCode(normalizedInput) == normalizedCode ||
            normalizedInput == hashCode(normalizedCode)
        }

        if (matchedCode == null) {
            return ActivationResult.Invalid()
        }

        // 检查是否已激活
        val currentStatus = getActivationStatus(appId)
        if (currentStatus.isActivated && currentStatus.isValid) {
            return ActivationResult.AlreadyActivated
        }

        // 验证激活码类型限制
        val deviceId = DeviceIdGenerator.getDeviceId(context)
        val validationResult = validateActivationCode(matchedCode, appId, deviceId)
        
        if (validationResult is ActivationResult.Success) {
            // 保存激活状态
            saveActivationStatus(appId, matchedCode, deviceId)
        }

        return validationResult
    }

    /**
     * 验证激活码的限制条件
     */
    private suspend fun validateActivationCode(
        code: ActivationCode,
        appId: Long,
        deviceId: String
    ): ActivationResult {
        val currentStatus = getActivationStatus(appId)

        // 如果已激活，检查是否仍然有效
        if (currentStatus.isActivated) {
            if (currentStatus.isExpired) {
                return ActivationResult.Expired
            }
            if (currentStatus.isUsageExceeded) {
                return ActivationResult.UsageExceeded
            }
            if (code.type == ActivationCodeType.DEVICE_BOUND && 
                currentStatus.deviceId != null && 
                currentStatus.deviceId != deviceId) {
                return ActivationResult.DeviceMismatch
            }
        }

        // 检查设备绑定
        if (code.type == ActivationCodeType.DEVICE_BOUND && code.allowDeviceBinding) {
            if (currentStatus.deviceId != null && currentStatus.deviceId != deviceId) {
                return ActivationResult.DeviceMismatch
            }
        }

        // 检查时间限制配置
        if (code.type == ActivationCodeType.TIME_LIMITED || 
            code.type == ActivationCodeType.COMBINED) {
            if (code.timeLimitMs == null || code.timeLimitMs <= 0) {
                return ActivationResult.Invalid("时间限制配置无效")
            }
        }

        // 检查使用次数限制配置
        if (code.type == ActivationCodeType.USAGE_LIMITED || 
            code.type == ActivationCodeType.COMBINED) {
            if (code.usageLimit == null || code.usageLimit <= 0) {
                return ActivationResult.Invalid("使用次数限制配置无效")
            }
        }

        return ActivationResult.Success
    }

    /**
     * 检查应用是否已激活
     */
    fun isActivated(appId: Long): Flow<Boolean> {
        return context.activationDataStore.data.map { preferences ->
            val activated = preferences[booleanPreferencesKey("activated_$appId")] ?: false
            if (activated) {
                // 检查是否仍然有效
                val status = getActivationStatusSync(appId, preferences)
                status.isValid
            } else {
                false
            }
        }
    }

    /**
     * 获取激活状态详情
     */
    suspend fun getActivationStatus(appId: Long): ActivationStatus {
        return context.activationDataStore.data.first().let { preferences ->
            getActivationStatusSync(appId, preferences)
        }
    }

    /**
     * 同步获取激活状态（内部使用）
     */
    private fun getActivationStatusSync(appId: Long, preferences: Preferences): ActivationStatus {
        val isActivated = preferences[booleanPreferencesKey("activated_$appId")] ?: false
        val activatedTime = preferences[longPreferencesKey("activated_time_$appId")]
        val expireTime = preferences[longPreferencesKey("expire_time_$appId")]
        val usageCount = preferences[intPreferencesKey("usage_count_$appId")] ?: 0
        val usageLimit = preferences[intPreferencesKey("usage_limit_$appId")]
        val deviceId = preferences[stringPreferencesKey("device_id_$appId")]
        val codeTypeStr = preferences[stringPreferencesKey("code_type_$appId")]
        val codeType = codeTypeStr?.let { 
            try {
                ActivationCodeType.valueOf(it)
            } catch (e: Exception) {
                null
            }
        }

        return ActivationStatus(
            isActivated = isActivated,
            activatedTime = activatedTime,
            expireTime = expireTime,
            usageCount = usageCount,
            usageLimit = usageLimit,
            deviceId = deviceId,
            codeType = codeType
        )
    }

    /**
     * 保存激活状态（新版本 - 支持多种类型）
     */
    suspend fun saveActivationStatus(appId: Long, code: ActivationCode, deviceId: String) {
        val currentTime = System.currentTimeMillis()
        val expireTime = when (code.type) {
            ActivationCodeType.TIME_LIMITED, ActivationCodeType.COMBINED -> {
                code.timeLimitMs?.let { currentTime + it }
            }
            else -> null
        }

        context.activationDataStore.edit { preferences ->
            preferences[booleanPreferencesKey("activated_$appId")] = true
            preferences[longPreferencesKey("activated_time_$appId")] = currentTime
            expireTime?.let {
                preferences[longPreferencesKey("expire_time_$appId")] = it
            }
            code.usageLimit?.let {
                preferences[intPreferencesKey("usage_limit_$appId")] = it
                preferences[intPreferencesKey("usage_count_$appId")] = 0
            }
            if (code.type == ActivationCodeType.DEVICE_BOUND && code.allowDeviceBinding) {
                preferences[stringPreferencesKey("device_id_$appId")] = deviceId
            }
            preferences[stringPreferencesKey("code_type_$appId")] = code.type.name
        }
    }

    /**
     * 保存激活状态（旧版本 - 兼容性）
     */
    suspend fun saveActivationStatus(appId: Long, activated: Boolean) {
        context.activationDataStore.edit { preferences ->
            preferences[booleanPreferencesKey("activated_$appId")] = activated
            if (activated) {
                preferences[longPreferencesKey("activated_time_$appId")] = 
                    System.currentTimeMillis()
            }
        }
    }

    /**
     * 增加使用次数
     */
    suspend fun incrementUsageCount(appId: Long) {
        context.activationDataStore.edit { preferences ->
            val currentCount = preferences[intPreferencesKey("usage_count_$appId")] ?: 0
            preferences[intPreferencesKey("usage_count_$appId")] = currentCount + 1
        }
    }

    /**
     * 重置激活状态
     */
    suspend fun resetActivation(appId: Long) {
        context.activationDataStore.edit { preferences ->
            preferences.remove(booleanPreferencesKey("activated_$appId"))
            preferences.remove(longPreferencesKey("activated_time_$appId"))
            preferences.remove(longPreferencesKey("expire_time_$appId"))
            preferences.remove(intPreferencesKey("usage_count_$appId"))
            preferences.remove(intPreferencesKey("usage_limit_$appId"))
            preferences.remove(stringPreferencesKey("device_id_$appId"))
            preferences.remove(stringPreferencesKey("code_type_$appId"))
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
     * 生成激活码对象（新版本）
     */
    fun generateActivationCode(
        type: ActivationCodeType = ActivationCodeType.PERMANENT,
        timeLimitMs: Long? = null,
        usageLimit: Int? = null,
        note: String? = null
    ): ActivationCode {
        val code = generateActivationCode("${System.currentTimeMillis()}_${type.name}")
        return ActivationCode(
            code = code,
            type = type,
            timeLimitMs = timeLimitMs,
            usageLimit = usageLimit,
            allowDeviceBinding = type == ActivationCodeType.DEVICE_BOUND,
            note = note
        )
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
     * 批量生成激活码对象（新版本）
     */
    fun generateActivationCodes(
        count: Int,
        type: ActivationCodeType = ActivationCodeType.PERMANENT,
        timeLimitMs: Long? = null,
        usageLimit: Int? = null
    ): List<ActivationCode> {
        return (1..count).map { index ->
            generateActivationCode(
                type = type,
                timeLimitMs = timeLimitMs,
                usageLimit = usageLimit,
                note = "批量生成 #$index"
            )
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
    data class Invalid(val message: String = "激活码无效") : ActivationResult()
    data object Empty : ActivationResult()
    data object AlreadyActivated : ActivationResult()
    data object DeviceMismatch : ActivationResult()
    data object Expired : ActivationResult()
    data object UsageExceeded : ActivationResult()
}
