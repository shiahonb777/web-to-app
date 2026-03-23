package com.webtoapp.core.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * 支持的语言枚举
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
    ARABIC("ar", "Arabic", "العربية", Locale("ar"), isRtl = true);
    
    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.find { it.code == code } ?: CHINESE
        }
    }
}

private val Context.languageDataStore by preferencesDataStore(name = "language_settings")

/**
 * 语言管理器
 * 管理应用的多语言设置
 */
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
    
    /**
     * 当前语言 Flow
     */
    val currentLanguageFlow: Flow<AppLanguage> = context.languageDataStore.data.map { prefs ->
        val code = prefs[LANGUAGE_KEY] ?: getSystemLanguageCode()
        AppLanguage.fromCode(code)
    }
    
    /**
     * 获取系统语言代码
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
     * 是否已选择过语言（首次启动检测）
     */
    val hasSelectedLanguageFlow: Flow<Boolean> = context.languageDataStore.data.map { prefs ->
        prefs[LANGUAGE_SELECTED_KEY] == "true"
    }
    
    /**
     * 检查是否已选择过语言
     */
    suspend fun hasSelectedLanguage(): Boolean {
        return hasSelectedLanguageFlow.first()
    }
    
    /**
     * 设置语言
     */
    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { prefs ->
            prefs[LANGUAGE_KEY] = language.code
            prefs[LANGUAGE_SELECTED_KEY] = "true"
        }
    }
    
    /**
     * 获取当前语言（同步）
     */
    suspend fun getCurrentLanguage(): AppLanguage {
        return currentLanguageFlow.first()
    }
    
    /**
     * 应用语言配置到 Context
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
     * 获取 AI 提示词管理器
     */
    fun getPromptManager(): AiPromptManager {
        return AiPromptManager
    }
}
