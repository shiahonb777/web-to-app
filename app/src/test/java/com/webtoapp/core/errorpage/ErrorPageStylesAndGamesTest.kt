package com.webtoapp.core.errorpage

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ErrorPageStylesAndGamesTest {

    @Test
    fun `every built in style returns css and body with title subtitle`() {
        val title = "标题"
        val subtitle = "副标题"

        ErrorPageStyle.entries.forEach { style ->
            val css = ErrorPageStyles.getStyleCss(style)
            val body = ErrorPageStyles.getStyleBody(style, title, subtitle)

            assertThat(css).isNotEmpty()
            assertThat(body).contains(title)
            assertThat(body).contains(subtitle)
        }
    }

    @Test
    fun `style specific css markers are present`() {
        assertThat(ErrorPageStyles.getStyleCss(ErrorPageStyle.SATELLITE)).contains("twinkle")
        assertThat(ErrorPageStyles.getStyleCss(ErrorPageStyle.OCEAN)).contains("bubbles")
        assertThat(ErrorPageStyles.getStyleCss(ErrorPageStyle.FOREST)).contains("fireflies")
        assertThat(ErrorPageStyles.getStyleCss(ErrorPageStyle.MINIMAL)).contains("line-art")
        assertThat(ErrorPageStyles.getStyleCss(ErrorPageStyle.NEON)).contains("neon-grid")
    }

    @Test
    fun `every mini game returns executable js`() {
        MiniGameType.entries.forEach { type ->
            val js = ErrorPageGames.getGameJs(type)
            assertThat(js).contains("gameCanvas")
            assertThat(js).contains("(function()")
        }
    }

    @Test
    fun `mini game scripts include expected mechanics markers`() {
        assertThat(ErrorPageGames.getGameJs(MiniGameType.BREAKOUT)).contains("bricks")
        assertThat(ErrorPageGames.getGameJs(MiniGameType.MAZE)).contains("trail")
        assertThat(ErrorPageGames.getGameJs(MiniGameType.STAR_CATCH)).contains("stars")
        assertThat(ErrorPageGames.getGameJs(MiniGameType.INK_ZEN)).contains("inkDrops")
    }
}
