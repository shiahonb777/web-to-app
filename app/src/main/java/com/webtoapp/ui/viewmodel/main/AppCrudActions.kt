package com.webtoapp.ui.viewmodel.main

import com.webtoapp.data.model.*
import com.webtoapp.ui.viewmodel.EditState

class AppCrudActions {

    fun buildWebAppForSave(
        state: EditState,
        currentApp: WebApp?,
        selectedCategoryId: Long?,
        normalizedUrl: (String, AppType, Boolean) -> String,
        themeType: String,
        iconPath: String?
    ): WebApp {
        val splashConfig = buildSplashConfig(state)
        val bgmConfig = buildBgmConfig(state)
        val apkExportConfig = buildApkExportConfig(state)
        val translateConfig = buildTranslateConfig(state)
        val activationCodeStrings = buildActivationCodes(state)
        val appType = currentApp?.appType ?: state.appType
        val resolvedUrl = normalizedUrl(state.url, appType, state.allowHttp)
        val categoryId = selectedCategoryId?.takeIf { it > 0 }

        return currentApp?.copy(
            name = state.name,
            url = resolvedUrl,
            iconPath = iconPath,
            appType = appType,
            mediaConfig = state.mediaConfig,
            htmlConfig = state.htmlConfig,
            activationEnabled = state.activationEnabled,
            activationCodes = activationCodeStrings,
            activationCodeList = state.activationCodeList,
            activationRequireEveryTime = state.activationRequireEveryTime,
            activationDialogConfig = sanitizeActivationDialog(state.activationDialogConfig),
            adsEnabled = state.adsEnabled,
            adConfig = state.adConfig,
            announcementEnabled = state.announcementEnabled,
            announcement = state.announcement,
            adBlockEnabled = state.adBlockEnabled,
            adBlockRules = state.adBlockRules,
            webViewConfig = state.webViewConfig,
            splashEnabled = state.splashEnabled,
            splashConfig = splashConfig,
            bgmEnabled = state.bgmEnabled,
            bgmConfig = bgmConfig,
            apkExportConfig = apkExportConfig,
            themeType = themeType,
            translateEnabled = state.translateEnabled,
            translateConfig = translateConfig,
            extensionModuleIds = state.extensionModuleIds.toList(),
            extensionFabIcon = state.extensionFabIcon.ifBlank { null },
            autoStartConfig = state.autoStartConfig,
            forcedRunConfig = state.forcedRunConfig,
            blackTechConfig = state.blackTechConfig,
            disguiseConfig = state.disguiseConfig,
            deviceDisguiseConfig = state.deviceDisguiseConfig
        ) ?: run {
            WebApp(
                name = state.name,
                url = resolvedUrl,
                iconPath = iconPath,
                appType = appType,
                mediaConfig = state.mediaConfig,
                htmlConfig = state.htmlConfig,
                activationEnabled = state.activationEnabled,
                activationCodes = activationCodeStrings,
                activationCodeList = state.activationCodeList,
                activationRequireEveryTime = state.activationRequireEveryTime,
                activationDialogConfig = sanitizeActivationDialog(state.activationDialogConfig),
                adsEnabled = state.adsEnabled,
                adConfig = state.adConfig,
                announcementEnabled = state.announcementEnabled,
                announcement = state.announcement,
                adBlockEnabled = state.adBlockEnabled,
                adBlockRules = state.adBlockRules,
                webViewConfig = state.webViewConfig,
                splashEnabled = state.splashEnabled,
                splashConfig = splashConfig,
                bgmEnabled = state.bgmEnabled,
                bgmConfig = bgmConfig,
                apkExportConfig = apkExportConfig,
                themeType = themeType,
                translateEnabled = state.translateEnabled,
                translateConfig = translateConfig,
                extensionModuleIds = state.extensionModuleIds.toList(),
                extensionFabIcon = state.extensionFabIcon.ifBlank { null },
                autoStartConfig = state.autoStartConfig,
                forcedRunConfig = state.forcedRunConfig,
                blackTechConfig = state.blackTechConfig,
                disguiseConfig = state.disguiseConfig,
                deviceDisguiseConfig = state.deviceDisguiseConfig,
                categoryId = categoryId
            )
        }
    }

    private fun buildSplashConfig(state: EditState): SplashConfig? {
        return if (state.splashEnabled && state.savedSplashPath != null) {
            state.splashConfig.copy(mediaPath = state.savedSplashPath)
        } else null
    }

    private fun buildBgmConfig(state: EditState): BgmConfig? {
        return if (state.bgmEnabled && state.bgmConfig.playlist.isNotEmpty()) {
            state.bgmConfig
        } else null
    }

    private fun buildApkExportConfig(state: EditState): ApkExportConfig? {
        val config = state.apkExportConfig
        return if (config.customPackageName.isNullOrBlank() &&
            config.customVersionName.isNullOrBlank() &&
            config.customVersionCode == null
        ) {
            null
        } else {
            config
        }
    }

    private fun buildTranslateConfig(state: EditState): TranslateConfig? {
        return if (state.translateEnabled) state.translateConfig else null
    }

    private fun buildActivationCodes(state: EditState): List<String> {
        val legacy = state.activationCodes.filter { !it.trimStart().startsWith("{") }
        return state.activationCodeList.map { it.toJson() } + legacy
    }

    private fun sanitizeActivationDialog(config: ActivationDialogConfig): ActivationDialogConfig? {
        return if (config.title.isBlank() &&
            config.subtitle.isBlank() &&
            config.inputLabel.isBlank() &&
            config.buttonText.isBlank()
        ) {
            null
        } else {
            config
        }
    }
}
