package com.webtoapp.core.extension

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.runner.RunWith
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DeclarativeNetRequestEngineTest {

    private val gson = Gson()
    private val stringListType = object : TypeToken<List<String>>() {}.type
    private val mapListType = object : TypeToken<List<Map<String, Any?>>>() {}.type

    @Before
    fun setUp() {
        DeclarativeNetRequestEngine.clear()
    }

    @After
    fun tearDown() {
        DeclarativeNetRequestEngine.clear()
    }

    @Test
    fun `static rulesets can be enabled and disabled independently`() {
        DeclarativeNetRequestEngine.loadStaticRules(
            extensionId = "ext-a",
            rulesetId = "rules_a",
            path = "rules-a.json",
            rulesJson = """
                [{"id":1,"priority":1,"action":{"type":"block"},"condition":{"urlFilter":"example.com"}}]
            """.trimIndent(),
            enabled = true
        )
        DeclarativeNetRequestEngine.loadStaticRules(
            extensionId = "ext-a",
            rulesetId = "rules_b",
            path = "rules-b.json",
            rulesJson = """
                [{"id":2,"priority":1,"action":{"type":"block"},"condition":{"urlFilter":"blocked.com"}}]
            """.trimIndent(),
            enabled = false
        )

        assertThat(parseStringList(DeclarativeNetRequestEngine.getEnabledStaticRulesetIdsJson("ext-a"))).hasSize(1)
        assertThat(DeclarativeNetRequestEngine.evaluate("https://blocked.com", "script")).isNull()

        DeclarativeNetRequestEngine.updateEnabledStaticRulesets(
            "ext-a",
            """["rules_b"]""",
            """["rules_a"]"""
        )

        val enabledIds = parseStringList(DeclarativeNetRequestEngine.getEnabledStaticRulesetIdsJson("ext-a"))
        assertThat(enabledIds).hasSize(1)
        assertThat(enabledIds.first()).isEqualTo("rules_b")
        assertThat(DeclarativeNetRequestEngine.evaluate("https://example.com", "script")).isNull()
        assertThat(DeclarativeNetRequestEngine.evaluate("https://blocked.com", "script")?.action)
            .isEqualTo(DeclarativeNetRequestEngine.ActionType.BLOCK)
    }

    @Test
    fun `session rules are returned and dynamic updates replace same rule id`() {
        DeclarativeNetRequestEngine.updateSessionRules(
            "ext-a",
            """
                [{"id":7,"priority":1,"action":{"type":"block"},"condition":{"urlFilter":"session.test"}}]
            """.trimIndent(),
            "[]"
        )
        DeclarativeNetRequestEngine.updateDynamicRules(
            "ext-a",
            """
                [{"id":9,"priority":1,"action":{"type":"allow"},"condition":{"urlFilter":"dynamic.test"}}]
            """.trimIndent(),
            "[]"
        )
        DeclarativeNetRequestEngine.updateDynamicRules(
            "ext-a",
            """
                [{"id":9,"priority":2,"action":{"type":"block"},"condition":{"urlFilter":"dynamic.test"}}]
            """.trimIndent(),
            "[]"
        )

        val sessionRules = parseRuleMaps(DeclarativeNetRequestEngine.getSessionRulesJson("ext-a"))
        val dynamicRules = parseRuleMaps(DeclarativeNetRequestEngine.getDynamicRulesJson("ext-a"))

        assertThat(sessionRules).hasSize(1)
        assertThat((sessionRules[0]["id"] as Number).toInt()).isEqualTo(7)
        assertThat(dynamicRules).hasSize(1)
        val action = dynamicRules[0]["action"] as Map<*, *>
        assertThat(action["type"]).isEqualTo("block")
        assertThat(DeclarativeNetRequestEngine.evaluate("https://dynamic.test", "script")?.action)
            .isEqualTo(DeclarativeNetRequestEngine.ActionType.BLOCK)
    }

    private fun parseStringList(json: String): List<String> {
        return gson.fromJson(json, stringListType) ?: emptyList()
    }

    private fun parseRuleMaps(json: String): List<Map<String, Any?>> {
        return gson.fromJson(json, mapListType) ?: emptyList()
    }
}
