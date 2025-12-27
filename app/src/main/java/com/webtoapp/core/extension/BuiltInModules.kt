package com.webtoapp.core.extension

/**
 * 内置扩展模块
 * 
 * 提供一些常用的预置模块，用户可以直接使用或作为参考
 */
object BuiltInModules {
    
    /**
     * 获取所有内置模块
     * 包含9个功能丰富的新模块
     */
    fun getAll(): List<ExtensionModule> = listOf(
        videoDownloader(),
        bilibiliVideoExtractor(),
        douyinVideoExtractor(),
        xiaohongshuVideoExtractor(),
        xiaohongshuImageDownloader(),  // 新增：小红书图片下载器
        videoEnhancer(),
        webAnalyzer(),
        advancedDarkMode(),
        privacyProtection(),
        contentEnhancer()
    )
    
    // ==================== 通用视频下载器 ====================
    
    private const val VIDEO_DOWNLOADER_CODE = """
(function() {
    'use strict';
    
    // 防抖配置
    const DEBOUNCE_DELAY = 500;
    let debounceTimer = null;
    let downloadBtn = null;
    let currentVideoSrc = null;
    
    // 创建下载按钮
    function createDownloadButton() {
        if (downloadBtn) return downloadBtn;
        
        downloadBtn = document.createElement('div');
        downloadBtn.id = 'wta-video-download-btn';
        downloadBtn.innerHTML = '⬇️';
        downloadBtn.style.cssText = `
            position: fixed;
            bottom: 80px;
            right: 20px;
            width: 56px;
            height: 56px;
            border-radius: 50%;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            display: none;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            cursor: pointer;
            z-index: 999999;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
            transition: transform 0.2s, box-shadow 0.2s;
            -webkit-tap-highlight-color: transparent;
            user-select: none;
        `;
        
        // 触摸/点击效果
        downloadBtn.addEventListener('touchstart', () => {
            downloadBtn.style.transform = 'scale(0.95)';
        }, { passive: true });
        
        downloadBtn.addEventListener('touchend', () => {
            downloadBtn.style.transform = 'scale(1)';
        }, { passive: true });
        
        downloadBtn.addEventListener('click', handleDownload);
        document.body.appendChild(downloadBtn);
        
        return downloadBtn;
    }
    
    // 处理下载
    function handleDownload() {
        if (!currentVideoSrc) {
            console.warn('[VideoDownloader] 没有可用的视频源');
            return;
        }
        
        console.log('[VideoDownloader] 尝试下载:', currentVideoSrc);
        
        // 判断视频地址类型
        if (currentVideoSrc.startsWith('blob:')) {
            // Blob URL - 打印 MediaSource 信息
            console.log('[VideoDownloader] 检测到 Blob URL');
            console.log('[VideoDownloader] Blob URL:', currentVideoSrc);
            
            // 尝试获取更多信息
            const video = document.querySelector('video');
            if (video) {
                console.log('[VideoDownloader] 视频信息:', {
                    duration: video.duration,
                    videoWidth: video.videoWidth,
                    videoHeight: video.videoHeight,
                    currentSrc: video.currentSrc,
                    readyState: video.readyState
                });
            }
            
            // 通知用户
            showToast('Blob视频流，详情已打印到控制台');
            
            // 尝试通过 NativeBridge 处理
            if (typeof NativeBridge !== 'undefined' && NativeBridge.downloadVideo) {
                NativeBridge.downloadVideo(currentVideoSrc, 'blob_video.mp4');
            }
        } else {
            // 普通 MP4 地址 - 直接下载
            console.log('[VideoDownloader] 检测到普通视频地址，开始下载');
            
            // 优先使用 NativeBridge
            if (typeof NativeBridge !== 'undefined' && NativeBridge.downloadVideo) {
                const filename = extractFilename(currentVideoSrc);
                NativeBridge.downloadVideo(currentVideoSrc, filename);
                showToast('开始下载视频...');
            } else {
                // 降级方案：使用 a 标签下载
                const a = document.createElement('a');
                a.href = currentVideoSrc;
                a.download = extractFilename(currentVideoSrc);
                a.style.display = 'none';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                showToast('开始下载...');
            }
        }
    }
    
    // 提取文件名
    function extractFilename(url) {
        try {
            const urlObj = new URL(url);
            const pathname = urlObj.pathname;
            const filename = pathname.split('/').pop();
            if (filename && filename.includes('.')) {
                return filename;
            }
        } catch (e) {}
        return 'video_' + Date.now() + '.mp4';
    }
    
    // 显示提示
    function showToast(message) {
        const toast = document.createElement('div');
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            bottom: 150px;
            left: 50%;
            transform: translateX(-50%);
            background: rgba(0,0,0,0.8);
            color: white;
            padding: 12px 24px;
            border-radius: 24px;
            font-size: 14px;
            z-index: 9999999;
            animation: fadeInOut 2s ease-in-out;
        `;
        
        // 添加动画样式
        if (!document.getElementById('wta-toast-style')) {
            const style = document.createElement('style');
            style.id = 'wta-toast-style';
            style.textContent = `
                @keyframes fadeInOut {
                    0% { opacity: 0; transform: translateX(-50%) translateY(20px); }
                    20% { opacity: 1; transform: translateX(-50%) translateY(0); }
                    80% { opacity: 1; transform: translateX(-50%) translateY(0); }
                    100% { opacity: 0; transform: translateX(-50%) translateY(-20px); }
                }
            `;
            document.head.appendChild(style);
        }
        
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 2000);
    }
    
    // 检测视频并更新按钮
    function detectVideos() {
        const videos = document.querySelectorAll('video');
        let foundSrc = null;
        
        for (const video of videos) {
            // 优先获取 src 属性
            let src = video.src || video.currentSrc;
            
            // 检查 source 子元素
            if (!src) {
                const source = video.querySelector('source');
                if (source) src = source.src;
            }
            
            if (src) {
                foundSrc = src;
                break;
            }
        }
        
        // 更新按钮状态
        const btn = createDownloadButton();
        if (foundSrc && foundSrc !== currentVideoSrc) {
            currentVideoSrc = foundSrc;
            btn.style.display = 'flex';
            console.log('[VideoDownloader] 检测到视频:', foundSrc.substring(0, 100));
        } else if (!foundSrc) {
            currentVideoSrc = null;
            btn.style.display = 'none';
        }
    }
    
    // 防抖检测
    function debouncedDetect() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(detectVideos, DEBOUNCE_DELAY);
    }
    
    // 初始化
    function init() {
        // 初始检测
        detectVideos();
        
        // 监听 DOM 变化
        const observer = new MutationObserver(debouncedDetect);
        observer.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['src']
        });
        
        // 监听视频事件
        document.addEventListener('loadedmetadata', debouncedDetect, true);
        document.addEventListener('play', debouncedDetect, true);
        
        console.log('[VideoDownloader] 模块已初始化');
    }
    
    // 启动
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
"""

    /**
     * 通用视频下载器
     * 自动检测网页视频，支持普通MP4和Blob流
     */
    private fun videoDownloader() = ExtensionModule(
        id = "builtin-video-downloader",
        name = "视频下载器",
        description = "自动检测网页视频，显示下载按钮。支持普通MP4直接下载，Blob流打印详情",
        icon = "⬇️",
        category = ModuleCategory.MEDIA,
        tags = listOf("视频", "下载", "MP4", "媒体"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD, ModulePermission.MEDIA),
        code = VIDEO_DOWNLOADER_CODE.trimIndent()
    )
    
    // ==================== B站视频提取器 ====================
    
    private const val BILIBILI_EXTRACTOR_CODE = """
(function() {
    'use strict';
    
    const REFERER = 'https://www.bilibili.com';
    let panel = null;
    let videoInfo = null;
    
    // 解析 __playinfo__ 获取视频音频地址
    function parsePlayInfo() {
        const playinfo = window.__playinfo__;
        if (!playinfo || !playinfo.data) {
            console.warn('[BilibiliExtractor] 未找到 __playinfo__');
            return null;
        }
        
        const data = playinfo.data;
        const result = { video: null, audio: null, quality: '' };
        
        // DASH 格式 (新版)
        if (data.dash) {
            const dash = data.dash;
            
            // 获取最高画质视频
            if (dash.video && dash.video.length > 0) {
                const videos = dash.video.sort((a, b) => (b.bandwidth || 0) - (a.bandwidth || 0));
                const best = videos[0];
                result.video = best.baseUrl || best.base_url;
                result.quality = getQualityName(best.id);
                console.log('[BilibiliExtractor] 视频流:', result.quality, best.bandwidth);
            }
            
            // 获取最高音质音频
            if (dash.audio && dash.audio.length > 0) {
                const audios = dash.audio.sort((a, b) => (b.bandwidth || 0) - (a.bandwidth || 0));
                const best = audios[0];
                result.audio = best.baseUrl || best.base_url;
                console.log('[BilibiliExtractor] 音频流:', best.bandwidth);
            }
        }
        // FLV 格式 (旧版)
        else if (data.durl && data.durl.length > 0) {
            result.video = data.durl[0].url;
            result.quality = getQualityName(data.quality);
            result.audio = null; // FLV 格式音视频合并
        }
        
        return result;
    }
    
    // 画质名称映射
    function getQualityName(qn) {
        const map = {
            127: '8K',
            126: '杜比视界',
            125: 'HDR',
            120: '4K',
            116: '1080P60',
            112: '1080P+',
            80: '1080P',
            74: '720P60',
            64: '720P',
            32: '480P',
            16: '360P'
        };
        return map[qn] || qn + 'P';
    }
    
    // 创建 UI 面板
    function createPanel() {
        if (panel) panel.remove();
        
        panel = document.createElement('div');
        panel.id = 'wta-bilibili-panel';
        panel.style.cssText = `
            position: fixed;
            bottom: 80px;
            right: 20px;
            background: rgba(30, 30, 30, 0.95);
            border-radius: 16px;
            padding: 16px;
            z-index: 999999;
            min-width: 200px;
            box-shadow: 0 8px 32px rgba(0,0,0,0.3);
            font-family: -apple-system, BlinkMacSystemFont, sans-serif;
            backdrop-filter: blur(10px);
            display: none;
        `;
        
        document.body.appendChild(panel);
        return panel;
    }
    
    // 更新面板内容
    function updatePanel(info) {
        if (!panel) createPanel();
        
        const title = document.querySelector('h1.video-title')?.textContent || 
                      document.querySelector('.video-title')?.textContent || 
                      '未知视频';
        
        panel.innerHTML = `
            <div style="color: #fff; margin-bottom: 12px; font-size: 14px; font-weight: 600;">
                B站视频提取
            </div>
            <div style="color: #aaa; font-size: 12px; margin-bottom: 12px; 
                        max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">
                ${'$'}{title}
            </div>
            <div style="color: #fb7299; font-size: 12px; margin-bottom: 16px;">
                画质: ${'$'}{info.quality || '未知'}
            </div>
            ${'$'}{info.video ? `
                <div class="wta-btn" data-type="video" style="
                    background: linear-gradient(135deg, #fb7299 0%, #fc9db8 100%);
                    color: white;
                    padding: 12px 16px;
                    border-radius: 8px;
                    margin-bottom: 8px;
                    cursor: pointer;
                    text-align: center;
                    font-size: 14px;
                    transition: transform 0.2s;
                ">⬇️ 下载视频流</div>
            ` : ''}
            ${'$'}{info.audio ? `
                <div class="wta-btn" data-type="audio" style="
                    background: linear-gradient(135deg, #23ade5 0%, #5bc0de 100%);
                    color: white;
                    padding: 12px 16px;
                    border-radius: 8px;
                    cursor: pointer;
                    text-align: center;
                    font-size: 14px;
                    transition: transform 0.2s;
                ">🎵 下载音频流</div>
            ` : ''}
            ${'$'}{!info.video && !info.audio ? `
                <div style="color: #ff6b6b; font-size: 12px;">未找到可用流</div>
            ` : ''}
            <div style="color: #666; font-size: 10px; margin-top: 12px; text-align: center;">
                点击外部关闭
            </div>
        `;
        
        // 绑定按钮事件
        panel.querySelectorAll('.wta-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.stopPropagation();
                const type = btn.dataset.type;
                const url = type === 'video' ? info.video : info.audio;
                downloadMedia(url, type);
            });
            
            btn.addEventListener('touchstart', () => {
                btn.style.transform = 'scale(0.95)';
            }, { passive: true });
            
            btn.addEventListener('touchend', () => {
                btn.style.transform = 'scale(1)';
            }, { passive: true });
        });
    }
    
    // 下载媒体
    function downloadMedia(url, type) {
        if (!url) return;
        
        console.log('[BilibiliExtractor] 下载' + type + ':', url);
        console.log('[BilibiliExtractor] Referer:', REFERER);
        
        // 通过 NativeBridge 发送到原生端（支持多线程下载和自定义 Header）
        if (typeof NativeBridge !== 'undefined') {
            const filename = 'bilibili_' + type + '_' + Date.now() + (type === 'video' ? '.m4s' : '.m4a');
            const headers = JSON.stringify({ 'Referer': REFERER });
            
            if (NativeBridge.downloadWithHeaders) {
                NativeBridge.downloadWithHeaders(url, filename, headers);
                showToast('开始下载' + (type === 'video' ? '视频' : '音频') + '...');
            } else if (NativeBridge.downloadVideo) {
                NativeBridge.downloadVideo(url, filename);
                showToast('开始下载（无Referer）...');
            }
        } else {
            // 降级：复制链接
            copyToClipboard(url);
            showToast('链接已复制，请用下载工具下载');
        }
        
        panel.style.display = 'none';
    }
    
    // 复制到剪贴板
    function copyToClipboard(text) {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
    }
    
    // 显示提示
    function showToast(message) {
        const toast = document.createElement('div');
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: rgba(0,0,0,0.85);
            color: white;
            padding: 16px 32px;
            border-radius: 12px;
            font-size: 14px;
            z-index: 9999999;
        `;
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 2000);
    }
    
    // 创建触发按钮
    function createTriggerButton() {
        const btn = document.createElement('div');
        btn.id = 'wta-bilibili-btn';
        btn.innerHTML = '📺';
        btn.style.cssText = `
            position: fixed;
            bottom: 80px;
            right: 20px;
            width: 56px;
            height: 56px;
            border-radius: 50%;
            background: linear-gradient(135deg, #fb7299 0%, #fc9db8 100%);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            cursor: pointer;
            z-index: 999998;
            box-shadow: 0 4px 15px rgba(251, 114, 153, 0.4);
            transition: transform 0.2s;
            -webkit-tap-highlight-color: transparent;
        `;
        
        btn.addEventListener('click', () => {
            videoInfo = parsePlayInfo();
            if (videoInfo && (videoInfo.video || videoInfo.audio)) {
                updatePanel(videoInfo);
                panel.style.display = 'block';
            } else {
                showToast('未找到视频信息，请等待视频加载');
            }
        });
        
        btn.addEventListener('touchstart', () => {
            btn.style.transform = 'scale(0.95)';
        }, { passive: true });
        
        btn.addEventListener('touchend', () => {
            btn.style.transform = 'scale(1)';
        }, { passive: true });
        
        document.body.appendChild(btn);
    }
    
    // 初始化
    function init() {
        // 检查是否在 B站
        if (!location.hostname.includes('bilibili.com')) {
            console.log('[BilibiliExtractor] 非B站页面，跳过');
            return;
        }
        
        createPanel();
        createTriggerButton();
        
        // 点击外部关闭面板
        document.addEventListener('click', (e) => {
            if (panel && panel.style.display === 'block' && !panel.contains(e.target)) {
                panel.style.display = 'none';
            }
        });
        
        console.log('[BilibiliExtractor] 模块已初始化');
    }
    
    // 启动
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        setTimeout(init, 1000); // 等待 __playinfo__ 加载
    }
})();
"""

