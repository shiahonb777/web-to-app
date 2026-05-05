package com.webtoapp.core.i18n

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale




enum class AppLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val locale: Locale,
    val isRtl: Boolean = false
) {
    CHINESE("zh", "Chinese", "中文", Locale.CHINESE),
    ENGLISH("en", "English", "English", Locale.ENGLISH),
    ARABIC("ar", "Arabic", "العربية", Locale("ar"), isRtl = true);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: CHINESE
        }
    }
}

private val Context.languageDataStore by preferencesDataStore(name = "language_settings")





@SuppressLint("StaticFieldLeak")
class LanguageManager(private val context: Context) {

    companion object {
        private val LANGUAGE_KEY = stringPreferencesKey("app_language")
        private val LANGUAGE_SELECTED_KEY = stringPreferencesKey("language_selected")

        @Volatile
        private var instance: LanguageManager? = null

        fun getInstance(context: Context): LanguageManager {
            return instance ?: synchronized(this) {
                instance ?: LanguageManager(context.applicationContext).also { instance = it }
            }
        }
    }




    val currentLanguageFlow: Flow<AppLanguage> = context.languageDataStore.data.map { prefs ->
        val code = prefs[LANGUAGE_KEY] ?: getSystemLanguageCode()
        AppLanguage.fromCode(code)
    }




    private fun getSystemLanguageCode(): String {
        val systemLocale = Locale.getDefault()
        return when {
            systemLocale.language == "zh" -> "zh"
            systemLocale.language == "ar" -> "ar"
            else -> "en"
        }
    }




    val hasSelectedLanguageFlow: Flow<Boolean> = context.languageDataStore.data.map { prefs ->
        prefs[LANGUAGE_SELECTED_KEY] == "true"
    }




    suspend fun hasSelectedLanguage(): Boolean {
        return hasSelectedLanguageFlow.first()
    }




    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language.code
            prefs[LANGUAGE_SELECTED_KEY] = "true"
        }
    }




    suspend fun getCurrentLanguage(): AppLanguage {
        return currentLanguageFlow.first()
    }




    fun applyLanguage(context: Context, language: AppLanguage): Context {
        val locale = language.locale
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }




    fun getPromptManager(): AiPromptManager {
        return AiPromptManager
    }
}
