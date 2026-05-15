package com.webtoapp.core.ai.v2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.v2.data.AgentMessage
import com.webtoapp.ui.design.WtaScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCodingV2Screen(onBack: () -> Unit, onOpenAiSettings: () -> Unit) {
    val ctx = LocalContext.current
    val vm: AiCodingV2ViewModel = viewModel(factory = AiCodingV2ViewModel.factory(ctx.applicationContext as android.app.Application))
    val state by vm.ui.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(state.error) { state.error?.let { snack.showSnackbar(it); vm.dismissError() } }
    LaunchedEffect(state.info) { state.info?.let { snack.showSnackbar(it); vm.dismissError() } }
    var showSessions by remember { mutableStateOf(false) }
    var showFiles by remember { mutableStateOf(false) }
    var showConfig by remember { mutableStateOf(false) }

    WtaScreen(title = state.currentSession?.title?.takeIf{it.isNotBlank()} ?: "AI 编程 v2", snackbarHostState = snack, onBack = onBack, actions = {
        IconButton(onClick={showSessions=true}){Icon(Icons.Default.History,null)}
        IconButton(onClick={showFiles=true}){Icon(Icons.Default.Folder,null)}
        IconButton(onClick={showConfig=true}){Icon(Icons.Default.Settings,null)}
    }) { _ ->
        Column(Modifier.fillMaxSize().imePadding()) {
            TypeRow(state, vm::setCodingType)
            HorizontalDivider()
            Box(Modifier.weight(1f)) { ConversationArea(state) }
            Composer(state, onSend = { vm.send(it) }, onCancel = { vm.cancel() })
        }
    }
    if(showSessions) ModalBottomSheet(onDismissRequest={showSessions=false}){ SessionsSheet(state,{vm.selectSession(it);showSessions=false},{vm.newSession(state.codingType);showSessions=false},{vm.deleteSession(it)}) }
    if(showFiles) ModalBottomSheet(onDismissRequest={showFiles=false}){ FilesSheet(state,vm::selectFile,vm::saveSelectedFile) }
    if(showConfig) ModalBottomSheet(onDismissRequest={showConfig=false}){ ConfigSheet(state,vm::setTextModel,vm::setImageModel,vm::setTemperature,vm::setMaxTurns,vm::setRules,{showConfig=false;onOpenAiSettings()}) }
}

@Composable private fun TypeRow(state: AiCodingV2UiState, onChange: (AiCodingType)->Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(6.dp)) {
        AiCodingType.entries.forEach { t -> AssistChip(onClick={onChange(t)}, label={Text(t.getDisplayName(),fontSize=12.sp)}, colors=AssistChipDefaults.assistChipColors(containerColor=if(state.codingType==t)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) }
    }
}

@Composable private fun ConversationArea(state: AiCodingV2UiState) {
    val ls = rememberLazyListState(); val msgs = state.currentSession?.messages.orEmpty()
    LaunchedEffect(msgs.size, state.streamingText) { val n=msgs.size+(if(state.phase!=AiCodingV2UiState.Phase.Idle)1 else 0); if(n>0)ls.animateScrollToItem(n-1) }
    if(msgs.isEmpty()&&state.phase==AiCodingV2UiState.Phase.Idle){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Text("输入需求开始对话",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)};return}
    LazyColumn(Modifier.fillMaxSize(),state=ls,contentPadding=PaddingValues(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        items(msgs,key={it.id}){m->MsgBubble(m)}
        if(state.phase!=AiCodingV2UiState.Phase.Idle) item{StreamBubble(state)}
        if(state.showFallbackHint) item{Surface(color=MaterialTheme.colorScheme.tertiaryContainer,shape=RoundedCornerShape(8.dp)){Text("ℹ️ 已切换为无工具模式",Modifier.padding(10.dp),style=MaterialTheme.typography.labelMedium)}}
    }
}

@Composable private fun MsgBubble(m: AgentMessage) {
    val isUser=m.role==AgentMessage.Role.USER; val color=when{m.isError->MaterialTheme.colorScheme.errorContainer;isUser->MaterialTheme.colorScheme.primaryContainer;else->MaterialTheme.colorScheme.surfaceVariant}
    Column(Modifier.fillMaxWidth(),horizontalAlignment=if(isUser)Alignment.End else Alignment.Start){
        Surface(color=color,shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(12.dp)){
            if(!m.thinking.isNullOrBlank()){Text("💭 ${m.thinking.take(200)}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.outline);Spacer(Modifier.height(4.dp))}
            Text(m.content,style=MaterialTheme.typography.bodyMedium)
            m.toolCalls.forEach{tc->Row(Modifier.fillMaxWidth().padding(top=4.dp).clip(RoundedCornerShape(6.dp)).background(if(tc.ok)MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer).padding(6.dp)){Text((if(tc.ok)"✓ " else "✗ ")+tc.name,style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Medium);Spacer(Modifier.width(4.dp));Text(tc.resultPreview.take(80),style=MaterialTheme.typography.labelSmall,maxLines=1)}}
            if(m.producedFiles.isNotEmpty()){Spacer(Modifier.height(4.dp));Text("文件：${m.producedFiles.joinToString(", ")}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        }}
    }
}

@Composable private fun StreamBubble(state: AiCodingV2UiState) {
    Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(12.dp).fillMaxWidth()){
        if(state.streamingThinking.isNotBlank()){Text("💭 ${state.streamingThinking.takeLast(200)}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.outline);Spacer(Modifier.height(4.dp))}
        if(state.streamingText.isNotBlank()) Text(state.streamingText.takeLast(500),style=MaterialTheme.typography.bodyMedium)
        state.pendingToolCalls.forEach{tc->Text((if(tc.ok)"⏳ " else "✗ ")+tc.name+" "+tc.resultPreview.take(60),style=MaterialTheme.typography.labelSmall)}
        Row(verticalAlignment=Alignment.CenterVertically){CircularProgressIndicator(Modifier.size(14.dp),strokeWidth=2.dp);Spacer(Modifier.width(8.dp));Text(when(state.phase){AiCodingV2UiState.Phase.Submitting->"连接中…";AiCodingV2UiState.Phase.AwaitingTool->"执行工具…";else->"生成中…"},style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    }}
}

@Composable private fun Composer(state: AiCodingV2UiState, onSend:(String)->Unit, onCancel:()->Unit) {
    var text by remember{mutableStateOf("")}; val working=state.phase!=AiCodingV2UiState.Phase.Idle; val canSend=state.canSend&&text.isNotBlank()
    Row(Modifier.fillMaxWidth().padding(12.dp,8.dp),verticalAlignment=Alignment.Bottom){
        Surface(Modifier.weight(1f),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surfaceVariant){
            BasicTextField(text,{text=it},Modifier.fillMaxWidth().heightIn(44.dp,200.dp).padding(14.dp,10.dp),textStyle=MaterialTheme.typography.bodyMedium.copy(color=MaterialTheme.colorScheme.onSurface),cursorBrush=SolidColor(MaterialTheme.colorScheme.primary),decorationBox={inner->if(text.isEmpty())Text(if(working)"AI 工作中…" else "描述你的需求",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant);inner()})
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick={if(working)onCancel() else if(canSend){onSend(text.trim());text=""}},enabled=working||canSend){if(working)Icon(Icons.Default.Stop,null) else Icon(Icons.AutoMirrored.Filled.Send,null)}
    }
}
