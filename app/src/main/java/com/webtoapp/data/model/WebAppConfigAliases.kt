package com.webtoapp.data.model

typealias AdConfig = com.webtoapp.data.model.webapp.config.AdConfig
typealias AnnouncementTemplateType = com.webtoapp.data.model.webapp.config.AnnouncementTemplateType
typealias Announcement = com.webtoapp.data.model.webapp.config.Announcement
typealias StatusBarColorMode = com.webtoapp.data.model.webapp.config.StatusBarColorMode
typealias StatusBarBackgroundType = com.webtoapp.data.model.webapp.config.StatusBarBackgroundType
typealias LongPressMenuStyle = com.webtoapp.data.model.webapp.config.LongPressMenuStyle
typealias UserAgentMode = com.webtoapp.data.model.webapp.config.UserAgentMode
typealias WebViewConfig = com.webtoapp.data.model.webapp.config.WebViewConfig
typealias FloatingWindowConfig = com.webtoapp.data.model.webapp.config.FloatingWindowConfig
typealias FloatingBorderStyle = com.webtoapp.data.model.webapp.config.FloatingBorderStyle
typealias UserScript = com.webtoapp.data.model.webapp.config.UserScript
typealias ScriptRunTime = com.webtoapp.data.model.webapp.config.ScriptRunTime
typealias NewWindowBehavior = com.webtoapp.data.model.webapp.config.NewWindowBehavior
typealias SplashConfig = com.webtoapp.data.model.webapp.config.SplashConfig
typealias SplashType = com.webtoapp.data.model.webapp.config.SplashType
typealias SplashOrientation = com.webtoapp.data.model.webapp.config.SplashOrientation
typealias KeyboardAdjustMode = com.webtoapp.data.model.webapp.config.KeyboardAdjustMode
typealias OrientationMode = com.webtoapp.data.model.webapp.config.OrientationMode
typealias ScreenAwakeMode = com.webtoapp.data.model.webapp.config.ScreenAwakeMode
typealias ViewportMode = com.webtoapp.data.model.webapp.config.ViewportMode
typealias MediaConfig = com.webtoapp.data.model.webapp.config.MediaConfig
typealias GalleryConfig = com.webtoapp.data.model.webapp.config.GalleryConfig
typealias GalleryItem = com.webtoapp.data.model.webapp.config.GalleryItem
typealias GalleryCategory = com.webtoapp.data.model.webapp.config.GalleryCategory
typealias GalleryItemType = com.webtoapp.data.model.webapp.config.GalleryItemType
typealias GalleryPlayMode = com.webtoapp.data.model.webapp.config.GalleryPlayMode
typealias GalleryViewMode = com.webtoapp.data.model.webapp.config.GalleryViewMode
typealias GallerySortOrder = com.webtoapp.data.model.webapp.config.GallerySortOrder
typealias NodeJsBuildMode = com.webtoapp.data.model.webapp.config.NodeJsBuildMode
typealias NodeJsConfig = com.webtoapp.data.model.webapp.config.NodeJsConfig
typealias WordPressConfig = com.webtoapp.data.model.webapp.config.WordPressConfig
typealias PhpAppConfig = com.webtoapp.data.model.webapp.config.PhpAppConfig
typealias PythonAppConfig = com.webtoapp.data.model.webapp.config.PythonAppConfig
typealias GoAppConfig = com.webtoapp.data.model.webapp.config.GoAppConfig
typealias MultiWebConfig = com.webtoapp.data.model.webapp.config.MultiWebConfig
typealias MultiWebSite = com.webtoapp.data.model.webapp.config.MultiWebSite
typealias HtmlConfig = com.webtoapp.data.model.webapp.config.HtmlConfig
typealias HtmlFile = com.webtoapp.data.model.webapp.config.HtmlFile
typealias HtmlFileType = com.webtoapp.data.model.webapp.config.HtmlFileType
typealias BgmPlayMode = com.webtoapp.data.model.webapp.config.BgmPlayMode
typealias BgmTag = com.webtoapp.data.model.webapp.config.BgmTag
typealias LrcLine = com.webtoapp.data.model.webapp.config.LrcLine
typealias LrcData = com.webtoapp.data.model.webapp.config.LrcData
typealias LrcTheme = com.webtoapp.data.model.webapp.config.LrcTheme
typealias LrcAnimationType = com.webtoapp.data.model.webapp.config.LrcAnimationType
typealias LrcPosition = com.webtoapp.data.model.webapp.config.LrcPosition
typealias BgmItem = com.webtoapp.data.model.webapp.config.BgmItem
typealias BgmConfig = com.webtoapp.data.model.webapp.config.BgmConfig
typealias ApkArchitecture = com.webtoapp.data.model.webapp.config.ApkArchitecture
typealias ApkExportConfig = com.webtoapp.data.model.webapp.config.ApkExportConfig
typealias PerformanceOptimizationConfig = com.webtoapp.data.model.webapp.config.PerformanceOptimizationConfig
typealias BackgroundRunExportConfig = com.webtoapp.data.model.webapp.config.BackgroundRunExportConfig
typealias ApkEncryptionConfig = com.webtoapp.data.model.webapp.config.ApkEncryptionConfig
typealias AppHardeningConfig = com.webtoapp.data.model.webapp.config.AppHardeningConfig
typealias TranslateLanguage = com.webtoapp.data.model.webapp.config.TranslateLanguage
typealias TranslateEngine = com.webtoapp.data.model.webapp.config.TranslateEngine
typealias TranslateConfig = com.webtoapp.data.model.webapp.config.TranslateConfig
typealias ActivationDialogConfig = com.webtoapp.data.model.webapp.config.ActivationDialogConfig
typealias AutoStartConfig = com.webtoapp.data.model.webapp.config.AutoStartConfig

object UserAgentVersions {
    const val CHROME = com.webtoapp.data.model.webapp.config.UserAgentVersions.CHROME
    const val FIREFOX = com.webtoapp.data.model.webapp.config.UserAgentVersions.FIREFOX
    const val SAFARI = com.webtoapp.data.model.webapp.config.UserAgentVersions.SAFARI
}

fun WebApp.getAllActivationCodes(): List<com.webtoapp.core.activation.ActivationCode> =
    com.webtoapp.data.model.webapp.config.getAllActivationCodes(this)

fun WebApp.getActivationCodeStrings(): List<String> =
    com.webtoapp.data.model.webapp.config.getActivationCodeStrings(this)
