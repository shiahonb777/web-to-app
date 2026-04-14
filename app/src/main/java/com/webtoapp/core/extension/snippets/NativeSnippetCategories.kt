package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.AppStringsProvider

internal fun nativeBridgeOperations() = CodeSnippetCategory(
        id = "native",
        name = AppStringsProvider.current().snippetNative,
        icon = "📱",
        description = AppStringsProvider.current().snippetNativeDesc,
        snippets = listOf(
            CodeSnippet(
                id = "native-toast",
                name = AppStringsProvider.current().snippetShowToast,
                description = AppStringsProvider.current().snippetShowToastDesc,
                code = """// 短提示
NativeBridge.showToast('操作成功');

NativeBridge.showToast('请稍候，正在处理...', 'long');""",
                tags = listOf(AppStringsProvider.current().tagToast, AppStringsProvider.current().tagToast, AppStringsProvider.current().tagMessage)
            ),
            CodeSnippet(
                id = "native-vibrate",
                name = AppStringsProvider.current().snippetVibrate,
                description = AppStringsProvider.current().snippetVibrateDesc,
                code = """// 短震动（100ms）
NativeBridge.vibrate();

// Custom when.
NativeBridge.vibrate(500);

NativeBridge.vibratePattern('100,200,100,200');""",
                tags = listOf(AppStringsProvider.current().tagVibrate, AppStringsProvider.current().tagFeedback, AppStringsProvider.current().tagHaptic)
            ),
            CodeSnippet(
                id = "native-copy",
                name = AppStringsProvider.current().snippetCopyToClipboard,
                description = AppStringsProvider.current().snippetCopyToClipboardDesc,
                code = """function copyText(text) {
    const success = NativeBridge.copyToClipboard(text);
    if (success) {
        NativeBridge.showToast('已复制到剪贴板');
        NativeBridge.vibrate(50);
    } else {
        NativeBridge.showToast('复制失败');
    }
}

// use in.
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('copy-btn')) {
        const text = e.target.dataset.text;
        copyText(text);
    }
});""",
                tags = listOf(AppStringsProvider.current().tagCopy, AppStringsProvider.current().tagClipboard, AppStringsProvider.current().tagClipboard)
            ),
            CodeSnippet(
                id = "native-share",
                name = AppStringsProvider.current().snippetShareContent,
                description = AppStringsProvider.current().snippetShareContentDesc,
                code = """// 分享文本和链接
function shareContent(title, text, url) {
    NativeBridge.share(title, text, url);
}

// before.
function shareCurrentPage() {
    NativeBridge.share(
        document.title,
        '我发现了一个有趣的页面',
        location.href
    );
}

// by.
const shareBtn = document.createElement('button');
shareBtn.textContent = '分享';
shareBtn.onclick = shareCurrentPage;""",
                tags = listOf(AppStringsProvider.current().tagShare, AppStringsProvider.current().tagShare, AppStringsProvider.current().tagSocial)
            ),
            CodeSnippet(
                id = "native-save-image",
                name = AppStringsProvider.current().snippetSaveImageToGallery,
                description = AppStringsProvider.current().snippetSaveImageToGalleryDesc,
                code = """// Save图片到相册
function saveImage(imageUrl, filename) {
    NativeBridge.saveImageToGallery(imageUrl, filename || '');
}

// as by Save.
document.querySelectorAll('img').forEach(img => {
    img.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        if (confirm('保存图片到相册？')) {
            saveImage(img.src);
        }
    });
});""",
                tags = listOf(AppStringsProvider.current().tagSave, AppStringsProvider.current().tagImage, AppStringsProvider.current().tagGallery, AppStringsProvider.current().tagDownload)
            ),
            CodeSnippet(
                id = "native-save-video",
                name = AppStringsProvider.current().snippetSaveVideoToGallery,
                description = AppStringsProvider.current().snippetSaveVideoToGalleryDesc,
                code = """// Save视频到相册
function saveVideo(videoUrl, filename) {
    NativeBridge.saveVideoToGallery(videoUrl, filename || '');
}

// as by.
document.querySelectorAll('video').forEach(video => {
    const btn = document.createElement('button');
    btn.textContent = '保存视频';
    btn.style.cssText = 'position:absolute;top:10px;right:10px;z-index:999;';
    btn.onclick = () => saveVideo(video.src);
    video.parentElement.style.position = 'relative';
    video.parentElement.appendChild(btn);
});""",
                tags = listOf(AppStringsProvider.current().tagSave, AppStringsProvider.current().tagVideo, AppStringsProvider.current().tagGallery, AppStringsProvider.current().tagDownload)
            ),
            CodeSnippet(
                id = "native-open-url",
                name = AppStringsProvider.current().snippetOpenInBrowser,
                description = AppStringsProvider.current().snippetOpenInBrowserDesc,
                code = """// 用系统浏览器打开链接
function openInBrowser(url) {
    NativeBridge.openUrl(url);
}

// intercept use.
document.addEventListener('click', (e) => {
    const link = e.target.closest('a');
    if (link && link.href && !link.href.startsWith(location.origin)) {
        e.preventDefault();
        openInBrowser(link.href);
    }
});""",
                tags = listOf(AppStringsProvider.current().tagBrowser, AppStringsProvider.current().tagLink, AppStringsProvider.current().tagExternal)
            ),
            CodeSnippet(
                id = "native-device-info",
                name = AppStringsProvider.current().snippetDeviceInfo,
                description = AppStringsProvider.current().snippetDeviceInfoDesc,
                code = """// Get设备信息
const deviceInfo = JSON.parse(NativeBridge.getDeviceInfo());
console.log('设备型号:', deviceInfo.model);
console.log('Android 版本:', deviceInfo.androidVersion);
console.log('屏幕尺寸:', deviceInfo.screenWidth, 'x', deviceInfo.screenHeight);

// Get use.
const appInfo = JSON.parse(NativeBridge.getAppInfo());
console.log('应用版本:', appInfo.versionName);

if (deviceInfo.screenWidth < 400) {
    document.body.classList.add('small-screen');
}""",
                tags = listOf(AppStringsProvider.current().tagDevice, AppStringsProvider.current().tagInfo, AppStringsProvider.current().tagScreen)
            ),
            CodeSnippet(
                id = "native-network",
                name = AppStringsProvider.current().snippetNetworkStatus,
                description = AppStringsProvider.current().snippetNetworkStatusDesc,
                code = """// Check网络是否可用
if (NativeBridge.isNetworkAvailable()) {
    console.log('网络可用');
} else {
    NativeBridge.showToast('当前无网络连接');
}

// Get.
const networkType = NativeBridge.getNetworkType();
console.log('网络类型:', networkType); // wifi, mobile, none

// as.
if (networkType === 'mobile') {
    // use.
    document.querySelectorAll('video').forEach(v => v.preload = 'none');
}""",
                tags = listOf(AppStringsProvider.current().tagNetwork, AppStringsProvider.current().tagWiFi, AppStringsProvider.current().tagData)
            ),
            CodeSnippet(
                id = "native-save-file",
                name = AppStringsProvider.current().snippetSaveFile,
                description = AppStringsProvider.current().snippetSaveFileDesc,
                code = """// Save文本文件
function saveTextFile(content, filename) {
    NativeBridge.saveToFile(content, filename, 'text/plain');
}

// Save JSON.
function saveJsonFile(data, filename) {
    const json = JSON.stringify(data, null, 2);
    NativeBridge.saveToFile(json, filename, 'application/json');
}

// Export.
const pageData = {
    title: document.title,
    url: location.href,
    content: document.body.innerText.substring(0, 1000)
};
saveJsonFile(pageData, 'page_data.json');""",
                tags = listOf(AppStringsProvider.current().tagSave, AppStringsProvider.current().tagFile, AppStringsProvider.current().tagExport)
            ),
            CodeSnippet(
                id = "native-image-download-btn",
                name = AppStringsProvider.current().snippetImageDownloadBtn,
                description = AppStringsProvider.current().snippetImageDownloadBtnDesc,
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
                tags = listOf(AppStringsProvider.current().tagImage, AppStringsProvider.current().tagDownload, AppStringsProvider.current().tagButton, AppStringsProvider.current().tagFloating)
            )
        )
)
internal fun domOperations() = CodeSnippetCategory(
        id = "dom",
        name = AppStringsProvider.current().snippetDom,
        icon = "🔧",
        description = AppStringsProvider.current().snippetDomDesc,
        snippets = listOf(
            CodeSnippet(
                id = "dom-query-single",
                name = AppStringsProvider.current().snippetQuerySingle,
                description = AppStringsProvider.current().snippetQuerySingleDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
}""",
                tags = listOf(AppStringsProvider.current().tagQuery, AppStringsProvider.current().tagSelector)
            ),
            CodeSnippet(
                id = "dom-query-all",
                name = AppStringsProvider.current().snippetQueryAll,
                description = AppStringsProvider.current().snippetQueryAllDesc,
                code = """document.querySelectorAll('选择器').forEach(el => {
});""",
                tags = listOf(AppStringsProvider.current().tagQuery, AppStringsProvider.current().tagIterate)
            ),
            CodeSnippet(
                id = "dom-hide-element",
                name = AppStringsProvider.current().snippetHideElement,
                description = AppStringsProvider.current().snippetHideElementDesc,
                code = """function hideElement(selector) {
    document.querySelectorAll(selector).forEach(el => {
        el.style.setProperty('display', 'none', 'important');
    });
}
hideElement('选择器');""",
                tags = listOf(AppStringsProvider.current().tagHide, AppStringsProvider.current().tagStyle)
            ),
            CodeSnippet(
                id = "dom-remove-element",
                name = AppStringsProvider.current().snippetRemoveElement,
                description = AppStringsProvider.current().snippetRemoveElementDesc,
                code = """function removeElement(selector) {
    document.querySelectorAll(selector).forEach(el => el.remove());
}
removeElement('选择器');""",
                tags = listOf(AppStringsProvider.current().tagDelete, AppStringsProvider.current().tagRemove)
            ),
            CodeSnippet(
                id = "dom-create-element",
                name = AppStringsProvider.current().snippetCreateElement,
                description = AppStringsProvider.current().snippetCreateElementDesc,
                code = """const newElement = document.createElement('div');
newElement.id = 'my-element';
newElement.className = 'my-class';
newElement.textContent = '内容';
newElement.style.cssText = 'color: red; font-size: 14px;';
document.body.appendChild(newElement);""",
                tags = listOf(AppStringsProvider.current().tagCreate, AppStringsProvider.current().tagAdd)
            ),
            CodeSnippet(
                id = "dom-modify-text",
                name = AppStringsProvider.current().snippetModifyText,
                description = AppStringsProvider.current().snippetModifyTextDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    element.textContent = '新的文本内容';
    // or use innerHTML Supports HTML.
    // element.innerHTML = '<strong> </strong>';.
}""",
                tags = listOf(AppStringsProvider.current().tagText, AppStringsProvider.current().tagModify)
            ),
            CodeSnippet(
                id = "dom-modify-attribute",
                name = AppStringsProvider.current().snippetModifyAttr,
                description = AppStringsProvider.current().snippetModifyAttrDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    element.setAttribute('属性名', '属性值');
    const value = element.getAttribute('属性名');
    element.removeAttribute('属性名');
}""",
                tags = listOf(AppStringsProvider.current().tagAttribute, AppStringsProvider.current().tagModify)
            ),
            CodeSnippet(
                id = "dom-insert-before",
                name = AppStringsProvider.current().snippetInsertBefore,
                description = AppStringsProvider.current().snippetInsertBeforeDesc,
                code = """const target = document.querySelector('目标选择器');
const newEl = document.createElement('div');
newEl.textContent = '新内容';
target.parentNode.insertBefore(newEl, target);""",
                tags = listOf(AppStringsProvider.current().tagInsert, AppStringsProvider.current().tagPosition)
            ),
            CodeSnippet(
                id = "dom-insert-after",
                name = AppStringsProvider.current().snippetInsertAfter,
                description = AppStringsProvider.current().snippetInsertAfterDesc,
                code = """const target = document.querySelector('目标选择器');
const newEl = document.createElement('div');
newEl.textContent = '新内容';
target.parentNode.insertBefore(newEl, target.nextSibling);""",
                tags = listOf(AppStringsProvider.current().tagInsert, AppStringsProvider.current().tagPosition)
            ),
            CodeSnippet(
                id = "dom-clone-element",
                name = AppStringsProvider.current().snippetCloneElement,
                description = AppStringsProvider.current().snippetCloneElementDesc,
                code = """const original = document.querySelector('选择器');
const clone = original.cloneNode(true);
clone.id = 'cloned-element';
document.body.appendChild(clone);""",
                tags = listOf(AppStringsProvider.current().tagClone, AppStringsProvider.current().tagCopy)
            ),
            CodeSnippet(
                id = "dom-wrap-element",
                name = AppStringsProvider.current().snippetWrapElement,
                description = AppStringsProvider.current().snippetWrapElementDesc,
                code = """function wrapElement(selector, wrapperTag = 'div') {
    document.querySelectorAll(selector).forEach(el => {
        const wrapper = document.createElement(wrapperTag);
        el.parentNode.insertBefore(wrapper, el);
        wrapper.appendChild(el);
    });
}
wrapElement('img', 'figure');""",
                tags = listOf(AppStringsProvider.current().tagWrap, AppStringsProvider.current().tagStructure)
            ),
            CodeSnippet(
                id = "dom-replace-element",
                name = AppStringsProvider.current().snippetReplaceElement,
                description = AppStringsProvider.current().snippetReplaceElementDesc,
                code = """function replaceElement(selector, newHtml) {
    document.querySelectorAll(selector).forEach(el => {
        const temp = document.createElement('div');
        temp.innerHTML = newHtml;
        el.replaceWith(temp.firstChild);
    });
}
replaceElement('.old-class', '<div class="new-class">新内容</div>');""",
                tags = listOf(AppStringsProvider.current().tagReplace, AppStringsProvider.current().tagModify)
            )
        )
)
internal fun styleOperations() = CodeSnippetCategory(
        id = "style",
        name = AppStringsProvider.current().snippetStyle,
        icon = "🎨",
        description = AppStringsProvider.current().snippetStyleDesc,
        snippets = listOf(
            CodeSnippet(
                id = "style-inject-css",
                name = AppStringsProvider.current().snippetInjectCss,
                description = AppStringsProvider.current().snippetInjectCssDesc,
                code = """const style = document.createElement('style');
style.id = 'my-custom-style';
style.textContent = `
    .my-class {
        color: red !important;
        font-size: 16px !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagCSS, AppStringsProvider.current().tagInject)
            ),
            CodeSnippet(
                id = "style-modify-inline",
                name = AppStringsProvider.current().snippetModifyInline,
                description = AppStringsProvider.current().snippetModifyInlineDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    element.style.color = 'red';
    element.style.fontSize = '16px';
    element.style.setProperty('display', 'block', 'important');
}""",
                tags = listOf(AppStringsProvider.current().tagStyle, AppStringsProvider.current().tagInline)
            ),
            CodeSnippet(
                id = "style-add-class",
                name = AppStringsProvider.current().snippetAddClass,
                description = AppStringsProvider.current().snippetAddClassDesc,
                code = """const element = document.querySelector('选择器');
if (element) {
    element.classList.add('new-class');
    element.classList.remove('old-class');
    element.classList.toggle('toggle-class');
    const hasClass = element.classList.contains('some-class');
}""",
                tags = listOf(AppStringsProvider.current().tagClassName, AppStringsProvider.current().tagClassName)
            ),
            CodeSnippet(
                id = "style-dark-mode",
                name = AppStringsProvider.current().snippetDarkMode,
                description = AppStringsProvider.current().snippetDarkModeDesc,
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
                tags = listOf(AppStringsProvider.current().tagDark, AppStringsProvider.current().tagTheme)
            ),
            CodeSnippet(
                id = "style-sepia-mode",
                name = AppStringsProvider.current().snippetSepiaMode,
                description = AppStringsProvider.current().snippetSepiaModeDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    html {
        filter: sepia(30%) brightness(95%) !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagEyeCare, AppStringsProvider.current().tagWarm)
            ),
            CodeSnippet(
                id = "style-grayscale",
                name = AppStringsProvider.current().snippetGrayscale,
                description = AppStringsProvider.current().snippetGrayscaleDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    html {
        filter: grayscale(100%) !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagGrayscale, AppStringsProvider.current().tagFilter)
            ),
            CodeSnippet(
                id = "style-custom-font",
                name = AppStringsProvider.current().snippetCustomFont,
                description = AppStringsProvider.current().snippetCustomFontDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    * {
        font-family: "Microsoft YaHei", "PingFang SC", sans-serif !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagFont, AppStringsProvider.current().tagStyle)
            ),
            CodeSnippet(
                id = "style-font-size",
                name = AppStringsProvider.current().snippetFontSize,
                description = AppStringsProvider.current().snippetFontSizeDesc,
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
                tags = listOf(AppStringsProvider.current().tagFont, AppStringsProvider.current().tagSize)
            ),
            CodeSnippet(
                id = "style-hide-scrollbar",
                name = AppStringsProvider.current().snippetHideScrollbar,
                description = AppStringsProvider.current().snippetHideScrollbarDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    ::-webkit-scrollbar { display: none !important; }
    * { scrollbar-width: none !important; }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagScrollbar, AppStringsProvider.current().tagHide)
            ),
            CodeSnippet(
                id = "style-highlight-links",
                name = AppStringsProvider.current().snippetHighlightLinks,
                description = AppStringsProvider.current().snippetHighlightLinksDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    a {
        background: yellow !important;
        color: #000 !important;
        padding: 2px 4px !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagLink, AppStringsProvider.current().tagHighlight)
            ),
            CodeSnippet(
                id = "style-max-width",
                name = AppStringsProvider.current().snippetMaxWidth,
                description = AppStringsProvider.current().snippetMaxWidthDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    body > * {
        max-width: 800px !important;
        margin-left: auto !important;
        margin-right: auto !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagWidth, AppStringsProvider.current().tagReading)
            ),
            CodeSnippet(
                id = "style-line-height",
                name = AppStringsProvider.current().snippetLineHeight,
                description = AppStringsProvider.current().snippetLineHeightDesc,
                code = """const style = document.createElement('style');
style.textContent = `
    p, li, span, div {
        line-height: 1.8 !important;
    }
`;
document.head.appendChild(style);""",
                tags = listOf(AppStringsProvider.current().tagLineHeight, AppStringsProvider.current().tagReading)
            )
        )
)
