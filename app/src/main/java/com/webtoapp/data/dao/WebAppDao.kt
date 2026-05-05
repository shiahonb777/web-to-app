package com.webtoapp.data.dao

import androidx.room.*
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.flow.Flow




data class WebAppSummary(
    val id: Long,
    val name: String,
    val url: String,
    val iconPath: String?,
    val appType: AppType,
    val updatedAt: Long,
    val categoryId: Long?,
    val activationEnabled: Boolean,
    val adBlockEnabled: Boolean,
    val announcementEnabled: Boolean,
)

data class WebAppStartupCandidate(
    val id: Long,
    val name: String,
    val url: String,
)




@Dao
interface WebAppDao {

    @Query("SELECT * FROM web_apps ORDER BY updatedAt DESC")
    fun getAllWebApps(): Flow<List<WebApp>>





    @Query(
        """
        SELECT id, name, url, iconPath, appType, updatedAt, categoryId,
               activationEnabled, adBlockEnabled, announcementEnabled
        FROM web_apps
        ORDER BY updatedAt DESC
        """
    )
    fun getAllWebAppSummaries(): Flow<List<WebAppSummary>>

    @Query(
        """
        SELECT id, name, url
        FROM web_apps
        WHERE appType = :appType
            AND iconPath IS NULL
            AND url LIKE 'http://%'
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getStartupCandidatesWithoutIcons(
        appType: AppType,
        limit: Int,
    ): List<WebAppStartupCandidate>

    @Query(
        """
        SELECT *
        FROM web_apps
        WHERE appType = :appType
            AND url LIKE 'http://%'
        ORDER BY updatedAt DESC
        """
    )
    fun getHttpWebApps(
        appType: AppType = AppType.WEB,
    ): Flow<List<WebApp>>

    @Query("SELECT * FROM web_apps WHERE id = :id")
    suspend fun getWebAppById(id: Long): WebApp?

    @Query("SELECT * FROM web_apps WHERE id = :id")
    fun getWebAppByIdFlow(id: Long): Flow<WebApp?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(webApp: WebApp): Long

    @Update
    suspend fun update(webApp: WebApp)

    @Delete
    suspend fun delete(webApp: WebApp)

    @Query("DELETE FROM web_apps WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE web_apps SET isActivated = :activated WHERE id = :id")
    suspend fun updateActivationStatus(id: Long, activated: Boolean)

    @Query(
        """
        UPDATE web_apps
        SET url = 'https://' || substr(url, 8),
            updatedAt = :updatedAt
        WHERE appType = :appType
            AND url LIKE 'http://%'
        """
    )
    suspend fun upgradeRemoteHttpUrls(
        appType: AppType,
        updatedAt: Long,
    ): Int

    @Query("SELECT COUNT(*) FROM web_apps")
    suspend fun getCount(): Int

    @Query("SELECT * FROM web_apps WHERE name LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'")
    fun searchWebApps(query: String): Flow<List<WebApp>>






    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(webApps: List<WebApp>): List<Long>




    @Update
    suspend fun updateAll(webApps: List<WebApp>)




    @Delete
    suspend fun deleteAll(webApps: List<WebApp>)




    @Query("DELETE FROM web_apps WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)




    @Query("UPDATE web_apps SET isActivated = :activated WHERE id IN (:ids)")
    suspend fun updateActivationStatusBatch(ids: List<Long>, activated: Boolean)




    @Query("SELECT * FROM web_apps WHERE id IN (:ids)")
    suspend fun getWebAppsByIds(ids: List<Long>): List<WebApp>




    @Query("SELECT * FROM web_apps WHERE isActivated = 1 ORDER BY updatedAt DESC")
    fun getActivatedWebApps(): Flow<List<WebApp>>




    @Query("SELECT * FROM web_apps ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentWebApps(limit: Int): Flow<List<WebApp>>




    @Query("SELECT COUNT(*) FROM web_apps WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long): Int




    @Query("UPDATE web_apps SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryId(categoryId: Long)
}
