package com.webtoapp.core.apkbuilder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApkEntryPolicyTest {

    @Test
    fun `php library only kept for php like app types`() {
        assertTrue(ApkEntryPolicy.isRequiredNativeLib("libphp.so", "PHP_APP", "SYSTEM_WEBVIEW"))
        assertFalse(ApkEntryPolicy.isRequiredNativeLib("libphp.so", "WEB", "SYSTEM_WEBVIEW"))
    }

    @Test
    fun `gecko libraries follow engine type`() {
        assertTrue(ApkEntryPolicy.isRequiredNativeLib("libnss3.so", "WEB", "GECKOVIEW"))
        assertFalse(ApkEntryPolicy.isRequiredNativeLib("libnss3.so", "WEB", "SYSTEM_WEBVIEW"))
    }

    @Test
    fun `editor only assets are stripped by app type`() {
        assertTrue(ApkEntryPolicy.isEditorOnlyAsset("assets/sample_projects/demo/index.html", "WEB", "SYSTEM_WEBVIEW"))
        assertFalse(ApkEntryPolicy.isEditorOnlyAsset("assets/php_router_server.php", "PHP_APP", "SYSTEM_WEBVIEW"))
        assertTrue(ApkEntryPolicy.isEditorOnlyAsset("assets/php_router_server.php", "WEB", "SYSTEM_WEBVIEW"))
    }

    @Test
    fun `icon policies detect launcher assets`() {
        assertTrue(ApkEntryPolicy.isIconEntry("res/mipmap-xxxhdpi-v4/ic_launcher.png"))
        assertTrue(ApkEntryPolicy.isAdaptiveIconEntry("res/drawable/ic_launcher_foreground.png"))
        assertTrue(ApkEntryPolicy.isAdaptiveIconDefinition("res/mipmap-anydpi-v26/ic_launcher.xml"))
    }
}
