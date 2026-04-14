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
    fun `stats expose current rule buckets for custom wildcard rules`() {
        adBlocker.initialize(useDefaultRules = false)
        adBlocker.setEnabled(true)
        adBlocker.addRule("*adunit*")

        assertThat(adBlocker.shouldBlock("https://example.com/adunit.js")).isTrue()

        val stats = adBlocker.getStats()
        assertThat(stats.keys).containsExactly(
            "exactHosts",
            "hostsFile",
            "networkBlock",
            "networkException",
            "cosmeticBlock",
            "cosmeticException",
            "scriptlets"
        )
        assertThat(stats["networkBlock"]).isEqualTo(1)
        assertThat(stats["scriptlets"]).isEqualTo(0)
    }
}

