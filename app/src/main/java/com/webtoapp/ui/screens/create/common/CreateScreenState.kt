package com.webtoapp.ui.screens.create.common

data class CreateScreenSection(
    val key: String,
    val visible: Boolean = true,
)

data class CreateScreenState(
    val isBusy: Boolean = false,
    val phase: String = "",
    val errorMessage: String? = null,
)
