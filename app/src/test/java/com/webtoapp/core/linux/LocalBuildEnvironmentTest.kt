package com.webtoapp.core.linux

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.lang.reflect.Method
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocalBuildEnvironmentTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        LocalBuildEnvironment.getRootDir(context).deleteRecursively()
    }

    @Test
    fun `cli paths point to unpacked package locations`() {
        assertThat(LocalBuildEnvironment.getNpmCliPath(context).path).endsWith("npm/package/bin/npm-cli.js")
        assertThat(LocalBuildEnvironment.getPnpmCliPath(context).path).endsWith("pnpm/package/bin/pnpm.cjs")
        assertThat(LocalBuildEnvironment.getYarnCliPath(context).path).endsWith("yarn/package/bin/yarn.js")
    }

    @Test
    fun `parse package script returns named script`() {
        val projectDir = File(context.cacheDir, "pkg-${System.nanoTime()}").apply { mkdirs() }
        val packageJson = File(projectDir, "package.json").apply {
            writeText(
                """
                {
                  "scripts": {
                    "build": "vite build",
                    "dev": "vite"
                  }
                }
                """.trimIndent()
            )
        }

        val method: Method = LocalBuildEnvironment::class.java.getDeclaredMethod(
            "parsePackageScript",
            File::class.java,
            String::class.java
        )
        method.isAccessible = true
        val buildScript = method.invoke(LocalBuildEnvironment, packageJson, "build") as String?
        val missingScript = method.invoke(LocalBuildEnvironment, packageJson, "lint") as String?

        assertThat(buildScript).isEqualTo("vite build")
        assertThat(missingScript).isNull()
    }
}
