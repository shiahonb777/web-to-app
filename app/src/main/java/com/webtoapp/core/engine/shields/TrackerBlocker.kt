package com.webtoapp.core.engine.shields

import android.net.Uri

















class TrackerBlocker {

    companion object {
        private const val TAG = "TrackerBlocker"


        private val ANALYTICS_TRACKERS = setOf(

            "google-analytics.com", "googletagmanager.com", "googletagservices.com",
            "analytics.google.com", "ssl.google-analytics.com",
            "www.googleoptimize.com", "tagmanager.google.com",

            "firebaselogging-pa.googleapis.com", "app-measurement.com",

            "connect.facebook.net", "pixel.facebook.com",

            "clarity.ms", "bat.bing.com", "c.bing.com",

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

            "matomo.org", "plausible.io", "umami.is",
            "simpleanalytics.com", "goatcounter.com",

            "hm.baidu.com", "tongji.baidu.com", "cnzz.com", "s.cnzz.com",
            "51.la", "51yes.com", "growingio.com",
            "sensors.data", "sensorsdata.cn", "static.sensorsdata.cn",
            "aldwx.com", "umeng.com", "alog.umeng.com", "alog.umengcloud.com",
            "log.aldwx.com", "giocdn.com",

            "analytics.yahoo.co.jp", "b.yjtag.jp",
            "treasure-data.com", "in.treasuredata.com",
            "wcslog.naver.com",

            "mc.yandex.ru", "metrika.yandex.ru",

            "statcounter.com", "histats.com", "clicky.com",
            "chartbeat.com", "static.chartbeat.com",
            "newrelic.com", "js-agent.newrelic.com", "bam.nr-data.net",
            "bugsnag.com", "sessions.bugsnag.com",


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

            "piano.io", "api-cdn.piano.io", "t.piano.io",
            "at-o.net", "xiti.com", "tag.at-o.net",
            "webtrekk.net", "wt-eu02.net",
            "econda.com", "econda-monitor.de",
            "etracker.com", "etracker.de",
            "matomo.cloud",

            "privacy-mgmt.com", "sp-prod.net", "sourcepoint.com",

            "analytiks.co", "clickagy.com"
        )


        private val SOCIAL_TRACKERS = setOf(

            "graph.facebook.com", "pixel.facebook.com",
            "connect.facebook.net", "staticxx.facebook.com",

            "l.instagram.com", "graph.instagram.com",

            "platform.twitter.com", "syndication.twitter.com",
            "analytics.twitter.com", "t.co",
            "static.ads-twitter.com", "ads-api.twitter.com",

            "snap.licdn.com", "platform.linkedin.com",
            "px.ads.linkedin.com",

            "ct.pinterest.com", "log.pinterest.com",
            "trk.pinterest.com", "widgets.pinterest.com",

            "analytics.tiktok.com", "mon.tiktokv.com",
            "analytics-sg.tiktok.com", "mcs.tiktokw.us",

            "tr.snapchat.com", "sc-static.net",

            "events.reddit.com", "redditmedia.com",

            "q.quora.com",

            "res.wx.qq.com", "open.weixin.qq.com",
            "badge.weibo.com", "widget.weibo.com",
            "beacon.qq.com", "report.qq.com", "pingtas.qq.com",

            "d.line-scdn.net", "w.line.me",

            "top-fwz1.mail.ru",

            "joinmastodon.org/apps"
        )


        private val FINGERPRINTING_TRACKERS = setOf(

            "fpjs.io", "fingerprint.com", "api.fpjs.io", "cdn.fpjs.io",
            "fpnpmcdn.net",

            "creativecdn.com", "iovation.com", "ci-mpsnare.iovation.com",
            "threatmetrix.com", "h.online-metrix.net",
            "deviceident.com",
            "seon.io", "cdn.seon.io",
            "castle.io",
            "riskcenter.akamai.com",



            "maxmind.com", "geoip.maxmind.com",
            "ipinfo.io", "ipapi.co", "ip-api.com",
            "whoer.net",

            "perfdrive.com", "perimeterx.net", "px-cdn.net", "px-cloud.net",
            "datadome.co", "ct.datadome.co",
            "imperva.com", "incapsula.com",
            "distil.net", "areyouahuman.com"


        )


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


        private val AD_NETWORK_TRACKERS = setOf(

            "doubleclick.net", "googlesyndication.com",
            "googleadservices.com", "pagead2.googlesyndication.com",
            "adservice.google.com", "www.googleadservices.com",

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

            "doubleverify.com", "cdn.doubleverify.com",
            "adsafeprotected.com", "static.adsafeprotected.com",

            "tanx.com", "alimama.com", "mmstat.com",
            "pos.baidu.com", "cpro.baidu.com", "dsp.baidu.com",
            "union.sogou.com",
            "e.qq.com", "gdt.qq.com", "mi.gdt.qq.com",
            "biddingx.com", "admaster.com.cn",
            "miaozhen.com", "s.gridsumdissector.com",

            "ad.doubleclick.net", "i-mobile.co.jp",
            "microad.net", "aladdinclk.microad.net",
            "yimg.jp",

            "search.daum.net",
            "adcreative.naver.com"
        )


        private val CNAME_TRACKERS = setOf(
            "smetrics.att.com", "smetrics.bestbuy.com",
            "data.pendo.io", "metrics.mzstatic.com",
            "tr.indeed.com", "eum-us-west-1.instana.io",
            "log.optimizely.com",

            "smetrics.nytimes.com", "smetrics.washingtonpost.com",
            "analytics.theguardian.com", "smetrics.bbc.com",
            "smetrics.cnn.com", "smetrics.foxnews.com",
            "smetrics.reuters.com",
            "smetrics.adobe.com", "smetrics.oracle.com",
            "smetrics.dell.com", "smetrics.microsoft.com",
            "smetrics.walmart.com", "smetrics.target.com",
            "smetrics.homedepot.com", "smetrics.lowes.com"
        )



        private data class PathPattern(
            val hostSuffix: String,
            val pathPrefix: String,
            val category: TrackerCategory
        )

        private val PATH_PATTERNS = listOf(

            PathPattern("facebook.com", "/tr", TrackerCategory.SOCIAL),
            PathPattern("facebook.com", "/signals/", TrackerCategory.SOCIAL),

            PathPattern("google-analytics.com", "/collect", TrackerCategory.ANALYTICS),
            PathPattern("google-analytics.com", "/j/collect", TrackerCategory.ANALYTICS),
            PathPattern("analytics.google.com", "/g/collect", TrackerCategory.ANALYTICS),

            PathPattern("", "/wp-json/analytics", TrackerCategory.ANALYTICS),
            PathPattern("", "/matomo.js", TrackerCategory.ANALYTICS),
            PathPattern("", "/matomo.php", TrackerCategory.ANALYTICS),
            PathPattern("", "/piwik.js", TrackerCategory.ANALYTICS),
            PathPattern("", "/piwik.php", TrackerCategory.ANALYTICS),

            PathPattern("linkedin.com", "/li/track", TrackerCategory.SOCIAL),
            PathPattern("licdn.com", "/li/track", TrackerCategory.SOCIAL),

            PathPattern("discord.com", "/api/v9/science", TrackerCategory.SOCIAL),
            PathPattern("discord.com", "/api/v10/science", TrackerCategory.SOCIAL),

            PathPattern("reddit.com", "/rpixel", TrackerCategory.SOCIAL),

            PathPattern("tiktok.com", "/i18n/pixel/", TrackerCategory.SOCIAL),

            PathPattern("baidu.com", "/hm.js", TrackerCategory.ANALYTICS),
            PathPattern("baidu.com", "/h.js", TrackerCategory.ANALYTICS),

            PathPattern("yandex.ru", "/metrika/", TrackerCategory.ANALYTICS),

            PathPattern("jsdelivr.net", "/npm/fingerprintjs", TrackerCategory.FINGERPRINTING),
            PathPattern("jsdelivr.net", "/npm/@aspect-build/", TrackerCategory.FINGERPRINTING),
            PathPattern("unpkg.com", "/fingerprintjs", TrackerCategory.FINGERPRINTING),
            PathPattern("cdnjs.cloudflare.com", "/ajax/libs/fingerprintjs", TrackerCategory.FINGERPRINTING),

            PathPattern("", "/coinhive.min.js", TrackerCategory.CRYPTOMINING),
            PathPattern("", "/cryptonight.wasm", TrackerCategory.CRYPTOMINING),

            PathPattern("vk.com", "/rtrg", TrackerCategory.SOCIAL),
            PathPattern("developers.kakao.com", "/sdk", TrackerCategory.SOCIAL),
            PathPattern("redditstatic.com", "/ads", TrackerCategory.SOCIAL),

            PathPattern("yimg.jp", "/images/listing", TrackerCategory.AD_NETWORK),
            PathPattern("search.daum.net", "/adbusiness", TrackerCategory.AD_NETWORK),
            PathPattern("naver.com", "/wcslog.js", TrackerCategory.ANALYTICS)
        )



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



    private val hostPathPatternIndex: Map<String, List<PathPattern>> by lazy {
        PATH_PATTERNS.filter { it.hostSuffix.isNotEmpty() }.groupBy { it.hostSuffix }
    }
    private val wildcardPathPatterns: List<PathPattern> by lazy {
        PATH_PATTERNS.filter { it.hostSuffix.isEmpty() }
    }









    private val TRACKER_SAFELIST = setOf(


        "connect.facebook.net",



        "www.googletagmanager.com",

        "sentry.io", "browser.sentry-cdn.com",
        "bugsnag.com", "sessions.bugsnag.com",


        "cdn.segment.com",

        "cdn.optimizely.com"
    )







    fun checkTracker(url: String): TrackerCategory? {

        val host = com.webtoapp.core.perf.NativePerfEngine.extractHost(url)?.lowercase() ?: return null


        if (TRACKER_SAFELIST.contains(host)) return null


        matchTrackerDomain(host)?.let { return it }


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





    private fun matchTrackerDomain(host: String): TrackerCategory? {
        var domain = host
        while (domain.contains('.')) {
            trackerCategoryMap[domain]?.let { return it }
            domain = domain.substringAfter('.')
        }
        return trackerCategoryMap[domain]
    }






    private fun matchPathPattern(host: String, path: String): TrackerCategory? {


        var suffix = host
        while (suffix.contains('.')) {
            hostPathPatternIndex[suffix]?.let { patterns ->
                for (p in patterns) {
                    if (path.startsWith(p.pathPrefix)) return p.category
                }
            }
            suffix = suffix.substringAfter('.')
        }

        hostPathPatternIndex[suffix]?.let { patterns ->
            for (p in patterns) {
                if (path.startsWith(p.pathPrefix)) return p.category
            }
        }


        for (p in wildcardPathPatterns) {
            if (path.startsWith(p.pathPrefix)) return p.category
        }

        return null
    }




    fun shouldBlock(url: String): Boolean {
        return checkTracker(url) != null
    }





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




    fun getRuleCount(): Int = allTrackerDomains.size + PATH_PATTERNS.size




    fun getCategoryStats(): Map<TrackerCategory, Int> = mapOf(
        TrackerCategory.ANALYTICS to ANALYTICS_TRACKERS.size,
        TrackerCategory.SOCIAL to SOCIAL_TRACKERS.size,
        TrackerCategory.FINGERPRINTING to FINGERPRINTING_TRACKERS.size,
        TrackerCategory.CRYPTOMINING to CRYPTOMINING_TRACKERS.size,
        TrackerCategory.AD_NETWORK to AD_NETWORK_TRACKERS.size
    )
}
