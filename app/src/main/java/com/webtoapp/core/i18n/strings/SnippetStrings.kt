package com.webtoapp.core.i18n.strings

import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.i18n.Strings

internal object SnippetStrings {
    private val lang: AppLanguage get() = Strings.delegateLanguage
    // ==================== Snippet Categories ====================
    val snippetNative: String get() = when (lang) {
        AppLanguage.CHINESE -> "原生能力"
        AppLanguage.ENGLISH -> "Native Features"
        AppLanguage.ARABIC -> "الميزات الأصلية"
    }

    val snippetNativeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "调用原生能力，如分享、震动、剪贴板等"
        AppLanguage.ENGLISH -> "Call native capabilities like share, vibrate, clipboard, etc."
        AppLanguage.ARABIC -> "استدعاء القدرات الأصلية مثل المشاركة والاهتزاز والحافظة وما إلى ذلك"
    }

    val snippetShowToast: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示 Toast 提示"
        AppLanguage.ENGLISH -> "Show Toast Message"
        AppLanguage.ARABIC -> "عرض رسالة Toast"
    }

    val snippetShowToastDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示原生 Toast 消息提示"
        AppLanguage.ENGLISH -> "Show native Toast message"
        AppLanguage.ARABIC -> "عرض رسالة Toast الأصلية"
    }

    val snippetVibrate: String get() = when (lang) {
        AppLanguage.CHINESE -> "震动反馈"
        AppLanguage.ENGLISH -> "Vibration Feedback"
        AppLanguage.ARABIC -> "استجابة الاهتزاز"
    }

    val snippetVibrateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "触发手机震动"
        AppLanguage.ENGLISH -> "Trigger phone vibration"
        AppLanguage.ARABIC -> "تشغيل اهتزاز الهاتف"
    }

    val snippetCopyToClipboard: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制到剪贴板"
        AppLanguage.ENGLISH -> "Copy to Clipboard"
        AppLanguage.ARABIC -> "نسخ إلى الحافظة"
    }

    val snippetCopyToClipboardDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制文本到系统剪贴板"
        AppLanguage.ENGLISH -> "Copy text to system clipboard"
        AppLanguage.ARABIC -> "نسخ النص إلى حافظة النظام"
    }

    val snippetSaveVideoToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存视频到相册"
        AppLanguage.ENGLISH -> "Save Video to Gallery"
        AppLanguage.ARABIC -> "حفظ الفيديو في المعرض"
    }

    val snippetSaveVideoToGalleryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将视频保存到手机相册"
        AppLanguage.ENGLISH -> "Save video to phone gallery"
        AppLanguage.ARABIC -> "حفظ الفيديو في معرض الهاتف"
    }

    val snippetOpenInBrowser: String get() = when (lang) {
        AppLanguage.CHINESE -> "用浏览器打开链接"
        AppLanguage.ENGLISH -> "Open Link in Browser"
        AppLanguage.ARABIC -> "فتح الرابط في المتصفح"
    }

    val snippetOpenInBrowserDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用系统浏览器打开外部链接"
        AppLanguage.ENGLISH -> "Open external link with system browser"
        AppLanguage.ARABIC -> "فتح الرابط الخارجي باستخدام متصفح النظام"
    }

    val snippetDeviceInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取设备信息"
        AppLanguage.ENGLISH -> "Get Device Info"
        AppLanguage.ARABIC -> "الحصول على معلومات الجهاز"
    }

    val snippetDeviceInfoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取手机设备和应用信息"
        AppLanguage.ENGLISH -> "Get phone device and app info"
        AppLanguage.ARABIC -> "الحصول على معلومات الجهاز والتطبيق"
    }

    val snippetNetworkStatus: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查网络状态"
        AppLanguage.ENGLISH -> "Check Network Status"
        AppLanguage.ARABIC -> "التحقق من حالة الشبكة"
    }

    val snippetNetworkStatusDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查网络连接状态和类型"
        AppLanguage.ENGLISH -> "Check network connection status and type"
        AppLanguage.ARABIC -> "التحقق من حالة ونوع اتصال الشبكة"
    }

    val snippetSaveFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存文件"
        AppLanguage.ENGLISH -> "Save File"
        AppLanguage.ARABIC -> "حفظ الملف"
    }

    val snippetSaveFileDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将内容保存为文件"
        AppLanguage.ENGLISH -> "Save content as file"
        AppLanguage.ARABIC -> "حفظ المحتوى كملف"
    }

    val snippetImageDownloadBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片下载按钮"
        AppLanguage.ENGLISH -> "Image Download Button"
        AppLanguage.ARABIC -> "زر تنزيل الصورة"
    }

    val snippetImageDownloadBtnDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "为图片添加悬浮下载按钮"
        AppLanguage.ENGLISH -> "Add floating download button to images"
        AppLanguage.ARABIC -> "إضافة زر تنزيل عائم للصور"
    }

    val snippetDom: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM 操作"
        AppLanguage.ENGLISH -> "DOM Operations"
        AppLanguage.ARABIC -> "عمليات DOM"
    }

    val snippetDomDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面元素的查询、修改、创建和删除"
        AppLanguage.ENGLISH -> "Query, modify, create and delete page elements"
        AppLanguage.ARABIC -> "استعلام وتعديل وإنشاء وحذف عناصر الصفحة"
    }

    val snippetStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "样式操作"
        AppLanguage.ENGLISH -> "Style Operations"
        AppLanguage.ARABIC -> "عمليات الأنماط"
    }

    val snippetStyleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 样式的注入、修改和动态控制"
        AppLanguage.ENGLISH -> "CSS style injection, modification and dynamic control"
        AppLanguage.ARABIC -> "حقن وتعديل وتحكم ديناميكي في أنماط CSS"
    }

    val snippetEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "事件处理"
        AppLanguage.ENGLISH -> "Event Handling"
        AppLanguage.ARABIC -> "معالجة الأحداث"
    }

    val snippetEventDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击、滚动、键盘等事件的监听和处理"
        AppLanguage.ENGLISH -> "Listen and handle click, scroll, keyboard events"
        AppLanguage.ARABIC -> "الاستماع ومعالجة أحداث النقر والتمرير ولوحة المفاتيح"
    }

    val snippetStorage: String get() = when (lang) {
        AppLanguage.CHINESE -> "本地存储"
        AppLanguage.ENGLISH -> "Local Storage"
        AppLanguage.ARABIC -> "التخزين المحلي"
    }

    val snippetStorageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据的本地存储和读取"
        AppLanguage.ENGLISH -> "Local data storage and retrieval"
        AppLanguage.ARABIC -> "تخزين واسترجاع البيانات المحلية"
    }

    val snippetNetwork: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络请求"
        AppLanguage.ENGLISH -> "Network Requests"
        AppLanguage.ARABIC -> "طلبات الشبكة"
    }

    val snippetNetworkDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "发起网络请求和处理响应"
        AppLanguage.ENGLISH -> "Make network requests and handle responses"
        AppLanguage.ARABIC -> "إجراء طلبات الشبكة ومعالجة الاستجابات"
    }

    val snippetUi: String get() = when (lang) {
        AppLanguage.CHINESE -> "UI 增强"
        AppLanguage.ENGLISH -> "UI Enhancement"
        AppLanguage.ARABIC -> "تحسين واجهة المستخدم"
    }

    val snippetUiDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮按钮、弹窗、通知等 UI 组件"
        AppLanguage.ENGLISH -> "Floating buttons, popups, notifications and other UI components"
        AppLanguage.ARABIC -> "أزرار عائمة، نوافذ منبثقة، إشعارات ومكونات واجهة أخرى"
    }

    val snippetWidget: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮组件"
        AppLanguage.ENGLISH -> "Floating Widgets"
        AppLanguage.ARABIC -> "أدوات عائمة"
    }

    val snippetWidgetDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮面板、工具栏、侧边栏等"
        AppLanguage.ENGLISH -> "Floating panels, toolbars, sidebars"
        AppLanguage.ARABIC -> "لوحات عائمة، أشرطة أدوات، أشرطة جانبية"
    }

    val snippetNotification: String get() = when (lang) {
        AppLanguage.CHINESE -> "通知系统"
        AppLanguage.ENGLISH -> "Notification System"
        AppLanguage.ARABIC -> "نظام الإشعارات"
    }

    val snippetNotificationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "各种通知和提醒功能"
        AppLanguage.ENGLISH -> "Various notification and alert features"
        AppLanguage.ARABIC -> "ميزات الإشعارات والتنبيهات المختلفة"
    }

    val snippetScroll: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动操作"
        AppLanguage.ENGLISH -> "Scroll Operations"
        AppLanguage.ARABIC -> "عمليات التمرير"
    }

    val snippetScrollDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面滚动控制和自动滚动"
        AppLanguage.ENGLISH -> "Page scroll control and auto-scroll"
        AppLanguage.ARABIC -> "التحكم في تمرير الصفحة والتمرير التلقائي"
    }

    val snippetQuerySingle: String get() = when (lang) {
        AppLanguage.CHINESE -> "查询单个元素"
        AppLanguage.ENGLISH -> "Query Single Element"
        AppLanguage.ARABIC -> "استعلام عنصر واحد"
    }

    val snippetQuerySingleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 CSS 选择器查询单个元素"
        AppLanguage.ENGLISH -> "Query single element using CSS selector"
        AppLanguage.ARABIC -> "استعلام عنصر واحد باستخدام محدد CSS"
    }

    val snippetQueryAll: String get() = when (lang) {
        AppLanguage.CHINESE -> "查询所有元素"
        AppLanguage.ENGLISH -> "Query All Elements"
        AppLanguage.ARABIC -> "استعلام جميع العناصر"
    }

    val snippetQueryAllDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 CSS 选择器查询所有匹配元素"
        AppLanguage.ENGLISH -> "Query all matching elements using CSS selector"
        AppLanguage.ARABIC -> "استعلام جميع العناصر المطابقة باستخدام محدد CSS"
    }

    val snippetHideElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏元素"
        AppLanguage.ENGLISH -> "Hide Element"
        AppLanguage.ARABIC -> "إخفاء العنصر"
    }

    val snippetHideElementDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏指定的页面元素"
        AppLanguage.ENGLISH -> "Hide specified page element"
        AppLanguage.ARABIC -> "إخفاء عنصر الصفحة المحدد"
    }

    val snippetRemoveElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除元素"
        AppLanguage.ENGLISH -> "Remove Element"
        AppLanguage.ARABIC -> "إزالة العنصر"
    }

    val snippetRemoveElementDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "从页面中删除指定元素"
        AppLanguage.ENGLISH -> "Remove specified element from page"
        AppLanguage.ARABIC -> "إزالة العنصر المحدد من الصفحة"
    }

    val snippetCreateElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建元素"
        AppLanguage.ENGLISH -> "Create Element"
        AppLanguage.ARABIC -> "إنشاء عنصر"
    }

    val snippetCreateElementDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建并添加新元素到页面"
        AppLanguage.ENGLISH -> "Create and add new element to page"
        AppLanguage.ARABIC -> "إنشاء وإضافة عنصر جديد إلى الصفحة"
    }

    val snippetModifyText: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改文本内容"
        AppLanguage.ENGLISH -> "Modify Text Content"
        AppLanguage.ARABIC -> "تعديل محتوى النص"
    }

    val snippetModifyTextDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改元素的文本内容"
        AppLanguage.ENGLISH -> "Modify element's text content"
        AppLanguage.ARABIC -> "تعديل محتوى نص العنصر"
    }

    val snippetModifyAttr: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改属性"
        AppLanguage.ENGLISH -> "Modify Attribute"
        AppLanguage.ARABIC -> "تعديل السمة"
    }

    val snippetModifyAttrDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改元素的属性值"
        AppLanguage.ENGLISH -> "Modify element's attribute value"
        AppLanguage.ARABIC -> "تعديل قيمة سمة العنصر"
    }

    val snippetInsertBefore: String get() = when (lang) {
        AppLanguage.CHINESE -> "在元素前插入"
        AppLanguage.ENGLISH -> "Insert Before Element"
        AppLanguage.ARABIC -> "إدراج قبل العنصر"
    }

    val snippetInsertBeforeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在指定元素前插入新元素"
        AppLanguage.ENGLISH -> "Insert new element before specified element"
        AppLanguage.ARABIC -> "إدراج عنصر جديد قبل العنصر المحدد"
    }

    val snippetInsertAfter: String get() = when (lang) {
        AppLanguage.CHINESE -> "在元素后插入"
        AppLanguage.ENGLISH -> "Insert After Element"
        AppLanguage.ARABIC -> "إدراج بعد العنصر"
    }

    val snippetInsertAfterDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在指定元素后插入新元素"
        AppLanguage.ENGLISH -> "Insert new element after specified element"
        AppLanguage.ARABIC -> "إدراج عنصر جديد بعد العنصر المحدد"
    }

    val snippetCloneElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "克隆元素"
        AppLanguage.ENGLISH -> "Clone Element"
        AppLanguage.ARABIC -> "استنساخ العنصر"
    }

    val snippetCloneElementDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "克隆一个元素及其子元素"
        AppLanguage.ENGLISH -> "Clone an element and its children"
        AppLanguage.ARABIC -> "استنساخ عنصر وعناصره الفرعية"
    }

    val snippetWrapElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "包裹元素"
        AppLanguage.ENGLISH -> "Wrap Element"
        AppLanguage.ARABIC -> "لف العنصر"
    }

    val snippetWrapElementDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "用新元素包裹现有元素"
        AppLanguage.ENGLISH -> "Wrap existing element with new element"
        AppLanguage.ARABIC -> "لف العنصر الموجود بعنصر جديد"
    }

    val snippetReplaceElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "替换元素"
        AppLanguage.ENGLISH -> "Replace Element"
        AppLanguage.ARABIC -> "استبدال العنصر"
    }

    val snippetReplaceElementDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "用新元素替换现有元素"
        AppLanguage.ENGLISH -> "Replace existing element with new element"
        AppLanguage.ARABIC -> "استبدال العنصر الموجود بعنصر جديد"
    }

    val snippetUtil: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具函数"
        AppLanguage.ENGLISH -> "Utility Functions"
        AppLanguage.ARABIC -> "الوظائف المساعدة"
    }

    val snippetUtilDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "常用工具函数和辅助方法"
        AppLanguage.ENGLISH -> "Common utility functions and helper methods"
        AppLanguage.ARABIC -> "الوظائف المساعدة والأساليب الشائعة"
    }

    val snippetData: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据处理"
        AppLanguage.ENGLISH -> "Data Processing"
        AppLanguage.ARABIC -> "معالجة البيانات"
    }

    val snippetDataDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "数据提取、转换、导出等操作"
        AppLanguage.ENGLISH -> "Data extraction, transformation, export operations"
        AppLanguage.ARABIC -> "استخراج البيانات وتحويلها وتصديرها"
    }

    val snippetSaveImageToGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存图片到相册"
        AppLanguage.ENGLISH -> "Save Image to Gallery"
        AppLanguage.ARABIC -> "حفظ الصورة في المعرض"
    }

    val snippetSaveImageToGalleryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将图片保存到手机相册"
        AppLanguage.ENGLISH -> "Save image to phone gallery"
        AppLanguage.ARABIC -> "حفظ الصورة في معرض الهاتف"
    }

    val snippetShareContent: String get() = when (lang) {
        AppLanguage.CHINESE -> "分享内容"
        AppLanguage.ENGLISH -> "Share Content"
        AppLanguage.ARABIC -> "مشاركة المحتوى"
    }

    val snippetShareContentDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "调用系统分享功能"
        AppLanguage.ENGLISH -> "Call system share function"
        AppLanguage.ARABIC -> "استدعاء وظيفة المشاركة في النظام"
    }

    val snippetInjectCss: String get() = when (lang) {
        AppLanguage.CHINESE -> "注入 CSS 样式"
        AppLanguage.ENGLISH -> "Inject CSS Styles"
        AppLanguage.ARABIC -> "حقن أنماط CSS"
    }

    val snippetInjectCssDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "向页面注入自定义 CSS"
        AppLanguage.ENGLISH -> "Inject custom CSS into page"
        AppLanguage.ARABIC -> "حقن CSS مخصص في الصفحة"
    }

    val snippetModifyInline: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改内联样式"
        AppLanguage.ENGLISH -> "Modify Inline Style"
        AppLanguage.ARABIC -> "تعديل النمط المضمن"
    }

    val snippetModifyInlineDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "直接修改元素的内联样式"
        AppLanguage.ENGLISH -> "Directly modify element's inline style"
        AppLanguage.ARABIC -> "تعديل النمط المضمن للعنصر مباشرة"
    }

    val snippetAddClass: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加/移除类名"
        AppLanguage.ENGLISH -> "Add/Remove Class"
        AppLanguage.ARABIC -> "إضافة/إزالة الفئة"
    }

    val snippetAddClassDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "操作元素的 CSS 类"
        AppLanguage.ENGLISH -> "Manipulate element's CSS classes"
        AppLanguage.ARABIC -> "التعامل مع فئات CSS للعنصر"
    }

    val snippetDarkMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "深色模式"
        AppLanguage.ENGLISH -> "Dark Mode"
        AppLanguage.ARABIC -> "الوضع الداكن"
    }

    val snippetDarkModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "为页面启用深色模式"
        AppLanguage.ENGLISH -> "Enable dark mode for page"
        AppLanguage.ARABIC -> "تمكين الوضع الداكن للصفحة"
    }

    val snippetSepiaMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "护眼模式（暖色）"
        AppLanguage.ENGLISH -> "Eye Protection Mode (Warm)"
        AppLanguage.ARABIC -> "وضع حماية العين (دافئ)"
    }

    val snippetSepiaModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "为页面添加暖色滤镜"
        AppLanguage.ENGLISH -> "Add warm color filter to page"
        AppLanguage.ARABIC -> "إضافة فلتر لون دافئ للصفحة"
    }

    val snippetGrayscale: String get() = when (lang) {
        AppLanguage.CHINESE -> "灰度模式"
        AppLanguage.ENGLISH -> "Grayscale Mode"
        AppLanguage.ARABIC -> "وضع التدرج الرمادي"
    }

    val snippetGrayscaleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将页面转为灰度显示"
        AppLanguage.ENGLISH -> "Convert page to grayscale"
        AppLanguage.ARABIC -> "تحويل الصفحة إلى تدرج رمادي"
    }

    val snippetCustomFont: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义字体"
        AppLanguage.ENGLISH -> "Custom Font"
        AppLanguage.ARABIC -> "خط مخصص"
    }

    val snippetCustomFontDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "替换页面字体"
        AppLanguage.ENGLISH -> "Replace page font"
        AppLanguage.ARABIC -> "استبدال خط الصفحة"
    }

    val snippetFontSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "调整字体大小"
        AppLanguage.ENGLISH -> "Adjust Font Size"
        AppLanguage.ARABIC -> "ضبط حجم الخط"
    }

    val snippetFontSizeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "统一调整页面字体大小"
        AppLanguage.ENGLISH -> "Uniformly adjust page font size"
        AppLanguage.ARABIC -> "ضبط حجم خط الصفحة بشكل موحد"
    }

    val snippetHideScrollbar: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏滚动条"
        AppLanguage.ENGLISH -> "Hide Scrollbar"
        AppLanguage.ARABIC -> "إخفاء شريط التمرير"
    }

    val snippetHideScrollbarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏页面滚动条但保留滚动功能"
        AppLanguage.ENGLISH -> "Hide scrollbar but keep scroll function"
        AppLanguage.ARABIC -> "إخفاء شريط التمرير مع الاحتفاظ بوظيفة التمرير"
    }

    val snippetHighlightLinks: String get() = when (lang) {
        AppLanguage.CHINESE -> "高亮链接"
        AppLanguage.ENGLISH -> "Highlight Links"
        AppLanguage.ARABIC -> "تمييز الروابط"
    }

    val snippetHighlightLinksDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "高亮显示页面所有链接"
        AppLanguage.ENGLISH -> "Highlight all links on page"
        AppLanguage.ARABIC -> "تمييز جميع الروابط في الصفحة"
    }

    val snippetMaxWidth: String get() = when (lang) {
        AppLanguage.CHINESE -> "限制内容宽度"
        AppLanguage.ENGLISH -> "Limit Content Width"
        AppLanguage.ARABIC -> "تحديد عرض المحتوى"
    }

    val snippetMaxWidthDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "限制页面内容最大宽度，提升阅读体验"
        AppLanguage.ENGLISH -> "Limit max content width for better reading"
        AppLanguage.ARABIC -> "تحديد أقصى عرض للمحتوى لقراءة أفضل"
    }

    val snippetLineHeight: String get() = when (lang) {
        AppLanguage.CHINESE -> "调整行高"
        AppLanguage.ENGLISH -> "Adjust Line Height"
        AppLanguage.ARABIC -> "ضبط ارتفاع السطر"
    }

    val snippetLineHeightDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "增加行高提升阅读舒适度"
        AppLanguage.ENGLISH -> "Increase line height for reading comfort"
        AppLanguage.ARABIC -> "زيادة ارتفاع السطر لراحة القراءة"
    }

    val snippetClickEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击事件"
        AppLanguage.ENGLISH -> "Click Event"
        AppLanguage.ARABIC -> "حدث النقر"
    }

    val snippetClickEventDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听元素点击事件"
        AppLanguage.ENGLISH -> "Listen for element click events"
        AppLanguage.ARABIC -> "الاستماع لأحداث نقر العناصر"
    }

    val snippetKeyboardEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "键盘事件"
        AppLanguage.ENGLISH -> "Keyboard Event"
        AppLanguage.ARABIC -> "حدث لوحة المفاتيح"
    }

    val snippetKeyboardEventDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听键盘按键"
        AppLanguage.ENGLISH -> "Listen for keyboard keys"
        AppLanguage.ARABIC -> "الاستماع لمفاتيح لوحة المفاتيح"
    }

    val snippetScrollEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动事件"
        AppLanguage.ENGLISH -> "Scroll Event"
        AppLanguage.ARABIC -> "حدث التمرير"
    }

    val snippetScrollEventDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听页面滚动"
        AppLanguage.ENGLISH -> "Listen for page scroll"
        AppLanguage.ARABIC -> "الاستماع لتمرير الصفحة"
    }

    val snippetMutationEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM 变化监听"
        AppLanguage.ENGLISH -> "DOM Mutation Observer"
        AppLanguage.ARABIC -> "مراقب تغييرات DOM"
    }

    val snippetMutationEventDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听 DOM 结构变化，适合处理动态加载内容"
        AppLanguage.ENGLISH -> "Observe DOM changes, suitable for dynamic content"
        AppLanguage.ARABIC -> "مراقبة تغييرات DOM، مناسب للمحتوى الديناميكي"
    }

    val snippetResizeEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "窗口大小变化"
        AppLanguage.ENGLISH -> "Window Resize"
        AppLanguage.ARABIC -> "تغيير حجم النافذة"
    }

    val snippetResizeEventDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听窗口大小变化"
        AppLanguage.ENGLISH -> "Listen for window resize"
        AppLanguage.ARABIC -> "الاستماع لتغيير حجم النافذة"
    }

    val snippetCopyEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制事件"
        AppLanguage.ENGLISH -> "Copy Event"
        AppLanguage.ARABIC -> "حدث النسخ"
    }

    val snippetCopyEventDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听或拦截复制操作"
        AppLanguage.ENGLISH -> "Listen or intercept copy operation"
        AppLanguage.ARABIC -> "الاستماع أو اعتراض عملية النسخ"
    }

    val snippetContextMenu: String get() = when (lang) {
        AppLanguage.CHINESE -> "右键菜单"
        AppLanguage.ENGLISH -> "Context Menu"
        AppLanguage.ARABIC -> "قائمة السياق"
    }

    val snippetContextMenuDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自定义或禁用右键菜单"
        AppLanguage.ENGLISH -> "Customize or disable context menu"
        AppLanguage.ARABIC -> "تخصيص أو تعطيل قائمة السياق"
    }

    val snippetVisibility: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面可见性变化"
        AppLanguage.ENGLISH -> "Page Visibility Change"
        AppLanguage.ARABIC -> "تغيير رؤية الصفحة"
    }

    val snippetVisibilityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听页面切换到后台/前台"
        AppLanguage.ENGLISH -> "Listen for page background/foreground switch"
        AppLanguage.ARABIC -> "الاستماع لتبديل الصفحة للخلفية/المقدمة"
    }

    val snippetBeforeUnload: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面关闭前"
        AppLanguage.ENGLISH -> "Before Page Unload"
        AppLanguage.ARABIC -> "قبل إغلاق الصفحة"
    }

    val snippetBeforeUnloadDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面关闭前执行操作"
        AppLanguage.ENGLISH -> "Execute operations before page closes"
        AppLanguage.ARABIC -> "تنفيذ العمليات قبل إغلاق الصفحة"
    }

    val snippetTouchEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "触摸事件"
        AppLanguage.ENGLISH -> "Touch Event"
        AppLanguage.ARABIC -> "حدث اللمس"
    }

    val snippetTouchEventDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听触摸操作"
        AppLanguage.ENGLISH -> "Listen for touch operations"
        AppLanguage.ARABIC -> "الاستماع لعمليات اللمس"
    }

    val snippetLongPress: String get() = when (lang) {
        AppLanguage.CHINESE -> "长按事件"
        AppLanguage.ENGLISH -> "Long Press Event"
        AppLanguage.ARABIC -> "حدث الضغط المطول"
    }

    val snippetLongPressDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听长按操作"
        AppLanguage.ENGLISH -> "Listen for long press operations"
        AppLanguage.ARABIC -> "الاستماع لعمليات الضغط المطول"
    }

    val snippetLocalSet: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存到本地存储"
        AppLanguage.ENGLISH -> "Save to Local Storage"
        AppLanguage.ARABIC -> "حفظ في التخزين المحلي"
    }

    val snippetLocalSetDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将数据保存到 localStorage"
        AppLanguage.ENGLISH -> "Save data to localStorage"
        AppLanguage.ARABIC -> "حفظ البيانات في localStorage"
    }

    val snippetLocalGet: String get() = when (lang) {
        AppLanguage.CHINESE -> "从本地存储读取"
        AppLanguage.ENGLISH -> "Read from Local Storage"
        AppLanguage.ARABIC -> "قراءة من التخزين المحلي"
    }

    val snippetLocalGetDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "从 localStorage 读取数据"
        AppLanguage.ENGLISH -> "Read data from localStorage"
        AppLanguage.ARABIC -> "قراءة البيانات من localStorage"
    }

    val snippetSessionStorage: String get() = when (lang) {
        AppLanguage.CHINESE -> "会话存储"
        AppLanguage.ENGLISH -> "Session Storage"
        AppLanguage.ARABIC -> "تخزين الجلسة"
    }

    val snippetSessionStorageDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 sessionStorage 临时存储"
        AppLanguage.ENGLISH -> "Use sessionStorage for temporary storage"
        AppLanguage.ARABIC -> "استخدام sessionStorage للتخزين المؤقت"
    }

    val snippetSetCookie: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置 Cookie"
        AppLanguage.ENGLISH -> "Set Cookie"
        AppLanguage.ARABIC -> "تعيين ملف تعريف الارتباط"
    }

    val snippetSetCookieDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置浏览器 Cookie"
        AppLanguage.ENGLISH -> "Set browser Cookie"
        AppLanguage.ARABIC -> "تعيين ملف تعريف ارتباط المتصفح"
    }

    val snippetGetCookie: String get() = when (lang) {
        AppLanguage.CHINESE -> "读取 Cookie"
        AppLanguage.ENGLISH -> "Get Cookie"
        AppLanguage.ARABIC -> "قراءة ملف تعريف الارتباط"
    }

    val snippetGetCookieDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "读取浏览器 Cookie"
        AppLanguage.ENGLISH -> "Read browser Cookie"
        AppLanguage.ARABIC -> "قراءة ملف تعريف ارتباط المتصفح"
    }

    val snippetDeleteCookie: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除 Cookie"
        AppLanguage.ENGLISH -> "Delete Cookie"
        AppLanguage.ARABIC -> "حذف ملف تعريف الارتباط"
    }

    val snippetDeleteCookieDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "删除指定 Cookie"
        AppLanguage.ENGLISH -> "Delete specified Cookie"
        AppLanguage.ARABIC -> "حذف ملف تعريف الارتباط المحدد"
    }

    val snippetIndexedDB: String get() = when (lang) {
        AppLanguage.CHINESE -> "IndexedDB 存储"
        AppLanguage.ENGLISH -> "IndexedDB Storage"
        AppLanguage.ARABIC -> "تخزين IndexedDB"
    }

    val snippetIndexedDBDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 IndexedDB 存储大量数据"
        AppLanguage.ENGLISH -> "Use IndexedDB for large data storage"
        AppLanguage.ARABIC -> "استخدام IndexedDB لتخزين كميات كبيرة من البيانات"
    }

    val snippetGetRequest: String get() = when (lang) {
        AppLanguage.CHINESE -> "GET 请求"
        AppLanguage.ENGLISH -> "GET Request"
        AppLanguage.ARABIC -> "طلب GET"
    }

    val snippetGetRequestDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送 GET 请求获取数据"
        AppLanguage.ENGLISH -> "Send GET request to fetch data"
        AppLanguage.ARABIC -> "إرسال طلب GET لجلب البيانات"
    }

    val snippetPostRequest: String get() = when (lang) {
        AppLanguage.CHINESE -> "POST 请求"
        AppLanguage.ENGLISH -> "POST Request"
        AppLanguage.ARABIC -> "طلب POST"
    }

    val snippetPostRequestDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送 POST 请求提交数据"
        AppLanguage.ENGLISH -> "Send POST request to submit data"
        AppLanguage.ARABIC -> "إرسال طلب POST لإرسال البيانات"
    }

    val snippetTimeoutRequest: String get() = when (lang) {
        AppLanguage.CHINESE -> "带超时的请求"
        AppLanguage.ENGLISH -> "Request with Timeout"
        AppLanguage.ARABIC -> "طلب مع مهلة"
    }

    val snippetTimeoutRequestDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置请求超时时间"
        AppLanguage.ENGLISH -> "Set request timeout"
        AppLanguage.ARABIC -> "تعيين مهلة الطلب"
    }

    val snippetRetryRequest: String get() = when (lang) {
        AppLanguage.CHINESE -> "请求重试"
        AppLanguage.ENGLISH -> "Request Retry"
        AppLanguage.ARABIC -> "إعادة محاولة الطلب"
    }

    val snippetRetryRequestDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "失败后自动重试"
        AppLanguage.ENGLISH -> "Auto retry on failure"
        AppLanguage.ARABIC -> "إعادة المحاولة تلقائياً عند الفشل"
    }

    val snippetDownloadFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载文件"
        AppLanguage.ENGLISH -> "Download File"
        AppLanguage.ARABIC -> "تنزيل الملف"
    }

    val snippetDownloadFileDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载文件到本地"
        AppLanguage.ENGLISH -> "Download file to local"
        AppLanguage.ARABIC -> "تنزيل الملف محلياً"
    }

    val snippetJsonp: String get() = when (lang) {
        AppLanguage.CHINESE -> "JSONP 请求"
        AppLanguage.ENGLISH -> "JSONP Request"
        AppLanguage.ARABIC -> "طلب JSONP"
    }

    val snippetJsonpDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "跨域 JSONP 请求"
        AppLanguage.ENGLISH -> "Cross-domain JSONP request"
        AppLanguage.ARABIC -> "طلب JSONP عبر النطاقات"
    }

    val snippetExtractTable: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取表格数据"
        AppLanguage.ENGLISH -> "Extract Table Data"
        AppLanguage.ARABIC -> "استخراج بيانات الجدول"
    }

    val snippetExtractTableDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 HTML 表格转换为 JSON"
        AppLanguage.ENGLISH -> "Convert HTML table to JSON"
        AppLanguage.ARABIC -> "تحويل جدول HTML إلى JSON"
    }

    val snippetExtractLinks: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取所有链接"
        AppLanguage.ENGLISH -> "Extract All Links"
        AppLanguage.ARABIC -> "استخراج جميع الروابط"
    }

    val snippetExtractLinksDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取页面所有链接"
        AppLanguage.ENGLISH -> "Extract all links from page"
        AppLanguage.ARABIC -> "استخراج جميع الروابط من الصفحة"
    }

    val snippetExtractImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取所有图片"
        AppLanguage.ENGLISH -> "Extract All Images"
        AppLanguage.ARABIC -> "استخراج جميع الصور"
    }

    val snippetExtractImagesDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取页面所有图片地址"
        AppLanguage.ENGLISH -> "Extract all image URLs from page"
        AppLanguage.ARABIC -> "استخراج جميع عناوين الصور من الصفحة"
    }

    val snippetExportJson: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出为 JSON"
        AppLanguage.ENGLISH -> "Export as JSON"
        AppLanguage.ARABIC -> "تصدير كـ JSON"
    }

    val snippetExportJsonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将数据导出为 JSON 文件"
        AppLanguage.ENGLISH -> "Export data as JSON file"
        AppLanguage.ARABIC -> "تصدير البيانات كملف JSON"
    }

    val snippetExportCsv: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出为 CSV"
        AppLanguage.ENGLISH -> "Export as CSV"
        AppLanguage.ARABIC -> "تصدير كـ CSV"
    }

    val snippetExportCsvDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将数据导出为 CSV 文件"
        AppLanguage.ENGLISH -> "Export data as CSV file"
        AppLanguage.ARABIC -> "تصدير البيانات كملف CSV"
    }

    val snippetParseUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "解析 URL 参数"
        AppLanguage.ENGLISH -> "Parse URL Parameters"
        AppLanguage.ARABIC -> "تحليل معلمات URL"
    }

    val snippetParseUrlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "解析 URL 查询参数"
        AppLanguage.ENGLISH -> "Parse URL query parameters"
        AppLanguage.ARABIC -> "تحليل معلمات استعلام URL"
    }

    val snippetBuildUrl: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建 URL"
        AppLanguage.ENGLISH -> "Build URL"
        AppLanguage.ARABIC -> "بناء URL"
    }

    val snippetBuildUrlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建带参数的 URL"
        AppLanguage.ENGLISH -> "Build URL with parameters"
        AppLanguage.ARABIC -> "بناء URL مع المعلمات"
    }

    val snippetFloatingButton: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮按钮"
        AppLanguage.ENGLISH -> "Floating Button"
        AppLanguage.ARABIC -> "زر عائم"
    }

    val snippetFloatingButtonDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建一个悬浮操作按钮"
        AppLanguage.ENGLISH -> "Create a floating action button"
        AppLanguage.ARABIC -> "إنشاء زر إجراء عائم"
    }

    val snippetToastUi: String get() = when (lang) {
        AppLanguage.CHINESE -> "Toast 提示"
        AppLanguage.ENGLISH -> "Toast Message"
        AppLanguage.ARABIC -> "رسالة Toast"
    }

    val snippetToastUiDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示短暂的提示消息"
        AppLanguage.ENGLISH -> "Show brief toast message"
        AppLanguage.ARABIC -> "عرض رسالة Toast قصيرة"
    }

    val snippetModal: String get() = when (lang) {
        AppLanguage.CHINESE -> "模态弹窗"
        AppLanguage.ENGLISH -> "Modal Dialog"
        AppLanguage.ARABIC -> "نافذة حوار نموذجية"
    }

    val snippetModalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建模态对话框"
        AppLanguage.ENGLISH -> "Create modal dialog"
        AppLanguage.ARABIC -> "إنشاء نافذة حوار نموذجية"
    }

    val snippetProgressBar: String get() = when (lang) {
        AppLanguage.CHINESE -> "阅读进度条"
        AppLanguage.ENGLISH -> "Reading Progress Bar"
        AppLanguage.ARABIC -> "شريط تقدم القراءة"
    }

    val snippetProgressBarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示页面阅读进度"
        AppLanguage.ENGLISH -> "Show page reading progress"
        AppLanguage.ARABIC -> "عرض تقدم قراءة الصفحة"
    }

    val snippetLoading: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载动画"
        AppLanguage.ENGLISH -> "Loading Animation"
        AppLanguage.ARABIC -> "رسوم متحركة للتحميل"
    }

    val snippetLoadingDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示加载中动画"
        AppLanguage.ENGLISH -> "Show loading animation"
        AppLanguage.ARABIC -> "عرض رسوم التحميل المتحركة"
    }

    val snippetSnackbar: String get() = when (lang) {
        AppLanguage.CHINESE -> "Snackbar 通知"
        AppLanguage.ENGLISH -> "Snackbar Notification"
        AppLanguage.ARABIC -> "إشعار Snackbar"
    }

    val snippetSnackbarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "底部滑出通知"
        AppLanguage.ENGLISH -> "Bottom slide-out notification"
        AppLanguage.ARABIC -> "إشعار منزلق من الأسفل"
    }

    val snippetToolbar: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮工具栏"
        AppLanguage.ENGLISH -> "Floating Toolbar"
        AppLanguage.ARABIC -> "شريط أدوات عائم"
    }

    val snippetToolbarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建可拖动的悬浮工具栏"
        AppLanguage.ENGLISH -> "Create draggable floating toolbar"
        AppLanguage.ARABIC -> "إنشاء شريط أدوات عائم قابل للسحب"
    }

    val snippetSidebar: String get() = when (lang) {
        AppLanguage.CHINESE -> "侧边栏面板"
        AppLanguage.ENGLISH -> "Sidebar Panel"
        AppLanguage.ARABIC -> "لوحة الشريط الجانبي"
    }

    val snippetSidebarDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建可展开的侧边栏"
        AppLanguage.ENGLISH -> "Create expandable sidebar"
        AppLanguage.ARABIC -> "إنشاء شريط جانبي قابل للتوسيع"
    }

    val snippetDraggable: String get() = when (lang) {
        AppLanguage.CHINESE -> "可拖动元素"
        AppLanguage.ENGLISH -> "Draggable Element"
        AppLanguage.ARABIC -> "عنصر قابل للسحب"
    }

    val snippetDraggableDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使元素可拖动"
        AppLanguage.ENGLISH -> "Make element draggable"
        AppLanguage.ARABIC -> "جعل العنصر قابلاً للسحب"
    }

    val snippetMiniPlayer: String get() = when (lang) {
        AppLanguage.CHINESE -> "迷你播放器"
        AppLanguage.ENGLISH -> "Mini Player"
        AppLanguage.ARABIC -> "مشغل صغير"
    }

    val snippetMiniPlayerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建悬浮迷你播放器"
        AppLanguage.ENGLISH -> "Create floating mini player"
        AppLanguage.ARABIC -> "إنشاء مشغل صغير عائم"
    }

    val snippetBrowserNotif: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器通知"
        AppLanguage.ENGLISH -> "Browser Notification"
        AppLanguage.ARABIC -> "إشعار المتصفح"
    }

    val snippetBrowserNotifDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "发送浏览器原生通知"
        AppLanguage.ENGLISH -> "Send browser native notification"
        AppLanguage.ARABIC -> "إرسال إشعار المتصفح الأصلي"
    }

    val snippetBadge: String get() = when (lang) {
        AppLanguage.CHINESE -> "角标提醒"
        AppLanguage.ENGLISH -> "Badge Notification"
        AppLanguage.ARABIC -> "إشعار الشارة"
    }

    val snippetBadgeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "在元素上显示数字角标"
        AppLanguage.ENGLISH -> "Show number badge on element"
        AppLanguage.ARABIC -> "عرض شارة رقمية على العنصر"
    }

    val snippetBanner: String get() = when (lang) {
        AppLanguage.CHINESE -> "顶部横幅提醒"
        AppLanguage.ENGLISH -> "Top Banner Alert"
        AppLanguage.ARABIC -> "تنبيه اللافتة العلوية"
    }

    val snippetBannerDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示顶部横幅通知"
        AppLanguage.ENGLISH -> "Show top banner notification"
        AppLanguage.ARABIC -> "عرض إشعار اللافتة العلوية"
    }

    val snippetScrollToTop: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动到顶部"
        AppLanguage.ENGLISH -> "Scroll to Top"
        AppLanguage.ARABIC -> "التمرير إلى الأعلى"
    }

    val snippetScrollToTopDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "平滑滚动到页面顶部"
        AppLanguage.ENGLISH -> "Smooth scroll to page top"
        AppLanguage.ARABIC -> "التمرير السلس إلى أعلى الصفحة"
    }

    val snippetScrollToBottom: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动到底部"
        AppLanguage.ENGLISH -> "Scroll to Bottom"
        AppLanguage.ARABIC -> "التمرير إلى الأسفل"
    }

    val snippetScrollToBottomDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "平滑滚动到页面底部"
        AppLanguage.ENGLISH -> "Smooth scroll to page bottom"
        AppLanguage.ARABIC -> "التمرير السلس إلى أسفل الصفحة"
    }

    val snippetScrollToElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动到元素"
        AppLanguage.ENGLISH -> "Scroll to Element"
        AppLanguage.ARABIC -> "التمرير إلى العنصر"
    }

    val snippetScrollToElementDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动到指定元素位置"
        AppLanguage.ENGLISH -> "Scroll to specified element position"
        AppLanguage.ARABIC -> "التمرير إلى موضع العنصر المحدد"
    }

    val snippetAutoScroll: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动滚动"
        AppLanguage.ENGLISH -> "Auto Scroll"
        AppLanguage.ARABIC -> "التمرير التلقائي"
    }

    val snippetAutoScrollDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动滚动页面"
        AppLanguage.ENGLISH -> "Auto scroll page"
        AppLanguage.ARABIC -> "التمرير التلقائي للصفحة"
    }

    val snippetBackToTopBtn: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回顶部按钮"
        AppLanguage.ENGLISH -> "Back to Top Button"
        AppLanguage.ARABIC -> "زر العودة للأعلى"
    }

    val snippetBackToTopBtnDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "添加返回顶部悬浮按钮"
        AppLanguage.ENGLISH -> "Add floating back to top button"
        AppLanguage.ARABIC -> "إضافة زر عائم للعودة للأعلى"
    }

    val snippetInfiniteScroll: String get() = when (lang) {
        AppLanguage.CHINESE -> "无限滚动加载"
        AppLanguage.ENGLISH -> "Infinite Scroll Load"
        AppLanguage.ARABIC -> "تحميل التمرير اللانهائي"
    }

    val snippetInfiniteScrollDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动到底部时自动加载更多"
        AppLanguage.ENGLISH -> "Auto load more when scrolling to bottom"
        AppLanguage.ARABIC -> "تحميل المزيد تلقائياً عند التمرير للأسفل"
    }

    val snippetScrollReveal: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动显示动画"
        AppLanguage.ENGLISH -> "Scroll Reveal Animation"
        AppLanguage.ARABIC -> "رسوم متحركة للكشف عند التمرير"
    }

    val snippetScrollRevealDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "元素滚动到视口时显示动画"
        AppLanguage.ENGLISH -> "Animate elements when scrolled into viewport"
        AppLanguage.ARABIC -> "تحريك العناصر عند التمرير إلى منفذ العرض"
    }

    val snippetScrollSpy: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动监听导航"
        AppLanguage.ENGLISH -> "Scroll Spy Navigation"
        AppLanguage.ARABIC -> "مراقبة التمرير للتنقل"
    }

    val snippetScrollSpyDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "根据滚动位置高亮导航项"
        AppLanguage.ENGLISH -> "Highlight nav items based on scroll position"
        AppLanguage.ARABIC -> "تمييز عناصر التنقل بناءً على موضع التمرير"
    }

    val snippetForm: String get() = when (lang) {
        AppLanguage.CHINESE -> "表单操作"
        AppLanguage.ENGLISH -> "Form Operations"
        AppLanguage.ARABIC -> "عمليات النموذج"
    }

    val snippetFormDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "表单填充、验证、提交等操作"
        AppLanguage.ENGLISH -> "Form filling, validation, submission operations"
        AppLanguage.ARABIC -> "عمليات ملء النموذج والتحقق والإرسال"
    }

    val snippetAutoFill: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动填充表单"
        AppLanguage.ENGLISH -> "Auto Fill Form"
        AppLanguage.ARABIC -> "ملء النموذج تلقائياً"
    }

    val snippetAutoFillDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动填充表单字段"
        AppLanguage.ENGLISH -> "Auto fill form fields"
        AppLanguage.ARABIC -> "ملء حقول النموذج تلقائياً"
    }

    val snippetGetFormData: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取表单数据"
        AppLanguage.ENGLISH -> "Get Form Data"
        AppLanguage.ARABIC -> "الحصول على بيانات النموذج"
    }

    val snippetGetFormDataDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取表单所有字段值"
        AppLanguage.ENGLISH -> "Get all form field values"
        AppLanguage.ARABIC -> "الحصول على جميع قيم حقول النموذج"
    }

    val snippetFormValidate: String get() = when (lang) {
        AppLanguage.CHINESE -> "表单验证"
        AppLanguage.ENGLISH -> "Form Validation"
        AppLanguage.ARABIC -> "التحقق من النموذج"
    }

    val snippetFormValidateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证表单字段"
        AppLanguage.ENGLISH -> "Validate form fields"
        AppLanguage.ARABIC -> "التحقق من حقول النموذج"
    }

    val snippetFormIntercept: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截表单提交"
        AppLanguage.ENGLISH -> "Intercept Form Submit"
        AppLanguage.ARABIC -> "اعتراض إرسال النموذج"
    }

    val snippetFormInterceptDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截并处理表单提交"
        AppLanguage.ENGLISH -> "Intercept and handle form submission"
        AppLanguage.ARABIC -> "اعتراض ومعالجة إرسال النموذج"
    }

    val snippetFormClear: String get() = when (lang) {
        AppLanguage.CHINESE -> "清空表单"
        AppLanguage.ENGLISH -> "Clear Form"
        AppLanguage.ARABIC -> "مسح النموذج"
    }

    val snippetFormClearDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "清空表单所有字段"
        AppLanguage.ENGLISH -> "Clear all form fields"
        AppLanguage.ARABIC -> "مسح جميع حقول النموذج"
    }

    val snippetPasswordToggle: String get() = when (lang) {
        AppLanguage.CHINESE -> "密码显示切换"
        AppLanguage.ENGLISH -> "Password Toggle"
        AppLanguage.ARABIC -> "تبديل عرض كلمة المرور"
    }

    val snippetPasswordToggleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "切换密码显示/隐藏"
        AppLanguage.ENGLISH -> "Toggle password show/hide"
        AppLanguage.ARABIC -> "تبديل إظهار/إخفاء كلمة المرور"
    }

    val snippetMedia: String get() = when (lang) {
        AppLanguage.CHINESE -> "媒体操作"
        AppLanguage.ENGLISH -> "Media Operations"
        AppLanguage.ARABIC -> "عمليات الوسائط"
    }

    val snippetMediaDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频、音频、图片等媒体控制"
        AppLanguage.ENGLISH -> "Video, audio, image media control"
        AppLanguage.ARABIC -> "التحكم في وسائط الفيديو والصوت والصور"
    }

    val snippetVideoSpeed: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频倍速控制"
        AppLanguage.ENGLISH -> "Video Speed Control"
        AppLanguage.ARABIC -> "التحكم في سرعة الفيديو"
    }

    val snippetVideoSpeedDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制视频播放速度"
        AppLanguage.ENGLISH -> "Control video playback speed"
        AppLanguage.ARABIC -> "التحكم في سرعة تشغيل الفيديو"
    }

    val snippetPiP: String get() = when (lang) {
        AppLanguage.CHINESE -> "画中画模式"
        AppLanguage.ENGLISH -> "Picture in Picture"
        AppLanguage.ARABIC -> "صورة في صورة"
    }

    val snippetPiPDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "启用视频画中画"
        AppLanguage.ENGLISH -> "Enable video picture-in-picture"
        AppLanguage.ARABIC -> "تمكين صورة داخل صورة للفيديو"
    }

    val snippetVideoScreenshot: String get() = when (lang) {
        AppLanguage.CHINESE -> "视频截图"
        AppLanguage.ENGLISH -> "Video Screenshot"
        AppLanguage.ARABIC -> "لقطة شاشة الفيديو"
    }

    val snippetVideoScreenshotDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "截取视频当前帧"
        AppLanguage.ENGLISH -> "Capture current video frame"
        AppLanguage.ARABIC -> "التقاط الإطار الحالي للفيديو"
    }

    val snippetImageZoom: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片放大查看"
        AppLanguage.ENGLISH -> "Image Zoom View"
        AppLanguage.ARABIC -> "عرض الصورة مكبرة"
    }

    val snippetImageZoomDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击图片放大显示"
        AppLanguage.ENGLISH -> "Click image to zoom"
        AppLanguage.ARABIC -> "انقر على الصورة للتكبير"
    }

    val snippetDownloadImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "批量下载图片"
        AppLanguage.ENGLISH -> "Batch Download Images"
        AppLanguage.ARABIC -> "تنزيل الصور دفعة واحدة"
    }

    val snippetDownloadImagesDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "下载页面所有图片"
        AppLanguage.ENGLISH -> "Download all images from page"
        AppLanguage.ARABIC -> "تنزيل جميع الصور من الصفحة"
    }

    val snippetAudioControl: String get() = when (lang) {
        AppLanguage.CHINESE -> "音频控制"
        AppLanguage.ENGLISH -> "Audio Control"
        AppLanguage.ARABIC -> "التحكم في الصوت"
    }

    val snippetAudioControlDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制页面音频播放"
        AppLanguage.ENGLISH -> "Control page audio playback"
        AppLanguage.ARABIC -> "التحكم في تشغيل صوت الصفحة"
    }

    val snippetLazyLoad: String get() = when (lang) {
        AppLanguage.CHINESE -> "图片懒加载"
        AppLanguage.ENGLISH -> "Image Lazy Load"
        AppLanguage.ARABIC -> "التحميل الكسول للصور"
    }

    val snippetLazyLoadDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "实现图片懒加载"
        AppLanguage.ENGLISH -> "Implement image lazy loading"
        AppLanguage.ARABIC -> "تنفيذ التحميل الكسول للصور"
    }

    val snippetFullscreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏控制"
        AppLanguage.ENGLISH -> "Fullscreen Control"
        AppLanguage.ARABIC -> "التحكم في ملء الشاشة"
    }

    val snippetFullscreenDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制元素全屏显示"
        AppLanguage.ENGLISH -> "Control element fullscreen display"
        AppLanguage.ARABIC -> "التحكم في عرض العنصر بملء الشاشة"
    }

    val snippetEnhance: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面增强"
        AppLanguage.ENGLISH -> "Page Enhancement"
        AppLanguage.ARABIC -> "تحسين الصفحة"
    }

    val snippetEnhanceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "页面功能增强和优化"
        AppLanguage.ENGLISH -> "Page feature enhancement and optimization"
        AppLanguage.ARABIC -> "تحسين وتعزيز ميزات الصفحة"
    }

    val snippetReadingMode: String get() = when (lang) {
        AppLanguage.CHINESE -> "阅读模式"
        AppLanguage.ENGLISH -> "Reading Mode"
        AppLanguage.ARABIC -> "وضع القراءة"
    }

    val snippetReadingModeDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取正文，简化页面"
        AppLanguage.ENGLISH -> "Extract content, simplify page"
        AppLanguage.ARABIC -> "استخراج المحتوى وتبسيط الصفحة"
    }

    val snippetCopyUnlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "解除复制限制"
        AppLanguage.ENGLISH -> "Unlock Copy Restriction"
        AppLanguage.ARABIC -> "إلغاء قيود النسخ"
    }

    val snippetCopyUnlockDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "移除网页复制保护"
        AppLanguage.ENGLISH -> "Remove webpage copy protection"
        AppLanguage.ARABIC -> "إزالة حماية نسخ صفحة الويب"
    }

    val snippetPrintFriendly: String get() = when (lang) {
        AppLanguage.CHINESE -> "打印优化"
        AppLanguage.ENGLISH -> "Print Friendly"
        AppLanguage.ARABIC -> "تحسين الطباعة"
    }

    val snippetPrintFriendlyDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化页面打印效果"
        AppLanguage.ENGLISH -> "Optimize page print output"
        AppLanguage.ARABIC -> "تحسين إخراج طباعة الصفحة"
    }

    val snippetTextToSpeech: String get() = when (lang) {
        AppLanguage.CHINESE -> "文字转语音"
        AppLanguage.ENGLISH -> "Text to Speech"
        AppLanguage.ARABIC -> "النص إلى كلام"
    }

    val snippetTextToSpeechDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "朗读选中文字"
        AppLanguage.ENGLISH -> "Read selected text aloud"
        AppLanguage.ARABIC -> "قراءة النص المحدد بصوت عالٍ"
    }

    val snippetWordCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "字数统计"
        AppLanguage.ENGLISH -> "Word Count"
        AppLanguage.ARABIC -> "عدد الكلمات"
    }

    val snippetWordCountDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "统计页面文字数量"
        AppLanguage.ENGLISH -> "Count page text quantity"
        AppLanguage.ARABIC -> "إحصاء كمية نص الصفحة"
    }

    val snippetHighlightSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "页内搜索高亮"
        AppLanguage.ENGLISH -> "In-page Search Highlight"
        AppLanguage.ARABIC -> "تمييز البحث في الصفحة"
    }

    val snippetHighlightSearchDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索并高亮关键词"
        AppLanguage.ENGLISH -> "Search and highlight keywords"
        AppLanguage.ARABIC -> "البحث وتمييز الكلمات المفتاحية"
    }

    val snippetHideAds: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏常见广告"
        AppLanguage.ENGLISH -> "Hide Common Ads"
        AppLanguage.ARABIC -> "إخفاء الإعلانات الشائعة"
    }

    val snippetHideAdsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏常见的广告元素"
        AppLanguage.ENGLISH -> "Hide common ad elements"
        AppLanguage.ARABIC -> "إخفاء عناصر الإعلانات الشائعة"
    }

    val snippetFilter: String get() = when (lang) {
        AppLanguage.CHINESE -> "内容过滤"
        AppLanguage.ENGLISH -> "Content Filter"
        AppLanguage.ARABIC -> "تصفية المحتوى"
    }

    val snippetFilterDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "过滤和筛选页面内容"
        AppLanguage.ENGLISH -> "Filter and screen page content"
        AppLanguage.ARABIC -> "تصفية وفحص محتوى الصفحة"
    }

    val snippetKeywordFilter: String get() = when (lang) {
        AppLanguage.CHINESE -> "关键词过滤"
        AppLanguage.ENGLISH -> "Keyword Filter"
        AppLanguage.ARABIC -> "تصفية الكلمات المفتاحية"
    }

    val snippetKeywordFilterDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏包含特定关键词的元素"
        AppLanguage.ENGLISH -> "Hide elements containing specific keywords"
        AppLanguage.ARABIC -> "إخفاء العناصر التي تحتوي على كلمات مفتاحية معينة"
    }

    val snippetRemoveEmpty: String get() = when (lang) {
        AppLanguage.CHINESE -> "移除空元素"
        AppLanguage.ENGLISH -> "Remove Empty Elements"
        AppLanguage.ARABIC -> "إزالة العناصر الفارغة"
    }

    val snippetRemoveEmptyDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "移除页面中的空元素"
        AppLanguage.ENGLISH -> "Remove empty elements from page"
        AppLanguage.ARABIC -> "إزالة العناصر الفارغة من الصفحة"
    }

    val snippetFilterComments: String get() = when (lang) {
        AppLanguage.CHINESE -> "过滤评论"
        AppLanguage.ENGLISH -> "Filter Comments"
        AppLanguage.ARABIC -> "تصفية التعليقات"
    }

    val snippetFilterCommentsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "根据条件过滤评论"
        AppLanguage.ENGLISH -> "Filter comments by conditions"
        AppLanguage.ARABIC -> "تصفية التعليقات حسب الشروط"
    }

    val snippetFilterSmallImages: String get() = when (lang) {
        AppLanguage.CHINESE -> "过滤小图片"
        AppLanguage.ENGLISH -> "Filter Small Images"
        AppLanguage.ARABIC -> "تصفية الصور الصغيرة"
    }

    val snippetFilterSmallImagesDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏尺寸过小的图片"
        AppLanguage.ENGLISH -> "Hide images with small dimensions"
        AppLanguage.ARABIC -> "إخفاء الصور ذات الأبعاد الصغيرة"
    }

    val snippetAdBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "广告拦截"
        AppLanguage.ENGLISH -> "Ad Blocker"
        AppLanguage.ARABIC -> "حظر الإعلانات"
    }

    val snippetAdBlockDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏蔽广告和弹窗"
        AppLanguage.ENGLISH -> "Block ads and popups"
        AppLanguage.ARABIC -> "حظر الإعلانات والنوافذ المنبثقة"
    }

    val snippetBlockPopup: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止弹窗"
        AppLanguage.ENGLISH -> "Block Popups"
        AppLanguage.ARABIC -> "حظر النوافذ المنبثقة"
    }

    val snippetBlockPopupDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止弹窗和新窗口"
        AppLanguage.ENGLISH -> "Block popups and new windows"
        AppLanguage.ARABIC -> "حظر النوافذ المنبثقة والنوافذ الجديدة"
    }

    val snippetRemoveOverlay: String get() = when (lang) {
        AppLanguage.CHINESE -> "移除遮罩层"
        AppLanguage.ENGLISH -> "Remove Overlay"
        AppLanguage.ARABIC -> "إزالة طبقة التغطية"
    }

    val snippetRemoveOverlayDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "移除阻止阅读的遮罩"
        AppLanguage.ENGLISH -> "Remove overlays blocking reading"
        AppLanguage.ARABIC -> "إزالة طبقات التغطية التي تحجب القراءة"
    }

    val snippetCssAdBlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS 广告屏蔽"
        AppLanguage.ENGLISH -> "CSS Ad Blocker"
        AppLanguage.ARABIC -> "حظر الإعلانات بـ CSS"
    }

    val snippetCssAdBlockDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "使用 CSS 隐藏广告"
        AppLanguage.ENGLISH -> "Use CSS to hide ads"
        AppLanguage.ARABIC -> "استخدام CSS لإخفاء الإعلانات"
    }

    val snippetAntiAdblock: String get() = when (lang) {
        AppLanguage.CHINESE -> "反反广告检测"
        AppLanguage.ENGLISH -> "Anti-Adblock Detection"
        AppLanguage.ARABIC -> "مكافحة كشف حظر الإعلانات"
    }

    val snippetAntiAdblockDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "绕过广告拦截检测"
        AppLanguage.ENGLISH -> "Bypass ad blocker detection"
        AppLanguage.ARABIC -> "تجاوز كشف حظر الإعلانات"
    }

    val snippetUtility: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具函数"
        AppLanguage.ENGLISH -> "Utility Functions"
        AppLanguage.ARABIC -> "دوال مساعدة"
    }

    val snippetUtilityDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "常用的工具函数和辅助方法"
        AppLanguage.ENGLISH -> "Common utility functions and helper methods"
        AppLanguage.ARABIC -> "دوال مساعدة شائعة وطرق مساندة"
    }

    val snippetDebounce: String get() = when (lang) {
        AppLanguage.CHINESE -> "防抖函数"
        AppLanguage.ENGLISH -> "Debounce Function"
        AppLanguage.ARABIC -> "دالة منع الارتداد"
    }

    val snippetDebounceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "限制函数执行频率（延迟执行）"
        AppLanguage.ENGLISH -> "Limit function execution frequency (delayed)"
        AppLanguage.ARABIC -> "تحديد تردد تنفيذ الدالة (مؤجل)"
    }

    val snippetThrottle: String get() = when (lang) {
        AppLanguage.CHINESE -> "节流函数"
        AppLanguage.ENGLISH -> "Throttle Function"
        AppLanguage.ARABIC -> "دالة الخنق"
    }

    val snippetThrottleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "限制函数执行频率（固定间隔）"
        AppLanguage.ENGLISH -> "Limit function execution frequency (fixed interval)"
        AppLanguage.ARABIC -> "تحديد تردد تنفيذ الدالة (فاصل ثابت)"
    }

    val snippetWaitElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "等待元素出现"
        AppLanguage.ENGLISH -> "Wait for Element"
        AppLanguage.ARABIC -> "انتظار ظهور العنصر"
    }

    val snippetWaitElementDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "等待指定元素出现在页面中"
        AppLanguage.ENGLISH -> "Wait for specified element to appear"
        AppLanguage.ARABIC -> "انتظار ظهور العنصر المحدد"
    }

    val snippetCopyText: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制文本"
        AppLanguage.ENGLISH -> "Copy Text"
        AppLanguage.ARABIC -> "نسخ النص"
    }

    val snippetCopyTextDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "复制文本到剪贴板"
        AppLanguage.ENGLISH -> "Copy text to clipboard"
        AppLanguage.ARABIC -> "نسخ النص إلى الحافظة"
    }

    val snippetFormatDate: String get() = when (lang) {
        AppLanguage.CHINESE -> "格式化日期"
        AppLanguage.ENGLISH -> "Format Date"
        AppLanguage.ARABIC -> "تنسيق التاريخ"
    }

    val snippetFormatDateDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将日期格式化为指定格式"
        AppLanguage.ENGLISH -> "Format date to specified format"
        AppLanguage.ARABIC -> "تنسيق التاريخ بالشكل المحدد"
    }

    val snippetRandomString: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成随机字符串"
        AppLanguage.ENGLISH -> "Generate Random String"
        AppLanguage.ARABIC -> "إنشاء سلسلة عشوائية"
    }

    val snippetRandomStringDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "生成指定长度的随机字符串"
        AppLanguage.ENGLISH -> "Generate random string of specified length"
        AppLanguage.ARABIC -> "إنشاء سلسلة عشوائية بطول محدد"
    }

    val snippetSleep: String get() = when (lang) {
        AppLanguage.CHINESE -> "延迟执行"
        AppLanguage.ENGLISH -> "Sleep/Delay"
        AppLanguage.ARABIC -> "تأخير التنفيذ"
    }

    val snippetSleepDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "异步延迟指定时间"
        AppLanguage.ENGLISH -> "Async delay for specified time"
        AppLanguage.ARABIC -> "تأخير غير متزامن لوقت محدد"
    }

    val snippetRetry: String get() = when (lang) {
        AppLanguage.CHINESE -> "重试函数"
        AppLanguage.ENGLISH -> "Retry Function"
        AppLanguage.ARABIC -> "دالة إعادة المحاولة"
    }

    val snippetRetryDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "失败后自动重试"
        AppLanguage.ENGLISH -> "Auto retry on failure"
        AppLanguage.ARABIC -> "إعادة المحاولة تلقائياً عند الفشل"
    }

    val snippetText: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本处理"
        AppLanguage.ENGLISH -> "Text Processing"
        AppLanguage.ARABIC -> "معالجة النص"
    }

    val snippetTextDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本提取、转换、处理"
        AppLanguage.ENGLISH -> "Text extraction, conversion, processing"
        AppLanguage.ARABIC -> "استخراج النص وتحويله ومعالجته"
    }

    val snippetExtractArticle: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取文章正文"
        AppLanguage.ENGLISH -> "Extract Article Content"
        AppLanguage.ARABIC -> "استخراج محتوى المقال"
    }

    val snippetExtractArticleDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "智能提取页面正文内容"
        AppLanguage.ENGLISH -> "Intelligently extract page content"
        AppLanguage.ARABIC -> "استخراج محتوى الصفحة بذكاء"
    }

    val snippetReplaceText: String get() = when (lang) {
        AppLanguage.CHINESE -> "批量替换文本"
        AppLanguage.ENGLISH -> "Batch Replace Text"
        AppLanguage.ARABIC -> "استبدال النص دفعة واحدة"
    }

    val snippetReplaceTextDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "替换页面中的文本"
        AppLanguage.ENGLISH -> "Replace text in page"
        AppLanguage.ARABIC -> "استبدال النص في الصفحة"
    }

    val snippetTranslateSelection: String get() = when (lang) {
        AppLanguage.CHINESE -> "选中文字翻译"
        AppLanguage.ENGLISH -> "Translate Selection"
        AppLanguage.ARABIC -> "ترجمة النص المحدد"
    }

    val snippetTranslateSelectionDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "选中文字后显示翻译"
        AppLanguage.ENGLISH -> "Show translation for selected text"
        AppLanguage.ARABIC -> "عرض الترجمة للنص المحدد"
    }

    val snippetHtmlToMarkdown: String get() = when (lang) {
        AppLanguage.CHINESE -> "HTML 转 Markdown"
        AppLanguage.ENGLISH -> "HTML to Markdown"
        AppLanguage.ARABIC -> "تحويل HTML إلى Markdown"
    }

    val snippetHtmlToMarkdownDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "将 HTML 转换为 Markdown"
        AppLanguage.ENGLISH -> "Convert HTML to Markdown"
        AppLanguage.ARABIC -> "تحويل HTML إلى Markdown"
    }

    val snippetIntercept: String get() = when (lang) {
        AppLanguage.CHINESE -> "请求拦截"
        AppLanguage.ENGLISH -> "Request Intercept"
        AppLanguage.ARABIC -> "اعتراض الطلبات"
    }

    val snippetInterceptDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截和修改网络请求"
        AppLanguage.ENGLISH -> "Intercept and modify network requests"
        AppLanguage.ARABIC -> "اعتراض وتعديل طلبات الشبكة"
    }

    val snippetInterceptFetch: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截 Fetch 请求"
        AppLanguage.ENGLISH -> "Intercept Fetch Request"
        AppLanguage.ARABIC -> "اعتراض طلب Fetch"
    }

    val snippetInterceptFetchDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截和修改 fetch 请求"
        AppLanguage.ENGLISH -> "Intercept and modify fetch requests"
        AppLanguage.ARABIC -> "اعتراض وتعديل طلبات fetch"
    }

    val snippetInterceptXhr: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截 XHR 请求"
        AppLanguage.ENGLISH -> "Intercept XHR Request"
        AppLanguage.ARABIC -> "اعتراض طلب XHR"
    }

    val snippetInterceptXhrDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截 XMLHttpRequest 请求"
        AppLanguage.ENGLISH -> "Intercept XMLHttpRequest requests"
        AppLanguage.ARABIC -> "اعتراض طلبات XMLHttpRequest"
    }

    val snippetInterceptWebSocket: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截 WebSocket"
        AppLanguage.ENGLISH -> "Intercept WebSocket"
        AppLanguage.ARABIC -> "اعتراض WebSocket"
    }

    val snippetInterceptWebSocketDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听 WebSocket 消息"
        AppLanguage.ENGLISH -> "Monitor WebSocket messages"
        AppLanguage.ARABIC -> "مراقبة رسائل WebSocket"
    }

    val snippetBlockRequests: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止特定请求"
        AppLanguage.ENGLISH -> "Block Specific Requests"
        AppLanguage.ARABIC -> "حظر طلبات محددة"
    }

    val snippetBlockRequestsDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止包含特定关键词的请求"
        AppLanguage.ENGLISH -> "Block requests containing specific keywords"
        AppLanguage.ARABIC -> "حظر الطلبات التي تحتوي على كلمات مفتاحية معينة"
    }

    val snippetAutomation: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动化"
        AppLanguage.ENGLISH -> "Automation"
        AppLanguage.ARABIC -> "الأتمتة"
    }

    val snippetAutomationDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动化操作和任务"
        AppLanguage.ENGLISH -> "Automated operations and tasks"
        AppLanguage.ARABIC -> "العمليات والمهام الآلية"
    }

    val snippetAutoClick: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动点击"
        AppLanguage.ENGLISH -> "Auto Click"
        AppLanguage.ARABIC -> "النقر التلقائي"
    }

    val snippetAutoClickDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动点击指定元素"
        AppLanguage.ENGLISH -> "Auto click specified element"
        AppLanguage.ARABIC -> "النقر تلقائياً على العنصر المحدد"
    }

    val snippetAutoClickInterval: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时自动点击"
        AppLanguage.ENGLISH -> "Timed Auto Click"
        AppLanguage.ARABIC -> "النقر التلقائي المؤقت"
    }

    val snippetAutoClickIntervalDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时重复点击元素"
        AppLanguage.ENGLISH -> "Repeatedly click element at intervals"
        AppLanguage.ARABIC -> "النقر المتكرر على العنصر على فترات"
    }

    val snippetAutoFillSubmit: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动填写表单"
        AppLanguage.ENGLISH -> "Auto Fill Form"
        AppLanguage.ARABIC -> "ملء النموذج تلقائياً"
    }

    val snippetAutoFillSubmitDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动填写并提交表单"
        AppLanguage.ENGLISH -> "Auto fill and submit form"
        AppLanguage.ARABIC -> "ملء النموذج وإرساله تلقائياً"
    }

    val snippetAutoRefresh: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动刷新页面"
        AppLanguage.ENGLISH -> "Auto Refresh Page"
        AppLanguage.ARABIC -> "تحديث الصفحة تلقائياً"
    }

    val snippetAutoRefreshDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时刷新页面"
        AppLanguage.ENGLISH -> "Refresh page at intervals"
        AppLanguage.ARABIC -> "تحديث الصفحة على فترات"
    }

    val snippetAutoScrollLoad: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动滚动加载"
        AppLanguage.ENGLISH -> "Auto Scroll Load"
        AppLanguage.ARABIC -> "التحميل بالتمرير التلقائي"
    }

    val snippetAutoScrollLoadDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动滚动到底部加载更多"
        AppLanguage.ENGLISH -> "Auto scroll to bottom to load more"
        AppLanguage.ARABIC -> "التمرير تلقائياً للأسفل لتحميل المزيد"
    }

    val snippetAutoLoginCheck: String get() = when (lang) {
        AppLanguage.CHINESE -> "自动登录检测"
        AppLanguage.ENGLISH -> "Auto Login Check"
        AppLanguage.ARABIC -> "فحص تسجيل الدخول التلقائي"
    }

    val snippetAutoLoginCheckDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测登录状态并提醒"
        AppLanguage.ENGLISH -> "Check login status and alert"
        AppLanguage.ARABIC -> "فحص حالة تسجيل الدخول والتنبيه"
    }

    val snippetDebug: String get() = when (lang) {
        AppLanguage.CHINESE -> "调试工具"
        AppLanguage.ENGLISH -> "Debug Tools"
        AppLanguage.ARABIC -> "أدوات التصحيح"
    }

    val snippetDebugDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "开发调试辅助工具"
        AppLanguage.ENGLISH -> "Development debugging tools"
        AppLanguage.ARABIC -> "أدوات تصحيح التطوير"
    }

    val snippetConsolePanel: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮控制台"
        AppLanguage.ENGLISH -> "Floating Console"
        AppLanguage.ARABIC -> "وحدة تحكم عائمة"
    }

    val snippetConsolePanelDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建悬浮日志面板"
        AppLanguage.ENGLISH -> "Create floating log panel"
        AppLanguage.ARABIC -> "إنشاء لوحة سجل عائمة"
    }

    val snippetElementInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "元素信息查看"
        AppLanguage.ENGLISH -> "Element Info Viewer"
        AppLanguage.ARABIC -> "عارض معلومات العنصر"
    }

    val snippetElementInfoDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击查看元素信息"
        AppLanguage.ENGLISH -> "Click to view element info"
        AppLanguage.ARABIC -> "انقر لعرض معلومات العنصر"
    }

    val snippetPerformance: String get() = when (lang) {
        AppLanguage.CHINESE -> "性能监控"
        AppLanguage.ENGLISH -> "Performance Monitor"
        AppLanguage.ARABIC -> "مراقب الأداء"
    }

    val snippetPerformanceDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "显示页面性能信息"
        AppLanguage.ENGLISH -> "Show page performance info"
        AppLanguage.ARABIC -> "عرض معلومات أداء الصفحة"
    }

    val snippetNetworkLog: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络请求日志"
        AppLanguage.ENGLISH -> "Network Request Log"
        AppLanguage.ARABIC -> "سجل طلبات الشبكة"
    }

    val snippetNetworkLogDesc: String get() = when (lang) {
        AppLanguage.CHINESE -> "记录所有网络请求"
        AppLanguage.ENGLISH -> "Log all network requests"
        AppLanguage.ARABIC -> "تسجيل جميع طلبات الشبكة"
    }
    // ==================== CodeSnippets Tags ====================
    val tagToast: String get() = when (lang) {
        AppLanguage.CHINESE -> "Notice"
        AppLanguage.ENGLISH -> "Toast"
        AppLanguage.ARABIC -> "تنبيه"
    }

    val tagMessage: String get() = when (lang) {
        AppLanguage.CHINESE -> "消息"
        AppLanguage.ENGLISH -> "Message"
        AppLanguage.ARABIC -> "رسالة"
    }

    val tagVibrate: String get() = when (lang) {
        AppLanguage.CHINESE -> "震动"
        AppLanguage.ENGLISH -> "Vibrate"
        AppLanguage.ARABIC -> "اهتزاز"
    }

    val tagFeedback: String get() = when (lang) {
        AppLanguage.CHINESE -> "反馈"
        AppLanguage.ENGLISH -> "Feedback"
        AppLanguage.ARABIC -> "ردود الفعل"
    }

    val tagHaptic: String get() = when (lang) {
        AppLanguage.CHINESE -> "触感"
        AppLanguage.ENGLISH -> "Haptic"
        AppLanguage.ARABIC -> "لمسي"
    }

    val tagClipboard: String get() = when (lang) {
        AppLanguage.CHINESE -> "剪贴板"
        AppLanguage.ENGLISH -> "Clipboard"
        AppLanguage.ARABIC -> "الحافظة"
    }

    val tagShare: String get() = when (lang) {
        AppLanguage.CHINESE -> "Share"
        AppLanguage.ENGLISH -> "Share"
        AppLanguage.ARABIC -> "مشاركة"
    }

    val tagSocial: String get() = when (lang) {
        AppLanguage.CHINESE -> "社交"
        AppLanguage.ENGLISH -> "Social"
        AppLanguage.ARABIC -> "اجتماعي"
    }

    val tagSave: String get() = when (lang) {
        AppLanguage.CHINESE -> "保存"
        AppLanguage.ENGLISH -> "Save"
        AppLanguage.ARABIC -> "حفظ"
    }

    val tagGallery: String get() = when (lang) {
        AppLanguage.CHINESE -> "相册"
        AppLanguage.ENGLISH -> "Gallery"
        AppLanguage.ARABIC -> "معرض"
    }

    val tagBrowser: String get() = when (lang) {
        AppLanguage.CHINESE -> "浏览器"
        AppLanguage.ENGLISH -> "Browser"
        AppLanguage.ARABIC -> "متصفح"
    }

    val tagLink: String get() = when (lang) {
        AppLanguage.CHINESE -> "链接"
        AppLanguage.ENGLISH -> "Link"
        AppLanguage.ARABIC -> "رابط"
    }

    val tagExternal: String get() = when (lang) {
        AppLanguage.CHINESE -> "外部"
        AppLanguage.ENGLISH -> "External"
        AppLanguage.ARABIC -> "خارجي"
    }

    val tagDevice: String get() = when (lang) {
        AppLanguage.CHINESE -> "设备"
        AppLanguage.ENGLISH -> "Device"
        AppLanguage.ARABIC -> "جهاز"
    }

    val tagInfo: String get() = when (lang) {
        AppLanguage.CHINESE -> "信息"
        AppLanguage.ENGLISH -> "Info"
        AppLanguage.ARABIC -> "معلومات"
    }

    val tagScreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "屏幕"
        AppLanguage.ENGLISH -> "Screen"
        AppLanguage.ARABIC -> "شاشة"
    }

    val tagNetwork: String get() = when (lang) {
        AppLanguage.CHINESE -> "网络"
        AppLanguage.ENGLISH -> "Network"
        AppLanguage.ARABIC -> "شبكة"
    }

    val tagWiFi: String get() = when (lang) {
        AppLanguage.CHINESE -> "WiFi"
        AppLanguage.ENGLISH -> "WiFi"
        AppLanguage.ARABIC -> "واي فاي"
    }

    val tagData: String get() = when (lang) {
        AppLanguage.CHINESE -> "流量"
        AppLanguage.ENGLISH -> "Data"
        AppLanguage.ARABIC -> "بيانات"
    }

    val tagFile: String get() = when (lang) {
        AppLanguage.CHINESE -> "文件"
        AppLanguage.ENGLISH -> "File"
        AppLanguage.ARABIC -> "ملف"
    }

    val tagExport: String get() = when (lang) {
        AppLanguage.CHINESE -> "导出"
        AppLanguage.ENGLISH -> "Export"
        AppLanguage.ARABIC -> "تصدير"
    }

    val tagFloating: String get() = when (lang) {
        AppLanguage.CHINESE -> "悬浮"
        AppLanguage.ENGLISH -> "Floating"
        AppLanguage.ARABIC -> "عائم"
    }

    val tagQuery: String get() = when (lang) {
        AppLanguage.CHINESE -> "查询"
        AppLanguage.ENGLISH -> "Query"
        AppLanguage.ARABIC -> "استعلام"
    }

    val tagSelector: String get() = when (lang) {
        AppLanguage.CHINESE -> "选择器"
        AppLanguage.ENGLISH -> "Selector"
        AppLanguage.ARABIC -> "محدد"
    }

    val tagIterate: String get() = when (lang) {
        AppLanguage.CHINESE -> "遍历"
        AppLanguage.ENGLISH -> "Iterate"
        AppLanguage.ARABIC -> "تكرار"
    }

    val tagHide: String get() = when (lang) {
        AppLanguage.CHINESE -> "隐藏"
        AppLanguage.ENGLISH -> "Hide"
        AppLanguage.ARABIC -> "إخفاء"
    }

    val tagStyle: String get() = when (lang) {
        AppLanguage.CHINESE -> "样式"
        AppLanguage.ENGLISH -> "Style"
        AppLanguage.ARABIC -> "نمط"
    }

    val tagDelete: String get() = when (lang) {
        AppLanguage.CHINESE -> "Delete"
        AppLanguage.ENGLISH -> "Delete"
        AppLanguage.ARABIC -> "حذف"
    }

    val tagRemove: String get() = when (lang) {
        AppLanguage.CHINESE -> "Remove"
        AppLanguage.ENGLISH -> "Remove"
        AppLanguage.ARABIC -> "إزالة"
    }

    val tagCreate: String get() = when (lang) {
        AppLanguage.CHINESE -> "创建"
        AppLanguage.ENGLISH -> "Create"
        AppLanguage.ARABIC -> "إنشاء"
    }

    val tagAdd: String get() = when (lang) {
        AppLanguage.CHINESE -> "Add"
        AppLanguage.ENGLISH -> "Add"
        AppLanguage.ARABIC -> "إضافة"
    }

    val tagText: String get() = when (lang) {
        AppLanguage.CHINESE -> "文本"
        AppLanguage.ENGLISH -> "Text"
        AppLanguage.ARABIC -> "نص"
    }

    val tagModify: String get() = when (lang) {
        AppLanguage.CHINESE -> "修改"
        AppLanguage.ENGLISH -> "Modify"
        AppLanguage.ARABIC -> "تعديل"
    }

    val tagAttribute: String get() = when (lang) {
        AppLanguage.CHINESE -> "属性"
        AppLanguage.ENGLISH -> "Attribute"
        AppLanguage.ARABIC -> "سمة"
    }

    val tagInsert: String get() = when (lang) {
        AppLanguage.CHINESE -> "插入"
        AppLanguage.ENGLISH -> "Insert"
        AppLanguage.ARABIC -> "إدراج"
    }

    val tagPosition: String get() = when (lang) {
        AppLanguage.CHINESE -> "位置"
        AppLanguage.ENGLISH -> "Position"
        AppLanguage.ARABIC -> "موقع"
    }

    val tagClone: String get() = when (lang) {
        AppLanguage.CHINESE -> "克隆"
        AppLanguage.ENGLISH -> "Clone"
        AppLanguage.ARABIC -> "استنساخ"
    }

    val tagWrap: String get() = when (lang) {
        AppLanguage.CHINESE -> "包裹"
        AppLanguage.ENGLISH -> "Wrap"
        AppLanguage.ARABIC -> "تغليف"
    }

    val tagStructure: String get() = when (lang) {
        AppLanguage.CHINESE -> "结构"
        AppLanguage.ENGLISH -> "Structure"
        AppLanguage.ARABIC -> "هيكل"
    }

    val tagReplace: String get() = when (lang) {
        AppLanguage.CHINESE -> "替换"
        AppLanguage.ENGLISH -> "Replace"
        AppLanguage.ARABIC -> "استبدال"
    }

    val tagCSS: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSS"
        AppLanguage.ENGLISH -> "CSS"
        AppLanguage.ARABIC -> "CSS"
    }

    val tagInject: String get() = when (lang) {
        AppLanguage.CHINESE -> "注入"
        AppLanguage.ENGLISH -> "Inject"
        AppLanguage.ARABIC -> "حقن"
    }

    val tagInline: String get() = when (lang) {
        AppLanguage.CHINESE -> "内联"
        AppLanguage.ENGLISH -> "Inline"
        AppLanguage.ARABIC -> "مضمن"
    }

    val tagClassName: String get() = when (lang) {
        AppLanguage.CHINESE -> "类名"
        AppLanguage.ENGLISH -> "Class"
        AppLanguage.ARABIC -> "فئة"
    }

    val tagWarm: String get() = when (lang) {
        AppLanguage.CHINESE -> "暖色"
        AppLanguage.ENGLISH -> "Warm"
        AppLanguage.ARABIC -> "دافئ"
    }

    val tagGrayscale: String get() = when (lang) {
        AppLanguage.CHINESE -> "灰度"
        AppLanguage.ENGLISH -> "Grayscale"
        AppLanguage.ARABIC -> "تدرج رمادي"
    }

    val tagFilter: String get() = when (lang) {
        AppLanguage.CHINESE -> "滤镜"
        AppLanguage.ENGLISH -> "Filter"
        AppLanguage.ARABIC -> "فلتر"
    }

    val tagFont: String get() = when (lang) {
        AppLanguage.CHINESE -> "字体"
        AppLanguage.ENGLISH -> "Font"
        AppLanguage.ARABIC -> "خط"
    }

    val tagSize: String get() = when (lang) {
        AppLanguage.CHINESE -> "大小"
        AppLanguage.ENGLISH -> "Size"
        AppLanguage.ARABIC -> "حجم"
    }

    val tagScrollbar: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动条"
        AppLanguage.ENGLISH -> "Scrollbar"
        AppLanguage.ARABIC -> "شريط التمرير"
    }

    val tagHighlight: String get() = when (lang) {
        AppLanguage.CHINESE -> "高亮"
        AppLanguage.ENGLISH -> "Highlight"
        AppLanguage.ARABIC -> "تمييز"
    }

    val tagWidth: String get() = when (lang) {
        AppLanguage.CHINESE -> "宽度"
        AppLanguage.ENGLISH -> "Width"
        AppLanguage.ARABIC -> "عرض"
    }

    val tagReading: String get() = when (lang) {
        AppLanguage.CHINESE -> "阅读"
        AppLanguage.ENGLISH -> "Reading"
        AppLanguage.ARABIC -> "قراءة"
    }

    val tagLineHeight: String get() = when (lang) {
        AppLanguage.CHINESE -> "行高"
        AppLanguage.ENGLISH -> "Line Height"
        AppLanguage.ARABIC -> "ارتفاع السطر"
    }

    val tagClick: String get() = when (lang) {
        AppLanguage.CHINESE -> "点击"
        AppLanguage.ENGLISH -> "Click"
        AppLanguage.ARABIC -> "نقر"
    }

    val tagEvent: String get() = when (lang) {
        AppLanguage.CHINESE -> "事件"
        AppLanguage.ENGLISH -> "Event"
        AppLanguage.ARABIC -> "حدث"
    }

    val tagKeyboard: String get() = when (lang) {
        AppLanguage.CHINESE -> "键盘"
        AppLanguage.ENGLISH -> "Keyboard"
        AppLanguage.ARABIC -> "لوحة المفاتيح"
    }

    val tagShortcut: String get() = when (lang) {
        AppLanguage.CHINESE -> "快捷键"
        AppLanguage.ENGLISH -> "Shortcut"
        AppLanguage.ARABIC -> "اختصار"
    }

    val tagScroll: String get() = when (lang) {
        AppLanguage.CHINESE -> "滚动"
        AppLanguage.ENGLISH -> "Scroll"
        AppLanguage.ARABIC -> "تمرير"
    }

    val tagListen: String get() = when (lang) {
        AppLanguage.CHINESE -> "监听"
        AppLanguage.ENGLISH -> "Listen"
        AppLanguage.ARABIC -> "استماع"
    }

    val tagDomChange: String get() = when (lang) {
        AppLanguage.CHINESE -> "DOM变化"
        AppLanguage.ENGLISH -> "DOM Change"
        AppLanguage.ARABIC -> "تغيير DOM"
    }

    val tagDynamic: String get() = when (lang) {
        AppLanguage.CHINESE -> "动态"
        AppLanguage.ENGLISH -> "Dynamic"
        AppLanguage.ARABIC -> "ديناميكي"
    }

    val tagWindow: String get() = when (lang) {
        AppLanguage.CHINESE -> "窗口"
        AppLanguage.ENGLISH -> "Window"
        AppLanguage.ARABIC -> "نافذة"
    }

    val tagRightClick: String get() = when (lang) {
        AppLanguage.CHINESE -> "右键"
        AppLanguage.ENGLISH -> "Right Click"
        AppLanguage.ARABIC -> "نقر يمين"
    }

    val tagMenu: String get() = when (lang) {
        AppLanguage.CHINESE -> "菜单"
        AppLanguage.ENGLISH -> "Menu"
        AppLanguage.ARABIC -> "قائمة"
    }

    val tagVisibility: String get() = when (lang) {
        AppLanguage.CHINESE -> "可见性"
        AppLanguage.ENGLISH -> "Visibility"
        AppLanguage.ARABIC -> "الرؤية"
    }

    val tagBackground: String get() = when (lang) {
        AppLanguage.CHINESE -> "后台"
        AppLanguage.ENGLISH -> "Background"
        AppLanguage.ARABIC -> "خلفية"
    }

    val tagClose: String get() = when (lang) {
        AppLanguage.CHINESE -> "Close"
        AppLanguage.ENGLISH -> "Close"
        AppLanguage.ARABIC -> "إغلاق"
    }

    val tagTouch: String get() = when (lang) {
        AppLanguage.CHINESE -> "触摸"
        AppLanguage.ENGLISH -> "Touch"
        AppLanguage.ARABIC -> "لمس"
    }

    val tagGesture: String get() = when (lang) {
        AppLanguage.CHINESE -> "手势"
        AppLanguage.ENGLISH -> "Gesture"
        AppLanguage.ARABIC -> "إيماءة"
    }

    val tagLongPress: String get() = when (lang) {
        AppLanguage.CHINESE -> "长按"
        AppLanguage.ENGLISH -> "Long Press"
        AppLanguage.ARABIC -> "ضغط مطول"
    }

    val tagStorage: String get() = when (lang) {
        AppLanguage.CHINESE -> "存储"
        AppLanguage.ENGLISH -> "Storage"
        AppLanguage.ARABIC -> "تخزين"
    }

    val tagRead: String get() = when (lang) {
        AppLanguage.CHINESE -> "读取"
        AppLanguage.ENGLISH -> "Read"
        AppLanguage.ARABIC -> "قراءة"
    }

    val tagSession: String get() = when (lang) {
        AppLanguage.CHINESE -> "会话"
        AppLanguage.ENGLISH -> "Session"
        AppLanguage.ARABIC -> "جلسة"
    }

    val tagTemporary: String get() = when (lang) {
        AppLanguage.CHINESE -> "临时"
        AppLanguage.ENGLISH -> "Temporary"
        AppLanguage.ARABIC -> "مؤقت"
    }

    val tagCookie: String get() = when (lang) {
        AppLanguage.CHINESE -> "Cookie"
        AppLanguage.ENGLISH -> "Cookie"
        AppLanguage.ARABIC -> "كوكي"
    }

    val tagSetting: String get() = when (lang) {
        AppLanguage.CHINESE -> "设置"
        AppLanguage.ENGLISH -> "Setting"
        AppLanguage.ARABIC -> "إعداد"
    }

    val tagIndexedDB: String get() = when (lang) {
        AppLanguage.CHINESE -> "IndexedDB"
        AppLanguage.ENGLISH -> "IndexedDB"
        AppLanguage.ARABIC -> "IndexedDB"
    }

    val tagBigData: String get() = when (lang) {
        AppLanguage.CHINESE -> "大数据"
        AppLanguage.ENGLISH -> "Big Data"
        AppLanguage.ARABIC -> "بيانات كبيرة"
    }

    val tagGET: String get() = when (lang) {
        AppLanguage.CHINESE -> "GET"
        AppLanguage.ENGLISH -> "GET"
        AppLanguage.ARABIC -> "GET"
    }

    val tagRequest: String get() = when (lang) {
        AppLanguage.CHINESE -> "请求"
        AppLanguage.ENGLISH -> "Request"
        AppLanguage.ARABIC -> "طلب"
    }

    val tagPOST: String get() = when (lang) {
        AppLanguage.CHINESE -> "POST"
        AppLanguage.ENGLISH -> "POST"
        AppLanguage.ARABIC -> "POST"
    }

    val tagSubmit: String get() = when (lang) {
        AppLanguage.CHINESE -> "提交"
        AppLanguage.ENGLISH -> "Submit"
        AppLanguage.ARABIC -> "إرسال"
    }

    val tagTimeout: String get() = when (lang) {
        AppLanguage.CHINESE -> "超时"
        AppLanguage.ENGLISH -> "Timeout"
        AppLanguage.ARABIC -> "مهلة"
    }

    val tagRetry: String get() = when (lang) {
        AppLanguage.CHINESE -> "Retry"
        AppLanguage.ENGLISH -> "Retry"
        AppLanguage.ARABIC -> "إعادة المحاولة"
    }

    val tagJSONP: String get() = when (lang) {
        AppLanguage.CHINESE -> "JSONP"
        AppLanguage.ENGLISH -> "JSONP"
        AppLanguage.ARABIC -> "JSONP"
    }

    val tagCrossDomain: String get() = when (lang) {
        AppLanguage.CHINESE -> "跨域"
        AppLanguage.ENGLISH -> "Cross Domain"
        AppLanguage.ARABIC -> "عبر النطاق"
    }

    val tagTable: String get() = when (lang) {
        AppLanguage.CHINESE -> "表格"
        AppLanguage.ENGLISH -> "Table"
        AppLanguage.ARABIC -> "جدول"
    }

    val tagExtract: String get() = when (lang) {
        AppLanguage.CHINESE -> "提取"
        AppLanguage.ENGLISH -> "Extract"
        AppLanguage.ARABIC -> "استخراج"
    }

    val tagJSON: String get() = when (lang) {
        AppLanguage.CHINESE -> "JSON"
        AppLanguage.ENGLISH -> "JSON"
        AppLanguage.ARABIC -> "JSON"
    }

    val tagCSV: String get() = when (lang) {
        AppLanguage.CHINESE -> "CSV"
        AppLanguage.ENGLISH -> "CSV"
        AppLanguage.ARABIC -> "CSV"
    }

    val tagURL: String get() = when (lang) {
        AppLanguage.CHINESE -> "URL"
        AppLanguage.ENGLISH -> "URL"
        AppLanguage.ARABIC -> "URL"
    }

    val tagParse: String get() = when (lang) {
        AppLanguage.CHINESE -> "解析"
        AppLanguage.ENGLISH -> "Parse"
        AppLanguage.ARABIC -> "تحليل"
    }

    val tagBuild: String get() = when (lang) {
        AppLanguage.CHINESE -> "构建"
        AppLanguage.ENGLISH -> "Build"
        AppLanguage.ARABIC -> "بناء"
    }

    val tagPopup: String get() = when (lang) {
        AppLanguage.CHINESE -> "弹窗"
        AppLanguage.ENGLISH -> "Popup"
        AppLanguage.ARABIC -> "نافذة منبثقة"
    }

    val tagDialog: String get() = when (lang) {
        AppLanguage.CHINESE -> "对话框"
        AppLanguage.ENGLISH -> "Dialog"
        AppLanguage.ARABIC -> "حوار"
    }

    val tagProgress: String get() = when (lang) {
        AppLanguage.CHINESE -> "进度"
        AppLanguage.ENGLISH -> "Progress"
        AppLanguage.ARABIC -> "تقدم"
    }

    val tagLoading: String get() = when (lang) {
        AppLanguage.CHINESE -> "加载"
        AppLanguage.ENGLISH -> "Loading"
        AppLanguage.ARABIC -> "تحميل"
    }

    val tagAnimation: String get() = when (lang) {
        AppLanguage.CHINESE -> "动画"
        AppLanguage.ENGLISH -> "Animation"
        AppLanguage.ARABIC -> "رسوم متحركة"
    }

    val tagNotification: String get() = when (lang) {
        AppLanguage.CHINESE -> "通知"
        AppLanguage.ENGLISH -> "Notification"
        AppLanguage.ARABIC -> "إشعار"
    }

    val tagSnackbar: String get() = when (lang) {
        AppLanguage.CHINESE -> "Snackbar"
        AppLanguage.ENGLISH -> "Snackbar"
        AppLanguage.ARABIC -> "Snackbar"
    }

    val tagToolbar: String get() = when (lang) {
        AppLanguage.CHINESE -> "工具栏"
        AppLanguage.ENGLISH -> "Toolbar"
        AppLanguage.ARABIC -> "شريط الأدوات"
    }

    val tagSidebar: String get() = when (lang) {
        AppLanguage.CHINESE -> "侧边栏"
        AppLanguage.ENGLISH -> "Sidebar"
        AppLanguage.ARABIC -> "الشريط الجانبي"
    }

    val tagPanel: String get() = when (lang) {
        AppLanguage.CHINESE -> "面板"
        AppLanguage.ENGLISH -> "Panel"
        AppLanguage.ARABIC -> "لوحة"
    }

    val tagDrag: String get() = when (lang) {
        AppLanguage.CHINESE -> "拖动"
        AppLanguage.ENGLISH -> "Drag"
        AppLanguage.ARABIC -> "سحب"
    }

    val tagInteraction: String get() = when (lang) {
        AppLanguage.CHINESE -> "交互"
        AppLanguage.ENGLISH -> "Interaction"
        AppLanguage.ARABIC -> "تفاعل"
    }

    val tagPlayer: String get() = when (lang) {
        AppLanguage.CHINESE -> "播放器"
        AppLanguage.ENGLISH -> "Player"
        AppLanguage.ARABIC -> "مشغل"
    }

    val tagMusic: String get() = when (lang) {
        AppLanguage.CHINESE -> "音乐"
        AppLanguage.ENGLISH -> "Music"
        AppLanguage.ARABIC -> "موسيقى"
    }

    val tagBadge: String get() = when (lang) {
        AppLanguage.CHINESE -> "角标"
        AppLanguage.ENGLISH -> "Badge"
        AppLanguage.ARABIC -> "شارة"
    }

    val tagNumber: String get() = when (lang) {
        AppLanguage.CHINESE -> "数字"
        AppLanguage.ENGLISH -> "Number"
        AppLanguage.ARABIC -> "رقم"
    }

    val tagBanner: String get() = when (lang) {
        AppLanguage.CHINESE -> "横幅"
        AppLanguage.ENGLISH -> "Banner"
        AppLanguage.ARABIC -> "لافتة"
    }

    val tagReminder: String get() = when (lang) {
        AppLanguage.CHINESE -> "提醒"
        AppLanguage.ENGLISH -> "Reminder"
        AppLanguage.ARABIC -> "تذكير"
    }

    val tagTop: String get() = when (lang) {
        AppLanguage.CHINESE -> "顶部"
        AppLanguage.ENGLISH -> "Top"
        AppLanguage.ARABIC -> "أعلى"
    }

    val tagBottom: String get() = when (lang) {
        AppLanguage.CHINESE -> "底部"
        AppLanguage.ENGLISH -> "Bottom"
        AppLanguage.ARABIC -> "أسفل"
    }

    val tagBackToTop: String get() = when (lang) {
        AppLanguage.CHINESE -> "返回顶部"
        AppLanguage.ENGLISH -> "Back to Top"
        AppLanguage.ARABIC -> "العودة للأعلى"
    }

    val tagNavigation: String get() = when (lang) {
        AppLanguage.CHINESE -> "导航"
        AppLanguage.ENGLISH -> "Navigation"
        AppLanguage.ARABIC -> "تنقل"
    }

    val tagForm: String get() = when (lang) {
        AppLanguage.CHINESE -> "表单"
        AppLanguage.ENGLISH -> "Form"
        AppLanguage.ARABIC -> "نموذج"
    }

    val tagFill: String get() = when (lang) {
        AppLanguage.CHINESE -> "填充"
        AppLanguage.ENGLISH -> "Fill"
        AppLanguage.ARABIC -> "تعبئة"
    }

    val tagGet: String get() = when (lang) {
        AppLanguage.CHINESE -> "获取"
        AppLanguage.ENGLISH -> "Get"
        AppLanguage.ARABIC -> "الحصول على"
    }

    val tagValidate: String get() = when (lang) {
        AppLanguage.CHINESE -> "验证"
        AppLanguage.ENGLISH -> "Validate"
        AppLanguage.ARABIC -> "تحقق"
    }

    val tagIntercept: String get() = when (lang) {
        AppLanguage.CHINESE -> "拦截"
        AppLanguage.ENGLISH -> "Intercept"
        AppLanguage.ARABIC -> "اعتراض"
    }

    val tagClear: String get() = when (lang) {
        AppLanguage.CHINESE -> "清空"
        AppLanguage.ENGLISH -> "Clear"
        AppLanguage.ARABIC -> "مسح"
    }

    val tagPassword: String get() = when (lang) {
        AppLanguage.CHINESE -> "密码"
        AppLanguage.ENGLISH -> "Password"
        AppLanguage.ARABIC -> "كلمة مرور"
    }

    val tagToggle: String get() = when (lang) {
        AppLanguage.CHINESE -> "切换"
        AppLanguage.ENGLISH -> "Toggle"
        AppLanguage.ARABIC -> "تبديل"
    }

    val tagZoom: String get() = when (lang) {
        AppLanguage.CHINESE -> "放大"
        AppLanguage.ENGLISH -> "Zoom"
        AppLanguage.ARABIC -> "تكبير"
    }

    val tagAudio: String get() = when (lang) {
        AppLanguage.CHINESE -> "音频"
        AppLanguage.ENGLISH -> "Audio"
        AppLanguage.ARABIC -> "صوت"
    }

    val tagControl: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制"
        AppLanguage.ENGLISH -> "Control"
        AppLanguage.ARABIC -> "تحكم"
    }

    val tagLazyLoad: String get() = when (lang) {
        AppLanguage.CHINESE -> "懒加载"
        AppLanguage.ENGLISH -> "Lazy Load"
        AppLanguage.ARABIC -> "تحميل كسول"
    }

    val tagFullscreen: String get() = when (lang) {
        AppLanguage.CHINESE -> "全屏"
        AppLanguage.ENGLISH -> "Fullscreen"
        AppLanguage.ARABIC -> "ملء الشاشة"
    }

    val tagSimplify: String get() = when (lang) {
        AppLanguage.CHINESE -> "简化"
        AppLanguage.ENGLISH -> "Simplify"
        AppLanguage.ARABIC -> "تبسيط"
    }

    val tagUnlock: String get() = when (lang) {
        AppLanguage.CHINESE -> "解锁"
        AppLanguage.ENGLISH -> "Unlock"
        AppLanguage.ARABIC -> "فتح"
    }

    val tagPrint: String get() = when (lang) {
        AppLanguage.CHINESE -> "打印"
        AppLanguage.ENGLISH -> "Print"
        AppLanguage.ARABIC -> "طباعة"
    }

    val tagOptimize: String get() = when (lang) {
        AppLanguage.CHINESE -> "优化"
        AppLanguage.ENGLISH -> "Optimize"
        AppLanguage.ARABIC -> "تحسين"
    }

    val tagVoice: String get() = when (lang) {
        AppLanguage.CHINESE -> "语音"
        AppLanguage.ENGLISH -> "Voice"
        AppLanguage.ARABIC -> "صوت"
    }

    val tagReadAloud: String get() = when (lang) {
        AppLanguage.CHINESE -> "朗读"
        AppLanguage.ENGLISH -> "Read Aloud"
        AppLanguage.ARABIC -> "قراءة بصوت عالٍ"
    }

    val tagStats: String get() = when (lang) {
        AppLanguage.CHINESE -> "统计"
        AppLanguage.ENGLISH -> "Stats"
        AppLanguage.ARABIC -> "إحصائيات"
    }

    val tagWordCount: String get() = when (lang) {
        AppLanguage.CHINESE -> "字数"
        AppLanguage.ENGLISH -> "Word Count"
        AppLanguage.ARABIC -> "عدد الكلمات"
    }

    val tagSearch: String get() = when (lang) {
        AppLanguage.CHINESE -> "搜索"
        AppLanguage.ENGLISH -> "Search"
        AppLanguage.ARABIC -> "بحث"
    }

    val tagKeyword: String get() = when (lang) {
        AppLanguage.CHINESE -> "关键词"
        AppLanguage.ENGLISH -> "Keyword"
        AppLanguage.ARABIC -> "كلمة مفتاحية"
    }

    val tagEmptyElement: String get() = when (lang) {
        AppLanguage.CHINESE -> "空元素"
        AppLanguage.ENGLISH -> "Empty Element"
        AppLanguage.ARABIC -> "عنصر فارغ"
    }

    val tagClean: String get() = when (lang) {
        AppLanguage.CHINESE -> "清理"
        AppLanguage.ENGLISH -> "Clean"
        AppLanguage.ARABIC -> "تنظيف"
    }

    val tagComment: String get() = when (lang) {
        AppLanguage.CHINESE -> "评论"
        AppLanguage.ENGLISH -> "Comment"
        AppLanguage.ARABIC -> "تعليق"
    }

    val tagPrevent: String get() = when (lang) {
        AppLanguage.CHINESE -> "阻止"
        AppLanguage.ENGLISH -> "Prevent"
        AppLanguage.ARABIC -> "منع"
    }

    val tagMask: String get() = when (lang) {
        AppLanguage.CHINESE -> "遮罩"
        AppLanguage.ENGLISH -> "Mask"
        AppLanguage.ARABIC -> "قناع"
    }

    val tagAntiDetect: String get() = when (lang) {
        AppLanguage.CHINESE -> "反检测"
        AppLanguage.ENGLISH -> "Anti-Detect"
        AppLanguage.ARABIC -> "مكافحة الكشف"
    }

    val tagDebounce: String get() = when (lang) {
        AppLanguage.CHINESE -> "防抖"
        AppLanguage.ENGLISH -> "Debounce"
        AppLanguage.ARABIC -> "منع الارتداد"
    }

    val tagPerformance: String get() = when (lang) {
        AppLanguage.CHINESE -> "性能"
        AppLanguage.ENGLISH -> "Performance"
        AppLanguage.ARABIC -> "أداء"
    }

    val tagThrottle: String get() = when (lang) {
        AppLanguage.CHINESE -> "节流"
        AppLanguage.ENGLISH -> "Throttle"
        AppLanguage.ARABIC -> "تقييد"
    }

    val tagWait: String get() = when (lang) {
        AppLanguage.CHINESE -> "等待"
        AppLanguage.ENGLISH -> "Wait"
        AppLanguage.ARABIC -> "انتظار"
    }

    val tagAsync: String get() = when (lang) {
        AppLanguage.CHINESE -> "异步"
        AppLanguage.ENGLISH -> "Async"
        AppLanguage.ARABIC -> "غير متزامن"
    }

    val tagDate: String get() = when (lang) {
        AppLanguage.CHINESE -> "日期"
        AppLanguage.ENGLISH -> "Date"
        AppLanguage.ARABIC -> "تاريخ"
    }

    val tagFormat: String get() = when (lang) {
        AppLanguage.CHINESE -> "格式化"
        AppLanguage.ENGLISH -> "Format"
        AppLanguage.ARABIC -> "تنسيق"
    }

    val tagRandom: String get() = when (lang) {
        AppLanguage.CHINESE -> "随机"
        AppLanguage.ENGLISH -> "Random"
        AppLanguage.ARABIC -> "عشوائي"
    }

    val tagString: String get() = when (lang) {
        AppLanguage.CHINESE -> "字符串"
        AppLanguage.ENGLISH -> "String"
        AppLanguage.ARABIC -> "سلسلة"
    }

    val tagDelay: String get() = when (lang) {
        AppLanguage.CHINESE -> "延迟"
        AppLanguage.ENGLISH -> "Delay"
        AppLanguage.ARABIC -> "تأخير"
    }

    val tagErrorHandle: String get() = when (lang) {
        AppLanguage.CHINESE -> "错误处理"
        AppLanguage.ENGLISH -> "Error Handling"
        AppLanguage.ARABIC -> "معالجة الأخطاء"
    }

    val tagArticle: String get() = when (lang) {
        AppLanguage.CHINESE -> "文章"
        AppLanguage.ENGLISH -> "Article"
        AppLanguage.ARABIC -> "مقال"
    }

    val tagMarkdown: String get() = when (lang) {
        AppLanguage.CHINESE -> "Markdown"
        AppLanguage.ENGLISH -> "Markdown"
        AppLanguage.ARABIC -> "Markdown"
    }

    val tagConvert: String get() = when (lang) {
        AppLanguage.CHINESE -> "转换"
        AppLanguage.ENGLISH -> "Convert"
        AppLanguage.ARABIC -> "تحويل"
    }

    val tagFetch: String get() = when (lang) {
        AppLanguage.CHINESE -> "fetch"
        AppLanguage.ENGLISH -> "Fetch"
        AppLanguage.ARABIC -> "Fetch"
    }

    val tagXHR: String get() = when (lang) {
        AppLanguage.CHINESE -> "XHR"
        AppLanguage.ENGLISH -> "XHR"
        AppLanguage.ARABIC -> "XHR"
    }

    val tagWebSocket: String get() = when (lang) {
        AppLanguage.CHINESE -> "WebSocket"
        AppLanguage.ENGLISH -> "WebSocket"
        AppLanguage.ARABIC -> "WebSocket"
    }

    val tagTimer: String get() = when (lang) {
        AppLanguage.CHINESE -> "定时"
        AppLanguage.ENGLISH -> "Timer"
        AppLanguage.ARABIC -> "مؤقت"
    }

    val tagRefresh: String get() = when (lang) {
        AppLanguage.CHINESE -> "Refresh"
        AppLanguage.ENGLISH -> "Refresh"
        AppLanguage.ARABIC -> "تحديث"
    }

    val tagLogin: String get() = when (lang) {
        AppLanguage.CHINESE -> "登录"
        AppLanguage.ENGLISH -> "Login"
        AppLanguage.ARABIC -> "تسجيل الدخول"
    }

    val tagDetect: String get() = when (lang) {
        AppLanguage.CHINESE -> "检测"
        AppLanguage.ENGLISH -> "Detect"
        AppLanguage.ARABIC -> "كشف"
    }

    val tagConsole: String get() = when (lang) {
        AppLanguage.CHINESE -> "控制台"
        AppLanguage.ENGLISH -> "Console"
        AppLanguage.ARABIC -> "وحدة التحكم"
    }

    val tagLog: String get() = when (lang) {
        AppLanguage.CHINESE -> "日志"
        AppLanguage.ENGLISH -> "Log"
        AppLanguage.ARABIC -> "سجل"
    }

    val tagInspect: String get() = when (lang) {
        AppLanguage.CHINESE -> "检查"
        AppLanguage.ENGLISH -> "Inspect"
        AppLanguage.ARABIC -> "فحص"
    }

    val tagMonitor: String get() = when (lang) {
        AppLanguage.CHINESE -> "监控"
        AppLanguage.ENGLISH -> "Monitor"
        AppLanguage.ARABIC -> "مراقبة"
    }
}
