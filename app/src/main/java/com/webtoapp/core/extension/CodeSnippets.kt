package com.webtoapp.core.extension

import com.webtoapp.core.i18n.Strings

/**
 * 代码块库
 *
 * 提供可复用的代码片段，用户可以在模块编辑器中选择插入
 * 包含 20+ 分类，200+ 代码片段
 */
object CodeSnippets {

    private fun tag(chinese: String, english: String, arabic: String = english): String {
        return Strings.localized(chinese, english, arabic)
    }

    private fun translateTag(tag: String): String = when (tag) {
        "\u63d0\u793a" -> tag("\u63d0\u793a", "Hint")
        "\u6d88\u606f" -> tag("\u6d88\u606f", "Message")
        "\u9707\u52a8" -> tag("\u9707\u52a8", "Vibration")
        "\u53cd\u9988" -> tag("\u53cd\u9988", "Feedback")
        "\u89e6\u611f" -> tag("\u89e6\u611f", "Haptic")
        "\u590d\u5236" -> tag("\u590d\u5236", "Copy")
        "\u526a\u8d34\u677f" -> tag("\u526a\u8d34\u677f", "Clipboard")
        "\u5206\u4eab" -> tag("\u5206\u4eab", "Share")
        "\u793e\u4ea4" -> tag("\u793e\u4ea4", "Social")
        "\u4fdd\u5b58" -> tag("\u4fdd\u5b58", "Save")
        "\u56fe\u7247" -> tag("\u56fe\u7247", "Image")
        "\u76f8\u518c" -> tag("\u76f8\u518c", "Album")
        "\u4e0b\u8f7d" -> tag("\u4e0b\u8f7d", "Download")
        "\u89c6\u9891" -> tag("\u89c6\u9891", "Video")
        "\u6d4f\u89c8\u5668" -> tag("\u6d4f\u89c8\u5668", "Browser")
        "\u94fe\u63a5" -> tag("\u94fe\u63a5", "Link")
        "\u5916\u90e8" -> tag("\u5916\u90e8", "External")
        "\u8bbe\u5907" -> tag("\u8bbe\u5907", "Device")
        "\u4fe1\u606f" -> tag("\u4fe1\u606f", "Info")
        "\u5c4f\u5e55" -> tag("\u5c4f\u5e55", "Screen")
        "\u7f51\u7edc" -> tag("\u7f51\u7edc", "Network")
        "\u6d41\u91cf" -> tag("\u6d41\u91cf", "Traffic")
        "\u6587\u4ef6" -> tag("\u6587\u4ef6", "File")
        "\u5bfc\u51fa" -> tag("\u5bfc\u51fa", "Export")
        "\u6309\u94ae" -> tag("\u6309\u94ae", "Button")
        "\u60ac\u6d6e" -> tag("\u60ac\u6d6e", "Floating")
        "\u67e5\u8be2" -> tag("\u67e5\u8be2", "Query")
        "\u9009\u62e9\u5668" -> tag("\u9009\u62e9\u5668", "Selector")
        "\u904d\u5386" -> tag("\u904d\u5386", "Traverse")
        "\u9690\u85cf" -> tag("\u9690\u85cf", "Hide")
        "\u6837\u5f0f" -> tag("\u6837\u5f0f", "Style")
        "\u5220\u9664" -> tag("\u5220\u9664", "Delete")
        "\u79fb\u9664" -> tag("\u79fb\u9664", "Remove")
        "\u521b\u5efa" -> tag("\u521b\u5efa", "Create")
        "\u6dfb\u52a0" -> tag("\u6dfb\u52a0", "Add")
        "\u6587\u672c" -> tag("\u6587\u672c", "Text")
        "\u4fee\u6539" -> tag("\u4fee\u6539", "Edit")
        "\u5c5e\u6027" -> tag("\u5c5e\u6027", "Attribute")
        "\u63d2\u5165" -> tag("\u63d2\u5165", "Insert")
        "\u4f4d\u7f6e" -> tag("\u4f4d\u7f6e", "Position")
        "\u514b\u9686" -> tag("\u514b\u9686", "Clone")
        "\u5305\u88f9" -> tag("\u5305\u88f9", "Wrap")
        "\u7ed3\u6784" -> tag("\u7ed3\u6784", "Structure")
        "\u66ff\u6362" -> tag("\u66ff\u6362", "Replace")
        "\u6ce8\u5165" -> tag("\u6ce8\u5165", "Inject")
        "\u5185\u8054" -> tag("\u5185\u8054", "Inline")
        "\u7c7b\u540d" -> tag("\u7c7b\u540d", "Class name")
        "\u6df1\u8272" -> tag("\u6df1\u8272", "Dark")
        "\u4e3b\u9898" -> tag("\u4e3b\u9898", "Theme")
        "\u62a4\u773c" -> tag("\u62a4\u773c", "Eye comfort")
        "\u6696\u8272" -> tag("\u6696\u8272", "Warm")
        "\u7070\u5ea6" -> tag("\u7070\u5ea6", "Grayscale")
        "\u6ee4\u955c" -> tag("\u6ee4\u955c", "Filter")
        "\u5b57\u4f53" -> tag("\u5b57\u4f53", "Font")
        "\u5927\u5c0f" -> tag("\u5927\u5c0f", "Size")
        "\u6eda\u52a8\u6761" -> tag("\u6eda\u52a8\u6761", "Scrollbar")
        "\u9ad8\u4eae" -> tag("\u9ad8\u4eae", "Highlight")
        "\u5bbd\u5ea6" -> tag("\u5bbd\u5ea6", "Width")
        "\u9605\u8bfb" -> tag("\u9605\u8bfb", "Reading")
        "\u884c\u9ad8" -> tag("\u884c\u9ad8", "Line height")
        "\u70b9\u51fb" -> tag("\u70b9\u51fb", "Click")
        "\u4e8b\u4ef6" -> tag("\u4e8b\u4ef6", "Event")
        "\u952e\u76d8" -> tag("\u952e\u76d8", "Keyboard")
        "\u5feb\u6377\u952e" -> tag("\u5feb\u6377\u952e", "Shortcut")
        "\u6eda\u52a8" -> tag("\u6eda\u52a8", "Scroll")
        "\u76d1\u542c" -> tag("\u76d1\u542c", "Listener")
        "DOM\u53d8\u5316" -> tag("DOM\u53d8\u5316", "DOM change")
        "\u52a8\u6001" -> tag("\u52a8\u6001", "Dynamic")
        "\u7a97\u53e3" -> tag("\u7a97\u53e3", "Window")
        "\u53f3\u952e" -> tag("\u53f3\u952e", "Right click")
        "\u83dc\u5355" -> tag("\u83dc\u5355", "Menu")
        "\u53ef\u89c1\u6027" -> tag("\u53ef\u89c1\u6027", "Visibility")
        "\u540e\u53f0" -> tag("\u540e\u53f0", "Background")
        "\u5173\u95ed" -> tag("\u5173\u95ed", "Close")
        "\u89e6\u6478" -> tag("\u89e6\u6478", "Touch")
        "\u624b\u52bf" -> tag("\u624b\u52bf", "Gesture")
        "\u957f\u6309" -> tag("\u957f\u6309", "Long press")
        "\u5b58\u50a8" -> tag("\u5b58\u50a8", "Storage")
        "\u8bfb\u53d6" -> tag("\u8bfb\u53d6", "Read")
        "\u4f1a\u8bdd" -> tag("\u4f1a\u8bdd", "Session")
        "\u4e34\u65f6" -> tag("\u4e34\u65f6", "Temporary")
        "\u8bbe\u7f6e" -> tag("\u8bbe\u7f6e", "Settings")
        "\u5927\u6570\u636e" -> tag("\u5927\u6570\u636e", "Big data")
        "\u8bf7\u6c42" -> tag("\u8bf7\u6c42", "Request")
        "\u63d0\u4ea4" -> tag("\u63d0\u4ea4", "Submit")
        "\u8d85\u65f6" -> tag("\u8d85\u65f6", "Timeout")
        "\u91cd\u8bd5" -> tag("\u91cd\u8bd5", "Retry")
        "\u8de8\u57df" -> tag("\u8de8\u57df", "Cross-origin")
        "\u8868\u683c" -> tag("\u8868\u683c", "Table")
        "\u63d0\u53d6" -> tag("\u63d0\u53d6", "Extract")
        "\u89e3\u6790" -> tag("\u89e3\u6790", "Parse")
        "\u6784\u5efa" -> tag("\u6784\u5efa", "Build")
        "\u5f39\u7a97" -> tag("\u5f39\u7a97", "Popup")
        "\u5bf9\u8bdd\u6846" -> tag("\u5bf9\u8bdd\u6846", "Dialog")
        "\u8fdb\u5ea6" -> tag("\u8fdb\u5ea6", "Progress")
        "\u52a0\u8f7d" -> tag("\u52a0\u8f7d", "Load")
        "\u52a8\u753b" -> tag("\u52a8\u753b", "Animation")
        "\u901a\u77e5" -> tag("\u901a\u77e5", "Notification")
        "\u5de5\u5177\u680f" -> tag("\u5de5\u5177\u680f", "Toolbar")
        "\u4fa7\u8fb9\u680f" -> tag("\u4fa7\u8fb9\u680f", "Sidebar")
        "\u9762\u677f" -> tag("\u9762\u677f", "Panel")
        "\u62d6\u52a8" -> tag("\u62d6\u52a8", "Drag")
        "\u4ea4\u4e92" -> tag("\u4ea4\u4e92", "Interaction")
        "\u64ad\u653e\u5668" -> tag("\u64ad\u653e\u5668", "Player")
        "\u97f3\u4e50" -> tag("\u97f3\u4e50", "Music")
        "\u89d2\u6807" -> tag("\u89d2\u6807", "Badge")
        "\u6570\u5b57" -> tag("\u6570\u5b57", "Number")
        "\u6a2a\u5e45" -> tag("\u6a2a\u5e45", "Banner")
        "\u63d0\u9192" -> tag("\u63d0\u9192", "Reminder")
        "\u9876\u90e8" -> tag("\u9876\u90e8", "Top")
        "\u5e95\u90e8" -> tag("\u5e95\u90e8", "Bottom")
        "\u5143\u7d20" -> tag("\u5143\u7d20", "Element")
        "\u81ea\u52a8" -> tag("\u81ea\u52a8", "Auto")
        "\u8fd4\u56de\u9876\u90e8" -> tag("\u8fd4\u56de\u9876\u90e8", "Back to top")
        "\u5bfc\u822a" -> tag("\u5bfc\u822a", "Navigation")
        "\u8868\u5355" -> tag("\u8868\u5355", "Form")
        "\u586b\u5145" -> tag("\u586b\u5145", "Fill")
        "\u83b7\u53d6" -> tag("\u83b7\u53d6", "Fetch")
        "\u9a8c\u8bc1" -> tag("\u9a8c\u8bc1", "Validation")
        "\u62e6\u622a" -> tag("\u62e6\u622a", "Intercept")
        "\u6e05\u7a7a" -> tag("\u6e05\u7a7a", "Clear")
        "\u5bc6\u7801" -> tag("\u5bc6\u7801", "Password")
        "\u5207\u6362" -> tag("\u5207\u6362", "Switch")
        "\u500d\u901f" -> tag("\u500d\u901f", "Speed")
        "\u753b\u4e2d\u753b" -> tag("\u753b\u4e2d\u753b", "Picture-in-picture")
        "\u622a\u56fe" -> tag("\u622a\u56fe", "Screenshot")
        "\u653e\u5927" -> tag("\u653e\u5927", "Zoom")
        "\u97f3\u9891" -> tag("\u97f3\u9891", "Audio")
        "\u63a7\u5236" -> tag("\u63a7\u5236", "Control")
        "\u61d2\u52a0\u8f7d" -> tag("\u61d2\u52a0\u8f7d", "Lazy load")
        "\u5168\u5c4f" -> tag("\u5168\u5c4f", "Fullscreen")
        "\u7b80\u5316" -> tag("\u7b80\u5316", "Simplify")
        "\u89e3\u9501" -> tag("\u89e3\u9501", "Unlock")
        "\u6253\u5370" -> tag("\u6253\u5370", "Print")
        "\u4f18\u5316" -> tag("\u4f18\u5316", "Optimization")
        "\u8bed\u97f3" -> tag("\u8bed\u97f3", "Voice")
        "\u6717\u8bfb" -> tag("\u6717\u8bfb", "Read aloud")
        "\u7edf\u8ba1" -> tag("\u7edf\u8ba1", "Stats")
        "\u5b57\u6570" -> tag("\u5b57\u6570", "Word count")
        "\u641c\u7d22" -> tag("\u641c\u7d22", "Search")
        "\u5173\u952e\u8bcd" -> tag("\u5173\u952e\u8bcd", "Keyword")
        "\u8fc7\u6ee4" -> tag("\u8fc7\u6ee4", "Filter")
        "\u7a7a\u5143\u7d20" -> tag("\u7a7a\u5143\u7d20", "Empty element")
        "\u6e05\u7406" -> tag("\u6e05\u7406", "Cleanup")
        "\u8bc4\u8bba" -> tag("\u8bc4\u8bba", "Comment")
        "\u5e7f\u544a" -> tag("\u5e7f\u544a", "Ads")
        "\u963b\u6b62" -> tag("\u963b\u6b62", "Block")
        "\u906e\u7f69" -> tag("\u906e\u7f69", "Mask")
        "\u53cd\u68c0\u6d4b" -> tag("\u53cd\u68c0\u6d4b", "Anti-detection")
        "\u9632\u6296" -> tag("\u9632\u6296", "Debounce")
        "\u6027\u80fd" -> tag("\u6027\u80fd", "Performance")
        "\u8282\u6d41" -> tag("\u8282\u6d41", "Throttle")
        "\u7b49\u5f85" -> tag("\u7b49\u5f85", "Wait")
        "\u5f02\u6b65" -> tag("\u5f02\u6b65", "Async")
        "\u65e5\u671f" -> tag("\u65e5\u671f", "Date")
        "\u683c\u5f0f\u5316" -> tag("\u683c\u5f0f\u5316", "Format")
        "\u968f\u673a" -> tag("\u968f\u673a", "Random")
        "\u5b57\u7b26\u4e32" -> tag("\u5b57\u7b26\u4e32", "String")
        "\u5ef6\u8fdf" -> tag("\u5ef6\u8fdf", "Delay")
        "\u9519\u8bef\u5904\u7406" -> tag("\u9519\u8bef\u5904\u7406", "Error handling")
        "\u6587\u7ae0" -> tag("\u6587\u7ae0", "Article")
        "\u7ffb\u8bd1" -> tag("\u7ffb\u8bd1", "Translate")
        "\u9009\u4e2d" -> tag("\u9009\u4e2d", "Selection")
        "\u8f6c\u6362" -> tag("\u8f6c\u6362", "Convert")
        "\u5b9a\u65f6" -> tag("\u5b9a\u65f6", "Timer")
        "\u5237\u65b0" -> tag("\u5237\u65b0", "Refresh")
        "\u767b\u5f55" -> tag("\u767b\u5f55", "Login")
        "\u68c0\u6d4b" -> tag("\u68c0\u6d4b", "Detect")
        "\u63a7\u5236\u53f0" -> tag("\u63a7\u5236\u53f0", "Console")
        "\u65e5\u5fd7" -> tag("\u65e5\u5fd7", "Log")
        "\u68c0\u67e5" -> tag("\u68c0\u67e5", "Check")
        "\u76d1\u63a7" -> tag("\u76d1\u63a7", "Monitor")
        else -> tag
    }

