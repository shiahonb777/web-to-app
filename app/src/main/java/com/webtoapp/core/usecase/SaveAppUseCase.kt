package com.webtoapp.core.usecase

import android.content.Context
import android.net.Uri
import com.webtoapp.core.common.AppResult
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import com.webtoapp.data.repository.WebAppRepository
import com.webtoapp.ui.theme.ThemeManager
import com.webtoapp.util.IconStorage
import com.webtoapp.util.HtmlStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 通用保存应用 UseCase
 * 抽取 MainViewModel 中所有 save*App / update*App 的公共逻辑：
 * 1. 保存图标
 * 2. 获取当前主题
 * 3. 构造 WebApp 实体
 * 4. 持久化到数据库
 */
class SaveAppUseCase(private val repository: WebAppRepository) {

    companion object {
        private const val TAG = "SaveAppUseCase"
    }

    /**
     * 保存图标到本地存储
     */
    suspend fun saveIcon(context: Context, iconUri: Uri?): String? {
        return iconUri?.let { uri ->
            withContext(Dispatchers.IO) {
                IconStorage.saveIconFromUri(context, uri)
            }
        }
    }

    /**
     * 获取当前主题类型名称
     */
    suspend fun getCurrentThemeType(context: Context): String {
        return try {
            val themeManager = ThemeManager.getInstance(context)
            themeManager.themeTypeFlow.first().name
        } catch (e: Exception) {
            "AURORA"
        }
    }

    /**
     * 创建新应用
     */
    suspend fun createApp(
        context: Context,
        name: String,
        defaultName: String,
        appType: AppType,
        iconUri: Uri?,
        categoryId: Long?,
        configure: WebApp.() -> WebApp = { this }
    ): AppResult<Long> = AppResult.suspendRunCatching(
        errorMessage = Strings.creationFailed.replaceFirst("%s", "")
    ) {
        val savedIconPath = saveIcon(context, iconUri)
        val currentThemeType = getCurrentThemeType(context)
        
        val webApp = WebApp(
            name = name.ifBlank { defaultName },
            url = "",
            iconPath = savedIconPath,
            appType = appType,
            themeType = currentThemeType,
            categoryId = categoryId?.takeIf { it > 0 }
        ).configure()
        
        withContext(Dispatchers.IO) {
            repository.createWebApp(webApp)
        }
    }

    /**
     * 更新已有应用
     */
    suspend fun updateApp(
        context: Context,
        appId: Long,
        name: String,
        iconUri: Uri?,
        configure: WebApp.() -> WebApp = { this }
    ): AppResult<Unit> = AppResult.suspendRunCatching(
        errorMessage = Strings.updateFailed.replaceFirst("%s", "")
    ) {
        val existingApp = withContext(Dispatchers.IO) {
            repository.getWebApp(appId)
        } ?: throw Exception("App not found")
        
        val savedIconPath = saveIcon(context, iconUri) ?: existingApp.iconPath
        
        val updatedApp = existingApp.copy(
            name = name.ifBlank { existingApp.name },
            iconPath = savedIconPath,
            updatedAt = System.currentTimeMillis()
        ).configure()
        
        withContext(Dispatchers.IO) {
            repository.updateWebApp(updatedApp)
        }
    }

    /**
     * 处理并保存 HTML 文件，返回保存后的文件列表
     */
    suspend fun processHtmlFiles(
        context: Context,
        files: List<HtmlFile>,
        projectId: String,
        processor: suspend (Context, List<HtmlFile>, String) -> List<HtmlFile>
    ): AppResult<List<HtmlFile>> = AppResult.suspendRunCatching(
        errorMessage = Strings.saveFailedCannotProcessHtml
    ) {
        val savedFiles = processor(context, files, projectId)
        
        if (savedFiles.none { it.type == HtmlFileType.HTML || it.name.endsWith(".html", ignoreCase = true) }) {
            withContext(Dispatchers.IO) { HtmlStorage.deleteProject(context, projectId) }
            throw Exception(Strings.saveFailedCannotProcessHtml)
        }
        
        withContext(Dispatchers.IO) { HtmlStorage.clearTempFiles(context) }
        savedFiles
    }
}
