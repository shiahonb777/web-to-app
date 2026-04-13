package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.Strings

internal fun pageEnhance() = CodeSnippetCategory(
        id = "enhance",
        name = Strings.snippetEnhance,
        icon = "✨",
        description = Strings.snippetEnhanceDesc,
        snippets = listOf(
            CodeSnippet(
                id = "enhance-reading-mode",
                name = Strings.snippetReadingMode,
                description = Strings.snippetReadingModeDesc,
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
                tags = listOf(Strings.tagReading, Strings.tagSimplify)
            ),
            CodeSnippet(
                id = "enhance-copy-unlock",
                name = Strings.snippetCopyUnlock,
                description = Strings.snippetCopyUnlockDesc,
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

// 移除事件监听
['copy', 'cut', 'paste', 'selectstart', 'contextmenu'].forEach(event => {
    document.addEventListener(event, e => e.stopPropagation(), true);
});

// 移除 oncopy 等属性
document.querySelectorAll('*').forEach(el => {
    ['oncopy', 'oncut', 'onpaste', 'onselectstart', 'oncontextmenu'].forEach(attr => {
        el.removeAttribute(attr);
    });
});

console.log('复制限制已解除');""",
                tags = listOf(Strings.tagCopy, Strings.tagUnlock)
            ),
            CodeSnippet(
                id = "enhance-print-friendly",
                name = Strings.snippetPrintFriendly,
                description = Strings.snippetPrintFriendlyDesc,
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
                tags = listOf(Strings.tagPrint, Strings.tagOptimize)
            ),
            CodeSnippet(
                id = "enhance-text-to-speech",
                name = Strings.snippetTextToSpeech,
                description = Strings.snippetTextToSpeechDesc,
                code = """function speakText(text) {
    if ('speechSynthesis' in window) {
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'zh-CN';
        utterance.rate = 1;
        speechSynthesis.speak(utterance);
    }
}

// 朗读选中文字
document.addEventListener('mouseup', () => {
    const selection = window.getSelection().toString().trim();
    if (selection.length > 0 && selection.length < 500) {
        // Show朗读按钮
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
                tags = listOf(Strings.tagVoice, Strings.tagReadAloud)
            ),
            CodeSnippet(
                id = "enhance-word-count",
                name = Strings.snippetWordCount,
                description = Strings.snippetWordCountDesc,
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
                tags = listOf(Strings.tagStats, Strings.tagWordCount)
            ),
            CodeSnippet(
                id = "enhance-highlight-search",
                name = Strings.snippetHighlightSearch,
                description = Strings.snippetHighlightSearchDesc,
                code = """function highlightText(keyword) {
    // 清除之前的高亮
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
                tags = listOf(Strings.tagSearch, Strings.tagHighlight)
            )
        )
)
internal fun contentFilter() = CodeSnippetCategory(
        id = "filter",
        name = Strings.snippetFilter,
        icon = "🔍",
        description = Strings.snippetFilterDesc,
        snippets = listOf(
            CodeSnippet(
                id = "filter-keywords",
                name = Strings.snippetKeywordFilter,
                description = Strings.snippetKeywordFilterDesc,
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
                tags = listOf(Strings.tagKeyword, Strings.tagFilter)
            ),
            CodeSnippet(
                id = "filter-empty",
                name = Strings.snippetRemoveEmpty,
                description = Strings.snippetRemoveEmptyDesc,
                code = """function removeEmptyElements(selector = 'div, p, span') {
    document.querySelectorAll(selector).forEach(el => {
        if (!el.textContent.trim() && !el.querySelector('img, video, iframe')) {
            el.remove();
        }
    });
}
removeEmptyElements();""",
                tags = listOf(Strings.tagEmptyElement, Strings.tagClean)
            ),
            CodeSnippet(
                id = "filter-comments",
                name = Strings.snippetFilterComments,
                description = Strings.snippetFilterCommentsDesc,
                code = """function filterComments(options = {}) {
    const { minLength = 0, keywords = [], selector = '[class*="comment"]' } = options;
    
    document.querySelectorAll(selector).forEach(comment => {
        const text = comment.textContent;
        
        // Filter短评论
        if (text.length < minLength) {
            comment.style.opacity = '0.3';
        }
        
        // Filter包含关键词的评论
        if (keywords.some(k => text.toLowerCase().includes(k.toLowerCase()))) {
            comment.style.display = 'none';
        }
    });
}
filterComments({ minLength: 10, keywords: ['广告', '推广'] });""",
                tags = listOf(Strings.tagComment, Strings.tagFilter)
            ),
            CodeSnippet(
                id = "filter-images-size",
                name = Strings.snippetFilterSmallImages,
                description = Strings.snippetFilterSmallImagesDesc,
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
                tags = listOf(Strings.tagImage, Strings.tagFilter)
            )
        )
)
internal fun adBlocker() = CodeSnippetCategory(
        id = "adblocker",
        name = Strings.snippetAdBlock,
        icon = "🛡️",
        description = Strings.snippetAdBlockDesc,
        snippets = listOf(
            CodeSnippet(
                id = "ad-hide-common",
                name = Strings.snippetHideAds,
                description = Strings.snippetHideAdsDesc,
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
                tags = listOf(Strings.tagAd, Strings.tagHide)
            ),
            CodeSnippet(
                id = "ad-block-popup",
                name = Strings.snippetBlockPopup,
                description = Strings.snippetBlockPopupDesc,
                code = """// 阻止 window.open
const originalOpen = window.open;
window.open = function(url, name, features) {
    console.log('[AdBlocker] 阻止弹窗:', url);
    return null;
};

// 阻止 alert/confirm/prompt
// window.alert = () => {};
// window.confirm = () => false;
// window.prompt = () => null;

// 移除弹窗元素
function removePopups() {
    document.querySelectorAll('[class*="popup"], [class*="modal"], [class*="overlay"]').forEach(el => {
        if (el.style.position === 'fixed' || el.style.position === 'absolute') {
            el.remove();
        }
    });
}
setInterval(removePopups, 1000);""",
                tags = listOf(Strings.tagPopup, Strings.tagPrevent)
            ),
            CodeSnippet(
                id = "ad-remove-overlay",
                name = Strings.snippetRemoveOverlay,
                description = Strings.snippetRemoveOverlayDesc,
                code = """function removeOverlays() {
    // 移除固定定位的遮罩
    document.querySelectorAll('*').forEach(el => {
        const style = getComputedStyle(el);
        if (style.position === 'fixed' && 
            (style.zIndex > 1000 || el.style.zIndex > 1000)) {
            const rect = el.getBoundingClientRect();
            // 如果覆盖大部分屏幕
            if (rect.width > window.innerWidth * 0.5 && 
                rect.height > window.innerHeight * 0.5) {
                el.remove();
            }
        }
    });
    
    // 恢复滚动
    document.body.style.overflow = 'auto';
    document.documentElement.style.overflow = 'auto';
}
removeOverlays();""",
                tags = listOf(Strings.tagMask, Strings.tagRemove)
            ),
            CodeSnippet(
                id = "ad-css-blocker",
                name = Strings.snippetCssAdBlock,
                description = Strings.snippetCssAdBlockDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    /* 常见广告选择器 */
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
                tags = listOf(Strings.tagCSS, Strings.tagAd)
            ),
            CodeSnippet(
                id = "ad-anti-adblock",
                name = Strings.snippetAntiAdblock,
                description = Strings.snippetAntiAdblockDesc,
                code = """// 伪装广告元素存在
const fakeAd = document.createElement('div');
fakeAd.className = 'ad ads adsbox ad-placeholder';
fakeAd.style.cssText = 'height: 1px; width: 1px; position: absolute; left: -9999px;';
document.body.appendChild(fakeAd);

// 覆盖检测函数
Object.defineProperty(window, 'adBlockDetected', { value: false, writable: false });

// 移除反广告拦截提示
const observer = new MutationObserver(() => {
    document.querySelectorAll('[class*="adblock"], [id*="adblock"]').forEach(el => {
        el.remove();
    });
});
observer.observe(document.body, { childList: true, subtree: true });""",
                tags = listOf(Strings.tagAntiDetect, Strings.tagAd)
            )
        )
)
internal fun interceptors() = CodeSnippetCategory(
        id = "intercept",
        name = Strings.snippetIntercept,
        icon = "🔀",
        description = Strings.snippetInterceptDesc,
        snippets = listOf(
            CodeSnippet(
                id = "intercept-fetch",
                name = Strings.snippetInterceptFetch,
                description = Strings.snippetInterceptFetchDesc,
                code = """const originalFetch = window.fetch;
window.fetch = async function(url, options = {}) {
    console.log('[Fetch]', url);
    
    // 可以修改请求
    // if (url.includes('ad')) return new Response('{}');
    
    const response = await originalFetch.call(this, url, options);
    
    // 可以处理响应
    console.log('[Fetch Response]', response.status);
    
    return response;
};""",
                tags = listOf(Strings.tagIntercept, Strings.tagFetch)
            ),
            CodeSnippet(
                id = "intercept-xhr",
                name = Strings.snippetInterceptXhr,
                description = Strings.snippetInterceptXhrDesc,
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
                tags = listOf(Strings.tagIntercept, Strings.tagXHR)
            ),
            CodeSnippet(
                id = "intercept-websocket",
                name = Strings.snippetInterceptWebSocket,
                description = Strings.snippetInterceptWebSocketDesc,
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
                tags = listOf(Strings.tagIntercept, Strings.tagWebSocket)
            ),
            CodeSnippet(
                id = "intercept-block-requests",
                name = Strings.snippetBlockRequests,
                description = Strings.snippetBlockRequestsDesc,
                code = """const blockedKeywords = ['ad', 'analytics', 'tracking', 'beacon'];

// 拦截 Fetch
const originalFetch = window.fetch;
window.fetch = function(url, options) {
    if (blockedKeywords.some(k => url.toLowerCase().includes(k))) {
        console.log('[Blocked Fetch]', url);
        return Promise.resolve(new Response('{}'));
    }
    return originalFetch.call(this, url, options);
};

// 拦截 XHR
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
                tags = listOf(Strings.tagPrevent, Strings.tagRequest)
            )
        )
)
