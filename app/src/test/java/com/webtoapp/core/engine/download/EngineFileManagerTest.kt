package com.webtoapp.core.engine.download

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.engine.EngineType
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EngineFileManagerTest {

    private lateinit var context: Context
    private lateinit var manager: EngineFileManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        manager = EngineFileManager(context)
        clearState()
    }

    @After
    fun tearDown() {
        clearState()
    }

    @Test
    fun `system webview is treated as already available`() {
        assertThat(manager.isEngineDownloaded(EngineType.SYSTEM_WEBVIEW)).isTrue()
    }

    @Test
    fun `isEngineDownloaded and isAbiDownloaded reflect extracted so files`() {
        val abi = "arm64-v8a"
        val abiDir = manager.getAbiDir(EngineType.GECKOVIEW, abi)
        File(abiDir, "libxul.so").writeBytes(byteArrayOf(1, 2, 3))

        assertThat(manager.isAbiDownloaded(EngineType.GECKOVIEW, abi)).isTrue()
        assertThat(manager.isEngineDownloaded(EngineType.GECKOVIEW)).isTrue()
    }

    @Test
    fun `downloaded version can be persisted and cleared`() {
        manager.setDownloadedVersion(EngineType.GECKOVIEW, "128.0.0")
        assertThat(manager.getDownloadedVersion(EngineType.GECKOVIEW)).isEqualTo("128.0.0")

        manager.deleteEngineFiles(EngineType.GECKOVIEW)

        assertThat(manager.getDownloadedVersion(EngineType.GECKOVIEW)).isNull()
    }

    @Test
    fun `listEngineNativeLibs and size include downloaded files`() {
        val abiDirA = manager.getAbiDir(EngineType.GECKOVIEW, "arm64-v8a")
        val abiDirB = manager.getAbiDir(EngineType.GECKOVIEW, "x86_64")
        File(abiDirA, "libxul.so").writeBytes(byteArrayOf(1, 2, 3, 4))
        File(abiDirA, "libmozglue.so").writeBytes(byteArrayOf(9))
        File(abiDirB, "libxul.so").writeBytes(byteArrayOf(5, 6))

        val libs = manager.listEngineNativeLibs(EngineType.GECKOVIEW)

        assertThat(libs.keys).containsExactly("arm64-v8a", "x86_64")
        assertThat(libs["arm64-v8a"]!!.map { it.name }).containsAtLeast("libxul.so", "libmozglue.so")
        assertThat(manager.getEngineSize(EngineType.GECKOVIEW)).isEqualTo(7L)
    }

    @Test
    fun `deleteEngineFiles removes gecko directory recursively`() {
        val engineDir = manager.getEngineDir(EngineType.GECKOVIEW)
        File(engineDir, "lib/arm64-v8a/libxul.so").apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1))
        }
        assertThat(engineDir.exists()).isTrue()

        val deleted = manager.deleteEngineFiles(EngineType.GECKOVIEW)

        assertThat(deleted).isTrue()
        assertThat(engineDir.exists()).isFalse()
    }

    private fun clearState() {
        manager.deleteEngineFiles(EngineType.GECKOVIEW)
        context.getSharedPreferences("engine_manager", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }
}
