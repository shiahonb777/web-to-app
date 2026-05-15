package com.webtoapp.core.linux

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.nodejs.NodeDependencyManager
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LinuxEnvironmentManagerTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        LinuxEnvironmentManager.resetForTests()
        LocalBuildEnvironment.getRootDir(context).deleteRecursively()
        NodeDependencyManager.getDepsDir(context).deleteRecursively()
    }

    @After
    fun tearDown() {
        LinuxEnvironmentManager.resetForTests()
        LocalBuildEnvironment.getRootDir(context).deleteRecursively()
        NodeDependencyManager.getDepsDir(context).deleteRecursively()
    }

    @Test
    fun `environment info reports installed when node launcher and npm cli exist`() {
        prepareNodeRuntime()
        LocalBuildEnvironment.getLauncherPath(context).apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(64))
            setExecutable(true)
        }
        LocalBuildEnvironment.getNpmCliPath(context).apply {
            parentFile?.mkdirs()
            writeText("console.log('npm')")
        }

        val info = kotlinx.coroutines.runBlocking {
            LinuxEnvironmentManager.getInstance(context).getEnvironmentInfo()
        }

        assertThat(info.nodeReady).isTrue()
        assertThat(info.npmReady).isTrue()
        assertThat(info.isInstalled).isTrue()
    }

    @Test
    fun `environment info reports node ready but npm missing`() {
        prepareNodeRuntime()
        LocalBuildEnvironment.getLauncherPath(context).apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(64))
            setExecutable(true)
        }

        val info = kotlinx.coroutines.runBlocking {
            LinuxEnvironmentManager.getInstance(context).getEnvironmentInfo()
        }

        assertThat(info.nodeReady).isTrue()
        assertThat(info.npmReady).isFalse()
        assertThat(info.isInstalled).isFalse()
    }

    private fun prepareNodeRuntime() {
        File(NodeDependencyManager.getNodeDir(context), NodeDependencyManager.NODE_BINARY_NAME).apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(1024))
        }
    }
}
