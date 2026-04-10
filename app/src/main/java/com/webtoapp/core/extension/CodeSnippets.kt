package com.webtoapp.core.extension

import com.webtoapp.core.i18n.Strings

/**
 * 代码块库
 * 
 * 提供可复用的代码片段，用户可以在模块编辑器中选择插入
 * 包含 20+ 分类，200+ 代码片段
 */
object CodeSnippets {
    
    /**
     * 获取所有代码块（按分类）
     */
    fun getAll(): List<CodeSnippetCategory> = listOf(
        // 原生能力（新增）
        nativeBridgeOperations(),
        
        // 基础操作
        domOperations(),
        styleOperations(),
        eventListeners(),
        
        // 数据处理
        storageOperations(),
        networkOperations(),
        dataProcessing(),
        
        // UI 组件
        uiComponents(),
        floatingWidgets(),
        notifications(),
        
        // 功能增强
        scrollOperations(),
        formOperations(),
        mediaOperations(),
        
        // Page处理
        pageEnhance(),
        contentFilter(),
        adBlocker(),
        
        // 工具函数
        utilityFunctions(),
        textProcessing(),
        
        // 高级功能
        interceptors(),
        automation(),
        debugging()
    )
    
    /**
     * 根据分类获取代码块
     */
    fun getByCategory(categoryId: String): CodeSnippetCategory? {
        return getAll().find { it.id == categoryId }
    }
    
