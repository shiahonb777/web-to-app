package com.webtoapp.util

/**
 * Shared constants — eliminates duplicate regex/constant definitions
 * across ApkBuilder, AppCloner, CreateAppScreen, etc.
 */
object AppConstants {

    /** File name sanitization: keep alphanumeric, underscore, hyphen, CJK chars */
    val SANITIZE_FILENAME_REGEX = Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]")

    /** Android package name validation */
    val PACKAGE_NAME_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

    /** HTML charset detection */
    val CHARSET_REGEX = Regex("""charset=["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE)

    /** Sanitize a file name: replace illegal chars with underscore, truncate to 50 chars */
    fun sanitizeFileName(name: String): String =
        name.replace(SANITIZE_FILENAME_REGEX, "_").take(50)

    /** Validate an Android package name */
    fun isValidPackageName(name: String): Boolean =
        PACKAGE_NAME_REGEX.matches(name)
}
