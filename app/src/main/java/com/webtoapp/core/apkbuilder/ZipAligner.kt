package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream















object ZipAligner {

    private const val TAG = "ZipAligner"


    private const val LFH_FIXED_SIZE = 30


    private const val DEFAULT_ALIGNMENT = 4L
    private const val NATIVE_LIB_ALIGNMENT = 16 * 1024L










    fun alignInPlace(apkFile: File): Boolean {
        val tempFile = File(apkFile.parent, apkFile.name + ".aligned")
        return try {
            val result = align(apkFile, tempFile)
            if (result) {
                apkFile.delete()
                tempFile.renameTo(apkFile)
                true
            } else {
                tempFile.delete()
                false
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "ZipAlign failed", e)
            tempFile.delete()
            false
        }
    }








    fun align(input: File, output: File): Boolean {
        if (!input.exists()) {
            AppLogger.e(TAG, "Input file does not exist: ${input.absolutePath}")
            return false
        }

        var alignedCount = 0
        var totalStored = 0

        try {
            ZipFile(input).use { zipIn ->
                FileOutputStream(output).use { fos ->
                    val countingStream = CountingOutputStream(fos)
                    ZipOutputStream(countingStream).use { zipOut ->

                        val entries = zipIn.entries().toList()
                            .sortedWith(
                                compareByDescending<ZipEntry> { it.name == "resources.arsc" }
                                    .thenBy { it.name.startsWith("META-INF/") }
                                    .thenBy { it.name }
                            )

                        for (entry in entries) {
                            if (entry.isDirectory) {
                                val newEntry = ZipEntry(entry.name)
                                newEntry.method = ZipEntry.STORED
                                newEntry.size = 0
                                newEntry.compressedSize = 0
                                newEntry.crc = 0
                                zipOut.putNextEntry(newEntry)
                                zipOut.closeEntry()
                                continue
                            }

                            val data = zipIn.getInputStream(entry).readBytes()

                            if (entry.method == ZipEntry.STORED || entry.name == "resources.arsc") {

                                totalStored++

                                val newEntry = ZipEntry(entry.name)
                                newEntry.method = ZipEntry.STORED
                                newEntry.size = data.size.toLong()
                                newEntry.compressedSize = data.size.toLong()

                                val crc = CRC32()
                                crc.update(data)
                                newEntry.crc = crc.value



                                val nameBytes = entry.name.toByteArray(Charsets.UTF_8)
                                val currentOffset = countingStream.bytesWritten
                                val dataOffset = currentOffset + LFH_FIXED_SIZE + nameBytes.size


                                val alignment = getEntryAlignment(entry.name)
                                val remainder = dataOffset % alignment
                                val padding = if (remainder == 0L) 0 else (alignment - remainder).toInt()

                                if (padding > 0) {


                                    val extraLen = padding
                                    newEntry.extra = ByteArray(extraLen)
                                    alignedCount++
                                }


                                val finalDataOffset = dataOffset + (newEntry.extra?.size ?: 0)
                                if (finalDataOffset % alignment != 0L) {

                                    val remainder2 = finalDataOffset % alignment
                                    val additionalPad = if (remainder2 == 0L) 0 else (alignment - remainder2).toInt()
                                    val existingExtra = newEntry.extra ?: ByteArray(0)
                                    newEntry.extra = ByteArray(existingExtra.size + additionalPad)
                                }

                                zipOut.putNextEntry(newEntry)
                                zipOut.write(data)
                                zipOut.closeEntry()

                            } else {

                                val newEntry = ZipEntry(entry.name)
                                newEntry.method = ZipEntry.DEFLATED
                                zipOut.putNextEntry(newEntry)
                                zipOut.write(data)
                                zipOut.closeEntry()
                            }
                        }
                    }
                }
            }

            AppLogger.d(TAG, "ZipAlign complete: $alignedCount/$totalStored STORED entries aligned")
            return true

        } catch (e: Exception) {
            AppLogger.e(TAG, "ZipAlign failed: ${e.message}", e)
            return false
        }
    }






