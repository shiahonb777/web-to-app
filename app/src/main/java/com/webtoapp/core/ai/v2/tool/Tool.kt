package com.webtoapp.core.ai.v2.tool

import com.google.gson.JsonElement
import com.google.gson.JsonObject

interface Tool {
    val name: String
    val description: String
    val parametersSchema: JsonElement
    suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult
}

data class ToolResult(val text: String, val ok: Boolean = true, val updatedFile: ToolFileChange? = null, val imageBase64: String? = null) {
    companion object {
        fun ok(text: String, change: ToolFileChange? = null, image: String? = null) = ToolResult(text, true, change, image)
        fun error(text: String) = ToolResult(text, false)
    }
}

data class ToolFileChange(val path: String, val kind: Kind, val newContent: String? = null) {
    enum class Kind { WRITE, EDIT, DELETE }
}
