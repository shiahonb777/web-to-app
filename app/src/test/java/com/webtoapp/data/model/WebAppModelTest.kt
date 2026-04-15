package com.webtoapp.data.model

import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.activation.ActivationCode
import com.webtoapp.core.activation.ActivationCodeType
import org.junit.Test

class WebAppModelTest {

    @Test
    fun `html config returns safe default entry file when invalid`() {
        assertThat(HtmlConfig(entryFile = "index.html").getValidEntryFile()).isEqualTo("index.html")
        assertThat(HtmlConfig(entryFile = ".html").getValidEntryFile()).isEqualTo("index.html")
        assertThat(HtmlConfig(entryFile = "").getValidEntryFile()).isEqualTo("index.html")
    }

    @Test
    fun `gallery config computes category filter sorting and stats`() {
        val items = listOf(
            GalleryItem(
                id = "1",
                path = "/a.jpg",
                type = GalleryItemType.IMAGE,
                name = "Zoo",
                categoryId = "c1",
                createdAt = 30
            ),
            GalleryItem(
                id = "2",
                path = "/b.mp4",
                type = GalleryItemType.VIDEO,
                name = "Alpha",
                categoryId = "c2",
                createdAt = 10
            ),
            GalleryItem(
                id = "3",
                path = "/c.jpg",
                type = GalleryItemType.IMAGE,
                name = "Beta",
                categoryId = "c1",
                createdAt = 20
            )
        )

        val byName = GalleryConfig(items = items, sortOrder = GallerySortOrder.NAME_ASC)
        val byDateDesc = GalleryConfig(items = items, sortOrder = GallerySortOrder.DATE_DESC)

        assertThat(byName.getItemsByCategory("c1").map { it.id }).containsExactly("1", "3").inOrder()
        assertThat(byName.getSortedItems().map { it.name }).containsExactly("Alpha", "Beta", "Zoo").inOrder()
        assertThat(byDateDesc.getSortedItems().map { it.createdAt }).containsExactly(30L, 20L, 10L).inOrder()
        assertThat(byName.imageCount).isEqualTo(2)
        assertThat(byName.videoCount).isEqualTo(1)
        assertThat(byName.totalCount).isEqualTo(3)
    }

    @Test
    fun `getAllActivationCodes returns only new activation code list`() {
        val webApp = WebApp(
            name = "Demo",
            url = "https://example.com",
            activationCodeList = listOf(
                ActivationCode(code = "NEW", type = ActivationCodeType.PERMANENT)
            ),
            activationCodes = listOf(
                "OLD",
                """{"code":"JSON","type":"USAGE_LIMITED","usageLimit":3}"""
            )
        )

        val allCodes = webApp.getAllActivationCodes()

        assertThat(allCodes.map { it.code }).containsExactly("NEW")
        assertThat(allCodes.first().type).isEqualTo(ActivationCodeType.PERMANENT)
    }

    @Test
    fun `getActivationCodeStrings serializes only new activation code list`() {
        val webApp = WebApp(
            name = "Demo",
            url = "https://example.com",
            activationCodeList = listOf(
                ActivationCode(code = "NEW", type = ActivationCodeType.DEVICE_BOUND)
            ),
            activationCodes = listOf(
                "OLD",
                """{"code":"SHOULD_SKIP"}"""
            )
        )

        val exported = webApp.getActivationCodeStrings()

        assertThat(exported).hasSize(1)
        assertThat(exported.single()).contains("\"code\":\"NEW\"")
        assertThat(exported.single()).doesNotContain("SHOULD_SKIP")
    }

    @Test
    fun `apk architecture parser falls back to universal`() {
        assertThat(ApkArchitecture.fromName("ARM64")).isEqualTo(ApkArchitecture.ARM64)
        assertThat(ApkArchitecture.fromName("UNKNOWN")).isEqualTo(ApkArchitecture.UNIVERSAL)
    }

    @Test
    fun `apk encryption config maps to crypto config correctly`() {
        val config = ApkEncryptionConfig(
            enabled = true,
            encryptConfig = false,
            encryptHtml = true,
            encryptMedia = true,
            encryptSplash = true,
            encryptBgm = false,
            enableIntegrityCheck = false,
            enableAntiDebug = true,
            enableAntiTamper = false,
            encryptionLevel = ApkEncryptionConfig.EncryptionLevel.HIGH
        )

        val mapped = config.toEncryptionConfig()

        assertThat(mapped.enabled).isTrue()
        assertThat(mapped.encryptConfig).isFalse()
        assertThat(mapped.encryptHtml).isTrue()
        assertThat(mapped.encryptMedia).isTrue()
        assertThat(mapped.encryptSplash).isTrue()
        assertThat(mapped.encryptBgm).isFalse()
        assertThat(mapped.enableIntegrityCheck).isFalse()
        assertThat(mapped.enableAntiDebug).isTrue()
        assertThat(mapped.enableAntiTamper).isFalse()
        assertThat(mapped.encryptionLevel).isEqualTo(com.webtoapp.core.crypto.EncryptionLevel.HIGH)
    }
}
