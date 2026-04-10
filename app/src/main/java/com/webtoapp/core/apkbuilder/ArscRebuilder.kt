package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ARSC 文件重构器
 * 突破字符串池大小限制，支持任意长度的应用名称
 * 
 * ARSC 文件结构：
 * - ResTable_header (12 bytes)
 * - ResStringPool (变长)
 * - ResTable_package (变长，可能有多个)
 */
class ArscRebuilder {
    
    companion object {
        private const val TAG = "ArscRebuilder"
        
        // Chunk types
        private const val RES_TABLE_TYPE: Short = 0x0002
        private const val RES_STRING_POOL_TYPE: Short = 0x0001
        private const val RES_TABLE_PACKAGE_TYPE: Short = 0x0200
        
        // String pool flags
        private const val UTF8_FLAG = 0x00000100
    }
    
    /**
     * 修改应用名称（突破长度限制）
     * 
     * @param arscData 原始 ARSC 数据
     * @param targetAppName 目标应用名称（可以是任意长度）
     * @return 修改后的 ARSC 数据
     */
    fun rebuildWithNewAppName(arscData: ByteArray, targetAppName: String): ByteArray {
        return rebuildWithNewAppNameAndIcons(arscData, targetAppName, replaceIcons = false)
    }

    /**
     * 修改应用名称 + 替换图标路径（突破长度限制 + 对抗 R8 混淆）
     * 
     * R8 资源混淆会将 res/mipmap-anydpi-v26/ic_launcher.xml 重命名为 res/a1.xml 之类的短路径，
     * 导致基于文件名匹配的图标替换方案完全失效。
     * 
     * 本方法通过解析 ARSC 层级结构（Package → TypeSpec → Type → Entry）
     * 定位 mipmap/ic_launcher* 和 drawable/ic_launcher_foreground* 资源条目，
     * 从条目中提取它们在全局字符串池中的文件路径索引（无论路径是否被混淆），
     * 然后在重建字符串池时将这些路径替换为我们已知的 PNG 路径。
     * 
     * @param arscData 原始 ARSC 数据
     * @param targetAppName 目标应用名称
     * @param replaceIcons 是否替换图标路径
     * @return 修改后的 ARSC 数据，以及发现的旧图标路径映射
     */
    fun rebuildWithNewAppNameAndIcons(
        arscData: ByteArray,
        targetAppName: String,
        replaceIcons: Boolean = false
    ): ByteArray {
        AppLogger.d(TAG, "rebuildWithNewAppName: target='$targetAppName', replaceIcons=$replaceIcons")
        
        try {
            val buffer = ByteBuffer.wrap(arscData).order(ByteOrder.LITTLE_ENDIAN)
            
            // 1. 解析文件头
            val tableType = buffer.short
            val tableHeaderSize = buffer.short
            val tableSize = buffer.int
            val packageCount = buffer.int
            
            if (tableType != RES_TABLE_TYPE) {
                AppLogger.e(TAG, "Invalid ARSC file: type=$tableType")
                return arscData
            }
            
            AppLogger.d(TAG, "Table header: size=$tableSize, packages=$packageCount")
            
            // 2. 解析字符串池
            val stringPoolStart = tableHeaderSize.toInt()
            buffer.position(stringPoolStart)
            
            val poolType = buffer.short
            val poolHeaderSize = buffer.short
            val poolSize = buffer.int
            val stringCount = buffer.int
            val styleCount = buffer.int
            val poolFlags = buffer.int
            val stringsStart = buffer.int
            val stylesStart = buffer.int
            
            if (poolType != RES_STRING_POOL_TYPE) {
                AppLogger.e(TAG, "Invalid string pool: type=$poolType")
                return arscData
            }
            
            val isUtf8 = (poolFlags and UTF8_FLAG) != 0
            AppLogger.d(TAG, "String pool: count=$stringCount, utf8=$isUtf8, size=$poolSize")
            
            // 3. 读取所有字符串
            val stringOffsets = IntArray(stringCount)
            for (i in 0 until stringCount) {
                stringOffsets[i] = buffer.int
            }
            
            // 跳过样式偏移
            buffer.position(buffer.position() + styleCount * 4)
            
            val stringsDataStart = stringPoolStart + stringsStart
            val strings = mutableListOf<String>()
            
            for (i in 0 until stringCount) {
                val str = readStringAt(arscData, stringsDataStart + stringOffsets[i], isUtf8)
                strings.add(str)
            }

            val afterStringPool = stringPoolStart + poolSize
            val remainingData = arscData.copyOfRange(afterStringPool, arscData.size)

            // 3.5. 发现图标路径（如果启用）
            // 只收集旧路径供 ApkBuilder 在 ZIP 层替换文件内容
            // 不修改 ARSC 中的路径，因为 ARSC 路径必须与 ZIP 条目名一致
            if (replaceIcons) {
                val iconPaths = findIconPathIndices(remainingData, strings)
                _lastDiscoveredIconPaths = iconPaths.map { (idx, _) -> strings[idx] }.toSet()
                AppLogger.d(TAG, "Discovered old icon paths (for ZIP replacement): $_lastDiscoveredIconPaths")
            }
            
            // 4. 找到并替换应用名称
            var modified = false
            val appNamePatterns = listOf(
                "WebToApp - Convert Any Website to Android App",
                "WebToApp"
            )
            
            for (i in strings.indices) {
                val str = strings[i]
                // 检查是否是应用名称（以 WebToApp 开头且不是其他字符串的一部分）
                for (pattern in appNamePatterns) {
                    if (str.startsWith(pattern) && !str.contains("/") && !str.contains(".")) {
                        AppLogger.d(TAG, "Found app_name at index $i: '$str' -> '$targetAppName'")
                        strings[i] = targetAppName
                        modified = true
                        break
                    }
                }
                if (modified) break
            }
            
            if (!modified) {
                AppLogger.w(TAG, "app_name not found in string pool, trying pattern matching...")
                // 尝试更宽松的匹配
                for (i in strings.indices) {
                    val str = strings[i]
                    if (str.trim().startsWith("WebToApp") && 
                        str.length < 100 && 
                        !str.contains("Theme") && 
                        !str.contains("http") &&
                        !str.contains(".")) {
                        AppLogger.d(TAG, "Found potential app_name at index $i: '$str'")
                        strings[i] = targetAppName
                        modified = true
                        break
                    }
                }
            }
            
            if (!modified) {
                AppLogger.e(TAG, "Could not find app_name to replace")
                if (!replaceIcons) return arscData
                // If replaceIcons is true, continue even without app name modification
            }
            
            // 5. 重新构建字符串池
            val newStringPool = buildStringPool(strings, isUtf8, styleCount, poolFlags)
            
            // 6. 重新构建 ARSC 文件
            val newArsc = ByteBuffer.allocate(tableHeaderSize + newStringPool.size + remainingData.size)
                .order(ByteOrder.LITTLE_ENDIAN)
            
            // 写入新的文件头
            newArsc.putShort(tableType)
            newArsc.putShort(tableHeaderSize)
            newArsc.putInt(tableHeaderSize + newStringPool.size + remainingData.size) // 新的总大小
            newArsc.putInt(packageCount)
            
            // 写入新的字符串池
            newArsc.put(newStringPool)
            
            // 写入剩余数据
            newArsc.put(remainingData)
            
            AppLogger.d(TAG, "Rebuild complete: old size=${arscData.size}, new size=${newArsc.array().size}")
            return newArsc.array()
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to rebuild ARSC", e)
            return arscData
        }
    }

