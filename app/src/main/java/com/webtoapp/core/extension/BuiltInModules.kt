package com.webtoapp.core.extension

import com.webtoapp.core.i18n.Strings

/**
 * 内置扩展模块
 * 
 * 所有模块都使用统一面板系统，UI 整齐美观不重叠
 */
object BuiltInModules {
    
    /**
     * 获取所有内置模块
     */
    fun getAll(): List<ExtensionModule> = listOf(
        videoDownloader(),
        bilibiliVideoExtractor(),
        douyinVideoExtractor(),
        xiaohongshuExtractor(),
        videoEnhancer(),
        webAnalyzer(),
        advancedDarkMode(),
        privacyProtection(),
        contentEnhancer(),
        elementBlocker()
    )
    
    // ==================== 通用视频下载器 ====================
    
    private fun videoDownloader() = ExtensionModule(
        id = "builtin-video-downloader",
        name = Strings.builtinVideoDownloader,
        description = Strings.builtinVideoDownloaderDesc,
        icon = "⬇️",
        category = ModuleCategory.MEDIA,
        tags = listOf("视频", "下载", "MP4"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD),
        code = VIDEO_DOWNLOADER_CODE
    )

    // ==================== B站视频提取 ====================
    
    private fun bilibiliVideoExtractor() = ExtensionModule(
        id = "builtin-bilibili-extractor",
        name = "B站视频",
        description = "提取B站最高画质视频和音频流",
        icon = "📺",
        category = ModuleCategory.MEDIA,
        tags = listOf("B站", "bilibili", "视频"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD),
        urlMatches = listOf(UrlMatchRule("*bilibili.com*")),
        code = BILIBILI_EXTRACTOR_CODE
    )
    
    // ==================== 抖音视频提取 ====================
    
    private fun douyinVideoExtractor() = ExtensionModule(
        id = "builtin-douyin-extractor",
        name = Strings.builtinDouyinExtractor,
        description = Strings.builtinDouyinExtractorDesc,
        icon = "🎵",
        category = ModuleCategory.MEDIA,
        tags = listOf("抖音", "douyin", "无水印"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD),
        urlMatches = listOf(UrlMatchRule("*douyin.com*")),
        code = DOUYIN_EXTRACTOR_CODE
    )
    
    // ==================== 小红书提取 ====================
    
    private fun xiaohongshuExtractor() = ExtensionModule(
        id = "builtin-xiaohongshu-extractor",
        name = Strings.builtinXiaohongshuExtractor,
        description = Strings.builtinXiaohongshuExtractorDesc,
        icon = "📕",
        category = ModuleCategory.MEDIA,
        tags = listOf("小红书", "图片", "视频"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD),
        urlMatches = listOf(UrlMatchRule("*xiaohongshu.com*"), UrlMatchRule("*xhslink.com*")),
        code = XIAOHONGSHU_EXTRACTOR_CODE
    )

    // ==================== 视频增强 ====================
    
    private fun videoEnhancer() = ExtensionModule(
        id = "builtin-video-enhancer",
        name = Strings.builtinVideoEnhancer,
        description = Strings.builtinVideoEnhancerDesc,
        icon = "🎬",
        category = ModuleCategory.VIDEO,
        tags = listOf("倍速", "画中画", "视频"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.MEDIA),
        code = VIDEO_ENHANCER_CODE
    )
    
    // ==================== 网页分析 ====================
    
    private fun webAnalyzer() = ExtensionModule(
        id = "builtin-web-analyzer",
        name = Strings.builtinWebAnalyzer,
        description = Strings.builtinWebAnalyzerDesc,
        icon = "🔍",
        category = ModuleCategory.DEVELOPER,
        tags = listOf("调试", "分析", "开发"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.NETWORK),
        code = WEB_ANALYZER_CODE
    )
    
    // ==================== 深色模式 ====================
    
    private fun advancedDarkMode() = ExtensionModule(
        id = "builtin-dark-mode",
        name = Strings.builtinDarkMode,
        description = Strings.builtinDarkModeDesc,
        icon = "🌙",
        category = ModuleCategory.THEME,
        tags = listOf("深色", "护眼", "主题"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.CSS_INJECT, ModulePermission.STORAGE),
        code = DARK_MODE_CODE
    )

    // ==================== 隐私保护 ====================
    
    private fun privacyProtection() = ExtensionModule(
        id = "builtin-privacy-protection",
        name = Strings.builtinPrivacyProtection,
        description = Strings.builtinPrivacyProtectionDesc,
        icon = "🛡️",
        category = ModuleCategory.SECURITY,
        tags = listOf("隐私", "安全", "反追踪"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.STORAGE),
        code = PRIVACY_PROTECTION_CODE
    )
    
    // ==================== 元素屏蔽器 ====================
    
    private fun elementBlocker() = ExtensionModule(
        id = "builtin-element-blocker",
        name = Strings.builtinElementBlocker,
        description = Strings.builtinElementBlockerDesc,
        icon = "🚫",
        category = ModuleCategory.CONTENT_FILTER,
        tags = listOf("屏蔽", "广告", "元素"),
        version = ModuleVersion(1, "1.0.0", "元素屏蔽器"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.STORAGE),
        code = ELEMENT_BLOCKER_CODE
    )
    
    // ==================== 内容增强 ====================
    
    private fun contentEnhancer() = ExtensionModule(
        id = "builtin-content-enhancer",
        name = Strings.builtinContentEnhancer,
        description = Strings.builtinContentEnhancerDesc,
        icon = "✨",
        category = ModuleCategory.CONTENT_ENHANCE,
        tags = listOf("复制", "翻译", "截图"),
        version = ModuleVersion(2, "2.0.0", "使用统一面板"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.CLIPBOARD),
        code = CONTENT_ENHANCER_CODE
    )


