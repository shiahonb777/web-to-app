package com.webtoapp.core.engine.shields

import android.net.Uri

/**
 * TrackerBlocker — 跟踪器专项拦截引擎
 *
 * 与 AdBlocker 分开管理，专注于隐私跟踪类请求：
 * - Analytics（数据分析）
 * - Social（社交追踪）
 * - Fingerprinting（指纹采集）
 * - Cryptomining（加密挖矿）
 * - Ad Networks（广告网络跟踪）
 * - CNAME Cloaking（一方伪装跟踪）
 *
 * 匹配方式：
 * 1. O(1) HashSet 域名查找 + 父域名遍历
 * 2. URL 路径模式匹配（host+path 捕获隐藏在一方域名下的跟踪端点）
 * 3. 查询参数名称匹配（常见跟踪参数如 fbclid、gclid 等标记但不拦截）
 */
class TrackerBlocker {

    companion object {
        private const val TAG = "TrackerBlocker"

        // ==================== Analytics 跟踪器 ====================
        private val ANALYTICS_TRACKERS = setOf(
            // Google Analytics & Tag Manager
            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "analytics.google.com", "ssl.google-analytics.com",
            "www.googleoptimize.com", "tagmanager.google.com",
            // Firebase Analytics
            "firebaselogging-pa.googleapis.com", "app-measurement.com",
            // Facebook/Meta Pixel
            "connect.facebook.net", "pixel.facebook.com",
            // Microsoft
            "clarity.ms", "bat.bing.com", "c.bing.com",
            // Major SaaS analytics
            "hotjar.com", "static.hotjar.com", "script.hotjar.com",
            "mixpanel.com", "cdn.mxpnl.com", "api-js.mixpanel.com",
            "segment.io", "segment.com", "cdn.segment.com", "api.segment.io",
            "amplitude.com", "cdn.amplitude.com", "api2.amplitude.com",
            "heapanalytics.com", "cdn.heapanalytics.com",
            "fullstory.com", "rs.fullstory.com", "edge.fullstory.com",
            "mouseflow.com", "cdn.mouseflow.com",
            "luckyorange.com", "cdn.luckyorange.com", "w1.luckyorange.com",
            "crazyegg.com", "script.crazyegg.com",
            "inspectlet.com", "cdn.inspectlet.com",
            "smartlook.com", "rec.smartlook.com",
            "contentsquare.net", "t.contentsquare.net",
            // Open-source / privacy-focused (still tracking)
            "matomo.org", "plausible.io", "umami.is",
            "simpleanalytics.com", "goatcounter.com",
            // Chinese analytics
            "hm.baidu.com", "tongji.baidu.com", "cnzz.com", "s.cnzz.com",
            "51.la", "51yes.com", "growingio.com",
            "sensors.data", "sensorsdata.cn", "static.sensorsdata.cn",
            "aldwx.com", "umeng.com", "alog.umeng.com", "alog.umengcloud.com",
            "log.aldwx.com", "giocdn.com",
            // Japanese / Korean analytics
            "analytics.yahoo.co.jp", "b.yjtag.jp",
            "treasure-data.com", "in.treasuredata.com",
            "wcslog.naver.com",
            // Russian analytics
            "mc.yandex.ru", "metrika.yandex.ru",
            // Other global analytics
            "statcounter.com", "histats.com", "clicky.com",
            "chartbeat.com", "static.chartbeat.com",
            "newrelic.com", "js-agent.newrelic.com", "bam.nr-data.net",
            "bugsnag.com", "sessions.bugsnag.com",
            // NOTE: sentry.io removed - it's essential error monitoring used by many websites
            // "sentry.io", "browser.sentry-cdn.com",  // Removed to prevent breaking error reporting
            "logrocket.com", "cdn.lr-in-prod.com",
            "pendo.io", "cdn.pendo.io", "app.pendo.io",
            "walkme.com", "cdn.walkme.com",
            "kissmetrics.com", "scripts.kissmetrics.com",
            "optimizely.com", "cdn.optimizely.com", "logx.optimizely.com",
            "quantserve.com", "pixel.quantserve.com",
            "scorecardresearch.com", "sb.scorecardresearch.com",
            "comscore.com", "b.scorecardresearch.com",
            "parsely.com", "cdn.parsely.com",
            "tealiumiq.com", "tags.tiqcdn.com",
            "adobedtm.com", "assets.adobedtm.com",
            "omtrdc.net", "2o7.net",
            // European analytics
            "piano.io", "api-cdn.piano.io", "t.piano.io",
            "at-o.net", "xiti.com", "tag.at-o.net",
            "webtrekk.net", "wt-eu02.net",
            "econda.com", "econda-monitor.de",
            "etracker.com", "etracker.de",
            "matomo.cloud",
            // Consent management / CMP analytics
            "privacy-mgmt.com", "sp-prod.net", "sourcepoint.com",
            // Turkish analytics
            "analytiks.co", "clickagy.com"
        )

        // ==================== Social 跟踪器 ====================
        private val SOCIAL_TRACKERS = setOf(
            // Facebook / Meta
            "graph.facebook.com", "pixel.facebook.com",
            "connect.facebook.net", "staticxx.facebook.com",
            // Instagram
            "l.instagram.com", "graph.instagram.com",
            // Twitter / X
            "platform.twitter.com", "syndication.twitter.com",
            "analytics.twitter.com", "t.co",
            "static.ads-twitter.com", "ads-api.twitter.com",
            // LinkedIn
            "snap.licdn.com", "platform.linkedin.com",
            "px.ads.linkedin.com",
            // Pinterest
            "ct.pinterest.com", "log.pinterest.com",
            "trk.pinterest.com", "widgets.pinterest.com",
            // TikTok
            "analytics.tiktok.com", "mon.tiktokv.com",
            "analytics-sg.tiktok.com", "mcs.tiktokw.us",
            // Snapchat
            "tr.snapchat.com", "sc-static.net",
            // Reddit
            "events.reddit.com", "redditmedia.com",
            // Quora
            "q.quora.com",
            // WeChat / Weibo / QQ
            "res.wx.qq.com", "open.weixin.qq.com",
            "badge.weibo.com", "widget.weibo.com",
            "beacon.qq.com", "report.qq.com", "pingtas.qq.com",
            // LINE
            "d.line-scdn.net", "w.line.me",
            // VK
            "top-fwz1.mail.ru",
            // Mastodon / Fediverse (analytics endpoints)
            "joinmastodon.org/apps"
        )

        // ==================== Fingerprinting 指纹采集 ====================
        private val FINGERPRINTING_TRACKERS = setOf(
            // FingerprintJS
            "fpjs.io", "fingerprint.com", "api.fpjs.io", "cdn.fpjs.io",
            "fpnpmcdn.net",
            // Fraud / device intelligence
            "creativecdn.com", "iovation.com", "ci-mpsnare.iovation.com",
            "threatmetrix.com", "h.online-metrix.net",
            "deviceident.com",
            "seon.io", "cdn.seon.io",
            "castle.io",
            "riskcenter.akamai.com",
            // NOTE: CAPTCHA services removed - essential for bot verification
            // "arkoselabs.com", "cdn.arkoselabs.com", "funcaptcha.com",
            // Canvas/WebGL fingerprinting services
            "maxmind.com", "geoip.maxmind.com",
            "ipinfo.io", "ipapi.co", "ip-api.com",
            "whoer.net",
            // Bot detection
            "perfdrive.com", "perimeterx.net", "px-cdn.net", "px-cloud.net",
            "datadome.co", "ct.datadome.co",
            "imperva.com", "incapsula.com",
            "distil.net", "areyouahuman.com"
            // NOTE: hCaptcha removed - essential for CAPTCHA verification
            // "hcaptcha.com", "newassets.hcaptcha.com"
        )

        // ==================== Cryptomining 加密挖矿 ====================
        private val CRYPTOMINING_TRACKERS = setOf(
            "coinhive.com", "coin-hive.com",
            "jsecoin.com", "cryptoloot.pro",
            "crypto-loot.com", "miner.pr0gramm.com",
            "authedmine.com", "minero.cc",
            "webminerpool.com", "coinlab.biz",
            "coinimp.com", "2giga.link",
            "hashforcash.us", "ppoi.org",
            "coin-have.com", "minescripts.info",
            "mining.3q3.de", "monerominer.rocks",
            "webminepool.com", "browsermine.com",
            "cloudcoins.co", "coinblind.com",
            "coinnebula.com", "gridcash.net",
            "load.jsecoin.com", "monerise.com",
            "perfekt.cc", "reasedoper.pw"
        )

        // ==================== 广告网络跟踪 ====================
        private val AD_NETWORK_TRACKERS = setOf(
            // Google
            "doubleclick.net", "googlesyndication.com",
            "googleadservices.com", "pagead2.googlesyndication.com",
            "adservice.google.com", "www.googleadservices.com",
            // Major DSPs / SSPs
            "adnxs.com", "ib.adnxs.com",
            "criteo.com", "criteo.net", "static.criteo.net",
            "taboola.com", "cdn.taboola.com", "trc.taboola.com",
            "outbrain.com", "widgets.outbrain.com", "log.outbrain.com",
            "pubmatic.com", "ads.pubmatic.com",
            "rubiconproject.com", "fastlane.rubiconproject.com",
            "openx.net", "u.openx.net",
            "casalemedia.com", "htlb.casalemedia.com",
            "sharethrough.com", "33across.com",
            "amazon-adsystem.com", "s.amazon-adsystem.com", "aax.amazon-adsystem.com",
            "media.net", "contextual.media.net",
            "moatads.com", "z.moatads.com", "px.moatads.com",
            "serving-sys.com",
            // DMP / data brokers
            "eyeota.net", "bluekai.com", "tags.bluekai.com",
            "exelator.com", "demdex.net", "dpm.demdex.net",
            "krxd.net", "cdn.krxd.net", "beacon.krxd.net",
            "adsrvr.org", "match.adsrvr.org",
            "bidswitch.net", "contextweb.com",
            "spotxchange.com", "search.spotxchange.com",
            "indexww.com", "indexexchange.com",
            "triplelift.com", "eb2.3lift.com",
            "smartadserver.com", "www6.smartadserver.com",
            "yieldmo.com", "ads.yieldmo.com",
            "sovrn.com", "ap.lijit.com",
            "conversantmedia.com", "media.richrelevance.com",
            "quantcast.com", "pixel.quantcast.com",
            // Ad verification
            "doubleverify.com", "cdn.doubleverify.com",
            "adsafeprotected.com", "static.adsafeprotected.com",
            // Chinese ad networks
            "tanx.com", "alimama.com", "mmstat.com",
            "pos.baidu.com", "cpro.baidu.com", "dsp.baidu.com",
            "union.sogou.com",
            "e.qq.com", "gdt.qq.com", "mi.gdt.qq.com",
            "biddingx.com", "admaster.com.cn",
            "miaozhen.com", "s.gridsumdissector.com",
            // Japanese ad networks
            "ad.doubleclick.net", "i-mobile.co.jp",
            "microad.net", "aladdinclk.microad.net",
            "yimg.jp",
            // Korean ad networks
            "search.daum.net",
            "adcreative.naver.com"
        )

        // ==================== CNAME Cloaking / 一方伪装跟踪 ====================
        private val CNAME_TRACKERS = setOf(
            "smetrics.att.com", "smetrics.bestbuy.com",
            "data.pendo.io", "metrics.mzstatic.com",
            "tr.indeed.com", "eum-us-west-1.instana.io",
            "log.optimizely.com",
            // International CNAME cloaking
            "smetrics.nytimes.com", "smetrics.washingtonpost.com",
            "analytics.theguardian.com", "smetrics.bbc.com",
            "smetrics.cnn.com", "smetrics.foxnews.com",
            "smetrics.reuters.com",
            "smetrics.adobe.com", "smetrics.oracle.com",
            "smetrics.dell.com", "smetrics.microsoft.com",
            "smetrics.walmart.com", "smetrics.target.com",
            "smetrics.homedepot.com", "smetrics.lowes.com"
        )

        // ==================== URL Path Patterns ====================
        // Matches host+path to catch trackers hiding on first-party or CDN domains
        private data class PathPattern(
            val hostSuffix: String,    // domain suffix to match
            val pathPrefix: String,    // path prefix to match
            val category: TrackerCategory
        )

        private val PATH_PATTERNS = listOf(
            // Facebook pixel on facebook.com
            PathPattern("facebook.com", "/tr", TrackerCategory.SOCIAL),
            PathPattern("facebook.com", "/signals/", TrackerCategory.SOCIAL),
            // Google collect endpoints
            PathPattern("google-analytics.com", "/collect", TrackerCategory.ANALYTICS),
            PathPattern("google-analytics.com", "/j/collect", TrackerCategory.ANALYTICS),
            PathPattern("analytics.google.com", "/g/collect", TrackerCategory.ANALYTICS),
            // Common self-hosted analytics endpoints
            PathPattern("", "/wp-json/analytics", TrackerCategory.ANALYTICS),
            PathPattern("", "/matomo.js", TrackerCategory.ANALYTICS),
            PathPattern("", "/matomo.php", TrackerCategory.ANALYTICS),
            PathPattern("", "/piwik.js", TrackerCategory.ANALYTICS),
            PathPattern("", "/piwik.php", TrackerCategory.ANALYTICS),
            // LinkedIn Insight tag
            PathPattern("linkedin.com", "/li/track", TrackerCategory.SOCIAL),
            PathPattern("licdn.com", "/li/track", TrackerCategory.SOCIAL),
            // Discord science
            PathPattern("discord.com", "/api/v9/science", TrackerCategory.SOCIAL),
            PathPattern("discord.com", "/api/v10/science", TrackerCategory.SOCIAL),
            // Reddit pixel
            PathPattern("reddit.com", "/rpixel", TrackerCategory.SOCIAL),
            // TikTok analytics
            PathPattern("tiktok.com", "/i18n/pixel/", TrackerCategory.SOCIAL),
            // Baidu analytics
            PathPattern("baidu.com", "/hm.js", TrackerCategory.ANALYTICS),
            PathPattern("baidu.com", "/h.js", TrackerCategory.ANALYTICS),
            // Yandex Metrica
            PathPattern("yandex.ru", "/metrika/", TrackerCategory.ANALYTICS),
            // Common CDN-hosted fingerprint scripts
            PathPattern("jsdelivr.net", "/npm/fingerprintjs", TrackerCategory.FINGERPRINTING),
            PathPattern("jsdelivr.net", "/npm/@aspect-build/", TrackerCategory.FINGERPRINTING),
            PathPattern("unpkg.com", "/fingerprintjs", TrackerCategory.FINGERPRINTING),
            PathPattern("cdnjs.cloudflare.com", "/ajax/libs/fingerprintjs", TrackerCategory.FINGERPRINTING),
            // Common mining script paths
            PathPattern("", "/coinhive.min.js", TrackerCategory.CRYPTOMINING),
            PathPattern("", "/cryptonight.wasm", TrackerCategory.CRYPTOMINING),
            // Path-based social trackers (domains too broad to block entirely)
            PathPattern("vk.com", "/rtrg", TrackerCategory.SOCIAL),
            PathPattern("developers.kakao.com", "/sdk", TrackerCategory.SOCIAL),
            PathPattern("redditstatic.com", "/ads", TrackerCategory.SOCIAL),
            // Path-based ad network trackers
            PathPattern("yimg.jp", "/images/listing", TrackerCategory.AD_NETWORK),
            PathPattern("search.daum.net", "/adbusiness", TrackerCategory.AD_NETWORK),
            PathPattern("naver.com", "/wcslog.js", TrackerCategory.ANALYTICS)
        )

        // ==================== Known tracking query parameters ====================
        // These don't cause blocking but can be stripped
        val TRACKING_QUERY_PARAMS = setOf(
            "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid",
            "msclkid", "twclid", "ttclid", "li_fat_id",
            "mc_cid", "mc_eid",
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
            "utm_id", "utm_source_platform", "utm_creative_format", "utm_marketing_tactic",
            "_ga", "_gl", "_gid",
            "yclid", "ymclid",
            "wickedid", "guccounter", "guce_referrer",
            "hsa_cam", "hsa_grp", "hsa_mt", "hsa_src", "hsa_ad", "hsa_acc",
            "hsa_net", "hsa_ver", "hsa_la", "hsa_ol", "hsa_kw",
            "ref_", "ref_src", "ref_url",
            "s_kwcid", "ef_id", "srsltid",
            "igshid", "si"
        )
    }

