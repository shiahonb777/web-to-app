package com.webtoapp.core.extension.agent

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * Agent 工具定义
 * 
 * 定义 AI Agent 可以调用的工具，用于代码检查、测试、修复等
 */

/**
 * 工具类型
 */
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
        SYNTAX_CHECK -> AppStringsProvider.current().toolTypeSyntaxCheck
        LINT_CODE -> AppStringsProvider.current().toolTypeLintCode
        SECURITY_SCAN -> AppStringsProvider.current().toolTypeSecurityScan
        GENERATE_CODE -> AppStringsProvider.current().toolTypeGenerateCode
        REFACTOR_CODE -> AppStringsProvider.current().toolTypeRefactorCode
        FIX_ERROR -> AppStringsProvider.current().toolTypeFixError
        TEST_MODULE -> AppStringsProvider.current().toolTypeTestModule
        VALIDATE_CONFIG -> AppStringsProvider.current().toolTypeValidateConfig
        GET_TEMPLATES -> AppStringsProvider.current().toolTypeGetTemplates
        GET_SNIPPETS -> AppStringsProvider.current().toolTypeGetSnippets
        SEARCH_DOCS -> AppStringsProvider.current().toolTypeSearchDocs
        CREATE_MODULE -> AppStringsProvider.current().toolTypeCreateModule
        UPDATE_MODULE -> AppStringsProvider.current().toolTypeUpdateModule
        PREVIEW_MODULE -> AppStringsProvider.current().toolTypePreviewModule
    }
    
    val description: String get() = when (this) {
        SYNTAX_CHECK -> AppStringsProvider.current().toolTypeSyntaxCheckDesc
        LINT_CODE -> AppStringsProvider.current().toolTypeLintCodeDesc
        SECURITY_SCAN -> AppStringsProvider.current().toolTypeSecurityScanDesc
        GENERATE_CODE -> AppStringsProvider.current().toolTypeGenerateCodeDesc
        REFACTOR_CODE -> AppStringsProvider.current().toolTypeRefactorCodeDesc
        FIX_ERROR -> AppStringsProvider.current().toolTypeFixErrorDesc
        TEST_MODULE -> AppStringsProvider.current().toolTypeTestModuleDesc
        VALIDATE_CONFIG -> AppStringsProvider.current().toolTypeValidateConfigDesc
        GET_TEMPLATES -> AppStringsProvider.current().toolTypeGetTemplatesDesc
        GET_SNIPPETS -> AppStringsProvider.current().toolTypeGetSnippetsDesc
        SEARCH_DOCS -> AppStringsProvider.current().toolTypeSearchDocsDesc
        CREATE_MODULE -> AppStringsProvider.current().toolTypeCreateModuleDesc
        UPDATE_MODULE -> AppStringsProvider.current().toolTypeUpdateModuleDesc
        PREVIEW_MODULE -> AppStringsProvider.current().toolTypePreviewModuleDesc
    }
}

/**
 * 工具参数定义
 */
data class ToolParameter(
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String,  // string, number, boolean, array, object
    @SerializedName("description")
    val description: String,
    @SerializedName("required")
    val required: Boolean = false,
    @SerializedName("enum")
    val enumValues: List<String>? = null,
    @SerializedName("default")
    val default: Any? = null
)

/**
 * 工具定义
 */
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
    
    /**
     * 转换为 OpenAI Function Calling 格式
     */
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

/**
 * 工具调用请求
 */
data class ToolCallRequest(
    @SerializedName("tool_name")
    val toolName: String,
    @SerializedName("arguments")
    val arguments: Map<String, Any?>,
    @SerializedName("call_id")
    val callId: String = java.util.UUID.randomUUID().toString()
)

/**
 * 工具调用结果
 */
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

/**
 * 语法检查结果
 */
data class SyntaxCheckResult(
    @SerializedName("valid")
    val valid: Boolean,
    @SerializedName("errors")
    val errors: List<CodeError> = emptyList(),
    @SerializedName("warnings")
    val warnings: List<CodeWarning> = emptyList()
)

/**
 * 代码错误
 */
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

/**
 * 代码警告
 */
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

/**
 * 错误严重程度
 */
