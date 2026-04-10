package com.webtoapp.core.ai

import android.content.Context
import com.google.gson.JsonParser
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.AiProvider
import com.webtoapp.data.model.ModelCapability

/**
 * LiteLLM 模型注册表
 * 
 * 基于 LiteLLM (github.com/BerriAI/litellm) 的 model_prices_and_context_window.json
 * 提供完整的模型配置数据：上下文长度、价格、能力标签等
 * 
 * JSON 格式 (compact):
 * {
 *   "model-id": {
 *     "p": "PROVIDER",     // 供应商枚举名
 *     "mi": 128000,        // max input tokens
 *     "mo": 16384,         // max output tokens
 *     "ic": 2.5,           // input cost per million tokens ($)
 *     "oc": 10.0,          // output cost per million tokens ($)
 *     "v": 1,              // supports vision
 *     "f": 1,              // supports function calling
 *     "ai": 1,             // supports audio input
 *     "ao": 1,             // supports audio output
 *     "ig": 1              // image generation mode
 *   }
 * }
 */
class LiteLLMModelRegistry private constructor() {
    
    companion object {
        @Volatile
        private var instance: LiteLLMModelRegistry? = null
        
        fun getInstance(context: Context): LiteLLMModelRegistry {
            return instance ?: synchronized(this) {
                instance ?: LiteLLMModelRegistry().also { registry ->
                    registry.loadFromAssets(context)
                    instance = registry
                }
            }
        }
    }
    
    // modelKey -> ModelInfo
    private val models = mutableMapOf<String, ModelInfo>()
    
    // provider -> list of model keys (for listing default models per provider)
    private val providerModels = mutableMapOf<String, MutableList<String>>()
    
    data class ModelInfo(
        val provider: String,           // AiProvider enum name
        val maxInputTokens: Int = 0,
        val maxOutputTokens: Int = 0,
        val inputCostPerMillion: Double = 0.0,
        val outputCostPerMillion: Double = 0.0,
        val supportsVision: Boolean = false,
        val supportsFunctionCalling: Boolean = false,
        val supportsAudioInput: Boolean = false,
        val supportsAudioOutput: Boolean = false,
        val isImageGeneration: Boolean = false
    )
    
    private fun loadFromAssets(context: Context) {
        try {
            val jsonStr = context.assets.open("ai/litellm_models.json")
                .bufferedReader().use { it.readText() }
            
            val json = JsonParser.parseString(jsonStr).asJsonObject
            
            json.entrySet().forEach { (key, value) ->
                if (!value.isJsonObject) return@forEach
                val obj = value.asJsonObject
                
                val provider = obj.get("p")?.asString ?: return@forEach
                
                val info = ModelInfo(
                    provider = provider,
                    maxInputTokens = obj.get("mi")?.asInt ?: 0,
                    maxOutputTokens = obj.get("mo")?.asInt ?: 0,
                    inputCostPerMillion = obj.get("ic")?.asDouble ?: 0.0,
                    outputCostPerMillion = obj.get("oc")?.asDouble ?: 0.0,
                    supportsVision = obj.has("v"),
                    supportsFunctionCalling = obj.has("f"),
                    supportsAudioInput = obj.has("ai"),
                    supportsAudioOutput = obj.has("ao"),
                    isImageGeneration = obj.has("ig")
                )
                
                models[key] = info
                providerModels.getOrPut(provider) { mutableListOf() }.add(key)
            }
            
            AppLogger.i("LiteLLMRegistry", "Loaded ${models.size} models from LiteLLM registry")
        } catch (e: Exception) {
            AppLogger.e("LiteLLMRegistry", "Failed to load LiteLLM registry: ${e.message}")
        }
    }
    
    /**
     * 查找模型信息
     * 支持多种匹配策略：
     * 1. 精确匹配 key
     * 2. provider/model 格式匹配
     * 3. 模糊匹配（去掉provider前缀后匹配）
     */
    fun findModel(modelId: String, provider: AiProvider? = null): ModelInfo? {
        // 1. 精确匹配
        models[modelId]?.let { return it }
        
        // 2. 带 provider 前缀匹配 (litellm 格式: "openai/gpt-4o")
        val providerPrefix = provider?.let { mapProviderToLiteLLMPrefix(it) }
        if (providerPrefix != null) {
            models["$providerPrefix/$modelId"]?.let { return it }
        }
        
        // 3. 去掉已有前缀再匹配
        val bareId = if (modelId.contains("/")) modelId.substringAfter("/") else modelId
        if (bareId != modelId) {
            models[bareId]?.let { return it }
        }
        
        // 4. 在对应 provider 的模型列表中查找包含 bareId 的模型
        if (provider != null) {
            val providerName = provider.name
            providerModels[providerName]?.forEach { key ->
                val keyBare = if (key.contains("/")) key.substringAfter("/") else key
                if (keyBare == bareId || keyBare == modelId) {
                    return models[key]
                }
            }
        }
        
        return null
    }
    
