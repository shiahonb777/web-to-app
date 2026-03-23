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
 * Activation Code Manager
 */
private val Context.activationDataStore: DataStore<Preferences> by preferencesDataStore(name = "activation")

class ActivationManager(private val context: Context) {

    companion object {
        private const val SALT = "WebToApp_Salt_2024"
    }

    /**
     * Verify activation code (new version - supports multiple types)
     * @param appId App ID
     * @param inputCode User input activation code
     * @param validCodes Valid activation code list (supports old format string and new format ActivationCode)
     * @return Verification result
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

        // Try to parse as new format activation code
        var matchedCode: ActivationCode? = null
        for (codeStr in validCodes) {
            // Try to parse as ActivationCode JSON
            val code = ActivationCode.fromJson(codeStr)
            if (code != null) {
                // New format activation code
                val normalizedCode = normalizeCode(code.code)
                if (normalizedInput == normalizedCode || 
                    hashCode(normalizedInput) == normalizedCode ||
                    normalizedInput == hashCode(normalizedCode)) {
                    matchedCode = code
                    break
                }
            } else {
                // Old format string activation code (compatibility)
                val normalizedValid = normalizeCode(codeStr)
                if (normalizedInput == normalizedValid || 
                    hashCode(normalizedInput) == normalizedValid ||
                    normalizedInput == hashCode(normalizedValid)) {
                    // Create permanent activation code using old format
                    matchedCode = ActivationCode.fromLegacyString(codeStr)
                    break
                }
            }
        }

        if (matchedCode == null) {
            return ActivationResult.Invalid()
        }

        // Check if already activated
        val currentStatus = getActivationStatus(appId)
        if (currentStatus.isActivated && currentStatus.isValid) {
            return ActivationResult.AlreadyActivated
        }

        // Verify activation code type restrictions
        val deviceId = DeviceIdGenerator.getDeviceId(context)
        val validationResult = validateActivationCode(matchedCode, appId, deviceId)
        
        if (validationResult is ActivationResult.Success) {
            // Save activation status
            saveActivationStatus(appId, matchedCode, deviceId)
        }

        return validationResult
    }

    /**
     * Verify activation code (new version - directly use ActivationCode objects)
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

        // Find matching activation code
        val matchedCode = validActivationCodes.firstOrNull { code ->
            val normalizedCode = normalizeCode(code.code)
            normalizedInput == normalizedCode || 
            hashCode(normalizedInput) == normalizedCode ||
            normalizedInput == hashCode(normalizedCode)
        }

        if (matchedCode == null) {
            return ActivationResult.Invalid()
        }

        // Check if already activated
        val currentStatus = getActivationStatus(appId)
        if (currentStatus.isActivated && currentStatus.isValid) {
            return ActivationResult.AlreadyActivated
        }

        // Verify activation code type restrictions
        val deviceId = DeviceIdGenerator.getDeviceId(context)
        val validationResult = validateActivationCode(matchedCode, appId, deviceId)
        
        if (validationResult is ActivationResult.Success) {
            // Save activation status
            saveActivationStatus(appId, matchedCode, deviceId)
        }

        return validationResult
    }

    /**
     * Validate activation code restrictions
     */
    private suspend fun validateActivationCode(
        code: ActivationCode,
        appId: Long,
        deviceId: String
    ): ActivationResult {
        val currentStatus = getActivationStatus(appId)

        // If already activated, check if still valid
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

        // Check device binding
        if (code.type == ActivationCodeType.DEVICE_BOUND && code.allowDeviceBinding) {
            if (currentStatus.deviceId != null && currentStatus.deviceId != deviceId) {
                return ActivationResult.DeviceMismatch
            }
        }

        // Check time limit config
        if (code.type == ActivationCodeType.TIME_LIMITED || 
            code.type == ActivationCodeType.COMBINED) {
            if (code.timeLimitMs == null || code.timeLimitMs <= 0) {
                return ActivationResult.Invalid("Invalid time limit config")
            }
        }

        // Check usage limit config
        if (code.type == ActivationCodeType.USAGE_LIMITED || 
            code.type == ActivationCodeType.COMBINED) {
            if (code.usageLimit == null || code.usageLimit <= 0) {
                return ActivationResult.Invalid("Invalid usage limit config")
            }
        }

        return ActivationResult.Success
    }

    /**
     * Check if app is activated
     */
    fun isActivated(appId: Long): Flow<Boolean> {
        return context.activationDataStore.data.map { preferences ->
            val activated = preferences[booleanPreferencesKey("activated_$appId")] ?: false
            if (activated) {
                // Check if still valid
                val status = getActivationStatusSync(appId, preferences)
                status.isValid
            } else {
                false
            }
        }
    }

    /**
     * Get activation status details
     */
    suspend fun getActivationStatus(appId: Long): ActivationStatus {
        return context.activationDataStore.data.first().let { preferences ->
            getActivationStatusSync(appId, preferences)
        }
    }

    /**
     * Synchronously get activation status (internal use)
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
     * Save activation status (new version - supports multiple types)
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
     * Save activation status (old version - compatibility)
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
     * Increment usage count
     */
    suspend fun incrementUsageCount(appId: Long) {
        context.activationDataStore.edit { preferences ->
            val currentCount = preferences[intPreferencesKey("usage_count_$appId")] ?: 0
            preferences[intPreferencesKey("usage_count_$appId")] = currentCount + 1
        }
    }

    /**
     * Reset activation status
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
     * Generate activation code (for admin use)
     */
    fun generateActivationCode(seed: String = System.currentTimeMillis().toString()): String {
        val hash = hashCode(seed)
        // Format as XXXX-XXXX-XXXX-XXXX
        return hash.take(16).uppercase().chunked(4).joinToString("-")
    }

    /**
     * Generate activation code object (new version)
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
     * Batch generate activation codes
     */
    fun generateActivationCodes(count: Int): List<String> {
        return (1..count).map { index ->
            generateActivationCode("${System.currentTimeMillis()}_$index")
        }
    }

    /**
     * Batch generate activation code objects (new version)
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
                note = "Batch generated #$index"
            )
        }
    }

    /**
     * Normalize activation code (remove spaces, dashes, convert to uppercase)
     */
    private fun normalizeCode(code: String): String {
        return code.replace("-", "")
            .replace(" ", "")
            .uppercase()
            .trim()
    }

    /**
     * Calculate hash value
     */
    private fun hashCode(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((input + SALT).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Activation result
 */
sealed class ActivationResult {
    data object Success : ActivationResult()
    data class Invalid(val message: String = "Invalid activation code") : ActivationResult()
    data object Empty : ActivationResult()
    data object AlreadyActivated : ActivationResult()
    data object DeviceMismatch : ActivationResult()
    data object Expired : ActivationResult()
    data object UsageExceeded : ActivationResult()
}
