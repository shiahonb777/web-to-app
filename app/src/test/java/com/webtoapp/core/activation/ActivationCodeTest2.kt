package com.webtoapp.core.activation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ActivationCodeExtendedTest {





    @Test
    fun `fromJson parses valid JSON object`() {
        val json = """{"code":"ABC123","type":"PERMANENT"}"""
        val code = ActivationCode.fromJson(json)
        assertThat(code).isNotNull()
        assertThat(code!!.code).isEqualTo("ABC123")
        assertThat(code.type).isEqualTo(ActivationCodeType.PERMANENT)
    }

    @Test
    fun `fromJson parses TIME_LIMITED type with timeLimitMs`() {
        val json = """{"code":"TIME1","type":"TIME_LIMITED","timeLimitMs":86400000}"""
        val code = ActivationCode.fromJson(json)
        assertThat(code).isNotNull()
        assertThat(code!!.type).isEqualTo(ActivationCodeType.TIME_LIMITED)
        assertThat(code.timeLimitMs).isEqualTo(86400000L)
    }

    @Test
    fun `fromJson parses USAGE_LIMITED type with usageLimit`() {
        val json = """{"code":"USE5","type":"USAGE_LIMITED","usageLimit":5}"""
        val code = ActivationCode.fromJson(json)
        assertThat(code).isNotNull()
        assertThat(code!!.type).isEqualTo(ActivationCodeType.USAGE_LIMITED)
        assertThat(code.usageLimit).isEqualTo(5)
    }

    @Test
    fun `fromJson parses DEVICE_BOUND type with allowDeviceBinding`() {
        val json = """{"code":"DEV1","type":"DEVICE_BOUND","allowDeviceBinding":true}"""
        val code = ActivationCode.fromJson(json)
        assertThat(code).isNotNull()
        assertThat(code!!.type).isEqualTo(ActivationCodeType.DEVICE_BOUND)
        assertThat(code.allowDeviceBinding).isTrue()
    }

    @Test
    fun `fromJson parses COMBINED type`() {
        val json = """{"code":"COMBO","type":"COMBINED","timeLimitMs":3600000,"usageLimit":10}"""
        val code = ActivationCode.fromJson(json)
        assertThat(code).isNotNull()
        assertThat(code!!.type).isEqualTo(ActivationCodeType.COMBINED)
        assertThat(code.timeLimitMs).isEqualTo(3600000L)
        assertThat(code.usageLimit).isEqualTo(10)
    }

    @Test
    fun `fromJson returns null for non-JSON string`() {
        val code = ActivationCode.fromJson("ABC123")
        assertThat(code).isNull()
    }

    @Test
    fun `fromJson returns null for array JSON`() {
        val code = ActivationCode.fromJson("[1,2,3]")
        assertThat(code).isNull()
    }

    @Test
    fun `fromJson returns null for invalid JSON`() {
        val code = ActivationCode.fromJson("{invalid}")
        assertThat(code).isNull()
    }

    @Test
    fun `fromJson handles JSON with leading whitespace`() {
        val json = """  {"code":"ABC","type":"PERMANENT"}"""
        val code = ActivationCode.fromJson(json)
        assertThat(code).isNotNull()
        assertThat(code!!.code).isEqualTo("ABC")
    }





    @Test
    fun `toJson produces valid JSON with code`() {
        val code = ActivationCode(code = "TEST123")
        val json = code.toJson()
        assertThat(json).contains("TEST123")
        assertThat(json).contains("PERMANENT")
    }

    @Test
    fun `toJson and fromJson are inverse operations`() {
        val original = ActivationCode(
            code = "ROUNDTRIP",
            type = ActivationCodeType.USAGE_LIMITED,
            usageLimit = 100,
            allowDeviceBinding = true,
            note = "test note"
        )
        val json = original.toJson()
        val restored = ActivationCode.fromJson(json)
        assertThat(restored).isNotNull()
        assertThat(restored!!.code).isEqualTo(original.code)
        assertThat(restored.type).isEqualTo(original.type)
        assertThat(restored.usageLimit).isEqualTo(original.usageLimit)
        assertThat(restored.allowDeviceBinding).isEqualTo(original.allowDeviceBinding)
        assertThat(restored.note).isEqualTo(original.note)
    }





    @Test
    fun `fromLegacyString creates PERMANENT code`() {
        val code = ActivationCode.fromLegacyString("OLD_CODE")
        assertThat(code.code).isEqualTo("OLD_CODE")
        assertThat(code.type).isEqualTo(ActivationCodeType.PERMANENT)
    }

    @Test
    fun `fromLegacyString creates code with default values`() {
        val code = ActivationCode.fromLegacyString("SIMPLE")
        assertThat(code.timeLimitMs).isNull()
        assertThat(code.usageLimit).isNull()
        assertThat(code.allowDeviceBinding).isFalse()
    }





    @Test
    fun `ActivationCodeType has all expected values`() {
        assertThat(ActivationCodeType.values()).asList().containsExactly(
            ActivationCodeType.PERMANENT,
            ActivationCodeType.TIME_LIMITED,
            ActivationCodeType.USAGE_LIMITED,
            ActivationCodeType.DEVICE_BOUND,
            ActivationCodeType.COMBINED
        )
    }

    @Test
    fun `each ActivationCodeType has non-blank displayName and description`() {
        for (type in ActivationCodeType.values()) {
            assertThat(type.displayName).isNotEmpty()
            assertThat(type.description).isNotEmpty()
        }
    }
}
