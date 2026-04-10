package com.webtoapp.core.ai.coding

import com.webtoapp.core.i18n.AiPromptManager
import com.webtoapp.core.i18n.Strings

/**
 * HTML编程AI - 提示词与模板管理
 * 
 * 优化原则：
 * 1. 简洁明确，避免冗余
 * 2. 关键规则前置
 * 3. 减少示例代码，让AI自由发挥
 */
object AiCodingPrompts {

    /**
     * 构建系统提示词 - 根据语言设置生成对应语言的提示词
     */
    fun buildSystemPrompt(
        config: SessionConfig,
        hasImageModel: Boolean = false,
        selectedTemplate: StyleTemplate? = null,
        selectedStyle: StyleReference? = null
    ): String {
        return AiPromptManager.getAiCodingSystemPrompt(
            language = Strings.currentLanguage.value,
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
     * 根据模板ID获取模板
     */
    fun getTemplateById(id: String): StyleTemplate? {
        return styleTemplates.find { it.id == id }
    }

    /**
     * 根据风格ID获取风格
     */
    fun getStyleById(id: String): StyleReference? {
        return styleReferences.find { it.id == id }
    }

    /**
     * 预置的风格模板库
     */
    val styleTemplates = listOf(
        StyleTemplate(
            id = "modern-minimal",
            name = Strings.styleModernMinimal,
            category = TemplateCategory.MODERN,
            description = Strings.styleModernMinimalDesc,
            cssFramework = null,
            colorScheme = ColorScheme(
                primary = "#3B82F6",
                secondary = "#6366F1",
                background = "#FFFFFF",
                surface = "#F9FAFB",
                text = "#111827",
                accent = "#10B981"
            ),
            promptHint = Strings.hintModernMinimal
        ),
        StyleTemplate(
            id = "glassmorphism",
            name = Strings.styleGlassmorphism,
            category = TemplateCategory.GLASSMORPHISM,
            description = Strings.styleGlassmorphismDesc,
            colorScheme = ColorScheme(
                primary = "#667EEA",
                secondary = "#764BA2",
                background = "linear-gradient(135deg, #667EEA, #764BA2)",
                surface = "rgba(255,255,255,0.25)",
                text = "#FFFFFF",
                accent = "#F093FB"
            ),
            promptHint = Strings.hintGlassmorphism
        ),
        StyleTemplate(
            id = "neumorphism",
            name = Strings.styleNeumorphism,
            category = TemplateCategory.NEUMORPHISM,
            description = Strings.styleNeumorphismDesc,
            colorScheme = ColorScheme(
                primary = "#6C63FF",
                secondary = "#A29BFE",
                background = "#E0E5EC",
                surface = "#E0E5EC",
                text = "#495057",
                accent = "#6C63FF"
            ),
            promptHint = Strings.hintNeumorphism
        ),
        StyleTemplate(
            id = "dark-mode",
            name = Strings.styleDarkMode,
            category = TemplateCategory.DARK,
            description = Strings.styleDarkModeDesc,
            colorScheme = ColorScheme(
                primary = "#818CF8",
                secondary = "#34D399",
                background = "#0F172A",
                surface = "#1E293B",
                text = "#F1F5F9",
                accent = "#F472B6"
            ),
            promptHint = Strings.hintDarkMode
        ),
        StyleTemplate(
            id = "cyberpunk",
            name = Strings.styleCyberpunk,
            category = TemplateCategory.CYBERPUNK,
            description = Strings.styleCyberpunkDesc,
            colorScheme = ColorScheme(
                primary = "#FF00FF",
                secondary = "#00FFFF",
                background = "#0A0A0A",
                surface = "#1A1A2E",
                text = "#EAEAEA",
                accent = "#FFE600"
            ),
            promptHint = Strings.hintCyberpunk
        ),
        StyleTemplate(
            id = "gradient",
            name = Strings.styleGradient,
            category = TemplateCategory.GRADIENT,
            description = Strings.styleGradientDesc,
            colorScheme = ColorScheme(
                primary = "#EC4899",
                secondary = "#8B5CF6",
                background = "linear-gradient(to right, #EC4899, #8B5CF6)",
                surface = "#FFFFFF",
                text = "#1F2937",
                accent = "#F59E0B"
            ),
            promptHint = Strings.hintGradient
        ),
        StyleTemplate(
            id = "minimal",
            name = Strings.styleMinimal,
            category = TemplateCategory.MINIMAL,
            description = Strings.styleMinimalDesc,
            colorScheme = ColorScheme(
                primary = "#000000",
                secondary = "#666666",
                background = "#FFFFFF",
                surface = "#FAFAFA",
                text = "#000000",
                accent = "#000000"
            ),
            promptHint = Strings.hintMinimal
        ),
        StyleTemplate(
            id = "nature",
            name = Strings.styleNature,
            category = TemplateCategory.NATURE,
            description = Strings.styleNatureDesc,
            colorScheme = ColorScheme(
                primary = "#059669",
                secondary = "#0D9488",
                background = "#ECFDF5",
                surface = "#FFFFFF",
                text = "#064E3B",
                accent = "#F59E0B"
            ),
            promptHint = Strings.hintNature
        ),
        StyleTemplate(
            id = "cute-cartoon",
            name = Strings.styleCuteCartoon,
            category = TemplateCategory.CREATIVE,
            description = Strings.styleCuteCartoonDesc,
            colorScheme = ColorScheme(
                primary = "#FF6B9D",
                secondary = "#C44569",
                background = "#FFF5F7",
                surface = "#FFFFFF",
                text = "#4A4A4A",
                accent = "#FFD93D"
            ),
            promptHint = Strings.hintCuteCartoon
        ),
        StyleTemplate(
            id = "neon-glow",
            name = Strings.styleNeonGlow,
            category = TemplateCategory.DARK,
            description = Strings.styleNeonGlowDesc,
            colorScheme = ColorScheme(
                primary = "#00F5FF",
                secondary = "#FF00E4",
                background = "#0D0D0D",
                surface = "#1A1A1A",
                text = "#FFFFFF",
                accent = "#39FF14"
            ),
            promptHint = Strings.hintNeonGlow
        )
    )

    /**
     * 风格参考词库
     */
    val styleReferences = listOf(
        StyleReference(
            id = "harry-potter",
            name = Strings.styleHarryPotter,
            category = StyleReferenceCategory.MOVIE,
            keywords = listOf("magic", "classic", "mystery", "Hogwarts"),
            description = Strings.styleHarryPotterDesc,
            colorHints = Strings.colorsHarryPotter.split(","),
            elementHints = Strings.elementsHarryPotter.split(",")
        ),
        StyleReference(
            id = "ghibli",
            name = Strings.styleGhibli,
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("Miyazaki", "nature", "warm", "healing"),
            description = Strings.styleGhibliDesc,
            colorHints = Strings.colorsGhibli.split(","),
            elementHints = Strings.elementsGhibli.split(",")
        ),
        StyleReference(
            id = "your-name",
            name = Strings.styleYourName,
            category = StyleReferenceCategory.ANIME,
            keywords = listOf("Shinkai", "light", "youth", "aesthetic"),
            description = Strings.styleYourNameDesc,
            colorHints = Strings.colorsYourName.split(","),
            elementHints = Strings.elementsYourName.split(",")
        ),
        StyleReference(
            id = "apple",
            name = Strings.styleApple,
            category = StyleReferenceCategory.BRAND,
            keywords = listOf("minimal", "elegant", "tech", "premium"),
            description = Strings.styleAppleDesc,
            colorHints = Strings.colorsApple.split(","),
            elementHints = Strings.elementsApple.split(",")
        ),
        StyleReference(
            id = "little-prince",
            name = Strings.styleLittlePrince,
            category = StyleReferenceCategory.BOOK,
            keywords = listOf("fairytale", "starry", "innocent", "poetic"),
            description = Strings.styleLittlePrinceDesc,
            colorHints = Strings.colorsLittlePrince.split(","),
            elementHints = Strings.elementsLittlePrince.split(",")
        ),
        StyleReference(
            id = "zelda-botw",
            name = Strings.styleZeldaBotw,
            category = StyleReferenceCategory.GAME,
            keywords = listOf("adventure", "nature", "cel-shading", "exploration"),
            description = Strings.styleZeldaBotwDesc,
            colorHints = Strings.colorsZelda.split(","),
            elementHints = Strings.elementsZelda.split(",")
        ),
        StyleReference(
            id = "art-deco",
            name = Strings.styleArtDeco,
            category = StyleReferenceCategory.ART,
            keywords = listOf("geometric", "symmetry", "luxury", "1920s"),
            description = Strings.styleArtDecoDesc,
            colorHints = Strings.colorsArtDeco.split(","),
            elementHints = Strings.elementsArtDeco.split(",")
        ),
        StyleReference(
            id = "japanese",
            name = Strings.styleJapanese,
            category = StyleReferenceCategory.CULTURE,
            keywords = listOf("wa", "zen", "whitespace", "traditional"),
            description = Strings.styleJapaneseDesc,
            colorHints = Strings.colorsJapanese.split(","),
            elementHints = Strings.elementsJapanese.split(",")
        )
    )

    /**
     * Rules 模板
     */
    val rulesTemplates: List<RulesTemplate> get() = listOf(
        RulesTemplate(
            id = "chinese",
            name = Strings.rulesChinese,
            description = Strings.rulesChineseDesc,
            rules = listOf(Strings.ruleUseChinese, Strings.ruleChineseComments)
        ),
        RulesTemplate(
            id = "game",
            name = Strings.rulesGame,
            description = Strings.rulesGameDesc,
            rules = listOf(
                Strings.ruleUseChinese,
                Strings.ruleGameFlow,
                Strings.ruleScoreAndInstructions,
                Strings.ruleTouchControl
            )
        ),
        RulesTemplate(
            id = "animation",
            name = Strings.rulesAnimation,
            description = Strings.rulesAnimationDesc,
            rules = listOf(
                Strings.ruleUseChinese,
                Strings.ruleCssAnimation,
                Strings.ruleTransition,
                Strings.rulePerformance
            )
        ),
        RulesTemplate(
            id = "form",
            name = Strings.rulesForm,
            description = Strings.rulesFormDesc,
            rules = listOf(
                Strings.ruleUseChinese,
                Strings.ruleFormValidation,
                Strings.ruleInputLabels,
                Strings.ruleSubmitLoading
            )
        )
    )
}
