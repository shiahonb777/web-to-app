package com.webtoapp.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.webtoapp.core.extension.ExtensionManager
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.LanguageManager
import com.webtoapp.core.i18n.Strings
import org.koin.compose.koinInject

@Composable
fun InitializeLanguage() {
    val context = LocalContext.current
    val languageManager: LanguageManager = koinInject()
    val extensionManager: ExtensionManager = koinInject()
    val language by languageManager.currentLanguageFlow.collectAsState(initial = AppLanguage.CHINESE)

    LaunchedEffect(language) {
        Strings.attachContext(context, language)
        Strings.setLanguage(language)
        try {
            extensionManager.reloadBuiltInModules()
        } catch (_: Exception) {
            // Ignore if ExtensionManager is not ready yet.
        }
    }
}
