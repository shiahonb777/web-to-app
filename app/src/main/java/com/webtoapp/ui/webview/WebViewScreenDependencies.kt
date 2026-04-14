package com.webtoapp.ui.webview

import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager
import com.webtoapp.core.webview.LocalHttpServer
import com.webtoapp.data.repository.WebAppRepository

data class WebViewScreenDependencies(
    val repository: WebAppRepository,
    val activation: ActivationManager,
    val announcement: AnnouncementManager,
    val adBlocker: AdBlocker,
    val localHttpServer: LocalHttpServer,
)
