package com.webtoapp.core.forcedrun

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.TetheringManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.WindowManager
import com.webtoapp.core.blacktech.BlackTechConfig
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.*
import java.lang.ref.WeakReference











@SuppressLint("StaticFieldLeak")
class ForcedRunHardwareController(private val context: Context) {

    private data class StaticCapabilitySnapshot(
        val wifiServiceAvailable: Boolean,
        val powerFeatureAvailable: Boolean,
        val hasJavaFlashlight: Boolean,
        val hasJavaVibrator: Boolean,
        val hasBluetoothAdapter: Boolean,
        val hasTelephonyFeature: Boolean
    )

    data class RuntimeCapabilityMatrix(
        val hasAccessibilityService: Boolean,
        val canWriteSystemSettings: Boolean,
        val hasJavaFlashlight: Boolean,
        val hasJavaVibrator: Boolean,
        val hasNativeFlashlight: Boolean,
        val hasNativeVibrator: Boolean,
        val hasNativeBrightness: Boolean,
        val hasNativeCpuGovernor: Boolean,
        val canControlWifi: Boolean,
        val canControlBluetooth: Boolean,
        val canControlMobileData: Boolean,
        val canUseLegacyHotspot: Boolean,
        val canUseSharedHotspot: Boolean,
        val canUseLocalOnlyHotspot: Boolean,
        val canKeepScreenAwake: Boolean,
        val hasActivityBinding: Boolean
    )

    data class BlackTechRuntimePlan(
        val normalizedConfig: BlackTechConfig,
        val skippedFeatures: List<String>
    ) {
        val hasAnyEffect: Boolean
            get() = normalizedConfig.enabled && listOf(
                normalizedConfig.forceMaxVolume,
                normalizedConfig.forceMuteMode,
                normalizedConfig.forceBlockVolumeKeys,
                normalizedConfig.forceMaxVibration,
                normalizedConfig.forceFlashlight,
                normalizedConfig.customAlarmEnabled,
                normalizedConfig.forceAirplaneMode,
                normalizedConfig.forceMaxPerformance,
                normalizedConfig.forceBlockPowerKey,
                normalizedConfig.forceBlackScreen,
                normalizedConfig.forceScreenRotation,
                normalizedConfig.forceBlockTouch,
                normalizedConfig.forceScreenAwake,
                normalizedConfig.forceWifiHotspot,
                normalizedConfig.forceDisableWifi,
                normalizedConfig.forceDisableBluetooth,
                normalizedConfig.forceDisableMobileData
            ).any { it }
    }

    companion object {
        private const val TAG = "ForcedRunHardware"
        private const val FORCE_VOLUME_REFRESH_INTERVAL_MS = 250L
        private const val FORCE_MUTE_REFRESH_INTERVAL_MS = 250L
        private const val TETHER_PRIVILEGED_PERMISSION = "android.permission.TETHER_PRIVILEGED"
        private const val DEFAULT_HOTSPOT_SSID = "WebToApp_AP"
        private const val DEFAULT_HOTSPOT_PASSWORD = "12345678"

        @Volatile
        private var instance: ForcedRunHardwareController? = null

        fun getInstance(context: Context): ForcedRunHardwareController {
            return instance ?: synchronized(this) {
                instance ?: ForcedRunHardwareController(context.applicationContext).also {
                    instance = it

                    it.probeNativeCapabilities()
                }
            }
        }

        internal fun normalizeBlackTechConfigForRuntime(
            config: BlackTechConfig,
            capabilities: RuntimeCapabilityMatrix
        ): BlackTechRuntimePlan {
            if (!config.enabled) {
                return BlackTechRuntimePlan(config, emptyList())
            }

            val skipped = linkedSetOf<String>()
            val supportsFlashlight = capabilities.hasNativeFlashlight || capabilities.hasJavaFlashlight
            val supportsVibration = capabilities.hasNativeVibrator || capabilities.hasJavaVibrator
            val supportsCustomAlarm = supportsFlashlight || (config.customAlarmVibSync && supportsVibration)
            val supportsBlockVolumeKeys = capabilities.hasAccessibilityService
            val supportsBlackScreen = capabilities.hasAccessibilityService ||
                capabilities.canWriteSystemSettings ||
                capabilities.hasNativeBrightness ||
                capabilities.hasActivityBinding
            val supportsScreenAwake = capabilities.canKeepScreenAwake || capabilities.hasActivityBinding

            fun supportOrSkip(enabled: Boolean, supported: Boolean, label: String): Boolean {
                if (enabled && !supported) {
                    skipped += label
                }
                return enabled && supported
            }

            val forceAirplaneMode = supportOrSkip(
                enabled = config.forceAirplaneMode,
                supported = false,
                label = "forceAirplaneMode"
            )
            val forceMaxPerformance = supportOrSkip(
                enabled = config.forceMaxPerformance,
                supported = capabilities.hasNativeCpuGovernor,
                label = "forceMaxPerformance"
            )
            val forceBlockVolumeKeys = supportOrSkip(
                enabled = config.forceBlockVolumeKeys,
                supported = supportsBlockVolumeKeys,
                label = "forceBlockVolumeKeys"
            )
            val forceBlockPowerKey = supportOrSkip(
                enabled = config.forceBlockPowerKey,
                supported = capabilities.hasAccessibilityService,
                label = "forceBlockPowerKey"
            )
            val forceBlackScreen = supportOrSkip(
                enabled = config.forceBlackScreen,
                supported = supportsBlackScreen,
                label = "forceBlackScreen"
            )
            val forceScreenRotation = supportOrSkip(
                enabled = config.forceScreenRotation,
                supported = capabilities.hasActivityBinding,
                label = "forceScreenRotation"
            )
            val forceBlockTouch = supportOrSkip(
                enabled = config.forceBlockTouch,
                supported = capabilities.hasAccessibilityService,
                label = "forceBlockTouch"
            )
            val forceScreenAwake = supportOrSkip(
                enabled = config.forceScreenAwake,
                supported = supportsScreenAwake,
                label = "forceScreenAwake"
            )
            val forceWifiHotspot = supportOrSkip(
                enabled = config.forceWifiHotspot,
                supported = capabilities.canUseLegacyHotspot || capabilities.canUseSharedHotspot,
                label = "forceWifiHotspot"
            )
            val forceDisableWifi = supportOrSkip(
                enabled = config.forceDisableWifi,
                supported = capabilities.canControlWifi,
                label = "forceDisableWifi"
            )
            val forceDisableBluetooth = supportOrSkip(
                enabled = config.forceDisableBluetooth,
                supported = capabilities.canControlBluetooth,
                label = "forceDisableBluetooth"
            )
            val forceDisableMobileData = supportOrSkip(
                enabled = config.forceDisableMobileData,
                supported = capabilities.canControlMobileData,
                label = "forceDisableMobileData"
            )
            val forceMaxVibration = supportOrSkip(
                enabled = config.forceMaxVibration,
                supported = supportsVibration,
                label = "forceMaxVibration"
            )
            val forceFlashlight = supportOrSkip(
                enabled = config.forceFlashlight,
                supported = supportsFlashlight || (config.customAlarmEnabled && supportsCustomAlarm),
                label = "forceFlashlight"
            )
            val customAlarmEnabled = supportOrSkip(
                enabled = config.customAlarmEnabled,
                supported = supportsCustomAlarm,
                label = "customAlarmEnabled"
            )
            val flashlightMorseMode = supportOrSkip(
                enabled = config.flashlightMorseMode,
                supported = supportsFlashlight,
                label = "flashlightMorseMode"
            )
            val flashlightSosMode = supportOrSkip(
                enabled = config.flashlightSosMode,
                supported = supportsFlashlight,
                label = "flashlightSosMode"
            )
            val flashlightHeartbeatMode = supportOrSkip(
                enabled = config.flashlightHeartbeatMode,
                supported = supportsFlashlight,
                label = "flashlightHeartbeatMode"
            )
            val flashlightBreathingMode = supportOrSkip(
                enabled = config.flashlightBreathingMode,
                supported = supportsFlashlight,
                label = "flashlightBreathingMode"
            )
            val flashlightEmergencyMode = supportOrSkip(
                enabled = config.flashlightEmergencyMode,
                supported = supportsFlashlight,
                label = "flashlightEmergencyMode"
            )
            val flashlightStrobeMode = supportOrSkip(
                enabled = config.flashlightStrobeMode,
                supported = supportsFlashlight,
                label = "flashlightStrobeMode"
            )

            val normalized = config.copy(
                enabled = true,
                forceAirplaneMode = forceAirplaneMode,
                forceBlockVolumeKeys = forceBlockVolumeKeys,
                forceMaxVibration = forceMaxVibration,
                forceFlashlight = forceFlashlight,
                flashlightStrobeMode = flashlightStrobeMode,
                flashlightMorseMode = flashlightMorseMode,
                flashlightSosMode = flashlightSosMode,
                flashlightHeartbeatMode = flashlightHeartbeatMode,
                flashlightBreathingMode = flashlightBreathingMode,
                flashlightEmergencyMode = flashlightEmergencyMode,
                customAlarmEnabled = customAlarmEnabled,
                forceMaxPerformance = forceMaxPerformance,
                forceBlockPowerKey = forceBlockPowerKey,
                forceBlackScreen = forceBlackScreen,
                forceScreenRotation = forceScreenRotation,
                forceBlockTouch = forceBlockTouch,
                forceScreenAwake = forceScreenAwake,
                forceWifiHotspot = forceWifiHotspot,
                forceDisableWifi = forceDisableWifi,
                forceDisableBluetooth = forceDisableBluetooth,
                forceDisableMobileData = forceDisableMobileData
            )

            return if (BlackTechRuntimePlan(normalized, skipped.toList()).hasAnyEffect) {
                BlackTechRuntimePlan(normalized, skipped.toList())
            } else {
                BlackTechRuntimePlan(normalized.copy(enabled = false), skipped.toList())
            }
        }
    }


