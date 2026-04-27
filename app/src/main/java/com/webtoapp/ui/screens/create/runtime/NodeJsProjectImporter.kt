package com.webtoapp.ui.screens.create.runtime

import android.content.Context
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.nodejs.NodeDependencyManager
import com.webtoapp.core.nodejs.NodeRuntime
import com.webtoapp.ui.screens.create.common.ImportedProject
import com.webtoapp.ui.screens.create.common.ProjectImportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class NodeJsProjectImporter(
    private val context: Context,
    private val analyzer: NodeJsProjectImportAnalyzer = NodeJsProjectImportAnalyzer(),
) {
    suspend fun importProject(
        projectDir: File,
        onDependencyDownloadStateChange: (Boolean) -> Unit = {},
    ): ImportedProject<NodeJsProjectImportAnalysis> {
        val analysis = withContext(Dispatchers.IO) {
            analyzer.analyze(projectDir)
        }
        if (analysis.buildMode != com.webtoapp.data.model.NodeJsBuildMode.STATIC &&
            withContext(Dispatchers.IO) { !NodeDependencyManager.isNodeReady(context) }
        ) {
            onDependencyDownloadStateChange(true)
            val success = try {
                NodeDependencyManager.downloadNodeRuntime(context)
            } finally {
                onDependencyDownloadStateChange(false)
            }
            if (!success) {
                throw ProjectImportException(AppStringsProvider.current().njsDownloadFailed)
            }
        }

        val projectId = UUID.randomUUID().toString()
        return withContext(Dispatchers.IO) {
            NodeRuntime(context).createProject(projectId, analysis.projectDir)
            ImportedProject(analysis = analysis, projectId = projectId)
        }
    }
}