    // ==================== 模块代码定义 ====================
    
    private const val VIDEO_DOWNLOADER_CODE = """
(function() {
    'use strict';
    
    const MODULE = { id: 'video-downloader', name: '视频下载', icon: '⬇️', color: '#667eea' };
    let videos = [];
    
    function detectVideos() {
        videos = [];
        document.querySelectorAll('video').forEach((v, i) => {
            const src = v.src || v.currentSrc || v.querySelector('source')?.src;
            if (src) videos.push({ i, src, blob: src.startsWith('blob:'), w: v.videoWidth, h: v.videoHeight });
        });
        return videos;
    }
    
    function getPanelHtml() {
        detectVideos();
        if (!videos.length) return '<div style="text-align:center;padding:40px;color:#9ca3af"><div style="font-size:48px;margin-bottom:16px">🎬</div><div>未检测到视频</div></div>';
        
        return '<div style="color:#6b7280;font-size:13px;margin-bottom:16px">检测到 ' + videos.length + ' 个视频</div>' +
            videos.map((v, i) => '<div style="background:#f9fafb;border-radius:12px;padding:16px;margin-bottom:12px;display:flex;align-items:center;gap:12px">' +
                '<div style="width:48px;height:48px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:12px;display:flex;align-items:center;justify-content:center;color:white;font-size:20px">🎬</div>' +
                '<div style="flex:1"><div style="font-weight:600;color:#1f2937">视频 ' + (i+1) + '</div><div style="font-size:12px;color:#9ca3af">' + v.w + 'x' + v.h + ' · ' + (v.blob ? 'Blob流' : 'MP4') + '</div></div>' +
                '<button onclick="__wtaDownloadVideo(' + i + ')" style="background:linear-gradient(135deg,#667eea,#764ba2);color:white;border:none;padding:10px 20px;border-radius:8px;font-size:14px;cursor:pointer">下载</button></div>'
            ).join('');
    }
    
    window.__wtaDownloadVideo = function(i) {
        const v = videos[i];
        if (!v) return;
        if (v.blob) { __WTA_MODULE_UI__.toast('Blob流暂不支持直接下载'); return; }
        if (typeof NativeBridge !== 'undefined' && NativeBridge.downloadVideo) {
            NativeBridge.downloadVideo(v.src, 'video_' + Date.now() + '.mp4');
            __WTA_MODULE_UI__.toast('开始下载...');
            __WTA_MODULE_UI__.closePanel();
        }
    };
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    
    document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', register) : register();
})();
"""


    private const val BILIBILI_EXTRACTOR_CODE = """
(function() {
    'use strict';
    if (!location.hostname.includes('bilibili.com')) return;
    
    const MODULE = { id: 'bilibili', name: 'B站视频', icon: '📺', color: '#fb7299' };
    const QN = { 127:'8K', 120:'4K', 116:'1080P60', 80:'1080P', 64:'720P', 32:'480P' };
    
    function getInfo() {
        const p = window.__playinfo__;
        if (!p?.data) return null;
        const d = p.data, r = { video: null, audio: null, quality: '' };
        if (d.dash) {
            if (d.dash.video?.length) { const v = d.dash.video.sort((a,b) => (b.bandwidth||0)-(a.bandwidth||0))[0]; r.video = v.baseUrl || v.base_url; r.quality = QN[v.id] || v.id+'P'; }
            if (d.dash.audio?.length) { r.audio = d.dash.audio.sort((a,b) => (b.bandwidth||0)-(a.bandwidth||0))[0].baseUrl; }
        } else if (d.durl?.length) { r.video = d.durl[0].url; r.quality = QN[d.quality] || d.quality+'P'; }
        return r;
    }
    
    function getPanelHtml() {
        const info = getInfo();
        const title = document.querySelector('h1.video-title, .video-title')?.textContent || '未知视频';
        if (!info?.video && !info?.audio) return '<div style="text-align:center;padding:40px;color:#9ca3af"><div style="font-size:48px;margin-bottom:16px">📺</div><div>未找到视频信息</div><div style="font-size:12px;margin-top:8px">请等待视频加载</div></div>';
        
        let html = '<div style="margin-bottom:20px"><div style="font-size:15px;font-weight:600;color:#1f2937;margin-bottom:8px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + title + '</div><div style="font-size:13px;color:#fb7299">画质: ' + info.quality + '</div></div>';
        if (info.video) html += '<button onclick="__wtaBiliDL(\'video\')" style="width:100%;background:linear-gradient(135deg,#fb7299,#fc9db8);color:white;border:none;padding:14px;border-radius:12px;font-size:15px;font-weight:500;cursor:pointer;margin-bottom:12px;display:flex;align-items:center;justify-content:center;gap:8px"><span>⬇️</span> 下载视频流</button>';
        if (info.audio) html += '<button onclick="__wtaBiliDL(\'audio\')" style="width:100%;background:linear-gradient(135deg,#23ade5,#5bc0de);color:white;border:none;padding:14px;border-radius:12px;font-size:15px;font-weight:500;cursor:pointer;display:flex;align-items:center;justify-content:center;gap:8px"><span>🎵</span> 下载音频流</button>';
        html += '<div style="margin-top:16px;padding:12px;background:#fef3f6;border-radius:8px;font-size:12px;color:#9ca3af">提示：B站视频和音频分离，需用工具合并</div>';
        return html;
    }
    
    window.__wtaBiliDL = function(type) {
        const info = getInfo();
        const url = type === 'video' ? info?.video : info?.audio;
        if (!url) return;
        const fn = 'bilibili_' + type + '_' + Date.now() + (type === 'video' ? '.m4s' : '.m4a');
        if (typeof NativeBridge !== 'undefined' && NativeBridge.downloadWithHeaders) {
            NativeBridge.downloadWithHeaders(url, fn, JSON.stringify({ Referer: 'https://www.bilibili.com' }));
            __WTA_MODULE_UI__.toast('开始下载' + (type === 'video' ? '视频' : '音频') + '...');
            __WTA_MODULE_UI__.closePanel();
        } else { navigator.clipboard?.writeText(url); __WTA_MODULE_UI__.toast('链接已复制'); }
    };
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    setTimeout(register, 1000);
})();
"""


