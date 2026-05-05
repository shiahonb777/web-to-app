package com.webtoapp.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipAlignerTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `align stores native libraries at 16KB data offsets`() {
        val input = temp.newFile("input.apk")
        val output = temp.newFile("output.apk")
        ZipOutputStream(input.outputStream()).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("assets/a.txt"))
            zipOut.write("hello".toByteArray())
            zipOut.closeEntry()
            val data = ByteArray(128) { it.toByte() }
            val entry = ZipEntry("lib/arm64-v8a/libnode.so")
            entry.method = ZipEntry.STORED
            entry.size = data.size.toLong()
            entry.compressedSize = data.size.toLong()
            entry.crc = CRC32().apply { update(data) }.value
            zipOut.putNextEntry(entry)
            zipOut.write(data)
            zipOut.closeEntry()
        }

        val aligned = ZipAligner.align(input, output)

        assertThat(aligned).isTrue()
        assertThat(ZipAligner.verifyNativeLibAlignment(output)).isTrue()
    }
}
