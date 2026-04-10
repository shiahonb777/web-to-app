package com.webtoapp.core.extension

/**
 * 统一扩展模块面板脚本
 * 
 * Provide unified UI panel，与应用主题风格一致
 * 采用毛玻璃效果、渐变色、圆角等现代设计元素
 */
object ExtensionPanelScript {
    
    // 预设图标 SVG 映射表（id → SVG HTML）
    private val ICON_SVG_MAP = mapOf(
        "star" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><polygon points="12,2 15.09,8.26 22,9.27 17,14.14 18.18,21 12,17.77 5.82,21 7,14.14 2,9.27 8.91,8.26"/></svg>""",
        "bolt" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><polygon points="13,2 3,14 12,14 11,22 21,10 12,10"/></svg>""",
        "shield" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z"/></svg>""",
        "heart" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>""",
        "play" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M8 5v14l11-7z"/></svg>""",
        "gear" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M19.14 12.94c.04-.31.06-.63.06-.94s-.02-.63-.06-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54C14.46 2.18 14.25 2 14 2h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58C4.86 11.37 4.84 11.69 4.84 12s.02.63.06.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41H14c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z"/></svg>""",
        "globe" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/></svg>""",
        "lock" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>""",
        "moon" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M9.37 5.51A7.35 7.35 0 009 7.5C9 12.19 12.81 16 17.5 16c.69 0 1.37-.1 2-.28A9 9 0 0112 21 9 9 0 013 12a9 9 0 016.37-8.6z"/></svg>""",
        "palette" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M12 2C6.49 2 2 6.49 2 12s4.49 10 10 10c1.38 0 2.5-1.12 2.5-2.5 0-.61-.23-1.2-.64-1.67-.08-.1-.13-.21-.13-.33 0-.28.22-.5.5-.5H16c3.31 0 6-2.69 6-6 0-4.96-4.49-9-10-9zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 8 6.5 8 8 8.67 8 9.5 7.33 11 6.5 11zm3-4C8.67 7 8 6.33 8 5.5S8.67 4 9.5 4s1.5.67 1.5 1.5S10.33 7 9.5 7zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 4 14.5 4s1.5.67 1.5 1.5S15.33 7 14.5 7zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 8 17.5 8s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"/></svg>""",
        "bulb" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M9 21c0 .55.45 1 1 1h4c.55 0 1-.45 1-1v-1H9v1zm3-19C8.14 2 5 5.14 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.86-3.14-7-7-7z"/></svg>""",
        "fire" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M13.5.67s.74 2.65.74 4.8c0 2.06-1.35 3.73-3.41 3.73-2.07 0-3.63-1.67-3.63-3.73l.03-.36C5.21 7.51 4 10.62 4 14c0 4.42 3.58 8 8 8s8-3.58 8-8C20 8.61 17.41 3.8 13.5.67zM11.71 19c-1.78 0-3.22-1.4-3.22-3.14 0-1.62 1.05-2.76 2.81-3.12 1.77-.36 3.6-1.21 4.62-2.58.39 1.29.59 2.65.59 4.04 0 2.65-2.15 4.8-4.8 4.8z"/></svg>""",
        "diamond" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M19 3H5L2 9l10 13L22 9l-3-6zm-1.58 5l-1.87-3h1.67l1.87 3h-1.67zM12 4.3L14.35 8H9.65L12 4.3zM7.78 5h1.67L7.58 8H5.91l1.87-3zM5.17 10h3.51L12 18.5 5.17 10zm5.5 0h2.66L12 16.53 10.67 10zm4.66 0h3.51L12 18.5l3.33-8.5z"/></svg>""",
        "rocket" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M12 2.5s-5 5.5-5 11.5c0 2.76 2.24 5 5 5s5-2.24 5-5c0-6-5-11.5-5-11.5zm0 14.5c-1.66 0-3-1.34-3-3 0-2.61 1.67-6.4 3-9.14 1.33 2.74 3 6.53 3 9.14 0 1.66-1.34 3-3 3z"/></svg>""",
        "eye" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg>""",
        "pin" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/></svg>""",
        "code" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/></svg>""",
        "music" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M12 3v10.55c-.59-.34-1.27-.55-2-.55-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4V7h4V3h-6z"/></svg>""",
        "camera" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><circle cx="12" cy="12" r="3.2"/><path d="M9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/></svg>""",
        "puzzle" to """<svg width="20" height="20" viewBox="0 0 24 24" fill="white"><path d="M20.5 11H19V7c0-1.1-.9-2-2-2h-4V3.5C13 2.12 11.88 1 10.5 1S8 2.12 8 3.5V5H4c-1.1 0-2 .9-2 2v3.8h1.5c1.49 0 2.7 1.21 2.7 2.7s-1.21 2.7-2.7 2.7H2V20c0 1.1.9 2 2 2h3.8v-1.5c0-1.49 1.21-2.7 2.7-2.7 1.49 0 2.7 1.21 2.7 2.7V22H17c1.1 0 2-.9 2-2v-4h1.5c1.38 0 2.5-1.12 2.5-2.5S21.88 11 20.5 11z"/></svg>"""
    )
    
    private val DEFAULT_SVG = """<svg width="20" height="20" viewBox="0 0 20 20" fill="white"><rect x="1" y="1" width="7.5" height="7.5" rx="2"/><rect x="11.5" y="1" width="7.5" height="7.5" rx="2"/><rect x="1" y="11.5" width="7.5" height="7.5" rx="2"/><rect x="11.5" y="11.5" width="7.5" height="7.5" rx="2"/></svg>"""
    
    /**
     * 获取面板初始化脚本
     * 应在页面加载时注入
     * @param fabIcon 自定义 FAB 图标 ID（如 "star"/"bolt"）、"custom:base64" 或旧版 emoji，空则使用默认图标
     */
    fun getPanelInitScript(fabIcon: String = ""): String {
        val icon = convertFabIconToHtml(fabIcon)
        return getScript(icon)
    }
    
    /**
     * 将 FAB 图标值转换为 HTML（SVG / img 标签 / emoji）
     */
    private fun convertFabIconToHtml(fabIcon: String): String {
        if (fabIcon.isBlank()) return DEFAULT_SVG
        // 自定义相册图片：custom:base64data
        if (fabIcon.startsWith("custom:")) {
            val base64 = fabIcon.removePrefix("custom:")
            return """<img src="data:image/png;base64,$base64" width="28" height="28" style="border-radius:50%;object-fit:cover;box-shadow:0 1px 4px rgba(0,0,0,0.15)">"""
        }
        // 预设图标 ID
        ICON_SVG_MAP[fabIcon]?.let { return it }
        // 向后兼容旧版 emoji 值
        return fabIcon
    }
    