    // Pre-built category map for O(1) domain lookup
    private val trackerCategoryMap: Map<String, TrackerCategory> by lazy {
        buildMap {
            ANALYTICS_TRACKERS.forEach { put(it, TrackerCategory.ANALYTICS) }
            SOCIAL_TRACKERS.forEach { put(it, TrackerCategory.SOCIAL) }
            FINGERPRINTING_TRACKERS.forEach { put(it, TrackerCategory.FINGERPRINTING) }
            CRYPTOMINING_TRACKERS.forEach { put(it, TrackerCategory.CRYPTOMINING) }
            AD_NETWORK_TRACKERS.forEach { put(it, TrackerCategory.AD_NETWORK) }
            CNAME_TRACKERS.forEach { put(it, TrackerCategory.ANALYTICS) }
        }
    }

    // All tracker domains in a single set for fast existence check
    private val allTrackerDomains: Set<String> by lazy {
        buildSet {
            addAll(ANALYTICS_TRACKERS)
            addAll(SOCIAL_TRACKERS)
            addAll(FINGERPRINTING_TRACKERS)
            addAll(CRYPTOMINING_TRACKERS)
            addAll(AD_NETWORK_TRACKERS)
            addAll(CNAME_TRACKERS)
        }
    }

    // ★ 路径模式按 hostSuffix 建索引，实现 O(1) 查找
    // hostSuffix 为空的模式（通配符）保留在单独列表中线性扫描
    private val hostPathPatternIndex: Map<String, List<PathPattern>> by lazy {
        PATH_PATTERNS.filter { it.hostSuffix.isNotEmpty() }.groupBy { it.hostSuffix }
    }
    private val wildcardPathPatterns: List<PathPattern> by lazy {
        PATH_PATTERNS.filter { it.hostSuffix.isEmpty() }
    }

