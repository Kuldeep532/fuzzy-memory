package com.nexuswavetech.nexusplus.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.remoteconfig.RemoteConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PaymentPlan(val key: String) { MONTHLY("monthly"), YEARLY("yearly") }

data class PaymentUiState(
    val selectedPlan: PaymentPlan = PaymentPlan.MONTHLY,
    val transactionId: String     = "",
    val isSubmitting: Boolean     = false,
    val submitSuccess: Boolean    = false,
    val errorMessage: String?     = null,
    val isPremium: Boolean        = false,
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

    fun setTransactionId(id: String) {
        _state.value = _state.value.copy(transactionId = id, errorMessage = null)
    }

    fun submitPayment() {
        val s = _state.value
        if (s.transactionId.isBlank()) {
            _state.value = s.copy(errorMessage = "Please enter your UPI transaction ID")
            return
        }
        val amount = if (s.selectedPlan == PaymentPlan.MONTHLY) monthlyAmount else yearlyAmount
        viewModelScope.launch {
            _state.value = _state.value.copy(isSubmitting = true, errorMessage = null)
            premium.submitPaymentRequest(s.transactionId, s.selectedPlan.key, amount)
                .onSuccess {
                    _state.value = _state.value.copy(isSubmitting = false, submitSuccess = true)
                }
                .onFailure { err ->
                    _state.value = _state.value.copy(
                        isSubmitting = false,
                        errorMessage = err.message ?: "Submission failed. Please try again.",
                    )
                }
        }
    }
}
