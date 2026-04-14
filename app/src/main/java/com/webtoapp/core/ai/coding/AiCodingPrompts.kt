package com.webtoapp.core.ai.coding

import com.webtoapp.core.i18n.AiPromptManager
import com.webtoapp.core.i18n.AppStringsProvider

/**
 * HTML AI -.
 * 
 * Note.
 * Note.
 * Note.
 * 3. AI.
 */
object AiCodingPrompts {

    /**
     * Note.
     */
    fun buildSystemPrompt(
        config: SessionConfig,
        hasImageModel: Boolean = false,
        selectedTemplate: StyleTemplate? = null,
        selectedStyle: StyleReference? = null
    ): String {
        return AiPromptManager.getAiCodingSystemPrompt(
            language = AppStringsProvider.currentLanguage,
            rules = config.getEffectiveRules(),
            hasImageModel = hasImageModel,
            templateName = selectedTemplate?.name,
            templateDesc = selectedTemplate?.description,
            templatePromptHint = selectedTemplate?.promptHint,
            colorScheme = selectedTemplate?.colorScheme?.let { c ->
                "Primary${c.primary} Background${c.background} Text${c.text}"
            },
            styleName = selectedStyle?.name,
            styleDesc = selectedStyle?.description,
            styleKeywords = selectedStyle?.keywords?.joinToString(", "),
            styleColors = selectedStyle?.colorHints?.joinToString(", ")
        )
    }

    /**
     * ID.
     */
    fun getTemplateById(id: String): StyleTemplate? {
        return styleTemplates.find { it.id == id }
    }

    /**
     * ID.
     */
    fun getStyleById(id: String): StyleReference? {
        return styleReferences.find { it.id == id }
    }