    /**
     * Essential service domains that should NOT be blocked even if they appear
     * in tracker lists. These domains serve dual purpose: tracking AND essential
     * website functionality (login SDKs, social widgets, comment systems, etc.)
     *
     * We only safelist the domain here; specific tracking endpoints from these
     * domains may still be blocked via PATH_PATTERNS.
     */
    private val TRACKER_SAFELIST = setOf(
        // Facebook SDK — needed for FB login, comments, and social plugins
        // (pixel.facebook.com is still blocked; connect.facebook.net is safelisted)
        "connect.facebook.net",
        // Google Tag Manager — many sites use it for essential A/B testing and
        // feature flags, not just analytics. Blocking breaks site functionality.
        // Individual analytics endpoints are still blocked via path patterns.
        "www.googletagmanager.com",
        // Sentry/Bugsnag — error monitoring essential for app developers
        "sentry.io", "browser.sentry-cdn.com",
        "bugsnag.com", "sessions.bugsnag.com",
        // These analytics tools also provide essential heatmap/session replay
        // for site owners — only block specific collect/beacon endpoints
        "cdn.segment.com",
        // Optimizely — many sites use it for feature flags, not just A/B tests
        "cdn.optimizely.com"
    )

    /**
     * 检查 URL 是否为跟踪器请求
     *
     * @param url 请求 URL
     * @return 跟踪器分类，如果不是跟踪器则返回 null
     */
    fun checkTracker(url: String): TrackerCategory? {
        // ★ Zero-allocation host extraction via NativePerfEngine (replaces Uri.parse())
        val host = com.webtoapp.core.perf.NativePerfEngine.extractHost(url)?.lowercase() ?: return null

        // Safelist check — some tracker domains also serve essential services
        if (TRACKER_SAFELIST.contains(host)) return null

        // 1. Domain match (O(1) with parent-domain walk)
        matchTrackerDomain(host)?.let { return it }

        // 2. Path pattern match — extract path without Uri allocation
        val pathStart = url.indexOf('/', url.indexOf(host) + host.length)
        if (pathStart >= 0) {
            val pathEnd = url.indexOfAny(charArrayOf('?', '#'), pathStart).let { if (it < 0) url.length else it }
            val path = url.substring(pathStart, pathEnd).lowercase()
            if (path.isNotEmpty()) {
                matchPathPattern(host, path)?.let { return it }
            }
        }

        return null
    }

