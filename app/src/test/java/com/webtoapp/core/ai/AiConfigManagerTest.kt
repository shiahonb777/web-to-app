package com.webtoapp.core.ai

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

class TestApplication : Application()

/**
 * Save and load tests for `AiConfigManager`.
 *
 * Robolectric keeps DataStore as a singleton, so state leaks across tests.
 * DataStore scenarios stay in one ordered test to avoid false failures.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApplication::class)
class AiConfigManagerTest {

    private val gson = Gson()

    // Gson round-trip tests without DataStore

    @Test
    fun `test Gson ApiKeyConfig roundtrip`() {
        val config = ApiKeyConfig(
            id = "gson-1", provider = AiProvider.OPENROUTER, apiKey = "or-key",
            baseUrl = "https://openrouter.ai/api", apiFormat = ApiFormat.OPENAI_COMPATIBLE
        )
        val json = gson.toJson(listOf(config))
        val type = object : TypeToken<List<ApiKeyConfig>>() {}.type
        val result: List<ApiKeyConfig> = gson.fromJson(json, type)
        assertEquals(1, result.size)
        assertEquals(AiProvider.OPENROUTER, result[0].provider)
        assertEquals("or-key", result[0].apiKey)
        println("✅ ApiKeyConfig Gson roundtrip OK")
    }

    @Test
    fun `test Gson SavedModel with featureMappings roundtrip`() {
        val model = SavedModel(
            id = "gson-m1",
            model = AiModel(id = "test-model", name = "Test Model", provider = AiProvider.OPENAI),
            apiKeyId = "key-1",
            capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE),
            featureMappings = mapOf(
                ModelCapability.TEXT to setOf(AiFeature.AI_CODING, AiFeature.GENERAL),
                ModelCapability.CODE to setOf(AiFeature.AI_CODING)
            )
        )
        val json = gson.toJson(listOf(model))
        println("SavedModel JSON: $json")

        val type = object : TypeToken<List<SavedModel>>() {}.type
        val result: List<SavedModel> = gson.fromJson(json, type)
        assertEquals(1, result.size)
        val fm = result[0].featureMappings
        assertEquals(2, fm.size)
        assertNotNull(fm[ModelCapability.TEXT])
        assertTrue(fm[ModelCapability.TEXT]!!.contains(AiFeature.AI_CODING))
        println("✅ SavedModel featureMappings Gson roundtrip OK")
        println("  Key types: ${fm.keys.map { it::class.java.name }}")
    }

    @Test
    fun `test Gson null safety`() {
        val type = object : TypeToken<List<ApiKeyConfig>>() {}.type
        val empty: List<ApiKeyConfig> = gson.fromJson("[]", type)
        assertEquals(0, empty.size)
        val nullResult: List<ApiKeyConfig>? = gson.fromJson("null", type)
        assertNull(nullResult)
        println("✅ Gson null safety OK")
    }

    @Test
    fun `test Gson all providers serialize and deserialize`() {
        // Verify every AiProvider round-trips through Gson.
        AiProvider.entries.forEach { provider ->
            val config = ApiKeyConfig(id = "test-${provider.name}", provider = provider, apiKey = "key")
            val json = gson.toJson(config)
            val result = gson.fromJson(json, ApiKeyConfig::class.java)
            assertEquals("Provider ${provider.name} should roundtrip", provider, result.provider)
        }
        println("✅ All ${AiProvider.entries.size} providers serialize/deserialize OK")
    }

    @Test
    fun `test Gson all ApiFormat values serialize and deserialize`() {
        ApiFormat.entries.forEach { format ->
            val config = ApiKeyConfig(id = "test", provider = AiProvider.CUSTOM, apiKey = "key", apiFormat = format)
            val json = gson.toJson(config)
            val result = gson.fromJson(json, ApiKeyConfig::class.java)
            assertEquals("ApiFormat ${format.name} should roundtrip", format, result.apiFormat)
        }
        println("✅ All ${ApiFormat.entries.size} API formats serialize/deserialize OK")
    }

    // Base64 compatibility checks

    @Test
    fun `test Base64 NO_WRAP vs DEFAULT with binary data`() {
        // Simulate AES-GCM output bytes.
        val iv = ByteArray(12) { (it * 17).toByte() }
        val ciphertext = ByteArray(100) { (it * 31 + 7).toByte() }
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        val encodedNoWrap = Base64.encodeToString(combined, Base64.NO_WRAP)

        // Expected path: NO_WRAP -> NO_WRAP.
        val decodedCorrect = Base64.decode(encodedNoWrap, Base64.NO_WRAP)
        assertArrayEquals("NO_WRAP→NO_WRAP should match", combined, decodedCorrect)
        println("✅ Base64 NO_WRAP roundtrip OK")

        // Legacy bug path: NO_WRAP -> DEFAULT.
        val decodedOld = Base64.decode(encodedNoWrap, Base64.DEFAULT)
        val matches = combined.contentEquals(decodedOld)
        println("NO_WRAP encode → DEFAULT decode matches? $matches")
        println("  (Robolectric 中可能兼容, 真机行为可能不同)")
    }

    // Full DataStore end-to-end flow
    // Keep this in one test to avoid singleton state leakage.

    @Test
    fun `test DataStore full save and retrieve end-to-end`() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        val manager = AiConfigManager(context)

        println("=== DataStore E2E Test ===")

        // Step 1: add API keys
        val key1 = ApiKeyConfig(id = "k1", provider = AiProvider.GOOGLE, apiKey = "google-key")
        val key2 = ApiKeyConfig(
            id = "k2", provider = AiProvider.DEEPSEEK, apiKey = "ds-key",
            alias = "DeepSeek 测试"
        )
        val key3 = ApiKeyConfig(
            id = "k3", provider = AiProvider.CUSTOM, apiKey = "custom-key",
            baseUrl = "https://my-api.com", apiFormat = ApiFormat.ANTHROPIC,
            customModelsEndpoint = "/api/models", customChatEndpoint = "/api/chat"
        )

        manager.addApiKey(key1)
        manager.addApiKey(key2)
        manager.addApiKey(key3)

        val keys = manager.apiKeysFlow.first()
        println("[Step 1] API Keys saved: ${keys.size}")
        keys.forEach { println("  ${it.id}: provider=${it.provider}, baseUrl=${it.baseUrl}, format=${it.apiFormat}") }

        assertEquals("Should have 3 keys", 3, keys.size)
        assertEquals(AiProvider.GOOGLE, keys[0].provider)
        assertEquals("google-key", keys[0].apiKey)
        assertEquals("DeepSeek 测试", keys[1].alias)
        assertEquals("https://my-api.com", keys[2].baseUrl)
        assertEquals(ApiFormat.ANTHROPIC, keys[2].apiFormat)
        assertEquals("/api/models", keys[2].customModelsEndpoint)
        println("✅ Step 1: API Keys save/read OK")

        // Step 2: save models with feature mappings
        val model1 = SavedModel(
            id = "m1",
            model = AiModel(id = "gemini-2.0-flash", name = "Gemini 2.0 Flash", provider = AiProvider.GOOGLE),
            apiKeyId = "k1",
            capabilities = listOf(ModelCapability.TEXT, ModelCapability.CODE),
            featureMappings = mapOf(
                ModelCapability.TEXT to setOf(AiFeature.GENERAL, AiFeature.TRANSLATION),
                ModelCapability.CODE to setOf(AiFeature.AI_CODING)
            ),
            isDefault = true
        )
        val model2 = SavedModel(
            id = "m2",
            model = AiModel(id = "deepseek-chat", name = "DeepSeek Chat", provider = AiProvider.DEEPSEEK),
            apiKeyId = "k2",
            capabilities = listOf(ModelCapability.TEXT),
            featureMappings = mapOf(ModelCapability.TEXT to setOf(AiFeature.GENERAL))
        )

        manager.saveModel(model1)
        manager.saveModel(model2)

        val models = manager.savedModelsFlow.first()
        println("[Step 2] Models saved: ${models.size}")
        models.forEach { println("  ${it.id}: ${it.model.name}, default=${it.isDefault}, fm=${it.featureMappings}") }

        assertEquals("Should have 2 models", 2, models.size)
        assertTrue("Model 1 should be default", models[0].isDefault)
        assertEquals(2, models[0].featureMappings.size)
        assertNotNull(models[0].featureMappings[ModelCapability.TEXT])
        assertNotNull(models[0].featureMappings[ModelCapability.CODE])
        assertTrue(models[0].featureMappings[ModelCapability.TEXT]!!.contains(AiFeature.TRANSLATION))
        assertEquals(1, models[1].featureMappings.size)
        println("✅ Step 2: Models with featureMappings save/read OK")

        // Step 3: load by ID
        val foundKey = manager.getApiKeyById("k1")
        val foundModel = manager.getSavedModelById("m1")
        assertNotNull("Should find key k1", foundKey)
        assertNotNull("Should find model m1", foundModel)
        assertEquals(AiProvider.GOOGLE, foundKey!!.provider)
        assertEquals("Gemini 2.0 Flash", foundModel!!.model.name)
        println("✅ Step 3: Find by ID OK")

        // Step 4: update a model
        val updatedModel = model1.copy(alias = "My Gemini", isDefault = false)
        manager.updateSavedModel(updatedModel)
        val modelsAfterUpdate = manager.savedModelsFlow.first()
        assertEquals("My Gemini", modelsAfterUpdate[0].alias)
        assertFalse(modelsAfterUpdate[0].isDefault)
        println("✅ Step 4: Update model OK")

        // Step 5: delete a model
        manager.deleteSavedModel("m2")
        val modelsAfterDelete = manager.savedModelsFlow.first()
        assertEquals(1, modelsAfterDelete.size)
        assertEquals("m1", modelsAfterDelete[0].id)
        println("✅ Step 5: Delete model OK")

        // Step 6: delete an API key
        manager.deleteApiKey("k3")
        val keysAfterDelete = manager.apiKeysFlow.first()
        assertEquals(2, keysAfterDelete.size)
        assertTrue(keysAfterDelete.none { it.id == "k3" })
        println("✅ Step 6: Delete API key OK")

        // Step 7: verify persistence with a new manager instance
        val manager2 = AiConfigManager(context)
        val keys2 = manager2.apiKeysFlow.first()
        val models2 = manager2.savedModelsFlow.first()
        assertEquals("Keys should persist", 2, keys2.size)
        assertEquals("Models should persist", 1, models2.size)
        println("✅ Step 7: Persistence across instances OK")

        println("\n✅✅✅ ALL E2E STEPS PASSED ✅✅✅")
    }
}
