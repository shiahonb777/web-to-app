package com.webtoapp.data.repository

import androidx.room.Transaction
import com.webtoapp.data.dao.WebAppDao
import com.webtoapp.data.dao.WebAppStartupCandidate
import com.webtoapp.data.dao.WebAppSummary
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.dao.AppCategoryDao
import kotlinx.coroutines.flow.Flow




class WebAppRepository(private val webAppDao: WebAppDao) {

    val allWebApps: Flow<List<WebApp>> = webAppDao.getAllWebApps()

    val httpWebApps: Flow<List<WebApp>> = webAppDao.getHttpWebApps()





    val allWebAppSummaries: Flow<List<WebAppSummary>> = webAppDao.getAllWebAppSummaries()

    suspend fun getStartupCandidatesWithoutIcons(
        appType: AppType = AppType.WEB,
        limit: Int = 5,
    ): List<WebAppStartupCandidate> = webAppDao.getStartupCandidatesWithoutIcons(appType, limit)

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

    suspend fun upgradeLegacyRemoteHttpWebUrls(updatedAt: Long = System.currentTimeMillis()): Int {
        return webAppDao.upgradeRemoteHttpUrls(
            appType = AppType.WEB,
            updatedAt = updatedAt,
        )
    }







    suspend fun createWebApps(webApps: List<WebApp>): List<Long> {
        if (webApps.isEmpty()) return emptyList()
        return webAppDao.insertAll(webApps)
    }




    suspend fun updateWebApps(webApps: List<WebApp>) {
        if (webApps.isEmpty()) return
        val now = System.currentTimeMillis()
        val updatedApps = webApps.map { it.copy(updatedAt = now) }
        webAppDao.updateAll(updatedApps)
    }




    suspend fun deleteWebApps(webApps: List<WebApp>) {
        if (webApps.isEmpty()) return
        webAppDao.deleteAll(webApps)
    }




    suspend fun deleteWebAppsByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        webAppDao.deleteByIds(ids)
    }




    suspend fun activateWebApps(ids: List<Long>) {
        if (ids.isEmpty()) return
        webAppDao.updateActivationStatusBatch(ids, true)
    }




    suspend fun deactivateWebApps(ids: List<Long>) {
        if (ids.isEmpty()) return
        webAppDao.updateActivationStatusBatch(ids, false)
    }




    suspend fun getWebApps(ids: List<Long>): List<WebApp> {
        if (ids.isEmpty()) return emptyList()
        return webAppDao.getWebAppsByIds(ids)
    }





    suspend fun duplicateWebApp(id: Long, newName: String? = null): Long? {
        val original = webAppDao.getWebAppById(id) ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0,
            name = newName ?: "${original.name} (副本)",
            createdAt = now,
            updatedAt = now,
            isActivated = false
        )
        return webAppDao.insert(copy)
    }




    fun getActivatedWebApps(): Flow<List<WebApp>> = webAppDao.getActivatedWebApps()




    fun getRecentWebApps(limit: Int = 10): Flow<List<WebApp>> = webAppDao.getRecentWebApps(limit)




    suspend fun isNameExists(name: String, excludeId: Long? = null): Boolean {
        return webAppDao.countByName(name, excludeId ?: -1) > 0
    }




    suspend fun clearCategoryId(categoryId: Long) {
        webAppDao.clearCategoryId(categoryId)
    }





    @Transaction
    suspend fun deleteCategoryWithCleanup(categoryId: Long, categoryDao: AppCategoryDao) {
        webAppDao.clearCategoryId(categoryId)
        categoryDao.deleteById(categoryId)
    }
}
