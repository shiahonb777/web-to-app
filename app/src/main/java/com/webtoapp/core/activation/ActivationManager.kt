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
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.i18n.AppStringsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Activation Code Manager
 *
 * Handles local activation code verification, lockout, and status persistence.
 */
private val Context.activationDataStore: DataStore<Preferences> by preferencesDataStore(name = "activation")

class ActivationManager(private val context: Context) {

    companion object {
        private const val TAG = "ActivationManager"
        private const val SALT = "WebToApp_Salt_2024"
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 2 * 60 * 1000L

        // Character pool for activation code generation (unambiguous chars)
        private const val CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        // Activation code length constraints
        const val MIN_CODE_LENGTH = 4
        const val MAX_CODE_LENGTH = 16
        const val DEFAULT_CODE_LENGTH = 8
    }

    private val secureRandom = SecureRandom()

    // ═══════════════════════════════════════════
    // VERIFICATION (unified entry point)
    // ═══════════════════════════════════════════

    /**
     * Verify activation code (supports both raw [String] list and [ActivationCode] objects).
     *
     * @param appId App ID
     * @param inputCode User input activation code
     * @param validCodes Valid activation code list (supports JSON-encoded ActivationCode or plain strings)
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

        val lockoutRemaining = getLockoutRemainingMs(appId)
        if (lockoutRemaining > 0) {
            AppLogger.w(TAG, "Verification blocked: app=$appId is locked out for ${formatLockoutRemaining(lockoutRemaining)}")
            return ActivationResult.Invalid(
                AppStringsProvider.current().tooManyAttemptsWithCountdown(formatLockoutRemaining(lockoutRemaining))
            )
        }

        val normalizedInput = normalizeCode(inputCode)

        // Parse each entry as ActivationCode (JSON) or legacy plain string
        val parsedCodes = validCodes.map { raw ->
            ActivationCode.fromJson(raw) ?: ActivationCode.fromLegacyString(raw)
        }

        return verifyAgainstCodes(appId, normalizedInput, parsedCodes)
    }

    /**
     * Verify activation code directly with [ActivationCode] objects.
     */
    suspend fun verifyActivationCodeWithObjects(
        appId: Long,
        inputCode: String,
        validActivationCodes: List<ActivationCode>
    ): ActivationResult {
        if (inputCode.isBlank()) {
            return ActivationResult.Empty
        }

        val lockoutRemaining = getLockoutRemainingMs(appId)
        if (lockoutRemaining > 0) {
            AppLogger.w(TAG, "Verification blocked: app=$appId is locked out")
            return ActivationResult.Invalid(
                AppStringsProvider.current().tooManyAttemptsWithCountdown(formatLockoutRemaining(lockoutRemaining))
            )
        }

        val normalizedInput = normalizeCode(inputCode)
        return verifyAgainstCodes(appId, normalizedInput, validActivationCodes)
    }

    /**
     * Core verification logic (single implementation used by both entry points).
     */
    private suspend fun verifyAgainstCodes(
        appId: Long,
        normalizedInput: String,
        validCodes: List<ActivationCode>
    ): ActivationResult {
        // Find matching activation code using constant-time comparison
        val matchedCode = validCodes.firstOrNull { code ->
            val normalizedCode = normalizeCode(code.code)
            constantTimeEquals(normalizedInput, normalizedCode) ||
                constantTimeEquals(sha256Hash(normalizedInput), normalizedCode)
        }

        if (matchedCode == null) {
            val newLockout = recordFailedAttempt(appId)
            AppLogger.w(TAG, "Verification failed: app=$appId, lockout=${newLockout}ms")
            return if (newLockout > 0) {
                ActivationResult.Invalid(
                    AppStringsProvider.current().tooManyAttemptsWithCountdown(formatLockoutRemaining(newLockout))
                )
            } else {
                ActivationResult.Invalid(AppStringsProvider.current().invalidActivationCode)
            }
        }

        // Check if already activated
        val currentStatus = getActivationStatus(appId)
        if (currentStatus.isActivated && currentStatus.isValid) {
            AppLogger.i(TAG, "Already activated: app=$appId")
            return ActivationResult.AlreadyActivated
        }

        // Validate activation code type restrictions
        val deviceId = DeviceIdGenerator.getDeviceId(context)
        val validationResult = validateActivationCode(matchedCode, appId, deviceId)

        if (validationResult is ActivationResult.Success) {
            clearFailedAttempts(appId)
            saveActivationStatus(appId, matchedCode, deviceId)
            AppLogger.i(TAG, "Activation successful: app=$appId, type=${matchedCode.type}")
        } else {
            AppLogger.w(TAG, "Activation rejected: app=$appId, result=$validationResult")
        }

        return validationResult
    }

