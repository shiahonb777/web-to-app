package com.webtoapp.core.apkbuilder

import com.webtoapp.util.AppConstants

internal object ApkBuildNaming {

    private val sanitizeFilenameRegex = AppConstants.SANITIZE_FILENAME_REGEX

    fun generatePackageName(appName: String): String {
        val raw = appName.hashCode().let {
            if (it < 0) (-it).toString(36) else it.toString(36)
        }.take(4).padStart(4, '0')

        return "com.w2a.${normalizePackageSegment(raw)}"
    }

    fun sanitizeFileName(name: String): String {
        return name.replace(sanitizeFilenameRegex, "_").take(50)
    }

    internal fun normalizePackageSegment(segment: String): String {
        if (segment.isEmpty()) return "a"

        val chars = segment.lowercase().toCharArray()
        chars[0] = when {
            chars[0] in 'a'..'z' -> chars[0]
            chars[0] in '0'..'9' -> ('a' + (chars[0] - '0'))
            else -> 'a'
        }
        return String(chars)
    }
}
