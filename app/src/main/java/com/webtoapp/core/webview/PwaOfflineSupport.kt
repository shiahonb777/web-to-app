package com.webtoapp.core.webview

import android.webkit.WebView
import com.webtoapp.core.logging.AppLogger

/**
 * PWA 离线支持
 * 
 * 通过向网页注入 Service Worker 缓存层，使包装的网站 App 支持离线访问。
 * 
 * 工作原理：
 * 1. 在页面加载完成后注入 Service Worker 注册脚本
 * 2. Service Worker 使用 Cache API 缓存关键资源（HTML、CSS、JS、图片）
 * 3. 离线时从缓存返回资源，显示缓存版本而非错误页
 * 4. 支持三种缓存策略：Cache First / Network First / Stale While Revalidate
 * 
 * 注意：Android WebView 从 Chrome 40+ 开始支持 Service Worker
 */
object PwaOfflineSupport {
    
    private const val TAG = "PwaOfflineSupport"
    
    /**
     * 缓存策略
     */
    enum class CacheStrategy {
        /** 优先从缓存读取，缓存无数据时走网络 — 最快但可能过时 */
        CACHE_FIRST,
        /** 优先走网络，网络失败时用缓存 — 保证数据最新 */
        NETWORK_FIRST,
        /** 立即返回缓存并在后台更新 — 平衡速度和新鲜度 */
        STALE_WHILE_REVALIDATE
    }
    
    /**
     * 离线支持配置
     */
    data class OfflineConfig(
        val enabled: Boolean = false,
        val strategy: CacheStrategy = CacheStrategy.NETWORK_FIRST,
        val maxCacheSizeMb: Int = 50,
        val maxAgeHours: Int = 24 * 7,
        val cacheableExtensions: List<String> = DEFAULT_CACHEABLE_EXTENSIONS,
        val excludePatterns: List<String> = emptyList()
    )
    
    private val DEFAULT_CACHEABLE_EXTENSIONS = listOf(
        "html", "htm", "css", "js", "json",
        "png", "jpg", "jpeg", "gif", "webp", "svg", "ico",
        "woff", "woff2", "ttf", "eot",
        "mp3", "mp4", "webm"
    )
    