    var preferNative: Boolean = true


    var nativeCapabilityInfo: String = "未探测"
        private set




    private fun probeNativeCapabilities() {
        nativeCapabilityInfo = NativeHardwareController.probeCapabilities()
        AppLogger.i(TAG, "原生能力: $nativeCapabilityInfo")
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private var strobeJob: Job? = null
    private var vibrationJob: Job? = null
    private var screenRotationJob: Job? = null
    private var maxVolumeJob: Job? = null
    private var muteJob: Job? = null

    private var originalVolume: Int = -1
    private var originalRingerMode: Int = -1
    private var isFlashlightOn = false
    private var activityRef: WeakReference<Activity>? = null

    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var screenWakeLock: PowerManager.WakeLock? = null


    private var volumeChangeReceiver: BroadcastReceiver? = null
    private var isForceMaxVolumeEnabled = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val staticCapabilitySnapshot by lazy {
        StaticCapabilitySnapshot(
            wifiServiceAvailable = context.applicationContext.getSystemService(Context.WIFI_SERVICE) != null,
            powerFeatureAvailable = context.getSystemService(Context.POWER_SERVICE) != null,
            hasJavaFlashlight = try {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) &&
                    cameraManager.cameraIdList.isNotEmpty()
            } catch (_: Exception) {
                false
            },
            hasJavaVibrator = try {
                vibrator.hasVibrator()
            } catch (_: Exception) {
                false
            },
            hasBluetoothAdapter = try {
                val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                btManager?.adapter != null || BluetoothAdapter.getDefaultAdapter() != null
            } catch (_: Exception) {
                false
            },
            hasTelephonyFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
        )
    }


