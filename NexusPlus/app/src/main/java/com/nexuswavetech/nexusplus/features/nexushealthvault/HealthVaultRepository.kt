package com.nexuswavetech.nexusplus.features.nexushealthvault

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

// ── Domain model ──────────────────────────────────────────────────────────────

data class HealthRecord(
    val id:         String,
    val category:   String,
    val label:      String,
    val value:      String,
    val unit:       String,
    val recordedOn: String,
)

// ── Repository ────────────────────────────────────────────────────────────────

/**
 * Persists health records to DataStore (local, always available).
 * Syncs text metadata to Firestore when the user is authenticated via Google.
 *
 * SECURITY RULES:
 *  - Only text fields are synced (no binary data, no attachments).
 *  - Cards, documents, and passwords NEVER leave the device.
 *  - If Firebase is not configured, works 100% offline.
 */
class HealthVaultRepository(private val context: Context) {

    private val Context.healthStore by preferencesDataStore(name = "nexus_health_vault")
    private val KEY = stringPreferencesKey("records_v1")

    val records: Flow<List<HealthRecord>> = context.healthStore.data.map { prefs ->
        parseJson(prefs[KEY] ?: "[]")
    }

    suspend fun addRecord(record: HealthRecord) {
        context.healthStore.edit { prefs ->
            val list = parseJson(prefs[KEY] ?: "[]").toMutableList()
            list.add(0, record)
            prefs[KEY] = toJson(list)
        }
        firestoreSet(record)
    }

    suspend fun deleteRecord(id: String) {
        context.healthStore.edit { prefs ->
            val list = parseJson(prefs[KEY] ?: "[]").filter { it.id != id }
            prefs[KEY] = toJson(list)
        }
        firestoreDelete(id)
    }

    // ── Firestore sync (small text only) ──────────────────────────────────

    private fun firestoreSet(r: HealthRecord) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("health_records").document(r.id)
                .set(mapOf(
                    "id"         to r.id,
                    "category"   to r.category,
                    "label"      to r.label,
                    "value"      to r.value,
                    "unit"       to r.unit,
                    "recordedOn" to r.recordedOn,
                    "syncedAt"   to System.currentTimeMillis(),
                ))
        }
    }

    private fun firestoreDelete(id: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        runCatching {
            FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("health_records").document(id)
                .delete()
        }
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

    private fun parseJson(raw: String): List<HealthRecord> = runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).map { i ->
            arr.getJSONObject(i).let { o ->
                HealthRecord(
                    id         = o.optString("id"),
                    category   = o.optString("category"),
                    label      = o.optString("label"),
                    value      = o.optString("value"),
                    unit       = o.optString("unit"),
                    recordedOn = o.optString("recordedOn"),
                )
            }
        }
    }.getOrElse { emptyList() }

    private fun toJson(records: List<HealthRecord>): String {
        val arr = JSONArray()
        records.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id); put("category", r.category)
                put("label", r.label); put("value", r.value)
                put("unit", r.unit); put("recordedOn", r.recordedOn)
            })
        }
        return arr.toString()
    }
}
