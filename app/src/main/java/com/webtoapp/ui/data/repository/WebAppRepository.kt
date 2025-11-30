package com.webtoapp.data.repository

import com.webtoapp.data.dao.WebAppDao
import com.webtoapp.data.model.WebApp
import kotlinx.coroutines.flow.Flow

/**
 * WebApp数据仓库
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

    suspend fun deleteWebAppById(id: Long) {
        webAppDao.deleteById(id)
    }

    suspend fun activateWebApp(id: Long) {
        webAppDao.updateActivationStatus(id, true)
    }

    suspend fun deactivateWebApp(id: Long) {
        webAppDao.updateActivationStatus(id, false)
    }

    fun searchWebApps(query: String): Flow<List<WebApp>> = webAppDao.searchWebApps(query)

    suspend fun getWebAppCount(): Int = webAppDao.getCount()
}
