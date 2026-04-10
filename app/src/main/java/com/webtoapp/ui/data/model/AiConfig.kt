package com.webtoapp.data.model

import com.webtoapp.core.i18n.Strings

/**
 * AI 供应商分类
 */
enum class ProviderCategory {
    RECOMMENDED,   // 推荐
    INTERNATIONAL, // 国际供应商
    CHINESE,       // 国内供应商
    AGGREGATOR,    // 聚合平台
    SELF_HOSTED,   // 本地/自托管
    CUSTOM;        // 自定义
    
    val displayName: String get() = when (this) {
        RECOMMENDED -> Strings.providerCategoryRecommended
        INTERNATIONAL -> Strings.providerCategoryInternational
        CHINESE -> Strings.providerCategoryChinese
        AGGREGATOR -> Strings.providerCategoryAggregator
        SELF_HOSTED -> Strings.providerCategorySelfHosted
        CUSTOM -> Strings.providerCategoryCustom
    }
}

/**
 * AI 供应商
 * 参考 LiteLLM (github.com/BerriAI/litellm) 的供应商列表
 */
enum class AiProvider(
    val baseUrl: String,
    val modelsEndpoint: String = "/v1/models",
    val apiKeyUrl: String = "",
    val category: ProviderCategory = ProviderCategory.INTERNATIONAL
) {
    // ==================== 推荐 ====================
    GOOGLE(
        baseUrl = "https://generativelanguage.googleapis.com",
        modelsEndpoint = "/v1beta/models",
        apiKeyUrl = "https://aistudio.google.com/apikey",
        category = ProviderCategory.RECOMMENDED
    ),
    OPENROUTER(
        baseUrl = "https://openrouter.ai/api",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://openrouter.ai/keys",
        category = ProviderCategory.RECOMMENDED
    ),
    
    // ==================== 国际供应商 ====================
    OPENAI(
        baseUrl = "https://api.openai.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://platform.openai.com/api-keys",
        category = ProviderCategory.INTERNATIONAL
    ),
    ANTHROPIC(
        baseUrl = "https://api.anthropic.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://console.anthropic.com/settings/keys",
        category = ProviderCategory.INTERNATIONAL
    ),
    GROK(
        baseUrl = "https://api.x.ai",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://console.x.ai/",
        category = ProviderCategory.INTERNATIONAL
    ),
    MISTRAL(
        baseUrl = "https://api.mistral.ai",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://console.mistral.ai/api-keys/",
        category = ProviderCategory.INTERNATIONAL
    ),
    COHERE(
        baseUrl = "https://api.cohere.com",
        modelsEndpoint = "/v2/models",
        apiKeyUrl = "https://dashboard.cohere.com/api-keys",
        category = ProviderCategory.INTERNATIONAL
    ),
    AI21(
        baseUrl = "https://api.ai21.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://studio.ai21.com/account/api-key",
        category = ProviderCategory.INTERNATIONAL
    ),
    
    // ==================== 快速推理 ====================
    GROQ(
        baseUrl = "https://api.groq.com/openai",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://console.groq.com/keys",
        category = ProviderCategory.INTERNATIONAL
    ),
    CEREBRAS(
        baseUrl = "https://api.cerebras.ai",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://cloud.cerebras.ai/",
        category = ProviderCategory.INTERNATIONAL
    ),
    SAMBANOVA(
        baseUrl = "https://api.sambanova.ai",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://cloud.sambanova.ai/apis",
        category = ProviderCategory.INTERNATIONAL
    ),
    
    // ==================== 聚合平台 ====================
    TOGETHER(
        baseUrl = "https://api.together.xyz",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://api.together.xyz/settings/api-keys",
        category = ProviderCategory.AGGREGATOR
    ),
    PERPLEXITY(
        baseUrl = "https://api.perplexity.ai",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://www.perplexity.ai/settings/api",
        category = ProviderCategory.AGGREGATOR
    ),
    FIREWORKS(
        baseUrl = "https://api.fireworks.ai/inference",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://fireworks.ai/api-keys",
        category = ProviderCategory.AGGREGATOR
    ),
    DEEPINFRA(
        baseUrl = "https://api.deepinfra.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://deepinfra.com/dash/api_keys",
        category = ProviderCategory.AGGREGATOR
    ),
    NOVITA(
        baseUrl = "https://api.novita.ai",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://novita.ai/settings#key-management",
        category = ProviderCategory.AGGREGATOR
    ),
    
    // ==================== 国内供应商 ====================
    DEEPSEEK(
        baseUrl = "https://api.deepseek.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://platform.deepseek.com/api_keys",
        category = ProviderCategory.CHINESE
    ),
    QWEN(
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://dashscope.console.aliyun.com/apiKey",
        category = ProviderCategory.CHINESE
    ),
    GLM(
        baseUrl = "https://open.bigmodel.cn/api/paas",
        modelsEndpoint = "/v4/models",
        apiKeyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        category = ProviderCategory.CHINESE
    ),
    VOLCANO(
        baseUrl = "https://ark.cn-beijing.volces.com/api",
        modelsEndpoint = "/v3/models",
        apiKeyUrl = "https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey",
        category = ProviderCategory.CHINESE
    ),
    MOONSHOT(
        baseUrl = "https://api.moonshot.cn",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://platform.moonshot.cn/console/api-keys",
        category = ProviderCategory.CHINESE
    ),
    MINIMAX(
        baseUrl = "https://api.minimax.chat",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://platform.minimaxi.com/user-center/basic-information/interface-key",
        category = ProviderCategory.CHINESE
    ),
    SILICONFLOW(
        baseUrl = "https://api.siliconflow.cn",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://cloud.siliconflow.cn/account/ak",
        category = ProviderCategory.CHINESE
    ),
    BAICHUAN(
        baseUrl = "https://api.baichuan-ai.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://platform.baichuan-ai.com/console/apikey",
        category = ProviderCategory.CHINESE
    ),
    YI(
        baseUrl = "https://api.lingyiwanwu.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://platform.lingyiwanwu.com/apikeys",
        category = ProviderCategory.CHINESE
    ),
    STEPFUN(
        baseUrl = "https://api.stepfun.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://platform.stepfun.com/interface-key",
        category = ProviderCategory.CHINESE
    ),
    HUNYUAN(
        baseUrl = "https://api.hunyuan.cloud.tencent.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://console.cloud.tencent.com/hunyuan/start",
        category = ProviderCategory.CHINESE
    ),
    SPARK(
        baseUrl = "https://spark-api-open.xf-yun.com",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://console.xfyun.cn/services/bm35",
        category = ProviderCategory.CHINESE
    ),
    
    // ==================== 本地/自托管 ====================
    OLLAMA(
        baseUrl = "http://localhost:11434",
        modelsEndpoint = "/api/tags",
        apiKeyUrl = "https://ollama.com/",
        category = ProviderCategory.SELF_HOSTED
    ),
    LM_STUDIO(
        baseUrl = "http://localhost:1234",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://lmstudio.ai/",
        category = ProviderCategory.SELF_HOSTED
    ),
    VLLM(
        baseUrl = "http://localhost:8000",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "https://docs.vllm.ai/",
        category = ProviderCategory.SELF_HOSTED
    ),
    
    // ==================== 自定义 ====================
    CUSTOM(
        baseUrl = "",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "",
        category = ProviderCategory.CUSTOM
    );
    
    val displayName: String get() = when (this) {
        GOOGLE -> Strings.providerGoogle
        OPENROUTER -> Strings.providerOpenRouter
        OPENAI -> Strings.providerOpenAI
        ANTHROPIC -> Strings.providerAnthropic
        GROK -> Strings.providerGrok
        MISTRAL -> Strings.providerMistral
        COHERE -> Strings.providerCohere
        AI21 -> Strings.providerAI21
        GROQ -> Strings.providerGroq
        CEREBRAS -> Strings.providerCerebras
        SAMBANOVA -> Strings.providerSambanova
        TOGETHER -> Strings.providerTogether
        PERPLEXITY -> Strings.providerPerplexity
        FIREWORKS -> Strings.providerFireworks
        DEEPINFRA -> Strings.providerDeepInfra
        NOVITA -> Strings.providerNovita
        DEEPSEEK -> Strings.providerDeepSeek
        QWEN -> Strings.providerQwen
        GLM -> Strings.providerGLM
        VOLCANO -> Strings.providerVolcano
        MOONSHOT -> Strings.providerMoonshot
        MINIMAX -> Strings.providerMiniMax
        SILICONFLOW -> Strings.providerSiliconFlow
        BAICHUAN -> Strings.providerBaichuan
        YI -> Strings.providerYi
        STEPFUN -> Strings.providerStepfun
        HUNYUAN -> Strings.providerHunyuan
        SPARK -> Strings.providerSpark
        OLLAMA -> Strings.providerOllama
        LM_STUDIO -> Strings.providerLmStudio
        VLLM -> Strings.providerVllm
        CUSTOM -> Strings.providerCustom
    }
    
    val description: String get() = when (this) {
        GOOGLE -> Strings.providerGoogleDesc
        OPENROUTER -> Strings.providerOpenRouterDesc
        OPENAI -> Strings.providerOpenAIDesc
        ANTHROPIC -> Strings.providerAnthropicDesc
        GROK -> Strings.providerGrokDesc
        MISTRAL -> Strings.providerMistralDesc
        COHERE -> Strings.providerCohereDesc
        AI21 -> Strings.providerAI21Desc
        GROQ -> Strings.providerGroqDesc
        CEREBRAS -> Strings.providerCerebrasDesc
        SAMBANOVA -> Strings.providerSambanovaDesc
        TOGETHER -> Strings.providerTogetherDesc
        PERPLEXITY -> Strings.providerPerplexityDesc
        FIREWORKS -> Strings.providerFireworksDesc
        DEEPINFRA -> Strings.providerDeepInfraDesc
        NOVITA -> Strings.providerNovitaDesc
        DEEPSEEK -> Strings.providerDeepSeekDesc
        QWEN -> Strings.providerQwenDesc
        GLM -> Strings.providerGLMDesc
        VOLCANO -> Strings.providerVolcanoDesc
        MOONSHOT -> Strings.providerMoonshotDesc
        MINIMAX -> Strings.providerMiniMaxDesc
        SILICONFLOW -> Strings.providerSiliconFlowDesc
        BAICHUAN -> Strings.providerBaichuanDesc
        YI -> Strings.providerYiDesc
        STEPFUN -> Strings.providerStepfunDesc
        HUNYUAN -> Strings.providerHunyuanDesc
        SPARK -> Strings.providerSparkDesc
        OLLAMA -> Strings.providerOllamaDesc
        LM_STUDIO -> Strings.providerLmStudioDesc
        VLLM -> Strings.providerVllmDesc
        CUSTOM -> Strings.providerCustomDesc
    }
    
    val pricing: String get() = when (this) {
        GOOGLE -> Strings.providerGooglePricing
        OPENROUTER -> Strings.providerOpenRouterPricing
        OPENAI -> Strings.providerOpenAIPricing
        ANTHROPIC -> Strings.providerAnthropicPricing
        GROK -> Strings.providerGrokPricing
        MISTRAL -> Strings.providerMistralPricing
        COHERE -> Strings.providerCoherePricing
        AI21 -> Strings.providerAI21Pricing
        GROQ -> Strings.providerGroqPricing
        CEREBRAS -> Strings.providerCerebrasPricing
        SAMBANOVA -> Strings.providerSambanovaPricing
        TOGETHER -> Strings.providerTogetherPricing
        PERPLEXITY -> Strings.providerPerplexityPricing
        FIREWORKS -> Strings.providerFireworksPricing
        DEEPINFRA -> Strings.providerDeepInfraPricing
        NOVITA -> Strings.providerNovitaPricing
        DEEPSEEK -> Strings.providerDeepSeekPricing
        QWEN -> Strings.providerQwenPricing
        GLM -> Strings.providerGLMPricing
        VOLCANO -> Strings.providerVolcanoPricing
        MOONSHOT -> Strings.providerMoonshotPricing
        MINIMAX -> Strings.providerMiniMaxPricing
        SILICONFLOW -> Strings.providerSiliconFlowPricing
        BAICHUAN -> Strings.providerBaichuanPricing
        YI -> Strings.providerYiPricing
        STEPFUN -> Strings.providerStepfunPricing
        HUNYUAN -> Strings.providerHunyuanPricing
        SPARK -> Strings.providerSparkPricing
        OLLAMA -> Strings.providerOllamaPricing
        LM_STUDIO -> Strings.providerLmStudioPricing
        VLLM -> Strings.providerVllmPricing
        CUSTOM -> Strings.providerCustomPricing
    }
    
    /**
     * 是否需要 API Key（本地模型不需要）
     */
    val requiresApiKey: Boolean get() = when (this) {
        OLLAMA, LM_STUDIO, VLLM -> false
        else -> true
    }
    
    /**
     * 是否允许自定义 Base URL
     */
    val allowCustomBaseUrl: Boolean get() = when (this) {
        CUSTOM, OLLAMA, LM_STUDIO, VLLM -> true
        else -> false
    }
}

