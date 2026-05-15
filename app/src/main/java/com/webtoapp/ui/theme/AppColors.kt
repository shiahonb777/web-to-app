package com.webtoapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Functional color tokens that cannot be expressed through [MaterialTheme.colorScheme]
 * or [com.webtoapp.ui.design.WtaColors.semantic]. Everything here is either:
 *  - A fixed, brand/language identity color (e.g. Kotlin yellow) or
 *  - A "product surface" color that must stay identical across light/dark
 *    (code editor, syntax highlighting backdrops).
 *
 * New code should prefer the Material3 color scheme or Wta semantic palette first and
 * only reach for AppColors when neither applies.
 */
object AppColors {

    /**
     * Legacy neutral-gray alias for sites that used to reference accent colors. Kept
     * so migrated screens keep compiling; prefer [MaterialTheme.colorScheme.primary]
     * or [com.webtoapp.ui.design.WtaColors.semantic] going forward.
     */
    val NeutralAccent: Color = Color(0xFF3A3A3A)

    // --- Status aliases (prefer WtaColors.semantic.* inside @Composable scope) ---
    val Success: Color = Color(0xFF2D6A3E)
    val Warning: Color = Color(0xFF8A5A00)
    val Error: Color = Color(0xFF8A1D1D)
    val Info: Color = Color(0xFF20486E)
    val Gray: Color = Color(0xFF9E9E9E)

    // --- Language / technology identity colors ---
    // These only matter in contexts that explicitly render tech-brand chips; in the
    // monochrome theme they collapse to the neutral accent so nothing stands out.
    val TypeScript: Color = NeutralAccent
    val JavaScript: Color = NeutralAccent
    val React: Color = NeutralAccent
    val Css: Color = NeutralAccent
    val WordPress: Color = NeutralAccent
    val Php: Color = NeutralAccent
    val Python: Color = NeutralAccent
    val SQLite: Color = NeutralAccent
    val NodeJs: Color = NeutralAccent
    val Go: Color = NeutralAccent
    val Svelte: Color = NeutralAccent
    val Laravel: Color = NeutralAccent
    val GoogleBlue: Color = NeutralAccent
    val UpdateBlue: Color = NeutralAccent
    val LightBlue: Color = NeutralAccent
    val Purple: Color = NeutralAccent
    val BrandOrange: Color = NeutralAccent
    val TelegramBlue: Color = NeutralAccent
    val BilibiliPink: Color = NeutralAccent
    val GitHubDark: Color = NeutralAccent
    val GmailRed: Color = NeutralAccent

    // --- Code editor palette (must stay constant regardless of theme) ---
    /** Editor chrome background (the main editor pane). */
    val EditorDark: Color = Color(0xFF1E1E1E)
    /** Editor secondary background (gutters, toolbars). */
    val EditorDarkAlt: Color = Color(0xFF2D2D2D)
    /** Default foreground text color inside the editor. */
    val CodeForeground: Color = Color(0xFFD4D4D4)
    /** Dimmed line numbers / inactive gutter text. */
    val CodeGutter: Color = Color(0xFF8C8C8C)
    /** Muted editor text (placeholders, trailing whitespace hints). */
    val CodeMuted: Color = Color(0xFF666666)
    /** Editor divider stroke color. */
    val CodeDivider: Color = Color(0xFF3A3A3A)

    // --- Catppuccin accents (used by the AI coding module's preset chromes) ---
    val CatppuccinBase: Color = Color(0xFF1E1E2E)
    val CatppuccinOverlay: Color = Color(0xFF6C7086)
    val CatppuccinSubtext: Color = Color(0xFF9CA0B0)
    val CatppuccinPeach: Color = Color(0xFFFAB387)
    val CatppuccinRed: Color = Color(0xFFF38BA8)
}
