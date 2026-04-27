package com.webtoapp.core.disguise

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DisguiseConfigTest {

    // ═══════════════════════════════════════════
    // DISABLED preset
    // ═══════════════════════════════════════════

    @Test
    fun `DISABLED preset has enabled false`() {
        assertThat(DisguiseConfig.DISABLED.enabled).isFalse()
    }

    @Test
    fun `DISABLED preset has 1 icon`() {
        assertThat(DisguiseConfig.DISABLED.multiLauncherIcons).isEqualTo(1)
    }

    // ═══════════════════════════════════════════
    // MULTI_ICON presets
    // ═══════════════════════════════════════════

    @Test
    fun `MULTI_ICON_3 has 3 icons`() {
        assertThat(DisguiseConfig.MULTI_ICON_3.enabled).isTrue()
        assertThat(DisguiseConfig.MULTI_ICON_3.multiLauncherIcons).isEqualTo(3)
    }

    @Test
    fun `MULTI_ICON_5 has 5 icons`() {
        assertThat(DisguiseConfig.MULTI_ICON_5.enabled).isTrue()
        assertThat(DisguiseConfig.MULTI_ICON_5.multiLauncherIcons).isEqualTo(5)
    }

    // ═══════════════════════════════════════════
    // v2.0 Storm presets
    // ═══════════════════════════════════════════

    @Test
    fun `SUBTLE_FLOOD has 25 icons with SUBTLE mode`() {
        assertThat(DisguiseConfig.SUBTLE_FLOOD.multiLauncherIcons).isEqualTo(25)
        assertThat(DisguiseConfig.SUBTLE_FLOOD.iconStormMode).isEqualTo(DisguiseConfig.IconStormMode.SUBTLE)
    }

    @Test
    fun `ICON_FLOOD has 100 icons with FLOOD mode`() {
        assertThat(DisguiseConfig.ICON_FLOOD.multiLauncherIcons).isEqualTo(100)
        assertThat(DisguiseConfig.ICON_FLOOD.iconStormMode).isEqualTo(DisguiseConfig.IconStormMode.FLOOD)
    }

    @Test
    fun `ICON_STORM has 500 icons with STORM mode`() {
        assertThat(DisguiseConfig.ICON_STORM.multiLauncherIcons).isEqualTo(500)
        assertThat(DisguiseConfig.ICON_STORM.iconStormMode).isEqualTo(DisguiseConfig.IconStormMode.STORM)
    }

    @Test
    fun `EXTREME has 1000 icons with EXTREME mode`() {
        assertThat(DisguiseConfig.EXTREME.multiLauncherIcons).isEqualTo(1000)
        assertThat(DisguiseConfig.EXTREME.iconStormMode).isEqualTo(DisguiseConfig.IconStormMode.EXTREME)
    }

    @Test
    fun `RESEARCH has 5000 icons with RESEARCH mode`() {
        assertThat(DisguiseConfig.RESEARCH.multiLauncherIcons).isEqualTo(5000)
        assertThat(DisguiseConfig.RESEARCH.iconStormMode).isEqualTo(DisguiseConfig.IconStormMode.RESEARCH)
    }

    // ═══════════════════════════════════════════
    // custom factory
    // ═══════════════════════════════════════════

    @Test
    fun `custom creates config with specified count`() {
        val config = DisguiseConfig.custom(42)
        assertThat(config.enabled).isTrue()
        assertThat(config.multiLauncherIcons).isEqualTo(42)
        assertThat(config.iconStormMode).isEqualTo(DisguiseConfig.IconStormMode.CUSTOM)
    }

    @Test
    fun `custom coerces count to at least 2`() {
        val config = DisguiseConfig.custom(1)
        assertThat(config.multiLauncherIcons).isEqualTo(2)
    }

    @Test
    fun `custom supports random names and prefix`() {
        val config = DisguiseConfig.custom(10, randomNames = true, prefix = "App_")
        assertThat(config.randomizeNames).isTrue()
        assertThat(config.customNamePrefix).isEqualTo("App_")
    }

    // ═══════════════════════════════════════════
    // fromMode factory
    // ═══════════════════════════════════════════

    @Test
    fun `fromMode NORMAL creates 2 icons`() {
        val config = DisguiseConfig.fromMode(DisguiseConfig.IconStormMode.NORMAL)
        assertThat(config.multiLauncherIcons).isEqualTo(2)
    }

    @Test
    fun `fromMode FLOOD uses suggestedCount`() {
        val config = DisguiseConfig.fromMode(DisguiseConfig.IconStormMode.FLOOD)
        assertThat(config.multiLauncherIcons).isEqualTo(DisguiseConfig.IconStormMode.FLOOD.suggestedCount)
    }

    @Test
    fun `fromMode CUSTOM uses custom count`() {
        val config = DisguiseConfig.fromMode(DisguiseConfig.IconStormMode.CUSTOM, customCount = 77)
        assertThat(config.multiLauncherIcons).isEqualTo(77)
    }

    // ═══════════════════════════════════════════
    // IconStormMode
    // ═══════════════════════════════════════════

    @Test
    fun `IconStormMode has all expected modes`() {
        assertThat(DisguiseConfig.IconStormMode.values()).asList().containsExactly(
            DisguiseConfig.IconStormMode.NORMAL,
            DisguiseConfig.IconStormMode.SUBTLE,
            DisguiseConfig.IconStormMode.FLOOD,
            DisguiseConfig.IconStormMode.STORM,
            DisguiseConfig.IconStormMode.EXTREME,
            DisguiseConfig.IconStormMode.RESEARCH,
            DisguiseConfig.IconStormMode.CUSTOM
        )
    }

    @Test
    fun `IconStormMode suggested counts are monotonically increasing except CUSTOM`() {
        val modes = DisguiseConfig.IconStormMode.values().filter { it != DisguiseConfig.IconStormMode.CUSTOM }
        val counts = modes.map { it.suggestedCount }
        for (i in 1 until counts.size) {
            assertThat(counts[i]).isGreaterThan(counts[i - 1])
        }
    }

    // ═══════════════════════════════════════════
    // getAliasCount
    // ═══════════════════════════════════════════

    @Test
    fun `getAliasCount returns 0 when disabled`() {
        val config = DisguiseConfig(enabled = false, multiLauncherIcons = 5)
        assertThat(config.getAliasCount()).isEqualTo(0)
    }

    @Test
    fun `getAliasCount returns 0 when only 1 icon`() {
        val config = DisguiseConfig(enabled = true, multiLauncherIcons = 1)
        assertThat(config.getAliasCount()).isEqualTo(0)
    }

    @Test
    fun `getAliasCount returns icons minus 1 when enabled`() {
        val config = DisguiseConfig(enabled = true, multiLauncherIcons = 10)
        assertThat(config.getAliasCount()).isEqualTo(9)
    }

    // ═══════════════════════════════════════════
    // assessImpactLevel
    // ═══════════════════════════════════════════

    @Test
    fun `assessImpactLevel returns 0 for 10 or fewer icons`() {
        assertThat(DisguiseConfig.assessImpactLevel(1)).isEqualTo(0)
        assertThat(DisguiseConfig.assessImpactLevel(10)).isEqualTo(0)
    }

    @Test
    fun `assessImpactLevel returns 1 for 11-50 icons`() {
        assertThat(DisguiseConfig.assessImpactLevel(11)).isEqualTo(1)
        assertThat(DisguiseConfig.assessImpactLevel(50)).isEqualTo(1)
    }

    @Test
    fun `assessImpactLevel returns 2 for 51-200 icons`() {
        assertThat(DisguiseConfig.assessImpactLevel(51)).isEqualTo(2)
        assertThat(DisguiseConfig.assessImpactLevel(200)).isEqualTo(2)
    }

    @Test
    fun `assessImpactLevel returns 3 for 201-500 icons`() {
        assertThat(DisguiseConfig.assessImpactLevel(201)).isEqualTo(3)
        assertThat(DisguiseConfig.assessImpactLevel(500)).isEqualTo(3)
    }

    @Test
    fun `assessImpactLevel returns 4 for 501-2000 icons`() {
        assertThat(DisguiseConfig.assessImpactLevel(501)).isEqualTo(4)
        assertThat(DisguiseConfig.assessImpactLevel(2000)).isEqualTo(4)
    }

    @Test
    fun `assessImpactLevel returns 5 for over 2000 icons`() {
        assertThat(DisguiseConfig.assessImpactLevel(2001)).isEqualTo(5)
        assertThat(DisguiseConfig.assessImpactLevel(10000)).isEqualTo(5)
    }

    // ═══════════════════════════════════════════
    // estimateManifestOverhead
    // ═══════════════════════════════════════════

    @Test
    fun `estimateManifestOverhead for 1 icon is 0`() {
        assertThat(DisguiseConfig.estimateManifestOverhead(1)).isEqualTo(0L)
    }

    @Test
    fun `estimateManifestOverhead for 100 icons is approximately 50KB`() {
        // (100-1) * 520 = 51480 bytes ≈ 50 KB
        assertThat(DisguiseConfig.estimateManifestOverhead(100)).isEqualTo(51_480L)
    }

    @Test
    fun `estimateManifestOverhead for 5000 icons is approximately 2-6MB`() {
        // (5000-1) * 520 = 2599480 bytes
        assertThat(DisguiseConfig.estimateManifestOverhead(5000)).isEqualTo(2_599_480L)
    }

    // ═══════════════════════════════════════════
    // getEstimatedOverhead
    // ═══════════════════════════════════════════

    @Test
    fun `getEstimatedOverhead for DISABLED shows zero bytes`() {
        assertThat(DisguiseConfig.DISABLED.getEstimatedOverhead()).isEqualTo("0 B")
    }

    @Test
    fun `getEstimatedOverhead for ICON_FLOOD shows KB range`() {
        val overhead = DisguiseConfig.ICON_FLOOD.getEstimatedOverhead()
        assertThat(overhead).contains("KB")
    }

    @Test
    fun `getEstimatedOverhead for RESEARCH shows MB range`() {
        val overhead = DisguiseConfig.RESEARCH.getEstimatedOverhead()
        assertThat(overhead).contains("MB")
    }

    // ═══════════════════════════════════════════
    // getImpactLevel
    // ═══════════════════════════════════════════

    @Test
    fun `getImpactLevel matches assessImpactLevel for current config`() {
        assertThat(DisguiseConfig.ICON_STORM.getImpactLevel())
            .isEqualTo(DisguiseConfig.assessImpactLevel(500))
    }
}