    fun verifyAlignment(apkFile: File): Boolean {
        try {

            RandomAccessFile(apkFile, "r").use { raf ->

                var offset = 0L
                val lfhSignature = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
                val header = ByteArray(30)

                while (offset < raf.length() - 30) {
                    raf.seek(offset)
                    raf.readFully(header)


                    if (header[0] != lfhSignature[0] || header[1] != lfhSignature[1] ||
                        header[2] != lfhSignature[2] || header[3] != lfhSignature[3]) {
                        break
                    }

                    val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                    val compressionMethod = buf.getShort(8).toInt() and 0xFFFF
                    val compressedSize = buf.getInt(18).toLong() and 0xFFFFFFFFL
                    val fileNameLen = buf.getShort(26).toInt() and 0xFFFF
                    val extraLen = buf.getShort(28).toInt() and 0xFFFF


                    val nameBytes = ByteArray(fileNameLen)
                    raf.readFully(nameBytes)
                    val fileName = String(nameBytes, Charsets.UTF_8)


                    val dataOffset = offset + LFH_FIXED_SIZE + fileNameLen + extraLen

                    if (fileName == "resources.arsc") {
                        val isStored = compressionMethod == 0
                        val isAligned = dataOffset % DEFAULT_ALIGNMENT == 0L

                        AppLogger.d(TAG, "resources.arsc: stored=$isStored, dataOffset=$dataOffset, " +
                                "aligned=$isAligned (${dataOffset % DEFAULT_ALIGNMENT})")

                        return isStored && isAligned
                    }



                    offset = dataOffset + compressedSize


                    val gpFlags = buf.getShort(6).toInt() and 0xFFFF
                    if (gpFlags and 0x08 != 0) {

                        offset += 16
                    }
                }
            }

            AppLogger.w(TAG, "resources.arsc not found in APK")
            return false

        } catch (e: Exception) {
            AppLogger.e(TAG, "Alignment verification failed: ${e.message}")
            return false
        }
    }




    fun verifyNativeLibAlignment(apkFile: File, alignment: Long = NATIVE_LIB_ALIGNMENT): Boolean {
        try {
            RandomAccessFile(apkFile, "r").use { raf ->
                var offset = 0L
                val header = ByteArray(30)
                var nativeLibCount = 0

                while (offset < raf.length() - 30) {
                    raf.seek(offset)
                    raf.readFully(header)

                    if (header[0] != 0x50.toByte() || header[1] != 0x4B.toByte() ||
                        header[2] != 0x03.toByte() || header[3] != 0x04.toByte()) {
                        break
                    }

                    val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                    val compressionMethod = buf.getShort(8).toInt() and 0xFFFF
                    val compressedSize = buf.getInt(18).toLong() and 0xFFFFFFFFL
                    val fileNameLen = buf.getShort(26).toInt() and 0xFFFF
                    val extraLen = buf.getShort(28).toInt() and 0xFFFF

                    val nameBytes = ByteArray(fileNameLen)
                    raf.readFully(nameBytes)
                    val fileName = String(nameBytes, Charsets.UTF_8)
                    val dataOffset = offset + LFH_FIXED_SIZE + fileNameLen + extraLen

                    if (isNativeLibraryEntry(fileName)) {
                        nativeLibCount++
                        val isStored = compressionMethod == 0
                        val isAligned = dataOffset % alignment == 0L
                        if (!isStored || !isAligned) {
                            AppLogger.w(TAG, "Native lib is not ${alignment / 1024}KB zip-aligned: $fileName stored=$isStored dataOffset=$dataOffset remainder=${dataOffset % alignment}")
                            return false
                        }
                    }

                    offset = dataOffset + compressedSize
                    val gpFlags = buf.getShort(6).toInt() and 0xFFFF
                    if (gpFlags and 0x08 != 0) {
                        offset += 16
                    }
                }

                AppLogger.d(TAG, "Native lib zip alignment verified: $nativeLibCount entries")
                return true
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Native lib alignment verification failed: ${e.message}", e)
            return false
        }
    }

    private fun getEntryAlignment(name: String): Long {
        return if (isNativeLibraryEntry(name)) NATIVE_LIB_ALIGNMENT else DEFAULT_ALIGNMENT
    }

    private fun isNativeLibraryEntry(name: String): Boolean {
        return name.startsWith("lib/") && name.endsWith(".so")
    }




    private class CountingOutputStream(
        private val wrapped: OutputStream
    ) : OutputStream() {
        var bytesWritten: Long = 0L
            private set

        override fun write(b: Int) {
            wrapped.write(b)
            bytesWritten++
        }

        override fun write(b: ByteArray) {
            wrapped.write(b)
            bytesWritten += b.size
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            wrapped.write(b, off, len)
            bytesWritten += len
        }

        override fun flush() = wrapped.flush()
        override fun close() = wrapped.close()
    }
}
