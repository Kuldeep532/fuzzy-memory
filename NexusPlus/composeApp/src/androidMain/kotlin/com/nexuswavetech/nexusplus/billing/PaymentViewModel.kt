package com.nexuswavetech.nexusplus.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PaymentPlan(val key: String) { MONTHLY("monthly"), YEARLY("yearly") }

data class PaymentUiState(
    val selectedPlan: PaymentPlan = PaymentPlan.MONTHLY,
    val isSubmitting: Boolean     = false,
    val submitSuccess: Boolean    = false,
    val errorMessage: String?     = null,
    val isPremium: Boolean        = false,
    val autoVerifyPending: Boolean = false,
)

class PaymentViewModel(
    private val premium: PremiumRepository,
    private val remoteConfig: RemoteConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    val isPremiumFlow = premium.isPremiumFlow

    val upiId:    String get() = premium.upiId
    val upiName:  String get() = premium.upiName
    val monthlyAmount: Int get() = premium.monthlyAmount
    val yearlyAmount:  Int get() = premium.yearlyAmount

    init {
        viewModelScope.launch {
            val active = premium.refreshFromFirestore()
            _state.value = _state.value.copy(isPremium = active)
        }
    }

    fun selectPlan(plan: PaymentPlan) {
        _state.value = _state.value.copy(selectedPlan = plan, errorMessage = null)
    }

    /**
     * Called after the user returns from the UPI payment app.
     * Automatically verifies the payment status from Firebase
     * and grants premium access without requiring a manual transaction ID.
     */
    fun onPaymentReturned() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, autoVerifyPending = true, errorMessage = null)
            // Poll Firestore for up to 60 seconds to detect the premium activation
            var verified = false
            var attempts = 0
            while (attempts < 30 && !verified) {
                delay(2_000L)
                verified = premium.refreshFromFirestore()
                attempts++
            }
            _state.value = _state.value.copy(
                isSubmitting = false,
                autoVerifyPending = false,
                isPremium = verified,
                submitSuccess = verified,
                errorMessage = if (!verified) "Verification pending. Please check your internet connection or try again later." else null,
            )
        }
    }

    /** Legacy manual verification — kept for edge cases but no longer required by UI. */
    fun submitPayment() {
        _state.value = _state.value.copy(
            errorMessage = "Auto-verification is now active. Please complete the UPI payment and return to the app.",
        )
    }
}
