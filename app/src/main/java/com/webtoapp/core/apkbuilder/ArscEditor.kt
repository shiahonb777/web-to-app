package com.webtoapp.core.apkbuilder

import com.webtoapp.core.logging.AppLogger








class ArscEditor {

    companion object {
        private const val TAG = "ArscEditor"


        private const val PAD_CHAR = '\u0000'
    }












    fun modifyAppName(arscData: ByteArray, oldAppName: String, newAppName: String): ByteArray {
        AppLogger.d(TAG, "modifyAppName: old length=${oldAppName.length}chars, new='$newAppName'")


        val oldNameVariants = listOf(
            oldAppName,
            "WebToApp - Convert Any Website to Android App",
            "WebToApp" + "\u00A0".repeat(88),
            "WebToApp" + "\u00AD".repeat(88),
            "WebToApp" + "\u200B".repeat(57),
            "WebToApp",
            "webtoapp"
        ).distinct()

        var result = arscData
        var usedEncoding = "none"
        var matchedVariant = ""

        for (oldName in oldNameVariants) {
            if (usedEncoding != "none") break


            val utf8Result = replaceStringByBytes(result, oldName, newAppName, Charsets.UTF_8)
            val utf8Changed = !utf8Result.contentEquals(result)

            if (utf8Changed) {
                result = utf8Result
                usedEncoding = "utf8"
                matchedVariant = "variant(${oldName.length}chars)"
                AppLogger.d(TAG, "UTF-8 match found, oldBytes=${oldName.toByteArray(Charsets.UTF_8).size}")
            } else {

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










    private fun replaceStringByBytes(
        data: ByteArray,
        oldStr: String,
        newStr: String,
        charset: java.nio.charset.Charset
    ): ByteArray {
        val oldBytes = oldStr.toByteArray(charset)
        val targetByteLen = oldBytes.size


        val safeNewStr = adjustStringToByteLength(newStr, targetByteLen, charset)
        val newBytes = safeNewStr.toByteArray(charset)


        val replacement = when {
            newBytes.size == targetByteLen -> newBytes
            newBytes.size < targetByteLen -> {

                val result = ByteArray(targetByteLen)
                System.arraycopy(newBytes, 0, result, 0, newBytes.size)

                val padBytes = PAD_CHAR.code.toByte()
                for (i in newBytes.size until targetByteLen) {
                    result[i] = padBytes
                }
                result
            }
            else -> {

                AppLogger.w(TAG, "字节长度调整异常: expected=$targetByteLen, got=${newBytes.size}")
                newBytes.copyOf(targetByteLen)
            }
        }

        AppLogger.d(TAG, "replaceStringByBytes: oldBytes=${oldBytes.size}, newBytes=${newBytes.size}, " +
                "replacement=${replacement.size}, charset=$charset")

        return replaceBytes(data, oldBytes, replacement)
    }




    private fun adjustStringToByteLength(str: String, targetByteLen: Int, charset: java.nio.charset.Charset): String {
        val fullBytes = str.toByteArray(charset)


        if (fullBytes.size <= targetByteLen) {
            return str
        }


        val builder = StringBuilder()
        var currentByteLen = 0

        for (char in str) {
            val charBytes = char.toString().toByteArray(charset)
            if (currentByteLen + charBytes.size <= targetByteLen) {
                builder.append(char)
                currentByteLen += charBytes.size
            } else {

                break
            }
        }

        AppLogger.d(TAG, "adjustStringToByteLength: '$str'(${fullBytes.size}B) -> '${builder}'(${currentByteLen}B), target=$targetByteLen")
        return builder.toString()
    }









    fun modifyIconPathsToPng(arscData: ByteArray): ByteArray {

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
