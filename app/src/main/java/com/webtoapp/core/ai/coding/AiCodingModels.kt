package com.webtoapp.core.ai.coding

import com.webtoapp.core.i18n.Strings

import java.util.UUID

/**
 * HTML AI -.
 */

/**
 * Note.
 */
data class AiCodingSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",  // UI Strings.newConversation.
    val messages: List<AiCodingMessage> = emptyList(),
    val checkpoints: List<ProjectCheckpoint> = emptyList(),  // Version.
    val currentCheckpointIndex: Int = -1,                     // Note.
    val config: SessionConfig = SessionConfig(),
    val projectDir: String? = null,                           // Note.
    val codingType: AiCodingType = AiCodingType.HTML,        // AI.
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Session Config.
 */
data class SessionConfig(
    val textModelId: String? = null,           // ID.
    val imageModelId: String? = null,          // ID.
    val temperature: Float = 0.7f,             // Note.
    val rules: List<String> = emptyList(),     // Note.
    val selectedTemplateId: String? = null,    // ID.
    val selectedStyleId: String? = null,       // ID.
    // Note.
    val enabledTools: Set<AiCodingToolType> = setOf(AiCodingToolType.WRITE_HTML)  // Enable.
) {
    /**
     * Note.
     */
    fun getEffectiveRules(): List<String> {
        return if (rules.isEmpty()) {
            listOf(Strings.ruleUseChinese)
        } else {
            rules
        }
    }
}

/**
 * HTML.
 */
enum class AiCodingToolType(
    val icon: String,
    val requiresImageModel: Boolean = false
) {
    WRITE_HTML("code"),
    EDIT_HTML("edit"),
    READ_CURRENT_CODE("book"),
    GENERATE_IMAGE("palette", true),
    GET_CONSOLE_LOGS("clipboard"),
    CHECK_SYNTAX("search"),
    AUTO_FIX("wrench");
    
    fun getDisplayName(): String = when (this) {
        WRITE_HTML -> Strings.toolWriteHtml
        EDIT_HTML -> Strings.toolEditHtml
        READ_CURRENT_CODE -> Strings.toolReadCurrentCode
        GENERATE_IMAGE -> Strings.toolGenerateImage
        GET_CONSOLE_LOGS -> Strings.toolGetConsoleLogs
        CHECK_SYNTAX -> Strings.toolCheckSyntax
        AUTO_FIX -> Strings.toolAutoFix
    }
    
    fun getDescription(): String = when (this) {
        WRITE_HTML -> Strings.toolWriteHtmlDesc
        EDIT_HTML -> Strings.toolEditHtmlDesc
        READ_CURRENT_CODE -> Strings.toolReadCurrentCodeDesc
        GENERATE_IMAGE -> Strings.toolGenerateImageDesc
        GET_CONSOLE_LOGS -> Strings.toolGetConsoleLogsDesc
        CHECK_SYNTAX -> Strings.toolCheckSyntaxDesc
        AUTO_FIX -> Strings.toolAutoFixDesc
    }
}

/**
 * Note.
 */
data class AiCodingMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val images: List<String> = emptyList(),    // Image 3.
    val thinking: String? = null,               // Note.
    val codeBlocks: List<CodeBlock> = emptyList(), // Note.
    val fileRefs: List<FileReference> = emptyList(), // File.
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,              // Yes.
    val originalContent: String? = null         // Note.
)

/**
 * Note.
 */