    /**
     * Note.
     */
    val styleTemplates = listOf(
        StyleTemplate(
            id = "modern-minimal",
            name = AppStringsProvider.current().styleModernMinimal,
            category = TemplateCategory.MODERN,
            description = AppStringsProvider.current().styleModernMinimalDesc,
            cssFramework = null,
            colorScheme = ColorScheme(
                primary = "#3B82F6",
                secondary = "#6366F1",
                background = "#FFFFFF",
                surface = "#F9FAFB",
                text = "#111827",
                accent = "#10B981"
            ),
            promptHint = AppStringsProvider.current().hintModernMinimal
        ),
        StyleTemplate(
            id = "glassmorphism",
            name = AppStringsProvider.current().styleGlassmorphism,
            category = TemplateCategory.GLASSMORPHISM,
            description = AppStringsProvider.current().styleGlassmorphismDesc,
            colorScheme = ColorScheme(
                primary = "#667EEA",
                secondary = "#764BA2",
                background = "linear-gradient(135deg, #667EEA, #764BA2)",
                surface = "rgba(255,255,255,0.25)",
                text = "#FFFFFF",
                accent = "#F093FB"
            ),
            promptHint = AppStringsProvider.current().hintGlassmorphism
        ),
        StyleTemplate(
            id = "neumorphism",
            name = AppStringsProvider.current().styleNeumorphism,
            category = TemplateCategory.NEUMORPHISM,
            description = AppStringsProvider.current().styleNeumorphismDesc,
            colorScheme = ColorScheme(
                primary = "#6C63FF",
                secondary = "#A29BFE",
                background = "#E0E5EC",
                surface = "#E0E5EC",
                text = "#495057",
                accent = "#6C63FF"
            ),
            promptHint = AppStringsProvider.current().hintNeumorphism
        ),
        StyleTemplate(
            id = "dark-mode",
            name = AppStringsProvider.current().styleDarkMode,
            category = TemplateCategory.DARK,
            description = AppStringsProvider.current().styleDarkModeDesc,
            colorScheme = ColorScheme(
                primary = "#818CF8",
                secondary = "#34D399",
                background = "#0F172A",
                surface = "#1E293B",
                text = "#F1F5F9",
                accent = "#F472B6"
            ),
            promptHint = AppStringsProvider.current().hintDarkMode
        ),
        StyleTemplate(
            id = "cyberpunk",
            name = AppStringsProvider.current().styleCyberpunk,
            category = TemplateCategory.CYBERPUNK,
            description = AppStringsProvider.current().styleCyberpunkDesc,
            colorScheme = ColorScheme(
                primary = "#FF00FF",
                secondary = "#00FFFF",
                background = "#0A0A0A",
                surface = "#1A1A2E",
                text = "#EAEAEA",
                accent = "#FFE600"
            ),
            promptHint = AppStringsProvider.current().hintCyberpunk
        ),
        StyleTemplate(
            id = "gradient",
            name = AppStringsProvider.current().styleGradient,
            category = TemplateCategory.GRADIENT,
            description = AppStringsProvider.current().styleGradientDesc,
            colorScheme = ColorScheme(
                primary = "#EC4899",
                secondary = "#8B5CF6",
                background = "linear-gradient(to right, #EC4899, #8B5CF6)",
                surface = "#FFFFFF",
                text = "#1F2937",
                accent = "#F59E0B"
            ),
            promptHint = AppStringsProvider.current().hintGradient
        ),
        StyleTemplate(
            id = "minimal",
            name = AppStringsProvider.current().styleMinimal,
            category = TemplateCategory.MINIMAL,
            description = AppStringsProvider.current().styleMinimalDesc,
            colorScheme = ColorScheme(
                primary = "#000000",
                secondary = "#666666",
                background = "#FFFFFF",
                surface = "#FAFAFA",
                text = "#000000",
                accent = "#000000"
            ),
            promptHint = AppStringsProvider.current().hintMinimal
        ),
        StyleTemplate(
            id = "nature",
            name = AppStringsProvider.current().styleNature,
            category = TemplateCategory.NATURE,
            description = AppStringsProvider.current().styleNatureDesc,
            colorScheme = ColorScheme(
                primary = "#059669",
                secondary = "#0D9488",
                background = "#ECFDF5",
                surface = "#FFFFFF",
                text = "#064E3B",
                accent = "#F59E0B"
            ),
            promptHint = AppStringsProvider.current().hintNature
        ),
        StyleTemplate(
            id = "cute-cartoon",
            name = AppStringsProvider.current().styleCuteCartoon,
            category = TemplateCategory.CREATIVE,
            description = AppStringsProvider.current().styleCuteCartoonDesc,
            colorScheme = ColorScheme(
                primary = "#FF6B9D",
                secondary = "#C44569",
                background = "#FFF5F7",
                surface = "#FFFFFF",
                text = "#4A4A4A",
                accent = "#FFD93D"
            ),
            promptHint = AppStringsProvider.current().hintCuteCartoon
        ),
        StyleTemplate(
            id = "neon-glow",
            name = AppStringsProvider.current().styleNeonGlow,
            category = TemplateCategory.DARK,
            description = AppStringsProvider.current().styleNeonGlowDesc,
            colorScheme = ColorScheme(
                primary = "#00F5FF",
                secondary = "#FF00E4",
                background = "#0D0D0D",
                surface = "#1A1A1A",
                text = "#FFFFFF",
                accent = "#39FF14"
            ),
            promptHint = AppStringsProvider.current().hintNeonGlow
        )
    )