    private const val DOUYIN_EXTRACTOR_CODE = """
(function() {
    'use strict';
    if (!location.hostname.includes('douyin.com')) return;
    
    const MODULE = { id: 'douyin', name: '抖音视频', icon: '🎵', color: '#fe2c55' };
    
    function findVideoData(obj, depth = 0) {
        if (depth > 10 || !obj || typeof obj !== 'object') return null;
        if (obj.video?.play_addr) {
            const url = obj.video.play_addr.url_list?.[0]?.replace('playwm', 'play').replace(/watermark=\d+/, 'watermark=0');
            return { id: obj.aweme_id || obj.id, desc: obj.desc || '', url, author: obj.author?.nickname || '' };
        }
        if (obj.aweme_detail) return findVideoData(obj.aweme_detail, depth + 1);
        if (obj.aweme_list?.[0]) return findVideoData(obj.aweme_list[0], depth + 1);
        for (const k of Object.keys(obj)) { const r = findVideoData(obj[k], depth + 1); if (r) return r; }
        return null;
    }
    
    function getVideoData() {
        try {
            if (window._ROUTER_DATA) { const r = findVideoData(window._ROUTER_DATA); if (r) return r; }
            if (window.__INITIAL_STATE__) { const r = findVideoData(window.__INITIAL_STATE__); if (r) return r; }
        } catch (e) {}
        return null;
    }
    
    function getPanelHtml() {
        const data = getVideoData();
        if (!data?.url) return '<div style="text-align:center;padding:40px;color:#9ca3af"><div style="font-size:48px;margin-bottom:16px">🎵</div><div>未找到视频</div><div style="font-size:12px;margin-top:8px">请等待视频加载</div></div>';
        
        return '<div style="margin-bottom:20px"><div style="font-size:15px;font-weight:600;color:#1f2937;margin-bottom:8px;overflow:hidden;text-overflow:ellipsis;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical">' + (data.desc || '抖音视频') + '</div>' +
            '<div style="font-size:13px;color:#9ca3af">@' + data.author + '</div></div>' +
            '<button onclick="__wtaDouyinDL()" style="width:100%;background:linear-gradient(135deg,#fe2c55,#ff6b81);color:white;border:none;padding:14px;border-radius:12px;font-size:15px;font-weight:500;cursor:pointer;display:flex;align-items:center;justify-content:center;gap:8px"><span>⬇️</span> 下载无水印视频</button>' +
            '<div style="margin-top:16px;padding:12px;background:#fff1f3;border-radius:8px;font-size:12px;color:#9ca3af">提示：下载的是无水印版本</div>';
    }
    
    window.__wtaDouyinDL = function() {
        const data = getVideoData();
        if (!data?.url) return;
        if (typeof NativeBridge !== 'undefined' && NativeBridge.downloadVideo) {
            NativeBridge.downloadVideo(data.url, 'douyin_' + (data.id || Date.now()) + '.mp4');
            __WTA_MODULE_UI__.toast('开始下载...');
            __WTA_MODULE_UI__.closePanel();
        } else { navigator.clipboard?.writeText(data.url); __WTA_MODULE_UI__.toast('链接已复制'); }
    };
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    setTimeout(register, 1000);
})();
"""


