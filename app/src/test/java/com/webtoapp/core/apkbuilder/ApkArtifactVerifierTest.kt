package com.webtoapp.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApkArtifactVerifierTest {

    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `web app requires app config entry`() {
        val apk = createApk("assets/other.txt")

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "Web",
                    packageName = "com.example.web",
                    targetUrl = "https://example.com",
                    appType = "WEB"
                ),
                encryptionEnabled = false
            )
        )

        assertThat(result.passed).isFalse()
        assertThat(result.issues.single().key).isEqualTo("config")
    }

    @Test
    fun `image app passes when media content is embedded`() {
        val apk = createApk(
            ApkTemplate.CONFIG_PATH,
            "assets/media_content.png"
        )

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "Image",
                    packageName = "com.example.image",
                    targetUrl = "",
                    appType = "IMAGE"
                ),
                encryptionEnabled = false
            )
        )

        assertThat(result.passed).isTrue()
        assertThat(result.checkedEntryCount).isEqualTo(2)
    }

    @Test
    fun `encrypted html app requires encrypted config and encrypted entry file`() {
        val index = temp.newFile("index.html").apply {
            writeText("<html></html>")
        }
        val apk = createApk(
            ApkTemplate.CONFIG_PATH,
            "${ApkTemplate.CONFIG_PATH}.enc",
            "assets/html/index.html.enc"
        )

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "HTML",
                    packageName = "com.example.html",
                    targetUrl = "",
                    appType = "HTML",
                    htmlEntryFile = "index.html"
                ),
                encryptionEnabled = true,
                htmlFiles = listOf(HtmlFile("index.html", index.absolutePath, HtmlFileType.HTML))
            )
        )

        assertThat(result.passed).isTrue()
        assertThat(result.checkedEntryCount).isEqualTo(3)
    }

    @Test
    fun `gallery app reports missing embedded item`() {
        val media = temp.newFile("photo.png").apply {
            writeText("png")
        }
        val apk = createApk(ApkTemplate.CONFIG_PATH)

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "Gallery",
                    packageName = "com.example.gallery",
                    targetUrl = "",
                    appType = "GALLERY"
                ),
                encryptionEnabled = false,
                galleryItems = listOf(
                    GalleryItem(
                        path = media.absolutePath,
                        type = GalleryItemType.IMAGE,
                        name = "Photo"
                    )
                )
            )
        )

        assertThat(result.passed).isFalse()
        assertThat(result.issues.map { it.key }).contains("galleryItems[0]")
    }

    @Test
    fun `node project verification respects excluded directories`() {
        val projectDir = temp.newFolder("node-project")
        File(projectDir, "server.js").writeText("console.log('ready')")
        File(projectDir, ".cache").mkdir()
        File(projectDir, ".cache/ignored.js").writeText("ignored")
        val apk = createApk(
            ApkTemplate.CONFIG_PATH,
            "assets/nodejs_app/server.js"
        )

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "Node",
                    packageName = "com.example.node",
                    targetUrl = "",
                    appType = "NODEJS_APP"
                ),
                encryptionEnabled = false,
                nodejsProjectDir = projectDir
            )
        )

        assertThat(result.passed).isTrue()
        assertThat(result.checkedEntryCount).isEqualTo(2)
    }

    @Test
    fun `frontend app with saved html files passes without project directory`() {
        val index = temp.newFile("index.html").apply {
            writeText("<html></html>")
        }
        val apk = createApk(
            ApkTemplate.CONFIG_PATH,
            "assets/html/index.html"
        )

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "Frontend",
                    packageName = "com.example.frontend",
                    targetUrl = "",
                    appType = "FRONTEND",
                    htmlEntryFile = "index.html"
                ),
                encryptionEnabled = false,
                htmlFiles = listOf(HtmlFile("index.html", index.absolutePath, HtmlFileType.HTML))
            )
        )

        assertThat(result.passed).isTrue()
        assertThat(result.checkedEntryCount).isEqualTo(2)
    }

    @Test
    fun `wordpress verification allows known empty core placeholder files`() {
        val projectDir = temp.newFolder("wordpress-project")
        File(projectDir, "index.php").writeText("<?php echo 'ok';")
        File(projectDir, "wp-includes/js/swfobject.js").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, "wp-includes/js/swfupload/handlers.js").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, "wp-includes/js/swfupload/license.txt").writeText("")
        File(projectDir, "wp-includes/js/swfupload/swfupload.js").writeText("")
        File(projectDir, "wp-includes/js/swfupload/handlers.min.js").writeText("")

        val apk = createApkWithContent(
            ApkTemplate.CONFIG_PATH to "config".toByteArray(),
            "assets/wordpress/index.php" to "data:index.php".toByteArray(),
            "assets/wordpress/wp-includes/js/swfobject.js" to ByteArray(0),
            "assets/wordpress/wp-includes/js/swfupload/handlers.js" to ByteArray(0),
            "assets/wordpress/wp-includes/js/swfupload/license.txt" to ByteArray(0),
            "assets/wordpress/wp-includes/js/swfupload/swfupload.js" to ByteArray(0),
            "assets/wordpress/wp-includes/js/swfupload/handlers.min.js" to ByteArray(0)
        )

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "WordPress",
                    packageName = "com.example.wordpress",
                    targetUrl = "",
                    appType = "WORDPRESS"
                ),
                encryptionEnabled = false,
                wordPressProjectDir = projectDir
            )
        )

        assertThat(result.passed).isTrue()
    }

    private fun createApk(vararg entries: String): File {
        val apk = temp.newFile("artifact-${System.nanoTime()}.apk")
        ZipOutputStream(apk.outputStream()).use { zipOut ->
            entries.forEach { name ->
                zipOut.putNextEntry(ZipEntry(name))
                zipOut.write("data:$name".toByteArray())
                zipOut.closeEntry()
            }
        }
        return apk
    }

    private fun createApkWithContent(vararg entries: Pair<String, ByteArray>): File {
        val apk = temp.newFile("artifact-${System.nanoTime()}.apk")
        ZipOutputStream(apk.outputStream()).use { zipOut ->
            entries.forEach { (name, content) ->
                zipOut.putNextEntry(ZipEntry(name))
                zipOut.write(content)
                zipOut.closeEntry()
            }
        }
        return apk
    }
}