    /** 上次发现的旧图标路径（供 ApkBuilder 用于跳过 ZIP 条目） */
    private var _lastDiscoveredIconPaths = emptySet<String>()
    fun getLastDiscoveredIconPaths(): Set<String> = _lastDiscoveredIconPaths

    /**
     * 解析 ARSC Package 数据，定位 mipmap/ic_launcher 和 drawable/ic_launcher_foreground 的
     * 文件路径在全局字符串池中的索引。
     * 
     * ARSC Package 结构:
     * - ResTable_package header (含 typeStrings 和 keyStrings 偏移)
     * - Type string pool (类型名: drawable, mipmap, ...)
     * - Key string pool (条目名: ic_launcher, ic_launcher_foreground, ...)
     * - TypeSpec chunks (每种类型一个)
     * - Type chunks (每种配置一个，包含条目指向全局字符串池的文件路径索引)
     *
     * @return List of (globalStringIndex, replacement path)
     */
    private fun findIconPathIndices(
        packageData: ByteArray,
        globalStrings: List<String>
    ): List<Pair<Int, String>> {
        val result = mutableListOf<Pair<Int, String>>()
        
        try {
            val buf = ByteBuffer.wrap(packageData).order(ByteOrder.LITTLE_ENDIAN)
            
            // 读取 Package header
            val pkgType = buf.short
            val pkgHeaderSize = buf.short.toInt() and 0xFFFF
            val pkgSize = buf.int
            val pkgId = buf.int
            
            if (pkgType != RES_TABLE_PACKAGE_TYPE) {
                AppLogger.w(TAG, "Not a package chunk: type=0x${pkgType.toString(16)}")
                return result
            }
            
            // 跳过 package name (128 chars = 256 bytes)
            buf.position(buf.position() + 256)
            
            val typeStringsOffset = buf.int  // 类型字符串池偏移（相对于 package header 开始）
            val lastPublicType = buf.int
            val keyStringsOffset = buf.int   // 键名字符串池偏移
            val lastPublicKey = buf.int
            
            // 读取类型字符串池
            val typeStrPoolPos = typeStringsOffset // Relative to package start which is at offset 0 of packageData
            buf.position(typeStrPoolPos)
            val typeStrings = readStringPool(buf, packageData)
            
            // 读取键名字符串池
            buf.position(keyStringsOffset)
            val keyStrings = readStringPool(buf, packageData)
            
            AppLogger.d(TAG, "Package types: ${typeStrings.joinToString()}")
            
            // 找到 mipmap 和 drawable 的类型 ID
            // 注意: 类型 ID 是 1-based (typeStrings 是 0-based)
            val mipmapTypeId = typeStrings.indexOfFirst { it == "mipmap" } + 1
            val drawableTypeId = typeStrings.indexOfFirst { it == "drawable" } + 1
            
            // 找到关键条目名在 keyStrings 中的索引
            val icLauncherKeyIdx = keyStrings.indexOf("ic_launcher")
            val icLauncherRoundKeyIdx = keyStrings.indexOf("ic_launcher_round")
            val icLauncherFgKeyIdx = keyStrings.indexOf("ic_launcher_foreground")
            
            AppLogger.d(TAG, "mipmapTypeId=$mipmapTypeId, drawableTypeId=$drawableTypeId")
            AppLogger.d(TAG, "keyIndices: ic_launcher=$icLauncherKeyIdx, round=$icLauncherRoundKeyIdx, foreground=$icLauncherFgKeyIdx")
            
            if (mipmapTypeId <= 0 && drawableTypeId <= 0) {
                AppLogger.w(TAG, "mipmap/drawable types not found in package")
                return result
            }
            
            // 遍历包内的所有 chunk，找到 ResTable_type chunks
            // 从 pkgHeaderSize 开始跳过 header，或者从 keyStrings 池之后开始
            // 我们需要扫描 pkgHeaderSize 之后的所有数据
            var pos = pkgHeaderSize
            var typeChunkCount = 0
            var mipmapTypeChunkCount = 0
            var drawableTypeChunkCount = 0
            AppLogger.d(TAG, "Scanning from pos=$pos, packageData.size=${packageData.size}, pkgHeaderSize=$pkgHeaderSize")
            while (pos + 8 <= packageData.size) {
                buf.position(pos)
                val chunkType = buf.short.toInt() and 0xFFFF
                val chunkHeaderSize = buf.short.toInt() and 0xFFFF
                val chunkSize = buf.int
                
                if (chunkSize <= 0 || pos + chunkSize > packageData.size) {
                    AppLogger.d(TAG, "Chunk scan ended at pos=$pos: chunkSize=$chunkSize, remaining=${packageData.size - pos}")
                    break
                }
                
                // ResTable_type (0x0201) — contains actual entries with file path references
                if (chunkType == 0x0201) {
                    typeChunkCount++
                    val typeId = buf.get().toInt() and 0xFF  // 1-based type ID
                    buf.get() // res0
                    buf.short // res1
                    val entryCount = buf.int
                    val entriesStart = buf.int  // offset from chunk start to entry data
                    
                    val isInteresting = typeId == mipmapTypeId || typeId == drawableTypeId
                    if (isInteresting) {
                        if (typeId == mipmapTypeId) mipmapTypeChunkCount++
                        if (typeId == drawableTypeId) drawableTypeChunkCount++
                        AppLogger.d(TAG, "Type chunk #$typeChunkCount: typeId=$typeId, entryCount=$entryCount, entriesStart=$entriesStart, chunkHeaderSize=$chunkHeaderSize, pos=$pos")
                    }
                    
                    // 跳过 ResTable_config (变长，但从 chunkHeaderSize 开始到 entriesStart 之前)
                    // 读取 entry offset 表
                    buf.position(pos + chunkHeaderSize)
                    val entryOffsets = IntArray(entryCount)
                    for (i in 0 until entryCount) {
                        entryOffsets[i] = buf.int
                    }
                    
                    // 遍历感兴趣的条目
                    if (isInteresting) {
                        // Log entry indices for ic_launcher entries
                        for (targetIdx in listOf(icLauncherKeyIdx, icLauncherRoundKeyIdx, icLauncherFgKeyIdx)) {
                            if (targetIdx >= 0 && targetIdx < entryCount) {
                                val offset = entryOffsets[targetIdx]
                                AppLogger.d(TAG, "  Target key $targetIdx offset in entry table: $offset (entryIdx maps to keyIdx)")
                            }
                        }
                    }

                    for (entryIdx in 0 until entryCount) {
                        if (entryOffsets[entryIdx] == -1) continue  // NO_ENTRY
                        
                        val entryPos = pos + entriesStart + entryOffsets[entryIdx]
                        if (entryPos + 8 > packageData.size) continue
                        
                        buf.position(entryPos)
                        val entrySize = buf.short.toInt() and 0xFFFF
                        val entryFlags = buf.short.toInt() and 0xFFFF
                        val entryKeyIndex = buf.int  // Index into key string pool
                        
                        val isComplex = (entryFlags and 0x0001) != 0
                        if (isComplex) {
                            if (isInteresting && (entryKeyIndex == icLauncherKeyIdx || entryKeyIndex == icLauncherRoundKeyIdx || entryKeyIndex == icLauncherFgKeyIdx)) {
                                AppLogger.d(TAG, "  Entry $entryIdx: keyIndex=$entryKeyIndex is COMPLEX (bag/map), skipping")
                            }
                            continue
                        }
                        
                        // Simple entry: next 8 bytes are Res_value
                        if (entryPos + entrySize + 8 > packageData.size) continue
                        val valueSize = buf.short.toInt() and 0xFFFF
                        buf.get() // res0
                        val valueType = buf.get().toInt() and 0xFF
                        val valueData = buf.int
                        
                        if (isInteresting && (entryKeyIndex == icLauncherKeyIdx || entryKeyIndex == icLauncherRoundKeyIdx || entryKeyIndex == icLauncherFgKeyIdx)) {
                            val keyName = if (entryKeyIndex >= 0 && entryKeyIndex < keyStrings.size) keyStrings[entryKeyIndex] else "?"
                            val pathStr = if (valueType == 0x03 && valueData >= 0 && valueData < globalStrings.size) globalStrings[valueData] else "N/A"
                            AppLogger.d(TAG, "  Entry $entryIdx: keyIndex=$entryKeyIndex('$keyName'), valueType=0x${valueType.toString(16)}, valueData=$valueData, path='$pathStr'")
                        }
                        
                        // We want TYPE_STRING (0x03) — file path reference into global string pool
                        if (valueType != 0x03) continue
                        
                        val globalStrIdx = valueData
                        if (globalStrIdx < 0 || globalStrIdx >= globalStrings.size) continue
                        
                        // Check if this is a mipmap/ic_launcher entry
                        // mipmap/ic_launcher 和 ic_launcher_round 指向 adaptive icon 定义 XML
                        // 这些 XML 绝对不能被替换！它们引用 @drawable/ic_launcher_foreground
                        // 替换 XML 内容为 PNG 会导致 Android 解析失败 → 回退默认图标
                        // 所以我们只记录日志，不收集到 result 中
                        if (typeId == mipmapTypeId) {
                            if (entryKeyIndex == icLauncherKeyIdx || entryKeyIndex == icLauncherRoundKeyIdx) {
                                val oldPath = globalStrings[globalStrIdx]
                                val keyName = if (entryKeyIndex >= 0 && entryKeyIndex < keyStrings.size) keyStrings[entryKeyIndex] else "?"
                                AppLogger.d(TAG, "Found mipmap/$keyName → '$oldPath' (adaptive icon XML, KEEPING)")
                            }
                        }
                        
                        // drawable/ic_launcher_foreground 是实际的前景图像文件
                        // 这个需要被替换为用户的自定义图标
                        if (typeId == drawableTypeId) {
                            if (entryKeyIndex == icLauncherFgKeyIdx) {
                                val oldPath = globalStrings[globalStrIdx]
                                AppLogger.d(TAG, "Found drawable/ic_launcher_foreground → '$oldPath' (foreground image, REPLACING)")
                                result.add(globalStrIdx to oldPath)
                            }
                        }
                    }
                }
                
                pos += chunkSize
            }
            AppLogger.d(TAG, "Scan complete: typeChunks=$typeChunkCount, mipmapChunks=$mipmapTypeChunkCount, drawableChunks=$drawableTypeChunkCount, results=${result.size}")
            
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to find icon path indices", e)
        }
        
        // 去重（同一全局字符串索引可能被多个 config 引用）
        return result.distinctBy { it.first }
    }

