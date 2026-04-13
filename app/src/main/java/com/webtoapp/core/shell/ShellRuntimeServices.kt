package com.webtoapp.core.shell

import com.webtoapp.core.activation.ActivationManager
import com.webtoapp.core.adblock.AdBlocker
import com.webtoapp.core.announcement.AnnouncementManager

/**
 * Shell 运行时专用服务入口。
 *
 * 这里只给 Shell/自启动/广播这类无法直接走 Compose 注入的路径使用，
 * 明确和编辑器主线隔离，避免再把依赖倒回 Application 静态 getter。
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
