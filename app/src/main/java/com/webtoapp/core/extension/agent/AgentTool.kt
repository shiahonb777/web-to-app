package com.webtoapp.core.extension.agent

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.webtoapp.core.i18n.Strings

/**
 * Agent 工具定义
 * 
 * 定义 AI Agent 可以调用的工具，用于代码检查、测试、修复等
 */

/**
 * 工具类型
 */
enum class AgentToolType(val icon: String) {
    SYNTAX_CHECK("🔍"),
    LINT_CODE("📏"),
    SECURITY_SCAN("🔒"),
    GENERATE_CODE("✨"),
    REFACTOR_CODE("🔧"),
    FIX_ERROR("🩹"),
    TEST_MODULE("🧪"),
    VALIDATE_CONFIG("✅"),
    GET_TEMPLATES("📋"),
    GET_SNIPPETS("📦"),
    SEARCH_DOCS("📚"),
    CREATE_MODULE("➕"),
    UPDATE_MODULE("📝"),
    PREVIEW_MODULE("👁️");
    
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
    /**
     * 转换为 OpenAI Function Calling 格式
     */
    fun toFunctionSchema(): JsonObject {
        val gson = Gson()
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
        description = Strings.agentToolSyntaxCheck,
        parameters = listOf(
            ToolParameter("code", "string", "要检查的代码", required = true),
            ToolParameter("language", "string", "代码语言", required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val lintCode = AgentToolDefinition(
        name = "lint_code",
        type = AgentToolType.LINT_CODE,
        description = Strings.agentToolLintCode,
        parameters = listOf(
            ToolParameter("code", "string", "要检查的代码", required = true),
            ToolParameter("language", "string", "代码语言", required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val securityScan = AgentToolDefinition(
        name = "security_scan",
        type = AgentToolType.SECURITY_SCAN,
        description = Strings.agentToolSecurityScan,
        parameters = listOf(
            ToolParameter("code", "string", "要扫描的代码", required = true)
        )
    )
    
    val generateCode = AgentToolDefinition(
        name = "generate_code",
        type = AgentToolType.GENERATE_CODE,
        description = Strings.agentToolGenerateCode,
        parameters = listOf(
            ToolParameter("requirement", "string", "功能需求描述", required = true),
            ToolParameter("language", "string", "目标语言", required = true, enumValues = listOf("javascript", "css", "both")),
            ToolParameter("context", "string", "上下文信息，如现有代码", required = false)
        )
    )
    
    val fixError = AgentToolDefinition(
        name = "fix_error",
        type = AgentToolType.FIX_ERROR,
        description = Strings.agentToolFixError,
        parameters = listOf(
            ToolParameter("code", "string", "包含错误的代码", required = true),
            ToolParameter("errors", "array", "错误列表", required = true),
            ToolParameter("language", "string", "代码语言", required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val refactorCode = AgentToolDefinition(
        name = "refactor_code",
        type = AgentToolType.REFACTOR_CODE,
        description = Strings.agentToolRefactorCode,
        parameters = listOf(
            ToolParameter("code", "string", "要重构的代码", required = true),
            ToolParameter("goals", "array", "重构目标", required = false, enumValues = listOf("readability", "performance", "maintainability", "security"))
        )
    )
    
    val testModule = AgentToolDefinition(
        name = "test_module",
        type = AgentToolType.TEST_MODULE,
        description = Strings.agentToolTestModule,
        parameters = listOf(
            ToolParameter("js_code", "string", "JavaScript 代码", required = true),
            ToolParameter("css_code", "string", "CSS 代码", required = false),
            ToolParameter("test_url", "string", "测试页面 URL", required = false)
        )
    )
    
    val validateConfig = AgentToolDefinition(
        name = "validate_config",
        type = AgentToolType.VALIDATE_CONFIG,
        description = Strings.agentToolValidateConfig,
        parameters = listOf(
            ToolParameter("config_items", "array", "配置项列表", required = true),
            ToolParameter("config_values", "object", "配置值", required = false)
        )
    )
    
    val getTemplates = AgentToolDefinition(
        name = "get_templates",
        type = AgentToolType.GET_TEMPLATES,
        description = Strings.agentToolGetTemplates,
        parameters = listOf(
            ToolParameter("category", "string", "Module category", required = false),
            ToolParameter("keywords", "array", "关键词", required = false)
        )
    )
    
    val getSnippets = AgentToolDefinition(
        name = "get_snippets",
        type = AgentToolType.GET_SNIPPETS,
        description = Strings.agentToolGetSnippets,
        parameters = listOf(
            ToolParameter("query", "string", "搜索关键词", required = true),
            ToolParameter("category", "string", "代码片段分类", required = false)
        )
    )
    
    val createModule = AgentToolDefinition(
        name = "create_module",
        type = AgentToolType.CREATE_MODULE,
        description = Strings.agentToolCreateModule,
        parameters = listOf(
            ToolParameter("name", "string", "Module name", required = true),
            ToolParameter("description", "string", "Module description", required = true),
            ToolParameter("icon", "string", "模块图标 (emoji)", required = false),
            ToolParameter("category", "string", "Module category", required = true),
            ToolParameter("js_code", "string", "JavaScript 代码", required = true),
            ToolParameter("css_code", "string", "CSS 代码", required = false),
            ToolParameter("config_items", "array", "Config item", required = false),
            ToolParameter("url_matches", "array", "URL match rules", required = false),
            ToolParameter("run_at", "string", "执行时机", required = false, enumValues = listOf("DOCUMENT_START", "DOCUMENT_END", "DOCUMENT_IDLE"))
        )
    )
    
    val previewModule = AgentToolDefinition(
        name = "preview_module",
        type = AgentToolType.PREVIEW_MODULE,
        description = Strings.agentToolPreviewModule,
        parameters = listOf(
            ToolParameter("module_id", "string", "模块 ID", required = false),
            ToolParameter("js_code", "string", "JavaScript 代码", required = false),
            ToolParameter("css_code", "string", "CSS 代码", required = false),
            ToolParameter("preview_url", "string", "预览页面 URL", required = true)
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
