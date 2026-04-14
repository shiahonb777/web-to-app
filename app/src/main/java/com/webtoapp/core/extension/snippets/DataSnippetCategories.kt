package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.Strings

internal fun storageOperations() = CodeSnippetCategory(
        id = "storage",
        name = Strings.snippetStorage,
        icon = "💾",
        description = Strings.snippetStorageDesc,
        snippets = listOf(
            CodeSnippet(
                id = "storage-local-set",
                name = Strings.snippetLocalSet,
                description = Strings.snippetLocalSetDesc,
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
                tags = listOf(Strings.tagStorage, Strings.tagSave)
            ),
            CodeSnippet(
                id = "storage-local-get",
                name = Strings.snippetLocalGet,
                description = Strings.snippetLocalGetDesc,
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
                tags = listOf(Strings.tagStorage, Strings.tagRead)
            ),
            CodeSnippet(
                id = "storage-session",
                name = Strings.snippetSessionStorage,
                description = Strings.snippetSessionStorageDesc,
                code = """// Save（页面关闭后清除）
sessionStorage.setItem('key', 'value');
const value = sessionStorage.getItem('key');
// Delete
sessionStorage.removeItem('key');
sessionStorage.clear();""",
                tags = listOf(Strings.tagSession, Strings.tagTemporary)
            ),
            CodeSnippet(
                id = "storage-cookie-set",
                name = Strings.snippetSetCookie,
                description = Strings.snippetSetCookieDesc,
                code = """function setCookie(name, value, days = 7) {
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    document.cookie = name + '=' + encodeURIComponent(value) + 
        '; expires=' + expires + '; path=/';
}
setCookie('myCookie', 'value', 30);""",
                tags = listOf(Strings.tagCookie, Strings.tagSetting)
            ),
            CodeSnippet(
                id = "storage-cookie-get",
                name = Strings.snippetGetCookie,
                description = Strings.snippetGetCookieDesc,
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
                tags = listOf(Strings.tagCookie, Strings.tagRead)
            ),
            CodeSnippet(
                id = "storage-cookie-delete",
                name = Strings.snippetDeleteCookie,
                description = Strings.snippetDeleteCookieDesc,
                code = """function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
}
deleteCookie('myCookie');""",
                tags = listOf(Strings.tagCookie, Strings.tagDelete)
            ),
            CodeSnippet(
                id = "storage-indexeddb",
                name = Strings.snippetIndexedDB,
                description = Strings.snippetIndexedDBDesc,
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
                tags = listOf(Strings.tagIndexedDB, Strings.tagBigData)
            )
        )
)
internal fun networkOperations() = CodeSnippetCategory(
        id = "network",
        name = Strings.snippetNetwork,
        icon = "🌐",
        description = Strings.snippetNetworkDesc,
        snippets = listOf(
            CodeSnippet(
                id = "network-fetch-get",
                name = Strings.snippetGetRequest,
                description = Strings.snippetGetRequestDesc,
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
                tags = listOf(Strings.tagGET, Strings.tagRequest)
            ),
            CodeSnippet(
                id = "network-fetch-post",
                name = Strings.snippetPostRequest,
                description = Strings.snippetPostRequestDesc,
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
                tags = listOf(Strings.tagPOST, Strings.tagSubmit)
            ),
            CodeSnippet(
                id = "network-fetch-timeout",
                name = Strings.snippetTimeoutRequest,
                description = Strings.snippetTimeoutRequestDesc,
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
                tags = listOf(Strings.tagTimeout, Strings.tagRequest)
            ),
            CodeSnippet(
                id = "network-retry",
                name = Strings.snippetRetryRequest,
                description = Strings.snippetRetryRequestDesc,
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
                tags = listOf(Strings.tagRetry, Strings.tagRequest)
            ),
            CodeSnippet(
                id = "network-download",
                name = Strings.snippetDownloadFile,
                description = Strings.snippetDownloadFileDesc,
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
                tags = listOf(Strings.tagDownload, Strings.tagFile)
            ),
            CodeSnippet(
                id = "network-jsonp",
                name = Strings.snippetJsonp,
                description = Strings.snippetJsonpDesc,
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
                tags = listOf(Strings.tagJSONP, Strings.tagCrossDomain)
            )
        )
)
internal fun dataProcessing() = CodeSnippetCategory(
        id = "data",
        name = Strings.snippetData,
        icon = "📊",
        description = Strings.snippetDataDesc,
        snippets = listOf(
            CodeSnippet(
                id = "data-extract-table",
                name = Strings.snippetExtractTable,
                description = Strings.snippetExtractTableDesc,
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
                tags = listOf(Strings.tagTable, Strings.tagExtract)
            ),
            CodeSnippet(
                id = "data-extract-links",
                name = Strings.snippetExtractLinks,
                description = Strings.snippetExtractLinksDesc,
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
                tags = listOf(Strings.tagLink, Strings.tagExtract)
            ),
            CodeSnippet(
                id = "data-extract-images",
                name = Strings.snippetExtractImages,
                description = Strings.snippetExtractImagesDesc,
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
                tags = listOf(Strings.tagImage, Strings.tagExtract)
            ),
            CodeSnippet(
                id = "data-export-json",
                name = Strings.snippetExportJson,
                description = Strings.snippetExportJsonDesc,
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
                tags = listOf(Strings.tagExport, Strings.tagJSON)
            ),
            CodeSnippet(
                id = "data-export-csv",
                name = Strings.snippetExportCsv,
                description = Strings.snippetExportCsvDesc,
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
                tags = listOf(Strings.tagExport, Strings.tagCSV)
            ),
            CodeSnippet(
                id = "data-parse-url",
                name = Strings.snippetParseUrl,
                description = Strings.snippetParseUrlDesc,
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
                tags = listOf(Strings.tagURL, Strings.tagParse)
            ),
            CodeSnippet(
                id = "data-build-url",
                name = Strings.snippetBuildUrl,
                description = Strings.snippetBuildUrlDesc,
                code = """function buildUrl(base, params) {
    const url = new URL(base);
    Object.entries(params).forEach(([key, value]) => {
        url.searchParams.set(key, value);
    });
    return url.toString();
}
const url = buildUrl('https://example.com/search', { q: 'test', page: 1 });""",
                tags = listOf(Strings.tagURL, Strings.tagBuild)
            )
        )
)
internal fun textProcessing() = CodeSnippetCategory(
        id = "text",
        name = Strings.snippetText,
        icon = "📄",
        description = Strings.snippetTextDesc,
        snippets = listOf(
            CodeSnippet(
                id = "text-extract-article",
                name = Strings.snippetExtractArticle,
                description = Strings.snippetExtractArticleDesc,
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
                tags = listOf(Strings.tagExtract, Strings.tagArticle)
            ),
            CodeSnippet(
                id = "text-replace-all",
                name = Strings.snippetReplaceText,
                description = Strings.snippetReplaceTextDesc,
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
                tags = listOf(Strings.tagReplace, Strings.tagText)
            ),
            CodeSnippet(
                id = "text-translate-selection",
                name = Strings.snippetTranslateSelection,
                description = Strings.snippetTranslateSelectionDesc,
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
                tags = listOf(Strings.tagTranslate, Strings.tagSelectedText)
            ),
            CodeSnippet(
                id = "text-markdown-convert",
                name = Strings.snippetHtmlToMarkdown,
                description = Strings.snippetHtmlToMarkdownDesc,
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
                tags = listOf(Strings.tagMarkdown, Strings.tagConvert)
            )
        )
)
