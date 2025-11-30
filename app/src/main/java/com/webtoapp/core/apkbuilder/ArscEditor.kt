package com.webtoapp.core.apkbuilder

import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android 资源表 (ARSC) 编辑器
 * 用于修改 resources.arsc 中的应用名称
 * 
 * 重要：ARSC 字符串池中的字符串带有长度前缀，简单替换必须保持字符串长度不变
 * 否则会导致 APK 解析失败
 */
class ArscEditor {

    companion object {
        private const val TAG = "ArscEditor"
        private const val ARSC_MAGIC = 0x0002
        private const val STRING_POOL_TYPE = 0x0001
        
        // 填充字符：使用空格而不是空字节，避免字符串被截断
        private const val PAD_CHAR = ' '
    }

    /**
     * 修改应用名称
     * 
     * 重要：按字节长度处理，确保替换后的字节数与原字符串完全相同
     * 这是因为 ARSC 字符串池的结构限制，无法改变字符串的实际字节长度
     * 
     * @param arscData 原始 ARSC 数据
     * @param oldAppName 原应用名（用于定位）
     * @param newAppName 新应用名
     * @return 修改后的 ARSC 数据
     */
    fun modifyAppName(arscData: ByteArray, oldAppName: String, newAppName: String): ByteArray {
        Log.d(TAG, "modifyAppName: old='$oldAppName', new='$newAppName'")
        
        // 首先尝试 UTF-8 编码查找和替换
        val utf8Result = replaceStringByBytes(arscData, oldAppName, newAppName, Charsets.UTF_8)
        val utf8Changed = !utf8Result.contentEquals(arscData)

        val finalResult = if (utf8Changed) {
            utf8Result
        } else {
            // 如果没找到，尝试 UTF-16LE 编码查找
            replaceStringByBytes(arscData, oldAppName, newAppName, Charsets.UTF_16LE)
        }

        val usedEncoding = when {
            utf8Changed -> "utf8"
            !finalResult.contentEquals(arscData) -> "utf16"
            else -> "none"
        }

        Log.d(TAG, "modifyAppName completed: encoding=$usedEncoding")
        
        return finalResult
    }
    
    /**
     * 按字节长度安全替换字符串
     * 核心逻辑：确保替换后的字节数与原字符串完全相同
     * 
     * 策略：
     * 1. 如果新字符串字节数 == 旧字符串字节数：直接替换
     * 2. 如果新字符串字节数 < 旧字符串字节数：用空格填充
     * 3. 如果新字符串字节数 > 旧字符串字节数：逐字符截断直到字节数合适
     */
    private fun replaceStringByBytes(
        data: ByteArray,
        oldStr: String,
        newStr: String,
        charset: java.nio.charset.Charset
    ): ByteArray {
        val oldBytes = oldStr.toByteArray(charset)
        val targetByteLen = oldBytes.size
        
        // 按字节长度安全调整新字符串
        val safeNewStr = adjustStringToByteLength(newStr, targetByteLen, charset)
        val newBytes = safeNewStr.toByteArray(charset)
        
        // 构建最终替换字节数组（确保长度完全匹配）
        val replacement = when {
            newBytes.size == targetByteLen -> newBytes
            newBytes.size < targetByteLen -> {
                // 用空格填充到目标长度
                val padBytes = PAD_CHAR.toString().toByteArray(charset)
                val result = ByteArray(targetByteLen)
                System.arraycopy(newBytes, 0, result, 0, newBytes.size)
                var pos = newBytes.size
                while (pos + padBytes.size <= targetByteLen) {
                    System.arraycopy(padBytes, 0, result, pos, padBytes.size)
                    pos += padBytes.size
                }
                // 处理剩余不足一个填充字符的字节
                while (pos < targetByteLen) {
                    result[pos] = 0x20 // ASCII 空格
                    pos++
                }
                result
            }
            else -> {
                // 理论上不应该到这里，因为 adjustStringToByteLength 已处理
                Log.w(TAG, "字节长度调整异常: expected=$targetByteLen, got=${newBytes.size}")
                newBytes.copyOf(targetByteLen)
            }
        }
        
        Log.d(TAG, "replaceStringByBytes: oldBytes=${oldBytes.size}, newBytes=${newBytes.size}, " +
                "replacement=${replacement.size}, charset=$charset")
        
        return replaceBytes(data, oldBytes, replacement)
    }
    
