package com.webtoapp.core.nodejs

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
class NodeDependencyManagerTest {

    private lateinit var context: Context
    private var originalLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        originalLocale = Locale.getDefault()
        NodeDependencyManager.setMirrorRegion(null)
        NodeDependencyManager.clearCache(context)
    }

    @After
    fun tearDown() {
        NodeDependencyManager.setMirrorRegion(null)
        Locale.setDefault(originalLocale)
        NodeDependencyManager.clearCache(context)
    }

    @Test
    fun `manual mirror region selection takes precedence`() {
        NodeDependencyManager.setMirrorRegion(NodeDependencyManager.MirrorRegion.CN)
        val cnConfig = NodeDependencyManager.getMirrorConfig()
        assertThat(NodeDependencyManager.getMirrorRegion()).isEqualTo(NodeDependencyManager.MirrorRegion.CN)
        assertThat(cnConfig.nodeUrls.size).isGreaterThan(1)

        NodeDependencyManager.setMirrorRegion(NodeDependencyManager.MirrorRegion.GLOBAL)
        val globalConfig = NodeDependencyManager.getMirrorConfig()
        assertThat(NodeDependencyManager.getMirrorRegion()).isEqualTo(NodeDependencyManager.MirrorRegion.GLOBAL)
        assertThat(globalConfig.nodeUrls).hasSize(1)
    }

    @Test
    fun `auto mirror region follows default locale language`() {
        NodeDependencyManager.setMirrorRegion(null)

        Locale.setDefault(Locale.CHINESE)
        assertThat(NodeDependencyManager.getMirrorRegion()).isEqualTo(NodeDependencyManager.MirrorRegion.CN)

        Locale.setDefault(Locale.ENGLISH)
        assertThat(NodeDependencyManager.getMirrorRegion()).isEqualTo(NodeDependencyManager.MirrorRegion.GLOBAL)
    }

    @Test
    fun `node directory helpers create expected paths`() {
        val depsDir = NodeDependencyManager.getDepsDir(context)
        val nodeDir = NodeDependencyManager.getNodeDir(context)
        val projectDir = NodeDependencyManager.getNodeProjectsDir(context)

        assertThat(depsDir.exists()).isTrue()
        assertThat(nodeDir.exists()).isTrue()
        assertThat(projectDir.exists()).isTrue()
        assertThat(nodeDir.absolutePath).contains("/nodejs_deps/node/")
    }

    @Test
    fun `getNodeLibraryPath returns null when runtime is not ready`() {
        val path = NodeDependencyManager.getNodeLibraryPath(context)

        assertThat(path).isNull()
        assertThat(NodeDependencyManager.isNodeReady(context)).isFalse()
    }

    @Test
    fun `node runtime becomes ready when downloaded library exists`() {
        val lib = File(NodeDependencyManager.getNodeDir(context), NodeDependencyManager.NODE_BINARY_NAME).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1))
        }

        val path = NodeDependencyManager.getNodeLibraryPath(context)

        assertThat(NodeDependencyManager.isNodeReady(context)).isTrue()
        assertThat(path).isEqualTo(lib.absolutePath)
    }
}
