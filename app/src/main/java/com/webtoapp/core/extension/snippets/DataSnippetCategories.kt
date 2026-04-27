package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.AppStringsProvider

internal fun storageOperations() = CodeSnippetCategory(
        id = "storage",
        name = AppStringsProvider.current().snippetStorage,
        icon = "💾",
        description = AppStringsProvider.current().snippetStorageDesc,
        snippets = listOf(
            CodeSnippet(
                id = "storage-local-set",
                name = AppStringsProvider.current().snippetLocalSet,
                description = AppStringsProvider.current().snippetLocalSetDesc,
                code = """function saveData(key, value) {
    try {
        localStorage.setItem(key, JSON.stringify(value));
        return true;
    } catch (e) {
        console.error('保存失败:', e);
        return false;
    }
}
saveData('myKey', { name: 'value' });""",
                tags = listOf(AppStringsProvider.current().tagStorage, AppStringsProvider.current().tagSave)
            ),
            CodeSnippet(
                id = "storage-local-get",
                name = AppStringsProvider.current().snippetLocalGet,
                description = AppStringsProvider.current().snippetLocalGetDesc,
                code = """function loadData(key, defaultValue = null) {
    try {
        const data = localStorage.getItem(key);
        return data ? JSON.parse(data) : defaultValue;
    } catch (e) {
        console.error('读取失败:', e);
        return defaultValue;
    }
}
const data = loadData('myKey', {});""",
                tags = listOf(AppStringsProvider.current().tagStorage, AppStringsProvider.current().tagRead)
            ),
            CodeSnippet(
                id = "storage-session",
                name = AppStringsProvider.current().snippetSessionStorage,
                description = AppStringsProvider.current().snippetSessionStorageDesc,
                code = """// Save（页面关闭后清除）
sessionStorage.setItem('key', 'value');
const value = sessionStorage.getItem('key');
// Delete
sessionStorage.removeItem('key');
sessionStorage.clear();""",
                tags = listOf(AppStringsProvider.current().tagSession, AppStringsProvider.current().tagTemporary)
            ),
            CodeSnippet(
                id = "storage-cookie-set",
                name = AppStringsProvider.current().snippetSetCookie,
                description = AppStringsProvider.current().snippetSetCookieDesc,
                code = """function setCookie(name, value, days = 7) {
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    document.cookie = name + '=' + encodeURIComponent(value) + 
        '; expires=' + expires + '; path=/';
}
setCookie('myCookie', 'value', 30);""",
                tags = listOf(AppStringsProvider.current().tagCookie, AppStringsProvider.current().tagSetting)
            ),
            CodeSnippet(
                id = "storage-cookie-get",
                name = AppStringsProvider.current().snippetGetCookie,
                description = AppStringsProvider.current().snippetGetCookieDesc,
                code = """function getCookie(name) {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [key, value] = cookie.trim().split('=');
        if (key === name) {
            return decodeURIComponent(value);
        }
    }
    return null;
}
const value = getCookie('myCookie');""",
                tags = listOf(AppStringsProvider.current().tagCookie, AppStringsProvider.current().tagRead)
            ),
            CodeSnippet(
                id = "storage-cookie-delete",
                name = AppStringsProvider.current().snippetDeleteCookie,
                description = AppStringsProvider.current().snippetDeleteCookieDesc,
                code = """function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
}
deleteCookie('myCookie');""",
                tags = listOf(AppStringsProvider.current().tagCookie, AppStringsProvider.current().tagDelete)
            ),
            CodeSnippet(
                id = "storage-indexeddb",
                name = AppStringsProvider.current().snippetIndexedDB,
                description = AppStringsProvider.current().snippetIndexedDBDesc,
                code = """const dbName = 'MyDatabase';
const storeName = 'MyStore';

function openDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(dbName, 1);
        request.onerror = () => reject(request.error);
        request.onsuccess = () => resolve(request.result);
        request.onupgradeneeded = (e) => {
            const db = e.target.result;
            if (!db.objectStoreNames.contains(storeName)) {
                db.createObjectStore(storeName, { keyPath: 'id' });
            }
        };
    });
}

async function saveToIDB(data) {
    const db = await openDB();
    const tx = db.transaction(storeName, 'readwrite');
    tx.objectStore(storeName).put(data);
}""",
                tags = listOf(AppStringsProvider.current().tagIndexedDB, AppStringsProvider.current().tagBigData)
            )
        )
)
internal fun networkOperations() = CodeSnippetCategory(
        id = "network",
        name = AppStringsProvider.current().snippetNetwork,
        icon = "🌐",
        description = AppStringsProvider.current().snippetNetworkDesc,
        snippets = listOf(
            CodeSnippet(
                id = "network-fetch-get",
                name = AppStringsProvider.current().snippetGetRequest,
                description = AppStringsProvider.current().snippetGetRequestDesc,
                code = """async function fetchData(url) {
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('请求失败');
        return await response.json();
    } catch (error) {
        console.error('请求错误:', error);
        return null;
    }
}
fetchData('https://api.example.com/data').then(console.log);""",
                tags = listOf(AppStringsProvider.current().tagGET, AppStringsProvider.current().tagRequest)
            ),
            CodeSnippet(
                id = "network-fetch-post",
                name = AppStringsProvider.current().snippetPostRequest,
                description = AppStringsProvider.current().snippetPostRequestDesc,
                code = """async function postData(url, data) {
    try {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await response.json();
    } catch (error) {
        console.error('请求错误:', error);
        return null;
    }
}
postData('https://api.example.com/submit', { name: 'value' });""",
                tags = listOf(AppStringsProvider.current().tagPOST, AppStringsProvider.current().tagSubmit)
            ),
            CodeSnippet(
                id = "network-fetch-timeout",
                name = AppStringsProvider.current().snippetTimeoutRequest,
                description = AppStringsProvider.current().snippetTimeoutRequestDesc,
                code = """async function fetchWithTimeout(url, timeout = 5000) {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeout);
    
    try {
        const response = await fetch(url, { signal: controller.signal });
        clearTimeout(timeoutId);
        return await response.json();
    } catch (error) {
        if (error.name === 'AbortError') {
            console.error('请求超时');
        }
        return null;
    }
}""",
                tags = listOf(AppStringsProvider.current().tagTimeout, AppStringsProvider.current().tagRequest)
            ),
            CodeSnippet(
                id = "network-retry",
                name = AppStringsProvider.current().snippetRetryRequest,
                description = AppStringsProvider.current().snippetRetryRequestDesc,
                code = """async function fetchWithRetry(url, retries = 3, delay = 1000) {
    for (let i = 0; i < retries; i++) {
        try {
            const response = await fetch(url);
            if (response.ok) return await response.json();
        } catch (error) {
            console.log('重试 ' + (i + 1) + '/' + retries);
            if (i < retries - 1) {
                await new Promise(r => setTimeout(r, delay));
            }
        }
    }
    return null;
}""",
                tags = listOf(AppStringsProvider.current().tagRetry, AppStringsProvider.current().tagRequest)
            ),
            CodeSnippet(
                id = "network-download",
                name = AppStringsProvider.current().snippetDownloadFile,
                description = AppStringsProvider.current().snippetDownloadFileDesc,
                code = """function downloadFile(url, filename) {
    const link = document.createElement('a');
    link.href = url;
    link.download = filename || 'download';
    link.click();
}

// Download Blob.
function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    downloadFile(url, filename);
    URL.revokeObjectURL(url);
}""",
                tags = listOf(AppStringsProvider.current().tagDownload, AppStringsProvider.current().tagFile)
            ),
            CodeSnippet(
                id = "network-jsonp",
                name = AppStringsProvider.current().snippetJsonp,
                description = AppStringsProvider.current().snippetJsonpDesc,
                code = """function jsonp(url, callbackName = 'callback') {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        const fnName = 'jsonp_' + Date.now();
        
        window[fnName] = (data) => {
            resolve(data);
            delete window[fnName];
            script.remove();
        };
        
        script.src = url + (url.includes('?') ? '&' : '?') + callbackName + '=' + fnName;
        script.onerror = reject;
        document.head.appendChild(script);
    });
}""",
                tags = listOf(AppStringsProvider.current().tagJSONP, AppStringsProvider.current().tagCrossDomain)
            )
        )
)
internal fun dataProcessing() = CodeSnippetCategory(
        id = "data",
        name = AppStringsProvider.current().snippetData,
        icon = "📊",
        description = AppStringsProvider.current().snippetDataDesc,
        snippets = listOf(
            CodeSnippet(
                id = "data-extract-table",
                name = AppStringsProvider.current().snippetExtractTable,
                description = AppStringsProvider.current().snippetExtractTableDesc,
                code = """function extractTableData(tableSelector) {
    const table = document.querySelector(tableSelector);
    if (!table) return [];
    
    const headers = Array.from(table.querySelectorAll('th'))
        .map(th => th.textContent.trim());
    
    return Array.from(table.querySelectorAll('tbody tr')).map(row => {
        const cells = row.querySelectorAll('td');
        const obj = {};
        headers.forEach((header, i) => {
            obj[header] = cells[i]?.textContent.trim() || '';
        });
        return obj;
    });
}
const data = extractTableData('table');
console.log(JSON.stringify(data, null, 2));""",
                tags = listOf(AppStringsProvider.current().tagTable, AppStringsProvider.current().tagExtract)
            ),
            CodeSnippet(
                id = "data-extract-links",
                name = AppStringsProvider.current().snippetExtractLinks,
                description = AppStringsProvider.current().snippetExtractLinksDesc,
                code = """function extractLinks(filter = '') {
    return Array.from(document.querySelectorAll('a[href]'))
        .map(a => ({
            text: a.textContent.trim(),
            href: a.href
        }))
        .filter(link => !filter || link.href.includes(filter));
}
const links = extractLinks();
console.log(links);""",
                tags = listOf(AppStringsProvider.current().tagLink, AppStringsProvider.current().tagExtract)
            ),
            CodeSnippet(
                id = "data-extract-images",
                name = AppStringsProvider.current().snippetExtractImages,
                description = AppStringsProvider.current().snippetExtractImagesDesc,
                code = """function extractImages(minSize = 100) {
    return Array.from(document.querySelectorAll('img'))
        .filter(img => img.naturalWidth >= minSize && img.naturalHeight >= minSize)
        .map(img => ({
            src: img.src,
            alt: img.alt,
            width: img.naturalWidth,
            height: img.naturalHeight
        }));
}
const images = extractImages();
console.log(images);""",
                tags = listOf(AppStringsProvider.current().tagImage, AppStringsProvider.current().tagExtract)
            ),
            CodeSnippet(
                id = "data-export-json",
                name = AppStringsProvider.current().snippetExportJson,
                description = AppStringsProvider.current().snippetExportJsonDesc,
                code = """function exportJSON(data, filename = 'data.json') {
    const json = JSON.stringify(data, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}
exportJSON({ name: 'test', value: 123 });""",
                tags = listOf(AppStringsProvider.current().tagExport, AppStringsProvider.current().tagJSON)
            ),
            CodeSnippet(
                id = "data-export-csv",
                name = AppStringsProvider.current().snippetExportCsv,
                description = AppStringsProvider.current().snippetExportCsvDesc,
                code = """function exportCSV(data, filename = 'data.csv') {
    if (!data.length) return;
    
    const headers = Object.keys(data[0]);
    const csv = [
        headers.join(','),
        ...data.map(row => headers.map(h => '"' + (row[h] || '') + '"').join(','))
    ].join('\\n');
    
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
}""",
                tags = listOf(AppStringsProvider.current().tagExport, AppStringsProvider.current().tagCSV)
            ),
            CodeSnippet(
                id = "data-parse-url",
                name = AppStringsProvider.current().snippetParseUrl,
                description = AppStringsProvider.current().snippetParseUrlDesc,
                code = """function parseUrlParams(url = location.href) {
    const params = {};
    const searchParams = new URL(url).searchParams;
    for (const [key, value] of searchParams) {
        params[key] = value;
    }
    return params;
}
const params = parseUrlParams();
console.log(params);""",
                tags = listOf(AppStringsProvider.current().tagURL, AppStringsProvider.current().tagParse)
            ),
            CodeSnippet(
                id = "data-build-url",
                name = AppStringsProvider.current().snippetBuildUrl,
                description = AppStringsProvider.current().snippetBuildUrlDesc,
                code = """function buildUrl(base, params) {
    const url = new URL(base);
    Object.entries(params).forEach(([key, value]) => {
        url.searchParams.set(key, value);
    });
    return url.toString();
}
const url = buildUrl('https://example.com/search', { q: 'test', page: 1 });""",
                tags = listOf(AppStringsProvider.current().tagURL, AppStringsProvider.current().tagBuild)
            )
        )
)
internal fun textProcessing() = CodeSnippetCategory(
        id = "text",
        name = AppStringsProvider.current().snippetText,
        icon = "📄",
        description = AppStringsProvider.current().snippetTextDesc,
        snippets = listOf(
            CodeSnippet(
                id = "text-extract-article",
                name = AppStringsProvider.current().snippetExtractArticle,
                description = AppStringsProvider.current().snippetExtractArticleDesc,
                code = """function extractArticle() {
    const selectors = [
        'article', '[class*="article"]', '[class*="content"]',
        '[class*="post"]', '[class*="entry"]', 'main', '.main'
    ];
    
    for (const selector of selectors) {
        const el = document.querySelector(selector);
        if (el && el.textContent.length > 500) {
            return {
                title: document.querySelector('h1')?.textContent || document.title,
                content: el.innerText,
                html: el.innerHTML
            };
        }
    }
    
    return { title: document.title, content: document.body.innerText };
}
const article = extractArticle();
console.log(article);""",
                tags = listOf(AppStringsProvider.current().tagExtract, AppStringsProvider.current().tagArticle)
            ),
            CodeSnippet(
                id = "text-replace-all",
                name = AppStringsProvider.current().snippetReplaceText,
                description = AppStringsProvider.current().snippetReplaceTextDesc,
                code = """function replaceText(replacements) {
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
    const textNodes = [];
    while (walker.nextNode()) textNodes.push(walker.currentNode);
    
    textNodes.forEach(node => {
        let text = node.textContent;
        Object.entries(replacements).forEach(([from, to]) => {
            text = text.replace(new RegExp(from, 'g'), to);
        });
        node.textContent = text;
    });
}
replaceText({
    '旧文本': '新文本',
    '广告': '[已屏蔽]'
});""",
                tags = listOf(AppStringsProvider.current().tagReplace, AppStringsProvider.current().tagText)
            ),
            CodeSnippet(
                id = "text-translate-selection",
                name = AppStringsProvider.current().snippetTranslateSelection,
                description = AppStringsProvider.current().snippetTranslateSelectionDesc,
                code = """document.addEventListener('mouseup', (e) => {
    const selection = window.getSelection().toString().trim();
    if (!selection || selection.length > 200) return;
    
    // by.
    document.querySelector('#translate-btn')?.remove();
    
    const btn = document.createElement('button');
    btn.id = 'translate-btn';
    btn.innerHTML = '🌐 翻译';
    btn.style.cssText = `
        position: fixed; z-index: 999999;
        left: ${"$"}{e.clientX}px; top: ${"$"}{e.clientY + 10}px;
        padding: 8px 16px; background: #333; color: white;
        border: none; border-radius: 20px; cursor: pointer;
    `;
    btn.onclick = () => {
        const url = 'https://translate.google.com/?sl=auto&tl=zh-CN&text=' + encodeURIComponent(selection);
        window.open(url, '_blank');
        btn.remove();
    };
    
    document.body.appendChild(btn);
    setTimeout(() => btn.remove(), 5000);
});""",
                tags = listOf(AppStringsProvider.current().tagTranslate, AppStringsProvider.current().tagSelectedText)
            ),
            CodeSnippet(
                id = "text-markdown-convert",
                name = AppStringsProvider.current().snippetHtmlToMarkdown,
                description = AppStringsProvider.current().snippetHtmlToMarkdownDesc,
                code = """function htmlToMarkdown(html) {
    let md = html;
    
    md = md.replace(/<h1[^>]*>(.*?)<\/h1>/gi, '# $1\\n');
    md = md.replace(/<h2[^>]*>(.*?)<\/h2>/gi, '## $1\\n');
    md = md.replace(/<h3[^>]*>(.*?)<\/h3>/gi, '### $1\\n');
    
    md = md.replace(/<strong[^>]*>(.*?)<\/strong>/gi, '**$1**');
    md = md.replace(/<b[^>]*>(.*?)<\/b>/gi, '**$1**');
    md = md.replace(/<em[^>]*>(.*?)<\/em>/gi, '*$1*');
    md = md.replace(/<i[^>]*>(.*?)<\/i>/gi, '*$1*');
    
    md = md.replace(/<a[^>]*href="([^"]*)"[^>]*>(.*?)<\/a>/gi, '[$2]($1)');
    md = md.replace(/<img[^>]*src="([^"]*)"[^>]*alt="([^"]*)"[^>]*>/gi, '![$2]($1)');
    
    // List
    md = md.replace(/<li[^>]*>(.*?)<\/li>/gi, '- $1\\n');
    
    md = md.replace(/<p[^>]*>(.*?)<\/p>/gi, '$1\\n\\n');
    md = md.replace(/<br[^>]*>/gi, '\\n');
    
    md = md.replace(/<[^>]+>/g, '');
    
    return md.trim();
}
const md = htmlToMarkdown(document.body.innerHTML);
console.log(md);""",
                tags = listOf(AppStringsProvider.current().tagMarkdown, AppStringsProvider.current().tagConvert)
            )
        )
)
