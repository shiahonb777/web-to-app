package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.AppStringsProvider

internal fun eventListeners() = CodeSnippetCategory(
        id = "events",
        name = AppStringsProvider.current().snippetEvent,
        icon = "👆",
        description = AppStringsProvider.current().snippetEventDesc,
        snippets = listOf(
            CodeSnippet(
                id = "event-click",
                name = AppStringsProvider.current().snippetClickEvent,
                description = AppStringsProvider.current().snippetClickEventDesc,
                code = """document.addEventListener('click', (e) => {
    const target = e.target;
    if (target.matches('选择器')) {
        e.preventDefault();
        // Handle.
    }
});""",
                tags = listOf(AppStringsProvider.current().tagClick, AppStringsProvider.current().tagEvent)
            ),
            CodeSnippet(
                id = "event-keyboard",
                name = AppStringsProvider.current().snippetKeyboardEvent,
                description = AppStringsProvider.current().snippetKeyboardEventDesc,
                code = """document.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
        // Handle.
    }
    if (e.ctrlKey && e.key === 's') {
        e.preventDefault();
        // Handle Ctrl+S
    }
});""",
                tags = listOf(AppStringsProvider.current().tagKeyboard, AppStringsProvider.current().tagShortcut)
            ),
            CodeSnippet(
                id = "event-scroll",
                name = AppStringsProvider.current().snippetScrollEvent,
                description = AppStringsProvider.current().snippetScrollEventDesc,
                code = """let lastScrollTop = 0;
window.addEventListener('scroll', () => {
    const scrollTop = window.scrollY;
    const direction = scrollTop > lastScrollTop ? 'down' : 'up';
    lastScrollTop = scrollTop;
    
    if (scrollTop > 300) {
        // Show by.
    }
});""",
                tags = listOf(AppStringsProvider.current().tagScroll, AppStringsProvider.current().tagPosition)
            ),
            CodeSnippet(
                id = "event-mutation",
                name = AppStringsProvider.current().snippetMutationEvent,
                description = AppStringsProvider.current().snippetMutationEventDesc,
                code = """const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
            if (node.nodeType === 1) {
                // Handle.
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
                tags = listOf(AppStringsProvider.current().tagListen, AppStringsProvider.current().tagDomChange, AppStringsProvider.current().tagDynamic)
            ),
            CodeSnippet(
                id = "event-resize",
                name = AppStringsProvider.current().snippetResizeEvent,
                description = AppStringsProvider.current().snippetResizeEventDesc,
                code = """let resizeTimeout;
window.addEventListener('resize', () => {
    clearTimeout(resizeTimeout);
    resizeTimeout = setTimeout(() => {
        const width = window.innerWidth;
        const height = window.innerHeight;
        // Handle large small.
    }, 100);
});""",
                tags = listOf(AppStringsProvider.current().tagWindow, AppStringsProvider.current().tagSize)
            ),
            CodeSnippet(
                id = "event-copy",
                name = AppStringsProvider.current().snippetCopyEvent,
                description = AppStringsProvider.current().snippetCopyEventDesc,
                code = """document.addEventListener('copy', (e) => {
    const selection = window.getSelection().toString();
    e.clipboardData.setData('text/plain', selection + '\\n来源: ' + location.href);
    e.preventDefault();
});""",
                tags = listOf(AppStringsProvider.current().tagCopy, AppStringsProvider.current().tagClipboard)
            ),
            CodeSnippet(
                id = "event-contextmenu",
                name = AppStringsProvider.current().snippetContextMenu,
                description = AppStringsProvider.current().snippetContextMenuDesc,
                code = """document.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    // Show single or single.
});""",
                tags = listOf(AppStringsProvider.current().tagRightClick, AppStringsProvider.current().tagMenu)
            ),
            CodeSnippet(
                id = "event-visibility",
                name = AppStringsProvider.current().snippetVisibility,
                description = AppStringsProvider.current().snippetVisibilityDesc,
                code = """document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        // Page to after.
        console.log('页面隐藏');
    } else {
        // Page to before.
        console.log('页面显示');
    }
});""",
                tags = listOf(AppStringsProvider.current().tagVisibility, AppStringsProvider.current().tagBackground)
            ),
            CodeSnippet(
                id = "event-beforeunload",
                name = AppStringsProvider.current().snippetBeforeUnload,
                description = AppStringsProvider.current().snippetBeforeUnloadDesc,
                code = """window.addEventListener('beforeunload', (e) => {
    // Save.
    localStorage.setItem('lastVisit', Date.now());
    
    // use .
    // e.preventDefault();
    // e.returnValue = '';
});""",
                tags = listOf(AppStringsProvider.current().tagClose, AppStringsProvider.current().tagSave)
            ),
            CodeSnippet(
                id = "event-touch",
                name = AppStringsProvider.current().snippetTouchEvent,
                description = AppStringsProvider.current().snippetTouchEventDesc,
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
                tags = listOf(AppStringsProvider.current().tagTouch, AppStringsProvider.current().tagGesture)
            ),
            CodeSnippet(
                id = "event-long-press",
                name = AppStringsProvider.current().snippetLongPress,
                description = AppStringsProvider.current().snippetLongPressDesc,
                code = """let pressTimer = null;
