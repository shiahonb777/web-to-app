package com.webtoapp.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigationRoutesTest {

    @Test
    fun `detail route detection excludes tab host`() {
        assertFalse(isDetailRoute(null))
        assertFalse(isDetailRoute(TAB_HOST_ROUTE))
        assertTrue(isDetailRoute(Routes.STATS))
    }

    @Test
    fun `bottom bar visibility follows top level routes`() {
        assertTrue(shouldShowBottomBar(TAB_HOST_ROUTE))
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
