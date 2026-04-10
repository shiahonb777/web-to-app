package com.webtoapp.core.engine.shields

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShieldsStatsTest {

    @Test
    fun `record methods update page and session stats`() {
        val stats = ShieldsStats()

        stats.recordAdBlocked()
        stats.recordTrackerBlocked(TrackerCategory.SOCIAL)
        stats.recordTrackerBlocked(TrackerCategory.SOCIAL)
        stats.recordHttpsUpgrade()
        stats.recordCookieConsentBlocked()
        stats.recordThirdPartyCookieBlocked()

        val page = stats.pageStats.value
        val session = stats.sessionStats.value

        assertThat(page.adsBlocked).isEqualTo(1)
        assertThat(page.trackersBlocked).isEqualTo(2)
        assertThat(page.httpsUpgrades).isEqualTo(1)
        assertThat(page.cookieConsentsBlocked).isEqualTo(1)
        assertThat(page.thirdPartyCookiesBlocked).isEqualTo(1)
        assertThat(page.trackerCategories[TrackerCategory.SOCIAL]).isEqualTo(2)
        assertThat(page.total).isEqualTo(6)

        assertThat(session.totalAdsBlocked).isEqualTo(1)
        assertThat(session.totalTrackersBlocked).isEqualTo(2)
        assertThat(session.totalHttpsUpgrades).isEqualTo(1)
        assertThat(session.totalCookieConsentsBlocked).isEqualTo(1)
        assertThat(session.totalThirdPartyCookiesBlocked).isEqualTo(1)
        assertThat(session.total).isEqualTo(6)

        assertThat(stats.totalPageBlocked).isEqualTo(6)
    }

    @Test
    fun `resetPageStats does not clear session stats`() {
        val stats = ShieldsStats()
        stats.recordAdBlocked()
        stats.recordTrackerBlocked(TrackerCategory.ANALYTICS)

        stats.resetPageStats()

        assertThat(stats.pageStats.value.total).isEqualTo(0)
        assertThat(stats.sessionStats.value.total).isEqualTo(2)
    }

    @Test
    fun `resetSessionStats clears session counters`() {
        val stats = ShieldsStats()
        stats.recordAdBlocked()
        stats.recordHttpsUpgrade()

        stats.resetSessionStats()

        assertThat(stats.sessionStats.value.total).isEqualTo(0)
        assertThat(stats.pageStats.value.total).isEqualTo(2)
    }
}
