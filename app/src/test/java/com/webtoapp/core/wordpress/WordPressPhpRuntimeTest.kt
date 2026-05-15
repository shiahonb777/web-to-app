package com.webtoapp.core.wordpress

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WordPressPhpRuntimeTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        WordPressDependencyManager.getDepsDir(context).deleteRecursively()
    }

    @Test
    fun `wordpress runtime disables native headers so router polyfill captures redirects and cookies`() {
        val runtime = WordPressPhpRuntime(context)
        val method = WordPressPhpRuntime::class.java.getDeclaredMethod(
            "buildPhpCommand",
            String::class.java,
            Int::class.javaPrimitiveType,
            String::class.java,
            String::class.java
        )
        method.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val command = method.invoke(
            runtime,
            "/tmp/php",
            18500,
            context.filesDir.absolutePath,
            File(context.cacheDir, "php_router_server.php").absolutePath
        ) as List<String>

        assertThat(command).contains("disable_functions=header,headers_list,headers_sent,header_remove,setcookie,setrawcookie")
    }

    @Test
    fun `php ready becomes true when downloaded php binary exists and is executable`() {
        val phpBinary = File(WordPressDependencyManager.getPhpDir(context), "php").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(1024))
            setExecutable(true)
        }

        assertThat(WordPressDependencyManager.isPhpReady(context)).isTrue()
        assertThat(WordPressDependencyManager.getPhpExecutablePath(context)).isEqualTo(phpBinary.absolutePath)
    }

    @Test
    fun `php ready repairs non executable downloaded php binary`() {
        val phpBinary = File(WordPressDependencyManager.getPhpDir(context), "php").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(1024))
            setReadable(true, false)
            setExecutable(false, false)
        }

        Files.getPosixFilePermissions(phpBinary.toPath())

        assertThat(phpBinary.canExecute()).isFalse()
        assertThat(WordPressDependencyManager.isPhpReady(context)).isTrue()
        assertThat(WordPressDependencyManager.getPhpExecutablePath(context)).isEqualTo(phpBinary.absolutePath)
        assertThat(phpBinary.canExecute()).isTrue()
    }
}