    private fun getScript(fabIcon: String): String = """
(function() {
    'use strict';
    
    // 防止重复初始化
    if (window.__WTA_PANEL__) return;
    
    // ==================== 多语言支持 ====================
    const LANG = (navigator.language || 'zh').toLowerCase().startsWith('ar') ? 'ar' : 
                 (navigator.language || 'zh').toLowerCase().startsWith('zh') ? 'zh' : 'en';
    const I18N = {
        zh: {
            panelTitle: '扩展管理',
            noModulesAvailable: '暂无可用扩展',
            panelInitialized: '扩展面板已初始化',
            unnamed: '未命名',
            tabAll: '全部',
            tabModules: '扩展模块',
            tabExtensions: '浏览器扩展',
            search: '搜索扩展...',
            active: '运行中',
            inactive: '未激活',
            enabled: '已启用',
            disabled: '已禁用',
            version: '版本',
            author: '作者',
            permissions: '权限',
            urlRules: 'URL 匹配规则',
            runAt: '注入时机',
            world: '执行环境',
            noDescription: '暂无描述',
            moduleCount: '个扩展模块',
            extCount: '个浏览器扩展',
            details: '详情',
            matchesCurrentPage: '匹配当前页面',
            notMatchCurrentPage: '不匹配当前页面',
            typeUserScript: '用户脚本',
            showModules: '显示扩展模块',
            fabHidden: '扩展模块已隐藏，点击右侧手柄显示',
            panelMinimized: '面板已最小化',
            launchWindow: '启动独立窗口',
            quickActions: '快捷操作',
            autoRunning: '自动运行中',
            autoRunningDesc: '此模块为自动加载模式，无需手动操作',
            runModeInteractive: '交互模式',
            runModeAuto: '自动模式',
            permDomAccess: 'DOM 访问',
            permStorage: '数据存储',
            permCssInject: 'CSS 注入',
            permDownload: '下载',
            permMedia: '媒体',
            permNetwork: '网络',
            permClipboard: '剪贴板',
            runAtDocStart: '文档开始加载',
            runAtDocEnd: '文档加载完成',
            runAtDocIdle: '文档空闲时',
            worldIsolated: '隔离环境',
            worldMain: '主环境'
        },
        en: {
            panelTitle: 'Extensions',
            noModulesAvailable: 'No extensions available',
            panelInitialized: 'Extension panel initialized',
            unnamed: 'Unnamed',
            tabAll: 'All',
            tabModules: 'Modules',
            tabExtensions: 'Browser Ext',
            search: 'Search extensions...',
            active: 'Active',
            inactive: 'Inactive',
            enabled: 'Enabled',
            disabled: 'Disabled',
            version: 'Version',
            author: 'Author',
            permissions: 'Permissions',
            urlRules: 'URL Match Rules',
            runAt: 'Run At',
            world: 'World',
            noDescription: 'No description',
            moduleCount: ' modules',
            extCount: ' browser extensions',
            details: 'Details',
            matchesCurrentPage: 'Matches current page',
            notMatchCurrentPage: 'Does not match current page',
            typeUserScript: 'UserScript',
            showModules: 'Show modules',
            fabHidden: 'Module hidden, tap edge handle to show',
            panelMinimized: 'Panel minimized',
            launchWindow: 'Launch Window',
            quickActions: 'Quick Actions',
            autoRunning: 'Auto Running',
            autoRunningDesc: 'This module runs automatically, no manual operation needed',
            runModeInteractive: 'Interactive',
            runModeAuto: 'Auto',
            permDomAccess: 'DOM Access',
            permStorage: 'Storage',
            permCssInject: 'CSS Inject',
            permDownload: 'Download',
            permMedia: 'Media',
            permNetwork: 'Network',
            permClipboard: 'Clipboard',
            runAtDocStart: 'Document Start',
            runAtDocEnd: 'Document End',
            runAtDocIdle: 'Document Idle',
            worldIsolated: 'Isolated',
            worldMain: 'Main'
        },
        ar: {
            panelTitle: 'الإضافات',
            noModulesAvailable: 'لا توجد إضافات متاحة',
            panelInitialized: 'تم تهيئة لوحة الإضافات',
            unnamed: 'بدون اسم',
            tabAll: 'الكل',
            tabModules: 'الوحدات',
            tabExtensions: 'إضافات المتصفح',
            search: 'بحث...',
            active: 'نشط',
            inactive: 'غير نشط',
            enabled: 'مفعل',
            disabled: 'معطل',
            version: 'الإصدار',
            author: 'المؤلف',
            permissions: 'الأذونات',
            urlRules: 'قواعد URL',
            runAt: 'وقت التشغيل',
            world: 'البيئة',
            noDescription: 'لا يوجد وصف',
            moduleCount: ' وحدات',
            extCount: ' إضافات',
            details: 'تفاصيل',
            matchesCurrentPage: 'يطابق الصفحة الحالية',
            notMatchCurrentPage: 'لا يطابق الصفحة الحالية',
            typeUserScript: 'سكربت مستخدم',
            showModules: 'عرض الوحدات',
            fabHidden: 'تم إخفاء الوحدة، انقر للعرض',
            panelMinimized: 'تم تصغير اللوحة',
            launchWindow: 'تشغيل نافذة مستقلة',
            quickActions: 'إجراءات سريعة',
            autoRunning: 'تشغيل تلقائي',
            autoRunningDesc: 'هذه الوحدة تعمل تلقائياً، لا تحتاج تشغيل يدوي',
            runModeInteractive: 'تفاعلي',
            runModeAuto: 'تلقائي',
            permDomAccess: 'وصول DOM',
            permStorage: 'التخزين',
            permCssInject: 'حقن CSS',
            permDownload: 'تحميل',
            permMedia: 'وسائط',
            permNetwork: 'شبكة',
            permClipboard: 'الحافظة',
            runAtDocStart: 'بداية المستند',
            runAtDocEnd: 'نهاية المستند',
            runAtDocIdle: 'خمول المستند',
            worldIsolated: 'بيئة معزولة',
            worldMain: 'بيئة رئيسية'
        }
    };
    const T = I18N[LANG] || I18N.en;
    
    // 权限/注入时机/执行环境 翻译映射
    const PERM_MAP = {
        DOM_ACCESS: T.permDomAccess, STORAGE: T.permStorage, CSS_INJECT: T.permCssInject,
        DOWNLOAD: T.permDownload, MEDIA: T.permMedia, NETWORK: T.permNetwork, CLIPBOARD: T.permClipboard
    };
    const RUNAT_MAP = {
        DOCUMENT_START: T.runAtDocStart, DOCUMENT_END: T.runAtDocEnd, DOCUMENT_IDLE: T.runAtDocIdle
    };
    const WORLD_MAP = {
        ISOLATED: T.worldIsolated, MAIN: T.worldMain
    };
    
    // ==================== 样式定义 ====================
    const PANEL_STYLES = `
        /* CSS 变量 - 主题色 */
        :root {
            --wta-primary: #3B82F6;
            --wta-primary-light: #60A5FA;
            --wta-primary-dark: #2563EB;
            --wta-gradient: #3B82F6;
            --wta-gradient-dark: #2563EB;
            --wta-surface: rgba(255, 255, 255, 0.95);
            --wta-surface-dim: rgba(255, 255, 255, 0.85);
            --wta-on-surface: #111827;
            --wta-on-surface-variant: #6b7280;
            --wta-outline: rgba(0, 0, 0, 0.06);
            --wta-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
            --wta-shadow-lg: 0 8px 40px rgba(0, 0, 0, 0.12);
            --wta-radius: 16px;
            --wta-radius-sm: 10px;
            --wta-radius-lg: 24px;
            --wta-success: #10B981;
            --wta-warning: #F59E0B;
            --wta-danger: #EF4444;
        }
        
        @media (prefers-color-scheme: dark) {
            :root {
                --wta-surface: rgba(24, 24, 27, 0.95);
                --wta-surface-dim: rgba(24, 24, 27, 0.85);
                --wta-on-surface: #f3f4f6;
                --wta-on-surface-variant: #9ca3af;
                --wta-outline: rgba(255, 255, 255, 0.08);
                --wta-shadow: 0 4px 24px rgba(0, 0, 0, 0.3);
                --wta-shadow-lg: 0 8px 40px rgba(0, 0, 0, 0.4);
            }
        }

        /* 主容器 - 最高层级，防止被页面元素遮挡 */
        #wta-ext-panel-container {
            position: fixed !important;
            top: 0 !important;
            left: 0 !important;
            right: 0 !important;
            bottom: 0 !important;
            z-index: 2147483647 !important;
            pointer-events: none !important;
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif;
            -webkit-font-smoothing: antialiased;
            isolation: isolate !important;
            transform: none !important;
            will-change: auto !important;
            contain: none !important;
            filter: none !important;
            perspective: none !important;
            clip-path: none !important;
            mask: none !important;
            opacity: 1 !important;
            visibility: visible !important;
            overflow: visible !important;
        }
        
        /* 悬浮触发按钮 - 毛玻璃效果，最高层级 */
        #wta-ext-fab {
            position: fixed !important;
            bottom: 80px;
            right: 16px;
            width: 50px !important;
            height: 50px !important;
            border-radius: 14px;
            background: var(--wta-primary) !important;
            color: white !important;
            display: flex !important;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            cursor: pointer;
            z-index: 2147483647 !important;
            box-shadow: 0 4px 16px rgba(59, 130, 246, 0.35);
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            pointer-events: auto !important;
            -webkit-tap-highlight-color: transparent;
            user-select: none;
            border: none;
            touch-action: none;
            isolation: isolate !important;
        }
        
        #wta-ext-fab:not(.dragging):hover {
            transform: scale(1.05);
            box-shadow: 0 6px 24px rgba(59, 130, 246, 0.45);
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
        
        /* 显示按钮 - 当FAB隐藏时显示，更明显的半圆手柄 */
        #wta-ext-show-btn {
            position: fixed !important;
            top: 50%;
            right: 0;
            transform: translateY(-50%);
            width: 14px;
            height: 72px;
            background: var(--wta-gradient);
            border-radius: 12px 0 0 12px;
            display: none;
            align-items: center;
            justify-content: center;
            color: white;
            font-size: 12px;
            cursor: pointer;
            z-index: 2147483647 !important;
            pointer-events: auto !important;
            box-shadow: -4px 0 16px rgba(59, 130, 246, 0.4);
            transition: width 0.25s ease, opacity 0.25s ease;
            opacity: 0.6;
            animation: wta-pulse 2s ease-in-out infinite;
        }
        
        @keyframes wta-pulse {
            0%, 100% { opacity: 0.6; }
            50% { opacity: 0.85; }
        }
        
        #wta-ext-show-btn:hover {
            width: 28px;
            opacity: 1;
            animation: none;
        }
        
        #wta-ext-show-btn:active {
            width: 24px;
        }
        
        #wta-ext-show-btn.visible {
            display: flex;
        }
        
        /* 边缘隐藏区域提示 */
        #wta-ext-hide-zone {
            position: fixed;
            top: 0;
            bottom: 0;
            width: 60px;
            z-index: 2147483645;
            pointer-events: none;
            opacity: 0;
            transition: opacity 0.2s ease;
        }
        
        #wta-ext-hide-zone.left {
            left: 0;
            background: linear-gradient(to right, rgba(59, 130, 246, 0.3), transparent);
        }
        
        #wta-ext-hide-zone.right {
            right: 0;
            background: linear-gradient(to left, rgba(59, 130, 246, 0.3), transparent);
        }
        
        #wta-ext-hide-zone.active {
            opacity: 1;
        }
        
        /* FAB 吸附动画 */
        #wta-ext-fab.snapping {
            transition: left 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), 
                        top 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), 
                        right 0.3s cubic-bezier(0.34, 1.56, 0.64, 1),
                        bottom 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
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
            position: fixed !important;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: rgba(0, 0, 0, 0.3);
            opacity: 0;
            visibility: hidden;
            transition: all 0.35s ease;
            z-index: 2147483644 !important;
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
            position: fixed !important;
            bottom: 0;
            left: 0;
            right: 0;
            max-height: 75vh;
            background: var(--wta-surface);
            border-radius: var(--wta-radius-lg) var(--wta-radius-lg) 0 0;
            transform: translateY(100%);
            transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
            z-index: 2147483645 !important;
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
            width: 36px;
            height: 4px;
            background: var(--wta-on-surface-variant);
            border-radius: 2px;
            margin: 12px auto 6px;
            opacity: 0.25;
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
            font-size: 17px;
            font-weight: 700;
            color: var(--wta-on-surface);
            letter-spacing: -0.2px;
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

        /* 模块列表容器 */
        #wta-module-grid {
            display: block;
            padding: 4px 4px;
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
            position: fixed !important;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%) scale(0.9);
            background: var(--wta-gradient-dark);
            color: white;
            padding: 16px 32px;
            border-radius: var(--wta-radius);
            font-size: 15px;
            font-weight: 600;
            z-index: 2147483647 !important;
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
            box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
        }
        
        .wta-btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 24px rgba(59, 130, 246, 0.4);
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
            box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15);
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
            background: rgba(59, 130, 246, 0.12);
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
            color: var(--wta-on-surface);
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

        /* ==================== 搜索栏 ==================== */
        .wta-search-wrap {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 0 16px 12px;
        }
        .wta-search-box {
            flex: 1;
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 12px;
            background: var(--wta-surface-dim);
            border-radius: 10px;
            border: 1px solid var(--wta-outline);
            transition: border-color 0.2s;
        }
        .wta-search-box:focus-within {
            border-color: var(--wta-primary);
        }
        .wta-search-box svg {
            flex-shrink: 0;
            opacity: 0.4;
        }
        .wta-search-box input {
            flex: 1;
            border: none;
            outline: none;
            background: transparent;
            color: var(--wta-on-surface);
            font-size: 14px;
            font-family: inherit;
        }
        .wta-search-box input::placeholder {
            color: var(--wta-on-surface-variant);
        }

        /* ==================== 标签页 ==================== */
        .wta-tabs {
            display: flex;
            gap: 2px;
            padding: 0 16px 12px;
            border-bottom: 1px solid var(--wta-outline);
        }
        .wta-tab {
            flex: 1;
            padding: 8px 4px;
            text-align: center;
            font-size: 13px;
            font-weight: 600;
            color: var(--wta-on-surface-variant);
            cursor: pointer;
            border-radius: 8px;
            transition: all 0.2s;
            -webkit-tap-highlight-color: transparent;
            position: relative;
        }
        .wta-tab:active { transform: scale(0.97); }
        .wta-tab.active {
            color: var(--wta-primary);
            background: rgba(59, 130, 246, 0.08);
        }
        .wta-tab-count {
            display: inline-block;
            min-width: 18px;
            height: 18px;
            line-height: 18px;
            padding: 0 5px;
            border-radius: 9px;
            background: var(--wta-surface-dim);
            color: var(--wta-on-surface-variant);
            font-size: 11px;
            font-weight: 700;
            margin-left: 4px;
            text-align: center;
        }
        .wta-tab.active .wta-tab-count {
            background: rgba(59, 130, 246, 0.15);
            color: var(--wta-primary);
        }

        /* ==================== 分组标题 ==================== */
        .wta-section-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 14px 0 8px;
        }
        .wta-section-title {
            font-size: 12px;
            font-weight: 700;
            color: var(--wta-on-surface-variant);
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }
        .wta-section-count {
            font-size: 11px;
            color: var(--wta-on-surface-variant);
            font-weight: 500;
        }

        /* ==================== 列表项 ==================== */
        .wta-ext-row {
            display: flex;
            align-items: center;
            gap: 12px;
            padding: 12px;
            border-radius: 12px;
            cursor: pointer;
            transition: background 0.15s;
            -webkit-tap-highlight-color: transparent;
            margin-bottom: 2px;
        }
        .wta-ext-row:hover {
            background: var(--wta-surface-dim);
        }
        .wta-ext-row:active {
            background: rgba(59, 130, 246, 0.06);
        }
        .wta-ext-row-icon {
            width: 40px;
            height: 40px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 20px;
            flex-shrink: 0;
            background: var(--wta-surface-dim);
            border: 1px solid var(--wta-outline);
            overflow: hidden;
        }
        .wta-ext-row-icon img {
            width: 24px;
            height: 24px;
            object-fit: contain;
        }
        .wta-ext-row-icon svg {
            width: 20px;
            height: 20px;
        }
        .wta-ext-row-info {
            flex: 1;
            min-width: 0;
        }
        .wta-ext-row-name {
            font-size: 14px;
            font-weight: 600;
            color: var(--wta-on-surface);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            line-height: 1.3;
        }
        .wta-ext-row-desc {
            font-size: 12px;
            color: var(--wta-on-surface-variant);
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            margin-top: 2px;
            line-height: 1.3;
        }
        .wta-ext-row-meta {
            display: flex;
            align-items: center;
            gap: 6px;
            margin-top: 4px;
        }

        /* ==================== 类型徽章 ==================== */
        .wta-type-badge {
            display: inline-flex;
            align-items: center;
            padding: 2px 7px;
            border-radius: 4px;
            font-size: 10px;
            font-weight: 600;
            letter-spacing: 0.3px;
            white-space: nowrap;
        }
        .wta-type-badge.chrome {
            background: rgba(66, 133, 244, 0.1);
            color: #4285F4;
        }
        .wta-type-badge.module {
            background: rgba(16, 185, 129, 0.1);
            color: var(--wta-success);
        }
        .wta-type-badge.userscript {
            background: rgba(245, 158, 11, 0.1);
            color: var(--wta-warning);
        }

        /* ==================== 列表行箭头 ==================== */
        .wta-ext-row-arrow {
            flex-shrink: 0;
            width: 16px;
            height: 16px;
            opacity: 0.3;
            margin-left: 4px;
        }
        .wta-ext-row:hover .wta-ext-row-arrow {
            opacity: 0.5;
        }

        /* ==================== 详情面板增强 ==================== */
        .wta-detail-section {
            padding: 14px 0;
            border-bottom: 1px solid var(--wta-outline);
        }
        .wta-detail-section:last-child {
            border-bottom: none;
        }
        .wta-detail-section-title {
            font-size: 12px;
            font-weight: 700;
            color: var(--wta-on-surface-variant);
            text-transform: uppercase;
            letter-spacing: 0.5px;
            margin-bottom: 10px;
        }
        .wta-detail-row {
            display: flex;
            align-items: flex-start;
            justify-content: space-between;
            padding: 6px 0;
            gap: 12px;
        }
        .wta-detail-label {
            font-size: 13px;
            color: var(--wta-on-surface-variant);
            flex-shrink: 0;
        }
        .wta-detail-value {
            font-size: 13px;
            color: var(--wta-on-surface);
            font-weight: 500;
            text-align: right;
            word-break: break-all;
        }
        .wta-detail-tag {
            display: inline-block;
            padding: 3px 8px;
            border-radius: 5px;
            font-size: 11px;
            font-weight: 500;
            background: var(--wta-surface-dim);
            color: var(--wta-on-surface-variant);
            margin: 2px;
        }
        .wta-detail-url-rule {
            display: block;
            padding: 6px 10px;
            background: var(--wta-surface-dim);
            border-radius: 6px;
            font-size: 12px;
            font-family: 'SF Mono', Menlo, Monaco, 'Courier New', monospace;
            color: var(--wta-on-surface);
            margin-bottom: 4px;
            word-break: break-all;
        }
        .wta-detail-hero {
            display: flex;
            align-items: center;
            gap: 14px;
            padding: 16px 0;
        }
        .wta-detail-hero-icon {
            width: 52px;
            height: 52px;
            border-radius: 14px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 28px;
            background: var(--wta-surface-dim);
            border: 1px solid var(--wta-outline);
            flex-shrink: 0;
        }
        .wta-detail-hero-info {
            flex: 1;
            min-width: 0;
        }
        .wta-detail-hero-name {
            font-size: 18px;
            font-weight: 700;
            color: var(--wta-on-surface);
        }
        .wta-detail-hero-type {
            margin-top: 4px;
        }

        /* ==================== 空状态 (新) ==================== */
        .wta-empty-state-v2 {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 40px 24px;
            color: var(--wta-on-surface-variant);
        }
        .wta-empty-state-v2 svg {
            opacity: 0.25;
            margin-bottom: 12px;
        }
        .wta-empty-state-v2 p {
            font-size: 14px;
            text-align: center;
            margin: 0;
            font-weight: 500;
        }

        /* ==================== 面板头部布局调整 ==================== */
        .wta-panel-header {
            padding: 6px 16px 10px;
        }
        .wta-panel-header-actions {
            display: flex;
            align-items: center;
            gap: 8px;
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
    
    const RUN_MODE = {
        INTERACTIVE: 'INTERACTIVE',
        AUTO: 'AUTO'
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
                if (e.target.closest('.wta-floating-panel-resize')) return;
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
        },
        
        makeResizable(panel, handle, minW = 200, minH = 150) {
            let isResizing = false;
            let startX, startY, startW, startH;
            
            const onStart = (e) => {
                isResizing = true;
                const touch = e.touches ? e.touches[0] : e;
                startX = touch.clientX;
                startY = touch.clientY;
                startW = panel.offsetWidth;
                startH = panel.offsetHeight;
                e.preventDefault();
                e.stopPropagation();
            };
            
            const onMove = (e) => {
                if (!isResizing) return;
                const touch = e.touches ? e.touches[0] : e;
                const newW = Math.max(minW, startW + (touch.clientX - startX));
                const newH = Math.max(minH, startH + (touch.clientY - startY));
                panel.style.width = newW + 'px';
                panel.style.height = newH + 'px';
            };
            
            const onEnd = () => {
                isResizing = false;
            };
            
            handle.addEventListener('mousedown', onStart);
            handle.addEventListener('touchstart', onStart, { passive: false });
            document.addEventListener('mousemove', onMove);
            document.addEventListener('touchmove', onMove, { passive: false });
            document.addEventListener('mouseup', onEnd);
            document.addEventListener('touchend', onEnd);
        }
    };

    // ==================== 面板管理器 ====================
    const WTA_PANEL = {
        modules: [],
        uiContainers: {}, // Storage各模块的UI容器
        isOpen: false,
        activeModuleId: null,
        activeTab: 'all',
        searchQuery: '',
        _initialized: false,
        
        // Initialize
        init() {
            this.injectStyles();
            this.createDOM();
            this.bindEvents();
            // Render pre-registered modules immediately if any
            if (this.modules.length > 0) {
                this.updateModules();
                this.updateBadge();
            }
            this._initialized = true;
            console.log('[WTA Panel] Initialized (' + this.modules.length + ' modules pre-registered)');
            // Delayed re-render: modules may register AFTER init via setTimeout/delayed injection
            // Chrome extensions register at DOCUMENT_END (50ms delay), standard modules at various times
            var self = this;
            setTimeout(function() {
                self.updateModules();
                self.updateBadge();
            }, 500);
            // Second re-render for very late registrations (DOCUMENT_IDLE modules)
            setTimeout(function() {
                self.updateModules();
                self.updateBadge();
            }, 2000);
        },
        
        // Inject样式
        injectStyles() {
            if (document.getElementById('wta-panel-styles')) return;
            const style = document.createElement('style');
            style.id = 'wta-panel-styles';
            style.textContent = PANEL_STYLES;
            (document.head || document.documentElement).appendChild(style);
        },
        
        // Create DOM 结构
        createDOM() {
            // 容器
            const container = document.createElement('div');
            container.id = 'wta-ext-panel-container';
            
            // FAB 按钮
            const fab = document.createElement('div');
            fab.id = 'wta-ext-fab';
            fab.innerHTML = '$fabIcon<span class="badge" style="display:none">0</span>';
            
            // 显示按钮（当FAB隐藏时）
            const showBtn = document.createElement('div');
            showBtn.id = 'wta-ext-show-btn';
            showBtn.innerHTML = '❮';
            showBtn.title = T.showModules;
            
            // 边缘隐藏区域提示
            const hideZoneLeft = document.createElement('div');
            hideZoneLeft.id = 'wta-ext-hide-zone';
            hideZoneLeft.className = 'left';
            
            const hideZoneRight = document.createElement('div');
            hideZoneRight.id = 'wta-ext-hide-zone';
            hideZoneRight.className = 'right';
            
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
            container.appendChild(hideZoneLeft);
            container.appendChild(hideZoneRight);
            container.appendChild(overlay);
            container.appendChild(panel);
            container.appendChild(toast);
            
            // 挂载到 <html> 而非 <body>，避免 body 上的 transform/will-change 影响 fixed 定位
            (document.documentElement || document.body).appendChild(container);
            
            // 恢复保存的位置和隐藏状态
            this.restoreFabState();
            
            // 启动层级保护：确保 FAB 容器始终在最顶层
            this.startElevationGuard();
        },
        
        // 层级保护：确保容器始终在 DOM 最顶层，不被页面动态元素遮挡
        startElevationGuard() {
            const self = this;
            
            // 提升容器到最顶层
            function elevate() {
                const container = document.getElementById('wta-ext-panel-container');
                if (!container) return;
                const parent = container.parentNode;
                if (parent && parent.lastElementChild !== container) {
                    parent.appendChild(container);
                }
                // 强制重置关键样式，防止页面 JS 覆盖
                container.style.setProperty('z-index', '2147483647', 'important');
                container.style.setProperty('position', 'fixed', 'important');
                container.style.setProperty('pointer-events', 'none', 'important');
            }
            
            // 定期检查（每2秒）
            setInterval(elevate, 2000);
            
            // MutationObserver：当 DOM 变化时检查是否需要提升
            try {
                const observer = new MutationObserver(function(mutations) {
                    for (var i = 0; i < mutations.length; i++) {
                        if (mutations[i].type === 'childList' && mutations[i].addedNodes.length > 0) {
                            elevate();
                            break;
                        }
                    }
                });
                const target = document.documentElement || document.body;
                observer.observe(target, { childList: true });
                if (target !== document.body && document.body) {
                    observer.observe(document.body, { childList: true });
                }
            } catch(e) { /* DOM observer setup failed */ }
        },

        // Get面板 HTML
        getPanelHTML() {
            return `
                <div class="wta-panel-handle"></div>
                <div class="wta-panel-header">
                    <span class="wta-panel-title">${'$'}{T.panelTitle}</span>
                    <div class="wta-panel-header-actions">
                        <div class="wta-panel-close" onclick="__WTA_PANEL__.hidePanel()">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                                <path d="M18 6L6 18M6 6l12 12"/>
                            </svg>
                        </div>
                    </div>
                </div>
                <div class="wta-search-wrap">
                    <div class="wta-search-box">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>
                        <input type="text" placeholder="${'$'}{T.search}" oninput="__WTA_PANEL__.searchModules(this.value)" id="wta-search-input"/>
                    </div>
                </div>
                <div class="wta-tabs" id="wta-tabs">
                    <div class="wta-tab active" data-tab="all" onclick="__WTA_PANEL__.switchTab('all')">${'$'}{T.tabAll}<span class="wta-tab-count" id="wta-count-all">0</span></div>
                    <div class="wta-tab" data-tab="modules" onclick="__WTA_PANEL__.switchTab('modules')">${'$'}{T.tabModules}<span class="wta-tab-count" id="wta-count-modules">0</span></div>
                    <div class="wta-tab" data-tab="extensions" onclick="__WTA_PANEL__.switchTab('extensions')">${'$'}{T.tabExtensions}<span class="wta-tab-count" id="wta-count-ext">0</span></div>
                </div>
                <div class="wta-module-list">
                    <div id="wta-module-grid"></div>
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
            let isPressed = false;
            let isDragging = false;
            let hasMoved = false;
            let startX, startY, startLeft, startTop;
            let thresholdPassed = false;
            const DRAG_THRESHOLD = 8;
            const HIDE_ZONE_WIDTH = 50;
            
            const showHideZone = (side) => {
                document.querySelectorAll('#wta-ext-hide-zone').forEach(el => {
                    el.classList.toggle('active', el.classList.contains(side));
                });
            };
            
            const hideAllZones = () => {
                document.querySelectorAll('#wta-ext-hide-zone').forEach(el => {
                    el.classList.remove('active');
                });
            };
            
            const onStart = (e) => {
                // 确保是在 FAB 上开始
                if (!fab.contains(e.target)) return;
                
                isPressed = true;
                thresholdPassed = false;
                const touch = e.touches ? e.touches[0] : e;
                startX = touch.clientX;
                startY = touch.clientY;
                const rect = fab.getBoundingClientRect();
                startLeft = rect.left;
                startTop = rect.top;
                hasMoved = false;
                
                // 移除吸附动画类
                fab.classList.remove('snapping');
                
                e.preventDefault();
            };
            
            const onMove = (e) => {
                if (!isPressed) return;
                
                const touch = e.touches ? e.touches[0] : e;
                let dx = touch.clientX - startX;
                let dy = touch.clientY - startY;
                
                // 检查是否超过拖动阈值
                if (!thresholdPassed) {
                    if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                        thresholdPassed = true;
                        hasMoved = true;
                        isDragging = true;
                        fab.classList.add('dragging');
                        // 重置起点以消除跳跃感
                        startX = touch.clientX;
                        startY = touch.clientY;
                        dx = 0;
                        dy = 0;
                    } else {
                        return;
                    }
                }
                
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
                
                // 检查是否进入隐藏区域
                if (newLeft < HIDE_ZONE_WIDTH) {
                    showHideZone('left');
                } else if (newLeft > maxX - HIDE_ZONE_WIDTH) {
                    showHideZone('right');
                } else {
                    hideAllZones();
                }
                
                e.preventDefault();
            };
            
            const onEnd = (e) => {
                if (!isPressed) return;
                isPressed = false;
                hideAllZones();
                
                if (isDragging) {
                    isDragging = false;
                    fab.classList.remove('dragging');
                    
                    const rect = fab.getBoundingClientRect();
                    const fabCenterX = rect.left + rect.width / 2;
                    const maxX = window.innerWidth - fab.offsetWidth;
                    
                    // 检查是否在隐藏区域内松手
                    if (rect.left < HIDE_ZONE_WIDTH || rect.left > maxX - HIDE_ZONE_WIDTH) {
                        this.hideFab();
                        return;
                    }
                    
                    // 边缘吸附
                    fab.classList.add('snapping');
                    if (fabCenterX < window.innerWidth / 2) {
                        // 吸附到左侧
                        fab.style.left = '16px';
                        fab.style.right = 'auto';
                    } else {
                        // 吸附到右侧
                        fab.style.left = 'auto';
                        fab.style.right = '16px';
                    }
                    
                    // 动画结束后移除类名并保存
                    setTimeout(() => {
                        fab.classList.remove('snapping');
                        this.saveFabState();
                    }, 300);
                } else if (!hasMoved) {
                    // 单击打开面板
                    this.togglePanel();
                }
            };
            
            fab.addEventListener('mousedown', onStart);
            fab.addEventListener('touchstart', onStart, { passive: false });
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
                this.showToast(T.fabHidden);
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
            } catch (e) { /* localStorage unavailable */ }
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
            } catch (e) { /* localStorage unavailable */ }
        },

        // 注册模块 - 支持多种UI类型和运行模式
        registerModule(moduleInfo) {
            const existing = this.modules.findIndex(m => m.id === moduleInfo.id);
            let finalInfo;
            if (existing >= 0) {
                // Update现有模块
                this.removeModuleUI(this.modules[existing].id);
                this.modules[existing] = { ...this.modules[existing], ...moduleInfo };
                finalInfo = this.modules[existing];
            } else {
                this.modules.push(moduleInfo);
                finalInfo = moduleInfo;
            }
            
            const moduleRunMode = finalInfo.runMode || RUN_MODE.INTERACTIVE;
            const uiType = finalInfo.uiConfig?.type || UI_TYPE.FLOATING_BUTTON;
            
            // AUTO 模式：只注册到 FAB 列表，不创建独立 UI 元素
            // INTERACTIVE 模式：创建对应 UI 元素 + 注册到 FAB 列表
            if (moduleRunMode === RUN_MODE.INTERACTIVE) {
                switch (uiType) {
                    case UI_TYPE.FLOATING_TOOLBAR:
                        this.createToolbar(finalInfo);
                        break;
                    case UI_TYPE.SIDEBAR:
                        this.createSidebar(finalInfo);
                        break;
                    case UI_TYPE.BOTTOM_BAR:
                        this.createBottomBar(finalInfo);
                        break;
                    case UI_TYPE.FLOATING_PANEL:
                        this.createFloatingPanel(finalInfo);
                        break;
                    case UI_TYPE.MINI_BUTTON:
                        this.createMiniButton(finalInfo);
                        break;
                    case UI_TYPE.CUSTOM:
                        this.createCustomUI(finalInfo);
                        break;
                    case UI_TYPE.FLOATING_BUTTON:
                    default:
                        break;
                }
            }
            
            // 所有模块都注册到 FAB 列表
            this.updateModules();
            this.updateBadge();
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
            const draggable = config.draggable === true;
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
            
            // Do NOT auto-show: panel starts hidden, user opens from FAB module list
            // Only auto-show if user previously had it open (check sessionStorage)
            const storageKey = `wta-fpanel-visible-${"$"}{moduleInfo.id}`;
            const wasOpen = sessionStorage.getItem(storageKey) === '1';
            if (wasOpen) {
                setTimeout(() => panel.classList.add('visible'), 10);
            }
            
            this.uiContainers[moduleInfo.id] = panel;
            
            if (moduleInfo.onAction) {
                const content = document.getElementById(`wta-fpanel-content-${"$"}{moduleInfo.id}`);
                moduleInfo.onAction(content);
            }
            
            // Setup resize if resizable
            if (resizable) {
                const resizeHandle = panel.querySelector('.wta-floating-panel-resize');
                if (resizeHandle) {
                    DragManager.makeResizable(panel, resizeHandle, 200, 150);
                }
            }
        },
        
        showFloatingPanel(moduleId) {
            const panel = document.getElementById(`wta-fpanel-${"$"}{moduleId}`);
            if (panel) {
                panel.classList.add('visible');
                sessionStorage.setItem(`wta-fpanel-visible-${"$"}{moduleId}`, '1');
                // Re-trigger onAction to refresh content
                const module = this.modules.find(m => m.id === moduleId);
                if (module && module.onAction) {
                    const content = document.getElementById(`wta-fpanel-content-${"$"}{moduleId}`);
                    if (content) module.onAction(content);
                }
            }
        },
        
        hideFloatingPanel(moduleId) {
            const panel = document.getElementById(`wta-fpanel-${"$"}{moduleId}`);
            if (panel) {
                panel.classList.remove('visible');
                sessionStorage.setItem(`wta-fpanel-visible-${"$"}{moduleId}`, '0');
            }
        },
        
        toggleFloatingPanel(moduleId) {
            const panel = document.getElementById(`wta-fpanel-${"$"}{moduleId}`);
            if (panel && panel.classList.contains('visible')) {
                this.hideFloatingPanel(moduleId);
            } else {
                this.showFloatingPanel(moduleId);
            }
        },
        
        minimizeFloatingPanel(moduleId) {
            this.hideFloatingPanel(moduleId);
            this.showToast(T.panelMinimized);
        },
        
        updateFloatingPanelContent(moduleId, html) {
            const content = document.getElementById(`wta-fpanel-content-${"$"}{moduleId}`);
            if (content) {
                content.innerHTML = html;
            }
        },
        
        // ==================== 启动独立窗口 ====================
        launchModuleWindow(moduleId) {
            const module = this.modules.find(m => m.id === moduleId);
            if (!module) return;
            
            // 关闭 FAB 面板
            this.hidePanel();
            
            // 检查是否已有独立窗口
            const existingWin = document.getElementById(`wta-modwin-${"$"}{moduleId}`);
            if (existingWin) {
                existingWin.classList.add('visible');
                sessionStorage.setItem(`wta-modwin-visible-${"$"}{moduleId}`, '1');
                return;
            }
            
            // 创建独立窗口（基于浮动面板）
            const config = module.uiConfig || {};
            const width = config.panelWidth || 380;
            const height = config.panelHeight || 500;
            
            const win = document.createElement('div');
            win.id = `wta-modwin-${"$"}{moduleId}`;
            win.className = 'wta-floating-panel wta-pos-center';
            win.style.width = width + 'px';
            win.style.height = height + 'px';
            win.style.zIndex = '2147483644';
            
            win.innerHTML = `
                <div class="wta-floating-panel-header">
                    <span class="wta-floating-panel-title">${"$"}{module.icon || '📦'} ${"$"}{module.name || T.unnamed}</span>
                    <div class="wta-floating-panel-actions">
                        <div class="wta-floating-panel-btn" onclick="__WTA_PANEL__.minimizeModuleWindow('${"$"}{moduleId}')">−</div>
                        <div class="wta-floating-panel-btn" onclick="__WTA_PANEL__.closeModuleWindow('${"$"}{moduleId}')">×</div>
                    </div>
                </div>
                <div class="wta-floating-panel-content" id="wta-modwin-content-${"$"}{moduleId}" style="padding:16px;overflow-y:auto">
                </div>
                <div class="wta-floating-panel-resize"></div>
            `;
            
            document.body.appendChild(win);
            
            // 拖拽
            DragManager.makeDraggable(win, win.querySelector('.wta-floating-panel-header'));
            
            // 缩放
            const resizeHandle = win.querySelector('.wta-floating-panel-resize');
            if (resizeHandle) {
                DragManager.makeResizable(win, resizeHandle, 280, 200);
            }
            
            // 填充内容
            const contentEl = document.getElementById(`wta-modwin-content-${"$"}{moduleId}`);
            if (contentEl && module.onAction) {
                module.onAction(contentEl);
            } else if (contentEl && module.panelHtml) {
                contentEl.innerHTML = module.panelHtml;
            }
            
            // 显示窗口
            setTimeout(() => win.classList.add('visible'), 10);
            sessionStorage.setItem(`wta-modwin-visible-${"$"}{moduleId}`, '1');
        },
        
        closeModuleWindow(moduleId) {
            const win = document.getElementById(`wta-modwin-${"$"}{moduleId}`);
            if (win) {
                win.classList.remove('visible');
                sessionStorage.setItem(`wta-modwin-visible-${"$"}{moduleId}`, '0');
                setTimeout(() => win.remove(), 300);
            }
        },
        
        minimizeModuleWindow(moduleId) {
            const win = document.getElementById(`wta-modwin-${"$"}{moduleId}`);
            if (win) {
                win.classList.remove('visible');
                sessionStorage.setItem(`wta-modwin-visible-${"$"}{moduleId}`, '0');
            }
            this.showToast(T.panelMinimized);
        },
        
        // ==================== 创建迷你按钮 ====================
        createMiniButton(moduleInfo) {
            const config = moduleInfo.uiConfig || {};
            const position = config.position || UI_POSITION.BOTTOM_RIGHT;
            const draggable = config.draggable === true;
            
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
        
        // 获取过滤后的模块列表
        getFilteredModules() {
            var all = this.modules.slice(); // Show all registered modules in management panel
            var q = this.searchQuery.toLowerCase().trim();
            if (q) {
                all = all.filter(function(m) {
                    return (m.name || '').toLowerCase().indexOf(q) >= 0 ||
                           (m.description || '').toLowerCase().indexOf(q) >= 0 ||
                           (m.id || '').toLowerCase().indexOf(q) >= 0;
                });
            }
            return all;
        },
        
        // 获取模块图标HTML
        getModuleIcon(m) {
            if (m.iconHtml) return m.iconHtml;
            if (m.icon && m.icon.indexOf('<') === 0) return m.icon;
            // drawable: 前缀 — Android drawable 资源，在 WebView 中使用 SVG fallback
            if (m.icon && m.icon.indexOf('drawable:') === 0) {
                // 尝试查找对应的 asset 图片
                var resName = m.icon.substring(9);
                // 内置扩展图标映射到 assets
                var assetMap = {
                    'ic_ext_bewlycat': 'extensions/bewlycat/assets/icon-512.png'
                };
                if (assetMap[resName]) {
                    return '<img src="file:///android_asset/' + assetMap[resName] + '" width="20" height="20" style="border-radius:4px;object-fit:contain">';
                }
                // 无对应 asset，使用 Chrome 扩展默认图标
                return '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="4"/><path d="M21.17 8H12"/><path d="M3.95 6.06L8.54 14"/><path d="M10.88 21.94L15.46 14"/></svg>';
            }
            // asset: 前缀 — assets 目录下的图片
            if (m.icon && m.icon.indexOf('asset:') === 0) {
                var assetPath = m.icon.substring(6);
                return '<img src="file:///android_asset/' + assetPath + '" width="20" height="20" style="border-radius:4px;object-fit:contain">';
            }
            if (m.icon) return m.icon;
            var isChromeExt = m.sourceType === 'CHROME_EXTENSION';
            if (isChromeExt) {
                return '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="12" r="10"/><circle cx="12" cy="12" r="4"/><path d="M21.17 8H12"/><path d="M3.95 6.06L8.54 14"/><path d="M10.88 21.94L15.46 14"/></svg>';
            }
            return '<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M9 3v18"/><path d="M3 9h6"/></svg>';
        },
        
        // 获取类型标签HTML
        getTypeBadge(m) {
            var isChromeExt = m.sourceType === 'CHROME_EXTENSION';
            if (isChromeExt) return '<span class="wta-type-badge chrome">Chrome</span>';
            if (m.sourceType === 'GREASEMONKEY') return '<span class="wta-type-badge userscript">' + T.typeUserScript + '</span>';
            return '<span class="wta-type-badge module">' + T.tabModules + '</span>';
        },
        
        // 渲染单个模块行
        renderModuleItem(m) {
            var icon = this.getModuleIcon(m);
            var badge = this.getTypeBadge(m);
            var desc = m.description || T.noDescription;
            var moduleRunMode = m.runMode || RUN_MODE.INTERACTIVE;
            var runModeBadge = moduleRunMode === RUN_MODE.AUTO ?
                '<span class="wta-type-badge" style="background:#fef3c7;color:#92400e">⚡ ' + T.runModeAuto + '</span>' :
                '<span class="wta-type-badge" style="background:#dbeafe;color:#1e40af">🖥️ ' + T.runModeInteractive + '</span>';
            return '<div class="wta-ext-row" onclick="__WTA_PANEL__.onModuleClick(\'' + m.id + '\')">' +
                '<div class="wta-ext-row-icon">' + icon + '</div>' +
                '<div class="wta-ext-row-info">' +
                    '<div class="wta-ext-row-name">' + (m.name || T.unnamed) + '</div>' +
                    '<div class="wta-ext-row-desc">' + desc + '</div>' +
                    '<div class="wta-ext-row-meta">' + badge + runModeBadge + (m.version ? '<span class="wta-type-badge module">v' + m.version + '</span>' : '') + '</div>' +
                '</div>' +
                '<svg class="wta-ext-row-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 18l6-6-6-6"/></svg>' +
            '</div>';
        },
        
        // 切换标签页
        switchTab(tab) {
            this.activeTab = tab;
            var tabs = document.querySelectorAll('#wta-tabs .wta-tab');
            tabs.forEach(function(t) {
                t.classList.toggle('active', t.getAttribute('data-tab') === tab);
            });
            this.updateModules();
        },
        
        // 搜索模块
        searchModules(query) {
            this.searchQuery = query || '';
            this.updateModules();
        },
        
        // Update模块列表（分类列表视图）
        updateModules() {
            var container = document.getElementById('wta-module-grid');
            if (!container) return;
            
            var filtered = this.getFilteredModules();
            var chromeExts = filtered.filter(function(m) { return m.sourceType === 'CHROME_EXTENSION'; });
            var customModules = filtered.filter(function(m) { return m.sourceType !== 'CHROME_EXTENSION'; });
            
            // 更新标签页计数
            this.updateTabCounts(customModules.length, chromeExts.length);
            
            if (filtered.length === 0) {
                container.innerHTML = '<div class="wta-empty-state-v2">' +
                    '<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M12 8v8"/><path d="M8 12h8"/></svg>' +
                    '<p>' + T.noModulesAvailable + '</p></div>';
                return;
            }
            
            var html = '';
            var self = this;
            var showModules = (this.activeTab === 'all' || this.activeTab === 'modules');
            var showExts = (this.activeTab === 'all' || this.activeTab === 'extensions');
            
            if (showModules && customModules.length > 0) {
                html += '<div class="wta-section-header"><span class="wta-section-title">' + T.tabModules + '</span><span class="wta-section-count">' + customModules.length + '</span></div>';
                customModules.forEach(function(m) { html += self.renderModuleItem(m); });
            }
            
            if (showExts && chromeExts.length > 0) {
                html += '<div class="wta-section-header"><span class="wta-section-title">' + T.tabExtensions + '</span><span class="wta-section-count">' + chromeExts.length + '</span></div>';
                chromeExts.forEach(function(m) { html += self.renderModuleItem(m); });
            }
            
            if (!html) {
                container.innerHTML = '<div class="wta-empty-state-v2">' +
                    '<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><rect x="3" y="3" width="18" height="18" rx="3"/><path d="M12 8v8"/><path d="M8 12h8"/></svg>' +
                    '<p>' + T.noModulesAvailable + '</p></div>';
                return;
            }
            
            container.innerHTML = html;
        },
        
        // 更新标签页计数
        updateTabCounts(moduleCount, extCount) {
            var total = moduleCount + extCount;
            var elAll = document.getElementById('wta-count-all');
            var elMod = document.getElementById('wta-count-modules');
            var elExt = document.getElementById('wta-count-ext');
            if (elAll) elAll.textContent = total;
            if (elMod) elMod.textContent = moduleCount;
            if (elExt) elExt.textContent = extCount;
        },
        
        // Update徽章
        updateBadge() {
            var badge = document.querySelector('#wta-ext-fab .badge');
            var count = this.modules.length;
            if (badge) {
                badge.textContent = count;
                badge.style.display = count > 0 ? 'flex' : 'none';
            }
            
            var fab = document.getElementById('wta-ext-fab');
            if (fab && !this.isOpen) {
                fab.style.display = count > 0 ? 'flex' : 'none';
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
            var panel = document.getElementById('wta-ext-main-panel');
            var overlay = document.getElementById('wta-ext-overlay');
            var fab = document.getElementById('wta-ext-fab');
            
            panel.classList.remove('visible');
            overlay.classList.remove('visible');
            
            fab.style.display = this.modules.length > 0 ? 'flex' : 'none';
            
            this.isOpen = false;
            this.activeModuleId = null;
            this.searchQuery = '';
            var searchInput = document.getElementById('wta-search-input');
            if (searchInput) searchInput.value = '';
            
            // Hide模块详情
            this.hideModuleDetail();
        },

        // Module点击
        onModuleClick(moduleId) {
            var module = this.modules.find(function(m) { return m.id === moduleId; });
            if (!module) return;
            this.showModulePanel(moduleId);
        },

        // 构建模块详情HTML
        buildModuleDetailHTML(module) {
            var icon = this.getModuleIcon(module);
            var badge = this.getTypeBadge(module);
            var isChromeExt = module.sourceType === 'CHROME_EXTENSION';
            
            var html = '<div class="wta-detail-hero">' +
                '<div class="wta-detail-hero-icon">' + icon + '</div>' +
                '<div class="wta-detail-hero-info">' +
                    '<div class="wta-detail-hero-name">' + (module.name || T.unnamed) + '</div>' +
                    '<div class="wta-detail-hero-type">' + badge + '</div>' +
                '</div>' +
            '</div>';
            
            // 基本信息
            html += '<div class="wta-detail-section">' +
                '<div class="wta-detail-section-title">' + T.details + '</div>';
            
            if (module.description) {
                html += '<div class="wta-detail-row"><span class="wta-detail-value" style="text-align:left;width:100%">' + module.description + '</span></div>';
            }
            if (module.version) {
                html += '<div class="wta-detail-row"><span class="wta-detail-label">' + T.version + '</span><span class="wta-detail-value">' + module.version + '</span></div>';
            }
            if (module.author) {
                html += '<div class="wta-detail-row"><span class="wta-detail-label">' + T.author + '</span><span class="wta-detail-value">' + module.author + '</span></div>';
            }
            if (module.runAt) {
                html += '<div class="wta-detail-row"><span class="wta-detail-label">' + T.runAt + '</span><span class="wta-detail-value">' + (RUNAT_MAP[module.runAt] || module.runAt) + '</span></div>';
            }
            if (module.world) {
                html += '<div class="wta-detail-row"><span class="wta-detail-label">' + T.world + '</span><span class="wta-detail-value">' + (WORLD_MAP[module.world] || module.world) + '</span></div>';
            }
            
            // 运行方式
            var moduleRunMode = module.runMode || RUN_MODE.INTERACTIVE;
            var runModeText = moduleRunMode === RUN_MODE.AUTO ? ('⚡ ' + T.runModeAuto) : ('🖥️ ' + T.runModeInteractive);
            var runModeColor = moduleRunMode === RUN_MODE.AUTO ? '#92400e' : '#1e40af';
            html += '<div class="wta-detail-row"><span class="wta-detail-label">' + T.runModeInteractive.replace(T.runModeInteractive, (LANG === 'zh' ? '运行方式' : LANG === 'ar' ? 'وضع التشغيل' : 'Run Mode')) + '</span><span class="wta-detail-value" style="color:' + runModeColor + '">' + runModeText + '</span></div>';
            
            // 状态
            var statusText = module.active ? T.active : T.inactive;
            var statusColor = module.active ? 'var(--wta-success)' : 'var(--wta-on-surface-variant)';
            html += '<div class="wta-detail-row"><span class="wta-detail-label">' + (module.active ? T.matchesCurrentPage : T.notMatchCurrentPage) + '</span><span class="wta-detail-value" style="color:' + statusColor + '">' + statusText + '</span></div>';
            
            html += '</div>';
            
            // URL规则
            if (module.urlMatches && module.urlMatches.length > 0) {
                html += '<div class="wta-detail-section">' +
                    '<div class="wta-detail-section-title">' + T.urlRules + '</div>';
                module.urlMatches.forEach(function(rule) {
                    var ruleText = (typeof rule === 'string') ? rule : (rule.pattern || rule.url || JSON.stringify(rule));
                    html += '<div class="wta-detail-url-rule">' + ruleText + '</div>';
                });
                html += '</div>';
            }
            
            // 权限
            if (module.permissions && module.permissions.length > 0) {
                html += '<div class="wta-detail-section">' +
                    '<div class="wta-detail-section-title">' + T.permissions + '</div>' +
                    '<div style="display:flex;flex-wrap:wrap;gap:4px">';
                module.permissions.forEach(function(perm) {
                    var permRaw = (typeof perm === 'string') ? perm : (perm.name || JSON.stringify(perm));
                    var permText = PERM_MAP[permRaw] || permRaw;
                    html += '<span class="wta-detail-tag">' + permText + '</span>';
                });
                html += '</div></div>';
            }
            
            return html;
        },

        // Show模块详情面板
        showModulePanel(moduleId) {
            var module = this.modules.find(function(m) { return m.id === moduleId; });
            if (!module) return;
            
            this.activeModuleId = moduleId;
            
            var detail = document.getElementById('wta-module-detail');
            var title = document.getElementById('wta-detail-title');
            var content = document.getElementById('wta-detail-content');
            
            title.textContent = module.name || T.unnamed;
            
            // 构建详情HTML
            var detailHTML = this.buildModuleDetailHTML(module);
            var moduleRunMode = module.runMode || RUN_MODE.INTERACTIVE;
            
            if (moduleRunMode === RUN_MODE.AUTO) {
                // AUTO 模式：显示自动运行状态，无操作界面
                var autoStatusHtml = '<div class="wta-detail-section">' +
                    '<div style="text-align:center;padding:24px">' +
                    '<div style="font-size:48px;margin-bottom:12px">⚡</div>' +
                    '<div style="font-size:15px;font-weight:600;color:var(--wta-on-surface);margin-bottom:8px">' + T.autoRunning + '</div>' +
                    '<div style="font-size:13px;color:var(--wta-on-surface-variant)">' + T.autoRunningDesc + '</div>' +
                    '</div></div>';
                content.innerHTML = detailHTML + autoStatusHtml;
            } else {
                // INTERACTIVE 模式：显示启动窗口按钮 + 简单 UI 操作界面
                var launchBtnHtml = '<div class="wta-detail-section">' +
                    '<button onclick="__WTA_PANEL__.launchModuleWindow(\'' + moduleId + '\')" ' +
                    'style="width:100%;padding:14px;border-radius:12px;border:none;font-size:15px;font-weight:500;cursor:pointer;' +
                    'background:linear-gradient(135deg,var(--wta-primary),#8b5cf6);color:white;display:flex;align-items:center;justify-content:center;gap:8px;margin-bottom:12px">' +
                    '<span style="font-size:18px">\ud83d\udda5\ufe0f</span> ' + T.launchWindow + '</button></div>';
                
                // 简单 UI 操作界面（内联在管理面板中）
                if (module.panelHtml) {
                    content.innerHTML = detailHTML + launchBtnHtml + '<div class="wta-detail-section">' +
                        '<div class="wta-detail-section-title">' + T.quickActions + '</div>' +
                        module.panelHtml + '</div>';
                } else if (module.onAction) {
                    content.innerHTML = detailHTML + launchBtnHtml + '<div class="wta-detail-section">' +
                        '<div class="wta-detail-section-title">' + T.quickActions + '</div>' +
                        '<div id="wta-module-action-content"></div></div>';
                    var actionEl = document.getElementById('wta-module-action-content');
                    if (actionEl) module.onAction(actionEl);
                } else {
                    content.innerHTML = detailHTML + launchBtnHtml;
                }
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
        
        // Update模块面板内容（支持所有UI类型）
        updateModulePanelContent(moduleId, html) {
            // FLOATING_BUTTON 类型：主面板内容
            if (this.activeModuleId === moduleId) {
                const content = document.getElementById('wta-detail-content');
                if (content) {
                    content.innerHTML = html;
                    return;
                }
            }
            // 独立窗口内容
            const modwinContent = document.getElementById('wta-modwin-content-' + moduleId);
            if (modwinContent) {
                modwinContent.innerHTML = html;
                return;
            }
            // MINI_BUTTON 类型：弹出面板内容
            const miniContent = document.getElementById('wta-mini-popup-content-' + moduleId);
            if (miniContent) {
                miniContent.innerHTML = html;
                return;
            }
            // FLOATING_PANEL 类型
            const fpContent = document.getElementById('wta-fpanel-content-' + moduleId);
            if (fpContent) {
                fpContent.innerHTML = html;
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
    
    // 等待面板初始化完成（DOM 已创建）
    function waitForPanel(callback, maxWait = 10000) {
        const start = Date.now();
        const check = () => {
            if (window.__WTA_PANEL__ && window.__WTA_PANEL__._initialized) {
                callback(window.__WTA_PANEL__);
            } else if (Date.now() - start < maxWait) {
                setTimeout(check, 100);
            } else if (window.__WTA_PANEL__) {
                // Fallback: panel object exists but not initialized, try anyway
                callback(window.__WTA_PANEL__);
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
    
    // 运行模式常量
    const RUN_MODE = {
        INTERACTIVE: 'INTERACTIVE',  // 交互模式
        AUTO: 'AUTO'                 // 自动模式
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
        RUN_MODE: RUN_MODE,
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
