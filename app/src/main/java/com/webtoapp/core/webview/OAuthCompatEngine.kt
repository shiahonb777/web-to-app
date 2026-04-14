package com.webtoapp.core.webview

import android.net.Uri
import com.webtoapp.core.logging.AppLogger

/**
 * use OAuth/ / WebView v3.0.
 *
 * use 99% / / in WebView .
 *
 * ## (v3.0)
 *
 * ### Tier 1: level.
 * Google, Facebook/Meta, Apple, Microsoft, Amazon
 *
 * ### Tier 2: /.
 * Twitter/X, GitHub, Discord, Reddit, LinkedIn, Spotify, Twitch
 *
 * ### Tier 3:
 * Line, Kakao, Naver, WeChat, QQ, Alipay, Douyin/TikTok, Yahoo Japan
 *
 * ### Tier 4:
 * VK, Yandex, Mail.ru, Shopify, Dropbox, Notion, Slack, Zoom.
 *
 * ### Tier 5: /.
 * PayPal, Stripe, Square, Braintree, Alipay, WeChat Pay
 *
 * ### Tier 6: CAPTCHA/.
 * Google reCAPTCHA, hCaptcha, Cloudflare Turnstile
 *
 * ##.
 *
 * ```
 * shouldOverrideUrlLoading() isOAuthUrl() WebView.
 * onPageStarted() getAntiDetectionJs() JS.
 * shouldInterceptRequest() sanitizeHeaders() WebView.
 * onPageFinished() getOAuthBlockDetectionJs() .
 * ```
 */
object OAuthCompatEngine {

    private const val TAG = "OAuthCompatEngine"


    enum class Provider {
        // Tier 1: level.
        GOOGLE, FACEBOOK, APPLE, MICROSOFT, AMAZON,
        // Tier 2: /.
        TWITTER, GITHUB, DISCORD, REDDIT, LINKEDIN, SPOTIFY, TWITCH,
        // Tier 3:
        LINE, KAKAO, NAVER, WECHAT, QQ, ALIPAY, TIKTOK, YAHOO_JAPAN,
        // Tier 4:
        VK, YANDEX, MAILRU, SHOPIFY, DROPBOX, NOTION, SLACK, ZOOM,
        // Tier 5:
        PAYPAL, STRIPE, SQUARE,
        // Tier 6: CAPTCHA
        RECAPTCHA, HCAPTCHA, CLOUDFLARE,
        // use.
        YAHOO,
        GENERIC_OAUTH
    }


    private val HOST_TO_PROVIDER: Map<String, Provider> = mapOf(
        // Tier 1: level .
        // Google
        "accounts.google.com" to Provider.GOOGLE,
        "accounts.youtube.com" to Provider.GOOGLE,
        "myaccount.google.com" to Provider.GOOGLE,
        // Facebook / Meta
        "www.facebook.com" to Provider.FACEBOOK,
        "m.facebook.com" to Provider.FACEBOOK,
        "web.facebook.com" to Provider.FACEBOOK,
        "facebook.com" to Provider.FACEBOOK,
        "www.instagram.com" to Provider.FACEBOOK,
        "www.messenger.com" to Provider.FACEBOOK,
        "www.threads.net" to Provider.FACEBOOK,
        // Apple
        "appleid.apple.com" to Provider.APPLE,
        "idmsa.apple.com" to Provider.APPLE,
        // Microsoft
        "login.microsoftonline.com" to Provider.MICROSOFT,
        "login.live.com" to Provider.MICROSOFT,
        "login.windows.net" to Provider.MICROSOFT,
        "login.microsoft.com" to Provider.MICROSOFT,
        "account.live.com" to Provider.MICROSOFT,
        // Amazon
        "www.amazon.com" to Provider.AMAZON,
        "apac.account.amazon.com" to Provider.AMAZON,
        "na.account.amazon.com" to Provider.AMAZON,
        "eu.account.amazon.com" to Provider.AMAZON,
        "www.amazon.co.jp" to Provider.AMAZON,
        "www.amazon.co.uk" to Provider.AMAZON,
        "www.amazon.de" to Provider.AMAZON,
        "www.amazon.in" to Provider.AMAZON,

        // Tier 2: / .
        // Twitter / X
        "api.twitter.com" to Provider.TWITTER,
        "twitter.com" to Provider.TWITTER,
        "api.x.com" to Provider.TWITTER,
        "x.com" to Provider.TWITTER,
        // GitHub
        "github.com" to Provider.GITHUB,
        // Discord
        "discord.com" to Provider.DISCORD,
        "discordapp.com" to Provider.DISCORD,
        // Reddit
        "www.reddit.com" to Provider.REDDIT,
        "old.reddit.com" to Provider.REDDIT,
        "ssl.reddit.com" to Provider.REDDIT,
        // LinkedIn
        "www.linkedin.com" to Provider.LINKEDIN,
        "linkedin.com" to Provider.LINKEDIN,
        // Spotify
        "accounts.spotify.com" to Provider.SPOTIFY,
        // Twitch
        "id.twitch.tv" to Provider.TWITCH,
        "www.twitch.tv" to Provider.TWITCH,

        // Tier 3: .
        // Line
        "access.line.me" to Provider.LINE,
        "liff.line.me" to Provider.LINE,
        // Kakao
        "accounts.kakao.com" to Provider.KAKAO,
        "kauth.kakao.com" to Provider.KAKAO,
        // Naver
        "nid.naver.com" to Provider.NAVER,
        "access.naver.com" to Provider.NAVER,
        // WeChat
        "open.weixin.qq.com" to Provider.WECHAT,
        "mp.weixin.qq.com" to Provider.WECHAT,
        // QQ
        "graph.qq.com" to Provider.QQ,
        "connect.qq.com" to Provider.QQ,
        "ssl.ptlogin2.qq.com" to Provider.QQ,
        "xui.ptlogin2.qq.com" to Provider.QQ,
        // Alipay
        "auth.alipay.com" to Provider.ALIPAY,
        "openauth.alipay.com" to Provider.ALIPAY,
        // TikTok / Douyin
        "www.tiktok.com" to Provider.TIKTOK,
        "open-api.tiktok.com" to Provider.TIKTOK,
        "open.douyin.com" to Provider.TIKTOK,
        // Yahoo Japan
        "login.yahoo.co.jp" to Provider.YAHOO_JAPAN,
        "auth.login.yahoo.co.jp" to Provider.YAHOO_JAPAN,
        // Yahoo Global
        "login.yahoo.com" to Provider.YAHOO,
        "api.login.yahoo.com" to Provider.YAHOO,

        // Tier 4: .
        // VK
        "oauth.vk.com" to Provider.VK,
        "id.vk.com" to Provider.VK,
        "login.vk.com" to Provider.VK,
        // Yandex
        "oauth.yandex.com" to Provider.YANDEX,
        "oauth.yandex.ru" to Provider.YANDEX,
        "passport.yandex.com" to Provider.YANDEX,
        "passport.yandex.ru" to Provider.YANDEX,
        // Mail.ru
        "connect.mail.ru" to Provider.MAILRU,
        "o2.mail.ru" to Provider.MAILRU,
        // Shopify
        "accounts.shopify.com" to Provider.SHOPIFY,
        // Dropbox
        "www.dropbox.com" to Provider.DROPBOX,
        // Notion
        "www.notion.so" to Provider.NOTION,
        // Slack
        "slack.com" to Provider.SLACK,
        "app.slack.com" to Provider.SLACK,
        // Zoom
        "zoom.us" to Provider.ZOOM,
        "us04web.zoom.us" to Provider.ZOOM,
        "us05web.zoom.us" to Provider.ZOOM,

        // Tier 5: .
        "www.paypal.com" to Provider.PAYPAL,
        "paypal.com" to Provider.PAYPAL,
        "checkout.stripe.com" to Provider.STRIPE,
        "billing.stripe.com" to Provider.STRIPE,
        "squareup.com" to Provider.SQUARE,
        "checkout.square.site" to Provider.SQUARE,

        // Tier 6: CAPTCHA
        "www.google.com" to Provider.RECAPTCHA, // /recaptcha 路径检测
        "www.gstatic.com" to Provider.RECAPTCHA,
        "hcaptcha.com" to Provider.HCAPTCHA,
        "newassets.hcaptcha.com" to Provider.HCAPTCHA,
        "challenges.cloudflare.com" to Provider.CLOUDFLARE
    )

