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
 * 
 * 注意：contextLength = -1 表示未知，inputPrice = -1.0 表示未知
 */
data class AiModel(
    val id: String,                        // 模型 ID（如 gpt-5.1-codex）
    val name: String,                      // 显示名称
    val provider: AiProvider,              // 供应商
    val capabilities: List<ModelCapability> = emptyList(), // 能力标签（空=未知）
    val contextLength: Int = -1,           // 上下文长度（token），-1 表示未知
    val inputPrice: Double = -1.0,         // 输入价格（$/百万 token），-1 表示未知
    val outputPrice: Double = -1.0,        // 输出价格（$/百万 token），-1 表示未知
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
 * 预置字幕主题（豪华版 - 每个主题都有独特的视觉语言）
 */
object PresetLrcThemes {
    val themes = listOf(
        // 1. 星河 - 深空紫蓝渐变，银河流光
        LrcTheme(
            id = "galaxy",
            name = "🌌 星河",
            fontSize = 20f,
            textColor = "#C5CAE9",           // 淡紫蓝
            highlightColor = "#E040FB",       // 亮紫红
            backgroundColor = "#F50D1B2D",    // 深空蓝
            strokeColor = "#7C4DFF",          // 紫色描边
            strokeWidth = 1.5f,
            shadowEnabled = true,
            animationType = LrcAnimationType.FADE,
            position = LrcPosition.CENTER
        ),
        // 2. 卡拉OK - 炫彩舞台，荧光渐变
        LrcTheme(
            id = "karaoke",
            name = "🎤 卡拉OK",
            fontSize = 24f,
            textColor = "#FFFFFF",
            highlightColor = "#FF1744",       // 炫红
            backgroundColor = "#F0000000",
            strokeColor = "#FF4081",          // 粉红描边
            strokeWidth = 2f,
            shadowEnabled = true,
            animationType = LrcAnimationType.KARAOKE,
            position = LrcPosition.BOTTOM
        ),
        // 3. 霓虹夜 - 赛博朋克，冷暖对比
        LrcTheme(
            id = "cyberpunk",
            name = "💜 霓虹夜",
            fontSize = 21f,
            textColor = "#00E5FF",            // 青色
            highlightColor = "#FF00FF",       // 品红
            backgroundColor = "#F8050510",    // 纯黑微红
            strokeColor = "#00FFFF",          // 青色描边
            strokeWidth = 1f,
            shadowEnabled = true,
            animationType = LrcAnimationType.FADE,
            position = LrcPosition.CENTER
        ),
        // 4. 月光 - 优雅银白，清冷高级
        LrcTheme(
            id = "moonlight",
            name = "🌙 月光",
            fontSize = 19f,
            textColor = "#B0BEC5",            // 银灰
            highlightColor = "#ECEFF1",       // 月白
            backgroundColor = "#E8101820",    // 深蓝夜
            strokeColor = "#546E7A",
            strokeWidth = 0.5f,
            shadowEnabled = true,
            animationType = LrcAnimationType.SLIDE_UP,
            position = LrcPosition.CENTER
        ),
        // 5. 金曲 - 复古奢华，金色年代
        LrcTheme(
            id = "golden",
            name = "🏆 金曲",
            fontSize = 22f,
            textColor = "#FFD54F",            // 琥珀金
            highlightColor = "#FFD700",       // 纯金
            backgroundColor = "#E81A1208",    // 深棕
            strokeColor = "#FFA000",          // 橙金描边
            strokeWidth = 1.5f,
            shadowEnabled = true,
            animationType = LrcAnimationType.TYPEWRITER,
            position = LrcPosition.CENTER
        ),
        // 6. 深海 - 神秘蓝绿，水下氛围
        LrcTheme(
            id = "ocean",
            name = "🌊 深海",
            fontSize = 20f,
            textColor = "#4DD0E1",            // 青绿
            highlightColor = "#00BCD4",       // 亮青
            backgroundColor = "#F5001A28",    // 深海蓝
            strokeColor = "#006064",          // 深青描边
            strokeWidth = 1f,
            shadowEnabled = true,
            animationType = LrcAnimationType.SCALE,
            position = LrcPosition.CENTER
        ),
        // 7. 樱落 - 日式美学，粉白渐变
        LrcTheme(
            id = "sakura",
            name = "🌸 樱落",
            fontSize = 20f,
            textColor = "#F8BBD0",            // 樱粉
            highlightColor = "#F50057",       // 玫红
            backgroundColor = "#E8180810",    // 暗樱
            strokeColor = "#FF4081",          // 粉红描边
            strokeWidth = 1f,
            shadowEnabled = true,
            animationType = LrcAnimationType.FADE,
            position = LrcPosition.CENTER
        ),
        // 8. 烈焰 - 火焰渐变，激情燃烧
        LrcTheme(
            id = "inferno",
            name = "🔥 烈焰",
            fontSize = 23f,
            textColor = "#FFAB40",            // 橙黄
            highlightColor = "#FF3D00",       // 烈焰红
            backgroundColor = "#F0100800",    // 深红黑
            strokeColor = "#DD2C00",          // 暗红描边
            strokeWidth = 2f,
            shadowEnabled = true,
            animationType = LrcAnimationType.SCALE,
            position = LrcPosition.CENTER
        )
    )
    
    fun getById(id: String): LrcTheme? = themes.find { it.id == id }
}
