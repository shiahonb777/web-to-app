package com.webtoapp.data.model

/**
 * AI 供应商
 */
enum class AiProvider(
    val displayName: String,
    val baseUrl: String,
    val modelsEndpoint: String = "/v1/models",
    val description: String = "",
    val apiKeyUrl: String = "",
    val pricing: String = ""
) {
    OPENAI(
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com",
        modelsEndpoint = "/v1/models",
        description = "文本表现出色，推理能力强，支持文本、视觉和图像生成",
        apiKeyUrl = "https://platform.openai.com/api-keys",
        pricing = "GPT 5.1 系列约 $10/ 百万token"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api",
        modelsEndpoint = "/v1/models",
        description = "聚合多家 AI 供应商，统一接口调用。可用同一 API Key 调用 OpenAI、Claude、Gemini 等多种模型",
        apiKeyUrl = "https://openrouter.ai/keys",
        pricing = "按模型不同计费，价格透明，有免费模型，强烈推荐"
    ),
    ANTHROPIC(
        displayName = "Anthropic/Claude",
        baseUrl = "https://api.anthropic.com",
        modelsEndpoint = "/v1/models",
        description = "Claude 系列模型，擅长文本理解和代码生成且有视觉支持，编程能力强。",
        apiKeyUrl = "https://console.anthropic.com/settings/keys",
        pricing = "Claude 4.5 Sonnet 约 $15/百万 token"
    ),
    GOOGLE(
        displayName = "Google/Gemini",
        baseUrl = "https://generativelanguage.googleapis.com",
        modelsEndpoint = "/v1beta/models",
        description = "★推荐★ Gemini 3.0 Pro 前端表现出色，原生多模态支持，全面顶配支持。",
        apiKeyUrl = "https://aistudio.google.com/apikey",
        pricing = "有免费额度，超出后按 token 计费"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com",
        modelsEndpoint = "/v1/models",
        description = "国家队，性价比高。目前仅支持文本和图像文本生成",
        apiKeyUrl = "https://platform.deepseek.com/api_keys",
        pricing = "极低价格，约 ¥0.4/百万 token"
    ),
    MINIMAX(
        displayName = "MiniMax",
        baseUrl = "https://api.minimax.chat",
        modelsEndpoint = "/v1/models",
        description = "国产，支持高音质人声语音克隆/合成。文本模型性能优秀，代码agent能力较强",
        apiKeyUrl = "https://platform.minimaxi.com/user-center/basic-information/interface-key",
        pricing = "价格较低，约 $1/百万 token"
    ),
    GLM(
        displayName = "智谱GLM",
        baseUrl = "https://open.bigmodel.cn/api/paas",
        modelsEndpoint = "/v4/models",
        description = "国产，GLM-4.6 系列性能优秀，编码能力强，支持多模态",
        apiKeyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        pricing = "价格较低，约 $2/百万 token"
    ),
    GROK(
        displayName = "xAI/Grok",
        baseUrl = "https://api.x.ai",
        modelsEndpoint = "/v1/models",
        description = "马斯克旗下 xAI 的 Grok 系列，支持文本和视觉",
        apiKeyUrl = "https://console.x.ai/",
        pricing = "价格便宜，Grok-4.1-fast 约 $0.5/百万 token"
    ),
    VOLCANO(
        displayName = "火山引擎",
        baseUrl = "https://ark.cn-beijing.volces.com/api",
        modelsEndpoint = "/v3/models",
        description = "字节跳动旗下，豆包大模型生态均衡。推荐模型：doubao-1.6-pro-256k",
        apiKeyUrl = "https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey",
        pricing = "有免费额度，价格便宜"
    ),
    SILICONFLOW(
        displayName = "硅基流动",
        baseUrl = "https://api.siliconflow.cn",
        modelsEndpoint = "/v1/models",
        description = "国产 AI 平台，聚合多种开源模型。",
        apiKeyUrl = "https://cloud.siliconflow.cn/account/ak",
        pricing = "有免费额度，价格便宜"
    ),
    CUSTOM(
        displayName = "自定义",
        baseUrl = "",
        modelsEndpoint = "/v1/models",
        description = "兼容 OpenAI API 格式的自定义服务。需要填写完整的 Base URL",
        apiKeyUrl = "",
        pricing = "取决于服务商"
    )
}

/**
 * 模型能力标签
 */
