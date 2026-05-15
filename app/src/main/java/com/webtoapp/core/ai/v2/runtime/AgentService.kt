package com.webtoapp.core.ai.v2.runtime

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
import androidx.core.app.NotificationCompat
import com.webtoapp.R
import com.webtoapp.core.ai.coding.AiCodingType
import com.webtoapp.core.ai.coding.ProjectFileManager
import com.webtoapp.core.ai.v2.agent.AgentEvent
import com.webtoapp.core.ai.v2.agent.AgentRunner
import com.webtoapp.core.ai.v2.data.CapabilityCache
import com.webtoapp.core.ai.v2.llm.DefaultLlmGateway
import com.webtoapp.core.ai.v2.llm.LlmMessage
import com.webtoapp.core.ai.v2.tool.ToolContext
import com.webtoapp.core.ai.v2.tool.ToolRegistry
import com.webtoapp.core.i18n.AppLanguage
import com.webtoapp.data.model.ApiKeyConfig
import com.webtoapp.data.model.SavedModel
import com.webtoapp.ui.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AgentService : Service() {
    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var fg = false

    private val _events = MutableSharedFlow<AgentEvent>(replay = 0, extraBufferCapacity = 128)
    val events: SharedFlow<AgentEvent> = _events.asSharedFlow()
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private lateinit var runner: AgentRunner

    inner class LocalBinder : Binder() { fun service(): AgentService = this@AgentService }
    override fun onBind(intent: Intent?): IBinder = binder
    override fun onCreate() { super.onCreate(); ensureChannel(); runner = AgentRunner(DefaultLlmGateway.create(this), CapabilityCache(this)) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { if(!fg){startForeground(NID, buildNotif("AI 编程服务已启动"));fg=true}; return START_NOT_STICKY }

    fun start(req: StartRequest) {
        job?.cancel(); if(!fg){startForeground(NID,buildNotif("AI 编程服务已启动"));fg=true}; acquireWake(); _isRunning.value=true
        job = scope.launch {
            try {
                val ctx = ToolContext(this@AgentService, req.sessionId, req.codingType, ProjectFileManager(this@AgentService), req.textModel, req.apiKey, req.imageModel, req.imageApiKey)
                val reg = ToolRegistry.defaultFor(req.codingType, req.imageModel!=null)
                runner.run(AgentRunner.Input(req.history, req.userRequirement, req.codingType, req.language, req.userRules, ctx, reg, req.temperature, req.maxTurns)).collect { _events.emit(it) }
            } catch(e:Exception){ _events.emit(AgentEvent.Failed(e.message?:"unknown error")) }
            finally { _isRunning.value=false; releaseWake() }
        }
    }
    fun cancel() { job?.cancel(); _isRunning.value=false }
    override fun onDestroy() { super.onDestroy(); job?.cancel(); scope.cancel(); releaseWake() }

    private fun ensureChannel() { if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){val nm=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; if(nm.getNotificationChannel(CH)==null) nm.createNotificationChannel(NotificationChannel(CH,"AI 编程",NotificationManager.IMPORTANCE_LOW))} }
    private fun buildNotif(text:String) = NotificationCompat.Builder(this,CH).setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("AI 编程").setContentText(text).setOngoing(true).setOnlyAlertOnce(true).setPriority(NotificationCompat.PRIORITY_LOW).setContentIntent(PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()
    private fun acquireWake() { if(wakeLock?.isHeld==true)return; val pm=getSystemService(Context.POWER_SERVICE) as PowerManager; wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"WebToApp:AgentV2").apply{setReferenceCounted(false);acquire(15*60*1000L)} }
    private fun releaseWake() { runCatching{if(wakeLock?.isHeld==true)wakeLock?.release()}; wakeLock=null }

    data class StartRequest(val sessionId:String, val codingType:AiCodingType, val language:AppLanguage, val userRequirement:String, val history:List<LlmMessage>, val userRules:List<String>, val textModel:SavedModel, val apiKey:ApiKeyConfig, val imageModel:SavedModel?=null, val imageApiKey:ApiKeyConfig?=null, val temperature:Float=0.7f, val maxTurns:Int=6)
    companion object { private const val CH="ai_agent_v2"; private const val NID=1101 }
}
