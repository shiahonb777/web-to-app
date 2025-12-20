package com.webtoapp.core.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * AI API 客户端
 * 支持多种 AI 服务提供商
 */
class AiApiClient(private val context: Context) {
    
    private val gson = Gson()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * 测试 API 连接
     */
    suspend fun testConnection(apiKey: ApiKeyConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = apiKey.baseUrl ?: apiKey.provider.baseUrl
            val modelsEndpoint = apiKey.provider.modelsEndpoint
            
            val request = when (apiKey.provider) {
                AiProvider.GOOGLE -> {
                    // Google Gemini 使用不同的认证方式
                    Request.Builder()
                        .url("$baseUrl$modelsEndpoint?key=${apiKey.apiKey.trim()}")
                        .get()
                        .build()
                }
                AiProvider.ANTHROPIC -> {
                    Request.Builder()
                        .url("$baseUrl$modelsEndpoint")
                        .header("x-api-key", apiKey.apiKey.trim())
                        .header("anthropic-version", "2023-06-01")
                        .get()
                        .build()
                }
                else -> {
                    Request.Builder()
                        .url("$baseUrl$modelsEndpoint")
                        .header("Authorization", "Bearer ${apiKey.apiKey.trim()}")
                        .get()
                        .build()
                }
            }
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("连接失败: ${response.code} - ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取模型列表（从 API 实时获取）
     */
    suspend fun fetchModels(apiKey: ApiKeyConfig): Result<List<AiModel>> = withContext(Dispatchers.IO) {
        try {
            val baseUrl = apiKey.baseUrl ?: apiKey.provider.baseUrl
            val modelsEndpoint = apiKey.provider.modelsEndpoint
            
            val request = when (apiKey.provider) {
                AiProvider.GOOGLE -> {
                    Request.Builder()
                        .url("$baseUrl$modelsEndpoint?key=${apiKey.apiKey.trim()}")
                        .get()
                        .build()
                }
                AiProvider.ANTHROPIC -> {
                    Request.Builder()
                        .url("$baseUrl$modelsEndpoint")
                        .header("x-api-key", apiKey.apiKey.trim())
                        .header("anthropic-version", "2023-06-01")
                        .get()
                        .build()
                }
                AiProvider.GLM -> {
                    Request.Builder()
                        .url("$baseUrl$modelsEndpoint")
                        .header("Authorization", apiKey.apiKey.trim())
                        .get()
                        .build()
                }
                else -> {
                    Request.Builder()
                        .url("$baseUrl$modelsEndpoint")
                        .header("Authorization", "Bearer ${apiKey.apiKey.trim()}")
                        .get()
                        .build()
                }
            }
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val models = parseModelsResponse(apiKey.provider, body)
                if (models.isEmpty()) {
                    Result.failure(Exception("API 返回的模型列表为空"))
                } else {
                    Result.success(models)
                }
            } else {
                val errorBody = response.body?.string() ?: ""
                Result.failure(Exception("获取模型列表失败: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("获取模型列表出错: ${e.message}"))
        }
    }
    
    /**
     * 解析模型列表响应（支持各供应商不同的响应格式）
     */
    private fun parseModelsResponse(provider: AiProvider, response: String): List<AiModel> {
        return try {
            val json = JsonParser.parseString(response).asJsonObject
            
            when (provider) {
                AiProvider.GOOGLE -> {
                    // Google 格式: {"models": [{"name": "models/gemini-1.5-pro", "displayName": "..."}]}
                    json.getAsJsonArray("models")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val name = obj.get("name")?.asString ?: return@mapNotNull null
                        val modelId = name.substringAfterLast("/")
                        // 过滤掉不支持 generateContent 的模型
                        val methods = obj.getAsJsonArray("supportedGenerationMethods")
                        val supportsGenerate = methods?.any { 
                            it.asString == "generateContent" 
                        } ?: false
                        if (!supportsGenerate) return@mapNotNull null
                        
                        AiModel(
                            id = modelId,
                            name = obj.get("displayName")?.asString ?: modelId,
                            provider = provider,
                            capabilities = inferCapabilities(modelId),
                            contextLength = obj.get("inputTokenLimit")?.asInt ?: 4096
                        )
                    } ?: emptyList()
                }
                AiProvider.ANTHROPIC -> {
                    // Anthropic 格式: {"data": [{"id": "claude-3-5-sonnet-20241022", "display_name": "..."}]}
                    json.getAsJsonArray("data")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString ?: return@mapNotNull null
                        AiModel(
                            id = modelId,
                            name = obj.get("display_name")?.asString ?: modelId,
                            provider = provider,
                            capabilities = inferCapabilities(modelId)
                        )
                    } ?: emptyList()
                }
                AiProvider.GLM -> {
                    // 智谱 GLM 格式: {"data": [{"id": "glm-4-plus", ...}]}
                    json.getAsJsonArray("data")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString ?: return@mapNotNull null
                        AiModel(
                            id = modelId,
                            name = modelId,
                            provider = provider,
                            capabilities = inferCapabilities(modelId)
                        )
                    } ?: emptyList()
                }
                AiProvider.VOLCANO -> {
                    // 火山引擎格式: {"data": [{"id": "...", "model": "..."}]} 或其他
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
                            capabilities = inferCapabilities(modelId)
                        )
                    } ?: emptyList()
                }
                AiProvider.MINIMAX -> {
                    // MiniMax 格式
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
                            capabilities = inferCapabilities(modelId)
                        )
                    } ?: emptyList()
                }
                AiProvider.OPENROUTER -> {
                    // OpenRouter 格式: {"data": [{"id": "openai/gpt-4o", "name": "..."}]}
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
                            capabilities = inferCapabilities(modelId),
                            contextLength = contextLength,
                            inputPrice = inputPrice,
                            outputPrice = outputPrice
                        )
                    } ?: emptyList()
                }
                else -> {
                    // 标准 OpenAI 格式: {"data": [{"id": "gpt-4o", ...}]}
                    json.getAsJsonArray("data")?.mapNotNull { modelJson ->
                        val obj = modelJson.asJsonObject
                        val modelId = obj.get("id")?.asString ?: return@mapNotNull null
                        // 尝试解析上下文长度（不同API可能使用不同字段名）
                        val contextLength = obj.get("context_length")?.asInt
                            ?: obj.get("context_window")?.asInt
                            ?: obj.get("max_tokens")?.asInt
                            ?: obj.get("max_context_length")?.asInt
                            ?: inferContextLength(modelId)
                        // 尝试解析价格（如果有pricing对象）
                        val pricing = obj.getAsJsonObject("pricing")
                        val inputPrice = pricing?.get("prompt")?.asString?.toDoubleOrNull()?.times(1_000_000)
                            ?: pricing?.get("input")?.asString?.toDoubleOrNull()?.times(1_000_000)
                            ?: inferInputPrice(modelId)
                        val outputPrice = pricing?.get("completion")?.asString?.toDoubleOrNull()?.times(1_000_000)
                            ?: pricing?.get("output")?.asString?.toDoubleOrNull()?.times(1_000_000)
                            ?: 0.0
                        AiModel(
                            id = modelId,
                            name = obj.get("name")?.asString ?: modelId,
                            provider = provider,
                            capabilities = inferCapabilities(modelId),
                            contextLength = contextLength,
                            inputPrice = inputPrice,
                            outputPrice = outputPrice
                        )
                    } ?: emptyList()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AiApiClient", "解析模型列表失败: ${e.message}, response: $response")
            emptyList()
        }
    }
    
    /**
     * 根据模型名称推断能力
     */
    private fun inferCapabilities(modelId: String): List<ModelCapability> {
        val id = modelId.lowercase()
        val capabilities = mutableListOf(ModelCapability.TEXT)
        
        // 音频能力
        if (id.contains("audio") || id.contains("whisper") || 
            id.contains("gemini-1.5") || id.contains("gemini-2") ||
            id.contains("gpt-4o") || id.contains("realtime")) {
            capabilities.add(ModelCapability.AUDIO)
        }
        
        // 图像能力
        if (id.contains("vision") || id.contains("gpt-4o") || 
            id.contains("gemini") || id.contains("claude-3")) {
            capabilities.add(ModelCapability.IMAGE)
        }
        
        // 代码能力
        if (id.contains("code") || id.contains("codex") || 
            id.contains("deepseek-coder")) {
            capabilities.add(ModelCapability.CODE)
        }
        
        // 图像生成能力
        if (id.contains("dall-e") || id.contains("imagen") || 
            id.contains("image-generation") || id.contains("gpt-image")) {
            capabilities.add(ModelCapability.IMAGE_GENERATION)
        }
        
        return capabilities
    }
    
    /**
     * 根据模型名称推断上下文长度
     */
    private fun inferContextLength(modelId: String): Int {
        val id = modelId.lowercase()
        return when {
            // 超长上下文模型
            id.contains("1m") || id.contains("1000k") -> 1000000
            id.contains("200k") -> 200000
            id.contains("128k") -> 128000
            id.contains("100k") -> 100000
            id.contains("64k") -> 64000
            id.contains("32k") -> 32000
            id.contains("16k") -> 16000
            // 特定模型推断
            id.contains("gpt-4o") || id.contains("gpt-4-turbo") -> 128000
            id.contains("gpt-4") -> 8192
            id.contains("gpt-3.5") -> 16000
            id.contains("claude-3") || id.contains("claude-4") -> 200000
            id.contains("gemini-1.5") || id.contains("gemini-2") -> 1000000
            id.contains("gemini") -> 32000
            id.contains("deepseek") -> 64000
            id.contains("qwen") -> 32000
            id.contains("glm-4") -> 128000
            id.contains("doubao") && id.contains("256k") -> 256000
            id.contains("doubao") && id.contains("128k") -> 128000
            id.contains("doubao") -> 32000
            else -> 8192  // 默认8K
        }
    }
    
    /**
     * 根据模型名称推断输入价格（$/百万token）
     */
    private fun inferInputPrice(modelId: String): Double {
        val id = modelId.lowercase()
        return when {
            // OpenAI
            id.contains("gpt-4o-mini") -> 0.15
            id.contains("gpt-4o") -> 2.5
            id.contains("gpt-4-turbo") -> 10.0
            id.contains("gpt-4") -> 30.0
            id.contains("gpt-3.5") -> 0.5
            // Claude
            id.contains("claude-3-opus") -> 15.0
            id.contains("claude-3-sonnet") || id.contains("claude-3.5-sonnet") -> 3.0
            id.contains("claude-3-haiku") -> 0.25
            // Gemini
            id.contains("gemini-1.5-pro") || id.contains("gemini-2") -> 1.25
            id.contains("gemini-1.5-flash") -> 0.075
            id.contains("gemini") -> 0.5
            // DeepSeek
            id.contains("deepseek") -> 0.14
            // 国产模型通常较便宜
            id.contains("qwen") || id.contains("glm") || id.contains("doubao") -> 0.5
            // 免费模型
            id.contains("free") -> 0.0
            else -> 0.0  // 未知价格显示为0
        }
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
    
    private fun buildIconPrompt(userPrompt: String): String = """
生成一个精美的应用图标：
- 尺寸：1024x1024，正方形
- 风格：现代简洁专业
- 背景：纯色或简单渐变
- 图案：居中清晰，辨识度高

用户需求：$userPrompt

直接输出图标图片。
    """.trimIndent()
    
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
            .header("Authorization", "Bearer $apiKey")
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
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val parts = json.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
            
            parts?.forEach { part ->
                val inlineData = part.asJsonObject.getAsJsonObject("inlineData")
                if (inlineData != null) {
                    val data = inlineData.get("data")?.asString
                    if (data != null) return Result.success(data)
                }
            }
            Result.failure(Exception("未找到图像数据"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseImageFromChatResponse(body: String): Result<String> {
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val content = json.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString ?: ""
            
            // 尝试从内容中提取 base64 图像
            val base64Regex = "data:image/[^;]+;base64,([A-Za-z0-9+/=]+)".toRegex()
            val match = base64Regex.find(content)
            if (match != null) {
                Result.success(match.groupValues[1])
            } else {
                Result.failure(Exception("未找到图像数据"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val text = json.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
            
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("无法解析响应"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val text = json.getAsJsonArray("content")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString
            
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("无法解析响应"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            .header("Authorization", "Bearer $apiKey")
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
            .header("Authorization", "Bearer $apiKey")
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
            .header("Authorization", "Bearer $apiKey")
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
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val content = json.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString
            
            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("无法解析响应"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 流式聊天 - 返回Flow实时输出内容
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
        
        // 构建请求体
        val messagesArray = com.google.gson.JsonArray()
        messages.forEach { msg ->
            messagesArray.add(JsonObject().apply {
                addProperty("role", msg["role"])
                addProperty("content", msg["content"])
            })
        }
        
        val body = JsonObject().apply {
            addProperty("model", model.id)
            add("messages", messagesArray)
            addProperty("temperature", temperature)
            addProperty("max_tokens", 8192)
            addProperty("stream", true)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer ${apiKey.apiKey}")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        trySend(StreamEvent.Started)
        
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                trySend(StreamEvent.Error(e.message ?: "连接失败"))
                close(e)
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    trySend(StreamEvent.Error("请求失败: ${response.code}"))
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
                    
                    while (!reader.exhausted()) {
                        val line = reader.readUtf8Line() ?: break
                        
                        if (line.startsWith("data: ")) {
                            val data = line.removePrefix("data: ").trim()
                            
                            if (data == "[DONE]") {
                                trySend(StreamEvent.Done(contentBuilder.toString()))
                                break
                            }
                            
                            try {
                                val json = gson.fromJson(data, JsonObject::class.java)
                                val delta = json.getAsJsonArray("choices")
                                    ?.get(0)?.asJsonObject
                                    ?.getAsJsonObject("delta")
                                
                                val content = delta?.get("content")?.asString
                                val reasoning = delta?.get("reasoning_content")?.asString
                                    ?: delta?.get("thinking")?.asString
                                
                                if (reasoning != null) {
                                    trySend(StreamEvent.Thinking(reasoning))
                                }
                                
                                if (content != null) {
                                    contentBuilder.append(content)
                                    trySend(StreamEvent.Content(content, contentBuilder.toString()))
                                }
                            } catch (e: Exception) {
                                // 忽略解析错误，继续处理
                            }
                        }
                    }
                    
                    if (contentBuilder.isNotEmpty()) {
                        trySend(StreamEvent.Done(contentBuilder.toString()))
                    }
                    close()
                } catch (e: Exception) {
                    trySend(StreamEvent.Error(e.message ?: "读取响应失败"))
                    close(e)
                }
            }
        })
        
        awaitClose { call.cancel() }
    }
}

/**
 * 流式事件类型
 */
sealed class StreamEvent {
    object Started : StreamEvent()
    data class Thinking(val content: String) : StreamEvent()
    data class Content(val delta: String, val accumulated: String) : StreamEvent()
    data class Done(val fullContent: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
