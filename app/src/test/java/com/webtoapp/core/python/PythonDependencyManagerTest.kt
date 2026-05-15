package com.webtoapp.core.python

import android.content.Context
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class PythonDependencyManagerTest {

    @Rule @JvmField
    val koinRule = com.webtoapp.util.KoinCleanupRule()

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

            writeBytes(ByteArray(1024 * 1024 + 1) { 0 })
            setExecutable(true)
        }
        File(PythonDependencyManager.getPythonDir(context), "lib/${PythonDependencyManager.getMuslLinkerName(PythonDependencyManager.getDeviceAbi())}").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(2048) { 1 })
            setExecutable(true)
        }

        assertThat(PythonDependencyManager.isPythonReady(context)).isTrue()
        assertThat(PythonDependencyManager.getPythonExecutablePath(context)).isEqualTo(binary.absolutePath)
    }

    @Test
    fun `python readiness stays false when musl linker is missing`() {
        File(PythonDependencyManager.getPythonDir(context), "bin/python3").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(1024 * 1024 + 1) { 0 })
            setExecutable(true)
        }

        assertThat(PythonDependencyManager.isPythonReady(context)).isFalse()
        assertThat(PythonDependencyManager.getMuslLinkerPath(context)).isNull()
    }

    @Test
    fun `builder musl linker path ignores downloaded non native linker`() {
        File(PythonDependencyManager.getPythonDir(context), "lib/${PythonDependencyManager.getMuslLinkerName(PythonDependencyManager.getDeviceAbi())}").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(2048) { 1 })
            setExecutable(true)
        }

        assertThat(PythonDependencyManager.getMuslLinkerPath(context)).isNotNull()
        assertThat(PythonDependencyManager.getBuilderMuslLinkerPath(context)).isNull()
    }

    @Test
    fun `sanitize requirements strips android hostile packages and uvicorn extras`() {
        val original = """
            fastapi==0.99.1
            uvicorn[standard]==0.27.0
            httptools==0.6.0
            watchfiles>=0.21
            uvloop==0.19.0
            pydantic==1.10.16
        """.trimIndent()

        val sanitized = PythonDependencyManager.sanitizeRequirementsForAndroid(original)

        assertThat(sanitized).contains("fastapi==0.99.1")
        assertThat(sanitized).contains("uvicorn==0.27.0")
        assertThat(sanitized).contains("pydantic==1.10.16")
        assertThat(sanitized).doesNotContain("uvicorn[standard]")
        assertThat(sanitized).doesNotContain("httptools")
        assertThat(sanitized).doesNotContain("watchfiles")
        assertThat(sanitized).doesNotContain("uvloop")
    }

    @Test
    fun `has installed packages ignores empty directory tree`() {
        val emptyDir = tempDir("empty-pypackages")
        File(emptyDir, "nested").mkdirs()

        assertThat(PythonDependencyManager.hasInstalledPackages(emptyDir)).isFalse()

        File(emptyDir, "nested/pkg.py").apply {
            parentFile?.mkdirs()
            writeText("print('ok')")
        }
        assertThat(PythonDependencyManager.hasInstalledPackages(emptyDir)).isTrue()
    }

    private fun tempDir(name: String): File {
        return File(context.cacheDir, "python-dep-test-$name-${System.nanoTime()}").apply { mkdirs() }
    }
}
