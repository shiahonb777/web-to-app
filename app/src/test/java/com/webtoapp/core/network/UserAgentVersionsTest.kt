package com.webtoapp.core.network

import com.google.common.truth.Truth.assertThat
import com.webtoapp.data.model.UserAgentMode
import com.webtoapp.data.model.UserAgentVersions
import org.junit.Test

/**
 * Tests for shared UA version constants and `UserAgentMode`.
 */
class UserAgentVersionsTest {

    @Test
    fun `UserAgentVersions constants are not empty`() {
        assertThat(UserAgentVersions.CHROME).isNotEmpty()
        assertThat(UserAgentVersions.FIREFOX).isNotEmpty()
        assertThat(UserAgentVersions.SAFARI).isNotEmpty()
    }

    @Test
    fun `CHROME_MOBILE contains Chrome version from UserAgentVersions`() {
        val ua = UserAgentMode.CHROME_MOBILE.userAgentString!!
        assertThat(ua).contains("Chrome/${UserAgentVersions.CHROME}")
    }

    @Test
    fun `CHROME_DESKTOP contains Chrome version from UserAgentVersions`() {
        val ua = UserAgentMode.CHROME_DESKTOP.userAgentString!!
        assertThat(ua).contains("Chrome/${UserAgentVersions.CHROME}")
    }

    @Test
    fun `FIREFOX_MOBILE contains Firefox version from UserAgentVersions`() {
        val ua = UserAgentMode.FIREFOX_MOBILE.userAgentString!!
        assertThat(ua).contains("Firefox/${UserAgentVersions.FIREFOX}")
    }

    @Test
    fun `FIREFOX_DESKTOP contains Firefox version from UserAgentVersions`() {
        val ua = UserAgentMode.FIREFOX_DESKTOP.userAgentString!!
        assertThat(ua).contains("Firefox/${UserAgentVersions.FIREFOX}")
    }

    @Test
    fun `SAFARI_MOBILE contains Safari version from UserAgentVersions`() {
        val ua = UserAgentMode.SAFARI_MOBILE.userAgentString!!
        assertThat(ua).contains("Version/${UserAgentVersions.SAFARI}")
    }

    @Test
    fun `SAFARI_DESKTOP contains Safari version from UserAgentVersions`() {
        val ua = UserAgentMode.SAFARI_DESKTOP.userAgentString!!
        assertThat(ua).contains("Version/${UserAgentVersions.SAFARI}")
    }

    @Test
    fun `EDGE_MOBILE contains Chrome version from UserAgentVersions`() {
        val ua = UserAgentMode.EDGE_MOBILE.userAgentString!!
        assertThat(ua).contains("Chrome/${UserAgentVersions.CHROME}")
        assertThat(ua).contains("EdgA/${UserAgentVersions.CHROME}")
    }

    @Test
    fun `EDGE_DESKTOP contains Chrome version from UserAgentVersions`() {
        val ua = UserAgentMode.EDGE_DESKTOP.userAgentString!!
        assertThat(ua).contains("Chrome/${UserAgentVersions.CHROME}")
        assertThat(ua).contains("Edg/${UserAgentVersions.CHROME}")
    }

    @Test
    fun `DEFAULT has null userAgentString`() {
        assertThat(UserAgentMode.DEFAULT.userAgentString).isNull()
    }

    @Test
    fun `CUSTOM has null userAgentString`() {
        assertThat(UserAgentMode.CUSTOM.userAgentString).isNull()
    }

    @Test
    fun `all non-null UA strings are valid format`() {
        UserAgentMode.entries
            .filter { it.userAgentString != null }
            .forEach { mode ->
                val ua = mode.userAgentString!!
                assertThat(ua).startsWith("Mozilla/5.0")
                assertThat(ua.length).isGreaterThan(50)
            }
    }
}
