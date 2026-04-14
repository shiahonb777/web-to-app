package com.webtoapp.core.extension

import android.content.Context
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.extension.agent.*
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * AI.
 *
 * use AI extension.
 *
 * use ModuleAgentEngine.
 * @see com.webtoapp.core.extension.agent.ModuleAgentEngine
 */
class AiModuleDeveloper(private val context: Context) {
    
    companion object {
        private val JSON_BLOCK_REGEX = Regex("```json\\s*([\\s\\S]*?)\\s*```")
        private val JS_BLOCK_REGEX = Regex("```(?:javascript|js)\\s*([\\s\\S]*?)\\s*```")
        private val CSS_BLOCK_REGEX = Regex("```css\\s*([\\s\\S]*?)\\s*```")
    }
    
    private val aiConfigManager = AiConfigManager(context)
    private val aiClient = AiApiClient(context)
    private val gson = com.webtoapp.util.GsonProvider.gson
    
    // Agent.
    val agentEngine = ModuleAgentEngine(context)
    
    /**
     * AI.
     *
     * use agentEngine.develop() Agent.
     */
    suspend fun generateModuleCode(
        prompt: String,
        category: ModuleCategory? = null,
        existingCode: String? = null
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        try {
            // Get AI config.
            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()
            
            if (apiKeys.isEmpty()) {
                return@withContext AiGenerationResult.Error(Strings.aiErrorNoApiKey)
            }
            
            val defaultModelId = aiConfigManager.defaultModelIdFlow.first()
            val savedModel = savedModels.find { it.id == defaultModelId } 
                ?: savedModels.firstOrNull()

            if (savedModel == null) {
                return@withContext AiGenerationResult.Error(Strings.aiErrorNoModel)
            }
            
            val apiKey = apiKeys.find { it.id == savedModel.apiKeyId }
            if (apiKey == null) {
                return@withContext AiGenerationResult.Error(Strings.aiErrorNoApiKeyForModel)
            }
            
            val systemPrompt = buildSystemPrompt(category)
            val userPrompt = buildUserPrompt(prompt, existingCode)
            
            val messages = listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt)
            )
            
            val response = aiClient.chat(apiKey, savedModel.model, messages)
            
            if (response.isSuccess) {
                val content = response.getOrNull() ?: ""
                val parsed = parseAiResponse(content)
                AiGenerationResult.Success(parsed)
            } else {
                AiGenerationResult.Error(response.exceptionOrNull()?.message ?: "Generation failed")
            }
        } catch (e: Exception) {
            AiGenerationResult.Error(e.message ?: Strings.aiErrorUnknown)
        }
    }

    /**
     */
    private fun buildSystemPrompt(category: ModuleCategory?): String {
        val categoryHint = category?.let {
            "用户希望创建一个「${it.getDisplayName()}」类型的模块，${it.getDescription()}。"
        } ?: ""
        
        return """
你是一个专业的 JavaScript/CSS 开发专家，专门为 WebToApp 扩展模块系统编写代码。

## 扩展模块系统说明
WebToApp 扩展模块是注入到网页中执行的 JavaScript/CSS 代码，类似于浏览器扩展或油猴脚本。

## 可用的内置函数
- `getConfig(key, defaultValue)` - 获取用户配置值
- `__MODULE_INFO__` - 包含模块信息的对象 {id, name, version}
- `__MODULE_CONFIG__` - 用户配置值对象

## 代码规范
1. 代码已被包装在 IIFE 中，无需再次包装
2. 使用 MutationObserver 监听 DOM 变化
3. 添加适当的错误处理
4. 代码要简洁高效
5. 添加必要的注释

$categoryHint

## 输出格式
请按以下 JSON 格式输出：

```json
{
  "name": "Module name",
  "description": "Module description",
  "icon": "适合的emoji图标",
  "jsCode": "JavaScript代码",
  "cssCode": "CSS代码（如果需要）",
  "configItems": [
    {
      "key": "配置键",
      "name": "配置名称",
      "type": "TEXT|NUMBER|BOOLEAN|SELECT|TEXTAREA",
      "defaultValue": "默认值",
      "options": ["选项1", "选项2"]
    }
  ]
}
```
        """.trimIndent()
    }
    
    /**
     * use.
     */
    private fun buildUserPrompt(prompt: String, existingCode: String?): String {
        return if (existingCode.isNullOrBlank()) {
            "请根据以下需求创建一个扩展模块：\n\n$prompt"
        } else {
            """
请根据以下需求修改/优化现有代码：

需求：$prompt

现有代码：
```javascript
$existingCode
```
            """.trimIndent()
        }
    }
    
    /**
     * AI.
     */
    private fun parseAiResponse(content: String): AiGeneratedModule {
        try {
            // Extract JSON.
            val jsonMatch = JSON_BLOCK_REGEX.find(content)
            
            if (jsonMatch != null) {
                val jsonStr = jsonMatch.groupValues[1]
                return gson.fromJson(jsonStr, AiGeneratedModule::class.java)
            }
            
            return gson.fromJson(content, AiGeneratedModule::class.java)
        } catch (e: Exception) {
            // Parse Extract.
            val jsCode = JS_BLOCK_REGEX.find(content)?.groupValues?.get(1) ?: content
            val cssCode = CSS_BLOCK_REGEX.find(content)?.groupValues?.get(1) ?: ""
            
            return AiGeneratedModule(
                name = Strings.aiGeneratedModule,
                description = Strings.aiGeneratedModuleDesc,
                icon = "smart_toy",
                jsCode = jsCode,
                cssCode = cssCode,
                configItems = emptyList()
            )
        }
    }
    
    /**
     */
    suspend fun optimizeCode(code: String): AiGenerationResult {
        return generateModuleCode(
            prompt = "请优化以下代码，提高性能和可读性，修复潜在问题",
            existingCode = code
        )
    }
}


/**
 * AI.
 */
sealed class AiGenerationResult {
    data class Success(val module: AiGeneratedModule) : AiGenerationResult()
    data class Error(val message: String) : AiGenerationResult()
}

/**
 * AI.
 */
data class AiGeneratedModule(
    val name: String,
    val description: String,
    val icon: String,
    val jsCode: String,
    val cssCode: String,
    val configItems: List<AiConfigItem>
) {
    /**
     * as ExtensionModule.
     */
    fun toExtensionModule(): ExtensionModule {
        return ExtensionModule(
            name = name.ifBlank { Strings.aiGeneratedModule },
            description = description,
            icon = icon.ifBlank { "🤖" },
            category = ModuleCategory.OTHER,
            code = jsCode,
            cssCode = cssCode,
            configItems = configItems.map { it.toModuleConfigItem() },
            configValues = configItems.associate { it.key to it.defaultValue },
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS),
            runMode = ModuleRunMode.INTERACTIVE
        )
    }
}

/**
 * AI config.
 */
data class AiConfigItem(
    val key: String,
    val name: String,
    val type: String = "TEXT",
    val defaultValue: String = "",
    val options: List<String> = emptyList()
) {
    fun toModuleConfigItem(): ModuleConfigItem {
        val configType = try {
            ConfigItemType.valueOf(type.uppercase())
        } catch (e: Exception) {
            ConfigItemType.TEXT
        }
        
        return ModuleConfigItem(
            key = key,
            name = name,
            type = configType,
            defaultValue = defaultValue,
            options = options
        )
    }
}