    /**
     * B站视频提取器
     * 解析 __playinfo__ 获取最高画质视频和音频流
     */
    private fun bilibiliVideoExtractor() = ExtensionModule(
        id = "builtin-bilibili-extractor",
        name = "B站视频提取",
        description = "提取B站视频的最高画质视频流和音频流地址，支持DASH格式",
        icon = "📺",
        category = ModuleCategory.MEDIA,
        tags = listOf("B站", "bilibili", "视频", "下载"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD, ModulePermission.NETWORK),
        urlMatches = listOf(
            UrlMatchRule("*://www.bilibili.com/*"),
            UrlMatchRule("*://m.bilibili.com/*")
        ),
        code = BILIBILI_EXTRACTOR_CODE.trimIndent()
    )
    
    // ==================== 抖音视频提取器 ====================
    
    private const val DOUYIN_EXTRACTOR_CODE = """
(function() {
    'use strict';
    
    const DEBOUNCE_DELAY = 800;
    let debounceTimer = null;
    let currentVideoId = null;
    let extractBtn = null;
    
    // 从页面数据中提取视频信息
    function extractVideoData() {
        let videoData = null;
        
        // 方法1: 从 RENDER_DATA 提取
        try {
            const scripts = document.querySelectorAll('script');
            for (const script of scripts) {
                const text = script.textContent || '';
                
                // 查找 render_data 或 __INITIAL_STATE__
                if (text.includes('window._ROUTER_DATA') || text.includes('RENDER_DATA')) {
                    const match = text.match(/window\._ROUTER_DATA\s*=\s*(\{[\s\S]*?\});?\s*(?:window\.|<\/script>|$)/);
                    if (match) {
                        const data = JSON.parse(match[1]);
                        videoData = findVideoInData(data);
                        if (videoData) break;
                    }
                }
                
                // SSR 数据
                if (text.includes('__INITIAL_STATE__')) {
                    const match = text.match(/__INITIAL_STATE__\s*=\s*(\{[\s\S]*?\});?\s*(?:window\.|<\/script>|$)/);
                    if (match) {
                        const data = JSON.parse(match[1]);
                        videoData = findVideoInData(data);
                        if (videoData) break;
                    }
                }
            }
        } catch (e) {
            console.warn('[DouyinExtractor] 解析脚本数据失败:', e);
        }
        
        // 方法2: 从全局变量提取
        if (!videoData) {
            try {
                if (window._ROUTER_DATA) {
                    videoData = findVideoInData(window._ROUTER_DATA);
                }
                if (!videoData && window.__INITIAL_STATE__) {
                    videoData = findVideoInData(window.__INITIAL_STATE__);
                }
            } catch (e) {
                console.warn('[DouyinExtractor] 读取全局变量失败:', e);
            }
        }
        
        return videoData;
    }
    
    // 递归查找视频数据
    function findVideoInData(obj, depth = 0) {
        if (depth > 10 || !obj || typeof obj !== 'object') return null;
        
        // 查找视频播放地址
        if (obj.video && obj.video.play_addr) {
            return {
                id: obj.aweme_id || obj.id,
                desc: obj.desc || '',
                playUrl: extractPlayUrl(obj.video.play_addr),
                coverUrl: obj.video.cover?.url_list?.[0] || '',
                author: obj.author?.nickname || ''
            };
        }
        
        // 查找 aweme_detail
        if (obj.aweme_detail) {
            return findVideoInData(obj.aweme_detail, depth + 1);
        }
        
        // 查找 aweme_list
        if (obj.aweme_list && Array.isArray(obj.aweme_list) && obj.aweme_list.length > 0) {
            return findVideoInData(obj.aweme_list[0], depth + 1);
        }
        
        // 递归搜索
        for (const key of Object.keys(obj)) {
            if (typeof obj[key] === 'object') {
                const result = findVideoInData(obj[key], depth + 1);
                if (result) return result;
            }
        }
        
        return null;
    }
    
    // 提取播放地址（去水印）
    function extractPlayUrl(playAddr) {
        if (!playAddr) return null;
        
        // 优先使用 url_list
        if (playAddr.url_list && playAddr.url_list.length > 0) {
            let url = playAddr.url_list[0];
            // 替换为无水印地址
            url = url.replace('playwm', 'play');
            url = url.replace(/watermark=\d+/, 'watermark=0');
            return url;
        }
        
        return null;
    }
    
    // 创建提取按钮
    function createExtractButton() {
        if (extractBtn) return extractBtn;
        
        extractBtn = document.createElement('div');
        extractBtn.id = 'wta-douyin-extract-btn';
        extractBtn.innerHTML = '🎬';
        extractBtn.style.cssText = `
            position: fixed;
            top: 50%;
            right: 16px;
            transform: translateY(-50%);
            width: 48px;
            height: 48px;
            border-radius: 50%;
            background: linear-gradient(135deg, #fe2c55 0%, #ff6b81 100%);
            color: white;
            display: none;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            cursor: pointer;
            z-index: 999999;
            box-shadow: 0 4px 15px rgba(254, 44, 85, 0.4);
            transition: transform 0.2s, opacity 0.3s;
            -webkit-tap-highlight-color: transparent;
        `;
        
        extractBtn.addEventListener('click', handleExtract);
        
        extractBtn.addEventListener('touchstart', () => {
            extractBtn.style.transform = 'translateY(-50%) scale(0.9)';
        }, { passive: true });
        
        extractBtn.addEventListener('touchend', () => {
            extractBtn.style.transform = 'translateY(-50%) scale(1)';
        }, { passive: true });
        
        document.body.appendChild(extractBtn);
        return extractBtn;
    }
    
    // 处理提取
    function handleExtract() {
        const videoData = extractVideoData();
        
        if (!videoData || !videoData.playUrl) {
            showToast('未找到视频地址，请稍后重试');
            console.warn('[DouyinExtractor] 未找到视频数据');
            return;
        }
        
        console.log('[DouyinExtractor] 提取到视频:', videoData);
        
        const url = videoData.playUrl;
        
        // 尝试通过 NativeBridge 下载
        if (typeof NativeBridge !== 'undefined' && NativeBridge.downloadVideo) {
            const filename = 'douyin_' + (videoData.id || Date.now()) + '.mp4';
            NativeBridge.downloadVideo(url, filename);
            showToast('开始下载无水印视频...');
        } else {
            // 降级：复制链接到剪贴板
            copyToClipboard(url);
            showToast('视频链接已复制到剪贴板');
            console.log('[DouyinExtractor] 无水印视频地址:', url);
        }
    }
    
    // 复制到剪贴板
    function copyToClipboard(text) {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).catch(() => {
                fallbackCopy(text);
            });
        } else {
            fallbackCopy(text);
        }
    }
    
    function fallbackCopy(text) {
        const textarea = document.createElement('textarea');
        textarea.value = text;
        textarea.style.cssText = 'position:fixed;opacity:0;';
        document.body.appendChild(textarea);
        textarea.select();
        document.execCommand('copy');
        document.body.removeChild(textarea);
    }
    
    // 显示提示
    function showToast(message) {
        const existing = document.getElementById('wta-douyin-toast');
        if (existing) existing.remove();
        
        const toast = document.createElement('div');
        toast.id = 'wta-douyin-toast';
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background: rgba(0,0,0,0.85);
            color: white;
            padding: 16px 28px;
            border-radius: 12px;
            font-size: 14px;
            z-index: 9999999;
            animation: wtaFadeIn 0.3s ease;
        `;
        
        if (!document.getElementById('wta-douyin-style')) {
            const style = document.createElement('style');
            style.id = 'wta-douyin-style';
            style.textContent = `
                @keyframes wtaFadeIn {
                    from { opacity: 0; transform: translate(-50%, -50%) scale(0.9); }
                    to { opacity: 1; transform: translate(-50%, -50%) scale(1); }
                }
            `;
            document.head.appendChild(style);
        }
        
        document.body.appendChild(toast);
        setTimeout(() => toast.remove(), 2500);
    }
    
    // 检测视频变化
    function detectVideoChange() {
        const video = document.querySelector('video');
        const btn = createExtractButton();
        
        if (video) {
            // 获取当前视频 ID（从 URL 或数据属性）
            const urlMatch = location.pathname.match(/\/video\/(\d+)/);
            const newVideoId = urlMatch ? urlMatch[1] : video.src;
            
            if (newVideoId !== currentVideoId) {
                currentVideoId = newVideoId;
                btn.style.display = 'flex';
                console.log('[DouyinExtractor] 检测到新视频:', currentVideoId);
            }
        } else {
            btn.style.display = 'none';
        }
    }
    
    // 防抖检测
    function debouncedDetect() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(detectVideoChange, DEBOUNCE_DELAY);
    }
    
    // 初始化
    function init() {
        // 检查是否在抖音
        if (!location.hostname.includes('douyin.com')) {
            console.log('[DouyinExtractor] 非抖音页面，跳过');
            return;
        }
        
        createExtractButton();
        detectVideoChange();
        
        // 监听 DOM 变化
        const observer = new MutationObserver(debouncedDetect);
        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
        
        // 监听滚动（短视频切换）
        window.addEventListener('scroll', debouncedDetect, { passive: true });
        
        // 监听 URL 变化
        let lastUrl = location.href;
        setInterval(() => {
            if (location.href !== lastUrl) {
                lastUrl = location.href;
                debouncedDetect();
            }
        }, 500);
        
        console.log('[DouyinExtractor] 模块已初始化');
    }
    
    // 启动
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
"""

    /**
     * 抖音视频提取器
     * 从页面数据中提取无水印视频地址
     */
    private fun douyinVideoExtractor() = ExtensionModule(
        id = "builtin-douyin-extractor",
        name = "抖音视频提取",
        description = "提取抖音网页版视频的无水印播放地址，支持复制链接或直接下载",
        icon = "🎬",
        category = ModuleCategory.MEDIA,
        tags = listOf("抖音", "douyin", "视频", "无水印"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD, ModulePermission.CLIPBOARD),
        urlMatches = listOf(
            UrlMatchRule("*://www.douyin.com/*"),
            UrlMatchRule("*://m.douyin.com/*")
        ),
        code = DOUYIN_EXTRACTOR_CODE.trimIndent()
    )
    
    // ==================== 小红书视频提取器 ====================
    
    private const val XIAOHONGSHU_EXTRACTOR_CODE = """
(function() {
    'use strict';
    
    const DEBOUNCE_DELAY = 600;
    let debounceTimer = null;
    let extractBtn = null;
    let currentNoteId = null;
    
    // 从页面数据提取视频信息
    function extractVideoData() {
        let videoData = null;
        
        // 方法1: 从 __INITIAL_STATE__ 提取
        try {
            if (window.__INITIAL_STATE__) {
                videoData = findVideoInState(window.__INITIAL_STATE__);
            }
        } catch (e) {
            console.warn('[XHSExtractor] 读取 __INITIAL_STATE__ 失败:', e);
        }
        
        // 方法2: 从 script 标签提取
        if (!videoData) {
            try {
                const scripts = document.querySelectorAll('script');
                for (const script of scripts) {
                    const text = script.textContent || '';
                    if (text.includes('__INITIAL_STATE__')) {
                        const match = text.match(/__INITIAL_STATE__\s*=\s*(\{[\s\S]*?\})\s*;?\s*(?:<\/script>|window\.)/);
                        if (match) {
                            const data = JSON.parse(match[1]);
                            videoData = findVideoInState(data);
                            if (videoData) break;
                        }
                    }
                }
            } catch (e) {
                console.warn('[XHSExtractor] 解析脚本失败:', e);
            }
        }
        
        return videoData;
    }
    
    // 在状态中查找视频
    function findVideoInState(state) {
        if (!state) return null;
        
        // 查找 note 数据
        const noteData = state.note?.noteDetailMap || state.note?.note || {};
        
        for (const key of Object.keys(noteData)) {
            const note = noteData[key]?.note || noteData[key];
            if (note && note.video) {
                const video = note.video;
                return {
                    id: note.noteId || note.id || key,
                    title: note.title || note.desc || '',
                    playUrl: extractBestUrl(video),
                    coverUrl: note.imageList?.[0]?.url || '',
                    author: note.user?.nickname || ''
                };
            }
        }
        
        return null;
    }
    
    // 提取最佳视频地址
    function extractBestUrl(video) {
        if (!video) return null;
        
        // 优先使用 media.stream
        if (video.media?.stream) {
            const streams = video.media.stream;
            // 选择最高画质
            const h264 = streams.h264 || streams.h265 || [];
            if (h264.length > 0) {
                const best = h264.sort((a, b) => (b.videoBitrate || 0) - (a.videoBitrate || 0))[0];
                if (best.masterUrl) return best.masterUrl;
            }
        }
        
        // 降级使用 consumer.originVideoKey
        if (video.consumer?.originVideoKey) {
            return 'https://sns-video-bd.xhscdn.com/' + video.consumer.originVideoKey;
        }
        
        return null;
    }
    
    // 创建提取按钮
    function createExtractButton() {
        if (extractBtn) return extractBtn;
        
        extractBtn = document.createElement('div');
        extractBtn.id = 'wta-xhs-extract-btn';
        extractBtn.innerHTML = '📱';
        extractBtn.style.cssText = `
            position: fixed;
            top: 50%;
            right: 16px;
            transform: translateY(-50%);
            width: 48px;
            height: 48px;
            border-radius: 50%;
            background: linear-gradient(135deg, #ff2442 0%, #ff6b7a 100%);
            color: white;
            display: none;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            cursor: pointer;
            z-index: 999999;
            box-shadow: 0 4px 15px rgba(255, 36, 66, 0.4);
            transition: transform 0.2s;
            -webkit-tap-highlight-color: transparent;
        `;
        
        extractBtn.addEventListener('click', handleExtract);
        
        extractBtn.addEventListener('touchstart', () => {
            extractBtn.style.transform = 'translateY(-50%) scale(0.9)';
        }, { passive: true });
        
        extractBtn.addEventListener('touchend', () => {
            extractBtn.style.transform = 'translateY(-50%) scale(1)';
        }, { passive: true });
        
        document.body.appendChild(extractBtn);
        return extractBtn;
    }
    
    // 处理提取
    function handleExtract() {
        const videoData = extractVideoData();
        
        if (!videoData || !videoData.playUrl) {
            showToast('未找到视频地址');
            return;
        }
        
        console.log('[XHSExtractor] 提取到视频:', videoData);
        
        if (typeof NativeBridge !== 'undefined' && NativeBridge.downloadVideo) {
            const filename = 'xiaohongshu_' + (videoData.id || Date.now()) + '.mp4';
            NativeBridge.downloadVideo(videoData.playUrl, filename);
            showToast('开始下载视频...');
        } else {
            copyToClipboard(videoData.playUrl);
            showToast('视频链接已复制');
        }
    }
    
    // 复制到剪贴板
    function copyToClipboard(text) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).catch(() => fallbackCopy(text));
        } else {
            fallbackCopy(text);
        }
    }
    
    function fallbackCopy(text) {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.cssText = 'position:fixed;opacity:0;';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
    }
    
    // 显示提示
    function showToast(msg) {
        const t = document.createElement('div');
        t.textContent = msg;
        t.style.cssText = `
            position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);
            background:rgba(0,0,0,0.85);color:white;padding:16px 28px;
            border-radius:12px;font-size:14px;z-index:9999999;
        `;
        document.body.appendChild(t);
        setTimeout(() => t.remove(), 2000);
    }
    
    // 检测视频
    function detectVideo() {
        const video = document.querySelector('video');
        const btn = createExtractButton();
        
        if (video) {
            const urlMatch = location.pathname.match(/\/explore\/([a-zA-Z0-9]+)/);
            const newId = urlMatch ? urlMatch[1] : 'unknown';
            
            if (newId !== currentNoteId) {
                currentNoteId = newId;
                btn.style.display = 'flex';
            }
        } else {
            btn.style.display = 'none';
        }
    }
    
    function debouncedDetect() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(detectVideo, DEBOUNCE_DELAY);
    }
    
    // 初始化
    function init() {
        if (!location.hostname.includes('xiaohongshu.com') && !location.hostname.includes('xhslink.com')) {
            return;
        }
        
        createExtractButton();
        detectVideo();
        
        const observer = new MutationObserver(debouncedDetect);
        observer.observe(document.body, { childList: true, subtree: true });
        
        window.addEventListener('scroll', debouncedDetect, { passive: true });
        
        console.log('[XHSExtractor] 模块已初始化');
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
"""

