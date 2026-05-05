package com.webtoapp.core.apkbuilder

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object ElfAligner16k {
    private const val EI_CLASS = 4
    private const val EI_DATA = 5
    private const val ELFCLASS32 = 1
    private const val ELFCLASS64 = 2
    private const val ELFDATA2LSB = 1
    private const val PT_LOAD = 1L
    const val ALIGNMENT = 16 * 1024L

    data class AlignmentResult(
        val outputFile: File,
        val changed: Boolean,
        val repacked: Boolean,
        val alreadyAligned: Boolean
    )

    fun ensureAligned(inputFile: File, workDir: File): AlignmentResult {
        if (!inputFile.isFile || inputFile.length() < 64L || !isElf(inputFile)) {
            return AlignmentResult(inputFile, changed = false, repacked = false, alreadyAligned = true)
        }
        val inspection = inspect(inputFile)
        if (inspection.aligned) {
            return AlignmentResult(inputFile, changed = false, repacked = false, alreadyAligned = true)
        }
        workDir.mkdirs()
        val outputFile = File(workDir, inputFile.nameWithoutExtension + ".aligned16k." + inputFile.extension.ifBlank { "so" })
        align(inputFile, outputFile)
        val alignedInspection = inspect(outputFile)
        if (!alignedInspection.aligned) {
            outputFile.delete()
            throw IllegalStateException("ELF 16KB alignment failed for ${inputFile.name}")
        }
        return AlignmentResult(outputFile, changed = true, repacked = inspection.needsRepack, alreadyAligned = false)
    }

    fun align(inputFile: File, outputFile: File) {
        val input = inputFile.readBytes()
        val elf = parse(input)
        val output = if (elf.needsRepack) repack(input, elf) else patchMetadata(input, elf)
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(output)
        outputFile.setExecutable(inputFile.canExecute(), false)
        outputFile.setReadable(inputFile.canRead(), false)
    }

    fun isAligned16k(file: File): Boolean {
        return try {
            inspect(file).aligned
        } catch (_: Exception) {
            false
        }
    }

    fun inspect(file: File): ElfInspection {
        return parse(file.readBytes()).toInspection()
    }

    fun isElf(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val magic = ByteArray(4)
                input.read(magic) == 4 && magic[0] == 0x7F.toByte() && magic[1] == 'E'.code.toByte() && magic[2] == 'L'.code.toByte() && magic[3] == 'F'.code.toByte()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun patchMetadata(input: ByteArray, elf: ElfFile): ByteArray {
        val output = input.copyOf()
        elf.programHeaders.filter { it.type == PT_LOAD }.forEach { ph ->
            writeProgramHeader(output, elf, ph.copy(align = ALIGNMENT))
        }
        return output
    }

    private fun repack(input: ByteArray, elf: ElfFile): ByteArray {
        val loads = elf.programHeaders.filter { it.type == PT_LOAD }.sortedBy { it.offset }
        if (loads.isEmpty()) {
            return patchMetadata(input, elf)
        }
        val layout = linkedMapOf<Int, Long>()
        var cursor = elf.headerEnd
        loads.forEachIndexed { index, ph ->
            val newOffset = if (index == 0 && ph.offset == 0L) {
                val residue = positiveMod(ph.vaddr, ALIGNMENT)
                if (residue != 0L) {
                    throw IllegalStateException("Unsupported ELF first PT_LOAD residue: $residue")
                }
                0L
            } else {
                alignToResidue(cursor, positiveMod(ph.vaddr, ALIGNMENT), ALIGNMENT)
            }
            layout[ph.index] = newOffset
            cursor = maxOf(cursor, newOffset + ph.filesz)
        }
        if (cursor > Int.MAX_VALUE) {
            throw IllegalStateException("ELF is too large to repack in memory: $cursor bytes")
        }
        val output = ByteArray(cursor.toInt())
        loads.forEach { ph ->
            if (ph.filesz > 0L) {
                val sourceStart = ph.offset.toIntExact()
                val targetStart = layout.getValue(ph.index).toIntExact()
                val size = ph.filesz.toIntExact()
                if (sourceStart < 0 || sourceStart + size > input.size) {
                    throw IllegalStateException("Invalid PT_LOAD range in ELF")
                }
                System.arraycopy(input, sourceStart, output, targetStart, size)
            }
        }
        val headerCopySize = min(elf.headerEnd.toIntExact(), input.size)
        System.arraycopy(input, 0, output, 0, headerCopySize)
        writeSectionHeaderStrip(output, elf)
        elf.programHeaders.forEach { ph ->
            val rewritten = if (ph.type == PT_LOAD) {
                ph.copy(offset = layout.getValue(ph.index), align = maxOf(ph.align, ALIGNMENT))
            } else {
                val remappedOffset = remapOffset(ph, loads, layout)
                ph.copy(offset = remappedOffset)
            }
            writeProgramHeader(output, elf, rewritten)
        }
        return output
    }

    private fun remapOffset(ph: ProgramHeader, loads: List<ProgramHeader>, layout: Map<Int, Long>): Long {
        if (ph.filesz == 0L) return ph.offset
        val owner = loads.firstOrNull { load ->
            ph.offset >= load.offset && ph.offset + ph.filesz <= load.offset + load.filesz
        }
        return if (owner != null) layout.getValue(owner.index) + (ph.offset - owner.offset) else ph.offset
    }

    private fun parse(bytes: ByteArray): ElfFile {
        if (bytes.size < 52) throw IllegalArgumentException("File is too small for ELF")
        if (bytes[0] != 0x7F.toByte() || bytes[1] != 'E'.code.toByte() || bytes[2] != 'L'.code.toByte() || bytes[3] != 'F'.code.toByte()) {
            throw IllegalArgumentException("Not an ELF file")
        }
        val elfClass = bytes[EI_CLASS].toInt() and 0xFF
        val elfData = bytes[EI_DATA].toInt() and 0xFF
        if (elfData != ELFDATA2LSB) throw IllegalArgumentException("Only little-endian ELF is supported")
        val is64 = when (elfClass) {
            ELFCLASS64 -> true
            ELFCLASS32 -> false
            else -> throw IllegalArgumentException("Unsupported ELF class: $elfClass")
        }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val phoff: Long
        val ehsize: Int
        val phentsize: Int
        val phnum: Int
        if (is64) {
            phoff = buffer.getLong(32)
            ehsize = buffer.getShort(52).toInt() and 0xFFFF
            phentsize = buffer.getShort(54).toInt() and 0xFFFF
            phnum = buffer.getShort(56).toInt() and 0xFFFF
        } else {
            phoff = buffer.getInt(28).toUnsignedLong()
            ehsize = buffer.getShort(40).toInt() and 0xFFFF
            phentsize = buffer.getShort(42).toInt() and 0xFFFF
            phnum = buffer.getShort(44).toInt() and 0xFFFF
        }
        if (phoff <= 0L || phentsize <= 0 || phnum <= 0) throw IllegalArgumentException("ELF has no program headers")
        val phTableEnd = phoff + phentsize.toLong() * phnum.toLong()
        if (phTableEnd > bytes.size) throw IllegalArgumentException("ELF program header table is out of range")
        val headers = (0 until phnum).map { index ->
            readProgramHeader(buffer, is64, phoff.toIntExact() + index * phentsize, index)
        }
        return ElfFile(is64 = is64, ehsize = ehsize, phoff = phoff, phentsize = phentsize, phnum = phnum, headerEnd = maxOf(ehsize.toLong(), phTableEnd), programHeaders = headers)
    }

    private fun readProgramHeader(buffer: ByteBuffer, is64: Boolean, offset: Int, index: Int): ProgramHeader {
        return if (is64) {
            ProgramHeader(
                index = index,
                headerOffset = offset,
                type = buffer.getInt(offset).toUnsignedLong(),
                flags = buffer.getInt(offset + 4).toUnsignedLong(),
                offset = buffer.getLong(offset + 8),
                vaddr = buffer.getLong(offset + 16),
                paddr = buffer.getLong(offset + 24),
                filesz = buffer.getLong(offset + 32),
                memsz = buffer.getLong(offset + 40),
                align = buffer.getLong(offset + 48)
            )
        } else {
            ProgramHeader(
                index = index,
                headerOffset = offset,
                type = buffer.getInt(offset).toUnsignedLong(),
                offset = buffer.getInt(offset + 4).toUnsignedLong(),
                vaddr = buffer.getInt(offset + 8).toUnsignedLong(),
                paddr = buffer.getInt(offset + 12).toUnsignedLong(),
                filesz = buffer.getInt(offset + 16).toUnsignedLong(),
                memsz = buffer.getInt(offset + 20).toUnsignedLong(),
                flags = buffer.getInt(offset + 24).toUnsignedLong(),
                align = buffer.getInt(offset + 28).toUnsignedLong()
            )
        }
    }

    private fun writeProgramHeader(bytes: ByteArray, elf: ElfFile, ph: ProgramHeader) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val o = ph.headerOffset
        if (elf.is64) {
            buffer.putInt(o, ph.type.toInt())
            buffer.putInt(o + 4, ph.flags.toInt())
            buffer.putLong(o + 8, ph.offset)
            buffer.putLong(o + 16, ph.vaddr)
            buffer.putLong(o + 24, ph.paddr)
            buffer.putLong(o + 32, ph.filesz)
            buffer.putLong(o + 40, ph.memsz)
            buffer.putLong(o + 48, ph.align)
        } else {
            buffer.putInt(o, ph.type.toInt())
            buffer.putInt(o + 4, ph.offset.toInt())
            buffer.putInt(o + 8, ph.vaddr.toInt())
            buffer.putInt(o + 12, ph.paddr.toInt())
            buffer.putInt(o + 16, ph.filesz.toInt())
            buffer.putInt(o + 20, ph.memsz.toInt())
            buffer.putInt(o + 24, ph.flags.toInt())
            buffer.putInt(o + 28, ph.align.toInt())
        }
    }

    private fun writeSectionHeaderStrip(bytes: ByteArray, elf: ElfFile) {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        if (elf.is64) {
            buffer.putLong(40, 0L)
            buffer.putShort(58, 0.toShort())
            buffer.putShort(60, 0.toShort())
            buffer.putShort(62, 0.toShort())
        } else {
            buffer.putInt(32, 0)
            buffer.putShort(46, 0.toShort())
            buffer.putShort(48, 0.toShort())
            buffer.putShort(50, 0.toShort())
        }
    }

    private fun ElfFile.toInspection(): ElfInspection {
        val loadSegments = programHeaders.filter { it.type == PT_LOAD }
        val aligned = loadSegments.all { it.align >= ALIGNMENT && positiveMod(it.offset, ALIGNMENT) == positiveMod(it.vaddr, ALIGNMENT) }
        val needsRepack = loadSegments.any { positiveMod(it.offset, ALIGNMENT) != positiveMod(it.vaddr, ALIGNMENT) }
        return ElfInspection(aligned = aligned, needsRepack = needsRepack, loadSegments = loadSegments)
    }

    private fun alignToResidue(value: Long, residue: Long, alignment: Long): Long {
        val current = positiveMod(value, alignment)
        val delta = positiveMod(residue - current, alignment)
        return value + delta
    }

    private fun positiveMod(value: Long, mod: Long): Long {
        val result = value % mod
        return if (result < 0L) result + mod else result
    }

    private fun Int.toUnsignedLong(): Long = toLong() and 0xFFFFFFFFL

    private fun Long.toIntExact(): Int {
        if (this < 0L || this > Int.MAX_VALUE) throw IllegalStateException("Value is out of Int range: $this")
        return toInt()
    }

    data class ElfInspection(
        val aligned: Boolean,
        val needsRepack: Boolean,
        val loadSegments: List<ProgramHeader>
    )

    data class ProgramHeader(
        val index: Int,
        val headerOffset: Int,
        val type: Long,
        val flags: Long,
        val offset: Long,
        val vaddr: Long,
        val paddr: Long,
        val filesz: Long,
        val memsz: Long,
        val align: Long
    )

    private data class ElfFile(
        val is64: Boolean,
        val ehsize: Int,
        val phoff: Long,
        val phentsize: Int,
        val phnum: Int,
        val headerEnd: Long,
        val programHeaders: List<ProgramHeader>
    ) {
        val needsRepack: Boolean
            get() = programHeaders.filter { it.type == PT_LOAD }.any {
                positiveMod(it.offset, ALIGNMENT) != positiveMod(it.vaddr, ALIGNMENT)
            }
    }
}
