package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Shared ZIP utility methods — extracted from ApkBuilder and AppCloner
 * to eliminate duplicate implementations.
 */
object ZipUtils {

    /**
     * Write entry using DEFLATED compression format.
     */
    fun writeEntryDeflated(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.DEFLATED
        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }

    /**
     * Write entry using STORED (uncompressed) format, simplified version.
     * For splash media etc. that need to be read by AssetManager.openFd().
     */
    fun writeEntryStoredSimple(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()

        val crc = CRC32()
        crc.update(data)
        entry.crc = crc.value

        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }

    /**
     * Write entry using STORED uncompressed format with 4-byte alignment for resources.arsc.
     * For resources.arsc, to satisfy Android R+ uncompressed and 4-byte alignment requirements.
     */
    fun writeEntryStored(zipOut: ZipOutputStream, name: String, data: ByteArray) {
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = data.size.toLong()
        entry.compressedSize = data.size.toLong()

        // Android 11+ requires resources.arsc data to be 4-byte aligned in APK
        // Since we ensure resources.arsc is the first entry, we can use extra field for alignment padding
        if (name == "resources.arsc") {
            val nameBytes = name.toByteArray(Charsets.UTF_8)
            val baseHeaderSize = 30 // ZIP local file header fixed length
            val base = baseHeaderSize + nameBytes.size
            // extra total length = 4(custom header) + padLen
            // Need (base + extraLen) % 4 == 0
            val padLen = (4 - (base + 4) % 4) % 4
            if (padLen > 0) {
                // Use 0xFFFF as private extra header ID
                val extra = ByteArray(4 + padLen)
                extra[0] = 0xFF.toByte()
                extra[1] = 0xFF.toByte()
                // data size = padLen (little-endian)
                extra[2] = (padLen and 0xFF).toByte()
                extra[3] = ((padLen shr 8) and 0xFF).toByte()
                // Remaining pad bytes default to 0
                entry.extra = extra
            }
        }

        val crc = CRC32()
        crc.update(data)
        entry.crc = crc.value

        zipOut.putNextEntry(entry)
        zipOut.write(data)
        zipOut.closeEntry()
    }

    /**
     * Streaming write large file using STORED uncompressed format.
     * For large video/binary files, avoid OOM.
     * Two steps: first calculate CRC, then write data.
     */
    fun writeEntryStoredStreaming(zipOut: ZipOutputStream, name: String, file: File) {
        val fileSize = file.length()

        // First pass: calculate CRC32 (streaming read, don't load to memory)
        val crc = CRC32()
        val buffer = ByteArray(8192)
        file.inputStream().buffered().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                crc.update(buffer, 0, bytesRead)
            }
        }

        // Create ZIP entry
        val entry = ZipEntry(name)
        entry.method = ZipEntry.STORED
        entry.size = fileSize
        entry.compressedSize = fileSize
        entry.crc = crc.value

        // Second pass: write data (streaming write)
        zipOut.putNextEntry(entry)
        file.inputStream().buffered().use { input ->
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                zipOut.write(buffer, 0, bytesRead)
            }
        }
        zipOut.closeEntry()

        AppLogger.d("ZipUtils", "Large file streaming embedded(STORED): $name (${fileSize / 1024} KB)")
    }

    /**
     * Copy ZIP entry from source to destination using DEFLATED compression.
     */
    fun copyEntry(zipIn: ZipFile, zipOut: ZipOutputStream, entry: ZipEntry) {
        val data = zipIn.getInputStream(entry).readBytes()
        writeEntryDeflated(zipOut, entry.name, data)
    }

    /**
     * Copy ZIP entry preserving original method (STORED entries keep their metadata).
     * Used by AppCloner where STORED entries must remain STORED.
     */
    fun copyEntryPreserveMethod(zipIn: ZipFile, zipOut: ZipOutputStream, entry: ZipEntry) {
        val data = zipIn.getInputStream(entry).readBytes()
        if (entry.method == ZipEntry.STORED) {
            // For resources.arsc, use writeEntryStored which handles 4-byte alignment
            if (entry.name == "resources.arsc") {
                writeEntryStored(zipOut, entry.name, data)
            } else {
                writeEntryStoredSimple(zipOut, entry.name, data)
            }
        } else {
            writeEntryDeflated(zipOut, entry.name, data)
        }
    }
}