    /**
     * 生成 Service Worker 脚本
     * 这个脚本会被注入到 WebView 中注册 Service Worker
     */
    fun generateServiceWorkerScript(config: OfflineConfig): String {
        val strategyName = when (config.strategy) {
            CacheStrategy.CACHE_FIRST -> "cache-first"
            CacheStrategy.NETWORK_FIRST -> "network-first"
            CacheStrategy.STALE_WHILE_REVALIDATE -> "stale-while-revalidate"
        }
        
        val extensionsArray = config.cacheableExtensions.joinToString(",") { "'$it'" }
        val excludeArray = config.excludePatterns.joinToString(",") { "'$it'" }
        val maxAgeMs = config.maxAgeHours.toLong() * 3600 * 1000
        val maxCacheBytes = config.maxCacheSizeMb.toLong() * 1024 * 1024
        
        return """
            const WTA_SW_SCRIPT = `
const CACHE_NAME = 'wta-offline-v1';
const STRATEGY = '${strategyName}';
const MAX_AGE_MS = ${maxAgeMs};
const MAX_CACHE_BYTES = ${maxCacheBytes};
const CACHEABLE_EXTENSIONS = [${extensionsArray}];
const EXCLUDE_PATTERNS = [${excludeArray}];

function isCacheable(url) {
    try {
        const u = new URL(url);
        if (u.protocol !== 'https:' && u.protocol !== 'http:') return false;
        for (const pattern of EXCLUDE_PATTERNS) {
            if (url.includes(pattern)) return false;
        }
        const ext = u.pathname.split('.').pop().toLowerCase().split('?')[0];
        if (CACHEABLE_EXTENSIONS.includes(ext)) return true;
        return false;
    } catch (e) { return false; }
}

async function trimCache() {
    try {
        const cache = await caches.open(CACHE_NAME);
        const keys = await cache.keys();
        let totalSize = 0;
        const entries = [];
        for (const req of keys) {
            const resp = await cache.match(req);
            if (resp) {
                const blob = await resp.clone().blob();
                const ts = resp.headers.get('x-wta-cached-at');
                entries.push({ req, size: blob.size, ts: ts ? parseInt(ts) : 0 });
                totalSize += blob.size;
            }
        }
        // Remove expired entries
        const now = Date.now();
        for (const e of entries) {
            if (e.ts > 0 && (now - e.ts) > MAX_AGE_MS) {
                await cache.delete(e.req);
                totalSize -= e.size;
            }
        }
        // If still over size limit, remove oldest first
        if (totalSize > MAX_CACHE_BYTES) {
            entries.sort((a, b) => a.ts - b.ts);
            for (const e of entries) {
                if (totalSize <= MAX_CACHE_BYTES) break;
                await cache.delete(e.req);
                totalSize -= e.size;
            }
        }
    } catch (e) { /* ignore trim errors */ }
}

async function addToCache(request, response) {
    try {
        const cache = await caches.open(CACHE_NAME);
        const headers = new Headers(response.headers);
        headers.set('x-wta-cached-at', String(Date.now()));
        const cachedResponse = new Response(await response.clone().blob(), {
            status: response.status,
            statusText: response.statusText,
            headers: headers
        });
        await cache.put(request, cachedResponse);
    } catch (e) { /* ignore cache write errors */ }
}

async function cacheFirst(request) {
    const cached = await caches.match(request);
    if (cached) return cached;
    try {
        const response = await fetch(request);
        if (response.ok && isCacheable(request.url)) {
            addToCache(request, response);
        }
        return response;
    } catch (e) {
        return new Response('Offline', { status: 503, statusText: 'Offline' });
    }
}

async function networkFirst(request) {
    try {
        const response = await fetch(request);
        if (response.ok && isCacheable(request.url)) {
            addToCache(request, response);
        }
        return response;
    } catch (e) {
        const cached = await caches.match(request);
        if (cached) return cached;
        return new Response('Offline', { status: 503, statusText: 'Offline' });
    }
}

async function staleWhileRevalidate(request) {
    const cached = await caches.match(request);
    const fetchPromise = fetch(request).then(response => {
        if (response.ok && isCacheable(request.url)) {
            addToCache(request, response);
        }
        return response;
    }).catch(() => null);
    
    if (cached) {
        fetchPromise; // fire and forget background update
        return cached;
    }
    const response = await fetchPromise;
    if (response) return response;
    return new Response('Offline', { status: 503, statusText: 'Offline' });
}

self.addEventListener('install', event => {
    self.skipWaiting();
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(names =>
            Promise.all(names.filter(n => n !== CACHE_NAME).map(n => caches.delete(n)))
        ).then(() => self.clients.claim())
    );
    // Trim cache periodically
    trimCache();
});

self.addEventListener('fetch', event => {
    const request = event.request;
    if (request.method !== 'GET') return;
    if (!isCacheable(request.url) && !request.url.endsWith('/')) return;
    
    let handler;
    switch (STRATEGY) {
        case 'cache-first': handler = cacheFirst; break;
        case 'stale-while-revalidate': handler = staleWhileRevalidate; break;
        default: handler = networkFirst;
    }
    event.respondWith(handler(request));
});
`;

            // Register Service Worker via Blob URL
            (function() {
                if (!('serviceWorker' in navigator)) {
                    console.log('[WTA] Service Worker not supported');
                    return;
                }
                
                // Check if already registered
                navigator.serviceWorker.getRegistrations().then(function(regs) {
                    const existing = regs.find(r => r.active && r.active.scriptURL.includes('blob:'));
                    if (existing) {
                        console.log('[WTA] Service Worker already registered');
                        return;
                    }
                    
                    const blob = new Blob([WTA_SW_SCRIPT], { type: 'application/javascript' });
                    const swUrl = URL.createObjectURL(blob);
                    
                    navigator.serviceWorker.register(swUrl, { scope: '/' })
                        .then(function(reg) {
                            console.log('[WTA] Service Worker registered:', reg.scope);
                            URL.revokeObjectURL(swUrl);
                        })
                        .catch(function(err) {
                            console.warn('[WTA] Service Worker registration failed:', err.message);
                            URL.revokeObjectURL(swUrl);
                        });
                });
            })();
        """.trimIndent()
    }
    
    /**
     * 注入 Service Worker 到 WebView
     * 应在 onPageFinished 后调用
     */
    fun injectServiceWorker(webView: WebView, config: OfflineConfig) {
        if (!config.enabled) return
        
        val script = generateServiceWorkerScript(config)
        webView.evaluateJavascript(script) { result ->
            AppLogger.d(TAG, "Service Worker injection result: $result")
        }
        AppLogger.i(TAG, "PWA offline support injected (strategy: ${config.strategy})")
    }
    
    /**
     * 生成离线提示页面 HTML
     * 当完全无网络且缓存也没有数据时显示
     */
    fun generateOfflineFallbackHtml(): String = """
        <!DOCTYPE html>
        <html lang="zh">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>离线模式</title>
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    display: flex; align-items: center; justify-content: center;
                    min-height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white; text-align: center; padding: 20px;
                }
                .container { max-width: 400px; }
                .icon { font-size: 64px; margin-bottom: 20px; }
                h1 { font-size: 24px; margin-bottom: 12px; font-weight: 600; }
                p { font-size: 16px; opacity: 0.85; line-height: 1.5; margin-bottom: 24px; }
                button {
                    background: rgba(255,255,255,0.2); color: white; border: 2px solid rgba(255,255,255,0.4);
                    padding: 12px 32px; border-radius: 25px; font-size: 16px; cursor: pointer;
                    transition: all 0.3s; backdrop-filter: blur(10px);
                }
                button:hover { background: rgba(255,255,255,0.3); }
                button:active { transform: scale(0.95); }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="icon">📡</div>
                <h1>当前处于离线状态</h1>
                <p>请检查网络连接后重试。<br>部分已缓存的内容可以正常浏览。</p>
                <button onclick="location.reload()">重新加载</button>
            </div>
        </body>
        </html>
    """.trimIndent()
}
