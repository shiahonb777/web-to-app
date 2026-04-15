package com.webtoapp.ui.screens.create.runtime

import android.content.Context
import com.webtoapp.core.python.PythonRuntime
import com.webtoapp.ui.screens.create.common.ImportedProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class PythonProjectImporter(
    private val context: Context,
    private val analyzer: PythonProjectImportAnalyzer = PythonProjectImportAnalyzer(),
) {
    suspend fun importProject(projectDir: File): ImportedProject<PythonProjectImportAnalysis> {
        val analysis = withContext(Dispatchers.IO) {
            analyzer.analyze(projectDir)
        }
        val projectId = UUID.randomUUID().toString()
        return withContext(Dispatchers.IO) {
            PythonRuntime(context).createProject(projectId, analysis.projectDir)
            ImportedProject(analysis = analysis, projectId = projectId)
        }
    }
}
