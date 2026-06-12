package com.nexuswavetech.nexusplus.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val isLoading: Boolean = false,
    val showGuestNameDialog: Boolean = false,
    val guestName: String = "",
    val guestNameError: String? = null,
    val navigateToMain: Boolean = false,
    val error: String? = null
)

class WelcomeViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    // ── Google Sign-In ──────────────────────────────────────────────────────

    /**
     * Called from the activity/composable after [com.google.android.gms.auth.api.signin.GoogleSignInClient]
     * returns an idToken. Pass the token here; all auth state is handled in the ViewModel.
     * When Firebase is wired up this flow works automatically.
     */
    fun onGoogleSignInTokenReceived(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.signInWithGoogle(idToken)) {
                is AuthResult.Success -> {
                    sessionManager.setAuthenticatedSession(
                        uid = result.uid,
                        name = result.displayName,
                        email = result.email,
                        photoUrl = result.photoUrl
                    )
                    _uiState.value = _uiState.value.copy(isLoading = false, navigateToMain = true)
                }
                is AuthResult.Failure -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    // ── Guest Flow ──────────────────────────────────────────────────────────

    fun onContinueAsGuestClicked() {
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
        _uiState.value = _uiState.value.copy(
            showGuestNameDialog = false,
            navigateToMain = true
        )
    }

    fun onGuestDialogDismissed() {
        _uiState.value = _uiState.value.copy(showGuestNameDialog = false, guestName = "")
    }

    fun onNavigationConsumed() {
        _uiState.value = _uiState.value.copy(navigateToMain = false)
    }
}
