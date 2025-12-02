package com.webtoapp.core.apkbuilder

import android.util.Log

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
        // 使用 null 字符填充，这样显示时会被忽略，不会显示空格
        private const val PAD_CHAR = '\u0000'
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
                // 用 null 字符填充到目标长度，显示时会被忽略
                val result = ByteArray(targetByteLen)
                System.arraycopy(newBytes, 0, result, 0, newBytes.size)
                // 剩余字节保持为 0 (null)，不需要额外填充
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
        // 适配多种可能的前景图路径：
        // - res/drawable/ic_launcher_foreground.xml
        // - res/drawable-anydpi-v24/ic_launcher_foreground.xml（Gradle 常见打包结果）
        val candidates = listOf(
            "res/drawable/ic_launcher_foreground",
            "res/drawable-v24/ic_launcher_foreground",
            "res/drawable-anydpi-v24/ic_launcher_foreground"
        )

        var result = arscData
        var changed = false

        for (base in candidates) {
            val oldPath = "${base}.xml"
            val newPath = "${base}.png"

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

}