enum class ErrorSeverity {
    ERROR,      // Error，必须修复
    WARNING,    // Warning，建议修复
    INFO,       // Info，可选优化
    HINT        // 提示
}

/**
 * 安全扫描结果
 */
data class SecurityScanResult(
    @SerializedName("safe")
    val safe: Boolean,
    @SerializedName("issues")
    val issues: List<SecurityIssue> = emptyList(),
    @SerializedName("risk_level")
    val riskLevel: RiskLevel = RiskLevel.LOW
)

/**
 * 安全问题
 */
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

/**
 * 风险等级
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * 模块验证结果
 */
data class ModuleValidationResult(
    @SerializedName("valid")
    val valid: Boolean,
    @SerializedName("issues")
    val issues: List<ValidationIssue> = emptyList()
)

/**
 * 验证问题
 */
data class ValidationIssue(
    @SerializedName("field")
    val field: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("severity")
    val severity: ErrorSeverity
)

/**
 * 工具链执行事件
 * 
 * 用于跟踪工具链执行过程中的各种事件
 * 
 * Requirements: 5.5
 */
sealed class ToolChainEvent {
    /**
     * 工具链开始执行
     * @param totalTools 工具链中的工具总数
     */
    data class ChainStarted(val totalTools: Int) : ToolChainEvent()
    
    /**
     * 单个工具开始执行
     * @param toolIndex 工具在链中的索引
     * @param request 工具调用请求
     */
    data class ToolStarted(val toolIndex: Int, val request: ToolCallRequest) : ToolChainEvent()
    
    /**
     * 单个工具执行完成
     * @param toolIndex 工具在链中的索引
     * @param result 工具执行结果
     */
    data class ToolCompleted(val toolIndex: Int, val result: ToolCallResult) : ToolChainEvent()
    
    /**
     * 工具链执行完成
     * @param results 所有工具的执行结果
     */
    data class ChainCompleted(val results: List<ToolCallResult>) : ToolChainEvent()
    
    /**
     * 工具链执行失败
     * @param failedToolIndex 失败的工具索引
     * @param error 错误信息
     * @param completedResults 已完成的工具结果
     */
    data class ChainFailed(
        val failedToolIndex: Int,
        val error: String,
        val completedResults: List<ToolCallResult>
    ) : ToolChainEvent()
}

/**
 * 预定义的 Agent 工具集
 */
object AgentTools {
    
