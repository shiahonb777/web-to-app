package com.webtoapp.core.engine.shields

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShieldScriptGeneratorsTest {

    @Test
    fun `gpc injector script and headers include expected privacy fields`() {
        val injector = GpcInjector()
        val script = injector.generateScript()
        val headers = injector.getHeaders()

        assertThat(script).contains("navigator.globalPrivacyControl")
        assertThat(script).contains("doNotTrack")
        assertThat(headers["Sec-GPC"]).isEqualTo("1")
        assertThat(headers["DNT"]).isEqualTo("1")
    }

    @Test
    fun `cookie consent blocker script contains observer and dismiss patterns`() {
        val blocker = CookieConsentBlocker()
        val script = blocker.generateScript()

        assertThat(script).contains("MutationObserver")
        assertThat(script).contains("tryClickButtons")
        assertThat(script).contains("rejectPatterns")
        assertThat(script).contains("acceptPatterns")
    }

    @Test
    fun `shields config presets expose expected defaults`() {
        assertThat(ShieldsConfig.DEFAULT.enabled).isTrue()
        assertThat(ShieldsConfig.DEFAULT.trackerBlocking).isTrue()

        assertThat(ShieldsConfig.DISABLED.enabled).isFalse()

        assertThat(ShieldsConfig.MAXIMUM.enabled).isTrue()
        assertThat(ShieldsConfig.MAXIMUM.thirdPartyCookiePolicy)
            .isEqualTo(ThirdPartyCookiePolicy.BLOCK_ALL_THIRD_PARTY)
        assertThat(ShieldsConfig.MAXIMUM.referrerPolicy)
            .isEqualTo(ShieldsReferrerPolicy.NO_REFERRER)
    }
}
