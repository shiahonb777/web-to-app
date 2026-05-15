package com.webtoapp.core.ai.v2.agent

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.coding.ProjectFileManager
import com.webtoapp.core.ai.v2.data.CapabilityCache
import com.webtoapp.core.ai.v2.llm.*
import com.webtoapp.core.ai.v2.prompt.FallbackParser
import com.webtoapp.core.ai.v2.prompt.ProjectFileSummary
import com.webtoapp.core.ai.v2.prompt.PromptBuilder
import com.webtoapp.core.ai.v2.tool.*
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AgentRunner(private val gateway: LlmGateway, private val capabilityCache: CapabilityCache) {
    data class Input(val history: List<LlmMessage>, val userRequirement: String, val codingType: AiCodingType, val language: AppLanguage, val userRules: List<String>, val toolContext: ToolContext, val registry: ToolRegistry, val temperature: Float = 0.7f, val maxTurns: Int = 6)

    fun run(input: Input): Flow<AgentEvent> = flow {
        emit(AgentEvent.Started)
        val provider = input.toolContext.apiKey.provider.name; val modelId = input.toolContext.textModel.model.id
        val support = capabilityCache.getToolSupport(provider, modelId)
        if (support != CapabilityCache.ToolSupport.UNSUPPORTED) {
            val outcome = runToolMode(input)
            when (outcome) {
                is Outcome.Success -> { if(support==CapabilityCache.ToolSupport.UNKNOWN) capabilityCache.recordToolSupport(provider,modelId,CapabilityCache.ToolSupport.SUPPORTED); emit(AgentEvent.Completed(outcome.summary, outcome.toolCount)); return@flow }
                is Outcome.Recoverable -> { capabilityCache.recordToolSupport(provider,modelId,CapabilityCache.ToolSupport.UNSUPPORTED); emit(AgentEvent.FallbackTriggered) }
                is Outcome.Hard -> { emit(AgentEvent.Failed(outcome.reason)); return@flow }
            }
        } else { emit(AgentEvent.FallbackTriggered) }
        when (val o = runFallbackMode(input)) { is Outcome.Success -> emit(AgentEvent.Completed(o.summary, o.toolCount)); is Outcome.Hard -> emit(AgentEvent.Failed(o.reason)); is Outcome.Recoverable -> emit(AgentEvent.Failed(o.reason)) }
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<AgentEvent>.runToolMode(input: Input): Outcome {
        val tools = input.registry.all; val decls = tools.map { ToolDeclaration(it.name, it.description, it.parametersSchema) }
        val sysPrompt = PromptBuilder.buildSystemPrompt(input.codingType, input.language, tools, true, input.userRules, summarize(input.toolContext.fileManager, input.toolContext.sessionId))
        val msgs = mutableListOf<LlmMessage>(LlmMessage(LlmMessage.Role.SYSTEM, sysPrompt)); msgs.addAll(input.history); msgs.add(LlmMessage(LlmMessage.Role.USER, input.userRequirement))
        val accText = StringBuilder(); var totalTools = 0
        for (turn in 1..input.maxTurns) {
            val turnText = StringBuilder(); val pending = LinkedHashMap<String, Pair<String, StringBuilder>>(); var fr = FinishReason.STOP; var hardErr: String? = null; var recoverable = false
            gateway.chatStream(ChatRequest(input.toolContext.apiKey, input.toolContext.textModel.model, msgs.toList(), decls, input.temperature, useTools = true)).collect { ev ->
                when (ev) {
                    is LlmEvent.Started -> Unit
                    is LlmEvent.TextDelta -> { turnText.append(ev.delta); accText.append(ev.delta); emit(AgentEvent.TextDelta(ev.delta, accText.toString())) }
                    is LlmEvent.ThinkingDelta -> emit(AgentEvent.ThinkingDelta(ev.delta, ev.delta))
                    is LlmEvent.ToolCallBegin -> { pending[ev.id] = ev.name to StringBuilder(); emit(AgentEvent.ToolStarted(ev.id, ev.name)) }
                    is LlmEvent.ToolCallArgsDelta -> { pending[ev.id]?.second?.append(ev.argsDelta); emit(AgentEvent.ToolArgsDelta(ev.id, ev.argsDelta)) }
                    is LlmEvent.ToolCallEnd -> { val entry = pending.getOrPut(ev.id){ev.name to StringBuilder()}; if(ev.argumentsJson.length >= entry.second.length){entry.second.clear();entry.second.append(ev.argumentsJson)} }
                    is LlmEvent.Done -> fr = ev.finishReason
                    is LlmEvent.Error -> { if(ev.recoverable) recoverable=true else hardErr=ev.message }
                }
            }
            if(recoverable) return Outcome.Recoverable("model rejected tools")
            if(hardErr!=null) return Outcome.Hard(hardErr)
            val assistantCalls = pending.entries.map { (id, pair) -> LlmToolCall(id, pair.first, pair.second.toString().ifBlank { "{}" }) }
            msgs.add(LlmMessage(LlmMessage.Role.ASSISTANT, turnText.toString(), assistantCalls))
            if(assistantCalls.isEmpty() || fr != FinishReason.TOOL_CALLS) return Outcome.Success(turnText.toString().trim().ifEmpty{accText.toString().trim()}, totalTools)
            for(call in assistantCalls) {
                totalTools++; val tool = input.registry.get(call.name)
                val result: ToolResult = if(tool==null) ToolResult.error("Unknown tool: ${call.name}") else runCatching{tool.execute(parseArgs(call.argumentsJson), input.toolContext)}.getOrElse{e->ToolResult.error("${call.name}: ${e.message}")}
                emit(AgentEvent.ToolFinished(call.id, call.name, call.argumentsJson, result))
                result.updatedFile?.let { emit(AgentEvent.FileChanged(it)) }
                msgs.add(LlmMessage(LlmMessage.Role.TOOL, result.text.take(10000), toolCallId = call.id, name = call.name))
            }
        }
        return Outcome.Success(accText.toString().trim().ifEmpty{"(max turns reached)"}, totalTools)
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<AgentEvent>.runFallbackMode(input: Input): Outcome {
        val tools = input.registry.all
        val sysPrompt = PromptBuilder.buildSystemPrompt(input.codingType, input.language, tools, false, input.userRules, summarize(input.toolContext.fileManager, input.toolContext.sessionId))
        val userMsg = PromptBuilder.buildFallbackUserMessage(input.codingType, input.language, input.userRequirement)
        val msgs = buildList { add(LlmMessage(LlmMessage.Role.SYSTEM, sysPrompt)); addAll(input.history); add(LlmMessage(LlmMessage.Role.USER, userMsg)) }
        val collected = StringBuilder(); var err: String? = null
        gateway.chatStream(ChatRequest(input.toolContext.apiKey, input.toolContext.textModel.model, msgs, emptyList(), input.temperature, useTools = false)).collect { ev ->
            when(ev){ is LlmEvent.TextDelta->{collected.append(ev.delta);emit(AgentEvent.TextDelta(ev.delta,collected.toString()))}; is LlmEvent.ThinkingDelta->emit(AgentEvent.ThinkingDelta(ev.delta,ev.delta)); is LlmEvent.Error->{err=ev.message}; else->Unit }
        }
        if(err!=null) return Outcome.Hard(err!!)
        val text = collected.toString(); val files = FallbackParser.parse(text, input.codingType)
        if(files.isEmpty()&&text.isBlank()) return Outcome.Hard("model returned empty response")
        var wc = 0; files.forEach { f -> val safe=input.toolContext.resolveSafePath(f.path)?:return@forEach; val info=input.toolContext.fileManager.createFile(input.toolContext.sessionId,safe,f.content,false); wc++; emit(AgentEvent.FileChanged(ToolFileChange(info.name,ToolFileChange.Kind.WRITE,f.content))) }
        return Outcome.Success(if(wc>0)"Wrote $wc file(s)" else text.trim(), wc)
    }

    private fun summarize(fm: ProjectFileManager, sid: String) = fm.listFiles(sid).map { val c=fm.readFile(sid,it.name)?:""; ProjectFileSummary(it.name,c.lines().size,c.length) }
    private fun parseArgs(json: String): JsonObject = runCatching{if(json.isBlank())JsonObject() else JsonParser.parseString(json).let{if(it.isJsonObject)it.asJsonObject else JsonObject()}}.getOrElse{JsonObject()}
    private sealed class Outcome { data class Success(val summary:String,val toolCount:Int):Outcome(); data class Recoverable(val reason:String):Outcome(); data class Hard(val reason:String):Outcome() }
    companion object { private const val TAG = "AgentRunner" }
}
