package com.webtoapp.core.forcedrun

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ForcedRunPermissionStatusTest {

    @Test
    fun `basic protection is fully granted without extra permissions`() {
        val status = ForcedRunManager.PermissionStatus(
            level = ProtectionLevel.BASIC,
            hasAccessibility = false,
            hasUsageStats = false,
            message = ""
        )

        assertThat(status.isFullyGranted).isTrue()
    }

    @Test
    fun `standard protection only requires accessibility`() {
        val status = ForcedRunManager.PermissionStatus(
            level = ProtectionLevel.STANDARD,
            hasAccessibility = true,
            hasUsageStats = false,
            message = ""
        )

        assertThat(status.isFullyGranted).isTrue()
    }

    @Test
    fun `maximum protection requires both accessibility and usage stats`() {
        val status = ForcedRunManager.PermissionStatus(
            level = ProtectionLevel.MAXIMUM,
            hasAccessibility = true,
            hasUsageStats = false,
            message = ""
        )

        assertThat(status.isFullyGranted).isFalse()
    }
}