    /**
     * 小红书视频提取器
     */
    private fun xiaohongshuVideoExtractor() = ExtensionModule(
        id = "builtin-xiaohongshu-extractor",
        name = "小红书视频提取",
        description = "提取小红书网页版视频的播放地址，支持复制链接或直接下载",
        icon = "📱",
        category = ModuleCategory.MEDIA,
        tags = listOf("小红书", "视频", "下载"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD, ModulePermission.CLIPBOARD),
        urlMatches = listOf(
            UrlMatchRule("*://www.xiaohongshu.com/*"),
            UrlMatchRule("*://xhslink.com/*")
        ),
        code = XIAOHONGSHU_EXTRACTOR_CODE.trimIndent()
    )
    
    // ==================== 小红书图片下载器 ====================
    
    private const val XIAOHONGSHU_IMAGE_DOWNLOADER_CODE = """
(function() {
    'use strict';
    
    // 防止重复注入
    if (window.__wtaXhsImageDownloader) return;
    window.__wtaXhsImageDownloader = true;
    
    console.log('[XhsImageDownloader] 初始化小红书图片下载器');
    
    let imagePanel = null;
    let currentImages = [];
    let downloadBtn = null;
    
    // ========== 1. 绕过长按事件阻止 ==========
    function bypassLongPressBlock() {
        var eventsToBlock = ['contextmenu', 'touchstart', 'touchmove', 'touchend'];
        
        eventsToBlock.forEach(function(eventType) {
            document.addEventListener(eventType, function(e) {
                var target = e.target;
                var isImageRelated = false;
                var current = target;
                var depth = 0;
                
                while (current && depth < 10) {
                    var tagName = current.tagName ? current.tagName.toUpperCase() : '';
                    if (tagName === 'IMG' || tagName === 'CANVAS') {
                        isImageRelated = true;
                        break;
                    }
                    var style = window.getComputedStyle(current);
                    if (style.backgroundImage && style.backgroundImage !== 'none' && 
                        style.backgroundImage.includes('url(')) {
                        isImageRelated = true;
                        break;
                    }
                    if (current.className && typeof current.className === 'string' && (
                        current.className.includes('note-image') ||
                        current.className.includes('swiper') ||
                        current.className.includes('carousel') ||
                        current.className.includes('slide') ||
                        current.className.includes('image')
                    )) {
                        isImageRelated = true;
                        break;
                    }
                    current = current.parentElement;
                    depth++;
                }
                
                if (isImageRelated) {
                    e.stopPropagation();
                }
            }, true);
        });
        
        function removeEventBlockers() {
            var elements = document.querySelectorAll('img, canvas, [class*="image"], [class*="swiper"], [class*="carousel"], [class*="slide"]');
            elements.forEach(function(el) {
                el.style.webkitTouchCallout = 'default';
                el.style.webkitUserSelect = 'auto';
                el.style.userSelect = 'auto';
                el.style.pointerEvents = 'auto';
                el.removeAttribute('oncontextmenu');
                el.removeAttribute('ontouchstart');
            });
        }
        
        removeEventBlockers();
        var observer = new MutationObserver(removeEventBlockers);
        observer.observe(document.body, { childList: true, subtree: true });
        
        console.log('[XhsImageDownloader] 长按阻止已绕过');
    }
    
    // ========== 2. 提取页面所有图片 ==========
    function extractAllImages() {
        var images = new Set();
        
        document.querySelectorAll('img').forEach(function(img) {
            var src = img.src || img.dataset.src || img.getAttribute('data-lazy-src');
            if (src && isValidImageUrl(src)) {
                images.add(getHighResUrl(src));
            }
        });
        
        document.querySelectorAll('*').forEach(function(el) {
            var style = window.getComputedStyle(el);
            var bgImage = style.backgroundImage;
            if (bgImage && bgImage !== 'none') {
                var matches = bgImage.match(/url\(['"]?([^'")\s]+)['"]?\)/g);
                if (matches) {
                    matches.forEach(function(match) {
                        var url = match.replace(/url\(['"]?/, '').replace(/['"]?\)/, '');
                        if (isValidImageUrl(url)) {
                            images.add(getHighResUrl(url));
                        }
                    });
                }
            }
        });
        
        var xhsSelectors = [
            '[class*="note-image"] img',
            '[class*="swiper-slide"] img',
            '[class*="carousel"] img',
            '[class*="media-container"] img',
            '.note-content img',
            '.feed-card img',
            '[data-v-] img'
        ];
        
        xhsSelectors.forEach(function(selector) {
            try {
                document.querySelectorAll(selector).forEach(function(img) {
                    var src = img.src || img.dataset.src;
                    if (src && isValidImageUrl(src)) {
                        images.add(getHighResUrl(src));
                    }
                });
            } catch (e) {}
        });
        
        try {
            var scripts = document.querySelectorAll('script');
            scripts.forEach(function(script) {
                var text = script.textContent || '';
                var urlMatches = text.match(/https?:\/\/[^"'\s]*(?:xhscdn|xiaohongshu)[^"'\s]*\.(?:jpg|jpeg|png|webp|gif)[^"'\s]*/gi);
                if (urlMatches) {
                    urlMatches.forEach(function(url) {
                        if (isValidImageUrl(url)) {
                            images.add(getHighResUrl(url));
                        }
                    });
                }
            });
        } catch (e) {}
        
        return Array.from(images);
    }
    
    function isValidImageUrl(url) {
        if (!url) return false;
        if (url.startsWith('data:image/svg')) return false;
        if (url.includes('avatar') || url.includes('icon') || url.includes('logo')) return false;
        if (url.includes('loading') || url.includes('placeholder')) return false;
        if (url.includes('xhscdn') || url.includes('xiaohongshu')) return true;
        if (url.match(/\.(jpg|jpeg|png|webp|gif)(\?|#|$)/i)) return true;
        return false;
    }
    
    function getHighResUrl(url) {
        if (!url) return url;
        url = url.replace(/\?imageView2\/\d+\/w\/\d+\/format\/\w+/i, '');
        url = url.replace(/\?x-oss-process=[^&]+/i, '');
        url = url.replace(/!nd_dft_[^!]+/i, '');
        url = url.replace(/\/\d+x\d+\//i, '/');
        url = url.replace(/^http:/, 'https:');
        return url;
    }
    
    // ========== 3. 创建下载按钮 ==========
    function createDownloadButton() {
        if (downloadBtn) return downloadBtn;
        
        downloadBtn = document.createElement('div');
        downloadBtn.id = 'wta-xhs-download-btn';
        downloadBtn.innerHTML = '🖼️';
        downloadBtn.style.cssText = 
            'position: fixed; bottom: 140px; right: 20px; width: 56px; height: 56px;' +
            'border-radius: 50%; background: linear-gradient(135deg, #ff2442 0%, #ff6b81 100%);' +
            'color: white; display: flex; align-items: center; justify-content: center;' +
            'font-size: 24px; cursor: pointer; z-index: 999999;' +
            'box-shadow: 0 4px 15px rgba(255, 36, 66, 0.4); transition: transform 0.2s;';
        
        downloadBtn.addEventListener('click', function() {
            currentImages = extractAllImages();
            if (currentImages.length > 0) {
                showImagePanel();
            } else {
                showToast('未找到可下载的图片');
            }
        });
        
        downloadBtn.addEventListener('touchstart', function() {
            downloadBtn.style.transform = 'scale(0.95)';
        }, { passive: true });
        
        downloadBtn.addEventListener('touchend', function() {
            downloadBtn.style.transform = 'scale(1)';
        }, { passive: true });
        
        document.body.appendChild(downloadBtn);
        return downloadBtn;
    }
    
    // ========== 4. 创建图片选择面板 ==========
    function showImagePanel() {
        if (imagePanel) imagePanel.remove();
        
        imagePanel = document.createElement('div');
        imagePanel.id = 'wta-xhs-image-panel';
        imagePanel.style.cssText = 
            'position: fixed; bottom: 0; left: 0; right: 0; max-height: 70vh;' +
            'background: rgba(30, 30, 30, 0.98); border-radius: 20px 20px 0 0;' +
            'z-index: 9999999; display: flex; flex-direction: column;' +
            'animation: wtaSlideUp 0.3s ease; backdrop-filter: blur(10px);';
        
        if (!document.getElementById('wta-xhs-style')) {
            var style = document.createElement('style');
            style.id = 'wta-xhs-style';
            style.textContent = 
                '@keyframes wtaSlideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }' +
                '.wta-img-item { position: relative; border-radius: 8px; overflow: hidden; cursor: pointer; transition: transform 0.2s; }' +
                '.wta-img-item:active { transform: scale(0.95); }' +
                '.wta-img-item img { width: 100%; height: 100%; object-fit: cover; }' +
                '.wta-img-item .wta-check { position: absolute; top: 8px; right: 8px; width: 24px; height: 24px;' +
                '  border-radius: 50%; background: rgba(255,255,255,0.3); border: 2px solid white;' +
                '  display: flex; align-items: center; justify-content: center; font-size: 14px; }' +
                '.wta-img-item.selected .wta-check { background: #ff2442; border-color: #ff2442; }';
            document.head.appendChild(style);
        }
        
        var header = document.createElement('div');
        header.style.cssText = 'display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid rgba(255,255,255,0.1);';
        header.innerHTML = 
            '<div style="color: white; font-size: 16px; font-weight: 600;">选择图片 (' + currentImages.length + ')</div>' +
            '<div style="display: flex; gap: 12px;">' +
            '  <button id="wta-select-all" style="background: #444; color: white; border: none; padding: 8px 16px; border-radius: 20px; font-size: 13px; cursor: pointer;">全选</button>' +
            '  <button id="wta-close-panel" style="background: none; border: none; color: #888; font-size: 24px; cursor: pointer;">×</button>' +
            '</div>';
        imagePanel.appendChild(header);
        
        var grid = document.createElement('div');
        grid.style.cssText = 'display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 16px; overflow-y: auto; max-height: calc(70vh - 140px);';
        
        var selectedImages = new Set();
        
        currentImages.forEach(function(url) {
            var item = document.createElement('div');
            item.className = 'wta-img-item';
            item.dataset.url = url;
            item.style.cssText = 'aspect-ratio: 1;';
            item.innerHTML = '<img src="' + url + '" loading="lazy" onerror="this.parentElement.style.display=\'none\'"><div class="wta-check">✓</div>';
            
            item.addEventListener('click', function() {
                if (selectedImages.has(url)) {
                    selectedImages.delete(url);
                    item.classList.remove('selected');
                } else {
                    selectedImages.add(url);
                    item.classList.add('selected');
                }
                updateDownloadButton();
            });
            
            grid.appendChild(item);
        });
        
        imagePanel.appendChild(grid);
        
        var footer = document.createElement('div');
        footer.style.cssText = 'padding: 16px 20px; border-top: 1px solid rgba(255,255,255,0.1); background: rgba(30, 30, 30, 0.98);';
        footer.innerHTML = '<button id="wta-download-selected" style="width: 100%; background: linear-gradient(135deg, #ff2442 0%, #ff6b81 100%); color: white; border: none; padding: 14px; border-radius: 12px; font-size: 16px; font-weight: 600; cursor: pointer;">下载选中 (0)</button>';
        imagePanel.appendChild(footer);
        
        document.body.appendChild(imagePanel);
        
        document.getElementById('wta-close-panel').addEventListener('click', function() {
            imagePanel.remove();
            imagePanel = null;
        });
        
        document.getElementById('wta-select-all').addEventListener('click', function() {
            var items = grid.querySelectorAll('.wta-img-item');
            var allSelected = selectedImages.size === currentImages.length;
            
            if (allSelected) {
                selectedImages.clear();
                items.forEach(function(item) { item.classList.remove('selected'); });
            } else {
                currentImages.forEach(function(url) { selectedImages.add(url); });
                items.forEach(function(item) { item.classList.add('selected'); });
            }
            updateDownloadButton();
        });
        
        document.getElementById('wta-download-selected').addEventListener('click', function() {
            if (selectedImages.size === 0) {
                showToast('请先选择图片');
                return;
            }
            downloadImages(Array.from(selectedImages));
        });
        
        function updateDownloadButton() {
            var btn = document.getElementById('wta-download-selected');
            btn.textContent = '下载选中 (' + selectedImages.size + ')';
        }
    }
    
    // ========== 5. 下载图片 ==========
    function downloadImages(urls) {
        var total = urls.length;
        var completed = 0;
        var failed = 0;
        
        showToast('开始下载 ' + total + ' 张图片...');
        
        urls.forEach(function(url, index) {
            setTimeout(function() {
                downloadSingleImage(url, function(success) {
                    if (success) completed++; else failed++;
                    
                    if (completed + failed === total) {
                        if (failed === 0) {
                            showToast('全部 ' + total + ' 张图片下载完成！');
                        } else {
                            showToast('下载完成：成功 ' + completed + ' 张，失败 ' + failed + ' 张');
                        }
                        if (imagePanel) { imagePanel.remove(); imagePanel = null; }
                    }
                });
            }, index * 500);
        });
    }
    
    function downloadSingleImage(url, callback) {
        var filename = 'xhs_' + Date.now() + '_' + Math.random().toString(36).substr(2, 6) + '.jpg';
        
        if (typeof NativeBridge !== 'undefined' && NativeBridge.saveImageToGallery) {
            try {
                NativeBridge.saveImageToGallery(url, filename);
                callback(true);
                return;
            } catch (e) {}
        }
        
        fetch(url, { mode: 'cors' })
            .then(function(response) { return response.blob(); })
            .then(function(blob) {
                var a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = filename;
                a.style.display = 'none';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                URL.revokeObjectURL(a.href);
                callback(true);
            })
            .catch(function(e) { callback(false); });
    }
    
    // ========== 6. 长按保存单张图片 ==========
    function enableLongPressSave() {
        var longPressTimer = null;
        var longPressTarget = null;
        
        document.addEventListener('touchstart', function(e) {
            var target = e.target;
            var imageUrl = findImageUrl(target);
            
            if (imageUrl) {
                longPressTarget = { element: target, url: imageUrl };
                longPressTimer = setTimeout(function() {
                    showSingleImageMenu(longPressTarget.url, e.touches[0]);
                }, 500);
            }
        }, { passive: true });
        
        document.addEventListener('touchmove', function() {
            clearTimeout(longPressTimer);
            longPressTarget = null;
        }, { passive: true });
        
        document.addEventListener('touchend', function() {
            clearTimeout(longPressTimer);
            longPressTarget = null;
        }, { passive: true });
    }
    
    function findImageUrl(element) {
        var current = element;
        var depth = 0;
        
        while (current && depth < 10) {
            if (current.tagName === 'IMG' && current.src) {
                return getHighResUrl(current.src);
            }
            
            var style = window.getComputedStyle(current);
            var bgImage = style.backgroundImage;
            if (bgImage && bgImage !== 'none' && bgImage.includes('url(')) {
                var match = bgImage.match(/url\(['"]?([^'")\s]+)['"]?\)/);
                if (match && isValidImageUrl(match[1])) {
                    return getHighResUrl(match[1]);
                }
            }
            
            var img = current.querySelector('img');
            if (img && img.src && isValidImageUrl(img.src)) {
                return getHighResUrl(img.src);
            }
            
            current = current.parentElement;
            depth++;
        }
        
        return null;
    }
    
    function showSingleImageMenu(url, touch) {
        var existing = document.getElementById('wta-single-image-menu');
        if (existing) existing.remove();
        
        var menu = document.createElement('div');
        menu.id = 'wta-single-image-menu';
        menu.style.cssText = 
            'position: fixed; top: ' + Math.min(touch.clientY, window.innerHeight - 150) + 'px;' +
            'left: ' + Math.min(touch.clientX - 75, window.innerWidth - 160) + 'px;' +
            'background: rgba(40, 40, 40, 0.98); border-radius: 12px; padding: 8px 0;' +
            'z-index: 99999999; min-width: 150px; box-shadow: 0 8px 32px rgba(0,0,0,0.4);';
        
        menu.innerHTML = 
            '<div class="wta-menu-item" data-action="save" style="padding: 12px 20px; color: white; cursor: pointer; display: flex; align-items: center; gap: 10px;">' +
            '  <span>💾</span><span>保存图片</span></div>' +
            '<div class="wta-menu-item" data-action="copy" style="padding: 12px 20px; color: white; cursor: pointer; display: flex; align-items: center; gap: 10px;">' +
            '  <span>📋</span><span>复制链接</span></div>';
        
        document.body.appendChild(menu);
        
        menu.querySelectorAll('.wta-menu-item').forEach(function(item) {
            item.addEventListener('click', function() {
                var action = item.dataset.action;
                if (action === 'save') {
                    downloadSingleImage(url, function(success) {
                        showToast(success ? '图片已保存' : '保存失败');
                    });
                } else if (action === 'copy') {
                    copyToClipboard(url);
                    showToast('链接已复制');
                }
                menu.remove();
            });
        });
        
        setTimeout(function() {
            document.addEventListener('click', function closeMenu(e) {
                if (!menu.contains(e.target)) {
                    menu.remove();
                    document.removeEventListener('click', closeMenu);
                }
            });
        }, 100);
    }
    
    function showToast(message) {
        var existing = document.getElementById('wta-xhs-toast');
        if (existing) existing.remove();
        
        var toast = document.createElement('div');
        toast.id = 'wta-xhs-toast';
        toast.textContent = message;
        toast.style.cssText = 'position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%);' +
            'background: rgba(0,0,0,0.85); color: white; padding: 14px 28px; border-radius: 12px;' +
            'font-size: 14px; z-index: 999999999;';
        document.body.appendChild(toast);
        setTimeout(function() { toast.remove(); }, 2500);
    }
    
    function copyToClipboard(text) {
        if (typeof NativeBridge !== 'undefined' && NativeBridge.copyToClipboard) {
            NativeBridge.copyToClipboard(text);
            return;
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).catch(function() { fallbackCopy(text); });
        } else {
            fallbackCopy(text);
        }
    }
    
    function fallbackCopy(text) {
        var ta = document.createElement('textarea');
        ta.value = text;
        ta.style.cssText = 'position:fixed;opacity:0;';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
    }
    
    function init() {
        if (!location.hostname.includes('xiaohongshu.com') && !location.hostname.includes('xhslink.com')) {
            return;
        }
        
        bypassLongPressBlock();
        createDownloadButton();
        enableLongPressSave();
        
        console.log('[XhsImageDownloader] 小红书图片下载器已初始化');
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        setTimeout(init, 500);
    }
})();
"""

