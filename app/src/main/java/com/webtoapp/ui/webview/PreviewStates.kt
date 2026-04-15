package com.webtoapp.ui.webview

/**
 * WordPress previewstate
 */
sealed class WordPressPreviewState {
    object Idle : WordPressPreviewState()
    object CheckingDeps : WordPressPreviewState()
    object Downloading : WordPressPreviewState()
    object CreatingProject : WordPressPreviewState()
    object StartingServer : WordPressPreviewState()
    data class Ready(val url: String) : WordPressPreviewState()
    data class Error(val message: String) : WordPressPreviewState()
}

/**
 * PHP apppreviewstate
 */
sealed class PhpAppPreviewState {
    object Idle : PhpAppPreviewState()
    object CheckingDeps : PhpAppPreviewState()
    object Downloading : PhpAppPreviewState()
    object StartingServer : PhpAppPreviewState()
    data class Ready(val url: String) : PhpAppPreviewState()
    data class Error(val message: String) : PhpAppPreviewState()
}

/**
 * Python apppreviewstate
 */
sealed class PythonAppPreviewState {
    object Idle : PythonAppPreviewState()
    object Starting : PythonAppPreviewState()
    object InstallingDeps : PythonAppPreviewState()
    object StartingServer : PythonAppPreviewState()
    data class Ready(val url: String) : PythonAppPreviewState()
    data class Error(val message: String) : PythonAppPreviewState()
}

/**
 * Node. js apppreviewstate
 */
sealed class NodeJsAppPreviewState {
    object Idle : NodeJsAppPreviewState()
    object Starting : NodeJsAppPreviewState()
    data class Ready(val url: String) : NodeJsAppPreviewState()
    data class Error(val message: String) : NodeJsAppPreviewState()
}

/**
 * Go apppreviewstate
 */
sealed class GoAppPreviewState {
    object Idle : GoAppPreviewState()
    object Starting : GoAppPreviewState()
    object StartingServer : GoAppPreviewState()
    data class Ready(val url: String) : GoAppPreviewState()
    data class Error(val message: String) : GoAppPreviewState()
}

/**
 * file
 */
fun formatWpBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt().coerceAtMost(units.size - 1)
    return String.format(java.util.Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, exp.toDouble()), units[exp])
}