    val syntaxCheck = AgentToolDefinition(
        name = "syntax_check",
        type = AgentToolType.SYNTAX_CHECK,
        description = AppStringsProvider.current().agentToolSyntaxCheck,
        parameters = listOf(
            ToolParameter("code", "string", AppStringsProvider.current().paramCodeToCheck, required = true),
            ToolParameter("language", "string", AppStringsProvider.current().paramCodeLang, required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val lintCode = AgentToolDefinition(
        name = "lint_code",
        type = AgentToolType.LINT_CODE,
        description = AppStringsProvider.current().agentToolLintCode,
        parameters = listOf(
            ToolParameter("code", "string", AppStringsProvider.current().paramCodeToCheck, required = true),
            ToolParameter("language", "string", AppStringsProvider.current().paramCodeLang, required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val securityScan = AgentToolDefinition(
        name = "security_scan",
        type = AgentToolType.SECURITY_SCAN,
        description = AppStringsProvider.current().agentToolSecurityScan,
        parameters = listOf(
            ToolParameter("code", "string", AppStringsProvider.current().paramCodeToScan, required = true)
        )
    )
    
    val generateCode = AgentToolDefinition(
        name = "generate_code",
        type = AgentToolType.GENERATE_CODE,
        description = AppStringsProvider.current().agentToolGenerateCode,
        parameters = listOf(
            ToolParameter("requirement", "string", AppStringsProvider.current().paramRequirement, required = true),
            ToolParameter("language", "string", AppStringsProvider.current().paramTargetLang, required = true, enumValues = listOf("javascript", "css", "both")),
            ToolParameter("context", "string", AppStringsProvider.current().paramContext, required = false)
        )
    )
    
    val fixError = AgentToolDefinition(
        name = "fix_error",
        type = AgentToolType.FIX_ERROR,
        description = AppStringsProvider.current().agentToolFixError,
        parameters = listOf(
            ToolParameter("code", "string", AppStringsProvider.current().paramCodeWithErrors, required = true),
            ToolParameter("errors", "array", AppStringsProvider.current().paramErrorList, required = true),
            ToolParameter("language", "string", AppStringsProvider.current().paramCodeLang, required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val refactorCode = AgentToolDefinition(
        name = "refactor_code",
        type = AgentToolType.REFACTOR_CODE,
        description = AppStringsProvider.current().agentToolRefactorCode,
        parameters = listOf(
            ToolParameter("code", "string", AppStringsProvider.current().paramCodeToRefactor, required = true),
            ToolParameter("goals", "array", AppStringsProvider.current().paramRefactorGoals, required = false, enumValues = listOf("readability", "performance", "maintainability", "security"))
        )
    )
    
    val testModule = AgentToolDefinition(
        name = "test_module",
        type = AgentToolType.TEST_MODULE,
        description = AppStringsProvider.current().agentToolTestModule,
        parameters = listOf(
            ToolParameter("js_code", "string", "JavaScript", required = true),
            ToolParameter("css_code", "string", "CSS", required = false),
            ToolParameter("test_url", "string", AppStringsProvider.current().paramTestUrl, required = false)
        )
    )
    
    val validateConfig = AgentToolDefinition(
        name = "validate_config",
        type = AgentToolType.VALIDATE_CONFIG,
        description = AppStringsProvider.current().agentToolValidateConfig,
        parameters = listOf(
            ToolParameter("config_items", "array", AppStringsProvider.current().paramConfigItems, required = true),
            ToolParameter("config_values", "object", AppStringsProvider.current().paramConfigValues, required = false)
        )
    )
    
    val getTemplates = AgentToolDefinition(
        name = "get_templates",
        type = AgentToolType.GET_TEMPLATES,
        description = AppStringsProvider.current().agentToolGetTemplates,
        parameters = listOf(
            ToolParameter("category", "string", "Module category", required = false),
            ToolParameter("keywords", "array", AppStringsProvider.current().paramKeywords, required = false)
        )
    )
    
    val getSnippets = AgentToolDefinition(
        name = "get_snippets",
        type = AgentToolType.GET_SNIPPETS,
        description = AppStringsProvider.current().agentToolGetSnippets,
        parameters = listOf(
            ToolParameter("query", "string", AppStringsProvider.current().paramSearchKeyword, required = true),
            ToolParameter("category", "string", AppStringsProvider.current().paramSnippetCategory, required = false)
        )
    )
    
    val createModule = AgentToolDefinition(
        name = "create_module",
        type = AgentToolType.CREATE_MODULE,
        description = AppStringsProvider.current().agentToolCreateModule,
        parameters = listOf(
            ToolParameter("name", "string", "Module name", required = true),
            ToolParameter("description", "string", "Module description", required = true),
            ToolParameter("icon", "string", AppStringsProvider.current().paramModuleIcon, required = false),
            ToolParameter("category", "string", "Module category", required = true),
            ToolParameter("js_code", "string", "JavaScript", required = true),
            ToolParameter("css_code", "string", "CSS", required = false),
            ToolParameter("config_items", "array", "Config items", required = false),
            ToolParameter("url_matches", "array", "URL match rules", required = false),
            ToolParameter("run_at", "string", AppStringsProvider.current().paramRunAt, required = false, enumValues = listOf("DOCUMENT_START", "DOCUMENT_END", "DOCUMENT_IDLE"))
        )
    )
    
    val previewModule = AgentToolDefinition(
        name = "preview_module",
        type = AgentToolType.PREVIEW_MODULE,
        description = AppStringsProvider.current().agentToolPreviewModule,
        parameters = listOf(
            ToolParameter("module_id", "string", AppStringsProvider.current().paramModuleId, required = false),
            ToolParameter("js_code", "string", "JavaScript", required = false),
            ToolParameter("css_code", "string", "CSS", required = false),
            ToolParameter("preview_url", "string", AppStringsProvider.current().paramPreviewUrl, required = true)
        )
    )
    
    /**
     * 获取所有工具定义
     */
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
    
    /**
     * 根据名称获取工具
     */
    fun getToolByName(name: String): AgentToolDefinition? {
        return getAllTools().find { it.name == name }
    }
}
