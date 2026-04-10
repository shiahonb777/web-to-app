package com.webtoapp.ui.data.converter

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
 * Room数据库类型转换器
 * 使用全局 Gson 单例，避免重复创建实例
 */
class Converters {
    
    companion object {
        // 全局 Gson 单例，线程安全
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
        
        // Cache TypeToken，避免重复创建
        private val stringListType: Type by lazy {
            object : TypeToken<List<String>>() {}.type
        }
        
        private val activationCodeListType: Type by lazy {
            object : TypeToken<List<ActivationCode>>() {}.type
        }
        
        /**
         * 通用的 JSON 序列化方法
         */
        fun <T> toJson(value: T?): String = gson.toJson(value)
        
        /**
         * 通用的 JSON 反序列化方法
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
         * 带默认值的 JSON 反序列化方法
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
         * Recursively merge missing fields from [defaults] into [current].
         * Existing fields in [current] always take priority.
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

            // Fill missing keys from defaults (recursive for nested object).
            defaults.asJsonObject.entrySet().forEach { (key, defaultValue) ->
                val currentValue = if (merged.has(key)) merged.get(key) else null
                if (currentValue == null || currentValue.isJsonNull) {
                    merged.add(key, defaultValue.deepCopy())
                } else if (!isTypeCompatible(defaultValue, currentValue)) {
                    // 类型不兼容（如版本升级后字段类型变更），使用默认值
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

    // List<String> 转换
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

    // AdConfig 转换
    @TypeConverter
    fun fromAdConfig(value: AdConfig?): String = toJson(value)

    @TypeConverter
    fun toAdConfig(value: String): AdConfig? = fromJson(value)

    // Announcement 转换
    @TypeConverter
    fun fromAnnouncement(value: Announcement?): String = toJson(value)

    @TypeConverter
    fun toAnnouncement(value: String): Announcement? = fromJson(value)

    // WebViewConfig 转换
    @TypeConverter
    fun fromWebViewConfig(value: WebViewConfig): String = toJson(value)

    @TypeConverter
    fun toWebViewConfig(value: String): WebViewConfig = fromJsonOrDefault(value, WebViewConfig())

    // SplashConfig 转换
    @TypeConverter
    fun fromSplashConfig(value: SplashConfig?): String = toJson(value)

    @TypeConverter
    fun toSplashConfig(value: String): SplashConfig? = fromJson(value)
    
    // AppType 转换
    @TypeConverter
    fun fromAppType(value: AppType): String = value.name

    @TypeConverter
    fun toAppType(value: String): AppType = try {
        AppType.valueOf(value)
    } catch (e: Exception) {
        AppType.WEB
    }
    
    // MediaConfig 转换
    @TypeConverter
    fun fromMediaConfig(value: MediaConfig?): String = toJson(value)

    @TypeConverter
    fun toMediaConfig(value: String): MediaConfig? = fromJson(value)
    
    // GalleryConfig 转换（多媒体画廊）
    @TypeConverter
    fun fromGalleryConfig(value: GalleryConfig?): String = toJson(value)

    @TypeConverter
    fun toGalleryConfig(value: String): GalleryConfig? = fromJson(value)
    
    // BgmConfig 转换
    @TypeConverter
    fun fromBgmConfig(value: BgmConfig?): String = toJson(value)

    @TypeConverter
    fun toBgmConfig(value: String): BgmConfig? = fromJson(value)
    
    // HtmlConfig 转换
    @TypeConverter
    fun fromHtmlConfig(value: HtmlConfig?): String = toJson(value)

    @TypeConverter
    fun toHtmlConfig(value: String): HtmlConfig? = fromJson(value)
    
    // ApkExportConfig 转换
    @TypeConverter
    fun fromApkExportConfig(value: ApkExportConfig?): String = toJson(value)

    @TypeConverter
    fun toApkExportConfig(value: String): ApkExportConfig? = fromJson(value)
    
    // TranslateConfig 转换
    @TypeConverter
    fun fromTranslateConfig(value: TranslateConfig?): String = toJson(value)

    @TypeConverter
    fun toTranslateConfig(value: String): TranslateConfig? = fromJson(value)
    
    // List<ActivationCode> 转换
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
    
    // ActivationDialogConfig 转换
    @TypeConverter
    fun fromActivationDialogConfig(value: ActivationDialogConfig?): String = toJson(value)

    @TypeConverter
    fun toActivationDialogConfig(value: String): ActivationDialogConfig? = fromJson(value)
    
    // AutoStartConfig 转换
    @TypeConverter
    fun fromAutoStartConfig(value: AutoStartConfig?): String = toJson(value)

    @TypeConverter
    fun toAutoStartConfig(value: String): AutoStartConfig? = fromJson(value)
    
    // ForcedRunConfig 转换
    @TypeConverter
    fun fromForcedRunConfig(value: com.webtoapp.core.forcedrun.ForcedRunConfig?): String = toJson(value)

    @TypeConverter
    fun toForcedRunConfig(value: String): com.webtoapp.core.forcedrun.ForcedRunConfig? = fromJson(value)
    
    // BlackTechConfig 转换（独立模块）
    @TypeConverter
    fun fromBlackTechConfig(value: com.webtoapp.core.blacktech.BlackTechConfig?): String = toJson(value)

    @TypeConverter
    fun toBlackTechConfig(value: String): com.webtoapp.core.blacktech.BlackTechConfig? = fromJson(value)
    
    // DisguiseConfig 转换（独立模块）
    @TypeConverter
    fun fromDisguiseConfig(value: com.webtoapp.core.disguise.DisguiseConfig?): String = toJson(value)

    @TypeConverter
    fun toDisguiseConfig(value: String): com.webtoapp.core.disguise.DisguiseConfig? = fromJson(value)
    
    // BrowserDisguiseConfig 转换（浏览器反指纹引擎）
    @TypeConverter
    fun fromBrowserDisguiseConfig(value: com.webtoapp.core.disguise.BrowserDisguiseConfig?): String = toJson(value)

    @TypeConverter
    fun toBrowserDisguiseConfig(value: String): com.webtoapp.core.disguise.BrowserDisguiseConfig? = fromJson(value)
    
    // DeviceDisguiseConfig 转换（设备伪装引擎）
    @TypeConverter
    fun fromDeviceDisguiseConfig(value: com.webtoapp.core.disguise.DeviceDisguiseConfig?): String = toJson(value)

    @TypeConverter
    fun toDeviceDisguiseConfig(value: String): com.webtoapp.core.disguise.DeviceDisguiseConfig? = fromJson(value)
    
    // WordPressConfig 转换
    @TypeConverter
    fun fromWordPressConfig(value: WordPressConfig?): String = toJson(value)

    @TypeConverter
    fun toWordPressConfig(value: String): WordPressConfig? = fromJson(value)
    
    // NodeJsConfig 转换
    @TypeConverter
    fun fromNodeJsConfig(value: NodeJsConfig?): String = toJson(value)

    @TypeConverter
    fun toNodeJsConfig(value: String): NodeJsConfig? = fromJson(value)
    
    // PhpAppConfig 转换
    @TypeConverter
    fun fromPhpAppConfig(value: PhpAppConfig?): String = toJson(value)

    @TypeConverter
    fun toPhpAppConfig(value: String): PhpAppConfig? = fromJson(value)
    
    // PythonAppConfig 转换
    @TypeConverter
    fun fromPythonAppConfig(value: PythonAppConfig?): String = toJson(value)

    @TypeConverter
    fun toPythonAppConfig(value: String): PythonAppConfig? = fromJson(value)
    
    // GoAppConfig 转换
    @TypeConverter
    fun fromGoAppConfig(value: GoAppConfig?): String = toJson(value)

    @TypeConverter
    fun toGoAppConfig(value: String): GoAppConfig? = fromJson(value)
    
    // MultiWebConfig 转换
    @TypeConverter
    fun fromMultiWebConfig(value: MultiWebConfig?): String = toJson(value)

    @TypeConverter
    fun toMultiWebConfig(value: String): MultiWebConfig? = fromJson(value)

    // CloudAppConfig 转换
    @TypeConverter
    fun fromCloudAppConfig(value: com.webtoapp.data.model.CloudAppConfig?): String = toJson(value)

    @TypeConverter
    fun toCloudAppConfig(value: String): com.webtoapp.data.model.CloudAppConfig? = fromJson(value)

}
