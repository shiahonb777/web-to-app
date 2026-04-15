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
        assertThat(pythonDir.absolutePath).isEqualTo(File(depsDir, "python").absolutePath)
    }

    @Test
    fun `python readiness is false when runtime binaries are absent`() {
        assertThat(PythonDependencyManager.isPythonReady(context)).isFalse()
        assertThat(PythonDependencyManager.getPythonExecutablePath(context))
            .isEqualTo(File(PythonDependencyManager.getPythonDir(context), "bin/python3").absolutePath)
        assertThat(PythonDependencyManager.getPipPath(context))
            .isEqualTo(File(PythonDependencyManager.getPythonDir(context), "bin/pip3").absolutePath)
    }

    @Test
    fun `python readiness becomes true when real binary exists in downloaded dir`() {
        val binary = File(PythonDependencyManager.getPythonDir(context), "bin/python3.12").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(1024 * 1024 + 1) { 1 })
            setExecutable(true)
        }

        assertThat(PythonDependencyManager.isPythonReady(context)).isTrue()
        assertThat(PythonDependencyManager.getPythonExecutablePath(context)).isEqualTo(binary.absolutePath)
    }
}
