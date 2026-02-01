package com.webtoapp.core.ai.htmlcoding

import com.webtoapp.core.i18n.Strings

import java.util.UUID

/**
 * HTML编程AI - 数据模型
 */

/**
 * 对话会话
 */
data class HtmlCodingSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",  // 使用空字符串，UI层显示时使用 Strings.newConversation
    val messages: List<HtmlCodingMessage> = emptyList(),
    val checkpoints: List<ProjectCheckpoint> = emptyList(),  // 版本检查点
    val currentCheckpointIndex: Int = -1,                     // 当前检查点索引
    val config: SessionConfig = SessionConfig(),
    val projectDir: String? = null,                           // 项目文件夹路径
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 会话配置
 */
data class SessionConfig(
    val textModelId: String? = null,           // 文本模型ID
    val imageModelId: String? = null,          // 图像模型ID（可选）
    val temperature: Float = 0.7f,             // 温度 0.0-2.0
    val rules: List<String> = listOf(Strings.ruleUseChineseConversation),  // 规则列表
    val selectedTemplateId: String? = null,    // 选中的模板ID
    val selectedStyleId: String? = null,       // 选中的风格ID
    // 工具包配置
    val enabledTools: Set<HtmlToolType> = setOf(HtmlToolType.WRITE_HTML)  // 启用的工具
)

const val LEGACY_DEFAULT_RULE_ZH = "使用中文进行对话"
const val LEGACY_RULE_CODE_COMMENTS_ZH = "代码注释使用中文"
const val LEGACY_RULE_GAME_FLOW_ZH = "游戏要有完整的开始、进行、结束流程"
const val LEGACY_RULE_GAME_SCORE_ZH = "添加分数显示和游戏说明"
const val LEGACY_RULE_GAME_TOUCH_ZH = "确保触摸控制流畅"
const val LEGACY_RULE_ANIMATION_SMOOTH_ZH = "添加流畅的 CSS 动画"
const val LEGACY_RULE_ANIMATION_TRANSITION_ZH = "使用 transition 优化交互反馈"
const val LEGACY_RULE_ANIMATION_PERFORMANCE_ZH = "考虑性能，避免过度动画"
const val LEGACY_RULE_FORM_VALIDATION_ZH = "表单要有完整的验证逻辑"
const val LEGACY_RULE_FORM_LABELS_ZH = "输入框要有清晰的标签和提示"
const val LEGACY_RULE_FORM_SUBMIT_LOADING_ZH = "提交按钮要有加载状态"

/**
 * HTML 工具类型
 */
enum class HtmlToolType(
    val icon: String,
    val requiresImageModel: Boolean = false
) {
    WRITE_HTML("📝"),
    EDIT_HTML("✏️"),
    GENERATE_IMAGE("🎨", true),
    GET_CONSOLE_LOGS("📋"),
    CHECK_SYNTAX("🔍"),
    AUTO_FIX("🔧");

    fun getDisplayName(): String = when (this) {
        WRITE_HTML -> Strings.toolWriteHtml
        EDIT_HTML -> Strings.toolEditHtml
        GENERATE_IMAGE -> Strings.toolGenerateImage
        GET_CONSOLE_LOGS -> Strings.toolGetConsoleLogs
        CHECK_SYNTAX -> Strings.toolCheckSyntax
        AUTO_FIX -> Strings.toolAutoFix
    }

    fun getDescription(): String = when (this) {
        WRITE_HTML -> Strings.toolWriteHtmlDesc
        EDIT_HTML -> Strings.toolEditHtmlDesc
        GENERATE_IMAGE -> Strings.toolGenerateImageDesc
        GET_CONSOLE_LOGS -> Strings.toolGetConsoleLogsDesc
        CHECK_SYNTAX -> Strings.toolCheckSyntaxDesc
        AUTO_FIX -> Strings.toolAutoFixDesc
    }
}

/**
 * 对话消息
 */
data class HtmlCodingMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val images: List<String> = emptyList(),    // 图片路径列表（最多3张）
    val thinking: String? = null,               // 思考过程（如有）
    val codeBlocks: List<CodeBlock> = emptyList(), // 提取的代码块（兼容旧数据）
    val fileRefs: List<FileReference> = emptyList(), // 文件引用（新机制）
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,              // 是否被编辑过
    val originalContent: String? = null         // 原始内容（编辑前）
)

/**
 * 文件引用（指向项目文件夹中的实际文件）
 */
