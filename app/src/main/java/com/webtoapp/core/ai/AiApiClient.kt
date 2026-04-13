package com.webtoapp.core.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.JsonObject
import com.webtoapp.core.ai.model.AiModelCatalogSupport
import com.webtoapp.core.ai.model.AiModelInference
import com.webtoapp.core.ai.provider.AiImageSupport
import com.webtoapp.core.ai.provider.AiResponseParser
import com.webtoapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import com.webtoapp.core.logging.AppLogger
import com.webtoapp.core.network.NetworkModule
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/**
 * AI API 客户端
 * 支持多种 AI 服务提供商
 */
class AiApiClient(private val context: Context) {
    
    private val gson = com.webtoapp.util.GsonProvider.gson
    private val registry by lazy { LiteLLMModelRegistry(context.applicationContext) }
    private val modelInference by lazy { AiModelInference(registry) }
    private val modelCatalog by lazy { AiModelCatalogSupport(registry, modelInference) }
    private val responseParser by lazy { AiResponseParser(gson) }
    private val imageSupport by lazy { AiImageSupport(gson, client) }
    
    /**
     * 清理 API Key，移除所有换行符和空白字符
     */
    private fun String.sanitize(): String = this.replace("\n", "").replace("\r", "").trim()
    
    private val client get() = NetworkModule.streamingClient
    
    /**
     * 智能拼接 URL，避免重复的路径段（如 /v1/v1）
     */
    private fun buildApiUrl(baseUrl: String, endpoint: String): String {
        val trimmedBase = baseUrl.trimEnd('/')
        val trimmedEndpoint = endpoint.trimStart('/')
        
        // 检查 baseUrl 是否已经包含 endpoint 的前缀路径（如 /v1）
        // 例如: baseUrl="https://api.example.com/v1", endpoint="/v1/models"
        // 应该变成 "https://api.example.com/v1/models" 而不是 "https://api.example.com/v1/v1/models"
        val endpointParts = trimmedEndpoint.split("/").filter { it.isNotEmpty() }
        if (endpointParts.isNotEmpty()) {
            val firstPart = endpointParts.first() // 例如 "v1"
            if (trimmedBase.endsWith("/$firstPart")) {
                // baseUrl 已经包含第一段，跳过
                val remainingEndpoint = endpointParts.drop(1).joinToString("/")
                return "$trimmedBase/$remainingEndpoint"
            }
        }
        
        return "$trimmedBase/$trimmedEndpoint"
    }
    
