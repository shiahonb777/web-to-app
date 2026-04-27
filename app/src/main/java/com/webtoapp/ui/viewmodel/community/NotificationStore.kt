package com.webtoapp.ui.viewmodel.community

import com.webtoapp.core.auth.AuthResult
import com.webtoapp.core.cloud.CloudApiClient
import com.webtoapp.core.cloud.NotificationItem
import com.webtoapp.core.i18n.Strings
import com.webtoapp.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationStore(
    private val scope: CoroutineScope,
    private val api: CloudApiClient,
    private val postFailureReport: (FailureReportParams) -> Unit
) {
    companion object {
        private const val TAG = "NotificationStore"
    }

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _notificationsLoading = MutableStateFlow(false)
    val notificationsLoading: StateFlow<Boolean> = _notificationsLoading.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun loadNotifications() {
        scope.launch {
            _notificationsLoading.value = true
            try {
                val result = api.listNotifications()
                when (result) {
                    is AuthResult.Success -> {
                        val (notifications, _) = result.data
                        _notifications.value = notifications
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load notifications failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.notificationsLoadFailed,
                            stage = Strings.loadNotificationsStage,
                            summary = Strings.notificationsFailedSummary,
                            serviceMessage = result.message
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Failed to load notifications", e)
                postFailureReport(FailureReportParams(
                    title = Strings.notificationsLoadFailed,
                    stage = Strings.loadNotificationsStage,
                    summary = Strings.notificationsExceptionSummary,
                    throwable = e
                ))
            } finally {
                _notificationsLoading.value = false
            }
        }
    }

    fun loadUnreadCount() {
        scope.launch {
            try {
                val result = api.getUnreadNotificationCount()
                when (result) {
                    is AuthResult.Success -> _unreadCount.value = result.data
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Load unread notification count failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.unreadCountLoadFailed,
                            stage = Strings.loadUnreadCountStage,
                            summary = Strings.unreadCountFailedSummary,
                            serviceMessage = result.message
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Load unread notification count exception", e)
                postFailureReport(FailureReportParams(
                    title = Strings.unreadCountLoadFailed,
                    stage = Strings.loadUnreadCountStage,
                    summary = Strings.unreadCountExceptionSummary,
                    throwable = e
                ))
            }
        }
    }

    fun markNotificationRead(notificationId: Int, onSuccess: () -> Unit = {}) {
        scope.launch {
            try {
                when (val result = api.markNotificationRead(notificationId)) {
                    is AuthResult.Success -> {
                        val wasUnread = _notifications.value.any { it.id == notificationId && !it.isRead }
                        _notifications.value = _notifications.value.map { notification ->
                            if (notification.id == notificationId) notification.copy(isRead = true) else notification
                        }
                        if (wasUnread) {
                            _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)
                        }
                        onSuccess()
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Mark notification read failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.notificationReadFailed,
                            stage = Strings.markNotificationReadStage,
                            summary = Strings.notificationReadFailedSummary,
                            serviceMessage = result.message,
                            extraContext = "notification_id: $notificationId"
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Mark notification read exception", e)
                postFailureReport(FailureReportParams(
                    title = Strings.notificationReadFailed,
                    stage = Strings.markNotificationReadStage,
                    summary = Strings.notificationReadExceptionSummary,
                    throwable = e,
                    extraContext = "notification_id: $notificationId"
                ))
            }
        }
    }

    fun markAllNotificationsRead() {
        scope.launch {
            try {
                when (val result = api.markAllNotificationsRead()) {
                    is AuthResult.Success -> {
                        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
                        _unreadCount.value = 0
                    }
                    is AuthResult.Error -> {
                        AppLogger.e(TAG, "Mark all notifications read failed: ${result.message}")
                        postFailureReport(FailureReportParams(
                            title = Strings.allNotificationsReadFailed,
                            stage = Strings.markAllReadStage,
                            summary = Strings.allNotificationsReadFailedSummary,
                            serviceMessage = result.message
                        ))
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Mark all notifications read exception", e)
                postFailureReport(FailureReportParams(
                    title = Strings.allNotificationsReadFailed,
                    stage = Strings.markAllReadStage,
                    summary = Strings.allNotificationsReadExceptionSummary,
                    throwable = e
                ))
            }
        }
    }
}
