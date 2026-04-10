package com.webtoapp.core.engine.shields

import android.content.Context
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BrowserShieldsTest {

    private lateinit var context: Context
    private lateinit var shields: BrowserShields

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("browser_shields_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        shields = BrowserShields.getInstance(context)
        shields.updateConfig(ShieldsConfig.DEFAULT)
        shields.stats.resetPageStats()
        shields.stats.resetSessionStats()
        shields.httpsUpgrader.clearFailedDomains()
    }

    @Test
    fun `setEnabled toggles main switch`() {
        shields.setEnabled(false)
        assertThat(shields.isEnabled()).isFalse()

        shields.setEnabled(true)
        assertThat(shields.isEnabled()).isTrue()
    }

    @Test
    fun `updateConfig changes individual protections`() {
        shields.updateConfig(
            ShieldsConfig.DEFAULT.copy(
                trackerBlocking = false,
                gpcEnabled = false,
                thirdPartyCookiePolicy = ThirdPartyCookiePolicy.BLOCK_ALL_THIRD_PARTY
            )
        )

        val config = shields.getConfig()

        assertThat(config.trackerBlocking).isFalse()
        assertThat(config.gpcEnabled).isFalse()
        assertThat(config.thirdPartyCookiePolicy).isEqualTo(ThirdPartyCookiePolicy.BLOCK_ALL_THIRD_PARTY)
    }

    @Test
    fun `onPageStarted resets page stats and clears pending https upgrades`() {
        shields.stats.recordAdBlocked()
        shields.stats.recordTrackerBlocked(TrackerCategory.ANALYTICS)
        val upgraded = shields.httpsUpgrader.tryUpgrade("http://example.com")
        assertThat(upgraded).isNotNull()

        shields.onPageStarted("http://example.com")

        assertThat(shields.stats.pageStats.value.total).isEqualTo(0)
        assertThat(shields.httpsUpgrader.onSslError(upgraded)).isNull()
    }

    @Test
    fun `singleton returns same instance`() {
        val second = BrowserShields.getInstance(context)

        assertThat(second).isSameInstanceAs(shields)
    }
}
