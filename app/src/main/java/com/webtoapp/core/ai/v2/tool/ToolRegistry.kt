package com.webtoapp.core.ai.v2.tool

import com.webtoapp.core.ai.coding.AiCodingType

class ToolRegistry(private val tools: List<Tool>) {
    val byName: Map<String, Tool> = tools.associateBy { it.name }
    val all: List<Tool> = tools
    fun get(name: String): Tool? = byName[name]
    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun defaultFor(codingType: AiCodingType, hasImageModel: Boolean): ToolRegistry {
            return ToolRegistry(buildList {
                add(WriteFileTool()); add(EditFileTool()); add(ReadFileTool()); add(ListFilesTool()); add(DeleteFileTool()); add(SyntaxCheckTool())
            })
        }
    }
}
