package com.webtoapp.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

object SvgIconMapper {
    fun getIcon(iconId: String): ImageVector = when (iconId) {
        "package", "📦"       -> Icons.Outlined.Inventory2
        "folder", "📁", "📂"  -> Icons.Outlined.FolderOpen
        "file", "📄"          -> Icons.Outlined.InsertDriveFile
        "clipboard", "📋"     -> Icons.Outlined.Assignment
        "search", "🔍", "🔎"  -> Icons.Outlined.Search
        "settings", "⚙️"     -> Icons.Outlined.Settings

        "code", "📝"          -> Icons.Outlined.Code
        "book", "📖", "📚"   -> Icons.Outlined.MenuBook
        "wrench", "🔧"        -> Icons.Outlined.Build
        "palette", "🎨"       -> Icons.Outlined.Palette
        "brush"               -> Icons.Outlined.Brush
        "edit", "✏️"          -> Icons.Outlined.Edit
        "edit_note"           -> Icons.Outlined.EditNote
        "keyboard", "⌨️"     -> Icons.Outlined.Keyboard
        "mouse", "🖱️"        -> Icons.Outlined.Mouse

        "globe", "🌐"         -> Icons.Outlined.Language
        "antenna", "📡"       -> Icons.Outlined.SettingsInputAntenna
        "wifi"                -> Icons.Outlined.Wifi
        "cloud"               -> Icons.Outlined.Cloud
        "link", "🔗"          -> Icons.Outlined.Link

        "shield", "🛡️"       -> Icons.Outlined.Shield
        "lock", "🔒"          -> Icons.Outlined.Lock
        "key"                 -> Icons.Outlined.Key
        "warning", "⚠️"      -> Icons.Outlined.Warning
        "block", "🚫"         -> Icons.Outlined.Block

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

        "priority_high", "🔴" -> Icons.Filled.Circle
        "priority_medium", "🟡" -> Icons.Filled.Circle
        "priority_low", "🟢"  -> Icons.Filled.Circle
        "error", "❌"         -> Icons.Outlined.Error
        "check", "✅"         -> Icons.Outlined.CheckCircle

        "play", "▶"           -> Icons.Filled.PlayArrow
        "pause", "⏸"         -> Icons.Filled.Pause
        "music", "music_note", "🎵" -> Icons.Outlined.MusicNote
        "camera", "📷", "📸" -> Icons.Outlined.CameraAlt
        "movie", "🎬"         -> Icons.Outlined.Movie
        "image", "🖼️"        -> Icons.Outlined.Image
        "volume", "🔊"        -> Icons.Outlined.VolumeUp

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

        "star", "⭐", "🌟"   -> Icons.Outlined.Star
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

        "python", "🐍"        -> Icons.Outlined.Code
        "php", "🐘"           -> Icons.Outlined.Storage
        "golang", "🔷"        -> Icons.Outlined.Hexagon
        "nodejs", "node"      -> Icons.Outlined.Javascript
        "html"                -> Icons.Outlined.Html

        "leaf", "🍃"          -> Icons.Outlined.Spa
        "flower", "🌸", "🌱"  -> Icons.Outlined.LocalFlorist
        "nature"              -> Icons.Outlined.Nature

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
        "add_circle", "➕"   -> Icons.Outlined.AddCircle
        "home", "🏠"          -> Icons.Outlined.Home
        "fast_forward", "⏩" -> Icons.Outlined.FastForward
        "phone_android", "📱" -> Icons.Outlined.PhoneAndroid
        "computer", "💻"     -> Icons.Outlined.Computer
        "trophy", "🏆"       -> Icons.Outlined.EmojiEvents
        "festival", "🎪"    -> Icons.Outlined.Celebration
        "list", "📜"          -> Icons.Outlined.List
        "build", "🩹"        -> Icons.Outlined.Build

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
        else                  -> Icons.Outlined.HelpOutline
    }

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
        else                  -> getIcon(iconId)
    }

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
        "🔘" -> "radio_button"
        "🖱️" -> "mouse"
        "🎀" -> "ribbon"
        "🎊" -> "celebration"
        "🛠️" -> "build"
        "📢" -> "campaign"
        else -> emojiOrId
    }

    fun isEmoji(value: String): Boolean {
        return value.isNotBlank() && value.any { it.code > 127 }
    }
}