    /**
     * 将字符串调整到指定的字节长度（按完整字符截断，不破坏编码）
     */
    private fun adjustStringToByteLength(str: String, targetByteLen: Int, charset: java.nio.charset.Charset): String {
        val fullBytes = str.toByteArray(charset)
        
        // 如果已经符合或更短，直接返回
        if (fullBytes.size <= targetByteLen) {
            return str
        }
        
        // 逐字符截断，确保不破坏多字节字符
        val builder = StringBuilder()
        var currentByteLen = 0
        
        for (char in str) {
            val charBytes = char.toString().toByteArray(charset)
            if (currentByteLen + charBytes.size <= targetByteLen) {
                builder.append(char)
                currentByteLen += charBytes.size
            } else {
                // 无法再添加完整字符，停止
                break
            }
        }
        
        Log.d(TAG, "adjustStringToByteLength: '$str'(${fullBytes.size}B) -> '${builder}'(${currentByteLen}B), target=$targetByteLen")
        return builder.toString()
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
     * 将资源表中出现的旧包名替换为新的包名，避免 Provider authority 等字符串冲突
     */
    fun replacePackageStrings(arscData: ByteArray, oldPackage: String, newPackage: String): ByteArray {
        var result = replaceStringInData(arscData, oldPackage, newPackage)
        result = replaceUtf16StringInData(result, oldPackage, newPackage)
        return result
    }

    /**
     * 安全地在数据中查找并替换字符串（保留用于兼容旧调用）
     */
    private fun replaceStringInDataSafe(
        data: ByteArray, 
        oldStr: String, 
        newStr: String,
        charset: java.nio.charset.Charset
    ): ByteArray {
        return replaceStringByBytes(data, oldStr, newStr, charset)
    }

    /**
     * 在数据中查找并替换 UTF-8 字符串（兼容旧方法）
     */
    private fun replaceStringInData(data: ByteArray, oldStr: String, newStr: String): ByteArray {
        return replaceStringByBytes(data, oldStr, newStr, Charsets.UTF_8)
    }

    /**
     * 在数据中查找并替换 UTF-16LE 字符串（兼容旧方法）
     */
    private fun replaceUtf16StringInData(data: ByteArray, oldStr: String, newStr: String): ByteArray {
        return replaceStringByBytes(data, oldStr, newStr, Charsets.UTF_16LE)
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
     * 原地替换字符串（按字节长度保持不变，用空格填充）
     */
    private fun replaceInPlace(data: ByteArray, oldStr: String, newStr: String) {
        // UTF-8 替换
        replaceInPlaceWithCharset(data, oldStr, newStr, Charsets.UTF_8)
        // UTF-16LE 替换
        replaceInPlaceWithCharset(data, oldStr, newStr, Charsets.UTF_16LE)
    }
    
    /**
     * 使用指定编码原地替换字符串
     */
    private fun replaceInPlaceWithCharset(data: ByteArray, oldStr: String, newStr: String, charset: java.nio.charset.Charset) {
        val oldBytes = oldStr.toByteArray(charset)
        val targetLen = oldBytes.size
        
        // 按字节长度安全调整新字符串
        val safeNewStr = adjustStringToByteLength(newStr, targetLen, charset)
        val newBytes = safeNewStr.toByteArray(charset)
        
        // 构建填充后的替换字节数组
        val padded = ByteArray(targetLen)
        System.arraycopy(newBytes, 0, padded, 0, newBytes.size)
        
        // 用空格填充剩余部分
        val padChar = PAD_CHAR.toString().toByteArray(charset)
        var pos = newBytes.size
        while (pos + padChar.size <= targetLen) {
            System.arraycopy(padChar, 0, padded, pos, padChar.size)
            pos += padChar.size
        }
        // 处理剩余不足一个填充字符的字节
        while (pos < targetLen) {
            padded[pos] = 0x20 // ASCII 空格
            pos++
        }
        
        replaceFirst(data, oldBytes, padded)
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
