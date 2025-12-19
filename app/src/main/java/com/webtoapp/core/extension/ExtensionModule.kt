package com.webtoapp.core.extension

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * 扩展模块分类
 */
enum class ModuleCategory(val displayName: String, val icon: String, val description: String) {
    // 内容处理类
    CONTENT_FILTER("内容过滤", "🚫", "屏蔽元素、广告过滤、内容隐藏"),
    CONTENT_ENHANCE("内容增强", "✨", "内容优化、排版美化、阅读增强"),
    
    // 样式类
    STYLE_MODIFIER("样式修改", "🎨", "自定义CSS、主题美化、界面调整"),
    THEME("主题美化", "🌈", "深色模式、配色方案、字体替换"),
    
    // 功能类
    FUNCTION_ENHANCE("功能增强", "⚡", "自动化操作、快捷功能、效率工具"),
    AUTOMATION("自动化", "🤖", "自动点击、自动填表、定时任务"),
    NAVIGATION("导航辅助", "🧭", "返回顶部、快速跳转、页面导航"),
    
    // 数据类
    DATA_EXTRACT("数据提取", "📊", "内容抓取、数据导出、信息收集"),
    DATA_SAVE("数据保存", "💾", "页面保存、截图、内容导出"),
    
    // 交互类
    INTERACTION("交互增强", "🖱️", "手势操作、快捷键、自动填表"),
    ACCESSIBILITY("无障碍", "♿", "辅助阅读、语音朗读、高对比度"),
    
    // 媒体类
    MEDIA("媒体处理", "🎬", "视频增强、图片处理、音频控制"),
    VIDEO("视频增强", "📹", "倍速播放、画中画、视频下载"),
    IMAGE("图片处理", "🖼️", "图片放大、批量下载、懒加载"),
    AUDIO("音频控制", "🎵", "音量控制、音频提取、播放增强"),
    
    // 安全类
    SECURITY("安全隐私", "🔒", "隐私保护、指纹防护、追踪拦截"),
    ANTI_TRACKING("反追踪", "🕵️", "阻止追踪、Cookie管理、隐私模式"),
    
    // 社交类
    SOCIAL("社交增强", "💬", "评论过滤、社交优化、消息增强"),
    
    // 购物类
    SHOPPING("购物助手", "🛒", "比价工具、优惠提醒、历史价格"),
    
    // 阅读类
    READING("阅读模式", "📖", "正文提取、排版优化、护眼模式"),
    
    // 翻译类
    TRANSLATE("翻译工具", "🌐", "划词翻译、全文翻译、多语言"),
    
    // 开发类
    DEVELOPER("开发调试", "🛠️", "调试工具、性能监控、日志输出"),
    
    // 其他
    OTHER("其他", "📦", "未分类的扩展模块")
}

/**
 * 模块执行时机
 */
enum class ModuleRunTime(val displayName: String, val description: String, val jsEvent: String) {
    DOCUMENT_START("页面开始", "DOM 未就绪时执行，适合拦截请求和早期修改", ""),
    DOCUMENT_END("DOM 就绪", "DOM 加载完成后执行（推荐），适合大多数场景", "DOMContentLoaded"),
    DOCUMENT_IDLE("页面空闲", "页面完全加载后执行，适合后处理和性能优化", "load"),
    CONTEXT_MENU("右键菜单", "右键菜单打开时执行", "contextmenu"),
    BEFORE_UNLOAD("页面关闭前", "页面即将关闭时执行，适合保存数据", "beforeunload")
}

/**
 * 模块触发条件
 */
enum class ModuleTrigger(val displayName: String, val description: String) {
    AUTO("自动执行", "页面加载时自动执行"),
    MANUAL("手动触发", "需要用户手动触发执行"),
    INTERVAL("定时执行", "按设定间隔定时执行"),
    MUTATION("DOM变化", "检测到DOM变化时执行"),
    SCROLL("滚动触发", "页面滚动时执行"),
    CLICK("点击触发", "点击指定元素时执行"),
    HOVER("悬停触发", "鼠标悬停时执行"),
    FOCUS("聚焦触发", "元素获得焦点时执行"),
    INPUT("输入触发", "用户输入时执行"),
    VISIBILITY("可见性变化", "元素可见性变化时执行")
}

