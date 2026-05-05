package com.webtoapp.core.hardening

import android.content.Context
import com.webtoapp.core.logging.AppLogger
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream




































class AntiReverseEngine(private val context: Context) {

    companion object {
        private const val TAG = "AntiReverseEngine"
    }




    data class AntiReverseConfig(
        val multiLayerAntiDebug: Boolean = true,
        val advancedFridaDetection: Boolean = true,
        val deepXposedDetection: Boolean = true,
        val magiskDetection: Boolean = false,
        val antiMemoryDump: Boolean = false,
        val antiScreenCapture: Boolean = false
    )




    fun writeAntiReverseConfig(zipOut: ZipOutputStream, config: AntiReverseConfig) {
        AppLogger.d(TAG, "写入反逆向配置")

        val configData = generateAntiReverseConfigData(config)

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/anti_reverse.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(configData)
        zipOut.closeEntry()


        if (config.multiLayerAntiDebug) {
            writeAntiDebugCheckpoints(zipOut)
        }


        if (config.advancedFridaDetection) {
            writeFridaSignatures(zipOut)
        }


        if (config.deepXposedDetection) {
            writeXposedDetectionConfig(zipOut)
        }

        AppLogger.d(TAG, "反逆向配置写入完成")
    }




    private fun generateAntiReverseConfigData(config: AntiReverseConfig): ByteArray {
        val data = ByteArrayOutputStream()


        data.write(byteArrayOf(0x41, 0x52, 0x45, 0x56))


        data.write(byteArrayOf(0x00, 0x01))


        var bitmap = 0
        if (config.multiLayerAntiDebug) bitmap = bitmap or 0x01
        if (config.advancedFridaDetection) bitmap = bitmap or 0x02
        if (config.deepXposedDetection) bitmap = bitmap or 0x04
        if (config.magiskDetection) bitmap = bitmap or 0x08
        if (config.antiMemoryDump) bitmap = bitmap or 0x10
        if (config.antiScreenCapture) bitmap = bitmap or 0x20
        data.write(byteArrayOf(bitmap.toByte()))


        data.write(byteArrayOf(0x00, 0x00, 0x0B, 0xB8.toByte()))


        data.write(byteArrayOf(0x00, 0x00, 0x03, 0xE8.toByte()))


        if (config.multiLayerAntiDebug) {

            data.write(byteArrayOf(0x04))

            data.write(byteArrayOf(
                0x01,
                0x02,
                0x03,
                0x04
            ))

            data.write(byteArrayOf(0x00, 0x00, 0x27, 0x10))
        }


        val padding = ByteArray(24)
        SecureRandom().nextBytes(padding)
        data.write(padding)

        return data.toByteArray()
    }





    private fun writeAntiDebugCheckpoints(zipOut: ZipOutputStream) {
        val checkpoints = ByteArrayOutputStream()


        checkpoints.write(byteArrayOf(0x43, 0x4B, 0x50, 0x54))


        val pointCount = 8
        checkpoints.write(byteArrayOf(pointCount.toByte()))





        val checkPointTypes = byteArrayOf(
            0x01, 0x01, 0x01,
            0x02, 0x02, 0x02,
            0x03, 0x03, 0x03,
            0x04, 0x04, 0x01,
            0x01, 0x04, 0x03,
            0x02, 0x01, 0x02,
            0x03, 0x02, 0x03,
            0x04, 0x03, 0x03
        )
        checkpoints.write(checkPointTypes)

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/debug_checkpoints.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(checkpoints.toByteArray())
        zipOut.closeEntry()
    }





    private fun writeFridaSignatures(zipOut: ZipOutputStream) {
        val sigs = ByteArrayOutputStream()


        sigs.write(byteArrayOf(0x46, 0x52, 0x49, 0x44))


        val fridaPorts = intArrayOf(27042, 27043, 27044, 27045, 4444)
        sigs.write(fridaPorts.size.toByte().toInt())
        fridaPorts.forEach { port ->
            sigs.write(byteArrayOf((port shr 8).toByte(), port.toByte()))
        }


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




    private fun writeXposedDetectionConfig(zipOut: ZipOutputStream) {
        val config = ByteArrayOutputStream()


        config.write(byteArrayOf(0x58, 0x50, 0x4F, 0x53))


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


        config.write(byteArrayOf(0x01))
        config.write(byteArrayOf(0x01))

        val entry = ZipEntry("assets/${AppHardeningEngine.HARDENING_ASSETS_DIR}/xposed_detect.dat")
        zipOut.putNextEntry(entry)
        zipOut.write(config.toByteArray())
        zipOut.closeEntry()
    }
}
