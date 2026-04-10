package com.webtoapp.core.extension

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExtensionModuleTest {

    @Test
    fun `matchesUrl returns true when no rules configured`() {
        val module = ExtensionModule(
            name = "No Rules Module",
            code = "console.log('ok')"
        )

        assertThat(module.matchesUrl("https://example.com")).isTrue()
    }

    @Test
    fun `matchesUrl applies include and exclude rules`() {
        val module = ExtensionModule(
            name = "Url Rule Module",
            code = "console.log('ok')",
            urlMatches = listOf(
                UrlMatchRule(pattern = "*example.com*"),
                UrlMatchRule(pattern = "https://admin.example.com/*", exclude = true)
            )
        )

        assertThat(module.matchesUrl("https://shop.example.com/product")).isTrue()
        assertThat(module.matchesUrl("https://admin.example.com/panel")).isFalse()
        assertThat(module.matchesUrl("https://other.com")).isFalse()
    }

    @Test
    fun `invalid regex rule fails closed without crashing`() {
        val module = ExtensionModule(
            name = "Regex Module",
            code = "console.log('ok')",
            urlMatches = listOf(
                UrlMatchRule(pattern = "[invalid-regex", isRegex = true)
            )
        )

        assertThat(module.matchesUrl("https://example.com")).isFalse()
    }

    @Test
    fun `validate detects missing required fields`() {
        val module = ExtensionModule(
            name = "",
            code = "",
            cssCode = "",
            configItems = listOf(
                ModuleConfigItem(
                    key = "token",
                    name = "Token",
                    required = true
                )
            ),
            configValues = emptyMap()
        )

        val errors = module.validate()

        assertThat(errors).contains("模块名称不能为空")
        assertThat(errors).contains("代码内容不能为空")
        assertThat(errors).contains("配置项 'Token' 为必填项")
    }

    @Test
    fun `generateExecutableCode embeds module metadata and config`() {
        val module = ExtensionModule(
            id = "module-1",
            name = "Injector",
            icon = "🚀",
            code = "window.__flag = getConfig('flag', 'off')",
            cssCode = "body { background: #000; }",
            configValues = mapOf("flag" to "on"),
            uiConfig = ModuleUiConfig(type = ModuleUiType.MINI_BUTTON)
        )

        val executable = module.generateExecutableCode()

        assertThat(executable).contains("const __MODULE_CONFIG__")
        assertThat(executable).contains("\"flag\":\"on\"")
        assertThat(executable).contains("id: 'module-1'")
        assertThat(executable).contains("name: 'Injector'")
        assertThat(executable).contains("ext-module-module-1")
        assertThat(executable).contains("window.__flag = getConfig('flag', 'off')")
    }
}