    /**
     * 快速读取字符串池内容（不重构）
     */
    private fun readStringPool(buf: ByteBuffer, fullData: ByteArray): List<String> {
        val startPos = buf.position()
        val poolType = buf.short
        val poolHeaderSize = buf.short.toInt() and 0xFFFF
        val poolSize = buf.int
        val strCount = buf.int
        val styleCount = buf.int
        val flags = buf.int
        val strStart = buf.int
        // stylesStart 和可能的扩展字段 — 通过 poolHeaderSize 跳过
        
        val isUtf8 = (flags and UTF8_FLAG) != 0
        
        // 跳过 header 中的剩余字段（stylesStart 等），直接定位到字符串偏移表
        buf.position(startPos + poolHeaderSize)
        
        val offsets = IntArray(strCount)
        for (i in 0 until strCount) {
            offsets[i] = buf.int
        }
        
        val stringsDataStart = startPos + strStart
        val result = mutableListOf<String>()
        for (i in 0 until strCount) {
            result.add(readStringAt(fullData, stringsDataStart + offsets[i], isUtf8))
        }
        
        // 将 buffer 位置移到池尾
        buf.position(startPos + poolSize)
        return result
    }
    
    /**
     * 读取指定位置的字符串
     */
    private fun readStringAt(data: ByteArray, offset: Int, isUtf8: Boolean): String {
        return if (isUtf8) {
            readUtf8String(data, offset)
        } else {
            readUtf16String(data, offset)
        }
    }
    
