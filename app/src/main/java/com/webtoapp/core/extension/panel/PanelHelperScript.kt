package com.webtoapp.core.extension.panel

/**
 * extension .
 */
object PanelHelperScript {
    val helperScript: String = """
(function() {
    'use strict';
    
    // etc.
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
    
    // UI.
    const UI_TYPE = {
        FLOATING_BUTTON: 'FLOATING_BUTTON',   // button.
        FLOATING_TOOLBAR: 'FLOATING_TOOLBAR', // Note.
        SIDEBAR: 'SIDEBAR',                   // Note.
        BOTTOM_BAR: 'BOTTOM_BAR',             // Note.
        FLOATING_PANEL: 'FLOATING_PANEL',     // Note.
        MINI_BUTTON: 'MINI_BUTTON',           // button.
        CUSTOM: 'CUSTOM'                      // CustomUI
    };
    
    const RUN_MODE = {
        INTERACTIVE: 'INTERACTIVE',  // interaction.
        AUTO: 'AUTO'                 // Note.
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
    
    // Module UI.
    window.__WTA_MODULE_UI__ = {
        // Export.
        UI_TYPE: UI_TYPE,
        RUN_MODE: RUN_MODE,
        UI_POSITION: UI_POSITION,
        
        /**
         * @param {Object} config config.
         * @param {string} config.id ID.
         * @param {string} config.name.
         * @param {string} config.icon emoji.
         * @param {string} config.panelHtml HTML.
         * @param {Function} config.onClick.
         * @param {Function} config.onAction.
         * @param {Object} config.uiConfig UIconfig.
         * @param {string} config.uiConfig.type UI.
         * @param {string} config.uiConfig.position.
         * @param {boolean} config.uiConfig.draggable is can.
         * @param {boolean} config.uiConfig.collapsible is can.
         * @param {string} config.uiConfig.toolbarOrientation 'HORIZONTAL'|'VERTICAL'.
         * @param {Array} config.uiConfig.toolbarItems.
         * @param {string} config.uiConfig.sidebarPosition 'LEFT'|'RIGHT'.
         * @param {number} config.uiConfig.sidebarWidth.
         * @param {number} config.uiConfig.panelWidth.
         * @param {number} config.uiConfig.panelHeight.
         * @param {boolean} config.uiConfig.resizable is can large small.
         * @param {boolean} config.uiConfig.showCloseButton by.
         * @param {boolean} config.uiConfig.showMinimizeButton small by.
         * @param {string} config.uiConfig.customHtml UI HTML.
         */
        register(config) {
            waitForPanel(panel => {
                panel.registerModule(config);
            });
        },
        
        /**
         * @param {string} id ID.
         * @param {string} icon (emoji)
         * @param {string} label.
         * @param {Function|string} action.
         * @param {Object} options.
         * @returns {Object} config.
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
         */
        updatePanel(moduleId, html) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateModulePanelContent(moduleId, html);
            }
        },
        
        /**
         */
        updateFloatingPanel(moduleId, html) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateFloatingPanelContent(moduleId, html);
            }
        },
        
        /**
         * UI.
         */
        updateCustomUI(moduleId, html) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateCustomUI(moduleId, html);
            }
        },
        
        /**
         * by.
         */
        updateMiniButtonBadge(moduleId, count) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.updateMiniButtonBadge(moduleId, count);
            }
        },
        
        /**
         * Sidebar visibility controls.
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
         * Floating panel visibility controls.
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
         * Bottom bar visibility control.
         */
        setBottomBarVisible(visible) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.setBottomBarVisible(visible);
            }
        },
        
        /**
         */
        toggleToolbarCollapse(moduleId) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.toggleToolbarCollapse(moduleId);
            }
        },
        
        /**
         * Toast.
         */
        toast(message, duration = 2000) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.showToast(message, duration);
            }
        },
        
        /**
         */
        closePanel() {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.hidePanel();
            }
        },
        
        /**
         */
        showPanel() {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.showPanel();
            }
        },
        
        /**
         */
        back() {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.hideModuleDetail();
            }
        },
        
        /**
         * FAB can.
         */
        setFabVisible(visible) {
            if (window.__WTA_PANEL__) {
                window.__WTA_PANEL__.setFabVisible(visible);
            }
        },
        
        /**
         * FAB.
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