    /**
     * + rules.
     */
    private val HOST_PATH_RULES: Map<String, List<String>> = mapOf(
        // Facebook/Meta /OAuth.
        "www.facebook.com" to listOf("/login", "/v", "/dialog/oauth", "/oauth"),
        "m.facebook.com" to listOf("/login", "/v", "/dialog/oauth", "/oauth"),
        "web.facebook.com" to listOf("/login", "/v", "/dialog/oauth", "/oauth"),
        "facebook.com" to listOf("/login", "/v", "/dialog/oauth", "/oauth"),
        "www.instagram.com" to listOf("/accounts/login", "/oauth"),
        "www.messenger.com" to listOf("/login"),
        "www.threads.net" to listOf("/login"),

        // Twitter/X
        "twitter.com" to listOf("/i/oauth2", "/oauth", "/login/oauth", "/i/flow/login"),
        "x.com" to listOf("/i/oauth2", "/oauth", "/login/oauth", "/i/flow/login"),
        "api.twitter.com" to listOf("/oauth", "/oauth2"),
        "api.x.com" to listOf("/oauth", "/oauth2"),

        // GitHub
        "github.com" to listOf("/login/oauth", "/login", "/sessions", "/session"),

        // Discord
        "discord.com" to listOf("/login", "/oauth2", "/authorize", "/register"),
        "discordapp.com" to listOf("/login", "/oauth2", "/authorize"),

        // Reddit
        "www.reddit.com" to listOf("/login", "/account/login", "/api/v1/authorize"),
        "old.reddit.com" to listOf("/login"),
        "ssl.reddit.com" to listOf("/api/v1/authorize"),

        // LinkedIn
        "www.linkedin.com" to listOf("/oauth", "/login", "/uas/login", "/checkpoint", "/authwall"),
        "linkedin.com" to listOf("/oauth", "/login", "/uas/login"),

        // Amazon
        "www.amazon.com" to listOf("/ap/signin", "/ap/oa", "/gp/sign-in", "/ap/register"),
        "www.amazon.co.jp" to listOf("/ap/signin", "/ap/oa", "/gp/sign-in"),
        "www.amazon.co.uk" to listOf("/ap/signin", "/ap/oa", "/gp/sign-in"),
        "www.amazon.de" to listOf("/ap/signin", "/ap/oa", "/gp/sign-in"),
        "www.amazon.in" to listOf("/ap/signin", "/ap/oa", "/gp/sign-in"),

        // TikTok
        "www.tiktok.com" to listOf("/login", "/auth/authorize", "/auth"),

        // Dropbox
        "www.dropbox.com" to listOf("/login", "/oauth2/authorize"),

        // Notion
        "www.notion.so" to listOf("/login", "/signup"),

        // Slack
        "slack.com" to listOf("/signin", "/oauth", "/openid/connect"),
        "app.slack.com" to listOf("/auth"),

        // Zoom
        "zoom.us" to listOf("/signin", "/oauth/authorize", "/login"),

        // PayPal
        "www.paypal.com" to listOf("/signin", "/connect", "/webapps/auth", "/checkout"),
        "paypal.com" to listOf("/signin", "/connect", "/webapps/auth"),

        // Twitch
        "www.twitch.tv" to listOf("/login"),

        // CAPTCHA.
        "www.google.com" to listOf("/recaptcha"),
        "www.gstatic.com" to listOf("/recaptcha")
    )

    /**
     * is /OAuth use.
     */
    private val FULL_DOMAIN_OAUTH_HOSTS: Set<String> = setOf(
        // Google
        "accounts.google.com", "accounts.youtube.com", "myaccount.google.com",
        // Apple
        "appleid.apple.com", "idmsa.apple.com",
        // Microsoft
        "login.microsoftonline.com", "login.live.com", "login.windows.net",
        "login.microsoft.com", "account.live.com",
        // Line
        "access.line.me", "liff.line.me",
        // Kakao
        "accounts.kakao.com", "kauth.kakao.com",
        // Naver
        "nid.naver.com", "access.naver.com",
        // Yahoo
        "login.yahoo.com", "api.login.yahoo.com",
        "login.yahoo.co.jp", "auth.login.yahoo.co.jp",
        // WeChat
        "open.weixin.qq.com", "mp.weixin.qq.com",
        // QQ
        "graph.qq.com", "connect.qq.com", "ssl.ptlogin2.qq.com", "xui.ptlogin2.qq.com",
        // Alipay
        "auth.alipay.com", "openauth.alipay.com",
        // TikTok/Douyin
        "open-api.tiktok.com", "open.douyin.com",
        // VK
        "oauth.vk.com", "id.vk.com", "login.vk.com",
        // Yandex
        "oauth.yandex.com", "oauth.yandex.ru", "passport.yandex.com", "passport.yandex.ru",
        // Mail.ru
        "connect.mail.ru", "o2.mail.ru",
        // Spotify
        "accounts.spotify.com",
        // Twitch
        "id.twitch.tv",
        // Shopify
        "accounts.shopify.com",
        // Stripe
        "checkout.stripe.com", "billing.stripe.com",
        // Square
        "squareup.com", "checkout.square.site",
        // Amazon regional
        "apac.account.amazon.com", "na.account.amazon.com", "eu.account.amazon.com",
        // CAPTCHA
        "hcaptcha.com", "newassets.hcaptcha.com", "challenges.cloudflare.com"
    )