    /**
     * 小红书图片下载器
     * 绕过长按限制，支持批量下载图片
     */
    private fun xiaohongshuImageDownloader() = ExtensionModule(
        id = "builtin-xiaohongshu-image-downloader",
        name = "小红书图片下载",
        description = "绕过小红书长按限制，支持长按保存单张图片或批量下载笔记中的所有图片",
        icon = "🖼️",
        category = ModuleCategory.MEDIA,
        tags = listOf("小红书", "图片", "下载", "批量"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD, ModulePermission.CLIPBOARD),
        urlMatches = listOf(
            UrlMatchRule("*://www.xiaohongshu.com/*"),
            UrlMatchRule("*://xhslink.com/*")
        ),
        code = XIAOHONGSHU_IMAGE_DOWNLOADER_CODE.trimIndent()
    )
    
    // ==================== 通用视频增强模块 ====================
    
    private const val VIDEO_ENHANCE_CODE = """
(function() {
    'use strict';
    
    let controlPanel = null;
    let currentVideo = null;
    let currentSpeed = 1.0;
    
    // ========== 1. 后台播放 - 修改 Page Visibility API ==========
    function enableBackgroundPlay() {
        // 覆盖 document.hidden
        Object.defineProperty(document, 'hidden', {
            get: () => false,
            configurable: true
        });
        
        // 覆盖 document.visibilityState
        Object.defineProperty(document, 'visibilityState', {
            get: () => 'visible',
            configurable: true
        });
        
        // 阻止 visibilitychange 事件
        document.addEventListener('visibilitychange', (e) => {
            e.stopImmediatePropagation();
        }, true);
        
        // 阻止 pagehide 事件
        window.addEventListener('pagehide', (e) => {
            e.stopImmediatePropagation();
        }, true);
        
        // 阻止 blur 事件导致的暂停
        window.addEventListener('blur', (e) => {
            e.stopImmediatePropagation();
        }, true);
        
        console.log('[VideoEnhance] 后台播放已启用');
    }
    
    // ========== 2. 阻止"打开App"遮罩 ==========
    function blockAppOpenOverlay() {
        // 通用遮罩选择器
        const overlaySelectors = [
            '[class*="open-app"]',
            '[class*="openapp"]',
            '[class*="download-app"]',
            '[class*="app-download"]',
            '[class*="guide-app"]',
            '[class*="app-guide"]',
            '[class*="modal-mask"]',
            '[class*="open-in-app"]',
            '[id*="open-app"]',
            '[id*="download-app"]',
            '.open-app-btn',
            '.download-guide',
            '.app-banner',
            // 抖音
            '[class*="login-guide"]',
            '[class*="guide-modal"]',
            // 知乎
            '.OpenInAppButton',
            '.AppBanner',
            '.ModalWrap',
            // 贴吧
            '.tb-open-app',
            '.open-tieba-app'
        ];
        
        function removeOverlays() {
            overlaySelectors.forEach(selector => {
                document.querySelectorAll(selector).forEach(el => {
                    el.style.display = 'none';
                    el.style.visibility = 'hidden';
                    el.style.opacity = '0';
                    el.style.pointerEvents = 'none';
                });
            });
            
            // 恢复页面滚动
            document.body.style.overflow = '';
            document.documentElement.style.overflow = '';
        }
        
        // 初始移除
        removeOverlays();
        
        // 监听 DOM 变化持续移除
        const observer = new MutationObserver(removeOverlays);
        observer.observe(document.body, { childList: true, subtree: true });
        
        // 阻止跳转到 App Store
        const originalOpen = window.open;
        window.open = function(url) {
            if (url && (url.includes('app.') || url.includes('itunes.apple') || 
                        url.includes('play.google') || url.includes('://apps.'))) {
                console.log('[VideoEnhance] 阻止跳转:', url);
                return null;
            }
            return originalOpen.apply(this, arguments);
        };
        
        console.log('[VideoEnhance] 遮罩拦截已启用');
    }
    
    // ========== 3. 创建控制面板 ==========
    function createControlPanel() {
        if (controlPanel) return controlPanel;
        
        controlPanel = document.createElement('div');
        controlPanel.id = 'wta-video-enhance-panel';
        controlPanel.innerHTML = `
            <style>
                #wta-video-enhance-panel {
                    position: fixed;
                    bottom: 140px;
                    right: 16px;
                    background: rgba(20, 20, 20, 0.95);
                    border-radius: 16px;
                    padding: 12px;
                    z-index: 999999;
                    display: none;
                    flex-direction: column;
                    gap: 8px;
                    min-width: 160px;
                    backdrop-filter: blur(10px);
                    box-shadow: 0 8px 32px rgba(0,0,0,0.4);
                    font-family: -apple-system, sans-serif;
                }
                #wta-video-enhance-panel .wta-title {
                    color: #fff;
                    font-size: 12px;
                    font-weight: 600;
                    margin-bottom: 4px;
                    text-align: center;
                }
                #wta-video-enhance-panel .wta-speed-display {
                    color: #4fc3f7;
                    font-size: 24px;
                    font-weight: bold;
                    text-align: center;
                    margin: 8px 0;
                }
                #wta-video-enhance-panel .wta-slider-container {
                    padding: 0 8px;
                }
                #wta-video-enhance-panel input[type="range"] {
                    width: 100%;
                    height: 6px;
                    -webkit-appearance: none;
                    background: linear-gradient(to right, #4fc3f7 0%, #4fc3f7 var(--progress), #444 var(--progress), #444 100%);
                    border-radius: 3px;
                    outline: none;
                }
                #wta-video-enhance-panel input[type="range"]::-webkit-slider-thumb {
                    -webkit-appearance: none;
                    width: 20px;
                    height: 20px;
                    border-radius: 50%;
                    background: #fff;
                    cursor: pointer;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.3);
                }
                #wta-video-enhance-panel .wta-speed-presets {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 6px;
                    justify-content: center;
                    margin-top: 8px;
                }
                #wta-video-enhance-panel .wta-preset-btn {
                    background: #333;
                    color: #fff;
                    border: none;
                    padding: 6px 10px;
                    border-radius: 6px;
                    font-size: 12px;
                    cursor: pointer;
                    transition: background 0.2s;
                }
                #wta-video-enhance-panel .wta-preset-btn:active {
                    background: #4fc3f7;
                }
                #wta-video-enhance-panel .wta-action-btn {
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    border: none;
                    padding: 10px;
                    border-radius: 8px;
                    font-size: 13px;
                    cursor: pointer;
                    margin-top: 4px;
                    transition: transform 0.2s;
                }
                #wta-video-enhance-panel .wta-action-btn:active {
                    transform: scale(0.95);
                }
            </style>
            <div class="wta-title">🎬 视频增强</div>
            <div class="wta-speed-display" id="wta-speed-value">1.0x</div>
            <div class="wta-slider-container">
                <input type="range" id="wta-speed-slider" min="0.5" max="5" step="0.1" value="1" style="--progress: 11%;">
            </div>
            <div class="wta-speed-presets">
                <button class="wta-preset-btn" data-speed="0.5">0.5x</button>
                <button class="wta-preset-btn" data-speed="1">1x</button>
                <button class="wta-preset-btn" data-speed="1.5">1.5x</button>
                <button class="wta-preset-btn" data-speed="2">2x</button>
                <button class="wta-preset-btn" data-speed="3">3x</button>
                <button class="wta-preset-btn" data-speed="5">5x</button>
            </div>
            <button class="wta-action-btn" id="wta-pip-btn">📺 画中画</button>
        `;
        
        document.body.appendChild(controlPanel);
        
        // 绑定滑块事件
        const slider = controlPanel.querySelector('#wta-speed-slider');
        const speedDisplay = controlPanel.querySelector('#wta-speed-value');
        
        slider.addEventListener('input', (e) => {
            const speed = parseFloat(e.target.value);
            setPlaybackSpeed(speed);
            speedDisplay.textContent = speed.toFixed(1) + 'x';
            // 更新滑块进度颜色
            const progress = ((speed - 0.5) / 4.5) * 100;
            slider.style.setProperty('--progress', progress + '%');
        });
        
        // 绑定预设按钮
        controlPanel.querySelectorAll('.wta-preset-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const speed = parseFloat(btn.dataset.speed);
                slider.value = speed;
                setPlaybackSpeed(speed);
                speedDisplay.textContent = speed.toFixed(1) + 'x';
                const progress = ((speed - 0.5) / 4.5) * 100;
                slider.style.setProperty('--progress', progress + '%');
            });
        });
        
        // 画中画按钮
        controlPanel.querySelector('#wta-pip-btn').addEventListener('click', togglePictureInPicture);
        
        return controlPanel;
    }
    
    // ========== 4. 设置播放速度 ==========
    function setPlaybackSpeed(speed) {
        currentSpeed = speed;
        document.querySelectorAll('video').forEach(video => {
            video.playbackRate = speed;
        });
        console.log('[VideoEnhance] 播放速度:', speed + 'x');
    }
    
    // ========== 5. 画中画模式 ==========
    async function togglePictureInPicture() {
        const video = document.querySelector('video');
        if (!video) {
            showToast('未找到视频');
            return;
        }
        
        try {
            if (document.pictureInPictureElement) {
                await document.exitPictureInPicture();
                showToast('已退出画中画');
            } else if (document.pictureInPictureEnabled) {
                await video.requestPictureInPicture();
                showToast('已进入画中画');
            } else {
                showToast('浏览器不支持画中画');
            }
        } catch (e) {
            console.error('[VideoEnhance] 画中画错误:', e);
            showToast('画中画启动失败');
        }
    }
    
    // ========== 6. 创建触发按钮 ==========
    function createTriggerButton() {
        const btn = document.createElement('div');
        btn.id = 'wta-video-enhance-btn';
        btn.innerHTML = '⚡';
        btn.style.cssText = `
            position: fixed;
            bottom: 80px;
            right: 16px;
            width: 52px;
            height: 52px;
            border-radius: 50%;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            display: none;
            align-items: center;
            justify-content: center;
            font-size: 22px;
            cursor: pointer;
            z-index: 999998;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
            transition: transform 0.2s;
            -webkit-tap-highlight-color: transparent;
        `;
        
        btn.addEventListener('click', () => {
            const panel = createControlPanel();
            panel.style.display = panel.style.display === 'flex' ? 'none' : 'flex';
        });
        
        btn.addEventListener('touchstart', () => {
            btn.style.transform = 'scale(0.9)';
        }, { passive: true });
        
        btn.addEventListener('touchend', () => {
            btn.style.transform = 'scale(1)';
        }, { passive: true });
        
        document.body.appendChild(btn);
        return btn;
    }
    
    // 显示提示
    function showToast(msg) {
        const t = document.createElement('div');
        t.textContent = msg;
        t.style.cssText = `
            position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);
            background:rgba(0,0,0,0.85);color:white;padding:14px 24px;
            border-radius:10px;font-size:14px;z-index:9999999;
        `;
        document.body.appendChild(t);
        setTimeout(() => t.remove(), 1800);
    }
    
    // 检测视频
    function detectVideo() {
        const video = document.querySelector('video');
        const btn = document.getElementById('wta-video-enhance-btn');
        
        if (video && btn) {
            btn.style.display = 'flex';
            // 应用当前速度
            if (currentSpeed !== 1.0) {
                video.playbackRate = currentSpeed;
            }
        } else if (btn) {
            btn.style.display = 'none';
        }
    }
    
    // 初始化
    function init() {
        // 启用后台播放
        enableBackgroundPlay();
        
        // 阻止打开App遮罩
        blockAppOpenOverlay();
        
        // 创建UI
        createTriggerButton();
        createControlPanel();
        
        // 检测视频
        detectVideo();
        
        // 监听 DOM 变化
        const observer = new MutationObserver(() => {
            detectVideo();
        });
        observer.observe(document.body, { childList: true, subtree: true });
        
        // 点击外部关闭面板
        document.addEventListener('click', (e) => {
            if (controlPanel && controlPanel.style.display === 'flex') {
                const btn = document.getElementById('wta-video-enhance-btn');
                if (!controlPanel.contains(e.target) && e.target !== btn) {
                    controlPanel.style.display = 'none';
                }
            }
        });
        
        console.log('[VideoEnhance] 模块已初始化');
    }
    
    // 启动
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
"""

