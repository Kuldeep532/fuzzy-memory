package com.nexuswavetech.nexusplus.features.biometricvault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SESSION_TIMEOUT_MS = 5 * 60_000L  // 5-minute auto-lock

data class VaultUiState(
    val isUnlocked:       Boolean         = false,
    val items:            List<VaultItem> = emptyList(),
    val selectedCategory: VaultCategory?  = null,
    val authError:        String?          = null,
    val sessionSecsLeft:  Int             = (SESSION_TIMEOUT_MS / 1000).toInt(),
)

class BiometricVaultViewModel(
    private val repository: BiometricVaultRepository,
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
        _state.update { it.copy(isUnlocked = false, authError = null) }
        timeoutJob?.cancel()
    }

    private fun startSessionTimer() {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch {
            var secsLeft = (SESSION_TIMEOUT_MS / 1000).toInt()
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
        // Lock on ViewModel death (back-press, process death)
        _state.update { it.copy(isUnlocked = false) }
    }
}