    /**
     * use OAuth/.
     */
    private val GENERIC_OAUTH_PATH_PATTERNS = listOf(
        "/oauth", "/oauth2", "/o/oauth2", "/authorize", "/auth/login",
        "/login/oauth", "/signin/oauth", "/connect/authorize",
        "/.well-known/openid-configuration", "/openid/connect",
        "/auth/realms", // Keycloak
        "/api/v1/authorize", // Generic REST APIs
        "/sso/login", "/saml/login", "/cas/login" // Enterprise SSO
    )

    /**
     * OAuth.
     */
    val ALL_OAUTH_HOSTS: Set<String> = HOST_TO_PROVIDER.keys


    /**
     * URL is as OAuth/ / /CAPTCHA.
     */
    fun isOAuthUrl(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        val path = uri.path?.lowercase() ?: ""

        // 1.
        if (host in FULL_DOMAIN_OAUTH_HOSTS) return true

        // 2. + rules.
        val pathRules = HOST_PATH_RULES[host]
        if (pathRules != null) {
            if (pathRules.any { prefix -> path.startsWith(prefix) }) return true
            return false
        }

        // 3. Google.
        if (host.endsWith(".google.com") || host == "google.com") {
            if (path.startsWith("/o/oauth2") || path.startsWith("/signin/oauth") ||
                path.startsWith("/servicelogin") || path.startsWith("/accounts") ||
                path.startsWith("/recaptcha")) {
                return true
            }
        }

        // 4. /QQ.
        if (host.endsWith(".qq.com") && (path.startsWith("/oauth") || path.startsWith("/connect"))) {
            return true
        }

        // 5.
        if (host.endsWith(".alipay.com") && (path.startsWith("/login") || path.startsWith("/oauth") || path.startsWith("/auth"))) {
            return true
        }

        // 6. Yandex.
        if (host.endsWith(".yandex.ru") || host.endsWith(".yandex.com")) {
            if (path.startsWith("/passport") || path.startsWith("/oauth") || path.startsWith("/auth")) {
                return true
            }
        }

        // 7. Shopify (*.myshopify.com/account/login)
        if (host.endsWith(".myshopify.com") && (path.startsWith("/account/login") || path.startsWith("/account"))) {
            return true
        }

        // 8. use OAuth.
        if (GENERIC_OAUTH_PATH_PATTERNS.any { pattern -> path.startsWith(pattern) }) {
            val query = uri.query?.lowercase() ?: ""
            if (query.contains("client_id") || query.contains("redirect_uri") ||
                query.contains("response_type") || query.contains("scope") ||
                query.contains("openid") || query.contains("code_challenge")) {
                return true
            }
        }

        return false
    }

    /**
     * Get URL OAuth.
     */
    fun getProviderType(url: String): Provider? {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
        val host = uri.host?.lowercase() ?: return null
        val path = uri.path?.lowercase() ?: ""

        HOST_TO_PROVIDER[host]?.let { provider ->
            val pathRules = HOST_PATH_RULES[host]
            if (pathRules != null) {
                if (!pathRules.any { prefix -> path.startsWith(prefix) }) return null
            }
            return provider
        }

        // Google.
        if (host.endsWith(".google.com") || host == "google.com") {
            if (path.startsWith("/recaptcha")) return Provider.RECAPTCHA
            if (path.startsWith("/o/oauth2") || path.startsWith("/signin/oauth") ||
                path.startsWith("/servicelogin") || path.startsWith("/accounts")) {
                return Provider.GOOGLE
            }
        }

        // /QQ.
        if (host.endsWith(".qq.com")) return Provider.QQ
        if (host.endsWith(".alipay.com")) return Provider.ALIPAY
        // Yandex
        if (host.endsWith(".yandex.ru") || host.endsWith(".yandex.com")) return Provider.YANDEX
        // Shopify
        if (host.endsWith(".myshopify.com")) return Provider.SHOPIFY

        if (isOAuthUrl(url)) return Provider.GENERIC_OAUTH
        return null
    }

    /**
     * Get URL JavaScript.
     *
     * v3.0: = use +.
     */
    fun getAntiDetectionJs(url: String): String? {
        val provider = getProviderType(url) ?: return null
        AppLogger.d(TAG, "Anti-detection JS for: $provider (${url.take(60)}...)")

        val sb = StringBuilder()
        sb.append(BASE_ANTI_DETECTION_JS)

        when (provider) {
            // Tier 1
            Provider.GOOGLE -> sb.append(GOOGLE_ANTI_DETECTION_JS)
            Provider.FACEBOOK -> sb.append(FACEBOOK_ANTI_DETECTION_JS)
            Provider.MICROSOFT -> sb.append(MICROSOFT_ANTI_DETECTION_JS)
            Provider.APPLE -> sb.append(APPLE_ANTI_DETECTION_JS)
            Provider.AMAZON -> sb.append(AMAZON_ANTI_DETECTION_JS)
            // Tier 2
            Provider.TWITTER -> sb.append(TWITTER_ANTI_DETECTION_JS)
            Provider.GITHUB -> sb.append(GITHUB_ANTI_DETECTION_JS)
            Provider.DISCORD -> sb.append(DISCORD_ANTI_DETECTION_JS)
            Provider.REDDIT -> sb.append(REDDIT_ANTI_DETECTION_JS)
            Provider.LINKEDIN -> sb.append(LINKEDIN_ANTI_DETECTION_JS)
            Provider.SPOTIFY -> sb.append(SPOTIFY_ANTI_DETECTION_JS)
            Provider.TWITCH -> sb.append(TWITCH_ANTI_DETECTION_JS)
            // Tier 3:
            Provider.LINE, Provider.KAKAO -> sb.append(LINE_KAKAO_ANTI_DETECTION_JS)
            Provider.NAVER -> sb.append(NAVER_ANTI_DETECTION_JS)
            Provider.WECHAT, Provider.QQ -> sb.append(WECHAT_QQ_ANTI_DETECTION_JS)
            Provider.ALIPAY -> sb.append(ALIPAY_ANTI_DETECTION_JS)
            Provider.TIKTOK -> sb.append(TIKTOK_ANTI_DETECTION_JS)
            Provider.YAHOO_JAPAN -> sb.append(YAHOO_JP_ANTI_DETECTION_JS)
            // Tier 4
            Provider.VK -> sb.append(VK_ANTI_DETECTION_JS)
            Provider.YANDEX -> sb.append(YANDEX_ANTI_DETECTION_JS)
            Provider.MAILRU -> sb.append(MAILRU_ANTI_DETECTION_JS)
            Provider.SHOPIFY -> sb.append(SHOPIFY_ANTI_DETECTION_JS)
            Provider.DROPBOX -> sb.append(DROPBOX_ANTI_DETECTION_JS)
            Provider.NOTION -> sb.append(NOTION_ANTI_DETECTION_JS)
            Provider.SLACK -> sb.append(SLACK_ANTI_DETECTION_JS)
            Provider.ZOOM -> sb.append(ZOOM_ANTI_DETECTION_JS)
            // Tier 5:
            Provider.PAYPAL -> sb.append(PAYPAL_ANTI_DETECTION_JS)
            Provider.STRIPE -> sb.append(STRIPE_ANTI_DETECTION_JS)
            Provider.SQUARE -> sb.append(STRIPE_ANTI_DETECTION_JS) // 类似策略
            // Tier 6: CAPTCHA
            Provider.RECAPTCHA -> sb.append(RECAPTCHA_ANTI_DETECTION_JS)
            Provider.HCAPTCHA -> sb.append(HCAPTCHA_ANTI_DETECTION_JS)
            Provider.CLOUDFLARE -> sb.append(CLOUDFLARE_ANTI_DETECTION_JS)
            // use.
            Provider.YAHOO -> sb.append(YAHOO_ANTI_DETECTION_JS)
            Provider.GENERIC_OAUTH -> { /* 基础层已足够 */ }
        }

        return sb.toString()
    }

