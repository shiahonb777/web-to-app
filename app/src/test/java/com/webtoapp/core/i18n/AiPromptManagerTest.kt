package com.webtoapp.core.i18n

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AiPromptManagerTest {

    @Test
    fun `code fix prompt includes error details code and attempt metadata`() {
        val prompt = AiPromptManager.getCodeFixPrompt(
            language = AppLanguage.ENGLISH,
            errorMessages = "Unexpected token at line 1",
            code = "const a = ;",
            attempt = 2,
            maxAttempts = 5
        )

        assertThat(prompt).contains("Attempt 2/5")
        assertThat(prompt).contains("Unexpected token at line 1")
        assertThat(prompt).contains("const a = ;")
        assertThat(prompt).contains("```javascript")
    }

    @Test
    fun `system prompts are non blank for all supported languages`() {
        AppLanguage.entries.forEach { language ->
            assertThat(AiPromptManager.getCodeFixSystemPrompt(language)).isNotEmpty()
        }
    }

    @Test
    fun `module development prompt includes injected hints and native bridge api`() {
        val prompt = AiPromptManager.getModuleDevelopmentSystemPrompt(
            language = AppLanguage.CHINESE,
            categoryHint = "推荐分类：MEDIA",
            existingCodeHint = "已有代码片段：console.log(1)",
            nativeBridgeApi = "NativeBridge.saveImage(url)"
        )

        assertThat(prompt).contains("推荐分类：MEDIA")
        assertThat(prompt).contains("已有代码片段：console.log(1)")
        assertThat(prompt).contains("NativeBridge.saveImage(url)")
        assertThat(prompt).contains("\"js_code\"")
        assertThat(prompt).contains("\"config_items\"")
    }

    @Test
    fun `user message template embeds category and existing code when provided`() {
        val message = AiPromptManager.getUserMessageTemplate(
            language = AppLanguage.ENGLISH,
            requirement = "Remove banner ads",
            categoryName = "CONTENT_FILTER",
            existingCode = "document.querySelector('.ad')?.remove();"
        )

        assertThat(message).contains("Remove banner ads")
        assertThat(message).contains("CONTENT_FILTER")
        assertThat(message).contains("document.querySelector('.ad')?.remove();")
    }
}
