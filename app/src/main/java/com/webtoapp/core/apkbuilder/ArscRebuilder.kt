package com.webtoapp.core.apkbuilder

import android.util.Log
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
        Log.d(TAG, "rebuildWithNewAppName: target='$targetAppName' (${targetAppName.toByteArray(Charsets.UTF_8).size} bytes)")
        
        try {
            val buffer = ByteBuffer.wrap(arscData).order(ByteOrder.LITTLE_ENDIAN)
            
            // 1. 解析文件头
            val tableType = buffer.short
            val tableHeaderSize = buffer.short
            val tableSize = buffer.int
            val packageCount = buffer.int
            
            if (tableType != RES_TABLE_TYPE) {
                Log.e(TAG, "Invalid ARSC file: type=$tableType")
                return arscData
            }
            
            Log.d(TAG, "Table header: size=$tableSize, packages=$packageCount")
            
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
                Log.e(TAG, "Invalid string pool: type=$poolType")
                return arscData
            }
            
            val isUtf8 = (poolFlags and UTF8_FLAG) != 0
            Log.d(TAG, "String pool: count=$stringCount, utf8=$isUtf8, size=$poolSize")
            
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
                        Log.d(TAG, "Found app_name at index $i: '$str' -> '$targetAppName'")
                        strings[i] = targetAppName
                        modified = true
                        break
                    }
                }
                if (modified) break
            }
            
            if (!modified) {
                Log.w(TAG, "app_name not found in string pool, trying pattern matching...")
                // 尝试更宽松的匹配
                for (i in strings.indices) {
                    val str = strings[i]
                    if (str.trim().startsWith("WebToApp") && 
                        str.length < 100 && 
                        !str.contains("Theme") && 
                        !str.contains("http") &&
                        !str.contains(".")) {
                        Log.d(TAG, "Found potential app_name at index $i: '$str'")
                        strings[i] = targetAppName
                        modified = true
                        break
                    }
                }
            }
            
            if (!modified) {
                Log.e(TAG, "Could not find app_name to replace")
                return arscData
            }
            
            // 5. 重新构建字符串池
            val newStringPool = buildStringPool(strings, isUtf8, styleCount, poolFlags)
            
            // 6. 重新构建 ARSC 文件
            val afterStringPool = stringPoolStart + poolSize
            val remainingData = arscData.copyOfRange(afterStringPool, arscData.size)
            
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
            
            Log.d(TAG, "Rebuild complete: old size=${arscData.size}, new size=${newArsc.array().size}")
            return newArsc.array()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rebuild ARSC", e)
            return arscData
        }
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