    /**
     * request.
     */
    fun sanitizeHeaders(url: String, headers: Map<String, String>): Map<String, String> {
        if (!isOAuthUrl(url)) return headers
        val clean = headers.toMutableMap()
        clean.remove("X-Requested-With")
        clean.remove("x-requested-with")
        com.webtoapp.core.kernel.BrowserKernel.getCleanUserAgent()?.let { ua ->
            clean["User-Agent"] = ua
        }
        return clean
    }

    /**
     * Check HTTP is as OAuth.
     */
    fun isOAuthBlockedError(statusCode: Int, url: String): Boolean {
        if (!isOAuthUrl(url)) return false
        return statusCode == 403 || (statusCode == 400 && getProviderType(url) == Provider.GOOGLE)
    }


    /**
     * URL is to Chrome Custom Tab.
     * Google OAuth CCT.
     */
    fun shouldRedirectToCustomTab(url: String): Boolean {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val host = uri.host?.lowercase() ?: return false
        val path = uri.path?.lowercase() ?: ""

        if (host == "accounts.google.com") {
            if (path.startsWith("/recaptcha") || path.startsWith("/gsi/")) return false
            return true
        }

        if ((host.endsWith(".google.com") || host == "google.com") &&
            (path.startsWith("/o/oauth2") || path.startsWith("/signin/oauth"))) {
            return true
        }

        return false
    }

    /**
     * JS.
     */
    fun getOAuthBlockDetectionJs(): String {
        return """(function(){
            'use strict';
            if(window.__wta_oauth_block_check__)return;
            window.__wta_oauth_block_check__=true;
            setTimeout(function(){
                try{
                    var t=(document.body&&document.body.innerText)||'';
                    var blocked=
                        t.indexOf('This browser or app may not be secure')!==-1||
                        t.indexOf('此浏览器或应用可能不安全')!==-1||
                        t.indexOf('このブラウザまたはアプリは安全でない可能性があります')!==-1||
                        t.indexOf('이 브라우저 또는 앱은 안전하지 않을 수 있습니다')!==-1||
                        t.indexOf('Diese Browser oder App ist möglicherweise nicht sicher')!==-1||
                        t.indexOf('Ce navigateur ou cette application ne sont peut-être pas sécurisés')!==-1||
                        t.indexOf('Este navegador o aplicación podría no ser seguro')!==-1||
                        t.indexOf('Этот браузер или приложение могут быть небезопасными')!==-1||
                        t.indexOf('Bu tarayıcı veya uygulama güvenli olmayabilir')!==-1||
                        t.indexOf('هذا المتصفح أو التطبيق قد لا يكون آمنًا')!==-1||
                        t.indexOf('disallowed_useragent')!==-1||
                        t.indexOf("Couldn't sign you in")!==-1||
                        t.indexOf('无法让您登录')!==-1||
                        t.indexOf('ログインできませんでした')!==-1||
                        t.indexOf('로그인할 수 없음')!==-1;
                    if(blocked&&typeof NativeOAuthBridge!=='undefined'){
                        NativeOAuthBridge.onOAuthBlocked(window.location.href);
                    }
                }catch(e){}
            },1500);
        })();""".trimIndent()
    }


