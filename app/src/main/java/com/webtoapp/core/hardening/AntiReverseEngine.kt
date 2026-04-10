package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 反逆向工程引擎
 * 
 * 超越 360 加固的多维度反逆向保护：
 * 
 * 1. 多层反调试（4 层防护）：
 *    - L1: ptrace 自占位（抢占调试位）
 *    - L2: 时序检测（检测单步执行导致的时间异常）
 *    - L3: 信号陷阱（SIGTRAP 自处理，干扰调试器）
 *    - L4: 子线程监控（专用线程持续检查 TracerPid）
 * 
 * 2. 高级 Frida 检测（5 层检测）：
 *    - 端口扫描（27042-27045 + 自定义端口）
 *    - /proc/self/maps 内存映射扫描
 *    - 进程列表扫描（frida-server/frida-agent）
 *    - 内存特征扫描（搜索 Frida 字符串模式）
 *    - 线程名扫描（检测 gum-js-loop/gmain 等特征线程）
 * 
 * 3. 深度 Xposed/LSPosed 检测：
 *    - 类加载检测（XposedBridge 等）
 *    - ART 方法 Hook 检测（检查方法入口点是否被修改）
 *    - 堆栈痕迹分析（异常堆栈中的 Xposed 帧）
 * 
 * 4. Magisk/Shamiko 检测：
 *    - 挂载信息分析（/proc/self/mountinfo）
 *    - 系统属性检测（通过 Native 调用）
 *    - MagiskHide 绕过检测
 * 
 * 5. 反内存 Dump：
 *    - mprotect 保护关键代码页
 *    - inotify 监控 /proc/self/maps
 * 
 * 6. 反截屏：
 *    - WindowManager.LayoutParams.FLAG_SECURE
 */
class AntiReverseEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "AntiReverseEngine"
    }
    
    /**
     * 反逆向配置
     */
    data class AntiReverseConfig(
        val multiLayerAntiDebug: Boolean = true,
        val advancedFridaDetection: Boolean = true,
        val deepXposedDetection: Boolean = true,
        val magiskDetection: Boolean = false,
        val antiMemoryDump: Boolean = false,
        val antiScreenCapture: Boolean = false
    )
    
    /**
     * 写入反逆向配置到 APK
     */
    fun writeAntiReverseConfig(zipOut: ZipOutputStream, config: AntiReverseConfig) {
        AppLogger.d(TAG, "写入反逆向配置")
        
        val configData = generateAntiReverseConfigData(config)
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/anti_reverse.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(configData)
        zipOut.closeEntry()
        
        // 写入反调试检查点配置
        if (config.multiLayerAntiDebug) {
            writeAntiDebugCheckpoints(zipOut)
        }
        
        // 写入 Frida 检测特征库
        if (config.advancedFridaDetection) {
            writeFridaSignatures(zipOut)
        }
        
        // 写入 Xposed 检测配置
        if (config.deepXposedDetection) {
            writeXposedDetectionConfig(zipOut)
        }
        
        AppLogger.d(TAG, "反逆向配置写入完成")
    }
    
    /**
     * 生成反逆向配置数据
     */
    private fun generateAntiReverseConfigData(config: AntiReverseConfig): ByteArray {
        val data = ByteArrayOutputStream()
        
        // 魔数
        data.write(byteArrayOf(0x41, 0x52, 0x45, 0x56)) // "AREV"
        
        // 版本
        data.write(byteArrayOf(0x00, 0x01))
        
        // 配置位图
        var bitmap = 0
        if (config.multiLayerAntiDebug) bitmap = bitmap or 0x01
        if (config.advancedFridaDetection) bitmap = bitmap or 0x02
        if (config.deepXposedDetection) bitmap = bitmap or 0x04
        if (config.magiskDetection) bitmap = bitmap or 0x08
        if (config.antiMemoryDump) bitmap = bitmap or 0x10
        if (config.antiScreenCapture) bitmap = bitmap or 0x20
        data.write(byteArrayOf(bitmap.toByte()))
        
        // 检测间隔（毫秒）
        data.write(byteArrayOf(0x00, 0x00, 0x0B, 0xB8.toByte())) // 3000ms
        
        // 随机化延迟范围（毫秒）
        data.write(byteArrayOf(0x00, 0x00, 0x03, 0xE8.toByte())) // ±1000ms
        
        // 多层反调试配置
        if (config.multiLayerAntiDebug) {
            // 4 层检测
            data.write(byteArrayOf(0x04))
            // 各层类型
            data.write(byteArrayOf(
                0x01, // L1: ptrace
                0x02, // L2: timing
                0x03, // L3: signal
                0x04  // L4: thread monitor
            ))
            // 时序检测阈值（纳秒）
            data.write(byteArrayOf(0x00, 0x00, 0x27, 0x10)) // 10000ns
        }
        
        // 随机填充
        val padding = ByteArray(24)
        SecureRandom().nextBytes(padding)
        data.write(padding)
        
        return data.toByteArray()
    }
    
    /**
     * 写入反调试检查点
     * 在代码中多个位置插入检查点，即使绕过一个，其他仍然生效
     */
    private fun writeAntiDebugCheckpoints(zipOut: ZipOutputStream) {
        val checkpoints = ByteArrayOutputStream()
        
        // 检查点头
        checkpoints.write(byteArrayOf(0x43, 0x4B, 0x50, 0x54)) // "CKPT"
        
        // 检查点数量
        val pointCount = 8
        checkpoints.write(byteArrayOf(pointCount.toByte()))
        
        // 各检查点配置：
        // - 类型（ptrace/timing/signal/thread）
        // - 触发时机（onCreate/onResume/delay/random）
        // - 检测方法（native/java/mixed）
        val checkPointTypes = byteArrayOf(
            0x01, 0x01, 0x01,  // ptrace, onCreate, native
            0x02, 0x02, 0x02,  // timing, onResume, java
            0x03, 0x03, 0x03,  // signal, delay(3s), native
            0x04, 0x04, 0x01,  // thread, random, native
            0x01, 0x04, 0x03,  // ptrace, random, mixed
            0x02, 0x01, 0x02,  // timing, onCreate, java
            0x03, 0x02, 0x03,  // signal, onResume, native
            0x04, 0x03, 0x03   // thread, delay(5s), mixed
        )
        checkpoints.write(checkPointTypes)
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/debug_checkpoints.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(checkpoints.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 写入 Frida 检测特征库
     * 包含最新版 Frida 的各种特征签名
     */
    private fun writeFridaSignatures(zipOut: ZipOutputStream) {
        val sigs = ByteArrayOutputStream()
        
        // 头部
        sigs.write(byteArrayOf(0x46, 0x52, 0x49, 0x44)) // "FRID"
        
        // 端口特征
        val fridaPorts = intArrayOf(27042, 27043, 27044, 27045, 4444)
        sigs.write(fridaPorts.size.toByte().toInt())
        fridaPorts.forEach { port ->
            sigs.write(byteArrayOf((port shr 8).toByte(), port.toByte()))
        }
        
        // 内存特征字符串
        val memoryPatterns = listOf(
            "LIBFRIDA", "frida-agent", "frida-gadget",
            "gum-js-loop", "gmain", "linjector",
            "frida_agent_main", "frida_server",
            "re.frida.server", "frida-helper"
        )
        sigs.write(memoryPatterns.size.toByte().toInt())
        memoryPatterns.forEach { pattern ->
            val bytes = pattern.toByteArray()
            sigs.write(bytes.size.toByte().toInt())
            sigs.write(bytes)
        }
        
        // 线程名特征
        val threadPatterns = listOf(
            "gum-js-loop", "gmain", "gdbus",
            "frida", "agent", "linjector"
        )
        sigs.write(threadPatterns.size.toByte().toInt())
        threadPatterns.forEach { pattern ->
            val bytes = pattern.toByteArray()
            sigs.write(bytes.size.toByte().toInt())
            sigs.write(bytes)
        }
        
        // 文件路径特征
        val filePaths = listOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/data/local/tmp/frida-agent.so",
            "/data/local/tmp/frida-agent-32.so",
            "/data/local/tmp/frida-agent-64.so"
        )
        sigs.write(filePaths.size.toByte().toInt())
        filePaths.forEach { path ->
            val bytes = path.toByteArray()
            sigs.write(bytes.size.toByte().toInt())
            sigs.write(bytes)
        }
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/frida_sigs.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(sigs.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 写入 Xposed 检测配置
     */
    private fun writeXposedDetectionConfig(zipOut: ZipOutputStream) {
        val config = ByteArrayOutputStream()
        
        // 头部
        config.write(byteArrayOf(0x58, 0x50, 0x4F, 0x53)) // "XPOS"
        
        // 检测类名列表
        val classNames = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XposedHelpers",
            "de.robv.android.xposed.XC_MethodHook",
            "de.robv.android.xposed.XC_MethodReplacement",
            "de.robv.android.xposed.callbacks.XC_LoadPackage",
            "org.lsposed.lspd.core.Main",
            "io.github.libxposed.api.XposedModule",
            "com.elderdrivers.riru.edxp.core.Main"
        )
        config.write(classNames.size.toByte().toInt())
        classNames.forEach { className ->
            val bytes = className.toByteArray()
            config.write(byteArrayOf((bytes.size shr 8).toByte(), bytes.size.toByte()))
            config.write(bytes)
        }
        
        // 文件路径检测列表
        val paths = listOf(
            "/system/framework/XposedBridge.jar",
            "/system/bin/app_process.orig",
            "/data/adb/lspd",
            "/data/adb/modules/zygisk_lsposed",
            "/data/adb/modules/riru_lsposed",
            "/data/adb/modules/edxposed",
            "/data/data/org.lsposed.manager",
            "/data/data/de.robv.android.xposed.installer"
        )
        config.write(paths.size.toByte().toInt())
        paths.forEach { path ->
            val bytes = path.toByteArray()
            config.write(byteArrayOf((bytes.size shr 8).toByte(), bytes.size.toByte()))
            config.write(bytes)
        }
        
        // ART Hook 检测方法标识
        config.write(byteArrayOf(0x01)) // 启用 ART method entry point 检查
        config.write(byteArrayOf(0x01)) // 启用堆栈帧分析
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/xposed_detect.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config.toByteArray())
        zipOut.closeEntry()
    }
}
