package com.webtoapp.util

import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ProcessCompatTest {

    @Test
    fun `isAliveCompat and waitForCompat reflect running process state`() {
        val process = ProcessBuilder("sh", "-c", "sleep 2").start()
        try {
            assertThat(process.isAliveCompat()).isTrue()
            assertThat(process.waitForCompat(100)).isFalse()
            assertThat(process.isAliveCompat()).isTrue()
        } finally {
            process.destroyForciblyCompat()
            process.waitFor(2, TimeUnit.SECONDS)
        }
        assertThat(process.isAliveCompat()).isFalse()
    }

    @Test
    fun `waitForCompat returns true when process exits within timeout`() {
        val process = ProcessBuilder("sh", "-c", "echo done").start()
        val finished = process.waitForCompat(1_000)

        assertThat(finished).isTrue()
        assertThat(process.waitFor(1, TimeUnit.SECONDS)).isTrue()
        assertThat(process.exitValue()).isEqualTo(0)
    }
}
