package com.webtoapp.ui.viewmodel

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.Announcement
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.BgmConfig
import com.webtoapp.data.model.BgmItem
import com.webtoapp.data.model.GalleryConfig
import com.webtoapp.data.model.GalleryItem
import com.webtoapp.data.model.GalleryItemType
import com.webtoapp.data.model.HtmlConfig
import com.webtoapp.data.model.SplashConfig
import com.webtoapp.data.model.SplashType
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.WebViewConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EditStateMapperTest {

    @Test
    fun `web app maps to edit state and back through draft builder`() {
        val webApp = WebApp(
            name = "Sample",
            url = "https://example.com",
            iconPath = "file:///icon.png",
            appType = AppType.WEB,
            htmlConfig = HtmlConfig(entryFile = "index.html"),
            splashConfig = SplashConfig(type = SplashType.IMAGE, mediaPath = "file:///splash.png"),
            bgmConfig = BgmConfig(
                playlist = listOf(BgmItem(name = "song", path = "/music.mp3"))
            ),
            announcement = Announcement(title = "Notice", content = "hello"),
            webViewConfig = WebViewConfig(hideToolbar = true),
            extensionEnabled = true,
            extensionModuleIds = listOf("mod-1"),
        )

        val editState = webApp.toEditState()

        assertThat(editState.name).isEqualTo("Sample")
        assertThat(editState.iconUri).isEqualTo(Uri.parse("file:///icon.png"))
        assertThat(editState.splashMediaUri).isEqualTo(Uri.parse("file:///splash.png"))
        assertThat(editState.extensionModuleIds).containsExactly("mod-1")

        val payload = editState.toDraftPayload(
            normalizedUrl = "https://example.com",
            iconPath = "file:///icon.png",
            extensionModuleIds = editState.extensionModuleIds,
            currentThemeType = "AURORA",
            externalizedWebViewConfig = editState.webViewConfig,
        )

        val rebuilt = webApp.applyDraft(editState, payload)

        assertThat(rebuilt.name).isEqualTo("Sample")
        assertThat(rebuilt.url).isEqualTo("https://example.com")
        assertThat(rebuilt.extensionModuleIds).containsExactly("mod-1")
    }

    @Test
    fun `blank activation dialog is normalized away in draft`() {
        val state = EditState(
            name = "Draft",
            url = "https://example.com",
        )

        val payload = state.toDraftPayload(
            normalizedUrl = "https://example.com",
            iconPath = null,
            extensionModuleIds = emptySet(),
            currentThemeType = "AURORA",
            externalizedWebViewConfig = state.webViewConfig,
        )

        val rebuilt = null.applyDraft(state, payload)

        assertThat(rebuilt.activationDialogConfig).isNull()
    }
}
