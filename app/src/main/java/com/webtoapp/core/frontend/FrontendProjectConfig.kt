package com.webtoapp.core.frontend

/**
 * 前端项目类型
 */
enum class FrontendFramework {
    VUE,        // Vue.js 项目
    REACT,      // React 项目
    NEXT,       // Next.js 项目
    NUXT,       // Nuxt.js 项目
    ANGULAR,    // Angular 项目
    SVELTE,     // Svelte 项目
    VITE,       // Vite 项目（通用）
    UNKNOWN     // 未知类型
}

/**
 * 数据库类型
 */
enum class DatabaseType {
    SQLITE,     // SQLite
    MYSQL,      // MySQL
    POSTGRESQL, // PostgreSQL
    MONGODB,    // MongoDB
    REDIS,      // Redis
    NONE        // 无数据库
}

/**
 * 项目检测结果
 */
data class ProjectDetectionResult(
    val framework: FrontendFramework,
    val frameworkVersion: String?,
    val packageManager: PackageManager,
    val hasTypeScript: Boolean,
    val databases: List<DatabaseType>,
    val dependencies: List<DependencyInfo>,
    val devDependencies: List<DependencyInfo>,
    val scripts: Map<String, String>,
    val buildCommand: String?,
    val devCommand: String?,
    val outputDir: String,
    val issues: List<ProjectIssue>,
    val suggestions: List<String>
)


/**
 * 依赖信息
 */
data class DependencyInfo(
    val name: String,
    val version: String,
    val isDevDependency: Boolean = false,
    val category: DependencyCategory = DependencyCategory.OTHER
)

/**
 * 依赖分类
 */
enum class DependencyCategory {
    FRAMEWORK,      // 框架核心
    UI_LIBRARY,     // UI 库
    STATE_MANAGEMENT, // 状态管理
    ROUTER,         // 路由
    HTTP_CLIENT,    // HTTP 客户端
    DATABASE,       // Database
    BUILD_TOOL,     // Build工具
    TESTING,        // 测试
    OTHER           // 其他
}

/**
 * 包管理器
 */
enum class PackageManager {
    NPM,
    YARN,
    PNPM,
    BUN
}

/**
 * 项目问题
 */
data class ProjectIssue(
    val severity: IssueSeverity,
    val type: IssueType,
    val message: String,
    val suggestion: String? = null
)

enum class IssueSeverity {
    ERROR,
    WARNING,
    INFO
}

enum class IssueType {
    MISSING_DEPENDENCY,
    INCOMPATIBLE_VERSION,
    MISSING_CONFIG,
    UNSUPPORTED_FEATURE,
    DATABASE_CONFIG,
    BUILD_ERROR,
    NO_DIST_FOLDER
}

/**
 * 前端项目配置
 */
data class FrontendProjectConfig(
    val projectPath: String,
    val framework: FrontendFramework,
    val outputDir: String,
    val hasDistFolder: Boolean = false
)

/**
 * 构建状态
 */
sealed class BuildState {
    object Idle : BuildState()
    object Scanning : BuildState()
    data class Importing(val progress: Float, val message: String) : BuildState()
    data class Success(val outputPath: String, val fileCount: Int) : BuildState()
    data class Error(val message: String) : BuildState()
    
    // 完整构建状态
    object CheckingEnvironment : BuildState()
    object InitializingEnvironment : BuildState()
    data class DownloadingEnvironment(val component: String, val progress: Float) : BuildState()
    data class CopyingProject(val progress: Float) : BuildState()
    data class InstallingDependencies(val progress: Float, val currentPackage: String) : BuildState()
    data class BuildingProject(val progress: Float, val stage: String) : BuildState()
    object ProcessingOutput : BuildState()
}

/**
 * 构建模式
 */
enum class BuildMode {
    IMPORT_DIST,    // Import已构建的 dist 目录
    FULL_BUILD      // 完整构建（使用 Linux 环境）
}

/**
 * 构建日志条目
 */
data class BuildLogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val message: String
)

enum class LogLevel {
    DEBUG,
    INFO,
    WARNING,
    ERROR
}
