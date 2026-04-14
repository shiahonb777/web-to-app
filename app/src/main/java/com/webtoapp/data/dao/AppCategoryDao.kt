package com.webtoapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.webtoapp.data.model.AppCategory
import kotlinx.coroutines.flow.Flow

/**
 * App category DAO.
 */
@Dao
interface AppCategoryDao {

    @Query("SELECT * FROM app_categories ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllCategories(): Flow<List<AppCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: AppCategory): Long

    @Update
    suspend fun update(category: AppCategory)

    @Delete
    suspend fun delete(category: AppCategory)
}
