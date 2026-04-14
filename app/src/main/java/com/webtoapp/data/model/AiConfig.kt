package com.webtoapp.data.model

import com.webtoapp.core.i18n.AppStringsProvider

/**
 * AI provider categories.
 */
enum class ProviderCategory {
    RECOMMENDED,   // Recommended
    INTERNATIONAL, // International providers
    CHINESE,       // Chinese providers
    AGGREGATOR,    // Aggregators
    SELF_HOSTED,   // Self-hosted
    CUSTOM;        // Custom
    
    val displayName: String get() = when (this) {
        RECOMMENDED -> AppStringsProvider.current().providerCategoryRecommended
        INTERNATIONAL -> AppStringsProvider.current().providerCategoryInternational
        CHINESE -> AppStringsProvider.current().providerCategoryChinese
        AGGREGATOR -> AppStringsProvider.current().providerCategoryAggregator
        SELF_HOSTED -> AppStringsProvider.current().providerCategorySelfHosted
        CUSTOM -> AppStringsProvider.current().providerCategoryCustom
    }
}

/**
 * AI providers.
 * Mirrors the LiteLLM (github.com/BerriAI/litellm) list.
 */
enum class AiProvider(
    val baseUrl: String,
    val modelsEndpoint: String = "/v1/models",
    val apiKeyUrl: String = "",
    val category: ProviderCategory = ProviderCategory.INTERNATIONAL
) {
    // ==================== Recommended ====================
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
    
    // ==================== International ====================
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
    
    // ==================== Fast inference ====================
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
    
    // ==================== Aggregators ====================
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
    
    // ==================== Chinese providers ====================
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
    
    // ==================== Self-hosted ====================
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
    
    // ==================== Custom ====================
    CUSTOM(
        baseUrl = "",
        modelsEndpoint = "/v1/models",
        apiKeyUrl = "",
        category = ProviderCategory.CUSTOM
    );
    
    val displayName: String get() = when (this) {
        GOOGLE -> AppStringsProvider.current().providerGoogle
        OPENROUTER -> AppStringsProvider.current().providerOpenRouter
        OPENAI -> AppStringsProvider.current().providerOpenAI
        ANTHROPIC -> AppStringsProvider.current().providerAnthropic
        GROK -> AppStringsProvider.current().providerGrok
        MISTRAL -> AppStringsProvider.current().providerMistral
        COHERE -> AppStringsProvider.current().providerCohere
        AI21 -> AppStringsProvider.current().providerAI21
        GROQ -> AppStringsProvider.current().providerGroq
        CEREBRAS -> AppStringsProvider.current().providerCerebras
        SAMBANOVA -> AppStringsProvider.current().providerSambanova
        TOGETHER -> AppStringsProvider.current().providerTogether
        PERPLEXITY -> AppStringsProvider.current().providerPerplexity
        FIREWORKS -> AppStringsProvider.current().providerFireworks
        DEEPINFRA -> AppStringsProvider.current().providerDeepInfra
        NOVITA -> AppStringsProvider.current().providerNovita
        DEEPSEEK -> AppStringsProvider.current().providerDeepSeek
        QWEN -> AppStringsProvider.current().providerQwen
        GLM -> AppStringsProvider.current().providerGLM
        VOLCANO -> AppStringsProvider.current().providerVolcano
        MOONSHOT -> AppStringsProvider.current().providerMoonshot
        MINIMAX -> AppStringsProvider.current().providerMiniMax
        SILICONFLOW -> AppStringsProvider.current().providerSiliconFlow
        BAICHUAN -> AppStringsProvider.current().providerBaichuan
        YI -> AppStringsProvider.current().providerYi
        STEPFUN -> AppStringsProvider.current().providerStepfun
        HUNYUAN -> AppStringsProvider.current().providerHunyuan
        SPARK -> AppStringsProvider.current().providerSpark
        OLLAMA -> AppStringsProvider.current().providerOllama
        LM_STUDIO -> AppStringsProvider.current().providerLmStudio
        VLLM -> AppStringsProvider.current().providerVllm
        CUSTOM -> AppStringsProvider.current().providerCustom
    }
    
    val description: String get() = when (this) {
        GOOGLE -> AppStringsProvider.current().providerGoogleDesc
        OPENROUTER -> AppStringsProvider.current().providerOpenRouterDesc
        OPENAI -> AppStringsProvider.current().providerOpenAIDesc
        ANTHROPIC -> AppStringsProvider.current().providerAnthropicDesc
        GROK -> AppStringsProvider.current().providerGrokDesc
        MISTRAL -> AppStringsProvider.current().providerMistralDesc
        COHERE -> AppStringsProvider.current().providerCohereDesc
        AI21 -> AppStringsProvider.current().providerAI21Desc
        GROQ -> AppStringsProvider.current().providerGroqDesc
        CEREBRAS -> AppStringsProvider.current().providerCerebrasDesc
        SAMBANOVA -> AppStringsProvider.current().providerSambanovaDesc
        TOGETHER -> AppStringsProvider.current().providerTogetherDesc
        PERPLEXITY -> AppStringsProvider.current().providerPerplexityDesc
        FIREWORKS -> AppStringsProvider.current().providerFireworksDesc
        DEEPINFRA -> AppStringsProvider.current().providerDeepInfraDesc
        NOVITA -> AppStringsProvider.current().providerNovitaDesc
        DEEPSEEK -> AppStringsProvider.current().providerDeepSeekDesc
        QWEN -> AppStringsProvider.current().providerQwenDesc
        GLM -> AppStringsProvider.current().providerGLMDesc
        VOLCANO -> AppStringsProvider.current().providerVolcanoDesc
        MOONSHOT -> AppStringsProvider.current().providerMoonshotDesc
        MINIMAX -> AppStringsProvider.current().providerMiniMaxDesc
        SILICONFLOW -> AppStringsProvider.current().providerSiliconFlowDesc
        BAICHUAN -> AppStringsProvider.current().providerBaichuanDesc
        YI -> AppStringsProvider.current().providerYiDesc
        STEPFUN -> AppStringsProvider.current().providerStepfunDesc
        HUNYUAN -> AppStringsProvider.current().providerHunyuanDesc
        SPARK -> AppStringsProvider.current().providerSparkDesc
        OLLAMA -> AppStringsProvider.current().providerOllamaDesc
        LM_STUDIO -> AppStringsProvider.current().providerLmStudioDesc
        VLLM -> AppStringsProvider.current().providerVllmDesc
        CUSTOM -> AppStringsProvider.current().providerCustomDesc
    }
    
    val pricing: String get() = when (this) {
        GOOGLE -> AppStringsProvider.current().providerGooglePricing
        OPENROUTER -> AppStringsProvider.current().providerOpenRouterPricing
        OPENAI -> AppStringsProvider.current().providerOpenAIPricing
        ANTHROPIC -> AppStringsProvider.current().providerAnthropicPricing
        GROK -> AppStringsProvider.current().providerGrokPricing
        MISTRAL -> AppStringsProvider.current().providerMistralPricing
        COHERE -> AppStringsProvider.current().providerCoherePricing
        AI21 -> AppStringsProvider.current().providerAI21Pricing
        GROQ -> AppStringsProvider.current().providerGroqPricing
        CEREBRAS -> AppStringsProvider.current().providerCerebrasPricing
        SAMBANOVA -> AppStringsProvider.current().providerSambanovaPricing
        TOGETHER -> AppStringsProvider.current().providerTogetherPricing
        PERPLEXITY -> AppStringsProvider.current().providerPerplexityPricing
        FIREWORKS -> AppStringsProvider.current().providerFireworksPricing
        DEEPINFRA -> AppStringsProvider.current().providerDeepInfraPricing
        NOVITA -> AppStringsProvider.current().providerNovitaPricing
        DEEPSEEK -> AppStringsProvider.current().providerDeepSeekPricing
        QWEN -> AppStringsProvider.current().providerQwenPricing
        GLM -> AppStringsProvider.current().providerGLMPricing
        VOLCANO -> AppStringsProvider.current().providerVolcanoPricing
        MOONSHOT -> AppStringsProvider.current().providerMoonshotPricing
        MINIMAX -> AppStringsProvider.current().providerMiniMaxPricing
        SILICONFLOW -> AppStringsProvider.current().providerSiliconFlowPricing
        BAICHUAN -> AppStringsProvider.current().providerBaichuanPricing
        YI -> AppStringsProvider.current().providerYiPricing
        STEPFUN -> AppStringsProvider.current().providerStepfunPricing
        HUNYUAN -> AppStringsProvider.current().providerHunyuanPricing
        SPARK -> AppStringsProvider.current().providerSparkPricing
        OLLAMA -> AppStringsProvider.current().providerOllamaPricing
        LM_STUDIO -> AppStringsProvider.current().providerLmStudioPricing
        VLLM -> AppStringsProvider.current().providerVllmPricing
        CUSTOM -> AppStringsProvider.current().providerCustomPricing
    }
    
    /**
     * Whether an API key is required (local models skip this).
     */
    val requiresApiKey: Boolean get() = when (this) {
        OLLAMA, LM_STUDIO, VLLM -> false
        else -> true
    }
    
    /**
     * Whether a custom base URL is allowed.
     */
    val allowCustomBaseUrl: Boolean get() = when (this) {
        CUSTOM, OLLAMA, LM_STUDIO, VLLM -> true
        else -> false
    }
}

