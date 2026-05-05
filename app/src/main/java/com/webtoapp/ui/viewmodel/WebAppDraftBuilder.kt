package com.webtoapp.ui.viewmodel

import com.webtoapp.data.model.ActivationDialogConfig
import com.webtoapp.data.model.ApkExportConfig
import com.webtoapp.data.model.WebApp
import com.webtoapp.data.model.isMeaningful

data class DraftBuildOverrides(
    val appNameFallback: String? = null,
    val categoryId: Long? = null,
)

data class DraftBuildPayload(
    val normalizedUrl: String,
    val iconPath: String?,
    val appType: com.webtoapp.data.model.AppType,
    val mediaConfig: com.webtoapp.data.model.MediaConfig?,
    val htmlConfig: com.webtoapp.data.model.HtmlConfig?,
    val splashConfig: com.webtoapp.data.model.SplashConfig?,
    val bgmConfig: com.webtoapp.data.model.BgmConfig?,
    val apkExportConfig: ApkExportConfig?,
    val translateConfig: com.webtoapp.data.model.TranslateConfig?,
    val currentThemeType: String,
    val externalizedWebViewConfig: com.webtoapp.data.model.WebViewConfig,
    val extensionModuleIds: List<String>,
)

private fun ActivationDialogConfig?.normalize(): ActivationDialogConfig? {
    val config = this ?: return null
    return if (
        config.title.isBlank() &&
        config.subtitle.isBlank() &&
        config.inputLabel.isBlank() &&
        config.buttonText.isBlank()
    ) {
        null
    } else {
        config
    }
}

fun EditState.toDraftPayload(
    normalizedUrl: String,
    iconPath: String?,
    extensionModuleIds: Set<String>,
    currentThemeType: String,
    externalizedWebViewConfig: com.webtoapp.data.model.WebViewConfig,
): DraftBuildPayload {
    val resolvedSplashConfig = if (splashEnabled && savedSplashPath != null) {
        splashConfig.copy(mediaPath = savedSplashPath)
    } else {
        null
    }

    val resolvedBgmConfig = if (bgmEnabled && bgmConfig.playlist.isNotEmpty()) {
        bgmConfig
    } else {
        null
    }

    val resolvedApkExportConfig = apkExportConfig.takeIf { it.isMeaningful() }
    val resolvedTranslateConfig = translateConfig.takeIf { translateEnabled }

    return DraftBuildPayload(
        normalizedUrl = normalizedUrl,
        iconPath = iconPath,
        appType = appType,
        mediaConfig = mediaConfig,
        htmlConfig = htmlConfig,
        splashConfig = resolvedSplashConfig,
        bgmConfig = resolvedBgmConfig,
        apkExportConfig = resolvedApkExportConfig,
        translateConfig = resolvedTranslateConfig,
        currentThemeType = currentThemeType,
        externalizedWebViewConfig = externalizedWebViewConfig,
        extensionModuleIds = extensionModuleIds.toList(),
    )
}

fun WebApp?.applyDraft(
    editState: EditState,
    payload: DraftBuildPayload,
    overrides: DraftBuildOverrides = DraftBuildOverrides(),
): WebApp {
    val resolvedName = editState.name.ifBlank {
        overrides.appNameFallback ?: this?.name.orEmpty()
    }
    val resolvedActivationDialogConfig = editState.activationDialogConfig.normalize()
    return this?.copy(
        name = resolvedName,
        url = payload.normalizedUrl,
        iconPath = payload.iconPath,
        appType = payload.appType,
        mediaConfig = payload.mediaConfig,
        htmlConfig = payload.htmlConfig,
        activationEnabled = editState.activationEnabled,
        activationCodes = emptyList(),
        activationCodeList = editState.activationCodeList,
        activationRequireEveryTime = editState.activationRequireEveryTime,
        activationDialogConfig = resolvedActivationDialogConfig,
        adsEnabled = editState.adsEnabled,
        adConfig = editState.adConfig,
        announcementEnabled = editState.announcementEnabled,
        announcement = editState.announcement,
        adBlockEnabled = editState.adBlockEnabled,
        adBlockRules = editState.adBlockRules,
        webViewConfig = payload.externalizedWebViewConfig,
        splashEnabled = editState.splashEnabled,
        splashConfig = payload.splashConfig,
        bgmEnabled = editState.bgmEnabled,
        bgmConfig = payload.bgmConfig,
        apkExportConfig = payload.apkExportConfig,
        themeType = payload.currentThemeType,
        translateEnabled = editState.translateEnabled,
        translateConfig = payload.translateConfig,
        extensionModuleIds = payload.extensionModuleIds,
        extensionEnabled = editState.extensionModuleEnabled,
        extensionFabIcon = editState.extensionFabIcon.ifBlank { null },
        autoStartConfig = editState.autoStartConfig,
        forcedRunConfig = editState.forcedRunConfig,
        blackTechConfig = editState.blackTechConfig,
        disguiseConfig = editState.disguiseConfig,
        deviceDisguiseConfig = editState.deviceDisguiseConfig,
    ) ?: WebApp(
        name = resolvedName,
        url = payload.normalizedUrl,
        iconPath = payload.iconPath,
        appType = payload.appType,
        mediaConfig = payload.mediaConfig,
        htmlConfig = payload.htmlConfig,
        activationEnabled = editState.activationEnabled,
        activationCodes = emptyList(),
        activationCodeList = editState.activationCodeList,
        activationRequireEveryTime = editState.activationRequireEveryTime,
        activationDialogConfig = resolvedActivationDialogConfig,
        adsEnabled = editState.adsEnabled,
        adConfig = editState.adConfig,
        announcementEnabled = editState.announcementEnabled,
        announcement = editState.announcement,
        adBlockEnabled = editState.adBlockEnabled,
        adBlockRules = editState.adBlockRules,
        webViewConfig = payload.externalizedWebViewConfig,
        splashEnabled = editState.splashEnabled,
        splashConfig = payload.splashConfig,
        bgmEnabled = editState.bgmEnabled,
        bgmConfig = payload.bgmConfig,
        apkExportConfig = payload.apkExportConfig,
        themeType = payload.currentThemeType,
        translateEnabled = editState.translateEnabled,
        translateConfig = payload.translateConfig,
        extensionModuleIds = payload.extensionModuleIds,
        extensionEnabled = editState.extensionModuleEnabled,
        extensionFabIcon = editState.extensionFabIcon.ifBlank { null },
        autoStartConfig = editState.autoStartConfig,
        forcedRunConfig = editState.forcedRunConfig,
        blackTechConfig = editState.blackTechConfig,
        disguiseConfig = editState.disguiseConfig,
        deviceDisguiseConfig = editState.deviceDisguiseConfig,
        categoryId = overrides.categoryId,
    )
}
