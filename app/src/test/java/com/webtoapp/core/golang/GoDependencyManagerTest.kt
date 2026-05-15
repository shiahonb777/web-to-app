package com.webtoapp.core.golang

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GoDependencyManagerTest {

    @Rule @JvmField
    val koinRule = com.webtoapp.util.KoinCleanupRule()

    @Test
    fun `parseElf returns invalid for missing or non elf files`() {
        val missing = File("/tmp/non-existent-go-elf-${System.nanoTime()}")
        val randomFile = Files.createTempFile("go-elf-random", ".bin").toFile().apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        }

        try {
            assertThat(GoDependencyManager.parseElf(missing).isValid).isFalse()
            assertThat(GoDependencyManager.parseElf(randomFile).isValid).isFalse()
        } finally {
            randomFile.delete()
        }
    }

    @Test
    fun `parseElf recognizes arm64 and x86 architectures`() {
        val arm64 = createElfFile(machine = 0xB7, elfClass = 2)
        val x86 = createElfFile(machine = 0x03, elfClass = 1)

        try {
            val arm64Info = GoDependencyManager.parseElf(arm64)
            val x86Info = GoDependencyManager.parseElf(x86)

            assertThat(arm64Info.isValid).isTrue()
            assertThat(arm64Info.is64Bit).isTrue()
            assertThat(arm64Info.abiName).isEqualTo("arm64-v8a")

            assertThat(x86Info.isValid).isTrue()
            assertThat(x86Info.is64Bit).isFalse()
            assertThat(x86Info.abiName).isEqualTo("x86")
        } finally {
            arm64.delete()
            x86.delete()
        }
    }

    @Test
    fun `isCompatible accepts exact match and supported fallback pairs`() {
        val deviceAbi = GoDependencyManager.getDeviceAbi()
        val exact = GoDependencyManager.ElfInfo(isValid = true, abiName = deviceAbi)

        assertThat(GoDependencyManager.isCompatible(exact)).isTrue()
        assertThat(GoDependencyManager.isCompatible(GoDependencyManager.ElfInfo(isValid = false))).isFalse()

        val arm32 = GoDependencyManager.ElfInfo(isValid = true, abiName = "armeabi-v7a")
        val x86 = GoDependencyManager.ElfInfo(isValid = true, abiName = "x86")

        when (deviceAbi) {
            "arm64-v8a" -> {
                assertThat(GoDependencyManager.isCompatible(arm32)).isTrue()
                assertThat(GoDependencyManager.isCompatible(x86)).isFalse()
            }
            "x86_64" -> {
                assertThat(GoDependencyManager.isCompatible(x86)).isTrue()
                assertThat(GoDependencyManager.isCompatible(arm32)).isFalse()
            }
            "armeabi-v7a" -> {
                assertThat(GoDependencyManager.isCompatible(arm32)).isTrue()
                assertThat(GoDependencyManager.isCompatible(x86)).isFalse()
            }
            "x86" -> {
                assertThat(GoDependencyManager.isCompatible(x86)).isTrue()
                assertThat(GoDependencyManager.isCompatible(arm32)).isFalse()
            }
            else -> {
                assertThat(GoDependencyManager.isCompatible(arm32)).isFalse()
                assertThat(GoDependencyManager.isCompatible(x86)).isFalse()
            }
        }
    }

    @Test
    fun `prepareBinary finds matching binary in nested project directory`() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val projectDir = Files.createTempDirectory("go-project").toFile()
        val nestedDir = File(projectDir, "dist/release").apply { mkdirs() }
        File(nestedDir, "server").apply {
            writeBytes(createElfHeader(machineForCurrentAbi(), elfClassForCurrentAbi()) + ByteArray(2048) { 7 })
            setExecutable(true)
        }

        try {
            val prepared = GoDependencyManager.prepareBinary(context, projectDir, "server")
            assertThat(prepared).isNotNull()
            assertThat(File(prepared!!).exists()).isTrue()
            assertThat(File(prepared).canExecute()).isTrue()
        } finally {
            projectDir.deleteRecursively()
            GoDependencyManager.clearCache(context)
        }
    }

    @Test
    fun `detectAnyCompatibleBinary finds binary under abi build directory`() {
        val projectDir = Files.createTempDirectory("go-project-abi").toFile()
        val abiDir = File(projectDir, "build/${GoDependencyManager.getDeviceAbi()}").apply { mkdirs() }
        val binary = File(abiDir, "server").apply {
            writeBytes(createElfHeader(machineForCurrentAbi(), elfClassForCurrentAbi()) + ByteArray(2048) { 3 })
            setExecutable(true)
        }

        try {
            val detected = GoDependencyManager.detectAnyCompatibleBinary(projectDir)
            assertThat(detected).isNotNull()
            assertThat(detected!!.absolutePath).isEqualTo(binary.absolutePath)
        } finally {
            projectDir.deleteRecursively()
        }
    }

    @Test
    fun `buildBinaryCommand runs executable through bundled loader`() {
        val context = RuntimeEnvironment.getApplication()
        val command = GoDependencyManager.buildBinaryCommand(
            context = context,
            executablePath = "/data/user/0/com.webtoapp/files/go_bins/server",
            arguments = listOf("mod", "download")
        )

        assertThat(command).containsExactly(
            File(context.applicationInfo.nativeLibraryDir, "libgo_exec_loader.so").absolutePath,
            "/data/user/0/com.webtoapp/files/go_bins/server",
            "mod",
            "download"
        ).inOrder()
    }

    @Test
    fun `configureGoBinaryEnvironment sets minimal execution env`() {
        val context = RuntimeEnvironment.getApplication()
        val env = linkedMapOf<String, String>(
            "PATH" to "/usr/bin",
            "LD_LIBRARY_PATH" to "/system/lib64"
        )

        GoDependencyManager.configureGoBinaryEnvironment(
            context = context,
            processEnv = env,
            libraryPath = "/toolchain/lib",
            additionalPathEntries = listOf("/custom/bin")
        )

        assertThat(env["HOME"]).isEqualTo(context.filesDir.absolutePath)
        assertThat(env["TMPDIR"]).isEqualTo(context.cacheDir.absolutePath)
        assertThat(env["LD_LIBRARY_PATH"]).isEqualTo("/toolchain/lib:/system/lib64")
        assertThat(env["PATH"]).isEqualTo("/custom/bin:/usr/bin")
    }

    @Test
    fun `clearCache removes prepared binaries and legacy go deps`() {
        val context = RuntimeEnvironment.getApplication()
        File(GoDependencyManager.getBinDir(context), "server").apply {
            parentFile?.mkdirs()
            writeText("bin")
        }
        File(context.filesDir, "go_deps/legacy.txt").apply {
            parentFile?.mkdirs()
            writeText("legacy")
        }

        GoDependencyManager.clearCache(context)

        assertThat(File(context.filesDir, "go_bins").exists()).isFalse()
        assertThat(File(context.filesDir, "go_deps").exists()).isFalse()
    }

    private fun createElfFile(machine: Int, elfClass: Int): File {
        val file = Files.createTempFile("go-elf", ".bin").toFile()
        file.writeBytes(createElfHeader(machine, elfClass))
        return file
    }

    private fun createElfHeader(machine: Int, elfClass: Int): ByteArray {
        val header = ByteArray(20)
        header[0] = 0x7f.toByte()
        header[1] = 'E'.code.toByte()
        header[2] = 'L'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = elfClass.toByte()
        header[18] = (machine and 0xFF).toByte()
        header[19] = ((machine shr 8) and 0xFF).toByte()
        return header
    }

    private fun machineForCurrentAbi(): Int {
        return when (GoDependencyManager.getDeviceAbi()) {
            "arm64-v8a" -> 0xB7
            "armeabi-v7a" -> 0x28
            "x86_64" -> 0x3E
            "x86" -> 0x03
            else -> 0xB7
        }
    }

    private fun elfClassForCurrentAbi(): Int {
        return when (GoDependencyManager.getDeviceAbi()) {
            "arm64-v8a", "x86_64" -> 2
            else -> 1
        }
    }
}
