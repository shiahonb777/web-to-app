package com.webtoapp.core.ai.v2.llm

import android.content.Context
import com.webtoapp.data.model.AiProvider
import kotlinx.coroutines.flow.Flow

interface LlmGateway {
    fun chatStream(req: ChatRequest): Flow<LlmEvent>
}

internal interface LlmProvider {
    fun supports(provider: AiProvider): Boolean
    fun chatStream(req: ChatRequest): Flow<LlmEvent>
}

class DefaultLlmGateway internal constructor(private val providers: List<LlmProvider>) : LlmGateway {
    override fun chatStream(req: ChatRequest): Flow<LlmEvent> {
        val provider = providers.firstOrNull { it.supports(req.apiKey.provider) }
            ?: providers.first { it.supports(AiProvider.OPENAI) }
        return provider.chatStream(req)
    }
    companion object {
        fun create(context: Context): LlmGateway = DefaultLlmGateway(
            listOf(AnthropicProvider(context), GeminiProvider(context), OllamaProvider(context), OpenAiCompatProvider(context))
        )
    }
}