/**
 * AI 功能场景
 * 定义应用中使用 AI 的各个功能模块
 */
enum class AiFeature(
    val icon: String,
    val defaultCapabilities: List<ModelCapability> = emptyList()
) {
    AI_CODING("Code", listOf(ModelCapability.TEXT, ModelCapability.CODE)),
    AI_CODING_IMAGE("Image", listOf(ModelCapability.IMAGE_GENERATION)),
    ICON_GENERATION("AutoAwesome", listOf(ModelCapability.IMAGE_GENERATION)),
    MODULE_DEVELOPMENT("Extension", listOf(ModelCapability.TEXT, ModelCapability.CODE, ModelCapability.FUNCTION_CALL)),
    LRC_GENERATION("MusicNote", listOf(ModelCapability.AUDIO, ModelCapability.TEXT)),
    TRANSLATION("Translate", listOf(ModelCapability.TEXT)),
    GENERAL("Chat", listOf(ModelCapability.TEXT));
    
    val displayName: String get() = when (this) {
        AI_CODING -> Strings.featureAiCoding
        AI_CODING_IMAGE -> Strings.featureAiCodingImage
        ICON_GENERATION -> Strings.featureIconGen
        MODULE_DEVELOPMENT -> Strings.featureModuleDev
        LRC_GENERATION -> Strings.featureLrcGen
        TRANSLATION -> Strings.featureTranslate
        GENERAL -> Strings.featureGeneral
    }
    
    val description: String get() = when (this) {
        AI_CODING -> Strings.featureAiCodingDesc
        AI_CODING_IMAGE -> Strings.featureAiCodingImageDesc
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
    val name: String,                      // Show名称
    val provider: AiProvider,              // 供应商
    val capabilities: List<ModelCapability> = listOf(ModelCapability.TEXT), // 能力标签
    val contextLength: Int = 4096,         // 上下文长度（token）
    val inputPrice: Double = 0.0,          // 输入价格（$/百万 token）
    val outputPrice: Double = 0.0,         // 输出价格（$/百万 token）
    val isCustom: Boolean = false          // Yes否为手动添加的模型
)

/**
 * API 格式类型（用于自定义端点）
 */
enum class ApiFormat {
    OPENAI_COMPATIBLE,  // OpenAI 兼容格式（默认）
    ANTHROPIC,          // Anthropic Claude 格式
    GOOGLE_GEMINI;      // Google Gemini 格式
    
    val displayName: String get() = when (this) {
        OPENAI_COMPATIBLE -> "OpenAI 兼容"
        ANTHROPIC -> "Anthropic Claude"
        GOOGLE_GEMINI -> "Google Gemini"
    }
}

/**
 * API 密钥配置
 */
data class ApiKeyConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val provider: AiProvider,
    val apiKey: String,
    val baseUrl: String? = null,                    // Custom base URL（可选）
    val customModelsEndpoint: String? = null,       // 自定义模型列表端点（如 /v1/models）
    val customChatEndpoint: String? = null,         // 自定义聊天端点（如 /v1/chat/completions）
    val apiFormat: ApiFormat = ApiFormat.OPENAI_COMPATIBLE, // API 格式类型
    val alias: String? = null,                      // 用户自定义别名（用于识别）
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * 获取显示名称（别名或供应商名称）
     */
    val displayName: String get() = alias?.takeIf { it.isNotBlank() } ?: provider.displayName
    
    /**
     * 获取有效的 models 端点
     */
    fun getEffectiveModelsEndpoint(): String {
        return customModelsEndpoint?.takeIf { it.isNotBlank() } ?: provider.modelsEndpoint
    }
    
    /**
     * 获取有效的 chat 端点
     */
    fun getEffectiveChatEndpoint(): String {
        return customChatEndpoint?.takeIf { it.isNotBlank() } ?: when (provider) {
            AiProvider.GLM -> "/v4/chat/completions"
            AiProvider.VOLCANO -> "/v3/chat/completions"
            AiProvider.ANTHROPIC -> "/v1/messages"
            AiProvider.GOOGLE -> "/v1beta/models"
            AiProvider.COHERE -> "/v2/chat"
            AiProvider.OLLAMA -> "/api/chat"
            else -> "/v1/chat/completions"
        }
    }
}