    private fun tags(vararg items: String): List<String> = items.map(::translateTag)



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

        // 页面处理
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
     * 搜索代码块
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
                code = Strings.localized(
                    chinese = """// 短提示
NativeBridge.showToast('操作成功');

// 长提示
NativeBridge.showToast('请稍候，正在处理...', 'long');""",
                    english = """// Short toast
NativeBridge.showToast('Operation successful');

// Long toast
NativeBridge.showToast('Please wait, processing...', 'long');"""
                ),
                tags = tags("提示", "Toast", "消息")
            ),
            CodeSnippet(
                id = "native-vibrate",
                name = Strings.snippetVibrate,
                description = Strings.snippetVibrateDesc,
                code = Strings.localized(
                    chinese = """// 短震动（100ms）
NativeBridge.vibrate();

// 自定义时长震动
NativeBridge.vibrate(500);

// 模式震动（震动-暂停-震动）
NativeBridge.vibratePattern('100,200,100,200');""",
                    english = """// Short vibration (100ms)
NativeBridge.vibrate();

// Custom duration vibration
NativeBridge.vibrate(500);

// Vibration pattern (vibrate-pause-vibrate)
NativeBridge.vibratePattern('100,200,100,200');"""
                ),
                tags = tags("震动", "反馈", "触感")
            ),
            CodeSnippet(
                id = "native-copy",
                name = Strings.snippetCopyToClipboard,
                description = Strings.snippetCopyToClipboardDesc,
                code = Strings.localized(
                    chinese = """function copyText(text) {
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
                    english = """function copyText(text) {
    const success = NativeBridge.copyToClipboard(text);
    if (success) {
        NativeBridge.showToast('Copied to clipboard');
        NativeBridge.vibrate(50);
    } else {
        NativeBridge.showToast('Copy failed');
    }
}

// Example: Copy selected text
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('copy-btn')) {
        const text = e.target.dataset.text;
        copyText(text);
    }
});"""
                ),
                tags = tags("复制", "剪贴板", "clipboard")
            ),
            CodeSnippet(
                id = "native-share",
                name = Strings.snippetShareContent,
                description = Strings.snippetShareContentDesc,
                code = Strings.localized(
                    chinese = """// 分享文本和链接
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
                    english = """// Share text and link
function shareContent(title, text, url) {
    NativeBridge.share(title, text, url);
}

// Share current page
function shareCurrentPage() {
    NativeBridge.share(
        document.title,
        'Found an interesting page',
        location.href
    );
}

// Add share button
const shareBtn = document.createElement('button');
shareBtn.textContent = 'Share';
shareBtn.onclick = shareCurrentPage;"""
                ),
                tags = tags("分享", "share", "社交")
            ),
            CodeSnippet(
                id = "native-save-image",
                name = Strings.snippetSaveImageToGallery,
                description = Strings.snippetSaveImageToGalleryDesc,
                code = Strings.localized(
                    chinese = """// 保存图片到相册
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
                    english = """// Save image to gallery
function saveImage(imageUrl, filename) {
    NativeBridge.saveImageToGallery(imageUrl, filename || '');
}

// Add long-press save for all images
document.querySelectorAll('img').forEach(img => {
    img.addEventListener('contextmenu', (e) => {
        e.preventDefault();
        if (confirm('Save image to gallery?')) {
            saveImage(img.src);
        }
    });
});"""
                ),
                tags = tags("保存", "图片", "相册", "下载")
            ),
            CodeSnippet(
                id = "native-save-video",
                name = Strings.snippetSaveVideoToGallery,
                description = Strings.snippetSaveVideoToGalleryDesc,
                code = Strings.localized(
                    chinese = """// 保存视频到相册
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
                    english = """// Save video to gallery
function saveVideo(videoUrl, filename) {
    NativeBridge.saveVideoToGallery(videoUrl, filename || '');
}

// Add download button for videos
document.querySelectorAll('video').forEach(video => {
    const btn = document.createElement('button');
    btn.textContent = 'Save Video';
    btn.style.cssText = 'position:absolute;top:10px;right:10px;z-index:999;';
    btn.onclick = () => saveVideo(video.src);
    video.parentElement.style.position = 'relative';
    video.parentElement.appendChild(btn);
});"""
                ),
                tags = tags("保存", "视频", "相册", "下载")
            ),
            CodeSnippet(
                id = "native-open-url",
                name = Strings.snippetOpenInBrowser,
                description = Strings.snippetOpenInBrowserDesc,
                code = Strings.localized(
                    chinese = """// 用系统浏览器打开链接
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
                    english = """// Open link in system browser
function openInBrowser(url) {
    NativeBridge.openUrl(url);
}

// Intercept external links and open in browser
document.addEventListener('click', (e) => {
    const link = e.target.closest('a');
    if (link && link.href && !link.href.startsWith(location.origin)) {
        e.preventDefault();
        openInBrowser(link.href);
    }
});"""
                ),
                tags = tags("浏览器", "链接", "外部")
            ),
            CodeSnippet(
                id = "native-device-info",
                name = Strings.snippetDeviceInfo,
                description = Strings.snippetDeviceInfoDesc,
                code = Strings.localized(
                    chinese = """// 获取设备信息
const deviceInfo = JSON.parse(NativeBridge.getDeviceInfo());
console.log('设备型号:', deviceInfo.model);
console.log('Android 版本:', deviceInfo.androidVersion);
console.log('屏幕尺寸:', deviceInfo.screenWidth, 'x', deviceInfo.screenHeight);

// 获取应用信息
const appInfo = JSON.parse(NativeBridge.getAppInfo());
console.log('应用版本:', appInfo.versionName);

// 根据设备调整布局
if (deviceInfo.screenWidth < 400) {
    document.body.classList.add('small-screen');
}""",
                    english = """// Get device info
const deviceInfo = JSON.parse(NativeBridge.getDeviceInfo());
console.log('Model:', deviceInfo.model);
console.log('Android Version:', deviceInfo.androidVersion);
console.log('Screen Size:', deviceInfo.screenWidth, 'x', deviceInfo.screenHeight);

// Get app info
const appInfo = JSON.parse(NativeBridge.getAppInfo());
console.log('App Version:', appInfo.versionName);

// Adjust layout based on device
if (deviceInfo.screenWidth < 400) {
    document.body.classList.add('small-screen');
}"""
                ),
                tags = tags("设备", "信息", "屏幕")
            ),
            CodeSnippet(
                id = "native-network",
                name = Strings.snippetNetworkStatus,
                description = Strings.snippetNetworkStatusDesc,
                code = Strings.localized(
                    chinese = """// 检查网络是否可用
if (NativeBridge.isNetworkAvailable()) {
    console.log('网络可用');
} else {
    NativeBridge.showToast('当前无网络连接');
}

// 获取网络类型
const networkType = NativeBridge.getNetworkType();
console.log('网络类型:', networkType); // wifi, mobile, none

// 根据网络类型调整行为
if (networkType === 'mobile') {
    // 移动网络下减少数据使用
    document.querySelectorAll('video').forEach(v => v.preload = 'none');
}""",
                    english = """// Check network availability
if (NativeBridge.isNetworkAvailable()) {
    console.log('Network available');
} else {
    NativeBridge.showToast('No network connection');
}

// Get network type
const networkType = NativeBridge.getNetworkType();
console.log('Network Type:', networkType); // wifi, mobile, none

// Adjust behavior based on network type
if (networkType === 'mobile') {
    // Reduce data usage on mobile data
    document.querySelectorAll('video').forEach(v => v.preload = 'none');
}"""
                ),
                tags = tags("网络", "WiFi", "流量")
            ),
            CodeSnippet(
                id = "native-save-file",
                name = Strings.snippetSaveFile,
                description = Strings.snippetSaveFileDesc,
                code = Strings.localized(
                    chinese = """// 保存文本文件
function saveTextFile(content, filename) {
    NativeBridge.saveToFile(content, filename, 'text/plain');
}

// 保存 JSON 文件
function saveJsonFile(data, filename) {
    const json = JSON.stringify(data, null, 2);
    NativeBridge.saveToFile(json, filename, 'application/json');
}

// 导出页面数据
const pageData = {
    title: document.title,
    url: location.href,
    content: document.body.innerText.substring(0, 1000)
};
saveJsonFile(pageData, 'page_data.json');""",
                    english = """// Save text file
function saveTextFile(content, filename) {
    NativeBridge.saveToFile(content, filename, 'text/plain');
}

// Save JSON file
function saveJsonFile(data, filename) {
    const json = JSON.stringify(data, null, 2);
    NativeBridge.saveToFile(json, filename, 'application/json');
}

// Export page data
const pageData = {
    title: document.title,
    url: location.href,
    content: document.body.innerText.substring(0, 1000)
};
saveJsonFile(pageData, 'page_data.json');"""
                ),
                tags = tags("保存", "文件", "导出")
            ),
            CodeSnippet(
                id = "native-image-download-btn",
                name = Strings.snippetImageDownloadBtn,
                description = Strings.snippetImageDownloadBtnDesc,
                code = Strings.localized(
                    chinese = """// 为所有图片添加下载按钮
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
                    english = """// Add download button to all images
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
    .observe(document.body, { childList: true, subtree: true });"""
                ),
                tags = tags("图片", "下载", "按钮", "悬浮")
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
                code = Strings.localized(
                    chinese = """const element = document.querySelector('选择器');
if (element) {
    // 对元素进行操作
}""",
                    english = """const element = document.querySelector('selector');
if (element) {
    // Operate on the element
}"""
                ),
                tags = tags("查询", "选择器")
            ),
            CodeSnippet(
                id = "dom-query-all",
                name = Strings.snippetQueryAll,
                description = Strings.snippetQueryAllDesc,
                code = Strings.localized(
                    chinese = """document.querySelectorAll('选择器').forEach(el => {
    // 对每个元素进行操作
});""",
                    english = """document.querySelectorAll('selector').forEach(el => {
    // Operate on each element
});"""
                ),
                tags = tags("查询", "遍历")
            ),
            CodeSnippet(
                id = "dom-hide-element",
                name = Strings.snippetHideElement,
                description = Strings.snippetHideElementDesc,
                code = Strings.localized(
                    chinese = """function hideElement(selector) {
    document.querySelectorAll(selector).forEach(el => {
        el.style.setProperty('display', 'none', 'important');
    });
}
hideElement('选择器');""",
                    english = """function hideElement(selector) {
    document.querySelectorAll(selector).forEach(el => {
        el.style.setProperty('display', 'none', 'important');
    });
}
hideElement('selector');"""
                ),
                tags = tags("隐藏", "样式")
            ),
            CodeSnippet(
                id = "dom-remove-element",
                name = Strings.snippetRemoveElement,
                description = Strings.snippetRemoveElementDesc,
                code = Strings.localized(
                    chinese = """function removeElement(selector) {
    document.querySelectorAll(selector).forEach(el => el.remove());
}
removeElement('选择器');""",
                    english = """function removeElement(selector) {
    document.querySelectorAll(selector).forEach(el => el.remove());
}
removeElement('selector');"""
                ),
                tags = tags("删除", "移除")
            ),
            CodeSnippet(
                id = "dom-create-element",
                name = Strings.snippetCreateElement,
                description = Strings.snippetCreateElementDesc,
                code = Strings.localized(
                    chinese = """const newElement = document.createElement('div');
newElement.id = 'my-element';
newElement.className = 'my-class';
newElement.textContent = '内容';
newElement.style.cssText = 'color: red; font-size: 14px;';
document.body.appendChild(newElement);""",
                    english = """const newElement = document.createElement('div');
newElement.id = 'my-element';
newElement.className = 'my-class';
newElement.textContent = 'Content';
newElement.style.cssText = 'color: red; font-size: 14px;';
document.body.appendChild(newElement);"""
                ),
                tags = tags("创建", "添加")
            ),
            CodeSnippet(
                id = "dom-modify-text",
                name = Strings.snippetModifyText,
                description = Strings.snippetModifyTextDesc,
                code = Strings.localized(
                    chinese = """const element = document.querySelector('选择器');
if (element) {
    element.textContent = '新的文本内容';
    // 或者使用 innerHTML 支持 HTML
    // element.innerHTML = '<strong>加粗文本</strong>';
}""",
                    english = """const element = document.querySelector('selector');
if (element) {
    element.textContent = 'New text content';
    // Or use innerHTML to support HTML
    // element.innerHTML = '<strong>Bold text</strong>';
}"""
                ),
                tags = tags("文本", "修改")
            ),
            CodeSnippet(
                id = "dom-modify-attribute",
                name = Strings.snippetModifyAttr,
                description = Strings.snippetModifyAttrDesc,
                code = Strings.localized(
                    chinese = """const element = document.querySelector('选择器');
if (element) {
    element.setAttribute('属性名', '属性值');
    const value = element.getAttribute('属性名');
    element.removeAttribute('属性名');
}""",
                    english = """const element = document.querySelector('selector');
if (element) {
    element.setAttribute('attribute', 'value');
    const value = element.getAttribute('attribute');
    element.removeAttribute('attribute');
}"""
                ),
                tags = tags("属性", "修改")
            ),
            CodeSnippet(
                id = "dom-insert-before",
                name = Strings.snippetInsertBefore,
                description = Strings.snippetInsertBeforeDesc,
                code = Strings.localized(
                    chinese = """const target = document.querySelector('目标选择器');
const newEl = document.createElement('div');
newEl.textContent = '新内容';
target.parentNode.insertBefore(newEl, target);""",
                    english = """const target = document.querySelector('target-selector');
const newEl = document.createElement('div');
newEl.textContent = 'New content';
target.parentNode.insertBefore(newEl, target);"""
                ),
                tags = tags("插入", "位置")
            ),
            CodeSnippet(
                id = "dom-insert-after",
                name = Strings.snippetInsertAfter,
                description = Strings.snippetInsertAfterDesc,
                code = Strings.localized(
                    chinese = """const target = document.querySelector('目标选择器');
const newEl = document.createElement('div');
newEl.textContent = '新内容';
target.parentNode.insertBefore(newEl, target.nextSibling);""",
                    english = """const target = document.querySelector('target-selector');
const newEl = document.createElement('div');
newEl.textContent = 'New content';
target.parentNode.insertBefore(newEl, target.nextSibling);"""
                ),
                tags = tags("插入", "位置")
            ),
            CodeSnippet(
                id = "dom-clone-element",
                name = Strings.snippetCloneElement,
                description = Strings.snippetCloneElementDesc,
                code = Strings.localized(
                    chinese = """const original = document.querySelector('选择器');
const clone = original.cloneNode(true);
clone.id = 'cloned-element';
document.body.appendChild(clone);""",
                    english = """const original = document.querySelector('selector');
const clone = original.cloneNode(true);
clone.id = 'cloned-element';
document.body.appendChild(clone);"""
                ),
                tags = tags("克隆", "复制")
            ),
            CodeSnippet(
                id = "dom-wrap-element",
                name = Strings.snippetWrapElement,
                description = Strings.snippetWrapElementDesc,
                code = Strings.localized(
                    chinese = """function wrapElement(selector, wrapperTag = 'div') {
    document.querySelectorAll(selector).forEach(el => {
        const wrapper = document.createElement(wrapperTag);
        el.parentNode.insertBefore(wrapper, el);
        wrapper.appendChild(el);
    });
}
wrapElement('img', 'figure');""",
                    english = """function wrapElement(selector, wrapperTag = 'div') {
    document.querySelectorAll(selector).forEach(el => {
        const wrapper = document.createElement(wrapperTag);
        el.parentNode.insertBefore(wrapper, el);
        wrapper.appendChild(el);
    });
}
wrapElement('img', 'figure');"""
                ),
                tags = tags("包裹", "结构")
            ),
            CodeSnippet(
                id = "dom-replace-element",
                name = Strings.snippetReplaceElement,
                description = Strings.snippetReplaceElementDesc,
                code = Strings.localized(
                    chinese = """function replaceElement(selector, newHtml) {
    document.querySelectorAll(selector).forEach(el => {
        const temp = document.createElement('div');
        temp.innerHTML = newHtml;
        el.replaceWith(temp.firstChild);
    });
}
replaceElement('.old-class', '<div class="new-class">新内容</div>');""",
                    english = """function replaceElement(selector, newHtml) {
    document.querySelectorAll(selector).forEach(el => {
        const temp = document.createElement('div');
        temp.innerHTML = newHtml;
        el.replaceWith(temp.firstChild);
    });
}
replaceElement('.old-class', '<div class="new-class">New content</div>');"""
                ),
                tags = tags("替换", "修改")
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
                tags = tags("CSS", "注入")
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
                tags = tags("样式", "内联")
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
                tags = tags("类名", "class")
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
                tags = tags("深色", "主题")
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
                tags = tags("护眼", "暖色")
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
                tags = tags("灰度", "滤镜")
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
                tags = tags("字体", "样式")
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
                tags = tags("字体", "大小")
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
                tags = tags("滚动条", "隐藏")
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
                tags = tags("链接", "高亮")
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
                tags = tags("宽度", "阅读")
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
                tags = tags("行高", "阅读")
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
        // 处理点击
    }
});""",
                tags = tags("点击", "事件")
            ),
            CodeSnippet(
                id = "event-keyboard",
                name = Strings.snippetKeyboardEvent,
                description = Strings.snippetKeyboardEventDesc,
                code = """document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        // 处理回车键
    }
    if (e.ctrlKey && e.key === 's') {
        e.preventDefault();
        // 处理 Ctrl+S
    }
});""",
                tags = tags("键盘", "快捷键")
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
        // 显示返回顶部按钮
    }
});""",
                tags = tags("滚动", "位置")
            ),
            CodeSnippet(
                id = "event-mutation",
                name = Strings.snippetMutationEvent,
                description = Strings.snippetMutationEventDesc,
                code = """const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
            if (node.nodeType === 1) {
                // 处理新添加的元素
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
                tags = tags("监听", "DOM变化", "动态")
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
        // 处理窗口大小变化
    }, 100);
});""",
                tags = tags("窗口", "大小")
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
                tags = tags("复制", "剪贴板")
            ),
            CodeSnippet(
                id = "event-contextmenu",
                name = Strings.snippetContextMenu,
                description = Strings.snippetContextMenuDesc,
                code = """document.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    // 显示自定义菜单或阻止默认菜单
});""",
                tags = tags("右键", "菜单")
            ),
            CodeSnippet(
                id = "event-visibility",
                name = Strings.snippetVisibility,
                description = Strings.snippetVisibilityDesc,
                code = """document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        // 页面切换到后台
        console.log('页面隐藏');
    } else {
        // 页面切换到前台
        console.log('页面显示');
    }
});""",
                tags = tags("可见性", "后台")
            ),
            CodeSnippet(
                id = "event-beforeunload",
                name = Strings.snippetBeforeUnload,
                description = Strings.snippetBeforeUnloadDesc,
                code = """window.addEventListener('beforeunload', (e) => {
    // 保存数据
    localStorage.setItem('lastVisit', Date.now());

    // 如需提示用户，取消注释以下代码
    // e.preventDefault();
    // e.returnValue = '';
});""",
                tags = tags("关闭", "保存")
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
                tags = tags("触摸", "手势")
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
                tags = tags("长按", "触摸")
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
                tags = tags("存储", "保存")
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
                tags = tags("存储", "读取")
            ),
            CodeSnippet(
                id = "storage-session",
                name = Strings.snippetSessionStorage,
                description = Strings.snippetSessionStorageDesc,
                code = """// 保存（页面关闭后清除）
sessionStorage.setItem('key', 'value');
// 读取
const value = sessionStorage.getItem('key');
// 删除
sessionStorage.removeItem('key');
// 清空所有
sessionStorage.clear();""",
                tags = tags("会话", "临时")
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
                tags = tags("Cookie", "设置")
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
                tags = tags("Cookie", "读取")
            ),
            CodeSnippet(
                id = "storage-cookie-delete",
                name = Strings.snippetDeleteCookie,
                description = Strings.snippetDeleteCookieDesc,
                code = """function deleteCookie(name) {
    document.cookie = name + '=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
}
deleteCookie('myCookie');""",
                tags = tags("Cookie", "删除")
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
                tags = tags("IndexedDB", "大数据")
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
                tags = tags("GET", "请求")
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
                tags = tags("POST", "提交")
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
                tags = tags("超时", "请求")
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
                tags = tags("重试", "请求")
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

// 下载 Blob 数据
function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    downloadFile(url, filename);
    URL.revokeObjectURL(url);
}""",
                tags = tags("下载", "文件")
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
                tags = tags("JSONP", "跨域")
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
                tags = tags("表格", "提取")
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
                tags = tags("链接", "提取")
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
                tags = tags("图片", "提取")
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
                tags = tags("导出", "JSON")
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
                tags = tags("导出", "CSV")
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
                tags = tags("URL", "解析")
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
                tags = tags("URL", "构建")
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
                tags = tags("按钮", "悬浮")
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
                tags = tags("提示", "消息")
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
                tags = tags("弹窗", "对话框")
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
                tags = tags("进度", "阅读")
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
                tags = tags("加载", "动画")
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
                tags = tags("通知", "Snackbar")
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
                tags = tags("工具栏", "悬浮")
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
                tags = tags("侧边栏", "面板")
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
                tags = tags("拖动", "交互")
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
                tags = tags("播放器", "音乐")
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
                tags = tags("通知", "浏览器")
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
                tags = tags("角标", "数字")
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
                tags = tags("横幅", "提醒")
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
                tags = tags("滚动", "顶部")
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
                tags = tags("滚动", "底部")
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
                tags = tags("滚动", "元素")
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
                tags = tags("滚动", "自动")
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
                tags = tags("按钮", "返回顶部")
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
                tags = tags("滚动", "加载")
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
                tags = tags("动画", "滚动")
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
                tags = tags("导航", "监听")
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
                tags = tags("表单", "填充")
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
                tags = tags("表单", "获取")
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
                tags = tags("表单", "验证")
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
                tags = tags("表单", "拦截")
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
                tags = tags("表单", "清空")
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
                tags = tags("密码", "切换")
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
                tags = tags("视频", "倍速")
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
                tags = tags("视频", "画中画")
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

    // 下载截图
    const link = document.createElement('a');
    link.download = 'screenshot_' + Date.now() + '.png';
    link.href = canvas.toDataURL('image/png');
    link.click();

    return canvas.toDataURL('image/png');
}
captureVideoFrame();""",
                tags = tags("视频", "截图")
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
                tags = tags("图片", "放大")
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
                tags = tags("图片", "下载")
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