    // ═══════════════════════════════════════════
    // VALIDATION
    // ═══════════════════════════════════════════

    /**
     * Validate activation code restrictions.
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
                currentStatus.deviceId != deviceId
            ) {
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
            code.type == ActivationCodeType.COMBINED
        ) {
            if (code.timeLimitMs == null || code.timeLimitMs <= 0) {
                return ActivationResult.Invalid(AppStringsProvider.current().invalidTimeLimitConfig)
            }
        }

        // Check usage limit config
        if (code.type == ActivationCodeType.USAGE_LIMITED ||
            code.type == ActivationCodeType.COMBINED
        ) {
            if (code.usageLimit == null || code.usageLimit <= 0) {
                return ActivationResult.Invalid(AppStringsProvider.current().invalidUsageLimitConfig)
            }
        }

        return ActivationResult.Success
    }

    // ═══════════════════════════════════════════
    // STATUS
    // ═══════════════════════════════════════════

    /**
     * Check if app is activated.
     */
    fun isActivated(appId: Long): Flow<Boolean> {
        return context.activationDataStore.data.map { preferences ->
            val activated = preferences[booleanPreferencesKey("activated_$appId")] ?: false
            if (activated) {
                val status = getActivationStatusSync(appId, preferences)
                status.isValid
            } else {
                false
            }
        }
    }

    /**
     * Get activation status details.
     */
    suspend fun getActivationStatus(appId: Long): ActivationStatus {
        return context.activationDataStore.data.first().let { preferences ->
            getActivationStatusSync(appId, preferences)
        }
    }

    /**
     * Synchronously get activation status (internal use).
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

    // ═══════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════

    /**
     * Save activation status (new version - supports multiple types).
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
        AppLogger.i(TAG, "Activation status saved: app=$appId, type=${code.type}")
    }

    /**
     * Save activation status (old version - compatibility).
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
     * Increment usage count.
     */
    suspend fun incrementUsageCount(appId: Long) {
        context.activationDataStore.edit { preferences ->
            val currentCount = preferences[intPreferencesKey("usage_count_$appId")] ?: 0
            preferences[intPreferencesKey("usage_count_$appId")] = currentCount + 1
        }
    }

    /**
     * Reset activation status.
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
            preferences.remove(intPreferencesKey("failed_attempts_$appId"))
            preferences.remove(longPreferencesKey("lockout_until_$appId"))
        }
        AppLogger.i(TAG, "Activation reset: app=$appId")
    }

    // ═══════════════════════════════════════════
    // CODE GENERATION
    // ═══════════════════════════════════════════

    /**
     * Generate a single activation code string using [SecureRandom].
     * Supports variable length (4-16 characters, default 8).
     */
    fun generateActivationCode(
        length: Int = DEFAULT_CODE_LENGTH,
        @Suppress("UNUSED_PARAMETER") seed: String = ""
    ): String {
        val safeLen = length.coerceIn(MIN_CODE_LENGTH, MAX_CODE_LENGTH)
        val chars = CharArray(safeLen) { CODE_CHARS[secureRandom.nextInt(CODE_CHARS.length)] }
        return String(chars)
    }