data class FileReference(
    val filename: String,           // 文件名 (如 index_v2.html)
    val baseName: String,           // 基础文件名 (如 index)
    val version: Int,               // 版本号
    val type: ProjectFileType,      // 文件类型
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 消息角色
 */
enum class MessageRole {
    USER,       // 用户
    ASSISTANT,  // AI助手
    SYSTEM      // 系统消息
}

/**
 * 代码块
 */
data class CodeBlock(
    val id: String = UUID.randomUUID().toString(),
    val language: String = "html",              // 语言类型
    val filename: String? = null,               // 文件名
    val content: String,                        // 代码内容
    val isComplete: Boolean = true              // 是否完整代码
)

/**
 * 项目检查点（版本控制）
 */
data class ProjectCheckpoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                           // 检查点名称
    val description: String = "",               // 描述
    val messageIndex: Int,                      // 消息索引位置
    val files: List<ProjectFile>,               // 该版本的所有文件
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 项目文件
 */
data class ProjectFile(
    val name: String,                           // 文件名
    val content: String,                        // 文件内容
    val type: ProjectFileType = ProjectFileType.HTML
)

/**
 * 项目文件类型
 */
enum class ProjectFileType(val extension: String, val mimeType: String) {
    HTML("html", "text/html"),
    CSS("css", "text/css"),
    JS("js", "application/javascript"),
    SVG("svg", "image/svg+xml"),
    JSON("json", "application/json"),
    IMAGE("png", "image/png"),
    OTHER("txt", "text/plain")
}

/**
 * 主题风格模板
 */
data class StyleTemplate(
    val id: String,
    val name: String,                           // 模板名称
    val category: TemplateCategory,             // 分类
    val description: String,                    // 描述
    val previewImage: String? = null,           // 预览图路径
    val cssFramework: String? = null,           // 使用的CSS框架
    val colorScheme: ColorScheme? = null,       // 配色方案
    val promptHint: String,                     // 提示词提示
    val exampleCode: String? = null             // 示例代码
)

/**
 * 模板分类
 */
enum class TemplateCategory {
    MODERN,
    GLASSMORPHISM,
    NEUMORPHISM,
    GRADIENT,
    DARK,
    MINIMAL,
    RETRO,
    CYBERPUNK,
    NATURE,
    BUSINESS,
    CREATIVE,
    GAME;

    fun getDisplayName(): String = when (this) {
        MODERN -> Strings.templateModern
        GLASSMORPHISM -> Strings.templateGlassmorphism
        NEUMORPHISM -> Strings.templateNeumorphism
        GRADIENT -> Strings.templateGradient
        DARK -> Strings.templateDark
        MINIMAL -> Strings.templateMinimal
        RETRO -> Strings.templateRetro
        CYBERPUNK -> Strings.templateCyberpunk
        NATURE -> Strings.templateNature
        BUSINESS -> Strings.templateBusiness
        CREATIVE -> Strings.templateCreative
        GAME -> Strings.templateGame
    }
}

/**
 * 配色方案
 */
data class ColorScheme(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String,
    val text: String,
    val accent: String
)

/**
 * 风格参考词
 */
data class StyleReference(
    val id: String,
    val name: String,                           // 风格名称（如"哈利波特风格"）
    val category: StyleReferenceCategory,       // 分类
    val keywords: List<String>,                 // 关键词
    val description: String,                    // 风格描述
    val colorHints: List<String>,               // 配色提示
    val elementHints: List<String>              // 元素提示
)

/**
 * 风格参考分类
 */
enum class StyleReferenceCategory {
    MOVIE,
    BOOK,
    ANIME,
    GAME,
    BRAND,
    ART,
    ERA,
    CULTURE;

    fun getDisplayName(): String = when (this) {
        MOVIE -> Strings.styleRefMovie
        BOOK -> Strings.styleRefBook
        ANIME -> Strings.styleRefAnime
        GAME -> Strings.styleRefGame
        BRAND -> Strings.styleRefBrand
        ART -> Strings.styleRefArt
        ERA -> Strings.styleRefEra
        CULTURE -> Strings.styleRefCulture
    }
}

/**
 * Rules 模板
 */
data class RulesTemplate(
    val id: String,
    val name: String,
    val description: String,
    val rules: List<String>
)

/**
 * 图像生成请求
 */
data class ImageGenerationRequest(
    val prompt: String,
    val negativePrompt: String? = null,
    val width: Int = 512,
    val height: Int = 512,
    val style: String? = null
)

/**
 * 图像生成结果
 */
data class ImageGenerationResult(
    val success: Boolean,
    val imageUrl: String? = null,
    val localPath: String? = null,
    val error: String? = null
)

/**
 * AI响应解析结果
 */
data class ParsedAiResponse(
    val textContent: String,                    // 纯文本内容
    val thinking: String?,                      // 思考内容
    val codeBlocks: List<CodeBlock>,           // 代码块列表
    val imageRequests: List<ImageGenerationRequest> // 图像生成请求
)

/**
 * 对话状态
 */
sealed class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    data class Streaming(val partialContent: String) : ChatState()
    data class GeneratingImage(val prompt: String) : ChatState()
    data class Error(val message: String) : ChatState()
}

/**
 * 保存配置
 */
data class SaveConfig(
    val directory: String,                      // 保存目录
    val projectName: String,                    // 项目名称
    val createFolder: Boolean = true,           // 是否创建文件夹
    val overwrite: Boolean = false              // 是否覆盖
)

/**
 * 代码库项目
 */
