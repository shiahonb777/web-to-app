package com.webtoapp.core.php

import android.content.Context
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.sample.SampleProjectExtractor
import com.webtoapp.core.sample.TypedSampleProject

/**
 * project.
 */
object PhpSampleManager {
    
    fun getSampleProjects(): List<TypedSampleProject> {
        val suffix = SampleProjectExtractor.getLanguageSuffix()
        return listOf(
            TypedSampleProject(
                id = "php-laravel$suffix",
                name = AppStringsProvider.current().samplePhpLaravelName,
                description = AppStringsProvider.current().samplePhpLaravelDesc,
                frameworkName = "Laravel",
                icon = "priority_high",
                tags = listOf("Laravel 10", AppStringsProvider.current().sampleTagMvc, "Blade"),
                brandColor = 0xFFFF2D20
            ),
            TypedSampleProject(
                id = "php-slim$suffix",
                name = AppStringsProvider.current().samplePhpSlimName,
                description = AppStringsProvider.current().samplePhpSlimDesc,
                frameworkName = "Slim",
                icon = "priority_low",
                tags = listOf("Slim 4", AppStringsProvider.current().sampleTagRest, AppStringsProvider.current().sampleTagLightweight),
                brandColor = 0xFF74B72E
            ),
            TypedSampleProject(
                id = "php-vanilla$suffix",
                name = AppStringsProvider.current().samplePhpVanillaName,
                description = AppStringsProvider.current().samplePhpVanillaDesc,
                frameworkName = "PHP",
                icon = "php",
                tags = listOf("PHP 8", AppStringsProvider.current().sampleTagNoFramework),
                brandColor = 0xFF777BB4
            )
        )
    }
    
    suspend fun extractSampleProject(
        context: Context,
        projectId: String
    ): Result<String> = SampleProjectExtractor.extractSampleProject(context, projectId)
}