package com.webtoapp.core.errorpage

import com.webtoapp.util.upgradeRemoteHttpToHttps





class ErrorPageManager(private val config: ErrorPageConfig) {

    /**
     * Map a raw WebView error code to a human-friendly short code displayed
     * in the error pill. We only elevate a handful of well-known codes to a
     * named constant; anything else is shown as a generic ERR + numeric code
     * so it remains diagnosable without leaking implementation details.
     */
    private fun formatErrorCode(code: Int): String {
        return when (code) {
            -1 -> "ERR_UNKNOWN"
            -2 -> "ERR_HOST_LOOKUP"
            -6 -> "ERR_CONNECT"
            -7 -> "ERR_TIMEOUT"
            -8 -> "ERR_REDIRECT_LOOP"
            -10 -> "ERR_UNSUPPORTED_SCHEME"
            -11 -> "ERR_FAILED_SSL_HANDSHAKE"
            -14 -> "ERR_FILE_NOT_FOUND"
            -15 -> "ERR_TOO_MANY_REQUESTS"
            404 -> "ERR_404"
            500, 502, 503, 504 -> "ERR_$code"
            else -> "ERR · $code"
        }
    }








    @Suppress("UNUSED_PARAMETER")
    fun generateErrorPage(errorCode: Int, description: String, failedUrl: String?): String? =
        generateErrorPage(errorCode, description, description, failedUrl)

    /**
     * Generate an error page HTML string, or `null` to let the system render its default.
     *
     * [rawDescription] is the original text from [android.webkit.WebResourceError.description]
     * before any normalization. Some upstream callers normalize the description (e.g. collapse
     * "CLEARTEXT ..." into a friendly sentence); keeping the raw copy lets the diagnostics
     * engine match on `EADDRNOTAVAIL` / `ECONNREFUSED` / `errno` strings which would otherwise
     * be lost.
     *
     * In [ErrorPageMode.DEFAULT] we normally return null (preserve system page), but if the
     * error matches an actionable diagnostic we build a minimal diagnostic page so the user
     * gets a remediation hint instead of "bind failed: EADDRNOTAVAIL".
     */
    @Suppress("UNUSED_PARAMETER")
    fun generateErrorPage(
        errorCode: Int,
        description: String,
        rawDescription: String,
        failedUrl: String?,
    ): String? {
        val diagnostic = NetworkErrorDiagnostics.diagnose(
            rawDescription = rawDescription,
            errorCode = errorCode,
            failedUrl = failedUrl,
            language = config.language,
        )
        return when (config.mode) {
            ErrorPageMode.DEFAULT -> diagnostic?.let { generateDiagnosticFallbackPage(it, failedUrl) }
            ErrorPageMode.BUILTIN_STYLE -> generateBuiltInPage(errorCode, description, failedUrl, diagnostic)
            ErrorPageMode.CUSTOM_HTML -> config.customHtml
            ErrorPageMode.CUSTOM_MEDIA -> generateMediaPage(failedUrl)
        }
    }



    private data class I18nStrings(
        val title: String,
        val subtitle: String,
        val retryButton: String,
        val autoRetryPrefix: String,
        val autoRetrySuffix: String,
        val gameLink: String,
        val gameLabel: String,
        val gameClose: String,
        val dir: String,
        val langCode: String
    )

    private fun getStrings(): I18nStrings {
        return when (config.language.uppercase()) {
            "ARABIC" -> I18nStrings(
                title = "تعذّر الوصول إلى الموقع",
                subtitle = "يُرجى التحقّق من اتصالك بالإنترنت والمحاولة مرة أخرى",
                retryButton = "إعادة المحاولة",
                autoRetryPrefix = "إعادة المحاولة خلال ",
                autoRetrySuffix = " ثوانٍ",
                gameLink = "جرّب لعبة أثناء الانتظار",
                gameLabel = "لعبة",
                gameClose = "إغلاق",
                dir = "rtl",
                langCode = "ar"
            )
            "ENGLISH" -> I18nStrings(
                title = "This site can\u2019t be reached",
                subtitle = "Check your internet connection and try again",
                retryButton = "Retry",
                autoRetryPrefix = "Retrying in ",
                autoRetrySuffix = "s",
                gameLink = "Play a game while you wait",
                gameLabel = "Game",
                gameClose = "Close",
                dir = "ltr",
                langCode = "en"
            )
            else -> I18nStrings(
                title = "无法访问此网站",
                subtitle = "请检查网络连接后重试",
                retryButton = "重试",
                autoRetryPrefix = "将在 ",
                autoRetrySuffix = " 秒后重试",
                gameLink = "等待时玩个小游戏",
                gameLabel = "小游戏",
                gameClose = "关闭",
                dir = "ltr",
                langCode = "zh"
            )
        }
    }



