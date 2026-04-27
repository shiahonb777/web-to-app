package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger

/**
 * Note: brief English comment.
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 */
class AxmlEditor {

    companion object {
        private const val TAG = "AxmlEditor"
        private const val ORIGINAL_PACKAGE = "com.webtoapp"
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun modifyPackageName(axmlData: ByteArray, newPackageName: String): ByteArray {
        val result = axmlData.copyOf()
        
        // Note: brief English comment.
        val utf8Ok = replacePackageInUtf8(result, ORIGINAL_PACKAGE, newPackageName)
        val utf16Ok = replacePackageInUtf16(result, ORIGINAL_PACKAGE, newPackageName)

        // Note: brief English comment.
        fixDynamicPermissionAndAuthorities(result, newPackageName)

        // Note: brief English comment.
        restoreComponentClassNames(result, newPackageName)

        // Note: brief English comment.
        stripTestOnlyFlag(result)

        AppLogger.d(
            "AxmlEditor",
            "modifyPackageName: from=$ORIGINAL_PACKAGE to=$newPackageName, utf8Ok=$utf8Ok, utf16Ok=$utf16Ok"
        )
        
        return result
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun replacePackageInUtf8(data: ByteArray, oldPkg: String, newPkg: String): Boolean {
        val oldBytes = oldPkg.toByteArray(Charsets.UTF_8)
        val newBytes = newPkg.toByteArray(Charsets.UTF_8)

        if (newBytes.size > oldBytes.size) {
            return false
        }

        val replacement = if (newBytes.size == oldBytes.size) {
            newBytes
        } else {
            newBytes + ByteArray(oldBytes.size - newBytes.size)
        }

        return replaceBytesInData(data, oldBytes, replacement)
    }

    /**
     * Note: brief English comment.
     * - <permission android:name="com.webtoapp.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"/>
     * - <uses-permission android:name="com.webtoapp.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"/>
     * - <provider android:authorities="com.webtoapp.fileprovider"/>
     * - <provider android:authorities="com.webtoapp.androidx-startup"/>
     * Note: brief English comment.
     */
    private fun fixDynamicPermissionAndAuthorities(data: ByteArray, newPackageName: String) {
        try {
            val oldPermission = "$ORIGINAL_PACKAGE.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
            val newPermission = "$newPackageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"

            val oldAuthority = "$ORIGINAL_PACKAGE.fileprovider"
            val newAuthority = "$newPackageName.fileprovider"
            val oldStartupAuthority = "$ORIGINAL_PACKAGE.androidx-startup"
            val newStartupAuthority = "$newPackageName.androidx-startup"

            replaceFullStringInData(data, oldPermission, newPermission)
            replaceFullStringInData(data, oldAuthority, newAuthority)
            replaceFullStringInData(data, oldStartupAuthority, newStartupAuthority)

        } catch (e: Exception) {
            AppLogger.w(TAG, "fixDynamicPermissionAndAuthorities failed", e)
        }
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun replaceFullStringInData(data: ByteArray, oldStr: String, newStr: String): Boolean {
        var patched = false

        // UTF-8
        run {
            val oldBytes = oldStr.toByteArray(Charsets.UTF_8)
            val newBytes = newStr.toByteArray(Charsets.UTF_8)
            if (newBytes.size <= oldBytes.size) {
                val replacement = if (newBytes.size == oldBytes.size) {
                    newBytes
                } else {
                    newBytes + ByteArray(oldBytes.size - newBytes.size)
                }
                if (replaceBytesInData(data, oldBytes, replacement)) {
                    patched = true
                }
            }
        }

        // UTF-16LE
        run {
            val oldBytes = oldStr.toByteArray(Charsets.UTF_16LE)
            val newBytes = newStr.toByteArray(Charsets.UTF_16LE)
            if (newBytes.size <= oldBytes.size) {
                val replacement = if (newBytes.size == oldBytes.size) {
                    newBytes
                } else {
                    newBytes + ByteArray(oldBytes.size - newBytes.size)
                }
                if (replaceBytesInData(data, oldBytes, replacement)) {
                    patched = true
                }
            }
        }

        return patched
    }

    private fun stripTestOnlyFlag(data: ByteArray) {
        try {
            val testOnlyIndex = findStringIndex(data, "testOnly")
            if (testOnlyIndex < 0) return
            clearTestOnlyAttributes(data, testOnlyIndex)
        } catch (e: Exception) {
            AppLogger.w(TAG, "stripTestOnlyFlag failed", e)
        }
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     */
    private fun restoreComponentClassNames(data: ByteArray, newPackageName: String) {
        val suffixes = listOf(
            "WebToAppApplication",
            "ui.MainActivity",
            "ui.webview.WebViewActivity",
            "ui.shell.ShellActivity"
        )

        var restored = false
        
        // Note: brief English comment.
        val padLen = ORIGINAL_PACKAGE.length - newPackageName.length

        for (suffix in suffixes) {
            // Note: brief English comment.
            val original = "$ORIGINAL_PACKAGE.$suffix"
            
            // Note: brief English comment.
            // Note: brief English comment.
            if (padLen >= 0) {
                // Note: brief English comment.
                val brokenPrefix = newPackageName + String(CharArray(padLen) { '\u0000' })
                val broken = "$brokenPrefix.$suffix"
                
                // Note: brief English comment.
                val brokenUtf8 = broken.toByteArray(Charsets.UTF_8)
                val originalUtf8 = original.toByteArray(Charsets.UTF_8)
                if (brokenUtf8.size == originalUtf8.size) {
                    if (replaceBytesInData(data, brokenUtf8, originalUtf8)) {
                        restored = true
                        AppLogger.d(TAG, "恢复类名(UTF8): $broken -> $original")
                    }
                }

                // Note: brief English comment.
                val brokenUtf16 = broken.toByteArray(Charsets.UTF_16LE)
                val originalUtf16 = original.toByteArray(Charsets.UTF_16LE)
                if (brokenUtf16.size == originalUtf16.size) {
                    if (replaceBytesInData(data, brokenUtf16, originalUtf16)) {
                        restored = true
                        AppLogger.d(TAG, "恢复类名(UTF16): $broken -> $original")
                    }
                }
            }
        }

        AppLogger.d(TAG, "restoreComponentClassNames: newPackage=$newPackageName, padLen=$padLen, restored=$restored")
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun replacePackageInUtf16(data: ByteArray, oldPkg: String, newPkg: String): Boolean {
        val oldBytes = oldPkg.toByteArray(Charsets.UTF_16LE)
        val newBytes = newPkg.toByteArray(Charsets.UTF_16LE)

        if (newBytes.size > oldBytes.size) {
            return false
        }

        val replacement = if (newBytes.size == oldBytes.size) {
            newBytes
        } else {
            newBytes + ByteArray(oldBytes.size - newBytes.size)
        }

        return replaceBytesInData(data, oldBytes, replacement)
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun replaceBytesInData(data: ByteArray, pattern: ByteArray, replacement: ByteArray): Boolean {
        var found = false
        var i = 0
        
        while (i <= data.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (data[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            
            if (match) {
                System.arraycopy(replacement, 0, data, i, replacement.size)
                found = true
                i += pattern.size
                AppLogger.d(TAG, "替换包名成功 at offset $i")
            } else {
                i++
            }
        }
        return found
    }

    private fun findStringIndex(data: ByteArray, target: String): Int {
        if (data.size < 8) return -1

        val fileType = readUInt16LE(data, 0)
        if (fileType != 0x0003) return -1

        val headerSize = readUInt16LE(data, 2)
        var offset = headerSize
        if (offset + 8 > data.size) return -1

        val chunkType = readUInt16LE(data, offset)
        if (chunkType != 0x0001) return -1

        val stringCount = readUInt32LE(data, offset + 8)
        val flags = readUInt32LE(data, offset + 16)
        val stringsStart = readUInt32LE(data, offset + 20)
        val headerSizeSp = readUInt16LE(data, offset + 2)
        val isUtf8 = (flags and 0x00000100) != 0

        val offsetsBase = offset + headerSizeSp
        val stringsBase = offset + stringsStart

        for (i in 0 until stringCount) {
            val off = readUInt32LE(data, offsetsBase + i * 4)
            val strOffset = stringsBase + off
            val s = if (isUtf8) readUtf8At(data, strOffset) else readUtf16At(data, strOffset)
            if (s == target) return i
        }
        return -1
    }

    private fun clearTestOnlyAttributes(data: ByteArray, testOnlyIndex: Int): Boolean {
        if (data.size < 8) return false

        val fileHeaderSize = readUInt16LE(data, 2)
        var offset = fileHeaderSize

        if (offset + 8 > data.size) return false
        val firstChunkSize = readUInt32LE(data, offset + 4)
        offset += firstChunkSize

        if (offset + 8 <= data.size) {
            val t = readUInt16LE(data, offset)
            if (t == 0x0180) {
                val size = readUInt32LE(data, offset + 4)
                offset += size
            }
        }

        var patched = false
        while (offset + 8 <= data.size) {
            val type = readUInt16LE(data, offset)
            val headerSize = readUInt16LE(data, offset + 2)
            val size = readUInt32LE(data, offset + 4)
            if (type == 0x0102 && offset + headerSize <= data.size) {
                val attributeSize = readUInt16LE(data, offset + 26)
                val attributeCount = readUInt16LE(data, offset + 28)
                var attrOffset = offset + headerSize
                for (i in 0 until attributeCount) {
                    if (attrOffset + attributeSize > data.size) break
                    val nameIdx = readUInt32LE(data, attrOffset + 4)
                    if (nameIdx == testOnlyIndex) {
                        val valueTypeOffset = attrOffset + 15
                        val valueDataOffset = attrOffset + 16
                        if (valueDataOffset + 4 <= data.size) {
                            data[valueTypeOffset] = 0
                            data[valueDataOffset] = 0
                            data[valueDataOffset + 1] = 0
                            data[valueDataOffset + 2] = 0
                            data[valueDataOffset + 3] = 0
                            patched = true
                        }
                    }
                    attrOffset += attributeSize
                }
            }
            if (size <= 0) break
            offset += size
        }
        return patched
    }

    private fun readUInt16LE(data: ByteArray, offset: Int): Int {
        if (offset + 2 > data.size) return 0
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8)
    }

    private fun readUInt32LE(data: ByteArray, offset: Int): Int {
        if (offset + 4 > data.size) return 0
        return (data[offset].toInt() and 0xFF) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun readUtf8At(data: ByteArray, offset: Int): String {
        if (offset >= data.size) return ""
        var o = offset
        if (o >= data.size) return ""
        var length = data[o].toInt() and 0x7F
        if (data[o].toInt() and 0x80 != 0) {
            if (o + 1 >= data.size) return ""
            length = ((data[o].toInt() and 0x7F) shl 8) or (data[o + 1].toInt() and 0xFF)
            o += 2
        } else {
            o += 1
        }
        var byteLen = data[o].toInt() and 0x7F
        if (data[o].toInt() and 0x80 != 0) {
            if (o + 1 >= data.size) return ""
            byteLen = ((data[o].toInt() and 0x7F) shl 8) or (data[o + 1].toInt() and 0xFF)
            o += 2
        } else {
            o += 1
        }
        if (o + byteLen > data.size) return ""
        return String(data, o, byteLen, Charsets.UTF_8)
    }

    private fun readUtf16At(data: ByteArray, offset: Int): String {
        if (offset + 2 > data.size) return ""
        var o = offset
        var length = readUInt16LE(data, o)
        if (length and 0x8000 != 0) {
            if (o + 4 > data.size) return ""
            length = ((length and 0x7FFF) shl 16) or readUInt16LE(data, o + 2)
            o += 4
        } else {
            o += 2
        }
        val byteLen = length * 2
        if (o + byteLen > data.size) return ""
        return String(data, o, byteLen, Charsets.UTF_16LE)
    }
}
