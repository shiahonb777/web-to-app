package com.webtoapp.core.golang

import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class GoDependencyManagerTest {

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

    private fun createElfFile(machine: Int, elfClass: Int): File {
        val file = Files.createTempFile("go-elf", ".bin").toFile()
        val header = ByteArray(20)
        header[0] = 0x7f.toByte()
        header[1] = 'E'.code.toByte()
        header[2] = 'L'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = elfClass.toByte()
        header[18] = (machine and 0xFF).toByte()
        header[19] = ((machine shr 8) and 0xFF).toByte()
        file.writeBytes(header)
        return file
    }
}
