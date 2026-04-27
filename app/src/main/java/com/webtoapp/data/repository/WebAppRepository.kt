package com.webtoapp.data.repository

import com.webtoapp.data.dao.WebAppDao
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.flow.Flow

/**
 * WebApp repository.
 *
 * Keeps only meaningful wrappers:
 * - single-record CRUD
 * - bulk import/update
 * - category unlinking
 */
class WebAppRepository(private val webAppDao: WebAppDao) {

    val allWebApps: Flow<List<WebApp>> = webAppDao.getAllWebApps()

    fun getWebAppById(id: Long): Flow<WebApp?> = webAppDao.getWebAppByIdFlow(id)

    suspend fun getWebApp(id: Long): WebApp? = webAppDao.getWebAppById(id)

    suspend fun createWebApp(webApp: WebApp): Long {
        return webAppDao.insert(webApp)
    }

    suspend fun updateWebApp(webApp: WebApp) {
        webAppDao.update(webApp.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteWebApp(webApp: WebApp) {
        webAppDao.delete(webApp)
    }

    suspend fun createWebApps(webApps: List<WebApp>): List<Long> {
        if (webApps.isEmpty()) return emptyList()
        return webAppDao.insertAll(webApps)
    }

    suspend fun updateWebApps(webApps: List<WebApp>) {
        if (webApps.isEmpty()) return
        val now = System.currentTimeMillis()
        webAppDao.updateAll(webApps.map { it.copy(updatedAt = now) })
    }

    suspend fun clearCategoryId(categoryId: Long) {
        webAppDao.clearCategoryId(categoryId)
    }
}
