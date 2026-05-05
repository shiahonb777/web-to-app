package com.webtoapp.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ElfAligner16kTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `metadata patch aligns load segments when offsets already match vaddr residues`() {
        val input = temp.newFile("aligned-residue.so")
        input.writeBytes(createElf64(loadOffset = 0x1000L, loadVaddr = 0x1000L, loadAlign = 0x1000L))
        val output = temp.newFile("aligned-output.so")

        ElfAligner16k.align(input, output)
        val inspection = ElfAligner16k.inspect(output)

        assertThat(inspection.aligned).isTrue()
        assertThat(inspection.needsRepack).isFalse()
        assertThat(inspection.loadSegments.single().align).isAtLeast(16 * 1024L)
        assertThat(output.length()).isEqualTo(input.length())
    }

    @Test
    fun `repack moves load segment when offset and vaddr residues differ`() {
        val input = temp.newFile("misaligned-residue.so")
        input.writeBytes(createElf64(loadOffset = 0x1000L, loadVaddr = 0x2000L, loadAlign = 0x1000L))
        val output = temp.newFile("repacked-output.so")

        ElfAligner16k.align(input, output)
        val inspection = ElfAligner16k.inspect(output)

        assertThat(inspection.aligned).isTrue()
        assertThat(inspection.loadSegments.single().offset % (16 * 1024L)).isEqualTo(0x2000L)
    }

    private fun createElf64(loadOffset: Long, loadVaddr: Long, loadAlign: Long): ByteArray {
        val fileSize = (loadOffset + 16).toInt()
        val bytes = ByteArray(fileSize)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        bytes[0] = 0x7F
        bytes[1] = 'E'.code.toByte()
        bytes[2] = 'L'.code.toByte()
        bytes[3] = 'F'.code.toByte()
        bytes[4] = 2
        bytes[5] = 1
        bytes[6] = 1
        buffer.putShort(16, 3.toShort())
        buffer.putShort(18, 183.toShort())
        buffer.putInt(20, 1)
        buffer.putLong(32, 64L)
        buffer.putLong(40, 0L)
        buffer.putInt(48, 0)
        buffer.putShort(52, 64.toShort())
        buffer.putShort(54, 56.toShort())
        buffer.putShort(56, 1.toShort())
        buffer.putShort(58, 0.toShort())
        buffer.putShort(60, 0.toShort())
        buffer.putShort(62, 0.toShort())
        val ph = 64
        buffer.putInt(ph, 1)
        buffer.putInt(ph + 4, 5)
        buffer.putLong(ph + 8, loadOffset)
        buffer.putLong(ph + 16, loadVaddr)
        buffer.putLong(ph + 24, loadVaddr)
        buffer.putLong(ph + 32, 16L)
        buffer.putLong(ph + 40, 16L)
        buffer.putLong(ph + 48, loadAlign)
        for (i in 0 until 16) {
            bytes[loadOffset.toInt() + i] = (0x40 + i).toByte()
        }
        return bytes
    }
}
