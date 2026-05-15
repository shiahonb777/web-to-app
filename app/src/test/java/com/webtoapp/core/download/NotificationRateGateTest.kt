package com.webtoapp.core.download

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NotificationRateGateTest {

    @Test
    fun `blocks repeated updates within interval for same phase`() {
        val gate = NotificationRateGate(minIntervalMs = 2_000L)

        assertThat(gate.allow("downloading", "Node.js", 1_000L)).isTrue()
        assertThat(gate.allow("downloading", "Node.js", 2_000L)).isFalse()
        assertThat(gate.allow("downloading", "Node.js", 3_100L)).isTrue()
    }

    @Test
    fun `allows immediate update when phase changes`() {
        val gate = NotificationRateGate(minIntervalMs = 2_000L)

        assertThat(gate.allow("downloading", "Node.js", 1_000L)).isTrue()
        assertThat(gate.allow("paused", "Node.js", 1_100L)).isTrue()
        assertThat(gate.allow("extracting", "Node.js", 1_200L)).isTrue()
    }

    @Test
    fun `reset clears throttling state`() {
        val gate = NotificationRateGate(minIntervalMs = 2_000L)

        assertThat(gate.allow("downloading", "Node.js", 1_000L)).isTrue()
        gate.reset()
        assertThat(gate.allow("downloading", "Node.js", 1_100L)).isTrue()
    }
}
