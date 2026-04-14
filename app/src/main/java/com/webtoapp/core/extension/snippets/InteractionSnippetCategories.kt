package com.webtoapp.core.extension.snippets

import com.webtoapp.core.i18n.Strings

internal fun eventListeners() = CodeSnippetCategory(
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
        // Handle.
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
        // Handle.
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
        // Show by.
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
        // Handle large small.
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
    // Show single or single.
});""",
                tags = listOf(Strings.tagRightClick, Strings.tagMenu)
            ),
            CodeSnippet(
                id = "event-visibility",
                name = Strings.snippetVisibility,
                description = Strings.snippetVisibilityDesc,
                code = """document.addEventListener('visibilitychange', () => {
    if (document.hidden) {
        // Page to after.
        console.log('页面隐藏');
    } else {
        // Page to before.
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
    // Save.
    localStorage.setItem('lastVisit', Date.now());
    
    // use .
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
                tags = listOf(Strings.tagLongPress, Strings.tagTouch)
            )
        )
)
internal fun notifications() = CodeSnippetCategory(
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
internal fun scrollOperations() = CodeSnippetCategory(
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

// by.
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
    // in multiple.
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
internal fun formOperations() = CodeSnippetCategory(
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
        
        // can in.
        // after is.
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
        // change.
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