    /**
     * 读取 UTF-8 字符串
     * 格式: [charLen (1-2 bytes)] [byteLen (1-2 bytes)] [data] [0x00]
     */
    private fun readUtf8String(data: ByteArray, offset: Int): String {
        var pos = offset
        
        // 读取字符长度（1-2 字节）
        var charLen = data[pos].toInt() and 0xFF
        pos++
        if ((charLen and 0x80) != 0) {
            charLen = ((charLen and 0x7F) shl 8) or (data[pos].toInt() and 0xFF)
            pos++
        }
        
        // 读取字节长度（1-2 字节）
        var byteLen = data[pos].toInt() and 0xFF
        pos++
        if ((byteLen and 0x80) != 0) {
            byteLen = ((byteLen and 0x7F) shl 8) or (data[pos].toInt() and 0xFF)
            pos++
        }
        
        // 读取字符串数据
        return String(data, pos, byteLen, Charsets.UTF_8)
    }
    
    /**
     * 读取 UTF-16 字符串
     * 格式: [charLen (2-4 bytes)] [data] [0x0000]
     */
    private fun readUtf16String(data: ByteArray, offset: Int): String {
        var pos = offset
        
        // 读取字符长度
        var charLen = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
        pos += 2
        
        if ((charLen and 0x8000) != 0) {
            val high = charLen and 0x7FFF
            val low = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
            charLen = (high shl 16) or low
            pos += 2
        }
        
        // 读取字符串数据
        val bytes = ByteArray(charLen * 2)
        System.arraycopy(data, pos, bytes, 0, charLen * 2)
        return String(bytes, Charsets.UTF_16LE)
    }
    
