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

    // WebToApp 基础名称（不含零宽空格）
    private val BASE_APP_NAME = "WebToApp"
    // 零宽空格的 UTF-8 编码
    private val ZWSP_UTF8 = byteArrayOf(0xE2.toByte(), 0x80.toByte(), 0x8B.toByte())
    // 零宽空格的 UTF-16LE 编码
    private val ZWSP_UTF16LE = byteArrayOf(0x0B.toByte(), 0x20.toByte())

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
        Log.d(TAG, "modifyAppName: old='$oldAppName'(${oldAppName.length}chars), new='$newAppName'(${newAppName.length}chars)")
        
        // 策略1：首先尝试精确匹配完整的原始字符串（UTF-8）
        var result = replaceStringByBytes(arscData, oldAppName, newAppName, Charsets.UTF_8)
        if (!result.contentEquals(arscData)) {
            Log.d(TAG, "modifyAppName completed: encoding=utf8 (exact match)")
            return result
        }
        
        // 策略2：尝试精确匹配（UTF-16LE）
        result = replaceStringByBytes(arscData, oldAppName, newAppName, Charsets.UTF_16LE)
        if (!result.contentEquals(arscData)) {
            Log.d(TAG, "modifyAppName completed: encoding=utf16 (exact match)")
            return result
        }
        
        // 策略3：如果精确匹配失败，且原始名称以 "WebToApp" 开头，尝试智能扫描模式
        // 这样可以避免在克隆第三方应用时误匹配
        if (oldAppName.startsWith(BASE_APP_NAME)) {
            Log.w(TAG, "精确匹配失败，尝试智能扫描模式...")
            result = smartReplaceAppName(arscData, newAppName)
            if (!result.contentEquals(arscData)) {
                Log.d(TAG, "modifyAppName completed: encoding=smart-scan")
                return result
            }
        }
        
        Log.e(TAG, "modifyAppName 失败：未找到匹配的应用名称字符串")
        return arscData
    }
    
    /**
     * 智能扫描替换：查找 "WebToApp" 后跟零宽空格的模式
     * 这种方式更健壮，可以处理 AAPT2 编译时可能修改零宽空格数量的情况
     */
    private fun smartReplaceAppName(arscData: ByteArray, newAppName: String): ByteArray {
        val result = arscData.copyOf()
        var totalReplaced = 0
        
        // 尝试 UTF-8 编码
        val utf8Replaced = smartReplaceWithEncoding(result, newAppName, Charsets.UTF_8, ZWSP_UTF8)
        totalReplaced += utf8Replaced
        
        // 尝试 UTF-16LE 编码
        val utf16Replaced = smartReplaceWithEncoding(result, newAppName, Charsets.UTF_16LE, ZWSP_UTF16LE)
        totalReplaced += utf16Replaced
        
        Log.d(TAG, "smartReplaceAppName: utf8替换=$utf8Replaced, utf16替换=$utf16Replaced")
        
        return result
    }
    
    /**
     * 使用指定编码进行智能替换
     * 查找 "WebToApp" 后跟连续零宽空格的模式，计算总字节长度后替换
     */
    private fun smartReplaceWithEncoding(
        data: ByteArray,
        newAppName: String,
        charset: java.nio.charset.Charset,
        zwspBytes: ByteArray
    ): Int {
        val baseBytes = BASE_APP_NAME.toByteArray(charset)
        var replacedCount = 0
        var i = 0
        
        while (i <= data.size - baseBytes.size) {
            // 检查是否匹配 "WebToApp"
            if (matchBytes(data, i, baseBytes)) {
                // 找到基础名称，计算后续零宽空格数量
                var zwspCount = 0
                var scanPos = i + baseBytes.size
                
                while (scanPos + zwspBytes.size <= data.size && matchBytes(data, scanPos, zwspBytes)) {
                    zwspCount++
                    scanPos += zwspBytes.size
                }
                
                // 只有当找到至少一个零宽空格时才认为是目标字符串
                if (zwspCount > 0) {
                    val totalOriginalLen = baseBytes.size + zwspCount * zwspBytes.size
                    Log.d(TAG, "智能匹配成功: pos=$i, baseLen=${baseBytes.size}, zwspCount=$zwspCount, totalLen=$totalOriginalLen, charset=$charset")
                    
                    // 构建替换内容
                    val replacement = buildReplacement(newAppName, totalOriginalLen, charset)
                    System.arraycopy(replacement, 0, data, i, replacement.size)
                    
                    replacedCount++
                    i += totalOriginalLen
                    continue
                }
            }
            i++
        }
        
        return replacedCount
    }
    
    /**
     * 构建替换字节数组，确保长度与原始字符串完全一致
     */
    private fun buildReplacement(newAppName: String, targetLen: Int, charset: java.nio.charset.Charset): ByteArray {
        val newBytes = newAppName.toByteArray(charset)
        val result = ByteArray(targetLen)
        
        if (newBytes.size >= targetLen) {
            // 新名称太长，需要截断（按字符截断以避免破坏编码）
            val safeStr = adjustStringToByteLength(newAppName, targetLen, charset)
            val safeBytes = safeStr.toByteArray(charset)
            System.arraycopy(safeBytes, 0, result, 0, safeBytes.size.coerceAtMost(targetLen))
        } else {
            // 新名称较短，复制后剩余填充 null
            System.arraycopy(newBytes, 0, result, 0, newBytes.size)
            // 剩余位置已经是 0 (null)，无需额外操作
        }
        
        return result
    }
    
    /**
     * 检查 data 从 offset 开始是否匹配 pattern
     */
    private fun matchBytes(data: ByteArray, offset: Int, pattern: ByteArray): Boolean {
        if (offset + pattern.size > data.size) return false
        for (j in pattern.indices) {
            if (data[offset + j] != pattern[j]) return false
        }
        return true
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
     * 将 ARSC 中 ic_launcher_foreground 的文件路径从 .xml 改为 .png
     * 这样 ic_launcher.xml 仍然是 adaptive icon，但前景图会从 PNG 资源加载
     * （完全沿用旧项目实现）
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
     * 强制替换图标路径（用于兼容 Adaptive Icon -> PNG 策略）
     *
     * 与旧版本 modifyIconPathsToPng 完全一致：
     * - 只替换 foreground 路径，让 Adaptive Icon XML 中的 foreground 引用指向我们的 PNG
     * - 不替换 mipmap-anydpi-v26 的入口路径（保留 Adaptive Icon XML 定义）
     *
     * 原理：
     * - Adaptive Icon XML (res/mipmap-anydpi-v26/ic_launcher.xml) 包含 foreground 和 background 引用
     * - 我们只需要把 foreground 的引用从 .xml 改成 .png
     * - 系统解析 Adaptive Icon 时会加载我们写入的 foreground PNG
     */
    fun forceReplaceIconPaths(arscData: ByteArray): ByteArray {
        var result = arscData

        // ========== 只替换 Adaptive Icon Foreground 路径（与旧版本完全一致）==========
        // 不替换 mipmap-anydpi-v26 入口路径！保留 Adaptive Icon XML 定义
        result = replaceIconPathSuffix(
            result,
            "res/drawable/ic_launcher_foreground.xml",
            "res/drawable/ic_launcher_foreground.png"
        )

        result = replaceIconPathSuffix(
            result,
            "res/drawable-v24/ic_launcher_foreground.xml",
            "res/drawable-v24/ic_launcher_foreground.png"
        )

        result = replaceIconPathSuffix(
            result,
            "res/drawable-anydpi-v24/ic_launcher_foreground.xml",
            "res/drawable-anydpi-v24/ic_launcher_foreground.png"
        )

        return result
    }

    /**
     * 将 ARSC 中某个图标资源路径从 .xml 替换为 .png（保持总长度不变）
     */
    private fun replaceIconPathSuffix(
        data: ByteArray,
        oldPath: String,
        newPath: String
    ): ByteArray {
        if (oldPath.length != newPath.length) {
            Log.w(TAG, "replaceIconPathSuffix 被调用时路径长度不一致，跳过: old='$oldPath', new='$newPath'")
            return data
        }

        val oldBytes = oldPath.toByteArray(Charsets.UTF_8)
        val newBytes = newPath.toByteArray(Charsets.UTF_8)

        Log.d(TAG, "replaceIconPathSuffix: '$oldPath' -> '$newPath'")

        return replaceBytes(data, oldBytes, newBytes)
    }

}
