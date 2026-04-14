package com.webtoapp.data.model.webapp.config

data class AdConfig(
    val bannerEnabled: Boolean = false,
    val bannerId: String = "",
    val interstitialEnabled: Boolean = false,
    val interstitialId: String = "",
    val splashEnabled: Boolean = false,
    val splashId: String = "",
    val splashDuration: Int = 3 // seconds
)

enum class AnnouncementTemplateType {
    MINIMAL,
    XIAOHONGSHU,
    GRADIENT,
    GLASSMORPHISM,
    NEON,
    CUTE,
    ELEGANT,
    FESTIVE,
    DARK,
    NATURE
}

data class Announcement(
    val title: String = "",
    val content: String = "",
    val linkUrl: String? = null,
    val linkText: String? = null,
    val showOnce: Boolean = true,
    val enabled: Boolean = true,
    val version: Int = 1,
    val template: AnnouncementTemplateType = AnnouncementTemplateType.XIAOHONGSHU,
    val showEmoji: Boolean = true,
    val animationEnabled: Boolean = true,
    val requireConfirmation: Boolean = false,
    val allowNeverShow: Boolean = true,

    val triggerOnLaunch: Boolean = true,
    val triggerOnNoNetwork: Boolean = false,
    val triggerIntervalMinutes: Int = 0,
    val triggerIntervalIncludeLaunch: Boolean = false
)

enum class TranslateLanguage(val code: String, val displayName: String) {
    CHINESE("zh-CN", "中文（简体）"),
    CHINESE_TW("zh-TW", "中文（繁體）"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    RUSSIAN("ru", "Русский"),
    ARABIC("ar", "العربية"),
    HINDI("hi", "हिन्दी"),
    THAI("th", "ไทย"),
    VIETNAMESE("vi", "Tiếng Việt"),
    INDONESIAN("id", "Bahasa Indonesia"),
    MALAY("ms", "Bahasa Melayu"),
    TURKISH("tr", "Türkçe"),
    ITALIAN("it", "Italiano"),
    DUTCH("nl", "Nederlands"),
    POLISH("pl", "Polski")
}

enum class TranslateEngine(val displayName: String) {
    AUTO("自动选择"),
    GOOGLE("Google Translate"),
    MYMEMORY("MyMemory"),
    LIBRE("LibreTranslate"),
    LINGVA("Lingva Translate")
}

data class TranslateConfig(
    val targetLanguage: TranslateLanguage = TranslateLanguage.CHINESE,
    val showFloatingButton: Boolean = true,
    val preferredEngine: TranslateEngine = TranslateEngine.AUTO,
    val autoTranslateOnLoad: Boolean = true
)

/**
 * WebApp extension - collect all activation codes (new and legacy).
 */
fun WebApp.getAllActivationCodes(): List<com.webtoapp.core.activation.ActivationCode> {
    val codes = mutableListOf<com.webtoapp.core.activation.ActivationCode>()
    
    codes.addAll(activationCodeList)
    
    activationCodes.forEach { codeStr ->
        val code = com.webtoapp.core.activation.ActivationCode.fromJson(codeStr)
        if (code != null) {
            codes.add(code)
        } else {
            codes.add(com.webtoapp.core.activation.ActivationCode.fromLegacyString(codeStr))
        }
    }
    
    return codes
}

/**
 * WebApp extension - list activation code strings for legacy support.
 */
fun WebApp.getActivationCodeStrings(): List<String> {
    val strings = mutableListOf<String>()
    
    activationCodeList.forEach { code ->
        strings.add(code.toJson())
    }
    
    activationCodes.forEach { codeStr ->
        if (!codeStr.trimStart().startsWith("{")) {
            strings.add(codeStr)
        }
    }
    
    return strings
}

data class ActivationDialogConfig(
    val title: String = "",
    val subtitle: String = "",
    val inputLabel: String = "",
    val buttonText: String = ""
)

data class AutoStartConfig(
    val bootStartEnabled: Boolean = false,
    val scheduledStartEnabled: Boolean = false,
    val scheduledTime: String = "08:00",
    val scheduledDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val scheduledRepeat: Boolean = true,
    val bootDelay: Long = 5000L
)
