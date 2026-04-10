package com.webtoapp.ui.data.converter

import com.google.common.truth.Truth.assertThat
import com.google.gson.JsonParser
import com.webtoapp.core.activation.ActivationCode
import com.webtoapp.data.model.AppType
import com.webtoapp.data.model.UserAgentMode
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `toStringList returns empty list for malformed json`() {
        assertThat(converters.toStringList("not-json")).isEmpty()
    }

    @Test
    fun `app type converter falls back to WEB for unknown value`() {
        assertThat(converters.toAppType("NOT_EXISTING_TYPE")).isEqualTo(AppType.WEB)
    }

    @Test
    fun `web view config converter merges defaults for missing fields`() {
        val partial = """{"javaScriptEnabled":false,"userAgentMode":"CHROME_DESKTOP"}"""

        val config = converters.toWebViewConfig(partial)

        assertThat(config.javaScriptEnabled).isFalse()
        assertThat(config.userAgentMode).isEqualTo(UserAgentMode.CHROME_DESKTOP)
        assertThat(config.domStorageEnabled).isTrue()
        assertThat(config.hideToolbar).isFalse()
        assertThat(config.zoomEnabled).isTrue()
    }

    @Test
    fun `activation code list converter supports roundtrip`() {
        val list = listOf(
            ActivationCode(code = "A", note = "one"),
            ActivationCode(code = "B", note = "two")
        )

        val encoded = converters.fromActivationCodeList(list)
        val decoded = converters.toActivationCodeList(encoded)

        assertThat(decoded.map { it.code }).containsExactly("A", "B").inOrder()
        assertThat(decoded.map { it.note }).containsExactly("one", "two").inOrder()
    }

    @Test
    fun `mergeMissingDefaults preserves current values and fills missing recursively`() {
        val defaults = JsonParser.parseString(
            """
            {
              "root": {
                "a": 1,
                "b": 2
              },
              "enabled": true
            }
            """.trimIndent()
        )
        val current = JsonParser.parseString(
            """
            {
              "root": {
                "a": 99
              },
              "extra": "ok"
            }
            """.trimIndent()
        )

        val merged = Converters.mergeMissingDefaults(defaults, current).asJsonObject

        assertThat(merged.getAsJsonObject("root").get("a").asInt).isEqualTo(99)
        assertThat(merged.getAsJsonObject("root").get("b").asInt).isEqualTo(2)
        assertThat(merged.get("enabled").asBoolean).isTrue()
        assertThat(merged.get("extra").asString).isEqualTo("ok")
    }
}

