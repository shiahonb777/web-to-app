package com.webtoapp.core.announcement

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.webtoapp.data.model.Announcement
import kotlinx.coroutines.flow.first

/**
 * 公告管理器
 */
private val Context.announcementDataStore: DataStore<Preferences> by preferencesDataStore(name = "announcement")

class AnnouncementManager(private val context: Context) {

    /**
     * 检查是否需要显示公告
     * @param appId 应用ID
     * @param announcement 公告配置
     * @return 是否需要显示
     */
    suspend fun shouldShowAnnouncement(appId: Long, announcement: Announcement?): Boolean {
        if (announcement == null || !announcement.enabled) {
            return false
        }

        if (announcement.title.isBlank() && announcement.content.isBlank()) {
            return false
        }

        // 如果设置为仅显示一次，检查是否已显示过
        if (announcement.showOnce) {
            val shownVersion = getShownVersion(appId)
            return shownVersion < announcement.version
        }

        return true
    }

    /**
     * 获取已显示的公告版本
     */
    private suspend fun getShownVersion(appId: Long): Int {
        return context.announcementDataStore.data.first()[
            intPreferencesKey("announcement_shown_$appId")
        ] ?: 0
    }

    /**
     * 标记公告已显示
     */
    suspend fun markAnnouncementShown(appId: Long, version: Int) {
        context.announcementDataStore.edit { preferences ->
            preferences[intPreferencesKey("announcement_shown_$appId")] = version
        }
    }

    /**
     * 重置公告显示状态（用于测试或重新显示）
     */
    suspend fun resetAnnouncementStatus(appId: Long) {
        context.announcementDataStore.edit { preferences ->
            preferences.remove(intPreferencesKey("announcement_shown_$appId"))
        }
    }

    /**
     * 创建公告配置
     */
    fun createAnnouncement(
        title: String,
        content: String,
        linkUrl: String? = null,
        linkText: String? = null,
        showOnce: Boolean = true
    ): Announcement {
        return Announcement(
            title = title,
            content = content,
            linkUrl = linkUrl,
            linkText = linkText,
            showOnce = showOnce,
            enabled = true,
            version = System.currentTimeMillis().toInt()
        )
    }
}
