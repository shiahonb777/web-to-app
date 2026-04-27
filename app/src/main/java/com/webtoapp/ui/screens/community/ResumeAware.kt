package com.webtoapp.ui.screens.community

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Returns a key that increments every time the host screen resumes (ON_RESUME).
 * Use as LaunchedEffect key to auto-reload data on every navigation back.
 */
@Composable
fun rememberResumeKey(): Int {
    val lifecycleOwner = LocalLifecycleOwner.current
    var key by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) key++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return key
}
