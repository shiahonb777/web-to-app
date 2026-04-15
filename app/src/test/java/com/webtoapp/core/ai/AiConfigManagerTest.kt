package com.webtoapp.core.ai

import android.app.Application
import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

    private class InMemoryPreferencesDataStore(
        initial: Preferences = emptyPreferences()
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private fun createTestDataStore(): DataStore<Preferences> {
        return InMemoryPreferencesDataStore()
    }

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
    fun `test DataStore full save and retrieve end-to-end`() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val dataStore = createTestDataStore()
        val manager = AiConfigManager(context, dataStore)
        assertTrue("should clear previous AI config state", manager.clearAll())

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

        val addKey1 = manager.addApiKey(key1)
        val addKey2 = manager.addApiKey(key2)
        val addKey3 = manager.addApiKey(key3)
        println("[Step 0] addApiKey results: k1=$addKey1, k2=$addKey2, k3=$addKey3")
        assertTrue("k1 should save successfully", addKey1)
        assertTrue("k2 should save successfully", addKey2)
        assertTrue("k3 should save successfully", addKey3)

        val keys = manager.apiKeysFlow.first()
        val keysById = keys.associateBy { it.id }
        println("[Step 1] API Keys saved: ${keys.size}")
        keys.forEach { println("  ${it.id}: provider=${it.provider}, baseUrl=${it.baseUrl}, format=${it.apiFormat}") }

        assertEquals("Should have 3 keys", 3, keys.size)
        assertEquals(AiProvider.GOOGLE, keysById.getValue("k1").provider)
        assertEquals("google-key", keysById.getValue("k1").apiKey)
        assertEquals("DeepSeek 测试", keysById.getValue("k2").alias)
        assertEquals("https://my-api.com", keysById.getValue("k3").baseUrl)
        assertEquals(ApiFormat.ANTHROPIC, keysById.getValue("k3").apiFormat)
        assertEquals("/api/models", keysById.getValue("k3").customModelsEndpoint)
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
        val modelsById = models.associateBy { it.id }
        println("[Step 2] Models saved: ${models.size}")
        models.forEach { println("  ${it.id}: ${it.model.name}, default=${it.isDefault}, fm=${it.featureMappings}") }

        assertEquals("Should have 2 models", 2, models.size)
        assertTrue("Model 1 should be default", modelsById.getValue("m1").isDefault)
        assertEquals(2, modelsById.getValue("m1").featureMappings.size)
        assertNotNull(modelsById.getValue("m1").featureMappings[ModelCapability.TEXT])
        assertNotNull(modelsById.getValue("m1").featureMappings[ModelCapability.CODE])
        assertTrue(modelsById.getValue("m1").featureMappings[ModelCapability.TEXT]!!.contains(AiFeature.TRANSLATION))
        assertEquals(1, modelsById.getValue("m2").featureMappings.size)
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
        assertTrue("updating saved model should succeed", manager.updateSavedModel(updatedModel))
        val updatedSavedModel = manager.getSavedModelById("m1")!!
        assertEquals("My Gemini", updatedSavedModel.alias)
        assertFalse(updatedSavedModel.isDefault)
        println("✅ Step 4: Update model OK")

        // Step 5: delete a model
        assertTrue("deleting saved model should succeed", manager.deleteSavedModel("m2"))
        val modelsAfterDelete = manager.savedModelsFlow.first()
        assertNull(manager.getSavedModelById("m2"))
        assertNotNull(manager.getSavedModelById("m1"))
        assertEquals(1, modelsAfterDelete.count { it.id == "m1" })
        println("✅ Step 5: Delete model OK")

        // Step 6: delete an API key
        assertTrue("deleting API key should succeed", manager.deleteApiKey("k3"))
        val keysAfterDelete = manager.apiKeysFlow.first()
        assertNull(manager.getApiKeyById("k3"))
        assertEquals(1, keysAfterDelete.count { it.id == "k1" })
        assertEquals(1, keysAfterDelete.count { it.id == "k2" })
        println("✅ Step 6: Delete API key OK")

        // Step 7: verify persistence with a new manager instance
        val manager2 = AiConfigManager(context, dataStore)
        val keys2 = manager2.apiKeysFlow.first()
        val models2 = manager2.savedModelsFlow.first()
        assertEquals("Keys should persist", 2, keys2.size)
        assertEquals("Models should persist", 1, models2.size)
        println("✅ Step 7: Persistence across instances OK")

        println("\n✅✅✅ ALL E2E STEPS PASSED ✅✅✅")
    }
}
