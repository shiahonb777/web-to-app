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

        // 恢复关键组件（Application / Activity）的类名到原始包名，避免 ClassNotFound
        restoreComponentClassNames(result, newPackageName)

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
            } else {
                i++
            }
        }
        return found
    }
}
