package com.webtoapp.core.forcedrun

import com.google.common.truth.Truth.assertThat
import com.webtoapp.core.blacktech.BlackTechConfig
import org.junit.Test

class ForcedRunHardwareControllerRuntimePlanTest {

    @Test
    fun `normalize black tech plan disables unsupported system toggles`() {
        val config = BlackTechConfig(
            enabled = true,
            forceAirplaneMode = true,
            forceBlockVolumeKeys = true,
            forceDisableWifi = true,
            forceDisableBluetooth = true,
            forceDisableMobileData = true,
            forceWifiHotspot = true,
            forceMaxPerformance = true,
            forceBlackScreen = true
        )

        val capabilities = ForcedRunHardwareController.RuntimeCapabilityMatrix(
            hasAccessibilityService = false,
            canWriteSystemSettings = false,
            hasJavaFlashlight = false,
            hasJavaVibrator = false,
            hasNativeFlashlight = false,
            hasNativeVibrator = false,
            hasNativeBrightness = false,
            hasNativeCpuGovernor = false,
            canControlWifi = false,
            canControlBluetooth = false,
            canControlMobileData = false,
            canUseLegacyHotspot = false,
            canUseSharedHotspot = false,
            canUseLocalOnlyHotspot = false,
            canKeepScreenAwake = false,
            hasActivityBinding = false
        )

        val plan = ForcedRunHardwareController.normalizeBlackTechConfigForRuntime(config, capabilities)

        assertThat(plan.normalizedConfig.enabled).isFalse()
        assertThat(plan.normalizedConfig.forceAirplaneMode).isFalse()
        assertThat(plan.normalizedConfig.forceBlockVolumeKeys).isFalse()
        assertThat(plan.normalizedConfig.forceDisableWifi).isFalse()
        assertThat(plan.normalizedConfig.forceDisableBluetooth).isFalse()
        assertThat(plan.normalizedConfig.forceDisableMobileData).isFalse()
        assertThat(plan.normalizedConfig.forceWifiHotspot).isFalse()
        assertThat(plan.normalizedConfig.forceMaxPerformance).isFalse()
        assertThat(plan.normalizedConfig.forceBlackScreen).isFalse()
        assertThat(plan.normalizedConfig.forceBlockTouch).isFalse()
        assertThat(plan.skippedFeatures).containsAtLeast(
            "forceAirplaneMode",
            "forceBlockVolumeKeys",
            "forceDisableWifi",
            "forceDisableBluetooth",
            "forceDisableMobileData",
            "forceWifiHotspot",
            "forceMaxPerformance",
            "forceBlackScreen"
        )
    }

    @Test
    fun `normalize black tech plan keeps supported media actions`() {
        val config = BlackTechConfig(
            enabled = true,
            forceFlashlight = true,
            flashlightStrobeMode = true,
            forceMaxVibration = true,
            forceBlockVolumeKeys = true,
            forceBlockTouch = true
        )

        val capabilities = ForcedRunHardwareController.RuntimeCapabilityMatrix(
            hasAccessibilityService = true,
            canWriteSystemSettings = false,
            hasJavaFlashlight = true,
            hasJavaVibrator = true,
            hasNativeFlashlight = false,
            hasNativeVibrator = false,
            hasNativeBrightness = false,
            hasNativeCpuGovernor = false,
            canControlWifi = false,
            canControlBluetooth = false,
            canControlMobileData = false,
            canUseLegacyHotspot = false,
            canUseSharedHotspot = false,
            canUseLocalOnlyHotspot = false,
            canKeepScreenAwake = false,
            hasActivityBinding = false
        )

        val plan = ForcedRunHardwareController.normalizeBlackTechConfigForRuntime(config, capabilities)

        assertThat(plan.normalizedConfig.enabled).isTrue()
        assertThat(plan.normalizedConfig.forceFlashlight).isTrue()
        assertThat(plan.normalizedConfig.flashlightStrobeMode).isTrue()
        assertThat(plan.normalizedConfig.forceMaxVibration).isTrue()
        assertThat(plan.normalizedConfig.forceBlockVolumeKeys).isTrue()
        assertThat(plan.normalizedConfig.forceBlockTouch).isTrue()
        assertThat(plan.skippedFeatures).isEmpty()
    }

    @Test
    fun `normalize black tech plan disables hotspot when only local only hotspot is available`() {
        val config = BlackTechConfig(
            enabled = true,
            forceWifiHotspot = true,
            forceFlashlight = true
        )

        val capabilities = ForcedRunHardwareController.RuntimeCapabilityMatrix(
            hasAccessibilityService = false,
            canWriteSystemSettings = false,
            hasJavaFlashlight = true,
            hasJavaVibrator = false,
            hasNativeFlashlight = false,
            hasNativeVibrator = false,
            hasNativeBrightness = false,
            hasNativeCpuGovernor = false,
            canControlWifi = false,
            canControlBluetooth = false,
            canControlMobileData = false,
            canUseLegacyHotspot = false,
            canUseSharedHotspot = false,
            canUseLocalOnlyHotspot = true,
            canKeepScreenAwake = false,
            hasActivityBinding = false
        )

        val plan = ForcedRunHardwareController.normalizeBlackTechConfigForRuntime(config, capabilities)

        assertThat(plan.normalizedConfig.enabled).isTrue()
        assertThat(plan.normalizedConfig.forceWifiHotspot).isFalse()
        assertThat(plan.normalizedConfig.forceFlashlight).isTrue()
        assertThat(plan.skippedFeatures).contains("forceWifiHotspot")
    }

    @Test
    fun `normalize black tech plan disables screen awake without wake lock or activity binding`() {
        val config = BlackTechConfig(
            enabled = true,
            forceScreenAwake = true,
            forceFlashlight = true
        )

        val capabilities = ForcedRunHardwareController.RuntimeCapabilityMatrix(
            hasAccessibilityService = false,
            canWriteSystemSettings = false,
            hasJavaFlashlight = true,
            hasJavaVibrator = false,
            hasNativeFlashlight = false,
            hasNativeVibrator = false,
            hasNativeBrightness = false,
            hasNativeCpuGovernor = false,
            canControlWifi = false,
            canControlBluetooth = false,
            canControlMobileData = false,
            canUseLegacyHotspot = false,
            canUseSharedHotspot = false,
            canUseLocalOnlyHotspot = false,
            canKeepScreenAwake = false,
            hasActivityBinding = false
        )

        val plan = ForcedRunHardwareController.normalizeBlackTechConfigForRuntime(config, capabilities)

        assertThat(plan.normalizedConfig.enabled).isTrue()
        assertThat(plan.normalizedConfig.forceScreenAwake).isFalse()
        assertThat(plan.normalizedConfig.forceFlashlight).isTrue()
        assertThat(plan.skippedFeatures).contains("forceScreenAwake")
    }
}
