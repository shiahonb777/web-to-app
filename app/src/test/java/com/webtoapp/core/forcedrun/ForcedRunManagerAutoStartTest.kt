package com.webtoapp.core.forcedrun

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ForcedRunManagerAutoStartTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `countdown can auto start before any completion`() {
        val manager = ForcedRunManager(context)
        val config = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.COUNTDOWN,
            countdownMinutes = 15
        )

        assertThat(manager.canAutoStart(config)).isTrue()
    }

    @Test
    fun `countdown auto start is blocked after completion until config changes`() {
        val manager = ForcedRunManager(context)
        val config = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.COUNTDOWN,
            countdownMinutes = 15
        )

        manager.startForcedRunMode(config)
        manager.stopForcedRunMode()

        assertThat(manager.canAutoStart(config)).isFalse()
        assertThat(manager.canAutoStart(config.copy(countdownMinutes = 20))).isTrue()
    }

    @Test
    fun `non countdown modes can still auto start`() {
        val manager = ForcedRunManager(context)
        val config = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.FIXED_TIME,
            startTime = "08:00",
            endTime = "09:00"
        )

        manager.startForcedRunMode(config)
        manager.stopForcedRunMode()

        assertThat(manager.canAutoStart(config)).isTrue()
    }
}
