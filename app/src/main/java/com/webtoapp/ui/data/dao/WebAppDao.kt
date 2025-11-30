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
}