    private const val XIAOHONGSHU_EXTRACTOR_CODE = """
(function() {
    'use strict';
    if (!location.hostname.includes('xiaohongshu.com') && !location.hostname.includes('xhslink.com')) return;
    
    const MODULE = { id: 'xiaohongshu', name: '小红书', icon: '📕', color: '#ff2442' };
    let mediaList = [];
    
    function detectMedia() {
        mediaList = [];
        // 检测图片
        document.querySelectorAll('img[src*="xhscdn"], img[src*="xiaohongshu"]').forEach((img, i) => {
            let src = img.src.split('?')[0];
            if (src.includes('avatar') || img.width < 100) return;
            if (!mediaList.find(m => m.src === src)) mediaList.push({ type: 'image', src, i: mediaList.length });
        });
        // 检测视频
        document.querySelectorAll('video').forEach((v, i) => {
            const src = v.src || v.querySelector('source')?.src;
            if (src && !mediaList.find(m => m.src === src)) mediaList.push({ type: 'video', src, i: mediaList.length });
        });
        return mediaList;
    }
    
    function getPanelHtml() {
        detectMedia();
        if (!mediaList.length) return '<div style="text-align:center;padding:40px;color:#9ca3af"><div style="font-size:48px;margin-bottom:16px">📕</div><div>未检测到媒体</div></div>';
        
        const images = mediaList.filter(m => m.type === 'image');
        const videos = mediaList.filter(m => m.type === 'video');
        
        let html = '<div style="color:#6b7280;font-size:13px;margin-bottom:16px">检测到 ' + images.length + ' 张图片，' + videos.length + ' 个视频</div>';
        
        if (images.length) {
            html += '<button onclick="__wtaXhsDLAll(\'image\')" style="width:100%;background:linear-gradient(135deg,#ff2442,#ff6b7a);color:white;border:none;padding:14px;border-radius:12px;font-size:15px;font-weight:500;cursor:pointer;margin-bottom:12px;display:flex;align-items:center;justify-content:center;gap:8px"><span>🖼️</span> 下载全部图片 (' + images.length + ')</button>';
        }
        if (videos.length) {
            html += '<button onclick="__wtaXhsDLAll(\'video\')" style="width:100%;background:linear-gradient(135deg,#667eea,#764ba2);color:white;border:none;padding:14px;border-radius:12px;font-size:15px;font-weight:500;cursor:pointer;margin-bottom:12px;display:flex;align-items:center;justify-content:center;gap:8px"><span>🎬</span> 下载全部视频 (' + videos.length + ')</button>';
        }
        
        html += '<div style="margin-top:8px;max-height:200px;overflow-y:auto">';
        mediaList.forEach((m, i) => {
            html += '<div style="display:flex;align-items:center;gap:12px;padding:12px;background:#f9fafb;border-radius:8px;margin-bottom:8px">' +
                '<span style="font-size:20px">' + (m.type === 'image' ? '🖼️' : '🎬') + '</span>' +
                '<div style="flex:1;font-size:13px;color:#4b5563;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + (m.type === 'image' ? '图片' : '视频') + ' ' + (i+1) + '</div>' +
                '<button onclick="__wtaXhsDL(' + i + ')" style="background:#f3f4f6;border:none;padding:6px 12px;border-radius:6px;font-size:12px;cursor:pointer">下载</button></div>';
        });
        html += '</div>';
        return html;
    }
    
    window.__wtaXhsDL = function(i) {
        const m = mediaList[i];
        if (!m) return;
        const ext = m.type === 'image' ? '.jpg' : '.mp4';
        if (typeof NativeBridge !== 'undefined' && NativeBridge.downloadVideo) {
            NativeBridge.downloadVideo(m.src, 'xhs_' + m.type + '_' + Date.now() + ext);
            __WTA_MODULE_UI__.toast('开始下载...');
        }
    };
    
    window.__wtaXhsDLAll = function(type) {
        const items = mediaList.filter(m => m.type === type);
        items.forEach((m, i) => setTimeout(() => __wtaXhsDL(mediaList.indexOf(m)), i * 500));
        __WTA_MODULE_UI__.toast('开始下载 ' + items.length + ' 个' + (type === 'image' ? '图片' : '视频') + '...');
        __WTA_MODULE_UI__.closePanel();
    };
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    setTimeout(register, 1000);
})();
"""


    private const val VIDEO_ENHANCER_CODE = """
(function() {
    'use strict';
    
    const MODULE = { id: 'video-enhancer', name: '视频增强', icon: '🎬', color: '#8b5cf6' };
    let currentSpeed = 1.0;
    const speeds = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0, 3.0];
    
    function getVideo() { return document.querySelector('video'); }
    
    function setSpeed(speed) {
        const v = getVideo();
        if (v) { v.playbackRate = speed; currentSpeed = speed; __WTA_MODULE_UI__.toast('播放速度: ' + speed + 'x'); }
    }
    
    function togglePiP() {
        const v = getVideo();
        if (!v) return;
        if (document.pictureInPictureElement) { document.exitPictureInPicture(); __WTA_MODULE_UI__.toast('退出画中画'); }
        else { v.requestPictureInPicture().then(() => __WTA_MODULE_UI__.toast('已开启画中画')).catch(() => __WTA_MODULE_UI__.toast('画中画不可用')); }
    }
    
    function getPanelHtml() {
        const v = getVideo();
        if (!v) return '<div style="text-align:center;padding:40px;color:#9ca3af"><div style="font-size:48px;margin-bottom:16px">🎬</div><div>未检测到视频</div></div>';
        
        return '<div style="margin-bottom:20px"><div style="font-size:14px;color:#6b7280;margin-bottom:12px">播放速度</div>' +
            '<div style="display:flex;flex-wrap:wrap;gap:8px">' +
            speeds.map(s => '<button onclick="__wtaSetSpeed(' + s + ')" style="flex:1;min-width:60px;padding:12px 8px;border-radius:8px;border:none;font-size:14px;cursor:pointer;' + 
                (currentSpeed === s ? 'background:linear-gradient(135deg,#8b5cf6,#a78bfa);color:white' : 'background:#f3f4f6;color:#374151') + '">' + s + 'x</button>').join('') +
            '</div></div>' +
            '<div style="margin-bottom:16px"><div style="font-size:14px;color:#6b7280;margin-bottom:12px">功能</div>' +
            '<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">' +
            '<button onclick="__wtaTogglePiP()" style="padding:14px;border-radius:12px;border:none;background:#f3f4f6;font-size:14px;cursor:pointer;display:flex;flex-direction:column;align-items:center;gap:4px"><span style="font-size:20px">📺</span>画中画</button>' +
            '<button onclick="__wtaToggleLoop()" style="padding:14px;border-radius:12px;border:none;background:#f3f4f6;font-size:14px;cursor:pointer;display:flex;flex-direction:column;align-items:center;gap:4px"><span style="font-size:20px">🔁</span>循环播放</button>' +
            '<button onclick="__wtaSkip(-10)" style="padding:14px;border-radius:12px;border:none;background:#f3f4f6;font-size:14px;cursor:pointer;display:flex;flex-direction:column;align-items:center;gap:4px"><span style="font-size:20px">⏪</span>后退10秒</button>' +
            '<button onclick="__wtaSkip(10)" style="padding:14px;border-radius:12px;border:none;background:#f3f4f6;font-size:14px;cursor:pointer;display:flex;flex-direction:column;align-items:center;gap:4px"><span style="font-size:20px">⏩</span>前进10秒</button>' +
            '</div></div>';
    }
    
    window.__wtaSetSpeed = function(s) { setSpeed(s); };
    window.__wtaTogglePiP = togglePiP;
    window.__wtaToggleLoop = function() { const v = getVideo(); if (v) { v.loop = !v.loop; __WTA_MODULE_UI__.toast(v.loop ? '已开启循环' : '已关闭循环'); } };
    window.__wtaSkip = function(s) { const v = getVideo(); if (v) { v.currentTime += s; __WTA_MODULE_UI__.toast((s > 0 ? '前进' : '后退') + Math.abs(s) + '秒'); } };
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', register) : register();
})();
"""


