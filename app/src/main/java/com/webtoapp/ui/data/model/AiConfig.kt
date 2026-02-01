package com.webtoapp.data.model

import com.webtoapp.core.i18n.Strings

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
        description = "Excellent text performance, strong reasoning, supports text, vision, and image generation",
        apiKeyUrl = "https://platform.openai.com/api-keys",
        pricing = "GPT 5.1 series ~ $10 per 1M tokens"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api",
        modelsEndpoint = "/v1/models",
        description = "Aggregates multiple AI providers with a unified API. Use one API key to access OpenAI, Claude, Gemini, and more",
        apiKeyUrl = "https://openrouter.ai/keys",
        pricing = "Pricing varies by model; transparent pricing with free models available"
    ),
    ANTHROPIC(
        displayName = "Anthropic/Claude",
        baseUrl = "https://api.anthropic.com",
        modelsEndpoint = "/v1/models",
        description = "Claude models excel at text understanding and code generation with vision support",
        apiKeyUrl = "https://console.anthropic.com/settings/keys",
        pricing = "Claude 4.5 Sonnet ~ $15 per 1M tokens"
    ),
    GOOGLE(
        displayName = "Google/Gemini",
        baseUrl = "https://generativelanguage.googleapis.com",
        modelsEndpoint = "/v1beta/models",
        description = "★ Recommended ★ Gemini 3.0 Pro excels in front-end use with native multimodal support",
        apiKeyUrl = "https://aistudio.google.com/apikey",
        pricing = "Free quota available; charged per token after"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com",
        modelsEndpoint = "/v1/models",
        description = "Great value; currently supports text and image-text generation",
        apiKeyUrl = "https://platform.deepseek.com/api_keys",
        pricing = "Very low price, about ¥0.4 per 1M tokens"
    ),
    MINIMAX(
        displayName = "MiniMax",
        baseUrl = "https://api.minimax.chat",
        modelsEndpoint = "/v1/models",
        description = "Supports high-quality voice cloning/synthesis. Strong text models and code-agent capability",
        apiKeyUrl = "https://platform.minimaxi.com/user-center/basic-information/interface-key",
        pricing = "Lower price, about $1 per 1M tokens"
    ),
    GLM(
        displayName = "Zhipu GLM",
        baseUrl = "https://open.bigmodel.cn/api/paas",
        modelsEndpoint = "/v4/models",
        description = "GLM-4.6 series with strong performance, coding ability, and multimodal support",
        apiKeyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        pricing = "Lower price, about $2 per 1M tokens"
    ),
    GROK(
        displayName = "xAI/Grok",
        baseUrl = "https://api.x.ai",
        modelsEndpoint = "/v1/models",
        description = "xAI Grok series with text and vision support",
        apiKeyUrl = "https://console.x.ai/",
        pricing = "Low price, Grok-4.1-fast ~ $0.5 per 1M tokens"
    ),
    VOLCANO(
        displayName = "Volcengine",
        baseUrl = "https://ark.cn-beijing.volces.com/api",
        modelsEndpoint = "/v3/models",
        description = "ByteDance Volcengine with a balanced Doubao model ecosystem. Recommended: doubao-1.6-pro-256k",
        apiKeyUrl = "https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey",
        pricing = "Free quota available, affordable pricing"
    ),
    SILICONFLOW(
        displayName = "SiliconFlow",
        baseUrl = "https://api.siliconflow.cn",
        modelsEndpoint = "/v1/models",
        description = "AI platform aggregating multiple open-source models",
        apiKeyUrl = "https://cloud.siliconflow.cn/account/ak",
        pricing = "Free quota available, affordable pricing"
    ),
    QWEN(
        displayName = "Qwen",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode",
        modelsEndpoint = "/v1/models",
        description = "Alibaba Cloud Qwen supports text, vision, audio, and more. Qwen3 has strong reasoning",
        apiKeyUrl = "https://dashscope.console.aliyun.com/apiKey",
        pricing = "Free quota available, affordable; about ¥0.5 per 1M tokens"
    ),
    CUSTOM(
        displayName = "Custom",
        baseUrl = "",
        modelsEndpoint = "/v1/models",
        description = "Custom service compatible with OpenAI API format. Requires a full Base URL",
        apiKeyUrl = "",
        pricing = "Depends on the provider"
    )
}

/**
 * AI 功能场景
 * 定义应用中使用 AI 的各个功能模块
 */
