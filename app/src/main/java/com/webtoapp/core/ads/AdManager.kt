package com.webtoapp.core.ads

import android.content.Context
import android.util.Log
import com.webtoapp.data.model.AdConfig

/**
 * Ad manager placeholder for wiring platform SDKs.
 *
 * Full integration needs:
 * 1. Add the SDK dependency in build.gradle
 * 2. Add the ad configuration in AndroidManifest.xml
 * 3. Implement initialization and display for each platform
 *
 * Placeholder hooks preserved for:
 * - Google AdMob
 * - Pangle (ByteDance)
 * - Tencent Ad Exchange
 * - Kuaishou Alliance
 */
class AdManager(private val context: Context) {

    private var isInitialized = false
    private var adConfig: AdConfig? = null

    /**
     * Initialize ad SDKs
     */
    fun initialize(config: AdConfig) {
        if (isInitialized) return
        adConfig = config

        // Ad SDK wiring goes here per selected platform.
        // Example: MobileAds.initialize(context) {}
        Log.d(TAG, "AdManager initialized with config: banner=${config.bannerEnabled}, interstitial=${config.interstitialEnabled}, splash=${config.splashEnabled}")

        isInitialized = true
    }

    /**
     * Show banner ad
     */
    fun showBannerAd(container: android.view.ViewGroup) {
        val config = adConfig ?: return
        if (!config.bannerEnabled || config.bannerId.isBlank()) return

        // Ad SDK not integrated, skip banner display
        // Example:
        // val adView = AdView(context)
        // adView.adSize = AdSize.BANNER
        // adView.adUnitId = config.bannerId
        // container.addView(adView)
        // adView.loadAd(AdRequest.Builder().build())
        Log.d(TAG, "showBannerAd called but ad SDK not integrated, bannerId=${config.bannerId}")
    }

    /**
     * Load interstitial ad
     */
    fun loadInterstitialAd(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        val config = adConfig ?: return
        if (!config.interstitialEnabled || config.interstitialId.isBlank()) {
            onFailed("插屏广告未配置")
            return
        }

        // Ad SDK not integrated, skip loading interstitials
        // Example:
        // InterstitialAd.load(context, config.interstitialId, AdRequest.Builder().build(),
        //     object : InterstitialAdLoadCallback() {
        //         override fun onAdLoaded(ad: InterstitialAd) { onLoaded() }
        //         override fun onAdFailedToLoad(error: LoadAdError) { onFailed(error.message) }
        //     })
        Log.d(TAG, "loadInterstitialAd called but ad SDK not integrated")
        onFailed("广告 SDK 未集成")
    }

    /**
     * Show interstitial ad
     */
    fun showInterstitialAd(activity: android.app.Activity, onDismissed: () -> Unit) {
        // Ad SDK not integrated, callback immediately
        // Example:
        // interstitialAd?.show(activity)
        // interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
        //     override fun onAdDismissedFullScreenContent() { onDismissed() }
        // }
        Log.d(TAG, "showInterstitialAd called but ad SDK not integrated")

        // Temporarily invoke the callback immediately
        onDismissed()
    }

    /**
     * Show splash ad
     */
    fun showSplashAd(
        activity: android.app.Activity,
        container: android.view.ViewGroup,
        onFinished: () -> Unit,
        onSkipped: () -> Unit
    ) {
        val config = adConfig ?: run {
            onFinished()
            return
        }

        if (!config.splashEnabled || config.splashId.isBlank()) {
            onFinished()
            return
        }

        // Ad SDK not integrated, simulate delay before callback
        // Splash ads typically require:
        // 1. Create the ad container view
        // 2. Load the ad
        // 3. Set up a timeout
        // 4. Handle the close callback
        Log.d(TAG, "showSplashAd called but ad SDK not integrated, duration=${config.splashDuration}s")

        // Simulate the delayed callback for now
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onFinished()
        }, (config.splashDuration * 1000).toLong())
    }

    /**
     * Release resources
     */
    fun destroy() {
        Log.d(TAG, "AdManager destroyed")
        adConfig = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "AdManager"
    }

    /**
     * Check if the requested ad type is ready
     */
    fun isAdReady(adType: AdType): Boolean {
        val config = adConfig ?: return false
        return when (adType) {
            AdType.BANNER -> config.bannerEnabled && config.bannerId.isNotBlank()
            AdType.INTERSTITIAL -> config.interstitialEnabled && config.interstitialId.isNotBlank()
            AdType.SPLASH -> config.splashEnabled && config.splashId.isNotBlank()
        }
    }
}

/**
 * Ad types
 */
enum class AdType {
    BANNER,
    INTERSTITIAL,
    SPLASH
}

/**
 * Ad callback interface
 */
interface AdCallback {
    fun onAdLoaded()
    fun onAdFailed(error: String)
    fun onAdClicked()
    fun onAdClosed()
}