data class FileReference(
    val filename: String,           // File ( index_v2.html).
    val baseName: String,           // ( index).
    val version: Int,               // Version.
    val type: ProjectFileType,      // File.
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Note.
 */
enum class MessageRole {
    USER,       // User
    ASSISTANT,  // AI.
    SYSTEM      // System.
}

/**
 * Note.
 */
data class CodeBlock(
    val id: String = UUID.randomUUID().toString(),
    val language: String = "html",              // Note.
    val filename: String? = null,               // File.
    val content: String,                        // Note.
    val isComplete: Boolean = true              // Yes.
)

/**
 * Note.
 */
data class ProjectCheckpoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,                           // Check.
    val description: String = "",               // Description
    val messageIndex: Int,                      // Note.
    val files: List<ProjectFile>,               // Note.
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Note.
 */
data class ProjectFile(
    val name: String,                           // File.
    val content: String,                        // File.
    val type: ProjectFileType = ProjectFileType.HTML
)

/**
 * Note.
 */
enum class ProjectFileType(val extension: String, val mimeType: String) {
    HTML("html", "text/html"),
    CSS("css", "text/css"),
    JS("js", "application/javascript"),
    SVG("svg", "image/svg+xml"),
    JSON("json", "application/json"),
    IMAGE("png", "image/png"),
    OTHER("txt", "text/plain")
}

/**
 * Note.
 */
data class StyleTemplate(
    val id: String,
    val name: String,                           // Note.
    val category: TemplateCategory,             // Note.
    val description: String,                    // Description
    val previewImage: String? = null,           // Note.
    val cssFramework: String? = null,           // CSS.
    val colorScheme: ColorScheme? = null,       // Note.
    val promptHint: String,                     // Note.
    val exampleCode: String? = null             // Note.
)

/**
 * Template Categories.
 */
enum class TemplateCategory {
    MODERN,
    GLASSMORPHISM,
    NEUMORPHISM,
    GRADIENT,
    DARK,
    MINIMAL,
    RETRO,
    CYBERPUNK,
    NATURE,
    BUSINESS,
    CREATIVE,
    GAME;
    
    fun getDisplayName(): String = when (this) {
        MODERN -> Strings.templateModern
        GLASSMORPHISM -> Strings.templateGlassmorphism
        NEUMORPHISM -> Strings.templateNeumorphism
        GRADIENT -> Strings.templateGradient
        DARK -> Strings.templateDark
        MINIMAL -> Strings.templateMinimal
        RETRO -> Strings.templateRetro
        CYBERPUNK -> Strings.templateCyberpunk
        NATURE -> Strings.templateNature
        BUSINESS -> Strings.templateBusiness
        CREATIVE -> Strings.templateCreative
        GAME -> Strings.templateGame
    }
}

/**
 * Note.
 */
data class ColorScheme(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String,
    val text: String,
    val accent: String
)

/**
 * Note.
 */
data class StyleReference(
    val id: String,
    val name: String,                           // Note.
    val category: StyleReferenceCategory,       // Note.
    val keywords: List<String>,                 // Note.
    val description: String,                    // Note.
    val colorHints: List<String>,               // Note.
    val elementHints: List<String>              // Note.
)

/**
 * Note.
 */
enum class StyleReferenceCategory {
    MOVIE,
    BOOK,
    ANIME,
    GAME,
    BRAND,
    ART,
    ERA,
    CULTURE;
    
    fun getDisplayName(): String = when (this) {
        MOVIE -> Strings.styleRefMovie
        BOOK -> Strings.styleRefBook
        ANIME -> Strings.styleRefAnime
        GAME -> Strings.styleRefGame
        BRAND -> Strings.styleRefBrand
        ART -> Strings.styleRefArt
        ERA -> Strings.styleRefEra
        CULTURE -> Strings.styleRefCulture
    }
}

/**
 * Rules.
 */
data class RulesTemplate(
    val id: String,
    val name: String,
    val description: String,
    val rules: List<String>
)

/**
 * Note.
 */
data class ImageGenerationRequest(
    val prompt: String,
    val negativePrompt: String? = null,
    val width: Int = 512,
    val height: Int = 512,
    val style: String? = null
)

/**
 * Note.
 */
data class ImageGenerationResult(
    val success: Boolean,
    val imageUrl: String? = null,
    val localPath: String? = null,
    val error: String? = null
)

/**
 * AI.
 */
data class ParsedAiResponse(
    val textContent: String,                    // Note.
    val thinking: String?,                      // Note.
    val codeBlocks: List<CodeBlock>,           // Note.
    val imageRequests: List<ImageGenerationRequest> // Note.
)

/**
 * Note.
 */
sealed class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    data class Streaming(val partialContent: String) : ChatState()
    data class GeneratingImage(val prompt: String) : ChatState()
    data class Error(val message: String) : ChatState()
}

/**
 * Note.
 */
data class SaveConfig(
    val directory: String,                      // Save.
    val projectName: String,                    // Note.
    val createFolder: Boolean = true,           // Yes.
    val overwrite: Boolean = false              // Yes.
)

/**
 * Note.
 */
data class CodeLibraryItem(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,                      // ID.
    val messageId: String,                      // ID.
    val title: String,                          // Note.
    val description: String = "",               // Description
    val files: List<ProjectFile>,               // File.
    val previewHtml: String,                    // HTML.
    val conversationContext: String,            // Note.
    val userPrompt: String,                     // User.
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),       // Note.
    val isFavorite: Boolean = false             // Yes.
)

/**
 * Note.
 */
