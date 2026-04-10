package com.webtoapp.core.pwa

/**
 * PWA Web App Manifest 数据模型
 * 参考: https://developer.mozilla.org/en-US/docs/Web/Manifest
 */
data class PwaManifest(
    val name: String? = null,
    val shortName: String? = null,
    val startUrl: String? = null,
    val scope: String? = null,
    val display: String? = null,           // standalone, fullscreen, minimal-ui, browser
    val themeColor: String? = null,        // #RRGGBB
    val backgroundColor: String? = null,
    val icons: List<PwaIcon> = emptyList(),
    val orientation: String? = null,       // portrait, landscape, any
    val description: String? = null,
    val lang: String? = null,
    val dir: String? = null                // ltr, rtl, auto
)

/**
 * PWA 图标
 */
data class PwaIcon(
    val src: String,
    val sizes: String? = null,     // "192x192", "512x512"
    val type: String? = null,      // "image/png"
    val purpose: String? = null    // "any", "maskable", "monochrome"
) {
    /**
     * 获取最大尺寸的像素值
     * 例如 "192x192" → 192, "48x48 96x96" → 96
     */
    val maxSizePixels: Int
        get() {
            if (sizes.isNullOrBlank() || sizes == "any") return Int.MAX_VALUE
            return sizes.split(" ")
                .mapNotNull { sizeStr ->
                    sizeStr.split("x", "X").firstOrNull()?.toIntOrNull()
                }
                .maxOrNull() ?: 0
        }
}

/**
 * PWA 分析结果
 */
data class PwaAnalysisResult(
    /** 是否是 PWA 站点（有 manifest.json 或 meta 标签） */
    val isPwa: Boolean = false,
    /** 建议的 App 名称 */
    val suggestedName: String? = null,
    /** 建议的图标 URL（完整 URL） */
    val suggestedIconUrl: String? = null,
    /** 建议的主题色 (HEX, 如 #FF5722) */
    val suggestedThemeColor: String? = null,
    /** 建议的背景色 (HEX) */
    val suggestedBackgroundColor: String? = null,
    /** 建议的显示模式 */
    val suggestedDisplay: String? = null,
    /** 建议的屏幕方向 */
    val suggestedOrientation: String? = null,
    /** PWA 的 start_url */
    val startUrl: String? = null,
    /** PWA 的 scope（用于推断 Deep Link 域名） */
    val scope: String? = null,
    /** 原始 manifest 数据 */
    val manifest: PwaManifest? = null,
    /** 数据来源 */
    val source: PwaDataSource = PwaDataSource.NONE,
    /** 分析错误信息（如有） */
    val errorMessage: String? = null
)

/**
 * PWA 数据来源
 */
enum class PwaDataSource {
    MANIFEST,   // 来自 manifest.json
    META_TAGS,  // 来自 HTML meta 标签
    NONE        // 无 PWA 数据
}

/**
 * PWA 分析状态（UI 使用）
 */
sealed class PwaAnalysisState {
    data object Idle : PwaAnalysisState()
    data object Analyzing : PwaAnalysisState()
    data class Success(val result: PwaAnalysisResult) : PwaAnalysisState()
    data class Error(val message: String) : PwaAnalysisState()
}
