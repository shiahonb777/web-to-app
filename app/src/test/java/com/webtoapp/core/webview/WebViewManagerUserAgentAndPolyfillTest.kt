package com.webtoapp.core.webview

import com.google.common.truth.Truth.assertThat
import org.junit.Test




class WebViewManagerUserAgentAndPolyfillTest {




    @Test
    fun `stripWebViewMarker removes wv marker from standard Android WebView UA`() {
        val androidWebViewUa =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7; wv) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Version/4.0 Chrome/130.0.0.0 Mobile Safari/537.36"

        val sanitized = WebViewManager.stripWebViewMarker(androidWebViewUa)

        assertThat(sanitized).doesNotContain("; wv)")
        assertThat(sanitized).doesNotContain(" wv)")
        assertThat(sanitized).doesNotContain("wv)")
        assertThat(sanitized).contains("Pixel 7)")
        assertThat(sanitized).contains("Chrome/130.0.0.0")
    }

    @Test
    fun `stripWebViewMarker is a no-op when UA has no wv marker`() {
        val chromeUa =
            "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/130.0.0.0 Mobile Safari/537.36"

        assertThat(WebViewManager.stripWebViewMarker(chromeUa)).isEqualTo(chromeUa)
    }

    @Test
    fun `stripWebViewMarker tolerates variants with extra spaces`() {
        val variant = "Mozilla/5.0 (Linux; Android 10; SM-G965F;  wv ) AppleWebKit/537.36 Chrome/100.0 Mobile"

        val sanitized = WebViewManager.stripWebViewMarker(variant)

        assertThat(sanitized).doesNotContain("wv")
        assertThat(sanitized).contains("SM-G965F)")
    }

    @Test
    fun `stripWebViewMarker leaves unrelated wv-like tokens untouched`() {


        val url = "https://example.wvtest.com/path?x=wvalue"
        val ua = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36"

        assertThat(WebViewManager.stripWebViewMarker(url)).isEqualTo(url)
        assertThat(WebViewManager.stripWebViewMarker(ua)).isEqualTo(ua)
    }

    @Test
    fun `stripWebViewMarker returns empty for empty input`() {
        assertThat(WebViewManager.stripWebViewMarker("")).isEmpty()
    }




    @Test
    fun `notification polyfill defines Notification API on window`() {
        val script = WebViewManager.NotificationPolyfillHolder.SCRIPT

        assertThat(script).contains("window.Notification = WebToAppNotification")
        assertThat(script).contains("WebToAppNotification.requestPermission = requestPermission")
        assertThat(script).contains("'permission'")
    }

    @Test
    fun `notification polyfill defines PushManager stub so capability checks pass`() {
        val script = WebViewManager.NotificationPolyfillHolder.SCRIPT

        assertThat(script).contains("typeof window.PushManager === 'undefined'")
        assertThat(script).contains("window.PushManager = WebToAppPushManager")
        assertThat(script).contains("WebToAppPushManager.prototype.subscribe")
        assertThat(script).contains("WebToAppPushManager.prototype.getSubscription")
        assertThat(script).contains("WebToAppPushManager.prototype.permissionState")
    }

    @Test
    fun `notification polyfill subscribe rejects so callers fall back to local notifications`() {
        val script = WebViewManager.NotificationPolyfillHolder.SCRIPT

        assertThat(script).contains("Promise.reject(new DOMException(")
        assertThat(script).contains("'NotAllowedError'")
    }

    @Test
    fun `notification polyfill defines PushSubscription stub`() {
        val script = WebViewManager.NotificationPolyfillHolder.SCRIPT

        assertThat(script).contains("typeof window.PushSubscription === 'undefined'")
        assertThat(script).contains("window.PushSubscription = WebToAppPushSubscription")
        assertThat(script).contains("WebToAppPushSubscription.prototype.unsubscribe")
    }

    @Test
    fun `notification polyfill wires pushManager onto service worker registration`() {
        val script = WebViewManager.NotificationPolyfillHolder.SCRIPT

        assertThat(script).contains("navigator.serviceWorker.ready")
        assertThat(script).contains("reg.showNotification")
        assertThat(script).contains("reg.pushManager = new window.PushManager()")
        assertThat(script).contains("reg.getNotifications")
    }

    @Test
    fun `notification polyfill patches navigator permissions query for notifications`() {
        val script = WebViewManager.NotificationPolyfillHolder.SCRIPT

        assertThat(script).contains("desc.name === 'notifications'")
        assertThat(script).contains("navigator.permissions.query")
    }
}
