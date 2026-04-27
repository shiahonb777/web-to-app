package com.webtoapp.ui.screens.community

import androidx.compose.ui.graphics.Color
import com.webtoapp.core.i18n.Strings

fun tagColor(tag: String): Color = when (tag) {
    "html" -> Color(0xFFE44D26)
    "css" -> Color(0xFF264DE4)
    "javascript", "typescript" -> Color(0xFFF7DF1E)
    "vue" -> Color(0xFF42B883)
    "react" -> Color(0xFF61DAFB)
    "angular" -> Color(0xFFDD0031)
    "svelte" -> Color(0xFFFF3E00)
    "nextjs" -> Color(0xFF888888)
    "nuxtjs" -> Color(0xFF00DC82)
    "nodejs" -> Color(0xFF339933)
    "python" -> Color(0xFF3776AB)
    "php" -> Color(0xFF777BB4)
    "go" -> Color(0xFF00ADD8)
    "webtoapp" -> Color(0xFF6C5CE7)
    "pwa" -> Color(0xFF5A0FC8)
    "responsive" -> Color(0xFF00BCD4)
    "animation" -> Color(0xFFFF5722)
    "game" -> Color(0xFFE91E63)
    "tool" -> Color(0xFF607D8B)
    "education" -> Color(0xFF4CAF50)
    // Chinese use_case tags
    Strings.tagNewsReading -> Color(0xFF2196F3)
    Strings.tagVideoPlayback -> Color(0xFFE91E63)
    Strings.tagSocialTool -> Color(0xFF9C27B0)
    Strings.tagOfficeEfficiency -> Color(0xFF3F51B5)
    Strings.tagLearningEducation -> Color(0xFF4CAF50)
    Strings.tagGameEntertainment -> Color(0xFFFF5722)
    Strings.tagLifeService -> Color(0xFF009688)
    Strings.tagShoppingCompare -> Color(0xFFFF9800)
    Strings.tagPrivacySecurity -> Color(0xFF795548)
    Strings.tagSystemTool -> Color(0xFF607D8B)
    // Chinese feature tags
    Strings.tagAdBlock -> Color(0xFFD32F2F)
    Strings.tagDarkMode -> Color(0xFF424242)
    Strings.tagOfflineUse -> Color(0xFF00897B)
    Strings.tagVideoDownload -> Color(0xFF1565C0)
    Strings.tagCustomIcon -> Color(0xFF6A1B9A)
    Strings.tagSplashScreen -> Color(0xFFF4511E)
    Strings.tagBackgroundRun -> Color(0xFF00838F)
    Strings.tagMultiLanguage -> Color(0xFF558B2F)
    Strings.tagUnpackWrap -> Color(0xFF37474F)
    Strings.tagModuleExtension -> Color(0xFF6C5CE7)
    else -> Color(0xFF9E9E9E)
}

fun formatTimeAgo(isoString: String?): String {
    if (isoString == null) return ""
    return try {
        // ── 1. Extract timezone offset from ISO string ──
        val tzRegex = Regex("([+-])(\\d{2}):(\\d{2})$")
        val tzMatch = tzRegex.find(isoString)
        val endsWithZ = isoString.endsWith("Z")

        val offsetMs = when {
            tzMatch != null -> {
                val sign = if (tzMatch.groupValues[1] == "+") 1 else -1
                val hh = tzMatch.groupValues[2].toInt()
                val mm = tzMatch.groupValues[3].toInt()
                sign * (hh * 3600_000L + mm * 60_000L)
            }
            endsWithZ -> 0L
            else -> 0L
        }

        // ── 2. Strip fractional seconds AND timezone suffix ──
        val cleaned = isoString
            .replace(Regex("\\.[0-9]+"), "")
            .replace(Regex("[+-]\\d{2}:\\d{2}$"), "")
            .replace(Regex("Z$"), "")

        // ── 3. Parse as local-of-offset, then convert to epoch millis ──
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val parsed = format.parse(cleaned) ?: return isoString
        val epochMs = parsed.time - offsetMs

        // ── 4. Relative time ──
        val now = System.currentTimeMillis()
        val diff = now - epochMs
        if (diff < 0) return Strings.timeJustNow
        val minutes = diff / 60000
        val hours = minutes / 60
        val days = hours / 24
        when {
            minutes < 1 -> Strings.timeJustNow
            minutes < 60 -> String.format(Strings.timeMinutesAgo, minutes)
            hours < 24 -> String.format(Strings.timeHoursAgo, hours)
            days < 7 -> String.format(Strings.timeDaysAgo, days)
            days < 30 -> String.format(Strings.timeWeeksAgo, days / 7)
            else -> String.format(Strings.timeMonthsAgo, days / 30)
        }
    } catch (_: Exception) { isoString }
}

fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return when {
        hours > 0 -> String.format(Strings.durationHourMinute, hours, minutes)
        minutes > 0 -> String.format(Strings.durationMinute, minutes)
        else -> Strings.durationLessThanMinute
    }
}
