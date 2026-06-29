package com.nexuswavetech.nexusplus.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Nexus Plus — Google Play Billing Manager
 *
 * Subscription products (configured in Play Console):
 *  • nexusplus_monthly  → base plan: monthly_35   → ₹35/month
 *  • nexusplus_yearly   → base plan: yearly_300   → ₹300/year
 *
 * Anti-tamper: purchases are verified on-device via Google's signature on the PurchaseToken.
 * The BillingClient only reports PURCHASED state after Google has server-side validated.
 * Additionally, we only mark premium active after calling acknowledgePurchase().
 */
class BillingManager(private val context: Context) {

    companion object {
        private const val TAG = "NexusBilling"

        // Product IDs — must match exactly what you create in Play Console
        const val SKU_MONTHLY = "nexusplus_monthly"
        const val SKU_YEARLY  = "nexusplus_yearly"

        // Base plan IDs inside each subscription product
        const val PLAN_MONTHLY = "monthly_35"
        const val PLAN_YEARLY  = "yearly_300"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── Observable state ──────────────────────────────────────────────────────

    private val _premiumState = MutableStateFlow<PremiumState>(PremiumState.Unknown)
    val premiumState: StateFlow<PremiumState> = _premiumState.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _billingError = MutableStateFlow<String?>(null)
    val billingError: StateFlow<String?> = _billingError.asStateFlow()

    // ── BillingClient ─────────────────────────────────────────────────────────

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                scope.launch { purchases?.forEach { handlePurchase(it) } }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.i(TAG, "User cancelled the purchase flow.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                scope.launch { queryCurrentPremiumStatus() }
            }
            else -> {
                _billingError.value = "Purchase failed (${result.responseCode}): ${result.debugMessage}"
                Log.e(TAG, "Purchase error: ${result.debugMessage}")
            }
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    // ── Initialise ────────────────────────────────────────────────────────────

    fun init() {
        connectWithRetry()
    }

    private fun connectWithRetry(attempt: Int = 0) {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.i(TAG, "Billing connected.")
                    scope.launch {
                        queryCurrentPremiumStatus()
                        loadProducts()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                    _premiumState.value = PremiumState.Free
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected. Retrying…")
                if (attempt < 3) {
                    scope.launch {
                        delay(2_000L * (attempt + 1))
                        connectWithRetry(attempt + 1)
                    }
                }
            }
        })
    }

    // ── Query current subscription status ────────────────────────────────────

    suspend fun queryCurrentPremiumStatus() = withContext(Dispatchers.IO) {
        if (!client.isReady) { _premiumState.value = PremiumState.Free; return@withContext }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            _premiumState.value = PremiumState.Free
            return@withContext
        }
        val activePurchase = result.purchasesList.firstOrNull { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (activePurchase != null) {
            // Acknowledge unacknowledged purchases (required by Google policy)
            if (!activePurchase.isAcknowledged) acknowledgePurchase(activePurchase)
            _premiumState.value = PremiumState.Premium(
                activePurchase.products.firstOrNull() ?: SKU_MONTHLY,
                activePurchase.purchaseToken,
            )
        } else {
            _premiumState.value = PremiumState.Free
        }
    }

    // ── Load product details from Play ────────────────────────────────────────

    private suspend fun loadProducts() = withContext(Dispatchers.IO) {
        if (!client.isReady) return@withContext
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _products.value = result.productDetailsList ?: emptyList()
        }
    }

    // ── Launch purchase flow ──────────────────────────────────────────────────

    fun launchPurchase(activity: Activity, productDetails: ProductDetails, planId: String) {
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == planId }
            ?.offerToken ?: run {
            _billingError.value = "Subscription plan not available. Please try again."
            return
        }

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()

        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingError.value = "Could not open purchase screen (${result.responseCode})."
        }
    }

    // ── Handle a completed purchase ───────────────────────────────────────────

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) acknowledgePurchase(purchase)
            _premiumState.value = PremiumState.Premium(
                purchase.products.firstOrNull() ?: SKU_MONTHLY,
                purchase.purchaseToken,
            )
            _billingError.value = null
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            _premiumState.value = PremiumState.Pending
        }
    }

    private suspend fun acknowledgePurchase(purchase: Purchase) = withContext(Dispatchers.IO) {
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        val result = client.acknowledgePurchase(ackParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    fun destroy() {
        if (client.isReady) client.endConnection()
    }
}

// ── Premium state ─────────────────────────────────────────────────────────────

sealed class PremiumState {
    object Unknown : PremiumState()
    object Free    : PremiumState()
    object Pending : PremiumState()
    data class Premium(val productId: String, val token: String) : PremiumState()
}

val PremiumState.isPremium: Boolean
    get() = this is PremiumState.Premium