    private const val WEB_ANALYZER_CODE = """
(function() {
    'use strict';
    
    const MODULE = { id: 'web-analyzer', name: '网页分析', icon: '🔍', color: '#059669' };
    
    function getPageInfo() {
        const scripts = document.querySelectorAll('script[src]').length;
        const styles = document.querySelectorAll('link[rel="stylesheet"]').length;
        const images = document.querySelectorAll('img').length;
        const links = document.querySelectorAll('a[href]').length;
        const forms = document.querySelectorAll('form').length;
        const iframes = document.querySelectorAll('iframe').length;
        const videos = document.querySelectorAll('video').length;
        
        return { scripts, styles, images, links, forms, iframes, videos };
    }
    
    function getPanelHtml() {
        const info = getPageInfo();
        const perf = performance.timing;
        const loadTime = perf.loadEventEnd - perf.navigationStart;
        const domReady = perf.domContentLoadedEventEnd - perf.navigationStart;
        
        return '<div style="margin-bottom:20px"><div style="font-size:14px;color:#6b7280;margin-bottom:12px">页面信息</div>' +
            '<div style="background:#f9fafb;border-radius:12px;padding:16px">' +
            '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">' +
            '<div><div style="font-size:12px;color:#9ca3af">标题</div><div style="font-size:13px;color:#1f2937;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">' + document.title + '</div></div>' +
            '<div><div style="font-size:12px;color:#9ca3af">域名</div><div style="font-size:13px;color:#1f2937">' + location.hostname + '</div></div>' +
            '</div></div></div>' +
            
            '<div style="margin-bottom:20px"><div style="font-size:14px;color:#6b7280;margin-bottom:12px">性能数据</div>' +
            '<div style="display:grid;grid-template-columns:1fr 1fr;gap:8px">' +
            '<div style="background:#ecfdf5;padding:12px;border-radius:8px;text-align:center"><div style="font-size:20px;font-weight:600;color:#059669">' + (loadTime > 0 ? loadTime : '-') + '</div><div style="font-size:11px;color:#6b7280">加载时间(ms)</div></div>' +
            '<div style="background:#eff6ff;padding:12px;border-radius:8px;text-align:center"><div style="font-size:20px;font-weight:600;color:#3b82f6">' + (domReady > 0 ? domReady : '-') + '</div><div style="font-size:11px;color:#6b7280">DOM就绪(ms)</div></div>' +
            '</div></div>' +
            
            '<div><div style="font-size:14px;color:#6b7280;margin-bottom:12px">元素统计</div>' +
            '<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:8px">' +
            [['📜', info.scripts, '脚本'], ['🎨', info.styles, '样式'], ['🖼️', info.images, '图片'], ['🔗', info.links, '链接'],
             ['📝', info.forms, '表单'], ['📺', info.iframes, 'iframe'], ['🎬', info.videos, '视频']].map(([icon, count, name]) =>
                '<div style="background:#f9fafb;padding:10px;border-radius:8px;text-align:center"><div style="font-size:16px">' + icon + '</div><div style="font-size:16px;font-weight:600;color:#1f2937">' + count + '</div><div style="font-size:10px;color:#9ca3af">' + name + '</div></div>'
            ).join('') +
            '</div></div>';
    }
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', register) : register();
})();
"""


    private const val DARK_MODE_CODE = """
(function() {
    'use strict';
    
    const MODULE = { id: 'dark-mode', name: '深色模式', icon: '🌙', color: '#6366f1' };
    const STORAGE_KEY = 'wta_dark_mode';
    let enabled = localStorage.getItem(STORAGE_KEY) === 'true';
    let styleEl = null;
    
    const darkCSS = 'html{filter:invert(1) hue-rotate(180deg)!important;background:#111!important}img,video,picture,canvas,iframe,[style*="background-image"]{filter:invert(1) hue-rotate(180deg)!important}';
    
    function toggle() {
        enabled = !enabled;
        localStorage.setItem(STORAGE_KEY, enabled);
        apply();
        __WTA_MODULE_UI__.toast(enabled ? '已开启深色模式' : '已关闭深色模式');
    }
    
    function apply() {
        if (enabled) {
            if (!styleEl) { styleEl = document.createElement('style'); styleEl.id = 'wta-dark-mode'; document.head.appendChild(styleEl); }
            styleEl.textContent = darkCSS;
        } else if (styleEl) { styleEl.textContent = ''; }
    }
    
    function getPanelHtml() {
        return '<div style="text-align:center;padding:20px">' +
            '<div style="font-size:64px;margin-bottom:20px">' + (enabled ? '🌙' : '☀️') + '</div>' +
            '<div style="font-size:18px;font-weight:600;color:#1f2937;margin-bottom:8px">' + (enabled ? '深色模式已开启' : '深色模式已关闭') + '</div>' +
            '<div style="font-size:13px;color:#9ca3af;margin-bottom:24px">智能反色，保护眼睛</div>' +
            '<button onclick="__wtaToggleDark()" style="width:100%;padding:14px;border-radius:12px;border:none;font-size:15px;font-weight:500;cursor:pointer;' +
            (enabled ? 'background:#f3f4f6;color:#374151' : 'background:linear-gradient(135deg,#6366f1,#8b5cf6);color:white') + '">' +
            (enabled ? '☀️ 关闭深色模式' : '🌙 开启深色模式') + '</button></div>';
    }
    
    window.__wtaToggleDark = function() { toggle(); if (typeof __WTA_MODULE_UI__ !== 'undefined') __WTA_MODULE_UI__.updatePanel(MODULE.id, getPanelHtml()); };
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    
    apply();
    document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', register) : register();
})();
"""