    /**
     * 构建新的字符串池
     */
    private fun buildStringPool(strings: List<String>, isUtf8: Boolean, styleCount: Int, flags: Int): ByteArray {
        val stringCount = strings.size
        
        // 构建字符串数据
        val stringDataList = mutableListOf<ByteArray>()
        for (str in strings) {
            val encoded = if (isUtf8) {
                encodeUtf8String(str)
            } else {
                encodeUtf16String(str)
            }
            stringDataList.add(encoded)
        }
        
        // 计算偏移和大小
        val headerSize = 28 // ResStringPool_header
        val offsetsSize = (stringCount + styleCount) * 4
        val stringsDataSize = stringDataList.sumOf { it.size }
        
        // 字符串数据起始位置（相对于头部开始）
        val stringsStart = headerSize + offsetsSize
        
        // 计算对齐（字符串数据需要 4 字节对齐）
        val alignedStringsDataSize = (stringsDataSize + 3) and 0x7FFFFFFC.toInt()
        
        val totalSize = stringsStart + alignedStringsDataSize
        
        val buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        
        // 写入头部
        buffer.putShort(RES_STRING_POOL_TYPE)
        buffer.putShort(headerSize.toShort())
        buffer.putInt(totalSize)
        buffer.putInt(stringCount)
        buffer.putInt(styleCount)
        buffer.putInt(flags)
        buffer.putInt(stringsStart)
        buffer.putInt(0) // stylesStart (no styles)
        
        // 写入字符串偏移
        var currentOffset = 0
        for (data in stringDataList) {
            buffer.putInt(currentOffset)
            currentOffset += data.size
        }
        
        // 写入样式偏移（如果有）
        for (i in 0 until styleCount) {
            buffer.putInt(0)
        }
        
        // 写入字符串数据
        for (data in stringDataList) {
            buffer.put(data)
        }
        
        // 填充对齐
        while (buffer.position() < totalSize) {
            buffer.put(0)
        }
        
        return buffer.array()
    }
    
