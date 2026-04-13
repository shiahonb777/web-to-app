package com.webtoapp.core.extension.panel

/**
 * 扩展模块辅助脚本。
 */
object PanelHelperScript {
    val helperScript: String = """
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
"""
}