    /**
     * Search代码块
     */
    fun search(query: String): List<CodeSnippet> {
        val lowerQuery = query.lowercase()
        return getAll().flatMap { it.snippets }.filter { snippet ->
            snippet.name.lowercase().contains(lowerQuery) ||
            snippet.description.lowercase().contains(lowerQuery) ||
            snippet.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }
    
    /**
     * 获取热门代码块
     */
    fun getPopular(): List<CodeSnippet> = listOf(
        getByCategory("native")?.snippets?.find { it.id == "native-save-image" },
        getByCategory("native")?.snippets?.find { it.id == "native-share" },
        getByCategory("dom")?.snippets?.find { it.id == "dom-hide-element" },
        getByCategory("style")?.snippets?.find { it.id == "style-inject-css" },
        getByCategory("ui")?.snippets?.find { it.id == "ui-floating-button" },
        getByCategory("scroll")?.snippets?.find { it.id == "scroll-to-top" },
        getByCategory("adblocker")?.snippets?.find { it.id == "ad-hide-common" },
        getByCategory("events")?.snippets?.find { it.id == "event-mutation" }
    ).filterNotNull()
    
    // ==================== 原生能力 (NativeBridge) ====================
    private fun nativeBridgeOperations() = CodeSnippetCategory(
        id = "native",
        name = Strings.snippetNative,
        icon = "📱",
        description = Strings.snippetNativeDesc,
        snippets = listOf(
            CodeSnippet(
                id = "native-toast",
                name = Strings.snippetShowToast,
                description = Strings.snippetShowToastDesc,
                code = """// 短提示
NativeBridge.showToast('操作成功');

// 长提示
NativeBridge.showToast('请稍候，正在处理...', 'long');""",
                tags = listOf(Strings.tagToast, Strings.tagToast, Strings.tagMessage)
            ),
            CodeSnippet(
                id = "native-vibrate",
                name = Strings.snippetVibrate,
                description = Strings.snippetVibrateDesc,
                code = """// 短震动（100ms）
NativeBridge.vibrate();

// Custom时长震动
NativeBridge.vibrate(500);

// 模式震动（震动-暂停-震动）
NativeBridge.vibratePattern('100,200,100,200');""",
                tags = listOf(Strings.tagVibrate, Strings.tagFeedback, Strings.tagHaptic)
            ),
            CodeSnippet(
                id = "native-copy",
                name = Strings.snippetCopyToClipboard,
                description = Strings.snippetCopyToClipboardDesc,
                code = """function copyText(text) {
    const success = NativeBridge.copyToClipboard(text);
    if (success) {
        NativeBridge.showToast('已复制到剪贴板');
        NativeBridge.vibrate(50);
    } else {
        NativeBridge.showToast('复制失败');
    }
}

// 使用示例：复制选中文本
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('copy-btn')) {
        const text = e.target.dataset.text;
        copyText(text);
    }
});""",
                tags = listOf(Strings.tagCopy, Strings.tagClipboard, Strings.tagClipboard)
            ),
            CodeSnippet(
                id = "native-share",
                name = Strings.snippetShareContent,
                description = Strings.snippetShareContentDesc,
                code = """// 分享文本和链接
function shareContent(title, text, url) {
    NativeBridge.share(title, text, url);
}

// 分享当前页面
function shareCurrentPage() {
    NativeBridge.share(
        document.title,
        '我发现了一个有趣的页面',
        location.href
    );
}

// 添加分享按钮
const shareBtn = document.createElement('button');
shareBtn.textContent = '分享';
shareBtn.onclick = shareCurrentPage;""",
                tags = listOf(Strings.tagShare, Strings.tagShare, Strings.tagSocial)
            ),
            CodeSnippet(
                id = "native-save-image",
                name = Strings.snippetSaveImageToGallery,
                description = Strings.snippetSaveImageToGalleryDesc,
                code = """// Save图片到相册
function saveImage(imageUrl, filename) {
    NativeBridge.saveImageToGallery(imageUrl, filename || '');
}

// 为所有图片添加长按保存功能
document.querySelectorAll('img').forEach(img => {
    img.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        if (confirm('保存图片到相册？')) {
            saveImage(img.src);
        }
    });
});""",
                tags = listOf(Strings.tagSave, Strings.tagImage, Strings.tagGallery, Strings.tagDownload)
            ),
            CodeSnippet(
                id = "native-save-video",
                name = Strings.snippetSaveVideoToGallery,
                description = Strings.snippetSaveVideoToGalleryDesc,
                code = """// Save视频到相册
function saveVideo(videoUrl, filename) {
    NativeBridge.saveVideoToGallery(videoUrl, filename || '');
}

// 为视频添加下载按钮
document.querySelectorAll('video').forEach(video => {
    const btn = document.createElement('button');
    btn.textContent = '保存视频';
    btn.style.cssText = 'position:absolute;top:10px;right:10px;z-index:999;';
    btn.onclick = () => saveVideo(video.src);
    video.parentElement.style.position = 'relative';
    video.parentElement.appendChild(btn);
});""",
                tags = listOf(Strings.tagSave, Strings.tagVideo, Strings.tagGallery, Strings.tagDownload)
            ),
            CodeSnippet(
                id = "native-open-url",
                name = Strings.snippetOpenInBrowser,
                description = Strings.snippetOpenInBrowserDesc,
                code = """// 用系统浏览器打开链接
function openInBrowser(url) {
    NativeBridge.openUrl(url);
}

// 拦截外部链接，用浏览器打开
document.addEventListener('click', (e) => {
    const link = e.target.closest('a');
    if (link && link.href && !link.href.startsWith(location.origin)) {
        e.preventDefault();
        openInBrowser(link.href);
    }
});""",
                tags = listOf(Strings.tagBrowser, Strings.tagLink, Strings.tagExternal)
            ),
            CodeSnippet(
                id = "native-device-info",
                name = Strings.snippetDeviceInfo,
                description = Strings.snippetDeviceInfoDesc,
                code = """// Get设备信息
const deviceInfo = JSON.parse(NativeBridge.getDeviceInfo());
console.log('设备型号:', deviceInfo.model);
console.log('Android 版本:', deviceInfo.androidVersion);
console.log('屏幕尺寸:', deviceInfo.screenWidth, 'x', deviceInfo.screenHeight);

// Get应用信息
const appInfo = JSON.parse(NativeBridge.getAppInfo());
console.log('应用版本:', appInfo.versionName);

// 根据设备调整布局
if (deviceInfo.screenWidth < 400) {
    document.body.classList.add('small-screen');
}""",
                tags = listOf(Strings.tagDevice, Strings.tagInfo, Strings.tagScreen)
            ),
            CodeSnippet(
                id = "native-network",
                name = Strings.snippetNetworkStatus,
                description = Strings.snippetNetworkStatusDesc,
                code = """// Check网络是否可用
if (NativeBridge.isNetworkAvailable()) {
    console.log('网络可用');
} else {
    NativeBridge.showToast('当前无网络连接');
}

// Get网络类型
const networkType = NativeBridge.getNetworkType();
console.log('网络类型:', networkType); // wifi, mobile, none

// 根据网络类型调整行为
if (networkType === 'mobile') {
    // 移动网络下减少数据使用
    document.querySelectorAll('video').forEach(v => v.preload = 'none');
}""",
                tags = listOf(Strings.tagNetwork, Strings.tagWiFi, Strings.tagData)
            ),
            CodeSnippet(
                id = "native-save-file",
                name = Strings.snippetSaveFile,
                description = Strings.snippetSaveFileDesc,
                code = """// Save文本文件
function saveTextFile(content, filename) {
    NativeBridge.saveToFile(content, filename, 'text/plain');
}

// Save JSON 文件
function saveJsonFile(data, filename) {
    const json = JSON.stringify(data, null, 2);
    NativeBridge.saveToFile(json, filename, 'application/json');
}

// Export页面数据
const pageData = {
    title: document.title,
    url: location.href,
    content: document.body.innerText.substring(0, 1000)
};
saveJsonFile(pageData, 'page_data.json');""",
                tags = listOf(Strings.tagSave, Strings.tagFile, Strings.tagExport)
            ),
            CodeSnippet(
                id = "native-image-download-btn",
                name = Strings.snippetImageDownloadBtn,
                description = Strings.snippetImageDownloadBtnDesc,
                code = """// 为所有图片添加下载按钮
function addImageDownloadButtons() {
    document.querySelectorAll('img').forEach(img => {
        if (img.dataset.downloadBtn) return;
        img.dataset.downloadBtn = 'true';
        
        const wrapper = document.createElement('div');
        wrapper.style.cssText = 'position:relative;display:inline-block;';
        
        const btn = document.createElement('button');
        btn.textContent = '💾';
        btn.style.cssText = `
            position: absolute;
            top: 5px;
            right: 5px;
            padding: 5px 10px;
            background: rgba(0,0,0,0.7);
            color: white;
            border: none;
            border-radius: 5px;
            cursor: pointer;
            opacity: 0;
            transition: opacity 0.3s;
            z-index: 100;
        `;
        
        wrapper.onmouseenter = () => btn.style.opacity = '1';
        wrapper.onmouseleave = () => btn.style.opacity = '0';
        btn.onclick = (e) => {
            e.stopPropagation();
            NativeBridge.saveImageToGallery(img.src);
            NativeBridge.vibrate(50);
        };
        
        img.parentNode.insertBefore(wrapper, img);
        wrapper.appendChild(img);
        wrapper.appendChild(btn);
    });
}

addImageDownloadButtons();
new MutationObserver(addImageDownloadButtons)
    .observe(document.body, { childList: true, subtree: true });""",
                tags = listOf(Strings.tagImage, Strings.tagDownload, Strings.tagButton, Strings.tagFloating)
            )
        )
    )

    // ==================== DOM 操作 ====================
    private fun domOperations() = CodeSnippetCategory(
        id = "dom",
        name = Strings.snippetDom,
        icon = "🔧",
        description = Strings.snippetDomDesc,
        snippets = listOf(
            CodeSnippet(
                id = "dom-query-single",
                name = Strings.snippetQuerySingle,
                description = Strings.snippetQuerySingleDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    // 对元素进行操作
}""",
                tags = listOf(Strings.tagQuery, Strings.tagSelector)
            ),
            CodeSnippet(
                id = "dom-query-all",
                name = Strings.snippetQueryAll,
                description = Strings.snippetQueryAllDesc,
                code = """document.querySelectorAll('选择器').forEach(el => {
    // 对每个元素进行操作
});""",
                tags = listOf(Strings.tagQuery, Strings.tagIterate)
            ),
            CodeSnippet(
                id = "dom-hide-element",
                name = Strings.snippetHideElement,
                description = Strings.snippetHideElementDesc,
                code = """function hideElement(selector) {
    document.querySelectorAll(selector).forEach(el => {
        el.style.setProperty('display', 'none', 'important');
    });
}
hideElement('选择器');""",
                tags = listOf(Strings.tagHide, Strings.tagStyle)
            ),
            CodeSnippet(
                id = "dom-remove-element",
                name = Strings.snippetRemoveElement,
                description = Strings.snippetRemoveElementDesc,
                code = """function removeElement(selector) {
    document.querySelectorAll(selector).forEach(el => el.remove());
}
removeElement('选择器');""",
                tags = listOf(Strings.tagDelete, Strings.tagRemove)
            ),
            CodeSnippet(
                id = "dom-create-element",
                name = Strings.snippetCreateElement,
                description = Strings.snippetCreateElementDesc,
                code = """const newElement = document.createElement('div');
newElement.id = 'my-element';
newElement.className = 'my-class';
newElement.textContent = '内容';
newElement.style.cssText = 'color: red; font-size: 14px;';
document.body.appendChild(newElement);""",
                tags = listOf(Strings.tagCreate, Strings.tagAdd)
            ),
            CodeSnippet(
                id = "dom-modify-text",
                name = Strings.snippetModifyText,
                description = Strings.snippetModifyTextDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    element.textContent = '新的文本内容';
    // 或者使用 innerHTML 支持 HTML
    // element.innerHTML = '<strong>加粗文本</strong>';
}""",
                tags = listOf(Strings.tagText, Strings.tagModify)
            ),
            CodeSnippet(
                id = "dom-modify-attribute",
                name = Strings.snippetModifyAttr,
                description = Strings.snippetModifyAttrDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    element.setAttribute('属性名', '属性值');
    const value = element.getAttribute('属性名');
    element.removeAttribute('属性名');
}""",
                tags = listOf(Strings.tagAttribute, Strings.tagModify)
            ),
            CodeSnippet(
                id = "dom-insert-before",
                name = Strings.snippetInsertBefore,
                description = Strings.snippetInsertBeforeDesc,
                code = """const target = document.querySelector('目标选择器');
const newEl = document.createElement('div');
newEl.textContent = '新内容';
target.parentNode.insertBefore(newEl, target);""",
                tags = listOf(Strings.tagInsert, Strings.tagPosition)
            ),
            CodeSnippet(
                id = "dom-insert-after",
                name = Strings.snippetInsertAfter,
                description = Strings.snippetInsertAfterDesc,
                code = """const target = document.querySelector('目标选择器');
const newEl = document.createElement('div');
newEl.textContent = '新内容';
target.parentNode.insertBefore(newEl, target.nextSibling);""",
                tags = listOf(Strings.tagInsert, Strings.tagPosition)
            ),
            CodeSnippet(
                id = "dom-clone-element",
                name = Strings.snippetCloneElement,
                description = Strings.snippetCloneElementDesc,
                code = """const original = document.querySelector('选择器');
const clone = original.cloneNode(true);
clone.id = 'cloned-element';
document.body.appendChild(clone);""",
                tags = listOf(Strings.tagClone, Strings.tagCopy)
            ),
            CodeSnippet(
                id = "dom-wrap-element",
                name = Strings.snippetWrapElement,
                description = Strings.snippetWrapElementDesc,
                code = """function wrapElement(selector, wrapperTag = 'div') {
    document.querySelectorAll(selector).forEach(el => {
        const wrapper = document.createElement(wrapperTag);
        el.parentNode.insertBefore(wrapper, el);
        wrapper.appendChild(el);
    });
}
wrapElement('img', 'figure');""",
                tags = listOf(Strings.tagWrap, Strings.tagStructure)
            ),
            CodeSnippet(
                id = "dom-replace-element",
                name = Strings.snippetReplaceElement,
                description = Strings.snippetReplaceElementDesc,
                code = """function replaceElement(selector, newHtml) {
    document.querySelectorAll(selector).forEach(el => {
        const temp = document.createElement('div');
        temp.innerHTML = newHtml;
        el.replaceWith(temp.firstChild);
    });
}
replaceElement('.old-class', '<div class="new-class">新内容</div>');""",
                tags = listOf(Strings.tagReplace, Strings.tagModify)
            )
        )
    )
    
    // ==================== 样式操作 ====================
    private fun styleOperations() = CodeSnippetCategory(
        id = "style",
        name = Strings.snippetStyle,
        icon = "🎨",
        description = Strings.snippetStyleDesc,
        snippets = listOf(
            CodeSnippet(
                id = "style-inject-css",
                name = Strings.snippetInjectCss,
                description = Strings.snippetInjectCssDesc,
                code = """const style = document.createElement('style');
style.id = 'my-custom-style';
style.textContent = `
    .my-class {
        color: red !important;
        font-size: 16px !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagCSS, Strings.tagInject)
            ),
            CodeSnippet(
                id = "style-modify-inline",
                name = Strings.snippetModifyInline,
                description = Strings.snippetModifyInlineDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    element.style.color = 'red';
    element.style.fontSize = '16px';
    element.style.setProperty('display', 'block', 'important');
}""",
                tags = listOf(Strings.tagStyle, Strings.tagInline)
            ),
            CodeSnippet(
                id = "style-add-class",
                name = Strings.snippetAddClass,
                description = Strings.snippetAddClassDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    element.classList.add('new-class');
    element.classList.remove('old-class');
    element.classList.toggle('toggle-class');
    const hasClass = element.classList.contains('some-class');
}""",
                tags = listOf(Strings.tagClassName, Strings.tagClassName)
            ),
            CodeSnippet(
                id = "style-dark-mode",
                name = Strings.snippetDarkMode,
                description = Strings.snippetDarkModeDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    html {
        filter: invert(1) hue-rotate(180deg) !important;
        background: #1a1a1a !important;
    }
    img, video, canvas, svg, [style*="background-image"] {
        filter: invert(1) hue-rotate(180deg) !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagDark, Strings.tagTheme)
            ),
            CodeSnippet(
                id = "style-sepia-mode",
                name = Strings.snippetSepiaMode,
                description = Strings.snippetSepiaModeDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    html {
        filter: sepia(30%) brightness(95%) !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagEyeCare, Strings.tagWarm)
            ),
            CodeSnippet(
                id = "style-grayscale",
                name = Strings.snippetGrayscale,
                description = Strings.snippetGrayscaleDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    html {
        filter: grayscale(100%) !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagGrayscale, Strings.tagFilter)
            ),
            CodeSnippet(
                id = "style-custom-font",
                name = Strings.snippetCustomFont,
                description = Strings.snippetCustomFontDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    * {
        font-family: "Microsoft YaHei", "PingFang SC", sans-serif !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagFont, Strings.tagStyle)
            ),
            CodeSnippet(
                id = "style-font-size",
                name = Strings.snippetFontSize,
                description = Strings.snippetFontSizeDesc,
                code = """function setFontSize(size) {
    const style = document.createElement('style');
    style.textContent = `
        body, p, span, div, a, li {
            font-size: ${"$"}{size}px !important;
        }
    `;
    document.head.appendChild(style);
}
setFontSize(16);""",
                tags = listOf(Strings.tagFont, Strings.tagSize)
            ),
            CodeSnippet(
                id = "style-hide-scrollbar",
                name = Strings.snippetHideScrollbar,
                description = Strings.snippetHideScrollbarDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    ::-webkit-scrollbar { display: none !important; }
    * { scrollbar-width: none !important; }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagScrollbar, Strings.tagHide)
            ),
            CodeSnippet(
                id = "style-highlight-links",
                name = Strings.snippetHighlightLinks,
                description = Strings.snippetHighlightLinksDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    a {
        background: yellow !important;
        color: #000 !important;
        padding: 2px 4px !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagLink, Strings.tagHighlight)
            ),
            CodeSnippet(
                id = "style-max-width",
                name = Strings.snippetMaxWidth,
                description = Strings.snippetMaxWidthDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    body > * {
        max-width: 800px !important;
        margin-left: auto !important;
        margin-right: auto !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagWidth, Strings.tagReading)
            ),
            CodeSnippet(
                id = "style-line-height",
                name = Strings.snippetLineHeight,
                description = Strings.snippetLineHeightDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    p, li, span, div {
        line-height: 1.8 !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(Strings.tagLineHeight, Strings.tagReading)
            )
        )
    )

    
    // ==================== 事件监听 ====================
    private fun eventListeners() = CodeSnippetCategory(
        id = "events",
        name = Strings.snippetEvent,
        icon = "👆",
        description = Strings.snippetEventDesc,
        snippets = listOf(
            CodeSnippet(
                id = "event-click",
                name = Strings.snippetClickEvent,
                description = Strings.snippetClickEventDesc,
                code = """document.addEventListener('click', (e) => {
    const target = e.target;
    if (target.matches('选择器')) {
        e.preventDefault();
        // Handle点击
    }
});""",
                tags = listOf(Strings.tagClick, Strings.tagEvent)
            ),
            CodeSnippet(
                id = "event-keyboard",
                name = Strings.snippetKeyboardEvent,
                description = Strings.snippetKeyboardEventDesc,
                code = """document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        // Handle回车键
    }
    if (e.ctrlKey && e.key === 's') {
        e.preventDefault();
        // Handle Ctrl+S
    }
});""",
                tags = listOf(Strings.tagKeyboard, Strings.tagShortcut)
            ),
            CodeSnippet(
                id = "event-scroll",
                name = Strings.snippetScrollEvent,
                description = Strings.snippetScrollEventDesc,
                code = """let lastScrollTop = 0;
window.addEventListener('scroll', () => {
    const scrollTop = window.scrollY;
    const direction = scrollTop > lastScrollTop ? 'down' : 'up';
    lastScrollTop = scrollTop;
    
    if (scrollTop > 300) {
        // Show返回顶部按钮
    }
});""",
                tags = listOf(Strings.tagScroll, Strings.tagPosition)
            ),
            CodeSnippet(
                id = "event-mutation",
                name = Strings.snippetMutationEvent,
                description = Strings.snippetMutationEventDesc,
                code = """const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
            if (node.nodeType === 1) {
                // Handle新添加的元素
                if (node.matches && node.matches('.ad')) {
                    node.remove();
                }
            }
        });
    });
});

observer.observe(document.body, {
    childList: true,
    subtree: true
});""",
                tags = listOf(Strings.tagListen, Strings.tagDomChange, Strings.tagDynamic)
            ),
            CodeSnippet(
                id = "event-resize",
                name = Strings.snippetResizeEvent,
                description = Strings.snippetResizeEventDesc,
                code = """let resizeTimeout;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(() => {
        const width = window.innerWidth;
        const height = window.innerHeight;
        // Handle窗口大小变化
    }, 100);
});""",
                tags = listOf(Strings.tagWindow, Strings.tagSize)
            ),
            CodeSnippet(
                id = "event-copy",
                name = Strings.snippetCopyEvent,
                description = Strings.snippetCopyEventDesc,
                code = """document.addEventListener('copy', (e) => {
    const selection = window.getSelection().toString();
    e.clipboardData.setData('text/plain', selection + '\\n来源: ' + location.href);
    e.preventDefault();
});""",
                tags = listOf(Strings.tagCopy, Strings.tagClipboard)
            ),
            CodeSnippet(
                id = "event-contextmenu",
                name = Strings.snippetContextMenu,
                description = Strings.snippetContextMenuDesc,
                code = """document.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    // Show自定义菜单或阻止默认菜单
});""",
                tags = listOf(Strings.tagRightClick, Strings.tagMenu)
            ),
            CodeSnippet(
                id = "event-visibility",
                name = Strings.snippetVisibility,
                description = Strings.snippetVisibilityDesc,
                code = """document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        // Page切换到后台
        console.log('页面隐藏');
    } else {
        // Page切换到前台
        console.log('页面显示');
    }
});""",
                tags = listOf(Strings.tagVisibility, Strings.tagBackground)
            ),
            CodeSnippet(
                id = "event-beforeunload",
                name = Strings.snippetBeforeUnload,
                description = Strings.snippetBeforeUnloadDesc,
                code = """window.addEventListener('beforeunload', (e) => {
    // Save数据
    localStorage.setItem('lastVisit', Date.now());
    
    // 如需提示用户，取消注释以下代码
    // e.preventDefault();
    // e.returnValue = '';
});""",
                tags = listOf(Strings.tagClose, Strings.tagSave)
            ),
            CodeSnippet(
                id = "event-touch",
                name = Strings.snippetTouchEvent,
                description = Strings.snippetTouchEventDesc,
                code = """let startX, startY;
document.addEventListener('touchstart', (e) => {
    startX = e.touches[0].clientX;
    startY = e.touches[0].clientY;
});

document.addEventListener('touchend', (e) => {
    const endX = e.changedTouches[0].clientX;
    const endY = e.changedTouches[0].clientY;
    const diffX = endX - startX;
    const diffY = endY - startY;
    
    if (Math.abs(diffX) > Math.abs(diffY)) {
        if (diffX > 50) console.log('右滑');
        else if (diffX < -50) console.log('左滑');
    } else {
        if (diffY > 50) console.log('下滑');
        else if (diffY < -50) console.log('上滑');
    }
});""",
                tags = listOf(Strings.tagTouch, Strings.tagGesture)
            ),
            CodeSnippet(
                id = "event-long-press",
                name = Strings.snippetLongPress,
                description = Strings.snippetLongPressDesc,
                code = """let pressTimer = null;
document.addEventListener('touchstart', (e) => {
    pressTimer = setTimeout(() => {
        // 长按触发
        console.log('长按:', e.target);
    }, 500);
});

document.addEventListener('touchend', () => {
    clearTimeout(pressTimer);
});

document.addEventListener('touchmove', () => {
    clearTimeout(pressTimer);
});""",
                tags = listOf(Strings.tagLongPress, Strings.tagTouch)
            )
        )
    )
    
    // ==================== 存储操作 ====================
    private fun storageOperations() = CodeSnippetCategory(
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
// 读取
const value = sessionStorage.getItem('key');
// Delete
sessionStorage.removeItem('key');
// 清空所有
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
    
    // ==================== 网络请求 ====================
    private fun networkOperations() = CodeSnippetCategory(
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

// Download Blob 数据
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

    
    // ==================== 数据处理 ====================
    private fun dataProcessing() = CodeSnippetCategory(
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
    
    // ==================== UI 组件 ====================
    private fun uiComponents() = CodeSnippetCategory(
        id = "ui",
        name = Strings.snippetUi,
        icon = "🎯",
        description = Strings.snippetUiDesc,
        snippets = listOf(
            CodeSnippet(
                id = "ui-floating-button",
                name = Strings.snippetFloatingButton,
                description = Strings.snippetFloatingButtonDesc,
                code = """function createFloatingButton(text, onClick, position = 'bottom-right') {
    const btn = document.createElement('div');
    btn.textContent = text;
    const positions = {
        'bottom-right': 'bottom: 80px; right: 20px;',
        'bottom-left': 'bottom: 80px; left: 20px;',
        'top-right': 'top: 80px; right: 20px;',
        'top-left': 'top: 80px; left: 20px;'
    };
    btn.style.cssText = `
        position: fixed; ${"$"}{positions[position]} z-index: 99999;
        padding: 12px 20px; background: rgba(0,0,0,0.8); color: white;
        border-radius: 25px; cursor: pointer; font-size: 14px;
        box-shadow: 0 2px 10px rgba(0,0,0,0.3); transition: transform 0.2s;
    `;
    btn.onmouseenter = () => btn.style.transform = 'scale(1.05)';
    btn.onmouseleave = () => btn.style.transform = 'scale(1)';
    btn.onclick = onClick;
    document.body.appendChild(btn);
    return btn;
}
createFloatingButton('⬆️', () => window.scrollTo({top: 0, behavior: 'smooth'}));""",
                tags = listOf(Strings.tagButton, Strings.tagFloating)
            ),
            CodeSnippet(
                id = "ui-toast",
                name = Strings.snippetToastUi,
                description = Strings.snippetToastUiDesc,
                code = """function showToast(message, duration = 3000) {
    const toast = document.createElement('div');
    toast.textContent = message;
    toast.style.cssText = `
        position: fixed; bottom: 100px; left: 50%; transform: translateX(-50%);
        z-index: 999999; padding: 12px 24px; background: rgba(0,0,0,0.8);
        color: white; border-radius: 25px; font-size: 14px;
        animation: fadeIn 0.3s;
    `;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s';
        setTimeout(() => toast.remove(), 300);
    }, duration);
}
showToast('操作成功！');""",
                tags = listOf(Strings.tagToast, Strings.tagMessage)
            ),
            CodeSnippet(
                id = "ui-modal",
                name = Strings.snippetModal,
                description = Strings.snippetModalDesc,
                code = """function showModal(title, content, onConfirm) {
    const overlay = document.createElement('div');
    overlay.style.cssText = `
        position: fixed; top: 0; left: 0; right: 0; bottom: 0;
        background: rgba(0,0,0,0.5); z-index: 999998;
        display: flex; align-items: center; justify-content: center;
    `;
    
    overlay.innerHTML = `
        <div style="background: white; border-radius: 12px; padding: 20px;
            min-width: 300px; max-width: 80%; box-shadow: 0 10px 40px rgba(0,0,0,0.3);">
            <h3 style="margin: 0 0 15px 0;">${"$"}{title}</h3>
            <div style="margin-bottom: 20px;">${"$"}{content}</div>
            <div style="text-align: right;">
                <button id="modal-cancel" style="padding: 8px 16px; margin-right: 10px;">取消</button>
                <button id="modal-confirm" style="padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 5px;">确定</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(overlay);
    overlay.querySelector('#modal-cancel').onclick = () => overlay.remove();
    overlay.querySelector('#modal-confirm').onclick = () => { onConfirm?.(); overlay.remove(); };
    overlay.onclick = (e) => e.target === overlay && overlay.remove();
}
showModal('提示', '确定要执行此操作吗？', () => console.log('确认'));""",
                tags = listOf(Strings.tagPopup, Strings.tagDialog)
            ),
            CodeSnippet(
                id = "ui-progress-bar",
                name = Strings.snippetProgressBar,
                description = Strings.snippetProgressBarDesc,
                code = """const progressBar = document.createElement('div');
progressBar.style.cssText = `
    position: fixed; top: 0; left: 0; height: 3px;
    background: linear-gradient(90deg, #007bff, #00d4ff);
    z-index: 999999; transition: width 0.1s; width: 0%;
`;
document.body.appendChild(progressBar);

window.addEventListener('scroll', () => {
    const scrollTop = window.scrollY;
    const docHeight = document.documentElement.scrollHeight - window.innerHeight;
    const progress = (scrollTop / docHeight) * 100;
    progressBar.style.width = progress + '%';
});""",
                tags = listOf(Strings.tagProgress, Strings.tagReading)
            ),
            CodeSnippet(
                id = "ui-loading",
                name = Strings.snippetLoading,
                description = Strings.snippetLoadingDesc,
                code = """function showLoading(message = '加载中...') {
    const loading = document.createElement('div');
    loading.id = 'custom-loading';
    loading.innerHTML = `
        <div style="position: fixed; top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(255,255,255,0.9); z-index: 999999;
            display: flex; flex-direction: column; align-items: center; justify-content: center;">
            <div style="width: 40px; height: 40px; border: 3px solid #f3f3f3;
                border-top: 3px solid #007bff; border-radius: 50%;
                animation: spin 1s linear infinite;"></div>
            <p style="margin-top: 15px; color: #666;">${"$"}{message}</p>
        </div>
    `;
    document.body.appendChild(loading);
}

function hideLoading() {
    document.getElementById('custom-loading')?.remove();
}""",
                tags = listOf(Strings.tagLoading, Strings.tagAnimation)
            ),
            CodeSnippet(
                id = "ui-snackbar",
                name = Strings.snippetSnackbar,
                description = Strings.snippetSnackbarDesc,
                code = """function showSnackbar(message, action, onAction) {
    const snackbar = document.createElement('div');
    snackbar.style.cssText = `
        position: fixed; bottom: 20px; left: 50%; transform: translateX(-50%) translateY(100px);
        background: #323232; color: white; padding: 14px 24px; border-radius: 4px;
        display: flex; align-items: center; gap: 24px; z-index: 999999;
        transition: transform 0.3s ease;
    `;
    snackbar.innerHTML = `
        <span>${"$"}{message}</span>
        ${"$"}{action ? '<button style="background: none; border: none; color: #bb86fc; cursor: pointer; font-weight: bold;">' + action + '</button>' : ''}
    `;
    document.body.appendChild(snackbar);
    
    setTimeout(() => snackbar.style.transform = 'translateX(-50%) translateY(0)', 10);
    
    if (action) {
        snackbar.querySelector('button').onclick = () => { onAction?.(); snackbar.remove(); };
    }
    
    setTimeout(() => {
        snackbar.style.transform = 'translateX(-50%) translateY(100px)';
        setTimeout(() => snackbar.remove(), 300);
    }, 4000);
}
showSnackbar('文件已删除', '撤销', () => console.log('撤销'));""",
                tags = listOf(Strings.tagNotification, Strings.tagSnackbar)
            )
        )
    )
    
    // ==================== 悬浮组件 ====================
    private fun floatingWidgets() = CodeSnippetCategory(
        id = "widgets",
        name = Strings.snippetWidget,
        icon = "🔲",
        description = Strings.snippetWidgetDesc,
        snippets = listOf(
            CodeSnippet(
                id = "widget-toolbar",
                name = Strings.snippetToolbar,
                description = Strings.snippetToolbarDesc,
                code = """function createToolbar(buttons) {
    const toolbar = document.createElement('div');
    toolbar.style.cssText = `
        position: fixed; bottom: 100px; right: 20px; z-index: 99999;
        background: white; border-radius: 30px; padding: 8px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.15);
        display: flex; flex-direction: column; gap: 8px;
    `;
    
    buttons.forEach(({ icon, title, onClick }) => {
        const btn = document.createElement('button');
        btn.innerHTML = icon;
        btn.title = title;
        btn.style.cssText = `
            width: 44px; height: 44px; border: none; border-radius: 50%;
            background: #f5f5f5; cursor: pointer; font-size: 20px;
            transition: background 0.2s;
        `;
        btn.onmouseenter = () => btn.style.background = '#e0e0e0';
        btn.onmouseleave = () => btn.style.background = '#f5f5f5';
        btn.onclick = onClick;
        toolbar.appendChild(btn);
    });
    
    document.body.appendChild(toolbar);
    return toolbar;
}

createToolbar([
    { icon: '⬆️', title: '返回顶部', onClick: () => window.scrollTo({top: 0, behavior: 'smooth'}) },
    { icon: '🌙', title: '深色模式', onClick: () => document.body.classList.toggle('dark') },
    { icon: '📖', title: '阅读模式', onClick: () => console.log('阅读模式') }
]);""",
                tags = listOf(Strings.tagToolbar, Strings.tagFloating)
            ),
            CodeSnippet(
                id = "widget-sidebar",
                name = Strings.snippetSidebar,
                description = Strings.snippetSidebarDesc,
                code = """function createSidebar(content) {
    const sidebar = document.createElement('div');
    sidebar.style.cssText = `
        position: fixed; top: 0; right: -300px; width: 300px; height: 100%;
        background: white; z-index: 999999; transition: right 0.3s;
        box-shadow: -2px 0 10px rgba(0,0,0,0.1); overflow-y: auto;
    `;
    sidebar.innerHTML = `
        <div style="padding: 20px;">
            <button id="close-sidebar" style="position: absolute; top: 10px; right: 10px;
                background: none; border: none; font-size: 24px; cursor: pointer;">×</button>
            ${"$"}{content}
        </div>
    `;
    
    const toggle = document.createElement('button');
    toggle.innerHTML = '☰';
    toggle.style.cssText = `
        position: fixed; top: 50%; right: 0; transform: translateY(-50%);
        z-index: 999998; padding: 10px; background: #007bff; color: white;
        border: none; border-radius: 5px 0 0 5px; cursor: pointer;
    `;
    
    let isOpen = false;
    toggle.onclick = () => {
        isOpen = !isOpen;
        sidebar.style.right = isOpen ? '0' : '-300px';
    };
    sidebar.querySelector('#close-sidebar').onclick = () => {
        isOpen = false;
        sidebar.style.right = '-300px';
    };
    
    document.body.appendChild(sidebar);
    document.body.appendChild(toggle);
}
createSidebar('<h3>设置</h3><p>这里是侧边栏内容</p>');""",
                tags = listOf(Strings.tagSidebar, Strings.tagPanel)
            ),
            CodeSnippet(
                id = "widget-draggable",
                name = Strings.snippetDraggable,
                description = Strings.snippetDraggableDesc,
                code = """function makeDraggable(element) {
    let isDragging = false;
    let offsetX, offsetY;
    
    element.style.cursor = 'move';
    element.style.userSelect = 'none';
    
    element.addEventListener('mousedown', (e) => {
        isDragging = true;
        offsetX = e.clientX - element.offsetLeft;
        offsetY = e.clientY - element.offsetTop;
    });
    
    document.addEventListener('mousemove', (e) => {
        if (!isDragging) return;
        element.style.left = (e.clientX - offsetX) + 'px';
        element.style.top = (e.clientY - offsetY) + 'px';
        element.style.right = 'auto';
        element.style.bottom = 'auto';
    });
    
    document.addEventListener('mouseup', () => {
        isDragging = false;
    });
}
// makeDraggable(document.querySelector('.my-widget'));""",
                tags = listOf(Strings.tagDrag, Strings.tagInteraction)
            ),
            CodeSnippet(
                id = "widget-mini-player",
                name = Strings.snippetMiniPlayer,
                description = Strings.snippetMiniPlayerDesc,
                code = """function createMiniPlayer() {
    const player = document.createElement('div');
    player.style.cssText = `
        position: fixed; bottom: 20px; right: 20px; z-index: 99999;
        background: #1a1a1a; color: white; border-radius: 12px;
        padding: 15px; width: 280px; box-shadow: 0 4px 20px rgba(0,0,0,0.3);
    `;
    player.innerHTML = `
        <div style="display: flex; align-items: center; gap: 12px;">
            <div style="width: 50px; height: 50px; background: #333; border-radius: 8px;"></div>
            <div style="flex: 1;">
                <div style="font-weight: bold;">歌曲名称</div>
                <div style="font-size: 12px; color: #888;">艺术家</div>
            </div>
        </div>
        <div style="display: flex; justify-content: center; gap: 20px; margin-top: 15px;">
            <button style="background: none; border: none; color: white; font-size: 20px; cursor: pointer;">⏮</button>
            <button style="background: none; border: none; color: white; font-size: 24px; cursor: pointer;">▶️</button>
            <button style="background: none; border: none; color: white; font-size: 20px; cursor: pointer;">⏭</button>
        </div>
    `;
    document.body.appendChild(player);
    return player;
}""",
                tags = listOf(Strings.tagPlayer, Strings.tagMusic)
            )
        )
    )

    
    // ==================== 通知系统 ====================
    private fun notifications() = CodeSnippetCategory(
        id = "notifications",
        name = Strings.snippetNotification,
        icon = "🔔",
        description = Strings.snippetNotificationDesc,
        snippets = listOf(
            CodeSnippet(
                id = "notif-browser",
                name = Strings.snippetBrowserNotif,
                description = Strings.snippetBrowserNotifDesc,
                code = """async function sendNotification(title, body, icon) {
    if (Notification.permission !== 'granted') {
        await Notification.requestPermission();
    }
    
    if (Notification.permission === 'granted') {
        new Notification(title, { body, icon });
    }
}
sendNotification('提醒', '这是一条通知消息');""",
                tags = listOf(Strings.tagNotification, Strings.tagBrowser)
            ),
            CodeSnippet(
                id = "notif-badge",
                name = Strings.snippetBadge,
                description = Strings.snippetBadgeDesc,
                code = """function addBadge(element, count) {
    let badge = element.querySelector('.badge');
    if (!badge) {
        badge = document.createElement('span');
        badge.className = 'badge';
        badge.style.cssText = `
            position: absolute; top: -8px; right: -8px;
            background: #ff4444; color: white; font-size: 12px;
            min-width: 18px; height: 18px; border-radius: 9px;
            display: flex; align-items: center; justify-content: center;
        `;
        element.style.position = 'relative';
        element.appendChild(badge);
    }
    badge.textContent = count > 99 ? '99+' : count;
    badge.style.display = count > 0 ? 'flex' : 'none';
}""",
                tags = listOf(Strings.tagBadge, Strings.tagNumber)
            ),
            CodeSnippet(
                id = "notif-alert-banner",
                name = Strings.snippetBanner,
                description = Strings.snippetBannerDesc,
                code = """function showBanner(message, type = 'info') {
    const colors = {
        info: '#2196F3',
        success: '#4CAF50',
        warning: '#FF9800',
        error: '#f44336'
    };
    
    const banner = document.createElement('div');
    banner.style.cssText = `
        position: fixed; top: 0; left: 0; right: 0; z-index: 999999;
        background: ${"$"}{colors[type]}; color: white; padding: 12px 20px;
        text-align: center; transform: translateY(-100%);
        transition: transform 0.3s ease;
    `;
    banner.innerHTML = `
        ${"$"}{message}
        <button onclick="this.parentElement.remove()" style="
            position: absolute; right: 10px; top: 50%; transform: translateY(-50%);
            background: none; border: none; color: white; font-size: 20px; cursor: pointer;
        ">×</button>
    `;
    
    document.body.appendChild(banner);
    setTimeout(() => banner.style.transform = 'translateY(0)', 10);
    setTimeout(() => {
        banner.style.transform = 'translateY(-100%)';
        setTimeout(() => banner.remove(), 300);
    }, 5000);
}
showBanner('这是一条提示信息', 'success');""",
                tags = listOf(Strings.tagBanner, Strings.tagReminder)
            )
        )
    )
    
    // ==================== 滚动操作 ====================
    private fun scrollOperations() = CodeSnippetCategory(
        id = "scroll",
        name = Strings.snippetScroll,
        icon = "📜",
        description = Strings.snippetScrollDesc,
        snippets = listOf(
            CodeSnippet(
                id = "scroll-to-top",
                name = Strings.snippetScrollToTop,
                description = Strings.snippetScrollToTopDesc,
                code = """function scrollToTop(smooth = true) {
    window.scrollTo({
        top: 0,
        behavior: smooth ? 'smooth' : 'auto'
    });
}
scrollToTop();""",
                tags = listOf(Strings.tagScroll, Strings.tagTop)
            ),
            CodeSnippet(
                id = "scroll-to-bottom",
                name = Strings.snippetScrollToBottom,
                description = Strings.snippetScrollToBottomDesc,
                code = """function scrollToBottom(smooth = true) {
    window.scrollTo({
        top: document.documentElement.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
    });
}
scrollToBottom();""",
                tags = listOf(Strings.tagScroll, Strings.tagBottom)
            ),
            CodeSnippet(
                id = "scroll-to-element",
                name = Strings.snippetScrollToElement,
                description = Strings.snippetScrollToElementDesc,
                code = """function scrollToElement(selector, offset = 0) {
    const element = document.querySelector(selector);
    if (element) {
        const top = element.getBoundingClientRect().top + window.scrollY - offset;
        window.scrollTo({ top, behavior: 'smooth' });
    }
}
scrollToElement('#target-section', 100);""",
                tags = listOf(Strings.tagScroll, Strings.tagElement)
            ),
            CodeSnippet(
                id = "scroll-auto",
                name = Strings.snippetAutoScroll,
                description = Strings.snippetAutoScrollDesc,
                code = """let autoScrolling = false;
let scrollInterval;

function startAutoScroll(speed = 1) {
    if (autoScrolling) return;
    autoScrolling = true;
    scrollInterval = setInterval(() => {
        window.scrollBy(0, speed);
        if (window.scrollY + window.innerHeight >= document.documentElement.scrollHeight) {
            stopAutoScroll();
        }
    }, 16);
}

function stopAutoScroll() {
    autoScrolling = false;
    clearInterval(scrollInterval);
}

function toggleAutoScroll(speed = 2) {
    autoScrolling ? stopAutoScroll() : startAutoScroll(speed);
}

// 按空格键切换
document.addEventListener('keydown', (e) => {
    if (e.code === 'Space' && e.target === document.body) {
        e.preventDefault();
        toggleAutoScroll();
    }
});""",
                tags = listOf(Strings.tagScroll, Strings.tagAuto)
            ),
            CodeSnippet(
                id = "scroll-back-to-top-btn",
                name = Strings.snippetBackToTopBtn,
                description = Strings.snippetBackToTopBtnDesc,
                code = """const backToTopBtn = document.createElement('div');
backToTopBtn.innerHTML = '⬆️';
backToTopBtn.style.cssText = `
    position: fixed; bottom: 80px; right: 20px; z-index: 99999;
    width: 50px; height: 50px; border-radius: 50%;
    background: rgba(0,0,0,0.7); color: white;
    display: none; align-items: center; justify-content: center;
    font-size: 24px; cursor: pointer;
    box-shadow: 0 2px 10px rgba(0,0,0,0.3);
    transition: opacity 0.3s, transform 0.3s;
`;
backToTopBtn.onclick = () => window.scrollTo({ top: 0, behavior: 'smooth' });
backToTopBtn.onmouseenter = () => backToTopBtn.style.transform = 'scale(1.1)';
backToTopBtn.onmouseleave = () => backToTopBtn.style.transform = 'scale(1)';
document.body.appendChild(backToTopBtn);

window.addEventListener('scroll', () => {
    backToTopBtn.style.display = window.scrollY > 300 ? 'flex' : 'none';
});""",
                tags = listOf(Strings.tagButton, Strings.tagBackToTop)
            ),
            CodeSnippet(
                id = "scroll-infinite",
                name = Strings.snippetInfiniteScroll,
                description = Strings.snippetInfiniteScrollDesc,
                code = """function setupInfiniteScroll(loadMore, threshold = 200) {
    let loading = false;
    
    window.addEventListener('scroll', async () => {
        if (loading) return;
        
        const scrollBottom = document.documentElement.scrollHeight - window.scrollY - window.innerHeight;
        
        if (scrollBottom < threshold) {
            loading = true;
            await loadMore();
            loading = false;
        }
    });
}

setupInfiniteScroll(async () => {
    console.log('加载更多内容...');
    // 在这里加载更多内容
});""",
                tags = listOf(Strings.tagScroll, Strings.tagLoading)
            ),
            CodeSnippet(
                id = "scroll-reveal",
                name = Strings.snippetScrollReveal,
                description = Strings.snippetScrollRevealDesc,
                code = """function setupScrollReveal(selector, animationClass = 'fade-in') {
    const style = document.createElement('style');
    style.textContent = `
        .scroll-hidden { opacity: 0; transform: translateY(20px); transition: all 0.6s; }
        .fade-in { opacity: 1 !important; transform: translateY(0) !important; }
    `;
    document.head.appendChild(style);
    
    const elements = document.querySelectorAll(selector);
    elements.forEach(el => el.classList.add('scroll-hidden'));
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add(animationClass);
            }
        });
    }, { threshold: 0.1 });
    
    elements.forEach(el => observer.observe(el));
}
setupScrollReveal('.card');""",
                tags = listOf(Strings.tagAnimation, Strings.tagScroll)
            ),
            CodeSnippet(
                id = "scroll-spy",
                name = Strings.snippetScrollSpy,
                description = Strings.snippetScrollSpyDesc,
                code = """function setupScrollSpy(navSelector, sectionSelector) {
    const navItems = document.querySelectorAll(navSelector);
    const sections = document.querySelectorAll(sectionSelector);
    
    window.addEventListener('scroll', () => {
        let current = '';
        sections.forEach(section => {
            const sectionTop = section.offsetTop - 100;
            if (window.scrollY >= sectionTop) {
                current = section.getAttribute('id');
            }
        });
        
        navItems.forEach(item => {
            item.classList.remove('active');
            if (item.getAttribute('href') === '#' + current) {
                item.classList.add('active');
            }
        });
    });
}
setupScrollSpy('nav a', 'section[id]');""",
                tags = listOf(Strings.tagNavigation, Strings.tagListen)
            )
        )
    )
    
    // ==================== 表单操作 ====================
    private fun formOperations() = CodeSnippetCategory(
        id = "form",
        name = Strings.snippetForm,
        icon = "📝",
        description = Strings.snippetFormDesc,
        snippets = listOf(
            CodeSnippet(
                id = "form-auto-fill",
                name = Strings.snippetAutoFill,
                description = Strings.snippetAutoFillDesc,
                code = """function autoFillForm(data) {
    Object.entries(data).forEach(([name, value]) => {
        const input = document.querySelector(`[name="${"$"}{name}"], #${"$"}{name}`);
        if (input) {
            input.value = value;
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
        }
    });
}
autoFillForm({
    username: '用户名',
    email: 'email@example.com',
    phone: '13800138000'
});""",
                tags = listOf(Strings.tagForm, Strings.tagFill)
            ),
            CodeSnippet(
                id = "form-get-values",
                name = Strings.snippetGetFormData,
                description = Strings.snippetGetFormDataDesc,
                code = """function getFormData(formSelector) {
    const form = document.querySelector(formSelector);
    if (!form) return null;
    
    const formData = new FormData(form);
    const data = {};
    formData.forEach((value, key) => {
        data[key] = value;
    });
    return data;
}
const data = getFormData('form');
console.log(data);""",
                tags = listOf(Strings.tagForm, Strings.tagGet)
            ),
            CodeSnippet(
                id = "form-validate",
                name = Strings.snippetFormValidate,
                description = Strings.snippetFormValidateDesc,
                code = """function validateForm(rules) {
    const errors = [];
    Object.entries(rules).forEach(([selector, rule]) => {
        const input = document.querySelector(selector);
        if (!input) return;
        
        const value = input.value.trim();
        if (rule.required && !value) {
            errors.push({ field: selector, message: rule.message || '此字段必填' });
        }
        if (rule.pattern && !rule.pattern.test(value)) {
            errors.push({ field: selector, message: rule.message || '格式不正确' });
        }
        if (rule.minLength && value.length < rule.minLength) {
            errors.push({ field: selector, message: '最少' + rule.minLength + '个字符' });
        }
    });
    return errors;
}
const errors = validateForm({
    '#email': { required: true, pattern: /^[^@]+@[^@]+$/, message: '请输入有效邮箱' },
    '#password': { required: true, minLength: 6, message: '密码至少6位' }
});""",
                tags = listOf(Strings.tagForm, Strings.tagValidate)
            ),
            CodeSnippet(
                id = "form-submit-intercept",
                name = Strings.snippetFormIntercept,
                description = Strings.snippetFormInterceptDesc,
                code = """document.querySelectorAll('form').forEach(form => {
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        
        const formData = new FormData(form);
        const data = Object.fromEntries(formData);
        
        console.log('表单数据:', data);
        
        // 可以在这里进行自定义处理
        // 然后决定是否继续提交
        // form.submit();
    });
});""",
                tags = listOf(Strings.tagForm, Strings.tagIntercept)
            ),
            CodeSnippet(
                id = "form-clear",
                name = Strings.snippetFormClear,
                description = Strings.snippetFormClearDesc,
                code = """function clearForm(formSelector) {
    const form = document.querySelector(formSelector);
    if (form) {
        form.reset();
        // 触发 change 事件
        form.querySelectorAll('input, select, textarea').forEach(el => {
            el.dispatchEvent(new Event('change', { bubbles: true }));
        });
    }
}
clearForm('#myForm');""",
                tags = listOf(Strings.tagForm, Strings.tagClear)
            ),
            CodeSnippet(
                id = "form-password-toggle",
                name = Strings.snippetPasswordToggle,
                description = Strings.snippetPasswordToggleDesc,
                code = """function addPasswordToggle(inputSelector) {
    const input = document.querySelector(inputSelector);
    if (!input) return;
    
    const toggle = document.createElement('button');
    toggle.type = 'button';
    toggle.innerHTML = '👁️';
    toggle.style.cssText = `
        position: absolute; right: 10px; top: 50%; transform: translateY(-50%);
        background: none; border: none; cursor: pointer; font-size: 16px;
    `;
    
    input.parentElement.style.position = 'relative';
    input.parentElement.appendChild(toggle);
    
    toggle.onclick = () => {
        input.type = input.type === 'password' ? 'text' : 'password';
        toggle.innerHTML = input.type === 'password' ? '👁️' : '🙈';
    };
}
addPasswordToggle('#password');""",
                tags = listOf(Strings.tagPassword, Strings.tagToggle)
            )
        )
    )

    
    // ==================== 媒体操作 ====================
    private fun mediaOperations() = CodeSnippetCategory(
        id = "media",
        name = Strings.snippetMedia,
        icon = "🎬",
        description = Strings.snippetMediaDesc,
        snippets = listOf(
            CodeSnippet(
                id = "media-video-speed",
                name = Strings.snippetVideoSpeed,
                description = Strings.snippetVideoSpeedDesc,
                code = """function setVideoSpeed(speed) {
    document.querySelectorAll('video').forEach(video => {
        video.playbackRate = speed;
    });
}
setVideoSpeed(2); // 2倍速

// 添加快捷键控制
document.addEventListener('keydown', (e) => {
    const video = document.querySelector('video');
    if (!video) return;
    if (e.key === '+' || e.key === '=') {
        video.playbackRate = Math.min(4, video.playbackRate + 0.25);
    }
    if (e.key === '-') {
        video.playbackRate = Math.max(0.25, video.playbackRate - 0.25);
    }
});""",
                tags = listOf(Strings.tagVideo, Strings.tagSpeed)
            ),
            CodeSnippet(
                id = "media-video-pip",
                name = Strings.snippetPiP,
                description = Strings.snippetPiPDesc,
                code = """async function enablePiP() {
    const video = document.querySelector('video');
    if (video && document.pictureInPictureEnabled) {
        try {
            if (document.pictureInPictureElement) {
                await document.exitPictureInPicture();
            } else {
                await video.requestPictureInPicture();
            }
        } catch (error) {
            console.error('画中画失败:', error);
        }
    }
}
enablePiP();""",
                tags = listOf(Strings.tagVideo, Strings.tagPiP)
            ),
            CodeSnippet(
                id = "media-video-screenshot",
                name = Strings.snippetVideoScreenshot,
                description = Strings.snippetVideoScreenshotDesc,
                code = """function captureVideoFrame(videoSelector) {
    const video = document.querySelector(videoSelector || 'video');
    if (!video) return null;
    
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    
    // Download截图
    const link = document.createElement('a');
    link.download = 'screenshot_' + Date.now() + '.png';
    link.href = canvas.toDataURL('image/png');
    link.click();
    
    return canvas.toDataURL('image/png');
}
captureVideoFrame();""",
                tags = listOf(Strings.tagVideo, Strings.tagScreenshot)
            ),
            CodeSnippet(
                id = "media-image-zoom",
                name = Strings.snippetImageZoom,
                description = Strings.snippetImageZoomDesc,
                code = """document.addEventListener('click', (e) => {
    if (e.target.tagName === 'IMG') {
        const overlay = document.createElement('div');
        overlay.style.cssText = `
            position: fixed; top: 0; left: 0; right: 0; bottom: 0;
            background: rgba(0,0,0,0.9); z-index: 999999;
            display: flex; align-items: center; justify-content: center;
            cursor: zoom-out;
        `;
        
        const img = document.createElement('img');
        img.src = e.target.src;
        img.style.cssText = 'max-width: 95%; max-height: 95%; object-fit: contain;';
        
        overlay.appendChild(img);
        overlay.onclick = () => overlay.remove();
        document.body.appendChild(overlay);
    }
});""",
                tags = listOf(Strings.tagImage, Strings.tagZoom)
            ),
            CodeSnippet(
                id = "media-download-images",
                name = Strings.snippetDownloadImages,
                description = Strings.snippetDownloadImagesDesc,
                code = """function downloadAllImages(minSize = 100) {
    const images = Array.from(document.querySelectorAll('img'))
        .filter(img => img.naturalWidth >= minSize && img.naturalHeight >= minSize);
    
    images.forEach((img, index) => {
        setTimeout(() => {
            const link = document.createElement('a');
            link.href = img.src;
            link.download = 'image_' + (index + 1) + '.jpg';
            link.click();
        }, index * 500); // 间隔500ms避免浏览器阻止
    });
    
    console.log('开始下载 ' + images.length + ' 张图片');
}
downloadAllImages();""",
                tags = listOf(Strings.tagImage, Strings.tagDownload)
            ),
            CodeSnippet(
                id = "media-audio-control",
                name = Strings.snippetAudioControl,
                description = Strings.snippetAudioControlDesc,
                code = """// 静音所有音视频
function muteAll() {
    document.querySelectorAll('video, audio').forEach(media => {
        media.muted = true;
    });
}

// Pause所有音视频
function pauseAll() {
    document.querySelectorAll('video, audio').forEach(media => {
        media.pause();
    });
}

// Set音量 (0-1)
function setVolume(volume) {
    document.querySelectorAll('video, audio').forEach(media => {
        media.volume = Math.max(0, Math.min(1, volume));
    });
}

muteAll(); // 静音所有""",
                tags = listOf(Strings.tagAudio, Strings.tagControl)
            ),
            CodeSnippet(
                id = "media-lazy-load",
                name = Strings.snippetLazyLoad,
                description = Strings.snippetLazyLoadDesc,
                code = """function setupLazyLoad() {
    const images = document.querySelectorAll('img[data-src]');
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                img.src = img.dataset.src;
                img.removeAttribute('data-src');
                observer.unobserve(img);
            }
        });
    }, { rootMargin: '100px' });
    
    images.forEach(img => observer.observe(img));
}
setupLazyLoad();""",
                tags = listOf(Strings.tagImage, Strings.tagLazyLoad)
            ),
            CodeSnippet(
                id = "media-fullscreen",
                name = Strings.snippetFullscreen,
                description = Strings.snippetFullscreenDesc,
                code = """function toggleFullscreen(element = document.documentElement) {
    if (!document.fullscreenElement) {
        element.requestFullscreen?.() ||
        element.webkitRequestFullscreen?.() ||
        element.mozRequestFullScreen?.();
    } else {
        document.exitFullscreen?.() ||
        document.webkitExitFullscreen?.() ||
        document.mozCancelFullScreen?.();
    }
}

// Video全屏
function videoFullscreen() {
    const video = document.querySelector('video');
    if (video) toggleFullscreen(video);
}""",
                tags = listOf(Strings.tagFullscreen, Strings.tagVideo)
            )
        )
    )
    
    // ==================== 页面增强 ====================
    private fun pageEnhance() = CodeSnippetCategory(
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

    
    // ==================== 内容过滤 ====================
    private fun contentFilter() = CodeSnippetCategory(
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
    
    // ==================== 广告拦截 ====================
    private fun adBlocker() = CodeSnippetCategory(
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
    
    // ==================== 工具函数 ====================
    private fun utilityFunctions() = CodeSnippetCategory(
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
// 使用示例
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
// 使用示例
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
        // 降级方案
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

// 使用示例
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

// 使用示例
retry(() => fetch('/api/data').then(r => r.json()), 3, 1000);""",
                tags = listOf(Strings.tagRetry, Strings.tagErrorHandle)
            )
        )
    )

    
    // ==================== 文本处理 ====================
    private fun textProcessing() = CodeSnippetCategory(
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
    // 尝试常见的文章容器
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
    
    // 移除旧的翻译按钮
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
    
    // 标题
    md = md.replace(/<h1[^>]*>(.*?)<\/h1>/gi, '# $1\\n');
    md = md.replace(/<h2[^>]*>(.*?)<\/h2>/gi, '## $1\\n');
    md = md.replace(/<h3[^>]*>(.*?)<\/h3>/gi, '### $1\\n');
    
    // 格式
    md = md.replace(/<strong[^>]*>(.*?)<\/strong>/gi, '**$1**');
    md = md.replace(/<b[^>]*>(.*?)<\/b>/gi, '**$1**');
    md = md.replace(/<em[^>]*>(.*?)<\/em>/gi, '*$1*');
    md = md.replace(/<i[^>]*>(.*?)<\/i>/gi, '*$1*');
    
    // 链接和图片
    md = md.replace(/<a[^>]*href="([^"]*)"[^>]*>(.*?)<\/a>/gi, '[$2]($1)');
    md = md.replace(/<img[^>]*src="([^"]*)"[^>]*alt="([^"]*)"[^>]*>/gi, '![$2]($1)');
    
    // List
    md = md.replace(/<li[^>]*>(.*?)<\/li>/gi, '- $1\\n');
    
    // 段落和换行
    md = md.replace(/<p[^>]*>(.*?)<\/p>/gi, '$1\\n\\n');
    md = md.replace(/<br[^>]*>/gi, '\\n');
    
    // 移除其他标签
    md = md.replace(/<[^>]+>/g, '');
    
    return md.trim();
}
const md = htmlToMarkdown(document.body.innerHTML);
console.log(md);""",
                tags = listOf(Strings.tagMarkdown, Strings.tagConvert)
            )
        )
    )
    
    // ==================== 请求拦截 ====================
    private fun interceptors() = CodeSnippetCategory(
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
    
    // ==================== 自动化 ====================
    private fun automation() = CodeSnippetCategory(
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
    
    click(); // 立即执行一次
    return setInterval(click, interval);
}

// 每5秒点击一次
const timer = autoClickInterval('.refresh-btn', 5000);
// Stop: clearInterval(timer);""",
                tags = listOf(Strings.tagTimer, Strings.tagClick)
            ),
            CodeSnippet(
                id = "auto-fill-form",
                name = Strings.snippetAutoFillSubmit,
                description = Strings.snippetAutoFillSubmitDesc,
                code = """async function autoFillAndSubmit(formData, submitSelector) {
    // 填写表单
    for (const [name, value] of Object.entries(formData)) {
        const input = document.querySelector(`[name="${"$"}{name}"], #${"$"}{name}`);
        if (input) {
            input.value = value;
            input.dispatchEvent(new Event('input', { bubbles: true }));
            input.dispatchEvent(new Event('change', { bubbles: true }));
            await new Promise(r => setTimeout(r, 100));
        }
    }
    
    // 点击提交
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
    
    // ==================== 调试工具 ====================
    private fun debugging() = CodeSnippetCategory(
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

// 按 Ctrl+Shift+I 切换
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

// 拦截 Fetch
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

// 拦截 XHR
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

// 查看日志
window.showNetworkLog = () => console.table(networkLog);""",
                tags = listOf(Strings.tagNetwork, Strings.tagLog)
            )
        )
    )
}

/**
 * 代码块分类
 */
data class CodeSnippetCategory(
    val id: String,
    val name: String,
    val icon: String,
    val description: String,
    val snippets: List<CodeSnippet>
)

/**
 * 代码块
 */
data class CodeSnippet(
    val id: String,
    val name: String,
    val description: String,
    val code: String,
    val tags: List<String> = emptyList()
)
