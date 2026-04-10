package com.webtoapp.core.golang

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.sample.SampleProjectExtractor
import com.webtoapp.core.sample.TypedSampleProject

/**
 * Go 示例项目管理器
 */
object GoSampleManager {
    
    fun getSampleProjects(): List<TypedSampleProject> {
        val suffix = SampleProjectExtractor.getLanguageSuffix()
        return listOf(
            TypedSampleProject(
                id = "go-gin$suffix",
                name = Strings.sampleGoGinName,
                description = Strings.sampleGoGinDesc,
                frameworkName = "Gin",
                icon = "cocktail",
                tags = listOf("Gin 1.9", Strings.sampleTagRest, Strings.sampleTagMiddleware),
                brandColor = 0xFF0090FF
            ),
            TypedSampleProject(
                id = "go-fiber$suffix",
                name = Strings.sampleGoFiberName,
                description = Strings.sampleGoFiberDesc,
                frameworkName = "Fiber",
                icon = "rocket",
                tags = listOf("Fiber 2", Strings.sampleTagHighPerf),
                brandColor = 0xFF7B2FBF
            ),
            TypedSampleProject(
                id = "go-echo$suffix",
                name = Strings.sampleGoEchoName,
                description = Strings.sampleGoEchoDesc,
                frameworkName = "Echo",
                icon = "volume",
                tags = listOf("Echo 4", Strings.sampleTagMinimalApi),
                brandColor = 0xFF00BCD4
            )
        )
    }
    
    suspend fun extractSampleProject(
        context: Context,
        projectId: String
    ): Result<String> = SampleProjectExtractor.extractSampleProject(context, projectId)
}
