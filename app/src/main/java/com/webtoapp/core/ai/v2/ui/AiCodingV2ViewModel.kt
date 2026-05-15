package com.webtoapp.core.ai.v2.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.webtoapp.core.ai.AiConfigManager
import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.v2.agent.AgentEvent
import com.webtoapp.core.ai.v2.data.*
import com.webtoapp.core.ai.v2.llm.LlmMessage
import com.webtoapp.core.ai.v2.llm.LlmToolCall
import com.webtoapp.core.ai.v2.runtime.AgentService
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.AiFeature
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AiCodingV2ViewModel(application: Application) : AndroidViewModel(application) {
    private val store = AgentSessionStore(application)
    private val configManager = AiConfigManager(application)
    private val _ui = MutableStateFlow(AiCodingV2UiState())
    val ui: StateFlow<AiCodingV2UiState> = _ui.asStateFlow()

    private var service: AgentService? = null
    private var bound = false
    private var eventJob: Job? = null
    private var streamSessionId: String? = null
    private val stText = StringBuilder()
    private val stThink = StringBuilder()
    private val stTools = mutableMapOf<String, RecordedToolCall>()

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName?, b: IBinder?) { service=(b as AgentService.LocalBinder).service(); bound=true; collectEvents() }
        override fun onServiceDisconnected(n: ComponentName?) { service=null; bound=false }
    }

    init {
        viewModelScope.launch { combine(store.sessionsFlow, store.currentSessionIdFlow){s,id->s to id}.collect{(sessions,id)->val cur=sessions.find{it.id==id}; _ui.update{it.copy(sessions=sessions,currentSession=cur,codingType=cur?.codingType?:it.codingType,configState=cur?.config?.toCS(it.configState)?:it.configState)}; refreshFiles(cur?.id)} }
        viewModelScope.launch { configManager.savedModelsFlow.collect { models -> val t=models.filter{it.supportsFeature(AiFeature.AI_CODING)||it.supportsFeature(AiFeature.MODULE_DEVELOPMENT)}.map{it.id to(it.alias?.takeIf{a->a.isNotBlank()}?:it.model.name)}; val i=models.filter{it.supportsFeature(AiFeature.AI_CODING_IMAGE)||it.supportsFeature(AiFeature.ICON_GENERATION)}.map{it.id to(it.alias?.takeIf{a->a.isNotBlank()}?:it.model.name)}; _ui.update{it.copy(configState=it.configState.copy(availableTextModelIds=t,availableImageModelIds=i))} } }
        val ctx = application; val intent = Intent(ctx, AgentService::class.java); ContextCompat.startForegroundService(ctx, intent); ctx.bindService(intent, conn, Context.BIND_AUTO_CREATE)
    }

    private fun collectEvents() { eventJob?.cancel(); eventJob = viewModelScope.launch { service?.events?.collect { handleEvent(it) } } }

    fun selectSession(id: String) { viewModelScope.launch { store.setCurrent(id) } }
    fun newSession(type: AiCodingType) { viewModelScope.launch { val s=store.create(type); _ui.value.configState.availableTextModelIds.firstOrNull()?.first?.let{store.updateConfig(s.id,AgentSessionConfig(textModelId=it))} } }
    fun deleteSession(id: String) { viewModelScope.launch { store.delete(id) } }
    fun setCodingType(t: AiCodingType) { val c=_ui.value.currentSession?:return; viewModelScope.launch{store.update(c.copy(codingType=t))} }
    fun setTextModel(id: String) { val c=_ui.value.currentSession?:return; viewModelScope.launch{store.updateConfig(c.id,c.config.copy(textModelId=id))} }
    fun setImageModel(id: String?) { val c=_ui.value.currentSession?:return; viewModelScope.launch{store.updateConfig(c.id,c.config.copy(imageModelId=id))} }
    fun setTemperature(t: Float) { val c=_ui.value.currentSession?:return; viewModelScope.launch{store.updateConfig(c.id,c.config.copy(temperature=t))} }
    fun setMaxTurns(n: Int) { val c=_ui.value.currentSession?:return; viewModelScope.launch{store.updateConfig(c.id,c.config.copy(maxTurns=n))} }
    fun setRules(r: List<String>) { val c=_ui.value.currentSession?:return; viewModelScope.launch{store.updateConfig(c.id,c.config.copy(rules=r))} }
    fun selectFile(path: String) { val sid=_ui.value.currentSession?.id?:return; _ui.update{it.copy(selectedFilePath=path,selectedFileContent=store.files.readFile(sid,path))} }
    fun saveSelectedFile(content: String) { val sid=_ui.value.currentSession?.id?:return; val p=_ui.value.selectedFilePath?:return; store.files.createFile(sid,p,content,false); refreshFiles(sid); _ui.update{it.copy(selectedFileContent=content,info="已保存 $p")} }
    fun dismissError() { _ui.update{it.copy(error=null,info=null)} }
    fun cancel() { service?.cancel(); _ui.update{it.copy(phase=AiCodingV2UiState.Phase.Idle)}; streamSessionId=null }

    fun send(message: String) {
        val cur = _ui.value.currentSession ?: return; if(message.isBlank()||_ui.value.phase!=AiCodingV2UiState.Phase.Idle) return
        val cfg = cur.config; val tmId = cfg.textModelId; if(tmId.isNullOrBlank()){_ui.update{it.copy(error="请先选择文本模型")};return}
        viewModelScope.launch {
            val models = configManager.savedModelsFlow.first(); val keys = configManager.apiKeysFlow.first()
            val tm = models.find{it.id==tmId} ?: run{_ui.update{it.copy(error="找不到文本模型")};return@launch}
            val tk = keys.find{it.id==tm.apiKeyId} ?: run{_ui.update{it.copy(error="API Key 不存在")};return@launch}
            val im = cfg.imageModelId?.let{id->models.find{it.id==id}}; val ik = im?.let{m->keys.find{it.id==m.apiKeyId}}
            val userMsg = AgentMessage(role=AgentMessage.Role.USER, content=message)
            val afterUser = store.appendMessage(cur.id, userMsg) ?: return@launch
            streamSessionId=cur.id; stText.clear(); stThink.clear(); stTools.clear()
            _ui.update{it.copy(phase=AiCodingV2UiState.Phase.Submitting,streamingText="",streamingThinking="",pendingToolCalls=emptyList(),showFallbackHint=false,error=null)}
            val history = afterUser.messages.dropLast(1).mapNotNull{m->when(m.role){AgentMessage.Role.USER->LlmMessage(LlmMessage.Role.USER,m.content);AgentMessage.Role.ASSISTANT->LlmMessage(LlmMessage.Role.ASSISTANT,m.content,m.toolCalls.map{LlmToolCall(it.toolCallId,it.name,it.argumentsJson)});else->null}}
            val svc = service ?: run{_ui.update{it.copy(phase=AiCodingV2UiState.Phase.Idle,error="AI 服务未连接")};return@launch}
            svc.start(AgentService.StartRequest(cur.id, cur.codingType, Strings.currentLanguage.value, message, history, cfg.rules, tm, tk, im, ik, cfg.temperature, cfg.maxTurns))
        }
    }

    private suspend fun handleEvent(ev: AgentEvent) {
        if(streamSessionId==null) return
        when(ev){
            is AgentEvent.Started -> _ui.update{it.copy(phase=AiCodingV2UiState.Phase.Streaming)}
            is AgentEvent.TextDelta -> { stText.setLength(0);stText.append(ev.accumulated); _ui.update{it.copy(streamingText=ev.accumulated)} }
            is AgentEvent.ThinkingDelta -> { stThink.append(ev.delta); _ui.update{it.copy(streamingThinking=stThink.toString())} }
            is AgentEvent.ToolStarted -> { stTools[ev.toolCallId]=RecordedToolCall(ev.toolCallId,ev.name,"","running…",true); _ui.update{it.copy(phase=AiCodingV2UiState.Phase.AwaitingTool,pendingToolCalls=stTools.values.toList())} }
            is AgentEvent.ToolArgsDelta -> Unit
            is AgentEvent.ToolFinished -> { stTools[ev.toolCallId]=RecordedToolCall(ev.toolCallId,ev.name,ev.arguments,ev.result.text.take(160),ev.result.ok); _ui.update{it.copy(phase=AiCodingV2UiState.Phase.Streaming,pendingToolCalls=stTools.values.toList())} }
            is AgentEvent.FileChanged -> refreshFiles(streamSessionId)
            is AgentEvent.FallbackTriggered -> _ui.update{it.copy(showFallbackHint=true)}
            is AgentEvent.Completed -> { val txt=stText.toString().trim().ifEmpty{ev.summaryText}; val files=stTools.values.filter{it.ok&&it.name in setOf("write_file","edit_file","delete_file")}.mapNotNull{runCatching{com.google.gson.JsonParser.parseString(it.argumentsJson).asJsonObject.get("path")?.asString}.getOrNull()}.distinct()
                store.appendMessage(streamSessionId!!, AgentMessage(role=AgentMessage.Role.ASSISTANT,content=txt,thinking=stThink.toString().takeIf{it.isNotBlank()},toolCalls=stTools.values.toList(),producedFiles=files))
                streamSessionId=null; _ui.update{it.copy(phase=AiCodingV2UiState.Phase.Idle,streamingText="",streamingThinking="",pendingToolCalls=emptyList(),info="完成（${ev.toolCallCount} 次工具调用）")} }
            is AgentEvent.Failed -> { store.appendMessage(streamSessionId!!, AgentMessage(role=AgentMessage.Role.ASSISTANT,content=stText.toString().ifBlank{"(no output)"}+"\n\n[错误] ${ev.message}",isError=true,toolCalls=stTools.values.toList()))
                streamSessionId=null; _ui.update{it.copy(phase=AiCodingV2UiState.Phase.Idle,streamingText="",streamingThinking="",pendingToolCalls=emptyList(),error=ev.message)} }
        }
    }

    private fun refreshFiles(sid: String?) { if(sid==null){_ui.update{it.copy(projectFiles=emptyList())};return}; val f=store.files.listFiles(sid); _ui.update{it.copy(projectFiles=f)} }
    override fun onCleared() { super.onCleared(); eventJob?.cancel(); if(bound) runCatching{getApplication<Application>().unbindService(conn)} }
    private fun AgentSessionConfig.toCS(prev: ConfigState) = prev.copy(textModelId=textModelId,imageModelId=imageModelId,temperature=temperature,rules=rules,maxTurns=maxTurns)
    companion object { fun factory(app: Application) = object:ViewModelProvider.Factory{ @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(c:Class<T>):T = AiCodingV2ViewModel(app) as T } }
}
