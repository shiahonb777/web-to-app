package com.webtoapp.ui.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
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
                .serializeNulls() // 序列化 null 值
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
        
        /**
         * 带默认值的 JSON 反序列化方法
         */
        inline fun <reified T> fromJsonOrDefault(value: String, default: T): T {
            return try {
                gson.fromJson(value, T::class.java) ?: default
            } catch (e: Exception) {
                default
            }
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
    
}
