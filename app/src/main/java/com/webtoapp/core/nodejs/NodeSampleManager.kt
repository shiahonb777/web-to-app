package com.webtoapp.core.nodejs

import android.content.Context
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.sample.SampleProjectExtractor
import com.webtoapp.core.sample.TypedSampleProject

/**
 * project.
 */
object NodeSampleManager {
    
    fun getSampleProjects(): List<TypedSampleProject> {
        val suffix = SampleProjectExtractor.getLanguageSuffix()
        return listOf(
            TypedSampleProject(
                id = "nodejs-express$suffix",
                name = AppStringsProvider.current().sampleNodeExpressName,
                description = AppStringsProvider.current().sampleNodeExpressDesc,
                frameworkName = "Express",
                icon = "check_circle",
                tags = listOf("Express 4", "Todo App", "CRUD"),
                brandColor = 0xFF259D3D
            ),
            TypedSampleProject(
                id = "nodejs-fastify$suffix",
                name = AppStringsProvider.current().sampleNodeFastifyName,
                description = AppStringsProvider.current().sampleNodeFastifyDesc,
                frameworkName = "Fastify",
                icon = "analytics",
                tags = listOf("Fastify 4", "Dashboard", "Real-time"),
                brandColor = 0xFF000000
            ),
            TypedSampleProject(
                id = "nodejs-koa$suffix",
                name = AppStringsProvider.current().sampleNodeKoaName,
                description = AppStringsProvider.current().sampleNodeKoaDesc,
                frameworkName = "Koa",
                icon = "edit_note",
                tags = listOf("Koa 2", "Markdown", "Notes"),
                brandColor = 0xFF33333D
            )
        )
    }
    
    suspend fun extractSampleProject(
        context: Context,
        projectId: String
    ): Result<String> = SampleProjectExtractor.extractSampleProject(context, projectId)
}