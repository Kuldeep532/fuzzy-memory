package com.nexuswavetech.nexusplus.features.notifications

import kotlinx.coroutines.flow.Flow

/**
 * Notification data model — platform-agnostic.
 */
data class NexusNotification(
    val id:          String,
    val title:       String,
    val body:        String,
    val timestampMs: Long,
    val isRead:      Boolean = false,
    val category:    String  = "system",
)

/**
 * Platform-agnostic notification repository.
 * Android actual uses DataStore; iOS/Desktop use NSUserDefaults/java.util.prefs.
 */
expect class NotificationRepository {
    val notifications: Flow<List<NexusNotification>>

    suspend fun markAsRead(id: String)
    suspend fun markAllRead()
    suspend fun deleteNotification(id: String)
    suspend fun addNotification(notif: NexusNotification)

    /**
     * Admin gate: Only an [com.nexuswavetech.nexusplus.core.UserSession.Authenticated]
     * user with [com.nexuswavetech.nexusplus.core.UserSession.Authenticated.isAdmin] == true
     * may send notifications. Returns true if successful.
     */
    suspend fun sendAdminNotification(
        title:    String,
        body:     String,
        category: String,
        session:  com.nexuswavetech.nexusplus.core.UserSession,
    ): Boolean
}
