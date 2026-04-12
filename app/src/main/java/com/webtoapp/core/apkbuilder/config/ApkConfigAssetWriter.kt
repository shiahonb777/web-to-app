package com.webtoapp.core.apkbuilder

import com.webtoapp.core.crypto.AssetEncryptor
import com.webtoapp.core.crypto.EncryptionConfig
import com.webtoapp.core.logging.AppLogger
import java.util.zip.ZipOutputStream

internal object ApkConfigAssetWriter {

    fun writeConfigEntry(
        zipOut: ZipOutputStream,
        template: ApkTemplate,
        config: ApkConfig,
        encryptor: AssetEncryptor? = null,
        encryptionConfig: EncryptionConfig = EncryptionConfig.DISABLED
    ) {
        val configJson = template.createConfigJson(config)
        AppLogger.d("ApkBuilder", "Writing config file: splashEnabled=${config.splashEnabled}, splashType=${config.splashType}")
        AppLogger.d("ApkBuilder", "Config userAgentMode=${config.userAgentMode}, customUserAgent=${config.customUserAgent}")
        AppLogger.d("ApkBuilder", "Config JSON content: $configJson")

        if (encryptionConfig.encryptConfig && encryptor != null) {
            val encryptedData = encryptor.encryptJson(configJson, "app_config.json")
            ZipUtils.writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH + ".enc", encryptedData)

            val data = configJson.toByteArray(Charsets.UTF_8)
            ZipUtils.writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH, data)
            AppLogger.d("ApkBuilder", "Config file encrypted (with plaintext fallback)")
            return
        }

        val data = configJson.toByteArray(Charsets.UTF_8)
        ZipUtils.writeEntryDeflated(zipOut, ApkTemplate.CONFIG_PATH, data)
    }
}
