package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.AppStringsProvider

internal fun pageEnhance() = CodeSnippetCategory(
        id = "enhance",
        name = AppStringsProvider.current().snippetEnhance,
        icon = "✨",
        description = AppStringsProvider.current().snippetEnhanceDesc,
        snippets = listOf(
            CodeSnippet(
                id = "enhance-reading-mode",
                name = AppStringsProvider.current().snippetReadingMode,
                description = AppStringsProvider.current().snippetReadingModeDesc,
                code = """function enableReadingMode() {
    const article = document.querySelector('article') || 
                   document.querySelector('[class*="content"]') ||
                   document.querySelector('main') ||
                   document.body;
    
    const title = document.querySelector('h1')?.textContent || document.title;
    const content = article.innerHTML;
    
    document.body.innerHTML = `
        <div style="max-width: 700px; margin: 0 auto; padding: 40px 20px;
            font-size: 18px; line-height: 1.8; font-family: Georgia, serif;">
            <h1 style="font-size: 28px; margin-bottom: 30px;">${"$"}{title}</h1>
            <div>${"$"}{content}</div>
            <button onclick="location.reload()" style="
                position: fixed; bottom: 20px; right: 20px;
                padding: 10px 20px; background: #333; color: white;
                border: none; border-radius: 5px; cursor: pointer;
            ">退出阅读模式</button>
        </div>
    `;
}
enableReadingMode();""",
                tags = listOf(AppStringsProvider.current().tagReading, AppStringsProvider.current().tagSimplify)
            ),
            CodeSnippet(
                id = "enhance-copy-unlock",
                name = AppStringsProvider.current().snippetCopyUnlock,
                description = AppStringsProvider.current().snippetCopyUnlockDesc,
                code = """// Inject样式
const style = document.createElement('style');
style.textContent = `
    * {
        -webkit-user-select: auto !important;
        -moz-user-select: auto !important;
        user-select: auto !important;
    }
`;
document.head.appendChild(style);

// observe.
['copy', 'cut', 'paste', 'selectstart', 'contextmenu'].forEach(event => {
    document.addEventListener(event, e => e.stopPropagation(), true);
});

// oncopy etc.
document.querySelectorAll('*').forEach(el => {
    ['oncopy', 'oncut', 'onpaste', 'onselectstart', 'oncontextmenu'].forEach(attr => {
        el.removeAttribute(attr);
    });
});

console.log('复制限制已解除');""",
                tags = listOf(AppStringsProvider.current().tagCopy, AppStringsProvider.current().tagUnlock)
            ),
            CodeSnippet(
                id = "enhance-print-friendly",
                name = AppStringsProvider.current().snippetPrintFriendly,
                description = AppStringsProvider.current().snippetPrintFriendlyDesc,
                code = """function preparePrint() {
    const style = document.createElement('style');
    style.textContent = `
        @media print {
            nav, header, footer, aside, .ad, .sidebar,
            [class*="nav"], [class*="header"], [class*="footer"],
            [class*="ad"], [class*="sidebar"], [class*="menu"] {
                display: none !important;
            }
            body {
                font-size: 12pt !important;
                line-height: 1.5 !important;
            }
            a { color: #000 !important; text-decoration: underline !important; }
            a::after { content: " (" attr(href) ")"; font-size: 10pt; }
        }
    `;
    document.head.appendChild(style);
    window.print();
}
preparePrint();""",
                tags = listOf(AppStringsProvider.current().tagPrint, AppStringsProvider.current().tagOptimize)
            ),
            CodeSnippet(
                id = "enhance-text-to-speech",
                name = AppStringsProvider.current().snippetTextToSpeech,
                description = AppStringsProvider.current().snippetTextToSpeechDesc,
                code = """function speakText(text) {
    if ('speechSynthesis' in window) {
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'zh-CN';
        utterance.rate = 1;
        speechSynthesis.speak(utterance);
    }
}

// in.
document.addEventListener('mouseup', () => {
    const selection = window.getSelection().toString().trim();
    if (selection.length > 0 && selection.length < 500) {
        // Show by.
        const btn = document.createElement('button');
        btn.innerHTML = '🔊';
        btn.style.cssText = `
            position: fixed; z-index: 999999;
            padding: 8px 12px; background: #333; color: white;
            border: none; border-radius: 20px; cursor: pointer;
        `;
        btn.onclick = () => { speakText(selection); btn.remove(); };
        document.body.appendChild(btn);
        
        const rect = window.getSelection().getRangeAt(0).getBoundingClientRect();
        btn.style.left = rect.left + 'px';
        btn.style.top = (rect.bottom + 10) + 'px';
        
        setTimeout(() => btn.remove(), 5000);
    }
});""",
                tags = listOf(AppStringsProvider.current().tagVoice, AppStringsProvider.current().tagReadAloud)
            ),
            CodeSnippet(
                id = "enhance-word-count",
                name = AppStringsProvider.current().snippetWordCount,
                description = AppStringsProvider.current().snippetWordCountDesc,
                code = """function countWords() {
    const text = document.body.innerText;
    const chinese = (text.match(/[\u4e00-\u9fa5]/g) || []).length;
    const english = (text.match(/[a-zA-Z]+/g) || []).length;
    const numbers = (text.match(/\d+/g) || []).length;
    const total = chinese + english + numbers;
    
    const result = `
        📊 字数统计
        ─────────
        中文: ${"$"}{chinese} 字
        英文: ${"$"}{english} 词
        数字: ${"$"}{numbers} 个
        总计: ${"$"}{total}
        阅读时间: 约 ${"$"}{Math.ceil(total / 300)} 分钟
    `;
    
    alert(result);
    return { chinese, english, numbers, total };
}
countWords();""",
                tags = listOf(AppStringsProvider.current().tagStats, AppStringsProvider.current().tagWordCount)
            ),
            CodeSnippet(
                id = "enhance-highlight-search",
                name = AppStringsProvider.current().snippetHighlightSearch,
                description = AppStringsProvider.current().snippetHighlightSearchDesc,
                code = """function highlightText(keyword) {
    // before.
    document.querySelectorAll('.search-highlight').forEach(el => {
        el.outerHTML = el.textContent;
    });
    
    if (!keyword) return;
    
    const regex = new RegExp('(' + keyword.replace(/[.*+?^${"$"}{}()|[\]\\]/g, '\\${"$"}&') + ')', 'gi');
    
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    const textNodes = [];
    while (walker.nextNode()) textNodes.push(walker.currentNode);
    
    textNodes.forEach(node => {
        if (regex.test(node.textContent)) {
            const span = document.createElement('span');
            span.innerHTML = node.textContent.replace(regex, 
                '<mark class="search-highlight" style="background: yellow;">${"$"}1</mark>');
            node.parentNode.replaceChild(span, node);
        }
    });
}
highlightText('搜索关键词');""",
                tags = listOf(AppStringsProvider.current().tagSearch, AppStringsProvider.current().tagHighlight)
            )
        )
)
internal fun contentFilter() = CodeSnippetCategory(
        id = "filter",
        name = AppStringsProvider.current().snippetFilter,
        icon = "🔍",
        description = AppStringsProvider.current().snippetFilterDesc,
        snippets = listOf(
            CodeSnippet(
                id = "filter-keywords",
                name = AppStringsProvider.current().snippetKeywordFilter,
                description = AppStringsProvider.current().snippetKeywordFilterDesc,
                code = """function filterByKeywords(keywords, selector = '*') {
    const keywordList = keywords.map(k => k.toLowerCase());
    
    document.querySelectorAll(selector).forEach(el => {
        const text = el.textContent.toLowerCase();
        if (keywordList.some(keyword => text.includes(keyword))) {
            el.style.display = 'none';
        }
    });
}
filterByKeywords(['广告', '推广', '赞助'], 'div, article, section');""",
                tags = listOf(AppStringsProvider.current().tagKeyword, AppStringsProvider.current().tagFilter)
            ),
            CodeSnippet(
                id = "filter-empty",
                name = AppStringsProvider.current().snippetRemoveEmpty,
                description = AppStringsProvider.current().snippetRemoveEmptyDesc,
                code = """function removeEmptyElements(selector = 'div, p, span') {
    document.querySelectorAll(selector).forEach(el => {
        if (!el.textContent.trim() && !el.querySelector('img, video, iframe')) {
            el.remove();
        }
    });
}
removeEmptyElements();""",
                tags = listOf(AppStringsProvider.current().tagEmptyElement, AppStringsProvider.current().tagClean)
            ),
            CodeSnippet(
                id = "filter-comments",
                name = AppStringsProvider.current().snippetFilterComments,
                description = AppStringsProvider.current().snippetFilterCommentsDesc,
                code = """function filterComments(options = {}) {
    const { minLength = 0, keywords = [], selector = '[class*="comment"]' } = options;
    
    document.querySelectorAll(selector).forEach(comment => {
        const text = comment.textContent;
        
        // Filter.
        if (text.length < minLength) {
            comment.style.opacity = '0.3';
        }
        
        // Filter.
        if (keywords.some(k => text.toLowerCase().includes(k.toLowerCase()))) {
            comment.style.display = 'none';
        }
    });
}
filterComments({ minLength: 10, keywords: ['广告', '推广'] });""",
                tags = listOf(AppStringsProvider.current().tagComment, AppStringsProvider.current().tagFilter)
            ),
            CodeSnippet(
                id = "filter-images-size",
                name = AppStringsProvider.current().snippetFilterSmallImages,
                description = AppStringsProvider.current().snippetFilterSmallImagesDesc,
                code = """function filterSmallImages(minWidth = 100, minHeight = 100) {
    document.querySelectorAll('img').forEach(img => {
        if (img.complete) {
            if (img.naturalWidth < minWidth || img.naturalHeight < minHeight) {
                img.style.display = 'none';
            }
        } else {
            img.onload = () => {
                if (img.naturalWidth < minWidth || img.naturalHeight < minHeight) {
                    img.style.display = 'none';
                }
            };
        }
    });
}
filterSmallImages(100, 100);""",
                tags = listOf(AppStringsProvider.current().tagImage, AppStringsProvider.current().tagFilter)
            )
        )
)
internal fun adBlocker() = CodeSnippetCategory(
        id = "adblocker",
        name = AppStringsProvider.current().snippetAdBlock,
        icon = "🛡️",
        description = AppStringsProvider.current().snippetAdBlockDesc,
        snippets = listOf(
            CodeSnippet(
                id = "ad-hide-common",
                name = AppStringsProvider.current().snippetHideAds,
                description = AppStringsProvider.current().snippetHideAdsDesc,
                code = """const adSelectors = [
    '[class*="ad-"]', '[class*="ads-"]', '[class*="advert"]',
    '[id*="ad-"]', '[id*="ads-"]', '[id*="advert"]',
    '[class*="banner"]', '[class*="popup"]', '[class*="modal"]',
    'ins.adsbygoogle', '.adsbygoogle', '[data-ad]',
    'iframe[src*="ad"]', 'iframe[src*="banner"]',
    '[class*="sponsor"]', '[class*="promo"]'
];

function hideAds() {
    adSelectors.forEach(selector => {
        try {
            document.querySelectorAll(selector).forEach(el => {
                el.style.setProperty('display', 'none', 'important');
            });
        } catch(e) { /* selector failed */ }
    });
}

hideAds();
const observer = new MutationObserver(hideAds);
observer.observe(document.body, { childList: true, subtree: true });""",
                tags = listOf(AppStringsProvider.current().tagAd, AppStringsProvider.current().tagHide)
            ),
            CodeSnippet(
                id = "ad-block-popup",
                name = AppStringsProvider.current().snippetBlockPopup,
                description = AppStringsProvider.current().snippetBlockPopupDesc,
                code = """// 阻止 window.open
const originalOpen = window.open;
window.open = function(url, name, features) {
    console.log('[AdBlocker] 阻止弹窗:', url);
    return null;
};

// alert/confirm/prompt.
// window.alert = () => {};
// window.confirm = () => false;
// window.prompt = () => null;

function removePopups() {
    document.querySelectorAll('[class*="popup"], [class*="modal"], [class*="overlay"]').forEach(el => {
        if (el.style.position === 'fixed' || el.style.position === 'absolute') {
            el.remove();
        }
    });
}
setInterval(removePopups, 1000);""",
                tags = listOf(AppStringsProvider.current().tagPopup, AppStringsProvider.current().tagPrevent)
            ),
            CodeSnippet(
                id = "ad-remove-overlay",
                name = AppStringsProvider.current().snippetRemoveOverlay,
                description = AppStringsProvider.current().snippetRemoveOverlayDesc,
                code = """function removeOverlays() {
    document.querySelectorAll('*').forEach(el => {
        const style = getComputedStyle(el);
        if (style.position === 'fixed' && 
            (style.zIndex > 1000 || el.style.zIndex > 1000)) {
            const rect = el.getBoundingClientRect();
            // large.
            if (rect.width > window.innerWidth * 0.5 && 
                rect.height > window.innerHeight * 0.5) {
                el.remove();
            }
        }
    });
    
    // restore.
    document.body.style.overflow = 'auto';
    document.documentElement.style.overflow = 'auto';
}
removeOverlays();""",
                tags = listOf(AppStringsProvider.current().tagMask, AppStringsProvider.current().tagRemove)
            ),
            CodeSnippet(
                id = "ad-css-blocker",
                name = AppStringsProvider.current().snippetCssAdBlock,
                description = AppStringsProvider.current().snippetCssAdBlockDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    /* */
    [class*="ad-"], [class*="ads-"], [class*="advert"],
    [id*="ad-"], [id*="ads-"], [id*="advert"],
    [class*="banner"], [class*="popup"],
    ins.adsbygoogle, .adsbygoogle,
    [data-ad], [data-ads], [data-advertisement],
    iframe[src*="ad"], iframe[src*="banner"],
    [class*="sponsor"], [class*="promo"] {
        display: none !important;
        visibility: hidden !important;
        height: 0 !important;
        width: 0 !important;
        overflow: hidden !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagCSS, AppStringsProvider.current().tagAd)
            ),
            CodeSnippet(
                id = "ad-anti-adblock",
                name = AppStringsProvider.current().snippetAntiAdblock,
                description = AppStringsProvider.current().snippetAntiAdblockDesc,
                code = """// 伪装广告元素存在
const fakeAd = document.createElement('div');
fakeAd.className = 'ad ads adsbox ad-placeholder';
fakeAd.style.cssText = 'height: 1px; width: 1px; position: absolute; left: -9999px;';
document.body.appendChild(fakeAd);

Object.defineProperty(window, 'adBlockDetected', { value: false, writable: false });

// intercept.
const observer = new MutationObserver(() => {
    document.querySelectorAll('[class*="adblock"], [id*="adblock"]').forEach(el => {
        el.remove();
    });
});
observer.observe(document.body, { childList: true, subtree: true });""",
                tags = listOf(AppStringsProvider.current().tagAntiDetect, AppStringsProvider.current().tagAd)
            )
        )
)
internal fun interceptors() = CodeSnippetCategory(
        id = "intercept",
        name = AppStringsProvider.current().snippetIntercept,
        icon = "🔀",
        description = AppStringsProvider.current().snippetInterceptDesc,
        snippets = listOf(
            CodeSnippet(
                id = "intercept-fetch",
                name = AppStringsProvider.current().snippetInterceptFetch,
                description = AppStringsProvider.current().snippetInterceptFetchDesc,
                code = """const originalFetch = window.fetch;
window.fetch = async function(url, options = {}) {
    console.log('[Fetch]', url);
    
    // can request.
    // if (url.includes('ad')) return new Response('{}');
    
    const response = await originalFetch.call(this, url, options);
    
    // can.
    console.log('[Fetch Response]', response.status);
    
    return response;
};""",
                tags = listOf(AppStringsProvider.current().tagIntercept, AppStringsProvider.current().tagFetch)
            ),
            CodeSnippet(
                id = "intercept-xhr",
                name = AppStringsProvider.current().snippetInterceptXhr,
                description = AppStringsProvider.current().snippetInterceptXhrDesc,
                code = """const originalOpen = XMLHttpRequest.prototype.open;
const originalSend = XMLHttpRequest.prototype.send;

XMLHttpRequest.prototype.open = function(method, url, ...args) {
    this._url = url;
    this._method = method;
    console.log('[XHR Open]', method, url);
    return originalOpen.call(this, method, url, ...args);
};

XMLHttpRequest.prototype.send = function(body) {
    this.addEventListener('load', function() {
        console.log('[XHR Response]', this._url, this.status);
    });
    return originalSend.call(this, body);
};""",
                tags = listOf(AppStringsProvider.current().tagIntercept, AppStringsProvider.current().tagXHR)
            ),
            CodeSnippet(
                id = "intercept-websocket",
                name = AppStringsProvider.current().snippetInterceptWebSocket,
                description = AppStringsProvider.current().snippetInterceptWebSocketDesc,
                code = """const OriginalWebSocket = window.WebSocket;
window.WebSocket = function(url, protocols) {
    console.log('[WebSocket]', url);
    
    const ws = new OriginalWebSocket(url, protocols);
    
    const originalSend = ws.send.bind(ws);
    ws.send = function(data) {
        console.log('[WS Send]', data);
        return originalSend(data);
    };
    
    ws.addEventListener('message', (e) => {
        console.log('[WS Receive]', e.data);
    });
    
    return ws;
};""",
                tags = listOf(AppStringsProvider.current().tagIntercept, AppStringsProvider.current().tagWebSocket)
            ),
            CodeSnippet(
                id = "intercept-block-requests",
                name = AppStringsProvider.current().snippetBlockRequests,
                description = AppStringsProvider.current().snippetBlockRequestsDesc,
                code = """const blockedKeywords = ['ad', 'analytics', 'tracking', 'beacon'];

// intercept Fetch.
const originalFetch = window.fetch;
window.fetch = function(url, options) {
    if (blockedKeywords.some(k => url.toLowerCase().includes(k))) {
        console.log('[Blocked Fetch]', url);
        return Promise.resolve(new Response('{}'));
    }
    return originalFetch.call(this, url, options);
};

// intercept XHR.
const originalOpen = XMLHttpRequest.prototype.open;
XMLHttpRequest.prototype.open = function(method, url, ...args) {
    this._blocked = blockedKeywords.some(k => url.toLowerCase().includes(k));
    if (this._blocked) console.log('[Blocked XHR]', url);
    return originalOpen.call(this, method, url, ...args);
};

const originalSend = XMLHttpRequest.prototype.send;
XMLHttpRequest.prototype.send = function(body) {
    if (this._blocked) return;
    return originalSend.call(this, body);
};""",
                tags = listOf(AppStringsProvider.current().tagPrevent, AppStringsProvider.current().tagRequest)
            )
        )
)
