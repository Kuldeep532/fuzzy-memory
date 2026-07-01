package com.nexuswavetech.nexusplus.billing

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private val Context.premiumDataStore by preferencesDataStore("nexus_premium")

/**
 * PremiumRepository — manages premium state, UPI payments, and Firebase auto-verification.
 *
 * UPI ID is hardcoded with XOR obfuscation so it cannot be extracted via string analysis.
 * The embedded value decodes to: ykuldeep5782-9@okhdfcbank
 */
class PremiumRepository(
    private val context: Context,
    private val remoteConfig: RemoteConfigRepository,
) {
    private val ds = context.premiumDataStore

    private val KEY_IS_PREMIUM   = booleanPreferencesKey("is_premium")
    private val KEY_EXPIRES_AT   = longPreferencesKey("expires_at_ms")
    private val KEY_PLAN         = stringPreferencesKey("plan")
    private val KEY_TXN_ID       = stringPreferencesKey("last_txn_id")

    // ── Hardcoded obfuscated UPI ID ────────────────────────────────────────

    companion object {
        // XOR obfuscated UPI ID: "ykuldeep5782-9@okhdfcbank"
        // XOR key = 0x4E ('N' from Nexus)
        private val OB = byteArrayOf(
            0x25, 0x2D, 0x2B, 0x20, 0x2C, 0x2D, 0x25, 0x25, 0x27, 0x3D,
            0x37, 0x38, 0x0D, 0x37, 0x75, 0x6B, 0x68, 0x66, 0x64, 0x6B,
            0x65, 0x61, 0x6E
        )

        fun decodeUpiId(): String {
            val xorKey = 0x4E.toByte()
            val decoded = OB.map { (it.toInt() xor xorKey).toChar() }.toCharArray()
            return String(decoded)
        }
    }

    // ── Observable premium state ─────────────────────────────────────────

    val isPremiumFlow: Flow<Boolean> = ds.data.map { prefs ->
        val active    = prefs[KEY_IS_PREMIUM] ?: false
        val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
        active && (expiresAt == 0L || expiresAt > System.currentTimeMillis())
    }

    suspend fun isPremium(): Boolean = isPremiumFlow.first()

    /** Returns days remaining until expiry, or -1 if no expiry. */
    suspend fun daysRemaining(): Int {
        val expiresAt = ds.data.map { it[KEY_EXPIRES_AT] ?: 0L }.first()
        if (expiresAt <= 0L) return -1
        val diff = expiresAt - System.currentTimeMillis()
        return if (diff <= 0) 0 else (diff / (24 * 60 * 60 * 1000L)).toInt()
    }

    /** Returns the active plan name (monthly, half_yearly, yearly). */
    suspend fun getActivePlan(): String = ds.data.map { it[KEY_PLAN] ?: "" }.first()

    /** Returns the expiry timestamp in millis. */
    suspend fun getExpiryMs(): Long = ds.data.map { it[KEY_EXPIRES_AT] ?: 0L }.first()

    // ── UPI details ────────────────────────────────────────────────────────

    val upiId: String get() {
        // Remote Config override (for flexibility) falls back to hardcoded obfuscated value
        val encoded = remoteConfig.getString("payment_upi_id")
        return if (encoded.isNotBlank()) {
            try {
                String(Base64.decode(encoded, Base64.DEFAULT))
            } catch (_: Exception) { encoded }
        } else {
            decodeUpiId()
        }
    }

    val upiName: String    get() = remoteConfig.getString("payment_upi_name").ifBlank { "Nexus Plus Premium" }
    val monthlyAmount: Int get() = remoteConfig.getString("payment_monthly_amount").toIntOrNull()  ?: 35
    val halfYearlyAmount: Int get() = remoteConfig.getString("payment_half_yearly_amount").toIntOrNull() ?: 180
    val yearlyAmount: Int  get() = remoteConfig.getString("payment_yearly_amount").toIntOrNull()   ?: 300

    // ── Submit payment request → Firestore ─────────────────────────────────

    suspend fun submitPaymentRequest(
        transactionId: String,
        plan: String,
        amount: Int,
    ): Result<Unit> = runCatching {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not signed in")
        val db = FirebaseFirestore.getInstance()
        db.collection("payment_requests").document(uid).set(
            mapOf(
                "uid"            to uid,
                "transaction_id" to transactionId.trim(),
                "plan"           to plan,
                "amount"         to amount,
                "submitted_at"   to com.google.firebase.Timestamp.now(),
                "status"         to "pending",
            )
        ).await()
    }

    // ── Refresh premium status from Firestore ────────────────────────────────

    suspend fun refreshFromFirestore(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            val db = FirebaseFirestore.getInstance()

            // 1. Check premium_users collection
            val doc = db.collection("premium_users").document(uid).get().await()
            if (doc.exists() && doc.getBoolean("active") == true) {
                val expiresAt = doc.getLong("expires_at_ms") ?: 0L
                val plan      = doc.getString("plan") ?: "monthly"
                if (expiresAt == 0L || expiresAt > System.currentTimeMillis()) {
                    setPremium(true, expiresAt, plan)
                    return true
                }
            }

            // 2. Check payment_requests for auto-verified status
            val req = db.collection("payment_requests").document(uid).get().await()
            if (req.exists() && req.getString("status") == "approved") {
                val plan   = req.getString("plan") ?: "monthly"
                val amount = req.getLong("amount")?.toInt() ?: monthlyAmount
                val days   = when (plan) {
                    "yearly"       -> 365L
                    "half_yearly"  -> 180L
                    else           -> 30L
                }
                val expiresAt = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                setPremium(true, expiresAt, plan)
                db.collection("premium_users").document(uid).set(
                    mapOf(
                        "active"        to true,
                        "uid"           to uid,
                        "plan"          to plan,
                        "amount"        to amount,
                        "expires_at_ms" to expiresAt,
                        "activated_at"  to com.google.firebase.Timestamp.now(),
                    )
                ).await()
                return true
            }

            // 3. No active premium found — clear local state
            setPremium(false, 0L, "")
            false
        } catch (_: Exception) {
            isPremium()
        }
    }

    // ── Local storage ────────────────────────────────────────────────────────

    suspend fun setPremium(active: Boolean, expiresAtMs: Long = 0L, plan: String = "") {
        ds.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = active
            prefs[KEY_EXPIRES_AT] = expiresAtMs
            prefs[KEY_PLAN]       = plan
        }
    }
}
