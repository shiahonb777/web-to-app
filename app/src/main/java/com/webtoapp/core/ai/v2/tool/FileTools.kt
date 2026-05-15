package com.webtoapp.core.ai.v2.tool

import com.google.gson.JsonElement
import com.google.gson.JsonObject

class WriteFileTool : Tool {
    override val name = "write_file"
    override val description = "Create or overwrite a file. Provide relative `path` and full `content`."
    override val parametersSchema: JsonElement = schema { property("path","string","Relative file path",required=true); property("content","string","Full file contents",required=true) }
    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val path = args.get("path")?.asString?.let{ctx.resolveSafePath(it)} ?: return ToolResult.error("write_file: invalid path")
        val content = args.get("content")?.asString ?: return ToolResult.error("write_file: missing content")
        if(content.isBlank()) return ToolResult.error("write_file: content is empty")
        val info = ctx.fileManager.createFile(ctx.sessionId, path, content, createNewVersion = false)
        return ToolResult.ok("Wrote ${info.name} (${content.length} chars)", ToolFileChange(info.name, ToolFileChange.Kind.WRITE, content))
    }
}

class EditFileTool : Tool {
    override val name = "edit_file"
    override val description = "Edit a region of an existing file. Specify `path`, `target` snippet, `operation` (replace|insert_before|insert_after|delete), and `content`."
    override val parametersSchema: JsonElement = schema { property("path","string","File to edit",required=true); enumProperty("operation",listOf("replace","insert_before","insert_after","delete"),"Edit operation",required=true); property("target","string","Existing snippet to anchor on",required=true); property("content","string","New content") }
    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val path = args.get("path")?.asString?.let{ctx.resolveSafePath(it)} ?: return ToolResult.error("edit_file: invalid path")
        val op = args.get("operation")?.asString ?: return ToolResult.error("edit_file: missing operation")
        val target = args.get("target")?.asString ?: return ToolResult.error("edit_file: missing target")
        val content = args.get("content")?.asString.orEmpty()
        val current = ctx.fileManager.readFile(ctx.sessionId, path) ?: return ToolResult.error("edit_file: $path not found")
        val idx = current.indexOf(target)
        if(idx < 0) {
            // Whitespace-tolerant fallback
            val normHay = current.replace(Regex("\\s+")," "); val normTarget = target.replace(Regex("\\s+")," ")
            val normIdx = normHay.indexOf(normTarget)
            if(normIdx < 0) return ToolResult.error("edit_file: target not found. Use read_file to check current contents.")
            // Can't map back precisely with collapsed whitespace; just do full-text replace on normalized
            val newContent = when(op){"replace"->current.replaceFirst(Regex("(?s)"+Regex.escape(target).replace("\\s+","\\\\s+")),content);"delete"->current.replaceFirst(Regex("(?s)"+Regex.escape(target).replace("\\s+","\\\\s+")),"");else->return ToolResult.error("edit_file: fuzzy match only supports replace/delete")}
            val info = ctx.fileManager.createFile(ctx.sessionId, path, newContent, createNewVersion = false)
            return ToolResult.ok("Edited ${info.name} (fuzzy)", ToolFileChange(info.name, ToolFileChange.Kind.EDIT, newContent))
        }
        val newContent = when(op){
            "replace"->{if(content.isEmpty()) return ToolResult.error("edit_file: replace needs content"); current.substring(0,idx)+content+current.substring(idx+target.length)}
            "insert_before"->{if(content.isEmpty()) return ToolResult.error("edit_file: insert_before needs content"); current.substring(0,idx)+content+current.substring(idx)}
            "insert_after"->{if(content.isEmpty()) return ToolResult.error("edit_file: insert_after needs content"); current.substring(0,idx+target.length)+content+current.substring(idx+target.length)}
            "delete"->current.substring(0,idx)+current.substring(idx+target.length)
            else->return ToolResult.error("edit_file: unknown operation $op")
        }
        val info = ctx.fileManager.createFile(ctx.sessionId, path, newContent, createNewVersion = false)
        return ToolResult.ok("Edited ${info.name}", ToolFileChange(info.name, ToolFileChange.Kind.EDIT, newContent))
    }
}

class ReadFileTool : Tool {
    override val name = "read_file"
    override val description = "Read a file's contents. Optionally include line numbers."
    override val parametersSchema: JsonElement = schema { property("path","string","File path",required=true); booleanProperty("with_line_numbers","Prefix lines with numbers") }
    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val path = args.get("path")?.asString?.let{ctx.resolveSafePath(it)} ?: return ToolResult.error("read_file: invalid path")
        val text = ctx.fileManager.readFile(ctx.sessionId, path) ?: return ToolResult.error("read_file: $path not found")
        val withLines = args.get("with_line_numbers")?.asBoolean ?: false
        val out = if(withLines) text.lines().mapIndexed{i,l->"${i+1}| $l"}.joinToString("\n") else text
        return ToolResult.ok(out)
    }
}

class ListFilesTool : Tool {
    override val name = "list_files"
    override val description = "List all files in the project."
    override val parametersSchema: JsonElement = schema {}
    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val files = ctx.fileManager.listFiles(ctx.sessionId)
        if(files.isEmpty()) return ToolResult.ok("(no files yet)")
        return ToolResult.ok(files.joinToString("\n"){"${it.name}  ${it.size}B"})
    }
}

class DeleteFileTool : Tool {
    override val name = "delete_file"
    override val description = "Delete a file from the project."
    override val parametersSchema: JsonElement = schema { property("path","string","File path",required=true) }
    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val path = args.get("path")?.asString?.let{ctx.resolveSafePath(it)} ?: return ToolResult.error("delete_file: invalid path")
        val ok = ctx.fileManager.deleteFile(ctx.sessionId, path)
        return if(ok) ToolResult.ok("Deleted $path", ToolFileChange(path, ToolFileChange.Kind.DELETE)) else ToolResult.error("delete_file: $path not found")
    }
}

class SyntaxCheckTool : Tool {
    override val name = "check_syntax"
    override val description = "Check a file for syntax issues."
    override val parametersSchema: JsonElement = schema { property("path","string","File to check",required=true) }
    override suspend fun execute(args: JsonObject, ctx: ToolContext): ToolResult {
        val path = args.get("path")?.asString?.let{ctx.resolveSafePath(it)} ?: return ToolResult.error("check_syntax: invalid path")
        val text = ctx.fileManager.readFile(ctx.sessionId, path) ?: return ToolResult.error("check_syntax: $path not found")
        val ext = path.substringAfterLast('.').lowercase()
        val problems = when(ext){"json"->checkJson(text);"js","ts","jsx","tsx"->checkBrackets(text);else->checkBrackets(text)}
        return if(problems.isEmpty()) ToolResult.ok("OK (${text.lines().size} lines)") else ToolResult.ok("${problems.size} issue(s):\n"+problems.joinToString("\n"){"- $it"})
    }
    private fun checkJson(t:String):List<String> = try{com.google.gson.JsonParser.parseString(t);emptyList()}catch(e:Exception){listOf("JSON: ${e.message}")}
    private fun checkBrackets(t:String):List<String>{var b=0;var p=0;var k=0;t.forEach{c->when(c){'{'->b++;'}'->b--;'('->p++;')'->p--;'['->k++;']'->k--}};val r=mutableListOf<String>();if(b!=0)r.add("Brace imbalance: $b");if(p!=0)r.add("Paren imbalance: $p");if(k!=0)r.add("Bracket imbalance: $k");return r}
}
