package com.webtoapp.core.shell

import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager

/**
 * runtime entry service.
 *
 * path startup usage.
 * dependency.
 */
object ShellRuntimeServices {

    private var shellModeManager: ShellModeManager? = null
    private var activationManager: ActivationManager? = null
    private var announcementManager: AnnouncementManager? = null
    private var adBlocker: AdBlocker? = null

    fun initialize(
        shellModeManager: ShellModeManager,
        activationManager: ActivationManager,
        announcementManager: AnnouncementManager,
        adBlocker: AdBlocker,
    ) {
        this.shellModeManager = shellModeManager
        this.activationManager = activationManager
        this.announcementManager = announcementManager
        this.adBlocker = adBlocker
    }

    fun reset() {
        shellModeManager = null
        activationManager = null
        announcementManager = null
        adBlocker = null
    }

    val shellMode: ShellModeManager
        get() = requireNotNull(shellModeManager) { "ShellRuntimeServices 尚未初始化 shellMode" }

    val activation: ActivationManager
        get() = requireNotNull(activationManager) { "ShellRuntimeServices 尚未初始化 activation" }

    val announcement: AnnouncementManager
        get() = requireNotNull(announcementManager) { "ShellRuntimeServices 尚未初始化 announcement" }

    val adBlock: AdBlocker
        get() = requireNotNull(adBlocker) { "ShellRuntimeServices 尚未初始化 adBlock" }
}