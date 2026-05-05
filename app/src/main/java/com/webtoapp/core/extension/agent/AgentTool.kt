package com.webtoapp.core.extension.agent

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.i18n.Strings










enum class AgentToolType(val icon: String) {
    SYNTAX_CHECK("search"),
    LINT_CODE("straighten"),
    SECURITY_SCAN("lock"),
    GENERATE_CODE("auto_awesome"),
    REFACTOR_CODE("wrench"),
    FIX_ERROR("build"),
    TEST_MODULE("science"),
    VALIDATE_CONFIG("check"),
    GET_TEMPLATES("clipboard"),
    GET_SNIPPETS("package"),
    SEARCH_DOCS("book"),
    CREATE_MODULE("add_circle"),
    UPDATE_MODULE("edit_note"),
    PREVIEW_MODULE("visibility");

    val displayName: String get() = when (this) {
        SYNTAX_CHECK -> Strings.toolTypeSyntaxCheck
        LINT_CODE -> Strings.toolTypeLintCode
        SECURITY_SCAN -> Strings.toolTypeSecurityScan
        GENERATE_CODE -> Strings.toolTypeGenerateCode
        REFACTOR_CODE -> Strings.toolTypeRefactorCode
        FIX_ERROR -> Strings.toolTypeFixError
        TEST_MODULE -> Strings.toolTypeTestModule
        VALIDATE_CONFIG -> Strings.toolTypeValidateConfig
        GET_TEMPLATES -> Strings.toolTypeGetTemplates
        GET_SNIPPETS -> Strings.toolTypeGetSnippets
        SEARCH_DOCS -> Strings.toolTypeSearchDocs
        CREATE_MODULE -> Strings.toolTypeCreateModule
        UPDATE_MODULE -> Strings.toolTypeUpdateModule
        PREVIEW_MODULE -> Strings.toolTypePreviewModule
    }

    val description: String get() = when (this) {
        SYNTAX_CHECK -> Strings.toolTypeSyntaxCheckDesc
        LINT_CODE -> Strings.toolTypeLintCodeDesc
        SECURITY_SCAN -> Strings.toolTypeSecurityScanDesc
        GENERATE_CODE -> Strings.toolTypeGenerateCodeDesc
        REFACTOR_CODE -> Strings.toolTypeRefactorCodeDesc
        FIX_ERROR -> Strings.toolTypeFixErrorDesc
        TEST_MODULE -> Strings.toolTypeTestModuleDesc
        VALIDATE_CONFIG -> Strings.toolTypeValidateConfigDesc
        GET_TEMPLATES -> Strings.toolTypeGetTemplatesDesc
        GET_SNIPPETS -> Strings.toolTypeGetSnippetsDesc
        SEARCH_DOCS -> Strings.toolTypeSearchDocsDesc
        CREATE_MODULE -> Strings.toolTypeCreateModuleDesc
        UPDATE_MODULE -> Strings.toolTypeUpdateModuleDesc
        PREVIEW_MODULE -> Strings.toolTypePreviewModuleDesc
    }
}




data class ToolParameter(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("required")
    val required: Boolean = false,
    @SerializedName("enum")
    val enumValues: List<String>? = null,
    @SerializedName("default")
    val default: Any? = null
)




data class AgentToolDefinition(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: AgentToolType,
    @SerializedName("description")
    val description: String,
    @SerializedName("parameters")
    val parameters: List<ToolParameter> = emptyList()
) {
    companion object {
        private val gson = com.webtoapp.util.GsonProvider.gson
    }




    fun toFunctionSchema(): JsonObject {
        return JsonObject().apply {
            addProperty("name", name)
            addProperty("description", description)
            add("parameters", JsonObject().apply {
                addProperty("type", "object")
                add("properties", JsonObject().apply {
                    parameters.forEach { param ->
                        add(param.name, JsonObject().apply {
                            addProperty("type", param.type)
                            addProperty("description", param.description)
                            param.enumValues?.let { enums ->
                                add("enum", gson.toJsonTree(enums))
                            }
                        })
                    }
                })
                add("required", gson.toJsonTree(parameters.filter { it.required }.map { it.name }))
            })
        }
    }
}




data class ToolCallRequest(
    @SerializedName("tool_name")
    val toolName: String,
    @SerializedName("arguments")
    val arguments: Map<String, Any?>,
    @SerializedName("call_id")
    val callId: String = java.util.UUID.randomUUID().toString()
)




