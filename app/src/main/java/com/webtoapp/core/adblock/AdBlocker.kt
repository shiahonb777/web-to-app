package com.webtoapp.core.adblock

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ad Blocker — production-grade ABP/uBlock-compatible filter engine
 *
 * Supports:
 * - ABP / EasyList / AdGuard network filter syntax (||, @@, $modifiers)
 * - Cosmetic element hiding (## / #@#)
 * - Resource-type–aware blocking ($script, $image, $stylesheet, …)
 * - $third-party / $first-party / $domain modifiers
 * - Exception rules (@@) to prevent page breakage
 * - Anti-anti-adblock scriptlet injection (abort-on-property-read, etc.)
 * - Comprehensive safelist for critical infrastructure domains
 * - Hosts file import (standard, AdGuard DNS, ABP)
 */
class AdBlocker {

    // ==================== Resource type enum ====================
    enum class ResourceType {
        DOCUMENT, SUBDOCUMENT, SCRIPT, STYLESHEET, IMAGE, FONT,
        XMLHTTPREQUEST, MEDIA, WEBSOCKET, OBJECT, PING, OTHER;

        companion object {
            fun fromWebViewType(type: String): ResourceType = when (type) {
                "main_frame" -> DOCUMENT
                "sub_frame" -> SUBDOCUMENT
                "script" -> SCRIPT
                "stylesheet" -> STYLESHEET
                "image" -> IMAGE
                "font" -> FONT
                "xmlhttprequest" -> XMLHTTPREQUEST
                "media" -> MEDIA
                "websocket" -> WEBSOCKET
                "object" -> OBJECT
                "ping" -> PING
                else -> OTHER
            }
        }
    }

    // ==================== Parsed filter data classes ====================
    /**
     * A parsed network filter rule.
     * Compiled once, matched many times — hot path is simple string/set operations.
     */
    private data class NetworkFilter(
        val pattern: String,
        val regex: Regex?,
        val isException: Boolean,
        val matchCase: Boolean,
        // Domain constraints: block only on these domains (empty = all)
        val domains: Set<String>,
        val excludedDomains: Set<String>,
        // Resource type constraints
        val allowedTypes: Set<ResourceType>?,   // null = all types
        val excludedTypes: Set<ResourceType>,
        // Party constraints
        val thirdPartyOnly: Boolean,
        val firstPartyOnly: Boolean,
        // Raw host anchor for fast O(1) pre-check (||domain^ → "domain")
        val anchorDomain: String?
    )

    /**
     * A parsed cosmetic filter (element hiding rule).
     */
    data class CosmeticFilter(
        val selector: String,
        val isException: Boolean,
        val domains: Set<String>,       // apply only on these domains (empty = all)
        val excludedDomains: Set<String>
    )

    companion object {
        private val IP_ADDRESS_REGEX = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        private val HOST_EXTRACT_REGEX = Regex("^(?:https?://)?([^/:]+)")
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val ABP_SEPARATOR_REGEX = Regex("[^\\w%.\\-]")

        // Critical infrastructure — NEVER block these or pages will break
        private val SAFELIST_HOSTS = setOf(
            // Translation
            "translate.googleapis.com", "translate.google.com", "translation.googleapis.com",
            // CDNs serving first-party content / JS libraries
            "cdn.jsdelivr.net", "cdnjs.cloudflare.com", "unpkg.com",
            "ajax.googleapis.com", "fonts.googleapis.com", "fonts.gstatic.com",
            "code.jquery.com", "cdn.bootcdn.net", "cdn.bootcss.com",
            "cdn.staticfile.org", "lib.baomitu.com",
            "cdn.npmmirror.com", "registry.npmmirror.com",
            "stackpath.bootstrapcdn.com", "maxcdn.bootstrapcdn.com",
            "cdn.tailwindcss.com",
            // Cloud / API services — essential for SaaS apps
            "firebaseapp.com", "firebaseio.com", "firebase.googleapis.com",
            "cloudfunctions.net", "cloudflare.com", "workers.dev",
            "vercel.app", "netlify.app", "pages.dev",
            "amazonaws.com", "cloudfront.net",
            "azureedge.net", "azurewebsites.net",
            // Login / OAuth — all providers from OAuthCompatEngine
            "accounts.google.com", "accounts.youtube.com", "myaccount.google.com",
            "login.microsoftonline.com", "login.live.com", "login.windows.net", "login.microsoft.com",
            "appleid.apple.com",
            "www.facebook.com", "m.facebook.com", "web.facebook.com",
            "api.twitter.com", "twitter.com", "api.x.com", "x.com",
            "github.com", "api.github.com",
            "discord.com", "discordapp.com",
            "access.line.me", "liff.line.me",
            "accounts.kakao.com", "kauth.kakao.com",
            "login.yahoo.com", "api.login.yahoo.com",
            "www.linkedin.com", "linkedin.com",
            "accounts.spotify.com",
            "id.twitch.tv",
            // Payment
            "js.stripe.com", "checkout.stripe.com", "www.paypal.com",
            "pay.google.com", "applepay.cdn-apple.com",
            "alipay.com", "mapi.alipay.com",
            // reCAPTCHA / hCaptcha / Cloudflare Turnstile
            "www.google.com", "www.gstatic.com",
            "hcaptcha.com", "js.hcaptcha.com",
            "challenges.cloudflare.com",
            "recaptcha.net", "www.recaptcha.net",
            // Map tiles / Geo
            "maps.googleapis.com", "maps.gstatic.com",
            "api.mapbox.com", "tiles.mapbox.com",
            "tile.openstreetmap.org", "api.amap.com", "webapi.amap.com",
            "api.map.baidu.com", "maponline0.bdimg.com",
            // Essential media / streaming
            "i.ytimg.com", "www.youtube.com", "youtube.com",
            "player.vimeo.com",
            // Error monitoring (essential for app developers)
            "sentry.io", "browser.sentry-cdn.com",
            "bugsnag.com", "sessions.bugsnag.com",
            // Push / Notification services
            "onesignal.com", "cdn.onesignal.com",
            "fcm.googleapis.com", "mtalk.google.com",
            // Shopping / E-commerce essentials
            "www.taobao.com", "www.tmall.com", "www.jd.com",
            "www.amazon.com", "www.ebay.com",
            // WebSocket / Real-time services
            "wss.pusher.com", "realtime-cloud.ably.io",
            "sockjs.pusher.com"
        )

        // Popular hosts file / filter list sources — International focus
        fun getPopularHostsSources() = listOf(
            // ── Core (enabled by default for all users) ──
            HostsSource(
                name = "EasyList",
                url = "https://easylist.to/easylist/easylist.txt",
                description = "Primary international ad-blocking filter list, maintained by the AdBlock community"
            ),
            HostsSource(
                name = "EasyPrivacy",
                url = "https://easylist.to/easylist/easyprivacy.txt",
                description = "Comprehensive tracker and analytics blocking for privacy protection"
            ),
            HostsSource(
                name = "uBlock Filters",
                url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/filters.txt",
                description = "uBlock Origin default filters — advanced rules for modern ad tech"
            ),
            HostsSource(
                name = "uBlock Privacy",
                url = "https://raw.githubusercontent.com/uBlockOrigin/uAssets/master/filters/privacy.txt",
                description = "uBlock Origin privacy-specific filters"
            ),
            HostsSource(
                name = "AdGuard Base",
                url = "https://filters.adtidy.org/extension/ublock/filters/2.txt",
                description = "AdGuard Base filter — broad international ad blocking"
            ),
            HostsSource(
                name = "AdGuard Tracking Protection",
                url = "https://filters.adtidy.org/extension/ublock/filters/3.txt",
                description = "AdGuard Tracking Protection filter — analytics & telemetry"
            ),
            HostsSource(
                name = "AdGuard Annoyances",
                url = "https://filters.adtidy.org/extension/ublock/filters/14.txt",
                description = "Blocks cookie notices, newsletter popups, and other annoyances"
            ),
            // ── Host-level lists ──
            HostsSource(
                name = "StevenBlack Hosts",
                url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                description = com.webtoapp.core.i18n.Strings.hostsStevenBlackDesc
            ),
            HostsSource(
                name = "AdAway Default",
                url = "https://adaway.org/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAdAwayDesc
            ),
            HostsSource(
                name = "Peter Lowe's List",
                url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0",
                description = "Curated ad & tracking server blocklist by Peter Lowe"
            ),
            HostsSource(
                name = "1Hosts Lite",
                url = "https://o0.pages.dev/Lite/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hosts1HostsLiteDesc
            ),
            // ── Regional lists (Europe) ──
            HostsSource(
                name = "EasyList Germany",
                url = "https://easylist.to/easylistgermany/easylistgermany.txt",
                description = "German websites — blocks ads on DE/AT/CH domains"
            ),
            HostsSource(
                name = "Liste FR (France)",
                url = "https://easylist-downloads.adblockplus.org/liste_fr.txt",
                description = "French websites — comprehensive ad blocking for FR domains"
            ),
            HostsSource(
                name = "EasyList Dutch",
                url = "https://easylist-downloads.adblockplus.org/easylistdutch.txt",
                description = "Dutch websites — blocks ads on NL/BE domains"
            ),
            HostsSource(
                name = "EasyList Italy",
                url = "https://easylist-downloads.adblockplus.org/easylistitaly.txt",
                description = "Italian websites — blocks ads on IT domains"
            ),
            HostsSource(
                name = "EasyList Spanish",
                url = "https://easylist-downloads.adblockplus.org/easylistspanish.txt",
                description = "Spanish websites — blocks ads on ES/LATAM domains"
            ),
            HostsSource(
                name = "EasyList Portuguese",
                url = "https://easylist-downloads.adblockplus.org/easylistportuguese.txt",
                description = "Portuguese/Brazilian websites — blocks ads on PT/BR domains"
            ),
            // ── Regional lists (Asia-Pacific) ──
            HostsSource(
                name = "AdGuard Japanese",
                url = "https://filters.adtidy.org/extension/ublock/filters/7.txt",
                description = "Japanese websites — blocks ads on JP domains & Japanese ad networks"
            ),
            HostsSource(
                name = "List-KR (Korea)",
                url = "https://raw.githubusercontent.com/ADBFilter/KoreanAdblockList/main/koreanlist.txt",
                description = "Korean websites — blocks ads on KR domains & Korean ad networks"
            ),
            HostsSource(
                name = "ABP Indonesia",
                url = "https://raw.githubusercontent.com/nicemayi/nicemayi-abp/master/nicemayi.txt",
                description = "Indonesian websites — regional ad network and popup blocking"
            ),
            HostsSource(
                name = "EasyList China",
                url = "https://easylist-downloads.adblockplus.org/easylistchina.txt",
                description = "Chinese websites — blocks ads on CN domains"
            ),
            HostsSource(
                name = "AdGuard DNS Filter",
                url = "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAdGuardDesc
            ),
            HostsSource(
                name = "Anti-AD",
                url = "https://anti-ad.net/hosts.txt",
                description = com.webtoapp.core.i18n.Strings.hostsAntiADDesc
            )
        )

        @Deprecated("Use getPopularHostsSources() instead for i18n support")
        val POPULAR_HOSTS_SOURCES get() = getPopularHostsSources()

        // ==================== Comprehensive default ad/tracker domains ====================
        private val DEFAULT_AD_HOSTS = setOf(
            // Google Ads
            "googleadservices.com", "googlesyndication.com", "doubleclick.net",
            "googletagmanager.com", "googletagservices.com",
            "pagead2.googlesyndication.com", "adservice.google.com",
            "afs.googlesyndication.com", "partner.googleadservices.com",
            // Facebook / Meta
            "an.facebook.com", "pixel.facebook.com",
            // Major ad exchanges
            "adnxs.com", "advertising.com", "taboola.com", "outbrain.com",
            "criteo.com", "criteo.net", "pubmatic.com", "rubiconproject.com",
            "casalemedia.com", "openx.net", "bidswitch.net", "adsrvr.org",
            "sharethrough.com", "33across.com", "media.net",
            "amazon-adsystem.com", "serving-sys.com", "smartadserver.com",
            "contextweb.com", "yieldmo.com", "sovrn.com",
            "liadm.com", "lijit.com", "revcontent.com",
            "zedo.com", "undertone.com", "conversantmedia.com",
            "indexexchange.com", "spotxchange.com", "districtm.io",
            "triplelift.com", "gumgum.com", "nativo.com",
            // Programmatic / header bidding
            "prebid.org", "id5-sync.com", "liveintent.com",
            "intentiq.com", "33across.com",
            "kargo.com", "brightcom.com", "nextmillennium.io",
            "emxdgt.com", "rhythmone.com", "improvedigital.com",
            // Video ad platforms
            "springserve.com", "spotx.tv", "teads.tv",
            "viralize.com", "unruly.co", "connatix.com",
            "vidoomy.com", "primis.tech",
            // Tracking / analytics (ad-serving related)
            "moatads.com", "doubleverify.com", "adsafeprotected.com",
            "scorecardresearch.com", "imrworldwide.com", "quantserve.com",
            "demdex.net", "krxd.net", "exelator.com", "bluekai.com",
            "eyeota.net", "rlcdn.com", "pippio.com",
            "addthis.com", "sharethis.com",
            // Pop-ups / redirectors
            "popads.net", "popcash.net", "propellerads.com",
            "clickadu.com", "trafficjunky.com", "exoclick.com",
            "juicyads.com", "plugrush.com", "hilltopads.com",
            "a-ads.com", "admaven.com", "evadav.com",
            "richpush.com", "pushground.com", "megapush.com",
            // Chinese ad networks
            "cpro.baidu.com", "pos.baidu.com", "cbjs.baidu.com",
            "eclick.baidu.com", "hm.baidu.com",
            "tanx.com", "alimama.com", "mmstat.com",
            "cnzz.com", "51.la",
            "union.sogou.com", "js.sogou.com",
            "c.360.cn", "g.360.cn",
            "s.360.cn", "stat.360.cn",
            "miaozhen.com", "admaster.com.cn",
            "gridsumdissector.com",
            "mediav.com", "ipinyou.com",
            "yigao.com", "adview.cn",
            "admon.cn", "allyes.com",
            "admaimai.com", "adsame.com",
            "aduu.cn", "baidustatic.com",
            "e.qq.com", "gdt.qq.com", "mi.gdt.qq.com",
            "biddingx.com", "dsp.baidu.com",
            // Japanese ad networks
            "i-mobile.co.jp", "microad.net", "adlantis.jp",
            "adjust.com", "ad-stir.com", "impact-ad.jp",
            "fluct.jp", "geniee.co.jp", "logly.co.jp",
            // Korean ad networks
            "adcreative.naver.com", "dn.adpnut.com",
            "ad.daum.net", "adfurikun.jp",
            "cauly.co.kr", "mobon.net",
            // Indian / Southeast Asian ad networks
            "vserv.com", "inmobi.com", "madgicx.com",
            "adcolony.com", "vungle.com", "ironsource.com",
            "applovin.com", "unity3d.com",
            // European ad networks
            "adform.net", "adform.com", "serving.adform.net",
            "smartadserver.com", "www6.smartadserver.com", "ww392.smartadserver.com",
            "teads.tv", "a.teads.tv", "cdn.teads.tv",
            "adtrue.com", "strossle.com", "plista.com",
            "meetrics.net", "adition.com", "yieldlab.net",
            "ligatus.com", "audience.network",
            // Turkish ad networks
            "reklamstore.com", "hurriyet.com.tr/reklam",
            "ilanjet.com", "adnow.com",
            // Russian ad networks
            "yandex-adv.com", "an.yandex.ru", "awaps.yandex.ru",
            "adfox.yandex.ru", "bs.yandex.ru",
            "mc.yandex.ru", "adfox.ru",
            // Latin American ad networks
            "publicidade.uol.com.br", "bol.uol.com.br",
            "publitas.com", "adsmovil.com",
            // Southeast Asian ad specific
            "adtima.vn", "admicro.vn",
            "accesstrade.vn", "eclick.vn",
            // Mobile SDK ad networks (global)
            "ads.mopub.com", "mopub.com",
            "fyber.com", "inner-active.mobi",
            "startapp.com", "is.com",
            "ads.inmobi.com", "config.inmobi.com",
            "tapjoy.com", "ads.tapjoy.com",
            "chartboost.com", "live.chartboost.com",
            "ads.unity3d.com", "unityads.unity3d.com",
            "pangle.io", "pangleglobal.biz",
            "liftoff.io", "smaato.net",
            "mobilefuse.com", "verve.com",
            // Generic ad-serving patterns
            "revive-adserver.com", "adzerk.net", "buysellads.com"
        )

        // ==================== Built-in ABP Network Filter Rules ====================
        // These fire immediately without downloading external lists.
        // Curated for international coverage (EN/EU/JP/KR/SEA markets).
        private val DEFAULT_NETWORK_RULES = listOf(
            // ── Google Ads / DoubleClick ──
            "||pagead2.googlesyndication.com^",
            "||adservice.google.com^",
            "||www.googleadservices.com/pagead/conversion*",
            "||googleads.g.doubleclick.net^",
            "||securepubads.g.doubleclick.net^",
            "||stats.g.doubleclick.net^",
            "||cm.g.doubleclick.net^",
            "||ad.doubleclick.net^",
            "||static.doubleclick.net/instream/ad_status.js",
            "||fundingchoicesmessages.google.com^",
            // ── Meta / Facebook Ads ──
            "||an.facebook.com^",
            "||pixel.facebook.com^",
            "||tr.facebook.com^",
            "||ad.atdmt.com^",
            "||connect.facebook.net/*/fbevents.js\$script",
            // ── Amazon Ads ──
            "||s.amazon-adsystem.com^",
            "||aax.amazon-adsystem.com^",
            "||fls-na.amazon.com^",
            "||unagi.amazon.com/1/events^",
            // ── Major SSPs & Exchanges ──
            "||ib.adnxs.com^",
            "||prebid.adnxs.com^",
            "||ads.pubmatic.com^",
            "||hbopenbid.pubmatic.com^",
            "||fastlane.rubiconproject.com^",
            "||optimized-by.rubiconproject.com^",
            "||u.openx.net^",
            "||rtb.openx.net^",
            "||htlb.casalemedia.com^",
            "||eb2.3lift.com^",
            "||tlx.3lift.com^",
            "||static.criteo.net/js^",
            "||bidder.criteo.com^",
            "||dis.criteo.com^",
            "||gum.criteo.com^",
            "||ssp.yahoo.com^",
            "||ads.yahoo.com^",
            // ── Native / Content Recommendation ──
            "||cdn.taboola.com/libtrc^",
            "||trc.taboola.com^",
            "||widgets.outbrain.com/outbrain.js",
            "||log.outbrain.com^",
            "||cdn.revcontent.com^",
            "||a.sharethrough.com^",
            "||nativo.com/ad^",
            "||assets.nativo.com^",
            "||ads.yieldmo.com^",
            // ── Pop-under / Aggressive Ad Networks ──
            "||propellerads.com^",
            "||cdn.propellerads.com^",
            "||popads.net^",
            "||c.popads.net^",
            "||serve.popads.net^",
            "||popcash.net^",
            "||clickadu.com^",
            "||s.clickadu.com^",
            "||exoclick.com^",
            "||syndication.exoclick.com^",
            "||juicyads.com^",
            "||ads.exosrv.com^",
            "||hilltopads.com^",
            "||a-ads.com^",
            "||ad.admaven.com^",
            "||richpush.com^",
            "||evadav.com^",
            // ── Video Ads ──
            "||imasdk.googleapis.com/js/sdkloader/ima3.js",
            "||vid.springserve.com^",
            "||ads.stickyadstv.com^",
            "||delivery.vidible.tv^",
            "||a.teads.tv^",
            "||cdn.teads.tv^",
            "||s.innovid.com^",
            // ── Ad Verification / Viewability (3rd-party tracking) ──
            "||pixel.moatads.com^",
            "||z.moatads.com^",
            "||cdn.doubleverify.com/dvbs_src.js",
            "||tps.doubleverify.com^",
            "||static.adsafeprotected.com^",
            "||pixel.adsafeprotected.com^",
            "||sb.scorecardresearch.com^",
            "||b.scorecardresearch.com^",
            "||pixel.quantserve.com^",
            "||imrworldwide.com/cgi-bin/m^",
            // ── Data Brokers / Audience Targeting ──
            "||dpm.demdex.net^",
            "||tags.bluekai.com^",
            "||stags.bluekai.com^",
            "||cdn.krxd.net^",
            "||beacon.krxd.net^",
            "||match.adsrvr.org^",
            "||id5-sync.com^",
            "||rlcdn.com^",
            "||pippio.com^",
            "||hb.yahoo.net^",
            // ── Cookie Consent / GDPR Banners ──
            // Exception: allow CMP frameworks to load but hide their UI via cosmetic rules
            "||cdn.cookielaw.org^\$third-party",
            "||consent.cookiebot.com^\$third-party",
            "||consent.cookiefirst.com^\$third-party",
            "||cdn.consentmanager.net^\$third-party",
            // ── Newsletter / Subscription Popups ──
            "||cdn.optinmonster.com^",
            "||api.convertkit.com/forms^\$third-party",
            "||js.hsforms.net^\$third-party",
            "||js.hscollectedforms.net^\$third-party",
            "||sumo.com/sumo.min.js\$third-party",
            // ── Anti-adblock bypass (common detectors) ──
            "||btloader.com^",
            "||fundingchoicesmessages.google.com^",
            "||pagead2.googlesyndication.com/pagead/managed/js/ump/",
            // ── Mobile Specific ──
            "||sdk.iad-01.braze.com^",
            "||sdk.iad-03.braze.com^",
            "||logs.applovin.com^",
            "||ms.applovin.com^",
            "||ads.inmobi.com^",
            "||config.uca.cloud.unity3d.com^\$third-party",
            "||ads.mopub.com^",
            "||art.mopub.com^",
            // ── Exception rules — NEVER block these ──
            "@@||sentry.io^",
            "@@||browser.sentry-cdn.com^",
            "@@||challenges.cloudflare.com^",
            "@@||js.stripe.com^",
            "@@||checkout.stripe.com^",
            "@@||www.paypal.com^",
            "@@||pay.google.com^",
            "@@||js.hcaptcha.com^",
            "@@||www.recaptcha.net^",
            "@@||cdn.jsdelivr.net^",
            "@@||cdnjs.cloudflare.com^",
            "@@||unpkg.com^",
            "@@||fonts.googleapis.com^",
            "@@||fonts.gstatic.com^",
            "@@||accounts.google.com^",
            "@@||firebaseapp.com^",
            "@@||firebase.googleapis.com^"
        )

        // ==================== Built-in Cosmetic (Element Hiding) Rules ====================
        // Universal selectors that hide common ad containers across international sites.
        private val DEFAULT_COSMETIC_RULES = listOf(
            // ── Generic ad containers (by ID) ──
            "###ad-banner",
            "###ad-container",
            "###ad-wrapper",
            "###ad_top",
            "###ad_bottom",
            "###ad_sidebar",
            "###top-ad",
            "###bottom-ad",
            "###sidebar-ad",
            "###header-ad",
            "###footer-ad",
            "###adsense",
            "###google_ads",
            "###dfp-ad-top",
            "###dfp-ad-right",
            // ── Generic ad containers (by class) ──
            "##.ad-banner",
            "##.ad-container",
            "##.ad-wrapper",
            "##.ad-slot",
            "##.ad-unit",
            "##.ad-zone",
            "##.ad-placement",
            "##.ad-holder",
            "##.ad-block",
            "##.ad-box",
            "##.ads-container",
            "##.ads-wrapper",
            "##.adsbygoogle",
            "##.dfp-ad",
            "##.google-ad",
            "##.advertisement",
            "##.sponsored-content",
            "##.sponsored-post",
            "##.native-ad",
            "##.promoted-content",
            // ── Taboola / Outbrain / Native ──
            "##.taboola-widget",
            "##.trc_rbox",
            "##.trc_related_container",
            "##div[id^=\"taboola-\"]",
            "##.OUTBRAIN",
            "##div[data-widget-id*=\"outbrain\"]",
            "##.ob-widget",
            "##.ob-smartfeed-wrapper",
            "##.revcontent-widget",
            // ── Cookie consent / GDPR banners ──
            "##.cookie-banner",
            "##.cookie-consent",
            "##.cookie-notice",
            "##.cookie-popup",
            "##.cookie-bar",
            "##.consent-banner",
            "##.consent-popup",
            "##.gdpr-banner",
            "##.gdpr-popup",
            "##.cc-banner",
            "##.cc-window",
            "###cookie-banner",
            "###cookie-consent",
            "###cookie-notice",
            "###gdpr-banner",
            "###CybotCookiebotDialog",
            "###onetrust-consent-sdk",
            "###onetrust-banner-sdk",
            "##div[class*=\"cookie-consent\"]",
            "##div[class*=\"cookie-notice\"]",
            "##div[id*=\"cookie-law\"]",
            "##div[aria-label=\"Cookie consent\"]",
            // ── Newsletter / Subscription popups ──
            "##.newsletter-popup",
            "##.newsletter-modal",
            "##.subscribe-popup",
            "##.email-popup",
            "##.signup-popup",
            "##.exit-intent-popup",
            "##div[class*=\"newsletter-popup\"]",
            "##div[class*=\"email-signup\"]",
            // ── Interstitials / Overlays ──
            "##.modal-backdrop[style*=\"z-index\"]",
            "##.interstitial-ad",
            "##.overlay-ad",
            "##.lightbox-ad",
            "##div[class*=\"interstitial\"]",
            // ── Common data attributes ──
            "##div[data-ad]",
            "##div[data-ad-slot]",
            "##div[data-ad-unit]",
            "##div[data-google-query-id]",
            "##div[data-dfp-ad]",
            "##ins.adsbygoogle",
            "##div[data-native-ad]",
            "##div[data-sponsored]",
            // ── Social tracking widgets (hidden embeds, not visible widgets) ──
            "##img[src*=\"/pixel.gif?\"]",
            "##img[src*=\"/beacon?\"]",
            "##img[src*=\"/track?\"]",
            "##img[width=\"1\"][height=\"1\"]",
            "##img[src*=\"facebook.com/tr?\"]"
        )

        // Resource type modifier name → enum
        private val TYPE_MODIFIERS = mapOf(
            "script" to ResourceType.SCRIPT,
            "stylesheet" to ResourceType.STYLESHEET,
            "css" to ResourceType.STYLESHEET,
            "image" to ResourceType.IMAGE,
            "font" to ResourceType.FONT,
            "xmlhttprequest" to ResourceType.XMLHTTPREQUEST,
            "xhr" to ResourceType.XMLHTTPREQUEST,
            "media" to ResourceType.MEDIA,
            "websocket" to ResourceType.WEBSOCKET,
            "object" to ResourceType.OBJECT,
            "subdocument" to ResourceType.SUBDOCUMENT,
            "sub_frame" to ResourceType.SUBDOCUMENT,
            "document" to ResourceType.DOCUMENT,
            "ping" to ResourceType.PING,
            "other" to ResourceType.OTHER
        )

        // First-party ad paths — match ads served from the page's own domain
        // These paths are common across ad-serving platforms
        private val FIRST_PARTY_AD_PATH_PATTERNS = listOf(
            "/ads/", "/ad/", "/adserver/", "/adservice/",
            "/adsense/", "/admanager/", "/adx/",
            "/banner/", "/banners/",
            "/pop-up/", "/popup/", "/popunder/",
            "/sponsor/", "/sponsored/",
            "/doubleclick/", "/dfp/",
            "/prebid/", "/header-bidding/",
            "/native-ad/", "/native_ad/",
            "/interstitial/", "/overlay-ad/",
            "/ad-slot/", "/adslot/",
            "/ad_unit/", "/adunit/"
        )

        // ★ Pre-compiled regex — single-pass matching replaces 17× String.contains() calls
        private val FIRST_PARTY_AD_PATH_REGEX: Regex by lazy {
            val escaped = FIRST_PARTY_AD_PATH_PATTERNS.joinToString("|") { Regex.escape(it) }
            Regex(escaped, RegexOption.IGNORE_CASE)
        }

        // Essential resource URL patterns — NEVER block these regardless of other rules
        // Prevents breaking core page functionality
        private val ESSENTIAL_RESOURCE_PATTERNS = listOf(
            // Core framework files
            "/jquery", "/react", "/vue", "/angular",
            "/bootstrap", "/tailwind",
            "/lodash", "/underscore", "/axios",
            "/moment", "/dayjs",
            // Web component / UI libraries
            "/element-ui/", "/element-plus/", "/ant-design/", "/antd/",
            "/material-ui/", "/mui/",
            "/swiper/", "/slick/", "/owl.carousel/",
            // Essential web APIs
            "/api/", "/graphql", "/rest/",
            "/auth/", "/login", "/oauth",
            "/checkout/", "/payment/", "/pay/",
            "/cart/", "/order/",
            // Service worker / manifest
            "/sw.js", "/service-worker", "/manifest.json", "/manifest.webmanifest"
        )

        // ★ Pre-compiled regex — single-pass matching replaces 18× String.contains() calls
        private val ESSENTIAL_RESOURCE_REGEX: Regex by lazy {
            val escaped = ESSENTIAL_RESOURCE_PATTERNS.joinToString("|") { Regex.escape(it) }
            Regex(escaped, RegexOption.IGNORE_CASE)
        }

        // Preallocate the blocked response buffer to ease GC pressure.
        // Each page may block dozens of ads, so reuse the ByteArray instead of reallocating.
        val EMPTY_BYTES = ByteArray(0)
        val BLOCKED_JS_BYTES = "/* blocked */".toByteArray()
        val BLOCKED_CSS_BYTES = "/* blocked */".toByteArray()
        val BLOCKED_JSON_BYTES = "{}".toByteArray()
    }

    // ==================== Storage ====================
    // Fast domain lookup sets
    private val exactHosts = mutableSetOf<String>()
    private val hostsFileHosts = mutableSetOf<String>()
    private val enabledHostsSources = mutableSetOf<String>()

    // Parsed ABP filters — split for performance
    private val networkBlockFilters = mutableListOf<NetworkFilter>()
    private val networkExceptionFilters = mutableListOf<NetworkFilter>()
    // Anchor-domain index: domain → list of filter indices — O(1) lookup
    private val anchorDomainIndex = HashMap<String, MutableList<Int>>()
    // ★ Exception filter anchor-domain index — mirrors anchorDomainIndex for @@rules
    private val exceptionAnchorDomainIndex = HashMap<String, MutableList<Int>>()

    // ★ LRU cache — avoid recalculating shouldBlock for the same URL
    // Iframes and repeated resources are common; cache hits stay around 30-50%
    @Suppress("serial")
    private val blockResultCache = object : LinkedHashMap<Int, Boolean>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Boolean>?): Boolean = size > 512
    }

    // Cosmetic filters
    private val cosmeticBlockFilters = mutableListOf<CosmeticFilter>()
    private val cosmeticExceptionFilters = mutableListOf<CosmeticFilter>()

    // Anti-anti-adblock scriptlets
    private val scriptletRules = mutableListOf<Pair<Set<String>, String>>() // domains → scriptlet call

    private var enabled = false

    // ==================== Public API ====================

    fun setEnabled(enable: Boolean) {
        enabled = enable
        synchronized(blockResultCache) { blockResultCache.clear() }
    }
    fun isEnabled(): Boolean = enabled

    /** Invalidate the LRU cache — call on page navigation */
    fun invalidateCache() {
        synchronized(blockResultCache) { blockResultCache.clear() }
    }

    /**
     * Initialize blocker with custom rules and optional defaults.
     */
    fun initialize(customRules: List<String> = emptyList(), useDefaultRules: Boolean = true) {
        exactHosts.clear()
        networkBlockFilters.clear()
        networkExceptionFilters.clear()
        anchorDomainIndex.clear()
        exceptionAnchorDomainIndex.clear()
        cosmeticBlockFilters.clear()
        cosmeticExceptionFilters.clear()
        scriptletRules.clear()
        blockResultCache.clear()

        if (useDefaultRules) {
            exactHosts.addAll(DEFAULT_AD_HOSTS)
            // ★ Built-in ABP network filter rules — instant protection without external lists
            DEFAULT_NETWORK_RULES.forEach { parseAndAddRule(it) }
            // ★ Built-in cosmetic (element hiding) rules — hide ad containers, cookie banners, popups
            DEFAULT_COSMETIC_RULES.forEach { parseAndAddRule(it) }
        }

        customRules.forEach { parseAndAddRule(it) }
    }

    /**
     * Full-featured blocking check.
     *
     * @param url             Request URL
     * @param pageHost        Host of the top-level page (for $third-party / $domain)
     * @param resourceType    Resource type string from WebView (e.g. "script", "image")
     * @param isThirdParty    Whether the request is third-party
     * @return true if the request should be blocked
     */
    fun shouldBlock(
        url: String,
        pageHost: String? = null,
        resourceType: String? = null,
        isThirdParty: Boolean = true
    ): Boolean {
        if (!enabled) return false

        val lowerUrl = url.lowercase()
        val urlHost = extractHost(lowerUrl)

        // Safelist — critical infrastructure never blocked (HashSet O(1))
        if (urlHost != null && matchesHostSet(urlHost, SAFELIST_HOSTS)) return false

        val resType = resourceType?.let { ResourceType.fromWebViewType(it) } ?: ResourceType.OTHER

        // Never block main document / subdocument navigations — prevents page breakage
        if (resType == ResourceType.DOCUMENT) return false

        // Essential resource protection — fonts, main stylesheets should never be blocked
        // as they cause severe visual breakage (invisible text, missing layout)
        if (resType == ResourceType.FONT) return false
        if (resType == ResourceType.STYLESHEET && !isThirdParty) return false

        // ★ Essential resource URL pattern protection — pre-compiled single-pass regex
        if (ESSENTIAL_RESOURCE_REGEX.containsMatchIn(lowerUrl)) return false

        // ★ LRU cache lookup — skip repeated regex scans for identical URLs
        val cacheKey = lowerUrl.hashCode() xor (if (isThirdParty) 0x9e3779b9.toInt() else 0)
        synchronized(blockResultCache) {
            blockResultCache[cacheKey]?.let { return it }
        }

        val result = shouldBlockInternal(lowerUrl, urlHost, pageHost, resType, isThirdParty)
        synchronized(blockResultCache) {
            blockResultCache[cacheKey] = result
        }
        return result
    }

    /**
     * Internal blocking logic — separated from shouldBlock for LRU cache wrapping.
     */
    private fun shouldBlockInternal(
        lowerUrl: String,
        urlHost: String?,
        pageHost: String?,
        resType: ResourceType,
        isThirdParty: Boolean
    ): Boolean {
        // Phase 1: Exact host match (HashSet O(1) — fastest check)
        val hostMatched = urlHost != null && (matchesHostSet(urlHost, exactHosts) || matchesHostSet(urlHost, hostsFileHosts))

        if (hostMatched) {
            if (matchesAnyNetworkFilter(lowerUrl, urlHost, pageHost, resType, isThirdParty, networkExceptionFilters)) {
                return false
            }
            return true
        }

        // Phase 2: Check exception filters
        if (matchesAnyNetworkFilter(lowerUrl, urlHost, pageHost, resType, isThirdParty, networkExceptionFilters)) {
            return false
        }

        // Phase 3: ABP network filters (block rules)
        if (matchesAnyNetworkFilter(lowerUrl, urlHost, pageHost, resType, isThirdParty, networkBlockFilters)) {
            return true
        }

        // Phase 4: First-party ad path detection — ★ pre-compiled single-pass regex
        if (!isThirdParty && FIRST_PARTY_AD_PATH_REGEX.containsMatchIn(lowerUrl)) {
            if (resType == ResourceType.SCRIPT || resType == ResourceType.XMLHTTPREQUEST ||
                resType == ResourceType.SUBDOCUMENT) {
                if (!matchesAnyNetworkFilter(lowerUrl, urlHost, pageHost, resType, isThirdParty, networkExceptionFilters)) {
                    return true
                }
            }
        }

        return false
    }

    // Backward-compatible overload (existing call sites)
    // Enhanced: extracts resource type from request headers for better filtering
    fun shouldBlock(request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        val resType = inferResourceTypeFromRequest(request)
        return shouldBlock(url, resourceType = resType)
    }

    /**
     * Infer resource type from WebResourceRequest Accept header and URL extension.
     */
    private fun inferResourceTypeFromRequest(request: WebResourceRequest): String {
        val accept = request.requestHeaders?.get("Accept") ?: ""
        return when {
            accept.contains("text/html") -> "main_frame"
            accept.contains("text/css") -> "stylesheet"
            accept.contains("image/") -> "image"
            accept.contains("font/") || accept.contains("application/font") -> "font"
            accept.contains("application/javascript") || accept.contains("text/javascript") -> "script"
            accept.contains("application/json") -> "xmlhttprequest"
            else -> {
                // Fallback: check URL extension
                val path = request.url.path?.lowercase() ?: ""
                when {
                    path.endsWith(".js") -> "script"
                    path.endsWith(".css") -> "stylesheet"
                    path.endsWith(".jpg") || path.endsWith(".jpeg") || path.endsWith(".png") ||
                        path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg") ||
                        path.endsWith(".ico") -> "image"
                    path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") ||
                        path.endsWith(".otf") || path.endsWith(".eot") -> "font"
                    path.endsWith(".mp4") || path.endsWith(".webm") || path.endsWith(".mp3") ||
                        path.endsWith(".ogg") -> "media"
                    else -> "other"
                }
            }
        }
    }

    /**
     * Get cosmetic CSS selectors to hide on a given page.
     * Returns a CSS string ready for injection as a <style> element.
     */
    fun getCosmeticFilterCss(pageHost: String): String {
        if (!enabled) return ""

        // Collect exception selectors for this page
        val exceptionSelectors = cosmeticExceptionFilters
            .filter { matchesCosmeticDomain(it, pageHost) }
            .map { it.selector }
            .toSet()

        // Collect block selectors, minus exceptions
        val selectors = cosmeticBlockFilters
            .filter { matchesCosmeticDomain(it, pageHost) && it.selector !in exceptionSelectors }
            .map { it.selector }
            .distinct()
            .toMutableList()

        // Built-in universal cosmetic selectors — target common ad containers
        // These work out-of-the-box without requiring imported filter lists
        val builtInSelectors = listOf(
            // Google Ads / AdSense
            "ins.adsbygoogle",
            "[id^=\"google_ads_\"]",
            "[id^=\"div-gpt-ad\"]",
            "[data-ad-slot]",
            "[data-ad-client]",
            "[data-google-query-id]",
            // Generic ad markers
            "[class*=\"ad-banner\"]",
            "[class*=\"ad-container\"]",
            "[class*=\"ad-wrapper\"]",
            "[id*=\"ad-banner\"]",
            "[class*=\"advertisement\"]",
            "[class*=\"sponsored-content\"]",
            "[data-ad]",
            "[data-adunit]",
            "[data-ad-unit]",
            // Ad iframes
            "iframe[src*=\"doubleclick\"]",
            "iframe[src*=\"googlesyndication\"]",
            "iframe[src*=\"adservice\"]",
            "iframe[src*=\"ad.php\"]",
            "iframe[id*=\"google_ads\"]",
            // Taboola / Outbrain native ads
            "[id^=\"taboola-\"]",
            "[class*=\"taboola\"]",
            "[class*=\"outbrain\"]",
            "[data-widget-type=\"taboola\"]",
            // Common Chinese ad elements
            "[class*=\"ad-box\"]",
            "[class*=\"ad-slot\"]",
            "[id*=\"J_ad\"]",
            "[class*=\"guanggao\"]",
            "[id*=\"guanggao\"]"
        )

        builtInSelectors.forEach { sel ->
            if (sel !in exceptionSelectors && sel !in selectors) {
                selectors.add(sel)
            }
        }

        if (selectors.isEmpty()) return ""

        // Batch selectors (avoid CSS selector list length limits in older WebViews)
        val batchSize = 50
        return selectors.chunked(batchSize).joinToString("\n") { batch ->
            batch.joinToString(",\n") + " { display: none !important; visibility: hidden !important; height: 0 !important; min-height: 0 !important; overflow: hidden !important; }"
        }
    }

    /**
     * Generate anti-anti-adblock scriptlet JS for a given page.
     */
    fun getAntiAdblockScript(pageHost: String): String {
        if (!enabled) return ""

        val applicableScriptlets = scriptletRules.filter { (domains, _) ->
            domains.isEmpty() || domains.any { d ->
                pageHost == d || pageHost.endsWith(".$d")
            }
        }
        if (applicableScriptlets.isEmpty()) return ""

        val sb = StringBuilder()
        sb.appendLine("(function(){")
        sb.appendLine("'use strict';")
        sb.appendLine("if(window.__wta_aab_injected__)return;window.__wta_aab_injected__=true;")

        applicableScriptlets.forEach { (_, scriptlet) ->
            sb.appendLine(scriptlet)
        }

        // Built-in universal anti-anti-adblock measures
        sb.appendLine(UNIVERSAL_ANTI_ADBLOCK_SCRIPT)

        sb.appendLine("})();")
        return sb.toString()
    }


    /**
     * Return empty response (used to block ad requests)
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(EMPTY_BYTES))
    }

    /**
     * Create a type-specific empty response to avoid page errors
     * Use the preallocated byte arrays and minimal InputStream wrappers to keep GC low
     */
    fun createEmptyResponse(resourceType: String): WebResourceResponse {
        return when (resourceType) {
            "script" -> WebResourceResponse("application/javascript", "UTF-8",
                ByteArrayInputStream(BLOCKED_JS_BYTES))
            "stylesheet" -> WebResourceResponse("text/css", "UTF-8",
                ByteArrayInputStream(BLOCKED_CSS_BYTES))
            "image" -> WebResourceResponse("image/gif", null,
                ByteArrayInputStream(TRANSPARENT_GIF))
            "xmlhttprequest" -> WebResourceResponse("application/json", "UTF-8",
                ByteArrayInputStream(BLOCKED_JSON_BYTES))
            else -> WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(EMPTY_BYTES))
        }
    }

    // ==================== Rule counts ====================
    fun getRuleCount(): Int = exactHosts.size + hostsFileHosts.size +
        networkBlockFilters.size + networkExceptionFilters.size +
        cosmeticBlockFilters.size + cosmeticExceptionFilters.size
    fun getHostsFileRuleCount(): Int = hostsFileHosts.size
    fun getNetworkFilterCount(): Int = networkBlockFilters.size + networkExceptionFilters.size
    fun getCosmeticFilterCount(): Int = cosmeticBlockFilters.size + cosmeticExceptionFilters.size

    fun getStats(): Map<String, Int> = mapOf(
        "exactHosts" to exactHosts.size,
        "hostsFile" to hostsFileHosts.size,
        "networkBlock" to networkBlockFilters.size,
        "networkException" to networkExceptionFilters.size,
        "cosmeticBlock" to cosmeticBlockFilters.size,
        "cosmeticException" to cosmeticExceptionFilters.size,
        "scriptlets" to scriptletRules.size
    )

    // ==================== Rule management ====================
    fun addRule(rule: String) {
        parseAndAddRule(rule)
        synchronized(blockResultCache) { blockResultCache.clear() }
    }

    fun removeRule(rule: String) {
        exactHosts.remove(rule)
        synchronized(blockResultCache) { blockResultCache.clear() }
    }

    fun clearRules() {
        exactHosts.clear()
        networkBlockFilters.clear()
        networkExceptionFilters.clear()
        anchorDomainIndex.clear()
        exceptionAnchorDomainIndex.clear()
        cosmeticBlockFilters.clear()
        cosmeticExceptionFilters.clear()
        scriptletRules.clear()
        blockResultCache.clear()
    }

    fun clearHostsFileRules() {
        hostsFileHosts.clear()
        enabledHostsSources.clear()
        synchronized(blockResultCache) { blockResultCache.clear() }
    }

    fun getEnabledHostsSources(): Set<String> = enabledHostsSources.toSet()
    fun isHostsSourceEnabled(url: String): Boolean = enabledHostsSources.contains(url)

    // ==================== ABP Filter Parser ====================

    /**
     * Parse a single filter rule in ABP / AdGuard / hosts format.
     */
    private fun parseAndAddRule(rawRule: String) {
        val rule = rawRule.trim()
        if (rule.isEmpty() || rule.startsWith("!") || rule.startsWith("[")) return

        // Cosmetic filter: domain##selector or domain#@#selector
        val cosmeticExIdx = rule.indexOf("#@#")
        val cosmeticIdx = if (cosmeticExIdx < 0) rule.indexOf("##") else -1

        if (cosmeticExIdx >= 0) {
            parseCosmeticFilter(rule, cosmeticExIdx, "#@#", isException = true)
            return
        }
        if (cosmeticIdx >= 0 && !rule.startsWith("||")) {
            parseCosmeticFilter(rule, cosmeticIdx, "##", isException = false)
            return
        }

        // Scriptlet: domain#%#//scriptlet(...)
        if (rule.contains("#%#//scriptlet(")) {
            parseScriptletRule(rule)
            return
        }
        // AG scriptlet shorthand: domain##+js(...)
        if (rule.contains("##+js(")) {
            parseScriptletRule(rule)
            return
        }

        // Network filter
        parseNetworkFilter(rule)
    }

    private fun parseCosmeticFilter(rule: String, idx: Int, delimiter: String, isException: Boolean) {
        val domainPart = rule.substring(0, idx)
        val selector = rule.substring(idx + delimiter.length).trim()
        if (selector.isEmpty()) return

        val (domains, excludedDomains) = parseDomainList(domainPart)

        val filter = CosmeticFilter(
            selector = selector,
            isException = isException,
            domains = domains,
            excludedDomains = excludedDomains
        )

        if (isException) cosmeticExceptionFilters.add(filter)
        else cosmeticBlockFilters.add(filter)
    }

    private fun parseScriptletRule(rule: String) {
        val domainEnd = rule.indexOfFirst { it == '#' }
        val domainPart = if (domainEnd > 0) rule.substring(0, domainEnd) else ""
        val (domains, _) = parseDomainList(domainPart)

        // Extract scriptlet name & args
        val scriptletMatch = Regex("(?:scriptlet|js)\\((.+)\\)").find(rule) ?: return
        val args = scriptletMatch.groupValues[1].split(",").map { it.trim().removeSurrounding("'").removeSurrounding("\"") }
        if (args.isEmpty()) return

        val js = generateScriptlet(args[0], args.drop(1))
        if (js.isNotEmpty()) {
            scriptletRules.add(domains to js)
        }
    }

    private fun parseNetworkFilter(rule: String) {
        var raw = rule
        val isException = raw.startsWith("@@")
        if (isException) raw = raw.removePrefix("@@")

        // Split off $ modifiers
        val dollarIdx = raw.lastIndexOf('$')
        var patternPart = raw
        var modifierPart = ""
        if (dollarIdx > 0) {
            // Make sure $ is not inside a regex
            val beforeDollar = raw.substring(0, dollarIdx)
            if (!beforeDollar.contains('/') || beforeDollar.count { it == '/' } % 2 == 0) {
                patternPart = beforeDollar
                modifierPart = raw.substring(dollarIdx + 1)
            }
        }

        // Parse modifiers
        var thirdPartyOnly = false
        var firstPartyOnly = false
        var matchCase = false
        val domainConstraints = mutableSetOf<String>()
        val excludedDomainConstraints = mutableSetOf<String>()
        val allowedTypes = mutableSetOf<ResourceType>()
        val excludedTypes = mutableSetOf<ResourceType>()

        if (modifierPart.isNotEmpty()) {
            modifierPart.split(",").forEach { mod ->
                val m = mod.trim().lowercase()
                when {
                    m == "third-party" || m == "3p" -> thirdPartyOnly = true
                    m == "~third-party" || m == "first-party" || m == "1p" -> firstPartyOnly = true
                    m == "match-case" -> matchCase = true
                    m == "important" -> { /* priority — we already prioritize exceptions */ }
                    m.startsWith("domain=") -> {
                        m.removePrefix("domain=").split("|").forEach { d ->
                            if (d.startsWith("~")) excludedDomainConstraints.add(d.removePrefix("~"))
                            else domainConstraints.add(d)
                        }
                    }
                    m.startsWith("~") -> {
                        TYPE_MODIFIERS[m.removePrefix("~")]?.let { excludedTypes.add(it) }
                    }
                    else -> {
                        TYPE_MODIFIERS[m]?.let { allowedTypes.add(it) }
                    }
                }
            }
        }

        // Detect anchor domain: ||domain^ or ||domain/
        var anchorDomain: String? = null
        if (patternPart.startsWith("||")) {
            val domainEnd = patternPart.indexOfFirst { it == '^' || it == '/' || it == '*' || it == '$' }
            anchorDomain = if (domainEnd > 2) patternPart.substring(2, domainEnd).lowercase()
            else patternPart.removePrefix("||").removeSuffix("^").removeSuffix("$").lowercase()
        }

        // Simple host-only rule: ||domain^ with no modifiers → fast path
        if (anchorDomain != null && !anchorDomain.contains('*') &&
            (patternPart == "||$anchorDomain^" || patternPart == "||$anchorDomain" ||
             patternPart == "||$anchorDomain^|") &&
            modifierPart.isEmpty() && !isException) {
            exactHosts.add(anchorDomain)
            return
        }

        // Compile pattern to regex
        val regex = compileAbpPattern(patternPart, matchCase)

        val filter = NetworkFilter(
            pattern = patternPart,
            regex = regex,
            isException = isException,
            matchCase = matchCase,
            domains = domainConstraints,
            excludedDomains = excludedDomainConstraints,
            allowedTypes = allowedTypes.ifEmpty { null },
            excludedTypes = excludedTypes,
            thirdPartyOnly = thirdPartyOnly,
            firstPartyOnly = firstPartyOnly,
            anchorDomain = anchorDomain
        )

        if (isException) {
            val idx = networkExceptionFilters.size
            networkExceptionFilters.add(filter)
            // ★ Index exception filters by anchor domain for O(1) lookup
            if (anchorDomain != null && !anchorDomain.contains('*')) {
                exceptionAnchorDomainIndex.getOrPut(anchorDomain) { mutableListOf() }.add(idx)
            }
        } else {
            val idx = networkBlockFilters.size
            networkBlockFilters.add(filter)
            // Index by anchor domain for fast lookup
            if (anchorDomain != null && !anchorDomain.contains('*')) {
                anchorDomainIndex.getOrPut(anchorDomain) { mutableListOf() }.add(idx)
            }
        }
    }

    /**
     * Compile ABP filter pattern to Regex.
     * ||  = domain anchor
     * |   = string start/end anchor
     * ^   = separator (anything except alphanumeric, %, -, .)
     * *   = wildcard
     */
    private fun compileAbpPattern(pattern: String, matchCase: Boolean): Regex? {
        if (pattern.isEmpty()) return null
        return try {
            var p = pattern
            // Regex filter: /regex/
            if (p.startsWith("/") && p.endsWith("/") && p.length > 2) {
                val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
                return Regex(p.substring(1, p.length - 1), options)
            }

            // Escape regex special chars (except ABP special chars we handle)
            p = p.replace("\\", "\\\\")
                .replace(".", "\\.")
                .replace("+", "\\+")
                .replace("?", "\\?")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("[", "\\[")
                .replace("]", "\\]")

            // ABP special: || → domain anchor
            p = p.replace("||", "^(?:https?|wss?)://(?:[^/]*\\.)?")
            // ABP special: | at start → string start
            if (p.startsWith("|")) p = "^" + p.removePrefix("|")
            // ABP special: | at end → string end
            if (p.endsWith("|")) p = p.removeSuffix("|") + "$"
            // ABP special: ^ → separator
            p = p.replace("^", "[^\\w%.\\-]")
            // ABP special: * → wildcard
            p = p.replace("*", ".*")

            val options = if (matchCase) emptySet() else setOf(RegexOption.IGNORE_CASE)
            Regex(p, options)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDomainList(domainStr: String): Pair<Set<String>, Set<String>> {
        if (domainStr.isBlank()) return emptySet<String>() to emptySet()
        val include = mutableSetOf<String>()
        val exclude = mutableSetOf<String>()
        domainStr.split(",").forEach { d ->
            val trimmed = d.trim().lowercase()
            if (trimmed.startsWith("~")) exclude.add(trimmed.removePrefix("~"))
            else if (trimmed.isNotEmpty()) include.add(trimmed)
        }
        return include to exclude
    }

    // ==================== Network filter matching ====================

    private fun matchesAnyNetworkFilter(
        url: String,
        urlHost: String?,
        pageHost: String?,
        resType: ResourceType,
        isThirdParty: Boolean,
        filters: List<NetworkFilter>
    ): Boolean {
        // ★ Determine which domain index to use based on filter list identity
        val domainIndex = when {
            filters === networkBlockFilters -> anchorDomainIndex
            filters === networkExceptionFilters -> exceptionAnchorDomainIndex
            else -> null
        }

        // Fast path: check anchor domain index first (O(1) per domain level)
        if (urlHost != null && domainIndex != null) {
            var domain: String = urlHost
            while (domain.contains('.')) {
                domainIndex[domain]?.forEach { idx ->
                    val filter = filters[idx]
                    if (matchesNetworkFilter(filter, url, urlHost, pageHost, resType, isThirdParty)) {
                        return true
                    }
                }
                domain = domain.substringAfter('.')
            }
            domainIndex[domain]?.forEach { idx ->
                val filter = filters[idx]
                if (matchesNetworkFilter(filter, url, urlHost, pageHost, resType, isThirdParty)) {
                    return true
                }
            }
        }

        // Full scan for non-indexed filters (those without anchor domain)
        for (filter in filters) {
            if (filter.anchorDomain != null && domainIndex != null) continue
            if (matchesNetworkFilter(filter, url, urlHost, pageHost, resType, isThirdParty)) {
                return true
            }
        }
        return false
    }

    private fun matchesNetworkFilter(
        filter: NetworkFilter,
        url: String,
        urlHost: String?,
        pageHost: String?,
        resType: ResourceType,
        isThirdParty: Boolean
    ): Boolean {
        // Resource type check
        if (filter.allowedTypes != null && resType !in filter.allowedTypes) return false
        if (resType in filter.excludedTypes) return false

        // Party check
        if (filter.thirdPartyOnly && !isThirdParty) return false
        if (filter.firstPartyOnly && isThirdParty) return false

        // Domain constraint check
        if (filter.domains.isNotEmpty() && pageHost != null) {
            if (!filter.domains.any { pageHost == it || pageHost.endsWith(".$it") }) return false
        }
        if (filter.excludedDomains.isNotEmpty() && pageHost != null) {
            if (filter.excludedDomains.any { pageHost == it || pageHost.endsWith(".$it") }) return false
        }

        // Pattern match
        val regex = filter.regex ?: return false
        return regex.containsMatchIn(url)
    }

    // ==================== Cosmetic filter matching ====================

    private fun matchesCosmeticDomain(filter: CosmeticFilter, pageHost: String): Boolean {
        if (filter.excludedDomains.any { pageHost == it || pageHost.endsWith(".$it") }) return false
        if (filter.domains.isEmpty()) return true
        return filter.domains.any { pageHost == it || pageHost.endsWith(".$it") }
    }

    // ==================== Host matching ====================

    private fun matchesHostSet(host: String, hostSet: Set<String>): Boolean {
        var domain = host
        while (domain.contains('.')) {
            if (hostSet.contains(domain)) return true
            domain = domain.substringAfter('.')
        }
        return hostSet.contains(domain)
    }

    /**
     * C-level host extraction with zero-allocation pointer traversal
     * Avoid Uri.parse(url).host to skip URI object creation and extra GC
     * shouldBlock runs for every subresource, so saving allocations matters
     */
    private fun extractHost(url: String): String? {
        // Fall back to Uri.parse when the C-level scan misses
        val host = com.webtoapp.core.perf.NativePerfEngine.extractHost(url)
        if (host != null) return host.lowercase()
        return try {
            Uri.parse(url).host?.lowercase()
        } catch (e: Exception) {
            HOST_EXTRACT_REGEX.find(url)?.groupValues?.getOrNull(1)?.lowercase()
        }
    }

    // ==================== Scriptlet generator ====================

    /**
     * Generate JS code for common anti-anti-adblock scriptlets.
     * Compatible with uBlock Origin / AdGuard scriptlet names.
     */
    private fun generateScriptlet(name: String, args: List<String>): String {
        return when (name) {
            "abort-on-property-read", "aopr" -> {
                val prop = args.getOrNull(0) ?: return ""
                """
                (function(){
                    var chain = '${prop}'.split('.');
                    var owner = window;
                    for(var i=0;i<chain.length-1;i++){
                        if(!owner[chain[i]])owner[chain[i]]={};
                        owner=owner[chain[i]];
                    }
                    var last=chain[chain.length-1];
                    Object.defineProperty(owner,last,{get:function(){throw new ReferenceError()},set:function(){}});
                })();
                """.trimIndent()
            }
            "abort-on-property-write", "aopw" -> {
                val prop = args.getOrNull(0) ?: return ""
                """
                (function(){
                    var chain='${prop}'.split('.');
                    var owner=window;
                    for(var i=0;i<chain.length-1;i++){
                        if(!owner[chain[i]])owner[chain[i]]={};
                        owner=owner[chain[i]];
                    }
                    var last=chain[chain.length-1];
                    Object.defineProperty(owner,last,{get:function(){return undefined;},set:function(){throw new Error();}});
                })();
                """.trimIndent()
            }
            "abort-current-inline-script", "acis" -> {
                val prop = args.getOrNull(0) ?: return ""
                val search = args.getOrNull(1) ?: ""
                """
                (function(){
                    var target='$prop';var needle='$search';
                    var chain=target.split('.');var owner=window;
                    for(var i=0;i<chain.length-1;i++){if(owner[chain[i]])owner=owner[chain[i]];else return;}
                    var last=chain[chain.length-1];var orig=owner[last];
                    Object.defineProperty(owner,last,{get:function(){
                        if(needle){
                            var e=new Error();if(e.stack&&e.stack.indexOf(needle)!==-1)throw new ReferenceError();
                        }else{
                            var e=new Error();var s=document.currentScript;
                            if(s&&s.textContent&&s.textContent.indexOf(target)!==-1)throw new ReferenceError();
                        }
                        return typeof orig==='function'?orig.bind(this):orig;
                    },set:function(v){orig=v;}});
                })();
                """.trimIndent()
            }
            "set-constant", "set" -> {
                val prop = args.getOrNull(0) ?: return ""
                val value = args.getOrNull(1) ?: "undefined"
                val jsValue = when(value) {
                    "true" -> "true"; "false" -> "false"
                    "null" -> "null"; "undefined" -> "undefined"
                    "noopFunc" -> "function(){}"
                    "trueFunc" -> "function(){return true;}"
                    "falseFunc" -> "function(){return false;}"
                    "emptyStr" -> "''"
                    "emptyArr" -> "[]"
                    "emptyObj" -> "{}"
                    "" -> "undefined"
                    else -> if (value.toDoubleOrNull() != null) value else "'$value'"
                }
                """
                (function(){
                    var chain='$prop'.split('.');var owner=window;
                    for(var i=0;i<chain.length-1;i++){
                        if(!owner[chain[i]])owner[chain[i]]={};
                        owner=owner[chain[i]];
                    }
                    var last=chain[chain.length-1];
                    try{Object.defineProperty(owner,last,{value:$jsValue,writable:false,configurable:true});}catch(e){/* expected */}
                })();
                """.trimIndent()
            }
            "remove-attr", "ra" -> {
                val attrs = args.getOrNull(0) ?: return ""
                val selector = args.getOrNull(1) ?: "*"
                """
                (function(){
                    var attrs='$attrs'.split('|');var sel='$selector';
                    function clean(){
                        document.querySelectorAll(sel).forEach(function(el){
                            attrs.forEach(function(a){el.removeAttribute(a);});
                        });
                    }
                    clean();
                    new MutationObserver(function(){clean();}).observe(document.documentElement,{childList:true,subtree:true,attributes:true});
                })();
                """.trimIndent()
            }
            "remove-class", "rc" -> {
                val classes = args.getOrNull(0) ?: return ""
                val selector = args.getOrNull(1) ?: "*"
                """
                (function(){
                    var cls='$classes'.split('|');var sel='$selector';
                    function clean(){
                        document.querySelectorAll(sel).forEach(function(el){
                            cls.forEach(function(c){el.classList.remove(c);});
                        });
                    }
                    clean();
                    new MutationObserver(function(){clean();}).observe(document.documentElement,{childList:true,subtree:true,attributes:true});
                })();
                """.trimIndent()
            }
            "nano-setInterval-booster", "nano-sib" -> {
                val needle = args.getOrNull(0) ?: ""
                val delay = args.getOrNull(1) ?: "1000"
                val boost = args.getOrNull(2) ?: "0.05"
                """
                (function(){
                    var needle='$needle';var delay=$delay;var boost=$boost;
                    var orig=window.setInterval;
                    window.setInterval=function(fn,ms){
                        var s=typeof fn==='function'?fn.toString():''+fn;
                        if((!needle||s.indexOf(needle)!==-1)&&ms==delay)ms=ms*boost;
                        return orig.call(this,fn,ms);
                    };
                })();
                """.trimIndent()
            }
            "nano-setTimeout-booster", "nano-stb" -> {
                val needle = args.getOrNull(0) ?: ""
                val delay = args.getOrNull(1) ?: "1000"
                val boost = args.getOrNull(2) ?: "0.05"
                """
                (function(){
                    var needle='$needle';var delay=$delay;var boost=$boost;
                    var orig=window.setTimeout;
                    window.setTimeout=function(fn,ms){
                        var s=typeof fn==='function'?fn.toString():''+fn;
                        if((!needle||s.indexOf(needle)!==-1)&&ms==delay)ms=ms*boost;
                        return orig.call(this,fn,ms);
                    };
                })();
                """.trimIndent()
            }
            "prevent-addEventListener", "aell" -> {
                val type = args.getOrNull(0) ?: ""
                val needle = args.getOrNull(1) ?: ""
                """
                (function(){
                    var type='$type';var needle='$needle';
                    var orig=EventTarget.prototype.addEventListener;
                    EventTarget.prototype.addEventListener=function(t,fn,o){
                        if(type&&t!==type)return orig.call(this,t,fn,o);
                        if(needle){var s=typeof fn==='function'?fn.toString():'';if(s.indexOf(needle)===-1)return orig.call(this,t,fn,o);}
                        return undefined;
                    };
                })();
                """.trimIndent()
            }
            "prevent-fetch", "no-fetch-if" -> {
                val needle = args.getOrNull(0) ?: ""
                """
                (function(){
                    var needle='$needle';var orig=window.fetch;
                    window.fetch=function(){
                        var url=typeof arguments[0]==='string'?arguments[0]:(arguments[0]&&arguments[0].url)||'';
                        if(!needle||url.indexOf(needle)!==-1)return Promise.resolve(new Response('',{status:200}));
                        return orig.apply(this,arguments);
                    };
                })();
                """.trimIndent()
            }
            "prevent-xhr", "no-xhr-if" -> {
                val needle = args.getOrNull(0) ?: ""
                """
                (function(){
                    var needle='$needle';var origOpen=XMLHttpRequest.prototype.open;
                    XMLHttpRequest.prototype.open=function(m,u){
                        this.__wta_url=u;return origOpen.apply(this,arguments);
                    };
                    var origSend=XMLHttpRequest.prototype.send;
                    XMLHttpRequest.prototype.send=function(){
                        if(needle&&this.__wta_url&&this.__wta_url.indexOf(needle)!==-1){
                            Object.defineProperty(this,'readyState',{value:4});
                            Object.defineProperty(this,'status',{value:200});
                            Object.defineProperty(this,'responseText',{value:''});
                            Object.defineProperty(this,'response',{value:''});
                            this.dispatchEvent(new Event('load'));return;
                        }
                        return origSend.apply(this,arguments);
                    };
                })();
                """.trimIndent()
            }
            else -> ""
        }
    }

    // ==================== Hosts File Support ====================

    suspend fun importHostsFromFile(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file"))
            val count = inputStream.use { stream ->
                parseFilterContent(stream.bufferedReader().readText())
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importHostsFromUrl(url: String, context: Context? = null): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Try URL content cache first (avoid re-downloading within TTL)
            var content: String? = null
            if (context != null) {
                content = AdBlockFilterCache.getCachedUrlContent(context, url)
            }
            
            if (content == null) {
                // Download from network
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "WebToApp/1.0")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext Result.failure(
                        Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
                    )
                }
                content = connection.inputStream.use { it.bufferedReader().readText() }
                connection.disconnect()
                
                // Cache the downloaded content
                if (context != null) {
                    AdBlockFilterCache.cacheUrlContent(context, url, content)
                }
            }
            
            val count = parseFilterContent(content)
            enabledHostsSources.add(url)
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Parse filter list content — supports both hosts format and ABP filter syntax.
     * Auto-detects format: if lines start with [Adblock or contain ## / || / @@, treat as ABP.
     */
    private fun parseFilterContent(content: String): Int {
        var count = 0
        val isAbpFormat = content.lineSequence().take(20).any { line ->
            val t = line.trim()
            t.startsWith("[Adblock") || t.startsWith("!") || t.startsWith("||") ||
                t.startsWith("@@") || t.contains("##") || t.contains("#@#")
        }

        content.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[") || trimmed.startsWith("#")) {
                // For hosts format, # is comment; for ABP, lines starting with only # are ignored
                if (!trimmed.startsWith("##") && !trimmed.startsWith("#@#") && !trimmed.startsWith("#%#") && !trimmed.startsWith("##+")) {
                    return@forEach
                }
            }

            if (isAbpFormat) {
                parseAndAddRule(trimmed)
                count++
            } else {
                val host = parseHostLine(trimmed)
                if (host != null && isValidHost(host)) {
                    hostsFileHosts.add(host.lowercase())
                    count++
                }
            }
        }
        return count
    }

    private fun parseHostLine(line: String): String? {
        if (line.startsWith("||")) {
            return line.removePrefix("||").removeSuffix("^").removeSuffix("$").trim()
        }
        val parts = line.split(WHITESPACE_REGEX)
        if (parts.size >= 2) {
            val firstPart = parts[0]
            if (firstPart == "0.0.0.0" || firstPart == "127.0.0.1" ||
                firstPart.startsWith("0.") || firstPart.startsWith("127.") ||
                firstPart == "::" || firstPart == "::1") {
                val domain = parts[1].split("#")[0].trim()
                if (domain.isNotEmpty() && domain != "localhost" && domain != "localhost.localdomain") {
                    return domain
                }
            }
        }
        if (parts.size == 1 && parts[0].contains(".") && !parts[0].contains("/")) {
            return parts[0]
        }
        return null
    }

    private fun isValidHost(host: String): Boolean {
        if (host.isEmpty() || host.length > 253) return false
        if (host.startsWith(".") || host.endsWith(".")) return false
        if (host.contains("..")) return false
        if (host.matches(IP_ADDRESS_REGEX)) return false
        if (host == "localhost" || host == "broadcasthost" || host == "local") return false
        return true
    }

    suspend fun saveHostsRules(context: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Legacy format (backward compat)
            val file = File(context.filesDir, "adblock_hosts.txt")
            file.writeText(hostsFileHosts.joinToString("\n"))
            val sourcesFile = File(context.filesDir, "adblock_hosts_sources.txt")
            sourcesFile.writeText(enabledHostsSources.joinToString("\n"))
            
            // Save full compiled state cache
            saveCompiledStateToCache(context)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadHostsRules(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Try loading from compiled cache first (fast path)
            val cachedState = AdBlockFilterCache.loadCompiledState(context)
            if (cachedState != null) {
                restoreFromCompiledState(cachedState)
                com.webtoapp.core.logging.AppLogger.i("AdBlocker", 
                    "Restored from compiled cache: ${getRuleCount()} rules")
                return@withContext Result.success(hostsFileHosts.size)
            }
            
            // Fallback to legacy format
            val file = File(context.filesDir, "adblock_hosts.txt")
            if (file.exists()) {
                hostsFileHosts.addAll(file.readLines().filter { it.isNotBlank() })
            }
            val sourcesFile = File(context.filesDir, "adblock_hosts_sources.txt")
            if (sourcesFile.exists()) {
                enabledHostsSources.addAll(sourcesFile.readLines().filter { it.isNotBlank() })
            }
            Result.success(hostsFileHosts.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Compiled State Cache Helpers ====================

    private suspend fun saveCompiledStateToCache(context: Context) {
        val blockPatterns = networkBlockFilters.map { it.toSerializable() }
        val exceptionPatterns = networkExceptionFilters.map { it.toSerializable() }

        AdBlockFilterCache.saveCompiledState(
            context = context,
            exactHosts = exactHosts.toSet(),
            hostsFileHosts = hostsFileHosts.toSet(),
            enabledSources = enabledHostsSources.toSet(),
            networkBlockPatterns = blockPatterns,
            networkExceptionPatterns = exceptionPatterns,
            cosmeticBlockFilters = cosmeticBlockFilters.toList(),
            cosmeticExceptionFilters = cosmeticExceptionFilters.toList(),
            scriptletRules = scriptletRules.toList()
        )
    }

    private fun restoreFromCompiledState(state: AdBlockFilterCache.CompiledState) {
        exactHosts.clear()
        hostsFileHosts.clear()
        enabledHostsSources.clear()
        networkBlockFilters.clear()
        networkExceptionFilters.clear()
        anchorDomainIndex.clear()
        exceptionAnchorDomainIndex.clear()
        cosmeticBlockFilters.clear()
        cosmeticExceptionFilters.clear()
        scriptletRules.clear()
        blockResultCache.clear()

        exactHosts.addAll(state.exactHosts)
        hostsFileHosts.addAll(state.hostsFileHosts)
        enabledHostsSources.addAll(state.enabledSources)

        // Recompile network filters (pattern → regex)
        state.networkBlockFilters.forEachIndexed { idx, sf ->
            val filter = sf.toNetworkFilter()
            networkBlockFilters.add(filter)
            if (filter.anchorDomain != null && !filter.anchorDomain.contains('*')) {
                anchorDomainIndex.getOrPut(filter.anchorDomain) { mutableListOf() }.add(idx)
            }
        }
        // ★ Index exception filters on restore
        state.networkExceptionFilters.forEachIndexed { idx, sf ->
            val filter = sf.toNetworkFilter()
            networkExceptionFilters.add(filter)
            if (filter.anchorDomain != null && !filter.anchorDomain.contains('*')) {
                exceptionAnchorDomainIndex.getOrPut(filter.anchorDomain) { mutableListOf() }.add(idx)
            }
        }

        cosmeticBlockFilters.addAll(state.cosmeticBlockFilters)
        cosmeticExceptionFilters.addAll(state.cosmeticExceptionFilters)
        scriptletRules.addAll(state.scriptletRules)
    }

    private fun NetworkFilter.toSerializable() = AdBlockFilterCache.SerializableNetworkFilter(
        pattern = pattern,
        isException = isException,
        matchCase = matchCase,
        domains = domains,
        excludedDomains = excludedDomains,
        allowedTypeNames = allowedTypes?.map { it.name }?.toSet(),
        excludedTypeNames = excludedTypes.map { it.name }.toSet(),
        thirdPartyOnly = thirdPartyOnly,
        firstPartyOnly = firstPartyOnly,
        anchorDomain = anchorDomain
    )

    private fun AdBlockFilterCache.SerializableNetworkFilter.toNetworkFilter() = NetworkFilter(
        pattern = pattern,
        regex = compileAbpPattern(pattern, matchCase),
        isException = isException,
        matchCase = matchCase,
        domains = domains,
        excludedDomains = excludedDomains,
        allowedTypes = allowedTypeNames?.mapNotNull { name ->
            try { ResourceType.valueOf(name) } catch (_: Exception) { null }
        }?.toSet(),
        excludedTypes = excludedTypeNames.mapNotNull { name ->
            try { ResourceType.valueOf(name) } catch (_: Exception) { null }
        }.toSet(),
        thirdPartyOnly = thirdPartyOnly,
        firstPartyOnly = firstPartyOnly,
        anchorDomain = anchorDomain
    )
}

// ==================== Constants ====================

/** 1×1 transparent GIF — returned for blocked image requests to avoid broken-image icons */
private val TRANSPARENT_GIF = byteArrayOf(
    0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
    0x00, 0x00, 0x00, 0x21, 0xF9.toByte(), 0x04, 0x01, 0x00, 0x00, 0x00,
    0x00, 0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00,
    0x00, 0x02, 0x01, 0x00, 0x00
)

/**
 * Universal anti-anti-adblock measures.
 * These defuse the most common adblock detection patterns without targeting specific sites.
 */
private const val UNIVERSAL_ANTI_ADBLOCK_SCRIPT = """
// 1. Fake ad element — many detectors create a div with ad classes and check visibility
(function(){
    var baitClasses=['ad-banner','ad_banner','ad-placeholder','adsbox','ad-container',
        'adbadge','BannerAd','ad-large','ad-top','GoogleAd','adsense',
        'ad-unit','ad-zone','ad-wrapper','ad-slot','textAd','text-ad',
        'banner-ad','sponsor-ad','ad-area','ad-display'];
    function createBait(){
        baitClasses.forEach(function(cls){
            var existing=document.querySelector('.'+cls);
            if(!existing){
                var d=document.createElement('div');
                d.className=cls;
                d.style.cssText='position:absolute!important;width:1px!important;height:1px!important;opacity:0.01!important;pointer-events:none!important;left:-9999px!important;';
                d.innerHTML='&nbsp;';
                (document.body||document.documentElement).appendChild(d);
            }
        });
    }
    if(document.readyState!=='loading')createBait();
    else document.addEventListener('DOMContentLoaded',createBait);
})();

// 2. Neutralize common adblock detection variables
(function(){
    try{
        // FuckAdBlock / BlockAdBlock
        window.fuckAdBlock=window.blockAdBlock={
            onDetected:function(){return this;},onNotDetected:function(fn){try{fn();}catch(e){/* expected */}return this;},
            on:function(_,fn){try{if(_===false||_==='notDetected')fn();}catch(e){/* expected */}return this;},
            check:function(){return false;},emitEvent:function(){return this;}
        };
        window.sniffAdBlock=window.canRunAds=true;
        window.isAdBlockActive=false;
        // AdBlock Detect 2.x
        if(typeof window.google_ad_status==='undefined')window.google_ad_status=1;
        // Additional common detection flags
        window.adBlockEnabled=false;
        window.adBlockDetected=false;
        window.adblockEnabled=false;
        window.abd=false;
        window.AdBlockDetected=false;
        // Adsense status spoofing
        if(typeof window.google_ad_status==='undefined')window.google_ad_status=1;
        if(typeof window.adsbygoogle==='undefined'){
            window.adsbygoogle={loaded:true,push:function(){}};
        }
    }catch(e){/* anti-adblock defuse failed */}
})();

// 3. Prevent overlay/modal adblock walls — remove fixed/sticky overlays that appear after ads fail
(function(){
    function removeWalls(){
        var selectors=[
            '[class*="adblock"]','[class*="ad-block"]','[id*="adblock"]','[id*="ad-block"]',
            '[class*="adb-overlay"]','[class*="blocker-overlay"]',
            '[class*="adblock-modal"]','[class*="anti-ad"]',
            '[class*="adblocker"]','[id*="adblocker"]',
            '[class*="disable-adblock"]','[class*="adblock-notice"]',
            '[class*="adblock-wall"]','[class*="paywall-adblock"]'
        ];
        var overlays=document.querySelectorAll(selectors.join(','));
        overlays.forEach(function(el){
            var style=window.getComputedStyle(el);
            if(style.position==='fixed'||style.position==='absolute'||style.position==='sticky'){
                el.style.setProperty('display','none','important');
                el.style.setProperty('visibility','hidden','important');
                el.style.setProperty('opacity','0','important');
                el.style.setProperty('pointer-events','none','important');
            }
        });
        // Restore scroll if locked
        if(document.body){
            var bs=window.getComputedStyle(document.body);
            if(bs.overflow==='hidden'||bs.overflowY==='hidden'){
                document.body.style.setProperty('overflow','auto','important');
                document.body.style.setProperty('overflow-y','auto','important');
            }
        }
        // Also fix html element overflow lock
        var html=document.documentElement;
        if(html){
            var hs=window.getComputedStyle(html);
            if(hs.overflow==='hidden'||hs.overflowY==='hidden'){
                html.style.setProperty('overflow','auto','important');
            }
        }
    }
    var obs=new MutationObserver(function(){removeWalls();});
    if(document.documentElement)obs.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(function(){obs.disconnect();},15000);
})();

// 4. Spoof getComputedStyle for ad bait elements
// Many detectors check if bait elements are hidden (height=0, display=none, visibility=hidden)
(function(){
    try{
        var origGetCS=window.getComputedStyle;
        window.getComputedStyle=function(el,pseudo){
            var result=origGetCS.call(window,el,pseudo);
            if(el&&el.className&&typeof el.className==='string'){
                var cls=el.className.toLowerCase();
                var adKeywords=['ad-banner','adsbox','ad_banner','ad-placeholder',
                    'ad-container','ad-unit','textad','banner-ad','ad-wrapper'];
                var isAdBait=adKeywords.some(function(k){return cls.indexOf(k)>=0;});
                if(isAdBait){
                    // Return a proxy that reports the element as visible
                    return new Proxy(result,{
                        get:function(target,prop){
                            if(prop==='display')return 'block';
                            if(prop==='visibility')return 'visible';
                            if(prop==='opacity')return '1';
                            if(prop==='height')return '250px';
                            if(prop==='width')return '300px';
                            var val=target[prop];
                            return typeof val==='function'?val.bind(target):val;
                        }
                    });
                }
            }
            return result;
        };
    }catch(e){/* Proxy not supported — skip */}
})();

// 5. Intercept fetch/XHR requests to ad-detection beacon URLs
// Prevent sites from reporting adblock status to their servers
(function(){
    try{
        var adDetectPatterns=['/adblock','ad-detect','adblock-detect',
            'blockadblock','detect-ad','adblocker-detect','pagead/ads'];
        // Intercept fetch
        var origFetch=window.fetch;
        window.fetch=function(url){
            if(typeof url==='string'){
                var lower=url.toLowerCase();
                if(adDetectPatterns.some(function(p){return lower.indexOf(p)>=0;})){
                    return Promise.resolve(new Response('',{status:200}));
                }
            }
            return origFetch.apply(window,arguments);
        };
        // Intercept XHR
        var origOpen=XMLHttpRequest.prototype.open;
        XMLHttpRequest.prototype.open=function(method,url){
            if(typeof url==='string'){
                var lower=url.toLowerCase();
                if(adDetectPatterns.some(function(p){return lower.indexOf(p)>=0;})){
                    this.__wta_blocked_adb=true;
                }
            }
            return origOpen.apply(this,arguments);
        };
        var origSend=XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.send=function(){
            if(this.__wta_blocked_adb){
                Object.defineProperty(this,'status',{get:function(){return 200;}});
                Object.defineProperty(this,'readyState',{get:function(){return 4;}});
                Object.defineProperty(this,'responseText',{get:function(){return '';}});
                var self=this;
                setTimeout(function(){
                    if(typeof self.onload==='function')self.onload();
                    if(typeof self.onreadystatechange==='function')self.onreadystatechange();
                },10);
                return;
            }
            return origSend.apply(this,arguments);
        };
    }catch(e){/* XHR/fetch interception failed */}
})();
"""

/**
 * Hosts source info
 */
data class HostsSource(
    val name: String,
    val url: String,
    val description: String
)
