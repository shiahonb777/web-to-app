package com.webtoapp.core.ai.v2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.reflect.TypeToken
import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.coding.ProjectFileManager
import com.webtoapp.util.GsonProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiV2Store: DataStore<Preferences> by preferencesDataStore(name = "ai_coding_v2")

class AgentSessionStore(private val context: Context) {
    private val gson = GsonProvider.gson
    val files = ProjectFileManager(context)
    val sessionsFlow: Flow<List<AgentSession>> = context.aiV2Store.data.map { decode(it[KEY_S]) }
    val currentSessionIdFlow: Flow<String?> = context.aiV2Store.data.map { it[KEY_C] }

    suspend fun create(codingType: AiCodingType, title: String = ""): AgentSession {
        val s = AgentSession(codingType = codingType, title = title)
        context.aiV2Store.edit { p -> val l = decode(p[KEY_S]).toMutableList(); l.add(0,s); p[KEY_S]=gson.toJson(l); p[KEY_C]=s.id }
        files.getSessionProjectDir(s.id); return s
    }
    suspend fun setCurrent(id: String) { context.aiV2Store.edit { it[KEY_C]=id } }
    suspend fun get(id: String): AgentSession? = sessionsFlow.first().find { it.id == id }
    suspend fun delete(id: String) { context.aiV2Store.edit { p -> val l=decode(p[KEY_S]).filter{it.id!=id}; p[KEY_S]=gson.toJson(l); if(p[KEY_C]==id)p.remove(KEY_C) }; files.deleteProject(id) }
    suspend fun update(session: AgentSession) { context.aiV2Store.edit { p -> val l=decode(p[KEY_S]).toMutableList(); val i=l.indexOfFirst{it.id==session.id}; if(i>=0){l[i]=session.copy(updatedAt=System.currentTimeMillis());p[KEY_S]=gson.toJson(l)} } }
    suspend fun appendMessage(sessionId: String, message: AgentMessage): AgentSession? { var out:AgentSession?=null; context.aiV2Store.edit { p -> val l=decode(p[KEY_S]).toMutableList(); val i=l.indexOfFirst{it.id==sessionId}; if(i>=0){var s=l[i]; if(s.title.isBlank()&&message.role==AgentMessage.Role.USER)s=s.copy(title=message.content.take(40)); s=s.copy(messages=s.messages+message,updatedAt=System.currentTimeMillis()); l[i]=s; out=s; p[KEY_S]=gson.toJson(l)} }; return out }
    suspend fun updateConfig(sessionId: String, config: AgentSessionConfig): AgentSession? { var out:AgentSession?=null; context.aiV2Store.edit { p -> val l=decode(p[KEY_S]).toMutableList(); val i=l.indexOfFirst{it.id==sessionId}; if(i>=0){val s=l[i].copy(config=config,updatedAt=System.currentTimeMillis()); l[i]=s; out=s; p[KEY_S]=gson.toJson(l)} }; return out }

    private fun decode(json: String?): List<AgentSession> { if(json.isNullOrBlank()) return emptyList(); return runCatching{gson.fromJson<List<AgentSession>>(json,object:TypeToken<List<AgentSession>>(){}.type)}.getOrElse{emptyList()} }
    companion object { private val KEY_S = stringPreferencesKey("sessions_v1"); private val KEY_C = stringPreferencesKey("current_id_v1") }
}