data class ToolCallResult(
    @SerializedName("call_id")
    val callId: String,
    @SerializedName("tool_name")
    val toolName: String,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("result")
    val result: Any?,
    @SerializedName("error")
    val error: String? = null,
    @SerializedName("execution_time_ms")
    val executionTimeMs: Long = 0
)




data class SyntaxCheckResult(
    @SerializedName("valid")
    val valid: Boolean,
    @SerializedName("errors")
    val errors: List<CodeError> = emptyList(),
    @SerializedName("warnings")
    val warnings: List<CodeWarning> = emptyList()
)




data class CodeError(
    @SerializedName("line")
    val line: Int,
    @SerializedName("column")
    val column: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("severity")
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    @SerializedName("rule")
    val rule: String? = null,
    @SerializedName("suggestion")
    val suggestion: String? = null
)




data class CodeWarning(
    @SerializedName("line")
    val line: Int,
    @SerializedName("column")
    val column: Int,
    @SerializedName("message")
    val message: String,
    @SerializedName("rule")
    val rule: String? = null
)




enum class ErrorSeverity {
    ERROR,
    WARNING,
    INFO,
    HINT
}




data class SecurityScanResult(
    @SerializedName("safe")
    val safe: Boolean,
    @SerializedName("issues")
    val issues: List<SecurityIssue> = emptyList(),
    @SerializedName("risk_level")
    val riskLevel: RiskLevel = RiskLevel.LOW
)




data class SecurityIssue(
    @SerializedName("type")
    val type: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("line")
    val line: Int? = null,
    @SerializedName("severity")
    val severity: RiskLevel,
    @SerializedName("recommendation")
    val recommendation: String
)




enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}




data class ModuleValidationResult(
    @SerializedName("valid")
    val valid: Boolean,
    @SerializedName("issues")
    val issues: List<ValidationIssue> = emptyList()
)




data class ValidationIssue(
    @SerializedName("field")
    val field: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("severity")
    val severity: ErrorSeverity
)








sealed class ToolChainEvent {




    data class ChainStarted(val totalTools: Int) : ToolChainEvent()






    data class ToolStarted(val toolIndex: Int, val request: ToolCallRequest) : ToolChainEvent()






    data class ToolCompleted(val toolIndex: Int, val result: ToolCallResult) : ToolChainEvent()





    data class ChainCompleted(val results: List<ToolCallResult>) : ToolChainEvent()







    data class ChainFailed(
        val failedToolIndex: Int,
        val error: String,
        val completedResults: List<ToolCallResult>
    ) : ToolChainEvent()
}




object AgentTools {

    val syntaxCheck = AgentToolDefinition(
        name = "syntax_check",
        type = AgentToolType.SYNTAX_CHECK,
        description = Strings.agentToolSyntaxCheck,
        parameters = listOf(
            ToolParameter("code", "string", Strings.paramCodeToCheck, required = true),
            ToolParameter("language", "string", Strings.paramCodeLang, required = true, enumValues = listOf("javascript", "css"))
        )
    )

    val lintCode = AgentToolDefinition(
        name = "lint_code",
        type = AgentToolType.LINT_CODE,
        description = Strings.agentToolLintCode,
        parameters = listOf(
            ToolParameter("code", "string", Strings.paramCodeToCheck, required = true),
            ToolParameter("language", "string", Strings.paramCodeLang, required = true, enumValues = listOf("javascript", "css"))
        )
    )

    val securityScan = AgentToolDefinition(
        name = "security_scan",
        type = AgentToolType.SECURITY_SCAN,
        description = Strings.agentToolSecurityScan,
        parameters = listOf(
            ToolParameter("code", "string", Strings.paramCodeToScan, required = true)
        )
    )

    val generateCode = AgentToolDefinition(
        name = "generate_code",
        type = AgentToolType.GENERATE_CODE,
        description = Strings.agentToolGenerateCode,
        parameters = listOf(
            ToolParameter("requirement", "string", Strings.paramRequirement, required = true),
            ToolParameter("language", "string", Strings.paramTargetLang, required = true, enumValues = listOf("javascript", "css", "both")),
            ToolParameter("context", "string", Strings.paramContext, required = false)
        )
    )

