package com.webtoapp.core.engine.shields

import com.webtoapp.core.i18n.Strings






enum class ThirdPartyCookiePolicy {
    ALLOW_ALL,
    BLOCK_CROSS_SITE,
    BLOCK_ALL_THIRD_PARTY;

    val displayName: String get() = when (this) {
        ALLOW_ALL -> Strings.shieldsCookieAllowAll
        BLOCK_CROSS_SITE -> Strings.shieldsCookieBlockCrossSite
        BLOCK_ALL_THIRD_PARTY -> Strings.shieldsCookieBlockAllThirdParty
    }
}






enum class ShieldsReferrerPolicy(val value: String) {
    NO_REFERRER("no-referrer"),
    ORIGIN("origin"),
    STRICT_ORIGIN_CROSS("strict-origin-when-cross-origin"),
    SAME_ORIGIN("same-origin"),
    UNSAFE_URL("unsafe-url");

    val displayName: String get() = when (this) {
        NO_REFERRER -> Strings.shieldsRefNoReferrer
        ORIGIN -> Strings.shieldsRefOrigin
        STRICT_ORIGIN_CROSS -> Strings.shieldsRefStrictOriginCross
        SAME_ORIGIN -> Strings.shieldsRefSameOrigin
        UNSAFE_URL -> Strings.shieldsRefUnsafeUrl
    }
}






enum class TrackerCategory {
    ANALYTICS,
    SOCIAL,
    FINGERPRINTING,
    CRYPTOMINING,
    AD_NETWORK;

    val displayName: String get() = when (this) {
        ANALYTICS -> Strings.shieldsTrackerAnalytics
        SOCIAL -> Strings.shieldsTrackerSocial
        FINGERPRINTING -> Strings.shieldsTrackerFingerprinting
        CRYPTOMINING -> Strings.shieldsTrackerCryptomining
        AD_NETWORK -> Strings.shieldsTrackerAdNetwork
    }
}






enum class SslErrorPolicy {

    AUTO_HTTP_FALLBACK,

    ASK_USER,

    BLOCK;

    val displayName: String get() = when (this) {
        AUTO_HTTP_FALLBACK -> Strings.sslErrorPolicyAutoFallback
        ASK_USER -> Strings.sslErrorPolicyAskUser
        BLOCK -> Strings.sslErrorPolicyBlock
    }

    val description: String get() = when (this) {
        AUTO_HTTP_FALLBACK -> Strings.sslErrorPolicyAutoFallbackDesc
        ASK_USER -> Strings.sslErrorPolicyAskUserDesc
        BLOCK -> Strings.sslErrorPolicyBlockDesc
    }
}





data class ShieldsConfig(

    val enabled: Boolean = true,


    val httpsUpgrade: Boolean = false,


    val sslErrorPolicy: SslErrorPolicy = SslErrorPolicy.AUTO_HTTP_FALLBACK,


    val trackerBlocking: Boolean = true,


    val cookieConsentBlock: Boolean = false,


    val gpcEnabled: Boolean = false,


    val thirdPartyCookiePolicy: ThirdPartyCookiePolicy = ThirdPartyCookiePolicy.ALLOW_ALL,


    val referrerPolicy: ShieldsReferrerPolicy = ShieldsReferrerPolicy.ORIGIN,


    val readerModeEnabled: Boolean = true
) {
    companion object {

        val DEFAULT = ShieldsConfig()


        val DISABLED = ShieldsConfig(enabled = false)


        val MAXIMUM = ShieldsConfig(
            enabled = true,
            httpsUpgrade = false,
            sslErrorPolicy = SslErrorPolicy.AUTO_HTTP_FALLBACK,
            trackerBlocking = true,
            cookieConsentBlock = true,
            gpcEnabled = true,
            thirdPartyCookiePolicy = ThirdPartyCookiePolicy.BLOCK_ALL_THIRD_PARTY,
            referrerPolicy = ShieldsReferrerPolicy.NO_REFERRER,
            readerModeEnabled = true
        )
    }
}
