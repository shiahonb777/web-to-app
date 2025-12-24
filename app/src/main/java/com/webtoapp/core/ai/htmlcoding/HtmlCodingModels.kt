package com.webtoapp.core.ai.htmlcoding

import java.util.UUID

/**
 * HTML编程AI - 数据模型
 */

/**
 * 对话会话
 */
data class HtmlCodingSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "新对话",
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
    val rules: List<String> = listOf("使用中文进行对话"),  // 规则列表
    val selectedTemplateId: String? = null,    // 选中的模板ID
    val selectedStyleId: String? = null,       // 选中的风格ID
    // 工具包配置
    val enabledTools: Set<HtmlToolType> = setOf(HtmlToolType.WRITE_HTML)  // 启用的工具
)

/**
 * HTML 工具类型
 */
enum class HtmlToolType(
    val displayName: String,
    val description: String,
    val icon: String,
    val requiresImageModel: Boolean = false  // 是否需要图像模型
) {
    WRITE_HTML(
        displayName = "写入 HTML",
        description = "创建或覆盖完整的 HTML 页面",
        icon = "📝"
    ),
    EDIT_HTML(
        displayName = "编辑 HTML",
        description = "在指定位置替换、插入或删除代码片段",
        icon = "✏️"
    ),
    GENERATE_IMAGE(
        displayName = "AI 图像生成",
        description = "使用 AI 生成图像并嵌入到 HTML 中作为插图",
        icon = "🎨",
        requiresImageModel = true
    ),
    GET_CONSOLE_LOGS(
        displayName = "获取控制台日志",
        description = "获取页面运行时的 console.log 输出和错误信息",
        icon = "📋"
    ),
    CHECK_SYNTAX(
        displayName = "语法检查",
        description = "检查 HTML/CSS/JavaScript 语法错误",
        icon = "🔍"
    ),
    AUTO_FIX(
        displayName = "自动修复",
        description = "自动修复检测到的语法错误",
        icon = "🔧"
    )
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
enum class TemplateCategory(val displayName: String) {
    MODERN("现代简约"),
    GLASSMORPHISM("玻璃拟态"),
    NEUMORPHISM("新拟物"),
    GRADIENT("渐变炫彩"),
    DARK("暗黑主题"),
    MINIMAL("极简风格"),
    RETRO("复古风格"),
    CYBERPUNK("赛博朋克"),
    NATURE("自然清新"),
    BUSINESS("商务专业"),
    CREATIVE("创意艺术"),
    GAME("游戏风格")
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
enum class StyleReferenceCategory(val displayName: String) {
    MOVIE("电影"),
    BOOK("书籍"),
    ANIME("动画"),
    GAME("游戏"),
    BRAND("品牌"),
    ART("艺术流派"),
    ERA("时代风格"),
    CULTURE("文化风格")
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
