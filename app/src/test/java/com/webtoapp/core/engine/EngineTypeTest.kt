package com.webtoapp.core.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EngineTypeTest {

    @Test
    fun `fromString resolves known engines and falls back to system webview`() {
        assertThat(EngineType.fromString("GECKOVIEW")).isEqualTo(EngineType.GECKOVIEW)
        assertThat(EngineType.fromString("system_webview")).isEqualTo(EngineType.SYSTEM_WEBVIEW)
        assertThat(EngineType.fromString("unknown")).isEqualTo(EngineType.SYSTEM_WEBVIEW)
    }

    @Test
    fun `engine metadata reflects download requirement`() {
        assertThat(EngineType.SYSTEM_WEBVIEW.requiresDownload).isFalse()
        assertThat(EngineType.SYSTEM_WEBVIEW.estimatedSizeMb).isEqualTo(0)

        assertThat(EngineType.GECKOVIEW.requiresDownload).isTrue()
        assertThat(EngineType.GECKOVIEW.estimatedSizeMb).isGreaterThan(0)
    }
}
