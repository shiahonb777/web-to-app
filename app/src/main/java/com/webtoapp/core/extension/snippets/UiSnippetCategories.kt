package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.Strings

internal fun uiComponents() = CodeSnippetCategory(
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
internal fun floatingWidgets() = CodeSnippetCategory(
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
internal fun mediaOperations() = CodeSnippetCategory(
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
