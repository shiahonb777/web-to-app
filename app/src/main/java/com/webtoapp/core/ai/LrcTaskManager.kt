package com.webtoapp.core.ai

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.taskDataStore by preferencesDataStore(name = "lrc_tasks")

/**
 * LRC 生成任务管理器
 * 负责任务的持久化存储和状态管理
 */
class LrcTaskManager(private val context: Context) {
    
    private val gson = Gson()
    private val tasksKey = stringPreferencesKey("tasks")
    
    /**
     * 获取所有任务的 Flow
     */
    val tasksFlow: Flow<List<LrcTask>> = context.taskDataStore.data.map { prefs ->
        val json = prefs[tasksKey] ?: "[]"
        try {
            val type = object : TypeToken<List<LrcTask>>() {}.type
            gson.fromJson<List<LrcTask>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 添加新任务
     */
    suspend fun addTask(task: LrcTask) {
        context.taskDataStore.edit { prefs ->
            val currentTasks = getCurrentTasks(prefs[tasksKey])
            val updatedTasks = listOf(task) + currentTasks
            prefs[tasksKey] = gson.toJson(updatedTasks)
        }
    }
    
    /**
     * 更新任务状态
     */
    suspend fun updateTask(task: LrcTask) {
        context.taskDataStore.edit { prefs ->
            val currentTasks = getCurrentTasks(prefs[tasksKey])
            val updatedTasks = currentTasks.map { 
                if (it.id == task.id) task else it 
            }
            prefs[tasksKey] = gson.toJson(updatedTasks)
        }
    }
    
    /**
     * 删除任务
     */
    suspend fun deleteTask(taskId: String) {
        context.taskDataStore.edit { prefs ->
            val currentTasks = getCurrentTasks(prefs[tasksKey])
            val updatedTasks = currentTasks.filter { it.id != taskId }
            prefs[tasksKey] = gson.toJson(updatedTasks)
        }
    }
    
    /**
     * 清空所有已完成/失败的任务
     */
    suspend fun clearCompletedTasks() {
        context.taskDataStore.edit { prefs ->
            val currentTasks = getCurrentTasks(prefs[tasksKey])
            val updatedTasks = currentTasks.filter { 
                it.status == LrcTaskStatus.PENDING || it.status == LrcTaskStatus.PROCESSING 
            }
            prefs[tasksKey] = gson.toJson(updatedTasks)
        }
    }
    
    /**
     * 根据 bgmPath 查找任务
     */
    suspend fun findTaskByBgmPath(bgmPath: String): LrcTask? {
        var result: LrcTask? = null
        context.taskDataStore.edit { prefs ->
            val currentTasks = getCurrentTasks(prefs[tasksKey])
            result = currentTasks.find { it.bgmPath == bgmPath }
        }
        return result
    }
    
    private fun getCurrentTasks(json: String?): List<LrcTask> {
        return try {
            val type = object : TypeToken<List<LrcTask>>() {}.type
            gson.fromJson<List<LrcTask>>(json ?: "[]", type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    companion object {
        @Volatile
        private var instance: LrcTaskManager? = null
        
        fun getInstance(context: Context): LrcTaskManager {
            return instance ?: synchronized(this) {
                instance ?: LrcTaskManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
