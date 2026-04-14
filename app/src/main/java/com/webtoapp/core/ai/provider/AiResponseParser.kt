package com.webtoapp.core.ai.provider

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.webtoapp.core.ai.extractContentFrom
import com.webtoapp.core.ai.ToolCallData
import com.webtoapp.core.ai.ToolCallResponse

internal class AiResponseParser(
    private val gson: Gson
) {
    fun parseGeminiChatResponse(body: String): Result<String> {
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

    fun parseAnthropicChatResponse(body: String): Result<String> {
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

    fun parseOpenAIChatResponse(body: String): Result<String> {
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val choiceObj = json.getAsJsonArray("choices")?.get(0)?.asJsonObject
            val content = extractContentFrom(choiceObj)

            if (content != null) {
                Result.success(content)
            } else {
                Result.failure(Exception("无法解析响应"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseOpenAIToolResponse(body: String): Result<ToolCallResponse> {
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val choice = json.getAsJsonArray("choices")?.get(0)?.asJsonObject
            val message = choice?.getAsJsonObject("message")

            val textContent = message?.get("content")?.asString ?: ""
            val toolCallsJson = message?.getAsJsonArray("tool_calls")

            val toolCalls = toolCallsJson?.mapNotNull { tc ->
                val tcObj = tc.asJsonObject
                val function = tcObj.getAsJsonObject("function")
                val id = tcObj.get("id")?.asString ?: ""
                val name = function?.get("name")?.asString ?: return@mapNotNull null
                val argsStr = function.get("arguments")?.asString ?: "{}"
                val args = try {
                    @Suppress("UNCHECKED_CAST")
                    gson.fromJson(argsStr, Map::class.java) as Map<String, Any?>
                } catch (_: Exception) {
                    emptyMap()
                }
                ToolCallData(id, name, args)
            } ?: emptyList()

            Result.success(
                ToolCallResponse(
                    textContent = textContent,
                    toolCalls = toolCalls
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseGeminiToolResponse(body: String): Result<ToolCallResponse> {
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val parts = json.getAsJsonArray("candidates")
                ?.get(0)?.asJsonObject
                ?.getAsJsonObject("content")
                ?.getAsJsonArray("parts")

            var textContent = ""
            val toolCalls = mutableListOf<ToolCallData>()

            parts?.forEach { part ->
                val partObj = part.asJsonObject

                partObj.get("text")?.asString?.let {
                    textContent += it
                }

                partObj.getAsJsonObject("functionCall")?.let { fc ->
                    val name = fc.get("name")?.asString ?: return@let
                    @Suppress("UNCHECKED_CAST")
                    val args = fc.getAsJsonObject("args")?.let { argsObj ->
                        gson.fromJson(argsObj, Map::class.java) as Map<String, Any?>
                    } ?: emptyMap()
                    toolCalls.add(
                        ToolCallData(
                            id = java.util.UUID.randomUUID().toString(),
                            name = name,
                            arguments = args
                        )
                    )
                }
            }

            Result.success(
                ToolCallResponse(
                    textContent = textContent,
                    toolCalls = toolCalls
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseAnthropicToolResponse(body: String): Result<ToolCallResponse> {
        return try {
            val json = gson.fromJson(body, JsonObject::class.java)
            val content = json.getAsJsonArray("content")

            var textContent = ""
            val toolCalls = mutableListOf<ToolCallData>()

            content?.forEach { block ->
                val blockObj = block.asJsonObject
                when (blockObj.get("type")?.asString) {
                    "text" -> {
                        textContent += blockObj.get("text")?.asString ?: ""
                    }

                    "tool_use" -> {
                        val id = blockObj.get("id")?.asString ?: ""
                        val name = blockObj.get("name")?.asString ?: ""
                        @Suppress("UNCHECKED_CAST")
                        val input = blockObj.getAsJsonObject("input")?.let { inputObj ->
                            gson.fromJson(inputObj, Map::class.java) as Map<String, Any?>
                        } ?: emptyMap()
                        toolCalls.add(ToolCallData(id, name, input))
                    }
                }
            }

            Result.success(
                ToolCallResponse(
                    textContent = textContent,
                    toolCalls = toolCalls
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
