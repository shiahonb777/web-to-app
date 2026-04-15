package com.webtoapp.core.ai.model

import com.webtoapp.core.ai.LiteLLMModelRegistry
import com.webtoapp.data.model.AiProvider
import com.webtoapp.data.model.ModelCapability

internal class AiModelInference(
    private val registry: LiteLLMModelRegistry
) {
    fun inferCapabilities(modelId: String, provider: AiProvider? = null): List<ModelCapability> {
        registry.getCapabilities(modelId, provider)?.let { return it }

        val id = modelId.lowercase()
        val capabilities = mutableListOf(ModelCapability.TEXT)

        if (
            id.contains("audio") || id.contains("whisper") ||
            id.contains("gemini-1.5") || id.contains("gemini-2") || id.contains("gemini-3") ||
            id.contains("gpt-4o") || id.contains("realtime")
        ) {
            capabilities.add(ModelCapability.AUDIO)
        }

        if (
            id.contains("vision") || id.contains("gpt-4o") || id.contains("gpt-5") ||
            id.contains("gemini") || id.contains("claude-3") || id.contains("claude-4") ||
            id.contains("pixtral") || id.contains("mistral-large") ||
            id.contains("llava") || id.contains("bakllava") ||
            id.contains("qwen-vl") || id.contains("qwen2-vl") || id.contains("qwen2.5-vl") ||
            id.contains("glm-4v") || id.contains("step-1v") || id.contains("step-2v") ||
            id.contains("yi-vision") || id.contains("internvl") ||
            id.contains("moonshot-v") || id.contains("kimi-v") ||
            id.contains("hunyuan-vision") || id.contains("grok-2-vision") || id.contains("grok-3")
        ) {
            capabilities.add(ModelCapability.IMAGE)
        }

        if (
            id.contains("code") || id.contains("codex") ||
            id.contains("deepseek-coder") || id.contains("codestral") ||
            id.contains("starcoder") || id.contains("codegemma") ||
            id.contains("codellama") || id.contains("codeqwen")
        ) {
            capabilities.add(ModelCapability.CODE)
        }

        if (
            id.contains("dall-e") || id.contains("imagen") ||
            id.contains("image-generation") || id.contains("gpt-image") ||
            id.contains("stable-diffusion") || id.contains("sdxl") ||
            id.contains("flux") || id.contains("playground")
        ) {
            capabilities.add(ModelCapability.IMAGE_GENERATION)
        }

        return capabilities
    }

    fun inferContextLength(modelId: String, provider: AiProvider? = null): Int {
        registry.getContextLength(modelId, provider)?.let { return it }

        val id = modelId.lowercase()
        return when {
            id.contains("1m") || id.contains("1000k") -> 1000000
            id.contains("256k") -> 256000
            id.contains("200k") -> 200000
            id.contains("128k") -> 128000
            id.contains("100k") -> 100000
            id.contains("64k") -> 64000
            id.contains("32k") -> 32000
            id.contains("16k") -> 16000
            id.contains("8k") -> 8192
            id.contains("gpt-5") || id.contains("gpt-4o") || id.contains("gpt-4-turbo") -> 128000
            id.contains("gpt-4") -> 8192
            id.contains("gpt-3.5") -> 16000
            id.contains("o1") || id.contains("o3") || id.contains("o4") -> 200000
            id.contains("claude-3") || id.contains("claude-4") -> 200000
            id.contains("gemini-1.5") || id.contains("gemini-2") || id.contains("gemini-3") -> 1000000
            id.contains("gemini") -> 32000
            id.contains("mistral-large") || id.contains("mistral-medium") -> 128000
            id.contains("mistral-small") || id.contains("mistral-nemo") -> 128000
            id.contains("mixtral") -> 32000
            id.contains("mistral") || id.contains("codestral") -> 32000
            id.contains("command-r-plus") || id.contains("command-r") -> 128000
            id.contains("command") -> 4096
            id.contains("jamba-1.5") -> 256000
            id.contains("jamba") -> 256000
            id.contains("grok-3") || id.contains("grok-2") -> 131072
            id.contains("grok") -> 8192
            id.contains("deepseek") -> 64000
            id.contains("qwen-long") || id.contains("qwen-turbo") -> 1000000
            id.contains("qwen2.5") || id.contains("qwen3") -> 131072
            id.contains("qwen") -> 32000
            id.contains("glm-4") -> 128000
            id.contains("glm") -> 8192
            id.contains("doubao") -> 32000
            id.contains("moonshot") || id.contains("kimi") -> 128000
            id.contains("baichuan") -> 32000
            id.contains("yi-large") -> 32000
            id.contains("yi") -> 16000
            id.contains("step-2") -> 256000
            id.contains("step-1") -> 128000
            id.contains("hunyuan") -> 32000
            id.contains("spark") -> 8192
            id.contains("minimax") || id.contains("abab") -> 245760
            id.contains("llama-3.3") || id.contains("llama-3.1") -> 131072
            id.contains("llama-3") || id.contains("llama3") -> 8192
            id.contains("llama-2") || id.contains("llama2") -> 4096
            id.contains("llama") -> 8192
            id.contains("sonar") -> 127072
            else -> 8192
        }
    }

    fun inferInputPrice(modelId: String, provider: AiProvider? = null): Double {
        registry.getInputPrice(modelId, provider)?.let { return it }

        val id = modelId.lowercase()
        return when {
            id.contains("free") || id.contains(":free") -> 0.0
            id.contains("gpt-4o-mini") -> 0.15
            id.contains("gpt-4o") -> 2.5
            id.contains("gpt-5") -> 10.0
            id.contains("gpt-4-turbo") -> 10.0
            id.contains("gpt-4") -> 30.0
            id.contains("gpt-3.5") -> 0.5
            id.contains("o1-mini") || id.contains("o3-mini") || id.contains("o4-mini") -> 1.1
            id.contains("o1") || id.contains("o3") -> 10.0
            id.contains("claude-4-opus") || id.contains("claude-3-opus") -> 15.0
            id.contains("claude-4-sonnet") || id.contains("claude-3.5-sonnet") || id.contains("claude-3.7-sonnet") -> 3.0
            id.contains("claude-3-sonnet") -> 3.0
            id.contains("claude-3-haiku") || id.contains("claude-3.5-haiku") -> 0.25
            id.contains("gemini-3") || id.contains("gemini-2.5-pro") -> 1.25
            id.contains("gemini-2.5-flash") || id.contains("gemini-2.0-flash") -> 0.075
            id.contains("gemini-1.5-pro") -> 1.25
            id.contains("gemini-1.5-flash") -> 0.075
            id.contains("gemini") -> 0.5
            id.contains("grok-3") -> 3.0
            id.contains("grok-2") -> 2.0
            id.contains("grok") -> 0.5
            id.contains("mistral-large") -> 2.0
            id.contains("mistral-medium") -> 2.7
            id.contains("mistral-small") -> 0.2
            id.contains("codestral") -> 0.3
            id.contains("mixtral-8x22b") -> 0.9
            id.contains("mixtral") -> 0.24
            id.contains("mistral") -> 0.25
            id.contains("command-r-plus") -> 2.5
            id.contains("command-r") -> 0.15
            id.contains("command") -> 1.0
            id.contains("jamba-1.5-large") -> 2.0
            id.contains("jamba-1.5-mini") -> 0.2
            id.contains("jamba") -> 0.5
            id.contains("deepseek") -> 0.14
            id.contains("qwen") -> 0.5
            id.contains("glm") -> 0.5
            id.contains("doubao") -> 0.3
            id.contains("moonshot") || id.contains("kimi") -> 1.0
            id.contains("minimax") || id.contains("abab") -> 1.0
            id.contains("baichuan") -> 0.5
            id.contains("yi") -> 0.3
            id.contains("step") -> 0.5
            id.contains("hunyuan") -> 0.5
            id.contains("spark") -> 0.5
            id.contains("llama-3.3-70b") || id.contains("llama-3.1-70b") -> 0.59
            id.contains("llama-3.1-405b") -> 3.0
            id.contains("llama-3.1-8b") || id.contains("llama-3-8b") -> 0.05
            id.contains("llama") -> 0.2
            id.contains("sonar-pro") -> 3.0
            id.contains("sonar") -> 1.0
            id.contains("ollama") || id.contains("lmstudio") -> 0.0
            else -> 0.0
        }
    }
}
