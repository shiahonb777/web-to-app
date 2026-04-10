package com.webtoapp.core.engine.download

import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.engine.EngineType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GeckoEngineDownloaderTest {

    private lateinit var context: Context
    private lateinit var fileManager: EngineFileManager
    private lateinit var downloader: GeckoEngineDownloader

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        fileManager = EngineFileManager(context)
        downloader = GeckoEngineDownloader(context, fileManager)
        fileManager.deleteEngineFiles(EngineType.GECKOVIEW)
        context.getSharedPreferences("engine_manager", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        fileManager.deleteEngineFiles(EngineType.GECKOVIEW)
    }

    @Test
    fun `supported abi list includes expected architectures`() {
        assertThat(GeckoEngineDownloader.SUPPORTED_ABIS)
            .containsExactly("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
    }

    @Test
    fun `getDownloadUrl builds maven path with artifact and version`() {
        val url = downloader.getDownloadUrl("arm64-v8a", "128.0.20240704121409")

        assertThat(url).contains("geckoview-arm64-v8a")
        assertThat(url).contains("128.0.20240704121409")
        assertThat(url).endsWith(".aar")
    }

    @Test
    fun `download returns success immediately when abi already cached with same version`() = runBlocking {
        val abi = fileManager.getDevicePrimaryAbi()
        val abiDir = fileManager.getAbiDir(EngineType.GECKOVIEW, abi)
        abiDir.mkdirs()
        java.io.File(abiDir, "libxul.so").writeBytes(byteArrayOf(1, 2, 3))
        fileManager.setDownloadedVersion(EngineType.GECKOVIEW, "cached-version")

        val success = downloader.download(abi = abi, version = "cached-version")

        assertThat(success).isTrue()
        assertThat(downloader.downloadState.value).isEqualTo(DownloadState.Completed)
    }

    @Test
    fun `download fails fast for unsupported abi`() = runBlocking {
        val success = downloader.download(abi = "mips", version = "1")

        assertThat(success).isFalse()
        val state = downloader.downloadState.value
        assertThat(state).isInstanceOf(DownloadState.Error::class.java)
        assertThat((state as DownloadState.Error).message).contains("Unsupported ABI")
    }

    @Test
    fun `resetState puts downloader back to idle`() {
        downloader.cancelDownload()
        downloader.resetState()

        assertThat(downloader.downloadState.value).isEqualTo(DownloadState.Idle)
    }
}
