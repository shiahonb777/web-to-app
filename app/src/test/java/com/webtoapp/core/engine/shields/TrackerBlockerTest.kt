package com.webtoapp.core.engine.shields

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TrackerBlockerTest {

    private val blocker = TrackerBlocker()

    @Test
    fun `checkTracker identifies analytics and ad network domains`() {
        assertThat(blocker.checkTracker("https://www.google-analytics.com/collect"))
            .isEqualTo(TrackerCategory.ANALYTICS)
        assertThat(blocker.checkTracker("https://a.b.doubleclick.net/pagead/ads"))
            .isEqualTo(TrackerCategory.AD_NETWORK)
    }

    @Test
    fun `checkTracker identifies social trackers`() {
        val category = blocker.checkTracker("https://platform.twitter.com/widgets.js")

        assertThat(category).isEqualTo(TrackerCategory.SOCIAL)
    }

    @Test
    fun `checkTracker returns null for non tracker and malformed urls`() {
        assertThat(blocker.checkTracker("https://example.com/index.html")).isNull()
        assertThat(blocker.checkTracker("not a url")).isNull()
    }

    @Test
    fun `shouldBlock follows tracker detection result`() {
        assertThat(blocker.shouldBlock("https://pixel.facebook.com/tr?id=123")).isTrue()
        assertThat(blocker.shouldBlock("https://developer.mozilla.org/")).isFalse()
    }

    @Test
    fun `rule count includes domain rules plus extra path patterns`() {
        val categorySum = blocker.getCategoryStats().values.sum()
        val ruleCount = blocker.getRuleCount()

        assertThat(categorySum).isGreaterThan(0)
        assertThat(ruleCount).isGreaterThan(0)
        assertThat(ruleCount).isGreaterThan(categorySum)
    }
}