    /**
     * use .
     *
     * :
     * - navigator.webdriver=false
     * - window.chrome.
     * - plugins/vendor/product.
     * - outerWidth/outerHeight.
     * - Java Bridge /.
     * - ServiceWorker stub
     * - BroadcastChannel stub
     * - Notification API stub
     * - Error stack.
     */
    private val BASE_ANTI_DETECTION_JS = """(function(){'use strict';
        if(window.__wta_oauth_compat__)return;window.__wta_oauth_compat__=true;

        // navigator.webdriver
        try{Object.defineProperty(navigator,'webdriver',{get:function(){return false},enumerable:true,configurable:true});}catch(e){}

        // window.chrome.
        try{
            if(!window.chrome)window.chrome={};
            if(!window.chrome.runtime)window.chrome.runtime={
                OnInstalledReason:{CHROME_UPDATE:'chrome_update',INSTALL:'install',SHARED_MODULE_UPDATE:'shared_module_update',UPDATE:'update'},
                PlatformOs:{ANDROID:'android',CROS:'cros',LINUX:'linux',MAC:'mac',WIN:'win'},
                connect:function(e){return{name:'',onDisconnect:{addListener:function(){},removeListener:function(){}},onMessage:{addListener:function(){},removeListener:function(){}},postMessage:function(){},disconnect:function(){}};},
                sendMessage:function(){},id:undefined,getManifest:function(){return{}},getURL:function(p){return''}
            };
            if(!window.chrome.app)window.chrome.app={
                InstallState:{DISABLED:'disabled',INSTALLED:'installed',NOT_INSTALLED:'not_installed'},
                getDetails:function(){return null},getIsInstalled:function(){return false},isInstalled:false
            };
            if(!window.chrome.loadTimes)window.chrome.loadTimes=function(){return{commitLoadTime:Date.now()/1000,firstPaintTime:Date.now()/1000,navigationType:'Other',wasFetchedViaSpdy:true,wasNpnNegotiated:true,npnNegotiatedProtocol:'h2',connectionInfo:'h2'};};
            if(!window.chrome.csi)window.chrome.csi=function(){return{onloadT:Date.now(),startE:Date.now()-300,pageT:performance.now(),tran:15};};
        }catch(e){}

        // plugins + mimeTypes
        try{Object.defineProperty(navigator,'plugins',{get:function(){return{length:5,0:{name:'PDF Viewer'},1:{name:'Chrome PDF Plugin'},2:{name:'Chrome PDF Viewer'},3:{name:'Native Client'},4:{name:'Chromium PDF Plugin'},item:function(i){return this[i]},namedItem:function(n){for(var i=0;i<5;i++){if(this[i].name===n)return this[i]}return null},refresh:function(){}}},enumerable:true});}catch(e){}
        try{Object.defineProperty(navigator,'mimeTypes',{get:function(){return{length:4,0:{type:'application/pdf',suffixes:'pdf'},1:{type:'application/x-google-chrome-pdf',suffixes:'pdf'},2:{type:'application/x-nacl',suffixes:''},3:{type:'application/x-pnacl',suffixes:''},item:function(i){return this[i]},namedItem:function(n){for(var i=0;i<4;i++){if(this[i].type===n)return this[i]}return null}}},enumerable:true});}catch(e){}

        // vendor / product
        try{Object.defineProperty(navigator,'vendor',{get:function(){return'Google Inc.'},enumerable:true});}catch(e){}
        try{Object.defineProperty(navigator,'product',{get:function(){return'Gecko'},enumerable:true});}catch(e){}
        try{Object.defineProperty(navigator,'productSub',{get:function(){return'20030107'},enumerable:true});}catch(e){}

        // outerWidth/outerHeight
        try{
            if(!window.outerWidth||window.outerWidth===0)Object.defineProperty(window,'outerWidth',{get:function(){return window.innerWidth},configurable:true});
            if(!window.outerHeight||window.outerHeight===0)Object.defineProperty(window,'outerHeight',{get:function(){return window.innerHeight+85},configurable:true});
        }catch(e){}

        // Java Bridge.
        try{
            var bn=[];for(var k in window){if(k.toLowerCase().indexOf('java')===0||k.indexOf('_')===0&&typeof window[k]==='object')bn.push(k);}
            bn.forEach(function(n){try{Object.defineProperty(window,n,{get:function(){return undefined},configurable:true})}catch(e){}});
        }catch(e){}

        try{
            ['__selenium_unwrapped','__webdriver_evaluate','__webdriver_script_function',
             '__webview_bridge','accessibility','accessibilityTraversal',
             'domAutomation','domAutomationController','_phantom','callPhantom',
             '__nightmare','DiscordNative','ReactNativeWebView','FBNative','__fbNative'
            ].forEach(function(f){try{delete window[f]}catch(e){}});
        }catch(e){}

        // ServiceWorker stub
        try{if(!navigator.serviceWorker){Object.defineProperty(navigator,'serviceWorker',{get:function(){return{
            register:function(){return Promise.reject(new DOMException('SecurityError'))},
            ready:Promise.resolve({active:null}),controller:null,
            getRegistrations:function(){return Promise.resolve([])},
            addEventListener:function(){},removeEventListener:function(){}
        }},configurable:true,enumerable:true});}}catch(e){}

        // BroadcastChannel stub
        try{if(!window.BroadcastChannel){window.BroadcastChannel=function(n){this.name=n;this.onmessage=null;};
            BroadcastChannel.prototype.postMessage=function(){};BroadcastChannel.prototype.close=function(){};
            BroadcastChannel.prototype.addEventListener=function(){};BroadcastChannel.prototype.removeEventListener=function(){};
        }}catch(e){}

        // Notification API
        try{if(typeof Notification==='undefined'){window.Notification=function(){};Notification.permission='default';
            Notification.requestPermission=function(cb){var r='default';if(cb)cb(r);return Promise.resolve(r)};
            Notification.maxActions=2;Notification.prototype.close=function(){};
        }}catch(e){}

        // document.visibilityState
        try{Object.defineProperty(document,'visibilityState',{get:function(){return'visible'},configurable:true});
            Object.defineProperty(document,'hidden',{get:function(){return false},configurable:true});}catch(e){}

        // document.hasFocus
        try{document.hasFocus=function(){return true}}catch(e){}

        // Error stack.
        try{
            var _ops=Error.prepareStackTrace;
            Error.prepareStackTrace=function(err,stack){
                stack=stack.filter(function(f){var fn=f.getFileName()||'';return fn.indexOf('injectedScript')===-1&&fn.indexOf('evaluateJavascript')===-1;});
                return _ops?_ops(err,stack):err.toString()+'\\n'+stack.map(function(f){return'    at '+f}).join('\\n');
            };
        }catch(e){}
    })();""".trimIndent()


    private val GOOGLE_ANTI_DETECTION_JS = """(function(){'use strict';
        // chrome.webstore.
        try{if(window.chrome&&!window.chrome.webstore)window.chrome.webstore={onInstallStageChanged:{},onDownloadProgress:{},install:function(){}};}catch(e){}
        // Battery API
        try{if(!navigator.getBattery)navigator.getBattery=function(){return Promise.resolve({charging:true,chargingTime:0,dischargingTime:Infinity,level:1,addEventListener:function(){},removeEventListener:function(){}})}}catch(e){}
        // Permissions API.
        try{if(navigator.permissions){var _oq=navigator.permissions.query;navigator.permissions.query=function(d){
            if(d&&d.name==='notifications')return Promise.resolve({state:'prompt',onchange:null,addEventListener:function(){},removeEventListener:function(){}});
            return _oq.call(navigator.permissions,d)};}}catch(e){}
        // Credential Management (Google One Tap)
        try{if(!navigator.credentials){Object.defineProperty(navigator,'credentials',{get:function(){return{
            create:function(){return Promise.resolve(null)},get:function(){return Promise.resolve(null)},
            preventSilentAccess:function(){return Promise.resolve()},store:function(){return Promise.resolve()}
        }},enumerable:true,configurable:true})}}catch(e){}
        // FedCM API (Federated Credential Management Chrome 108+)
        try{if(!window.IdentityCredential){window.IdentityCredential=function(){};
            window.IdentityProvider={getUserInfo:function(){return Promise.resolve([])}};}}catch(e){}
    })();""".trimIndent()

    private val FACEBOOK_ANTI_DETECTION_JS = """(function(){'use strict';
        // ReactNativeWebView / FBNative bridge.
        try{delete window.ReactNativeWebView;delete window.FBNative;delete window.__fbNative;}catch(e){}
        // SharedWorker.
        try{if(!window.SharedWorker){window.SharedWorker=function(){return{port:{start:function(){},postMessage:function(){},addEventListener:function(){},removeEventListener:function(){}}}}}}catch(e){}
        // FB window.name is "webview".
        try{if(window.name&&window.name.toLowerCase().indexOf('webview')!==-1)window.name=''}catch(e){}
        // FB Pixel / conversion API cookie Supports.
        try{Object.defineProperty(navigator,'cookieEnabled',{get:function(){return true},configurable:true})}catch(e){}
        // IndexedDB.
        try{if(!window.indexedDB)window.indexedDB=window.mozIndexedDB||window.webkitIndexedDB||window.msIndexedDB}catch(e){}
    })();""".trimIndent()