    private fun generateBuiltInPage(
        errorCode: Int,
        description: String,
        failedUrl: String?,
        diagnostic: NetworkErrorDiagnostics.Diagnostic?,
    ): String {
        val style = config.builtInStyle
        val strings = getStrings()
        val retryBtnText = config.retryButtonText.ifBlank { strings.retryButton }
        val showGame = config.showMiniGame
        val gameType = config.miniGameType
        val autoRetry = config.autoRetrySeconds


        if (style != ErrorPageStyle.MATERIAL) {
            return generateLegacyPage(style, strings, retryBtnText, showGame, gameType, autoRetry, failedUrl, diagnostic)
        }

        val safeUrl = failedUrl
            ?.let { upgradeRemoteHttpToHttps(it) }
            ?.replace("'", "\\'")
            ?.replace("\"", "&quot;")
            ?: ""

        val gameJs = if (showGame) ErrorPageGames.getGameJs(gameType) else ""

        return """
<!DOCTYPE html>
<html lang="${strings.langCode}" dir="${strings.dir}">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
html,body{width:100%;height:100%;-webkit-tap-highlight-color:transparent;}

body{
    font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','SF Pro Text',system-ui,sans-serif;
    display:flex;flex-direction:column;align-items:center;justify-content:center;
    min-height:100vh;padding:40px 28px;text-align:center;
    background:#fbfbfc;color:#111113;
    -webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale;
    transition:background 0.3s,color 0.3s;
}

@media(prefers-color-scheme:dark){
    body{background:#0a0a0c;color:#ebebee;}
    .subtitle{color:#9b9ba0 !important;}
    .retry-btn{
        background:#ebebee !important;color:#0a0a0c !important;
        border-color:transparent !important;
    }
    .retry-btn:active{background:#d4d4d8 !important;}
    .auto-retry{color:#7a7a7f !important;}
    .game-link{color:#9b9ba0 !important;}
    .error-code{color:#7a7a7f !important;border-color:#2a2a2f !important;background:rgba(255,255,255,0.02) !important;}
    .game-overlay{background:rgba(0,0,0,0.94) !important;}
    .icon-ring circle{stroke:#3a3a3f !important;}
    .icon-ring .icon-stem{stroke:#7a7a7f !important;}
    .icon-ring .icon-dot{fill:#7a7a7f !important;}
    .icon-ring .icon-smile{stroke:#3a3a3f !important;}
}

.icon-container{
    width:88px;height:88px;margin-bottom:32px;
    opacity:0;animation:fadeIn 0.5s cubic-bezier(0.22,1,0.36,1) 0.05s forwards;
}
.icon-container svg{width:100%;height:100%;}

.title{
    font-size:22px;font-weight:600;line-height:1.3;
    color:#111113;margin-bottom:8px;letter-spacing:-0.015em;
    opacity:0;animation:fadeIn 0.5s cubic-bezier(0.22,1,0.36,1) 0.12s forwards;
}
@media(prefers-color-scheme:dark){.title{color:#ebebee;}}

.subtitle{
    font-size:14px;font-weight:400;line-height:1.5;
    color:#55555a;margin-bottom:28px;max-width:340px;
    opacity:0;animation:fadeIn 0.5s cubic-bezier(0.22,1,0.36,1) 0.2s forwards;
}

.retry-btn{
    display:inline-flex;align-items:center;justify-content:center;
    height:44px;padding:0 24px;border-radius:10px;
    border:1px solid transparent;background:#111113;
    color:#ffffff;font-family:inherit;font-size:15px;font-weight:500;
    cursor:pointer;transition:transform 0.15s ease, background 0.15s ease;
    letter-spacing:-0.005em;text-decoration:none;
    opacity:0;animation:fadeIn 0.5s cubic-bezier(0.22,1,0.36,1) 0.3s forwards;
    min-width:140px;
}
.retry-btn:hover{background:#29292c;}
.retry-btn:active{transform:scale(0.97);}

.auto-retry{
    font-size:12.5px;color:#7a7a7f;margin-top:14px;
    opacity:0;animation:fadeIn 0.5s cubic-bezier(0.22,1,0.36,1) 0.4s forwards;
    letter-spacing:0.005em;
}

.error-code{
    font-size:11px;color:#7a7a7f;margin-top:28px;
    padding:5px 10px;border:1px solid #e4e4e7;border-radius:999px;
    background:rgba(0,0,0,0.02);
    font-family:'SF Mono','Roboto Mono',monospace;letter-spacing:0.04em;
    opacity:0;animation:fadeIn 0.5s cubic-bezier(0.22,1,0.36,1) 0.5s forwards;
}

.game-link{
    display:inline-block;margin-top:18px;font-size:13px;
    color:#55555a;cursor:pointer;text-decoration:none;
    opacity:0;animation:fadeIn 0.5s cubic-bezier(0.22,1,0.36,1) 0.55s forwards;
    transition:color 0.15s;font-weight:500;letter-spacing:0;
}
.game-link:hover{color:#111113;}

@keyframes fadeIn{from{opacity:0;transform:translateY(6px);}to{opacity:1;transform:translateY(0);}}

/* Game overlay */
.game-overlay{
    position:fixed;top:0;left:0;width:100%;height:100%;z-index:100;
    display:none;flex-direction:column;align-items:center;justify-content:center;
    background:rgba(0,0,0,0.92);backdrop-filter:blur(8px);-webkit-backdrop-filter:blur(8px);
}
.game-overlay.active{display:flex;}
.game-header{
    display:flex;justify-content:space-between;align-items:center;
    width:100%;max-width:320px;padding:8px 4px;
}
.game-header span{color:rgba(255,255,255,0.7);font-size:13px;font-weight:500;letter-spacing:0.01em;}
.game-close{
    color:rgba(255,255,255,0.85);font-size:13px;cursor:pointer;
    padding:6px 14px;border:1px solid rgba(255,255,255,0.25);border-radius:999px;
    font-weight:500;transition:all 0.15s;
}
.game-close:active{background:rgba(255,255,255,0.12);transform:scale(0.97);}
canvas{border-radius:12px;margin-top:8px;touch-action:none;}

/* Diagnostic card */
.diag-card{
    max-width:380px;width:100%;margin:0 0 22px 0;
    padding:14px 16px;border-radius:14px;text-align:start;
    background:rgba(0,0,0,0.03);border:1px solid rgba(0,0,0,0.06);
    opacity:0;animation:fadeIn 0.5s cubic-bezier(0.22,1,0.36,1) 0.25s forwards;
}
.diag-card.diag-error{background:rgba(211,47,47,0.06);border-color:rgba(211,47,47,0.18);}
.diag-card.diag-warning{background:rgba(237,108,2,0.06);border-color:rgba(237,108,2,0.18);}
.diag-head{font-size:12px;font-weight:600;letter-spacing:0.02em;margin-bottom:8px;color:#55555a;text-transform:uppercase;}
.diag-card.diag-error .diag-head{color:#b3261e;}
.diag-card.diag-warning .diag-head{color:#ad590b;}
.diag-list{list-style:none;padding:0;margin:0;}
.diag-list li{
    position:relative;padding-inline-start:18px;font-size:13px;line-height:1.55;
    color:#3a3a3f;margin-bottom:4px;
}
.diag-list li::before{
    content:'';position:absolute;inset-inline-start:6px;top:10px;width:4px;height:4px;
    border-radius:999px;background:#8e8e93;
}
@media(prefers-color-scheme:dark){
    .diag-card{background:rgba(255,255,255,0.04);border-color:rgba(255,255,255,0.08);}
    .diag-card.diag-error{background:rgba(255,115,115,0.08);border-color:rgba(255,115,115,0.22);}
    .diag-card.diag-warning{background:rgba(255,167,38,0.08);border-color:rgba(255,167,38,0.22);}
    .diag-head{color:#9b9ba0;}
    .diag-card.diag-error .diag-head{color:#ff8a80;}
    .diag-card.diag-warning .diag-head{color:#ffbc5c;}
    .diag-list li{color:#c6c6cb;}
    .diag-list li::before{background:#7a7a7f;}
}
</style>
</head>
<body>

<div class="icon-container">
    <svg class="icon-ring" viewBox="0 0 88 88" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="44" cy="44" r="40" stroke="#dadce0" stroke-width="1.5"/>
        <circle cx="44" cy="44" r="40" stroke="#dadce0" stroke-width="1.5" stroke-dasharray="6 5" opacity="0.35"/>
        <path class="icon-stem" d="M44 28v22" stroke="#8e8e93" stroke-width="2.5" stroke-linecap="round"/>
        <circle class="icon-dot" cx="44" cy="58" r="2" fill="#8e8e93"/>
    </svg>
</div>

<h1 class="title">${diagnostic?.title ?: strings.title}</h1>
<p class="subtitle">${diagnostic?.cause ?: strings.subtitle}</p>

${if (diagnostic != null) renderDiagnosticCard(diagnostic) else ""}

<button class="retry-btn" onclick="retryLoad()">${retryBtnText}</button>

${if (autoRetry > 0) """<div class="auto-retry" id="autoRetry">${strings.autoRetryPrefix}<span id="countdown">$autoRetry</span>${strings.autoRetrySuffix}</div>""" else ""}

${if (showGame) """<a class="game-link" onclick="showGame()">${strings.gameLink} →</a>""" else ""}

<div class="error-code">${formatErrorCode(errorCode)}${if (diagnostic != null) " · ${escapeHtml(diagnostic.key)}" else ""}</div>

${if (showGame) """
<div class="game-overlay" id="gameOverlay">
    <div class="game-header">
        <span>${strings.gameLabel}</span>
        <div class="game-close" onclick="hideGame()">${strings.gameClose}</div>
    </div>
    <canvas id="gameCanvas" width="300" height="380"></canvas>
</div>
""" else ""}

<script>
var __retryUrl='$safeUrl';
function retryLoad(){
    if(__retryUrl)location.href=__retryUrl;
    else location.reload();
}
${if (autoRetry > 0) """
(function(){
    var sec=$autoRetry,el=document.getElementById('countdown');
    var t=setInterval(function(){
        sec--;if(el)el.textContent=sec;
        if(sec<=0){clearInterval(t);retryLoad();}
    },1000);
})();
""" else ""}

${if (showGame) """
var __gameStarted=false;
function showGame(){
    document.getElementById('gameOverlay').classList.add('active');
    if(!__gameStarted){__gameStarted=true;startGame();}
}
function hideGame(){document.getElementById('gameOverlay').classList.remove('active');}
function startGame(){
    $gameJs
}
""" else ""}
</script>
</body>
</html>
        """.trimIndent()
    }



