package com.webtoapp.core.stats

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppUsageStatsTest {





    @Test
    fun `formattedTotalUsage shows less than 1m for under 60 seconds`() {
        val stats = AppUsageStats(appId = 1, totalUsageMs = 30_000)
        assertThat(stats.formattedTotalUsage).isEqualTo("<1m")
    }

    @Test
    fun `formattedTotalUsage shows minutes only for under 1 hour`() {
        val stats = AppUsageStats(appId = 1, totalUsageMs = 25 * 60 * 1000L)
        assertThat(stats.formattedTotalUsage).isEqualTo("25m")
    }

    @Test
    fun `formattedTotalUsage shows hours and minutes for over 1 hour`() {
        val stats = AppUsageStats(appId = 1, totalUsageMs = 90 * 60 * 1000L)
        assertThat(stats.formattedTotalUsage).isEqualTo("1h 30m")
    }

    @Test
    fun `formattedTotalUsage shows 0m for exactly 0 ms`() {
        val stats = AppUsageStats(appId = 1, totalUsageMs = 0)
        assertThat(stats.formattedTotalUsage).isEqualTo("<1m")
    }

    @Test
    fun `formattedTotalUsage handles large values`() {
        val stats = AppUsageStats(appId = 1, totalUsageMs = 10 * 3600 * 1000L)
        assertThat(stats.formattedTotalUsage).isEqualTo("10h 0m")
    }





    @Test
    fun `HealthStatus has all expected values`() {
        assertThat(HealthStatus.values()).asList().containsExactly(
            HealthStatus.UNKNOWN,
            HealthStatus.ONLINE,
            HealthStatus.SLOW,
            HealthStatus.OFFLINE
        )
    }





    @Test
    fun `AppHealthRecord default status is UNKNOWN`() {
        val record = AppHealthRecord(appId = 1, url = "https://example.com")
        assertThat(record.status).isEqualTo(HealthStatus.UNKNOWN)
        assertThat(record.responseTimeMs).isEqualTo(0)
        assertThat(record.httpStatusCode).isEqualTo(0)
    }





    @Test
    fun `AppHealthSummary holds all fields`() {
        val summary = AppHealthSummary(
            appId = 1,
            latestStatus = HealthStatus.ONLINE,
            latestResponseTimeMs = 150,
            lastCheckedAt = 1000L,
            uptimePercent = 99.5f
        )
        assertThat(summary.appId).isEqualTo(1)
        assertThat(summary.latestStatus).isEqualTo(HealthStatus.ONLINE)
        assertThat(summary.uptimePercent).isWithin(0.01f).of(99.5f)
    }
}
