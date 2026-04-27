package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger

/**
 * Note: brief English comment.
 * Note: brief English comment.
 * 
 * Note: brief English comment.
 * Note: brief English comment.
 */
class ArscEditor {

    companion object {
        private const val TAG = "ArscEditor"
        // Note: brief English comment.
        // Note: brief English comment.
        private const val PAD_CHAR = '\u0000'
    }

    /**
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun modifyAppName(arscData: ByteArray, oldAppName: String, newAppName: String): ByteArray {
        AppLogger.d(TAG, "modifyAppName: old length=${oldAppName.length}chars, new='$newAppName'")
        
        // Note: brief English comment.
        val oldNameVariants = listOf(
            oldAppName,                                                    // 当前版本
            "WebToApp - Convert Any Website to Android App",              // 当前版本明确值
            "WebToApp" + "\u00A0".repeat(88),                             // 兼容旧版不间断空格填充
            "WebToApp" + "\u00AD".repeat(88),                             // 兼容软连字符填充
            "WebToApp" + "\u200B".repeat(57),                             // 兼容零宽度空格填充
            "WebToApp",                                                   // 兼容简短版本
            "webtoapp"                                                    // 兼容小写形式
        ).distinct()
        
        var result = arscData
        var usedEncoding = "none"
        var matchedVariant = ""
        
        for (oldName in oldNameVariants) {
            if (usedEncoding != "none") break
            
            // Note: brief English comment.
            val utf8Result = replaceStringByBytes(result, oldName, newAppName, Charsets.UTF_8)
            val utf8Changed = !utf8Result.contentEquals(result)
            
            if (utf8Changed) {
                result = utf8Result
                usedEncoding = "utf8"
                matchedVariant = "variant(${oldName.length}chars)"
                AppLogger.d(TAG, "UTF-8 match found, oldBytes=${oldName.toByteArray(Charsets.UTF_8).size}")
            } else {
                // Note: brief English comment.
                val utf16Result = replaceStringByBytes(result, oldName, newAppName, Charsets.UTF_16LE)
                val utf16Changed = !utf16Result.contentEquals(result)
                
                if (utf16Changed) {
                    result = utf16Result
                    usedEncoding = "utf16"
                    matchedVariant = "variant(${oldName.length}chars)"
                    AppLogger.d(TAG, "UTF-16LE match found, oldBytes=${oldName.toByteArray(Charsets.UTF_16LE).size}")
                }
            }
        }

        AppLogger.d(TAG, "modifyAppName completed: encoding=$usedEncoding, $matchedVariant")
        
        return result
    }
    
    /**
     * Note: brief English comment.
     * Note: brief English comment.
     * 
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    private fun replaceStringByBytes(
        data: ByteArray,
        oldStr: String,
        newStr: String,
        charset: java.nio.charset.Charset
    ): ByteArray {
        val oldBytes = oldStr.toByteArray(charset)
        val targetByteLen = oldBytes.size
        
        // Note: brief English comment.
        val safeNewStr = adjustStringToByteLength(newStr, targetByteLen, charset)
        val newBytes = safeNewStr.toByteArray(charset)
        
        // Note: brief English comment.
        val replacement = when {
            newBytes.size == targetByteLen -> newBytes
            newBytes.size < targetByteLen -> {
                // Note: brief English comment.
                val result = ByteArray(targetByteLen)
                System.arraycopy(newBytes, 0, result, 0, newBytes.size)
                // Note: brief English comment.
                val padBytes = PAD_CHAR.code.toByte()
                for (i in newBytes.size until targetByteLen) {
                    result[i] = padBytes
                }
                result
            }
            else -> {
                // Note: brief English comment.
                AppLogger.w(TAG, "字节长度调整异常: expected=$targetByteLen, got=${newBytes.size}")
                newBytes.copyOf(targetByteLen)
            }
        }
        
        AppLogger.d(TAG, "replaceStringByBytes: oldBytes=${oldBytes.size}, newBytes=${newBytes.size}, " +
                "replacement=${replacement.size}, charset=$charset")
        
        return replaceBytes(data, oldBytes, replacement)
    }
    
    /**
     * Note: brief English comment.
     */
    private fun adjustStringToByteLength(str: String, targetByteLen: Int, charset: java.nio.charset.Charset): String {
        val fullBytes = str.toByteArray(charset)
        
        // Note: brief English comment.
        if (fullBytes.size <= targetByteLen) {
            return str
        }
        
        // Note: brief English comment.
        val builder = StringBuilder()
        var currentByteLen = 0
        
        for (char in str) {
            val charBytes = char.toString().toByteArray(charset)
            if (currentByteLen + charBytes.size <= targetByteLen) {
                builder.append(char)
                currentByteLen += charBytes.size
            } else {
                // Note: brief English comment.
                break
            }
        }
        
        AppLogger.d(TAG, "adjustStringToByteLength: '$str'(${fullBytes.size}B) -> '${builder}'(${currentByteLen}B), target=$targetByteLen")
        return builder.toString()
    }

    /**
     * Note: brief English comment.
     * Note: brief English comment.
     *
     * Note: brief English comment.
     * Note: brief English comment.
     * Note: brief English comment.
     */
    fun modifyIconPathsToPng(arscData: ByteArray): ByteArray {
        // Note: brief English comment.
        val candidates = listOf(
            "res/drawable/ic_launcher_foreground",
            "res/drawable/ic_launcher_foreground_new",
            "res/drawable-v24/ic_launcher_foreground",
            "res/drawable-v24/ic_launcher_foreground_new",
            "res/drawable-anydpi-v24/ic_launcher_foreground",
            "res/drawable-anydpi-v24/ic_launcher_foreground_new"
        )

        var result = arscData
        var changed = false

        // Note: brief English comment.
        val extensionPairs = listOf(
            ".xml" to ".png",
            ".jpg" to ".png"
        )

        for (base in candidates) {
            for ((oldExt, newExt) in extensionPairs) {
                val oldPath = "${base}${oldExt}"
                val newPath = "${base}${newExt}"

                if (oldPath.length != newPath.length) continue

                val before = result
                result = replaceBytes(
                    result,
                    oldPath.toByteArray(Charsets.UTF_8),
                    newPath.toByteArray(Charsets.UTF_8)
                )
                if (!result.contentEquals(before)) {
                    changed = true
                    AppLogger.d("ArscEditor", "modifyIconPathsToPng: replaced $oldPath -> $newPath")
                }
            }
        }

        AppLogger.d("ArscEditor", "modifyIconPathsToPng: changed=$changed")
        return result
    }


    /**
     * Note: brief English comment.
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