    /**
     * 编码 UTF-8 字符串
     */
    private fun encodeUtf8String(str: String): ByteArray {
        val utf8Bytes = str.toByteArray(Charsets.UTF_8)
        val charLen = str.length
        val byteLen = utf8Bytes.size
        
        // 计算长度前缀大小
        val charLenSize = if (charLen > 0x7F) 2 else 1
        val byteLenSize = if (byteLen > 0x7F) 2 else 1
        
        val result = ByteArray(charLenSize + byteLenSize + byteLen + 1) // +1 for null terminator
        var pos = 0
        
        // 写入字符长度
        if (charLen > 0x7F) {
            result[pos++] = (0x80 or ((charLen shr 8) and 0x7F)).toByte()
            result[pos++] = (charLen and 0xFF).toByte()
        } else {
            result[pos++] = charLen.toByte()
        }
        
        // 写入字节长度
        if (byteLen > 0x7F) {
            result[pos++] = (0x80 or ((byteLen shr 8) and 0x7F)).toByte()
            result[pos++] = (byteLen and 0xFF).toByte()
        } else {
            result[pos++] = byteLen.toByte()
        }
        
        // 写入数据
        System.arraycopy(utf8Bytes, 0, result, pos, byteLen)
        pos += byteLen
        
        // null terminator
        result[pos] = 0
        
        return result
    }
    
    /**
     * 编码 UTF-16 字符串
     */
    private fun encodeUtf16String(str: String): ByteArray {
        val utf16Bytes = str.toByteArray(Charsets.UTF_16LE)
        val charLen = str.length
        
        // 计算长度前缀大小
        val lenSize = if (charLen > 0x7FFF) 4 else 2
        
        val result = ByteArray(lenSize + utf16Bytes.size + 2) // +2 for null terminator
        var pos = 0
        
        // 写入长度
        if (charLen > 0x7FFF) {
            val high = 0x8000 or ((charLen shr 16) and 0x7FFF)
            val low = charLen and 0xFFFF
            result[pos++] = (high and 0xFF).toByte()
            result[pos++] = ((high shr 8) and 0xFF).toByte()
            result[pos++] = (low and 0xFF).toByte()
            result[pos++] = ((low shr 8) and 0xFF).toByte()
        } else {
            result[pos++] = (charLen and 0xFF).toByte()
            result[pos++] = ((charLen shr 8) and 0xFF).toByte()
        }
        
        // 写入数据
        System.arraycopy(utf16Bytes, 0, result, pos, utf16Bytes.size)
        pos += utf16Bytes.size
        
        // null terminator
        result[pos++] = 0
        result[pos] = 0
        
        return result
    }
}