    private val MICROSOFT_ANTI_DETECTION_JS = """(function(){'use strict';
        // IE/Edge.
        try{delete window.MSStream;delete navigator.msMaxTouchPoints}catch(e){}
        // IndexedDB.
        try{if(!window.indexedDB)window.indexedDB=window.mozIndexedDB||window.webkitIndexedDB||window.msIndexedDB}catch(e){}
        // Crypto.subtle (MSAL PKCE flow)
        try{if(!window.crypto||!window.crypto.subtle){
            if(!window.crypto)window.crypto={};
            if(!window.crypto.subtle)window.crypto.subtle={
                digest:function(a,d){return Promise.resolve(new ArrayBuffer(32))},
                generateKey:function(){return Promise.resolve({})},
                exportKey:function(){return Promise.resolve(new ArrayBuffer(32))}
            }
        }}catch(e){}
        // navigator.credentials (Windows Hello / FIDO2)
        try{if(!navigator.credentials){Object.defineProperty(navigator,'credentials',{get:function(){return{
            create:function(){return Promise.resolve(null)},get:function(){return Promise.resolve(null)},
            preventSilentAccess:function(){return Promise.resolve()},store:function(){return Promise.resolve()}
        }},enumerable:true,configurable:true})}}catch(e){}
    })();""".trimIndent()

    private val APPLE_ANTI_DETECTION_JS = """(function(){'use strict';
        // ApplePaySession.
        try{if(!window.ApplePaySession){window.ApplePaySession={canMakePayments:function(){return false},supportsVersion:function(){return false}}}}catch(e){}
        // Safari-specific DOM features
        try{if(!document.requestStorageAccess)document.requestStorageAccess=function(){return Promise.resolve()}}catch(e){}
        try{if(!document.hasStorageAccess)document.hasStorageAccess=function(){return Promise.resolve(true)}}catch(e){}
    })();""".trimIndent()

    private val AMAZON_ANTI_DETECTION_JS = """(function(){'use strict';
        // Amazon WebView cookie.
        try{Object.defineProperty(navigator,'cookieEnabled',{get:function(){return true},configurable:true})}catch(e){}
        // Amazon Cognito SDK.
        try{if(!window.crypto||!window.crypto.getRandomValues){
            if(!window.crypto)window.crypto={};
            window.crypto.getRandomValues=function(arr){for(var i=0;i<arr.length;i++)arr[i]=Math.floor(Math.random()*256);return arr}
        }}catch(e){}
        // localStorage (Amazon session)
        try{if(!window.localStorage){var _s={};window.localStorage={getItem:function(k){return _s[k]||null},setItem:function(k,v){_s[k]=String(v)},removeItem:function(k){delete _s[k]},clear:function(){_s={}},key:function(i){return Object.keys(_s)[i]||null},get length(){return Object.keys(_s).length}}}}catch(e){}
    })();""".trimIndent()


    private val TWITTER_ANTI_DETECTION_JS = """(function(){'use strict';
        // Twitter/X use React large IntersectionObserver.
        try{if(!window.IntersectionObserver){window.IntersectionObserver=function(cb,opt){this.observe=function(){};this.unobserve=function(){};this.disconnect=function(){};this.takeRecords=function(){return[]}};
            window.IntersectionObserverEntry=function(){}}}catch(e){}
        // ResizeObserver (Twitter cards)
        try{if(!window.ResizeObserver){window.ResizeObserver=function(cb){this.observe=function(){};this.unobserve=function(){};this.disconnect=function(){}}}}catch(e){}
        // Performance Observer (Twitter analytics)
        try{if(!window.PerformanceObserver){window.PerformanceObserver=function(cb){this.observe=function(){};this.disconnect=function(){};this.takeRecords=function(){return[]}}}}catch(e){}
    })();""".trimIndent()

    private val GITHUB_ANTI_DETECTION_JS = """(function(){'use strict';
        // GitHub use WebAuthn/FIDO2 for 2FA.
        try{if(!navigator.credentials){Object.defineProperty(navigator,'credentials',{get:function(){return{
            create:function(opt){if(opt&&opt.publicKey)return Promise.reject(new DOMException('Not allowed','NotAllowedError'));return Promise.resolve(null)},
            get:function(opt){if(opt&&opt.publicKey)return Promise.reject(new DOMException('Not allowed','NotAllowedError'));return Promise.resolve(null)},
            preventSilentAccess:function(){return Promise.resolve()},store:function(){return Promise.resolve()}
        }},enumerable:true,configurable:true})}}catch(e){}
        // Clipboard API (GitHub code copy)
        try{if(!navigator.clipboard){Object.defineProperty(navigator,'clipboard',{get:function(){return{
            writeText:function(t){return Promise.resolve()},readText:function(){return Promise.resolve('')},
            write:function(){return Promise.resolve()},read:function(){return Promise.resolve([])}
        }},enumerable:true,configurable:true})}}catch(e){}
    })();""".trimIndent()

    private val DISCORD_ANTI_DETECTION_JS = """(function(){'use strict';
        // Discord /.
        try{delete window.DiscordNative;delete window.__OVERLAY__;delete window.__DISCORD_OVERLAY__}catch(e){}
        // Brave.
        try{if(navigator.brave)delete navigator.brave}catch(e){}
        // WebSocket (Discord Gateway)
        try{if(!window.WebSocket){/* WebSocket 应该在 WebView 中可用, 只是确保 */}}catch(e){}
        // AudioContext.
        try{if(!window.AudioContext&&window.webkitAudioContext)window.AudioContext=window.webkitAudioContext}catch(e){}
    })();""".trimIndent()

    private val REDDIT_ANTI_DETECTION_JS = """(function(){'use strict';
        // Reddit use IntersectionObserver.
        try{if(!window.IntersectionObserver){window.IntersectionObserver=function(cb,opt){this.observe=function(){};this.unobserve=function(){};this.disconnect=function(){};this.takeRecords=function(){return[]}}}}catch(e){}
        // Reddit Vault (crypto features)
        try{if(!window.crypto||!window.crypto.subtle){if(!window.crypto)window.crypto={};if(!window.crypto.subtle)window.crypto.subtle={digest:function(){return Promise.resolve(new ArrayBuffer(32))}}}}catch(e){}
    })();""".trimIndent()

    private val LINKEDIN_ANTI_DETECTION_JS = """(function(){'use strict';
        // LinkedIn cookies.
        try{Object.defineProperty(navigator,'cookieEnabled',{get:function(){return true},configurable:true})}catch(e){}
        // LinkedIn window.name.
        try{if(window.name&&window.name.toLowerCase().indexOf('webview')!==-1)window.name=''}catch(e){}
        // LinkedIn Insight Tag CSP
        try{if(!window.performance||!window.performance.getEntriesByType)if(window.performance)window.performance.getEntriesByType=function(){return[]}}catch(e){}
    })();""".trimIndent()

