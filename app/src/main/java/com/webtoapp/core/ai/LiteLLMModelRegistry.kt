package com.webtoapp.core.ai

import android.content.Context
import com.google.gson.JsonParser
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.AiProvider
import com.webtoapp.data.model.ModelCapability

/**
 * LiteLLM.
 * 
 * LiteLLM (github.com/BerriAI/litellm) model_prices_and_context_window.json.
 * Note.
 * 
 * JSON (compact):.
 * {
 *   "model-id": {
 *     "p": "PROVIDER", //.
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
class LiteLLMModelRegistry(context: Context) {

    init {
        loadFromAssets(context.applicationContext)
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
     * Note.
     * Note.
     * 1. key.
     * 2. provider/model.
     * 3. provider.
     */
    fun findModel(modelId: String, provider: AiProvider? = null): ModelInfo? {
        // Note.
        models[modelId]?.let { return it }
        
        // 2. provider (litellm : "openai/gpt-4o").
        val providerPrefix = provider?.let { mapProviderToLiteLLMPrefix(it) }
        if (providerPrefix != null) {
            models["$providerPrefix/$modelId"]?.let { return it }
        }
        
        // Note.
        val bareId = if (modelId.contains("/")) modelId.substringAfter("/") else modelId
        if (bareId != modelId) {
            models[bareId]?.let { return it }
        }
        
        // 4. provider bareId.
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
     * Note.
     */
    fun getContextLength(modelId: String, provider: AiProvider? = null): Int? {
        return findModel(modelId, provider)?.maxInputTokens?.takeIf { it > 0 }
    }
    
    /**
     * ($/ token).
     */
    fun getInputPrice(modelId: String, provider: AiProvider? = null): Double? {
        return findModel(modelId, provider)?.inputCostPerMillion?.takeIf { it > 0.0 }
    }
    
    /**
     * ($/ token).
     */
    fun getOutputPrice(modelId: String, provider: AiProvider? = null): Double? {
        return findModel(modelId, provider)?.outputCostPerMillion?.takeIf { it > 0.0 }
    }
    
    /**
     * Model Capabilities.
     */
    fun getCapabilities(modelId: String, provider: AiProvider? = null): List<ModelCapability>? {
        val info = findModel(modelId, provider) ?: return null
        val caps = mutableListOf(ModelCapability.TEXT)
        
        if (info.supportsVision) caps.add(ModelCapability.IMAGE)
        if (info.supportsFunctionCalling) caps.add(ModelCapability.FUNCTION_CALL)
        if (info.supportsAudioInput || info.supportsAudioOutput) caps.add(ModelCapability.AUDIO)
        if (info.isImageGeneration) caps.add(ModelCapability.IMAGE_GENERATION)
        
        // Note.
        val id = modelId.lowercase()
        if (id.contains("code") || id.contains("codestral") || id.contains("starcoder") ||
            id.contains("codellama") || id.contains("codegemma") || id.contains("codeqwen") ||
            id.contains("deepseek-coder")) {
            caps.add(ModelCapability.CODE)
        }
        
        return caps
    }
    
    /**
     * ID.
     * API /models.
     */
    fun getDefaultModels(provider: AiProvider): List<String> {
        val providerName = provider.name
        return providerModels[providerName]?.map { key ->
            // provider ( "openai/gpt-4o" -> "gpt-4o").
            if (key.contains("/")) key.substringAfter("/") else key
        }?.distinct()?.sorted() ?: emptyList()
    }
    
    /**
     * Note.
     */
    fun getRecommendedModels(provider: AiProvider): List<String> {
        val all = getDefaultModels(provider)
        if (all.isEmpty()) return emptyList()
        
        // Note.
        val deprecated = listOf("0301", "0314", "0613", "0125", "preview", "experimental")
        return all.filter { modelId ->
            val id = modelId.lowercase()
            // Note.
            !deprecated.any { suffix -> id.endsWith(suffix) } &&
            !id.contains("instruct") && // instruct.
            !id.startsWith("ft:") // Note.
        }.take(50) // Note.
    }
    
    /**
     * Note.
     */
    fun isKnownModel(modelId: String, provider: AiProvider? = null): Boolean {
        return findModel(modelId, provider) != null
    }
    
    /**
     * Note.
     */
    fun totalModels(): Int = models.size
    
    /**
     * AiProvider LiteLLM provider.
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
