package com.webtoapp.core.activation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ActivationStatusTest {





    @Test
    fun `isValid returns false when not activated`() {
        val status = ActivationStatus(isActivated = false)
        assertThat(status.isValid).isFalse()
    }

    @Test
    fun `isValid returns true when activated with no limits`() {
        val status = ActivationStatus(isActivated = true)
        assertThat(status.isValid).isTrue()
    }

    @Test
    fun `isValid returns false when expired`() {
        val status = ActivationStatus(
            isActivated = true,
            expireTime = System.currentTimeMillis() - 1000
        )
        assertThat(status.isValid).isFalse()
    }

    @Test
    fun `isValid returns true when not yet expired`() {
        val status = ActivationStatus(
            isActivated = true,
            expireTime = System.currentTimeMillis() + 60_000
        )
        assertThat(status.isValid).isTrue()
    }

    @Test
    fun `isValid returns false when usage exceeded`() {
        val status = ActivationStatus(
            isActivated = true,
            usageCount = 5,
            usageLimit = 5
        )
        assertThat(status.isValid).isFalse()
    }

    @Test
    fun `isValid returns true when usage within limit`() {
        val status = ActivationStatus(
            isActivated = true,
            usageCount = 3,
            usageLimit = 5
        )
        assertThat(status.isValid).isTrue()
    }





    @Test
    fun `isExpired returns false when no expireTime`() {
        val status = ActivationStatus(isActivated = true)
        assertThat(status.isExpired).isFalse()
    }

    @Test
    fun `isExpired returns true when past expireTime`() {
        val status = ActivationStatus(expireTime = System.currentTimeMillis() - 1)
        assertThat(status.isExpired).isTrue()
    }

    @Test
    fun `isExpired returns false when before expireTime`() {
        val status = ActivationStatus(expireTime = System.currentTimeMillis() + 60_000)
        assertThat(status.isExpired).isFalse()
    }





    @Test
    fun `isUsageExceeded returns false when no usageLimit`() {
        val status = ActivationStatus(usageCount = 100)
        assertThat(status.isUsageExceeded).isFalse()
    }

    @Test
    fun `isUsageExceeded returns true when count equals limit`() {
        val status = ActivationStatus(usageCount = 10, usageLimit = 10)
        assertThat(status.isUsageExceeded).isTrue()
    }

    @Test
    fun `isUsageExceeded returns false when count below limit`() {
        val status = ActivationStatus(usageCount = 5, usageLimit = 10)
        assertThat(status.isUsageExceeded).isFalse()
    }





    @Test
    fun `remainingTimeMs returns null when no expireTime`() {
        val status = ActivationStatus()
        assertThat(status.remainingTimeMs).isNull()
    }

    @Test
    fun `remainingTimeMs returns positive value when not expired`() {
        val future = System.currentTimeMillis() + 30_000
        val status = ActivationStatus(expireTime = future)
        assertThat(status.remainingTimeMs).isGreaterThan(0L)
    }

    @Test
    fun `remainingTimeMs returns 0 when already expired`() {
        val past = System.currentTimeMillis() - 10_000
        val status = ActivationStatus(expireTime = past)
        assertThat(status.remainingTimeMs).isEqualTo(0L)
    }





    @Test
    fun `remainingUsage returns null when no usageLimit`() {
        val status = ActivationStatus(usageCount = 5)
        assertThat(status.remainingUsage).isNull()
    }

    @Test
    fun `remainingUsage returns positive when within limit`() {
        val status = ActivationStatus(usageCount = 3, usageLimit = 10)
        assertThat(status.remainingUsage).isEqualTo(7)
    }

    @Test
    fun `remainingUsage returns 0 when usage exceeded`() {
        val status = ActivationStatus(usageCount = 10, usageLimit = 10)
        assertThat(status.remainingUsage).isEqualTo(0)
    }
}
