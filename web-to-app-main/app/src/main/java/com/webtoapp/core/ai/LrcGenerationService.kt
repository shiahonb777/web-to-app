package com.webtoapp.core.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.webtoapp.data.model.*
import com.webtoapp.util.BgmStorage
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * LRC 字幕生成后台服务
 * 支持在后台持续运行，即使应用被关闭
 */
class LrcGenerationService : Service() {
    
    companion object {
        private const val CHANNEL_ID = "lrc_generation_channel"
        private const val NOTIFICATION_ID = 1001
        private const val RESULT_CHANNEL_ID = "lrc_result_channel"
        
        const val ACTION_START_TASK = "com.webtoapp.action.START_LRC_TASK"
        const val ACTION_CANCEL_TASK = "com.webtoapp.action.CANCEL_LRC_TASK"
        const val ACTION_LRC_TASK_COMPLETED = "com.webtoapp.action.LRC_TASK_COMPLETED"
        const val EXTRA_TASK = "extra_task"
        const val EXTRA_API_KEY = "extra_api_key"
        const val EXTRA_MODEL = "extra_model"
        const val EXTRA_BGM_PATH = "extra_bgm_path"
        const val EXTRA_BGM_NAME = "extra_bgm_name"
        const val EXTRA_SUCCESS = "extra_success"
        
        private val taskQueue = ConcurrentLinkedQueue<LrcTask>()
        private var currentTask: LrcTask? = null
        
        // 任务状态回调
        var onTaskStatusChanged: ((LrcTask) -> Unit)? = null
        
        fun startTask(
            context: Context,
            task: LrcTask,
            apiKey: ApiKeyConfig,
            model: SavedModel
        ) {
            // 保存任务到历史记录
            kotlinx.coroutines.GlobalScope.launch {
                LrcTaskManager.getInstance(context).addTask(task)
            }
            
            val intent = Intent(context, LrcGenerationService::class.java).apply {
                action = ACTION_START_TASK
                putExtra(EXTRA_TASK, Gson().toJson(task))
                putExtra(EXTRA_API_KEY, Gson().toJson(apiKey))
                putExtra(EXTRA_MODEL, Gson().toJson(model))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private lateinit var aiApiClient: AiApiClient
    private lateinit var notificationManager: NotificationManager
    
    override fun onCreate() {
        super.onCreate()
        aiApiClient = AiApiClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannels()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TASK -> {
                val taskJson = intent.getStringExtra(EXTRA_TASK) ?: return START_NOT_STICKY
                val apiKeyJson = intent.getStringExtra(EXTRA_API_KEY) ?: return START_NOT_STICKY
                val modelJson = intent.getStringExtra(EXTRA_MODEL) ?: return START_NOT_STICKY
                
                val task = gson.fromJson(taskJson, LrcTask::class.java)
                val apiKey = gson.fromJson(apiKeyJson, ApiKeyConfig::class.java)
                val model = gson.fromJson(modelJson, SavedModel::class.java)
                
                // 添加到队列
                taskQueue.add(task)
                
                // 启动前台服务
                startForeground(NOTIFICATION_ID, createProgressNotification("正在准备..."))
                
                // 处理队列
                processQueue(apiKey, model)
            }
            ACTION_CANCEL_TASK -> {
                // 取消当前任务
                serviceScope.coroutineContext.cancelChildren()
                stopSelf()
            }
        }
        
        return START_STICKY
    }
    
    private fun processQueue(apiKey: ApiKeyConfig, model: SavedModel) {
        serviceScope.launch {
            while (taskQueue.isNotEmpty()) {
                val task = taskQueue.poll() ?: continue
                currentTask = task.copy(status = LrcTaskStatus.PROCESSING)
                
                // 通知任务开始
                onTaskStatusChanged?.invoke(currentTask!!)
                updateNotification("正在处理: ${task.bgmName}")
                
                try {
                    // 调用 AI API 生成歌词
                    val result = aiApiClient.generateLrc(
                        apiKey = apiKey,
                        model = model.model,
                        audioPath = task.bgmPath
                    )
                    
                    if (result.isSuccess) {
                        val lrcData = result.getOrNull()!!
                        
                        // 保存 LRC 到文件
                        val saved = BgmStorage.saveLrc(this@LrcGenerationService, task.bgmPath, lrcData)
                        android.util.Log.d("LrcGenerationService", "LRC 保存结果: $saved, 路径: ${task.bgmPath}")
                        
                        currentTask = currentTask!!.copy(
                            status = LrcTaskStatus.COMPLETED,
                            resultLrc = lrcData,
                            progress = 100,
                            completedAt = System.currentTimeMillis()
                        )
                        
                        // 更新任务历史记录
                        LrcTaskManager.getInstance(this@LrcGenerationService).updateTask(currentTask!!)
                        
                        // 发送成功通知
                        showResultNotification(
                            title = "歌词生成完成",
                            content = "${task.bgmName} 的歌词已生成成功"
                        )
                        
                        // 发送广播通知 UI 刷新
                        sendCompletionBroadcast(task.bgmPath, task.bgmName, true)
                    } else {
                        currentTask = currentTask!!.copy(
                            status = LrcTaskStatus.FAILED,
                            errorMessage = result.exceptionOrNull()?.message,
                            completedAt = System.currentTimeMillis()
                        )
                        
                        // 更新任务历史记录
                        LrcTaskManager.getInstance(this@LrcGenerationService).updateTask(currentTask!!)
                        
                        // 发送失败通知
                        showResultNotification(
                            title = "歌词生成失败",
                            content = "${task.bgmName}: ${result.exceptionOrNull()?.message}"
                        )
                        
                        // 发送广播通知 UI
                        sendCompletionBroadcast(task.bgmPath, task.bgmName, false)
                    }
                } catch (e: Exception) {
                    currentTask = currentTask!!.copy(
                        status = LrcTaskStatus.FAILED,
                        errorMessage = e.message,
                        completedAt = System.currentTimeMillis()
                    )
                    
                    // 更新任务历史记录
                    LrcTaskManager.getInstance(this@LrcGenerationService).updateTask(currentTask!!)
                    
                    showResultNotification(
                        title = "歌词生成失败",
                        content = "${task.bgmName}: ${e.message}"
                    )
                    
                    // 发送广播通知 UI
                    sendCompletionBroadcast(task.bgmPath, task.bgmName, false)
                }
                
                // 通知任务状态变化
                onTaskStatusChanged?.invoke(currentTask!!)
            }
            
            // 所有任务完成，停止服务
            currentTask = null
            stopSelf()
        }
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 进度通知渠道
            val progressChannel = NotificationChannel(
                CHANNEL_ID,
                "歌词生成进度",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示歌词生成任务的进度"
            }
            
            // 结果通知渠道
            val resultChannel = NotificationChannel(
                RESULT_CHANNEL_ID,
                "歌词生成结果",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "显示歌词生成任务的完成状态"
            }
            
            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(resultChannel)
        }
    }
    
    private fun createProgressNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在生成歌词")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification(content: String) {
        val notification = createProgressNotification(content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun showResultNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * 发送任务完成广播，通知 UI 刷新
     */
    private fun sendCompletionBroadcast(bgmPath: String, bgmName: String, success: Boolean) {
        val intent = Intent(ACTION_LRC_TASK_COMPLETED).apply {
            putExtra(EXTRA_BGM_PATH, bgmPath)
            putExtra(EXTRA_BGM_NAME, bgmName)
            putExtra(EXTRA_SUCCESS, success)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        android.util.Log.d("LrcGenerationService", "发送完成广播: bgmPath=$bgmPath, bgmName=$bgmName, success=$success")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
