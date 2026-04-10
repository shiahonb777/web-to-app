package com.webtoapp.core.python

import android.content.Context
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PythonDependencyManagerTest {

    private lateinit var context: Context
    private var originalLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        originalLocale = Locale.getDefault()
        PythonDependencyManager.setMirrorRegion(null)
        PythonDependencyManager.clearCache(context)
    }

    @After
    fun tearDown() {
        PythonDependencyManager.setMirrorRegion(null)
        Locale.setDefault(originalLocale)
        PythonDependencyManager.clearCache(context)
    }

    @Test
    fun `manual and automatic mirror region selection works`() {
        PythonDependencyManager.setMirrorRegion(PythonDependencyManager.MirrorRegion.CN)
        assertThat(PythonDependencyManager.getMirrorRegion()).isEqualTo(PythonDependencyManager.MirrorRegion.CN)

        PythonDependencyManager.setMirrorRegion(PythonDependencyManager.MirrorRegion.GLOBAL)
        assertThat(PythonDependencyManager.getMirrorRegion()).isEqualTo(PythonDependencyManager.MirrorRegion.GLOBAL)

        PythonDependencyManager.setMirrorRegion(null)
        Locale.setDefault(Locale.CHINESE)
        assertThat(PythonDependencyManager.getMirrorRegion()).isEqualTo(PythonDependencyManager.MirrorRegion.CN)
        Locale.setDefault(Locale.ENGLISH)
        assertThat(PythonDependencyManager.getMirrorRegion()).isEqualTo(PythonDependencyManager.MirrorRegion.GLOBAL)
    }

    @Test
    fun `directory helpers create expected project and runtime locations`() {
        val depsDir = PythonDependencyManager.getDepsDir(context)
        val pythonDir = PythonDependencyManager.getPythonDir(context)
        val projectsDir = PythonDependencyManager.getProjectsDir(context)

        assertThat(depsDir.exists()).isTrue()
        assertThat(pythonDir.exists()).isTrue()
        assertThat(projectsDir.exists()).isTrue()
        assertThat(pythonDir.absolutePath).contains("/python_deps/python")
    }

    @Test
    fun `python readiness is false when runtime binaries are absent`() {
        assertThat(PythonDependencyManager.isPythonReady(context)).isFalse()
        assertThat(PythonDependencyManager.getPythonExecutablePath(context))
            .contains("/python_deps/python/bin/python3")
        assertThat(PythonDependencyManager.getPipPath(context))
            .contains("/python_deps/python/bin/pip3")
    }

    @Test
    fun `python readiness becomes true when binary exists in downloaded dir`() {
        val binary = File(PythonDependencyManager.getPythonDir(context), "bin/python3").apply {
            parentFile?.mkdirs()
            writeText("#!/system/bin/sh")
            setExecutable(true)
        }

        assertThat(PythonDependencyManager.isPythonReady(context)).isTrue()
        assertThat(PythonDependencyManager.getPythonExecutablePath(context)).isEqualTo(binary.absolutePath)
    }
}
