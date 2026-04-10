package com.webtoapp.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GsonProviderTest {

    data class SamplePayload(
        val id: Int,
        val name: String
    )

    @Test
    fun `gson provider exposes singleton instance`() {
        val first = GsonProvider.gson
        val second = GsonProvider.gson

        assertThat(first).isSameInstanceAs(second)
    }

    @Test
    fun `gson provider serializes and deserializes data classes`() {
        val payload = SamplePayload(id = 7, name = "WebToApp")

        val json = GsonProvider.gson.toJson(payload)
        val parsed = GsonProvider.gson.fromJson(json, SamplePayload::class.java)

        assertThat(parsed).isEqualTo(payload)
    }
}
