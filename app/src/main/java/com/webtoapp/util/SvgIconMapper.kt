package com.webtoapp.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * SVG Icon Mapper — 将文本标识 / 旧版 emoji 映射到 Material Icons (Vector Drawable)
 *
 * 设计原则：Apple HIG 级别的图标规范，所有图标统一使用 Material Symbols，
 * 彻底杜绝 emoji 在 UI 层的使用。
 *
 * 兼容性：保留对旧版 emoji 字符串的反向映射，确保持久化数据无缝迁移。
 */
object SvgIconMapper {

    /**
     * 获取 Outlined 风格图标（默认）
     */
    fun getIcon(iconId: String): ImageVector = when (iconId) {
        // ── 通用 ──
        "package", "📦"       -> Icons.Outlined.Inventory2
        "folder", "📁", "📂"  -> Icons.Outlined.FolderOpen
        "file"                -> Icons.Outlined.InsertDriveFile
        "clipboard", "📋"     -> Icons.Outlined.Assignment
        "search", "🔍", "🔎"  -> Icons.Outlined.Search
        "settings"            -> Icons.Outlined.Settings

        // ── 编辑 & 工具 ──
        "code", "📝"          -> Icons.Outlined.Code
        "book", "📖"          -> Icons.Outlined.MenuBook
        "wrench", "🔧"        -> Icons.Outlined.Build
        "palette", "🎨"       -> Icons.Outlined.Palette
        "brush"               -> Icons.Outlined.Brush
        "edit", "✏️"          -> Icons.Outlined.Edit
        "edit_note"           -> Icons.Outlined.EditNote
        "keyboard", "⌨️"     -> Icons.Outlined.Keyboard
        "mouse", "🖱️"        -> Icons.Outlined.Mouse

        // ── 网络 & 语言 ──
        "globe", "🌐"         -> Icons.Outlined.Language
        "antenna", "📡"       -> Icons.Outlined.SettingsInputAntenna
        "wifi"                -> Icons.Outlined.Wifi
        "cloud"               -> Icons.Outlined.Cloud
        "link", "🔗"          -> Icons.Outlined.Link

        // ── 安全 ──
        "shield", "🛡️"       -> Icons.Outlined.Shield
        "lock", "🔒"          -> Icons.Outlined.Lock
        "key"                 -> Icons.Outlined.Key
        "warning", "⚠️"      -> Icons.Outlined.Warning
        "block", "🚫"         -> Icons.Outlined.Block

        // ── AI & 扩展 ──
        "robot", "🤖"         -> Icons.Outlined.SmartToy
        "smart_toy"           -> Icons.Outlined.SmartToy
        "extension", "🧩"    -> Icons.Outlined.Extension
        "person", "👤"        -> Icons.Outlined.Person
        "person_search", "🕵️" -> Icons.Outlined.PersonSearch
        "thinking", "🤔"      -> Icons.Outlined.Psychology
        "lightbulb", "💡"     -> Icons.Outlined.Lightbulb
        "auto_fix"            -> Icons.Outlined.AutoFixHigh
        "puzzle"              -> Icons.Outlined.Extension
        "monkey", "🐵"        -> Icons.Outlined.Code
        "cat", "🐱"           -> Icons.Outlined.Pets
        "help", "❓"          -> Icons.Outlined.Help

        // ── 状态 & 优先级 ──
        "priority_high", "🔴" -> Icons.Filled.Circle
        "priority_medium", "🟡" -> Icons.Filled.Circle
        "priority_low", "🟢"  -> Icons.Filled.Circle
        "error"               -> Icons.Outlined.Error
        "check", "✅"         -> Icons.Outlined.CheckCircle

        // ── 媒体 ──
        "play", "▶"           -> Icons.Filled.PlayArrow
        "pause", "⏸"         -> Icons.Filled.Pause
        "music", "music_note", "🎵" -> Icons.Outlined.MusicNote
        "camera", "📷"        -> Icons.Outlined.CameraAlt
        "movie", "🎬"         -> Icons.Outlined.Movie
        "image", "🖼️"        -> Icons.Outlined.Image
        "volume", "🔊"        -> Icons.Outlined.VolumeUp

        // ── UI & 布局 ──
        "dark_mode", "🌙"     -> Icons.Outlined.DarkMode
        "font_download", "🔤" -> Icons.Outlined.FontDownload
        "theater_comedy", "🎭" -> Icons.Outlined.TheaterComedy
        "straighten", "📐"    -> Icons.Outlined.Straighten
        "radio_button", "🔘"  -> Icons.Outlined.RadioButtonChecked
        "refresh", "🔄"       -> Icons.Outlined.Refresh
        "arrow_upward", "⬆️"  -> Icons.Outlined.ArrowUpward
        "analytics", "📊"     -> Icons.Outlined.Analytics
        "notifications_off", "🔕" -> Icons.Outlined.NotificationsOff
        "cookie", "🍪"        -> Icons.Outlined.Cookie

        // ── 品质 & 徽章 ──
        "star", "⭐"          -> Icons.Outlined.Star
        "heart", "❤️"         -> Icons.Outlined.Favorite
        "fire", "🔥"          -> Icons.Outlined.LocalFireDepartment
        "diamond", "💎"       -> Icons.Outlined.Diamond
        "rocket", "🚀"        -> Icons.Outlined.RocketLaunch
        "target", "🎯"        -> Icons.Outlined.GpsFixed
        "bolt", "⚡"          -> Icons.Outlined.FlashOn
        "gaming", "🎮"        -> Icons.Outlined.SportsEsports
        "celebration", "🎉"   -> Icons.Outlined.Celebration
        "gift", "🎁"          -> Icons.Outlined.CardGiftcard
        "rainbow", "🌈"       -> Icons.Outlined.FilterDrama
        "science", "🧪"       -> Icons.Outlined.Science
        "eco", "🌿"           -> Icons.Outlined.Spa
        "cocktail", "🍸"      -> Icons.Outlined.LocalBar

        // ── 编程语言 ──
        "python", "🐍"        -> Icons.Outlined.Code
        "php", "🐘"           -> Icons.Outlined.Storage
        "golang", "🔷"        -> Icons.Outlined.Hexagon
        "nodejs", "node"      -> Icons.Outlined.Javascript
        "html"                -> Icons.Outlined.Html

        // ── 自然 & 装饰 ──
        "leaf", "🍃"          -> Icons.Outlined.Spa
        "flower", "🌸", "🌱"  -> Icons.Outlined.LocalFlorist
        "nature"              -> Icons.Outlined.Nature

        // ── 其他 ──
        "chat", "💬"          -> Icons.Outlined.Chat
        "info"                -> Icons.Outlined.Info
        "download"            -> Icons.Outlined.Download
        "share"               -> Icons.Outlined.Share
        "auto_awesome", "✨"  -> Icons.Outlined.AutoAwesome
        "explore", "🧭"      -> Icons.Outlined.Explore
        "save", "💾"          -> Icons.Outlined.Save
        "accessibility", "♿" -> Icons.Outlined.Accessibility
        "videocam", "📹"     -> Icons.Outlined.Videocam
        "shopping_cart", "🛒" -> Icons.Outlined.ShoppingCart
        "desktop_windows"     -> Icons.Outlined.DesktopWindows
        "visibility"          -> Icons.Outlined.Visibility
        "attachment", "📎"    -> Icons.Outlined.AttachFile
        "pause", "⏸️"         -> Icons.Outlined.Pause
        "thinking", "🤔"     -> Icons.Outlined.Psychology
        "check", "✅"         -> Icons.Outlined.Check
        "error", "❌"         -> Icons.Outlined.Error
        "add_circle", "➕"   -> Icons.Outlined.AddCircle
        "file", "📄"          -> Icons.Outlined.InsertDriveFile
        "folder", "📁"        -> Icons.Outlined.Folder
        "book", "📚"          -> Icons.Outlined.MenuBook
        "help", "❓"           -> Icons.Outlined.HelpOutline
        "warning", "⚠️"      -> Icons.Outlined.Warning
        "heart", "❤️"         -> Icons.Outlined.Favorite
        "home", "🏠"          -> Icons.Outlined.Home
        "camera", "📷", "📸" -> Icons.Outlined.CameraAlt
        "movie", "🎬"         -> Icons.Outlined.Movie
        "lightbulb", "💡"    -> Icons.Outlined.Lightbulb
        "fast_forward", "⏩" -> Icons.Outlined.FastForward
        "shield", "🛡️"      -> Icons.Outlined.Shield
        "target", "🎯"       -> Icons.Outlined.GpsFixed
        "gaming", "🎮"       -> Icons.Outlined.SportsEsports
        "phone_android", "📱" -> Icons.Outlined.PhoneAndroid
        "computer", "💻"     -> Icons.Outlined.Computer
        "star", "⭐", "🌟"  -> Icons.Outlined.Star
        "fire", "🔥"          -> Icons.Outlined.Whatshot
        "diamond", "💎"      -> Icons.Outlined.Diamond
        "gift", "🎁"          -> Icons.Outlined.CardGiftcard
        "trophy", "🏆"       -> Icons.Outlined.EmojiEvents
        "festival", "🎪"    -> Icons.Outlined.Celebration
        "mouse", "🖱️"        -> Icons.Outlined.Mouse
        "list", "📜"          -> Icons.Outlined.List
        "settings", "⚙️"    -> Icons.Outlined.Settings
        "html", "🌐"          -> Icons.Outlined.Language
        "golang", "🔷"       -> Icons.Outlined.Code
        "python", "🐍"       -> Icons.Outlined.Code
        "clipboard", "📋"    -> Icons.Outlined.Assignment
        "edit_note", "📝"    -> Icons.Outlined.EditNote
        "build", "🩹"        -> Icons.Outlined.Build

        // ── 新增映射 ──
        "tv", "📺"            -> Icons.Outlined.Tv
        "menu_book", "📕"     -> Icons.Outlined.MenuBook
        "check_circle"        -> Icons.Outlined.CheckCircle
        "folder_open"         -> Icons.Outlined.FolderOpen
        "newspaper", "📰"     -> Icons.Outlined.Newspaper
        "work", "💼"          -> Icons.Outlined.Work
        "shopping_bag", "🛍️" -> Icons.Outlined.ShoppingBag
        "directions_car", "🚗" -> Icons.Outlined.DirectionsCar
        "flight", "✈️"       -> Icons.Outlined.Flight
        "directions_boat", "🚢" -> Icons.Outlined.DirectionsBoat
        "public", "🌍"       -> Icons.Outlined.Public
        "light_mode", "☀️"   -> Icons.Outlined.LightMode
        "content_copy"        -> Icons.Outlined.ContentCopy
        "translate"           -> Icons.Outlined.Translate
        "picture_in_picture"  -> Icons.Outlined.PictureInPicture
        "repeat", "🔁"       -> Icons.Outlined.Repeat
        "block"               -> Icons.Outlined.Block
        "radio_button"        -> Icons.Outlined.RadioButtonChecked
        "bolt"                -> Icons.Outlined.Bolt
        "package"             -> Icons.Outlined.Inventory2

        // ── 默认 ──
        else                  -> Icons.Outlined.HelpOutline
    }