    /**
     * 通用视频增强模块
     * 倍速控制、画中画、后台播放、阻止App跳转
     */
    private fun videoEnhancer() = ExtensionModule(
        id = "builtin-video-enhancer",
        name = "视频增强",
        description = "强制倍速(0.5x-5x)、画中画、后台播放、阻止打开App遮罩",
        icon = "⚡",
        category = ModuleCategory.MEDIA,
        tags = listOf("视频", "倍速", "画中画", "后台播放"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.MEDIA),
        code = VIDEO_ENHANCE_CODE.trimIndent()
    )
    
    // ==================== 网页分析工具 ====================
    
    private const val WEB_ANALYZER_CODE = """
(function() {
    'use strict';
    
    let mainPanel = null;
    let isInspectMode = false;
    let highlightOverlay = null;
    let networkRequests = [];
    let consoleLogs = [];
    let currentTab = 'inspect';
    
    // ========== 样式 ==========
    const STYLES = `
        #wta-analyzer-panel {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            height: 45vh;
            background: #1e1e1e;
            z-index: 9999999;
            display: none;
            flex-direction: column;
            font-family: 'SF Mono', Consolas, monospace;
            font-size: 12px;
            color: #d4d4d4;
            box-shadow: 0 -4px 20px rgba(0,0,0,0.5);
        }
        #wta-analyzer-panel.show { display: flex; }
        .wta-tabs {
            display: flex;
            background: #252526;
            border-bottom: 1px solid #3c3c3c;
        }
        .wta-tab {
            padding: 10px 16px;
            cursor: pointer;
            border-bottom: 2px solid transparent;
            color: #808080;
            transition: all 0.2s;
        }
        .wta-tab.active {
            color: #fff;
            border-bottom-color: #007acc;
            background: #1e1e1e;
        }
        .wta-tab-content {
            flex: 1;
            overflow: auto;
            padding: 12px;
            display: none;
        }
        .wta-tab-content.active { display: block; }
        .wta-close-btn {
            position: absolute;
            top: 8px;
            right: 12px;
            background: none;
            border: none;
            color: #808080;
            font-size: 18px;
            cursor: pointer;
        }
        .wta-close-btn:hover { color: #fff; }
        .wta-request-item {
            padding: 8px;
            border-bottom: 1px solid #3c3c3c;
            cursor: pointer;
        }
        .wta-request-item:hover { background: #2d2d2d; }
        .wta-method { 
            display: inline-block;
            padding: 2px 6px;
            border-radius: 3px;
            font-size: 10px;
            margin-right: 8px;
        }
        .wta-method.GET { background: #4caf50; color: #fff; }
        .wta-method.POST { background: #ff9800; color: #fff; }
        .wta-method.PUT { background: #2196f3; color: #fff; }
        .wta-method.DELETE { background: #f44336; color: #fff; }
        .wta-url { color: #9cdcfe; word-break: break-all; }
        .wta-status { margin-left: 8px; }
        .wta-status.ok { color: #4caf50; }
        .wta-status.error { color: #f44336; }
        .wta-cookie-item {
            display: flex;
            justify-content: space-between;
            padding: 8px;
            border-bottom: 1px solid #3c3c3c;
        }
        .wta-cookie-name { color: #dcdcaa; }
        .wta-cookie-value { color: #ce9178; max-width: 60%; overflow: hidden; text-overflow: ellipsis; }
        .wta-btn {
            background: #0e639c;
            color: white;
            border: none;
            padding: 6px 12px;
            border-radius: 4px;
            cursor: pointer;
            margin: 4px;
        }
        .wta-btn:hover { background: #1177bb; }
        .wta-btn.danger { background: #c42b1c; }
        .wta-console-input {
            display: flex;
            padding: 8px;
            background: #252526;
            border-top: 1px solid #3c3c3c;
        }
        .wta-console-input input {
            flex: 1;
            background: #3c3c3c;
            border: none;
            color: #fff;
            padding: 8px;
            border-radius: 4px;
            font-family: inherit;
        }
        .wta-log-item { padding: 4px 8px; border-bottom: 1px solid #2d2d2d; }
        .wta-log-item.log { color: #d4d4d4; }
        .wta-log-item.warn { color: #dcdcaa; background: rgba(255,200,0,0.1); }
        .wta-log-item.error { color: #f48771; background: rgba(255,0,0,0.1); }
        .wta-element-info { background: #252526; padding: 12px; border-radius: 8px; margin-top: 8px; }
        .wta-element-info pre { margin: 0; white-space: pre-wrap; word-break: break-all; }
        .wta-highlight {
            position: fixed;
            pointer-events: none;
            background: rgba(0, 122, 204, 0.3);
            border: 2px solid #007acc;
            z-index: 9999998;
        }
        #wta-analyzer-btn {
            position: fixed;
            bottom: 80px;
            left: 16px;
            width: 52px;
            height: 52px;
            border-radius: 50%;
            background: linear-gradient(135deg, #00b4d8 0%, #0077b6 100%);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 22px;
            cursor: pointer;
            z-index: 999998;
            box-shadow: 0 4px 15px rgba(0, 180, 216, 0.4);
        }
    `;
    
    // ========== 1. 网络请求拦截 ==========
    function interceptNetwork() {
        // 拦截 XMLHttpRequest
        const originalXHROpen = XMLHttpRequest.prototype.open;
        const originalXHRSend = XMLHttpRequest.prototype.send;
        
        XMLHttpRequest.prototype.open = function(method, url) {
            this._wtaMethod = method;
            this._wtaUrl = url;
            this._wtaStartTime = Date.now();
            return originalXHROpen.apply(this, arguments);
        };
        
        XMLHttpRequest.prototype.send = function() {
            this.addEventListener('loadend', () => {
                networkRequests.unshift({
                    type: 'XHR',
                    method: this._wtaMethod,
                    url: this._wtaUrl,
                    status: this.status,
                    time: Date.now() - this._wtaStartTime,
                    response: this.responseText?.substring(0, 500)
                });
                if (networkRequests.length > 100) networkRequests.pop();
                updateNetworkTab();
            });
            return originalXHRSend.apply(this, arguments);
        };
        
        // 拦截 fetch
        const originalFetch = window.fetch;
        window.fetch = function(url, options = {}) {
            const startTime = Date.now();
            const method = options.method || 'GET';
            
            return originalFetch.apply(this, arguments).then(response => {
                networkRequests.unshift({
                    type: 'Fetch',
                    method: method,
                    url: typeof url === 'string' ? url : url.url,
                    status: response.status,
                    time: Date.now() - startTime
                });
                if (networkRequests.length > 100) networkRequests.pop();
                updateNetworkTab();
                return response;
            }).catch(err => {
                networkRequests.unshift({
                    type: 'Fetch',
                    method: method,
                    url: typeof url === 'string' ? url : url.url,
                    status: 'Error',
                    time: Date.now() - startTime
                });
                updateNetworkTab();
                throw err;
            });
        };
    }
    
    // ========== 2. Console 拦截 ==========
    function interceptConsole() {
        const methods = ['log', 'warn', 'error', 'info'];
        methods.forEach(method => {
            const original = console[method];
            console[method] = function(...args) {
                consoleLogs.unshift({
                    type: method,
                    message: args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' '),
                    time: new Date().toLocaleTimeString()
                });
                if (consoleLogs.length > 200) consoleLogs.pop();
                updateConsoleTab();
                return original.apply(this, args);
            };
        });
    }
    
    // ========== 3. 元素审查 ==========
    function startInspectMode() {
        isInspectMode = true;
        document.body.style.cursor = 'crosshair';
        
        if (!highlightOverlay) {
            highlightOverlay = document.createElement('div');
            highlightOverlay.className = 'wta-highlight';
            document.body.appendChild(highlightOverlay);
        }
        
        document.addEventListener('mousemove', handleInspectMove);
        document.addEventListener('click', handleInspectClick, true);
    }
    
    function stopInspectMode() {
        isInspectMode = false;
        document.body.style.cursor = '';
        if (highlightOverlay) highlightOverlay.style.display = 'none';
        document.removeEventListener('mousemove', handleInspectMove);
        document.removeEventListener('click', handleInspectClick, true);
    }
    
    function handleInspectMove(e) {
        if (!isInspectMode || !highlightOverlay) return;
        const el = e.target;
        if (el === highlightOverlay || el.closest('#wta-analyzer-panel')) return;
        
        const rect = el.getBoundingClientRect();
        highlightOverlay.style.cssText = `
            display: block;
            top: ${'$'}{rect.top}px;
            left: ${'$'}{rect.left}px;
            width: ${'$'}{rect.width}px;
            height: ${'$'}{rect.height}px;
        `;
    }
    
    function handleInspectClick(e) {
        if (!isInspectMode) return;
        const el = e.target;
        if (el.closest('#wta-analyzer-panel') || el.id === 'wta-analyzer-btn') return;
        
        e.preventDefault();
        e.stopPropagation();
        stopInspectMode();
        showElementInfo(el);
    }
    
    function showElementInfo(el) {
        const computed = window.getComputedStyle(el);
        const info = {
            tag: el.tagName.toLowerCase(),
            id: el.id || '(none)',
            classes: el.className || '(none)',
            size: `${'$'}{el.offsetWidth} x ${'$'}{el.offsetHeight}`,
            position: `${'$'}{computed.position}`,
            display: computed.display,
            color: computed.color,
            background: computed.backgroundColor,
            font: `${'$'}{computed.fontSize} ${'$'}{computed.fontFamily.split(',')[0]}`,
            html: el.outerHTML.substring(0, 500)
        };
        
        // 检查图片
        let imgSrc = '';
        if (el.tagName === 'IMG') imgSrc = el.src;
        else if (computed.backgroundImage !== 'none') {
            const match = computed.backgroundImage.match(/url\(["']?(.+?)["']?\)/);
            if (match) imgSrc = match[1];
        }
        
        const content = document.querySelector('#wta-inspect-content');
        if (content) {
            content.innerHTML = `
                <div class="wta-element-info">
                    <div><strong>标签:</strong> &lt;${'$'}{info.tag}&gt;</div>
                    <div><strong>ID:</strong> ${'$'}{info.id}</div>
                    <div><strong>Class:</strong> ${'$'}{info.classes}</div>
                    <div><strong>尺寸:</strong> ${'$'}{info.size}</div>
                    <div><strong>定位:</strong> ${'$'}{info.position} / ${'$'}{info.display}</div>
                    <div><strong>颜色:</strong> ${'$'}{info.color}</div>
                    <div><strong>背景:</strong> ${'$'}{info.background}</div>
                    <div><strong>字体:</strong> ${'$'}{info.font}</div>
                    ${'$'}{imgSrc ? `<div><strong>图片:</strong> <a href="${'$'}{imgSrc}" target="_blank" style="color:#4fc3f7;">${'$'}{imgSrc.substring(0,80)}...</a></div>` : ''}
                    <div style="margin-top:12px;"><strong>HTML:</strong></div>
                    <pre style="color:#ce9178;font-size:11px;max-height:150px;overflow:auto;">${'$'}{escapeHtml(info.html)}</pre>
                </div>
            `;
        }
    }
    
    function escapeHtml(str) {
        return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }
    
    // ========== 4. Cookie 管理 ==========
    function getCookies() {
        return document.cookie.split(';').map(c => {
            const [name, ...rest] = c.trim().split('=');
            return { name, value: rest.join('=') };
        }).filter(c => c.name);
    }
    
    function clearAllCookies() {
        const cookies = getCookies();
        cookies.forEach(c => {
            document.cookie = `${'$'}{c.name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
        });
        updateCookieTab();
    }
    
    // ========== 5. 创建主面板 ==========
    function createMainPanel() {
        if (mainPanel) return mainPanel;
        
        // 注入样式
        const style = document.createElement('style');
        style.textContent = STYLES;
        document.head.appendChild(style);
        
        mainPanel = document.createElement('div');
        mainPanel.id = 'wta-analyzer-panel';
        mainPanel.innerHTML = `
            <button class="wta-close-btn" id="wta-close-panel">✕</button>
            <div class="wta-tabs">
                <div class="wta-tab active" data-tab="inspect">🔍 元素</div>
                <div class="wta-tab" data-tab="network">📡 网络</div>
                <div class="wta-tab" data-tab="cookie">🍪 Cookie</div>
                <div class="wta-tab" data-tab="console">💻 控制台</div>
            </div>
            <div class="wta-tab-content active" id="wta-tab-inspect">
                <button class="wta-btn" id="wta-start-inspect">🎯 选取元素</button>
                <div id="wta-inspect-content"></div>
            </div>
            <div class="wta-tab-content" id="wta-tab-network">
                <button class="wta-btn" id="wta-clear-network">清空</button>
                <div id="wta-network-list"></div>
            </div>
            <div class="wta-tab-content" id="wta-tab-cookie">
                <button class="wta-btn danger" id="wta-clear-cookies">清除所有Cookie</button>
                <div id="wta-cookie-list"></div>
            </div>
            <div class="wta-tab-content" id="wta-tab-console">
                <div id="wta-console-logs" style="flex:1;overflow:auto;"></div>
            </div>
            <div class="wta-console-input">
                <input type="text" id="wta-js-input" placeholder="输入 JavaScript 代码...">
                <button class="wta-btn" id="wta-run-js">运行</button>
            </div>
        `;
        
        document.body.appendChild(mainPanel);
        bindPanelEvents();
        return mainPanel;
    }
    
    function bindPanelEvents() {
        // Tab 切换
        mainPanel.querySelectorAll('.wta-tab').forEach(tab => {
            tab.addEventListener('click', () => {
                mainPanel.querySelectorAll('.wta-tab').forEach(t => t.classList.remove('active'));
                mainPanel.querySelectorAll('.wta-tab-content').forEach(c => c.classList.remove('active'));
                tab.classList.add('active');
                document.getElementById('wta-tab-' + tab.dataset.tab).classList.add('active');
                
                if (tab.dataset.tab === 'cookie') updateCookieTab();
                if (tab.dataset.tab === 'network') updateNetworkTab();
                if (tab.dataset.tab === 'console') updateConsoleTab();
            });
        });
        
        // 关闭面板
        document.getElementById('wta-close-panel').addEventListener('click', () => {
            mainPanel.classList.remove('show');
            stopInspectMode();
        });
        
        // 元素审查
        document.getElementById('wta-start-inspect').addEventListener('click', startInspectMode);
        
        // 清空网络
        document.getElementById('wta-clear-network').addEventListener('click', () => {
            networkRequests = [];
            updateNetworkTab();
        });
        
        // 清除 Cookie
        document.getElementById('wta-clear-cookies').addEventListener('click', () => {
            if (confirm('确定清除所有Cookie？')) {
                clearAllCookies();
            }
        });
        
        // 运行 JS
        document.getElementById('wta-run-js').addEventListener('click', runCustomJS);
        document.getElementById('wta-js-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') runCustomJS();
        });
    }
    
    function runCustomJS() {
        const input = document.getElementById('wta-js-input');
        const code = input.value.trim();
        if (!code) return;
        
        try {
            const result = eval(code);
            console.log('> ' + code);
            if (result !== undefined) console.log(result);
        } catch (e) {
            console.error('Error: ' + e.message);
        }
        input.value = '';
    }
    
    function updateNetworkTab() {
        const list = document.getElementById('wta-network-list');
        if (!list) return;
        
        list.innerHTML = networkRequests.map(r => `
            <div class="wta-request-item">
                <span class="wta-method ${'$'}{r.method}">${'$'}{r.method}</span>
                <span class="wta-url">${'$'}{r.url?.substring(0, 60)}${'$'}{r.url?.length > 60 ? '...' : ''}</span>
                <span class="wta-status ${'$'}{r.status >= 200 && r.status < 400 ? 'ok' : 'error'}">${'$'}{r.status}</span>
                <span style="color:#666;margin-left:8px;">${'$'}{r.time}ms</span>
            </div>
        `).join('') || '<div style="color:#666;padding:20px;text-align:center;">暂无请求</div>';
    }
    
    function updateCookieTab() {
        const list = document.getElementById('wta-cookie-list');
        if (!list) return;
        
        const cookies = getCookies();
        list.innerHTML = cookies.map(c => `
            <div class="wta-cookie-item">
                <span class="wta-cookie-name">${'$'}{c.name}</span>
                <span class="wta-cookie-value">${'$'}{c.value}</span>
            </div>
        `).join('') || '<div style="color:#666;padding:20px;text-align:center;">无Cookie</div>';
    }
    
    function updateConsoleTab() {
        const logs = document.getElementById('wta-console-logs');
        if (!logs) return;
        
        logs.innerHTML = consoleLogs.map(l => `
            <div class="wta-log-item ${'$'}{l.type}">
                <span style="color:#666;margin-right:8px;">${'$'}{l.time}</span>
                ${'$'}{escapeHtml(l.message)}
            </div>
        `).join('');
    }
    
    // ========== 6. 触发按钮 ==========
    function createTriggerButton() {
        const btn = document.createElement('div');
        btn.id = 'wta-analyzer-btn';
        btn.innerHTML = '🔧';
        btn.addEventListener('click', () => {
            createMainPanel();
            mainPanel.classList.toggle('show');
        });
        document.body.appendChild(btn);
    }
    
    // ========== 初始化 ==========
    function init() {
        interceptNetwork();
        interceptConsole();
        createTriggerButton();
        console.log('[WebAnalyzer] 模块已初始化');
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
"""

