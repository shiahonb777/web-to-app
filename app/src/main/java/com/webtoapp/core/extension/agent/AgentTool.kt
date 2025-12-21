package com.webtoapp.core.extension.agent

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Agent 工具定义
 * 
 * 定义 AI Agent 可以调用的工具，用于代码检查、测试、修复等
 */

/**
 * 工具类型
 */
enum class AgentToolType(
    val displayName: String,
    val description: String,
    val icon: String
) {
    // 代码分析工具
    SYNTAX_CHECK("语法检查", "检查 JavaScript/CSS 代码语法错误", "🔍"),
    LINT_CODE("代码规范检查", "检查代码风格和最佳实践", "📏"),
    SECURITY_SCAN("安全扫描", "检查潜在的安全问题", "🔒"),
    
    // 代码生成工具
    GENERATE_CODE("生成代码", "根据需求生成代码", "✨"),
    REFACTOR_CODE("重构代码", "优化和重构现有代码", "🔧"),
    FIX_ERROR("修复错误", "自动修复检测到的错误", "🩹"),
    
    // 测试工具
    TEST_MODULE("测试模块", "在测试页面运行模块", "🧪"),
    VALIDATE_CONFIG("验证配置", "验证模块配置项", "✅"),
    
    // 信息获取工具
    GET_TEMPLATES("获取模板", "获取相关代码模板", "📋"),
    GET_SNIPPETS("获取代码片段", "获取可用的代码片段", "📦"),
    SEARCH_DOCS("搜索文档", "搜索相关文档和示例", "📚"),
    
    // 模块操作工具
    CREATE_MODULE("创建模块", "创建新的扩展模块", "➕"),
    UPDATE_MODULE("更新模块", "更新现有模块", "📝"),
    PREVIEW_MODULE("预览模块", "预览模块效果", "👁️")
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
    ERROR,      // 错误，必须修复
    WARNING,    // 警告，建议修复
    INFO,       // 信息，可选优化
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
        description = "检查 JavaScript 或 CSS 代码的语法错误。返回错误列表和修复建议。",
        parameters = listOf(
            ToolParameter("code", "string", "要检查的代码", required = true),
            ToolParameter("language", "string", "代码语言", required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val lintCode = AgentToolDefinition(
        name = "lint_code",
        type = AgentToolType.LINT_CODE,
        description = "检查代码风格和最佳实践，提供优化建议。",
        parameters = listOf(
            ToolParameter("code", "string", "要检查的代码", required = true),
            ToolParameter("language", "string", "代码语言", required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val securityScan = AgentToolDefinition(
        name = "security_scan",
        type = AgentToolType.SECURITY_SCAN,
        description = "扫描代码中的安全问题，如 XSS、不安全的 eval 使用等。",
        parameters = listOf(
            ToolParameter("code", "string", "要扫描的代码", required = true)
        )
    )
    
    val generateCode = AgentToolDefinition(
        name = "generate_code",
        type = AgentToolType.GENERATE_CODE,
        description = "根据需求描述生成 JavaScript/CSS 代码。",
        parameters = listOf(
            ToolParameter("requirement", "string", "功能需求描述", required = true),
            ToolParameter("language", "string", "目标语言", required = true, enumValues = listOf("javascript", "css", "both")),
            ToolParameter("context", "string", "上下文信息，如现有代码", required = false)
        )
    )
    
    val fixError = AgentToolDefinition(
        name = "fix_error",
        type = AgentToolType.FIX_ERROR,
        description = "自动修复代码中检测到的错误。",
        parameters = listOf(
            ToolParameter("code", "string", "包含错误的代码", required = true),
            ToolParameter("errors", "array", "错误列表", required = true),
            ToolParameter("language", "string", "代码语言", required = true, enumValues = listOf("javascript", "css"))
        )
    )
    
    val refactorCode = AgentToolDefinition(
        name = "refactor_code",
        type = AgentToolType.REFACTOR_CODE,
        description = "重构和优化代码，提高可读性和性能。",
        parameters = listOf(
            ToolParameter("code", "string", "要重构的代码", required = true),
            ToolParameter("goals", "array", "重构目标", required = false, enumValues = listOf("readability", "performance", "maintainability", "security"))
        )
    )
    
    val testModule = AgentToolDefinition(
        name = "test_module",
        type = AgentToolType.TEST_MODULE,
        description = "在测试页面运行模块代码，返回执行结果。",
        parameters = listOf(
            ToolParameter("js_code", "string", "JavaScript 代码", required = true),
            ToolParameter("css_code", "string", "CSS 代码", required = false),
            ToolParameter("test_url", "string", "测试页面 URL", required = false)
        )
    )
    
    val validateConfig = AgentToolDefinition(
        name = "validate_config",
        type = AgentToolType.VALIDATE_CONFIG,
        description = "验证模块配置项的完整性和正确性。",
        parameters = listOf(
            ToolParameter("config_items", "array", "配置项列表", required = true),
            ToolParameter("config_values", "object", "配置值", required = false)
        )
    )
    
    val getTemplates = AgentToolDefinition(
        name = "get_templates",
        type = AgentToolType.GET_TEMPLATES,
        description = "获取与需求相关的代码模板。",
        parameters = listOf(
            ToolParameter("category", "string", "模块分类", required = false),
            ToolParameter("keywords", "array", "关键词", required = false)
        )
    )
    
    val getSnippets = AgentToolDefinition(
        name = "get_snippets",
        type = AgentToolType.GET_SNIPPETS,
        description = "搜索可用的代码片段。",
        parameters = listOf(
            ToolParameter("query", "string", "搜索关键词", required = true),
            ToolParameter("category", "string", "代码片段分类", required = false)
        )
    )
    
    val createModule = AgentToolDefinition(
        name = "create_module",
        type = AgentToolType.CREATE_MODULE,
        description = "创建新的扩展模块。",
        parameters = listOf(
            ToolParameter("name", "string", "模块名称", required = true),
            ToolParameter("description", "string", "模块描述", required = true),
            ToolParameter("icon", "string", "模块图标 (emoji)", required = false),
            ToolParameter("category", "string", "模块分类", required = true),
            ToolParameter("js_code", "string", "JavaScript 代码", required = true),
            ToolParameter("css_code", "string", "CSS 代码", required = false),
            ToolParameter("config_items", "array", "配置项", required = false),
            ToolParameter("url_matches", "array", "URL 匹配规则", required = false),
            ToolParameter("run_at", "string", "执行时机", required = false, enumValues = listOf("DOCUMENT_START", "DOCUMENT_END", "DOCUMENT_IDLE"))
        )
    )
    
    val previewModule = AgentToolDefinition(
        name = "preview_module",
        type = AgentToolType.PREVIEW_MODULE,
        description = "预览模块在指定页面的效果。",
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
