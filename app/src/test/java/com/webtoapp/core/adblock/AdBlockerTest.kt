package com.webtoapp.core.adblock

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class AdBlockerTest {

    private lateinit var adBlocker: AdBlocker

    @Before
    fun setUp() {
        adBlocker = AdBlocker()
    }

    @Test
    fun `shouldBlock returns false when blocker disabled`() {
        adBlocker.initialize(useDefaultRules = true)
        adBlocker.setEnabled(false)

        assertThat(adBlocker.shouldBlock("https://doubleclick.net/banner.js")).isFalse()
    }

    @Test
    fun `default rules block known ad domains including subdomains`() {
        adBlocker.initialize(useDefaultRules = true)
        adBlocker.setEnabled(true)

        assertThat(adBlocker.shouldBlock("https://doubleclick.net/ads")).isTrue()
        assertThat(adBlocker.shouldBlock("https://sub.doubleclick.net/script.js")).isTrue()
    }

    @Test
    fun `translation whitelist is never blocked`() {
        adBlocker.initialize(useDefaultRules = true)
        adBlocker.setEnabled(true)

        assertThat(adBlocker.shouldBlock("https://translate.googleapis.com/translate_a/single")).isFalse()
        assertThat(adBlocker.shouldBlock("https://translate.google.com")).isFalse()
    }

    @Test
    fun `custom wildcard rule can be added and removed`() {
        adBlocker.initialize(useDefaultRules = false)
        adBlocker.setEnabled(true)

        adBlocker.addRule("*tracker*")
        assertThat(adBlocker.shouldBlock("https://example.com/tracker/pixel")).isTrue()

        adBlocker.removeRule("*tracker*")
        assertThat(adBlocker.shouldBlock("https://example.com/tracker/pixel")).isFalse()
    }

    @Test
    fun `custom substring rule matches full url`() {
        adBlocker.initialize(useDefaultRules = false)
        adBlocker.setEnabled(true)

        adBlocker.addRule("example.com/ads")
        assertThat(adBlocker.shouldBlock("https://cdn.example.com/ads/index.js")).isTrue()
        assertThat(adBlocker.shouldBlock("https://cdn.example.com/content/index.js")).isFalse()
    }

    @Test
    fun `clearRules clears all normal rules but keeps hosts rules separate`() {
        adBlocker.initialize(useDefaultRules = true)
        adBlocker.setEnabled(true)
        assertThat(adBlocker.getRuleCount()).isGreaterThan(0)

        adBlocker.clearRules()
        assertThat(adBlocker.getRuleCount()).isEqualTo(0)
        assertThat(adBlocker.shouldBlock("https://doubleclick.net/ads")).isFalse()
    }

    @Test
    fun `stats reflect network filter count after adding rules`() {
        adBlocker.initialize(useDefaultRules = false)
        adBlocker.setEnabled(true)
        adBlocker.addRule("*adunit*")

        assertThat(adBlocker.getStats()["networkBlock"]).isEqualTo(1)
        assertThat(adBlocker.shouldBlock("https://example.com/adunit.js")).isTrue()
    }

    @Test
    fun `aliexpress and alicdn parent domains protect all subdomains from custom rules`() {
        adBlocker.initialize(useDefaultRules = true)
        adBlocker.setEnabled(true)


        adBlocker.addRule("||alicdn.com^")
        adBlocker.addRule("||aliexpress-media.com^")
        adBlocker.addRule("||aliexpress.com^")


        assertThat(adBlocker.shouldBlock("https://ae01.alicdn.com/kf/product.jpg", resourceType = "image")).isFalse()
        assertThat(adBlocker.shouldBlock("https://ae02.alicdn.com/kf/product.jpg", resourceType = "image")).isFalse()
        assertThat(adBlocker.shouldBlock("https://ae03.alicdn.com/kf/product.jpg", resourceType = "image")).isFalse()
        assertThat(adBlocker.shouldBlock("https://ae04.alicdn.com/kf/product.jpg", resourceType = "image")).isFalse()
        assertThat(adBlocker.shouldBlock("https://ae05.alicdn.com/kf/product.jpg", resourceType = "image")).isFalse()
        assertThat(adBlocker.shouldBlock("https://img.alicdn.com/imgextra/i1/x.jpg", resourceType = "image")).isFalse()
        assertThat(adBlocker.shouldBlock("https://gw.alicdn.com/tps/i4/x.png", resourceType = "image")).isFalse()
        assertThat(adBlocker.shouldBlock("https://g.alicdn.com/code/lib/x.js", resourceType = "script")).isFalse()
        assertThat(adBlocker.shouldBlock("https://at.alicdn.com/t/font.woff2", resourceType = "font")).isFalse()


        assertThat(adBlocker.shouldBlock("https://ae-pic-a1.aliexpress-media.com/kf/x.jpg", resourceType = "image")).isFalse()
        assertThat(adBlocker.shouldBlock("https://ae-pic-a2.aliexpress-media.com/kf/x.jpg", resourceType = "image")).isFalse()


        assertThat(adBlocker.shouldBlock("https://aliexpress.ru/item/1.html", resourceType = "main_frame")).isFalse()
        assertThat(adBlocker.shouldBlock("https://m.aliexpress.ru/store/x", resourceType = "xmlhttprequest")).isFalse()
        assertThat(adBlocker.shouldBlock("https://www.aliexpress.com/item/x", resourceType = "main_frame")).isFalse()
        assertThat(adBlocker.shouldBlock("https://s.click.aliexpress.com/e/abc", resourceType = "xmlhttprequest")).isFalse()
    }

}

