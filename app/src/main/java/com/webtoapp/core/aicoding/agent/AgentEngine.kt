package com.webtoapp.core.aicoding.agent

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.webtoapp.core.aicoding.llm.ChatRequest
import com.webtoapp.core.aicoding.llm.FinishReason
import com.webtoapp.core.aicoding.llm.LlmEvent
import com.webtoapp.core.aicoding.llm.LlmGateway
import com.webtoapp.core.aicoding.llm.LlmMessage
import com.webtoapp.core.aicoding.llm.LlmToolCall
import com.webtoapp.core.aicoding.llm.ToolDeclaration
import com.webtoapp.core.aicoding.permission.PermissionChecker
import com.webtoapp.core.aicoding.permission.PermissionDecision
import com.webtoapp.core.aicoding.tool.ToolContext
import com.webtoapp.core.aicoding.tool.ToolRegistry
import com.webtoapp.core.aicoding.tool.ToolResult
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class AgentEngine(
    private val gateway: LlmGateway,
    private val permissionChecker: PermissionChecker,
    private val abortController: AbortController = AbortController()
) {

    data class Input(
        val systemPrompt: String,
        val history: List<LlmMessage>,
        val userMessage: String,
        val toolContext: ToolContext,
        val registry: ToolRegistry,
        val temperature: Float = 0.7f,
        val maxTurns: Int = 8,
        val maxTokens: Int = 8192
    )

    fun run(input: Input): Flow<AgentEvent> = channelFlow {
        send(AgentEvent.Started)

        val messages = mutableListOf<LlmMessage>()
        messages += LlmMessage(LlmMessage.Role.SYSTEM, input.systemPrompt)
        messages += input.history

        if (input.userMessage.isNotEmpty()) {
            messages += LlmMessage(LlmMessage.Role.USER, input.userMessage)
        }

        val declarations = input.registry.all.map { tool ->
            ToolDeclaration(tool.name, tool.description, tool.parametersSchema)
        }

        var totalToolCalls = 0
        val accText = StringBuilder()

        try {
            for (turn in 1..input.maxTurns) {
                if (abortController.aborted) { send(AgentEvent.Aborted); return@channelFlow }

                val turnText = StringBuilder()

                val turnThinking = StringBuilder()
                val pending = LinkedHashMap<String, Pair<String, StringBuilder>>()
                var finishReason = FinishReason.STOP
                var hardError: String? = null

                gateway.chatStream(
                    ChatRequest(
                        apiKey = input.toolContext.textApiKey,
                        model = input.toolContext.textModel.model,
                        messages = messages.toList(),
                        tools = declarations,
                        temperature = input.temperature,
                        maxTokens = input.maxTokens,
                        useTools = true
                    )
                ).collect { ev ->
                    when (ev) {
                        is LlmEvent.Started -> Unit
                        is LlmEvent.TextDelta -> {
                            turnText.append(ev.delta)
                            accText.append(ev.delta)
                            send(AgentEvent.TextDelta(ev.delta, accText.toString()))
                        }
                        is LlmEvent.ThinkingDelta -> {
                            turnThinking.append(ev.delta)
                            send(AgentEvent.ThinkingDelta(ev.delta, turnThinking.toString()))
                        }
                        is LlmEvent.ToolCallBegin -> {
                            pending[ev.id] = ev.name to StringBuilder()

                            val marker = "\u2063TC:${ev.id}\u2063"
                            accText.append(marker)
                            send(AgentEvent.TextDelta(marker, accText.toString()))
                            send(AgentEvent.ToolCallStarted(ev.id, ev.name))
                        }
                        is LlmEvent.ToolCallArgsDelta -> {
                            pending[ev.id]?.second?.append(ev.argsDelta)
                            send(AgentEvent.ToolCallArgsDelta(ev.id, ev.argsDelta))
                        }
                        is LlmEvent.ToolCallEnd -> {
                            val entry = pending.getOrPut(ev.id) { ev.name to StringBuilder() }
                            if (ev.argumentsJson.length >= entry.second.length) {
                                entry.second.clear()
                                entry.second.append(ev.argumentsJson)
                            }
                        }
                        is LlmEvent.Done -> finishReason = ev.finishReason
                        is LlmEvent.Error -> if (!ev.recoverable) hardError = ev.message
                                              else send(AgentEvent.Notice(ev.message))
                    }
                }

                if (hardError != null) { send(AgentEvent.Failed(hardError!!)); return@channelFlow }

                if (finishReason == FinishReason.LENGTH) {
                    send(AgentEvent.Notice(Strings.aiCodingOutputTruncated))
                }

                val assistantToolCalls = pending.entries.map { (id, pair) ->
                    LlmToolCall(id, pair.first, pair.second.toString().ifBlank { "{}" })
                }
                messages += LlmMessage(
                    role = LlmMessage.Role.ASSISTANT,
                    content = turnText.toString(),
                    toolCalls = assistantToolCalls,
                    reasoningContent = turnThinking.toString().takeIf { it.isNotEmpty() }
                )

                if (assistantToolCalls.isEmpty() || finishReason != FinishReason.TOOL_CALLS) {
                    send(AgentEvent.Completed(
                        summary = turnText.toString().trim().ifEmpty { accText.toString().trim() },
                        toolCallCount = totalToolCalls
                    ))
                    return@channelFlow
                }

                val batches = batchToolCalls(assistantToolCalls, input.registry)
                val toolMessages = mutableListOf<LlmMessage>()

                for (batch in batches) {
                    if (abortController.aborted) { send(AgentEvent.Aborted); return@channelFlow }
                    if (batch.parallel) {
                        val results = runParallel(batch.calls, input, channel)
                        for ((call, result) in results) {
                            totalToolCalls++
                            emitToolFinish(call, result, channel)
                            toolMessages += LlmMessage(
                                role = LlmMessage.Role.TOOL,
                                content = trimToolText(result.text),
                                toolCallId = call.id,
                                name = call.name
                            )
                        }
                    } else {
                        for (call in batch.calls) {
                            if (abortController.aborted) { send(AgentEvent.Aborted); return@channelFlow }
                            val result = runSequential(call, input, channel)
                            totalToolCalls++
                            emitToolFinish(call, result, channel)
                            toolMessages += LlmMessage(
                                role = LlmMessage.Role.TOOL,
                                content = trimToolText(result.text),
                                toolCallId = call.id,
                                name = call.name
                            )
                        }
                    }
                }

                messages += toolMessages
            }

            send(AgentEvent.Completed(
                summary = accText.toString().trim().ifEmpty { "(reached max turns)" },
                toolCallCount = totalToolCalls
            ))
        } catch (e: AgentAbortedException) {
            send(AgentEvent.Aborted)
        } catch (t: Throwable) {
            AppLogger.e(TAG, "engine crash: ${t.message}", t)
            send(AgentEvent.Failed(t.message ?: "engine error"))
        }
    }

    private suspend fun emitToolFinish(
        call: LlmToolCall,
        result: ToolResult,
        out: SendChannel<AgentEvent>
    ) {
        out.send(AgentEvent.ToolFinished(call.id, call.name, call.argumentsJson, result))
        result.fileChange?.let { out.send(AgentEvent.FileChanged(it)) }
    }

    private suspend fun runSequential(
        call: LlmToolCall,
        input: Input,
        out: SendChannel<AgentEvent>
    ): ToolResult {
        val tool = input.registry[call.name]
            ?: return ToolResult.error("Unknown tool: ${call.name}")
        val args = parseArgs(call.argumentsJson)

        val decision = permissionChecker.check(tool, args, input.toolContext)
        if (decision == PermissionDecision.Deny) {

            val mode = permissionChecker.mode
            val planFile = input.toolContext.activePlanFile
            val hint = when {
                mode == com.webtoapp.core.aicoding.permission.PermissionMode.Plan && planFile != null ->
                    " — in plan mode, only Write/Edit to $planFile is allowed. " +
                        "Write the plan there and call ExitPlanMode."
                mode == com.webtoapp.core.aicoding.permission.PermissionMode.Plan ->
                    " — plan mode forbids this tool. Use Read / Glob / Grep / ListFiles / AskUserQuestion " +
                        "to investigate, then ExitPlanMode."
                mode == com.webtoapp.core.aicoding.permission.PermissionMode.Dream ->
                    " — dream mode only permits writes inside .memory/."
                else -> ""
            }
            return ToolResult.error("Permission denied: ${call.name}$hint")
        }

        val accumulated = StringBuilder()
        val callCtx = input.toolContext.copy(
            progress = { delta: String ->
                if (delta.isNotEmpty()) {
                    accumulated.append(delta)
                    val sent = out.trySend(
                        AgentEvent.ToolProgress(
                            toolCallId = call.id,
                            name = call.name,
                            delta = delta,
                            accumulated = accumulated.toString()
                        )
                    )

                    if (sent.isClosed) Unit
                }
            }
        )

        return runCatching { tool.execute(args, callCtx) }
            .getOrElse { ToolResult.error("${call.name}: ${it.message ?: it::class.simpleName}") }
    }

    private suspend fun runParallel(
        calls: List<LlmToolCall>,
        input: Input,
        out: SendChannel<AgentEvent>
    ): List<Pair<LlmToolCall, ToolResult>> =
        coroutineScope {
            val deferred = calls.map { call ->
                async {
                    call to runSequential(call, input, out)
                }
            }
            deferred.map { it.await() }
        }

    private fun batchToolCalls(calls: List<LlmToolCall>, registry: ToolRegistry): List<Batch> {
        val batches = mutableListOf<Batch>()
        for (call in calls) {
            val readOnly = registry[call.name]?.isReadOnly() == true
            val last = batches.lastOrNull()
            if (readOnly && last != null && last.parallel) {
                last.calls += call
            } else {
                batches += Batch(parallel = readOnly, calls = mutableListOf(call))
            }
        }
        return batches
    }

    private fun parseArgs(json: String): JsonObject = runCatching {
        if (json.isBlank()) JsonObject()
        else {
            val el = JsonParser.parseString(json)
            if (el.isJsonObject) el.asJsonObject else JsonObject()
        }
    }.getOrElse { JsonObject() }

    private fun trimToolText(text: String): String =
        if (text.length <= MAX_TOOL_RESULT_CHARS) text
        else text.substring(0, MAX_TOOL_RESULT_CHARS) + "\n… (tool result truncated)"

    private data class Batch(val parallel: Boolean, val calls: MutableList<LlmToolCall>)

    companion object {
        private const val TAG = "AgentEngine"
        private const val MAX_TOOL_RESULT_CHARS = 32_000
    }
}