// 暂停所有音视频
function pauseAll() {
    document.querySelectorAll('video, audio').forEach(media => {
        media.pause();
    });
}

// 设置音量 (0-1)
function setVolume(volume) {
    document.querySelectorAll('video, audio').forEach(media => {
        media.volume = Math.max(0, Math.min(1, volume));
    });
}

muteAll(); // 静音所有""",
                tags = tags("音频", "控制")
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
                tags = tags("图片", "懒加载")
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

// 视频全屏
function videoFullscreen() {
    const video = document.querySelector('video');
    if (video) toggleFullscreen(video);
}""",
                tags = tags("全屏", "视频")
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
                tags = tags("阅读", "简化")
            ),
            CodeSnippet(
                id = "enhance-copy-unlock",
                name = Strings.snippetCopyUnlock,
                description = Strings.snippetCopyUnlockDesc,
                code = """// 注入样式
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
                tags = tags("复制", "解锁")
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
                tags = tags("打印", "优化")
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
        // 显示朗读按钮
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
                tags = tags("语音", "朗读")
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
                tags = tags("统计", "字数")
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
                tags = tags("搜索", "高亮")
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
                tags = tags("关键词", "过滤")
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
                tags = tags("空元素", "清理")
            ),
            CodeSnippet(
                id = "filter-comments",
                name = Strings.snippetFilterComments,
                description = Strings.snippetFilterCommentsDesc,
                code = """function filterComments(options = {}) {
    const { minLength = 0, keywords = [], selector = '[class*="comment"]' } = options;

    document.querySelectorAll(selector).forEach(comment => {
        const text = comment.textContent;

        // 过滤短评论
        if (text.length < minLength) {
            comment.style.opacity = '0.3';
        }

        // 过滤包含关键词的评论
        if (keywords.some(k => text.toLowerCase().includes(k.toLowerCase()))) {
            comment.style.display = 'none';
        }
    });
}
filterComments({ minLength: 10, keywords: ['广告', '推广'] });""",
                tags = tags("评论", "过滤")
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
                tags = tags("图片", "过滤")
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
        } catch(e) {}
    });
}

