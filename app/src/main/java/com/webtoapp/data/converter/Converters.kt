package com.webtoapp.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.AdConfig
import com.webtoapp.data.model.Announcement
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
}
