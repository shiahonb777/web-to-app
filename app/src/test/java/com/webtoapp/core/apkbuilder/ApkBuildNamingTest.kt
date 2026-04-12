package com.webtoapp.core.apkbuilder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkBuildNamingTest {

    @Test
    fun `generatePackageName returns stable short package`() {
        val pkg = ApkBuildNaming.generatePackageName("My Fancy App")

        assertTrue(pkg.startsWith("com.w2a."))
        assertTrue(pkg.length <= 12)
        assertTrue(pkg.substringAfterLast('.').matches(Regex("[a-z][a-z0-9]{3}")))
    }

    @Test
    fun `normalizePackageSegment rewrites illegal leading characters`() {
        assertEquals("a123", ApkBuildNaming.normalizePackageSegment("0123"))
        assertEquals("azzz", ApkBuildNaming.normalizePackageSegment("-zzz"))
    }

    @Test
    fun `sanitizeFileName strips invalid characters and limits length`() {
        val sanitized = ApkBuildNaming.sanitizeFileName("bad:/\\\\name*?.apk")

        assertTrue(sanitized.contains("_"))
        assertTrue(sanitized.length <= 50)
    }
}
