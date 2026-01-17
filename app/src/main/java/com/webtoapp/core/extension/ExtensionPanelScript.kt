package com.webtoapp.core.extension

/**
 * 统一扩展模块面板脚本
 * 
 * 提供美观的统一 UI 面板，与应用主题风格一致
 * 采用毛玻璃效果、渐变色、圆角等现代设计元素
 */
object ExtensionPanelScript {
    
    /**
     * 获取面板初始化脚本
     * 应在页面加载时注入
     */
    fun getPanelInitScript(): String = """
(function() {
    'use strict';
    
    // 防止重复初始化
    if (window.__WTA_PANEL__) return;
    
    // ==================== 多语言支持 ====================
    const LANG = (navigator.language || 'zh').toLowerCase().startsWith('ar') ? 'ar' : 
                 (navigator.language || 'zh').toLowerCase().startsWith('zh') ? 'zh' : 'en';
    const I18N = {
        zh: {
            extensionModules: '扩展模块',
            noModulesAvailable: '暂无可用模块',
            panelInitialized: '扩展面板已初始化',
            unnamed: '未命名'
        },
        en: {
            extensionModules: 'Extension Modules',
            noModulesAvailable: 'No modules available',
            panelInitialized: 'Extension panel initialized',
            unnamed: 'Unnamed'
        },
        ar: {
            extensionModules: 'الوحدات الإضافية',
            noModulesAvailable: 'لا توجد وحدات متاحة',
            panelInitialized: 'تم تهيئة لوحة الإضافات',
            unnamed: 'بدون اسم'
        }
    };
    const T = I18N[LANG] || I18N.en;
    
    // ==================== 样式定义 ====================
    const PANEL_STYLES = `
        /* CSS 变量 - 主题色 */
        :root {
            --wta-primary: #7B68EE;
            --wta-primary-light: #9D8DF1;
            --wta-primary-dark: #5A4FCF;
            --wta-gradient: linear-gradient(135deg, #7B68EE 0%, #9D8DF1 50%, #B8A9F5 100%);
            --wta-gradient-dark: linear-gradient(135deg, #5A4FCF 0%, #7B68EE 100%);
            --wta-surface: rgba(255, 255, 255, 0.95);
            --wta-surface-dim: rgba(255, 255, 255, 0.85);
            --wta-on-surface: #1a1a2e;
            --wta-on-surface-variant: #6b7280;
            --wta-outline: rgba(123, 104, 238, 0.2);
            --wta-shadow: 0 8px 32px rgba(123, 104, 238, 0.15);
            --wta-shadow-lg: 0 16px 48px rgba(123, 104, 238, 0.25);
            --wta-radius: 20px;
            --wta-radius-sm: 12px;
            --wta-radius-lg: 28px;
        }
        
        @media (prefers-color-scheme: dark) {
            :root {
                --wta-surface: rgba(30, 30, 46, 0.95);
                --wta-surface-dim: rgba(30, 30, 46, 0.85);
                --wta-on-surface: #f3f4f6;
                --wta-on-surface-variant: #9ca3af;
                --wta-outline: rgba(123, 104, 238, 0.3);
                --wta-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
                --wta-shadow-lg: 0 16px 48px rgba(0, 0, 0, 0.5);
            }
        }

        /* 主容器 */
        #wta-ext-panel-container {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            z-index: 2147483646;
            pointer-events: none;
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
            -webkit-font-smoothing: antialiased;
        }
        
        /* 悬浮触发按钮 - 毛玻璃效果 */
        #wta-ext-fab {
            position: fixed;
            bottom: 80px;
            right: 16px;
            width: 56px;
            height: 56px;
            border-radius: 18px;
            background: var(--wta-gradient);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            cursor: pointer;
            z-index: 2147483647;
            box-shadow: var(--wta-shadow-lg), inset 0 1px 0 rgba(255,255,255,0.2);
            transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
            pointer-events: auto;
            -webkit-tap-highlight-color: transparent;
            user-select: none;
            border: 1px solid rgba(255,255,255,0.2);
            backdrop-filter: blur(10px);
            -webkit-backdrop-filter: blur(10px);
        }
        
        #wta-ext-fab:hover {
            transform: scale(1.08) translateY(-2px);
            box-shadow: var(--wta-shadow-lg), 0 0 30px rgba(123, 104, 238, 0.4);
        }
        
        #wta-ext-fab:active {
            transform: scale(0.95);
            transition-duration: 0.1s;
        }

        /* 模块数量徽章 */
        #wta-ext-fab .badge {
            position: absolute;
            top: -6px;
            right: -6px;
            min-width: 20px;
            height: 20px;
            border-radius: 10px;
            background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
            color: white;
            font-size: 11px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0 6px;
            box-shadow: 0 2px 8px rgba(238, 90, 90, 0.4);
            border: 2px solid var(--wta-surface);
        }
        
        /* 遮罩层 - 毛玻璃 */
        #wta-ext-overlay {
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.3);
            opacity: 0;
            visibility: hidden;
            transition: all 0.35s ease;
            z-index: 2147483644;
            pointer-events: auto;
            backdrop-filter: blur(8px);
            -webkit-backdrop-filter: blur(8px);
        }
        
        #wta-ext-overlay.visible {
            opacity: 1;
            visibility: visible;
        }

        /* 主面板 - 毛玻璃卡片 */
        #wta-ext-main-panel {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            max-height: 75vh;
            background: var(--wta-surface);
            border-radius: var(--wta-radius-lg) var(--wta-radius-lg) 0 0;
            transform: translateY(100%);
            transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
            z-index: 2147483645;
            pointer-events: auto;
            overflow: hidden;
            box-shadow: var(--wta-shadow-lg);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border: 1px solid var(--wta-outline);
            border-bottom: none;
        }
        
        #wta-ext-main-panel.visible {
            transform: translateY(0);
        }

        /* 面板拖动条 */
        .wta-panel-handle {
            width: 40px;
            height: 5px;
            background: linear-gradient(90deg, var(--wta-primary-light), var(--wta-primary));
            border-radius: 3px;
            margin: 14px auto 10px;
            opacity: 0.6;
        }
        
        /* 面板头部 */
        .wta-panel-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 8px 20px 18px;
            border-bottom: 1px solid var(--wta-outline);
        }
        
        .wta-panel-title {
            font-size: 20px;
            font-weight: 700;
            background: var(--wta-gradient-dark);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
            letter-spacing: -0.3px;
        }
        
        .wta-panel-close {
            width: 36px;
            height: 36px;
            border-radius: var(--wta-radius-sm);
            background: var(--wta-surface-dim);
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.25s ease;
            color: var(--wta-on-surface-variant);
            border: 1px solid var(--wta-outline);
        }
        
        .wta-panel-close:hover {
            background: var(--wta-primary);
            color: white;
            transform: rotate(90deg);
        }
        
        .wta-panel-close:active {
            transform: scale(0.9) rotate(90deg);
        }

        /* 模块列表 */
        .wta-module-list {
            padding: 20px;
            max-height: calc(75vh - 100px);
            overflow-y: auto;
            -webkit-overflow-scrolling: touch;
        }
        
        /* 自定义滚动条 */
        .wta-module-list::-webkit-scrollbar {
            width: 6px;
        }
        .wta-module-list::-webkit-scrollbar-track {
            background: transparent;
        }
        .wta-module-list::-webkit-scrollbar-thumb {
            background: var(--wta-primary-light);
            border-radius: 3px;
        }

        /* 模块网格 */
        .wta-module-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 16px;
        }
        
        @media (max-width: 400px) {
            .wta-module-grid {
                grid-template-columns: repeat(3, 1fr);
                gap: 12px;
            }
        }
        
        /* 模块项 - 卡片风格 */
        .wta-module-item {
            display: flex;
            flex-direction: column;
            align-items: center;
            padding: 16px 8px;
            border-radius: var(--wta-radius);
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            background: var(--wta-surface-dim);
            border: 1px solid transparent;
            position: relative;
            overflow: hidden;
        }
        
        .wta-module-item::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: var(--wta-gradient);
            opacity: 0;
            transition: opacity 0.3s ease;
            z-index: 0;
        }
        
        .wta-module-item:hover {
            transform: translateY(-4px) scale(1.02);
            border-color: var(--wta-primary-light);
            box-shadow: 0 8px 24px rgba(123, 104, 238, 0.2);
        }
        
        .wta-module-item:hover::before {
            opacity: 0.08;
        }
        
        .wta-module-item:active {
            transform: scale(0.95);
            transition-duration: 0.1s;
        }
        
        .wta-module-icon {
            width: 56px;
            height: 56px;
            border-radius: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 28px;
            margin-bottom: 10px;
            transition: all 0.3s ease;
            position: relative;
            z-index: 1;
            background: linear-gradient(135deg, rgba(123, 104, 238, 0.15) 0%, rgba(157, 141, 241, 0.1) 100%);
            box-shadow: inset 0 1px 0 rgba(255,255,255,0.5), 0 4px 12px rgba(123, 104, 238, 0.1);
        }
        
        .wta-module-item:hover .wta-module-icon {
            transform: scale(1.1) rotate(-3deg);
            box-shadow: 0 6px 20px rgba(123, 104, 238, 0.25);
        }
        
        .wta-module-name {
            font-size: 12px;
            font-weight: 600;
            color: var(--wta-on-surface);
            text-align: center;
            max-width: 100%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            position: relative;
            z-index: 1;
            letter-spacing: -0.2px;
        }

        /* 模块详情面板 */
        .wta-module-detail {
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: var(--wta-surface);
            transform: translateX(100%);
            transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
            overflow: hidden;
            display: flex;
            flex-direction: column;
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
        }
        
        .wta-module-detail.visible {
            transform: translateX(0);
        }
        
        .wta-detail-header {
            display: flex;
            align-items: center;
            padding: 18px 20px;
            border-bottom: 1px solid var(--wta-outline);
            gap: 14px;
        }
        
        .wta-detail-back {
            width: 40px;
            height: 40px;
            border-radius: var(--wta-radius-sm);
            background: var(--wta-surface-dim);
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            flex-shrink: 0;
            transition: all 0.25s ease;
            color: var(--wta-on-surface-variant);
            border: 1px solid var(--wta-outline);
        }
        
        .wta-detail-back:hover {
            background: var(--wta-primary);
            color: white;
            transform: translateX(-3px);
        }
        
        .wta-detail-title {
            flex: 1;
            font-size: 18px;
            font-weight: 700;
            color: var(--wta-on-surface);
            letter-spacing: -0.3px;
        }
        
        .wta-detail-content {
            flex: 1;
            overflow-y: auto;
            padding: 20px;
            -webkit-overflow-scrolling: touch;
        }

        /* Toast 提示 - 现代风格 */
        #wta-toast {
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%) scale(0.9);
            background: var(--wta-gradient-dark);
            color: white;
            padding: 16px 32px;
            border-radius: var(--wta-radius);
            font-size: 15px;
            font-weight: 600;
            z-index: 2147483647;
            opacity: 0;
            visibility: hidden;
            transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            pointer-events: none;
            max-width: 85%;
            text-align: center;
            box-shadow: var(--wta-shadow-lg);
            backdrop-filter: blur(10px);
            -webkit-backdrop-filter: blur(10px);
            letter-spacing: -0.2px;
        }
        
        #wta-toast.visible {
            opacity: 1;
            visibility: visible;
            transform: translate(-50%, -50%) scale(1);
        }
        
        /* 空状态 */
        .wta-empty-state {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 48px 24px;
            color: var(--wta-on-surface-variant);
        }
        
        .wta-empty-icon {
            font-size: 56px;
            margin-bottom: 16px;
            opacity: 0.6;
        }
        
        .wta-empty-text {
            font-size: 15px;
            text-align: center;
            font-weight: 500;
        }

        /* 按钮样式 */
        .wta-btn {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            padding: 14px 28px;
            border-radius: var(--wta-radius-sm);
            font-size: 15px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            border: none;
            outline: none;
            -webkit-tap-highlight-color: transparent;
            letter-spacing: -0.2px;
        }
        
        .wta-btn-primary {
            background: var(--wta-gradient);
            color: white;
            box-shadow: 0 4px 16px rgba(123, 104, 238, 0.3);
        }
        
        .wta-btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 24px rgba(123, 104, 238, 0.4);
        }
        
        .wta-btn-primary:active {
            transform: scale(0.97);
        }
        
        .wta-btn-secondary {
            background: var(--wta-surface-dim);
            color: var(--wta-on-surface);
            border: 1px solid var(--wta-outline);
        }
        
        .wta-btn-secondary:hover {
            background: var(--wta-primary);
            color: white;
            border-color: var(--wta-primary);
        }
        
        /* 输入框样式 */
        .wta-input {
            width: 100%;
            padding: 14px 18px;
            border-radius: var(--wta-radius-sm);
            border: 1px solid var(--wta-outline);
            background: var(--wta-surface-dim);
            color: var(--wta-on-surface);
            font-size: 15px;
            outline: none;
            transition: all 0.25s ease;
        }
        
        .wta-input:focus {
            border-color: var(--wta-primary);
            box-shadow: 0 0 0 3px rgba(123, 104, 238, 0.15);
        }
        
        .wta-input::placeholder {
            color: var(--wta-on-surface-variant);
        }
        
        /* 开关样式 */
        .wta-switch {
            position: relative;
            width: 52px;
            height: 28px;
            background: var(--wta-surface-dim);
            border-radius: 14px;
            cursor: pointer;
            transition: all 0.3s ease;
            border: 1px solid var(--wta-outline);
        }
        
        .wta-switch.active {
            background: var(--wta-gradient);
            border-color: transparent;
        }
        
        .wta-switch::after {
            content: '';
            position: absolute;
            top: 3px;
            left: 3px;
            width: 20px;
            height: 20px;
            background: white;
            border-radius: 50%;
            transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            box-shadow: 0 2px 6px rgba(0,0,0,0.15);
        }
        
        .wta-switch.active::after {
            transform: translateX(24px);
        }
    `;


    // ==================== 面板管理器 ====================
    const WTA_PANEL = {
        modules: [],
        isOpen: false,
        activeModuleId: null,
        
        // 初始化
        init() {
            this.injectStyles();
            this.createDOM();
            this.bindEvents();
            console.log('[WTA Panel] ' + T.panelInitialized);
        },
        
        // 注入样式
        injectStyles() {
            if (document.getElementById('wta-panel-styles')) return;
            const style = document.createElement('style');
            style.id = 'wta-panel-styles';
            style.textContent = PANEL_STYLES;
            document.head.appendChild(style);
        },
        
        // 创建 DOM 结构
        createDOM() {
            // 容器
            const container = document.createElement('div');
            container.id = 'wta-ext-panel-container';
            
            // FAB 按钮
            const fab = document.createElement('div');
            fab.id = 'wta-ext-fab';
            fab.innerHTML = '🧩<span class="badge" style="display:none">0</span>';
            
            // 遮罩
            const overlay = document.createElement('div');
            overlay.id = 'wta-ext-overlay';
            
            // 主面板
            const panel = document.createElement('div');
            panel.id = 'wta-ext-main-panel';
            panel.innerHTML = this.getPanelHTML();
            
            // Toast
            const toast = document.createElement('div');
            toast.id = 'wta-toast';
            
            container.appendChild(fab);
            container.appendChild(overlay);
            container.appendChild(panel);
            container.appendChild(toast);
            document.body.appendChild(container);
        },

        // 获取面板 HTML
        getPanelHTML() {
            return `
                <div class="wta-panel-handle"></div>
                <div class="wta-panel-header">
                    <span class="wta-panel-title">${'$'}{T.extensionModules}</span>
                    <div class="wta-panel-close" onclick="__WTA_PANEL__.hidePanel()">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                            <path d="M18 6L6 18M6 6l12 12"/>
                        </svg>
                    </div>
                </div>
                <div class="wta-module-list">
                    <div class="wta-module-grid" id="wta-module-grid"></div>
                </div>
                <div class="wta-module-detail" id="wta-module-detail">
                    <div class="wta-detail-header">
                        <div class="wta-detail-back" onclick="__WTA_PANEL__.hideModuleDetail()">
                            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                                <path d="M15 18l-6-6 6-6"/>
                            </svg>
                        </div>
                        <span class="wta-detail-title" id="wta-detail-title"></span>
                    </div>
                    <div class="wta-detail-content" id="wta-detail-content"></div>
                </div>
            `;
        },

        // 绑定事件
        bindEvents() {
            const fab = document.getElementById('wta-ext-fab');
            const overlay = document.getElementById('wta-ext-overlay');
            
            fab.addEventListener('click', () => this.togglePanel());
            overlay.addEventListener('click', () => this.hidePanel());
            
            // 触摸反馈
            fab.addEventListener('touchstart', () => {
                fab.style.transform = 'scale(0.92)';
            }, { passive: true });
            fab.addEventListener('touchend', () => {
                fab.style.transform = '';
            }, { passive: true });
        },

        // 注册模块
        registerModule(moduleInfo) {
            const existing = this.modules.findIndex(m => m.id === moduleInfo.id);
            if (existing >= 0) {
                this.modules[existing] = { ...this.modules[existing], ...moduleInfo };
            } else {
                this.modules.push(moduleInfo);
            }
            this.updateModules();
            this.updateBadge();
        },
        
        // 更新模块列表
        updateModules() {
            const grid = document.getElementById('wta-module-grid');
            if (!grid) return;
            
            if (this.modules.length === 0) {
                grid.innerHTML = `
                    <div class="wta-empty-state" style="grid-column: 1/-1">
                        <div class="wta-empty-icon">📦</div>
                        <div class="wta-empty-text">${'$'}{T.noModulesAvailable}</div>
                    </div>
                `;
                return;
            }
            
            grid.innerHTML = this.modules.map(m => `
                <div class="wta-module-item" onclick="__WTA_PANEL__.onModuleClick('${"$"}{m.id}')">
                    <div class="wta-module-icon">
                        ${"$"}{m.icon || '📦'}
                    </div>
                    <div class="wta-module-name">${"$"}{m.name || T.unnamed}</div>
                </div>
            `).join('');
        },
        
        // 更新徽章
        updateBadge() {
            const badge = document.querySelector('#wta-ext-fab .badge');
            if (badge) {
                const count = this.modules.length;
                badge.textContent = count;
                badge.style.display = count > 0 ? 'flex' : 'none';
            }
        },

        // 切换面板
        togglePanel() {
            if (this.isOpen) {
                this.hidePanel();
            } else {
                this.showPanel();
            }
        },
        
        // 显示面板
        showPanel() {
            const panel = document.getElementById('wta-ext-main-panel');
            const overlay = document.getElementById('wta-ext-overlay');
            const fab = document.getElementById('wta-ext-fab');
            
            panel.classList.add('visible');
            overlay.classList.add('visible');
            fab.style.display = 'none';
            this.isOpen = true;
            
            // 隐藏模块详情
            this.hideModuleDetail();
        },
        
        // 隐藏面板
        hidePanel() {
            const panel = document.getElementById('wta-ext-main-panel');
            const overlay = document.getElementById('wta-ext-overlay');
            const fab = document.getElementById('wta-ext-fab');
            
            panel.classList.remove('visible');
            overlay.classList.remove('visible');
            fab.style.display = 'flex';
            this.isOpen = false;
            this.activeModuleId = null;
            
            // 隐藏模块详情
            this.hideModuleDetail();
        },

        // 模块点击
        onModuleClick(moduleId) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            // 如果模块有面板内容，显示详情
            if (module.panelHtml || module.onAction) {
                this.showModulePanel(moduleId);
            } else if (module.onClick) {
                // 执行点击回调
                module.onClick();
                this.hidePanel();
            }
        },

        // 显示模块详情面板
        showModulePanel(moduleId) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            this.activeModuleId = moduleId;
            
            const detail = document.getElementById('wta-module-detail');
            const title = document.getElementById('wta-detail-title');
            const content = document.getElementById('wta-detail-content');
            
            title.textContent = module.name || '模块详情';
            
            // 设置面板内容
            if (module.panelHtml) {
                content.innerHTML = module.panelHtml;
            } else if (module.onAction) {
                content.innerHTML = '<div style="text-align:center;padding:20px;color:var(--wta-on-surface-variant)">加载中...</div>';
                module.onAction(content);
            } else {
                content.innerHTML = '<div class="wta-empty-state"><div class="wta-empty-text">此模块无详情面板</div></div>';
            }
            
            detail.classList.add('visible');
        },
        
        // 隐藏模块详情
        hideModuleDetail() {
            const detail = document.getElementById('wta-module-detail');
            if (detail) {
                detail.classList.remove('visible');
            }
            this.activeModuleId = null;
        },
        
        // 更新模块面板内容
        updateModulePanelContent(moduleId, html) {
            if (this.activeModuleId !== moduleId) return;
            const content = document.getElementById('wta-detail-content');
            if (content) {
                content.innerHTML = html;
            }
        },

        // 显示 Toast
        showToast(message, duration = 2000) {
            const toast = document.getElementById('wta-toast');
            if (!toast) return;
            
            toast.textContent = message;
            toast.classList.add('visible');
            
            clearTimeout(this._toastTimer);
            this._toastTimer = setTimeout(() => {
                toast.classList.remove('visible');
            }, duration);
        },
        
        // 设置 FAB 可见性
        setFabVisible(visible) {
            const fab = document.getElementById('wta-ext-fab');
            if (fab) {
                fab.style.display = visible ? 'flex' : 'none';
            }
        },
        
        // 设置 FAB 位置
        setFabPosition(bottom, right) {
            const fab = document.getElementById('wta-ext-fab');
            if (fab) {
                fab.style.bottom = bottom + 'px';
                fab.style.right = right + 'px';
            }
        }
    };
    
    // 暴露全局接口
    window.__WTA_PANEL__ = WTA_PANEL;
    
    // 初始化
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => WTA_PANEL.init());
    } else {
        WTA_PANEL.init();
    }
})();
""".trimIndent()



