package com.webtoapp.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun `compareVersions handles major and minor correctly`() {
        assertThat(AppUpdateChecker.compareVersions("1.10.0", "1.9.9")).isGreaterThan(0)
        assertThat(AppUpdateChecker.compareVersions("2.0.0", "10.0.0")).isLessThan(0)
    }

    @Test
    fun `compareVersions handles v prefix and missing patch`() {
        assertThat(AppUpdateChecker.compareVersions("v1.5.0", "1.5.0")).isEqualTo(0)
        assertThat(AppUpdateChecker.compareVersions("1.5", "1.5.1")).isLessThan(0)
        assertThat(AppUpdateChecker.compareVersions("1.5.1", "1.5")).isGreaterThan(0)
    }

    @Test
    fun `compareVersions treats invalid input as zero`() {
        assertThat(AppUpdateChecker.compareVersions("invalid", "1.0.0")).isLessThan(0)
        assertThat(AppUpdateChecker.compareVersions("invalid", "invalid")).isEqualTo(0)
    }
}

