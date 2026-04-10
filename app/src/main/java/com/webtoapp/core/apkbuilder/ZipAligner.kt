package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Pure-Java ZipAlign implementation
 * 
 * Ensures resources.arsc (and other STORED entries) have their data
 * aligned on a 4-byte boundary within the APK file.
 * 
 * This is required for Android R+ (API 30): 
 * "-124: Failed parse during installPackageLI: Targeting R+ (version 30 and above) 
 *  requires the resources.arsc of installed APKs to be stored uncompressed 
 *  and aligned on a 4-byte boundary"
 * 
 * The key insight: Java's ZipOutputStream doesn't expose the current stream position,
 * so we use a CountingOutputStream wrapper to track byte offsets precisely.
 */
object ZipAligner {
    
    private const val TAG = "ZipAligner"
    
    /** ZIP local file header fixed size (before name and extra fields) */
    private const val LFH_FIXED_SIZE = 30
    
    /** Default alignment for STORED entries */
    private const val ALIGNMENT = 4
    
    /**
     * Align an APK file in-place.
     * 
     * Reads the input APK, rewrites it with proper 4-byte alignment for all 
     * STORED entries (especially resources.arsc), and replaces the original file.
     * 
     * @param apkFile The APK file to align (will be modified in place)
     * @return true if alignment was successful
     */
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
    
    /**
     * Align an APK file.
     * 
     * @param input  The input APK file
     * @param output The output (aligned) APK file
     * @return true if alignment was successful
     */
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
                        // Process entries - resources.arsc first for optimal alignment
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
                                // STORED entry → needs 4-byte alignment
                                totalStored++
                                
                                val newEntry = ZipEntry(entry.name)
                                newEntry.method = ZipEntry.STORED
                                newEntry.size = data.size.toLong()
                                newEntry.compressedSize = data.size.toLong()
                                
                                val crc = CRC32()
                                crc.update(data)
                                newEntry.crc = crc.value
                                
                                // Calculate padding needed for alignment
                                // Data starts at: currentOffset + LFH_FIXED_SIZE + nameLength + extraLength
                                val nameBytes = entry.name.toByteArray(Charsets.UTF_8)
                                val currentOffset = countingStream.bytesWritten
                                val dataOffset = currentOffset + LFH_FIXED_SIZE + nameBytes.size
                                
                                // How many padding bytes do we need in the extra field?
                                val remainder = (dataOffset % ALIGNMENT).toInt()
                                val padding = if (remainder == 0) 0 else ALIGNMENT - remainder
                                
                                if (padding > 0) {
                                    // Use extra field for alignment padding
                                    // Format: 2-byte header ID (0xFFFF) + 2-byte data size + padding bytes
                                    val extraLen = padding  // Simple: just padding bytes as extra
                                    newEntry.extra = ByteArray(extraLen)
                                    alignedCount++
                                }
                                
                                // Verify alignment
                                val finalDataOffset = dataOffset + (newEntry.extra?.size ?: 0)
                                if (finalDataOffset % ALIGNMENT != 0L) {
                                    // Second attempt with adjusted padding
                                    val remainder2 = (finalDataOffset % ALIGNMENT).toInt()
                                    val additionalPad = if (remainder2 == 0) 0 else ALIGNMENT - remainder2
                                    val existingExtra = newEntry.extra ?: ByteArray(0)
                                    newEntry.extra = ByteArray(existingExtra.size + additionalPad)
                                }
                                
                                zipOut.putNextEntry(newEntry)
                                zipOut.write(data)
                                zipOut.closeEntry()
                                
                            } else {
                                // DEFLATED entry → just copy (no alignment needed)
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
    
    /**
     * Verify that resources.arsc in the APK is properly aligned.
     * 
     * @return true if resources.arsc is uncompressed and 4-byte aligned
     */
    fun verifyAlignment(apkFile: File): Boolean {
        try {
            // Read the raw ZIP to check actual file offsets
            RandomAccessFile(apkFile, "r").use { raf ->
                // Find resources.arsc by scanning local file headers
                var offset = 0L
                val lfhSignature = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // PK\x03\x04
                val header = ByteArray(30)
                
                while (offset < raf.length() - 30) {
                    raf.seek(offset)
                    raf.readFully(header)
                    
                    // Check local file header signature
                    if (header[0] != lfhSignature[0] || header[1] != lfhSignature[1] ||
                        header[2] != lfhSignature[2] || header[3] != lfhSignature[3]) {
                        break  // No more local file headers
                    }
                    
                    val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
                    val compressionMethod = buf.getShort(8).toInt() and 0xFFFF
                    val compressedSize = buf.getInt(18).toLong() and 0xFFFFFFFFL
                    val fileNameLen = buf.getShort(26).toInt() and 0xFFFF
                    val extraLen = buf.getShort(28).toInt() and 0xFFFF
                    
                    // Read file name
                    val nameBytes = ByteArray(fileNameLen)
                    raf.readFully(nameBytes)
                    val fileName = String(nameBytes, Charsets.UTF_8)
                    
                    // Data starts after local file header + name + extra
                    val dataOffset = offset + LFH_FIXED_SIZE + fileNameLen + extraLen
                    
                    if (fileName == "resources.arsc") {
                        val isStored = compressionMethod == 0  // STORED
                        val isAligned = dataOffset % ALIGNMENT == 0L
                        
                        AppLogger.d(TAG, "resources.arsc: stored=$isStored, dataOffset=$dataOffset, " +
                                "aligned=$isAligned (${dataOffset % ALIGNMENT})")
                        
                        return isStored && isAligned
                    }
                    
                    // Skip to next entry: header + name + extra + data
                    // For STORED, use compressedSize. For DEFLATED, also use compressedSize.
                    offset = dataOffset + compressedSize
                    
                    // Check for data descriptor (bit 3 of general purpose bit flag)
                    val gpFlags = buf.getShort(6).toInt() and 0xFFFF
                    if (gpFlags and 0x08 != 0) {
                        // Has data descriptor (12 or 16 bytes)
                        offset += 16  // Be safe, try 16
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
    
    /**
     * OutputStream wrapper that counts bytes written.
     */
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
