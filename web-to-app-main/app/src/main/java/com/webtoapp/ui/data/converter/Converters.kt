package com.webtoapp.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.AdConfig
import com.webtoapp.data.model.Announcement
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.MediaConfig
import com.webtoapp.data.model.SplashConfig
import com.webtoapp.data.model.WebViewConfig

/**
 * Room数据库类型转换器
 */
class Converters {
    private val gson = Gson()

    // List<String> 转换
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // AdConfig 转换
    @TypeConverter
    fun fromAdConfig(value: AdConfig?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAdConfig(value: String): AdConfig? {
        return try {
            gson.fromJson(value, AdConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Announcement 转换
    @TypeConverter
    fun fromAnnouncement(value: Announcement?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toAnnouncement(value: String): Announcement? {
        return try {
            gson.fromJson(value, Announcement::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // WebViewConfig 转换
    @TypeConverter
    fun fromWebViewConfig(value: WebViewConfig): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWebViewConfig(value: String): WebViewConfig {
        return try {
            gson.fromJson(value, WebViewConfig::class.java) ?: WebViewConfig()
        } catch (e: Exception) {
            WebViewConfig()
        }
    }

    // SplashConfig 转换
    @TypeConverter
    fun fromSplashConfig(value: SplashConfig?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSplashConfig(value: String): SplashConfig? {
        return try {
            gson.fromJson(value, SplashConfig::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
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
    fun fromMediaConfig(value: MediaConfig?): String = gson.toJson(value)

    @TypeConverter
    fun toMediaConfig(value: String): MediaConfig? = try {
        gson.fromJson(value, MediaConfig::class.java)
    } catch (e: Exception) {
        null
    }
    
    // BgmConfig 转换
    @TypeConverter
    fun fromBgmConfig(value: BgmConfig?): String = gson.toJson(value)

    @TypeConverter
    fun toBgmConfig(value: String): BgmConfig? = try {
        gson.fromJson(value, BgmConfig::class.java)
    } catch (e: Exception) {
        null
    }
    
    // HtmlConfig 转换
    @TypeConverter
    fun fromHtmlConfig(value: HtmlConfig?): String = gson.toJson(value)

    @TypeConverter
    fun toHtmlConfig(value: String): HtmlConfig? = try {
        gson.fromJson(value, HtmlConfig::class.java)
    } catch (e: Exception) {
        null
    }
    
    // ApkExportConfig 转换
    @TypeConverter
    fun fromApkExportConfig(value: ApkExportConfig?): String = gson.toJson(value)

    @TypeConverter
    fun toApkExportConfig(value: String): ApkExportConfig? = try {
        gson.fromJson(value, ApkExportConfig::class.java)
    } catch (e: Exception) {
        null
    }
}
