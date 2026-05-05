package com.webtoapp.core.cloud

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GitHubHostsDnsTest {

    @Test
    fun `shouldRefresh blocks within success ttl`() {
        val now = 10_000_000L
        val lastSuccess = now - 1_000L
        val lastAttempt = 0L

        assertThat(GitHubHostsDns.shouldRefresh(now, lastSuccess, lastAttempt)).isFalse()
    }

    @Test
    fun `shouldRefresh blocks within retry ttl after failure`() {
        val now = 10_000_000L
        val lastSuccess = 0L
        val lastAttempt = now - 1_000L

        assertThat(GitHubHostsDns.shouldRefresh(now, lastSuccess, lastAttempt)).isFalse()
    }

    @Test
    fun `shouldRefresh allows after ttl expires`() {
        val now = 10_000_000L
        val lastSuccess = 0L
        val lastAttempt = 0L

        assertThat(GitHubHostsDns.shouldRefresh(now, lastSuccess, lastAttempt)).isTrue()
    }
}
