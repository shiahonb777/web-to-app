package com.webtoapp.core.apkbuilder

import com.webtoapp.data.model.NetworkTrustConfig
import com.webtoapp.util.NetworkTrustStorage
import java.io.File

object NetworkSecurityConfigBuilder {
    fun build(config: NetworkTrustConfig): String {
        val anchors = buildAnchors(config)
        val cleartext = config.cleartextTrafficPermitted.toString()
        return """
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="$cleartext">
$anchors
    </base-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="false">localhost</domain>
        <domain includeSubdomains="false">127.0.0.1</domain>
        <domain includeSubdomains="false">10.0.2.2</domain>
$anchors
    </domain-config>
</network-security-config>
        """.trimIndent()
    }

    fun customRawEntries(config: NetworkTrustConfig): List<CustomCaRawEntry> =
        config.customCaCertificates.mapIndexedNotNull { index, certificate ->
            val file = File(certificate.filePath)
            if (!file.isFile || !file.canRead()) return@mapIndexedNotNull null
            CustomCaRawEntry(
                resourceName = NetworkTrustStorage.rawResourceName(index),
                sourceFile = file
            )
        }

    private fun buildAnchors(config: NetworkTrustConfig): String {
        val certs = mutableListOf<String>()
        if (config.trustSystemCa) certs += """            <certificates src="system" />"""
        if (config.trustUserCa) certs += """            <certificates src="user" />"""
        config.customCaCertificates.forEachIndexed { index, _ ->
            certs += """            <certificates src="@raw/${NetworkTrustStorage.rawResourceName(index)}" />"""
        }
        val body = if (certs.isEmpty()) {
            """            <certificates src="system" />"""
        } else {
            certs.joinToString("\n")
        }
        return """
        <trust-anchors>
$body
        </trust-anchors>
        """.trimIndent()
    }
}

data class CustomCaRawEntry(
    val resourceName: String,
    val sourceFile: File
)