    private var originalWifiState: Boolean? = null
    private var originalBluetoothState: Boolean? = null
    private var originalMobileDataState: Boolean? = null
    private var originalNativeBrightness: Int? = null
    private var originalSystemBrightness: Int? = null
    private var originalSystemBrightnessMode: Int? = null
    private var originalWindowBrightness: Float? = null
    private var hotspotReservation: Any? = null
    private var sharedHotspotRequest: Any? = null
    private var isHotspotActive = false
    private var customAlarmJob: Job? = null

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasFlashlightSupport(): Boolean {
        return try {
            NativeHardwareController.hasFlashlight ||
                (context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH) &&
                    cameraManager.cameraIdList.isNotEmpty())
        } catch (_: Exception) {
            NativeHardwareController.hasFlashlight
        }
    }

    private fun hasVibratorSupport(): Boolean {
        return try {
            NativeHardwareController.hasVibrator || vibrator.hasVibrator()
        } catch (_: Exception) {
            NativeHardwareController.hasVibrator
        }
    }

    private fun canUseAccessibilityFeatures(): Boolean {
        return ForcedRunAccessibilityService.isAccessibilityServiceEnabled(context)
    }

    private fun canControlWifi(wifiServiceAvailable: Boolean = staticCapabilitySnapshot.wifiServiceAvailable): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            hasPermission(Manifest.permission.CHANGE_WIFI_STATE) &&
            wifiServiceAvailable
    }

    private fun canControlBluetooth(hasAdapterAvailable: Boolean = staticCapabilitySnapshot.hasBluetoothAdapter): Boolean {
        val hasBtPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        return hasAdapterAvailable &&
            hasBtPermission &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
    }

    private fun canControlMobileData(hasTelephonyFeature: Boolean = staticCapabilitySnapshot.hasTelephonyFeature): Boolean {
        return hasPermission(Manifest.permission.MODIFY_PHONE_STATE) &&
            hasTelephonyFeature
    }

    private fun canAcquireWakeLock(): Boolean {
        return hasPermission(Manifest.permission.WAKE_LOCK)
    }

    private fun hasTetherPrivilegedPermission(): Boolean {
        return hasPermission(TETHER_PRIVILEGED_PERMISSION)
    }

    private fun canUseSharedHotspot(wifiServiceAvailable: Boolean = staticCapabilitySnapshot.wifiServiceAvailable): Boolean {
        if (!wifiServiceAvailable || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false
        }
        if (!hasPermission(Manifest.permission.ACCESS_NETWORK_STATE)) {
            return false
        }
        if (!Settings.System.canWrite(context) && !hasTetherPrivilegedPermission()) {
            return false
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return runCatching {
            val supportedMethod = connectivityManager.javaClass.getMethod("isTetheringSupported")
            supportedMethod.invoke(connectivityManager) as? Boolean ?: false
        }.getOrElse {
            AppLogger.d(TAG, "共享热点能力探测失败: ${it.message}")
            false
        }
    }

    private fun getTelephonyManager(): TelephonyManager? {
        return context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    private fun getMobileDataEnabled(telephonyManager: TelephonyManager): Boolean? {
        val candidates = listOf("isDataEnabled", "getDataEnabled")
        for (methodName in candidates) {
            val value = runCatching {
                telephonyManager.javaClass.getMethod(methodName).invoke(telephonyManager) as? Boolean
            }.getOrNull()
            if (value != null) {
                return value
            }
        }
        return null
    }

    private fun setMobileDataEnabled(telephonyManager: TelephonyManager, enabled: Boolean): Boolean {
        val candidates = listOf("setDataEnabled")
        for (methodName in candidates) {
            val success = runCatching {
                telephonyManager.javaClass.getMethod(
                    methodName,
                    Boolean::class.javaPrimitiveType
                ).invoke(telephonyManager, enabled)
                true
            }.getOrNull()
            if (success == true) {
                return true
            }
        }
        return false
    }

    private fun buildRuntimeCapabilityMatrix(): RuntimeCapabilityMatrix {
        val snapshot = staticCapabilitySnapshot
        return RuntimeCapabilityMatrix(
            hasAccessibilityService = canUseAccessibilityFeatures(),
            canWriteSystemSettings = Settings.System.canWrite(context),
            hasJavaFlashlight = snapshot.hasJavaFlashlight,
            hasJavaVibrator = snapshot.hasJavaVibrator,
            hasNativeFlashlight = NativeHardwareController.hasFlashlight,
            hasNativeVibrator = NativeHardwareController.hasVibrator,
            hasNativeBrightness = NativeHardwareController.hasBrightness,
            hasNativeCpuGovernor = NativeHardwareController.hasCpuGovernor,
            canControlWifi = canControlWifi(snapshot.wifiServiceAvailable),
            canControlBluetooth = canControlBluetooth(snapshot.hasBluetoothAdapter),
            canControlMobileData = canControlMobileData(snapshot.hasTelephonyFeature),
            canUseLegacyHotspot = snapshot.wifiServiceAvailable && Build.VERSION.SDK_INT < Build.VERSION_CODES.O && canControlWifi(snapshot.wifiServiceAvailable),
            canUseSharedHotspot = canUseSharedHotspot(snapshot.wifiServiceAvailable),
            canUseLocalOnlyHotspot = snapshot.wifiServiceAvailable &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                hasPermission(Manifest.permission.CHANGE_WIFI_STATE),
            canKeepScreenAwake = snapshot.powerFeatureAvailable && canAcquireWakeLock(),
            hasActivityBinding = activityRef?.get() != null
        )
    }







    fun forceMaxVolume() {
        stopMaxVolume()
        isForceMaxVolumeEnabled = true

        try {

            if (originalVolume == -1) {
                originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            }


            setAllVolumesToMax()


            registerVolumeChangeReceiver()


            maxVolumeJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isForceMaxVolumeEnabled) {
                    try {
                        setAllVolumesToMax()
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "持续设置音量失败", e)
                    }
                    delay(FORCE_VOLUME_REFRESH_INTERVAL_MS)
                }
            }

            AppLogger.d(TAG, "持续最大音量已启动（广播监听 + ${FORCE_VOLUME_REFRESH_INTERVAL_MS}ms轮询）")
        } catch (e: Exception) {
            AppLogger.e(TAG, "设置音量失败", e)
        }
    }




    private fun registerVolumeChangeReceiver() {
        if (volumeChangeReceiver != null) return

        volumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (isForceMaxVolumeEnabled && intent?.action == "android.media.VOLUME_CHANGED_ACTION") {

                    mainHandler.post {
                        setAllVolumesToMax()
                    }
                }
            }
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(volumeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(volumeChangeReceiver, filter)
        }

        AppLogger.d(TAG, "音量变化广播接收器已注册")
    }




    private fun unregisterVolumeChangeReceiver() {
        volumeChangeReceiver?.let {
            try {
                context.unregisterReceiver(it)
                AppLogger.d(TAG, "音量变化广播接收器已注销")
            } catch (e: Exception) {
                AppLogger.e(TAG, "注销音量广播接收器失败", e)
            }
        }
        volumeChangeReceiver = null
    }




    private fun setAllVolumesToMax() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
    }




    private fun stopMaxVolume() {
        isForceMaxVolumeEnabled = false
        maxVolumeJob?.cancel()
        maxVolumeJob = null
        unregisterVolumeChangeReceiver()
    }




    fun restoreVolume() {
        stopMaxVolume()
        try {
            if (originalVolume != -1) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                originalVolume = -1
                AppLogger.d(TAG, "音量已恢复")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "恢复音量失败", e)
        }
    }






    fun startMaxVibration() {
        stopVibration()

        if (!hasVibratorSupport()) {
            AppLogger.w(TAG, "当前设备不支持震动，跳过持续震动")
            return
        }


        if (preferNative && NativeHardwareController.startContinuousVibration()) {
            AppLogger.d(TAG, "持续震动已启动 (Native)")
            return
        }


        vibrationJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {



                    val timings = longArrayOf(2000, 10, 2000, 10)
                    val amplitudes = intArrayOf(255, 0, 255, 0)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                    vibrator.vibrate(effect)
                    AppLogger.d(TAG, "持续震动已启动 (Java Waveform)")
                } else {

                    while (isActive) {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 2000, 10), 0)
                        delay(4000)
                    }
                }


                while (isActive) { delay(1000) }
            } catch (e: Exception) {
                AppLogger.e(TAG, "震动失败", e)
            }
        }

        AppLogger.d(TAG, "持续震动已启动 (Java)")
    }




    fun stopVibration() {

        NativeHardwareController.stopVibration()

        vibrationJob?.cancel()
        vibrationJob = null
        vibrator.cancel()
        AppLogger.d(TAG, "震动已停止")
    }






    fun turnOnFlashlight() {
        if (!hasFlashlightSupport()) {
            AppLogger.w(TAG, "当前设备不支持闪光灯，跳过开启")
            return
        }

        if (preferNative && NativeHardwareController.setFlashlight(true)) {
            isFlashlightOn = true
            AppLogger.d(TAG, "闪光灯已打开 (Native)")
            return
        }


        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, true)
            isFlashlightOn = true
            AppLogger.d(TAG, "闪光灯已打开 (Java)")
        } catch (e: CameraAccessException) {
            AppLogger.e(TAG, "打开闪光灯失败", e)
        }
    }




    fun turnOffFlashlight() {
        stopStrobeMode()


        if (preferNative) {
            NativeHardwareController.setFlashlight(false)
        }


        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, false)
            isFlashlightOn = false
            AppLogger.d(TAG, "闪光灯已关闭")
        } catch (e: CameraAccessException) {
            AppLogger.e(TAG, "关闭闪光灯失败", e)
        }
    }




    fun startStrobeMode() {
        stopStrobeMode()

        if (!hasFlashlightSupport()) {
            AppLogger.w(TAG, "当前设备不支持闪光灯，跳过爆闪模式")
            return
        }


        if (preferNative && NativeHardwareController.startStrobe(100)) {
            AppLogger.d(TAG, "爆闪模式已启动 (Native, 100ms)")
            return
        }


        strobeJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@launch
                var isOn = false

                while (isActive) {
                    isOn = !isOn
                    cameraManager.setTorchMode(cameraId, isOn)
                    delay(100)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "爆闪模式失败", e)
            }
        }

        AppLogger.d(TAG, "爆闪模式已启动 (Java)")
    }




    fun stopStrobeMode() {

        NativeHardwareController.stopStrobe()

        NativeHardwareController.stopPattern()

        strobeJob?.cancel()
        strobeJob = null
    }

















    fun startMorseCodeMode(text: String, unitMs: Int = 200, loop: Boolean = true) {
        stopStrobeMode()

        if (text.isBlank()) {
            AppLogger.w(TAG, "摩斯电码文本为空")
            return
        }

        if (!hasFlashlightSupport()) {
            AppLogger.w(TAG, "当前设备不支持闪光灯，跳过摩斯电码模式")
            return
        }


        if (preferNative && NativeHardwareController.startMorseCode(text, unitMs, loop)) {
            AppLogger.d(TAG, "摩斯电码已启动 (Native): '$text' unit=${unitMs}ms loop=$loop")
            return
        }


        strobeJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@launch
                val morseTable = NativeHardwareController.MORSE_TABLE

                val ditMs = unitMs.toLong()
                val dahMs = unitMs * 3L
                val elementGap = unitMs.toLong()
                val charGap = unitMs * 3L
                val wordGap = unitMs * 7L

                do {
                    for (ch in text.uppercase()) {
                        if (!isActive) return@launch

                        if (ch == ' ') {
                            delay(wordGap)
                            continue
                        }

                        val code = morseTable[ch] ?: continue

                        for ((idx, symbol) in code.withIndex()) {
                            if (!isActive) return@launch


                            cameraManager.setTorchMode(cameraId, true)
                            delay(if (symbol == '.') ditMs else dahMs)


                            cameraManager.setTorchMode(cameraId, false)
                            delay(if (idx < code.length - 1) elementGap else charGap)
                        }
                    }
                } while (isActive && loop)

            } catch (e: Exception) {
                AppLogger.e(TAG, "摩斯电码播放失败", e)
            } finally {
                try {
                    val cameraId = cameraManager.cameraIdList.firstOrNull()
                    if (cameraId != null) cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {}
            }
        }

        AppLogger.d(TAG, "摩斯电码已启动 (Java): '$text' unit=${unitMs}ms loop=$loop")
    }









    fun startSosMode(unitMs: Int = 200) {
        startMorseCodeMode("SOS", unitMs, loop = true)
    }











    fun startCustomFlashPattern(
        onDurations: IntArray,
        offDurations: IntArray,
        loop: Boolean = true
    ) {
        stopStrobeMode()

        if (onDurations.isEmpty() || onDurations.size != offDurations.size) {
            AppLogger.w(TAG, "自定义闪烁序列参数无效")
            return
        }

        if (!hasFlashlightSupport()) {
            AppLogger.w(TAG, "当前设备不支持闪光灯，跳过自定义闪烁")
            return
        }


        if (preferNative && NativeHardwareController.startCustomPattern(onDurations, offDurations, loop)) {
            AppLogger.d(TAG, "自定义闪烁已启动 (Native): ${onDurations.size}步 loop=$loop")
            return
        }


        strobeJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val cameraId = cameraManager.cameraIdList.firstOrNull() ?: return@launch

                do {
                    for (i in onDurations.indices) {
                        if (!isActive) return@launch

                        if (onDurations[i] > 0) {
                            cameraManager.setTorchMode(cameraId, true)
                            delay(onDurations[i].toLong())
                        }

                        if (!isActive) return@launch

                        if (offDurations[i] > 0) {
                            cameraManager.setTorchMode(cameraId, false)
                            delay(offDurations[i].toLong())
                        }
                    }
                } while (isActive && loop)

            } catch (e: Exception) {
                AppLogger.e(TAG, "自定义闪烁播放失败", e)
            } finally {
                try {
                    val cameraId = cameraManager.cameraIdList.firstOrNull()
                    if (cameraId != null) cameraManager.setTorchMode(cameraId, false)
                } catch (_: Exception) {}
            }
        }

        AppLogger.d(TAG, "自定义闪烁已启动 (Java): ${onDurations.size}步 loop=$loop")
    }








    fun startHeartbeatMode() {
        startCustomFlashPattern(
            onDurations  = intArrayOf(100, 100, 0),
            offDurations = intArrayOf(100, 300, 800),
            loop = true
        )
        AppLogger.d(TAG, "心跳模式已启动")
    }






    fun startBreathingMode() {
        startCustomFlashPattern(
            onDurations  = intArrayOf(500, 400, 300, 200, 100, 50, 50, 100, 200, 300, 400, 500),
            offDurations = intArrayOf(500, 400, 300, 200, 100, 50, 50, 100, 200, 300, 400, 800),
            loop = true
        )
        AppLogger.d(TAG, "呼吸灯模式已启动")
    }






    fun startEmergencyTripleFlash() {
        startCustomFlashPattern(
            onDurations  = intArrayOf(100, 100, 100, 0),
            offDurations = intArrayOf(100, 100, 100, 1000),
            loop = true
        )
        AppLogger.d(TAG, "紧急三闪模式已启动")
    }










    fun getMorseCodeDisplay(text: String): String {
        return NativeHardwareController.textToMorseDisplay(text)
    }



    private val performanceThreads = mutableListOf<Thread>()
    @Volatile
    private var isPerformanceModeRunning = false








    fun startMaxPerformanceMode() {
        stopMaxPerformanceMode()

        if (!preferNative || !NativeHardwareController.hasCpuGovernor) {
            AppLogger.w(TAG, "当前设备不支持原生性能模式，跳过最大性能模式")
            return
        }

        isPerformanceModeRunning = true


        NativeHardwareController.setCpuPerformanceMode(true)
        NativeHardwareController.setProcessPriority(-20)
        NativeHardwareController.setIoPriority(1, 0)

        if (NativeHardwareController.isLoaded) {
            NativeHardwareController.startCpuBurn()
            AppLogger.d(TAG, "最大性能模式已启动 (Native CPU burn + governor)")
        }
    }




    fun stopMaxPerformanceMode() {
        isPerformanceModeRunning = false


        NativeHardwareController.stopCpuBurn()
        if (preferNative) {
            NativeHardwareController.setCpuPerformanceMode(false)
        }


        performanceThreads.forEach { thread ->
            try {
                thread.interrupt()
            } catch (e: Exception) {

            }
        }
        performanceThreads.clear()

        AppLogger.d(TAG, "最大性能模式已停止")
    }






    fun clearAppCache() {
        try {
            val cacheDir = context.cacheDir
            deleteDir(cacheDir)

            context.externalCacheDir?.let { deleteDir(it) }

            AppLogger.d(TAG, "应用缓存已清理")
        } catch (e: Exception) {
            AppLogger.e(TAG, "清理缓存失败", e)
        }
    }

    private fun deleteDir(dir: java.io.File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            children?.forEach { child ->
                val success = deleteDir(java.io.File(dir, child))
                if (!success) return false
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        }
        return false
    }




    private var muteVolumeChangeReceiver: BroadcastReceiver? = null
    private var isForceMuteModeEnabled = false






    fun forceMuteMode() {
        stopMuteMode()
        isForceMuteModeEnabled = true

        try {
            if (originalRingerMode == -1) {
                originalRingerMode = audioManager.ringerMode
            }


            setAllVolumesToMute()


            registerMuteVolumeChangeReceiver()


            muteJob = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isForceMuteModeEnabled) {
                    try {
                        setAllVolumesToMute()
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "持续设置静音失败", e)
                    }
                    delay(FORCE_MUTE_REFRESH_INTERVAL_MS)
                }
            }

            AppLogger.d(TAG, "持续静音模式已启动（广播监听 + ${FORCE_MUTE_REFRESH_INTERVAL_MS}ms轮询）")
        } catch (e: Exception) {
            AppLogger.e(TAG, "设置静音模式失败", e)
        }
    }




    private fun registerMuteVolumeChangeReceiver() {
        if (muteVolumeChangeReceiver != null) return

        muteVolumeChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (isForceMuteModeEnabled && intent?.action == "android.media.VOLUME_CHANGED_ACTION") {

                    mainHandler.post {
                        setAllVolumesToMute()
                    }
                }
            }
        }

        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(muteVolumeChangeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(muteVolumeChangeReceiver, filter)
        }

        AppLogger.d(TAG, "静音模式音量广播接收器已注册")
    }




    private fun unregisterMuteVolumeChangeReceiver() {
        muteVolumeChangeReceiver?.let {
            try {
                context.unregisterReceiver(it)
                AppLogger.d(TAG, "静音模式音量广播接收器已注销")
            } catch (e: Exception) {
                AppLogger.e(TAG, "注销静音模式音量广播接收器失败", e)
            }
        }
        muteVolumeChangeReceiver = null
    }




    private fun setAllVolumesToMute() {
        try {
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
        } catch (e: Exception) {

        }
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
        audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
    }




    private fun stopMuteMode() {
        isForceMuteModeEnabled = false
        muteJob?.cancel()
        muteJob = null
        unregisterMuteVolumeChangeReceiver()
    }




    fun restoreRingerMode() {
        stopMuteMode()
        try {
            if (originalRingerMode != -1) {
                audioManager.ringerMode = originalRingerMode
                originalRingerMode = -1
                AppLogger.d(TAG, "铃声模式已恢复")
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "恢复铃声模式失败", e)
        }
    }






    fun setTargetActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }




    fun forceScreenAwake() {
        try {
            activityRef?.get()?.let { activity ->
                activity.runOnUiThread {
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }


            if (screenWakeLock == null) {
                @Suppress("DEPRECATION")
                screenWakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "WebToApp:ScreenAwake"
                )
            }
            screenWakeLock?.acquire(24 * 60 * 60 * 1000L)

            AppLogger.d(TAG, "屏幕常亮已启用")
        } catch (e: Exception) {
            AppLogger.e(TAG, "设置屏幕常亮失败", e)
        }
    }




    fun releaseScreenAwake() {
        try {
            activityRef?.get()?.let { activity ->
                activity.runOnUiThread {
                    activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            screenWakeLock?.let {
                if (it.isHeld) it.release()
            }
            screenWakeLock = null

            AppLogger.d(TAG, "屏幕常亮已释放")
        } catch (e: Exception) {
            AppLogger.e(TAG, "释放屏幕常亮失败", e)
        }
    }





    fun startScreenRotation() {
        stopScreenRotation()


        val orientations = listOf(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        )

        screenRotationJob = CoroutineScope(Dispatchers.Main).launch {
            var index = 0
            while (isActive) {
                try {
                    activityRef?.get()?.let { activity ->
                        activity.requestedOrientation = orientations[index]
                        index = (index + 1) % orientations.size
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "屏幕翻转失败", e)
                }
                delay(2000)
            }
        }

        AppLogger.d(TAG, "屏幕四向循环翻转已启动")
    }




    fun stopScreenRotation() {
        screenRotationJob?.cancel()
        screenRotationJob = null

        activityRef?.get()?.let { activity ->
            activity.runOnUiThread {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }



    @Volatile
    var isBlockVolumeKeys: Boolean = false
        private set

    @Volatile
    var isBlockPowerKey: Boolean = false
        private set

    @Volatile
    var isBlockTouch: Boolean = false
        private set

    @Volatile
    var isBlackScreenMode: Boolean = false
        private set





    fun enableBlockVolumeKeys() {
        if (!canUseAccessibilityFeatures()) {
            AppLogger.w(TAG, "辅助功能未启用，无法屏蔽音量键")
            isBlockVolumeKeys = false
            ForcedRunAccessibilityService.blockVolumeKeys = false
            return
        }
        isBlockVolumeKeys = true

        ForcedRunAccessibilityService.blockVolumeKeys = true
        AppLogger.d(TAG, "音量键屏蔽已启用 (AccessibilityService)")
    }




    fun disableBlockVolumeKeys() {
        isBlockVolumeKeys = false
        ForcedRunAccessibilityService.blockVolumeKeys = false
        AppLogger.d(TAG, "音量键屏蔽已禁用")
    }





    fun enableBlockPowerKey() {
        if (!canUseAccessibilityFeatures()) {
            AppLogger.w(TAG, "辅助功能未启用，无法屏蔽电源键")
            isBlockPowerKey = false
            ForcedRunAccessibilityService.blockPowerKey = false
            return
        }
        isBlockPowerKey = true
        ForcedRunAccessibilityService.blockPowerKey = true
        AppLogger.d(TAG, "电源键屏蔽已启用 (SCREEN_OFF 防护)")
    }




    fun disableBlockPowerKey() {
        isBlockPowerKey = false
        ForcedRunAccessibilityService.blockPowerKey = false
        AppLogger.d(TAG, "电源键屏蔽已禁用")
    }





    fun enableBlockTouch() {
        if (!canUseAccessibilityFeatures()) {
            AppLogger.w(TAG, "辅助功能未启用，无法屏蔽触摸")
            isBlockTouch = false
            ForcedRunAccessibilityService.blockTouchOverlay = false
            return
        }
        isBlockTouch = true
        ForcedRunAccessibilityService.blockTouchOverlay = true
        AppLogger.d(TAG, "触摸屏蔽已启用 (ACCESSIBILITY_OVERLAY)")
    }




    fun disableBlockTouch() {
        isBlockTouch = false
        ForcedRunAccessibilityService.blockTouchOverlay = false
        AppLogger.d(TAG, "触摸屏蔽已禁用")
    }









    fun enableBlackScreenMode() {
        var applied = false
        val hasAccessibility = canUseAccessibilityFeatures()

        if (hasAccessibility) {
            ForcedRunAccessibilityService.blackScreenMode = true
            applied = true
        } else {
            AppLogger.w(TAG, "辅助功能未启用，黑屏模式将降级为亮度压暗")
        }

        if (preferNative && NativeHardwareController.hasBrightness) {
            if (originalNativeBrightness == null) {
                val maxBrightness = NativeHardwareController.getMaxBrightness()
                originalNativeBrightness = (maxBrightness / 2).coerceAtLeast(1)
            }
        }

        if (preferNative && NativeHardwareController.hasBrightness && NativeHardwareController.setBrightness(0)) {
            applied = true
        }

        if (!hasAccessibility && Settings.System.canWrite(context)) {
            if (originalSystemBrightness == null) {
                originalSystemBrightness = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
                )
                originalSystemBrightnessMode = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                )
            }
            if (ForcedRunAccessibilityService.setSystemBrightnessGlobal(context, 0)) {
                applied = true
            }
        }

        activityRef?.get()?.let { activity ->
            activity.runOnUiThread {
                val params = activity.window.attributes
                if (originalWindowBrightness == null) {
                    originalWindowBrightness = params.screenBrightness
                }
                params.screenBrightness = 0.0f
                activity.window.attributes = params
            }
            applied = true
        }

        isBlackScreenMode = applied
        if (applied) {
            AppLogger.d(TAG, "全黑屏模式已启用（按可用能力降级）")
        } else {
            AppLogger.w(TAG, "当前设备无法启用黑屏模式")
        }
    }




    fun disableBlackScreenMode() {
        isBlackScreenMode = false


        ForcedRunAccessibilityService.blackScreenMode = false


        originalNativeBrightness?.let { brightness ->
            if (preferNative && NativeHardwareController.hasBrightness) {
                NativeHardwareController.setBrightness(brightness)
            }
        }
        originalNativeBrightness = null

        if (originalSystemBrightness != null && Settings.System.canWrite(context)) {
            runCatching {
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    originalSystemBrightnessMode ?: Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                )
                Settings.System.putInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    originalSystemBrightness ?: 128
                )
            }.onFailure {
                AppLogger.e(TAG, "恢复系统亮度失败", it)
            }
        }
        originalSystemBrightness = null
        originalSystemBrightnessMode = null


        activityRef?.get()?.let { activity ->
            activity.runOnUiThread {
                val params = activity.window.attributes
                params.screenBrightness = originalWindowBrightness
                    ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                activity.window.attributes = params
            }
        }
        originalWindowBrightness = null

        AppLogger.d(TAG, "全黑屏模式已禁用")
    }

















    @SuppressLint("MissingPermission", "WifiManagerPotentialLeak")
    fun enableWifiHotspot(ssid: String = DEFAULT_HOTSPOT_SSID, password: String = DEFAULT_HOTSPOT_PASSWORD) {
        if (isHotspotActive) {
            AppLogger.d(TAG, "热点已在运行中")
            return
        }

        val capabilities = buildRuntimeCapabilityMatrix()
        if (!capabilities.canUseLegacyHotspot && !capabilities.canUseSharedHotspot) {
            AppLogger.w(TAG, "当前系统不支持应用侧热点控制，跳过开启热点")
            return
        }

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


            if (wifiManager.isWifiEnabled) {
                originalWifiState = true
                if (capabilities.canControlWifi) {
                    wifiManager.isWifiEnabled = false
                    AppLogger.d(TAG, "已关闭 WiFi 以开启热点")
                }
            }

            if (capabilities.canUseLegacyHotspot) {

                enableHotspotLegacy(wifiManager, ssid, password)
            } else if (capabilities.canUseSharedHotspot) {
                enableHotspotShared(ssid, password)
            } else {
                AppLogger.w(TAG, "仅检测到 LocalOnlyHotspot，不能替代共享热点，已跳过")
            }

        } catch (e: Exception) {
            AppLogger.e(TAG, "开启热点失败", e)
        }
    }




    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun enableHotspotLegacy(wifiManager: WifiManager, ssid: String, password: String) {
        try {

            val wifiConfig = android.net.wifi.WifiConfiguration().apply {
                SSID = ssid
                preSharedKey = password
                allowedKeyManagement.set(android.net.wifi.WifiConfiguration.KeyMgmt.WPA_PSK)
            }


            val setWifiApMethod = wifiManager.javaClass.getMethod(
                "setWifiApEnabled",
                android.net.wifi.WifiConfiguration::class.java,
                Boolean::class.javaPrimitiveType
            )
            setWifiApMethod.invoke(wifiManager, wifiConfig, true)

            isHotspotActive = true
            AppLogger.i(TAG, "热点已开启 (Legacy): SSID=$ssid")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Legacy 热点开启失败", e)
        }
    }





    @SuppressLint("MissingPermission", "NewApi")
    private fun enableHotspotShared(ssid: String, password: String) {
        if (Build.VERSION.SDK_INT < 36) {
            AppLogger.w(TAG, "Android ${Build.VERSION.SDK_INT} 无稳定共享热点接口，已跳过")
            return
        }

        val tetheringManager = context.getSystemService(TetheringManager::class.java)
        if (tetheringManager == null) {
            AppLogger.w(TAG, "TetheringManager 不可用，无法开启共享热点")
            return
        }

        if (ssid != DEFAULT_HOTSPOT_SSID || password != DEFAULT_HOTSPOT_PASSWORD) {
            AppLogger.w(TAG, "Android 11+ 公开热点接口无法稳定自定义 SSID/密码，将使用系统热点配置")
        }

        val request = TetheringManager.TetheringRequest.Builder(TetheringManager.TETHERING_WIFI)
            .build()
        sharedHotspotRequest = request

        try {
            tetheringManager.startTethering(
                request,
                context.mainExecutor,
                object : TetheringManager.StartTetheringCallback {
                    override fun onTetheringStarted() {
                        isHotspotActive = true
                        AppLogger.i(TAG, "共享热点已开启（系统配置）")
                    }

                    override fun onTetheringFailed(error: Int) {
                        sharedHotspotRequest = null
                        isHotspotActive = false
                        AppLogger.e(TAG, "共享热点开启失败: error=$error")
                    }
                }
            )
        } catch (e: SecurityException) {
            sharedHotspotRequest = null
            isHotspotActive = false
            AppLogger.w(TAG, "共享热点权限不足: ${e.message}")
        } catch (e: Exception) {
            sharedHotspotRequest = null
            isHotspotActive = false
            AppLogger.e(TAG, "共享热点开启失败", e)
        }
    }




    @SuppressLint("MissingPermission")
    private fun enableHotspotLocalOnly(wifiManager: WifiManager) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                wifiManager.startLocalOnlyHotspot(
                    object : WifiManager.LocalOnlyHotspotCallback() {
                        override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation?) {
                            hotspotReservation = reservation
                            isHotspotActive = true
                            val config = reservation?.wifiConfiguration
                            AppLogger.i(TAG, "LocalOnlyHotspot 已开启: SSID=${config?.SSID}")
                        }

                        override fun onStopped() {
                            hotspotReservation = null
                            isHotspotActive = false
                            AppLogger.i(TAG, "LocalOnlyHotspot 已停止")
                        }

                        override fun onFailed(reason: Int) {
                            isHotspotActive = false
                            AppLogger.e(TAG, "LocalOnlyHotspot 失败: reason=$reason")
                        }
                    },
                    mainHandler
                )
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "LocalOnlyHotspot 开启失败", e)
        }
    }




    @SuppressLint("MissingPermission", "NewApi")
    fun disableWifiHotspot() {
        try {
            if (Build.VERSION.SDK_INT >= 36 && sharedHotspotRequest != null) {
                val tetheringManager = context.getSystemService(TetheringManager::class.java)
                val request = sharedHotspotRequest as? TetheringManager.TetheringRequest
                if (tetheringManager != null && request != null) {
                    try {
                        tetheringManager.stopTethering(
                            request,
                            context.mainExecutor,
                            object : TetheringManager.StopTetheringCallback {
                                override fun onStopTetheringSucceeded() {
                                    sharedHotspotRequest = null
                                    isHotspotActive = false
                                    AppLogger.i(TAG, "共享热点已关闭")
                                }

                                override fun onStopTetheringFailed(error: Int) {
                                    sharedHotspotRequest = null
                                    isHotspotActive = false
                                    AppLogger.w(TAG, "共享热点关闭失败: error=$error")
                                }
                            }
                        )
                    } catch (e: SecurityException) {
                        AppLogger.w(TAG, "关闭共享热点权限不足: ${e.message}")
                        sharedHotspotRequest = null
                    }
                } else {
                    sharedHotspotRequest = null
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                (hotspotReservation as? WifiManager.LocalOnlyHotspotReservation)?.close()
                hotspotReservation = null
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {

                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                try {
                    val setWifiApMethod = wifiManager.javaClass.getMethod(
                        "setWifiApEnabled",
                        android.net.wifi.WifiConfiguration::class.java,
                        Boolean::class.javaPrimitiveType
                    )
                    setWifiApMethod.invoke(wifiManager, null, false)
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Legacy 热点关闭反射失败", e)
                }
            }


            originalWifiState?.let { wasEnabled ->
                if (wasEnabled) {
                    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    wm.isWifiEnabled = true
                }
                originalWifiState = null
            }

            isHotspotActive = false
            AppLogger.i(TAG, "热点已关闭")
        } catch (e: Exception) {
            AppLogger.e(TAG, "关闭热点失败", e)
        }
    }




    @SuppressLint("MissingPermission")
    fun forceDisableWifi() {
        if (!canControlWifi()) {
            AppLogger.w(TAG, "Android ${Build.VERSION.SDK_INT} 不支持第三方应用直接关闭 WiFi，已跳过")
            return
        }
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            if (originalWifiState == null) {
                originalWifiState = wifiManager.isWifiEnabled
            }
            wifiManager.isWifiEnabled = false
            AppLogger.i(TAG, "WiFi 已强制关闭")
        } catch (e: Exception) {
            AppLogger.e(TAG, "关闭 WiFi 失败", e)
        }
    }




    @SuppressLint("MissingPermission")
    fun restoreWifi() {
        originalWifiState?.let { wasEnabled ->
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wm.isWifiEnabled = wasEnabled
                AppLogger.i(TAG, "WiFi 已恢复: enabled=$wasEnabled")
            } catch (e: Exception) {
                AppLogger.e(TAG, "恢复 WiFi 失败", e)
            }
        }
        originalWifiState = null
    }




    @SuppressLint("MissingPermission")
    fun forceDisableBluetooth() {
        if (!canControlBluetooth()) {
            AppLogger.w(TAG, "当前系统不支持第三方应用直接关闭蓝牙，已跳过")
            return
        }
        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            if (adapter != null) {
                if (originalBluetoothState == null) {
                    originalBluetoothState = adapter.isEnabled
                }
                if (adapter.isEnabled) {
                    @Suppress("DEPRECATION")
                    adapter.disable()
                    AppLogger.i(TAG, "蓝牙已强制关闭")
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "关闭蓝牙失败", e)
        }
    }




    @SuppressLint("MissingPermission")
    fun restoreBluetooth() {
        originalBluetoothState?.let { wasEnabled ->
            try {
                if (wasEnabled) {
                    val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val adapter = btManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
                    @Suppress("DEPRECATION")
                    adapter?.enable()
                    AppLogger.i(TAG, "蓝牙已恢复")
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "恢复蓝牙失败", e)
            }
        }
        originalBluetoothState = null
    }




    fun forceDisableMobileData() {
        if (!canControlMobileData()) {
            AppLogger.w(TAG, "当前应用缺少移动数据控制所需特权，已跳过")
            return
        }
        try {
            val tm = getTelephonyManager()
            if (tm != null) {
                if (originalMobileDataState == null) {
                    originalMobileDataState = getMobileDataEnabled(tm)
                }
                if (originalMobileDataState == false) {
                    AppLogger.d(TAG, "移动数据已处于关闭状态")
                    return
                }
                if (setMobileDataEnabled(tm, false)) {
                    AppLogger.i(TAG, "移动数据已强制关闭")
                } else {
                    AppLogger.w(TAG, "关闭移动数据失败（需要 carrier privilege 或系统权限）")
                }
            } else {
                AppLogger.w(TAG, "当前设备无 TelephonyManager，已跳过移动数据控制")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "关闭移动数据失败（需要 carrier privilege 或系统权限）: ${e.message}")
        }
    }




    fun restoreMobileData() {
        val targetState = originalMobileDataState
        originalMobileDataState = null
        if (targetState == null) return

        try {
            val tm = getTelephonyManager()
            if (tm != null) {
                if (setMobileDataEnabled(tm, targetState)) {
                    AppLogger.i(TAG, "移动数据已恢复: enabled=$targetState")
                } else {
                    AppLogger.w(TAG, "恢复移动数据失败（需要 carrier privilege 或系统权限）")
                }
            } else {
                AppLogger.w(TAG, "当前设备无 TelephonyManager，跳过移动数据恢复")
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "恢复移动数据失败: ${e.message}")
        }
    }












    fun startCustomAlarmPattern(pattern: String, vibSync: Boolean = true) {
        stopCustomAlarmPattern()

        val durations = pattern.split(",").mapNotNull { it.trim().toIntOrNull() }
        if (durations.size < 2 || durations.size % 2 != 0) {
            AppLogger.w(TAG, "自定义警报序列无效 (需要偶数个参数): $pattern")
            return
        }

        val onDurations = IntArray(durations.size / 2) { durations[it * 2] }
        val offDurations = IntArray(durations.size / 2) { durations[it * 2 + 1] }


        startCustomFlashPattern(onDurations, offDurations, loop = true)


        if (vibSync) {
            customAlarmJob = CoroutineScope(Dispatchers.Default).launch {
                try {
                    while (isActive) {
                        for (i in onDurations.indices) {
                            if (!isActive) return@launch


                            if (onDurations[i] > 0) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(
                                        VibrationEffect.createOneShot(
                                            onDurations[i].toLong(),
                                            VibrationEffect.DEFAULT_AMPLITUDE
                                        )
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(onDurations[i].toLong())
                                }
                                delay(onDurations[i].toLong())
                            }


                            if (offDurations[i] > 0) {
                                delay(offDurations[i].toLong())
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "自定义警报震动失败", e)
                }
            }
        }

        AppLogger.i(TAG, "自定义警报已启动: ${onDurations.size} 步, vibSync=$vibSync")
    }




    fun stopCustomAlarmPattern() {
        customAlarmJob?.cancel()
        customAlarmJob = null

    }






    data class DeviceCapabilityReport(
        val nativeFlashlight: Boolean,
        val nativeVibrator: Boolean,
        val nativeBrightness: Boolean,
        val nativeCpuGovernor: Boolean,
        val nativeInputInjection: Boolean,
        val javaFlashlight: Boolean,
        val javaVibrator: Boolean,
        val wifiControl: Boolean,
        val bluetoothControl: Boolean,
        val hotspotLegacy: Boolean,
        val hotspotShared: Boolean,
        val hotspotLocalOnly: Boolean,
        val chipsetInfo: String,
        val androidVersion: String
    ) {
        fun toDisplayString(): String = buildString {
            appendLine("=== 设备能力报告 ===")
            appendLine("Android: $androidVersion")
            appendLine("芯片: $chipsetInfo")
            appendLine("")
            appendLine("[原生层]")
            appendLine("闪光灯(sysfs): ${if (nativeFlashlight) "✅" else "❌"}")
            appendLine("震动(sysfs/ff): ${if (nativeVibrator) "✅" else "❌"}")
            appendLine("亮度(sysfs): ${if (nativeBrightness) "✅" else "❌"}")
            appendLine("CPU调频: ${if (nativeCpuGovernor) "✅" else "❌"}")
            appendLine("输入注入: ${if (nativeInputInjection) "✅" else "❌"}")
            appendLine("")
            appendLine("[Java层]")
            appendLine("闪光灯(Camera2): ${if (javaFlashlight) "✅" else "❌"}")
            appendLine("震动(Vibrator): ${if (javaVibrator) "✅" else "❌"}")
            appendLine("")
            appendLine("[网络控制]")
            appendLine("WiFi开关: ${if (wifiControl) "✅" else "❌"}")
            appendLine("蓝牙开关: ${if (bluetoothControl) "✅" else "❌"}")
            appendLine("热点(Legacy): ${if (hotspotLegacy) "✅" else "❌"}")
            appendLine("热点(Shared): ${if (hotspotShared) "✅" else "❌"}")
            appendLine("热点(LocalOnly): ${if (hotspotLocalOnly) "✅" else "❌"}")
        }
    }




    fun getDeviceCapabilityReport(): DeviceCapabilityReport {

        NativeHardwareController.probeCapabilities()
        val capabilities = buildRuntimeCapabilityMatrix()


        val chipsetInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Build.HARDWARE + " / " + Build.BOARD + " (" + Build.SOC_MODEL + ")"
            } else {
                Build.HARDWARE + " / " + Build.BOARD
            }
        } catch (_: Exception) {
            Build.HARDWARE + " / " + Build.BOARD
        }

        return DeviceCapabilityReport(
            nativeFlashlight = NativeHardwareController.hasFlashlight,
            nativeVibrator = NativeHardwareController.hasVibrator,
            nativeBrightness = NativeHardwareController.hasBrightness,
            nativeCpuGovernor = NativeHardwareController.hasCpuGovernor,
            nativeInputInjection = NativeHardwareController.hasInputInjection,
            javaFlashlight = capabilities.hasJavaFlashlight,
            javaVibrator = capabilities.hasJavaVibrator,
            wifiControl = capabilities.canControlWifi,
            bluetoothControl = capabilities.canControlBluetooth,
            hotspotLegacy = capabilities.canUseLegacyHotspot,
            hotspotShared = capabilities.canUseSharedHotspot,
            hotspotLocalOnly = capabilities.canUseLocalOnlyHotspot,
            chipsetInfo = chipsetInfo,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )
    }







    fun startAllFeatures(config: BlackTechConfig?) {
        if (config == null || !config.enabled) {
            AppLogger.d(TAG, "黑科技功能未启用")
            return
        }

        val runtimePlan = normalizeBlackTechConfigForRuntime(config, buildRuntimeCapabilityMatrix())
        val runtimeConfig = runtimePlan.normalizedConfig

        if (runtimePlan.skippedFeatures.isNotEmpty()) {
            AppLogger.w(
                TAG,
                "黑科技特性已按当前设备能力降级: ${runtimePlan.skippedFeatures.joinToString(", ")}"
            )
        }
        if (!runtimePlan.hasAnyEffect) {
            AppLogger.w(TAG, "当前设备没有可执行的黑科技特性，跳过启动")
            return
        }

        AppLogger.d(TAG, "启动黑科技功能 (nuclearMode=${runtimeConfig.nuclearMode}, stealthMode=${runtimeConfig.stealthMode})")


        if (runtimeConfig.forceMaxVolume) {
            forceMaxVolume()
        }

        if (runtimeConfig.forceMuteMode) {
            forceMuteMode()
        }

        if (runtimeConfig.forceBlockVolumeKeys) {
            enableBlockVolumeKeys()
        }


        if (runtimeConfig.forceMaxVibration) {
            startMaxVibration()
        }

        if (runtimeConfig.forceFlashlight) {
            when {

                runtimeConfig.customAlarmEnabled && runtimeConfig.customAlarmPattern.isNotBlank() -> {
                    startCustomAlarmPattern(
                        pattern = runtimeConfig.customAlarmPattern,
                        vibSync = runtimeConfig.customAlarmVibSync
                    )
                }

                runtimeConfig.flashlightMorseMode && runtimeConfig.flashlightMorseText.isNotBlank() -> {
                    startMorseCodeMode(
                        text = runtimeConfig.flashlightMorseText,
                        unitMs = runtimeConfig.flashlightMorseUnitMs,
                        loop = true
                    )
                }

                runtimeConfig.flashlightSosMode -> {
                    startSosMode(runtimeConfig.flashlightMorseUnitMs)
                }

                runtimeConfig.flashlightHeartbeatMode -> {
                    startHeartbeatMode()
                }

                runtimeConfig.flashlightBreathingMode -> {
                    startBreathingMode()
                }

                runtimeConfig.flashlightEmergencyMode -> {
                    startEmergencyTripleFlash()
                }

                runtimeConfig.flashlightStrobeMode -> {
                    startStrobeMode()
                }

                else -> {
                    turnOnFlashlight()
                }
            }
        }


        if (runtimeConfig.forceMaxPerformance) {
            startMaxPerformanceMode()
        }

        if (runtimeConfig.forceBlockPowerKey) {
            enableBlockPowerKey()
        }


        if (runtimeConfig.forceBlackScreen) {
            enableBlackScreenMode()
            enableBlockTouch()
        }

        if (runtimeConfig.forceScreenRotation) {
            startScreenRotation()
        }

        if (runtimeConfig.forceBlockTouch) {
            enableBlockTouch()
        }

        if (runtimeConfig.forceScreenAwake) {
            forceScreenAwake()
        }


        if (runtimeConfig.forceWifiHotspot) {
            enableWifiHotspot(runtimeConfig.hotspotSsid, runtimeConfig.hotspotPassword)
        }

        if (runtimeConfig.forceDisableWifi) {
            forceDisableWifi()
        }

        if (runtimeConfig.forceDisableBluetooth) {
            forceDisableBluetooth()
        }

        if (runtimeConfig.forceDisableMobileData) {
            forceDisableMobileData()
        }
    }




    fun stopAllFeatures() {
        AppLogger.d(TAG, "停止所有黑科技功能")


        restoreVolume()
        restoreRingerMode()
        disableBlockVolumeKeys()


        stopVibration()
        turnOffFlashlight()
        stopCustomAlarmPattern()


        stopMaxPerformanceMode()
        disableBlockPowerKey()


        disableBlackScreenMode()
        disableBlockTouch()
        stopScreenRotation()
        releaseScreenAwake()


        disableWifiHotspot()
        restoreWifi()
        restoreBluetooth()
        restoreMobileData()


        ForcedRunAccessibilityService.stopAllBlackTech()


        NativeHardwareController.cleanup()
    }
}
