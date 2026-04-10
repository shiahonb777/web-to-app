package com.webtoapp.ui.components.aimodule

import androidx.compose.animation.*
import com.webtoapp.ui.components.PremiumButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AiFeature
import com.webtoapp.data.model.AiProvider
import com.webtoapp.data.model.SavedModel
import androidx.compose.ui.graphics.Color

/**
 * 模型选择器组件
 * 
 * 用于在 AI 模块开发界面选择要使用的 AI 模型
 * 
 * @param selectedModel 当前选中的模型
 * @param availableModels 可用的模型列表（已过滤支持 MODULE_DEVELOPMENT 的模型）
 * @param onModelSelected 模型选择回调
 * @param onConfigureClick 点击配置按钮的回调（跳转到 AI 设置）
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelector(
    selectedModel: SavedModel?,
    availableModels: List<SavedModel>,
    onModelSelected: (SavedModel) -> Unit,
    onConfigureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Filter支持 MODULE_DEVELOPMENT 的模型
    val filteredModels = remember(availableModels) {
        filterModelsForModuleDevelopment(availableModels)
    }
    
    Box(modifier = modifier) {
        // Select器按钮
        ModelSelectorButton(
            selectedModel = selectedModel,
            hasModels = filteredModels.isNotEmpty(),
            onClick = { 
                if (filteredModels.isNotEmpty()) {
                    expanded = true
                } else {
                    onConfigureClick()
                }
            }
        )
        
        // 下拉菜单
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .widthIn(min = 280.dp, max = 360.dp)
                .heightIn(max = 400.dp)
        ) {
            if (filteredModels.isEmpty()) {
                // Empty状态
                EmptyModelState(
                    onConfigureClick = {
                        expanded = false
                        onConfigureClick()
                    }
                )
            } else {
                // 模型列表
                Text(
                    Strings.selectModel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                filteredModels.forEach { model ->
                    ModelDropdownItem(
                        model = model,
                        isSelected = selectedModel?.id == model.id,
                        onClick = {
                            onModelSelected(model)
                            expanded = false
                        }
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Configure更多模型
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                Strings.configureMoreModels,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onConfigureClick()
                    }
                )
            }
        }
    }
}

/**
 * 模型选择器按钮
 */
@Composable
private fun ModelSelectorButton(
    selectedModel: SavedModel?,
    hasModels: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (com.webtoapp.ui.theme.LocalIsDarkTheme.current) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.72f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 供应商图标
            ProviderIcon(
                provider = selectedModel?.model?.provider,
                modifier = Modifier.size(28.dp)
            )
            
            // 模型信息
            Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                if (selectedModel != null) {
                    Text(
                        selectedModel.alias ?: selectedModel.model.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        selectedModel.model.provider.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (!hasModels) {
                    Text(
                        Strings.noModelConfigured,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        Strings.clickToConfigureAiModel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        Strings.selectModel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 下拉箭头
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = Strings.expand,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 模型下拉项
 */
@Composable
private fun ModelDropdownItem(
    model: SavedModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 供应商图标
                ProviderIcon(
                    provider = model.model.provider,
                    modifier = Modifier.size(24.dp)
                )
                
                // 模型信息
                Column(modifier = Modifier.weight(weight = 1f, fill = true)) {
                    Text(
                        model.alias ?: model.model.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            model.model.provider.displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (model.isDefault) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    Strings.defaultLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
                
                // 选中标记
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = Strings.selectedLabel,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        onClick = onClick,
        modifier = Modifier.background(
            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    )
}

/**
 * 空状态提示
 */
@Composable
private fun EmptyModelState(
    onConfigureClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Outlined.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Text(
            Strings.noAvailableModels,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            Strings.configureModelHint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        PremiumButton(
            onClick = onConfigureClick,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(Strings.goToConfig)
        }
    }
}

/**
 * 供应商图标
 */
@Composable
fun ProviderIcon(
    provider: AiProvider?,
    modifier: Modifier = Modifier
) {
    val (label, backgroundColor) = when (provider) {
        AiProvider.GOOGLE -> "G" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.OPENROUTER -> "OR" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.OPENAI -> "AI" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.ANTHROPIC -> "A" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.GROK -> "X" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.MISTRAL -> "M" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.COHERE -> "C" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.AI21 -> "21" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.GROQ -> "GQ" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.CEREBRAS -> "CB" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.SAMBANOVA -> "SN" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.TOGETHER -> "TG" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.PERPLEXITY -> "PP" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.FIREWORKS -> "FW" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.DEEPINFRA -> "DI" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.NOVITA -> "NV" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.DEEPSEEK -> "DS" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.QWEN -> "QW" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.GLM -> "GL" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.VOLCANO -> "VL" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.MOONSHOT -> "MS" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.MINIMAX -> "MM" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.SILICONFLOW -> "SF" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.BAICHUAN -> "BC" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.YI -> "Yi" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.STEPFUN -> "SF" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.HUNYUAN -> "HY" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.SPARK -> "SP" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.OLLAMA -> "OL" to MaterialTheme.colorScheme.primaryContainer
        AiProvider.LM_STUDIO -> "LM" to MaterialTheme.colorScheme.secondaryContainer
        AiProvider.VLLM -> "VL" to MaterialTheme.colorScheme.tertiaryContainer
        AiProvider.CUSTOM -> "C" to MaterialTheme.colorScheme.surfaceVariant
        null -> "?" to MaterialTheme.colorScheme.surfaceVariant
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 过滤支持模块开发功能的模型
 * 
 * 此函数用于筛选出支持 MODULE_DEVELOPMENT 功能的模型
 * 
 * @param models 所有已保存的模型列表
 * @return 支持模块开发功能的模型列表
 */
fun filterModelsForModuleDevelopment(models: List<SavedModel>): List<SavedModel> {
    return models.filter { it.supportsFeature(AiFeature.MODULE_DEVELOPMENT) }
}