enum class ModelCapability(val displayName: String, val description: String) {
    TEXT("文本生成", "基础文本对话和生成"),
    AUDIO("音频理解", "理解和转录音频内容"),
    IMAGE("图像理解", "理解和分析图片内容"),
    IMAGE_GENERATION("图像生成", "生成图片和图标"),
    VIDEO("视频理解", "理解视频内容"),
    CODE("代码生成", "生成和理解代码"),
    FUNCTION_CALL("函数调用", "支持工具调用"),
    LONG_CONTEXT("长上下文", "支持超长文本输入")
}

/**
 * AI 模型配置
 */
data class AiModel(
    val id: String,                        // 模型 ID（如 gpt-5.1-codex）
    val name: String,                      // 显示名称
    val provider: AiProvider,              // 供应商
    val capabilities: List<ModelCapability> = listOf(ModelCapability.TEXT), // 能力标签
    val contextLength: Int = 4096,         // 上下文长度（token）
    val inputPrice: Double = 0.0,          // 输入价格（$/百万 token）
    val outputPrice: Double = 0.0,         // 输出价格（$/百万 token）
    val isCustom: Boolean = false          // 是否为手动添加的模型
)

/**
 * API 密钥配置
 */
data class ApiKeyConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val provider: AiProvider,
    val apiKey: String,
    val baseUrl: String? = null,           // 自定义 base URL（可选）
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 已保存的模型配置（用户选择并保存的模型）
 */
data class SavedModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val model: AiModel,
    val apiKeyId: String,                  // 关联的 API Key ID
    val alias: String? = null,             // 用户自定义别名
    val capabilities: List<ModelCapability>, // 用户指定的能力标签
    val isDefault: Boolean = false,        // 是否为默认模型
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * AI 配置（整体配置，存储在 DataStore）
 */
data class AiSettings(
    val apiKeys: List<ApiKeyConfig> = emptyList(),
    val savedModels: List<SavedModel> = emptyList(),
    val defaultModelId: String? = null     // 默认模型 ID
)

/**
 * LRC 生成任务状态
 */
enum class LrcTaskStatus {
    PENDING,     // 等待中
    PROCESSING,  // 处理中
    COMPLETED,   // 已完成
    FAILED       // 失败
}

/**
 * LRC 生成任务
 */
data class LrcTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bgmItemId: String,           // 音乐项 ID
    val bgmName: String,             // 音乐名称
    val bgmPath: String,             // 音乐路径
    val modelId: String,             // 使用的模型 ID
    val status: LrcTaskStatus = LrcTaskStatus.PENDING,
    val progress: Int = 0,           // 进度 0-100
    val resultLrc: LrcData? = null,  // 生成的歌词
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * 预置字幕主题
 */
object PresetLrcThemes {
    val themes = listOf(
        LrcTheme(
            id = "default",
            name = "默认",
            textColor = "#FFFFFF",
            highlightColor = "#FFD700",
            backgroundColor = "#80000000",
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "karaoke",
            name = "卡拉OK",
            textColor = "#FFFFFF",
            highlightColor = "#FF4081",
            backgroundColor = "#00000000",
            strokeColor = "#000000",
            strokeWidth = 2f,
            animationType = LrcAnimationType.KARAOKE
        ),
        LrcTheme(
            id = "neon",
            name = "霓虹",
            textColor = "#00FFFF",
            highlightColor = "#FF00FF",
            backgroundColor = "#40000000",
            shadowEnabled = true,
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "minimal",
            name = "极简",
            fontSize = 16f,
            textColor = "#CCCCCC",
            highlightColor = "#FFFFFF",
            backgroundColor = "#00000000",
            animationType = LrcAnimationType.SLIDE_UP
        ),
        LrcTheme(
            id = "classic",
            name = "经典",
            fontSize = 20f,
            textColor = "#FFE4B5",
            highlightColor = "#FFD700",
            backgroundColor = "#60000000",
            animationType = LrcAnimationType.TYPEWRITER
        ),
        LrcTheme(
            id = "dark",
            name = "暗夜",
            textColor = "#AAAAAA",
            highlightColor = "#4FC3F7",
            backgroundColor = "#E0000000",
            animationType = LrcAnimationType.SCALE
        ),
        LrcTheme(
            id = "romantic",
            name = "浪漫",
            textColor = "#FFB6C1",
            highlightColor = "#FF69B4",
            backgroundColor = "#40000000",
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "energetic",
            name = "活力",
            fontSize = 22f,
            textColor = "#FFEB3B",
            highlightColor = "#FF5722",
            backgroundColor = "#00000000",
            strokeColor = "#000000",
            strokeWidth = 3f,
            animationType = LrcAnimationType.SCALE
        )
    )
    
    fun getById(id: String): LrcTheme? = themes.find { it.id == id }
}