hideAds();
const observer = new MutationObserver(hideAds);
observer.observe(document.body, { childList: true, subtree: true });""",
                tags = tags("广告", "隐藏")
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
                tags = tags("弹窗", "阻止")
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
                tags = tags("遮罩", "移除")
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
                tags = tags("CSS", "广告")
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
                tags = tags("反检测", "广告")
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
                tags = tags("防抖", "性能")
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
                tags = tags("节流", "性能")
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
                tags = tags("等待", "异步")
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
                tags = tags("复制", "剪贴板")
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
                tags = tags("日期", "格式化")
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
                tags = tags("随机", "字符串")
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
                tags = tags("延迟", "异步")
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
                tags = tags("重试", "错误处理")
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
                tags = tags("提取", "文章")
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
                tags = tags("替换", "文本")
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
                tags = tags("翻译", "选中")
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

    // 列表
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
                tags = tags("Markdown", "转换")
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
                tags = tags("拦截", "fetch")
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
                tags = tags("拦截", "XHR")
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
                tags = tags("拦截", "WebSocket")
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
                tags = tags("阻止", "请求")
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
                tags = tags("自动", "点击")
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
// 停止: clearInterval(timer);""",
                tags = tags("定时", "点击")
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
                tags = tags("表单", "自动")
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
                tags = tags("刷新", "定时")
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
                tags = tags("滚动", "加载")
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
                tags = tags("登录", "检测")
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
                tags = tags("控制台", "日志")
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
                tags = tags("检查", "元素")
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
                tags = tags("性能", "监控")
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
                tags = tags("网络", "日志")
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