    /**
     * 检查域名是否匹配跟踪器列表
     * 使用父域名遍历：a.b.tracker.com → b.tracker.com → tracker.com
     */
    private fun matchTrackerDomain(host: String): TrackerCategory? {
        var domain = host
        while (domain.contains('.')) {
            trackerCategoryMap[domain]?.let { return it }
            domain = domain.substringAfter('.')
        }
        return trackerCategoryMap[domain]
    }

    /**
     * ★ 优化后的 host+path 匹配
     * 1. 先用 HashMap 按 hostSuffix 查找（O(1)），检查 host 是否 endsWith
     * 2. 再扫描通配符规则（hostSuffix 为空的，数量极少，约 7 条）
     */
    private fun matchPathPattern(host: String, path: String): TrackerCategory? {
        // ★ 快速路径：用 hostSuffix 索引做 O(1) 查找
        // 遍历 host 的后缀：host=sub.facebook.com → 查 sub.facebook.com, facebook.com, com
        var suffix = host
        while (suffix.contains('.')) {
            hostPathPatternIndex[suffix]?.let { patterns ->
                for (p in patterns) {
                    if (path.startsWith(p.pathPrefix)) return p.category
                }
            }
            suffix = suffix.substringAfter('.')
        }
        // 完全匹配最后一段（几乎不会命中，但逻辑完整）
        hostPathPatternIndex[suffix]?.let { patterns ->
            for (p in patterns) {
                if (path.startsWith(p.pathPrefix)) return p.category
            }
        }

        // ★ 通配符模式：hostSuffix 为空 → 匹配所有域名，仅检查 path
        for (p in wildcardPathPatterns) {
            if (path.startsWith(p.pathPrefix)) return p.category
        }

        return null
    }