data class CodeLibraryItem(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,                      // 关联的会话ID
    val messageId: String,                      // 关联的消息ID
    val title: String,                          // 项目标题
    val description: String = "",               // 描述
    val files: List<ProjectFile>,               // 文件列表
    val previewHtml: String,                    // 预览用的合并HTML
    val conversationContext: String,            // 对话上下文摘要
    val userPrompt: String,                     // 用户原始提问
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),       // 标签
    val isFavorite: Boolean = false             // 是否收藏
)

/**
 * 对话检查点（增强版）
 */
data class ConversationCheckpoint(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,                      // 会话ID
    val name: String,                           // 检查点名称
    val messageCount: Int,                      // 消息数量
    val messages: List<HtmlCodingMessage>,      // 完整消息列表快照
    val codeLibraryIds: List<String>,           // 关联的代码库项目ID列表
    val config: SessionConfig,                  // 会话配置快照
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 获取本地化的HTML工具显示名称
 */
fun HtmlToolType.getLocalizedDisplayName(): String {
    return when (this) {
        HtmlToolType.WRITE_HTML -> com.webtoapp.core.i18n.Strings.featureWriteHtml
        HtmlToolType.EDIT_HTML -> com.webtoapp.core.i18n.Strings.featureEditHtml
        HtmlToolType.GENERATE_IMAGE -> com.webtoapp.core.i18n.Strings.aiImageGeneration
        HtmlToolType.GET_CONSOLE_LOGS -> com.webtoapp.core.i18n.Strings.featureGetConsoleLogs
        HtmlToolType.CHECK_SYNTAX -> com.webtoapp.core.i18n.Strings.featureCheckSyntax
        HtmlToolType.AUTO_FIX -> com.webtoapp.core.i18n.Strings.featureAutoFix
    }
}

/**
 * 获取本地化的HTML工具描述
 */
fun HtmlToolType.getLocalizedDescription(): String {
    return when (this) {
        HtmlToolType.WRITE_HTML -> com.webtoapp.core.i18n.Strings.writeHtmlDesc
        HtmlToolType.EDIT_HTML -> com.webtoapp.core.i18n.Strings.editHtmlDesc
        HtmlToolType.GENERATE_IMAGE -> com.webtoapp.core.i18n.Strings.generateImageDesc
        HtmlToolType.GET_CONSOLE_LOGS -> com.webtoapp.core.i18n.Strings.getConsoleLogsDesc
        HtmlToolType.CHECK_SYNTAX -> com.webtoapp.core.i18n.Strings.checkSyntaxDesc
        HtmlToolType.AUTO_FIX -> com.webtoapp.core.i18n.Strings.autoFixDesc
    }
}

/**
 * 获取本地化的模板分类显示名称
 */
fun TemplateCategory.getLocalizedDisplayName(): String {
    return when (this) {
        TemplateCategory.MODERN -> com.webtoapp.core.i18n.Strings.templateModern
        TemplateCategory.GLASSMORPHISM -> com.webtoapp.core.i18n.Strings.templateGlassmorphism
        TemplateCategory.NEUMORPHISM -> com.webtoapp.core.i18n.Strings.templateNeumorphism
        TemplateCategory.GRADIENT -> com.webtoapp.core.i18n.Strings.templateGradient
        TemplateCategory.DARK -> com.webtoapp.core.i18n.Strings.templateDark
        TemplateCategory.MINIMAL -> com.webtoapp.core.i18n.Strings.templateMinimal
        TemplateCategory.RETRO -> com.webtoapp.core.i18n.Strings.templateRetro
        TemplateCategory.CYBERPUNK -> com.webtoapp.core.i18n.Strings.templateCyberpunk
        TemplateCategory.NATURE -> com.webtoapp.core.i18n.Strings.templateNature
        TemplateCategory.BUSINESS -> com.webtoapp.core.i18n.Strings.templateBusiness
        TemplateCategory.CREATIVE -> com.webtoapp.core.i18n.Strings.templateCreative
        TemplateCategory.GAME -> com.webtoapp.core.i18n.Strings.templateGame
    }
}

/**
 * 获取本地化的风格参考分类显示名称
 */
fun StyleReferenceCategory.getLocalizedDisplayName(): String {
    return when (this) {
        StyleReferenceCategory.MOVIE -> com.webtoapp.core.i18n.Strings.styleMovie
        StyleReferenceCategory.BOOK -> com.webtoapp.core.i18n.Strings.styleBook
        StyleReferenceCategory.ANIME -> com.webtoapp.core.i18n.Strings.styleAnime
        StyleReferenceCategory.GAME -> com.webtoapp.core.i18n.Strings.styleGame
        StyleReferenceCategory.BRAND -> com.webtoapp.core.i18n.Strings.styleBrand
        StyleReferenceCategory.ART -> com.webtoapp.core.i18n.Strings.styleArt
        StyleReferenceCategory.ERA -> com.webtoapp.core.i18n.Strings.styleEra
        StyleReferenceCategory.CULTURE -> com.webtoapp.core.i18n.Strings.styleCulture
    }
}
