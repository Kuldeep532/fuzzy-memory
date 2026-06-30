package com.nexuswavetech.nexusplus.billing

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Base64

private val Context.premiumDataStore by preferencesDataStore("nexus_premium")

class PremiumRepository(
    private val context: Context,
    private val remoteConfig: RemoteConfigRepository,
) {
    private val ds = context.premiumDataStore

    private val KEY_IS_PREMIUM   = booleanPreferencesKey("is_premium")
    private val KEY_EXPIRES_AT   = longPreferencesKey("expires_at_ms")
    private val KEY_PLAN         = stringPreferencesKey("plan")
    private val KEY_TXN_ID       = stringPreferencesKey("last_txn_id")

    // ── Observable premium state ──────────────────────────────────────────────

    val isPremiumFlow: Flow<Boolean> = ds.data.map { prefs ->
        val active   = prefs[KEY_IS_PREMIUM] ?: false
        val expiresAt = prefs[KEY_EXPIRES_AT] ?: 0L
        active && (expiresAt == 0L || expiresAt > System.currentTimeMillis())
    }

    suspend fun isPremium(): Boolean = isPremiumFlow.first()

    // ── UPI details from Remote Config (obfuscated) ───────────────────────────

    val upiId: String get() {
        val encoded = remoteConfig.getString("payment_upi_id")
        return if (encoded.isNotBlank()) {
            try { String(Base64.getDecoder().decode(encoded)) } catch (_: Exception) { encoded }
        } else "nexuswave@upi"
    }

    val upiName: String    get() = remoteConfig.getString("payment_upi_name").ifBlank { "Nexus Wave Technologies" }
    val monthlyAmount: Int get() = remoteConfig.getString("payment_monthly_amount").toIntOrNull() ?: 35
    val yearlyAmount: Int  get() = remoteConfig.getString("payment_yearly_amount").toIntOrNull()  ?: 300

    // ── Submit payment request → Firestore ───────────────────────────────────

    suspend fun submitPaymentRequest(
        transactionId: String,
        plan: String,
        amount: Int,
    ): Result<Unit> = runCatching {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not signed in")
        val db  = FirebaseFirestore.getInstance()
        db.collection("payment_requests").document(uid).set(
            mapOf(
                "uid"           to uid,
                "transaction_id" to transactionId.trim(),
                "plan"          to plan,
                "amount"        to amount,
                "submitted_at"  to com.google.firebase.Timestamp.now(),
                "status"        to "pending",
            )
        ).await()
    }

    // ── Refresh premium status from Firestore ─────────────────────────────────

    suspend fun refreshFromFirestore(): Boolean {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return false
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("premium_users").document(uid).get().await()
            if (doc.exists() && doc.getBoolean("active") == true) {
                val expiresAt = doc.getLong("expires_at_ms") ?: 0L
                val plan      = doc.getString("plan") ?: "monthly"
                if (expiresAt == 0L || expiresAt > System.currentTimeMillis()) {
                    setPremium(true, expiresAt, plan)
                    return true
                }
            }
            // Check payment_requests for approved status (auto-verification path)
            val req = db.collection("payment_requests").document(uid).get().await()
            if (req.exists() && req.getString("status") == "approved") {
                val plan = req.getString("plan") ?: "monthly"
                val amount = req.getLong("amount")?.toInt() ?: monthlyAmount
                val days = if (plan == "yearly") 365L else 30L
                val expiresAt = System.currentTimeMillis() + (days * 24 * 60 * 60 * 1000L)
                setPremium(true, expiresAt, plan)
                db.collection("premium_users").document(uid).set(
                    mapOf(
                        "active"       to true,
                        "uid"          to uid,
                        "plan"         to plan,
                        "amount"       to amount,
                        "expires_at_ms" to expiresAt,
                        "activated_at"  to com.google.firebase.Timestamp.now(),
                    )
                ).await()
                return true
            }
            setPremium(false, 0L, "")
            false
        } catch (_: Exception) {
            isPremium()
        }
    }

    // ── Local storage ──────────────────────────────────────────────────────────

    suspend fun setPremium(active: Boolean, expiresAtMs: Long = 0L, plan: String = "") {
        ds.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = active
            prefs[KEY_EXPIRES_AT] = expiresAtMs
            prefs[KEY_PLAN]       = plan
        }
    }

    suspend fun getActivePlan(): String = ds.data.map { it[KEY_PLAN] ?: "" }.first()
}
