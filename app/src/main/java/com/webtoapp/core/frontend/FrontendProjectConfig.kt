package com.webtoapp.core.frontend




enum class FrontendFramework {
    VUE,
    REACT,
    NEXT,
    NUXT,
    ANGULAR,
    SVELTE,
    VITE,
    UNKNOWN
}




enum class DatabaseType {
    SQLITE,
    MYSQL,
    POSTGRESQL,
    MONGODB,
    REDIS,
    NONE
}




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
    val suggestions: List<String>,
    val runtimeRequirement: ProjectRuntimeRequirement = ProjectRuntimeRequirement()
)





data class DependencyInfo(
    val name: String,
    val version: String,
    val isDevDependency: Boolean = false,
    val category: DependencyCategory = DependencyCategory.OTHER
)




enum class DependencyCategory {
    FRAMEWORK,
    UI_LIBRARY,
    STATE_MANAGEMENT,
    ROUTER,
    HTTP_CLIENT,
    DATABASE,
    BUILD_TOOL,
    TESTING,
    OTHER
}




enum class PackageManager {
    NPM,
    YARN,
    PNPM,
    BUN
}




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




enum class BackendFramework {
    EXPRESS,
    FASTIFY,
    KOA,
    NESTJS,
    HAPI,
    NONE
}




data class FrontendProjectConfig(
    val projectPath: String,
    val framework: FrontendFramework,
    val outputDir: String,
    val hasDistFolder: Boolean = false
)




data class ProjectRuntimeRequirement(
    val needsNodeRuntime: Boolean = false,
    val backendFramework: BackendFramework = BackendFramework.NONE,
    val backendEntryFile: String? = null,
    val isSSR: Boolean = false,
    val canStaticExport: Boolean = false,
    val envVars: Map<String, String> = emptyMap(),
    val envVarHints: Map<String, String> = emptyMap()
)




sealed class BuildState {
    object Idle : BuildState()
    object Scanning : BuildState()
    data class Importing(val progress: Float, val message: String) : BuildState()
    data class Success(val outputPath: String, val fileCount: Int) : BuildState()
    data class Error(val message: String) : BuildState()


    object CheckingEnvironment : BuildState()
    object InitializingEnvironment : BuildState()
    data class DownloadingEnvironment(val component: String, val progress: Float) : BuildState()
    data class CopyingProject(val progress: Float) : BuildState()
    data class InstallingDependencies(val progress: Float, val currentPackage: String) : BuildState()
    data class BuildingProject(val progress: Float, val stage: String) : BuildState()
    object ProcessingOutput : BuildState()
}




enum class BuildMode {
    IMPORT_DIST,
    FULL_BUILD
}




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
