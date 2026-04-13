package com.webtoapp.ui.screens.create.webview.cards.interaction

import androidx.compose.runtime.Composable
import com.webtoapp.core.errorpage.ErrorPageConfig
import com.webtoapp.data.model.LongPressMenuStyle
import com.webtoapp.ui.screens.create.webview.cards.system.AdBlockCard as SystemAdBlockCard
import com.webtoapp.ui.screens.create.webview.cards.system.ErrorPageConfigCard as SystemErrorPageConfigCard
import com.webtoapp.ui.screens.create.webview.cards.system.LongPressMenuCard as SystemLongPressMenuCard
import com.webtoapp.ui.viewmodel.EditState

@Composable
fun LongPressMenuCard(
    style: LongPressMenuStyle,
    onStyleChange: (LongPressMenuStyle) -> Unit
) {
    SystemLongPressMenuCard(style = style, onStyleChange = onStyleChange)
}

@Composable
fun AdBlockCard(
    editState: EditState,
    onEnabledChange: (Boolean) -> Unit,
    onRulesChange: (List<String>) -> Unit,
    onToggleEnabledChange: (Boolean) -> Unit = {}
) {
    SystemAdBlockCard(
        editState = editState,
        onEnabledChange = onEnabledChange,
        onRulesChange = onRulesChange,
        onToggleEnabledChange = onToggleEnabledChange
    )
}

@Composable
fun ErrorPageConfigCard(
    config: ErrorPageConfig,
    onConfigChange: (ErrorPageConfig) -> Unit
) {
    SystemErrorPageConfigCard(config = config, onConfigChange = onConfigChange)
}
