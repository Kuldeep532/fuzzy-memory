package com.nexuswavetech.nexusplus.features.biometricvault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexuswavetech.nexusplus.core.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VaultUiState(
    val isUnlocked:       Boolean         = false,
    val items:            List<VaultItem> = emptyList(),
    val selectedCategory: VaultCategory?  = null,
    val authError:        String?          = null,
    val sessionSecsLeft:  Int             = 0,
    val autoLockMinutes:  Int             = SettingsRepository.VAULT_LOCK_DEFAULT,
)

class BiometricVaultViewModel(
    private val repository:        BiometricVaultRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(VaultUiState())
    val state: StateFlow<VaultUiState> = _state.asStateFlow()

    private var timeoutJob: Job? = null

    init {
        viewModelScope.launch {
            repository.itemsFlow.collect { items ->
                _state.update { it.copy(items = items) }
            }
        }
        viewModelScope.launch {
            settingsRepository.vaultAutoLockMinutes.collect { mins ->
                _state.update { it.copy(autoLockMinutes = mins) }
                if (_state.value.isUnlocked) startSessionTimer()
            }
        }
    }

    fun onUnlockSuccess() {
        _state.update { it.copy(isUnlocked = true, authError = null) }
        startSessionTimer()
    }

    fun onAuthError(msg: String) {
        _state.update { it.copy(authError = msg) }
    }

    fun setCategory(cat: VaultCategory?) {
        _state.update { it.copy(selectedCategory = cat) }
    }

    fun addItem(item: VaultItem) = viewModelScope.launch {
        val updated = _state.value.items + item
        repository.saveItems(updated)
        resetSessionTimer()
    }

    fun deleteItem(id: String) = viewModelScope.launch {
        val updated = _state.value.items.filter { it.id != id }
        repository.saveItems(updated)
        resetSessionTimer()
    }

    fun onUserActivity() = resetSessionTimer()

    fun lock() {
        _state.update { it.copy(isUnlocked = false, authError = null, sessionSecsLeft = 0) }
        timeoutJob?.cancel()
    }

    private fun startSessionTimer() {
        timeoutJob?.cancel()
        val lockMins = _state.value.autoLockMinutes
        if (lockMins <= 0) {
            // Auto-lock disabled — clear any stale countdown
            _state.update { it.copy(sessionSecsLeft = -1) }
            return
        }
        val totalSecs = lockMins * 60
        timeoutJob = viewModelScope.launch {
            var secsLeft = totalSecs
            while (secsLeft > 0) {
                _state.update { it.copy(sessionSecsLeft = secsLeft) }
                delay(1_000L)
                secsLeft--
            }
            lock()
        }
    }

    private fun resetSessionTimer() {
        if (_state.value.isUnlocked) startSessionTimer()
    }

    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
        _state.update { it.copy(isUnlocked = false) }
    }
}
