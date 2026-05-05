package com.webtoapp.util





object AppConstants {


    val SANITIZE_FILENAME_REGEX = Regex("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]")


    val PACKAGE_NAME_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")


    val CHARSET_REGEX = Regex("""charset=["']?([^"'\s>]+)""", RegexOption.IGNORE_CASE)


    fun sanitizeFileName(name: String): String =
        name.replace(SANITIZE_FILENAME_REGEX, "_").take(50)


    fun isValidPackageName(name: String): Boolean =
        PACKAGE_NAME_REGEX.matches(name)
}
