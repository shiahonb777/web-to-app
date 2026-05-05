package com.webtoapp.core.forcedrun




enum class ForcedRunMode {
    FIXED_TIME,
    COUNTDOWN,
    DURATION
}









data class ForcedRunConfig(
    val enabled: Boolean = false,
    val mode: ForcedRunMode = ForcedRunMode.FIXED_TIME,


    val startTime: String = "08:00",
    val endTime: String = "12:00",
    val activeDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),


    val countdownMinutes: Int = 60,


    val accessStartTime: String = "08:00",
    val accessEndTime: String = "22:00",
    val accessDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),


    val protectionLevel: ProtectionLevel = ProtectionLevel.MAXIMUM,


    val blockSystemUI: Boolean = true,
    val blockBackButton: Boolean = true,
    val blockHomeButton: Boolean = true,
    val blockRecentApps: Boolean = true,
    val blockNotifications: Boolean = true,
    val blockPowerButton: Boolean = false,
    val showCountdown: Boolean = true,
    val allowEmergencyExit: Boolean = false,
    val emergencyPassword: String? = null,


    val showStartNotification: Boolean = true,
    val showEndNotification: Boolean = true,
    val warningBeforeEnd: Int = 5,


    val persistCountdown: Boolean = true
) {
    companion object {

        val DISABLED = ForcedRunConfig(enabled = false)


        val STUDY_MODE = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.FIXED_TIME,
            startTime = "08:00",
            endTime = "12:00",
            activeDays = listOf(1, 2, 3, 4, 5),
            blockSystemUI = true,
            blockBackButton = true,
            blockHomeButton = true,
            showCountdown = true
        )


        val FOCUS_MODE = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.COUNTDOWN,
            countdownMinutes = 25,
            blockSystemUI = true,
            blockBackButton = true,
            showCountdown = true,
            allowEmergencyExit = true
        )


        val KIDS_MODE = ForcedRunConfig(
            enabled = true,
            mode = ForcedRunMode.DURATION,
            accessStartTime = "08:00",
            accessEndTime = "20:00",
            accessDays = listOf(1, 2, 3, 4, 5, 6, 7),
            blockSystemUI = true,
            blockBackButton = true,
            blockHomeButton = true,
            blockRecentApps = true,
            blockPowerButton = true,
            emergencyPassword = "1234"
        )
    }
}
