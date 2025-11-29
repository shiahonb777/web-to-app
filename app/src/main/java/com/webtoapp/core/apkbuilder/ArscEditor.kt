package com.webtoapp.core.apkbuilder

import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android 资源表 (ARSC) 编辑器
 * 用于修改 resources.arsc 中的应用名称
 * 
 * 注意：这是一个简化实现，直接在字符串池中查找并替换 app_name 的值
 */
class ArscEditor {

    companion object {
        private const val ARSC_MAGIC = 0x0002
        private const val STRING_POOL_TYPE = 0x0001
    }

    /**
     * 修改应用名称
     * @param arscData 原始 ARSC 数据
     * @param oldAppName 原应用名（用于定位）
     * @param newAppName 新应用名
     * @return 修改后的 ARSC 数据
     */
    fun modifyAppName(arscData: ByteArray, oldAppName: String, newAppName: String): ByteArray {
        // 简化实现：直接在二进制数据中查找并替换字符串
        // 注意：这种方法要求新旧字符串长度相同或者新字符串更短
        
        // 首先尝试 UTF-8 编码查找
        val utf8Result = replaceStringInData(arscData, oldAppName, newAppName)
        val utf8Changed = !utf8Result.contentEquals(arscData)

        val finalResult = if (utf8Changed) {
            utf8Result
        } else {
            // 如果没找到，尝试 UTF-16LE 编码查找
            replaceUtf16StringInData(arscData, oldAppName, newAppName)
        }

        val usedEncoding = when {
            utf8Changed -> "utf8"
            !finalResult.contentEquals(arscData) -> "utf16"
            else -> "none"
        }

        Log.d(
            "ArscEditor",
            "modifyAppName: old='$oldAppName', new='$newAppName', encoding=$usedEncoding"
        )
        
        return finalResult
    }

    /**
     * 将 ARSC 中 ic_launcher_foreground 的文件路径从 .xml 改为 .png
     * 这样 ic_launcher.xml 仍然是 adaptive icon，但前景图会从 PNG 资源加载
     */
    fun modifyIconPathsToPng(arscData: ByteArray): ByteArray {
        val replacements = mapOf(
            "res/drawable/ic_launcher_foreground.xml" to "res/drawable/ic_launcher_foreground.png"
        )

        var result = arscData
        var changed = false

        for ((oldPath, newPath) in replacements) {
            if (oldPath.length != newPath.length) continue

            val before = result
            result = replaceBytes(
                result,
                oldPath.toByteArray(Charsets.UTF_8),
                newPath.toByteArray(Charsets.UTF_8)
            )
            if (!result.contentEquals(before)) {
                changed = true
            }
        }

        Log.d("ArscEditor", "modifyIconPathsToPng: changed=$changed")
        return result
    }

    /**
     * 在数据中查找并替换 UTF-8 字符串
     */
    private fun replaceStringInData(data: ByteArray, oldStr: String, newStr: String): ByteArray {
        val oldBytes = oldStr.toByteArray(Charsets.UTF_8)
        val newBytes = newStr.toByteArray(Charsets.UTF_8)
        
        // 如果新字符串更长，需要截断
        val replacement = if (newBytes.size <= oldBytes.size) {
            newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
        } else {
            newBytes.copyOf(oldBytes.size)
        }
        
        return replaceBytes(data, oldBytes, replacement)
    }

    /**
     * 在数据中查找并替换 UTF-16LE 字符串
     */
    private fun replaceUtf16StringInData(data: ByteArray, oldStr: String, newStr: String): ByteArray {
        val oldBytes = oldStr.toByteArray(Charsets.UTF_16LE)
        val newBytes = newStr.toByteArray(Charsets.UTF_16LE)
        
        val replacement = if (newBytes.size <= oldBytes.size) {
            newBytes + ByteArray(oldBytes.size - newBytes.size) { 0 }
        } else {
            newBytes.copyOf(oldBytes.size)
        }
        
        return replaceBytes(data, oldBytes, replacement)
    }

