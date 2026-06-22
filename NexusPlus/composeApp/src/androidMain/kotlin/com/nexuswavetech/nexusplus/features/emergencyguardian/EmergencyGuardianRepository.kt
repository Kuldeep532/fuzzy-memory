package com.nexuswavetech.nexusplus.features.emergencyguardian

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.guardianStore by preferencesDataStore(name = "emergency_guardian_store")

class EmergencyGuardianRepository(private val context: Context) {

    companion object {
        private val CONTACTS_KEY = stringPreferencesKey("emergency_contacts")
    }

    val contactsFlow: Flow<List<EmergencyContact>> = context.guardianStore.data.map { prefs ->
        val raw = prefs[CONTACTS_KEY] ?: return@map emptyList()
        runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i -> arr.getJSONObject(i).toContact() }
        }.getOrElse { emptyList() }
    }

    suspend fun addContact(contact: EmergencyContact) {
        context.guardianStore.edit { prefs ->
            val existing = runCatching {
                val raw = prefs[CONTACTS_KEY] ?: "[]"
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i -> arr.getJSONObject(i).toContact() }
            }.getOrElse { emptyList() }
            val updated = existing + contact
            prefs[CONTACTS_KEY] = updated.toJsonString()
        }
    }

    suspend fun deleteContact(id: String) {
        context.guardianStore.edit { prefs ->
            val existing = runCatching {
                val raw = prefs[CONTACTS_KEY] ?: "[]"
                val arr = JSONArray(raw)
                (0 until arr.length()).map { i -> arr.getJSONObject(i).toContact() }
            }.getOrElse { emptyList() }
            val updated = existing.filter { it.id != id }
            prefs[CONTACTS_KEY] = updated.toJsonString()
        }
    }

    private fun List<EmergencyContact>.toJsonString(): String {
        val arr = JSONArray()
        forEach { c ->
            arr.put(JSONObject().apply {
                put("id",    c.id)
                put("name",  c.name)
                put("phone", c.phone)
            })
        }
        return arr.toString()
    }

    private fun JSONObject.toContact() = EmergencyContact(
        id    = optString("id",    UUID.randomUUID().toString()),
        name  = optString("name",  ""),
        phone = optString("phone", ""),
    )
}
