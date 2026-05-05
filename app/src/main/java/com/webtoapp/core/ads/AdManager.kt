package com.webtoapp.core.ads

import android.content.Context
import android.util.Log
import com.webtoapp.data.model.AdConfig















class AdManager(private val context: Context) {

    private var isInitialized = false
    private var adConfig: AdConfig? = null




    fun initialize(config: AdConfig) {
        if (isInitialized) return
        adConfig = config




        Log.d(TAG, "AdManager initialized with config: banner=${config.bannerEnabled}, interstitial=${config.interstitialEnabled}, splash=${config.splashEnabled}")

        isInitialized = true
    }




    fun showBannerAd(container: android.view.ViewGroup) {
        val config = adConfig ?: return
        if (!config.bannerEnabled || config.bannerId.isBlank()) return








        Log.d(TAG, "showBannerAd called but ad SDK not integrated, bannerId=${config.bannerId}")
    }




    fun loadInterstitialAd(onLoaded: () -> Unit, onFailed: (String) -> Unit) {
        val config = adConfig ?: return
        if (!config.interstitialEnabled || config.interstitialId.isBlank()) {
            onFailed("插屏广告未配置")
            return
        }








        Log.d(TAG, "loadInterstitialAd called but ad SDK not integrated")
        onFailed("广告 SDK 未集成")
    }




    fun showInterstitialAd(activity: android.app.Activity, onDismissed: () -> Unit) {






        Log.d(TAG, "showInterstitialAd called but ad SDK not integrated")


        onDismissed()
    }




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







        Log.d(TAG, "showSplashAd called but ad SDK not integrated, duration=${config.splashDuration}s")


        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            onFinished()
        }, (config.splashDuration * 1000).toLong())
    }




    fun destroy() {
        Log.d(TAG, "AdManager destroyed")
        adConfig = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "AdManager"
    }




    fun isAdReady(adType: AdType): Boolean {
        val config = adConfig ?: return false
        return when (adType) {
            AdType.BANNER -> config.bannerEnabled && config.bannerId.isNotBlank()
            AdType.INTERSTITIAL -> config.interstitialEnabled && config.interstitialId.isNotBlank()
            AdType.SPLASH -> config.splashEnabled && config.splashId.isNotBlank()
        }
    }
}




enum class AdType {
    BANNER,
    INTERSTITIAL,
    SPLASH
}




interface AdCallback {
    fun onAdLoaded()
    fun onAdFailed(error: String)
    fun onAdClicked()
    fun onAdClosed()
}
