package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 高级环境检测器
 * 
 * 超越传统加固的环境检测能力：
 * 
 * 1. 高级模拟器检测（硬件级）：
 *    - CPU 指令集特征（ARM vs x86 真实性验证）
 *    - 传感器数据验证（加速度计/陀螺仪/磁力计持续数据流分析）
 *    - 电池温度/电压异常检测（模拟器通常返回固定值）
 *    - GPU 渲染能力检测（OpenGL ES 特征）
 *    - 蓝牙/WiFi 硬件存在性验证
 *    - IMEI/IMSI 格式和校验位验证
 * 
 * 2. 虚拟化环境检测：
 *    - VirtualXposed / 太极 检测
 *    - Parallel Space / Dual Space 检测
 *    - VMOS / x8sandbox 检测
 *    - 私有目录路径异常检测（多开环境特征）
 *    - uid/gid 一致性检查
 * 
 * 3. USB 调试检测：
 *    - Settings.Global.ADB_ENABLED
 *    - USB 连接状态
 *    - ADB over WiFi（5555 端口）
 * 
 * 4. VPN/代理检测：
 *    - NetworkInterface 遍历（tun0/ppp0）
 *    - ConnectivityManager VPN 网络检测
 *    - 系统代理设置检测
 *    - HTTP_PROXY 环境变量检测
 * 
 * 5. 开发者选项检测：
 *    - Settings.Global.DEVELOPMENT_SETTINGS_ENABLED
 *    - 布局边界显示/GPU 渲染等开发者选项
 */
class EnvironmentDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "EnvironmentDetector"
    }
    
    /**
     * 环境检测配置
     */
    data class EnvironmentConfig(
        val advancedEmulatorDetection: Boolean = false,
        val virtualAppDetection: Boolean = true,
        val usbDebuggingDetection: Boolean = false,
        val vpnDetection: Boolean = false,
        val developerOptionsDetection: Boolean = false
    )
    
    /**
     * 写入环境检测配置到 APK
     */
    fun writeEnvironmentConfig(zipOut: ZipOutputStream, config: EnvironmentConfig) {
        AppLogger.e(TAG, "写入环境检测配置")
        
        val configData = generateEnvironmentConfigData(config)
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/env_detect.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(configData)
        zipOut.closeEntry()
        
        // 写入模拟器硬件特征库
        if (config.advancedEmulatorDetection) {
            writeEmulatorFingerprints(zipOut)
        }
        
        // 写入虚拟化应用特征库
        if (config.virtualAppDetection) {
            writeVirtualAppSignatures(zipOut)
        }
        
        AppLogger.e(TAG, "环境检测配置写入完成")
    }
    
    /**
     * 生成环境检测配置数据
     */
    private fun generateEnvironmentConfigData(config: EnvironmentConfig): ByteArray {
        val data = ByteArrayOutputStream()
        
        // 魔数
        data.write(byteArrayOf(0x45, 0x4E, 0x56, 0x44)) // "ENVD"
        
        // 版本
        data.write(byteArrayOf(0x00, 0x01))
        
        // 配置位图
        var bitmap = 0
        if (config.advancedEmulatorDetection) bitmap = bitmap or 0x01
        if (config.virtualAppDetection) bitmap = bitmap or 0x02
        if (config.usbDebuggingDetection) bitmap = bitmap or 0x04
        if (config.vpnDetection) bitmap = bitmap or 0x08
        if (config.developerOptionsDetection) bitmap = bitmap or 0x10
        data.write(byteArrayOf(bitmap.toByte()))
        
        // 模拟器检测配置
        if (config.advancedEmulatorDetection) {
            // 检测维度
            data.write(byteArrayOf(0x06)) // 6 个维度
            // 各维度权重（评分机制）
            data.write(byteArrayOf(
                0x14, // CPU 指令集: 20 分
                0x19, // 传感器数据: 25 分
                0x0F, // 电池温度: 15 分
                0x0A, // GPU 特征: 10 分
                0x14, // 硬件存在性: 20 分
                0x0A  // IMEI/IMSI: 10 分
            ))
            // 阈值（总分 100，超过阈值判定为模拟器）
            data.write(byteArrayOf(0x3C)) // 60 分
            
            // 传感器采样参数
            data.write(byteArrayOf(
                0x00, 0x0A, // 采样次数: 10
                0x00, 0x64  // 采样间隔: 100ms
            ))
        }
        
        // VPN 检测配置
        if (config.vpnDetection) {
            // VPN 网络接口名特征
            val vpnInterfaces = listOf("tun0", "ppp0", "pptp0", "l2tp0", "ipsec0")
            data.write(vpnInterfaces.size.toByte().toInt())
            vpnInterfaces.forEach { iface ->
                val bytes = iface.toByteArray()
                data.write(bytes.size.toByte().toInt())
                data.write(bytes)
            }
        }
        
        // 随机填充
        val padding = ByteArray(16)
        SecureRandom().nextBytes(padding)
        data.write(padding)
        
        return data.toByteArray()
    }
    
    /**
     * 写入模拟器硬件特征库
     */
    private fun writeEmulatorFingerprints(zipOut: ZipOutputStream) {
        val fps = ByteArrayOutputStream()
        
        // 头部
        fps.write(byteArrayOf(0x45, 0x4D, 0x46, 0x50)) // "EMFP"
        
        // 已知模拟器 Build 特征
        val buildFingerprints = listOf(
            // Build.FINGERPRINT
            "generic", "unknown", "sdk_gphone", "vbox86",
            // Build.MODEL
            "google_sdk", "Emulator", "Android SDK built for x86",
            "Droid4X", "TiantianVM", "Andy",
            // Build.MANUFACTURER
            "Genymotion", "unknown",
            // Build.HARDWARE
            "goldfish", "ranchu", "vbox86",
            // Build.PRODUCT
            "google_sdk", "sdk_x86", "vbox86p", "nox"
        )
        fps.write(byteArrayOf((buildFingerprints.size shr 8).toByte(), buildFingerprints.size.toByte()))
        buildFingerprints.forEach { fp ->
            val bytes = fp.toByteArray()
            fps.write(bytes.size.toByte().toInt())
            fps.write(bytes)
        }
        
        // 已知模拟器特征文件
        val emulatorFiles = listOf(
            "/dev/socket/qemud", "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace", "/system/bin/qemu-props",
            "/dev/goldfish_pipe", "/dev/vboxguest",
            "/dev/vboxuser", "/system/lib/vboxsf.ko",
            "/fstab.nox", "/init.nox.rc",
            "/ueventd.nox.rc", "/fstab.ttVM_x86",
            "/init.ttVM_x86.rc"
        )
        fps.write(byteArrayOf((emulatorFiles.size shr 8).toByte(), emulatorFiles.size.toByte()))
        emulatorFiles.forEach { file ->
            val bytes = file.toByteArray()
            fps.write(bytes.size.toByte().toInt())
            fps.write(bytes)
        }
        
        // 已知模拟器 CPU 特征
        val cpuFeatures = listOf(
            "hypervisor", "vmx", "svm", "hvm"
        )
        fps.write(cpuFeatures.size.toByte().toInt())
        cpuFeatures.forEach { feature ->
            val bytes = feature.toByteArray()
            fps.write(bytes.size.toByte().toInt())
            fps.write(bytes)
        }
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/emu_fps.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(fps.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * 写入虚拟化应用特征库
     */
    private fun writeVirtualAppSignatures(zipOut: ZipOutputStream) {
        val sigs = ByteArrayOutputStream()
        
        // 头部
        sigs.write(byteArrayOf(0x56, 0x41, 0x50, 0x50)) // "VAPP"
        
        // 已知虚拟化应用包名
        val virtualApps = listOf(
            // VirtualXposed
            "io.va.exposed", "com.lbe.parallel.intl",
            // 太极
            "me.weishu.exp", "me.weishu.freeform",
            // Parallel Space
            "com.lbe.parallel.intl", "com.parallel.space.lite",
            // Dual Space
            "com.ludashi.dualspace", "com.excelliance.dualaid",
            // VMOS
            "com.vmos.pro", "com.vmos.lite",
            // x8sandbox
            "com.x8bit.biern",
            // 应用分身
            "com.applisto.appcloner", "com.applisto.appcloner.cloned",
            // 其他
            "com.bly.dkplat", "info.cloneapp.mochat.in.goast",
            "com.oasisfeng.island"
        )
        sigs.write(byteArrayOf((virtualApps.size shr 8).toByte(), virtualApps.size.toByte()))
        virtualApps.forEach { pkg ->
            val bytes = pkg.toByteArray()
            sigs.write(byteArrayOf((bytes.size shr 8).toByte(), bytes.size.toByte()))
            sigs.write(bytes)
        }
        
        // 虚拟化特征路径
        val virtualPaths = listOf(
            "/data/data/io.va.exposed",
            "/data/user/0/io.va.exposed",
            "/data/data/com.lbe.parallel.intl",
            "/storage/emulated/0/parallel_intl"
        )
        sigs.write(virtualPaths.size.toByte().toInt())
        virtualPaths.forEach { path ->
            val bytes = path.toByteArray()
            sigs.write(bytes.size.toByte().toInt())
            sigs.write(bytes)
        }
        
        // 路径异常检测特征
        // 虚拟化环境中 getFilesDir() 返回的路径通常包含多层嵌套
        sigs.write(byteArrayOf(0x01)) // 启用路径深度检查
        sigs.write(byteArrayOf(0x04)) // 最大允许路径深度: 4
        
        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/vapp_sigs.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(sigs.toByteArray())
        zipOut.closeEntry()
    }
}