/**
 * 已保存的模型配置（用户选择并保存的模型）
 */
data class SavedModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val model: AiModel,
    val apiKeyId: String,                  // 关联的 API Key ID
    val alias: String? = null,             // User自定义别名
    val capabilities: List<ModelCapability>, // User指定的能力标签
    val featureMappings: Map<ModelCapability, Set<AiFeature>> = emptyMap(), // 能力到功能的自定义映射
    val isDefault: Boolean = false,        // Yes否为默认模型
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
    val defaultModelId: String? = null     // Default模型 ID
)

/**
 * LRC 生成任务状态
 */
enum class LrcTaskStatus {
    PENDING,     // 等待中
    PROCESSING,  // Handle中
    COMPLETED,   // Completed
    FAILED       // Failed
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
    val resultLrc: LrcData? = null,  // Generate的歌词
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
fun AiProvider.getLocalizedDisplayName(): String = displayName

/**
 * 获取本地化的AI功能场景显示名称
 */
fun AiFeature.getLocalizedDisplayName(): String {
    return when (this) {
        AiFeature.AI_CODING -> "HTML ${com.webtoapp.core.i18n.Strings.coding}"
        AiFeature.AI_CODING_IMAGE -> "HTML ${com.webtoapp.core.i18n.Strings.coding} (${com.webtoapp.core.i18n.Strings.image})"
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
        AiFeature.AI_CODING -> Strings.aiCodingDesc
        AiFeature.AI_CODING_IMAGE -> Strings.aiCodingImageDesc
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
fun AiProvider.getLocalizedDescription(): String = description

/**
 * 获取本地化的AI供应商定价信息
 */
fun AiProvider.getLocalizedPricing(): String = pricing