data class ConversationCheckpoint(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,                      // SessionID
    val name: String,                           // Check.
    val messageCount: Int,                      // Note.
    val messages: List<AiCodingMessage>,      // Note.
    val codeLibraryIds: List<String>,           // ID.
    val config: SessionConfig,                  // Session.
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * HTML.
 */
fun AiCodingToolType.getLocalizedDisplayName(): String {
    return when (this) {
        AiCodingToolType.WRITE_HTML -> com.webtoapp.core.i18n.Strings.featureWriteHtml
        AiCodingToolType.EDIT_HTML -> com.webtoapp.core.i18n.Strings.featureEditHtml
        AiCodingToolType.READ_CURRENT_CODE -> com.webtoapp.core.i18n.Strings.featureReadCurrentCode
        AiCodingToolType.GENERATE_IMAGE -> com.webtoapp.core.i18n.Strings.aiImageGeneration
        AiCodingToolType.GET_CONSOLE_LOGS -> com.webtoapp.core.i18n.Strings.featureGetConsoleLogs
        AiCodingToolType.CHECK_SYNTAX -> com.webtoapp.core.i18n.Strings.featureCheckSyntax
        AiCodingToolType.AUTO_FIX -> com.webtoapp.core.i18n.Strings.featureAutoFix
    }
}

/**
 * HTML.
 */
fun AiCodingToolType.getLocalizedDescription(): String {
    return when (this) {
        AiCodingToolType.WRITE_HTML -> com.webtoapp.core.i18n.Strings.writeHtmlDesc
        AiCodingToolType.EDIT_HTML -> com.webtoapp.core.i18n.Strings.editHtmlDesc
        AiCodingToolType.READ_CURRENT_CODE -> com.webtoapp.core.i18n.Strings.readCurrentCodeDesc
        AiCodingToolType.GENERATE_IMAGE -> com.webtoapp.core.i18n.Strings.generateImageDesc
        AiCodingToolType.GET_CONSOLE_LOGS -> com.webtoapp.core.i18n.Strings.getConsoleLogsDesc
        AiCodingToolType.CHECK_SYNTAX -> com.webtoapp.core.i18n.Strings.checkSyntaxDesc
        AiCodingToolType.AUTO_FIX -> com.webtoapp.core.i18n.Strings.autoFixDesc
    }
}

/**
 * Template Categories.
 */
fun TemplateCategory.getLocalizedDisplayName(): String {
    return when (this) {
        TemplateCategory.MODERN -> com.webtoapp.core.i18n.Strings.templateModern
        TemplateCategory.GLASSMORPHISM -> com.webtoapp.core.i18n.Strings.templateGlassmorphism
        TemplateCategory.NEUMORPHISM -> com.webtoapp.core.i18n.Strings.templateNeumorphism
        TemplateCategory.GRADIENT -> com.webtoapp.core.i18n.Strings.templateGradient
        TemplateCategory.DARK -> com.webtoapp.core.i18n.Strings.templateDark
        TemplateCategory.MINIMAL -> com.webtoapp.core.i18n.Strings.templateMinimal
        TemplateCategory.RETRO -> com.webtoapp.core.i18n.Strings.templateRetro
        TemplateCategory.CYBERPUNK -> com.webtoapp.core.i18n.Strings.templateCyberpunk
        TemplateCategory.NATURE -> com.webtoapp.core.i18n.Strings.templateNature
        TemplateCategory.BUSINESS -> com.webtoapp.core.i18n.Strings.templateBusiness
        TemplateCategory.CREATIVE -> com.webtoapp.core.i18n.Strings.templateCreative
        TemplateCategory.GAME -> com.webtoapp.core.i18n.Strings.templateGame
    }
}

/**
 * Note.
 */
fun StyleReferenceCategory.getLocalizedDisplayName(): String {
    return when (this) {
        StyleReferenceCategory.MOVIE -> com.webtoapp.core.i18n.Strings.styleMovie
        StyleReferenceCategory.BOOK -> com.webtoapp.core.i18n.Strings.styleBook
        StyleReferenceCategory.ANIME -> com.webtoapp.core.i18n.Strings.styleAnime
        StyleReferenceCategory.GAME -> com.webtoapp.core.i18n.Strings.styleGame
        StyleReferenceCategory.BRAND -> com.webtoapp.core.i18n.Strings.styleBrand
        StyleReferenceCategory.ART -> com.webtoapp.core.i18n.Strings.styleArt
        StyleReferenceCategory.ERA -> com.webtoapp.core.i18n.Strings.styleEra
        StyleReferenceCategory.CULTURE -> com.webtoapp.core.i18n.Strings.styleCulture
    }
}
