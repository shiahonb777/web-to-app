package com.webtoapp.core.ai.model

import com.google.gson.JsonParser
import com.webtoapp.core.ai.LiteLLMModelRegistry
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.data.model.AiModel
import com.webtoapp.data.model.AiProvider

internal class AiModelCatalogSupport(
    private val registry: LiteLLMModelRegistry,
    private val inference: AiModelInference
) {
    fun getRegistryFallbackModels(provider: AiProvider): List<AiModel> {
        val modelIds = registry.getRecommendedModels(provider)
        return modelIds.mapNotNull { modelId ->
            val info = registry.findModel(modelId, provider) ?: return@mapNotNull null
            AiModel(
                id = modelId,
                name = modelId,
                provider = provider,
                capabilities = registry.getCapabilities(modelId, provider)
                    ?: inference.inferCapabilities(modelId, provider),
                contextLength = info.maxInputTokens.takeIf { it > 0 }
                    ?: inference.inferContextLength(modelId, provider),
                inputPrice = info.inputCostPerMillion.takeIf { it > 0.0 }
                    ?: inference.inferInputPrice(modelId, provider),
                outputPrice = info.outputCostPerMillion
            )
        }
    }

    fun enrichModelWithRegistry(model: AiModel, provider: AiProvider): AiModel {
        val info = registry.findModel(model.id, provider) ?: return model
        return model.copy(
            contextLength = if (model.contextLength <= 0 || model.contextLength == 8192) {
                info.maxInputTokens.takeIf { it > 0 } ?: model.contextLength
            } else {
                model.contextLength
            },
            inputPrice = if (model.inputPrice <= 0.0) {
                info.inputCostPerMillion.takeIf { it > 0.0 } ?: model.inputPrice
            } else {
                model.inputPrice
            },
            outputPrice = if (model.outputPrice <= 0.0) {
                info.outputCostPerMillion.takeIf { it > 0.0 } ?: model.outputPrice
            } else {
                model.outputPrice
            },
            capabilities = if (model.capabilities.size <= 1) {
                registry.getCapabilities(model.id, provider) ?: model.capabilities
            } else {
                model.capabilities
            }
        )
    }

    fun parseModelsResponse(provider: AiProvider, response: String): List<AiModel> {
        return try {
            val json = JsonParser.parseString(response).asJsonObject

            when (provider) {
                AiProvider.GOOGLE -> {
                    json.getAsJsonArray("models")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val name = obj.get("name")?.asString ?: return@mapNotNull null
                        val modelId = name.substringAfterLast("/")
                        val methods = obj.getAsJsonArray("supportedGenerationMethods")
                        val supportsGenerate = methods?.any { it.asString == "generateContent" } ?: false
                        if (!supportsGenerate) return@mapNotNull null

                        AiModel(
                            id = modelId,
                            name = obj.get("displayName")?.asString ?: modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider),
                            contextLength = obj.get("inputTokenLimit")?.asInt ?: 4096
                        )
                    } ?: emptyList()
                }

                AiProvider.ANTHROPIC -> {
                    json.getAsJsonArray("data")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString ?: return@mapNotNull null
                        AiModel(
                            id = modelId,
                            name = obj.get("display_name")?.asString ?: modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider)
                        )
                    } ?: emptyList()
                }

                AiProvider.GLM -> {
                    json.getAsJsonArray("data")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString ?: return@mapNotNull null
                        AiModel(
                            id = modelId,
                            name = modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider)
                        )
                    } ?: emptyList()
                }

                AiProvider.VOLCANO -> {
                    val dataArray = json.getAsJsonArray("data") ?: json.getAsJsonArray("models")
                    dataArray?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString
                            ?: obj.get("model")?.asString
                            ?: return@mapNotNull null
                        AiModel(
                            id = modelId,
                            name = obj.get("name")?.asString ?: modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider)
                        )
                    } ?: emptyList()
                }

                AiProvider.MINIMAX -> {
                    val dataArray = json.getAsJsonArray("data") ?: json.getAsJsonArray("models")
                    dataArray?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString
                            ?: obj.get("model")?.asString
                            ?: return@mapNotNull null
                        AiModel(
                            id = modelId,
                            name = modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider)
                        )
                    } ?: emptyList()
                }

                AiProvider.OLLAMA -> {
                    json.getAsJsonArray("models")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelName = obj.get("name")?.asString ?: return@mapNotNull null
                        val modelId = modelName.removeSuffix(":latest")
                        AiModel(
                            id = modelId,
                            name = modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider),
                            contextLength = inference.inferContextLength(modelId, provider)
                        )
                    } ?: emptyList()
                }

                AiProvider.COHERE -> {
                    val modelsArray = json.getAsJsonArray("models") ?: json.getAsJsonArray("data")
                    modelsArray?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("name")?.asString
                            ?: obj.get("id")?.asString
                            ?: return@mapNotNull null
                        val contextLength = obj.get("context_length")?.asInt
                            ?: obj.get("max_input_tokens")?.asInt
                            ?: inference.inferContextLength(modelId, provider)
                        AiModel(
                            id = modelId,
                            name = modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider),
                            contextLength = contextLength
                        )
                    } ?: emptyList()
                }

                AiProvider.OPENROUTER -> {
                    json.getAsJsonArray("data")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString ?: return@mapNotNull null
                        val contextLength = obj.get("context_length")?.asInt ?: 4096
                        val pricing = obj.getAsJsonObject("pricing")
                        val inputPrice = pricing?.get("prompt")?.asString?.toDoubleOrNull()?.times(1_000_000) ?: 0.0
                        val outputPrice = pricing?.get("completion")?.asString?.toDoubleOrNull()?.times(1_000_000) ?: 0.0
                        AiModel(
                            id = modelId,
                            name = obj.get("name")?.asString ?: modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider),
                            contextLength = contextLength,
                            inputPrice = inputPrice,
                            outputPrice = outputPrice
                        )
                    } ?: emptyList()
                }

                else -> {
                    json.getAsJsonArray("data")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString ?: return@mapNotNull null
                        val contextLength = obj.get("context_length")?.asInt
                            ?: obj.get("context_window")?.asInt
                            ?: obj.get("max_tokens")?.asInt
                            ?: obj.get("max_context_length")?.asInt
                            ?: inference.inferContextLength(modelId, provider)
                        val pricing = obj.getAsJsonObject("pricing")
                        val inputPrice = pricing?.get("prompt")?.asString?.toDoubleOrNull()?.times(1_000_000)
                            ?: pricing?.get("input")?.asString?.toDoubleOrNull()?.times(1_000_000)
                            ?: inference.inferInputPrice(modelId, provider)
                        val outputPrice = pricing?.get("completion")?.asString?.toDoubleOrNull()?.times(1_000_000)
                            ?: pricing?.get("output")?.asString?.toDoubleOrNull()?.times(1_000_000)
                            ?: 0.0
                        AiModel(
                            id = modelId,
                            name = obj.get("name")?.asString ?: modelId,
                            provider = provider,
                            capabilities = inference.inferCapabilities(modelId, provider),
                            contextLength = contextLength,
                            inputPrice = inputPrice,
                            outputPrice = outputPrice
                        )
                    } ?: emptyList()
                }
            }
        } catch (e: Exception) {
            AppLogger.e("AiModelCatalogSupport", "解析模型列表失败: ${e.message}, response: $response")
            emptyList()
        }
    }
}
