package com.webtoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.flow.Flow

/**
 * WebApp 数据访问对象。
 *
 * 这里只保留当前主流程仍在用的读写入口，
 * 不再继续养那些纯转调、已经没人叫的历史接口。
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(webApps: List<WebApp>): List<Long>

    @Update
    suspend fun updateAll(webApps: List<WebApp>)

    @Query("UPDATE web_apps SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryId(categoryId: Long)
}
