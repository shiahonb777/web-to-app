package com.webtoapp.core.ai.v2.llm

import android.content.Context
import com.google.gson.JsonArray
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
import java.util.UUID

internal class GeminiProvider(@Suppress("UNUSED_PARAMETER") context: Context) : LlmProvider {
    private val gson = GsonProvider.gson; private val sse = SseParser(); private val client get() = NetworkModule.streamingClient
    override fun supports(provider: AiProvider) = provider == AiProvider.GOOGLE
    override fun chatStream(req: ChatRequest): Flow<LlmEvent> = callbackFlow {
        val base = HttpHelpers.baseUrl(req.apiKey); val key = req.apiKey.apiKey.trim()
        val url = "$base/v1beta/models/${req.model.id}:streamGenerateContent?alt=sse&key=$key"
        val body = buildBody(req)
        val request = Request.Builder().url(url).header("Content-Type","application/json").post(gson.toJson(body).toRequestBody("application/json".toMediaType())).build()
        trySend(LlmEvent.Started)
        val call = client.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { trySend(LlmEvent.Error(e.message?:"网络错误")); close() }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { val eb=runCatching{response.body?.string()}.getOrNull().orEmpty(); response.body?.close(); val(m,r)=HttpHelpers.classifyHttpError(response.code,eb); trySend(LlmEvent.Error(m,r)); close(); return }
                try {
                    val source = response.body?.source() ?: run { trySend(LlmEvent.Error("响应体为空")); close(); return }
                    var hasTools = false
                    sse.consume(source) { _, payload ->
                        try {
                            val json = JsonParser.parseString(payload).asJsonObject
                            val cand = json.getAsJsonArray("candidates")?.takeIf{it.size()>0}?.get(0)?.asJsonObject ?: return@consume true
                            cand.getAsJsonObject("content")?.getAsJsonArray("parts")?.forEach { pEl ->
                                val p = pEl.asJsonObject
                                p.get("text")?.takeUnless{it.isJsonNull}?.asString?.takeIf{it.isNotEmpty()}?.let{trySend(LlmEvent.TextDelta(it))}
                                p.getAsJsonObject("functionCall")?.let { fc ->
                                    hasTools = true; val nm=fc.get("name")?.asString.orEmpty(); val args=fc.get("args")?.let{gson.toJson(it)}.orEmpty(); val id="gem_${UUID.randomUUID()}"
                                    trySend(LlmEvent.ToolCallBegin(id,nm)); if(args.isNotEmpty()) trySend(LlmEvent.ToolCallArgsDelta(id,args)); trySend(LlmEvent.ToolCallEnd(id,nm,args))
                                }
                            }
                        } catch(_:Exception){}; true
                    }
                    trySend(LlmEvent.Done(if(hasTools) FinishReason.TOOL_CALLS else FinishReason.STOP))
                } finally { runCatching{response.body?.close()}; close() }
            }
        }); awaitClose { call.cancel() }
    }
    private fun buildBody(req: ChatRequest): JsonObject {
        val sys = req.messages.filter{it.role==LlmMessage.Role.SYSTEM}.joinToString("\n\n"){it.content}
        val contents = JsonArray(); req.messages.filter{it.role!=LlmMessage.Role.SYSTEM}.forEach { m ->
            contents.add(JsonObject().apply { addProperty("role",if(m.role==LlmMessage.Role.ASSISTANT)"model" else "user"); add("parts",buildParts(m)) })
        }
        return JsonObject().apply {
            add("contents",contents)
            if(sys.isNotBlank()) add("systemInstruction",JsonObject().apply{add("parts",JsonArray().apply{add(JsonObject().apply{addProperty("text",sys)})})})
            add("generationConfig",JsonObject().apply{addProperty("temperature",req.temperature);addProperty("maxOutputTokens",req.maxTokens)})
            if(req.useTools&&req.tools.isNotEmpty()) add("tools",JsonArray().apply{add(JsonObject().apply{add("functionDeclarations",JsonArray().apply{req.tools.forEach{t->add(JsonObject().apply{addProperty("name",t.name);addProperty("description",t.description);add("parameters",t.parametersSchema)})}})})})
        }
    }
    private fun buildParts(m: LlmMessage): JsonArray { val parts=JsonArray()
        when(m.role){LlmMessage.Role.USER->{if(m.content.isNotEmpty())parts.add(JsonObject().apply{addProperty("text",m.content)})}
        LlmMessage.Role.ASSISTANT->{if(m.content.isNotEmpty())parts.add(JsonObject().apply{addProperty("text",m.content)});m.toolCalls.forEach{tc->parts.add(JsonObject().apply{add("functionCall",JsonObject().apply{addProperty("name",tc.name);add("args",runCatching{JsonParser.parseString(tc.argumentsJson)}.getOrElse{JsonObject()})})})}}
        LlmMessage.Role.TOOL->{parts.add(JsonObject().apply{add("functionResponse",JsonObject().apply{addProperty("name",m.name?:"tool");add("response",JsonObject().apply{addProperty("result",m.content)})})})}
        else->{}}; return parts
    }
}