    /**
     * 字节数组替换
     */
    private fun replaceBytes(data: ByteArray, pattern: ByteArray, replacement: ByteArray): ByteArray {
        val result = data.copyOf()
        var i = 0
        while (i <= result.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (result[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                System.arraycopy(replacement, 0, result, i, replacement.size)
                i += pattern.size
            } else {
                i++
            }
        }
        return result
    }

    /**
     * 扫描并替换所有主字符串池中的指定字符串
     * 这是一个更精确的实现，解析 ARSC 结构
     */
    fun modifyStringInPool(arscData: ByteArray, targetStrings: Map<String, String>): ByteArray {
        if (targetStrings.isEmpty()) return arscData
        
        return try {
            val buffer = ByteBuffer.wrap(arscData).order(ByteOrder.LITTLE_ENDIAN)
            
            // 读取资源表头
            val type = buffer.short.toInt() and 0xFFFF
            val headerSize = buffer.short.toInt() and 0xFFFF
            buffer.int // tableSize (跳过)
            buffer.int // packageCount (跳过)
            
            if (type != ARSC_MAGIC) {
                return arscData
            }
            
            // 定位全局字符串池
            val stringPoolOffset = headerSize
            buffer.position(stringPoolOffset)
            
            val poolType = buffer.short.toInt() and 0xFFFF
            if (poolType != STRING_POOL_TYPE) {
                return arscData
            }
            
            val poolHeaderSize = buffer.short.toInt() and 0xFFFF
            buffer.int // poolSize (跳过)
            val stringCount = buffer.int
            val styleCount = buffer.int
            val flags = buffer.int
            val stringsStart = buffer.int
            buffer.int // stylesStart (跳过)
            
            val isUtf8 = (flags and 0x100) != 0
            
            // 读取字符串偏移
            val offsets = IntArray(stringCount) { buffer.int }
            
            // 跳过样式偏移
            repeat(styleCount) { buffer.int }
            
            // 读取字符串
            val stringsDataStart = stringPoolOffset + poolHeaderSize + stringsStart
            val strings = mutableListOf<String>()
            
            for (i in 0 until stringCount) {
                buffer.position(stringsDataStart + offsets[i])
                val str = if (isUtf8) {
                    readUtf8String(buffer)
                } else {
                    readUtf16String(buffer)
                }
                // 替换字符串
                val replaced = targetStrings[str] ?: str
                strings.add(replaced)
            }
            
            // 重建 ARSC（简化：只在原位置替换字符串内容）
            val result = arscData.copyOf()
            for (entry in targetStrings) {
                replaceInPlace(result, entry.key, entry.value)
            }
            
            result
            
        } catch (e: Exception) {
            arscData
        }
    }

    /**
     * 原地替换字符串（保持长度不变或填充空字节）
     */
    private fun replaceInPlace(data: ByteArray, oldStr: String, newStr: String) {
        // UTF-8
        val oldUtf8 = oldStr.toByteArray(Charsets.UTF_8)
        val newUtf8 = newStr.toByteArray(Charsets.UTF_8)
        if (newUtf8.size <= oldUtf8.size) {
            val padded = newUtf8 + ByteArray(oldUtf8.size - newUtf8.size) { 0 }
            replaceFirst(data, oldUtf8, padded)
        }
        
        // UTF-16LE
        val oldUtf16 = oldStr.toByteArray(Charsets.UTF_16LE)
        val newUtf16 = newStr.toByteArray(Charsets.UTF_16LE)
        if (newUtf16.size <= oldUtf16.size) {
            val padded = newUtf16 + ByteArray(oldUtf16.size - newUtf16.size) { 0 }
            replaceFirst(data, oldUtf16, padded)
        }
    }

    /**
     * 替换第一个匹配
     */
    private fun replaceFirst(data: ByteArray, pattern: ByteArray, replacement: ByteArray) {
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
                return
            }
            i++
        }
    }

    private fun readUtf8String(buffer: ByteBuffer): String {
        val charLen = buffer.get().toInt() and 0xFF
        val byteLen = if (charLen > 0x7F) {
            val b2 = buffer.get().toInt() and 0xFF
            ((charLen and 0x7F) shl 8) or b2
        } else {
            buffer.get().toInt() and 0xFF
        }
        val bytes = ByteArray(byteLen)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16String(buffer: ByteBuffer): String {
        val charLen = buffer.short.toInt() and 0xFFFF
        val chars = CharArray(charLen)
        for (i in 0 until charLen) {
            chars[i] = buffer.short.toInt().toChar()
        }
        return String(chars)
    }
}
