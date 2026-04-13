package com.webtoapp.ui.viewmodel.main

import android.net.Uri
import com.webtoapp.core.disguise.DeviceDisguiseConfig
import com.webtoapp.data.model.*
import com.webtoapp.ui.viewmodel.EditState

class StateFactory {
    fun newState(): EditState = EditState()

    fun fromWebApp(webApp: WebApp): EditState {
        return EditState(
            name = webApp.name,
            url = webApp.url,
            iconUri = webApp.iconPath?.let { Uri.parse(it) },
            savedIconPath = webApp.iconPath,
            appType = webApp.appType,
            mediaConfig = webApp.mediaConfig,
            htmlConfig = webApp.htmlConfig,
            allowHttp = webApp.allowHttp,
            activationEnabled = webApp.activationEnabled,
            activationCodes = webApp.activationCodes,
            activationCodeList = webApp.activationCodeList,
            activationRequireEveryTime = webApp.activationRequireEveryTime,
            activationDialogConfig = webApp.activationDialogConfig ?: ActivationDialogConfig(),
            adsEnabled = webApp.adsEnabled,
            adConfig = webApp.adConfig ?: AdConfig(),
            announcementEnabled = webApp.announcementEnabled,
            announcement = webApp.announcement ?: Announcement(),
            adBlockEnabled = webApp.adBlockEnabled,
            adBlockRules = webApp.adBlockRules,
            webViewConfig = webApp.webViewConfig,
            splashEnabled = webApp.splashEnabled,
            splashConfig = webApp.splashConfig ?: SplashConfig(),
            splashMediaUri = webApp.splashConfig?.mediaPath?.let { Uri.parse(it) },
            savedSplashPath = webApp.splashConfig?.mediaPath,
            bgmEnabled = webApp.bgmEnabled,
            bgmConfig = webApp.bgmConfig ?: BgmConfig(),
            apkExportConfig = webApp.apkExportConfig ?: ApkExportConfig(),
            themeType = webApp.themeType,
            translateEnabled = webApp.translateEnabled,
            translateConfig = webApp.translateConfig ?: com.webtoapp.data.model.TranslateConfig(),
            extensionModuleEnabled = webApp.extensionModuleIds.isNotEmpty(),
            extensionModuleIds = webApp.extensionModuleIds.toSet(),
            extensionFabIcon = webApp.extensionFabIcon ?: "",
            autoStartConfig = webApp.autoStartConfig,
            forcedRunConfig = webApp.forcedRunConfig,
            blackTechConfig = webApp.blackTechConfig,
            disguiseConfig = webApp.disguiseConfig,
            deviceDisguiseConfig = webApp.deviceDisguiseConfig ?: DeviceDisguiseConfig()
        )
    }
}
