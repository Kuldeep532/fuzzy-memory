package com.nexuswavetech.nexusplus.features.notifications

import com.nexuswavetech.nexusplus.core.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop (JVM) stub implementation of [NotificationRepository].
 * Uses in-memory storage; notifications are not persisted across restarts.
 */
actual class NotificationRepository {

    private val _notifications = MutableStateFlow<List<NexusNotification>>(emptyList())
    actual val notifications: Flow<List<NexusNotification>> = _notifications.asStateFlow()

    actual suspend fun markAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }

    actual suspend fun markAllRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
    }

    actual suspend fun deleteNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    actual suspend fun addNotification(notif: NexusNotification) {
        _notifications.value = listOf(notif) + _notifications.value
    }

    actual suspend fun sendAdminNotification(
        title:    String,
        body:     String,
        category: String,
        session:  UserSession,
    ): Boolean {
        if (session !is UserSession.Authenticated || !session.isAdmin) return false
        addNotification(
            NexusNotification(
                id          = "desktop-${System.nanoTime()}",
                title       = title,
                body        = body,
                timestampMs = System.currentTimeMillis(),
                category    = category,
            )
        )
        return true
    }
}
