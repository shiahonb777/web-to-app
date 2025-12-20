package com.webtoapp.core.extension

/**
 * 内置扩展模块
 * 
 * 提供一些常用的预置模块，用户可以直接使用或作为参考
 */
object BuiltInModules {
    
    /**
     * 获取所有内置模块
     */
    fun getAll(): List<ExtensionModule> = listOf(
        elementBlocker(),
        darkMode(),
        autoScroll(),
        copyProtectionRemover(),
        imageDownloader(),
        videoSpeedController(),
        adBlockerEnhanced(),
        readingMode(),
        customFont(),
        pageTranslateHelper(),
        // 新增模块
        scrollToTopButton(),
        pageZoom(),
        autoRefresh(),
        nightShield(),
        quickSearch()
    )
    
    /**
     * 元素屏蔽器 - 屏蔽页面指定元素
     */
    private fun elementBlocker() = ExtensionModule(
        id = "builtin-element-blocker",
        name = "元素屏蔽器",
        description = "通过 CSS 选择器屏蔽页面上的任意元素，支持多个选择器",
        icon = "🚫",
        category = ModuleCategory.CONTENT_FILTER,
        tags = listOf("屏蔽", "隐藏", "广告", "弹窗"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp", url = "https://github.com/WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "selectors",
                name = "CSS 选择器",
                description = "要屏蔽的元素选择器，每行一个",
                type = ConfigItemType.TEXTAREA,
                defaultValue = ".ad-banner\n#popup\n[class*=\"advertisement\"]",
                placeholder = "输入 CSS 选择器，每行一个"
            ),
            ModuleConfigItem(
                key = "hideMethod",
                name = "隐藏方式",
                description = "选择如何隐藏元素",
                type = ConfigItemType.SELECT,
                defaultValue = "display",
                options = listOf("display", "visibility", "opacity", "remove")
            )
        ),
        configValues = mapOf(
            "selectors" to ".ad-banner\n#popup\n[class*=\"advertisement\"]",
            "hideMethod" to "display"
        ),
        code = """
            const selectors = getConfig('selectors', '').split('\n').filter(s => s.trim());
            const hideMethod = getConfig('hideMethod', 'display');
            
            function hideElements() {
                selectors.forEach(selector => {
                    try {
                        document.querySelectorAll(selector.trim()).forEach(el => {
                            switch(hideMethod) {
                                case 'display':
                                    el.style.setProperty('display', 'none', 'important');
                                    break;
                                case 'visibility':
                                    el.style.setProperty('visibility', 'hidden', 'important');
                                    break;
                                case 'opacity':
                                    el.style.setProperty('opacity', '0', 'important');
                                    el.style.setProperty('pointer-events', 'none', 'important');
                                    break;
                                case 'remove':
                                    el.remove();
                                    break;
                            }
                        });
                    } catch(e) {
                        console.warn('[ElementBlocker] Invalid selector:', selector);
                    }
                });
            }
            
            // 初始执行
            hideElements();
            
            // 监听 DOM 变化
            const observer = new MutationObserver(hideElements);
            observer.observe(document.body, { childList: true, subtree: true });
        """.trimIndent()
    )
    
    /**
     * 深色模式 - 强制页面深色显示
     */
    private fun darkMode() = ExtensionModule(
        id = "builtin-dark-mode",
        name = "深色模式",
        description = "为任意网页强制启用深色模式，保护眼睛",
        icon = "🌙",
        category = ModuleCategory.STYLE_MODIFIER,
        tags = listOf("深色", "护眼", "主题", "夜间"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "brightness",
                name = "亮度",
                description = "页面整体亮度 (0-100)",
                type = ConfigItemType.NUMBER,
                defaultValue = "90"
            ),
            ModuleConfigItem(
                key = "contrast",
                name = "对比度",
                description = "页面对比度 (0-200)",
                type = ConfigItemType.NUMBER,
                defaultValue = "100"
            )
        ),
        configValues = mapOf("brightness" to "90", "contrast" to "100"),
        cssCode = """
            html {
                filter: invert(1) hue-rotate(180deg) !important;
                background: #1a1a1a !important;
            }
            img, video, canvas, svg, [style*="background-image"] {
                filter: invert(1) hue-rotate(180deg) !important;
            }
        """.trimIndent(),
        code = """
            const brightness = parseInt(getConfig('brightness', '90')) / 100;
            const contrast = parseInt(getConfig('contrast', '100')) / 100;
            document.documentElement.style.filter = 
                `invert(1) hue-rotate(180deg) brightness(${'$'}{brightness}) contrast(${'$'}{contrast})`;
        """.trimIndent()
    )
    
    /**
     * 自动滚动 - 页面自动滚动
     */
    private fun autoScroll() = ExtensionModule(
        id = "builtin-auto-scroll",
        name = "自动滚动",
        description = "自动滚动页面，适合阅读长文章",
        icon = "📜",
        category = ModuleCategory.FUNCTION_ENHANCE,
        tags = listOf("滚动", "阅读", "自动化"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "speed",
                name = "滚动速度",
                description = "每秒滚动像素数",
                type = ConfigItemType.NUMBER,
                defaultValue = "50"
            ),
            ModuleConfigItem(
                key = "autoStart",
                name = "自动开始",
                description = "页面加载后自动开始滚动",
                type = ConfigItemType.BOOLEAN,
                defaultValue = "false"
            )
        ),
        configValues = mapOf("speed" to "50", "autoStart" to "false"),
        code = """
            let scrolling = getConfig('autoStart', 'false') === 'true';
            const speed = parseInt(getConfig('speed', '50'));
            let scrollInterval = null;
            
            function startScroll() {
                if (scrollInterval) return;
                scrolling = true;
                scrollInterval = setInterval(() => {
                    window.scrollBy(0, speed / 60);
                }, 1000 / 60);
            }
            
            function stopScroll() {
                scrolling = false;
                if (scrollInterval) {
                    clearInterval(scrollInterval);
                    scrollInterval = null;
                }
            }
            
            function toggleScroll() {
                scrolling ? stopScroll() : startScroll();
            }
            
            // 创建控制按钮
            const btn = document.createElement('div');
            btn.innerHTML = '⏯️';
            btn.style.cssText = `
                position: fixed; bottom: 80px; right: 20px; z-index: 99999;
                width: 50px; height: 50px; border-radius: 50%;
                background: rgba(0,0,0,0.7); color: white;
                display: flex; align-items: center; justify-content: center;
                font-size: 24px; cursor: pointer; user-select: none;
                box-shadow: 0 2px 10px rgba(0,0,0,0.3);
            `;
            btn.onclick = toggleScroll;
            document.body.appendChild(btn);
            
            // 空格键控制
            document.addEventListener('keydown', e => {
                if (e.code === 'Space' && e.target === document.body) {
                    e.preventDefault();
                    toggleScroll();
                }
            });
            
            if (scrolling) startScroll();
        """.trimIndent()
    )
    
    /**
     * 复制保护移除 - 解除网页复制限制
     */
    private fun copyProtectionRemover() = ExtensionModule(
        id = "builtin-copy-protection-remover",
        name = "解除复制限制",
        description = "移除网页的复制保护，允许自由复制文本",
        icon = "📋",
        category = ModuleCategory.FUNCTION_ENHANCE,
        tags = listOf("复制", "解锁", "文本"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        cssCode = """
            * {
                -webkit-user-select: auto !important;
                -moz-user-select: auto !important;
                -ms-user-select: auto !important;
                user-select: auto !important;
            }
        """.trimIndent(),
        code = """
            // 移除事件监听
            const events = ['copy', 'cut', 'paste', 'selectstart', 'contextmenu', 'dragstart'];
            events.forEach(event => {
                document.addEventListener(event, e => e.stopPropagation(), true);
            });
            
            // 移除 oncopy 等属性
            document.querySelectorAll('*').forEach(el => {
                events.forEach(event => {
                    el.removeAttribute('on' + event);
                });
            });
            
            // 覆盖 getSelection
            const originalGetSelection = window.getSelection;
            window.getSelection = function() {
                return originalGetSelection.call(window);
            };
            
            console.log('[CopyProtectionRemover] 复制限制已解除');
        """.trimIndent()
    )
    
    /**
     * 图片下载器 - 长按保存图片
     */
    private fun imageDownloader() = ExtensionModule(
        id = "builtin-image-downloader",
        name = "图片下载器",
        description = "长按图片显示下载按钮，支持保存网页图片",
        icon = "🖼️",
        category = ModuleCategory.MEDIA,
        tags = listOf("图片", "下载", "保存"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOWNLOAD),
        code = """
            let longPressTimer = null;
            let currentImg = null;
            
            document.addEventListener('touchstart', e => {
                const img = e.target.closest('img');
                if (!img) return;
                
                currentImg = img;
                longPressTimer = setTimeout(() => {
                    showDownloadDialog(img.src);
                }, 500);
            });
            
            document.addEventListener('touchend', () => {
                clearTimeout(longPressTimer);
            });
            
            document.addEventListener('touchmove', () => {
                clearTimeout(longPressTimer);
            });
            
            function showDownloadDialog(src) {
                const dialog = document.createElement('div');
                dialog.style.cssText = `
                    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
                    background: rgba(0,0,0,0.8); z-index: 999999;
                    display: flex; flex-direction: column;
                    align-items: center; justify-content: center;
                `;
                
                const img = document.createElement('img');
                img.src = src;
                img.style.cssText = 'max-width: 90%; max-height: 60%; object-fit: contain;';
                
                const btn = document.createElement('a');
                btn.href = src;
                btn.download = 'image_' + Date.now() + '.jpg';
                btn.textContent = '📥 保存图片';
                btn.style.cssText = `
                    margin-top: 20px; padding: 15px 30px;
                    background: #4CAF50; color: white;
                    border-radius: 25px; text-decoration: none;
                    font-size: 18px;
                `;
                
                dialog.appendChild(img);
                dialog.appendChild(btn);
                dialog.onclick = e => {
                    if (e.target === dialog) dialog.remove();
                };
                
                document.body.appendChild(dialog);
            }
        """.trimIndent()
    )

    
    /**
     * 视频倍速控制 - 控制视频播放速度
     */
    private fun videoSpeedController() = ExtensionModule(
        id = "builtin-video-speed",
        name = "视频倍速控制",
        description = "为网页视频添加倍速控制按钮",
        icon = "⏩",
        category = ModuleCategory.MEDIA,
        tags = listOf("视频", "倍速", "播放"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.MEDIA),
        configItems = listOf(
            ModuleConfigItem(
                key = "defaultSpeed",
                name = "默认倍速",
                type = ConfigItemType.SELECT,
                defaultValue = "1",
                options = listOf("0.5", "0.75", "1", "1.25", "1.5", "1.75", "2", "2.5", "3")
            )
        ),
        configValues = mapOf("defaultSpeed" to "1"),
        code = """
            const speeds = [0.5, 0.75, 1, 1.25, 1.5, 1.75, 2, 2.5, 3];
            let currentSpeedIndex = speeds.indexOf(parseFloat(getConfig('defaultSpeed', '1')));
            if (currentSpeedIndex === -1) currentSpeedIndex = 2;
            
            function createSpeedControl(video) {
                if (video.dataset.speedControlAdded) return;
                video.dataset.speedControlAdded = 'true';
                
                const container = document.createElement('div');
                container.style.cssText = `
                    position: absolute; top: 10px; right: 10px; z-index: 9999;
                    background: rgba(0,0,0,0.7); color: white;
                    padding: 5px 10px; border-radius: 5px;
                    font-size: 14px; cursor: pointer;
                    user-select: none;
                `;
                container.textContent = speeds[currentSpeedIndex] + 'x';
                
                container.onclick = () => {
                    currentSpeedIndex = (currentSpeedIndex + 1) % speeds.length;
                    const speed = speeds[currentSpeedIndex];
                    video.playbackRate = speed;
                    container.textContent = speed + 'x';
                };
                
                const parent = video.parentElement;
                if (parent) {
                    parent.style.position = 'relative';
                    parent.appendChild(container);
                }
                
                video.playbackRate = speeds[currentSpeedIndex];
            }
            
            // 处理现有视频
            document.querySelectorAll('video').forEach(createSpeedControl);
            
            // 监听新视频
            const observer = new MutationObserver(mutations => {
                mutations.forEach(mutation => {
                    mutation.addedNodes.forEach(node => {
                        if (node.nodeName === 'VIDEO') {
                            createSpeedControl(node);
                        } else if (node.querySelectorAll) {
                            node.querySelectorAll('video').forEach(createSpeedControl);
                        }
                    });
                });
            });
            observer.observe(document.body, { childList: true, subtree: true });
        """.trimIndent()
    )
    
    /**
     * 增强广告拦截 - 更强力的广告过滤
     */
    private fun adBlockerEnhanced() = ExtensionModule(
        id = "builtin-adblocker-enhanced",
        name = "增强广告拦截",
        description = "更强力的广告过滤，屏蔽常见广告元素和弹窗",
        icon = "🛡️",
        category = ModuleCategory.CONTENT_FILTER,
        tags = listOf("广告", "拦截", "弹窗", "过滤"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        cssCode = """
            /* 常见广告选择器 */
            [class*="ad-"], [class*="ads-"], [class*="advert"],
            [class*="banner"], [class*="popup"], [class*="modal"],
            [id*="ad-"], [id*="ads-"], [id*="advert"],
            [id*="banner"], [id*="popup"],
            iframe[src*="ad"], iframe[src*="banner"],
            ins.adsbygoogle, .adsbygoogle,
            [data-ad], [data-ads], [data-advertisement] {
                display: none !important;
                visibility: hidden !important;
                height: 0 !important;
                width: 0 !important;
                overflow: hidden !important;
            }
        """.trimIndent(),
        code = """
            // 阻止弹窗
            const originalOpen = window.open;
            window.open = function() {
                console.log('[AdBlocker] Blocked popup');
                return null;
            };
            
            // 移除广告元素
            const adSelectors = [
                '[class*="ad-"]', '[class*="ads-"]', '[class*="advert"]',
                '[id*="ad-"]', '[id*="ads-"]', '[id*="advert"]',
                'ins.adsbygoogle', '.adsbygoogle',
                '[data-ad]', '[data-ads]'
            ];
            
            function removeAds() {
                adSelectors.forEach(selector => {
                    try {
                        document.querySelectorAll(selector).forEach(el => {
                            el.style.display = 'none';
                        });
                    } catch(e) {}
                });
            }
            
            removeAds();
            const observer = new MutationObserver(removeAds);
            observer.observe(document.documentElement, { childList: true, subtree: true });
        """.trimIndent()
    )
    
    /**
     * 阅读模式 - 提取正文内容
     */
    private fun readingMode() = ExtensionModule(
        id = "builtin-reading-mode",
        name = "阅读模式",
        description = "提取页面正文，提供清爽的阅读体验",
        icon = "📖",
        category = ModuleCategory.STYLE_MODIFIER,
        tags = listOf("阅读", "正文", "简洁"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_IDLE,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "fontSize",
                name = "字体大小",
                type = ConfigItemType.NUMBER,
                defaultValue = "18"
            ),
            ModuleConfigItem(
                key = "lineHeight",
                name = "行高",
                type = ConfigItemType.NUMBER,
                defaultValue = "1.8"
            ),
            ModuleConfigItem(
                key = "maxWidth",
                name = "最大宽度",
                type = ConfigItemType.NUMBER,
                defaultValue = "800"
            )
        ),
        configValues = mapOf("fontSize" to "18", "lineHeight" to "1.8", "maxWidth" to "800"),
        code = """
            let readingModeEnabled = false;
            let originalContent = null;
            
            function enableReadingMode() {
                if (readingModeEnabled) return;
                
                const fontSize = getConfig('fontSize', '18');
                const lineHeight = getConfig('lineHeight', '1.8');
                const maxWidth = getConfig('maxWidth', '800');
                
                // 保存原始内容
                originalContent = document.body.innerHTML;
                
                // 提取正文
                const article = document.querySelector('article') || 
                               document.querySelector('[class*="content"]') ||
                               document.querySelector('[class*="article"]') ||
                               document.querySelector('main') ||
                               document.body;
                
                const title = document.querySelector('h1')?.textContent || document.title;
                const content = article.innerHTML;
                
                document.body.innerHTML = `
                    <div style="
                        max-width: ${'$'}{maxWidth}px; margin: 0 auto; padding: 20px;
                        font-size: ${'$'}{fontSize}px; line-height: ${'$'}{lineHeight};
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    ">
                        <h1 style="font-size: 1.5em; margin-bottom: 20px;">${'$'}{title}</h1>
                        <div>${'$'}{content}</div>
                        <button id="exitReadingMode" style="
                            position: fixed; bottom: 20px; right: 20px;
                            padding: 10px 20px; background: #333; color: white;
                            border: none; border-radius: 5px; cursor: pointer;
                        ">退出阅读模式</button>
                    </div>
                `;
                
                document.getElementById('exitReadingMode').onclick = disableReadingMode;
                readingModeEnabled = true;
            }
            
            function disableReadingMode() {
                if (!readingModeEnabled || !originalContent) return;
                document.body.innerHTML = originalContent;
                readingModeEnabled = false;
            }
            
            // 创建触发按钮
            const btn = document.createElement('div');
            btn.innerHTML = '📖';
            btn.style.cssText = `
                position: fixed; bottom: 20px; right: 20px; z-index: 99999;
                width: 50px; height: 50px; border-radius: 50%;
                background: rgba(0,0,0,0.7); color: white;
                display: flex; align-items: center; justify-content: center;
                font-size: 24px; cursor: pointer;
                box-shadow: 0 2px 10px rgba(0,0,0,0.3);
            `;
            btn.onclick = enableReadingMode;
            document.body.appendChild(btn);
        """.trimIndent()
    )
    
    /**
     * 自定义字体 - 替换页面字体
     */
    private fun customFont() = ExtensionModule(
        id = "builtin-custom-font",
        name = "自定义字体",
        description = "替换网页字体为指定字体",
        icon = "🔤",
        category = ModuleCategory.STYLE_MODIFIER,
        tags = listOf("字体", "样式", "美化"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "fontFamily",
                name = "字体",
                type = ConfigItemType.SELECT,
                defaultValue = "system-ui",
                options = listOf(
                    "system-ui",
                    "Microsoft YaHei",
                    "PingFang SC",
                    "Noto Sans SC",
                    "Source Han Sans CN",
                    "Helvetica Neue",
                    "Arial",
                    "Georgia",
                    "Times New Roman"
                )
            )
        ),
        configValues = mapOf("fontFamily" to "system-ui"),
        code = """
            const fontFamily = getConfig('fontFamily', 'system-ui');
            const style = document.createElement('style');
            style.textContent = `
                * {
                    font-family: "${'$'}{fontFamily}", -apple-system, BlinkMacSystemFont, sans-serif !important;
                }
            `;
            document.head.appendChild(style);
        """.trimIndent()
    )
    
    /**
     * 翻译助手 - 选中文本翻译
     */
    private fun pageTranslateHelper() = ExtensionModule(
        id = "builtin-translate-helper",
        name = "翻译助手",
        description = "选中文本后显示翻译按钮，快速翻译",
        icon = "🌐",
        category = ModuleCategory.FUNCTION_ENHANCE,
        tags = listOf("翻译", "文本", "工具"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.NETWORK),
        configItems = listOf(
            ModuleConfigItem(
                key = "targetLang",
                name = "目标语言",
                type = ConfigItemType.SELECT,
                defaultValue = "zh-CN",
                options = listOf("zh-CN", "en", "ja", "ko", "fr", "de", "es")
            )
        ),
        configValues = mapOf("targetLang" to "zh-CN"),
        code = """
            const targetLang = getConfig('targetLang', 'zh-CN');
            let tooltip = null;
            
            document.addEventListener('mouseup', e => {
                const selection = window.getSelection();
                const text = selection.toString().trim();
                
                if (tooltip) {
                    tooltip.remove();
                    tooltip = null;
                }
                
                if (!text || text.length > 500) return;
                
                tooltip = document.createElement('div');
                tooltip.style.cssText = `
                    position: fixed; z-index: 999999;
                    background: #333; color: white;
                    padding: 8px 12px; border-radius: 5px;
                    font-size: 14px; cursor: pointer;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.3);
                    left: ${'$'}{e.clientX}px; top: ${'$'}{e.clientY + 10}px;
                `;
                tooltip.textContent = '🌐 翻译';
                tooltip.onclick = () => {
                    const url = `https://translate.google.com/?sl=auto&tl=${'$'}{targetLang}&text=${'$'}{encodeURIComponent(text)}`;
                    window.open(url, '_blank');
                    tooltip.remove();
                };
                
                document.body.appendChild(tooltip);
                
                setTimeout(() => {
                    if (tooltip) tooltip.remove();
                }, 5000);
            });
        """.trimIndent()
    )

    /**
     * 返回顶部按钮 - 添加悬浮返回顶部按钮
     */
    private fun scrollToTopButton() = ExtensionModule(
        id = "builtin-scroll-to-top",
        name = "返回顶部",
        description = "添加悬浮返回顶部按钮，滚动一定距离后显示",
        icon = "⬆️",
        category = ModuleCategory.FUNCTION_ENHANCE,
        tags = listOf("滚动", "导航", "按钮"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "showAfter",
                name = "显示阈值",
                description = "滚动多少像素后显示按钮",
                type = ConfigItemType.NUMBER,
                defaultValue = "300"
            ),
            ModuleConfigItem(
                key = "position",
                name = "按钮位置",
                type = ConfigItemType.SELECT,
                defaultValue = "right",
                options = listOf("left", "right")
            )
        ),
        configValues = mapOf("showAfter" to "300", "position" to "right"),
        code = """
            const showAfter = parseInt(getConfig('showAfter', '300'));
            const position = getConfig('position', 'right');
            
            const btn = document.createElement('div');
            btn.innerHTML = '⬆️';
            btn.style.cssText = `
                position: fixed;
                bottom: 80px;
                ${'$'}{position}: 20px;
                z-index: 99999;
                width: 50px;
                height: 50px;
                border-radius: 50%;
                background: rgba(0,0,0,0.7);
                color: white;
                display: none;
                align-items: center;
                justify-content: center;
                font-size: 24px;
                cursor: pointer;
                box-shadow: 0 2px 10px rgba(0,0,0,0.3);
                transition: opacity 0.3s, transform 0.3s;
            `;
            
            btn.onclick = () => {
                window.scrollTo({ top: 0, behavior: 'smooth' });
            };
            
            btn.onmouseenter = () => { btn.style.transform = 'scale(1.1)'; };
            btn.onmouseleave = () => { btn.style.transform = 'scale(1)'; };
            
            document.body.appendChild(btn);
            
            window.addEventListener('scroll', () => {
                btn.style.display = window.scrollY > showAfter ? 'flex' : 'none';
            });
        """.trimIndent()
    )
    
    /**
     * 页面缩放 - 调整页面缩放比例
     */
    private fun pageZoom() = ExtensionModule(
        id = "builtin-page-zoom",
        name = "页面缩放",
        description = "添加页面缩放控制，支持放大缩小页面内容",
        icon = "🔍",
        category = ModuleCategory.STYLE_MODIFIER,
        tags = listOf("缩放", "放大", "缩小"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "defaultZoom",
                name = "默认缩放",
                description = "默认缩放比例 (%)",
                type = ConfigItemType.NUMBER,
                defaultValue = "100"
            ),
            ModuleConfigItem(
                key = "showControls",
                name = "显示控制按钮",
                type = ConfigItemType.BOOLEAN,
                defaultValue = "true"
            )
        ),
        configValues = mapOf("defaultZoom" to "100", "showControls" to "true"),
        code = """
            let zoom = parseInt(getConfig('defaultZoom', '100'));
            const showControls = getConfig('showControls', 'true') === 'true';
            
            function applyZoom() {
                document.body.style.zoom = zoom + '%';
            }
            
            applyZoom();
            
            if (showControls) {
                const panel = document.createElement('div');
                panel.style.cssText = `
                    position: fixed;
                    bottom: 140px;
                    right: 20px;
                    z-index: 99999;
                    display: flex;
                    flex-direction: column;
                    gap: 5px;
                `;
                
                const btnStyle = `
                    width: 40px;
                    height: 40px;
                    border-radius: 50%;
                    background: rgba(0,0,0,0.7);
                    color: white;
                    border: none;
                    font-size: 18px;
                    cursor: pointer;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                `;
                
                const zoomIn = document.createElement('button');
                zoomIn.innerHTML = '+';
                zoomIn.style.cssText = btnStyle;
                zoomIn.onclick = () => { zoom = Math.min(200, zoom + 10); applyZoom(); };
                
                const zoomOut = document.createElement('button');
                zoomOut.innerHTML = '-';
                zoomOut.style.cssText = btnStyle;
                zoomOut.onclick = () => { zoom = Math.max(50, zoom - 10); applyZoom(); };
                
                const reset = document.createElement('button');
                reset.innerHTML = '⟲';
                reset.style.cssText = btnStyle;
                reset.onclick = () => { zoom = 100; applyZoom(); };
                
                panel.appendChild(zoomIn);
                panel.appendChild(zoomOut);
                panel.appendChild(reset);
                document.body.appendChild(panel);
            }
        """.trimIndent()
    )
    
    /**
     * 自动刷新 - 定时刷新页面
     */
    private fun autoRefresh() = ExtensionModule(
        id = "builtin-auto-refresh",
        name = "自动刷新",
        description = "定时自动刷新页面，适合监控类网页",
        icon = "🔄",
        category = ModuleCategory.FUNCTION_ENHANCE,
        tags = listOf("刷新", "定时", "监控"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "interval",
                name = "刷新间隔",
                description = "刷新间隔（秒）",
                type = ConfigItemType.NUMBER,
                defaultValue = "60"
            ),
            ModuleConfigItem(
                key = "showCountdown",
                name = "显示倒计时",
                type = ConfigItemType.BOOLEAN,
                defaultValue = "true"
            )
        ),
        configValues = mapOf("interval" to "60", "showCountdown" to "true"),
        code = """
            const interval = parseInt(getConfig('interval', '60'));
            const showCountdown = getConfig('showCountdown', 'true') === 'true';
            let countdown = interval;
            let paused = false;
            
            const display = document.createElement('div');
            display.style.cssText = `
                position: fixed;
                top: 10px;
                right: 10px;
                z-index: 99999;
                background: rgba(0,0,0,0.7);
                color: white;
                padding: 8px 12px;
                border-radius: 20px;
                font-size: 12px;
                cursor: pointer;
                display: ${'$'}{showCountdown ? 'block' : 'none'};
            `;
            display.title = '点击暂停/继续';
            display.onclick = () => { paused = !paused; };
            document.body.appendChild(display);
            
            setInterval(() => {
                if (paused) {
                    display.textContent = '⏸ 已暂停';
                    return;
                }
                countdown--;
                display.textContent = '🔄 ' + countdown + 's';
                if (countdown <= 0) {
                    location.reload();
                }
            }, 1000);
        """.trimIndent()
    )
    
    /**
     * 护眼模式 - 降低蓝光
     */
    private fun nightShield() = ExtensionModule(
        id = "builtin-night-shield",
        name = "护眼模式",
        description = "降低屏幕蓝光，保护眼睛",
        icon = "👁️",
        category = ModuleCategory.STYLE_MODIFIER,
        tags = listOf("护眼", "蓝光", "夜间"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_START,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "intensity",
                name = "强度",
                description = "护眼强度 (0-100)",
                type = ConfigItemType.NUMBER,
                defaultValue = "30"
            )
        ),
        configValues = mapOf("intensity" to "30"),
        cssCode = """
            html::after {
                content: '';
                position: fixed;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: rgba(255, 180, 100, var(--night-shield-opacity, 0.3));
                pointer-events: none;
                z-index: 999999;
            }
        """.trimIndent(),
        code = """
            const intensity = parseInt(getConfig('intensity', '30')) / 100;
            document.documentElement.style.setProperty('--night-shield-opacity', intensity);
        """.trimIndent()
    )
    
    /**
     * 快速搜索 - 选中文字快速搜索
     */
    private fun quickSearch() = ExtensionModule(
        id = "builtin-quick-search",
        name = "快速搜索",
        description = "选中文字后显示搜索按钮，快速搜索",
        icon = "🔎",
        category = ModuleCategory.FUNCTION_ENHANCE,
        tags = listOf("搜索", "选中", "快捷"),
        version = ModuleVersion(1, "1.0.0", "初始版本"),
        author = ModuleAuthor("WebToApp"),
        builtIn = true,
        enabled = false,
        runAt = ModuleRunTime.DOCUMENT_END,
        permissions = listOf(ModulePermission.DOM_ACCESS),
        configItems = listOf(
            ModuleConfigItem(
                key = "searchEngine",
                name = "搜索引擎",
                type = ConfigItemType.SELECT,
                defaultValue = "baidu",
                options = listOf("baidu", "google", "bing", "sogou")
            )
        ),
        configValues = mapOf("searchEngine" to "baidu"),
        code = """
            const engines = {
                baidu: 'https://www.baidu.com/s?wd=',
                google: 'https://www.google.com/search?q=',
                bing: 'https://www.bing.com/search?q=',
                sogou: 'https://www.sogou.com/web?query='
            };
            const engine = getConfig('searchEngine', 'baidu');
            const searchUrl = engines[engine] || engines.baidu;
            
            let popup = null;
            
            document.addEventListener('mouseup', e => {
                const selection = window.getSelection();
                const text = selection.toString().trim();
                
                if (popup) {
                    popup.remove();
                    popup = null;
                }
                
                if (!text || text.length > 100) return;
                
                popup = document.createElement('div');
                popup.style.cssText = `
                    position: fixed;
                    z-index: 999999;
                    background: #333;
                    color: white;
                    padding: 8px 16px;
                    border-radius: 20px;
                    font-size: 14px;
                    cursor: pointer;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.3);
                    left: ${'$'}{e.clientX}px;
                    top: ${'$'}{e.clientY + 10}px;
                    display: flex;
                    align-items: center;
                    gap: 6px;
                `;
                popup.innerHTML = '🔎 搜索';
                popup.onclick = () => {
                    window.open(searchUrl + encodeURIComponent(text), '_blank');
                    popup.remove();
                    popup = null;
                };
                
                document.body.appendChild(popup);
                
                setTimeout(() => {
                    if (popup) {
                        popup.remove();
                        popup = null;
                    }
                }, 5000);
            });
        """.trimIndent()
    )
}
