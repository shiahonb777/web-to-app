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

internal class AnthropicProvider(@Suppress("UNUSED_PARAMETER") context: Context) : LlmProvider {
    private val gson = GsonProvider.gson; private val sse = SseParser(); private val client get() = NetworkModule.streamingClient
    override fun supports(provider: AiProvider) = provider == AiProvider.ANTHROPIC
    override fun chatStream(req: ChatRequest): Flow<LlmEvent> = callbackFlow {
        val url = HttpHelpers.joinUrl(HttpHelpers.baseUrl(req.apiKey), "/v1/messages")
        val body = buildBody(req)
        val b = Request.Builder().url(url).header("Content-Type","application/json").post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
        HttpHelpers.applyAuth(b, req.apiKey); trySend(LlmEvent.Started)
        val call = client.newCall(b.build())
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { trySend(LlmEvent.Error(e.message?:"网络错误")); close() }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) { val eb=runCatching{response.body?.string()}.getOrNull().orEmpty(); response.body?.close(); val(m,r)=HttpHelpers.classifyHttpError(response.code,eb); trySend(LlmEvent.Error(m,r)); close(); return }
                try {
                    val source = response.body?.source() ?: run { trySend(LlmEvent.Error("响应体为空")); close(); return }
                    val blockId = HashMap<Int,String>(); val blockName = HashMap<Int,String>(); val toolArgs = LinkedHashMap<String,StringBuilder>()
                    var stopReason: String? = null; var finished = false
                    sse.consume(source) { _, payload ->
                        try {
                            val json = JsonParser.parseString(payload).asJsonObject
                            when (json.get("type")?.asString) {
                                "content_block_start" -> { val idx=json.get("index")?.asInt?:0; val cb=json.getAsJsonObject("content_block")?:return@consume true
                                    if(cb.get("type")?.asString=="tool_use"){val id=cb.get("id")?.asString?:"t_$idx";val nm=cb.get("name")?.asString.orEmpty();blockId[idx]=id;blockName[idx]=nm;toolArgs[id]=StringBuilder();trySend(LlmEvent.ToolCallBegin(id,nm))}}
                                "content_block_delta" -> { val idx=json.get("index")?.asInt?:0; val d=json.getAsJsonObject("delta")?:return@consume true
                                    when(d.get("type")?.asString){"text_delta"->d.get("text")?.asString?.takeIf{it.isNotEmpty()}?.let{trySend(LlmEvent.TextDelta(it))}
                                    "thinking_delta"->d.get("thinking")?.asString?.takeIf{it.isNotEmpty()}?.let{trySend(LlmEvent.ThinkingDelta(it))}
                                    "input_json_delta"->{val p=d.get("partial_json")?.asString.orEmpty();if(p.isNotEmpty()){val id=blockId[idx]?:return@consume true;toolArgs.getOrPut(id){StringBuilder()}.append(p);trySend(LlmEvent.ToolCallArgsDelta(id,p))}}}}
                                "content_block_stop" -> { val idx=json.get("index")?.asInt?:0; val id=blockId[idx]; val nm=blockName[idx]; if(id!=null&&nm!=null) trySend(LlmEvent.ToolCallEnd(id,nm,toolArgs[id]?.toString().orEmpty())) }
                                "message_delta" -> { json.getAsJsonObject("delta")?.get("stop_reason")?.takeUnless{it.isJsonNull}?.asString?.let{stopReason=it} }
                                "message_stop" -> { finished=true; val fr=when(stopReason){"tool_use"->FinishReason.TOOL_CALLS;"max_tokens"->FinishReason.LENGTH;else->FinishReason.STOP}; trySend(LlmEvent.Done(fr)); return@consume false }
                                "error" -> { trySend(LlmEvent.Error(json.getAsJsonObject("error")?.get("message")?.asString?:"Anthropic error")); return@consume false }
                            }
                        } catch(_:Exception){}; true
                    }
                    if(!finished) trySend(LlmEvent.Done(FinishReason.STOP))
                } finally { runCatching{response.body?.close()}; close() }
            }
        }); awaitClose { call.cancel() }
    }
    private fun buildBody(req: ChatRequest): JsonObject { val sys=req.messages.filter{it.role==LlmMessage.Role.SYSTEM}.joinToString("\n\n"){it.content}; val msgs=req.messages.filter{it.role!=LlmMessage.Role.SYSTEM}
        return JsonObject().apply { addProperty("model",req.model.id);addProperty("max_tokens",req.maxTokens);addProperty("temperature",req.temperature);addProperty("stream",true)
            if(sys.isNotBlank()) addProperty("system",sys); add("messages",JsonArray().apply{msgs.forEach{m->add(buildMsg(m))}})
            if(req.useTools&&req.tools.isNotEmpty()) add("tools",JsonArray().apply{req.tools.forEach{t->add(JsonObject().apply{addProperty("name",t.name);addProperty("description",t.description);add("input_schema",t.parametersSchema)})}})
        }
    }
    private fun buildMsg(m: LlmMessage): JsonObject = when(m.role) {
        LlmMessage.Role.USER -> JsonObject().apply{addProperty("role","user");addProperty("content",m.content)}
        LlmMessage.Role.ASSISTANT -> JsonObject().apply{addProperty("role","assistant");val c=JsonArray();if(m.content.isNotEmpty())c.add(JsonObject().apply{addProperty("type","text");addProperty("text",m.content)});m.toolCalls.forEach{tc->c.add(JsonObject().apply{addProperty("type","tool_use");addProperty("id",tc.id);addProperty("name",tc.name);add("input",runCatching{JsonParser.parseString(tc.argumentsJson)}.getOrElse{JsonObject()})})};add("content",c)}
        LlmMessage.Role.TOOL -> JsonObject().apply{addProperty("role","user");add("content",JsonArray().apply{add(JsonObject().apply{addProperty("type","tool_result");addProperty("tool_use_id",m.toolCallId.orEmpty());addProperty("content",m.content)})})}
        else -> JsonObject()
    }
}
