package com.webtoapp.core.python

import android.content.Context
import com.webtoapp.core.i18n.AppStringsProvider
import com.webtoapp.core.sample.SampleProjectExtractor
import com.webtoapp.core.sample.TypedSampleProject

/**
 * project.
 */
object PythonSampleManager {
    
    fun getSampleProjects(): List<TypedSampleProject> {
        val suffix = SampleProjectExtractor.getLanguageSuffix()
        return listOf(
            TypedSampleProject(
                id = "python-flask$suffix",
                name = AppStringsProvider.current().samplePythonFlaskName,
                description = AppStringsProvider.current().samplePythonFlaskDesc,
                frameworkName = "Flask",
                icon = "science",
                tags = listOf("Flask 3", AppStringsProvider.current().sampleTagWsgi, "Jinja2"),
                brandColor = 0xFF333333
            ),
            TypedSampleProject(
                id = "python-fastapi$suffix",
                name = AppStringsProvider.current().samplePythonFastapiName,
                description = AppStringsProvider.current().samplePythonFastapiDesc,
                frameworkName = "FastAPI",
                icon = "bolt",
                tags = listOf("FastAPI", AppStringsProvider.current().sampleTagAsgi, AppStringsProvider.current().sampleTagOpenapi),
                brandColor = 0xFF009688
            ),
            TypedSampleProject(
                id = "python-django$suffix",
                name = AppStringsProvider.current().samplePythonDjangoName,
                description = AppStringsProvider.current().samplePythonDjangoDesc,
                frameworkName = "Django",
                icon = "eco",
                tags = listOf("Django 5", AppStringsProvider.current().sampleTagOrm, AppStringsProvider.current().sampleTagAdmin),
                brandColor = 0xFF092E20
            )
        )
    }
    
    suspend fun extractSampleProject(
        context: Context,
        projectId: String
    ): Result<String> = SampleProjectExtractor.extractSampleProject(context, projectId)
}