    /**
     * 获取 Filled 风格图标
     */
    fun getFilledIcon(iconId: String): ImageVector = when (iconId) {
        "package", "📦"       -> Icons.Filled.Inventory2
        "folder", "📁", "📂"  -> Icons.Filled.FolderOpen
        "person", "👤"        -> Icons.Filled.Person
        "robot", "🤖"         -> Icons.Filled.SmartToy
        "thinking", "🤔"      -> Icons.Filled.Psychology
        "puzzle", "🧩"        -> Icons.Filled.Extension
        "star", "⭐"          -> Icons.Filled.Star
        "heart", "❤️"         -> Icons.Filled.Favorite
        "warning", "⚠️"      -> Icons.Filled.Warning
        "play", "▶"           -> Icons.Filled.PlayArrow
        "pause", "⏸"         -> Icons.Filled.Pause
        else                  -> getIcon(iconId) // fallback to outlined
    }

    /**
     * 将旧版 emoji 字符串转换为规范化的 icon ID
     * 用于数据迁移场景
     */
    fun normalizeIconId(emojiOrId: String): String = when (emojiOrId) {
        "📦" -> "package"
        "📁", "📂" -> "folder"
        "📝" -> "code"
        "📖" -> "book"
        "🎨" -> "palette"
        "📋" -> "clipboard"
        "🔍" -> "search"
        "🔧" -> "wrench"
        "🌐" -> "globe"
        "🛡️" -> "shield"
        "🔒" -> "lock"
        "🤖" -> "robot"
        "👤" -> "person"
        "🤔" -> "thinking"
        "💡" -> "lightbulb"
        "🧩" -> "puzzle"
        "🐵" -> "monkey"
        "⚠️" -> "warning"
        "🚫" -> "block"
        "🔴" -> "priority_high"
        "🟡" -> "priority_medium"
        "🟢" -> "priority_low"
        "▶" -> "play"
        "⏸" -> "pause"
        "🎵" -> "music"
        "📷" -> "camera"
        "🎬" -> "movie"
        "⭐" -> "star"
        "❤️" -> "heart"
        "🔥" -> "fire"
        "💎" -> "diamond"
        "🚀" -> "rocket"
        "🎯" -> "target"
        "⚡" -> "bolt"
        "🎮" -> "gaming"
        "🐍" -> "python"
        "🐘" -> "php"
        "🔷" -> "golang"
        "📡" -> "antenna"
        "🎉" -> "celebration"
        "🎁" -> "gift"
        "💬" -> "chat"
        "❓" -> "help"
        "🌿", "🍃" -> "leaf"
        "🌸", "🌱" -> "flower"
        "📺" -> "tv"
        "📕" -> "menu_book"
        "⬇️" -> "download"
        "📰" -> "newspaper"
        "💼" -> "work"
        "🛍️" -> "shopping_bag"
        "🚗" -> "directions_car"
        "✈️" -> "flight"
        "🚢" -> "directions_boat"
        "🌍" -> "public"
        "☀️" -> "light_mode"
        "🌙" -> "dark_mode"
        "📊" -> "analytics"
        "📸" -> "camera"
        "🏠" -> "home"
        "📱" -> "phone_android"
        "💻" -> "computer"
        "✨" -> "auto_awesome"
        "🔁" -> "repeat"
        "⏩" -> "fast_forward"
        "🚫" -> "block"
        "🔘" -> "radio_button"
        "🖱️" -> "mouse"
        "🎀" -> "ribbon"
        "🎊" -> "celebration"
        "🛡️" -> "shield"
        "📋" -> "clipboard"
        "📝" -> "edit_note"
        "🛠️" -> "build"
        "📦" -> "package"
        "🔒" -> "lock"
        "🧩" -> "extension"
        "🤖" -> "smart_toy"
        "📢" -> "campaign"
        else -> emojiOrId // 已经是规范 ID 或无法识别的，原样返回
    }

    /**
     * 判断字符串是否为 emoji（非 ASCII）
     */
    fun isEmoji(value: String): Boolean {
        return value.isNotBlank() && value.any { it.code > 127 }
    }
}
