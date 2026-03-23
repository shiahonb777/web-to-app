package com.webtoapp.core.extension

import com.webtoapp.core.i18n.Strings

/**
 * 模块代码模板
 * 
 * 提供丰富的代码模板，帮助用户快速创建扩展模块
 */
object ModuleTemplates {
    
    /**
     * 获取所有模板（按分类组织）
     */
    fun getAll(): List<ModuleTemplate> = listOf(
        // 内容过滤类
        elementHider(),
        adBlockerPro(),
        popupBlocker(),
        cookieBannerRemover(),
        
        // 样式修改类
        cssInjector(),
        darkModeForce(),
        fontChanger(),
        colorTheme(),
        layoutFixer(),
        
        // 功能增强类
        autoClicker(),
        formFiller(),
        pageModifier(),
        customButton(),
        keyboardShortcuts(),
        autoRefresh(),
        scrollToTop(),
        
        // 数据提取类
        dataExtractor(),
        linkCollector(),
        imageGrabber(),
        
        // Media处理类
        videoEnhancer(),
        imageZoomer(),
        audioController(),
        
        // Security隐私类
        notificationBlocker(),
        trackingBlocker(),
        fingerprintProtector(),
        
        // 开发调试类
        consoleLogger(),
        networkMonitor(),
        domInspector()
    )
    
    /**
     * 按分类获取模板
     */
    fun getByCategory(category: ModuleCategory): List<ModuleTemplate> {
        return getAll().filter { it.category == category }
    }

    // ==================== 内容过滤类模板 ====================
    