/**
 * 模块权限
 */
enum class ModulePermission(val displayName: String, val description: String, val dangerous: Boolean = false) {
    // 基础权限
    DOM_ACCESS("DOM 访问", "读取和修改页面元素"),
    DOM_OBSERVE("DOM 监听", "监听页面元素变化"),
    CSS_INJECT("CSS 注入", "向页面注入样式"),
    
    // 存储权限
    STORAGE("本地存储", "读写 localStorage/sessionStorage"),
    COOKIE("Cookie", "读写 Cookie", true),
    INDEXED_DB("IndexedDB", "访问 IndexedDB 数据库", true),
    CACHE("缓存控制", "管理浏览器缓存"),
    
    // 网络权限
    NETWORK("网络请求", "发送 HTTP 请求", true),
    WEBSOCKET("WebSocket", "建立 WebSocket 连接", true),
    FETCH_INTERCEPT("请求拦截", "拦截和修改网络请求", true),
    
    // 用户交互权限
    CLIPBOARD("剪贴板", "读写剪贴板内容", true),
    NOTIFICATION("通知", "显示通知消息"),
    ALERT("弹窗", "显示 alert/confirm/prompt"),
    KEYBOARD("键盘监听", "监听键盘事件"),
    MOUSE("鼠标监听", "监听鼠标事件"),
    TOUCH("触摸监听", "监听触摸事件"),
    
    // 设备权限
    LOCATION("位置信息", "获取地理位置", true),
    CAMERA("摄像头", "访问摄像头", true),
    MICROPHONE("麦克风", "访问麦克风", true),
    DEVICE_INFO("设备信息", "获取设备信息"),
    
    // 媒体权限
    MEDIA("媒体控制", "控制音视频播放"),
    FULLSCREEN("全屏控制", "控制全屏模式"),
    PICTURE_IN_PICTURE("画中画", "启用画中画模式"),
    SCREEN_CAPTURE("屏幕截图", "截取页面内容", true),
    
    // 文件权限
    DOWNLOAD("下载", "触发文件下载"),
    FILE_ACCESS("文件访问", "访问本地文件", true),
    
    // 高级权限
    EVAL("动态执行", "执行动态代码", true),
    IFRAME("iframe 访问", "访问 iframe 内容", true),
    WINDOW_OPEN("新窗口", "打开新窗口/标签页"),
    HISTORY("历史记录", "访问浏览历史"),
    NAVIGATION("页面导航", "控制页面跳转")
}

/**
 * 模块配置项类型
 */
enum class ConfigItemType(val displayName: String, val description: String) {
    // 基础类型
    TEXT("文本", "单行文本输入"),
    TEXTAREA("多行文本", "多行文本输入，适合代码或长文本"),
    NUMBER("数字", "数字输入，支持整数和小数"),
    BOOLEAN("开关", "是/否 开关选择"),
    
    // 选择类型
    SELECT("单选", "下拉单选列表"),
    MULTI_SELECT("多选", "多选列表"),
    RADIO("单选按钮", "单选按钮组"),
    CHECKBOX("复选框", "复选框组"),
    
    // 特殊输入类型
    COLOR("颜色", "颜色选择器"),
    URL("网址", "URL 输入，带格式验证"),
    EMAIL("邮箱", "邮箱输入，带格式验证"),
    PASSWORD("密码", "密码输入，内容隐藏"),
    
    // 高级类型
    REGEX("正则表达式", "正则表达式输入"),
    CSS_SELECTOR("CSS选择器", "CSS 选择器输入"),
    JAVASCRIPT("JavaScript", "JavaScript 代码输入"),
    JSON("JSON", "JSON 格式数据输入"),
    
    // 范围类型
    RANGE("滑块", "数值范围滑块"),
    DATE("日期", "日期选择"),
    TIME("时间", "时间选择"),
    DATETIME("日期时间", "日期时间选择"),
    
