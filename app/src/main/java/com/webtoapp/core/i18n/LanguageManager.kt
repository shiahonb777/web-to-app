package com.webtoapp.core.i18n

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Note.
 */
enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val locale: Locale,
    val isRtl: Boolean = false
) {
    CHINESE("zh", "Chinese", "中文", Locale.CHINESE),
    ENGLISH("en", "English", "English", Locale.ENGLISH),
    ARABIC("ar", "Arabic", "العربية", Locale.forLanguageTag("ar"), isRtl = true);
    
    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: CHINESE
        }
    }
}

private val Context.languageDataStore by preferencesDataStore(name = "language_settings")

/**
 * Note.
 * Note.
 */
@SuppressLint("StaticFieldLeak")
class LanguageManager(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _currentLanguageState = MutableStateFlow(AppLanguage.fromCode(getSystemLanguageCode()))
    private val storedLanguageFlow: Flow<AppLanguage> = context.languageDataStore.data.map { prefs ->
        val code = prefs[LANGUAGE_KEY] ?: getSystemLanguageCode()
        AppLanguage.fromCode(code)
    }
    
    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val LANGUAGE_SELECTED_KEY = stringPreferencesKey("language_selected")
    }

    init {
        scope.launch {
            storedLanguageFlow.collect { language ->
                _currentLanguageState.value = language
            }
        }
    }
    
    /**
     * Flow.
     */
    val currentLanguageFlow: StateFlow<AppLanguage> = _currentLanguageState
    val currentLanguage: AppLanguage get() = _currentLanguageState.value
    
    /**
     * Note.
     */
    private fun getSystemLanguageCode(): String {
        val systemLocale = Locale.getDefault()
        return when {
            systemLocale.language == "zh" -> "zh"
            systemLocale.language == "ar" -> "ar"
            else -> "en"
        }
    }
    
    /**
     * Note.
     */
    val hasSelectedLanguageFlow: Flow<Boolean> = context.languageDataStore.data.map { prefs ->
        prefs[LANGUAGE_SELECTED_KEY] == "true"
    }
    
    /**
     * Note.
     */
    suspend fun hasSelectedLanguage(): Boolean {
        return hasSelectedLanguageFlow.first()
    }
    
    /**
     * Note.
     */
    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language.code
            prefs[LANGUAGE_SELECTED_KEY] = "true"
        }
        _currentLanguageState.value = language
    }
    
    /**
     * Note.
     */
    suspend fun getCurrentLanguage(): AppLanguage {
        return storedLanguageFlow.first()
    }
    
    /**
     * Context.
     */
    fun applyLanguage(context: Context, language: AppLanguage): Context {
        val locale = language.locale
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        return context.createConfigurationContext(config)
    }
    
    /**
     * AI.
     */
    fun getPromptManager(): AiPromptManager {
        return AiPromptManager
    }
}