    /**
     * Generate activation code object (new version).
     */
    fun generateActivationCode(
        type: ActivationCodeType = ActivationCodeType.PERMANENT,
        timeLimitMs: Long? = null,
        usageLimit: Int? = null,
        note: String? = null,
        length: Int = DEFAULT_CODE_LENGTH
    ): ActivationCode {
        val code = generateActivationCode(length)
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
     * Batch generate activation codes.
     */
    fun generateActivationCodes(count: Int, length: Int = DEFAULT_CODE_LENGTH): List<String> {
        return (1..count).map { generateActivationCode(length) }
    }

    /**
     * Batch generate activation code objects (new version).
     */
    fun generateActivationCodes(
        count: Int,
        type: ActivationCodeType = ActivationCodeType.PERMANENT,
        timeLimitMs: Long? = null,
        usageLimit: Int? = null,
        length: Int = DEFAULT_CODE_LENGTH
    ): List<ActivationCode> {
        return (1..count).map { index ->
            generateActivationCode(
                type = type,
                timeLimitMs = timeLimitMs,
                usageLimit = usageLimit,
                note = "${AppStringsProvider.current().batchGeneratedNote} #$index",
                length = length
            )
        }
    }

    // ═══════════════════════════════════════════
    // CRYPTO UTILITIES
    // ═══════════════════════════════════════════

    /**
     * Normalize activation code (remove spaces, dashes, convert to uppercase).
     */
    private fun normalizeCode(code: String): String {
        return code.replace("-", "")
            .replace(" ", "")
            .uppercase()
            .trim()
    }

    /**
     * SHA-256 hash with salt.
     */
    private fun sha256Hash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest((input + SALT).toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    // ═══════════════════════════════════════════
    // LOCKOUT
    // ═══════════════════════════════════════════

    private suspend fun getLockoutRemainingMs(appId: Long): Long {
        val now = System.currentTimeMillis()
        val lockoutUntil = context.activationDataStore.data.first()[longPreferencesKey("lockout_until_$appId")] ?: 0L
        return (lockoutUntil - now).coerceAtLeast(0L)
    }

    private suspend fun recordFailedAttempt(appId: Long): Long {
        val attemptsKey = intPreferencesKey("failed_attempts_$appId")
        val lockoutKey = longPreferencesKey("lockout_until_$appId")

        context.activationDataStore.edit { preferences ->
            val now = System.currentTimeMillis()
            val currentLockoutUntil = preferences[lockoutKey] ?: 0L

            if (currentLockoutUntil > now) {
                return@edit  // still locked out, don't change anything
            }

            val failedAttempts = (preferences[attemptsKey] ?: 0) + 1
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                val lockoutUntil = now + LOCKOUT_DURATION_MS
                preferences[lockoutKey] = lockoutUntil
                preferences[attemptsKey] = 0
                AppLogger.w(TAG, "Account locked out: app=$appId, until=${lockoutUntil}")
            } else {
                preferences[attemptsKey] = failedAttempts
                preferences.remove(lockoutKey)
            }
        }

        // Re-read to get the actual lockout value (avoids race condition with closure variable)
        return getLockoutRemainingMs(appId)
    }

    private suspend fun clearFailedAttempts(appId: Long) {
        val attemptsKey = intPreferencesKey("failed_attempts_$appId")
        val lockoutKey = longPreferencesKey("lockout_until_$appId")
        context.activationDataStore.edit { preferences ->
            preferences.remove(attemptsKey)
            preferences.remove(lockoutKey)
        }
    }

    private fun formatLockoutRemaining(remainingMs: Long): String {
        val totalSeconds = ((remainingMs + 999) / 1000).coerceAtLeast(1)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }
}

/**
 * Activation result
 */
sealed class ActivationResult {
    data object Success : ActivationResult()
    data class Invalid(val message: String = "") : ActivationResult()
    data object Empty : ActivationResult()
    data object AlreadyActivated : ActivationResult()
    data object DeviceMismatch : ActivationResult()
    data object Expired : ActivationResult()
    data object UsageExceeded : ActivationResult()
}
