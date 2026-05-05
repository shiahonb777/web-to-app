package com.webtoapp.ui.shell

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShellWebViewNavigationTest {

    @Test
    fun `back exits when previous page is generated error page`() {
        assertThat(
            ShellWebViewNavigation.shouldFinishInsteadOfBack(
                currentUrl = "http://127.0.0.1:18500/",
                previousUrl = "data:text/html;charset=utf-8;base64,PGgxPkVycm9yPC9oMT4="
            )
        ).isTrue()
    }

    @Test
    fun `back exits when current generated error page points back to local runtime`() {
        assertThat(
            ShellWebViewNavigation.shouldFinishInsteadOfBack(
                currentUrl = "data:text/html;charset=utf-8;base64,PGgxPkVycm9yPC9oMT4=",
                previousUrl = "http://127.0.0.1:18500/"
            )
        ).isTrue()
    }

    @Test
    fun `back exits when previous page is blank`() {
        assertThat(
            ShellWebViewNavigation.shouldFinishInsteadOfBack(
                currentUrl = "http://127.0.0.1:18500/",
                previousUrl = "about:blank"
            )
        ).isTrue()
    }

    @Test
    fun `back keeps normal web history`() {
        assertThat(
            ShellWebViewNavigation.shouldFinishInsteadOfBack(
                currentUrl = "https://example.com/page2",
                previousUrl = "https://example.com/page1"
            )
        ).isFalse()
    }
}
