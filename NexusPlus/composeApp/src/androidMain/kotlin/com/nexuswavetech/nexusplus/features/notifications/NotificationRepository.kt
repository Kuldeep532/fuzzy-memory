package com.nexuswavetech.nexusplus.features.notifications

import android.content.Context
import com.nexuswavetech.nexusplus.core.FeatureCatalog
import com.nexuswavetech.nexusplus.sound.NexusSoundManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

actual class NotificationRepository(private val context: Context) {

    private val Context.notifStore by preferencesDataStore(name = "nexus_notifications")
    private val NOTIF_KEY = stringPreferencesKey("notifications_json")

    actual val notifications: Flow<List<NexusNotification>> = context.notifStore.data.map { prefs ->
        val raw = prefs[NOTIF_KEY] ?: return@map seedNotifications()
        runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                NexusNotification(
                    id          = o.optString("id"),
                    title       = o.optString("title"),
                    body        = o.optString("body"),
                    timestampMs = o.optLong("ts"),
                    isRead      = o.optBoolean("read", false),
                    category    = o.optString("category", "system"),
                )
            }.sortedByDescending { it.timestampMs }
        }.getOrElse { seedNotifications() }
    }

    actual suspend fun markAsRead(id: String) {
        context.notifStore.edit { prefs ->
            val raw = prefs[NOTIF_KEY] ?: return@edit
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("id") == id) o.put("read", true)
            }
            prefs[NOTIF_KEY] = arr.toString()
        }
    }

    actual suspend fun markAllRead() {
        context.notifStore.edit { prefs ->
            val raw = prefs[NOTIF_KEY] ?: return@edit
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) arr.getJSONObject(i).put("read", true)
            prefs[NOTIF_KEY] = arr.toString()
        }
    }

    actual suspend fun deleteNotification(id: String) {
        context.notifStore.edit { prefs ->
            val raw = prefs[NOTIF_KEY] ?: return@edit
            val arr = JSONArray(raw)
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                if (o.optString("id") != id) newArr.put(o)
            }
            prefs[NOTIF_KEY] = newArr.toString()
        }
    }

    /**
     * Add a notification — ADMIN ONLY.
     * Use [sendAdminNotification] from UI to enforce the admin check.
     */
    actual suspend fun addNotification(notif: NexusNotification) {
        NexusSoundManager.play(NexusSoundManager.SoundEvent.NOTIFICATION_RECEIVED)
        context.notifStore.edit { prefs ->
            val raw = prefs[NOTIF_KEY] ?: JSONArray().toString()
            val arr = JSONArray(raw)
            arr.put(JSONObject().apply {
                put("id",       notif.id)
                put("title",    notif.title)
                put("body",     notif.body)
                put("ts",       notif.timestampMs)
                put("read",     notif.isRead)
                put("category", notif.category)
            })
            prefs[NOTIF_KEY] = arr.toString()
        }
    }

    /**
     * Admin gate: Only an [com.nexuswavetech.nexusplus.core.UserSession.Authenticated]
     * user with [com.nexuswavetech.nexusplus.core.UserSession.Authenticated.isAdmin] == true
     * may send notifications. Returns true if successful.
     */
    actual suspend fun sendAdminNotification(
        title:    String,
        body:     String,
        category: String,
        session:  com.nexuswavetech.nexusplus.core.UserSession,
    ): Boolean {
        val auth = session as? com.nexuswavetech.nexusplus.core.UserSession.Authenticated
        if (auth == null || !auth.isAdmin) return false
        addNotification(
            NexusNotification(
                id          = java.util.UUID.randomUUID().toString(),
                title       = title,
                body        = body,
                timestampMs = System.currentTimeMillis(),
                isRead      = false,
                category    = category,
            )
        )
        return true
    }

    private fun seedNotifications(): List<NexusNotification> {
        val now = System.currentTimeMillis()
        return listOf(
            NexusNotification("seed_1", "Welcome to Nexus Plus", "Your ${FeatureCatalog.totalCount}-feature super-app is ready. Explore all hubs!", now - 60_000, false, "system"),
            NexusNotification("seed_2", "Biometric Vault", "Vault now uses AES-256-GCM hardware encryption. Your data is safer than ever.", now - 120_000, false, "security"),
            NexusNotification("seed_3", "Health Vault Added", "Track vitals, medications and appointments — secured with biometric lock.", now - 180_000, false, "update"),
        )
    }
}