/**
 * AI feature scenarios.
 * Defines the app modules that use AI.
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
        AI_CODING -> AppStringsProvider.current().featureAiCoding
        AI_CODING_IMAGE -> AppStringsProvider.current().featureAiCodingImage
        ICON_GENERATION -> AppStringsProvider.current().featureIconGen
        MODULE_DEVELOPMENT -> AppStringsProvider.current().featureModuleDev
        LRC_GENERATION -> AppStringsProvider.current().featureLrcGen
        TRANSLATION -> AppStringsProvider.current().featureTranslate
        GENERAL -> AppStringsProvider.current().featureGeneral
    }
    
    val description: String get() = when (this) {
        AI_CODING -> AppStringsProvider.current().featureAiCodingDesc
        AI_CODING_IMAGE -> AppStringsProvider.current().featureAiCodingImageDesc
        ICON_GENERATION -> AppStringsProvider.current().featureIconGenDesc
        MODULE_DEVELOPMENT -> AppStringsProvider.current().featureModuleDevDesc
        LRC_GENERATION -> AppStringsProvider.current().featureLrcGenDesc
        TRANSLATION -> AppStringsProvider.current().featureTranslateDesc
        GENERAL -> AppStringsProvider.current().featureGeneralDesc
    }
}

/**
 * Model capability tags.
 */