    /**
     * 获取模块注册辅助脚本
     * 模块可以使用这些辅助函数来注册自己的 UI
     */
    fun getModuleHelperScript(): String = """
(function() {
    'use strict';
    
    // 等待面板初始化
    function waitForPanel(callback, maxWait = 5000) {
        const start = Date.now();
        const check = () => {
            if (window.__WTA_PANEL__) {
                callback(window.__WTA_PANEL__);
            } else if (Date.now() - start < maxWait) {
                setTimeout(check, 50);
            }
        };
        check();
    }
    
    // 模块 UI 辅助对象
    window.__WTA_MODULE_UI__ = {
        /**
         * 注册模块到统一面板
         * @param {Object} config 模块配置
         * @param {string} config.id 模块ID
         * @param {string} config.name 模块名称
         * @param {string} config.icon 模块图标（emoji）
         * @param {string} config.color 主题色（十六进制，如 #667eea）- 已弃用，使用统一主题
         * @param {string} config.panelHtml 面板HTML内容（可选）
         * @param {Function} config.onClick 点击回调（可选，无面板时使用）
         * @param {Function} config.onAction 动态生成面板内容的回调（可选）
         */
        register(config) {
            waitForPanel(panel => {
                panel.registerModule(config);
            });
        },
        
        /**
         * 更新模块面板内容
         */
        updatePanel(moduleId, html) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateModulePanelContent(moduleId, html);
            }
        },
        
        /**
         * 显示 Toast 提示
         */
        toast(message, duration = 2000) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.showToast(message, duration);
            }
        },
        
        /**
         * 关闭面板
         */
        closePanel() {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.hidePanel();
            }
        },
        
        /**
         * 返回模块列表
         */
        back() {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.hideModuleDetail();
            }
        }
    };
})();
""".trimIndent()
}
