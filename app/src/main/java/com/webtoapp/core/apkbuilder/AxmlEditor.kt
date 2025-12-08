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
        
        // 原始版本信息（与 build.gradle.kts 保持一致）
        private const val ORIGINAL_VERSION_CODE = 15
        private const val ORIGINAL_VERSION_NAME = "1.5.0"
        
        // Android 属性资源 ID
        private const val ATTR_VERSION_CODE = 0x0101021b  // android:versionCode
        private const val ATTR_VERSION_NAME = 0x0101021c  // android:versionName
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
     * 修改 AndroidManifest.xml 中的版本信息
     * 
     * @param axmlData 原始 AXML 数据
     * @param newVersionCode 新版本号
     * @param newVersionName 新版本名
     * @return 修改后的 AXML 数据
     */
    fun modifyVersion(axmlData: ByteArray, newVersionCode: Int, newVersionName: String): ByteArray {
        val result = axmlData.copyOf()
        
        // 修改 versionCode（整数属性）
        val versionCodeOk = modifyVersionCodeInAxml(result, newVersionCode)
        
        // 修改 versionName（字符串属性）
        val versionNameOk = modifyVersionNameInAxml(result, newVersionName)
        
        Log.d("AxmlEditor", "modifyVersion: versionCode=$newVersionCode(ok=$versionCodeOk), versionName=$newVersionName(ok=$versionNameOk)")
        
        return result
    }
    
    /**
     * 修改 AXML 中的 versionCode 属性值
     * versionCode 是整数类型，直接存储在属性的 data 字段中
     */
    private fun modifyVersionCodeInAxml(data: ByteArray, newVersionCode: Int): Boolean {
        // 查找 manifest 元素中的 versionCode 属性并修改其值
        return modifyIntAttribute(data, ATTR_VERSION_CODE, newVersionCode)
    }
    
    /**
     * 修改 AXML 中的 versionName 属性值
     * versionName 是字符串类型，需要替换字符串池中的值
     */
    private fun modifyVersionNameInAxml(data: ByteArray, newVersionName: String): Boolean {
        // 策略：在字符串池中找到原始 versionName 并替换
        // 如果新版本名更长，需要截断或填充
        return replaceFullStringInData(data, ORIGINAL_VERSION_NAME, newVersionName)
    }
    
    /**
     * 修改 AXML 中指定资源 ID 的整数属性值
     * 遍历所有 XML 元素，查找匹配的属性并修改
     */
    private fun modifyIntAttribute(data: ByteArray, attrResId: Int, newValue: Int): Boolean {
        if (data.size < 8) return false
        
        val fileType = readUInt16LE(data, 0)
        if (fileType != 0x0003) return false  // 不是 AXML 文件
        
        val fileHeaderSize = readUInt16LE(data, 2)
        var offset = fileHeaderSize
        
        // 跳过字符串池
        if (offset + 8 > data.size) return false
        val stringPoolSize = readUInt32LE(data, offset + 4)
        offset += stringPoolSize
        
        // 跳过资源 ID 映射（如果存在）
        if (offset + 8 <= data.size) {
            val chunkType = readUInt16LE(data, offset)
            if (chunkType == 0x0180) {
                val size = readUInt32LE(data, offset + 4)
                offset += size
            }
        }
        
        var patched = false
        
        // 遍历 XML 内容块
        while (offset + 8 <= data.size) {
            val type = readUInt16LE(data, offset)
            val headerSize = readUInt16LE(data, offset + 2)
            val size = readUInt32LE(data, offset + 4)
            
            // 0x0102 = START_TAG
            if (type == 0x0102 && offset + headerSize <= data.size) {
                val attributeSize = readUInt16LE(data, offset + 26)
                val attributeCount = readUInt16LE(data, offset + 28)
                var attrOffset = offset + headerSize
                
                for (i in 0 until attributeCount) {
                    if (attrOffset + attributeSize > data.size) break
                    
                    // 属性结构：
                    // 0-3: namespace string index
                    // 4-7: name string index  
                    // 8-11: raw value string index
                    // 12-13: size
                    // 14: res0 (always 0)
                    // 15: type (0x10 = int, 0x03 = string ref)
                    // 16-19: data
                    
                    // 读取资源 ID（在资源 ID 映射中的索引，这里我们检查 data 字段中的实际值）
                    val attrNameIdx = readUInt32LE(data, attrOffset + 4)
                    val attrType = data[attrOffset + 15].toInt() and 0xFF
                    val attrData = readUInt32LE(data, attrOffset + 16)
                    
                    // 对于 versionCode，检查类型是否为整数 (0x10) 且值匹配原始 versionCode
                    if (attrType == 0x10 && attrData == ORIGINAL_VERSION_CODE) {
                        // 写入新的 versionCode
                        writeUInt32LE(data, attrOffset + 16, newValue)
                        patched = true
                        Log.d("AxmlEditor", "Patched versionCode at offset ${attrOffset + 16}: $ORIGINAL_VERSION_CODE -> $newValue")
                    }
                    
                    attrOffset += attributeSize
                }
            }
            
            if (size <= 0) break
            offset += size
        }
        
        return patched
    }
    
    /**
     * 写入 32 位小端序整数
     */
    private fun writeUInt32LE(data: ByteArray, offset: Int, value: Int) {
        if (offset + 4 > data.size) return
        data[offset] = (value and 0xFF).toByte()
        data[offset + 1] = ((value shr 8) and 0xFF).toByte()
        data[offset + 2] = ((value shr 16) and 0xFF).toByte()
        data[offset + 3] = ((value shr 24) and 0xFF).toByte()
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
     */
    private fun restoreComponentClassNames(data: ByteArray, newPackageName: String) {
        val suffixes = listOf(
            "WebToAppApplication",
            "ui.MainActivity",
            "ui.webview.WebViewActivity",
            "ui.shell.ShellActivity"
        )

        var restored = false

        for (suffix in suffixes) {
            val broken = "$newPackageName.$suffix"
            val original = "$ORIGINAL_PACKAGE.$suffix"

            if (broken.length == original.length) {
                val brokenUtf8 = broken.toByteArray(Charsets.UTF_8)
                val originalUtf8 = original.toByteArray(Charsets.UTF_8)
                if (brokenUtf8.size == originalUtf8.size) {
                    if (replaceBytesInData(data, brokenUtf8, originalUtf8)) {
                        restored = true
                    }
                }

                val brokenUtf16 = broken.toByteArray(Charsets.UTF_16LE)
                val originalUtf16 = original.toByteArray(Charsets.UTF_16LE)
                if (brokenUtf16.size == originalUtf16.size) {
                    if (replaceBytesInData(data, brokenUtf16, originalUtf16)) {
                        restored = true
                    }
                }
            }
        }

        Log.d("AxmlEditor", "restoreComponentClassNames: newPackage=$newPackageName, restored=$restored")
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
     * 增加"独立字符串"检查：只有当匹配的字节序列后面是终止符（0x00）或特定分隔符时才替换
     * 这避免了误伤以包名作为前缀的字符串（如类名 com.package.MainActivity）
     */
    private fun replaceBytesInData(data: ByteArray, pattern: ByteArray, replacement: ByteArray): Boolean {
        var found = false
        var i = 0
        val isUtf16 = pattern.size >= 2 && pattern[1].toInt() == 0 
        
        while (i <= data.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (data[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            
            if (match) {
                // 检查后续字符，确保这是独立的包名字符串
                val nextByteIndex = i + pattern.size
                var isIndependent = false
                
                if (nextByteIndex >= data.size) {
                    isIndependent = true
                } else {
                    val nextByte = data[nextByteIndex]
                    // 检查是否是终止符或分隔符
                    if (isUtf16) {
                        // UTF-16LE: 检查双字节 0x0000
                        if (nextByteIndex + 1 < data.size) {
                            val nextByte2 = data[nextByteIndex + 1]
                            if (nextByte == 0.toByte() && nextByte2 == 0.toByte()) {
                                isIndependent = true
                            }
                        }
                    } else {
                        // UTF-8: 检查单字节 0x00
                        if (nextByte == 0.toByte()) {
                            isIndependent = true
                        }
                    }
                }

                if (isIndependent) {
                    System.arraycopy(replacement, 0, data, i, replacement.size)
                    found = true
                    i += pattern.size
                } else {
                    // 这是一个前缀（如 com.pkg.Activity），跳过
                    i++
                }
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
