package com.nexuswavetech.nexusplus.features.encryptednotes

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class EncryptedNote(
    val id:        String = UUID.randomUUID().toString(),
    val title:     String = "",
    val body:      String = "",
    val createdAt: Long   = System.currentTimeMillis(),
    val updatedAt: Long   = System.currentTimeMillis(),
)

/**
 * AES-256-GCM encrypted notes backed by Android Keystore.
 * Hardware-backed key when available; 12-byte random IV per write.
 */
class EncryptedNotesRepository(private val context: Context) {

    companion object {
        private const val KEY_ALIAS    = "nexus_notes_aes256_key"
        private const val STORE_NAME   = "nexus_notes_secure"
        private val NOTES_PREF_KEY     = stringPreferencesKey("notes_enc_payload")
        private const val GCM_TAG_BITS = 128
        private const val GCM_IV_BYTES = 12
    }

    private val Context.notesStore by preferencesDataStore(name = STORE_NAME)

    private fun getOrCreateKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").also { it.load(null) }
        if (ks.containsAlias(KEY_ALIAS)) {
            return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return keyGen.generateKey()
    }

    private fun encrypt(plaintext: String): String {
        val key    = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val combined = cipher.iv + cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(combined)
    }

    private fun decrypt(encoded: String): String {
        val combined   = Base64.getDecoder().decode(encoded)
        val iv         = combined.sliceArray(0 until GCM_IV_BYTES)
        val ciphertext = combined.sliceArray(GCM_IV_BYTES until combined.size)
        val cipher     = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    val notesFlow: Flow<List<EncryptedNote>> = context.notesStore.data.map { prefs ->
        val raw = prefs[NOTES_PREF_KEY] ?: return@map emptyList()
        runCatching {
            val arr = JSONArray(decrypt(raw))
            (0 until arr.length()).map { i -> arr.getJSONObject(i).toNote() }
        }.getOrElse { emptyList() }
    }

    suspend fun saveNotes(notes: List<EncryptedNote>) {
        val arr = JSONArray().also { a -> notes.forEach { n -> a.put(n.toJson()) } }
        context.notesStore.edit { it[NOTES_PREF_KEY] = encrypt(arr.toString()) }
    }

    private fun JSONObject.toNote() = EncryptedNote(
        id        = optString("id", UUID.randomUUID().toString()),
        title     = optString("title"),
        body      = optString("body"),
        createdAt = optLong("createdAt", System.currentTimeMillis()),
        updatedAt = optLong("updatedAt", System.currentTimeMillis()),
    )

    private fun EncryptedNote.toJson() = JSONObject().apply {
        put("id",        id)
        put("title",     title)
        put("body",      body)
        put("createdAt", createdAt)
        put("updatedAt", updatedAt)
    }
}