    /**
     * 网页分析工具
     * 元素审查、网络监控、Cookie管理、Console注入
     */
    private fun webAnalyzer() = ExtensionModule(
        id = "builtin-web-analyzer",
        name = "网页分析工具",
        description = "元素审查、网络请求监控、Cookie管理、JS控制台",
        icon = "🔧",
        category = ModuleCategory.DEVELOPER,
        tags = listOf("开发", "调试", "网络", "Cookie"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.NETWORK, ModulePermission.STORAGE),
        code = WEB_ANALYZER_CODE.trimIndent()
    )
    
    // ==================== 高级暗黑模式 ====================
    
    private const val ADVANCED_DARK_MODE_CODE = """
(function() {
    'use strict';
    
    let isDarkMode = false;
    let styleElement = null;
    let observer = null;
    let scheduleTimer = null;
    
    // 配置
    const config = {
        startHour: parseInt(getConfig('startHour', '19')),
        endHour: parseInt(getConfig('endHour', '7')),
        imageBrightness: parseFloat(getConfig('imageBrightness', '0.8')),
        autoSchedule: getConfig('autoSchedule', 'true') === 'true'
    };
    
    // ========== 1. 核心暗黑模式样式 ==========
    const DARK_STYLES = `
        /* 智能色彩反转 - 基础 */
        html.wta-dark-mode {
            filter: invert(1) hue-rotate(180deg) !important;
            background: #121212 !important;
        }
        
        /* 图片、视频、Canvas 反转回来 */
        html.wta-dark-mode img,
        html.wta-dark-mode video,
        html.wta-dark-mode canvas,
        html.wta-dark-mode svg,
        html.wta-dark-mode picture,
        html.wta-dark-mode [style*="background-image"],
        html.wta-dark-mode iframe {
            filter: invert(1) hue-rotate(180deg) !important;
        }
        
        /* 图片亮度控制 */
        html.wta-dark-mode img,
        html.wta-dark-mode video {
            opacity: var(--wta-img-brightness, 0.8) !important;
            transition: opacity 0.3s ease !important;
        }
        
        /* 图片交互时恢复亮度 */
        html.wta-dark-mode img:hover,
        html.wta-dark-mode img:active,
        html.wta-dark-mode img.wta-img-active,
        html.wta-dark-mode video:hover,
        html.wta-dark-mode video:active {
            opacity: 1 !important;
        }
        
        /* 降低高饱和度颜色亮度 */
        html.wta-dark-mode {
            --wta-saturate: 0.8;
        }
        
        html.wta-dark-mode *:not(img):not(video):not(canvas):not(svg) {
            filter: saturate(var(--wta-saturate)) !important;
        }
        
        /* 强制覆盖白色背景 */
        html.wta-dark-mode body,
        html.wta-dark-mode div,
        html.wta-dark-mode section,
        html.wta-dark-mode article,
        html.wta-dark-mode header,
        html.wta-dark-mode footer,
        html.wta-dark-mode main,
        html.wta-dark-mode aside,
        html.wta-dark-mode nav {
            background-color: inherit !important;
        }
        
        /* 修复常见白色元素 */
        html.wta-dark-mode [style*="background: white"],
        html.wta-dark-mode [style*="background:#fff"],
        html.wta-dark-mode [style*="background: #fff"],
        html.wta-dark-mode [style*="background-color: white"],
        html.wta-dark-mode [style*="background-color:#fff"],
        html.wta-dark-mode [style*="background-color: #fff"],
        html.wta-dark-mode [style*="background-color: rgb(255, 255, 255)"] {
            background-color: #1a1a1a !important;
        }
        
        /* 修复输入框 */
        html.wta-dark-mode input,
        html.wta-dark-mode textarea,
        html.wta-dark-mode select {
            background-color: #2d2d2d !important;
            color: #e0e0e0 !important;
            border-color: #444 !important;
        }
        
        /* 修复滚动条 */
        html.wta-dark-mode ::-webkit-scrollbar {
            background: #1a1a1a !important;
        }
        html.wta-dark-mode ::-webkit-scrollbar-thumb {
            background: #444 !important;
        }
    `;
    
    // ========== 2. 注入样式 ==========
    function injectStyles() {
        if (styleElement) return;
        
        styleElement = document.createElement('style');
        styleElement.id = 'wta-dark-mode-styles';
        styleElement.textContent = DARK_STYLES;
        document.head.appendChild(styleElement);
        
        // 设置图片亮度变量
        document.documentElement.style.setProperty('--wta-img-brightness', config.imageBrightness);
    }
    
    // ========== 3. 启用/禁用暗黑模式 ==========
    function enableDarkMode() {
        if (isDarkMode) return;
        
        injectStyles();
        document.documentElement.classList.add('wta-dark-mode');
        isDarkMode = true;
        
        // 启动强力覆盖监听
        startForceOverride();
        
        // 绑定图片交互
        bindImageInteraction();
        
        console.log('[DarkMode] 暗黑模式已启用');
        updateToggleButton();
    }
    
    function disableDarkMode() {
        if (!isDarkMode) return;
        
        document.documentElement.classList.remove('wta-dark-mode');
        isDarkMode = false;
        
        // 停止监听
        if (observer) {
            observer.disconnect();
            observer = null;
        }
        
        console.log('[DarkMode] 暗黑模式已禁用');
        updateToggleButton();
    }
    
    function toggleDarkMode() {
        isDarkMode ? disableDarkMode() : enableDarkMode();
    }
    
    // ========== 4. 强力覆盖 - 监听动态样式 ==========
    function startForceOverride() {
        if (observer) return;
        
        observer = new MutationObserver((mutations) => {
            mutations.forEach(mutation => {
                // 监听 style 属性变化
                if (mutation.type === 'attributes' && mutation.attributeName === 'style') {
                    const el = mutation.target;
                    forceOverrideElement(el);
                }
                
                // 监听新增节点
                if (mutation.type === 'childList') {
                    mutation.addedNodes.forEach(node => {
                        if (node.nodeType === 1) {
                            forceOverrideElement(node);
                            node.querySelectorAll?.('*').forEach(forceOverrideElement);
                        }
                    });
                }
            });
        });
        
        observer.observe(document.body, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['style']
        });
        
        // 初始扫描
        document.querySelectorAll('*').forEach(forceOverrideElement);
    }
    
    function forceOverrideElement(el) {
        if (!el || !el.style) return;
        
        const computed = window.getComputedStyle(el);
        const bgColor = computed.backgroundColor;
        
        // 检测亮色背景
        if (bgColor && isLightColor(bgColor)) {
            el.style.setProperty('background-color', '#1a1a1a', 'important');
        }
    }
    
    function isLightColor(color) {
        const match = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
        if (!match) return false;
        
        const [, r, g, b] = match.map(Number);
        // 计算亮度
        const brightness = (r * 299 + g * 587 + b * 114) / 1000;
        return brightness > 200;
    }
    
    // ========== 5. 图片交互 - 长按/点击恢复亮度 ==========
    function bindImageInteraction() {
        let pressTimer = null;
        
        document.addEventListener('touchstart', (e) => {
            const img = e.target.closest('img');
            if (!img) return;
            
            pressTimer = setTimeout(() => {
                img.classList.add('wta-img-active');
            }, 200);
        }, { passive: true });
        
        document.addEventListener('touchend', () => {
            clearTimeout(pressTimer);
            document.querySelectorAll('.wta-img-active').forEach(img => {
                img.classList.remove('wta-img-active');
            });
        }, { passive: true });
        
        document.addEventListener('touchcancel', () => {
            clearTimeout(pressTimer);
        }, { passive: true });
    }
    
    // ========== 6. 定时任务 ==========
    function checkSchedule() {
        if (!config.autoSchedule) return;
        
        const hour = new Date().getHours();
        const shouldBeDark = config.startHour > config.endHour
            ? (hour >= config.startHour || hour < config.endHour)
            : (hour >= config.startHour && hour < config.endHour);
        
        if (shouldBeDark && !isDarkMode) {
            enableDarkMode();
        } else if (!shouldBeDark && isDarkMode) {
            disableDarkMode();
        }
    }
    
    function startSchedule() {
        if (!config.autoSchedule) return;
        
        // 立即检查一次
        checkSchedule();
        
        // 每分钟检查
        scheduleTimer = setInterval(checkSchedule, 60000);
        
        // 尝试调用原生 API 获取系统暗黑模式状态
        if (typeof NativeBridge !== 'undefined' && NativeBridge.isDarkMode) {
            try {
                const systemDark = NativeBridge.isDarkMode();
                if (systemDark && !isDarkMode) enableDarkMode();
            } catch (e) {}
        }
    }
    
    // ========== 7. 创建切换按钮 ==========
    function createToggleButton() {
        const btn = document.createElement('div');
        btn.id = 'wta-dark-mode-btn';
        btn.innerHTML = '🌙';
        btn.style.cssText = `
            position: fixed;
            bottom: 200px;
            right: 16px;
            width: 48px;
            height: 48px;
            border-radius: 50%;
            background: linear-gradient(135deg, #2c3e50 0%, #1a1a2e 100%);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            cursor: pointer;
            z-index: 999998;
            box-shadow: 0 4px 15px rgba(0,0,0,0.3);
            transition: transform 0.2s, background 0.3s;
            -webkit-tap-highlight-color: transparent;
        `;
        
        btn.addEventListener('click', toggleDarkMode);
        
        btn.addEventListener('touchstart', () => {
            btn.style.transform = 'scale(0.9)';
        }, { passive: true });
        
        btn.addEventListener('touchend', () => {
            btn.style.transform = 'scale(1)';
        }, { passive: true });
        
        document.body.appendChild(btn);
        return btn;
    }
    
    function updateToggleButton() {
        const btn = document.getElementById('wta-dark-mode-btn');
        if (!btn) return;
        
        if (isDarkMode) {
            btn.innerHTML = '☀️';
            btn.style.background = 'linear-gradient(135deg, #f39c12 0%, #e74c3c 100%)';
        } else {
            btn.innerHTML = '🌙';
            btn.style.background = 'linear-gradient(135deg, #2c3e50 0%, #1a1a2e 100%)';
        }
    }
    
    // ========== 初始化 ==========
    function init() {
        createToggleButton();
        
        // 启动定时任务
        startSchedule();
        
        // 如果不是自动模式，检查是否应该默认启用
        if (!config.autoSchedule) {
            // 检查系统偏好
            if (window.matchMedia?.('(prefers-color-scheme: dark)').matches) {
                enableDarkMode();
            }
        }
        
        console.log('[DarkMode] 模块已初始化');
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
"""

