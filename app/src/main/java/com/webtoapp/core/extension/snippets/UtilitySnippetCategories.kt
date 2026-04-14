package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.Strings

internal fun utilityFunctions() = CodeSnippetCategory(
        id = "utility",
        name = Strings.snippetUtility,
        icon = "🔨",
        description = Strings.snippetUtilityDesc,
        snippets = listOf(
            CodeSnippet(
                id = "util-debounce",
                name = Strings.snippetDebounce,
                description = Strings.snippetDebounceDesc,
                code = """function debounce(func, wait = 300) {
    let timeout;
    return function(...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}
// use.
const debouncedSearch = debounce((query) => {
    console.log('搜索:', query);
}, 500);""",
                tags = listOf(Strings.tagDebounce, Strings.tagPerformance)
            ),
            CodeSnippet(
                id = "util-throttle",
                name = Strings.snippetThrottle,
                description = Strings.snippetThrottleDesc,
                code = """function throttle(func, limit = 300) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}
// use.
const throttledScroll = throttle(() => {
    console.log('滚动位置:', window.scrollY);
}, 100);""",
                tags = listOf(Strings.tagThrottle, Strings.tagPerformance)
            ),
            CodeSnippet(
                id = "util-wait-element",
                name = Strings.snippetWaitElement,
                description = Strings.snippetWaitElementDesc,
                code = """function waitForElement(selector, timeout = 10000) {
    return new Promise((resolve, reject) => {
        const element = document.querySelector(selector);
        if (element) {
            resolve(element);
            return;
        }
        
        const observer = new MutationObserver(() => {
            const el = document.querySelector(selector);
            if (el) {
                observer.disconnect();
                resolve(el);
            }
        });
        
        observer.observe(document.body, { childList: true, subtree: true });
        
        setTimeout(() => {
            observer.disconnect();
            reject(new Error('元素未找到: ' + selector));
        }, timeout);
    });
}
waitForElement('.target-class').then(el => console.log('找到元素:', el));""",
                tags = listOf(Strings.tagWait, Strings.tagAsync)
            ),
            CodeSnippet(
                id = "util-copy-text",
                name = Strings.snippetCopyText,
                description = Strings.snippetCopyTextDesc,
                code = """async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        console.log('复制成功');
        return true;
    } catch (err) {
        // level Approach.
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.cssText = 'position: fixed; opacity: 0;';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        textarea.remove();
        return true;
    }
}
copyToClipboard('要复制的文本');""",
                tags = listOf(Strings.tagCopy, Strings.tagClipboard)
            ),
            CodeSnippet(
                id = "util-format-date",
                name = Strings.snippetFormatDate,
                description = Strings.snippetFormatDateDesc,
                code = """function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
    const d = new Date(date);
    const map = {
        'YYYY': d.getFullYear(),
        'MM': String(d.getMonth() + 1).padStart(2, '0'),
        'DD': String(d.getDate()).padStart(2, '0'),
        'HH': String(d.getHours()).padStart(2, '0'),
        'mm': String(d.getMinutes()).padStart(2, '0'),
        'ss': String(d.getSeconds()).padStart(2, '0')
    };
    return format.replace(/YYYY|MM|DD|HH|mm|ss/g, match => map[match]);
}
console.log(formatDate(new Date())); // 2024-01-01 12:00:00""",
                tags = listOf(Strings.tagDate, Strings.tagFormat)
            ),
            CodeSnippet(
                id = "util-random-string",
                name = Strings.snippetRandomString,
                description = Strings.snippetRandomStringDesc,
                code = """function randomString(length = 8) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}
console.log(randomString(16));""",
                tags = listOf(Strings.tagRandom, Strings.tagString)
            ),
            CodeSnippet(
                id = "util-sleep",
                name = Strings.snippetSleep,
                description = Strings.snippetSleepDesc,
                code = """function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// use.
async function example() {
    console.log('开始');
    await sleep(2000);
    console.log('2秒后');
}""",
                tags = listOf(Strings.tagDelay, Strings.tagAsync)
            ),
            CodeSnippet(
                id = "util-retry",
                name = Strings.snippetRetry,
                description = Strings.snippetRetryDesc,
                code = """async function retry(fn, retries = 3, delay = 1000) {
    for (let i = 0; i < retries; i++) {
        try {
            return await fn();
        } catch (error) {
            if (i === retries - 1) throw error;
            console.log('重试 ' + (i + 1) + '/' + retries);
            await new Promise(r => setTimeout(r, delay));
        }
    }
}

// use.
retry(() => fetch('/api/data').then(r => r.json()), 3, 1000);""",
                tags = listOf(Strings.tagRetry, Strings.tagErrorHandle)
            )
        )
)
internal fun automation() = CodeSnippetCategory(
        id = "automation",
        name = Strings.snippetAutomation,
        icon = "🤖",
        description = Strings.snippetAutomationDesc,
        snippets = listOf(
            CodeSnippet(
                id = "auto-click",
                name = Strings.snippetAutoClick,
                description = Strings.snippetAutoClickDesc,
                code = """function autoClick(selector, delay = 1000) {
    setTimeout(() => {
        const element = document.querySelector(selector);
        if (element) {
            element.click();
            console.log('[AutoClick]', selector);
        }
    }, delay);
}
autoClick('.close-btn', 2000);""",
                tags = listOf(Strings.tagAuto, Strings.tagClick)
            ),
            CodeSnippet(
                id = "auto-click-interval",
                name = Strings.snippetAutoClickInterval,
                description = Strings.snippetAutoClickIntervalDesc,
                code = """function autoClickInterval(selector, interval = 5000) {
    const click = () => {
        const element = document.querySelector(selector);
        if (element) {
            element.click();
            console.log('[AutoClick]', new Date().toLocaleTimeString());
        }
    };
    
    click(); // Note.
    return setInterval(click, interval);
}

// 5.
const timer = autoClickInterval('.refresh-btn', 5000);
// Stop: clearInterval(timer);""",
                tags = listOf(Strings.tagTimer, Strings.tagClick)
            ),
            CodeSnippet(
                id = "auto-fill-form",
                name = Strings.snippetAutoFillSubmit,
                description = Strings.snippetAutoFillSubmitDesc,
                code = """async function autoFillAndSubmit(formData, submitSelector) {
    // single.
    for (const [name, value] of Object.entries(formData)) {
        const input = document.querySelector(`[name="${"$"}{name}"], #${"$"}{name}`);
        if (input) {
            input.value = value;
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
            await new Promise(r => setTimeout(r, 100));
        }
    }
    
    if (submitSelector) {
        await new Promise(r => setTimeout(r, 500));
        document.querySelector(submitSelector)?.click();
    }
}

autoFillAndSubmit({
    username: 'user',
    password: 'pass'
}, 'button[type="submit"]');""",
                tags = listOf(Strings.tagForm, Strings.tagAuto)
            ),
            CodeSnippet(
                id = "auto-refresh",
                name = Strings.snippetAutoRefresh,
                description = Strings.snippetAutoRefreshDesc,
                code = """function autoRefresh(seconds = 60) {
    let countdown = seconds;
    
    const display = document.createElement('div');
    display.style.cssText = `
        position: fixed; top: 10px; right: 10px; z-index: 999999;
        background: rgba(0,0,0,0.7); color: white;
        padding: 8px 12px; border-radius: 20px; font-size: 12px;
    `;
    document.body.appendChild(display);
    
    const timer = setInterval(() => {
        countdown--;
        display.textContent = '🔄 ' + countdown + 's';
        if (countdown <= 0) {
            location.reload();
        }
    }, 1000);
    
    display.onclick = () => {
        clearInterval(timer);
        display.remove();
    };
    display.title = '点击取消';
    
    return timer;
}
autoRefresh(60);""",
                tags = listOf(Strings.tagRefresh, Strings.tagTimer)
            ),
            CodeSnippet(
                id = "auto-scroll-load",
                name = Strings.snippetAutoScrollLoad,
                description = Strings.snippetAutoScrollLoadDesc,
                code = """async function autoScrollLoad(maxScrolls = 10, delay = 2000) {
    let scrollCount = 0;
    
    while (scrollCount < maxScrolls) {
        const prevHeight = document.documentElement.scrollHeight;
        
        window.scrollTo(0, document.documentElement.scrollHeight);
        await new Promise(r => setTimeout(r, delay));
        
        const newHeight = document.documentElement.scrollHeight;
        if (newHeight === prevHeight) {
            console.log('已到达底部');
            break;
        }
        
        scrollCount++;
        console.log('已滚动 ' + scrollCount + ' 次');
    }
    
    window.scrollTo(0, 0);
    console.log('加载完成');
}
autoScrollLoad(10, 2000);""",
                tags = listOf(Strings.tagScroll, Strings.tagLoading)
            ),
            CodeSnippet(
                id = "auto-login-check",
                name = Strings.snippetAutoLoginCheck,
                description = Strings.snippetAutoLoginCheckDesc,
                code = """function checkLoginStatus(loggedInSelector, loginUrl) {
    const isLoggedIn = !!document.querySelector(loggedInSelector);
    
    if (!isLoggedIn) {
        const shouldLogin = confirm('检测到未登录，是否跳转到登录页面？');
        if (shouldLogin) {
            location.href = loginUrl;
        }
    }
    
    return isLoggedIn;
}
checkLoginStatus('.user-avatar', '/login');""",
                tags = listOf(Strings.tagLogin, Strings.tagDetect)
            )
        )
)
internal fun debugging() = CodeSnippetCategory(
        id = "debug",
        name = Strings.snippetDebug,
        icon = "🐛",
        description = Strings.snippetDebugDesc,
        snippets = listOf(
            CodeSnippet(
                id = "debug-console-panel",
                name = Strings.snippetConsolePanel,
                description = Strings.snippetConsolePanelDesc,
                code = """const panel = document.createElement('div');
panel.style.cssText = `
    position: fixed; bottom: 0; left: 0; right: 0; height: 200px;
    background: #1e1e1e; color: #fff; font-family: monospace;
    font-size: 12px; overflow-y: auto; z-index: 999999;
    padding: 10px; border-top: 2px solid #007acc;
`;
panel.innerHTML = '<div style="color: #888;">📋 Console Panel</div>';
document.body.appendChild(panel);

const originalLog = console.log;
console.log = function(...args) {
    originalLog.apply(console, args);
    const line = document.createElement('div');
    line.textContent = args.map(a => typeof a === 'object' ? JSON.stringify(a) : a).join(' ');
    panel.appendChild(line);
    panel.scrollTop = panel.scrollHeight;
};""",
                tags = listOf(Strings.tagConsole, Strings.tagLog)
            ),
            CodeSnippet(
                id = "debug-element-info",
                name = Strings.snippetElementInfo,
                description = Strings.snippetElementInfoDesc,
                code = """let inspecting = false;
const overlay = document.createElement('div');
overlay.style.cssText = `
    position: fixed; pointer-events: none; z-index: 999999;
    border: 2px solid #007acc; background: rgba(0, 122, 204, 0.1);
`;

document.addEventListener('mousemove', (e) => {
    if (!inspecting) return;
    const el = document.elementFromPoint(e.clientX, e.clientY);
    if (el && el !== overlay) {
        const rect = el.getBoundingClientRect();
        overlay.style.cssText += `
            left: ${"$"}{rect.left}px; top: ${"$"}{rect.top}px;
            width: ${"$"}{rect.width}px; height: ${"$"}{rect.height}px;
            display: block;
        `;
    }
});

document.addEventListener('click', (e) => {
    if (!inspecting) return;
    e.preventDefault();
    e.stopPropagation();
    const el = document.elementFromPoint(e.clientX, e.clientY);
    console.log('Element:', el);
    console.log('Tag:', el.tagName);
    console.log('ID:', el.id);
    console.log('Class:', el.className);
    console.log('Selector:', getSelector(el));
}, true);

function getSelector(el) {
    if (el.id) return '#' + el.id;
    if (el.className) return '.' + el.className.split(' ').join('.');
    return el.tagName.toLowerCase();
}

// by Ctrl+Shift+I.
document.addEventListener('keydown', (e) => {
    if (e.ctrlKey && e.shiftKey && e.key === 'I') {
        inspecting = !inspecting;
        document.body.appendChild(overlay);
        console.log('Inspector:', inspecting ? 'ON' : 'OFF');
    }
});""",
                tags = listOf(Strings.tagInspect, Strings.tagElement)
            ),
            CodeSnippet(
                id = "debug-performance",
                name = Strings.snippetPerformance,
                description = Strings.snippetPerformanceDesc,
                code = """function showPerformance() {
    const perf = performance.timing;
    const loadTime = perf.loadEventEnd - perf.navigationStart;
    const domReady = perf.domContentLoadedEventEnd - perf.navigationStart;
    const firstPaint = performance.getEntriesByType('paint')[0]?.startTime || 0;
    
    const info = `
        📊 性能信息
        ─────────────
        页面加载: ${"$"}{loadTime}ms
        DOM 就绪: ${"$"}{domReady}ms
        首次绘制: ${"$"}{Math.round(firstPaint)}ms
        资源数量: ${"$"}{performance.getEntriesByType('resource').length}
        内存使用: ${"$"}{Math.round((performance.memory?.usedJSHeapSize || 0) / 1024 / 1024)}MB
    `;
    
    console.log(info);
    alert(info);
}
showPerformance();""",
                tags = listOf(Strings.tagPerformance, Strings.tagMonitor)
            ),
            CodeSnippet(
                id = "debug-network-log",
                name = Strings.snippetNetworkLog,
                description = Strings.snippetNetworkLogDesc,
                code = """const networkLog = [];

// intercept Fetch.
const originalFetch = window.fetch;
window.fetch = async function(url, options = {}) {
    const start = Date.now();
    const response = await originalFetch.call(this, url, options);
    networkLog.push({
        type: 'fetch',
        url,
        method: options.method || 'GET',
        status: response.status,
        time: Date.now() - start
    });
    return response;
};

// intercept XHR.
const originalOpen = XMLHttpRequest.prototype.open;
XMLHttpRequest.prototype.open = function(method, url) {
    this._logData = { type: 'xhr', url, method, start: Date.now() };
    return originalOpen.apply(this, arguments);
};

const originalSend = XMLHttpRequest.prototype.send;
XMLHttpRequest.prototype.send = function() {
    this.addEventListener('load', () => {
        this._logData.status = this.status;
        this._logData.time = Date.now() - this._logData.start;
        networkLog.push(this._logData);
    });
    return originalSend.apply(this, arguments);
};

window.showNetworkLog = () => console.table(networkLog);""",
                tags = listOf(Strings.tagNetwork, Strings.tagLog)
            )
        )
)