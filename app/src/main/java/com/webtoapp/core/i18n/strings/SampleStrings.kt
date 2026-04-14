package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object SampleStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Test Page ====================
    val testPageBasicHtml: String get() = when (lang) {
        AppLanguage.CHINESE -> "基础HTML页面"
        AppLanguage.ENGLISH -> "Basic HTML Page"
        AppLanguage.ARABIC -> "صفحة HTML أساسية"
    }

    val testPageBasicHtmlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "包含常见HTML元素的测试页面"
        AppLanguage.ENGLISH -> "Test page with common HTML elements"
        AppLanguage.ARABIC -> "صفحة اختبار مع عناصر HTML شائعة"
    }

    val testPageForm: String get() = when (lang) {
        AppLanguage.CHINESE -> "表单测试页"
        AppLanguage.ENGLISH -> "Form Test Page"
        AppLanguage.ARABIC -> "صفحة اختبار النموذج"
    }

    val testPageFormDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "包含各种表单元素的测试页面"
        AppLanguage.ENGLISH -> "Test page with various form elements"
        AppLanguage.ARABIC -> "صفحة اختبار مع عناصر نموذج متنوعة"
    }

    val testPageMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体测试页"
        AppLanguage.ENGLISH -> "Media Test Page"
        AppLanguage.ARABIC -> "صفحة اختبار الوسائط"
    }

    val testPageMediaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "包含图片、视频、音频的测试页面"
        AppLanguage.ENGLISH -> "Test page with images, videos, audio"
        AppLanguage.ARABIC -> "صفحة اختبار مع صور وفيديو وصوت"
    }

    val testPageAdSimulator: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告模拟页"
        AppLanguage.ENGLISH -> "Ad Simulator Page"
        AppLanguage.ARABIC -> "صفحة محاكاة الإعلانات"
    }

    val testPageAdSimulatorDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "模拟各种广告元素，用于测试广告拦截"
        AppLanguage.ENGLISH -> "Simulate various ad elements for testing ad blocking"
        AppLanguage.ARABIC -> "محاكاة عناصر إعلانية متنوعة لاختبار حظر الإعلانات"
    }

    val testPagePopup: String get() = when (lang) {
        AppLanguage.CHINESE -> "弹窗测试页"
        AppLanguage.ENGLISH -> "Popup Test Page"
        AppLanguage.ARABIC -> "صفحة اختبار النوافذ المنبثقة"
    }

    val testPagePopupDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试各种弹窗和对话框"
        AppLanguage.ENGLISH -> "Test various popups and dialogs"
        AppLanguage.ARABIC -> "اختبار النوافذ المنبثقة والحوارات المتنوعة"
    }

    val testPageScroll: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动测试页"
        AppLanguage.ENGLISH -> "Scroll Test Page"
        AppLanguage.ARABIC -> "صفحة اختبار التمرير"
    }

    val testPageScrollDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "长页面，用于测试滚动相关功能"
        AppLanguage.ENGLISH -> "Long page for testing scroll-related features"
        AppLanguage.ARABIC -> "صفحة طويلة لاختبار ميزات التمرير"
    }

    val testPageStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "样式测试页"
        AppLanguage.ENGLISH -> "Style Test Page"
        AppLanguage.ARABIC -> "صفحة اختبار الأنماط"
    }

    val testPageStyleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试CSS样式修改效果"
        AppLanguage.ENGLISH -> "Test CSS style modification effects"
        AppLanguage.ARABIC -> "اختبار تأثيرات تعديل أنماط CSS"
    }

    val testPageApi: String get() = when (lang) {
        AppLanguage.CHINESE -> "API测试页"
        AppLanguage.ENGLISH -> "API Test Page"
        AppLanguage.ARABIC -> "صفحة اختبار API"
    }

    val testPageApiDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "测试网络请求和API调用"
        AppLanguage.ENGLISH -> "Test network requests and API calls"
        AppLanguage.ARABIC -> "اختبار طلبات الشبكة واستدعاءات API"
    }
}
