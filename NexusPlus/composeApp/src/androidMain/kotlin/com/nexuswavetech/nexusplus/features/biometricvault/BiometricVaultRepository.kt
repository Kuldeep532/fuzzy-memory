package com.nexuswavetech.nexusplus.features.biometricvault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure storage repository for Biometric Vault items.
 *
 * Security guarantees:
 *  - AES-256-GCM encryption via Android Keystore (hardware-backed when available)
 *  - 12-byte random IV per encrypt call; IV prepended to ciphertext and stored together
 *  - GCM authentication tag (128-bit) prevents undetected tampering
 *  - Key is NOT user-auth-required so the app can decrypt after biometric succeeds;
 *    the gate is enforced by the UI via BiometricPrompt before calling loadItems().
 */
class BiometricVaultRepository(private val context: Context) {

    companion object {
        private const val KEY_ALIAS       = "nexus_vault_aes256_key"
        private const val STORE_NAME      = "nexus_vault_secure"
        private val VAULT_PREF_KEY        = stringPreferencesKey("vault_enc_items")
        private const val GCM_TAG_BITS    = 128
        private const val GCM_IV_BYTES    = 12
    }

    private val Context.vaultStore by preferencesDataStore(name = STORE_NAME)

    // ── Android Keystore key management ──────────────────────────────────────

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

    // ── Encryption / Decryption ───────────────────────────────────────────────

    private fun encrypt(plaintext: String): String {
        val key    = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv         = cipher.iv                                // 12 bytes random
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined   = iv + ciphertext                          // IV || CT
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val combined   = Base64.decode(encoded, Base64.NO_WRAP)
        val iv         = combined.sliceArray(0 until GCM_IV_BYTES)
        val ciphertext = combined.sliceArray(GCM_IV_BYTES until combined.size)
        val key        = getOrCreateKey()
        val cipher     = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    val itemsFlow: Flow<List<VaultItem>> = context.vaultStore.data.map { prefs ->
        val raw = prefs[VAULT_PREF_KEY] ?: return@map emptyList()
        runCatching {
            val plaintext = decrypt(raw)
            val arr       = JSONArray(plaintext)
            (0 until arr.length()).map { i -> arr.getJSONObject(i).toVaultItem(i) }
        }.getOrElse { emptyList() }
    }

    suspend fun saveItems(items: List<VaultItem>) {
        val arr = JSONArray()
        items.forEach { item -> arr.put(item.toJson()) }
        val encrypted = encrypt(arr.toString())
        context.vaultStore.edit { it[VAULT_PREF_KEY] = encrypted }
    }

    // ── JSON serialization helpers ────────────────────────────────────────────

    private fun JSONObject.toVaultItem(fallbackIndex: Int): VaultItem = VaultItem(
        id          = optString("id", fallbackIndex.toString()),
        category    = runCatching { VaultCategory.valueOf(optString("category", "NOTE")) }.getOrDefault(VaultCategory.NOTE),
        title       = optString("title"),
        secret      = optString("secret"),
        cardHolder  = optString("cardHolder"),
        cardNumber  = optString("cardNumber"),
        cardExpiry  = optString("cardExpiry"),
        cardCvv     = optString("cardCvv"),
        docNumber   = optString("docNumber"),
        docExpiry   = optString("docExpiry"),
        docTag      = optString("docTag"),
        docSubType  = optString("docSubType"),
        fileUri     = optString("fileUri"),
    )

    private fun VaultItem.toJson(): JSONObject = JSONObject().apply {
        put("id",          id)
        put("category",    category.name)
        put("title",       title)
        put("secret",      secret)
        put("cardHolder",  cardHolder)
        put("cardNumber",  cardNumber)
        put("cardExpiry",  cardExpiry)
        put("cardCvv",     cardCvv)
        put("docNumber",   docNumber)
        put("docExpiry",   docExpiry)
        put("docTag",      docTag)
        put("docSubType",  docSubType)
        put("fileUri",     fileUri)
    }
}
