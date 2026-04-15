package com.webtoapp.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.ActivationDialogConfig
import com.webtoapp.data.model.AdConfig
import com.webtoapp.data.model.Announcement
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.AutoStartConfig
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.GalleryConfig
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.MediaConfig
import com.webtoapp.data.model.SplashConfig
import com.webtoapp.data.model.TranslateConfig
import com.webtoapp.data.model.WebViewConfig
import com.webtoapp.data.model.NodeJsConfig
import com.webtoapp.data.model.WordPressConfig
import com.webtoapp.data.model.PhpAppConfig
import com.webtoapp.data.model.PythonAppConfig
import com.webtoapp.data.model.GoAppConfig
import com.webtoapp.data.model.MultiWebConfig
import com.webtoapp.core.activation.ActivationCode
import java.lang.reflect.Type

/**
 * Room converters backed by a shared Gson instance.
 */
class Converters {
    
    companion object {
        // Shared Gson instance, thread-safe
        @PublishedApi
        internal val gson: Gson by lazy {
            GsonBuilder()
                .registerTypeHierarchyAdapter(Enum::class.java,
                    com.google.gson.JsonDeserializer<Enum<*>> { json, typeOfT, _ ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val enumClass = typeOfT as Class<out Enum<*>>
                            java.lang.Enum.valueOf(enumClass, json.asString)
                        } catch (e: Exception) {
                            // Return first enum constant as fallback instead of crashing
                            @Suppress("UNCHECKED_CAST")
                            val enumClass = typeOfT as Class<out Enum<*>>
                            enumClass.enumConstants?.firstOrNull()
                        }
                    })
                .create()
        }
        
        // Cache TypeToken to avoid recreation
        private val stringListType: Type by lazy {
            object : TypeToken<List<String>>() {}.type
        }
        
        private val activationCodeListType: Type by lazy {
            object : TypeToken<List<ActivationCode>>() {}.type
        }
        
        /**
         * Generic JSON serializer.
         */
        fun <T> toJson(value: T?): String = gson.toJson(value)