enum class AiFeature(
    val icon: String,
    val defaultCapabilities: List<ModelCapability> = emptyList()
) {
    HTML_CODING("Code", listOf(ModelCapability.TEXT, ModelCapability.CODE)),
    HTML_CODING_IMAGE("Image", listOf(ModelCapability.IMAGE_GENERATION)),
    ICON_GENERATION("AutoAwesome", listOf(ModelCapability.IMAGE_GENERATION)),
    MODULE_DEVELOPMENT("Extension", listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.FUNCTION_CALL)),
    LRC_GENERATION("MusicNote", listOf(ModelCapability.AUDIO, ModelCapability.TEXT)),
    TRANSLATION("Translate", listOf(ModelCapability.TEXT)),
    GENERAL("Chat", listOf(ModelCapability.TEXT));

    val displayName: String get() = when (this) {
        HTML_CODING -> Strings.featureHtmlCoding
        HTML_CODING_IMAGE -> Strings.featureHtmlCodingImage
        ICON_GENERATION -> Strings.featureIconGen
        MODULE_DEVELOPMENT -> Strings.featureModuleDev
        LRC_GENERATION -> Strings.featureLrcGen
        TRANSLATION -> Strings.featureTranslate
        GENERAL -> Strings.featureGeneral
    }

    val description: String get() = when (this) {
        HTML_CODING -> Strings.featureHtmlCodingDesc
        HTML_CODING_IMAGE -> Strings.featureHtmlCodingImageDesc
        ICON_GENERATION -> Strings.featureIconGenDesc
        MODULE_DEVELOPMENT -> Strings.featureModuleDevDesc
        LRC_GENERATION -> Strings.featureLrcGenDesc
        TRANSLATION -> Strings.featureTranslateDesc
        GENERAL -> Strings.featureGeneralDesc
    }
}

/**
 * 模型能力标签
 */
enum class ModelCapability {
    TEXT, AUDIO, IMAGE, IMAGE_GENERATION, VIDEO, CODE, FUNCTION_CALL, LONG_CONTEXT;

    val displayName: String get() = when (this) {
        TEXT -> Strings.capabilityText
        AUDIO -> Strings.capabilityAudio
        IMAGE -> Strings.capabilityImage
        IMAGE_GENERATION -> Strings.capabilityImageGen
        VIDEO -> Strings.capabilityVideo
        CODE -> Strings.capabilityCode
        FUNCTION_CALL -> Strings.capabilityFunctionCall
        LONG_CONTEXT -> Strings.capabilityLongContext
    }

    val description: String get() = when (this) {
        TEXT -> Strings.capabilityTextDesc
        AUDIO -> Strings.capabilityAudioDesc
        IMAGE -> Strings.capabilityImageDesc
        IMAGE_GENERATION -> Strings.capabilityImageGenDesc
        VIDEO -> Strings.capabilityVideoDesc
        CODE -> Strings.capabilityCodeDesc
        FUNCTION_CALL -> Strings.capabilityFunctionCallDesc
        LONG_CONTEXT -> Strings.capabilityLongContextDesc
    }
}

/**
 * 获取本地化的能力显示名称
 */
fun ModelCapability.getLocalizedDisplayName(): String {
    return when (this) {
        ModelCapability.TEXT -> com.webtoapp.core.i18n.Strings.textGeneration
        ModelCapability.AUDIO -> com.webtoapp.core.i18n.Strings.audioUnderstanding
        ModelCapability.IMAGE -> com.webtoapp.core.i18n.Strings.imageUnderstanding
        ModelCapability.IMAGE_GENERATION -> com.webtoapp.core.i18n.Strings.imageGeneration
        ModelCapability.VIDEO -> com.webtoapp.core.i18n.Strings.imageUnderstanding // 复用
        ModelCapability.CODE -> com.webtoapp.core.i18n.Strings.codeGeneration
        ModelCapability.FUNCTION_CALL -> com.webtoapp.core.i18n.Strings.functionCall
        ModelCapability.LONG_CONTEXT -> com.webtoapp.core.i18n.Strings.longContext
    }
}

/**
 * 获取本地化的能力描述
 */
