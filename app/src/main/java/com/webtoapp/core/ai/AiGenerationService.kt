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
import com.webtoapp.core.logging.AppLogger
import androidx.core.app.NotificationCompat
import com.webtoapp.R
import com.webtoapp.core.ai.coding.HtmlAgentEvent
import com.webtoapp.core.ai.coding.AiCodingAgent
import com.webtoapp.core.i18n.Strings
import com.webtoapp.data.model.SavedModel
import com.webtoapp.core.ai.coding.SessionConfig
import com.webtoapp.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
/**
 * AI generation foreground service
 *
 * Keeps AI generation tasks running when the user switches apps or the process is backgrounded
 */
class AiGenerationService : Service() {
    
    companion object {
        private const val TAG = "AiGenerationService"
        private const val CHANNEL_ID = "ai_generation_channel"
        private const val NOTIFICATION_ID = 1001
        
        // Service status
        var isRunning = false
            private set
    }
    
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Current generation job
    private var currentJob: Job? = null
    private var htmlAgent: AiCodingAgent? = null
    
    // WakeLock to keep the CPU running
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Event flow for UI events (no replay to prevent duplicates)
    private val _eventFlow = MutableSharedFlow<HtmlAgentEvent>(replay = 0, extraBufferCapacity = 64)
    val eventFlow: SharedFlow<HtmlAgentEvent> = _eventFlow.asSharedFlow()
    
    // Generation state flag
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    // Cache the last result for UI reconnection
    private var lastCompletedEvent: HtmlAgentEvent.Completed? = null
    private var lastHtmlContent: String? = null
    
    inner class LocalBinder : Binder() {
        fun getService(): AiGenerationService = this@AiGenerationService
    }
    
    override fun onCreate() {
        super.onCreate()
        AppLogger.w(TAG, "Service created")
        createNotificationChannel()
        htmlAgent = AiCodingAgent(this)
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
        AppLogger.w(TAG, "Service started")
        startForeground(NOTIFICATION_ID, createNotification(Strings.aiGenerationServiceRunning))
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        AppLogger.w(TAG, "Service destroyed")
        isRunning = false
        currentJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()
    }
    
    /**
     * Return the most recent completed event for UI reconnection
     */
    fun getLastCompletedEvent(): HtmlAgentEvent.Completed? = lastCompletedEvent
    
    /**
     * Get the last generated HTML content
     */
    fun getLastHtmlContent(): String? = lastHtmlContent
    
    /**
     * Start an AI generation task
     */
    fun startGeneration(
        requirement: String,
        currentHtml: String?,
        sessionConfig: SessionConfig?,
        model: SavedModel,
        sessionId: String? = null  // Session ID used for file operations
    ) {
        // Cancel any previous job
        currentJob?.cancel()
        lastCompletedEvent = null
        lastHtmlContent = null
        
        // Reset the event flow to clear buffered events
        _eventFlow.resetReplayCache()
        
        _isGenerating.value = true
        updateNotification(Strings.generatingHtmlCode)
        
        AppLogger.w(TAG, "Starting generation for requirement: ${requirement.take(50)}..., sessionId: $sessionId")
        
        // Acquire WakeLock to keep the CPU running
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
                    // Forward events to the UI
                    _eventFlow.emit(event)
                    
                    // Update the notification and cache state
                    when (event) {
                        is HtmlAgentEvent.CodeDelta -> {
                            updateNotification(Strings.generatingCodeChars.format(event.accumulated.length))
                        }
                        is HtmlAgentEvent.FileCreated -> {
                            val versionInfo = if (event.isNewVersion) "v${event.fileInfo.version}" else Strings.newFile
                            updateNotification(Strings.fileCreatedVersion.format(event.fileInfo.name, versionInfo))
                            AppLogger.w(TAG, "File created: ${event.fileInfo.name}, version=${event.fileInfo.version}")
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
                            // Delay stopping the foreground service
                            delay(3000)
                            stopForegroundCompat()
                        }
                        else -> {}
                    }
                }
            } catch (e: CancellationException) {
                AppLogger.w(TAG, "Generation cancelled")
                _eventFlow.emit(HtmlAgentEvent.Error(Strings.generationCancelled))
                releaseWakeLock()
            } catch (e: Exception) {
                AppLogger.e(TAG, "Generation error", e)
                _eventFlow.emit(HtmlAgentEvent.Error(Strings.generationFailed.format(e.message)))
                releaseWakeLock()
            } finally {
                _isGenerating.value = false
            }
        }
    }
    
    /**
     * Cancel the current generation job
     */
    fun cancelGeneration() {
        currentJob?.cancel()
        _isGenerating.value = false
        releaseWakeLock()
        updateNotification(Strings.generationCancelled)
        serviceScope.launch {
            delay(1000)
            stopForegroundCompat()
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }
    
    /**
     * Acquire a WakeLock
     */
    private fun acquireWakeLock() {
        try {
            wakeLock?.let {
                if (!it.isHeld) {
                    it.acquire(10 * 60 * 1000L) // Up to 10 minutes
                    AppLogger.w(TAG, "WakeLock acquired")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to acquire WakeLock", e)
        }
    }
    
    /**
     * Release the WakeLock
     */
    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    AppLogger.w(TAG, "WakeLock released")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to release WakeLock", e)
        }
    }
    
    /**
     * Create the notification channel
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
     * Build the foreground notification
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
     * Refresh the notification content
     */
    private fun updateNotification(content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(content))
    }
}
