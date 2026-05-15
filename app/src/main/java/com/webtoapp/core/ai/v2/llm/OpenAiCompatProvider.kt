package com.webtoapp.core.ai.v2.llm

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.network.NetworkModule
import com.webtoapp.data.model.AiProvider
import com.webtoapp.util.GsonProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

internal class OpenAiCompatProvider(@Suppress("UNUSED_PARAMETER") context: Context) : LlmProvider {
    private val gson = GsonProvider.gson
    private val sse = SseParser()
    private val client get() = NetworkModule.streamingClient

    override fun supports(provider: AiProvider) = provider != AiProvider.ANTHROPIC && provider != AiProvider.GOOGLE && provider != AiProvider.OLLAMA

    override fun chatStream(req: ChatRequest): Flow<LlmEvent> = callbackFlow {
        val url = HttpHelpers.joinUrl(HttpHelpers.baseUrl(req.apiKey), req.apiKey.getEffectiveChatEndpoint())
        val body = buildBody(req)
        val builder = Request.Builder().url(url).header("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
        HttpHelpers.applyAuth(builder, req.apiKey)
        trySend(LlmEvent.Started)
        val call = client.newCall(builder.build())
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { trySend(LlmEvent.Error(e.message ?: "网络错误")); close() }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    val eb = runCatching { response.body?.string() }.getOrNull().orEmpty()
                    response.body?.close()
                    val (msg, rec) = HttpHelpers.classifyHttpError(response.code, eb)
                    trySend(LlmEvent.Error(msg, rec)); close(); return
                }
                try {
                    val source = response.body?.source() ?: run { trySend(LlmEvent.Error("响应体为空")); close(); return }
                    val toolArgsAccum = LinkedHashMap<String, StringBuilder>()
                    val toolNames = LinkedHashMap<String, String>()
                    val toolIndexToId = HashMap<Int, String>()
                    var finishReason = FinishReason.STOP
                    var done = false
                    sse.consume(source) { _, payload ->
                        if (payload == "[DONE]") { done = true; emitFinish(toolNames, toolArgsAccum, finishReason); return@consume false }
                        try {
                            val json = JsonParser.parseString(payload).asJsonObject
                            json.getAsJsonObject("error")?.let { trySend(LlmEvent.Error(it.get("message")?.asString ?: "API error")); return@consume false }
                            val choice = json.getAsJsonArray("choices")?.takeIf { it.size() > 0 }?.get(0)?.asJsonObject ?: return@consume true
                            val delta = choice.getAsJsonObject("delta") ?: choice.getAsJsonObject("message") ?: return@consume true
                            // Reasoning
                            sequenceOf("reasoning_content","thinking","reasoning").mapNotNull { delta.get(it) }.firstOrNull()?.let { el ->
                                if (!el.isJsonNull && el.isJsonPrimitive) el.asString.takeIf { it.isNotEmpty() }?.let { trySend(LlmEvent.ThinkingDelta(it)) }
                            }
                            // Content
                            delta.get("content")?.takeUnless { it.isJsonNull }?.let { el ->
                                if (el.isJsonPrimitive) el.asString.takeIf { it.isNotEmpty() }?.let { trySend(LlmEvent.TextDelta(it)) }
                            }
                            // Tool calls
                            delta.getAsJsonArray("tool_calls")?.forEach { tcEl ->
                                val tc = tcEl.asJsonObject; val idx = tc.get("index")?.asInt ?: 0
                                val id = tc.get("id")?.asString ?: toolIndexToId[idx] ?: "call_$idx"
                                toolIndexToId[idx] = id
                                val func = tc.getAsJsonObject("function")
                                val name = func?.get("name")?.asString
                                val args = func?.get("arguments")?.asString.orEmpty()
                                if (name != null && toolNames[id] == null) { toolNames[id] = name; trySend(LlmEvent.ToolCallBegin(id, name)) }
                                if (args.isNotEmpty()) { toolArgsAccum.getOrPut(id) { StringBuilder() }.append(args); trySend(LlmEvent.ToolCallArgsDelta(id, args)) }
                            }
                            choice.get("finish_reason")?.takeUnless { it.isJsonNull }?.asString?.let { r ->
                                finishReason = when (r) { "tool_calls","function_call" -> FinishReason.TOOL_CALLS; "length" -> FinishReason.LENGTH; else -> FinishReason.STOP }
                            }
                        } catch (_: Exception) {}
                        true
                    }
                    if (!done) emitFinish(toolNames, toolArgsAccum, finishReason)
                } finally { runCatching { response.body?.close() }; close() }
            }
        })
        awaitClose { call.cancel() }
    }

    private fun kotlinx.coroutines.channels.ProducerScope<LlmEvent>.emitFinish(names: Map<String, String>, args: Map<String, StringBuilder>, fr: FinishReason) {
        names.forEach { (id, name) -> trySend(LlmEvent.ToolCallEnd(id, name, args[id]?.toString().orEmpty())) }
        trySend(LlmEvent.Done(if (names.isNotEmpty() && fr == FinishReason.STOP) FinishReason.TOOL_CALLS else fr))
    }

    private fun buildBody(req: ChatRequest) = JsonObject().apply {
        addProperty("model", req.model.id); addProperty("stream", true); addProperty("temperature", req.temperature); addProperty("max_tokens", req.maxTokens)
        add("messages", JsonArray().apply { req.messages.forEach { msg -> add(buildMsg(msg)) } })
        if (req.useTools && req.tools.isNotEmpty()) add("tools", JsonArray().apply { req.tools.forEach { t -> add(buildTool(t)) } })
    }
    private fun buildMsg(msg: LlmMessage) = JsonObject().apply {
        addProperty("role", when(msg.role){LlmMessage.Role.SYSTEM->"system";LlmMessage.Role.USER->"user";LlmMessage.Role.ASSISTANT->"assistant";LlmMessage.Role.TOOL->"tool"})
        if (msg.toolCalls.isNotEmpty()) { add("tool_calls", JsonArray().apply { msg.toolCalls.forEach { tc -> add(JsonObject().apply { addProperty("id",tc.id);addProperty("type","function");add("function",JsonObject().apply{addProperty("name",tc.name);addProperty("arguments",tc.argumentsJson)}) }) } }); if(msg.content.isNotEmpty()) addProperty("content",msg.content) }
        else addProperty("content", msg.content)
        msg.toolCallId?.let { addProperty("tool_call_id", it) }; msg.name?.let { addProperty("name", it) }
    }
    private fun buildTool(t: ToolDeclaration) = JsonObject().apply { addProperty("type","function"); add("function", JsonObject().apply { addProperty("name",t.name);addProperty("description",t.description);add("parameters",t.parametersSchema) }) }
}