    /**
     * 获取模型上下文长度
     */
    fun getContextLength(modelId: String, provider: AiProvider? = null): Int? {
        return findModel(modelId, provider)?.maxInputTokens?.takeIf { it > 0 }
    }
    
    /**
     * 获取模型输入价格 ($/百万token)
     */
    fun getInputPrice(modelId: String, provider: AiProvider? = null): Double? {
        return findModel(modelId, provider)?.inputCostPerMillion?.takeIf { it > 0.0 }
    }
    
    /**
     * 获取模型输出价格 ($/百万token)
     */
    fun getOutputPrice(modelId: String, provider: AiProvider? = null): Double? {
        return findModel(modelId, provider)?.outputCostPerMillion?.takeIf { it > 0.0 }
    }
    
    /**
     * 获取模型能力列表
     */
    fun getCapabilities(modelId: String, provider: AiProvider? = null): List<ModelCapability>? {
        val info = findModel(modelId, provider) ?: return null
        val caps = mutableListOf(ModelCapability.TEXT)
        
        if (info.supportsVision) caps.add(ModelCapability.IMAGE)
        if (info.supportsFunctionCalling) caps.add(ModelCapability.FUNCTION_CALL)
        if (info.supportsAudioInput || info.supportsAudioOutput) caps.add(ModelCapability.AUDIO)
        if (info.isImageGeneration) caps.add(ModelCapability.IMAGE_GENERATION)
        
        // 基于模型名推断代码能力
        val id = modelId.lowercase()
        if (id.contains("code") || id.contains("codestral") || id.contains("starcoder") ||
            id.contains("codellama") || id.contains("codegemma") || id.contains("codeqwen") ||
            id.contains("deepseek-coder")) {
            caps.add(ModelCapability.CODE)
        }
        
        return caps
    }
    
    /**
     * 获取指定供应商的所有已知模型ID列表
     * 用于在 API 不提供 /models 端点时提供默认模型列表
     */
    fun getDefaultModels(provider: AiProvider): List<String> {
        val providerName = provider.name
        return providerModels[providerName]?.map { key ->
            // 去掉 provider 前缀 (如 "openai/gpt-4o" -> "gpt-4o")
            if (key.contains("/")) key.substringAfter("/") else key
        }?.distinct()?.sorted() ?: emptyList()
    }
    
    /**
     * 获取指定供应商的推荐模型（常用、非过时的模型）
     */
    fun getRecommendedModels(provider: AiProvider): List<String> {
        val all = getDefaultModels(provider)
        if (all.isEmpty()) return emptyList()
        
        // 过滤掉明显过时的模型
        val deprecated = listOf("0301", "0314", "0613", "0125", "preview", "experimental")
        return all.filter { modelId ->
            val id = modelId.lowercase()
            // 保留最新版本，过滤旧版本
            !deprecated.any { suffix -> id.endsWith(suffix) } &&
            !id.contains("instruct") && // 通常 instruct 变体不推荐直接使用
            !id.startsWith("ft:") // 排除微调模型
        }.take(50) // 限制数量
    }
    
    /**
     * 模型是否已知（在注册表中有记录）
     */
    fun isKnownModel(modelId: String, provider: AiProvider? = null): Boolean {
        return findModel(modelId, provider) != null
    }
    
    /**
     * 获取注册表中的总模型数
     */
    fun totalModels(): Int = models.size
    
    /**
     * 映射 AiProvider 到 LiteLLM 的 provider 前缀
     */
    private fun mapProviderToLiteLLMPrefix(provider: AiProvider): String? {
        return when (provider) {
            AiProvider.OPENAI -> "openai"
            AiProvider.ANTHROPIC -> "anthropic"
            AiProvider.GOOGLE -> "gemini"
            AiProvider.GROK -> "xai"
            AiProvider.MISTRAL -> "mistral"
            AiProvider.COHERE -> "cohere_chat"
            AiProvider.AI21 -> "ai21"
            AiProvider.GROQ -> "groq"
            AiProvider.CEREBRAS -> "cerebras"
            AiProvider.SAMBANOVA -> "sambanova"
            AiProvider.TOGETHER -> "together_ai"
            AiProvider.PERPLEXITY -> "perplexity"
            AiProvider.FIREWORKS -> "fireworks_ai"
            AiProvider.DEEPINFRA -> "deepinfra"
            AiProvider.NOVITA -> "novita"
            AiProvider.DEEPSEEK -> "deepseek"
            AiProvider.QWEN -> "dashscope"
            AiProvider.VOLCANO -> "volcengine"
            AiProvider.MOONSHOT -> "moonshot"
            AiProvider.MINIMAX -> "minimax"
            AiProvider.OPENROUTER -> "openrouter"
            AiProvider.OLLAMA -> "ollama"
            else -> null
        }
    }
}