document.addEventListener('touchstart', (e) => {
    pressTimer = setTimeout(() => {
        // by.
        console.log('长按:', e.target);
    }, 500);
});

document.addEventListener('touchend', () => {
    clearTimeout(pressTimer);
});

document.addEventListener('touchmove', () => {
    clearTimeout(pressTimer);
});""",
                tags = listOf(AppStringsProvider.current().tagLongPress, AppStringsProvider.current().tagTouch)
            )
        )
)
internal fun notifications() = CodeSnippetCategory(
        id = "notifications",
        name = AppStringsProvider.current().snippetNotification,
        icon = "🔔",
        description = AppStringsProvider.current().snippetNotificationDesc,
        snippets = listOf(
            CodeSnippet(
                id = "notif-browser",
                name = AppStringsProvider.current().snippetBrowserNotif,
                description = AppStringsProvider.current().snippetBrowserNotifDesc,
                code = """async function sendNotification(title, body, icon) {
    if (Notification.permission !== 'granted') {
        await Notification.requestPermission();
    }
    
    if (Notification.permission === 'granted') {
        new Notification(title, { body, icon });
    }
}
sendNotification('提醒', '这是一条通知消息');""",
                tags = listOf(AppStringsProvider.current().tagNotification, AppStringsProvider.current().tagBrowser)
            ),
            CodeSnippet(
                id = "notif-badge",
                name = AppStringsProvider.current().snippetBadge,
                description = AppStringsProvider.current().snippetBadgeDesc,
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
                tags = listOf(AppStringsProvider.current().tagBadge, AppStringsProvider.current().tagNumber)
            ),
            CodeSnippet(
                id = "notif-alert-banner",
                name = AppStringsProvider.current().snippetBanner,
                description = AppStringsProvider.current().snippetBannerDesc,
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
                tags = listOf(AppStringsProvider.current().tagBanner, AppStringsProvider.current().tagReminder)
            )
        )
)
internal fun scrollOperations() = CodeSnippetCategory(
        id = "scroll",
        name = AppStringsProvider.current().snippetScroll,
        icon = "📜",
        description = AppStringsProvider.current().snippetScrollDesc,
        snippets = listOf(
            CodeSnippet(
                id = "scroll-to-top",
                name = AppStringsProvider.current().snippetScrollToTop,
                description = AppStringsProvider.current().snippetScrollToTopDesc,
                code = """function scrollToTop(smooth = true) {
    window.scrollTo({
        top: 0,
        behavior: smooth ? 'smooth' : 'auto'
    });
}
scrollToTop();""",
                tags = listOf(AppStringsProvider.current().tagScroll, AppStringsProvider.current().tagTop)
            ),
            CodeSnippet(
                id = "scroll-to-bottom",
                name = AppStringsProvider.current().snippetScrollToBottom,
                description = AppStringsProvider.current().snippetScrollToBottomDesc,
                code = """function scrollToBottom(smooth = true) {
    window.scrollTo({
        top: document.documentElement.scrollHeight,
        behavior: smooth ? 'smooth' : 'auto'
    });
}
scrollToBottom();""",
                tags = listOf(AppStringsProvider.current().tagScroll, AppStringsProvider.current().tagBottom)
            ),
            CodeSnippet(
                id = "scroll-to-element",
                name = AppStringsProvider.current().snippetScrollToElement,
                description = AppStringsProvider.current().snippetScrollToElementDesc,
                code = """function scrollToElement(selector, offset = 0) {
    const element = document.querySelector(selector);
    if (element) {
        const top = element.getBoundingClientRect().top + window.scrollY - offset;
        window.scrollTo({ top, behavior: 'smooth' });
    }
}
scrollToElement('#target-section', 100);""",
                tags = listOf(AppStringsProvider.current().tagScroll, AppStringsProvider.current().tagElement)
            ),
            CodeSnippet(
                id = "scroll-auto",
                name = AppStringsProvider.current().snippetAutoScroll,
                description = AppStringsProvider.current().snippetAutoScrollDesc,
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

// by.
document.addEventListener('keydown', (e) => {
    if (e.code === 'Space' && e.target === document.body) {
        e.preventDefault();
        toggleAutoScroll();
    }
});""",
                tags = listOf(AppStringsProvider.current().tagScroll, AppStringsProvider.current().tagAuto)
            ),
            CodeSnippet(
                id = "scroll-back-to-top-btn",
                name = AppStringsProvider.current().snippetBackToTopBtn,
                description = AppStringsProvider.current().snippetBackToTopBtnDesc,
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
                tags = listOf(AppStringsProvider.current().tagButton, AppStringsProvider.current().tagBackToTop)
            ),
            CodeSnippet(
                id = "scroll-infinite",
                name = AppStringsProvider.current().snippetInfiniteScroll,
                description = AppStringsProvider.current().snippetInfiniteScrollDesc,
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
    // in multiple.
});""",
                tags = listOf(AppStringsProvider.current().tagScroll, AppStringsProvider.current().tagLoading)
            ),
            CodeSnippet(
                id = "scroll-reveal",
                name = AppStringsProvider.current().snippetScrollReveal,
                description = AppStringsProvider.current().snippetScrollRevealDesc,
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
                tags = listOf(AppStringsProvider.current().tagAnimation, AppStringsProvider.current().tagScroll)
            ),
            CodeSnippet(
                id = "scroll-spy",
                name = AppStringsProvider.current().snippetScrollSpy,
                description = AppStringsProvider.current().snippetScrollSpyDesc,
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
                tags = listOf(AppStringsProvider.current().tagNavigation, AppStringsProvider.current().tagListen)
            )
        )
)
internal fun formOperations() = CodeSnippetCategory(
        id = "form",
        name = AppStringsProvider.current().snippetForm,
        icon = "📝",
        description = AppStringsProvider.current().snippetFormDesc,
        snippets = listOf(
            CodeSnippet(
                id = "form-auto-fill",
                name = AppStringsProvider.current().snippetAutoFill,
                description = AppStringsProvider.current().snippetAutoFillDesc,
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
                tags = listOf(AppStringsProvider.current().tagForm, AppStringsProvider.current().tagFill)
            ),
            CodeSnippet(
                id = "form-get-values",
                name = AppStringsProvider.current().snippetGetFormData,
                description = AppStringsProvider.current().snippetGetFormDataDesc,
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
                tags = listOf(AppStringsProvider.current().tagForm, AppStringsProvider.current().tagGet)
            ),
            CodeSnippet(
                id = "form-validate",
                name = AppStringsProvider.current().snippetFormValidate,
                description = AppStringsProvider.current().snippetFormValidateDesc,
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
                tags = listOf(AppStringsProvider.current().tagForm, AppStringsProvider.current().tagValidate)
            ),
            CodeSnippet(
                id = "form-submit-intercept",
                name = AppStringsProvider.current().snippetFormIntercept,
                description = AppStringsProvider.current().snippetFormInterceptDesc,
                code = """document.querySelectorAll('form').forEach(form => {
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        
        const formData = new FormData(form);
        const data = Object.fromEntries(formData);
        
        console.log('表单数据:', data);
        
        // can in.
        // after is.
        // form.submit();
    });
});""",
                tags = listOf(AppStringsProvider.current().tagForm, AppStringsProvider.current().tagIntercept)
            ),
            CodeSnippet(
                id = "form-clear",
                name = AppStringsProvider.current().snippetFormClear,
                description = AppStringsProvider.current().snippetFormClearDesc,
                code = """function clearForm(formSelector) {
    const form = document.querySelector(formSelector);
    if (form) {
        form.reset();
        // change.
        form.querySelectorAll('input, select, textarea').forEach(el => {
            el.dispatchEvent(new Event('change', { bubbles: true }));
        });
    }
}
clearForm('#myForm');""",
                tags = listOf(AppStringsProvider.current().tagForm, AppStringsProvider.current().tagClear)
            ),
            CodeSnippet(
                id = "form-password-toggle",
                name = AppStringsProvider.current().snippetPasswordToggle,
                description = AppStringsProvider.current().snippetPasswordToggleDesc,
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
                tags = listOf(AppStringsProvider.current().tagPassword, AppStringsProvider.current().tagToggle)
            )
        )
)
