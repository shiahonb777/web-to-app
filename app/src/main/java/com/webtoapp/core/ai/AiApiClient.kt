package com.webtoapp.core.ai

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.ai.htmlcoding.StreamDelta
import com.webtoapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
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
     * 发送音频进行 LRC 生成
     * 支持多种供应商，自动选择最佳的音频处理方式
     */
    suspend fun generateLrc(
        apiKey: ApiKeyConfig,
        model: AiModel,
        audioPath: String,
        targetLanguage: String = "zh"
    ): Result<LrcData> = withContext(Dispatchers.IO) {
        try {
            // 读取音频文件并转为 Base64
            val audioBytes = if (audioPath.startsWith("asset:///")) {
                val assetPath = audioPath.removePrefix("asset:///")
                context.assets.open(assetPath).readBytes()
            } else {
                java.io.File(audioPath).readBytes()
            }
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            
            // 根据供应商构建请求
            val baseUrl = apiKey.baseUrl ?: apiKey.provider.baseUrl
            
            val result = when (apiKey.provider) {
                AiProvider.GOOGLE -> {
                    generateLrcWithGemini(baseUrl, apiKey.apiKey.trim(), model.id, audioBase64, targetLanguage)
                }
                AiProvider.OPENAI -> {
                    generateLrcWithOpenAI(baseUrl, apiKey.apiKey.trim(), model.id, audioBase64, targetLanguage)
                }
                AiProvider.OPENROUTER -> {
                    generateLrcWithOpenRouter(baseUrl, apiKey.apiKey.trim(), model.id, audioBase64, targetLanguage)
                }
                AiProvider.ANTHROPIC -> {
                    // Claude 不原生支持音频，返回提示
                    Result.failure(Exception("Claude 模型不支持原生音频理解，请使用支持音频的模型（如 Gemini、GPT-4o）或使用手动对齐功能"))
                }
                AiProvider.GLM -> {
                    generateLrcWithGLM(baseUrl, apiKey.apiKey.trim(), model.id, audioBase64, targetLanguage)
                }
                AiProvider.VOLCANO -> {
                    generateLrcWithVolcano(baseUrl, apiKey.apiKey.trim(), model.id, audioBase64, targetLanguage)
                }
                AiProvider.MINIMAX -> {
                    generateLrcWithMiniMax(baseUrl, apiKey.apiKey.trim(), model.id, audioBase64, targetLanguage)
                }
                AiProvider.GROK, AiProvider.DEEPSEEK, AiProvider.SILICONFLOW, AiProvider.CUSTOM -> {
                    generateLrcWithOpenAICompatible(baseUrl, apiKey.apiKey.trim(), model.id, audioBase64, targetLanguage)
                }
            }
            
            result
        } catch (e: Exception) {
            Result.failure(Exception("LRC 生成失败: ${e.message}"))
        }
    }
    
    /**
     * 使用 Gemini 生成 LRC
     */
    private fun generateLrcWithGemini(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        audioBase64: String,
        targetLanguage: String
    ): Result<LrcData> {
        val prompt = buildLrcPrompt(targetLanguage)
        
        val requestBody = JsonObject().apply {
            add("contents", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    add("parts", com.google.gson.JsonArray().apply {
                        // 先放音频，让模型先"听"
                        add(JsonObject().apply {
                            add("inline_data", JsonObject().apply {
                                addProperty("mime_type", "audio/mpeg")
                                addProperty("data", audioBase64)
                            })
                        })
                        // 再放提示词
                        add(JsonObject().apply {
                            addProperty("text", prompt)
                        })
                    })
                })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("temperature", 0.05)  // 极低温度以获得更精确稳定的输出
                addProperty("maxOutputTokens", 8192)
                addProperty("topP", 0.8)
                addProperty("topK", 40)
            })
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:generateContent?key=$apiKey")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: ""
            return parseLrcFromGeminiResponse(body)
        } else {
            return Result.failure(Exception("Gemini API 调用失败: ${response.code}"))
        }
    }
    
    /**
     * 使用 OpenAI API 生成 LRC
     * GPT-4o 等模型支持音频理解
     */
    private fun generateLrcWithOpenAI(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        audioBase64: String,
        targetLanguage: String
    ): Result<LrcData> {
        val prompt = buildLrcPrompt(targetLanguage)
        
        // OpenAI GPT-4o 音频格式
        val requestBody = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", com.google.gson.JsonArray().apply {
                        // 音频部分 - 使用 OpenAI 的 input_audio 格式
                        add(JsonObject().apply {
                            addProperty("type", "input_audio")
                            add("input_audio", JsonObject().apply {
                                addProperty("data", audioBase64)
                                addProperty("format", "mp3")
                            })
                        })
                        // 提示词
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", prompt)
                        })
                    })
                })
            })
            addProperty("temperature", 0.1)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseLrcFromOpenAIResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("OpenAI API 调用失败: ${response.code}"))
        }
    }
    
    /**
     * 使用 OpenRouter API 生成 LRC
     * OpenRouter 支持多种模型，使用统一的格式
     */
    private fun generateLrcWithOpenRouter(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        audioBase64: String,
        targetLanguage: String
    ): Result<LrcData> {
        val prompt = buildLrcPrompt(targetLanguage)
        
        val requestBody = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", com.google.gson.JsonArray().apply {
                        // 音频作为 URL 格式
                        add(JsonObject().apply {
                            addProperty("type", "audio_url")
                            add("audio_url", JsonObject().apply {
                                addProperty("url", "data:audio/mp3;base64,$audioBase64")
                            })
                        })
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", prompt)
                        })
                    })
                })
            })
            addProperty("temperature", 0.1)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://webtoapp.app")
            .header("X-Title", "WebToApp LRC Generator")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseLrcFromOpenAIResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("OpenRouter API 调用失败: ${response.code}"))
        }
    }
    
    /**
     * 使用智谱 GLM API 生成 LRC
     */
    private fun generateLrcWithGLM(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        audioBase64: String,
        targetLanguage: String
    ): Result<LrcData> {
        val prompt = buildLrcPrompt(targetLanguage)
        
        val requestBody = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "audio_url")
                            add("audio_url", JsonObject().apply {
                                addProperty("url", "data:audio/mp3;base64,$audioBase64")
                            })
                        })
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", prompt)
                        })
                    })
                })
            })
            addProperty("temperature", 0.1)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v4/chat/completions")
            .header("Authorization", apiKey)
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseLrcFromOpenAIResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("GLM API 调用失败: ${response.code}"))
        }
    }
    
    /**
     * 使用火山引擎 API 生成 LRC
     */
    private fun generateLrcWithVolcano(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        audioBase64: String,
        targetLanguage: String
    ): Result<LrcData> {
        val prompt = buildLrcPrompt(targetLanguage)
        
        val requestBody = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "audio_url")
                            add("audio_url", JsonObject().apply {
                                addProperty("url", "data:audio/mp3;base64,$audioBase64")
                            })
                        })
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", prompt)
                        })
                    })
                })
            })
            addProperty("temperature", 0.1)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v3/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseLrcFromOpenAIResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("火山引擎 API 调用失败: ${response.code}"))
        }
    }
    
    /**
     * 使用 MiniMax API 生成 LRC
     */
    private fun generateLrcWithMiniMax(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        audioBase64: String,
        targetLanguage: String
    ): Result<LrcData> {
        val prompt = buildLrcPrompt(targetLanguage)
        
        val requestBody = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "audio")
                            add("audio", JsonObject().apply {
                                addProperty("audio_bytes", audioBase64)
                                addProperty("format", "mp3")
                            })
                        })
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", prompt)
                        })
                    })
                })
            })
            addProperty("temperature", 0.1)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseLrcFromOpenAIResponse(response.body?.string() ?: "")
        } else {
            Result.failure(Exception("MiniMax API 调用失败: ${response.code}"))
        }
    }
    
    /**
     * 使用 OpenAI 兼容格式的 API 生成 LRC
     * 适用于 Grok、DeepSeek、硅基流动、自定义端点等
     */
    private fun generateLrcWithOpenAICompatible(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        audioBase64: String,
        targetLanguage: String
    ): Result<LrcData> {
        val prompt = buildLrcPrompt(targetLanguage)
        
        // 尝试多种音频格式，先用 audio_url 格式
        val requestBody = JsonObject().apply {
            addProperty("model", modelId)
            add("messages", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    add("content", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("type", "audio_url")
                            add("audio_url", JsonObject().apply {
                                addProperty("url", "data:audio/mp3;base64,$audioBase64")
                            })
                        })
                        add(JsonObject().apply {
                            addProperty("type", "text")
                            addProperty("text", prompt)
                        })
                    })
                })
            })
            addProperty("temperature", 0.1)
            addProperty("max_tokens", 8192)
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(gson.toJson(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            parseLrcFromOpenAIResponse(response.body?.string() ?: "")
        } else {
            val errorCode = response.code
            val errorBody = response.body?.string() ?: ""
            // 如果是 400 错误，可能是不支持音频格式
            if (errorCode == 400 && (errorBody.contains("audio") || errorBody.contains("unsupported"))) {
                Result.failure(Exception("该模型不支持音频理解，请使用支持音频的模型（如 Gemini 2.0、GPT-4o）或使用手动对齐功能"))
            } else {
                Result.failure(Exception("API 调用失败: $errorCode - $errorBody"))
            }
        }
    }
    
    /**
     * 构建 LRC 生成提示词 - 专业版，参考 OpenLRC 项目的最佳实践
     * 强调时间精确对齐和内容准确性
     */
    private fun buildLrcPrompt(targetLanguage: String): String {
        val langName = when (targetLanguage.lowercase()) {
            "zh", "zh-cn", "chinese" -> "中文"
            "en", "english" -> "English"
            "ja", "japanese" -> "日本語"
            "ko", "korean" -> "한국어"
            else -> targetLanguage
        }
        
        return """
# 音频转录与 LRC 字幕生成任务

你是一位专业的音频转录和字幕时间轴对齐专家。请仔细聆听这段音频，并生成精确对齐的 LRC 格式歌词/字幕。

## 核心任务
1. **语音识别**：准确转录音频中的所有语音内容
2. **时间对齐**：为每句内容标注精确的开始时间戳
3. **结构识别**：识别前奏、间奏、尾奏等无人声部分

## 时间对齐要求（最重要）
- 时间格式：`[mm:ss.xx]`，精确到 10 毫秒（如 [00:15.30]）
- 时间戳必须标注在该句**开始发声的精确时刻**
- 仔细聆听每个句子的起始点，考虑：
  - 音乐节拍和节奏
  - 呼吸和停顿
  - 句与句之间的间隔
- 前奏、间奏部分用 `♪` 或 `...` 标注

## 内容转录要求
- 使用 $langName 输出
- 保持原文准确性，不要添加、删除或修改内容
- 按自然语句或乐句分行
- 每行控制在 15-25 字以内，便于阅读
- 如遇听不清的部分，根据上下文合理推断

## 转录流程
1. 完整聆听音频，把握整体结构和时长
2. 识别第一句人声开始的时间点（前奏时长）
3. 逐句转录，同时记录每句的精确开始时间
4. 标注间奏、尾奏等特殊段落
5. 检查时间戳的连贯性和准确性

## 输出格式
只输出纯 LRC 格式内容，不要包含任何解释或说明：

```
[00:00.00] ♪ 前奏
[00:12.50] 第一句歌词在这里开始
[00:16.80] 第二句紧跟着
[00:25.00] ♪ 间奏
[00:35.20] 继续下一段
```

## 特别注意
- 即使是纯音乐部分也要标注时间戳
- 确保最后一句有正确的结束标记
- 如果音频很长，可以分段输出，但要保持时间连续性

现在请开始分析音频并生成 LRC：
        """.trimIndent()
    }
    
    /**
     * 解析 Gemini 响应
     */
    private fun parseLrcFromGeminiResponse(response: String): Result<LrcData> {
        return try {
            val json = JsonParser.parseString(response).asJsonObject
            val text = json.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")
                ?.get(0)?.asJsonObject
                ?.get("text")?.asString ?: ""
            
            parseLrcText(text)
        } catch (e: Exception) {
            Result.failure(Exception("解析 Gemini 响应失败: ${e.message}"))
        }
    }
    
    /**
     * 解析 OpenAI 格式响应
     */
    private fun parseLrcFromOpenAIResponse(response: String): Result<LrcData> {
        return try {
            val json = JsonParser.parseString(response).asJsonObject
            val text = json.getAsJsonArray("choices")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("message")
                ?.get("content")?.asString ?: ""
            
            parseLrcText(text)
        } catch (e: Exception) {
            Result.failure(Exception("解析响应失败: ${e.message}"))
        }
    }
    
    /**
     * 解析 LRC 文本
     */
    private fun parseLrcText(text: String): Result<LrcData> {
        val lines = mutableListOf<LrcLine>()
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")
        
        val lrcLines = text.lines().filter { it.trim().startsWith("[") }
        
        for (i in lrcLines.indices) {
            val line = lrcLines[i]
            val match = regex.find(line) ?: continue
            
            val minutes = match.groupValues[1].toLongOrNull() ?: 0
            val seconds = match.groupValues[2].toLongOrNull() ?: 0
            val millis = match.groupValues[3].let {
                if (it.length == 2) it.toLong() * 10 else it.toLong()
            }
            val lyricText = match.groupValues[4].trim()
            
            if (lyricText.isEmpty()) continue
            
            val startTime = minutes * 60000 + seconds * 1000 + millis
            
            // 计算结束时间（下一句的开始时间，或者当前时间 + 5秒）
            val endTime = if (i < lrcLines.size - 1) {
                val nextMatch = regex.find(lrcLines[i + 1])
                if (nextMatch != null) {
                    val nextMinutes = nextMatch.groupValues[1].toLongOrNull() ?: 0
                    val nextSeconds = nextMatch.groupValues[2].toLongOrNull() ?: 0
                    val nextMillis = nextMatch.groupValues[3].let {
                        if (it.length == 2) it.toLong() * 10 else it.toLong()
                    }
                    nextMinutes * 60000 + nextSeconds * 1000 + nextMillis
                } else {
                    startTime + 5000
                }
            } else {
                startTime + 5000
            }
            
            lines.add(LrcLine(
                startTime = startTime,
                endTime = endTime,
                text = lyricText
            ))
        }
        
        return if (lines.isNotEmpty()) {
            Result.success(LrcData(lines = lines))
        } else {
            Result.failure(Exception("未能解析出有效的歌词"))
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
                            contextLength = obj.get("inputTokenLimit")?.asInt ?: -1  // -1 表示未知
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
                        val contextLength = obj.get("context_length")?.asInt ?: -1  // -1 表示未知
                        val pricing = obj.getAsJsonObject("pricing")
                        // 价格可能是数字或字符串类型，需要兼容处理；无数据时使用 -1 表示未知
                        val inputPrice = pricing?.get("prompt")?.let { elem ->
                            if (elem.isJsonPrimitive) {
                                val prim = elem.asJsonPrimitive
                                when {
                                    prim.isNumber -> prim.asDouble * 1_000_000
                                    prim.isString -> prim.asString.toDoubleOrNull()?.times(1_000_000)
                                    else -> null
                                }
                            } else null
                        } ?: -1.0
                        val outputPrice = pricing?.get("completion")?.let { elem ->
                            if (elem.isJsonPrimitive) {
                                val prim = elem.asJsonPrimitive
                                when {
                                    prim.isNumber -> prim.asDouble * 1_000_000
                                    prim.isString -> prim.asString.toDoubleOrNull()?.times(1_000_000)
                                    else -> null
                                }
                            } else null
                        } ?: -1.0
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
                        AiModel(
                            id = modelId,
                            name = modelId,
                            provider = provider,
                            capabilities = inferCapabilities(modelId)
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
     * 模型能力标签 - 仅针对明确可判断的特定能力进行推断
     * 
     * 策略：
     * - 不再推断通用能力（文本、视觉等），因为不可靠
     * - 仅推断模型名称明确表明的特定能力（如 dall-e = 图像生成）
     * - 用户可在保存模型时手动补充或修改能力标签
     */
    private fun inferCapabilities(modelId: String): List<ModelCapability> {
        val id = modelId.lowercase()
        val capabilities = mutableListOf<ModelCapability>()
        
        // 仅推断模型名称明确表明的特定能力
        
        // 图像生成能力（模型名称明确表明）
        if (id.contains("dall-e") || id.contains("imagen") || 
            id.contains("image-generation") || id.contains("gpt-image") ||
            id.contains("flux") || id.contains("stable-diffusion") ||
            id.contains("midjourney")) {
            capabilities.add(ModelCapability.IMAGE_GENERATION)
        }
        
        // 音频/语音能力（模型名称明确表明）
        if (id.contains("whisper") || id.contains("tts") || 
            id.contains("audio") || id.contains("speech") ||
            id.contains("realtime")) {
            capabilities.add(ModelCapability.AUDIO)
        }
        
        // 不再推断：TEXT、IMAGE（视觉）、CODE、FUNCTION_CALL、LONG_CONTEXT
        // 这些能力需要用户手动标记，因为仅靠名称无法可靠判断
        
        return capabilities
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
            val baseUrl = apiKey.baseUrl ?: apiKey.provider.baseUrl
            
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
    
    // ==================== 流式聊天接口 ====================
    
    /**
     * 流式聊天接口
     * @param apiKey API密钥配置
     * @param model 模型
     * @param messages 消息列表
     * @param temperature 温度参数
     * @return 返回Flow<StreamDelta>，包含正文和思考内容增量
     */
    fun chatStream(
        apiKey: ApiKeyConfig,
        model: AiModel,
        messages: List<Map<String, String>>,
        temperature: Float = 0.7f
    ): Flow<StreamDelta> = callbackFlow {
        val baseUrl = apiKey.baseUrl ?: apiKey.provider.baseUrl
        
        val onDelta: (StreamDelta) -> Unit = { delta ->
            trySend(delta)
        }
        
        val onComplete: () -> Unit = {
            trySend(StreamDelta(isComplete = true))
            close()
        }
        
        val onError: (Exception) -> Unit = { e ->
            close(e)
        }
        
        when (apiKey.provider) {
            AiProvider.GOOGLE -> {
                streamWithGemini(baseUrl, apiKey.apiKey, model.id, messages, temperature, onDelta, onComplete, onError)
            }
            AiProvider.ANTHROPIC -> {
                streamWithAnthropic(baseUrl, apiKey.apiKey, model.id, messages, temperature, onDelta, onComplete, onError)
            }
            else -> {
                streamWithOpenAICompatible(baseUrl, apiKey.apiKey, model.id, messages, temperature, apiKey.provider, onDelta, onComplete, onError)
            }
        }
        
        awaitClose { }
    }
    
    /**
     * OpenAI兼容格式流式聊天
     */
    private fun streamWithOpenAICompatible(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float,
        provider: AiProvider,
        onDelta: (StreamDelta) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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
            addProperty("max_tokens", 16384)
            addProperty("stream", true)
        }
        
        val endpoint = when (provider) {
            AiProvider.GLM -> "$baseUrl/v4/chat/completions"
            AiProvider.VOLCANO -> "$baseUrl/v3/chat/completions"
            else -> "$baseUrl/v1/chat/completions"
        }
        
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val eventSourceFactory = EventSources.createFactory(client)
        
        var isCompleted = false  // 防止重复触发完成
        
        eventSourceFactory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    if (!isCompleted) {
                        isCompleted = true
                        onComplete()
                    }
                    return
                }
                
                try {
                    val json = gson.fromJson(data, JsonObject::class.java)
                    val choiceObj = json.getAsJsonArray("choices")?.get(0)?.asJsonObject
                    val deltaObj = choiceObj?.getAsJsonObject("delta")
                    
                    // 检查 finish_reason（某些 API 用这个表示结束）
                    val finishReason = choiceObj?.get("finish_reason")?.takeIf { !it.isJsonNull }?.asString
                    if (finishReason == "stop" || finishReason == "end_turn" || finishReason == "length") {
                        if (!isCompleted) {
                            isCompleted = true
                            onComplete()
                        }
                        return
                    }
                    
                    // 解析正文内容 - 支持多种字段名
                    val content = deltaObj?.get("content")?.takeIf { !it.isJsonNull }?.asString
                        ?: deltaObj?.get("text")?.takeIf { !it.isJsonNull }?.asString
                        // 某些 API 直接在 message 中返回内容
                        ?: choiceObj?.getAsJsonObject("message")?.get("content")?.takeIf { !it.isJsonNull }?.asString
                    
                    // 解析思考内容 - 支持多种格式
                    val reasoning = deltaObj?.get("reasoning_content")?.takeIf { !it.isJsonNull }?.asString
                        ?: deltaObj?.get("reasoning")?.takeIf { !it.isJsonNull }?.asString
                        ?: deltaObj?.get("thinking")?.takeIf { !it.isJsonNull }?.asString
                    
                    if (content != null || reasoning != null) {
                        onDelta(StreamDelta(content = content, reasoning = reasoning))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AiApiClient", "流式解析错误: ${e.message}, data: $data")
                }
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!isCompleted) {
                    isCompleted = true
                    if (t != null) {
                        onError(Exception(t.message ?: "流式请求失败"))
                    } else {
                        onComplete()  // 连接关闭也触发完成
                    }
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                if (!isCompleted) {
                    isCompleted = true
                    onComplete()
                }
            }
        })
    }
    
    /**
     * Gemini流式聊天
     */
    private fun streamWithGemini(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float,
        onDelta: (StreamDelta) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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
                addProperty("maxOutputTokens", 16384)
            })
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1beta/models/$modelId:streamGenerateContent?alt=sse&key=$apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val eventSourceFactory = EventSources.createFactory(client)
        
        var isCompleted = false  // 防止重复触发完成
        
        eventSourceFactory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = gson.fromJson(data, JsonObject::class.java)
                    
                    // 检查是否有错误
                    json.getAsJsonObject("error")?.let { error ->
                        val errorMsg = error.get("message")?.asString ?: "未知错误"
                        android.util.Log.e("AiApiClient", "Gemini API 错误: $errorMsg")
                        if (!isCompleted) {
                            isCompleted = true
                            onError(Exception(errorMsg))
                        }
                        return
                    }
                    
                    // 提取所有 parts 中的文本
                    val candidates = json.getAsJsonArray("candidates")
                    val contentObj = candidates?.get(0)?.asJsonObject?.getAsJsonObject("content")
                    val parts = contentObj?.getAsJsonArray("parts")
                    
                    parts?.forEach { partElement ->
                        val part = partElement.asJsonObject
                        val text = part.get("text")?.takeIf { !it.isJsonNull }?.asString
                        text?.let { onDelta(StreamDelta(content = it)) }
                    }
                    
                    // 检查 finishReason
                    val finishReason = candidates?.get(0)?.asJsonObject?.get("finishReason")?.takeIf { !it.isJsonNull }?.asString
                    if (finishReason == "STOP" || finishReason == "MAX_TOKENS") {
                        if (!isCompleted) {
                            isCompleted = true
                            onComplete()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AiApiClient", "Gemini 流式解析错误: ${e.message}, data: $data")
                }
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!isCompleted) {
                    isCompleted = true
                    if (t != null) {
                        android.util.Log.e("AiApiClient", "Gemini 流式请求失败: ${t.message}")
                        onError(Exception(t.message ?: "流式请求失败"))
                    } else {
                        // 读取响应体中的错误信息
                        val errorBody = response?.body?.string()
                        if (errorBody != null && errorBody.contains("error")) {
                            android.util.Log.e("AiApiClient", "Gemini API 返回错误: $errorBody")
                            onError(Exception("API 错误: $errorBody"))
                        } else {
                            onComplete()
                        }
                    }
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                if (!isCompleted) {
                    isCompleted = true
                    onComplete()
                }
            }
        })
    }
    
    /**
     * Anthropic流式聊天
     */
    private fun streamWithAnthropic(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: List<Map<String, String>>,
        temperature: Float,
        onDelta: (StreamDelta) -> Unit,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
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
            addProperty("max_tokens", 16384)
            addProperty("temperature", temperature)
            addProperty("stream", true)
            systemMessage?.let { addProperty("system", it) }
        }
        
        val request = Request.Builder()
            .url("$baseUrl/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        
        val eventSourceFactory = EventSources.createFactory(client)
        
        var currentBlockType = "text"  // 跟踪当前content block类型
        var isCompleted = false  // 防止重复触发完成
        
        eventSourceFactory.newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val json = gson.fromJson(data, JsonObject::class.java)
                    val eventType = json.get("type")?.asString
                    
                    when (eventType) {
                        "error" -> {
                            val errorObj = json.getAsJsonObject("error")
                            val errorMsg = errorObj?.get("message")?.asString ?: "未知错误"
                            android.util.Log.e("AiApiClient", "Anthropic API 错误: $errorMsg")
                            if (!isCompleted) {
                                isCompleted = true
                                onError(Exception(errorMsg))
                            }
                        }
                        "content_block_start" -> {
                            // 检查是否是thinking block
                            val blockType = json.getAsJsonObject("content_block")
                                ?.get("type")?.asString
                            currentBlockType = blockType ?: "text"
                        }
                        "content_block_delta" -> {
                            val deltaObj = json.getAsJsonObject("delta")
                            val deltaType = deltaObj?.get("type")?.asString
                            
                            when (deltaType) {
                                "thinking_delta" -> {
                                    // Claude thinking内容
                                    val thinking = deltaObj?.get("thinking")?.takeIf { !it.isJsonNull }?.asString
                                    thinking?.let { onDelta(StreamDelta(reasoning = it)) }
                                }
                                "text_delta" -> {
                                    val text = deltaObj?.get("text")?.takeIf { !it.isJsonNull }?.asString
                                    text?.let { onDelta(StreamDelta(content = it)) }
                                }
                            }
                        }
                        "message_delta" -> {
                            // 检查 stop_reason
                            val stopReason = json.getAsJsonObject("delta")
                                ?.get("stop_reason")?.takeIf { !it.isJsonNull }?.asString
                            if (stopReason != null && !isCompleted) {
                                isCompleted = true
                                onComplete()
                            }
                        }
                        "message_stop" -> {
                            if (!isCompleted) {
                                isCompleted = true
                                onComplete()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AiApiClient", "Anthropic 流式解析错误: ${e.message}, data: $data")
                }
            }
            
            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (!isCompleted) {
                    isCompleted = true
                    if (t != null) {
                        onError(Exception(t.message ?: "流式请求失败"))
                    } else {
                        onComplete()
                    }
                }
            }
            
            override fun onClosed(eventSource: EventSource) {
                if (!isCompleted) {
                    isCompleted = true
                    onComplete()
                }
            }
        })
    }
}
