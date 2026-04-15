package com.webtoapp.core.ai

import com.google.gson.JsonObject

/**
 * Streaming event types
 */
sealed class StreamEvent {
    object Started : StreamEvent()
    data class Thinking(val content: String) : StreamEvent()
    data class Content(val delta: String, val accumulated: String) : StreamEvent()
    data class Done(val fullContent: String) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}

/**
 * Tool calling response
 */
data class ToolCallResponse(
    val textContent: String = "",
    val thinking: String = "",
    val toolCalls: List<ToolCallData> = emptyList()
)

data class ToolCallData(
    val id: String,
    val name: String,
    val arguments: Map<String, Any?>
)

/**
 * Streaming tool invocation events
 */
sealed class ToolStreamEvent {
    object Started : ToolStreamEvent()
    data class TextDelta(val delta: String, val accumulated: String) : ToolStreamEvent()
    data class ThinkingDelta(val delta: String, val accumulated: String) : ToolStreamEvent()
    data class ToolCallStart(val toolName: String, val toolCallId: String) : ToolStreamEvent()
    data class ToolArgumentsDelta(val toolCallId: String, val delta: String, val accumulated: String) : ToolStreamEvent()
    data class ToolCallComplete(val toolCallId: String, val toolName: String, val arguments: String) : ToolStreamEvent()
    data class Done(val textContent: String, val toolCalls: List<ToolCallInfo>) : ToolStreamEvent()
    data class Error(val message: String) : ToolStreamEvent()
}

/**
 * Tool call information
 */
data class ToolCallInfo(
    val id: String,
    val name: String,
    val arguments: String
)

/**
 * Extract text from a content element (supports multiple JSON formats)
 */
internal fun extractTextFromContentElement(elem: com.google.gson.JsonElement?): String? {
    if (elem == null || elem.isJsonNull) return null
    return when {
        elem.isJsonPrimitive -> elem.asJsonPrimitive.asString
        elem.isJsonArray -> buildString {
            elem.asJsonArray.forEach { part ->
                if (part.isJsonObject) {
                    val obj = part.asJsonObject
                    obj.get("text")?.asString?.let { append(it) }
                    obj.get("content")?.asString?.let { append(it) }
                } else if (part.isJsonPrimitive) {
                    append(part.asString)
                }
            }
        }.ifEmpty { null }  // Use ifEmpty instead of ifBlank so spaces are kept
        elem.isJsonObject -> {
            val obj = elem.asJsonObject
            obj.get("text")?.asString ?: obj.get("content")?.asString
        }
        else -> null
    }
}

/**
 * Extract reasoning/thinking content from a streamed choice
 */
internal fun extractReasoningFrom(choiceObj: JsonObject?, deltaObj: JsonObject?): String? {
    val fromDelta = sequenceOf(
        deltaObj?.get("reasoning_content"),
        deltaObj?.get("thinking"),
        deltaObj?.get("reasoning"),
        deltaObj?.get("thought"),
        deltaObj?.get("reasoning_blocks")
    ).mapNotNull { elem ->
        when {
            elem == null || elem.isJsonNull -> null
            elem.isJsonPrimitive -> elem.asString
            else -> extractTextFromContentElement(elem)
        }
    }.firstOrNull()
    if (fromDelta != null) return fromDelta
    return sequenceOf(
        choiceObj?.get("reasoning_content"),
        choiceObj?.get("reasoning")
    ).mapNotNull { elem ->
        when {
            elem == null || elem.isJsonNull -> null
            elem.isJsonPrimitive -> elem.asString
            else -> extractTextFromContentElement(elem)
        }
    }.firstOrNull()
}

/**
 * Extract the text content from a streamed choice
 */
internal fun extractContentFrom(choiceObj: JsonObject?): String? {
    if (choiceObj == null) return null
    val delta = choiceObj.getAsJsonObject("delta")
    // Note: avoid isNotBlank() because whitespace (e.g., CSS) may be meaningful
    extractTextFromContentElement(delta?.get("content"))?.let { if (it.isNotEmpty()) return it }
    val message = choiceObj.getAsJsonObject("message")
    extractTextFromContentElement(message?.get("content"))?.let { if (it.isNotEmpty()) return it }
    return null
}
