package com.webtoapp.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationRoutesTest {

    @Test
    fun `detail route detection excludes top level tabs`() {
        assertFalse(isDetailRoute(null))
        assertFalse(isDetailRoute(Routes.HOME))
        assertTrue(isDetailRoute(Routes.STATS))
    }

    @Test
    fun `bottom bar visibility follows tab routes`() {
        assertTrue(shouldShowBottomBar(null))
        assertTrue(shouldShowBottomBar(Routes.HOME))
        assertFalse(shouldShowBottomBar(Routes.STATS))
    }

    @Test
    fun `tab index mapping stays stable`() {
        assertEquals(0, findTabIndexForRoute(Routes.HOME))
        assertEquals(4, findTabIndexForRoute(Routes.MORE))
        assertNull(findTabIndexForRoute(Routes.STATS))
    }
}
