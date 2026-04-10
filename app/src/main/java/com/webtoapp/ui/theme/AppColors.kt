package com.webtoapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 应用级通用颜色常量
 * 集中管理跨文件重复使用的硬编码颜色，提升一致性和可维护性
 */
object AppColors {
    
    // ==================== 状态颜色 ====================
    
    /** 成功/在线/通过 — Material Green 500 */
    val Success = Color(0xFF4CAF50)
    
    /** 错误/失败/危险 — Material Red 600 */
    val Error = Color(0xFFE53935)
    
    /** 警告/注意 — Material Orange 500 */
    val Warning = Color(0xFFFF9800)
    
    /** 信息/链接 — Material Blue 500 */
    val Info = Color(0xFF2196F3)
    
    // ==================== 编程语言/技术栈颜色 ====================
    
    /** TypeScript */
    val TypeScript = Color(0xFF3178C6)
    
    /** JavaScript — 黄色 */
    val JavaScript = Color(0xFFF7DF1E)
    
    /** React — 天蓝色 */
    val React = Color(0xFF61DAFB)
    
    /** Go — 青色 */
    val Go = Color(0xFF00ADD8)
    
    /** CSS — 蓝色 */
    val Css = Color(0xFF264DE4)
    
    /** WordPress — 深蓝 */
    val WordPress = Color(0xFF21759B)
    
    /** Google Blue */
    val GoogleBlue = Color(0xFF4285F4)
    
    /** Svelte — 橙红 */
    val Svelte = Color(0xFFFF3E00)
    
    /** Laravel — 红色 */
    val Laravel = Color(0xFFFF2D20)
    
    // ==================== 编辑器/代码颜色 ====================
    
    /** 编辑器深色背景 */
    val EditorDark = Color(0xFF1E1E1E)
    
    /** 编辑器次深色背景 */
    val EditorDarkAlt = Color(0xFF2D2D2D)
    
    /** Catppuccin 深色背景 */
    val CatppuccinBase = Color(0xFF1E1E2E)
    
    /** Catppuccin 次要文字 */
    val CatppuccinOverlay = Color(0xFF6C7086)
    
    /** Catppuccin 表面 */
    val CatppuccinSubtext = Color(0xFF9CA0B0)
    
    /** Catppuccin 橙色 */
    val CatppuccinPeach = Color(0xFFFAB387)
    
    /** Catppuccin 红色 */
    val CatppuccinRed = Color(0xFFF38BA8)
    
    // ==================== 通用 UI 颜色 ====================
    
    /** 中灰色 — 禁用/次要 */
    val Gray = Color(0xFF9E9E9E)
    
    /** 浅蓝色 — 次要信息 */
    val LightBlue = Color(0xFF4FC3F7)
    
    /** 紫色 — 高级/AI */
    val Purple = Color(0xFF8B5CF6)
    
    /** Firefox/品牌橙 */
    val BrandOrange = Color(0xFFFE640B)
    
    /** Telegram 蓝 */
    val TelegramBlue = Color(0xFF12B7F5)
}
