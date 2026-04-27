package com.webtoapp.core.forcedrun

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ForcedRunConfigTest {

    // ═══════════════════════════════════════════
    // DISABLED preset
    // ═══════════════════════════════════════════

    @Test
    fun `DISABLED preset has enabled false`() {
        assertThat(ForcedRunConfig.DISABLED.enabled).isFalse()
    }

    @Test
    fun `DISABLED preset uses FIXED_TIME mode by default`() {
        assertThat(ForcedRunConfig.DISABLED.mode).isEqualTo(ForcedRunMode.FIXED_TIME)
    }

    // ═══════════════════════════════════════════
    // STUDY_MODE preset
    // ═══════════════════════════════════════════

    @Test
    fun `STUDY_MODE preset is enabled`() {
        assertThat(ForcedRunConfig.STUDY_MODE.enabled).isTrue()
    }

    @Test
    fun `STUDY_MODE uses FIXED_TIME mode`() {
        assertThat(ForcedRunConfig.STUDY_MODE.mode).isEqualTo(ForcedRunMode.FIXED_TIME)
    }

    @Test
    fun `STUDY_MODE runs on weekdays only`() {
        assertThat(ForcedRunConfig.STUDY_MODE.activeDays).containsExactly(1, 2, 3, 4, 5)
    }

    @Test
    fun `STUDY_MODE blocks system UI and back button`() {
        assertThat(ForcedRunConfig.STUDY_MODE.blockSystemUI).isTrue()
        assertThat(ForcedRunConfig.STUDY_MODE.blockBackButton).isTrue()
        assertThat(ForcedRunConfig.STUDY_MODE.blockHomeButton).isTrue()
    }

    @Test
    fun `STUDY_MODE shows countdown`() {
        assertThat(ForcedRunConfig.STUDY_MODE.showCountdown).isTrue()
    }

    // ═══════════════════════════════════════════
    // FOCUS_MODE preset
    // ═══════════════════════════════════════════

    @Test
    fun `FOCUS_MODE preset is enabled`() {
        assertThat(ForcedRunConfig.FOCUS_MODE.enabled).isTrue()
    }

    @Test
    fun `FOCUS_MODE uses COUNTDOWN mode`() {
        assertThat(ForcedRunConfig.FOCUS_MODE.mode).isEqualTo(ForcedRunMode.COUNTDOWN)
    }

    @Test
    fun `FOCUS_MODE has 25 minute countdown (pomodoro)`() {
        assertThat(ForcedRunConfig.FOCUS_MODE.countdownMinutes).isEqualTo(25)
    }

    @Test
    fun `FOCUS_MODE allows emergency exit`() {
        assertThat(ForcedRunConfig.FOCUS_MODE.allowEmergencyExit).isTrue()
    }

    // ═══════════════════════════════════════════
    // KIDS_MODE preset
    // ═══════════════════════════════════════════

    @Test
    fun `KIDS_MODE preset is enabled`() {
        assertThat(ForcedRunConfig.KIDS_MODE.enabled).isTrue()
    }

    @Test
    fun `KIDS_MODE uses DURATION mode`() {
        assertThat(ForcedRunConfig.KIDS_MODE.mode).isEqualTo(ForcedRunMode.DURATION)
    }

    @Test
    fun `KIDS_MODE blocks all navigation`() {
        assertThat(ForcedRunConfig.KIDS_MODE.blockBackButton).isTrue()
        assertThat(ForcedRunConfig.KIDS_MODE.blockHomeButton).isTrue()
        assertThat(ForcedRunConfig.KIDS_MODE.blockRecentApps).isTrue()
        assertThat(ForcedRunConfig.KIDS_MODE.blockPowerButton).isTrue()
    }

    @Test
    fun `KIDS_MODE has emergency password`() {
        assertThat(ForcedRunConfig.KIDS_MODE.emergencyPassword).isEqualTo("1234")
    }

    @Test
    fun `KIDS_MODE allows access every day`() {
        assertThat(ForcedRunConfig.KIDS_MODE.accessDays).containsExactly(1, 2, 3, 4, 5, 6, 7)
    }

    // ═══════════════════════════════════════════
    // ForcedRunMode enum
    // ═══════════════════════════════════════════

    @Test
    fun `ForcedRunMode has all expected modes`() {
        assertThat(ForcedRunMode.values()).asList().containsExactly(
            ForcedRunMode.FIXED_TIME,
            ForcedRunMode.COUNTDOWN,
            ForcedRunMode.DURATION
        )
    }

    // ═══════════════════════════════════════════
    // ProtectionLevel
    // ═══════════════════════════════════════════

    @Test
    fun `default config has MAXIMUM protection level`() {
        val config = ForcedRunConfig(enabled = true)
        assertThat(config.protectionLevel).isEqualTo(ProtectionLevel.MAXIMUM)
    }

    // ═══════════════════════════════════════════
    // Data class copy
    // ═══════════════════════════════════════════

    @Test
    fun `config can be copied with modifications`() {
        val base = ForcedRunConfig.DISABLED
        val modified = base.copy(enabled = true, mode = ForcedRunMode.COUNTDOWN, countdownMinutes = 45)
        assertThat(modified.enabled).isTrue()
        assertThat(modified.mode).isEqualTo(ForcedRunMode.COUNTDOWN)
        assertThat(modified.countdownMinutes).isEqualTo(45)
    }
}
