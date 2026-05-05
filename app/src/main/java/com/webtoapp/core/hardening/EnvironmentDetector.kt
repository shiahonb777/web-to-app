package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream




































class EnvironmentDetector(private val context: Context) {

    companion object {
        private const val TAG = "EnvironmentDetector"
    }




    data class EnvironmentConfig(
        val advancedEmulatorDetection: Boolean = false,
        val virtualAppDetection: Boolean = true,
        val usbDebuggingDetection: Boolean = false,
        val vpnDetection: Boolean = false,
        val developerOptionsDetection: Boolean = false
    )




    fun writeEnvironmentConfig(zipOut: ZipOutputStream, config: EnvironmentConfig) {
        AppLogger.e(TAG, "写入环境检测配置")

        val configData = generateEnvironmentConfigData(config)

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/env_detect.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(configData)
        zipOut.closeEntry()


        if (config.advancedEmulatorDetection) {
            writeEmulatorFingerprints(zipOut)
        }


        if (config.virtualAppDetection) {
            writeVirtualAppSignatures(zipOut)
        }

        AppLogger.e(TAG, "环境检测配置写入完成")
    }




    private fun generateEnvironmentConfigData(config: EnvironmentConfig): ByteArray {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x45, 0x4E, 0x56, 0x44))


        data.write(byteArrayOf(0x00, 0x01))


        var bitmap = 0
        if (config.advancedEmulatorDetection) bitmap = bitmap or 0x01
        if (config.virtualAppDetection) bitmap = bitmap or 0x02
        if (config.usbDebuggingDetection) bitmap = bitmap or 0x04
        if (config.vpnDetection) bitmap = bitmap or 0x08
        if (config.developerOptionsDetection) bitmap = bitmap or 0x10
        data.write(byteArrayOf(bitmap.toByte()))


        if (config.advancedEmulatorDetection) {

            data.write(byteArrayOf(0x06))

            data.write(byteArrayOf(
                0x14,
                0x19,
                0x0F,
                0x0A,
                0x14,
                0x0A
            ))

            data.write(byteArrayOf(0x3C))


            data.write(byteArrayOf(
                0x00, 0x0A,
                0x00, 0x64
            ))
        }


        if (config.vpnDetection) {

            val vpnInterfaces = listOf("tun0", "ppp0", "pptp0", "l2tp0", "ipsec0")
            data.write(vpnInterfaces.size.toByte().toInt())
            vpnInterfaces.forEach { iface ->
                val bytes = iface.toByteArray()
                data.write(bytes.size.toByte().toInt())
                data.write(bytes)
            }
        }


        val padding = ByteArray(16)
        SecureRandom().nextBytes(padding)
        data.write(padding)

        return data.toByteArray()
    }




    private fun writeEmulatorFingerprints(zipOut: ZipOutputStream) {
        val fps = ByteArrayOutputStream()


        fps.write(byteArrayOf(0x45, 0x4D, 0x46, 0x50))


        val buildFingerprints = listOf(

            "generic", "unknown", "sdk_gphone", "vbox86",

            "google_sdk", "Emulator", "Android SDK built for x86",
            "Droid4X", "TiantianVM", "Andy",

            "Genymotion", "unknown",

            "goldfish", "ranchu", "vbox86",

            "google_sdk", "sdk_x86", "vbox86p", "nox"
        )
        fps.write(byteArrayOf((buildFingerprints.size shr 8).toByte(), buildFingerprints.size.toByte()))
        buildFingerprints.forEach { fp ->
            val bytes = fp.toByteArray()
            fps.write(bytes.size.toByte().toInt())
            fps.write(bytes)
        }


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




    private fun writeVirtualAppSignatures(zipOut: ZipOutputStream) {
        val sigs = ByteArrayOutputStream()


        sigs.write(byteArrayOf(0x56, 0x41, 0x50, 0x50))


        val virtualApps = listOf(

            "io.va.exposed", "com.lbe.parallel.intl",

            "me.weishu.exp", "me.weishu.freeform",

            "com.lbe.parallel.intl", "com.parallel.space.lite",

            "com.ludashi.dualspace", "com.excelliance.dualaid",

            "com.vmos.pro", "com.vmos.lite",

            "com.x8bit.biern",

            "com.applisto.appcloner", "com.applisto.appcloner.cloned",

            "com.bly.dkplat", "info.cloneapp.mochat.in.goast",
            "com.oasisfeng.island"
        )
        sigs.write(byteArrayOf((virtualApps.size shr 8).toByte(), virtualApps.size.toByte()))
        virtualApps.forEach { pkg ->
            val bytes = pkg.toByteArray()
            sigs.write(byteArrayOf((bytes.size shr 8).toByte(), bytes.size.toByte()))
            sigs.write(bytes)
        }


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



        sigs.write(byteArrayOf(0x01))
        sigs.write(byteArrayOf(0x04))

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/vapp_sigs.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(sigs.toByteArray())
        zipOut.closeEntry()
    }
}