    private fun generateLegacyPage(
        style: ErrorPageStyle,
        strings: I18nStrings,
        retryBtnText: String,
        showGame: Boolean,
        gameType: MiniGameType,
        autoRetry: Int,
        failedUrl: String?,
        diagnostic: NetworkErrorDiagnostics.Diagnostic?,
    ): String {
        val resolvedTitle = diagnostic?.title ?: strings.title
        val resolvedSubtitle = diagnostic?.cause ?: strings.subtitle
        val styleCss = ErrorPageStyles.getStyleCss(style)
        val styleBody = ErrorPageStyles.getStyleBody(style, resolvedTitle, resolvedSubtitle)
        val gameJs = if (showGame) ErrorPageGames.getGameJs(gameType) else ""

        val safeUrl = failedUrl
            ?.let { upgradeRemoteHttpToHttps(it) }
            ?.replace("'", "\\'")
            ?.replace("\"", "&quot;")
            ?: ""

        return """
<!DOCTYPE html>
<html lang="${strings.langCode}" dir="${strings.dir}">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
html,body{width:100%;height:100%;overflow-x:hidden;-webkit-tap-highlight-color:transparent;}
body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;padding:24px;text-align:center;}
.illustration{margin-bottom:24px;opacity:0;animation:fadeUp 0.6s ease 0.2s forwards;}
.error-title{font-size:20px;font-weight:600;margin-bottom:8px;opacity:0;animation:fadeUp 0.6s ease 0.4s forwards;}
.error-subtitle{font-size:14px;margin-bottom:28px;opacity:0;animation:fadeUp 0.6s ease 0.6s forwards;}
.retry-btn{
    display:inline-block;padding:11px 28px;border-radius:10px;border:none;
    color:#fff;font-size:15px;font-weight:500;cursor:pointer;letter-spacing:-0.005em;
    transition:all 0.2s ease;text-decoration:none;
    opacity:0;animation:fadeUp 0.6s ease 0.8s forwards;
}
.retry-section{margin-bottom:16px;}
.auto-retry{font-size:12px;opacity:0.5;margin-top:10px;opacity:0;animation:fadeUp 0.6s ease 1s forwards;}
.game-link{
    display:inline-block;margin-top:20px;font-size:12px;opacity:0.5;
    cursor:pointer;text-decoration:none;color:inherit;
    opacity:0;animation:fadeUp 0.6s ease 1.1s forwards;
    transition:opacity 0.2s;
}
.game-link:hover,.game-link:active{opacity:0.8;}
@keyframes fadeUp{from{opacity:0;transform:translateY(12px);}to{opacity:1;transform:translateY(0);}}

/* 游戏容器 */
.game-overlay{
    position:fixed;top:0;left:0;width:100%;height:100%;z-index:100;
    display:none;flex-direction:column;align-items:center;justify-content:center;
    background:rgba(0,0,0,0.95);
}
.game-overlay.active{display:flex;}
.game-header{
    display:flex;justify-content:space-between;align-items:center;
    width:100%;max-width:320px;padding:8px 4px;
}
.game-header span{color:rgba(255,255,255,0.6);font-size:13px;}
.game-close{
    color:rgba(255,255,255,0.5);font-size:13px;cursor:pointer;
    padding:4px 12px;border:1px solid rgba(255,255,255,0.2);border-radius:12px;
}
.game-close:active{background:rgba(255,255,255,0.1);}
canvas{border-radius:8px;margin-top:4px;touch-action:none;}

/* Diagnostic card (legacy) */
.diag-card{
    max-width:380px;width:100%;margin:0 auto 18px auto;
    padding:12px 14px;border-radius:12px;text-align:start;
    background:rgba(255,255,255,0.06);border:1px solid rgba(255,255,255,0.08);
    color:inherit;
}
.diag-card.diag-error{background:rgba(211,47,47,0.14);border-color:rgba(211,47,47,0.3);}
.diag-card.diag-warning{background:rgba(237,108,2,0.14);border-color:rgba(237,108,2,0.3);}
.diag-head{font-size:11px;font-weight:600;letter-spacing:0.04em;margin-bottom:6px;opacity:0.7;text-transform:uppercase;}
.diag-list{list-style:none;padding:0;margin:0;}
.diag-list li{position:relative;padding-inline-start:16px;font-size:13px;line-height:1.5;margin-bottom:3px;opacity:0.9;}
.diag-list li::before{content:'';position:absolute;inset-inline-start:5px;top:9px;width:4px;height:4px;border-radius:999px;background:currentColor;opacity:0.55;}

$styleCss
</style>
</head>
<body>
$styleBody

${if (diagnostic != null) renderDiagnosticCard(diagnostic) else ""}

<div class="retry-section">
    <button class="retry-btn" onclick="retryLoad()">$retryBtnText</button>
    ${if (autoRetry > 0) """<div class="auto-retry" id="autoRetry">${strings.autoRetryPrefix}<span id="countdown">$autoRetry</span>${strings.autoRetrySuffix}</div>""" else ""}
</div>

${if (showGame) """<a class="game-link" onclick="showGame()">${strings.gameLink} →</a>""" else ""}

${if (showGame) """
<div class="game-overlay" id="gameOverlay">
    <div class="game-header">
        <span>${strings.gameLabel}</span>
        <div class="game-close" onclick="hideGame()">${strings.gameClose}</div>
    </div>
    <canvas id="gameCanvas" width="300" height="380"></canvas>
</div>
""" else ""}

<script>
var __retryUrl='$safeUrl';
function retryLoad(){
    if(__retryUrl)location.href=__retryUrl;
    else location.reload();
}
${if (autoRetry > 0) """
(function(){
    var sec=$autoRetry,el=document.getElementById('countdown');
    var t=setInterval(function(){
        sec--;if(el)el.textContent=sec;
        if(sec<=0){clearInterval(t);retryLoad();}
    },1000);
})();
""" else ""}

${if (showGame) """
var __gameStarted=false;
function showGame(){
    document.getElementById('gameOverlay').classList.add('active');
    if(!__gameStarted){__gameStarted=true;startGame();}
}
function hideGame(){document.getElementById('gameOverlay').classList.remove('active');}
function startGame(){
    $gameJs
}
""" else ""}
</script>
</body>
</html>
        """.trimIndent()
    }