    /**
     * Note.
     */
    val styleReferences = listOf(
        StyleReference(
            id = "harry-potter",
            name = AppStringsProvider.current().styleHarryPotter,
            category = StyleReferenceCategory.MOVIE,
            keywords = listOf("magic", "classic", "mystery", "Hogwarts"),
            description = AppStringsProvider.current().styleHarryPotterDesc,
            colorHints = AppStringsProvider.current().colorsHarryPotter.split(","),
            elementHints = AppStringsProvider.current().elementsHarryPotter.split(",")
        ),
        StyleReference(
            id = "ghibli",
            name = AppStringsProvider.current().styleGhibli,
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("Miyazaki", "nature", "warm", "healing"),
            description = AppStringsProvider.current().styleGhibliDesc,
            colorHints = AppStringsProvider.current().colorsGhibli.split(","),
            elementHints = AppStringsProvider.current().elementsGhibli.split(",")
        ),
        StyleReference(
            id = "your-name",
            name = AppStringsProvider.current().styleYourName,
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("Shinkai", "light", "youth", "aesthetic"),
            description = AppStringsProvider.current().styleYourNameDesc,
            colorHints = AppStringsProvider.current().colorsYourName.split(","),
            elementHints = AppStringsProvider.current().elementsYourName.split(",")
        ),
        StyleReference(
            id = "apple",
            name = AppStringsProvider.current().styleApple,
            category = StyleReferenceCategory.BRAND,
            keywords = listOf("minimal", "elegant", "tech", "premium"),
            description = AppStringsProvider.current().styleAppleDesc,
            colorHints = AppStringsProvider.current().colorsApple.split(","),
            elementHints = AppStringsProvider.current().elementsApple.split(",")
        ),
        StyleReference(
            id = "little-prince",
            name = AppStringsProvider.current().styleLittlePrince,
            category = StyleReferenceCategory.BOOK,
            keywords = listOf("fairytale", "starry", "innocent", "poetic"),
            description = AppStringsProvider.current().styleLittlePrinceDesc,
            colorHints = AppStringsProvider.current().colorsLittlePrince.split(","),
            elementHints = AppStringsProvider.current().elementsLittlePrince.split(",")
        ),
        StyleReference(
            id = "zelda-botw",
            name = AppStringsProvider.current().styleZeldaBotw,
            category = StyleReferenceCategory.GAME,
            keywords = listOf("adventure", "nature", "cel-shading", "exploration"),
            description = AppStringsProvider.current().styleZeldaBotwDesc,
            colorHints = AppStringsProvider.current().colorsZelda.split(","),
            elementHints = AppStringsProvider.current().elementsZelda.split(",")
        ),
        StyleReference(
            id = "art-deco",
            name = AppStringsProvider.current().styleArtDeco,
            category = StyleReferenceCategory.ART,
            keywords = listOf("geometric", "symmetry", "luxury", "1920s"),
            description = AppStringsProvider.current().styleArtDecoDesc,
            colorHints = AppStringsProvider.current().colorsArtDeco.split(","),
            elementHints = AppStringsProvider.current().elementsArtDeco.split(",")
        ),
        StyleReference(
            id = "japanese",
            name = AppStringsProvider.current().styleJapanese,
            category = StyleReferenceCategory.CULTURE,
            keywords = listOf("wa", "zen", "whitespace", "traditional"),
            description = AppStringsProvider.current().styleJapaneseDesc,
            colorHints = AppStringsProvider.current().colorsJapanese.split(","),
            elementHints = AppStringsProvider.current().elementsJapanese.split(",")
        )
    )

    /**
     * Rules.
     */
    val rulesTemplates: List<RulesTemplate> get() = listOf(
        RulesTemplate(
            id = "chinese",
            name = AppStringsProvider.current().rulesChinese,
            description = AppStringsProvider.current().rulesChineseDesc,
            rules = listOf(AppStringsProvider.current().ruleUseChinese, AppStringsProvider.current().ruleChineseComments)
        ),
        RulesTemplate(
            id = "game",
            name = AppStringsProvider.current().rulesGame,
            description = AppStringsProvider.current().rulesGameDesc,
            rules = listOf(
                AppStringsProvider.current().ruleUseChinese,
                AppStringsProvider.current().ruleGameFlow,
                AppStringsProvider.current().ruleScoreAndInstructions,
                AppStringsProvider.current().ruleTouchControl
            )
        ),
        RulesTemplate(
            id = "animation",
            name = AppStringsProvider.current().rulesAnimation,
            description = AppStringsProvider.current().rulesAnimationDesc,
            rules = listOf(
                AppStringsProvider.current().ruleUseChinese,
                AppStringsProvider.current().ruleCssAnimation,
                AppStringsProvider.current().ruleTransition,
                AppStringsProvider.current().rulePerformance
            )
        ),
        RulesTemplate(
            id = "form",
            name = AppStringsProvider.current().rulesForm,
            description = AppStringsProvider.current().rulesFormDesc,
            rules = listOf(
                AppStringsProvider.current().ruleUseChinese,
                AppStringsProvider.current().ruleFormValidation,
                AppStringsProvider.current().ruleInputLabels,
                AppStringsProvider.current().ruleSubmitLoading
            )
        )
    )
}
