package com.webtoapp.ui.navigation

import androidx.navigation.NavHostController
import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.coding.ProjectFile
import java.io.File
import java.net.URLEncoder

internal object AiExportCoordinator {
    fun exportHtmlProject(
        navController: NavHostController,
        files: List<ProjectFile>,
        projectName: String,
    ) {
        val tempDir = writeProjectFiles(
            navController = navController,
            cacheDirName = "ai_html_export",
            files = files,
        )
        navController.navigate(buildCreateHtmlImportRoute(tempDir, projectName))
    }

    fun exportCodingProject(
        navController: NavHostController,
        files: List<ProjectFile>,
        projectName: String,
        codingType: AiCodingType,
    ) {
        val tempDir = writeProjectFiles(
            navController = navController,
            cacheDirName = "ai_coding_export",
            files = files,
        )

        when (codingType) {
            AiCodingType.HTML -> navController.navigate(buildCreateHtmlImportRoute(tempDir, projectName))
            AiCodingType.FRONTEND -> navController.navigate(Routes.CREATE_FRONTEND_APP)
            AiCodingType.NODEJS -> navController.navigate(Routes.CREATE_NODEJS_APP)
            AiCodingType.WORDPRESS -> navController.navigate(Routes.CREATE_WORDPRESS_APP)
            AiCodingType.PHP -> navController.navigate(Routes.CREATE_PHP_APP)
            AiCodingType.PYTHON -> navController.navigate(Routes.CREATE_PYTHON_APP)
            AiCodingType.GO -> navController.navigate(Routes.CREATE_GO_APP)
        }
    }

    private fun writeProjectFiles(
        navController: NavHostController,
        cacheDirName: String,
        files: List<ProjectFile>,
    ): File {
        val tempDir = File(navController.context.cacheDir, cacheDirName).apply {
            if (exists()) {
                deleteRecursively()
            }
            mkdirs()
        }

        files.forEach { file ->
            File(tempDir, file.name).writeText(file.content)
        }
        return tempDir
    }

    private fun buildCreateHtmlImportRoute(tempDir: File, projectName: String): String {
        return "${Routes.CREATE_HTML_APP}?importDir=${
            URLEncoder.encode(tempDir.absolutePath, "UTF-8")
        }&projectName=${
            URLEncoder.encode(projectName, "UTF-8")
        }"
    }
}