    private fun generateMediaPage(failedUrl: String?): String {
        val strings = getStrings()
        val mediaPath = config.customMediaPath ?: ""
        val retryBtnText = config.retryButtonText.ifBlank { strings.retryButton }
        val isVideo = mediaPath.endsWith(".mp4") || mediaPath.endsWith(".webm")
        val safeUrl = failedUrl
            ?.let { upgradeRemoteHttpToHttps(it) }
            ?.replace("'", "\\'")
            ?.replace("\"", "&quot;")
            ?: ""

        val mediaHtml = if (isVideo) {
            """<video src="$mediaPath" autoplay loop muted playsinline style="max-width:80%;max-height:50vh;border-radius:12px;"></video>"""
        } else {
            """<img src="$mediaPath" style="max-width:80%;max-height:50vh;border-radius:12px;object-fit:contain;" alt=""/>"""
        }

        return """
<!DOCTYPE html>
<html lang="${strings.langCode}" dir="${strings.dir}">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
body{
    font-family:'Google Sans','Roboto',-apple-system,BlinkMacSystemFont,'Segoe UI',system-ui,sans-serif;
    display:flex;flex-direction:column;align-items:center;justify-content:center;
    min-height:100vh;padding:24px;text-align:center;
    background:#1f1f1f;color:#e8eaed;
}
.media-container{margin-bottom:24px;}
.retry-btn{
    display:inline-flex;align-items:center;justify-content:center;
    height:40px;padding:0 24px;border-radius:20px;
    border:1px solid rgba(255,255,255,0.2);background:transparent;
    color:#8ab4f8;font-size:14px;font-weight:500;cursor:pointer;
    font-family:inherit;
}
.retry-btn:active{background:rgba(138,180,248,0.08);}
</style>
</head>
<body>
<div class="media-container">$mediaHtml</div>
<button class="retry-btn" onclick="var u='$safeUrl';if(u)location.href=u;else location.reload();">$retryBtnText</button>
</body>
</html>
        """.trimIndent()
    }