enum class ModelCapability {
    TEXT, AUDIO, IMAGE, IMAGE_GENERATION, VIDEO, CODE, FUNCTION_CALL, LONG_CONTEXT;
    
    val displayName: String get() = when (this) {
        TEXT -> AppStringsProvider.current().capabilityText
        AUDIO -> AppStringsProvider.current().capabilityAudio
        IMAGE -> AppStringsProvider.current().capabilityImage
        IMAGE_GENERATION -> AppStringsProvider.current().capabilityImageGen
        VIDEO -> AppStringsProvider.current().capabilityVideo
        CODE -> AppStringsProvider.current().capabilityCode
        FUNCTION_CALL -> AppStringsProvider.current().capabilityFunctionCall
        LONG_CONTEXT -> AppStringsProvider.current().capabilityLongContext
    }
    
    val description: String get() = when (this) {
        TEXT -> AppStringsProvider.current().capabilityTextDesc
        AUDIO -> AppStringsProvider.current().capabilityAudioDesc
        IMAGE -> AppStringsProvider.current().capabilityImageDesc
        IMAGE_GENERATION -> AppStringsProvider.current().capabilityImageGenDesc
        VIDEO -> AppStringsProvider.current().capabilityVideoDesc
        CODE -> AppStringsProvider.current().capabilityCodeDesc
        FUNCTION_CALL -> AppStringsProvider.current().capabilityFunctionCallDesc
        LONG_CONTEXT -> AppStringsProvider.current().capabilityLongContextDesc
    }
}

