package com.webtoapp.core.ai.v2.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.webtoapp.ui.design.WtaButton
import com.webtoapp.ui.design.WtaButtonSize
import com.webtoapp.ui.design.WtaButtonVariant

@Composable
internal fun SessionsSheet(state: AiCodingV2UiState, onSelect:(String)->Unit, onNew:()->Unit, onDelete:(String)->Unit) {
    Column(Modifier.padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Text("会话历史",style=MaterialTheme.typography.titleMedium)
            WtaButton(onClick=onNew,text="新建",variant=WtaButtonVariant.Tonal,size=WtaButtonSize.Small,leadingIcon=Icons.Default.Add)
        }
        Spacer(Modifier.height(12.dp))
        if(state.sessions.isEmpty()){Text("（暂无会话）",color=MaterialTheme.colorScheme.onSurfaceVariant);return}
        LazyColumn(Modifier.heightIn(max=480.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            items(state.sessions,key={it.id}){s->
                val cur=s.id==state.currentSession?.id
                Surface(color=if(cur)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(10.dp),modifier=Modifier.fillMaxWidth().clickable{onSelect(s.id)}){
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Column(Modifier.weight(1f)){Text(s.title.ifBlank{"（未命名）"},style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Medium);Text("${s.codingType.getDisplayName()} · ${s.messages.size} 条",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                        IconButton(onClick={onDelete(s.id)}){Icon(Icons.Default.Delete,null)}
                    }
                }
            }
        }
    }
}

@Composable
internal fun FilesSheet(state: AiCodingV2UiState, onSelect:(String)->Unit, onSave:(String)->Unit) {
    var editing by remember(state.selectedFilePath){mutableStateOf(state.selectedFileContent.orEmpty())}
    LaunchedEffect(state.selectedFileContent){editing=state.selectedFileContent.orEmpty()}
    Column(Modifier.padding(16.dp).heightIn(max=600.dp)){
        Text("项目文件",style=MaterialTheme.typography.titleMedium); Spacer(Modifier.height(12.dp))
        if(state.projectFiles.isEmpty()){Text("（暂无文件）",color=MaterialTheme.colorScheme.onSurfaceVariant);return}
        LazyColumn(Modifier.heightIn(max=200.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            items(state.projectFiles,key={it.path}){f->
                Surface(color=if(f.name==state.selectedFilePath)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxWidth().clickable{onSelect(f.name)}){
                    Row(Modifier.padding(10.dp,8.dp)){Text(f.name,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall);Text("${f.size}B",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
                }
            }
        }
        if(state.selectedFilePath!=null){
            Spacer(Modifier.height(12.dp));HorizontalDivider();Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(state.selectedFilePath?:"",Modifier.weight(1f),style=MaterialTheme.typography.titleSmall);IconButton(onClick={onSave(editing)}){Icon(Icons.Default.Save,null)}}
            Spacer(Modifier.height(6.dp))
            Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxWidth().heightIn(max=320.dp)){
                BasicTextField(editing,{editing=it},Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()),textStyle=MaterialTheme.typography.bodySmall.copy(color=MaterialTheme.colorScheme.onSurface),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary))
            }
        }
    }
}

@Composable
internal fun ConfigSheet(state: AiCodingV2UiState, onText:(String)->Unit, onImage:(String?)->Unit, onTemp:(Float)->Unit, onTurns:(Int)->Unit, onRules:(List<String>)->Unit, onOpenSettings:()->Unit) {
    val cfg=state.configState; var rulesText by remember(cfg.rules){mutableStateOf(cfg.rules.joinToString("\n"))}
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()).heightIn(max=600.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("会话配置",style=MaterialTheme.typography.titleMedium)
        // Text model
        Text("文本模型",style=MaterialTheme.typography.labelLarge)
        if(cfg.availableTextModelIds.isEmpty()){Surface(color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(8.dp)){Column(Modifier.padding(10.dp)){Text("未配置模型",style=MaterialTheme.typography.bodySmall);Spacer(Modifier.height(6.dp));WtaButton(onClick=onOpenSettings,text="前往 AI 设置",variant=WtaButtonVariant.Tonal,size=WtaButtonSize.Small)}}}
        else ChipGroup(cfg.availableTextModelIds,cfg.textModelId,onText)
        // Image model
        Text("图像模型（可选）",style=MaterialTheme.typography.labelLarge)
        if(cfg.availableImageModelIds.isEmpty()) Text("未配置",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        else ChipGroup(listOf("__none__" to "未启用")+cfg.availableImageModelIds,cfg.imageModelId?:"__none__"){id->onImage(if(id=="__none__")null else id)}
        // Temperature
        Text("Temperature: ${"%.2f".format(cfg.temperature)}",style=MaterialTheme.typography.labelLarge)
        Slider(cfg.temperature,onTemp,valueRange=0f..1.5f,steps=14)
        // Max turns
        Text("ReAct 最大轮数: ${cfg.maxTurns}",style=MaterialTheme.typography.labelLarge)
        Slider(cfg.maxTurns.toFloat(),{onTurns(it.toInt())},valueRange=1f..12f,steps=10)
        // Rules
        Text("用户规则（每行一条）",style=MaterialTheme.typography.labelLarge)
        Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(8.dp),modifier=Modifier.fillMaxWidth().heightIn(80.dp,200.dp)){BasicTextField(rulesText,{rulesText=it},Modifier.fillMaxWidth().padding(10.dp),textStyle=MaterialTheme.typography.bodySmall.copy(color=MaterialTheme.colorScheme.onSurface),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary))}
        WtaButton(onClick={onRules(rulesText.lines().map{it.trim()}.filter{it.isNotEmpty()})},text="保存规则",variant=WtaButtonVariant.Tonal,size=WtaButtonSize.Small)
        Spacer(Modifier.height(8.dp))
        WtaButton(onClick=onOpenSettings,text="管理 API Key 与模型",variant=WtaButtonVariant.Outlined,size=WtaButtonSize.Medium)
    }
}

@Composable private fun ChipGroup(options:List<Pair<String,String>>,selected:String?,onSelect:(String)->Unit){
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
        options.forEach{(id,label)->AssistChip(onClick={onSelect(id)},label={Text(label,style=MaterialTheme.typography.labelMedium)},colors=AssistChipDefaults.assistChipColors(containerColor=if(id==selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant))}
    }
}