    // 文件类型
    FILE("文件", "文件选择"),
    IMAGE("图片", "图片选择/上传")
}

/**
 * 模块配置项定义
 */
data class ModuleConfigItem(
    @SerializedName("key")
    val key: String,                              // 配置键名
    @SerializedName("name")
    val name: String,                             // 显示名称
    @SerializedName("description")
    val description: String = "",                 // 配置说明
    @SerializedName("type")
    val type: ConfigItemType = ConfigItemType.TEXT, // 配置类型
    @SerializedName("defaultValue")
    val defaultValue: String = "",                // 默认值
    @SerializedName("options")
    val options: List<String> = emptyList(),      // 选项列表（SELECT/MULTI_SELECT 类型）
    @SerializedName("required")
    val required: Boolean = false,                // 是否必填
    @SerializedName("placeholder")
    val placeholder: String = "",                 // 占位提示
    @SerializedName("validation")
    val validation: String? = null                // 验证正则（可选）
)

/**
 * 模块作者信息
 */
data class ModuleAuthor(
    @SerializedName("name")
    val name: String,                             // 作者名称
    @SerializedName("email")
    val email: String? = null,                    // 邮箱
    @SerializedName("url")
    val url: String? = null,                      // 主页
    @SerializedName("qq")
    val qq: String? = null                        // QQ
)

/**
 * 模块版本信息
 */
data class ModuleVersion(
    @SerializedName("code")
    val code: Int = 1,                            // 版本号
    @SerializedName("name")
    val name: String = "1.0.0",                   // 版本名
    @SerializedName("changelog")
    val changelog: String = ""                    // 更新日志
)

/**
 * URL 匹配规则
 */
data class UrlMatchRule(
    @SerializedName("pattern")
    val pattern: String,                          // 匹配模式（支持通配符和正则）
    @SerializedName("isRegex")
    val isRegex: Boolean = false,                 // 是否为正则表达式
    @SerializedName("exclude")
    val exclude: Boolean = false                  // 是否为排除规则
)


/**
 * 扩展模块 - 核心数据模型
 * 
 * 这是整个扩展系统的核心，定义了一个可复用、可分享的功能模块
 */
