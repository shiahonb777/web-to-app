package com.webtoapp.core.extension

/**
 * 统一扩展模块面板脚本
 * 
 * Provide unified UI panel，与应用主题风格一致
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
            touch-action: none;
        }
        
        #wta-ext-fab:not(.dragging):hover {
            transform: scale(1.08) translateY(-2px);
            box-shadow: var(--wta-shadow-lg), 0 0 30px rgba(123, 104, 238, 0.4);
        }
        
        #wta-ext-fab:active:not(.dragging) {
            transform: scale(0.95);
            transition-duration: 0.1s;
        }
        
        #wta-ext-fab.dragging {
            opacity: 0.85;
            cursor: grabbing;
            transition: none;
        }
        
        #wta-ext-fab.hidden {
            display: none !important;
        }
        
        /* 显示按钮 - 当FAB隐藏时显示 */
        #wta-ext-show-btn {
            position: fixed;
            bottom: 50%;
            right: 0;
            width: 24px;
            height: 48px;
            background: var(--wta-gradient);
            border-radius: 12px 0 0 12px;
            display: none;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 14px;
            cursor: pointer;
            z-index: 2147483647;
            pointer-events: auto;
            box-shadow: -2px 0 12px rgba(123, 104, 238, 0.3);
            transition: all 0.3s ease;
            opacity: 0.7;
        }
        
        #wta-ext-show-btn:hover {
            width: 32px;
            opacity: 1;
        }
        
        #wta-ext-show-btn.visible {
            display: flex;
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
            z-index: 10;
        }
        
        .wta-module-detail:not(.visible) {
            pointer-events: none;
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
        
        /* ==================== 多UI类型支持 ==================== */
        
        /* 悬浮工具栏 - FLOATING_TOOLBAR */
        .wta-toolbar {
            position: fixed;
            display: flex;
            align-items: center;
            gap: 6px;
            padding: 8px;
            background: var(--wta-surface);
            border-radius: var(--wta-radius);
            box-shadow: var(--wta-shadow-lg);
            z-index: 2147483646;
            pointer-events: auto;
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border: 1px solid var(--wta-outline);
            transition: all 0.3s ease;
        }
        
        .wta-toolbar.vertical {
            flex-direction: column;
        }
        
        .wta-toolbar.horizontal {
            flex-direction: row;
        }
        
        .wta-toolbar.collapsed {
            padding: 4px;
        }
        
        .wta-toolbar.collapsed .wta-toolbar-item-label,
        .wta-toolbar.collapsed .wta-toolbar-item-badge {
            display: none;
        }
        
        .wta-toolbar-toggle {
            width: 32px;
            height: 32px;
            border-radius: 10px;
            background: var(--wta-gradient);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            font-size: 14px;
            transition: all 0.25s ease;
            flex-shrink: 0;
        }
        
        .wta-toolbar-toggle:hover {
            transform: scale(1.1);
        }
        
        .wta-toolbar-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 10px 14px;
            border-radius: 12px;
            cursor: pointer;
            transition: all 0.25s ease;
            position: relative;
            background: transparent;
            color: var(--wta-on-surface);
        }
        
        .wta-toolbar.vertical .wta-toolbar-item {
            width: 100%;
            justify-content: flex-start;
        }
        
        .wta-toolbar-item:hover {
            background: rgba(123, 104, 238, 0.12);
        }
        
        .wta-toolbar-item:active {
            transform: scale(0.95);
        }
        
        .wta-toolbar-item-icon {
            width: 28px;
            height: 28px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 18px;
            flex-shrink: 0;
        }
        
        .wta-toolbar-item-label {
            font-size: 13px;
            font-weight: 600;
            white-space: nowrap;
        }
        
        .wta-toolbar-item-badge {
            position: absolute;
            top: 4px;
            right: 4px;
            min-width: 16px;
            height: 16px;
            border-radius: 8px;
            background: #ff6b6b;
            color: white;
            font-size: 10px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0 4px;
        }
        
        /* 侧边栏 - SIDEBAR */
        .wta-sidebar {
            position: fixed;
            top: 0;
            bottom: 0;
            width: 280px;
            max-width: 85vw;
            background: var(--wta-surface);
            z-index: 2147483646;
            pointer-events: auto;
            transform: translateX(-100%);
            transition: transform 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            box-shadow: var(--wta-shadow-lg);
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }
        
        .wta-sidebar.right {
            left: auto;
            right: 0;
            transform: translateX(100%);
        }
        
        .wta-sidebar.left {
            left: 0;
            right: auto;
        }
        
        .wta-sidebar.visible {
            transform: translateX(0);
        }
        
        .wta-sidebar-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 20px;
            border-bottom: 1px solid var(--wta-outline);
        }
        
        .wta-sidebar-title {
            font-size: 18px;
            font-weight: 700;
            background: var(--wta-gradient-dark);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        
        .wta-sidebar-close {
            width: 36px;
            height: 36px;
            border-radius: 10px;
            background: var(--wta-surface-dim);
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.25s ease;
            color: var(--wta-on-surface-variant);
        }
        
        .wta-sidebar-close:hover {
            background: var(--wta-primary);
            color: white;
        }
        
        .wta-sidebar-content {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
            -webkit-overflow-scrolling: touch;
        }
        
        .wta-sidebar-trigger {
            position: fixed;
            top: 50%;
            transform: translateY(-50%);
            width: 24px;
            height: 80px;
            background: var(--wta-gradient);
            z-index: 2147483645;
            pointer-events: auto;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 12px;
            transition: all 0.25s ease;
        }
        
        .wta-sidebar-trigger.left {
            left: 0;
            border-radius: 0 12px 12px 0;
        }
        
        .wta-sidebar-trigger.right {
            right: 0;
            border-radius: 12px 0 0 12px;
        }
        
        .wta-sidebar-trigger:hover {
            width: 32px;
        }
        
        /* 底部栏 - BOTTOM_BAR */
        .wta-bottom-bar {
            position: fixed;
            bottom: 0;
            left: 0;
            right: 0;
            height: 64px;
            background: var(--wta-surface);
            z-index: 2147483646;
            pointer-events: auto;
            display: flex;
            align-items: center;
            justify-content: space-around;
            padding: 0 8px;
            padding-bottom: env(safe-area-inset-bottom, 0);
            box-shadow: 0 -4px 24px rgba(0,0,0,0.1);
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border-top: 1px solid var(--wta-outline);
            transition: transform 0.3s ease;
        }
        
        .wta-bottom-bar.hidden {
            transform: translateY(100%);
        }
        
        .wta-bottom-bar-item {
            flex: 1;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 8px 4px;
            cursor: pointer;
            transition: all 0.25s ease;
            position: relative;
            color: var(--wta-on-surface-variant);
            max-width: 96px;
        }
        
        .wta-bottom-bar-item:active {
            transform: scale(0.9);
        }
        
        .wta-bottom-bar-item.active {
            color: var(--wta-primary);
        }
        
        .wta-bottom-bar-item-icon {
            width: 28px;
            height: 28px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            margin-bottom: 2px;
        }
        
        .wta-bottom-bar-item-label {
            font-size: 11px;
            font-weight: 600;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 100%;
        }
        
        .wta-bottom-bar-item-badge {
            position: absolute;
            top: 2px;
            right: calc(50% - 20px);
            min-width: 16px;
            height: 16px;
            border-radius: 8px;
            background: #ff6b6b;
            color: white;
            font-size: 10px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0 4px;
        }
        
        /* 悬浮面板 - FLOATING_PANEL */
        .wta-floating-panel {
            position: fixed;
            background: var(--wta-surface);
            border-radius: var(--wta-radius);
            box-shadow: var(--wta-shadow-lg);
            z-index: 2147483646;
            pointer-events: auto;
            backdrop-filter: blur(20px);
            -webkit-backdrop-filter: blur(20px);
            border: 1px solid var(--wta-outline);
            overflow: hidden;
            min-width: 200px;
            max-width: 90vw;
            max-height: 80vh;
            display: flex;
            flex-direction: column;
            transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            opacity: 0;
            visibility: hidden;
            transform: scale(0.9);
        }
        
        .wta-floating-panel.visible {
            opacity: 1;
            visibility: visible;
            transform: scale(1);
        }
        
        .wta-floating-panel-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 14px 16px;
            border-bottom: 1px solid var(--wta-outline);
            cursor: move;
            user-select: none;
        }
        
        .wta-floating-panel-title {
            font-size: 15px;
            font-weight: 700;
            color: var(--wta-on-surface);
        }
        
        .wta-floating-panel-actions {
            display: flex;
            gap: 6px;
        }
        
        .wta-floating-panel-btn {
            width: 28px;
            height: 28px;
            border-radius: 8px;
            background: var(--wta-surface-dim);
            display: flex;
            align-items: center;
            justify-content: center;
            cursor: pointer;
            transition: all 0.2s ease;
            color: var(--wta-on-surface-variant);
            font-size: 12px;
        }
        
        .wta-floating-panel-btn:hover {
            background: var(--wta-primary);
            color: white;
        }
        
        .wta-floating-panel-content {
            flex: 1;
            overflow-y: auto;
            padding: 16px;
            -webkit-overflow-scrolling: touch;
        }
        
        .wta-floating-panel-resize {
            position: absolute;
            bottom: 0;
            right: 0;
            width: 16px;
            height: 16px;
            cursor: se-resize;
            background: linear-gradient(-45deg, var(--wta-primary) 30%, transparent 30%);
            opacity: 0.5;
        }
        
        /* 迷你按钮 - MINI_BUTTON */
        .wta-mini-btn {
            position: fixed;
            width: 44px;
            height: 44px;
            border-radius: 14px;
            background: var(--wta-gradient);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            cursor: pointer;
            z-index: 2147483646;
            box-shadow: var(--wta-shadow);
            transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
            pointer-events: auto;
            -webkit-tap-highlight-color: transparent;
            user-select: none;
            border: 1px solid rgba(255,255,255,0.2);
            backdrop-filter: blur(10px);
            -webkit-backdrop-filter: blur(10px);
        }
        
        .wta-mini-btn:hover {
            transform: scale(1.1);
            box-shadow: var(--wta-shadow-lg);
        }
        
        .wta-mini-btn:active {
            transform: scale(0.92);
        }
        
        .wta-mini-btn .badge {
            position: absolute;
            top: -4px;
            right: -4px;
            min-width: 16px;
            height: 16px;
            border-radius: 8px;
            background: #ff6b6b;
            color: white;
            font-size: 10px;
            font-weight: 700;
            display: flex;
            align-items: center;
            justify-content: center;
            padding: 0 4px;
        }
        
        .wta-mini-btn-label {
            position: absolute;
            top: calc(100% + 6px);
            left: 50%;
            transform: translateX(-50%);
            background: var(--wta-surface);
            color: var(--wta-on-surface);
            padding: 6px 12px;
            border-radius: 8px;
            font-size: 12px;
            font-weight: 600;
            white-space: nowrap;
            box-shadow: var(--wta-shadow);
            opacity: 0;
            visibility: hidden;
            transition: all 0.2s ease;
            pointer-events: none;
        }
        
        .wta-mini-btn:hover .wta-mini-btn-label {
            opacity: 1;
            visibility: visible;
        }
        
        /* 自定义UI容器 - CUSTOM */
        .wta-custom-container {
            position: fixed;
            z-index: 2147483640;
            pointer-events: auto;
        }
        
        /* 位置工具类 */
        .wta-pos-top-left { top: 16px; left: 16px; }
        .wta-pos-top-center { top: 16px; left: 50%; transform: translateX(-50%); }
        .wta-pos-top-right { top: 16px; right: 16px; }
        .wta-pos-center-left { top: 50%; left: 16px; transform: translateY(-50%); }
        .wta-pos-center { top: 50%; left: 50%; transform: translate(-50%, -50%); }
        .wta-pos-center-right { top: 50%; right: 16px; transform: translateY(-50%); }
        .wta-pos-bottom-left { bottom: 80px; left: 16px; }
        .wta-pos-bottom-center { bottom: 80px; left: 50%; transform: translateX(-50%); }
        .wta-pos-bottom-right { bottom: 80px; right: 16px; }
        
        /* 可拖动元素 */
        .wta-draggable {
            touch-action: none;
        }
        
        .wta-draggable.dragging {
            opacity: 0.9;
            cursor: grabbing;
        }
    `;


    // ==================== UI 类型常量 ====================
    const UI_TYPE = {
        FLOATING_BUTTON: 'FLOATING_BUTTON',
        FLOATING_TOOLBAR: 'FLOATING_TOOLBAR',
        SIDEBAR: 'SIDEBAR',
        BOTTOM_BAR: 'BOTTOM_BAR',
        FLOATING_PANEL: 'FLOATING_PANEL',
        MINI_BUTTON: 'MINI_BUTTON',
        CUSTOM: 'CUSTOM'
    };
    
    const UI_POSITION = {
        TOP_LEFT: 'TOP_LEFT',
        TOP_CENTER: 'TOP_CENTER',
        TOP_RIGHT: 'TOP_RIGHT',
        CENTER_LEFT: 'CENTER_LEFT',
        CENTER: 'CENTER',
        CENTER_RIGHT: 'CENTER_RIGHT',
        BOTTOM_LEFT: 'BOTTOM_LEFT',
        BOTTOM_CENTER: 'BOTTOM_CENTER',
        BOTTOM_RIGHT: 'BOTTOM_RIGHT'
    };
    
    // 位置到CSS类的映射
    const positionClassMap = {
        TOP_LEFT: 'wta-pos-top-left',
        TOP_CENTER: 'wta-pos-top-center',
        TOP_RIGHT: 'wta-pos-top-right',
        CENTER_LEFT: 'wta-pos-center-left',
        CENTER: 'wta-pos-center',
        CENTER_RIGHT: 'wta-pos-center-right',
        BOTTOM_LEFT: 'wta-pos-bottom-left',
        BOTTOM_CENTER: 'wta-pos-bottom-center',
        BOTTOM_RIGHT: 'wta-pos-bottom-right'
    };

    // ==================== 拖动管理器 ====================
    const DragManager = {
        makeDraggable(element, handle = null) {
            const dragHandle = handle || element;
            let isDragging = false;
            let startX, startY, startLeft, startTop;
            
            const onStart = (e) => {
                if (e.target.closest('.wta-floating-panel-btn')) return;
                isDragging = true;
                element.classList.add('dragging');
                const touch = e.touches ? e.touches[0] : e;
                startX = touch.clientX;
                startY = touch.clientY;
                const rect = element.getBoundingClientRect();
                startLeft = rect.left;
                startTop = rect.top;
                e.preventDefault();
            };
            
            const onMove = (e) => {
                if (!isDragging) return;
                const touch = e.touches ? e.touches[0] : e;
                const dx = touch.clientX - startX;
                const dy = touch.clientY - startY;
                element.style.left = (startLeft + dx) + 'px';
                element.style.top = (startTop + dy) + 'px';
                element.style.right = 'auto';
                element.style.bottom = 'auto';
                element.style.transform = 'none';
            };
            
            const onEnd = () => {
                isDragging = false;
                element.classList.remove('dragging');
            };
            
            dragHandle.addEventListener('mousedown', onStart);
            dragHandle.addEventListener('touchstart', onStart, { passive: false });
            document.addEventListener('mousemove', onMove);
            document.addEventListener('touchmove', onMove, { passive: false });
            document.addEventListener('mouseup', onEnd);
            document.addEventListener('touchend', onEnd);
            
            element.classList.add('wta-draggable');
        }
    };

    // ==================== 面板管理器 ====================
    const WTA_PANEL = {
        modules: [],
        uiContainers: {}, // Storage各模块的UI容器
        isOpen: false,
        activeModuleId: null,
        
        // Initialize
        init() {
            this.injectStyles();
            this.createDOM();
            this.bindEvents();
            console.log('[WTA Panel] ' + T.panelInitialized);
        },
        
        // Inject样式
        injectStyles() {
            if (document.getElementById('wta-panel-styles')) return;
            const style = document.createElement('style');
            style.id = 'wta-panel-styles';
            style.textContent = PANEL_STYLES;
            document.head.appendChild(style);
        },
        
        // Create DOM 结构
        createDOM() {
            // 容器
            const container = document.createElement('div');
            container.id = 'wta-ext-panel-container';
            
            // FAB 按钮
            const fab = document.createElement('div');
            fab.id = 'wta-ext-fab';
            fab.innerHTML = '🧩<span class="badge" style="display:none">0</span>';
            
            // 显示按钮（当FAB隐藏时）
            const showBtn = document.createElement('div');
            showBtn.id = 'wta-ext-show-btn';
            showBtn.innerHTML = '❮';
            showBtn.title = '显示扩展模块';
            
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
            container.appendChild(showBtn);
            container.appendChild(overlay);
            container.appendChild(panel);
            container.appendChild(toast);
            document.body.appendChild(container);
            
            // 恢复保存的位置和隐藏状态
            this.restoreFabState();
        },

        // Get面板 HTML
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
            const detail = document.getElementById('wta-module-detail');
            const showBtn = document.getElementById('wta-ext-show-btn');
            
            // FAB拖动功能
            this.initFabDrag(fab);
            
            // 显示按钮点击
            showBtn.addEventListener('click', () => this.showFab());
            
            overlay.addEventListener('click', () => this.hidePanel());
            
            // 详情面板点击事件拦截，防止事件穿透到下层模块列表
            if (detail) {
                detail.addEventListener('click', (e) => {
                    e.stopPropagation();
                });
            }
        },
        
        // 初始化FAB拖动功能
        initFabDrag(fab) {
            let isDragging = false;
            let hasMoved = false;
            let startX, startY, startLeft, startTop;
            let longPressTimer = null;
            
            const onStart = (e) => {
                const touch = e.touches ? e.touches[0] : e;
                startX = touch.clientX;
                startY = touch.clientY;
                const rect = fab.getBoundingClientRect();
                startLeft = rect.left;
                startTop = rect.top;
                hasMoved = false;
                
                // 长按隐藏
                longPressTimer = setTimeout(() => {
                    if (!hasMoved) {
                        this.hideFab();
                    }
                }, 800);
            };
            
            const onMove = (e) => {
                const touch = e.touches ? e.touches[0] : e;
                const dx = touch.clientX - startX;
                const dy = touch.clientY - startY;
                
                // 移动超过5px则认为开始拖动
                if (Math.abs(dx) > 5 || Math.abs(dy) > 5) {
                    hasMoved = true;
                    isDragging = true;
                    fab.classList.add('dragging');
                    clearTimeout(longPressTimer);
                    
                    let newLeft = startLeft + dx;
                    let newTop = startTop + dy;
                    
                    // 边界限制
                    const maxX = window.innerWidth - fab.offsetWidth;
                    const maxY = window.innerHeight - fab.offsetHeight;
                    newLeft = Math.max(0, Math.min(newLeft, maxX));
                    newTop = Math.max(0, Math.min(newTop, maxY));
                    
                    fab.style.left = newLeft + 'px';
                    fab.style.top = newTop + 'px';
                    fab.style.right = 'auto';
                    fab.style.bottom = 'auto';
                    
                    e.preventDefault();
                }
            };
            
            const onEnd = () => {
                clearTimeout(longPressTimer);
                if (isDragging) {
                    isDragging = false;
                    fab.classList.remove('dragging');
                    // 保存位置
                    this.saveFabState();
                } else if (!hasMoved) {
                    // 单击打开面板
                    this.togglePanel();
                }
            };
            
            fab.addEventListener('mousedown', onStart);
            fab.addEventListener('touchstart', onStart, { passive: true });
            document.addEventListener('mousemove', onMove);
            document.addEventListener('touchmove', onMove, { passive: false });
            document.addEventListener('mouseup', onEnd);
            document.addEventListener('touchend', onEnd);
        },
        
        // 隐藏FAB
        hideFab() {
            const fab = document.getElementById('wta-ext-fab');
            const showBtn = document.getElementById('wta-ext-show-btn');
            if (fab && showBtn) {
                fab.classList.add('hidden');
                showBtn.classList.add('visible');
                this.showToast(LANG === 'zh' ? '扩展模块已隐藏，点击右侧按钮显示' : 
                              LANG === 'ar' ? 'تم إخفاء الوحدة، انقر للعرض' : 
                              'Module hidden, click right edge to show');
                this.saveFabState();
            }
        },
        
        // 显示FAB
        showFab() {
            const fab = document.getElementById('wta-ext-fab');
            const showBtn = document.getElementById('wta-ext-show-btn');
            if (fab && showBtn) {
                fab.classList.remove('hidden');
                showBtn.classList.remove('visible');
                this.saveFabState();
            }
        },
        
        // 保存FAB状态
        saveFabState() {
            const fab = document.getElementById('wta-ext-fab');
            if (!fab) return;
            try {
                const state = {
                    hidden: fab.classList.contains('hidden'),
                    left: fab.style.left,
                    top: fab.style.top,
                    right: fab.style.right,
                    bottom: fab.style.bottom
                };
                localStorage.setItem('wta_fab_state', JSON.stringify(state));
            } catch (e) {}
        },
        
        // 恢复FAB状态
        restoreFabState() {
            try {
                const saved = localStorage.getItem('wta_fab_state');
                if (!saved) return;
                const state = JSON.parse(saved);
                const fab = document.getElementById('wta-ext-fab');
                const showBtn = document.getElementById('wta-ext-show-btn');
                if (!fab || !showBtn) return;
                
                if (state.hidden) {
                    fab.classList.add('hidden');
                    showBtn.classList.add('visible');
                }
                
                if (state.left && state.left !== 'auto') {
                    fab.style.left = state.left;
                    fab.style.right = 'auto';
                }
                if (state.top && state.top !== 'auto') {
                    fab.style.top = state.top;
                    fab.style.bottom = 'auto';
                }
            } catch (e) {}
        },

        // 注册模块 - 支持多种UI类型
        registerModule(moduleInfo) {
            const existing = this.modules.findIndex(m => m.id === moduleInfo.id);
            if (existing >= 0) {
                // Update现有模块
                this.removeModuleUI(this.modules[existing].id);
                this.modules[existing] = { ...this.modules[existing], ...moduleInfo };
            } else {
                this.modules.push(moduleInfo);
            }
            
            // 根据UI类型创建UI
            const uiType = moduleInfo.uiConfig?.type || UI_TYPE.FLOATING_BUTTON;
            
            switch (uiType) {
                case UI_TYPE.FLOATING_TOOLBAR:
                    this.createToolbar(moduleInfo);
                    break;
                case UI_TYPE.SIDEBAR:
                    this.createSidebar(moduleInfo);
                    break;
                case UI_TYPE.BOTTOM_BAR:
                    this.createBottomBar(moduleInfo);
                    break;
                case UI_TYPE.FLOATING_PANEL:
                    this.createFloatingPanel(moduleInfo);
                    break;
                case UI_TYPE.MINI_BUTTON:
                    this.createMiniButton(moduleInfo);
                    break;
                case UI_TYPE.CUSTOM:
                    this.createCustomUI(moduleInfo);
                    break;
                case UI_TYPE.FLOATING_BUTTON:
                default:
                    // Default行为：添加到统一面板
                    this.updateModules();
                    this.updateBadge();
                    break;
            }
        },
        
        // 移除模块UI
        removeModuleUI(moduleId) {
            const container = this.uiContainers[moduleId];
            if (container) {
                if (Array.isArray(container)) {
                    container.forEach(el => el.remove());
                } else {
                    container.remove();
                }
                delete this.uiContainers[moduleId];
            }
        },
        
        // ==================== 创建悬浮工具栏 ====================
        createToolbar(moduleInfo) {
            const config = moduleInfo.uiConfig || {};
            const position = config.position || UI_POSITION.BOTTOM_RIGHT;
            const orientation = config.toolbarOrientation || 'HORIZONTAL';
            const items = config.toolbarItems || [];
            const draggable = config.draggable !== false;
            const collapsible = config.collapsible !== false;
            
            const toolbar = document.createElement('div');
            toolbar.id = `wta-toolbar-${"$"}{moduleInfo.id}`;
            toolbar.className = `wta-toolbar ${"$"}{orientation.toLowerCase()} ${"$"}{positionClassMap[position] || ''}`;
            
            let html = '';
            
            // 如果可折叠，添加切换按钮
            if (collapsible) {
                html += `<div class="wta-toolbar-toggle" onclick="__WTA_PANEL__.toggleToolbarCollapse('${"$"}{moduleInfo.id}')">☰</div>`;
            }
            
            // 添加工具栏项
            items.forEach((item, idx) => {
                html += `
                    <div class="wta-toolbar-item" onclick="__WTA_PANEL__.onToolbarItemClick('${"$"}{moduleInfo.id}', ${"$"}{idx})" title="${"$"}{item.tooltip || ''}">
                        <div class="wta-toolbar-item-icon">${"$"}{item.icon || '⚙️'}</div>
                        ${"$"}{item.showLabel !== false ? `<span class="wta-toolbar-item-label">${"$"}{item.label || ''}</span>` : ''}
                        ${"$"}{item.badge ? `<span class="wta-toolbar-item-badge">${"$"}{item.badge}</span>` : ''}
                    </div>
                `;
            });
            
            toolbar.innerHTML = html;
            document.body.appendChild(toolbar);
            
            if (draggable) {
                DragManager.makeDraggable(toolbar);
            }
            
            this.uiContainers[moduleInfo.id] = toolbar;
        },
        
        toggleToolbarCollapse(moduleId) {
            const toolbar = document.getElementById(`wta-toolbar-${"$"}{moduleId}`);
            if (toolbar) {
                toolbar.classList.toggle('collapsed');
            }
        },
        
        onToolbarItemClick(moduleId, itemIndex) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            const items = module.uiConfig?.toolbarItems || [];
            const item = items[itemIndex];
            if (item && item.action) {
                // 尝试调用action函数
                try {
                    if (typeof item.action === 'function') {
                        item.action();
                    } else if (typeof item.action === 'string') {
                        eval(item.action);
                    }
                } catch (e) {
                    console.error('[WTA] Toolbar item action error:', e);
                }
            }
            // 通知模块
            if (module.onToolbarClick) {
                module.onToolbarClick(itemIndex, item);
            }
        },
        
        // ==================== 创建侧边栏 ====================
        createSidebar(moduleInfo) {
            const config = moduleInfo.uiConfig || {};
            const sidebarPosition = config.sidebarPosition || 'LEFT';
            const width = config.sidebarWidth || 280;
            
            // Create触发条
            const trigger = document.createElement('div');
            trigger.id = `wta-sidebar-trigger-${"$"}{moduleInfo.id}`;
            trigger.className = `wta-sidebar-trigger ${"$"}{sidebarPosition.toLowerCase()}`;
            trigger.innerHTML = sidebarPosition === 'LEFT' ? '❯' : '❮';
            trigger.onclick = () => this.toggleSidebar(moduleInfo.id);
            
            // Create侧边栏
            const sidebar = document.createElement('div');
            sidebar.id = `wta-sidebar-${"$"}{moduleInfo.id}`;
            sidebar.className = `wta-sidebar ${"$"}{sidebarPosition.toLowerCase()}`;
            sidebar.style.width = width + 'px';
            
            sidebar.innerHTML = `
                <div class="wta-sidebar-header">
                    <span class="wta-sidebar-title">${"$"}{moduleInfo.name || T.unnamed}</span>
                    <div class="wta-sidebar-close" onclick="__WTA_PANEL__.hideSidebar('${"$"}{moduleInfo.id}')">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                            <path d="M18 6L6 18M6 6l12 12"/>
                        </svg>
                    </div>
                </div>
                <div class="wta-sidebar-content" id="wta-sidebar-content-${"$"}{moduleInfo.id}">
                    ${"$"}{moduleInfo.panelHtml || ''}
                </div>
            `;
            
            document.body.appendChild(trigger);
            document.body.appendChild(sidebar);
            
            this.uiContainers[moduleInfo.id] = [trigger, sidebar];
            
            // 如果有onAction回调，执行它
            if (moduleInfo.onAction) {
                const content = document.getElementById(`wta-sidebar-content-${"$"}{moduleInfo.id}`);
                moduleInfo.onAction(content);
            }
        },
        
        toggleSidebar(moduleId) {
            const sidebar = document.getElementById(`wta-sidebar-${"$"}{moduleId}`);
            if (sidebar) {
                sidebar.classList.toggle('visible');
            }
        },
        
        hideSidebar(moduleId) {
            const sidebar = document.getElementById(`wta-sidebar-${"$"}{moduleId}`);
            if (sidebar) {
                sidebar.classList.remove('visible');
            }
        },
        
        showSidebar(moduleId) {
            const sidebar = document.getElementById(`wta-sidebar-${"$"}{moduleId}`);
            if (sidebar) {
                sidebar.classList.add('visible');
            }
        },
        
        // ==================== 创建底部栏 ====================
        createBottomBar(moduleInfo) {
            const config = moduleInfo.uiConfig || {};
            const items = config.toolbarItems || [];
            
            // Check是否已经有底部栏，如果有则合并
            let bottomBar = document.getElementById('wta-bottom-bar');
            if (!bottomBar) {
                bottomBar = document.createElement('div');
                bottomBar.id = 'wta-bottom-bar';
                bottomBar.className = 'wta-bottom-bar';
                bottomBar.innerHTML = '';
                document.body.appendChild(bottomBar);
            }
            
            // 添加模块的底部栏项
            items.forEach((item, idx) => {
                const itemEl = document.createElement('div');
                itemEl.className = 'wta-bottom-bar-item';
                itemEl.id = `wta-bb-item-${"$"}{moduleInfo.id}-${"$"}{idx}`;
                itemEl.onclick = () => this.onBottomBarItemClick(moduleInfo.id, idx);
                itemEl.innerHTML = `
                    <div class="wta-bottom-bar-item-icon">${"$"}{item.icon || '⚙️'}</div>
                    <div class="wta-bottom-bar-item-label">${"$"}{item.label || ''}</div>
                    ${"$"}{item.badge ? `<span class="wta-bottom-bar-item-badge">${"$"}{item.badge}</span>` : ''}
                `;
                bottomBar.appendChild(itemEl);
            });
            
            this.uiContainers[moduleInfo.id] = { type: 'bottomBar', items: items.length };
        },
        
        onBottomBarItemClick(moduleId, itemIndex) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            const items = module.uiConfig?.toolbarItems || [];
            const item = items[itemIndex];
            
            // 移除其他活动状态
            document.querySelectorAll('.wta-bottom-bar-item').forEach(el => el.classList.remove('active'));
            document.getElementById(`wta-bb-item-${"$"}{moduleId}-${"$"}{itemIndex}`)?.classList.add('active');
            
            if (item && item.action) {
                try {
                    if (typeof item.action === 'function') {
                        item.action();
                    } else if (typeof item.action === 'string') {
                        eval(item.action);
                    }
                } catch (e) {
                    console.error('[WTA] Bottom bar item action error:', e);
                }
            }
            if (module.onBottomBarClick) {
                module.onBottomBarClick(itemIndex, item);
            }
        },
        
        setBottomBarVisible(visible) {
            const bar = document.getElementById('wta-bottom-bar');
            if (bar) {
                bar.classList.toggle('hidden', !visible);
            }
        },
        
        // ==================== 创建悬浮面板 ====================
        createFloatingPanel(moduleInfo) {
            const config = moduleInfo.uiConfig || {};
            const position = config.position || UI_POSITION.CENTER;
            const draggable = config.draggable !== false;
            const resizable = config.resizable !== false;
            const width = config.panelWidth || 320;
            const height = config.panelHeight || 400;
            const showCloseButton = config.showCloseButton !== false;
            const showMinimizeButton = config.showMinimizeButton !== false;
            
            const panel = document.createElement('div');
            panel.id = `wta-fpanel-${"$"}{moduleInfo.id}`;
            panel.className = `wta-floating-panel ${"$"}{positionClassMap[position] || ''}`;
            panel.style.width = width + 'px';
            panel.style.height = height + 'px';
            
            let actionsHtml = '';
            if (showMinimizeButton) {
                actionsHtml += `<div class="wta-floating-panel-btn" onclick="__WTA_PANEL__.minimizeFloatingPanel('${"$"}{moduleInfo.id}')">−</div>`;
            }
            if (showCloseButton) {
                actionsHtml += `<div class="wta-floating-panel-btn" onclick="__WTA_PANEL__.hideFloatingPanel('${"$"}{moduleInfo.id}')">×</div>`;
            }
            
            panel.innerHTML = `
                <div class="wta-floating-panel-header">
                    <span class="wta-floating-panel-title">${"$"}{moduleInfo.name || T.unnamed}</span>
                    <div class="wta-floating-panel-actions">${"$"}{actionsHtml}</div>
                </div>
                <div class="wta-floating-panel-content" id="wta-fpanel-content-${"$"}{moduleInfo.id}">
                    ${"$"}{moduleInfo.panelHtml || ''}
                </div>
                ${"$"}{resizable ? '<div class="wta-floating-panel-resize"></div>' : ''}
            `;
            
            document.body.appendChild(panel);
            
            if (draggable) {
                DragManager.makeDraggable(panel, panel.querySelector('.wta-floating-panel-header'));
            }
            
            // Default显示
            setTimeout(() => panel.classList.add('visible'), 10);
            
            this.uiContainers[moduleInfo.id] = panel;
            
            if (moduleInfo.onAction) {
                const content = document.getElementById(`wta-fpanel-content-${"$"}{moduleInfo.id}`);
                moduleInfo.onAction(content);
            }
        },
        
        showFloatingPanel(moduleId) {
            const panel = document.getElementById(`wta-fpanel-${"$"}{moduleId}`);
            if (panel) {
                panel.classList.add('visible');
            }
        },
        
        hideFloatingPanel(moduleId) {
            const panel = document.getElementById(`wta-fpanel-${"$"}{moduleId}`);
            if (panel) {
                panel.classList.remove('visible');
            }
        },
        
        minimizeFloatingPanel(moduleId) {
            // 简化实现：隐藏面板
            this.hideFloatingPanel(moduleId);
            this.showToast('面板已最小化');
        },
        
        updateFloatingPanelContent(moduleId, html) {
            const content = document.getElementById(`wta-fpanel-content-${"$"}{moduleId}`);
            if (content) {
                content.innerHTML = html;
            }
        },
        
        // ==================== 创建迷你按钮 ====================
        createMiniButton(moduleInfo) {
            const config = moduleInfo.uiConfig || {};
            const position = config.position || UI_POSITION.BOTTOM_RIGHT;
            const draggable = config.draggable !== false;
            
            const btn = document.createElement('div');
            btn.id = `wta-mini-${"$"}{moduleInfo.id}`;
            btn.className = `wta-mini-btn ${"$"}{positionClassMap[position] || ''}`;
            btn.innerHTML = `
                ${"$"}{moduleInfo.icon || '🔧'}
                ${"$"}{config.showBadge !== false ? '<span class="badge" style="display:none"></span>' : ''}
                ${"$"}{config.showLabelOnHover !== false ? `<span class="wta-mini-btn-label">${"$"}{moduleInfo.name || ''}</span>` : ''}
            `;
            
            btn.onclick = () => this.onMiniButtonClick(moduleInfo.id);
            
            document.body.appendChild(btn);
            
            if (draggable) {
                DragManager.makeDraggable(btn);
            }
            
            this.uiContainers[moduleInfo.id] = btn;
        },
        
        onMiniButtonClick(moduleId) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            if (module.panelHtml || module.onAction) {
                // Create或显示弹出面板
                this.showMiniButtonPanel(moduleId);
            } else if (module.onClick) {
                module.onClick();
            }
        },
        
        showMiniButtonPanel(moduleId) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            // Check是否已有弹出面板
            let popup = document.getElementById(`wta-mini-popup-${"$"}{moduleId}`);
            if (!popup) {
                popup = document.createElement('div');
                popup.id = `wta-mini-popup-${"$"}{moduleId}`;
                popup.className = 'wta-floating-panel';
                popup.style.width = '300px';
                popup.style.maxHeight = '400px';
                
                const btn = document.getElementById(`wta-mini-${"$"}{moduleId}`);
                if (btn) {
                    const rect = btn.getBoundingClientRect();
                    popup.style.bottom = (window.innerHeight - rect.top + 10) + 'px';
                    popup.style.right = (window.innerWidth - rect.right) + 'px';
                }
                
                popup.innerHTML = `
                    <div class="wta-floating-panel-header">
                        <span class="wta-floating-panel-title">${"$"}{module.name || T.unnamed}</span>
                        <div class="wta-floating-panel-actions">
                            <div class="wta-floating-panel-btn" onclick="__WTA_PANEL__.hideMiniButtonPanel('${"$"}{moduleId}')">×</div>
                        </div>
                    </div>
                    <div class="wta-floating-panel-content" id="wta-mini-popup-content-${"$"}{moduleId}">
                        ${"$"}{module.panelHtml || ''}
                    </div>
                `;
                
                document.body.appendChild(popup);
                
                if (module.onAction) {
                    const content = document.getElementById(`wta-mini-popup-content-${"$"}{moduleId}`);
                    module.onAction(content);
                }
            }
            
            setTimeout(() => popup.classList.add('visible'), 10);
        },
        
        hideMiniButtonPanel(moduleId) {
            const popup = document.getElementById(`wta-mini-popup-${"$"}{moduleId}`);
            if (popup) {
                popup.classList.remove('visible');
            }
        },
        
        updateMiniButtonBadge(moduleId, count) {
            const btn = document.getElementById(`wta-mini-${"$"}{moduleId}`);
            if (btn) {
                const badge = btn.querySelector('.badge');
                if (badge) {
                    badge.textContent = count;
                    badge.style.display = count > 0 ? 'flex' : 'none';
                }
            }
        },
        
        // ==================== 创建自定义UI ====================
        createCustomUI(moduleInfo) {
            const config = moduleInfo.uiConfig || {};
            const position = config.position || UI_POSITION.BOTTOM_RIGHT;
            const customHtml = config.customHtml || moduleInfo.panelHtml || '';
            
            const container = document.createElement('div');
            container.id = `wta-custom-${"$"}{moduleInfo.id}`;
            container.className = `wta-custom-container ${"$"}{positionClassMap[position] || ''}`;
            container.innerHTML = customHtml;
            
            document.body.appendChild(container);
            
            if (config.draggable) {
                DragManager.makeDraggable(container);
            }
            
            this.uiContainers[moduleInfo.id] = container;
            
            // 调用onCustomInit回调
            if (moduleInfo.onCustomInit) {
                moduleInfo.onCustomInit(container);
            }
        },
        
        updateCustomUI(moduleId, html) {
            const container = document.getElementById(`wta-custom-${"$"}{moduleId}`);
            if (container) {
                container.innerHTML = html;
            }
        },
        
        // ==================== 通用方法 ====================
        
        // Update模块列表（仅用于FLOATING_BUTTON类型）
        updateModules() {
            const grid = document.getElementById('wta-module-grid');
            if (!grid) return;
            
            // 只显示FLOATING_BUTTON类型的模块
            const fabModules = this.modules.filter(m => {
                const uiType = m.uiConfig?.type || UI_TYPE.FLOATING_BUTTON;
                return uiType === UI_TYPE.FLOATING_BUTTON;
            });
            
            if (fabModules.length === 0) {
                grid.innerHTML = `
                    <div class="wta-empty-state" style="grid-column: 1/-1">
                        <div class="wta-empty-icon">📦</div>
                        <div class="wta-empty-text">${'$'}{T.noModulesAvailable}</div>
                    </div>
                `;
                return;
            }
            
            grid.innerHTML = fabModules.map(m => `
                <div class="wta-module-item" onclick="__WTA_PANEL__.onModuleClick('${"$"}{m.id}')">
                    <div class="wta-module-icon">
                        ${"$"}{m.icon || '📦'}
                    </div>
                    <div class="wta-module-name">${"$"}{m.name || T.unnamed}</div>
                </div>
            `).join('');
        },
        
        // Update徽章
        updateBadge() {
            const badge = document.querySelector('#wta-ext-fab .badge');
            if (badge) {
                const fabModules = this.modules.filter(m => {
                    const uiType = m.uiConfig?.type || UI_TYPE.FLOATING_BUTTON;
                    return uiType === UI_TYPE.FLOATING_BUTTON;
                });
                const count = fabModules.length;
                badge.textContent = count;
                badge.style.display = count > 0 ? 'flex' : 'none';
            }
            
            // 如果没有FLOATING_BUTTON类型的模块，隐藏FAB
            const fab = document.getElementById('wta-ext-fab');
            const hasFabModules = this.modules.some(m => {
                const uiType = m.uiConfig?.type || UI_TYPE.FLOATING_BUTTON;
                return uiType === UI_TYPE.FLOATING_BUTTON;
            });
            if (fab && !this.isOpen) {
                fab.style.display = hasFabModules ? 'flex' : 'none';
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
        
        // Show面板
        showPanel() {
            const panel = document.getElementById('wta-ext-main-panel');
            const overlay = document.getElementById('wta-ext-overlay');
            const fab = document.getElementById('wta-ext-fab');
            
            panel.classList.add('visible');
            overlay.classList.add('visible');
            fab.style.display = 'none';
            this.isOpen = true;
            
            // Hide模块详情
            this.hideModuleDetail();
        },
        
        // Hide面板
        hidePanel() {
            const panel = document.getElementById('wta-ext-main-panel');
            const overlay = document.getElementById('wta-ext-overlay');
            const fab = document.getElementById('wta-ext-fab');
            
            panel.classList.remove('visible');
            overlay.classList.remove('visible');
            
            // Check是否有FLOATING_BUTTON类型的模块
            const hasFabModules = this.modules.some(m => {
                const uiType = m.uiConfig?.type || UI_TYPE.FLOATING_BUTTON;
                return uiType === UI_TYPE.FLOATING_BUTTON;
            });
            fab.style.display = hasFabModules ? 'flex' : 'none';
            
            this.isOpen = false;
            this.activeModuleId = null;
            
            // Hide模块详情
            this.hideModuleDetail();
        },

        // Module点击
        onModuleClick(moduleId) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            // 如果模块有面板内容，显示详情
            if (module.panelHtml || module.onAction) {
                this.showModulePanel(moduleId);
            } else if (module.onClick) {
                // Execute点击回调
                module.onClick();
                this.hidePanel();
            }
        },

        // Show模块详情面板
        showModulePanel(moduleId) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            this.activeModuleId = moduleId;
            
            const detail = document.getElementById('wta-module-detail');
            const title = document.getElementById('wta-detail-title');
            const content = document.getElementById('wta-detail-content');
            
            title.textContent = module.name || '模块详情';
            
            // Set面板内容
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
        
        // Hide模块详情
        hideModuleDetail() {
            const detail = document.getElementById('wta-module-detail');
            if (detail) {
                detail.classList.remove('visible');
            }
            this.activeModuleId = null;
        },
        
        // Update模块面板内容
        updateModulePanelContent(moduleId, html) {
            if (this.activeModuleId !== moduleId) return;
            const content = document.getElementById('wta-detail-content');
            if (content) {
                content.innerHTML = html;
            }
        },

        // Show Toast
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
        
        // Set FAB 可见性
        setFabVisible(visible) {
            const fab = document.getElementById('wta-ext-fab');
            if (fab) {
                fab.style.display = visible ? 'flex' : 'none';
            }
        },
        
        // Set FAB 位置
        setFabPosition(bottom, right) {
            const fab = document.getElementById('wta-ext-fab');
            if (fab) {
                fab.style.bottom = bottom + 'px';
                fab.style.right = right + 'px';
            }
        },
        
        // GetUI类型常量
        getUITypes() {
            return UI_TYPE;
        },
        
        // Get位置常量
        getPositions() {
            return UI_POSITION;
        }
    };
    
    // 暴露全局接口
    window.__WTA_PANEL__ = WTA_PANEL;
    
    // Initialize
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
    
    // UI 类型常量
    const UI_TYPE = {
        FLOATING_BUTTON: 'FLOATING_BUTTON',   // Default统一面板按钮
        FLOATING_TOOLBAR: 'FLOATING_TOOLBAR', // 悬浮工具栏
        SIDEBAR: 'SIDEBAR',                   // 侧边栏
        BOTTOM_BAR: 'BOTTOM_BAR',             // 底部栏
        FLOATING_PANEL: 'FLOATING_PANEL',     // 悬浮面板
        MINI_BUTTON: 'MINI_BUTTON',           // 迷你按钮
        CUSTOM: 'CUSTOM'                      // CustomUI
    };
    
    // 位置常量
    const UI_POSITION = {
        TOP_LEFT: 'TOP_LEFT',
        TOP_CENTER: 'TOP_CENTER',
        TOP_RIGHT: 'TOP_RIGHT',
        CENTER_LEFT: 'CENTER_LEFT',
        CENTER: 'CENTER',
        CENTER_RIGHT: 'CENTER_RIGHT',
        BOTTOM_LEFT: 'BOTTOM_LEFT',
        BOTTOM_CENTER: 'BOTTOM_CENTER',
        BOTTOM_RIGHT: 'BOTTOM_RIGHT'
    };
    
    // Module UI 辅助对象
    window.__WTA_MODULE_UI__ = {
        // Export常量
        UI_TYPE: UI_TYPE,
        UI_POSITION: UI_POSITION,
        
        /**
         * 注册模块
         * @param {Object} config 模块配置
         * @param {string} config.id 模块ID（必填）
         * @param {string} config.name 模块名称（必填）
         * @param {string} config.icon 模块图标（emoji）
         * @param {string} config.panelHtml 面板HTML内容
         * @param {Function} config.onClick 点击回调
         * @param {Function} config.onAction 动态生成面板内容的回调
         * @param {Object} config.uiConfig UI配置对象
         * @param {string} config.uiConfig.type UI类型（UI_TYPE常量）
         * @param {string} config.uiConfig.position 位置（UI_POSITION常量）
         * @param {boolean} config.uiConfig.draggable 是否可拖动
         * @param {boolean} config.uiConfig.collapsible 是否可折叠（工具栏）
         * @param {string} config.uiConfig.toolbarOrientation 工具栏方向（'HORIZONTAL'|'VERTICAL'）
         * @param {Array} config.uiConfig.toolbarItems 工具栏项数组
         * @param {string} config.uiConfig.sidebarPosition 侧边栏位置（'LEFT'|'RIGHT'）
         * @param {number} config.uiConfig.sidebarWidth 侧边栏宽度
         * @param {number} config.uiConfig.panelWidth 悬浮面板宽度
         * @param {number} config.uiConfig.panelHeight 悬浮面板高度
         * @param {boolean} config.uiConfig.resizable 是否可调整大小
         * @param {boolean} config.uiConfig.showCloseButton 显示关闭按钮
         * @param {boolean} config.uiConfig.showMinimizeButton 显示最小化按钮
         * @param {string} config.uiConfig.customHtml 自定义UI的HTML
         */
        register(config) {
            waitForPanel(panel => {
                panel.registerModule(config);
            });
        },
        
        /**
         * 创建工具栏项
         * @param {string} id 项ID
         * @param {string} icon 图标(emoji)
         * @param {string} label 标签
         * @param {Function|string} action 点击动作
         * @param {Object} options 额外选项
         * @returns {Object} 工具栏项配置
         */
        createToolbarItem(id, icon, label, action, options = {}) {
            return {
                id: id,
                icon: icon,
                label: label,
                action: action,
                tooltip: options.tooltip || label,
                showLabel: options.showLabel !== false,
                badge: options.badge || null
            };
        },
        
        /**
         * 更新模块面板内容（FLOATING_BUTTON类型）
         */
        updatePanel(moduleId, html) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateModulePanelContent(moduleId, html);
            }
        },
        
        /**
         * 更新悬浮面板内容
         */
        updateFloatingPanel(moduleId, html) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateFloatingPanelContent(moduleId, html);
            }
        },
        
        /**
         * 更新自定义UI内容
         */
        updateCustomUI(moduleId, html) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateCustomUI(moduleId, html);
            }
        },
        
        /**
         * 更新迷你按钮徽章
         */
        updateMiniButtonBadge(moduleId, count) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateMiniButtonBadge(moduleId, count);
            }
        },
        
        /**
         * 显示/隐藏侧边栏
         */
        showSidebar(moduleId) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.showSidebar(moduleId);
            }
        },
        
        hideSidebar(moduleId) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.hideSidebar(moduleId);
            }
        },
        
        toggleSidebar(moduleId) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.toggleSidebar(moduleId);
            }
        },
        
        /**
         * 显示/隐藏悬浮面板
         */
        showFloatingPanel(moduleId) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.showFloatingPanel(moduleId);
            }
        },
        
        hideFloatingPanel(moduleId) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.hideFloatingPanel(moduleId);
            }
        },
        
        /**
         * 显示/隐藏底部栏
         */
        setBottomBarVisible(visible) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.setBottomBarVisible(visible);
            }
        },
        
        /**
         * 切换工具栏折叠状态
         */
        toggleToolbarCollapse(moduleId) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.toggleToolbarCollapse(moduleId);
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
         * 关闭主面板
         */
        closePanel() {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.hidePanel();
            }
        },
        
        /**
         * 显示主面板
         */
        showPanel() {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.showPanel();
            }
        },
        
        /**
         * 返回模块列表
         */
        back() {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.hideModuleDetail();
            }
        },
        
        /**
         * 设置FAB可见性
         */
        setFabVisible(visible) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.setFabVisible(visible);
            }
        },
        
        /**
         * 设置FAB位置
         */
        setFabPosition(bottom, right) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.setFabPosition(bottom, right);
            }
        }
    };
})();
""".trimIndent()
}