/**
 * Gets the localized capability display name.
 */
fun ModelCapability.getLocalizedDisplayName(): String {
    return when (this) {
        ModelCapability.TEXT -> com.webtoapp.core.i18n.AppStringsProvider.current().textGeneration
        ModelCapability.AUDIO -> com.webtoapp.core.i18n.AppStringsProvider.current().audioUnderstanding
        ModelCapability.IMAGE -> com.webtoapp.core.i18n.AppStringsProvider.current().imageUnderstanding
        ModelCapability.IMAGE_GENERATION -> com.webtoapp.core.i18n.AppStringsProvider.current().imageGeneration
        ModelCapability.VIDEO -> com.webtoapp.core.i18n.AppStringsProvider.current().imageUnderstanding // Reuse
        ModelCapability.CODE -> com.webtoapp.core.i18n.AppStringsProvider.current().codeGeneration
        ModelCapability.FUNCTION_CALL -> com.webtoapp.core.i18n.AppStringsProvider.current().functionCall
        ModelCapability.LONG_CONTEXT -> com.webtoapp.core.i18n.AppStringsProvider.current().longContext
    }
}

/**
 * Gets the localized capability description.
 */
fun ModelCapability.getLocalizedDescription(): String {
    return when (this) {
        ModelCapability.TEXT -> com.webtoapp.core.i18n.AppStringsProvider.current().basicTextDialogue
        ModelCapability.AUDIO -> com.webtoapp.core.i18n.AppStringsProvider.current().understandAndTranscribeAudio
        ModelCapability.IMAGE -> com.webtoapp.core.i18n.AppStringsProvider.current().understandAndAnalyzeImages
        ModelCapability.IMAGE_GENERATION -> com.webtoapp.core.i18n.AppStringsProvider.current().generateImages
        ModelCapability.VIDEO -> com.webtoapp.core.i18n.AppStringsProvider.current().understandAndAnalyzeImages // Reuse
        ModelCapability.CODE -> com.webtoapp.core.i18n.AppStringsProvider.current().generateAndUnderstandCode
        ModelCapability.FUNCTION_CALL -> com.webtoapp.core.i18n.AppStringsProvider.current().supportToolCall
        ModelCapability.LONG_CONTEXT -> com.webtoapp.core.i18n.AppStringsProvider.current().supportLongTextInput
    }
}

/**
 * Mapping from capability tags to feature scenarios.
 */
