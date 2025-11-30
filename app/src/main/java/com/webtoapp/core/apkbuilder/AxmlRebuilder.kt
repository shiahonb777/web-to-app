package com.webtoapp.core.apkbuilder

import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * AXML 重构器
 * 支持修改字符串池并重建整个 AXML 文件
 * 
 * 主要用途：将相对路径类名（如 .MainActivity）展开为绝对路径（如 com.pkg.MainActivity）
 * 这样修改包名后，组件类名仍然指向原包名下的类，避免 ClassNotFoundException
 */
class AxmlRebuilder {

    companion object {
        private const val TAG = "AxmlRebuilder"
        
        // AXML Chunk Types
        private const val CHUNK_AXML_FILE = 0x0003
        private const val CHUNK_STRING_POOL = 0x0001
        private const val CHUNK_RESOURCE_MAP = 0x0180
        private const val CHUNK_START_NAMESPACE = 0x0100
        private const val CHUNK_END_NAMESPACE = 0x0101
        private const val CHUNK_START_ELEMENT = 0x0102
        private const val CHUNK_END_ELEMENT = 0x0103
        private const val CHUNK_TEXT = 0x0104
        
        // Attribute resource IDs for android:name
        private const val ATTR_NAME = 0x01010003
    }

    /**
     * 展开相对路径类名并修改包名
     * 
     * @param axmlData 原始 AXML 数据
     * @param originalPackage 原始包名
     * @param newPackage 新包名
     * @return 修改后的 AXML 数据
     */
    fun expandAndModify(axmlData: ByteArray, originalPackage: String, newPackage: String): ByteArray {
        return try {
            val parsed = parseAxml(axmlData)
            if (parsed == null) {
                Log.e(TAG, "Failed to parse AXML")
                return axmlData
            }
            
            // 步骤1：找到需要展开的相对路径类名
            val expansions = findRelativeClassNames(parsed, originalPackage)
            Log.d(TAG, "Found ${expansions.size} relative class names to expand")
            
            // 步骤2：添加新字符串并更新引用（如果有相对路径）
            if (expansions.isNotEmpty()) {
                expandClassNames(parsed, expansions)
            }
            
            // 步骤3：修改包名和所有包名前缀的字符串（权限、authorities 等）
            // 即使没有相对路径类名，也需要处理权限冲突
            replacePackageString(parsed, originalPackage, newPackage)
            
            // 步骤4：重建 AXML
            val result = rebuildAxml(parsed)
            
            Log.d(TAG, "AXML rebuild complete: original=${axmlData.size}, new=${result.size}")
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "AXML rebuild failed", e)
            axmlData
        }
    }

    /**
     * 解析 AXML 文件结构
     */
    private fun parseAxml(data: ByteArray): ParsedAxml? {
        if (data.size < 8) return null
        
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        
        val fileType = buffer.short.toInt() and 0xFFFF
        val fileHeaderSize = buffer.short.toInt() and 0xFFFF
        val fileSize = buffer.int
        
        if (fileType != CHUNK_AXML_FILE) {
            Log.e(TAG, "Not a valid AXML file: type=0x${fileType.toString(16)}")
            return null
        }
        
        val chunks = mutableListOf<Chunk>()
        var stringPool: StringPool? = null
        var resourceMap: IntArray? = null
        
        var offset = fileHeaderSize
        while (offset + 8 <= data.size) {
            buffer.position(offset)
            val chunkType = buffer.short.toInt() and 0xFFFF
            val chunkHeaderSize = buffer.short.toInt() and 0xFFFF
            val chunkSize = buffer.int
            
            if (chunkSize <= 0 || offset + chunkSize > data.size) break
            
            when (chunkType) {
                CHUNK_STRING_POOL -> {
                    stringPool = parseStringPool(data, offset)
                }
                CHUNK_RESOURCE_MAP -> {
                    resourceMap = parseResourceMap(data, offset, chunkSize)
                }
                else -> {
                    chunks.add(Chunk(chunkType, offset, chunkSize, data.copyOfRange(offset, offset + chunkSize)))
                }
            }
            
            offset += chunkSize
        }
        
        if (stringPool == null) {
            Log.e(TAG, "String pool not found")
            return null
        }
        
        return ParsedAxml(fileHeaderSize, stringPool, resourceMap, chunks)
    }

    /**
     * 解析字符串池
     */
    private fun parseStringPool(data: ByteArray, offset: Int): StringPool {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(offset)
        
        buffer.short // type
        val headerSize = buffer.short.toInt() and 0xFFFF
        val chunkSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int
        
        val isUtf8 = (flags and 0x100) != 0
        
        // 读取字符串偏移
        val stringOffsets = IntArray(stringCount) { buffer.int }
        
        // 读取样式偏移
        val styleOffsets = IntArray(styleCount) { buffer.int }
        
        // 读取字符串
        val stringsDataStart = offset + stringsStart
        val strings = mutableListOf<String>()
        
        for (i in 0 until stringCount) {
            val strOffset = stringsDataStart + stringOffsets[i]
            val str = if (isUtf8) {
                readUtf8String(data, strOffset)
            } else {
                readUtf16String(data, strOffset)
            }
            strings.add(str)
        }
        
        // 读取样式数据（如果有）
        val stylesData = if (styleCount > 0 && stylesStart > 0) {
            val stylesDataStart = offset + stylesStart
            val stylesDataEnd = offset + chunkSize
            data.copyOfRange(stylesDataStart, stylesDataEnd)
        } else {
            null
        }
        
        return StringPool(
            isUtf8 = isUtf8,
            strings = strings.toMutableList(),
            styleOffsets = styleOffsets,
            stylesData = stylesData
        )
    }

    /**
     * 解析资源 ID 映射
     */
    private fun parseResourceMap(data: ByteArray, offset: Int, size: Int): IntArray {
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(offset + 8) // 跳过 chunk header
        val count = (size - 8) / 4
        return IntArray(count) { buffer.int }
    }

    /**
     * 查找需要展开的相对路径类名
     */
    private fun findRelativeClassNames(parsed: ParsedAxml, originalPackage: String): List<ClassNameExpansion> {
        val expansions = mutableListOf<ClassNameExpansion>()
        val resourceMap = parsed.resourceMap ?: return expansions
        
        // 找到 android:name 属性的资源 ID 索引
        val nameAttrIndex = resourceMap.indexOf(ATTR_NAME)
        if (nameAttrIndex < 0) {
            Log.d(TAG, "android:name attribute not found in resource map")
            return expansions
        }
        
        // 扫描所有 START_ELEMENT chunk，查找 android:name 属性
        for (chunk in parsed.chunks) {
            if (chunk.type != CHUNK_START_ELEMENT) continue
            
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            buffer.position(16) // 跳过 header 和 line number, comment
            
            val namespaceUri = buffer.int
            val elementName = buffer.int
            val attrStart = buffer.short.toInt() and 0xFFFF
            val attrSize = buffer.short.toInt() and 0xFFFF
            val attrCount = buffer.short.toInt() and 0xFFFF
            
            if (attrSize == 0 || attrCount == 0) continue
            
            // 检查是否是组件元素（activity, service, receiver, provider, application）
            val elementNameStr = parsed.stringPool.strings.getOrNull(elementName) ?: continue
            if (elementNameStr !in listOf("activity", "service", "receiver", "provider", "application", "activity-alias")) {
                continue
            }
            
            // 扫描属性
            for (i in 0 until attrCount) {
                val attrOffset = 36 + i * attrSize
                if (attrOffset + 20 > chunk.data.size) break
                
                buffer.position(attrOffset)
                val attrNs = buffer.int
                val attrName = buffer.int
                val attrRawValue = buffer.int
                val attrValueSize = buffer.short.toInt() and 0xFFFF
                buffer.get() // res0
                val attrValueType = buffer.get().toInt() and 0xFF
                val attrValueData = buffer.int
                
                // 检查是否是 android:name 属性
                if (attrName != nameAttrIndex) continue
                
                // 检查属性值类型是否是字符串（type 3）
                if (attrValueType != 3) continue
                
                // 获取字符串值
                val stringIndex = attrValueData
                val stringValue = parsed.stringPool.strings.getOrNull(stringIndex) ?: continue
                
                // 检查是否是相对路径类名
                if (stringValue.startsWith(".") || (!stringValue.contains(".") && stringValue.isNotEmpty())) {
                    val absoluteName = if (stringValue.startsWith(".")) {
                        originalPackage + stringValue
                    } else {
                        "$originalPackage.$stringValue"
                    }
                    
                    expansions.add(ClassNameExpansion(
                        chunkIndex = parsed.chunks.indexOf(chunk),
                        attrIndex = i,
                        attrOffset = attrOffset,
                        originalStringIndex = stringIndex,
                        originalValue = stringValue,
                        expandedValue = absoluteName
                    ))
                    
                    Log.d(TAG, "Found relative class name: '$stringValue' -> '$absoluteName' in <$elementNameStr>")
                }
            }
        }
        
        return expansions
    }

    /**
     * 展开类名并更新引用
     */
    private fun expandClassNames(parsed: ParsedAxml, expansions: List<ClassNameExpansion>): ParsedAxml {
        val stringPool = parsed.stringPool
        
        for (expansion in expansions) {
            // 检查展开后的字符串是否已存在
            var newIndex = stringPool.strings.indexOf(expansion.expandedValue)
            
            if (newIndex < 0) {
                // 添加新字符串
                newIndex = stringPool.strings.size
                stringPool.strings.add(expansion.expandedValue)
                Log.d(TAG, "Added new string at index $newIndex: '${expansion.expandedValue}'")
            }
            
            // 更新 chunk 中的属性引用
            val chunk = parsed.chunks[expansion.chunkIndex]
            val buffer = ByteBuffer.wrap(chunk.data).order(ByteOrder.LITTLE_ENDIAN)
            
            // 更新 rawValue（字符串索引）
            buffer.position(expansion.attrOffset + 8)
            buffer.putInt(newIndex)
            
            // 更新 valueData（字符串索引）
            buffer.position(expansion.attrOffset + 16)
            buffer.putInt(newIndex)
        }
        
        return parsed
    }

    /**
     * 替换包名相关的字符串
     * 包括：package 属性、权限名称、provider authorities 等
     */
    private fun replacePackageString(parsed: ParsedAxml, oldPackage: String, newPackage: String) {
        val stringPool = parsed.stringPool
        
        for (i in stringPool.strings.indices) {
            val str = stringPool.strings[i]
            
            when {
                // 1. 完全匹配的包名（package 属性）
                str == oldPackage -> {
                    stringPool.strings[i] = newPackage
                    Log.d(TAG, "Replaced package at index $i: '$oldPackage' -> '$newPackage'")
                }
                
                // 2. 以包名开头的字符串（权限、authorities 等）
                // 例如：com.deepseek.permission.XXX -> c.abc123.permission.XXX
                str.startsWith("$oldPackage.") -> {
                    val newStr = newPackage + str.substring(oldPackage.length)
                    stringPool.strings[i] = newStr
                    Log.d(TAG, "Replaced prefixed string at index $i: '$str' -> '$newStr'")
                }
            }
        }
    }

    /**
     * 重建 AXML 文件
     */
    private fun rebuildAxml(parsed: ParsedAxml): ByteArray {
        val output = ByteArrayOutputStream()
        
        // 1. 重建字符串池
        val stringPoolData = rebuildStringPool(parsed.stringPool)
        
        // 2. 重建资源映射
        val resourceMapData = if (parsed.resourceMap != null) {
            rebuildResourceMap(parsed.resourceMap)
        } else {
            ByteArray(0)
        }
        
        // 3. 收集所有其他 chunks
        val chunksData = ByteArrayOutputStream()
        for (chunk in parsed.chunks) {
            chunksData.write(chunk.data)
        }
        
        // 4. 计算总大小
        val totalSize = parsed.fileHeaderSize + stringPoolData.size + resourceMapData.size + chunksData.size()
        
        // 5. 写入文件头
        val header = ByteBuffer.allocate(parsed.fileHeaderSize).order(ByteOrder.LITTLE_ENDIAN)
        header.putShort(CHUNK_AXML_FILE.toShort())
        header.putShort(parsed.fileHeaderSize.toShort())
        header.putInt(totalSize)
        output.write(header.array())
        
        // 6. 写入字符串池
        output.write(stringPoolData)
        
        // 7. 写入资源映射
        output.write(resourceMapData)
        
        // 8. 写入其他 chunks
        chunksData.writeTo(output)
        
        return output.toByteArray()
    }

    /**
     * 重建字符串池
     */
    private fun rebuildStringPool(pool: StringPool): ByteArray {
        val isUtf8 = pool.isUtf8
        val stringCount = pool.strings.size
        val styleCount = pool.styleOffsets.size
        
        // 序列化字符串数据
        val stringsBuffer = ByteArrayOutputStream()
        val stringOffsets = IntArray(stringCount)
        
        for (i in 0 until stringCount) {
            stringOffsets[i] = stringsBuffer.size()
            val str = pool.strings[i]
            
            if (isUtf8) {
                writeUtf8String(stringsBuffer, str)
            } else {
                writeUtf16String(stringsBuffer, str)
            }
        }
        
        // 对齐到 4 字节
        while (stringsBuffer.size() % 4 != 0) {
            stringsBuffer.write(0)
        }
        
        val stringsData = stringsBuffer.toByteArray()
        
        // 计算各部分大小
        val headerSize = 28
        val offsetsSize = (stringCount + styleCount) * 4
        val stringsStart = headerSize + offsetsSize
        val stylesStart = if (styleCount > 0 && pool.stylesData != null) {
            stringsStart + stringsData.size
        } else {
            0
        }
        val stylesDataSize = pool.stylesData?.size ?: 0
        val chunkSize = stringsStart + stringsData.size + stylesDataSize
        
        // 构建字符串池 chunk
        val result = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        
        // Chunk header
        result.putShort(CHUNK_STRING_POOL.toShort())
        result.putShort(headerSize.toShort())
        result.putInt(chunkSize)
        result.putInt(stringCount)
        result.putInt(styleCount)
        result.putInt(if (isUtf8) 0x100 else 0) // flags
        result.putInt(stringsStart)
        result.putInt(stylesStart)
        
        // String offsets
        for (offset in stringOffsets) {
            result.putInt(offset)
        }
        
        // Style offsets
        for (offset in pool.styleOffsets) {
            result.putInt(offset)
        }
        
        // String data
        result.put(stringsData)
        
        // Styles data
        pool.stylesData?.let { result.put(it) }
        
        return result.array()
    }

    /**
     * 重建资源映射
     */
    private fun rebuildResourceMap(resourceMap: IntArray): ByteArray {
        val size = 8 + resourceMap.size * 4
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(CHUNK_RESOURCE_MAP.toShort())
        buffer.putShort(8.toShort())
        buffer.putInt(size)
        for (id in resourceMap) {
            buffer.putInt(id)
        }
        return buffer.array()
    }

    /**
     * 简单的包名替换（当没有相对路径类名时使用）
     */
    private fun simplePackageReplace(data: ByteArray, oldPackage: String, newPackage: String): ByteArray {
        val result = data.copyOf()
        
        // UTF-8 替换
        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_8)
        
        // UTF-16LE 替换
        replacePackageBytes(result, oldPackage, newPackage, Charsets.UTF_16LE)
        
        return result
    }

    private fun replacePackageBytes(data: ByteArray, oldPkg: String, newPkg: String, charset: java.nio.charset.Charset) {
        val oldBytes = oldPkg.toByteArray(charset)
        val newBytes = newPkg.toByteArray(charset)
        
        if (newBytes.size > oldBytes.size) return
        
        val replacement = if (newBytes.size == oldBytes.size) {
            newBytes
        } else {
            newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
        }
        
        val isUtf16 = charset == Charsets.UTF_16LE
        var i = 0
        
        while (i <= data.size - oldBytes.size) {
            var match = true
            for (j in oldBytes.indices) {
                if (data[i + j] != oldBytes[j]) {
                    match = false
                    break
                }
            }
            
            if (match) {
                // 检查是否是独立字符串
                val nextIndex = i + oldBytes.size
                var isIndependent = nextIndex >= data.size
                
                if (!isIndependent) {
                    if (isUtf16) {
                        if (nextIndex + 1 < data.size) {
                            isIndependent = data[nextIndex] == 0.toByte() && data[nextIndex + 1] == 0.toByte()
                        }
                    } else {
                        isIndependent = data[nextIndex] == 0.toByte()
                    }
                }
                
                if (isIndependent) {
                    System.arraycopy(replacement, 0, data, i, replacement.size)
                    i += oldBytes.size
                } else {
                    i++
                }
            } else {
                i++
            }
        }
    }

    // ========== 辅助方法 ==========

    private fun readUtf8String(data: ByteArray, offset: Int): String {
        if (offset >= data.size) return ""
        var o = offset
        
        // 读取字符长度（可能是 1 或 2 字节）
        var charLen = data[o].toInt() and 0x7F
        if (data[o].toInt() and 0x80 != 0) {
            if (o + 1 >= data.size) return ""
            charLen = ((data[o].toInt() and 0x7F) shl 8) or (data[o + 1].toInt() and 0xFF)
            o += 2
        } else {
            o += 1
        }
        
        // 读取字节长度
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

    private fun readUtf16String(data: ByteArray, offset: Int): String {
        if (offset + 2 > data.size) return ""
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        buffer.position(offset)
        
        var length = buffer.short.toInt() and 0xFFFF
        if (length and 0x8000 != 0) {
            if (offset + 4 > data.size) return ""
            length = ((length and 0x7FFF) shl 16) or (buffer.short.toInt() and 0xFFFF)
        }
        
        val byteLen = length * 2
        if (buffer.position() + byteLen > data.size) return ""
        
        val strBytes = ByteArray(byteLen)
        buffer.get(strBytes)
        return String(strBytes, Charsets.UTF_16LE)
    }

    private fun writeUtf8String(output: ByteArrayOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        val charLen = str.length
        val byteLen = bytes.size
        
        // 写入字符长度
        if (charLen > 0x7F) {
            output.write(0x80 or ((charLen shr 8) and 0x7F))
            output.write(charLen and 0xFF)
        } else {
            output.write(charLen)
        }
        
        // 写入字节长度
        if (byteLen > 0x7F) {
            output.write(0x80 or ((byteLen shr 8) and 0x7F))
            output.write(byteLen and 0xFF)
        } else {
            output.write(byteLen)
        }
        
        // 写入字符串数据
        output.write(bytes)
        
        // 写入 null 终止符
        output.write(0)
    }

    private fun writeUtf16String(output: ByteArrayOutputStream, str: String) {
        val length = str.length
        
        // 写入长度
        if (length > 0x7FFF) {
            output.write(0x80 or ((length shr 24) and 0x7F))
            output.write((length shr 16) and 0xFF)
            output.write((length shr 8) and 0xFF)
            output.write(length and 0xFF)
        } else {
            output.write(length and 0xFF)
            output.write((length shr 8) and 0xFF)
        }
        
        // 写入字符串数据
        val bytes = str.toByteArray(Charsets.UTF_16LE)
        output.write(bytes)
        
        // 写入 null 终止符（2 字节）
        output.write(0)
        output.write(0)
    }

    // ========== 数据类 ==========

    private data class ParsedAxml(
        val fileHeaderSize: Int,
        val stringPool: StringPool,
        val resourceMap: IntArray?,
        val chunks: List<Chunk>
    )

    private data class StringPool(
        val isUtf8: Boolean,
        val strings: MutableList<String>,
        val styleOffsets: IntArray,
        val stylesData: ByteArray?
    )

    private data class Chunk(
        val type: Int,
        val offset: Int,
        val size: Int,
        val data: ByteArray
    )

    private data class ClassNameExpansion(
        val chunkIndex: Int,
        val attrIndex: Int,
        val attrOffset: Int,
        val originalStringIndex: Int,
        val originalValue: String,
        val expandedValue: String
    )
}