        /**
         * Generic JSON deserializer.
         */
        inline fun <reified T> fromJson(value: String): T? {
            return try {
                gson.fromJson(value, T::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        // Cache for default JSON trees — avoids re-serializing the same default
        // object (e.g. WebViewConfig()) on every row loaded from DB.
        @PublishedApi
        internal val defaultJsonCache = java.util.concurrent.ConcurrentHashMap<Class<*>, JsonElement>()
        
        /**
         * JSON deserializer that merges defaults.
         */
        inline fun <reified T> fromJsonOrDefault(value: String, default: T): T {
            return try {
                val parsed = JsonParser.parseString(value)
                val clazz = T::class.java
                val defaultJson = defaultJsonCache.getOrPut(clazz) {
                    gson.toJsonTree(default)
                }
                val merged = mergeMissingDefaults(defaultJson, parsed)
                gson.fromJson(merged, clazz) ?: default
            } catch (e: Exception) {
                default
            }
        }

        /**
         * Fill missing fields from [defaults], keeping [current] values first.
         */
        @PublishedApi
        internal fun mergeMissingDefaults(defaults: JsonElement, current: JsonElement?): JsonElement {
            if (!defaults.isJsonObject) {
                return current?.deepCopy() ?: defaults.deepCopy()
            }

            val merged = JsonObject()
            val currentObj = if (current != null && current.isJsonObject) current.asJsonObject else JsonObject()

            // Keep existing values first.
            currentObj.entrySet().forEach { (key, value) ->
                merged.add(key, value)
            }

                // Fill missing keys from defaults recursively.
            defaults.asJsonObject.entrySet().forEach { (key, defaultValue) ->
                val currentValue = if (merged.has(key)) merged.get(key) else null
                if (currentValue == null || currentValue.isJsonNull) {
                    merged.add(key, defaultValue.deepCopy())
                } else if (!isTypeCompatible(defaultValue, currentValue)) {
                    // Type mismatch (e.g., field changed after upgrade), fall back to default
                    merged.add(key, defaultValue.deepCopy())
                } else {
                    merged.add(key, mergeMissingDefaults(defaultValue, currentValue))
                }
            }

            return merged
        }
        
        /**
         * Check if two JsonElements have compatible types for merging.
         * Returns false when types differ fundamentally (e.g., primitive vs object),
         * indicating the old value should be replaced with the default.
         */
        private fun isTypeCompatible(default: JsonElement, current: JsonElement): Boolean {
            if (default.isJsonObject && !current.isJsonObject) return false
            if (default.isJsonArray && !current.isJsonArray) return false
            if (default.isJsonPrimitive && current.isJsonObject) return false
            if (default.isJsonPrimitive && current.isJsonArray) return false
            return true
        }
    }

    // List<String> converter
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            gson.fromJson(value, stringListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // AdConfig converter
    @TypeConverter
    fun fromAdConfig(value: AdConfig?): String = toJson(value)

    @TypeConverter
    fun toAdConfig(value: String): AdConfig? = fromJson(value)

    // Announcement converter
    @TypeConverter
    fun fromAnnouncement(value: Announcement?): String = toJson(value)

    @TypeConverter
    fun toAnnouncement(value: String): Announcement? = fromJson(value)

    // WebViewConfig converter
    @TypeConverter
    fun fromWebViewConfig(value: WebViewConfig): String = toJson(value)

    @TypeConverter
    fun toWebViewConfig(value: String): WebViewConfig = fromJsonOrDefault(value, WebViewConfig())

    // SplashConfig converter
    @TypeConverter
    fun fromSplashConfig(value: SplashConfig?): String = toJson(value)

    @TypeConverter
    fun toSplashConfig(value: String): SplashConfig? = fromJson(value)
    
    // AppType converter
    @TypeConverter
    fun fromAppType(value: AppType): String = value.name

    @TypeConverter
    fun toAppType(value: String): AppType = try {
        AppType.valueOf(value)
    } catch (e: Exception) {
        AppType.WEB
    }
    
    // MediaConfig converter
    @TypeConverter
    fun fromMediaConfig(value: MediaConfig?): String = toJson(value)

    @TypeConverter
    fun toMediaConfig(value: String): MediaConfig? = fromJson(value)
    
    // GalleryConfig converter (media gallery)
    @TypeConverter
    fun fromGalleryConfig(value: GalleryConfig?): String = toJson(value)

    @TypeConverter
    fun toGalleryConfig(value: String): GalleryConfig? = fromJson(value)
    
    // BgmConfig converter
    @TypeConverter
    fun fromBgmConfig(value: BgmConfig?): String = toJson(value)

    @TypeConverter
    fun toBgmConfig(value: String): BgmConfig? = fromJson(value)
    
    // HtmlConfig converter
    @TypeConverter
    fun fromHtmlConfig(value: HtmlConfig?): String = toJson(value)

    @TypeConverter
    fun toHtmlConfig(value: String): HtmlConfig? = fromJson(value)
    
    // ApkExportConfig converter
    @TypeConverter
    fun fromApkExportConfig(value: ApkExportConfig?): String = toJson(value)

    @TypeConverter
    fun toApkExportConfig(value: String): ApkExportConfig? = fromJson(value)
    
    // TranslateConfig converter
    @TypeConverter
    fun fromTranslateConfig(value: TranslateConfig?): String = toJson(value)

    @TypeConverter
    fun toTranslateConfig(value: String): TranslateConfig? = fromJson(value)
    
    // List<ActivationCode> converter
    @TypeConverter
    fun fromActivationCodeList(value: List<ActivationCode>?): String {
        return gson.toJson(value ?: emptyList<ActivationCode>())
    }

    @TypeConverter
    fun toActivationCodeList(value: String): List<ActivationCode> {
        return try {
            gson.fromJson(value, activationCodeListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // ActivationDialogConfig converter
    @TypeConverter
    fun fromActivationDialogConfig(value: ActivationDialogConfig?): String = toJson(value)

    @TypeConverter
    fun toActivationDialogConfig(value: String): ActivationDialogConfig? = fromJson(value)
    
    // AutoStartConfig converter
    @TypeConverter
    fun fromAutoStartConfig(value: AutoStartConfig?): String = toJson(value)

    @TypeConverter
    fun toAutoStartConfig(value: String): AutoStartConfig? = fromJson(value)
    
    // ForcedRunConfig converter
    @TypeConverter
    fun fromForcedRunConfig(value: com.webtoapp.core.forcedrun.ForcedRunConfig?): String = toJson(value)

    @TypeConverter
    fun toForcedRunConfig(value: String): com.webtoapp.core.forcedrun.ForcedRunConfig? = fromJson(value)
    
    // BlackTechConfig converter (isolated module)
    @TypeConverter
    fun fromBlackTechConfig(value: com.webtoapp.core.blacktech.BlackTechConfig?): String = toJson(value)

    @TypeConverter
    fun toBlackTechConfig(value: String): com.webtoapp.core.blacktech.BlackTechConfig? = fromJson(value)
    
    // DisguiseConfig converter (isolated module)
    @TypeConverter
    fun fromDisguiseConfig(value: com.webtoapp.core.disguise.DisguiseConfig?): String = toJson(value)

    @TypeConverter
    fun toDisguiseConfig(value: String): com.webtoapp.core.disguise.DisguiseConfig? = fromJson(value)
    
    // BrowserDisguiseConfig converter (browser spoofing)
    @TypeConverter
    fun fromBrowserDisguiseConfig(value: com.webtoapp.core.disguise.BrowserDisguiseConfig?): String = toJson(value)

    @TypeConverter
    fun toBrowserDisguiseConfig(value: String): com.webtoapp.core.disguise.BrowserDisguiseConfig? = fromJson(value)
    
    // DeviceDisguiseConfig converter (device spoofing)
    @TypeConverter
    fun fromDeviceDisguiseConfig(value: com.webtoapp.core.disguise.DeviceDisguiseConfig?): String = toJson(value)

    @TypeConverter
    fun toDeviceDisguiseConfig(value: String): com.webtoapp.core.disguise.DeviceDisguiseConfig? = fromJson(value)
    
    // WordPressConfig converter
    @TypeConverter
    fun fromWordPressConfig(value: WordPressConfig?): String = toJson(value)

    @TypeConverter
    fun toWordPressConfig(value: String): WordPressConfig? = fromJson(value)
    
    // NodeJsConfig converter
    @TypeConverter
    fun fromNodeJsConfig(value: NodeJsConfig?): String = toJson(value)

    @TypeConverter
    fun toNodeJsConfig(value: String): NodeJsConfig? = fromJson(value)
    
    // PhpAppConfig converter
    @TypeConverter
    fun fromPhpAppConfig(value: PhpAppConfig?): String = toJson(value)

    @TypeConverter
    fun toPhpAppConfig(value: String): PhpAppConfig? = fromJson(value)
    
    // PythonAppConfig converter
    @TypeConverter
    fun fromPythonAppConfig(value: PythonAppConfig?): String = toJson(value)

    @TypeConverter
    fun toPythonAppConfig(value: String): PythonAppConfig? = fromJson(value)
    
    // GoAppConfig converter
    @TypeConverter
    fun fromGoAppConfig(value: GoAppConfig?): String = toJson(value)

    @TypeConverter
    fun toGoAppConfig(value: String): GoAppConfig? = fromJson(value)
    
    // MultiWebConfig converter
    @TypeConverter
    fun fromMultiWebConfig(value: MultiWebConfig?): String = toJson(value)

    @TypeConverter
    fun toMultiWebConfig(value: String): MultiWebConfig? = fromJson(value)

    // CloudAppConfig converter
    @TypeConverter
    fun fromCloudAppConfig(value: com.webtoapp.data.model.CloudAppConfig?): String = toJson(value)

    @TypeConverter
    fun toCloudAppConfig(value: String): com.webtoapp.data.model.CloudAppConfig? = fromJson(value)

}
