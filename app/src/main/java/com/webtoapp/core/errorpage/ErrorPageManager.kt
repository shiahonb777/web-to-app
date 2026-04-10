package com.webtoapp.core.errorpage

import com.webtoapp.util.upgradeRemoteHttpToHttps

/**
 * 网络错误页管理器
 * Material Design 3 风格 + 多语言支持 (中/英/阿)
 */
class ErrorPageManager(private val config: ErrorPageConfig) {

    /**
     * 生成完整的错误页 HTML
     * @param errorCode WebView 错误码
     * @param description 错误描述
     * @param failedUrl 失败的原始 URL
     * @return 完整的 HTML 字符串，或 null（DEFAULT 模式不拦截）
     */
    @Suppress("UNUSED_PARAMETER")
    fun generateErrorPage(errorCode: Int, description: String, failedUrl: String?): String? {
        return when (config.mode) {
            ErrorPageMode.DEFAULT -> null
            ErrorPageMode.BUILTIN_STYLE -> generateBuiltInPage(errorCode, description, failedUrl)
            ErrorPageMode.CUSTOM_HTML -> config.customHtml
            ErrorPageMode.CUSTOM_MEDIA -> generateMediaPage(failedUrl)
        }
    }

    // ======================== i18n 字符串 ========================

    private data class I18nStrings(
        val title: String,
        val subtitle: String,
        val retryButton: String,
        val autoRetryPrefix: String,
        val autoRetrySuffix: String,
        val gameLink: String,
        val gameLabel: String,
        val gameClose: String,
        val dir: String,       // "ltr" or "rtl"
        val langCode: String   // HTML lang attribute
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

    // ======================== Material Design 3 页面 ========================

    private fun generateBuiltInPage(errorCode: Int, description: String, failedUrl: String?): String {
        val style = config.builtInStyle
        val strings = getStrings()
        val retryBtnText = config.retryButtonText.ifBlank { strings.retryButton }
        val showGame = config.showMiniGame
        val gameType = config.miniGameType
        val autoRetry = config.autoRetrySeconds

        // 如果是旧风格（非 Material），委托给 ErrorPageStyles
        if (style != ErrorPageStyle.MATERIAL) {
            return generateLegacyPage(style, strings, retryBtnText, showGame, gameType, autoRetry, failedUrl)
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
    font-family:'Google Sans','Roboto',-apple-system,BlinkMacSystemFont,'Segoe UI',system-ui,sans-serif;
    display:flex;flex-direction:column;align-items:center;justify-content:center;
    min-height:100vh;padding:32px 24px;text-align:center;
    background:#fff;color:#202124;
    transition:background 0.3s,color 0.3s;
}

@media(prefers-color-scheme:dark){
    body{background:#1f1f1f;color:#e8eaed;}
    .subtitle{color:#9aa0a6 !important;}
    .retry-btn{
        background:transparent !important;color:#8ab4f8 !important;
        border-color:#8ab4f8 !important;
    }
    .retry-btn:active{background:rgba(138,180,248,0.08) !important;}
    .auto-retry{color:#9aa0a6 !important;}
    .game-link{color:#8ab4f8 !important;}
    .error-code{color:#9aa0a6 !important;border-color:#3c4043 !important;}
    .game-overlay{background:rgba(0,0,0,0.96) !important;}
}

.icon-container{
    width:96px;height:96px;margin-bottom:28px;
    opacity:0;animation:fadeIn 0.4s ease 0.1s forwards;
}
.icon-container svg{width:100%;height:100%;}

.title{
    font-size:22px;font-weight:400;line-height:1.4;
    color:#202124;margin-bottom:8px;letter-spacing:-0.2px;
    opacity:0;animation:fadeIn 0.4s ease 0.2s forwards;
}
@media(prefers-color-scheme:dark){.title{color:#e8eaed;}}

.subtitle{
    font-size:14px;font-weight:400;line-height:1.5;
    color:#5f6368;margin-bottom:32px;max-width:360px;
    opacity:0;animation:fadeIn 0.4s ease 0.3s forwards;
}

.retry-btn{
    display:inline-flex;align-items:center;justify-content:center;
    height:40px;padding:0 24px;border-radius:20px;
    border:1px solid #dadce0;background:transparent;
    color:#1a73e8;font-family:inherit;font-size:14px;font-weight:500;
    cursor:pointer;transition:all 0.15s ease;
    letter-spacing:0.25px;text-decoration:none;
    opacity:0;animation:fadeIn 0.4s ease 0.4s forwards;
}
.retry-btn:hover{background:rgba(26,115,232,0.04);}
.retry-btn:active{background:rgba(26,115,232,0.1);transform:scale(0.98);}

.auto-retry{
    font-size:12px;color:#80868b;margin-top:12px;
    opacity:0;animation:fadeIn 0.4s ease 0.5s forwards;
}

.error-code{
    font-size:11px;color:#80868b;margin-top:24px;
    padding:6px 12px;border:1px solid #dadce0;border-radius:8px;
    font-family:'Roboto Mono',monospace;letter-spacing:0.3px;
    opacity:0;animation:fadeIn 0.4s ease 0.6s forwards;
}

.game-link{
    display:inline-block;margin-top:20px;font-size:13px;
    color:#1a73e8;cursor:pointer;text-decoration:none;
    opacity:0;animation:fadeIn 0.4s ease 0.65s forwards;
    transition:opacity 0.15s;font-weight:500;
}
.game-link:hover{text-decoration:underline;}

@keyframes fadeIn{from{opacity:0;transform:translateY(8px);}to{opacity:1;transform:translateY(0);}}

/* 游戏容器 */
.game-overlay{
    position:fixed;top:0;left:0;width:100%;height:100%;z-index:100;
    display:none;flex-direction:column;align-items:center;justify-content:center;
    background:rgba(0,0,0,0.92);backdrop-filter:blur(4px);-webkit-backdrop-filter:blur(4px);
}
.game-overlay.active{display:flex;}
.game-header{
    display:flex;justify-content:space-between;align-items:center;
    width:100%;max-width:320px;padding:8px 4px;
}
.game-header span{color:rgba(255,255,255,0.7);font-size:13px;font-weight:500;}
.game-close{
    color:rgba(255,255,255,0.6);font-size:13px;cursor:pointer;
    padding:6px 16px;border:1px solid rgba(255,255,255,0.2);border-radius:20px;
    font-weight:500;transition:all 0.15s;
}
.game-close:active{background:rgba(255,255,255,0.1);}
canvas{border-radius:12px;margin-top:8px;touch-action:none;}
</style>
</head>
<body>

<div class="icon-container">
    <svg viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="48" cy="48" r="44" stroke="#dadce0" stroke-width="2"/>
        <circle cx="48" cy="48" r="44" stroke="#dadce0" stroke-width="2" stroke-dasharray="8 6" opacity="0.4"/>
        <path d="M48 28v24" stroke="#9aa0a6" stroke-width="2.5" stroke-linecap="round"/>
        <circle cx="48" cy="62" r="2" fill="#9aa0a6"/>
        <path d="M28 68c4-8 10-14 20-14s16 6 20 14" stroke="#dadce0" stroke-width="1.5" stroke-linecap="round" fill="none" opacity="0.5"/>
    </svg>
</div>

<h1 class="title">${strings.title}</h1>
<p class="subtitle">${strings.subtitle}</p>

<button class="retry-btn" onclick="retryLoad()">${retryBtnText}</button>

${if (autoRetry > 0) """<div class="auto-retry" id="autoRetry">${strings.autoRetryPrefix}<span id="countdown">$autoRetry</span>${strings.autoRetrySuffix}</div>""" else ""}

${if (showGame) """<a class="game-link" onclick="showGame()">${strings.gameLink} →</a>""" else ""}

<div class="error-code">ERR_CONNECTION · ${errorCode}</div>

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

    // ======================== 旧风格兼容 ========================

    private fun generateLegacyPage(
        style: ErrorPageStyle,
        strings: I18nStrings,
        retryBtnText: String,
        showGame: Boolean,
        gameType: MiniGameType,
        autoRetry: Int,
        failedUrl: String?
    ): String {
        val styleCss = ErrorPageStyles.getStyleCss(style)
        val styleBody = ErrorPageStyles.getStyleBody(style, strings.title, strings.subtitle)
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
    display:inline-block;padding:12px 36px;border-radius:24px;border:none;
    color:#fff;font-size:15px;font-weight:500;cursor:pointer;
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

$styleCss
</style>
</head>
<body>
$styleBody

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

    /**
     * 生成自定义媒体错误页
     */
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
}
