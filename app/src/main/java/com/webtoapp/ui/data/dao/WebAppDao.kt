package com.webtoapp.data.dao

import androidx.room.*
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.flow.Flow

/**
 * WebApp数据访问对象
 */
@Dao
interface WebAppDao {

    @Query("SELECT * FROM web_apps ORDER BY updatedAt DESC")
    fun getAllWebApps(): Flow<List<WebApp>>

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

    @Query("SELECT COUNT(*) FROM web_apps")
    suspend fun getCount(): Int

    @Query("SELECT * FROM web_apps WHERE name LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'")
    fun searchWebApps(query: String): Flow<List<WebApp>>
    
    // ==================== 批量操作 ====================
    
    /**
     * 批量插入
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(webApps: List<WebApp>): List<Long>
    
    /**
     * 批量更新
     */
    @Update
    suspend fun updateAll(webApps: List<WebApp>)
    
    /**
     * 批量删除
     */
    @Delete
    suspend fun deleteAll(webApps: List<WebApp>)
    
    /**
     * 批量删除 (通过 ID)
     */
    @Query("DELETE FROM web_apps WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    /**
     * 批量更新激活状态
     */
    @Query("UPDATE web_apps SET isActivated = :activated WHERE id IN (:ids)")
    suspend fun updateActivationStatusBatch(ids: List<Long>, activated: Boolean)
    
    /**
     * 批量获取 (通过 ID 列表)
     */
    @Query("SELECT * FROM web_apps WHERE id IN (:ids)")
    suspend fun getWebAppsByIds(ids: List<Long>): List<WebApp>
    
    /**
     * 获取已激活的应用
     */
    @Query("SELECT * FROM web_apps WHERE isActivated = 1 ORDER BY updatedAt DESC")
    fun getActivatedWebApps(): Flow<List<WebApp>>
    
    /**
     * 获取最近更新的应用
     */
    @Query("SELECT * FROM web_apps ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentWebApps(limit: Int): Flow<List<WebApp>>
    
    /**
     * 检查名称是否存在
     */
    @Query("SELECT COUNT(*) FROM web_apps WHERE name = :name AND id != :excludeId")
    suspend fun countByName(name: String, excludeId: Long): Int
}
