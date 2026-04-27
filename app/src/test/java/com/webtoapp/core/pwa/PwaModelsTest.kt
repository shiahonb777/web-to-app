package com.webtoapp.core.pwa

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PwaModelsTest {

    // ═══════════════════════════════════════════
    // PwaIcon.maxSizePixels
    // ═══════════════════════════════════════════

    @Test
    fun `maxSizePixels returns Int MAX_VALUE for blank sizes`() {
        val icon = PwaIcon(src = "icon.png", sizes = "")
        assertThat(icon.maxSizePixels).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `maxSizePixels returns Int MAX_VALUE for any sizes`() {
        val icon = PwaIcon(src = "icon.png", sizes = "any")
        assertThat(icon.maxSizePixels).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `maxSizePixels parses single size`() {
        val icon = PwaIcon(src = "icon.png", sizes = "192x192")
        assertThat(icon.maxSizePixels).isEqualTo(192)
    }

    @Test
    fun `maxSizePixels parses multiple sizes and returns max`() {
        val icon = PwaIcon(src = "icon.png", sizes = "48x48 96x96 192x192")
        assertThat(icon.maxSizePixels).isEqualTo(192)
    }

    @Test
    fun `maxSizePixels parses non-square sizes`() {
        val icon = PwaIcon(src = "icon.png", sizes = "120x120 512x512")
        assertThat(icon.maxSizePixels).isEqualTo(512)
    }

    @Test
    fun `maxSizePixels returns 0 for unparseable sizes`() {
        val icon = PwaIcon(src = "icon.png", sizes = "invalid")
        assertThat(icon.maxSizePixels).isEqualTo(0)
    }

    @Test
    fun `maxSizePixels handles null sizes`() {
        val icon = PwaIcon(src = "icon.png", sizes = null)
        assertThat(icon.maxSizePixels).isEqualTo(Int.MAX_VALUE)
    }

    // ═══════════════════════════════════════════
    // PwaManifest defaults
    // ═══════════════════════════════════════════

    @Test
    fun `PwaManifest default has empty icons list`() {
        val manifest = PwaManifest()
        assertThat(manifest.icons).isEmpty()
    }

    @Test
    fun `PwaManifest default has null fields`() {
        val manifest = PwaManifest()
        assertThat(manifest.name).isNull()
        assertThat(manifest.shortName).isNull()
        assertThat(manifest.startUrl).isNull()
        assertThat(manifest.themeColor).isNull()
    }

    // ═══════════════════════════════════════════
    // PwaAnalysisResult defaults
    // ═══════════════════════════════════════════

    @Test
    fun `PwaAnalysisResult default is not PWA`() {
        val result = PwaAnalysisResult()
        assertThat(result.isPwa).isFalse()
        assertThat(result.source).isEqualTo(PwaDataSource.NONE)
    }

    @Test
    fun `PwaAnalysisResult with manifest source is PWA`() {
        val result = PwaAnalysisResult(isPwa = true, source = PwaDataSource.MANIFEST)
        assertThat(result.isPwa).isTrue()
        assertThat(result.source).isEqualTo(PwaDataSource.MANIFEST)
    }

    // ═══════════════════════════════════════════
    // PwaDataSource enum
    // ═══════════════════════════════════════════

    @Test
    fun `PwaDataSource has all expected values`() {
        assertThat(PwaDataSource.values()).asList().containsExactly(
            PwaDataSource.MANIFEST,
            PwaDataSource.META_TAGS,
            PwaDataSource.NONE
        )
    }

    // ═══════════════════════════════════════════
    // PwaAnalysisState sealed class
    // ═══════════════════════════════════════════

    @Test
    fun `PwaAnalysisState Idle is data object`() {
        assertThat(PwaAnalysisState.Idle).isInstanceOf(PwaAnalysisState::class.java)
    }

    @Test
    fun `PwaAnalysisState Analyzing is data object`() {
        assertThat(PwaAnalysisState.Analyzing).isInstanceOf(PwaAnalysisState::class.java)
    }

    @Test
    fun `PwaAnalysisState Success wraps result`() {
        val result = PwaAnalysisResult(isPwa = true)
        val state = PwaAnalysisState.Success(result)
        assertThat(state.result.isPwa).isTrue()
    }

    @Test
    fun `PwaAnalysisState Error wraps message`() {
        val state = PwaAnalysisState.Error("Network failed")
        assertThat(state.message).isEqualTo("Network failed")
    }
}