    private val SPOTIFY_ANTI_DETECTION_JS = """(function(){'use strict';
        // Spotify Web Playback SDK EME (Encrypted Media Extensions)
        try{if(!navigator.requestMediaKeySystemAccess){navigator.requestMediaKeySystemAccess=function(){return Promise.reject(new DOMException('Not supported','NotSupportedError'))}}}catch(e){}
        // Spotify use Fetch API.
        try{if(!window.fetch){window.fetch=function(url,opt){return Promise.reject(new TypeError('NetworkError'))}}}catch(e){}
    })();""".trimIndent()

    private val TWITCH_ANTI_DETECTION_JS = """(function(){'use strict';
        // Twitch use MutationObserver large DOM.
        try{if(!window.MutationObserver&&window.WebKitMutationObserver)window.MutationObserver=window.WebKitMutationObserver}catch(e){}
        // Twitch WebGL.
    })();""".trimIndent()


    private val LINE_KAKAO_ANTI_DETECTION_JS = """(function(){'use strict';
        // Line LIFF bridge.
        try{delete window.liff;delete window.__liff}catch(e){}
        // Kakao SDK bridge.
        try{delete window.Kakao;delete window.__kakao__}catch(e){}
        // LIFF WebView UA.
        try{if(navigator.userAgent.indexOf('LIFF')>-1||navigator.userAgent.indexOf('Line')>-1){
            var ua=navigator.userAgent.replace(/\s*LIFF\/[\d.]+/g,'').replace(/\s*Line\/[\d.]+/g,'');
            Object.defineProperty(navigator,'userAgent',{get:function(){return ua},configurable:true});
        }}catch(e){}
        // Kakao WebView UA.
        try{if(navigator.userAgent.indexOf('KAKAOTALK')>-1){
            var ua2=navigator.userAgent.replace(/\s*KAKAOTALK[\s\/][\d.]+/g,'');
            Object.defineProperty(navigator,'userAgent',{get:function(){return ua2},configurable:true});
        }}catch(e){}
    })();""".trimIndent()

    private val NAVER_ANTI_DETECTION_JS = """(function(){'use strict';
        // Naver InApp bridge.
        try{delete window.__naver__;delete window.NaverLogin;delete window.naver}catch(e){}
        // Naver WebView UA.
        try{if(navigator.userAgent.indexOf('NAVER')>-1){
            var ua=navigator.userAgent.replace(/\s*NAVER\([^\)]*\)/g,'');
            Object.defineProperty(navigator,'userAgent',{get:function(){return ua},configurable:true});
        }}catch(e){}
    })();""".trimIndent()

    private val WECHAT_QQ_ANTI_DETECTION_JS = """(function(){'use strict';
        // /QQ WebView bridge.
        try{delete window.WeixinJSBridge;delete window.__wxjs_environment;delete window.wx}catch(e){}
        try{delete window.mqq;delete window.QQBrowser;delete window.__qqmusic__;delete window.TencentGDT}catch(e){}
        // UA (MicroMessenger/x.x.x)
        try{if(navigator.userAgent.indexOf('MicroMessenger')>-1||navigator.userAgent.indexOf('miniProgram')>-1){
            var ua=navigator.userAgent.replace(/\s*MicroMessenger\/[\d.]+/g,'').replace(/\s*miniProgram\/[\d.]+/g,'').replace(/\s*NetType\/\w+/g,'').replace(/\s*Language\/\w+/g,'');
            Object.defineProperty(navigator,'userAgent',{get:function(){return ua},configurable:true});
        }}catch(e){}
        // QQ UA.
        try{if(navigator.userAgent.indexOf('QQ/')>-1){
            var ua2=navigator.userAgent.replace(/\s*QQ\/[\d.]+/g,'').replace(/\s*MQQBrowser\/[\d.]+/g,'');
            Object.defineProperty(navigator,'userAgent',{get:function(){return ua2},configurable:true});
        }}catch(e){}
    })();""".trimIndent()

    private val ALIPAY_ANTI_DETECTION_JS = """(function(){'use strict';
        // bridge.
        try{delete window.AlipayJSBridge;delete window.ap;delete window.my;delete window.AFAppScript}catch(e){}
        // UA.
        try{if(navigator.userAgent.indexOf('AlipayClient')>-1||navigator.userAgent.indexOf('AliApp')>-1){
            var ua=navigator.userAgent.replace(/\s*AlipayClient\/[\d.]+/g,'').replace(/\s*AliApp\([^\)]*\)/g,'');
            Object.defineProperty(navigator,'userAgent',{get:function(){return ua},configurable:true});
        }}catch(e){}
    })();""".trimIndent()

    private val TIKTOK_ANTI_DETECTION_JS = """(function(){'use strict';
        // TikTok/Douyin WebView bridge.
        try{delete window.TikTok;delete window.bytedance;delete window.__bd__;delete window.JSBridge}catch(e){}
        // TikTok UA.
        try{if(navigator.userAgent.indexOf('musical_ly')>-1||navigator.userAgent.indexOf('BytedanceWebview')>-1||navigator.userAgent.indexOf('TikTok')>-1){
            var ua=navigator.userAgent.replace(/\s*musical_ly[\s\/][\d.]+/g,'').replace(/\s*BytedanceWebview\/[\d.]+/g,'').replace(/\s*TikTok[\s\/][\d.]+/g,'').replace(/\s*app_version\/[\d.]+/g,'');
            Object.defineProperty(navigator,'userAgent',{get:function(){return ua},configurable:true});
        }}catch(e){}
    })();""".trimIndent()

    private val YAHOO_JP_ANTI_DETECTION_JS = """(function(){'use strict';
        // Yahoo Japan single, API.
        try{if(!navigator.credentials){Object.defineProperty(navigator,'credentials',{get:function(){return{
            get:function(){return Promise.resolve(null)},store:function(){return Promise.resolve()},
            preventSilentAccess:function(){return Promise.resolve()},create:function(){return Promise.resolve(null)}
        }},enumerable:true,configurable:true})}}catch(e){}
    })();""".trimIndent()


    private val VK_ANTI_DETECTION_JS = """(function(){'use strict';
        // VK App bridge.
        try{delete window.vkBridge;delete window.VK;delete window.vkConnect}catch(e){}
        // VK Notification.
    })();""".trimIndent()

    private val YANDEX_ANTI_DETECTION_JS = """(function(){'use strict';
        // Yandex.
        try{if(navigator.brave)delete navigator.brave}catch(e){}
        // Yandex window.yandex.
        try{delete window.yandex;delete window.Ya}catch(e){}
    })();""".trimIndent()

    private val MAILRU_ANTI_DETECTION_JS = """(function(){'use strict';
        // Mail.ru SDK.
        try{delete window.mailru;delete window.mr}catch(e){}
    })();""".trimIndent()

