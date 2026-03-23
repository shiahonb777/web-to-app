package com.webtoapp.core.apkbuilder

import android.util.Log

/**
 * Android 二进制 XML (AXML) 编辑器
 * 用于修改 AndroidManifest.xml 中的包名
 * 
 * 采用原地字节替换策略，确保文件结构不被破坏
 */
class AxmlEditor {

    companion object {
        private const val ORIGINAL_PACKAGE = "com.webtoapp"
    }

    /**
     * 修改 AndroidManifest.xml 中的包名
     * 使用原地替换策略，新包名长度必须 <= 原包名长度
     * 
     * @param axmlData 原始 AXML 数据
     * @param newPackageName 新的包名
     * @return 修改后的 AXML 数据
     */
    fun modifyPackageName(axmlData: ByteArray, newPackageName: String): ByteArray {
        val result = axmlData.copyOf()
        
        // 检测编码格式并替换（UTF-8 和 UTF-16LE 都尝试）
        val utf8Ok = replacePackageInUtf8(result, ORIGINAL_PACKAGE, newPackageName)
        val utf16Ok = replacePackageInUtf16(result, ORIGINAL_PACKAGE, newPackageName)

        // 修复由 androidx.core 引入的动态接收器权限和 FileProvider authorities，避免与宿主包名冲突
        fixDynamicPermissionAndAuthorities(result, newPackageName)

        // 恢复关键组件（Application / Activity）的类名到原始包名，避免 ClassNotFound
        restoreComponentClassNames(result, newPackageName)

        // 尝试禁用 android:testOnly 标记，避免 INSTALL_FAILED_TEST_ONLY
        stripTestOnlyFlag(result)

        Log.d(
            "AxmlEditor",
            "modifyPackageName: from=$ORIGINAL_PACKAGE to=$newPackageName, utf8Ok=$utf8Ok, utf16Ok=$utf16Ok"
        )
        
        return result
    }

    /**
     * 在 UTF-8 编码的数据中直接按字节替换包名
     * 保证新包名字节长度不大于旧包名，避免破坏结构
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
            newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
        }

        return replaceBytesInData(data, oldBytes, replacement)
    }

    /**
     * 修复 androidx.core / androidx.startup 引入的权限和 Provider authorities：
     * - <permission android:name="com.webtoapp.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"/>
     * - <uses-permission android:name="com.webtoapp.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"/>
     * - <provider android:authorities="com.webtoapp.fileprovider"/>
     * - <provider android:authorities="com.webtoapp.androidx-startup"/>
     * 在导出 APK 中，这些前缀需要替换为新包名，避免与宿主 WebToApp 以及其它导出 APK 冲突。
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
            Log.w("AxmlEditor", "fixDynamicPermissionAndAuthorities failed", e)
        }
    }

    /**
     * 将字符串池中的完整字符串从 oldStr 替换为 newStr（支持 UTF-8 / UTF-16LE），
     * 要求 newStr 的字节长度不大于 oldStr，以保证原地替换安全。
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
                    newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
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
                    newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
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
            Log.w("AxmlEditor", "stripTestOnlyFlag failed", e)
        }
    }

    /**
     * 将因为包名替换而被改成 newPackageName 前缀的关键组件类名恢复回 ORIGINAL_PACKAGE
     * 避免出现 "com.w2a.extn.WebToAppApplication" 这类在 dex 中不存在的类
     * 
     * 注意：由于包名替换时会用0填充，broken字符串实际上是 newPackageName + 填充的0 + suffix
     */
    private fun restoreComponentClassNames(data: ByteArray, newPackageName: String) {
        val suffixes = listOf(
            "WebToAppApplication",
            "ui.MainActivity",
            "ui.webview.WebViewActivity",
            "ui.shell.ShellActivity"
        )

        var restored = false
        
        // 计算填充长度（原包名和新包名的差值）
        val padLen = ORIGINAL_PACKAGE.length - newPackageName.length

        for (suffix in suffixes) {
            // 原始类名（正确的）
            val original = "$ORIGINAL_PACKAGE.$suffix"
            
            // 被破坏的类名：newPackageName + 填充的0字符 + "." + suffix
            // 但实际上由于替换时0填充，破坏的模式是 newPackageName + 0*padLen + "." + suffix
            if (padLen >= 0) {
                // 构造被破坏的字节模式
                val brokenPrefix = newPackageName + String(CharArray(padLen) { '\u0000' })
                val broken = "$brokenPrefix.$suffix"
                
                // UTF-8 恢复
                val brokenUtf8 = broken.toByteArray(Charsets.UTF_8)
                val originalUtf8 = original.toByteArray(Charsets.UTF_8)
                if (brokenUtf8.size == originalUtf8.size) {
                    if (replaceBytesInData(data, brokenUtf8, originalUtf8)) {
                        restored = true
                        Log.d("AxmlEditor", "恢复类名(UTF8): $broken -> $original")
                    }
                }

                // UTF-16LE 恢复
                val brokenUtf16 = broken.toByteArray(Charsets.UTF_16LE)
                val originalUtf16 = original.toByteArray(Charsets.UTF_16LE)
                if (brokenUtf16.size == originalUtf16.size) {
                    if (replaceBytesInData(data, brokenUtf16, originalUtf16)) {
                        restored = true
                        Log.d("AxmlEditor", "恢复类名(UTF16): $broken -> $original")
                    }
                }
            }
        }

        Log.d("AxmlEditor", "restoreComponentClassNames: newPackage=$newPackageName, padLen=$padLen, restored=$restored")
    }

    /**
     * 在 UTF-16LE 编码的数据中直接按字节替换包名
     * 保证新包名字节长度不大于旧包名
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
            newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
        }

        return replaceBytesInData(data, oldBytes, replacement)
    }

    /**
     * 在整个数据中查找并替换所有匹配的字节序列
     * 直接替换所有匹配项，不做独立字符串检查
     * 类名恢复由 restoreComponentClassNames 单独处理
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
                Log.d("AxmlEditor", "替换包名成功 at offset $i")
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
