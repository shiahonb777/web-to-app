package com.webtoapp.core.extension

import android.content.Context
import com.google.gson.Gson
import com.webtoapp.core.ai.AiApiClient
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.extension.agent.*
import com.webtoapp.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * AI 模块开发器
 * 
 * 使用 AI 辅助生成扩展模块代码
 * 
 * 注意：推荐使用新的 ModuleAgentEngine 获得更好的开发体验
 * @see com.webtoapp.core.extension.agent.ModuleAgentEngine
 */
class AiModuleDeveloper(private val context: Context) {
    
    private val aiConfigManager = AiConfigManager(context)
    private val aiClient = AiApiClient(context)
    private val gson = Gson()
    
    // 新的 Agent 引擎（推荐使用）
    val agentEngine = ModuleAgentEngine(context)
    
    /**
     * AI 生成模块代码（简化版本）
     * 
     * 推荐使用 agentEngine.develop() 获得完整的 Agent 开发体验
     */
    suspend fun generateModuleCode(
        prompt: String,
        category: ModuleCategory? = null,
        existingCode: String? = null
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        try {
            // Get AI 配置
            val apiKeys = aiConfigManager.apiKeysFlow.first()
            val savedModels = aiConfigManager.savedModelsFlow.first()
            
            if (apiKeys.isEmpty()) {
                return@withContext AiGenerationResult.Error("请先在 AI 设置中配置 API Key")
            }
            
            val defaultModelId = aiConfigManager.defaultModelIdFlow.first()
            val savedModel = savedModels.find { it.id == defaultModelId } 
                ?: savedModels.firstOrNull()

            if (savedModel == null) {
                return@withContext AiGenerationResult.Error("请先在 AI 设置中添加并保存模型")
            }
            
            val apiKey = apiKeys.find { it.id == savedModel.apiKeyId }
            if (apiKey == null) {
                return@withContext AiGenerationResult.Error("找不到模型对应的 API Key")
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
            AiGenerationResult.Error(e.message ?: "未知错误")
        }
    }

    /**
     * 构建系统提示词
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
     * 构建用户提示词
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
     * 解析 AI 响应
     */
    private fun parseAiResponse(content: String): AiGeneratedModule {
        try {
            // 提取 JSON 块
            val jsonPattern = Regex("```json\\s*([\\s\\S]*?)\\s*```")
            val jsonMatch = jsonPattern.find(content)
            
            if (jsonMatch != null) {
                val jsonStr = jsonMatch.groupValues[1]
                return gson.fromJson(jsonStr, AiGeneratedModule::class.java)
            }
            
            // 尝试直接解析
            return gson.fromJson(content, AiGeneratedModule::class.java)
        } catch (e: Exception) {
            // Parse失败，尝试提取代码块
            val jsPattern = Regex("```(?:javascript|js)\\s*([\\s\\S]*?)\\s*```")
            val cssPattern = Regex("```css\\s*([\\s\\S]*?)\\s*```")
            
            val jsCode = jsPattern.find(content)?.groupValues?.get(1) ?: content
            val cssCode = cssPattern.find(content)?.groupValues?.get(1) ?: ""
            
            return AiGeneratedModule(
                name = "AI 生成模块",
                description = "由 AI 生成的扩展模块",
                icon = "🤖",
                jsCode = jsCode,
                cssCode = cssCode,
                configItems = emptyList()
            )
        }
    }
    
    /**
     * 优化现有代码
     */
    suspend fun optimizeCode(code: String): AiGenerationResult {
        return generateModuleCode(
            prompt = "请优化以下代码，提高性能和可读性，修复潜在问题",
            existingCode = code
        )
    }
}


/**
 * AI 生成结果
 */
sealed class AiGenerationResult {
    data class Success(val module: AiGeneratedModule) : AiGenerationResult()
    data class Error(val message: String) : AiGenerationResult()
}

/**
 * AI 生成的模块数据
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
     * 转换为 ExtensionModule
     */
    fun toExtensionModule(): ExtensionModule {
        return ExtensionModule(
            name = name.ifBlank { "AI 生成模块" },
            description = description,
            icon = icon.ifBlank { "🤖" },
            category = ModuleCategory.OTHER,
            code = jsCode,
            cssCode = cssCode,
            configItems = configItems.map { it.toModuleConfigItem() },
            configValues = configItems.associate { it.key to it.defaultValue },
            runAt = ModuleRunTime.DOCUMENT_END,
            permissions = listOf(ModulePermission.DOM_ACCESS)
        )
    }
}

/**
 * AI 配置项
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
