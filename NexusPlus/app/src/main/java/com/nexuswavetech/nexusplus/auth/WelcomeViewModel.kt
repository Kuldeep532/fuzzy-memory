package com.nexuswavetech.nexusplus.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val isLoading: Boolean           = false,
    val showGuestNameDialog: Boolean = false,
    val guestName: String            = "",
    val guestNameError: String?      = null,
    val navigateToMain: Boolean      = false,
    val error: String?               = null,
)

class WelcomeViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val consentRepository: ConsentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    // ── Legal consent state ─────────────────────────────────────────────────

    val privacyAccepted: StateFlow<Boolean> = consentRepository.privacyAccepted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val termsAccepted: StateFlow<Boolean> = consentRepository.termsAccepted
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * Auth buttons (Google + Guest) are enabled only when both legal
     * documents have been accepted — Compliance Gate per security directive.
     */
    val legalConsentGranted: StateFlow<Boolean> =
        combine(privacyAccepted, termsAccepted) { privacy, terms ->
            privacy && terms
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onPrivacyToggled(accepted: Boolean) {
        viewModelScope.launch { consentRepository.setPrivacyAccepted(accepted) }
    }

    fun onTermsToggled(accepted: Boolean) {
        viewModelScope.launch { consentRepository.setTermsAccepted(accepted) }
    }

    // ── Google Sign-In ──────────────────────────────────────────────────────

    /**
     * Called from the activity/composable after GoogleSignInClient returns an
     * idToken. When Firebase is wired up this flow works without any changes.
     */
    fun onGoogleSignInTokenReceived(idToken: String) {
        if (!legalConsentGranted.value) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    sessionManager.setAuthenticatedSession(
                        uid      = result.uid,
                        name     = result.displayName,
                        email    = result.email,
                        photoUrl = result.photoUrl,
                        isAdmin  = result.isAdmin,
                    )
                    _uiState.value = _uiState.value.copy(isLoading = false, navigateToMain = true)
                }
                is AuthResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun onGoogleSignInError(message: String) {
        _uiState.value = _uiState.value.copy(isLoading = false, error = message)
    }

    // ── Guest Flow ──────────────────────────────────────────────────────────

    fun onContinueAsGuestClicked() {
        if (!legalConsentGranted.value) return
        _uiState.value = _uiState.value.copy(showGuestNameDialog = true, guestNameError = null)
    }

    fun onGuestNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(guestName = name, guestNameError = null)
    }

    fun onGuestNameSubmitted() {
        val name = _uiState.value.guestName.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(guestNameError = "Please enter your name")
            return
        }
        sessionManager.setGuestSession(name)
        _uiState.value = _uiState.value.copy(showGuestNameDialog = false, navigateToMain = true)
    }

    fun onGuestDialogDismissed() {
        _uiState.value = _uiState.value.copy(showGuestNameDialog = false, guestName = "")
    }

    fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToMain = false)
    }
}