    // ---------------------------------------------------------------
    // Diagnostic helpers
    // ---------------------------------------------------------------

    /** Render the diagnostic card HTML fragment used by both builtin and legacy templates. */
    private fun renderDiagnosticCard(diag: NetworkErrorDiagnostics.Diagnostic): String {
        val severityClass = when (diag.severity) {
            NetworkErrorDiagnostics.Severity.ERROR -> "diag-error"
            NetworkErrorDiagnostics.Severity.WARNING -> "diag-warning"
            NetworkErrorDiagnostics.Severity.INFO -> "diag-info"
        }
        val strings = getStrings()
        val head = when (strings.langCode) {
            "en" -> "What to try"
            "ar" -> "حلول مقترحة"
            else -> "建议排查"
        }
        val items = diag.suggestions.joinToString("") { "<li>${escapeHtml(it)}</li>" }
        return """<div class="diag-card $severityClass"><div class="diag-head">$head</div><ul class="diag-list">$items</ul></div>"""
    }

    /**
     * Minimal standalone page used in [ErrorPageMode.DEFAULT] when we have a useful diagnostic.
     * Kept intentionally small — no animations, no game, no auto-retry — because users who
     * opted into DEFAULT explicitly asked for the system UI; we only override it to turn
     * unreadable errno into actionable guidance.
     */
    private fun generateDiagnosticFallbackPage(
        diag: NetworkErrorDiagnostics.Diagnostic,
        failedUrl: String?,
    ): String {
        val strings = getStrings()
        val retryBtnText = config.retryButtonText.ifBlank { strings.retryButton }
        val safeUrl = failedUrl
            ?.let { upgradeRemoteHttpToHttps(it) }
            ?.replace("'", "\\'")
            ?.replace("\"", "&quot;")
            ?: ""
        val severityColor = when (diag.severity) {
            NetworkErrorDiagnostics.Severity.ERROR -> "#b3261e"
            NetworkErrorDiagnostics.Severity.WARNING -> "#ad590b"
            NetworkErrorDiagnostics.Severity.INFO -> "#55555a"
        }
        val head = when (strings.langCode) {
            "en" -> "What to try"
            "ar" -> "حلول مقترحة"
            else -> "建议排查"
        }
        val items = diag.suggestions.joinToString("") { "<li>${escapeHtml(it)}</li>" }
        return """
<!DOCTYPE html>
<html lang="${strings.langCode}" dir="${strings.dir}">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
<style>
*{margin:0;padding:0;box-sizing:border-box;}
html,body{width:100%;min-height:100%;}
body{
    font-family:-apple-system,BlinkMacSystemFont,'Segoe UI','SF Pro Text',system-ui,sans-serif;
    display:flex;flex-direction:column;align-items:center;justify-content:center;
    min-height:100vh;padding:36px 24px;text-align:start;
    background:#fbfbfc;color:#111113;
    -webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale;
}
.wrap{max-width:380px;width:100%;}
h1{font-size:20px;font-weight:600;line-height:1.35;margin-bottom:8px;letter-spacing:-0.015em;color:$severityColor;}
p.cause{font-size:14px;line-height:1.55;color:#3a3a3f;margin-bottom:16px;}
.head{font-size:11px;font-weight:600;letter-spacing:0.04em;margin-bottom:6px;color:#8e8e93;text-transform:uppercase;}
ul{list-style:none;padding:0;margin:0 0 24px 0;}
ul li{position:relative;padding-inline-start:18px;font-size:13.5px;line-height:1.6;color:#3a3a3f;margin-bottom:4px;}
ul li::before{content:'';position:absolute;inset-inline-start:6px;top:10px;width:4px;height:4px;border-radius:999px;background:#8e8e93;}
button{
    display:inline-flex;align-items:center;justify-content:center;
    height:40px;padding:0 22px;border-radius:10px;border:1px solid transparent;
    background:#111113;color:#fff;font-size:14px;font-weight:500;cursor:pointer;
    font-family:inherit;letter-spacing:-0.005em;
}
button:active{transform:scale(0.97);}
.code{margin-top:18px;font-size:11px;color:#8e8e93;letter-spacing:0.04em;font-family:'SF Mono','Roboto Mono',monospace;}
@media(prefers-color-scheme:dark){
    body{background:#0a0a0c;color:#ebebee;}
    p.cause,ul li{color:#c6c6cb;}
    .head,.code{color:#7a7a7f;}
    button{background:#ebebee;color:#0a0a0c;}
}
</style>
</head>
<body>
<div class="wrap">
<h1>${escapeHtml(diag.title)}</h1>
<p class="cause">${escapeHtml(diag.cause)}</p>
<div class="head">$head</div>
<ul>$items</ul>
<button onclick="var u='$safeUrl';if(u)location.href=u;else location.reload();">${escapeHtml(retryBtnText)}</button>
<div class="code">${escapeHtml(diag.key)}</div>
</div>
</body>
</html>
        """.trimIndent()
    }

    private fun escapeHtml(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
