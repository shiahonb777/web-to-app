package com.webtoapp.core.php

import android.content.Context
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.sample.SampleProjectExtractor
import com.webtoapp.core.sample.TypedSampleProject

/**
 * PHP 示例项目管理器
 */
object PhpSampleManager {
    
    fun getSampleProjects(): List<TypedSampleProject> {
        val suffix = SampleProjectExtractor.getLanguageSuffix()
        return listOf(
            TypedSampleProject(
                id = "php-laravel$suffix",
                name = Strings.samplePhpLaravelName,
                description = Strings.samplePhpLaravelDesc,
                frameworkName = "Laravel",
                icon = "priority_high",
                tags = listOf("Laravel 10", Strings.sampleTagMvc, "Blade"),
                brandColor = 0xFFFF2D20
            ),
            TypedSampleProject(
                id = "php-slim$suffix",
                name = Strings.samplePhpSlimName,
                description = Strings.samplePhpSlimDesc,
                frameworkName = "Slim",
                icon = "priority_low",
                tags = listOf("Slim 4", Strings.sampleTagRest, Strings.sampleTagLightweight),
                brandColor = 0xFF74B72E
            ),
            TypedSampleProject(
                id = "php-vanilla$suffix",
                name = Strings.samplePhpVanillaName,
                description = Strings.samplePhpVanillaDesc,
                frameworkName = "PHP",
                icon = "php",
                tags = listOf("PHP 8", Strings.sampleTagNoFramework),
                brandColor = 0xFF777BB4
            )
        )
    }
    
    suspend fun extractSampleProject(
        context: Context,
        projectId: String
    ): Result<String> = SampleProjectExtractor.extractSampleProject(context, projectId)
}
