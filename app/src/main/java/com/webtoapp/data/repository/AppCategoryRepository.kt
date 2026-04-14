package com.webtoapp.data.repository

import com.webtoapp.data.dao.AppCategoryDao
import com.webtoapp.data.model.AppCategory
import kotlinx.coroutines.flow.Flow

/**
 * App category repository.
 */
class AppCategoryRepository(private val categoryDao: AppCategoryDao) {

    val allCategories: Flow<List<AppCategory>> = categoryDao.getAllCategories()

    suspend fun createCategory(category: AppCategory): Long {
        return categoryDao.insert(category)
    }

    suspend fun updateCategory(category: AppCategory) {
        categoryDao.update(category)
    }

    suspend fun deleteCategory(category: AppCategory) {
        categoryDao.delete(category)
    }
}