    /**
     * 高级暗黑模式
     * 智能色彩反转、图片亮度控制、强力覆盖、定时任务
     */
    private fun advancedDarkMode() = ExtensionModule(
        id = "builtin-advanced-dark-mode",
        name = "高级暗黑模式",
        description = "智能色彩反转、图片亮度控制、强力覆盖动态样式、支持定时开关",
        icon = "🌙",
        category = ModuleCategory.STYLE_MODIFIER,
        tags = listOf("暗黑", "护眼", "夜间", "主题"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "autoSchedule",
                name = "定时开关",
                description = "根据时间自动开启/关闭",
                type = ConfigItemType.BOOLEAN,
                defaultValue = "true"
            ),
            ModuleConfigItem(
                key = "startHour",
                name = "开始时间",
                description = "暗黑模式开始时间（小时，0-23）",
                type = ConfigItemType.NUMBER,
                defaultValue = "19"
            ),
            ModuleConfigItem(
                key = "endHour",
                name = "结束时间",
                description = "暗黑模式结束时间（小时，0-23）",
                type = ConfigItemType.NUMBER,
                defaultValue = "7"
            ),
            ModuleConfigItem(
                key = "imageBrightness",
                name = "图片亮度",
                description = "图片亮度（0.5-1.0）",
                type = ConfigItemType.NUMBER,
                defaultValue = "0.8"
            )
        ),
        configValues = mapOf(
            "autoSchedule" to "true",
            "startHour" to "19",
            "endHour" to "7",
            "imageBrightness" to "0.8"
        ),
        code = ADVANCED_DARK_MODE_CODE.trimIndent()
    )
    
    // ==================== 隐私保护模块 ====================
    
    private const val PRIVACY_PROTECTION_CODE = """
(function() {
    'use strict';
    
    // ========== 1. 广告域名黑名单 ==========
    const AD_DOMAINS = [
        'doubleclick.net', 'googlesyndication.com', 'googleadservices.com',
        'google-analytics.com', 'googletagmanager.com', 'googletagservices.com',
        'facebook.net', 'fbcdn.net', 'connect.facebook.net',
        'ads.twitter.com', 'analytics.twitter.com',
        'advertising.com', 'adnxs.com', 'adsrvr.org',
        'criteo.com', 'criteo.net', 'outbrain.com', 'taboola.com',
        'moatads.com', 'scorecardresearch.com', 'quantserve.com',
        'amazon-adsystem.com', 'media.net', 'pubmatic.com',
        'rubiconproject.com', 'openx.net', 'casalemedia.com',
        'bidswitch.net', 'adform.net', 'adsafeprotected.com',
        'baidu.com/cpro', 'pos.baidu.com', 'cpro.baidu.com',
        'tanx.com', 'mmstat.com', 'cnzz.com', 'umeng.com',
        'jiathis.com', 'bshare.cn', 'bdimg.share', 'bdstatic.com/linksubmit'
    ];
    
    // ========== 2. 广告 CSS 选择器 ==========
    const AD_SELECTORS = [
        '[class*="ad-"]', '[class*="ads-"]', '[class*="advert"]',
        '[class*="banner"]', '[class*="sponsor"]', '[class*="promo"]',
        '[id*="ad-"]', '[id*="ads-"]', '[id*="advert"]',
        '[id*="banner"]', '[id*="sponsor"]',
        '[class*="ad_"]', '[class*="ads_"]', '[id*="ad_"]',
        '.ad-container', '.ad-wrapper', '.ad-box', '.ad-slot',
        '.advertisement', '.advertising', '.adsbygoogle',
        '.banner-ad', '.sidebar-ad', '.footer-ad',
        '[data-ad]', '[data-ads]', '[data-ad-slot]',
        'ins.adsbygoogle', 'amp-ad', 'amp-embed',
        '[class*="google-ad"]', '[class*="dfp-"]',
        // 中文站点常见
        '[class*="guanggao"]', '[class*="tuiguang"]',
        '.ad-box', '.ad-item', '.ad-list'
    ];
    
    // ========== 3. 暴力去广告 ==========
    function blockAds() {
        // 注入 CSS 隐藏广告元素
        const style = document.createElement('style');
        style.id = 'wta-privacy-ad-block';
        style.textContent = AD_SELECTORS.map(s => s + `{
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            width: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }`).join('\n');
        document.head.appendChild(style);
        
        // 拦截广告脚本加载
        const originalCreateElement = document.createElement.bind(document);
        document.createElement = function(tagName) {
            const element = originalCreateElement(tagName);
            
            if (tagName.toLowerCase() === 'script') {
                const originalSetAttribute = element.setAttribute.bind(element);
                element.setAttribute = function(name, value) {
                    if (name === 'src' && isAdUrl(value)) {
                        console.log('[Privacy] 拦截广告脚本:', value);
                        return;
                    }
                    return originalSetAttribute(name, value);
                };
                
                // 拦截 src 属性直接赋值
                Object.defineProperty(element, 'src', {
                    set: function(value) {
                        if (isAdUrl(value)) {
                            console.log('[Privacy] 拦截广告脚本:', value);
                            return;
                        }
                        originalSetAttribute('src', value);
                    },
                    get: function() {
                        return element.getAttribute('src');
                    }
                });
            }
            
            return element;
        };
        
        // 阻止弹窗广告
        const originalOpen = window.open;
        window.open = function(url) {
            if (url && isAdUrl(url)) {
                console.log('[Privacy] 拦截弹窗广告:', url);
                return null;
            }
            return originalOpen.apply(this, arguments);
        };
        
        // 持续移除广告元素
        function removeAdElements() {
            AD_SELECTORS.forEach(selector => {
                try {
                    document.querySelectorAll(selector).forEach(el => {
                        if (!el.dataset.wtaHidden) {
                            el.style.cssText = 'display:none!important;visibility:hidden!important;';
                            el.dataset.wtaHidden = 'true';
                        }
                    });
                } catch (e) {}
            });
        }
        
        removeAdElements();
        const observer = new MutationObserver(removeAdElements);
        observer.observe(document.documentElement, { childList: true, subtree: true });
    }
    
    function isAdUrl(url) {
        if (!url) return false;
        const lowerUrl = url.toLowerCase();
        return AD_DOMAINS.some(domain => lowerUrl.includes(domain));
    }
    
    // ========== 4. 反指纹追踪 ==========
    function antiFingerprint() {
        // 随机化 Canvas 指纹
        const originalToDataURL = HTMLCanvasElement.prototype.toDataURL;
        HTMLCanvasElement.prototype.toDataURL = function(type) {
            const context = this.getContext('2d');
            if (context) {
                // 添加微小噪点
                const imageData = context.getImageData(0, 0, this.width, this.height);
                for (let i = 0; i < imageData.data.length; i += 4) {
                    imageData.data[i] ^= (Math.random() * 2) | 0;
                }
                context.putImageData(imageData, 0, 0);
            }
            return originalToDataURL.apply(this, arguments);
        };
        
        // 随机化 WebGL 指纹
        const getParameterProxyHandler = {
            apply: function(target, thisArg, args) {
                const param = args[0];
                const result = Reflect.apply(target, thisArg, args);
                // 对某些参数返回随机值
                if (param === 37445 || param === 37446) { // UNMASKED_VENDOR/RENDERER
                    return 'WebKit WebGL';
                }
                return result;
            }
        };
        
        try {
            const canvas = document.createElement('canvas');
            const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
            if (gl) {
                gl.getParameter = new Proxy(gl.getParameter, getParameterProxyHandler);
            }
        } catch (e) {}
        
        // 修改 navigator 属性
        const fakeNavigator = {
            platform: 'Linux armv8l',
            hardwareConcurrency: 4 + Math.floor(Math.random() * 4),
            deviceMemory: 4,
            languages: ['zh-CN', 'zh', 'en'],
            plugins: { length: 0 }
        };
        
        Object.keys(fakeNavigator).forEach(key => {
            try {
                Object.defineProperty(navigator, key, {
                    get: () => fakeNavigator[key],
                    configurable: true
                });
            } catch (e) {}
        });
        
        // 阻止 Battery API
        if (navigator.getBattery) {
            navigator.getBattery = () => Promise.reject('Battery API disabled');
        }
        
        // 阻止 Geolocation
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition = (s, e) => {
                if (e) e({ code: 1, message: 'Permission denied' });
            };
            navigator.geolocation.watchPosition = () => 0;
        }
        
        console.log('[Privacy] 反指纹追踪已启用');
    }
    
    // ========== 5. 点击劫持保护 ==========
    function clickjackProtection() {
        // 检测透明覆盖层
        function detectOverlay(e) {
            const target = e.target;
            const computed = window.getComputedStyle(target);
            
            // 检测透明或半透明的全屏覆盖层
            const isOverlay = (
                (computed.position === 'fixed' || computed.position === 'absolute') &&
                (parseFloat(computed.opacity) < 0.1 || computed.backgroundColor === 'transparent') &&
                target.offsetWidth > window.innerWidth * 0.8 &&
                target.offsetHeight > window.innerHeight * 0.8
            );
            
            if (isOverlay) {
                console.log('[Privacy] 检测到点击劫持覆盖层，已拦截');
                e.preventDefault();
                e.stopPropagation();
                target.style.display = 'none';
                return false;
            }
        }
        
        document.addEventListener('click', detectOverlay, true);
        document.addEventListener('touchstart', detectOverlay, true);
        
        // 移除可疑的透明层
        function removeOverlays() {
            document.querySelectorAll('div, a').forEach(el => {
                const computed = window.getComputedStyle(el);
                if (
                    (computed.position === 'fixed' || computed.position === 'absolute') &&
                    parseFloat(computed.opacity) < 0.05 &&
                    el.offsetWidth > window.innerWidth * 0.5 &&
                    el.offsetHeight > window.innerHeight * 0.5 &&
                    !el.querySelector('img, video, input, button')
                ) {
                    el.style.display = 'none';
                    console.log('[Privacy] 移除可疑透明层');
                }
            });
        }
        
        setTimeout(removeOverlays, 1000);
        setTimeout(removeOverlays, 3000);
    }
    
    // ========== 6. 外链警告 ==========
    function externalLinkWarning() {
        const currentHost = location.hostname;
        
        document.addEventListener('click', (e) => {
            const link = e.target.closest('a');
            if (!link || !link.href) return;
            
            try {
                const url = new URL(link.href);
                const targetHost = url.hostname;
                
                // 检查是否为外部链接
                if (targetHost && targetHost !== currentHost && 
                    !targetHost.endsWith('.' + currentHost) &&
                    !currentHost.endsWith('.' + targetHost)) {
                    
                    e.preventDefault();
                    e.stopPropagation();
                    
                    // 使用原生对话框或 NativeBridge
                    const message = '您即将离开当前 App，前往：\n' + targetHost + '\n\n是否继续？';
                    
                    if (typeof NativeBridge !== 'undefined' && NativeBridge.showConfirmDialog) {
                        NativeBridge.showConfirmDialog('外链提醒', message, (confirmed) => {
                            if (confirmed) window.location.href = link.href;
                        });
                    } else {
                        if (confirm(message)) {
                            window.location.href = link.href;
                        }
                    }
                }
            } catch (e) {}
        }, true);
        
        console.log('[Privacy] 外链警告已启用');
    }
    
    // ========== 7. 创建状态指示器 ==========
    function createIndicator() {
        const indicator = document.createElement('div');
        indicator.id = 'wta-privacy-indicator';
        indicator.innerHTML = '🛡️';
        indicator.title = '隐私保护已启用';
        indicator.style.cssText = `
            position: fixed;
            top: 10px;
            right: 10px;
            width: 32px;
            height: 32px;
            border-radius: 50%;
            background: rgba(76, 175, 80, 0.9);
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 16px;
            z-index: 999999;
            box-shadow: 0 2px 8px rgba(0,0,0,0.2);
            cursor: pointer;
            transition: transform 0.2s;
        `;
        
        indicator.addEventListener('click', () => {
            showStats();
        });
        
        document.body.appendChild(indicator);
    }
    
    let blockedCount = 0;
    function showStats() {
        const msg = `🛡️ 隐私保护统计\n\n` +
                    `已拦截广告请求: ${'$'}{blockedCount}\n` +
                    `反指纹追踪: 已启用\n` +
                    `点击劫持保护: 已启用\n` +
                    `外链警告: 已启用`;
        alert(msg);
    }
    
    // ========== 初始化 ==========
    function init() {
        // 尽早执行反指纹
        antiFingerprint();
        
        // DOM 加载后执行其他功能
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                blockAds();
                clickjackProtection();
                externalLinkWarning();
                createIndicator();
            });
        } else {
            blockAds();
            clickjackProtection();
            externalLinkWarning();
            createIndicator();
        }
        
        console.log('[Privacy] 隐私保护模块已初始化');
    }
    
    init();
})();
"""

    /**
     * 隐私保护模块
     * 去广告、反指纹、点击保护、外链警告
     */
    private fun privacyProtection() = ExtensionModule(
        id = "builtin-privacy-protection",
        name = "隐私保护",
        description = "暴力去广告、反指纹追踪、点击劫持保护、外链警告",
        icon = "🛡️",
        category = ModuleCategory.SECURITY,
        tags = listOf("隐私", "广告", "安全", "追踪"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.NETWORK),
        code = PRIVACY_PROTECTION_CODE.trimIndent()
    )
    
    // ==================== 内容处理增强模块 ====================
    
    private const val CONTENT_ENHANCE_CODE = """
(function() {
    'use strict';
    
    let selectionPopup = null;
    let toolPanel = null;
    
    // ========== 1. 强制复制 - 破解复制限制 ==========
    function enableForceCopy() {
        // 注入 CSS 允许选择
        const style = document.createElement('style');
        style.id = 'wta-force-copy-style';
        style.textContent = `
            *, *::before, *::after {
                -webkit-user-select: auto !important;
                -moz-user-select: auto !important;
                -ms-user-select: auto !important;
                user-select: auto !important;
                -webkit-touch-callout: default !important;
            }
        `;
        document.head.appendChild(style);
        
        // 阻止禁用复制的事件
        const events = ['copy', 'cut', 'paste', 'selectstart', 'contextmenu', 'dragstart', 'mousedown'];
        events.forEach(event => {
            document.addEventListener(event, (e) => {
                e.stopPropagation();
            }, true);
        });
        
        // 移除元素上的禁用属性
        function removeRestrictions() {
            document.querySelectorAll('*').forEach(el => {
                events.forEach(event => {
                    el.removeAttribute('on' + event);
                });
                el.style.userSelect = 'auto';
                el.style.webkitUserSelect = 'auto';
            });
        }
        
        removeRestrictions();
        
        // 监听 DOM 变化持续移除
        const observer = new MutationObserver(removeRestrictions);
        observer.observe(document.body, { childList: true, subtree: true });
        
        // 覆盖可能被修改的方法
        document.oncopy = null;
        document.oncut = null;
        document.onpaste = null;
        document.onselectstart = null;
        document.oncontextmenu = null;
        
        console.log('[ContentEnhance] 强制复制已启用');
    }
    
    // ========== 2. 划词翻译 ==========
    function enableTranslation() {
        document.addEventListener('mouseup', handleSelection);
        document.addEventListener('touchend', handleSelection);
    }
    
    function handleSelection(e) {
        // 延迟执行，等待选择完成
        setTimeout(() => {
            const selection = window.getSelection();
            const text = selection.toString().trim();
            
            // 移除旧的弹窗
            if (selectionPopup) {
                selectionPopup.remove();
                selectionPopup = null;
            }
            
            if (!text || text.length < 2 || text.length > 500) return;
            
            // 获取选区位置
            const range = selection.getRangeAt(0);
            const rect = range.getBoundingClientRect();
            
            // 创建弹窗
            selectionPopup = document.createElement('div');
            selectionPopup.id = 'wta-selection-popup';
            selectionPopup.innerHTML = `
                <button class="wta-sel-btn" data-action="translate">🌐 翻译</button>
                <button class="wta-sel-btn" data-action="copy">📋 复制</button>
                <button class="wta-sel-btn" data-action="markdown">📝 MD</button>
            `;
            selectionPopup.style.cssText = `
                position: fixed;
                top: ${'$'}{Math.max(10, rect.top - 45)}px;
                left: ${'$'}{Math.min(window.innerWidth - 180, Math.max(10, rect.left))}px;
                background: rgba(30, 30, 30, 0.95);
                border-radius: 8px;
                padding: 6px;
                display: flex;
                gap: 4px;
                z-index: 9999999;
                box-shadow: 0 4px 12px rgba(0,0,0,0.3);
                animation: wtaPopIn 0.2s ease;
            `;
            
            // 注入动画样式
            if (!document.getElementById('wta-content-style')) {
                const animStyle = document.createElement('style');
                animStyle.id = 'wta-content-style';
                animStyle.textContent = `
                    @keyframes wtaPopIn {
                        from { opacity: 0; transform: translateY(10px); }
                        to { opacity: 1; transform: translateY(0); }
                    }
                    .wta-sel-btn {
                        background: #444;
                        color: white;
                        border: none;
                        padding: 8px 12px;
                        border-radius: 6px;
                        font-size: 12px;
                        cursor: pointer;
                        transition: background 0.2s;
                    }
                    .wta-sel-btn:active { background: #666; }
                    #wta-translate-result {
                        position: fixed;
                        background: rgba(30, 30, 30, 0.95);
                        color: white;
                        padding: 16px;
                        border-radius: 12px;
                        max-width: 300px;
                        max-height: 200px;
                        overflow: auto;
                        z-index: 9999999;
                        box-shadow: 0 8px 24px rgba(0,0,0,0.4);
                        font-size: 14px;
                        line-height: 1.5;
                    }
                `;
                document.head.appendChild(animStyle);
            }
            
            document.body.appendChild(selectionPopup);
            
            // 绑定按钮事件
            selectionPopup.querySelectorAll('.wta-sel-btn').forEach(btn => {
                btn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    const action = btn.dataset.action;
                    
                    if (action === 'translate') {
                        translateText(text, rect);
                    } else if (action === 'copy') {
                        copyToClipboard(text);
                        showToast('已复制到剪贴板');
                    } else if (action === 'markdown') {
                        const md = textToMarkdown(text);
                        copyToClipboard(md);
                        showToast('Markdown 已复制');
                    }
                    
                    selectionPopup?.remove();
                    selectionPopup = null;
                });
            });
            
            // 点击其他地方关闭
            setTimeout(() => {
                document.addEventListener('click', closePopup, { once: true });
            }, 100);
        }, 50);
    }
    
    function closePopup() {
        if (selectionPopup) {
            selectionPopup.remove();
            selectionPopup = null;
        }
    }
    
    async function translateText(text, rect) {
        // 显示加载状态
        const resultDiv = document.createElement('div');
        resultDiv.id = 'wta-translate-result';
        resultDiv.textContent = '翻译中...';
        resultDiv.style.top = (rect.bottom + 10) + 'px';
        resultDiv.style.left = Math.max(10, rect.left) + 'px';
        document.body.appendChild(resultDiv);
        
        try {
            // 使用 Google 翻译 API（免费接口）
            const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=zh-CN&dt=t&q=${'$'}{encodeURIComponent(text)}`;
            const response = await fetch(url);
            const data = await response.json();
            
            let translated = '';
            if (data && data[0]) {
                data[0].forEach(item => {
                    if (item[0]) translated += item[0];
                });
            }
            
            resultDiv.innerHTML = `
                <div style="color:#4fc3f7;font-size:12px;margin-bottom:8px;">翻译结果</div>
                <div>${'$'}{translated || '翻译失败'}</div>
                <div style="margin-top:12px;padding-top:8px;border-top:1px solid #444;color:#888;font-size:11px;">
                    原文: ${'$'}{text.substring(0, 50)}${'$'}{text.length > 50 ? '...' : ''}
                </div>
            `;
        } catch (e) {
            resultDiv.textContent = '翻译失败，请重试';
            console.error('[ContentEnhance] 翻译错误:', e);
        }
        
        // 点击关闭
        setTimeout(() => {
            document.addEventListener('click', () => resultDiv.remove(), { once: true });
        }, 100);
        
        // 5秒后自动关闭
        setTimeout(() => resultDiv.remove(), 8000);
    }
    
    // ========== 3. 一键长截图 ==========
    function enableLongScreenshot() {
        // 功能在工具面板中提供
    }
    
    function triggerLongScreenshot() {
        const pageHeight = Math.max(
            document.body.scrollHeight,
            document.body.offsetHeight,
            document.documentElement.scrollHeight,
            document.documentElement.offsetHeight
        );
        const pageWidth = document.documentElement.clientWidth;
        
        console.log('[ContentEnhance] 页面尺寸:', pageWidth, 'x', pageHeight);
        
        // 通知原生端
        if (typeof NativeBridge !== 'undefined' && NativeBridge.startLongScreenshot) {
            NativeBridge.startLongScreenshot(pageWidth, pageHeight, document.title);
            showToast('开始长截图...');
        } else {
            showToast('长截图需要原生支持');
            console.log('[ContentEnhance] 长截图参数:', { pageWidth, pageHeight, title: document.title });
        }
    }
    
    // ========== 4. Markdown 转化 ==========
    function htmlToMarkdown(element) {
        let md = '';
        
        function processNode(node, depth = 0) {
            if (node.nodeType === Node.TEXT_NODE) {
                return node.textContent;
            }
            
            if (node.nodeType !== Node.ELEMENT_NODE) return '';
            
            const tag = node.tagName.toLowerCase();
            let content = '';
            
            // 递归处理子节点
            node.childNodes.forEach(child => {
                content += processNode(child, depth);
            });
            
            content = content.trim();
            if (!content && !['img', 'br', 'hr'].includes(tag)) return '';
            
            switch (tag) {
                case 'h1': return `# ${'$'}{content}\n\n`;
                case 'h2': return `## ${'$'}{content}\n\n`;
                case 'h3': return `### ${'$'}{content}\n\n`;
                case 'h4': return `#### ${'$'}{content}\n\n`;
                case 'h5': return `##### ${'$'}{content}\n\n`;
                case 'h6': return `###### ${'$'}{content}\n\n`;
                case 'p': return `${'$'}{content}\n\n`;
                case 'br': return '\n';
                case 'hr': return '\n---\n\n';
                case 'strong':
                case 'b': return `**${'$'}{content}**`;
                case 'em':
                case 'i': return `*${'$'}{content}*`;
                case 'code': return `\`${'$'}{content}\``;
                case 'pre': return `\n\`\`\`\n${'$'}{content}\n\`\`\`\n\n`;
                case 'blockquote': return `> ${'$'}{content.replace(/\n/g, '\n> ')}\n\n`;
                case 'a':
                    const href = node.getAttribute('href') || '';
                    return `[${'$'}{content}](${'$'}{href})`;
                case 'img':
                    const src = node.getAttribute('src') || '';
                    const alt = node.getAttribute('alt') || 'image';
                    return `![${'$'}{alt}](${'$'}{src})`;
                case 'ul':
                case 'ol':
                    return content + '\n';
                case 'li':
                    const parent = node.parentElement?.tagName.toLowerCase();
                    const prefix = parent === 'ol' ? '1. ' : '- ';
                    return `${'$'}{prefix}${'$'}{content}\n`;
                case 'div':
                case 'section':
                case 'article':
                    return content + '\n';
                default:
                    return content;
            }
        }
        
        md = processNode(element);
        
        // 清理多余空行
        md = md.replace(/\n{3,}/g, '\n\n').trim();
        
        return md;
    }
    
    function textToMarkdown(text) {
        // 简单文本转 Markdown
        return text.split('\n').map(line => line.trim()).filter(line => line).join('\n\n');
    }
    
    function convertPageToMarkdown() {
        // 尝试找到正文区域
        const article = document.querySelector('article') ||
                       document.querySelector('[class*="content"]') ||
                       document.querySelector('[class*="article"]') ||
                       document.querySelector('main') ||
                       document.body;
        
        const md = htmlToMarkdown(article);
        const title = document.title;
        const url = location.href;
        
        const fullMd = `# ${'$'}{title}\n\n> 来源: ${'$'}{url}\n\n${'$'}{md}`;
        
        copyToClipboard(fullMd);
        showToast('Markdown 已复制到剪贴板');
        
        console.log('[ContentEnhance] Markdown 长度:', fullMd.length);
    }
    
    // ========== 5. 工具面板 ==========
    function createToolPanel() {
        if (toolPanel) return toolPanel;
        
        toolPanel = document.createElement('div');
        toolPanel.id = 'wta-content-tool-panel';
        toolPanel.innerHTML = `
            <div class="wta-tool-btn" data-action="screenshot" title="长截图">📸</div>
            <div class="wta-tool-btn" data-action="markdown" title="转Markdown">📝</div>
            <div class="wta-tool-btn" data-action="copy-all" title="复制全文">📄</div>
        `;
        toolPanel.style.cssText = `
            position: fixed;
            bottom: 260px;
            right: 16px;
            display: flex;
            flex-direction: column;
            gap: 8px;
            z-index: 999998;
        `;
        
        // 按钮样式
        const btnStyle = `
            width: 44px;
            height: 44px;
            border-radius: 50%;
            background: linear-gradient(135deg, #43a047 0%, #2e7d32 100%);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            cursor: pointer;
            box-shadow: 0 4px 12px rgba(67, 160, 71, 0.3);
            transition: transform 0.2s;
        `;
        
        toolPanel.querySelectorAll('.wta-tool-btn').forEach(btn => {
            btn.style.cssText = btnStyle;
            
            btn.addEventListener('click', () => {
                const action = btn.dataset.action;
                if (action === 'screenshot') {
                    triggerLongScreenshot();
                } else if (action === 'markdown') {
                    convertPageToMarkdown();
                } else if (action === 'copy-all') {
                    const text = document.body.innerText;
                    copyToClipboard(text);
                    showToast('全文已复制');
                }
            });
            
            btn.addEventListener('touchstart', () => {
                btn.style.transform = 'scale(0.9)';
            }, { passive: true });
            
            btn.addEventListener('touchend', () => {
                btn.style.transform = 'scale(1)';
            }, { passive: true });
        });
        
        document.body.appendChild(toolPanel);
        return toolPanel;
    }
    
    // ========== 工具函数 ==========
    function copyToClipboard(text) {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).catch(() => fallbackCopy(text));
        } else {
            fallbackCopy(text);
        }
    }
    
    function fallbackCopy(text) {
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.cssText = 'position:fixed;opacity:0;';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
    }
    
    function showToast(msg) {
        const t = document.createElement('div');
        t.textContent = msg;
        t.style.cssText = `
            position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);
            background:rgba(0,0,0,0.85);color:white;padding:14px 24px;
            border-radius:10px;font-size:14px;z-index:9999999;
        `;
        document.body.appendChild(t);
        setTimeout(() => t.remove(), 2000);
    }
    
    // ========== 初始化 ==========
    function init() {
        enableForceCopy();
        enableTranslation();
        
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', createToolPanel);
        } else {
            createToolPanel();
        }
        
        console.log('[ContentEnhance] 内容增强模块已初始化');
    }
    
    init();
})();
"""

    /**
     * 内容处理增强模块
     * 强制复制、划词翻译、长截图、Markdown转化
     */
    private fun contentEnhancer() = ExtensionModule(
        id = "builtin-content-enhancer",
        name = "内容增强",
        description = "强制复制、划词翻译、一键长截图、Markdown转化",
        icon = "📝",
        category = ModuleCategory.FUNCTION_ENHANCE,
        tags = listOf("复制", "翻译", "截图", "Markdown"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.CLIPBOARD, ModulePermission.NETWORK),
        code = CONTENT_ENHANCE_CODE.trimIndent()
    )
}
