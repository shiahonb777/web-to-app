package com.webtoapp.ui.screens.create.runtime

import android.content.Context
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.php.PhpAppRuntime
import com.webtoapp.core.wordpress.WordPressDependencyManager
import com.webtoapp.ui.screens.create.common.ImportedProject
import com.webtoapp.ui.screens.create.common.ProjectImportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PhpProjectImporter(
    private val context: Context,
    private val analyzer: PhpProjectImportAnalyzer = PhpProjectImportAnalyzer(),
) {
    suspend fun importProject(
        projectDir: File,
        onDependencyDownloadStateChange: (Boolean) -> Unit = {},
    ): ImportedProject<PhpProjectImportAnalysis> {
        val analysis = withContext(Dispatchers.IO) {
            analyzer.analyze(projectDir)
        }
        if (withContext(Dispatchers.IO) { !WordPressDependencyManager.isPhpReady(context) }) {
            onDependencyDownloadStateChange(true)
            val success = try {
                WordPressDependencyManager.downloadAllDependencies(context)
            } finally {
                onDependencyDownloadStateChange(false)
            }
            if (!success) {
                throw ProjectImportException(AppStringsProvider.current().wpDownloadFailed)
            }
        }

        val projectId = UUID.randomUUID().toString()
        return withContext(Dispatchers.IO) {
            PhpAppRuntime(context).createProject(projectId, analysis.projectDir)
            ImportedProject(analysis = analysis, projectId = projectId)
        }
    }
}