data class ExtensionModule(
    // 基本标识
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),  // 唯一ID
    @SerializedName("name")
    val name: String,                               // 模块名称
    @SerializedName("description")
    val description: String = "",                   // 模块描述
    @SerializedName("icon")
    val icon: String = "📦",                        // 模块图标（emoji）
    
    // 分类与标签
    @SerializedName("category")
    val category: ModuleCategory = ModuleCategory.OTHER, // 模块分类
    @SerializedName("tags")
    val tags: List<String> = emptyList(),           // 标签列表
    
    // 版本与作者
    @SerializedName("version")
    val version: ModuleVersion = ModuleVersion(),   // 版本信息
    @SerializedName("author")
    val author: ModuleAuthor? = null,               // 作者信息
    
    // 代码内容
    @SerializedName("code")
    val code: String = "",                          // JavaScript 代码
    @SerializedName("cssCode")
    val cssCode: String = "",                       // CSS 代码（可选）
    
    // 执行配置
    @SerializedName("runAt")
    val runAt: ModuleRunTime = ModuleRunTime.DOCUMENT_END, // 执行时机
    @SerializedName("urlMatches")
    val urlMatches: List<UrlMatchRule> = emptyList(), // URL 匹配规则（空则匹配所有）
    
    // 权限声明
    @SerializedName("permissions")
    val permissions: List<ModulePermission> = emptyList(), // 所需权限
    
    // 配置项定义
    @SerializedName("configItems")
    val configItems: List<ModuleConfigItem> = emptyList(), // 用户可配置项
    @SerializedName("configValues")
    val configValues: Map<String, String> = emptyMap(),    // 用户配置值
    
    // 依赖关系
    @SerializedName("dependencies")
    val dependencies: List<String> = emptyList(),   // 依赖的其他模块ID
    
    // 状态
    @SerializedName("enabled")
    val enabled: Boolean = true,                    // 是否启用
    @SerializedName("builtIn")
    val builtIn: Boolean = false,                   // 是否为内置模块
    
    // 元数据
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    @SerializedName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson = Gson()
        
        /**
         * 从 JSON 字符串解析模块
         */
        fun fromJson(json: String): ExtensionModule? {
            return try {
                gson.fromJson(json, ExtensionModule::class.java)
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * 从分享码解析模块（Base64 编码的 JSON）
         */
        fun fromShareCode(shareCode: String): ExtensionModule? {
            return try {
                val json = String(android.util.Base64.decode(shareCode, android.util.Base64.DEFAULT))
                fromJson(json)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * 转换为 JSON 字符串
     */
    fun toJson(): String = gson.toJson(this)
    
    /**
     * 生成分享码（Base64 编码）
     */
    fun toShareCode(): String {
        return android.util.Base64.encodeToString(toJson().toByteArray(), android.util.Base64.NO_WRAP)
    }
    
    /**
     * 检查 URL 是否匹配此模块
     */
    fun matchesUrl(url: String): Boolean {
        if (urlMatches.isEmpty()) return true
        
        val includeRules = urlMatches.filter { !it.exclude }
        val excludeRules = urlMatches.filter { it.exclude }
        
        // 先检查排除规则
        for (rule in excludeRules) {
            if (matchRule(url, rule)) return false
        }
        
        // 如果没有包含规则，默认匹配
        if (includeRules.isEmpty()) return true
        
        // 检查包含规则
        return includeRules.any { matchRule(url, it) }
    }
    
    private fun matchRule(url: String, rule: UrlMatchRule): Boolean {
        return if (rule.isRegex) {
            try {
                Regex(rule.pattern).containsMatchIn(url)
            } catch (e: Exception) {
                false
            }
        } else {
            // 通配符匹配：* 匹配任意字符
            val regexPattern = rule.pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
            try {
                Regex(regexPattern, RegexOption.IGNORE_CASE).containsMatchIn(url)
            } catch (e: Exception) {
                url.contains(rule.pattern, ignoreCase = true)
            }
        }
    }
    
    /**
     * 生成最终执行的代码（注入配置值）
     */
    fun generateExecutableCode(): String {
        val configJson = gson.toJson(configValues)
        return """
            (function() {
                'use strict';
                // 模块配置
                const __MODULE_CONFIG__ = $configJson;
                const __MODULE_INFO__ = {
                    id: '${id}',
                    name: '${name.replace("'", "\\'")}',
                    version: '${version.name}'
                };
                
                // 配置访问函数
                function getConfig(key, defaultValue) {
                    return __MODULE_CONFIG__[key] !== undefined ? __MODULE_CONFIG__[key] : defaultValue;
                }
                
                // CSS 注入
                ${if (cssCode.isNotBlank()) """
                (function() {
                    const style = document.createElement('style');
                    style.id = 'ext-module-${id}';
                    style.textContent = `${cssCode.replace("`", "\\`")}`;
                    (document.head || document.documentElement).appendChild(style);
                })();
                """ else ""}
                
                // 用户代码
                try {
                    $code
                } catch(e) {
                    console.error('[ExtModule: ${name}] Error:', e);
                }
            })();
        """.trimIndent()
    }
    
    /**
     * 验证模块完整性
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) errors.add("模块名称不能为空")
        if (code.isBlank() && cssCode.isBlank()) errors.add("代码内容不能为空")
        
        // 验证配置项
        configItems.forEach { item ->
            if (item.required && configValues[item.key].isNullOrBlank()) {
                errors.add("配置项 '${item.name}' 为必填项")
            }
        }
        
        return errors
    }
}

/**
 * 模块包 - 用于导出/导入多个模块
 */
data class ModulePackage(
    @SerializedName("name")
    val name: String,                               // 包名称
    @SerializedName("description")
    val description: String = "",                   // 包描述
    @SerializedName("author")
    val author: ModuleAuthor? = null,               // 作者
    @SerializedName("modules")
    val modules: List<ExtensionModule>,             // 模块列表
    @SerializedName("version")
    val version: String = "1.0.0",                  // 包版本
    @SerializedName("createdAt")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val gson = Gson()
        
        fun fromJson(json: String): ModulePackage? {
            return try {
                gson.fromJson(json, ModulePackage::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    fun toJson(): String = gson.toJson(this)
}

/**
 * 模块分类分组 - 用于 UI 展示
 */
object ModuleCategoryGroups {
    
    /**
     * 分类分组定义
     */
    val groups = listOf(
        CategoryGroup(
            name = "内容处理",
            icon = "📝",
            categories = listOf(
                ModuleCategory.CONTENT_FILTER,
                ModuleCategory.CONTENT_ENHANCE,
                ModuleCategory.READING
            )
        ),
        CategoryGroup(
            name = "外观样式",
            icon = "🎨",
            categories = listOf(
                ModuleCategory.STYLE_MODIFIER,
                ModuleCategory.THEME
            )
        ),
        CategoryGroup(
            name = "功能增强",
            icon = "⚡",
            categories = listOf(
                ModuleCategory.FUNCTION_ENHANCE,
                ModuleCategory.AUTOMATION,
                ModuleCategory.NAVIGATION,
                ModuleCategory.INTERACTION
            )
        ),
        CategoryGroup(
            name = "数据工具",
            icon = "📊",
            categories = listOf(
                ModuleCategory.DATA_EXTRACT,
                ModuleCategory.DATA_SAVE,
                ModuleCategory.TRANSLATE
            )
        ),
        CategoryGroup(
            name = "媒体处理",
            icon = "🎬",
            categories = listOf(
                ModuleCategory.MEDIA,
                ModuleCategory.VIDEO,
                ModuleCategory.IMAGE,
                ModuleCategory.AUDIO
            )
        ),
        CategoryGroup(
            name = "安全隐私",
            icon = "🔒",
            categories = listOf(
                ModuleCategory.SECURITY,
                ModuleCategory.ANTI_TRACKING
            )
        ),
        CategoryGroup(
            name = "生活工具",
            icon = "🛠️",
            categories = listOf(
                ModuleCategory.SOCIAL,
                ModuleCategory.SHOPPING,
                ModuleCategory.ACCESSIBILITY
            )
        ),
        CategoryGroup(
            name = "开发调试",
            icon = "💻",
            categories = listOf(
                ModuleCategory.DEVELOPER
            )
        ),
        CategoryGroup(
            name = "其他",
            icon = "📦",
            categories = listOf(
                ModuleCategory.OTHER
            )
        )
    )
    
    /**
     * 获取分类所属的分组
     */
    fun getGroupForCategory(category: ModuleCategory): CategoryGroup? {
        return groups.find { it.categories.contains(category) }
    }
}

/**
 * 分类分组
 */
data class CategoryGroup(
    val name: String,
    val icon: String,
    val categories: List<ModuleCategory>
)

/**
 * 权限分组 - 用于 UI 展示
 */
object ModulePermissionGroups {
    
    val groups = listOf(
        PermissionGroup(
            name = "基础权限",
            permissions = listOf(
                ModulePermission.DOM_ACCESS,
                ModulePermission.DOM_OBSERVE,
                ModulePermission.CSS_INJECT
            )
        ),
        PermissionGroup(
            name = "存储权限",
            permissions = listOf(
                ModulePermission.STORAGE,
                ModulePermission.COOKIE,
                ModulePermission.INDEXED_DB,
                ModulePermission.CACHE
            )
        ),
        PermissionGroup(
            name = "网络权限",
            permissions = listOf(
                ModulePermission.NETWORK,
                ModulePermission.WEBSOCKET,
                ModulePermission.FETCH_INTERCEPT
            )
        ),
        PermissionGroup(
            name = "用户交互",
            permissions = listOf(
                ModulePermission.CLIPBOARD,
                ModulePermission.NOTIFICATION,
                ModulePermission.ALERT,
                ModulePermission.KEYBOARD,
                ModulePermission.MOUSE,
                ModulePermission.TOUCH
            )
        ),
        PermissionGroup(
            name = "设备权限",
            permissions = listOf(
                ModulePermission.LOCATION,
                ModulePermission.CAMERA,
                ModulePermission.MICROPHONE,
                ModulePermission.DEVICE_INFO
            )
        ),
        PermissionGroup(
            name = "媒体权限",
            permissions = listOf(
                ModulePermission.MEDIA,
                ModulePermission.FULLSCREEN,
                ModulePermission.PICTURE_IN_PICTURE,
                ModulePermission.SCREEN_CAPTURE
            )
        ),
        PermissionGroup(
            name = "文件权限",
            permissions = listOf(
                ModulePermission.DOWNLOAD,
                ModulePermission.FILE_ACCESS
            )
        ),
        PermissionGroup(
            name = "高级权限",
            permissions = listOf(
                ModulePermission.EVAL,
                ModulePermission.IFRAME,
                ModulePermission.WINDOW_OPEN,
                ModulePermission.HISTORY,
                ModulePermission.NAVIGATION
            )
        )
    )
}

/**
 * 权限分组
 */
data class PermissionGroup(
    val name: String,
    val permissions: List<ModulePermission>
)

/**
 * 常用模块预设 - 快速创建常见类型模块
 */
object ModulePresets {
    
    /**
     * 创建元素屏蔽模块
     */
    fun createElementBlocker(
        name: String,
        selectors: List<String>,
        description: String = "屏蔽指定元素"
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "🚫",
            category = ModuleCategory.CONTENT_FILTER,
            tags = listOf("屏蔽", "隐藏"),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS, ModulePermission.DOM_OBSERVE),
            code = """
                const selectors = ${selectors.joinToString(",", "[", "]") { "\"$it\"" }};
                function hide() {
                    selectors.forEach(s => {
                        document.querySelectorAll(s).forEach(el => el.style.display = 'none');
                    });
                }
                hide();
                new MutationObserver(hide).observe(document.body, { childList: true, subtree: true });
            """.trimIndent()
        )
    }
    
    /**
     * 创建样式注入模块
     */
    fun createStyleInjector(
        name: String,
        cssCode: String,
        description: String = "注入自定义样式"
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "🎨",
            category = ModuleCategory.STYLE_MODIFIER,
            tags = listOf("样式", "CSS"),
            runAt = ModuleRunTime.DOCUMENT_START,
            permissions = listOf(ModulePermission.CSS_INJECT),
            cssCode = cssCode
        )
    }
    
    /**
     * 创建自动点击模块
     */
    fun createAutoClicker(
        name: String,
        selector: String,
        delay: Int = 1000,
        description: String = "自动点击指定元素"
    ): ExtensionModule {
        return ExtensionModule(
            name = name,
            description = description,
            icon = "🖱️",
            category = ModuleCategory.AUTOMATION,
            tags = listOf("自动", "点击"),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            code = """
                setTimeout(() => {
                    const el = document.querySelector('$selector');
                    if (el) el.click();
                }, $delay);
            """.trimIndent()
        )
    }
    
    /**
     * 创建悬浮按钮模块
     */
    fun createFloatingButton(
        name: String,
        buttonText: String,
        onClick: String,
        position: String = "bottom-right",
        description: String = "添加悬浮按钮"
    ): ExtensionModule {
        val positionStyle = when (position) {
            "bottom-left" -> "bottom: 80px; left: 20px;"
            "top-right" -> "top: 80px; right: 20px;"
            "top-left" -> "top: 80px; left: 20px;"
            else -> "bottom: 80px; right: 20px;"
        }
        
        return ExtensionModule(
            name = name,
            description = description,
            icon = "🔘",
            category = ModuleCategory.FUNCTION_ENHANCE,
            tags = listOf("按钮", "悬浮"),
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            code = """
                const btn = document.createElement('div');
                btn.textContent = '$buttonText';
                btn.style.cssText = 'position:fixed;$positionStyle;z-index:99999;padding:12px 20px;background:rgba(0,0,0,0.8);color:white;border-radius:25px;cursor:pointer;font-size:14px;box-shadow:0 2px 10px rgba(0,0,0,0.3);';
                btn.onclick = () => { $onClick };
                document.body.appendChild(btn);
            """.trimIndent()
        )
    }
}
