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

    private fun startSleepProcess(seconds: Int): Process {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        return if (osName.contains("win")) {
            ProcessBuilder("cmd", "/c", "ping 127.0.0.1 -n ${seconds + 1} >NUL").start()
        } else {
            ProcessBuilder("sh", "-c", "sleep $seconds").start()
        }
    }

    private fun startExitProcess(): Process {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        return if (osName.contains("win")) {
            ProcessBuilder("cmd", "/c", "exit /b 0").start()
        } else {
            ProcessBuilder("sh", "-c", "echo done").start()
        }
    }

    @Test
    fun `isAliveCompat and waitForCompat reflect running process state`() {
        val process = startSleepProcess(seconds = 2)
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
        val process = startExitProcess()
        val finished = process.waitForCompat(1_000)

        assertThat(finished).isTrue()
        assertThat(process.waitFor(1, TimeUnit.SECONDS)).isTrue()
        assertThat(process.exitValue()).isEqualTo(0)
    }
}
