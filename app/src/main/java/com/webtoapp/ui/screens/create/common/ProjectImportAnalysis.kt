package com.webtoapp.ui.screens.create.common

import java.io.File

interface ProjectImportAnalysis {
    val projectDir: File
    val suggestedAppName: String?
    val framework: String?
    val entryFile: String?
    val envVars: Map<String, String>
    val detectedPort: Int?
}

data class ImportedProject<T : ProjectImportAnalysis>(
    val analysis: T,
    val projectId: String,
)

class ProjectImportException(message: String) : IllegalStateException(message)