    private val SHOPIFY_ANTI_DETECTION_JS = """(function(){'use strict';
        // Shopify Fetch + Crypto.
        try{if(!window.crypto||!window.crypto.subtle){if(!window.crypto)window.crypto={};
            window.crypto.subtle={digest:function(){return Promise.resolve(new ArrayBuffer(32))}}}}catch(e){}
        // Shopify Checkout CSP.
        try{Object.defineProperty(navigator,'cookieEnabled',{get:function(){return true},configurable:true})}catch(e){}
    })();""".trimIndent()

    private val DROPBOX_ANTI_DETECTION_JS = """(function(){'use strict';
        // Dropbox use Sentry (error tracking)
        try{if(!window.fetch){window.fetch=function(){return Promise.reject(new TypeError('NetworkError'))}}}catch(e){}
    })();""".trimIndent()

    private val NOTION_ANTI_DETECTION_JS = """(function(){'use strict';
        // Notion use WASM + Crypto.
        try{if(!window.crypto||!window.crypto.subtle){if(!window.crypto)window.crypto={};
            window.crypto.subtle={digest:function(a,d){return Promise.resolve(new ArrayBuffer(32))}}}}catch(e){}
        // Notion Clipboard API.
        try{if(!navigator.clipboard){Object.defineProperty(navigator,'clipboard',{get:function(){return{
            writeText:function(){return Promise.resolve()},readText:function(){return Promise.resolve('')}
        }},enumerable:true,configurable:true})}}catch(e){}
    })();""".trimIndent()

    private val SLACK_ANTI_DETECTION_JS = """(function(){'use strict';
        // Slack IndexedDB + BroadcastChannel.
        // Slack desktop app bridge.
        try{delete window.desktop;delete window.slackElectron}catch(e){}
    })();""".trimIndent()

    private val ZOOM_ANTI_DETECTION_JS = """(function(){'use strict';
        // Zoom MediaDevices.
        try{if(!navigator.mediaDevices){Object.defineProperty(navigator,'mediaDevices',{get:function(){return{
            enumerateDevices:function(){return Promise.resolve([])},
            getUserMedia:function(){return Promise.reject(new DOMException('NotAllowed'))},
            getSupportedConstraints:function(){return{width:true,height:true,aspectRatio:true,frameRate:true,facingMode:true,deviceId:true}}
        }},enumerable:true,configurable:true})}}catch(e){}
        // Zoom Web SDK AudioContext.
        try{if(!window.AudioContext&&window.webkitAudioContext)window.AudioContext=window.webkitAudioContext}catch(e){}
    })();""".trimIndent()


    private val PAYPAL_ANTI_DETECTION_JS = """(function(){'use strict';
        // PayPal Checkout SDK.
        // window.name Check.
        try{if(window.name&&window.name.toLowerCase().indexOf('webview')!==-1)window.name=''}catch(e){}
        // PayPal window.opener.
        try{if(!window.opener)Object.defineProperty(window,'opener',{get:function(){return null},configurable:true,set:function(){}})}catch(e){}
        // PayPal postMessage origin Check.
        try{window.postMessage=window.postMessage}catch(e){}
        // Crypto.
        try{if(!window.crypto||!window.crypto.getRandomValues){
            if(!window.crypto)window.crypto={};
            window.crypto.getRandomValues=function(arr){for(var i=0;i<arr.length;i++)arr[i]=Math.floor(Math.random()*256);return arr}
        }}catch(e){}
        // PayPal indexedDB.
        try{if(!window.indexedDB)window.indexedDB=window.mozIndexedDB||window.webkitIndexedDB||window.msIndexedDB}catch(e){}
    })();""".trimIndent()

    private val STRIPE_ANTI_DETECTION_JS = """(function(){'use strict';
        // Stripe.js __SENTRY__.
        try{delete window.__SENTRY__}catch(e){}
        // Stripe Elements IntersectionObserver.
        try{if(!window.IntersectionObserver){window.IntersectionObserver=function(cb,opt){this.observe=function(){};this.unobserve=function(){};this.disconnect=function(){};this.takeRecords=function(){return[]}}}}catch(e){}
        // Stripe 3D Secure iframe.
        try{if(!window.crypto||!window.crypto.subtle){if(!window.crypto)window.crypto={};
            window.crypto.subtle={digest:function(){return Promise.resolve(new ArrayBuffer(32))},
                generateKey:function(){return Promise.resolve({})},exportKey:function(){return Promise.resolve(new ArrayBuffer(32))}}
        }}catch(e){}
    })();""".trimIndent()

    // ==================== Tier 6: CAPTCHA ====================

    private val RECAPTCHA_ANTI_DETECTION_JS = """(function(){'use strict';
        // reCAPTCHA v2/v3.
        // 1. focus/blur.
        try{Object.defineProperty(document,'visibilityState',{get:function(){return'visible'},configurable:true});
            Object.defineProperty(document,'hidden',{get:function(){return false},configurable:true})}catch(e){}
        // 2. Performance API.
        try{if(!window.PerformanceObserver){window.PerformanceObserver=function(cb){this.observe=function(){};this.disconnect=function(){};this.takeRecords=function(){return[]}}}}catch(e){}
        // 3. reCAPTCHA Enterprise SharedArrayBuffer.
        try{if(typeof SharedArrayBuffer==='undefined'){window.SharedArrayBuffer=function(s){return new ArrayBuffer(s)}}}catch(e){}
    })();""".trimIndent()

    private val HCAPTCHA_ANTI_DETECTION_JS = """(function(){'use strict';
        // hCaptcha WebGL + Canvas.
        // hCaptcha accessibility tree.
        try{if(!window.getComputedStyle){window.getComputedStyle=function(el){return el.style||{}}}}catch(e){}
        // hCaptcha use postMessage.
        try{document.hasFocus=function(){return true}}catch(e){}
    })();""".trimIndent()

    private val CLOUDFLARE_ANTI_DETECTION_JS = """(function(){'use strict';
        // Cloudflare Turnstile.
        // 1. JS.
        try{Object.defineProperty(document,'visibilityState',{get:function(){return'visible'},configurable:true})}catch(e){}
        // 2. Performance.now()
        // 3. Canvas.
        // 4. WebGL.
        // 5. Cloudflare bot detection screen properties
        try{if(screen.width===0)Object.defineProperty(screen,'width',{get:function(){return 1920},configurable:true})}catch(e){}
        try{if(screen.height===0)Object.defineProperty(screen,'height',{get:function(){return 1080},configurable:true})}catch(e){}
        // 6. Worker Supports.
        try{if(!window.Worker){window.Worker=function(url){this.postMessage=function(){};this.terminate=function(){};this.onmessage=null;this.onerror=null}}}catch(e){}
    })();""".trimIndent()

    private val YAHOO_ANTI_DETECTION_JS = """(function(){'use strict';
        // Yahoo.
        try{Object.defineProperty(navigator,'cookieEnabled',{get:function(){return true},configurable:true})}catch(e){}
    })();""".trimIndent()
}
