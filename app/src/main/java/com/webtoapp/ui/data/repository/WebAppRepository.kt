package com.webtoapp.data.repository

import androidx.room.Transaction
import com.webtoapp.data.dao.WebAppDao
import com.webtoapp.data.dao.WebAppSummary
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.dao.AppCategoryDao
import kotlinx.coroutines.flow.Flow

/**
 * WebApp数据仓库
 */
class WebAppRepository(private val webAppDao: WebAppDao) {

    val allWebApps: Flow<List<WebApp>> = webAppDao.getAllWebApps()
    
    /**
     * 轻量列表查询，仅返回展示所需字段，避免加载完整 WebApp 的 80+ 字段
     * 推荐在主列表页使用以降低内存和反序列化开销
     */
    val allWebAppSummaries: Flow<List<WebAppSummary>> = webAppDao.getAllWebAppSummaries()

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
    
    // ==================== 批量操作优化 ====================
    
    /**
     * 批量创建 WebApp
     * @return 插入的 ID 列表
     */
    suspend fun createWebApps(webApps: List<WebApp>): List<Long> {
        if (webApps.isEmpty()) return emptyList()
        return webAppDao.insertAll(webApps)
    }
    
    /**
     * 批量更新 WebApp
     */
    suspend fun updateWebApps(webApps: List<WebApp>) {
        if (webApps.isEmpty()) return
        val now = System.currentTimeMillis()
        val updatedApps = webApps.map { it.copy(updatedAt = now) }
        webAppDao.updateAll(updatedApps)
    }
    
    /**
     * 批量删除 WebApp
     */
    suspend fun deleteWebApps(webApps: List<WebApp>) {
        if (webApps.isEmpty()) return
        webAppDao.deleteAll(webApps)
    }
    
    /**
     * 批量删除 WebApp (通过 ID)
     */
    suspend fun deleteWebAppsByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        webAppDao.deleteByIds(ids)
    }
    
    /**
     * 批量激活 WebApp
     */
    suspend fun activateWebApps(ids: List<Long>) {
        if (ids.isEmpty()) return
        webAppDao.updateActivationStatusBatch(ids, true)
    }
    
    /**
     * 批量停用 WebApp
     */
    suspend fun deactivateWebApps(ids: List<Long>) {
        if (ids.isEmpty()) return
        webAppDao.updateActivationStatusBatch(ids, false)
    }
    
    /**
     * 获取多个 WebApp (通过 ID 列表)
     */
    suspend fun getWebApps(ids: List<Long>): List<WebApp> {
        if (ids.isEmpty()) return emptyList()
        return webAppDao.getWebAppsByIds(ids)
    }
    
    /**
     * 复制 WebApp
     * @return 新创建的 WebApp ID
     */
    suspend fun duplicateWebApp(id: Long, newName: String? = null): Long? {
        val original = webAppDao.getWebAppById(id) ?: return null
        val now = System.currentTimeMillis()
        val copy = original.copy(
            id = 0, // Room 会自动生成新 ID
            name = newName ?: "${original.name} (副本)",
            createdAt = now,
            updatedAt = now,
            isActivated = false
        )
        return webAppDao.insert(copy)
    }
    
    /**
     * 获取已激活的 WebApp 列表
     */
    fun getActivatedWebApps(): Flow<List<WebApp>> = webAppDao.getActivatedWebApps()
    
    /**
     * 获取最近更新的 WebApp 列表
     */
    fun getRecentWebApps(limit: Int = 10): Flow<List<WebApp>> = webAppDao.getRecentWebApps(limit)
    
    /**
     * 检查名称是否已存在
     */
    suspend fun isNameExists(name: String, excludeId: Long? = null): Boolean {
        return webAppDao.countByName(name, excludeId ?: -1) > 0
    }
    
    /**
     * 批量清除指定分类 ID（将分类下的应用设为未分类）
     */
    suspend fun clearCategoryId(categoryId: Long) {
        webAppDao.clearCategoryId(categoryId)
    }
    
    /**
     * 在事务中删除分类：先清除关联，再删除分类本身
     * 确保数据一致性
     */
    @Transaction
    suspend fun deleteCategoryWithCleanup(categoryId: Long, categoryDao: AppCategoryDao) {
        webAppDao.clearCategoryId(categoryId)
        categoryDao.deleteById(categoryId)
    }
}
