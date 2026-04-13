package com.webtoapp.core.apkbuilder

import android.content.Context
import com.webtoapp.core.crypto.AssetEncryptor
import com.webtoapp.core.crypto.EncryptionConfig
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.LrcData
import java.io.File
import java.util.zip.ZipOutputStream

internal class BgmAssetWriter(private val context: Context) {

    fun addToAssets(
        zipOut: ZipOutputStream,
        bgmPaths: List<String>,
        lrcDataList: List<LrcData?>,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        AppLogger.d("ApkBuilder", "Preparing to embed ${bgmPaths.size} BGM files, encrypt=${encryptionConfig.encryptBgm}")

        bgmPaths.forEachIndexed { index, bgmPath ->
            try {
                val assetName = "bgm/bgm_$index.mp3"
                val bgmBytes = loadBgmBytes(bgmPath) ?: return@forEachIndexed

                if (encryptionConfig.encryptBgm && encryptor != null) {
                    val encryptedData = encryptor.encrypt(bgmBytes, assetName)
                    ZipUtils.writeEntryDeflated(zipOut, "assets/${assetName}.enc", encryptedData)
                    AppLogger.d("ApkBuilder", "BGM encrypted and embedded: assets/${assetName}.enc (${encryptedData.size} bytes)")
                } else {
                    ZipUtils.writeEntryStoredSimple(zipOut, "assets/$assetName", bgmBytes)
                    AppLogger.d("ApkBuilder", "BGM embedded(STORED): assets/$assetName (${bgmBytes.size} bytes)")
                }

                val lrcData = lrcDataList.getOrNull(index)
                if (lrcData != null && lrcData.lines.isNotEmpty()) {
                    val lrcAssetName = "bgm/bgm_$index.lrc"
                    val lrcBytes = convertLrcDataToLrcString(lrcData).toByteArray(Charsets.UTF_8)

                    if (encryptionConfig.encryptBgm && encryptor != null) {
                        val encryptedLrc = encryptor.encrypt(lrcBytes, lrcAssetName)
                        ZipUtils.writeEntryDeflated(zipOut, "assets/${lrcAssetName}.enc", encryptedLrc)
                        AppLogger.d("ApkBuilder", "LRC encrypted and embedded: assets/${lrcAssetName}.enc")
                    } else {
                        ZipUtils.writeEntryDeflated(zipOut, "assets/$lrcAssetName", lrcBytes)
                        AppLogger.d("ApkBuilder", "LRC embedded: assets/$lrcAssetName")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e("ApkBuilder", "Failed to embed BGM: $bgmPath", e)
            }
        }
    }

    private fun loadBgmBytes(bgmPath: String): ByteArray? {
        val bgmFile = File(bgmPath)
        if (!bgmFile.exists()) {
            if (bgmPath.startsWith("asset:///")) {
                val assetPath = bgmPath.removePrefix("asset:///")
                return context.assets.open(assetPath).use { it.readBytes() }
            }
            AppLogger.e("ApkBuilder", "BGM file does not exist: $bgmPath")
            return null
        }

        if (!bgmFile.canRead()) {
            AppLogger.e("ApkBuilder", "BGM file cannot be read: $bgmPath")
            return null
        }

        val bytes = bgmFile.readBytes()
        if (bytes.isEmpty()) {
            AppLogger.e("ApkBuilder", "BGM file is empty: $bgmPath")
            return null
        }
        return bytes
    }

    private fun convertLrcDataToLrcString(lrcData: LrcData): String {
        val sb = StringBuilder()
        lrcData.title?.let { sb.appendLine("[ti:$it]") }
        lrcData.artist?.let { sb.appendLine("[ar:$it]") }
        lrcData.album?.let { sb.appendLine("[al:$it]") }
        sb.appendLine()

        lrcData.lines.forEach { line ->
            val minutes = line.startTime / 60000
            val seconds = (line.startTime % 60000) / 1000
            val centiseconds = (line.startTime % 1000) / 10
            sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, line.text))
            line.translation?.let { translation ->
                sb.appendLine("[%02d:%02d.%02d]%s".format(minutes, seconds, centiseconds, translation))
            }
        }
        return sb.toString()
    }
}
