package com.webtoapp.util

import android.content.Context
import android.net.Uri
import com.webtoapp.data.model.CustomCaCertificate
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.UUID

object NetworkTrustStorage {
    private const val CERT_DIR = "custom_ca"
    private const val MAX_CERT_BYTES = 256 * 1024

    fun importCertificate(
        context: Context,
        uri: Uri,
        displayNameHint: String? = null
    ): CustomCaCertificate {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalArgumentException("Unable to read certificate")

        require(bytes.isNotEmpty()) { "Certificate is empty" }
        require(bytes.size <= MAX_CERT_BYTES) { "Certificate is too large" }

        val certificate = parseX509(bytes)
        val encoded = certificate.encoded
        val sha256 = encoded.sha256Hex()
        val id = UUID.randomUUID().toString()
        val safeName = sanitizeResourceName(displayNameHint ?: certificate.subjectX500Principal.name)
        val file = certDirectory(context).resolve("${safeName}_${sha256.take(12)}.cer")
        file.writeBytes(encoded)

        return CustomCaCertificate(
            id = id,
            displayName = displayNameHint?.trim()?.takeIf { it.isNotBlank() }
                ?: certificate.subjectX500Principal.name,
            filePath = file.absolutePath,
            sha256 = sha256
        )
    }

    fun validateCertificateFile(path: String): Boolean {
        val file = File(path)
        if (!file.isFile || !file.canRead() || file.length() <= 0L) return false
        return runCatching { parseX509(file.readBytes()) }.isSuccess
    }

    fun rawResourceName(index: Int): String = "wta_custom_ca_${index + 1}"

    fun sanitizeResourceName(value: String): String {
        val normalized = value.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9_]+"), "_")
            .trim('_')
        val base = normalized.ifBlank { "custom_ca" }
        return if (base.first().isLetter()) base.take(32) else "ca_${base.take(29)}"
    }

    fun parseX509(bytes: ByteArray): X509Certificate {
        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(bytes.inputStream()) as X509Certificate
    }

    fun ByteArray.sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun certDirectory(context: Context): File =
        File(context.filesDir, CERT_DIR).apply { mkdirs() }
}
