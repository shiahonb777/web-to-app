package com.webtoapp.core.engine.shields

import com.webtoapp.core.i18n.Strings

/**
 * Cookie.
 *
 * system usage.
 */
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

/**
 * Shields Referrer.
 *
 * system usage.
 */
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

/**
 * Note.
 *
 * system usage.
 */
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

/**
 * error.
 *
 * signature error.
 */
enum class SslErrorPolicy {
    /** compat fallback auto. */
    AUTO_HTTP_FALLBACK,
    /** user. */
    ASK_USER,
    /** security. */
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

/**
 * config.
 * protection.
 */
data class ShieldsConfig(
    /** Note. */
    val enabled: Boolean = true,
    
    /** default auto. */
    val httpsUpgrade: Boolean = false,

    /** default error compat. */
    val sslErrorPolicy: SslErrorPolicy = SslErrorPolicy.AUTO_HTTP_FALLBACK,

    /** blocker. */
    val trackerBlocking: Boolean = true,
    
    /** auto. */
    val cookieConsentBlock: Boolean = true,
    
    /** Global Privacy Control. */
    val gpcEnabled: Boolean = true,
    
    /** Cookie. */
    val thirdPartyCookiePolicy: ThirdPartyCookiePolicy = ThirdPartyCookiePolicy.BLOCK_CROSS_SITE,
    
    /** Referrer. */
    val referrerPolicy: ShieldsReferrerPolicy = ShieldsReferrerPolicy.STRICT_ORIGIN_CROSS,
    
    /** Note. */
    val readerModeEnabled: Boolean = true
) {
    companion object {
        /** config default. */
        val DEFAULT = ShieldsConfig()
        
        /** Note. */
        val DISABLED = ShieldsConfig(enabled = false)
        
        /** default. */
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