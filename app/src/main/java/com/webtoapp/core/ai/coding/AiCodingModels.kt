package com.webtoapp.core.ai.coding

import com.webtoapp.core.i18n.Strings

import java.util.UUID








data class AiCodingSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val messages: List<AiCodingMessage> = emptyList(),
    val checkpoints: List<ProjectCheckpoint> = emptyList(),
    val currentCheckpointIndex: Int = -1,
    val config: SessionConfig = SessionConfig(),
    val projectDir: String? = null,
    val codingType: AiCodingType = AiCodingType.HTML,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)




data class SessionConfig(
    val textModelId: String? = null,
    val imageModelId: String? = null,
    val temperature: Float = 0.7f,
    val rules: List<String> = emptyList(),
    val selectedTemplateId: String? = null,
    val selectedStyleId: String? = null,

    val enabledTools: Set<AiCodingToolType> = setOf(AiCodingToolType.WRITE_HTML)
) {



    fun getEffectiveRules(): List<String> {
        return if (rules.isEmpty()) {
            listOf(Strings.ruleUseChinese)
        } else {
            rules
        }
    }
}




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




data class AiCodingMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val images: List<String> = emptyList(),
    val thinking: String? = null,
    val codeBlocks: List<CodeBlock> = emptyList(),
    val fileRefs: List<FileReference> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val originalContent: String? = null
)




data class FileReference(
    val filename: String,
    val baseName: String,
    val version: Int,
    val type: ProjectFileType,
    val createdAt: Long = System.currentTimeMillis()
)




enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}




data class CodeBlock(
    val id: String = UUID.randomUUID().toString(),
    val language: String = "html",
    val filename: String? = null,
    val content: String,
    val isComplete: Boolean = true
)




data class ProjectCheckpoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val messageIndex: Int,
    val files: List<ProjectFile>,
    val timestamp: Long = System.currentTimeMillis()
)




data class ProjectFile(
    val name: String,
    val content: String,
    val type: ProjectFileType = ProjectFileType.HTML
)




enum class ProjectFileType(val extension: String, val mimeType: String) {
    HTML("html", "text/html"),
    CSS("css", "text/css"),
    JS("js", "application/javascript"),
    SVG("svg", "image/svg+xml"),
    JSON("json", "application/json"),
    IMAGE("png", "image/png"),
    OTHER("txt", "text/plain")
}




data class StyleTemplate(
    val id: String,
    val name: String,
    val category: TemplateCategory,
    val description: String,
    val previewImage: String? = null,
    val cssFramework: String? = null,
    val colorScheme: ColorScheme? = null,
    val promptHint: String,
    val exampleCode: String? = null
)




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




data class ColorScheme(
    val primary: String,
    val secondary: String,
    val background: String,
    val surface: String,
    val text: String,
    val accent: String
)




data class StyleReference(
    val id: String,
    val name: String,
    val category: StyleReferenceCategory,
    val keywords: List<String>,
    val description: String,
    val colorHints: List<String>,
    val elementHints: List<String>
)




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




data class RulesTemplate(
    val id: String,
    val name: String,
    val description: String,
    val rules: List<String>
)




data class ImageGenerationRequest(
    val prompt: String,
    val negativePrompt: String? = null,
    val width: Int = 512,
    val height: Int = 512,
    val style: String? = null
)




data class ImageGenerationResult(
    val success: Boolean,
    val imageUrl: String? = null,
    val localPath: String? = null,
    val error: String? = null
)




data class ParsedAiResponse(
    val textContent: String,
    val thinking: String?,
    val codeBlocks: List<CodeBlock>,
    val imageRequests: List<ImageGenerationRequest>
)




sealed class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    data class Streaming(val partialContent: String) : ChatState()
    data class GeneratingImage(val prompt: String) : ChatState()
    data class Error(val message: String) : ChatState()
}




data class SaveConfig(
    val directory: String,
    val projectName: String,
    val createFolder: Boolean = true,
    val overwrite: Boolean = false
)




data class CodeLibraryItem(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val messageId: String,
    val title: String,
    val description: String = "",
    val files: List<ProjectFile>,
    val previewHtml: String,
    val conversationContext: String,
    val userPrompt: String,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val isFavorite: Boolean = false
)




data class ConversationCheckpoint(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val name: String,
    val messageCount: Int,
    val messages: List<AiCodingMessage>,
    val codeLibraryIds: List<String>,
    val config: SessionConfig,
    val timestamp: Long = System.currentTimeMillis()
)




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