fun ModelCapability.getLocalizedDescription(): String {
    return when (this) {
        ModelCapability.TEXT -> com.webtoapp.core.i18n.Strings.basicTextDialogue
        ModelCapability.AUDIO -> com.webtoapp.core.i18n.Strings.understandAndTranscribeAudio
        ModelCapability.IMAGE -> com.webtoapp.core.i18n.Strings.understandAndAnalyzeImages
        ModelCapability.IMAGE_GENERATION -> com.webtoapp.core.i18n.Strings.generateImages
        ModelCapability.VIDEO -> com.webtoapp.core.i18n.Strings.understandAndAnalyzeImages // 复用
        ModelCapability.CODE -> com.webtoapp.core.i18n.Strings.generateAndUnderstandCode
        ModelCapability.FUNCTION_CALL -> com.webtoapp.core.i18n.Strings.supportToolCall
        ModelCapability.LONG_CONTEXT -> com.webtoapp.core.i18n.Strings.supportLongTextInput
    }
}

/**
 * 能力标签到功能场景的映射配置
 */
data class CapabilityFeatureMapping(
    val capability: ModelCapability,
    val enabledFeatures: Set<AiFeature>
) {
    companion object {
        /**
         * 获取默认的能力-功能映射
         */
        fun getDefaultMappings(): List<CapabilityFeatureMapping> {
            return ModelCapability.entries.map { capability ->
                CapabilityFeatureMapping(
                    capability = capability,
                    enabledFeatures = AiFeature.entries.filter { feature ->
                        feature.defaultCapabilities.contains(capability)
                    }.toSet()
                )
            }
        }
    }
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
    val featureMappings: Map<ModelCapability, Set<AiFeature>> = emptyMap(), // 能力到功能的自定义映射
    val isDefault: Boolean = false,        // 是否为默认模型
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取此模型支持的功能场景
     * 根据能力标签和自定义映射计算
     */
    fun getSupportedFeatures(): Set<AiFeature> {
        val features = mutableSetOf<AiFeature>()
        capabilities.forEach { capability ->
            // 优先使用自定义映射，否则使用默认映射
            val mappedFeatures = featureMappings[capability]
                ?: AiFeature.entries.filter { it.defaultCapabilities.contains(capability) }.toSet()
            features.addAll(mappedFeatures)
        }
        return features
    }

    /**
     * 检查此模型是否支持指定功能
     */
    fun supportsFeature(feature: AiFeature): Boolean {
        return getSupportedFeatures().contains(feature)
    }

    /**
     * 获取指定能力对应的功能列表
     */
    fun getFeaturesForCapability(capability: ModelCapability): Set<AiFeature> {
        return featureMappings[capability]
            ?: AiFeature.entries.filter { it.defaultCapabilities.contains(capability) }.toSet()
    }
}

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
            name = Strings.lrcThemeDefault,
            textColor = "#FFFFFF",
            highlightColor = "#FFD700",
            backgroundColor = "#80000000",
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "karaoke",
            name = Strings.lrcThemeKaraoke,
            textColor = "#FFFFFF",
            highlightColor = "#FF4081",
            backgroundColor = "#00000000",
            strokeColor = "#000000",
            strokeWidth = 2f,
            animationType = LrcAnimationType.KARAOKE
        ),
        LrcTheme(
            id = "neon",
            name = Strings.lrcThemeNeon,
            textColor = "#00FFFF",
            highlightColor = "#FF00FF",
            backgroundColor = "#40000000",
            shadowEnabled = true,
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "minimal",
            name = Strings.lrcThemeMinimal,
            fontSize = 16f,
            textColor = "#CCCCCC",
            highlightColor = "#FFFFFF",
            backgroundColor = "#00000000",
            animationType = LrcAnimationType.SLIDE_UP
        ),
        LrcTheme(
            id = "classic",
            name = Strings.lrcThemeClassic,
            fontSize = 20f,
            textColor = "#FFE4B5",
            highlightColor = "#FFD700",
            backgroundColor = "#60000000",
            animationType = LrcAnimationType.TYPEWRITER
        ),
        LrcTheme(
            id = "dark",
            name = Strings.lrcThemeDark,
            textColor = "#AAAAAA",
            highlightColor = "#4FC3F7",
            backgroundColor = "#E0000000",
            animationType = LrcAnimationType.SCALE
        ),
        LrcTheme(
            id = "romantic",
            name = Strings.lrcThemeRomantic,
            textColor = "#FFB6C1",
            highlightColor = "#FF69B4",
            backgroundColor = "#40000000",
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "energetic",
            name = Strings.lrcThemeEnergetic,
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

/**
 * 获取本地化的AI供应商显示名称
 */
fun AiProvider.getLocalizedDisplayName(): String {
    return when (this) {
        AiProvider.OPENAI -> "OpenAI"
        AiProvider.OPENROUTER -> "OpenRouter"
        AiProvider.ANTHROPIC -> "Anthropic/Claude"
        AiProvider.GOOGLE -> "Google/Gemini"
        AiProvider.DEEPSEEK -> "DeepSeek"
        AiProvider.MINIMAX -> "MiniMax"
        AiProvider.GLM -> com.webtoapp.core.i18n.Strings.providerGlm
        AiProvider.GROK -> "xAI/Grok"
        AiProvider.VOLCANO -> com.webtoapp.core.i18n.Strings.providerVolcano
        AiProvider.SILICONFLOW -> com.webtoapp.core.i18n.Strings.providerSiliconflow
        AiProvider.QWEN -> com.webtoapp.core.i18n.Strings.providerQwen
        AiProvider.CUSTOM -> com.webtoapp.core.i18n.Strings.providerCustom
    }
}

/**
 * 获取本地化的AI功能场景显示名称
 */
fun AiFeature.getLocalizedDisplayName(): String {
    return when (this) {
        AiFeature.HTML_CODING -> "HTML ${com.webtoapp.core.i18n.Strings.coding}"
        AiFeature.HTML_CODING_IMAGE -> "HTML ${com.webtoapp.core.i18n.Strings.coding} (${com.webtoapp.core.i18n.Strings.image})"
        AiFeature.ICON_GENERATION -> com.webtoapp.core.i18n.Strings.featureIconGeneration
        AiFeature.MODULE_DEVELOPMENT -> com.webtoapp.core.i18n.Strings.featureModuleDevelopment
        AiFeature.LRC_GENERATION -> com.webtoapp.core.i18n.Strings.featureLrcGeneration
        AiFeature.TRANSLATION -> com.webtoapp.core.i18n.Strings.featureTranslation
        AiFeature.GENERAL -> com.webtoapp.core.i18n.Strings.featureGeneralChat
    }
}

/**
 * 获取本地化的AI功能场景描述
 */
fun AiFeature.getLocalizedDescription(): String {
    return when (this) {
        AiFeature.HTML_CODING -> Strings.htmlCodingDesc
        AiFeature.HTML_CODING_IMAGE -> Strings.htmlCodingImageDesc
        AiFeature.ICON_GENERATION -> Strings.iconGenerationDesc
        AiFeature.MODULE_DEVELOPMENT -> Strings.moduleDevelopmentDesc
        AiFeature.LRC_GENERATION -> Strings.lrcGenerationDesc
        AiFeature.TRANSLATION -> Strings.translationDesc
        AiFeature.GENERAL -> Strings.generalChatDesc
    }
}

/**
 * 获取本地化的AI供应商描述
 */
fun AiProvider.getLocalizedDescription(): String {
    return when (this) {
        AiProvider.OPENAI -> Strings.providerOpenaiDesc
        AiProvider.OPENROUTER -> Strings.providerOpenrouterDesc
        AiProvider.ANTHROPIC -> Strings.providerAnthropicDesc
        AiProvider.GOOGLE -> Strings.providerGoogleDesc
        AiProvider.DEEPSEEK -> Strings.providerDeepseekDesc
        AiProvider.MINIMAX -> Strings.providerMinimaxDesc
        AiProvider.GLM -> Strings.providerGlmDesc
        AiProvider.GROK -> Strings.providerGrokDesc
        AiProvider.VOLCANO -> Strings.providerVolcanoDesc
        AiProvider.SILICONFLOW -> Strings.providerSiliconflowDesc
        AiProvider.QWEN -> Strings.providerQwenDesc
        AiProvider.CUSTOM -> Strings.providerCustomDesc
    }
}

/**
 * 获取本地化的AI供应商定价信息
 */
fun AiProvider.getLocalizedPricing(): String {
    return when (this) {
        AiProvider.OPENAI -> Strings.providerOpenaiPricing
        AiProvider.OPENROUTER -> Strings.providerOpenrouterPricing
        AiProvider.ANTHROPIC -> Strings.providerAnthropicPricing
        AiProvider.GOOGLE -> Strings.providerGooglePricing
        AiProvider.DEEPSEEK -> Strings.providerDeepseekPricing
        AiProvider.MINIMAX -> Strings.providerMinimaxPricing
        AiProvider.GLM -> Strings.providerGlmPricing
        AiProvider.GROK -> Strings.providerGrokPricing
        AiProvider.VOLCANO -> Strings.providerVolcanoPricing
        AiProvider.SILICONFLOW -> Strings.providerSiliconflowPricing
        AiProvider.QWEN -> Strings.providerQwenPricing
        AiProvider.CUSTOM -> Strings.providerCustomPricing
    }
}
