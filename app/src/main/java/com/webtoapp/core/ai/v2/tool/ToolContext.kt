package com.webtoapp.core.ai.v2.tool

import android.content.Context
import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.coding.ProjectFileManager
import com.webtoapp.data.model.ApiKeyConfig
import com.webtoapp.data.model.SavedModel

data class ToolContext(
    val androidContext: Context,
    val sessionId: String,
    val codingType: AiCodingType,
    val fileManager: ProjectFileManager,
    val textModel: SavedModel,
    val apiKey: ApiKeyConfig,
    val imageModel: SavedModel? = null,
    val imageApiKey: ApiKeyConfig? = null
) {
    fun resolveSafePath(rawPath: String): String? {
        if (rawPath.isBlank()) return null
        val cleaned = rawPath.trim().trimStart('/').trim('\\')
        if (cleaned.contains("..") || cleaned.contains(":") || cleaned.length > 500) return null
        return cleaned.replace('\\', '/')
    }
}