data class CapabilityFeatureMapping(
    val capability: ModelCapability,
    val enabledFeatures: Set<AiFeature>
) {
    companion object {
        /**
         * Gets the default capability-to-feature mapping.
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
 * AI model configuration.
 */
data class AiModel(
    val id: String,                        // Model ID (e.g., gpt-5.1-codex)
    val name: String,                      // Display name
    val provider: AiProvider,              // Provider
    val capabilities: List<ModelCapability> = listOf(ModelCapability.TEXT), // Capability tags
    val contextLength: Int = 4096,         // Context length (tokens)
    val inputPrice: Double = 0.0,          // Input price ($/million tokens)
    val outputPrice: Double = 0.0,         // Output price ($/million tokens)
    val isCustom: Boolean = false          // Whether the model was added manually
)

/**
 * API format types (for custom endpoints).
 */
enum class ApiFormat {
    OPENAI_COMPATIBLE,  // OpenAI compatible format (default)
    ANTHROPIC,          // Anthropic Claude format
    GOOGLE_GEMINI;      // Google Gemini format
    
    val displayName: String get() = when (this) {
        OPENAI_COMPATIBLE -> "OpenAI compatible"
        ANTHROPIC -> "Anthropic Claude"
        GOOGLE_GEMINI -> "Google Gemini"
    }
}

/**
 * API key configuration.
 */
data class ApiKeyConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val provider: AiProvider,
    val apiKey: String,
    val baseUrl: String? = null,                    // Custom base URL (optional)
    val customModelsEndpoint: String? = null,       // Custom models endpoint (e.g., /v1/models)
    val customChatEndpoint: String? = null,         // Custom chat endpoint (e.g., /v1/chat/completions)
    val apiFormat: ApiFormat = ApiFormat.OPENAI_COMPATIBLE, // API format type
    val alias: String? = null,                      // User-defined alias for identification
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Gets the display name (alias or provider name).
     */
    val displayName: String get() = alias?.takeIf { it.isNotBlank() } ?: provider.displayName
    
    /**
     * Gets the valid models endpoint.
     */
    fun getEffectiveModelsEndpoint(): String {
        return customModelsEndpoint?.takeIf { it.isNotBlank() } ?: provider.modelsEndpoint
    }
    
    /**
     * Gets the valid chat endpoint.
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
 * Saved model configurations chosen by users.
 */
data class SavedModel(
    val id: String = java.util.UUID.randomUUID().toString(),
    val model: AiModel,
    val apiKeyId: String,                  // Associated API key ID
    val alias: String? = null,             // User alias
    val capabilities: List<ModelCapability>, // User-selected capability tags
    val featureMappings: Map<ModelCapability, Set<AiFeature>> = emptyMap(), // Custom capability-to-feature mappings
    val isDefault: Boolean = false,        // Whether this is the default model
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Computes feature scenarios supported by this model using capability tags and custom mappings.
     */
    fun getSupportedFeatures(): Set<AiFeature> {
        val features = mutableSetOf<AiFeature>()
        capabilities.forEach { capability ->
            // Prefer custom mappings; fall back to defaults otherwise
            val mappedFeatures = featureMappings[capability]
                ?: AiFeature.entries.filter { it.defaultCapabilities.contains(capability) }.toSet()
            features.addAll(mappedFeatures)
        }
        return features
    }
    
    /**
     * Checks if this model supports the specified feature.
     */
    fun supportsFeature(feature: AiFeature): Boolean {
        return getSupportedFeatures().contains(feature)
    }
    
    /**
     * Gets the feature list for the specified capability.
     */
    fun getFeaturesForCapability(capability: ModelCapability): Set<AiFeature> {
        return featureMappings[capability]
            ?: AiFeature.entries.filter { it.defaultCapabilities.contains(capability) }.toSet()
    }
}

/**
 * AI settings (stored in DataStore).
 */
data class AiSettings(
    val apiKeys: List<ApiKeyConfig> = emptyList(),
    val savedModels: List<SavedModel> = emptyList(),
    val defaultModelId: String? = null     // Default model ID
)

/**
 * LRC generation task status.
 */
enum class LrcTaskStatus {
    PENDING,     // Pending
    PROCESSING,  // Processing
    COMPLETED,   // Completed
    FAILED       // Failed
}

/**
 * LRC task.
 */
data class LrcTask(
    val id: String = java.util.UUID.randomUUID().toString(),
    val bgmItemId: String,           // Music item ID
    val bgmName: String,             // Music name
    val bgmPath: String,             // Music path
    val modelId: String,             // Model ID used
    val status: LrcTaskStatus = LrcTaskStatus.PENDING,
    val progress: Int = 0,           // Progress 0-100
    val resultLrc: LrcData? = null,  // Generated lyrics
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

/**
 * Preset lyric themes.
 */
object PresetLrcThemes {
    val themes = listOf(
        LrcTheme(
            id = "default",
            name = AppStringsProvider.current().lrcThemeDefault,
            textColor = "#FFFFFF",
            highlightColor = "#FFD700",
            backgroundColor = "#80000000",
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "karaoke",
            name = AppStringsProvider.current().lrcThemeKaraoke,
            textColor = "#FFFFFF",
            highlightColor = "#FF4081",
            backgroundColor = "#00000000",
            strokeColor = "#000000",
            strokeWidth = 2f,
            animationType = LrcAnimationType.KARAOKE
        ),
        LrcTheme(
            id = "neon",
            name = AppStringsProvider.current().lrcThemeNeon,
            textColor = "#00FFFF",
            highlightColor = "#FF00FF",
            backgroundColor = "#40000000",
            shadowEnabled = true,
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "minimal",
            name = AppStringsProvider.current().lrcThemeMinimal,
            fontSize = 16f,
            textColor = "#CCCCCC",
            highlightColor = "#FFFFFF",
            backgroundColor = "#00000000",
            animationType = LrcAnimationType.SLIDE_UP
        ),
        LrcTheme(
            id = "classic",
            name = AppStringsProvider.current().lrcThemeClassic,
            fontSize = 20f,
            textColor = "#FFE4B5",
            highlightColor = "#FFD700",
            backgroundColor = "#60000000",
            animationType = LrcAnimationType.TYPEWRITER
        ),
        LrcTheme(
            id = "dark",
            name = AppStringsProvider.current().lrcThemeDark,
            textColor = "#AAAAAA",
            highlightColor = "#4FC3F7",
            backgroundColor = "#E0000000",
            animationType = LrcAnimationType.SCALE
        ),
        LrcTheme(
            id = "romantic",
            name = AppStringsProvider.current().lrcThemeRomantic,
            textColor = "#FFB6C1",
            highlightColor = "#FF69B4",
            backgroundColor = "#40000000",
            animationType = LrcAnimationType.FADE
        ),
        LrcTheme(
            id = "energetic",
            name = AppStringsProvider.current().lrcThemeEnergetic,
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
 * Gets the localized AI provider display name.
 */
fun AiProvider.getLocalizedDisplayName(): String = displayName

/**
 * Gets the localized AI feature display name.
 */
fun AiFeature.getLocalizedDisplayName(): String {
    return when (this) {
        AiFeature.AI_CODING -> "HTML ${com.webtoapp.core.i18n.AppStringsProvider.current().coding}"
        AiFeature.AI_CODING_IMAGE -> "HTML ${com.webtoapp.core.i18n.AppStringsProvider.current().coding} (${com.webtoapp.core.i18n.AppStringsProvider.current().image})"
        AiFeature.ICON_GENERATION -> com.webtoapp.core.i18n.AppStringsProvider.current().featureIconGeneration
        AiFeature.MODULE_DEVELOPMENT -> com.webtoapp.core.i18n.AppStringsProvider.current().featureModuleDevelopment
        AiFeature.LRC_GENERATION -> com.webtoapp.core.i18n.AppStringsProvider.current().featureLrcGeneration
        AiFeature.TRANSLATION -> com.webtoapp.core.i18n.AppStringsProvider.current().featureTranslation
        AiFeature.GENERAL -> com.webtoapp.core.i18n.AppStringsProvider.current().featureGeneralChat
    }
}

/**
 * Gets the localized AI feature description.
 */
fun AiFeature.getLocalizedDescription(): String {
    return when (this) {
        AiFeature.AI_CODING -> AppStringsProvider.current().aiCodingDesc
        AiFeature.AI_CODING_IMAGE -> AppStringsProvider.current().aiCodingImageDesc
        AiFeature.ICON_GENERATION -> AppStringsProvider.current().iconGenerationDesc
        AiFeature.MODULE_DEVELOPMENT -> AppStringsProvider.current().moduleDevelopmentDesc
        AiFeature.LRC_GENERATION -> AppStringsProvider.current().lrcGenerationDesc
        AiFeature.TRANSLATION -> AppStringsProvider.current().translationDesc
        AiFeature.GENERAL -> AppStringsProvider.current().generalChatDesc
    }
}

/**
 * Gets the localized AI provider description.
 */
fun AiProvider.getLocalizedDescription(): String = description

/**
 * Gets the localized AI provider pricing info.
 */
fun AiProvider.getLocalizedPricing(): String = pricing
