package com.webtoapp.core.engine.shields

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReaderModeTest {

    private val readerMode = ReaderMode()

    @Test
    fun `generateReaderHtml escapes title and applies theme style`() {
        val html = readerMode.generateReaderHtml(
            title = "<Hello & 'World'>",
            siteName = "Site",
            author = "Author",
            content = "<p>Body</p>",
            url = "https://example.com/post",
            theme = ReaderMode.Theme.DARK,
            fontSize = 20,
            lineHeight = 2.0f
        )

        assertThat(html).contains("&lt;Hello &amp; &#39;World&#39;&gt;")
        assertThat(html).contains("background-color: #1A1A1A")
        assertThat(html).contains("font-size: 20px")
        assertThat(html).contains("<span>Site</span>")
        assertThat(html).contains("<span> · Author</span>")
    }

    @Test
    fun `generateReaderHtml omits optional metadata when empty`() {
        val html = readerMode.generateReaderHtml(
            title = "Title",
            siteName = "",
            author = "",
            content = "<p>Body</p>",
            url = "https://example.com"
        )

        assertThat(html).doesNotContain("<span> · ")
    }

    @Test
    fun `generateExtractScript contains readability extraction logic`() {
        val script = readerMode.generateExtractScript()

        assertThat(script).contains("Readability-lite algorithm")
        assertThat(script).contains("removeSelectors")
        assertThat(script).contains("JSON.stringify(result)")
    }

    @Test
    fun `generateDetectScript contains readability heuristic thresholds`() {
        val script = readerMode.generateDetectScript()

        assertThat(script).contains("textLen > 800")
        assertThat(script).contains("pCount >= 3")
        assertThat(script).contains("return isReadable ? 'true' : 'false'")
    }
}
