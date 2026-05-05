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









class SaveAppUseCase(private val repository: WebAppRepository) {

    companion object {
        private const val TAG = "SaveAppUseCase"
    }




    suspend fun saveIcon(context: Context, iconUri: Uri?): String? {
        return iconUri?.let { uri ->
            withContext(Dispatchers.IO) {
                IconStorage.saveIconFromUri(context, uri)
            }
        }
    }




    suspend fun getCurrentThemeType(context: Context): String {
        return try {
            val themeManager = ThemeManager.getInstance(context)
            themeManager.themeTypeFlow.first().name
        } catch (e: Exception) {
            "AURORA"
        }
    }




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