    private const val PRIVACY_PROTECTION_CODE = """
(function() {
    'use strict';
    
    const MODULE = { id: 'privacy', name: '隐私保护', icon: '🛡️', color: '#dc2626' };
    const STORAGE_KEY = 'wta_privacy';
    let settings = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{"tracking":true,"fingerprint":true,"cookies":false}');
    
    function save() { localStorage.setItem(STORAGE_KEY, JSON.stringify(settings)); }
    
    function applyProtection() {
        if (settings.tracking) {
            // 阻止常见追踪器
            const blocked = ['google-analytics.com', 'googletagmanager.com', 'facebook.net', 'doubleclick.net', 'hotjar.com'];
            const origFetch = window.fetch;
            window.fetch = function(url, opts) {
                if (blocked.some(b => url.toString().includes(b))) { console.log('[Privacy] Blocked:', url); return Promise.reject(); }
                return origFetch.apply(this, arguments);
            };
        }
        if (settings.fingerprint) {
            // 模糊指纹
            Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4 });
            Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });
            Object.defineProperty(screen, 'colorDepth', { get: () => 24 });
        }
    }
    
    function getPanelHtml() {
        const items = [
            { key: 'tracking', icon: '🚫', name: '阻止追踪', desc: '拦截常见追踪脚本' },
            { key: 'fingerprint', icon: '🎭', name: '指纹保护', desc: '模糊设备指纹信息' },
            { key: 'cookies', icon: '🍪', name: '清理Cookies', desc: '退出时清理Cookies' }
        ];
        
        return '<div style="margin-bottom:16px;text-align:center"><div style="font-size:48px;margin-bottom:8px">🛡️</div><div style="font-size:13px;color:#9ca3af">保护您的隐私安全</div></div>' +
            items.map(item => '<div style="display:flex;align-items:center;gap:12px;padding:16px;background:#f9fafb;border-radius:12px;margin-bottom:8px">' +
                '<span style="font-size:24px">' + item.icon + '</span>' +
                '<div style="flex:1"><div style="font-weight:500;color:#1f2937">' + item.name + '</div><div style="font-size:12px;color:#9ca3af">' + item.desc + '</div></div>' +
                '<label style="position:relative;width:48px;height:28px"><input type="checkbox" ' + (settings[item.key] ? 'checked' : '') + ' onchange="__wtaPrivacyToggle(\'' + item.key + '\')" style="opacity:0;width:0;height:0">' +
                '<span style="position:absolute;cursor:pointer;top:0;left:0;right:0;bottom:0;background:' + (settings[item.key] ? '#22c55e' : '#d1d5db') + ';border-radius:14px;transition:.3s"></span>' +
                '<span style="position:absolute;height:24px;width:24px;left:' + (settings[item.key] ? '22px' : '2px') + ';bottom:2px;background:white;border-radius:50%;transition:.3s;box-shadow:0 1px 3px rgba(0,0,0,.2)"></span></label></div>'
            ).join('');
    }
    
    window.__wtaPrivacyToggle = function(key) {
        settings[key] = !settings[key];
        save();
        __WTA_MODULE_UI__.toast(settings[key] ? '已开启' : '已关闭');
        if (typeof __WTA_MODULE_UI__ !== 'undefined') __WTA_MODULE_UI__.updatePanel(MODULE.id, getPanelHtml());
    };
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    
    applyProtection();
    document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', register) : register();
})();
"""


    private const val CONTENT_ENHANCER_CODE = """
(function() {
    'use strict';
    
    const MODULE = { id: 'content-enhancer', name: '内容增强', icon: '✨', color: '#f59e0b' };
    
    function enableCopy() {
        document.body.style.userSelect = 'auto';
        document.body.style.webkitUserSelect = 'auto';
        ['copy', 'cut', 'paste', 'selectstart', 'contextmenu'].forEach(e => {
            document.addEventListener(e, ev => ev.stopPropagation(), true);
        });
        const style = document.createElement('style');
        style.textContent = '*{user-select:auto!important;-webkit-user-select:auto!important}';
        document.head.appendChild(style);
        __WTA_MODULE_UI__.toast('已解除复制限制');
    }
    
    function copyPageText() {
        const text = document.body.innerText;
        navigator.clipboard?.writeText(text).then(() => __WTA_MODULE_UI__.toast('页面文本已复制'));
    }
    
    function copyPageHtml() {
        const html = document.documentElement.outerHTML;
        navigator.clipboard?.writeText(html).then(() => __WTA_MODULE_UI__.toast('页面HTML已复制'));
    }
    
    function scrollToTop() { window.scrollTo({ top: 0, behavior: 'smooth' }); __WTA_MODULE_UI__.toast('已回到顶部'); }
    function scrollToBottom() { window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' }); __WTA_MODULE_UI__.toast('已到达底部'); }
    
    function getPanelHtml() {
        const tools = [
            { icon: '📋', name: '解除复制限制', fn: '__wtaEnableCopy()' },
            { icon: '📄', name: '复制页面文本', fn: '__wtaCopyText()' },
            { icon: '🔤', name: '复制页面HTML', fn: '__wtaCopyHtml()' },
            { icon: '⬆️', name: '回到顶部', fn: '__wtaScrollTop()' },
            { icon: '⬇️', name: '滚动到底部', fn: '__wtaScrollBottom()' }
        ];
        
        return '<div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">' +
            tools.map(t => '<button onclick="' + t.fn + '" style="padding:20px 12px;border-radius:12px;border:none;background:#f9fafb;cursor:pointer;display:flex;flex-direction:column;align-items:center;gap:8px;transition:all .2s">' +
                '<span style="font-size:28px">' + t.icon + '</span>' +
                '<span style="font-size:13px;color:#374151">' + t.name + '</span></button>'
            ).join('') + '</div>';
    }
    
    window.__wtaEnableCopy = enableCopy;
    window.__wtaCopyText = copyPageText;
    window.__wtaCopyHtml = copyPageHtml;
    window.__wtaScrollTop = scrollToTop;
    window.__wtaScrollBottom = scrollToBottom;
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', register) : register();
})();
"""


