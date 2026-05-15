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

internal class OllamaProvider(@Suppress("UNUSED_PARAMETER") context: Context) : LlmProvider {
    private val gson = GsonProvider.gson; private val client get() = NetworkModule.streamingClient
    override fun supports(provider: AiProvider) = provider == AiProvider.OLLAMA
    override fun chatStream(req: ChatRequest): Flow<LlmEvent> = callbackFlow {
        val url = HttpHelpers.joinUrl(HttpHelpers.baseUrl(req.apiKey), "/api/chat")
        val body = JsonObject().apply {
            addProperty("model",req.model.id); addProperty("stream",true)
            add("messages",JsonArray().apply{req.messages.forEach{m->add(JsonObject().apply{addProperty("role",when(m.role){LlmMessage.Role.SYSTEM->"system";LlmMessage.Role.USER->"user";LlmMessage.Role.ASSISTANT->"assistant";LlmMessage.Role.TOOL->"tool"});addProperty("content",m.content)})}})
            add("options",JsonObject().apply{addProperty("temperature",req.temperature)})
            if(req.useTools&&req.tools.isNotEmpty()) add("tools",JsonArray().apply{req.tools.forEach{t->add(JsonObject().apply{addProperty("type","function");add("function",JsonObject().apply{addProperty("name",t.name);addProperty("description",t.description);add("parameters",t.parametersSchema)})})}})
        }
        val b = Request.Builder().url(url).header("Content-Type","application/json").post(gson.toJson(body).toRequestBody("application/json".toMediaType()))
        HttpHelpers.applyAuth(b, req.apiKey); trySend(LlmEvent.Started)
        val call = client.newCall(b.build())
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { trySend(LlmEvent.Error(e.message?:"网络错误")); close() }
            override fun onResponse(call: Call, response: Response) {
                if(!response.isSuccessful){val eb=runCatching{response.body?.string()}.getOrNull().orEmpty();response.body?.close();val(m,r)=HttpHelpers.classifyHttpError(response.code,eb);trySend(LlmEvent.Error(m,r));close();return}
                try {
                    val source = response.body?.source() ?: run{trySend(LlmEvent.Error("响应体为空"));close();return}
                    var hasTools=false; var done=false
                    while(!source.exhausted()){
                        val line=source.readUtf8Line()?:break; if(line.isBlank()) continue
                        try { val json=JsonParser.parseString(line).asJsonObject
                            json.getAsJsonObject("message")?.let{msg->
                                msg.get("content")?.asString?.takeIf{it.isNotEmpty()}?.let{trySend(LlmEvent.TextDelta(it))}
                                msg.getAsJsonArray("tool_calls")?.forEach{tcEl->val tc=tcEl.asJsonObject;val func=tc.getAsJsonObject("function")?:return@forEach;val nm=func.get("name")?.asString?:return@forEach;val args=func.get("arguments")?.let{gson.toJson(it)}.orEmpty();val id="oll_${UUID.randomUUID()}";hasTools=true;trySend(LlmEvent.ToolCallBegin(id,nm));if(args.isNotEmpty())trySend(LlmEvent.ToolCallArgsDelta(id,args));trySend(LlmEvent.ToolCallEnd(id,nm,args))}
                            }
                            if(json.get("done")?.asBoolean==true&&!done){done=true;trySend(LlmEvent.Done(if(hasTools)FinishReason.TOOL_CALLS else FinishReason.STOP))}
                        }catch(_:Exception){}
                    }
                    if(!done) trySend(LlmEvent.Done(if(hasTools)FinishReason.TOOL_CALLS else FinishReason.STOP))
                } finally { runCatching{response.body?.close()};close() }
            }
        }); awaitClose{call.cancel()}
    }
}
