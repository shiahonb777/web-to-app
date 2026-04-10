package com.webtoapp.core.activation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ActivationCodeTest {

    @Test
    fun `fromJson parses valid activation code json`() {
        val json = """
            {
              "code":"ABC-123",
              "type":"TIME_LIMITED",
              "timeLimitMs":3600000,
              "usageLimit":5,
              "allowDeviceBinding":true
            }
        """.trimIndent()

        val parsed = ActivationCode.fromJson(json)

        assertThat(parsed).isNotNull()
        assertThat(parsed?.code).isEqualTo("ABC-123")
        assertThat(parsed?.type).isEqualTo(ActivationCodeType.TIME_LIMITED)
        assertThat(parsed?.timeLimitMs).isEqualTo(3_600_000L)
        assertThat(parsed?.usageLimit).isEqualTo(5)
        assertThat(parsed?.allowDeviceBinding).isTrue()
    }

    @Test
    fun `fromJson returns null for legacy code string`() {
        val parsed = ActivationCode.fromJson("LEGACY-CODE-001")
        assertThat(parsed).isNull()
    }

    @Test
    fun `fromJson returns null for malformed json`() {
        val parsed = ActivationCode.fromJson("{\"code\":\"A\"")
        assertThat(parsed).isNull()
    }

    @Test
    fun `fromLegacyString creates permanent code`() {
        val code = ActivationCode.fromLegacyString("OLD-CODE")

        assertThat(code.code).isEqualTo("OLD-CODE")
        assertThat(code.type).isEqualTo(ActivationCodeType.PERMANENT)
        assertThat(code.timeLimitMs).isNull()
        assertThat(code.usageLimit).isNull()
    }

    @Test
    fun `toJson and fromJson support roundtrip`() {
        val original = ActivationCode(
            code = "ROUNDTRIP",
            type = ActivationCodeType.COMBINED,
            timeLimitMs = 60_000L,
            usageLimit = 10,
            allowDeviceBinding = true,
            note = "test"
        )

        val restored = ActivationCode.fromJson(original.toJson())

        assertThat(restored).isNotNull()
        assertThat(restored?.copy(createdAt = 0L)).isEqualTo(original.copy(createdAt = 0L))
    }
}