    private const val ELEMENT_BLOCKER_CODE = """
(function() {
    'use strict';
    
    // 多语言支持
    const LANG = (navigator.language || 'zh').toLowerCase().startsWith('ar') ? 'ar' : 
                 (navigator.language || 'zh').toLowerCase().startsWith('zh') ? 'zh' : 'en';
    const I18N = {
        zh: {
            name: '元素屏蔽',
            blocked: '已屏蔽元素',
            unblocked: '已取消屏蔽',
            selected: '已选中',
            dblClickToBlock: '双击屏蔽',
            selectMode: '选择模式：单击选择，双击屏蔽，按 ESC 退出',
            clearedAll: '已清除所有屏蔽',
            selectElement: '选择要屏蔽的元素',
            blockedCount: '已屏蔽 {0} 个元素',
            delete: '删除',
            clearAll: '清除所有屏蔽',
            clickToSelect: '点击上方按钮选择要屏蔽的元素'
        },
        en: {
            name: 'Element Blocker',
            blocked: 'Element blocked',
            unblocked: 'Unblocked',
            selected: 'Selected',
            dblClickToBlock: 'double-click to block',
            selectMode: 'Select mode: click to select, double-click to block, ESC to exit',
            clearedAll: 'All blocks cleared',
            selectElement: 'Select element to block',
            blockedCount: '{0} elements blocked',
            delete: 'Delete',
            clearAll: 'Clear all blocks',
            clickToSelect: 'Click the button above to select elements'
        },
        ar: {
            name: 'مانع العناصر',
            blocked: 'تم حظر العنصر',
            unblocked: 'تم إلغاء الحظر',
            selected: 'محدد',
            dblClickToBlock: 'انقر مرتين للحظر',
            selectMode: 'وضع التحديد: انقر للتحديد، انقر مرتين للحظر، ESC للخروج',
            clearedAll: 'تم مسح جميع الحظر',
            selectElement: 'حدد العنصر للحظر',
            blockedCount: 'تم حظر {0} عنصر',
            delete: 'حذف',
            clearAll: 'مسح جميع الحظر',
            clickToSelect: 'انقر على الزر أعلاه لتحديد العناصر'
        }
    };
    const T = I18N[LANG] || I18N.en;
    
    const MODULE = { id: 'element-blocker', name: T.name, icon: '🚫', color: '#ef4444' };
    const STORAGE_KEY = 'wta_blocked_elements';
    let blockedSelectors = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    let selectMode = false;
    let hoveredElement = null;
    let highlightOverlay = null;
    
    // 创建高亮覆盖层
    function createOverlay() {
        if (highlightOverlay) return;
        highlightOverlay = document.createElement('div');
        highlightOverlay.id = 'wta-element-highlight';
        highlightOverlay.style.cssText = 'position:fixed;pointer-events:none;z-index:2147483646;border:2px solid #ef4444;background:rgba(239,68,68,0.15);transition:all 0.1s ease;display:none';
        document.body.appendChild(highlightOverlay);
    }
    
    // 生成元素的唯一选择器
    function getSelector(el) {
        if (!el || el === document.body || el === document.documentElement) return null;
        if (el.id) return '#' + CSS.escape(el.id);
        
        let path = [];
        while (el && el !== document.body && el !== document.documentElement) {
            let selector = el.tagName.toLowerCase();
            if (el.className && typeof el.className === 'string') {
                const classes = el.className.trim().split(/\s+/).filter(c => c && !c.includes('wta-'));
                if (classes.length) selector += '.' + classes.map(c => CSS.escape(c)).join('.');
            }
            const parent = el.parentElement;
            if (parent) {
                const siblings = Array.from(parent.children).filter(c => c.tagName === el.tagName);
                if (siblings.length > 1) {
                    const idx = siblings.indexOf(el) + 1;
                    selector += ':nth-of-type(' + idx + ')';
                }
            }
            path.unshift(selector);
            el = parent;
            if (path.length >= 4) break;
        }
        return path.join(' > ');
    }
    
    // 应用屏蔽规则
    function applyBlockedRules() {
        let styleEl = document.getElementById('wta-blocked-styles');
        if (!styleEl) {
            styleEl = document.createElement('style');
            styleEl.id = 'wta-blocked-styles';
            document.head.appendChild(styleEl);
        }
        if (blockedSelectors.length) {
            styleEl.textContent = blockedSelectors.map(s => s + '{display:none!important}').join('');
        } else {
            styleEl.textContent = '';
        }
    }
    
    // 保存屏蔽规则
    function saveRules() {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(blockedSelectors));
        applyBlockedRules();
    }
    
    // 屏蔽元素
    function blockElement(selector) {
        if (!selector || blockedSelectors.includes(selector)) return;
        blockedSelectors.push(selector);
        saveRules();
        __WTA_MODULE_UI__.toast(T.blocked);
    }
    
    // 取消屏蔽
    function unblockElement(index) {
        blockedSelectors.splice(index, 1);
        saveRules();
        __WTA_MODULE_UI__.toast(T.unblocked);
        __WTA_MODULE_UI__.updatePanel(MODULE.id, getPanelHtml());
    }
    
    // 鼠标移动事件
    function onMouseMove(e) {
        if (!selectMode) return;
        const el = document.elementFromPoint(e.clientX, e.clientY);
        if (!el || el === highlightOverlay || el.closest('#wta-module-panel') || el.closest('#wta-module-fab')) {
            if (highlightOverlay) highlightOverlay.style.display = 'none';
            hoveredElement = null;
            return;
        }
        hoveredElement = el;
        const rect = el.getBoundingClientRect();
        if (highlightOverlay) {
            highlightOverlay.style.display = 'block';
            highlightOverlay.style.left = rect.left + 'px';
            highlightOverlay.style.top = rect.top + 'px';
            highlightOverlay.style.width = rect.width + 'px';
            highlightOverlay.style.height = rect.height + 'px';
        }
    }
    
    // 单击选择
    function onClick(e) {
        if (!selectMode || !hoveredElement) return;
        e.preventDefault();
        e.stopPropagation();
        const selector = getSelector(hoveredElement);
        if (selector) {
            __WTA_MODULE_UI__.toast(T.selected + ': ' + hoveredElement.tagName.toLowerCase() + ' (' + T.dblClickToBlock + ')');
        }
    }
    
    // 双击屏蔽
    function onDblClick(e) {
        if (!selectMode || !hoveredElement) return;
        e.preventDefault();
        e.stopPropagation();
        const selector = getSelector(hoveredElement);
        if (selector) {
            blockElement(selector);
            exitSelectMode();
        }
    }
    
    // 进入选择模式
    function enterSelectMode() {
        selectMode = true;
        createOverlay();
        document.addEventListener('mousemove', onMouseMove, true);
        document.addEventListener('click', onClick, true);
        document.addEventListener('dblclick', onDblClick, true);
        document.body.style.cursor = 'crosshair';
        __WTA_MODULE_UI__.toast(T.selectMode);
        __WTA_MODULE_UI__.closePanel();
        
        // ESC 退出
        document.addEventListener('keydown', function escHandler(e) {
            if (e.key === 'Escape') {
                exitSelectMode();
                document.removeEventListener('keydown', escHandler);
            }
        });
    }
    
    // 退出选择模式
    function exitSelectMode() {
        selectMode = false;
        hoveredElement = null;
        if (highlightOverlay) highlightOverlay.style.display = 'none';
        document.removeEventListener('mousemove', onMouseMove, true);
        document.removeEventListener('click', onClick, true);
        document.removeEventListener('dblclick', onDblClick, true);
        document.body.style.cursor = '';
    }
    
    // 清除所有屏蔽
    function clearAll() {
        blockedSelectors = [];
        saveRules();
        __WTA_MODULE_UI__.toast(T.clearedAll);
        __WTA_MODULE_UI__.updatePanel(MODULE.id, getPanelHtml());
    }
    
    function getPanelHtml() {
        let html = '<div style="margin-bottom:20px">' +
            '<button onclick="__wtaEnterSelectMode()" style="width:100%;background:linear-gradient(135deg,#ef4444,#f87171);color:white;border:none;padding:14px;border-radius:12px;font-size:15px;font-weight:500;cursor:pointer;display:flex;align-items:center;justify-content:center;gap:8px">' +
            '<span>👆</span> ' + T.selectElement + '</button></div>';
        
        html += '<div style="font-size:14px;color:#6b7280;margin-bottom:12px">' + T.blockedCount.replace('{0}', blockedSelectors.length) + '</div>';
        
        if (blockedSelectors.length) {
            html += '<div style="max-height:200px;overflow-y:auto">';
            blockedSelectors.forEach((selector, i) => {
                html += '<div style="display:flex;align-items:center;gap:8px;padding:10px;background:#f9fafb;border-radius:8px;margin-bottom:8px">' +
                    '<span style="flex:1;font-size:12px;color:#4b5563;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-family:monospace">' + selector.replace(/</g, '&lt;') + '</span>' +
                    '<button onclick="__wtaUnblock(' + i + ')" style="background:#fee2e2;color:#dc2626;border:none;padding:4px 8px;border-radius:4px;font-size:12px;cursor:pointer">' + T.delete + '</button></div>';
            });
            html += '</div>';
            html += '<button onclick="__wtaClearAllBlocks()" style="width:100%;margin-top:12px;background:#f3f4f6;color:#6b7280;border:none;padding:10px;border-radius:8px;font-size:13px;cursor:pointer">' + T.clearAll + '</button>';
        } else {
            html += '<div style="text-align:center;padding:24px;color:#9ca3af"><div style="font-size:32px;margin-bottom:8px">🎯</div><div style="font-size:13px">' + T.clickToSelect + '</div></div>';
        }
        
        return html;
    }
    
    window.__wtaEnterSelectMode = enterSelectMode;
    window.__wtaUnblock = unblockElement;
    window.__wtaClearAllBlocks = clearAll;
    
    function register() {
        if (typeof __WTA_MODULE_UI__ === 'undefined') { setTimeout(register, 100); return; }
        __WTA_MODULE_UI__.register({ ...MODULE, onAction: c => c.innerHTML = getPanelHtml() });
    }
    
    applyBlockedRules();
    document.readyState === 'loading' ? document.addEventListener('DOMContentLoaded', register) : register();
})();
"""
}
