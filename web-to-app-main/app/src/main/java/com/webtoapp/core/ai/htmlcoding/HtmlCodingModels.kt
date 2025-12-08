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
    val selectedStyleId: String? = null        // 选中的风格ID
)

/**
 * 对话消息
 */
data class HtmlCodingMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val images: List<String> = emptyList(),    // 图片路径列表（最多3张）
    val thinking: String? = null,               // 思考过程（如有）
    val codeBlocks: List<CodeBlock> = emptyList(), // 提取的代码块
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,              // 是否被编辑过
    val originalContent: String? = null         // 原始内容（编辑前）
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
