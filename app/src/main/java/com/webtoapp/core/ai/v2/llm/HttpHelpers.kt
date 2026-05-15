package com.webtoapp.core.ai.v2.llm

import com.webtoapp.data.model.AiProvider
import com.webtoapp.data.model.ApiKeyConfig
import okhttp3.Request

internal object HttpHelpers {
    fun baseUrl(apiKey: ApiKeyConfig): String =
        (apiKey.baseUrl?.takeIf { it.isNotBlank() } ?: apiKey.provider.baseUrl).trimEnd('/')

    fun joinUrl(base: String, endpoint: String): String {
        val b = base.trimEnd('/'); val e = endpoint.trimStart('/')
        val parts = e.split("/").filter { it.isNotEmpty() }
        if (parts.isNotEmpty() && b.endsWith("/${parts.first()}")) {
            val rest = parts.drop(1).joinToString("/")
            return if (rest.isEmpty()) b else "$b/$rest"
        }
        return "$b/$e"
    }

    fun applyAuth(builder: Request.Builder, apiKey: ApiKeyConfig) {
        val key = apiKey.apiKey.trim().replace("\n", "").replace("\r", "")
        when (apiKey.provider) {
            AiProvider.ANTHROPIC -> { builder.header("x-api-key", key); builder.header("anthropic-version", "2023-06-01") }
            AiProvider.GLM -> builder.header("Authorization", key)
            AiProvider.OLLAMA, AiProvider.LM_STUDIO, AiProvider.VLLM -> { if (key.isNotBlank()) builder.header("Authorization", "Bearer $key") }
            else -> builder.header("Authorization", "Bearer $key")
        }
    }

    fun classifyHttpError(code: Int, body: String): Pair<String, Boolean> {
        val lower = body.lowercase()
        val toolRelated = "tool" in lower || "function" in lower
        val msg = when (code) {
            400 -> "请求格式错误 (400): ${body.take(300)}"
            401 -> "API Key 无效或已过期 (401)"
            403 -> "访问被拒绝 (403)"
            404 -> "端点或模型不存在 (404)"
            429 -> "请求过于频繁 (429)"
            in 500..599 -> "服务器错误 ($code)"
            else -> "请求失败 ($code): ${body.take(300)}"
        }
        return msg to (code == 400 && toolRelated)
    }
}
