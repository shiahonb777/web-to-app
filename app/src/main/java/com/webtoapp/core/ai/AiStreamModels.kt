package com.webtoapp.core.ai

import com.google.gson.JsonObject

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

/**
 * Tool Calling 响应
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
 * 流式工具调用事件
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
 * 工具调用信息
 */
data class ToolCallInfo(
    val id: String,
    val name: String,
    val arguments: String
)

/**
 * 从 content 元素中提取文本（支持多种 JSON 格式）
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
        }.ifEmpty { null }  // 改用 ifEmpty 而不是 ifBlank，保留空格
        elem.isJsonObject -> {
            val obj = elem.asJsonObject
            obj.get("text")?.asString ?: obj.get("content")?.asString
        }
        else -> null
    }
}

/**
 * 从流式 choice 中提取 reasoning/thinking 内容
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
 * 从流式 choice 中提取文本内容
 */
internal fun extractContentFrom(choiceObj: JsonObject?): String? {
    if (choiceObj == null) return null
    val delta = choiceObj.getAsJsonObject("delta")
    // 注意：不要使用 isNotBlank()，因为空格也是有效内容（如 CSS 中的空格）
    extractTextFromContentElement(delta?.get("content"))?.let { if (it.isNotEmpty()) return it }
    val message = choiceObj.getAsJsonObject("message")
    extractTextFromContentElement(message?.get("content"))?.let { if (it.isNotEmpty()) return it }
    return null
}