    private fun elementHider() = ModuleTemplate(
        id = "template-element-hider",
        name = Strings.tplElementHider,
        description = Strings.tplElementHiderDesc,
        icon = "🚫",
        category = ModuleCategory.CONTENT_FILTER,
        configItems = listOf(
            ModuleConfigItem(
                key = "selectors",
                name = Strings.configCssSelector,
                description = Strings.configCssSelectorDesc,
                type = ConfigItemType.TEXTAREA,
                defaultValue = ".ad-banner\n#popup\n[class*=\"advertisement\"]",
                placeholder = Strings.configCssSelectorPlaceholder
            ),
            ModuleConfigItem(
                key = "hideMethod",
                name = Strings.configHideMethod,
                type = ConfigItemType.SELECT,
                defaultValue = "display",
                options = listOf("display", "visibility", "opacity", "remove")
            )
        ),
        code = """
const selectors = getConfig('selectors', '').split('\n').filter(s => s.trim());
const hideMethod = getConfig('hideMethod', 'display');

function hideElements() {
    selectors.forEach(selector => {
        try {
            document.querySelectorAll(selector.trim()).forEach(el => {
                switch(hideMethod) {
                    case 'display': el.style.setProperty('display', 'none', 'important'); break;
                    case 'visibility': el.style.setProperty('visibility', 'hidden', 'important'); break;
                    case 'opacity': el.style.setProperty('opacity', '0', 'important'); break;
                    case 'remove': el.remove(); break;
                }
            });
        } catch(e) { console.warn('[ElementHider] Invalid selector:', selector); }
    });
}

hideElements();
const observer = new MutationObserver(hideElements);
observer.observe(document.body, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun adBlockerPro() = ModuleTemplate(
        id = "template-adblocker-pro",
        name = Strings.tplAdBlocker,
        description = Strings.tplAdBlockerDesc,
        icon = "🛡️",
        category = ModuleCategory.CONTENT_FILTER,
        configItems = listOf(
            ModuleConfigItem(
                key = "blockPopups",
                name = Strings.configBlockPopups,
                type = ConfigItemType.BOOLEAN,
                defaultValue = "true"
            ),
            ModuleConfigItem(
                key = "blockOverlays",
                name = Strings.configBlockOverlays,
                type = ConfigItemType.BOOLEAN,
                defaultValue = "true"
            )
        ),
        code = """
const blockPopups = getConfig('blockPopups', 'true') === 'true';
const blockOverlays = getConfig('blockOverlays', 'true') === 'true';

// 拦截弹窗
if (blockPopups) {
    window.open = () => { console.log('[AdBlocker] Blocked popup'); return null; };
}

// Ad选择器
const adSelectors = [
    '[class*="ad-"]', '[class*="ads-"]', '[class*="advert"]',
    '[id*="ad-"]', '[id*="ads-"]', 'ins.adsbygoogle',
    '[data-ad]', '[data-ads]', '.sponsored', '.promotion'
];

function removeAds() {
    adSelectors.forEach(sel => {
        document.querySelectorAll(sel).forEach(el => el.style.display = 'none');
    });
    
    if (blockOverlays) {
        document.querySelectorAll('[class*="overlay"], [class*="modal"]').forEach(el => {
            if (el.style.position === 'fixed' || getComputedStyle(el).position === 'fixed') {
                el.style.display = 'none';
            }
        });
    }
}

removeAds();
new MutationObserver(removeAds).observe(document.documentElement, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = """
[class*="ad-"], [class*="ads-"], [class*="advert"],
ins.adsbygoogle, .adsbygoogle, [data-ad] {
    display: none !important;
}
        """.trimIndent()
    )

    private fun popupBlocker() = ModuleTemplate(
        id = "template-popup-blocker",
        name = Strings.tplPopupBlocker,
        description = Strings.tplPopupBlockerDesc,
        icon = "🔕",
        category = ModuleCategory.CONTENT_FILTER,
        configItems = listOf(
            ModuleConfigItem(
                key = "autoClose",
                name = Strings.configAutoCloseDelay,
                type = ConfigItemType.NUMBER,
                defaultValue = "500"
            )
        ),
        code = """
const autoCloseDelay = parseInt(getConfig('autoClose', '500'));

// 拦截通知权限
if (window.Notification) {
    Notification.requestPermission = () => Promise.resolve('denied');
    Object.defineProperty(Notification, 'permission', { get: () => 'denied' });
}

// Auto点击关闭按钮
function closePopups() {
    const closeSelectors = [
        '[class*="close"]', '[class*="dismiss"]', '[aria-label*="Close"]',
        '[aria-label*="Close"]', 'button[class*="cancel"]', '.modal-close'
    ];
    closeSelectors.forEach(sel => {
        document.querySelectorAll(sel).forEach(btn => {
            if (btn.offsetParent !== null) btn.click();
        });
    });
}

setTimeout(closePopups, autoCloseDelay);
new MutationObserver(() => setTimeout(closePopups, 100))
    .observe(document.body, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun cookieBannerRemover() = ModuleTemplate(
        id = "template-cookie-banner",
        name = Strings.tplCookieBanner,
        description = Strings.tplCookieBannerDesc,
        icon = "🍪",
        category = ModuleCategory.CONTENT_FILTER,
        configItems = emptyList(),
        code = """
const cookieSelectors = [
    '[class*="cookie"]', '[id*="cookie"]', '[class*="consent"]',
    '[class*="gdpr"]', '[id*="gdpr"]', '.cc-banner', '#CybotCookiebotDialog'
];

function removeCookieBanners() {
    cookieSelectors.forEach(sel => {
        document.querySelectorAll(sel).forEach(el => {
            if (el.textContent.toLowerCase().includes('cookie') ||
                el.textContent.toLowerCase().includes('consent')) {
                el.remove();
            }
        });
    });
    // 恢复滚动
    document.body.style.overflow = '';
    document.documentElement.style.overflow = '';
}

removeCookieBanners();
setTimeout(removeCookieBanners, 1000);
new MutationObserver(removeCookieBanners).observe(document.body, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = ""
    )
    
    // ==================== 样式修改类模板 ====================
    
    private fun cssInjector() = ModuleTemplate(
        id = "template-css-injector",
        name = Strings.tplCssInjector,
        description = Strings.tplCssInjectorDesc,
        icon = "🎨",
        category = ModuleCategory.STYLE_MODIFIER,
        configItems = listOf(
            ModuleConfigItem(
                key = "customCss",
                name = Strings.configCssCode,
                type = ConfigItemType.TEXTAREA,
                defaultValue = "body { font-size: 16px !important; }"
            )
        ),
        code = """
const customCss = getConfig('customCss', '');
if (customCss) {
    const style = document.createElement('style');
    style.textContent = customCss;
    document.head.appendChild(style);
}
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun darkModeForce() = ModuleTemplate(
        id = "template-dark-mode",
        name = Strings.tplDarkMode,
        description = Strings.tplDarkModeDesc,
        icon = "🌙",
        category = ModuleCategory.STYLE_MODIFIER,
        configItems = listOf(
            ModuleConfigItem(
                key = "brightness",
                name = Strings.configBrightness,
                type = ConfigItemType.NUMBER,
                defaultValue = "90"
            ),
            ModuleConfigItem(
                key = "contrast",
                name = Strings.configContrast,
                type = ConfigItemType.NUMBER,
                defaultValue = "100"
            )
        ),
        code = """
const brightness = parseInt(getConfig('brightness', '90')) / 100;
const contrast = parseInt(getConfig('contrast', '100')) / 100;
document.documentElement.style.filter = `invert(1) hue-rotate(180deg) brightness(${'$'}{brightness}) contrast(${'$'}{contrast})`;
        """.trimIndent(),
        cssCode = """
html { filter: invert(1) hue-rotate(180deg); background: #1a1a1a !important; }
img, video, canvas, svg, [style*="background-image"] { filter: invert(1) hue-rotate(180deg); }
        """.trimIndent()
    )

    private fun fontChanger() = ModuleTemplate(
        id = "template-font-changer",
        name = Strings.tplFontChanger,
        description = Strings.tplFontChangerDesc,
        icon = "🔤",
        category = ModuleCategory.STYLE_MODIFIER,
        configItems = listOf(
            ModuleConfigItem(
                key = "fontFamily",
                name = Strings.configFont,
                type = ConfigItemType.SELECT,
                defaultValue = "system-ui",
                options = listOf("system-ui", "Microsoft YaHei", "PingFang SC", "Noto Sans SC", "Arial", "Georgia")
            ),
            ModuleConfigItem(
                key = "fontSize",
                name = Strings.configFontSize,
                type = ConfigItemType.NUMBER,
                defaultValue = "16"
            )
        ),
        code = """
const fontFamily = getConfig('fontFamily', 'system-ui');
const fontSize = getConfig('fontSize', '16');
const style = document.createElement('style');
style.textContent = `* { font-family: "${'$'}{fontFamily}", sans-serif !important; font-size: ${'$'}{fontSize}px !important; }`;
document.head.appendChild(style);
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun colorTheme() = ModuleTemplate(
        id = "template-color-theme",
        name = Strings.templateColorTheme,
        description = Strings.templateColorThemeDesc,
        icon = "🎭",
        category = ModuleCategory.STYLE_MODIFIER,
        configItems = listOf(
            ModuleConfigItem(
                key = "bgColor",
                name = Strings.templateBgColor,
                type = ConfigItemType.COLOR,
                defaultValue = "#ffffff"
            ),
            ModuleConfigItem(
                key = "textColor",
                name = Strings.templateTextColor,
                type = ConfigItemType.COLOR,
                defaultValue = "#333333"
            ),
            ModuleConfigItem(
                key = "linkColor",
                name = Strings.templateLinkColor,
                type = ConfigItemType.COLOR,
                defaultValue = "#0066cc"
            )
        ),
        code = """
const bgColor = getConfig('bgColor', '#ffffff');
const textColor = getConfig('textColor', '#333333');
const linkColor = getConfig('linkColor', '#0066cc');
const style = document.createElement('style');
style.textContent = `
    body { background-color: ${'$'}{bgColor} !important; color: ${'$'}{textColor} !important; }
    a { color: ${'$'}{linkColor} !important; }
`;
document.head.appendChild(style);
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun layoutFixer() = ModuleTemplate(
        id = "template-layout-fixer",
        name = Strings.templateLayoutFixer,
        description = Strings.templateLayoutFixerDesc,
        icon = "📐",
        category = ModuleCategory.STYLE_MODIFIER,
        configItems = listOf(
            ModuleConfigItem(
                key = "maxWidth",
                name = Strings.templateMaxWidth,
                type = ConfigItemType.NUMBER,
                defaultValue = "1200"
            ),
            ModuleConfigItem(
                key = "centerContent",
                name = Strings.templateCenterContent,
                type = ConfigItemType.BOOLEAN,
                defaultValue = "true"
            )
        ),
        code = "",
        cssCode = """
body > * { max-width: var(--max-width, 1200px) !important; margin-left: auto !important; margin-right: auto !important; }
        """.trimIndent()
    )
    
    // ==================== 功能增强类模板 ====================
    
    private fun autoClicker() = ModuleTemplate(
        id = "template-auto-clicker",
        name = Strings.templateAutoClicker,
        description = Strings.templateAutoClickerDesc,
        icon = "🖱️",
        category = ModuleCategory.FUNCTION_ENHANCE,
        configItems = listOf(
            ModuleConfigItem(
                key = "selector",
                name = Strings.templateClickTarget,
                type = ConfigItemType.TEXT,
                defaultValue = ".close-btn",
                placeholder = "CSS Selector"
            ),
            ModuleConfigItem(
                key = "delay",
                name = Strings.templateDelay,
                type = ConfigItemType.NUMBER,
                defaultValue = "1000"
            ),
            ModuleConfigItem(
                key = "repeat",
                name = Strings.templateRepeatClick,
                type = ConfigItemType.BOOLEAN,
                defaultValue = "false"
            )
        ),
        code = """
const selector = getConfig('selector', '.close-btn');
const delay = parseInt(getConfig('delay', '1000'));
const repeat = getConfig('repeat', 'false') === 'true';

function autoClick() {
    const el = document.querySelector(selector);
    if (el && !el.dataset.autoClicked) {
        el.click();
        if (!repeat) el.dataset.autoClicked = 'true';
        console.log('[AutoClicker] Clicked:', selector);
    }
}

setTimeout(autoClick, delay);
if (repeat) setInterval(autoClick, delay);
new MutationObserver(autoClick).observe(document.body, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = ""
    )

    private fun formFiller() = ModuleTemplate(
        id = "template-form-filler",
        name = Strings.templateFormFiller,
        description = Strings.templateFormFillerDesc,
        icon = "📝",
        category = ModuleCategory.FUNCTION_ENHANCE,
        configItems = listOf(
            ModuleConfigItem(
                key = "fieldSelector",
                name = Strings.templateFieldSelector,
                type = ConfigItemType.TEXT,
                defaultValue = "input[name=\"username\"]"
            ),
            ModuleConfigItem(
                key = "fieldValue",
                name = Strings.templateFieldValue,
                type = ConfigItemType.TEXT,
                defaultValue = ""
            )
        ),
        code = """
const fieldSelector = getConfig('fieldSelector', '');
const fieldValue = getConfig('fieldValue', '');

function fillForm() {
    const field = document.querySelector(fieldSelector);
    if (field && fieldValue) {
        field.value = fieldValue;
        field.dispatchEvent(new Event('input', { bubbles: true }));
        field.dispatchEvent(new Event('change', { bubbles: true }));
    }
}

fillForm();
new MutationObserver(fillForm).observe(document.body, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun pageModifier() = ModuleTemplate(
        id = "template-page-modifier",
        name = Strings.templatePageModifier,
        description = Strings.templatePageModifierDesc,
        icon = "✏️",
        category = ModuleCategory.FUNCTION_ENHANCE,
        configItems = listOf(
            ModuleConfigItem(
                key = "selector",
                name = Strings.templateTargetSelector,
                type = ConfigItemType.TEXT,
                defaultValue = "h1"
            ),
            ModuleConfigItem(
                key = "newText",
                name = Strings.templateNewText,
                type = ConfigItemType.TEXT,
                defaultValue = ""
            ),
            ModuleConfigItem(
                key = "newStyle",
                name = Strings.templateNewStyle,
                type = ConfigItemType.TEXT,
                defaultValue = "color: red;"
            )
        ),
        code = """
const selector = getConfig('selector', 'h1');
const newText = getConfig('newText', '');
const newStyle = getConfig('newStyle', '');

function modifyPage() {
    document.querySelectorAll(selector).forEach(el => {
        if (newText) el.textContent = newText;
        if (newStyle) el.style.cssText += newStyle;
    });
}

modifyPage();
new MutationObserver(modifyPage).observe(document.body, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun customButton() = ModuleTemplate(
        id = "template-custom-button",
        name = Strings.templateCustomButton,
        description = Strings.templateCustomButtonDesc,
        icon = "🔘",
        category = ModuleCategory.FUNCTION_ENHANCE,
        configItems = listOf(
            ModuleConfigItem(
                key = "buttonText",
                name = Strings.templateButtonText,
                type = ConfigItemType.TEXT,
                defaultValue = "⬆️"
            ),
            ModuleConfigItem(
                key = "action",
                name = Strings.templateClickAction,
                type = ConfigItemType.TEXTAREA,
                defaultValue = "window.scrollTo({ top: 0, behavior: 'smooth' });"
            ),
            ModuleConfigItem(
                key = "position",
                name = Strings.templatePosition,
                type = ConfigItemType.SELECT,
                defaultValue = "bottom-right",
                options = listOf("bottom-right", "bottom-left", "top-right", "top-left")
            )
        ),
        code = """
const buttonText = getConfig('buttonText', '⬆️');
const action = getConfig('action', 'window.scrollTo({ top: 0, behavior: "smooth" });');
const position = getConfig('position', 'bottom-right');

const btn = document.createElement('div');
btn.textContent = buttonText;
const positions = {
    'bottom-right': 'bottom: 80px; right: 20px;',
    'bottom-left': 'bottom: 80px; left: 20px;',
    'top-right': 'top: 80px; right: 20px;',
    'top-left': 'top: 80px; left: 20px;'
};
btn.style.cssText = `position: fixed; ${'$'}{positions[position]} z-index: 99999; width: 50px; height: 50px; border-radius: 50%; background: rgba(0,0,0,0.7); color: white; display: flex; align-items: center; justify-content: center; font-size: 24px; cursor: pointer; box-shadow: 0 2px 10px rgba(0,0,0,0.3);`;
btn.onclick = () => { try { eval(action); } catch(e) { console.error(e); } };
document.body.appendChild(btn);
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun keyboardShortcuts() = ModuleTemplate(
        id = "template-keyboard-shortcuts",
        name = Strings.templateKeyboardShortcuts,
        description = Strings.templateKeyboardShortcutsDesc,
        icon = "⌨️",
        category = ModuleCategory.FUNCTION_ENHANCE,
        configItems = listOf(
            ModuleConfigItem(
                key = "shortcuts",
                name = Strings.templateShortcutsConfig,
                description = "Format: key=action, one per line",
                type = ConfigItemType.TEXTAREA,
                defaultValue = "t=window.scrollTo({top:0,behavior:'smooth'})\nb=window.scrollTo({top:document.body.scrollHeight,behavior:'smooth'})"
            )
        ),
        code = """
const shortcuts = getConfig('shortcuts', '').split('\n').filter(s => s.includes('='));
const keyMap = {};
shortcuts.forEach(s => {
    const [key, action] = s.split('=');
    keyMap[key.trim().toLowerCase()] = action;
});

document.addEventListener('keydown', e => {
    if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
    const action = keyMap[e.key.toLowerCase()];
    if (action) { e.preventDefault(); try { eval(action); } catch(err) { console.error(err); } }
});
        """.trimIndent(),
        cssCode = ""
    )

    private fun autoRefresh() = ModuleTemplate(
        id = "template-auto-refresh",
        name = Strings.templateAutoRefresh,
        description = Strings.templateAutoRefreshDesc,
        icon = "🔄",
        category = ModuleCategory.FUNCTION_ENHANCE,
        configItems = listOf(
            ModuleConfigItem(
                key = "interval",
                name = Strings.templateRefreshInterval,
                type = ConfigItemType.NUMBER,
                defaultValue = "60"
            ),
            ModuleConfigItem(
                key = "showCountdown",
                name = Strings.templateShowCountdown,
                type = ConfigItemType.BOOLEAN,
                defaultValue = "true"
            )
        ),
        code = """
const interval = parseInt(getConfig('interval', '60'));
const showCountdown = getConfig('showCountdown', 'true') === 'true';
let countdown = interval;

if (showCountdown) {
    const display = document.createElement('div');
    display.style.cssText = 'position:fixed;top:10px;right:10px;z-index:99999;background:rgba(0,0,0,0.7);color:white;padding:5px 10px;border-radius:5px;font-size:12px;';
    document.body.appendChild(display);
    
    setInterval(() => {
        countdown--;
        display.textContent = `刷新: ${'$'}{countdown}s`;
        if (countdown <= 0) location.reload();
    }, 1000);
} else {
    setTimeout(() => location.reload(), interval * 1000);
}
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun scrollToTop() = ModuleTemplate(
        id = "template-scroll-to-top",
        name = Strings.templateScrollToTop,
        description = Strings.templateScrollToTopDesc,
        icon = "⬆️",
        category = ModuleCategory.FUNCTION_ENHANCE,
        configItems = listOf(
            ModuleConfigItem(
                key = "showAfter",
                name = Strings.templateShowAfterScroll,
                type = ConfigItemType.NUMBER,
                defaultValue = "300"
            )
        ),
        code = """
const showAfter = parseInt(getConfig('showAfter', '300'));
const btn = document.createElement('div');
btn.innerHTML = '⬆️';
btn.style.cssText = 'position:fixed;bottom:80px;right:20px;z-index:99999;width:50px;height:50px;border-radius:50%;background:rgba(0,0,0,0.7);color:white;display:none;align-items:center;justify-content:center;font-size:24px;cursor:pointer;box-shadow:0 2px 10px rgba(0,0,0,0.3);';
btn.onclick = () => window.scrollTo({ top: 0, behavior: 'smooth' });
document.body.appendChild(btn);

window.addEventListener('scroll', () => {
    btn.style.display = window.scrollY > showAfter ? 'flex' : 'none';
});
        """.trimIndent(),
        cssCode = ""
    )
    
    // ==================== 数据提取类模板 ====================
    
    private fun dataExtractor() = ModuleTemplate(
        id = "template-data-extractor",
        name = Strings.templateDataExtractor,
        description = Strings.templateDataExtractorDesc,
        icon = "📊",
        category = ModuleCategory.DATA_EXTRACT,
        configItems = listOf(
            ModuleConfigItem(
                key = "selector",
                name = Strings.templateDataSelector,
                type = ConfigItemType.TEXT,
                defaultValue = "table tr"
            ),
            ModuleConfigItem(
                key = "attribute",
                name = Strings.templateExtractAttribute,
                description = "Leave empty to extract text",
                type = ConfigItemType.TEXT,
                defaultValue = ""
            )
        ),
        code = """
const selector = getConfig('selector', 'table tr');
const attribute = getConfig('attribute', '');

function extractData() {
    const elements = document.querySelectorAll(selector);
    const data = [];
    elements.forEach(el => {
        data.push(attribute ? el.getAttribute(attribute) : el.textContent.trim());
    });
    console.log('[DataExtractor] Extracted', data.length, 'items:');
    console.table(data);
    window.__extractedData = data;
    return data;
}

window.__extractedData = extractData();
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun linkCollector() = ModuleTemplate(
        id = "template-link-collector",
        name = Strings.templateLinkCollector,
        description = Strings.templateLinkCollectorDesc,
        icon = "🔗",
        category = ModuleCategory.DATA_EXTRACT,
        configItems = listOf(
            ModuleConfigItem(
                key = "filter",
                name = Strings.templateFilterKeyword,
                description = "Only collect links containing keyword",
                type = ConfigItemType.TEXT,
                defaultValue = ""
            )
        ),
        code = """
const filter = getConfig('filter', '').toLowerCase();
const links = Array.from(document.querySelectorAll('a[href]'))
    .map(a => ({ text: a.textContent.trim(), href: a.href }))
    .filter(l => !filter || l.href.toLowerCase().includes(filter) || l.text.toLowerCase().includes(filter));

console.log('[LinkCollector] Found', links.length, 'links:');
console.table(links);
window.__collectedLinks = links;
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun imageGrabber() = ModuleTemplate(
        id = "template-image-grabber",
        name = Strings.templateImageGrabber,
        description = Strings.templateImageGrabberDesc,
        icon = "🖼️",
        category = ModuleCategory.DATA_EXTRACT,
        configItems = listOf(
            ModuleConfigItem(
                key = "minSize",
                name = Strings.templateMinSize,
                type = ConfigItemType.NUMBER,
                defaultValue = "100"
            )
        ),
        code = """
const minSize = parseInt(getConfig('minSize', '100'));
const images = Array.from(document.querySelectorAll('img'))
    .filter(img => img.naturalWidth >= minSize && img.naturalHeight >= minSize)
    .map(img => ({ src: img.src, width: img.naturalWidth, height: img.naturalHeight, alt: img.alt }));

console.log('[ImageGrabber] Found', images.length, 'images:');
console.table(images);
window.__grabbedImages = images;
        """.trimIndent(),
        cssCode = ""
    )

    // ==================== 媒体处理类模板 ====================
    
    private fun videoEnhancer() = ModuleTemplate(
        id = "template-video-enhancer",
        name = Strings.templateVideoEnhancer,
        description = Strings.templateVideoEnhancerDesc,
        icon = "🎬",
        category = ModuleCategory.MEDIA,
        configItems = listOf(
            ModuleConfigItem(
                key = "defaultSpeed",
                name = Strings.templateDefaultSpeed,
                type = ConfigItemType.SELECT,
                defaultValue = "1",
                options = listOf("0.5", "0.75", "1", "1.25", "1.5", "1.75", "2", "2.5", "3")
            ),
            ModuleConfigItem(
                key = "showControls",
                name = Strings.templateShowControlPanel,
                type = ConfigItemType.BOOLEAN,
                defaultValue = "true"
            )
        ),
        code = """
const defaultSpeed = parseFloat(getConfig('defaultSpeed', '1'));
const showControls = getConfig('showControls', 'true') === 'true';
const speeds = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3];
let currentSpeedIndex = speeds.indexOf(defaultSpeed);

function enhanceVideo(video) {
    if (video.dataset.enhanced) return;
    video.dataset.enhanced = 'true';
    video.playbackRate = defaultSpeed;
    
    if (showControls) {
        const panel = document.createElement('div');
        panel.style.cssText = 'position:absolute;top:10px;right:10px;z-index:9999;background:rgba(0,0,0,0.7);color:white;padding:5px 10px;border-radius:5px;font-size:14px;cursor:pointer;';
        panel.textContent = defaultSpeed + 'x';
        panel.onclick = () => {
            currentSpeedIndex = (currentSpeedIndex + 1) % speeds.length;
            video.playbackRate = speeds[currentSpeedIndex];
            panel.textContent = speeds[currentSpeedIndex] + 'x';
        };
        video.parentElement.style.position = 'relative';
        video.parentElement.appendChild(panel);
    }
}

document.querySelectorAll('video').forEach(enhanceVideo);
new MutationObserver(muts => {
    muts.forEach(m => m.addedNodes.forEach(n => {
        if (n.nodeName === 'VIDEO') enhanceVideo(n);
        else if (n.querySelectorAll) n.querySelectorAll('video').forEach(enhanceVideo);
    }));
}).observe(document.body, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun imageZoomer() = ModuleTemplate(
        id = "template-image-zoomer",
        name = Strings.templateImageZoomer,
        description = Strings.templateImageZoomerDesc,
        icon = "🔍",
        category = ModuleCategory.MEDIA,
        configItems = emptyList(),
        code = """
document.addEventListener('click', e => {
    const img = e.target.closest('img');
    if (!img) return;
    
    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.9);z-index:999999;display:flex;align-items:center;justify-content:center;cursor:zoom-out;';
    
    const bigImg = document.createElement('img');
    bigImg.src = img.src;
    bigImg.style.cssText = 'max-width:95%;max-height:95%;object-fit:contain;';
    
    overlay.appendChild(bigImg);
    overlay.onclick = () => overlay.remove();
    document.body.appendChild(overlay);
});
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun audioController() = ModuleTemplate(
        id = "template-audio-controller",
        name = Strings.templateAudioController,
        description = Strings.templateAudioControllerDesc,
        icon = "🎵",
        category = ModuleCategory.MEDIA,
        configItems = listOf(
            ModuleConfigItem(
                key = "defaultVolume",
                name = Strings.templateDefaultVolume,
                type = ConfigItemType.NUMBER,
                defaultValue = "50"
            )
        ),
        code = """
const defaultVolume = parseInt(getConfig('defaultVolume', '50')) / 100;

function controlAudio(audio) {
    if (audio.dataset.controlled) return;
    audio.dataset.controlled = 'true';
    audio.volume = defaultVolume;
}

document.querySelectorAll('audio, video').forEach(controlAudio);
new MutationObserver(muts => {
    muts.forEach(m => m.addedNodes.forEach(n => {
        if (n.nodeName === 'AUDIO' || n.nodeName === 'VIDEO') controlAudio(n);
        else if (n.querySelectorAll) n.querySelectorAll('audio, video').forEach(controlAudio);
    }));
}).observe(document.body, { childList: true, subtree: true });
        """.trimIndent(),
        cssCode = ""
    )
    
    // ==================== 安全隐私类模板 ====================
    
    private fun notificationBlocker() = ModuleTemplate(
        id = "template-notification-blocker",
        name = Strings.templateNotificationBlocker,
        description = Strings.templateNotificationBlockerDesc,
        icon = "🔕",
        category = ModuleCategory.SECURITY,
        configItems = emptyList(),
        code = """
if (window.Notification) {
    Notification.requestPermission = () => Promise.resolve('denied');
    Object.defineProperty(Notification, 'permission', { get: () => 'denied' });
}
console.log('[NotificationBlocker] Notifications blocked');
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun trackingBlocker() = ModuleTemplate(
        id = "template-tracking-blocker",
        name = Strings.templateTrackingBlocker,
        description = Strings.templateTrackingBlockerDesc,
        icon = "🕵️",
        category = ModuleCategory.SECURITY,
        configItems = emptyList(),
        code = """
// 阻止常见追踪
const trackers = ['google-analytics', 'googletagmanager', 'facebook', 'hotjar', 'mixpanel'];
const originalFetch = window.fetch;
window.fetch = function(url, ...args) {
    if (trackers.some(t => url.includes(t))) {
        console.log('[TrackingBlocker] Blocked:', url);
        return Promise.resolve(new Response('', { status: 200 }));
    }
    return originalFetch.apply(this, [url, ...args]);
};

// 阻止 beacon
navigator.sendBeacon = () => { console.log('[TrackingBlocker] Beacon blocked'); return false; };
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun fingerprintProtector() = ModuleTemplate(
        id = "template-fingerprint-protector",
        name = Strings.templateFingerprintProtector,
        description = Strings.templateFingerprintProtectorDesc,
        icon = "🛡️",
        category = ModuleCategory.SECURITY,
        configItems = emptyList(),
        code = """
// Shuffle化 canvas 指纹
const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
HTMLCanvasElement.prototype.toDataURL = function() {
    const ctx = this.getContext('2d');
    if (ctx) {
        const imageData = ctx.getImageData(0, 0, this.width, this.height);
        for (let i = 0; i < imageData.data.length; i += 4) {
            imageData.data[i] ^= Math.random() > 0.5 ? 1 : 0;
        }
        ctx.putImageData(imageData, 0, 0);
    }
    return originalToDataURL.apply(this, arguments);
};

// Shuffle化屏幕信息
Object.defineProperty(screen, 'width', { get: () => 1920 + Math.floor(Math.random() * 100) });
Object.defineProperty(screen, 'height', { get: () => 1080 + Math.floor(Math.random() * 100) });
        """.trimIndent(),
        cssCode = ""
    )

    // ==================== 开发调试类模板 ====================
    
    private fun consoleLogger() = ModuleTemplate(
        id = "template-console-logger",
        name = Strings.templateConsoleLogger,
        description = Strings.templateConsoleLoggerDesc,
        icon = "📋",
        category = ModuleCategory.DEVELOPER,
        configItems = listOf(
            ModuleConfigItem(
                key = "maxLogs",
                name = Strings.templateMaxLogs,
                type = ConfigItemType.NUMBER,
                defaultValue = "50"
            )
        ),
        code = """
const maxLogs = parseInt(getConfig('maxLogs', '50'));
const logs = [];

const panel = document.createElement('div');
panel.style.cssText = 'position:fixed;bottom:0;left:0;right:0;height:200px;background:#1e1e1e;color:#fff;font-family:monospace;font-size:12px;overflow-y:auto;z-index:999999;padding:10px;border-top:2px solid #333;';
panel.innerHTML = '<div style="margin-bottom:5px;color:#888;">📋 Console Logger</div>';
const logContainer = document.createElement('div');
panel.appendChild(logContainer);
document.body.appendChild(panel);

function addLog(type, args) {
    const colors = { log: '#fff', warn: '#ff0', error: '#f55', info: '#5af' };
    const log = document.createElement('div');
    log.style.color = colors[type] || '#fff';
    log.textContent = `[${'$'}{type}] ${'$'}{Array.from(args).map(a => typeof a === 'object' ? JSON.stringify(a) : a).join(' ')}`;
    logs.push(log);
    if (logs.length > maxLogs) logs.shift().remove();
    logContainer.appendChild(log);
    panel.scrollTop = panel.scrollHeight;
}

['log', 'warn', 'error', 'info'].forEach(type => {
    const original = console[type];
    console[type] = function(...args) { addLog(type, args); original.apply(console, args); };
});
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun networkMonitor() = ModuleTemplate(
        id = "template-network-monitor",
        name = Strings.templateNetworkMonitor,
        description = Strings.templateNetworkMonitorDesc,
        icon = "🌐",
        category = ModuleCategory.DEVELOPER,
        configItems = emptyList(),
        code = """
const requests = [];
const panel = document.createElement('div');
panel.style.cssText = 'position:fixed;top:10px;right:10px;width:300px;max-height:400px;background:#1e1e1e;color:#fff;font-family:monospace;font-size:11px;overflow-y:auto;z-index:999999;padding:10px;border-radius:8px;box-shadow:0 4px 20px rgba(0,0,0,0.5);';
panel.innerHTML = '<div style="margin-bottom:5px;color:#888;">🌐 Network Monitor</div>';
const list = document.createElement('div');
panel.appendChild(list);
document.body.appendChild(panel);

const originalFetch = window.fetch;
window.fetch = async function(url, ...args) {
    const start = Date.now();
    try {
        const res = await originalFetch.apply(this, [url, ...args]);
        addRequest(url, res.status, Date.now() - start);
        return res;
    } catch(e) {
        addRequest(url, 'ERR', Date.now() - start);
        throw e;
    }
};

function addRequest(url, status, time) {
    const item = document.createElement('div');
    item.style.cssText = 'padding:4px 0;border-bottom:1px solid #333;';
    const color = status === 200 ? '#5f5' : status === 'ERR' ? '#f55' : '#ff0';
    item.innerHTML = `<span style="color:${'$'}{color}">${'$'}{status}</span> <span style="color:#888">${'$'}{time}ms</span> ${'$'}{url.substring(0, 40)}...`;
    list.appendChild(item);
    if (list.children.length > 20) list.firstChild.remove();
}
        """.trimIndent(),
        cssCode = ""
    )
    
    private fun domInspector() = ModuleTemplate(
        id = "template-dom-inspector",
        name = Strings.templateDomInspector,
        description = Strings.templateDomInspectorDesc,
        icon = "🔎",
        category = ModuleCategory.DEVELOPER,
        configItems = emptyList(),
        code = """
const tooltip = document.createElement('div');
tooltip.style.cssText = 'position:fixed;z-index:999999;background:#1e1e1e;color:#fff;font-family:monospace;font-size:11px;padding:8px;border-radius:4px;pointer-events:none;display:none;max-width:300px;box-shadow:0 2px 10px rgba(0,0,0,0.5);';
document.body.appendChild(tooltip);

let lastEl = null;
document.addEventListener('mousemove', e => {
    const el = document.elementFromPoint(e.clientX, e.clientY);
    if (!el || el === tooltip || el === lastEl) return;
    lastEl = el;
    
    const tag = el.tagName.toLowerCase();
    const id = el.id ? `#${'$'}{el.id}` : '';
    const classes = el.className ? `.${'$'}{el.className.split(' ').join('.')}` : '';
    const size = `${'$'}{el.offsetWidth}x${'$'}{el.offsetHeight}`;
    
    tooltip.innerHTML = `<div style="color:#5af">${'$'}{tag}${'$'}{id}${'$'}{classes}</div><div style="color:#888">Size: ${'$'}{size}</div>`;
    tooltip.style.display = 'block';
    tooltip.style.left = Math.min(e.clientX + 10, window.innerWidth - 320) + 'px';
    tooltip.style.top = Math.min(e.clientY + 10, window.innerHeight - 100) + 'px';
    
    el.style.outline = '2px solid #5af';
});

document.addEventListener('mouseout', e => {
    if (lastEl) lastEl.style.outline = '';
    tooltip.style.display = 'none';
});
        """.trimIndent(),
        cssCode = ""
    )
}

/**
 * 模块模板数据类
 */
data class ModuleTemplate(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val category: ModuleCategory,
    val configItems: List<ModuleConfigItem>,
    val code: String,
    val cssCode: String,
    val uiConfig: ModuleUiConfig = ModuleUiConfig()  // 新增 UI 配置
) {
    /**
     * 转换为 ExtensionModule
     */
    fun toModule(
        moduleName: String = name,
        moduleDescription: String = description
    ): ExtensionModule {
        return ExtensionModule(
            name = moduleName,
            description = moduleDescription,
            icon = icon,
            category = category,
            code = code,
            cssCode = cssCode,
            configItems = configItems,
            configValues = configItems.associate { it.key to it.defaultValue },
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            uiConfig = uiConfig  // 传递 UI 配置
        )
    }
}
