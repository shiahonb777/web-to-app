package com.webtoapp.core.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.webtoapp.R
import com.webtoapp.core.ai.htmlcoding.HtmlAgentEvent
import com.webtoapp.core.ai.htmlcoding.HtmlCodingAgent
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.SavedModel
import com.webtoapp.core.ai.htmlcoding.SessionConfig
import com.webtoapp.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * AI 生成前台服务
 * 
 * 用于在后台保持 AI 生成任务运行，防止用户切换应用时任务被终止
 */
class AiGenerationService : Service() {
    
    companion object {
        private const val TAG = "AiGenerationService"
        private const val CHANNEL_ID = "ai_generation_channel"
        private const val NOTIFICATION_ID = 1001
        
        // 服务状态
        var isRunning = false
            private set
    }
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 当前生成任务
    private var currentJob: Job? = null
    private var htmlAgent: HtmlCodingAgent? = null
    
    // WakeLock 保持 CPU 运行
    private var wakeLock: PowerManager.WakeLock? = null
    
    // 事件流 - 用于向 UI 发送事件（不使用 replay，避免重复事件）
    private val _eventFlow = MutableSharedFlow<HtmlAgentEvent>(replay = 0, extraBufferCapacity = 64)
    val eventFlow: SharedFlow<HtmlAgentEvent> = _eventFlow.asSharedFlow()
    
    // Generate状态
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    // 最终结果缓存（用于 UI 重新连接时获取结果）
    private var lastCompletedEvent: HtmlAgentEvent.Completed? = null
    private var lastHtmlContent: String? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): AiGenerationService = this@AiGenerationService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
        htmlAgent = HtmlCodingAgent(this)
        isRunning = true
        
        // Get WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WebToApp:AiGenerationWakeLock"
        )
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        startForeground(NOTIFICATION_ID, createNotification(Strings.aiGenerationServiceRunning))
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        isRunning = false
        currentJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
    }
    
    /**
     * 获取最后完成的事件（用于 UI 重新连接）
     */
    fun getLastCompletedEvent(): HtmlAgentEvent.Completed? = lastCompletedEvent
    
    /**
     * 获取最后生成的 HTML
     */
    fun getLastHtmlContent(): String? = lastHtmlContent
    
    /**
     * 开始 AI 生成任务
     */
    fun startGeneration(
        requirement: String,
        currentHtml: String?,
        sessionConfig: SessionConfig?,
        model: SavedModel,
        sessionId: String? = null  // SessionID，用于文件操作
    ) {
        // Cancel之前的任务
        currentJob?.cancel()
        lastCompletedEvent = null
        lastHtmlContent = null
        
        // Reset事件流，清除可能残留的缓冲事件
        _eventFlow.resetReplayCache()
        
        _isGenerating.value = true
        updateNotification(Strings.generatingHtmlCode)
        
        Log.d(TAG, "Starting generation for requirement: ${requirement.take(50)}..., sessionId: $sessionId")
        
        // Get WakeLock 保持 CPU 运行
        acquireWakeLock()
        
        currentJob = serviceScope.launch {
            try {
                htmlAgent?.developWithStream(
                    requirement = requirement,
                    currentHtml = currentHtml,
                    sessionConfig = sessionConfig,
                    model = model,
                    sessionId = sessionId
                )?.collect { event ->
                    // 转发事件到 UI
                    _eventFlow.emit(event)
                    
                    // Update通知和缓存
                    when (event) {
                        is HtmlAgentEvent.CodeDelta -> {
                            updateNotification(Strings.generatingCodeChars.format(event.accumulated.length))
                        }
                        is HtmlAgentEvent.FileCreated -> {
                            val versionInfo = if (event.isNewVersion) "v${event.fileInfo.version}" else Strings.newFile
                            updateNotification(Strings.fileCreatedVersion.format(event.fileInfo.name, versionInfo))
                            Log.d(TAG, "File created: ${event.fileInfo.name}, version=${event.fileInfo.version}")
                        }
                        is HtmlAgentEvent.HtmlComplete -> {
                            lastHtmlContent = event.html
                            updateNotification(Strings.codeGenerationComplete)
                        }
                        is HtmlAgentEvent.Error -> {
                            updateNotification(Strings.generationFailed.format(event.message))
                            _isGenerating.value = false
                            releaseWakeLock()
                        }
                        is HtmlAgentEvent.Completed -> {
                            lastCompletedEvent = event
                            _isGenerating.value = false
                            updateNotification(Strings.generationComplete)
                            releaseWakeLock()
                            // 延迟停止前台服务
                            delay(3000)
                            stopForeground(STOP_FOREGROUND_REMOVE)
                        }
                        else -> {}
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Generation cancelled")
                _eventFlow.emit(HtmlAgentEvent.Error(Strings.generationCancelled))
                releaseWakeLock()
            } catch (e: Exception) {
                Log.e(TAG, "Generation error", e)
                _eventFlow.emit(HtmlAgentEvent.Error(Strings.generationFailed.format(e.message)))
                releaseWakeLock()
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * 取消当前生成任务
     */
    fun cancelGeneration() {
        currentJob?.cancel()
        _isGenerating.value = false
        releaseWakeLock()
        updateNotification(Strings.generationCancelled)
        serviceScope.launch {
            delay(1000)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }
    
    /**
     * 获取 WakeLock
     */
    private fun acquireWakeLock() {
        try {
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire(10 * 60 * 1000L) // 最多 10 分钟
                    Log.d(TAG, "WakeLock acquired")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }
    
    /**
     * 释放 WakeLock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release WakeLock", e)
        }
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                Strings.aiGenerationService,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = Strings.aiCodeGenerationNotification
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(Strings.htmlCodingAssistant)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }
}
