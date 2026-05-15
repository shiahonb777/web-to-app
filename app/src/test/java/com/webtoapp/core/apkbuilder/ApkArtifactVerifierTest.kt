package com.webtoapp.core.apkbuilder

import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.HtmlFile
import com.webtoapp.data.model.HtmlFileType
import com.webtoapp.data.model.MultiWebSite
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

    @Test
    fun `python verification allows known empty package marker files`() {
        val projectDir = temp.newFolder("python-project")
        File(projectDir, "app.py").writeText("print('ok')")
        File(projectDir, ".pypackages/fastapi/py.typed").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/starlette/py.typed").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/pydantic/py.typed").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/anyio/streams/__init__.py").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/flask/py.typed").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/werkzeug/middleware/__init__.py").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/werkzeug/sansio/__init__.py").apply {
            parentFile?.mkdirs()
            writeText("")
        }

        val apk = createApkWithContent(
            ApkTemplate.CONFIG_PATH to "config".toByteArray(),
            "assets/python_app/app.py" to "print('ok')".toByteArray(),
            "assets/python_app/.pypackages/fastapi/py.typed" to ByteArray(0),
            "assets/python_app/.pypackages/starlette/py.typed" to ByteArray(0),
            "assets/python_app/.pypackages/pydantic/py.typed" to ByteArray(0),
            "assets/python_app/.pypackages/anyio/streams/__init__.py" to ByteArray(0),
            "assets/python_app/.pypackages/flask/py.typed" to ByteArray(0),
            "assets/python_app/.pypackages/werkzeug/middleware/__init__.py" to ByteArray(0),
            "assets/python_app/.pypackages/werkzeug/sansio/__init__.py" to ByteArray(0)
        )

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "Python",
                    packageName = "com.example.python",
                    targetUrl = "",
                    appType = "PYTHON_APP"
                ),
                encryptionEnabled = false,
                pythonAppProjectDir = projectDir
            )
        )

        assertThat(result.passed).isTrue()
    }

    @Test
    fun `python verification allows known empty django package marker files`() {
        val projectDir = temp.newFolder("python-django-project")
        File(projectDir, "manage.py").writeText("print('ok')")
        File(projectDir, "mysite/__init__.py").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/Django-5.0.dist-info/REQUESTED").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/gunicorn-21.2.0.dist-info/REQUESTED").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/asgiref/py.typed").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/packaging/py.typed").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/sqlparse/py.typed").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/django/contrib/__init__.py").apply {
            parentFile?.mkdirs()
            writeText("")
        }
        File(projectDir, ".pypackages/django/conf/app_template/__init__.py-tpl").apply {
            parentFile?.mkdirs()
            writeText("")
        }

        val apk = createApkWithContent(
            ApkTemplate.CONFIG_PATH to "config".toByteArray(),
            "assets/python_app/manage.py" to "print('ok')".toByteArray(),
            "assets/python_app/mysite/__init__.py" to ByteArray(0),
            "assets/python_app/.pypackages/Django-5.0.dist-info/REQUESTED" to ByteArray(0),
            "assets/python_app/.pypackages/gunicorn-21.2.0.dist-info/REQUESTED" to ByteArray(0),
            "assets/python_app/.pypackages/asgiref/py.typed" to ByteArray(0),
            "assets/python_app/.pypackages/packaging/py.typed" to ByteArray(0),
            "assets/python_app/.pypackages/sqlparse/py.typed" to ByteArray(0),
            "assets/python_app/.pypackages/django/contrib/__init__.py" to ByteArray(0),
            "assets/python_app/.pypackages/django/conf/app_template/__init__.py-tpl" to ByteArray(0)
        )

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "Python Django",
                    packageName = "com.example.python.django",
                    targetUrl = "",
                    appType = "PYTHON_APP"
                ),
                encryptionEnabled = false,
                pythonAppProjectDir = projectDir
            )
        )

        assertThat(result.passed).isTrue()
    }

    @Test
    fun `multi web verification requires embedded local site file`() {
        val projectDir = temp.newFolder("multi-web-project")
        File(projectDir, "site-a/index.html").apply {
            parentFile?.mkdirs()
            writeText("<html>ok</html>")
        }
        val apk = createApk(
            ApkTemplate.CONFIG_PATH,
            "assets/html_projects/site-a/index.html"
        )

        val result = ApkArtifactVerifier.verify(
            ApkArtifactVerificationRequest(
                apkFile = apk,
                config = ApkConfig(
                    appName = "Multi Web",
                    packageName = "com.example.multiweb",
                    targetUrl = "",
                    appType = "MULTI_WEB"
                ),
                encryptionEnabled = false,
                multiWebSites = listOf(
                    MultiWebSite(
                        id = "site-a",
                        name = "Local",
                        type = "LOCAL",
                        localFilePath = "site-a/index.html"
                    )
                ),
                multiWebProjectDir = projectDir
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