    /**
     * 测试 API 连接
     */
    suspend fun testConnection(apiKey: ApiKeyConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = (apiKey.baseUrl ?: apiKey.provider.baseUrl).trimEnd('/')
            // 使用自定义端点（如果配置了）
            val modelsEndpoint = apiKey.getEffectiveModelsEndpoint()
            val fullUrl = buildApiUrl(baseUrl, modelsEndpoint)
            val displayUrl = when (apiKey.provider) {
                AiProvider.GOOGLE -> "$fullUrl?key=***"
                else -> fullUrl
            }
            
            AppLogger.i("AiApiClient", "Testing API connection: provider=${apiKey.provider.name}, url=$displayUrl")
            
            val request = when (apiKey.provider) {
                AiProvider.GOOGLE -> {
                    // Google Gemini 使用不同的认证方式
                    Request.Builder()
                        .url("$fullUrl?key=${apiKey.apiKey.trim()}")
                        .get()
                        .build()
                }
                AiProvider.ANTHROPIC -> {
                    Request.Builder()
                        .url(fullUrl)
                        .header("x-api-key", apiKey.apiKey.sanitize())
                        .header("anthropic-version", "2023-06-01")
                        .get()
                        .build()
                }
                AiProvider.OLLAMA, AiProvider.LM_STUDIO, AiProvider.VLLM -> {
                    // 本地模型不需要 API Key
                    val builder = Request.Builder().url(fullUrl).get()
                    if (apiKey.apiKey.isNotBlank()) {
                        builder.header("Authorization", "Bearer ${apiKey.apiKey.sanitize()}")
                    }
                    builder.build()
                }
                AiProvider.COHERE -> {
                    Request.Builder()
                        .url(fullUrl)
                        .header("Authorization", "Bearer ${apiKey.apiKey.sanitize()}")
                        .header("Accept", "application/json")
                        .get()
                        .build()
                }
                else -> {
                    Request.Builder()
                        .url(fullUrl)
                        .header("Authorization", "Bearer ${apiKey.apiKey.sanitize()}")
                        .get()
                        .build()
                }
            }
            
            AppLogger.d("AiApiClient", "Request URL: ${request.url}")
            
            val response = client.newCall(request).execute()
            val responseCode = response.code
            val responseMessage = response.message
            
            AppLogger.i("AiApiClient", "Response: code=$responseCode, message=$responseMessage")
            
            if (response.isSuccessful) {
                AppLogger.i("AiApiClient", "API connection test SUCCESS")
                Result.success(true)
            } else {
                val errorBody = response.body?.string() ?: ""
                AppLogger.e("AiApiClient", "API connection test FAILED: code=$responseCode, body=$errorBody")
                
                // 提供更详细的错误信息
                val errorMsg = when (responseCode) {
                    400 -> "请求参数错误 (400): $errorBody"
                    401 -> "API Key 无效或已过期 (401)"
                    403 -> "访问被拒绝 (403): 请检查权限或配额"
                    404 -> "端点不存在 (404): 请检查 Base URL 是否正确，当前: $fullUrl"
                    429 -> "请求过于频繁 (429)"
                    500, 502, 503 -> "服务器错误 ($responseCode)"
                    else -> "连接失败: $responseCode - ${errorBody.take(200)}"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            AppLogger.e("AiApiClient", "API connection test EXCEPTION: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取模型列表（从 API 实时获取）
     */
    suspend fun fetchModels(apiKey: ApiKeyConfig): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = (apiKey.baseUrl ?: apiKey.provider.baseUrl).trimEnd('/')
            // 使用自定义端点（如果配置了）
            val modelsEndpoint = apiKey.getEffectiveModelsEndpoint()
            val fullUrl = buildApiUrl(baseUrl, modelsEndpoint)
            
            AppLogger.d("AiApiClient", "Fetching models: baseUrl=$baseUrl, endpoint=$modelsEndpoint, fullUrl=$fullUrl")
            
            AppLogger.d("AiApiClient", "Fetching models from: $fullUrl")
            
            val request = when (apiKey.provider) {
                AiProvider.GOOGLE -> {
                    Request.Builder()
                        .url("$fullUrl?key=${apiKey.apiKey.trim()}")
                        .get()
                        .build()
                }
                AiProvider.ANTHROPIC -> {
                    Request.Builder()
                        .url(fullUrl)
                        .header("x-api-key", apiKey.apiKey.sanitize())
                        .header("anthropic-version", "2023-06-01")
                        .get()
                        .build()
                }
                AiProvider.GLM -> {
                    Request.Builder()
                        .url(fullUrl)
                        .header("Authorization", apiKey.apiKey.sanitize())
                        .get()
                        .build()
                }
                AiProvider.OLLAMA, AiProvider.LM_STUDIO, AiProvider.VLLM -> {
                    val builder = Request.Builder().url(fullUrl).get()
                    if (apiKey.apiKey.isNotBlank()) {
                        builder.header("Authorization", "Bearer ${apiKey.apiKey.sanitize()}")
                    }
                    builder.build()
                }
                AiProvider.COHERE -> {
                    Request.Builder()
                        .url(fullUrl)
                        .header("Authorization", "Bearer ${apiKey.apiKey.sanitize()}")
                        .header("Accept", "application/json")
                        .get()
                        .build()
                }
                else -> {
                    Request.Builder()
                        .url(fullUrl)
                        .header("Authorization", "Bearer ${apiKey.apiKey.sanitize()}")
                        .get()
                        .build()
                }
            }
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val models = parseModelsResponse(apiKey.provider, body)
                if (models.isEmpty()) {
                    // API 返回空列表时，使用 LiteLLM 注册表的默认模型列表
                    val fallbackModels = getRegistryFallbackModels(apiKey.provider)
                    if (fallbackModels.isNotEmpty()) {
                        AppLogger.i("AiApiClient", "API returned empty, using ${fallbackModels.size} models from LiteLLM registry")
                        Result.success(fallbackModels)
                    } else {
                        Result.failure(Exception("API 返回的模型列表为空"))
                    }
                } else {
                    // 使用注册表数据补充模型信息
                    val enriched = models.map { model -> enrichModelWithRegistry(model, apiKey.provider) }
                    Result.success(enriched)
                }
            } else {
                val errorBody = response.body?.string() ?: ""
                // API 请求失败时也尝试使用注册表
                val fallbackModels = getRegistryFallbackModels(apiKey.provider)
                if (fallbackModels.isNotEmpty()) {
                    AppLogger.i("AiApiClient", "API failed (${response.code}), using ${fallbackModels.size} models from LiteLLM registry")
                    Result.success(fallbackModels)
                } else {
                    Result.failure(Exception("获取模型列表失败: ${response.code} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            // 网络异常时也尝试使用注册表
            val fallbackModels = getRegistryFallbackModels(apiKey.provider)
            if (fallbackModels.isNotEmpty()) {
                AppLogger.i("AiApiClient", "Network error, using ${fallbackModels.size} models from LiteLLM registry")
                Result.success(fallbackModels)
            } else {
                Result.failure(Exception("获取模型列表出错: ${e.message}"))
            }
        }
    }
    
    /**
     * 从 LiteLLM 注册表获取供应商的默认模型列表
     */
    private fun getRegistryFallbackModels(provider: AiProvider): List<AiModel> {
        return modelCatalog.getRegistryFallbackModels(provider)
    }
    
    /**
     * 使用 LiteLLM 注册表数据补充模型信息
     */
    private fun enrichModelWithRegistry(model: AiModel, provider: AiProvider): AiModel {
        return modelCatalog.enrichModelWithRegistry(model, provider)
    }
    
    /**
     * 解析模型列表响应（支持各供应商不同的响应格式）
     */
    private fun parseModelsResponse(provider: AiProvider, response: String): List<AiModel> {
        return modelCatalog.parseModelsResponse(provider, response)
    }
    
    /**
     * 根据模型名称推断能力
     * 优先使用 LiteLLM 注册表数据，回退到基于名称的推断
     */
    private fun inferCapabilities(modelId: String, provider: AiProvider? = null): List<ModelCapability> {
        return modelInference.inferCapabilities(modelId, provider)
    }
    
    /**
     * 根据模型名称推断上下文长度
     * 优先使用 LiteLLM 注册表数据，回退到基于名称的推断
     */
    private fun inferContextLength(modelId: String, provider: AiProvider? = null): Int {
        return modelInference.inferContextLength(modelId, provider)
    }
    
    /**
     * 根据模型名称推断输入价格（$/百万token）
     * 优先使用 LiteLLM 注册表数据，回退到基于名称的推断
     */
    private fun inferInputPrice(modelId: String, provider: AiProvider? = null): Double {
        return modelInference.inferInputPrice(modelId, provider)
    }
    
    /**
     * 生成应用图标
     */
    suspend fun generateAppIcon(
        context: Context,
        prompt: String,
        referenceImages: List<String>,
        apiKey: ApiKeyConfig,
        model: SavedModel
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = apiKey.baseUrl ?: apiKey.provider.baseUrl
            val iconPrompt = buildIconPrompt(prompt)
            
            // 读取参考图片（最多3张）
            val imageDataList = referenceImages.take(3).mapNotNull { path ->
                try {
                    val bytes = if (path.startsWith("content://")) {
                        context.contentResolver.openInputStream(android.net.Uri.parse(path))?.readBytes()
                    } else {
                        java.io.File(path).readBytes()
                    }
                    bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                } catch (e: Exception) { null }
            }
            
            when (apiKey.provider) {
                AiProvider.GOOGLE -> generateIconWithGemini(baseUrl, apiKey.apiKey.trim(), model.model.id, iconPrompt, imageDataList)
                else -> generateIconWithOpenAIFormat(baseUrl, apiKey.apiKey.trim(), model.model.id, iconPrompt, imageDataList)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildIconPrompt(userPrompt: String): String {
        return imageSupport.buildIconPrompt(userPrompt)
    }
    
    private fun generateIconWithGemini(
        baseUrl: String, apiKey: String, modelId: String, 
        prompt: String, images: List<String>
    ): Result<String> {
        val parts = com.google.gson.JsonArray().apply {
            add(JsonObject().apply { addProperty("text", prompt) })
            images.forEach { img ->
                add(JsonObject().apply {
                    add("inline_data", JsonObject().apply {
                        addProperty("mime_type", "image/png")
                        addProperty("data", img)
                    })
                })
            }
        }
        
        val body = JsonObject().apply {
            add("contents", com.google.gson.JsonArray().apply {
                add(JsonObject().apply { add("parts", parts) })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.8)
                add("responseModalities", com.google.gson.JsonArray().apply { add("IMAGE"); add("TEXT") })
            })
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:generateContent?key=$apiKey")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseImageFromGeminiResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("生成失败: ${response.code}"))
        }
    }
    
    private fun generateIconWithOpenAIFormat(
        baseUrl: String, apiKey: String, modelId: String,
        prompt: String, images: List<String>
    ): Result<String> {
        val content = com.google.gson.JsonArray().apply {
            add(JsonObject().apply { 
                addProperty("type", "text")
                addProperty("text", prompt) 
            })
            images.forEach { img ->
                add(JsonObject().apply {
                    addProperty("type", "image_url")
                    add("image_url", JsonObject().apply {
                        addProperty("url", "data:image/png;base64,$img")
                    })
                })
            }
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", content)
                })
            })
            addProperty("max_tokens", 4096)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer ${apiKey.sanitize()}")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseImageFromChatResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("生成失败: ${response.code}"))
        }
    }
    
    private fun parseImageFromGeminiResponse(body: String): Result<String> {
        return imageSupport.parseImageFromGeminiResponse(body)
    }
    
    private fun parseImageFromChatResponse(body: String): Result<String> {
        return imageSupport.parseImageFromChatResponse(body)
    }
    
    // ==================== 通用聊天接口 ====================
    
    /**
     * 通用聊天接口
     * @param apiKey API密钥配置
     * @param model 模型
     * @param messages 消息列表，格式为 [{"role": "system/user/assistant", "content": "..."}]
     * @param temperature 温度参数
     * @return 返回AI响应文本
     */
    suspend fun chat(
        apiKey: ApiKeyConfig,
        model: AiModel,
        messages: List<Map<String, String>>,
        temperature: Float = 0.7f
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 优先使用用户自定义的baseUrl，否则使用供应商默认的baseUrl
            val baseUrl = when {
                !apiKey.baseUrl.isNullOrBlank() -> apiKey.baseUrl.trimEnd('/')
                apiKey.provider.baseUrl.isNotBlank() -> apiKey.provider.baseUrl.trimEnd('/')
                else -> return@withContext Result.failure(Exception("未配置API地址，请在设置中填写Base URL"))
            }
            
            when (apiKey.provider) {
                AiProvider.GOOGLE -> chatWithGemini(baseUrl, apiKey.apiKey, model.id, messages, temperature)
                AiProvider.ANTHROPIC -> chatWithAnthropic(baseUrl, apiKey.apiKey, model.id, messages, temperature)
                AiProvider.GLM -> chatWithGLM(baseUrl, apiKey.apiKey, model.id, messages, temperature)
                AiProvider.VOLCANO -> chatWithVolcano(baseUrl, apiKey.apiKey, model.id, messages, temperature)
                AiProvider.OLLAMA -> chatWithOllama(baseUrl, apiKey.apiKey, model.id, messages, temperature)
                AiProvider.COHERE -> chatWithCohere(baseUrl, apiKey.apiKey, model.id, messages, temperature)
                else -> chatWithOpenAICompatible(baseUrl, apiKey.apiKey, model.id, messages, temperature)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gemini 聊天
     */
    private fun chatWithGemini(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Result<String> {
        // 转换消息格式
        val contents = com.google.gson.JsonArray()
        var systemInstruction: String? = null
        
        messages.forEach { msg ->
            val role = msg["role"] ?: "user"
            val content = msg["content"] ?: ""
            
            if (role == "system") {
                systemInstruction = content
            } else {
                contents.add(JsonObject().apply {
                    addProperty("role", if (role == "assistant") "model" else "user")
                    add("parts", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", content)
                        })
                    })
                })
            }
        }
        
        val body = JsonObject().apply {
            add("contents", contents)
            systemInstruction?.let { instruction ->
                add("systemInstruction", JsonObject().apply {
                    add("parts", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", instruction)
                        })
                    })
                })
            }
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", temperature)
                addProperty("maxOutputTokens", 8192)
            })
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:generateContent?key=$apiKey")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseGeminiChatResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    private fun parseGeminiChatResponse(body: String): Result<String> {
        return responseParser.parseGeminiChatResponse(body)
    }
    
    /**
     * Anthropic/Claude 聊天
     */
    private fun chatWithAnthropic(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Result<String> {
        // 提取系统消息
        val systemMessage = messages.find { it["role"] == "system" }?.get("content")
        val chatMessages = messages.filter { it["role"] != "system" }
        
        val messagesArray = com.google.gson.JsonArray()
        chatMessages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            addProperty("max_tokens", 8192)
            addProperty("temperature", temperature)
            systemMessage?.let { addProperty("system", it) }
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseAnthropicChatResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    private fun parseAnthropicChatResponse(body: String): Result<String> {
        return responseParser.parseAnthropicChatResponse(body)
    }
    
    /**
     * 智谱GLM 聊天
     */
    private fun chatWithGLM(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Result<String> {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            addProperty("temperature", temperature)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v4/chat/completions")
            .header("Authorization", "Bearer ${apiKey.sanitize()}")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseOpenAIChatResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    /**
     * 火山引擎 聊天
     */
    private fun chatWithVolcano(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Result<String> {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            addProperty("temperature", temperature)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v3/chat/completions")
            .header("Authorization", "Bearer ${apiKey.sanitize()}")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseOpenAIChatResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    /**
     * Ollama 聊天
     */
    private fun chatWithOllama(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Result<String> {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            add("options", JsonObject().apply {
                addProperty("temperature", temperature)
            })
            addProperty("stream", false)
        }
        
        val builder = Request.Builder()
            .url("$baseUrl/api/chat")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${apiKey.sanitize()}")
        }
        
        val response = client.newCall(builder.build()).execute()
        return if (response.isSuccessful) {
            try {
                val json = gson.fromJson(response.body?.string() ?: "", JsonObject::class.java)
                val content = json.getAsJsonObject("message")?.get("content")?.asString
                if (content != null) Result.success(content)
                else Result.failure(Exception("无法解析响应"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    /**
     * Cohere 聊天
     */
    private fun chatWithCohere(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Result<String> {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            addProperty("temperature", temperature)
            addProperty("stream", false)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v2/chat")
            .header("Authorization", "Bearer ${apiKey.sanitize()}")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            try {
                val json = gson.fromJson(response.body?.string() ?: "", JsonObject::class.java)
                val content = json.getAsJsonObject("message")?.getAsJsonArray("content")
                    ?.get(0)?.asJsonObject?.get("text")?.asString
                    ?: json.get("text")?.asString
                if (content != null) Result.success(content)
                else Result.failure(Exception("无法解析响应"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    /**
     * OpenAI 兼容格式聊天（适用于大多数供应商）
     */
    private fun chatWithOpenAICompatible(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Result<String> {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            addProperty("temperature", temperature)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer ${apiKey.sanitize()}")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseOpenAIChatResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    private fun parseOpenAIChatResponse(body: String): Result<String> {
        return responseParser.parseOpenAIChatResponse(body)
    }
    
    /**
     * 流式聊天 - 返回Flow实时输出内容
     * 支持 Google Gemini、Anthropic Claude 和 OpenAI 兼容格式
     * 
     * 优化：添加节流机制，减少UI更新频率，避免卡顿
     */
    fun chatStream(
        apiKey: ApiKeyConfig,
        model: AiModel,
        messages: List<Map<String, String>>,
        temperature: Float = 0.7f
    ): Flow<StreamEvent> = callbackFlow {
        // 优先使用用户自定义的baseUrl，否则使用供应商默认的baseUrl
        val baseUrl = when {
            !apiKey.baseUrl.isNullOrBlank() -> apiKey.baseUrl.trimEnd('/')
            apiKey.provider.baseUrl.isNotBlank() -> apiKey.provider.baseUrl.trimEnd('/')
            else -> {
                trySend(StreamEvent.Error("未配置API地址，请在设置中填写Base URL"))
                close()
                return@callbackFlow
            }
        }
        
        // 根据供应商构建不同的请求
        val request = when (apiKey.provider) {
            AiProvider.GOOGLE -> buildGeminiStreamRequest(baseUrl, apiKey.apiKey.trim(), model.id, messages, temperature)
            AiProvider.ANTHROPIC -> buildAnthropicStreamRequest(baseUrl, apiKey.apiKey.sanitize(), model.id, messages, temperature)
            AiProvider.OLLAMA -> buildOllamaStreamRequest(baseUrl, apiKey.apiKey, model.id, messages, temperature)
            else -> buildOpenAIStreamRequest(baseUrl, apiKey, model.id, messages, temperature)
        }
        
        trySend(StreamEvent.Started)
        
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val errorMsg = when {
                    e.message?.contains("connection abort", ignoreCase = true) == true -> 
                        "网络连接中断，请检查网络后重试"
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "请求超时，请检查网络连接"
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> 
                        "无法连接服务器，请检查网络或API地址"
                    e.message?.contains("Connection refused", ignoreCase = true) == true -> 
                        "服务器拒绝连接，请检查API地址是否正确"
                    else -> e.message ?: "网络连接失败"
                }
                trySend(StreamEvent.Error(errorMsg))
                close(e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    response.body?.close()
                    
                    // Parse错误信息
                    val errorMsg = when (response.code) {
                        400 -> {
                            try {
                                val json = gson.fromJson(errorBody, JsonObject::class.java)
                                val error = json.getAsJsonObject("error")
                                val message = error?.get("message")?.asString ?: errorBody
                                "请求参数错误: $message"
                            } catch (e: Exception) {
                                "请求参数错误: $errorBody"
                            }
                        }
                        401 -> "API Key 无效或已过期，请检查设置"
                        403 -> "API 访问被拒绝，请检查权限或配额"
                        404 -> "模型不存在或 API 端点错误，请检查模型名称"
                        429 -> "请求过于频繁，请稍后重试"
                        500, 502, 503 -> "服务器错误，请稍后重试"
                        else -> "请求失败: ${response.code} - $errorBody"
                    }
                    
                    trySend(StreamEvent.Error(errorMsg))
                    close()
                    return
                }
                
                try {
                    val reader = response.body?.source() ?: run {
                        trySend(StreamEvent.Error("响应体为空"))
                        close()
                        return
                    }
                    
                    val contentBuilder = StringBuilder()
                    var doneSent = false
                    var hasReceivedData = false
                    var lastReceivedPayload = ""  // 记录最后收到的数据，用于调试
                    
                    var currentEvent: String? = null
                    val dataBuffer = StringBuilder()
                    
                    fun flushEvent() {
                        val payload = dataBuffer.toString().trim()
                        dataBuffer.setLength(0)
                        if (payload.isEmpty()) return
                        hasReceivedData = true
                        lastReceivedPayload = payload.take(500)  // Save最后收到的数据（截取前500字符）
                        AppLogger.d("AiApiClient", "StreamChat received payload: ${payload.take(200)}...")
                        if (payload == "[DONE]" || currentEvent == "done" || currentEvent == "message_stop") {
                            if (!doneSent) {
                                doneSent = true
                                trySend(StreamEvent.Done(contentBuilder.toString()))
                            }
                            return
                        }
                        try {
                            val json = gson.fromJson(payload, JsonObject::class.java)
                            
                            // Error handling
                            val error = json.getAsJsonObject("error")
                            if (error != null) {
                                val errorMsg = error.get("message")?.asString ?: "API返回错误"
                                trySend(StreamEvent.Error(errorMsg))
                                close()
                                return
                            }
                            
                            // 根据供应商解析响应
                            when (apiKey.provider) {
                                AiProvider.GOOGLE -> {
                                    // Gemini 流式响应格式
                                    val candidates = json.getAsJsonArray("candidates")
                                    val content = candidates?.get(0)?.asJsonObject
                                        ?.getAsJsonObject("content")
                                        ?.getAsJsonArray("parts")
                                        ?.get(0)?.asJsonObject
                                        ?.get("text")?.asString
                                    if (!content.isNullOrEmpty()) {
                                        contentBuilder.append(content)
                                        trySend(StreamEvent.Content(content, contentBuilder.toString()))
                                    }
                                }
                                AiProvider.OLLAMA -> {
                                    // Ollama 流式响应: {"message":{"content":"..."},"done":false}
                                    val isDone = json.get("done")?.asBoolean ?: false
                                    val content = json.getAsJsonObject("message")?.get("content")?.asString
                                    if (!content.isNullOrEmpty()) {
                                        contentBuilder.append(content)
                                        trySend(StreamEvent.Content(content, contentBuilder.toString()))
                                    }
                                    if (isDone && !doneSent) {
                                        doneSent = true
                                        trySend(StreamEvent.Done(contentBuilder.toString()))
                                    }
                                }
                                AiProvider.ANTHROPIC -> {
                                    // Anthropic 流式响应格式
                                    val type = json.get("type")?.asString
                                    when (type) {
                                        "content_block_delta" -> {
                                            val delta = json.getAsJsonObject("delta")
                                            val text = delta?.get("text")?.asString
                                            if (!text.isNullOrEmpty()) {
                                                contentBuilder.append(text)
                                                trySend(StreamEvent.Content(text, contentBuilder.toString()))
                                            }
                                        }
                                        "message_stop" -> {
                                            if (!doneSent) {
                                                doneSent = true
                                                trySend(StreamEvent.Done(contentBuilder.toString()))
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    // OpenAI 兼容格式
                                    val choiceObj = json.getAsJsonArray("choices")?.get(0)?.asJsonObject
                                    val delta = choiceObj?.getAsJsonObject("delta")
                                    
                                    val content: String? = extractContentFrom(choiceObj)
                                    val reasoning = extractReasoningFrom(choiceObj, delta)
                                    if (reasoning != null) trySend(StreamEvent.Thinking(reasoning))
                                    
                                    if (content != null) {
                                        // Debug：检测空格字符
                                        val spaceCount = content.count { it == ' ' }
                                        val hasSpecialSpaces = content.any { it.code in listOf(0x00A0, 0x3000, 0x2003, 0x2002) }
                                        if (spaceCount > 0 || hasSpecialSpaces) {
                                            AppLogger.d("AiApiClient", "🔍 StreamChat Space Debug - content: '$content'")
                                            AppLogger.d("AiApiClient", "🔍 StreamChat Space Debug - length: ${content.length}, spaceCount: $spaceCount")
                                            AppLogger.d("AiApiClient", "🔍 StreamChat Space Debug - charCodes: ${content.map { "${it}(${it.code})" }}")
                                        }
                                        
                                        // 注意：之前这里会跳过开头的纯空白内容，可能导致空格丢失
                                        val shouldAppend = if (contentBuilder.isEmpty()) content.any { !it.isWhitespace() } else true
                                        if (shouldAppend) {
                                            contentBuilder.append(content)
                                            trySend(StreamEvent.Content(content, contentBuilder.toString()))
                                        } else {
                                            AppLogger.w("AiApiClient", "⚠️ Skipped whitespace-only content at start: '$content'")
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // 忽略无法解析的分片
                        }
                    }
                    
                    while (!reader.exhausted()) {
                        val line = reader.readUtf8Line() ?: break
                        
                        when {
                            line.startsWith("event:") -> {
                                currentEvent = line.removePrefix("event:").trim()
                            }
                            line.startsWith("data:") -> {
                                dataBuffer.append(line.removePrefix("data:").trimStart()).append('\n')
                            }
                            line.isBlank() -> {
                                flushEvent()
                                if (doneSent) break
                            }
                            else -> {
                                // 某些实现直接输出 JSON 行（非SSE）
                                try {
                                    val json = gson.fromJson(line, JsonObject::class.java)
                                    val choiceObj = json.getAsJsonArray("choices")?.get(0)?.asJsonObject
                                    val reasoning = extractReasoningFrom(choiceObj, choiceObj?.getAsJsonObject("delta"))
                                    if (!reasoning.isNullOrBlank()) {
                                        trySend(StreamEvent.Thinking(reasoning))
                                    }
                                    val content = extractContentFrom(choiceObj)
                                    if (!content.isNullOrEmpty()) {
                                        val shouldAppend = if (contentBuilder.isEmpty()) content.any { !it.isWhitespace() } else true
                                        if (shouldAppend) {
                                            contentBuilder.append(content)
                                            trySend(StreamEvent.Content(content, contentBuilder.toString()))
                                        }
                                    }
                                } catch (_: Exception) {
                                    // 忽略非JSON行
                                }
                            }
                        }
                    }
                    // Handle最后一个未刷新的事件
                    if (dataBuffer.isNotEmpty() && !doneSent) {
                        flushEvent()
                    }
                    
                    // 确保发送Done事件
                    if (!doneSent) {
                        if (contentBuilder.isEmpty()) {
                            val debugInfo = if (!hasReceivedData) {
                                "未收到任何数据，API可能不支持流式输出"
                            } else {
                                // 记录详细日志帮助调试
                                AppLogger.e("AiApiClient", "StreamChat 解析失败，最后收到的数据: $lastReceivedPayload")
                                "API返回数据格式异常，请查看日志或尝试其他模型。数据预览: ${lastReceivedPayload.take(100)}..."
                            }
                            trySend(StreamEvent.Error(debugInfo))
                        } else {
                            trySend(StreamEvent.Done(contentBuilder.toString()))
                        }
                    }
                    // 确保关闭 response body
                    response.body?.close()
                    close()
                } catch (e: Exception) {
                    response.body?.close()
                    trySend(StreamEvent.Error(e.message ?: "读取响应失败"))
                    close(e)
                }
            }
        })
        
        awaitClose { call.cancel() }
    }
    
    /**
     * 构建 Google Gemini 流式请求
     */
    private fun buildGeminiStreamRequest(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Request {
        val contents = com.google.gson.JsonArray()
        var systemInstruction: String? = null
        
        messages.forEach { msg ->
            val role = msg["role"] ?: "user"
            val content = msg["content"] ?: ""
            
            if (role == "system") {
                systemInstruction = content
            } else {
                contents.add(JsonObject().apply {
                    addProperty("role", if (role == "assistant") "model" else "user")
                    add("parts", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", content)
                        })
                    })
                })
            }
        }
        
        val body = JsonObject().apply {
            add("contents", contents)
            systemInstruction?.let { instruction ->
                add("systemInstruction", JsonObject().apply {
                    add("parts", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", instruction)
                        })
                    })
                })
            }
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", temperature)
                addProperty("maxOutputTokens", 8192)
            })
        }
        
        // Gemini 流式 API 使用 streamGenerateContent 端点
        return Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:streamGenerateContent?alt=sse&key=$apiKey")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
    }
    
    /**
     * 构建 Anthropic Claude 流式请求
     */
    private fun buildAnthropicStreamRequest(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Request {
        val systemMessage = messages.find { it["role"] == "system" }?.get("content")
        val chatMessages = messages.filter { it["role"] != "system" }
        
        val messagesArray = com.google.gson.JsonArray()
        chatMessages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            addProperty("max_tokens", 8192)
            addProperty("temperature", temperature)
            addProperty("stream", true)
            systemMessage?.let { addProperty("system", it) }
        }
        
        return Request.Builder()
            .url("$baseUrl/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
    }
    
    /**
     * 构建 OpenAI 兼容格式流式请求
     */
    private fun buildOpenAIStreamRequest(
        baseUrl: String,
        apiKey: ApiKeyConfig,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Request {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            addProperty("temperature", temperature)
            addProperty("max_tokens", 8192)
            addProperty("stream", true)
        }
        
        // 使用自定义端点（如果配置了）
        val streamEndpoint = apiKey.getEffectiveChatEndpoint()
        
        AppLogger.d("AiApiClient", "Building stream request: baseUrl=$baseUrl, endpoint=$streamEndpoint")
        
        return Request.Builder()
            .url(buildApiUrl(baseUrl, streamEndpoint))
            .header("Authorization", "Bearer ${apiKey.apiKey.sanitize()}")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
    }
    
    /**
     * 构建 Ollama 流式请求
     */
    private fun buildOllamaStreamRequest(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float
    ): Request {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            add("options", JsonObject().apply {
                addProperty("temperature", temperature)
            })
            addProperty("stream", true)
        }
        
        val builder = Request.Builder()
            .url("$baseUrl/api/chat")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
        if (apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${apiKey.sanitize()}")
        }
        
        return builder.build()
    }
    
    // ==================== Tool Calling 接口 ====================
    
    /**
     * 带工具调用的聊天接口
     * 支持 OpenAI 兼容格式的 Function Calling
     */
    suspend fun chatWithTools(
        apiKey: ApiKeyConfig,
        model: AiModel,
        messages: List<Map<String, String>>,
        tools: List<Map<String, Any>>,
        temperature: Float = 0.7f
    ): Result<ToolCallResponse> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = when {
                !apiKey.baseUrl.isNullOrBlank() -> apiKey.baseUrl.trimEnd('/')
                apiKey.provider.baseUrl.isNotBlank() -> apiKey.provider.baseUrl.trimEnd('/')
                else -> return@withContext Result.failure(Exception("未配置API地址"))
            }
            
            when (apiKey.provider) {
                AiProvider.GOOGLE -> chatWithToolsGemini(baseUrl, apiKey.apiKey.trim(), model.id, messages, tools, temperature)
                AiProvider.ANTHROPIC -> chatWithToolsAnthropic(baseUrl, apiKey.apiKey.sanitize(), model.id, messages, tools, temperature)
                else -> chatWithToolsOpenAI(baseUrl, apiKey.apiKey.sanitize(), model.id, messages, tools, temperature)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * OpenAI 格式的 Tool Calling
     */
    private fun chatWithToolsOpenAI(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        tools: List<Map<String, Any>>,
        temperature: Float
    ): Result<ToolCallResponse> {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val toolsArray = com.google.gson.JsonArray()
        tools.forEach { tool ->
            toolsArray.add(gson.toJsonTree(tool))
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            add("tools", toolsArray)
            addProperty("temperature", temperature)
            addProperty("max_tokens", 16384)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseOpenAIToolResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    /**
     * 解析 OpenAI Tool Calling 响应
     */
    private fun parseOpenAIToolResponse(body: String): Result<ToolCallResponse> {
        return responseParser.parseOpenAIToolResponse(body)
    }
    
    /**
     * Gemini 格式的 Tool Calling
     */
    private fun chatWithToolsGemini(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        tools: List<Map<String, Any>>,
        temperature: Float
    ): Result<ToolCallResponse> {
        val contents = com.google.gson.JsonArray()
        var systemInstruction: String? = null
        
        messages.forEach { msg ->
            val role = msg["role"] ?: "user"
            val content = msg["content"] ?: ""
            
            if (role == "system") {
                systemInstruction = content
            } else {
                contents.add(JsonObject().apply {
                    addProperty("role", if (role == "assistant") "model" else "user")
                    add("parts", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", content)
                        })
                    })
                })
            }
        }
        
        // 转换工具格式为 Gemini 格式
        val geminiTools = com.google.gson.JsonArray()
        val functionDeclarations = com.google.gson.JsonArray()
        
        tools.forEach { tool ->
            val function = tool["function"] as? Map<*, *> ?: return@forEach
            functionDeclarations.add(JsonObject().apply {
                addProperty("name", function["name"] as? String ?: "")
                addProperty("description", function["description"] as? String ?: "")
                add("parameters", gson.toJsonTree(function["parameters"]))
            })
        }
        
        geminiTools.add(JsonObject().apply {
            add("functionDeclarations", functionDeclarations)
        })
        
        val body = JsonObject().apply {
            add("contents", contents)
            add("tools", geminiTools)
            systemInstruction?.let { instruction ->
                add("systemInstruction", JsonObject().apply {
                    add("parts", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", instruction)
                        })
                    })
                })
            }
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", temperature)
                addProperty("maxOutputTokens", 16384)
            })
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:generateContent?key=$apiKey")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseGeminiToolResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    /**
     * 解析 Gemini Tool Calling 响应
     */
    private fun parseGeminiToolResponse(body: String): Result<ToolCallResponse> {
        return responseParser.parseGeminiToolResponse(body)
    }
    
    /**
     * Anthropic 格式的 Tool Calling
     */
    private fun chatWithToolsAnthropic(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        tools: List<Map<String, Any>>,
        temperature: Float
    ): Result<ToolCallResponse> {
        val systemMessage = messages.find { it["role"] == "system" }?.get("content")
        val chatMessages = messages.filter { it["role"] != "system" }
        
        val messagesArray = com.google.gson.JsonArray()
        chatMessages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        // 转换工具格式为 Anthropic 格式
        val anthropicTools = com.google.gson.JsonArray()
        tools.forEach { tool ->
            val function = tool["function"] as? Map<*, *> ?: return@forEach
            anthropicTools.add(JsonObject().apply {
                addProperty("name", function["name"] as? String ?: "")
                addProperty("description", function["description"] as? String ?: "")
                add("input_schema", gson.toJsonTree(function["parameters"]))
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            add("tools", anthropicTools)
            addProperty("max_tokens", 16384)
            addProperty("temperature", temperature)
            systemMessage?.let { addProperty("system", it) }
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseAnthropicToolResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("请求失败: ${response.code} - ${response.body?.string()}"))
        }
    }
    
    /**
     * 解析 Anthropic Tool Calling 响应
     */
    private fun parseAnthropicToolResponse(body: String): Result<ToolCallResponse> {
        return responseParser.parseAnthropicToolResponse(body)
    }
    
    // ==================== 流式 Tool Calling 接口 ====================
    
    /**
     * 流式带工具调用的聊天接口
     * 支持实时输出工具参数（如 HTML 代码）
     */
    fun chatStreamWithTools(
        apiKey: ApiKeyConfig,
        model: AiModel,
        messages: List<Map<String, Any>>,
        tools: List<Map<String, Any>>,
        temperature: Float = 0.7f
    ): Flow<ToolStreamEvent> = callbackFlow {
        val baseUrl = when {
            !apiKey.baseUrl.isNullOrBlank() -> apiKey.baseUrl.trimEnd('/')
            apiKey.provider.baseUrl.isNotBlank() -> apiKey.provider.baseUrl.trimEnd('/')
            else -> {
                trySend(ToolStreamEvent.Error("未配置API地址"))
                close()
                return@callbackFlow
            }
        }
        
        // Build请求
        val request = buildOpenAIStreamWithToolsRequest(baseUrl, apiKey, model.id, messages, tools, temperature)
        
        trySend(ToolStreamEvent.Started)
        
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                val errorMsg = when {
                    e.message?.contains("connection abort", ignoreCase = true) == true -> 
                        "网络连接中断，请检查网络后重试"
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "请求超时，请检查网络连接"
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> 
                        "无法连接服务器，请检查网络或API地址"
                    e.message?.contains("Connection refused", ignoreCase = true) == true -> 
                        "服务器拒绝连接，请检查API地址是否正确"
                    else -> e.message ?: "网络连接失败"
                }
                trySend(ToolStreamEvent.Error(errorMsg))
                close(e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    response.body?.close()
                    
                    // Parse错误信息
                    val errorMsg = when (response.code) {
                        400 -> {
                            // 尝试解析具体错误
                            try {
                                val json = gson.fromJson(errorBody, JsonObject::class.java)
                                val error = json.getAsJsonObject("error")
                                val message = error?.get("message")?.asString ?: errorBody
                                "请求参数错误: $message"
                            } catch (e: Exception) {
                                "请求参数错误: $errorBody"
                            }
                        }
                        401 -> "API Key 无效或已过期，请检查设置"
                        403 -> "API 访问被拒绝，请检查权限或配额"
                        404 -> "模型不存在或 API 端点错误，请检查模型名称"
                        429 -> "请求过于频繁，请稍后重试"
                        500, 502, 503 -> "服务器错误，请稍后重试"
                        else -> "请求失败: ${response.code} - $errorBody"
                    }
                    
                    trySend(ToolStreamEvent.Error(errorMsg))
                    close()
                    return
                }
                
                try {
                    val reader = response.body?.source() ?: run {
                        trySend(ToolStreamEvent.Error("响应体为空"))
                        close()
                        return
                    }
                    
                    val textBuilder = StringBuilder()
                    val thinkingBuilder = StringBuilder()
                    
                    // 工具调用状态
                    val toolCallsMap = mutableMapOf<Int, ToolCallState>()
                    val completedToolCalls = mutableListOf<ToolCallInfo>()
                    
                    var doneSent = false
                    val dataBuffer = StringBuilder()
                    
                    // Handle单个 JSON chunk
                    fun processStreamChunk(
                        json: JsonObject,
                        textBuilder: StringBuilder,
                        thinkingBuilder: StringBuilder,
                        toolCallsMap: MutableMap<Int, ToolCallState>,
                        completedToolCalls: MutableList<ToolCallInfo>
                    ) {
                        // 记录原始 JSON 用于调试
                        AppLogger.d("AiApiClient", "📦 Raw chunk: ${json.toString().take(300)}...")
                        
                        val choicesArray = json.getAsJsonArray("choices")
                        if (choicesArray == null || choicesArray.size() == 0) {
                            AppLogger.w("AiApiClient", "⚠️ No choices in response: ${json.toString().take(200)}")
                            return
                        }
                        
                        val choiceObj = choicesArray.get(0)?.asJsonObject ?: return
                        val delta = choiceObj.getAsJsonObject("delta")
                        val finishReason = choiceObj.get("finish_reason")?.let { 
                            if (it.isJsonNull) null else it.asString 
                        }
                        
                        AppLogger.d("AiApiClient", "📝 Delta: ${delta?.toString()?.take(200) ?: "null"}, finishReason: $finishReason")
                        
                        // Handle文本内容
                        delta?.get("content")?.let { contentElem ->
                            if (!contentElem.isJsonNull) {
                                val content = contentElem.asString
                                if (content.isNotEmpty()) {
                                    // Debug：检测空格字符
                                    val spaceCount = content.count { it == ' ' }
                                    val hasSpecialSpaces = content.any { it.code in listOf(0x00A0, 0x3000, 0x2003, 0x2002) }
                                    if (spaceCount > 0 || hasSpecialSpaces) {
                                        AppLogger.d("AiApiClient", "🔍 Space Debug - content: '$content'")
                                        AppLogger.d("AiApiClient", "🔍 Space Debug - length: ${content.length}, spaceCount: $spaceCount, hasSpecialSpaces: $hasSpecialSpaces")
                                        AppLogger.d("AiApiClient", "🔍 Space Debug - charCodes: ${content.map { "${it}(${it.code})" }}")
                                    }
                                    textBuilder.append(content)
                                    AppLogger.d("AiApiClient", "TextDelta: ${content.take(50)}...")
                                    trySend(ToolStreamEvent.TextDelta(content, textBuilder.toString()))
                                }
                            }
                        }
                        
                        // Handle思考内容 - 支持多种字段名
                        // MiniMax 使用 reasoning_content
                        val thinkingContent = delta?.get("reasoning_content")?.let { 
                            if (!it.isJsonNull) it.asString else null 
                        } ?: delta?.get("reasoning")?.let { 
                            if (!it.isJsonNull) it.asString else null 
                        } ?: delta?.get("thinking")?.let { 
                            if (!it.isJsonNull) it.asString else null 
                        }
                        
                        if (!thinkingContent.isNullOrEmpty()) {
                            thinkingBuilder.append(thinkingContent)
                            AppLogger.d("AiApiClient", "ThinkingDelta: ${thinkingContent.take(50)}...")
                            trySend(ToolStreamEvent.ThinkingDelta(thinkingContent, thinkingBuilder.toString()))
                        }
                        
                        // Handle工具调用 - 支持多种格式
                        // 1. OpenAI 标准格式: delta.tool_calls
                        delta?.getAsJsonArray("tool_calls")?.forEach { tc ->
                            val tcObj = tc.asJsonObject
                            val index = tcObj.get("index")?.asInt ?: 0
                            
                            AppLogger.d("AiApiClient", "Tool call chunk (delta.tool_calls): index=$index, tcObj=$tcObj")
                            
                            val state = toolCallsMap.getOrPut(index) { ToolCallState() }
                            
                            tcObj.get("id")?.asString?.let { id ->
                                if (state.id.isEmpty()) {
                                    state.id = id
                                    AppLogger.d("AiApiClient", "Tool call id: $id")
                                }
                            }
                            
                            tcObj.getAsJsonObject("function")?.let { func ->
                                func.get("name")?.asString?.let { name ->
                                    if (state.name.isEmpty()) {
                                        state.name = name
                                        AppLogger.d("AiApiClient", "Tool call name: $name")
                                        trySend(ToolStreamEvent.ToolCallStart(name, state.id))
                                    }
                                }
                                
                                func.get("arguments")?.asString?.let { argsDelta ->
                                    state.arguments.append(argsDelta)
                                    AppLogger.d("AiApiClient", "Tool arguments delta: ${argsDelta.take(100)}..., total: ${state.arguments.length}")
                                    trySend(ToolStreamEvent.ToolArgumentsDelta(
                                        state.id,
                                        argsDelta,
                                        state.arguments.toString()
                                    ))
                                }
                            }
                        }
                        
                        // 2. 旧版 OpenAI 格式: delta.function_call
                        if (toolCallsMap.isEmpty()) {
                            delta?.getAsJsonObject("function_call")?.let { funcCall ->
                                AppLogger.d("AiApiClient", "Tool call chunk (delta.function_call): $funcCall")
                                
                                val state = toolCallsMap.getOrPut(0) { ToolCallState() }
                                
                                funcCall.get("name")?.asString?.let { name ->
                                    if (state.name.isEmpty()) {
                                        state.name = name
                                        state.id = "func_call_0"
                                        AppLogger.d("AiApiClient", "Function call name: $name")
                                        trySend(ToolStreamEvent.ToolCallStart(name, state.id))
                                    }
                                }
                                
                                funcCall.get("arguments")?.asString?.let { argsDelta ->
                                    state.arguments.append(argsDelta)
                                    AppLogger.d("AiApiClient", "Function arguments delta: ${argsDelta.take(100)}...")
                                    trySend(ToolStreamEvent.ToolArgumentsDelta(
                                        state.id,
                                        argsDelta,
                                        state.arguments.toString()
                                    ))
                                }
                            }
                        }
                        
                        // 3. 某些模型在 message 而不是 delta 中返回工具调用（非流式部分）
                        if (toolCallsMap.isEmpty()) {
                            choiceObj.getAsJsonObject("message")?.let { message ->
                                message.getAsJsonArray("tool_calls")?.forEach { tc ->
                                    val tcObj = tc.asJsonObject
                                    val index = tcObj.get("index")?.asInt ?: toolCallsMap.size
                                    
                                    AppLogger.d("AiApiClient", "Tool call chunk (message.tool_calls): index=$index, tcObj=$tcObj")
                                    
                                    val state = toolCallsMap.getOrPut(index) { ToolCallState() }
                                    
                                    tcObj.get("id")?.asString?.let { id ->
                                        state.id = id
                                    }
                                    
                                    tcObj.getAsJsonObject("function")?.let { func ->
                                        func.get("name")?.asString?.let { name ->
                                            state.name = name
                                            trySend(ToolStreamEvent.ToolCallStart(name, state.id))
                                        }
                                        
                                        func.get("arguments")?.asString?.let { args ->
                                            state.arguments.clear()
                                            state.arguments.append(args)
                                            trySend(ToolStreamEvent.ToolArgumentsDelta(
                                                state.id,
                                                args,
                                                args
                                            ))
                                        }
                                    }
                                }
                                
                                // 旧版格式: message.function_call
                                message.getAsJsonObject("function_call")?.let { funcCall ->
                                    AppLogger.d("AiApiClient", "Tool call chunk (message.function_call): $funcCall")
                                    
                                    val state = toolCallsMap.getOrPut(0) { ToolCallState() }
                                    
                                    funcCall.get("name")?.asString?.let { name ->
                                        state.name = name
                                        state.id = "func_call_0"
                                        trySend(ToolStreamEvent.ToolCallStart(name, state.id))
                                    }
                                    
                                    funcCall.get("arguments")?.asString?.let { args ->
                                        state.arguments.clear()
                                        state.arguments.append(args)
                                        trySend(ToolStreamEvent.ToolArgumentsDelta(
                                            state.id,
                                            args,
                                            args
                                        ))
                                    }
                                }
                            }
                        }
                        
                        // Check是否完成
                        // Support多种 finish_reason: tool_calls, function_call, stop
                        if (finishReason == "tool_calls" || finishReason == "function_call" || finishReason == "stop") {
                            AppLogger.d("AiApiClient", "Finish reason: $finishReason, toolCallsMap size: ${toolCallsMap.size}")
                            toolCallsMap.values.forEach { state ->
                                AppLogger.d("AiApiClient", "Tool state: name=${state.name}, id=${state.id}, args length=${state.arguments.length}")
                                if (state.name.isNotEmpty()) {
                                    val info = ToolCallInfo(state.id, state.name, state.arguments.toString())
                                    completedToolCalls.add(info)
                                    trySend(ToolStreamEvent.ToolCallComplete(
                                        state.id,
                                        state.name,
                                        state.arguments.toString()
                                    ))
                                }
                            }
                        }
                    }
                    
                    while (!reader.exhausted()) {
                        val line = reader.readUtf8Line() ?: break
                        
                        when {
                            line.startsWith("data:") -> {
                                val data = line.removePrefix("data:").trimStart()
                                if (data == "[DONE]") {
                                    if (!doneSent) {
                                        doneSent = true
                                        AppLogger.d("AiApiClient", "Stream done, textBuilder length: ${textBuilder.length}, thinkingBuilder length: ${thinkingBuilder.length}")
                                        trySend(ToolStreamEvent.Done(textBuilder.toString(), completedToolCalls))
                                    }
                                } else if (data.isNotEmpty()) {
                                    // 累积数据到 buffer
                                    dataBuffer.append(data)
                                    
                                    // 尝试解析累积的 JSON
                                    val fullPayload = dataBuffer.toString()
                                    try {
                                        val json = gson.fromJson(fullPayload, JsonObject::class.java)
                                        // Parse成功，处理这个事件并清空 buffer
                                        AppLogger.d("AiApiClient", "Parsed JSON chunk, length: ${fullPayload.length}")
                                        dataBuffer.setLength(0)
                                        processStreamChunk(json, textBuilder, thinkingBuilder, toolCallsMap, completedToolCalls)
                                    } catch (e: Exception) {
                                        // JSON 不完整，保留 buffer 继续累积
                                        AppLogger.d("AiApiClient", "JSON incomplete, buffer size: ${dataBuffer.length}, waiting for more data...")
                                    }
                                }
                            }
                            line.isBlank() -> {
                                // Empty行表示事件结束，尝试处理累积的数据
                                if (dataBuffer.isNotEmpty()) {
                                    val payload = dataBuffer.toString().trim()
                                    dataBuffer.setLength(0)
                                    if (payload.isNotEmpty() && payload != "[DONE]") {
                                        try {
                                            val json = gson.fromJson(payload, JsonObject::class.java)
                                            processStreamChunk(json, textBuilder, thinkingBuilder, toolCallsMap, completedToolCalls)
                                        } catch (e: Exception) {
                                            AppLogger.e("AiApiClient", "Final parse error: ${e.message}")
                                        }
                                    }
                                }
                                if (doneSent) break
                            }
                        }
                    }
                    
                    // Handle最后的数据
                    if (dataBuffer.isNotEmpty() && !doneSent) {
                        val payload = dataBuffer.toString().trim()
                        dataBuffer.setLength(0)
                        if (payload.isNotEmpty() && payload != "[DONE]") {
                            try {
                                val json = gson.fromJson(payload, JsonObject::class.java)
                                processStreamChunk(json, textBuilder, thinkingBuilder, toolCallsMap, completedToolCalls)
                            } catch (e: Exception) {
                                AppLogger.e("AiApiClient", "Final buffer parse error: ${e.message}")
                            }
                        }
                    }
                    
                    if (!doneSent) {
                        AppLogger.d("AiApiClient", "Final Done: text=${textBuilder.length}, thinking=${thinkingBuilder.length}, tools=${completedToolCalls.size}")
                        trySend(ToolStreamEvent.Done(textBuilder.toString(), completedToolCalls))
                    }
                    
                    // 确保关闭 response body
                    response.body?.close()
                    close()
                } catch (e: Exception) {
                    response.body?.close()
                    trySend(ToolStreamEvent.Error(e.message ?: "读取响应失败"))
                    close(e)
                }
            }
        })
        
        awaitClose { call.cancel() }
    }
    
    /**
     * 工具调用状态（用于流式解析）
     */
    private class ToolCallState {
        var id: String = ""
        var name: String = ""
        val arguments = StringBuilder()
    }
    
    /**
     * 构建 OpenAI 格式的流式工具调用请求
     */
    private fun buildOpenAIStreamWithToolsRequest(
        baseUrl: String,
        apiKey: ApiKeyConfig,
        modelId: String,
        messages: List<Map<String, Any>>,
        tools: List<Map<String, Any>>,
        temperature: Float
    ): Request {
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"] as? String)
                val content = msg["content"]
                if (content is String) addProperty("content", content)
                // tool_call_id for tool response messages
                val toolCallId = msg["tool_call_id"]
                if (toolCallId is String) addProperty("tool_call_id", toolCallId)
                // tool_calls for assistant messages (multi-turn ReAct)
                val toolCalls = msg["tool_calls"]
                if (toolCalls != null) add("tool_calls", gson.toJsonTree(toolCalls))
            })
        }
        
        val toolsArray = com.google.gson.JsonArray()
        tools.forEach { tool ->
            toolsArray.add(gson.toJsonTree(tool))
        }
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", messagesArray)
            add("tools", toolsArray)
            addProperty("temperature", temperature)
            addProperty("max_tokens", 16384)
            addProperty("stream", true)
            
            // 强制模型使用工具（如果提供了工具）
            // 根据模型类型选择合适的 tool_choice 值
            // 某些模型（如 DeepSeek、GLM）可能不支持 "required"
            if (tools.isNotEmpty()) {
                val modelLower = modelId.lowercase()
                val providerName = apiKey.provider.name.lowercase()
                val toolChoice = when {
                    // DeepSeek 模型使用 "auto"，因为 "required" 可能不被支持
                    modelLower.contains("deepseek") -> "auto"
                    // GLM 模型使用 "auto"
                    modelLower.contains("glm") -> "auto"
                    // Qwen 模型使用 "auto"
                    modelLower.contains("qwen") -> "auto"
                    // 豆包模型使用 "auto"
                    modelLower.contains("doubao") -> "auto"
                    // 硅基流动平台的模型使用 "auto"
                    providerName == "siliconflow" -> "auto"
                    // MiniMax 模型使用 "auto"
                    providerName == "minimax" || modelLower.contains("minimax") -> "auto"
                    // 火山引擎使用 "auto"
                    providerName == "volcano" -> "auto"
                    // OpenAI 和其他支持 "required" 的模型
                    else -> "auto"  // 改为默认使用 "auto"，更兼容
                }
                addProperty("tool_choice", toolChoice)
            }
            
            // 为支持思考模式的模型添加特殊参数
            val modelLower = modelId.lowercase()
            when {
                // DeepSeek 思考模型
                modelLower.contains("deepseek") && (modelLower.contains("reasoner") || modelLower.contains("r1")) -> {
                    // DeepSeek R1 默认启用思考，无需额外参数
                }
                // Qwen 思考模型
                modelLower.contains("qwen") && modelLower.contains("qwq") -> {
                    // QwQ 默认启用思考
                }
                // Claude 思考模式 (需要 extended_thinking)
                modelLower.contains("claude") && modelLower.contains("thinking") -> {
                    add("thinking", JsonObject().apply {
                        addProperty("type", "enabled")
                        addProperty("budget_tokens", 10000)
                    })
                }
            }
            
            // Enable流式选项以获取更多信息
            add("stream_options", JsonObject().apply {
                addProperty("include_usage", true)
            })
        }
        
        val streamEndpoint = when (apiKey.provider) {
            AiProvider.GLM -> "/v4/chat/completions"
            AiProvider.VOLCANO -> "/v3/chat/completions"
            else -> "/v1/chat/completions"
        }
        
        // 记录完整的请求体用于调试
        val requestBodyJson = gson.toJson(body)
        AppLogger.d("AiApiClient", "🔧 Tool calling request URL: $baseUrl$streamEndpoint")
        AppLogger.d("AiApiClient", "🔧 Tool calling request model: $modelId")
        AppLogger.d("AiApiClient", "🔧 Tool calling request tools count: ${tools.size}")
        AppLogger.d("AiApiClient", "🔧 Tool calling request body (first 1000 chars): ${requestBodyJson.take(1000)}")
        
        return Request.Builder()
            .url("$baseUrl$streamEndpoint")
            .header("Authorization", "Bearer ${apiKey.apiKey.sanitize()}")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()
    }
    
    // ==================== 图像生成接口 ====================
    
    /**
     * 生成图像
     * @return 返回 base64 编码的图像数据
     */
    suspend fun generateImage(
        prompt: String,
        apiKey: ApiKeyConfig,
        model: SavedModel,
        width: Int = 512,
        height: Int = 512
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = when {
                !apiKey.baseUrl.isNullOrBlank() -> apiKey.baseUrl.trimEnd('/')
                apiKey.provider.baseUrl.isNotBlank() -> apiKey.provider.baseUrl.trimEnd('/')
                else -> return@withContext Result.failure(Exception("未配置API地址"))
            }
            
            when (apiKey.provider) {
                AiProvider.OPENAI, AiProvider.OPENROUTER -> {
                    generateImageWithDallE(baseUrl, apiKey.apiKey, model.model.id, prompt, width, height)
                }
                AiProvider.GOOGLE -> {
                    generateImageWithGemini(baseUrl, apiKey.apiKey, model.model.id, prompt)
                }
                else -> {
                    // 尝试使用 OpenAI 兼容格式
                    generateImageWithOpenAICompatible(baseUrl, apiKey.apiKey, model.model.id, prompt, width, height)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 使用 DALL-E 格式生成图像
     */
    private fun generateImageWithDallE(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        prompt: String,
        width: Int,
        height: Int
    ): Result<String> {
        val size = "${width}x${height}"
        
        val body = JsonObject().apply {
            addProperty("model", modelId)
            addProperty("prompt", prompt)
            addProperty("n", 1)
            addProperty("size", size)
            addProperty("response_format", "b64_json")
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/images/generations")
            .header("Authorization", "Bearer ${apiKey.sanitize()}")
            .header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseImageFromDallEResponse(response.body?.string() ?: "")
        } else {
            val errorBody = response.body?.string() ?: ""
            Result.failure(Exception("图像生成失败: ${response.code} - $errorBody"))
        }
    }
    
    /**
     * 使用 Gemini 生成图像
     */
    private fun generateImageWithGemini(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        prompt: String
    ): Result<String> {
        val parts = com.google.gson.JsonArray().apply {
            add(JsonObject().apply { addProperty("text", prompt) })
        }
        
        val body = JsonObject().apply {
            add("contents", com.google.gson.JsonArray().apply {
                add(JsonObject().apply { add("parts", parts) })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.8)
                add("responseModalities", com.google.gson.JsonArray().apply { 
                    add("IMAGE")
                    add("TEXT") 
                })
            })
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:generateContent?key=$apiKey")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseImageFromGeminiImageResponse(response.body?.string() ?: "")
        } else {
            val errorBody = response.body?.string() ?: ""
            Result.failure(Exception("图像生成失败: ${response.code} - $errorBody"))
        }
    }
    
    /**
     * 使用 OpenAI 兼容格式生成图像
     */
    private fun generateImageWithOpenAICompatible(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        prompt: String,
        width: Int,
        height: Int
    ): Result<String> {
        // 尝试 DALL-E 格式
        return generateImageWithDallE(baseUrl, apiKey, modelId, prompt, width, height)
    }
    
    /**
     * 解析 DALL-E 响应
     */
    private fun parseImageFromDallEResponse(body: String): Result<String> {
        return imageSupport.parseImageFromDallEResponse(body)
    }
    
    /**
     * 解析 Gemini 图像响应
     */
    private fun parseImageFromGeminiImageResponse(body: String): Result<String> {
        return imageSupport.parseImageFromGeminiImageResponse(body)
    }
    
    /**
     * 下载图像并转为 base64
     */
    private fun downloadImageAsBase64(url: String): Result<String> {
        return imageSupport.downloadImageAsBase64(url)
    }
}