    /**
     * 检查 URL 是否应该被拦截（任意分类）
     */
    fun shouldBlock(url: String): Boolean {
        return checkTracker(url) != null
    }

    /**
     * 从 URL 中剥离已知跟踪查询参数
     * @return 清理后的 URL，如果无变化则返回 null
     */
    fun stripTrackingParams(url: String): String? {
        val uri = try { Uri.parse(url) } catch (_: Exception) { return null }
        val queryParams = uri.queryParameterNames
        if (queryParams.isEmpty()) return null

        val trackingParams = queryParams.filter { it.lowercase() in TRACKING_QUERY_PARAMS }
        if (trackingParams.isEmpty()) return null

        val builder = uri.buildUpon().clearQuery()
        for (param in queryParams) {
            if (param.lowercase() !in TRACKING_QUERY_PARAMS) {
                uri.getQueryParameter(param)?.let { builder.appendQueryParameter(param, it) }
            }
        }
        return builder.build().toString()
    }

    /**
     * 获取规则总数
     */
    fun getRuleCount(): Int = allTrackerDomains.size + PATH_PATTERNS.size

    /**
     * 获取各分类规则数
     */
    fun getCategoryStats(): Map<TrackerCategory, Int> = mapOf(
        TrackerCategory.ANALYTICS to ANALYTICS_TRACKERS.size,
        TrackerCategory.SOCIAL to SOCIAL_TRACKERS.size,
        TrackerCategory.FINGERPRINTING to FINGERPRINTING_TRACKERS.size,
        TrackerCategory.CRYPTOMINING to CRYPTOMINING_TRACKERS.size,
        TrackerCategory.AD_NETWORK to AD_NETWORK_TRACKERS.size
    )
}
