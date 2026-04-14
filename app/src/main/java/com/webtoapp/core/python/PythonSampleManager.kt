package com.webtoapp.core.python

import android.content.Context
import com.webtoapp.core.i18n.Strings
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
                name = Strings.samplePythonFlaskName,
                description = Strings.samplePythonFlaskDesc,
                frameworkName = "Flask",
                icon = "science",
                tags = listOf("Flask 3", Strings.sampleTagWsgi, "Jinja2"),
                brandColor = 0xFF333333
            ),
            TypedSampleProject(
                id = "python-fastapi$suffix",
                name = Strings.samplePythonFastapiName,
                description = Strings.samplePythonFastapiDesc,
                frameworkName = "FastAPI",
                icon = "bolt",
                tags = listOf("FastAPI", Strings.sampleTagAsgi, Strings.sampleTagOpenapi),
                brandColor = 0xFF009688
            ),
            TypedSampleProject(
                id = "python-django$suffix",
                name = Strings.samplePythonDjangoName,
                description = Strings.samplePythonDjangoDesc,
                frameworkName = "Django",
                icon = "eco",
                tags = listOf("Django 5", Strings.sampleTagOrm, Strings.sampleTagAdmin),
                brandColor = 0xFF092E20
            )
        )
    }
    
    suspend fun extractSampleProject(
        context: Context,
        projectId: String
    ): Result<String> = SampleProjectExtractor.extractSampleProject(context, projectId)
}