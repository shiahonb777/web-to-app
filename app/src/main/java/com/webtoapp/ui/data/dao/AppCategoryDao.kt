package com.webtoapp.data.dao

import androidx.room.*
import com.webtoapp.data.model.AppCategory
import kotlinx.coroutines.flow.Flow

/**
 * 应用分类 DAO
 */
@Dao
interface AppCategoryDao {
    
    @Query("SELECT * FROM app_categories ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllCategories(): Flow<List<AppCategory>>
    
    @Query("SELECT * FROM app_categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): AppCategory?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: AppCategory): Long
    
    @Update
    suspend fun update(category: AppCategory)
    
    @Delete
    suspend fun delete(category: AppCategory)
    
    @Query("DELETE FROM app_categories WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT COUNT(*) FROM app_categories")
    suspend fun getCategoryCount(): Int
}