    val fixError = AgentToolDefinition(
        name = "fix_error",
        type = AgentToolType.FIX_ERROR,
        description = Strings.agentToolFixError,
        parameters = listOf(
            ToolParameter("code", "string", Strings.paramCodeWithErrors, required = true),
            ToolParameter("errors", "array", Strings.paramErrorList, required = true),
            ToolParameter("language", "string", Strings.paramCodeLang, required = true, enumValues = listOf("javascript", "css"))
        )
    )

    val refactorCode = AgentToolDefinition(
        name = "refactor_code",
        type = AgentToolType.REFACTOR_CODE,
        description = Strings.agentToolRefactorCode,
        parameters = listOf(
            ToolParameter("code", "string", Strings.paramCodeToRefactor, required = true),
            ToolParameter("goals", "array", Strings.paramRefactorGoals, required = false, enumValues = listOf("readability", "performance", "maintainability", "security"))
        )
    )

    val testModule = AgentToolDefinition(
        name = "test_module",
        type = AgentToolType.TEST_MODULE,
        description = Strings.agentToolTestModule,
        parameters = listOf(
            ToolParameter("js_code", "string", "JavaScript", required = true),
            ToolParameter("css_code", "string", "CSS", required = false),
            ToolParameter("test_url", "string", Strings.paramTestUrl, required = false)
        )
    )

    val validateConfig = AgentToolDefinition(
        name = "validate_config",
        type = AgentToolType.VALIDATE_CONFIG,
        description = Strings.agentToolValidateConfig,
        parameters = listOf(
            ToolParameter("config_items", "array", Strings.paramConfigItems, required = true),
            ToolParameter("config_values", "object", Strings.paramConfigValues, required = false)
        )
    )

    val getTemplates = AgentToolDefinition(
        name = "get_templates",
        type = AgentToolType.GET_TEMPLATES,
        description = Strings.agentToolGetTemplates,
        parameters = listOf(
            ToolParameter("category", "string", "Module category", required = false),
            ToolParameter("keywords", "array", Strings.paramKeywords, required = false)
        )
    )

    val getSnippets = AgentToolDefinition(
        name = "get_snippets",
        type = AgentToolType.GET_SNIPPETS,
        description = Strings.agentToolGetSnippets,
        parameters = listOf(
            ToolParameter("query", "string", Strings.paramSearchKeyword, required = true),
            ToolParameter("category", "string", Strings.paramSnippetCategory, required = false)
        )
    )

    val createModule = AgentToolDefinition(
        name = "create_module",
        type = AgentToolType.CREATE_MODULE,
        description = Strings.agentToolCreateModule,
        parameters = listOf(
            ToolParameter("name", "string", "Module name", required = true),
            ToolParameter("description", "string", "Module description", required = true),
            ToolParameter("icon", "string", Strings.paramModuleIcon, required = false),
            ToolParameter("category", "string", "Module category", required = true),
            ToolParameter("js_code", "string", "JavaScript", required = true),
            ToolParameter("css_code", "string", "CSS", required = false),
            ToolParameter("config_items", "array", "Config items", required = false),
            ToolParameter("url_matches", "array", "URL match rules", required = false),
            ToolParameter("run_at", "string", Strings.paramRunAt, required = false, enumValues = listOf("DOCUMENT_START", "DOCUMENT_END", "DOCUMENT_IDLE"))
        )
    )

    val previewModule = AgentToolDefinition(
        name = "preview_module",
        type = AgentToolType.PREVIEW_MODULE,
        description = Strings.agentToolPreviewModule,
        parameters = listOf(
            ToolParameter("module_id", "string", Strings.paramModuleId, required = false),
            ToolParameter("js_code", "string", "JavaScript", required = false),
            ToolParameter("css_code", "string", "CSS", required = false),
            ToolParameter("preview_url", "string", Strings.paramPreviewUrl, required = true)
        )
    )




    fun getAllTools(): List<AgentToolDefinition> = listOf(
        syntaxCheck,
        lintCode,
        securityScan,
        generateCode,
        fixError,
        refactorCode,
        testModule,
        validateConfig,
        getTemplates,
        getSnippets,
        createModule,
        previewModule
    )




    fun getToolByName(name: String): AgentToolDefinition? {
        return getAllTools().find { it.name == name }
    }
}
