package com.nexuswavetech.nexusplus.billing

import android.app.Activity
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for premium subscription state.
 * Wraps BillingManager and exposes simple flows for the UI.
 */
class PremiumRepository(private val billing: BillingManager) {

    val premiumState: StateFlow<PremiumState> = billing.premiumState
    val products: StateFlow<List<ProductDetails>>  = billing.products
    val billingError: StateFlow<String?>           = billing.billingError

    val isPremium: Boolean
        get() = billing.premiumState.value.isPremium

    fun init() = billing.init()

    suspend fun refresh() = billing.queryCurrentPremiumStatus()

    fun purchaseMonthly(activity: Activity) {
        val product = billing.products.value.firstOrNull { it.productId == BillingManager.SKU_MONTHLY }
            ?: return
        billing.launchPurchase(activity, product, BillingManager.PLAN_MONTHLY)
    }

    fun purchaseYearly(activity: Activity) {
        val product = billing.products.value.firstOrNull { it.productId == BillingManager.SKU_YEARLY }
            ?: return
        billing.launchPurchase(activity, product, BillingManager.PLAN_YEARLY)
    }

    fun destroy() = billing.destroy()
}
