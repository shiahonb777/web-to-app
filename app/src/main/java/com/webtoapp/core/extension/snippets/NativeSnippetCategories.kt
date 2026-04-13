package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.Strings

internal fun nativeBridgeOperations() = CodeSnippetCategory(
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
internal fun domOperations() = CodeSnippetCategory(
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
internal fun styleOperations() = CodeSnippetCategory(
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
