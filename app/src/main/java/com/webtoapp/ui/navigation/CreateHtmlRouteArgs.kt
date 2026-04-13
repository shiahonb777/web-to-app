package com.webtoapp.ui.navigation

import androidx.navigation.NavBackStackEntry

internal data class CreateHtmlRouteArgs(
    val importDir: String?,
    val projectName: String?,
) {
    companion object {
        fun from(backStackEntry: NavBackStackEntry): CreateHtmlRouteArgs {
            return CreateHtmlRouteArgs(
                importDir = backStackEntry.arguments?.getString("importDir").decodeUrlArg(),
                projectName = backStackEntry.arguments?.getString("projectName").decodeUrlArg(),
            )
        }
    }
}

private fun String?.decodeUrlArg(): String? {
    if (this == null) {
        return null
    }
    return try {
        java.net.URLDecoder.decode(this, "UTF-8")
    } catch (_: Exception) {
        null
    }
}
